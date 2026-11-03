package com.headunit.launcher.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.headunit.launcher.NavEngine
import com.headunit.launcher.OfflineRouter
import com.headunit.launcher.PackRegions
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.headunit.launcher.Prefs
import java.io.File
import com.headunit.launcher.PreferenceKeys

/**
 * The Mapbox map with no signal: a circle round Home, the current route's strip, and (optionally) every route's strip as it starts.
 * Mapbox keeps these on the device itself; the map uses them automatically when there's no signal.
 */
@Composable
private fun MapboxOfflineGroup(prefs: Prefs, engine: NavEngine) {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) { com.headunit.launcher.MapboxOffline.refresh(ctx) }
    val off = com.headunit.launcher.MapboxOffline
    var confirm by remember { mutableStateOf<String?>(null) }
    SGroup("Carte Mapbox sans réseau", footer = "Mapbox ne laisse chaque appareil garder qu'une surface hors ligne limitée : un rayon autour du domicile et plusieurs itinéraires tiennent, pas un pays entier. Les zones parcourues récemment sont aussi gardées automatiquement.") {
        if (!engine.hasMapbox) SNote("Ajoutez d'abord votre jeton d'accès Mapbox dans Réglages > Clés et comptes.")
        val km = prefs.s(PreferenceKeys.MBHOMEKM, "30").toIntOrNull() ?: 30
        val home = engine.places.slot("home")
        SItem("Autour du domicile", if (home == null) "Définissez d'abord le domicile (cherchez, puis enregistrez un lieu comme domicile)" else "La carte 3D dans un rayon de $km km du domicile · environ ${off.homeMb(km)} Mo")
        // (the distance choice and Save go on a line of their own: beside the title they squeezed it into a narrow column)
        androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Segmented(listOf("15" to "15 km", "30" to "30 km", "50" to "50 km"), km.toString()) { prefs.put(PreferenceKeys.MBHOMEKM, it) }
            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            if (home != null && engine.hasMapbox) SButton(if (off.busy?.startsWith("Autour du domicile") == true) "Enregistrement…" else "Enregistrer", primary = true) { off.saveHome(ctx, home.pos, km) }
        }
        val r = engine.route
        SItem("Enregistrer la carte de cet itinéraire", if (r == null) "Calculez d'abord un itinéraire" else engine.offlineRouteName() + " · une bande le long de l'itinéraire · environ ${off.routeMb(r.len)} Mo") {
            if (r != null && engine.hasMapbox) SButton("Enregistrer", primary = true) { off.saveRoute(ctx, engine.offlineRouteName(), r.pts) }
        }
        SToggle(prefs, "Enregistrer la carte de chaque itinéraire", "mbautoroute", false, "Au démarrage d'un itinéraire, sa bande de carte est enregistrée en arrière-plan")
        if (prefs.b(PreferenceKeys.MBAUTOROUTE, false)) SChoice("Enregistrer les itinéraires", listOf("wifi" to "En Wi-Fi seulement", "always" to "Toujours (utilise les données mobiles)"), prefs.s(PreferenceKeys.MAPBOX_AUTO_NETWORK, "wifi")) { prefs.put(PreferenceKeys.MAPBOX_AUTO_NETWORK, it) }
        if (off.saved.isNotEmpty() || off.busy != null) {
            SItem("Enregistré", null)
            for (a in off.saved) {
                SItem(a.name, "%.0f MB".format(a.mb) + if (a.complete) " · enregistré" else " · partiellement enregistré") {
                    if (confirm == a.id) {
                        SButton("Oui, supprimer", danger = true) { confirm = null; off.delete(ctx, a.id) }
                        SButton("Garder") { confirm = null }
                    } else SButton("Supprimer", danger = true) { confirm = a.id }
                }
            }
        }
        if (off.busy != null) {
            SItem(off.busy ?: "", off.status) { SButton("Arrêter") { off.stop() } }
            LinearProgressIndicator(progress = { off.progress }, color = LocalVw.current.accent, trackColor = LocalVw.current.tile, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))
        } else if (off.status.isNotBlank()) SNote(off.status)
    }
}

private fun fmtSize(b: Long): String = if (b >= 1_000_000_000L) "%.1f GB".format(b / 1e9) else "%.0f MB".format(b / 1e6)

/** The downloaded areas: what's on the unit, which is in use, add another, update and delete. */
@Composable
internal fun OfflineMapsContent(prefs: Prefs, engine: NavEngine) {
    val c = LocalVw.current
    val ctx = LocalContext.current
    @Suppress("UNUSED_VARIABLE") val rev = engine.packRev // re-read the files when a download, switch or delete finishes
    @Suppress("UNUSED_VARIABLE") val pick = prefs.s(PreferenceKeys.PACK_REGION, "")   // redraw when the area to add changes
    val region = engine.chosenPack()
    val have = engine.installedPacks()
    val activeId = engine.installedPackId()
    var confirmDelete by remember { mutableStateOf<String?>(null) }
    var picking by remember { mutableStateOf(false) }
    // has the area to add been built yet? (checked when it changes)
    var avail by remember(region.id) { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(region.id, have.size) { if (region.id !in have) avail = withContext(Dispatchers.IO) { PackRegions.available(region) } }
    // sizes and free space are worked out in the background (a pack's font folder holds thousands of small files)
    val sizes by androidx.compose.runtime.produceState(emptyMap<String, Long>(), have, rev) {
        value = withContext(Dispatchers.IO) { have.associateWith { OfflineRouter.sizeOf(OfflineRouter.packDir(ctx, it)) } }
    }
    // areas downloaded before the speed limit and camera files existed: has the file been published for them since?
    val extrasReady by androidx.compose.runtime.produceState(emptySet<String>(), have, rev) {
        value = withContext(Dispatchers.IO) {
            have.filter { !File(OfflineRouter.packDir(ctx, it), "limits.bin").exists() && PackRegions.hasExtras(PackRegions.byId(it)) == true }.toSet()
        }
    }
    val free by androidx.compose.runtime.produceState(-1L, rev) {
        value = withContext(Dispatchers.IO) { try { android.os.StatFs(OfflineRouter.packsRoot(ctx).path).availableBytes } catch (e: Exception) { -1L } }
    }

    MapboxOfflineGroup(prefs, engine)
    SGroup("Zones téléchargées", footer = "Gardez autant de zones que vous voulez : celle où vous êtes est utilisée automatiquement (les itinéraires hors ligne fonctionnent à l'intérieur d'une zone). Chacune est une photo des routes au jour de sa création, mettez-la à jour de temps en temps." + if (free >= 0) " ${fmtSize(free)} libres." else "") {
        if (have.isEmpty()) SNote("Rien de téléchargé. Choisissez une zone ci-dessous et téléchargez-la en Wi-Fi.")
        for (id in have) {
            val r = PackRegions.byId(id)
            val d = OfflineRouter.packDir(ctx, id)
            val tiles = File(d, "tiles.tar")
            val built = if (tiles.exists()) java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.UK).format(java.util.Date(tiles.lastModified())) else ""
            val inUse = id == activeId
            val hasLimits = File(d, "limits.bin").exists()
            SItem(r.name + if (inUse) " · utilisée" else "", (sizes[id]?.let { fmtSize(it) } ?: "…") + (if (built.isNotEmpty()) " · créée le $built" else "") +
                (if (hasLimits) " · limitations et radars hors ligne" else "")) {
                if (confirmDelete == id) {
                    SButton("Oui, supprimer", danger = true) { confirmDelete = null; engine.deletePack(id) }
                    SButton("Garder") { confirmDelete = null }
                } else {
                    if (!inUse && have.size > 1) SButton("Utiliser") { engine.useArea(id) }
                    SButton(if (engine.packDownloading == id) "Mise à jour…" else "Mettre à jour") { engine.downloadRegion(r) }
                    SButton("Supprimer", danger = true) { confirmDelete = id }
                }
            }
        }
        for (id in extrasReady) if (engine.packDownloading != id)
            SNote("Limitations et radars hors ligne disponibles pour ${PackRegions.byId(id).name} : touchez sa mise à jour (seuls les nouveaux fichiers sont téléchargés, en général moins de 100 Mo).")
        val age = engine.packAgeDays()
        if (age != null && age >= 120 && !engine.packBusy) SNote("La zone utilisée date d'environ ${age / 30} mois. Mettez-la à jour en Wi-Fi pour les nouvelles routes et carrefours.")
    }
    SGroup("Ajouter une zone") {
        SItem("Zone", region.name + " · " + region.group) { SButton("Changer") { picking = true } }
        if (region.id !in have && avail == false) SNote("Le pack ${region.name} n'a pas encore été créé, il ne peut pas être téléchargé pour l'instant.")
        SItem(if (region.id in have) "${region.name} est déjà téléchargé" else "Télécharger ${region.name}", "De préférence en Wi-Fi : un pays fait en général 1 à 4 Go") {
            SButton(if (engine.packBusy) "Téléchargement…" else if (region.id in have) "Mettre à jour" else "Télécharger", primary = true) { engine.downloadRegion(region) }
        }
        if (engine.packBusy) LinearProgressIndicator(color = c.accent, trackColor = c.tile, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))
        if (engine.packStatus.isNotBlank()) SNote((engine.packDownloading?.let { PackRegions.byId(it).name + ": " } ?: "") + engine.packStatus + if (engine.packBusy) " · vous pouvez quitter cette page" else "")
    }
    SGroup("Sans réseau") {
        SChoice("Carte à afficher", listOf("auto" to "Auto", "always" to "Téléchargée", "never" to "En ligne"), prefs.s(PreferenceKeys.OFF_MAP, "auto"),
            "Auto : sans réseau, la carte Mapbox continue avec les zones en mémoire (là où vous êtes passé récemment, et les zones Mapbox enregistrées) ; la carte du pack téléchargé ne prend le relais que là où Mapbox n'a rien, et la carte en ligne revient avec le réseau") { prefs.put(PreferenceKeys.OFF_MAP, it); engine.restyle() }
    }
    if (picking) PackPickerDialog(region, { prefs.put(PreferenceKeys.PACK_REGION, it.id) }, engine.fix?.pos ?: engine.map.center()) { picking = false }
}
