# backup-restore-data-completeness Specification

## Purpose
TBD - created by archiving change fix-backup-restore-wanted-and-value-history. Update Purpose after archive.
## Requirements
### Requirement: Backup includes the wanted marker for every card
The full backup payload SHALL include each card's `wanted` marker alongside its other catalog attributes.

#### Scenario: Card marked as wanted
- **WHEN** a full backup is generated
- **THEN** each card entry in the payload SHALL include its current `wanted` value

### Requirement: Restore applies the wanted marker
Restoring a full backup SHALL set each recreated card's `wanted` marker from the payload.

#### Scenario: Restoring a wanted card
- **WHEN** a backup payload is restored and a card entry has `wanted` set to true
- **THEN** the recreated card SHALL have its `wanted` marker set to true

#### Scenario: Restoring a legacy payload without the wanted field
- **WHEN** a backup payload from a previous version without a `wanted` field is restored
- **THEN** the recreated card's `wanted` marker SHALL default to false

### Requirement: Backup includes collection and edition value history
The full backup payload SHALL include a `valueHistory` section containing all recorded collection-level value snapshots and edition-level value snapshots.

#### Scenario: Snapshots exist
- **WHEN** a full backup is generated and value snapshots exist
- **THEN** the payload SHALL include a `valueHistory` section listing all collection-level and edition-level value snapshots with their recorded values, currency, source, and timestamps

#### Scenario: No snapshots exist
- **WHEN** a full backup is generated and no value snapshots exist
- **THEN** the payload's `valueHistory` section SHALL be present but empty

### Requirement: Restore applies value history with remapped edition references
Restoring a full backup SHALL recreate the collection-level and edition-level value snapshots, remapping each edition-level snapshot's edition reference to the corresponding newly-created edition.

#### Scenario: Restoring edition-level snapshots for an edition still present in the backup
- **WHEN** a backup payload is restored and an edition-level value snapshot references an edition that is also present in the payload's editions
- **THEN** the recreated snapshot SHALL reference the newly-created edition's id

#### Scenario: Restoring edition-level snapshots for an edition no longer present
- **WHEN** a backup payload is restored and an edition-level value snapshot references an edition not present in the payload's editions
- **THEN** that snapshot SHALL be skipped during restore

#### Scenario: Restoring a legacy payload without value history
- **WHEN** a backup payload from a previous version without a `valueHistory` section is restored
- **THEN** the restore SHALL succeed with no value snapshots recreated

### Requirement: API keys and admin credentials remain excluded from backup and restore
The full backup payload SHALL NOT include API key records or admin user credentials, and restoring a backup SHALL NOT modify existing API keys or admin user credentials.

#### Scenario: Backup does not include API keys or credentials
- **WHEN** a full backup is generated
- **THEN** the payload SHALL NOT include API key records or admin user credentials

#### Scenario: Restore does not modify API keys or credentials
- **WHEN** a full backup is restored
- **THEN** existing API key records and admin user credentials SHALL remain unchanged

