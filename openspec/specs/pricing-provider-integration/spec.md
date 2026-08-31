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
The system SHALL determine a card's market price from a provider row by evaluating an ordered list of price source candidates and using the first candidate that is present and whose associated currency matches the configured provider currency.

The ordered candidates are:
1. `prices.cardmarket.7d_average`
2. `prices.cardmarket.30d_average`
3. `prices.cardmarket.lowest_near_mint_FR`
4. `prices.cardmarket.lowest_near_mint_FR_EU_only`
5. `prices.cardmarket.lowest_near_mint`
6. `prices.tcg_player.market_price`

For candidates 1-5, the associated currency is `prices.cardmarket.currency`. For candidate 6, the associated currency is `prices.tcg_player.currency`.

#### Scenario: Earlier candidate in EUR takes precedence
- **WHEN** a provider row contains `prices.cardmarket.7d_average` with `prices.cardmarket.currency` equal to the configured provider currency
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

#### Scenario: tcg_player.market_price used as last resort
- **WHEN** none of `prices.cardmarket.7d_average`, `30d_average`, `lowest_near_mint_FR`, `lowest_near_mint_FR_EU_only`, or `lowest_near_mint` are usable (absent or currency mismatch)
- **AND** `prices.tcg_player.market_price` is present with `prices.tcg_player.currency` equal to the configured provider currency
- **THEN** the system SHALL use `prices.tcg_player.market_price` as the card's market price

#### Scenario: No usable candidate leaves the price unresolved
- **WHEN** none of the 6 ordered candidates are usable (each absent, or present with a currency not matching the configured provider currency)
- **THEN** the system SHALL NOT fall back to any other price extraction method
- **AND** the card's price SHALL be treated as unresolved for that row

#### Scenario: Zero is a legitimate price value
- **WHEN** the first usable candidate in priority order has a value of zero
- **THEN** the system SHALL use zero as the card's market price
- **AND** SHALL NOT treat it as a missing value or continue to the next candidate
