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
import com.example.data.entity.Income
import com.example.data.entity.RecurringExpense
import com.example.data.entity.SavingsGoal

@Database(
    entities = [Expense::class, FamilyMember::class, ExpenseCategory::class, RecurringExpense::class, SavingsGoal::class, Income::class],
    version = 9,
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

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `expenses` ADD COLUMN `cloudId` TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `incomes` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `cloudId` TEXT NOT NULL DEFAULT '',
                        `title` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `memberId` INTEGER NOT NULL,
                        `memberName` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `note` TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `family_members` ADD COLUMN `sortOrder` INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE `expense_categories` ADD COLUMN `sortOrder` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `expenses` ADD COLUMN `payerMemberId` INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE `expenses` ADD COLUMN `payerMemberName` TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE `recurring_expenses` ADD COLUMN `payerMemberId` INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE `recurring_expenses` ADD COLUMN `payerMemberName` TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `expenses` ADD COLUMN `payerMemberIdsCsv` TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE `expenses` ADD COLUMN `payerAmountsCsv` TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE `recurring_expenses` ADD COLUMN `payerMemberIdsCsv` TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE `recurring_expenses` ADD COLUMN `payerAmountsCsv` TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `expense_categories` ADD COLUMN `fontColorHex` INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                // If legacy oikonomos_database exists, safely copy over to fintel_database
                try {
                    val oldDb = context.applicationContext.getDatabasePath("oikonomos_database")
                    val newDb = context.applicationContext.getDatabasePath("fintel_database")
                    if (oldDb.exists() && !newDb.exists()) {
                        oldDb.copyTo(newDb, overwrite = true)
                        val oldWal = context.applicationContext.getDatabasePath("oikonomos_database-wal")
                        val newWal = context.applicationContext.getDatabasePath("fintel_database-wal")
                        if (oldWal.exists()) oldWal.copyTo(newWal, overwrite = true)
                        val oldShm = context.applicationContext.getDatabasePath("oikonomos_database-shm")
                        val newShm = context.applicationContext.getDatabasePath("fintel_database-shm")
                        if (oldShm.exists()) oldShm.copyTo(newShm, overwrite = true)
                    }
                } catch (_: Exception) {}

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fintel_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                    .fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
