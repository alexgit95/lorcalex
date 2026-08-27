## Why

The current cardmarket price extraction rule (French near-mint price first, then generic near-mint, then a broad generic key-scan fallback) is producing frequent inaccurate prices in practice. The user wants a stricter, more predictable priority order based on recent transaction averages, with a hard currency check at every step and no silent fallback to guesswork.

## What Changes

- **BREAKING**: Replace the "Cardmarket French near-mint price precedence" rule with a new, strict ordered priority list. For a provider row, the market price is the first available value (in order) among:
  1. `prices.cardmarket.7d_average`
  2. `prices.cardmarket.30d_average`
  3. `prices.cardmarket.lowest_near_mint_FR`
  4. `prices.cardmarket.lowest_near_mint_FR_EU_only`
  5. `prices.cardmarket.lowest_near_mint`
  6. `prices.tcg_player.market_price`
- Each candidate is only used if its associated currency field (`prices.cardmarket.currency` for items 1-5, `prices.tcg_player.currency` for item 6) equals the configured provider currency (e.g. EUR). A candidate present but in the wrong currency is skipped, not treated as a currency violation that aborts the whole row.
- **BREAKING**: Remove the generic key-scan fallback (`isLikelyPriceKey` / `isLikelyPriceContainerKey` recursive search) for this code path. If none of the 6 ordered candidates are available/valid, the row is marked `UNRESOLVED_PRICE` — no further fallback is attempted.

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `pricing-provider-integration`: replaces the "Cardmarket French near-mint price precedence" requirement with a new ordered, currency-checked price-source priority rule, and removes the generic fallback for this extraction path.

## Impact

- Backend: `PricingSyncService.extractPriceFromRow()` / `extractCardmarketPreferredPrice()` (and related helpers `isAcceptableCardmarketCurrency`, `extractPriceNode`) in `PricingSyncService.java`.
- Tests: existing tests asserting `lowest_near_mint_FR_EU_only` precedence and generic-fallback behavior (e.g. in `PricingSyncServiceTest.java`) will need updating to match the new order.
- No database schema changes; affects only how `Card.marketPrice` is computed during pricing sync.
