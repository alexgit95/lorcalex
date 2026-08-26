## 1. Scheduling core

- [x] 1.1 Introduce dynamic pricing scheduler service bound to pricing_schedule_cron
- [x] 1.2 Validate cron expression and implement safe fallback behavior
- [x] 1.3 Ensure scheduler reconfiguration is applied after admin setting updates

## 2. Startup catch-up

- [x] 2.1 Add persistent setting for last scheduled run date
- [x] 2.2 Implement startup catch-up trigger when current day was not processed
- [x] 2.3 Ensure catch-up uses existing sync guard against concurrent runs

## 3. Admin controls and status

- [x] 3.1 Expose/confirm admin controls for pricing schedule and enable switch
- [x] 3.2 Extend pricing status response with schedule validity and last scheduled run date

## 4. Tests and documentation

- [x] 4.1 Add unit tests for cron validation and scheduler fallback behavior
- [x] 4.2 Add integration tests for startup catch-up single-run semantics
- [x] 4.3 Add integration tests for no-catch-up when day already processed
- [x] 4.4 Update README and CHANGELOG if behavioral contract changes
