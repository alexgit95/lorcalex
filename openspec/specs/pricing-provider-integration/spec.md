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

### Requirement: Cardmarket French near-mint price precedence
The system SHALL prioritize the cardmarket French near-mint price field over any other price field when extracting a card's market price from a provider row.

#### Scenario: French near-mint cardmarket price is present
- **WHEN** a provider row contains `prices.cardmarket.lowest_near_mint_FR_EU_only`
- **THEN** the system SHALL use that value as the card's market price
- **AND** SHALL NOT use average, other regional, or other marketplace price fields when this field is present

#### Scenario: French near-mint cardmarket price is absent
- **WHEN** a provider row does not contain `prices.cardmarket.lowest_near_mint_FR_EU_only`
- **THEN** the system SHALL fall back to `prices.cardmarket.lowest_near_mint` when its currency matches the configured provider currency
- **AND** SHALL fall back to the existing generic price extraction rules when `prices.cardmarket.lowest_near_mint` is absent or its currency does not match

#### Scenario: Cardmarket lowest_near_mint currency does not match provider currency
- **WHEN** `prices.cardmarket.lowest_near_mint_FR_EU_only` is absent and `prices.cardmarket.currency` does not match the configured provider currency
- **THEN** the system SHALL NOT use `prices.cardmarket.lowest_near_mint` as the card's market price
- **AND** SHALL fall back to the existing generic price extraction rules

#### Scenario: Zero is a legitimate cardmarket price
- **WHEN** `prices.cardmarket.lowest_near_mint_FR_EU_only` is present with value zero
- **THEN** the system SHALL use zero as the card's market price
- **AND** SHALL NOT treat it as a missing value or fall back to other price fields

### Requirement: Generic price field discovery recognizes marketplace container keys regardless of separators
The system SHALL recognize marketplace price container keys (such as `tcg_player`, `tcgplayer`, `cardmarket`) as valid nested price sources whether or not the key uses underscores.

#### Scenario: Marketplace price nested under an underscored container key
- **WHEN** a provider row exposes a price under a container key such as `tcg_player` containing a `market_price` field
- **THEN** the generic price extraction fallback SHALL recognize `tcg_player` as a price container and extract `market_price` from it
