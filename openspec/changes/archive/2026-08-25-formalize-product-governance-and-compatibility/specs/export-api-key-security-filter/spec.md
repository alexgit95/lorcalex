## ADDED Requirements

### Requirement: Dedicated export API key security filter
Validation of API keys for export access MUST be enforced by a dedicated security filter in the request chain.

#### Scenario: Missing or invalid key
- **WHEN** a request targets the export endpoint without a valid API key
- **THEN** the security filter SHALL reject the request with HTTP 403

#### Scenario: Valid key
- **WHEN** a request targets the export endpoint with a valid non-expired API key
- **THEN** the security filter SHALL allow request processing to continue
- **AND** last-used metadata SHALL be updated
