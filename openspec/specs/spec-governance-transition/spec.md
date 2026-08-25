# spec-governance-transition Specification

## Purpose
TBD - created by archiving change formalize-product-governance-and-compatibility. Update Purpose after archive.
## Requirements
### Requirement: Two-phase source-of-truth governance
The project MUST operate with a two-phase governance model for product truth. During Phase 1, implemented code SHALL remain operational truth while OpenSpec baselines are authored. During Phase 2, approved OpenSpec artifacts SHALL be the canonical source of truth for product behavior.

#### Scenario: Phase 1 behavior conflict
- **WHEN** an inconsistency is identified between code behavior and OpenSpec draft text during Phase 1
- **THEN** code behavior SHALL be treated as current truth
- **AND** the inconsistency SHALL be recorded and resolved by updating OpenSpec artifacts before Phase 2 completion

#### Scenario: Phase 2 behavior conflict
- **WHEN** an inconsistency is identified between code behavior and approved OpenSpec artifacts during Phase 2
- **THEN** OpenSpec SHALL be treated as canonical truth
- **AND** implementation SHALL be aligned to OpenSpec or the OpenSpec change SHALL be formally amended

