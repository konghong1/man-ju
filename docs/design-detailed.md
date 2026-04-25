# AI 漫剧生成 App 详细设计（V3：多模型商版）

## 1. 产品目标与范围

目标：构建可生产使用的 AI 漫剧生成系统，支持不同模型商统一接入与可运营管理。  
范围：先完成“文本漫剧生产链”，再接图像生成链路。

核心产物：
1. 剧集大纲（Episode Arc）
2. 场景剧情（Scene Beats）
3. 分镜面板（Panels）
4. 绘图提示词（Prompt Pack）
5. 版本快照与质量报告（Quality Report）

## 2. 为什么上一版看起来“没有 LLM 支持”

上一版按 MVP 思路先做了生成链路骨架与可验证流程，LLM 只保留抽象入口。  
V3 直接升级为“LLM Gateway 一等公民”，要求：
1. 多模型商统一协议
2. 步骤级模型路由
3. 自动降级与重试
4. 成本、延迟、成功率可观测

## 3. 前端架构（Vue）

技术栈：
1. Vue 3 + TypeScript + Vite
2. Vue Router
3. Pinia
4. Axios（统一鉴权、错误拦截）
5. Vitest + Vue Test Utils + Playwright

页面：
1. `/` 项目列表
2. `/projects/new` 创建向导
3. `/projects/:id/workspace` 生成工作台
4. `/projects/:id/review` 质检页
5. `/projects/:id/export` 导出页
6. `/settings/models` 模型配置页（新增）

前端新增 LLM 管理能力：
1. Provider 启停（OpenAI/Anthropic/Google/Azure/OpenRouter/兼容 OpenAI）
2. 模型别名映射（业务别名 -> 实际模型）
3. 步骤路由配置（S2 用模型A，S3 用模型B）
4. 运行时观测面板（token/cost/latency/error）

## 4. 后端架构

分层：
1. `api`：REST + SSE
2. `application`：用例编排
3. `domain`：业务规则与实体
4. `generator`：分步骤内容生成器
5. `infra`：存储、队列、LLM 网关适配

### 4.1 Harness 生成流水线

1. `S1_INPUT_NORMALIZE`
2. `S2_STORY_PLAN`
3. `S3_SCENE_WRITE`
4. `S4_PANELIZE`
5. `S5_PROMPT_COMPILE`
6. `S6_QUALITY_GATE`
7. `S7_PERSIST_VERSION`

每步落盘：
1. 输入/输出 payload
2. provider/model 选择结果
3. token 与 cost
4. latency、重试次数、错误码

### 4.2 LLM Gateway（核心新增）

统一接口：
1. `chat(request): ChatResult`
2. `structured(request, schema): JsonResult`
3. `stream(request): EventStream`

Provider 适配器（首批）：
1. OpenAI
2. Anthropic
3. Google Gemini
4. Azure OpenAI
5. OpenAI-Compatible（用于 DeepSeek/Qwen/自建网关）
6. Ollama（本地离线）

网关关键机制：
1. 路由策略：按步骤、成本、延迟、质量分数选模型
2. 降级链：主模型失败 -> 备模型 -> 保底模型
3. 熔断与限流：provider 级 circuit breaker + rate limiter
4. 幂等重试：网络错误可重试，业务错误不重试
5. 输出约束：JSON Schema 校验，不合法自动重试/修复

### 4.3 模型目录与路由

模型目录（Model Registry）字段：
1. provider
2. modelId
3. capability（reasoning/json/tool/vision/context）
4. price（input/output）
5. reliabilityScore
6. enabled

路由粒度：
1. 全局默认路由
2. 项目级覆盖路由
3. 步骤级覆盖路由
4. 紧急降级策略

### 4.4 安全与合规

1. API Key 使用 KMS/环境变量，严禁落日志
2. Prompt 与输出做敏感信息扫描
3. 供应商错误信息脱敏再返回前端

## 5. API 设计（V3）

项目与生成：
1. `POST /api/projects`
2. `GET /api/projects`
3. `GET /api/projects/{id}`
4. `POST /api/projects/{id}/jobs`
5. `GET /api/jobs/{id}`
6. `GET /api/jobs/{id}/events`
7. `POST /api/jobs/{id}/retry`

模型网关管理（新增）：
1. `GET /api/llm/providers`
2. `PATCH /api/llm/providers/{provider}`
3. `GET /api/llm/models`
4. `POST /api/llm/routes/validate`
5. `PUT /api/llm/routes/project/{projectId}`
6. `GET /api/llm/metrics?from=&to=`

版本与导出：
1. `GET /api/projects/{id}/versions`
2. `POST /api/projects/{id}/rollback`
3. `GET /api/projects/{id}/export`

## 6. 可观测性与成本治理

1. 指标：QPS、成功率、P95 延迟、每步 token/cost
2. 维度：provider/model/step/project
3. 预算：项目级预算上限，超限自动降级
4. 告警：失败率、超时率、成本突增

## 7. 里程碑（V3）

1. M1：Vue 工作台 + 基础 Harness
2. M2：LLM Gateway + 双 provider（OpenAI/Anthropic）
3. M3：多 provider 路由/降级/熔断 + 指标面板
4. M4：版本回滚、导出、E2E、上线准备
