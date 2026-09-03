## MODIFIED Requirements

### Requirement: Cardmarket price source priority order
The system SHALL determine a card's market price from a provider row by evaluating an ordered list of price source candidates and using the first candidate that is present, whose associated currency matches the configured provider currency, and — for the two average-based candidates — that is plausible relative to a reference median computed from the row's own price fields.

The ordered candidates are:
1. `prices.cardmarket.7d_average`
2. `prices.cardmarket.30d_average`
3. `prices.cardmarket.lowest_near_mint_FR`
4. `prices.cardmarket.lowest_near_mint_FR_EU_only`
5. `prices.cardmarket.lowest_near_mint`
6. `prices.tcg_player.market_price`

For candidates 1-5, the associated currency is `prices.cardmarket.currency`. For candidate 6, the associated currency is `prices.tcg_player.currency`.

The reference median is computed from all non-null, non-zero values among: the 8 regional `lowest_near_mint*` fields (`lowest_near_mint`, `lowest_near_mint_EU_only`, `lowest_near_mint_DE`, `lowest_near_mint_DE_EU_only`, `lowest_near_mint_FR`, `lowest_near_mint_FR_EU_only`, `lowest_near_mint_IT`, `lowest_near_mint_IT_EU_only`), `7d_average`, and `30d_average`.

#### Scenario: Earlier candidate in EUR takes precedence
- **WHEN** a provider row contains `prices.cardmarket.7d_average` with `prices.cardmarket.currency` equal to the configured provider currency, and no plausibility guard rejects it
- **THEN** the system SHALL use `prices.cardmarket.7d_average` as the card's market price
- **AND** SHALL NOT evaluate `30d_average`, `lowest_near_mint_FR`, `lowest_near_mint_FR_EU_only`, `lowest_near_mint`, or `tcg_player.market_price`

#### Scenario: Candidate skipped due to currency mismatch
- **WHEN** a candidate's value is present but its associated currency does not equal the configured provider currency
- **THEN** the system SHALL skip that candidate without using its value
- **AND** SHALL continue evaluating the next candidate in priority order

#### Scenario: Candidate skipped because absent
- **WHEN** a candidate field is absent from the provider row
- **THEN** the system SHALL skip that candidate
- **AND** SHALL continue evaluating the next candidate in priority order

#### Scenario: Implausible average is rejected and falls through
- **WHEN** `prices.cardmarket.7d_average` (or, if reached, `30d_average`) is present, currency-matched, a reference median is computable from at least 5 of the row's other price fields, and the candidate's value is strictly less than one fifth of that median or strictly greater than five times that median
- **THEN** the system SHALL treat that candidate as unusable and continue evaluating the next candidate in priority order
- **AND** the value exactly equal to five times the median (or one fifth of the median) SHALL be treated as plausible, not rejected

#### Scenario: Reference median requires at least 5 pooled values
- **WHEN** fewer than 5 non-null, non-zero values are available across the row's pooled price fields
- **THEN** no reference median SHALL be computed for that row
- **AND** `prices.cardmarket.7d_average` and `prices.cardmarket.30d_average` SHALL be evaluated using only the existing presence and currency checks, with no plausibility rejection

#### Scenario: No reference median available leaves averages unguarded
- **WHEN** none of the row's `lowest_near_mint*`, `7d_average`, or `30d_average` fields (excluding zero values) are present
- **THEN** no reference median can be computed
- **AND** `prices.cardmarket.7d_average` and `prices.cardmarket.30d_average` SHALL be evaluated using only the existing presence and currency checks, with no plausibility rejection

#### Scenario: Zero-valued fields do not contribute to the reference median
- **WHEN** computing the reference median for a row
- **THEN** any of the pooled fields whose value is exactly zero SHALL be excluded from the median calculation
- **AND** this exclusion SHALL NOT affect whether a zero value is itself usable as a final candidate price elsewhere in this requirement

#### Scenario: lowest_near_mint and tcg_player candidates are never plausibility-guarded
- **WHEN** evaluating `lowest_near_mint_FR`, `lowest_near_mint_FR_EU_only`, `lowest_near_mint`, or `tcg_player.market_price`
- **THEN** the system SHALL apply only the existing presence and currency checks to these candidates
- **AND** SHALL NOT reject any of them based on the reference median

#### Scenario: tcg_player.market_price used as last resort
- **WHEN** none of `prices.cardmarket.7d_average`, `30d_average`, `lowest_near_mint_FR`, `lowest_near_mint_FR_EU_only`, or `lowest_near_mint` are usable (absent, currency mismatch, or, for the two averages, rejected as implausible)
- **AND** `prices.tcg_player.market_price` is present with `prices.tcg_player.currency` equal to the configured provider currency
- **THEN** the system SHALL use `prices.tcg_player.market_price` as the card's market price

#### Scenario: No usable candidate leaves the price unresolved
- **WHEN** none of the 6 ordered candidates are usable (each absent, currency-mismatched, or — for the two averages — rejected as implausible)
- **THEN** the system SHALL NOT fall back to any other price extraction method
- **AND** the card's price SHALL be treated as unresolved for that row

#### Scenario: Zero is a legitimate final price value
- **WHEN** the first usable candidate in priority order has a value of zero
- **THEN** the system SHALL use zero as the card's market price
- **AND** SHALL NOT treat it as a missing value or continue to the next candidate
