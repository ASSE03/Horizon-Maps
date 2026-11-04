package com.headunit.launcher

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Fork FR : le tarif d'une borne de recharge en France, tiré de la base nationale des IRVE (fichier consolidé de data.gouv.fr,
 * republié par ODRÉ avec une recherche autour d'un point). Le champ « tarification » est un texte libre saisi par l'opérateur
 * (« 0,49 €/kWh », « 0,30 €/min après 1 h »…) : il est montré tel quel, raccourci. Rien hors de France.
 */
object IrvePrice {
    private val cache = java.util.concurrent.ConcurrentHashMap<String, String>()

    /** Le tarif près de [p] (dans un rayon de 120 m), « Gratuit », ou "" si la base ne dit rien. Réseau : hors du fil principal. */
    fun near(p: LL): String {
        val key = String.format(java.util.Locale.US, "%.4f,%.4f", p.lat, p.lon)
        cache[key]?.let { return it }
        val out = try {
            val u = "https://odre.opendatasoft.com/api/records/1.0/search/?dataset=bornes-irve&rows=20" +
                "&geofilter.distance=" + URLEncoder.encode(String.format(java.util.Locale.US, "%.6f,%.6f,120", p.lat, p.lon), "UTF-8")
            val c = URL(u).openConnection() as HttpURLConnection
            val body = try {
                c.connectTimeout = 6000; c.readTimeout = 8000
                c.setRequestProperty("User-Agent", "HorizonNavFR")
                if (c.responseCode != 200) throw IllegalStateException("HTTP ${c.responseCode}")
                c.inputStream.bufferedReader().use { it.readText() }
            } finally { c.disconnect() }
            val recs = JSONObject(body).optJSONArray("records")
            val tarifs = LinkedHashMap<String, Int>()
            var free = 0; var n = 0
            if (recs != null) for (i in 0 until recs.length()) {
                val f = recs.optJSONObject(i)?.optJSONObject("fields") ?: continue
                n++
                if (f.optString("gratuit").lowercase() in setOf("true", "1", "oui", "vrai")) free++
                val t = f.optString("tarification").trim().replace(Regex("\\s+"), " ")
                if (t.isNotBlank() && t.lowercase() !in setOf("non renseigné", "nc", "n/a", "inconnu")) tarifs[t] = (tarifs[t] ?: 0) + 1
            }
            when {
                n == 0 -> ""
                tarifs.isNotEmpty() -> "Tarif : " + tarifs.maxBy { it.value }.key.let { if (it.length > 70) it.take(68) + "…" else it }
                free > 0 && free * 2 >= n -> "Gratuit"
                else -> ""
            }
        } catch (e: Exception) { return "" }
        cache[key] = out
        return out
    }
}
