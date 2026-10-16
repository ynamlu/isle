# 心屿（Isle）· 10 分钟极速复现指南

> 全国高校 Vibe Coding 开发挑战赛参赛作品
> 「重塑范式：基于 Vibe Coding 的全栈应用速成挑战赛」

心屿是一款原生 Android 情感陪伴应用。用户可创建专属陪伴角色（恋人 / 闺蜜 / 朋友 / 家人），
自定义性格、背景与说话风格，角色拥有**完全离线的端侧语义长期记忆**，会记住用户说过的话、
在意的事，在每次对话中带着对用户的了解出现。

---

## 一、10 分钟复现步骤

### 0. 环境要求

| 依赖 | 版本 | 说明 |
| --- | --- | --- |
| JDK | 17+ | 构建使用 JDK 17 字节码 |
| Android SDK | Platform 37 / Build-Tools 36 | `minSdk 26`、`compileSdk 37`、`targetSdk 37` |
| Gradle | 9.6.0 | **随仓库提供 Wrapper，无需手动安装** |
| 网络 | 可访问 Google Maven / Maven Central | 首次构建需拉取依赖 |

> 本项目不需要 Android Studio，命令行即可完成全部构建。

### 1. 克隆仓库

```bash
git clone <你的仓库地址>
cd Isle
```

### 2. 配置本地 SDK 路径

在项目根目录创建 `local.properties`（该文件已被 `.gitignore` 忽略，不会入库）：

```properties
sdk.dir=/Users/你的用户名/Library/Android/sdk
# Windows 示例：sdk.dir=C\:\\Users\\你的用户名\\AppData\\Local\\Android\\Sdk
```

### 3. 配置模型服务密钥（可选）

在同一个 `local.properties` 中追加一行，构建时会被注入为 `BuildConfig.DEMO_DEEPSEEK_API_KEY`：

```properties
DEEPSEEK_API_KEY=sk-你的密钥
```

- **留空也可以**：应用启动后在「设置 → 模型服务」中手动填写。
- 优先级：**用户在应用内填写的 Key > 构建时注入的演示 Key**。
- 出于安全考虑，该文件不提交；演示用 APK 请勿公开传播，演示结束后建议在控制台停用该密钥。

### 4. 构建

```bash
# macOS / Linux
./gradlew assembleDebug

# Windows
gradlew.bat assembleDebug
```

**产物位置**

```
app/build/outputs/apk/debug/app-arm64-v8a-debug.apk    # 现代手机（推荐）
app/build/outputs/apk/debug/app-armeabi-v7a-debug.apk  # 旧机型
```

> 已按 CPU 架构拆分（`splits.abi`），以避免 ONNX Runtime 四种架构原生库全部打包导致 APK 膨胀到 180MB+。

### 5. 安装到设备

```bash
adb install -r app/build/outputs/apk/debug/app-arm64-v8a-debug.apk
```

### 6. 首次使用

1. 启动应用 → 首页已有内置角色「小屿」
2. 点击卡片进入聊天 → 可直接对话（若已注入演示密钥）
3. 右上角「🧠」查看该角色已积累的长期记忆
4. 右上角「✏️」编辑角色；首页「＋ 创建新角色」可自定义新角色
5. 「设置」→「我的资料」填写称呼 / 性格 / 自述，所有角色都能立刻了解你

### 7. 运行自动化测试（可选，验证端侧向量能力）

```bash
./gradlew connectedDebugAndroidTest   # 需要连接真机或模拟器（ARM）
```

包含两个仪器化测试：
- `TextEmbedderTest`：验证端侧 embedding 语义区分度
- `MemoryIsolationTest`：验证多角色记忆严格隔离

---

## 二、技术栈

| 层次 | 选型 |
| --- | --- |
| 语言 / 构建 | Kotlin 2.3.21 · AGP 9.4.1 · Gradle 9.6.0 |
| 界面 | Jetpack Compose · Material 3 · Navigation Compose |
| 视觉 | Haze 2.0.1（真实背景模糊 / 液态玻璃） |
| 本地存储 | Room 2.8.5（角色 / 消息 / 记忆）· DataStore（配置） |
| 网络 | Retrofit 3.0 · OkHttp 5.5 · okhttp-sse · kotlinx.serialization |
| 端侧 AI | ONNX Runtime Android 1.31 + `bge-small-zh-v1.5`(int8) |
| 图片 | Coil 3.4.0 · Photo Picker · FileProvider |

---

## 三、项目结构

```
Isle/
├── app/src/main/
│   ├── java/com/isle/companion/
│   │   ├── IsleApp.kt                 # 应用入口，持有依赖容器
│   │   ├── MainActivity.kt            # 单一 Activity + Compose
│   │   ├── di/AppContainer.kt         # 轻量依赖容器（无第三方 DI 框架）
│   │   ├── data/
│   │   │   ├── local/                 # Room：实体 / DAO / 数据库
│   │   │   ├── prefs/                 # DataStore：模型配置、用户资料
│   │   │   ├── remote/                # DeepSeek 客户端、DTO、消息内容工厂
│   │   │   └── repository/            # 角色 / 会话 / 记忆 仓储
│   │   ├── domain/
│   │   │   ├── memory/                # 端侧向量：分词器 / 推理 / 抽取 / 向量运算
│   │   │   └── prompt/PromptBuilder.kt # 提示词组装（角色卡 + 记忆 + 用户资料）
│   │   └── ui/
│   │       ├── theme/                 # 色板 / 设计规范 / 玻璃体系
│   │       ├── component/             # 头像 / 图片选择器 / 大图查看器
│   │       ├── navigation/            # 路由与导航图
│   │       └── screen/                # 角色 / 聊天 / 记忆 / 设置四个模块
│   ├── assets/embedding/              # 端侧向量模型与词表（随包分发）
│   └── res/
├── app/src/androidTest/               # 仪器化测试
├── docs/                              # 开发记录与技术文档
├── gradle/libs.versions.toml          # 版本目录（依赖集中管理）
└── local.properties                   # 本机配置（不入库）
```

---

## 四、常见问题排查

| 现象 | 原因与解法 |
| --- | --- |
| `Unresolved reference 'kotlin'` / 插件冲突 | 本项目使用**独立 Kotlin 插件**，必须保持 `android.builtInKotlin=false` 与 `android.newDsl=false`（见 `gradle.properties`） |
| `Module was compiled with an incompatible version of Kotlin` | 依赖的 Kotlin 元数据高于编译器。本项目已升到 Kotlin 2.3.21 覆盖绝大多数库；若新增库仍报错，降级该库版本 |
| `Using kotlin.sourceSets DSL ... is not allowed` | KSP 生成源码需要 `android.disallowKotlinSourceSets=false` |
| KSP 生成代码报 `Unresolved reference` | 执行 `./gradlew clean` 后重新构建 |
| 首次构建卡在下载 | 需要可访问 Google Maven 与 Maven Central 的网络 |
| 应用内键盘唤不起来 | 与设备输入法有关（历史排查见 `docs/DEVELOPMENT_LOG.md`），确认系统默认输入法为可用键盘 |
| 图片发送后点开模糊 | 已修复：聊天图片按 1600px 保存（旧图不追溯） |

---

## 五、隐私与合规说明

- 角色、记忆、对话历史与用户资料**全部存储在本机**（Room / DataStore），应用不含任何自建服务端。
- 发送消息时，对话内容与「我的资料」会传输至**用户自行配置的模型服务**（默认 DeepSeek）用于生成回复。
- 端侧语义向量（记忆检索）完全在手机本地计算，**不联网、不外传**。
- 本应用用于竞赛作品展示，不替代专业心理咨询服务。
