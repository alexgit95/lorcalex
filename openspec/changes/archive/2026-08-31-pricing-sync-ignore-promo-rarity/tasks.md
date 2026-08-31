## 1. Implementation

- [x] 1.1 In `PricingSyncService.applyPricingFromProviderRows`, add a case-insensitive check on the row's `rarity` field (`"Promo"`) at the start of the per-row loop, before `resolveCard` is called.
- [x] 1.2 When the check matches, skip the row entirely: do not call `resolveCard` or `extractPriceFromRow`, do not touch `unresolved`, `statusCounts`, `mappingSamples`, or `priceSamples` for that row, and do not log a diagnostic line for it.
- [x] 1.3 Verify no other counters in `runSync` (`resolvedMappings`, `unresolvedMappings`, `unresolvedCount`, `unresolvedDiagnosticLogs`) are incremented as a side effect of skipped promo rows.

## 2. Tests

- [x] 2.1 Add a unit/integration test asserting a provider row with `rarity: "Promo"` (and a case variant, e.g. `"promo"`) produces no `Card` update, no `UNRESOLVED_MAPPING` status count, and no mapping/price diagnostic sample.
- [x] 2.2 Add a regression test confirming a non-promo unresolved row (e.g. no matching card, non-promo rarity) still increments `UNRESOLVED_MAPPING` and appears in diagnostic samples as before.
- [x] 2.3 Add a test covering `applyManualPricingImport` with a promo row in the pasted JSON to confirm the same skip behavior applies to the manual import path.

## 3. Validation

- [x] 3.1 Run the pricing sync test suite (`PricingSyncServiceTest` and related) to confirm no regressions.
- [x] 3.2 Run `openspec validate pricing-sync-ignore-promo-rarity --strict` before archiving.

## 4. Documentation

- [x] 4.1 Update README pricing section to document that provider rows with `rarity: Promo` are ignored before mapping and never appear in sync report counters/samples.
- [x] 4.2 Add a CHANGELOG entry under `[Unreleased]` describing the pricing sync behavior change.
