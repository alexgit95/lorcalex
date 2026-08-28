## Context

`AdminController.fullBackup()`/`fullRestore()` currently exports/imports 4 top-level sections: `editions`, `cards`, `collection`, `settings`. `Card.wanted` (added by `add-wanted-card-marking`) was never added to the `cards` mapping in either direction. Separately, `CollectionValueSnapshot` and `EditionValueSnapshot` (both plain `JpaRepository`-backed entities, already used read-only by `PricingInsightsService` for the Pricing tab's value trend) are not part of the backup payload at all. `EditionValueSnapshot.editionId` is a raw `Long`, not a JPA relation, so — like `stats_enabled_sets` today — it must be remapped from old to new edition IDs during restore, since editions are recreated with new IDs on every restore.

## Goals / Non-Goals

**Goals:**
- `wanted` round-trips through backup/export and restore/import.
- Collection-level and edition-level value snapshots round-trip through backup/restore, with `EditionValueSnapshot.editionId` correctly remapped to the newly-created edition IDs.
- Both additions are backward-compatible: legacy (N-1) payloads without `wanted` or `valueHistory` still import successfully (`wanted` defaults to `false`, `valueHistory` defaults to empty), consistent with the existing N/N-1 compatibility contract. Payload `version` stays `"2"`.
- Add N/N-1 compatibility tests covering both new fields, per `import-export-compatibility-contract`'s mandatory test coverage requirement.

**Non-Goals:**
- `ApiKey` and `User` remain excluded from backup/restore (confirmed intentional — security-sensitive, environment-specific).
- No change to how value snapshots are recorded during normal pricing sync (`PricingInsightsService`) — only backup/restore is affected.
- No pruning/retention policy for snapshot history in the backup (all existing rows are exported as-is).

## Decisions

- **New top-level `valueHistory` object** in the backup payload, with two arrays: `collectionSnapshots` (from `CollectionValueSnapshot`: `recordedAt`, `totalCollectionValueEur`, `currency`, `source`) and `editionSnapshots` (from `EditionValueSnapshot`: `recordedAt`, `editionId` — the *old* id, resolved like other edition references — `editionCode`, `editionName`, `totalValueEur`). Keeping this as a separate top-level key (rather than nesting under `editions`) mirrors the existing flat structure of the payload and keeps each section independently optional for backward compatibility.
- **`editionId` remapping reuses the existing `oldIdToNewEdition` map** built in `fullRestore()` while restoring `editions` (currently only used for `stats_enabled_sets`). `editionSnapshots` restoration looks up the new edition id from that map; if not found (edition removed from catalog since the backup was taken), the snapshot's `editionCode`/`editionName` are kept for historical display but the row is skipped (no edition to attach a numeric id to) — since `editionId` is a plain column, not a FK, there's no hard requirement for it to reference a live edition, but skipping avoids attaching stale/incorrect ids.
- **`wanted` added directly to the existing `cards` array** (not a separate section) since it's a per-card catalog attribute like `rarity` or `imageUrl`, exported as `false` when absent and restored via `card.setWanted(...)`.
- **Backward compatibility**: both `cardsRaw` entries missing `wanted` and a payload missing the `valueHistory` key entirely SHALL be handled with safe defaults (`false` / empty lists) — no version bump, matching how pricing fields were added previously without changing `version`.

## Risks / Trade-offs

- [Snapshot volume grows over time (pricing sync runs daily), so `valueHistory` could add non-trivial size to the backup] → Acceptable trade-off per explicit requirement ("l'historique de valeur doit être inclus"); no pruning added now, can be revisited later if backup size becomes a problem.
- [`EditionValueSnapshot` rows for an edition no longer present in the restored catalog] → Skipped during restore (see Decisions) rather than restored with a dangling/incorrect `editionId`.
- [Restore order matters: `valueHistory` restoration must happen after editions are recreated, to have `oldIdToNewEdition` populated] → Enforced by placing the value-history restore step after the existing editions-restore step in `fullRestore()`.
