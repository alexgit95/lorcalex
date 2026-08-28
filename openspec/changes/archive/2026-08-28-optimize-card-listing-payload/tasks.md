## 1. Backend: lightweight listing payload

- [x] 1.1 Add `@JsonInclude(JsonInclude.Include.NON_NULL)` at the class level on `CardDTO`.
- [x] 1.2 Add a `thumbnailUrl` field to `CardDTO`.
- [x] 1.3 Add a `toDTO(Card card, UserCollection uc, boolean includeRulesText)` overload in `CardService`; when `includeRulesText` is false, leave `bodyText`/`flavorText` unset (null) and always set `thumbnailUrl`. Keep the existing `toDTO(Card, UserCollection)` as a convenience that defaults to `includeRulesText=true`.
- [x] 1.4 Update `getCardsByEdition`, `getAllCardsWithCollection`, and `searchCards` in `CardService` to call the new overload with `includeRulesText=false`.
- [x] 1.5 Keep `getCardById` using `includeRulesText=true` (full detail, unchanged behavior).

## 2. Backend: tests

- [x] 2.1 Unit test: listing methods (`getCardsByEdition`, `getAllCardsWithCollection`, `searchCards`) produce a `CardDTO` with `bodyText`/`flavorText` null and `thumbnailUrl` populated.
- [x] 2.2 Unit test: `getCardById` produces a `CardDTO` with `bodyText`/`flavorText` populated as before.
- [x] 2.3 Integration test: `GET /api/cards` response JSON does not contain `bodyText`/`flavorText` keys for any card; `GET /api/cards/{id}` response JSON does contain them.

## 3. Frontend: use thumbnail image in the Collection grid

- [x] 3.1 Update `cardItemHTML` to use `card.thumbnailUrl` (falling back to `card.imageUrl` if absent) for the grid's lazy `data-src`.
- [x] 3.2 Manual verification: Collection grid images still load correctly (via the existing `IntersectionObserver` lazy loading) using the thumbnail source; the detail modal (`openModal`) still shows the full-resolution `imageUrl` unaffected.

## 4. Documentation

- [x] 4.1 Update `CHANGELOG.md` under `[Unreleased]` (Changed) describing the lighter Collection grid images and reduced listing payload size.
