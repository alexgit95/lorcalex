## 1. Backend pricing insights API

- [x] 1.1 Add repository query support for latest priced catalog cards ordered by `lastPriceAt` descending with limit 20
- [x] 1.2 Implement pricing-insights service that computes edition valuation totals using `(quantity + foilQuantity) x marketPrice`
- [x] 1.3 Reuse tracked-edition filter semantics from Statistics (`stats_enabled_sets`) in valuation scope
- [x] 1.4 Enforce EUR-only outputs and add exclusion counters for missing price and non-EUR rows
- [x] 1.5 Expose authenticated endpoint (e.g. `/api/pricing/insights`) returning latest priced cards, edition valuations, totals, and currency

## 2. Frontend pricing tab

- [x] 2.1 Add `Prix` tab entry to SPA navigation and routing
- [x] 2.2 Add API client method for pricing insights payload retrieval
- [x] 2.3 Implement pricing tab UI section for 20 latest priced catalog cards
- [x] 2.4 Implement pricing tab UI section for value totals by tracked edition and global total in EUR
- [x] 2.5 Display explicit empty/error states and EUR formatting consistently

## 3. Quality and documentation

- [x] 3.1 Add unit tests for valuation formula, EUR-only filtering, and exclusion counters
- [x] 3.2 Add integration/API tests for insights endpoint ordering, scope filtering, and response contract
- [x] 3.3 Update README and CHANGELOG with pricing tab behavior and valuation rules
