## Why

La collection Lorcalex ne peut pas être chargée directement dans Dreamborn.ink pour construire un deck. Un export CSV dédié doit fournir les cartes jouables dans le format attendu tout en conservant un exemplaire de chaque carte dans la collection physique.

## What Changes

- Ajouter dans l'administration une action qui télécharge l'export Dreamborn.ink au format CSV.
- Ajouter un endpoint administrateur qui produit un CSV avec les colonnes exactes `Set Number`, `Card Number`, `Variant` et `Count`.
- Permettre de choisir au lancement de l'export si un exemplaire doit être conservé en réserve ; l'option est activée par défaut et réserve prioritairement une foil, puis une normale, pour chaque carte.
- Exclure du fichier les variantes dont la quantité jouable est nulle et les cartes auxquelles il manque le numéro de set ou le numéro de carte.
- Documenter l'export Dreamborn.ink, l'option de réserve et l'endpoint d'administration dans le README et le changelog.

## Capabilities

### New Capabilities
- `dreamborn-csv-export`: Télécharger depuis l'administration un CSV Dreamborn.ink dérivé des quantités jouables de la collection.

### Modified Capabilities

- Aucun.

## Impact

- Contrôleur d'administration et nouvelle réponse HTTP CSV authentifiée.
- Interface d'administration et déclenchement du téléchargement dans le navigateur.
- Tests d'intégration du nouvel endpoint et des règles de calcul des quantités exportées.
- Documentation utilisateur dans le README et entrée de publication dans le changelog.