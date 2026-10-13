package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.FirebaseCategory
import com.example.data.model.FirebaseExpense
import com.example.data.model.FirebaseIncome
import com.example.data.model.FirebaseMember
import com.example.data.model.FirebaseRecurringExpense
import com.example.data.model.FirebaseSavingsGoal
import com.example.data.model.Household
import com.example.data.model.UserProfile
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
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
            // Enable Persistent Disk Cache so expenses and balances load immediately from disk on app launch
            try {
                val diskCacheSettings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(
                        com.google.firebase.firestore.PersistentCacheSettings.newBuilder()
                            .setSizeBytes(com.google.firebase.firestore.FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                            .build()
                    )
                    .build()
                firestoreInstance.firestoreSettings = diskCacheSettings
            } catch (e: Exception) {
                Log.w(TAG, "Persistent cache settings could not be set: ${e.message}")
            }
            return FirestoreSyncRepository(firestoreInstance)
        }
    }

    suspend fun getOrCreateUserProfile(
        uid: String,
        email: String,
        displayName: String,
        firstName: String = "",
        lastName: String = "",
        birthDate: String = "",
        phoneNumber: String = "",
        occupation: String = "",
        city: String = "",
        authProvider: String = ""
    ): UserProfile {
        val userRef = db.collection("users").document(uid)
        val snapshot = userRef.get().await()

        return if (snapshot.exists()) {
            val existing = snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: UserProfile(uid = uid, email = email, displayName = displayName)

            // Respect whatever name was saved in Firestore first; only fallback to Auth displayName if blank
            val profileDisplayName = if (existing.displayName.isNotBlank()) existing.displayName else displayName.ifBlank { "Χρήστης Fintel" }
            val derivedFirst = if (existing.firstName.isNotBlank()) {
                existing.firstName
            } else if (firstName.isNotBlank()) {
                firstName
            } else {
                profileDisplayName.split(" ").firstOrNull() ?: ""
            }

            val derivedLast = if (existing.lastName.isNotBlank()) {
                existing.lastName
            } else if (lastName.isNotBlank()) {
                lastName
            } else {
                val parts = profileDisplayName.split(" ")
                if (parts.size > 1) parts.drop(1).joinToString(" ") else ""
            }

            val effectiveAuthProvider = if (authProvider.isNotBlank()) {
                authProvider
            } else if (existing.authProvider.isNotBlank()) {
                existing.authProvider
            } else "password"

            val merged = existing.copy(
                displayName = profileDisplayName,
                firstName = derivedFirst,
                lastName = derivedLast,
                birthDate = if (existing.birthDate.isNotBlank()) existing.birthDate else birthDate,
                phoneNumber = if (existing.phoneNumber.isNotBlank()) existing.phoneNumber else phoneNumber,
                occupation = if (existing.occupation.isNotBlank()) existing.occupation else occupation,
                city = if (existing.city.isNotBlank()) existing.city else city,
                authProvider = effectiveAuthProvider
            )

            if (merged.householdId.isBlank() || merged.householdIds.isEmpty()) {
                val newHousehold = createHousehold(uid, "Το Εξοδολόγιό μου")
                val updated = merged.copy(
                    householdId = newHousehold.id,
                    householdIds = listOf(newHousehold.id)
                )
                userRef.set(updated, SetOptions.merge()).await()
                updated
            } else {
                // Merge safely without overwriting other collections or existing fields
                userRef.set(merged, SetOptions.merge()).await()
                merged
            }
        } else {
            val householdId = UUID.randomUUID().toString().take(12)
            val inviteCode = "FINTEL-" + Random.nextInt(1000, 9999).toString()
            val household = Household(
                id = householdId,
                name = "Το Εξοδολόγιό μου",
                ownerId = uid,
                inviteCode = inviteCode,
                memberUids = listOf(uid),
                createdAt = Timestamp.now()
            )
            db.collection("households").document(householdId).set(household).await()

            val profileDisplayName = displayName.ifBlank {
                if (firstName.isNotBlank() || lastName.isNotBlank()) "$firstName $lastName".trim() else "Χρήστης Fintel"
            }
            val derivedFirst = if (firstName.isNotBlank()) firstName else profileDisplayName.split(" ").firstOrNull().orEmpty()
            val derivedLast = if (lastName.isNotBlank()) lastName else {
                val parts = profileDisplayName.split(" ")
                if (parts.size > 1) parts.drop(1).joinToString(" ") else ""
            }

            val newProfile = UserProfile(
                uid = uid,
                email = email,
                displayName = profileDisplayName,
                firstName = derivedFirst,
                lastName = derivedLast,
                birthDate = birthDate,
                phoneNumber = phoneNumber,
                occupation = occupation,
                city = city,
                householdId = householdId,
                householdIds = listOf(householdId),
                isPremium = false,
                authProvider = authProvider.ifBlank { "password" },
                createdAt = Timestamp.now()
            )
            userRef.set(newProfile).await()
            newProfile
        }
    }

    suspend fun updateUserAuthProvider(uid: String, provider: String) {
        if (uid.isBlank()) return
        db.collection("users").document(uid).set(mapOf("authProvider" to provider), SetOptions.merge()).await()
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
        db.collection("households").document(householdId).set(
            mapOf("name" to newName.trim(), "updatedAt" to Timestamp.now()),
            com.google.firebase.firestore.SetOptions.merge()
        ).await()
    }

    suspend fun updateUserProfileName(uid: String, newDisplayName: String) {
        if (uid.isBlank() || newDisplayName.isBlank()) return
        db.collection("users").document(uid).set(
            mapOf("displayName" to newDisplayName.trim()),
            SetOptions.merge()
        ).await()
    }

    suspend fun updateUserProfilePhoto(uid: String, photoUrl: String) {
        if (uid.isBlank()) return
        db.collection("users").document(uid).set(
            mapOf("photoUrl" to photoUrl),
            SetOptions.merge()
        ).await()
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        if (userId.isBlank()) return flowOf(null)
        return db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    val p = snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    if (p != null && p.uid.isBlank()) {
                        p.uid = snapshot.id
                    }
                    p
                } else null
            }
    }

    suspend fun updateUserFullProfile(
        uid: String,
        firstName: String,
        lastName: String,
        birthDate: String,
        phoneNumber: String,
        occupation: String,
        city: String
    ): Result<UserProfile> {
        if (uid.isBlank()) return Result.failure(Exception("Μη έγκυρο αναγνωριστικό χρήστη"))
        val fullName = "$firstName $lastName".trim().ifBlank { "Χρήστης Fintel" }
        val updates = mapOf(
            "displayName" to fullName,
            "firstName" to firstName.trim(),
            "lastName" to lastName.trim(),
            "birthDate" to birthDate.trim(),
            "phoneNumber" to phoneNumber.trim(),
            "occupation" to occupation.trim(),
            "city" to city.trim(),
            "updatedAt" to Timestamp.now()
        )
        // Use SetOptions.merge() so document is created or merged without NOT_FOUND exception
        db.collection("users").document(uid).set(updates, SetOptions.merge()).await()
        val doc = db.collection("users").document(uid).get().await()
        val updatedFromDoc = doc.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        val finalProfile = (updatedFromDoc ?: UserProfile(uid = uid)).copy(
            uid = uid,
            displayName = if (updatedFromDoc?.displayName.isNullOrBlank()) fullName else updatedFromDoc!!.displayName,
            firstName = if (updatedFromDoc?.firstName.isNullOrBlank()) firstName.trim() else updatedFromDoc!!.firstName,
            lastName = if (updatedFromDoc?.lastName.isNullOrBlank()) lastName.trim() else updatedFromDoc!!.lastName,
            birthDate = if (updatedFromDoc?.birthDate.isNullOrBlank()) birthDate.trim() else updatedFromDoc!!.birthDate,
            phoneNumber = if (updatedFromDoc?.phoneNumber.isNullOrBlank()) phoneNumber.trim() else updatedFromDoc!!.phoneNumber,
            occupation = if (updatedFromDoc?.occupation.isNullOrBlank()) occupation.trim() else updatedFromDoc!!.occupation,
            city = if (updatedFromDoc?.city.isNullOrBlank()) city.trim() else updatedFromDoc!!.city
        )
        return Result.success(finalProfile)
    }

    suspend fun findUserByPhoneNumber(phone: String): UserProfile? {
        val raw = phone.trim().replace(" ", "").replace("-", "").replace("(", "").replace(")", "")
        if (raw.isBlank()) return null
        
        val digitsOnly = when {
            raw.startsWith("0030") -> raw.removePrefix("0030")
            raw.startsWith("+30") -> raw.removePrefix("+30")
            else -> raw
        }

        // Try direct lookups with variations
        val variations = listOf(raw, digitsOnly, "+30$digitsOnly", "0030$digitsOnly").distinct()
        for (v in variations) {
            val query = db.collection("users").whereEqualTo("phoneNumber", v).get().await()
            if (!query.isEmpty) {
                val profile = query.documents.first().toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                if (profile != null) return profile
            }
        }

        // Fallback: Scan users collection to match normalized phone numbers
        try {
            val allUsers = db.collection("users").limit(50).get().await()
            for (doc in allUsers.documents) {
                val u = doc.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: continue
                val uDigits = u.phoneNumber.trim().replace(" ", "").replace("-", "").replace("(", "").replace(")", "")
                    .let { if (it.startsWith("+30")) it.removePrefix("+30") else if (it.startsWith("0030")) it.removePrefix("0030") else it }
                if (uDigits.isNotBlank() && (uDigits == digitsOnly || u.phoneNumber == raw)) {
                    return u
                }
            }
        } catch (_: Exception) {}

        return null
    }

    suspend fun findUserByEmail(email: String): UserProfile? {
        val clean = email.trim()
        if (clean.isBlank()) return null
        val q1 = db.collection("users").whereEqualTo("email", clean).get().await()
        if (!q1.isEmpty) {
            return q1.documents.first().toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        val lower = clean.lowercase()
        if (lower != clean) {
            val q2 = db.collection("users").whereEqualTo("email", lower).get().await()
            if (!q2.isEmpty) {
                return q2.documents.first().toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
        }
        return null
    }

    suspend fun savePasswordResetPhoneCode(phone: String, code: String, userEmail: String) {
        val cleanPhone = phone.trim().replace(" ", "").replace("-", "")
        val data = mapOf(
            "phone" to cleanPhone,
            "code" to code,
            "userEmail" to userEmail,
            "createdAt" to Timestamp.now()
        )
        db.collection("phone_password_resets").document(cleanPhone).set(data).await()
    }

    suspend fun getPasswordResetPhoneCode(phone: String): Pair<String, String>? {
        val cleanPhone = phone.trim().replace(" ", "").replace("-", "")
        val doc = db.collection("phone_password_resets").document(cleanPhone).get().await()
        if (!doc.exists()) return null
        val code = doc.getString("code") ?: return null
        val email = doc.getString("userEmail") ?: return null
        return Pair(code, email)
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
                querySnapshot.documents.mapNotNull { doc ->
                    val obj = doc.toObject(FirebaseExpense::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    if (obj != null && obj.id.isBlank()) {
                        obj.id = doc.id
                    }
                    obj
                }.sortedWith(
                    compareByDescending<FirebaseExpense> { it.timestamp }
                        .thenByDescending { it.createdAt?.toDate()?.time ?: 0L }
                        .thenByDescending { it.id }
                )
            }
    }

    suspend fun syncExpense(expense: FirebaseExpense) {
        if (expense.householdId.isBlank()) return

        val docId = if (expense.id.isNotBlank()) expense.id else UUID.randomUUID().toString()
        val expenseToSave = expense.copy(id = docId, createdAt = expense.createdAt ?: Timestamp.now())

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
                querySnapshot.documents.mapNotNull { doc ->
                    val obj = doc.toObject(FirebaseExpense::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    if (obj != null && obj.id.isBlank()) {
                        obj.id = doc.id
                    }
                    obj
                }.sortedWith(
                    compareByDescending<FirebaseExpense> { it.timestamp }
                        .thenByDescending { it.createdAt?.toDate()?.time ?: 0L }
                        .thenByDescending { it.id }
                )
            }
    }

    suspend fun syncUserExpense(userId: String, expense: FirebaseExpense) {
        if (userId.isBlank()) return
        val docId = if (expense.id.isNotBlank()) expense.id else UUID.randomUUID().toString()
        val expenseToSave = expense.copy(id = docId, createdAt = expense.createdAt ?: Timestamp.now())

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
                querySnapshot.documents.mapNotNull { doc ->
                    val obj = doc.toObject(FirebaseIncome::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    if (obj != null && obj.id.isBlank()) {
                        obj.id = doc.id
                    }
                    obj
                }.sortedWith(
                    compareByDescending<FirebaseIncome> { it.timestamp }
                        .thenByDescending { it.createdAt?.toDate()?.time ?: 0L }
                        .thenByDescending { it.id }
                )
            }
    }

    suspend fun syncIncome(income: FirebaseIncome) {
        if (income.householdId.isBlank()) return

        val docId = if (income.id.isNotBlank()) income.id else UUID.randomUUID().toString()
        val incomeToSave = income.copy(id = docId, createdAt = income.createdAt ?: Timestamp.now())

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
                querySnapshot.documents.mapNotNull { doc ->
                    val obj = doc.toObject(FirebaseIncome::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    if (obj != null && obj.id.isBlank()) {
                        obj.id = doc.id
                    }
                    obj
                }.sortedWith(
                    compareByDescending<FirebaseIncome> { it.timestamp }
                        .thenByDescending { it.createdAt?.toDate()?.time ?: 0L }
                        .thenByDescending { it.id }
                )
            }
    }

    suspend fun syncUserIncome(userId: String, income: FirebaseIncome) {
        if (userId.isBlank()) return
        val docId = if (income.id.isNotBlank()) income.id else UUID.randomUUID().toString()
        val incomeToSave = income.copy(id = docId, createdAt = income.createdAt ?: Timestamp.now())

        db.collection("users").document(userId)
            .collection("incomes").document(docId)
            .set(incomeToSave)
            .await()
    }

    suspend fun enableNetwork() {
        try {
            db.enableNetwork().await()
        } catch (e: Exception) {
            Log.w(TAG, "enableNetwork warning: ${e.message}")
        }
    }

    suspend fun getExpensesFromCache(householdId: String): List<FirebaseExpense> {
        if (householdId.isBlank()) return emptyList()
        return try {
            val snapshot = db.collection("households").document(householdId)
                .collection("expenses")
                .get(com.google.firebase.firestore.Source.CACHE)
                .await()
            snapshot.documents.mapNotNull { doc ->
                val obj = doc.toObject(FirebaseExpense::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                if (obj != null && obj.id.isBlank()) {
                    obj.id = doc.id
                }
                obj
            }.sortedWith(
                compareByDescending<FirebaseExpense> { it.timestamp }
                    .thenByDescending { it.createdAt?.toDate()?.time ?: 0L }
                    .thenByDescending { it.id }
            )
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getIncomesFromCache(householdId: String): List<FirebaseIncome> {
        if (householdId.isBlank()) return emptyList()
        return try {
            val snapshot = db.collection("households").document(householdId)
                .collection("incomes")
                .get(com.google.firebase.firestore.Source.CACHE)
                .await()
            snapshot.documents.mapNotNull { doc ->
                val obj = doc.toObject(FirebaseIncome::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                if (obj != null && obj.id.isBlank()) {
                    obj.id = doc.id
                }
                obj
            }.sortedWith(
                compareByDescending<FirebaseIncome> { it.timestamp }
                    .thenByDescending { it.createdAt?.toDate()?.time ?: 0L }
                    .thenByDescending { it.id }
            )
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getExpensesOnce(householdId: String): List<FirebaseExpense> {
        if (householdId.isBlank()) return emptyList()
        return try {
            val snapshot = db.collection("households").document(householdId)
                .collection("expenses")
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                val obj = doc.toObject(FirebaseExpense::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                if (obj != null && obj.id.isBlank()) {
                    obj.id = doc.id
                }
                obj
            }.sortedWith(
                compareByDescending<FirebaseExpense> { it.timestamp }
                    .thenByDescending { it.createdAt?.toDate()?.time ?: 0L }
                    .thenByDescending { it.id }
            )
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching expenses once", e)
            emptyList()
        }
    }

    suspend fun getIncomesOnce(householdId: String): List<FirebaseIncome> {
        if (householdId.isBlank()) return emptyList()
        return try {
            val snapshot = db.collection("households").document(householdId)
                .collection("incomes")
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                val obj = doc.toObject(FirebaseIncome::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                if (obj != null && obj.id.isBlank()) {
                    obj.id = doc.id
                }
                obj
            }.sortedWith(
                compareByDescending<FirebaseIncome> { it.timestamp }
                    .thenByDescending { it.createdAt?.toDate()?.time ?: 0L }
                    .thenByDescending { it.id }
            )
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching incomes once", e)
            emptyList()
        }
    }

    suspend fun getUserExpensesOnce(userId: String): List<FirebaseExpense> {
        if (userId.isBlank()) return emptyList()
        return try {
            val snapshot = db.collection("users").document(userId)
                .collection("expenses")
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                val obj = doc.toObject(FirebaseExpense::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                if (obj != null && obj.id.isBlank()) {
                    obj.id = doc.id
                }
                obj
            }.sortedWith(
                compareByDescending<FirebaseExpense> { it.timestamp }
                    .thenByDescending { it.createdAt?.toDate()?.time ?: 0L }
                    .thenByDescending { it.id }
            )
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching user expenses once", e)
            emptyList()
        }
    }

    suspend fun getUserIncomesOnce(userId: String): List<FirebaseIncome> {
        if (userId.isBlank()) return emptyList()
        return try {
            val snapshot = db.collection("users").document(userId)
                .collection("incomes")
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                val obj = doc.toObject(FirebaseIncome::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                if (obj != null && obj.id.isBlank()) {
                    obj.id = doc.id
                }
                obj
            }.sortedWith(
                compareByDescending<FirebaseIncome> { it.timestamp }
                    .thenByDescending { it.createdAt?.toDate()?.time ?: 0L }
                    .thenByDescending { it.id }
            )
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching user incomes once", e)
            emptyList()
        }
    }

    suspend fun deleteUserIncome(userId: String, incomeId: String) {
        if (userId.isBlank() || incomeId.isBlank()) return
        db.collection("users").document(userId)
            .collection("incomes").document(incomeId)
            .delete()
            .await()
    }

    // Household Members Sync
    fun observeMembers(householdId: String): Flow<List<FirebaseMember>> {
        if (householdId.isBlank()) return flowOf(emptyList())
        return db.collection("households").document(householdId)
            .collection("members")
            .snapshots()
            .map { querySnapshot ->
                querySnapshot.toObjects(FirebaseMember::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(compareBy<FirebaseMember> { it.sortOrder }.thenBy { it.id })
            }
    }

    suspend fun syncMember(householdId: String, member: FirebaseMember) {
        if (householdId.isBlank()) return
        val docId = if (member.id.isNotBlank()) member.id else UUID.randomUUID().toString()
        val toSave = member.copy(id = docId, householdId = householdId, createdAt = Timestamp.now())
        db.collection("households").document(householdId)
            .collection("members").document(docId)
            .set(toSave)
            .await()
    }

    suspend fun deleteMember(householdId: String, memberId: String) {
        if (householdId.isBlank() || memberId.isBlank()) return
        db.collection("households").document(householdId)
            .collection("members").document(memberId)
            .delete()
            .await()
    }

    // Household Categories Sync
    fun observeCategories(householdId: String): Flow<List<FirebaseCategory>> {
        if (householdId.isBlank()) return flowOf(emptyList())
        return db.collection("households").document(householdId)
            .collection("categories")
            .snapshots()
            .map { querySnapshot ->
                querySnapshot.toObjects(FirebaseCategory::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(compareBy<FirebaseCategory> { it.sortOrder }.thenBy { it.id })
            }
    }

    suspend fun syncCategory(householdId: String, category: FirebaseCategory) {
        if (householdId.isBlank()) return
        val docId = if (category.id.isNotBlank()) category.id else UUID.randomUUID().toString()
        val toSave = category.copy(id = docId, householdId = householdId, createdAt = Timestamp.now())
        db.collection("households").document(householdId)
            .collection("categories").document(docId)
            .set(toSave)
            .await()
    }

    suspend fun deleteCategory(householdId: String, categoryId: String) {
        if (householdId.isBlank() || categoryId.isBlank()) return
        db.collection("households").document(householdId)
            .collection("categories").document(categoryId)
            .delete()
            .await()
    }

    // Direct / One-time fetches for Members
    suspend fun getMembersOnce(householdId: String): List<FirebaseMember> {
        if (householdId.isBlank()) return emptyList()
        return try {
            val snapshot = db.collection("households").document(householdId)
                .collection("members")
                .get()
                .await()
            snapshot.toObjects(FirebaseMember::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .sortedWith(compareBy<FirebaseMember> { it.sortOrder }.thenBy { it.id })
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getUserMembersOnce(userId: String): List<FirebaseMember> {
        if (userId.isBlank()) return emptyList()
        return try {
            val snapshot = db.collection("users").document(userId)
                .collection("members")
                .get()
                .await()
            snapshot.toObjects(FirebaseMember::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .sortedWith(compareBy<FirebaseMember> { it.sortOrder }.thenBy { it.id })
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun observeUserMembers(userId: String): Flow<List<FirebaseMember>> {
        if (userId.isBlank()) return flowOf(emptyList())
        return db.collection("users").document(userId)
            .collection("members")
            .snapshots()
            .map { querySnapshot ->
                querySnapshot.toObjects(FirebaseMember::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(compareBy<FirebaseMember> { it.sortOrder }.thenBy { it.id })
            }
    }

    suspend fun syncUserMember(userId: String, member: FirebaseMember) {
        if (userId.isBlank()) return
        val docId = if (member.id.isNotBlank()) member.id else UUID.randomUUID().toString()
        val toSave = member.copy(id = docId, userId = userId, createdAt = Timestamp.now())
        db.collection("users").document(userId)
            .collection("members").document(docId)
            .set(toSave)
            .await()
    }

    suspend fun deleteUserMember(userId: String, memberId: String) {
        if (userId.isBlank() || memberId.isBlank()) return
        db.collection("users").document(userId)
            .collection("members").document(memberId)
            .delete()
            .await()
    }

    // Direct / One-time fetches for Categories
    suspend fun getCategoriesOnce(householdId: String): List<FirebaseCategory> {
        if (householdId.isBlank()) return emptyList()
        return try {
            val snapshot = db.collection("households").document(householdId)
                .collection("categories")
                .get()
                .await()
            snapshot.toObjects(FirebaseCategory::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .sortedWith(compareBy<FirebaseCategory> { it.sortOrder }.thenBy { it.id })
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getUserCategoriesOnce(userId: String): List<FirebaseCategory> {
        if (userId.isBlank()) return emptyList()
        return try {
            val snapshot = db.collection("users").document(userId)
                .collection("categories")
                .get()
                .await()
            snapshot.toObjects(FirebaseCategory::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .sortedWith(compareBy<FirebaseCategory> { it.sortOrder }.thenBy { it.id })
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun observeUserCategories(userId: String): Flow<List<FirebaseCategory>> {
        if (userId.isBlank()) return flowOf(emptyList())
        return db.collection("users").document(userId)
            .collection("categories")
            .snapshots()
            .map { querySnapshot ->
                querySnapshot.toObjects(FirebaseCategory::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(compareBy<FirebaseCategory> { it.sortOrder }.thenBy { it.id })
            }
    }

    suspend fun syncUserCategory(userId: String, category: FirebaseCategory) {
        if (userId.isBlank()) return
        val docId = if (category.id.isNotBlank()) category.id else UUID.randomUUID().toString()
        val toSave = category.copy(id = docId, userId = userId, createdAt = Timestamp.now())
        db.collection("users").document(userId)
            .collection("categories").document(docId)
            .set(toSave)
            .await()
    }

    suspend fun deleteUserCategory(userId: String, categoryId: String) {
        if (userId.isBlank() || categoryId.isBlank()) return
        db.collection("users").document(userId)
            .collection("categories").document(categoryId)
            .delete()
            .await()
    }

    // Savings Goals Cloud Sync
    fun observeSavingsGoals(householdId: String): Flow<List<FirebaseSavingsGoal>> {
        if (householdId.isBlank()) return flowOf(emptyList())
        return db.collection("households").document(householdId)
            .collection("savings_goals")
            .snapshots()
            .map { querySnapshot ->
                querySnapshot.toObjects(FirebaseSavingsGoal::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.title }
            }
    }

    fun observeUserSavingsGoals(userId: String): Flow<List<FirebaseSavingsGoal>> {
        if (userId.isBlank()) return flowOf(emptyList())
        return db.collection("users").document(userId)
            .collection("savings_goals")
            .snapshots()
            .map { querySnapshot ->
                querySnapshot.toObjects(FirebaseSavingsGoal::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.title }
            }
    }

    suspend fun getSavingsGoalsOnce(householdId: String): List<FirebaseSavingsGoal> {
        if (householdId.isBlank()) return emptyList()
        return try {
            val snapshot = db.collection("households").document(householdId)
                .collection("savings_goals")
                .get()
                .await()
            val list = snapshot.toObjects(FirebaseSavingsGoal::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            if (list.isNotEmpty()) list else {
                val alt = db.collection("households").document(householdId)
                    .collection("savingsGoals").get().await()
                alt.toObjects(FirebaseSavingsGoal::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getUserSavingsGoalsOnce(userId: String): List<FirebaseSavingsGoal> {
        if (userId.isBlank()) return emptyList()
        return try {
            val snapshot = db.collection("users").document(userId)
                .collection("savings_goals")
                .get()
                .await()
            val list = snapshot.toObjects(FirebaseSavingsGoal::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            if (list.isNotEmpty()) list else {
                val alt = db.collection("users").document(userId)
                    .collection("savingsGoals").get().await()
                alt.toObjects(FirebaseSavingsGoal::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun syncSavingsGoal(householdId: String, goal: FirebaseSavingsGoal) {
        if (householdId.isBlank()) return
        val docId = if (goal.id.isNotBlank()) goal.id else UUID.randomUUID().toString()
        val toSave = goal.copy(id = docId, householdId = householdId, createdAt = Timestamp.now())
        db.collection("households").document(householdId)
            .collection("savings_goals").document(docId)
            .set(toSave)
            .await()
    }

    suspend fun syncUserSavingsGoal(userId: String, goal: FirebaseSavingsGoal) {
        if (userId.isBlank()) return
        val docId = if (goal.id.isNotBlank()) goal.id else UUID.randomUUID().toString()
        val toSave = goal.copy(id = docId, userId = userId, createdAt = Timestamp.now())
        db.collection("users").document(userId)
            .collection("savings_goals").document(docId)
            .set(toSave)
            .await()
    }

    suspend fun deleteSavingsGoal(householdId: String, goalId: String) {
        if (householdId.isBlank() || goalId.isBlank()) return
        db.collection("households").document(householdId)
            .collection("savings_goals").document(goalId)
            .delete()
            .await()
    }

    suspend fun deleteUserSavingsGoal(userId: String, goalId: String) {
        if (userId.isBlank() || goalId.isBlank()) return
        db.collection("users").document(userId)
            .collection("savings_goals").document(goalId)
            .delete()
            .await()
    }

    // Recurring Expenses Cloud Sync
    fun observeRecurringExpenses(householdId: String): Flow<List<FirebaseRecurringExpense>> {
        if (householdId.isBlank()) return flowOf(emptyList())
        return db.collection("households").document(householdId)
            .collection("recurring_expenses")
            .snapshots()
            .map { querySnapshot ->
                querySnapshot.toObjects(FirebaseRecurringExpense::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.title }
            }
    }

    fun observeUserRecurringExpenses(userId: String): Flow<List<FirebaseRecurringExpense>> {
        if (userId.isBlank()) return flowOf(emptyList())
        return db.collection("users").document(userId)
            .collection("recurring_expenses")
            .snapshots()
            .map { querySnapshot ->
                querySnapshot.toObjects(FirebaseRecurringExpense::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.title }
            }
    }

    suspend fun getRecurringExpensesOnce(householdId: String): List<FirebaseRecurringExpense> {
        if (householdId.isBlank()) return emptyList()
        return try {
            val snapshot = db.collection("households").document(householdId)
                .collection("recurring_expenses")
                .get()
                .await()
            val list = snapshot.toObjects(FirebaseRecurringExpense::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            if (list.isNotEmpty()) list else {
                val alt = db.collection("households").document(householdId)
                    .collection("recurringExpenses").get().await()
                alt.toObjects(FirebaseRecurringExpense::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getUserRecurringExpensesOnce(userId: String): List<FirebaseRecurringExpense> {
        if (userId.isBlank()) return emptyList()
        return try {
            val snapshot = db.collection("users").document(userId)
                .collection("recurring_expenses")
                .get()
                .await()
            val list = snapshot.toObjects(FirebaseRecurringExpense::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            if (list.isNotEmpty()) list else {
                val alt = db.collection("users").document(userId)
                    .collection("recurringExpenses").get().await()
                alt.toObjects(FirebaseRecurringExpense::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun syncRecurringExpense(householdId: String, recurring: FirebaseRecurringExpense) {
        if (householdId.isBlank()) return
        val docId = if (recurring.id.isNotBlank()) recurring.id else UUID.randomUUID().toString()
        val toSave = recurring.copy(id = docId, householdId = householdId, createdAt = Timestamp.now())
        db.collection("households").document(householdId)
            .collection("recurring_expenses").document(docId)
            .set(toSave)
            .await()
    }

    suspend fun syncUserRecurringExpense(userId: String, recurring: FirebaseRecurringExpense) {
        if (userId.isBlank()) return
        val docId = if (recurring.id.isNotBlank()) recurring.id else UUID.randomUUID().toString()
        val toSave = recurring.copy(id = docId, userId = userId, createdAt = Timestamp.now())
        db.collection("users").document(userId)
            .collection("recurring_expenses").document(docId)
            .set(toSave)
            .await()
    }

    suspend fun deleteRecurringExpense(householdId: String, recurringId: String) {
        if (householdId.isBlank() || recurringId.isBlank()) return
        db.collection("households").document(householdId)
            .collection("recurring_expenses").document(recurringId)
            .delete()
            .await()
    }

    suspend fun deleteUserRecurringExpense(userId: String, recurringId: String) {
        if (userId.isBlank() || recurringId.isBlank()) return
        db.collection("users").document(userId)
            .collection("recurring_expenses").document(recurringId)
            .delete()
            .await()
    }

    // Cross-Household & Cross-Scope helper scans to recover data across any household associated with the user
    suspend fun getAllMembersAcrossAllScopes(householdIds: List<String>, userId: String): List<FirebaseMember> {
        val result = mutableListOf<FirebaseMember>()
        val distinctHids = (householdIds + listOf(userId)).filter { it.isNotBlank() }.distinct()
        for (hId in householdIds.filter { it.isNotBlank() }.distinct()) {
            val members = getMembersOnce(hId)
            result.addAll(members)
        }
        if (userId.isNotBlank()) {
            val userMembers = getUserMembersOnce(userId)
            result.addAll(userMembers)
        }
        return result
    }

    suspend fun syncAndCleanMember(
        householdIds: List<String>,
        userId: String,
        member: FirebaseMember,
        oldName: String = ""
    ) {
        val canonicalDocId = member.id.ifBlank { "1" }
        val toSave = member.copy(id = canonicalDocId, createdAt = Timestamp.now())

        for (hId in householdIds.filter { it.isNotBlank() }.distinct()) {
            try {
                db.collection("households").document(hId)
                    .collection("members").document(canonicalDocId)
                    .set(toSave.copy(householdId = hId))
                    .await()
            } catch (e: Exception) {
                Log.w("FirestoreSyncRepo", "Error syncing member to household $hId: ${e.message}")
            }
        }
        if (userId.isNotBlank()) {
            try {
                db.collection("users").document(userId)
                    .collection("members").document(canonicalDocId)
                    .set(toSave.copy(userId = userId))
                    .await()
            } catch (e: Exception) {
                Log.w("FirestoreSyncRepo", "Error syncing member to user $userId: ${e.message}")
            }
        }

        // Purge old stale documents with old name or duplicate doc IDs
        val namesToPurge = mutableListOf<String>()
        if (oldName.isNotBlank() && !oldName.equals(member.name, ignoreCase = true)) {
            namesToPurge.add(oldName.trim().lowercase())
        }

        for (hId in householdIds.filter { it.isNotBlank() }.distinct()) {
            try {
                val snapshot = db.collection("households").document(hId).collection("members").get().await()
                for (doc in snapshot.documents) {
                    if (doc.id == canonicalDocId) continue
                    val remoteName = doc.getString("name").orEmpty().trim().lowercase()
                    val remoteIsDef = doc.getBoolean("isDefault") ?: false
                    if ((namesToPurge.isNotEmpty() && namesToPurge.contains(remoteName)) ||
                        (canonicalDocId == "1" && remoteIsDef) ||
                        (remoteName == member.name.trim().lowercase())) {
                        doc.reference.delete().await()
                    }
                }
            } catch (e: Exception) {}
        }

        if (userId.isNotBlank()) {
            try {
                val snapshot = db.collection("users").document(userId).collection("members").get().await()
                for (doc in snapshot.documents) {
                    if (doc.id == canonicalDocId) continue
                    val remoteName = doc.getString("name").orEmpty().trim().lowercase()
                    val remoteIsDef = doc.getBoolean("isDefault") ?: false
                    if ((namesToPurge.isNotEmpty() && namesToPurge.contains(remoteName)) ||
                        (canonicalDocId == "1" && remoteIsDef) ||
                        (remoteName == member.name.trim().lowercase())) {
                        doc.reference.delete().await()
                    }
                }
            } catch (e: Exception) {}
        }
    }

    suspend fun deleteMemberFromAllScopes(memberId: String, householdIds: List<String>, userId: String, memberName: String = "") {
        if (memberId.isBlank() && memberName.isBlank()) return
        val targetNameNorm = memberName.trim().lowercase()
        if (userId.isNotBlank()) {
            try { 
                if (memberId.isNotBlank()) deleteUserMember(userId, memberId)
                if (targetNameNorm.isNotBlank()) {
                    val snap = db.collection("users").document(userId).collection("members").get().await()
                    for (doc in snap.documents) {
                        if (doc.getString("name").orEmpty().trim().lowercase() == targetNameNorm) {
                            doc.reference.delete().await()
                        }
                    }
                }
            } catch (e: Exception) {}
        }
        for (hId in householdIds.filter { it.isNotBlank() }.distinct()) {
            try { 
                if (memberId.isNotBlank()) deleteMember(hId, memberId)
                if (targetNameNorm.isNotBlank()) {
                    val snap = db.collection("households").document(hId).collection("members").get().await()
                    for (doc in snap.documents) {
                        if (doc.getString("name").orEmpty().trim().lowercase() == targetNameNorm) {
                            doc.reference.delete().await()
                        }
                    }
                }
            } catch (e: Exception) {}
        }
    }

    suspend fun getAllCategoriesAcrossAllScopes(householdIds: List<String>, userId: String): List<FirebaseCategory> {
        val result = mutableListOf<FirebaseCategory>()
        for (hId in householdIds.filter { it.isNotBlank() }.distinct()) {
            val cats = getCategoriesOnce(hId)
            result.addAll(cats)
        }
        if (userId.isNotBlank()) {
            val userCats = getUserCategoriesOnce(userId)
            result.addAll(userCats)
        }
        return result
    }

    suspend fun deleteCategoryFromAllScopes(categoryId: String, householdIds: List<String>, userId: String) {
        if (categoryId.isBlank()) return
        if (userId.isNotBlank()) {
            try { deleteUserCategory(userId, categoryId) } catch (e: Exception) {}
        }
        for (hId in householdIds.filter { it.isNotBlank() }.distinct()) {
            try { deleteCategory(hId, categoryId) } catch (e: Exception) {}
        }
    }

    suspend fun fetchAllHouseholdMembers(householdIds: List<String>): List<FirebaseMember> {
        val result = mutableListOf<FirebaseMember>()
        for (hId in householdIds.filter { it.isNotBlank() }.distinct()) {
            val members = getMembersOnce(hId)
            result.addAll(members)
        }
        return result
    }

    suspend fun fetchAllHouseholdCategories(householdIds: List<String>): List<FirebaseCategory> {
        val result = mutableListOf<FirebaseCategory>()
        for (hId in householdIds.filter { it.isNotBlank() }.distinct()) {
            val cats = getCategoriesOnce(hId)
            result.addAll(cats)
        }
        return result
    }

    suspend fun fetchAllHouseholdSavingsGoals(householdIds: List<String>): List<FirebaseSavingsGoal> {
        val result = mutableListOf<FirebaseSavingsGoal>()
        for (hId in householdIds) {
            val goals = getSavingsGoalsOnce(hId)
            result.addAll(goals)
        }
        return result.distinctBy { it.id.ifBlank { it.title } }
    }

    suspend fun fetchAllHouseholdRecurringExpenses(householdIds: List<String>): List<FirebaseRecurringExpense> {
        val result = mutableListOf<FirebaseRecurringExpense>()
        for (hId in householdIds) {
            val recurring = getRecurringExpensesOnce(hId)
            result.addAll(recurring)
        }
        return result.distinctBy { it.id.ifBlank { it.title } }
    }
}
