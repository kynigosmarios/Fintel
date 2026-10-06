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
import androidx.compose.material.icons.filled.Euro
import androidx.compose.material.icons.filled.Group
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
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald700
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ManualExpenseDialog(
    expenseToEdit: Expense?,
    categories: List<ExpenseCategory>,
    members: List<FamilyMember>,
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
        note: String
    ) -> Unit
) {
    var title by remember { mutableStateOf(expenseToEdit?.title ?: "") }
    var amountText by remember {
        mutableStateOf(expenseToEdit?.let { String.format(Locale.US, "%.2f", it.amount) } ?: "")
    }
    var selectedCategoryId by remember {
        mutableStateOf(expenseToEdit?.categoryId ?: categories.firstOrNull()?.id ?: 1L)
    }
    var selectedTimestamp by remember {
        mutableLongStateOf(expenseToEdit?.timestamp ?: System.currentTimeMillis())
    }
    var showDatePicker by remember { mutableStateOf(false) }

    var isAllFamily by remember {
        val hasSpecific = expenseToEdit != null && (
            expenseToEdit.splitType == "SPECIFIC_MEMBERS" ||
            (expenseToEdit.memberNamesDisplay.isNotBlank() && !expenseToEdit.memberNamesDisplay.equals("Κοινό Οικογένειας", ignoreCase = true))
        )
        mutableStateOf(if (expenseToEdit != null) !hasSpecific else true)
    }

    val selectedMemberIds = remember {
        val list = mutableStateListOf<Long>()
        if (expenseToEdit != null) {
            val parsedIds = expenseToEdit.memberIdsCsv.split(",").mapNotNull { it.trim().toLongOrNull() }
            if (parsedIds.isNotEmpty()) {
                list.addAll(parsedIds)
            } else if (expenseToEdit.memberNamesDisplay.isNotBlank() && !expenseToEdit.memberNamesDisplay.equals("Κοινό Οικογένειας", ignoreCase = true)) {
                val matched = members.filter { m -> expenseToEdit.memberNamesDisplay.contains(m.name, ignoreCase = true) }
                if (matched.isNotEmpty()) list.addAll(matched.map { it.id }) else list.addAll(members.map { it.id })
            } else {
                list.addAll(members.map { it.id })
            }
        } else {
            list.addAll(members.map { it.id })
        }
        list
    }

    var note by remember { mutableStateOf(expenseToEdit?.note ?: "") }
    var errorText by remember { mutableStateOf<String?>(null) }

    val currentAmount = amountText.toDoubleOrNull() ?: 0.0
    val activeMemberCount = if (isAllFamily) members.size else selectedMemberIds.size.coerceAtLeast(1)
    val splitPerPerson = currentAmount / activeMemberCount

    // Date formatting in Greek
    val displayDateFormat = remember { SimpleDateFormat("EEEE, d MMMM yyyy", Locale.forLanguageTag("el-GR")) }
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
                    Text("Επιλογή", color = Emerald700, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Ακύρωση")
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
                        text = if (expenseToEdit == null) "Νέο Έξοδο" else "Επεξεργασία Εξόδου",
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
                                contentDescription = "Κλείσιμο",
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
                        label = { Text("Περιγραφή εξόδου (π.χ. Σούπερ Μάρκετ)") },
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
                        label = { Text("Ποσό σε Ευρώ (€)") },
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

                    // Date Picker Selector (Requirement 1.1)
                    Text(
                        text = "Ημερομηνία Εξόδου:",
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
                                        text = if (isSelectedDateToday) "Σήμερα" else "Ημερομηνία",
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
                                    text = "Αλλαγή",
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
                        text = "Κατηγορία:",
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
                                onClick = { selectedCategoryId = cat.id },
                                label = { Text(cat.name, fontSize = 12.sp) },
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

                    // Member Split Section
                    Text(
                        text = "Καταμερισμός Εξόδου:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
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
                                text = "Κοινό έξοδο όλου του σπιτιού (Οικογένεια)",
                                fontSize = 13.sp,
                                fontWeight = if (isAllFamily) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }

                    if (!isAllFamily) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Επιλέξτε μέλη που συμμετέχουν:",
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

                                FilterChip(
                                    selected = isChecked,
                                    onClick = {
                                        if (isChecked) {
                                            if (selectedMemberIds.size > 1) {
                                                selectedMemberIds.remove(member.id)
                                            }
                                        } else {
                                            selectedMemberIds.add(member.id)
                                        }
                                    },
                                    label = { Text("${member.name} (${member.role})", fontSize = 12.sp) },
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
                                    text = "Υπολογισμός διαμοιρασμού: ",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = String.format(Locale.US, "%.2f € / άτομο", splitPerPerson),
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
                        label = { Text("Σημειώσεις (προαιρετικό)") },
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

                // 3. Fixed Sticky Bottom Action Bar (Cancel + Save) - ALWAYS visible on screen! (Requirement 1.2)
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
                        Text("Ακύρωση")
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = {
                            val parsedAmount = amountText.toDoubleOrNull()
                            if (title.isBlank()) {
                                errorText = "Παρακαλώ συμπληρώστε τίτλο εξόδου"
                                return@Button
                            }
                            if (parsedAmount == null || parsedAmount <= 0) {
                                errorText = "Παρακαλώ εισάγετε έγκυρο ποσό"
                                return@Button
                            }

                            val chosenCat = categories.find { it.id == selectedCategoryId }
                                ?: categories.first()

                            val splitType = if (isAllFamily) "ALL_FAMILY" else "SPECIFIC_MEMBERS"
                            val finalMemberIds = if (isAllFamily) {
                                members.map { it.id }
                            } else {
                                selectedMemberIds.toList()
                            }

                            val memberDisplay = if (isAllFamily) {
                                "Κοινό Οικογένειας"
                            } else {
                                val active = members.filter { finalMemberIds.contains(it.id) }
                                active.joinToString(" & ") { it.name }
                            }

                            val finalTimestamp = if (isSelectedDateToday && expenseToEdit == null) {
                                System.currentTimeMillis()
                            } else {
                                selectedTimestamp
                            }

                            onSave(
                                expenseToEdit?.id ?: 0L,
                                title.trim(),
                                parsedAmount,
                                chosenCat.id,
                                chosenCat.name,
                                finalTimestamp,
                                splitType,
                                finalMemberIds,
                                memberDisplay,
                                note.trim()
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald700),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("save_expense_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Αποθήκευση",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}
