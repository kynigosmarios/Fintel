package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.FirebaseExpense
import com.example.data.model.FirebaseIncome
import com.example.data.model.Household
import com.example.data.model.UserProfile
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlin.random.Random

class FirestoreSyncRepository(
    private val db: FirebaseFirestore
) {
    companion object {
        private const val TAG = "FirestoreSyncRepo"

        fun create(context: Context): FirestoreSyncRepository {
            val databaseId = try {
                context.applicationContext.getString(R.string.firestore_database_id)
            } catch (e: Exception) {
                "(default)"
            }
            val firestoreInstance = if (databaseId.isNotBlank() && databaseId != "(default)") {
                try {
                    FirebaseFirestore.getInstance(databaseId)
                } catch (e: Exception) {
                    FirebaseFirestore.getInstance()
                }
            } else {
                FirebaseFirestore.getInstance()
            }
            // Ensure zero data stored on device flash memory (Memory Cache only)
            try {
                val memorySettings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(com.google.firebase.firestore.MemoryCacheSettings.newBuilder().build())
                    .build()
                firestoreInstance.firestoreSettings = memorySettings
            } catch (e: Exception) {
                Log.w(TAG, "Memory cache settings could not be set: ${e.message}")
            }
            return FirestoreSyncRepository(firestoreInstance)
        }
    }

    suspend fun getOrCreateUserProfile(uid: String, email: String, displayName: String): UserProfile {
        val userRef = db.collection("users").document(uid)
        val snapshot = userRef.get().await()

        return if (snapshot.exists()) {
            val existing = snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: UserProfile(uid = uid, email = email, displayName = displayName)

            if (existing.householdId.isBlank() || existing.householdIds.isEmpty()) {
                val newHousehold = createHousehold(uid, "Το Εξοδολόγιό μου")
                val updated = existing.copy(
                    householdId = newHousehold.id,
                    householdIds = listOf(newHousehold.id)
                )
                userRef.set(updated).await()
                updated
            } else {
                existing
            }
        } else {
            val householdId = UUID.randomUUID().toString().take(12)
            val inviteCode = "OIKO-" + Random.nextInt(1000, 9999).toString()
            val household = Household(
                id = householdId,
                name = "Το Εξοδολόγιό μου",
                ownerId = uid,
                inviteCode = inviteCode,
                memberUids = listOf(uid),
                createdAt = Timestamp.now()
            )
            db.collection("households").document(householdId).set(household).await()

            val newProfile = UserProfile(
                uid = uid,
                email = email,
                displayName = displayName.ifBlank { "Χρήστης OikoNomos" },
                householdId = householdId,
                householdIds = listOf(householdId),
                isPremium = false,
                createdAt = Timestamp.now()
            )
            userRef.set(newProfile).await()
            newProfile
        }
    }

    suspend fun createHousehold(ownerUid: String, exodologioName: String): Household {
        val householdId = UUID.randomUUID().toString().take(12)
        val inviteCode = "OIKO-" + Random.nextInt(1000, 9999).toString()

        val household = Household(
            id = householdId,
            name = exodologioName.ifBlank { "Το Εξοδολόγιό μου" },
            ownerId = ownerUid,
            inviteCode = inviteCode,
            memberUids = listOf(ownerUid),
            createdAt = Timestamp.now()
        )

        db.collection("households").document(householdId).set(household).await()

        // Update user profile with householdId and add to householdIds list
        val userRef = db.collection("users").document(ownerUid)
        userRef.update(
            "householdId", householdId,
            "householdIds", FieldValue.arrayUnion(householdId)
        ).await()

        return household
    }

    suspend fun fetchUserHouseholds(householdIds: List<String>): List<Household> {
        if (householdIds.isEmpty()) return emptyList()
        val list = mutableListOf<Household>()
        for (id in householdIds) {
            try {
                val doc = db.collection("households").document(id).get().await()
                if (doc.exists()) {
                    doc.toObject(Household::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.let {
                        list.add(it)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error fetching household $id", e)
            }
        }
        return list
    }

    suspend fun switchActiveHousehold(uid: String, householdId: String) {
        db.collection("users").document(uid).update("householdId", householdId).await()
    }

    suspend fun renameHousehold(householdId: String, newName: String) {
        if (householdId.isBlank() || newName.isBlank()) return
        db.collection("households").document(householdId).update("name", newName).await()
    }

    suspend fun updateUserProfileName(uid: String, newDisplayName: String) {
        if (uid.isBlank() || newDisplayName.isBlank()) return
        db.collection("users").document(uid).update("displayName", newDisplayName).await()
    }

    suspend fun setPremiumStatus(uid: String, isPremium: Boolean) {
        db.collection("users").document(uid).update("isPremium", isPremium).await()
    }

    suspend fun joinHouseholdByCode(userUid: String, inviteCode: String): Result<Household> {
        val normalizedCode = inviteCode.trim().uppercase()
        val querySnapshot = db.collection("households")
            .whereEqualTo("inviteCode", normalizedCode)
            .get()
            .await()

        if (querySnapshot.isEmpty) {
            return Result.failure(Exception("Δεν βρέθηκε εξοδολόγιο με τον κωδικό: $normalizedCode"))
        }

        val doc = querySnapshot.documents.first()
        val household = doc.toObject(Household::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            ?: return Result.failure(Exception("Σφάλμα ανάγνωσης στοιχείων εξοδολογίου"))

        // Add userUid to memberUids array
        doc.reference.update("memberUids", FieldValue.arrayUnion(userUid)).await()

        // Update user profile
        db.collection("users").document(userUid).update(
            "householdId", household.id,
            "householdIds", FieldValue.arrayUnion(household.id)
        ).await()

        return Result.success(household.copy(memberUids = (household.memberUids + userUid).distinct()))
    }

    fun observeHousehold(householdId: String): Flow<Household?> {
        if (householdId.isBlank()) return flowOf(null)

        return db.collection("households").document(householdId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(Household::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else null
            }
    }

    fun observeExpenses(householdId: String): Flow<List<FirebaseExpense>> {
        if (householdId.isBlank()) return flowOf(emptyList())

        return db.collection("households").document(householdId)
            .collection("expenses")
            .snapshots()
            .map { querySnapshot ->
                querySnapshot.toObjects(FirebaseExpense::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(
                        compareByDescending<FirebaseExpense> { it.timestamp }
                            .thenByDescending { it.createdAt?.toDate()?.time ?: 0L }
                            .thenByDescending { it.id }
                    )
            }
    }

    suspend fun syncExpense(expense: FirebaseExpense) {
        if (expense.householdId.isBlank()) return

        val docId = if (expense.id.isNotBlank()) expense.id else UUID.randomUUID().toString()
        val expenseToSave = expense.copy(id = docId, createdAt = Timestamp.now())

        db.collection("households").document(expense.householdId)
            .collection("expenses").document(docId)
            .set(expenseToSave)
            .await()
    }

    suspend fun deleteExpense(householdId: String, expenseId: String) {
        if (householdId.isBlank() || expenseId.isBlank()) return
        db.collection("households").document(householdId)
            .collection("expenses").document(expenseId)
            .delete()
            .await()
    }

    fun observeUserExpenses(userId: String): Flow<List<FirebaseExpense>> {
        if (userId.isBlank()) return flowOf(emptyList())

        return db.collection("users").document(userId)
            .collection("expenses")
            .snapshots()
            .map { querySnapshot ->
                querySnapshot.toObjects(FirebaseExpense::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(
                        compareByDescending<FirebaseExpense> { it.timestamp }
                            .thenByDescending { it.createdAt?.toDate()?.time ?: 0L }
                            .thenByDescending { it.id }
                    )
            }
    }

    suspend fun syncUserExpense(userId: String, expense: FirebaseExpense) {
        if (userId.isBlank()) return
        val docId = if (expense.id.isNotBlank()) expense.id else UUID.randomUUID().toString()
        val expenseToSave = expense.copy(id = docId, createdAt = Timestamp.now())

        db.collection("users").document(userId)
            .collection("expenses").document(docId)
            .set(expenseToSave)
            .await()
    }

    suspend fun deleteUserExpense(userId: String, expenseId: String) {
        if (userId.isBlank() || expenseId.isBlank()) return
        db.collection("users").document(userId)
            .collection("expenses").document(expenseId)
            .delete()
            .await()
    }

    suspend fun clearAllExpenses(householdId: String, userId: String) {
        try {
            if (householdId.isNotBlank()) {
                val docs = db.collection("households").document(householdId)
                    .collection("expenses").get().await()
                for (doc in docs.documents) {
                    doc.reference.delete().await()
                }
            }
            if (userId.isNotBlank()) {
                val docs = db.collection("users").document(userId)
                    .collection("expenses").get().await()
                for (doc in docs.documents) {
                    doc.reference.delete().await()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error clearing cloud expenses", e)
        }
    }

    // Incomes Cloud Sync
    fun observeIncomes(householdId: String): Flow<List<FirebaseIncome>> {
        if (householdId.isBlank()) return flowOf(emptyList())

        return db.collection("households").document(householdId)
            .collection("incomes")
            .snapshots()
            .map { querySnapshot ->
                querySnapshot.toObjects(FirebaseIncome::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(
                        compareByDescending<FirebaseIncome> { it.timestamp }
                            .thenByDescending { it.createdAt?.toDate()?.time ?: 0L }
                            .thenByDescending { it.id }
                    )
            }
    }

    suspend fun syncIncome(income: FirebaseIncome) {
        if (income.householdId.isBlank()) return

        val docId = if (income.id.isNotBlank()) income.id else UUID.randomUUID().toString()
        val incomeToSave = income.copy(id = docId, createdAt = Timestamp.now())

        db.collection("households").document(income.householdId)
            .collection("incomes").document(docId)
            .set(incomeToSave)
            .await()
    }

    suspend fun deleteIncome(householdId: String, incomeId: String) {
        if (householdId.isBlank() || incomeId.isBlank()) return
        db.collection("households").document(householdId)
            .collection("incomes").document(incomeId)
            .delete()
            .await()
    }

    fun observeUserIncomes(userId: String): Flow<List<FirebaseIncome>> {
        if (userId.isBlank()) return flowOf(emptyList())

        return db.collection("users").document(userId)
            .collection("incomes")
            .snapshots()
            .map { querySnapshot ->
                querySnapshot.toObjects(FirebaseIncome::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(
                        compareByDescending<FirebaseIncome> { it.timestamp }
                            .thenByDescending { it.createdAt?.toDate()?.time ?: 0L }
                            .thenByDescending { it.id }
                    )
            }
    }

    suspend fun syncUserIncome(userId: String, income: FirebaseIncome) {
        if (userId.isBlank()) return
        val docId = if (income.id.isNotBlank()) income.id else UUID.randomUUID().toString()
        val incomeToSave = income.copy(id = docId, createdAt = Timestamp.now())

        db.collection("users").document(userId)
            .collection("incomes").document(docId)
            .set(incomeToSave)
            .await()
    }

    suspend fun deleteUserIncome(userId: String, incomeId: String) {
        if (userId.isBlank() || incomeId.isBlank()) return
        db.collection("users").document(userId)
            .collection("incomes").document(incomeId)
            .delete()
            .await()
    }
}
