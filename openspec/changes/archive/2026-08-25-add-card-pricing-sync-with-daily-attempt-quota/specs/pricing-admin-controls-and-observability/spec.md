## ADDED Requirements

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
