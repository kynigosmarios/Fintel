package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Euro
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.entity.Expense
import com.example.data.entity.ExpenseCategory
import com.example.data.entity.FamilyMember
import com.example.data.entity.getPayerShares
import com.example.ui.theme.Amber700
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald700
import com.example.util.Localization
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ManualExpenseDialog(
    expenseToEdit: Expense?,
    categories: List<ExpenseCategory>,
    members: List<FamilyMember>,
    appLanguage: String = "EL",
    onDismiss: () -> Unit,
    onSave: (
        id: Long,
        title: String,
        amount: Double,
        categoryId: Long,
        categoryName: String,
        timestamp: Long,
        splitType: String,
        memberIds: List<Long>,
        memberNamesDisplay: String,
        note: String,
        payerMemberId: Long,
        payerMemberName: String,
        payerMemberIdsCsv: String,
        payerAmountsCsv: String
    ) -> Unit
) {
    val lang = Localization.Language.fromCode(appLanguage)

    // Multi-payer selection state
    val selectedPayerIds = remember(expenseToEdit, members) {
        val list = mutableStateListOf<Long>()
        if (expenseToEdit != null) {
            val parsedIds = if (expenseToEdit.payerAmountsCsv.isNotBlank()) {
                expenseToEdit.payerAmountsCsv.split(",").mapNotNull { it.split(":").firstOrNull()?.trim()?.toLongOrNull() }
            } else if (expenseToEdit.payerMemberIdsCsv.isNotBlank()) {
                expenseToEdit.payerMemberIdsCsv.split(",").mapNotNull { it.trim().toLongOrNull() }
            } else if (expenseToEdit.payerMemberId != 0L) {
                listOf(expenseToEdit.payerMemberId)
            } else emptyList()

            val valid = parsedIds.filter { id -> members.any { it.id == id } }
            if (valid.isNotEmpty()) {
                list.addAll(valid)
            } else if (expenseToEdit.payerMemberName.isNotBlank()) {
                val cleanPayer = expenseToEdit.payerMemberName.replace(Regex("\\([^)]*\\)"), "").trim()
                if (cleanPayer.equals("Γονείς", ignoreCase = true) || cleanPayer.equals("Parents", ignoreCase = true)) {
                    val parents = members.filter { it.role.equals("Γονέας", ignoreCase = true) }
                    if (parents.isNotEmpty()) list.addAll(parents.map { it.id })
                } else {
                    val names = cleanPayer.split(Regex("[&,/]+")).map { it.trim() }.filter { it.isNotBlank() }
                    val matched = members.filter { m -> names.any { n -> n.equals(m.name, ignoreCase = true) || cleanPayer.contains(m.name, ignoreCase = true) } }
                    if (matched.isNotEmpty()) {
                        list.addAll(matched.map { it.id })
                    } else {
                        val def = members.firstOrNull { it.role.equals("Γονέας", ignoreCase = true) } ?: members.firstOrNull()
                        if (def != null) list.add(def.id)
                    }
                }
            } else {
                val def = members.firstOrNull { it.role.equals("Γονέας", ignoreCase = true) } ?: members.firstOrNull()
                if (def != null) list.add(def.id)
            }
        } else {
            val def = members.firstOrNull { it.role.equals("Γονέας", ignoreCase = true) } ?: members.firstOrNull()
            if (def != null) list.add(def.id)
        }
        list
    }

    var isCustomPayerSplit by remember(expenseToEdit) {
        val hasCustom = if (expenseToEdit != null && expenseToEdit.payerAmountsCsv.isNotBlank()) {
            val parts = expenseToEdit.payerAmountsCsv.split(",")
            val amounts = parts.mapNotNull { it.split(":").getOrNull(1)?.trim()?.toDoubleOrNull() }
            if (amounts.size >= 2) {
                val first = amounts[0]
                amounts.any { Math.abs(it - first) > 0.01 }
            } else false
        } else false
        mutableStateOf(hasCustom)
    }

    val customPayerAmounts = remember(expenseToEdit) {
        val map = mutableStateMapOf<Long, String>()
        if (expenseToEdit != null && expenseToEdit.payerAmountsCsv.isNotBlank()) {
            expenseToEdit.payerAmountsCsv.split(",").forEach { entry ->
                val parts = entry.split(":")
                if (parts.size == 2) {
                    val id = parts[0].trim().toLongOrNull()
                    val amt = parts[1].trim().toDoubleOrNull()
                    if (id != null && amt != null) {
                        map[id] = String.format(Locale.US, "%.2f", amt)
                    }
                }
            }
        }
        map
    }

    var title by remember(expenseToEdit) { mutableStateOf(expenseToEdit?.title ?: "") }
    var amountText by remember(expenseToEdit) {
        mutableStateOf(expenseToEdit?.let { String.format(Locale.US, "%.2f", it.amount) } ?: "")
    }
    var selectedCategoryId by remember(expenseToEdit, categories) {
        val initialId = if (expenseToEdit != null) {
            if (expenseToEdit.categoryId == 0L || expenseToEdit.categoryName.isBlank()) 0L
            else if (categories.any { it.id == expenseToEdit.categoryId }) expenseToEdit.categoryId
            else 0L
        } else {
            categories.firstOrNull()?.id ?: 0L
        }
        mutableStateOf(initialId)
    }
    var selectedTimestamp by remember(expenseToEdit) {
        mutableLongStateOf(expenseToEdit?.timestamp ?: System.currentTimeMillis())
    }
    var showDatePicker by remember { mutableStateOf(false) }

    var isAllFamily by remember(expenseToEdit) {
        val hasSpecific = expenseToEdit != null && (
            expenseToEdit.splitType == "SPECIFIC_MEMBERS" ||
            expenseToEdit.memberIdsCsv.isNotBlank() ||
            (expenseToEdit.memberNamesDisplay.isNotBlank() && !expenseToEdit.memberNamesDisplay.equals("Κοινό Οικογένειας", ignoreCase = true) && !expenseToEdit.memberNamesDisplay.equals("Family Shared", ignoreCase = true))
        )
        val isEmptyMember = expenseToEdit != null && expenseToEdit.memberIdsCsv.isBlank() && expenseToEdit.memberNamesDisplay.isBlank()
        mutableStateOf(if (expenseToEdit != null) (!hasSpecific && !isEmptyMember) else true)
    }

    val selectedMemberIds = remember(expenseToEdit, members) {
        val list = mutableStateListOf<Long>()
        if (expenseToEdit != null) {
            val parsedIds = expenseToEdit.memberIdsCsv.split(",").mapNotNull { it.trim().toLongOrNull() }
            val validIds = parsedIds.filter { id -> members.any { it.id == id } }
            if (validIds.isNotEmpty()) {
                list.addAll(validIds)
            } else if (expenseToEdit.memberNamesDisplay.isNotBlank() && !expenseToEdit.memberNamesDisplay.equals("Κοινό Οικογένειας", ignoreCase = true) && !expenseToEdit.memberNamesDisplay.equals("Family Shared", ignoreCase = true)) {
                val matched = members.filter { m -> expenseToEdit.memberNamesDisplay.contains(m.name, ignoreCase = true) }
                list.addAll(matched.map { it.id })
            }
        } else {
            list.addAll(members.map { it.id })
        }
        list
    }

    var note by remember(expenseToEdit) { mutableStateOf(expenseToEdit?.note ?: "") }
    var errorText by remember(expenseToEdit) { mutableStateOf<String?>(null) }

    val currentAmount = amountText.toDoubleOrNull() ?: 0.0
    val activeMemberCount = if (isAllFamily) members.size else selectedMemberIds.size.coerceAtLeast(1)
    val splitPerPerson = currentAmount / activeMemberCount

    // Date formatting localized
    val displayDateFormat = remember(lang) { SimpleDateFormat("EEEE, d MMMM yyyy", Localization.getDateLocale(lang)) }
    val todayDateString = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val isSelectedDateToday = remember(selectedTimestamp) {
        val selectedString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(selectedTimestamp))
        selectedString == todayDateString
    }

    // Material 3 Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedTimestamp
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utcMillis ->
                            val cal = java.util.Calendar.getInstance()
                            val curHour = cal.get(java.util.Calendar.HOUR_OF_DAY)
                            val curMinute = cal.get(java.util.Calendar.MINUTE)
                            val curSecond = cal.get(java.util.Calendar.SECOND)

                            val utcCal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = utcMillis
                            }

                            val targetCal = java.util.Calendar.getInstance().apply {
                                set(java.util.Calendar.YEAR, utcCal.get(java.util.Calendar.YEAR))
                                set(java.util.Calendar.MONTH, utcCal.get(java.util.Calendar.MONTH))
                                set(java.util.Calendar.DAY_OF_MONTH, utcCal.get(java.util.Calendar.DAY_OF_MONTH))
                                set(java.util.Calendar.HOUR_OF_DAY, curHour)
                                set(java.util.Calendar.MINUTE, curMinute)
                                set(java.util.Calendar.SECOND, curSecond)
                            }
                            selectedTimestamp = targetCal.timeInMillis
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(Localization.select(lang), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(Localization.cancel(lang))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .padding(vertical = 12.dp)
                .testTag("manual_expense_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // 1. Fixed Header (Title + Close Button)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (expenseToEdit == null) Localization.recordExpenseTitle(lang) else Localization.editExpenseTitle(lang),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Emerald700
                    )
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .size(32.dp)
                            .clickable { onDismiss() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = Localization.close(lang),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Scrollable Content Area (Middle)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Title Input
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            errorText = null
                        },
                        label = { Text(Localization.expenseDescLabel(lang)) },
                        placeholder = { Text(Localization.expenseDescPlaceholder(lang)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("expense_title_input"),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Emerald700)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Amount Input
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = {
                            amountText = it.replace(',', '.')
                            errorText = null
                        },
                        label = { Text(Localization.expenseAmountLabel(lang)) },
                        leadingIcon = {
                            Icon(Icons.Default.Euro, contentDescription = null, tint = Emerald700)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("expense_amount_input"),
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Emerald700)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Date Picker Selector
                    Text(
                        text = Localization.dateLabel(lang),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true }
                            .testTag("expense_date_picker_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = Emerald700,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (isSelectedDateToday) Localization.today(lang) else Localization.date(lang),
                                        fontSize = 11.sp,
                                        color = Emerald700,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = displayDateFormat.format(Date(selectedTimestamp)).replaceFirstChar { it.uppercase() },
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Emerald100
                            ) {
                                Text(
                                    text = Localization.changeAction(lang),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Emerald700,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Category Selection
                    Text(
                        text = Localization.categoryLabel(lang),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { cat ->
                            val isSelected = cat.id == selectedCategoryId
                            val catColor = Color(cat.colorHex)
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategoryId = if (isSelected) 0L else cat.id },
                                label = { Text(Localization.translateCategory(cat.name, lang), fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = CategoryIconHelper.getIcon(cat.iconKey),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isSelected) Emerald700 else catColor
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Emerald700.copy(alpha = 0.15f),
                                    selectedLabelColor = Emerald700
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 1. Payer Section (Who paid)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreditCard,
                            contentDescription = null,
                            tint = Emerald700,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                        Text(
                            text = Localization.payersSectionTitle(lang),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = Localization.payersSubtitle(lang),
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        members.forEach { member ->
                            val isPayer = selectedPayerIds.contains(member.id)
                            val avatarColor = Color(member.avatarColorHex)
                            val translatedRole = Localization.translateRole(member.role, lang)

                            FilterChip(
                                selected = isPayer,
                                onClick = {
                                    if (isPayer) {
                                        if (selectedPayerIds.size > 1) {
                                            selectedPayerIds.remove(member.id)
                                            customPayerAmounts.remove(member.id)
                                        }
                                    } else {
                                        selectedPayerIds.add(member.id)
                                        if (isCustomPayerSplit) {
                                            val currentAllocated = selectedPayerIds.sumOf { customPayerAmounts[it]?.toDoubleOrNull() ?: 0.0 }
                                            val remaining = (currentAmount - currentAllocated).coerceAtLeast(0.0)
                                            customPayerAmounts[member.id] = if (remaining > 0) String.format(Locale.US, "%.2f", remaining) else "0.00"
                                        }
                                    }
                                },
                                label = { Text("${member.name} ($translatedRole)", fontSize = 12.sp) },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(avatarColor)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Emerald700.copy(alpha = 0.2f),
                                    selectedLabelColor = Emerald700
                                )
                            )
                        }
                    }

                    // Multi-payer allocation details card (when 2 or more payers selected)
                    if (selectedPayerIds.size > 1) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            ),
                            border = BorderStroke(1.dp, Emerald700.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                // Full-width Segmented Tab Control
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                                        .padding(3.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (!isCustomPayerSplit) Emerald700 else Color.Transparent)
                                            .clickable { isCustomPayerSplit = false }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = Localization.payerSplitModeEqual(lang),
                                            fontSize = 12.5.sp,
                                            fontWeight = if (!isCustomPayerSplit) FontWeight.Bold else FontWeight.Medium,
                                            color = if (!isCustomPayerSplit) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isCustomPayerSplit) Emerald700 else Color.Transparent)
                                            .clickable {
                                                isCustomPayerSplit = true
                                                val share = if (currentAmount > 0) currentAmount / selectedPayerIds.size else 0.0
                                                selectedPayerIds.forEach { id ->
                                                    if (customPayerAmounts[id].isNullOrBlank()) {
                                                        customPayerAmounts[id] = String.format(Locale.US, "%.2f", share)
                                                    }
                                                }
                                            }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = Localization.payerSplitModeCustom(lang),
                                            fontSize = 12.5.sp,
                                            fontWeight = if (isCustomPayerSplit) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isCustomPayerSplit) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                if (!isCustomPayerSplit) {
                                    // Equal split display
                                    val equalShare = if (currentAmount > 0) currentAmount / selectedPayerIds.size else 0.0

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Emerald700.copy(alpha = 0.08f))
                                            .padding(horizontal = 12.dp, vertical = 9.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (lang == Localization.Language.ENGLISH) "Share per person" else "Μερίδιο ανά άτομο",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = String.format(Locale.US, "%.2f €", equalShare),
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Emerald700
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        selectedPayerIds.forEach { pid ->
                                            val m = members.find { it.id == pid } ?: return@forEach
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(10.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(m.avatarColorHex))
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = m.name,
                                                        fontSize = 12.5.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = " (${Localization.translateRole(m.role, lang)})",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Text(
                                                    text = String.format(Locale.US, "%.2f €", equalShare),
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Emerald700
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    // Custom amounts per payer inputs
                                    Text(
                                        text = if (lang == Localization.Language.ENGLISH) "Enter amount paid per member:" else "Ορίστε το ποσό που πλήρωσε κάθε μέλος:",
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        selectedPayerIds.forEach { pid ->
                                            val m = members.find { it.id == pid } ?: return@forEach
                                            val currentVal = customPayerAmounts[pid] ?: ""

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    modifier = Modifier.weight(1f),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(10.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(m.avatarColorHex))
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = m.name,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = " (${Localization.translateRole(m.role, lang)})",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }

                                                OutlinedTextField(
                                                    value = currentVal,
                                                    onValueChange = { input ->
                                                        customPayerAmounts[pid] = input.replace(',', '.')
                                                    },
                                                    modifier = Modifier.width(115.dp),
                                                    shape = RoundedCornerShape(10.dp),
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                    singleLine = true,
                                                    trailingIcon = {
                                                        Text("€", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Emerald700)
                                                    },
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = Emerald700,
                                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                                    )
                                                )
                                            }
                                        }

                                        // Balance & Auto-completion calculation
                                        val totalAllocated = selectedPayerIds.sumOf { customPayerAmounts[it]?.toDoubleOrNull() ?: 0.0 }
                                        val diff = currentAmount - totalAllocated

                                        Spacer(modifier = Modifier.height(8.dp))

                                        if (currentAmount > 0.0) {
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = when {
                                                    Math.abs(diff) < 0.01 -> Emerald700.copy(alpha = 0.08f)
                                                    diff > 0.01 -> Amber700.copy(alpha = 0.08f)
                                                    else -> MaterialTheme.colorScheme.error.copy(alpha = 0.08f)
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                if (Math.abs(diff) < 0.01) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.Check, contentDescription = null, tint = Emerald700, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = Localization.payerFullyCovered(lang),
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Emerald700
                                                        )
                                                    }
                                                } else if (diff > 0.01) {
                                                    Text(
                                                        text = Localization.payerRemaining(lang, String.format(Locale.US, "%.2f €", diff)),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Amber700
                                                    )

                                                    // Quick button to allocate remainder to the second payer if 2 payers
                                                    if (selectedPayerIds.size == 2) {
                                                        val targetId = selectedPayerIds[1]
                                                        val targetMember = members.find { it.id == targetId }
                                                        if (targetMember != null) {
                                                            Surface(
                                                                shape = RoundedCornerShape(6.dp),
                                                                color = Amber700.copy(alpha = 0.12f),
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(6.dp))
                                                                    .clickable {
                                                                        val cur = customPayerAmounts[targetId]?.toDoubleOrNull() ?: 0.0
                                                                        customPayerAmounts[targetId] = String.format(Locale.US, "%.2f", cur + diff)
                                                                    }
                                                            ) {
                                                                Text(
                                                                    text = Localization.payerQuickBalance(lang, targetMember.name, String.format(Locale.US, "%.2f €", diff)),
                                                                    fontSize = 10.5.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = Amber700,
                                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    Text(
                                                        text = Localization.payerExceeded(lang, String.format(Locale.US, "%.2f €", -diff)),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.error
                                                    )
                                                }

                                                Text(
                                                    text = "${String.format(Locale.US, "%.2f", totalAllocated)} / ${String.format(Locale.US, "%.2f €", currentAmount)}",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. Beneficiary Section (Who the expense concerns)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (lang == Localization.Language.ENGLISH) "Who is this expense for?" else "Σε ποιον αφορά το έξοδο;",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (lang == Localization.Language.ENGLISH) "e.g. Choose children to track expenses per child" else "π.χ. Επιλέξτε τα παιδιά για να γνωρίζετε πόσα αναλογούν σε αυτά",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // All Family Toggle
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isAllFamily) Emerald700.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isAllFamily = !isAllFamily }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isAllFamily,
                                onCheckedChange = { isAllFamily = it }
                            )
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = null,
                                tint = Emerald700,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (lang == Localization.Language.ENGLISH) "Common to entire household (Family)" else "Κοινό έξοδο όλου του σπιτιού (Οικογένεια)",
                                fontSize = 13.sp,
                                fontWeight = if (isAllFamily) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }

                    if (!isAllFamily) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (lang == Localization.Language.ENGLISH) "Select participating members:" else "Επιλέξτε μέλη που συμμετέχουν:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            members.forEach { member ->
                                val isChecked = selectedMemberIds.contains(member.id)
                                val avatarColor = Color(member.avatarColorHex)
                                val translatedRole = Localization.translateRole(member.role, lang)

                                FilterChip(
                                    selected = isChecked,
                                    onClick = {
                                        if (isChecked) {
                                            selectedMemberIds.remove(member.id)
                                        } else {
                                            selectedMemberIds.add(member.id)
                                        }
                                    },
                                    label = { Text("${member.name} ($translatedRole)", fontSize = 12.sp) },
                                    leadingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(avatarColor)
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = avatarColor.copy(alpha = 0.2f)
                                    )
                                )
                            }
                        }
                    }

                    // Live split preview
                    if (currentAmount > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Emerald700.copy(alpha = 0.08f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (lang == Localization.Language.ENGLISH) "Split calculation: " else "Υπολογισμός διαμοιρασμού: ",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (lang == Localization.Language.ENGLISH)
                                        String.format(Locale.US, "%.2f € / person", splitPerPerson)
                                    else
                                        String.format(Locale.US, "%.2f € / άτομο", splitPerPerson),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Emerald700
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Optional note
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text(Localization.noteLabel(lang)) },
                        placeholder = { Text(Localization.notePlaceholder(lang)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Emerald700)
                    )

                    if (errorText != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorText ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 3. Fixed Sticky Bottom Action Bar (Cancel + Save)
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    modifier = Modifier.padding(vertical = 10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(Localization.cancel(lang))
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = {
                            val parsedAmount = amountText.toDoubleOrNull()
                            if (title.isBlank()) {
                                errorText = Localization.fillDescriptionError(lang)
                                return@Button
                            }
                            if (parsedAmount == null || parsedAmount <= 0) {
                                errorText = Localization.fillAmountError(lang)
                                return@Button
                            }

                            val chosenCat = categories.find { it.id == selectedCategoryId }
                            val catId = chosenCat?.id ?: 0L
                            val catName = chosenCat?.name ?: ""

                            val splitType = if (isAllFamily) "ALL_FAMILY" else "SPECIFIC_MEMBERS"
                            val finalMemberIds = if (isAllFamily) {
                                members.map { it.id }
                            } else {
                                selectedMemberIds.toList()
                            }

                            val memberDisplay = if (isAllFamily) {
                                if (lang == Localization.Language.ENGLISH) "Family Shared" else "Κοινό Οικογένειας"
                            } else {
                                val active = members.filter { finalMemberIds.contains(it.id) }
                                active.joinToString(" & ") { it.name }
                            }

                            val finalTimestamp = if (isSelectedDateToday && expenseToEdit == null) {
                                System.currentTimeMillis()
                            } else {
                                selectedTimestamp
                            }

                            val finalPayerIds = selectedPayerIds.toList().ifEmpty {
                                val def = members.firstOrNull { it.role.equals("Γονέας", ignoreCase = true) } ?: members.firstOrNull()
                                if (def != null) listOf(def.id) else emptyList()
                            }

                            val finalPayerMember: FamilyMember? = if (finalPayerIds.isNotEmpty()) members.find { it.id == finalPayerIds[0] } else null
                            val finalPayerId = finalPayerMember?.id ?: 0L
                            val finalPayerMemberIdsCsv = finalPayerIds.joinToString(",")

                            val finalPayerAmountsCsv: String
                            val finalPayerName: String

                            if (finalPayerIds.size <= 1) {
                                finalPayerAmountsCsv = if (finalPayerIds.isNotEmpty()) "${finalPayerIds[0]}:${String.format(Locale.US, "%.2f", parsedAmount)}" else ""
                                finalPayerName = finalPayerMember?.name ?: ""
                            } else {
                                if (isCustomPayerSplit) {
                                    finalPayerAmountsCsv = finalPayerIds.joinToString(",") { id ->
                                        val amt = customPayerAmounts[id]?.toDoubleOrNull() ?: (parsedAmount / finalPayerIds.size)
                                        String.format(Locale.US, "%d:%.2f", id, amt)
                                    }
                                    finalPayerName = finalPayerIds.mapNotNull { id -> members.find { it.id == id } }
                                        .joinToString(" & ") { m ->
                                            val amt = customPayerAmounts[m.id]?.toDoubleOrNull() ?: (parsedAmount / finalPayerIds.size)
                                            "${m.name} (${String.format(Locale.US, "%.2f €", amt)})"
                                        }
                                } else {
                                    val perPerson = parsedAmount / finalPayerIds.size
                                    finalPayerAmountsCsv = finalPayerIds.joinToString(",") { id ->
                                        String.format(Locale.US, "%d:%.2f", id, perPerson)
                                    }
                                    val names = finalPayerIds.mapNotNull { id -> members.find { it.id == id }?.name }
                                    finalPayerName = "${names.joinToString(" & ")} (από ${String.format(Locale.US, "%.2f €", perPerson)})"
                                }
                            }

                            onSave(
                                expenseToEdit?.id ?: 0L,
                                title.trim(),
                                parsedAmount,
                                catId,
                                catName,
                                finalTimestamp,
                                splitType,
                                finalMemberIds,
                                memberDisplay,
                                note.trim(),
                                finalPayerId,
                                finalPayerName,
                                finalPayerMemberIdsCsv,
                                finalPayerAmountsCsv
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald700, contentColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("save_expense_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = Localization.save(lang),
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}
