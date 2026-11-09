package com.headunit.launcher

import android.content.Context
import java.net.HttpURLConnection
import java.net.URL

/**
 * Fork FR : les radars fixes de France, d'un fichier publié chaque semaine sur GitHub (OpenStreetMap + liste officielle du
 * ministère de l'Intérieur). Téléchargé une fois par semaine, il sert hors ligne et ne dépend plus des serveurs Overpass,
 * souvent saturés.
 */
object RadarsFr {
    private const val URL_FILE = "https://github.com/ASSE03/Horizon-Maps/releases/download/radars/radars_fr.json"
    // (relu chaque jour : un radar retiré disparaît vite)
    private const val WEEK = 86_400_000L

    @Volatile private var pts: FloatArray? = null       // lat, lon, direction (-1 : inconnue), vitesse contrôlée (0 : inconnue) à la suite
    /** La vitesse contrôlée (km/h) des radars trouvés, d'après la liste officielle. */
    val vma = java.util.concurrent.ConcurrentHashMap<Long, Int>()
    @Volatile var status = "pas encore chargé"
    @Volatile private var tryAt = 0L

    private fun file(ctx: Context) = java.io.File(ctx.filesDir, "radars_fr.json")

    /** Charge le fichier (et le télécharge s'il manque ou a plus d'une semaine). À appeler hors du fil principal. */
    fun ensure(ctx: Context) {
        val f = file(ctx)
        val now = System.currentTimeMillis()
        val stale = !f.exists() || now - f.lastModified() > WEEK
        if (stale && now - tryAt > 30 * 60_000L) {
            tryAt = now
            try {
                val c = URL(URL_FILE).also { com.headunit.launcher.Net.check() }.openConnection() as HttpURLConnection
                c.connectTimeout = 8000; c.readTimeout = 20000; c.instanceFollowRedirects = true
                c.setRequestProperty("User-Agent", "HorizonNav/1.0 (Android navigation app)")
                if (c.responseCode in 200..299) {
                    val tmp = java.io.File(f.path + ".tmp")
                    c.inputStream.use { i -> tmp.outputStream().use { o -> i.copyTo(o) } }
                    if (parse(tmp.readText()) != null) { tmp.renameTo(f); pts = null } else tmp.delete()
                } else status = "téléchargement : HTTP ${c.responseCode}"
            } catch (e: Exception) { status = "téléchargement : ${e.javaClass.simpleName}" }
        }
        if (pts == null && f.exists()) {
            val a = try { parse(f.readText()) } catch (e: Exception) { null }
            if (a != null) {
                pts = a
                val date = try { org.json.JSONObject(f.readText()).optString("date") } catch (e: Exception) { "" }
                status = "${a.size / 4} radars (fichier du $date)"
            }
        }
    }

    private fun parse(body: String): FloatArray? {
        val arr = org.json.JSONObject(body).optJSONArray("r") ?: return null
        if (arr.length() < 1000) return null
        val out = FloatArray(arr.length() * 4)
        for (i in 0 until arr.length()) {
            val p = arr.getJSONArray(i)
            out[i * 4] = p.getDouble(0).toFloat(); out[i * 4 + 1] = p.getDouble(1).toFloat(); out[i * 4 + 2] = p.optDouble(2, -1.0).toFloat()
            out[i * 4 + 3] = p.optDouble(3, 0.0).toFloat()
        }
        return out
    }

    /** Radars à moins de [radius] m de [p] ; null si le fichier n'est pas (encore) là. */
    fun cameras(ctx: Context, p: LL, radius: Double): List<LL>? {
        ensure(ctx)
        val a = pts ?: return null
        val dLat = radius / 111_320.0
        val dLon = radius / (111_320.0 * Math.cos(Math.toRadians(p.lat)).coerceAtLeast(0.2))
        val out = ArrayList<LL>()
        var i = 0
        while (i < a.size) {
            val la = a[i].toDouble(); val lo = a[i + 1].toDouble()
            if (Math.abs(la - p.lat) <= dLat && Math.abs(lo - p.lon) <= dLon) {
                val c = LL(la, lo)
                if (Geo.dist(c, p) <= radius) {
                    out += c
                    val d = a[i + 2]
                    if (d >= 0f) Overpass.camDir[Overpass.camKey(c)] = d.toDouble()
                    val v = a[i + 3].toInt()
                    if (v > 0) vma[Overpass.camKey(c)] = v
                }
            }
            i += 4
        }
        return out
    }
}
