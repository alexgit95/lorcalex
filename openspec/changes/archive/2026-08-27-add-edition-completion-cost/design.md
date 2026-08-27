## Context

`PricingInsightsService.getInsights()` builds `PricingEditionValuationDTO` entries by iterating **owned** `UserCollection` rows only (`collectionRepository.findAllWithCardAndEdition()`), summing `(quantity + foilQuantity) × marketPrice` per tracked edition. It has no notion of "missing" cards today — cards the user owns zero copies of never appear in that loop. `StatisticsService.buildEditionStat()` already has the pattern needed to reason about all catalog cards of an edition (`cardRepository.findByEditionOrderByCardNumberAsc(edition)`) versus what's owned, for completion percentages — but it only counts cards, it does not touch price.

`Card.rarity` stores French rarity strings (`Commune`, `Inhabituelle`, `Rare`, `Très Rare`, `Légendaire`, `Enchanté`, confirmed against real fixture data). `StatisticsService.RARITIES` already groups the first five for completion tracking, excluding `Enchanté`.

## Goals / Non-Goals

**Goals:**
- For each tracked edition in the Pricing tab, compute two completion-cost totals from missing cards (cards with zero owned copies, normal + foil): one for `Commune/Inhabituelle/Rare/Très Rare/Légendaire`, one for everything else (e.g. `Enchanté`).
- Exclude missing cards without a `marketPrice` from both totals, but surface a single count of them per edition.
- Reuse the existing tracked-edition scope (`stats_enabled_sets` via `statisticsService.resolveEnabledSetIds()`), matching the rest of the Pricing tab.

**Non-Goals:**
- No new UI section, no global (all-editions) aggregate — only enriching the existing per-edition entries.
- No change to the existing `totalValueEur` (owned collection value) computation or its formula.
- No change to `StatisticsService`'s own rarity grouping/completion logic (a separate, hardcoded constant is acceptable here, even though it mirrors `StatisticsService.RARITIES` plus `Légendaire` folded in — see Decisions).

## Decisions

- **Compute completion cost inside `PricingInsightsService.getInsights()`**, as an additional pass over `cardRepository.findByEditionOrderByCardNumberAsc(edition)` for each tracked edition, cross-referenced against a per-edition set of "owned card IDs" built from the existing collection query. Rationale: keeps all Pricing-tab computation in one service, avoids a second full DB round-trip through `StatisticsService`.
- **"Missing" means zero owned copies** (`quantity + foilQuantity == 0`, or no `UserCollection` row at all) — not "fewer than N copies". Matches the user's confirmed intent: cost to own at least one copy of every card in the set.
- **Rarity split hardcoded as two sets**: `BASE_RARITIES = {Commune, Inhabituelle, Rare, Très Rare, Légendaire}` (bucket 1) and "everything else" (bucket 2, no explicit list — any rarity value not in `BASE_RARITIES`, including `Enchanté` and any future/unknown rarity). Rationale: an open-ended second bucket avoids missing new/unlisted rarities silently falling out of both totals.
- **New DTO fields on `PricingEditionValuationDTO`**: `completionCostBaseEur` (bucket 1 total), `completionCostPremiumEur` (bucket 2 total), `missingCardsUnknownPrice` (count). Rationale: keeps the existing `totalValueEur` field and API shape intact; additive fields are backward compatible for any other consumer of this DTO.
- **Single global unknown-price counter per edition** (not split per bucket), per explicit user confirmation.
- **Frontend displays only the base tier**, labeled to make clear it is the cost of missing cards ("Coût des cartes manquantes (Courantes et Légendaire)"), rendered directly inside the existing edition row in `renderPricingPage()`'s `editionRows` block, below the existing edition name/total-value line. `completionCostPremiumEur` is still computed and returned by the API (kept for potential future use) but intentionally not rendered — per explicit user decision after reconsidering the initial two-bucket display.

## Risks / Trade-offs

- [Risk] Computing completion cost requires reading all catalog cards per tracked edition (not just owned ones), adding DB load to `getInsights()` → Mitigation: catalog sizes per edition are small (order of ~200-300 cards), and this endpoint is not on a hot path (no polling); acceptable for now.
- [Risk] The two-bucket rarity split duplicates rarity knowledge already encoded in `StatisticsService.RARITIES` (four of five values match, `Légendaire` folded into bucket 1 here vs. excluded in similar-but-different ways elsewhere) → Mitigation: accepted as a small, explicit duplication scoped to this feature; not worth a shared constant given the two call sites reason about different things (set completion % vs. cost-to-complete).
- [Trade-off] No global cross-edition total is added, so users must scan each edition row individually to gauge total completion cost across their whole collection — acceptable per explicit user preference for per-edition detail only.
- [Trade-off] `completionCostPremiumEur` is computed but unused by any UI today, which is a small amount of dead-weight computation/API surface — accepted since it's cheap to compute alongside the base tier and avoids re-deriving the split later if the premium tier display is reintroduced.
