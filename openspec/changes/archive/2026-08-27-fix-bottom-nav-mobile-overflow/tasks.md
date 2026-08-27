## 1. CSS fix

- [x] 1.1 Add `min-width: 0` to `.nav-item` in `app.css` so flex items can shrink below their content's intrinsic width
- [x] 1.2 Add a `@media (max-width: 400px)` block reducing `.nav-item` horizontal padding (e.g. `8px 16px` → `8px 4px`)
- [x] 1.3 In the same breakpoint, reduce `.nav-item svg` size (e.g. 24px → 20px) and `.nav-item span` font-size (e.g. 0.65rem → 0.58rem)

## 2. Verification

- [x] 2.1 Verify in browser dev tools at 360px, 375px, and 390px viewport widths that all 6 bottom-nav items (including Admin) are fully visible and tappable
- [x] 2.2 Verify the layout above 400px width is unchanged from before this fix

## 3. Documentation

- [x] 3.1 Add a CHANGELOG entry describing the mobile nav overflow fix
