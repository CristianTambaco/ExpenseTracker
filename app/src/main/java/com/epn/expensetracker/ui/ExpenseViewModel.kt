package com.epn.expensetracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.epn.expensetracker.data.local.ExpenseEntity
import com.epn.expensetracker.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para la pantalla principal.
 *
 * Mantiene el estado de la UI y coordina las operaciones con el repositorio.
 */
class ExpenseViewModel(
    private val repository: ExpenseRepository,
    recordatorioActivoInicial: Boolean = true,
    horaRecordatorioInicial: Int = 21,
    minutoRecordatorioInicial: Int = 0
) : ViewModel() {

    // Estado del formulario de nuevo gasto
    private val _monto = MutableStateFlow("")
    val monto: StateFlow<String> = _monto.asStateFlow()

    private val _descripcion = MutableStateFlow("")
    val descripcion: StateFlow<String> = _descripcion.asStateFlow()

    private val _categoriaSeleccionada = MutableStateFlow("Comida")
    val categoriaSeleccionada: StateFlow<String> = _categoriaSeleccionada.asStateFlow()

    // Estado del recordatorio (cargado desde preferencias)
    private val _recordatorioActivo = MutableStateFlow(recordatorioActivoInicial)
    val recordatorioActivo: StateFlow<Boolean> = _recordatorioActivo.asStateFlow()

    // Hora del recordatorio (cargado desde preferencias)
    private val _horaRecordatorio = MutableStateFlow(horaRecordatorioInicial)
    val horaRecordatorio: StateFlow<Int> = _horaRecordatorio.asStateFlow()

    private val _minutoRecordatorio = MutableStateFlow(minutoRecordatorioInicial)
    val minutoRecordatorio: StateFlow<Int> = _minutoRecordatorio.asStateFlow()

    // Estado para edición de gastos
    private val _gastoEnEdicion = MutableStateFlow<ExpenseEntity?>(null)
    val gastoEnEdicion: StateFlow<ExpenseEntity?> = _gastoEnEdicion.asStateFlow()

    private val _mostrarDialogoEdicion = MutableStateFlow(false)
    val mostrarDialogoEdicion: StateFlow<Boolean> = _mostrarDialogoEdicion.asStateFlow()

    // Estados temporales para el diálogo de edición
    private val _montoEdicion = MutableStateFlow("")
    val montoEdicion: StateFlow<String> = _montoEdicion.asStateFlow()

    private val _descripcionEdicion = MutableStateFlow("")
    val descripcionEdicion: StateFlow<String> = _descripcionEdicion.asStateFlow()

    private val _categoriaEdicion = MutableStateFlow("Comida")
    val categoriaEdicion: StateFlow<String> = _categoriaEdicion.asStateFlow()

    // Lista de gastos (viene directo del repositorio)
    val gastos = repository.todosLosGastos

    // Total general
    val total = repository.totalGeneral

    // Categorías disponibles
    val categorias = listOf("Comida", "Transporte", "Entretenimiento", "Servicios", "Otros")

    // Funciones para actualizar el formulario
    fun actualizarMonto(valor: String) {
        // Solo permitimos números y un punto decimal
        if (valor.isEmpty() || valor.matches(Regex("^\\d*\\.?\\d*$"))) {
            _monto.value = valor
        }
    }

    fun actualizarDescripcion(valor: String) {
        _descripcion.value = valor
    }

    fun seleccionarCategoria(categoria: String) {
        _categoriaSeleccionada.value = categoria
    }

    /**
     * Activa o desactiva el recordatorio.
     */
    fun cambiarEstadoRecordatorio(activo: Boolean) {
        _recordatorioActivo.value = activo
    }

    /**
     * Actualiza la hora del recordatorio.
     */
    fun actualizarHoraRecordatorio(hora: Int, minuto: Int) {
        _horaRecordatorio.value = hora
        _minutoRecordatorio.value = minuto
    }

    /**
     * Guarda un nuevo gasto y limpia el formulario.
     */
    fun guardarGasto() {
        val montoDouble = _monto.value.toDoubleOrNull()

        // Validación básica
        if (montoDouble == null || montoDouble <= 0) return
        if (_descripcion.value.isBlank()) return

        // viewModelScope cancela automáticamente si el ViewModel se destruye
        viewModelScope.launch {
            val nuevoGasto = ExpenseEntity(
                monto = montoDouble,
                descripcion = _descripcion.value.trim(),
                categoria = _categoriaSeleccionada.value
            )
            repository.agregar(nuevoGasto)

            // Limpiar formulario después de guardar
            _monto.value = ""
            _descripcion.value = ""
        }
    }

    /**
     * Elimina un gasto de la base de datos.
     */
    fun eliminarGasto(gasto: ExpenseEntity) {
        viewModelScope.launch {
            repository.eliminar(gasto)
        }
    }

    /**
     * Inicia la edición de un gasto existente.
     * Carga los datos del gasto en el formulario de edición.
     */
    fun iniciarEdicion(gasto: ExpenseEntity) {
        _gastoEnEdicion.value = gasto
        _montoEdicion.value = gasto.monto.toString()
        _descripcionEdicion.value = gasto.descripcion
        _categoriaEdicion.value = gasto.categoria
        _mostrarDialogoEdicion.value = true
    }

    /**
     * Actualiza los campos del formulario de edición.
     */
    fun actualizarMontoEdicion(valor: String) {
        if (valor.isEmpty() || valor.matches(Regex("^\\d*\\.?\\d*$"))) {
            _montoEdicion.value = valor
        }
    }

    fun actualizarDescripcionEdicion(valor: String) {
        _descripcionEdicion.value = valor
    }

    fun seleccionarCategoriaEdicion(categoria: String) {
        _categoriaEdicion.value = categoria
    }

    /**
     * Guarda los cambios del gasto editado.
     */
    fun actualizarGasto() {
        val gastoOriginal = _gastoEnEdicion.value ?: return
        val montoDouble = _montoEdicion.value.toDoubleOrNull()

        // Validación básica
        if (montoDouble == null || montoDouble <= 0) return
        if (_descripcionEdicion.value.isBlank()) return

        viewModelScope.launch {
            val gastoActualizado = gastoOriginal.copy(
                monto = montoDouble,
                descripcion = _descripcionEdicion.value.trim(),
                categoria = _categoriaEdicion.value
            )
            repository.actualizar(gastoActualizado)
            cancelarEdicion()
        }
    }

    /**
     * Cancela la edición y cierra el diálogo.
     */
    fun cancelarEdicion() {
        _gastoEnEdicion.value = null
        _mostrarDialogoEdicion.value = false
        _montoEdicion.value = ""
        _descripcionEdicion.value = ""
        _categoriaEdicion.value = "Comida"
    }
}

/**
 * Factory para crear el ViewModel con sus dependencias.
 *
 * Esto es necesario porque ViewModel no puede recibir parámetros
 * en su constructor directamente.
 */
class ExpenseViewModelFactory(
    private val repository: ExpenseRepository,
    private val recordatorioActivo: Boolean,
    private val horaRecordatorio: Int,
    private val minutoRecordatorio: Int
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
            return ExpenseViewModel(
                repository,
                recordatorioActivo,
                horaRecordatorio,
                minutoRecordatorio
            ) as T
        }
        throw IllegalArgumentException("ViewModel desconocido")
    }
}
