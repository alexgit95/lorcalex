## 1. Backend

- [x] 1.1 Add `id` field to `CollectionValueTrendPointDTO`.
- [x] 1.2 In `CollectionValueTrendService.getTrend()`, populate `point.setId(snapshot.getId())`.
- [x] 1.3 Add `deleteByRecordedAt(LocalDateTime recordedAt)` to `EditionValueSnapshotRepository`.
- [x] 1.4 Add `@Transactional CollectionValueTrendService.deleteSnapshot(Long snapshotId)`: look up the `CollectionValueSnapshot`, throw/return not-found if absent, otherwise delete it and call `editionValueSnapshotRepository.deleteByRecordedAt(snapshot.getRecordedAt())`.
- [x] 1.5 Add `DELETE /api/pricing/trend/{snapshotId}` to `PricingController`, calling `deleteSnapshot`, returning a not-found error response when the snapshot doesn't exist (mirroring the existing `removePrice` error-handling shape).

## 2. Frontend

- [x] 2.1 In the Pricing tab (`app.js`), add a collapsed-by-default disclosure ("Historique détaillé ▸") below the trend chart.
- [x] 2.2 Render a table inside the disclosure from the existing `trendPoints` data (date, valeur, bouton supprimer per row) — no extra API call needed.
- [x] 2.3 Wire the delete button to a confirmation prompt, then `DELETE /api/pricing/trend/{id}` on confirm, then refresh the trend chart and history table (and edition deltas) on success.
- [x] 2.4 Add the `deleteTrendPoint` (or equivalent) call to the `api` client wrapper used elsewhere in `app.js`.

## 3. Tests

- [x] 3.1 `CollectionValueTrendServiceTest`: `getTrend()` includes the snapshot id; `deleteSnapshot()` deletes the collection snapshot and all edition snapshots sharing its `recordedAt`, leaves snapshots at other timestamps untouched, and handles a non-existent id.
- [x] 3.2 `PricingControllerTest` (or equivalent integration test): `DELETE /api/pricing/trend/{id}` returns success and removes the point from a subsequent `GET /trend`; returns a not-found error for an unknown id.
- [x] 3.3 Integration test confirming `getEditionDeltas()` falls back to a different reference snapshot after the closest one is deleted (no manual recalculation required).

## 4. Validation

- [x] 4.1 Run the pricing-related test suites (`CollectionValueTrendServiceTest`, `PricingControllerTest`, `PricingInsightsIntegrationTest`) to confirm no regressions.
- [x] 4.2 Run `openspec validate collection-value-trend-point-deletion --strict` before archiving.

## 5. Documentation

- [x] 5.1 Update README (Pricing tab / valuation section) documenting the new history list and deletion behavior, including that it is irreversible and also removes correlated edition-level snapshots.
- [x] 5.2 Add a CHANGELOG entry under `[Unreleased]` describing the new trend point deletion feature.
