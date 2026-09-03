## Context

The confetti overlay (`celebrateWantedCardScan` in `app.js`, `.confetti-piece`/`.confetti-overlay` in `app.css`) previously auto-removed itself via `setTimeout(() => overlay.remove(), 2000)`, with the CSS animation set to `linear forwards` (single run). This felt too brief in practice.

## Goals / Non-Goals

**Goals:**
- The animation loops continuously for as long as the current wanted-card scan result is displayed.
- The animation stops when the user starts a new scan (restart button, or continuous-scan auto-restart) or navigates away from the Scanner page.

**Non-Goals:**
- No change to when the animation triggers (still single-match + wanted, per the existing "Scanning a single wanted card triggers a celebratory animation" requirement — unchanged).
- No change to non-blocking behavior — scan controls must remain interactive while looping, same as before.

## Decisions

- **CSS animation changed from `linear forwards` to `linear infinite`** on `.confetti-piece`, so each particle keeps falling/resetting indefinitely instead of stopping after one pass.
- **Explicit `stopWantedCelebration()` function** removes the overlay; called from `restartScannerCapture()` (covers both the manual "Recommencer" button and the continuous-scan loop's restart) and from the scanner-page-leave cleanup already present in `handleRoute`.
- **`celebrateWantedCardScan()` calls `stopWantedCelebration()` first** before creating a new overlay, so re-triggering on a newly scanned wanted card cleanly replaces any still-looping overlay instead of stacking multiple overlays.

## Risks / Trade-offs

- [An indefinitely-looping animation left running if a code path fails to call `stopWantedCelebration()`] → Mitigated by hooking into both existing reset points (`restartScannerCapture`, page-leave cleanup in `handleRoute`), the same points already relied on for camera/scan-state cleanup.
