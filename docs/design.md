# AI 漫剧生成 App 设计方案

## 1. 目标

构建一个可运行的 Web App，用户输入故事设定后，系统自动生成：
- 剧集梗概
- 分幕剧情（Scene）
- 分镜面板（Panel）
- 每个面板可用于绘图模型的图像提示词（image prompt）

## 2. 参考文章方法（Harness）

本项目采用《Effective harnesses for long-running agents》中的核心做法：
- 结构化特性清单：使用 `feature_list.json` 管理功能与验证状态。
- 会话进度日志：使用 `claude-progress.txt` 记录每轮改动与验证结果。
- 初始化脚本：提供 `init.sh`/`init.ps1` 快速验证环境与运行检查。
- 增量交付：先完成 MVP 全链路，再扩展真实模型接入。
- 明确验收：每项功能都绑定可执行检查（API 或测试）。

## 3. 架构设计

### 3.1 技术栈
- 后端：Java 17 + Spring Boot
- 前端：静态 HTML/CSS/JS（由 Spring Boot 提供）
- 存储：本地 JSON 文件（`data/projects`）

### 3.2 模块划分
- `api`：REST API（创建项目、查询项目、健康检查）
- `service`：漫剧生成引擎（MVP 先用规则增强生成器）
- `storage`：项目持久化（文件存储）
- `model`：请求/响应与领域模型
- `static`：前端页面与交互脚本

### 3.3 数据流
1. 用户在页面输入题材、风格、主角、冲突等。
2. 前端调用 `POST /api/projects`。
3. 生成引擎产出 `ComicProject`（episodes/scenes/panels）。
4. 存储层落盘为 JSON。
5. 前端展示结构化漫剧结果。

## 4. API 设计（MVP）

- `GET /api/health`：健康检查
- `POST /api/projects`：创建漫剧项目
- `GET /api/projects/{id}`：查询指定项目
- `GET /api/projects`：列出最近项目

## 5. 生成策略（MVP）

- 基于用户输入生成统一故事主线（synopsis）。
- 按篇幅自动拆分 3~6 个场景。
- 每个场景产出固定数量分镜（默认 3）。
- 每个分镜输出：
  - 镜头描述（camera）
  - 旁白（narration）
  - 对白（dialogue）
  - 音效（sfx）
  - 图像提示词（imagePrompt）

## 6. 扩展路线

- 接入真实 LLM（OpenAI/Anthropic 等）替代规则增强生成器。
- 引入任务队列，支持长任务异步生成与重试。
- 增加分镜图片生成功能、角色设定库、分镜时序编辑器。
- 增加评估回归测试，保障生成质量稳定。
