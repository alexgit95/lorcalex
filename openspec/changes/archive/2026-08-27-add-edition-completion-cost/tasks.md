## 1. Backend computation

- [x] 1.1 Add `completionCostBaseEur`, `completionCostPremiumEur`, and `missingCardsUnknownPrice` fields to `PricingEditionValuationDTO`
- [x] 1.2 In `PricingInsightsService.getInsights()`, for each tracked edition, determine owned card IDs (quantity + foilQuantity > 0) and iterate the edition's catalog cards to find missing ones
- [x] 1.3 For each missing card with a known `marketPrice`, add it to `completionCostBaseEur` (rarity in Commune/Inhabituelle/Rare/Très Rare/Légendaire) or `completionCostPremiumEur` (any other rarity)
- [x] 1.4 For each missing card without a `marketPrice`, increment `missingCardsUnknownPrice` instead of contributing to either total
- [x] 1.5 Round both cost totals to 2 decimal places, consistent with existing `totalValueEur` handling

## 2. Frontend display

- [x] 2.1 In `renderPricingPage()`'s edition row template, add a "Cartes Courantes et Légendaire" cost line below the existing edition name/total-value line (do not display the premium-tier cost)
- [x] 2.2 When `missingCardsUnknownPrice > 0` for an edition, display an indication that the cost is understated (e.g. "prix inconnu pour N cartes, coût minoré")

## 3. Verification

- [x] 3.1 Add/update a test covering: owned cards excluded, missing cards split correctly by rarity tier, missing cards without price excluded but counted, and tracked-edition scope respected
- [ ] 3.2 Manually verify the Pricing tab displays only the "Cartes Courantes et Légendaire" cost line per tracked edition (no premium-tier line)

## 4. Documentation

- [x] 4.1 Update README with the new per-edition completion-cost figures
- [x] 4.2 Add a CHANGELOG entry describing this addition
