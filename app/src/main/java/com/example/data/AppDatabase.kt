package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.ExpenseDao
import com.example.data.entity.Expense
import com.example.data.entity.ExpenseCategory
import com.example.data.entity.FamilyMember
import com.example.data.entity.Income
import com.example.data.entity.RecurringExpense
import com.example.data.entity.SavingsGoal

@Database(
    entities = [
        Expense::class,
        FamilyMember::class,
        ExpenseCategory::class,
        RecurringExpense::class,
        SavingsGoal::class,
        Income::class
    ],
    version = 12,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                // Copy legacy database file if present
                try {
                    val appContext = context.applicationContext
                    val oldDb = appContext.getDatabasePath("oikonomos_database")
                    val newDb = appContext.getDatabasePath("fintel_database")
                    if (oldDb.exists() && !newDb.exists()) {
                        oldDb.copyTo(newDb, overwrite = true)
                        val oldWal = appContext.getDatabasePath("oikonomos_database-wal")
                        val newWal = appContext.getDatabasePath("fintel_database-wal")
                        if (oldWal.exists()) oldWal.copyTo(newWal, overwrite = true)
                        val oldShm = appContext.getDatabasePath("oikonomos_database-shm")
                        val newShm = appContext.getDatabasePath("fintel_database-shm")
                        if (oldShm.exists()) oldShm.copyTo(newShm, overwrite = true)
                    }
                } catch (ignored: Exception) {}

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fintel_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
