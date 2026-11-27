# 心屿分享页

这是情绪手账二维码指向的落地页。

## 安装入口

页面链接到 v2.3.5 标签的 README。准备好独立的公开安装包后，可将「查看安装指南」的链接改为安装包地址，并同步更新按钮文字。

## 部署

把 `share/` 整个目录上传到静态托管，使用 `index.html` 作为入口。需要一起上传 `style.css`、`icon.png` 和 `screenshot.png`。

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
