## Why

The full backup/restore feature (Administration → Sauvegarde & Restauration complètes) is meant to capture the entire application state for migration and recovery, but two gaps were found: the `wanted` flag added by a later change was never wired into the backup/restore payload, and the collection/edition value history (used by the Pricing tab's trend chart) is not captured at all. A backup taken today silently loses this data on restore, which also violates the project's own `import-export-compatibility-contract` policy requiring any backup/export field change to be covered.

## What Changes

- Add `wanted` to the `cards` array in the backup export payload, and restore it onto the corresponding `Card` on import.
- Add a new `valueHistory` section to the backup payload containing collection-level value snapshots (`CollectionValueSnapshot`) and edition-level value snapshots (`EditionValueSnapshot`), and restore them on import, remapping `editionId` references to the newly-created edition IDs (same pattern already used for `stats_enabled_sets`).
- Both additions are backward-compatible: payload `version` stays `"2"`; a legacy (N-1) payload missing `wanted` or `valueHistory` SHALL still import successfully, defaulting `wanted` to `false` and `valueHistory` to empty.
- Confirm (no change): `ApiKey` records and `User` credentials remain intentionally excluded from backup/restore (security-sensitive, environment-specific).

## Capabilities

### New Capabilities
- `backup-restore-data-completeness`: Defines which application data the full backup/restore feature must cover (collection ownership, wanted markers, current pricing, pricing sync configuration, and collection/edition value history), and what is intentionally excluded.

### Modified Capabilities
(none — `import-export-compatibility-contract` already defines the N/N-1 compatibility process; this change adds new covered fields under that existing policy without altering its requirements)

## Impact

- Backend: `AdminController.fullBackup()` / `fullRestore()`, new repositories/queries for `CollectionValueSnapshot` and `EditionValueSnapshot` reads and inserts.
- Tests: new N/N-1 compatibility fixtures and tests per `import-export-compatibility-contract`'s mandatory test coverage requirement.
- Documentation: README backup/restore section and CHANGELOG entry (per `release-documentation-discipline`).
