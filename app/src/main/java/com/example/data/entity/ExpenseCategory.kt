package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expense_categories")
data class ExpenseCategory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String = "",
    val iconKey: String = "other", // "cart", "coffee", "sports", "bill", "food", "car", "medical", "other"
    val monthlyBudget: Double = 0.0,
    val colorHex: Long = 0xFF059669,
    val voiceKeywords: String = "",
    val sortOrder: Int = 0,
    val fontColorHex: Long = 0L
) {
    val monthlyLimit: Double get() = monthlyBudget
    val iconName: String get() = iconKey
}
