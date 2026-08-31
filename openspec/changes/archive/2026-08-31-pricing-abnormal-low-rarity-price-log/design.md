## Context

`PricingSyncService.applyPricingFromProviderRows` already gates one debug log (`"High market price detected"`) behind an admin-configurable enable toggle and threshold (`pricing_log_high_price_enabled` / `pricing_log_high_price_threshold`, added in a prior change). That log fires for any rarity, so a genuinely expensive `Enchanted` or `Iconic` card is expected to trip it — it says nothing about whether a price is *surprising for its rarity*. Provider rows carry their own English `rarity` field (`Common`, `Uncommon`, `rare`, `Super_rare`, `Legendary`, `Epic`, `Iconic`, `Enchanted`, casing inconsistent per observed fixtures), separate from the local `Card.rarity` (French vocabulary). A price above a few euros for a `Common`/`Uncommon`/`rare`/`Super_rare` row is unusual and worth a dedicated alert, distinct from the general high-price log.

## Goals / Non-Goals

**Goals:**
- Add a fully independent admin-configurable alert log: enable toggle, price threshold (integer EUR), and the list of "low" rarities to watch, all editable from the admin UI without a redeploy.
- Log at INFO level, including card identity, rarity, computed price, and the raw provider row (API return) for manual verification.
- Apply the same check in both the scheduled/manual sync and the manual JSON import path (both go through `applyPricingFromProviderRows`).

**Non-Goals:**
- No coupling with `pricing_log_high_price_enabled`/`pricing_log_high_price_threshold` — separate settings, separate code path, no shared state or precedence rules between the two logs (both can fire independently for the same row).
- No validation/whitelist of rarity values against a fixed enum — the rarity list setting is free-text CSV, trusted as entered by the admin.
- No local `Card.rarity` (French) involvement — matching is only against the provider row's own `rarity` field, consistent with how the `Promo` row-skip feature already treats provider rarity as a separate vocabulary.

## Decisions

- **Three new independent setting keys** in `PricingSettingsService`, following the existing naming convention:
  - `KEY_LOG_ABNORMAL_PRICE_ENABLED = "pricing_log_abnormal_price_enabled"` — default `false` (opt-in, since this is a new alert with an implied default rarity list that admins may want to tune before turning on).
  - `KEY_LOG_ABNORMAL_PRICE_THRESHOLD = "pricing_log_abnormal_price_threshold"` — default `5`, same int-clamped-to-0 pattern as `getHighPriceLogThreshold()`.
  - `KEY_LOG_ABNORMAL_PRICE_RARITIES = "pricing_log_abnormal_price_rarities"` — default `"Common,Uncommon,rare,Super_rare"`, free-text CSV.
  - Alternative considered — default the enable toggle to `true` (matching `pricing_log_high_price_enabled`'s always-on-by-default precedent): rejected because this is a *new* signal an admin hasn't seen before; opt-in avoids surprising existing deployments with a new log line the first time they upgrade.
- **Getter `getAbnormalPriceLogRarities()`** returns a `Set<String>` of lowercased, trimmed, non-empty tokens split on `,` from the CSV setting (falling back to the default CSV when unset/blank). Comparison against a row's rarity is done by lowercasing the row's `rarity` value and checking set membership — mirrors the case-insensitive approach already used for the `Promo` row-skip check.
- **Insertion point**: a second, independent `if` block in the same loop in `applyPricingFromProviderRows`, right next to the existing high-price log check, reusing `row.price` and `row.rawRow` (no new data threading needed).
- **Log format**: `"Abnormal price detected for low rarity card (cardId={}, externalId={}, rarity={}, computedPrice={}, providerRow={})"` — mirrors the existing high-price log's structure for consistency, with `rarity` and `computedPrice` added.
- **Admin UI**: new grid row with a Oui/Non `<select>`, a `<input type="number" min="0">`, and a `<input type="text">` for the CSV rarity list, in the same pricing settings panel, wired to `api.updateSetting(...)` for each of the three keys.

## Risks / Trade-offs

- [Risk] Admin enters malformed CSV (extra commas, spaces, mixed casing) → mitigated by trimming, lowercasing, and filtering empty tokens when parsing; malformed entries simply won't match any row rather than causing an error.
- [Risk] The two independent price-threshold settings (`pricing_log_high_price_threshold` and `pricing_log_abnormal_price_threshold`) could both fire for the same row → acceptable per proposal scope; they are intentionally independent and may both log for the same event.
