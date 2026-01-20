package com.epn.expensetracker.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Define las operaciones disponibles para la tabla de gastos.
 *
 * Room genera la implementación automáticamente basándose
 * en las anotaciones y los tipos de retorno.
 */
@Dao
interface ExpenseDao {

    /**
     * Obtiene todos los gastos ordenados del más reciente al más antiguo.
     *
     * Retorna Flow para que la UI se actualice automáticamente
     * cuando haya cambios en la base de datos.
     */
    @Query("SELECT * FROM gastos ORDER BY fecha DESC")
    fun obtenerTodos(): Flow<List<ExpenseEntity>>

    /**
     * Calcula el total gastado en una categoría específica.
     * Útil para mostrar estadísticas.
     */
    @Query("SELECT SUM(monto) FROM gastos WHERE categoria = :categoria")
    fun totalPorCategoria(categoria: String): Flow<Double?>

    /**
     * Calcula el total de todos los gastos.
     */
    @Query("SELECT SUM(monto) FROM gastos")
    fun totalGeneral(): Flow<Double?>

    /**
     * Inserta un nuevo gasto.
     * suspend indica que es una operación asíncrona (no bloquea el hilo principal).
     */
    @Insert
    suspend fun insertar(gasto: ExpenseEntity)

    /**
     * Actualiza un gasto existente.
     * Room usa el ID para saber cuál actualizar.
     */
    @Update
    suspend fun actualizar(gasto: ExpenseEntity)

    /**
     * Elimina un gasto.
     */
    @Delete
    suspend fun eliminar(gasto: ExpenseEntity)
}
