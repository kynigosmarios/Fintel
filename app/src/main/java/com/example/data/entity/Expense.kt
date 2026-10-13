package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.FirebaseExpense
import com.example.util.Localization

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
    val note: String = "",
    val payerMemberId: Long = 0L,
    val payerMemberName: String = "",
    val payerMemberIdsCsv: String = "",
    val payerAmountsCsv: String = ""
)

fun Expense.toFirebaseExpense(
    householdId: String,
    userId: String,
    userName: String
): FirebaseExpense {
    val docId = if (cloudId.isNotBlank()) cloudId else if (id > 0) "exp_$id" else java.util.UUID.randomUUID().toString()
    return FirebaseExpense(
        id = docId,
        householdId = householdId,
        title = title,
        amount = amount,
        categoryId = categoryId,
        categoryName = categoryName,
        timestamp = timestamp,
        splitType = splitType,
        memberIdsCsv = memberIdsCsv,
        memberNamesDisplay = memberNamesDisplay,
        note = note,
        payerMemberId = payerMemberId,
        payerMemberName = payerMemberName,
        payerMemberIdsCsv = payerMemberIdsCsv,
        payerAmountsCsv = payerAmountsCsv,
        createdByUid = userId,
        createdByName = userName
    )
}

data class PayerShare(
    val memberId: Long,
    val memberName: String,
    val amount: Double
)

fun Expense.getPayerShares(members: List<FamilyMember> = emptyList()): List<PayerShare> {
    // Strip amount annotations in parentheses first, e.g. "(από 20,00 €)" or "(30,00 €)"
    // so that decimal commas inside currency numbers do not corrupt name splitting!
    val cleanPayerString = payerMemberName.replace(Regex("\\([^)]*\\)"), "").trim()
    val individualNames = cleanPayerString
        .split(Regex("[&,/]+"))
        .map { it.trim() }
        .filter { it.isNotBlank() }

    // 1. If explicit custom amounts are stored in payerAmountsCsv (e.g. "1:30.00,2:10.00")
    if (payerAmountsCsv.isNotBlank()) {
        val parsed = payerAmountsCsv.split(",").mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) {
                val id = parts[0].trim().toLongOrNull()
                val amt = parts[1].trim().toDoubleOrNull()
                if (id != null && amt != null) Pair(id, amt) else null
            } else null
        }
        if (parsed.isNotEmpty()) {
            return parsed.mapIndexed { index, (id, amt) ->
                val member = members.find { it.id == id }
                    ?: if (index < individualNames.size) members.find { it.name.equals(individualNames[index], ignoreCase = true) } else null
                val name = member?.name
                    ?: if (index < individualNames.size) individualNames[index]
                    else if (id == payerMemberId && cleanPayerString.isNotBlank() && !cleanPayerString.contains("&")) cleanPayerString
                    else "Μέλος #$id"
                PayerShare(memberId = id, memberName = name, amount = amt)
            }
        }
    }

    // 2. If multiple payer IDs are stored in payerMemberIdsCsv (e.g. "1,2") without custom amounts -> equal split
    if (payerMemberIdsCsv.isNotBlank()) {
        val ids = payerMemberIdsCsv.split(",").mapNotNull { it.trim().toLongOrNull() }
        if (ids.isNotEmpty()) {
            val shareAmt = if (amount > 0.0) amount / ids.size else 0.0
            return ids.mapIndexed { index, id ->
                val member = members.find { it.id == id }
                    ?: if (index < individualNames.size) members.find { it.name.equals(individualNames[index], ignoreCase = true) } else null
                val name = member?.name
                    ?: if (index < individualNames.size) individualNames[index]
                    else if (id == payerMemberId && cleanPayerString.isNotBlank() && !cleanPayerString.contains("&")) cleanPayerString
                    else "Μέλος #$id"
                PayerShare(memberId = id, memberName = name, amount = shareAmt)
            }
        }
    }

    // 3. If multiple names were in cleanPayerString (e.g. "Μάριος & Βασιλική")
    if (individualNames.size > 1) {
        val shareAmt = if (amount > 0.0) amount / individualNames.size else 0.0
        return individualNames.mapIndexed { index, name ->
            val member = members.find { it.name.equals(name, ignoreCase = true) }
            val id = member?.id ?: (index + 1L)
            PayerShare(memberId = id, memberName = member?.name ?: name, amount = shareAmt)
        }
    }

    // 4. Fallback to single payer
    val singlePayer = members.find { it.id == payerMemberId }
        ?: if (cleanPayerString.isNotBlank()) members.find { it.name.equals(cleanPayerString, ignoreCase = true) } else null
        ?: members.firstOrNull { it.role.equals("Γονέας", ignoreCase = true) }
        ?: members.firstOrNull()

    return if (singlePayer != null) {
        listOf(PayerShare(memberId = singlePayer.id, memberName = singlePayer.name, amount = amount))
    } else {
        listOf(PayerShare(memberId = payerMemberId, memberName = cleanPayerString.ifBlank { "Οικογένεια" }, amount = amount))
    }
}

fun Expense.hasMultiplePayers(members: List<FamilyMember> = emptyList()): Boolean {
    if (payerMemberIdsCsv.isNotBlank() && payerMemberIdsCsv.split(",").filter { it.isNotBlank() }.size > 1) return true
    if (payerAmountsCsv.isNotBlank() && payerAmountsCsv.split(",").filter { it.isNotBlank() }.size > 1) return true
    if (payerMemberName.contains("&") || payerMemberName.contains(",")) return true
    return false
}

fun Expense.getPayerDisplay(members: List<FamilyMember> = emptyList()): String {
    val shares = getPayerShares(members)
    if (shares.isEmpty()) {
        return payerMemberName.ifBlank { "Οικογένεια" }
    }
    if (shares.size == 1) {
        return shares[0].memberName
    }
    // Check if shares are virtually equal
    val firstAmt = shares[0].amount
    val areEqual = shares.all { Math.abs(it.amount - firstAmt) < 0.01 }
    return if (areEqual) {
        // e.g. "Μαρία & Γιάννης (από 20,00 €)"
        "${shares.joinToString(" & ") { it.memberName }} (από ${String.format(java.util.Locale.US, "%.2f €", firstAmt)})"
    } else {
        // e.g. "Μαρία (30,00 €) & Γιάννης (10,00 €)"
        shares.joinToString(" & ") { "${it.memberName} (${String.format(java.util.Locale.US, "%.2f €", it.amount)})" }
    }
}

fun getMemberInitials(name: String): String {
    val clean = name.trim()
    if (clean.isBlank()) return ""
    if (clean.equals("Οικογένεια", ignoreCase = true) || clean.equals("Family", ignoreCase = true)) {
        return "Οικ."
    }
    val parts = clean.split(Regex("[\\s&+,/]+")).filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> ""
        parts.size == 1 -> "${parts[0].take(1).uppercase()}."
        else -> "${parts[0].take(1).uppercase()}.${parts[1].take(1).uppercase()}."
    }
}

fun Expense.getPayerInitialsDisplay(members: List<FamilyMember> = emptyList()): String {
    val shares = getPayerShares(members)
    if (shares.isEmpty()) {
        val raw = payerMemberName.ifBlank { "Οικ." }
        return getMemberInitials(raw)
    }
    if (shares.size == 1) {
        return getMemberInitials(shares[0].memberName)
    }
    return shares.joinToString(" & ") { getMemberInitials(it.memberName) }
}

fun Expense.getBeneficiaryInitialsDisplay(members: List<FamilyMember> = emptyList()): String? {
    if (splitType == "ALL_FAMILY") {
        return "Οικογένεια"
    }
    val raw = memberNamesDisplay.trim()
    if (raw.isBlank()) return null
    val names = raw.split(",").map { it.trim() }.filter { it.isNotBlank() }
    if (names.isEmpty()) return null
    return names.joinToString(", ") { getMemberInitials(it) }
}

fun Expense.getPayerCategoryOrNamesDisplay(
    members: List<FamilyMember> = emptyList(),
    lang: Localization.Language = Localization.Language.GREEK
): String {
    // If payer is explicitly labeled with a category name
    if (payerMemberName.trim().equals("Γονείς", ignoreCase = true) || payerMemberName.trim().equals("Parents", ignoreCase = true)) {
        return if (lang == Localization.Language.ENGLISH) "Parents" else "Γονείς"
    }

    if (payerMemberName.trim().equals("Παιδιά", ignoreCase = true) || payerMemberName.trim().equals("Children", ignoreCase = true)) {
        return if (lang == Localization.Language.ENGLISH) "Children" else "Παιδιά"
    }

    if (payerMemberName.trim().equals("Οικογένεια", ignoreCase = true) || payerMemberName.trim().equals("Family", ignoreCase = true)) {
        return if (lang == Localization.Language.ENGLISH) "Family" else "Οικογένεια"
    }

    val shares = getPayerShares(members)
    if (shares.isEmpty()) {
        return payerMemberName.ifBlank { if (lang == Localization.Language.ENGLISH) "Family" else "Οικογένεια" }
    }

    // Single payer -> always display their individual name
    if (shares.size == 1) {
        return shares[0].memberName
    }

    // Identify member objects for all payers
    val payerMembers = shares.mapNotNull { share ->
        members.find { it.id == share.memberId }
            ?: members.find { it.name.equals(share.memberName, ignoreCase = true) }
    }

    if (payerMembers.isNotEmpty() && members.isNotEmpty()) {
        // Check if ALL payers share a category and constitute ALL members of that category in the household
        val firstRole = payerMembers.first().role.trim()
        val allSameRole = payerMembers.size == shares.size && payerMembers.all {
            Localization.pluralRoleName(it.role, lang).equals(Localization.pluralRoleName(firstRole, lang), ignoreCase = true)
        }
        if (allSameRole) {
            val categoryPlural = Localization.pluralRoleName(firstRole, lang)
            val allHouseholdOfCategory = members.filter {
                Localization.pluralRoleName(it.role, lang).equals(categoryPlural, ignoreCase = true)
            }
            val payerIds = payerMembers.map { it.id }.toSet()
            val allCategoryIds = allHouseholdOfCategory.map { it.id }.toSet()
            // If they are ALL of that category in the household (and count >= 2)
            if (allHouseholdOfCategory.size >= 2 && payerIds.containsAll(allCategoryIds)) {
                return categoryPlural
            }
        }

        // Check if ALL household members are payers
        val householdIds = members.map { it.id }.toSet()
        val payerIds = payerMembers.map { it.id }.toSet()
        if (payerMembers.size >= members.size && payerIds.containsAll(householdIds)) {
            return if (lang == Localization.Language.ENGLISH) "Family" else "Οικογένεια"
        }
    }

    // Otherwise, show individual names joined by &
    return shares.joinToString(" & ") { it.memberName }
}

fun Expense.getBeneficiaryCategoryOrNamesDisplay(
    members: List<FamilyMember> = emptyList(),
    lang: Localization.Language = Localization.Language.GREEK
): String {
    if (splitType == "ALL_FAMILY" || memberNamesDisplay == "Κοινό Οικογένειας") {
        return if (lang == Localization.Language.ENGLISH) "Family" else "Οικογένεια"
    }

    if (memberNamesDisplay.trim().equals("Παιδιά", ignoreCase = true) || memberNamesDisplay.trim().equals("Children", ignoreCase = true)) {
        return if (lang == Localization.Language.ENGLISH) "Children" else "Παιδιά"
    }

    if (memberNamesDisplay.trim().equals("Γονείς", ignoreCase = true) || memberNamesDisplay.trim().equals("Parents", ignoreCase = true)) {
        return if (lang == Localization.Language.ENGLISH) "Parents" else "Γονείς"
    }

    if (memberNamesDisplay.trim().equals("Οικογένεια", ignoreCase = true) || memberNamesDisplay.trim().equals("Family", ignoreCase = true)) {
        return if (lang == Localization.Language.ENGLISH) "Family" else "Οικογένεια"
    }

    // Find beneficiary members
    val ids = if (memberIdsCsv.isNotBlank()) {
        memberIdsCsv.split(",").mapNotNull { it.trim().toLongOrNull() }
    } else emptyList()

    val beneficiaryMembers = if (ids.isNotEmpty() && members.isNotEmpty()) {
        ids.mapNotNull { id -> members.find { it.id == id } }
    } else {
        val rawNames = memberNamesDisplay.split(Regex("[,&]+")).map { it.trim() }.filter { it.isNotBlank() }
        rawNames.mapNotNull { name -> members.find { it.name.equals(name, ignoreCase = true) } }
    }

    // Single beneficiary -> always display their name
    if (beneficiaryMembers.size == 1) {
        return beneficiaryMembers[0].name
    }

    // Check if ALL beneficiaries share a role and constitute ALL members of that role in the household
    if (beneficiaryMembers.isNotEmpty() && members.isNotEmpty()) {
        val firstRole = beneficiaryMembers.first().role.trim()
        val allSameRole = beneficiaryMembers.all {
            Localization.pluralRoleName(it.role, lang).equals(Localization.pluralRoleName(firstRole, lang), ignoreCase = true)
        }
        if (allSameRole) {
            val categoryPlural = Localization.pluralRoleName(firstRole, lang)
            val allHouseholdOfCategory = members.filter {
                Localization.pluralRoleName(it.role, lang).equals(categoryPlural, ignoreCase = true)
            }
            val benIds = beneficiaryMembers.map { it.id }.toSet()
            val allCategoryIds = allHouseholdOfCategory.map { it.id }.toSet()
            // If they are ALL of that category in the household (and count >= 2)
            if (allHouseholdOfCategory.size >= 2 && benIds.containsAll(allCategoryIds)) {
                return categoryPlural
            }
        }

        // Check if ALL household members are beneficiaries
        val householdIds = members.map { it.id }.toSet()
        val benIds = beneficiaryMembers.map { it.id }.toSet()
        if (beneficiaryMembers.size >= members.size && benIds.containsAll(householdIds)) {
            return if (lang == Localization.Language.ENGLISH) "Family" else "Οικογένεια"
        }
    }

    // Otherwise, write full names
    if (beneficiaryMembers.isNotEmpty()) {
        return beneficiaryMembers.joinToString(", ") { it.name }
    }

    return memberNamesDisplay.ifBlank { if (lang == Localization.Language.ENGLISH) "Family" else "Οικογένεια" }
}

