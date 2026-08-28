## MODIFIED Requirements

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
