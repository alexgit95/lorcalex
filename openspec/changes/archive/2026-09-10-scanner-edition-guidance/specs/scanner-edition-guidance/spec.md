## ADDED Requirements

### Requirement: Scanner edition selector
The system SHALL display an edition selector in the Scanner using the editions available in the catalog. The selector SHALL include `Toutes les editions` as its initial value and SHALL not persist a specific edition when the Scanner is reopened.

#### Scenario: Scanner opens with the default scope
- **WHEN** a user opens the Scanner
- **THEN** the edition selector SHALL be set to `Toutes les editions`
- **AND** card resolution SHALL search across all editions

#### Scenario: Available editions populate the selector
- **WHEN** the Scanner loads the catalog editions successfully
- **THEN** the selector SHALL present each available edition as a selectable option

### Requirement: Selected edition strictly scopes card resolution
The system SHALL restrict Scanner card resolution to the explicitly selected edition for both OCR captures and manual number lookups. The system SHALL NOT fall back to another edition when no card matches within the selected edition.

#### Scenario: OCR capture resolves within the selected edition
- **WHEN** OCR recognizes a card number while a specific edition is selected
- **THEN** the system SHALL return only cards from that selected edition matching the recognized number

#### Scenario: Manual lookup resolves within the selected edition
- **WHEN** a user submits a manual card number while a specific edition is selected
- **THEN** the system SHALL return only cards from that selected edition matching the entered number

#### Scenario: No selected-edition match exists
- **WHEN** a recognized or manually entered card number has no match in the selected edition
- **THEN** the system SHALL report that no card was found in the selected edition
- **AND** the system SHALL NOT present matches from other editions

### Requirement: Global resolution retains OCR set disambiguation
The system SHALL preserve existing global resolution behavior when `Toutes les editions` is selected. In that state, a set number recognized by OCR or entered manually SHALL only be used to narrow multiple global matches.

#### Scenario: OCR set number narrows global candidates
- **WHEN** the edition selector is set to `Toutes les editions`
- **AND** OCR recognizes a card number and set number with multiple matching cards
- **THEN** the system SHALL prefer candidates from the recognized set when such candidates exist

#### Scenario: Manual set entry narrows global candidates
- **WHEN** the edition selector is set to `Toutes les editions`
- **AND** a user enters both a card number and a set number with multiple matching cards
- **THEN** the system SHALL prefer candidates from the entered set when such candidates exist

### Requirement: Manual set entry reflects edition scope
The system SHALL disable the manual set number input while a specific edition is selected and SHALL enable it when `Toutes les editions` is selected.

#### Scenario: Specific edition disables manual set entry
- **WHEN** a user selects a specific edition in the Scanner
- **THEN** the manual set number input SHALL be disabled

#### Scenario: Returning to global scope enables manual set entry
- **WHEN** a user changes the selector from a specific edition to `Toutes les editions`
- **THEN** the manual set number input SHALL be enabled