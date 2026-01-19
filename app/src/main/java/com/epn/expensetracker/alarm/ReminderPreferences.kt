package com.epn.expensetracker.alarm

import android.content.Context
import android.content.SharedPreferences

/**
 * Maneja la persistencia de las preferencias de recordatorio.
 */
object ReminderPreferences {

    private const val PREFS_NAME = "reminder_prefs"
    private const val KEY_ACTIVO = "recordatorio_activo"
    private const val KEY_HORA = "recordatorio_hora"
    private const val KEY_MINUTO = "recordatorio_minuto"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun guardarConfiguracion(context: Context, activo: Boolean, hora: Int, minuto: Int) {
        getPrefs(context).edit().apply {
            putBoolean(KEY_ACTIVO, activo)
            putInt(KEY_HORA, hora)
            putInt(KEY_MINUTO, minuto)
            apply()
        }
    }

    fun estaActivo(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_ACTIVO, true)
    }

    fun obtenerHora(context: Context): Int {
        return getPrefs(context).getInt(KEY_HORA, 21)  // 9 PM por defecto
    }

    fun obtenerMinuto(context: Context): Int {
        return getPrefs(context).getInt(KEY_MINUTO, 0)
    }
}
