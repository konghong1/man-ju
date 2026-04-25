# AI 漫剧生成 App 任务拆分（V3：多模型商）

说明：本版重点细化 LLM 多模型商接入与运营能力。每项包含依赖和 DoD。

## 1. 前端模块（Vue）

| Task ID | 子任务 | 依赖 | DoD |
|---|---|---|---|
| FE-01 | 初始化 Vue3 + TS + Vite + ESLint + Prettier | 无 | `npm run dev`、`npm run lint` 正常 |
| FE-02 | 配置 Vue Router（列表/创建/工作台/质检/导出/模型设置） | FE-01 | 全路由可访问 |
| FE-03 | 配置 Pinia（project/job/llm/ui store） | FE-01 | 刷新后状态可恢复 |
| FE-04 | API SDK 层（Axios + token + 错误统一） | FE-01 | 统一错误提示可用 |
| FE-05 | 项目列表页 | FE-02, FE-04 | 列表、筛选、分页可用 |
| FE-06 | 项目创建向导页 | FE-02, FE-04 | 表单校验完整，创建成功跳转 |
| FE-07 | 工作台壳（左树/中编辑/右时间线） | FE-02, FE-03 | 响应式布局可用 |
| FE-08 | SceneEditor 与 PanelEditor | FE-07 | 自动保存 + 增删改可用 |
| FE-09 | Timeline（SSE 实时步骤流） | FE-07, FE-04 | 任务状态实时刷新 |
| FE-10 | QualityReport 面板 | FE-07, FE-04 | 问题可定位到 Scene/Panel |
| FE-11 | Export 页 | FE-04 | JSON/Markdown/Prompt 包下载成功 |
| FE-12 | 模型设置页（新增） | FE-02, FE-04 | provider 启停、模型路由配置可保存 |
| FE-13 | 模型运行指标面板（新增） | FE-12, FE-04 | provider/model/step 指标可视化 |
| FE-14 | 前端单测 | FE-05~FE-13 | 核心组件单测通过 |
| FE-15 | 前端 E2E | FE-05~FE-13 | 创建->生成->导出->切模型回归通过 |

## 2. 后端核心（Harness）

| Task ID | 子任务 | 依赖 | DoD |
|---|---|---|---|
| BE-01 | 分层重构（api/application/domain/generator/infra） | 无 | 职责清晰无交叉 |
| BE-02 | 项目 CRUD 与分页 | BE-01 | API 合约通过 |
| BE-03 | Job 状态机（queued/running/succeeded/failed） | BE-01 | 状态流转合法 |
| BE-04 | StepResult 模型（input/output/metrics/error） | BE-03 | 每步可持久化 |
| BE-05 | Harness Orchestrator（S1~S7） | BE-03, BE-04 | 全链路可执行 |
| BE-06 | Retry/Resume（步骤级） | BE-05 | 失败可断点恢复 |
| BE-07 | Version Snapshot + Rollback | BE-02, BE-05 | 可回滚指定版本 |
| BE-08 | SSE 事件流 | BE-03 | 前端可实时接收 |
| BE-09 | Export API | BE-02, BE-05 | 导出包结构稳定 |

## 3. LLM 网关模块（新增重点）

| Task ID | 子任务 | 依赖 | DoD |
|---|---|---|---|
| LLM-01 | 定义统一接口 `LlmClient`（chat/structured/stream） | BE-01 | 所有 provider 遵循同一协议 |
| LLM-02 | 定义 `ModelRegistry`（模型能力/价格/上下文） | LLM-01 | 可按 provider/model 查询 |
| LLM-03 | 定义 `RoutingPolicy`（step->model 映射） | LLM-02 | 支持全局/项目/步骤覆盖 |
| LLM-04 | OpenAI 适配器 | LLM-01 | 完成至少一个步骤调用 |
| LLM-05 | Anthropic 适配器 | LLM-01 | 完成至少一个步骤调用 |
| LLM-06 | Gemini 适配器 | LLM-01 | 完成至少一个步骤调用 |
| LLM-07 | Azure OpenAI 适配器 | LLM-01 | 完成至少一个步骤调用 |
| LLM-08 | OpenAI-Compatible 适配器（DeepSeek/Qwen/本地网关） | LLM-01 | 自定义 baseUrl 可调用 |
| LLM-09 | Ollama 适配器 | LLM-01 | 本地模型可调用 |
| LLM-10 | Provider 健康检查与探活 | LLM-04~LLM-09 | `/health` 输出 provider 状态 |
| LLM-11 | 降级链（primary->secondary->fallback） | LLM-03, LLM-04~09 | 主模型失败自动降级 |
| LLM-12 | 熔断器 + 限流器 | LLM-11 | provider 异常不雪崩 |
| LLM-13 | 结构化输出校验（JSON Schema） | LLM-01 | 非法输出可重试修复 |
| LLM-14 | Prompt Template Registry | LLM-01 | 模板版本化可回溯 |
| LLM-15 | Token/Cost 计量器 | LLM-04~09 | 每步记录 token 与成本 |
| LLM-16 | 预算控制器（项目级上限） | LLM-15 | 超预算触发降级策略 |
| LLM-17 | 路由配置 API | LLM-03 | 可读写项目路由策略 |
| LLM-18 | Provider 管理 API | LLM-10 | 前端可启停 provider |
| LLM-19 | 网关审计日志（脱敏） | LLM-04~09 | 不泄露 key/prompt 敏感信息 |
| LLM-20 | 多 provider 压测与回归基线 | LLM-11~16 | 稳定性报告可输出 |

## 4. 基础设施（INFRA）

| Task ID | 子任务 | 依赖 | DoD |
|---|---|---|---|
| INF-01 | Repository 抽象统一 | BE-01 | 可插拔文件/数据库存储 |
| INF-02 | 文件存储原子写 + 并发锁 | INF-01 | 并发写无损坏 |
| INF-03 | PostgreSQL schema（Project/Job/Step/Version） | INF-01 | DDL 可迁移 |
| INF-04 | Flyway 基线脚本 | INF-03 | 重复执行安全 |
| INF-05 | 配置中心（profile + env + secrets） | 无 | dev/test/prod 隔离 |
| INF-06 | 指标采集（Prometheus/Micrometer） | BE-03, LLM-15 | 指标可查询 |
| INF-07 | 告警规则（失败率/超时/成本） | INF-06 | 触发规则可验证 |

## 5. QA

| Task ID | 子任务 | 依赖 | DoD |
|---|---|---|---|
| QA-01 | OpenAPI 合约测试 | BE-02~BE-09 | 合约测试全绿 |
| QA-02 | Harness 回归样例集 | BE-05~BE-07 | 同输入输出可比对 |
| QA-03 | Provider 兼容性测试矩阵 | LLM-04~LLM-09 | 各 provider 至少 1 组通过 |
| QA-04 | 降级链故障注入测试 | LLM-11~LLM-12 | 故障可自动切换 |
| QA-05 | 成本与预算测试 | LLM-15~LLM-16 | 预算阈值行为正确 |
| QA-06 | E2E（含切换模型商） | FE-15, BE-08, LLM-17 | 全链路可回归 |

## 6. OPS

| Task ID | 子任务 | 依赖 | DoD |
|---|---|---|---|
| OPS-01 | 一键 init（后端+前端+smoke） | FE-01, BE-02 | 一条命令拉起并验证 |
| OPS-02 | CI（lint/test/build/e2e） | QA-01~QA-06 | PR 自动校验 |
| OPS-03 | 发布流水线 | OPS-02 | 可重复打包发布 |
| OPS-04 | 运行手册（provider 密钥与故障恢复） | LLM-18, OPS-03 | 新人可按文档上线 |

## 7. 执行顺序（建议）

1. Sprint-1（骨架）：FE-01~07, BE-01~05, INF-01, OPS-01
2. Sprint-2（双 provider）：LLM-01~05, BE-06~08, FE-09~10, QA-01
3. Sprint-3（多 provider）：LLM-06~13, FE-12~13, QA-03~04
4. Sprint-4（成本与上线）：LLM-14~20, BE-09, FE-11, QA-05~06, OPS-02~04
