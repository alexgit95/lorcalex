## ADDED Requirements

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
