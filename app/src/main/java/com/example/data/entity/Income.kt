package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "incomes")
data class Income(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cloudId: String = "",
    val title: String,
    val amount: Double,
    val memberId: Long,
    val memberName: String,
    val category: String = "Μισθός", // Μισθός, Ενοίκιο, Επιχείρηση, Επιδότηση, Bonus, Άλλο
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = ""
)
