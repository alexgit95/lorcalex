## 1. Backend aggregation

- [x] 1.1 Add `RarityCountDTO`-like nested DTO (e.g. `MissingByColorDTO { inkColor, byRarity: List<{rarity, missingCards}> }`) and add `List<MissingByColorDTO> missingByColor` to `EditionStatDTO`.
- [x] 1.2 In `StatisticsService.buildEditionStat`, compute the missing-cards breakdown by (ink color, rarity) from the edition's cards and owned collection, reusing the already-fetched card list from `buildRarityStats` where possible. Only include rarities with `missingCards > 0`; omit colors with zero missing cards entirely from `missingByColor` (empty cell client-side).
- [x] 1.3 Ensure `resolveEnabledSetIds()` / tracked-editions filtering continues to apply identically to this new breakdown (same editions as the rest of `/api/stats`).

## 2. Frontend table rendering

- [x] 2.1 Add a `normalize(value)` JS helper (lowercase, strip accents, strip whitespace) used to resolve `icons/ink/${normalize(inkColor)}.png` and `icons/rarity/${normalize(rarity)}.png`.
- [x] 2.2 Remove the `rarityCharts` markup block and its per-edition `Chart.js` instantiation loop from the Stats tab renderer in `app.js`.
- [x] 2.3 Add the new table: header row with the 6 ink color icons (fixed order) + "Total" column; one row per tracked edition with missing cards > 0 (hide 0-missing editions); each cell renders rarity icon+count pills (Commune → Légendaire order, only rarities > 0) plus a `(=N)` subtotal, empty if no missing cards for that color.
- [x] 2.4 Add `onerror` fallback on ink/rarity `<img>` tags to show the plain text label if the icon fails to load, and set `title`/`alt` attributes for accessibility.
- [x] 2.5 Wrap the table in a horizontally-scrollable container for narrow viewports.

## 3. Cleanup

- [x] 3.1 Remove the throwaway `static/prototype-stats-table.html` file once the real table is implemented and validated in the app.

## 4. Tests

- [x] 4.1 Backend unit test: `StatisticsService` returns correct `missingByColor` breakdown (rarities with 0 missing excluded, colors with 0 missing excluded) for a sample edition/collection state.
- [x] 4.2 Backend unit test: tracked-editions filter (`stats_enabled_sets`) still applies to editions returned with the new breakdown.
- [x] 4.3 Manual/visual check: table matches the approved prototype (icon sizing, hidden 0-missing editions, subtotal display, Total column). Confirmed by user; added a row-separator border for readability per feedback.

## 5. Documentation

- [x] 5.1 Update README if the Stats tab section documents the per-edition charts, to describe the new table instead. (No existing README section covers this; nothing to update.)
- [x] 5.2 Add a CHANGELOG entry describing the Stats tab change (per-edition rarity charts replaced by the missing-cards-by-color-and-rarity table).
