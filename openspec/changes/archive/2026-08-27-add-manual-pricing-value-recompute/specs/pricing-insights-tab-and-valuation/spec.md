## ADDED Requirements

### Requirement: Manual collection value recomputation
The system SHALL allow an authenticated user to manually trigger a recomputation of the collection value snapshot (total collection value and per-edition valuation) from prices currently stored in the database, without contacting the pricing provider.

#### Scenario: User triggers manual recomputation
- **WHEN** an authenticated user submits a manual recomputation request from the Pricing tab
- **THEN** the system SHALL recompute the total collection value and per-edition valuation from currently stored card prices
- **AND** the system SHALL persist a new collection value snapshot and per-edition value snapshots
- **AND** the system SHALL NOT call the external pricing provider or consume pricing sync API budget

#### Scenario: Recomputation succeeds
- **WHEN** the manual recomputation completes successfully
- **THEN** the system SHALL return a success response
- **AND** the Pricing tab SHALL display a success confirmation
- **AND** the Pricing tab SHALL refresh its displayed insights, value trend, and edition deltas to reflect the new snapshot

#### Scenario: Recomputation fails
- **WHEN** the manual recomputation fails due to an unexpected error
- **THEN** the system SHALL return an error response including the error message and root cause
- **AND** the Pricing tab SHALL display the error message and root cause to the user

#### Scenario: Recomputation in progress
- **WHEN** a manual recomputation request is in flight
- **THEN** the Pricing tab SHALL disable the trigger control and indicate that a recomputation is in progress
- **AND** the trigger control SHALL be re-enabled once the request completes, whether it succeeds or fails
