## 1. Governance and documentation baseline

- [x] 1.1 Define and publish the two-phase source-of-truth process (code-first transition, then OpenSpec-canonical)
- [x] 1.2 Add repository contribution checklist requiring README and changelog updates for every behavior change
- [x] 1.3 Deduplicate existing changelog so each released version has a single authoritative section
- [x] 1.4 Add verification step in review workflow to fail when behavior changes do not include README or changelog updates

## 2. Security and operational constraints

- [x] 2.1 Implement dedicated API key export security filter in the security chain
- [x] 2.2 Add integration tests for export filter outcomes: valid key, missing key, invalid key, expired key, and last-used update
- [x] 2.3 Keep recent endpoint limits fixed to 10/20/25/50 and verify fallback behavior for invalid values
- [x] 2.4 Keep single broad CORS policy and document it as explicit operational decision in technical docs

## 3. Collection semantics and invariants

- [x] 3.1 Enforce owned semantics as quantity > 0 OR foilQuantity > 0 across service and API layers
- [x] 3.2 Enforce foil invariant as foil == (foilQuantity > 0) on write and read paths
- [x] 3.3 Add data-repair migration for pre-existing inconsistent foil rows if any are detected
- [x] 3.4 Update statistics logic/tests to ensure completion counts distinct owned cards only

## 4. Scanner and import behavior formalization

- [x] 4.1 Introduce configurable OCR TOTAL upper bound setting with documented fallback default
- [x] 4.2 Add tests for OCR parser behavior with configured, out-of-range, and fallback bound values
- [x] 4.3 Ensure companion import merge and replace modes are explicit in API and UI flows
- [x] 4.4 Add structured import report fields for imported count, skipped count, and categorized skip causes

## 5. Import/export compatibility contract

- [x] 5.1 Document import/export compatibility policy as N/N-1 contract in project documentation
- [x] 5.2 Create versioned payload fixtures for N and N-1 backup/export samples
- [x] 5.3 Add mandatory unit and integration compatibility tests for import/export evolution using N/N-1 fixtures
- [x] 5.4 Configure CI gate to fail when compatibility tests are missing or failing

## 6. Release documentation and validation

- [x] 6.1 Update README sections for all implemented behavior changes in this change set
- [x] 6.2 Update changelog with one coherent release entry describing all relevant behavior and compatibility impacts
- [x] 6.3 Run full test suite including compatibility and security integration tests before apply/merge
- [x] 6.4 Run openspec validation and confirm the change is ready for implementation tracking
