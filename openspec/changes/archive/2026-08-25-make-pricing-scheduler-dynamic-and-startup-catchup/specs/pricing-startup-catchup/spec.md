## ADDED Requirements

### Requirement: Startup catch-up for missed daily run
The system SHALL perform a single startup catch-up run when the daily scheduled run has not yet been executed for the current day.

#### Scenario: Startup with missed daily run
- WHEN application starts and last scheduled run date differs from current date
- THEN the system SHALL trigger one startup catch-up run

#### Scenario: Startup after daily run already executed
- WHEN application starts and last scheduled run date equals current date
- THEN the system SHALL NOT trigger a startup catch-up run

### Requirement: Catch-up respects existing execution guards
Startup catch-up SHALL use the same guards and semantics as regular runs.

#### Scenario: Catch-up while sync already running
- WHEN startup catch-up is requested while another pricing run is in progress
- THEN catch-up SHALL be skipped without starting a concurrent run

#### Scenario: Catch-up with exhausted budget
- WHEN startup catch-up runs with zero remaining daily budget
- THEN no provider request SHALL be dispatched
- AND run result SHALL report budget-blocked behavior
