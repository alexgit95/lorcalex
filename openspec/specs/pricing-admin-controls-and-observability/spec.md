# pricing-admin-controls-and-observability Specification

## Purpose
TBD - created by archiving change add-card-pricing-sync-with-daily-attempt-quota. Update Purpose after archive.
## Requirements
### Requirement: Admin pricing settings control
The system SHALL expose admin-configurable pricing settings including daily hard cap context, daily safety margin, sync enable flag, provider configuration, and scheduling configuration.

#### Scenario: Admin updates daily safety margin
- **WHEN** an admin updates pricing daily safety margin setting
- **THEN** subsequent pricing runs SHALL enforce the recalculated operational budget (hard cap minus safety margin)

### Requirement: Pricing status observability
The system SHALL expose pricing synchronization status including daily call hard cap, safety margin, effective operational budget, calls used, calls remaining, cursor position, processed pages, and stop reason.

#### Scenario: Admin checks pricing status
- **WHEN** admin requests pricing status
- **THEN** the response SHALL include effective daily budget telemetry and pagination progress telemetry

### Requirement: Admin schedule controls
The system SHALL expose admin controls for pricing schedule configuration, including cron expression and enable switch.

#### Scenario: Admin updates pricing schedule
- WHEN admin saves a new schedule configuration
- THEN subsequent automatic runs SHALL follow the new configuration without requiring application restart

### Requirement: Scheduling status observability
The system SHALL expose scheduling telemetry in admin status output.

#### Scenario: Admin checks scheduling status
- WHEN admin requests pricing status
- THEN response SHALL include schedule expression, schedule validity state, and last scheduled run date

