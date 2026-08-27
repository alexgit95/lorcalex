## Why

The Pricing tab shows the current value of tracked editions, but gives no visibility into how much it would cost to *finish* an edition. Users want to see, per tracked edition, the cost of acquiring the cards they're still missing — split by rarity tier, since "chase" rarities (Enchanté and similar) are typically far more expensive than the rest.

## What Changes

- For each tracked edition shown in the Pricing tab's "Valeur par édition suivie" section, add a completion-cost figure computed from currently missing cards (cards the user owns zero copies of, normal or foil):
  - **Coût des cartes manquantes (Courantes et Légendaire)**: sum of `marketPrice` for missing cards with rarity in `Commune, Inhabituelle, Rare, Très Rare, Légendaire`. The label makes explicit that this is the cost of missing cards, not the edition's full value. This is the only completion-cost figure displayed in the UI.
  - The API also computes a second, premium-tier total (missing cards with any other rarity, e.g. `Enchanté`) for future use, but it is not shown in this iteration of the Pricing tab.
- Missing cards without a stored `marketPrice` are excluded from both totals; the edition row shows a single count of such cards (e.g. "prix inconnu pour X cartes, coût minoré").
- Scope matches the existing tracked-edition filter (`stats_enabled_sets`), consistent with the rest of the Pricing tab.
- No new UI section or global aggregate is added — the existing per-edition rows are enriched in place.

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `pricing-insights-tab-and-valuation`: adds completion-cost figures (two rarity-tier totals plus an unknown-price counter) to each tracked edition's valuation entry; only the base tier is displayed in the UI.

## Impact

- Backend: `PricingInsightsService` (edition valuation computation), likely a new/extended DTO field on the edition valuation entry.
- Frontend: `app.js` `renderPricingPage()` edition row rendering (`editionRows` in the "Valeur par édition suivie" section).
- No database schema changes; reuses existing `Card.rarity`, `Card.marketPrice`, and `UserCollection` quantities.
