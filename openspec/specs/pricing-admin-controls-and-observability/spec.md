# pricing-admin-controls-and-observability Specification

## Purpose
TBD - created by archiving change add-card-pricing-sync-with-daily-attempt-quota. Update Purpose after archive.
## Requirements
### Requirement: Admin pricing settings control
The system SHALL expose admin-configurable pricing settings, including daily budget, sync enable flag, provider key reference, and scheduling configuration.

#### Scenario: Admin updates daily budget
- **WHEN** an admin updates the pricing daily budget setting
- **THEN** subsequent pricing runs SHALL enforce the updated value

### Requirement: Pricing status observability
The system SHALL expose pricing synchronization status including date, budget used, budget remaining, processed count, and error summary.

#### Scenario: Admin checks pricing status
- **WHEN** admin requests pricing status
- **THEN** the response SHALL include current daily budget usage and operational progress fields

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

