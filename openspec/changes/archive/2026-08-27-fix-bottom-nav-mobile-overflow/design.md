## Context

`app.css` defines `.bottom-nav` as a fixed-position flex row (`display: flex; justify-content: space-around`) containing 6 `.nav-item` links (Collection, Stats, Prix, Scanner, Récents, Admin), each with `padding: 8px 16px`, a 24×24px SVG icon, and a 0.65rem label. There are no `@media` queries anywhere in `app.css`, so this layout is identical across all viewport widths. On viewports around 375-390px wide, the combined natural width of the 6 items exceeds the viewport, and because `html, body { overflow-x: hidden }`, the overflow is clipped rather than causing a scrollbar or wrap — the last item (Admin) ends up rendered outside the visible area.

## Goals / Non-Goals

**Goals:**
- All 6 bottom-nav items remain visible and tappable on mobile viewports as narrow as ~360px.
- Fix is CSS-only; no changes to `navHTML()` structure, routing, or the set of nav items.

**Non-Goals:**
- Not introducing an overflow menu ("More"), horizontal scroll, or reducing the number of always-visible nav items — user chose a targeted CSS fix over restructuring.
- Not addressing other unrelated areas of the app that may lack responsive styling (out of scope for this change).

## Decisions

- **Add a `@media (max-width: 400px)` breakpoint** scoped to `.bottom-nav .nav-item`, `.nav-item svg`, and `.nav-item span` rather than rewriting the base styles. Rationale: keeps desktop/tablet layout untouched, isolates risk to small screens, and is the smallest possible diff for a targeted CSS fix.
- **Reduce horizontal padding** from `8px 16px` to `8px 4px` inside the breakpoint. Rationale: padding is the single largest contributor to overflow (32px × 6 items = 192px); shrinking it recovers the most space with the least visual impact.
- **Reduce icon size** from 24×24px to 20×20px inside the breakpoint. Rationale: icons remain legible at 20px while recovering ~24px total across 6 items.
- **Reduce label font-size** slightly (0.65rem → 0.58rem) inside the breakpoint, keeping labels present (not hidden) so all tabs stay identifiable.
- **Add `min-width: 0` to `.nav-item`** (unconditionally, not just in the breakpoint). Rationale: this is the actual root cause enabler — `flex: 1` alone does not let a flex item shrink below its content's intrinsic min-width; without `min-width: 0`, even the reduced padding/sizes above could still overflow on the narrowest devices.
- **400px chosen as breakpoint threshold** rather than a device-specific value (e.g. 375px), to comfortably cover the 360-390px range mentioned by the user with some margin, while leaving larger phones/tablets on the current (already-working) layout.

## Risks / Trade-offs

- [Risk] Smaller tap targets on narrow screens could slightly hurt usability → Mitigation: padding reduction is horizontal only; vertical padding and overall tap height are unchanged, keeping the tappable area reasonable.
- [Risk] Shrinking icons/labels further on very old/small devices (e.g. <360px) may still be tight → Mitigation: acceptable for this iteration since no wrap/scroll fallback exists yet; can be revisited if reports of remaining overflow come in below 360px.
- [Trade-off] This is a size/spacing fix, not a structural one — if a 7th nav item is ever added, the same overflow risk returns and this fix would need to be revisited or replaced with a "More" menu (option C considered during exploration but deferred).
