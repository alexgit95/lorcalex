## ADDED Requirements

### Requirement: Recent activity timestamp reflects only genuine collection changes
`UserCollection.lastAddedAt` SHALL be updated only when a card is added to the collection, its owned quantity is manually changed, or its quantity is imported from an external source. It SHALL NOT be updated as a side effect of maintenance or data-repair operations.

#### Scenario: Adding a card updates recent activity
- **WHEN** a card is added to the collection via the add-card action
- **THEN** the card's `lastAddedAt` SHALL be set to the current time
- **AND** the card SHALL appear at the top of the "Récents" list

#### Scenario: Manually changing quantity updates recent activity
- **WHEN** a user manually updates the owned quantity or foil quantity of an already-owned card
- **THEN** the card's `lastAddedAt` SHALL be set to the current time

#### Scenario: Companion import updates recent activity
- **WHEN** owned quantities are imported from a Companion export for a card
- **THEN** the card's `lastAddedAt` SHALL be set to the current time

#### Scenario: Integrity repair does not affect recent activity
- **WHEN** the collection integrity repair job corrects an inconsistent `foil`, `quantity`, or `foilQuantity` value for a card
- **THEN** the card's `lastAddedAt` SHALL remain unchanged
- **AND** the card SHALL NOT be moved to the top of the "Récents" list as a result

#### Scenario: Backup restore preserves original recent activity timestamp
- **WHEN** a collection is restored from a backup that includes a `lastAddedAt` value for a card
- **THEN** the restored card's `lastAddedAt` SHALL match the value from the backup, not the restoration time
