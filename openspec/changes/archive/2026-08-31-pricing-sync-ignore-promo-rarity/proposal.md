## Why

Provider pricing rows for promo cards (`rarity` field equal to `Promo`) can never map to a local `Card`, because the catalog import intentionally excludes promo cards entirely (`LorcaJsonService` skips any card with `promoGrouping`). Today these rows still go through full mapping resolution, fail, and get counted as `UNRESOLVED_MAPPING`, which pollutes sync report counters, mapping/price diagnostic samples, and (since the recent unresolved-mapping diagnostic logging feature) emits one noisy log line per promo row on every sync run, even though nothing is actually wrong.

## What Changes

- Pricing sync SHALL skip any provider card-page row whose `rarity` field equals `Promo` (case-insensitive) before attempting local card mapping.
- Skipped promo rows SHALL NOT invoke card mapping resolution (`resolveCard`).
- Skipped promo rows SHALL NOT increment any sync report counter (`resolvedMappings`, `unresolvedMappings`, `unresolvedCount`, `statusCounts`, etc.) and SHALL NOT appear in mapping or price diagnostic samples.
- Skipped promo rows SHALL NOT emit the per-row unresolved-mapping diagnostic log line, nor contribute to the aggregated unresolved-mapping warning log.

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `pricing-provider-integration`: adds a requirement that promo-rarity provider rows are ignored before mapping resolution, with no telemetry/log side effects.

## Impact

- Affected code: `PricingSyncService.applyPricingFromProviderRows` (row-level filtering added before `resolveCard`/`extractPriceFromRow`), same method used by both scheduled/manual sync and the manual JSON import tool.
- No API or persisted schema changes. No behavior change for non-promo rows.
