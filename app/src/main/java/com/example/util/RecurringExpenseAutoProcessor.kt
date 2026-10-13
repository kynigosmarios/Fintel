package com.example.util

import android.content.Context
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.entity.Expense
import com.example.data.entity.RecurringExpense
import com.example.data.entity.toFirebaseExpense
import com.example.data.model.FirebaseRecurringExpense
import com.example.data.repository.ExpenseRepository
import com.example.data.repository.FirestoreSyncRepository
import com.google.firebase.auth.FirebaseUser
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Automates the execution of standing orders (πάγιες εντολές).
 * Ensures that recurring expenses are automatically registered in the expense ledger
 * on the exact date specified, with the exact title, amount, category, payer, and notes.
 */
object RecurringExpenseAutoProcessor {

    private const val TAG = "RecurringAutoProcessor"

    /**
     * Calculates the exact timestamp for a recurring expense on the scheduled day of a given month/year.
     * The hour is set to 09:00:00 in the morning so it appears under that date properly.
     */
    fun calculateTargetTimestamp(year: Int, monthZeroIndexed: Int, dayOfMonth: Int): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroIndexed)
            val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
            val actualDay = dayOfMonth.coerceIn(1, maxDay)
            set(Calendar.DAY_OF_MONTH, actualDay)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    /**
     * Checks if a recurring expense has already been processed for this month.
     * Uses both the explicit lastGeneratedMonth key ("yyyy-MM") and verifies
     * whether a corresponding expense already exists in Room for this month to prevent duplicates.
     */
    fun isAlreadyGeneratedForMonth(
        recurring: RecurringExpense,
        monthKey: String,
        monthExpenses: List<Expense>
    ): Boolean {
        if (recurring.lastGeneratedMonth == monthKey) {
            return true
        }

        // Verification check: does an expense matching title, categoryId, amount already exist in this month?
        val alreadyExists = monthExpenses.any { exp ->
            exp.title.equals(recurring.title, ignoreCase = true) &&
            exp.categoryId == recurring.categoryId &&
            Math.abs(exp.amount - recurring.amount) < 0.001
        }
        return alreadyExists
    }

    /**
     * Evaluates all active standing orders and creates expenses for any that are due for the current month.
     *
     * @return List of newly created Expense objects.
     */
    suspend fun processDueRecurringExpenses(
        repository: ExpenseRepository,
        firestoreSyncRepo: FirestoreSyncRepository? = null,
        householdId: String = "",
        user: FirebaseUser? = null
    ): List<Expense> {
        return try {
            val recurringList = repository.getAllRecurringExpensesList()
            if (recurringList.isEmpty()) return emptyList()

            val calendar = Calendar.getInstance()
            val currentYear = calendar.get(Calendar.YEAR)
            val currentMonth = calendar.get(Calendar.MONTH) // 0-indexed
            val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
            val currentMonthKey = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())

            val monthStart = Calendar.getInstance().apply {
                set(Calendar.YEAR, currentYear)
                set(Calendar.MONTH, currentMonth)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val monthEnd = Calendar.getInstance().apply {
                set(Calendar.YEAR, currentYear)
                set(Calendar.MONTH, currentMonth)
                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis

            val allExpenses = repository.getAllExpensesList()
            val monthExpenses = allExpenses.filter { it.timestamp in monthStart..monthEnd }

            val newlyCreated = mutableListOf<Expense>()

            for (recurring in recurringList) {
                if (!recurring.isActive) continue

                // Check if the scheduled day of month has arrived or passed
                if (currentDay < recurring.dayOfMonth) {
                    continue
                }

                // Check if already executed for this month
                if (isAlreadyGeneratedForMonth(recurring, currentMonthKey, monthExpenses)) {
                    if (recurring.lastGeneratedMonth != currentMonthKey) {
                        val updated = recurring.copy(lastGeneratedMonth = currentMonthKey)
                        repository.updateRecurringExpense(updated)
                        syncRecurringToCloud(updated, firestoreSyncRepo, householdId, user)
                    }
                    continue
                }

                // Due! Create the Expense with the exact date, description, category, and amount!
                val targetTimestamp = calculateTargetTimestamp(currentYear, currentMonth, recurring.dayOfMonth)
                val noteText = if (recurring.note.isNotBlank()) {
                    recurring.note
                } else {
                    "Πάγια εντολή (${recurring.dayOfMonth}η του μήνα)"
                }

                val docId = "recurring_${recurring.id}_${currentMonthKey}"
                val newExpense = Expense(
                    cloudId = docId,
                    title = recurring.title,
                    amount = recurring.amount,
                    categoryId = recurring.categoryId,
                    categoryName = recurring.categoryName,
                    timestamp = targetTimestamp,
                    splitType = recurring.splitType,
                    memberIdsCsv = recurring.memberIdsCsv,
                    memberNamesDisplay = recurring.memberNamesDisplay,
                    note = noteText,
                    payerMemberId = recurring.payerMemberId,
                    payerMemberName = recurring.payerMemberName,
                    payerMemberIdsCsv = recurring.payerMemberIdsCsv,
                    payerAmountsCsv = recurring.payerAmountsCsv
                )

                val insertedId = repository.insertExpense(newExpense)
                val savedExpense = newExpense.copy(
                    id = insertedId,
                    cloudId = docId
                )
                newlyCreated.add(savedExpense)

                // Update recurring rule's lastGeneratedMonth
                val updatedRecurring = recurring.copy(lastGeneratedMonth = currentMonthKey)
                repository.updateRecurringExpense(updatedRecurring)

                // Sync to Firestore if user / household are available
                if (firestoreSyncRepo != null) {
                    val uid = user?.uid.orEmpty()
                    val uName = user?.displayName ?: "Χρήστης"
                    val fbExp = savedExpense.toFirebaseExpense(
                        householdId = householdId,
                        userId = uid,
                        userName = uName
                    )
                    if (uid.isNotBlank()) {
                        try {
                            firestoreSyncRepo.syncUserExpense(uid, fbExp)
                        } catch (e: Exception) {
                            Log.w(TAG, "Sync to user cloud failed: ${e.message}")
                        }
                    }
                    if (householdId.isNotBlank()) {
                        try {
                            firestoreSyncRepo.syncExpense(fbExp)
                        } catch (e: Exception) {
                            Log.w(TAG, "Sync to household cloud failed: ${e.message}")
                        }
                    }
                }
                syncRecurringToCloud(updatedRecurring, firestoreSyncRepo, householdId, user)
                Log.i(TAG, "Auto-registered standing order: «${recurring.title}» (${recurring.amount}€) for day ${recurring.dayOfMonth}/$currentMonthKey")
            }

            newlyCreated
        } catch (e: Exception) {
            Log.e(TAG, "Error in processDueRecurringExpenses: ${e.message}", e)
            emptyList()
        }
    }

    private suspend fun syncRecurringToCloud(
        recurring: RecurringExpense,
        firestoreSyncRepo: FirestoreSyncRepository?,
        householdId: String,
        user: FirebaseUser?
    ) {
        if (firestoreSyncRepo == null) return
        val uid = user?.uid.orEmpty()
        val fbRec = FirebaseRecurringExpense(
            id = recurring.id.toString(),
            householdId = householdId,
            userId = uid,
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
            lastGeneratedMonth = recurring.lastGeneratedMonth,
            payerMemberId = recurring.payerMemberId,
            payerMemberName = recurring.payerMemberName,
            payerMemberIdsCsv = recurring.payerMemberIdsCsv,
            payerAmountsCsv = recurring.payerAmountsCsv
        )
        if (householdId.isNotBlank()) {
            try {
                firestoreSyncRepo.syncRecurringExpense(householdId, fbRec)
            } catch (e: Exception) {
                Log.w(TAG, "Household recurring sync error: ${e.message}")
            }
        }
        if (uid.isNotBlank()) {
            try {
                firestoreSyncRepo.syncUserRecurringExpense(uid, fbRec)
            } catch (e: Exception) {
                Log.w(TAG, "User recurring sync error: ${e.message}")
            }
        }
    }

    /**
     * Executes check from application context (for receivers/background triggers).
     */
    suspend fun processDueRecurringExpensesFromContext(context: Context): List<Expense> {
        return try {
            val database = AppDatabase.getDatabase(context)
            val repository = ExpenseRepository(database.expenseDao(), context.applicationContext as android.app.Application)
            processDueRecurringExpenses(repository)
        } catch (e: Exception) {
            Log.e(TAG, "processDueRecurringExpensesFromContext failed: ${e.message}", e)
            emptyList()
        }
    }
}
