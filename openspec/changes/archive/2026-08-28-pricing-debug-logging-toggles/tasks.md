## 1. Settings backend

- [x] 1.1 Add `KEY_LOG_HIGH_PRICE_ENABLED` and `KEY_LOG_UNRESOLVED_MAPPING_ENABLED` constants to `PricingSettingsService`.
- [x] 1.2 Add `isHighPriceLogEnabled()` (default `true`) and `isUnresolvedMappingLogEnabled()` (default `false`) getters, following the `isSyncEnabled()` pattern.

## 2. Sync logging behavior

- [x] 2.1 Extract (or reuse) the mapping-lookup diagnostic values (editionCode, setNumber, cardNumber, episodeCodeSetNumber, externalId) already computed in `resolveCard`/`buildRowDiagnostic` so they can be logged per unresolved row.
- [x] 2.2 Gate the existing `"High market price detected"` log line in `applyPricingFromProviderRows` behind `pricingSettingsService.isHighPriceLogEnabled()`.
- [x] 2.3 Add a per-card `log.info(...)` call for every `UNRESOLVED_MAPPING` row (not limited to 3 samples), gated behind `pricingSettingsService.isUnresolvedMappingLogEnabled()`, including the raw provider row and the lookup criteria attempted.
- [x] 2.4 Confirm the existing capped `mappingSamples` (3) behavior in the sync/manual-import report is unchanged.

## 3. Admin UI

- [x] 3.1 Add two new Oui/Non `<select>` controls in `app.js` pricing settings panel, next to `pricingSyncEnabled`, bound to `pricing_log_high_price_enabled` and `pricing_log_unresolved_mapping_enabled`.
- [x] 3.2 Wire save handler to call `api.updateSetting('pricing_log_high_price_enabled', ...)` and `api.updateSetting('pricing_log_unresolved_mapping_enabled', ...)`.

## 4. Tests

- [x] 4.1 Unit test: high price log emitted when setting enabled, suppressed when disabled.
- [x] 4.2 Unit test: unresolved mapping diagnostic log emitted once per unresolved row when setting enabled (verify count matches unresolved count, e.g. >3 rows), suppressed when disabled.
- [x] 4.3 Unit test: `mappingSamples` cap of 3 in the report remains unchanged regardless of the new setting's value.
- [x] 4.4 Verify default values preserve current behavior (high price log on by default, unresolved mapping log off by default) via `PricingSettingsService` tests.

## 5. Documentation

- [x] 5.1 Update README with the two new admin pricing settings (`pricing_log_high_price_enabled`, `pricing_log_unresolved_mapping_enabled`), their defaults, and where to toggle them in the admin UI.
- [x] 5.2 Add a CHANGELOG entry for the target release describing the new pricing debug logging toggles.
