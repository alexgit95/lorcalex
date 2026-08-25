## ADDED Requirements

### Requirement: Import/export schema compatibility policy
Backup and export payload evolution MUST follow an N/N-1 compatibility contract.

#### Scenario: Importing N-1 payload on N runtime
- **WHEN** a payload produced by version N-1 is imported on runtime version N
- **THEN** the import SHALL succeed with documented compatibility behavior
- **AND** any migration steps SHALL be deterministic and documented

### Requirement: Mandatory compatibility test coverage
Any schema or field-level change affecting backup or export payloads MUST include automated compatibility tests for N and N-1 payload fixtures.

#### Scenario: Schema change without compatibility tests
- **WHEN** a change modifies import/export payload structure
- **THEN** the change SHALL be considered non-compliant if N and N-1 compatibility tests are absent or failing

### Requirement: Documentation of compatibility changes
Any compatibility-impacting payload change MUST be documented in README and changelog.

#### Scenario: Payload contract update
- **WHEN** payload schema behavior is changed
- **THEN** README SHALL be updated with the contract implications
- **AND** changelog SHALL include a compatibility note for the release
