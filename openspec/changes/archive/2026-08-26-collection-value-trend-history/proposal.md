## Why

La collection a une valeur calculable aujourd'hui, mais elle n'est pas historisee. Les utilisateurs ne peuvent pas voir l'evolution de la valeur totale de leur collection dans le temps ni comparer la valeur d'une edition par rapport a 7 jours et 30 jours.

## What Changes

- Calculer et enregistrer un snapshot de la valeur totale de la collection a chaque sync pricing.
- Exposer une serie de valeurs historiques pour afficher le graphe de la valeur totale de la collection.
- Calculer pour chaque edition sa valeur actuelle, ainsi que ses deltas par rapport a 7 jours et 30 jours.
- Reutiliser la formule de valorisation existante: `(quantity + foilQuantity) x marketPrice`.
- Rester en EUR pour les valeurs de tendance et les deltas affichees.
- Alimenter le frontend avec les donnees de tendance globale et par edition.

## Capabilities

### New Capabilities
- `collection-value-trend-history`: historique de la valeur de collection et deltas par edition.

### Modified Capabilities
- `pricing-insights-tab-and-valuation`: enrichir l'API de pricing avec donnees historiques et deltas.

## Impact

- Backend pricing: ajout d'un snapshot historique apres chaque sync pricing.
- Backend API: expose la serie globale ainsi que les deltas par edition.
- Frontend: affiche le graphe global de la valeur totale et le tableau des editions avec delta 7j / 30j.
- Services de valorisation: reutilisation de la logique existante `(quantity + foilQuantity) x marketPrice`.
- Donnees: stockage des snapshots et comparaison de valeurs sur des periodes de reference.
- Tests: tests de calcul de snapshot, series historiques et deltas par edition.
