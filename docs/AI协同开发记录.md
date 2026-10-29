# 心屿（Isle）· AI 协同开发记录

> **从立项、开发、调试到成品**
> 一份以「意图 → 方案 → 报错 → 定位 → 修复 → 验证」为主线的 AI 协同实录

| 项目 | 内容 |
| --- | --- |
| 项目 | 心屿（Isle）· 原生 Android AI 情感陪伴应用 |
| 协作方式 | Vibe Coding：人类定义约束与验收标准，AI 生成与执行，编译器/测试/真机做裁判 |
| 时间跨度 | 2026-10-15 15:51 — 2026-10-17 15:30 |
| 核心链路 | 角色卡 + 全局用户资料 + 端侧语义记忆 + DeepSeek 流式对话 |
| 交付物 | APK、6 份 Markdown 文档 + 2 份 PDF、17 张截图、40+ 次提交 |

> **说明**
> 本文中的用户发言为**保留原意的书面化整理**，不是逐字稿；AI 方案、报错原文、命令输出、测试数据均来自真实执行结果。
> 完整的 785 条消息（含 749 次工具调用）保存在本地会话数据库，不随包分发。

---

## 一页看懂

| 阶段 | 时间 | 目标 | 结果 |
| --- | --- | --- | --- |
| 立项与选型 | 10-15 15:51–16:03 | 产品形态、平台、技术栈 | Kotlin + Compose + Room + ONNX |
| 工程骨架 | 16:03–16:07 | 可编译、可安装 | 25 个文件；首个 APK 11.6MB |
| 数据与网络 | 16:07–16:24 | 角色 / 会话 / 记忆 + DeepSeek | Room 三表、SSE、图片多模态 |
| 端侧记忆 | 16:25–16:30 | 完全离线的语义检索 | bge-small-zh int8、512 维、2 tests |
| 资料与隐私 | 16:31–16:55 | 可交付的设置体系 | 四个二级页、密钥注入、隐私修正 |
| 体验攻坚 | 16:55–17:18 | 图片 / 输入法 / 质感 | 1600px、IME 责任边界、玻璃卡片 |
| 视觉重构 | 17:18–17:42 | 全局统一 | Haze 2.0.1、Kotlin 2.3.21、ABI 拆分 |
| 交付与合规 | 10-16–10-17 | 文档 / 截图 / 未成年保护 | 22 页技术文档、16 页说明书、年龄门 |
| 交付后修复 | 10-17 15:00–15:30 | 真机验收问题 | 8 项修复；2 tests / 0 failures |

---

## 迭代零 · 立项与技术选型

### 目标

在写第一行代码前，把「做什么、给谁做、在哪个平台做、记忆怎么实现」定清楚。

### 意图（整理）

| # | 用户诉求 | 落地结论 |
| --- | --- | --- |
| 1 | 先熟悉既有 Web 原型，输出架构与关键实现 | 识别出多 Provider、情感分析、长期记忆缺口 |
| 2 | 评估重构为原生 Android，模型只接 DeepSeek-V4.1-Flash | 可行；砍掉多 Provider 与后端代理 |
| 3 | 产品形态为 AI 女友/男友/闺蜜/朋友 | 定位「可自定义角色 + 长期记忆」 |
| 4 | 角色可自定义性格、简介，并保留记忆 | 设计 `Character` / `Memory` 两个核心实体 |
| 5 | DeepSeek 暂无 Embedding API | 改为端侧 `bge-small-zh-v1.5` ONNX |
| 6 | 默认角色 + 多角色 + 用户上传头像；暂不做语音/立绘 | 收敛 v1 范围，头像落应用内部目录 |
| 7 | 新建 Android 工程，命名「心屿」 | 包名 `com.isle.companion`，仅 Android |

### 决策

- **平台**：原生 Kotlin + Jetpack Compose。端侧 ONNX 推理需要直连原生接口，跨平台方案的 FFI 成本更高。
- **架构**：UI（Compose）→ Domain（Prompt/Memory）→ Data（Room/DataStore/DeepSeek）→ 手写 `AppContainer`，不引入 DI 框架。
- **记忆**：LLM 抽取事实 → 端侧向量化 → Room BLOB → 本地加权召回，不依赖云端向量库。
- **边界**：不含自建服务端；对话内容只在发送时传输至用户配置的模型服务。

### 复盘

- 先收敛范围，再写代码。最早一版曾考虑语音、立绘、实时通话，最终全部推迟到 v1 之后。
- 「记忆」不是附加功能，而是产品定义的一部分；它决定了数据模型、提示词与隐私表述。

---

## 迭代一 · 工程骨架：先让项目跑起来

### 目标

产出一个可编译、可安装、可启动的最小工程，为后续所有功能提供稳定地基。

### 意图（整理）

> 新建一个原生 Android 工程，应用名「心屿」，包名 `com.isle.companion`；使用较新的 Kotlin + Compose 组合，先搭一个能编译、能跑起来的骨架。

### 方案

生成 `settings.gradle.kts`、根 `build.gradle.kts`、版本目录 `libs.versions.toml`、`gradle.properties`、Manifest、`MainActivity`、Compose 主题与占位首页，并立即执行构建。

### 报错与定位

**报错 1**

```
Cannot add extension with name 'kotlin', as there is an extension already registered with that name.
```

**定位**：反编译 AGP jar 的选项类，发现 `BooleanOption.BUILT_IN_KOTLIN`（对应 `android.builtInKotlin`），确认 AGP 9.4.1 已内置 Kotlin 编译器并注册了 `kotlin` 扩展。

**首轮修复**：移除独立的 `org.jetbrains.kotlin.android` 插件，改用 AGP 内置 Kotlin。

**报错 2（连带问题）**

```
Starting in Kotlin 2.0, the Compose Compiler Gradle plugin is required when compose is enabled.
```

**修复**：只补回 `org.jetbrains.kotlin.plugin.compose`，不重新引入 Kotlin Android 插件。

### 验证

```
./gradlew assembleDebug
BUILD SUCCESSFUL in 33s
产物：app-debug.apk（11.6 MB）
```

### 复盘

- **不猜 API**：让 AI 去读依赖字节码，而不是「回忆文档」。
- **连带问题预判**：移除插件前先确认 Compose 编译是否依赖它。
- 这一轮建立的目录结构、版本目录与构建约束，后续 40+ 次提交都沿用。

---

## 迭代二 · 数据层与网络层：打通「角色 → 对话」

### 目标

建立 Room 三表与 DeepSeek 客户端，让应用真正能创建角色、发送消息、收到流式回复。

### 意图（整理）

> 实现本地数据层：角色、消息、记忆三张表；记忆表预留 `embedding` 二进制列；删除角色时级联清除会话与记忆。
> 网络层使用 DeepSeek 的 OpenAI 兼容接口：地址可在应用内修改，支持 SSE 流式与主动取消，支持图片 base64 内联，DTO 兼容 `reasoning_content`。

### 方案

- `CharacterEntity / MessageEntity / MemoryEntity` + 三个 DAO + `IsleDatabase`；
- `DeepSeekClient`：Retrofit 非流式 + OkHttp SSE 流式；
- `ContentFactory`：纯文本 / 文本 + 图片多模态；
- `PromptBuilder`：角色卡 + 用户资料 + 召回记忆 + 最近 12 条历史。

### 报错与定位

**报错 3（KSP 注入源码被拒）**

```
Using kotlin.sourceSets DSL to add Kotlin sources is not allowed with built-in Kotlin.
Solution: ... set android.disallowKotlinSourceSets=false in gradle.properties.
```

**处理**：按官方提示加入 `android.disallowKotlinSourceSets=false`。
> 该开关只在内置 Kotlin 方案下需要；升级 Kotlin 2.3.21、改用独立插件后已移除。

**报错 4（依赖元数据版本不兼容）**

```
Class 'kotlin.reflect.KClass' was compiled with an incompatible version of Kotlin.
The actual metadata version is 2.4.0, but the compiler version 2.2.0 can read versions up to 2.3.0.
```

**定位**

```bash
./gradlew :app:dependencyInsight \
  --dependency org.jetbrains.kotlin:kotlin-stdlib \
  --configuration debugCompileClasspath
```

```
org.jetbrains.kotlin:kotlin-stdlib:2.4.10
  Selection reasons:
    - By constraint
+--- io.coil-kt.coil3:coil-android:3.6.3
```

→ **Coil 3.6.3 通过 constraint 强制 stdlib 2.4.10**。逐版本核对 POM 后确认 Coil **3.4.0** 依赖 stdlib 2.3.10，降级后构建通过。

### 验证

| 项目 | 结果 |
| --- | --- |
| 安装启动 | 正常，无崩溃 |
| 角色人设 | 回复符合设定（闺蜜口吻、使用「嗯嗯」「其实呀」） |
| 开场白 | 首次进入聊天页自动发送 |
| 图片多模态 | 发送截图后，AI 准确描述页面内容 |
| APK | 21.5MB；解包确认新类已进包 |

### 复盘

- **报错即上下文**：回灌 `dependencyInsight` 的完整输出，而不是只贴报错行。
- **降级是临时方案**：同一类元数据问题在视觉阶段再次出现，最终通过升级 Kotlin 根治。

---

## 迭代三 · 端侧语义记忆：产品真正的差异化

### 目标

让角色「记得你」，且记忆检索完全在手机本地完成，不依赖云端向量库。

### 意图（整理）

> 记忆检索使用端侧中文 embedding，模型放 assets 随包分发。
> 先定义抽象接口与降级路径：模型加载失败时，对话、角色、记忆书写都不能受影响。
> 分词器自行实现，不引入重型 NLP 依赖。

### 方案

```
EmbeddingProvider（接口）
  └─ OnnxEmbeddingProvider（加载失败返回 null）

BertTokenizer      —— WordPiece（CJK 逐字切分 + 贪心最长匹配）
TextEmbedder       —— ONNX Runtime，CLS 池化 + L2 归一化 → 512 维
VectorMath         —— 余弦相似度 + FloatArray ↔ ByteArray
MemoryRepository   —— 写入补向量；召回 语义0.8 + 关键词0.2 + 重要度0.1
MemoryExtractor    —— LLM 抽取结构化记忆（JSON）
```

### 关键问题 1：记忆措辞过于生冷

**意图（整理）**

> 记忆文案不要再用「用户……」这种第三人称冷叙述，改成角色第一人称、有温度的表述。

**定位**：抽取提示词要求使用第三人称「用户」，输出如「用户喜欢吃东西。」

**修复后的抽取约束**

```
1. 用「你」称呼对方，写成一句温柔自然的话，像是你在心里默默记住的
2. 绝对不能出现「用户」这两个字，也不要写成冷冰冰的资料条目
3. 已知的信息不要重复记录，只记这次新出现的内容
4. 每条独立完整，能脱离上下文理解
5. 只记明确出现的信息，不要推测
```

**验证输出**

```
记得七月二十号是你的生日，是夏天里海边最舒服的时候
知道你周末喜欢去爬山，身上总带着一股很清爽的劲儿
```

### 关键问题 2：多角色记忆是否串台

**意图（整理）**

> 多角色记忆必须严格隔离；这类问题不能靠肉眼确认，需要用测试固化。

**做法**：`MemoryIsolationTest` 四步断言——
创建两个角色各写一条记忆 → 断言互不可见 → 删除其一 → 断言级联清除且不影响另一个。

### 验证（真机）

```
./gradlew connectedDebugAndroidTest
→ 2 tests, 0 failures

[EmbedderTest] sim(爬山, 登山) = 0.8496689
[EmbedderTest] sim(爬山, 股票) = 0.38915947
```

语义相近句 **0.85**，无关句 **0.39**；512 维，3 次推理约 0.4s（真机）。

### 复盘

- **抽象先行**：先定义接口与失败回退，再替换实现。
- **不可见的正确性 → 测试固化**：记忆隔离这种「看不见」的约束必须写成断言。
- **情感需求也是需求**：措辞温度通过提示词工程直接解决。

---

## 迭代四 · 设置、资料与隐私：从「能用」到「可交付」

### 目标

建立可交付的设置体系：全局用户资料、模型配置、密钥注入、准确的隐私表述。

### 意图（整理）

> 设置页需要项目介绍与开发背景；API Key 放到二级页面；评估演示 Key 打入 APK 的风险。
> 增加全局「我的资料」，让所有角色都能快速了解用户；同时确保隐私文案与真实数据流一致。

### 方案

| 模块 | 实现 |
| --- | --- |
| 设置主页 | 关于心屿内嵌 + 我的资料 / 模型服务 / 回复风格 / 技术亮点四个入口 |
| 模型服务 | 二级页配置 API Key、地址、模型 ID；演示 Key 构建时注入 |
| 我的资料 | 称呼 / 性格 / 自述，DataStore 全局共享 |
| 隐私文案 | 从「仅保存在本机」改为「本地存储，发送时传输至模型服务」 |

### 密钥方案

```properties
# local.properties（不入库）
DEEPSEEK_API_KEY=sk-...
```

构建时注入 `BuildConfig.DEMO_DEEPSEEK_API_KEY`；优先级：**用户填写的 Key > 内置演示 Key**。

> 演示 APK 中的密钥可被反编译提取，只用于演示；正式发布需改为自建后端代理。

### 关键解释：为什么资料不能「只发一次」

用户问：既然有知识库，为什么资料不能只发送一次就长期生效？

**回答**：对话接口是无状态的——模型不会跨请求保留上下文。所谓「记住」，是应用在每次请求时把
**角色卡 + 召回记忆 + 用户资料**重新拼进系统提示词。资料只占几十个 token，成本可忽略；
真正需要控制的是历史窗口与记忆条数。

### 验证

- 真机确认设置页四个入口与两处隐私文案；
- 「我的资料」进入系统提示词与记忆抽取提示词；
- 记忆抽取增加「已知信息不重复记录」规则。

### 复盘

- **安全要说清代价**：密钥注入不是「藏起来」，而是「演示可接受、正式发布不可接受」。
- **隐私文案必须与数据流一致**：一句「仅保存在本机」如果被理解成「不出设备」，就是误导。
- **无状态 API 需要应用层「记忆」**：这解释了为什么记忆库是产品核心，而不是附属功能。

---

## 迭代五 · 体验缺陷攻坚：用证据链定位问题

### 5.1 图片点开模糊

**意图（整理）**

> 聊天中发出的图片点开看不清，需要支持全屏查看并保证清晰度。

**定位路径**

```
ChatScreen（点击图片）→ ChatViewModel.attachImage
  → AvatarStorage.importFromUri → scaleDown(bitmap, 512)   ← 问题在此
```

**根因**：聊天图片复用了头像的 512px 压缩逻辑。

**修复**：抽出 `maxSize` 参数——头像 512px、聊天图 **1600px**；查看器由 `fillMaxWidth` 改为 `fillMaxSize`。

### 5.2 输入法唤不起来

**意图（整理）**

> 聊天页点击输入框无法唤起输入法，这是严重阻断问题，请定位根因。

**排查（用系统状态做裁判）**

```bash
adb shell dumpsys input_method | grep -E "mInputShown|mServedView|mCurMethodId"
# mInputShown=true
# mServedView=androidx.compose.ui.platform.AndroidComposeView
# mServedInputConnection=NullableInputConnectionWrapperApi34
```

系统明确表示「输入法已显示且已连接」→ App 请求链路正常。
临时切换系统键盘 → 键盘立刻弹出 → 判定为**设备输入法配置问题**（默认输入法被设为微信输入法的插件服务）。

**App 侧加固**：`windowSoftInputMode="adjustResize"` + 各页 `imePadding()`。

### 5.3 UI 质感偏弱

**意图（整理）**

> 当前界面偏「素」，卡片与顶栏缺少层次；希望在视觉上更有质感，并保持全局统一。

**处理**：先补玻璃卡片、渐变与阴影；随后发现真正的「玻璃」需要真实背景模糊，进入迭代六。

### 复盘

- **沿调用链回溯**：图片模糊不是渲染问题，而是上游压缩参数问题。
- **用系统状态区分责任边界**：`dumpsys` 一锤定音，避免在 App 侧做无效修复。
- **先可用，再好看**：视觉问题拆成「层次 → 统一 → 真实模糊」三步。

---

## 迭代六 · 液态玻璃与视觉体系

### 目标

从「功能可用」走到「视觉统一」：真实背景模糊、设计规范、包体控制。

### 意图（整理）

> 目标视觉是 iOS 那种液态玻璃：通透、磨砂、有真实背景模糊；同时保持 9 个页面风格一致。
> 右下角悬浮加号不美观，首页移除编辑角色入口。

### 关键问题 1：UI 不统一的根因

排查发现主题开启了 **Material You 动态取色**（`dynamicColor = true`），系统按壁纸生成一套配色，与品牌紫渐变同时存在 → 同一界面出现两套色彩体系。

**修复**：关闭动态取色；新建 `DesignSystem.kt` 固化圆角（10/14/18/22/28）与间距（4/8/12/16/20/24）。

### 关键问题 2：Haze 2.x 元数据不兼容

**报错**

```
Module was compiled with an incompatible version of Kotlin.
The binary version of its metadata is 2.4.0, expected version is 2.2.0.
```

**第一次尝试（方向错误）**：降级 Haze 到 1.7.3，能编译但只能使用旧 API，效果受限。

**转折**：用户提出「能否升级 Kotlin？」——随后评估升级路径：

1. 查 KSP 版本列表 → 最新 `2.3.12`，**没有 2.4 系列**；升到 Kotlin 2.4 会导致 Room 注解处理不可用；
2. 关键事实：**Kotlin 编译器版本 N 可读取 metadata 至 N+1**；
3. 结论：升到 **Kotlin 2.3.21**——既解锁 Haze 2.x，又保持 KSP 可用。

**升级后的新阻碍**

```
The 'org.jetbrains.kotlin.android' plugin is not compatible with AGP's 9.0 new DSL
(`android.newDsl=true` is enabled by default).
Solution: ... set `android.newDsl=false` in `gradle.properties`.
```

**修复**

```properties
android.builtInKotlin=false
android.newDsl=false
```

并应用独立 Kotlin 2.3.21 + KSP 2.3.12。

### 关键问题 3：Haze API 不确定

**意图（整理）**

> 不要猜 API 写法。直接反编译 jar，把 `hazeBlur` 的精确签名与相关包名查出来。

```bash
javap -classpath . dev.chrisbanes.haze.blur.HazeBlurKt
# public static final androidx.compose.ui.Modifier hazeBlur(
#     Modifier, HazeInput, HazeBlurStyle, HazePerformanceMode, boolean)

unzip -l haze-blur-materials-2.0.1.aar | grep HazeMaterials
# → dev/chrisbanes/haze/blur/materials/HazeMaterials.class
```

依据真实签名生成调用代码，一次编译通过。

### 关键问题 4：玻璃强度失衡

| 参数 | 调整前 | 调整后 |
| --- | --- | --- |
| `blurRadius` | 48dp | **22dp** |
| `noiseFactor` | 0.14 | **0.06** |
| 玻璃底色 alpha | 0.10 | **0.14** |

**教训**：玻璃强度必须与「背后内容的复杂度」匹配，不能一味调参。

### 包体优化

```
dex              64.5 MB
lib/x86_64       39.1 MB
lib/x86          39.0 MB
lib/arm64-v8a    32.8 MB
lib/armeabi-v7a  23.2 MB
assets           23.0 MB
```

**根因**：ONNX Runtime 自带四套 CPU 架构原生库。
**修复**：`splits.abi` 仅保留 ARM → **185MB 降至 67MB（arm64）/ 58MB（armv7）**。

### 一次真实失误

使用 `awk '!seen[$0]++'` 去重导入语句时，该命令会删除文件中所有重复行（包括合法代码行），导致 `CharacterListScreen.kt` 损坏。

**修复**：整文件重写。
**流程改进**：此类「全局去重」手法此后禁止用于源码文件。

### 复盘

- **同一类问题第二次出现时改变策略**：从「逐个降级依赖」升级为「评估并升级编译器」。
- **升级前先评估生态约束**：KSP 无 2.4 → 选择「编译器 2.3.x + 可读 metadata 2.4.0」的临界版本。
- **官方报错自带解法**：AGP 的报错直接给出了 `android.newDsl=false`。

---

## 迭代七 · 从功能到交付

### 7.1 文档、截图与仓库

**意图（整理）**

> 按交付要求梳理完整开发流程，只写真实发生的过程；产出 README、技术文档、使用说明书与 AI 记录，并用真机截图替换占位图。

**产出**

| 文档 | 结果 |
| --- | --- |
| README | 10 分钟复现指南（环境、构建、安装、测试、排错） |
| 技术文档 | 22 页；意图控制 / 架构 / 端侧记忆 / 提示词 / 可复现性 |
| 使用说明书 | 16 页；9 张真机截图 |
| 开发日志 | 五阶段产出 + 13 个真实问题 |
| AI 记录 | 协同 Prompt 链 + 对话节选 |
| 仓库 | Public；基础提交 12 个，后续按真实时间继续提交 |

**关键决策**：PDF 用 Markdown → HTML → Chrome Headless 生成；截图全部来自真机，不使用设计稿冒充。

### 7.2 未成年保护：从「告知」到「分级限制」

**第一版**：选择「恋人 / 自定义」时弹出免责声明。
**问题**：新建角色默认关系就是「恋人」，用户不点选项直接保存即可绕过。

**修复**：默认关系改为中性的「闺蜜」；只有主动选择亲密关系才触发声明。

**第二版**：用户指出「仅靠弹窗无法阻止未成年人使用」。
**方案**：年龄门 + 青少年模式——

| 机制 | 落地 |
| --- | --- |
| 年龄门 | 首次启动确认年龄段；未确认不进入主界面 |
| 青少年模式 | 隐藏「恋人」「自定义」；设置页显示标识 |
| 内容约束 | 系统提示词追加最高优先级保护条款 |
| 记忆约束 | 不记录涉及亲密关系的内容 |
| 能力边界 | 年龄为自声明；实名核验需服务端与合规资质 |

原有点击弹窗与对应截图一并移除；文档、截图、PDF 同步更新。

### 7.3 成品状态

| 维度 | 状态 |
| --- | --- |
| 应用 | 心屿 v1.0.0；`minSdk 26` / `compileSdk 37` |
| 角色 | 预置小屿 / 阿岸 / 林深 + 多角色自定义（性格/背景/说话风格/头像） |
| 对话 | DeepSeek SSE 流式；图片多模态；可停止、失败重试、清空会话 |
| 记忆 | LLM 抽取 + 端侧 512 维向量 + 本地加权召回 Top-6 |
| 合规 | 年龄门 + 青少年模式 + 提示词/记忆双重约束 |
| 演示数据 | 3 个角色 / 6 条记忆 / 7 条示例对话，可重置 |
| 质量 | Kotlin 0 警告；2 tests / 0 failures；arm64 APK 68MB |

---

## 迭代八 · 交付后修复

> 交付验收后，又在真机上发现 8 个问题。它们不改变产品形态，但直接影响「能不能放心交付」。

| # | 问题 | 修复 |
| --- | --- | --- |
| 1 | 冷启动时年龄门闪一下 | 区分「配置未加载 / 未确认」，加载完成前只渲染背景 |
| 2 | 编辑角色后列表顺序变化 | 更新时保留原 `createdAt` |
| 3 | 同毫秒消息排序可能错位 | `ORDER BY timestamp DESC, id DESC`；助手 id 取插入返回值 |
| 4 | SSE 完成回调可能重复结算 | `AtomicBoolean` 只允许结算一次 |
| 5 | ONNX 输入张量未释放 | `OnnxTensor.use { }` 显式关闭 |
| 6 | 「仅存本机」与云备份矛盾 | `allowBackup=false` |
| 7 | 聊天页附图弹窗文案错误 | 图片选择器支持标题，聊天页显示「发送图片」 |
| 8 | 记忆解析只看首尾方括号 | 实现 `removeThinkTags()` + `extractJSON()` |

**验证**

```
./gradlew compileDebugKotlin --offline --rerun-tasks
BUILD SUCCESSFUL

./gradlew connectedDebugAndroidTest
2 tests, 0 failures
```

真机复测：年龄门、编辑角色顺序、消息顺序、图片弹窗、联网流式回复、记忆抽取与召回均通过。

### 复盘

- **交付不是终点**：真正的问题往往在「装机后、联网后、反复操作后」才出现。
- **每个修复都要有可验证的判据**：数据库顺序、返回 id、单次结算、manifest 标志。
- **文档要与代码同时收口**：页数、隐私表述、构建约束、提交历史说明一并校准。

---

## 附录 · 开发期问题索引（13 个）

| # | 阶段 | 问题 | 定位与修复 |
| --- | --- | --- | --- |
| 1 | 骨架 | AGP 9 内置 Kotlin 与独立插件冲突 | 反编译 AGP jar 查选项；首轮移除独立插件，后期升级 Kotlin 后改回独立插件 + `builtInKotlin=false` |
| 2 | 骨架 | Maven 依赖 TLS 握手失败 | `curl` 验证四源均 200，判定瞬时抖动，重试通过 |
| 3 | 数据 | KSP 注入 sourceSets 被拒 | 官方报错给出的 `disallowKotlinSourceSets=false`（后期移除） |
| 4 | 数据 | 依赖元数据 2.4.0 高于编译器 2.2.0 | `dependencyInsight` 定位 Coil 3.6.3，降级到 3.4.0 |
| 5 | 体验 | 聊天图片点开模糊 | 沿调用链回溯，聊天图复用头像 512px 压缩；改为 1600px |
| 6 | 记忆 | 记忆措辞「用户……」过于生冷 | 重写抽取提示词为第一人称、禁止「用户」二字 |
| 7 | 记忆 | 多角色记忆是否串台 | 无法肉眼确认，编写 `MemoryIsolationTest` 固化 |
| 8 | 体验 | 应用内键盘唤不起来 | `dumpsys input_method` 判定设备输入法问题；App 侧补 `imePadding` |
| 9 | 视觉 | 玻璃强度失衡 | 真机截图对比，`blurRadius` 48→22dp、`noiseFactor` 0.14→0.06 |
| 10 | 视觉 | Haze 2.x 元数据不兼容 | 评估 KSP 生态后升级 Kotlin 2.3.21，并关闭 AGP 新 DSL |
| 11 | 视觉 | 去重命令误删源码 | `awk` 删除合法重复行；整文件重写，禁用该手法 |
| 12 | 视觉 | UI 不统一 | 发现 Material You 动态取色；关闭并建立设计规范 |
| 13 | 交付 | APK 体积 185MB | 解包统计四套 ABI 原生库；`splits.abi` 拆分到 67/58MB |

> 另有交付后修复 8 项与交付前收口 1 轮，见「迭代八」「迭代九」。

---

## 迭代九 · 交付前收口（10-26～10-29）

> 按交付要求收口为**单版本**：保留主包名 `com.isle.companion`、版本 `v1.0.0`。

| 改造 | 结果 |
| --- | --- |
| 构建收口 | 只保留 `assembleDebug` 一个构建；移除双构建配置 |
| 演示数据 | 首次启动写入 3 个角色、6 条记忆、7 条示例对话；设置页可一键重置 |
| 聊天可靠性 | 新增停止生成、失败重试、清空会话；错误不再写入助手消息 |
| 图片链路 | 先读尺寸再采样解码；文件名 UUID；删角色/清空会话清理聊天图；拍照缓存定期清理 |
| 设置能力 | 年龄分组重选、最大 tokens、测试连接/余额、演示数据重置 |
| 记忆质量 | 语义近似去重（余弦 ≥ 0.92 合并），减少近义重复 |
| 数据演进 | Room v2：`messages(characterId, timestamp)` 复合索引 + Migration(1→2) |
| 视觉 | 窗口背景对齐主题，新增 `values-night`，减少冷启动白闪 |

**验证**：`assembleDebug` 通过；演示数据 3/6/7；设置页测试连接返回余额；重置演示数据恢复 3/6/7；年龄分组可回到年龄门；覆盖安装验证过 v1→v2 迁移。

**收口结果**：单版本，`com.isle.companion`，v1.0.0，arm64 68MB。

---

## 协同方法总结

### 1. 先定契约，再生成代码

每次让 AI 生成前，先明确「分层边界 + 数据流 + 命名规范 + 验收标准」。

> **反例**：帮我写个聊天页面。
> **本项目写法**：写聊天页 ViewModel。输入 `characterId`；从 `AppContainer` 取仓储与客户端；发送流程是「落库用户消息 → 插占位助手消息 → 流式更新 → 完成后落库并异步抽取记忆」；对外暴露 `StateFlow`；验收标准是编译通过。

### 2. 用编译器和测试当裁判

| 手段 | 用途 | 实例 |
| --- | --- | --- |
| `compileDebugKotlin --rerun-tasks` | 强制全量编译，杜绝增量缓存假通过 | 每次改动后必跑 |
| `connectedDebugAndroidTest` | 仪器化测试 | 向量区分度、记忆隔离 |
| `dependencyInsight` | 定位依赖冲突真实来源 | 查出 Coil 强约束 stdlib |
| `dumpsys input_method` | 读取系统真实状态 | 键盘问题责任边界 |
| `javap` 反编译 jar | 确认 API 方法签名 | Haze `hazeBlur` 签名 |
| 解包统计 | 定位包体膨胀来源 | 四套 ABI 原生库 |

### 3. 报错即上下文（闭环排错法）

回灌给 AI 的应是完整证据链，而不是现象描述：

```
报错原文（完整栈 + 关键行）
+ 依赖树（谁引入了冲突版本）
+ 环境事实（Gradle / JDK / AGP / Kotlin 版本）
+ 已尝试的修复及结果
```

### 4. 抽象先行，预留降级路径

高风险能力（端侧向量、网络）先定义接口与失败回退，保证任何一步失败都不会让应用不可用。

### 5. 同类问题第二次出现时，改变策略

依赖元数据不兼容出现两次：第一次「逐个降级库」，第二次改为**评估并升级编译器**，根治整类问题。

### 6. 官方报错往往自带解法

AGP 的两处报错都直接给出了配置项（`android.disallowKotlinSourceSets=false`（后期移除）、`android.newDsl=false`）。
**先完整读完报错再动手**，比搜索更快。

## 验证与数据

| 指标 | 结果 |
| --- | --- |
| 开发期真实问题 | 13 个（全部定位到根因并修复） |
| 交付后修复 | 8 个 + 交付前收口 1 轮 |
| 仪器化测试 | 2 tests / 0 failures |
| 端侧向量 | 512 维；相近句 0.85 / 无关句 0.39 |
| 记忆召回 | 语义 0.8 + 关键词 0.2 + 重要度 0.1；Top-6 |
| 包体 | 185MB → 68MB（arm64）/ 58MB（armv7） |
| 全量编译 | BUILD SUCCESSFUL；Kotlin 0 警告 |
| 提交 | 40+ 次；10-16 按模块整理 12 个基础提交 |

---

## 结语

这个项目不是「让 AI 一次写对」的演示，而是一份**把失败也纳入流程**的记录：

- 报错不是障碍，是定位的入口；
- 测试不是装饰，是把「看不见的正确性」固定下来；
- 文档不是事后补的说明，而是与代码同步收口的交付物。

从一句「可以重构为原生 Android 吗」，到一款能记住你、能通过真机验收的应用——
真正可复用的不是某一版代码，而是这套**定义约束 → 生成 → 验证 → 回灌 → 修正**的协作方式。
