# 心屿（Isle）

> 全国高校 Vibe Coding 开发挑战赛参赛作品
> 主题：重塑范式 —— 基于 Vibe Coding 的全栈应用速成挑战

心屿是一款原生 Android **AI 情感陪伴**应用。

你可以创建专属的陪伴角色——恋人、闺蜜、朋友或家人，为 TA 设定性格、背景与说话方式，
上传 TA 的模样。TA 会记得你说过的话、喜欢的东西、在意的事，在每一次聊天里，
带着对你的了解出现，像一位认识很久的朋友，而不是每次都要重新认识的陌生人。

**技术亮点：长期记忆完全运行在手机本地**，对话内容与记忆不出设备。

---

## 目录

- [一、功能一览](#一功能一览)
- [二、10 分钟极速复现](#二10-分钟极速复现)
- [三、技术栈](#三技术栈)
- [四、项目结构](#四项目结构)
- [五、设计规范](#五设计规范)
- [六、常见问题排查](#六常见问题排查)
- [七、测试与验证](#七测试与验证)
- [八、隐私与合规](#八隐私与合规)
- [九、文档索引](#九文档索引)

---

## 一、功能一览

### 1.1 角色系统

| 能力 | 说明 |
| --- | --- |
| 内置默认角色 | 首次启动自动创建「小屿」，开箱即可对话 |
| 多角色自定义 | 名称、关系类型（恋人/闺蜜/朋友/家人/自定义）、性格、背景故事、说话风格、开场白 |
| 头像上传 | 支持相册选择与相机拍摄；自动压缩至 512px 存入应用内部目录 |
| 角色编辑 | 聊天页右上角一键进入；删除角色时级联清除其会话与记忆 |

### 1.2 长期记忆（核心）

| 能力 | 说明 |
| --- | --- |
| 自动抽取 | 每轮对话后用 LLM 抽取值得记住的事实（限 5 条/轮） |
| 温情口吻 | 记忆以角色第一人称书写，例如「记得七月二十号是你的生日…」 |
| 端侧向量化 | `bge-small-zh-v1.5`（int8，512 维）在手机本地推理，**不联网** |
| 语义召回 | 每轮按 `语义 0.8 + 关键词 0.2 + 重要度 0.1` 加权，注入 Top-6 条 |
| 严格隔离 | 每个角色拥有独立记忆库，互不串台（有仪器化测试保证） |
| 记忆管理 | 分类标签展示、单条删除、一键清空 |
| 失败降级 | 向量模型不可用时自动回退关键词匹配，不影响其他功能 |

### 1.3 对话体验

| 能力 | 说明 |
| --- | --- |
| 流式回复 | SSE 逐字返回，像真人打字 |
| 图片交流 | 可发送图片给角色（多模态），AI 能理解图片内容 |
| 大图查看 | 点击图片全屏查看，支持双指缩放与拖动 |
| 角色人设 | 通过系统提示词严格约束：保持设定、口语化、不用 Markdown、不暴露 AI 身份 |

### 1.4 其他

- **我的资料**：填写称呼 / 性格 / 自述，全局共享，新角色一见面就了解你
- **模型配置**：API Key、地址、模型 ID、温度均可在应用内调整
- **液态玻璃视觉**：真实背景模糊（backdrop blur），内容从顶栏下方滚过时实时模糊
- **深色模式**：完整亮/暗双套色板

---

## 二、10 分钟极速复现

### 2.0 环境要求

| 依赖 | 版本 | 说明 |
| --- | --- | --- |
| JDK | 17+ | 构建使用 JDK 17 字节码 |
| Android SDK | Platform 37 / Build-Tools 36 | `minSdk 26`、`compileSdk 37`、`targetSdk 37` |
| Gradle | 9.6.0 | **随仓库提供 Wrapper，无需手动安装** |
| 网络 | 可访问 Google Maven / Maven Central | 首次构建需拉取依赖 |

> **不需要 Android Studio**，命令行即可完成构建、安装与测试。

### 2.1 克隆仓库

```bash
git clone https://github.com/ynamlu/isle.git
cd isle
```

### 2.2 配置本地 SDK 路径

在项目根目录创建 `local.properties`（已被 `.gitignore` 排除，不会入库）：

```properties
# macOS
sdk.dir=/Users/你的用户名/Library/Android/sdk
# Windows（注意双反斜杠转义）
# sdk.dir=C\:\\Users\\你的用户名\\AppData\\Local\\Android\\Sdk
# Linux
# sdk.dir=/home/你的用户名/Android/Sdk
```

### 2.3 配置模型服务密钥（可选）

在同一文件中追加一行，构建时会被注入为 `BuildConfig.DEMO_DEEPSEEK_API_KEY`：

```properties
DEEPSEEK_API_KEY=sk-你的密钥
```

说明：
- **留空也可以**——应用启动后在「设置 → 模型服务」中手动填写即可
- 优先级：**用户在应用内填写的 Key > 构建时注入的演示 Key**
- 该文件不入库；演示用 APK 请勿公开传播

### 2.4 构建

```bash
# macOS / Linux
./gradlew assembleDebug

# Windows
gradlew.bat assembleDebug
```

**产物位置**

```
app/build/outputs/apk/debug/app-arm64-v8a-debug.apk     # 现代手机（推荐）
app/build/outputs/apk/debug/app-armeabi-v7a-debug.apk   # 旧机型
```

> 已按 CPU 架构拆分（`splits.abi`）。ONNX Runtime 自带四套原生库，
> 不拆分会导致包体从 67MB 膨胀到 185MB。

### 2.5 安装与运行

```bash
adb install -r app/build/outputs/apk/debug/app-arm64-v8a-debug.apk
```

### 2.6 首次使用路径

1. 启动应用 → 首页已有内置角色「小屿」
2. 点击卡片进入聊天 → 可直接对话（若已注入演示密钥）
3. 右上角 🧠 → 查看该角色已积累的长期记忆
4. 右上角 ✏️ → 编辑角色信息
5. 首页「＋ 创建新角色」→ 自定义新角色
6. 「设置」→「我的资料」→ 填写后所有角色都能立刻了解你

### 2.7 运行自动化测试（可选）

```bash
./gradlew connectedDebugAndroidTest   # 需要连接真机或 ARM 模拟器
```

包含两个仪器化测试：

| 测试 | 验证内容 | 预期结果 |
| --- | --- | --- |
| `TextEmbedderTest` | 端侧 embedding 的语义区分度 | 相近句相似度显著高于无关句 |
| `MemoryIsolationTest` | 多角色记忆严格隔离 + 级联删除 | 2 tests, 0 failures |

---

## 三、技术栈

| 层次 | 选型 | 版本 |
| --- | --- | --- |
| 语言 | Kotlin | 2.3.21 |
| 构建 | Android Gradle Plugin / Gradle | 9.4.1 / 9.6.0 |
| 界面 | Jetpack Compose + Material 3 | BOM 2026.09.00 |
| 导航 | Navigation Compose | 2.10.2 |
| 视觉 | Haze（真实背景模糊 / 液态玻璃） | 2.0.1 |
| 本地数据库 | Room | 2.8.5 |
| 键值配置 | DataStore Preferences | 1.2.1 |
| 网络 | Retrofit / OkHttp / okhttp-sse | 3.0.0 / 5.5.0 |
| 序列化 | kotlinx.serialization | 1.11.0 |
| 端侧推理 | ONNX Runtime Android | 1.31.0 |
| 端侧模型 | bge-small-zh-v1.5（int8） | 512 维 / 23.9MB |
| 图片 | Coil | 3.4.0 |

> **版本约束说明**：本项目使用独立 Kotlin 插件（非 AGP 内置），
> 详见 [六、常见问题排查](#六常见问题排查)。

---

## 四、项目结构

```
Isle/
├── app/
│   ├── build.gradle.kts                    # 模块配置（SDK 版本、签名、密钥注入、ABI 拆分）
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml         # 应用声明、FileProvider、软键盘模式
│       │   ├── assets/embedding/           # 端侧向量模型与词表（随包分发）
│       │   │   ├── model_int8.onnx         # bge-small-zh-v1.5 量化模型（23.9MB）
│       │   │   └── vocab.txt               # BERT 词表（21128 词条）
│       │   ├── java/com/isle/companion/
│       │   │   ├── IsleApp.kt              # 应用入口，持有依赖容器
│       │   │   ├── MainActivity.kt         # 单一 Activity + Compose 宿主
│       │   │   ├── di/
│       │   │   │   ├── AppContainer.kt     # 手写依赖容器
│       │   │   │   └── ContainerExt.kt     # Context 扩展
│       │   │   ├── data/
│       │   │   │   ├── local/              # Room：实体 / DAO / 数据库
│       │   │   │   ├── prefs/              # DataStore：模型配置、用户资料
│       │   │   │   ├── remote/             # DeepSeek 客户端、DTO、多模态内容工厂
│       │   │   │   └── repository/         # 角色 / 会话 / 记忆 仓储
│       │   │   ├── domain/
│       │   │   │   ├── memory/             # 分词器 / 向量推理 / 抽取 / 向量运算
│       │   │   │   └── prompt/             # 提示词组装
│       │   │   ├── ui/
│       │   │   │   ├── theme/              # 色板 / 设计规范 / 玻璃体系
│       │   │   │   ├── component/          # 头像 / 图片选择 / 大图查看 / 关于
│       │   │   │   ├── navigation/         # 路由与导航图
│       │   │   │   └── screen/
│       │   │   │       ├── character/      # 角色列表 / 编辑
│       │   │   │       ├── chat/           # 聊天页
│       │   │   │       ├── memory/         # 记忆管理
│       │   │   │       └── settings/       # 设置主页 + 4 个二级页
│       │   │   └── util/AvatarStorage.kt   # 图片导入与分级压缩
│       │   └── res/                        # 资源（主题 / 图标 / 备份规则）
│       └── androidTest/                    # 仪器化测试
├── docs/                                   # 开发记录 / 技术文档 / AI 协同记录
├── gradle/libs.versions.toml               # 版本目录（依赖版本唯一来源）
├── gradle.properties                       # 构建开关（含关键约束）
└── local.properties                        # 本机配置（不入库）
```

---

## 五、设计规范

为保证 AI 生成的界面代码风格统一，先行固化了设计规范（`ui/theme/DesignSystem.kt`）。

### 5.1 色彩

| 角色 | 亮色 | 暗色 | 用途 |
| --- | --- | --- | --- |
| primary | `#6C5CE7` 岛屿暮紫 | `#B4A7F5` | 品牌主色、强调 |
| secondary | `#5AA9E6` 海雾蓝 | `#8FCBF0` | 渐变副色 |
| tertiary | `#F08BA8` 暖粉 | `#F3A8C0` | 点缀 |
| background | `#F6F4FF` | `#121016` | 页面底色 |

**已关闭 Material You 动态取色**——系统按壁纸生成的配色会与品牌色互相干扰，
导致同一界面出现两套色彩体系。

### 5.2 圆角与间距

| 令牌 | 值 | 用途 |
| --- | --- | --- |
| `IsleRadius.card` | 20dp | 常规卡片 |
| `IsleRadius.cardLarge` | 24dp | 强调卡片（Hero） |
| `IsleRadius.field` | 16dp | 输入框 |
| `IsleRadius.chip` | 10dp | 标签 |
| `IsleSpacing.md / lg / xl` | 12 / 16 / 20dp | 元素 / 屏幕 / 区块间距 |

### 5.3 液态玻璃

| 令牌 | 参数 | 说明 |
| --- | --- | --- |
| `blurRadius` | 22dp | 背景模糊半径 |
| `noiseFactor` | 0.06 | 磨砂颗粒感 |
| 玻璃底色 | 白色 alpha 0.14（亮） | 保持通透 |
| 卡片填充 | 白色渐变 0.40 → 0.18 | 渐变底色可透出 |

用法：
```kotlin
val hazeState = rememberLiquidGlassState()
// 内容容器（作为模糊来源）
Modifier.liquidGlassSource(hazeState)
// 悬浮栏（顶栏 / 输入栏）
Modifier.liquidGlass(hazeState)
```

---

## 六、常见问题排查

| # | 现象 | 原因 | 解法 |
| --- | --- | --- | --- |
| 1 | `Cannot add extension with name 'kotlin'` | AGP 9 内置 Kotlin 与独立插件冲突 | 保持 `android.builtInKotlin=false`（本项目已改用独立插件） |
| 2 | `plugin 'org.jetbrains.kotlin.android' is not compatible with AGP's 9.0 new DSL` | AGP 9 新 DSL 与独立 Kotlin 插件不兼容 | 保持 `android.newDsl=false` |
| 3 | `Module was compiled with an incompatible version of Kotlin` | 某依赖的 Kotlin 元数据高于当前编译器 | 本项目编译器为 2.3.21（可读 2.4.0）；若新增库仍报错，降级该库 |
| 4 | `Using kotlin.sourceSets DSL ... is not allowed` | KSP 生成源码需要注入 sourceSets | 保持 `android.disallowKotlinSourceSets=false` |
| 5 | KSP 生成代码报 `Unresolved reference` | 增量缓存不一致 | `./gradlew clean` 后重新构建 |
| 6 | 依赖下载卡住 / TLS 报错 | 网络不通或瞬时抖动 | 确认可访问 Google Maven 与 Maven Central；重试 |
| 7 | 应用内键盘唤不起来 | 设备默认输入法异常（非 App 问题） | 到系统设置确认默认输入法为可用键盘 |
| 8 | 图片发送后点开模糊 | 历史数据：早期版本聊天图按 512px 保存 | 已修复（现为 1600px）；旧图不追溯，重新发送即可 |
| 9 | APK 体积异常大 | 未按架构拆分，四套原生库全量打包 | 确认 `splits.abi` 生效，安装对应架构的 APK |
| 10 | 构建成功但修改未生效 | Gradle 增量缓存 | 使用 `--rerun-tasks` 强制全量 |

**关键约束速查**（`gradle.properties`）：

```properties
android.builtInKotlin=false   # 关闭 AGP 内置 Kotlin（编译器固定 2.2.0）
android.newDsl=false          # 关闭 AGP 9 新 DSL，允许独立 Kotlin 插件
android.disallowKotlinSourceSets=false   # 允许 KSP 注入生成源码
```

---

## 七、测试与验证

### 7.1 仪器化测试

```bash
./gradlew connectedDebugAndroidTest
```

| 测试 | 断言内容 | 实测结果 |
| --- | --- | --- |
| `TextEmbedderTest` | 向量维度为 512；相近句相似度显著高于无关句 | `sim(爬山,登山)=0.85`、`sim(爬山,股票)=0.39` |
| `MemoryIsolationTest` | 角色间记忆互不可见；删除角色级联清除其记忆 | 2 tests, 0 failures |

### 7.2 编译验证

```bash
./gradlew :app:compileDebugKotlin --rerun-tasks
# BUILD SUCCESSFUL，0 警告
```

### 7.3 真机验证清单

- [x] 首次启动自动创建默认角色，无崩溃
- [x] 进入聊天页自动发送角色开场白
- [x] 发送文本 → 流式逐字回复，人设口吻符合设定
- [x] 发送图片 → AI 能描述图片内容（多模态生效）
- [x] 对话后自动生成长期记忆并在记忆页可见
- [x] 多角色记忆互不串台
- [x] 头像上传（相册 + 拍照）正常
- [x] 图片全屏查看支持双指缩放与拖动
- [x] 设置四个二级页跳转正常
- [x] 键盘弹出时输入框自动上移

---

## 八、隐私与合规

### 8.1 数据存储

- 角色、会话记录、长期记忆、用户资料**全部存储在本机**
  （Room 数据库 + DataStore），应用**不含任何自建服务端**
- 端侧语义向量（记忆检索）**完全在手机本地计算，不联网、不外传**

### 8.2 数据传输

- 发送消息时，**对话内容与「我的资料」会传输至用户自行配置的模型服务**
  （默认 DeepSeek）用于生成回复
- 记忆抽取同样复用该模型服务

### 8.3 密钥安全

- 演示密钥通过 `local.properties` 在构建时注入，**不含在仓库中**
- 应用内填写的密钥优先于内置演示密钥
- 演示 APK 请勿公开分发；正式发布需改为自建后端代理

### 8.4 免责声明

本应用为竞赛作品展示，不替代专业心理咨询服务。

---

## 九、文档索引

| 文档 | 内容 |
| --- | --- |
| `docs/DEVELOPMENT_LOG.md` | 开发全流程记录：五个阶段的产出、13 个问题的定位与修复过程 |
| `docs/TECHNICAL_REPORT.md` | 技术文档：意图控制策略、架构设计、端侧记忆引擎、可复现性论证 |
| `docs/AI_COLLABORATION_LOG.md` | AI 协同过程：关键 Prompt 链还原（含报错原文与引导过程） |
| `docs/AI_CONVERSATION_EXPORT.md` | 对话历史全量导出（自动生成 + 敏感信息脱敏） |
