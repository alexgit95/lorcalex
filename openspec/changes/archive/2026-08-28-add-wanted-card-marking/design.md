## Context

`Card` is the catalog entity (per card id + edition, shared globally — the app is single-user aside from auth). `UserCollection` tracks ownership (`quantity`, `foilQuantity`) and is deleted whenever both reach zero, per the `collection-ownership-and-foil-invariants` spec. The frontend is plain JS (`app.js`) rendering card grids via `cardItemHTML`/`ownedPricingCardItemHTML`, and a camera-based OCR scanner (`handleCapture` → `handleFoundCards` → `renderCardConfirmation` for single matches, `renderFoundCards` for multiple candidates) that adds scanned cards via `autoAddCard`.

The schema uses `spring.jpa.hibernate.ddl-auto=update`, so new columns are applied automatically without a migration tool.

## Goals / Non-Goals

**Goals:**
- Let a user mark/unmark any specific catalog `Card` (id + edition) as "wanted", independent of ownership.
- Show a subtle visual cue (gold border over the existing grayed-out `.missing` look) only while `wanted && !owned`.
- Persist `wanted` even after the card becomes owned (no auto-clear), but hide the visual cue once owned.
- Trigger a non-blocking confetti/fireworks overlay the moment a scan resolves to exactly one matching wanted card (`renderCardConfirmation` path), before/regardless of the user confirming the add.

**Non-Goals:**
- No change to `UserCollection` ownership/foil invariants.
- No animation for the multi-candidate scan path (`renderFoundCards`), for manual/UI-grid additions, or for the "add" confirmation click itself.
- No per-user scoping of `wanted` (matches the app's existing single-collection model).
- No auto-clearing of `wanted` when a card is later sold off/un-owned again.

## Decisions

- **Storage: `wanted` boolean lives on `Card`, not `UserCollection`.** `UserCollection` rows are deleted at zero quantity (existing invariant), so a flag stored there would be lost whenever a card is unowned — incompatible with "mark a not-yet-owned card as wanted". Putting it on the catalog entity (`Card.wanted`, default `false`) keeps it independent of ownership lifecycle and requires no changes to the ownership invariants.
- **Toggle endpoint**: new `PATCH /api/cards/{id}/wanted` (or similar) that flips/sets `wanted` on the given `Card` id and returns the updated `CardDTO`. Kept separate from `CollectionController` since it doesn't touch `UserCollection`.
- **Display rule**: `wanted` is included in `CardDTO`. Client applies a `.wanted` CSS class (gold border) only when `card.wanted && !card.owned`, layered on top of the existing `.missing` styling — no change to the `.missing`/`.owned` classes themselves.
- **Animation scope**: detection happens client-side. In `handleFoundCards`, only the `cards.length === 1` branch (which calls `renderCardConfirmation`) checks `card.wanted` and fires the confetti overlay immediately upon rendering the confirmation view — before the user clicks "Ajouter". The multi-candidate branch (`renderFoundCards`) never triggers it, since the match is ambiguous.
- **Animation implementation**: a lightweight, dependency-free overlay (absolutely-positioned full-screen `div` with CSS-animated particles, or a small canvas confetti burst) appended to the DOM and auto-removed after ~1.5-2s. Non-blocking: no modal, no interaction required, scanning UI remains usable underneath.

## Risks / Trade-offs

- [Confetti fires on every re-scan of the same wanted, still-unowned card] → Acceptable per requirements (no dedup requested); user controls this by unmarking `wanted` or completing the acquisition.
- [`wanted` never auto-clears, so a "wanted" catalog entry with a hidden flag persists indefinitely after ownership] → Accepted trade-off per explicit decision; flag remains available if user wants to see/reuse it later (e.g. via an admin/debug view), just not surfaced in the normal grid.
- [New endpoint surface increases API footprint] → Kept minimal: single PATCH-style toggle, reusing existing auth/session middleware.

