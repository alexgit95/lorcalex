## Why

Card grid rendering and API payload size are both larger than necessary: the Collection grid downloads and decodes full-resolution card images (~600×840) for ~110px thumbnails, and every card-listing API response includes full rules text (`bodyText`, `flavorText`) that only the detail view needs. On a resource-constrained Raspberry Pi server and on client devices, this wastes network bandwidth, JSON serialization CPU, and image decode time for every Collection page load.

## What Changes

- Expose the existing `Card.thumbnailUrl` (already populated during catalog import, currently unused) on `CardDTO`, and use it instead of `imageUrl` for the Collection grid's card images. `imageUrl` continues to be used for the card detail/modal view.
- Introduce a lightweight card-listing representation (without `bodyText`/`flavorText`) used by the endpoints that back the Collection grid listing, reducing JSON payload size and serialization cost on the server. The detail/lookup endpoints (single-card fetch, scan lookup) continue returning full card data including rules text.

## Capabilities

### New Capabilities
- `card-grid-thumbnail-images`: Use the lighter thumbnail image for the Collection grid, reserving the full-resolution image for the detail view.
- `card-listing-lightweight-payload`: Card-listing responses exclude rules text fields not needed for grid display, while single-card detail responses remain unchanged.

### Modified Capabilities
(none — no existing spec covers card image selection or the card-listing payload shape)

## Impact

- Backend: `CardDTO` (add `thumbnailUrl`), `CardService` (`toDTO`, listing methods), possibly a new lightweight DTO/mapping for listing endpoints (`/api/cards`, `/api/cards/lookup`) vs. detail endpoints (`/api/cards/{id}`).
- Frontend: `app.js` (`cardItemHTML` uses `thumbnailUrl` for the Collection grid), no change to the detail modal (still uses `imageUrl`).
- No database schema changes (`thumbnailUrl` column already exists on `Card`).
