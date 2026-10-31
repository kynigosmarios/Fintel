package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

/**
 * Robust, fail-safe Firestore mappers that safely extract fields from DocumentSnapshot.
 * These protect against field type mismatches (Double vs Long/Int), conflicting annotations,
 * and missing values so that queries NEVER throw unhandled exceptions or drop documents.
 */

fun DocumentSnapshot.toSafeExpense(): FirebaseExpense {
    val existing = try {
        this.toObject(FirebaseExpense::class.java)
    } catch (_: Exception) {
        null
    }
    val rawAmount = get("amount")
    val amt = when (rawAmount) {
        is Number -> rawAmount.toDouble()
        is String -> rawAmount.toDoubleOrNull() ?: 0.0
        else -> existing?.amount ?: 0.0
    }
    val rawCatId = get("categoryId")
    val catId = when (rawCatId) {
        is Number -> rawCatId.toLong()
        is String -> rawCatId.toLongOrNull() ?: 1L
        else -> existing?.categoryId ?: 1L
    }
    val rawTimestamp = get("timestamp")
    val ts = when (rawTimestamp) {
        is Number -> rawTimestamp.toLong()
        is String -> rawTimestamp.toLongOrNull() ?: 0L
        is Timestamp -> rawTimestamp.toDate().time
        else -> getTimestamp("createdAt")?.toDate()?.time ?: existing?.timestamp ?: 0L
    }
    val rawPayerId = get("payerMemberId")
    val payerId = when (rawPayerId) {
        is Number -> rawPayerId.toLong()
        is String -> rawPayerId.toLongOrNull() ?: 0L
        else -> existing?.payerMemberId ?: 0L
    }

    val docId = getString("id")?.ifBlank { this.id } ?: this.id

    return (existing ?: FirebaseExpense()).copy(
        id = docId,
        householdId = getString("householdId") ?: existing?.householdId ?: "",
        title = getString("title") ?: existing?.title ?: "",
        amount = amt,
        categoryId = catId,
        categoryName = getString("categoryName") ?: existing?.categoryName ?: "",
        timestamp = ts,
        splitType = getString("splitType") ?: existing?.splitType ?: "ALL",
        memberIdsCsv = getString("memberIdsCsv") ?: existing?.memberIdsCsv ?: "",
        memberNamesDisplay = getString("memberNamesDisplay") ?: existing?.memberNamesDisplay ?: "",
        note = getString("note") ?: existing?.note ?: "",
        payerMemberId = payerId,
        payerMemberName = getString("payerMemberName") ?: existing?.payerMemberName ?: "",
        payerMemberIdsCsv = getString("payerMemberIdsCsv") ?: existing?.payerMemberIdsCsv ?: "",
        payerAmountsCsv = getString("payerAmountsCsv") ?: existing?.payerAmountsCsv ?: "",
        createdByUid = getString("createdByUid") ?: existing?.createdByUid ?: "",
        createdByName = getString("createdByName") ?: existing?.createdByName ?: "",
        createdAt = getTimestamp("createdAt") ?: existing?.createdAt
    )
}

fun DocumentSnapshot.toSafeMember(): FirebaseMember {
    val existing = try {
        this.toObject(FirebaseMember::class.java)
    } catch (_: Exception) {
        null
    }
    val rawDefault = get("default") ?: get("isDefault")
    val defVal = when (rawDefault) {
        is Boolean -> rawDefault
        is String -> rawDefault.toBoolean()
        is Number -> rawDefault.toInt() != 0
        else -> existing?.isDefault ?: false
    }
    val rawColor = get("avatarColorHex")
    val color = when (rawColor) {
        is Number -> rawColor.toLong()
        is String -> rawColor.toLongOrNull() ?: 0xFF059669
        else -> existing?.avatarColorHex ?: 0xFF059669
    }
    val rawSort = get("sortOrder")
    val sort = when (rawSort) {
        is Number -> rawSort.toInt()
        is String -> rawSort.toIntOrNull() ?: 0
        else -> existing?.sortOrder ?: 0
    }
    val docId = getString("id")?.ifBlank { this.id } ?: this.id

    return (existing ?: FirebaseMember()).copy(
        id = docId,
        householdId = getString("householdId") ?: existing?.householdId ?: "",
        userId = getString("userId") ?: existing?.userId ?: "",
        name = getString("name") ?: existing?.name ?: "",
        role = getString("role") ?: existing?.role ?: "Γονέας",
        avatarColorHex = color,
        voiceKeywords = getString("voiceKeywords") ?: existing?.voiceKeywords ?: "",
        isDefault = defVal,
        sortOrder = sort,
        createdAt = getTimestamp("createdAt") ?: existing?.createdAt
    )
}

fun DocumentSnapshot.toSafeIncome(): FirebaseIncome {
    val existing = try {
        this.toObject(FirebaseIncome::class.java)
    } catch (_: Exception) {
        null
    }
    val rawAmount = get("amount")
    val amt = when (rawAmount) {
        is Number -> rawAmount.toDouble()
        is String -> rawAmount.toDoubleOrNull() ?: 0.0
        else -> existing?.amount ?: 0.0
    }
    val rawMemberId = get("memberId")
    val mId = when (rawMemberId) {
        is Number -> rawMemberId.toLong()
        is String -> rawMemberId.toLongOrNull() ?: 0L
        else -> existing?.memberId ?: 0L
    }
    val rawTimestamp = get("timestamp")
    val ts = when (rawTimestamp) {
        is Number -> rawTimestamp.toLong()
        is String -> rawTimestamp.toLongOrNull() ?: 0L
        is Timestamp -> rawTimestamp.toDate().time
        else -> getTimestamp("createdAt")?.toDate()?.time ?: existing?.timestamp ?: 0L
    }
    val docId = getString("id")?.ifBlank { this.id } ?: this.id

    return (existing ?: FirebaseIncome()).copy(
        id = docId,
        householdId = getString("householdId") ?: existing?.householdId ?: "",
        title = getString("title") ?: existing?.title ?: "",
        amount = amt,
        memberId = mId,
        memberName = getString("memberName") ?: existing?.memberName ?: "",
        category = getString("category") ?: existing?.category ?: "Μισθός",
        timestamp = ts,
        note = getString("note") ?: existing?.note ?: "",
        createdByUid = getString("createdByUid") ?: existing?.createdByUid ?: "",
        createdByName = getString("createdByName") ?: existing?.createdByName ?: "",
        createdAt = getTimestamp("createdAt") ?: existing?.createdAt
    )
}

fun DocumentSnapshot.toSafeCategory(): FirebaseCategory {
    val existing = try {
        this.toObject(FirebaseCategory::class.java)
    } catch (_: Exception) {
        null
    }
    val rawBudget = get("monthlyBudget")
    val budget = when (rawBudget) {
        is Number -> rawBudget.toDouble()
        is String -> rawBudget.toDoubleOrNull() ?: 0.0
        else -> existing?.monthlyBudget ?: 0.0
    }
    val rawColor = get("colorHex")
    val color = when (rawColor) {
        is Number -> rawColor.toLong()
        is String -> rawColor.toLongOrNull() ?: 0xFF059669
        else -> existing?.colorHex ?: 0xFF059669
    }
    val rawFontColor = get("fontColorHex")
    val fontColor = when (rawFontColor) {
        is Number -> rawFontColor.toLong()
        is String -> rawFontColor.toLongOrNull() ?: 0L
        else -> existing?.fontColorHex ?: 0L
    }
    val rawSort = get("sortOrder")
    val sort = when (rawSort) {
        is Number -> rawSort.toInt()
        is String -> rawSort.toIntOrNull() ?: 0
        else -> existing?.sortOrder ?: 0
    }
    val docId = getString("id")?.ifBlank { this.id } ?: this.id

    return (existing ?: FirebaseCategory()).copy(
        id = docId,
        householdId = getString("householdId") ?: existing?.householdId ?: "",
        userId = getString("userId") ?: existing?.userId ?: "",
        name = getString("name") ?: existing?.name ?: "",
        iconKey = getString("iconKey") ?: existing?.iconKey ?: "cart",
        monthlyBudget = budget,
        colorHex = color,
        voiceKeywords = getString("voiceKeywords") ?: existing?.voiceKeywords ?: "",
        sortOrder = sort,
        fontColorHex = fontColor,
        createdAt = getTimestamp("createdAt") ?: existing?.createdAt
    )
}

fun DocumentSnapshot.toSafeHousehold(): Household {
    val existing = try {
        this.toObject(Household::class.java)
    } catch (_: Exception) {
        null
    }
    val members = (get("memberUids") as? List<*>)?.mapNotNull { it?.toString() } ?: existing?.memberUids ?: emptyList()
    val invited = (get("invitedEmails") as? List<*>)?.mapNotNull { it?.toString() } ?: existing?.invitedEmails ?: emptyList()
    val rawPrem = get("isPremium") ?: get("premium")
    val isPrem = when (rawPrem) {
        is Boolean -> rawPrem
        is String -> rawPrem.toBoolean()
        is Number -> rawPrem.toInt() != 0
        else -> existing?.isPremium ?: false
    }
    val docId = getString("id")?.ifBlank { this.id } ?: this.id

    val rolesMap = (get("memberRoles") as? Map<*, *>)?.mapNotNull { (k, v) ->
        if (k != null && v != null) k.toString() to v.toString() else null
    }?.toMap() ?: existing?.memberRoles ?: emptyMap()

    return (existing ?: Household()).copy(
        id = docId,
        name = getString("name") ?: existing?.name ?: "Το Εξοδολόγιό μου",
        ownerId = getString("ownerId") ?: existing?.ownerId ?: "",
        inviteCode = getString("inviteCode") ?: existing?.inviteCode ?: "",
        memberUids = members,
        invitedEmails = invited,
        memberRoles = rolesMap,
        pendingInviteRole = getString("pendingInviteRole") ?: existing?.pendingInviteRole ?: "CONTRIBUTOR",
        isPremium = isPrem,
        createdAt = getTimestamp("createdAt") ?: existing?.createdAt,
        updatedAt = getTimestamp("updatedAt") ?: existing?.updatedAt
    )
}

fun DocumentSnapshot.toSafeUserProfile(): UserProfile {
    val existing = try {
        this.toObject(UserProfile::class.java)
    } catch (_: Exception) {
        null
    }
    val hids = (get("householdIds") as? List<*>)?.mapNotNull { it?.toString() } ?: existing?.householdIds ?: emptyList()
    val rawPrem = get("isPremium") ?: get("premium")
    val isPrem = when (rawPrem) {
        is Boolean -> rawPrem
        is String -> rawPrem.toBoolean()
        is Number -> rawPrem.toInt() != 0
        else -> existing?.isPremium ?: false
    }
    val uidVal = getString("uid")?.ifBlank { this.id } ?: this.id

    return (existing ?: UserProfile()).copy(
        uid = uidVal,
        email = getString("email") ?: existing?.email ?: "",
        displayName = getString("displayName") ?: existing?.displayName ?: "",
        firstName = getString("firstName") ?: existing?.firstName ?: "",
        lastName = getString("lastName") ?: existing?.lastName ?: "",
        birthDate = getString("birthDate") ?: existing?.birthDate ?: "",
        phoneNumber = getString("phoneNumber") ?: existing?.phoneNumber ?: "",
        occupation = getString("occupation") ?: existing?.occupation ?: "",
        city = getString("city") ?: existing?.city ?: "",
        householdId = getString("householdId") ?: existing?.householdId ?: "",
        householdIds = hids,
        isPremium = isPrem,
        photoUrl = getString("photoUrl") ?: existing?.photoUrl ?: "",
        authProvider = getString("authProvider") ?: existing?.authProvider ?: "password",
        createdAt = getTimestamp("createdAt") ?: existing?.createdAt,
        updatedAt = getTimestamp("updatedAt") ?: existing?.updatedAt
    )
}

fun DocumentSnapshot.toSafeSavingsGoal(): FirebaseSavingsGoal {
    val existing = try {
        this.toObject(FirebaseSavingsGoal::class.java)
    } catch (_: Exception) {
        null
    }
    val target = getDouble("targetAmount") ?: (get("targetAmount") as? Number)?.toDouble() ?: existing?.targetAmount ?: 0.0
    val current = getDouble("currentAmount") ?: (get("currentAmount") as? Number)?.toDouble() ?: existing?.currentAmount ?: 0.0
    val color = getLong("colorHex") ?: (get("colorHex") as? Number)?.toLong() ?: existing?.colorHex ?: 0xFF059669
    val docId = getString("id")?.ifBlank { this.id } ?: this.id

    return (existing ?: FirebaseSavingsGoal()).copy(
        id = docId,
        householdId = getString("householdId") ?: existing?.householdId ?: "",
        userId = getString("userId") ?: existing?.userId ?: "",
        title = getString("title") ?: existing?.title ?: "",
        targetAmount = target,
        currentAmount = current,
        deadlineDate = getString("deadlineDate") ?: existing?.deadlineDate ?: "",
        iconKey = getString("iconKey") ?: existing?.iconKey ?: "vacation",
        colorHex = color,
        note = getString("note") ?: existing?.note ?: "",
        createdAt = getTimestamp("createdAt") ?: existing?.createdAt
    )
}

fun DocumentSnapshot.toSafeRecurringExpense(): FirebaseRecurringExpense {
    val existing = try {
        this.toObject(FirebaseRecurringExpense::class.java)
    } catch (_: Exception) {
        null
    }
    val amt = getDouble("amount") ?: (get("amount") as? Number)?.toDouble() ?: existing?.amount ?: 0.0
    val catId = getLong("categoryId") ?: (get("categoryId") as? Number)?.toLong() ?: existing?.categoryId ?: 0L
    val day = getLong("dayOfMonth")?.toInt() ?: (get("dayOfMonth") as? Number)?.toInt() ?: existing?.dayOfMonth ?: 1
    val rawAct = get("isActive") ?: get("active")
    val isAct = when (rawAct) {
        is Boolean -> rawAct
        is String -> rawAct.toBoolean()
        is Number -> rawAct.toInt() != 0
        else -> existing?.isActive ?: true
    }
    val rawPayerId = get("payerMemberId")
    val payerId = when (rawPayerId) {
        is Number -> rawPayerId.toLong()
        is String -> rawPayerId.toLongOrNull() ?: 0L
        else -> existing?.payerMemberId ?: 0L
    }
    val docId = getString("id")?.ifBlank { this.id } ?: this.id

    return (existing ?: FirebaseRecurringExpense()).copy(
        id = docId,
        householdId = getString("householdId") ?: existing?.householdId ?: "",
        userId = getString("userId") ?: existing?.userId ?: "",
        title = getString("title") ?: existing?.title ?: "",
        amount = amt,
        categoryId = catId,
        categoryName = getString("categoryName") ?: existing?.categoryName ?: "",
        frequency = getString("frequency") ?: existing?.frequency ?: "MONTHLY",
        dayOfMonth = day,
        splitType = getString("splitType") ?: existing?.splitType ?: "ALL_FAMILY",
        memberIdsCsv = getString("memberIdsCsv") ?: existing?.memberIdsCsv ?: "",
        memberNamesDisplay = getString("memberNamesDisplay") ?: existing?.memberNamesDisplay ?: "Κοινό Οικογένειας",
        note = getString("note") ?: existing?.note ?: "",
        isActive = isAct,
        lastGeneratedMonth = getString("lastGeneratedMonth") ?: existing?.lastGeneratedMonth ?: "",
        payerMemberId = payerId,
        payerMemberName = getString("payerMemberName") ?: existing?.payerMemberName ?: "",
        payerMemberIdsCsv = getString("payerMemberIdsCsv") ?: existing?.payerMemberIdsCsv ?: "",
        payerAmountsCsv = getString("payerAmountsCsv") ?: existing?.payerAmountsCsv ?: "",
        createdAt = getTimestamp("createdAt") ?: existing?.createdAt
    )
}
