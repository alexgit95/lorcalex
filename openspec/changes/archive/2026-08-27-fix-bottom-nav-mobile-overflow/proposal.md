## Why

On narrow mobile viewports (~375-390px wide), the bottom navigation bar's 6 fixed-size items overflow the screen width. Because `html, body` has `overflow-x: hidden`, the overflow is silently clipped instead of wrapping or scrolling, making the last item ("Admin") invisible and unreachable on those devices.

## What Changes

- Add a responsive breakpoint (`@media (max-width: ~400px)`) for `.bottom-nav` / `.nav-item` that reduces horizontal padding, icon size, and label font size so all 6 items fit within the viewport width.
- Ensure `.nav-item` can actually shrink (e.g. `min-width: 0`) instead of relying on `flex: 1` alone, which does not force content below its natural size.

## Capabilities

### New Capabilities
- `mobile-navigation-responsiveness`: the bottom navigation bar must remain fully visible and usable (all items reachable, no clipping) on small mobile viewports.

### Modified Capabilities
(none)

## Impact

- Frontend: `app.css` (`.bottom-nav`, `.nav-item`, `.nav-item span`, `.nav-item svg` rules), no HTML/JS structure changes required.
- No backend or API impact.
