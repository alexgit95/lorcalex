## 1. Backend

- [x] 1.1 Add `KEY_LOG_HIGH_PRICE_THRESHOLD = "pricing_log_high_price_threshold"` constant in `PricingSettingsService`.
- [x] 1.2 Add `getHighPriceLogThreshold()` getter: `parseInt(getValueOrDefault(KEY, "5"), 5)` clamped to a minimum of `0`.
- [x] 1.3 In `PricingSyncService.applyPricingFromProviderRows`, remove the `HIGH_PRICE_LOG_THRESHOLD` constant and use `pricingSettingsService.getHighPriceLogThreshold()` for the comparison, keeping the strict `>` operator.

## 2. Admin UI

- [x] 2.1 In `app.js`, read `pricing_log_high_price_threshold` (default `"5"`) alongside the other pricing settings values.
- [x] 2.2 Add a `<input type="number" min="0" id="pricingLogHighPriceThreshold">` next to the existing `pricingLogHighPriceEnabled` select.
- [x] 2.3 Wire the save handler to call `api.updateSetting('pricing_log_high_price_threshold', ...)`.

## 3. Tests

- [x] 3.1 Add/update `PricingSettingsServiceTest` covering `getHighPriceLogThreshold()` default, explicit value, and negative-value clamping to 0.
- [x] 3.2 Update `PricingSyncServiceTest` high-price log tests to stub `getHighPriceLogThreshold()` and verify the log fires/doesn't fire relative to a configured (non-default) threshold, and that the comparison stays strict (price equal to threshold does not log).

## 4. Validation

- [x] 4.1 Run `PricingSettingsServiceTest` and `PricingSyncServiceTest` to confirm no regressions.
- [x] 4.2 Run `openspec validate pricing-configurable-high-price-log-threshold --strict` before archiving.

## 5. Documentation

- [x] 5.1 Update README pricing settings section: document `pricing_log_high_price_threshold` (default `5`, integer EUR, no upper bound) next to the existing `pricing_log_high_price_enabled` entry.
- [x] 5.2 Add a CHANGELOG entry under `[Unreleased]` describing the new configurable threshold.
