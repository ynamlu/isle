# 心屿（Isle）· 技术文档

> 全国高校 Vibe Coding 开发挑战赛
> 主题：重构范式 —— 基于 Vibe Coding 的全栈应用速成挑战
>
> 本文档重点阐述三项内容：**意图控制策略**、**架构设计**、**可复现性论证**。

---

## 一、项目定位与创新点

### 1.1 产品定位

心屿是一款原生 Android **AI 情感陪伴**应用。用户可创建专属陪伴角色（恋人 / 闺蜜 / 朋友 / 家人），
自定义其性格、背景与说话风格；角色会**记住**用户说过的话、喜欢的东西、在意的事，
在每次对话中带着对用户的了解出现。

### 1.2 核心差异化：端侧语义记忆

市面上同类产品的"记忆"普遍依赖云端向量数据库，存在三个问题：**隐私外泄、联网依赖、按量计费**。

心屿采用**完全离线的端侧语义记忆**：

| 环节 | 实现 |
| --- | --- |
| 记忆抽取 | LLM 从对话中抽取事实，输出结构化 JSON |
| 向量化 | 端侧 ONNX 模型（`bge-small-zh-v1.5` int8，512 维） |
| 存储 | Room 二进制列，随角色级联删除 |
| 召回 | 余弦相似度 + 关键词 + 重要度加权排序 |
| 隐私 | **推理全在手机本地，对话内容不出设备** |

---

## 二、意图控制策略（Intent Control）

Vibe Coding 的核心风险是"AI 生成的结果看似合理却偏离意图"。本项目用四层机制约束。

### 2.1 约束一：先定契约，再要代码

每次请求 AI 生成代码前，先用自然语言固化四件事：

```
1. 分层边界  —— 这段代码属于 UI / Domain / Data 哪一层，禁止跨层调用
2. 数据流    —— 输入是什么、输出是什么、经由哪些对象
3. 命名规范  —— 与既有代码保持一致（例如 Repository / Provider / Entity 后缀）
4. 验收标准  —— 如何证明它是对的（编译通过 / 哪个测试 / 看什么日志）
```

**反例（低效 Prompt）**：「帮我写个聊天页面」
**本项目用法**：「写聊天页的 ViewModel。输入是 characterId；它从 AppContainer 取
`chatRepository` / `memoryRepository` / `deepSeekClient`；发送流程是先落库用户消息、
再插占位助手消息、再流式更新、完成后落库并异步抽取记忆；对外暴露 StateFlow；验收标准是编译通过。」
—— 这类 Prompt 使 AI 输出直接落在既有架构内，几乎不需要返工。

### 2.2 约束二：让编译器与测试当裁判

不依赖"看起来对"，全部结论必须可验证：

| 手段 | 用途 |
| --- | --- |
| `./gradlew :app:compileDebugKotlin --rerun-tasks` | 强制全量编译，杜绝增量缓存的假通过 |
| `connectedDebugAndroidTest` | 端侧向量语义区分度、多角色记忆隔离 |
| `dependencyInsight` | 定位依赖冲突的真实来源 |
| `dumpsys input_method` / `logcat` | 区分"App 问题"与"设备问题" |
| `javap` 反编译依赖 jar | 在 AI 不确定 API 时，直接从字节码确认方法签名 |

> 典型案例：接入 Haze 库时，不靠猜测 API，而是 `javap` 反编译其 jar 得到精确签名：
> `hazeBlur(Modifier, HazeInput, HazeBlurStyle, HazePerformanceMode, boolean)`，
> 再据此约束 AI 生成调用代码，一次通过。

### 2.3 约束三：报错即上下文（闭环排错法）

遇到失败时，把**完整证据链**回灌给 AI，而不是只描述现象：

```
报错原文（完整栈 + 关键行）
+  依赖树（谁引入了冲突版本）
+  环境事实（Gradle / JDK / AGP / Kotlin 版本）
+  已尝试的修复及其结果
```

**典型案例（原文摘录）**：

> 报错：`Class 'kotlin.reflect.KClass' was compiled with an incompatible version of Kotlin.
> The actual metadata version is 2.4.0, but the compiler version 2.2.0 can read versions up to 2.3.0.`

定位过程：
1. 执行 `dependencyInsight --dependency kotlin-stdlib` → 发现 **Coil 3.6.3 强约束 stdlib 2.4.10**
2. 逐个检查候选库的 POM，确认 Coil 3.4.0 使用 stdlib 2.3.10（编译器可读）
3. 降级至 3.4.0 → 编译通过

同类问题在接入 Haze 2.x 时再次出现，此次选择**根治**：将 Kotlin 编译器升级到 2.3.21
（可读元数据至 2.4.0），一次解决整类问题。

### 2.4 约束四：抽象先行，为失败预留降级路径

对高风险能力，先定义接口与降级方案，再替换实现：

```kotlin
interface EmbeddingProvider { suspend fun embed(text: String): FloatArray? }

class OnnxEmbeddingProvider(context) : EmbeddingProvider   // 端侧模型
// 模型加载失败 → embed() 返回 null → 召回自动回退关键词匹配
```

结果：即使模型加载失败，应用的**对话、角色、记忆书写**等能力全部不受影响，只是召回退化为关键词模式。

---

## 三、架构设计

### 3.1 总体分层

```
┌─────────────────────────────────────────────────────┐
│  UI 层（Compose）                                    │
│  Screen（无状态）  ←→  ViewModel（StateFlow）         │
│  角色列表 / 角色编辑 / 聊天 / 记忆 / 设置(4 个子页)     │
├─────────────────────────────────────────────────────┤
│  Domain 层                                           │
│  PromptBuilder   —— 角色卡 + 记忆 + 用户资料 → 提示词  │
│  MemoryExtractor —— 对话 → 结构化记忆（LLM）          │
│  BertTokenizer / TextEmbedder / VectorMath —— 端侧向量 │
├─────────────────────────────────────────────────────┤
│  Data 层                                             │
│  Repository：Character / Chat / Memory               │
│  本地：Room（3 表）+ DataStore（2 组配置）             │
│  远端：DeepSeekClient（SSE 流式）+ ContentFactory      │
├─────────────────────────────────────────────────────┤
│  基础设施                                            │
│  AppContainer（手写依赖容器）· AvatarStorage（图片）   │
└─────────────────────────────────────────────────────┘
```

### 3.2 关键设计决策与理由

| 决策 | 理由 |
| --- | --- |
| **手写 AppContainer 而非引入 DI 框架** | 依赖关系少（约 10 个对象），手写容器更易被 AI 正确理解与修改，减少幻觉面 |
| **单一 Activity + Compose Navigation** | 状态集中在 ViewModel，避免跨 Activity 传参带来的一致性风险 |
| **Provider 抽象 + 运行时 `@Url`** | 模型地址在 DataStore 中可配置，无需重新构建即可切换服务 |
| **记忆按 `characterId` 外键隔离** | 多角色场景下杜绝记忆串台，删除角色时级联清除 |
| **向量以 `ByteArray` 存 Room** | 避免引入向量数据库，几千条记忆暴力余弦已足够（毫秒级） |
| **按 CPU 架构拆分 APK** | ONNX Runtime 含四套原生库，不拆分会导致包体达 185MB；拆分后 67MB |

### 3.3 关键数据流：一次对话

```
用户输入（可带图片）
   ↓
ChatViewModel.send()
   ↓ ① 落库用户消息
   ↓ ② 插占位助手消息（便于流式回填）
   ↓ ③ MemoryRepository.retrieveRelevant()   ← 端侧向量召回 Top-K 记忆
   ↓ ④ PromptBuilder.buildConversation()
   │     系统提示词 = 角色卡 + 召回记忆 + 我的资料 + 对话规范
   │     消息列表   = 系统 + 最近 12 条历史 + 当前消息（含图片 base64）
   ↓ ⑤ DeepSeekClient.stream()  ← SSE 逐字返回
   ↓ ⑥ 增量更新 UI（StateFlow）
   ↓ ⑦ 完成后落库
   ↓ ⑧ 异步：MemoryExtractor 抽取新事实 → 端侧向量化 → 写入记忆库
```

### 3.4 提示词工程：记忆的"温度"

抽取记忆时，不使用中性的第三人称陈述（"用户喜欢爬山"），而是约束为**角色的第一人称心理活动**：

```
要求：
1. 用「你」称呼对方，写成一句温柔自然的话，像是你在心里默默记住的
2. 绝对不能出现「用户」这两个字，也不要写成冷冰冰的资料条目
3. 已知的信息不要重复记录，只记这次新出现的内容
```

抽取结果示例：
- `记得七月二十号是你的生日，是夏天里海边最舒服的时候`
- `知道你周末喜欢去爬山，身上总带着一股很清爽的劲儿`

同一份数据，既是**模型可用的上下文**，也是**用户在记忆页能读到的情感内容**。

---

## 四、可复现性论证

### 4.1 环境可复现

- 全部依赖版本集中在 `gradle/libs.versions.toml`（版本目录），单一可信来源
- Gradle Wrapper 随仓库分发，**构建不依赖本机 Gradle 安装**
- `local.properties` 模板化（SDK 路径 + 可选密钥），且被 `.gitignore` 排除
- 提供逐条排错表（见 README 第四节），覆盖构建期的已知坑

### 4.2 行为可验证

| 验证维度 | 手段 | 实测结果 |
| --- | --- | --- |
| 编译正确性 | `compileDebugKotlin --rerun-tasks` | BUILD SUCCESSFUL，0 警告 |
| 端侧向量有效性 | `TextEmbedderTest`（仪器化） | `sim(爬山,登山)=0.85` vs `sim(爬山,股票)=0.39` |
| 多角色隔离 | `MemoryIsolationTest`（仪器化） | 2 tests, 0 failures |
| 端到端 | 真机对话 + 图片 + 记忆查看 | 全部通过 |

### 4.3 演进路径可追溯

- Git 提交按**真实开发顺序**重建：技术骨架 → 数据层 → 网络层 → 记忆引擎 → 界面 → 视觉体系 → 文档
- 每个提交对应一个功能模块的落地，**不存在 `initial commit` 一次性入库**
- `docs/DEVELOPMENT_LOG.md` 逐条记录了 13 个真实问题及其定位与修复过程，
  每一条都可在录屏中重演（例如：修改 Kotlin 版本使 Haze 编译通过）

### 4.4 依赖风险已显式声明

对每一个"被版本卡住"的依赖，文档中均标注了**约束与原因**：
Coil 锁定 3.4.0、Kotlin 必须 ≥2.3.21、必须保持 `android.builtInKotlin=false` 等。
复现者若遇到问题，可依据 README 的排错表直接定位。

---

## 五、已知限制与后续规划

| 项 | 说明 |
| --- | --- |
| 向量检索规模 | 当前为暴力余弦（几千条内毫秒级）；若记忆量级上升到数万条，需引入 HNSW 索引 |
| 记忆抽取时机 | 每轮对话后触发一次 LLM 抽取，长对话成本会累积；后续可改为"按间隔 + 摘要压缩" |
| 语音能力 | 未实现（TTS / STT 已在架构上预留） |
| 平台 | 仅 Android；端侧向量与 UI 层的接口已做抽象，便于后续移植 |
| 发布体积 | 当前为 debug 包（67MB）；release 开启 R8 与资源压缩后预计显著下降 |
