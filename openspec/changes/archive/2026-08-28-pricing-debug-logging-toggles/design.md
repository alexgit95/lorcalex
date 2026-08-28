## Context

`PricingSyncService.applyPricingFromProviderRows` currently:
- Always logs `"High market price detected (cardId=..., externalId=..., price=..., providerRow=...)"` at INFO when a resolved card's price exceeds `HIGH_PRICE_LOG_THRESHOLD`.
- On `UNRESOLVED_MAPPING` (card lookup failure via `resolveCard`), only captures up to 3 diagnostic samples (`mappingSamples`) for inclusion in the sync report JSON. With large syncs (e.g. 138 unresolved mappings), most failures are invisible without re-running with extra tooling.

Admin-configurable behavior already exists via `PricingSettingsService`, backed by `AppSettingsRepository` (simple key/value string store), e.g. `KEY_SYNC_ENABLED = "pricing_sync_enabled"` with `isSyncEnabled()` defaulting via `parseBoolean(getValueOrDefault(...), true)`. The admin UI (`app.js`) exposes such toggles as a "Oui"/"Non" `<select>` wired to `api.updateSetting(key, value)`.

## Goals / Non-Goals

**Goals:**
- Let admins turn the `"High market price detected"` log on/off without a redeploy.
- Let admins turn on a new diagnostic log that emits one INFO line per `UNRESOLVED_MAPPING` card, containing the raw provider row and the mapping lookup criteria attempted, without the 3-sample cap.
- Persist both toggles using the existing `AppSettings` mechanism, independently of each other.

**Non-Goals:**
- Changing the `mappingSamples`/`priceSamples` cap (3) used in the manual-import and sync report JSON responses.
- Adding real SQL statement logging (e.g. Hibernate query logging) — the diagnostic reuses the same candidate-key values already computed by `resolveCard`/`buildRowDiagnostic`, not literal SQL.
- Changing the `HIGH_PRICE_LOG_THRESHOLD` value or price-resolution logic.

## Decisions

- **Two independent settings keys**, following the existing naming convention in `PricingSettingsService`:
  - `KEY_LOG_HIGH_PRICE_ENABLED = "pricing_log_high_price_enabled"` — default `true` (preserves current always-on behavior).
  - `KEY_LOG_UNRESOLVED_MAPPING_ENABLED = "pricing_log_unresolved_mapping_enabled"` — default `false` (avoids surprise log volume increase for existing deployments; admin opts in when debugging).
  - Alternative considered: a single combined "debug logging" toggle. Rejected because the user explicitly wants independent control per log type.
- **Getters on `PricingSettingsService`**: `isHighPriceLogEnabled()` and `isUnresolvedMappingLogEnabled()`, mirroring `isSyncEnabled()` (synchronized, `parseBoolean(getValueOrDefault(key, default), defaultBool)`).
- **Log level**: both remain at INFO (matching the existing high-price log level and the user's explicit request), gated by the toggle rather than by log level, so operators can enable/disable without touching logger configuration.
- **Unresolved mapping diagnostic line format**: one log call per unresolved row inside the existing loop in `applyPricingFromProviderRows`, e.g.:
  ```
  log.info("Unresolved mapping (editionCode={}, setNumber={}, cardNumber={}, episodeCodeSetNumber={}, externalId={}, providerRow={})",
          editionCode, setNumber, cardNumber, episodeCodeSetNumber, externalId, row);
  ```
  This reuses the same field extraction already performed inside `resolveCard`/`buildRowDiagnostic`, so no duplicate parsing logic is introduced — the relevant private helper is extended (or a shared extraction is factored out) to expose these five values for logging as well as for the capped `mappingSamples`.
- **No change to `mappingSamples` cap**: the new per-card log is independent of the JSON report; the report keeps returning only 3 samples to avoid bloating HTTP responses, while the log carries the full detail to the application log files.
- **Admin UI**: two new `<select>` (Oui/Non) controls added next to the existing `pricingSyncEnabled` control in `app.js`, each calling `api.updateSetting('pricing_log_high_price_enabled', ...)` / `api.updateSetting('pricing_log_unresolved_mapping_enabled', ...)` on save.

## Risks / Trade-offs

- [Risk] Enabling the unresolved-mapping log during a large sync (100+ unresolved rows) could still be noisy → Mitigation: toggle defaults to off; admin enables only while actively debugging, then disables again.
- [Risk] Logging the full raw `providerRow` per unresolved card could include large nested payloads → Mitigation: this already happens for the existing 3-sample cap and for the high-price log; behavior is consistent with existing logging conventions in this service.
- [Risk] Two independent toggles add minor settings surface area → Mitigation: follows an established, well-understood pattern (`AppSettings` key/value + admin select), low maintenance cost.

## Migration Plan

- Additive only: new settings keys default to values that preserve current behavior (`pricing_log_high_price_enabled=true`, `pricing_log_unresolved_mapping_enabled=false`). No data migration or backfill needed.
- No rollback concerns beyond reverting the code change; existing `AppSettings` rows for unrelated keys are unaffected.

## Open Questions

- None outstanding; defaults and toggle granularity were confirmed with the requester during exploration.
