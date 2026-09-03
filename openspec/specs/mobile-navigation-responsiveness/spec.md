# mobile-navigation-responsiveness Specification

## Purpose
TBD - created by archiving change fix-bottom-nav-mobile-overflow. Update Purpose after archive.

## Requirements
### Requirement: Bottom navigation visibility on narrow mobile viewports
The system SHALL keep all bottom navigation items fully visible and reachable on mobile viewports as narrow as 360px wide.

#### Scenario: All nav items visible on a narrow phone
- **WHEN** the application is loaded on a viewport 360-390px wide
- **THEN** all bottom navigation items, including the last one, SHALL be rendered within the visible viewport width
- **AND** no navigation item SHALL be clipped or rendered outside the visible area

#### Scenario: Each nav item remains tappable
- **WHEN** the application is loaded on a viewport 360-390px wide
- **THEN** each bottom navigation item SHALL remain individually tappable with its icon and label visible
