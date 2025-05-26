package com.example.water_sentinel

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.app.Application

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(applicationContext)
    }
}

object NotificationHelper {

    const val CHANNEL_ID = "water-sentinel-channel"
    private var notificationIdCounter = 0

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.channel_name)
            val descriptionText = "Notificações sobre riscos de alagamento e enchentes"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                vibrationPattern = longArrayOf(100, 200, 100, 200, 300, 400)
                // enableLights(true)
                // lightColor = Color.RED // Se quiser luzes
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d("NotificationHelper", "Canal de notificação '$CHANNEL_ID' criado.")
        }
    }

    fun sendFloodRiskNotification(
        context: Context,
        riskLevel: String, // Ex: "Risco Baixo", "Risco Médio", "Risco Alto"
        message: String
    ) {

        val notificationId = notificationIdCounter++

        // Intent para abrir o app ao clicar na notificação
        val intent = Intent(context, DashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(context, notificationId /* requestCode precisa ser único se o intent for diferente */, intent, pendingIntentFlags)

        val priority = when (riskLevel) {
            "Risco Alto" -> NotificationCompat.PRIORITY_HIGH
            "Risco Médio" -> NotificationCompat.PRIORITY_DEFAULT
            "Risco Baixo" -> NotificationCompat.PRIORITY_LOW
            else -> NotificationCompat.PRIORITY_DEFAULT
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.perigo_chuva) // icone da notificação
            .setContentTitle(riskLevel)
            .setContentText(message)
            .setPriority(priority)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(100, 200, 300, 400, 500))

        with(NotificationManagerCompat.from(context)) {
            try {
                notify(notificationId, builder.build())
                Log.d("NotificationHelper", "Notificação enviada: ID $notificationId, Título: $riskLevel")
            } catch (e: SecurityException) {
                Log.e("NotificationHelper", "Erro de segurança ao enviar notificação. Permissão POST_NOTIFICATIONS está faltando?", e)
                // Este catch é uma segurança extra, a checagem de permissão acima deveria prevenir isso.
            }
        }
    }

    fun simulateLowRisk(context: Context) {
        sendFloodRiskNotification(context, "Baixo Risco", "Sem chuva, sem preocupações!")
    }

    fun simulateMediumRisk(context: Context) {
        sendFloodRiskNotification(context, "Médio Risco", "Atenção! Nível de água subindo. Prepare-se.")
    }

    fun simulateHighRisk(context: Context) {
        sendFloodRiskNotification(context, "Alto Risco ", "PERIGO! Risco iminente de enchente. Procure um local seguro!")
    }
}