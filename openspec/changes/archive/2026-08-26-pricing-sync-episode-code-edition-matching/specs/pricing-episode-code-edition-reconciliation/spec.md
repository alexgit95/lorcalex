## ADDED Requirements

### Requirement: Episode code numeric identity extraction
The system SHALL extract the leading numeric segment from provider `episode.code` values and use it as deterministic set identity input for card reconciliation.

#### Scenario: Episode code contains numeric prefix
- **WHEN** a provider card row includes `episode.code` with alphanumeric format such as `11WSP`
- **THEN** the system SHALL derive numeric identity `11` from the leading digits
- **AND** the derived value SHALL be available to deterministic local card lookup with `card_number`

#### Scenario: Episode code has no leading digits
- **WHEN** a provider card row includes `episode.code` without a leading numeric segment
- **THEN** the system SHALL skip episode-code-derived numeric identity for that row
- **AND** the mapping engine SHALL continue with deterministic fallback identifiers

### Requirement: Deterministic fallback mapping after episode normalization
The system SHALL apply deterministic fallback mapping when episode-code-derived identity is unavailable or does not produce a match.

#### Scenario: Episode-derived identity does not resolve a local card
- **WHEN** matching by episode-derived numeric identity and `card_number` fails
- **THEN** the system SHALL attempt configured numeric set fields with `card_number`
- **AND** next SHALL attempt edition-code plus `card_number`
- **AND** finally SHALL attempt external-id matching

#### Scenario: No deterministic identifier yields a match
- **WHEN** all deterministic matching attempts fail for a provider row
- **THEN** the row SHALL be marked unresolved
- **AND** unresolved diagnostics SHALL include enough identifiers to explain the failure
