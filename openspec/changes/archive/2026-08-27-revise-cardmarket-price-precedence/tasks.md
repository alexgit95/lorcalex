## 1. Backend price extraction

- [x] 1.1 Replace `extractCardmarketPreferredPrice()` in `PricingSyncService.java` with an ordered evaluation of the 6 candidates (`7d_average`, `30d_average`, `lowest_near_mint_FR`, `lowest_near_mint_FR_EU_only`, `lowest_near_mint`, `tcg_player.market_price`), each with its own currency check
- [x] 1.2 Update `extractPriceFromRow()` so it no longer falls back to the generic recursive scan (`extractPriceNode(payload, false)`) when no ordered candidate is usable
- [x] 1.3 Remove or mark unused the now-dead generic fallback helpers (`isLikelyPriceKey`, `isLikelyPriceContainerKey`) if no longer referenced elsewhere in `PricingSyncService.java`
- [x] 1.4 Ensure zero values are treated as usable (not skipped as "missing") for every candidate, consistent with existing zero-price handling

## 2. Tests

- [x] 2.1 Update `PricingSyncServiceTest.java` test `runSync_prefersCardmarketLowestNearMintFrEuOnly` (and any similarly-named tests) to reflect the new priority order (e.g. `7d_average` now wins over `lowest_near_mint_FR_EU_only`)
- [x] 2.2 Add a test verifying priority order across all 6 candidates (e.g. only `lowest_near_mint` present and valid → used; only `tcg_player.market_price` present and valid → used)
- [x] 2.3 Add a test verifying a candidate with mismatched currency is skipped in favor of the next candidate
- [x] 2.4 Add a test verifying that when none of the 6 candidates are usable, the row is marked `UNRESOLVED_PRICE` (no generic fallback)
- [x] 2.5 Add a test verifying zero is used as a legitimate price when it is the first usable candidate
- [x] 2.6 Update/verify the fixture-based test using `retourPriceAPI.json` (`runSync_mapsFixtureLikeRowFromRetourPriceApi`) still passes with the new rule (note: the fixture card's `7d_average` will now be preferred over `lowest_near_mint`)
- [x] 2.7 Verify the existing high-price debug logging in `applyPricingFromProviderRows()` (`HIGH_PRICE_LOG_THRESHOLD`, strictly greater than 5 EUR, logs the raw provider row) still triggers correctly with prices resolved via the new priority order — left unchanged (`>`, not `>=`) as confirmed with the user

## 3. Documentation

- [x] 3.1 Update README's pricing sync section to describe the new price source priority order
- [x] 3.2 Add a CHANGELOG entry (marked as a breaking behavior change) describing the new price extraction priority order and removal of the generic fallback
