## Context

La synchronisation pricing est déjà capable de traiter un backlog avec budget strict par tentatives. Le besoin restant est purement orchestration: rendre l'exécution quotidienne réellement autonome et paramétrable, tout en couvrant le cas où l'application n'était pas active à l'heure prévue.

## Goals

- Permettre un cron configurable via settings.
- Exécuter automatiquement chaque jour sans action manuelle.
- Rattraper un run manqué au démarrage, une seule fois par jour.
- Préserver les garde-fous existants: quota local, verrou de concurrence, priorisation.

## Non-Goals

- Changer la règle de comptage quota (tentative-based).
- Changer l'algorithme de priorisation des cartes.
- Ajouter de nouveaux fournisseurs pricing.

## Decisions

1. Scheduler dynamique
- Introduire un service dédié de planification qui lit pricing_schedule_cron et (re)programme la tâche.
- Validation de cron: fallback sûr si expression invalide, avec indication de statut.

2. Activation/désactivation
- pricing_sync_enabled reste la garde principale.
- Si désactivé, ni cron ni catch-up startup ne déclenchent de run.

3. Catch-up startup
- Stocker la date du dernier run planifié dans un setting dédié (pricing_last_scheduled_run_date).
- Au démarrage, si la date stockée est différente du jour courant, déclencher un run startup_catchup.
- Après tentative de run, marquer la date du jour pour éviter un second catch-up.

4. Concurrence
- Réutiliser la garde running existante dans le moteur de sync pour éviter toute exécution simultanée.

5. Observabilité
- Le statut admin expose: cron effectif, cron valide/invalide, prochain run calculé (si disponible), dernière date de run planifié.

## Risks and Mitigations

- Cron invalide entré par l'admin
  - Mitigation: validation stricte et fallback explicite.
- Double déclenchement startup puis cron immédiat
  - Mitigation: verrou de concurrence + enregistrement de la date de run planifié.
- Ambiguïté timezone
  - Mitigation: documenter la timezone serveur comme référence de planification.
