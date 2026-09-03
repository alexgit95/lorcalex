## Why

The Collection page grid, when viewing a single edition (~200-300 cards), becomes sluggish on desktop: an unbounded `auto-fill` grid renders up to ~28 columns on wide screens, so far more card images are near the viewport at once than the browser's native lazy-loading can smoothly handle, and every keystroke in search re-filters and rebuilds the whole grid with no debounce.

## What Changes

- Cap the collection grid to at most 10 columns on desktop while keeping the current natural (auto-fill-like) column count on narrower/mobile screens, recomputed live on window resize.
- Replace reliance on native `loading="lazy"` with an explicit `IntersectionObserver`-driven image loading strategy (image `src` set only once a card approaches the viewport, within roughly one screen-height margin), with a reserved-space skeleton so no layout shift occurs before an image loads.
- Require at least 3 characters typed before the search filter is applied, and debounce filtering by 300ms after the last keystroke; below 3 characters, the grid shows all cards for the current edition (unfiltered) with a "type at least 3 characters" hint replacing normal empty-state messaging only when applicable.
- Scope: Collection page grid only (not Recent scans, Pricing, or other card grids).

## Capabilities

### New Capabilities
- `collection-grid-adaptive-columns`: Adaptive, resize-reactive column cap (max 10) for the Collection page grid, with no floor different from current behavior.
- `collection-grid-lazy-image-loading`: Viewport-proximity-based image loading with reserved layout space (skeleton) for the Collection page grid.
- `collection-search-debounced-filtering`: Minimum-length and debounce rules for the Collection page search input.

### Modified Capabilities
(none — no existing spec covers collection grid layout, image loading, or search filtering)

## Impact

- Frontend only: `app.js` (`renderCards`, `cardItemHTML`, `renderCollection`/search input wiring, new resize/observer helpers), `app.css` (`.cards-grid` column variable, skeleton styling).
- No backend or API changes.
