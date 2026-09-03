## Why

`PricingSyncService` currently emits a `"High market price detected"` log line unconditionally, and it produces at most 3 sample diagnostics for `UNRESOLVED_MAPPING` rows out of potentially hundreds per sync run (e.g. `statusCounts={UNRESOLVED_MAPPING=138}`). Debugging mapping failures today requires re-running with extra instrumentation. Admins need a way to turn on a full, one-line-per-card diagnostic for unresolved mappings when investigating, and to independently silence/enable the high-price log, without a redeploy.

## What Changes

- Add an admin-configurable toggle to enable/disable the existing `"High market price detected"` log line (defaults to current always-on behavior).
- Add a new admin-configurable toggle to enable/disable a per-card diagnostic log for every `UNRESOLVED_MAPPING` row (not just the first 3 samples), emitted at INFO level to the application logs. Each log line includes the raw provider row payload and the mapping lookup criteria attempted (editionCode, setNumber, cardNumber, episodeCodeSetNumber, externalId).
- Both toggles are independent, persisted via the existing `AppSettings` key/value mechanism (same pattern as `pricing_sync_enabled`), and exposed in the admin UI.

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `pricing-admin-controls-and-observability`: adds two new admin-configurable logging toggles (high market price log, unresolved mapping diagnostic log) with persisted settings and admin UI controls.
- `pricing-background-sync`: unresolved mapping handling now supports an optional full per-card diagnostic log line (beyond the existing 3-sample cap used in the sync report), gated by an admin setting.

## Impact

- `PricingSettingsService`: two new setting keys/getters.
- `PricingSyncService`: conditional logging around `"High market price detected"` and around each `UNRESOLVED_MAPPING` occurrence in `applyPricingFromProviderRows`.
- `app.js` (admin UI): two new Oui/Non toggles wired to `api.updateSetting(...)`.
- No changes to API contracts, database schema, or existing sync report fields (`mappingSamples` stays capped at 3).
