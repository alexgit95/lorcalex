## Context

`CollectionValueTrendService.persistSnapshotFromCurrentCollection()` already computes the total collection value and per-edition totals from cards currently stored in the database (no external HTTP calls) and writes a `CollectionValueSnapshot` + `EditionValueSnapshot` row. Today it is only invoked at the end of `PricingSyncService.runSync(...)`, which is gated behind the admin-triggered, budget-limited pricing sync. There is no way to refresh the trend/edition-delta history on demand without running a full sync.

`PricingController` already exposes `/api/pricing/insights`, `/api/pricing/trend`, `/api/pricing/edition-deltas` under standard session authentication (no role check beyond being logged in).

## Goals / Non-Goals

**Goals:**
- Let a logged-in user force a fresh collection-value snapshot (total + per edition) from the Pricing tab, using only prices already stored locally.
- Reflect the new snapshot immediately in the tab's stats, trend chart, and edition-delta table.
- Give clear feedback: loading state while in flight, toast on success, error + root cause on failure.

**Non-Goals:**
- Does not trigger a pricing provider sync or consume API budget.
- Does not introduce a new authorization role; reuses existing authentication.
- Does not change how snapshots are stored, queried, or how deltas (7d/30d) are computed.
- Does not add rate limiting/anti-spam beyond disabling the button while a request is in flight.

## Decisions

- **Reuse `persistSnapshotFromCurrentCollection()` directly** from a new controller endpoint rather than calling `PricingSyncService.runSync(...)`. Rationale: the user only wants to recompute values from already-stored prices, not re-fetch prices from the provider; reusing the existing method avoids duplicating snapshot logic and keeps the two concerns (price fetching vs. value snapshotting) separate.
- **New endpoint `POST /api/pricing/recompute-value` on `PricingController`**, not `AdminController`. Rationale: this is a user-facing Pricing tab action available to any authenticated user, not an admin-only operation; placing it alongside `/api/pricing/insights` etc. matches existing controller responsibility boundaries.
- **No new role/permission check.** The app currently has no endpoint-level role enforcement besides `/api/export`, and all provisioned users are `ROLE_ADMIN` by default. Adding a role gate here would be inconsistent with the rest of the Pricing tab and add complexity with no current benefit.
- **Client-side button disabling during the request** instead of server-side throttling. Rationale: the operation is cheap (local DB read + one snapshot write), so the main risk is accidental double-submission from a user, not abuse; disabling the button is sufficient.
- **Response reuses existing DTOs**: the endpoint returns the same shape as `/api/pricing/insights` (or a minimal ack) so the frontend can decide to re-fetch `insights`, `trend`, and `edition-deltas` afterward rather than trying to merge a partial response.
- **Error handling surfaces exception message and root cause** to the frontend (e.g., `{ "message": ..., "rootCause": ... }`) so the toast/error UI can show both, matching the user's request for full error visibility instead of a generic failure message.

## Risks / Trade-offs

- [Risk] Frequent manual recomputation could create many near-duplicate snapshots in a short time window, slightly noising the trend chart → Mitigation: acceptable given low expected click frequency and the button-disable-while-loading guard; no server-side debounce is added in this iteration since it's out of scope per user decision.
- [Risk] Exposing raw exception messages/root cause to the client could leak internal details → Mitigation: this already matches the pattern used elsewhere in this app's admin/pricing error responses (internal single/trusted-user tool), and the message only reflects the pricing computation failure, not sensitive data.
- [Trade-off] Not gating behind a role means any authenticated user can trigger recomputation; acceptable since the app currently has no multi-tenant/non-admin user model in practice.

## Open Questions

None — scope, placement, auth, and error/success feedback were confirmed during exploration.
