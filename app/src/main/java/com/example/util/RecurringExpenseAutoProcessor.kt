package com.example.util

import android.content.Context
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.entity.Expense
import com.example.data.entity.RecurringExpense
import com.example.data.repository.ExpenseRepository
import com.example.data.repository.FirestoreSyncRepository
import com.google.firebase.auth.FirebaseUser
import java.util.Calendar
import java.util.Locale

object RecurringExpenseAutoProcessor {
    private const val TAG = "RecurringAutoProcessor"

    fun calculateTargetTimestamp(year: Int, monthZeroIndexed: Int, dayOfMonth: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, monthZeroIndexed)
        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth.coerceIn(1, calendar.getActualMaximum(Calendar.DAY_OF_MONTH)))
        calendar.set(Calendar.HOUR_OF_DAY, 9)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun isAlreadyGeneratedForMonth(recurring: RecurringExpense, monthKey: String, monthExpenses: List<Expense>): Boolean {
        if (recurring.lastGeneratedMonth == monthKey) return true
        return monthExpenses.any { expense ->
            expense.title.equals(recurring.title, ignoreCase = true) &&
                    expense.categoryId == recurring.categoryId &&
                    Math.abs(expense.amount - recurring.amount) < 0.001
        }
    }

    suspend fun processDueRecurringExpenses(
        repository: ExpenseRepository,
        firestoreSyncRepo: FirestoreSyncRepository? = null,
        householdId: String = "",
        user: FirebaseUser? = null
    ): List<Expense> {
        val generatedList = mutableListOf<Expense>()
        try {
            val allRecurring = repository.getAllRecurringExpensesList()
            val activeRecurring = allRecurring.filter { it.isActive }
            if (activeRecurring.isEmpty()) return emptyList()

            val calendar = Calendar.getInstance()
            val currentYear = calendar.get(Calendar.YEAR)
            val currentMonth = calendar.get(Calendar.MONTH)
            val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
            val monthKey = String.format(Locale.US, "%04d-%02d", currentYear, currentMonth + 1)

            val allExpenses = repository.getAllExpensesList()

            for (recurring in activeRecurring) {
                if (currentDay >= recurring.dayOfMonth) {
                    if (!isAlreadyGeneratedForMonth(recurring, monthKey, allExpenses)) {
                        val expense = Expense(
                            title = recurring.title,
                            amount = recurring.amount,
                            categoryId = recurring.categoryId,
                            categoryName = recurring.categoryName,
                            timestamp = calculateTargetTimestamp(currentYear, currentMonth, recurring.dayOfMonth),
                            splitType = recurring.splitType,
                            memberIdsCsv = recurring.memberIdsCsv,
                            memberNamesDisplay = recurring.memberNamesDisplay,
                            splitAmountPerPerson = recurring.amount,
                            note = recurring.note.ifBlank { "Πάγια εντολή: ${recurring.title}" },
                            payerMemberId = recurring.payerMemberId,
                            payerMemberName = recurring.payerMemberName,
                            payerMemberIdsCsv = recurring.payerMemberIdsCsv,
                            payerAmountsCsv = recurring.payerAmountsCsv
                        )
                        val newId = repository.insertExpense(expense)
                        val insertedExpense = expense.copy(id = newId)
                        generatedList.add(insertedExpense)

                        val updatedRecurring = recurring.copy(lastGeneratedMonth = monthKey)
                        repository.updateRecurringExpense(updatedRecurring)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing recurring expenses: ${e.message}")
        }
        return generatedList
    }

    suspend fun processDueRecurringExpensesFromContext(context: Context): List<Expense> {
        val db = AppDatabase.getDatabase(context)
        val repository = ExpenseRepository(db.expenseDao(), context)
        return processDueRecurringExpenses(repository)
    }
}
