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
    val splitType: String = "ALL_FAMILY", // "ALL_FAMILY" or "SPECIFIC_MEMBERS"
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
    // Strip any existing parenthesis like "(από 1,50€)", "(από 2.00 €)", or "(...)"
    val cleanRawPayer = payerMemberName.replace(Regex("\\s*\\([^)]*\\)"), "").trim()

    // 1. Explicit collective labels
    if (cleanRawPayer.equals("Γονείς", ignoreCase = true) || cleanRawPayer.equals("Parents", ignoreCase = true)) {
        return if (lang == Localization.Language.ENGLISH) "Parents" else "Γονείς"
    }
    if (cleanRawPayer.equals("Παιδιά", ignoreCase = true) || cleanRawPayer.equals("Children", ignoreCase = true)) {
        return if (lang == Localization.Language.ENGLISH) "Children" else "Παιδιά"
    }
    if (cleanRawPayer.equals("Συγκάτοικοι", ignoreCase = true) || cleanRawPayer.equals("Roommates", ignoreCase = true)) {
        return if (lang == Localization.Language.ENGLISH) "Roommates" else "Συγκάτοικοι"
    }
    if (cleanRawPayer.equals("Οικογένεια", ignoreCase = true) || cleanRawPayer.equals("Family", ignoreCase = true)) {
        return if (lang == Localization.Language.ENGLISH) "Family" else "Οικογένεια"
    }

    // 2. Resolve payer member objects
    val ids = if (payerMemberIdsCsv.isNotBlank()) {
        payerMemberIdsCsv.split(",").mapNotNull { it.trim().toLongOrNull() }
    } else emptyList()

    val matchedById = if (ids.isNotEmpty() && members.isNotEmpty()) {
        ids.mapNotNull { id -> members.find { it.id == id } }
    } else emptyList()

    val matchedPayers: List<FamilyMember> = if (matchedById.isNotEmpty()) {
        matchedById
    } else if (cleanRawPayer.isNotBlank() && members.isNotEmpty()) {
        val rawParts = cleanRawPayer.split(Regex("[,&+]+|\\s+και\\s+|\\s+and\\s+")).map { it.trim() }.filter { it.isNotBlank() }
        rawParts.mapNotNull { namePart ->
            val cleanName = namePart.replace(Regex("\\([^)]*\\)"), "").trim()
            members.find {
                it.name.equals(cleanName, ignoreCase = true) ||
                it.name.contains(cleanName, ignoreCase = true) ||
                cleanName.contains(it.name, ignoreCase = true)
            }
        }.distinctBy { it.id }
    } else if (payerMemberId != 0L && members.isNotEmpty()) {
        members.find { it.id == payerMemberId }?.let { listOf(it) } ?: emptyList()
    } else emptyList()

    // 3. Single payer -> display individual name without parenthesis
    if (matchedPayers.size == 1) {
        return matchedPayers[0].name
    }

    // 4. Two or more payers: Check if ALL share the same role (e.g. all parents, all children, all roommates)
    if (matchedPayers.size >= 2) {
        val firstRolePlural = Localization.pluralRoleName(matchedPayers.first().role, lang)
        val allSameRole = matchedPayers.all {
            Localization.pluralRoleName(it.role, lang).equals(firstRolePlural, ignoreCase = true)
        }
        if (allSameRole) {
            return firstRolePlural
        }

        // Check if all members of the household
        if (members.isNotEmpty() && matchedPayers.size >= members.size) {
            return if (lang == Localization.Language.ENGLISH) "Family" else "Οικογένεια"
        }

        return matchedPayers.joinToString(" & ") { it.name }
    }

    return cleanRawPayer.ifBlank {
        if (lang == Localization.Language.ENGLISH) "Family" else "Οικογένεια"
    }
}

fun Expense.getBeneficiaryCategoryOrNamesDisplay(
    members: List<FamilyMember> = emptyList(),
    lang: Localization.Language = Localization.Language.GREEK
): String {
    val raw = memberNamesDisplay.replace(Regex("\\s*\\([^)]*\\)"), "").trim()

    // 1. Explicit family / shared keywords
    if (splitType == "ALL_FAMILY" || splitType == "ALL" ||
        raw.equals("Κοινό Οικογένειας", ignoreCase = true) ||
        raw.equals("Family Shared", ignoreCase = true) ||
        raw.equals("Οικογένεια", ignoreCase = true) ||
        raw.equals("Family", ignoreCase = true)
    ) {
        return if (lang == Localization.Language.ENGLISH) "Family" else "Οικογένεια"
    }

    if (raw.equals("Παιδιά", ignoreCase = true) || raw.equals("Children", ignoreCase = true)) {
        return if (lang == Localization.Language.ENGLISH) "Children" else "Παιδιά"
    }

    if (raw.equals("Γονείς", ignoreCase = true) || raw.equals("Parents", ignoreCase = true)) {
        return if (lang == Localization.Language.ENGLISH) "Parents" else "Γονείς"
    }

    if (raw.equals("Συγκάτοικοι", ignoreCase = true) || raw.equals("Roommates", ignoreCase = true)) {
        return if (lang == Localization.Language.ENGLISH) "Roommates" else "Συγκάτοικοι"
    }

    // 2. Resolve members by memberIdsCsv first, then fallback to names in memberNamesDisplay
    val ids = if (memberIdsCsv.isNotBlank()) {
        memberIdsCsv.split(",").mapNotNull { it.trim().toLongOrNull() }
    } else emptyList()

    val matchedById = if (ids.isNotEmpty() && members.isNotEmpty()) {
        ids.mapNotNull { id -> members.find { it.id == id } }
    } else emptyList()

    val matchedMembers: List<FamilyMember> = if (matchedById.isNotEmpty()) {
        matchedById
    } else if (raw.isNotBlank() && members.isNotEmpty()) {
        val rawParts = raw.split(Regex("[,&+]+|\\s+και\\s+|\\s+and\\s+")).map { it.trim() }.filter { it.isNotBlank() }
        rawParts.mapNotNull { namePart ->
            members.find {
                it.name.equals(namePart, ignoreCase = true) ||
                it.name.contains(namePart, ignoreCase = true) ||
                namePart.contains(it.name, ignoreCase = true)
            }
        }.distinctBy { it.id }
    } else emptyList()

    // 3. Single beneficiary -> display individual name
    if (matchedMembers.size == 1) {
        return matchedMembers[0].name
    }

    // 4. Two or more beneficiaries: Check if ALL share the same role (e.g. all parents, all children, all roommates)
    if (matchedMembers.size >= 2) {
        val firstRolePlural = Localization.pluralRoleName(matchedMembers.first().role, lang)
        val allSameRole = matchedMembers.all {
            Localization.pluralRoleName(it.role, lang).equals(firstRolePlural, ignoreCase = true)
        }
        if (allSameRole) {
            return firstRolePlural
        }

        // Check if they constitute all members of the household
        if (members.isNotEmpty() && matchedMembers.size >= members.size) {
            val allIds = members.map { it.id }.toSet()
            if (matchedMembers.map { it.id }.toSet().containsAll(allIds)) {
                return if (lang == Localization.Language.ENGLISH) "Family" else "Οικογένεια"
            }
        }

        // Mixed roles -> display joined names
        return matchedMembers.joinToString(" & ") { it.name }
    }

    // Fallback if matchedMembers was empty
    if (raw.isNotBlank()) {
        return raw
    }

    return if (lang == Localization.Language.ENGLISH) "Personal" else "Ατομικό"
}


