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
