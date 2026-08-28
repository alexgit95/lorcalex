# collection-search-debounced-filtering Specification

## Purpose
TBD - created by archiving change improve-collection-grid-performance. Update Purpose after archive.
## Requirements
### Requirement: Collection search requires a minimum length and is debounced
The Collection page search input SHALL only apply its filter once the entered query is at least 3 characters long, and SHALL debounce filtering by 300ms after the last keystroke before re-rendering the grid.

#### Scenario: Query below minimum length
- **WHEN** the Collection search query has fewer than 3 characters
- **THEN** the grid SHALL show all cards for the currently selected edition, unfiltered by the query

#### Scenario: Query below minimum length shows a hint
- **WHEN** the Collection search query is non-empty but has fewer than 3 characters
- **THEN** a hint indicating a 3-character minimum SHALL be shown, instead of the normal "no results" empty state

#### Scenario: Query reaches minimum length
- **WHEN** the Collection search query has 3 or more characters
- **THEN** the grid SHALL filter to cards matching the query 300ms after the last keystroke

#### Scenario: Rapid typing debounces filtering
- **WHEN** the user types multiple characters in quick succession in the Collection search input
- **THEN** the grid SHALL only re-filter once, 300ms after the last keystroke, not on every intermediate keystroke

