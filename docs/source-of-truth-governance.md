# Source of Truth Governance

## Phase 1: Code-first transition

During transition, implemented code remains the runtime truth while OpenSpec baselines are completed and reviewed.

## Phase 2: OpenSpec-canonical

After transition, approved OpenSpec artifacts are canonical. Any divergence between implementation and OpenSpec must be resolved by either:
- updating implementation to match OpenSpec, or
- approving an OpenSpec amendment before release.

## Change review requirements

For every behavior or operational change:
- README.md must be updated.
- CHANGELOG.md must be updated.
- Import/export contract changes must include N/N-1 compatibility tests.
