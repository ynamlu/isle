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

## 安全边界

- 真实 Key 只在 Worker Secret，不进 APK；
- `CLIENT_TOKEN` 仍可能被反编译提取，但它只能访问你的代理，不能拿到真实 Key；
- 已配置按 IP 每日限额、模型白名单、`max_tokens` 上限；
- 如发现滥用：轮换 `CLIENT_TOKEN`，或在 Cloudflare WAF 加限流规则；
- 更高安全等级可加设备注册、短期 JWT、Play Integrity，但比赛阶段通常不需要。

## 不想用代理时

保持 `AI_BASE_URL` 指向模型服务、`AI_API_KEY` 填用户自己的 Key，即 BYOK 模式。
