## Objective

Add a collection value trend history that is produced automatically during each pricing sync. The data should support both:

- a global collection value chart over time
- per-edition value comparisons against 7-day and 30-day baselines

## Key Design Decisions

### 1. Snapshot is produced after pricing sync completes
The pricing sync already updates each card's market price and timestamp. After the successful update phase, the system will recompute the collection valuation in EUR and persist a snapshot for the current date.

### 2. Global chart uses time-series snapshots
A dedicated collection value snapshot record stores a timestamp and the total collection value in EUR. The UI renders the points chronologically to show the trend.

### 3. Edition deltas are computed from snapshot history
For each edition, the system calculates:
- current value
- value at 7 days
- value at 30 days
- delta 7d
- delta 30d

The same valuation formula is reused for all snapshots:
`(quantity + foilQuantity) x marketPrice`

### 4. Data stays EUR-only for these outputs
The trend and delta outputs remain in EUR, consistent with the pricing insights feature. Cards without a market price or with a non-EUR currency remain excluded from the valuation aggregates.

### 5. UI remains simple and readable
The front end should not over-engineer the graph. It needs:
- one main chart for total collection value
- one table or list for edition deltas with 7d and 30d values

## Data Model Sketch

### CollectionValueSnapshot
- id
- userId
- recordedAt
- totalCollectionValueEur
- currency
- source
- createdAt

### EditionValueSnapshot
- id
- snapshotId
- editionId
- editionCode
- editionName
- totalValueEur
- createdAt

## Computation Rules

- Use the tracked-edition scope already used by statistics and valuation aggregation.
- Exclude cards with no price from valuation aggregates.
- Exclude cards with non-EUR price currency from monetary aggregates.
- Store snapshots only after successful pricing syncs, not during partial failures.

## APIs

### Proposed endpoints
- GET /api/pricing/trend
- GET /api/pricing/edition-deltas

### Response shape
- global trend: ordered list of `{ recordedAt, totalCollectionValueEur }`
- edition deltas: ordered list of `{ editionId, editionCode, editionName, currentValueEur, value7dEur, value30dEur, delta7dPercent, delta30dPercent }`

## Risks and Trade-offs

- If no historical snapshots exist for 7d or 30d, deltas should be omitted or set to null rather than forcing a misleading value.
- The system must keep snapshot writing lightweight so it does not slow down pricing sync runs.
- Since the feature relies on pricing updates, it inherits the same currency and data-quality constraints as the existing pricing insights data.
