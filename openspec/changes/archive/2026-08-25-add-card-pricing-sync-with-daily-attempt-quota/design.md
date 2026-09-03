## Context

Lorcalex already has asynchronous long-running ingestion patterns in `LorcaJsonService` and app-level settings persisted in `app_settings`. The pricing feature must integrate without breaking current flows and must be financially safe under provider billing constraints.

The strongest product constraints from discovery are:
- never rely on provider 429 for cost control
- count attempts only as the consumed unit
- no fixed freshness SLA; prioritize oldest updates after filling missing values.

## Goals / Non-Goals

**Goals:**
- Add provider-backed card prices at scale.
- Guarantee strict local daily budget enforcement based on attempted calls.
- Ensure deterministic refresh prioritization order.
- Persist usage counters so restarts cannot cause accidental over-consumption.
- Provide admin configuration and visibility.
- Keep release governance: README/changelog updates and compatibility tests.

**Non-Goals:**
- Real-time per-card pricing on every UI request.
- Multi-provider arbitration at first delivery.
- Dynamic SLA freshness windows.

## Decisions

1. Billing unit and quota accounting
- Decision: one outbound provider attempt consumes one budget unit.
- Rationale: safest interpretation against overbilling risk.
- Alternative rejected: count only successful calls.

2. Local hard-stop enforcement
- Decision: budget is enforced before dispatch; if `usedToday >= dailyBudget`, no request is sent.
- Rationale: deterministic no-overspend guarantee.
- Alternative rejected: post-hoc stop after provider response.

3. Daily counter persistence
- Decision: persist `usageDate` and `usedToday` in settings; update on every attempt.
- Rationale: restart-safe accounting.
- Alternative rejected: in-memory counters.

4. Refresh ordering
- Decision: process `lastPriceAt IS NULL` first, then `lastPriceAt ASC`.
- Rationale: maximizes coverage, then fairness by staleness.
- Alternative rejected: random or static set-based cycles.

5. Scheduling model
- Decision: daily scheduled job with optional manual trigger that obeys the same quota guard.
- Rationale: predictable operational behavior with override control.
- Alternative rejected: continuous loop runner.

6. Provider errors and 429
- Decision: 429/4xx/5xx/timeouts are error telemetry only; attempts still counted.
- Rationale: budget must reflect potential billable attempt.
- Alternative rejected: exempt failed attempts.

7. Data model shape
- Decision: add pricing metadata directly on Card for v1 (`marketPrice`, `priceCurrency`, `priceSource`, `lastPriceAt`, `lastPriceStatus`).
- Rationale: lower implementation complexity and immediate API availability.
- Alternative considered: separate snapshot table; deferred unless historical analytics become mandatory.

8. Documentation and compatibility discipline
- Decision: pricing-related contract changes require README + changelog updates and N/N-1 compatibility tests for backup/export.
- Rationale: preserve governance baseline established in prior change.

## Risks / Trade-offs

- [Provider identifier mismatch] card mapping may fail for some cards.
  -> Mitigation: deterministic fallback lookup strategy and explicit failure status per card.

- [Job duration vs budget] large catalogs may take many days to cycle.
  -> Mitigation: staleness-priority ordering and operator-adjustable daily budget.

- [Counter corruption] malformed settings could break accounting.
  -> Mitigation: strict setting validation and safe defaults with fail-closed dispatch guard.

- [Schema coupling] price fields on Card may limit future history use cases.
  -> Mitigation: keep source/time/status fields now and define migration path to snapshot table if needed.
