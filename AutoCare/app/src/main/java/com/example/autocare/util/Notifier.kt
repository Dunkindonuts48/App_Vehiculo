package com.example.autocare.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import com.example.autocare.MainActivity
import com.example.autocare.R
import com.example.autocare.vehicle.Vehicle

object Notifier {

    private const val CHANNEL_ID   = "predictive_maint_channel"
    private const val CHANNEL_NAME = "Mantenimiento predictivo"
    private const val CHANNEL_DESC = "Avisos de mantenimiento según conducción"

    private fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val ch = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = CHANNEL_DESC }
                nm.createNotificationChannel(ch)
            }
        }
    }

    fun showPredictive(ctx: Context, vehicle: Vehicle, score: Int) {
        val nmCompat = NotificationManagerCompat.from(ctx)

        if (!nmCompat.areNotificationsEnabled()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                ctx, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        ensureChannel(ctx)

        val title = "Revisión anticipada recomendada"
        val text  = "${vehicle.brand} ${vehicle.model} — estilo exigente (score $score)"

        val contentIntent = pendingToTracking(ctx, vehicle.id)

        val largeIcon = BitmapFactory.decodeResource(ctx.resources, R.drawable.logo_notificacion_autocare)

        val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo_notificacion_autocare)
            .setLargeIcon(largeIcon)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setColor(ContextCompat.getColor(ctx, R.color.teal_700))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            nmCompat.notify(vehicle.id, notification)
        } catch (_: SecurityException) {
        }
    }
    fun pendingToTracking(context: Context, vehicleId: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("navigateTo", "tracking/$vehicleId")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(intent)
            getPendingIntent(vehicleId, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
    }
}