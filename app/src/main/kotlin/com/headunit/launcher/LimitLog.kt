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
