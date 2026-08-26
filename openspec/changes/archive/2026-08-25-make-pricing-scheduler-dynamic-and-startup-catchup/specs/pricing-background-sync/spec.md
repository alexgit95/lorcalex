## ADDED Requirements

### Requirement: Configurable daily pricing schedule
The system SHALL execute pricing background synchronization on a configurable cron schedule stored in application settings.

#### Scenario: Valid cron configured
- WHEN pricing schedule setting contains a valid cron expression
- THEN the scheduler SHALL execute pricing synchronization according to that cron

#### Scenario: Invalid cron configured
- WHEN pricing schedule setting is invalid
- THEN the scheduler SHALL fallback to a safe default schedule
- AND scheduler status SHALL report the invalid configuration condition

### Requirement: Schedule enable switch
The system SHALL honor the pricing enable switch for all automatic schedule triggers.

#### Scenario: Schedule disabled
- WHEN pricing_sync_enabled is false
- THEN scheduled cron trigger SHALL NOT execute a pricing run
