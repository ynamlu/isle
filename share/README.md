# 心屿分享页

这是情绪手账二维码指向的落地页。

## 部署

把 `share/` 整个目录上传到任意静态托管（Vercel / Netlify / Cloudflare Pages / GitHub Pages / 对象存储均可）。

## 配置二维码地址

部署完成后，在项目根目录的 `local.properties` 中添加：

```properties
SHARE_QR_URL=https://你的域名/
```

然后重新构建 APK：

```bash
./gradlew :app:assembleDebug
```

手账图右下角的二维码会指向这个地址。

## 当前默认地址

如果未配置 `SHARE_QR_URL`，二维码默认指向：

```text
https://github.com/ynamlu/isle
```
