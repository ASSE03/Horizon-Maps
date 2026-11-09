package com.headunit.launcher

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Fork FR : les prévisions Bison Futé (vert, orange, rouge, noir) pour le jour du départ, dans les départements que traverse
 * l'itinéraire, dans le sens des départs et dans celui des retours. Données : le fichier du site Bison Futé
 * (previsions.json : pour chaque jour et chacun des 94 départements de métropole, « départ,retour »), gardé 12 h. Les
 * départements sont trouvés avec leurs contours simplifiés (assets/depts_fr.txt, d'après france-geojson).
 */
object BisonFute {
    private const val URL_JSON = "https://www.bison-fute.gouv.fr/previsions/previsions.json"
    private class Dept(val code: String, val name: String, val lat: DoubleArray, val lon: DoubleArray,
                       val minLat: Double, val maxLat: Double, val minLon: Double, val maxLon: Double)
    @Volatile private var depts: List<Dept>? = null
    @Volatile var status = ""

    private fun loadDepts(ctx: Context): List<Dept> {
        depts?.let { return it }
        val out = ArrayList<Dept>()
        ctx.assets.open("depts_fr.txt").bufferedReader().useLines { lines ->
            for (l in lines) {
                val parts = l.split('|'); if (parts.size < 3) continue
                val pts = parts[2].split(';').mapNotNull { q -> q.split(',').takeIf { it.size == 2 }?.let { it[0].toDoubleOrNull() to it[1].toDoubleOrNull() } }
                    .filter { it.first != null && it.second != null }
                if (pts.size < 3) continue
                val la = DoubleArray(pts.size) { pts[it].first!! }; val lo = DoubleArray(pts.size) { pts[it].second!! }
                out += Dept(parts[0], parts[1], la, lo, la.min(), la.max(), lo.min(), lo.max())
            }
        }
        depts = out
        return out
    }

    private fun inside(d: Dept, p: LL): Boolean {
        if (p.lat < d.minLat || p.lat > d.maxLat || p.lon < d.minLon || p.lon > d.maxLon) return false
        var c = false; var j = d.lat.size - 1
        for (i in d.lat.indices) {
            if ((d.lat[i] > p.lat) != (d.lat[j] > p.lat) && p.lon < (d.lon[j] - d.lon[i]) * (p.lat - d.lat[i]) / (d.lat[j] - d.lat[i]) + d.lon[i]) c = !c
            j = i
        }
        return c
    }

    private fun data(ctx: Context): JSONObject? {
        val f = File(ctx.filesDir, "bisonfute.json")
        if (f.exists() && System.currentTimeMillis() - f.lastModified() < 12 * 3_600_000L) try { return JSONObject(f.readText()) } catch (e: Exception) { }
        return try {
            val c = URL(URL_JSON).also { com.headunit.launcher.Net.check() }.openConnection() as HttpURLConnection
            val body = try {
                c.connectTimeout = 8000; c.readTimeout = 15000
                c.setRequestProperty("User-Agent", "HorizonNavFR")
                if (c.responseCode != 200) throw IllegalStateException("HTTP ${c.responseCode}")
                c.inputStream.bufferedReader().use { it.readText() }
            } finally { c.disconnect() }
            val j = JSONObject(body)
            f.writeText(body)
            j
        } catch (e: Exception) {
            status = "Bison Futé : ${e.javaClass.simpleName} ${e.message?.take(60)}"
            if (f.exists()) try { JSONObject(f.readText()) } catch (x: Exception) { null } else null
        }
    }

    private fun rank(c: String) = when (c.trim().uppercase()) { "O" -> 1; "R" -> 2; "N" -> 3; else -> 0 }
    private fun word(r: Int) = when (r) { 1 -> "orange"; 2 -> "rouge"; 3 -> "noir"; else -> "vert" }

    /**
     * Le message pour un itinéraire [pts] parti le jour [atMs], ou null quand tout est vert (ou hors de France, ou sans données).
     * Réseau et calcul : hors du fil principal.
     */
    fun forRoute(ctx: Context, pts: List<LL>, cum: DoubleArray, atMs: Long): String? {
        val ds = loadDepts(ctx)
        val j = data(ctx) ?: return null
        val day = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.FRANCE).format(java.util.Date(atMs))
        val days = j.optJSONArray("days") ?: return null
        val idx = (0 until days.length()).firstOrNull { days.optString(it) == day } ?: run { status = "Bison Futé : pas de prévision pour le $day"; return null }
        val line = j.optJSONArray("deptsLine") ?: return null
        val vals = j.optJSONArray("values")?.optJSONArray(idx) ?: return null
        val col = HashMap<String, Pair<Int, Int>>()
        for (i in 0 until minOf(line.length(), vals.length())) {
            val v = vals.optString(i).split(',')
            col[line.optString(i).trimStart('0')] = rank(v.getOrElse(0) { "V" }) to rank(v.getOrElse(1) { "V" })
        }
        // les départements traversés, dans l'ordre, un point tous les 3 km environ
        val seen = LinkedHashMap<String, String>()
        var next = 0.0
        var last: Dept? = null
        for (i in pts.indices) {
            if (i < cum.size && cum[i] < next && i != pts.size - 1) continue
            next = (if (i < cum.size) cum[i] else next) + 3000.0
            val p = pts[i]
            val d = last?.takeIf { inside(it, p) } ?: ds.firstOrNull { inside(it, p) } ?: continue
            last = d
            if (d.code !in seen) seen[d.code] = d.name
        }
        if (seen.isEmpty()) return null
        var dep = 0; var ret = 0
        val depNames = ArrayList<String>(); val retNames = ArrayList<String>()
        for ((code, name) in seen) {
            val (a, b) = col[code] ?: continue
            if (a > dep) { dep = a; depNames.clear() }; if (a == dep && a > 0) depNames += name
            if (b > ret) { ret = b; retNames.clear() }; if (b == ret && b > 0) retNames += name
        }
        status = "Bison Futé $day : ${seen.size} départements, départs ${word(dep)}, retours ${word(ret)}"
        if (dep == 0 && ret == 0) return null
        fun part(r: Int, sens: String, names: List<String>) = "${word(r)} dans le sens des $sens" + (if (names.size <= 3) " (" + names.joinToString(", ") + ")" else " (${names.take(2).joinToString(", ")}…)")
        val parts = listOfNotNull(if (dep > 0) part(dep, "départs", depNames) else null, if (ret > 0) part(ret, "retours", retNames) else null)
        val quand = if (day == java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.FRANCE).format(java.util.Date())) "aujourd'hui" else "le $day"
        return "Bison Futé $quand : " + parts.joinToString(", ")
    }
}
