## ADDED Requirements

### Requirement: Missing-value first prioritization
The pricing scheduler SHALL prioritize cards without value before any card that already has a value timestamp.

#### Scenario: Queue build with mixed cards
- **WHEN** the scheduler builds the processing queue
- **THEN** cards with null last price timestamp SHALL be ordered before cards with non-null last price timestamp

### Requirement: Oldest-refresh next prioritization
After missing-value cards are processed, the scheduler SHALL process cards by ascending last price timestamp (oldest first).

#### Scenario: Queue build for already valued cards
- **WHEN** only cards with existing value timestamps remain
- **THEN** the scheduler SHALL process the smallest timestamp first and continue in ascending order
