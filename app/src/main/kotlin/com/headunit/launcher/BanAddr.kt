package com.headunit.launcher

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Fork FR : la Base Adresse Nationale (toutes les adresses de France, avec les numéros), via le géocodeur public de l'IGN.
 * TomTom et Mapbox ne connaissent pas toutes les adresses des petites communes (« 4 place de la Bascule, Saint-Cyr ») : celle-ci
 * oui. Gratuit, sans clé.
 */
object BanAddr {
    @Volatile var lastNote = ""

    private val ENDPOINTS = listOf("https://data.geopf.fr/geocodage/search", "https://api-adresse.data.gouv.fr/search/")

    fun search(q: String, near: LL?): List<Place> {
        val enc = URLEncoder.encode(q.trim(), "UTF-8")
        val bias = near?.let { String.format(java.util.Locale.US, "&lat=%.5f&lon=%.5f", it.lat, it.lon) } ?: ""
        for (ep in ENDPOINTS) {
            try {
                val c = URL("$ep?q=$enc&limit=6$bias").also { com.headunit.launcher.Net.check() }.openConnection() as HttpURLConnection
                c.connectTimeout = 5000; c.readTimeout = 7000
                c.setRequestProperty("User-Agent", "HorizonNav/1.0 (Android navigation app)")
                if (c.responseCode !in 200..299) { lastNote = "${ep.substringAfter("//").substringBefore('/')}: HTTP ${c.responseCode}"; continue }
                val feats = org.json.JSONObject(c.inputStream.bufferedReader().readText()).optJSONArray("features") ?: continue
                val out = ArrayList<Place>()
                for (i in 0 until feats.length()) {
                    val f = feats.getJSONObject(i)
                    val pr = f.optJSONObject("properties") ?: continue
                    // (les résultats peu sûrs sont écartés : sinon une rue d'une autre ville passe avant)
                    if (pr.optDouble("score", 0.0) < 0.45) continue
                    val co = f.optJSONObject("geometry")?.optJSONArray("coordinates") ?: continue
                    val name = pr.optString("name").ifBlank { pr.optString("label") }
                    val sub = listOf(pr.optString("postcode"), pr.optString("city")).filter { it.isNotBlank() }.joinToString(" ")
                    out += Place(name, sub, LL(co.getDouble(1), co.getDouble(0)))
                }
                lastNote = "${ep.substringAfter("//").substringBefore('/')} ok (${out.size})"
                return out
            } catch (e: Exception) { lastNote = "${ep.substringAfter("//").substringBefore('/')}: ${e.javaClass.simpleName}" }
        }
        return emptyList()
    }
}
