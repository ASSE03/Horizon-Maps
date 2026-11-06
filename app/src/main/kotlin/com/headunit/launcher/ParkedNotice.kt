package com.headunit.launcher

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

/**
 * Fork FR : à la déconnexion d'Android Auto (voiture garée), une notification « Voiture garée » avec un bouton « À pied »
 * qui ouvre l'appli et calcule l'itinéraire piéton jusqu'à la voiture.
 */
object ParkedNotice {
    private const val CHANNEL = "parked_car"
    private const val ID = 4711
    const val EXTRA_WALK = "fr.walk_to_car"

    fun show(ctx: Context) {
        try {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL) == null) {
                nm.createNotificationChannel(NotificationChannel(CHANNEL, "Voiture garée", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Quand Android Auto se déconnecte : où est garée la voiture, avec l'itinéraire à pied"
                    setShowBadge(false)
                })
            }
            val open = PendingIntent.getActivity(ctx, 1,
                Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val walk = PendingIntent.getActivity(ctx, 2,
                Intent(ctx, MainActivity::class.java).putExtra(EXTRA_WALK, true).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val time = java.text.SimpleDateFormat("HH:mm", java.util.Locale.FRANCE).format(java.util.Date())
            val n = android.app.Notification.Builder(ctx, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_nav)
                .setContentTitle("Voiture garée")
                .setContentText("Emplacement mémorisé à $time. « À pied » pour y retourner.")
                .setContentIntent(open)
                .addAction(android.app.Notification.Action.Builder(android.graphics.drawable.Icon.createWithResource(ctx, R.drawable.ic_stat_nav), "À pied", walk).build())
                .setAutoCancel(true)
                .build()
            nm.notify(ID, n)
        } catch (e: Exception) { }
    }

    fun cancel(ctx: Context) {
        try { (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(ID) } catch (e: Exception) { }
    }
}
