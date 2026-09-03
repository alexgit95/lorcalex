## Why

Pricing synchronization currently misses valid local cards when provider payloads only expose set identity through `episode.code` values such as `11WSP`. The current reconciliation logic does not consistently derive the numeric edition identity from this field, which causes unresolved mappings and stale prices.

## What Changes

- Clarify and enforce deterministic reconciliation rules for provider rows using card number plus edition identity derived from episode context.
- Add explicit extraction rule for `episode.code` where the leading numeric segment (example: `11WSP` -> `11`) becomes the set identity candidate.
- Define ordered fallback matching so sync remains robust when one identifier is missing:
  - preferred: episode-derived numeric edition identity + `card_number`
  - fallback: provider set number fields + `card_number`
  - fallback: edition code + `card_number`
  - last resort: external id
- Add test scenarios based on realistic provider payload fixtures (including Winterspell rows from test resources) to prevent regression.
- Fix price extraction to prioritize `prices.cardmarket.lowest_near_mint_FR_EU_only` over generic average/other price fields, since that field is the authoritative source for this deployment.
- Add a temporary admin tool to simulate a provider episode-cards API response from a manually pasted JSON payload, applying pricing updates without calling the provider or consuming call budget.

## Capabilities

### New Capabilities
- `pricing-episode-code-edition-reconciliation`: Reconciles provider card rows by deriving edition identity from `episode.code` and combining it with `card_number`.
- `pricing-admin-manual-import-simulation`: Temporary admin capability to apply pricing updates from a manually pasted provider-shaped JSON payload, without any outbound provider call or budget consumption.

### Modified Capabilities
- `pricing-provider-integration`: Extend mapping requirements so episode-scoped provider payloads are normalized before repository lookup, and prioritize `prices.cardmarket.lowest_near_mint_FR_EU_only` as the authoritative price field.

## Impact

- Affected services: pricing sync mapping flow, provider row normalization, and price field extraction.
- Affected controller: new admin endpoint `POST /api/admin/pricing/simulate-import`.
- Affected UI: temporary admin section to paste provider JSON and trigger a simulated pricing update.
- Affected tests: pricing sync unit/integration coverage using fixture-based provider payload examples, including price field priority regression tests.
- No API contract break expected for existing admin endpoints; impact is behavioral accuracy, observability of unresolved mappings, and correct price sourcing.
