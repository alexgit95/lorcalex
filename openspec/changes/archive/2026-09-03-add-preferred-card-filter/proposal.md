## Why

Les cartes marquées comme préférées sont actuellement repérables individuellement, mais il n'existe aucun moyen de les afficher rapidement dans la collection. Un filtre dédié permettra de retrouver les cartes préférées d'une édition, qu'elles soient déjà possédées ou encore manquantes.

## What Changes

- Ajouter un filtre **Préférées** à côté des filtres Toutes, Possédées, Manquantes et Foil dans la page Collection.
- Afficher dans ce mode toutes les cartes dont le marqueur `wanted` est actif, sans filtrer selon leur état de possession.
- Conserver le filtre compatible avec l'édition sélectionnée, la recherche par nom et les autres états de la page.
- Documenter le nouveau filtre dans le README et le CHANGELOG.

## Capabilities

### New Capabilities

- `preferred-card-filter`: Permet de filtrer la grille Collection sur les cartes préférées, possédées ou non.

### Modified Capabilities

## Impact

- Interface statique de la page Collection, principalement `src/main/resources/static/app.js`.
- Tests JavaScript ou tests de comportement de la grille si une couverture frontend existe.
- Documentation utilisateur dans `README.md` et historique des changements dans `CHANGELOG.md`.
- Aucun changement d'API, de modèle de données ou de persistance : le champ `wanted` déjà exposé par les cartes sera réutilisé.