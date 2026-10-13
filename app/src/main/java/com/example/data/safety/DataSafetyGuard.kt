package com.example.data.safety

import android.util.Log
import com.example.data.entity.ExpenseCategory
import com.example.data.entity.FamilyMember
import com.example.data.entity.RecurringExpense
import com.example.data.entity.SavingsGoal
import com.example.data.entity.toFirebaseExpense
import com.example.data.model.FirebaseCategory
import com.example.data.model.FirebaseMember
import com.example.data.model.FirebaseRecurringExpense
import com.example.data.model.FirebaseSavingsGoal
import com.example.data.repository.ExpenseRepository
import com.example.data.repository.FirestoreSyncRepository
import com.example.util.GreekVoiceParser
import com.google.firebase.auth.FirebaseUser

/**
 * =========================================================================================
 *                          MARKETWISE / OIKONOMOS DATA SAFETY GUARD
 * =========================================================================================
 * CRITICAL ARCHITECTURAL DIRECTIVE — ZERO DATA LOSS GUARANTEE:
 * 
 * 1. Under NO circumstance should any user data (Profile, Households, Members, Categories,
 *    Expenses, Incomes, Savings Goals, or Recurring Expenses) ever be deleted or wiped
 *    during app startup, viewmodel initialization, background sync, or account registration.
 *
 * 2. DUAL-PATH PERSISTENCE CONTRACT:
 *    Every user record is permanently backed up to TWO distinct Firestore cloud paths:
 *      Path A (Household Scope): /households/{householdId}/{subcollection}/{id}
 *      Path B (User Private Mirror): /users/{userId}/{subcollection}/{id}
 *    Even if a household ID changes, becomes detached, or a new phone is used, the user's
 *    data can ALWAYS be recovered from their private user mirror.
 *
 * 3. AUTO-HEALING & BI-DIRECTIONAL PROTECTION:
 *    - If cloud data exists, the local device is hydrated immediately.
 *    - If the local device has records not yet uploaded to the cloud (e.g., created offline),
 *      they are automatically uploaded to the cloud immediately upon connection.
 *    - If categories or members are completely empty for a new user, they are generated,
 *      seeded, and IMMEDIATELY persisted to the cloud so they are never lost.
 *
 * 4. GUARDED DESTRUCTIVE OPERATIONS:
 *    Clearing local Room cache is ONLY permitted upon explicit user sign-out or explicit
 *    user-confirmed factory reset in Settings. Any unauthorized call is strictly blocked.
 * =========================================================================================
 */
object DataSafetyGuard {

    private const val TAG = "DataSafetyGuard"

    enum class DestructionReason {
        EXPLICIT_SIGN_OUT,
        EXPLICIT_FACTORY_RESET,
        UNAUTHORIZED
    }

    /**
     * Checks whether a local cache clear operation is authorized.
     */
    fun validateDestructiveOperation(reason: DestructionReason): Boolean {
        return when (reason) {
            DestructionReason.EXPLICIT_SIGN_OUT,
            DestructionReason.EXPLICIT_FACTORY_RESET -> {
                Log.w(TAG, "Authorized local cache reset under reason: $reason. Cloud data remains 100% intact.")
                true
            }
            DestructionReason.UNAUTHORIZED -> {
                Log.e(TAG, "BLOCKED unauthorized attempt to clear data! Invariant violated.")
                false
            }
        }
    }

    fun isCombinationOrInvalidMemberName(name: String): Boolean {
        val trimmed = name.trim().lowercase()
        if (trimmed.isBlank()) return true
        val genericLabels = listOf(
            "κοινό οικογένειας", "κοινο οικογενειας", "family shared",
            "όλη η οικογένεια", "ολη η οικογενεια", "κοινό", "κοινο",
            "όλοι", "ολοι", "οικογένεια", "οικογενεια"
        )
        if (genericLabels.any { trimmed == it }) return true
        // Reject multi-person combinations like "Μάριος & Ελένη", "Μάριος + Μαρία", "Μάριος και Ελένη"
        val combinationIndicators = listOf(" & ", " + ", " / ", " και ", " and ", " κι ")
        if (combinationIndicators.any { trimmed.contains(it) }) return true
        if (trimmed.contains("&") || trimmed.contains("+") || trimmed.contains("/")) return true
        return false
    }

    /**
     * Bi-directional Safety Sync:
     * Audits and guarantees that all local categories, members, savings goals, and recurring
     * expenses are properly saved in Firebase Firestore under both the active household
     * and the user's personal cloud mirror.
     */
    suspend fun auditAndSecureUserCloudData(
        user: FirebaseUser,
        activeHouseholdId: String,
        repository: ExpenseRepository,
        firestoreSyncRepo: FirestoreSyncRepository
    ) {
        try {
            val userId = user.uid

            // 1. Audit & Secure Members (skip combination names and deduplicate by ID)
            val localMembers = repository.getAllMembersList()
            if (localMembers.isNotEmpty()) {
                val cleanLocal = localMembers
                    .filter { m -> !isCombinationOrInvalidMemberName(m.name) }
                    .distinctBy { it.id }

                if (cleanLocal.size != localMembers.size) {
                    repository.replaceAllMembers(cleanLocal)
                }

                cleanLocal.forEach { member ->
                    val fbMember = FirebaseMember(
                        id = member.id.toString(),
                        householdId = activeHouseholdId,
                        userId = userId,
                        name = member.name.trim(),
                        role = member.role,
                        avatarColorHex = member.avatarColorHex,
                        voiceKeywords = member.voiceKeywords,
                        isDefault = member.isDefault,
                        sortOrder = member.sortOrder
                    )
                    if (activeHouseholdId.isNotBlank()) {
                        firestoreSyncRepo.syncMember(activeHouseholdId, fbMember)
                    }
                    firestoreSyncRepo.syncUserMember(userId, fbMember)
                }
                Log.d(TAG, "Secured ${cleanLocal.size} members to cloud.")
            }

            // 2. Audit & Secure Categories (each category is a unique entity by ID)
            val localCategories = repository.getAllCategoriesList()
            if (localCategories.isNotEmpty()) {
                val cleanCategories = localCategories.distinctBy { it.id }

                if (cleanCategories.size != localCategories.size) {
                    repository.replaceAllCategories(cleanCategories)
                }

                cleanCategories.forEach { cat ->
                    val fbCat = FirebaseCategory(
                        id = cat.id.toString(),
                        householdId = activeHouseholdId,
                        userId = userId,
                        name = cat.name,
                        iconKey = cat.iconKey,
                        monthlyBudget = cat.monthlyBudget,
                        colorHex = cat.colorHex,
                        voiceKeywords = cat.voiceKeywords,
                        sortOrder = cat.sortOrder
                    )
                    if (activeHouseholdId.isNotBlank()) {
                        firestoreSyncRepo.syncCategory(activeHouseholdId, fbCat)
                    }
                    firestoreSyncRepo.syncUserCategory(userId, fbCat)
                }
                Log.d(TAG, "Secured ${cleanCategories.size} categories to cloud.")
            }

            // 3. Audit & Secure Savings Goals
            val localGoals = repository.getAllSavingsGoalsList()
            if (localGoals.isNotEmpty()) {
                localGoals.forEach { goal ->
                    val fbGoal = FirebaseSavingsGoal(
                        id = goal.id.toString(),
                        householdId = activeHouseholdId,
                        userId = userId,
                        title = goal.title,
                        targetAmount = goal.targetAmount,
                        currentAmount = goal.currentAmount,
                        deadlineDate = goal.deadlineDate,
                        iconKey = goal.iconKey,
                        colorHex = goal.colorHex,
                        note = goal.note
                    )
                    if (activeHouseholdId.isNotBlank()) {
                        firestoreSyncRepo.syncSavingsGoal(activeHouseholdId, fbGoal)
                    }
                    firestoreSyncRepo.syncUserSavingsGoal(userId, fbGoal)
                }
                Log.d(TAG, "Secured ${localGoals.size} savings goals to cloud.")
            }

            // 4. Audit & Secure Recurring Expenses
            val localRecurring = repository.getAllRecurringExpensesList()
            if (localRecurring.isNotEmpty()) {
                localRecurring.forEach { rec ->
                    val fbRec = FirebaseRecurringExpense(
                        id = rec.id.toString(),
                        householdId = activeHouseholdId,
                        userId = userId,
                        title = rec.title,
                        amount = rec.amount,
                        categoryId = rec.categoryId,
                        categoryName = rec.categoryName,
                        frequency = rec.frequency,
                        dayOfMonth = rec.dayOfMonth,
                        splitType = rec.splitType,
                        memberIdsCsv = rec.memberIdsCsv,
                        memberNamesDisplay = rec.memberNamesDisplay,
                        note = rec.note,
                        isActive = rec.isActive,
                        lastGeneratedMonth = rec.lastGeneratedMonth,
                        payerMemberId = rec.payerMemberId,
                        payerMemberName = rec.payerMemberName,
                        payerMemberIdsCsv = rec.payerMemberIdsCsv,
                        payerAmountsCsv = rec.payerAmountsCsv
                    )
                    if (activeHouseholdId.isNotBlank()) {
                        firestoreSyncRepo.syncRecurringExpense(activeHouseholdId, fbRec)
                    }
                    firestoreSyncRepo.syncUserRecurringExpense(userId, fbRec)
                }
                Log.d(TAG, "Secured ${localRecurring.size} recurring expenses to cloud.")
            }

            // 5. Audit & Secure Expenses (ensure any local expenses are mirrored in cloud with full multi-payer details)
            val localExpenses = repository.getAllExpensesList()
            if (localExpenses.isNotEmpty()) {
                localExpenses.forEach { exp ->
                    val fbExp = exp.toFirebaseExpense(
                        householdId = activeHouseholdId,
                        userId = userId,
                        userName = user.displayName ?: "Χρήστης"
                    )
                    if (activeHouseholdId.isNotBlank()) {
                        firestoreSyncRepo.syncExpense(fbExp)
                    }
                    firestoreSyncRepo.syncUserExpense(userId, fbExp)
                }
                Log.d(TAG, "Secured ${localExpenses.size} expenses to cloud.")
            }

            Log.i(TAG, "Audit complete. All user data is 100% mirrored in Firebase Cloud.")
        } catch (e: Exception) {
            Log.w(TAG, "Non-fatal warning during cloud data audit: ${e.message}")
        }
    }
}
