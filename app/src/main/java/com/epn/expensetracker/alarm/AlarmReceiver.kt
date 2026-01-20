package com.epn.expensetracker.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.epn.expensetracker.MainActivity
import com.epn.expensetracker.R

/**
 * BroadcastReceiver que se ejecuta cuando la alarma se dispara.
 * Funciona incluso con la app completamente cerrada y el teléfono bloqueado.
 */
class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AlarmReceiver"
        const val CHANNEL_ID = "expense_reminder_channel"
        const val NOTIFICATION_ID = 1001
        private const val WAKELOCK_TIMEOUT = 10000L // 10 segundos
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive: Alarma recibida!")
        
        // Adquirir WakeLock para asegurar que el dispositivo se mantenga despierto
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "ExpenseTracker:AlarmWakeLock"
        )
        wakeLock.acquire(WAKELOCK_TIMEOUT)
        
        try {
            mostrarNotificacion(context)

            // Reprogramar para el día siguiente
            val hora = ReminderPreferences.obtenerHora(context)
            val minuto = ReminderPreferences.obtenerMinuto(context)
            ReminderScheduler.programarRecordatorio(context, hora, minuto)
            Log.d(TAG, "Alarma reprogramada para mañana a las $hora:$minuto")
        } finally {
            // Liberar WakeLock
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
        }
    }

    private fun mostrarNotificacion(context: Context) {
        val notificationManager = context
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Obtener sonido personalizado o usar el predeterminado
        val sonidoUriString = ReminderPreferences.obtenerSonidoUri(context)
        val soundUri: Uri = if (sonidoUriString != null) {
            Uri.parse(sonidoUriString)
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }

        // Verificar si la vibración está activa
        val vibracionActiva = ReminderPreferences.esVibracionActiva(context)

        // Patrón de vibración personalizado: espera, vibra, pausa, vibra
        val vibrationPattern = longArrayOf(0, 500, 200, 500)

        // Crear el canal (obligatorio desde Android 8.0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Eliminar canal anterior para aplicar nuevos ajustes
            notificationManager.deleteNotificationChannel(CHANNEL_ID)
            
            val canal = NotificationChannel(
                CHANNEL_ID,
                "Recordatorios de Gastos",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Recordatorios diarios para registrar gastos"
                
                // Configurar sonido con atributos de audio
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()
                setSound(soundUri, audioAttributes)
                
                // Configurar vibración
                enableVibration(vibracionActiva)
                if (vibracionActiva) {
                    vibrationPattern.let { this.vibrationPattern = it }
                }
                
                // Asegurar que las luces estén habilitadas
                enableLights(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(canal)
        }

        // Vibrar manualmente para garantizar vibración en todos los dispositivos
        if (vibracionActiva) {
            vibrarDispositivo(context, vibrationPattern)
        }

        // Reproducir sonido manualmente para garantizar que suene
        reproducirSonido(context, soundUri)

        // Intent para abrir la app al tocar la notificación
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Full-screen intent para mostrar en pantalla de bloqueo (Android 10+)
        val fullScreenIntent = PendingIntent.getActivity(
            context,
            1,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("¿Registraste tus gastos?")
            .setContentText("No olvides anotar lo que gastaste hoy")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(fullScreenIntent, true)
            .setDefaults(0)

        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
        Log.d(TAG, "Notificación mostrada")
    }

    /**
     * Vibra el dispositivo manualmente para garantizar que funcione.
     */
    private fun vibrarDispositivo(context: Context, pattern: LongArray) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val vibrator = vibratorManager.defaultVibrator
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
            Log.d(TAG, "Vibración ejecutada")
        } catch (e: Exception) {
            Log.e(TAG, "Error al vibrar", e)
        }
    }

    /**
     * Reproduce el sonido de notificación manualmente.
     */
    private fun reproducirSonido(context: Context, soundUri: Uri) {
        try {
            val ringtone = RingtoneManager.getRingtone(context, soundUri)
            ringtone?.play()
            Log.d(TAG, "Sonido reproducido: $soundUri")
        } catch (e: Exception) {
            Log.e(TAG, "Error al reproducir sonido", e)
            // Fallback al sonido predeterminado
            try {
                val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val ringtone = RingtoneManager.getRingtone(context, defaultUri)
                ringtone?.play()
            } catch (e2: Exception) {
                Log.e(TAG, "Error al reproducir sonido predeterminado", e2)
            }
        }
    }
}
