## Context

La page Collection de `src/main/resources/static/app.js` charge les cartes de l'édition sélectionnée, puis applique localement un filtre de présentation sur les cartes possédées, manquantes ou foil. Chaque `CardDTO` transporte déjà le booléen `wanted`, y compris lorsqu'une carte est possédée.

Le changement doit rester limité à la grille de collection : aucune nouvelle donnée ne doit être stockée et aucun endpoint supplémentaire n'est nécessaire.

## Goals / Non-Goals

**Goals:**

- Ajouter un mode de filtre `wanted` présenté à l'utilisateur sous le libellé **Préférées**.
- Inclure toutes les cartes dont `wanted` vaut `true`, indépendamment de `owned`.
- Préserver la sélection d'édition, la recherche par nom et les filtres existants.
- Conserver le filtre sélectionné lors des rendus de la grille déclenchés par une modification locale.

**Non-Goals:**

- Modifier le modèle `Card`, l'API ou la persistance du marqueur préféré.
- Changer la possibilité de marquer ou démarquer une carte comme préférée.
- Combiner plusieurs filtres de statut entre eux, par exemple Préférées et Foil simultanément.

## Decisions

### Réutiliser le filtrage local de la grille

Le filtre sera ajouté à la liste des clés et libellés déjà rendue par `renderCollection()`. `renderCards()` appliquera la condition `wanted === true` au même niveau que les conditions `owned`, `missing` et `foil`.

Cette approche évite un appel réseau ou une évolution d'API, puisque `getCards` renvoie déjà le champ nécessaire. Une requête serveur dédiée serait disproportionnée pour un catalogue déjà chargé par édition.

### Traiter Préférées comme un filtre de statut exclusif

Le mode `wanted` sélectionnera toutes les cartes préférées, qu'elles soient possédées ou manquantes. La sélection d'édition et la recherche resteront appliquées après ce filtre, car elles constituent des contraintes indépendantes de navigation et de recherche.

### Utiliser « Préférées » comme libellé d'interface

Le terme français **Préférées** décrit le concept utilisateur sans exposer le nom technique `wanted`. La clé interne restera `wanted` pour rester alignée avec le modèle et les contrats existants.

## Risks / Trade-offs

- [Une édition ou une recherche peut ne contenir aucune carte préférée] → Réutiliser l'état vide existant de la grille.
- [Une carte préférée possédée n'affiche pas l'étoile dans la grille] → Le filtre doit se baser sur la donnée persistée `wanted`, et non sur la présence du bouton visuel.
- [Le filtre foil ne peut pas être combiné avec Préférées] → Conserver le comportement actuel de filtre de statut unique et le documenter dans les tâches.