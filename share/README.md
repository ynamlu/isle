# 心屿分享页

这是情绪手账二维码指向的落地页。

## 下载地址

页面上的「下载 APK」指向夸克网盘：

```text
https://pan.quark.cn/s/f9591b0413aa
```

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

## 当前地址

当前部署地址：

```text
https://isle.ynatech.xyz/
```

如果未配置 `SHARE_QR_URL`，构建默认也使用该地址。
