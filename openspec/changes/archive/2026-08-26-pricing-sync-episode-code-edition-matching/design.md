## Context

Pricing synchronization currently depends on deterministic provider-to-local mapping to update prices. The provider card payload includes `card_number` at row level and set identity inside nested `episode` metadata. In real payloads stored in test resources, the relevant set identity appears as an alphanumeric code such as `11WSP` while local catalog identity is represented numerically (edition id/set number) in database-backed entities.

Current mapping succeeds when `set_num` or equivalent numeric set fields are present, and partially succeeds when row-level edition code aligns with local edition code. It fails for payloads where only `episode.code` carries set identity, producing unresolved mappings.

## Goals / Non-Goals

**Goals:**
- Normalize episode-scoped provider metadata so deterministic mapping can derive numeric edition identity from `episode.code`.
- Define deterministic matching precedence and fallbacks when identifiers are partially missing.
- Add regression coverage using fixture-like provider rows reflecting Winterspell payload structure.

**Non-Goals:**
- Rework pricing source selection or currency logic.
- Change admin API contracts or pricing status payload schema.
- Introduce non-deterministic (name/fuzzy) matching.

## Decisions

1. Add episode code numeric extraction as first-class mapping input.
- Decision: parse leading digits from `episode.code` (for example `11WSP` -> `11`) and treat result as primary numeric set identity candidate.
- Rationale: this matches provider payload reality and local catalog convention while preserving deterministic behavior.
- Alternative rejected: rely only on `set_num` fields, which are not reliably present across payload variants.

2. Keep deterministic lookup sequence with explicit precedence.
- Decision: mapping order becomes:
  1) episode-derived numeric identity + `card_number`
  2) row or episode numeric set fields + `card_number`
  3) edition code + `card_number`
  4) external id fallback
- Rationale: maintains strict identifier-based mapping while reducing unresolved rows.
- Alternative rejected: direct fuzzy matching by name or slug.

3. Preserve compatibility with existing repository contracts.
- Decision: reuse existing repository lookups where possible and only add/adjust behavior needed for episode-derived identity.
- Rationale: minimizes implementation risk and keeps query semantics transparent.
- Alternative rejected: replacing repository contracts with broad ad hoc queries.

4. Validate with realistic fixture-derived scenarios.
- Decision: add tests that emulate rows containing `episode.code=11WSP` and `card_number` values from test fixtures.
- Rationale: captures the observed production-like mismatch and protects against regression.
- Alternative rejected: synthetic tests without episode nesting.

## Risks / Trade-offs

- [Incorrect interpretation of numeric segment meaning] -> Mitigation: explicitly document and test whether the derived numeric segment maps to local edition id or local set number in this project context.
- [Ambiguous payloads where both row set fields and episode code disagree] -> Mitigation: enforce deterministic precedence and log unresolved diagnostics when conflicts prevent mapping.
- [Overfitting to one provider code format] -> Mitigation: constrain extraction to leading numeric segment and keep fallback chain for other payload shapes.
- [Hidden regression in already-working mappings] -> Mitigation: keep existing matching paths and add regression tests for both old and new successful routes.
