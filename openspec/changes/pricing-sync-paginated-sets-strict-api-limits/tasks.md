## 1. Provider Pagination Foundations

- [x] 1.1 Add provider client operations for paginated episode discovery (`/episodes?page=n`) with paging metadata extraction
- [x] 1.2 Add provider client operations for paginated episode cards (`/episodes/{id}/cards?page=n&per_page=100`)
- [x] 1.3 Implement deterministic provider-card to local-card mapping using set code + card number, with unresolved telemetry fallback

## 2. Strict Call Governance

- [x] 2.1 Introduce daily hard cap and daily safety margin settings and compute effective operational budget
- [x] 2.2 Enforce strict 30-calls-per-minute limiter for all outbound pricing provider requests
- [x] 2.3 Count every outbound request (including retries and non-2xx responses) against daily consumed calls

## 3. Cursored Multi-Day Orchestration

- [x] 3.1 Add persistent pagination cursor settings (phase, episode page, episode id, episode cards page)
- [x] 3.2 Update pricing sync orchestration to stop on operational budget and persist cursor before exit
- [x] 3.3 Resume from persisted cursor on next run and continue traversal without restarting completed pages

## 4. Refresh Prioritization Policy Update

- [x] 4.1 Implement card priority tiering: no price first, older than 7 days second, remaining cards last
- [x] 4.2 Ensure scheduled and manual runs apply identical priority and limiter guards
- [x] 4.3 Preserve existing card pricing metadata update semantics and unresolved/error status reporting

## 5. Admin Controls and Observability

- [x] 5.1 Extend admin settings support for safety margin and minute-limit controls
- [x] 5.2 Extend pricing status payload with hard cap, safety margin, effective budget, minute limiter status, cursor position, and stop reason
- [x] 5.3 Update admin UI pricing section to display new operational telemetry clearly

## 6. Testing and Reliability

- [ ] 6.1 Add unit tests for daily hard cap, safety margin enforcement, and per-minute cap enforcement
- [ ] 6.2 Add unit tests for priority ordering (no price, stale > 7 days, rest)
- [ ] 6.3 Add integration tests for partial-run cursor persistence and next-run resume behavior
- [ ] 6.4 Add provider error and retry tests ensuring strict outbound call accounting

## 7. Documentation and Release Discipline

- [x] 7.1 Update README pricing synchronization section for paginated set sync and strict limit behavior
- [x] 7.2 Update CHANGELOG with behavior shift from per-card lookup to paginated set retrieval and dual-limit guardrails
- [x] 7.3 Validate OpenSpec artifacts and ensure change is apply-ready
