package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.Expense
import com.example.data.entity.ExpenseCategory
import com.example.data.entity.FamilyMember
import com.example.data.entity.Income
import com.example.ui.components.CategoryIconHelper
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald200
import com.example.ui.theme.Emerald300
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Emerald700
import com.example.ui.theme.Emerald800
import com.example.ui.theme.Emerald900
import com.example.ui.theme.Emerald950
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.example.ui.components.ExodologioExportCard

data class MonthSectionData(
    val key: String,
    val title: String,
    val total: Double,
    val count: Int,
    val days: Map<String, List<Expense>>
)

@Composable
fun ExpensesScreen(
    expenses: List<Expense>,
    incomes: List<Income> = emptyList(),
    categories: List<ExpenseCategory>,
    members: List<FamilyMember>,
    selectedMemberFilter: Long?,
    selectedCategoryFilter: Long?,
    searchQuery: String,
    onMemberFilterChange: (Long?) -> Unit,
    onCategoryFilterChange: (Long?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onAddManualExpense: () -> Unit,
    onAddManualIncome: () -> Unit = {},
    onVoiceExpense: () -> Unit = {},
    onEditExpense: (Expense) -> Unit,
    onDeleteExpense: (Expense) -> Unit,
    onEditIncome: (Income) -> Unit = {},
    onDeleteIncome: (Income) -> Unit = {},
    onExportExcel: () -> Unit = {},
    onExportPdf: () -> Unit = {},
    onShareReport: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }
    var incomeToDelete by remember { mutableStateOf<Income?>(null) }
    var activeViewMode by remember { mutableStateOf(0) } // 0 = Expenses, 1 = Incomes

    // Period filter state ("CURRENT_MONTH", "ALL", or "yyyy-MM")
    var selectedPeriodKey by remember { mutableStateOf<String?>("CURRENT_MONTH") }
    var showPeriodDropdown by remember { mutableStateOf(false) }

    // Dropdown filters state
    var showMemberDropdown by remember { mutableStateOf(false) }
    var showCategoryDropdown by remember { mutableStateOf(false) }

    val currentMonthKey = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date()) }
    val currentMonthLabel = remember {
        val cal = java.util.Calendar.getInstance()
        SimpleDateFormat("LLLL yyyy", Locale.forLanguageTag("el-GR")).format(cal.time).replaceFirstChar { it.uppercase() }
    }

    // Extract available year-month strings from expenses
    val availableMonths = remember(expenses) {
        val set = linkedSetOf<String>()
        set.add(currentMonthKey)
        val fmt = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        expenses.forEach { exp ->
            set.add(fmt.format(Date(exp.timestamp)))
        }
        set.toList().sortedDescending()
    }

    fun getMonthLabel(key: String): String {
        return try {
            val date = SimpleDateFormat("yyyy-MM", Locale.getDefault()).parse(key) ?: Date()
            SimpleDateFormat("LLLL yyyy", Locale.forLanguageTag("el-GR")).format(date).replaceFirstChar { it.uppercase() }
        } catch (e: Exception) {
            key
        }
    }

    val activePeriodDisplay = when (selectedPeriodKey) {
        "ALL" -> "Όλα μαζί"
        "CURRENT_MONTH", null -> currentMonthLabel
        else -> getMonthLabel(selectedPeriodKey!!)
    }

    // Filter expenses by selected period (Month & Year)
    val periodFilteredExpenses = remember(expenses, selectedPeriodKey) {
        when (selectedPeriodKey) {
            "ALL" -> expenses
            "CURRENT_MONTH", null -> expenses.filter {
                SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(it.timestamp)) == currentMonthKey
            }
            else -> expenses.filter {
                SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(it.timestamp)) == selectedPeriodKey
            }
        }
    }

    // Filter incomes by selected period
    val periodFilteredIncomes = remember(incomes, selectedPeriodKey) {
        when (selectedPeriodKey) {
            "ALL" -> incomes
            "CURRENT_MONTH", null -> incomes.filter {
                SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(it.timestamp)) == currentMonthKey
            }
            else -> incomes.filter {
                SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(it.timestamp)) == selectedPeriodKey
            }
        }
    }

    // Calculations based on periodFilteredExpenses & periodFilteredIncomes
    val totalSpend = periodFilteredExpenses.sumOf { it.amount }
    val totalIncome = periodFilteredIncomes.sumOf { it.amount }
    val netBalance = totalIncome - totalSpend
    val commonSpend = periodFilteredExpenses.filter { it.splitType == "ALL_FAMILY" }.sumOf { it.amount }
    val individualSpend = totalSpend - commonSpend
    val totalBudget = categories.sumOf { it.monthlyBudget }.coerceAtLeast(1.0)
    val budgetProgress = (totalSpend / totalBudget).toFloat().coerceIn(0f, 1f)
    val remainingBudget = (totalBudget - totalSpend).coerceAtLeast(0.0)

    // Collapsed months state map
    val collapsedMonths = remember { mutableStateMapOf<String, Boolean>() }

    // Group periodFilteredExpenses into Months & Days
    val expensesByMonth = remember(periodFilteredExpenses) {
        val monthKeyFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val monthDisplayFormat = SimpleDateFormat("LLLL yyyy", Locale.forLanguageTag("el-GR"))
        val dayDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = dayDateFormat.format(Date())
        val yesterdayStr = dayDateFormat.format(Date(System.currentTimeMillis() - 24L * 3600 * 1000))
        val dayDisplayFormat = SimpleDateFormat("EEEE, d MMMM", Locale.forLanguageTag("el-GR"))

        // Sort all expenses by entry timestamp descending first
        val sortedExpenses = periodFilteredExpenses.sortedWith(
            compareByDescending<Expense> { it.timestamp }.thenByDescending { it.id }
        )

        sortedExpenses
            .groupBy { exp -> monthKeyFormat.format(Date(exp.timestamp)) }
            .map { (monthKey, monthExpenses) ->
                val sampleDate = Date(monthExpenses.firstOrNull()?.timestamp ?: System.currentTimeMillis())
                val monthTitle = monthDisplayFormat.format(sampleDate).replaceFirstChar { it.uppercase() }
                val monthTotal = monthExpenses.sumOf { it.amount }

                // Group by dayKey (YYYY-MM-DD) so days sort strictly chronologically descending
                val groupedByDayKey = monthExpenses.groupBy { exp ->
                    dayDateFormat.format(Date(exp.timestamp))
                }

                // Build days map maintaining chronological order and proper headers
                val groupedDays = linkedMapOf<String, List<Expense>>()
                groupedByDayKey.keys.sortedDescending().forEach { dayKey ->
                    val dayExpenses = groupedByDayKey[dayKey] ?: emptyList()
                    val headerTitle = when (dayKey) {
                        todayStr -> "Σήμερα"
                        yesterdayStr -> "Χθες"
                        else -> {
                            val firstExp = dayExpenses.firstOrNull()
                            if (firstExp != null) {
                                dayDisplayFormat.format(Date(firstExp.timestamp)).replaceFirstChar { it.uppercase() }
                            } else {
                                dayKey
                            }
                        }
                    }
                    groupedDays[headerTitle] = dayExpenses
                }

                MonthSectionData(
                    key = monthKey,
                    title = monthTitle,
                    total = monthTotal,
                    count = monthExpenses.size,
                    days = groupedDays
                )
            }
            .sortedByDescending { it.key }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("expenses_screen_list"),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // Modern Neo-Fintech Hero Monthly Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .testTag("monthly_summary_card"),
                shape = RoundedCornerShape(26.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Emerald950, Emerald900, Emerald800)
                            )
                        )
                        .padding(22.dp)
                ) {
                    Column {
                        // Top row: Clickable Month Badge with Dropdown + Manual Add Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Requirement 3: "Έξοδα μήνα" with month/year/all selector
                            Box {
                                Column(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { showPeriodDropdown = true }
                                        .padding(vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Έξοδα μήνα",
                                        color = Emerald300,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(if (budgetProgress > 0.9f) Color(0xFFF87171) else Emerald400)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = activePeriodDisplay,
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Επιλογή μήνα και έτους",
                                            tint = Emerald300,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = showPeriodDropdown,
                                    onDismissRequest = { showPeriodDropdown = false }
                                ) {
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            Icon(Icons.Default.Receipt, contentDescription = null, tint = Emerald700, modifier = Modifier.size(16.dp))
                                        },
                                        text = {
                                            Text(
                                                text = "Όλα μαζί (Όλοι οι μήνες)",
                                                fontWeight = if (selectedPeriodKey == "ALL") FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            selectedPeriodKey = "ALL"
                                            showPeriodDropdown = false
                                        }
                                    )
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Emerald700, modifier = Modifier.size(16.dp))
                                        },
                                        text = {
                                            Text(
                                                text = "Τρέχων Μήνας ($currentMonthLabel)",
                                                fontWeight = if (selectedPeriodKey == "CURRENT_MONTH" || selectedPeriodKey == currentMonthKey) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            selectedPeriodKey = "CURRENT_MONTH"
                                            showPeriodDropdown = false
                                        }
                                    )
                                    availableMonths.filter { it != currentMonthKey }.forEach { monthKey ->
                                        DropdownMenuItem(
                                            leadingIcon = {
                                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Emerald700, modifier = Modifier.size(16.dp))
                                            },
                                            text = {
                                                Text(
                                                    text = getMonthLabel(monthKey),
                                                    fontWeight = if (selectedPeriodKey == monthKey) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            onClick = {
                                                selectedPeriodKey = monthKey
                                                showPeriodDropdown = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Modern glass manual add button
                            Surface(
                                shape = RoundedCornerShape(13.dp),
                                color = Emerald500.copy(alpha = 0.25f),
                                border = BorderStroke(1.dp, Emerald400.copy(alpha = 0.4f)),
                                modifier = Modifier.clickable {
                                    if (activeViewMode == 0) onAddManualExpense() else onAddManualIncome()
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (activeViewMode == 0) "Έξοδο" else "Έσοδο",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Triple Financial Snapshot: Έσοδα, Έξοδα, Υπόλοιπο
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Incomes
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.20f),
                                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.40f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)) {
                                    Text(
                                        text = "Έσοδα",
                                        fontSize = 13.5.sp,
                                        color = Emerald200,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = String.format(Locale.US, "%.2f €", totalIncome),
                                        fontSize = 17.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF6EE7B7),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            // Expenses
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFEF4444).copy(alpha = 0.18f),
                                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.35f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)) {
                                    Text(
                                        text = "Έξοδα",
                                        fontSize = 13.5.sp,
                                        color = Color(0xFFFCA5A5),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = String.format(Locale.US, "%.2f €", totalSpend),
                                        fontSize = 17.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            // Balance
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = (if (netBalance >= 0) Color(0xFF10B981) else Color(0xFFEF4444)).copy(alpha = 0.20f),
                                border = BorderStroke(1.dp, (if (netBalance >= 0) Color(0xFF10B981) else Color(0xFFEF4444)).copy(alpha = 0.40f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)) {
                                    Text(
                                        text = "Υπόλοιπο",
                                        fontSize = 13.5.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = String.format(Locale.US, "%s%.2f €", if (netBalance >= 0) "+" else "", netBalance),
                                        fontSize = 17.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (netBalance >= 0) Color(0xFF6EE7B7) else Color(0xFFFCA5A5),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Switcher Segment: Έξοδα / Έσοδα
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(4.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(9.dp),
                                    color = if (activeViewMode == 0) Emerald500 else Color.Transparent,
                                    modifier = Modifier.weight(1f).clickable { activeViewMode = 0 }
                                ) {
                                    Text(
                                        text = "💸 Έξοδα (${periodFilteredExpenses.size})",
                                        color = Color.White,
                                        fontSize = 12.5.sp,
                                        fontWeight = if (activeViewMode == 0) FontWeight.ExtraBold else FontWeight.Medium,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 7.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(9.dp),
                                    color = if (activeViewMode == 1) Emerald500 else Color.Transparent,
                                    modifier = Modifier.weight(1f).clickable { activeViewMode = 1 }
                                ) {
                                    Text(
                                        text = "💰 Έσοδα (${periodFilteredIncomes.size})",
                                        color = Color.White,
                                        fontSize = 12.5.sp,
                                        fontWeight = if (activeViewMode == 1) FontWeight.ExtraBold else FontWeight.Medium,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 7.dp)
                                    )
                                }
                            }
                        }

                        if (activeViewMode == 0) {
                            Spacer(modifier = Modifier.height(12.dp))

                            // Budget Progress & Remaining Track
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Υπόλοιπο Budget: ${String.format(Locale.US, "%.2f €", remainingBudget)}",
                                    color = Emerald100,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "${(budgetProgress * 100).toInt()}% του budget",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            LinearProgressIndicator(
                                progress = { budgetProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(CircleShape),
                                color = if (budgetProgress > 0.9f) Color(0xFFF87171) else Emerald400,
                                trackColor = Color.White.copy(alpha = 0.2f)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Dual Breakdown Capsules (Common vs Individual)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White.copy(alpha = 0.08f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Group,
                                                contentDescription = null,
                                                tint = Emerald300,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = "Κοινά Σπιτιού",
                                                color = Emerald100,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = String.format(Locale.US, "%.2f €", commonSpend),
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White.copy(alpha = 0.08f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.TrendingDown,
                                                contentDescription = null,
                                                tint = Emerald300,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = "Ατομικά",
                                                color = Emerald100,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = String.format(Locale.US, "%.2f €", individualSpend),
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Search Bar & Side-by-Side Dropdown Filters (Requirement 2)
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                // Search Input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Αναζήτηση εξόδου, μέλους, ποσού...", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Emerald700)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Καθαρισμός", tint = Color.Gray)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_search_input"),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Emerald700,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Requirement 2: Side-by-Side Dropdown Menus for Members and Categories
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Member Dropdown Button (Left)
                    Box(modifier = Modifier.weight(1f)) {
                        val selectedMember = members.find { it.id == selectedMemberFilter }
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (selectedMemberFilter != null) Emerald700.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, if (selectedMemberFilter != null) Emerald700 else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showMemberDropdown = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (selectedMember != null) {
                                        Box(
                                            modifier = Modifier
                                                .size(13.dp)
                                                .clip(CircleShape)
                                                .background(Color(selectedMember.avatarColorHex))
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = selectedMember.name,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    } else if (selectedMemberFilter == -1L) {
                                        Icon(Icons.Default.Group, contentDescription = null, tint = Emerald700, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Κοινά Οικογένειας", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    } else {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Όλα", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                    }
                                }
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                            }
                        }

                        DropdownMenu(
                            expanded = showMemberDropdown,
                            onDismissRequest = { showMemberDropdown = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                text = { Text("Όλα", fontWeight = if (selectedMemberFilter == null) FontWeight.Bold else FontWeight.Normal) },
                                onClick = {
                                    onMemberFilterChange(null)
                                    showMemberDropdown = false
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Group, contentDescription = null, tint = Emerald700, modifier = Modifier.size(16.dp)) },
                                text = { Text("Κοινά Οικογένειας", fontWeight = if (selectedMemberFilter == -1L) FontWeight.Bold else FontWeight.Normal) },
                                onClick = {
                                    onMemberFilterChange(-1L)
                                    showMemberDropdown = false
                                }
                            )
                            if (members.isNotEmpty()) {
                                HorizontalDivider()
                            }
                            members.forEach { m ->
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(Color(m.avatarColorHex))
                                        )
                                    },
                                    text = { Text("${m.name} (${m.role})", fontWeight = if (selectedMemberFilter == m.id) FontWeight.Bold else FontWeight.Normal) },
                                    onClick = {
                                        onMemberFilterChange(m.id)
                                        showMemberDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Category Dropdown Button (Right)
                    Box(modifier = Modifier.weight(1f)) {
                        val selectedCategory = categories.find { it.id == selectedCategoryFilter }
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (selectedCategoryFilter != null) Emerald700.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, if (selectedCategoryFilter != null) Emerald700 else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showCategoryDropdown = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (selectedCategory != null) {
                                        Icon(
                                            CategoryIconHelper.getIcon(selectedCategory.iconKey),
                                            contentDescription = null,
                                            tint = Color(selectedCategory.colorHex),
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = selectedCategory.name,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    } else {
                                        Icon(Icons.Default.Category, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Όλες οι Κατηγορίες", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                    }
                                }
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                            }
                        }

                        DropdownMenu(
                            expanded = showCategoryDropdown,
                            onDismissRequest = { showCategoryDropdown = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                text = { Text("Όλες οι Κατηγορίες", fontWeight = if (selectedCategoryFilter == null) FontWeight.Bold else FontWeight.Normal) },
                                onClick = {
                                    onCategoryFilterChange(null)
                                    showCategoryDropdown = false
                                }
                            )
                            if (categories.isNotEmpty()) {
                                HorizontalDivider()
                            }
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            CategoryIconHelper.getIcon(cat.iconKey),
                                            contentDescription = null,
                                            tint = Color(cat.colorHex),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    text = { Text(cat.name, fontWeight = if (selectedCategoryFilter == cat.id) FontWeight.Bold else FontWeight.Normal) },
                                    onClick = {
                                        onCategoryFilterChange(cat.id)
                                        showCategoryDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }


        item { Spacer(modifier = Modifier.height(6.dp)) }

        if (activeViewMode == 1) {
            // INCOMES LIST
            if (periodFilteredIncomes.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 20.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF10B981).copy(alpha = 0.15f),
                                modifier = Modifier.size(68.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.TrendingUp,
                                        contentDescription = null,
                                        tint = Color(0xFF059669),
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Δεν βρέθηκαν έσοδα",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Δεν υπάρχουν καταχωρημένα έσοδα για το επιλεγμένο διάστημα ή μέλος.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 19.sp
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = onAddManualIncome,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Καταχώρηση Νέου Εσόδου", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                val dayDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val todayStr = dayDateFormat.format(Date())
                val yesterdayStr = dayDateFormat.format(Date(System.currentTimeMillis() - 24L * 3600 * 1000))
                val dayDisplayFormat = SimpleDateFormat("EEEE, d MMMM", Locale.forLanguageTag("el-GR"))

                val incomesByDay = periodFilteredIncomes.groupBy { inc ->
                    val dayKey = dayDateFormat.format(Date(inc.timestamp))
                    when (dayKey) {
                        todayStr -> "Σήμερα"
                        yesterdayStr -> "Χθες"
                        else -> dayDisplayFormat.format(Date(inc.timestamp)).replaceFirstChar { it.uppercase() }
                    }
                }

                incomesByDay.forEach { (dateHeader, dayIncomes) ->
                    item(key = "income_day_header_$dateHeader") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dateHeader,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = String.format(Locale.US, "+%.2f €", dayIncomes.sumOf { it.amount }),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF059669)
                            )
                        }
                    }

                    items(dayIncomes, key = { "inc_${it.id}_${it.cloudId}" }) { income ->
                        val member = members.find { it.id == income.memberId }
                        val memberColor = member?.let { Color(it.avatarColorHex) } ?: Color(0xFF3B82F6)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.TrendingUp,
                                                contentDescription = null,
                                                tint = Color(0xFF059669),
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = income.title,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            // Member Chip
                                            Surface(
                                                shape = RoundedCornerShape(7.dp),
                                                color = memberColor.copy(alpha = 0.12f)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Person,
                                                        contentDescription = null,
                                                        tint = memberColor,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = income.memberName.ifBlank { "Μέλος" },
                                                        fontSize = 11.sp,
                                                        color = memberColor,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }

                                            // Category Chip
                                            Surface(
                                                shape = RoundedCornerShape(7.dp),
                                                color = Color(0xFF10B981).copy(alpha = 0.12f)
                                            ) {
                                                Text(
                                                    text = income.category,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF047857),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }

                                        if (income.note.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = "📝 ${income.note}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }

                                // Amount & Actions
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = String.format(Locale.US, "+%.2f €", income.amount),
                                        fontSize = 16.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF059669)
                                    )

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Row {
                                        IconButton(
                                            onClick = { onEditIncome(income) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Επεξεργασία",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { incomeToDelete = income },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Διαγραφή",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Friendly Empty state
            if (periodFilteredExpenses.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Emerald100,
                            modifier = Modifier.size(68.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = Emerald700,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (expenses.isEmpty()) "Καλώς ήρθατε στο OikoNomos!" else "Δεν βρέθηκαν έξοδα",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (expenses.isEmpty()) {
                                "Το εξοδολόγιό σας είναι πεντακάθαρο. Καταχωρήστε εύκολα και γρήγορα το πρώτο σας έξοδο:"
                            } else {
                                "Δεν υπάρχουν καταχωρημένα έξοδα για το επιλεγμένο διάστημα ή τα φίλτρα."
                            },
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 19.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Action 1: Voice Button
                        Button(
                            onClick = onVoiceExpense,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald700),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Φωνητική Καταχώρηση", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Action 2: Manual Add Button
                        OutlinedButton(
                            onClick = onAddManualExpense,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Χειροκίνητη Προσθήκη", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Requirement 1: Monthly Grouped Expenses (No endless scrolling, clean month separation)
        expensesByMonth.forEachIndexed { index, monthSection ->
            // Collapse older months by default when viewing multiple months, keeping latest month open
            val isCollapsed = collapsedMonths[monthSection.key] ?: (expensesByMonth.size > 1 && index > 0)

            // Month Section Header Card
            item(key = "month_header_${monthSection.key}") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable {
                            collapsedMonths[monthSection.key] = !isCollapsed
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Emerald700.copy(alpha = 0.12f),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = Emerald700,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = monthSection.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${monthSection.count} συναλλαγές",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = String.format(Locale.US, "%.2f €", monthSection.total),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Emerald700
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = if (isCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                contentDescription = if (isCollapsed) "Ανάπτυξη" else "Σύμπτυξη",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Days inside the month (Only if not collapsed)
            if (!isCollapsed) {
                monthSection.days.forEach { (dateHeader, dayExpenses) ->
                    item(key = "day_header_${monthSection.key}_$dateHeader") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dateHeader,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = String.format(Locale.US, "%.2f €", dayExpenses.sumOf { it.amount }),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    items(dayExpenses, key = { it.id }) { expense ->
                        val category = categories.find { it.id == expense.categoryId }
                        val catColor = category?.let { Color(it.colorHex) } ?: Emerald700
                        val catIcon = CategoryIconHelper.getIcon(category?.iconKey ?: "other")

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .testTag("expense_card_${expense.id}"),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Category Colored Icon Circle
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = catColor.copy(alpha = 0.14f),
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = catIcon,
                                                contentDescription = null,
                                                tint = catColor,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = expense.title,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Category and Member Pills in a clean, elegant row
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            // Category Pill
                                            Surface(
                                                shape = RoundedCornerShape(7.dp),
                                                color = catColor.copy(alpha = 0.12f)
                                            ) {
                                                Text(
                                                    text = expense.categoryName.ifBlank { "Γενικά" },
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = catColor,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }

                                            // Member / Split Info Pill (Distinct soft blue color)
                                            val memberLabel = if (expense.splitType == "ALL_FAMILY") {
                                                "Κοινό Οικογένειας"
                                            } else {
                                                expense.memberNamesDisplay.ifBlank { null }
                                            }

                                            if (memberLabel != null) {
                                                Surface(
                                                    shape = RoundedCornerShape(7.dp),
                                                    color = Color(0xFF3B82F6).copy(alpha = 0.10f)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            imageVector = if (expense.splitType == "ALL_FAMILY") Icons.Default.Group else Icons.Default.Person,
                                                            contentDescription = null,
                                                            tint = Color(0xFF2563EB),
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = memberLabel,
                                                            fontSize = 11.sp,
                                                            color = Color(0xFF1D4ED8),
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Note if present
                                        if (expense.note.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "📝 ${expense.note}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }

                                // Amount & Actions
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = String.format(Locale.US, "%.2f €", expense.amount),
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Row {
                                        IconButton(
                                            onClick = { onEditExpense(expense) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Επεξεργασία",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { expenseToDelete = expense },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Διαγραφή",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

        // Section: Bottom Exodologio Export Options (Excel, PDF, Share)
        item {
            ExodologioExportCard(
                onExportExcel = onExportExcel,
                onExportPdf = onExportPdf,
                onShare = onShareReport
            )
        }
    }

    // Delete Expense Confirmation Dialog
    if (expenseToDelete != null) {
        val target = expenseToDelete!!
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            shape = RoundedCornerShape(20.dp),
            title = { Text("Διαγραφή Εξόδου", fontWeight = FontWeight.Bold) },
            text = { Text("Είστε σίγουροι ότι θέλετε να διαγράψετε το έξοδο «${target.title}» (${String.format(Locale.US, "%.2f€", target.amount)});") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteExpense(target)
                        expenseToDelete = null
                    }
                ) {
                    Text("Διαγραφή", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { expenseToDelete = null }) {
                    Text("Ακύρωση")
                }
            }
        )
    }

    // Delete Income Confirmation Dialog
    if (incomeToDelete != null) {
        val target = incomeToDelete!!
        AlertDialog(
            onDismissRequest = { incomeToDelete = null },
            shape = RoundedCornerShape(20.dp),
            title = { Text("Διαγραφή Εσόδου", fontWeight = FontWeight.Bold) },
            text = { Text("Είστε σίγουροι ότι θέλετε να διαγράψετε το έσοδο «${target.title}» (${String.format(Locale.US, "%.2f€", target.amount)});") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteIncome(target)
                        incomeToDelete = null
                    }
                ) {
                    Text("Διαγραφή", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { incomeToDelete = null }) {
                    Text("Ακύρωση")
                }
            }
        )
    }
}
