# collection-ownership-and-foil-invariants Specification

## Purpose
TBD - created by archiving change formalize-product-governance-and-compatibility. Update Purpose after archive.
## Requirements
### Requirement: Owned card semantics
A card MUST be considered owned if and only if quantity is greater than zero OR foilQuantity is greater than zero.

#### Scenario: Owned by normal copy
- **WHEN** quantity is greater than zero and foilQuantity is zero
- **THEN** the card SHALL be marked as owned

#### Scenario: Owned by foil copy only
- **WHEN** quantity is zero and foilQuantity is greater than zero
- **THEN** the card SHALL be marked as owned

#### Scenario: Not owned
- **WHEN** quantity is zero and foilQuantity is zero
- **THEN** the card SHALL be marked as not owned

### Requirement: Foil consistency invariant
The foil flag MUST equal the expression foilQuantity > 0 for persisted and returned collection records.

#### Scenario: Positive foil quantity
- **WHEN** foilQuantity is greater than zero
- **THEN** foil SHALL be true

#### Scenario: Zero foil quantity
- **WHEN** foilQuantity is zero
- **THEN** foil SHALL be false

### Requirement: Stats completion semantics
Collection completion metrics MUST count distinct owned cards, not total copy volume.

#### Scenario: Duplicate copies do not increase completion
- **WHEN** a user increases copies of an already-owned card
- **THEN** completion percentage SHALL remain unchanged
- **AND** only volume-oriented metrics MAY increase

