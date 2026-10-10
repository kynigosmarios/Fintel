package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cloudId: String = "",
    val title: String,
    val amount: Double,
    val categoryId: Long,
    val categoryName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val splitType: String, // "ALL_FAMILY" or "SPECIFIC_MEMBERS"
    val memberIdsCsv: String = "", // e.g. "1,2"
    val memberNamesDisplay: String = "Κοινό Οικογένειας", // e.g. "Κοινό Οικογένειας" ή ονόματα μελών
    val splitAmountPerPerson: Double = 0.0,
    val originalVoicePrompt: String = "",
    val note: String = ""
)
