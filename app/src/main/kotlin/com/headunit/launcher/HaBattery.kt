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

    @Volatile var status = ""; private set
    @Volatile var lastPct: Int? = null; private set
    @Volatile var lastAt = 0L; private set

    fun configured(p: Prefs) = p.s(URL_KEY, "").isNotBlank() && p.s(TOKEN_KEY, "").isNotBlank() && p.s(ENTITY_KEY, "").isNotBlank()

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
