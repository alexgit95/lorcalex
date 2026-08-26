## ADDED Requirements

### Requirement: Strict daily budget on attempts
The system SHALL enforce a strict local daily budget based on attempted outbound pricing requests.

#### Scenario: Budget reached before dispatch
- **WHEN** used attempts are greater than or equal to daily budget
- **THEN** the system SHALL NOT dispatch any additional outbound pricing request

#### Scenario: Attempt always consumes budget
- **WHEN** one outbound pricing request is dispatched
- **THEN** exactly one budget unit SHALL be consumed regardless of provider outcome (success, 4xx, 5xx, timeout, network failure)

### Requirement: Persistent daily usage accounting
The system SHALL persist usage counters so restarts cannot bypass daily budget limits.

#### Scenario: Restart after partial usage
- **WHEN** the application restarts during the same usage date
- **THEN** remaining budget SHALL be computed from persisted usage counters

#### Scenario: Daily rollover
- **WHEN** current date differs from persisted usage date
- **THEN** usage counters SHALL reset before any new pricing dispatch
