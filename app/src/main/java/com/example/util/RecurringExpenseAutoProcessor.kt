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
        if (recurring.lastGeneratedMonth.isNotBlank() && recurring.lastGeneratedMonth.startsWith(monthKey.take(7))) return true

        val normRecTitle = GreekVoiceParser.normalizeGreek(recurring.title.trim().lowercase())
        return monthExpenses.any { expense ->
            val normExpTitle = GreekVoiceParser.normalizeGreek(expense.title.trim().lowercase())
            val titleMatches = normExpTitle == normRecTitle ||
                    (normRecTitle.length >= 4 && normExpTitle.contains(normRecTitle)) ||
                    (normExpTitle.length >= 4 && normRecTitle.contains(normExpTitle)) ||
                    (normRecTitle.contains("ενοικιο") && normExpTitle.contains("ενοικιο")) ||
                    (normRecTitle.contains("αυτοκινητ") && normExpTitle.contains("αυτοκινητ")) ||
                    (normRecTitle.contains("netflix") && normExpTitle.contains("netflix")) ||
                    (normRecTitle.contains("κινητ") && normExpTitle.contains("κινητ"))

            val amountMatches = Math.abs(expense.amount - recurring.amount) < 2.0
            (titleMatches && (amountMatches || expense.categoryId == recurring.categoryId)) ||
                    (titleMatches && normExpTitle.isNotBlank())
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

            // Deduplicate active recurring by normalized title so duplicate records never trigger multiple inserts
            val uniqueRecurring = activeRecurring.distinctBy {
                GreekVoiceParser.normalizeGreek(it.title.trim().lowercase())
            }

            val calendar = Calendar.getInstance()
            val currentYear = calendar.get(Calendar.YEAR)
            val currentMonth = calendar.get(Calendar.MONTH)
            val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
            val monthKey = String.format(Locale.US, "%04d-%02d", currentYear, currentMonth + 1)

            val allExpenses = repository.getAllExpensesList()
            val currentMonthExpenses = allExpenses.filter { exp ->
                val cal = Calendar.getInstance().apply { timeInMillis = exp.timestamp }
                cal.get(Calendar.YEAR) == currentYear && cal.get(Calendar.MONTH) == currentMonth
            }.toMutableList()

            val processedRoots = mutableSetOf<String>()

            for (recurring in uniqueRecurring) {
                val normTitle = GreekVoiceParser.normalizeGreek(recurring.title.trim().lowercase())
                val rootKey = when {
                    normTitle.contains("ενοικιο") -> "ενοικιο"
                    normTitle.contains("αυτοκινητ") -> "αυτοκινητ"
                    normTitle.contains("netflix") -> "netflix"
                    normTitle.contains("κινητ") -> "κινητ"
                    else -> normTitle
                }

                if (processedRoots.contains(rootKey)) {
                    continue
                }

                if (currentDay >= recurring.dayOfMonth) {
                    val alreadyRecorded = isAlreadyGeneratedForMonth(recurring, monthKey, currentMonthExpenses)
                    if (alreadyRecorded) {
                        processedRoots.add(rootKey)
                        if (recurring.lastGeneratedMonth != monthKey) {
                            val updatedRecurring = recurring.copy(lastGeneratedMonth = monthKey)
                            repository.updateRecurringExpense(updatedRecurring)
                            if (householdId.isNotBlank() && firestoreSyncRepo != null) {
                                val fbRec = com.example.data.model.FirebaseRecurringExpense(
                                    id = if (recurring.id > 0) "rec_${recurring.id}" else recurring.title,
                                    householdId = householdId,
                                    userId = user?.uid.orEmpty(),
                                    title = recurring.title,
                                    amount = recurring.amount,
                                    categoryId = recurring.categoryId,
                                    categoryName = recurring.categoryName,
                                    frequency = recurring.frequency,
                                    dayOfMonth = recurring.dayOfMonth,
                                    splitType = recurring.splitType,
                                    memberIdsCsv = recurring.memberIdsCsv,
                                    memberNamesDisplay = recurring.memberNamesDisplay,
                                    note = recurring.note,
                                    isActive = recurring.isActive,
                                    lastGeneratedMonth = monthKey,
                                    payerMemberId = recurring.payerMemberId,
                                    payerMemberName = recurring.payerMemberName,
                                    payerMemberIdsCsv = recurring.payerMemberIdsCsv,
                                    payerAmountsCsv = recurring.payerAmountsCsv
                                )
                                firestoreSyncRepo.syncRecurringExpense(householdId, fbRec)
                            }
                        }
                    } else {
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
                        currentMonthExpenses.add(insertedExpense)
                        processedRoots.add(rootKey)

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
