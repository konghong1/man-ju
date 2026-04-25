# Task Node Status (2026-04-25)

This run validates each module by task node and records the result.

| Module | Node | Verification | Result |
|---|---|---|---|
| Core | `CORE-001` health check | `GET /api/health` | pass (`status=ok`) |
| Backend | `CORE-002` project create/read | `POST /api/projects` + `GET /api/projects/{id}` | pass |
| Backend | `BE-001/BE-002` job orchestration/status | `GET /api/jobs/{id}` | pass (`SUCCEEDED`) |
| Backend | `BE-007` version snapshot | `GET /api/projects/{id}/versions` | pass (`count=1`) |
| Backend | `BE-009` export | `GET /api/projects/{id}/export?format=json|markdown|prompt` | pass (`200`) |
| LLM | `LLM-014` provider management | `GET /api/llm/providers`, `PATCH /api/llm/providers/{provider}` | pass |
| LLM | `LLM-002` model catalog | `GET /api/llm/models` | pass (`count=7`) |
| LLM | `LLM-017` routing policy | `PUT /api/llm/routes/project/{projectId}` | pass (`S2_STORY_PLAN=gemini`) |
| LLM | `INF-002` runtime metrics | `GET /api/llm/metrics` | pass (metric rows returned) |
| Frontend | `FE-001` router startup | `npm run build` + `npm run preview` | pass |
| Frontend | `FE-003` workspace flow | browser open `/projects/{id}/workspace` | pass |
| Frontend | `FE-004` model settings + metrics | browser open `/settings/models` (backend API wired) | pass |
| QA | `QA-001~QA-003` regression | `mvn test -q` | pass |
| Ops | `OPS-001` init chain | `init.ps1` covers backend test + frontend build + dual-service health checks | ready |

## Browser Smoke Artifacts

- `target/browser-smoke/node-01-project-list.png`
- `target/browser-smoke/node-02-new-project.png`
- `target/browser-smoke/node-03-workspace.png`
- `target/browser-smoke/node-04-review.png`
- `target/browser-smoke/node-05-export.png`
- `target/browser-smoke/node-06-model-settings.png`

## API Node Artifact

- `target/task-node-results.json`

## Docker Middleware Check

The current project flow passes with local file storage plus built-in adapters.
No missing mandatory middleware was detected, so no new Docker middleware install was required in this run.
