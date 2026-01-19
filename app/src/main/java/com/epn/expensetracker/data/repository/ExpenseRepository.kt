package com.epn.expensetracker.data.repository

import com.epn.expensetracker.data.local.ExpenseDao
import com.epn.expensetracker.data.local.ExpenseEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio que centraliza el acceso a los datos de gastos.
 *
 * Actualmente solo usa Room, pero podría agregar:
 * - Caché en memoria
 * - Sincronización con API remota
 * - Validaciones de negocio
 */
class ExpenseRepository(private val dao: ExpenseDao) {

    // Exponemos los datos como Flow para reactividad
    val todosLosGastos: Flow<List<ExpenseEntity>> = dao.obtenerTodos()

    val totalGeneral: Flow<Double?> = dao.totalGeneral()

    fun totalPorCategoria(categoria: String): Flow<Double?> {
        return dao.totalPorCategoria(categoria)
    }

    suspend fun agregar(gasto: ExpenseEntity) {
        dao.insertar(gasto)
    }

    suspend fun actualizar(gasto: ExpenseEntity) {
        dao.actualizar(gasto)
    }

    suspend fun eliminar(gasto: ExpenseEntity) {
        dao.eliminar(gasto)
    }
}
