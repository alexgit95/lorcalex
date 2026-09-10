## Context

Le Scanner résout actuellement un numéro de carte dans toutes les éditions, puis utilise le numéro de set éventuellement lu par OCR pour réduire une liste de résultats. Le catalogue expose déjà les éditions via `/api/editions` et la recherche de carte accepte un paramètre `editionId` facultatif via `/api/cards/lookup`.

Cette évolution doit guider la résolution lorsqu'un utilisateur scanne des cartes d'une édition connue, sans dégrader la recherche globale actuelle. Le choix validé est un filtre strict: une édition explicitement sélectionnée ne doit jamais produire de résultat d'une autre édition.

## Goals / Non-Goals

**Goals:**
- Afficher un `<select>` d'édition dans l'onglet Scanner, alimenté par le catalogue.
- Conserver `Toutes les éditions` comme valeur initiale et comme comportement de recherche global.
- Utiliser l'identifiant de l'édition choisie pour limiter les recherches OCR et manuelles.
- Désactiver le champ de set de la saisie manuelle lorsqu'une édition précise est active.

**Non-Goals:**
- Modifier l'algorithme OCR, son prétraitement ou ses bornes de validation.
- Ajouter, modifier ou persister des données d'édition.
- Créer un nouvel endpoint ou mémoriser la sélection entre les ouvertures de l'onglet.

## Decisions

### Utiliser un select unique alimenté par l'API des éditions

Le Scanner chargera les éditions avec l'endpoint existant `/api/editions` et présentera d'abord l'option `Toutes les éditions`. Un select compact reste utilisable lorsque le catalogue contient de nombreuses éditions et évite une barre de filtres trop large.

Alternative considérée: utiliser les puces de filtre de la Collection. Cette approche est cohérente visuellement mais devient peu pratique avec un nombre d'éditions croissant.

### Transmettre l'édition sélectionnée au lookup

Quand une valeur d'édition précise est sélectionnée, les appels de résolution transmettront son `editionId` à `api.lookupCard`. L'API renvoie alors exclusivement les cartes de cette édition. Sans édition sélectionnée, le paramètre reste absent et la recherche conserve son périmètre global.

Alternative considérée: récupérer toutes les cartes puis filtrer côté client. Le filtrage côté serveur est déjà disponible, réduit la réponse et garantit le respect du blocage strict.

### Donner priorité au filtre explicite sur le set OCR et manuel

Une édition choisie est le contexte explicite de l'utilisateur et prévaut sur le numéro de set détecté par OCR. En recherche globale, le set OCR et le champ de set manuel continuent d'affiner uniquement une liste ambiguë. Sous une édition précise, le champ `Set` manuel est désactivé afin de ne pas suggérer une contrainte concurrente.

Alternative considérée: un repli sur toutes les éditions en cas d'absence de résultat. Cette option masquerait une erreur de contexte et contredirait le filtre strict demandé.

## Risks / Trade-offs

- [L'utilisateur sélectionne une mauvaise édition] -> Le Scanner ne retourne aucun résultat hors de l'édition choisie et indique clairement que la carte est introuvable dans cette édition.
- [Le chargement du catalogue échoue] -> Le select conserve ou revient à `Toutes les éditions`, qui permet toujours la résolution globale existante.
- [Le set lu par OCR diffère de l'édition choisie] -> L'édition explicite reste prioritaire; le détail OCR peut conserver cette information pour le diagnostic sans modifier le résultat.