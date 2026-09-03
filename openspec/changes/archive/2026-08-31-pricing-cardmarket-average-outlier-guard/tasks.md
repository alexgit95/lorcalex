## 1. Implementation

- [x] 1.1 In `PricingSyncService`, add a private helper computing the reference median from the row's cardmarket fields: the 8 regional `lowest_near_mint*` fields plus `7d_average` and `30d_average`, excluding null and zero values; return `Optional<BigDecimal>` (empty if fewer than 5 values are available).
- [x] 1.2 Add a private helper `isPlausibleAverage(value, medianOpt)`: returns `true` if `medianOpt` is empty (no guard possible) or if `value` is within `[median/5, median*5]` inclusive; returns `false` otherwise.
- [x] 1.3 In `extractCardmarketPreferredPrice`, compute the reference median once per row, then gate only the `7d_average` and `30d_average` candidates with `isPlausibleAverage` (in addition to their existing presence/currency checks) before accepting them; a rejected candidate falls through to the next candidate in the existing loop, unchanged.
- [x] 1.4 Leave `lowest_near_mint_FR`, `lowest_near_mint_FR_EU_only`, `lowest_near_mint`, and `tcg_player.market_price` evaluation exactly as-is (no guard).

## 2. Tests

- [x] 2.1 Add a test: row with `7d_average` far above the median of consistent `lowest_*` fields is rejected, and the price falls through to `lowest_near_mint_FR` (or the next usable candidate).
- [x] 2.2 Add a test: row with both `7d_average` and `30d_average` implausible relative to the median falls through to `lowest_near_mint_FR`.
- [x] 2.3 Add a test: row with fewer than 5 usable pooled values (median pool below minimum) still uses `7d_average` as-is (no guard applied) — regression test preserving current behavior for thin/sparse rows, including the pre-existing 3-value case that previously would have been wrongly rejected.
- [x] 2.4 Add a test: value exactly equal to `median * 5` is accepted, not rejected (boundary inclusive).
- [x] 2.5 Add a test: zero-valued `lowest_*` fields are excluded from the median pool (don't skew the computed median), while a zero value used as a final accepted candidate elsewhere still resolves to zero (existing "zero is legitimate" behavior unaffected).
- [x] 2.6 Add a regression test confirming the normal cascade (no implausible average present) behaves exactly as before this change.

## 3. Validation

- [x] 3.1 Run `PricingSyncServiceTest` (and any other pricing extraction tests) to confirm no regressions.
- [x] 3.2 Run `openspec validate pricing-cardmarket-average-outlier-guard --strict` before archiving.

## 4. Documentation

- [x] 4.1 Update README (pricing extraction / cardmarket priority section) documenting the plausibility guard, the reference median composition, the 5-value minimum pool, and the ×5 rule with the boundary behavior.
- [x] 4.2 Add a CHANGELOG entry under `[Unreleased]` describing the fix for aberrant average-based prices.
