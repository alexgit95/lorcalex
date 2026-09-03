## ADDED Requirements

### Requirement: Promo rarity rows are ignored before mapping
When a provider set-card row's `rarity` field equals `Promo` (case-insensitive), the system SHALL skip that row before attempting local card mapping.

#### Scenario: Promo row skipped without mapping attempt
- **WHEN** a provider card-page row has `rarity` equal to `Promo` (any letter casing)
- **THEN** the system SHALL NOT attempt to resolve that row to a local card
- **AND** SHALL NOT attempt price extraction for that row

#### Scenario: Skipped promo row leaves no trace in sync telemetry
- **WHEN** a provider card-page row is skipped because its `rarity` is `Promo`
- **THEN** the sync run SHALL NOT increment any resolved, unresolved, success, or error counter for that row
- **AND** SHALL NOT include that row in mapping or price diagnostic samples
- **AND** SHALL NOT emit an unresolved-mapping diagnostic log line for that row, regardless of the unresolved-mapping logging setting

#### Scenario: Non-promo rows unaffected
- **WHEN** a provider card-page row's `rarity` is any value other than `Promo`
- **THEN** existing mapping, pricing, and telemetry behavior SHALL apply unchanged
