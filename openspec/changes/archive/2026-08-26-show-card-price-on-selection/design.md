## Context

Le frontend charge deja des objets carte contenant `marketPrice`, `priceCurrency` et `lastPriceAt`. Les cartes de la collection, des derniers scans et de l'onglet Prix ouvrent une meme modale de detail, tandis que le scanner utilise une vue de confirmation independante. Ces deux surfaces affichent l'identite et l'etat de collection, mais pas les metadonnees de prix disponibles.

## Goals / Non-Goals

**Goals:**
- Montrer le prix de marche lorsqu'il est disponible dans chaque fiche ouverte par selection d'une carte.
- Montrer la date de derniere mise a jour du prix lorsqu'elle est disponible.
- Appliquer le meme rendu dans la modale de detail partagee et dans la confirmation du scanner.
- Conserver le comportement actuel pour les cartes sans valeur de prix ou sans date.

**Non-Goals:**
- Declencher une synchronisation ou un appel API au moment de la selection.
- Modifier la collecte, le stockage, la devise ou la priorisation des prix.
- Ajouter le prix aux commandes d'ajout direct qui ne presentent pas une fiche de carte.

## Decisions

### Rendre les metadonnees depuis l'objet carte deja charge

La modale et le scanner utiliseront les proprietes deja presentes sur l'objet `CardDTO` recu par le frontend. Cette approche n'ajoute aucune latence ni dependance reseau a un clic et garantit que le prix correspond aux donnees visibles dans les autres ecrans.

L'alternative consistant a appeler `/api/cards/{id}` a chaque selection est ecartee: elle dupliquerait des donnees deja presentes et rendrait l'affichage dependant du reseau.

### Centraliser le fragment de presentation du prix

Un petit rendu reutilisable construira les lignes de prix et de date pour la modale de detail et la confirmation du scanner. Le prix utilisera le format monetaire existant; la date utilisera le format date/heure deja employe pour les derniers scans.

L'alternative de copier le balisage dans chaque vue est ecartee car elle rendrait les libelles et le traitement des valeurs absentes susceptibles de diverger.

### Rendre chaque metadonnee independamment

Le prix ne sera affiche que lorsque `marketPrice` n'est pas nul. La date ne sera affichee que lorsque `lastPriceAt` est presente. Une date sans prix restera donc informative si elle existe, sans afficher de valeur de remplacement trompeuse.

## Risks / Trade-offs

- [Une valeur est stockee dans une devise inattendue] -> Le rendu conserve la convention de format monetaire existante; cette proposition ne modifie pas le contrat de devise du pricing.
- [Les vues de selection divergent a l'avenir] -> Le fragment de presentation commun sera utilise par les deux parcours actuels et couvert par des tests ou verifications de rendu cibles.
- [Anciennes cartes sans metadonnees] -> Les lignes correspondantes sont masquees et les parcours de consultation et d'ajout restent utilisables.

## Migration Plan

La modification est purement additive dans le frontend et ne necessite ni migration de donnees ni changement d'API. Un retour arriere consiste a retirer le rendu de presentation sans affecter les prix deja stockes.

## Open Questions

- Aucune: la demande confirme l'affichage du prix et de la date de derniere mise a jour quand ils existent.