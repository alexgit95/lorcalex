## Context

The Collection page (`renderCollection`/`renderCards` in [app.js](../../../src/main/resources/static/app.js), `.cards-grid`/`.card-item` in [app.css](../../../src/main/resources/static/app.css)) renders one edition's cards (~200-300) as a CSS grid with `grid-template-columns: repeat(auto-fill, minmax(110px, 1fr))`. On wide desktop viewports this yields up to ~28 columns, and every card's `<img>` uses native `loading="lazy"`, whose preload margin is browser-controlled and not tunable. The search input filters on every keystroke with no minimum length or debounce, rebuilding the whole grid (`area.innerHTML = ...`) each time.

## Goals / Non-Goals

**Goals:**
- Cap the Collection grid to at most 10 columns on desktop while preserving today's natural column count on mobile/narrow screens (no different floor).
- Recompute the column count live as the window/container is resized.
- Load card images only as they approach the viewport (~1 screen-height margin), with a skeleton that reserves the image's space so no layout shift occurs.
- Require 3+ characters before filtering the search input, debounced 300ms after the last keystroke.
- Scope all of the above to the Collection page grid only.

**Non-Goals:**
- No DOM virtualization/windowing (out of scope — catalog size per edition, ~200-300 cards, doesn't warrant it).
- No changes to Recent scans, Pricing, or other card grids/search inputs.
- No backend/API changes.

## Decisions

- **Adaptive column cap via CSS custom property, not media queries**: a `ResizeObserver` on `#cardsArea` computes `cols = min(10, floor(containerWidth / 110))` (mirroring the existing `minmax(110px, 1fr)` sizing) and sets `--cols` on the grid container; `.cards-grid` uses `grid-template-columns: repeat(var(--cols), 1fr)`. This reproduces today's auto-fill-like behavior on narrow/mobile screens (no new floor) while capping desktop at 10, and reacts live to window resizes. Plain media-query breakpoints were considered but can't express "same formula as today, just capped" without hardcoding arbitrary width thresholds.
- **`IntersectionObserver` + `data-src` instead of `loading="lazy"`**: `cardItemHTML` emits `<img data-src="..." class="card-img-lazy">` with no `src`; a shared observer (rootMargin ~ one viewport height) sets `img.src = img.dataset.src` and unobserves once a card's image enters the margin. This gives explicit, consistent control over when images load, independent of browser-specific native lazy-load heuristics.
- **Skeleton via existing `aspect-ratio` sizing**: `.card-item img` already reserves space via `aspect-ratio: 600/840` in CSS, independent of whether `src` is set. Add a `.card-img-lazy` background (skeleton color) removed once the image's `load` event fires, so pending cards show a placeholder block instead of a blank gap — no additional layout math needed.
- **Search: minimum length + debounce, "unfiltered below threshold"**: the search input handler debounces 300ms; if the (trimmed) query has fewer than 3 characters, `renderCards()` treats it as no filter (shows all cards for the current edition) and, only when the query is non-empty but under 3 characters, shows a small inline hint ("tapez au moins 3 caractères") instead of altering the empty-state path used when there are genuinely zero matches.

## Risks / Trade-offs

- [`ResizeObserver` runs on every resize/reflow of `#cardsArea`] → Debounce/throttle the column recompute (e.g. via `requestAnimationFrame`) to avoid layout thrashing during a drag-resize.
- [Switching away from native `loading="lazy"` loses the browser's own optimizations (e.g. decode scheduling)] → Acceptable trade-off for explicit control; images still decode normally once `src` is set, just at a time we choose.
- [3-character minimum could hide a valid 1-2 character match while typing] → Accepted per explicit requirement; the transition is fast (debounced 300ms) and results appear as soon as the threshold is crossed.
