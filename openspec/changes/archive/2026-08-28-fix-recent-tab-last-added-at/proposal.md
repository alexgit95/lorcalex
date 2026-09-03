## Why

The "Récents" tab is meant to show the cards the user most recently scanned/added or whose quantity they most recently changed. Today it sorts by `UserCollection.lastAddedAt`, which is bumped by a blanket JPA `@PreUpdate` hook on *any* save of the row — including the startup `CollectionIntegrityRepair` job that silently normalizes the `foil` flag. As a result, old cards that only had a technical `foil`/`quantity` inconsistency repaired get pushed to the top of "Récents" with no real user action behind it, confusing the feature's purpose.

## What Changes

- Remove the blanket `@PreUpdate` timestamp bump on `UserCollection`.
- Make `lastAddedAt` updates explicit, only in code paths that represent a genuine collection change: adding a card (`CollectionService.addCard`), manually updating quantity (`CollectionService.updateQuantity`), and importing quantities from a Companion export (`LorcaJsonService`).
- `CollectionIntegrityRepair` (foil/quantity invariant repair) SHALL NOT alter `lastAddedAt` when it corrects inconsistent rows.
- Backup restore behavior is unaffected (it already sets `lastAddedAt` explicitly from backup data on newly-persisted rows via `@PrePersist`).
- **BREAKING (internal only):** removes the implicit `@PreUpdate` auto-touch behavior on `UserCollection`; any future code that saves a `UserCollection` row for administrative reasons will no longer accidentally affect "Récents" ordering, but also won't get an automatic timestamp bump — callers must opt in explicitly if they represent a genuine collection change.

## Capabilities

### New Capabilities
- `collection-recent-activity-tracking`: defines which actions are allowed to update `lastAddedAt` (and therefore surface a card in "Récents"), and which are not.

### Modified Capabilities
(none — `collection-ownership-and-foil-invariants` covers owned/foil semantics, not recency tracking; no requirement in it changes)

## Impact

- `UserCollection` model: remove `@PreUpdate` lifecycle callback.
- `CollectionService.addCard` / `updateQuantity`: explicitly set `lastAddedAt` on genuine changes.
- `LorcaJsonService` (Companion import loop): explicitly set `lastAddedAt` per imported row.
- `CollectionIntegrityRepair`: no code change needed once `@PreUpdate` is removed (it simply stops touching the timestamp).
- `UserCollectionAuditTest`: existing test asserting `@PreUpdate` behavior needs to be replaced with tests on the explicit call sites.
- No API contract changes — `/api/collection/recent` behavior improves (more accurate ordering) without a shape change.
