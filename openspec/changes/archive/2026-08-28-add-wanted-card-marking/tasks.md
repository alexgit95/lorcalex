## 1. Backend: data model & API

- [x] 1.1 Add `wanted` boolean field (default `false`) to `Card` entity.
- [x] 1.2 Add `wanted` field to `CardDTO` and populate it in `CardService.toDTO(...)`.
- [x] 1.3 Add a controller endpoint (e.g. `PATCH /api/cards/{id}/wanted`) to set/toggle `wanted` on a `Card` and return the updated `CardDTO`.
- [x] 1.4 Add/adjust a repository method or service call to persist the `wanted` change without touching `UserCollection`.

## 2. Backend: tests

- [x] 2.1 Unit test: toggling `wanted` on a not-owned card persists the flag and does not create/modify a `UserCollection` row.
- [x] 2.2 Unit test: `wanted` remains `true` after a card's ownership is added then removed (quantity/foilQuantity back to zero).
- [x] 2.3 Integration test for the new endpoint (success + not-found card id).

## 3. Frontend: wanted toggle & display

- [x] 3.1 Add an API client helper to call the new wanted-toggle endpoint.
- [x] 3.2 Add a ⭐ toggle control to `cardItemHTML` for non-owned cards in the collection grid; wire click handler to call the toggle endpoint and update local state.
- [x] 3.3 Add the same ⭐ toggle control to `renderCardConfirmation` (scan confirmation view).
- [x] 3.4 Add `.wanted` CSS rule in `app.css`: gold border overlay, applied only when `wanted && !owned` (layered on top of `.missing`).
- [x] 3.5 Apply the `.wanted` class conditionally in the relevant render functions (`cardItemHTML`, `renderCardConfirmation`) based on `card.wanted && !card.owned`.

## 4. Frontend: scan celebration animation

- [x] 4.1 Implement a lightweight, dependency-free confetti/fireworks overlay helper (DOM/CSS or canvas based) that auto-removes itself after ~1.5-2s without blocking interaction.
- [x] 4.2 In `handleFoundCards`, when `cards.length === 1` and the resolved card is `wanted`, trigger the celebration overlay immediately when `renderCardConfirmation` is invoked (before/independent of user confirming the add).
- [x] 4.3 Ensure the multi-candidate path (`renderFoundCards`) never triggers the celebration animation.
- [x] 4.4 Manual verification: scan confirmation controls (Ajouter, Recommencer) remain clickable while the animation plays.

## 5. Documentation

- [x] 5.1 Update `README.md` if it documents card/collection API surface, to mention the new wanted endpoint.
