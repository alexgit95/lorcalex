## Context

Le produit dispose deja d'une synchronisation pricing qui alimente `marketPrice`, `priceCurrency` et `lastPriceAt` sur les cartes. En revanche, il n'existe pas de vue dediee pour exploiter ces donnees: l'operateur ne peut pas visualiser rapidement les dernieres cartes valorisees, ni suivre la valeur de sa collection par edition.

L'application a deja:
- un filtre des editions suivies pour Stats (`stats_enabled_sets`),
- des quantites collection `quantity` et `foilQuantity`,
- une devise cible pricing configuree en EUR.

La solution doit rester coherente avec ces invariants, sans introduire de modele foil-pricing dedie.

## Goals / Non-Goals

**Goals:**
- Ajouter un onglet `Prix` qui affiche les 20 dernieres cartes du catalogue ayant recu une valorisation.
- Afficher la valorisation de collection par edition suivie dans Stats.
- Standardiser l'affichage monetaire en EUR.
- Centraliser le calcul de valorisation cote backend pour eviter les divergences front.

**Non-Goals:**
- Ajouter un prix foil distinct.
- Ajouter une conversion multi-devises dynamique.
- Modifier la logique de budget/tentatives de la synchronisation pricing.

## Decisions

1. API backend dediee pour l'onglet Prix
- Ajouter un endpoint JWT (ex: `/api/pricing/insights`) qui retourne:
  - `latestPricedCards`: 20 cartes triees par `lastPriceAt` descendant sur tout le catalogue.
  - `editionValuations`: total par edition suivie.
  - `currency`: `EUR`.
- Rationale: limite la logique front, reduit les requetes et fixe une source de verite unique.

2. Regle de valorisation collection
- Formule unique par carte: `(quantity + foilQuantity) x marketPrice`.
- Seules les cartes avec `marketPrice` non nul et `priceCurrency == EUR` contribuent aux totaux.
- Rationale: conforme au besoin fonctionnel et robuste sans extension de modele.

3. Perimetre des editions valorisees
- Reutiliser le filtre `stats_enabled_sets` deja applique par le module Stats.
- Si aucun filtre configure, appliquer la meme semantique que Stats (tous les sets).
- Rationale: evite une divergence entre l'onglet Prix et les chiffres Stats.

4. Strategie EUR-only
- Tous les montants retournes par l'endpoint sont en EUR.
- Les cartes non-EUR sont ignorees dans les aggregats et comptees dans un indicateur d'exclusion (telemetrie de qualite).
- Rationale: respecte la contrainte produit "tout afficher en euro" sans conversion implicite.

## Risks / Trade-offs

- [Risque] Donnees pricing heterogenes (cartes sans prix ou hors EUR) reduisent la couverture des totaux.
  -> Mitigation: exposer des compteurs d'exclusion dans la reponse (`excludedNoPrice`, `excludedNonEur`).

- [Risque] Charge DB si aggregation naive sur gros catalogue.
  -> Mitigation: requetes ciblees (top 20 pricees + aggregation collection par edition) et mapping limite au payload utile.

- [Trade-off] EUR-only sans conversion peut sous-estimer temporairement la valeur si le provider retourne une autre devise.
  -> Mitigation: documenter explicitement la regle et prioriser des sources EUR.
