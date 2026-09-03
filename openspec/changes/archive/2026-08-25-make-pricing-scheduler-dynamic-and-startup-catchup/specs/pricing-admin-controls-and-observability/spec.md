## ADDED Requirements

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
