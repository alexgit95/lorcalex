## Context

La SPA d'administration est incluse dans le JAR Spring Boot et rend son en-tête côté navigateur. Le POM définit déjà la version de l'application, mais l'artefact n'embarque pas de révision Git. Le Dockerfile copie le POM et les sources, sans copier le répertoire `.git`; une résolution automatique du SHA pendant le build de l'image ne serait donc pas fiable.

La publication d'images Docker est effectuée par GitHub Actions, qui fournit le SHA complet du commit déclencheur dans `github.sha`. L'endpoint sous `/api/admin/**` est déjà protégé par authentification.

## Goals / Non-Goals

**Goals:**
- Conserver dans chaque JAR la version du POM et la révision source explicitement fournie au build.
- Propager le SHA GitHub Actions jusqu'au build Maven exécuté dans Docker.
- Exposer l'identité de build aux seuls administrateurs et l'afficher dans l'en-tête Administration.
- Afficher la révision sous forme de SHA court de 7 caractères.
- Garder le build exploitable sans information Git avec une valeur de repli explicite.

**Non-Goals:**
- Fournir l'identité de build sur une route publique ou dans les exports de données.
- Déterminer dynamiquement le commit du conteneur au démarrage.
- Ajouter une interface d'historique de versions ou de gestion des déploiements.

## Decisions

### Générer les métadonnées dans l'artefact Maven

Le package Maven générera des propriétés de build contenant la version `${project.version}` et une propriété `git.commit`. L'application lira ces propriétés plutôt que le POM ou Git à l'exécution.

Cette approche rend l'identité immuable et rattachée au JAR réellement exécuté. Lire `git rev-parse` dans l'application est écarté: Git et le répertoire de travail ne sont pas présents dans l'image finale. Utiliser le seul `pom.properties` est également écarté: il ne contient pas de SHA.

### Transmettre explicitement le SHA au build Docker

Le Dockerfile acceptera un argument `GIT_COMMIT` et le passera au package Maven sous forme de propriété. Les étapes de publication GitHub Actions fourniront `${{ github.sha }}` comme argument de build.

La valeur complète est stockée afin de préserver la traçabilité. L'API et l'interface produisent le SHA court à partir des sept premiers caractères. Le build local ou une construction qui ne fournit pas `GIT_COMMIT` utilisera `unknown` plutôt que d'échouer ou d'afficher une information inventée.

### Publier une API d'administration dédiée

`GET /api/admin/version` retournera la version et le commit court de l'artefact actif. Il relève de la règle existante qui authentifie les routes d'administration.

Réutiliser `/api/admin/settings` est écarté, car l'identité de build ne constitue pas un paramètre modifiable et ne doit pas être stockée en base de données.

### Charger l'identité avec les données d'administration

La SPA ajoutera l'appel de version à son chargement parallèle de la page puis rendra `version - commit` dans l'en-tête. En cas d'échec de l'appel, la page conserve ses fonctionnalités existantes et affiche une valeur indisponible plutôt que de bloquer son rendu.

## Risks / Trade-offs

- [Un build non issu de GitHub Actions n'envoie pas de SHA] -> Le repli `unknown` rend cette absence visible sans interrompre l'empaquetage.
- [Une mise à jour partielle du workflow oublie un chemin de publication] -> Toutes les actions `docker/build-push-action` recevront le même argument de build et seront revues par un test de package ainsi qu'une vérification de workflow.
- [Un SHA court peut théoriquement être ambigu] -> Le SHA complet reste embarqué dans les propriétés de build; l'affichage est volontairement limité à sept caractères pour rester lisible.
- [L'API est indisponible pendant un chargement] -> L'affichage de version est non bloquant et n'empêche pas les opérations d'administration.