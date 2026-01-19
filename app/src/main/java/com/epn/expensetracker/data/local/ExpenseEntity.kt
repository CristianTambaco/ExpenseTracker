package com.epn.expensetracker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Representa un gasto en la base de datos.
 *
 * La anotación @Entity le dice a Room que esta clase es una tabla.
 * Cada propiedad será una columna.
 */
@Entity(tableName = "gastos")
data class ExpenseEntity(

    // La clave primaria identifica cada registro de forma única.
    // autoGenerate = true hace que Room asigne el ID automáticamente.
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // El monto del gasto. Usamos Double para manejar decimales.
    val monto: Double,

    // Una descripción corta: "Almuerzo", "Gasolina", etc.
    val descripcion: String,

    // La categoría ayuda a organizar: "Comida", "Transporte", "Entretenimiento"
    val categoria: String,

    // Guardamos la fecha como timestamp (milisegundos desde 1970).
    // Es más fácil de comparar y ordenar que un String.
    val fecha: Long = System.currentTimeMillis()
)
