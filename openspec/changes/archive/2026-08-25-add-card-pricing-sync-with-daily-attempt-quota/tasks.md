## 1. Data model and persistence

- [x] 1.1 Extend card storage with pricing metadata fields (value, currency, source, last update timestamp, status)
- [x] 1.2 Add repository queries for cards without pricing and cards sorted by oldest pricing timestamp
- [x] 1.3 Add settings keys for pricing sync enablement, daily budget, usage date, used attempts, and scheduler config

## 2. Provider integration and mapping

- [x] 2.1 Implement provider client for RapidAPI Lorcana Prices API with configurable credentials
- [x] 2.2 Implement deterministic card mapping strategy from local card identifiers to provider lookup
- [x] 2.3 Persist provider success and unresolved/failure statuses per attempted card update

## 3. Budget enforcement and accounting

- [x] 3.1 Implement strict local daily budget guard checked before every outbound request
- [x] 3.2 Count each outbound attempt as one consumed unit regardless of response outcome
- [x] 3.3 Persist counter updates immediately so restart cannot bypass daily limits
- [x] 3.4 Implement daily rollover reset based on persisted usage date

## 4. Refresh prioritization and background orchestration

- [x] 4.1 Build processing queue prioritizing cards with missing price metadata first
- [x] 4.2 Process remaining cards by ascending last update timestamp (oldest first)
- [x] 4.3 Implement daily scheduled pricing run that stops on budget exhaustion or queue depletion
- [x] 4.4 Add optional manual admin trigger that reuses identical budget and prioritization guards

## 5. Admin controls and observability

- [x] 5.1 Expose admin settings endpoints/UI for pricing budget and sync controls
- [x] 5.2 Expose pricing status endpoint/UI (used budget, remaining, processed, errors)
- [x] 5.3 Add progress/error telemetry for provider failures including 429 as telemetry only

## 6. Compatibility, tests, and documentation

- [x] 6.1 Extend backup/export contract for pricing metadata with N/N-1 compatibility behavior
- [x] 6.2 Add/refresh N and N-1 fixtures including missing-pricing backward compatibility case
- [x] 6.2a Ensure import/export round-trip preserves card pricing value, currency, source, and last pricing scan timestamp without loss
- [x] 6.3 Add unit tests for budget accounting semantics (attempt-based counting)
- [x] 6.4 Add integration tests for queue prioritization, stop-on-budget, rollover, and restart safety
- [x] 6.5 Update README pricing section with quota, prioritization, and scheduling behavior
- [x] 6.6 Update CHANGELOG with pricing feature and compatibility impacts
