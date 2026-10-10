package com.example.data.model

import com.google.firebase.Timestamp

data class Household(
    val id: String = "",
    val name: String = "Το Εξοδολόγιό μου",
    val ownerId: String = "",
    val inviteCode: String = "",
    val memberUids: List<String> = emptyList(),
    val isPremium: Boolean = false,
    val createdAt: Timestamp? = null
)

data class FirebaseExpense(
    val id: String = "",
    val householdId: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val categoryId: Long = 1L,
    val categoryName: String = "",
    val timestamp: Long = 0L,
    val splitType: String = "ALL",
    val memberIdsCsv: String = "",
    val memberNamesDisplay: String = "",
    val note: String = "",
    val createdByUid: String = "",
    val createdByName: String = "",
    val createdAt: Timestamp? = null
)

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val birthDate: String = "",
    val phoneNumber: String = "",
    val occupation: String = "",
    val city: String = "",
    val householdId: String = "",
    val householdIds: List<String> = emptyList(),
    val isPremium: Boolean = false,
    val createdAt: Timestamp? = null
)

data class FirebaseIncome(
    val id: String = "",
    val householdId: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val memberId: Long = 0L,
    val memberName: String = "",
    val category: String = "Μισθός",
    val timestamp: Long = 0L,
    val note: String = "",
    val createdByUid: String = "",
    val createdByName: String = "",
    val createdAt: Timestamp? = null
)

data class FirebaseMember(
    val id: String = "",
    val householdId: String = "",
    val userId: String = "",
    val name: String = "",
    val role: String = "Γονέας",
    val avatarColorHex: Long = 0xFF059669,
    val voiceKeywords: String = "",
    val isDefault: Boolean = false,
    val createdAt: Timestamp? = null
)

data class FirebaseCategory(
    val id: String = "",
    val householdId: String = "",
    val userId: String = "",
    val name: String = "",
    val iconKey: String = "cart",
    val monthlyBudget: Double = 0.0,
    val colorHex: Long = 0xFF059669,
    val voiceKeywords: String = "",
    val createdAt: Timestamp? = null
)

data class FirebaseSavingsGoal(
    val id: String = "",
    val householdId: String = "",
    val userId: String = "",
    val title: String = "",
    val targetAmount: Double = 0.0,
    val currentAmount: Double = 0.0,
    val deadlineDate: String = "",
    val iconKey: String = "vacation",
    val colorHex: Long = 0xFF059669,
    val note: String = "",
    val createdAt: Timestamp? = null
)

data class FirebaseRecurringExpense(
    val id: String = "",
    val householdId: String = "",
    val userId: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val categoryId: Long = 0L,
    val categoryName: String = "",
    val frequency: String = "MONTHLY",
    val dayOfMonth: Int = 1,
    val splitType: String = "ALL_FAMILY",
    val memberIdsCsv: String = "",
    val memberNamesDisplay: String = "Κοινό Οικογένειας",
    val note: String = "",
    val isActive: Boolean = true,
    val lastGeneratedMonth: String = "",
    val createdAt: Timestamp? = null
)
