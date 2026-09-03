## Why

Since the `revise-cardmarket-price-precedence` change (2026-08-27), `cardmarket.7d_average` is tried first in the price-source cascade. In practice, the provider sometimes returns a wildly aberrant `7d_average` (and occasionally `30d_average` too) while every `lowest_near_mint*` field on the same row is consistent and plausible (e.g. `7d_average=199` while all 8 regional `lowest_*` fields read `~0.02`). Because averages are tried before any `lowest_*` field, this currently produces grossly incorrect `Card.marketPrice` values. A plausibility guard on the two average candidates — using the median of the row's own price fields as a reference — prevents this without changing the cascade's behavior for the normal case.

## What Changes

- Before evaluating `cardmarket.7d_average` and `cardmarket.30d_average` in the existing price-source cascade, compute a reference median from all currently-available cardmarket price fields on the row: the 8 regional `lowest_near_mint*` variants plus `7d_average` and `30d_average` themselves (zero values excluded from this pool, treated as absent).
- If the median can be computed (pool non-empty) and a candidate average's value falls outside `[median / 3, median × 3]` (inclusive at the boundary — exactly `median × 3` is still accepted), that candidate is treated as implausible and skipped, falling through to the next candidate in the existing cascade order.
- If the median cannot be computed (pool empty), `7d_average`/`30d_average` are used as-is — current behavior is preserved exactly for rows lacking enough data to sanity-check.
- No change to the cascade order itself, to the `lowest_near_mint_FR` / `lowest_near_mint_FR_EU_only` / `lowest_near_mint` candidates (no guard applied to these — they anchor the reference), or to `tcg_player.market_price` (last resort, always accepted as today, no guard).
- **BREAKING**: none for callers — `extractPriceFromRow`'s signature and return shape are unchanged; only the internal decision of which candidate is "usable" changes for rows with an implausible average.

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `pricing-provider-integration`: the "Cardmarket price source priority order" requirement is extended with a plausibility guard on the `7d_average`/`30d_average` candidates, based on the median of the row's available price fields.

## Impact

- Affected code: `PricingSyncService.extractCardmarketPreferredPrice()` (new median computation + guard applied to the first two candidates only), same method used by both scheduled/manual sync and the manual JSON import tool.
- No new settings, no schema changes.
- Tests: existing tests asserting the 7d/30d-first cascade will need a companion fixture with a consistent `lowest_*` set to exercise the new guard, alongside the existing "no guard interferes with normal cascade" cases.
- Documentation: README and CHANGELOG updated per `release-documentation-discipline` (pricing behavior change).
