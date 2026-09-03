## Why

The Pricing tab shows a total collection value and a per-edition valuation history/trend, but the underlying snapshots only get refreshed as a side effect of a full pricing sync (admin-triggered, budget-limited external API calls). Users have no way to force a fresh snapshot of the trend/edition history on demand without waiting for or triggering an external price sync.

## What Changes

- Add a `POST /api/pricing/recompute-value` endpoint that recomputes and persists a new collection value snapshot (total + per edition) directly from prices already stored in the database — no external pricing provider calls, no budget consumption.
- Add a "Recalculer" button on the Pricing tab, next to the "Valeur totale (EUR)" stat card.
  - While the request is in flight, the button is disabled and shows a loading label (e.g. "⏳ Recalcul…").
  - On success: show a toast confirmation, then re-fetch and re-render insights, trend, and edition-deltas so the chart/table reflect the new snapshot immediately.
  - On failure: show the error message and root cause to the user.
- No new authorization role is introduced; the endpoint requires the same authentication as the rest of the Pricing tab.

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `pricing-insights-tab-and-valuation`: adds a requirement for a manual, on-demand recomputation of the collection value snapshot (total + per-edition) from currently stored prices, triggerable from the Pricing tab UI.

## Impact

- Backend: `PricingController` (new endpoint), reuses existing `CollectionValueTrendService.persistSnapshotFromCurrentCollection()`.
- Frontend: `app.js` `renderPricingPage()` (new button, loading state, toast/error handling, re-fetch of insights/trend/edition-deltas).
- No database schema changes; reuses existing `collection_value_snapshots` / `edition_value_snapshots` tables.
