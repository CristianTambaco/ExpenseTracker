package com.epn.expensetracker

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.epn.expensetracker.alarm.ReminderPreferences
import com.epn.expensetracker.alarm.ReminderScheduler
import com.epn.expensetracker.data.local.AppDatabase
import com.epn.expensetracker.data.repository.ExpenseRepository
import com.epn.expensetracker.ui.ExpenseScreen
import com.epn.expensetracker.ui.ExpenseViewModel
import com.epn.expensetracker.ui.ExpenseViewModelFactory
import com.epn.expensetracker.ui.theme.ExpenseTrackerTheme

class MainActivity : ComponentActivity() {

    // Guardamos la hora pendiente por si hay que pedir permiso primero
    private var horaPendiente = 21
    private var minutoPendiente = 0

    // Launcher para pedir permiso de notificaciones
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            verificarYProgramarAlarma()
        } else {
            Toast.makeText(
                this,
                "Se necesita permiso de notificaciones para los recordatorios",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verificar y solicitar permisos necesarios al inicio
        verificarPermisosIniciales()

        // Crear dependencias
        val database = AppDatabase.getInstance(applicationContext)
        val repository = ExpenseRepository(database.expenseDao())

        // Cargar preferencias guardadas
        val recordatorioActivo = ReminderPreferences.estaActivo(this)
        val horaGuardada = ReminderPreferences.obtenerHora(this)
        val minutoGuardado = ReminderPreferences.obtenerMinuto(this)

        // Si hay recordatorio activo, reprogramar (por si la app fue cerrada)
        if (recordatorioActivo) {
            horaPendiente = horaGuardada
            minutoPendiente = minutoGuardado
            verificarYProgramarAlarma()
        }

        val viewModelFactory = ExpenseViewModelFactory(
            repository,
            recordatorioActivo,
            horaGuardada,
            minutoGuardado
        )

        setContent {
            ExpenseTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: ExpenseViewModel = viewModel(factory = viewModelFactory)

                    ExpenseScreen(
                        viewModel = viewModel,
                        onRecordatorioChange = { activo, hora, minuto ->
                            manejarCambioRecordatorio(activo, hora, minuto)
                        }
                    )
                }
            }
        }
    }

    /**
     * Verifica permisos necesarios al iniciar la app.
     */
    private fun verificarPermisosIniciales() {
        // Solicitar desactivar optimización de batería
        solicitarDesactivarOptimizacionBateria()
    }

    /**
     * Solicita al usuario desactivar la optimización de batería para la app.
     * Esto es importante para que las alarmas funcionen en segundo plano.
     */
    private fun solicitarDesactivarOptimizacionBateria() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            val packageName = packageName
            
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    // Si falla, intentar abrir configuración general de batería
                    try {
                        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        startActivity(intent)
                    } catch (e2: Exception) {
                        e2.printStackTrace()
                    }
                }
            }
        }
    }

    /**
     * Maneja los cambios en la configuración del recordatorio.
     */
    private fun manejarCambioRecordatorio(activo: Boolean, hora: Int, minuto: Int) {
        // Guardar preferencias siempre
        ReminderPreferences.guardarConfiguracion(this, activo, hora, minuto)

        if (!activo) {
            ReminderScheduler.cancelarRecordatorio(this)
            return
        }

        horaPendiente = hora
        minutoPendiente = minuto

        // Verificar permisos (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    verificarYProgramarAlarma()
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            verificarYProgramarAlarma()
        }
    }

    /**
     * Verifica permiso de alarmas exactas y programa la alarma.
     */
    private fun verificarYProgramarAlarma() {
        // Verificar permiso de alarmas exactas (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                // Solicitar permiso de alarmas exactas
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                    Toast.makeText(
                        this,
                        "Por favor, permite alarmas exactas para que los recordatorios funcionen correctamente",
                        Toast.LENGTH_LONG
                    ).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        
        // Programar la alarma
        ReminderScheduler.programarRecordatorio(this, horaPendiente, minutoPendiente)
    }
}
