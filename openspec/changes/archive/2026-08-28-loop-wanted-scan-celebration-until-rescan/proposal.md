## Why

The scan celebration confetti was found to be too fast/short-lived in practice; it should keep playing continuously as a persistent visual cue until the user starts a new scan, rather than auto-stopping after a fixed short duration.

## What Changes

- The celebratory overlay animation now loops continuously instead of auto-dismissing after ~2 seconds.
- The animation stops when the user restarts the scan (e.g. clicking "Recommencer", or the scanner auto-restarting continuous scan), or when leaving the Scanner page.

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `wanted-card-scan-celebration`: the "Celebratory animation does not block scanner interaction" requirement's auto-dismiss behavior is replaced with loop-until-rescan behavior.

## Impact

- Frontend: `app.js` (`celebrateWantedCardScan`, new `stopWantedCelebration`, called from `restartScannerCapture` and the scanner-page-leave cleanup in `handleRoute`), `app.css` (`.confetti-piece` animation changed from a single run to an infinite loop).
- `preview-wanted-scan-animation.html` (static demo page) updated to match.
