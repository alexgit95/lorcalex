## Why

The current pricing sync model consumes one outbound call per card, which cannot keep catalog prices up to date under a strict 100-calls-per-day API contract. We need a set-paginated retrieval model that updates many cards per call while enforcing strict daily and per-minute safety limits.

## What Changes

- Replace card-by-card provider sync with paginated set discovery and paginated per-set card retrieval using the provider episodes endpoints.
- Enforce hard outbound API safeguards:
  - never exceed 100 calls per day
  - never exceed 30 calls per minute
  - keep a configurable daily safety margin below the hard daily cap.
- Add persistent sync cursor/checkpoint state to support safe multi-day continuation across restarts.
- Apply pricing update prioritization in this order:
  - cards without price
  - cards with last price older than 7 days
  - remaining cards.
- Add observability for pagination progress, cursor position, throttling state, and remaining daily budget.
- Keep manual and scheduled runs aligned to identical quota and throttling guards.

## Capabilities

### New Capabilities
- `pricing-set-paginated-sync`: Provider orchestration for paginated episodes and paginated episode cards with durable continuation cursor.
- `pricing-api-throttling-safety`: Strict outbound call governance with daily hard cap, daily safety margin, and per-minute hard cap.

### Modified Capabilities
- `pricing-provider-integration`: Provider lookup model changes from single-card search to set and set-card pagination endpoints.
- `pricing-refresh-prioritization`: Priority policy changes to three-tier recency model (no price, older than 7 days, remainder).
- `pricing-background-sync`: Run semantics change to multi-day continuation with checkpoint resume while preserving schedule/manual parity.
- `pricing-admin-controls-and-observability`: Status and controls expand to include throttle/budget safety settings and cursor telemetry.
- `pricing-attempt-budget-enforcement`: Budget accounting changes from card-attempt semantics to outbound API-call semantics with reserved safety margin.

## Impact

- Backend services:
  - pricing sync orchestration
  - provider client contract and mapping
  - settings persistence for limits and cursor state.
- Data and settings:
  - new app settings keys for safety margin, per-minute cap, and pagination cursor fields.
- Admin API/UI:
  - pricing status payload enriched with pagination/cursor/throttle telemetry and effective daily operational budget.
- Tests:
  - unit/integration coverage for strict cap enforcement, minute throttling, cursor resume, and priority ordering.
- Documentation:
  - README and CHANGELOG updates for new pricing sync behavior and operational constraints.
