package com.epn.expensetracker.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * BroadcastReceiver que se ejecuta cuando el dispositivo se reinicia.
 * Reprograma las alarmas de recordatorio ya que se pierden al reiniciar.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON" ||
            intent.action == "com.htc.intent.action.QUICKBOOT_POWERON") {
            
            // Verificar si el recordatorio estaba activo
            if (ReminderPreferences.estaActivo(context)) {
                val hora = ReminderPreferences.obtenerHora(context)
                val minuto = ReminderPreferences.obtenerMinuto(context)
                
                // Reprogramar el recordatorio
                ReminderScheduler.programarRecordatorio(context, hora, minuto)
            }
        }
    }
}
