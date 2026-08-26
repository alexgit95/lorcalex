# pricing-set-paginated-sync Specification

## Purpose
Define set-first provider pagination and durable resume behavior for pricing synchronization.

## Requirements
### Requirement: Paginated set discovery
The system SHALL discover pricing sets from the provider using paginated episode listing until all pages are processed.

#### Scenario: Episode listing spans multiple pages
- **WHEN** provider episode paging indicates more than one page
- **THEN** the system SHALL request each page sequentially until `paging.current == paging.total`

#### Scenario: Episode listing is empty
- **WHEN** provider returns zero episodes
- **THEN** the run SHALL complete without requesting episode cards

### Requirement: Paginated set card retrieval
The system SHALL fetch set card prices using paginated episode-card retrieval for each discovered set.

#### Scenario: Set has multiple card pages
- **WHEN** a set card response indicates multiple pages
- **THEN** the system SHALL request all pages for that set up to its current run budget constraints

#### Scenario: Set has a single card page
- **WHEN** a set card response contains one page
- **THEN** the system SHALL process that page and continue to the next set

### Requirement: Durable cursor checkpoint and resume
The system SHALL persist pagination cursor state so a stopped run can resume from the last durable checkpoint.

#### Scenario: Run stops on budget before finishing all sets
- **WHEN** the run reaches its effective daily operational budget mid-traversal
- **THEN** the system SHALL persist current phase/set/page cursor before exiting

#### Scenario: Next run resumes after partial traversal
- **WHEN** a subsequent run starts with a persisted unfinished cursor
- **THEN** the system SHALL continue from the persisted cursor instead of restarting from page 1 of all sets
