## 1. Episode Identity Normalization

- [x] 1.1 Extend pricing sync episode parsing to derive leading numeric identity from `episode.code` (for example `11WSP` -> `11`)
- [x] 1.2 Expose episode-derived numeric identity to row-level mapping flow in addition to existing set-number candidates
- [x] 1.3 Preserve behavior when episode code is missing or does not start with digits

## 2. Deterministic Mapping Cascade

- [x] 2.1 Update resolve-card logic to apply deterministic precedence: episode-derived numeric identity, numeric set fallbacks, edition code, external id
- [x] 2.2 Keep unresolved diagnostics informative by including episode and row identifiers used during matching
- [x] 2.3 Confirm existing successful mapping paths remain unchanged when episode-derived identity is not applicable

## 3. Regression Tests With Fixture-like Payloads

- [x] 3.1 Add unit test covering `episode.code=11WSP` + `card_number` mapping success
- [x] 3.2 Add unit test covering no-leading-digit episode code to verify fallback sequence
- [x] 3.3 Add test using provider row shape aligned with [src/test/resources/retourPriceAPI.json](src/test/resources/retourPriceAPI.json) fields (`episode.code`, `card_number`, nested prices)

## 4. Validation

- [x] 4.1 Run targeted pricing sync tests with local Maven binary at `C:\USINE_LOGICIELLE\apache-maven\bin\mvn.cmd`
- [x] 4.2 Confirm no regression in existing pricing sync integration tests
- [x] 4.3 Capture final verification notes in change artifacts before apply

## 5. Manual Pricing Import Simulation (Admin Tool)

- [x] 5.1 Add `PricingSyncService.applyManualPricingImport` to apply pricing from a manually supplied JSON payload, reusing the existing deterministic mapping cascade, without calling the provider or consuming budget
- [x] 5.2 Add admin endpoint `POST /api/admin/pricing/simulate-import`
- [x] 5.3 Add temporary admin UI section (clearly marked as temporary) to paste a provider-shaped JSON payload and trigger the simulated pricing update

## 6. Cardmarket Price Field Priority Fix

- [x] 6.1 Prioritize `prices.cardmarket.lowest_near_mint_FR_EU_only` over generic average/other price fields when extracting a card's market price
- [x] 6.2 Fall back to existing generic price extraction rules when that field is absent
- [x] 6.3 Add regression test proving the priority field wins over average and other marketplace prices
- [x] 6.4 Update fixture-based test to assert the exact expected price value instead of a non-null check

## 7. Final Validation

- [x] 7.1 Re-run targeted pricing sync tests with local Maven binary after adding the admin tool and the price field fix
- [x] 7.2 Confirm no regression across unit and integration pricing sync tests

## 8. Verification Notes

- Focused command executed:
	- `C:\USINE_LOGICIELLE\apache-maven\bin\mvn.cmd -B -Dtest=PricingSyncServiceTest,PricingSyncIntegrationTest test -q`
- Result: `EXITCODE:0`
- Surefire summary (final):
	- `PricingSyncServiceTest`: 10 tests, 0 failures, 0 errors
	- `PricingSyncIntegrationTest`: 4 tests, 0 failures, 0 errors
