package com.epn.expensetracker.ui

import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.epn.expensetracker.alarm.ReminderPreferences
import com.epn.expensetracker.data.local.ExpenseEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pantalla principal de la aplicación.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(
    viewModel: ExpenseViewModel,
    onRecordatorioChange: (Boolean, Int, Int) -> Unit
) {
    // Estados del formulario
    val monto by viewModel.monto.collectAsState()
    val descripcion by viewModel.descripcion.collectAsState()
    val categoriaSeleccionada by viewModel.categoriaSeleccionada.collectAsState()
    val gastos by viewModel.gastos.collectAsState(initial = emptyList())
    val total by viewModel.total.collectAsState(initial = 0.0)

    // Estados del recordatorio
    val recordatorioActivo by viewModel.recordatorioActivo.collectAsState()
    val horaRecordatorio by viewModel.horaRecordatorio.collectAsState()
    val minutoRecordatorio by viewModel.minutoRecordatorio.collectAsState()

    // Estados para edición
    val mostrarDialogoEdicion by viewModel.mostrarDialogoEdicion.collectAsState()
    val montoEdicion by viewModel.montoEdicion.collectAsState()
    val descripcionEdicion by viewModel.descripcionEdicion.collectAsState()
    val categoriaEdicion by viewModel.categoriaEdicion.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Gastos") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Formulario para agregar gasto
            FormularioGasto(
                monto = monto,
                descripcion = descripcion,
                categoriaSeleccionada = categoriaSeleccionada,
                categorias = viewModel.categorias,
                onMontoChange = { viewModel.actualizarMonto(it) },
                onDescripcionChange = { viewModel.actualizarDescripcion(it) },
                onCategoriaChange = { viewModel.seleccionarCategoria(it) },
                onGuardar = { viewModel.guardarGasto() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Configuración del recordatorio
            ConfiguracionRecordatorio(
                activo = recordatorioActivo,
                hora = horaRecordatorio,
                minuto = minutoRecordatorio,
                onActivoChange = { nuevoEstado ->
                    viewModel.cambiarEstadoRecordatorio(nuevoEstado)
                    onRecordatorioChange(nuevoEstado, horaRecordatorio, minutoRecordatorio)
                },
                onHoraChange = { nuevaHora, nuevoMinuto ->
                    viewModel.actualizarHoraRecordatorio(nuevaHora, nuevoMinuto)
                    if (recordatorioActivo) {
                        onRecordatorioChange(true, nuevaHora, nuevoMinuto)
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Total gastado
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Text(
                    text = "Total: $${String.format("%.2f", total ?: 0.0)}",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Lista de gastos
            Text(
                text = "Historial",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (gastos.isEmpty()) {
                Text(
                    text = "No hay gastos registrados",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                gastos.forEach { gasto ->
                    GastoItem(
                        gasto = gasto,
                        onEliminar = { viewModel.eliminarGasto(gasto) },
                        onEditar = { viewModel.iniciarEdicion(gasto) }
                    )
                }
            }
        }

        // Diálogo de edición
        if (mostrarDialogoEdicion) {
            DialogoEditarGasto(
                monto = montoEdicion,
                descripcion = descripcionEdicion,
                categoriaSeleccionada = categoriaEdicion,
                categorias = viewModel.categorias,
                onMontoChange = { viewModel.actualizarMontoEdicion(it) },
                onDescripcionChange = { viewModel.actualizarDescripcionEdicion(it) },
                onCategoriaChange = { viewModel.seleccionarCategoriaEdicion(it) },
                onConfirmar = { viewModel.actualizarGasto() },
                onCancelar = { viewModel.cancelarEdicion() }
            )
        }
    }
}

/**
 * Tarjeta de configuración del recordatorio.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguracionRecordatorio(
    activo: Boolean,
    hora: Int,
    minuto: Int,
    onActivoChange: (Boolean) -> Unit,
    onHoraChange: (Int, Int) -> Unit
) {
    var mostrarTimePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    // Estado para vibración
    var vibracionActiva by remember { 
        mutableStateOf(ReminderPreferences.esVibracionActiva(context)) 
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Recordatorio diario",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Activar notificación",
                    style = MaterialTheme.typography.bodyMedium
                )
                Switch(
                    checked = activo,
                    onCheckedChange = onActivoChange
                )
            }

            // Opciones adicionales (solo visibles si está activo)
            if (activo) {
                Spacer(modifier = Modifier.height(8.dp))

                // Selector de hora
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { mostrarTimePicker = true },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Hora del recordatorio",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    val horaFormateada = String.format("%02d:%02d", hora, minuto)
                    Text(
                        text = horaFormateada,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Selector de sonido personalizado
                SelectorSonido(
                    onSonidoSeleccionado = { uri ->
                        ReminderPreferences.guardarSonidoUri(context, uri?.toString())
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Switch de vibración
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Vibración",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = vibracionActiva,
                        onCheckedChange = { activa ->
                            vibracionActiva = activa
                            ReminderPreferences.guardarVibracionActiva(context, activa)
                        }
                    )
                }
            }
        }
    }

    // Diálogo con el TimePicker
    if (mostrarTimePicker) {
        TimePickerDialog(
            horaInicial = hora,
            minutoInicial = minuto,
            onConfirm = { nuevaHora, nuevoMinuto ->
                onHoraChange(nuevaHora, nuevoMinuto)
                mostrarTimePicker = false
            },
            onDismiss = { mostrarTimePicker = false }
        )
    }
}

/**
 * Diálogo con el TimePicker de Material 3.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    horaInicial: Int,
    minutoInicial: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = horaInicial,
        initialMinute = minutoInicial,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seleccionar hora") },
        text = {
            TimePicker(state = timePickerState)
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(timePickerState.hour, timePickerState.minute) }
            ) {
                Text("Aceptar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

/**
 * Formulario para ingresar un nuevo gasto.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioGasto(
    monto: String,
    descripcion: String,
    categoriaSeleccionada: String,
    categorias: List<String>,
    onMontoChange: (String) -> Unit,
    onDescripcionChange: (String) -> Unit,
    onCategoriaChange: (String) -> Unit,
    onGuardar: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Nuevo Gasto",
                style = MaterialTheme.typography.titleMedium
            )

            // Campo de monto
            OutlinedTextField(
                value = monto,
                onValueChange = onMontoChange,
                label = { Text("Monto") },
                leadingIcon = { Text("$") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Campo de descripción
            OutlinedTextField(
                value = descripcion,
                onValueChange = onDescripcionChange,
                label = { Text("Descripción") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Selector de categoría (Dropdown)
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = categoriaSeleccionada,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Categoría") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    categorias.forEach { categoria ->
                        DropdownMenuItem(
                            text = { Text(categoria) },
                            onClick = {
                                onCategoriaChange(categoria)
                                expanded = false
                            }
                        )
                    }
                }
            }

            // Botón guardar
            Button(
                onClick = onGuardar,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar")
            }
        }
    }
}

/**
 * Muestra un gasto individual en la lista.
 */
@Composable
fun GastoItem(
    gasto: ExpenseEntity,
    onEliminar: () -> Unit,
    onEditar: () -> Unit
) {
    val fechaFormateada = remember(gasto.fecha) {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        sdf.format(Date(gasto.fecha))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = gasto.descripcion,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "${gasto.categoria} • $fechaFormateada",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "$${String.format("%.2f", gasto.monto)}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            IconButton(onClick = onEditar) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Editar",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(onClick = onEliminar) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * Diálogo modal para editar un gasto existente.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogoEditarGasto(
    monto: String,
    descripcion: String,
    categoriaSeleccionada: String,
    categorias: List<String>,
    onMontoChange: (String) -> Unit,
    onDescripcionChange: (String) -> Unit,
    onCategoriaChange: (String) -> Unit,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Editar Gasto") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Campo de monto
                OutlinedTextField(
                    value = monto,
                    onValueChange = onMontoChange,
                    label = { Text("Monto") },
                    leadingIcon = { Text("$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Campo de descripción
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = onDescripcionChange,
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Selector de categoría
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = categoriaSeleccionada,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoría") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        categorias.forEach { categoria ->
                            DropdownMenuItem(
                                text = { Text(categoria) },
                                onClick = {
                                    onCategoriaChange(categoria)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirmar) {
                Text("Actualizar")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onCancelar) {
                Text("Cancelar")
            }
        }
    )
}

/**
 * Selector de sonido personalizado para notificaciones.
 */
@Composable
fun SelectorSonido(
    onSonidoSeleccionado: (Uri?) -> Unit
) {
    val context = LocalContext.current
    
    // Obtener el nombre del sonido actual
    var nombreSonido by remember {
        val uriString = ReminderPreferences.obtenerSonidoUri(context)
        val nombre = if (uriString != null) {
            try {
                val ringtone = RingtoneManager.getRingtone(context, Uri.parse(uriString))
                ringtone?.getTitle(context) ?: "Personalizado"
            } catch (e: Exception) {
                "Personalizado"
            }
        } else {
            "Predeterminado"
        }
        mutableStateOf(nombre)
    }

    // Launcher para el selector de tonos
    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        onSonidoSeleccionado(uri)
        
        // Actualizar nombre del sonido
        nombreSonido = if (uri != null) {
            try {
                val ringtone = RingtoneManager.getRingtone(context, uri)
                ringtone?.getTitle(context) ?: "Personalizado"
            } catch (e: Exception) {
                "Personalizado"
            }
        } else {
            "Predeterminado"
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val existingUri = ReminderPreferences.obtenerSonidoUri(context)
                val intent = android.content.Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Seleccionar sonido")
                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                    if (existingUri != null) {
                        putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(existingUri))
                    }
                }
                ringtonePickerLauncher.launch(intent)
            }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {

//            Icon(
//                imageVector = Icons.Default.Notifications,
//                contentDescription = null,
//                tint = MaterialTheme.colorScheme.primary
//            )

            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Sonido",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Text(
            text = nombreSonido,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
