package com.headunit.launcher

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Fork FR : niveau de batterie réel de la voiture lu dans Home Assistant (par exemple l'état de charge de la MG4 publié par
 * saic-python-mqtt-gateway). Réglages > Carburant et recharge > Votre voiture électrique : adresse de Home Assistant (Nabu Casa
 * ou autre adresse joignable depuis la voiture), jeton longue durée et entité du % de batterie. Le jeton n'est jamais sauvegardé.
 */
object HaBattery {
    const val URL_KEY = "ha_url"
    const val TOKEN_KEY = "ha_token"
    const val ENTITY_KEY = "ha_soc_entity"
    const val CONS_KEY = "ha_cons_entity"

    @Volatile var status = ""; private set
    @Volatile var lastPct: Int? = null; private set
    @Volatile var lastAt = 0L; private set

    fun configured(p: Prefs) = p.s(URL_KEY, "").isNotBlank() && p.s(TOKEN_KEY, "").isNotBlank() && p.s(ENTITY_KEY, "").isNotBlank()

    /** Valeur numérique d'une entité, ou null (réseau : hors du fil principal). */
    private fun number(base: String, token: String, entity: String): Double? = try {
        val c = URL("$base/api/states/$entity").openConnection() as HttpURLConnection
        try {
            c.connectTimeout = 8_000; c.readTimeout = 10_000
            c.setRequestProperty("Authorization", "Bearer $token")
            if (c.responseCode == 200) JSONObject(c.inputStream.bufferedReader().use { it.readText() }).optString("state").replace(',', '.').toDoubleOrNull() else null
        } finally { c.disconnect() }
    } catch (e: Exception) { null }

    /**
     * Consommation réelle de la voiture (entité facultative), rangée dans le réglage de consommation (stocké en miles par kWh).
     * L'unité est devinée à la valeur : au-dessus de 12 ce sont des kWh/100 km (ex. 16,5), en dessous des km/kWh (ex. 6,1).
     * Renvoie les km/kWh retenus, ou null.
     */
    fun syncConsumption(ctx: Context): Double? {
        val p = Prefs(ctx)
        if (!configured(p)) return null
        val base = p.s(URL_KEY, "").trim().trimEnd('/'); val token = p.s(TOKEN_KEY, "").trim()
        val entity = p.s(CONS_KEY, "").trim()
        // saic-python-mqtt-gateway (MG) : pas de capteur de consommation, mais l'autonomie et l'énergie restante donnent les km/kWh
        // que la voiture utilise elle-même (sensor.<vin>_range / sensor.<vin>_soc_kwh), et la capacité de la batterie
        val soc = p.s(ENTITY_KEY, "").trim()
        val prefix = if (soc.endsWith("_soc")) soc.removeSuffix("_soc") else null
        if (prefix != null) number(base, token, "${prefix}_total_battery_capacity")?.takeIf { it in 10.0..200.0 }?.let { cap ->
            p.put(PreferenceKeys.EV_BATTERY_KWH, String.format(java.util.Locale.US, "%.1f", cap))
        }
        val v: Double = if (entity.isNotEmpty()) number(base, token, entity) ?: return null
        else {
            if (prefix == null) return null
            val range = number(base, token, "${prefix}_range") ?: return null
            val kwh = number(base, token, "${prefix}_soc_kwh") ?: return null
            if (kwh < 3.0 || range <= 0) return null
            range / kwh
        }
        if (v <= 0) return null
        val kmPerKwh = if (v > 12) 100.0 / v else v
        if (kmPerKwh !in 2.0..12.0) return null
        p.put(PreferenceKeys.EVMPK, String.format(java.util.Locale.US, "%.3f", kmPerKwh / 1.609344))
        return kmPerKwh
    }

    /** Réseau : hors du fil principal. Le % de batterie (0..100), ou null avec [status] qui dit pourquoi. */
    fun fetch(ctx: Context): Int? {
        val p = Prefs(ctx)
        if (!configured(p)) { status = "non configuré"; return null }
        val base = p.s(URL_KEY, "").trim().trimEnd('/')
        val entity = p.s(ENTITY_KEY, "").trim()
        return try {
            val c = URL("$base/api/states/$entity").openConnection() as HttpURLConnection
            try {
                c.connectTimeout = 8_000; c.readTimeout = 10_000
                c.setRequestProperty("Authorization", "Bearer ${p.s(TOKEN_KEY, "").trim()}")
                c.setRequestProperty("Content-Type", "application/json")
                when (val code = c.responseCode) {
                    200 -> {
                        val st = JSONObject(c.inputStream.bufferedReader().use { it.readText() }).optString("state")
                        val v = st.replace(',', '.').toDoubleOrNull()
                        if (v == null) { status = "état « $st » illisible (voiture endormie ?)"; null }
                        else {
                            val pct = Math.round(v).toInt().coerceIn(0, 100)
                            lastPct = pct; lastAt = System.currentTimeMillis()
                            status = "$pct % lu à ${Clock.hm(lastAt)}"
                            pct
                        }
                    }
                    401 -> { status = "jeton Home Assistant refusé"; null }
                    404 -> { status = "entité $entity introuvable"; null }
                    else -> { status = "Home Assistant a répondu HTTP $code"; null }
                }
            } finally { c.disconnect() }
        } catch (e: Exception) {
            status = "Home Assistant injoignable (${e.javaClass.simpleName})"
            null
        }
    }
}
