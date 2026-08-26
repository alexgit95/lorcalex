## MODIFIED Requirements

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

## ADDED Requirements

### Requirement: Cardmarket French near-mint price precedence
The system SHALL prioritize the cardmarket French near-mint price field over any other price field when extracting a card's market price from a provider row.

#### Scenario: French near-mint cardmarket price is present
- **WHEN** a provider row contains `prices.cardmarket.lowest_near_mint_FR_EU_only`
- **THEN** the system SHALL use that value as the card's market price
- **AND** SHALL NOT use average, other regional, or other marketplace price fields when this field is present

#### Scenario: French near-mint cardmarket price is absent
- **WHEN** a provider row does not contain `prices.cardmarket.lowest_near_mint_FR_EU_only`
- **THEN** the system SHALL fall back to the existing generic price extraction rules
