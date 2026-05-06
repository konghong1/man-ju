# Project defects review (2026-04-30)

This note lists practical gaps observed from a quick architecture/code review.

## 1) Job execution is synchronous and blocks request flow
- `createProject`/`rerunProject` eventually call `runAndPersist`, and `runAndPersist` invokes `harnessOrchestrator.run(...)` inline.
- There is no queue worker or async handoff in this path, so long-running generation can tie up request threads.

## 2) File persistence is non-atomic and lacks corruption tolerance
- Repository writes directly with `TRUNCATE_EXISTING` to the final JSON path.
- If the process crashes mid-write, partial files are possible and subsequent reads throw hard errors.

## 3) Retry/idempotency guardrails are weak for duplicate submissions
- New project/job IDs are always random UUID-derived identifiers.
- There is no idempotency key or duplicate request detection, so user retries can create duplicated jobs/projects.

## 4) No explicit concurrency control on project/job update races
- Project/job updates rely on full object overwrite saves.
- There is no optimistic version field / compare-and-swap check visible in service-layer updates.

## 5) Error surface is infrastructure-oriented in key storage paths
- Storage errors throw `IllegalStateException` with internal details.
- Without careful API translation, clients can receive opaque failures and operators get limited structured diagnostics.

## 6) Frontend quality gates appear under-specified from repo root workflow
- Root project has Maven tests, and frontend has a separate Node/Vite app.
- No single root-level orchestration/check target is obvious for unified CI quality gate execution.

