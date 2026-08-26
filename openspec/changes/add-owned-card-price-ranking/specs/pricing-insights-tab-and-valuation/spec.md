## ADDED Requirements

### Requirement: Pricing insights expose the owned-card price ranking
The system SHALL include the owned-card unit-price ranking in the Pricing insights response alongside existing valuation and latest-priced-card data.

#### Scenario: Retrieve pricing insights with the owned-card ranking
- **WHEN** an authenticated user requests Pricing insights
- **THEN** the response SHALL include the owned-card price ranking with card identity, edition identity, price, currency, normal quantity, and foil quantity

### Requirement: Pricing tab prioritizes collection price ranking
The Pricing tab SHALL render the owned-card price ranking before collection trend and edition valuation sections, and SHALL render the existing latest-priced catalog cards section after those sections.

#### Scenario: View reordered Pricing tab
- **WHEN** an authenticated user opens the Pricing tab
- **THEN** the owned-card price ranking SHALL be shown before the collection trend and edition valuation sections
- **AND** the latest-priced catalog cards section SHALL be shown at the bottom of the Pricing tab