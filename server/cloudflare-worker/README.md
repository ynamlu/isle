# 心屿 Cloudflare Worker 代理

真实模型 Key 只保存在 Cloudflare Secret 中，APK 只携带可撤销的 `CLIENT_TOKEN`。

## 1. 安装 Wrangler

```bash
npm install -g wrangler
wrangler login
```

## 2. 创建 KV（限流用，可选）

```bash
wrangler kv namespace create RATE_LIMIT
```

把返回的 id 填进 `wrangler.toml` 的 `REPLACE_WITH_KV_NAMESPACE_ID`。

## 3. 配置密钥

```bash
wrangler secret put UPSTREAM_API_KEY   # 粘贴真实的 DeepSeek / 千问 MaaS Key
wrangler secret put CLIENT_TOKEN       # 自己生成一个随机串，给 App 用
```

不要把真实 Key 写进 `wrangler.toml` 或代码。

## 4. 部署

```bash
cd server/cloudflare-worker
wrangler deploy
```

部署成功后会得到：

```text
https://isle-proxy.<你的子域>.workers.dev
```

本项目已绑定自定义域名：

```text
https://isle-proxy.ynatech.xyz
```

## 5. 配置 App

在项目根目录 `local.properties` 中：

```properties
AI_BASE_URL=https://isle-proxy.ynatech.xyz/v1
AI_MODEL=qwen3.5-omni-plus
AI_API_KEY=<CLIENT_TOKEN>
```

然后重新构建：

```bash
./gradlew :app:assembleDebug
```

此时 APK 里只有代理令牌，没有真实模型 Key。

## 换 Key 与轮换令牌

### 只换上游真实 Key（最常见）

不需要改代码，也不需要重新打包 APK：

```bash
cd server/cloudflare-worker
npx wrangler secret put UPSTREAM_API_KEY
```

粘贴新 Key 回车即可，Secret 会自动生成新版本并立即生效。  
App 端无需任何操作。

### 换代理令牌 CLIENT_TOKEN

```bash
npx wrangler secret put CLIENT_TOKEN
```

然后必须同步更新 App 的 `local.properties`：

```properties
AI_API_KEY=<新的 CLIENT_TOKEN>
```

并重新打包、重新分发 APK。旧 APK 会因令牌失效而无法调用代理。

### 换代理地址

修改 `local.properties` 的 `AI_BASE_URL`，重新打包并分发 APK。

### 换模型

修改 `wrangler.toml` 的 `ALLOWED_MODELS` 和 `local.properties` 的 `AI_MODEL`：

```bash
npx wrangler deploy
./gradlew :app:assembleDebug
```

### 换完后验证

```bash
curl -sS https://isle-proxy.ynatech.xyz/v1/models \
  -H "Authorization: Bearer <CLIENT_TOKEN>"
```

返回模型列表即正常；再在 App 里发一条消息确认流式回复。

## 安全边界

- 真实 Key 只在 Worker Secret，不进 APK；
- `CLIENT_TOKEN` 仍可能被反编译提取，但它只能访问你的代理，不能拿到真实 Key；
- 已配置按 IP 每日限额、模型白名单、`max_tokens` 上限；
- 如发现滥用：轮换 `CLIENT_TOKEN`，或在 Cloudflare WAF 加限流规则；
- 更高安全等级可加设备注册、短期 JWT、Play Integrity，但比赛阶段通常不需要。

## 不想用代理时

保持 `AI_BASE_URL` 指向模型服务、`AI_API_KEY` 填用户自己的 Key，即 BYOK 模式。
