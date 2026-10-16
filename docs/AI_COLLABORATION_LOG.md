# 心屿（Isle）· AI 协同过程记录（Prompt 链还原）

> **说明**：本文档基于本项目从 0 到 1 开发过程中真实发生的 AI 协作会话整理而成，
> 按时间顺序还原**关键 Prompt 链**：用户如何表达意图 → AI 首轮方案 → 报错 → 用户如何引导 AI 定位与修复。
> 文中 Prompt 为要点还原（非逐字稿），所有报错文本与验证数据均来自真实执行输出。
> 原始对话截图/录屏作为独立材料一并提供。

---

## 迭代一 · 从零搭建可编译的工程骨架

### 背景
项目从零开始，目标是产出一款原生 Android 应用。第一步必须先让工程能编译、能安装、能启动。

### 关键 Prompt 链

**Prompt 1（意图表达）**
> 帮我建一个新的安卓工程，应用名叫「心屿」，包名 `com.isle.companion`，
> 用较新稳定版的 Kotlin + Compose 组合，先只搭一个能编译、能跑起来的骨架。

**AI 首轮方案**：生成 Gradle 配置、Manifest、MainActivity、Compose 主题与占位首页，并执行构建。

### 报错与引导过程

**报错 1**
```
Cannot add extension with name 'kotlin', as there is an extension already registered with that name.
```

**引导 Prompt（关键）**
> 这是 AGP 9 内置 Kotlin 支持与独立 Kotlin 插件冲突。请去查证官方迁移方式，不要猜测。

**AI 定位与修复**：反编译 AGP jar 发现 `BUILT_IN_KOTLIN` 选项（`android.builtInKotlin`），
确认 AGP 9.4.1 已内置 Kotlin 编译器；移除独立 `org.jetbrains.kotlin.android` 插件。

**报错 2**
```
Starting in Kotlin 2.0, the Compose Compiler Gradle plugin is required when compose is enabled.
```

**修复**：仅保留 `org.jetbrains.kotlin.plugin.compose`。

### 验证结果
```
./gradlew assembleDebug
BUILD SUCCESSFUL in 33s
产物：app-debug.apk (11.6 MB)
```

---

## 迭代二 · 端侧语义记忆引擎（本项目核心差异化）

### 背景
产品核心是"AI 伴侣能记住你"。市面方案普遍依赖云端向量库，本项目要求**完全离线**。

### 关键 Prompt 链

**Prompt 2（意图 + 约束）**
> 记忆检索要用**端侧中文 embedding**，模型放 assets 里随包分发。
> 先定义好抽象接口和降级路径：如果模型加载失败，应用其他功能不能受影响。

**AI 首轮方案**：定义 `EmbeddingProvider` 接口 → `OnnxEmbeddingProvider` 实现 →
自实现 `BertTokenizer`（WordPiece）→ `TextEmbedder`（ONNX Runtime，CLS 池化 + L2 归一化）→
`VectorMath`（余弦 + 字节序列化）→ `MemoryRepository`（语义 0.8 + 关键词 0.2 + 重要度 0.1 加权召回）。

### 报错与引导过程

**报错 3（依赖元数据不兼容）**
```
e: Class 'kotlin.reflect.KClass' was compiled with an incompatible version of Kotlin.
   The actual metadata version is 2.4.0, but the compiler version 2.2.0 can read versions up to 2.3.0.
```

**引导 Prompt（关键）**
> 不要猜。用 `dependencyInsight` 查出是谁把 kotlin-stdlib 拉到 2.4.10 的，再决定是降库还是升编译器。

**AI 定位**：执行
```bash
./gradlew :app:dependencyInsight --dependency org.jetbrains.kotlin:kotlin-stdlib --configuration debugCompileClasspath
```
输出显示 **Coil 3.6.3 通过 constraint 强制 stdlib 2.4.10**。
逐版本核对 POM 后确认 Coil 3.4.0 依赖 stdlib 2.3.10（2.2.0 编译器可读）→ 降级至 3.4.0。

**报错 4（KSP 注入源码被 AGP 拒绝）**
```
Using kotlin.sourceSets DSL to add Kotlin sources is not allowed with built-in Kotlin.
Solution: Use android.sourceSets DSL instead.
To suppress this error, set android.disallowKotlinSourceSets=false in gradle.properties.
```

**修复**：按官方提示加入 `android.disallowKotlinSourceSets=false`。

### 验证结果（关键：不靠"看起来对"，靠测试）
编写仪器化测试并在真机执行：

```
./gradlew connectedDebugAndroidTest
2 tests, 0 failures

[EmbedderTest] sim(爬山, 登山) = 0.8496689
[EmbedderTest] sim(爬山, 股票) = 0.38915947
```

语义相近句相似度 **0.85**，无关句 **0.39**，区分度显著；向量 **512 维**，3 次推理约 0.4s。

端到端实测（真机对话后自动生成的记忆）：
```
记得七月二十号是你的生日，是夏天里海边最舒服的时候
知道你周末喜欢去爬山，身上总带着一股很清爽的劲儿
```

---

## 迭代三 · 依赖冲突的根治：升级 Kotlin 解锁现代库生态

### 背景
视觉阶段需要接入 **Haze** 实现 iOS 那种真实背景模糊（液态玻璃）。

### 报错与引导过程

**报错 5**
```
e: Module was compiled with an incompatible version of Kotlin.
   The binary version of its metadata is 2.4.0, expected version is 2.2.0.
```

**第一次尝试（错误方向）**：降级 Haze 到 1.7.3 —— 能编译，但用的是旧 API，效果受限。

**用户的关键提问（转折点）**
> 可以升级 Kotlin 吗？

**引导 Prompt（关键）**
> 先查清升级路径上的依赖：KSP 是否支持 Kotlin 2.4？如果不行，选一个"编译器版本够新、
> 生态仍完整"的版本。

**AI 分析与决策**：
1. 查 KSP 版本列表 → 最新为 `2.3.12`，**没有 2.4 系列** → Kotlin 2.4 会导致 Room 注解处理不可用
2. 但编译器版本 N 可读取 metadata 至 N+1 → **Kotlin 2.3.x 可读 metadata 2.4.0**
3. 结论：升到 **Kotlin 2.3.21**，既解锁 Haze 2.x，又保持 KSP 可用

**报错 6（升级后的新阻碍）**
```
⛔ Failed to apply plugin 'org.jetbrains.kotlin.android'
The 'org.jetbrains.kotlin.android' plugin is not compatible with AGP's 9.0 new DSL
(`android.newDsl=true` is enabled by default).
Solution: ... or set `android.newDsl=false` in `gradle.properties` to temporarily bypass this issue.
```

**修复**：按官方提示在 `gradle.properties` 中同时设置
```properties
android.builtInKotlin=false
android.newDsl=false
```
并应用独立 Kotlin 插件 2.3.21 + KSP 2.3.12。

**报错 7（Haze 2.x 的 API 不确定）**

**引导 Prompt（关键）**
> 不要猜 API 写法。直接反编译它的 jar，把 `hazeBlur` 的精确签名和 `HazeInput` 的包名查出来。

**AI 执行**：
```bash
javap -classpath . dev.chrisbanes.haze.blur.HazeBlurKt
# → hazeBlur(Modifier, HazeInput, HazeBlurStyle, HazePerformanceMode, boolean)

unzip -l haze-blur-materials-2.0.1.aar | grep HazeMaterials
# → dev/chrisbanes/haze/blur/materials/HazeMaterials.class
```
依据真实签名生成调用代码，**一次编译通过**。

### 验证结果
```
./gradlew :app:compileDebugKotlin --rerun-tasks
BUILD SUCCESSFUL，0 警告
```
真机生效：内容从顶栏下方滚过时实时模糊（backdrop blur）。

---

## 迭代四 · 从"功能可用"到"体验可信"（体验类问题引导）

### 4.1 记忆措辞过于生冷

**用户 Prompt（原始表述）**
> 记忆里不要是用户什么什么，太生冷了，要心与心

**AI 分析与修复**：定位到记忆抽取的提示词使用了中性第三人称。改写约束：
```
1. 用「你」称呼对方，写成一句温柔自然的话，像是你在心里默默记住的
2. 绝对不能出现「用户」这两个字，也不要写成冷冰冰的资料条目
3. 已知的信息不要重复记录，只记这次新出现的内容
```
**验证**：真机重新抽取后输出——
`记得七月二十号是你的生日…`、`知道你周末喜欢去爬山…`

### 4.2 多角色记忆是否串台

**用户 Prompt**
> 多角色不要记忆错乱

**AI 应对**：不靠"看起来没问题"，编写仪器化测试 `MemoryIsolationTest`：
创建两个角色各写一条记忆 → 断言互不可见 → 删除其一 → 断言级联清除且不影响另一个。
**结果**：`2 tests, 0 failures`。

### 4.3 图片点开模糊

**用户反馈**
> 发出去的图片没法看大图 / 图片打开看不清楚

**AI 定位**：沿调用链回溯，发现聊天图片**复用了头像压缩逻辑（512px）**。
**修复**：为存储逻辑抽出 `maxSize` 参数——头像 512px、聊天图 1600px。

### 4.4 "UI 没有全局统一"

**用户反馈**
> 还是不好看啊，而且 UI 没有全局统一

**AI 定位（根因）**：主题开启了 **Material You 动态取色**，系统按壁纸生成配色，与品牌紫互相干扰。
**修复**：关闭动态取色 + 建立 `DesignSystem.kt`（圆角/间距体系）+ 统一 9 个页面的顶栏与卡片。

### 4.5 键盘唤不起来（区分"App 问题"与"设备问题"）

**用户反馈**
> 有个严重问题点说点什么拉不起来输入法！！！

**AI 排查（用系统状态做裁判）**：
```bash
adb shell dumpsys input_method | grep -E "mInputShown|mServedView"
# mInputShown=true
# mServedView=AndroidComposeView   ← App 已正确请求并连接输入法
```
进一步临时切换系统键盘 → **键盘立刻弹出** → 判定为**设备输入法配置问题**（默认输入法被设成了微信输入法的插件服务）。
App 侧仍做加固：`windowSoftInputMode="adjustResize"` + 各页 `imePadding()`。

---

## 协同方法总结

| 原则 | 具体做法 |
| --- | --- |
| **先定契约，再要代码** | 每次生成前先说清：分层边界 / 数据流 / 命名规范 / 验收标准 |
| **让编译器与测试当裁判** | 全量编译、仪器化测试、`dependencyInsight`、`dumpsys`、`javap` 反编译 |
| **报错即上下文** | 把完整报错栈 + 依赖树 + 环境事实一起回灌，而非只描述现象 |
| **抽象先行，预留降级** | 高风险能力先定接口（`EmbeddingProvider`）与失败回退路径 |
| **先定位根因，再动手改** | 不试错式修改；每次修改都能说明"为什么是这里" |
