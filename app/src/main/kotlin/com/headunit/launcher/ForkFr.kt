package com.headunit.launcher

import kotlin.math.abs

/*
 * Fork FR : ce qui est propre à la version française et ne dépend pas du reste du code est rangé ici, à part des fichiers de
 * l'original, pour que les fusions avec Horizon Nav ne le touchent pas (moins de conflits, moins de risque de le perdre).
 */

/**
 * Le retard du GPS, mesuré tout seul dans les virages (Réglages › En conduisant › Avance de la flèche › Auto).
 *
 * Le gyroscope du téléphone voit la voiture tourner sans délai ; le cap GPS le voit avec un retard (le point a déjà vieilli quand il
 * arrive). Dans chaque virage franc, on cherche le décalage (0 à 2,5 s) pour lequel la courbe du cap GPS colle le mieux à celle du
 * gyroscope : c'est ce retard que la flèche doit rattraper. On garde la médiane des derniers virages.
 */
object GpsLag {
    private class Fix(val t: Long, val bearing: Double)
    private val fixes = ArrayDeque<Fix>()
    private val estimates = ArrayDeque<Double>()
    private var lastTurnAt = 0L

    /** Le retard mesuré (en secondes), ou null tant qu'il n'y a pas eu assez de virages. */
    @Volatile var lag: Double? = null; private set
    /** Pour le diagnostic : nombre de virages mesurés, et le dernier retard trouvé. */
    @Volatile var turns = 0; private set
    @Volatile var lastFound = Double.NaN; private set

    fun restore(saved: Double?) { if (saved != null && saved in 0.0..2.5 && lag == null) lag = saved }

    private fun unwrap(d: Double): Double { var x = d; while (x > 180.0) x -= 360.0; while (x < -180.0) x += 360.0; return x }

    /**
     * Un point GPS avec un vrai cap (pas un cap calculé), arrivé maintenant. Renvoie le nouveau retard quand il vient de changer
     * (pour le garder dans les réglages), null sinon.
     */
    @Synchronized fun onFix(bearing: Double, speedMs: Double): Double? {
        val now = android.os.SystemClock.uptimeMillis()
        if (speedMs < 5.0 || !Gyro.alive()) { fixes.clear(); return null }
        fixes.addLast(Fix(now, bearing))
        while (fixes.isNotEmpty() && now - fixes.first().t > 9000L) fixes.removeFirst()
        if (fixes.size < 6 || now - lastTurnAt < 6000L) return null
        // un virage franc dans la fenêtre : au moins 35° de cap en tout, et le dernier point au calme (sorti du virage)
        val f = fixes.toList()
        val b0 = f.first().bearing
        val curve = DoubleArray(f.size); var acc = 0.0
        for (i in 1 until f.size) { acc += unwrap(f[i].bearing - f[i - 1].bearing); curve[i] = acc }
        val swing = curve.maxOrNull()!! - curve.minOrNull()!!
        if (swing < 35.0) return null
        if (f.size >= 3 && abs(unwrap(f[f.size - 1].bearing - f[f.size - 3].bearing)) > 8.0) return null
        // le décalage qui fait coller le gyroscope au cap GPS
        var best = Double.NaN; var bestErr = Double.MAX_VALUE
        var tau = 0.0
        while (tau <= 2.5001) {
            val ms = (tau * 1000).toLong()
            val g0 = Gyro.totalAt(f.first().t - ms)
            if (g0 != null) {
                var err = 0.0; var ok = true
                for (i in f.indices) {
                    val g = Gyro.totalAt(f[i].t - ms)
                    if (g == null) { ok = false; break }
                    val d = curve[i] - (g - g0)
                    err += d * d
                }
                if (ok) { val rms = Math.sqrt(err / f.size); if (rms < bestErr) { bestErr = rms; best = tau } }
            }
            tau += 0.1
        }
        lastTurnAt = now
        fixes.clear()
        // un virage mal suivi (gyroscope mal fixé, cap GPS bruité) : ignoré
        if (best.isNaN() || bestErr > 9.0 || b0.isNaN()) return null
        lastFound = best
        turns++
        estimates.addLast(best)
        while (estimates.size > 9) estimates.removeFirst()
        if (estimates.size < 3) return null
        val m = estimates.sorted()[estimates.size / 2]
        val old = lag
        lag = m
        return if (old == null || abs(old - m) >= 0.05) m else null
    }
}

/**
 * La météo prévue à destination pour l'heure d'arrivée (Open-Meteo, prévisions horaires), montrée dans la bulle de température
 * pendant le guidage. Null si l'heure d'arrivée est hors des prévisions ou si le service ne répond pas.
 */
object ArrivalWx {
    fun at(p: LL, whenMs: Long): Weather.Now? = try {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=${p.lat}&longitude=${p.lon}" +
            "&hourly=temperature_2m,weather_code,precipitation_probability&timeformat=unixtime&forecast_days=3"
        val c = java.net.URL(url).also { Net.check() }.openConnection() as java.net.HttpURLConnection
        c.connectTimeout = 10000; c.readTimeout = 10000
        val h = org.json.JSONObject(c.inputStream.bufferedReader().readText()).getJSONObject("hourly")
        val times = h.getJSONArray("time"); val temps = h.getJSONArray("temperature_2m")
        val codes = h.getJSONArray("weather_code"); val pops = h.optJSONArray("precipitation_probability")
        val target = whenMs / 1000L
        var best = -1; var bestGap = Long.MAX_VALUE
        for (i in 0 until times.length()) {
            val g = abs(times.getLong(i) - target)
            if (g < bestGap) { bestGap = g; best = i }
        }
        if (best < 0 || bestGap > 5400L) null else {
            var code = codes.getInt(best)
            // (pluie probable à 60 % ou plus alors que le code dit seulement nuageux : montrée comme pluie)
            val pop = pops?.optInt(best, 0) ?: 0
            if (code <= 3 && pop >= 60) code = 61
            Weather.Now(temps.getDouble(best), code)
        }
    } catch (e: Exception) { null }
}

/**
 * La consommation d'une voiture électrique selon le temps et la vitesse : le froid (chauffage, batterie froide : +1,3 % par degré
 * sous 15 °C, jusqu'à -20 °C), la clim (+1 % par degré au-dessus de 28 °C), et l'air qui freine au-dessus de 80 km/h de moyenne
 * (+0,8 % par km/h, jusqu'à 135) ; en ville (moins de 45 km/h de moyenne) un peu moins (-5 %). Un multiplicateur de la
 * consommation réglée, entre 0,9 et 1,8.
 */
object EvCons {
    fun factor(tempC: Double?, avgKmh: Double?): Double {
        val ft = when {
            tempC == null -> 1.0
            tempC < 15.0 -> 1.0 + (15.0 - tempC.coerceAtLeast(-20.0)) * 0.013
            tempC > 28.0 -> 1.0 + (tempC.coerceAtMost(42.0) - 28.0) * 0.01
            else -> 1.0
        }
        val fv = when {
            avgKmh == null -> 1.0
            avgKmh > 80.0 -> 1.0 + (avgKmh.coerceAtMost(135.0) - 80.0) * 0.008
            avgKmh < 45.0 -> 0.95
            else -> 1.0
        }
        return (ft * fv).coerceIn(0.9, 1.8)
    }
}
