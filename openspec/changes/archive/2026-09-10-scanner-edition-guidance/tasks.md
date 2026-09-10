## 1. Scanner edition selector

- [x] 1.1 Load catalog editions for the Scanner and render a select control with `Toutes les editions` as the initial option.
- [x] 1.2 Keep the selected edition in Scanner-local state and reset it to the global option whenever the Scanner is rendered again.
- [x] 1.3 Disable the manual set number input while a specific edition is selected, and re-enable it when global scope is restored.

## 2. Strict card resolution

- [x] 2.1 Pass the selected edition identifier to OCR card lookup when a specific edition is active.
- [x] 2.2 Pass the selected edition identifier to manual card lookup when a specific edition is active.
- [x] 2.3 Preserve the existing set-number candidate narrowing only while the Scanner uses the global edition scope.
- [x] 2.4 Show an edition-scoped not-found result without retrying against other editions.

## 3. Verification

- [x] 3.1 Verify global Scanner lookup still returns and disambiguates cards across all editions.
- [x] 3.2 Verify OCR and manual lookup return no out-of-edition result when a specific edition is selected.
- [x] 3.3 Run the relevant automated test suite or focused validation available in the project.