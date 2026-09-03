# wanted-card-scan-celebration Specification

## Purpose
TBD - created by archiving change add-wanted-card-marking. Update Purpose after archive.
## Requirements
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
The celebratory animation SHALL be non-blocking: it SHALL NOT prevent or delay the user's ability to interact with the scan confirmation controls (e.g. confirm add, restart scan) while it plays. The animation SHALL loop continuously until the user starts a new scan (e.g. restarting the scan capture) or navigates away from the scanner, at which point it SHALL stop automatically without requiring manual dismissal.

#### Scenario: Animation auto-dismisses
- **WHEN** the celebratory animation is triggered
- **THEN** it SHALL loop continuously without requiring the user to manually dismiss it
- **AND** it SHALL only stop automatically, once the scan is restarted or the scanner page is left

#### Scenario: Animation stops when the scan is restarted
- **WHEN** the celebratory animation is looping
- **AND** the user restarts the scan capture (e.g. via the "Recommencer" control or continuous-scan auto-restart)
- **THEN** the animation SHALL stop

#### Scenario: Animation stops when leaving the scanner page
- **WHEN** the celebratory animation is looping
- **AND** the user navigates away from the scanner page
- **THEN** the animation SHALL stop

#### Scenario: Scan controls remain usable during animation
- **WHEN** the celebratory animation is playing
- **THEN** the scan confirmation controls SHALL remain interactive

