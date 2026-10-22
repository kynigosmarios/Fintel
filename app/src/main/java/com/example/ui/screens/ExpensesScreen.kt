package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.Expense
import com.example.data.entity.ExpenseCategory
import com.example.data.entity.FamilyMember
import com.example.data.entity.Income
import com.example.data.entity.getBeneficiaryCategoryOrNamesDisplay
import com.example.data.entity.getPayerCategoryOrNamesDisplay
import com.example.ui.components.CategoryIconHelper
import com.example.ui.theme.Emerald700
import com.example.util.Localization
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

sealed class ActivityItem(val timestamp: Long, val key: String) {
    data class ExpenseItem(val expense: Expense) : ActivityItem(expense.timestamp, "exp_${expense.id}_${expense.cloudId}")
    data class IncomeItem(val income: Income) : ActivityItem(income.timestamp, "inc_${income.id}")
}

@Composable
fun ExpensesScreen(
    expenses: List<Expense>,
    incomes: List<Income>,
    categories: List<ExpenseCategory>,
    members: List<FamilyMember>,
    selectedMemberFilter: Long?,
    selectedCategoryFilter: Long?,
    searchQuery: String,
    appLanguage: String,
    onMemberFilterChange: (Long?) -> Unit,
    onCategoryFilterChange: (Long?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onAddManualExpense: () -> Unit,
    onAddManualIncome: () -> Unit,
    onVoiceExpense: () -> Unit,
    onEditExpense: (Expense) -> Unit,
    onDeleteExpense: (Expense) -> Unit,
    onEditIncome: (Income) -> Unit,
    onDeleteIncome: (Income) -> Unit,
    onExportExcel: () -> Unit,
    onExportPdf: () -> Unit,
    onShareReport: () -> Unit,
    onAddMember: () -> Unit,
    onAddCategory: () -> Unit,
    onSeedStandardCategories: () -> Unit,
    isInitialLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val lang = Localization.Language.fromCode(appLanguage)
    val locale = Localization.getDateLocale(lang)

    val monthYearFormat = remember(locale) { SimpleDateFormat("LLLL yyyy", locale) }
    val monthYearKeyFormat = remember { SimpleDateFormat("yyyy-MM", Locale.US) }

    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }
    var incomeToDelete by remember { mutableStateOf<Income?>(null) }

    // Segmented tab inside hero card: "EXPENSE" or "INCOME"
    var selectedTypeFilter by remember { mutableStateOf("EXPENSE") }

    // Dropdown toggles
    var showMonthDropdown by remember { mutableStateOf(false) }
    var showMemberDropdown by remember { mutableStateOf(false) }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var isMonthlyListExpanded by remember { mutableStateOf(true) }

    // Extract available months from dataset
    val availableMonths = remember(expenses, incomes, locale) {
        val allTimestamps = (expenses.map { it.timestamp } + incomes.map { it.timestamp })
            .filter { it > 0 }
            .distinct()

        val list = allTimestamps.map { ts ->
            val key = monthYearKeyFormat.format(Date(ts))
            val rawName = monthYearFormat.format(Date(ts))
            val display = rawName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
            Triple(key, display, ts)
        }
            .distinctBy { it.first }
            .sortedByDescending { it.third }

        if (list.isEmpty()) {
            val now = System.currentTimeMillis()
            val rawNow = monthYearFormat.format(Date(now))
            listOf(Triple(monthYearKeyFormat.format(Date(now)), rawNow.replaceFirstChar { it.titlecase(locale) }, now))
        } else list
    }

    // Default to the first (most recent) month e.g. "Οκτώβριος 2026", and smoothly update when dataset loads
    var selectedMonthKey by remember {
        mutableStateOf(availableMonths.firstOrNull()?.first ?: "")
    }

    LaunchedEffect(availableMonths) {
        if (selectedMonthKey.isBlank() || (selectedMonthKey != "ALL" && availableMonths.none { it.first == selectedMonthKey })) {
            selectedMonthKey = availableMonths.firstOrNull()?.first ?: ""
        }
    }

    val currentMonthDisplay = remember(selectedMonthKey, availableMonths, lang) {
        if (selectedMonthKey == "ALL") {
            if (lang == Localization.Language.ENGLISH) "All Months" else "Όλοι οι μήνες"
        } else {
            availableMonths.find { it.first == selectedMonthKey }?.second ?: "Οκτώβριος 2026"
        }
    }

    // Month-filtered datasets for the Hero Card metrics
    val monthExpenses = remember(expenses, selectedMonthKey) {
        if (selectedMonthKey.isBlank() || selectedMonthKey == "ALL") expenses
        else expenses.filter { monthYearKeyFormat.format(Date(it.timestamp)) == selectedMonthKey }
    }
    val monthIncomes = remember(incomes, selectedMonthKey) {
        if (selectedMonthKey.isBlank() || selectedMonthKey == "ALL") incomes
        else incomes.filter { monthYearKeyFormat.format(Date(it.timestamp)) == selectedMonthKey }
    }

    val totalExpensesAmount = remember(monthExpenses) { monthExpenses.sumOf { it.amount } }
    val totalIncomesAmount = remember(monthIncomes) { monthIncomes.sumOf { it.amount } }
    val netBalance = totalIncomesAmount - totalExpensesAmount

    // Dynamic budget calculation: sum of monthly budgets of categories
    val totalBudget = remember(categories) {
        val sum = categories.sumOf { it.monthlyBudget }
        if (sum > 0.0) sum else 2962.00
    }
    val remainingBudget = remember(totalBudget, totalExpensesAmount) {
        maxOf(0.0, totalBudget - totalExpensesAmount)
    }
    val budgetPercent = remember(totalBudget, totalExpensesAmount) {
        if (totalBudget > 0.0) ((totalExpensesAmount / totalBudget) * 100).toInt() else 0
    }
    val isWithinBudget = totalExpensesAmount <= totalBudget

    // Shared family ("Κοινά Σπιτιού") vs Individual ("Ατομικά")
    val sharedExpenses = remember(monthExpenses) {
        monthExpenses.filter { it.splitType == "ALL" || it.splitType == "ALL_FAMILY" }.sumOf { it.amount }
    }
    val individualExpenses = remember(monthExpenses) {
        monthExpenses.filter { it.splitType != "ALL" && it.splitType != "ALL_FAMILY" }.sumOf { it.amount }
    }

    // Filtered transaction items to display in the list below
    val displayItems = remember(monthExpenses, monthIncomes, selectedTypeFilter) {
        when (selectedTypeFilter) {
            "EXPENSE" -> monthExpenses.map { ActivityItem.ExpenseItem(it) }
            "INCOME" -> monthIncomes.map { ActivityItem.IncomeItem(it) }
            else -> (monthExpenses.map { ActivityItem.ExpenseItem(it) } + monthIncomes.map { ActivityItem.IncomeItem(it) })
                .sortedByDescending { it.timestamp }
        }
    }

    // Date grouping: Group items into "Σήμερα", "Χθες", or genuine calendar date (e.g. "Τετάρτη, 14 Οκτωβρίου 2026")
    val groupedItems = remember(displayItems, locale, lang) {
        val dayFormat = SimpleDateFormat("EEEE, d MMMM yyyy", locale)
        val dayKeyFormat = SimpleDateFormat("yyyyMMdd", Locale.US)

        val calToday = Calendar.getInstance()
        val calYesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

        displayItems.groupBy { item ->
            val itemCal = Calendar.getInstance().apply { timeInMillis = item.timestamp }

            val isToday = itemCal.get(Calendar.YEAR) == calToday.get(Calendar.YEAR) &&
                    itemCal.get(Calendar.DAY_OF_YEAR) == calToday.get(Calendar.DAY_OF_YEAR)

            val isYesterday = itemCal.get(Calendar.YEAR) == calYesterday.get(Calendar.YEAR) &&
                    itemCal.get(Calendar.DAY_OF_YEAR) == calYesterday.get(Calendar.DAY_OF_YEAR)

            val label = when {
                isToday -> if (lang == Localization.Language.ENGLISH) "Today" else "Σήμερα"
                isYesterday -> if (lang == Localization.Language.ENGLISH) "Yesterday" else "Χθες"
                else -> {
                    val raw = dayFormat.format(Date(item.timestamp))
                    raw.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
                }
            }
            val key = dayKeyFormat.format(Date(item.timestamp))
            Pair(key, label)
        }.entries.sortedByDescending { it.key.first }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. HERO FOREST GREEN CARD (Exact match to Screenshot 1)
            item(key = "hero_dashboard_card") {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("hero_balance_card"),
                    shape = RoundedCornerShape(26.dp),
                    color = Color(0xFF044837),
                    border = BorderStroke(1.dp, Color(0xFF065F46).copy(alpha = 0.6f)),
                    tonalElevation = 6.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF044837),
                                        Color(0xFF023628)
                                    )
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Top Row: Month Dropdown Chip + '+ Έξοδο' Pill Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Month Selector Chip
                                Box {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = Color(0xFF0A5541).copy(alpha = 0.85f),
                                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                                        modifier = Modifier
                                            .clickable { showMonthDropdown = true }
                                            .testTag("month_selector_pill")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(7.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF34D399))
                                            )
                                            Text(
                                                text = currentMonthDisplay,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = "Select Month",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = showMonthDropdown,
                                        onDismissRequest = { showMonthDropdown = false }
                                    ) {
                                        if (availableMonths.size > 1) {
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = if (lang == Localization.Language.ENGLISH) "All Months" else "Όλοι οι μήνες",
                                                        fontWeight = if (selectedMonthKey == "ALL") FontWeight.Bold else FontWeight.Normal,
                                                        color = if (selectedMonthKey == "ALL") Emerald700 else MaterialTheme.colorScheme.onSurface
                                                    )
                                                },
                                                onClick = {
                                                    selectedMonthKey = "ALL"
                                                    showMonthDropdown = false
                                                }
                                            )
                                        }
                                        availableMonths.forEach { month ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = month.second,
                                                        fontWeight = if (month.first == selectedMonthKey) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (month.first == selectedMonthKey) Emerald700 else MaterialTheme.colorScheme.onSurface
                                                    )
                                                },
                                                onClick = {
                                                    selectedMonthKey = month.first
                                                    showMonthDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // '+ Έξοδο' Pill Button
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFF065F46),
                                    border = BorderStroke(1.dp, Color(0xFF34D399).copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .clickable { onAddManualExpense() }
                                        .testTag("hero_add_expense_btn")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Text(
                                            text = if (lang == Localization.Language.ENGLISH) "+ Expense" else "+ Έξοδο",
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Net Balance Header
                            Text(
                                text = if (lang == Localization.Language.ENGLISH) "NET BALANCE" else "ΚΑΘΑΡΟ ΥΠΟΛΟΙΠΟ",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFA7F3D0),
                                letterSpacing = 0.8.sp
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Big Amount
                            Text(
                                text = String.format(Locale.US, "%s%.2f €", if (netBalance >= 0) "+" else "", netBalance),
                                fontSize = 35.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // 'Εντός προϋπολογισμού' Status Badge
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF065F46).copy(alpha = 0.75f),
                                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                        contentDescription = null,
                                        tint = Color(0xFF6EE7B7),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = if (isWithinBudget)
                                            (if (lang == Localization.Language.ENGLISH) "Within budget" else "Εντός προϋπολογισμού")
                                        else
                                            (if (lang == Localization.Language.ENGLISH) "Over budget" else "Εκτός προϋπολογισμού"),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF6EE7B7)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Middle Row: Incomes & Expenses Cards side by side
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Left Card: Έσοδα
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onAddManualIncome() },
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFF065F46).copy(alpha = 0.35f),
                                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.25f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF059669),
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        Column {
                                            Text(
                                                text = Localization.income(lang),
                                                fontSize = 12.sp,
                                                color = Color(0xFFA7F3D0)
                                            )
                                            Text(
                                                text = String.format(Locale.US, "%.2f €", totalIncomesAmount),
                                                fontSize = 17.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }

                                // Right Card: Έξοδα
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onAddManualExpense() },
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFF451A1A).copy(alpha = 0.35f),
                                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.25f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF7F1D1D),
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.ArrowDownward,
                                                    contentDescription = null,
                                                    tint = Color(0xFFFCA5A5),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        Column {
                                            Text(
                                                text = Localization.expenses(lang),
                                                fontSize = 12.sp,
                                                color = Color(0xFFFCA5A5)
                                            )
                                            Text(
                                                text = String.format(Locale.US, "%.2f €", totalExpensesAmount),
                                                fontSize = 17.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Segmented Switcher Tab: 💸 Έξοδα (71) | 💰 Έσοδα (1)
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF023225),
                                border = BorderStroke(1.dp, Color(0xFF065F46).copy(alpha = 0.7f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(4.dp)
                                ) {
                                    // Tab 1: Έξοδα
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { selectedTypeFilter = "EXPENSE" },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (selectedTypeFilter == "EXPENSE") Color(0xFF10B981) else Color.Transparent
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 9.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = "💸 ", fontSize = 14.sp)
                                            Text(
                                                text = "${Localization.expenses(lang)} (${monthExpenses.size})",
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (selectedTypeFilter == "EXPENSE") Color.White else Color(0xFFA7F3D0)
                                            )
                                        }
                                    }

                                    // Tab 2: Έσοδα
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { selectedTypeFilter = "INCOME" },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (selectedTypeFilter == "INCOME") Color(0xFF10B981) else Color.Transparent
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 9.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = "💰 ", fontSize = 14.sp)
                                            Text(
                                                text = "${Localization.income(lang)} (${monthIncomes.size})",
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (selectedTypeFilter == "INCOME") Color.White else Color(0xFFA7F3D0)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Budget Progress Section
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (lang == Localization.Language.ENGLISH)
                                        String.format(Locale.US, "Remaining Budget: %.2f €", remainingBudget)
                                    else
                                        String.format(Locale.US, "Υπόλοιπο Budget: %.2f €", remainingBudget),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF065F46).copy(alpha = 0.8f)
                                ) {
                                    Text(
                                        text = if (lang == Localization.Language.ENGLISH)
                                            "$budgetPercent% of budget"
                                        else
                                            "$budgetPercent% του budget",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFA7F3D0),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Budget Progress Bar
                            val animatedProgress by animateFloatAsState(
                                targetValue = (budgetPercent.coerceIn(0, 100) / 100f),
                                label = "budget_progress"
                            )
                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = Color(0xFF34D399),
                                trackColor = Color(0xFF065F46).copy(alpha = 0.7f),
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Bottom Row: Κοινά Σπιτιού & Ατομικά Cards
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Left Card: Κοινά Σπιτιού
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFF065F46).copy(alpha = 0.35f),
                                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.2f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Group,
                                                contentDescription = null,
                                                tint = Color(0xFFA7F3D0),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = if (lang == Localization.Language.ENGLISH) "Shared" else "Κοινά Σπιτιού",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFFA7F3D0)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = String.format(Locale.US, "%.2f €", sharedExpenses),
                                            fontSize = 17.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }

                                // Right Card: Ατομικά
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFF065F46).copy(alpha = 0.35f),
                                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.2f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                                contentDescription = null,
                                                tint = Color(0xFFA7F3D0),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = if (lang == Localization.Language.ENGLISH) "Personal" else "Ατομικά",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFFA7F3D0)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = String.format(Locale.US, "%.2f €", individualExpenses),
                                            fontSize = 17.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. SEARCH BAR (Matches Screenshot 1 & 2)
            item(key = "search_bar_item") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = {
                            Text(
                                text = Localization.searchExpensesPlaceholder(lang),
                                fontSize = 13.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = Emerald700,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("expenses_search_input")
                    )
                }
            }

            // 3. FILTER DROPDOWN BUTTONS (Matches Screenshot 1 & 2: '👤 Όλα ▾' and '🔺 Όλες οι Κατηγορίες ▾')
            item(key = "filter_dropdowns_item") {
                val selectedMemberName = remember(selectedMemberFilter, members, lang) {
                    if (selectedMemberFilter == null) {
                        Localization.allFilter(lang)
                    } else {
                        members.find { it.id == selectedMemberFilter }?.name ?: Localization.allFilter(lang)
                    }
                }

                val selectedCategoryName = remember(selectedCategoryFilter, categories, lang) {
                    if (selectedCategoryFilter == null) {
                        Localization.allCategoriesFilter(lang)
                    } else {
                        categories.find { it.id == selectedCategoryFilter }?.let { Localization.translateCategory(it.name, lang) } ?: Localization.allCategoriesFilter(lang)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Member Filter Dropdown
                    Box(modifier = Modifier.weight(1f)) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showMemberDropdown = true }
                                .testTag("member_filter_dropdown")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = selectedMemberName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showMemberDropdown,
                            onDismissRequest = { showMemberDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = Localization.allFilter(lang),
                                        fontWeight = if (selectedMemberFilter == null) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedMemberFilter == null) Emerald700 else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    onMemberFilterChange(null)
                                    showMemberDropdown = false
                                }
                            )
                            members.forEach { m ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = m.name,
                                            fontWeight = if (selectedMemberFilter == m.id) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selectedMemberFilter == m.id) Emerald700 else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        onMemberFilterChange(m.id)
                                        showMemberDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Category Filter Dropdown
                    Box(modifier = Modifier.weight(1f)) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showCategoryDropdown = true }
                                .testTag("category_filter_dropdown")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Category,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = selectedCategoryName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showCategoryDropdown,
                            onDismissRequest = { showCategoryDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = Localization.allCategoriesFilter(lang),
                                        fontWeight = if (selectedCategoryFilter == null) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedCategoryFilter == null) Emerald700 else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    onCategoryFilterChange(null)
                                    showCategoryDropdown = false
                                }
                            )
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = Localization.translateCategory(cat.name, lang),
                                            fontWeight = if (selectedCategoryFilter == cat.id) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selectedCategoryFilter == cat.id) Emerald700 else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
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

            // 4. MONTHLY SUMMARY EXPANDABLE CARD (Matches Screenshot 1 & 2: '📅 Οκτώβριος 2026 | 71 συναλλαγές | 2062.66 € ⌃')
            item(key = "monthly_summary_card") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable { isMonthlyListExpanded = !isMonthlyListExpanded }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Mint box with Calendar Icon
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFD1FAE5),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = Color(0xFF059669),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = currentMonthDisplay,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${displayItems.size} συναλλαγές",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = String.format(Locale.US, "%.2f €", if (selectedTypeFilter == "INCOME") totalIncomesAmount else totalExpensesAmount),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF047857)
                            )
                            Icon(
                                imageVector = if (isMonthlyListExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isMonthlyListExpanded) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // 5. TRANSACTIONS LIST (Grouped by Day: Σήμερα, Χθες, etc.)
            if (isInitialLoading) {
                item(key = "loading_state") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Emerald700)
                    }
                }
            } else if (displayItems.isEmpty()) {
                item(key = "empty_state") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                shape = CircleShape,
                                color = Emerald700.copy(alpha = 0.12f),
                                modifier = Modifier.size(60.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Receipt,
                                        contentDescription = null,
                                        tint = Emerald700,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = Localization.noExpensesFound(lang),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = Localization.noExpensesForPeriod(lang),
                                fontSize = 12.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else if (isMonthlyListExpanded) {
                groupedItems.forEach { group ->
                    val dayLabel = group.key.second
                    val itemsInDay = group.value
                    val dayTotal = itemsInDay.filterIsInstance<ActivityItem.ExpenseItem>().sumOf { it.expense.amount }
                    val dayIncomeTotal = itemsInDay.filterIsInstance<ActivityItem.IncomeItem>().sumOf { it.income.amount }

                    // Day Header Row (Σήμερα - 28.00 €, Χθες - 559.20 €)
                    item(key = "header_${group.key.first}") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dayLabel,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF059669)
                            )

                            val displayDayTotal = if (selectedTypeFilter == "INCOME") dayIncomeTotal else dayTotal
                            if (displayDayTotal > 0.0) {
                                Text(
                                    text = String.format(Locale.US, "%.2f €", displayDayTotal),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }

                    // Individual Cards for this Day
                    items(itemsInDay, key = { it.key }) { item ->
                        when (item) {
                            is ActivityItem.ExpenseItem -> {
                                val expense = item.expense
                                val matchingCategory = categories.find { it.id == expense.categoryId }
                                    ?: categories.find { it.name.equals(expense.categoryName, ignoreCase = true) }
                                val iconKey = matchingCategory?.iconKey ?: "cart"

                                // Payer display (Chip 1: green badge e.g. "👥 Γονείς" or "👤 Μάριος")
                                val payerText = expense.getPayerCategoryOrNamesDisplay(members, lang)
                                    .replace(Regex("\\s*\\([^)]*\\)"), "").trim()

                                // Target / Beneficiary display (Chip 2: blue badge e.g. "👥 Γονείς" or "👤 Γεωργία")
                                val beneficiaryText = expense.getBeneficiaryCategoryOrNamesDisplay(members, lang)
                                    .replace(Regex("\\s*\\([^)]*\\)"), "").trim()

                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                    tonalElevation = 1.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 2.dp)
                                        .clickable { onEditExpense(expense) }
                                        .testTag("expense_card_${expense.id}")
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp)
                                    ) {
                                        // Top Row: Icon + Title + Category + Amount
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Category icon in soft mint rounded box
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = Color(0xFFD1FAE5),
                                                modifier = Modifier.size(46.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = CategoryIconHelper.getIcon(iconKey),
                                                        contentDescription = null,
                                                        tint = Color(0xFF059669),
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = expense.title,
                                                    fontSize = 15.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(
                                                        text = Localization.translateCategory(expense.categoryName, lang),
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = Color(0xFF059669),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    val cardCal = remember(expense.timestamp) {
                                                        Calendar.getInstance().apply { timeInMillis = expense.timestamp }
                                                    }
                                                    val hasTime = cardCal.get(Calendar.HOUR_OF_DAY) != 0 || cardCal.get(Calendar.MINUTE) != 0
                                                    val cardDateStr = remember(expense.timestamp, locale, hasTime) {
                                                        val pattern = if (hasTime) "dd/MM/yyyy • HH:mm" else "dd/MM/yyyy"
                                                        SimpleDateFormat(pattern, locale).format(Date(expense.timestamp))
                                                    }
                                                    Text(
                                                        text = " • $cardDateStr",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Normal,
                                                        color = Color(0xFF64748B),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            Text(
                                                text = String.format(Locale.US, "%.2f €", expense.amount),
                                                fontSize = 17.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Bottom Row: Member Chips + Actions (Pencil & Trash)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f, fill = false)
                                            ) {
                                                // Chip 1 (Payer): Mint background
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = Color(0xFFE6F4EA),
                                                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f))
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = if (payerText.contains("&") || payerText.contains(",") || payerText.contains("Γονείς") || payerText.contains("Παιδιά") || payerText.contains("Συγκάτοικοι") || payerText.contains("Οικογένεια") || payerText.contains("Parents") || payerText.contains("Children") || payerText.contains("Roommates") || payerText.contains("Family")) Icons.Default.Group else Icons.Default.Person,
                                                            contentDescription = null,
                                                            tint = Color(0xFF047857),
                                                            modifier = Modifier.size(13.dp)
                                                        )
                                                        Text(
                                                            text = payerText,
                                                            fontSize = 11.5.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = Color(0xFF047857),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }

                                                // Chip 2 (Target / Split): Soft Blue background
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = Color(0xFFEFF6FF),
                                                    border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.3f))
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = if (beneficiaryText.contains("Γονείς") || beneficiaryText.contains("Παιδιά") || beneficiaryText.contains("Συγκάτοικοι") || beneficiaryText.contains("Οικογένεια") || beneficiaryText.contains("Parents") || beneficiaryText.contains("Children") || beneficiaryText.contains("Roommates") || beneficiaryText.contains("Family") || beneficiaryText.contains("&") || beneficiaryText.contains(",")) Icons.Default.Group else Icons.Default.Person,
                                                            contentDescription = null,
                                                            tint = Color(0xFF2563EB),
                                                            modifier = Modifier.size(13.dp)
                                                        )
                                                        Text(
                                                            text = beneficiaryText,
                                                            fontSize = 11.5.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = Color(0xFF1D4ED8),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            }

                                            // Action Buttons: Edit & Delete
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                IconButton(
                                                    onClick = { onEditExpense(expense) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "Edit",
                                                        tint = Color(0xFF64748B),
                                                        modifier = Modifier.size(17.dp)
                                                    )
                                                }

                                                IconButton(
                                                    onClick = { expenseToDelete = expense },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Delete",
                                                        tint = Color(0xFFEF4444).copy(alpha = 0.85f),
                                                        modifier = Modifier.size(17.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            is ActivityItem.IncomeItem -> {
                                val income = item.income

                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f)),
                                    tonalElevation = 1.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 2.dp)
                                        .clickable { onEditIncome(income) }
                                        .testTag("income_card_${income.id}")
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = Color(0xFFD1FAE5),
                                                modifier = Modifier.size(46.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.ArrowUpward,
                                                        contentDescription = null,
                                                        tint = Color(0xFF059669),
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = income.title,
                                                    fontSize = 15.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(
                                                        text = income.category.ifBlank { Localization.income(lang) },
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = Color(0xFF059669),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    val cardCal = remember(income.timestamp) {
                                                        Calendar.getInstance().apply { timeInMillis = income.timestamp }
                                                    }
                                                    val hasTime = cardCal.get(Calendar.HOUR_OF_DAY) != 0 || cardCal.get(Calendar.MINUTE) != 0
                                                    val cardDateStr = remember(income.timestamp, locale, hasTime) {
                                                        val pattern = if (hasTime) "dd/MM/yyyy • HH:mm" else "dd/MM/yyyy"
                                                        SimpleDateFormat(pattern, locale).format(Date(income.timestamp))
                                                    }
                                                    Text(
                                                        text = " • $cardDateStr",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Normal,
                                                        color = Color(0xFF64748B),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            Text(
                                                text = String.format(Locale.US, "+%.2f €", income.amount),
                                                fontSize = 17.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF059669)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            if (income.memberName.isNotBlank()) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = Color(0xFFE6F4EA),
                                                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f))
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Person,
                                                            contentDescription = null,
                                                            tint = Color(0xFF047857),
                                                            modifier = Modifier.size(13.dp)
                                                        )
                                                        Text(
                                                            text = income.memberName,
                                                            fontSize = 11.5.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = Color(0xFF047857)
                                                        )
                                                    }
                                                }
                                            } else {
                                                Spacer(modifier = Modifier.width(1.dp))
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                IconButton(
                                                    onClick = { onEditIncome(income) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "Edit",
                                                        tint = Color(0xFF64748B),
                                                        modifier = Modifier.size(17.dp)
                                                    )
                                                }

                                                IconButton(
                                                    onClick = { incomeToDelete = income },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Delete",
                                                        tint = Color(0xFFEF4444).copy(alpha = 0.85f),
                                                        modifier = Modifier.size(17.dp)
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
        }
    }

    // Delete Expense Confirmation Dialog
    if (expenseToDelete != null) {
        val exp = expenseToDelete!!
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            title = { Text(Localization.delete(lang)) },
            text = { Text("Είστε βέβαιοι ότι θέλετε να διαγράψετε το έξοδο «${exp.title}» (${String.format(Locale.US, "%.2f €", exp.amount)});") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteExpense(exp)
                        expenseToDelete = null
                    }
                ) {
                    Text(Localization.delete(lang), color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
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
        val inc = incomeToDelete!!
        AlertDialog(
            onDismissRequest = { incomeToDelete = null },
            title = { Text(Localization.delete(lang)) },
            text = { Text("Είστε βέβαιοι ότι θέλετε να διαγράψετε το έσοδο «${inc.title}» (${String.format(Locale.US, "%.2f €", inc.amount)});") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteIncome(inc)
                        incomeToDelete = null
                    }
                ) {
                    Text(Localization.delete(lang), color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
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
