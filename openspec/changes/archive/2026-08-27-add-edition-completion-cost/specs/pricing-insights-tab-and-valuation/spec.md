## ADDED Requirements

### Requirement: Edition completion cost by rarity tier
The system SHALL compute, for each tracked edition shown in the Pricing tab, the cost of acquiring currently missing cards, split into two rarity tiers.

#### Scenario: Missing card contributes to the base rarity tier
- **WHEN** a tracked edition contains a catalog card with rarity `Commune`, `Inhabituelle`, `Rare`, `Très Rare`, or `Légendaire`
- **AND** the user owns zero copies of that card (normal and foil quantities both zero, or no collection entry)
- **THEN** the card's `marketPrice` SHALL be added to that edition's base-tier completion cost

#### Scenario: Missing card contributes to the premium rarity tier
- **WHEN** a tracked edition contains a catalog card with a rarity other than `Commune`, `Inhabituelle`, `Rare`, `Très Rare`, or `Légendaire` (e.g. `Enchanté`)
- **AND** the user owns zero copies of that card
- **THEN** the card's `marketPrice` SHALL be added to that edition's premium-tier completion cost

#### Scenario: Owned cards do not contribute to completion cost
- **WHEN** a tracked edition contains a catalog card that the user owns at least one copy of (normal or foil)
- **THEN** that card SHALL NOT contribute to either completion-cost tier for that edition

#### Scenario: Missing card without a known price is excluded but counted
- **WHEN** a missing card in a tracked edition has no stored `marketPrice`
- **THEN** that card SHALL be excluded from both completion-cost tiers
- **AND** the edition's response SHALL include a single count of such cards, combined across both tiers

#### Scenario: Completion cost respects the tracked-edition scope
- **WHEN** the Pricing tab data is requested
- **THEN** completion-cost figures SHALL only be computed and returned for editions within the same tracked-edition scope (`stats_enabled_sets`) as the rest of the Pricing tab

#### Scenario: Pricing tab displays base-tier completion cost per tracked edition
- **WHEN** an authenticated user views the "Valeur par édition suivie" section of the Pricing tab
- **THEN** each tracked edition's entry SHALL display its base-tier completion cost labeled to clearly indicate it is the cost of missing cards (e.g. "Coût des cartes manquantes (Courantes et Légendaire)")
- **AND** the premium-tier completion cost SHALL NOT be displayed in this section, even though it is computed and returned by the API
- **AND**, if any missing cards in that edition have no known price, an indication that the displayed cost is understated for that number of cards
