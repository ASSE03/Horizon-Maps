# Nouveautés d'Horizon Nav FR

Ajoutez chaque changement visible dans l'appli sous **Non publié**, avant de publier, sous les titres ### Nouveau, ### Amélioré ou ### Corrigé. Écrivez chaque ligne sous la forme « Titre : ce que ça fait ». La carte « Nouveautés » affiche le titre en gras et l'explication en dessous. Seuls les changements de l'appli vont ici, pas la publication ni GitHub. Inutile d'effacer les lignes après une publication : celles déjà parues dans une version précédente sont retirées automatiquement des notes suivantes. S'il n'y a rien de nouveau, les notes reprennent les messages des commits.

## Non publié

### Nouveau
- Couleurs personnalisées : une vignette « Personnalisé » dans Réglages › Aspect de la carte › Couleurs, avec un éditeur par-dessus la carte en direct. Le jour et la nuit ont chacun leurs couleurs pour le sol, l'eau, les parcs, les routes, les grands axes, les bâtiments, les zones colorées et les textes, plus la luminosité de nuit, une teinte générale, des bâtiments lumineux et un halo au sol. On part du thème qu'on avait, donc rien ne change tant qu'on ne modifie rien. (Remplace les couleurs « Clair de lune », retirées par Horizon Maps.)
- Taille des radars : un curseur dans Réglages › Sur la carte › Sur votre itinéraire, de 50 % à 350 %, pour agrandir ou réduire le badge rouge des radars sur la carte (téléphone, autoradio et Android Auto).
- Couleurs « Crépuscule » : un nouveau choix dans Réglages › Aspect de la carte › Couleurs, les couleurs Horizon d'avant avec une nuit plus violette.
- Retour à la voiture à pied : « À pied » sur la carte « Votre voiture » calcule maintenant un itinéraire piéton dans Horizon Nav (Mapbox) au lieu d'ouvrir une autre appli.
- Limitations comme MG4Dash : les limitations de vitesse viennent maintenant d'OpenStreetMap seul, avec les paquets France de MG4Dash gardés sur le téléphone, la route la plus proche orientée dans votre sens et les règles françaises (50 en ville, 80 ailleurs, 110 sur voie rapide, 130 sur autoroute). L'ancien fonctionnement reste dans Réglages › Alertes › Vitesse › Source des limitations.
- Bison Futé : au choix de l'itinéraire, une ligne prévient quand le jour du départ est orange, rouge ou noir sur les départements traversés, dans le sens des départs et dans celui des retours.
- Tarif des bornes : en France, la liste des bornes et l'arrêt recharge proposé affichent le tarif tiré de la base nationale des bornes (par exemple « 0,49 €/kWh ») ou « Gratuit ».
- Démo avec départ au choix : avec le bouton de démo activé, choisissez le lieu de départ (recherche ou appui long sur la carte) et la vitesse, de ×1 à ×8.
- Mise à jour avec AAEnabler : Mettre à jour ouvre AAEnabler avec la nouvelle version déjà choisie, et l'appli reste dans Android Auto. Si l'installation n'aboutit pas, un nouvel appui réutilise le fichier déjà téléchargé.
- Photos aériennes de l'IGN : quand Mapbox refuse ses photos satellite à votre compte, la vue satellite affiche les photos de l'IGN sur toute la France, sous les routes et les noms.
- Avance de la flèche : la flèche est placée là où vous serez dans 1 seconde, pour compenser le retard du GPS. Réglable de 0 à 2 s dans Réglages › Écran et tailles › En conduite.
- Jour et nuit au soleil sur Android Auto : l'écran de la voiture passe en nuit au coucher du soleil et en jour à son lever, là où vous êtes, comme MG4Dash.
- Vue satellite sur Android Auto : le réglage du téléphone s'applique à l'écran de la voiture, et une ligne Vue satellite est ajoutée dans ses réglages.
- Étapes sur la carte : chaque étape est marquée d'un point, vert pour un arrêt recharge, et l'arrêt recharge a sa propre icône sur la barre du trajet.
- Disponibilité des bornes : la liste des bornes indique combien de points de charge sont libres en ce moment, avec leur puissance et leurs prises (avec votre clé TomTom, quand l'opérateur partage l'info).
- Votre voiture garée : le téléphone retient où vous vous êtes garé à la fin de chaque trajet et propose d'y retourner à pied.
- Inclinaison 3D réglable : de 30° à 65°, séparément pour un écran large, un écran haut et Android Auto (Réglages › Aspect de la carte › Vue 3D).
- Rapport en un appui : Réglages › À propos et sauvegarde › Rapport envoie directement le rapport dans le dépôt GitHub, sans texte à écrire.

### Amélioré
- Toilettes : recherche d'abord dans OpenStreetMap (la base la plus complète en France), avec aussi les toilettes des stations-service et des aires, et un rayon élargi à 10 km si rien n'est trouvé tout près.
- Téléchargements rangés : une fois la mise à jour installée, sa copie dans Téléchargements est supprimée au démarrage suivant.
- Choix de l'arrêt recharge : la borne proposée tient compte de sa puissance, de ses places libres en ce moment et de sa position sur le trajet, et le temps de charge de la puissance de votre voiture (nouveau réglage, 135 kW par défaut).
- Batterie à l'arrivée : pendant le trajet, elle est recalculée à partir de la vraie batterie lue dans Home Assistant toutes les 5 minutes, et à partir de 80 % après un arrêt recharge.
- Position à l'arrêt : dans un bâtiment, la position combinée d'Android (GPS, Wi-Fi et réseau) place la flèche plus juste, et le compteur affiche 0 au lieu de quelques km/h.
- Alerte radar : elle annonce la distance (« Radar dans 500 mètres ») au lieu de « Radar devant ».
- Radars dès l'ouverture : les derniers radars trouvés s'affichent tout de suite, puis sont mis à jour, et une réponse vide d'un serveur est redemandée à un autre.
- Thème Horizon la nuit simplifié : les bâtiments ressortent toujours plus clairs que le sol, sur le téléphone, l'autoradio et Android Auto. Les réglages de lumière, de luminosité et de bâtiments de nuit sont retirés.
- Liste des bornes : le nom n'est plus coupé, les places libres sont en dessous, l'adresse et le tarif tiennent sur 2 lignes.
- Réglages allégés : plus de rubrique de don, ni de proposition de don à l'ouverture.

### Corrigé
- Carte claire avant le lever du soleil : la lumière d'aube (carte rosée) ne s'affiche plus tant que l'écran est en mode nuit ; elle commençait 40 minutes avant le lever, en pleine nuit.
- Virages sans itinéraire : la flèche ne file plus tout droit au-delà du carrefour, et la carte tourne plus vite dans les virages francs au lieu de rester de travers 2 à 3 secondes.
- Nom de rue figé : quand les serveurs OpenStreetMap ne répondent pas, le nom vient de Nominatim, et l'ancien nom disparaît dès qu'on s'en est éloigné de 150 m au lieu de rester affiché sur les rues suivantes.
- Plantage avec l'écran conducteur : sur les voitures qui affichent la carte derrière le volant (vu sur une Jaguar), l'appli pouvait planter dès le démarrage d'Android Auto. C'est réglé.
- Démo avec un départ choisi : lancer le guidage avec « Démarrer » lance aussi la démo (la flèche restait au départ). Le bouton « Lancer la démo » est maintenant en tête de la ligne, toujours visible.
- Incidents en anglais (« Heavy rain ») : les plus courants s'affichent et s'annoncent en français.
- Démo : une erreur sur une position ne la bloque plus, et le rapport indique où elle en est.
- Mise à jour en deux fois : l'installation passe directement par l'installateur d'Android au lieu d'AAEnabler, qui se fermait sans installer.
- Démo avec Android Auto : l'écran de la voiture part du départ de démo choisi sur le téléphone, et non plus de votre position.
- Démo lancée sur le téléphone : l'écran Android Auto suit maintenant le trajet simulé au lieu de rester au départ.
- Flèche à côté de la rue à l'arrêt : la position est aussi recalée sur la route à l'arrêt et au pas (12 m au plus), plus seulement en roulant.
- « Vérifier maintenant » ne faisait rien quand la vérification automatique du lancement était en cours : le bouton affiche maintenant « Vérification… » puis le résultat.
- Toilettes introuvables quand les serveurs Overpass sont saturés : la recherche passe d'abord par Nominatim (la recherche d'OpenStreetMap), plus rapide et disponible.
- Recherches OpenStreetMap (toilettes, radars…) : les quatre serveurs sont essayés au lieu de deux, et un serveur en panne est mis de côté 5 minutes. Message clair quand aucun ne répond.
- Boussole : le trait rouge tourne avec la carte et montre toujours le nord.
- Carte « Votre voiture » : le titre et « Garée il y a… » ne sont plus coupés, le bouton « À pied » passe en dessous.
- Carte « Votre voiture » écrasée : le bouton s'appelle maintenant « À pied », et « Votre voiture » ne s'écrit plus lettre par lettre à la verticale.
- Carte voilée par le brouillard : le brouillard ajouté selon la météo ne recouvre plus la vue satellite et est plus léger sur la carte normale.
- Carte bloquée par Mapbox : les photos satellite refusées n'étaient plus redemandées en boucle, ce qui faisait aussi refuser la carte normale et le trafic.
- Serveurs OpenStreetMap bloqués : le téléchargement des zones à faibles émissions n'est plus relancé à chaque ouverture après un échec.
- Démo qui partait de votre position : elle part bien du départ choisi.
- Cases démesurées en portrait : l'arrêt recharge et le choix du départ de démo n'étirent plus leur case, et leur bouton reste visible.
- Bouton Paramètres d'Android Auto : il cherche l'écran de paramètres d'Android Auto, au lieu de retomber sur la fiche de l'appli.
