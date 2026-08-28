# stats-missing-cards-breakdown Specification

## Purpose
TBD - created by archiving change stats-missing-cards-color-rarity-table. Update Purpose after archive.
## Requirements
### Requirement: Missing cards breakdown table by edition, ink color and rarity
The system SHALL display, in the Stats tab, a table of missing cards per tracked edition broken down by ink color and rarity, replacing the previous per-edition rarity charts.

#### Scenario: Edition with missing cards is displayed
- **WHEN** a tracked edition has at least one missing card across any ink color or rarity
- **THEN** the table SHALL include one row for that edition
- **AND** the row SHALL show, for each of the 6 ink colors, the missing card count broken down by rarity (Commune, Inhabituelle, Rare, Très Rare, Légendaire, in that order), including only rarities with a missing count greater than zero
- **AND** the row SHALL show a subtotal of missing cards for each ink color that has at least one missing card
- **AND** the row SHALL show a total missing card count across all colors and rarities for that edition

#### Scenario: Edition with zero missing cards is hidden
- **WHEN** a tracked edition has zero missing cards across every ink color and rarity
- **THEN** the table SHALL NOT include a row for that edition

#### Scenario: Ink color with zero missing cards for an edition
- **WHEN** a tracked edition has zero missing cards for a given ink color
- **THEN** the corresponding table cell SHALL be empty for that ink color

#### Scenario: Ink color and rarity icons used instead of text labels
- **WHEN** the table is rendered
- **THEN** each of the 6 ink color columns SHALL be labeled with the corresponding ink color icon instead of a text label
- **AND** each rarity entry within a cell SHALL be shown with the corresponding rarity icon instead of a text abbreviation
- **AND** each icon SHALL provide an accessible text equivalent (title/alt) and fall back to the color or rarity name as text if the icon image fails to load

#### Scenario: Tracked editions filter is respected
- **WHEN** the admin has configured a subset of tracked editions via the stats set filter
- **THEN** the table SHALL only include rows for editions in that tracked subset, consistent with the rest of the Stats tab
