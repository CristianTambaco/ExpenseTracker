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

class ExpenseViewModel(
    private val repository: ExpenseRepository,
    recordatorioActivoInicial: Boolean = true,
    horaRecordatorioInicial: Int = 21,
    minutoRecordatorioInicial: Int = 0
) : ViewModel() {

    // Estado del formulario
    private val _monto = MutableStateFlow("")
    val monto: StateFlow<String> = _monto.asStateFlow()

    private val _descripcion = MutableStateFlow("")
    val descripcion: StateFlow<String> = _descripcion.asStateFlow()

    private val _categoriaSeleccionada = MutableStateFlow("Comida")
    val categoriaSeleccionada: StateFlow<String> = _categoriaSeleccionada.asStateFlow()

    // Estado de edición
    private var gastoEnEdicionId: Int? = null

    // Recordatorio
    private val _recordatorioActivo = MutableStateFlow(recordatorioActivoInicial)
    val recordatorioActivo: StateFlow<Boolean> = _recordatorioActivo.asStateFlow()

    private val _horaRecordatorio = MutableStateFlow(horaRecordatorioInicial)
    val horaRecordatorio: StateFlow<Int> = _horaRecordatorio.asStateFlow()

    private val _minutoRecordatorio = MutableStateFlow(minutoRecordatorioInicial)
    val minutoRecordatorio: StateFlow<Int> = _minutoRecordatorio.asStateFlow()

    // Datos
    val gastos = repository.todosLosGastos
    val total = repository.totalGeneral
    val categorias = listOf("Comida", "Transporte", "Entretenimiento", "Servicios", "Otros")

    // Formulario
    fun actualizarMonto(valor: String) {
        if (valor.isEmpty() || valor.matches(Regex("^\\d*\\.?\\d*\$"))) {
            _monto.value = valor
        }
    }

    fun actualizarDescripcion(valor: String) {
        _descripcion.value = valor
    }

    fun seleccionarCategoria(categoria: String) {
        _categoriaSeleccionada.value = categoria
    }

    // Edición
    fun cargarGastoParaEdicion(gasto: ExpenseEntity) {
        _monto.value = gasto.monto.toString()
        _descripcion.value = gasto.descripcion
        _categoriaSeleccionada.value = gasto.categoria
        gastoEnEdicionId = gasto.id
    }

    fun cancelarEdicion() {
        _monto.value = ""
        _descripcion.value = ""
        _categoriaSeleccionada.value = "Comida"
        gastoEnEdicionId = null
    }

    // Guardar (crear o actualizar)
    fun guardarGasto() {
        val montoDouble = _monto.value.toDoubleOrNull()
        if (montoDouble == null || montoDouble <= 0 || _descripcion.value.isBlank()) return

        viewModelScope.launch {
            if (gastoEnEdicionId != null) {
                // ACTUALIZAR
                val gastoActualizado = ExpenseEntity(
                    id = gastoEnEdicionId!!,
                    monto = montoDouble,
                    descripcion = _descripcion.value.trim(),
                    categoria = _categoriaSeleccionada.value,
                    fecha = System.currentTimeMillis()
                )
                repository.actualizar(gastoActualizado)
                gastoEnEdicionId = null
            } else {
                // CREAR
                val nuevoGasto = ExpenseEntity(
                    monto = montoDouble,
                    descripcion = _descripcion.value.trim(),
                    categoria = _categoriaSeleccionada.value
                )
                repository.agregar(nuevoGasto)
            }
            // Limpiar
            _monto.value = ""
            _descripcion.value = ""
            _categoriaSeleccionada.value = "Comida"
        }
    }

    // Eliminar
    fun eliminarGasto(gasto: ExpenseEntity) {
        viewModelScope.launch {
            repository.eliminar(gasto)
        }
    }

    // Recordatorio
    fun cambiarEstadoRecordatorio(activo: Boolean) {
        _recordatorioActivo.value = activo
    }

    fun actualizarHoraRecordatorio(hora: Int, minuto: Int) {
        _horaRecordatorio.value = hora
        _minutoRecordatorio.value = minuto
    }
}

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