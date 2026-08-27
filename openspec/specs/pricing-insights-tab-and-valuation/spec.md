# pricing-insights-tab-and-valuation Specification

## Purpose
TBD - created by archiving change add-pricing-tab-and-edition-eur-valuation. Update Purpose after archive.
## Requirements
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

### Requirement: Manual collection value recomputation
The system SHALL allow an authenticated user to manually trigger a recomputation of the collection value snapshot (total collection value and per-edition valuation) from prices currently stored in the database, without contacting the pricing provider.

#### Scenario: User triggers manual recomputation
- **WHEN** an authenticated user submits a manual recomputation request from the Pricing tab
- **THEN** the system SHALL recompute the total collection value and per-edition valuation from currently stored card prices
- **AND** the system SHALL persist a new collection value snapshot and per-edition value snapshots
- **AND** the system SHALL NOT call the external pricing provider or consume pricing sync API budget

#### Scenario: Recomputation succeeds
- **WHEN** the manual recomputation completes successfully
- **THEN** the system SHALL return a success response
- **AND** the Pricing tab SHALL display a success confirmation
- **AND** the Pricing tab SHALL refresh its displayed insights, value trend, and edition deltas to reflect the new snapshot

#### Scenario: Recomputation fails
- **WHEN** the manual recomputation fails due to an unexpected error
- **THEN** the system SHALL return an error response including the error message and root cause
- **AND** the Pricing tab SHALL display the error message and root cause to the user

#### Scenario: Recomputation in progress
- **WHEN** a manual recomputation request is in flight
- **THEN** the Pricing tab SHALL disable the trigger control and indicate that a recomputation is in progress
- **AND** the trigger control SHALL be re-enabled once the request completes, whether it succeeds or fails

