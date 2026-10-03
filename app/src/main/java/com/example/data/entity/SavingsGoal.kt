package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "savings_goals")
data class SavingsGoal(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val deadlineDate: String = "", // e.g. "Καλοκαίρι 2027" or "31/12/2026"
    val iconKey: String = "vacation", // vacation, car, home, emergency, tech, kids
    val colorHex: Long = 0xFF059669,
    val note: String = ""
)
