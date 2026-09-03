# collection-value-trend-history Specification

## Purpose
This feature provides a historical view of the collection value and edition-level trends derived from pricing sync results.

## Requirements
### Requirement: Pricing sync creates valuation snapshots
The system SHALL compute and persist a collection valuation snapshot after each successful pricing synchronization run.

#### Scenario: successful pricing sync
- **WHEN** a pricing synchronization run completes successfully
- **THEN** the system SHALL recompute the current collection valuation in EUR
- **AND** SHALL persist a snapshot with the total collection value and timestamp
- **AND** SHALL use that snapshot as the source for the collection value trend graph

### Requirement: Global collection value trend
The system SHALL expose a time series of total collection valuation values for chart rendering.

#### Scenario: display collection value trend
- **WHEN** the user opens the collection value chart
- **THEN** the system SHALL return ordered snapshots by timestamp
- **AND** SHALL expose totalCollectionValueEur in EUR
- **AND** SHALL support delta computation for 7-day and 30-day periods

### Requirement: Edition-level value deltas
The system SHALL compute per-edition current value and 7-day / 30-day comparison values.

#### Scenario: display edition deltas
- **WHEN** the user views edition valuation rows
- **THEN** the system SHALL return the current value, 7-day value, 30-day value, delta 7d, and delta 30d
- **AND** SHALL compute the delta values using the valuation formula `(quantity + foilQuantity) x marketPrice`

### Requirement: EUR-only output
The system SHALL express all collection trend and delta outputs in EUR.

#### Scenario: monetary output uses EUR
- **WHEN** trend data is returned
- **THEN** all returned monetary values SHALL be expressed in EUR
- **AND** cards without price or with non-EUR price currency SHALL be excluded from the aggregates

### Requirement: historical baseline availability
The system SHALL handle missing 7-day or 30-day historical baselines gracefully.

#### Scenario: missing baseline data
- **WHEN** a 7-day or 30-day snapshot is unavailable
- **THEN** the system SHALL omit or null the corresponding delta rather than invent a value
- **AND** SHALL keep the output stable for the remaining valid snapshots
