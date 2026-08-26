## ADDED Requirements

### Requirement: Pricing feature documentation updates
Any pricing-related behavior, quota, or scheduling change SHALL update README and CHANGELOG in the same change set.

#### Scenario: Pricing behavior changed in implementation
- **WHEN** a change modifies pricing sync behavior or settings contract
- **THEN** README SHALL be updated with the new behavior
- **AND** CHANGELOG SHALL include the pricing impact for that release
