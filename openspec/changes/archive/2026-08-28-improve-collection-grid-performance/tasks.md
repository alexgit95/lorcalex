## 1. Adaptive column cap

- [x] 1.1 Add a `ResizeObserver` on `#cardsArea` (Collection page only) that computes `cols = min(10, floor(containerWidth / 110))` and sets a `--cols` CSS custom property on the grid container.
- [x] 1.2 Update `.cards-grid` in `app.css` to `grid-template-columns: repeat(var(--cols), 1fr)`, scoped so it doesn't affect other card grids (Recent scans, Pricing).
- [x] 1.3 Throttle the resize recompute (e.g. via `requestAnimationFrame`) to avoid layout thrashing during continuous window resizing.
- [x] 1.4 Manual verification: narrow/mobile viewport keeps the same column count as before; wide desktop viewport caps at 10.

## 2. Viewport-proximity image loading with reserved space

- [x] 2.1 Update `cardItemHTML` (Collection grid only) to emit `<img data-src="..." class="card-img-lazy">` instead of an eager/`loading="lazy"` `src`.
- [x] 2.2 Add a shared `IntersectionObserver` (rootMargin ≈ one viewport height) that sets `img.src = img.dataset.src` and unobserves the element once triggered.
- [x] 2.3 Add a `.card-img-lazy` skeleton background style in `app.css`, removed once the image's `load` event fires (space already reserved via existing `aspect-ratio: 600/840`).
- [x] 2.4 Preserve the existing broken-image fallback behavior (`onerror` hides the image) for lazily-loaded images.
- [x] 2.5 Manual verification: scrolling the Collection grid loads images progressively with no visible layout shift, and previously-loaded images are not re-fetched when scrolled back into view.

## 3. Debounced, minimum-length search

- [x] 3.1 Update the `#searchInput` handler in `renderCollection` to debounce filtering by 300ms after the last keystroke.
- [x] 3.2 Update `renderCards()` filtering logic: queries under 3 characters SHALL NOT filter (show all cards for the current edition); non-empty queries under 3 characters SHALL show a "type at least 3 characters" hint instead of the normal empty-state message.
- [x] 3.3 Manual verification: typing quickly only triggers one re-render after the pause; queries of 1-2 characters show the hint and an unfiltered grid; queries of 3+ characters filter as before.

## 4. Documentation

- [x] 4.1 Update `README.md` with the Collection page's new behavior: 10-column desktop cap (adaptive, unchanged on mobile), progressive image loading, and the 3-character/300ms debounce search rule.
- [x] 4.2 Add a `CHANGELOG.md` entry under `[Unreleased]` (Changed) describing the Collection grid performance improvements (column cap, lazy image loading, debounced search).
