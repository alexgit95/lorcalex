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

### Requirement: Admin-configurable abnormal low-rarity price alert log
The system SHALL expose an admin-configurable alert log, independent of the existing high market price log, that fires when a resolved card's price exceeds a configurable threshold and the provider row's rarity is within a configurable, admin-editable list of "low" rarities. The enable toggle, price threshold, and rarity list SHALL each be persisted via the application settings mechanism and take effect without requiring an application restart.

#### Scenario: Admin enables the abnormal price alert log
- **WHEN** an admin sets the abnormal price alert logging setting to enabled
- **THEN** subsequent pricing runs SHALL emit an alert log line for each card whose provider row rarity is in the configured rarity list and whose computed price exceeds the configured threshold

#### Scenario: Admin disables the abnormal price alert log
- **WHEN** an admin sets the abnormal price alert logging setting to disabled (the default)
- **THEN** subsequent pricing runs SHALL NOT emit the abnormal price alert log line, even when a card would otherwise match the rarity and threshold conditions

#### Scenario: Rarity outside the configured list does not trigger the alert
- **WHEN** the abnormal price alert log is enabled and a provider row's rarity is not present (case-insensitively) in the configured rarity list
- **THEN** the system SHALL NOT emit the abnormal price alert log line for that row, regardless of its computed price

#### Scenario: Admin customizes the monitored rarity list
- **WHEN** an admin sets the abnormal price alert rarity list to a custom comma-separated value
- **THEN** subsequent pricing runs SHALL match provider row rarities against that custom list instead of the default, case-insensitively

#### Scenario: Default rarity list and threshold apply when unset
- **WHEN** no abnormal price alert threshold or rarity list has been configured
- **THEN** the system SHALL use a default threshold of 5 EUR and a default rarity list of Common, Uncommon, rare, and Super_rare

#### Scenario: Alert log content includes provider return and computed price
- **WHEN** the abnormal price alert log is emitted for a card
- **THEN** the log line SHALL include the card identity, the matched rarity, the computed price, and the raw provider row payload

#### Scenario: Abnormal price alert is independent from the high market price log
- **WHEN** both the abnormal price alert log and the high market price log are enabled and a card's price satisfies both conditions
- **THEN** both log lines SHALL be emitted independently, with neither setting affecting the other's behavior

