# pricing-refresh-prioritization Specification

## Purpose
TBD - created by archiving change add-card-pricing-sync-with-daily-attempt-quota. Update Purpose after archive.
## Requirements
### Requirement: Missing-value first prioritization
The pricing scheduler SHALL prioritize cards without value before any card with existing value metadata.

#### Scenario: Queue build with mixed cards
- **WHEN** the scheduler evaluates candidate cards for update
- **THEN** cards with missing pricing metadata SHALL be handled before cards that already have pricing metadata

### Requirement: Oldest-refresh next prioritization
After missing-value cards are prioritized, the scheduler SHALL prioritize cards with last price timestamp older than seven days before remaining recently priced cards.

#### Scenario: Queue build with stale and recent priced cards
- **WHEN** cards with existing pricing metadata are evaluated
- **THEN** cards with `lastPriceAt` older than seven days SHALL be prioritized ahead of cards priced within the last seven days

#### Scenario: Queue build with only recent priced cards
- **WHEN** no missing-value cards and no stale-over-seven-days cards remain
- **THEN** the scheduler SHALL process remaining cards as the final priority tier

