package com.headunit.launcher

import android.location.Location
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.abs
import kotlin.math.cos

/**
 * Fork FR : moteur de limitations repris de MG4Dash (ASSE03/MG4Dash-SWI68, map/SpeedLimit.kt), mêmes paquets.
 *
 * Limitation de vitesse de la route — PORTAGE SWI68.
 *
 * Tirée d'OpenStreetMap (étiquette maxspeed, sinon type de zone français). Depuis le 2026-10-22, d'abord
 * par paquets de 1° × 1° préparés chaque mois pour la France (release « speedlimits », voir plus bas),
 * gardés sur la voiture ; l'API Overpass ne sert plus qu'en dehors des paquets.
 * Relevés du 2026-10-20/21 : depuis la voiture (4G de la T-box), les serveurs Overpass répondent mal
 * (SSLHandshakeException, puis « timeout » même sur une zone de 1 km). Les routes sont donc chargées
 * par petites tuiles de [TILE]° (≈ 1,1 × 0,75 km), d'abord celle de la voiture puis celles devant
 * elle, et **gardées sur le stockage** ([KEEP_DAYS] jours) : sur les trajets déjà faits, la limitation
 * s'affiche tout de suite, même sans réseau. La route la plus proche, orientée comme la voiture, est
 * cherchée sur place à chaque position.
 */
object MgSpeedLimit {
    private const val TAG = "Limitation"
    // Relevé le 2026-10-20 : depuis la voiture, overpass-api.de échoue en HTTPS (SSLHandshakeException,
    // Android 9) et kumi.systems finit en timeout. Plusieurs serveurs, et repli en HTTP simple si HTTPS échoue.
    private val SERVERS = listOf("overpass-api.de/api/interpreter", "overpass.private.coffee/api/interpreter",
        "maps.mail.ru/osm/tools/overpass/api/interpreter", "overpass.kumi.systems/api/interpreter")
    // overpass.osm.jp retiré le 2026-10-22 : HTTP 404 depuis la voiture
    private const val ROADS = "motorway|trunk|primary|secondary|tertiary|unclassified|residential|living_street|" +
        "motorway_link|trunk_link|primary_link|secondary_link|tertiary_link|road"
    private const val TILE = 0.01
    private const val E5 = 100_000.0
    private const val AROUND_M = 300.0
    private const val AHEAD_M = 700.0
    private const val KEEP_DAYS = 60L
    private const val MATCH_M = 30.0
    private const val MAX_ACCURACY_M = 50f
    /**
     * À l'arrêt (démarrage, stationnement) : position moins précise acceptée, route cherchée plus loin
     * (parking, cour) et cap ignoré (il ne veut rien dire à l'arrêt). Relevé le 2026-10-21 : limitation
     * voulue dès le démarrage, sans rouler.
     */
    private const val STILL_ACCURACY_M = 100f
    private const val STILL_MATCH_M = 150.0
    @Volatile private var lastFix: Location? = null
    /** Retard de la position GPS : la recherche se fait un peu en avant (relevé le 2026-10-21, 70 → 90 trop lent). */
    private const val LOOKAHEAD_S = 1.5f
    /** Limitation gardée tant qu'aucune route n'est trouvée (croisement, GPS qui saute) avant d'effacer. */
    private const val HOLD_MS = 10_000L
    private var lastFoundAt = 0L

    private val _limit = MutableStateFlow<Int?>(null)
    /** Limitation en km/h de la route en cours, ou null si inconnue. */
    val limit: StateFlow<Int?> = _limit
    @Volatile var status: String = "pas encore demandée (en attente d'une position)"; private set

    /** Route : points en 1e-5 ° (entiers : quatre fois moins de mémoire pour les paquets). */
    private class Way(val lat: IntArray, val lon: IntArray, val limit: Int?, val label: String,
                      /** Indice dans [PACK_ROADS], -1 si inconnu ; [dual] = sens unique (chaussées séparées). */
                      val cls: Int = -1, val dual: Boolean = false)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val loading = java.util.concurrent.atomic.AtomicBoolean(false)
    private val tiles = java.util.concurrent.ConcurrentHashMap<String, List<Way>>()
    @Volatile private var wanted: List<String> = emptyList()
    @Volatile private var ways: List<Way> = emptyList()
    @Volatile private var dir: java.io.File? = null
    @Volatile private var nextNetTry = 0L
    private var failures = 0
    private var server = 0
    private var prev: Location? = null
    @Volatile private var zoneInfo = "aucune tuile chargée"
    private var fromDisk = 0
    /** Dernier résultat de chaque serveur, pour le Diagnostic. */
    private val serverState = java.util.concurrent.ConcurrentHashMap<String, String>()
    private var fromNet = 0

    /** Dossier des tuiles gardées (appelé au démarrage par LocationRepo). */
    fun init(context: android.content.Context) {
        if (appCtx != null) return
        appCtx = context.applicationContext
        dir = java.io.File(context.filesDir, "speedlimit").apply { mkdirs() }
        packDir = java.io.File(context.filesDir, "speedlimit_packs").apply { mkdirs() }
    }

    // ── Paquets de 1° × 1° (France, OpenStreetMap, générés chaque mois par le workflow
    //    « Limitations de vitesse » et publiés dans la release « speedlimits » du dépôt) ──
    // Relevé le 2026-10-22 : aucun serveur Overpass ne répondait depuis la 4G de la voiture (502,
    // timeouts, certificat). GitHub, lui, répond (mises à jour) : les routes viennent de là.

    private const val PACKS_API = "https://api.github.com/repos/ASSE03/MG4Dash-SWI68/releases/tags/speedlimits"
    private const val PACK_AHEAD_M = 4000.0
    private const val MAX_PACKS = 3   // environ 20 Mo en mémoire par paquet dense (Lyon, Paris)
    private val PACK_ROADS = ROADS.split('|')
    private val PACK_LABELS = PACK_ROADS.map { "$it (paquet OSM)" }
    @Volatile private var appCtx: android.content.Context? = null
    @Volatile private var packDir: java.io.File? = null
    private val packs = java.util.concurrent.ConcurrentHashMap<String, Map<String, List<Way>>>()
    private val packUse = java.util.concurrent.ConcurrentHashMap<String, Long>()
    private val packNextTry = java.util.concurrent.ConcurrentHashMap<String, Long>()
    private class Asset(val url: String, val updated: Long, val size: Long)
    @Volatile private var index: Map<String, Asset>? = null
    @Volatile private var indexAt = 0L
    @Volatile private var indexNextTry = 0L
    @Volatile private var packInfo = "paquets : —"

    /** Nom du paquet d'une case de 0,01° « tx_ty » : « p<lat>_<lon>.bin ». */
    private fun packOf(cell: String): String {
        val i = cell.indexOf('_')
        val tx = cell.substring(0, i).toInt(); val ty = cell.substring(i + 1).toInt()
        return "p${Math.floorDiv(ty, 100)}_${Math.floorDiv(tx, 100)}.bin"
    }

    private fun packAt(lat: Double, lon: Double) = "p${Math.floor(lat).toInt()}_${Math.floor(lon).toInt()}.bin"

    /** Paquets des cases voulues, plus celui à [PACK_AHEAD_M] devant (téléchargé avant d'y entrer). */
    private fun packsWanted(): List<String> {
        val out = LinkedHashSet<String>()
        wanted.forEach { out += packOf(it) }
        lastFix?.let { f ->
            if (f.hasBearing() && f.hasSpeed() && f.speed > 3f) {
                val b = Math.toRadians(f.bearing.toDouble())
                out += packAt(f.latitude + PACK_AHEAD_M * Math.cos(b) / 110_540.0,
                    f.longitude + PACK_AHEAD_M * Math.sin(b) / (cos(Math.toRadians(f.latitude)) * 111_320.0))
            }
        }
        return out.toList()
    }

    /** Reste-t-il quelque chose à faire pour ce paquet (charger, télécharger, rafraîchir) ? */
    private fun packWorth(p: String): Boolean {
        if (android.os.SystemClock.uptimeMillis() < (packNextTry[p] ?: 0L)) return false
        if (!packs.containsKey(p)) return true
        val a = index?.get(p) ?: return false
        val f = packDir?.let { java.io.File(it, p) } ?: return false
        return a.updated > f.lastModified()
    }

    /** Charge le paquet (stockage, ou téléchargement s'il manque ou si une version plus récente existe). */
    private fun ensurePack(p: String) {
        val d = packDir ?: return
        val f = java.io.File(d, p)
        val idx = loadIndex()
        val asset = idx?.get(p)
        val stale = asset != null && f.exists() && asset.updated > f.lastModified()
        if (asset != null && (!f.exists() || stale)) {
            try {
                downloadPack(asset, f)
                packs.remove(p)
                packInfo = "paquets : $p téléchargé (${f.length() / 1024} Ko) à ${hhmmss()}"
                android.util.Log.i(TAG, packInfo)
            } catch (e: Exception) {
                packNextTry[p] = android.os.SystemClock.uptimeMillis() + 60_000L
                packInfo = "paquets : $p, téléchargement en échec (${e.javaClass.simpleName} ${e.message.orEmpty().take(40)}) à ${hhmmss()}"
                android.util.Log.w(TAG, packInfo)
                if (!f.exists()) return
            }
        }
        if (!f.exists()) {
            // Hors de France (pas de paquet) ou index illisible : Overpass prend le relais
            packNextTry[p] = android.os.SystemClock.uptimeMillis() + if (idx == null) 60_000L else 3_600_000L
            if (idx != null) packInfo = "paquets : aucun pour $p (hors zone), Overpass"
            return
        }
        if (packs.containsKey(p)) return
        try {
            val t0 = android.os.SystemClock.uptimeMillis()
            val cells = readPack(f, p)
            packs[p] = cells
            packUse[p] = android.os.SystemClock.uptimeMillis()
            if (packs.size > MAX_PACKS) {
                val keep = packsWanted().toSet()
                packs.keys.filter { it !in keep }.sortedBy { packUse[it] ?: 0L }.take(packs.size - MAX_PACKS).forEach { packs.remove(it) }
            }
            rebuild(); lastFix?.let { match(it) }
            packInfo = "paquets : $p chargé (${cells.size} cases, ${android.os.SystemClock.uptimeMillis() - t0} ms)" +
                (index?.let { " · ${it.size} en ligne" } ?: "")
            android.util.Log.i(TAG, packInfo)
        } catch (e: Exception) {
            f.delete()
            packNextTry[p] = android.os.SystemClock.uptimeMillis() + 60_000L
            packInfo = "paquets : $p illisible (${e.message}), supprimé"
            android.util.Log.w(TAG, packInfo)
        }
    }

    /** Liste des paquets de la release, rafraîchie une fois par jour ; gardée sur le stockage. */
    private fun loadIndex(): Map<String, Asset>? {
        val now = System.currentTimeMillis()
        index?.let { if (now - indexAt < 86_400_000L) return it }
        val cache = packDir?.let { java.io.File(it, "release.json") }
        if (android.os.SystemClock.uptimeMillis() >= indexNextTry) {
            try {
                val token = appCtx?.let { Prefs(it).s(DiagUpload.TOKEN_KEY, "").trim() }.orEmpty()
                val c = URL(PACKS_API).openConnection() as HttpURLConnection
                val body = try {
                    c.connectTimeout = 10_000; c.readTimeout = 20_000
                    c.setRequestProperty("Accept", "application/vnd.github+json")
                    c.setRequestProperty("User-Agent", "HorizonNavFR")
                    if (token.isNotEmpty()) c.setRequestProperty("Authorization", "Bearer $token")
                    if (c.responseCode != 200) throw IllegalStateException("HTTP ${c.responseCode}${if (token.isEmpty()) " (jeton GitHub absent)" else ""}")
                    c.inputStream.bufferedReader().use { it.readText() }
                } finally { c.disconnect() }
                cache?.writeText(body)
                index = parseIndex(body); indexAt = now
                return index
            } catch (e: Exception) {
                indexNextTry = android.os.SystemClock.uptimeMillis() + 120_000L
                packInfo = "paquets : liste illisible (${e.javaClass.simpleName} ${e.message.orEmpty().take(50)})"
                android.util.Log.w(TAG, packInfo)
            }
        }
        // Sans réseau : dernière liste connue
        if (index == null && cache != null && cache.exists()) try { index = parseIndex(cache.readText()); indexAt = 0L } catch (_: Exception) {}
        return index
    }

    private fun parseIndex(body: String): Map<String, Asset> {
        val iso = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
        val arr = JSONObject(body).optJSONArray("assets") ?: return emptyMap()
        val out = HashMap<String, Asset>()
        for (i in 0 until arr.length()) {
            val a = arr.getJSONObject(i)
            val name = a.optString("name")
            if (!name.startsWith("p") || !name.endsWith(".bin")) continue
            val upd = try { iso.parse(a.optString("updated_at"))?.time ?: 0L } catch (_: Exception) { 0L }
            out[name] = Asset(a.optString("url"), upd, a.optLong("size"))
        }
        return out
    }

    /** Asset de release : par l'API avec le jeton (dépôt privé), puis le lien signé sans jeton. */
    private fun downloadPack(a: Asset, f: java.io.File) {
        val token = appCtx?.let { Prefs(it).s(DiagUpload.TOKEN_KEY, "").trim() }.orEmpty()
        var url = URL(a.url); var hops = 0
        while (true) {
            val c = url.openConnection() as HttpURLConnection
            try {
                c.instanceFollowRedirects = false
                c.connectTimeout = 10_000; c.readTimeout = 60_000
                c.setRequestProperty("User-Agent", "HorizonNavFR")
                if (hops == 0) {
                    c.setRequestProperty("Accept", "application/octet-stream")
                    if (token.isNotEmpty()) c.setRequestProperty("Authorization", "Bearer $token")
                }
                val code = c.responseCode
                if (code in 300..399 && hops < 5) { url = URL(url, c.getHeaderField("Location")); hops++; continue }
                if (code != 200) throw IllegalStateException("HTTP $code")
                val tmp = java.io.File(f.path + ".tmp")
                c.inputStream.use { ins -> tmp.outputStream().use { ins.copyTo(it) } }
                if (!tmp.renameTo(f)) throw IllegalStateException("enregistrement impossible")
                return
            } finally { c.disconnect() }
        }
    }

    /**
     * Lit un paquet (format de tools/speedlimit_packs.py) et range chaque route dans les cases de
     * 0,01° qu'elle traverse (segments échantillonnés : un long segment traverse des cases sans point).
     */
    private fun readPack(f: java.io.File, p: String): Map<String, List<Way>> {
        val (tlat, tlon) = p.removePrefix("p").removeSuffix(".bin").split('_').map { it.toInt() }
        val ins = java.io.DataInputStream(java.io.BufferedInputStream(java.util.zip.GZIPInputStream(java.io.FileInputStream(f)), 65536))
        val cells = HashMap<String, MutableList<Way>>()
        ins.use { s ->
            fun varint(): Long {
                var r = 0L; var shift = 0
                while (true) {
                    val b = s.read(); if (b < 0) throw java.io.EOFException()
                    r = r or ((b and 0x7F).toLong() shl shift); shift += 7
                    if (b < 0x80) return r
                }
            }
            fun zz(n: Long): Int = ((n ushr 1) xor -(n and 1)).toInt()
            val seen = HashSet<Long>()
            while (true) {
                val lim = s.read(); if (lim < 0) break
                val clsByte = s.read()
                // Bit 7 : sens unique (chaussées séparées), ajouté le 2026-10-23 ; anciens paquets sans
                val cls = clsByte and 0x7F; val dual = clsByte and 0x80 != 0
                val n = varint().toInt()
                val lat = IntArray(n); val lon = IntArray(n)
                var la = tlat * 100_000; var lo = tlon * 100_000
                for (i in 0 until n) { la += zz(varint()); lo += zz(varint()); lat[i] = la; lon[i] = lo }
                val w = Way(lat, lon, lim.takeIf { it > 0 }, PACK_LABELS.getOrElse(cls) { "route (paquet OSM)" }, cls, dual)
                // Cases de 0,01° (1000 unités de 1e-5 °) traversées, pas d'échantillonnage de 500 unités
                seen.clear()
                for (i in 0 until n) {
                    val steps = if (i == 0) 0 else maxOf(abs(lat[i] - lat[i - 1]), abs(lon[i] - lon[i - 1])) / 500
                    for (k in 0..steps) {
                        val fr = if (steps == 0) 1.0 else k.toDouble() / steps
                        val a = if (i == 0) lat[0].toDouble() else lat[i - 1] + (lat[i] - lat[i - 1]) * fr
                        val o = if (i == 0) lon[0].toDouble() else lon[i - 1] + (lon[i] - lon[i - 1]) * fr
                        val ty = Math.floor(a / 1000.0).toLong(); val tx = Math.floor(o / 1000.0).toLong()
                        // Seules les cases de ce paquet (une route qui en sort est aussi dans le paquet voisin)
                        if (Math.floorDiv(ty, 100L) != tlat.toLong() || Math.floorDiv(tx, 100L) != tlon.toLong()) continue
                        if (seen.add((tx shl 32) xor (ty and 0xFFFFFFFFL))) cells.getOrPut("${tx}_${ty}") { ArrayList(4) } += w
                    }
                }
            }
        }
        return cells
    }

    fun onLocation(loc: Location) {
        // Positions imprécises (réseau mobile) : inutilisables pour trouver la route, on les ignore
        val still = !(loc.hasSpeed() && loc.speed > 2f)
        if (loc.hasAccuracy() && loc.accuracy > (if (still) STILL_ACCURACY_M else MAX_ACCURACY_M)) {
            status = "position ignorée (${loc.provider}, précision ${loc.accuracy.toInt()} m) · $zoneInfo"
            return
        }
        // Cap : celui de la position, sinon celui du déplacement depuis la position précédente
        val p = prev
        if (!loc.hasBearing() && p != null && p.distanceTo(loc) > 5f) {
            loc.bearing = p.bearingTo(loc).let { if (it < 0) it + 360 else it }
        }
        if (p == null || p.distanceTo(loc) > 5f) prev = Location(loc)
        lastFix = Location(loc)
        val keys = tilesFor(loc)
        if (keys != wanted) { wanted = keys; rebuild() }
        match(loc)
        kick()
    }

    /** Tuile de la voiture d'abord, puis celles à [AROUND_M] autour et celles [AHEAD_M] devant. */
    private fun tilesFor(loc: Location): List<String> {
        val out = LinkedHashSet<String>()
        fun add(lat: Double, lon: Double, r: Double) {
            val dLat = r / 110_540.0; val dLon = r / (cos(Math.toRadians(lat)) * 111_320.0)
            for (ty in Math.floor((lat - dLat) / TILE).toInt()..Math.floor((lat + dLat) / TILE).toInt())
                for (tx in Math.floor((lon - dLon) / TILE).toInt()..Math.floor((lon + dLon) / TILE).toInt()) out += "${tx}_${ty}"
        }
        add(loc.latitude, loc.longitude, 0.0)
        add(loc.latitude, loc.longitude, AROUND_M)
        if (loc.hasBearing() && loc.hasSpeed() && loc.speed > 3f) {
            val b = Math.toRadians(loc.bearing.toDouble())
            add(loc.latitude + AHEAD_M * Math.cos(b) / 110_540.0,
                loc.longitude + AHEAD_M * Math.sin(b) / (cos(Math.toRadians(loc.latitude)) * 111_320.0), 0.0)
        }
        return out.toList()
    }

    private fun rebuild() { ways = wanted.mapNotNull { cellWays(it) }.flatten() }

    /** Routes d'une case de 0,01° : du paquet de la zone s'il est chargé, sinon d'Overpass. */
    private fun cellWays(k: String): List<Way>? = packs[packOf(k)]?.let { it[k] ?: emptyList() } ?: tiles[k]
    private fun covered(k: String) = packs.containsKey(packOf(k)) || tiles.containsKey(k)

    /** Charge les tuiles manquantes : du stockage si elles y sont, sinon d'Overpass (une à la fois). */
    private fun kick() {
        val needPacks = packsWanted()
        if (needPacks.none { packWorth(it) } && wanted.all { covered(it) }) return
        if (!loading.compareAndSet(false, true)) return
        scope.launch {
            try {
                // 1) Paquets de la zone (release « speedlimits » du dépôt) : la voie normale
                for (p in needPacks) if (packWorth(p)) ensurePack(p)
                // 2) Overpass seulement là où aucun paquet n'existe ou ne se télécharge pas
                for (k in wanted.toList()) {
                    if (covered(k)) continue
                    val saved = readDisk(k)
                    if (saved != null) { tiles[k] = saved; fromDisk++; rebuild(); lastFix?.let { match(it) }; continue }
                    val now = android.os.SystemClock.uptimeMillis()
                    if (now < nextNetTry) break
                    try {
                        val body = download(k)
                        val list = parseWays(body)
                        writeDisk(k, body)
                        tiles[k] = list; fromNet++; failures = 0; rebuild()
                        // À l'arrêt, les positions peuvent se faire rares : la route est cherchée dès la tuile reçue
                        lastFix?.let { match(it) }
                        serverState[host()] = "ok"
                        zoneInfo = "tuile ${list.size} route(s) par ${host()} à ${hhmmss()}"
                        android.util.Log.i(TAG, zoneInfo)
                    } catch (e: Exception) {
                        failures++
                        nextNetTry = now + minOf(30_000L, 3_000L * failures)
                        serverState[host()] = "${e.javaClass.simpleName.removeSuffix("Exception")} ${e.message.orEmpty().take(40)}"
                        zoneInfo = "erreurs (${hhmmss()}) : " + serverState.entries.joinToString(" ; ") { "${it.key} ${it.value}" }
                        android.util.Log.w(TAG, zoneInfo)
                        server = (server + 1) % SERVERS.size
                        break
                    }
                }
                // Mémoire : seules les tuiles utiles restent chargées
                if (tiles.size > 60) tiles.keys.filter { it !in wanted }.forEach { tiles.remove(it) }
            } finally { loading.set(false) }
        }
    }

    private fun file(k: String) = dir?.let { java.io.File(it, "$k.json") }

    private fun readDisk(k: String): List<Way>? {
        val f = file(k) ?: return null
        if (!f.exists()) return null
        if (System.currentTimeMillis() - f.lastModified() > KEEP_DAYS * 86_400_000L) return null
        return try { parseWays(f.readText()) } catch (e: Exception) { f.delete(); null }
    }

    private fun writeDisk(k: String, body: String) {
        val f = file(k) ?: return
        try { val t = java.io.File(f.path + ".tmp"); t.writeText(body); t.renameTo(f) } catch (_: Exception) {}
    }

    private fun host() = SERVERS[server].substringBefore("/")
    private fun hhmmss() = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.FRANCE).format(java.util.Date())

    private fun download(k: String): String {
        val (tx, ty) = k.split('_').map { it.toInt() }
        val s = ty * TILE; val w = tx * TILE
        val bbox = String.format(java.util.Locale.US, "%.5f,%.5f,%.5f,%.5f", s, w, s + TILE, w + TILE)
        val q = "[out:json][timeout:20];way($bbox)[highway~\"^($ROADS)$\"];out tags geom;"
        return try { post("https://" + SERVERS[server], q) }
            catch (e: javax.net.ssl.SSLException) {
                android.util.Log.w(TAG, "${host()} : HTTPS refusé (${e.message}), essai en HTTP")
                post("http://" + SERVERS[server], q)
            }
    }

    /**
     * Requête GET (?data=…) : acceptée par tous les serveurs Overpass, alors que certains refusent le
     * POST (overpass.osm.jp : HTTP 405, relevé le 2026-10-22). Redirections suivies (maps.mail.ru : 301).
     */
    private fun post(url: String, q: String, hops: Int = 0): String {
        val full = if (hops == 0) url + "?data=" + URLEncoder.encode(q, "UTF-8") else url
        val c = URL(full).openConnection() as HttpURLConnection
        val body = try {
            c.connectTimeout = 10_000; c.readTimeout = 30_000
            c.instanceFollowRedirects = false
            c.setRequestProperty("User-Agent", "HorizonNavFR")
            val code = c.responseCode
            if (code in 301..308 && hops < 2) c.getHeaderField("Location")?.let { return post(URL(URL(full), it).toString(), q, hops + 1) }
            if (code != 200) throw IllegalStateException("HTTP $code")
            c.inputStream.bufferedReader().use { it.readText() }
        } finally { c.disconnect() }
        return body
    }

    private fun parseWays(body: String): List<Way> {
        val els = JSONObject(body).optJSONArray("elements") ?: return emptyList()
        val out = ArrayList<Way>(els.length())
        for (i in 0 until els.length()) {
            val w = els.getJSONObject(i)
            val geom = w.optJSONArray("geometry") ?: continue
            if (geom.length() < 2) continue
            val tags = w.optJSONObject("tags") ?: JSONObject()
            val lat = IntArray(geom.length()) { Math.round(geom.getJSONObject(it).getDouble("lat") * E5).toInt() }
            val lon = IntArray(geom.length()) { Math.round(geom.getJSONObject(it).getDouble("lon") * E5).toInt() }
            out += Way(lat, lon, limitOf(tags),
                "${tags.optString("highway")} « ${tags.optString("name")} » maxspeed=${tags.optString("maxspeed").ifBlank { "—" }}",
                PACK_ROADS.indexOf(tags.optString("highway")), tags.optString("oneway") in setOf("yes", "1", "-1"))
        }
        return out
    }

    /** Route la plus proche (à 30 m au plus), pénalisée si elle n'est pas orientée comme la voiture. */
    private fun match(fix: Location) {
        val list = ways
        if (list.isEmpty()) {
            if (android.os.SystemClock.uptimeMillis() - lastFoundAt >= HOLD_MS && _limit.value != null) _limit.value = null
            status = "inconnue · routes pas encore chargées · $packInfo · tuiles 0/${wanted.size} (stockage $fromDisk, réseau $fromNet) · $zoneInfo"
            return
        }
        // Position projetée en avant selon la vitesse, pour compenser le retard du GPS
        val loc = Location(fix)
        // fork FR : pas de projection en avant pendant un virage (rond-point) : en ligne droite, elle tombait sur une route voisine
        val turning = fix.hasBearing() && prevBearing >= 0f && run { var t = abs(fix.bearing - prevBearing) % 360f; if (t > 180f) t = 360f - t; t > 12f }
        if (fix.hasBearing()) prevBearing = fix.bearing
        if (fix.hasSpeed() && fix.speed > 5f && fix.hasBearing() && !turning) {
            val d = fix.speed * LOOKAHEAD_S
            val b = Math.toRadians(fix.bearing.toDouble())
            loc.latitude = fix.latitude + d * Math.cos(b) / 110_540.0
            loc.longitude = fix.longitude + d * Math.sin(b) / (cos(Math.toRadians(fix.latitude)) * 111_320.0)
        }
        val still = !(fix.hasSpeed() && fix.speed > 2f)
        val maxD = if (still) STILL_MATCH_M else MATCH_M + 40
        var best: Way? = null; var bestD = Double.MAX_VALUE
        for (w in list) {
            var d = Double.MAX_VALUE; var segBearing = 0.0
            for (j in 0 until w.lat.size - 1) {
                val (dist, brg) = segment(loc, w.lat[j] / E5, w.lon[j] / E5, w.lat[j + 1] / E5, w.lon[j + 1] / E5)
                if (dist < d) { d = dist; segBearing = brg }
            }
            if (d > maxD) continue
            if (!still && loc.hasBearing()) {
                var diff = abs(((loc.bearing - segBearing) % 180 + 180) % 180)
                if (diff > 90) diff = 180 - diff
                if (diff > 35) d += 40
            }
            // fork FR : la route suivie jusqu'ici est préférée (10 m d'avance) : au milieu du rond-point près de Davézieux, une rue
            // qui y débouche (Chemin de Gourdan, 50) passait devant l'anneau (90)
            if (w === lastWay) d -= 10.0
            if (d < bestD) { bestD = d; best = w }
        }
        val found = best?.takeIf { bestD <= maxD }
        if (found != null) lastWay = found
        val now = android.os.SystemClock.uptimeMillis()
        var est = ""
        val v = if (found != null) { lastFoundAt = now; found.limit ?: estimate(found, loc, list)?.also { est = " (estimée)" } }
            else if (now - lastFoundAt < HOLD_MS) _limit.value   // route perdue un instant : on garde l'affichage
            else null
        if (_limit.value != v) _limit.value = v
        estimated = est.isNotEmpty() || (found == null && estimated)
        val acc = if (fix.hasAccuracy()) "±${fix.accuracy.toInt()} m" else "précision ?"
        val ready = wanted.count { covered(it) }
        status = "${v ?: "inconnue"}$est · $packInfo · ${found?.label ?: if (still) "aucune route à 150 m (arrêt)" else "aucune route à 30 m"} · position ${fix.provider} $acc à ${hhmmss()} · " +
            "tuiles $ready/${wanted.size} (stockage $fromDisk, réseau $fromNet) · $zoneInfo"
    }

    /**
     * Portage SWI68 (2026-10-23) : route sans limitation dans OpenStreetMap → limitation par défaut
     * française, jamais une valeur inventée : 50 en agglomération, 80 hors agglomération, 110 sur
     * une voie rapide à chaussées séparées. Agglomération = au moins 3 rues résidentielles à 150 m.
     * Rien pour les bretelles (limitations trop variables).
     */
    private fun estimate(w: Way, loc: Location, list: List<Way>): Int? {
        if (!estimateOn) return null
        val road = PACK_ROADS.getOrNull(w.cls) ?: return null
        if (road == "motorway") return 130
        if (road.endsWith("_link") && road != "primary_link" && road != "secondary_link" && road != "tertiary_link") return null
        if (road == "trunk" && w.dual) return 110
        // fork FR : agglomération = des rues résidentielles proches (2 points chacune) ou des petites routes sans classement
        // (1 point) : à 4 points, c'est 50. Avant, il fallait 3 rues résidentielles, et un village dont les rues sont
        // « unclassified » donnait 80 en plein bourg (Saint-Cyr, le 06/11)
        var near = 0
        for (o in list) {
            if (o.cls != 5 && o.cls != 6 && o.cls != 7) continue   // unclassified, residential, living_street
            if (o === w) continue
            var close = false
            for (j in 0 until o.lat.size - 1) {
                if (segment(loc, o.lat[j] / E5, o.lon[j] / E5, o.lat[j + 1] / E5, o.lon[j + 1] / E5).first <= 150.0) { close = true; break }
            }
            if (close) { near += if (o.cls == 5) 1 else 2; if (near >= 4) return 50 }
        }
        return 80
    }
    @Volatile private var estimateOn = true
    @Volatile private var lastWay: Way? = null
    @Volatile private var prevBearing = -1f
    /** Fork FR : la limitation affichée est estimée (route sans limitation dans OpenStreetMap). */
    @Volatile var estimated = false

    private fun limitOf(tags: JSONObject): Int? =
        // En France, beaucoup de rues n'ont pas « maxspeed » mais un type de zone (FR:urban = 50, FR:zone30…)
        parse(tags.optString("maxspeed")) ?: parse(tags.optString("maxspeed:forward"))
            ?: parse(tags.optString("maxspeed:type")) ?: parse(tags.optString("source:maxspeed"))
            ?: parse(tags.optString("zone:maxspeed")) ?: parse(tags.optString("zone:traffic"))
            // Sans étiquette : rue résidentielle = agglomération en France, 50 (relevé le 2026-10-21,
            // « Rue des Blés Dorés » sans maxspeed alors que la caméra de la voiture lisait 50)
            ?: when (tags.optString("highway")) { "motorway" -> 130; "living_street" -> 20; "residential" -> 50; else -> null }

    /** Distance (m) du point au segment et cap du segment (°), en projection locale. */
    private fun segment(p: Location, lat1: Double, lon1: Double, lat2: Double, lon2: Double): Pair<Double, Double> {
        val k = cos(Math.toRadians(p.latitude)) * 111_320.0
        val ax = (lon1 - p.longitude) * k; val ay = (lat1 - p.latitude) * 110_540.0
        val bx = (lon2 - p.longitude) * k; val by = (lat2 - p.latitude) * 110_540.0
        val dx = bx - ax; val dy = by - ay
        val len2 = dx * dx + dy * dy
        val t = if (len2 == 0.0) 0.0 else (-(ax * dx + ay * dy) / len2).coerceIn(0.0, 1.0)
        val cx = ax + t * dx; val cy = ay + t * dy
        val brg = (Math.toDegrees(Math.atan2(dx, dy)) + 360) % 360
        return Math.sqrt(cx * cx + cy * cy) to brg
    }

    private fun parse(v: String?): Int? {
        if (v.isNullOrBlank()) return null
        val s = v.trim().split(';').first().trim()
        return when {
            s.equals("FR:urban", true) -> 50
            s.equals("FR:rural", true) -> 80
            s.equals("FR:motorway", true) -> 130
            s.equals("FR:zone30", true) || s.equals("FR:30", true) -> 30
            s.endsWith("mph") -> s.removeSuffix("mph").trim().toIntOrNull()?.let { Math.round(it * 1.609f) }
            else -> s.toIntOrNull()
        }
    }
}
