## 1. Presentation of Price Metadata

- [x] 1.1 Add a reusable frontend rendering fragment for an available market price and last price update timestamp, using the existing monetary and date formatting helpers.
- [x] 1.2 Render that fragment in the shared card detail modal used by the collection, recent scans, and Pricing tab.
- [x] 1.3 Render that fragment in the scanner card confirmation view, including the path where the user chooses one card from multiple matches.

## 2. Verification

- [x] 2.1 Verify that a selected card with price and timestamp shows both values in the shared detail modal and scanner confirmation.
- [x] 2.2 Verify that cards with either one or neither price metadata value show only available information and no misleading placeholder.
- [x] 2.3 Run the relevant frontend or application test suite and confirm that selecting a card does not initiate a pricing refresh.