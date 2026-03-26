# lorcalex

Je veux une PWA avec une stack sur spring boot et postgres.

En local je voudrais une base de donnée sqllite et sous docker postgres.

Tu devras me generer à la fois le code mais aussi le dockerfile et le necessaire pour un deploiement via docker sur portainer.

Indique toutes ces infos dans le readme.

Cette application a pour but de gerer ma collection de carte Lorcana.

Une carte sera defini, par si je la possede ou non , en combien d'exemplaire, une image (que l'on peut retrouver sur internet) mais aussi son edition.

Pour toutes les infos de la carte il faudra appeler des API externes (activable via une interface d'administration).

Je voudrais un premier ecran qui recapitule le contenu de ma collection, trié par numero et par collection, avec une vue sur les manquantes et sur l'entiereté de la collection.

Je voudrais aussi une page de statistique qui me montre via differents diagramme le nombre de carte manquante/possédé et leur rareté par edition.

Je voudrais aussi un ecran qui permet de scanner et ajouter une carte. Lors du scan l'application devrait reconnaitre la carte, l'ajouter seul à la collection en emetant un bip et une vibration. Cette PWA doit etre optimiser pour etre utiliser sur mobile.

Pour ce qui est du scan et de la reconnaissance je voudrais qu'un maximum de traitement soit fait coté client pour epargner au maximum le serveur.

L'application devra etre proteger par une page de login, avec admin / admin en local et sinon un login mot de passe comme variable d'environnement sur portainer.






