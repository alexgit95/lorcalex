## ADDED Requirements

### Requirement: Card-listing responses omit rules text fields
Card-listing API responses (used to populate the Collection grid, including edition-scoped listing and name search) SHALL NOT include the card's rules text fields (body text, flavor text) in the response payload.

#### Scenario: Edition-scoped card listing
- **WHEN** cards are listed for a specific edition
- **THEN** the response for each card SHALL NOT include its rules text fields

#### Scenario: Full catalog listing
- **WHEN** cards are listed across all editions
- **THEN** the response for each card SHALL NOT include its rules text fields

#### Scenario: Name search listing
- **WHEN** cards are listed via a name search query
- **THEN** the response for each card SHALL NOT include its rules text fields

### Requirement: Single-card detail responses retain rules text fields
A single-card detail response (fetched by card id) SHALL continue to include the card's rules text fields (body text, flavor text).

#### Scenario: Single-card detail fetch
- **WHEN** a single card is fetched by its id
- **THEN** the response SHALL include its rules text fields
