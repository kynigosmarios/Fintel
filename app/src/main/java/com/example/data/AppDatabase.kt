package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.ExpenseDao
import com.example.data.entity.Expense
import com.example.data.entity.ExpenseCategory
import com.example.data.entity.FamilyMember
import com.example.data.entity.RecurringExpense
import com.example.data.entity.SavingsGoal

@Database(
    entities = [Expense::class, FamilyMember::class, ExpenseCategory::class, RecurringExpense::class, SavingsGoal::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Version 1 to 2 migrations (preserved)
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `recurring_expenses` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `categoryId` INTEGER NOT NULL,
                        `categoryName` TEXT NOT NULL,
                        `frequency` TEXT NOT NULL,
                        `dayOfMonth` INTEGER NOT NULL,
                        `splitType` TEXT NOT NULL,
                        `memberIdsCsv` TEXT NOT NULL,
                        `memberNamesDisplay` TEXT NOT NULL,
                        `note` TEXT NOT NULL,
                        `isActive` INTEGER NOT NULL,
                        `lastGeneratedMonth` TEXT NOT NULL
                    )
                """.trimIndent())
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `savings_goals` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `targetAmount` REAL NOT NULL,
                        `currentAmount` REAL NOT NULL,
                        `deadlineDate` TEXT NOT NULL,
                        `iconKey` TEXT NOT NULL,
                        `colorHex` INTEGER NOT NULL,
                        `note` TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "oikonomos_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
