## MODIFIED Requirements

### Requirement: Admin-configurable pricing debug logging toggles
The system SHALL expose two independent admin-configurable settings to control pricing debug logging: one for the high market price detection log, and one for the unresolved mapping diagnostic log. Both SHALL be persisted via the application settings mechanism and take effect without requiring an application restart. The high market price log threshold itself SHALL also be admin-configurable as an integer EUR value, persisted the same way.

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

#### Scenario: Admin configures a custom high price log threshold
- **WHEN** an admin sets the high market price log threshold to an integer value
- **THEN** subsequent pricing runs SHALL use that value, instead of the previous default, to decide whether a resolved card price triggers the high market price log line
- **AND** the comparison SHALL remain strictly greater than the configured threshold

#### Scenario: High price log threshold defaults when unset
- **WHEN** no high market price log threshold has been configured
- **THEN** the system SHALL use a default threshold of 5 EUR, preserving prior behavior

#### Scenario: Negative threshold value is rejected in effect
- **WHEN** the stored high market price log threshold value is negative
- **THEN** the system SHALL treat the effective threshold as 0 instead of a negative number
