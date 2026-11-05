package com.headunit.launcher

import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

/**
 * Fork FR : toilettes publiques via Nominatim (la recherche d'OpenStreetMap). Mêmes données qu'Overpass,
 * mais un autre serveur, rapide et bien plus disponible quand les serveurs Overpass sont saturés.
 */
object NominatimPoi {
    @Volatile var lastNote = ""

    /** Toilettes dans un carré de [radius] mètres autour de [c], triées par distance. */
    fun toilets(c: LL, radius: Int): List<Nearby> {
        val dLat = radius / 111_320.0
        val dLon = radius / (111_320.0 * Math.cos(Math.toRadians(c.lat)).coerceAtLeast(0.2))
        val url = String.format(java.util.Locale.US,
            "https://nominatim.openstreetmap.org/search?amenity=toilets&viewbox=%.5f,%.5f,%.5f,%.5f&bounded=1&format=jsonv2&limit=40&extratags=1&accept-language=fr",
            c.lon - dLon, c.lat + dLat, c.lon + dLon, c.lat - dLat)
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 6000; conn.readTimeout = 9000
        conn.setRequestProperty("User-Agent", "HorizonNav/1.0 (Android navigation app)")
        val code = conn.responseCode
        if (code !in 200..299) { lastNote = "nominatim: HTTP $code"; throw java.io.IOException("nominatim HTTP $code") }
        val arr = JSONArray(conn.inputStream.bufferedReader().readText())
        val out = ArrayList<Nearby>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val pos = LL(o.optString("lat").toDoubleOrNull() ?: continue, o.optString("lon").toDoubleOrNull() ?: continue)
            val d = Geo.dist(c, pos)
            if (d > radius * 1.05) continue
            val tags = o.optJSONObject("extratags")
            val access = tags?.optString("access").orEmpty()
            if (access == "private" || access == "no") continue
            val extra = ArrayList<String>()
            when (tags?.optString("fee")) { "yes" -> extra += "Payantes"; "no" -> extra += "Gratuites" }
            if (tags?.optString("wheelchair") == "yes") extra += "Accessibles PMR"
            if (access == "customers") extra += "Réservées aux clients"
            tags?.optString("opening_hours")?.takeIf { it.isNotBlank() && it.length <= 22 }?.let { extra += it }
            // "Rue Gaston Duclos, Annonay, Tournon-sur-Rhône, …" : la rue et la commune suffisent
            val parts = o.optString("display_name").split(", ").filter { it.isNotBlank() }
            val name = o.optString("name").ifBlank { "Toilettes" }
            val sub = parts.filter { it != name }.take(2).joinToString(", ")
            out += Nearby(Place(name, sub, pos), d, extra.joinToString(" · "))
        }
        lastNote = "nominatim ok (${out.size})"
        return out.sortedBy { it.dist }
    }

    /** Toilettes le long d'une ligne (l'itinéraire devant) : un carré de [radius] m tous les 8 km environ, 5 au plus. */
    fun toiletsAlong(line: List<LL>, radius: Int): List<Nearby> {
        val pts = ArrayList<LL>(); var last: LL? = null
        for (q in line) if (last == null || Geo.dist(last, q) > 8000.0) { pts += q; last = q }
        val seen = HashSet<String>(); val out = ArrayList<Nearby>()
        for (q in pts.take(5)) {
            // (Nominatim demande une requête par seconde au plus)
            if (q !== pts.first()) Thread.sleep(1100)
            for (n in try { toilets(q, radius) } catch (e: Exception) { emptyList() })
                if (seen.add(String.format(java.util.Locale.US, "%.5f,%.5f", n.place.pos.lat, n.place.pos.lon))) out += n
        }
        return out
    }
}
