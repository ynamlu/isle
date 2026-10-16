# 心屿（Isle）· 开发全流程记录

> 本文档记录本项目从 0 到 1 的**真实开发链路**：每一次技术决策、每一个阶段的产出、
> 以及全程踩到的坑与修复方式。所有条目均可在录屏中重演验证。

---

## 阶段零 · 技术选型与架构决策

### 目标
确定平台范围、技术栈与代码组织方式，并把它固化为对后续所有 AI 生成代码的约束。

### 真实决策过程

**平台范围**：赛题为移动端应用，团队资源集中在 Android 单端打磨，不做跨平台折中。

**技术栈选择依据**：

| 维度 | 评估 | 结论 |
| --- | --- | --- |
| 端侧 AI 推理 | 核心差异化能力是**端侧语义向量**（记忆检索），需要直接调用 ONNX Runtime 原生接口 | 原生 Kotlin 集成链路最短 |
| 系统能力 | 输入法、图片选择、文件存储、通知等系统 API 直连 | 原生 Kotlin 无需插件桥接 |
| UI 表达 | 液态玻璃（真实背景模糊）需要底层渲染控制 | Compose + 原生渲染可控 |

**最终决策**：**Kotlin + Jetpack Compose 原生 Android**。该决策使端侧向量推理从"跨语言桥接调试"
降为"直接调用 API"，是项目能在一个短周期内跑通核心能力的前提。

### 架构分层（先定约束，再让 AI 生成代码）
```
UI（Compose Screen / ViewModel）
  ↓
Domain（提示词组装 / 记忆引擎 / 向量运算）
  ↓
Data（Room / DataStore / DeepSeek 客户端 / 仓储）
```
**约束原则**：AI 生成的任何代码必须落在上述四层之内，禁止跨层直接调用；
依赖注入使用手写 `AppContainer`，不引入 DI 框架（减少 AI 幻觉面）。

## 阶段一 · 项目骨架

### 目标
搭建可编译、可运行的最小工程。

### 产出
`settings.gradle.kts` · 根 `build.gradle.kts` · `gradle/libs.versions.toml`（版本目录）·
`gradle.properties` · Gradle Wrapper 9.6.0 · `AndroidManifest.xml` · `res/values/*`

### 关键问题与修复
| # | 报错 | 定位过程 | 修复 |
| --- | --- | --- | --- |
| 1 | `Cannot add extension with name 'kotlin', as there is an extension already registered` | AGP 9 已内置 Kotlin 支持，与独立 `org.jetbrains.kotlin.android` 插件冲突 | 移除独立 Kotlin 插件，改用内置 |
| 2 | `Starting in Kotlin 2.0, the Compose Compiler Gradle plugin is required` | Compose 编译需要独立编译器插件 | 应用 `org.jetbrains.kotlin.plugin.compose` |
| 3 | Gradle 报 TLS handshake failed | 瞬时网络抖动，重试即通过 | 重试 |

### 验证
`./gradlew assembleDebug` → **BUILD SUCCESSFUL**，产出 11.6MB APK。

---

## 阶段二 · 数据层与角色系统

### 目标
落地三张核心表与数据流闭环。

### 产出
- **Room**：`CharacterEntity`（角色）、`MessageEntity`（会话）、`MemoryEntity`（长期记忆，含 `embedding` 二进制列）
- 三个 DAO + `IsleDatabase`（外键级联删除）
- **DeepSeek 客户端**：`DeepSeekApi`（Retrofit，`@Url` 支持运行时可配地址）+ `DeepSeekClient`（OkHttp SSE 流式 + 可取消）
- **多模态消息工厂** `ContentFactory`：文本 / 图片 base64 内联（`image_url`）
- **提示词引擎** `PromptBuilder`：角色卡 + 召回记忆 + 用户资料 → 系统提示词
- **仓储层** 与 **`AppContainer`** 依赖容器
- **UI**：角色列表 / 角色编辑 / 聊天 / 设置四个页面 + Navigation 路由
- **头像上传**：Photo Picker + FileProvider 拍照

### 关键问题与修复
| # | 报错 | 定位过程 | 修复 |
| --- | --- | --- | --- |
| 4 | `Using kotlin.sourceSets DSL to add Kotlin sources is not allowed with built-in Kotlin` | KSP 需要向 kotlin sourceSets 注入生成源码，AGP 9 默认禁止 | 增加 `android.disallowKotlinSourceSets=false` |
| 5 | `Class 'kotlin.reflect.KClass' was compiled with an incompatible version of Kotlin (2.4.0 vs 2.2.0)` | 依赖解析把 `kotlin-stdlib` 拉到 2.4.10 | 逐库排查，定位到 Coil 3.6.3 强约束 stdlib 2.4；降级 Coil 至 **3.4.0** |
| 6 | 聊天图片点开大图模糊 | 发现聊天图片复用了头像压缩逻辑（512px） | 抽取 `maxSize` 参数：头像 512px、聊天图 **1600px** |

### 验证
- 真机安装启动，首屏渲染正常
- 与角色完成真实对话，回复符合角色设定（闺蜜口吻、使用口癖）
- **图片多模态实测**：发送注册页截图，AI 准确描述了页面内容

---

## 阶段三 · 端侧语义记忆引擎

### 目标
实现"完全离线"的长期记忆检索，形成核心差异化。

### 产出
- **模型**：`bge-small-zh-v1.5` ONNX **int8 量化版**（23.9MB）+ `vocab.txt`（21128 词），随包分发于 `assets/embedding/`
- **`BertTokenizer`**：自实现 BERT WordPiece 分词（CJK 字符切分 + 贪心最长匹配）
- **`TextEmbedder`**：ONNX Runtime 推理，CLS 池化 + L2 归一化，输出 512 维向量
- **`VectorMath`**：余弦相似度 + FloatArray 字节序列化（存 Room）
- **`MemoryRepository`**：写入自动补向量；召回采用 `语义 0.8 + 关键词 0.2 + 重要度 0.1` 加权
- **`MemoryExtractor`**：用 LLM 从对话抽取事实（JSON 数组，限 5 条/轮）
- **记忆管理界面**：分类标签、单条删除、一键清空

### 关键决策：降低集成风险
先用「关键词 + 重要度」实现召回，**把检索层抽象为 `EmbeddingProvider` 接口**，
再接入 ONNX 实现；模型加载失败自动降级为无向量模式，不影响应用其他功能。

### 关键问题与修复
| # | 问题 | 定位过程 | 修复 |
| --- | --- | --- | --- |
| 7 | 记忆措辞"用户喜欢吃东西。"过于生冷 | 抽取提示词使用第三人称 | 改为**角色第一人称温情口吻**，并在提示词中禁止出现"用户"二字，增加"已知信息不重复记录"规则 |
| 8 | 多角色之间是否记忆串台 | 无法通过肉眼确认 | 编写仪器化测试 `MemoryIsolationTest`，证明隔离 + 级联删除 |

### 验证（真实测试数据）
| 测试 | 结果 |
| --- | --- |
| `./gradlew connectedDebugAndroidTest` | **2 tests, 0 failures** |
| 端侧语义区分度 | `sim(爬山, 登山) = 0.85` vs `sim(爬山, 股票) = 0.39` |
| 向量维度 / 耗时 | 512 维，3 次推理约 0.4s（真机） |
| 端到端 | 对话后自动生成记忆：「记得七月二十号是你的生日…」「知道你周末喜欢去爬山…」 |

---

## 阶段四 · 视觉体系重构

### 目标
从"能用"提升到"耐看"，并统一全局风格。

### 关键定位
用户反馈"UI 没有全局统一" → 排查发现根因是 **Material You 动态取色**：
系统按壁纸生成配色，与品牌紫互相打架。

### 产出
| 项 | 内容 |
| --- | --- |
| 主题 | **关闭动态取色**，定义完整品牌色板（亮/暗双套） |
| 设计规范 | `DesignSystem.kt`：圆角体系（10/14/18/22/28）、间距体系（4/8/12/16/20/24） |
| 液态玻璃 | 接入 **Haze 2.0.1**，实现真实背景模糊（backdrop blur）+ 磨砂噪点 |
| 首页重构 | 去掉悬浮加号 → 列表内「＋ 创建新角色」卡片；新增 Hero 卡与分组标题 |
| 交互补全 | 聊天页右上角改为「编辑角色」；图片支持全屏查看（双指缩放/拖动） |
| 合规 | 隐私文案由"仅保存在本机"改为准确表述（说明会传输至模型服务） |

### 关键问题与修复
| # | 问题 | 定位过程 | 修复 |
| --- | --- | --- | --- |
| 9 | 应用内键盘唤不起来 | 用 `dumpsys input_method` 采集状态：`mInputShown=true`、`mServedView=AndroidComposeView`、IME 窗口存在 → 判定 App 侧正常；临时切换系统键盘后立刻弹出 → **确认是设备输入法问题** | App 侧仍做加固：`windowSoftInputMode="adjustResize"` + 各页 `imePadding()` |
| 10 | Haze 2.x 无法使用 | `Module was compiled with an incompatible version of Kotlin (2.4.0 vs 2.2.0)` | **升级 Kotlin 2.3.21**（详见下条），升级后 Haze 2.0.1 直接可用 |
| 11 | Kotlin 编译器版本受限 | AGP 9 内置 Kotlin 编译器固定 2.2.0，无法读取新库元数据 | 关闭内置 Kotlin（`android.builtInKotlin=false`）+ 关闭 AGP 新 DSL（`android.newDsl=false`），改用独立 Kotlin 插件 2.3.21；KSP 同步升至 2.3.12 |
| 12 | 一次批量去重命令误删源码 | 使用 `awk` 全局去重导致文件内合法重复行被删 | 整文件重写修复，并在流程中禁用该手法 |
| 13 | 首页顶部模糊过重 | 大面积高饱和渐变卡滚入顶栏后被 48dp 强模糊糊成色雾 | `blurRadius` 48→22dp、`noiseFactor` 0.14→0.06、底色 alpha 0.10→0.14 |

### 验证
真机逐页截图确认：首页、聊天页、设置页、角色编辑页、技术信息页、图片查看器。

---

## 阶段五 · 收尾

- 清理未使用代码与依赖（`okhttp-logging`、`ui-tooling-preview`、`room-ktx`、未使用的插件别名）
- APK 体积优化：**185MB → 67MB**（按 CPU 架构拆分，避免 ONNX Runtime 四种架构原生库全量打包）
- 开发者信息与项目背景文档化

---

## 附：AI 协同方式说明

本项目的开发全程采用 **Vibe Coding** 范式，核心方法是：

1. **先定约束，再生成代码**：每次让 AI 生成前，先用自然语言明确"分层边界 + 数据流 + 命名规范"，
   而不是直接说"帮我写个聊天页"。
2. **用编译器和测试做裁判**：AI 给出的方案不靠"看起来对"，一律通过 `compileDebugKotlin`、
   仪器化测试、真机日志（`dumpsys` / `logcat`）验证。
3. **报错即上下文**：把完整的报错栈、依赖树（`dependencyInsight`）、反编译出的类结构
   （`javap` 查 API 签名）回灌给 AI，让问题的定位过程可复现。
4. **抽象层先行**：对高风险能力（端侧向量、记忆检索）先定义接口与降级路径，再替换实现，
   保证任何一步失败都不会让应用不可用。

> 完整的 Prompt 链与修 Bug 过程见提交的「AI 对话历史导出」材料。
