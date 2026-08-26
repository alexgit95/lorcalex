# pricing-provider-integration Specification

## Purpose
TBD - created by archiving change add-card-pricing-sync-with-daily-attempt-quota. Update Purpose after archive.
## Requirements
### Requirement: Provider-backed pricing lookup
The system SHALL fetch card prices from the configured external Lorcana pricing provider using paginated set discovery and paginated set-card retrieval with deterministic local card mapping.

#### Scenario: Provider set pages are traversed
- **WHEN** a pricing run starts with remaining outbound call budget
- **THEN** the system SHALL retrieve provider episodes through all available pages within active budget constraints

#### Scenario: Provider set-card page mapped to local cards
- **WHEN** provider returns a paginated card page for a set
- **THEN** the system SHALL deterministically map returned cards to local cards using `setNumber + cardNumber`
- **AND** if row-level `setNumber` is absent, the system SHALL use episode-level set number for matching
- **AND** the system SHALL update pricing metadata for mapped cards

#### Scenario: Provider card cannot be deterministically mapped
- **WHEN** provider response cannot be mapped to a local card deterministically
- **THEN** the item SHALL be marked unresolved and included in error telemetry

#### Scenario: Name mismatch does not affect deterministic mapping
- **WHEN** provider card name differs from local card name formatting
- **THEN** mapping SHALL still rely on deterministic identifiers (`setNumber + cardNumber`) and SHALL NOT require name equality

### Requirement: Provider errors are telemetry, not budget control
Provider throttling or error responses SHALL NOT relax local call-limit enforcement semantics.

#### Scenario: Provider returns 429
- **WHEN** provider returns HTTP 429
- **THEN** the outbound request SHALL still consume one daily call unit
- **AND** the response SHALL be recorded as provider error telemetry

