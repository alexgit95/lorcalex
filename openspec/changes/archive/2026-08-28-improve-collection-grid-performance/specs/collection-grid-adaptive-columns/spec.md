## ADDED Requirements

### Requirement: Collection grid column count is capped and screen-adaptive
The Collection page grid SHALL display at most 10 columns regardless of viewport width, while using the same natural column count as before on narrower screens (no different minimum than prior behavior). The column count SHALL recompute live when the viewport or grid container is resized.

#### Scenario: Wide desktop viewport
- **WHEN** the Collection grid is displayed on a viewport wide enough that the prior unbounded layout would exceed 10 columns
- **THEN** the grid SHALL display exactly 10 columns

#### Scenario: Narrow or mobile viewport
- **WHEN** the Collection grid is displayed on a viewport narrow enough that the prior layout would produce fewer than 10 columns
- **THEN** the grid SHALL display the same column count as the prior unbounded layout would have produced

#### Scenario: Window resized while viewing the Collection grid
- **WHEN** the browser window or grid container is resized while the Collection page is open
- **THEN** the column count SHALL be recomputed and applied without requiring a page reload

### Requirement: Column cap applies only to the Collection page grid
The 10-column cap and adaptive recomputation SHALL apply only to the Collection page's card grid, not to other card grids (e.g. Recent scans, Pricing).

#### Scenario: Other grids unaffected
- **WHEN** a card grid other than the Collection page grid is displayed
- **THEN** its column layout SHALL be unaffected by the Collection grid's column cap logic
