## Context

`PricingSyncService.extractPriceFromRow()` currently calls `extractCardmarketPreferredPrice()`, which checks `prices.cardmarket.lowest_near_mint_FR_EU_only` first (no currency check), then falls back to `prices.cardmarket.lowest_near_mint` (with a currency check via `isAcceptableCardmarketCurrency`). If both are unavailable, it falls back to a broad generic recursive scan (`extractPriceNode` over the whole row) that matches loosely-named keys (`price`, `value`, `avg`, `average`, `low`, `high`, etc.) anywhere in the payload, including inside `tcg_player`/`cardmarket` containers.

In practice this has produced inaccurate prices: the generic fallback can pick up unintended fields, and the current precedence (near-mint snapshot prices) doesn't reflect recent transaction trends as well as short/medium-term averages would.

## Goals / Non-Goals

**Goals:**
- Replace the price source priority with an explicit, ordered list of exactly 6 candidates, each currency-checked before use.
- Remove the generic key-scan fallback for this extraction path, so unresolved prices are explicit (`UNRESOLVED_PRICE`) rather than guessed from an arbitrary matching key.
- Keep the extraction entry point (`extractPriceFromRow`) and its return shape unchanged so callers (`applyPricingFromProviderRows`) don't need to change.

**Non-Goals:**
- Not changing the `RapidApiPricingProviderClient.extractPrice()` path used by the single-card `fetchPrice()` flow — this change is scoped to the bulk provider-row mapping path used during paginated set sync (the path this fixture and rule apply to).
- Not changing currency configuration (`pricingSettingsService.getProviderCurrency()`), rounding (`HALF_UD` to 2 decimals), or downstream card update logic (`applyPricingFromProviderRows`).
- Not adding a way to see/audit which of the 6 sources was actually used per card (out of scope; could be a follow-up).
- Not changing the existing high-price debug logging in `applyPricingFromProviderRows()` (`HIGH_PRICE_LOG_THRESHOLD`, strictly greater than 5 EUR, logs the raw provider row for manual verification). This lives outside the extraction methods touched by this change and is preserved exactly as-is, including its strict `>` (not `>=`) comparison — confirmed with the user as intentionally unchanged.

## Decisions

- **Replace `extractCardmarketPreferredPrice()` with an ordered candidate list.** Implement as an ordered sequence of (value-extractor, currency-extractor) pairs evaluated in order; return the first candidate whose value is present and whose currency equals the configured provider currency:
  1. `cardmarket.7d_average` (currency: `cardmarket.currency`)
  2. `cardmarket.30d_average` (currency: `cardmarket.currency`)
  3. `cardmarket.lowest_near_mint_FR` (currency: `cardmarket.currency`)
  4. `cardmarket.lowest_near_mint_FR_EU_only` (currency: `cardmarket.currency`)
  5. `cardmarket.lowest_near_mint` (currency: `cardmarket.currency`)
  6. `tcg_player.market_price` (currency: `tcg_player.currency`)
  Rationale: matches the user's explicit ordering; a single ordered loop is simpler and easier to test than the previous two-branch special-case structure, and naturally supports removing the old FR_EU_only-first / no-currency-check quirk.
- **Currency check applies per-candidate, not per-container.** Each of the 5 cardmarket-sourced candidates re-checks `cardmarket.currency` (same value each time, but checked consistently); `tcg_player.market_price` checks `tcg_player.currency` independently. Rationale: the two price containers can in principle have different currencies; checking independently avoids assuming they're always in sync, and matches the user's explicit requirement ("toujours vérifier que c'est bien EUR").
- **Zero is a valid price.** Consistent with the existing "Zero is a legitimate cardmarket price" scenario, a candidate value of `0` (or `0.0`) is treated as present and usable, not as "missing" — only `null`/absent fields are skipped.
- **No fallback to the generic recursive scan.** If all 6 candidates are absent or currency-mismatched, `extractPriceFromRow` returns a `BigDecimalPrice` with `null` value, causing the existing `UNRESOLVED_PRICE` handling in `applyPricingFromProviderRows` to apply unchanged. Rationale: per user decision — an explicit "we don't know the price" is preferable to a value picked up from an arbitrary matching key that could be wildly wrong (e.g. `available_items` accidentally matching a loosely-named key in some future payload shape).
- **`isLikelyPriceKey` / `isLikelyPriceContainerKey` / generic `extractPriceNode` recursive-scan branch is removed from this call path** (dead code for `extractPriceFromRow`), but the helper `extractPriceNode(node, fromLikelyPriceContext)` for reading a single scalar/currency field may be reused internally for reading the 6 named fields, since it already handles Number/String parsing.

## Risks / Trade-offs

- [Risk] Removing the generic fallback means rows with unusual/unexpected payload shapes (not matching any of the 6 known fields) will now be `UNRESOLVED_PRICE` instead of possibly resolving via the old fallback → Mitigation: this is the explicit trade-off requested — explicit unresolved beats a potentially wrong guessed price; unresolved rows are already tracked via existing telemetry (`statusCounts.merge("UNRESOLVED_PRICE", ...)`, `priceSamples`).
- [Risk] `7d_average`/`30d_average` are averages, not point-in-time prices, so the resulting `marketPrice` semantics shift slightly (recent average vs. current near-mint listing) → Mitigation: this is the explicit intent of the change (address inaccurate prices by preferring recent-transaction averages); documented in README/CHANGELOG as part of this change's task list.
- [Trade-off] This only touches the bulk provider-row path, not `RapidApiPricingProviderClient.extractPrice()`; the two extraction paths will diverge further in behavior → accepted since the proposal explicitly scopes this to the path exercised by the paginated set-sync/fixture in question.

## Open Questions

None — priority order, per-candidate currency checks, and removal of the generic fallback were explicitly confirmed with the user.
