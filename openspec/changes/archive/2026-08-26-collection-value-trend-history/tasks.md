## 1. Backend snapshot persistence
- [x] 1.1 Add a persisted `CollectionValueSnapshot` entity for total collection valuation over time.
- [x] 1.2 Add a persisted `EditionValueSnapshot` entity or equivalent per-edition timeline table.
- [x] 1.3 Add repository methods to fetch latest snapshots ordered by timestamp descending.
- [x] 1.4 Add repository methods to fetch historical values for a given edition by date range.

## 2. Pricing sync integration
- [x] 2.1 After each successful pricing sync, recompute the total collection value in EUR.
- [x] 2.2 Persist the global value snapshot using the sync completion timestamp.
- [x] 2.3 Recompute per-edition valuations using the same formula: `(quantity + foilQuantity) x marketPrice`.
- [x] 2.4 Persist edition-level snapshots for the same timestamp.
- [x] 2.5 Skip snapshot creation when the pricing sync fails or is partially incomplete.

## 3. Trend calculation service
- [x] 3.1 Implement a service that returns the ordered global collection value trend.
- [x] 3.2 Implement a service that computes per-edition deltas against 7-day and 30-day baselines.
- [x] 3.3 Handle missing baseline values by returning null or omitting the delta cleanly.
- [x] 3.4 Ensure all outputs remain in EUR and exclude non-EUR / missing-price entries.

## 4. API exposure
- [x] 4.1 Add endpoint for the collection total value trend (`/api/pricing/trend`).
- [x] 4.2 Add endpoint for edition deltas (`/api/pricing/edition-deltas`).
- [x] 4.3 Document response payloads and field semantics.

## 5. Frontend rendering
- [x] 5.1 Render the global collection value chart from the API trend data.
- [x] 5.2 Render a per-edition list/table with current value, 7d value, 30d value, delta 7d, delta 30d.
- [x] 5.3 Add presentation rules for positive/negative change indicators and null baselines.

## 6. Validation
- [x] 6.1 Add unit tests for total value snapshot calculation.
- [x] 6.2 Add unit tests for per-edition 7-day and 30-day delta computation.
- [x] 6.3 Add integration tests for API response payloads and ordering.
- [x] 6.4 Validate EUR-only filtering and exclusion counters remain consistent.

## 7. Documentation
- [x] 7.1 Update README with the new collection value trend feature and the pricing-sync snapshot model.
- [x] 7.2 Update CHANGELOG with the added total collection graph and edition delta tracking.
- [x] 7.3 Document the API endpoints and payload fields for trend/global and edition delta responses.
- [x] 7.4 Add a short note on the snapshot cadence and EUR-only valuation rules.
