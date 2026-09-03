## Context

`PricingSyncService.applyPricingFromProviderRows` is the single entry point used both by the scheduled/manual background sync loop and the manual JSON paste import tool. For every provider row it currently calls `resolveCard(row, episodeSetNumber)` first, and only falls back to price extraction if a local `Card` is found. Promo cards are never imported into the local catalog (`LorcaJsonService.processCard` skips any row with `promoGrouping`), so a promo row from the provider can never resolve to a local `Card`. Today it falls into the `UNRESOLVED_MAPPING` path: counted in `statusCounts`, counted in `unresolvedRows`, sampled into `mappingSamples`, and — since unresolved-mapping diagnostic logging was added — logged individually when that setting is enabled. This is pure noise: it is expected, not a mapping failure.

## Goals / Non-Goals

**Goals:**
- Recognize promo rows (`row["rarity"]` equals `Promo`, case-insensitive) before any mapping attempt.
- Ensure skipped promo rows leave zero trace in sync report counters, diagnostic samples, and logs.
- Apply the fix in the single shared method so both scheduled/manual sync and the manual JSON import tool benefit.

**Non-Goals:**
- No change to how non-promo unresolved rows are handled.
- No new persisted state, no new admin setting/toggle — the behavior is unconditional.
- Not addressing the (currently impossible) case of a `Card` entity existing locally with a promo-equivalent rarity; catalog import already excludes promo cards, so this is out of scope.

## Decisions

- **Filter on provider row `rarity` field, case-insensitive equality to `Promo`.** The provider's own English rarity vocabulary already appears in this field (`Common`, `Uncommon`, `rare`, `Super_rare`, `Legendary`, `Epic`, `Iconic`, `Enchanted`, casing is inconsistent in observed samples), so a case-insensitive check is used for robustness. This is distinct from the local `Card.rarity` field, which uses French values and never contains a promo variant.
- **Filter before `resolveCard`, inside the per-row loop of `applyPricingFromProviderRows`.** Placing the check first avoids the mapping-resolution cost entirely (per proposal decision #4) and guarantees no downstream counters/samples/logs are touched, since none of that code executes for skipped rows.
- **No new counter, status, or telemetry for skipped promo rows.** Per proposal decision #3, these rows should be invisible in the sync report — not even a distinct `SKIPPED_PROMO` bucket — since they are expected and not actionable by an admin reviewing the report.
- **Alternative considered — mark as a distinct status (e.g. `SKIPPED_PROMO`) in `statusCounts`:** rejected because the goal is to eliminate noise, and a new always-present counter that trends with catalog size would itself become noise over time with no actionable value.

## Risks / Trade-offs

- [Risk] Provider changes the promo rarity string casing/value in the future (e.g. `PROMO`, `promo_card`) → mitigated by case-insensitive exact match on `Promo`; if the provider vocabulary changes further, this filter will need a follow-up update (acceptable given current fixture evidence only shows a `rarity` string field).
- [Risk] A row legitimately lacks a `rarity` field → treated as non-promo (filter only triggers on an explicit case-insensitive match), so existing behavior for such rows is unchanged.

## Open Questions

None — scope confirmed with stakeholder during exploration (field: `rarity`; issue: log/report noise only; no counter; skip mapping entirely).
