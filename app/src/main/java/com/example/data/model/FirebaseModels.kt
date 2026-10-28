package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

@IgnoreExtraProperties
data class Household(
    var id: String = "",
    var name: String = "Το Εξοδολόγιό μου",
    var ownerId: String = "",
    var inviteCode: String = "",
    var memberUids: List<String> = emptyList(),
    var invitedEmails: List<String> = emptyList(),
    var isPremium: Boolean = false,
    var createdAt: Timestamp? = null,
    var updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirebaseExpense(
    var id: String = "",
    var householdId: String = "",
    var title: String = "",
    var amount: Double = 0.0,
    var categoryId: Long = 1L,
    var categoryName: String = "",
    var timestamp: Long = 0L,
    var splitType: String = "ALL",
    var memberIdsCsv: String = "",
    var memberNamesDisplay: String = "",
    var note: String = "",
    var payerMemberId: Long = 0L,
    var payerMemberName: String = "",
    var payerMemberIdsCsv: String = "",
    var payerAmountsCsv: String = "",
    var createdByUid: String = "",
    var createdByName: String = "",
    var createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UserProfile(
    var uid: String = "",
    var email: String = "",
    var displayName: String = "",
    var firstName: String = "",
    var lastName: String = "",
    var birthDate: String = "",
    var phoneNumber: String = "",
    var occupation: String = "",
    var city: String = "",
    var householdId: String = "",
    var householdIds: List<String> = emptyList(),
    var isPremium: Boolean = false,
    var photoUrl: String = "",
    var authProvider: String = "password", // "password" or "google"
    var appLanguage: String = "EL",
    var themeMode: String = "SYSTEM",
    var reminderEnabled: Boolean = false,
    var reminderHour: Int = 21,
    var reminderMinute: Int = 30,
    var notificationConsentCompleted: Boolean = false,
    var createdAt: Timestamp? = null,
    var updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirebaseIncome(
    var id: String = "",
    var householdId: String = "",
    var title: String = "",
    var amount: Double = 0.0,
    var memberId: Long = 0L,
    var memberName: String = "",
    var category: String = "Μισθός",
    var timestamp: Long = 0L,
    var note: String = "",
    var createdByUid: String = "",
    var createdByName: String = "",
    var createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirebaseMember(
    var id: String = "",
    var householdId: String = "",
    var userId: String = "",
    var name: String = "",
    var role: String = "Γονέας",
    var avatarColorHex: Long = 0xFF059669,
    var voiceKeywords: String = "",
    @get:PropertyName("default")
    @set:PropertyName("default")
    var isDefault: Boolean = false,
    var sortOrder: Int = 0,
    var createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirebaseCategory(
    var id: String = "",
    var householdId: String = "",
    var userId: String = "",
    var name: String = "",
    var iconKey: String = "cart",
    var monthlyBudget: Double = 0.0,
    var colorHex: Long = 0xFF059669,
    var voiceKeywords: String = "",
    var sortOrder: Int = 0,
    var fontColorHex: Long = 0L,
    var createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirebaseSavingsGoal(
    var id: String = "",
    var householdId: String = "",
    var userId: String = "",
    var title: String = "",
    var targetAmount: Double = 0.0,
    var currentAmount: Double = 0.0,
    var deadlineDate: String = "",
    var iconKey: String = "vacation",
    var colorHex: Long = 0xFF059669,
    var note: String = "",
    var createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirebaseRecurringExpense(
    var id: String = "",
    var householdId: String = "",
    var userId: String = "",
    var title: String = "",
    var amount: Double = 0.0,
    var categoryId: Long = 0L,
    var categoryName: String = "",
    var frequency: String = "MONTHLY",
    var dayOfMonth: Int = 1,
    var splitType: String = "ALL_FAMILY",
    var memberIdsCsv: String = "",
    var memberNamesDisplay: String = "Κοινό Οικογένειας",
    var note: String = "",
    var isActive: Boolean = true,
    var lastGeneratedMonth: String = "",
    var payerMemberId: Long = 0L,
    var payerMemberName: String = "",
    var payerMemberIdsCsv: String = "",
    var payerAmountsCsv: String = "",
    var createdAt: Timestamp? = null
)
