# card-grid-thumbnail-images Specification

## Purpose
TBD - created by archiving change optimize-card-listing-payload. Update Purpose after archive.
## Requirements
### Requirement: Collection grid uses the lightweight thumbnail image
The Collection page grid SHALL display each card's thumbnail image instead of its full-resolution image. If a card has no thumbnail image available, the grid SHALL fall back to the full-resolution image for that card.

#### Scenario: Card has a thumbnail image
- **WHEN** a card with a thumbnail image is displayed in the Collection grid
- **THEN** the grid SHALL load and display the thumbnail image, not the full-resolution image

#### Scenario: Card has no thumbnail image
- **WHEN** a card without a thumbnail image is displayed in the Collection grid
- **THEN** the grid SHALL fall back to displaying the full-resolution image

### Requirement: Card detail view continues to use the full-resolution image
The card detail view (modal) SHALL continue to display each card's full-resolution image, unaffected by the Collection grid's thumbnail usage.

#### Scenario: Detail view unaffected
- **WHEN** a card's detail view is opened
- **THEN** it SHALL display the full-resolution image, regardless of what the grid displayed

