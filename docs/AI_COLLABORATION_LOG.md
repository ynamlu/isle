# 心屿（Isle）· AI 协同过程记录（Prompt 链还原）

> **说明**
> 本文档基于本项目从 0 到 1 开发过程中真实发生的 AI 协作会话整理而成，
> 按时间顺序还原**关键 Prompt 链**：意图表达 → AI 首轮方案 → 报错 → 引导 AI 定位 → 修复 → 验证。
>
> - 文中 Prompt 为**要点还原**（非逐字稿），所有报错文本、命令输出与验证数据**均来自真实执行结果**
> - 全量原始记录见 `docs/AI_CONVERSATION_EXPORT.md`（由会话数据库自动导出）

---

## 目录

- [协同概览](#协同概览)
- [迭代一 · 从零搭建可编译的工程骨架](#迭代一--从零搭建可编译的工程骨架)
- [迭代二 · 数据层与角色系统](#迭代二--数据层与角色系统)
- [迭代三 · 端侧语义记忆引擎](#迭代三--端侧语义记忆引擎核心差异化)
- [迭代四 · 升级 Kotlin 解锁液态玻璃](#迭代四--升级-kotlin-解锁液态玻璃)
- [迭代五 · 六类体验问题的根因定位](#迭代五--六类体验问题的根因定位)
- [迭代六 · 视觉体系与交付优化](#迭代六--视觉体系与交付优化)
- [协同方法总结](#协同方法总结)
- [效率数据](#效率数据)

---

## 协同概览

| 维度 | 数据 |
| --- | --- |
| 协作形态 | 全程 Vibe Coding（意图驱动 + AI 生成 + 人类约束与验证） |
| 关键决策点 | 8 个（平台范围 / 技术栈 / 记忆方案 / 视觉体系 / 依赖升级策略等） |
| 真实问题 | 13 个（含 4 个依赖版本天花板问题、3 个体验类根因问题） |
| 验证手段 | 全量编译、仪器化测试、依赖树分析、系统状态采集、字节码反编译、真机截图 |
| 交付物 | 可运行 APK + 40+ 次提交（10-16 按模块整理 12 个基础提交）+ 4 份文档 |

**核心协作特征**：本项目不是"让 AI 写代码然后复制粘贴"，而是
**用自然语言定义约束 → 让 AI 生成 → 用可验证手段裁判 → 把失败证据链回灌给 AI**的闭环。

---

## 迭代一 · 从零搭建可编译的工程骨架

### 背景
项目从零开始。第一步必须先让工程能编译、能安装、能启动——否则后续所有工作都没有落脚点。

### Prompt 链

**Prompt 1（意图表达 + 约束）**
> 帮我建一个新的安卓工程，应用名叫「心屿」，包名 `com.isle.companion`，
> 用较新稳定版的 Kotlin + Compose 组合，先只搭一个能编译、能跑起来的骨架。

**AI 首轮方案**：生成 `settings.gradle.kts`、根 `build.gradle.kts`、版本目录 `libs.versions.toml`、
`gradle.properties`、Manifest、MainActivity、Compose 主题与占位首页，并执行构建。

### 报错与引导

**报错 1**
```
Cannot add extension with name 'kotlin', as there is an extension already registered with that name.
```

**Prompt 2（引导方向，而非直接要答案）**
> 这是 AGP 9 内置 Kotlin 支持与独立 Kotlin 插件冲突。请去查证官方迁移方式，不要猜测。

**AI 定位过程**：
1. 反编译 AGP jar 的选项类，发现 `BooleanOption.BUILT_IN_KOTLIN`（对应属性 `android.builtInKotlin`）
2. 确认 AGP 9.4.1 **已内置 Kotlin 编译器并注册了 `kotlin` 扩展**
3. 移除独立的 `org.jetbrains.kotlin.android` 插件

**报错 2（移除插件后的连带问题）**
```
Starting in Kotlin 2.0, the Compose Compiler Gradle plugin is required when compose is enabled.
```

**修复**：仅补回 `org.jetbrains.kotlin.plugin.compose`。

### 验证
```
./gradlew assembleDebug
BUILD SUCCESSFUL in 33s
产物：app-debug.apk（11.6 MB）
```

### 本轮的协同要点
- **不猜 API**：让 AI 去读依赖的字节码而不是"回忆文档"
- **连带问题预判**：移除插件前先确认 Compose 编译是否依赖它

---

## 迭代二 · 数据层与角色系统

### 背景
需要把"角色 → 会话 → 记忆"的数据闭环打通，并让 AI 真的能回复。

### Prompt 链

**Prompt 3（结构化契约）**
> 实现本地数据层：三张表——角色、消息、记忆。
> 记忆表要预留 `embedding` 二进制列（后续接端侧向量）。
> 角色删除时其会话与记忆必须级联清除。
> 用 Room + KSP，DAO 全部返回 `Flow` 或 `suspend`。

**Prompt 4（网络层契约）**
> 写 DeepSeek 客户端。要求：
> 1. 模型地址要能在应用内改，所以 Retrofit 用 `@Url` 而不是固定 baseUrl
> 2. 必须支持流式（SSE）且能被主动取消
> 3. 要支持发送图片（多模态），图片以 base64 内联
> 4. DTO 要兼容 `reasoning_content` 字段

### 报错与引导

**报错 3（KSP 注入源码被拒）**
```
Using kotlin.sourceSets DSL to add Kotlin sources is not allowed with built-in Kotlin.
Kotlin source set 'debug' contains: [.../build/generated/ksp/debug/kotlin, ...]
Solution: Use android.sourceSets DSL instead.
To suppress this error, set android.disallowKotlinSourceSets=false in gradle.properties.
```

**AI 处理**：读取官方报错中的 Solution，加入
`android.disallowKotlinSourceSets=false`（AGP 9 内置 Kotlin 默认禁止外部注入 sourceSets）。
> 该开关只在“内置 Kotlin”方案下需要；后来升级 Kotlin 2.3.21、改用独立插件后已移除。

**报错 4（依赖元数据版本不兼容）**
```
e: Class 'kotlin.reflect.KClass' was compiled with an incompatible version of Kotlin.
   The actual metadata version is 2.4.0, but the compiler version 2.2.0 can read versions up to 2.3.0.
The class is loaded from .../kotlin-stdlib-2.4.10.jar!/kotlin/reflect/KClass.class
```

**Prompt 5（关键引导：先查证据，再改代码）**
> 不要猜。用 `dependencyInsight` 查出是谁把 kotlin-stdlib 拉到 2.4.10 的，再决定是降库还是升编译器。

**AI 执行与结论**：
```bash
./gradlew :app:dependencyInsight \
  --dependency org.jetbrains.kotlin:kotlin-stdlib \
  --configuration debugCompileClasspath
```
输出关键片段：
```
org.jetbrains.kotlin:kotlin-stdlib:2.4.10
  Selection reasons:
    - By constraint
    - By conflict resolution: between versions 2.4.10, 2.3.20, 2.2.21, 2.2.10, ...
+--- io.coil-kt.coil3:coil-android:3.6.3
```
→ 定位到 **Coil 3.6.3 通过 constraint 强制 stdlib 2.4.10**。
逐版本核对 POM 后确认 Coil **3.4.0** 依赖 stdlib 2.3.10 → 降级。

### 验证（真机）
| 项目 | 结果 |
| --- | --- |
| 安装启动 | 正常，无崩溃 |
| 角色人设 | 回复符合设定（闺蜜口吻、使用预设口癖「嗯嗯」「其实呀」） |
| 开场白 | 首次进入聊天页自动发送 |
| **图片多模态** | 发送注册页截图后，AI 准确描述了页面内容（用户名、密码框、性别选项、按钮、底部提示） |

### 本轮的协同要点
- **报错即上下文**：把 `dependencyInsight` 的完整输出回灌，而不是只贴报错行
- **降级是临时方案**：本轮选择降库；该问题在迭代四被**根治**（升级编译器）

---

## 迭代三 · 端侧语义记忆引擎（核心差异化）

### 背景
产品核心是"AI 伴侣能记住你"。同类方案普遍依赖云端向量库，本项目要求**完全离线**。

### Prompt 链

**Prompt 6（意图 + 架构约束 + 降级要求）**
> 记忆检索要用**端侧中文 embedding**，模型放 assets 里随包分发。
> 先定义好抽象接口和降级路径：如果模型加载失败，应用其他功能不能受影响。
> 分词器自己实现，不要引入重型 NLP 依赖。

**AI 首轮方案**：
```
EmbeddingProvider（接口）
  └─ OnnxEmbeddingProvider（实现，加载失败返回 null）

BertTokenizer      —— 自实现 WordPiece（CJK 逐字切分 + 贪心最长匹配）
TextEmbedder       —— ONNX Runtime，CLS 池化 + L2 归一化 → 512 维
VectorMath         —— 余弦相似度 + FloatArray ↔ ByteArray
MemoryRepository   —— 写入补向量；召回 语义0.8 + 关键词0.2 + 重要度0.1
MemoryExtractor    —— LLM 抽取结构化记忆（JSON）
```

### 关键问题：记忆措辞过于生冷

**用户反馈（原文）**
> 记忆里不要是用户什么什么，太生冷了，要心与心

**AI 定位**：抽取提示词中要求"使用第三人称「用户」"，导致输出如「用户喜欢吃东西。」

**Prompt 7（改写约束）**
> 改抽取提示词：用第一人称、温柔口吻，禁止出现"用户"二字，
> 并加上"已知信息不重复记录"的规则。

**修复后的抽取约束**：
```
1. 用「你」称呼对方，写成一句温柔自然的话，像是你在心里默默记住的
2. 绝对不能出现「用户」这两个字，也不要写成冷冰冰的资料条目
3. 已知的信息不要重复记录，只记这次新出现的内容
4. 每条独立完整，能脱离上下文理解
5. 只记明确出现的信息，不要推测
```

**验证输出**：
```
记得七月二十号是你的生日，是夏天里海边最舒服的时候
知道你周末喜欢去爬山，身上总带着一股很清爽的劲儿
```

### 关键问题：多角色记忆是否串台

**用户反馈（原文）**
> 多角色不要记忆错乱

**Prompt 8（把"不可见"变成"可验证"）**
> 这类问题不能靠肉眼确认。写一个仪器化测试：两个角色各写一条记忆，
> 断言互不可见；删除其一，断言级联清除且不影响另一个。

**AI 产出**：`MemoryIsolationTest`（4 步断言）

### 验证（真实测试数据）
```
./gradlew connectedDebugAndroidTest
→ 2 tests, 0 failures

[EmbedderTest] sim(爬山, 登山) = 0.8496689
[EmbedderTest] sim(爬山, 股票) = 0.38915947
```
语义相近句 **0.85**，无关句 **0.39**；512 维，3 次推理约 0.4s（真机）。

### 本轮的协同要点
- **抽象先行**：先定义接口与失败回退，再替换实现
- **不可见的正确性 → 测试固化**：记忆隔离这种"看不见"的约束必须写成断言
- **情感需求也是需求**：措辞温度通过提示词工程直接解决

---

## 迭代四 · 升级 Kotlin 解锁液态玻璃

### 背景
视觉阶段需要接入 **Haze** 实现 iOS 那种真实背景模糊（backdrop blur / 液态玻璃）。

### 报错与引导

**报错 5**
```
e: Module was compiled with an incompatible version of Kotlin.
   The binary version of its metadata is 2.4.0, expected version is 2.2.0.
```
（Haze 2.0.1 的 `haze-api.jar`、`haze-blur-api.jar`、`haze-materials-api.jar` 均报此错）

**第一次尝试（方向错误）**
降级 Haze 到 1.7.3 —— 能编译，但只能使用旧 API，视觉能力受限。

**用户的转折性提问**
> 可以升级 Kotlin 吗？

**Prompt 9（关键引导：先评估生态，再定版本）**
> 先查清升级路径上的依赖：KSP 是否支持 Kotlin 2.4？
> 如果不行，选一个"编译器版本够新、生态仍完整"的版本。

**AI 分析与决策链**：
1. 查 KSP 版本列表 → 最新为 `2.3.12`，**没有 2.4 系列**
   → 若升到 Kotlin 2.4，Room 注解处理将不可用 ❌
2. 关键事实：**Kotlin 编译器版本 N 可读取 metadata 至 N+1**
   → **Kotlin 2.3.x 恰好可读 metadata 2.4.0** ✅
3. 结论：升到 **Kotlin 2.3.21**——既解锁 Haze 2.x，又保持 KSP 可用

**报错 6（升级后的新阻碍）**
```
⛔ Failed to apply plugin 'org.jetbrains.kotlin.android'
   The 'org.jetbrains.kotlin.android' plugin is not compatible with AGP's 9.0 new DSL
   (`android.newDsl=true` is enabled by default).
   Solution: ... or set `android.newDsl=false` in `gradle.properties` to temporarily bypass this issue.
```

**修复**：在 `gradle.properties` 中同时设置
```properties
android.builtInKotlin=false   # 关闭 AGP 内置 Kotlin（固定 2.2.0）
android.newDsl=false          # 关闭 AGP 9 新 DSL，允许独立 Kotlin 插件
```
并应用独立 Kotlin 插件 2.3.21 + KSP 2.3.12。

**报错 7（Haze 2.x 的 API 不确定）**

**Prompt 10（不猜 API，直接查字节码）**
> 不要猜 API 写法。直接反编译它的 jar，把 `hazeBlur` 的精确签名和
> `HazeInput`、`HazeMaterials` 的包名查出来。

**AI 执行**：
```bash
javap -classpath . dev.chrisbanes.haze.blur.HazeBlurKt
# public static final androidx.compose.ui.Modifier hazeBlur(
#     Modifier, HazeInput, HazeBlurStyle, HazePerformanceMode, boolean)

unzip -l haze-blur-materials-2.0.1.aar | grep HazeMaterials
# → dev/chrisbanes/haze/blur/materials/HazeMaterials.class
```
依据真实签名生成调用代码，**一次编译通过**。

### 验证
```
./gradlew :app:compileDebugKotlin --rerun-tasks
BUILD SUCCESSFUL in 2s（Kotlin 0 警告）
```
真机生效：内容从顶栏下方滚过时实时模糊。

### 本轮的协同要点
- **从"逐个降级依赖"升级为"根治版本天花板"**：同一类问题第二次出现时改变策略
- **升级前先评估生态约束**（KSP 无 2.4 → 选择临界版本）
- **官方报错自带解法**：AGP 的报错信息直接给出了 `android.newDsl=false`

---

## 迭代五 · 六类体验问题的根因定位

### 5.1 图片点开模糊

**用户反馈**
> 发出去的图片没法看大图 / 图片打开看不清楚

**AI 定位路径**：沿调用链回溯
```
ChatScreen（点击图片）→ ChatViewModel.attachImage
  → AvatarStorage.importFromUri → scaleDown(bitmap, 512)   ← 问题在此
```
**根因**：聊天图片**复用了头像的压缩逻辑（512px 上限）**。

**修复**：为存储逻辑抽出 `maxSize` 参数——头像 512px、**聊天图 1600px**；
大图查看器容器由 `fillMaxWidth` 改为 `fillMaxSize`。

### 5.2 "UI 没有全局统一"

**用户反馈**
> 还是不好看啊，而且 UI 没有全局统一

**AI 定位**：排查主题配置，发现 **Material You 动态取色**（`dynamicColor = true`）
让系统按壁纸生成一套配色，与品牌紫渐变**同时存在** → 同一界面出现两套色彩体系。

**修复**：
1. 关闭动态取色，改为完整定义的品牌色板（亮/暗双套）
2. 新建 `DesignSystem.kt` 固化圆角体系与间距体系
3. 统一 9 个页面的顶栏与卡片样式

### 5.3 应用内键盘唤不起来

**用户反馈**
> 有个严重问题点说点什么拉不起来输入法！！！

**Prompt 11（用系统状态做裁判，而非猜测）**
> 先采集系统层面的输入法状态，确认到底是 App 没请求，还是系统没显示。

**AI 排查**：
```bash
adb shell dumpsys input_method | grep -E "mInputShown|mServedView|mCurMethodId"
# mInputShown=true
# mServedView=androidx.compose.ui.platform.AndroidComposeView
# mServedInputConnection=NullableInputConnectionWrapperApi34
```
系统明确表示"输入法已显示且已连接" → **App 侧请求链路正常**。
进一步临时切换系统键盘 → **键盘立刻弹出** → 判定为**设备输入法配置问题**
（该机默认输入法被设成了微信输入法的插件服务）。

**App 侧仍做加固**：`windowSoftInputMode="adjustResize"` + 各页 `imePadding()`。

### 5.4 记忆措辞生冷
（见迭代三，此处不赘述）

### 5.5 记忆隔离
（见迭代三，此处不赘述）

### 5.6 首页顶部模糊过重

**用户反馈**
> 感觉首页上面特别模糊

**AI 定位**：首页大面积高饱和渐变卡滚入顶栏后，被 48dp 强模糊糊成一大片色雾。

**修复（参数收敛）**：

| 参数 | 调整前 | 调整后 |
| --- | --- | --- |
| `blurRadius` | 48dp | **22dp** |
| `noiseFactor` | 0.14 | **0.06** |
| 玻璃底色 alpha | 0.10 | **0.14** |

**教训**：玻璃强度必须与"背后内容的复杂度"匹配，不能一味调参。

### 本轮的协同要点
- **沿调用链回溯定位**：图片模糊不是"渲染问题"，而是"上游压缩参数问题"
- **用系统状态区分责任边界**：`dumpsys` 一锤定音，避免在 App 侧做无效修复
- **用户反馈 → 根因 → 验证** 三段式闭环

---

## 迭代六 · 视觉体系与交付优化

### 6.1 液态玻璃从"半透明"到"真模糊"

**用户反馈（原文）**
> 就是 iOS 那种液态玻璃质感，透透的

**AI 决策**：单纯的半透明白色不是玻璃。需要 **backdrop blur（真实背景模糊）**。

**Prompt 12**
> 接入能实现真实背景模糊的库，先确认 API 用法（不要猜），
> 并说明它在我们这个技术栈下的兼容性风险。

**AI 执行链**：
1. 选定 **Haze 2.0.1**
2. 读取官方文档确认基础用法：`hazeSource()` + `hazeBlur(input, style)`
3. 遇到元数据不兼容 → 触发迭代四的 Kotlin 升级
4. 反编译 jar 确认精确签名与包名
5. 建立 `LiquidGlass.kt` 抽象出三个 API：
   `rememberLiquidGlassState()` / `liquidGlassSource()` / `liquidGlass()`
6. 应用于 9 个页面

### 6.2 APK 体积优化

**AI 定位**：解包统计各目录占用
```
dex              64.5 MB
lib/x86_64       39.1 MB
lib/x86          39.0 MB
lib/arm64-v8a    32.8 MB
lib/armeabi-v7a  23.2 MB
assets           23.0 MB     ← 端侧模型
```
**根因**：ONNX Runtime 自带**四套 CPU 架构**的原生库。

**修复**：启用 `splits.abi`，仅保留 ARM 架构并拆分产物
→ **185MB 降至 67MB（arm64）/ 58MB（armv7）**。

### 6.3 代码与依赖清理

**AI 执行**：扫描并移除
- 死代码：`EmbeddingProvider.NoOp`、`ContentFactory.emptyObject()`、
  `SettingsRepository.updateMaxTokens`、`AvatarStorage.file()`、
  `ChatRepository.observe()/clear()` 等
- 未使用依赖：`okhttp-logging`、`ui-tooling-preview`、`room-ktx`
- 版本目录中未使用的插件别名

**验证**：`compileDebugKotlin --rerun-tasks` → **BUILD SUCCESSFUL，Kotlin 0 警告**（Gradle 另有 2 条 AGP 9 迁移提示）

### 本轮的一次失误（如实记录）

**问题**：使用 `awk '!seen[$0]++'` 去重导入语句时，该命令会删除文件中**所有**重复行
（包括合法的重复代码行），导致 `CharacterListScreen.kt` 结构损坏、编译报错。

**修复**：整文件重写恢复。

**流程改进**：此类"全局去重"手法此后禁止用于源码文件。

---

## 协同方法总结

### 1. 先定契约，再生成代码

每次让 AI 生成前，先用自然语言明确"分层边界 + 数据流 + 命名规范 + 验收标准"。

**反例**：「帮我写个聊天页面」
**本项目写法**：

> 写聊天页的 ViewModel。输入是 `characterId`；它从 `AppContainer` 取
> `chatRepository` / `memoryRepository` / `deepSeekClient`；发送流程是：
> 先落库用户消息 → 再插占位助手消息 → 流式更新 → 完成后落库并异步抽取记忆；
> 对外暴露 `StateFlow`；验收标准是编译通过。

### 2. 用编译器和测试当裁判

| 手段 | 用途 | 实例 |
| --- | --- | --- |
| `compileDebugKotlin --rerun-tasks` | 强制全量编译，杜绝增量缓存假通过 | 每次改动后必跑 |
| `connectedDebugAndroidTest` | 仪器化测试 | 向量区分度、记忆隔离 |
| `dependencyInsight` | 定位依赖冲突真实来源 | 查出 Coil 强约束 stdlib |
| `dumpsys input_method` | 读取系统真实状态 | 键盘问题责任边界判定 |
| `javap` 反编译 jar | 确认 API 方法签名 | 取 Haze `hazeBlur` 签名 |
| 解包统计 | 定位包体膨胀来源 | 发现四套 ABI 原生库 |

### 3. 报错即上下文（闭环排错法）

回灌给 AI 的应是**完整证据链**，而不是现象描述：
```
报错原文（完整栈 + 关键行）
+ 依赖树（谁引入了冲突版本）
+ 环境事实（Gradle / JDK / AGP / Kotlin 版本）
+ 已尝试的修复及结果
```

### 4. 抽象先行，预留降级路径

高风险能力（端侧向量、网络）先定义接口与失败回退，保证任何一步失败都不会让应用不可用。

### 5. 同类问题第二次出现时，改变策略

依赖元数据不兼容出现两次：第一次"逐个降级库"，第二次改为**评估并升级编译器根治整类问题**。

### 6. 官方报错往往自带解法

AGP 的两处报错都直接给出了配置项（`android.disallowKotlinSourceSets=false`（后期移除）、
`android.newDsl=false`）。**先完整读完报错再动手**，比搜索更快。

---

## 效率数据

| 指标 | 数值 |
| --- | --- |
| 关键 Prompt 链 | 12 条（本文档） |
| 全量对话 | 见 `docs/AI_CONVERSATION_EXPORT.md` |
| 真实问题 | 13 个（全部定位到根因并修复） |
| 仪器化测试 | 2 个（共 2 tests, 0 failures） |
| 端侧向量实测 | 相近句 0.85 / 无关句 0.39 |
| 包体优化 | 185MB → 67MB（-64%） |
| 编译告警 | Kotlin 0；Gradle 2 条 AGP 9 迁移提示（非错误） |
