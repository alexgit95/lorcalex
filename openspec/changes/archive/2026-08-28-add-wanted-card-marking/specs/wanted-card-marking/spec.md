## ADDED Requirements

### Requirement: Card can be marked as wanted independent of ownership
The system SHALL allow a specific catalog card (identified by card id within its edition) to be marked or unmarked as "wanted" independent of whether it is currently owned. The wanted state SHALL persist even after the card becomes owned, and SHALL NOT be affected by ownership quantity changes (including a card returning to zero quantity).

#### Scenario: Mark a not-yet-owned card as wanted
- **WHEN** a user toggles the wanted marker on a card that is not owned
- **THEN** the card SHALL be recorded as wanted
- **AND** the card's ownership data SHALL remain unchanged

#### Scenario: Unmark a wanted card
- **WHEN** a user toggles the wanted marker on a card that is currently wanted
- **THEN** the card SHALL be recorded as not wanted

#### Scenario: Wanted flag survives ownership changes
- **WHEN** a wanted card is added to the collection and later removed (quantity and foil quantity both return to zero)
- **THEN** the card SHALL remain recorded as wanted

### Requirement: Wanted toggle is available on collection grid and scan confirmation
The system SHALL expose a toggle control for the wanted marker on each non-owned card shown in the collection grid, and on the single-card scan confirmation view.

#### Scenario: Toggle from collection grid
- **WHEN** a user interacts with the wanted toggle on a non-owned card in the collection grid
- **THEN** the card's wanted state SHALL be updated accordingly

#### Scenario: Toggle from scan confirmation
- **WHEN** a user interacts with the wanted toggle on the scan confirmation view for a resolved card
- **THEN** the card's wanted state SHALL be updated accordingly

### Requirement: Wanted-but-unowned cards display a distinct visual cue
The system SHALL visually distinguish a card that is wanted and not owned by overlaying a gold border on top of its existing not-owned (grayed-out) appearance. Once the card becomes owned, this visual cue SHALL NOT be shown, even though the wanted state remains stored as true.

#### Scenario: Wanted and not owned
- **WHEN** a card has wanted set to true and is not owned
- **THEN** the card SHALL be rendered with its normal not-owned appearance plus a gold border overlay

#### Scenario: Wanted and owned
- **WHEN** a card has wanted set to true and is owned
- **THEN** the card SHALL be rendered with its normal owned appearance
- **AND** the gold border overlay SHALL NOT be shown

#### Scenario: Not wanted
- **WHEN** a card has wanted set to false
- **THEN** the card's appearance SHALL be unaffected by the wanted marker
