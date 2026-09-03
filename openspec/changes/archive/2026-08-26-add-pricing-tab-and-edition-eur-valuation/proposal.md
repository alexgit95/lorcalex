## Why

La synchronisation pricing existe deja, mais les informations produites restent difficiles a exploiter au quotidien. Il manque une vue produit dediee pour suivre les dernieres cartes valorisees et estimer la valeur de collection par edition suivie.

## What Changes

- Ajouter un nouvel onglet `Prix` dans l'application.
- Afficher les 20 dernieres cartes du catalogue ayant recu un prix (ordre `lastPriceAt` decroissant).
- Afficher la valeur totale de collection par edition suivie dans Stats.
- Appliquer la formule de valorisation: `(quantity + foilQuantity) x marketPrice`.
- Afficher les montants uniquement en EUR dans l'onglet Prix et les agregats de valorisation.
- Exposer un endpoint backend dedie pour servir la vue Prix (dernieres cartes valorisees + valorisation par edition).

## Capabilities

### New Capabilities
- `pricing-insights-tab-and-valuation`: Vue Prix (catalogue recemment price + valorisation collection par edition suivie en EUR).

### Modified Capabilities
- *(none)*

## Impact

- Frontend SPA: navigation et rendu d'un nouvel onglet dans `src/main/resources/static/app.js`.
- Backend API: nouvel endpoint de consultation pricing insights (JWT).
- Services statistiques/pricing: agregation des valeurs par edition avec filtre `stats_enabled_sets`.
- Repositories: requetes ciblees pour les dernieres cartes pricees et la valorisation par edition.
- Tests: unitaires/integration pour endpoint et regles de calcul en EUR.
- Documentation: README et changelog sur le nouvel onglet Prix et la regle de valorisation.
