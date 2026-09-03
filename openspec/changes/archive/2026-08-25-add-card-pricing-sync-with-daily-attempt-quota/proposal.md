## Why

The application currently has no card valuation capability, while users need up-to-date card values at collection scale (thousands of cards). We need a cost-safe pricing synchronization model that never exceeds a configurable daily call budget and prioritizes high-value refresh order.

## What Changes

- Add card pricing support backed by an external provider (RapidAPI Lorcana Prices API by TCGGO).
- Add local strict daily budget enforcement based on attempted outbound calls.
- Add backlog-first refresh strategy:
  - first cards without any value
  - then cards with oldest last update timestamp.
- Add daily background pricing job that processes cards until budget is exhausted.
- Add admin-configurable settings for pricing sync behavior and quotas.
- Add operational visibility for pricing progress and daily budget usage.
- Add compatibility and regression tests for quota, prioritization, and persistence semantics.
- Ensure import/export preserves pricing value fields and last pricing scan timestamp used by refresh strategy.
- Update README and changelog as mandatory release documentation.

## Capabilities

### New Capabilities
- `card-pricing-storage`: Store card value metadata (value, currency, source, last update, status).
- `pricing-attempt-budget-enforcement`: Enforce strict daily budget using attempted outbound requests as the billable unit.
- `pricing-refresh-prioritization`: Prioritize cards with missing values first, then oldest refreshed cards.
- `pricing-background-sync`: Run a recurring background pricing synchronization process with stop-on-budget behavior.
- `pricing-admin-controls-and-observability`: Configure pricing settings and expose sync/usage status to admin.
- `pricing-provider-integration`: Integrate external Lorcana price provider with resilient request handling.

### Modified Capabilities
- `release-documentation-discipline`: Add explicit requirement that pricing behavior changes MUST update README and CHANGELOG.
- `import-export-compatibility-contract`: Extend compatibility contract to include pricing fields in backup/export payload evolution.

## Impact

- Backend model and persistence:
  - card schema extensions for pricing fields
  - optional pricing sync state fields/settings.
- Services and orchestration:
  - new pricing service and background worker/scheduler.
- Admin API/UI:
  - settings for quota and sync behavior
  - status endpoints and reporting.
- Tests:
  - new unit/integration tests for quota accounting, selection ordering, restart safety, and provider failures.
- Documentation:
  - README pricing section
  - changelog release entry.
