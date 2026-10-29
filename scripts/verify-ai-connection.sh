#!/usr/bin/env bash
# 读取 local.properties 中的模型服务配置，直接验证所填地址 / 模型 / Key 是否可用。
# 覆盖三步：GET /models 探活 → 非流式 /chat/completions → 流式 SSE。
#
# 用法： ./scripts/verify-ai-connection.sh
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PROPS="$ROOT/local.properties"

if [[ ! -f "$PROPS" ]]; then
  echo "✗ 未找到 local.properties：$PROPS" >&2
  exit 1
fi

get_prop() {
  local key="$1"
  # 与 Gradle 的 Properties 行为对齐：忽略注释行，去掉首尾空白
  awk -v k="$key" '
    /^[[:space:]]*[#!]/ { next }
    {
      line = $0
      sub(/^[[:space:]]+/, "", line)
      pos = index(line, "=")
      if (pos == 0) next
      name = substr(line, 1, pos - 1)
      gsub(/[[:space:]]+$/, "", name)
      if (name == k) {
        value = substr(line, pos + 1)
        sub(/\r$/, "", value)
        gsub(/^[[:space:]]+|[[:space:]]+$/, "", value)
        print value
        exit
      }
    }
  ' "$PROPS"
}

AI_API_KEY="$(get_prop AI_API_KEY)"
if [[ -z "$AI_API_KEY" ]]; then
  AI_API_KEY="$(get_prop OPENAI_API_KEY)"
fi
LEGACY_KEY="$(get_prop DEEPSEEK_API_KEY)"

LEGACY_MODE=0
if [[ -z "$AI_API_KEY" && -n "$LEGACY_KEY" ]]; then
  AI_API_KEY="$LEGACY_KEY"
  LEGACY_MODE=1
fi

AI_BASE_URL="$(get_prop AI_BASE_URL)"
if [[ -z "$AI_BASE_URL" ]]; then AI_BASE_URL="$(get_prop OPENAI_BASE_URL)"; fi
if [[ -z "$AI_BASE_URL" ]]; then AI_BASE_URL="$(get_prop OPENAI_API_BASE)"; fi
AI_MODEL="$(get_prop AI_MODEL)"
if [[ -z "$AI_MODEL" ]]; then AI_MODEL="$(get_prop OPENAI_MODEL)"; fi
if [[ -z "$AI_MODEL" ]]; then AI_MODEL="$(get_prop AI_MODEL_ID)"; fi

if [[ -z "$AI_BASE_URL" ]]; then
  if [[ "$LEGACY_MODE" == "1" ]]; then AI_BASE_URL="https://api.deepseek.com/v1"; else AI_BASE_URL="https://api.openai.com/v1"; fi
fi
if [[ -z "$AI_MODEL" ]]; then
  if [[ "$LEGACY_MODE" == "1" ]]; then AI_MODEL="deepseek-flash"; else AI_MODEL="gpt-4o-mini"; fi
fi

# 与 OpenAiUrls 相同的地址归一化：去掉尾部斜杠 / 已知接口后缀，host-only 时补 /v1
BASE="${AI_BASE_URL%/}"
BASE="${BASE%/chat/completions}"
BASE="${BASE%/models}"
if [[ "$BASE" =~ ^https?://[^/]+$ ]]; then
  BASE="$BASE/v1"
fi

echo "配置来源：local.properties"
echo "  API 地址：$BASE"
echo "  模型 ID ：$AI_MODEL"
if [[ -n "$AI_API_KEY" ]]; then
  echo "  API Key ：${AI_API_KEY:0:6}…${AI_API_KEY: -4}（已隐藏）"
else
  echo "  API Key ：（空，本地服务可留空）"
fi
echo

AUTH_ARGS=()
if [[ -n "$AI_API_KEY" ]]; then
  AUTH_ARGS=(-H "Authorization: Bearer $AI_API_KEY")
fi

TMP_BODY="$(mktemp)"
trap 'rm -f "$TMP_BODY"' EXIT

echo "① 探活：GET $BASE/models"
MODELS_CODE="$(curl -sS -m 30 -o "$TMP_BODY" -w "%{http_code}" "${AUTH_ARGS[@]}" "$BASE/models" || true)"
if [[ "$MODELS_CODE" == "200" ]]; then
  MODEL_COUNT="$(python3 -c 'import json,sys; d=json.load(sys.stdin); print(len(d.get("data", [])))' < "$TMP_BODY" 2>/dev/null || echo "?")"
  echo "   ✓ HTTP 200，共 $MODEL_COUNT 个模型"
else
  echo "   ⚠ HTTP $MODELS_CODE（部分服务未实现 /models，继续用对话接口验证）"
fi
echo

PY_JSON_ESCAPE='import json,sys; print(json.dumps(sys.argv[1]))'
MODEL_JSON="$(python3 -c "$PY_JSON_ESCAPE" "$AI_MODEL")"
CHAT_PAYLOAD="{\"model\":$MODEL_JSON,\"messages\":[{\"role\":\"user\",\"content\":\"只回复两个字：你好\"}],\"max_tokens\":1024}"

echo "② 非流式：POST $BASE/chat/completions"
CHAT_CODE="$(curl -sS -m 60 -o "$TMP_BODY" -w "%{http_code}" \
  "${AUTH_ARGS[@]}" -H "Content-Type: application/json" \
  -d "$CHAT_PAYLOAD" "$BASE/chat/completions" || true)"
if [[ "$CHAT_CODE" != "200" ]]; then
  echo "   ✗ HTTP $CHAT_CODE"
  head -c 800 "$TMP_BODY"; echo
  exit 1
fi
CONTENT="$(python3 -c '
import json,sys
d = json.load(sys.stdin)
choices = d.get("choices") or []
print((choices[0].get("message") or {}).get("content") or "")
' < "$TMP_BODY" 2>/dev/null || true)"
if [[ -z "$CONTENT" ]]; then
  echo "   ✗ 返回体中没有 choices[0].message.content"
  head -c 800 "$TMP_BODY"; echo
  exit 1
fi
echo "   ✓ 回复：$CONTENT"
echo

echo "③ 流式：POST $BASE/chat/completions (stream=true)"
STREAM_PAYLOAD="{\"model\":$MODEL_JSON,\"messages\":[{\"role\":\"user\",\"content\":\"用一句话夸夸我\"}],\"stream\":true,\"max_tokens\":2048}"
STREAM_TEXT="$(curl -sS -N -m 60 \
  "${AUTH_ARGS[@]}" -H "Content-Type: application/json" -H "Accept: text/event-stream" \
  -d "$STREAM_PAYLOAD" "$BASE/chat/completions" | python3 -c '
import json,sys
parts = []
for line in sys.stdin:
    line = line.strip()
    if not line.startswith("data:"):
        continue
    data = line[5:].strip()
    if data == "[DONE]":
        break
    try:
        obj = json.loads(data)
    except Exception:
        continue
    for choice in obj.get("choices") or []:
        content = (choice.get("delta") or {}).get("content")
        if content:
            parts.append(content)
print("".join(parts))
' || true)"
if [[ -z "$STREAM_TEXT" ]]; then
  echo "   ✗ 未收到任何流式正文"
  exit 1
fi
echo "   ✓ 流式回复：$STREAM_TEXT"
echo
echo "✓ 全部通过：地址 / 模型 / Key 均可用，OpenAI 兼容协议链路正常。"
