# collection-grid-lazy-image-loading Specification

## Purpose
TBD - created by archiving change improve-collection-grid-performance. Update Purpose after archive.
## Requirements
### Requirement: Collection grid images load only near the viewport
Card images in the Collection page grid SHALL NOT be fetched until the card approaches the viewport, within approximately one screen-height of margin. Images already loaded SHALL NOT be reloaded as the user continues scrolling.

#### Scenario: Card far below the viewport
- **WHEN** a card in the Collection grid is far enough below the visible viewport (beyond the loading margin)
- **THEN** its image SHALL NOT be fetched yet

#### Scenario: Card approaching the viewport
- **WHEN** a card in the Collection grid scrolls to within approximately one screen-height of the viewport
- **THEN** its image SHALL be fetched and displayed

#### Scenario: Previously loaded card scrolled past
- **WHEN** a card whose image has already loaded is scrolled out of view and back into view
- **THEN** its image SHALL NOT be re-fetched

### Requirement: Pending card images reserve their layout space
A card whose image has not yet loaded SHALL display a placeholder occupying the same space the image will use once loaded, so that no layout shift occurs when the image finishes loading.

#### Scenario: Image not yet loaded
- **WHEN** a card's image has not yet been fetched or is still loading
- **THEN** the card SHALL display a placeholder reserving the image's final dimensions

#### Scenario: Image finishes loading
- **WHEN** a card's image finishes loading
- **THEN** the placeholder SHALL be replaced by the image without changing the card's layout position or size

### Requirement: Lazy image loading applies only to the Collection page grid
The viewport-proximity loading behavior SHALL apply only to the Collection page's card grid, not to other card grids (e.g. Recent scans, Pricing).

#### Scenario: Other grids unaffected
- **WHEN** a card grid other than the Collection page grid is displayed
- **THEN** its image loading behavior SHALL be unaffected by this requirement

