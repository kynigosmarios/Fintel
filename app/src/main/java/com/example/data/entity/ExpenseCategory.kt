package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expense_categories")
data class ExpenseCategory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val iconKey: String, // "cart", "coffee", "sports", "bill", "food", "car", "medical", "other"
    val monthlyBudget: Double,
    val colorHex: Long,
    val voiceKeywords: String,
    val sortOrder: Int = 0,
    val fontColorHex: Long = 0L
)
