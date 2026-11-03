
package com.headunit.launcher.ui

import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Traffic
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.TurnRight
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.headunit.launcher.BuildConfig
import com.headunit.launcher.FuelFinder
import com.headunit.launcher.Geo
import com.headunit.launcher.NavEngine
import com.headunit.launcher.OfflinePlaces
import com.headunit.launcher.Place
import com.headunit.launcher.Prefs
import com.headunit.launcher.PreferenceKeys
import com.headunit.launcher.mapSource
import com.headunit.launcher.mapDetail
import com.headunit.launcher.setApiKey
import com.headunit.launcher.Zones
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** A settings section; [heading] puts a small group heading (Map, Driving, You, Device) above it in the list. */
private class Section(val title: String, val sub: String, val icon: ImageVector, val tint: Color, val heading: String? = null)

// (the order here is the order in the list; SettingsIndex refers to them by number)
private val sections = listOf(
    Section("Aspect de la carte", "Jour et nuit, détail, 3D, satellite", Icons.Rounded.Map, Color(0xFF4C8DFF), "CARTE"),
    Section("Sur la carte", "Trafic, feux, radars, panneaux, météo", Icons.Rounded.Traffic, Color(0xFF4C8DFF)),
    Section("Votre flèche et le tracé", "Flèche ou voiture, couleurs, aspect du tracé", Icons.Rounded.Navigation, Color(0xFF4C8DFF)),
    Section("Itinéraires", "À éviter, éco, plus rapides, démarrage", Icons.Rounded.Route, Color(0xFF12A4B4), "CONDUITE"),
    Section("Carte de direction et voies", "Panneaux, voies, carte de trajet, arrivée", Icons.Rounded.TurnRight, Color(0xFF12A4B4)),
    Section("Alertes", "Vitesse, radars, dangers, signalements", Icons.Rounded.NotificationsActive, Color(0xFFEA4335)),
    Section("Son et voix", "Guidage vocal et volume", Icons.Rounded.VolumeUp, Color(0xFFEA4335)),
    Section("Lieux et trajets", "Domicile, travail, favoris, partage, journal", Icons.Rounded.Star, Color(0xFFF29900), "VOUS"),
    Section("Carburant et recharge", "Votre carburant, prix, voiture électrique", Icons.Rounded.LocalGasStation, Color(0xFF34A853)),
    Section("Cartes hors ligne", "Cartes, itinéraires et noms de villes sans réseau", Icons.Rounded.CloudDownload, Color(0xFF8E5CF0)),
    Section("Écran et tailles", "Luminosité, tailles, unités, en conduite", Icons.Rounded.BrightnessMedium, Color(0xFFFFB020), "APPAREIL"),
    Section("Android Auto", "Tailles sur l'écran de la voiture et installation", Icons.Rounded.DirectionsCar, Color(0xFF9CA4BF)),
    Section("Performances", "Fluidité sur les autoradios lents, démo", Icons.Rounded.Speed, Color(0xFF9CA4BF)),
    Section("Clés et comptes", "Clés de carte et recherche", Icons.Rounded.VpnKey, Color(0xFF9CA4BF)),
    Section("À propos et sauvegarde", "Mises à jour, autorisations, sauvegarde", Icons.Rounded.Info, Color(0xFF9CA4BF)),
)

/** Every setting in one screen: sections on the left, the chosen section on the right, in the map screen's soft style. */
@Composable
fun SettingsScreen(prefs: Prefs, engine: NavEngine, onClose: () -> Unit) {
    val base = LocalVw.current
    val v = remember(base) { navVw(base) }
    CompositionLocalProvider(LocalVw provides v) { SettingsBody(prefs, engine, onClose) }
}

@Composable
private fun SettingsBody(prefs: Prefs, engine: NavEngine, onClose: () -> Unit) {
    val c = LocalVw.current
    var cat by rememberSaveable { mutableIntStateOf(0) }
    // portrait / narrow: one pane at a time (the section list, or the open section with a back arrow)
    val narrowS = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 720
    var showList by rememberSaveable { mutableStateOf(true) }
    androidx.activity.compose.BackHandler(enabled = narrowS && !showList) { showList = true }
    var query by remember { mutableStateOf("") }
    var highlight by remember { mutableStateOf<String?>(null) }
    val focus = androidx.compose.ui.platform.LocalFocusManager.current
    val words = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
    val matches = remember(query) {
        if (words.isEmpty()) emptyList() else settingsIndex.filter { r ->
            val hay = (r.title + " " + r.sub + " " + r.group + " " + sections[r.section].title).lowercase()
            words.all { hay.contains(it) }
        }.sortedBy { if (it.title.lowercase().startsWith(words[0])) 0 else if (it.title.lowercase().contains(words[0])) 1 else 2 }.take(40)
    }
    Row(Modifier.fillMaxSize().background(c.bg).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        // the list of sections
        if (!narrowS || showList) Column(
            Modifier.then(if (narrowS) Modifier.fillMaxWidth() else Modifier.width(290.dp)).fillMaxHeight().clip(RoundedCornerShape(28.dp)).background(c.panel).border(1.dp, c.line, RoundedCornerShape(28.dp)).verticalScroll(rememberScrollState()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(Modifier.fillMaxWidth().padding(start = 4.dp, top = 4.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(46.dp).clip(CircleShape).background(c.tile).pressClick(onClose), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.ArrowBack, "Retour à la carte", tint = c.text, modifier = Modifier.size(24.dp))
                }
                Text("Réglages", color = c.text, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 14.dp))
            }
            // search every setting by name
            Row(
                Modifier.fillMaxWidth().padding(bottom = 8.dp).height(46.dp).clip(RoundedCornerShape(50)).background(c.tile).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Search, null, tint = c.sub, modifier = Modifier.size(20.dp))
                Box(Modifier.weight(1f).padding(start = 10.dp), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) Text("Rechercher un réglage", color = c.sub, fontSize = 15.sp)
                    androidx.compose.foundation.text.BasicTextField(
                        value = query, onValueChange = { query = it; highlight = null }, singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(color = c.text, fontSize = 15.sp),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(c.accent),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { focus.clearFocus() }),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (query.isNotEmpty()) Icon(Icons.Rounded.Close, "Effacer", tint = c.sub, modifier = Modifier.size(20.dp).clickable { query = ""; focus.clearFocus() })
            }
            if (words.isNotEmpty()) {
                if (matches.isEmpty()) Text("Aucun résultat pour « ${query.trim()} »", color = c.sub, fontSize = 13.sp, modifier = Modifier.padding(10.dp))
                matches.forEach { r ->
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable {
                            cat = r.section; highlight = r.title; query = ""; focus.clearFocus(); showList = false
                        }.padding(horizontal = 12.dp, vertical = 9.dp),
                    ) {
                        Text(r.title, color = c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(sections[r.section].title + (if (r.group.isNotBlank()) " · " + r.group else ""), color = c.sub, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            } else sections.forEachIndexed { i, s ->
                // the small group headings in the list: Map, Driving, You, Device
                if (s.heading != null) Text(s.heading, color = c.sub, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp,
                    modifier = Modifier.padding(start = 14.dp, top = if (i == 0) 2.dp else 12.dp, bottom = 2.dp))
                val on = i == cat
                val rowBg by androidx.compose.animation.animateColorAsState(if (on) c.accent.copy(alpha = 0.16f) else Color.Transparent, androidx.compose.animation.core.tween(200), label = "secBg")
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(rowBg).clickable { cat = i; highlight = null; showList = false }.padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(38.dp).clip(CircleShape).background(s.tint.copy(alpha = if (on) 0.26f else 0.16f)), contentAlignment = Alignment.Center) {
                        Icon(s.icon, null, tint = s.tint, modifier = Modifier.size(21.dp))
                    }
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(s.title, color = if (on) c.accent else c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text(s.sub, color = c.sub, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        // the chosen section (each opens at the top), sliding up gently into place
        if (!narrowS || !showList) androidx.compose.animation.AnimatedContent(
            targetState = cat,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            transitionSpec = {
                (androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(220, delayMillis = 60)) +
                    androidx.compose.animation.slideInVertically(androidx.compose.animation.core.tween(260)) { it / 24 }) togetherWith
                    androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(120))
            },
            label = "section",
        ) { cat ->
          CompositionLocalProvider(LocalSettingHighlight provides highlight) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 4.dp, vertical = 4.dp)) {
                if (narrowS) Row(Modifier.padding(bottom = 6.dp).clip(RoundedCornerShape(50)).clickable { showList = true }.padding(end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).clip(CircleShape).background(c.tile), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.ArrowBack, "Tous les réglages", tint = c.text, modifier = Modifier.size(22.dp))
                    }
                    Text("Réglages", color = c.sub, fontSize = 15.sp, modifier = Modifier.padding(start = 10.dp))
                }
                Text(sections[cat].title, color = c.text, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp, top = 6.dp))
                Text(sections[cat].sub, color = c.sub, fontSize = 13.sp, modifier = Modifier.padding(start = 6.dp, bottom = 16.dp))
                Column(Modifier.widthIn(max = 820.dp)) {
                    when (cat) {
                        0 -> MapLookSection(prefs, engine)
                        1 -> OnTheMapSection(prefs, engine)
                        2 -> ArrowSection(prefs, engine)
                        3 -> RoutesSection(prefs)
                        4 -> GuidanceSection(prefs, engine)
                        5 -> AlertsSection(prefs, engine)
                        6 -> SoundSection(prefs, engine)
                        7 -> { PlacesSection(engine); PhoneLinkGroup(prefs); SharingGroups(prefs) }
                        8 -> FuelSection(prefs, engine)
                        9 -> { OfflineMapsContent(prefs, engine); OfflinePlacesGroup(engine) }
                        10 -> ScreenSection(prefs, engine)
                        11 -> AndroidAutoSection(prefs)
                        12 -> PerformanceSection(prefs)
                        13 -> KeysSection(prefs, engine)
                        else -> AboutSection(prefs, engine)
                    }
                }
            }
          }
        }
    }
}

// ------------------------------------------------------------------ Map look

@Composable
private fun MapLookSection(prefs: Prefs, engine: NavEngine) {
    SGroup("Jour et nuit") {
        SChoice("Jour et nuit", listOf("auto" to "Auto", "lights" to "Phares", "sun" to "Soleil", "times" to "Mes horaires", "day" to "Jour", "night" to "Nuit"), prefs.s(PreferenceKeys.THEME, "auto"),
            "Auto suit le mode nuit de l'autoradio dès qu'il l'a activé, et le lever et coucher du soleil en attendant. Phares suit le mode nuit de l'autoradio quand il bascule avec vos feux (activez son option de mode nuit liée aux phares ou à l'éclairage), avec les heures du soleil en attendant. Soleil bascule au lever et au coucher du soleil. Mes horaires bascule aux heures que vous choisissez") { prefs.put(PreferenceKeys.THEME, it) }
        if (prefs.s(PreferenceKeys.THEME, "auto") == "times") {
            SFieldItem("La nuit commence à", prefs.s(PreferenceKeys.NIGHT_FROM, "19:00"), "Format 24 h, par exemple 19:30", placeholder = "19:00") { prefs.put(PreferenceKeys.NIGHT_FROM, it.trim()) }
            SFieldItem("Le jour commence à", prefs.s(PreferenceKeys.DAY_FROM, "07:00"), "Format 24 h, par exemple 06:45", placeholder = "07:00") { prefs.put(PreferenceKeys.DAY_FROM, it.trim()) }
        }
    }
    SGroup("La carte") {
        SChoice("Source de la carte", listOf("mapbox" to "Mapbox", "tomtom" to "TomTom", "osm" to "OpenStreetMap"), prefs.mapSource(),
            "Mapbox : la carte 3D. TomTom : la carte TomTom aux couleurs Horizon (clé TomTom nécessaire). Toutes demandent le jeton Mapbox, car c'est le moteur Mapbox qui les dessine") { prefs.put(PreferenceKeys.MAP_STYLE, it) }
        if (prefs.mapSource() == "mapbox") {
            SChoice("Niveau de détail", listOf("full" to "3D complète", "lite" to "Allégée", "lightest" to "Minimale"), prefs.mapDetail(),
                "3D complète : bâtiments, monuments, arbres et ombres. Allégée : bâtiments plats, sans monuments ni ombres. Minimale : la carte 2D simplifiée de Mapbox, pour les autoradios lents") { prefs.put(PreferenceKeys.MAP_DETAIL, it) }
            MapColoursPicker(prefs.s(PreferenceKeys.MAP_COLOURS, "mapbox")) { prefs.put(PreferenceKeys.MAP_COLOURS, it) }
        }
        DefaultZoomRow(prefs)
        SToggle(prefs, "Chemins et sentiers", "paths", true, "Désactivez pour une carte de conduite plus épurée")
        SToggle(prefs, "Panneaux en verre", "glass", true, "Cartes translucides teintées par la carte. Désactivé : cartes opaques")
        SChoice("Position du logo Mapbox", listOf("tl" to "En haut à gauche", "tr" to "En haut à droite", "bl" to "En bas à gauche", "br" to "En bas à droite"), prefs.s(PreferenceKeys.MAPBOX_LOGO, "tr"),
            "Mapbox impose son logo sur la carte ; choisissez le coin qui vous convient") { prefs.put(PreferenceKeys.MAPBOX_LOGO, it) }
    }
    SGroup("3D et satellite") {
        SToggle(prefs, "Satellite", "satellite", false)
        SToggle(prefs, "Bâtiments 3D", "buildings3d", true)
        SToggle(prefs, "Façades détaillées", "facades", true, "Fenêtres, portes et vitrines sur les bâtiments, là où Mapbox les a (surtout en centre-ville)")
        SToggle(prefs, "Monuments 3D", "landmarks3d", true, "Modèles détaillés de bâtiments célèbres, ponts et stades")
        SToggle(prefs, "Relief 3D", "terrain", false, "Le vrai relief. Superbe en région vallonnée, mais plus lourd pour un autoradio lent")
        SToggle(prefs, "Brume et ciel", "skyweather", true, "Brume à l'horizon selon la météo : grise par temps couvert, plus dense sous la pluie, épaisse dans le brouillard")
        SToggle(prefs, "Ciel nocturne en 3D", "nightsky", true, "Étoiles scintillantes au-dessus de l'horizon la nuit")
        SToggle(prefs, "Arbres dans les parcs", "trees", true, "Les arbres 3D de Mapbox dans les parcs et le long des rues")
        SToggle(prefs, "Dégager la route devant", "clearview", true, "Les arbres et monuments 3D de Mapbox sont masqués autour de votre flèche et, en itinéraire, au milieu de la route juste devant, pour ne jamais la cacher")
        SToggle(prefs, "Toucher pour mettre en évidence", "tapselect", true, "Touchez un bâtiment pour l'éclairer ; touchez un commerce ou un lieu, puis appui long pour y aller")
    }
}

// ------------------------------------------------------------------ On the map

@Composable
private fun OnTheMapSection(prefs: Prefs, engine: NavEngine) {
    SGroup("Trafic") {
        SToggle(prefs, "Trafic en direct", "traffic", true)
        SToggle(prefs, "Vue du trafic à l'arrêt", PreferenceKeys.STOPPED_OVERVIEW, true, "Après 30 secondes d'arrêt (feu, file), la carte recule pour montrer le trafic autour de vous vu de dessus, et revient quand vous repartez")
        SToggle(prefs, "Trafic uniquement sur l'itinéraire pendant le guidage", "routetrafficonly", false, "Désactivé : le trafic des routes autour de vous s'affiche aussi, comme dans Google Maps")
        SToggle(prefs, "Trafic fluide en vert", PreferenceKeys.TRAFFIC_GREEN, false, "Grands axes fluides en vert, en plus du trafic ralenti en orange et rouge")
    }
    SGroup("Sur votre itinéraire") {
        SToggle(prefs, "Feux tricolores", "signals", true, if (prefs.b(PreferenceKeys.SIGNALS, true)) "Seulement ceux de votre itinéraire. Actuellement : " + engine.map.signalInfo() else null)
        SToggle(prefs, "Radars et travaux", "hazardicons", true)
        SToggle(prefs, "Passages piétons", "zebras", true, "Passages piétons de votre itinéraire peints sur la route, avec balises lumineuses (de près)")
        SToggle(prefs, "Panneaux stop", PreferenceKeys.STOP_SIGNS, true, "Stops de votre itinéraire affichés sur la carte, avec un petit signal sonore avant chacun (là où OpenStreetMap les connaît)")
        SToggle(prefs, "Caméras de lecture de plaques", PreferenceKeys.ALPR_CAMS, true, "Caméras Flock et autres lecteurs automatiques de plaques recensés sur OpenStreetMap. Affichées sur la carte seulement, sans alerte")
    }
    SGroup("Mobilier urbain 3D") {
        SToggle(prefs, "Lampadaires, feux et radars en 3D", "furniture3d", true, "Modèles 3D grandeur nature avec lampes et feux allumés (3D complète). Désactivé : badges et blocs simplifiés")
        SToggle(prefs, "Lampadaires", "lamps", true, if (prefs.b(PreferenceKeys.LAMPS, true)) "Actuellement : " + engine.map.lampInfo() else null)
        if (prefs.b(PreferenceKeys.LAMPS, true)) SSlider("Espacement des lampadaires", prefs.s(PreferenceKeys.LAMPGAP, "40").toFloatOrNull() ?: 40f, 15f..120f, " m", reset = 40f) { prefs.put(PreferenceKeys.LAMPGAP, it.toString()) }
    }
    SGroup("Météo") {
        SToggle(prefs, "Effets météo sur la carte", "rainfx", false, "Pluie, neige (et brouillard) sur la carte selon la météo là où vous êtes")
        SToggle(prefs, "Calque radar de pluie", "radar", false, "Radar en direct de RainViewer, sous l'itinéraire")
    }
}

// ------------------------------------------------------------------ Your arrow & route line

@Composable
private fun ArrowSection(prefs: Prefs, engine: NavEngine) {
    SGroup("Votre flèche", footer = "Vrai modèle 3D : dessiné dans la carte elle-même, éclairé et ombré comme les bâtiments (3D complète uniquement ; sinon l'image de la même forme est utilisée). La flèche 3D et la voiture sont des images de formes 3D. La flèche plate est la plus légère sur un appareil lent.") {
        val loc = prefs.s(PreferenceKeys.LOCATOR, "arrow")
        val ctx = LocalContext.current
        val hasPic = prefs.s(PreferenceKeys.LOCATOR_IMAGE, "").isNotBlank() && java.io.File(ctx.filesDir, "locator.png").exists()
        val pickPic = rememberLocatorImagePicker { ok ->
            if (ok) { prefs.put(PreferenceKeys.LOCATOR_IMAGE, System.currentTimeMillis().toString()); prefs.put(PreferenceKeys.LOCATOR, "custom") }
            else android.widget.Toast.makeText(ctx, "Impossible de lire cette image (essayez un PNG ou un JPG)", android.widget.Toast.LENGTH_SHORT).show()
        }
        fun choosePic() { if (!pickPic()) android.widget.Toast.makeText(ctx, "Aucune appli de cet appareil ne permet de choisir une image (installez un gestionnaire de fichiers ou une galerie)", android.widget.Toast.LENGTH_LONG).show() }
        SChoice("Repère de position", listOf("arrow" to "Flèche 3D", "car" to "Voiture", "model" to "Vrai modèle 3D", "flat" to "Flèche plate", "custom" to "Votre image"), loc) {
            if (it == "custom" && !hasPic) choosePic() else prefs.put(PreferenceKeys.LOCATOR, it)
        }
        if (loc == "model") {
            SChoice("Forme du modèle", listOf("arrow" to "Flèche", "car" to "Voiture"), prefs.s(PreferenceKeys.MODEL_SHAPE, "arrow")) { prefs.put(PreferenceKeys.MODEL_SHAPE, it) }
            SToggle(prefs, "Halo", PreferenceKeys.MODEL_GLOW, true, "Un léger halo bleu autour du modèle, et le modèle bien éclairé")
            if (prefs.mapDetail() != "full") SNote("Le vrai modèle 3D s'affiche avec Niveau de détail : 3D complète. En Allégée et Minimale, l'image de la même forme est utilisée.")
        }
        val shapeCar = loc == "car" || (loc == "model" && prefs.s(PreferenceKeys.MODEL_SHAPE, "arrow") == "car")
        val shapeArrow = loc == "arrow" || (loc == "model" && !shapeCar)
        if (loc == "custom") {
            SItem("Image", "Un PNG à fond transparent rend le mieux. Orientez son haut dans le sens de la marche") { SButton(if (hasPic) "Changer" else "Choisir") { choosePic() } }
            SToggle(prefs, "Tourne avec vous", PreferenceKeys.LOCATOR_IMAGE_TURNS, true, "À plat sur la route et tourne avec vous, comme la flèche. Désactivé : reste droite")
            SToggle(prefs, "Recadrage rond", PreferenceKeys.LOCATOR_IMAGE_CIRCLE, false, "La découpe en cercle avec un bord blanc (idéal pour les photos)")
        }
        // the car's shape: for the Car picture and the real 3D car alike
        if (shapeCar) {
            val cur = prefs.s(PreferenceKeys.CAR_MODEL, "saloon")
            val colour = prefs.s(PreferenceKeys.CAR_COLOUR, "#F4F5F7")
            val names = listOf("saloon" to "Berline", "hatch" to "Compacte", "estate" to "Break", "suv" to "SUV", "pickup" to "Pick-up", "van" to "Utilitaire", "sports" to "Sportive")
            SItem("Type de voiture", names.firstOrNull { it.first == cur }?.second)
            androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for ((id, name) in names) {
                    val pic = remember(id, colour) { try { engine.map.carPreview(id, colour).asImageBitmap() } catch (e: Throwable) { null } }
                    val sel = id == cur
                    androidx.compose.foundation.layout.Column(
                        Modifier.width(92.dp).clip(RoundedCornerShape(16.dp)).background(if (sel) LocalVw.current.accent.copy(alpha = 0.22f) else LocalVw.current.tile)
                            .border(if (sel) 2.dp else 0.dp, if (sel) LocalVw.current.accent else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(16.dp))
                            .clickable { prefs.put(PreferenceKeys.CAR_MODEL, id) }.padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (pic != null) androidx.compose.foundation.Image(pic, name, Modifier.size(80.dp))
                        Text(name, color = LocalVw.current.text, fontSize = 12.sp)
                    }
                }
            }
        }
        if (shapeCar) SColours("Couleur de la voiture", listOf(
            "#F4F5F7" to "Blanc", "#B9BEC8" to "Argent", "#7A808C" to "Gris", "#3E434D" to "Graphite", "#2A2D34" to "Noir", "#1E2A4A" to "Bleu marine",
            "#2F6BDF" to "Bleu", "#5FB3E8" to "Bleu ciel", "#1FA39A" to "Bleu canard", "#2E8B57" to "Vert", "#6E8B3D" to "Olive", "#F2C230" to "Jaune",
            "#F07C28" to "Orange", "#D7263D" to "Rouge", "#8E1B2E" to "Bordeaux", "#7B4FC9" to "Violet", "#E86FA8" to "Rose", "#C9A27A" to "Champagne", "#6B4A35" to "Marron",
        ), prefs.s(PreferenceKeys.CAR_COLOUR, "#F4F5F7")) { prefs.put(PreferenceKeys.CAR_COLOUR, it) }
        else if (shapeArrow) SColours("Couleur de la flèche", listOf(
            "#F2F2F6" to "Blanc", "#C2C6D0" to "Argent", "#8A909C" to "Gris", "#3E434D" to "Graphite", "#4C8DFF" to "Bleu", "#6FD0FF" to "Bleu ciel",
            "#1FC7A6" to "Bleu canard", "#3DDC84" to "Vert", "#B5E34A" to "Vert citron", "#FFD54A" to "Jaune", "#FF9A3C" to "Orange", "#FF4D5E" to "Rouge",
            "#FF6FB5" to "Rose", "#A56BFF" to "Violet",
        ), prefs.s(PreferenceKeys.ARROW_COLOUR, "#F2F2F6")) { prefs.put(PreferenceKeys.ARROW_COLOUR, it) }
        SSlider(if (shapeCar) "Taille de la voiture" else if (loc == "custom") "Taille de l'image" else "Taille de la flèche", prefs.s(PreferenceKeys.ARROW_PERCENT, "240").toFloatOrNull() ?: 240f, 100f..500f, "%", reset = 240f) { prefs.put(PreferenceKeys.ARROW_PERCENT, it.toString()) }
    }
    SGroup("Tracé de l'itinéraire") {
        SToggle(prefs, "Flèche de direction sur le tracé", "turnarrows", true, "Une flèche blanche à travers le prochain carrefour ou rond-point")
        SToggle(prefs, "Chevrons le long du tracé", "routechevrons", true, "Petites flèches le long du tracé en vue rapprochée")
        SToggle(prefs, "Ruban d'itinéraire", "ribbon", true, "Une ombre douce sous le tracé, et l'itinéraire visible en transparence à travers les bâtiments 3D devant")
        SToggle(prefs, "Lumière animée le long du tracé", "routeflow", true)
        SColours("Couleur des itinéraires alternatifs", listOf(
            "#C4D9E6" to "Gris-bleu pâle (par défaut)", "#A9D4F0" to "Bleu clair", "#5AB7F5" to "Bleu", "#315CBA" to "Bleu foncé",
            "#A9B4C2" to "Gris argent", "#FFFFFF" to "Blanc", "#42CFA5" to "Menthe", "#F4C542" to "Jaune",
            "#FF9A4A" to "Orange", "#F05D6C" to "Rouge", "#C58AF9" to "Violet", "#F08AC5" to "Rose",
        ), prefs.s(PreferenceKeys.ALT_ROUTE_COLOUR, "#C4D9E6")) { prefs.put(PreferenceKeys.ALT_ROUTE_COLOUR, it) }
    }
}

// ------------------------------------------------------------------ Routes

@Composable
private fun RoutesSection(prefs: Prefs) {
    SGroup("À éviter", footer = "Les changements s'appliquent au prochain itinéraire calculé. « Éviter cette route » sur la carte ne vaut que pour ce trajet.") {
        SToggle(prefs, "Éviter les péages", "avoid_toll", false, if (prefs.b(PreferenceKeys.AVOID_TOLL, false)) "Partout" else null)
        // ...or only in some countries: a route that would use a toll road in one of them is planned without toll roads
        if (!prefs.b(PreferenceKeys.AVOID_TOLL, false)) {
            val chosen = prefs.s(PreferenceKeys.AVOID_TOLL_COUNTRIES, "").split(',').filter { it.isNotBlank() }.toSet()
            SItem("Éviter les péages seulement en", if (chosen.isEmpty()) "Choisissez des pays : un itinéraire qui y passerait par un péage est calculé sans péage" else "Un itinéraire qui passerait par un péage dans l'un de ces pays est calculé sans péage")
            androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((cc, name) in listOf("FR" to "France", "IT" to "Italie", "ES" to "Espagne", "PT" to "Portugal", "AT" to "Autriche", "CH" to "Suisse",
                    "SI" to "Slovénie", "HR" to "Croatie", "PL" to "Pologne", "CZ" to "Tchéquie", "HU" to "Hongrie", "GR" to "Grèce", "IE" to "Irlande", "NO" to "Norvège", "US" to "USA")) {
                    Pill(name, cc in chosen, { prefs.put(PreferenceKeys.AVOID_TOLL_COUNTRIES, (if (cc in chosen) chosen - cc else chosen + cc).joinToString(",")) })
                }
            }
        }
        SToggle(prefs, "Éviter les autoroutes", "avoid_motorway", false)
        SToggle(prefs, "Éviter les ferries", "avoid_ferry", false)
        SToggle(prefs, "Éviter les routes non goudronnées", "avoid_unpaved", true)
        SToggle(prefs, "Éviter les ZFE (zones à faibles émissions)", "avoidzones", false, "Une zone où vous partez ou arrivez ne peut pas être évitée")
        ZonesItem()
    }
    SGroup("Choix de l'itinéraire") {
        SToggle(prefs, "Itinéraires éco", "route_eco", false, "Économes en carburant plutôt que les plus rapides")
        SToggle(prefs, "Proposer des itinéraires plus rapides", "fasterroute", true)
        if (prefs.b(PreferenceKeys.FASTER_ROUTE, true)) SToggle(prefs, "Garder les autres itinéraires sur la carte", "livealts", true, "Lignes grises avec +/− minutes, à toucher à tout moment. Désactivé : une carte « Itinéraire plus rapide trouvé » à la place")
        SToggle(prefs, "Deviner votre destination", "predict", true, "Avec le temps de trajet en direct, y compris domicile et travail à vos heures habituelles")
        SToggle(prefs, "Météo sur l'itinéraire", "routewx", true)
    }
    SGroup("Démarrer un itinéraire") {
        SToggle(prefs, "Démarrage auto après compte à rebours", PreferenceKeys.AUTO_START_ROUTE, true, "Lance le guidage automatiquement à la fin du compte à rebours")
        SSlider("Durée du compte à rebours", prefs.i(PreferenceKeys.AUTO_START_SECONDS, 10).coerceIn(3, 30).toFloat(), 3f..30f, " s", sub = "Délai avant le début du guidage", steps = 26, reset = 10f) { prefs.put(PreferenceKeys.AUTO_START_SECONDS, it.coerceIn(3, 30)) }
        SToggle(prefs, "Survoler un nouvel itinéraire", "cinematic", true, "Un survol 3D de 3 secondes avant la vue d'ensemble. Touchez la carte pour passer")
        SToggle(prefs, "Bouton de survol de l'itinéraire", "flyover", true, "Dans les options d'itinéraire : un survol 3D de tout le trajet avant de partir")
    }
}

// ------------------------------------------------------------------ Turn card & lanes

@Composable
private fun GuidanceSection(prefs: Prefs, engine: NavEngine) {
    val mi = engine.imperial
    SGroup("Carte de direction") {
        SToggle(prefs, "Panneaux de direction sur la carte", "uksigns", true, "Aux sorties : numéro de sortie, routes et villes, comme sur les panneaux (style français, britannique au Royaume-Uni)")
        SToggle(prefs, "Carte de direction animée", "livecard", true, "Grandit et s'illumine à l'approche du virage")
        SToggle(prefs, "Guidage par voie sur la carte de direction", "lanestrip", true)
        if (prefs.b(PreferenceKeys.LANE_STRIP, true)) SChoice("Style du guidage par voie", listOf("hero" to "Grande vue des voies", "classic" to "Bandeau classique"), prefs.s(PreferenceKeys.LANE_STYLE, "hero"),
            if (prefs.s(PreferenceKeys.LANE_STYLE, "hero") == "hero") "Avant les carrefours à deux voies ou plus, la carte de direction affiche un grand schéma des voies avec la vôtre en surbrillance" else "Le petit bandeau de voies à chaque carrefour") { prefs.put(PreferenceKeys.LANE_STYLE, it) }
        SChoice("Direction suivante sur la carte", listOf("close" to "Si proche", "always" to "Toujours", "off" to "Désactivé"), prefs.s(PreferenceKeys.THEN_MODE, "close"),
            when (prefs.s(PreferenceKeys.THEN_MODE, "close")) { "close" -> "Seulement quand la direction suivante arrive juste après (dans les 15 secondes environ)"; "always" -> "La direction d'après, tout le temps"; else -> null }) { prefs.put(PreferenceKeys.THEN_MODE, it) }
        if (prefs.s(PreferenceKeys.THEN_MODE, "close") != "off") SChoice("Affichée comme", listOf("row" to "Une ligne sous la carte", "icon" to "Une petite flèche"), prefs.s(PreferenceKeys.THEN_STYLE, "row")) { prefs.put(PreferenceKeys.THEN_STYLE, it) }
        SToggle(prefs, "Direction suivante dans la notification", PreferenceKeys.TURN_NOTIF, true, "La flèche, la distance et la route dans la notification d'Horizon Nav pendant le guidage, pour les lanceurs de voiture, les montres et les écrans qui affichent les notifications de navigation")
    }
    SGroup("Voies et carrefours") {
        SToggle(prefs, "Voies peintes sur la carte", "lanemap", true)
        SToggle(prefs, "Zoom sur les voies avant un carrefour", "laneview", true)
        SToggle(prefs, "Vue rapprochée des échangeurs", "junctionview", true, "Aux bretelles, sorties d'autoroute, embranchements et séparations de voies, la vue se rapproche et s'aplatit un peu sur les 200 derniers mètres")
        SToggle(prefs, "Passer la carte en 2D dans les virages", "turn2d", true)
        SToggle(prefs, "Inclinaison selon la vitesse", "speedtilt", true, "Plus à plat à basse vitesse, vue plus lointaine à haute vitesse")
    }
    SGroup("Carte de trajet") {
        SToggle(prefs, "Barre d'itinéraire sur la carte de trajet", "routebar", true, "Trafic, radars, aires et étapes, avec votre progression")
        SToggle(prefs, "Carte de la prochaine aire", "servicescard", true)
        SToggle(prefs, "Radar des dangers", "hazardradar", false, (if (mi) "Les 2 prochains miles" else "Les 3 prochains km") + " : virages, passages, écoles, ralentisseurs, radars et contrôles")
    }
    SGroup("Arrivée") {
        SToggle(prefs, "Moment d'arrivée", "arrivalfx", true, "La caméra plonge vers votre destination et montre le parking le plus proche")
        SToggle(prefs, "Parking près de la destination", "parkprompt", true, (if (mi) "Environ un mile" else "Environ 1,5 km") + " avant l'arrivée, les parkings proches avec le temps de marche depuis chacun")
        SToggle(prefs, "Itinéraire retour", "routeback", true, "Sur la carte d'arrivée : un seul appui pour revenir à votre point de départ")
    }
}


/** What the app knows about clean air zones, with a button to download them again. */
@Composable
private fun ZonesItem() {
    val ctx = LocalContext.current
    val sc = rememberCoroutineScope()
    var st by remember { mutableStateOf(Zones.all(ctx).let { z -> if (z.isEmpty()) "Pas encore téléchargé" else "${z.size} zones" + z.map { it.name }.filter { it != "Zone à faibles émissions" }.distinct().takeIf { it.isNotEmpty() }?.let { n -> " : " + n.joinToString(", ").take(140) }.orEmpty() }) }
    var busy by remember { mutableStateOf(false) }
    SItem("Zones à faibles émissions sur cet appareil", st) {
        SButton(if (busy) "Mise à jour…" else "Mettre à jour") {
            if (!busy) { busy = true; sc.launch { withContext(Dispatchers.IO) { Zones.fetch(ctx) }; st = Zones.status; busy = false } }
        }
    }
}

// ------------------------------------------------------------------ Alerts

@Composable
private fun AlertsSection(prefs: Prefs, engine: NavEngine) {
    SGroup("Vitesse") {
        SToggle(prefs, "Avertir en cas d'excès de vitesse", "speedwarn", true)
        if (prefs.b(PreferenceKeys.SPEEDWARN, true)) SChoice("Avertir au-delà de", listOf("0", "5", "10").map { it to it + if (engine.imperial) " mph" else " km/h" }, prefs.s(PreferenceKeys.TRAFFIC_WARNING, "5")) { prefs.put(PreferenceKeys.TRAFFIC_WARNING, it) }
        SToggle(prefs, "Annoncer les changements de limitation", "limitchange", true)
        SToggle(prefs, "Afficher les limitations estimées", "limitest", false, "Sans limitation connue, affiche la limitation habituelle pour ce type de route (grisée). Désactivé : aucun panneau plutôt qu'une estimation")
        SToggle(prefs, "Radars", "cameras", true)
        if (prefs.b(PreferenceKeys.CAMERA_WARNINGS, true)) SToggle(prefs, "Désactivés là où c'est interdit", "cambanobey", true, "La France, l'Allemagne et la Suisse interdisent les alertes radar : elles s'y coupent d'elles-mêmes")
        SToggle(prefs, "Radars tronçons", "avgzones", true, "Affiche votre moyenne par rapport à la limite")
    }
    SGroup("Sur la route", footer = "Radars tronçons, ralentisseurs, écoles et ZFE viennent d'OpenStreetMap : certains peuvent manquer.") {
        SToggle(prefs, "Travaux et incidents", "incidents", true)
        SToggle(prefs, "Alerte contresens", "wrongway", true, "Si vous vous engagez à contresens dans une rue à sens unique")
        SToggle(prefs, "Ralentisseurs", "bumps", true)
        SToggle(prefs, "Écoles aux heures d'entrée et de sortie", "schools", true)
        SToggle(prefs, "Zones à faibles émissions (ZFE)", "zonewarn", true)
        SToggle(prefs, "Rappel de pause après 2 heures", "breakremind", true)
        SToggle(prefs, "Passages à niveau et gués", "railford", true, "Un panneau et une alerte vocale pour les passages à niveau et les gués de votre itinéraire")
        SToggle(prefs, "Conseils météo", "wxadvice", true, "Une discrète remarque par forte pluie, neige, brouillard ou verglas quand vous êtes proche de la limite")
    }
    SGroup("Vos signalements", footer = "Faites un signalement depuis le bouton d'options de la carte. Les signalements restent sur cet appareil et disparaissent après quelques heures.") {
        SToggle(prefs, "Avertir des signalements devant", "policealerts", true)
        SItem("Signalements encore actifs", "${engine.reportsActive().size}") { SButton("Effacer", danger = true) { engine.clearReports() } }
    }
}

// ------------------------------------------------------------------ Sound & voice

@Composable
private fun SoundSection(prefs: Prefs, engine: NavEngine) {
    val ctx = LocalContext.current
    SGroup("Son") {
        SChoice("Guidage vocal", listOf("0" to "Tout", "1" to "Alertes seules", "2" to "Désactivé"), engine.soundMode.toString(),
            "Alertes seules : pas d'instructions de direction, mais radars, limitations et dangers restent annoncés") { prefs.put(PreferenceKeys.SOUND_MODE, it.toInt()) }
        SToggle(prefs, "Annoncer les noms de rues", PreferenceKeys.VOICE_ROADS, true, "Désactivé : seulement la direction, « Dans 200 mètres, tournez à gauche », sans la rue ni les villes des panneaux")
        SToggle(prefs, "Instructions plus intelligentes", "smartvoice", true, "Numéros de sortie et directions des panneaux, la manœuvre suivante quand elle suit de près, et moins d'annonces anticipées sur vos routes habituelles")
        SChoice("Canal audio de la voix", listOf("nav" to "Navigation", "media" to "Média"), prefs.s(PreferenceKeys.VOICE_CHANNEL, "nav"),
            "Si vous entendez les bips mais pas la voix, choisissez Média : certains autoradios ne jouent pas le canal navigation") { prefs.put(PreferenceKeys.VOICE_CHANNEL, it) }
        SItem("Voix et langue", "Utilise la synthèse vocale du système (installez la voix française)") { SButton("Ouvrir") { openSystemSettings(ctx, "com.android.settings.TTS_SETTINGS") } }
        SItem("Volume") { SButton("Ouvrir") { openSystemSettings(ctx, Settings.ACTION_SOUND_SETTINGS) } }
    }
}


// ------------------------------------------------------------------ Places & trips

@Composable
private fun PlaceItem(label: String, p: Place?, onChange: () -> Unit, onClear: () -> Unit) {
    SItem(label, p?.let { it.name + (if (it.sub.isNotBlank()) ", " + it.sub else "") } ?: "Non défini") {
        SButton(if (p != null) "Changer" else "Définir", primary = true, onClick = onChange)
        if (p != null) SButton("Effacer", danger = true, onClick = onClear)
    }
}

@Composable
private fun PlacesSection(engine: NavEngine) {
    val pl = engine.places
    var settingSlot by remember { mutableStateOf<String?>(null) }
    // (read again whenever the saved places change, so a change shows straight away)
    val slotsRaw = engine.prefs.s(PreferenceKeys.PLACE_HOME, "") + engine.prefs.s("place_work", "")
    SGroup("Lieux enregistrés", footer = "Domicile et Travail se définissent aussi depuis les pastilles sous la barre de recherche, ou la maison et la mallette dans les options d'itinéraire.") {
        PlaceItem("Maison", remember(slotsRaw) { pl.slot("home") }, { settingSlot = "home" }) { pl.clearSlot("home") }
        PlaceItem("Travail", remember(slotsRaw) { pl.slot("work") }, { settingSlot = "work" }) { pl.clearSlot("work") }
    }
    settingSlot?.let { s -> AddPlaceDialog(engine, null, slot = s) { settingSlot = null } }
    // your own labelled places: rename, reorder or remove
    val raw = engine.prefs.s(PreferenceKeys.PLACES_CUSTOM, "")
    val mine = remember(raw) { pl.customs() }
    var renaming by remember { mutableStateOf<String?>(null) }
    var adding by remember { mutableStateOf(false) }
    SGroup("Vos lieux", footer = "Affichés en pastilles sous la barre de recherche, après Domicile et Travail. Ajoutez-en un avec la pastille +, ou le signet dans les options d'itinéraire.") {
        if (mine.isEmpty()) SNote("Aucun lieu personnel pour l'instant.")
        mine.forEachIndexed { i, lp ->
            if (renaming == lp.label) {
                var t by remember(lp.label) { mutableStateOf(lp.label) }
                SItem("Renommer ${lp.label}") {
                    SField(t, { t = it.take(24) }, width = 220.dp)
                    SButton("Enregistrer", primary = true) { pl.renameCustom(lp.label, t); renaming = null }
                }
            } else SItem(lp.label, lp.place.name + if (lp.place.sub.isNotBlank()) ", " + lp.place.sub else "") {
                if (i > 0) SButton("↑") { pl.moveCustom(lp.label, -1) }
                SButton("Renommer") { renaming = lp.label }
                SButton("Retirer", danger = true) { pl.removeCustom(lp.label) }
            }
        }
        SItem("Ajouter un lieu") { SButton("Ajouter", primary = true) { adding = true } }
    }
    if (adding) AddPlaceDialog(engine, null) { adding = false }
    val favRaw = engine.prefs.s(PreferenceKeys.PLACES_FAVOURITES, "")
    val favs = remember(favRaw) { pl.favourites() }
    var renamingFav by remember { mutableStateOf<Place?>(null) }
    var slotFor by remember { mutableStateOf<Place?>(null) }
    SGroup("Favoris") {
        if (favs.isEmpty()) SNote("Aucun favori pour l'instant. Touchez l'étoile d'une destination pour l'ajouter.")
        favs.forEach { f ->
            when {
                renamingFav == f -> {
                    var t by remember(f) { mutableStateOf(f.name) }
                    SItem("Renommer ${f.name}") {
                        SField(t, { t = it.take(40) }, width = 220.dp)
                        SButton("Enregistrer", primary = true) { pl.renameFavourite(f, t); renamingFav = null }
                    }
                }
                slotFor == f -> SItem("Définir ${f.name} comme…") {
                    SButton("Maison", primary = true) { pl.setSlot("home", f); slotFor = null; engine.toast("${f.name} est maintenant le domicile", 2500) }
                    SButton("Travail", primary = true) { pl.setSlot("work", f); slotFor = null; engine.toast("${f.name} est maintenant le travail", 2500) }
                    SButton("Annuler") { slotFor = null }
                }
                else -> SItem(f.name, f.sub) {
                    SButton("Renommer") { renamingFav = f }
                    SButton("Définir Domicile / Travail") { slotFor = f }
                    SButton("Retirer", danger = true) { pl.toggleFavourite(f) }
                }
            }
        }
        if (favs.size > 1) SItem("Supprimer tous les favoris") { SButton("Tout supprimer", danger = true) { pl.clearFavourites() } }
    }
    SGroup("Historique", footer = "Faites glisser un lieu sur le côté dans la liste Enregistrés et récents pour le retirer.") {
        SToggle(engine.prefs, "Enregistrés et récents sous la barre de recherche", "quicklist", true, "La liste des favoris et lieux récents sur la carte")
        SItem("Destinations récentes", "${pl.recents().size} récentes, plus les lieux mémorisés pour chercher sans réseau") {
            SButton("Effacer", danger = true) { pl.clearHistory(); engine.toast("Historique effacé", 2000) }
        }
    }
    TripsGroup(engine)
}

@Composable
private fun TripsGroup(engine: NavEngine) {
    var shown by remember { mutableStateOf(engine.trips.all()) }
    val fmt = com.headunit.launcher.Clock.format("EEE d MMM, ")
    val k = if (engine.imperial) 2.23694 else 3.6
    val unit = if (engine.imperial) "mph" else "km/h"
    SGroup("Journal des trajets") {
        if (shown.isEmpty()) SNote("Aucun trajet pour l'instant. Chaque trajet est enregistré ici quand vous vous garez.")
        else SItem("${shown.size} trajets", Geo.fmtDist(shown.sumOf { it.dist }, engine.imperial) + " au total") { SButton("Effacer", danger = true) { engine.trips.clear(); shown = emptyList() } }
        shown.take(50).forEach { t ->
            val mins = ((t.end - t.start) / 60000L).toInt()
            SItem(
                fmt.format(java.util.Date(t.start)) + (if (t.to.isNotBlank()) " → ${t.to}" else ""),
                "${Geo.fmtDur(mins * 60.0)} · en mouvement ${Geo.fmtDur(t.moving.toDouble())} · max ${Math.round(t.maxSpeed * k)} $unit",
            ) { Text(Geo.fmtDist(t.dist, engine.imperial), color = LocalVw.current.text, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        }
    }
}


/** Share your ETA and places sent from other apps (part of Places & trips). */
/** Your own normal zoom when not on a route: a slider from further out to closer, and Reset. */
@Composable
private fun DefaultZoomRow(prefs: Prefs) {
    val c = LocalVw.current
    val saved = (prefs.s(PreferenceKeys.DEFAULT_ZOOM, "0").toFloatOrNull() ?: 0f).coerceIn(-2f, 2f)
    var v by remember(saved) { mutableStateOf(saved) }
    fun save(x: Float) = prefs.put(PreferenceKeys.DEFAULT_ZOOM, String.format(java.util.Locale.US, "%.2f", x))
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        SItem("Zoom par défaut", "Hors itinéraire. Le zoom auto recule toujours à vitesse élevée et se rapproche aux carrefours, autour de votre réglage. Ou zoomez avec + et − sur la carte et touchez « En faire mon zoom par défaut »") {
            if (kotlin.math.abs(v) > 0.01f) SButton("Réinitialiser") { v = 0f; save(0f) }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Plus loin", color = c.sub, fontSize = 13.sp)
            androidx.compose.material3.Slider(value = v, onValueChange = { v = (Math.round(it * 4f) / 4f) }, onValueChangeFinished = { save(v) },
                valueRange = -2f..2f, modifier = Modifier.weight(1f).padding(horizontal = 10.dp))
            Text("Plus près", color = c.sub, fontSize = 13.sp)
        }
    }
}

/**
 * Sending places from the phone to the head unit (through a free relay, encrypted with the pairing code). The head unit makes the
 * code; you type it on the phone.
 */
@Composable
private fun PhoneLinkGroup(prefs: Prefs) {
    val code = prefs.s(PreferenceKeys.LINK_CODE, "")
    val role = prefs.s(PreferenceKeys.LINK_ROLE, "")
    var typing by remember { mutableStateOf(false) }
    SGroup("Envoyer des lieux depuis votre téléphone", footer = "Associez une seule fois : sur l'autoradio, touchez Associer un téléphone et saisissez son code dans Horizon Nav sur votre téléphone (au même endroit). Ensuite « Envoyer à la voiture » sur l'écran d'itinéraire du téléphone envoie le lieu à l'autoradio, qui le lance à son prochain allumage (sous 12 heures). Les lieux passent par ntfy.sh, un relais de messages gratuit, chiffrés avec votre code pour que seuls votre téléphone et votre voiture puissent les lire.") {
        when {
            code.isNotBlank() && role == "car" -> {
                SItem("Ceci est la voiture", "Associé. Code ${com.headunit.launcher.CarLink.pretty(code)} : saisissez-le sur un autre téléphone pour l'ajouter aussi") {
                    SButton("Dissocier", danger = true) { prefs.put(PreferenceKeys.LINK_CODE, ""); prefs.put(PreferenceKeys.LINK_ROLE, ""); prefs.put(PreferenceKeys.LINK_LAST, "") }
                }
                val qr = remember(code) { donateQr(com.headunit.launcher.CarLink.pretty(code), 300) }
                if (qr != null) Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.size(110.dp).clip(RoundedCornerShape(12.dp)).background(Color.White).padding(6.dp)) {
                        androidx.compose.foundation.Image(qr, "Le code d'association en QR code", Modifier.size(98.dp), filterQuality = androidx.compose.ui.graphics.FilterQuality.None)
                    }
                    Text(com.headunit.launcher.CarLink.pretty(code), color = LocalVw.current.text, fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                }
            }
            code.isNotBlank() && role == "phone" -> SItem("Ceci est votre téléphone", "Associé à votre voiture (${com.headunit.launcher.CarLink.pretty(code)}). « Envoyer à la voiture » se trouve sur l'écran d'itinéraire, à côté d'Enregistrer") {
                SButton("Dissocier", danger = true) { prefs.put(PreferenceKeys.LINK_CODE, ""); prefs.put(PreferenceKeys.LINK_ROLE, "") }
            }
            else -> {
                SItem("Sur l'autoradio", "Crée un code à saisir sur votre téléphone") {
                    SButton("Associer un téléphone", primary = true) { prefs.put(PreferenceKeys.LINK_CODE, com.headunit.launcher.CarLink.newCode()); prefs.put(PreferenceKeys.LINK_ROLE, "car"); prefs.put(PreferenceKeys.LINK_LAST, "") }
                }
                SItem("Sur votre téléphone", "Saisissez le code affiché par l'autoradio") { SButton("Saisir le code") { typing = true } }
                if (typing) SFieldItem("Code de la voiture", "", "Par exemple K7P-4QX-M2D", placeholder = "K7P-4QX-M2D") { t ->
                    val ok = com.headunit.launcher.CarLink.clean(t)
                    if (ok != null) { prefs.put(PreferenceKeys.LINK_CODE, ok); prefs.put(PreferenceKeys.LINK_ROLE, "phone"); typing = false }
                }
            }
        }
    }
}

@Composable
private fun SharingGroups(prefs: Prefs) {
    val ctx = LocalContext.current
    val pkg = android.net.Uri.parse("package:" + ctx.packageName)
    SGroup("Partager votre heure d'arrivée", footer = "Le bouton de partage de la carte de trajet envoie par SMS votre heure d'arrivée et un lien de carte. Sur un autoradio sans SIM, il ouvre plutôt votre messagerie.") {
        val pickEta = rememberEtaContactPicker(prefs)
        SItem("Envoyer à", prefs.s(PreferenceKeys.ETA_NAME, "").ifBlank { "Personne de choisi" }) {
            SButton(if (prefs.s(PreferenceKeys.ETA_NUMBER, "").isBlank()) "Choisir" else "Changer") { pickEta() }
            if (prefs.s(PreferenceKeys.ETA_NUMBER, "").isNotBlank()) SButton("Effacer", danger = true) { prefs.put(PreferenceKeys.ETA_NUMBER, ""); prefs.put(PreferenceKeys.ETA_NAME, "") }
        }
    }
    SGroup("Lieux depuis d'autres applis", footer = "Partagez un lieu vers Horizon Nav depuis Google Maps, WhatsApp, Contacts ou une page web et l'itinéraire est calculé. Pour ouvrir directement les liens Google Maps ici, touchez Ouvrir par défaut et ajoutez les liens.") {
        SItem("Liens Google Maps") {
            SButton("Ouvrir par défaut") {
                if (android.os.Build.VERSION.SDK_INT >= 31) openSystemSettings(ctx, Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS, pkg)
                else openSystemSettings(ctx, Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkg)
            }
        }
    }
}

// ------------------------------------------------------------------ Fuel & charging

@Composable
private fun FuelSection(prefs: Prefs, engine: NavEngine) {
    // (kept in the same units inside whichever are shown, miles per kWh and UK miles per gallon, so saved figures don't change)
    val mi = engine.imperial
    fun num(t: String) = t.filter { it.isDigit() || it == '.' }
    fun fmt1(v: Double) = if (v == Math.floor(v)) v.toLong().toString() else String.format(java.util.Locale.US, "%.1f", v)
    val ctx = LocalContext.current
    val sc = rememberCoroutineScope()
    // fork FR : les réglages britanniques (Fuel Finder) ne s'affichent qu'au Royaume-Uni ou s'ils sont déjà remplis
    val ukFuel = engine.detectedCountry.equals("GB", true) || prefs.s(PreferenceKeys.FFID, "").isNotBlank()
    val deFuel = engine.detectedCountry.equals("DE", true) || prefs.s(PreferenceKeys.TANKERKOENIG_KEY, "").isNotBlank()
    SGroup(if (ukFuel) "Prix des carburants" else "Carburant", footer = if (ukFuel) "Au Royaume-Uni, les prix en direct viennent du service gouvernemental Fuel Finder. C'est gratuit : sur gov.uk, cherchez « Fuel Finder », inscrivez-vous comme destinataire d'informations avec un GOV.UK One Login, puis collez ici l'identifiant et le secret client. Sans eux la liste des stations fonctionne, mais sans les prix. (En France, les prix sont automatiques : voir plus bas.)" else if (!deFuel) "Les prix en France et en Autriche s'affichent tout seuls. La clé Tankerkönig (prix en Allemagne) apparaît ici quand vous y êtes." else null) {
        SChoice("Votre carburant", listOf("E10" to "SP95-E10", "E5" to "SP98", "B7" to "Gazole", "E85" to "Superéthanol (E85)", "EV" to "Électrique"), prefs.s(PreferenceKeys.FUEL_TYPE, "E10")) { prefs.put(PreferenceKeys.FUEL_TYPE, it) }
        SToggle(prefs, "Proposer un plein sur les longs trajets", "smartfuel", true, if (prefs.s(PreferenceKeys.FUEL_TYPE, "E10") == "EV") (if (mi) "Au-delà d'environ 95 miles" else "Au-delà d'environ 150 km") + ", une borne sur le trajet avec le plus petit détour" else (if (mi) "Au-delà d'environ 95 miles" else "Au-delà d'environ 150 km") + ", la station la moins chère sur le trajet")
        if (ukFuel) {
            SFieldItem("Identifiant client", prefs.s(PreferenceKeys.FFID, "")) { prefs.put(PreferenceKeys.FFID, it.trim()) }
            SFieldItem("Secret client", prefs.s(PreferenceKeys.FFSECRET, ""), password = true) { prefs.put(PreferenceKeys.FFSECRET, it.trim()) }
            var st by remember { mutableStateOf(FuelFinder.status) }
            var busy by remember { mutableStateOf(false) }
            SItem("Prix", st.ifBlank { if (FuelFinder.configured(prefs)) "Pas encore téléchargé" else "Renseignez d'abord vos identifiants" }) {
                SButton(if (busy) "Mise à jour…" else "Mettre à jour maintenant", primary = true) {
                    if (!busy && FuelFinder.configured(prefs)) {
                        busy = true; st = "Téléchargement des stations et des prix (une minute la première fois)…"
                        sc.launch {
                            try { withContext(Dispatchers.IO) { FuelFinder.refresh(ctx, prefs, force = true) } } catch (e: Exception) { }
                            st = FuelFinder.status; busy = false
                        }
                    }
                }
            }
            SItem("Un prix erroné ?") { SButton("Le signaler") { openSystemSettings(ctx, android.content.Intent.ACTION_VIEW, android.net.Uri.parse(FuelFinder.REPORT_URL)) } }
        }
    }
    if (deFuel) SGroup(if (ukFuel) "Prix des carburants hors Royaume-Uni" else "Prix des carburants", footer = "La France et l'Autriche fonctionnent toutes seules, via les services de prix de leurs gouvernements. L'Allemagne demande une clé gratuite sur creativecommons.tankerkoenig.de (Tankerkönig). Ailleurs la liste des stations fonctionne, sans les prix.") {
        SFieldItem("Clé Tankerkönig (Allemagne)", prefs.s(PreferenceKeys.TANKERKOENIG_KEY, ""), placeholder = "Collez la clé", password = true) { prefs.put(PreferenceKeys.TANKERKOENIG_KEY, it.trim()) }
    }
    if (prefs.s(PreferenceKeys.FUEL_TYPE, "E10") == "EV") SGroup("Votre voiture électrique", footer = "Avec la consommation et le prix renseignés, chaque itinéraire indique à peu près ce que coûtera la recharge. La consommation et la capacité de batterie servent aussi à l'estimation de batterie sur la carte de trajet et aux suggestions d'arrêt recharge (réglez votre niveau de batterie dans les options d'itinéraire).") {
        // fork FR : la batterie réelle de la voiture, lue dans Home Assistant au moment de choisir une destination
        SFieldItem("Home Assistant : adresse", prefs.s(com.headunit.launcher.HaBattery.URL_KEY, ""),
            "Pour lire la batterie réelle de la voiture (ex. https://xxxx.ui.nabu.casa)" + com.headunit.launcher.HaBattery.status.takeIf { it.isNotBlank() }.let { if (it != null) " · $it" else "" },
            placeholder = "https://…") { prefs.put(com.headunit.launcher.HaBattery.URL_KEY, it.trim()) }
        SFieldItem("Home Assistant : jeton longue durée", prefs.s(com.headunit.launcher.HaBattery.TOKEN_KEY, ""),
            "Profil Home Assistant › Sécurité › Jetons d'accès longue durée", password = true) { prefs.put(com.headunit.launcher.HaBattery.TOKEN_KEY, it.trim()) }
        SFieldItem("Home Assistant : entité batterie (%)", prefs.s(com.headunit.launcher.HaBattery.ENTITY_KEY, ""),
            "L'entité du niveau de charge, ex. sensor.mg4_soc", placeholder = "sensor.…") { prefs.put(com.headunit.launcher.HaBattery.ENTITY_KEY, it.trim()) }
        SFieldItem("Home Assistant : entité consommation (facultatif)", prefs.s(com.headunit.launcher.HaBattery.CONS_KEY, ""),
            "Vide : calculée d'après l'autonomie et l'énergie restante (MG via saic-gateway). Sinon une entité en kWh/100 km ou km/kWh. Remplit tout seul la Consommation et la Batterie utile", placeholder = "sensor.…") { prefs.put(com.headunit.launcher.HaBattery.CONS_KEY, it.trim()) }
        SToggle(prefs, "Afficher aussi les stations-service", PreferenceKeys.EV_SHOW_FUEL, false, "Le bouton Carburant sur la carte (téléphone et Android Auto). Désactivé : Recharge le remplace")
        if (mi) SFieldItem("Consommation (miles par kWh)", prefs.s(PreferenceKeys.EVMPK, ""), placeholder = "ex. 3.5", number = true) { s -> prefs.put(PreferenceKeys.EVMPK, num(s).take(5)) }
        else SFieldItem("Consommation (km par kWh)", prefs.s(PreferenceKeys.EVMPK, "").toDoubleOrNull()?.let { fmt1(it * 1.609344) } ?: "", placeholder = "ex. 5.6", number = true) { s ->
            prefs.put(PreferenceKeys.EVMPK, num(s).toDoubleOrNull()?.takeIf { it > 0 }?.let { String.format(java.util.Locale.US, "%.3f", it / 1.609344) } ?: "")
        }
        SFieldItem("Batterie utile (kWh)", prefs.s(PreferenceKeys.EV_BATTERY_KWH, ""), "Pour l'estimation de batterie et les arrêts recharge (60 si vide)", placeholder = "ex. 64", number = true) { s -> prefs.put(PreferenceKeys.EV_BATTERY_KWH, s.filter { it.isDigit() || it == '.' }.take(5)) }
        SFieldItem(if (mi) "Prix de l'électricité (pence par kWh)" else "Prix de l'électricité (centimes par kWh)", prefs.s(PreferenceKeys.EVPPK, ""), placeholder = "ex. 28", number = true) { s -> prefs.put(PreferenceKeys.EVPPK, s.filter { it.isDigit() || it == '.' }.take(6)) }
    } else SGroup("Coût du carburant dans les options d'itinéraire", footer = "Avec les deux renseignés, chaque itinéraire indique à peu près son coût en carburant.") {
        if (mi) SFieldItem("Consommation (miles par gallon)", prefs.s(PreferenceKeys.FUELMPG, ""), placeholder = "ex. 45", number = true) { s -> prefs.put(PreferenceKeys.FUELMPG, num(s).take(5)) }
        // litres per 100 km: 282.48 / UK miles per gallon
        else SFieldItem("Consommation (litres aux 100 km)", prefs.s(PreferenceKeys.FUELMPG, "").toDoubleOrNull()?.takeIf { it > 0 }?.let { fmt1(282.481 / it) } ?: "", placeholder = "ex. 6.3", number = true) { s ->
            prefs.put(PreferenceKeys.FUELMPG, num(s).toDoubleOrNull()?.takeIf { it > 0 }?.let { String.format(java.util.Locale.US, "%.2f", 282.481 / it) } ?: "")
        }
        SFieldItem(if (mi) "Prix du carburant (pence par litre)" else "Prix du carburant (centimes par litre)", prefs.s(PreferenceKeys.FUELPPL, ""), placeholder = "ex. 139.9", number = true) { s -> prefs.put(PreferenceKeys.FUELPPL, s.filter { it.isDigit() || it == '.' }.take(6)) }
    }
}


/** Town and village names for your country, so search works with no signal even without the full pack. */
@Composable
private fun OfflinePlacesGroup(engine: NavEngine) {
    val ctx = LocalContext.current
    val sc = rememberCoroutineScope()
    // the country of the chosen pack, else the one you're in
    val cc = engine.chosenPack().country.ifBlank { engine.detectedCountry }.ifBlank { "GB" }
    val where = countryName(cc)
    var state by remember { mutableStateOf(if (OfflinePlaces.installed(ctx)) "Installé (%.1f Mo)".format(OfflinePlaces.sizeMb(ctx)) else "Non téléchargé") }
    var busy by remember { mutableStateOf(false) }
    SGroup("Noms de lieux", footer = "Un petit téléchargement des villes et villages ($where), pour les chercher sans réseau même sans le pack complet.") {
        SItem("Villes et villages", state) {
            SButton(if (busy) "Téléchargement…" else if (OfflinePlaces.installed(ctx)) "Mettre à jour" else "Télécharger", primary = !OfflinePlaces.installed(ctx)) {
                if (!busy) {
                    busy = true; state = "Téléchargement (réseau nécessaire)…"
                    sc.launch {
                        try {
                            val n = withContext(Dispatchers.IO) { OfflinePlaces.download(ctx, cc) }
                            state = "Installé : $n lieux"; engine.toast("Lieux hors ligne prêts", 3000)
                        } catch (e: Exception) { state = "Échec : ${e.message}" } finally { busy = false }
                    }
                }
            }
            if (OfflinePlaces.installed(ctx) && !busy) SButton("Retirer", danger = true) { OfflinePlaces.delete(ctx); state = "Non téléchargé" }
        }
    }
}

// ------------------------------------------------------------------ Screen & sizes

@Composable
private fun ScreenSection(prefs: Prefs, engine: NavEngine) {
    SGroup("Écran") {
        SSlider("Luminosité", prefs.i(PreferenceKeys.BRIGHT, 100).toFloat(), 10f..100f, "%", sub = "100 % laisse le système décider", live = true) { prefs.put(PreferenceKeys.BRIGHT, it) }
        SToggle(prefs, "Assombrir l'écran la nuit", "dimnight", false)
        SToggle(prefs, "Garder l'écran allumé", "keepon", true)
        val screenConfig = androidx.compose.ui.platform.LocalConfiguration.current
        val autoPercent = recommendedMainUiScale(screenConfig.screenWidthDp, screenConfig.screenHeightDp)
        val autoScale = prefs.b(PreferenceKeys.UI_AUTO_SCALE, prefs.s(PreferenceKeys.UI_SCALE, "").isBlank())
        SItem(
            "Mise à l'échelle automatique",
            if (autoScale) "Selon la taille et la forme de l'écran (actuellement $autoPercent %)" else "Taille manuelle du texte et des boutons activée"
        ) {
            Switch(checked = autoScale, onCheckedChange = { enabled ->
                if (!enabled) prefs.put(PreferenceKeys.UI_SCALE, autoPercent)
                prefs.put(PreferenceKeys.UI_AUTO_SCALE, enabled)
            })
        }
        SSlider(
            "Taille du texte et des boutons",
            (if (autoScale) autoPercent else prefs.i(PreferenceKeys.UI_SCALE, autoPercent)).toFloat(),
            60f..140f, "%", sub = if (autoScale) "Déplacez le curseur pour passer en taille manuelle" else "Réglez à la main, ou réactivez la mise à l'échelle automatique", steps = 15, reset = 100f
        ) {
            prefs.put(PreferenceKeys.UI_AUTO_SCALE, false)
            prefs.put(PreferenceKeys.UI_SCALE, it)
        }
        var mainAdvanced by rememberSaveable { mutableStateOf(false) }
        SItem(if (mainAdvanced) "Tailles d'affichage avancées" else "Tailles d'affichage avancées", "Réglez chaque élément sans changer l'échelle globale") {
            SButton(if (mainAdvanced) "Masquer" else "Afficher") { mainAdvanced = !mainAdvanced }
        }
        if (mainAdvanced) {
            SSlider("Taille des cartes", prefs.i(PreferenceKeys.MAIN_CARD_SIZE, 100).toFloat(), 70f..150f, "%", sub = "Cartes de direction, de trajet et d'itinéraire", steps = 15, reset = 100f) { prefs.put(PreferenceKeys.MAIN_CARD_SIZE, it) }
            SSlider("Taille des boutons", prefs.i(PreferenceKeys.MAIN_BUTTON_SIZE, 100).toFloat(), 70f..150f, "%", sub = "Commandes à l'écran", steps = 15, reset = 100f) { prefs.put(PreferenceKeys.MAIN_BUTTON_SIZE, it) }
            SSlider("Taille du texte", prefs.i(PreferenceKeys.MAIN_TEXT_SIZE, 100).toFloat(), 70f..150f, "%", sub = "Échelle du texte indépendante", steps = 15, reset = 100f) { prefs.put(PreferenceKeys.MAIN_TEXT_SIZE, it) }
            SSlider("Taille de la flèche de navigation", prefs.i(PreferenceKeys.MAIN_ARROW_SIZE, 100).toFloat(), 60f..180f, "%", sub = "Flèche de position sur la carte", steps = 11, reset = 100f) { prefs.put(PreferenceKeys.MAIN_ARROW_SIZE, it) }
            SSlider("Taille du nom de rue", prefs.i(PreferenceKeys.ROAD_NAME_SIZE, 100).toFloat(), 70f..200f, "%", sub = "Le nom de la route où vous êtes, sous votre flèche", steps = 12, reset = 100f) { prefs.put(PreferenceKeys.ROAD_NAME_SIZE, it) }
        }

    }
    SGroup("En conduite") {
        SToggle(prefs, "Votre vitesse à côté de la limitation", "mapspeed", true)
        SToggle(prefs, "Anneau du compteur", "speedring", true, "Se remplit vers la limite, passe à l'orange puis au rouge")
        SSlider("Taille de la limitation et du compteur", prefs.i(PreferenceKeys.SPEED_SIZE, 100).toFloat(), 50f..200f, "%", sub = "Sur le téléphone et l'écran de la voiture", steps = 29, reset = 100f) { prefs.put(PreferenceKeys.SPEED_SIZE, it) }
        SChoice("Position de la vitesse", listOf("bottom" to "En bas", "middle" to "Au milieu", "top" to "En haut"), prefs.s(PreferenceKeys.SPEED_POS, "bottom"),
            "Du côté opposé aux cartes pour ne jamais les cacher. Montez-la si votre main la cache") { prefs.put(PreferenceKeys.SPEED_POS, it) }
        SToggle(prefs, "Route et cap en conduite sans itinéraire", "cruise", true)
        SToggle(prefs, "Garder la flèche sur la route sans itinéraire", "snaproad", true)
        SToggle(prefs, "Assombrir la carte dans les tunnels", "tunneldim", true)
        SToggle(prefs, "Moins de boutons en conduite", "drivesimple", true)
        SToggle(prefs, "Bloquer la saisie en conduite", "drivelock", true, "La recherche vocale reste possible")
    }
    SGroup("Unités") {
        SChoice("Distance et vitesse", listOf("mi" to "Miles", "km" to "Kilomètres"), if (engine.imperial) "mi" else "km") { prefs.put(PreferenceKeys.DISTANCE_UNIT, it) }
        SChoice("Horloge", listOf("auto" to "Automatique", "24" to "24 h", "12" to "12 h"), prefs.s(PreferenceKeys.CLOCK, "auto"),
            "Automatique suit le réglage 12 / 24 h du téléphone ou de l'autoradio") { prefs.put(PreferenceKeys.CLOCK, it) }
        SChoice("Température", listOf("C" to "°C", "F" to "°F"), if (isImperial(prefs)) "F" else "C") { prefs.put(PreferenceKeys.TEMPERATURE_UNIT, it) }
        SChoice("Position du volant", listOf("" to "Automatique", "rhd" to "Volant à droite", "lhd" to "Volant à gauche"), prefs.s(PreferenceKeys.DRIVE, ""),
            when (prefs.s(PreferenceKeys.DRIVE, "")) {
                "rhd" -> "Conducteur à droite (Royaume-Uni) : cartes à gauche, boutons à droite"
                "lhd" -> "Conducteur à gauche (France, Europe, États-Unis) : écran en miroir, cartes à droite"
                else -> "Suit le pays où vous êtes" + (if (engine.mirrored) " : volant à gauche, écran en miroir" else " : volant à droite")
            }) { prefs.put(PreferenceKeys.DRIVE, it) }
    }
}

// ------------------------------------------------------------------ Android Auto

@Composable
private fun AndroidAutoSection(prefs: Prefs) {
    val ctx = LocalContext.current
    // Android Auto only lists apps that weren't installed from Play when its own "Sources inconnues" is on, and some phones hide them
    // anyway: the steps that get it showing, with shortcuts
    SGroup("Écran de la voiture", footer = "Si Horizon Nav n'apparaît pas dans Android Auto : 1. Ouvrez les paramètres d'Android Auto, touchez une dizaine de fois le numéro de version en bas, puis dans le menu en haut à droite ouvrez les Paramètres pour les développeurs et activez Sources inconnues. 2. De retour dans les paramètres d'Android Auto, ouvrez Personnaliser le lanceur et vérifiez que Horizon Nav est coché. 3. Forcez l'arrêt d'Android Auto (Infos sur l'appli ci-dessous), débranchez et rebranchez. Si elle n'apparaît toujours pas, l'Android Auto de votre téléphone n'affiche que les applis installées par le Play Store : installez l'APK avec un installateur qui déclare le Play Store comme source (par exemple « adb install -i com.android.vending », ou une appli comme AAAD ou Install with Options).") {
        val gh = "com.google.android.projection.gearhead"
        val ghUri = android.net.Uri.parse("package:$gh")
        val hasAa = remember { try { ctx.packageManager.getPackageInfo(gh, 0); true } catch (e: Exception) { false } }
        val source = remember {
            try {
                val inst = if (android.os.Build.VERSION.SDK_INT >= 30) ctx.packageManager.getInstallSourceInfo(ctx.packageName).installingPackageName
                    else @Suppress("DEPRECATION") ctx.packageManager.getInstallerPackageName(ctx.packageName)
                when (inst) {
                    "com.android.vending" -> "Google Play (Android Auto l'affiche sans rien faire de plus)"
                    null -> "Inconnu (installé depuis un fichier) : nécessite Sources inconnues dans Android Auto"
                    ctx.packageName -> "Mise à jour intégrée d'Horizon Nav : nécessite Sources inconnues dans Android Auto"
                    else -> "$inst : nécessite Sources inconnues dans Android Auto"
                }
            } catch (e: Exception) { "Inconnu" }
        }
        SSlider("Taille globale sur l'écran de la voiture", prefs.i(PreferenceKeys.CARSCALE, 100).toFloat(), 40f..300f, "%", sub = "Échelle de base de tout l'affichage", steps = 25, reset = 100f) { prefs.put(PreferenceKeys.CARSCALE, it) }
        SSlider("Taille du compteur et du panneau de limitation", prefs.i(PreferenceKeys.CAR_SPEED_SIZE, 100).toFloat(), 40f..180f, "%", sub = "Réduit automatiquement si la place à l'écran est insuffisante", reset = 100f) { prefs.put(PreferenceKeys.CAR_SPEED_SIZE, it) }
        var carAdvanced by rememberSaveable { mutableStateOf(false) }
        SItem("Tailles avancées pour l'écran de la voiture", "Réglez finement chaque élément") {
            SButton(if (carAdvanced) "Masquer" else "Afficher") { carAdvanced = !carAdvanced }
        }
        if (carAdvanced) {
            SSlider("Taille des cartes", prefs.i(PreferenceKeys.CAR_CARD_SIZE, 100).toFloat(), 70f..150f, "%", sub = "Cartes de direction, de trajet et d'itinéraire", steps = 16, reset = 100f) { prefs.put(PreferenceKeys.CAR_CARD_SIZE, it) }
            SSlider("Taille des boutons", prefs.i(PreferenceKeys.CAR_BUTTON_SIZE, 100).toFloat(), 70f..150f, "%", sub = "Commandes rondes et boutons tactiles", steps = 16, reset = 100f) { prefs.put(PreferenceKeys.CAR_BUTTON_SIZE, it) }
            SSlider("Taille du texte", prefs.i(PreferenceKeys.CAR_TEXT_SIZE, 100).toFloat(), 70f..150f, "%", sub = "Indépendant de la taille des cartes et des boutons", steps = 16, reset = 100f) { prefs.put(PreferenceKeys.CAR_TEXT_SIZE, it) }
            SSlider("Taille de la flèche de navigation", prefs.i(PreferenceKeys.CAR_ARROW_SIZE, 100).toFloat(), 60f..180f, "%", reset = 100f) { prefs.put(PreferenceKeys.CAR_ARROW_SIZE, it) }
            SSlider("Taille du nom de rue", prefs.i(PreferenceKeys.CAR_ROAD_NAME_SIZE, 100).toFloat(), 70f..200f, "%", sub = "Le nom de la route où vous êtes, sous votre flèche", steps = 12, reset = 100f) { prefs.put(PreferenceKeys.CAR_ROAD_NAME_SIZE, it) }
        }
        SItem("Installé par", source)
        SItem("Paramètres d'Android Auto", if (hasAa) null else "Android Auto n'est pas installé sur ce téléphone") {
            if (hasAa) SButton("Ouvrir") {
                val i = ctx.packageManager.getLaunchIntentForPackage(gh)
                try {
                    if (i != null) ctx.startActivity(i.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) else openSystemSettings(ctx, Settings.ACTION_APPLICATION_DETAILS_SETTINGS, ghUri)
                } catch (e: Exception) { openSystemSettings(ctx, Settings.ACTION_APPLICATION_DETAILS_SETTINGS, ghUri) }
            }
        }
        if (hasAa) SItem("Infos sur l'appli Android Auto", "Pour forcer son arrêt après avoir modifié ses paramètres") {
            SButton("Infos sur l'appli") { openSystemSettings(ctx, Settings.ACTION_APPLICATION_DETAILS_SETTINGS, ghUri) }
        }
        // the most asked question: Android Auto only lists apps from Google Play unless told otherwise
        if (hasAa) SItem("Absente d'Android Auto ?", "1. Dans les paramètres d'Android Auto, touchez plusieurs fois Version pour débloquer les Paramètres pour les développeurs, puis activez Sources inconnues. " +
            "2. Forcez l'arrêt d'Android Auto (Infos sur l'appli ci-dessus) et reconnectez. 3. Si elle n'apparaît toujours pas, installez Horizon Nav avec l'appli gratuite AAEnabler, ce dont la plupart des gens ont besoin. " +
            "Les commandes vocales via le micro d'Android Auto peuvent ne pas atteindre les applis hors Google Play : utilisez plutôt le bouton de recherche de l'appli.") {
            SButton("AAEnabler") {
                try { ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/malebuffy/AAEnabler")).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
                catch (e: Exception) { android.widget.Toast.makeText(ctx, "Pas de navigateur sur cet appareil : cherchez AAEnabler sur GitHub", android.widget.Toast.LENGTH_LONG).show() }
            }
        }
    }
}

// ------------------------------------------------------------------ Performance

@Composable
private fun PerformanceSection(prefs: Prefs) {
    SGroup("Fluidité", footer = "Baissez ces réglages si la carte est lente ou si l'autoradio chauffe. La netteté et le dessin rapide s'appliquent après un redémarrage de l'appli.") {
        SChoice("Netteté de la carte", listOf("100" to "Maximale", "80" to "Équilibrée", "65" to "Légère", "50" to "Minimale"), prefs.s(PreferenceKeys.MAP_RESOLUTION, "80")) { prefs.put(PreferenceKeys.MAP_RESOLUTION, it) }
        SChoice("Images par seconde", listOf("auto" to "Auto", "20" to "20", "30" to "30", "45" to "45", "60" to "60"), prefs.s(PreferenceKeys.MAP_FPS, "auto"),
            if (prefs.s(PreferenceKeys.MAP_FPS, "auto") == "auto") "Démarre à 30 et baisse quand l'autoradio ne suit pas, pour une carte fluide" else "Plus bas, c'est plus léger pour l'autoradio") { prefs.put(PreferenceKeys.MAP_FPS, it) }
        SToggle(prefs, "Dessin rapide de la carte", "mapfast", true)
        // (the old "Carte légère" switch is now Settings > Map > Map detail)
        SToggle(prefs, "Indicateur de performances", "perfmeter", false, "Une ligne en haut de la carte : images dessinées, mouvements de caméra et pauses. Pratique pour trouver ce qui ralentit la carte")
    }
    SGroup("Démo") {
        SToggle(prefs, "Bouton de trajet de démo", "demo", false, "Rejoue un itinéraire pour essayer le guidage à l'arrêt")
    }
}

// ------------------------------------------------------------------ Keys & accounts

@Composable
private fun KeysSection(prefs: Prefs, engine: NavEngine) {
    SGroup("Recherche") {
        SChoice("Résultats de recherche", searchCountryOptions(engine), searchCountryValue(prefs)) { prefs.put(PreferenceKeys.COUNTRY, it) }
        if (engine.hasKey && engine.hasMapbox) SChoice("Rechercher avec", listOf("auto" to "TomTom", "mapbox" to "Mapbox"), if (prefs.s(PreferenceKeys.SEARCHWITH, "auto") == "mapbox") "mapbox" else "auto",
            "Horizon Nav utilise la recherche TomTom : la meilleure pour les adresses et numéros de rue") { prefs.put(PreferenceKeys.SEARCHWITH, it) }
        else if (!engine.hasKey) SNote("Pour la recherche d'adresse la plus précise, ajoutez une clé TomTom gratuite ci-dessous.")
    }
    SGroup("Comptes de carte", footer = "La recherche trouve aussi les lieux enregistrés ou déjà visités. Sans réseau, itinéraires, carte et recherche viennent du pack hors ligne.") {
        SFieldItem("Clé API TomTom", prefs.s(PreferenceKeys.API_KEY, ""), if (BuildConfig.TOMTOM_API_KEY.isNotBlank() && prefs.s(PreferenceKeys.API_KEY, "").isBlank()) "Clé intégrée à l'appli utilisée, sauf si vous en collez une autre" else "Facultatif : la recherche d'adresse la plus précise, et une solution de secours pour les itinéraires et le trafic. Gratuite sur developer.tomtom.com",
            placeholder = "Coller une clé") { prefs.setApiKey(it) }
        SFieldItem("Jeton d'accès Mapbox", prefs.s(PreferenceKeys.MAPBOX_TOKEN, ""), "Utilisez un jeton public actif commençant par pk. (jamais sk.). Si la carte affiche 401, ouvrez account.mapbox.com > Tokens, copiez un jeton actif et vérifiez qu'il a les droits styles:read et fonts:read.",
            placeholder = "Coller un jeton") { prefs.put(PreferenceKeys.MAPBOX_TOKEN, com.headunit.launcher.MapboxRest.clean(it)) }
    }
    if (engine.hasKey && engine.hasMapbox) SGroup("Itinéraires, recherche et trafic", footer = "Auto utilise Mapbox quand la carte Mapbox est affichée, TomTom sinon. Si celui en cours ne marche plus (limite du jour, clé refusée, panne), l'autre prend le relais tout seul pendant un moment.") {
        SChoice("Utiliser", listOf("auto" to "Auto", "tomtom" to "TomTom", "mapbox" to "Mapbox"), prefs.s(PreferenceKeys.DATAPROV, "auto")) { prefs.put(PreferenceKeys.DATAPROV, it) }
    }
}

// ------------------------------------------------------------------ About & backup

@Composable
private fun AboutSection(prefs: Prefs, engine: NavEngine) {
    val ctx = LocalContext.current
    val pkg = android.net.Uri.parse("package:" + ctx.packageName)
    var donating by remember { mutableStateOf(false) }
    if (donating) DonateDialog(LocalVw.current.dark) { donating = false }
    SGroup("Soutenir Horizon Nav") {
        SItem("Faire un don", "Gratuite, sans pub. Les dons aident à la faire vivre (PayPal)") { SButton("Faire un don", primary = true) { donating = true } }
    }
    SGroup("Horizon Nav") {
        SItem("Version", BuildConfig.VERSION_NAME)
        if (com.headunit.launcher.Updater.enabled) {
            SItem("Rechercher des mises à jour", engine.updateStatus.ifBlank { null }) {
                val u = engine.appUpdate
                if (u != null && !engine.updateBusy) SButton("Mettre à jour vers ${u.version}", primary = true) { engine.installAppUpdate() }
                else SButton(if (engine.updateBusy) "Mise à jour…" else "Vérifier maintenant") { engine.checkForUpdate(manual = true) }
            }
            SToggle(prefs, "Chercher les mises à jour automatiquement", "updcheck", true, "À l'ouverture de l'appli avec une connexion (au plus toutes les demi-heures)")
        }
        SItem("Configuration", "Refaire les étapes du premier lancement : clé de carte, domicile et travail, cartes hors ligne, autorisations") { SButton("Lancer la configuration") { prefs.put(PreferenceKeys.SETUP_DONE, false); prefs.put(PreferenceKeys.FORCESETUP, true) } }
        var feedback by remember { mutableStateOf(false) }
        SItem("Avis ou problème", "Envoie un rapport avec la version, votre appareil et le dernier plantage, pour une correction rapide") { SButton("Signaler", primary = true) { feedback = true } }
        if (feedback) FeedbackDialog(engine) { feedback = false }
        // fork FR : jeton pour déposer les rapports et les plantages dans le dépôt GitHub du fork
        SFieldItem("Jeton GitHub (rapports)", prefs.s(com.headunit.launcher.DiagUpload.TOKEN_KEY, ""),
            "Jeton « fine-grained » limité à ${BuildConfig.UPDATE_REPO}, droit « Contents : Read and write ». Les rapports et plantages sont alors envoyés tout seuls dans la branche « diagnostics »" +
                (com.headunit.launcher.DiagUpload.status.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""),
            placeholder = "github_pat_…", password = true) { prefs.put(com.headunit.launcher.DiagUpload.TOKEN_KEY, it.trim()) }
    }
    SGroup("Autorisations", footer = "Pendant un itinéraire, le guidage continue quand vous changez d'appli et une notification s'affiche. Autorisez les notifications pour qu'elle apparaisse.") {
        val hasLoc = ctx.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        SItem("Localisation", if (hasLoc) "Autorisée" else "Refusée : la navigation en a besoin") {
            SButton("Autorisations de l'appli", primary = !hasLoc) { openSystemSettings(ctx, Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkg) }
        }
        SItem("Services de localisation") { SButton("Ouvrir") { openSystemSettings(ctx, Settings.ACTION_LOCATION_SOURCE_SETTINGS) } }
    }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            val ok = try {
                ctx.contentResolver.openOutputStream(uri)?.use { it.write(prefs.exportJson().toByteArray()) }
                true
            } catch (e: Exception) { false }
            android.widget.Toast.makeText(ctx, if (ok) "Sauvegarde enregistrée" else "Impossible d'enregistrer la sauvegarde", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val msg = try {
                val text = ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
                "${prefs.importJson(text)} réglages restaurés"
            } catch (e: Exception) { "Ce fichier n'est pas une sauvegarde valide" }
            android.widget.Toast.makeText(ctx, msg, android.widget.Toast.LENGTH_LONG).show()
        }
    }
    SGroup("Sauvegarde", footer = "Comprend les réglages et lieux enregistrés, pas la clé API ni le pack hors ligne. Une sauvegarde d'Horizon Launcher peut aussi être restaurée ici.") {
        SItem("Réglages et lieux enregistrés") {
            SButton("Sauvegarder") { exporter.launch("horizon-nav-backup.json") }
            SButton("Restaurer") { importer.launch(arrayOf("application/json", "text/plain", "*/*")) }
        }
    }
    val cf = java.io.File(ctx.filesDir, "lastcrash.txt")
    var rep by remember { mutableStateOf(if (cf.exists()) cf.readText() else "") }
    if (rep.isNotBlank()) SGroup("Rapport de problème", footer = "L'appli s'est fermée de façon inattendue la dernière fois. Envoyez une photo de ceci pour que ce soit corrigé.") {
        Text(rep.take(900), color = LocalVw.current.text, fontSize = 10.sp, modifier = Modifier.padding(vertical = 6.dp))
        SItem("Effacer le rapport") { SButton("Effacer") { cf.delete(); rep = "" } }
    }
}

/** Readable name of a country code ("UK", "Norvège"...). */
internal fun countryName(cc: String): String = when (cc.uppercase()) {
    "GB" -> "Royaume-Uni"; "US" -> "États-Unis"; "" -> ""
    else -> java.util.Locale("", cc).getDisplayCountry(java.util.Locale.FRANCE).ifBlank { cc }
}

/** Search limited to the country you're in (followed as you travel) or anywhere. */
internal fun searchCountryOptions(engine: NavEngine): List<Pair<String, String>> {
    val here = engine.detectedCountry
    return listOf("auto" to (if (here.isNotBlank()) "${countryName(here)} uniquement" else "Mon pays uniquement"), "" to "Partout")
}

internal fun searchCountryValue(prefs: Prefs): String = if (prefs.s(PreferenceKeys.COUNTRY, "auto").isBlank()) "" else "auto"

/**
 * Feedback: say what happened in a line or two, then send it as a GitHub issue (the browser opens with it filled in) or copy it to
 * paste anywhere (Reddit, a message). Either way the app version, unit, main settings and the last crash go with it.
 */
@Composable
private fun FeedbackDialog(engine: NavEngine, onClose: () -> Unit) {
    val c = LocalVw.current
    val ctx = LocalContext.current
    var note by remember { mutableStateOf("") }
    androidx.compose.ui.window.Dialog(onDismissRequest = onClose) {
        Column(Modifier.widthIn(max = 560.dp).fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(c.panel).padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Avis ou problème", color = c.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Que s'est-il passé, ou que souhaitez-vous ? Où vous étiez et ce que vous avez touché aide beaucoup.", color = c.sub, fontSize = 14.sp)
            SField(note, { note = it.take(600) }, placeholder = "ex. Mauvais recalcul près de Lyon", width = 516.dp, fill = true)
            Text("Envoyés avec : la version de l'appli, votre autoradio, les principaux réglages, vos zones téléchargées et le dernier plantage (ni lieux ni clés API).", color = c.sub, fontSize = 12.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                SButton("Fermer") { onClose() }
                SButton("Copier") {
                    val cm = ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                    cm?.setPrimaryClip(android.content.ClipData.newPlainText("Horizon Nav report", engine.feedbackReport(note)))
                    engine.toast("Rapport copié : collez-le dans un message ou un commentaire Reddit", 3500)
                }
                // fork FR : avec un jeton GitHub, le rapport est déposé directement dans le dépôt (branche « diagnostics »)
                if (com.headunit.launcher.DiagUpload.hasToken(ctx)) SButton("Envoyer", primary = true) {
                    val report = engine.feedbackReport(note)
                    engine.toast("Envoi du rapport…", 2000)
                    Thread {
                        val msg = try { "Rapport envoyé : " + com.headunit.launcher.DiagUpload.send(ctx, "avis-${com.headunit.launcher.DiagUpload.stamp()}.txt", report) }
                                  catch (e: Exception) { "Envoi impossible : ${e.message}. Utilisez Copier." }
                        android.os.Handler(android.os.Looper.getMainLooper()).post { engine.toast(msg, 5000) }
                    }.start()
                    onClose()
                } else SButton("Envoyer sur GitHub", primary = true) {
                    val title = note.lineSequence().firstOrNull()?.take(80)?.ifBlank { null } ?: "Report from Horizon Nav ${com.headunit.launcher.BuildConfig.VERSION_NAME}"
                    // a link has a length limit, so a very long report is cut down (Copy always has all of it)
                    val body = engine.feedbackReport(note).take(5000)
                    val url = "https://github.com/${BuildConfig.UPDATE_REPO}/issues/new?title=" + android.net.Uri.encode(title) + "&body=" + android.net.Uri.encode(body)
                    try { ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    catch (e: Exception) { engine.toast("Pas de navigateur sur cet appareil : utilisez Copier", 3500) }
                    onClose()
                }
            }
        }
    }
}

/**
 * Settings > Map > Colours: a small picture of each colour set (land, water, park, roads, a building and the route), tap to pick.
 * Day colours are shown; each one has its night version.
 */
@Composable
private fun MapColoursPicker(value: String, onPick: (String) -> Unit) {
    val c = LocalVw.current
    // land, water, park, road, building
    val sets = listOf(
        Triple("mapbox", "Mapbox", listOf(0xFFEDEFE9, 0xFF9CCBF0, 0xFFC6E4B4, 0xFFFFFFFF, 0xFFDCDCE0)),
        Triple("horizon", "Horizon", listOf(0xFFF4F2EE, 0xFFAADAF5, 0xFFCDEBC4, 0xFFFFFFFF, 0xFFE7E5E1)),
        Triple("faded", "Pastel", listOf(0xFFF1F1F1, 0xFFD9E2EA, 0xFFE2E8DE, 0xFFFFFFFF, 0xFFE6E6E6)),
        Triple("grey", "Gris", listOf(0xFFE8E8E8, 0xFFCFCFCF, 0xFFDADADA, 0xFFFFFFFF, 0xFFD4D4D4)),
    )
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        SItem("Couleurs", "Horizon : la carte Mapbox aux couleurs douces de l'appli. Couleurs de jour affichées ; chacune a une version de nuit")
        Row(Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()).padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for ((id, name, cols) in sets) {
                val sel = id == value
                Column(
                    Modifier.width(124.dp).clip(RoundedCornerShape(14.dp)).background(c.tile)
                        .border(2.dp, if (sel) c.accent else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(14.dp))
                        .clickable { onPick(id) },
                ) {
                    androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(76.dp)) {
                        val w = size.width; val h = size.height
                        fun col(i: Int) = androidx.compose.ui.graphics.Color(cols[i])
                        drawRect(col(0))
                        drawRect(col(1), topLeft = androidx.compose.ui.geometry.Offset(w * 0.68f, 0f), size = androidx.compose.ui.geometry.Size(w * 0.32f, h * 0.3f))
                        drawRect(col(2), topLeft = androidx.compose.ui.geometry.Offset(w * 0.06f, h * 0.62f), size = androidx.compose.ui.geometry.Size(w * 0.24f, h * 0.26f))
                        drawRect(col(4), topLeft = androidx.compose.ui.geometry.Offset(w * 0.47f, h * 0.1f), size = androidx.compose.ui.geometry.Size(w * 0.18f, h * 0.2f))
                        drawRect(col(3), topLeft = androidx.compose.ui.geometry.Offset(0f, h * 0.44f), size = androidx.compose.ui.geometry.Size(w, h * 0.12f))
                        drawRect(col(3), topLeft = androidx.compose.ui.geometry.Offset(w * 0.36f, 0f), size = androidx.compose.ui.geometry.Size(w * 0.06f, h))
                        drawRect(androidx.compose.ui.graphics.Color(0xFF2A7BE4), topLeft = androidx.compose.ui.geometry.Offset(w * 0.68f, 0f), size = androidx.compose.ui.geometry.Size(w * 0.05f, h))
                    }
                    Text(name, color = c.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
                }
            }
        }
    }
}
