# preferred-card-filter Specification

## Purpose

Décrire le filtre de la collection permettant d'afficher les cartes préférées.

## Requirements

### Requirement: Filter collection cards by preferred status
The Collection page SHALL provide a filter labeled **Préférées** alongside the existing collection status filters. When selected, the grid SHALL display every catalog card whose `wanted` marker is true, regardless of whether the card is owned or missing.

#### Scenario: Display preferred owned and missing cards
- **WHEN** the user selects the **Préférées** filter for an edition
- **THEN** the grid SHALL include preferred cards that are owned
- **AND** the grid SHALL include preferred cards that are missing
- **AND** the grid SHALL exclude cards whose `wanted` marker is false

#### Scenario: Respect the selected edition
- **WHEN** the user selects an edition and then selects the **Préférées** filter
- **THEN** the grid SHALL display only preferred cards belonging to the selected edition

#### Scenario: Respect the card name search
- **WHEN** the **Préférées** filter is active and the user enters a valid card name search
- **THEN** the grid SHALL display only preferred cards matching that search within the selected edition scope

#### Scenario: No preferred cards match
- **WHEN** the **Préférées** filter is active and no loaded card has `wanted` set to true within the active search and edition scope
- **THEN** the grid SHALL display the existing empty-state view