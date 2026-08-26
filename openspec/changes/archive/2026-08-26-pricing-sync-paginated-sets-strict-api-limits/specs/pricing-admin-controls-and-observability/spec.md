## MODIFIED Requirements

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
