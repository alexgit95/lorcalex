## Purpose

Define the Dreamborn-compatible CSV export contract for collection data.

## Requirements

### Requirement: Download a Dreamborn CSV from administration
The system SHALL provide an authenticated administrator endpoint at `GET /api/admin/export/dreamborn` and an administration action that downloads its result as a CSV file. The administration action SHALL allow the administrator to choose whether to apply the reserve, with the reserve enabled by default.

#### Scenario: Administrator downloads the export
- **WHEN** an authenticated administrator triggers the Dreamborn export action
- **THEN** the browser SHALL download the CSV returned by `GET /api/admin/export/dreamborn`

#### Scenario: Administrator disables the reserve
- **WHEN** an authenticated administrator disables the reserve before triggering the Dreamborn export action
- **THEN** the browser SHALL request the CSV with `reserve=false`

#### Scenario: Unauthenticated user requests the export
- **WHEN** a request without administrator authentication is made to `GET /api/admin/export/dreamborn`
- **THEN** the system SHALL reject the request according to the existing administration security policy

### Requirement: Produce the Dreamborn file format
The system SHALL return a UTF-8 `text/csv` response whose first line is exactly `Set Number,Card Number,Variant,Count`. Each subsequent line SHALL contain an edition set number, a card number, the literal variant `normal` or `foil`, and a strictly positive count in that column order.

#### Scenario: Export regular and foil variants
- **WHEN** a playable collection entry has positive regular and foil quantities after the reserve is applied
- **THEN** the CSV SHALL contain a separate row for each playable variant

#### Scenario: Exclude cards without Dreamborn identifiers
- **WHEN** a collection entry has no edition set number or no card number
- **THEN** the entry SHALL not produce any CSV row

### Requirement: Reserve one physical card per collection entry
The system SHALL derive export counts without changing stored collection quantities. The `reserve` query parameter SHALL control whether a card is reserved and SHALL default to `true` when omitted. When the reserve is enabled, for each eligible collection entry, the system SHALL reserve one foil card when a positive foil quantity exists; otherwise, it SHALL reserve one normal card when a positive normal quantity exists. When the reserve is disabled, the system SHALL export the stored normal and foil quantities without deduction. The system SHALL omit a variant whose final quantity is zero or less.

#### Scenario: Prefer reserving a foil card
- **WHEN** an eligible collection entry has two normal cards and one foil card
- **THEN** the CSV SHALL contain a normal row with count `2` and no foil row

#### Scenario: Reserve a normal card when no foil exists
- **WHEN** an eligible collection entry has two normal cards and no foil card
- **THEN** the CSV SHALL contain a normal row with count `1`

#### Scenario: Do not export a single owned card
- **WHEN** an eligible collection entry has exactly one card across its normal and foil quantities
- **THEN** the entry SHALL not produce any CSV row

#### Scenario: Export full quantities without the reserve
- **WHEN** an eligible collection entry has two normal cards and one foil card and the CSV is requested with `reserve=false`
- **THEN** the CSV SHALL contain a normal row with count `2` and a foil row with count `1`