package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "family_members")
data class FamilyMember(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String = "",
    val role: String = "Γονέας", // "Γονέας", "Παιδί", "Άλλο"
    val avatarColorHex: Long = 0xFF059669,
    val voiceKeywords: String = "", // Comma separated: "μάριος, μπαμπάς, πατέρας"
    val isDefault: Boolean = false,
    val sortOrder: Int = 0
) {
    val colorHex: Long get() = avatarColorHex
}
