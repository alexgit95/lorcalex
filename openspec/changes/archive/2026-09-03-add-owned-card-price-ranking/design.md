## Context

L'endpoint `GET /api/pricing/insights` calcule déjà la valorisation des cartes de collection à partir de `UserCollection`, en utilisant le prix EUR des cartes et le périmètre d'éditions suivi par Statistics. Son interface affiche les dernières cartes tarifées du catalogue avant les sections de valorisation. `CardDTO` transporte déjà les données de prix, d'édition et de possession nécessaires à un classement de cartes détenues.

La suppression demandée concerne exclusivement les données de prix d'une `Card`. L'entité `UserCollection`, qui contient `quantity` et `foilQuantity`, ne doit être ni lue pour modification ni écrite par cette action.

## Goals / Non-Goals

**Goals:**
- Exposer, dans les pricing insights, jusqu'aux 100 cartes possédées et tarifées en EUR, triées par prix unitaire décroissant.
- Permettre à l'interface de limiter localement l'affichage aux 20, 50 ou 100 premières cartes.
- Maintenir le périmètre d'éditions et les règles d'exclusion de la valorisation existante.
- Permettre la suppression confirmée des données de prix d'une carte possédée sans modifier son inventaire.
- Placer les dernières cartes du catalogue valorisées à la fin de l'onglet Prix.

**Non-Goals:**
- Classer les cartes selon la valeur totale détenue, les quantités ou la valeur foil.
- Ajouter une saisie manuelle de prix, un historique d'effacement ou une règle d'exclusion permanente des synchronisations.
- Modifier les quantités, la propriété ou les données d'audit de collection.

## Decisions

### Calculer le top dans `PricingInsightsService`

Le service parcourt déjà les lignes `UserCollection` avec les cartes et éditions chargées. Il produira une liste dédiée de `CardDTO` en appelant `CardService.toDTO(card, userCollection)`, après filtrage sur possession positive, périmètre suivi et prix EUR. La liste sera triée par `marketPrice` décroissant, puis par identifiant de carte croissant pour garantir un ordre stable, et limitée à 100 éléments.

L'alternative consistant à trier dans le navigateur imposerait d'exposer toute la collection valorisée et rendrait le contrat d'API moins borné. Une requête de dépôt dédiée est inutile à ce stade car le parcours chargé existe déjà et la limite maximale est faible.

### Utiliser le même contrat de devise et de périmètre que la valorisation

Le classement réutilisera `resolveEnabledSetIds()`, la vérification EUR et le traitement de possession positive existants. Ainsi, une carte sans prix, non-EUR, hors périmètre ou dont le prix vient d'être supprimé ne figure ni dans le top ni dans les agrégats de valorisation.

### Ajouter une suppression de prix séparée de l'inventaire

Une route protégée dédiée sous `/api/pricing/cards/{cardId}/price` supprimera les données de cotation de la `Card` ciblée, après vérification qu'elle est possédée. Elle remettra le prix et ses métadonnées de cotation à l'état non valorisé, sans appeler de service de collection ni enregistrer de `UserCollection`. Une synchronisation EUR ultérieure pourra renseigner de nouveau ces champs.

L'alternative consistant à utiliser une mise à jour générique de carte est rejetée : elle pourrait exposer ou réutiliser accidentellement les champs de quantité. Une exclusion durable de synchronisation est hors périmètre ; l'utilisateur demande une suppression de valeur, pas un blocage de prix.

### Rendre la limite un contrôle de présentation

L'API renvoie le top borné à 100. L'onglet Prix présente un contrôle segmenté 20, 50 ou 100 qui ne déclenche pas de nouvel appel et affiche la tranche correspondante. Chaque carte indique son prix unitaire ainsi que les quantités normale et foil, sans que ces valeurs ne modifient le rang.

### Réorganiser l'onglet sans retirer l'information catalogue

La section des dernières cartes du catalogue valorisées reste inchangée dans son contenu et sa limite de 20, mais elle est rendue après toutes les sections de valeur et le nouveau top.

## Risks / Trade-offs

- [Une égalité de prix peut donner un ordre qui semble arbitraire] → utiliser l'identifiant de carte comme second critère de tri et le couvrir par test.
- [Une suppression de prix peut être involontaire] → demander une confirmation explicite avant l'appel API et rafraîchir les insights après succès.
- [Une synchronisation suivante peut restaurer le prix supprimé] → afficher que la suppression retire la valorisation courante ; le comportement attendu est que la prochaine synchronisation EUR puisse la recréer.
- [Les prix foil ne sont pas distincts dans le modèle] → afficher les quantités foil à titre informatif et conserver le prix unitaire unique déjà utilisé par la valorisation.
