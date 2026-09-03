## Context

`Card` already has both `imageUrl` (full resolution, ~600×840) and `thumbnailUrl` (lighter image, populated during catalog import from the Ravensburger API — see [LorcaJsonService.java](../../../src/main/java/com/alexgit95/service/LorcaJsonService.java#L274)), but `CardDTO` never exposes `thumbnailUrl`, and the Collection grid (`cardItemHTML`) always renders `imageUrl` even though cards display at ~110px wide. Separately, every `CardDTO` — whether returned by a listing endpoint (`/api/cards`, used to populate the Collection grid) or a single-card fetch — includes `bodyText` and `flavorText` (full French rules text), which the frontend never displays (confirmed: neither `cardItemHTML` nor the detail modal in `openModal` reference these fields).

## Goals / Non-Goals

**Goals:**
- Use `thumbnailUrl` for the Collection grid's card images; keep `imageUrl` for the detail modal (unchanged).
- Omit `bodyText`/`flavorText` from listing-endpoint responses (`/api/cards`, used for the grid), reducing JSON payload size and serialization work.
- Keep single-card detail responses (`/api/cards/{id}`) unchanged (still include rules text), since that's the intended "full detail" surface even though the current frontend doesn't call it yet.

**Non-Goals:**
- No new DTO class or breaking API shape change — `CardDTO` keeps the same fields; listing responses simply omit the two rules-text fields from the JSON when not populated.
- No change to the detail modal, which continues to read `imageUrl` from the same card object already in memory (`collState.cards`), not a fresh `/api/cards/{id}` fetch.
- No change to the scanner lookup endpoint (`/api/cards/lookup`) — low volume (1-3 cards per scan), not a meaningful contributor to payload size.
- No gzip/compression changes (explored and deliberately deferred).

## Decisions

- **Single `CardDTO`, conditional population instead of a parallel lightweight class**: add `@JsonInclude(JsonInclude.Include.NON_NULL)` at the class level, and add a `toDTO(Card card, UserCollection uc, boolean includeRulesText)` overload. Listing methods (`getCardsByEdition`, `getAllCardsWithCollection`, `searchCards`) call it with `includeRulesText=false` (leaving `bodyText`/`flavorText` null, which `@JsonInclude(NON_NULL)` then omits from the JSON entirely — not just nulled). `getCardById` keeps `includeRulesText=true`. This avoids introducing a second DTO type and mapping path, minimizing risk and code duplication for a single-app-wide serialization tweak.
- **`thumbnailUrl` added alongside `imageUrl` on `CardDTO`** (both present in every response, including listing) — the grid needs `thumbnailUrl`, the modal needs `imageUrl`, and both are sourced from the same in-memory card object (`collState.cards`) rather than separate fetches, so both fields must stay available together.
- **`cardItemHTML` uses `thumbnailUrl` for the Collection grid's lazy `data-src`**; falls back to `imageUrl` if `thumbnailUrl` is absent (e.g. cards imported before this field existed, or import sources without a thumbnail). The modal (`openModal`) is untouched and keeps using `imageUrl`.

## Risks / Trade-offs

- [`@JsonInclude(NON_NULL)` applies at the `CardDTO` class level] → Verify no other consumer of `CardDTO` currently depends on `bodyText`/`flavorText` being present-but-null in JSON (confirmed: frontend never reads them; no other known API consumer).
- [Some catalog cards may have no `thumbnailUrl` (older imports, provider gaps)] → Grid falls back to `imageUrl` for those specific cards; no visual regression, just no size saving for that subset.
- [Two `toDTO` call sites now differ by a boolean flag] → Low complexity; the default (rules text included) remains the safe fallback for any future consumer of `getCardById`.
