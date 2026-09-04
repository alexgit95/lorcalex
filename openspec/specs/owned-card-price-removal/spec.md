# owned-card-price-removal Specification

## Purpose

Décrire la suppression contrôlée du prix d’une carte possédée sans modifier son inventaire.

## Requirements

### Requirement: User removes a card price without altering collection ownership
The system SHALL provide a confirmed, authenticated action from the Pricing tab to remove the current pricing data of an owned card. The action SHALL clear the card's current pricing data and SHALL NOT modify `quantity`, `foilQuantity`, or other collection ownership data.

#### Scenario: Confirm price removal for an owned card
- **WHEN** an authenticated user confirms price removal for an owned card from the Pricing tab
- **THEN** the system SHALL clear the card's current pricing data
- **AND** the card's normal and foil quantities SHALL remain unchanged
- **AND** the card SHALL no longer contribute to collection valuation or the owned-card price ranking

#### Scenario: Cancel price removal
- **WHEN** the user dismisses the price-removal confirmation
- **THEN** the system SHALL not change the card price or any collection quantity

#### Scenario: A later price synchronization restores the price
- **WHEN** a later synchronization provides a EUR price for a card whose price was removed
- **THEN** the system SHALL store the synchronized price according to the standard pricing synchronization behavior