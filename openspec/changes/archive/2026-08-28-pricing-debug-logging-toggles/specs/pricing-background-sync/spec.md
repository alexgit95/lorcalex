## ADDED Requirements

### Requirement: Unresolved mapping diagnostic logging
When the unresolved mapping logging setting is enabled, the system SHALL emit one diagnostic log line for every card row that fails mapping resolution (`UNRESOLVED_MAPPING`) during a pricing sync run, independent of the existing capped sample list included in the sync report.

#### Scenario: Unresolved mapping row logged when enabled
- **WHEN** a provider row fails card mapping resolution during a pricing sync run and the unresolved mapping logging setting is enabled
- **THEN** the system SHALL emit a single log line containing the raw provider row and the mapping lookup criteria attempted (edition code, set number, card number, episode code set number, external id)
- **AND** this SHALL occur for every unresolved row in the run, not only the first three

#### Scenario: Unresolved mapping row not logged when disabled
- **WHEN** a provider row fails card mapping resolution during a pricing sync run and the unresolved mapping logging setting is disabled
- **THEN** the system SHALL NOT emit the per-row diagnostic log line
- **AND** the existing capped sample list in the sync report SHALL be unaffected
