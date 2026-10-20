package com.example.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.entity.Expense
import com.example.ui.theme.Emerald700
import com.example.util.ExpenseReportExporter
import com.example.util.Localization
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun ExportReportDialog(
    expenses: List<Expense>,
    appLanguage: String = "EL",
    onDismiss: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val lang = Localization.Language.fromCode(appLanguage)
    val context = LocalContext.current

    // 0: Current Month, 1: Specific Single Day, 2: Custom Date Range, 3: All Time
    var selectedFilterIndex by remember { mutableIntStateOf(0) }

    val dateSdf = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    var selectedSingleDayTimestamp by remember {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        mutableLongStateOf(cal.timeInMillis)
    }

    var rangeStartTimestamp by remember {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, -7)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        mutableLongStateOf(cal.timeInMillis)
    }

    var rangeEndTimestamp by remember {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        mutableLongStateOf(cal.timeInMillis)
    }

    val targetExpenses = remember(expenses, selectedFilterIndex, selectedSingleDayTimestamp, rangeStartTimestamp, rangeEndTimestamp) {
        when (selectedFilterIndex) {
            0 -> {
                val cal = Calendar.getInstance()
                val curYear = cal.get(Calendar.YEAR)
                val curMonth = cal.get(Calendar.MONTH)
                expenses.filter {
                    val expCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                    expCal.get(Calendar.YEAR) == curYear && expCal.get(Calendar.MONTH) == curMonth
                }
            }
            1 -> {
                val dayStart = selectedSingleDayTimestamp
                val dayEnd = selectedSingleDayTimestamp + 24 * 60 * 60 * 1000L
                expenses.filter { it.timestamp in dayStart until dayEnd }
            }
            2 -> {
                expenses.filter { it.timestamp in rangeStartTimestamp..rangeEndTimestamp }
            }
            else -> expenses
        }
    }

    val periodTitle = when (selectedFilterIndex) {
        0 -> if (lang == Localization.Language.ENGLISH) "Current Month" else "Τρέχων Μήνας"
        1 -> if (lang == Localization.Language.ENGLISH) "Day: ${dateSdf.format(selectedSingleDayTimestamp)}" else "Ημέρα: ${dateSdf.format(selectedSingleDayTimestamp)}"
        2 -> if (lang == Localization.Language.ENGLISH) "${dateSdf.format(rangeStartTimestamp)} - ${dateSdf.format(rangeEndTimestamp)}" else "${dateSdf.format(rangeStartTimestamp)} - ${dateSdf.format(rangeEndTimestamp)}"
        else -> if (lang == Localization.Language.ENGLISH) "All Transactions" else "Όλες οι Συναλλαγές"
    }

    val totalAmount = targetExpenses.sumOf { it.amount }

    fun showSingleDatePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = selectedSingleDayTimestamp }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCal = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                selectedSingleDayTimestamp = newCal.timeInMillis
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun showRangeStartDatePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = rangeStartTimestamp }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCal = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                rangeStartTimestamp = newCal.timeInMillis
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun showRangeEndDatePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = rangeEndTimestamp }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCal = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 23, 59, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                rangeEndTimestamp = newCal.timeInMillis
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("export_report_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
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
                            color = Emerald700.copy(alpha = 0.15f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.TableChart,
                                    contentDescription = null,
                                    tint = Emerald700,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (lang == Localization.Language.ENGLISH) "Export Report" else "Εξαγωγή Αναφοράς",
                                fontSize = 17.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (lang == Localization.Language.ENGLISH) "In real Excel & PDF format" else "Σε μορφή Excel & PDF",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = Localization.close(lang))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Period Selection: Grid of 4 options
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Option 0: Current Month
                        FilterPill(
                            title = if (lang == Localization.Language.ENGLISH) "This Month" else "Τρέχων Μήνας",
                            isSelected = selectedFilterIndex == 0,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedFilterIndex = 0 }
                        )

                        // Option 1: Single Day
                        FilterPill(
                            title = if (lang == Localization.Language.ENGLISH) "Single Day" else "Συγκεκριμένη Ημέρα",
                            isSelected = selectedFilterIndex == 1,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedFilterIndex = 1 }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Option 2: Custom Range
                        FilterPill(
                            title = if (lang == Localization.Language.ENGLISH) "Date Range" else "Χρονικό Διάστημα",
                            isSelected = selectedFilterIndex == 2,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedFilterIndex = 2 }
                        )

                        // Option 3: All
                        FilterPill(
                            title = if (lang == Localization.Language.ENGLISH) "All Time" else "Όλα τα Έξοδα",
                            isSelected = selectedFilterIndex == 3,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedFilterIndex = 3 }
                        )
                    }
                }

                // Sub-selectors for Single Day or Date Range
                if (selectedFilterIndex == 1) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showSingleDatePicker() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Emerald700, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (lang == Localization.Language.ENGLISH) "Select day:" else "Επιλογή ημέρας:",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = dateSdf.format(selectedSingleDayTimestamp),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Emerald700
                            )
                        }
                    }
                } else if (selectedFilterIndex == 2) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // From
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showRangeStartDatePicker() }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = if (lang == Localization.Language.ENGLISH) "From" else "Από",
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = dateSdf.format(rangeStartTimestamp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Emerald700
                                )
                            }
                        }

                        // To
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showRangeEndDatePicker() }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = if (lang == Localization.Language.ENGLISH) "To" else "Έως",
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = dateSdf.format(rangeEndTimestamp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Emerald700
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Stats Preview Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = if (lang == Localization.Language.ENGLISH) "Period:" else "Περίοδος:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = periodTitle, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = if (lang == Localization.Language.ENGLISH) "Expenses count:" else "Πλήθος Εξόδων:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = if (lang == Localization.Language.ENGLISH) "${targetExpenses.size} transactions" else "${targetExpenses.size} συναλλαγές",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = if (lang == Localization.Language.ENGLISH) "Total Amount:" else "Συνολικό Ποσό:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Emerald700)
                            Text(
                                text = "${String.format(Locale.US, "%.2f", totalAmount)} €",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Emerald700
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action Button 1: Real Excel Export
                Button(
                    onClick = {
                        ExpenseReportExporter.exportExcel(context, targetExpenses, periodTitle)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald700, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("export_excel_action_button")
                ) {
                    Icon(Icons.Default.TableChart, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (lang == Localization.Language.ENGLISH) "Export to Excel" else "Εξαγωγή σε Excel",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action Button 2: Real PDF Export
                Button(
                    onClick = {
                        ExpenseReportExporter.exportPdf(context, targetExpenses, periodTitle)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626), contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("export_pdf_action_button")
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (lang == Localization.Language.ENGLISH) "Export to PDF" else "Εξαγωγή σε PDF",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterPill(
    title: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) Emerald700 else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, if (isSelected) Emerald700 else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier.clickable { onClick() }
    ) {
        Text(
            text = title,
            fontSize = 11.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 1
        )
    }
}
