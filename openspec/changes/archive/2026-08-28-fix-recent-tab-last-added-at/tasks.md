## 1. Model change

- [x] 1.1 Remove the `@PreUpdate` lifecycle callback from `UserCollection` (keep `@PrePersist` unchanged).

## 2. Explicit bump at genuine-change call sites

- [x] 2.1 `CollectionService.addCard`: explicitly set `uc.setLastAddedAt(LocalDateTime.now())` before `save(uc)`.
- [x] 2.2 `CollectionService.updateQuantity`: explicitly set `uc.setLastAddedAt(LocalDateTime.now())` before `save(uc)`.
- [x] 2.3 `LorcaJsonService` Companion import loop: explicitly set `uc.setLastAddedAt(LocalDateTime.now())` for each imported/updated row before `save(uc)`.
- [x] 2.4 Confirm `CollectionIntegrityRepair` requires no code change (it never sets `lastAddedAt`) and that `AdminController` backup restore is unaffected (already sets `lastAddedAt` explicitly on new entities via `@PrePersist`).

## 3. Tests

- [x] 3.1 Update `UserCollectionAuditTest`: remove/replace `onUpdate_updatesLastAddedAtAndPreservesFirstAddedAt` (which asserted the old `@PreUpdate` behavior) with a test confirming a plain field update with no explicit `setLastAddedAt` call does NOT change `lastAddedAt`.
- [x] 3.2 `CollectionServiceTest`: add/update tests asserting `addCard` and `updateQuantity` bump `lastAddedAt` to "now" on genuine changes.
- [x] 3.3 Add a test (e.g. in a `CollectionIntegrityRepair` test, creating one if none exists) confirming a repaired row's `lastAddedAt` is unchanged after `repairFoilInvariantIfNeeded()` runs.
- [x] 3.4 Confirm existing backup restore tests (`BackupRestoreIntegrationTest`, `RealBackupRestoreTest`) still pass unchanged (no regression on restore behavior).

## 4. Documentation

- [x] 4.1 Add a CHANGELOG entry describing the "Récents" ordering fix (old cards no longer resurface due to background integrity repairs).
