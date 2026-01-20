package com.epn.expensetracker.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import java.util.Calendar

/**
 * Clase utilitaria para programar el recordatorio diario usando AlarmManager.
 * Usa alarmas exactas para garantizar que las notificaciones lleguen a la hora configurada,
 * incluso con optimización de batería activa (Doze mode).
 */
object ReminderScheduler {

    private const val TAG = "ReminderScheduler"
    private const val ALARM_REQUEST_CODE = 1001

    /**
     * Verifica si la app puede programar alarmas exactas (Android 12+).
     */
    fun puedeUsarAlarmasExactas(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    /**
     * Obtiene el Intent para abrir la configuración de alarmas exactas.
     */
    fun getIntentConfiguracionAlarmas(context: Context): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else {
            null
        }
    }

    /**
     * Programa un recordatorio diario a la hora indicada.
     */
    fun programarRecordatorio(context: Context, hora: Int, minuto: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.epn.expensetracker.ALARM_TRIGGER"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Calcular el tiempo para la próxima alarma
        val calendario = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hora)
            set(Calendar.MINUTE, minuto)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Si ya pasó la hora, programar para mañana
        if (calendario.timeInMillis <= System.currentTimeMillis()) {
            calendario.add(Calendar.DAY_OF_MONTH, 1)
        }

        Log.d(TAG, "Programando alarma para: ${calendario.time}")

        try {
            // Android 12+ requiere verificar permiso de alarmas exactas
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setAlarmClock(
                        AlarmManager.AlarmClockInfo(calendario.timeInMillis, pendingIntent),
                        pendingIntent
                    )
                    Log.d(TAG, "Alarma programada con setAlarmClock (Android 12+)")
                } else {
                    // Fallback: usar alarma inexacta si no hay permiso
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendario.timeInMillis,
                        pendingIntent
                    )
                    Log.d(TAG, "Usando alarma inexacta (sin permiso de alarmas exactas)")
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Android 6-11: usar setAlarmClock para máxima fiabilidad
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(calendario.timeInMillis, pendingIntent),
                    pendingIntent
                )
                Log.d(TAG, "Alarma programada con setAlarmClock (Android 6-11)")
            } else {
                // Android 5 y menor
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendario.timeInMillis,
                    pendingIntent
                )
                Log.d(TAG, "Alarma programada con setExact (Android 5)")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Error de seguridad al programar alarma", e)
            // Fallback a alarma inexacta
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendario.timeInMillis,
                pendingIntent
            )
        }
    }

    /**
     * Cancela el recordatorio programado.
     */
    fun cancelarRecordatorio(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.epn.expensetracker.ALARM_TRIGGER"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Alarma cancelada")
    }
}
