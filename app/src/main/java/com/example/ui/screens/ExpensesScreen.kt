package com.example.ui.screens

import com.example.util.Localization
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
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.ui.theme.HeroEmeraldGradient
import com.example.ui.theme.GlassBorderBrush
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
    appLanguage: String = "EL",
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
    isInitialLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    val lang = Localization.Language.fromCode(appLanguage)
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
    val currentMonthLabel = remember(lang) {
        val cal = java.util.Calendar.getInstance()
        SimpleDateFormat("LLLL yyyy", Localization.getDateLocale(lang)).format(cal.time).replaceFirstChar { it.uppercase() }
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
            SimpleDateFormat("LLLL yyyy", Localization.getDateLocale(lang)).format(date).replaceFirstChar { it.uppercase() }
        } catch (e: Exception) {
            key
        }
    }

    val activePeriodDisplay = when (selectedPeriodKey) {
        "ALL" -> Localization.allMonthsCombined(lang)
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
    val expensesByMonth = remember(periodFilteredExpenses, lang) {
        val monthKeyFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val monthDisplayFormat = SimpleDateFormat("LLLL yyyy", Localization.getDateLocale(lang))
        val dayDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = dayDateFormat.format(Date())
        val yesterdayStr = dayDateFormat.format(Date(System.currentTimeMillis() - 24L * 3600 * 1000))
        val dayDisplayFormat = SimpleDateFormat("EEEE, d MMMM", Localization.getDateLocale(lang))

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
                        todayStr -> Localization.today(lang)
                        yesterdayStr -> Localization.yesterday(lang)
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
            val expenseTabColor by animateColorAsState(
                targetValue = if (activeViewMode == 0) Emerald500 else Color.Transparent,
                animationSpec = tween(220),
                label = "expense_tab_color"
            )
            val incomeTabColor by animateColorAsState(
                targetValue = if (activeViewMode == 1) Emerald500 else Color.Transparent,
                animationSpec = tween(220),
                label = "income_tab_color"
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .testTag("monthly_summary_card"),
                shape = RoundedCornerShape(28.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                border = BorderStroke(1.2.dp, GlassBorderBrush)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(HeroEmeraldGradient)
                        .padding(22.dp)
                ) {
                    // Ambient radial glow in top corner
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .align(Alignment.TopEnd)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(Emerald400.copy(alpha = 0.25f), Color.Transparent)
                                ),
                                shape = CircleShape
                            )
                    )

                    Column {
                        // Top row: Clickable Month Badge with Dropdown + Manual Add Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Month/year selector badge
                            Box {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)),
                                    modifier = Modifier
                                        .clickable { showPeriodDropdown = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(if (budgetProgress > 0.9f) Color(0xFFF87171) else Emerald300)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = activePeriodDisplay,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = Localization.selectMonthYear(lang),
                                            tint = Emerald200,
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
                                                text = Localization.allMonthsCombined(lang),
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
                                                text = "${Localization.currentMonth(lang)} ($currentMonthLabel)",
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
                                color = Emerald500.copy(alpha = 0.28f),
                                border = BorderStroke(1.dp, Emerald300.copy(alpha = 0.5f)),
                                modifier = Modifier.clickable {
                                    if (activeViewMode == 0) onAddManualExpense() else onAddManualIncome()
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (activeViewMode == 0) Localization.singleExpense(lang) else Localization.singleIncome(lang),
                                        color = Color.White,
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Center: Prominent Net Balance (Copilot / Revolut style)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = Localization.netBalanceLabel(lang),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                color = Emerald200.copy(alpha = 0.9f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = String.format(Locale.US, "%s%.2f €", if (netBalance >= 0) "+" else "", netBalance),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = (-0.5).sp,
                                maxLines = 1,
                                softWrap = false
                            )
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = (if (netBalance >= 0) Color(0xFF10B981) else Color(0xFFEF4444)).copy(alpha = 0.22f),
                                border = BorderStroke(1.dp, (if (netBalance >= 0) Color(0xFF34D399) else Color(0xFFFCA5A5)).copy(alpha = 0.35f)),
                                modifier = Modifier.padding(top = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (netBalance >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                        contentDescription = null,
                                        tint = if (netBalance >= 0) Color(0xFF6EE7B7) else Color(0xFFFCA5A5),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = if (netBalance >= 0) Localization.withinBudget(lang) else Localization.overBudget(lang),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (netBalance >= 0) Color(0xFF6EE7B7) else Color(0xFFFCA5A5)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Dual Breakdown Capsules: Incomes vs Expenses
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Incomes Pill
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.18f),
                                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF10B981).copy(alpha = 0.30f),
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.TrendingUp,
                                                contentDescription = null,
                                                tint = Color(0xFF6EE7B7),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = Localization.income(lang),
                                            fontSize = 11.5.sp,
                                            color = Emerald200,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = String.format(Locale.US, "%.2f €", totalIncome),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }

                            // Expenses Pill
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFEF4444).copy(alpha = 0.18f),
                                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.35f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFEF4444).copy(alpha = 0.30f),
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.TrendingDown,
                                                contentDescription = null,
                                                tint = Color(0xFFFCA5A5),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = Localization.expenses(lang),
                                            fontSize = 11.5.sp,
                                            color = Color(0xFFFCA5A5),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = String.format(Locale.US, "%.2f €", totalSpend),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Switcher Segment: Έξοδα / Έσοδα (with smooth animated pill)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(4.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(11.dp),
                                    color = expenseTabColor,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { activeViewMode = 0 }
                                ) {
                                    Text(
                                        text = Localization.expensesCount(lang, periodFilteredExpenses.size),
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = if (activeViewMode == 0) FontWeight.ExtraBold else FontWeight.Medium,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(11.dp),
                                    color = incomeTabColor,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { activeViewMode = 1 }
                                ) {
                                    Text(
                                        text = Localization.incomesCount(lang, periodFilteredIncomes.size),
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = if (activeViewMode == 1) FontWeight.ExtraBold else FontWeight.Medium,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp)
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
                                    text = "${Localization.remainingBudgetPrefix(lang)} ${String.format(Locale.US, "%.2f €", remainingBudget)}",
                                    color = Emerald100,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "${(budgetProgress * 100).toInt()}% ${Localization.ofBudget(lang)}",
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
                                                text = Localization.commonHousehold(lang),
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
                                                text = Localization.individualPersonal(lang),
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
                    placeholder = { Text(Localization.searchExpensesPlaceholder(lang), fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Emerald700)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = Localization.clear(lang), tint = Color.Gray)
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
                                        Text(Localization.commonFamilyFilter(lang), fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    } else {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(Localization.allFilter(lang), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
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
                                text = { Text(Localization.allFilter(lang), fontWeight = if (selectedMemberFilter == null) FontWeight.Bold else FontWeight.Normal) },
                                onClick = {
                                    onMemberFilterChange(null)
                                    showMemberDropdown = false
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Group, contentDescription = null, tint = Emerald700, modifier = Modifier.size(16.dp)) },
                                text = { Text(Localization.commonFamilyFilter(lang), fontWeight = if (selectedMemberFilter == -1L) FontWeight.Bold else FontWeight.Normal) },
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
                                    text = { Text("${m.name} (${Localization.translateRole(m.role, lang)})", fontWeight = if (selectedMemberFilter == m.id) FontWeight.Bold else FontWeight.Normal) },
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
                                            text = Localization.translateCategory(selectedCategory.name, lang),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    } else {
                                        Icon(Icons.Default.Category, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(Localization.allCategoriesFilter(lang), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
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
                                text = { Text(Localization.allCategoriesFilter(lang), fontWeight = if (selectedCategoryFilter == null) FontWeight.Bold else FontWeight.Normal) },
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
                                    text = { Text(Localization.translateCategory(cat.name, lang), fontWeight = if (selectedCategoryFilter == cat.id) FontWeight.Bold else FontWeight.Normal) },
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
                                text = Localization.noIncomesFound(lang),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = Localization.noIncomesForPeriod(lang),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 19.sp
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = onAddManualIncome,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669), contentColor = Color.White),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(Localization.recordNewIncome(lang), fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            } else {
                val dayDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val todayStr = dayDateFormat.format(Date())
                val yesterdayStr = dayDateFormat.format(Date(System.currentTimeMillis() - 24L * 3600 * 1000))
                val dayDisplayFormat = SimpleDateFormat("EEEE, d MMMM", Localization.getDateLocale(lang))

                val incomesByDay = periodFilteredIncomes.groupBy { inc ->
                    val dayKey = dayDateFormat.format(Date(inc.timestamp))
                    when (dayKey) {
                        todayStr -> Localization.today(lang)
                        yesterdayStr -> Localization.yesterday(lang)
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
                                                        text = income.memberName.ifBlank { if (lang == Localization.Language.ENGLISH) "Member" else "Μέλος" },
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
                                                    text = Localization.translateCategory(income.category, lang),
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
                                                contentDescription = Localization.edit(lang),
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
                                                contentDescription = Localization.delete(lang),
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
            // Friendly Empty state or initial fast loading state
            if (isInitialLoading && periodFilteredExpenses.isEmpty()) {
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = Emerald700,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = if (lang == Localization.Language.ENGLISH) "Loading expenses..." else "Φόρτωση εξόδων...",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else if (periodFilteredExpenses.isEmpty()) {
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
                            text = if (expenses.isEmpty()) {
                                if (lang == Localization.Language.ENGLISH) "Welcome to OikoNomos!" else "Καλώς ήρθατε στο OikoNomos!"
                            } else {
                                Localization.noExpensesFound(lang)
                            },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (expenses.isEmpty()) {
                                if (lang == Localization.Language.ENGLISH)
                                    "Your household is clear. Start tracking your expenses quickly and easily:"
                                else
                                    "Το εξοδολόγιό σας είναι πεντακάθαρο. Καταχωρήστε εύκολα και γρήγορα το πρώτο σας έξοδο:"
                            } else {
                                Localization.noExpensesForPeriod(lang)
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
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald700, contentColor = Color.White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(Localization.voiceInput(lang), fontWeight = FontWeight.Bold, color = Color.White)
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
                            Text(Localization.addExpense(lang), fontWeight = FontWeight.SemiBold)
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
                                    text = Localization.transactionsPlural(lang, monthSection.count),
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
                                contentDescription = if (isCollapsed) (if (lang == Localization.Language.ENGLISH) "Expand" else "Ανάπτυξη") else (if (lang == Localization.Language.ENGLISH) "Collapse" else "Σύμπτυξη"),
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
                                                    text = Localization.translateCategory(expense.categoryName.ifBlank { "General" }, lang),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = catColor,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }

                                            // Member / Split Info Pill (Distinct soft blue color)
                                            val memberLabel = if (expense.splitType == "ALL_FAMILY") {
                                                Localization.commonFamilyFilter(lang)
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
                                                contentDescription = Localization.edit(lang),
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
                                                contentDescription = Localization.delete(lang),
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
            title = { Text(if (lang == Localization.Language.ENGLISH) "Delete Expense" else "Διαγραφή Εξόδου", fontWeight = FontWeight.Bold) },
            text = { Text(if (lang == Localization.Language.ENGLISH) "Are you sure you want to delete «${target.title}» (${String.format(Locale.US, "%.2f€", target.amount)})?" else "Είστε σίγουροι ότι θέλετε να διαγράψετε το έξοδο «${target.title}» (${String.format(Locale.US, "%.2f€", target.amount)});") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteExpense(target)
                        expenseToDelete = null
                    }
                ) {
                    Text(Localization.delete(lang), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { expenseToDelete = null }) {
                    Text(Localization.cancel(lang))
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
            title = { Text(if (lang == Localization.Language.ENGLISH) "Delete Income" else "Διαγραφή Εσόδου", fontWeight = FontWeight.Bold) },
            text = { Text(if (lang == Localization.Language.ENGLISH) "Are you sure you want to delete «${target.title}» (${String.format(Locale.US, "%.2f€", target.amount)})?" else "Είστε σίγουροι ότι θέλετε να διαγράψετε το έσοδο «${target.title}» (${String.format(Locale.US, "%.2f€", target.amount)});") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteIncome(target)
                        incomeToDelete = null
                    }
                ) {
                    Text(Localization.delete(lang), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { incomeToDelete = null }) {
                    Text(Localization.cancel(lang))
                }
            }
        )
    }
}
