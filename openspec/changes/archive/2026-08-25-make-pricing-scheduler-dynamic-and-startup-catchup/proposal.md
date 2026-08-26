## Why

Le système pricing existe déjà mais le déclenchement planifié est figé. En pratique, les opérateurs peuvent avoir l'impression de devoir lancer manuellement la synchronisation si l'horaire ne correspond pas à leur exploitation ou si l'application était arrêtée au moment du cron.

## What Changes

- Rendre l'horaire de synchronisation pricing configurable via setting applicatif.
- Conserver la possibilité d'activer/désactiver la tâche de fond via setting existant.
- Ajouter un rattrapage au démarrage: si aucun run planifié n'a encore été exécuté pour la journée courante, lancer un run unique au startup.
- Empêcher les doubles exécutions (startup + scheduler + manuel) via garde de concurrence.
- Exposer l'état de planification et la date du dernier run planifié dans le statut admin.

## Scope

Cette évolution concerne uniquement l'orchestration de planification et de rattrapage. Elle ne modifie pas les règles métiers de quota par tentative, ni la logique de priorisation des cartes.

## Impact

- Services backend de scheduling pricing.
- Paramètres app_settings liés à la planification.
- UI admin pour paramétrer l'horaire et visualiser l'état.
- Tests unitaires/intégration de scheduling et de rattrapage.
