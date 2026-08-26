## MODIFIED Requirements

### Requirement: Strict daily budget on attempts
The system SHALL enforce a strict local daily outbound-call budget based on attempted provider HTTP requests, with an operational safety margin below the hard daily cap.

#### Scenario: Operational budget reached before dispatch
- **WHEN** used daily outbound calls are greater than or equal to effective operational budget
- **THEN** the system SHALL NOT dispatch any additional outbound provider request

#### Scenario: Outbound request always consumes budget
- **WHEN** one outbound provider HTTP request is dispatched
- **THEN** exactly one daily call unit SHALL be consumed regardless of provider outcome (success, 4xx, 5xx, timeout, network failure)

### Requirement: Persistent daily usage accounting
The system SHALL persist daily call usage counters so restarts cannot bypass daily call budget limits.

#### Scenario: Restart after partial daily usage
- **WHEN** the application restarts during the same usage date
- **THEN** remaining operational budget SHALL be computed from persisted usage counters

#### Scenario: Daily rollover
- **WHEN** current date differs from persisted usage date
- **THEN** usage counters SHALL reset before any new outbound provider dispatch
