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
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import com.example.data.entity.FamilyMember
import com.example.data.entity.Income
import com.example.util.Localization
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val DEFAULT_INCOME_CATEGORIES = listOf(
    "Μισθός",
    "Ενοίκιο",
    "Επιχείρηση / Freelance",
    "Επιδότηση",
    "Bonus / Δώρο",
    "Επενδύσεις",
    "Άλλο"
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ManualIncomeDialog(
    incomeToEdit: Income?,
    members: List<FamilyMember>,
    appLanguage: String = "EL",
    onDismiss: () -> Unit,
    onSave: (
        id: Long,
        title: String,
        amount: Double,
        memberId: Long,
        memberName: String,
        category: String,
        timestamp: Long,
        note: String
    ) -> Unit
) {
    val lang = Localization.Language.fromCode(appLanguage)

    var title by remember(incomeToEdit) { mutableStateOf(incomeToEdit?.title ?: "") }
    var amountText by remember(incomeToEdit) {
        mutableStateOf(incomeToEdit?.let { String.format(Locale.US, "%.2f", it.amount) } ?: "")
    }
    var selectedCategory by remember(incomeToEdit) {
        mutableStateOf(incomeToEdit?.category ?: "Μισθός")
    }
    var selectedMemberId by remember(incomeToEdit, members) {
        val initialId = if (incomeToEdit != null) {
            if (incomeToEdit.memberId == 0L || incomeToEdit.memberName.isBlank()) 0L
            else if (members.any { it.id == incomeToEdit.memberId }) incomeToEdit.memberId
            else 0L
        } else {
            members.firstOrNull()?.id ?: 0L
        }
        mutableStateOf(initialId)
    }
    var selectedTimestamp by remember(incomeToEdit) {
        mutableLongStateOf(incomeToEdit?.timestamp ?: System.currentTimeMillis())
    }
    var note by remember(incomeToEdit) { mutableStateOf(incomeToEdit?.note ?: "") }

    var showDatePicker by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedTimestamp
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
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
                }) {
                    Text(Localization.select(lang))
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
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp)
                .testTag("manual_income_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF059669).copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = Color(0xFF059669),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (incomeToEdit == null) Localization.recordIncomeTitle(lang) else Localization.editIncomeTitle(lang),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = Localization.close(lang), modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        errorMessage = null
                    },
                    label = { Text(Localization.incomeDescLabel(lang)) },
                    placeholder = { Text(Localization.incomeDescPlaceholder(lang)) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("income_title_input")
                )

                // Quick Title Suggestions
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("Μισθός", "Ενοίκιο", "Freelance", "Επιδότηση", "Bonus").forEach { sug ->
                        val translatedSug = Localization.translateCategory(sug, lang)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.clickable {
                                title = translatedSug
                                if (selectedCategory == "Μισθός" && sug != "Μισθός") {
                                    selectedCategory = when (sug) {
                                        "Ενοίκιο" -> "Ενοίκιο"
                                        "Freelance" -> "Επιχείρηση / Freelance"
                                        "Επιδότηση" -> "Επιδότηση"
                                        "Bonus" -> "Bonus / Δώρο"
                                        else -> "Άλλο"
                                    }
                                }
                            }
                        ) {
                            Text(
                                text = "+ $translatedSug",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Amount Input
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it.replace(',', '.')
                        errorMessage = null
                    },
                    label = { Text(Localization.incomeAmountLabel(lang)) },
                    placeholder = { Text("0.00") },
                    leadingIcon = {
                        Icon(Icons.Default.Euro, contentDescription = null, tint = Color(0xFF059669))
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("income_amount_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF059669)
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Member Selection (Who brought the income)
                Text(
                    text = Localization.memberWhoContributed(lang),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(6.dp))

                if (members.isEmpty()) {
                    Text(
                        text = Localization.noMembersIncomeHint(lang),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        members.forEach { member ->
                            val isSelected = member.id == selectedMemberId
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedMemberId = if (isSelected) 0L else member.id },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(Color(member.avatarColorHex))
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = member.name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF10B981).copy(alpha = 0.15f),
                                    selectedLabelColor = Color(0xFF047857)
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Income Category Selection
                Text(
                    text = Localization.incomeCategoryLabel(lang),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DEFAULT_INCOME_CATEGORIES.forEach { cat ->
                        val isSelected = cat == selectedCategory
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = if (isSelected) "" else cat },
                            label = { Text(Localization.translateCategory(cat, lang), fontSize = 12.sp) },
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF059669).copy(alpha = 0.12f),
                                selectedLabelColor = Color(0xFF059669)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Date Picker trigger
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            val dateStr = SimpleDateFormat("dd MMMM yyyy", Localization.getDateLocale(lang)).format(Date(selectedTimestamp))
                            Text(text = "${Localization.dateLabel(lang)} $dateStr", fontSize = 13.sp)
                        }
                        Text(Localization.changeAction(lang), fontSize = 12.sp, color = Color(0xFF059669), fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Note
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(Localization.noteLabel(lang)) },
                    placeholder = { Text(Localization.notePlaceholder(lang)) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Save Button
                Button(
                    onClick = {
                        val parsedAmount = amountText.toDoubleOrNull()
                        if (title.isBlank()) {
                            errorMessage = Localization.fillDescriptionError(lang)
                            return@Button
                        }
                        if (parsedAmount == null || parsedAmount <= 0.0) {
                            errorMessage = Localization.fillAmountError(lang)
                            return@Button
                        }

                        val member = members.find { it.id == selectedMemberId }
                        val memberName = member?.name ?: ""

                        onSave(
                            incomeToEdit?.id ?: 0L,
                            title.trim(),
                            parsedAmount,
                            selectedMemberId,
                            memberName,
                            selectedCategory,
                            selectedTimestamp,
                            note.trim()
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669), contentColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_income_button")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (incomeToEdit == null) Localization.recordNewIncome(lang) else Localization.save(lang),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}
