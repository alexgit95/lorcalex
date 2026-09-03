# companion-import-modes-and-reporting Specification

## Purpose
TBD - created by archiving change formalize-product-governance-and-compatibility. Update Purpose after archive.
## Requirements
### Requirement: Explicit companion import modes
Companion import MUST support two explicit modes: merge and replace.

#### Scenario: Merge mode behavior
- **WHEN** companion import is executed in merge mode
- **THEN** imported quantities SHALL be added to existing stored quantities

#### Scenario: Replace mode behavior
- **WHEN** companion import is executed in replace mode
- **THEN** existing stored quantities SHALL be replaced by imported quantities

### Requirement: Detailed import report
Companion import completion MUST produce a structured report including imported count, skipped count, and categorized skip causes.

#### Scenario: Import report generation
- **WHEN** a companion import operation completes
- **THEN** the result SHALL include imported total
- **AND** it SHALL include skipped total
- **AND** it SHALL include explicit causes for skipped entries (for example unknown card mapping or invalid row format)

