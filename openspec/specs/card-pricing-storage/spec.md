# card-pricing-storage Specification

## Purpose
TBD - created by archiving change add-card-pricing-sync-with-daily-attempt-quota. Update Purpose after archive.
## Requirements
### Requirement: Persist card pricing metadata
The system SHALL persist pricing metadata on each card, including price value, currency, provider source, last price update timestamp, and last update status.

#### Scenario: Successful provider update
- **WHEN** a provider price response is successfully mapped to a card
- **THEN** the card pricing metadata SHALL be persisted with updated value, currency, source, and timestamp

#### Scenario: Failed provider update
- **WHEN** a provider request for a card fails or cannot be mapped
- **THEN** the card SHALL persist a failure status and the attempt timestamp for observability

