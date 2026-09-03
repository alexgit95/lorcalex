## ADDED Requirements

### Requirement: Owned cards are ranked by unit price
The system SHALL expose up to 100 owned cards with a stored EUR market price, ordered by unit market price descending and then card identifier ascending. The ranking SHALL include only cards with a positive normal or foil quantity and within the tracked-edition scope used for collection valuation.

#### Scenario: Rank eligible owned cards by unit price
- **WHEN** pricing insights are requested for cards owned in tracked editions with EUR prices
- **THEN** the response SHALL contain the eligible cards in descending `marketPrice` order
- **AND** cards with the same price SHALL be ordered by ascending card identifier
- **AND** each ranked card SHALL include its normal and foil quantities

#### Scenario: Exclude ineligible owned cards
- **WHEN** an owned card has no market price, a non-EUR price, no positive quantity, or belongs to an untracked edition
- **THEN** the card SHALL not appear in the owned-card price ranking

#### Scenario: Cap the ranking response
- **WHEN** more than 100 owned cards are eligible for the ranking
- **THEN** the response SHALL contain only the first 100 cards in ranking order

### Requirement: User selects visible ranking size
The Pricing tab SHALL allow the user to select a visible ranking size of 20, 50, or 100 cards from the ranking response.

#### Scenario: Select a ranking size
- **WHEN** the user selects 20, 50, or 100 in the owned-card ranking control
- **THEN** the Pricing tab SHALL display no more than the selected number of highest-ranked cards
