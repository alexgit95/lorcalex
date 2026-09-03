## ADDED Requirements

### Requirement: Scanning a single wanted card triggers a celebratory animation
The system SHALL trigger a non-blocking celebratory overlay animation immediately when the scanner resolves a capture to exactly one matching card and that card is marked as wanted. The animation SHALL trigger regardless of whether the user subsequently confirms adding the card to the collection.

#### Scenario: Single match, wanted card
- **WHEN** the scanner resolves a capture to exactly one matching card
- **AND** that card is marked as wanted
- **THEN** the system SHALL display a non-blocking celebratory overlay animation immediately upon showing the scan confirmation view

#### Scenario: Single match, not wanted
- **WHEN** the scanner resolves a capture to exactly one matching card
- **AND** that card is not marked as wanted
- **THEN** no celebratory animation SHALL be displayed

#### Scenario: Multiple candidate matches
- **WHEN** the scanner resolves a capture to more than one candidate card
- **THEN** no celebratory animation SHALL be displayed, even if one of the candidates is marked as wanted

#### Scenario: Confirmation without adding
- **WHEN** the celebratory animation has been triggered for a wanted card scan
- **AND** the user does not confirm adding the card to the collection
- **THEN** the animation SHALL still have been shown

### Requirement: Celebratory animation does not block scanner interaction
The celebratory animation SHALL be non-blocking: it SHALL NOT prevent or delay the user's ability to interact with the scan confirmation controls (e.g. confirm add, restart scan) while it plays, and SHALL be removed automatically without requiring user dismissal.

#### Scenario: Animation auto-dismisses
- **WHEN** the celebratory animation is triggered
- **THEN** it SHALL be automatically removed after a short duration without requiring user interaction

#### Scenario: Scan controls remain usable during animation
- **WHEN** the celebratory animation is playing
- **THEN** the scan confirmation controls SHALL remain interactive
