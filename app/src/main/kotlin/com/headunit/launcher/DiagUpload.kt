package com.headunit.launcher

import android.content.Context
import android.util.Base64
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Fork FR : envoi des rapports (avis, plantages, blocages) dans le dépôt GitHub du fork, branche « diagnostics »
 * (dossier diagnostics/), comme MG4Dash SWI68. La branche n'est pas « main » : un envoi ne relance aucune compilation.
 * Il faut un jeton GitHub « fine-grained » limité au dépôt, avec « Contents : Read and write »
 * (Réglages > À propos et sauvegarde > Jeton GitHub). Le jeton n'est jamais mis dans les sauvegardes.
 */
object DiagUpload {
    const val TOKEN_KEY = "ghtoken"
    private const val BRANCH = "diagnostics"
    private const val SENT = "diag_sent"
    private val api get() = "https://api.github.com/repos/${BuildConfig.UPDATE_REPO}/contents/diagnostics/"
    @Volatile var status = ""; private set

    fun stamp(): String = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
    fun hasToken(ctx: Context) = Prefs(ctx).s(TOKEN_KEY, "").isNotBlank()

    /** Réseau : hors du fil principal. Renvoie le chemin déposé, ou lève une exception au message lisible. */
    fun send(ctx: Context, name: String, text: String): String {
        val token = Prefs(ctx).s(TOKEN_KEY, "").trim()
        if (token.isEmpty()) throw IllegalStateException("pas de jeton GitHub (Réglages > À propos et sauvegarde)")
        val c = URL(api + name).openConnection() as HttpURLConnection
        try {
            c.requestMethod = "PUT"; c.doOutput = true
            c.connectTimeout = 10_000; c.readTimeout = 30_000
            c.setRequestProperty("Accept", "application/vnd.github+json")
            c.setRequestProperty("Authorization", "Bearer $token")
            c.setRequestProperty("User-Agent", "HorizonNav-FR")
            c.setRequestProperty("Content-Type", "application/json")
            val body = JSONObject()
                .put("message", "Rapport ${name.removeSuffix(".txt")}")
                .put("branch", BRANCH)
                .put("content", Base64.encodeToString(text.toByteArray(Charsets.UTF_8), Base64.NO_WRAP))
            c.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            return when (val code = c.responseCode) {
                200, 201 -> "diagnostics/$name"
                401 -> throw IllegalStateException("jeton GitHub refusé (expiré ?)")
                403, 404 -> throw IllegalStateException("le jeton n'a pas le droit d'écriture (« Contents : Read and write ») — HTTP $code")
                422 -> throw IllegalStateException("la branche « diagnostics » est absente du dépôt — HTTP 422")
                else -> throw IllegalStateException("HTTP $code")
            }
        } finally { c.disconnect() }
    }

    /**
     * Au lancement : envoie le dernier plantage et le dernier blocage s'ils n'ont pas encore été envoyés (repérés par la date
     * de leur fichier). Silencieux sans jeton ou sans réseau : ce sera retenté au lancement suivant.
     */
    fun uploadPending(ctx: Context) {
        if (!hasToken(ctx)) { status = "pas de jeton GitHub"; return }
        val sp = ctx.getSharedPreferences(SENT, Context.MODE_PRIVATE)
        var sent = 0
        try {
            val files: List<Pair<String, File>> = listOf("plantage" to File(ctx.filesDir, "lastcrash.txt"), "blocage" to FreezeWatch.file(ctx))
            for (entry in files) {
                val kind = entry.first
                val f = entry.second
                if (!f.exists()) continue
                val mark = f.lastModified()
                if (sp.getLong(kind, 0L) == mark) continue
                val head = "${kind.replaceFirstChar { it.uppercase() }} d'Horizon Nav FR ${BuildConfig.VERSION_NAME} · " +
                    "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}, Android ${android.os.Build.VERSION.RELEASE} · fichier du ${Date(mark)}\n\n"
                send(ctx, "$kind-${stamp()}.txt", head + f.readText().take(20_000))
                sp.edit().putLong(kind, mark).apply()
                sent++
            }
            status = if (sent > 0) "$sent rapport(s) envoyé(s)" else "aucun plantage en attente"
        } catch (e: Exception) {
            status = "envoi impossible : ${e.message}"
            android.util.Log.w("DiagUpload", status)
        }
    }
}
