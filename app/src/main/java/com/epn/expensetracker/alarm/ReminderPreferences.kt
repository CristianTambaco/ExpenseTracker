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
    private const val KEY_SONIDO_URI = "sonido_uri"
    private const val KEY_VIBRACION_ACTIVA = "vibracion_activa"

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

    fun guardarSonidoUri(context: Context, uri: String?) {
        getPrefs(context).edit().apply {
            putString(KEY_SONIDO_URI, uri)
            apply()
        }
    }

    fun obtenerSonidoUri(context: Context): String? {
        return getPrefs(context).getString(KEY_SONIDO_URI, null)
    }

    fun guardarVibracionActiva(context: Context, activa: Boolean) {
        getPrefs(context).edit().apply {
            putBoolean(KEY_VIBRACION_ACTIVA, activa)
            apply()
        }
    }

    fun esVibracionActiva(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_VIBRACION_ACTIVA, true)
    }
}
