## ADDED Requirements

### Requirement: Collection value trend point deletion
The system SHALL allow an authenticated user to permanently delete a single collection value trend point from the Pricing tab, along with every edition-level value snapshot recorded at the same timestamp as that point.

#### Scenario: Trend point exposes its identifier
- **WHEN** the Pricing tab requests the collection value trend
- **THEN** each returned trend point SHALL include the underlying snapshot's identifier in addition to its recorded timestamp and total value

#### Scenario: User deletes a trend point
- **WHEN** an authenticated user requests deletion of a trend point by its identifier
- **THEN** the system SHALL permanently delete the corresponding collection value snapshot
- **AND** the system SHALL permanently delete every edition-level value snapshot recorded at the same timestamp as the deleted collection value snapshot
- **AND** the deletion SHALL NOT be reversible

#### Scenario: Deleted point is removed from the trend
- **WHEN** a trend point has been deleted
- **THEN** subsequent requests for the collection value trend SHALL NOT include that point

#### Scenario: Edition deltas adjust after deletion without manual recalculation
- **WHEN** a trend point (and its correlated edition-level snapshots) has been deleted
- **THEN** subsequent edition-delta calculations SHALL use the next-closest remaining snapshot as their reference, without requiring any other snapshot to be recomputed or modified

#### Scenario: Deletion of a non-existent trend point
- **WHEN** deletion is requested for a trend point identifier that does not exist
- **THEN** the system SHALL return an error response indicating the point was not found

#### Scenario: History list is collapsed by default
- **WHEN** an authenticated user opens the Pricing tab
- **THEN** the detailed trend point history list SHALL be collapsed by default
- **AND** the user SHALL be able to expand it to view and delete individual points

#### Scenario: Trend chart remains read-only
- **WHEN** an authenticated user views the collection value trend chart
- **THEN** the chart itself SHALL NOT provide a way to delete a point directly
- **AND** point deletion SHALL only be available through the detailed history list
