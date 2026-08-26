## ADDED Requirements

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
