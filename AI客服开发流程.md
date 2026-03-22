你这个方向非常对，而且你已经有一份可复用的雏形。  
我看完 `@consultant` 代码后，结论是：**它已经具备“AI 对话 + 工具调用 + 基础 RAG”的骨架，但业务域是“到店商家/券”而不是“网球场预约”，所以要做的是“迁移业务工具 + 重建知识库 + 接入你现有鉴权体系”。**

## 我先帮你拆解 `consultant` 现状

### 1) 已有能力（可直接复用）

- **AiService + 流式输出**：`ConsultantService` 已经是 LangChain4j 标准写法，支持会话 + 流式返回。
- **会话记忆**：基于 Redis 存聊天上下文（`RedisChatMemoryStore`）。
- **工具调用（Function Calling）**：有 `shopTool/reservationTool/voucherTool`，模型可调用 Java 方法落库/查库。
- **RAG 检索器**：`contentRetriever` 已接入到 AiService。

关键配置点在这里：  

```12:20:/Users/tennis/IdeaProjects/tennis_court_booking/consultant/src/main/java/com/itheima/consultant/aiservice/ConsultantService.java
@AiService(
        wiringMode = AiServiceWiringMode.EXPLICIT,
        chatModel = "openAiChatModel",
        streamingChatModel = "openAiStreamingChatModel",
        chatMemoryProvider = "chatMemoryProvider",
        contentRetriever = "contentRetriever",
        tools = {"shopTool","reservationTool","voucherTool"}
)
```

### 2) 当前不匹配你项目的地方

- **系统提示词是“商家/优惠券/到店消费”语境**，不是网球场预约。  
- **Tool 绑定的数据表和服务也是商家域**，和你现在 `court/booking/user` 业务对象不一致。
- **RAG 存储现在是内存向量库倾向**（`InMemoryEmbeddingStore`），不适合长期商用沉淀。
- **聊天接口没接你现有 JWT 用户体系**（你主项目通过 `JwtAuthenticationFilter` 已把 `userId` 注入 request）。

---

## 如何给你的网球预约项目加入智能客服（落地版）

## 目标架构（与你现有项目契合）

1. 前端发送用户问题（带 JWT）。
2. 后端 `/api/ai/chat` 收到请求，取 `userId` 作为 `memoryId`。
3. LangChain4j 先做 RAG 检索（规则文档、FAQ、退款说明等）。
4. 模型在必要时调用业务 Tool（查场地、查我的订单、下单建议、取消规则）。
5. 组装答案返回前端（建议流式 SSE）。

---

## 分阶段实施（建议按这个顺序）

### Phase 1：最小可用（1-2天）

- 在主项目新增 `ai` 模块代码（不用再单独跑 consultant 子项目）：
  - `AiChatController`（`/api/ai/chat`）
  - `TennisConsultantService`（AiService）
  - `AiCommonConfig`（chat model / embedding / memory / retriever）
  - `RedisChatMemoryStore`（可复用 consultant 实现）
- 把 `memoryId` 绑定为当前登录用户：`memoryId = "u:" + userId`。
- 新建 `system-tennis.txt`，改成网球场景提示词（预约、支付、取消、管理员权限边界）。

### Phase 2：业务工具接入（2-4天）

新增工具类（替换掉商家券工具）：

- `CourtTool`：查询场地、营业时间、价格
- `BookingTool`：查询“我的预约”、检查时段可用、创建预约（可先只读后写）
- `PolicyTool`：返回取消规则/支付说明（可先从配置或文档读取）

工具方法内部直接调你已有 `CourtService/BookingService`，这块你现在基础很好，特别是你已有：

- 时段冲突校验
- 用户维度“我的预约”
- 取消申请/支付状态流转

### Phase 3：RAG 从 demo 到可商用（3-7天）

- 文档源：平台帮助中心、预约规则、退款规则、场地介绍、常见问题。
- **向量库建议**：
  - 开发期：内存或本地都行
  - 生产建议：Redis Stack / Milvus / pgvector（至少选一个持久化）
- 增量更新机制：
  - 文档变更后触发重新切分+嵌入
  - 记录文档版本，支持重建索引
- 检索策略：
  - `topK + minScore`
  - 按租户/业务域 metadata 过滤（后续如有多场馆运营非常关键）

---

## 与你当前代码的关键对齐点（非常加分）

- 你已具备 JWT 上下文注入，可直接做“用户级记忆”：

```51:55:/Users/tennis/IdeaProjects/tennis_court_booking/src/main/java/com/tennis_court_booking/security/JwtAuthenticationFilter.java
request.setAttribute("userId", jwtUtil.extractUserId(token));
request.setAttribute("username", username);
request.setAttribute("role", jwtUtil.extractRole(token));
```

- 你已有可复用业务能力，Tool 直接调服务层即可（例如预约校验、我的订单、取消申请等），不用再造一套逻辑。

---

## 商用可行性与难点（你面试可直接说）

### 能商用吗？

- **可以做到“商用雏形”**：已具备 AI 会话、RAG 检索、业务工具调用。
- 距离“生产级”还需补：可观测性、风控、敏感信息保护、SLA、降级策略。

### 最难的技术点

1. **幻觉控制**：必须“优先工具结果 + RAG证据”，不能自由编造订单状态。
2. **权限边界**：AI 不能越权查他人订单（memoryId 与 userId 强绑定 + tool层再鉴权）。
3. **RAG 数据治理**：文档更新、分块策略、召回质量、过期知识清理。
4. **稳定性**：模型超时/限流时要有降级（转人工、固定FAQ、重试队列）。

---

## 你下一步最优动作

如果你同意，我可以下一步直接给你出**“可直接开工的代码改造清单（到类名/接口参数级别）”**，包括：

- 主项目 `pom.xml` 需要新增的依赖
- 新增哪些 `Controller/Service/Tool/Config` 文件
- 前端聊天接口如何调用（含流式返回格式）
- 第一版 `system-tennis.txt` 提示词模板（可直接用）

第 1 步已经完成并且编译通过了，你现在主项目已经具备“调用大模型并返回结果”的最小能力。

已改动：

- 新增依赖`pom.xml`
  - `langchain4j-open-ai-spring-boot-starter`
  - `langchain4j-spring-boot-starter`
- 新增模型配置`src/main/resources/application.yml`
  - `langchain4j.open-ai.chat-model.`*
  - `api-key` 使用环境变量`AI_API_KEY`
- 新增 AI 服务接口`src/main/java/com/tennis_court_booking/ai/service/SimpleAiService.java`
- 新增测试接口`src/main/java/com/tennis_court_booking/ai/controller/AiChatController.java`
  - `POST /api/ai/chat/simple`
  - 请求体`{"message":"..."}`
  - 成功时 `data` 为对象：`{ "content": "展示给用户的正文（已去掉模型内部的 ---QUICK--- 段）", "quickOptions": ["快捷1","快捷2", ...] }`（最多 5 条；模型需在原文末尾按约定输出 `---QUICK---` 与 JSON 数组，由后端解析）

我已执行 `mvn -DskipTests compile`，结果成功。

---

你现在可以这样测：

1. 启动前先设置 key（以 zsh 为例）

```bash

export AI_API_KEY=你的key

```

1. 登录拿到 token 后调用

```bash

curl -X POST "[http://localhost:8080/api/ai/chat/simple](http://localhost:8080/api/ai/chat/simple)" \

  -H "Content-Type: application/json" \

  -H "Authorization: Bearer 你的JWT" \

  -d '{"message":"你好，帮我介绍下如何预约网球场"}'

```

---

下一步（第 2 步）建议：  

开始把 `SimpleAiService` 升级为 **会话记忆 + 用户 memoryId 绑定（userId）**，然后再接 RAG。  

你确认后我继续直接改。

已经开始做“会话记忆 + userId 绑定”了。你这一步的目标是：同一个登录用户多轮提问时，模型能带着历史回答；不同用户之间不互相混淆。

## 我做了哪些改动（第 2 步）

### 1. `SimpleAiService`：把接口改成“带 memoryId 的对话”

文件`src/main/java/com/tennis_court_booking/ai/service/SimpleAiService.java`

- 改了方法签名`chat(String message)` -> `chat(@MemoryId String memoryId, @UserMessage String message)`
- 给 `@AiService` 增加显式装配：指定
  - `chatModel = "openAiChatModel"`
  - `chatMemoryProvider = "chatMemoryProvider"`

为什么这么做：  

LangChain4j 只有在方法参数上带了 `@MemoryId` 时，才知道“同一个 memoryId 对应同一份历史对话”，从而实现会话记忆。

---

### 2. 新增会话记忆配置（ChatMemoryProvider + ChatMemoryStore）

新增文件：

- `src/main/java/com/tennis_court_booking/ai/config/AiCommonConfig.java`
- `src/main/java/com/tennis_court_booking/ai/memory/InMemoryChatMemoryStore.java`

做了两件事：

- `AiCommonConfig` 提供 `chatMemoryProvider`，其内部使用 `MessageWindowChatMemory(maxMessages=20)`
- `InMemoryChatMemoryStore` 用一个线程安全内存 Map 保存每个 `memoryId` 对应的消息列表（开发/演示用）

为什么这么做：  

- `ChatMemoryStore` 负责“保存历史”
- `ChatMemoryProvider` 负责“按 memoryId 取出历史并喂给模型”
- 用 `maxMessages(20)` 控制上下文长度，避免 token 过长

注意：这是**内存版**，重启服务会丢失；多实例部署时也不会共享。后面做 RAG/商用时建议替换成 Redis 版本。

---

### 3. `AiChatController`：从 JWT 的 `userId` 构造 memoryId 并传给模型

文件`src/main/java/com/tennis_court_booking/ai/controller/AiChatController.java`

- `@PostMapping("/chat/simple")` 现在直接注入`@RequestAttribute("userId") Integer userId`
- 构造`memoryId = "u:" + userId`
- 调用`simpleAiService.chat(memoryId, message)`

为什么这么做：  

这样每个登录用户的 memoryId 都固定且唯一，保证：

- 同一用户：多轮上下文连续
- 不同用户：不会串话

---

## 你接下来要做什么（验证这一改动）

1. **重启后端服务**（因为接口签名和内存存储都变了）
2. 用同一个 token 连续发两条消息，例如：
  - 第一句`你好，帮我介绍下如何预约网球场？`
  - 第二句`那我想预约明天上午，流程怎么走？`

如果记忆生效，第二句的回答会更贴合第一句你正在“预约流程”的上下文。

需要我下一步再带你做：**把记忆存储换成 Redis 持久化**（这样你重启/多实例也不丢历史）吗？











你现在“memory + userId 绑定”本身没问题，但**记忆只负责让模型记住上下文**，不会自动保证它遵守你的业务规则。要让回复符合你项目逻辑，关键是两件事：

1) **用更严格的 system prompt**把“能做什么/必须怎么做/不能编造”写死  

2) **给模型接入业务 Tool**（查场地、查可约时段、查我的预约、提交取消/支付/创建预约），让“事实与动作”都必须来自你的后端逻辑

我已经按这个思路替你做了第 2 步改造。

## 我做了哪些改动（以及为什么）

### 1. 强化系统提示词（强约束业务逻辑）

文件`src/main/java/com/tennis_court_booking/ai/service/SimpleAiService.java`

- 给 `@SystemMessage` 增加了明确规则：

  - 禁止编造场地/时段/订单状态

  - 预约时必须补齐字段`courtId、bookingDate、startTime、endTime、contactName、contactPhone`

  - 当有 `courtId + bookingDate` 时必须先查可约时段（用工具）

  - 取消/支付必须提供 `bookingId` 和支付渠道（并走工具）

**为什么**：否则模型会“凭感觉”给出流程/数据，天然不可能稳定贴合你的业务。

### 2. 接入业务 Tool（让模型调用后端真实能力）

新增文件：

- `src/main/java/com/tennis_court_booking/ai/tools/CourtTool.java`

- `src/main/java/com/tennis_court_booking/ai/tools/BookingTool.java`

功能包括：

- `CourtTool`

  - 查询所有场地

  - 根据场地ID查询

  - 模糊关键词筛选

- `BookingTool`

  - `myBookings()`：查询当前登录用户的我的预约（从 `request attribute("userId")` 取，保证不串用户）

  - `getCourtSlotOptions(courtId, bookingDate, excludeBookingId)`：查某场地某日可约时段

  - `checkTimeAvailable(...)`：判断某时间是否可预约

  - `requestCancel(bookingId)`：提交取消申请

  - `payBooking(bookingId, channel)`：确认付款

  - `createBooking(...)`：创建预约（直接走你现有 `bookingService.addBooking`，冲突会按你逻辑失败并返回失败原因给模型）

并且在 `SimpleAiService` 里把工具声明为：

- `tools = {"courtTool", "bookingTool"}`

**为什么**：有了 Tool，模型在“需要事实/需要写入数据”时就只能依赖你的服务逻辑；你的校验（时间冲突、每日上限、状态限制）就会自动生效，从而回复更符合你的项目逻辑。

## 你接下来怎么验证（按这 2 轮测）

1) 重启后端（因为我们改了 AI 服务与 Tool）

2) 同一个 token 连续发两轮，例如：

- 轮1`我想预约明天上午，流程怎么走？`

  - 期望：AI 会先追问 `courtId、bookingDate、startTime/endTime、联系人信息`，不应胡编

- 轮2（你给出 courtId 和日期后）`courtId=7，bookingDate=2026-03-21，我想约 09:00-10:00`

  - 期望：AI 会用工具给出可约/不可约结果或可选时段，并引导下一步（创建/支付）

如果你愿意，把你“现在不符合逻辑”的那两句原始对话（用户怎么问、AI怎么答）贴出来，我可以再把 system prompt/tool 参数规则调得更贴合你们实际页面流程。