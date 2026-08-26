# pricing-provider-integration Specification

## Purpose
TBD - created by archiving change add-card-pricing-sync-with-daily-attempt-quota. Update Purpose after archive.
## Requirements
### Requirement: Provider-backed pricing lookup
The system SHALL fetch card prices from the configured external Lorcana pricing provider using a deterministic card mapping strategy.

#### Scenario: Provider result mapped to card
- **WHEN** provider returns a resolvable card price
- **THEN** the card pricing metadata SHALL be updated with provider source attribution

#### Scenario: Provider result unresolved
- **WHEN** provider response cannot be mapped to a local card deterministically
- **THEN** the attempt SHALL be marked with unresolved status and included in error telemetry

### Requirement: Provider errors are telemetry, not budget control
Provider throttling or error responses SHALL NOT alter local budget enforcement semantics.

#### Scenario: Provider returns 429
- **WHEN** provider returns HTTP 429
- **THEN** the attempt SHALL still consume one budget unit
- **AND** the response SHALL be recorded as provider error telemetry

