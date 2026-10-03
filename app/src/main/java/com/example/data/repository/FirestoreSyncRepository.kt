package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.FirebaseExpense
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
            val databaseId = context.applicationContext.getString(R.string.firestore_database_id)
            val firestoreInstance = FirebaseFirestore.getInstance(databaseId)
            return FirestoreSyncRepository(firestoreInstance)
        }
    }

    suspend fun getOrCreateUserProfile(uid: String, email: String, displayName: String): UserProfile {
        val userRef = db.collection("users").document(uid)
        val snapshot = userRef.get().await()

        return if (snapshot.exists()) {
            snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: UserProfile(uid = uid, email = email, displayName = displayName)
        } else {
            val newProfile = UserProfile(
                uid = uid,
                email = email,
                displayName = displayName.ifBlank { "Χρήστης OikoNomos" },
                householdId = "",
                createdAt = Timestamp.now()
            )
            userRef.set(newProfile).await()
            newProfile
        }
    }

    suspend fun createHousehold(ownerUid: String, familyName: String): Household {
        val householdId = UUID.randomUUID().toString().take(12)
        val inviteCode = "OIKO-" + Random.nextInt(1000, 9999).toString()

        val household = Household(
            id = householdId,
            name = familyName.ifBlank { "Οικογένεια" },
            ownerId = ownerUid,
            inviteCode = inviteCode,
            memberUids = listOf(ownerUid),
            createdAt = Timestamp.now()
        )

        db.collection("households").document(householdId).set(household).await()

        // Update user profile with householdId
        db.collection("users").document(ownerUid).update("householdId", householdId).await()

        return household
    }

    suspend fun joinHouseholdByCode(userUid: String, inviteCode: String): Result<Household> {
        val normalizedCode = inviteCode.trim().uppercase()
        val querySnapshot = db.collection("households")
            .whereEqualTo("inviteCode", normalizedCode)
            .get()
            .await()

        if (querySnapshot.isEmpty) {
            return Result.failure(Exception("Δεν βρέθηκε νοικοκυριό με τον κωδικό: $normalizedCode"))
        }

        val doc = querySnapshot.documents.first()
        val household = doc.toObject(Household::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            ?: return Result.failure(Exception("Σφάλμα ανάγνωσης στοιχείων νοικοκυριού"))

        // Add userUid to memberUids array
        doc.reference.update("memberUids", FieldValue.arrayUnion(userUid)).await()

        // Update user profile
        db.collection("users").document(userUid).update("householdId", household.id).await()

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
                    .sortedByDescending { it.timestamp }
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
                    .sortedByDescending { it.timestamp }
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
}
