# Horizon Nav FR

Version française d'[Horizon Nav](https://github.com/benbishop028-arch/Horizon-Maps), le GPS gratuit pour autoradios Android et Android Auto, de benbishop028-arch. Sans publicité, sans compte et sans pistage.

Ce fork traduit toute l'appli (écrans, annonces et commandes vocales, Android Auto) et ajoute des réglages pensés pour la France. Les nouveautés de l'auteur y sont reprises et traduites régulièrement.

⬇️ **[Télécharger la dernière version (APK)](https://github.com/ASSE03/Horizon-Maps/releases/download/app/HorizonNav-public.apk)**

L'appli s'installe à côté de l'appli officielle, sous le nom **« Horizon Nav FR »**, puis se met à jour toute seule.

💙 Pour soutenir l'auteur de l'appli d'origine : **[PayPal](https://www.paypal.me/BBishop462)** (facultatif).

---

## Ce que le fork ajoute

- **Tout en français** : écrans, réglages, annonces vocales (« Dans 300 mètres, tournez à droite sur l'A7 »), commandes vocales et écran Android Auto. La voix est celle de la synthèse vocale française du téléphone.
- **Panneaux de direction à la française** : panneaux bleus (autoroute), verts (routes principales) et blancs (routes locales), avec les cartouches A, N, D et E aux bonnes couleurs, et la case « SORTIE ». Les panneaux britanniques restent au Royaume-Uni.
- **ZFE** : les zones à faibles émissions françaises sont téléchargées, signalées sur l'itinéraire et peuvent être évitées.
- **Batterie de la voiture via Home Assistant** : niveau de charge, autonomie et consommation lus dans Home Assistant (adresse et jeton dans les réglages), pour l'estimation de la charge à l'arrivée.
- **Jour et nuit sur Android Auto au lever et au coucher du soleil**, selon votre position. On peut aussi suivre la voiture, ou forcer le jour ou la nuit.
- **Limitations de vitesse vérifiées** : celle de l'itinéraire est comparée à la carte OpenStreetMap, et la plus basse est gardée tant que les deux ne sont pas d'accord.
- **Vue satellite de secours** : si Mapbox refuse les photos de sa nouvelle vue satellite, l'appli passe sur l'ancienne.
- **Rapports de bug déposés dans ce dépôt** : avec un jeton GitHub, « Avis ou problème » envoie le rapport directement ici (branche `diagnostics`).
- **Réglages simplifiés** : les champs propres au Royaume-Uni ou à l'Allemagne (prix des carburants) n'apparaissent que dans ces pays.
- **Ville affichée** pour les stations de recharge et les lieux trouvés à proximité.

---

## Fonctions de l'appli

### 🗺️ La carte
- Carte 3D fluide avec bâtiments, monuments et arbres, et relief 3D en option
- Votre position : une voiture 3D (berline, compacte, break, SUV, pick-up, utilitaire ou sportive) de la couleur de votre choix, une flèche ou votre propre image
- Vue 2D / 3D, gardée d'un trajet à l'autre
- Thèmes jour, nuit, automatique ou liés aux phares, et vue satellite
- Zoom et carte orientée au nord gardés en mémoire, zoom par défaut réglable
- Vue d'ensemble du trafic à l'arrêt (désactivable)
- Gros plan des sorties et des bretelles

### 🧭 Le guidage
- Itinéraires avec le trafic en direct. Avec les deux clés, TomTom et Mapbox sont interrogés et le plus rapide est proposé en premier
- Cartes et calcul d'itinéraire hors ligne pour tous les pays, plusieurs zones à la fois. Les grands pays se téléchargent par région, et l'appli propose celle où vous êtes
- Voies de circulation sur la carte de direction, et panneau des voies au-dessus de la route aux échangeurs
- Limitations de vitesse, zones de vitesse moyenne et stops, avec alertes vocales
- Radars, là où la loi les autorise (voir plus bas)
- Propositions d'itinéraire plus rapide en cours de route
- Éviter les péages, les autoroutes, les ferries ou les ZFE
- Mode poids lourd : hauteur, largeur, longueur, poids et matières dangereuses ; les itinéraires évitent les ponts bas, les limites de poids et les interdictions, avec alertes à l'approche
- Voiture électrique : niveau de batterie, charge à l'arrivée et arrêts recharge
- Prix des carburants en France, au Royaume-Uni, en Autriche et en Allemagne, E85 compris
- Arrêts en un geste le long du trajet : carburant, recharge, café, restauration, aires et toilettes
- Signalements (police, accident, danger, véhicule en panne, radar mobile) depuis l'autoradio, le téléphone ou Android Auto

### 📱 Téléphone et voiture
- Android Auto complet, avec la même carte que dans l'appli, la recherche et les signalements depuis l'écran de la voiture
- Carte sur l'écran du conducteur, sur les voitures dont Android Auto le permet
- Prochaine direction dans la notification, pour les lanceurs et les montres qui l'affichent
- Envoi d'une destination du téléphone vers l'autoradio
- Reprise du trajet si l'autoradio redémarre en route

---

## Installation

### Sur un téléphone ou un autoradio
1. Téléchargez l'APK avec le lien ci-dessus et ouvrez-le.
2. Autorisez l'installation depuis des sources inconnues si Android le demande.
3. Ouvrez Horizon Nav FR et suivez la mise en route : localisation, unités et clés (voir plus bas).
4. Pour rouler sans réseau, téléchargez votre zone dans **Réglages › Cartes hors ligne**.

### Sur Android Auto
Android Auto n'affiche les applis installées hors du Play Store qu'une fois autorisé :

1. Ouvrez les paramètres d'Android Auto sur le téléphone.
2. Touchez une dizaine de fois **Version**, tout en bas, pour activer les paramètres pour les développeurs.
3. Menu ⋮ › **Paramètres pour les développeurs** › cochez **Sources inconnues**.
4. Dans **Personnaliser le lanceur**, vérifiez que **Horizon Nav FR** est coché.

**Toujours absente ?** Sur certains téléphones, Android Auto ignore les applis installées directement depuis l'APK. Installez-la plutôt avec **[AAEnabler](https://github.com/malebuffy/AAEnabler)** ou **[KingInstaller](https://github.com/fcaronte/KingInstaller/releases)**.

Avec AAEnabler, faites aussi les mises à jour avec lui : l'appli enregistre chaque nouvelle version dans le dossier Téléchargements pour qu'AAEnabler l'installe.

---

## 🔑 Vos clés gratuites : Mapbox et TomTom

L'appli n'a aucune clé intégrée : vous ajoutez les vôtres dans **Réglages › Clés et comptes**.

**Mapbox** (nécessaire pour la carte)
1. Créez un compte sur [account.mapbox.com](https://account.mapbox.com).
2. Dans **Tokens**, copiez votre **Default public token** avec le bouton de copie (il commence par `pk.`).
3. Collez-le dans la case Mapbox. Ne le tapez pas à la main : une seule erreur et Mapbox le refuse (erreur 401).

**TomTom** (conseillé : meilleure recherche d'adresses, trafic, et un second avis sur les itinéraires)
1. Créez un compte sur [developer.tomtom.com](https://developer.tomtom.com).
2. Copiez votre clé API depuis le tableau de bord.
3. Collez-la dans la case TomTom.

Les deux ont des quotas gratuits largement suffisants pour une personne. Une même clé peut servir sur plusieurs appareils.

---

## ⚠️ Radars en France

En France, la loi interdit aux GPS d'indiquer l'emplacement exact des radars. L'amende peut aller jusqu'à 1 500 € et l'appareil peut être confisqué. Le réglage **Réglages › Alertes › Radars › Désactivés là où c'est interdit** coupe donc les radars en France, en Allemagne et en Suisse. Il est activé par défaut.

---

## 🔒 Ce qui sort de la voiture

Pas de compte, pas de publicité, pas de serveur Horizon Nav. L'appli parle directement à ces services, seulement pour la fonction utilisée :

| Service | Ce qu'il reçoit | Pour quoi |
|---|---|---|
| Mapbox | Votre position et votre destination | Carte, itinéraires, recherche, trafic, limitations |
| TomTom (avec votre clé) | Votre position et votre destination | Itinéraires, recherche, trafic, limitations |
| OpenStreetMap (Overpass, Nominatim, Photon) | Les environs | Radars, limitations, ZFE, limites poids lourds, feux, recherche |
| Open-Meteo, RainViewer | Votre position approximative | Météo et radar de pluie (si activés) |
| Prix des carburants | La zone recherchée | Prix des carburants |
| Home Assistant (si configuré) | Rien | Lecture de la batterie de la voiture |
| GitHub | Rien sur vous, sauf les rapports que vous envoyez | Mises à jour et rapports de bug |

Les rapports envoyés contiennent la version, l'appareil, les principaux réglages et les derniers changements de limitation avec leur position GPS. Ils ne contiennent aucune clé.

---

## 🛠️ Dépannage

- **Carte blanche ou noire** : vérifiez la clé Mapbox dans Réglages › Clés et comptes.
- **Mapbox « 401 »** : la clé est refusée. Recopiez-la avec le bouton de copie.
- **« Pas de réseau » alors qu'il y en a** : vérifiez la date et l'heure de l'appareil.
- **Lent sur un autoradio modeste** : baissez le détail de la carte et les images par seconde dans les réglages.
- **Téléphone chaud sur Android Auto** : laissez les images par seconde sur Auto.
- **Trop petit sur l'écran de la voiture** : Réglages › Android Auto › Tailles sur l'écran de la voiture.
- **Pas de voix** : installez la voix française dans les paramètres de synthèse vocale d'Android.
- **Vue satellite ou radars absents** : un VPN ou un bloqueur de pub peut faire refuser les demandes par Mapbox ou OpenStreetMap. Essayez sans.

---

## 💬 Signaler un problème

Le plus simple : **Réglages › À propos et sauvegarde › Avis ou problème** (bouton **Signaler**). Décrivez ce qui s'est passé et où, puis touchez **Envoyer** (avec un jeton GitHub enregistré) ou **Copier**.

---

## Pour les développeurs

- Chaque modification envoyée sur `main` compile l'APK (workflow **Build Horizon Nav FR**, onglet Actions) et le publie sur la release `app`, où l'appli cherche ses mises à jour.
- Chaque version a aussi sa propre release `v1.0.N`, dont les notes alimentent la carte « Nouveautés ». Ces notes viennent de la section « Non publié » de [CHANGELOG.fr.md](CHANGELOG.fr.md) (lignes « Titre : ce que ça fait », sans répéter celles déjà parues), ou à défaut des messages des commits. Seules les 10 dernières versions sont gardées.
- Le secret `SIGNING_KEYSTORE_BASE64` (keystore de signature) est nécessaire pour que les mises à jour s'installent par-dessus la version précédente.
