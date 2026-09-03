## 1. Backend

- [x] 1.1 Add `KEY_LOG_ABNORMAL_PRICE_ENABLED`, `KEY_LOG_ABNORMAL_PRICE_THRESHOLD`, `KEY_LOG_ABNORMAL_PRICE_RARITIES` constants in `PricingSettingsService`.
- [x] 1.2 Add `isAbnormalPriceLogEnabled()` (default `false`), `getAbnormalPriceLogThreshold()` (default `5`, clamped to `>= 0`), and `getAbnormalPriceLogRarities()` (returns lowercased/trimmed `Set<String>` parsed from CSV, default `"Common,Uncommon,rare,Super_rare"`).
- [x] 1.3 In `PricingSyncService.applyPricingFromProviderRows`, add an independent check next to the existing high-price log: if enabled, the row's `rarity` (lowercased) is in the configured rarity set, and `row.price` exceeds the configured threshold, log `"Abnormal price detected for low rarity card (cardId=..., externalId=..., rarity=..., computedPrice=..., providerRow=...)"` at INFO.

## 2. Admin UI

- [x] 2.1 In `app.js`, read `pricing_log_abnormal_price_enabled` (default `"false"`), `pricing_log_abnormal_price_threshold` (default `"5"`), and `pricing_log_abnormal_price_rarities` (default `"Common,Uncommon,rare,Super_rare"`).
- [x] 2.2 Add a new grid row with a Oui/Non `<select>`, a numeric `<input min="0">`, and a text `<input>` for the CSV rarity list, in the pricing settings panel.
- [x] 2.3 Wire the save handler to call `api.updateSetting(...)` for all three new keys.

## 3. Tests

- [x] 3.1 Add `PricingSettingsServiceTest` coverage for the three new getters: defaults, explicit values, negative-threshold clamping, and CSV parsing (whitespace, mixed casing, empty tokens).
- [x] 3.2 Add `PricingSyncServiceTest` coverage: alert fires for a matching low-rarity row above threshold; does not fire when disabled; does not fire for a rarity outside the configured list (e.g. `Enchanted`); does not fire below/at threshold; fires independently of the existing high-price log when both conditions are met simultaneously.

## 4. Validation

- [x] 4.1 Run `PricingSettingsServiceTest` and `PricingSyncServiceTest` to confirm no regressions.
- [x] 4.2 Run `openspec validate pricing-abnormal-low-rarity-price-log --strict` before archiving.

## 5. Documentation

- [x] 5.1 Update README pricing settings section documenting the three new settings, their defaults, and where to configure them in the admin UI.
- [x] 5.2 Add a CHANGELOG entry under `[Unreleased]` describing the new alert log.
