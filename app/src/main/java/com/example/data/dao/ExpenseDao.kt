package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.entity.Expense
import com.example.data.entity.ExpenseCategory
import com.example.data.entity.FamilyMember
import com.example.data.entity.Income
import com.example.data.entity.RecurringExpense
import com.example.data.entity.SavingsGoal
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    // Expenses
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC, id DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<Expense>)

    @Update
    suspend fun updateExpense(expense: Expense): Int

    @Transaction
    suspend fun saveOrUpdateExpense(expense: Expense): Long {
        if (expense.id > 0) {
            val updated = updateExpense(expense)
            if (updated > 0) return expense.id
        }
        if (expense.cloudId.isNotBlank()) {
            val byCloud = getExpenseByCloudId(expense.cloudId)
            if (byCloud != null) {
                val updated = updateExpense(expense.copy(id = byCloud.id))
                if (updated > 0) return byCloud.id
            }
        }
        return insertExpense(expense)
    }

    @Query("SELECT * FROM expenses WHERE cloudId = :cloudId LIMIT 1")
    suspend fun getExpenseByCloudId(cloudId: String): Expense?

    @Query("DELETE FROM expenses WHERE cloudId = :cloudId AND id != :keepId")
    suspend fun deleteDuplicateExpensesByCloudId(cloudId: String, keepId: Long)

    @Transaction
    suspend fun replaceAllExpensesTransaction(expenses: List<Expense>) {
        if (expenses.isEmpty()) return
        deleteAllExpenses()
        insertExpenses(expenses)
    }

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: Long)

    @Query("DELETE FROM expenses")
    suspend fun deleteAllExpenses()

    @Query("SELECT COUNT(*) FROM expenses")
    suspend fun getExpenseCount(): Int

    // Family Members
    @Query("SELECT * FROM family_members ORDER BY sortOrder ASC, id ASC")
    fun getAllMembers(): Flow<List<FamilyMember>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: FamilyMember): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<FamilyMember>)

    @Update
    suspend fun updateMember(member: FamilyMember)

    @Update
    suspend fun updateMembers(members: List<FamilyMember>)

    @Delete
    suspend fun deleteMember(member: FamilyMember)

    @Query("SELECT COUNT(*) FROM family_members")
    suspend fun getMemberCount(): Int

    @Query("DELETE FROM family_members")
    suspend fun deleteAllMembers()

    @Transaction
    suspend fun replaceAllMembersTransaction(members: List<FamilyMember>) {
        if (members.isEmpty()) return
        deleteAllMembers()
        insertMembers(members)
    }

    // Categories
    @Query("SELECT * FROM expense_categories ORDER BY sortOrder ASC, id ASC")
    fun getAllCategories(): Flow<List<ExpenseCategory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: ExpenseCategory): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<ExpenseCategory>)

    @Update
    suspend fun updateCategory(category: ExpenseCategory)

    @Update
    suspend fun updateCategories(categories: List<ExpenseCategory>)

    @Delete
    suspend fun deleteCategory(category: ExpenseCategory)

    @Query("SELECT COUNT(*) FROM expense_categories")
    suspend fun getCategoryCount(): Int

    @Query("DELETE FROM expense_categories")
    suspend fun deleteAllCategories()

    @Transaction
    suspend fun replaceAllCategoriesTransaction(categories: List<ExpenseCategory>) {
        if (categories.isEmpty()) return
        deleteAllCategories()
        insertCategories(categories)
    }

    // Recurring Expenses
    @Query("SELECT * FROM recurring_expenses ORDER BY id ASC")
    fun getAllRecurringExpenses(): Flow<List<RecurringExpense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringExpense(recurring: RecurringExpense): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringExpenses(recurringList: List<RecurringExpense>)

    @Update
    suspend fun updateRecurringExpense(recurring: RecurringExpense)

    @Delete
    suspend fun deleteRecurringExpense(recurring: RecurringExpense)

    @Query("DELETE FROM recurring_expenses")
    suspend fun deleteAllRecurringExpenses()

    @Query("SELECT COUNT(*) FROM recurring_expenses")
    suspend fun getRecurringCount(): Int

    // Savings Goals
    @Query("SELECT * FROM savings_goals ORDER BY id ASC")
    fun getAllSavingsGoals(): Flow<List<SavingsGoal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingsGoal(goal: SavingsGoal): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingsGoals(goals: List<SavingsGoal>)

    @Update
    suspend fun updateSavingsGoal(goal: SavingsGoal)

    @Delete
    suspend fun deleteSavingsGoal(goal: SavingsGoal)

    @Query("DELETE FROM savings_goals")
    suspend fun deleteAllSavingsGoals()

    @Query("SELECT COUNT(*) FROM savings_goals")
    suspend fun getSavingsGoalCount(): Int

    // Incomes
    @Query("SELECT * FROM incomes ORDER BY timestamp DESC")
    fun getAllIncomes(): Flow<List<Income>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncome(income: Income): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncomes(incomes: List<Income>)

    @Update
    suspend fun updateIncome(income: Income)

    @Delete
    suspend fun deleteIncome(income: Income)

    @Query("DELETE FROM incomes WHERE id = :id")
    suspend fun deleteIncomeById(id: Long)

    @Query("DELETE FROM incomes")
    suspend fun deleteAllIncomes()

    // Safety & Bi-directional Sync One-shot Getters
    @Query("SELECT * FROM expense_categories ORDER BY sortOrder ASC, id ASC")
    suspend fun getAllCategoriesList(): List<ExpenseCategory>

    @Query("SELECT * FROM family_members ORDER BY sortOrder ASC, id ASC")
    suspend fun getAllMembersList(): List<FamilyMember>

    @Query("SELECT * FROM savings_goals ORDER BY id ASC")
    suspend fun getAllSavingsGoalsList(): List<SavingsGoal>

    @Query("SELECT * FROM recurring_expenses ORDER BY id ASC")
    suspend fun getAllRecurringExpensesList(): List<RecurringExpense>

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    suspend fun getAllExpensesList(): List<Expense>

    @Query("SELECT * FROM incomes ORDER BY timestamp DESC")
    suspend fun getAllIncomesList(): List<Income>
}
