## Context

`UserCollection.lastAddedAt` drives the ordering of the "Récents" tab (`UserCollectionRepository.findRecentWithCard`, `ORDER BY uc.lastAddedAt DESC`). Today it's maintained by a JPA `@PreUpdate` lifecycle callback that unconditionally sets `lastAddedAt = LocalDateTime.now()` on every UPDATE of the row, regardless of what changed or why.

This is too broad: `CollectionIntegrityRepair` (an `ApplicationReadyEvent` listener that normalizes `foil`/`quantity`/`foilQuantity` inconsistencies at startup) calls `userCollectionRepository.saveAll(dirty)` for any row it repairs, which triggers `@PreUpdate` and bumps `lastAddedAt` — pushing old, untouched-by-the-user cards to the top of "Récents". This was confirmed as the cause of the reported bug (old foil cards surfacing in "Récents" without being scanned).

Other callers of `.save(uc)` on an existing `UserCollection`:
- `CollectionService.addCard` / `updateQuantity` — genuine user actions (scan/manual quantity change); SHOULD affect recency.
- `LorcaJsonService` Companion import loop — genuine bulk import of owned quantities; SHOULD affect recency.
- `AdminController` backup restore — always constructs a **new** `UserCollection()` (id null) with `lastAddedAt` set explicitly from the backup payload before `save()`, so it goes through `@PrePersist` (which only fills null fields), never `@PreUpdate`. Unaffected by this change.

## Goals / Non-Goals

**Goals:**
- `lastAddedAt` (and therefore "Récents" ordering) SHALL only change when a card is actually added, its quantity is manually changed, or its quantity is imported from an external source (Companion).
- Maintenance/repair code paths (`CollectionIntegrityRepair`) SHALL NOT affect `lastAddedAt`.
- Backup restore behavior remains unchanged.

**Non-Goals:**
- Not changing the `foil`/`quantity` invariant repair logic itself — only ensuring it no longer has a side effect on recency.
- Not introducing a full audit/event-log system for collection changes — this is scoped to the single `lastAddedAt` field.
- Not changing the "Récents" API contract or the 3-tab pagination/limit UI.

## Decisions

- **Remove the `@PreUpdate` lifecycle callback on `UserCollection` entirely.** `firstAddedAt`/`lastAddedAt` will only be set via `@PrePersist` (unchanged, fills nulls on insert) and via explicit `uc.setLastAddedAt(LocalDateTime.now())` calls at the specific call sites that represent a genuine collection change.
  - Alternative considered: keep `@PreUpdate` but make it conditional by comparing old vs new `quantity`/`foilQuantity` (e.g. via a `@PostLoad`-captured snapshot). Rejected — more complex, still an implicit mechanism relying on entity lifecycle introspection, harder to reason about and test than explicit call-site intent.
  - Alternative considered: keep `@PreUpdate`, but have `CollectionIntegrityRepair` use a native/JPQL bulk `UPDATE` (bypassing entity lifecycle callbacks) instead of `saveAll`. Rejected as the sole fix — it only patches the one known offending caller; any future maintenance code calling `.save()` on `UserCollection` would silently reintroduce the same bug. The chosen approach makes the timestamp's semantics explicit everywhere, which is more robust long-term.
- **Explicit bump locations**: `CollectionService.addCard`, `CollectionService.updateQuantity`, and the Companion import loop in `LorcaJsonService` each call `uc.setLastAddedAt(LocalDateTime.now())` right before `save(uc)`.
- **`CollectionIntegrityRepair` requires no code change** beyond the `@PreUpdate` removal — it already only mutates `quantity`/`foilQuantity`/`foil`, never touches `lastAddedAt`, so once the automatic hook is gone, its saves naturally stop affecting recency.

## Risks / Trade-offs

- [Risk] Any other, currently-unknown, code path that saves an existing `UserCollection` row without explicitly bumping `lastAddedAt` will silently stop appearing in "Récents" for that action, even if it was implicitly relying on `@PreUpdate` → Mitigation: the impact audit above covers all current callers (`grep` confirmed only 4 call sites project-wide); tests will assert the explicit behavior at each of the 3 genuine-action call sites.
- [Risk] Removing `@PreUpdate` changes a previously-tested behavior (`UserCollectionAuditTest.onUpdate_updatesLastAddedAtAndPreservesFirstAddedAt`) → Mitigation: update/replace that test to assert the new explicit-bump behavior instead of the implicit lifecycle one.

## Migration Plan

- No data migration needed — this only changes when new writes touch `lastAddedAt` going forward; existing historical timestamps are left as-is (including any already-inflated ones from past repair runs, which will naturally age out of "Récents" as new genuine activity occurs).
- Deploy as a normal code change; no schema change, no feature flag needed given the low risk and narrow scope.

## Open Questions

- None outstanding — the fix and its call-site audit were confirmed during exploration.
