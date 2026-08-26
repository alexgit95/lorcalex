## Context

The current pricing synchronization performs one provider request per card and consumes one daily budget unit per request. Under a strict external API contract (maximum 100 calls/day and 30 calls/minute), card-by-card retrieval cannot keep pace with catalog growth.

The provider exposes paginated endpoints for episodes (sets) and paginated cards per episode. This enables a higher card-updates-per-call ratio but requires new orchestration: pagination traversal, strict call governance, durable cursor checkpoints, and deterministic resume across days.

Primary operational constraints are strict and non-negotiable:
- never exceed 100 outbound provider calls per day
- never exceed 30 outbound provider calls per minute.

Product prioritization policy for refresh is explicitly changed to:
1. cards without price
2. cards with last price older than 7 days
3. remaining cards.

## Goals / Non-Goals

**Goals:**
- Move pricing retrieval to paginated set discovery and paginated set-card retrieval.
- Enforce strict daily and per-minute call limits with a safety margin below the daily hard cap.
- Persist cursor/checkpoint state so long runs continue safely across restarts and across days.
- Apply refresh priorities in the requested three-tier order (no price, stale > 7 days, rest).
- Keep scheduled and manual runs under identical guards and telemetry semantics.

**Non-Goals:**
- Realtime repricing on each UI view.
- Multi-provider arbitration or weighted provider blending.
- Historical time-series pricing snapshots beyond current card metadata fields.

## Decisions

1. Retrieval model: set-first pagination
- Decision: traverse `/episodes?page=n`, then for each episode traverse `/episodes/{id}/cards?page=n&per_page=100`.
- Rationale: maximizes cards updated per API call and directly matches provider-supported contracts.
- Alternative rejected: keep single-card `/cards/search` calls and tune budget only.

2. Daily budget model with safety margin
- Decision: enforce both `dailyHardLimit=100` and `dailyOperationalBudget = dailyHardLimit - dailySafetyMargin` with default safety margin > 0.
- Rationale: hard guarantee plus operational buffer for retries/control calls.
- Alternative rejected: use full 100 calls as working budget.

3. Per-minute rate limiting
- Decision: enforce a strict global minute limiter with hard ceiling at 30 calls/minute.
- Rationale: prevents accidental burst violations from loops/retries/concurrency.
- Alternative rejected: sleep-based best effort without central limiter.

4. Call accounting semantics
- Decision: every outbound provider HTTP request consumes one unit of daily call budget (including retries and non-2xx outcomes).
- Rationale: aligns with strict safety requirement and avoids undercounting.
- Alternative rejected: count only successful pages.

5. Cursor persistence and resume
- Decision: persist phase/page/set cursor and save progress during traversal; on next run continue from last durable checkpoint.
- Rationale: enables multi-day completion and restart safety without repeating completed pages.
- Alternative rejected: in-memory cursor only.

6. Priority policy execution
- Decision: apply update priority by local card state classes:
  - P0: no price
  - P1: last price older than 7 days
  - P2: remaining cards.
- Rationale: matches requested business policy and keeps current data fresh enough while focusing on high-need cards.
- Alternative rejected: oldest-first across all priced cards.

7. Set ordering strategy under fixed priority policy
- Decision: preserve deterministic set traversal order (provider page order) while applying card-level priority classes when processing page payloads.
- Rationale: requested policy defines card priority, not custom set-level ranking.
- Alternative rejected: dynamic set scoring/reordering.

8. Observability extension
- Decision: include cursor state, effective operational budget, minute limiter status, processed pages, and stop reason in pricing status outputs.
- Rationale: operations need clear visibility to validate hard-limit compliance and progress.
- Alternative rejected: keep only attempted/remaining counters.

## Risks / Trade-offs

- [Provider payload mismatch with local card identity] -> Mitigation: deterministic matching cascade (code+number, then controlled fallbacks) and unresolved telemetry.
- [Long catch-up windows on large catalogs] -> Mitigation: durable cursor + daily continuation; no reset of progress on restart.
- [Retry storms can consume budget quickly] -> Mitigation: bounded retries, retry counted as call, clear stop reasons when budget is near exhaustion.
- [Minute limiter contention if parallelism is introduced later] -> Mitigation: enforce one global limiter for all pricing outbound calls.
- [Operational confusion between hard limit and effective budget] -> Mitigation: expose both values and safety margin explicitly in admin status.
