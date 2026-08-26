# pricing-background-sync Specification

## Purpose
TBD - created by archiving change add-card-pricing-sync-with-daily-attempt-quota. Update Purpose after archive.
## Requirements
### Requirement: Daily background pricing run
The system SHALL execute a recurring background pricing synchronization run that processes paginated set/card retrieval until effective daily operational budget exhaustion or traversal completion.

#### Scenario: Scheduled run starts with remaining operational budget
- **WHEN** the daily pricing trigger fires and remaining operational budget is positive
- **THEN** the run SHALL continue paginated traversal from persisted cursor and process updates until traversal completion or budget exhaustion

#### Scenario: Scheduled run starts with zero remaining operational budget
- **WHEN** the daily pricing trigger fires and remaining operational budget is zero
- **THEN** no provider request SHALL be dispatched and the run SHALL terminate immediately

### Requirement: Startup does not auto-trigger pricing synchronization
The system SHALL NOT launch pricing synchronization automatically at application startup.

#### Scenario: Application startup completes
- **WHEN** the application becomes ready
- **THEN** no startup catch-up pricing run SHALL be triggered automatically
- **AND** pricing synchronization SHALL only run from scheduled triggers or explicit manual admin triggers

### Requirement: Manual trigger uses same guards
Any manual admin-triggered pricing run SHALL use the same daily and per-minute call guards, pagination semantics, and prioritization constraints as scheduled runs.

#### Scenario: Manual run after budget exhaustion
- **WHEN** admin triggers a run while daily operational budget is exhausted
- **THEN** the run SHALL refuse to dispatch requests and report zero remaining operational budget

#### Scenario: Manual run with minute limiter pressure
- **WHEN** admin triggers a run while minute limiter threshold is reached
- **THEN** the run SHALL defer additional calls until minute limits allow dispatch

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

