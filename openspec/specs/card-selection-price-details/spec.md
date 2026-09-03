# Card Selection Price Details Specification

## Purpose
Define how available card price metadata is displayed when users select or confirm a card.

## Requirements
### Requirement: Selected card details show available price metadata
The system SHALL display a selected card's stored market price and last price update timestamp in its detail view when those values are available.

#### Scenario: Detail view has price and update timestamp
- **WHEN** a user selects a card with both `marketPrice` and `lastPriceAt`
- **THEN** the detail view SHALL show the formatted market price
- **AND** the detail view SHALL show the formatted last price update timestamp

#### Scenario: Detail view has no price metadata
- **WHEN** a user selects a card with neither `marketPrice` nor `lastPriceAt`
- **THEN** the detail view SHALL not show a price value, placeholder, or update timestamp

#### Scenario: Detail view has only one price metadata value
- **WHEN** a user selects a card with only `marketPrice` or only `lastPriceAt`
- **THEN** the detail view SHALL show the available value
- **AND** the detail view SHALL not show a placeholder for the unavailable value

### Requirement: Every card selection flow presents available price metadata
The system SHALL use the same price metadata rules when a card is selected from the collection, recent scans, Pricing tab, or scanner identification confirmation.

#### Scenario: User opens the shared card detail modal
- **WHEN** a user selects a card from the collection, recent scans, or Pricing tab
- **THEN** the shared card detail modal SHALL present the available price metadata for that card

#### Scenario: Scanner identifies one card
- **WHEN** the scanner identifies a card and presents its confirmation view
- **THEN** the confirmation view SHALL present the available price metadata for that card before the user adds it to the collection

#### Scenario: Scanner user resolves multiple matching cards
- **WHEN** the scanner presents multiple matching cards and the user selects one
- **THEN** the resulting card confirmation view SHALL present the available price metadata for the selected card

### Requirement: Price metadata display does not refresh pricing data
The system SHALL render stored price metadata during card selection without starting a price synchronization or requesting a new price.

#### Scenario: User selects a card with a stored price
- **WHEN** a user opens a card detail or scanner confirmation view
- **THEN** the system SHALL render the price metadata already available for that card
- **AND** the system SHALL not initiate a pricing synchronization as a result of that selection