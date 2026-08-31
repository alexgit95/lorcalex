## 1. CSV export backend

- [x] 1.1 Ajouter à `AdminController` l'endpoint administrateur `GET /api/admin/export/dreamborn` acceptant `reserve`, avec `true` par défaut, et renvoyant un téléchargement `text/csv` UTF-8.
- [x] 1.2 Construire les lignes `Set Number,Card Number,Variant,Count` depuis les entrées de collection, sans modifier les quantités persistées.
- [x] 1.3 Lorsque `reserve=true`, appliquer la réserve d'un exemplaire par carte : foil en priorité, puis normal ; lorsque `reserve=false`, conserver toutes les quantités ; dans les deux cas, ne conserver que les variantes dont la quantité finale est positive.
- [x] 1.4 Ignorer les cartes dépourvues de numéro de set ou de numéro de carte.

## 2. Validation backend

- [x] 2.1 Ajouter des tests d'intégration couvrant l'authentification, le type de contenu, l'en-tête CSV et l'attachement de téléchargement.
- [x] 2.2 Ajouter des tests pour les quantités normale et foil, la réserve foil-prioritaire par défaut, la désactivation de la réserve, l'absence de ligne pour un exemplaire unique et l'exclusion des identifiants incomplets.

## 3. Administration

- [x] 3.1 Ajouter dans la page Administration une action d'export Dreamborn.ink, un contrôle de réserve activé par défaut et l'appel authentifié de l'endpoint CSV avec la valeur choisie.
- [x] 3.2 Déclencher le téléchargement navigateur avec un nom de fichier Dreamborn daté et afficher le résultat ou une erreur dans l'interface.

## 4. Documentation

- [x] 4.1 Mettre à jour le README avec l'action d'export Dreamborn.ink, le format CSV, l'option de réserve activée par défaut et le paramètre `reserve` de l'endpoint administrateur.
- [x] 4.2 Ajouter au changelog l'export Dreamborn.ink et le choix d'appliquer ou non la réserve.

## 5. Final validation

- [x] 5.1 Exécuter les tests ciblés puis la suite Maven pertinente et vérifier que le fichier produit suit exactement le format Dreamborn fourni.