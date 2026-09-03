## Why

The collection value trend curve (Pricing tab) is fed automatically by every pricing sync/manual recompute, with no way to correct a data point produced by a bad sync run (e.g. a mapping/extraction bug temporarily inflating or deflating the computed total). An aberrant point permanently skews the visual trend and the 7-day/30-day edition deltas that reference it. Users need a way to remove such a point once identified.

## What Changes

- Expose the collection-level snapshot's database id in the trend API response (`CollectionValueTrendPointDTO`), currently missing.
- Add a `DELETE /api/pricing/trend/{snapshotId}` endpoint that hard-deletes the `CollectionValueSnapshot` row, and also hard-deletes every `EditionValueSnapshot` row sharing the same `recordedAt` timestamp (both are written together by the same sync/recompute event, so a bad global point implies bad per-edition points too).
- No compensating recalculation: subsequent edition-delta lookups (7-day/30-day reference) naturally fall back to the next-closest remaining snapshot.
- Add a collapsed-by-default, discoverable detailed history list (table) under the trend chart in the Pricing tab, with a delete action per row; the chart itself remains read-only/non-interactive.
- **BREAKING**: none — this only adds a field to an existing response and adds a new endpoint/UI affordance; no existing behavior changes for callers that ignore the new `id` field.

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `pricing-insights-tab-and-valuation`: adds a requirement for deleting a collection value trend point (and its correlated edition-level snapshots) from the Pricing tab.

## Impact

- Affected code: `CollectionValueTrendPointDTO` (add `id`), `CollectionValueTrendService` (populate id, add deletion method), `PricingController` (new `DELETE` endpoint), `CollectionValueSnapshotRepository`/`EditionValueSnapshotRepository` (delete-by-recordedAt), `app.js` Pricing tab (collapsible history table + delete action).
- No new settings, no schema/migration changes (uses existing tables and JPA delete operations).
- Documentation: README and CHANGELOG updated per `release-documentation-discipline` (user-visible behavior change).
