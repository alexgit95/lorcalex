## Why

The `"High market price detected"` debug log currently fires whenever a resolved card price exceeds a hardcoded 5 EUR threshold (`HIGH_PRICE_LOG_THRESHOLD`). Admins can already turn this log on/off (`pricing_log_high_price_enabled`), but cannot adjust the threshold itself without a code change and redeploy. As the collection's price range grows, a fixed 5 EUR cutoff produces either too much noise (low-value catalogs) or misses interesting cases (high-value catalogs) — this should be tunable from the admin UI like the other pricing debug settings.

## What Changes

- Add a new admin-configurable setting for the high market price log threshold (integer, EUR), defaulting to `5` to preserve current behavior.
- Replace the hardcoded `HIGH_PRICE_LOG_THRESHOLD` constant in `PricingSyncService` with a read of this setting on each evaluation.
- Keep the existing strict `>` comparison (a price exactly equal to the threshold does not trigger the log).
- Add a numeric input in the admin UI next to the existing "Log High market price" Oui/Non control, validated to be an integer `>= 0` (no upper bound).

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `pricing-admin-controls-and-observability`: the high market price debug logging requirement is extended so the price threshold itself is admin-configurable, not just the on/off toggle.

## Impact

- Affected code: `PricingSettingsService` (new setting key + getter), `PricingSyncService.applyPricingFromProviderRows` (use getter instead of constant), `app.js` admin pricing settings panel (new numeric input + save wiring).
- No new API endpoints — reuses the existing generic `updateSetting` mechanism.
- Documentation: README (pricing settings section) and CHANGELOG updated per `release-documentation-discipline`.
