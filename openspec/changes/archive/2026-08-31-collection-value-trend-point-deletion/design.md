## Context

`CollectionValueTrendService.getTrend()` returns `CollectionValueTrendPointDTO` objects built from `CollectionValueSnapshot` rows, but the DTO currently exposes only `recordedAt`/`totalCollectionValueEur` — no `id`. `CollectionValueSnapshot` and `EditionValueSnapshot` are both written together, once per sync/manual-recompute event, sharing the same `recordedAt` value, but there is no foreign key between them (correlation is by timestamp equality only). `getEditionDeltas()` picks the snapshot nearest to the 7-day/30-day thresholds from whatever rows remain in `EditionValueSnapshot` at query time — it has no dependency on which snapshot is "current", so removing rows requires no compensating recalculation.

## Goals / Non-Goals

**Goals:**
- Let a user permanently remove one collection-level trend point (and its correlated per-edition points) from the Pricing tab.
- Keep the trend chart itself read-only; deletion happens through a separate, collapsed-by-default history list.
- Reuse existing JPA repositories; no schema changes.

**Non-Goals:**
- No soft-delete/undo — this is an explicit hard delete, consistent with there being no soft-delete concept elsewhere in this codebase.
- No change to how `getEditionDeltas()` selects reference snapshots — it already tolerates missing snapshots naturally (falls back to whatever is closest to the threshold among remaining rows).
- No bulk/range deletion — one point at a time, matching the "I spotted one aberrant point" use case.
- No change to the "current" collection valuation shown elsewhere (e.g. summary card) — that reads live data, not snapshots.

## Decisions

- **Expose `id` on `CollectionValueTrendPointDTO`.** Minimal addition; `CollectionValueTrendService.getTrend()` sets it from `snapshot.getId()`. This is additive and non-breaking for any existing consumer that ignores unknown fields.
- **Correlated deletion by `recordedAt` equality, not a new FK.** Since there is no relation between `CollectionValueSnapshot` and `EditionValueSnapshot` today, deletion resolves the target snapshot's `recordedAt`, then deletes that `CollectionValueSnapshot` plus every `EditionValueSnapshot` with the same `recordedAt`. Implemented as a new repository method (e.g. `deleteByRecordedAt(LocalDateTime)`) on `EditionValueSnapshotRepository`, called from a new `CollectionValueTrendService.deleteSnapshot(Long snapshotId)` wrapped in `@Transactional`.
  - Alternative considered — add a real FK/shared identifier between the two tables: rejected as a larger migration for a feature whose correctness only needs timestamp correlation, which already holds by construction (both written in the same transaction with the same `LocalDateTime.now()` capture).
- **New endpoint `DELETE /api/pricing/trend/{snapshotId}`** in `PricingController`, mirroring the existing `DELETE /api/pricing/cards/{cardId}/price` precedent (simple id-based hard delete, `ResponseEntity` success/error shape consistent with other pricing endpoints). Returns 404 if the snapshot id doesn't exist.
- **No compensating recalculation after deletion.** `getEditionDeltas()` is called fresh on every request and just uses whatever `EditionValueSnapshot` rows currently exist — deleting rows naturally shifts which snapshot is "closest to 7/30 days" on the next call, which is the desired effect (an aberrant reference point stops being used).
- **UI: collapsed-by-default detail table**, not a clickable chart point. A disclosure/accordion below the chart ("Historique détaillé ▸"), collapsed on load, expanding to a table (date, valeur, action supprimer) built from the same `trend` data the chart already has (no extra API call). Chosen over chart-point interactivity because it's simpler to build, more discoverable once expanded, and doesn't require Chart.js click-hit-testing code.
- **Confirmation before delete.** Since this is a hard, irreversible delete, the UI SHALL ask for confirmation (native `confirm()` or existing modal pattern) before calling the endpoint.

## Risks / Trade-offs

- [Risk] User deletes the wrong point by mistake (irreversible) → mitigated by requiring a confirmation step before the DELETE call; no further mitigation (no undo) per explicit non-goal.
- [Risk] Deleting a snapshot changes historical edition-delta references silently → acceptable and intended per proposal; the whole point is that a bad reference point should stop being used.
- [Risk] Concurrent sync writing a new snapshot with the exact same `recordedAt` as one being deleted (extremely unlikely given `LocalDateTime.now()` precision) → out of scope; not a realistic contention scenario for a manually-triggered admin/user action.
