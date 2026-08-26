## Why

L'onglet Prix montre aujourd'hui les dernières mises à jour du catalogue et la valeur globale de la collection, sans permettre d'identifier rapidement les cartes possédées les plus chères. Les utilisateurs doivent aussi pouvoir retirer une valorisation devenue indésirable sans modifier leur inventaire.

## What Changes

- Ajouter dans l'onglet Prix un classement des cartes possédées par prix unitaire EUR décroissant, avec des limites d'affichage de 20, 50 ou 100 cartes.
- Afficher les quantités normale et foil à titre informatif dans chaque ligne classée, sans les utiliser dans l'ordre de classement.
- Déplacer la section des 20 dernières cartes du catalogue valorisées en bas de l'onglet Prix.
- Ajouter une action confirmée permettant de supprimer uniquement le prix d'une carte depuis l'onglet Prix, sans modifier les quantités possédées.
- Exclure du classement et de la valorisation les cartes dont le prix a été supprimé, jusqu'à une prochaine synchronisation EUR.

## Capabilities

### New Capabilities
- `owned-card-price-ranking`: Classe les cartes possédées par prix unitaire EUR et permet d'en choisir la taille d'affichage.
- `owned-card-price-removal`: Permet de supprimer le prix d'une carte possédée sans modifier son inventaire.

### Modified Capabilities
- `pricing-insights-tab-and-valuation`: Réorganise le contenu de l'onglet Prix et étend les données d'insights avec le classement des cartes possédées.

## Impact

- API `GET /api/pricing/insights` et ses DTOs de réponse.
- API dédiée et protégée pour supprimer une valorisation de carte.
- `PricingInsightsService`, couche de persistance des cartes et tests associés.
- Interface statique de l'onglet Prix, notamment la fiche de carte et la section des dernières valorisations.
- README pour documenter le top de collection et la suppression de prix sans effet sur les quantités.
- CHANGELOG pour consigner cette évolution fonctionnelle dans la prochaine version.