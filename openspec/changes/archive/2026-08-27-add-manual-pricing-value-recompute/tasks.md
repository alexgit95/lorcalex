## 1. Backend endpoint

- [x] 1.1 Add `POST /api/pricing/recompute-value` to `PricingController`, calling `CollectionValueTrendService.persistSnapshotFromCurrentCollection()`
- [x] 1.2 On success, return an acknowledgement response (e.g. `{ "success": true }`)
- [x] 1.3 On failure, catch the exception and return an error response including the exception message and root cause (e.g. `{ "message": ..., "rootCause": ... }`)

## 2. Frontend button and interactions

- [x] 2.1 Add a "Recalculer" button in `renderPricingPage()` next to the "Valeur totale (EUR)" stat card
- [x] 2.2 Wire the button click to call the new endpoint via the `api` client
- [x] 2.3 Disable the button and show a loading label (e.g. "⏳ Recalcul…") while the request is in flight; re-enable it once the request settles (success or error)
- [x] 2.4 On success, show a toast confirmation and re-fetch/re-render `getPricingInsights`, `getTrend`, and `getEditionDeltas`
- [x] 2.5 On failure, display the error message and root cause to the user

## 3. Verification

- [x] 3.1 Add/update a test covering the new endpoint (success path, and error path returning message + root cause)
- [x] 3.2 Manually verify the Pricing tab: click button, confirm loading state, toast on success, and that stats/chart/edition table refresh

## 4. Documentation

- [x] 4.1 Update README with the new manual recompute button/endpoint behavior
- [x] 4.2 Add a CHANGELOG entry describing the pricing impact of this change
