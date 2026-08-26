## MODIFIED Requirements

### Requirement: Provider-backed pricing lookup
The system SHALL fetch card prices from the configured external Lorcana pricing provider using paginated set discovery and paginated set-card retrieval with deterministic local card mapping.

#### Scenario: Provider set pages are traversed
- **WHEN** a pricing run starts with remaining outbound call budget
- **THEN** the system SHALL retrieve provider episodes through all available pages within active budget constraints

#### Scenario: Provider set-card page mapped to local cards
- **WHEN** provider returns a paginated card page for a set
- **THEN** the system SHALL deterministically map returned cards to local cards and update pricing metadata for mapped cards

#### Scenario: Provider card cannot be deterministically mapped
- **WHEN** provider response cannot be mapped to a local card deterministically
- **THEN** the item SHALL be marked unresolved and included in error telemetry

### Requirement: Provider errors are telemetry, not budget control
Provider throttling or error responses SHALL NOT relax local call-limit enforcement semantics.

#### Scenario: Provider returns 429
- **WHEN** provider returns HTTP 429
- **THEN** the outbound request SHALL still consume one daily call unit
- **AND** the response SHALL be recorded as provider error telemetry
