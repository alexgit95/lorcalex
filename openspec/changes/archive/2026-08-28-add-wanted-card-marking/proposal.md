## Why

Collectors want a way to flag specific catalog cards ("wanted") independent of ownership, see a subtle visual cue for wanted cards they don't yet own, and get a small celebratory moment when a wanted card is recognized by the scanner.

## What Changes

- Add a `wanted` flag on `Card` (catalog entity, per card id/edition), toggleable via a new endpoint.
- Expose `wanted` on `CardDTO`.
- Add a toggle control (⭐) on the collection grid card item (for non-owned cards) and on the scan confirmation view.
- Display: cards that are `wanted && !owned` get an overlay gold border on top of the existing `.missing` (grayed) style. Once a card becomes owned, the gold border is hidden even though `wanted` remains `true` in storage (no auto-clear).
- Scan celebration: when the scanner resolves to exactly one matched card (`renderCardConfirmation` path, not the multi-candidate `renderFoundCards` path) and that card is `wanted`, trigger a non-blocking confetti/fireworks overlay animation immediately upon recognition — regardless of whether the user goes on to confirm the add.

## Capabilities

### New Capabilities
- `wanted-card-marking`: Toggling, storage, and visual display rules for marking a specific catalog card as wanted, independent of ownership state.
- `wanted-card-scan-celebration`: Triggering a non-blocking celebratory animation when a single wanted card is recognized during a scan.

### Modified Capabilities
(none — ownership/foil invariants on `UserCollection` are untouched; `wanted` is a separate field on `Card`)

## Impact

- `Card` entity (+ migration for new column), `CardDTO`.
- New controller endpoint to toggle `wanted` for a card id.
- `CardService`/mapping layer to include `wanted` in DTOs.
- Frontend: `app.js` (`cardItemHTML`, `renderCardConfirmation`, `handleFoundCards`/`handleCapture` scan flow, new confetti overlay helper), `app.css` (`.wanted` style).
