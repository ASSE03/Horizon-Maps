package com.headunit.launcher

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * Fork FR : la vitesse de rotation de la voiture, lue sur le gyroscope du téléphone (sans délai, contrairement au cap GPS).
 * La rotation autour de la verticale (projetée sur la gravité) ne dépend pas de la façon dont le téléphone est tenu ou fixé.
 */
object Gyro : SensorEventListener {
    private var sm: SensorManager? = null
    private val g = floatArrayOf(0f, 0f, 9.81f)
    private var gAt = 0L
    private var lastNs = 0L
    /** Degrés par seconde, positif vers la droite (lissé). */
    @Volatile var rate = 0.0; private set
    /** Angle total tourné depuis le démarrage, en degrés (positif vers la droite). */
    @Volatile var total = 0.0; private set
    @Volatile var at = 0L; private set

    fun start(ctx: Context) {
        if (sm != null) return
        val m = ctx.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return
        val gy = m.getDefaultSensor(Sensor.TYPE_GYROSCOPE) ?: return
        sm = m
        try {
            m.registerListener(this, gy, SensorManager.SENSOR_DELAY_GAME)
            (m.getDefaultSensor(Sensor.TYPE_GRAVITY) ?: m.getDefaultSensor(Sensor.TYPE_ACCELEROMETER))?.let { m.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        } catch (_: Exception) { sm = null }
    }

    fun stop() { try { sm?.unregisterListener(this) } catch (_: Exception) {}; sm = null; at = 0L }

    /** Vrai si le gyroscope a donné une mesure récente (avec la gravité connue). */
    fun alive(): Boolean = System.currentTimeMillis() - at < 400L && System.currentTimeMillis() - gAt < 2000L

    override fun onSensorChanged(e: SensorEvent) {
        when (e.sensor.type) {
            Sensor.TYPE_GRAVITY, Sensor.TYPE_ACCELEROMETER -> {
                // l'accéléromètre seul est filtré lentement pour n'en garder que la gravité
                val k = if (e.sensor.type == Sensor.TYPE_GRAVITY) 1f else 0.05f
                for (i in 0..2) g[i] += (e.values[i] - g[i]) * k
                gAt = System.currentTimeMillis()
            }
            Sensor.TYPE_GYROSCOPE -> {
                val n = Math.sqrt((g[0] * g[0] + g[1] * g[1] + g[2] * g[2]).toDouble())
                if (n < 5.0) return
                // rotation autour de la verticale ; le gyroscope compte positif dans le sens inverse des aiguilles d'une montre
                // vu du haut (la gravité mesurée pointe vers le haut), donc tourner à gauche : on inverse pour « positif à droite »
                val w = -(e.values[0] * g[0] + e.values[1] * g[1] + e.values[2] * g[2]) / n * 180.0 / Math.PI
                val dt = if (lastNs > 0L) (e.timestamp - lastNs) / 1e9 else 0.0
                lastNs = e.timestamp
                if (dt <= 0.0 || dt > 0.5) return
                total += w * dt
                rate += (w - rate) * (dt / (dt + 0.15)).coerceIn(0.0, 1.0)
                at = System.currentTimeMillis()
            }
        }
    }

    override fun onAccuracyChanged(s: Sensor?, a: Int) {}
}
