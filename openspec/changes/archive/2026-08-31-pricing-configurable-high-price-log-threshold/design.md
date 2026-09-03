## Context

`PricingSyncService.applyPricingFromProviderRows` currently gates the `"High market price detected"` log line behind `pricingSettingsService.isHighPriceLogEnabled()` and compares against a hardcoded `HIGH_PRICE_LOG_THRESHOLD = BigDecimal.valueOf(5)`. Two prior changes (`revise-cardmarket-price-precedence`, `pricing-debug-logging-toggles`) explicitly kept this threshold value out of scope. This change reverses that decision at the user's request: the threshold becomes an admin-configurable integer setting, following the same `AppSettings` key/value mechanism already used for every other pricing setting.

## Goals / Non-Goals

**Goals:**
- Let admins change the high-price log threshold (EUR, integer) from the admin UI without a redeploy.
- Preserve current behavior by default (`5`, strict `>` comparison).
- Reuse the existing generic settings persistence/update path (`AppSettings` + `api.updateSetting`) — no new REST endpoints.

**Non-Goals:**
- Changing the comparison operator (`>` stays strict, not `>=`).
- Supporting decimal thresholds (e.g. `5.50`) — integer EUR only, per stakeholder decision.
- Changing what the log line contains, or the `isHighPriceLogEnabled()` on/off toggle semantics.
- Adding an upper bound on the threshold value.

## Decisions

- **New setting key `pricing_log_high_price_threshold`**, default `"5"`, following the existing naming convention (`pricing_log_high_price_enabled` already exists as its sibling toggle).
- **Getter `getHighPriceLogThreshold()` on `PricingSettingsService`**, mirroring `getDailySafetyMargin()` / `getMinuteLimit()`: `parseInt(getValueOrDefault(KEY, "5"), 5)` clamped to a minimum of `0` (`Math.max(0, parsed)`), no upper bound.
- **`PricingSyncService`**: remove the `HIGH_PRICE_LOG_THRESHOLD` constant; call `pricingSettingsService.getHighPriceLogThreshold()` at the point of comparison (same place the `isHighPriceLogEnabled()` check already happens), building a `BigDecimal` from the int for the existing `compareTo` call. Comparison remains strict `>`.
- **Admin UI**: add a `<input type="number" min="0">` next to the existing `pricingLogHighPriceEnabled` `<select>` in the same grid row (mirrors how `pricingDailySafetyMargin` sits next to other numeric settings), wired to `api.updateSetting('pricing_log_high_price_threshold', ...)` in the existing save handler.
- **Alternative considered — decimal threshold (`BigDecimal`/`step="0.01"` input)**: rejected per stakeholder decision; integer EUR keeps parity with other integer pricing settings (`pricing_minute_limit`, `pricing_daily_safety_margin`) and avoids adding a new decimal-parsing helper for a single setting.

## Risks / Trade-offs

- [Risk] Admin enters a negative value → mitigated by clamping to `0` in the getter (same defensive pattern as other numeric getters), independent of any client-side `min="0"` validation.
- [Risk] Existing deployments have no `pricing_log_high_price_threshold` row yet → default `"5"` preserves current behavior exactly; no migration needed.
