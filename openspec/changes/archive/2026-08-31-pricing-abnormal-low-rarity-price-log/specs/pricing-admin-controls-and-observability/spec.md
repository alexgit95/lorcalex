## ADDED Requirements

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
