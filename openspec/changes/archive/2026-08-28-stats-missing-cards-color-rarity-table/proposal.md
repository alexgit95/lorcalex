## Why

The Stats tab currently renders one Chart.js bar chart per tracked edition ("Cartes par rareté") showing owned/missing counts by rarity only. This doesn't let a collector see, at a glance, which **ink color + rarity** combinations are still missing per edition, and N per-edition charts becomes unwieldy as more sets are tracked. A compact table gives the same information more densely and adds the color dimension that isn't available today.

## What Changes

- **BREAKING (UI only):** Remove the per-edition "Cartes par rareté" Chart.js bar charts from the Stats tab.
- Add a new table "Manquantes par édition" with one row per tracked edition (editions with 0 missing cards everywhere are hidden), one column per ink color (always all 6, using the ink icon as column header), a compact per-rarity breakdown inside each cell (rarity icon + missing count, ordered Commune → Légendaire, only rarities with count > 0 shown, empty cell if 0 for that edition/color), a subtotal per cell, and a final "Total" column per edition.
- Add local ink-color and rarity icon assets (`static/icons/ink/*.png`, `static/icons/rarity/*.png`) used as column headers and inline pills instead of text labels.
- Extend the stats backend response with a per-edition, per-color, per-rarity missing-card breakdown (new data not currently computed anywhere).
- Existing global charts (doughnut, "Cartes par édition", global "Cartes par rareté") are unaffected and remain as-is.

## Capabilities

### New Capabilities
- `stats-missing-cards-breakdown`: per-edition table showing missing card counts broken down by ink color and rarity, replacing the per-edition rarity charts, with dedicated ink/rarity icon assets.

### Modified Capabilities
(none — no existing spec currently documents the per-edition rarity charts' behavior)

## Impact

- `StatisticsService` / `EditionStatDTO` / `RarityStatDTO` (backend): new aggregation of missing cards by (edition, ink color, rarity).
- `app.js` Stats tab rendering: remove `rarityCharts` Chart.js blocks, add new HTML table renderer.
- New static assets under `src/main/resources/static/icons/ink/` and `src/main/resources/static/icons/rarity/` (already added to the repo).
- No API contract removal — this adds fields/data, doesn't remove the existing `/api/stats` response shape (only the frontend rendering of per-edition rarity data changes).
