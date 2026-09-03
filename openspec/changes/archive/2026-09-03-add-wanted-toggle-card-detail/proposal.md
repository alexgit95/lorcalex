## Why

Les utilisateurs peuvent actuellement marquer une carte comme voulue depuis la grille de collection ou la confirmation d’un scan, mais pas depuis son détail. Ils doivent donc revenir à une autre vue pour ajouter ou retirer ce statut, en particulier pour une carte déjà possédée.

## What Changes

- Ajouter au détail d’une carte un contrôle permettant de marquer ou démarquer la carte comme voulue.
- Rendre ce contrôle disponible pour les cartes possédées et non possédées.
- Afficher une action et un état accessibles qui distinguent l’ajout du retrait du statut wanted.
- Mettre à jour l’état du détail et des listes de cartes après une modification, sans changer les quantités possédées.
- Conserver la règle visuelle existante : la bordure dorée n’apparaît que pour une carte voulue et non possédée.
- Documenter le parcours dans le README et ajouter une entrée `[Unreleased]` dans le changelog.

## Capabilities

### New Capabilities

- `wanted-card-detail-toggle`: Contrôle du statut wanted depuis le détail d’une carte, quel que soit son état de possession.

### Modified Capabilities

## Impact

- Frontend statique : rendu du modal de détail et synchronisation des caches de cartes dans `src/main/resources/static/app.js`.
- Styles du contrôle dans `src/main/resources/static/app.css` si nécessaire.
- API existante `PATCH /api/cards/{id}/wanted`, sans modification de contrat backend.
- Tests frontend ou tests de parcours à ajouter selon les mécanismes de test disponibles.
- Documentation utilisateur dans `README.md` et historique des changements dans `CHANGELOG.md`.
