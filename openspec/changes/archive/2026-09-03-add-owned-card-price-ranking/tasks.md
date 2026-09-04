## 1. Pricing insights contract

- [x] 1.1 Extend `PricingInsightsDTO` with the owned-card price-ranking list.
- [x] 1.2 Build the ranking in `PricingInsightsService` from owned collection rows using the existing tracked-edition and EUR-price rules.
- [x] 1.3 Sort the ranking by unit `marketPrice` descending and card identifier ascending, and cap the API response at 100 cards.
- [x] 1.4 Preserve collection quantities in ranked `CardDTO` entries and keep the existing latest-priced catalogue list behavior intact.

## 2. Price removal API

- [x] 2.1 Add an authenticated pricing endpoint that removes current pricing data for a specified owned card.
- [x] 2.2 Implement the pricing-data reset on `Card` without calling or persisting `UserCollection`.
- [x] 2.3 Return appropriate errors for an unknown or unowned card and leave all collection quantities unchanged on every failure path.

## 3. Pricing tab experience

- [x] 3.1 Render the owned-card unit-price ranking near the top of the Pricing tab with price and normal/foil quantities.
- [x] 3.2 Add the 20, 50, and 100 segmented display control and apply it to the API ranking response.
- [x] 3.3 Move the existing latest-priced catalogue section to the bottom of the Pricing tab without changing its 20-card limit.
- [x] 3.4 Add a confirmed "Supprimer le prix" action to the Pricing card detail, call the removal endpoint, and refresh the displayed insights after success.

## 4. Verification

- [x] 4.1 Add service tests for ranking eligibility, ordering, tie-breaking, and the 100-card cap.
- [x] 4.2 Add controller or integration tests for the ranking response and for price removal preserving normal and foil quantities.
- [x] 4.3 Run the focused pricing tests and the Maven test suite.

## 5. Documentation

- [x] 5.1 Update the README Pricing documentation with the owned-card unit-price ranking, its 20/50/100 display options, and the price-removal behavior that preserves collection quantities.
- [x] 5.2 Add a CHANGELOG entry for the owned-card price ranking, reordered Pricing tab, and price-removal action.