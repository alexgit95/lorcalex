## ADDED Requirements

### Requirement: Mandatory README and changelog updates per behavior change
Any change that modifies user-visible behavior or operational workflow MUST include corresponding README updates and a changelog entry.

#### Scenario: Functional pull request validation
- **WHEN** a pull request changes user-visible behavior
- **THEN** it SHALL include updates to README sections affected by that behavior
- **AND** it SHALL include a changelog update for the target release

### Requirement: Changelog version uniqueness
The changelog MUST contain exactly one authoritative section per released version.

#### Scenario: Duplicate version entries detected
- **WHEN** multiple changelog sections are found for the same semantic version
- **THEN** duplicate entries SHALL be merged into one authoritative version section
- **AND** conflicting statements SHALL be reconciled before release publication
