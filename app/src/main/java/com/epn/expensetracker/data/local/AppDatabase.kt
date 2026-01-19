package com.epn.expensetracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Clase principal de la base de datos.
 *
 * @Database indica:
 * - entities: qué tablas tiene (puede ser más de una)
 * - version: versión del esquema (importante para migraciones)
 */
@Database(
    entities = [ExpenseEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    // Room implementa esto automáticamente
    abstract fun expenseDao(): ExpenseDao

    companion object {
        /*
         * Usamos el patrón Singleton para tener una única instancia.
         *
         * Crear conexiones a la base de datos es costoso.
         * Si creamos muchas instancias, la app se vuelve lenta y
         * podemos tener problemas de concurrencia.
         */

        @Volatile  // Asegura que los cambios sean visibles en todos los hilos
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            // Si ya existe una instancia, la retornamos
            return INSTANCE ?: synchronized(this) {
                // synchronized evita que dos hilos creen instancias al mismo tiempo
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expense_database"  // nombre del archivo .db
                ).build()

                INSTANCE = instance
                instance
            }
        }
    }
}
