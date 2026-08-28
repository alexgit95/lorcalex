## 1. Frontend: loop until rescan

- [x] 1.1 Change `.confetti-piece` animation from `confetti-fall linear forwards` to `confetti-fall linear infinite` in `app.css`.
- [x] 1.2 Add `stopWantedCelebration()` in `app.js`, removing the overlay and clearing the tracked reference.
- [x] 1.3 Call `stopWantedCelebration()` at the start of `celebrateWantedCardScan()` to avoid stacking overlays on repeated wanted scans.
- [x] 1.4 Call `stopWantedCelebration()` from `restartScannerCapture()` (covers manual "Recommencer" and continuous-scan auto-restart).
- [x] 1.5 Call `stopWantedCelebration()` from the scanner-page-leave cleanup in `handleRoute`.

## 2. Demo page

- [x] 2.1 Update `preview-wanted-scan-animation.html` to loop the confetti and stop it on "Recommencer", matching the real behavior.

## 3. Verification

- [x] 3.1 Manual verification: scanning a wanted card loops the confetti indefinitely; restarting the scan or leaving the Scanner page stops it; scan confirmation controls remain interactive throughout.
