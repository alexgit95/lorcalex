## Why

A resolved card price above a few euros is unusual for provider-reported low rarities (`Common`, `Uncommon`, `rare`, `Super_rare` in the provider's own vocabulary) — such cases often signal a mapping bug (wrong local card matched) or a price extraction bug (wrong source/currency picked), rather than a genuine market spike. Today only a generic "High market price detected" log exists, which fires for any rarity and doesn't distinguish "expected to be expensive" (Legendary/Epic/Iconic/Enchanted) from "suspicious for its rarity". A dedicated alert log, independent from the generic one, lets admins catch data-quality issues without being drowned in expected high prices for premium rarities.

## What Changes

- Add a new, independent admin-configurable log that fires when a resolved card's price exceeds a configurable threshold (default 5 EUR) AND the provider row's `rarity` field is in a configurable, admin-editable list (default: `Common,Uncommon,rare,Super_rare`, matched case-insensitively).
- The log includes the card identity, rarity, computed price, and the raw provider row (API return), at INFO level.
- Three new independent admin settings: enable/disable toggle, price threshold, and the comma-separated rarity list — all editable from the admin UI, following the existing pricing debug settings pattern.
- Applies to both the scheduled/manual pricing sync and the manual JSON paste import tool, since both share `applyPricingFromProviderRows`.
- This is fully independent from the existing `pricing_log_high_price_enabled`/`pricing_log_high_price_threshold` settings — no shared state, no change to that log's behavior.

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `pricing-admin-controls-and-observability`: adds a new admin-configurable pricing debug logging requirement (abnormal low-rarity price alert), alongside the existing high-price and unresolved-mapping logging toggles.

## Impact

- Affected code: `PricingSettingsService` (three new setting keys + getters), `PricingSyncService.applyPricingFromProviderRows` (new independent check next to the existing high-price log), `app.js` admin pricing settings panel (new toggle + numeric input + text input).
- No new API endpoints — reuses the existing generic `updateSetting` mechanism.
- Documentation: README (pricing settings section) and CHANGELOG updated per `release-documentation-discipline`.
