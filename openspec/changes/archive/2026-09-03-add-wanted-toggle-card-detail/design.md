## Context

The application renders the card detail view as a modal from the collection, recent scans, and pricing views. The modal already supports collection quantity changes and uses the existing `PATCH /api/cards/{id}/wanted` endpoint elsewhere in the frontend, but it does not currently expose that action. The `wanted` flag belongs to the catalog card and is independent of `UserCollection`, so the detail control must work for both owned and unowned cards without touching quantity data.

## Goals / Non-Goals

**Goals:**

- Add a wanted toggle to the existing card detail modal for owned and unowned cards.
- Reuse the existing API and return value rather than adding a backend endpoint.
- Keep the modal, collection grid, recent scans, and pricing card caches synchronized after a successful toggle.
- Make the control's accessible name describe the action available in the current state.
- Preserve the existing gold-border rule: show it only when `wanted` is true and the card is not owned.

**Non-Goals:**

- Change ownership or foil quantity behavior.
- Change the storage model or API contract.
- Automatically clear wanted when a card becomes owned or unowned.
- Add a wanted control to unrelated views that do not display card detail.

## Decisions

### Reuse the existing modal and endpoint
Add the control inside `openModal` and call `api.setWanted(card.id, !card.wanted)`. This keeps the behavior consistent with the collection grid and scanner and avoids a second API contract. A separate detail endpoint or modal would duplicate existing behavior without adding capability.

### Keep the control independent from ownership
Render the control outside the owned/unowned quantity branch so it is always available. The backend already treats `wanted` independently from `UserCollection`; hiding it for owned cards would make the state harder to manage and conflict with the requirement that it remains persistent.

### Synchronize every in-memory card source
On success, replace the matching card in `collState.cards`, `recentCardsState`, `pricingCardsState`, and `ownedPricingCardsState`, then update `collState.modal` with the returned DTO. Re-render the modal so its label and active state are immediately correct. The grid is refreshed where it is mounted, while views without a collection grid retain their updated cache for their next render.

### Use state-specific accessible action text
The control's title and accessible label should say that it will add or remove the card from the wanted list. The visual star state can remain consistent with the existing control, but the action must not have the same label in both states.

## Risks / Trade-offs

- [A request failure could leave the UI unchanged while the user expects a toggle] -> Keep the current state until the API succeeds and display the existing error feedback pattern.
- [The same card can exist in several frontend caches] -> Update all known caches from the single DTO returned by the endpoint.
- [The modal is reused by pages with different backing data] -> Resolve the card through existing sources and update each source by id; do not assume the card is always in the collection list.
- [A wanted card can be owned while its gold border is hidden] -> Keep the visual class conditional on `wanted && !owned`, separate from the toggle's availability.

## Migration Plan

No database or API migration is required. The implementation uses the existing `wanted` column, DTO field, and PATCH endpoint. Deployment is a static frontend update; rollback consists of reverting the frontend change, leaving persisted wanted values intact.

## Open Questions

None. The control is explicitly available for both owned and unowned cards.
