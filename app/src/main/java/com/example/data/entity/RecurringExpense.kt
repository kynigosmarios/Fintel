package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recurring_expenses")
data class RecurringExpense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val categoryId: Long,
    val categoryName: String,
    val frequency: String = "MONTHLY", // "MONTHLY", "WEEKLY", "YEARLY"
    val dayOfMonth: Int = 1, // 1 to 31
    val splitType: String = "ALL_FAMILY", // "ALL_FAMILY" or "SPECIFIC_MEMBERS"
    val memberIdsCsv: String = "",
    val memberNamesDisplay: String = "Κοινό Οικογένειας",
    val note: String = "",
    val isActive: Boolean = true,
    val lastGeneratedMonth: String = "", // e.g. "2026-10"
    val payerMemberId: Long = 0L,
    val payerMemberName: String = "",
    val payerMemberIdsCsv: String = "",
    val payerAmountsCsv: String = ""
)
