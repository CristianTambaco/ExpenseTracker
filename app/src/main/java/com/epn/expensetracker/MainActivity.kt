package com.epn.expensetracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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
            ReminderScheduler.programarRecordatorio(this, horaPendiente, minutoPendiente)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Crear dependencias
        val database = AppDatabase.getInstance(applicationContext)
        val repository = ExpenseRepository(database.expenseDao())

        // Cargar preferencias guardadas
        val recordatorioActivo = ReminderPreferences.estaActivo(this)
        val horaGuardada = ReminderPreferences.obtenerHora(this)
        val minutoGuardado = ReminderPreferences.obtenerMinuto(this)

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
                    ReminderScheduler.programarRecordatorio(this, hora, minuto)
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            ReminderScheduler.programarRecordatorio(this, hora, minuto)
        }
    }
}
