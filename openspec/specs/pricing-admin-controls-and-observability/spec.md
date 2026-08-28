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

### Requirement: Admin-configurable pricing debug logging toggles
The system SHALL expose two independent admin-configurable settings to control pricing debug logging: one for the high market price detection log, and one for the unresolved mapping diagnostic log. Both SHALL be persisted via the application settings mechanism and take effect without requiring an application restart.

#### Scenario: Admin enables high market price log
- **WHEN** an admin sets the high market price logging setting to enabled
- **THEN** subsequent pricing runs SHALL emit a log line for each card whose resolved price exceeds the high price threshold

#### Scenario: Admin disables high market price log
- **WHEN** an admin sets the high market price logging setting to disabled
- **THEN** subsequent pricing runs SHALL NOT emit the high market price log line, even when a resolved price exceeds the high price threshold

#### Scenario: Admin enables unresolved mapping diagnostic log
- **WHEN** an admin sets the unresolved mapping logging setting to enabled
- **THEN** subsequent pricing runs SHALL emit one diagnostic log line per card that fails mapping resolution, containing the raw provider row payload and the mapping lookup criteria attempted (edition code, set number, card number, episode code set number, external id)

#### Scenario: Admin disables unresolved mapping diagnostic log
- **WHEN** an admin sets the unresolved mapping logging setting to disabled (the default)
- **THEN** subsequent pricing runs SHALL NOT emit the per-card unresolved mapping diagnostic log line

