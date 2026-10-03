package com.example.data.model

import com.google.firebase.Timestamp

data class Household(
    val id: String = "",
    val name: String = "Οικογένεια",
    val ownerId: String = "",
    val inviteCode: String = "",
    val memberUids: List<String> = emptyList(),
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
    val createdAt: Timestamp? = null
)
