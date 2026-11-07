# 心屿 Cloudflare Worker 代理

真实模型 Key 只保存在 Cloudflare Secret 中，APK 只携带可撤销的 `CLIENT_TOKEN`。

## 1. 安装 Wrangler

```bash
npm install -g wrangler
wrangler login
```

## 2. 配置密钥

```bash
wrangler secret put UPSTREAM_API_KEY   # 粘贴真实的 DeepSeek / 千问 MaaS Key
wrangler secret put CLIENT_TOKEN       # 自己生成一个随机串，给 App 用
```

不要把真实 Key 写进 `wrangler.toml` 或代码。

## 3. 部署

```bash
cd server/cloudflare-worker
wrangler deploy
```

部署成功后，Worker 名称会出现在 Cloudflare 控制台。本项目已关闭 `workers.dev`，只使用自定义域名：

```text
https://isle-proxy.ynatech.xyz
```

未绑定自定义域名时，也可以在 `wrangler.toml` 中显式设置 `workers_dev = true` 后使用 `workers.dev` 地址。

已启用 Cloudflare Smart Placement，尽量把 Worker 调度到靠近上游的区域。

## 4. 配置 App

在项目根目录 `local.properties` 中：

```properties
AI_BASE_URL=https://isle-proxy.ynatech.xyz/v1
AI_MODEL=deepseek-v4.1-flash-1017
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

## 性能注意

Worker 会读取并重新序列化请求体，图片 base64 越大，代理转发耗时越明显。建议客户端把聊天图片压到 1024px / JPEG 82 左右，通常可降到 100–200KB。

## 安全边界

- 真实 Key 只在 Worker Secret，不进 APK；
- `CLIENT_TOKEN` 仍可能被反编译提取，但它只能访问你的代理，不能拿到真实 Key；
- 已配置模型白名单、`max_tokens` 上限与 10MB 请求体上限；
- 当前不启用按 IP 限流，避免图片与长回复被限额打断；
- 如发现滥用：轮换 `CLIENT_TOKEN`，或在 Cloudflare WAF 临时加规则；
- 更高安全等级可加设备注册、短期 JWT、Play Integrity，但比赛阶段通常不需要。

## 不想用代理时

保持 `AI_BASE_URL` 指向模型服务、`AI_API_KEY` 填自己的 Key，即 BYOK 模式。
