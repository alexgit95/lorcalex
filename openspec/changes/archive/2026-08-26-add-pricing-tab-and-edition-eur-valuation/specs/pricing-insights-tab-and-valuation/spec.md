## ADDED Requirements

### Requirement: Pricing insights tab shows latest priced catalog cards
The system SHALL provide a dedicated Pricing tab that displays the 20 most recently priced cards from the full catalog, ordered by latest price timestamp descending.

#### Scenario: View latest priced cards
- **WHEN** an authenticated user opens the Pricing tab
- **THEN** the system SHALL display up to 20 catalog cards sorted by `lastPriceAt` descending
- **AND** each row SHALL include card identity, edition identity, market price, currency, and `lastPriceAt`

### Requirement: Collection valuation by tracked editions
The system SHALL compute and expose collection valuation totals per edition using the same tracked-edition scope as Statistics.

#### Scenario: Valuation totals for tracked editions
- **WHEN** the Pricing tab data is requested
- **THEN** the system SHALL include valuation totals per tracked edition
- **AND** each card contribution SHALL use `(quantity + foilQuantity) x marketPrice`

#### Scenario: No tracked-edition filter configured
- **WHEN** no tracked-edition filter is configured
- **THEN** the valuation scope SHALL default to all editions, consistent with Statistics behavior

### Requirement: EUR-only monetary outputs
The system SHALL expose monetary values in EUR only for Pricing-tab outputs.

#### Scenario: Card and valuation outputs are EUR
- **WHEN** Pricing-tab data is returned
- **THEN** all returned monetary amounts SHALL be expressed in EUR
- **AND** cards with non-EUR prices SHALL be excluded from valuation aggregates

#### Scenario: Missing price values
- **WHEN** a tracked collection card has no stored market price
- **THEN** that card SHALL be excluded from valuation aggregates
- **AND** exclusion counters SHALL be available in the response for observability
