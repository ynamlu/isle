/**
 * 心屿 · Cloudflare Worker 代理
 *
 * 作用：
 * 1. 真实模型 Key 只保存在 Worker Secret 中，绝不进入 APK；
 * 2. App 只携带可撤销的 CLIENT_TOKEN；
 * 3. 限制模型白名单与 max_tokens；
 * 4. 透传 SSE，保持流式回复；
 * 5. 不启用请求限流，优先保证图片与长回复速度。
 *
 * 部署：见同目录 README.md
 */

const CORS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "Authorization, Content-Type",
  "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
};

export default {
  async fetch(request, env) {
    const url = new URL(request.url);

    if (request.method === "OPTIONS") {
      return new Response(null, { headers: CORS });
    }

    if (request.method === "GET" && url.pathname === "/v1/models") {
      if (!authorized(request, env)) return unauthorized();
      return proxyModels(env);
    }

    if (request.method === "POST" && url.pathname === "/v1/chat/completions") {
      if (!authorized(request, env)) return unauthorized();
      return proxyChat(request, env);
    }

    return json({ error: "not found" }, 404);
  },
};

function authorized(request, env) {
  const header = request.headers.get("Authorization") || "";
  return Boolean(env.CLIENT_TOKEN) && header === `Bearer ${env.CLIENT_TOKEN}`;
}

function unauthorized() {
  return json({ error: "unauthorized" }, 401);
}

function allowedModels(env) {
  return (env.ALLOWED_MODELS || "")
    .split(",")
    .map((model) => model.trim())
    .filter(Boolean);
}

async function proxyModels(env) {
  const upstream = await fetch(`${env.UPSTREAM_BASE_URL}/models`, {
    headers: {
      Authorization: `Bearer ${env.UPSTREAM_API_KEY}`,
    },
  });

  if (!upstream.ok) {
    return new Response(upstream.body, {
      status: upstream.status,
      headers: {
        "Content-Type": upstream.headers.get("Content-Type") || "application/json",
        "Cache-Control": "no-store",
        ...CORS,
      },
    });
  }

  const payload = await upstream.json();
  const allowed = allowedModels(env);
  if (allowed.length > 0 && Array.isArray(payload.data)) {
    payload.data = payload.data.filter((model) => allowed.includes(model.id));
  }
  return json(payload);
}

async function proxyChat(request, env) {
  let body;
  try {
    const raw = await request.text();
    if (raw.length > 10_000_000) return json({ error: "payload too large" }, 413);
    body = JSON.parse(raw);
  } catch {
    return json({ error: "bad json" }, 400);
  }

  const allowed = allowedModels(env);
  if (allowed.length > 0 && !allowed.includes(body.model)) {
    return json({ error: "model not allowed" }, 400);
  }

  const maxTokens = Number(env.MAX_TOKENS || 2048);
  body.max_tokens = Math.min(Number(body.max_tokens) || maxTokens, maxTokens);
  body.stream = Boolean(body.stream);

  const upstream = await fetch(`${env.UPSTREAM_BASE_URL}/chat/completions`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${env.UPSTREAM_API_KEY}`,
    },
    body: JSON.stringify(body),
  });

  return new Response(upstream.body, {
    status: upstream.status,
    headers: {
      "Content-Type": upstream.headers.get("Content-Type") || "application/json",
      "Cache-Control": "no-cache, no-store",
      "X-Accel-Buffering": "no",
      ...CORS,
    },
  });
}

function json(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      "Content-Type": "application/json; charset=utf-8",
      "Cache-Control": "no-store",
      ...CORS,
    },
  });
}
