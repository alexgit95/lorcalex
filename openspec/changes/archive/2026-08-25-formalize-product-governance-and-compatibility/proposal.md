## Why

Project behavior is currently defined mostly by implemented code and ad-hoc documentation, which creates ambiguity and drift risk. We need a formal governance baseline that transitions toward OpenSpec as canonical source of truth while preserving delivery momentum.

## What Changes

- Define a two-phase source-of-truth model: code-first now, OpenSpec-canonical next.
- Formalize release documentation discipline: every functional change MUST update README and changelog.
- Normalize changelog hygiene: one section per released version, no duplicates.
- Introduce dedicated API key export filter for consistent security responsibility boundaries.
- Formalize collection semantics:
  - card ownership is true when quantity > 0 OR foilQuantity > 0
  - foil consistency invariant: foil iff foilQuantity > 0
  - stats completion uses distinct owned cards.
- Keep recent endpoint limits fixed to 10/20/25/50.
- Make scanner OCR total upper bound configurable through settings.
- Formalize Companion import behavior with explicit merge/replace modes and detailed import report.
- Establish import/export compatibility policy:
  - documented N/N-1 contract
  - mandatory unit/integration compatibility tests for import/export payload evolution.
- Keep a single broad CORS policy as an explicit product decision.

## Capabilities

### New Capabilities
- `spec-governance-transition`: Defines phased source-of-truth governance from code-first to OpenSpec-canonical.
- `release-documentation-discipline`: Requires README and changelog updates for each behavior change and changelog deduplication rules.
- `collection-ownership-and-foil-invariants`: Defines owned semantics, foil invariants, and stats completion semantics.
- `import-export-compatibility-contract`: Defines N/N-1 compatibility contract and mandatory test coverage gates.
- `companion-import-modes-and-reporting`: Defines merge/replace import modes and required import reporting fields.
- `scanner-ocr-configurable-bounds`: Defines configurable OCR parsing bounds for N/TOTAL validation.
- `export-api-key-security-filter`: Defines dedicated filter-based API key validation for export endpoint.
- `operational-api-constraints`: Defines fixed recent limits and explicit broad CORS policy.

### Modified Capabilities
- None (no baseline capability specs currently exist under openspec/specs).

## Impact

- Affected code areas:
  - security filter chain and export access path
  - collection and statistics semantics
  - scanner parser configuration path
  - companion import reporting payload
  - backup/export compatibility test suite and CI gates
- Affected documentation:
  - README behavior sections
  - changelog release entries
- Affected process:
  - PR/release checklist must enforce docs updates and compatibility tests.
