# import-export-compatibility-contract Specification

## Purpose
TBD - created by archiving change formalize-product-governance-and-compatibility. Update Purpose after archive.
## Requirements
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

### Requirement: Pricing fields follow N/N-1 compatibility
Any pricing fields added to backup/export payloads SHALL respect N/N-1 compatibility policy.

#### Scenario: Importing previous payload version
- **WHEN** runtime version N imports a payload from N-1 without pricing fields
- **THEN** import SHALL succeed with safe defaults for missing pricing metadata

#### Scenario: Exporting current payload version
- **WHEN** runtime version N exports backup data
- **THEN** pricing metadata SHALL be present according to current schema contract

#### Scenario: Import/export preserves card value
- **WHEN** runtime version N exports a card containing pricing value and later re-imports that payload
- **THEN** the imported card SHALL preserve the same pricing value as exported
- **AND** currency and source associated with that value SHALL remain consistent
- **AND** the last pricing update timestamp (last scan date) SHALL be preserved for refresh prioritization

### Requirement: Pricing compatibility tests are mandatory
Pricing payload evolution SHALL include unit and integration compatibility tests for N and N-1 fixtures.

#### Scenario: Pricing schema changed
- **WHEN** pricing payload schema is modified
- **THEN** compatibility tests for N and N-1 SHALL be updated and pass before merge

