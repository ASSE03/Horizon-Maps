
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.Switch
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.rounded.DriveEta
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Shop
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Traffic
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material.icons.rounded.Cloud
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.headunit.launcher.BuildConfig
import com.headunit.launcher.FuelFinder
import com.headunit.launcher.Geo
import com.headunit.launcher.L
import com.headunit.launcher.NavEngine
import com.headunit.launcher.OfflinePlaces
import com.headunit.launcher.OwnModel
import com.headunit.launcher.Place
import com.headunit.launcher.Prefs
import com.headunit.launcher.PreferenceKeys
import com.headunit.launcher.carSpeedSpot
import com.headunit.launcher.setCarSpeedSpot
import com.headunit.launcher.mapSource
import com.headunit.launcher.mapDetail
import com.headunit.launcher.setApiKey
import com.headunit.launcher.Zones
import com.headunit.launcher.Vehicle
import kotlinx.coroutines.Dispatchers
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** A settings section; [heading] puts a small group heading (Map, Driving, You, Device) above it in the list. */
private class Section(val title: String, val sub: String, val icon: ImageVector, val tint: Color, val heading: String? = null)

// (the order here is the order in the list; SettingsIndex refers to them by number)
// (made each time it's read, so the names follow a change of language straight away)
private val sections: List<Section> get() = listOf(
    Section("Style de carte", "Couleurs, jour et nuit, détail de la carte", Icons.Rounded.Map, Color(0xFF4C8DFF), "CARTE"),
    Section("Lumière et 3D", "Lumière, ombres, 3D, ciel, satellite", Icons.Rounded.WbTwilight, Color(0xFF4C8DFF)),
    Section("Météo", "Météo en direct, pluie, neige, brouillard, radar, prévisions", Icons.Rounded.Cloud, Color(0xFF4C8DFF)),
    Section("Sur la carte", "Trafic, feux, radars, panneaux, lampadaires", Icons.Rounded.Traffic, Color(0xFF4C8DFF)),
    Section("Votre flèche et la ligne d'itinéraire", "Flèche ou voiture, couleurs, aspect de l'itinéraire", Icons.Rounded.Navigation, Color(0xFF4C8DFF)),
    Section("Itinéraires", "Véhicule, éviter, choisir, démarrer", Icons.Rounded.Route, Color(0xFF12A4B4), "CONDUITE"),
    Section("Carte de direction et voies", "Panneaux, voies, carte de trajet, arrivée", Icons.Rounded.TurnRight, Color(0xFF12A4B4)),
    Section("Alertes", "Vitesse, radars, dangers, signalements", Icons.Rounded.NotificationsActive, Color(0xFFEA4335)),
    Section("Son et voix", "Guidage vocal et volume", Icons.Rounded.VolumeUp, Color(0xFFEA4335)),
    Section("En conduisant", "Nom de rue, flèche, tunnels, médias, verrouillage", Icons.Rounded.DriveEta, Color(0xFF12A4B4)),
    Section("Lieux et trajets", "Domicile, travail, favoris, partage, journal", Icons.Rounded.Star, Color(0xFFF29900), "VOUS"),
    Section("Carburant et recharge", "Votre carburant, prix, voiture électrique", Icons.Rounded.LocalGasStation, Color(0xFF34A853)),
    Section("Cartes hors ligne", "Cartes, itinéraires et noms de villes sans réseau", Icons.Rounded.CloudDownload, Color(0xFF8E5CF0)),
    Section("Écran et tailles", "Luminosité, tailles, vitesse, positions des cartes, unités", Icons.Rounded.BrightnessMedium, Color(0xFFFFB020), "DEVICE"),
    Section("Android Auto", "Tailles sur l'écran de la voiture et configuration", Icons.Rounded.DirectionsCar, Color(0xFF9CA4BF)),
    Section("Performances", "Fluidité sur les autoradios lents, démo", Icons.Rounded.Speed, Color(0xFF9CA4BF)),
    Section("Clés et comptes", "Clés de carte et recherche", Icons.Rounded.VpnKey, Color(0xFF9CA4BF)),
    Section("À propos et sauvegarde", "Rapport, mises à jour, autorisations, sauvegarde", Icons.Rounded.Info, Color(0xFF9CA4BF)),
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
    // opens on the page you were last on (kept between visits)
    var cat by rememberSaveable { mutableIntStateOf(prefs.i("settingscat", 0).coerceIn(0, sections.size - 1)) }
    androidx.compose.runtime.LaunchedEffect(cat) { prefs.put("settingscat", cat) }
    // portrait / narrow: one pane at a time (the section list, or the open section with a back arrow)
    val narrowS = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 720
    var showList by rememberSaveable { mutableStateOf(true) }
    androidx.activity.compose.BackHandler(enabled = narrowS && !showList) { showList = true }
    // the Google Play testers invitation, opened from the banner at the top of the list
    var playInvite by remember { mutableStateOf(false) }
    // Horizon Maps Plus (Play edition, once its key server is set up)
    var plusOpen by remember { mutableStateOf(false) }
    if (plusOpen) PlusDialog { plusOpen = false }
    if (playInvite) PlayInviteDialog(c.dark) { how ->
        // (from here "Not now" just closes it; "Don't show again" or opening Play also stops the popup on the map)
        if (how != InviteClose.LATER) prefs.put(PreferenceKeys.PLAY_INVITE, "never")
        playInvite = false
    }
    var query by remember { mutableStateOf("") }
    var highlight by remember { mutableStateOf<String?>(null) }
    val focus = androidx.compose.ui.platform.LocalFocusManager.current
    val words = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
    val matches = remember(query) {
        if (words.isEmpty()) emptyList() else settingsIndex.filter { r ->
            // (English and the chosen language both match, so "Sprache" and "language" find the same setting)
            val hay = (r.title + " " + L.t(r.title) + " " + r.sub + " " + r.group + " " + L.t(r.group) + " " + sections[r.section].title).lowercase()
            words.all { hay.contains(it) }
        }.sortedBy { val t = it.title.lowercase() + " " + L.t(it.title).lowercase(); if (t.startsWith(words[0]) || L.t(it.title).lowercase().startsWith(words[0])) 0 else if (t.contains(words[0])) 1 else 2 }.take(40)
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
                        Text(L.t(r.title), color = c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(sections[r.section].title + (if (r.group.isNotBlank()) " · " + L.t(r.group) else ""), color = c.sub, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            } else {
            // GitHub edition: a small banner about the coming Google Play version; tapping it opens the testers invitation
            if (false && BuildConfig.STORE != "play") PlayBanner(c) { playInvite = true }
            if (com.headunit.launcher.Plus.offered) PlusBanner { plusOpen = true }
            sections.forEachIndexed { i, s ->
                // the small group headings in the list: Map, Driving, You, Device
                if (s.heading != null) Text(L.t(s.heading), color = c.sub, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp,
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
           // the map pages show the real map at the top, so every change can be seen as it's made
           val withPreview = cat in 0..3
           // (in portrait too the preview now stays at the top while the settings scroll under it, as in landscape, so a change
           // can be seen as it's made, GitHub #124. Only a really short screen still scrolls it away, to leave room for the settings)
           val pinPreview = !narrowS || androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp >= 600
           val scroll = rememberScrollState()
           Column(Modifier.fillMaxSize()) {
            // portrait: the back arrow and the page name sit in a bar that stays put above everything, so going back never
            // means scrolling up first (the whole bar is the tap target)
            if (narrowS) Row(
                Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(RoundedCornerShape(50)).clickable { showList = true }.padding(end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(c.tile), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.ArrowBack, "Tous les réglages", tint = c.text, modifier = Modifier.size(22.dp))
                }
                Text(sections[cat].title, color = c.text, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 12.dp))
            }
            if (withPreview && pinPreview) SettingsMapPreview(engine, if (narrowS) 170 else 210)
            // (the on-screen keyboard takes the bottom of the screen: the settings scroll in the space above it, so the box being
            // typed in can be seen, smikkelvriend #200)
            Box(Modifier.fillMaxSize().imePadding()) {
             Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(horizontal = 4.dp, vertical = 4.dp)) {
                if (withPreview && !pinPreview) SettingsMapPreview(engine, 170)
                // (in portrait the page name is already in the bar above, so only its short description starts the page)
                if (!narrowS) Text(sections[cat].title, color = c.text, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp, top = 6.dp))
                Text(sections[cat].sub, color = c.sub, fontSize = 13.sp, modifier = Modifier.padding(start = 6.dp, top = if (narrowS) 2.dp else 0.dp, bottom = 16.dp))
                Column(Modifier.widthIn(max = 820.dp)) {
                    when (cat) {
                        0 -> MapStyleSection(prefs, engine)
                        1 -> LightSection(prefs)
                        2 -> WeatherSection(prefs)
                        3 -> OnTheMapSection(prefs, engine)
                        4 -> ArrowSection(prefs, engine)
                        5 -> RoutesSection(prefs, engine)
                        6 -> GuidanceSection(prefs, engine)
                        7 -> AlertsSection(prefs, engine)
                        8 -> SoundSection(prefs, engine)
                        9 -> WhileDrivingSection(prefs)
                        10 -> { PlacesSection(engine); MyRoutesGroup(engine, onShown = onClose); GpxGroup(engine, onShown = onClose); PhoneLinkGroup(prefs); SharingGroups(prefs) }
                        11 -> FuelSection(prefs, engine)
                        12 -> { OfflineMapsContent(prefs, engine); OfflinePlacesGroup(engine) }
                        13 -> ScreenSection(prefs, engine)
                        14 -> AndroidAutoSection(prefs)
                        15 -> PerformanceSection(prefs)
                        16 -> KeysSection(prefs, engine)
                        else -> AboutSection(prefs, engine)
                    }
                }
             }
             // soft edges: the settings fade out under whatever is pinned above them (and into the bottom of the screen) instead
             // of being cut off by a hard line. Each edge only shows while there is more to scroll that way
             val topFade by androidx.compose.animation.core.animateFloatAsState(if (scroll.value > 0) 1f else 0f, label = "topFade")
             val bottomFade by androidx.compose.animation.core.animateFloatAsState(if (scroll.value < scroll.maxValue) 1f else 0f, label = "bottomFade")
             Box(Modifier.align(Alignment.TopCenter).fillMaxWidth().height(28.dp).graphicsLayer { alpha = topFade }
                 .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(c.bg, c.bg.copy(alpha = 0.7f), Color.Transparent))))
             Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(28.dp).graphicsLayer { alpha = bottomFade }
                 .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, c.bg.copy(alpha = 0.7f), c.bg))))
            }
           }
          }
        }
    }
}

// ------------------------------------------------------------------ Map look

@Composable
private fun MapStyleSection(prefs: Prefs, engine: NavEngine) {
    SGroup("Jour et nuit") {
        // on a phone: Auto is sunrise and sunset, and "Phone's dark mode" follows the phone's own setting (lukaszsakul, #181);
        // Headlights is only for head units
        if (engine.onPhone()) SChoice("Jour et nuit", listOf("auto" to "Auto", "phone" to "Mode sombre du téléphone", "times" to "Mes horaires", "day" to "Jour", "night" to "Nuit"),
            prefs.s(PreferenceKeys.THEME, "auto").let { if (it == "sun" || it == "lights") "auto" else it },
            "Auto bascule au lever et au coucher du soleil, avec la lumière de l’aube et du crépuscule entre les deux. Mode sombre du téléphone suit le réglage clair / sombre de votre téléphone. Mes horaires bascule aux heures que vous choisissez") { prefs.put(PreferenceKeys.THEME, it) }
        else SChoice("Jour et nuit", listOf("auto" to "Auto", "lights" to "Phares", "sun" to "Heures du soleil", "times" to "Mes horaires", "day" to "Jour", "night" to "Nuit"), prefs.s(PreferenceKeys.THEME, "auto"),
            "Auto suit le mode nuit de l'autoradio dès qu'il y passe, et le lever et coucher du soleil en attendant. Phares suit le mode nuit de l'autoradio quand il change avec vos feux (activez son option phares ou éclairage), avec les heures du soleil en attendant. Heures du soleil change au lever et au coucher du soleil. Mes horaires change aux heures que vous choisissez") { prefs.put(PreferenceKeys.THEME, it) }
        // Headlights: whether this head unit has been seen switching with the lights yet (until then sun times stand in), and what the
        // lights signal says right now, so it's easy to check it works (lights on: it should say on within a couple of seconds)
        if (prefs.s(PreferenceKeys.THEME, "auto") == "lights") {
            val lightsOn = rememberSystemNight(useLamps = true)
            val seen = prefs.b(PreferenceKeys.SYSDARKSEEN, false) || lightsOn
            SNote(
                if (seen) "Seuls vos phares décident, même dans les tunnels. Signal des phares : ${if (lightsOn) L.t("on") else L.t("off")}"
                else "Votre autoradio n'a pas encore basculé avec les phares, donc le lever et le coucher du soleil servent en attendant. Activez son option de mode nuit liée aux phares ou à l'éclairage, puis allumez vos phares pour vérifier"
            )
            // units that pass the lights on in their own setting (Teyes, FYT-type): a few have it the other way round
            val ctxL = androidx.compose.ui.platform.LocalContext.current
            val hasLamps = androidx.compose.runtime.remember { readLampsNight(ctxL) != null }
            if (hasLamps) SToggle(prefs, "Le signal des feux est inversé", PreferenceKeys.LAMPS_INVERT, false, "Activez si « allumés » s'affiche feux éteints")
        }
        if (prefs.s(PreferenceKeys.THEME, "auto") == "times") {
            SFieldItem("La nuit commence à", prefs.s(PreferenceKeys.NIGHT_FROM, "19:00"), "Format 24 h, par exemple 19:30", placeholder = "19:00") { prefs.put(PreferenceKeys.NIGHT_FROM, it.trim()) }
            SFieldItem("Le jour commence à", prefs.s(PreferenceKeys.DAY_FROM, "07:00"), "Format 24 h, par exemple 06:45", placeholder = "07:00") { prefs.put(PreferenceKeys.DAY_FROM, it.trim()) }
        }
    }
    if (prefs.mapSource() == "mapbox") SGroup("Couleurs et lettrage") {
        MapColoursPicker(prefs.s(PreferenceKeys.MAP_COLOURS, "mapbox")) { picked ->
            val now = prefs.s(PreferenceKeys.MAP_COLOURS, "mapbox")
            // Custom starts as exactly the theme you had (nothing changes until you change something)
            if (picked == "custom" && now != "custom") {
                prefs.put(PreferenceKeys.CUSTOM_BASE, now)
                prefs.put(PreferenceKeys.CUSTOM_DAY, ""); prefs.put(PreferenceKeys.CUSTOM_NIGHT, ""); prefs.put(PreferenceKeys.CUSTOM_ROUTE_W, 100)
            }
            prefs.put(PreferenceKeys.MAP_COLOURS, picked)
        }
        if (prefs.s(PreferenceKeys.MAP_COLOURS, "mapbox") == "car") {
            SItem("Couleur de l'éclairage de la voiture", "La carte en prend une touche, surtout la nuit")
            val cur = prefs.s(PreferenceKeys.CAR_TINT, "#8A5AD0")
            Row(Modifier.padding(start = 4.dp, bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (hex in listOf("#E64B4B", "#FF9A3C", "#F2D04A", "#2FC9A0", "#3C8CFF", "#8A5AD0", "#E64BD8", "#FFFFFF")) {
                    val sel = hex.equals(cur, true)
                    Box(Modifier.size(34.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Color(android.graphics.Color.parseColor(hex)))
                        .border(if (sel) 3.dp else 1.dp, if (sel) LocalVw.current.accent else Color.White.copy(alpha = 0.25f), androidx.compose.foundation.shape.CircleShape)
                        .pressClick { prefs.put(PreferenceKeys.CAR_TINT, hex) })
                }
            }
            SPercent("Intensité", "La part de cette couleur que prend la carte", prefs, PreferenceKeys.CAR_TINT_STR, 10, 100, 40)
        }
        // (karthikeyansr: Mapbox's own lettering is narrow, and hard to read at a glance; these are wider and heavier)
        SChoice("Police", listOf("" to "Standard") + com.headunit.launcher.MAP_FONTS.map { it to it }, prefs.s(PreferenceKeys.MAP_FONT, ""),
            "La police des noms de routes et de lieux sur la carte", wrap = true) { prefs.put(PreferenceKeys.MAP_FONT, it) }
    }
    if (prefs.mapSource() == "mapbox" && prefs.s(PreferenceKeys.MAP_COLOURS, "mapbox") == "custom") CustomThemeGroup(prefs, engine)
    if (prefs.mapSource() == "mapbox") SavedThemesGroup(prefs)
    SGroup("La carte") {
        // only the choices that work here: the free Play version is always OpenStreetMap (no choice at all), Plus can't show
        // TomTom's map, and TomTom's map needs a TomTom key
        if (!com.headunit.launcher.Plus.free) {
            val src = prefs.mapSource()
            if (com.headunit.launcher.Plus.offered)
                SChoice("Source de la carte", listOf("mapbox" to "Mapbox", "osm" to "OpenStreetMap"), src) { prefs.put(PreferenceKeys.MAP_STYLE, it) }
            else {
                val withTomTom = engine.hasKey || src == "tomtom"
                SChoice("Source de la carte", listOf("mapbox" to "Mapbox") + (if (withTomTom) listOf("tomtom" to "TomTom") else emptyList()) + listOf("osm" to "OpenStreetMap"), src,
                    "Mapbox : la carte 3D. TomTom : la carte TomTom aux couleurs Horizon (clé TomTom requise). Toutes nécessitent le jeton Mapbox, car c'est le moteur de Mapbox qui les dessine") { prefs.put(PreferenceKeys.MAP_STYLE, it) }
            }
        }
        if (prefs.mapSource() == "mapbox") {
            SChoice("Niveau de détail", listOf("full" to "3D complète", "lite" to "Allégée", "lightest" to "Minimale"), prefs.mapDetail(),
                "3D complète : bâtiments, monuments, arbres et ombres. Allégée : bâtiments plats, sans monuments ni ombres. Minimale : la carte 2D simplifiée de Mapbox, pour les autoradios lents") { prefs.put(PreferenceKeys.MAP_DETAIL, it) }
        }
        DefaultZoomRow(prefs)
        SToggle(prefs, "Chemins et sentiers", "paths", true, "Désactivez pour une carte de conduite plus épurée")
        SChoice("Routes HD", listOf("auto" to "Automatique", "on" to "Activé", "off" to "Désactivé"), prefs.s(PreferenceKeys.HD_ROADS, "auto"),
            sub = "Marquages au sol, flèches de direction et ponts en 3D là où Mapbox les a (3D complète). Automatique : activé seulement au Royaume-Uni, car dans certains pays ils masquent les rues ordinaires") { prefs.put(PreferenceKeys.HD_ROADS, it) }
    }
}

@Composable
private fun LightSection(prefs: Prefs) {
    if (prefs.mapSource() == "mapbox") SGroup("Lumière") {
        SPercent("Luminosité de nuit", "Luminosité de toute la carte de nuit. 100 % correspond à l'éclairage de nuit de Mapbox", prefs, PreferenceKeys.NIGHT_BRIGHT, 30, 200, 100)
        SToggle(prefs, "Clair de lune la nuit", PreferenceKeys.MOONLIT, false, "Comme au crépuscule, mais avec la lune : une lumière bleue rasante aux ombres longues et douces, à la place de l'éclairage de nuit plat de Mapbox. Le ciel nocturne et vos couleurs restent")
        if (prefs.b(PreferenceKeys.MOONLIT, false))
            SPercent("Intensité de la lune", "Force avec laquelle la lune éclaire les murs et projette des ombres", prefs, PreferenceKeys.MOON_STR, 0, 200, 100)
        SToggle(prefs, "Ombres", PreferenceKeys.SHADOWS, true, "Les bâtiments et arbres projettent des ombres. Désactivé allège aussi le travail d'un autoradio lent")
        SToggle(prefs, "Soleil à sa vraie place", PreferenceKeys.SUN_REAL, false, "La lumière et les ombres viennent de la position réelle du soleil, et la nuit de la lune")
        // (Overcast light is in Settings > Weather, with everything else the weather changes)
    }
    // everything that sets the map's angle in one place (the switches below were in Turn card & lanes and On the map, where the
    // flat view when stopped wasn't found: bluelions25, #312)
    SGroup("Vue de la carte", footer = "Inclinaison : de 30° à 65°. Auto est le choix de l'appli (54°, ou 52° avec une carte allégée). L'inclinaison selon la vitesse et l'aplatissement dans les virages s'ajoutent par-dessus.") {
        STilt("Inclinaison sur écran large", "Autoradios en paysage. Plus haut montre plus loin (plus à dessiner pour un appareil lent)", prefs, PreferenceKeys.TILT_WIDE)
        STilt("Inclinaison sur écran vertical", "Écrans en portrait et téléphones", prefs, PreferenceKeys.TILT_TALL)
        STilt("Inclinaison sur Android Auto", "L'écran de la voiture", prefs, PreferenceKeys.TILT_CAR)
        SToggle(prefs, "Aplatir la carte en 2D aux virages", "turn2d", true)
        SToggle(prefs, "Inclinaison selon la vitesse", "speedtilt", true, "Plus plat à basse vitesse, vue plus longue à vitesse élevée")
        SToggle(prefs, "Aperçu du trafic à l'arrêt", PreferenceKeys.STOPPED_OVERVIEW, true, "Après 30 secondes à l'arrêt (feu, bouchon), la carte recule en vue du dessus du trafic autour de vous, puis revient quand vous repartez")
    }
    SGroup("3D") {
        SToggle(prefs, "Ombrage du relief", PreferenceKeys.HILLSHADE, false, "Un ombrage doux sur les pentes, pour voir vallées et crêtes même sur une carte à plat. Plus léger que le relief 3D")
        SToggle(prefs, "Bâtiments 3D", "buildings3d", true)
        SToggle(prefs, "Façades détaillées", "facades", true, "Fenêtres, portes et vitrines sur les bâtiments, là où Mapbox les a (surtout en centre-ville)")
        SToggle(prefs, "Monuments 3D", "landmarks3d", true, "Modèles détaillés de bâtiments célèbres, ponts et stades")
        SToggle(prefs, "Relief 3D", "terrain", false, "Le vrai relief. Superbe en région vallonnée, mais plus lourd pour un autoradio lent")
        SToggle(prefs, "Arbres dans les parcs", "trees", true, "Les arbres 3D de Mapbox dans les parcs et le long des rues")
        SToggle(prefs, "Dégager la route devant", "clearview", true, "Les arbres et monuments 3D de Mapbox sont masqués autour de votre flèche et, en itinéraire, au milieu de la route juste devant, pour ne jamais la cacher")
        SToggle(prefs, "Toucher pour mettre en évidence", "tapselect", true, "Touchez un bâtiment pour l'éclairer ; touchez un commerce ou un lieu, puis appui long pour y aller")
    }
    SGroup("Ciel et satellite") {
        SToggle(prefs, "Satellite", "satellite", false)
        SToggle(prefs, "Ciel nocturne en 3D", "nightsky", true, "Étoiles scintillantes au-dessus de l'horizon à la nuit tombée")
    }
}

// ------------------------------------------------------------------ Weather

/** Everything the weather changes, in one place: live weather and what it shows on the map, the light, advice, forecasts, radar. */
@Composable
private fun WeatherSection(prefs: Prefs) {
    val live = prefs.b(PreferenceKeys.RAIN_EFFECTS, false)
    val metKey = prefs.s(PreferenceKeys.METOFFICE_KEY, "").isNotBlank()
    SGroup("Météo en direct", footer = if (metKey) "Source : Met Office (clé dans Clés et comptes), Open-Meteo en cas d'échec."
        else "Source : Open-Meteo. Au Royaume-Uni, une clé Met Office gratuite (Clés et comptes) est souvent plus précise.") {
        // (the key is still "rainfx": switching it on is what fetches the weather where you are, every 10 minutes)
        SToggle(prefs, "Météo en direct", PreferenceKeys.RAIN_EFFECTS, false, "Vérifie la météo là où vous êtes toutes les 10 minutes, pour tout ce qui suit")
        if (live) {
            SToggle(prefs, "Pluie sur la carte", PreferenceKeys.WX_RAIN, true, "De la pluie qui tombe sur la carte, plus forte lors d'une averse ou d'un orage")
            SToggle(prefs, "Neige sur la carte", PreferenceKeys.WX_SNOW, true, "De la neige qui tombe sur la carte")
            SToggle(prefs, "Brouillard et brume sur la carte", PreferenceKeys.WX_FOG, true, "Un voile pâle sur la carte et une brume épaisse par brouillard. Désactivé, cela s'affiche comme un simple jour couvert")
            SToggle(prefs, "Brume vers l'horizon", PreferenceKeys.SKYWEATHER, true, "Le ciel selon la météo : gris par temps couvert, plus épais sous la pluie")
            SToggle(prefs, "Conseils météo", PreferenceKeys.WXADVICE, true, "Une note discrète par forte pluie, neige, brouillard ou verglas quand vous êtes proche de la limite")
            val st = com.headunit.launcher.MetOffice.status
            if (metKey && st.isNotBlank()) SNote(st)
        }
    }
    // (only Mapbox's map has its own light)
    if (prefs.mapSource() == "mapbox") SGroup("Légère") {
        // (one choice for the two old switches: "Always" already covered "In rain or snow", so both on was the same as Always)
        val overcast = if (prefs.b(PreferenceKeys.OVERCAST, false)) "always" else if (prefs.b(PreferenceKeys.WEATHER_LIGHT, false)) "wet" else "off"
        SChoice("Lumière couverte", listOf("off" to "Désactivé", "wet" to "Par pluie ou neige", "always" to "Toujours"), overcast,
            when (overcast) {
                "always" -> "Lumière douce sans ombres dures, plus reposante en cas d'éblouissement"
                "wet" -> if (live) "Une lumière du jour plus grise et plus douce quand il pleut ou neige là où vous êtes" else "Activez Météo en direct ci-dessus"
                else -> null
            }) {
            prefs.put(PreferenceKeys.OVERCAST, it == "always")
            prefs.put(PreferenceKeys.WEATHER_LIGHT, it == "wet")
        }
    }
    SGroup("Prévisions et radar") {
        SToggle(prefs, "Météo sur l'itinéraire", PreferenceKeys.ROUTEWX, true, "Les prévisions le long de votre trajet, pour le moment où vous y serez")
        SToggle(prefs, "Calque radar de pluie", PreferenceKeys.RAIN_RADAR, false, "Radar en direct de RainViewer, sous l'itinéraire")
    }
}

// ------------------------------------------------------------------ On the map

@Composable
private fun OnTheMapSection(prefs: Prefs, engine: NavEngine) {
    SGroup("Appui long sur la carte") {
        SToggle(prefs, "Qu'y a-t-il ici", PreferenceKeys.WHATS_HERE, true, "Un appui long sur la carte montre d'abord la route, sa limitation et le lieu, avec Y aller et Ajouter une étape. Désactivé : on y va directement, comme avant")
    }
    SGroup("Lieux enregistrés") {
        SToggle(prefs, "Lieux enregistrés sur la carte", PreferenceKeys.FAVS_ON_MAP, true, "Domicile, Travail, favoris et lieux étoilés. Touchez-en un pour y aller")
        if (prefs.b(PreferenceKeys.FAVS_ON_MAP, true))
            SSlider("Icônes des lieux enregistrés", prefs.i(PreferenceKeys.SAVED_ICON_SIZE, 100).coerceIn(50, 250).toFloat(), 50f..250f, "%", steps = 39, reset = 100f) { prefs.put(PreferenceKeys.SAVED_ICON_SIZE, it) }
    }
    SGroup("Trafic") {
        SToggle(prefs, "Trafic en direct", "traffic", true)
        SToggle(prefs, "Vue du trafic à l'arrêt", PreferenceKeys.STOPPED_OVERVIEW, true, "Après 30 secondes d'arrêt (feu, file), la carte recule pour montrer le trafic autour de vous vu de dessus, et revient quand vous repartez")
        SToggle(prefs, "Trafic uniquement sur l'itinéraire pendant le guidage", "routetrafficonly", false, "Désactivé : le trafic des routes autour de vous s'affiche aussi, comme dans Google Maps")
        SToggle(prefs, "Trafic fluide en vert", PreferenceKeys.TRAFFIC_GREEN, false, "Grands axes fluides en vert, en plus du trafic ralenti en orange et rouge")
        SToggle(prefs, "Couleurs du trafic fondues", PreferenceKeys.BLEND_TRAFFIC, true, "Sur le tracé et la barre du trajet, les couleurs du trafic se fondent l'une dans l'autre au lieu de changer par paliers")
    }
    SGroup("Sur votre itinéraire") {
        SToggle(prefs, "Feux tricolores", "signals", true, if (prefs.b(PreferenceKeys.SIGNALS, true)) "Seulement ceux de votre itinéraire. Actuellement : " + engine.map.signalInfo() else null)
        SToggle(prefs, "Icônes des radars et travaux", "hazardicons", true, "Leurs icônes sur la carte. Les alertes sont dans Réglages > Alertes")
        if (prefs.b("hazardicons", true)) SPercent("Taille des radars", "Le badge rouge des radars sur la carte", prefs, PreferenceKeys.CAM_SIZE, 50, 350)
        SToggle(prefs, "Passages piétons", "zebras", true, "Passages piétons de votre itinéraire peints sur la route, avec balises lumineuses (de près)")
        SToggle(prefs, "Panneaux stop", PreferenceKeys.STOP_SIGNS, true, "Stops de votre itinéraire affichés sur la carte, avec un petit signal sonore avant chacun (là où OpenStreetMap les connaît)")
        SToggle(prefs, "Caméras de lecture de plaques", PreferenceKeys.ALPR_CAMS, true, "Caméras Flock et autres lecteurs automatiques de plaques recensés sur OpenStreetMap. Affichées sur la carte seulement, sans alerte")
    }
    SGroup("Mobilier urbain 3D") {
        SToggle(prefs, "Lampadaires, feux et radars en 3D", "furniture3d", true, "Modèles 3D grandeur nature avec lampes et feux allumés (3D complète). Désactivé : badges et blocs simplifiés")
        SToggle(prefs, "Lampadaires", "lamps", true, if (prefs.b(PreferenceKeys.LAMPS, true)) "Actuellement : " + engine.map.lampInfo() else null)
        if (prefs.b(PreferenceKeys.LAMPS, true)) SToggle(prefs, "Lampadaires sur Android Auto", PreferenceKeys.CAR_LAMPS, true, "Aussi sur l'écran de la voiture (au prochain branchement, ou tout de suite depuis les réglages de la voiture)")
        if (prefs.b(PreferenceKeys.LAMPS, true)) SSlider("Espacement des lampadaires", prefs.s(PreferenceKeys.LAMPGAP, "40").toFloatOrNull() ?: 40f, 15f..120f, " m", reset = 40f) { prefs.put(PreferenceKeys.LAMPGAP, it.toString()) }
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
        SPictureChoice("Repère de position", null, listOf("arrow" to "Flèche 3D", "car" to "Voiture", "model" to "Vrai modèle 3D", "flat" to "Flèche plate", "custom" to "Votre image"), loc, { id, c -> locatorPicture(id, c) }) {
            if (it == "custom" && !hasPic) choosePic() else prefs.put(PreferenceKeys.LOCATOR, it)
        }
        // your own 3D model (.glb): picked, checked and measured, then turned to point the way you drive
        val own = OwnModel.info(prefs)
        val pickModel = rememberModelPicker(prefs) { why ->
            if (why == null) prefs.put(PreferenceKeys.MODEL_SHAPE, "own")
            else android.widget.Toast.makeText(ctx, if (why == "too big") "Ce modèle est trop lourd, même avec ses images réduites. Réduisez son nombre de polygones (dans l'appli qui l'a créé, ou avec Decimate dans Blender) et réessayez"
                else "Ce fichier n'est pas un modèle 3D .glb", android.widget.Toast.LENGTH_LONG).show()
        }
        fun chooseModel() { if (!pickModel()) android.widget.Toast.makeText(ctx, "Aucune application de cet appareil ne peut choisir un fichier (installez un gestionnaire de fichiers)", android.widget.Toast.LENGTH_LONG).show() }
        if (loc == "model") {
            SChoice("Forme du modèle", listOf("arrow" to "Flèche", "car" to "Voiture", "own" to "Votre modèle"), prefs.s(PreferenceKeys.MODEL_SHAPE, "arrow")) {
                if (it == "own" && own == null) chooseModel() else prefs.put(PreferenceKeys.MODEL_SHAPE, it)
            }
            if (prefs.s(PreferenceKeys.MODEL_SHAPE, "arrow") == "own" && own != null) {
                SItem("Fichier du modèle 3D", own.first + " · " + String.format(java.util.Locale.UK, "%.1f MB", own.second / (1024.0 * 1024.0))) { SButton("Modifier") { chooseModel() } }
                SNote("Un modèle .glb. Au-delà de 8 Mo, ses images sont réduites pour qu'il passe. Orientez-le dans le sens de la marche avec Tourner ; au-delà de 3 Mo, il peut saccader sur les autoradios lents")
                if (engine.map.ownFailed) SNote("Votre modèle 3D n'a pas pu être affiché, la voiture intégrée est donc utilisée")
                SSlider("Rotation", prefs.s(PreferenceKeys.MODEL_TURN, "0").toFloatOrNull() ?: 0f, 0f..345f, "°", steps = 22, reset = 0f) { prefs.put(PreferenceKeys.MODEL_TURN, it.toString()) }
            }
            SToggle(prefs, "Halo", PreferenceKeys.MODEL_GLOW, true, "Un doux halo bleu autour du modèle, éclairé vivement")
            if (prefs.mapDetail() != "full") SNote("Le vrai modèle 3D s'affiche avec Détail de la carte : 3D complète. En Allégée et La plus légère, l'image de la même forme est utilisée.")
        }
        val ownShape = loc == "model" && prefs.s(PreferenceKeys.MODEL_SHAPE, "arrow") == "own" && own != null
        val shapeCar = loc == "car" || (loc == "model" && prefs.s(PreferenceKeys.MODEL_SHAPE, "arrow") == "car")
        val shapeArrow = loc == "arrow" || (loc == "model" && !shapeCar && !ownShape)
        if (loc == "custom") {
            SItem("Image", "Un PNG à fond transparent rend le mieux. Orientez son haut dans le sens de la marche") { SButton(if (hasPic) "Changer" else "Choisir") { choosePic() } }
            SToggle(prefs, "Tourne avec vous", PreferenceKeys.LOCATOR_IMAGE_TURNS, true, "À plat sur la route et tourne avec vous, comme la flèche. Désactivé : reste droite")
            SToggle(prefs, "Retirer le fond blanc", PreferenceKeys.LOCATOR_IMAGE_CLEAR, true, "Pour une image enregistrée sur fond blanc plutôt que transparent : le blanc autour devient transparent. Une image déjà transparente n'est pas modifiée")
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
        SSlider(if (ownShape) "Taille du modèle" else if (shapeCar) "Taille de la voiture" else if (loc == "custom") "Taille de l'image" else "Taille de la flèche", prefs.s(PreferenceKeys.ARROW_PERCENT, "240").toFloatOrNull() ?: 240f, 60f..500f, "%", reset = 240f) { prefs.put(PreferenceKeys.ARROW_PERCENT, it.toString()) }
        SToggle(prefs, "Faisceau des phares la nuit", PreferenceKeys.BEAM, true, "Un doux cône de lumière sur la route devant vous la nuit, en roulant")
    }
    SGroup("Position de la flèche") {
        SToggle(prefs, "Garder la flèche sur la route sans itinéraire", "snaproad", true)
        SChoice("Avance de la flèche", listOf("0" to "Aucune", "0.5" to "0,5 s", "1.0" to "1 s", "1.5" to "1,5 s", "2.0" to "2 s"), prefs.s("arrowlead", "1.0"),
            "Compense le retard du GPS : la flèche est placée là où vous serez dans ce délai. Augmentez si elle arrive aux croisements après la voiture, baissez si elle les dépasse avant") { prefs.put("arrowlead", it) }
        SToggle(prefs, "Garder la flèche sur le côté", PreferenceKeys.ARROW_SIDE, false, "Quand les cartes se replient, au lieu de revenir au centre")
    }
    SGroup("Tracé de l'itinéraire") {
        SToggle(prefs, "Drapeaux 3D", PreferenceKeys.FLAGS_3D, true, "Un grand drapeau à votre destination et de plus petits orange à vos arrêts, au-dessus des bâtiments pour les voir de loin")
        SToggle(prefs, "Flèche de direction sur l'itinéraire", "turnarrows", true, "Une flèche blanche à travers la prochaine intersection ou le rond-point")
        SToggle(prefs, "Chevrons le long de l'itinéraire", "routechevrons", true, "Petites flèches sur la ligne en zoom rapproché")
        SToggle(prefs, "Ruban d'itinéraire", "ribbon", true, "Une ombre douce sous la ligne, et l'itinéraire visible à travers les bâtiments 3D devant")
        SToggle(prefs, "Lumière qui défile sur l'itinéraire", "routeflow", true)
        SColours("Couleur des itinéraires alternatifs", listOf(
            "#C4D9E6" to "Gris-bleu pâle (par défaut)", "#A9D4F0" to "Bleu clair", "#5AB7F5" to "Bleu", "#315CBA" to "Bleu foncé",
            "#A9B4C2" to "Gris argent", "#FFFFFF" to "Blanc", "#42CFA5" to "Menthe", "#F4C542" to "Jaune",
            "#FF9A4A" to "Orange", "#F05D6C" to "Rouge", "#C58AF9" to "Violet", "#F08AC5" to "Rose",
        ), prefs.s(PreferenceKeys.ALT_ROUTE_COLOUR, "#C4D9E6")) { prefs.put(PreferenceKeys.ALT_ROUTE_COLOUR, it) }
    }
}

// ------------------------------------------------------------------ Routes

@Composable
private fun RoutesSection(prefs: Prefs, engine: NavEngine) {
    VehicleGroup(prefs, engine.imperial)
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
        SToggle(prefs, "Éviter les trains autos", PreferenceKeys.AVOID_TRAIN, false, "Les trains qui transportent votre voiture, comme la navette du tunnel sous la Manche ou les trains des tunnels alpins")
        SToggle(prefs, "Éviter les routes non goudronnées", "avoid_unpaved", true)
        SToggle(prefs, "Éviter les ZFE (zones à faibles émissions)", "avoidzones", false, "Une zone où vous partez ou arrivez ne peut pas être évitée")
        ZonesItem()
    }
    SGroup("Choix de l'itinéraire") {
        SToggle(prefs, "Itinéraires éco", "route_eco", false, "Économes en carburant plutôt que les plus rapides")
        SToggle(prefs, "Préférer les itinéraires plus simples", "prefer_simple", true, "Quand un itinéraire n'est qu'environ une minute plus lent mais a bien moins de virages, il est choisi en premier (indiqué La plus simple). Désactivé choisit toujours le plus rapide")
        SToggle(prefs, "Proposer des itinéraires plus rapides", "fasterroute", true)
        if (prefs.b(PreferenceKeys.FASTER_ROUTE, true)) SToggle(prefs, "Garder les autres itinéraires sur la carte", "livealts", true, "Lignes grises avec +/− minutes à toucher à tout moment. Désactivé affiche plutôt une carte « Itinéraire plus rapide trouvé »")
        SToggle(prefs, "Suggérer votre destination", "predict", true, "Avec le temps de trajet en direct, y compris domicile et travail à vos heures habituelles")
        SToggle(prefs, "Arriver du bon côté de la route", PreferenceKeys.ARRIVE_SIDE, true, "L'itinéraire vous amène du côté trottoir de votre destination, sans traverser la circulation. Itinéraires Mapbox et hors ligne")
    }
    SGroup("Démarrer un itinéraire") {
        SToggle(prefs, "Compte à rebours de démarrage auto", PreferenceKeys.AUTO_START_ROUTE, true, "Démarrer automatiquement la navigation à la fin du compte à rebours")
        if (prefs.b(PreferenceKeys.AUTO_START_ROUTE, true)) SSlider("Compte à rebours", prefs.i(PreferenceKeys.AUTO_START_SECONDS, 10).coerceIn(3, 30).toFloat(), 3f..30f, " s", sub = "Délai avant le démarrage de la navigation", steps = 26, reset = 10f) { prefs.put(PreferenceKeys.AUTO_START_SECONDS, it.coerceIn(3, 30)) }
        SToggle(prefs, "Survoler un nouvel itinéraire", "cinematic", true, "Un survol 3D de 3 secondes avant l'aperçu. Touchez la carte pour passer")
        SToggle(prefs, "Bouton Survoler l'itinéraire", "flyover", true, "Dans les options d'itinéraire : un survol 3D de tout le trajet avant de partir")
    }
}

/** Car or lorry; for a lorry its size and weight, which every route is then planned for (see Vehicle). */
@Composable
private fun VehicleGroup(prefs: Prefs, imperial: Boolean) {
    val lorry = prefs.s(PreferenceKeys.VEHICLE, "car") == "lorry"
    SGroup("Véhicule", footer = if (lorry) "Utilisé par TomTom et les itinéraires hors ligne, et par Mapbox pour la hauteur, la largeur et le poids. Les limites viennent des données de la carte et certaines peuvent manquer : respectez toujours la signalisation." else null) {
        SChoice("Véhicule", listOf("car" to "Voiture", "lorry" to "Poids lourd"), if (lorry) "lorry" else "car",
            if (lorry) "Les itinéraires évitent ponts bas, limites de poids et interdictions poids lourds selon votre gabarit" else null) { prefs.put(PreferenceKeys.VEHICLE, it) }
        // your top speed for arrival times (vans, lorries, towing), in the units you drive in (GitHub #134)
        run {
            val c = LocalVw.current
            val mph = imperial
            val steps = if (mph) Vehicle.TOP_MPH else Vehicle.TOP_KMH
            val savedUnit = prefs.s(PreferenceKeys.TOP_SPEED_UNIT, "mph")
            val savedV = prefs.i(PreferenceKeys.TOP_SPEED, 0)
            // a value saved in the other unit is shown at the nearest step of this one
            val cur = if (savedV <= 0) 0 else {
                val inUnit = if ((savedUnit == "mph") == mph) savedV.toDouble() else if (mph) savedV / 1.609344 else savedV * 1.609344
                steps.filter { it > 0 }.minByOrNull { kotlin.math.abs(it - inUnit) } ?: 0
            }
            val idx = steps.indexOf(cur).coerceAtLeast(0)
            fun save(v: Int) { prefs.put(PreferenceKeys.TOP_SPEED_UNIT, if (mph) "mph" else "kmh"); prefs.put(PreferenceKeys.TOP_SPEED, v); Vehicle.load(prefs) }
            SItem("Vitesse maximale pour les heures d'arrivée", "Les itinéraires sont chronométrés sans dépasser cette vitesse : pour les utilitaires, poids lourds ou avec remorque. Désactivé utilise les vitesses normales") {
                SButton("−") { save(steps[(idx - 1).coerceAtLeast(0)]) }
                Text(if (cur == 0) "Désactivé" else "$cur ${if (mph) "mph" else "km/h"}", color = c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.widthIn(min = 92.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                SButton("+") { save(steps[(idx + 1).coerceAtMost(steps.size - 1)]) }
            }
        }
        if (lorry) {
            SStep("Hauteur", "Chargement compris. Sert à vérifier les ponts bas", prefs, PreferenceKeys.LORRY_HEIGHT, 400, 200, 500, 5) { Vehicle.heightText(it, imperial) }
            SStep("Largeur", "Sans les rétroviseurs", prefs, PreferenceKeys.LORRY_WIDTH, 255, 180, 300, 5) { String.format(java.util.Locale.US, "%.2f m", it / 100.0) }
            SStep("Longueur", "Tracteur et remorque", prefs, PreferenceKeys.LORRY_LENGTH, 1650, 500, 2500, 50) { String.format(java.util.Locale.US, "%.1f m", it / 100.0) }
            SStep("Poids", "En charge", prefs, PreferenceKeys.LORRY_WEIGHT, 440, 35, 600, 5) { tonnes(it) }
            SStep("Poids par essieu", "Essieu le plus chargé", prefs, PreferenceKeys.LORRY_AXLE, 115, 50, 200, 5) { tonnes(it) }
            SToggle(prefs, "Matières dangereuses", PreferenceKeys.LORRY_HAZMAT, false, "Évite les tunnels et routes interdits aux matières dangereuses")
        }
    }
}

private fun tonnes(t10: Int): String = if (t10 % 10 == 0) "${t10 / 10} t" else String.format(java.util.Locale.US, "%.1f t", t10 / 10.0)

/** A tilt in degrees with a slider, or Auto (0) for the app's own choice. Saved when you let go. */
@Composable
private fun STilt(title: String, sub: String, prefs: Prefs, key: String) {
    val c = LocalVw.current
    val saved = prefs.i(key, 0)
    var v by remember(key, saved) { mutableStateOf(if (saved in 30..65) saved.toFloat() else 54f) }
    var moving by remember(key, saved) { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        SItem(title, sub) {
            Text(if (saved in 30..65 || moving) "${v.roundToInt()}°" else "Auto", color = c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            if (saved in 30..65) SButton("Auto") { prefs.put(key, 0) }
        }
        androidx.compose.material3.Slider(
            value = v, onValueChange = { v = it; moving = true }, onValueChangeFinished = { prefs.put(key, v.roundToInt().coerceIn(30, 65)) },
            valueRange = 30f..65f, steps = 34,
            colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = c.accent, activeTrackColor = c.accent, inactiveTrackColor = c.tile),
        )
    }
}

/** A percentage with a slider ([min] to [max], in steps of 10), and Reset back to [def]. Saved when you let go. */
@Composable
internal fun SPercent(title: String, sub: String, prefs: Prefs, key: String, min: Int, max: Int, def: Int = 100) {
    val c = LocalVw.current
    val saved = prefs.i(key, def).coerceIn(min, max)
    var v by remember(key, saved) { mutableStateOf(saved.toFloat()) }
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        SItem(title, sub) {
            Text("${v.roundToInt()}%", color = c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            if (saved != def) SButton("Réinitialiser") { prefs.put(key, def) }
        }
        androidx.compose.material3.Slider(
            value = v, onValueChange = { v = (it / 10f).roundToInt() * 10f }, onValueChangeFinished = { prefs.put(key, v.roundToInt().coerceIn(min, max)) },
            valueRange = min.toFloat()..max.toFloat(), steps = (max - min) / 10 - 1,
            colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = c.accent, activeTrackColor = c.accent, inactiveTrackColor = c.tile),
        )
    }
}

/** A number set with − and + (a lorry's measurements): [show] turns the stored value into what's shown. */
@Composable
private fun SStep(title: String, sub: String, prefs: Prefs, key: String, def: Int, min: Int, max: Int, by: Int, show: (Int) -> String) {
    val c = LocalVw.current
    val v = prefs.i(key, def).coerceIn(min, max)
    SItem(title, sub) {
        SButton("−") { prefs.put(key, (v - by).coerceAtLeast(min)) }
        Text(show(v), color = c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.widthIn(min = 92.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        SButton("+") { prefs.put(key, (v + by).coerceAtMost(max)) }
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
    }
    SGroup("Voies et carrefours") {
        SToggle(prefs, "Images des échangeurs", PreferenceKeys.JUNCTION_PICS, false, "Les images Mapbox des échangeurs d'autoroute, avec les voies à prendre, dans le dernier kilomètre. Seulement là où Mapbox en a, et pour les comptes Mapbox auxquels il les propose")
        SToggle(prefs, "Voies sur la carte", "lanemap", true)
        if (prefs.b(PreferenceKeys.LANEMAP, true)) SPictureChoice(
            "Voies affichées en",
            when (prefs.s(PreferenceKeys.LANE_MAP_STYLE, "lit")) {
                "closeup" -> "Un dessin des voies du carrefour sous la carte de direction, sur les 400 derniers mètres. Rien n'est dessiné sur la carte"
                "sign" -> "Un panneau au-dessus de la route au carrefour, avec les voies à prendre éclairées"
                else -> "La voie à prendre éclairée sur la route. Sur autoroute, quand les vraies voies sont connues, le tracé s'y déplace"
            },
            listOf("lit" to "Voie éclairée", "closeup" to "Vue rapprochée", "sign" to "Panneau"), prefs.s(PreferenceKeys.LANE_MAP_STYLE, "lit"), { id, c -> lanePicture(id, c) },
        ) { prefs.put(PreferenceKeys.LANE_MAP_STYLE, it) }
        SToggle(prefs, "Zoomer sur les voies avant une intersection", "laneview", true)
        SToggle(prefs, "Intersection en gros plan", "junctionview", true, "Aux bretelles, sorties d'autoroute, bifurcations et séparations de voies, la vue se rapproche et s'aplatit un peu sur les 200 derniers mètres")
    }
    SGroup("Carte de trajet") {
        SToggle(prefs, "Barre d'itinéraire sur la carte de trajet", "routebar", true, "Trafic, radars, aires et étapes, avec votre progression")
        SToggle(prefs, "Carte de la prochaine aire", "servicescard", true)
        SToggle(prefs, "Radar des dangers", "hazardradar", false, (if (mi) "Les 2 prochains miles" else "Les 3 prochains km") + " : virages, passages, écoles, ralentisseurs, radars et contrôles")
    }
    SGroup("Arrivée") {
        SToggle(prefs, "Moment d'arrivée", "arrivalfx", true, "La caméra plonge vers votre destination et montre le parking le plus proche")
        SToggle(prefs, "Mettre en lumière votre destination", PreferenceKeys.ARRIVAL_PLACE, true, "À l'arrivée, le bâtiment s'illumine, son entrée est indiquée et une ligne pointillée montre le trajet à pied depuis le parking")
        SToggle(prefs, "Parking près de la destination", "parkprompt", true, (if (mi) "Environ un mile" else "Environ 1,5 km") + " avant l'arrivée, les parkings proches avec le temps de marche depuis chacun")
        SToggle(prefs, "Itinéraire retour", "routeback", true, "Sur la carte d'arrivée : un seul appui pour revenir à votre point de départ")
        SToggle(prefs, "Notification « Voiture garée »", "parkednotice", true, "Quand Android Auto se déconnecte à l'arrêt : une notification avec l'heure et un bouton « À pied » pour revenir à la voiture")
    }
    SGroup("Autres écrans") {
        SToggle(prefs, "Direction suivante dans la notification", PreferenceKeys.TURN_NOTIF, true, "La flèche, la distance et la route dans la notification d'Horizon Nav pendant le guidage, pour les lanceurs de voiture, les montres et les écrans qui affichent les notifications de navigation")
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
    // fork FR : pour chaque type d'alerte, l'affichage à l'écran, le bip et l'annonce vocale se choisissent séparément
    SGroup("Comment être prévenu", "Pour chaque alerte : Écran (la bannière), Bip et Voix (l'annonce). Touchez pour activer ou couper") {
        for ((g, label) in listOf(
            "camera" to "Radars (fixes et tronçons)", "speed" to "Excès de vitesse (Écran : le compteur passe au rouge)", "limit" to "Changement de limitation", "bend" to "Virages dangereux",
            "stop" to "Stops", "bump" to "Ralentisseurs", "school" to "Écoles", "rail" to "Passages à niveau et gués",
            "incident" to "Incidents et routes fermées", "zone" to "Zones à faibles émissions", "wrongway" to "Contresens",
            "rain" to "Pluie et neige sur le trajet", "break" to "Pause conseillée", "plan" to "Trajets planifiés",
        )) AlertWaysRow(prefs, engine, g, label)
    }
    SGroup("Vitesse") {
        SToggle(prefs, "Avertir en cas d'excès de vitesse", "speedwarn", true)
        if (prefs.b(PreferenceKeys.SPEEDWARN, true)) SChoice("Avertir au-delà de", listOf("0", "5", "10").map { it to it + if (engine.imperial) " mph" else " km/h" }, prefs.s(PreferenceKeys.TRAFFIC_WARNING, "5")) { prefs.put(PreferenceKeys.TRAFFIC_WARNING, it) }
        SToggle(prefs, "Faire clignoter le panneau en excès", PreferenceKeys.FLASH_OVER, true, "Le panneau de limitation clignote quand vous dépassez de plus que la marge ci-dessous")
        if (prefs.b(PreferenceKeys.FLASH_OVER, true)) {
            val pct = prefs.b(PreferenceKeys.FLASH_PCT, false)
            val unit = if (pct) "%" else if (engine.imperial) " mph" else " km/h"
            SChoice("Marge en", listOf("speed" to (if (engine.imperial) "mph" else "km/h"), "pct" to "% de la limitation"), if (pct) "pct" else "speed") { prefs.put(PreferenceKeys.FLASH_PCT, it == "pct") }
            SChoice("En agglomération", listOf("5", "10", "15", "20").map { it to it + unit }, prefs.s(PreferenceKeys.FLASH_TOWN, "10"),
                "Limitations jusqu'à 60 km/h") { prefs.put(PreferenceKeys.FLASH_TOWN, it) }
            SChoice("Hors agglomération", listOf("5", "10", "15", "20").map { it to it + unit }, prefs.s(PreferenceKeys.FLASH_OUT, "15")) { prefs.put(PreferenceKeys.FLASH_OUT, it) }
        }
        SToggle(prefs, "Dire « Ralentissez »", PreferenceKeys.SAY_SLOW_DOWN, false, "Le mot « Ralentissez » après le bip quand vous dépassez la limitation, au lieu du bip seul")
        SChoice("Alerte de vitesse maximale", (if (engine.imperial) listOf("0", "60", "70", "80", "90") else listOf("0", "90", "100", "110", "120", "130", "140")).map { it to (if (it == "0") "Désactivé" else it + if (engine.imperial) " mph" else " km/h") },
            prefs.s(PreferenceKeys.ALERT_TOP_SPEED, "0"), "Un bip et « Ralentissez » quand vous atteignez cette vitesse, quelle que soit la limitation") { prefs.put(PreferenceKeys.ALERT_TOP_SPEED, it) }
        SToggle(prefs, "Annoncer les changements de limitation", "limitchange", true)
        SToggle(prefs, "Ignorer les hausses sur un petit tronçon", "limithold", true, "Une limite plus haute n'est prise qu'après 200 m dessus : un petit tronçon mal renseigné (un 90 entre un 50 et un 70) n'est ni affiché ni annoncé. Les baisses sont prises tout de suite")
        SToggle(prefs, "50 en agglomération", "towncap", true, "Entre les panneaux d'entrée et de sortie d'une agglomération (d'après OpenStreetMap), une limite à 80 ou 90 lue sur la carte est ramenée à 50 : c'est presque toujours une erreur de la carte")
        SToggle(prefs, "Prochaine limitation", PreferenceKeys.NEXT_LIMIT, true, "La prochaine limitation sur votre trajet, en panneau sur la carte là où elle change et en petit à côté de votre panneau avec la distance, dès 800 m environ")
        SChoice("Source des limitations", listOf("mg4" to "Comme MG4Dash", "mix" to "MG4Dash + TomTom", "auto" to "TomTom et Mapbox"), prefs.s("limitsrc", "mg4"),
            when (prefs.s("limitsrc", "mg4")) {
                "mg4" -> "OpenStreetMap seul, avec les paquets France de MG4Dash (jeton GitHub ayant accès à MG4Dash-SWI68), sinon par petites zones gardées 60 jours"
                "mix" -> "Les paquets MG4Dash (OpenStreetMap), corrigés par les relevés de TomTom quand il en a (clé TomTom nécessaire, une lecture toutes les 6 s environ). Utile là où OpenStreetMap n'a pas de limitation ou se trompe"
                else -> "TomTom ou Mapbox d'abord, OpenStreetMap en secours (comme l'appli d'origine)"
            }) { prefs.put("limitsrc", it) }
        SToggle(prefs, "Afficher les limitations estimées", "limitest", false, "Sans limitation connue, affiche la limitation habituelle pour ce type de route (grisée). Désactivé : aucun panneau plutôt qu'une estimation")
    }
    SGroup("Radars", footer = "Radars : liste officielle du ministère de l'Intérieur, sens contrôlé et radars de chantier de Lufop (lufop.net, CC BY-SA 4.0) et d'OpenStreetMap.") {
        SToggle(prefs, "Alertes radar", "cameras", true, "Alertes vocales et à l'écran à l'approche d'un radar")
        if (prefs.b(PreferenceKeys.CAMERA_WARNINGS, true)) SToggle(prefs, "Désactivés là où c'est interdit", "cambanobey", true, "La France, l'Allemagne et la Suisse interdisent les alertes radar : elles s'y coupent d'elles-mêmes")
        SToggle(prefs, "Radars tronçons", "avgzones", true, "Affiche votre moyenne par rapport à la limite")
    }
    SGroup("Dangers", footer = "Ralentisseurs, écoles, passages à niveau et gués viennent d'OpenStreetMap : certains peuvent manquer.") {
        SToggle(prefs, "Travaux et incidents", "incidents", true)
        if (prefs.b("incidents", true)) SToggle(prefs, "Alertes travaux", "roadworkswarn", true, "Désactivé : plus de bandeau ni d'annonce pour les travaux. Accidents et fermetures restent signalés")
        SToggle(prefs, "Alerte contresens", "wrongway", true, "Si vous vous engagez à contresens dans une rue à sens unique")
        SToggle(prefs, "Ralentisseurs", "bumps", true)
        SToggle(prefs, "Virages dangereux", PreferenceKeys.BEND_WARN, true, "Les virages où il faut ralentir sont rayés de jaune sur l'itinéraire, avec une alerte et une vitesse confortable avant chacun")
        SToggle(prefs, "Écoles aux heures d'entrée et de sortie", "schools", true)
        SToggle(prefs, "Passages à niveau et gués", "railford", true, "Un panneau et une alerte vocale pour les passages à niveau et les gués de votre itinéraire")
    }
    SGroup("Zones et rappels") {
        SToggle(prefs, "Zones à faibles émissions (ZFE)", "zonewarn", true)
        SToggle(prefs, "Rappel de pause après 2 heures", "breakremind", true)
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
    SGroup("Instructions") {
        SChoice("Guidage vocal", listOf("0" to "Tout", "1" to "Alertes seules", "2" to "Désactivé"), engine.soundMode.toString(),
            "Alertes seules : pas d'instructions de direction, mais radars, limitations et dangers restent annoncés") { prefs.put(PreferenceKeys.SOUND_MODE, it.toInt()) }
        SToggle(prefs, "Annoncer les noms de rues", PreferenceKeys.VOICE_ROADS, true, "Désactivé : seulement la direction, « Dans 200 mètres, tournez à gauche », sans la rue ni les villes des panneaux")
        SToggle(prefs, "Repères dans les indications", PreferenceKeys.LANDMARK_DIRS, true, "« Aux feux, tournez à droite » au lieu de la distance quand le virage est à des feux ou à un stop, sur la carte de direction et dans la voix")
        SToggle(prefs, "Instructions plus intelligentes", "smartvoice", true, "Numéros de sortie et directions des panneaux, la manœuvre suivante quand elle suit de près, et moins d'annonces anticipées sur vos routes habituelles")
    }
    SGroup("Voix et volume") {
        SChoice("Canal audio de la voix", listOf("nav" to "Navigation", "media" to "Média", "notif" to "Notification", "alarm" to "Alarme", "ring" to "Sonnerie"), prefs.s(PreferenceKeys.VOICE_CHANNEL, "nav"),
            "Si vous entendez les bips mais pas la voix, choisissez Média : certains autoradios ne jouent pas le canal navigation. Si la voix suit le volume de la musique, essayez Notification, Alarme ou Sonnerie (certains autoradios appellent l'un d'eux Diffusion)", wrap = true) { prefs.put(PreferenceKeys.VOICE_CHANNEL, it) }
        if (android.os.Build.VERSION.SDK_INT >= 28 && !engine.isCar) SChoice("Sortie de la voix", listOf("auto" to "Automatique", "phone" to "Haut-parleur du téléphone"), prefs.s(PreferenceKeys.VOICE_OUT, "auto"),
            "Haut-parleur du téléphone : les indications et les bips restent sur le téléphone pendant que la musique passe en Bluetooth. Pas pour Android Auto, qui utilise les haut-parleurs de la voiture") { prefs.put(PreferenceKeys.VOICE_OUT, it) }
        SToggle(prefs, "Mettre la musique en pause pour la voix", PreferenceKeys.VOICE_PAUSES_MUSIC, false, "Arrête la musique pendant les indications et la reprend après, au lieu de seulement la baisser. Pour quand la baisse ne suffit pas : de combien la musique baisse dépend de votre appli de musique")
        SToggle(prefs, "Jouer la voix comme les bips", PreferenceKeys.VOICE_VIA_APP, false, "À activer si la musique baisse pour les bips mais pas pour la voix : certains autoradios ne la baissent que pour les sons joués par l'appli elle-même")
        SItem("Voix et langue", "Utilise la synthèse vocale du système (installez la voix française)") { SButton("Ouvrir") { openSystemSettings(ctx, "com.android.settings.TTS_SETTINGS") } }
        SItem("Volume") { SButton("Ouvrir") { openSystemSettings(ctx, Settings.ACTION_SOUND_SETTINGS) } }
        SSlider("Volume des bips", prefs.i(PreferenceKeys.CHIME_VOL, 70).coerceIn(5, 100).toFloat(), 5f..100f, "%",
            sub = "Les bips d'excès de vitesse, de changement de limite et de radars, par rapport à la voix. Le volume de l'autoradio, réglé pendant qu'il parle, change les deux",
            reset = 70f) { prefs.put(PreferenceKeys.CHIME_VOL, it.toInt()); engine.testChime() }
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

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun PlacesSection(engine: NavEngine) {
    val pl = engine.places
    val importContext = LocalContext.current
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
    // (filed in groups if you like, GitHub #138: a group folds open to its places; each place has a Group button)
    var grouping by remember { mutableStateOf<String?>(null) }
    var openGroups by remember { mutableStateOf(setOf<String>()) }
    var renamingGroup by remember { mutableStateOf<String?>(null) }
    val groups = remember(raw) { pl.groups() }
    var importingGoogle by remember { mutableStateOf(false) }
    val googleMapsPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) {
            importingGoogle = false
        } else {
            Thread {
                val outcome = try {
                    val json = importContext.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
                    val places = com.headunit.launcher.GoogleMapsImport.parse(json)
                    // (into Your places, in a Google Maps group and not starred, so 50 imported places don't fill the row next to
                    // Home and Work: GitHub #261)
                    if (places.isEmpty()) null else pl.importToPlaces(places, "Google Maps")
                } catch (_: Throwable) { null }
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    importingGoogle = false
                    if (outcome == null) engine.toast("Aucun lieu enregistré trouvé dans ce fichier", 3500)
                    else engine.toast("${outcome.first} importés dans Vos lieux, dans le groupe Google Maps · déjà enregistrés ${outcome.second}", 5000)
                }
            }.start()
        }
    }
    SGroup("Vos lieux", footer = "Ceux avec une étoile s’affichent à côté de Domicile et Travail sous la barre de recherche ; touchez une étoile pour changer. Tous sont dans la recherche et le raccourci Vos lieux.") {
        if (mine.isEmpty()) SNote("Pas encore de lieux à vous.")
        @Composable
        fun placeRow(i: Int, lp: com.headunit.launcher.LabeledPlace, indent: Boolean) {
            val ind = if (indent) Modifier.padding(start = 18.dp) else Modifier
            Column(ind) {
                if (renaming == lp.label) {
                    var t by remember(lp.label) { mutableStateOf(lp.label) }
                    SItem("Renommer ${lp.label}") {
                        SField(t, { t = it.take(24) }, width = 220.dp)
                        SButton("Enregistrer", primary = true) { pl.renameCustom(lp.label, t); renaming = null }
                    }
                } else Row(verticalAlignment = Alignment.CenterVertically) {
                    // the star: shown next to Home and Work (yellow) or not (grey)
                    Icon(if (lp.pinned) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        if (lp.pinned) "Affiché à côté de Domicile et Travail" else "Non affiché à côté de Domicile et Travail",
                        tint = if (lp.pinned) Color(0xFFFFC928) else LocalVw.current.sub,
                        modifier = Modifier.size(40.dp).clip(CircleShape).clickable { pl.setPinned(lp.label, !lp.pinned) }.padding(8.dp))
                    Box(Modifier.weight(1f)) { SItem(lp.label, lp.place.name + if (lp.place.sub.isNotBlank()) ", " + lp.place.sub else "") {
                    if (i > 0) SButton("↑") { pl.moveCustom(lp.label, -1) }
                    SButton("Groupe") { grouping = if (grouping == lp.label) null else lp.label }
                    SButton("Renommer") { renaming = lp.label }
                    SButton("Supprimer", danger = true) { pl.removeCustom(lp.label) }
                } }
                }
                if (grouping == lp.label) {
                    var newName by remember(lp.label) { mutableStateOf("") }
                    androidx.compose.foundation.layout.FlowRow(
                        Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        for (g in groups) if (g != lp.group) SButton(g) { pl.setGroup(lp.label, g); grouping = null; openGroups = openGroups + g }
                        SField(newName, { newName = it.take(24) }, placeholder = "Nouveau groupe", width = 180.dp)
                        if (newName.isNotBlank()) SButton("Ajouter", primary = true) { pl.setGroup(lp.label, newName); openGroups = openGroups + newName.trim(); grouping = null }
                        if (lp.group.isNotBlank()) SButton("Aucun groupe") { pl.setGroup(lp.label, ""); grouping = null }
                        SButton("Annuler") { grouping = null }
                    }
                }
            }
        }
        // the groups first, each folding open; then the places in no group
        for (g in groups) {
            val inG = mine.withIndex().filter { it.value.group == g }
            val open = g in openGroups
            if (renamingGroup == g) {
                var t by remember(g) { mutableStateOf(g) }
                SItem("Renommer $g") {
                    SField(t, { t = it.take(24) }, width = 220.dp)
                    SButton("Enregistrer", primary = true) { pl.renameGroup(g, t); if (open) openGroups = openGroups - g + t.trim(); renamingGroup = null }
                }
            } else SItem((if (open) "▾ " else "▸ ") + g, if (inG.size == 1) "1 lieu" else "${inG.size} lieux",
                onClick = { openGroups = if (open) openGroups - g else openGroups + g }) {
                SButton("Renommer") { renamingGroup = g }
            }
            if (open) for ((i, lp) in inG) placeRow(i, lp, true)
        }
        mine.forEachIndexed { i, lp -> if (lp.group.isBlank()) placeRow(i, lp, false) }
        SItem("Ajouter un lieu") { SButton("Ajouter", primary = true) { adding = true } }
        SItem("Importer depuis Google Maps", "Choisissez un fichier Saved Places.json de Google Takeout. Les lieux vont ici dans un groupe Google Maps") {
            SButton(if (importingGoogle) "Importation…" else "Choisir", primary = true) {
                if (!importingGoogle) {
                    importingGoogle = true
                    try { googleMapsPicker.launch(arrayOf("application/json", "text/plain", "*/*")) }
                    catch (_: Exception) { importingGoogle = false; engine.toast("Aucun lieu enregistré trouvé dans ce fichier", 3500) }
                }
            }
        }
    }
    if (adding) AddPlaceDialog(engine, null) { adding = false }
    val favRaw = engine.prefs.s(PreferenceKeys.PLACES_FAVOURITES, "")
    val storedFavs = remember(favRaw) { pl.favourites() }
    var favSortNearest by rememberSaveable { mutableStateOf(false) }
    val hereForFavs = engine.fix?.pos
    val favs = remember(storedFavs, favSortNearest, hereForFavs, L.lang) {
        if (favSortNearest && hereForFavs != null) storedFavs.sortedBy { Geo.dist(hereForFavs, it.pos) }
        else {
            val collator = java.text.Collator.getInstance(L.locale)
            storedFavs.sortedWith { a, b -> collator.compare(a.name, b.name) }
        }
    }
    var renamingFav by remember { mutableStateOf<Place?>(null) }
    var slotFor by remember { mutableStateOf<Place?>(null) }
    SGroup("Favoris") {

        if (favs.size > 1) SItem("Trier les favoris") {
            SButton("A–Z", primary = !favSortNearest) { favSortNearest = false }
            SButton("Le plus proche", primary = favSortNearest) { favSortNearest = true }
        }
        if (favs.isEmpty()) SNote("Aucun favori pour l'instant. Touchez l'étoile d'une destination pour l'ajouter.")
        favs.forEach { f ->
            when {
                renamingFav == f -> {
                    var t by remember(f) { mutableStateOf(f.name) }
                    SItemStacked("Renommer ${f.name}") {
                        SField(t, { t = it.take(40) }, width = 220.dp)
                        SButton("Enregistrer", primary = true) { pl.renameFavourite(f, t); renamingFav = null }
                    }
                }
                slotFor == f -> SItemStacked("Définir ${f.name}…") {
                    SButton("Domicile", primary = true) { pl.setSlot("home", f); slotFor = null; engine.toast("${f.name} est désormais le domicile", 2500) }
                    SButton("Travail", primary = true) { pl.setSlot("work", f); slotFor = null; engine.toast("${f.name} est désormais le travail", 2500) }
                    SButton("Annuler") { slotFor = null }
                }
                else -> SItemStacked(f.name, f.sub) {
                    SButton("Renommer") { renamingFav = f }
                    SButton("Définir Domicile / Travail") { slotFor = f }
                    SButton("Déplacer vers Vos lieux") { pl.moveFavouriteToPlaces(f); engine.toast("${f.name} déplacé vers Vos lieux", 2500) }
                    SButton("Retirer", danger = true) { pl.toggleFavourite(f) }
                }
            }
        }
        // (favourites are best kept short, as they all show next to Home and Work: the rest can go to Your places in one go, GitHub #261)
        if (favs.size > 1) SItem("Déplacer tous les favoris vers Vos lieux", "Dans un groupe Favoris, pour qu'ils quittent la rangée à côté de Domicile et Travail") {
            SButton("Tout déplacer") { val n = pl.moveAllFavouritesToPlaces("Favoris"); engine.toast("$n déplacé vers Vos lieux", 2500) }
        }
        if (favs.size > 1) SItem("Supprimer tous les favoris") { SButton("Tout supprimer", danger = true) { pl.clearFavourites() } }
    }
    // (the Home and Work buttons under the search bar, in the app and on Android Auto: each can be left off)
    // everything about the area under the search bar in one place (it was spread over four groups)
    SGroup("Sous la barre de recherche", footer = "Sur cet écran et sur Android Auto. Vos lieux Domicile et Travail sont conservés dans tous les cas.") {
        SToggle(engine.prefs, "Recherche en bouton", PreferenceKeys.SEARCH_BUTTON, false,
            "Sans itinéraire, un bouton de recherche rond dans le coin au lieu de la barre de recherche et de ses boutons, pour que la flèche soit au centre. Touchez-le pour chercher")
        SToggle(engine.prefs, "Bouton Domicile", PreferenceKeys.HOME_BTN, true)
        SToggle(engine.prefs, "Bouton Travail", PreferenceKeys.WORK_BTN, true)
        SToggle(engine.prefs, "Vos lieux à côté de Domicile et Travail", PreferenceKeys.ROW_PLACES, true, "Seuls ceux avec une étoile s’y affichent")
        SToggle(engine.prefs, "Lieux récents sous la barre de recherche", "quicklist", true, "Vos lieux enregistrés sont dans la rangée de tuiles sous la barre de recherche, qui se termine par Ajouter un lieu")
    }
    ShortcutsGroup(engine)
    SGroup("Historique", footer = "Faites glisser un lieu sur le côté dans la liste Enregistrés & récents pour le retirer.") {
        SItem("Destinations récentes", "${pl.recents().size} récents, plus des lieux mémorisés pour chercher sans réseau") {
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
    SToggle(prefs, "Zoom automatique", PreferenceKeys.AUTO_ZOOM, true, "Dézoome à grande vitesse et zoome aux intersections. Désactivé garde un zoom fixe, que + et − modifient toujours")
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
    val auFuel = engine.detectedCountry.equals("AU", true) || prefs.s(PreferenceKeys.NSW_FUEL_KEY, "").isNotBlank()
    SGroup(if (ukFuel) "Prix des carburants" else "Carburant", footer = if (ukFuel) "Au Royaume-Uni, les prix en direct viennent du service gouvernemental Fuel Finder. C'est gratuit : sur gov.uk, cherchez « Fuel Finder », inscrivez-vous comme destinataire d'informations avec un GOV.UK One Login, puis collez ici l'identifiant et le secret client. Sans eux la liste des stations fonctionne, mais sans les prix. (En France, les prix sont automatiques : voir plus bas.)" else if (!deFuel) "Les prix en France et en Autriche s'affichent tout seuls. La clé Tankerkönig (prix en Allemagne) apparaît ici quand vous y êtes." else null) {
        SChoice("Votre carburant", listOf("E10" to "SP95-E10", "E5" to "SP98", "B7" to "Gazole", "E85" to "Superéthanol (E85)", "LPG" to "GPL", "EV" to "Électrique"), prefs.s(PreferenceKeys.FUEL_TYPE, "E10")) { prefs.put(PreferenceKeys.FUEL_TYPE, it) }
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
    if (deFuel || auFuel) SGroup(if (ukFuel) "Prix des carburants hors Royaume-Uni" else "Prix des carburants", footer = "La France et l'Autriche fonctionnent toutes seules, via les services de prix de leurs gouvernements. L'Allemagne demande une clé gratuite sur creativecommons.tankerkoenig.de (Tankerkönig). L'Australie (Nouvelle-Galles du Sud et Tasmanie) demande une clé et un secret API gratuits sur api.nsw.gov.au (Fuel API). Ailleurs la liste des stations fonctionne, sans les prix.") {
        if (deFuel) SFieldItem("Clé Tankerkönig (Allemagne)", prefs.s(PreferenceKeys.TANKERKOENIG_KEY, ""), placeholder = "Collez la clé", password = true) { prefs.put(PreferenceKeys.TANKERKOENIG_KEY, it.trim()) }
        if (auFuel) SFieldItem("Clé API FuelCheck (Australie)", prefs.s(PreferenceKeys.NSW_FUEL_KEY, ""), sub = "Nouvelle-Galles du Sud et Tasmanie", placeholder = "Collez la clé API", password = true) { prefs.put(PreferenceKeys.NSW_FUEL_KEY, it.trim()) }
        if (auFuel) SFieldItem("Secret API FuelCheck", prefs.s(PreferenceKeys.NSW_FUEL_SECRET, ""), placeholder = "Collez le secret API", password = true) { prefs.put(PreferenceKeys.NSW_FUEL_SECRET, it.trim()) }
    }
    if (prefs.s(PreferenceKeys.FUEL_TYPE, "E10") == "EV") SGroup("Votre voiture électrique", footer = "Avec la consommation et le prix renseignés, chaque itinéraire indique à peu près ce que coûtera la recharge. La consommation et la capacité de batterie servent aussi à l'estimation de batterie sur la carte de trajet et aux suggestions d'arrêt recharge (réglez votre niveau de batterie dans les options d'itinéraire).") {
        SToggle(prefs, "Estimation de batterie sur la carte de trajet", PreferenceKeys.EV_PCT_CARD, true, "Le pourcentage de batterie (maintenant, à la borne, à l'arrivée), sur le téléphone et Android Auto")
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
        SFieldItem("Puissance de charge maximale (kW)", prefs.s("ev_max_kw", ""), "Pour le temps de recharge aux arrêts proposés (135 si vide). Voir la fiche technique de la voiture", placeholder = "ex. 135", number = true) { s -> prefs.put("ev_max_kw", s.filter { it.isDigit() }.take(3)) }
        // (in currencies where a kWh or a litre costs more than a few of the main unit, rupees or kroner say, the price is asked for
        // as it's normally written, in that currency: bijindas)
        val cur = moneyCurrency(prefs)
        val small = smallUnitPrices(cur)
        val sym = try { cur.getSymbol(java.util.Locale.getDefault()) } catch (e: Exception) { cur.currencyCode }
        SFieldItem(if (!small) "Prix de l'électricité par kWh ($sym)" else if (mi) "Prix de l'électricité (pence par kWh)" else "Prix de l'électricité (centimes par kWh)", prefs.s(PreferenceKeys.EVPPK, ""), placeholder = if (small) "ex. 28" else "", number = true) { s -> prefs.put(PreferenceKeys.EVPPK, s.filter { it.isDigit() || it == '.' || it == ',' }.take(6)) }
    } else SGroup("Coût du carburant dans les options d'itinéraire", footer = "Avec les deux renseignés, chaque itinéraire indique environ le coût en carburant.") {
        if (mi) SFieldItem("Consommation (miles par gallon)", prefs.s(PreferenceKeys.FUELMPG, ""), placeholder = "ex. 45", number = true) { s -> prefs.put(PreferenceKeys.FUELMPG, num(s).take(5)) }
        // litres per 100 km: 282.48 / UK miles per gallon
        else SFieldItem("Consommation (litres aux 100 km)", prefs.s(PreferenceKeys.FUELMPG, "").toDoubleOrNull()?.takeIf { it > 0 }?.let { fmt1(282.481 / it) } ?: "", placeholder = "ex. 6.3", number = true) { s ->
            prefs.put(PreferenceKeys.FUELMPG, num(s).toDoubleOrNull()?.takeIf { it > 0 }?.let { String.format(java.util.Locale.US, "%.2f", 282.481 / it) } ?: "")
        }
        val cur = moneyCurrency(prefs)
        val small = smallUnitPrices(cur)
        val sym = try { cur.getSymbol(java.util.Locale.getDefault()) } catch (e: Exception) { cur.currencyCode }
        SFieldItem(if (!small) "Prix du carburant au litre ($sym)" else if (mi) "Prix du carburant (pence par litre)" else "Prix du carburant (centimes par litre)", prefs.s(PreferenceKeys.FUELPPL, ""), placeholder = if (small) "ex. 1,899" else "", number = true) { s -> prefs.put(PreferenceKeys.FUELPPL, s.filter { it.isDigit() || it == '.' || it == ',' }.take(6)) }
    }
    // (ZarulOfficial #316)
    SGroup("Coût réel du carburant") {
        SToggle(prefs, "Coût réel du carburant", PreferenceKeys.LIVE_FUEL, false, "Ce que le trajet a coûté jusqu'ici, d'après la distance vraiment parcourue, sous les chiffres de la carte de trajet, et le total à l'arrivée. Utilise la consommation et le prix ci-dessus")
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
        SToggle(prefs, "Plein écran", PreferenceKeys.FULLSCREEN, true, "Masque les barres d'état et de navigation. Désactivez si votre autoradio a sa propre barre (climatisation, bouton accueil) que la carte recouvre")
        SSlider("Luminosité", prefs.i(PreferenceKeys.BRIGHT, 100).toFloat(), 10f..100f, "%", sub = "100 % laisse le système décider", live = true) { prefs.put(PreferenceKeys.BRIGHT, it) }
        SToggle(prefs, "Assombrir l'écran la nuit", "dimnight", false)
        SToggle(prefs, "Garder l'écran allumé", "keepon", true)
        SToggle(prefs, "Panneaux en verre", "glass", true, "Cartes translucides teintées par la carte. Désactivé : cartes opaques")
        SToggle(prefs, "Nouvelle disposition en portrait", PreferenceKeys.PORTRAIT_SHEET, true, "Sur un écran vertical sans itinéraire : seulement la barre de recherche en haut, et vos lieux, la musique et les messages dans un volet en bas")
        SChoice("Position du logo Mapbox", listOf("tl" to "En haut à gauche", "tr" to "En haut à droite", "bl" to "En bas à gauche", "br" to "En bas à droite"), prefs.s(PreferenceKeys.MAPBOX_LOGO, "tr"),
            "Mapbox impose son logo sur la carte ; choisissez le coin qui vous convient") { prefs.put(PreferenceKeys.MAPBOX_LOGO, it) }
    }
    SGroup("Tailles") {
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
            SSlider("Taille de la flèche de direction", prefs.i(PreferenceKeys.TURN_ARROW, 100).toFloat(), 80f..200f, "%", sub = "La flèche de la carte de direction, seule", steps = 11, reset = 100f) { prefs.put(PreferenceKeys.TURN_ARROW, it) }
            SSlider("Largeur de la carte de direction", prefs.i(PreferenceKeys.TURN_WIDTH, 100).toFloat(), 60f..130f, "%", sub = "Jusqu'où la carte de direction s'étend sur la carte", steps = 13, reset = 100f) { prefs.put(PreferenceKeys.TURN_WIDTH, it) }
            SSlider("Hauteur de la carte de direction", prefs.i(PreferenceKeys.TURN_HEIGHT, 100).toFloat(), 70f..130f, "%", sub = "L'espace au-dessus et au-dessous du texte", steps = 11, reset = 100f) { prefs.put(PreferenceKeys.TURN_HEIGHT, it) }
            SSlider("Taille des boutons", prefs.i(PreferenceKeys.MAIN_BUTTON_SIZE, 100).toFloat(), 70f..150f, "%", sub = "Commandes à l'écran", steps = 15, reset = 100f) { prefs.put(PreferenceKeys.MAIN_BUTTON_SIZE, it) }
            SSlider("Taille du texte", prefs.i(PreferenceKeys.MAIN_TEXT_SIZE, 100).toFloat(), 70f..150f, "%", sub = "Mise à l'échelle indépendante du texte", steps = 15, reset = 100f) { prefs.put(PreferenceKeys.MAIN_TEXT_SIZE, it) }
            SSlider("Taille du nom de rue", prefs.i(PreferenceKeys.ROAD_NAME_SIZE, 100).toFloat(), 70f..200f, "%", sub = "Le nom de la route où vous êtes, sous votre flèche", steps = 12, reset = 100f) { prefs.put(PreferenceKeys.ROAD_NAME_SIZE, it) }
        }

    }
    SGroup("Affichage de la vitesse") {
        SToggle(prefs, "Votre vitesse à côté de la limitation", "mapspeed", true)
        SToggle(prefs, "Anneau du compteur", "speedring", true, "Se remplit vers la limite, passe à l'orange puis au rouge")
        SToggle(prefs, "Grand panneau de limitation", PreferenceKeys.SPEED_SWAP, false, "Inverse les deux : le panneau de limitation en grand, votre vitesse en petit à côté")
        SSlider("Taille de la limitation et du compteur", prefs.i(PreferenceKeys.SPEED_SIZE, 100).toFloat(), 50f..200f, "%", sub = "Sur cet écran (Android Auto a sa propre taille, dans Android Auto)", steps = 29, reset = 100f) { prefs.put(PreferenceKeys.SPEED_SIZE, it) }
        SPictureChoice("Position de la vitesse", "Du côté opposé aux cartes pour ne jamais les cacher. Montez-la si votre main la cache",
            listOf("bottom" to "En bas", "middle" to "Au milieu", "top" to "En haut"), prefs.s(PreferenceKeys.SPEED_POS, "bottom"), { id, c -> speedPicture(id, c) },
        ) { prefs.put(PreferenceKeys.SPEED_POS, it) }
    }
    SGroup("Position des cartes", "Où se placent la carte de direction, la carte du trajet et le compteur sur un écran large (en portrait, les cartes restent en haut et en bas)") {
        CardPositionsPreview(prefs, car = false)
        SChoice("Carte de direction", listOf("auto" to "Selon le volant", "left" to "En haut à gauche", "right" to "En haut à droite"), prefs.s(PreferenceKeys.CARDS_SIDE, "auto")) { prefs.put(PreferenceKeys.CARDS_SIDE, it) }
        SChoice("Carte de trajet", listOf("auto" to "Sous la carte de direction", "left" to "En bas à gauche", "centre" to "En bas au centre", "right" to "En bas à droite"), prefs.s(PreferenceKeys.TRIP_POS, "auto")) { prefs.put(PreferenceKeys.TRIP_POS, it) }
        SChoice("Compteur", listOf("auto" to "À l'opposé des cartes", "left" to "En bas à gauche", "right" to "En bas à droite"), prefs.s(PreferenceKeys.SPEED_SIDE, "auto")) { prefs.put(PreferenceKeys.SPEED_SIDE, it) }
    }
    SGroup("Unités") {
        SChoice("Distance et vitesse", listOf("mi" to "Miles", "km" to "Kilomètres"), if (engine.imperial) "mi" else "km") { prefs.put(PreferenceKeys.DISTANCE_UNIT, it) }
        SChoice("Horloge", listOf("auto" to "Automatique", "24" to "24-hour", "12" to "12-hour"), prefs.s(PreferenceKeys.CLOCK, "auto"),
            "Automatique suit le réglage 12 / 24 h du téléphone ou de l'autoradio") { prefs.put(PreferenceKeys.CLOCK, it) }
        SChoice("Température", listOf("C" to "°C", "F" to "°F"), if (isImperial(prefs)) "F" else "C") { prefs.put(PreferenceKeys.TEMPERATURE_UNIT, it) }
        SChoice("Côté de conduite", listOf("" to "Automatique", "rhd" to "Conduite à droite", "lhd" to "Conduite à gauche"), prefs.s(PreferenceKeys.DRIVE, ""),
            when (prefs.s(PreferenceKeys.DRIVE, "")) {
                "rhd" -> "Conducteur à droite (R.-U.) : cartes à gauche, boutons à droite"
                "lhd" -> "Conducteur à gauche (É.-U., Europe) : écran en miroir, cartes à droite"
                else -> "Suit le pays où vous êtes" + (if (engine.mirrored) ": conduite à gauche, écran en miroir" else ": conduite à droite")
            }) { prefs.put(PreferenceKeys.DRIVE, it) }
    }
}

// ------------------------------------------------------------------ While driving

/** What the screen does while you drive (it used to be a group at the end of Screen & sizes, which had grown very long). */
@Composable
private fun WhileDrivingSection(prefs: Prefs) {
    SGroup("En conduisant") {
        SToggle(prefs, "Nom de la route en conduisant sans itinéraire", "cruise", true)
        if (prefs.b("cruise", true)) SToggle(prefs, "Direction à côté du nom de la route", PreferenceKeys.CRUISE_HEADING, true, "« Direction E » après la route. Désactivé n'affiche que la route")
        SToggle(prefs, "Assombrir la carte dans les tunnels", "tunneldim", true)
        SToggle(prefs, "Moins de boutons en conduisant", "drivesimple", true)
        if (android.os.Build.VERSION.SDK_INT >= 26) SToggle(prefs, "Image dans l'image", PreferenceKeys.PIP, true, "Sur un téléphone, en guidage : en allant sur l'écran d'accueil ou dans une autre appli, la carte, la prochaine direction et le temps restant restent dans une petite fenêtre")
        SToggle(prefs, "Horloge sur la carte", PreferenceKeys.CLOCK_MAP, false, "L’heure, avec le compteur de vitesse. Pratique quand la barre d’état de l’autoradio est masquée")
        SToggle(prefs, "Bloquer la saisie en conduisant", "drivelock", true, "La recherche vocale fonctionne toujours")
        SToggle(prefs, "Commandes multimédia", PreferenceKeys.MEDIA_BAR, false, "Précédent, lecture / pause et suivant en bas de l'écran pendant que de la musique ou la radio joue sur cet appareil, depuis n'importe quelle appli")
        if (prefs.b(PreferenceKeys.MEDIA_BAR, false) && com.headunit.launcher.MediaNow.songInfoPossible) {
            val ctx = androidx.compose.ui.platform.LocalContext.current
            // (checked again every couple of seconds while this shows, so it changes when you come back from Android's page)
            var granted by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(com.headunit.launcher.MediaNow.accessGranted(ctx)) }
            androidx.compose.runtime.LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(2000); granted = com.headunit.launcher.MediaNow.accessGranted(ctx) } }
            SItem("Titre et pochette", "Affiche ce qui est en cours de lecture. Nécessite l'accès aux notifications, comme les applis d'écran d'accueil pour leurs commandes multimédia",
                onClick = { com.headunit.launcher.MediaNow.openAccessSettings(ctx) }) {
                if (granted) Text("Autorisée", color = LocalVw.current.sub, fontSize = 14.sp) else SButton("Autoriser") { com.headunit.launcher.MediaNow.openAccessSettings(ctx) }
            }
        }
    }
}

// ------------------------------------------------------------------ Android Auto

@Composable
private fun AndroidAutoSection(prefs: Prefs) {
    val ctx = LocalContext.current
    // Android Auto only lists apps that weren't installed from Play when its own "Sources inconnues" is on, and some phones hide them
    // anyway: the steps that get it showing, with shortcuts
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
    CarTripButtonsGroup(prefs)
    SGroup {
        SChoice("Fonctionnement au frais", listOf("off" to "Désactivé", "auto" to "Automatique", "always" to "Toujours"), prefs.s(PreferenceKeys.CAR_COOL, "off"),
            "Pour les téléphones qui chauffent : 30 images par seconde, carte à plat, sans pluie ni lumières. Automatique l'active quand le téléphone signale qu'il chauffe") { prefs.put(PreferenceKeys.CAR_COOL, it) }
        SToggle(prefs, "Barre de recherche et lieux sur la carte", PreferenceKeys.CAR_MAPUI, true, "Désactivé pour une voiture à molette : toute la carte, avec Rechercher, Domicile et Travail dans Lieux (★)")
        if (prefs.b(PreferenceKeys.CAR_MAPUI, true)) SChoice("Les masquer après", listOf("30" to "30 s", "60" to "1 min", "120" to "2 min", "300" to "5 min", "0" to "Jamais"),
            prefs.s(PreferenceKeys.CAR_MAPUI_HIDE, "0"), "Reviennent quand vous touchez l'écran") { prefs.put(PreferenceKeys.CAR_MAPUI_HIDE, it) }
        SToggle(prefs, "Bouton du son sur la carte", PreferenceKeys.CAR_SOUND_BTN, true, "Son activé / Alertes seulement / Muet, hors navigation. Désactivé laisse plus de place aux Lieux récents ; le même choix est dans les Réglages de la voiture")
    }
    // Voice search from the car screen's microphone button listens through the app, which needs the microphone allowed. Android
    // only lets an app ask from its own screen (asking from the car screen is refused on newer Android), so it's asked here
    run {
        var micOk by remember { mutableStateOf(ctx.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) }
        val askMic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> micOk = granted }
        SGroup("Recherche vocale sur l'écran de la voiture", footer = "Avec le micro autorisé, le bouton micro de l'écran de la voiture écoute tout de suite. Sinon, il ouvre l'écran de recherche d'Android Auto, qui a son propre micro.") {
            SItem("Microphone", if (micOk) "Autorisée" else "Non autorisé") {
                if (!micOk) SButton("Autoriser", primary = true) { askMic.launch(android.Manifest.permission.RECORD_AUDIO) }
            }
        }
    }
    SGroup("Position des cartes sur l'écran de la voiture", "La carte de direction reste en haut à gauche dans Android Auto, là où ses propres boutons laissent de la place") {
        CardPositionsPreview(prefs, car = true)
        SChoice("Carte de trajet", listOf("auto" to "Sous la carte de direction", "centre" to "En bas au centre", "right" to "En bas à droite"), prefs.s(PreferenceKeys.CAR_TRIP_POS, "auto")) { prefs.put(PreferenceKeys.CAR_TRIP_POS, it) }
        // (Android Auto's split screen, the map in a tile beside another app: a bar along the bottom, or a narrow column, GitHub)
        SChoice("Carte du trajet en écran partagé", listOf("bar" to "Barre en bas", "column" to "Colonne à gauche"), prefs.s(PreferenceKeys.CAR_SPLIT_TRIP, "bar")) { prefs.put(PreferenceKeys.CAR_SPLIT_TRIP, it) }
        // (one choice for where the speedometer goes: it used to be two, "Speedometer" here for the bottom corner and "Speed
        // position on the car screen" further down, and the first did nothing unless the second was on Bottom)
        SChoice("Compteur", listOf("bl" to "En bas à gauche", "br" to "En bas à droite", "middle" to "Sous la carte de direction", "top" to "En haut à droite"), prefs.carSpeedSpot(),
            "En haut à droite se place dans le coin même, et s'efface tant que les boutons d'Android Auto y sont affichés", wrap = true) { prefs.setCarSpeedSpot(it) }
        SChoice("Style du compteur sur l'écran de la voiture", listOf("app" to "Comme dans l'appli", "compact" to "Compact"), prefs.s(PreferenceKeys.CAR_SPEED_STYLE, "app"),
            "Compact est une petite pastille sombre avec la limite et votre vitesse côte à côte") { prefs.put(PreferenceKeys.CAR_SPEED_STYLE, it) }
    }
    SGroup("Jour et nuit sur l'écran de la voiture") {
        SChoice("Jour et nuit", listOf("sun" to "Soleil", "car" to "Voiture", "phone" to "Téléphone", "day" to "Jour", "night" to "Nuit"), prefs.s(PreferenceKeys.CARTHEME, "sun"),
            when (prefs.s(PreferenceKeys.CARTHEME, "sun")) { "sun" -> "Passe en mode nuit au coucher du soleil et en mode jour à son lever, là où vous êtes"; "car" -> "Suit le réglage de la voiture (ses phares ou son propre mode)"; "phone" -> "Clair ou sombre comme le téléphone"; else -> null }) { prefs.put(PreferenceKeys.CARTHEME, it) }
    }
    SGroup("Tailles sur l'écran de la voiture") {
        SSlider("Taille générale sur l'écran de la voiture", prefs.i(PreferenceKeys.CARSCALE, 100).toFloat(), 40f..300f, "%", sub = "Échelle de base pour tout l'affichage" + (prefs.i(PreferenceKeys.CARSCALE_MAX, 0).takeIf { it in 100..299 }?.let { " · " + "Jusqu'à $it% tient sur l'écran de cette voiture" } ?: ""), steps = 25, reset = 100f) { prefs.put(PreferenceKeys.CARSCALE, it) }
        SSlider("Taille du compteur et du panneau de limitation", prefs.i(PreferenceKeys.CAR_SPEED_SIZE, 100).toFloat(), 40f..180f, "%", sub = "Rétrécit automatiquement si la zone d'écran disponible est trop petite", reset = 100f) { prefs.put(PreferenceKeys.CAR_SPEED_SIZE, it) }
        SSlider("Taille du panneau de limitation", prefs.i(PreferenceKeys.CAR_LIMIT_SIZE, 100).toFloat(), 80f..170f, "%", sub = "Agrandit ou réduit le panneau de limitation à côté du compteur", reset = 100f) { prefs.put(PreferenceKeys.CAR_LIMIT_SIZE, it) }
        var carAdvanced by rememberSaveable { mutableStateOf(false) }
        SItem("Tailles avancées pour l'écran de la voiture", "Réglez finement chaque élément") {
            SButton(if (carAdvanced) "Masquer" else "Afficher") { carAdvanced = !carAdvanced }
        }
        if (carAdvanced) {
            SSlider("Taille des cartes", prefs.i(PreferenceKeys.CAR_CARD_SIZE, 100).toFloat(), 70f..150f, "%", sub = "Cartes de direction, de trajet et d'itinéraire", steps = 16, reset = 100f) { prefs.put(PreferenceKeys.CAR_CARD_SIZE, it) }
            SSlider("Taille de la flèche de direction", prefs.i(PreferenceKeys.CAR_TURN_ARROW, 100).toFloat(), 80f..200f, "%", sub = "La flèche de la carte de direction, seule", steps = 11, reset = 100f) { prefs.put(PreferenceKeys.CAR_TURN_ARROW, it) }
            SSlider("Largeur de la carte de direction", prefs.i(PreferenceKeys.CAR_TURN_WIDTH, 100).toFloat(), 60f..130f, "%", sub = "Jusqu'où la carte de direction s'étend sur la carte", steps = 13, reset = 100f) { prefs.put(PreferenceKeys.CAR_TURN_WIDTH, it) }
            SSlider("Hauteur de la carte de direction", prefs.i(PreferenceKeys.CAR_TURN_HEIGHT, 100).toFloat(), 70f..130f, "%", sub = "L'espace au-dessus et au-dessous du texte", steps = 11, reset = 100f) { prefs.put(PreferenceKeys.CAR_TURN_HEIGHT, it) }
            SSlider("Taille des boutons", prefs.i(PreferenceKeys.CAR_BUTTON_SIZE, 100).toFloat(), 70f..150f, "%", sub = "Commandes rondes et boutons tactiles", steps = 16, reset = 100f) { prefs.put(PreferenceKeys.CAR_BUTTON_SIZE, it) }
            SSlider("Taille du texte", prefs.i(PreferenceKeys.CAR_TEXT_SIZE, 100).toFloat(), 70f..150f, "%", sub = "Indépendant de la taille des cartes et des boutons", steps = 16, reset = 100f) { prefs.put(PreferenceKeys.CAR_TEXT_SIZE, it) }
            SSlider("Taille de la flèche de navigation", prefs.i(PreferenceKeys.CAR_ARROW_SIZE, 100).toFloat(), 30f..180f, "%", reset = 100f) { prefs.put(PreferenceKeys.CAR_ARROW_SIZE, it) }
            SSlider("Taille du nom de rue", prefs.i(PreferenceKeys.CAR_ROAD_NAME_SIZE, 100).toFloat(), 70f..200f, "%", sub = "Le nom de la route où vous êtes, sous votre flèche", steps = 12, reset = 100f) { prefs.put(PreferenceKeys.CAR_ROAD_NAME_SIZE, it) }
        }
    }
    SGroup("Installation", footer = "Si Horizon Nav n'apparaît pas dans Android Auto : 1. Ouvrez les paramètres d'Android Auto, touchez une dizaine de fois le numéro de version en bas, puis dans le menu en haut à droite ouvrez les Paramètres pour les développeurs et activez Sources inconnues. 2. De retour dans les paramètres d'Android Auto, ouvrez Personnaliser le lanceur et vérifiez que Horizon Nav est coché. 3. Forcez l'arrêt d'Android Auto (Infos sur l'appli ci-dessous), débranchez et rebranchez. Si elle n'apparaît toujours pas, l'Android Auto de votre téléphone n'affiche que les applis installées par le Play Store : installez l'APK avec un installateur qui déclare le Play Store comme source (par exemple « adb install -i com.android.vending », ou une appli comme AAAD ou Install with Options).") {
        SItem("Installé par", source + if (source.startsWith("Google Play")) (if (prefs.b("aadirect", true)) " · mises à jour : directes, comme AAEnabler" else " · mises à jour : à installer avec AAEnabler") else "")
        SItem("Paramètres d'Android Auto", if (hasAa) null else "Android Auto n'est pas installé sur ce téléphone") {
            if (hasAa) SButton("Ouvrir") {
                val i = ctx.packageManager.getLaunchIntentForPackage(gh) ?: aaSettingsIntent(ctx, gh)
                try {
                    if (i != null) ctx.startActivity(i.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) else openSystemSettings(ctx, Settings.ACTION_APPLICATION_DETAILS_SETTINGS, ghUri)
                } catch (e: Exception) { openSystemSettings(ctx, Settings.ACTION_APPLICATION_DETAILS_SETTINGS, ghUri) }
            }
        }
        if (hasAa) SItem("Infos sur l'appli Android Auto", "Pour forcer son arrêt après avoir modifié ses paramètres") {
            SButton("Infos sur l'appli") { openSystemSettings(ctx, Settings.ACTION_APPLICATION_DETAILS_SETTINGS, ghUri) }
        }
        // the most asked question: Android Auto only lists apps from Google Play unless told otherwise
        if (hasAa && BuildConfig.STORE != "play") SItem("Absent d'Android Auto ?", "1. Dans les réglages d'Android Auto, touchez plusieurs fois Version pour débloquer les paramètres pour les développeurs, puis activez Sources inconnues. " +
            "2. Forcez l'arrêt d'Android Auto (Infos sur l'appli ci-dessus) et reconnectez. 3. S'il n'apparaît toujours pas, installez Horizon Nav avec l'appli gratuite AAEnabler, nécessaire pour la plupart. " +
            "Les demandes vocales via le micro d'Android Auto peuvent ne pas atteindre les applis hors Google Play : utilisez plutôt le bouton de recherche de l'appli.") {
            SButton("AAEnabler") {
                try { ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/malebuffy/AAEnabler")).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
                catch (e: Exception) { android.widget.Toast.makeText(ctx, "Pas de navigateur sur cet appareil : cherchez AAEnabler sur GitHub", android.widget.Toast.LENGTH_LONG).show() }
            }
        }
    }
}

/**
 * Fork FR : Android Auto n'a souvent pas d'icône (Samsung, Android récents) : on cherche son écran de paramètres parmi ses
 * activités ouvrables, plutôt que d'ouvrir la fiche de l'appli comme le bouton « Infos sur l'appli ».
 */
private fun aaSettingsIntent(ctx: android.content.Context, pkg: String): android.content.Intent? = try {
    val pm = ctx.packageManager
    val info = if (android.os.Build.VERSION.SDK_INT >= 33) pm.getPackageInfo(pkg, android.content.pm.PackageManager.PackageInfoFlags.of(android.content.pm.PackageManager.GET_ACTIVITIES.toLong()))
               else @Suppress("DEPRECATION") pm.getPackageInfo(pkg, android.content.pm.PackageManager.GET_ACTIVITIES)
    val acts = info.activities.orEmpty().filter { it.exported && it.enabled && it.permission == null }
    fun score(n: String): Int {
        val l = n.lowercase()
        return when {
            l.endsWith("companion.settings.settingsactivity") -> 0
            l.contains("settings") && l.contains("companion") -> 1
            l.endsWith(".settingsactivity") -> 2
            l.contains("settings") && !l.contains("developer") && !l.contains("notification") -> 3
            else -> 99
        }
    }
    acts.map { it.name to score(it.name) }.filter { it.second < 99 }.minByOrNull { it.second }?.let { (n, _) ->
        android.content.Intent().setClassName(pkg, n)
    }
} catch (e: Throwable) { null }

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
    SGroup("Diagnostic") {
        SToggle(prefs, "Afficher le diagnostic des voies", PreferenceKeys.HD_LANE_DIAGNOSTICS, false,
            "Visible seulement en mode démo ; ne change pas le guidage par voie.")
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
    // Weather (shown in every edition, Plus included: the Met Office key is always your own)
    SGroup("Météo", footer = "La météo en direct (Réglages > Météo) vient d'Open-Meteo. Au Royaume-Uni, une clé Met Office gratuite est souvent plus précise : datahub.metoffice.gov.uk, abonnez-vous à Site-Specific Forecast (Global spot), puis collez la clé API. Si le Met Office échoue, Open-Meteo est utilisé.") {
        SFieldItem("Clé API Met Office", prefs.s(PreferenceKeys.METOFFICE_KEY, ""), "Facultatif, Royaume-Uni. Offre gratuite : 360 requêtes par jour (l'app demande au plus toutes les 10 minutes)",
            placeholder = "Collez une clé") { prefs.put(PreferenceKeys.METOFFICE_KEY, it.trim()) }
        if (prefs.s(PreferenceKeys.METOFFICE_KEY, "").isNotBlank()) {
            SItem("Tester le Met Office", if (engine.fix == null) "En attente de votre position" else "Récupère la météo là où vous êtes maintenant") {
                SButton("Tester", primary = true) { engine.refreshWeather() }
            }
            val st = com.headunit.launcher.MetOffice.status
            if (st.isNotBlank()) SNote(st)
        }
    }
    // Horizon Maps on Play: the keys are the app's own (Plus) or not needed (the free OpenStreetMap version), so there's nothing to paste
    if (com.headunit.launcher.Plus.offered) {
        SGroup("Comptes de carte") {
            SNote(if (com.headunit.launcher.Plus.active) "Plus est activé : la carte, la recherche, les itinéraires et le trafic utilisent les comptes de Horizon Maps, aucune clé à ajouter."
                else "La version gratuite utilise OpenStreetMap, avec des itinéraires calculés depuis votre pack pays. Horizon Maps Plus ajoute des bâtiments et monuments 3D détaillés, le satellite et le trafic en direct, sans clé à configurer.")
        }
        return
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
    // fork FR : plus de don, et le rapport part directement (sans texte à écrire)
    val ctxR = LocalContext.current
    SGroup("Horizon Nav") {
        SItem("Version", BuildConfig.VERSION_NAME) { SButton("Nouveautés") { WhatsNewCard.reopen = true } }
        if (com.headunit.launcher.Updater.enabled) {
            SItem("Rechercher des mises à jour", engine.updateStatus.ifBlank { null }) {
                val u = engine.appUpdate
                if (u != null && !engine.updateBusy) SButton("Mettre à jour vers ${u.version}", primary = true) { engine.installAppUpdate() }
                else SButton(if (engine.updateBusy) "Mise à jour…" else "Vérifier maintenant") { engine.checkForUpdate(manual = true) }
            }
            SToggle(prefs, "Chercher les mises à jour automatiquement", "updcheck", true, "À l'ouverture de l'appli avec une connexion (au plus toutes les demi-heures)")
        }
        SItem("Configuration", "Refaire les étapes du premier lancement : clé de carte, domicile et travail, cartes hors ligne, autorisations") { SButton("Lancer la configuration") { prefs.put(PreferenceKeys.SETUP_DONE, false); prefs.put(PreferenceKeys.FORCESETUP, true) } }
        SItem("Rapport", if (com.headunit.launcher.DiagUpload.hasToken(ctxR)) "Envoie un rapport dans le dépôt GitHub : version, appareil, réglages, dernier plantage, erreurs de carte et derniers changements de limitation avec leur position (aucune clé API)"
            else "Copie le rapport (version, appareil, réglages, dernier plantage) pour le coller où vous voulez. Avec un jeton GitHub ci-dessous, il est envoyé directement") {
            SButton(if (com.headunit.launcher.DiagUpload.hasToken(ctxR)) "Envoyer" else "Copier", primary = true) { sendReport(ctxR, engine) }
        }
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
    // (with the GPX tracks in it a backup can be a couple of megabytes: written and read off the main thread, so a slow unit
    // doesn't stall; bijindas)
    val mainH = remember { android.os.Handler(android.os.Looper.getMainLooper()) }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) Thread {
            val ok = try {
                val text = com.headunit.launcher.Backup.export(ctx, prefs)
                ctx.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) } != null
            } catch (e: Throwable) { false }
            mainH.post { android.widget.Toast.makeText(ctx, if (ok) "Sauvegarde enregistrée" else "Impossible d'enregistrer la sauvegarde", android.widget.Toast.LENGTH_SHORT).show() }
        }.start()
    }
    val restoreFrom: (android.net.Uri?) -> Unit = { uri ->
        if (uri != null) Thread {
            val msg = try {
                val text = ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
                val (n, t) = com.headunit.launcher.Backup.restore(ctx, prefs, text)
                "$n réglages restaurés" + (if (t > 0) " · " + "$t traces GPX" else "")
            } catch (e: Throwable) { "Ce fichier n'est pas une sauvegarde valide" }
            mainH.post { android.widget.Toast.makeText(ctx, msg, android.widget.Toast.LENGTH_LONG).show() }
        }.start()
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> restoreFrom(uri) }
    // (the older kind of file picker, for a phone or unit where nothing answers the newer one: franciscoserranoadt's Xiaomi
    // crashed on Restore)
    val importer2 = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res -> restoreFrom(res.data?.data) }
    // automatic backup: a folder picked once (its permission kept), then a backup there once a day when the app opens
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            try { ctx.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION) } catch (e: Exception) { }
            prefs.put(PreferenceKeys.AUTO_BACKUP_DIR, uri.toString())
            com.headunit.launcher.Backup.maybeRun(ctx, prefs, force = true)
        }
    }
    SGroup("Sauvegarde", footer = "Comprend les réglages, les lieux enregistrés et les traces GPX, pas le pack hors ligne. Vos clés API n’y sont que si l’interrupteur est activé. Une sauvegarde de Horizon Launcher peut aussi être restaurée ici.") {
        SToggle(prefs, "Inclure les clés API", PreferenceKeys.BACKUP_KEYS, false, "Met vos clés TomTom, Mapbox et Met Office dans le fichier de sauvegarde, pour que Restaurer les remette. Le fichier est en texte clair : gardez-le en lieu sûr")
        SItem("Réglages, lieux et traces") {
            SButton("Sauvegarder") {
                try { exporter.launch("horizon-nav-backup.json") }
                catch (e: Exception) { android.widget.Toast.makeText(ctx, "Aucune application de cet appareil ne peut choisir un fichier (installez un gestionnaire de fichiers)", android.widget.Toast.LENGTH_LONG).show() }
            }
            SButton("Restaurer") {
                try { importer.launch(arrayOf("application/json", "text/plain", "*/*")) }
                catch (e: Exception) {
                    try { importer2.launch(android.content.Intent(android.content.Intent.ACTION_GET_CONTENT).setType("*/*").addCategory(android.content.Intent.CATEGORY_OPENABLE)) }
                    catch (e2: Exception) { android.widget.Toast.makeText(ctx, "Aucune application de cet appareil ne peut choisir un fichier (installez un gestionnaire de fichiers)", android.widget.Toast.LENGTH_LONG).show() }
                }
            }
        }
        val autoDir = prefs.s(PreferenceKeys.AUTO_BACKUP_DIR, "")
        val lastAt = prefs.s(PreferenceKeys.AUTO_BACKUP_AT, "0").toLongOrNull() ?: 0L
        val failAt = prefs.s(PreferenceKeys.AUTO_BACKUP_AT + "fail", "0").toLongOrNull() ?: 0L
        SItem("Sauvegarde automatique",
            if (autoDir.isBlank()) "Une fois par jour, dans un dossier de votre choix"
            else "Une fois par jour, dans ${com.headunit.launcher.Backup.folderName(autoDir)}" +
                (if (failAt > lastAt) " · " + "la dernière n’a pas pu être enregistrée : choisissez à nouveau le dossier" else if (lastAt > 0) " · " + "dernière ${com.headunit.launcher.Clock.format("EEE d MMM, ").format(java.util.Date(lastAt))}" else "")) {
            if (autoDir.isBlank()) SButton("Choisir un dossier") {
                try { folderPicker.launch(null) } catch (e: Exception) { android.widget.Toast.makeText(ctx, "Cet appareil ne peut pas choisir de dossier", android.widget.Toast.LENGTH_LONG).show() }
            } else {
                SButton("Modifier") { try { folderPicker.launch(null) } catch (e: Exception) { } }
                SButton("Désactiver", danger = true) {
                    try { ctx.contentResolver.releasePersistableUriPermission(android.net.Uri.parse(autoDir), android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION) } catch (e: Exception) { }
                    prefs.put(PreferenceKeys.AUTO_BACKUP_DIR, "")
                }
            }
        }
        if (autoDir.isNotBlank()) SChoice("Fichiers de sauvegarde", listOf("one" to "Remplacer la dernière", "week" to "Garder les 7 dernières"), prefs.s(PreferenceKeys.AUTO_BACKUP_KEEP, "one"),
            "Remplacer la dernière garde un seul fichier, écrasé chaque jour. Garder les 7 dernières en ajoute une par jour et supprime la plus ancienne") { prefs.put(PreferenceKeys.AUTO_BACKUP_KEEP, it) }
    }
    val cf = java.io.File(ctx.filesDir, "lastcrash.txt")
    var rep by remember { mutableStateOf(if (cf.exists()) cf.readText() else "") }
    // a report from before the last update (older reports have no version line, or another version's): it may well be fixed already
    val oldRep = rep.isNotBlank() && !rep.startsWith("Horizon Nav ${BuildConfig.VERSION_NAME} ")
    if (rep.isNotBlank()) SGroup("Rapport de problème", footer = if (oldRep) "Il date d'une version plus ancienne, avant votre dernière mise à jour : c'est peut-être déjà corrigé. Effacez-le, et envoyez-le s'il revient." else "L'appli s'est fermée de façon inattendue la dernière fois. Envoyez ce rapport pour que ce soit corrigé.") {
        Text(rep.take(900), color = LocalVw.current.text, fontSize = 10.sp, modifier = Modifier.padding(vertical = 6.dp))
        // sent the same way as Feedback (the report includes this crash and the unit's details)
        SItem("Ce plantage", "L'envoie avec les infos de votre appareil, comme Rapport") {
            SButton("Envoyer le rapport", primary = true) { sendReport(ctxR, engine) }
            SButton("Effacer") { cf.delete(); rep = "" }
        }
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
/** Fork FR : envoie le rapport dans le dépôt (avec un jeton GitHub), sinon le copie. */
internal fun sendReport(ctx: android.content.Context, engine: NavEngine) {
    val report = engine.feedbackReport("")
    if (com.headunit.launcher.DiagUpload.hasToken(ctx)) {
        engine.toast("Envoi du rapport…", 2000)
        Thread {
            val msg = try { "Rapport envoyé : " + com.headunit.launcher.DiagUpload.send(ctx, "avis-${com.headunit.launcher.DiagUpload.stamp()}.txt", report) }
                      catch (e: Exception) { "Envoi impossible : ${e.message}" }
            android.os.Handler(android.os.Looper.getMainLooper()).post { engine.toast(msg, 5000) }
        }.start()
    } else {
        val cm = ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
        cm?.setPrimaryClip(android.content.ClipData.newPlainText("Horizon Nav report", report))
        engine.toast("Rapport copié", 2500)
    }
}

@Suppress("unused")
@Composable
private fun FeedbackDialog(engine: NavEngine, onClose: () -> Unit) {
    val c = LocalVw.current
    val ctx = LocalContext.current
    var note by remember { mutableStateOf("") }
    androidx.compose.ui.window.Dialog(onDismissRequest = onClose) {
        // (scrolls: on a short, wide head unit screen the buttons were below the bottom of the screen and couldn't be reached, bluelions25 #329)
        Column(Modifier.widthIn(max = 560.dp).fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(c.panel).verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Un retour ou un problème", color = c.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Que s'est-il passé, ou que souhaitez-vous ? L'endroit et ce que vous avez touché aident.", color = c.sub, fontSize = 14.sp)
            SField(note, { note = it.take(600) }, placeholder = "ex. Mauvais recalcul près de Lyon", width = 516.dp, fill = true)
            Text("Envoyés avec : la version de l'appli, votre autoradio, les principaux réglages, vos zones téléchargées le dernier plantage et les derniers changements de limitation avec leur position (aucune clé API).", color = c.sub, fontSize = 12.sp)
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
 * "Request a language": the language wanted (and anything else) goes to GitHub as an issue, filled in, from the browser. The
 * title and the details stay in English so they can be read there; only what the person typed is theirs.
 */
@Composable
private fun LanguageRequestDialog(engine: NavEngine, onClose: () -> Unit) {
    val c = LocalVw.current
    val ctx = LocalContext.current
    var lang by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var helper by remember { mutableStateOf(false) }
    androidx.compose.ui.window.Dialog(onDismissRequest = onClose) {
        // (scrolls: on a short, wide head unit screen the buttons were below the bottom of the screen and couldn't be reached, bluelions25 #329)
        Column(Modifier.widthIn(max = 560.dp).fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(c.panel).verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Demander une langue", color = c.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Dans quelle langue voulez-vous Horizon Nav ? Si vous la parlez, dites-le : vous pourriez aider à vérifier les textes.", color = c.sub, fontSize = 14.sp)
            SField(lang, { lang = it.take(40) }, placeholder = "ex. suédois", width = 516.dp, fill = true)
            SField(note, { note = it.take(400) }, placeholder = "Autre chose (facultatif)", width = 516.dp, fill = true)
            Row(Modifier.fillMaxWidth().pressClick { helper = !helper }, verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Checkbox(helper, { helper = it })
                Text("Je peux aider à vérifier les textes", color = c.text, fontSize = 15.sp)
            }
            Text("Ouvre GitHub avec la demande remplie : touchez Submit pour l'envoyer.", color = c.sub, fontSize = 12.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                SButton("Fermer") { onClose() }
                SButton("Envoyer sur GitHub", primary = true) {
                    val want = lang.trim()
                    if (want.isEmpty()) { engine.toast("Saisissez d'abord la langue", 2500); return@SButton }
                    val phone = java.util.Locale.getDefault()
                    val body = buildString {
                        append("**Language requested:** ").append(want).append("\n\n")
                        append("**Can help check the wording:** ").append(if (helper) "yes" else "no").append("\n\n")
                        if (note.isNotBlank()) append("**Notes:** ").append(note.trim()).append("\n\n")
                        append("---\n")
                        append("Phone language: ").append(phone.getDisplayName(java.util.Locale.ENGLISH)).append(" (").append(phone.toLanguageTag()).append(")\n")
                        append("App language: ").append(L.lang).append("\n")
                        append("Horizon Nav ").append(BuildConfig.VERSION_NAME).append("\n")
                        append("_Sent from Settings › Sound & voice › Request a language_")
                    }
                    val title = "Language request: " + want
                    val url = "https://github.com/${BuildConfig.UPDATE_REPO}/issues/new?title=" + android.net.Uri.encode(title) + "&body=" + android.net.Uri.encode(body)
                    try { ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    catch (e: Exception) { engine.toast("Aucun navigateur sur cet appareil : utilisez Copier", 3500) }
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
        Triple("twilight", "Crépuscule", listOf(0xFFF1F0F5, 0xFFAAD4F5, 0xFFCDEBC4, 0xFFFFFFFF, 0xFFE5E3EA)),
        Triple("faded", "Pastel", listOf(0xFFF1F1F1, 0xFFD9E2EA, 0xFFE2E8DE, 0xFFFFFFFF, 0xFFE6E6E6)),
        Triple("grey", "Gris", listOf(0xFFE8E8E8, 0xFFCFCFCF, 0xFFDADADA, 0xFFFFFFFF, 0xFFD4D4D4)),
        Triple("ember", "Braise", listOf(0xFFF3EEE6, 0xFFB8D0DC, 0xFFD6E4C4, 0xFFFFFFFF, 0xFFE6DDD0)),
        Triple("mono", "Nuit monochrome", listOf(0xFFEFEFEF, 0xFFD4D4D4, 0xFFDEDEDE, 0xFFFFFFFF, 0xFFE2E2E2)),
        Triple("forest", "Forêt", listOf(0xFFEEF0E6, 0xFFA8CFC8, 0xFFBCDCAE, 0xFFFFFFFF, 0xFFE0E2D4)),
        Triple("arctic", "Arctique", listOf(0xFFF2F6FA, 0xFFBFE0F5, 0xFFD8ECEA, 0xFFFFFFFF, 0xFFE4ECF4)),
        Triple("contrast", "Contraste élevé", listOf(0xFFFFFFFF, 0xFF7CC4F0, 0xFFB4E0A4, 0xFF222222, 0xFFD8D8D8)),
        Triple("sepia", "Sépia", listOf(0xFFEFE4CF, 0xFFC8D2C0, 0xFFD8DCB4, 0xFFFAF4E6, 0xFFE2D4B8)),
        Triple("neon", "Néon", listOf(0xFFECEBF4, 0xFFA8E4F4, 0xFFC8ECD4, 0xFFFFFFFF, 0xFFE2E0EE)),
        Triple("pastel", "Pastel doux", listOf(0xFFF6F3F0, 0xFFCFE4F2, 0xFFDCECD6, 0xFFFFFFFF, 0xFFECE8E6)),
        Triple("car", "Assorties à ma voiture", listOf(0xFF1A1424, 0xFF14182C, 0xFF232C30, 0xFF5A4A6E, 0xFF2A2240)),
        Triple("custom", "Personnalisé", listOf(0xFF26304C, 0xFF2F6FB0, 0xFF3F7A5A, 0xFF8A93AE, 0xFFE0A84A)),
    )
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        SItem("Couleurs", "Horizon : la carte Mapbox aux couleurs douces de l'appli, bleu ardoise la nuit. Crépuscule : la même avec une nuit plus violette. Nuit monochrome : des gris, seuls l'itinéraire et les alertes en couleur. Assorties à ma voiture : une touche de la couleur de l'éclairage intérieur de la voiture. Personnalisé : vos propres couleurs. Couleurs de jour affichées (la nuit pour Assorties à ma voiture) ; chacune a une version de nuit")
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


// ------------------------------------------------------------------ pictures for the choices, and the map preview

private fun androidx.compose.ui.graphics.drawscope.DrawScope.lanePicture(id: String, c: Vw) {
    val w = size.width; val h = size.height
    drawRect(Color(0xFF2A3042))
    when (id) {
        "lit" -> {
            // three lanes seen ahead, the middle one lit green
            val road = androidx.compose.ui.graphics.Path().apply { moveTo(w * 0.2f, h); lineTo(w * 0.8f, h); lineTo(w * 0.62f, 0f); lineTo(w * 0.38f, 0f); close() }
            drawPath(road, Color(0xFF596274))
            val lit = androidx.compose.ui.graphics.Path().apply { moveTo(w * 0.4f, h); lineTo(w * 0.6f, h); lineTo(w * 0.54f, 0f); lineTo(w * 0.46f, 0f); close() }
            drawPath(lit, Color(0xFF34C759).copy(alpha = 0.75f))
            for (f in listOf(0.4f, 0.6f)) drawLine(Color.White, androidx.compose.ui.geometry.Offset(w * f, h), androidx.compose.ui.geometry.Offset(w * (0.5f + (f - 0.5f) * 0.4f), 0f), 2f)
        }
        "closeup" -> {
            drawRoundRect(Color(0xFF1C2130), androidx.compose.ui.geometry.Offset(w * 0.08f, h * 0.12f), androidx.compose.ui.geometry.Size(w * 0.84f, h * 0.76f), androidx.compose.ui.geometry.CornerRadius(10f))
            for ((i, on) in listOf(false, true, true).withIndex()) {
                val x = w * (0.28f + i * 0.22f)
                drawLine(if (on) Color.White else Color(0xFF6A7184), androidx.compose.ui.geometry.Offset(x, h * 0.72f), androidx.compose.ui.geometry.Offset(x, h * 0.32f), 5f)
                drawLine(if (on) Color.White else Color(0xFF6A7184), androidx.compose.ui.geometry.Offset(x - 7f, h * 0.42f), androidx.compose.ui.geometry.Offset(x, h * 0.3f), 5f)
                drawLine(if (on) Color.White else Color(0xFF6A7184), androidx.compose.ui.geometry.Offset(x + 7f, h * 0.42f), androidx.compose.ui.geometry.Offset(x, h * 0.3f), 5f)
            }
        }
        else -> {
            // a blue gantry sign over the road
            drawRect(Color(0xFF596274), androidx.compose.ui.geometry.Offset(w * 0.3f, h * 0.55f), androidx.compose.ui.geometry.Size(w * 0.4f, h * 0.45f))
            drawRoundRect(Color(0xFF1E5BC6), androidx.compose.ui.geometry.Offset(w * 0.12f, h * 0.1f), androidx.compose.ui.geometry.Size(w * 0.76f, h * 0.38f), androidx.compose.ui.geometry.CornerRadius(6f))
            for (i in 0..2) {
                val x = w * (0.3f + i * 0.2f)
                drawLine(if (i == 0) Color(0xFF8FB0E8) else Color.White, androidx.compose.ui.geometry.Offset(x, h * 0.42f), androidx.compose.ui.geometry.Offset(x, h * 0.18f), 4f)
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.speedPicture(id: String, c: Vw) {
    val w = size.width; val h = size.height
    drawRect(Color(0xFF2A3042))
    // the cards on the left, the speed sign on the right at the chosen height
    drawRoundRect(Color(0xFF3A4256), androidx.compose.ui.geometry.Offset(w * 0.06f, h * 0.1f), androidx.compose.ui.geometry.Size(w * 0.36f, h * 0.3f), androidx.compose.ui.geometry.CornerRadius(6f))
    drawRoundRect(Color(0xFF3A4256), androidx.compose.ui.geometry.Offset(w * 0.06f, h * 0.7f), androidx.compose.ui.geometry.Size(w * 0.5f, h * 0.22f), androidx.compose.ui.geometry.CornerRadius(6f))
    val cy = when (id) { "top" -> h * 0.24f; "middle" -> h * 0.5f; else -> h * 0.76f }
    val ctr = androidx.compose.ui.geometry.Offset(w * 0.82f, cy)
    drawCircle(Color.White, h * 0.17f, ctr)
    drawCircle(Color(0xFFE0202A), h * 0.17f, ctr, style = androidx.compose.ui.graphics.drawscope.Stroke(h * 0.05f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.locatorPicture(id: String, c: Vw) {
    val w = size.width; val h = size.height
    drawRect(Color(0xFF2A3042))
    drawRect(Color(0xFF596274), androidx.compose.ui.geometry.Offset(w * 0.38f, 0f), androidx.compose.ui.geometry.Size(w * 0.24f, h))
    val cx = w / 2; val cy = h * 0.55f
    when (id) {
        "car" -> {
            drawRoundRect(Color(0xFF3C8CFF), androidx.compose.ui.geometry.Offset(cx - 10f, cy - 18f), androidx.compose.ui.geometry.Size(20f, 36f), androidx.compose.ui.geometry.CornerRadius(7f))
            drawRoundRect(Color(0xFF1B2A44), androidx.compose.ui.geometry.Offset(cx - 7f, cy - 10f), androidx.compose.ui.geometry.Size(14f, 9f), androidx.compose.ui.geometry.CornerRadius(3f))
        }
        "custom" -> {
            drawCircle(Color(0xFF9CA4BF), 16f, androidx.compose.ui.geometry.Offset(cx, cy))
            drawCircle(Color(0xFF2A3042), 6f, androidx.compose.ui.geometry.Offset(cx, cy - 3f))
        }
        else -> {
            val p = androidx.compose.ui.graphics.Path().apply { moveTo(cx, cy - 20f); lineTo(cx + 15f, cy + 16f); lineTo(cx, cy + 8f); lineTo(cx - 15f, cy + 16f); close() }
            if (id == "flat") drawPath(p, Color.White)
            else {
                drawPath(p, Color(0xFF3C8CFF))
                drawPath(p, Color.White, style = androidx.compose.ui.graphics.drawscope.Stroke(3f))
                if (id == "model") drawCircle(Color(0x553C8CFF), 26f, androidx.compose.ui.geometry.Offset(cx, cy))
            }
        }
    }
}

/**
 * The real map at the top of the map settings pages, so every change shows as it's made. Day / Night show it in that light for a
 * moment (to check the night colours in daylight); Now, or leaving, goes back. It can't be dragged, so the map stays where it was.
 */
@Composable
private fun SettingsMapPreview(engine: NavEngine, heightDp: Int) {
    val c = LocalVw.current
    // (shared with the Custom theme's Day / Night tabs, so the preview shows the one being edited)
    var mode by PreviewLight::mode
    androidx.compose.runtime.DisposableEffect(mode) {
        engine.map.forceLight(when (mode) { "day" -> "day"; "night" -> "night"; else -> null })
        onDispose { }
    }
    androidx.compose.runtime.DisposableEffect(Unit) { engine.previewInSettings(true); onDispose { engine.map.forceLight(null); PreviewLight.mode = "now"; engine.previewInSettings(false) } }
    Box(Modifier.fillMaxWidth().widthIn(max = 820.dp).height(heightDp.dp).padding(bottom = 10.dp).clip(RoundedCornerShape(20.dp)).border(1.dp, c.line, RoundedCornerShape(20.dp))) {
        MapHost(engine.map, Modifier.fillMaxSize(), cornerDp = 20)
        // (touches stop here: the preview only shows)
        Box(Modifier.fillMaxSize().pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent().changes.forEach { it.consume() } } })
        // (the label and the Day / Night / Now tabs share one row, so a long label gives way rather than running under the tabs:
        // "Podgląd na żywo" did in Polish, ChrisSantosGarage)
        Row(Modifier.align(Alignment.TopStart).fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Aperçu en direct", color = Color.White, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false).clip(RoundedCornerShape(10.dp)).background(Color(0x99000000)).padding(horizontal = 10.dp, vertical = 4.dp))
        Box(Modifier.weight(1f).width(8.dp))
        Row(Modifier.clip(RoundedCornerShape(16.dp)).background(Color(0x99000000))) {
            for ((id, label) in listOf("day" to "Jour", "night" to "Nuit", "now" to "Maintenant")) {
                val sel = id == mode
                Box(Modifier.background(if (sel) c.accent else Color.Transparent).pressClick { mode = id }.padding(horizontal = 14.dp, vertical = 7.dp)) {
                    Text(label, color = Color.White, fontSize = 13.sp, fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }
        }
    }
}


/**
 * Card positions: a little picture of a wide screen showing where the turn card, trip card and speedometer will sit, redrawn as the
 * choices change (resolved the same way the map screens do it).
 */
@Composable
private fun CardPositionsPreview(prefs: com.headunit.launcher.Prefs, car: Boolean) {
    val vw = LocalVw.current
    val engineMir = prefs.s(PreferenceKeys.DRIVE, "") == "lhd" ||
        (prefs.s(PreferenceKeys.DRIVE, "") == "" && prefs.s(PreferenceKeys.COUNTRYDETECTED, "").let { it.isNotBlank() && it.uppercase() !in com.headunit.launcher.KEEP_LEFT })
    val mir = if (car) false else when (prefs.s(PreferenceKeys.CARDS_SIDE, "auto")) { "left" -> false; "right" -> true; else -> engineMir }
    val trip = if (car) prefs.s(PreferenceKeys.CAR_TRIP_POS, "auto") else prefs.s(PreferenceKeys.TRIP_POS, "auto")
    val tripAt = if (trip == "auto") (if (mir) "right" else "left") else trip
    val sp = if (car) prefs.s(PreferenceKeys.CAR_SPEED_SIDE, "auto") else prefs.s(PreferenceKeys.SPEED_SIDE, "auto")
    val speedAt = if (sp == "auto") (if (car && trip == "right") "left" else if (mir) "left" else "right") else sp
    val green = Color(0xFF1F7A57)
    val dark = Color(0xFF1B2029)
    androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).height(130.dp).clip(RoundedCornerShape(12.dp))) {
        val w = size.width; val h = size.height
        drawRect(Color(0xFF2A3242))
        drawLine(Color(0xFF3A4558), androidx.compose.ui.geometry.Offset(0f, h * 0.7f), androidx.compose.ui.geometry.Offset(w, h * 0.45f), strokeWidth = h * 0.06f)
        val pad = w * 0.03f
        // the turn card
        val tw = w * 0.32f; val th = h * 0.26f
        val tx = if (mir) w - pad - tw else pad
        drawRoundRect(green, androidx.compose.ui.geometry.Offset(tx, pad), androidx.compose.ui.geometry.Size(tw, th), androidx.compose.ui.geometry.CornerRadius(10f))
        // the speedometer
        val r = h * 0.12f
        val sx = if (speedAt == "left") pad + r else w - pad - r
        // the trip strip
        val sw = w * 0.36f; val sh = h * 0.13f
        val sharing = speedAt == tripAt
        val sy = h - pad - r - (if (sharing) sh + pad else 0f)
        drawCircle(dark, r, androidx.compose.ui.geometry.Offset(sx, sy))
        drawCircle(Color(0xFF5A9BFF), r, androidx.compose.ui.geometry.Offset(sx, sy), style = androidx.compose.ui.graphics.drawscope.Stroke(r * 0.18f))
        val stx = when (tripAt) { "centre" -> (w - sw) / 2; "right" -> w - pad - sw; else -> pad }
        drawRoundRect(dark, androidx.compose.ui.geometry.Offset(stx, h - pad - sh), androidx.compose.ui.geometry.Size(sw, sh), androidx.compose.ui.geometry.CornerRadius(sh / 2))
        drawRoundRect(vw.accent, androidx.compose.ui.geometry.Offset(stx + sw * 0.08f, h - pad - sh * 0.62f), androidx.compose.ui.geometry.Size(sw * 0.3f, sh * 0.24f), androidx.compose.ui.geometry.CornerRadius(4f))
        // your arrow
        val ax = w * 0.5f; val ay = h * 0.55f
        val path = androidx.compose.ui.graphics.Path().apply { moveTo(ax, ay - h * 0.07f); lineTo(ax + h * 0.05f, ay + h * 0.05f); lineTo(ax, ay + h * 0.025f); lineTo(ax - h * 0.05f, ay + h * 0.05f); close() }
        drawPath(path, Color(0xFF9CC7FF))
    }
}

/** Fork FR : une ligne « type d'alerte » avec ses trois choix Écran, Bip, Voix. */
@Composable
private fun AlertWaysRow(prefs: Prefs, engine: NavEngine, g: String, label: String) {
    val c = LocalVw.current
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, color = c.text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for ((ch, name) in listOf("screen" to "Écran", "beep" to "Bip", "voice" to "Voix")) {
                val on = engine.alertOn(g, ch)
                Pill(name, on, { prefs.put("alert_${g}_$ch", !on) })
            }
        }
    }
}

/** GitHub edition: a small banner at the top of the Settings list about the coming Google Play version (tap: the invitation). */
@Composable
private fun PlayBanner(c: Vw, onClick: () -> Unit) {
    val blue = if (c.dark) Color(0xFF8FB6FF) else Color(0xFF1F5FBF)
    Row(
        Modifier.fillMaxWidth().padding(bottom = 6.dp).clip(RoundedCornerShape(18.dp))
            .background(blue.copy(alpha = if (c.dark) 0.14f else 0.10f)).border(1.dp, blue.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(blue.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Shop, null, tint = blue, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text("Horizon Maps arrive sur Google Play", color = c.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("Touchez pour aider à le tester", color = c.sub, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = blue, modifier = Modifier.size(20.dp))
    }
}
