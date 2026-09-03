## Context

La page Administration peut déjà déclencher des téléchargements depuis des données renvoyées par l'API et `AdminController` peut accéder aux entrées de collection avec leur carte associée. Chaque entrée conserve une quantité normale et une quantité foil ; la carte fournit son numéro et l'édition son numéro de set. Dreamborn.ink attend un CSV plat avec une ligne par variante.

L'export doit représenter les cartes disponibles pour jouer, sans modifier les quantités persistées de la collection. Au lancement du téléchargement, l'administrateur choisit d'appliquer ou non la règle métier de réserve, activée par défaut. Lorsque cette règle est active, elle réserve un exemplaire par carte, en choisissant une foil en priorité lorsqu'elle existe.

## Goals / Non-Goals

**Goals:**
- Proposer depuis l'administration un téléchargement CSV directement importable dans Dreamborn.ink.
- Produire exactement les colonnes `Set Number`, `Card Number`, `Variant` et `Count`, dans cet ordre.
- Permettre à l'administrateur d'activer ou de désactiver la réserve d'un exemplaire par entrée de collection.
- Déduire une seule carte réservée, foil en priorité puis normale, lorsque la réserve est activée.
- Ne produire que des lignes valides avec une quantité strictement positive.

**Non-Goals:**
- Ne pas importer de deck ou de collection depuis Dreamborn.ink.
- Ne pas modifier les données de collection, ni ajouter de préférence de réserve persistée.
- Ne pas exposer l'export Dreamborn via le endpoint d'export API par clé existant.
- Ne pas convertir ou rapprocher les identifiants de cartes manquants.

## Decisions

### Endpoint administrateur CSV dédié

Ajouter un endpoint `GET /api/admin/export/dreamborn` protégé par l'authentification administrateur existante. Il acceptera le paramètre booléen `reserve`, valant `true` par défaut lorsqu'il est omis, et renverra `text/csv` avec une en-tête `Content-Disposition` de téléchargement. Un endpoint dédié maintient la séparation entre la sauvegarde JSON, l'export API externe et l'export spécifique à Dreamborn.

L'alternative consistant à faire calculer et sérialiser le CSV entièrement dans le navigateur est écartée : le serveur est propriétaire des données et permet de tester le contrat de fichier directement.

### Génération des lignes par variante

Pour chaque entrée de collection, le générateur l'ignore si `Edition.setNumber` ou `Card.cardNumber` est absent. Sinon, il lit les quantités normale et foil en les traitant comme zéro lorsqu'elles sont nulles. Lorsque `reserve=true`, il réserve un exemplaire foil si la quantité foil est positive ; à défaut, il réserve un exemplaire normal si la quantité normale est positive. Lorsque `reserve=false`, il conserve les quantités d'origine. Il émet ensuite, dans l'ordre normal puis foil, une ligne par quantité finale strictement positive.

Cette règle réserve exactement une carte physique par carte, même lorsqu'elle existe dans les deux variantes. L'alternative consistant à décrémenter les deux variantes retirerait deux exemplaires et ne refléterait pas la règle métier.

### Téléchargement depuis l'administration

Ajouter une action dédiée dans la zone d'administration, accompagnée d'un contrôle activé par défaut pour choisir la réserve. Le navigateur appellera l'endpoint avec le jeton d'authentification et la valeur explicite de `reserve`, créera un `Blob` CSV et lancera le téléchargement. Le nom du fichier indiquera Dreamborn et la date de l'export.

## Risks / Trade-offs

- [Une carte manque de numéro de set ou de carte] -> L'entrée est ignorée pour éviter de produire un CSV non importable ; les données locales restent inchangées.
- [Les valeurs de quantité historiques sont nulles ou négatives] -> Les valeurs non positives sont traitées comme non exportables, sans faire échouer l'export entier.
- [L'outil Dreamborn attend un encodage strict] -> Le contenu est produit en UTF-8 avec des valeurs numériques et des variantes contrôlées ; le test vérifie l'en-tête et les lignes exactes.