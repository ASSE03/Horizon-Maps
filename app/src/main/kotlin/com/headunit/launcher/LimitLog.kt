package com.headunit.launcher

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Fork FR : journal des derniers changements de limitation (source, valeur, position arrondie), joint aux rapports
 * « Avis ou problème » pour comprendre une limitation fausse.
 */
object LimitLog {
    private const val MAX = 80
    private val lines = ArrayList<String>()
    private var loaded = false
    private fun file(ctx: Context) = File(ctx.filesDir, "limits.log")

    @Synchronized fun add(ctx: Context, line: String) {
        try {
            if (!loaded) { loaded = true; file(ctx).takeIf { it.exists() }?.readLines()?.let { lines.addAll(it.takeLast(MAX)) } }
            lines += SimpleDateFormat("dd/MM HH:mm:ss", Locale.FRANCE).format(Date()) + " " + line
            while (lines.size > MAX) lines.removeAt(0)
            file(ctx).writeText(lines.joinToString("\n"))
        } catch (e: Exception) { }
    }

    @Synchronized fun read(ctx: Context): String = try {
        if (!loaded) { loaded = true; file(ctx).takeIf { it.exists() }?.readLines()?.let { lines.addAll(it.takeLast(MAX)) } }
        lines.takeLast(40).joinToString("\n")
    } catch (e: Exception) { "" }

    fun pos(p: LL?) = if (p == null) "?" else String.format(Locale.US, "%.5f,%.5f", p.lat, p.lon)
}

/** Fork FR : les dernières erreurs de chargement de la carte Mapbox (sans le jeton), jointes aux rapports. */
object MapErrors {
    private val lines = ArrayList<String>()
    private val counts = LinkedHashMap<String, Int>()
    @Volatile var probe = ""
    @Volatile private var probing = false
    @Synchronized fun add(type: String, source: String?, msg: String?) {
        val m = (msg ?: "").replace(Regex("access_token=[^&\\s]+"), "access_token=…").take(220)
        val line = "$type · ${source ?: "-"} · $m"
        counts[line] = (counts[line] ?: 0) + 1
        if (counts.size > 40) counts.remove(counts.keys.first())
        if (lines.lastOrNull()?.endsWith(line) == true) return
        lines += SimpleDateFormat("HH:mm:ss", Locale.FRANCE).format(Date()) + " " + line
        while (lines.size > 25) lines.removeAt(0)
    }
    /** Fork FR : nombre d'erreurs de carte depuis l'ouverture. */
    @Synchronized fun total(): Int = counts.values.sum()
    @Synchronized fun read(): String = lines.joinToString("\n") +
        (if (counts.isEmpty()) "" else "\nTotal par erreur depuis l'ouverture :\n" + counts.entries.joinToString("\n") { "${it.value} × ${it.key}" })

    /**
     * Fork FR : interroge directement Mapbox avec la clé (photos satellite, carte), pour lire sa réponse exacte (code et message)
     * quand il refuse. Résultat dans [probe], pour les rapports. Hors du fil principal.
     */
    fun runProbe(token: String) {
        if (probing || token.isBlank()) return
        probing = true
        Thread {
            val out = StringBuilder()
            val tests = listOf(
                "satellite" to "https://api.mapbox.com/v4/mapbox.satellite/14/8402/5859@2x.jpg90",
                "carte" to "https://api.mapbox.com/v4/mapbox.mapbox-streets-v8/14/8402/5859.mvt",
                "style satellite" to "https://api.mapbox.com/styles/v1/mapbox/standard-satellite")
            for ((name, u) in tests) {
                try {
                    val c = java.net.URL("$u?access_token=" + java.net.URLEncoder.encode(token, "UTF-8")).openConnection() as java.net.HttpURLConnection
                    c.connectTimeout = 8000; c.readTimeout = 10000
                    c.setRequestProperty("User-Agent", "HorizonNavFR")
                    val code = c.responseCode
                    val body = try { (if (code >= 400) c.errorStream else null)?.bufferedReader()?.readText()?.take(160)?.replace("\n", " ") ?: "" } catch (e: Exception) { "" }
                    val lim = listOf("x-rate-limit-limit", "x-rate-limit-remaining", "retry-after").mapNotNull { h -> c.getHeaderField(h)?.let { "$h=$it" } }.joinToString(" ")
                    out.append("$name : HTTP $code ${body.ifBlank { "" }} $lim".trim()).append(" · ")
                    c.disconnect()
                } catch (e: Exception) { out.append("$name : ${e.javaClass.simpleName} ${e.message?.take(60)} · ") }
            }
            probe = SimpleDateFormat("HH:mm:ss", Locale.FRANCE).format(Date()) + " " + out.toString().removeSuffix(" · ")
            probing = false
        }.start()
    }
}

/** Fork FR : journal des mises à jour (téléchargement, lancement de l'installation, résultat), joint aux rapports. */
object UpdLog {
    private fun file(ctx: Context) = File(ctx.filesDir, "update.log")
    @Synchronized fun add(ctx: Context, line: String) {
        try {
            val f = file(ctx)
            val old = if (f.exists()) f.readLines().takeLast(30) else emptyList()
            f.writeText((old + (SimpleDateFormat("dd/MM HH:mm:ss", Locale.FRANCE).format(Date()) + " " + line)).joinToString("\n"))
        } catch (e: Exception) { }
    }
    fun read(ctx: Context): String = try { file(ctx).takeIf { it.exists() }?.readLines()?.takeLast(15)?.joinToString("\n") ?: "" } catch (e: Exception) { "" }
}
