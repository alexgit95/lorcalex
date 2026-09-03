# pricing-provider-integration Specification

## Purpose
TBD - created by archiving change add-card-pricing-sync-with-daily-attempt-quota. Update Purpose after archive.
## Requirements
### Requirement: Provider-backed pricing lookup
The system SHALL fetch card prices from the configured external Lorcana pricing provider using paginated set discovery and paginated set-card retrieval with deterministic local card mapping.

#### Scenario: Provider set pages are traversed
- **WHEN** a pricing run starts with remaining outbound call budget
- **THEN** the system SHALL retrieve provider episodes through all available pages within active budget constraints

#### Scenario: Provider set-card page mapped to local cards
- **WHEN** provider returns a paginated card page for a set
- **THEN** the system SHALL deterministically map returned cards to local cards using `cardNumber` and normalized episode-aware set identity
- **AND** if `episode.code` is present with a numeric prefix (example `11WSP`), the system SHALL extract the prefix and use it as the primary numeric set identity candidate
- **AND** if row-level or episode-level numeric set fields are available, the system SHALL use them as deterministic fallback set identity candidates
- **AND** if numeric set identity matching fails, the system SHALL attempt deterministic edition-code plus `cardNumber` matching
- **AND** the system SHALL update pricing metadata for mapped cards

#### Scenario: Provider card cannot be deterministically mapped
- **WHEN** provider response cannot be mapped to a local card deterministically
- **THEN** the item SHALL be marked unresolved and included in error telemetry

#### Scenario: Name mismatch does not affect deterministic mapping
- **WHEN** provider card name differs from local card name formatting
- **THEN** mapping SHALL still rely on deterministic identifiers and SHALL NOT require name equality

### Requirement: Provider errors are telemetry, not budget control
Provider throttling or error responses SHALL NOT relax local call-limit enforcement semantics.

#### Scenario: Provider returns 429
- **WHEN** provider returns HTTP 429
- **THEN** the outbound request SHALL still consume one daily call unit
- **AND** the response SHALL be recorded as provider error telemetry

### Requirement: Promo rarity rows are ignored before mapping
When a provider set-card row's `rarity` field equals `Promo` (case-insensitive), the system SHALL skip that row before attempting local card mapping.

#### Scenario: Promo row skipped without mapping attempt
- **WHEN** a provider card-page row has `rarity` equal to `Promo` (any letter casing)
- **THEN** the system SHALL NOT attempt to resolve that row to a local card
- **AND** SHALL NOT attempt price extraction for that row

#### Scenario: Skipped promo row leaves no trace in sync telemetry
- **WHEN** a provider card-page row is skipped because its `rarity` is `Promo`
- **THEN** the sync run SHALL NOT increment any resolved, unresolved, success, or error counter for that row
- **AND** SHALL NOT include that row in mapping or price diagnostic samples
- **AND** SHALL NOT emit an unresolved-mapping diagnostic log line for that row, regardless of the unresolved-mapping logging setting

#### Scenario: Non-promo rows unaffected
- **WHEN** a provider card-page row's `rarity` is any value other than `Promo`
- **THEN** existing mapping, pricing, and telemetry behavior SHALL apply unchanged

### Requirement: Cardmarket price source priority order
The system SHALL determine a card's market price from a provider row by evaluating an ordered list of price source candidates and using the first candidate that is present, whose associated currency matches the configured provider currency, and — for the two average-based candidates — that is plausible relative to a reference median computed from the row's own price fields.

The ordered candidates are:
1. `prices.cardmarket.7d_average`
2. `prices.cardmarket.30d_average`
3. `prices.cardmarket.lowest_near_mint_FR`
4. `prices.cardmarket.lowest_near_mint_FR_EU_only`
5. `prices.cardmarket.lowest_near_mint`
6. `prices.tcg_player.market_price`

For candidates 1-5, the associated currency is `prices.cardmarket.currency`. For candidate 6, the associated currency is `prices.tcg_player.currency`.

The reference median is computed from all non-null, non-zero values among: the 8 regional `lowest_near_mint*` fields (`lowest_near_mint`, `lowest_near_mint_EU_only`, `lowest_near_mint_DE`, `lowest_near_mint_DE_EU_only`, `lowest_near_mint_FR`, `lowest_near_mint_FR_EU_only`, `lowest_near_mint_IT`, `lowest_near_mint_IT_EU_only`), `7d_average`, and `30d_average`.

#### Scenario: Earlier candidate in EUR takes precedence
- **WHEN** a provider row contains `prices.cardmarket.7d_average` with `prices.cardmarket.currency` equal to the configured provider currency, and no plausibility guard rejects it
- **THEN** the system SHALL use `prices.cardmarket.7d_average` as the card's market price
- **AND** SHALL NOT evaluate `30d_average`, `lowest_near_mint_FR`, `lowest_near_mint_FR_EU_only`, `lowest_near_mint`, or `tcg_player.market_price`

#### Scenario: Candidate skipped due to currency mismatch
- **WHEN** a candidate's value is present but its associated currency does not equal the configured provider currency
- **THEN** the system SHALL skip that candidate without using its value
- **AND** SHALL continue evaluating the next candidate in priority order

#### Scenario: Candidate skipped because absent
- **WHEN** a candidate field is absent from the provider row
- **THEN** the system SHALL skip that candidate
- **AND** SHALL continue evaluating the next candidate in priority order

#### Scenario: Implausible average is rejected and falls through
- **WHEN** `prices.cardmarket.7d_average` (or, if reached, `30d_average`) is present, currency-matched, a reference median is computable from at least 5 of the row's other price fields, and the candidate's value is strictly less than one fifth of that median or strictly greater than five times that median
- **THEN** the system SHALL treat that candidate as unusable and continue evaluating the next candidate in priority order
- **AND** the value exactly equal to five times the median (or one fifth of the median) SHALL be treated as plausible, not rejected

#### Scenario: Reference median requires at least 5 pooled values
- **WHEN** fewer than 5 non-null, non-zero values are available across the row's pooled price fields
- **THEN** no reference median SHALL be computed for that row
- **AND** `prices.cardmarket.7d_average` and `prices.cardmarket.30d_average` SHALL be evaluated using only the existing presence and currency checks, with no plausibility rejection

#### Scenario: No reference median available leaves averages unguarded
- **WHEN** none of the row's `lowest_near_mint*`, `7d_average`, or `30d_average` fields (excluding zero values) are present
- **THEN** no reference median can be computed
- **AND** `prices.cardmarket.7d_average` and `prices.cardmarket.30d_average` SHALL be evaluated using only the existing presence and currency checks, with no plausibility rejection

#### Scenario: Zero-valued fields do not contribute to the reference median
- **WHEN** computing the reference median for a row
- **THEN** any of the pooled fields whose value is exactly zero SHALL be excluded from the median calculation
- **AND** this exclusion SHALL NOT affect whether a zero value is itself usable as a final candidate price elsewhere in this requirement

#### Scenario: lowest_near_mint and tcg_player candidates are never plausibility-guarded
- **WHEN** evaluating `lowest_near_mint_FR`, `lowest_near_mint_FR_EU_only`, `lowest_near_mint`, or `tcg_player.market_price`
- **THEN** the system SHALL apply only the existing presence and currency checks to these candidates
- **AND** SHALL NOT reject any of them based on the reference median

#### Scenario: tcg_player.market_price used as last resort
- **WHEN** none of `prices.cardmarket.7d_average`, `30d_average`, `lowest_near_mint_FR`, `lowest_near_mint_FR_EU_only`, or `lowest_near_mint` are usable (absent, currency mismatch, or, for the two averages, rejected as implausible)
- **AND** `prices.tcg_player.market_price` is present with `prices.tcg_player.currency` equal to the configured provider currency
- **THEN** the system SHALL use `prices.tcg_player.market_price` as the card's market price

#### Scenario: No usable candidate leaves the price unresolved
- **WHEN** none of the 6 ordered candidates are usable (each absent, currency-mismatched, or — for the two averages — rejected as implausible)
- **THEN** the system SHALL NOT fall back to any other price extraction method
- **AND** the card's price SHALL be treated as unresolved for that row

#### Scenario: Zero is a legitimate final price value
- **WHEN** the first usable candidate in priority order has a value of zero
- **THEN** the system SHALL use zero as the card's market price
- **AND** SHALL NOT treat it as a missing value or continue to the next candidate
