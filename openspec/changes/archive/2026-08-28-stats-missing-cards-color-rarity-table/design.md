## Context

The Stats tab (`app.js`, function rendering `#statsContent`) currently builds, per tracked edition, a Chart.js stacked bar chart ("Cartes par rareté") from `EditionStatDTO.byRarity` (a `List<RarityStatDTO>` with `{rarity, totalCards, ownedCards, missingCards}`, rarity only — no color dimension). `StatisticsService.buildRarityStats` computes this from `Card.rarity`, which stores French values matching a fixed list: `Commune, Inhabituelle, Rare, Très Rare, Légendaire`.

`Card.inkColor` also stores French values (`Ambre, Améthyste, Émeraude, Rubis, Saphir, Acier`) but is never aggregated into stats today — it's only surfaced in `CardDTO`/export/backup payloads.

Local icon assets were added directly to the repo during exploration and are ready to use:
- `static/icons/ink/{ambre,amethyste,emeraude,rubis,saphir,acier}.png`
- `static/icons/rarity/{commune,inhabituelle,rare,tresrare,legendaire}.png`

Filenames are lowercase, accent-stripped versions of the exact `Card.rarity` / `Card.inkColor` DB values, so a single generic `normalize(value)` (lowercase + strip diacritics + strip spaces) resolves to the correct filename for both dimensions — no per-value lookup table needed.

A throwaway HTML prototype (`static/prototype-stats-table.html`, not wired into the app) was built and approved during exploration; it should be removed once the real implementation lands.

## Goals / Non-Goals

**Goals:**
- Replace the N per-edition Chart.js rarity charts with a single HTML table: rows = tracked editions (hiding editions with 0 missing cards everywhere), columns = the 6 ink colors (icon header) + a final Total column.
- Each cell shows, for that edition/color, a compact per-rarity breakdown: rarity icon + missing count, ordered Commune → Légendaire, only for rarities with count > 0, followed by a subtotal `(=N)`. Cell is empty if 0 missing for every rarity in that color.
- Use the local ink/rarity icon assets as visual labels instead of text, with `alt`/`title` fallback text for accessibility and broken-image resilience.

**Non-Goals:**
- No change to the existing global charts (doughnut, "Cartes par édition", global "Cartes par rareté") — they stay as-is.
- No change to `stats_enabled_sets` filtering semantics — the table respects the same tracked-editions filter as today.
- Not introducing a generic icon-mapping framework beyond the simple `normalize()` helper needed here.

## Decisions

- **Backend aggregation**: add a new nested breakdown to the stats response. Introduce `MissingByColorDTO { String inkColor; List<RarityCountDTO> byRarity; }` (only rarities with `missingCards > 0`) attached to `EditionStatDTO` (e.g. `List<MissingByColorDTO> missingByColor`), computed in `StatisticsService` by grouping the edition's cards by `(inkColor, rarity)` and subtracting owned counts (reusing the existing owned/collection repository query pattern already used per rarity, extended with an ink color parameter).
  - Alternative considered: compute this client-side from full card lists. Rejected — the Stats endpoint doesn't currently return full card lists (only aggregate counts), and keeping aggregation server-side avoids shipping per-card data just for a summary table.
- **Icon resolution**: a single JS helper `normalize(value)` (lowercase, strip accents via `normalize('NFD').replace(/[\u0300-\u036f]/g, '')`, strip whitespace) builds `icons/ink/${normalize(inkColor)}.png` and `icons/rarity/${normalize(rarity)}.png`. No per-color/per-rarity mapping table.
- **Fallback**: `<img onerror="...">` on each icon falls back to the plain text label (color/rarity name), consistent with the existing `onerror` pattern already used for card images in `app.js`.
- **Row/column shape**: fixed 6 ink-color columns always rendered (even if empty for a given edition) to keep the table structurally stable across editions; rarity order within a cell is fixed (Commune → Légendaire) matching `RARITIES` order already defined in `StatisticsService`.
- **Hiding fully-complete editions**: an edition row is omitted entirely when its total missing count (sum across all colors/rarities) is 0, per confirmed decision during exploration.

## Risks / Trade-offs

- [Risk] Table width: 6 color columns + edition + total can require horizontal scroll on narrow mobile viewports → Mitigation: wrap the table in a horizontally-scrollable container (existing `.chart-container` pattern can be reused/adapted), consistent with how other data-dense sections already behave on mobile.
- [Risk] Visual density inside cells (up to 5 icon+count pills per cell) → Mitigation: icons sized small (~14-16px), validated visually via the approved prototype before implementation.
- [Risk] New backend aggregation adds N×6×5 potential group-by combinations per stats request → Mitigation: bounded by (tracked editions × 6 colors × 5 rarities), same order of magnitude as existing per-edition rarity aggregation; no new N+1 query pattern introduced if grouped in a single pass over `cardRepository.findByEditionOrderByCardNumberAsc(edition)` (already fetched for the existing `byRarity` computation).

## Migration Plan

- Additive backend change (new field on `EditionStatDTO`); no existing field removed, so no API break for other consumers of `/api/stats` (if any).
- Frontend: remove `rarityCharts` markup/rendering block and its `Chart.js` instantiation loop; add the new table renderer in the same Stats tab function.
- Remove `static/prototype-stats-table.html` once the real table is implemented and validated.
- No data migration needed — computed on read from existing `Card`/`UserCollection` tables.

## Open Questions

- None outstanding; table shape, icon source, ordering, and empty-state rules were all confirmed during exploration.
