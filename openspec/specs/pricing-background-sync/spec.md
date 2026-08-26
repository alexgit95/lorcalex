# pricing-background-sync Specification

## Purpose
TBD - created by archiving change add-card-pricing-sync-with-daily-attempt-quota. Update Purpose after archive.
## Requirements
### Requirement: Daily background pricing run
The system SHALL execute a recurring background pricing synchronization run that processes cards until budget exhaustion or queue depletion.

#### Scenario: Scheduled run starts with remaining budget
- **WHEN** the daily pricing trigger fires and remaining budget is positive
- **THEN** the run SHALL process cards following prioritization rules until budget is exhausted or no eligible cards remain

#### Scenario: Scheduled run starts with zero remaining budget
- **WHEN** the daily pricing trigger fires and remaining budget is zero
- **THEN** no provider request SHALL be dispatched and the run SHALL terminate immediately

### Requirement: Manual trigger uses same guards
Any manual admin-triggered pricing run SHALL use the same budget and prioritization constraints as scheduled runs.

#### Scenario: Manual run after budget exhaustion
- **WHEN** admin triggers a run while daily budget is exhausted
- **THEN** the run SHALL refuse to dispatch requests and report zero remaining budget

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

