## Why

Les prix synchronises sont disponibles dans les donnees de carte, mais ils ne sont pas visibles lorsqu'un utilisateur consulte une carte depuis la collection, les derniers scans ou l'onglet Prix. La confirmation d'une carte identifiee par le scanner ne montre pas non plus cette information, ce qui oblige a naviguer vers une autre vue pour connaitre la valeur et sa fraicheur.

## What Changes

- Afficher le prix de marche d'une carte et la date de sa derniere mise a jour dans la fiche de detail ouverte apres la selection d'une carte.
- Afficher les memes informations dans la confirmation presentee par le scanner apres l'identification d'une carte.
- Masquer ces informations lorsqu'aucun prix ou aucune date de mise a jour n'est disponible, sans modifier les donnees ni declencher de synchronisation de prix.

## Capabilities

### New Capabilities
- `card-selection-price-details`: Affichage conditionnel du prix de marche et de sa derniere date de mise a jour dans les parcours de consultation et de confirmation d'une carte.

### Modified Capabilities

- Aucun.

## Impact

- Frontend: [src/main/resources/static/app.js](../../../../src/main/resources/static/app.js), notamment la modale de detail partagee et les vues de confirmation du scanner.
- Presentation: styles de detail de carte existants dans `app.css`, seulement si un style dedie est necessaire.
- Le contrat API `CardDTO`, qui expose deja `marketPrice` et `lastPriceAt`, ne change pas.