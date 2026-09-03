## Context

Lorcalex currently relies on implemented behavior as practical source of truth, with partial narrative documentation and no canonical OpenSpec capability set yet. Recent feature growth (foil model, scanner rules, API export, companion import, backup/restore) introduced cross-cutting decisions that now require formal contracts to prevent drift.

The selected policy set intentionally mixes immediate governance choices and implementation constraints:
- keep current runtime behavior where explicitly validated by product decisions
- formalize rules as testable requirements
- enforce release documentation hygiene as a hard process requirement.

## Goals / Non-Goals

**Goals:**
- Establish OpenSpec artifacts that encode validated product decisions.
- Define explicit invariants and contracts for ownership, foil consistency, import/export compatibility, scanner bounds, and security filtering.
- Make import/export compatibility checks mandatory via automated tests.
- Make README and changelog updates mandatory for each functional change.

**Non-Goals:**
- Immediate full implementation of every change in this proposal.
- CORS hardening beyond the explicitly chosen broad policy.
- Changing fixed recent limits away from 10/20/25/50.

## Decisions

1. Source-of-truth governance
- Decision: Two-phase governance.
  - Phase 1: code remains runtime truth while OpenSpec baselines are created.
  - Phase 2: OpenSpec is canonical; code and docs must align with approved specs.
- Rationale: avoids delivery freeze while converging to spec-first governance.
- Alternative considered: immediate OpenSpec-only governance; rejected due to migration risk and current absence of baseline specs.

2. Documentation discipline
- Decision: every behavior change MUST update README and changelog entry; changelog must remain deduplicated by version.
- Rationale: preserves operational clarity for users and maintainers.
- Alternative considered: optional documentation updates; rejected as source of drift.

3. Export security architecture
- Decision: API key validation for export endpoint SHALL be handled by a dedicated filter in the security chain.
- Rationale: centralizes auth concerns and improves consistency with existing filter-based security model.
- Alternative considered: controller-only validation; rejected for weaker separation of concerns.

4. Foil and ownership semantics
- Decision:
  - owned(card) = quantity > 0 OR foilQuantity > 0
  - foil flag invariant: foil == (foilQuantity > 0)
  - stats completion uses distinct owned cards, not copy volume.
- Rationale: aligns with product intent and avoids contradictory states.
- Alternative considered: separate semantic role for foil flag; rejected because it allows incoherent data.

5. Scanner bounds strategy
- Decision: OCR total upper bound SHALL be configurable via settings with documented fallback.
- Rationale: future-proofs parsing against larger set sizes.
- Alternative considered: static constant; rejected due to repeated release friction.

6. Companion import behavior
- Decision: keep explicit merge and replace modes, and require detailed import report fields.
- Rationale: operational transparency and predictable reconciliation.
- Alternative considered: merge-only behavior; rejected for limited recovery workflows.

7. Compatibility policy
- Decision: import/export schema evolution SHALL honor N/N-1 compatibility and SHALL be guarded by mandatory automated tests.
- Rationale: migration safety and predictable backup/restore behavior.
- Alternative considered: best-effort compatibility; rejected due to restore risk.

8. Operational constraints
- Decision: keep recent limits fixed to 10/20/25/50 and keep single broad CORS policy as explicit product choice.
- Rationale: respects validated product direction.
- Alternative considered: dynamic limits and stricter environment-scoped CORS; postponed by product decision.

## Risks / Trade-offs

- [Governance transition ambiguity] During phase 1, temporary divergence may persist between code and new specs.
  -> Mitigation: explicit transition checkpoints and review gate requiring spec update on behavior changes.

- [Filter migration regression] Moving export API key checks to a filter can break access paths if chain ordering is wrong.
  -> Mitigation: integration tests for valid key, missing key, invalid key, expired key, and lastUsedAt updates.

- [Data consistency debt] Existing rows may violate foil invariant.
  -> Mitigation: add migration/repair routine and invariant-focused tests before enforcing strict checks.

- [Documentation overhead] Mandatory README/changelog updates increase PR effort.
  -> Mitigation: PR template checklist and lightweight section update guidelines.

- [Compatibility burden] N/N-1 test matrix adds maintenance cost.
  -> Mitigation: maintain fixture versions and run focused compatibility suite in CI.
