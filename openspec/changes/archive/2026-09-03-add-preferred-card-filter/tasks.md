## 1. Filtre de la grille Collection

- [x] 1.1 Ajouter l'option interne `wanted` et le libellé **Préférées** dans la barre des filtres de la page Collection.
- [x] 1.2 Appliquer le filtre `wanted` sur le booléen de carte afin d'inclure les cartes préférées possédées et manquantes.
- [x] 1.3 Vérifier que le filtre reste compatible avec la sélection d'édition, la recherche par nom et l'état vide existant.

## 2. Vérification

- [x] 2.1 Vérifier la couverture de comportement : aucune infrastructure de tests frontend n'est présente dans le dépôt ; la condition de filtrage a été validée par diagnostics statiques et inspection ciblée.
- [x] 2.2 Vérifier manuellement la page Collection avec une édition contenant des cartes préférées dans les deux états de possession.

## 3. Documentation

- [x] 3.1 Mettre à jour la description de la page Collection dans `README.md` pour mentionner le filtre **Préférées**.
- [x] 3.2 Ajouter une entrée **Unreleased** dans `CHANGELOG.md` pour le nouveau filtre.