# wanted-card-detail-toggle Specification

## Purpose
Permettre de gérer le statut wanted directement depuis le détail d’une carte, qu’elle soit possédée ou non.

## Requirements
### Requirement: Wanted toggle is available in card detail
The system SHALL expose a control in the card detail view for adding or removing the card from the wanted list, regardless of whether the card is currently owned.

#### Scenario: Add an un wanted card from detail
- **WHEN** a user opens the detail of a card whose wanted state is false and activates the wanted control
- **THEN** the card SHALL be persisted with wanted set to true
- **AND** the card's ownership quantities SHALL remain unchanged
- **AND** the detail view SHALL show the control as an active wanted state

#### Scenario: Remove a wanted card from detail
- **WHEN** a user opens the detail of a card whose wanted state is true and activates the wanted control
- **THEN** the card SHALL be persisted with wanted set to false
- **AND** the card's ownership quantities SHALL remain unchanged
- **AND** the detail view SHALL show the control as an inactive wanted state

#### Scenario: Toggle a possessed card from detail
- **WHEN** a user toggles the wanted control for a card with one or more owned copies
- **THEN** the wanted state SHALL be updated using the same behavior as for an unowned card
- **AND** the regular and foil quantities SHALL remain unchanged

#### Scenario: Toggle state remains synchronized across views
- **WHEN** the wanted state is changed successfully in the card detail view
- **THEN** the updated card state returned by the API SHALL be reflected in the open detail
- **AND** subsequent collection, recent-scan, or pricing views SHALL use the updated wanted state when that card is present

### Requirement: Wanted detail control communicates its current action
The system SHALL expose an accessible name or label that communicates whether activating the control will add the card to or remove the card from the wanted list.

#### Scenario: Inactive wanted control
- **WHEN** the card is not wanted
- **THEN** the control SHALL communicate an add-to-wanted action

#### Scenario: Active wanted control
- **WHEN** the card is wanted
- **THEN** the control SHALL communicate a remove-from-wanted action
