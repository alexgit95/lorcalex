## ADDED Requirements

### Requirement: Strict daily call hard cap with safety margin
The system SHALL enforce a strict daily outbound call hard cap and an effective operational budget reduced by a safety margin.

#### Scenario: Effective budget reached before hard cap
- **WHEN** used daily calls are greater than or equal to effective operational budget
- **THEN** the system SHALL stop dispatching new provider calls for the day

#### Scenario: Safety margin configured
- **WHEN** a positive safety margin is configured
- **THEN** effective operational budget SHALL equal daily hard cap minus safety margin

### Requirement: Strict per-minute call cap
The system SHALL enforce a strict outbound call cap of at most 30 calls per minute.

#### Scenario: Minute cap would be exceeded
- **WHEN** dispatching the next provider request would exceed 30 calls in the current rolling minute
- **THEN** the system SHALL delay or refuse dispatch until within limit

### Requirement: Outbound call accounting includes retries and failures
Every outbound provider HTTP request SHALL consume one daily call unit, including retries and non-success responses.

#### Scenario: Provider call fails
- **WHEN** a provider request returns 4xx, 5xx, timeout, or network error
- **THEN** that request SHALL still count as one consumed daily call

#### Scenario: Retry is attempted
- **WHEN** the system retries a failed provider request
- **THEN** each retry request SHALL consume an additional daily call unit
