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

    // O ID do canal que você já estava usando
    const val CHANNEL_ID = "water-sentinel-channel"
    private var notificationIdCounter = 0 // Para IDs de notificação únicos

    // Este método deve ser chamado pela sua classe Application
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.channel_name) // Certifique-se de ter este string resource
            val descriptionText = "Notificações sobre riscos de alagamento e enchentes" // Ou outro resource
            val importance = NotificationManager.IMPORTANCE_HIGH // Use HIGH para alertas importantes
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                vibrationPattern = longArrayOf(100, 200, 100, 200, 300, 400) // Ajuste conforme necessário
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
        // Verifica a permissão ANTES de tentar construir e enviar a notificação
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w("NotificationHelper", "Permissão POST_NOTIFICATIONS não concedida. Não é possível enviar notificação.")
                // Idealmente, a Activity já deveria ter solicitado a permissão.
                // Você pode optar por não fazer nada aqui ou logar,
                // já que a responsabilidade de pedir permissão é da Activity.
                return
            }
        }

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
            .setSmallIcon(R.drawable.background_gradient) // ADICIONE ESTE ÍCONE EM RES/DRAWABLE
            .setContentTitle(riskLevel)
            .setContentText(message)
            .setPriority(priority)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        // .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)) // Som padrão
        // .setVibrate(longArrayOf(100, 200, 300, 400, 500)) // Padrão de vibração já definido no canal para Android O+

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

    // Funções de simulação (chamadas pelos botões na Activity)
    fun simulateLowRisk(context: Context) {
        sendFloodRiskNotification(context, "Risco Baixo", "Nível de água estável. Monitore.")
    }

    fun simulateMediumRisk(context: Context) {
        sendFloodRiskNotification(context, "Risco Médio", "Atenção! Nível de água subindo. Prepare-se.")
    }

    fun simulateHighRisk(context: Context) {
        sendFloodRiskNotification(context, "Risco Alto", "PERIGO! Risco iminente de enchente. Procure um local seguro!")
    }
}