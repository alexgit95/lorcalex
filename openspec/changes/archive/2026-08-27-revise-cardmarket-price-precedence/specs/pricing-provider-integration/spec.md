## MODIFIED Requirements

### Requirement: Cardmarket price source priority order
The system SHALL determine a card's market price from a provider row by evaluating an ordered list of price source candidates and using the first candidate that is present and whose associated currency matches the configured provider currency.

The ordered candidates are:
1. `prices.cardmarket.7d_average`
2. `prices.cardmarket.30d_average`
3. `prices.cardmarket.lowest_near_mint_FR`
4. `prices.cardmarket.lowest_near_mint_FR_EU_only`
5. `prices.cardmarket.lowest_near_mint`
6. `prices.tcg_player.market_price`

For candidates 1-5, the associated currency is `prices.cardmarket.currency`. For candidate 6, the associated currency is `prices.tcg_player.currency`.

#### Scenario: Earlier candidate in EUR takes precedence
- **WHEN** a provider row contains `prices.cardmarket.7d_average` with `prices.cardmarket.currency` equal to the configured provider currency
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

#### Scenario: tcg_player.market_price used as last resort
- **WHEN** none of `prices.cardmarket.7d_average`, `30d_average`, `lowest_near_mint_FR`, `lowest_near_mint_FR_EU_only`, or `lowest_near_mint` are usable (absent or currency mismatch)
- **AND** `prices.tcg_player.market_price` is present with `prices.tcg_player.currency` equal to the configured provider currency
- **THEN** the system SHALL use `prices.tcg_player.market_price` as the card's market price

#### Scenario: No usable candidate leaves the price unresolved
- **WHEN** none of the 6 ordered candidates are usable (each absent, or present with a currency not matching the configured provider currency)
- **THEN** the system SHALL NOT fall back to any other price extraction method
- **AND** the card's price SHALL be treated as unresolved for that row

#### Scenario: Zero is a legitimate price value
- **WHEN** the first usable candidate in priority order has a value of zero
- **THEN** the system SHALL use zero as the card's market price
- **AND** SHALL NOT treat it as a missing value or continue to the next candidate

## REMOVED Requirements

### Requirement: Generic price field discovery recognizes marketplace container keys regardless of separators
**Reason**: The generic key-scan fallback (matching loosely-named keys such as `price`, `value`, `avg`, `average`, `low`, `high` anywhere in the payload, including within container keys like `tcg_player`/`cardmarket`) has produced inaccurate prices in practice and is replaced by the strict ordered candidate list in the "Cardmarket price source priority order" requirement, which has no further fallback.
**Migration**: Provider rows that do not expose any of the 6 named ordered candidates (in the configured provider currency) are now treated as unresolved (`UNRESOLVED_PRICE`) instead of having a price guessed from an arbitrary matching key.
