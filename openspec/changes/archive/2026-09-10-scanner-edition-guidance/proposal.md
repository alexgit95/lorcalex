## Why

Le numéro de carte reconnu par le scanner peut appartenir à plusieurs éditions. Les utilisateurs qui scannent une série précise doivent pouvoir guider la résolution afin d'éviter une liste de résultats ambiguë ou l'ajout d'une carte de la mauvaise édition.

## What Changes

- Ajouter au Scanner un sélecteur d'édition alimenté par le catalogue disponible.
- Prévoir l'option par défaut `Toutes les éditions`, qui préserve la résolution actuelle sur l'ensemble du catalogue.
- Appliquer strictement l'édition choisie aux recherches issues de l'OCR et à la saisie manuelle, sans repli automatique vers les autres éditions.
- Désactiver le champ de numéro de set de la saisie manuelle lorsqu'une édition précise fixe déjà ce contexte.

## Capabilities

### New Capabilities
- `scanner-edition-guidance`: Permet de limiter strictement la résolution des cartes du Scanner à une édition choisie, tout en conservant une recherche globale par défaut.

### Modified Capabilities

- None.

## Impact

- Interface Scanner et son état côté client dans `src/main/resources/static/app.js`.
- Réutilisation des endpoints existants `/api/editions` et `/api/cards/lookup`; aucun nouvel endpoint n'est requis.
- Tests front-end ciblés ou tests de contrat API, selon les mécanismes de test disponibles dans le projet.