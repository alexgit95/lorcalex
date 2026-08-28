## 1. Backend: wanted marker in backup/restore

- [x] 1.1 Add `wanted` to the `cards` mapping in `AdminController.fullBackup()`.
- [x] 1.2 Restore `wanted` onto each recreated `Card` in `AdminController.fullRestore()`, defaulting to `false` when the field is absent (legacy payload).

## 2. Backend: value history in backup/restore

- [x] 2.1 Inject `CollectionValueSnapshotRepository` and `EditionValueSnapshotRepository` into `AdminController`.
- [x] 2.2 In `fullBackup()`, add a `valueHistory` object with `collectionSnapshots` (recordedAt, totalCollectionValueEur, currency, source) and `editionSnapshots` (recordedAt, editionId, editionCode, editionName, totalValueEur) built from all rows of both repositories.
- [x] 2.3 In `fullRestore()`, after editions are recreated (so `oldIdToNewEdition` is populated) and before returning the result, restore `collectionSnapshots` as-is and restore `editionSnapshots` by remapping `editionId` via `oldIdToNewEdition`, skipping any snapshot whose old edition id has no match.
- [x] 2.4 Handle a payload missing the `valueHistory` key entirely (legacy payload) by restoring zero snapshots without error.
- [x] 2.5 Delete existing `CollectionValueSnapshot`/`EditionValueSnapshot` rows during the restore's initial cleanup step, consistent with how other tables are cleared before restore.

## 3. Backend: tests

- [x] 3.1 Unit/integration test: a backup generated after marking a card as wanted, then restored, results in that card being wanted again.
- [x] 3.2 Unit/integration test: a backup generated with collection and edition value snapshots, then restored, recreates the same snapshot data with edition references correctly remapped to new edition ids.
- [x] 3.3 Unit/integration test: restoring a legacy (N-1) payload without `wanted` on cards and without a `valueHistory` key succeeds, with cards defaulting to `wanted=false` and no snapshots created.
- [x] 3.4 Test: an edition-level snapshot referencing an edition not present in the restored payload is skipped without error.
- [x] 3.5 Add/update N/N-1 compatibility fixtures (`src/test/resources/compat/`) to cover the new `wanted` and `valueHistory` fields, per the `import-export-compatibility-contract` mandatory test coverage requirement.
- [x] 3.6 Test: a full backup payload does not include any `ApiKey` or `User` data, and restoring a backup does not modify existing `ApiKey`/`User` rows.

## 4. Documentation

- [x] 4.1 Update the README "Sauvegarde & Restauration complètes" section to document the `wanted` field on cards and the new `valueHistory` section (contents and backward-compatibility behavior).
- [x] 4.2 Add a CHANGELOG entry under `[Unreleased]` (Fixed or Changed) describing the backup/restore completeness fix.
