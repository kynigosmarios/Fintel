package com.example.ui.screens

import com.example.util.Localization
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.TableChart
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import java.text.SimpleDateFormat
import java.util.Date
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TrendingUp
import com.example.data.entity.Expense
import com.example.data.entity.ExpenseCategory
import com.example.data.entity.FamilyMember
import com.example.data.entity.Income
import com.example.data.entity.getPayerShares
import com.example.ui.components.BudgetAlertsCard
import com.example.ui.components.CategoryIconHelper
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald700
import com.example.util.GreekVoiceParser
import java.util.Locale

data class PieChartSlice(
    val label: String,
    val value: Double,
    val color: Color
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PieChartView(
    slices: List<PieChartSlice>,
    totalAmount: Double,
    appLanguage: String = "EN",
    modifier: Modifier = Modifier
) {
    val lang = Localization.Language.fromCode(appLanguage)
    val total = totalAmount.coerceAtLeast(1.0)
    val validSlices = slices.filter { it.value > 0.001 }

    if (validSlices.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(130.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = Localization.noExpensesRegistered(lang),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(190.dp),
            contentAlignment = Alignment.Center
        ) {
            val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                val strokeWidth = 20.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val centerOffset = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
                val rect = androidx.compose.ui.geometry.Rect(
                    centerOffset.x - radius,
                    centerOffset.y - radius,
                    centerOffset.x + radius,
                    centerOffset.y + radius
                )

                // Background subtle ring track
                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = rect.topLeft,
                    size = rect.size,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                var startAngle = -90f
                validSlices.forEach { slice ->
                    val sweepAngle = ((slice.value / total) * 360f).toFloat()
                    val gap = if (validSlices.size > 1) 4f else 0f
                    val effectiveSweep = (sweepAngle - gap).coerceAtLeast(1f)
                    drawArc(
                        color = slice.color,
                        startAngle = startAngle + (gap / 2),
                        sweepAngle = effectiveSweep,
                        useCenter = false,
                        topLeft = rect.topLeft,
                        size = rect.size,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    startAngle += sweepAngle
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = Localization.total(lang).uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = String.format(Locale.US, "%.2f €", totalAmount),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            validSlices.forEach { slice ->
                val pct = ((slice.value / total) * 100).toInt()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(slice.color)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${slice.label} ($pct%): ${String.format(Locale.US, "%.2f€", slice.value)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnalyticsScreen(
    expenses: List<Expense>,
    incomes: List<Income> = emptyList(),
    categories: List<ExpenseCategory>,
    members: List<FamilyMember>,
    appLanguage: String = "EN",
    onExportReport: () -> Unit = {},
    onManageCategories: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val lang = Localization.Language.fromCode(appLanguage)
    var splitMode by remember { mutableIntStateOf(0) } // 0 = Ατομικά, 1 = Γονείς + Κοινά, 2 = Ισόποσα σε όλους
    var chartTypeFamily by remember { mutableIntStateOf(0) } // 0 = Μπάρες, 1 = Πίτα (Requirement 6)
    var chartTypeMembers by remember { mutableIntStateOf(0) } // 0 = Μπάρες, 1 = Πίτα (Requirement 6)
    var chartTypeIncome by remember { mutableIntStateOf(0) } // 0 = Μπάρες, 1 = Πίτα

    // 3 Period Modes: Month, Year, Total (Requirement 1)
    var filterScope by remember { mutableStateOf("MONTH") } // "MONTH", "YEAR", "ALL"

    // Month Selection State
    val currentMonthKey = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date()) }
    val currentMonthLabel = remember(lang) {
        val cal = java.util.Calendar.getInstance()
        SimpleDateFormat("LLLL yyyy", Localization.getDateLocale(lang)).format(cal.time).replaceFirstChar { it.uppercase() }
    }

    val availableMonths = remember(expenses, incomes) {
        val set = linkedSetOf<String>()
        set.add(currentMonthKey)
        val fmt = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        expenses.forEach { exp -> set.add(fmt.format(Date(exp.timestamp))) }
        incomes.forEach { inc -> set.add(fmt.format(Date(inc.timestamp))) }
        set.toList().sortedDescending()
    }

    // Year Selection State
    val currentYearKey = remember { SimpleDateFormat("yyyy", Locale.getDefault()).format(Date()) }
    val availableYears = remember(expenses, incomes) {
        val set = linkedSetOf<String>()
        set.add(currentYearKey)
        val fmt = SimpleDateFormat("yyyy", Locale.getDefault())
        expenses.forEach { exp -> set.add(fmt.format(Date(exp.timestamp))) }
        incomes.forEach { inc -> set.add(fmt.format(Date(inc.timestamp))) }
        set.toList().sortedDescending()
    }

    var selectedPeriodKey by remember { mutableStateOf("CURRENT_MONTH") }
    var selectedYearKey by remember { mutableStateOf(currentYearKey) }
    var showPeriodDropdown by remember { mutableStateOf(false) }
    var showYearDropdown by remember { mutableStateOf(false) }

    fun getMonthLabel(key: String): String {
        return try {
            val date = SimpleDateFormat("yyyy-MM", Locale.getDefault()).parse(key) ?: Date()
            SimpleDateFormat("LLLL yyyy", Localization.getDateLocale(lang)).format(date).replaceFirstChar { it.uppercase() }
        } catch (e: Exception) {
            key
        }
    }

    val activePeriodDisplay = when (filterScope) {
        "ALL" -> Localization.allMonthsCombined(lang)
        "YEAR" -> if (lang == Localization.Language.ENGLISH) "Year $selectedYearKey" else "Έτος $selectedYearKey"
        else -> if (selectedPeriodKey == "CURRENT_MONTH" || selectedPeriodKey == currentMonthKey) currentMonthLabel else getMonthLabel(selectedPeriodKey)
    }

    // Filter expenses and incomes by selected scope (Month, Year, Total)
    val periodExpenses = remember(expenses, filterScope, selectedPeriodKey, selectedYearKey) {
        when (filterScope) {
            "ALL" -> expenses
            "YEAR" -> expenses.filter {
                SimpleDateFormat("yyyy", Locale.getDefault()).format(Date(it.timestamp)) == selectedYearKey
            }
            else -> {
                val effKey = if (selectedPeriodKey == "CURRENT_MONTH") currentMonthKey else selectedPeriodKey
                expenses.filter {
                    SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(it.timestamp)) == effKey
                }
            }
        }
    }

    val periodIncomes = remember(incomes, filterScope, selectedPeriodKey, selectedYearKey) {
        when (filterScope) {
            "ALL" -> incomes
            "YEAR" -> incomes.filter {
                SimpleDateFormat("yyyy", Locale.getDefault()).format(Date(it.timestamp)) == selectedYearKey
            }
            else -> {
                val effKey = if (selectedPeriodKey == "CURRENT_MONTH") currentMonthKey else selectedPeriodKey
                incomes.filter {
                    SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(it.timestamp)) == effKey
                }
            }
        }
    }

    val totalSpend = periodExpenses.sumOf { it.amount }
    val totalIncome = periodIncomes.sumOf { it.amount }
    val netBalance = totalIncome - totalSpend
    val savingsRate = if (totalIncome > 0) ((netBalance / totalIncome) * 100).coerceIn(-100.0, 100.0) else 0.0

    // 3 Smart Universal Statistics: Daily Average, Savings Ratio, Peak Day
    val daysInActivePeriod = remember(filterScope, selectedPeriodKey, selectedYearKey, periodExpenses) {
        val cal = java.util.Calendar.getInstance()
        when (filterScope) {
            "ALL" -> {
                if (periodExpenses.isEmpty()) 1 else {
                    val minTs = periodExpenses.minOf { it.timestamp }
                    val maxTs = periodExpenses.maxOf { it.timestamp }
                    maxOf(1, ((maxTs - minTs) / (1000L * 60 * 60 * 24)).toInt() + 1)
                }
            }
            "YEAR" -> {
                val curYear = SimpleDateFormat("yyyy", Locale.getDefault()).format(cal.time)
                if (selectedYearKey == curYear) {
                    maxOf(1, cal.get(java.util.Calendar.DAY_OF_YEAR))
                } else {
                    val y = selectedYearKey.toIntOrNull() ?: 2026
                    try {
                        if (java.time.Year.of(y).isLeap) 366 else 365
                    } catch (_: Exception) {
                        365
                    }
                }
            }
            else -> {
                if (selectedPeriodKey == "CURRENT_MONTH" || selectedPeriodKey == currentMonthKey) {
                    maxOf(1, cal.get(java.util.Calendar.DAY_OF_MONTH))
                } else {
                    try {
                        val date = SimpleDateFormat("yyyy-MM", Locale.US).parse(selectedPeriodKey) ?: Date()
                        cal.time = date
                        cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
                    } catch (e: Exception) {
                        30
                    }
                }
            }
        }
    }
    val dailyAverageExpense = if (daysInActivePeriod > 0) totalSpend / daysInActivePeriod else 0.0

    val peakSpendingDayInfo = remember(periodExpenses, lang) {
        if (periodExpenses.isEmpty()) null else {
            val fmtKey = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val dayMap = periodExpenses.groupBy { fmtKey.format(Date(it.timestamp)) }
            val peak = dayMap.maxByOrNull { entry -> entry.value.sumOf { it.amount } }
            peak?.let { entry ->
                val daySum = entry.value.sumOf { it.amount }
                val parsedDate = try { fmtKey.parse(entry.key) ?: Date() } catch (e: Exception) { Date() }
                val disp = SimpleDateFormat("dd MMM", Localization.getDateLocale(lang)).format(parsedDate)
                Pair(disp, daySum)
            }
        }
    }

    // Income breakdown by Member
    val memberIncomeMap = members.associateWith { member ->
        periodIncomes.filter { it.memberId == member.id }.sumOf { it.amount }
    }
    val unassignedMemberIncomes = periodIncomes.filter { inc ->
        members.none { it.id == inc.memberId }
    }.sumOf { it.amount }

    // Income breakdown by Category
    val categoryIncomeMap = periodIncomes.groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount } }
        .toList()
        .sortedByDescending { it.second }

    // Helper to identify which family members an expense belongs to
    fun getMembersForExpense(exp: Expense): List<FamilyMember> {
        val isExplicitAllFamily = exp.splitType == "ALL_FAMILY" &&
            (exp.memberNamesDisplay.isBlank() || exp.memberNamesDisplay.equals("Κοινό Οικογένειας", ignoreCase = true))

        if (isExplicitAllFamily) {
            // Check if title or note specifically mentions a member before declaring 100% common
            val combinedText = "${exp.title} ${exp.note}"
            val normCombined = GreekVoiceParser.normalizeGreek(combinedText)
            val byMention = members.filter { m ->
                val nameNorm = GreekVoiceParser.normalizeGreek(m.name)
                val keywords = m.voiceKeywords.split(",").map { GreekVoiceParser.normalizeGreek(it.trim()) }.filter { it.isNotBlank() }
                (keywords + nameNorm).any { kw -> GreekVoiceParser.containsWord(normCombined, kw) }
            }
            if (byMention.isNotEmpty()) return byMention

            // Check if category specifically targets kids
            val catNorm = GreekVoiceParser.normalizeGreek(exp.categoryName)
            if (catNorm.contains("παιδ") || catNorm.contains("σχολ") || catNorm.contains("φροντιστηρ")) {
                val kids = members.filter { it.role.equals("Παιδί", ignoreCase = true) }
                if (kids.isNotEmpty()) return kids
            }
            return emptyList()
        }

        // 1. By ID in memberIdsCsv
        val ids = exp.memberIdsCsv.split(",").mapNotNull { it.trim().toLongOrNull() }
        val byId = members.filter { ids.contains(it.id) }
        if (byId.isNotEmpty()) return byId

        // 2. By name or collective role in memberNamesDisplay
        if (exp.memberNamesDisplay.isNotBlank() && !exp.memberNamesDisplay.equals("Κοινό Οικογένειας", ignoreCase = true) && !exp.memberNamesDisplay.equals("Family Shared", ignoreCase = true)) {
            val raw = exp.memberNamesDisplay.trim()
            if (raw.equals("Γονείς", ignoreCase = true) || raw.equals("Parents", ignoreCase = true)) {
                val parents = members.filter { it.role.equals("Γονέας", ignoreCase = true) }
                if (parents.isNotEmpty()) return parents
            } else if (raw.equals("Παιδιά", ignoreCase = true) || raw.equals("Children", ignoreCase = true)) {
                val kids = members.filter { it.role.equals("Παιδί", ignoreCase = true) }
                if (kids.isNotEmpty()) return kids
            } else if (raw.equals("Συγκάτοικοι", ignoreCase = true) || raw.equals("Roommates", ignoreCase = true)) {
                val roommates = members.filter { it.role.equals("Συγκάτοικος", ignoreCase = true) || it.role.equals("Συγκάτοικοι", ignoreCase = true) }
                if (roommates.isNotEmpty()) return roommates
            }
            val byName = members.filter { m ->
                GreekVoiceParser.containsWord(GreekVoiceParser.normalizeGreek(exp.memberNamesDisplay), GreekVoiceParser.normalizeGreek(m.name)) ||
                exp.memberNamesDisplay.contains(m.name, ignoreCase = true)
            }
            if (byName.isNotEmpty()) return byName
        }

        // 3. By title or note mention
        val combinedText = "${exp.title} ${exp.note}"
        val normCombined = GreekVoiceParser.normalizeGreek(combinedText)
        val byMention = members.filter { m ->
            val nameNorm = GreekVoiceParser.normalizeGreek(m.name)
            val keywords = m.voiceKeywords.split(",").map { GreekVoiceParser.normalizeGreek(it.trim()) }.filter { it.isNotBlank() }
            (keywords + nameNorm).any { kw -> GreekVoiceParser.containsWord(normCombined, kw) }
        }
        if (byMention.isNotEmpty()) return byMention

        // 4. By category if specifically for kids
        val catNorm = GreekVoiceParser.normalizeGreek(exp.categoryName)
        if (catNorm.contains("παιδ") || catNorm.contains("σχολ") || catNorm.contains("φροντιστηρ")) {
            val kids = members.filter { it.role.equals("Παιδί", ignoreCase = true) }
            if (kids.isNotEmpty()) return kids
        }

        return emptyList()
    }

    // 1. Calculate Individual vs Common Household Spend
    val individualSpendMap = members.associateWith { 0.0 }.toMutableMap()
    var commonHouseholdSpend = 0.0

    periodExpenses.forEach { exp ->
        val assigned = getMembersForExpense(exp)
        if (assigned.isEmpty()) {
            commonHouseholdSpend += exp.amount
        } else {
            val perPerson = exp.amount / assigned.size
            assigned.forEach { m ->
                individualSpendMap[m] = (individualSpendMap[m] ?: 0.0) + perPerson
            }
        }
    }

    // 2. Parents vs Kids calculations (Parents bear direct expenses + common household expenses)
    var directParentsSpend = 0.0
    var kidsSpend = 0.0

    members.forEach { m ->
        val spend = individualSpendMap[m] ?: 0.0
        if (m.role.equals("Παιδί", ignoreCase = true)) {
            kidsSpend += spend
        } else {
            directParentsSpend += spend
        }
    }

    val parentsTotalSpend = directParentsSpend + commonHouseholdSpend
    val parentsList = members.filter { it.role.equals("Γονέας", ignoreCase = true) }
    val kidsMembers = members.filter { it.role.equals("Παιδί", ignoreCase = true) }
    val hasFamilyWithKids = parentsList.isNotEmpty() && kidsMembers.isNotEmpty()

    // 2b. Calculate Who Actually Paid (Payer spending, supporting multiple payers and custom splits)
    val payerSpendMap = members.associateWith { 0.0 }.toMutableMap()
    periodExpenses.forEach { exp ->
        val shares = exp.getPayerShares(members)
        shares.forEach { share ->
            val payer = members.find { it.id == share.memberId }
                ?: members.find { it.name.equals(share.memberName, ignoreCase = true) }
                ?: members.firstOrNull { it.role.equals("Γονέας", ignoreCase = true) }
                ?: members.firstOrNull()

            if (payer != null) {
                payerSpendMap[payer] = (payerSpendMap[payer] ?: 0.0) + share.amount
            }
        }
    }

    // Available split modes:
    // If hasFamilyWithKids: 0 = Individual, 1 = Parents, 2 = Kids, 3 = Who Paid
    // If NOT hasFamilyWithKids: 0 = Individual, 3 = Who Paid
    val displayedMembers = when {
        splitMode == 3 -> members // Ποιος Πλήρωσε
        splitMode == 1 && hasFamilyWithKids -> parentsList // Γονείς
        splitMode == 2 && hasFamilyWithKids -> kidsMembers // Παιδιά
        else -> members // Ατομικά
    }

    val memberSpendMap = displayedMembers.associateWith { member ->
        if (splitMode == 3) {
            payerSpendMap[member] ?: 0.0
        } else {
            val ind = individualSpendMap[member] ?: 0.0
            val comShare = when {
                splitMode == 2 -> 0.0 // Kids mode: direct child expenses
                hasFamilyWithKids -> {
                    // In a family with kids, parents share the common household expenses
                    if (member.role.equals("Γονέας", ignoreCase = true) && parentsList.isNotEmpty()) {
                        commonHouseholdSpend / parentsList.size
                    } else if (parentsList.isEmpty() && members.isNotEmpty()) {
                        commonHouseholdSpend / members.size
                    } else {
                        0.0
                    }
                }
                else -> {
                    // In other households, all members participate in common household expenses
                    if (members.isNotEmpty()) commonHouseholdSpend / members.size else 0.0
                }
            }
            ind + comShare
        }
    }

    // 4. Category budget analysis (matched by ID or normalized Greek name)
    val categorySpendMap = categories.associateWith { cat ->
        val catNorm = GreekVoiceParser.normalizeGreek(cat.name.trim().lowercase())
        periodExpenses.filter { exp ->
            exp.categoryId == cat.id ||
            (exp.categoryName.isNotBlank() && GreekVoiceParser.normalizeGreek(exp.categoryName.trim().lowercase()) == catNorm)
        }.sumOf { it.amount }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("analytics_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Period Selector Bar (Month, Year, Total - Requirement 1)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("analytics_month_selector_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 10.dp)
                ) {
                    // 3 Filter Buttons: "Μήνας", "Χρονιά", "Σύνολο"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val scopes = listOf(
                            Triple("MONTH", if (lang == Localization.Language.ENGLISH) "Month" else "Μήνας", Icons.Default.CalendarMonth),
                            Triple("YEAR", if (lang == Localization.Language.ENGLISH) "Year" else "Χρονιά", Icons.Default.DateRange),
                            Triple("ALL", if (lang == Localization.Language.ENGLISH) "Total" else "Σύνολο", Icons.Default.AllInclusive)
                        )

                        scopes.forEach { (scopeKey, label, icon) ->
                            val isSelected = filterScope == scopeKey
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) Emerald700 else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { filterScope = scopeKey }
                                    .testTag("analytics_filter_${scopeKey.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = label,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Secondary Sub-row: Selection controls depending on scope
                    when (filterScope) {
                        "MONTH" -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        val effKey = if (selectedPeriodKey == "CURRENT_MONTH") currentMonthKey else selectedPeriodKey
                                        try {
                                            val cal = java.util.Calendar.getInstance().apply {
                                                time = SimpleDateFormat("yyyy-MM", Locale.getDefault()).parse(effKey) ?: Date()
                                                add(java.util.Calendar.MONTH, -1)
                                            }
                                            val prevKey = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(cal.time)
                                            selectedPeriodKey = if (prevKey == currentMonthKey) "CURRENT_MONTH" else prevKey
                                        } catch (e: Exception) {
                                            selectedPeriodKey = "CURRENT_MONTH"
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChevronLeft,
                                        contentDescription = "Previous Month",
                                        tint = Emerald700
                                    )
                                }

                                Box(contentAlignment = Alignment.Center) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Emerald700.copy(alpha = 0.09f),
                                        border = BorderStroke(1.dp, Emerald700.copy(alpha = 0.25f)),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { showPeriodDropdown = true }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CalendarMonth,
                                                contentDescription = null,
                                                tint = Emerald700,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (selectedPeriodKey == "CURRENT_MONTH" || selectedPeriodKey == currentMonthKey) currentMonthLabel else getMonthLabel(selectedPeriodKey),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = Localization.selectMonthYear(lang),
                                                tint = Emerald700,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = showPeriodDropdown,
                                        onDismissRequest = { showPeriodDropdown = false }
                                    ) {
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
                                            val label = getMonthLabel(monthKey)
                                            DropdownMenuItem(
                                                leadingIcon = {
                                                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Emerald700, modifier = Modifier.size(16.dp))
                                                },
                                                text = {
                                                    Text(
                                                        text = label,
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

                                IconButton(
                                    onClick = {
                                        val effKey = if (selectedPeriodKey == "CURRENT_MONTH") currentMonthKey else selectedPeriodKey
                                        try {
                                            val cal = java.util.Calendar.getInstance().apply {
                                                time = SimpleDateFormat("yyyy-MM", Locale.getDefault()).parse(effKey) ?: Date()
                                                add(java.util.Calendar.MONTH, 1)
                                            }
                                            val nextKey = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(cal.time)
                                            selectedPeriodKey = if (nextKey == currentMonthKey) "CURRENT_MONTH" else nextKey
                                        } catch (e: Exception) {
                                            selectedPeriodKey = "CURRENT_MONTH"
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Next Month",
                                        tint = Emerald700
                                    )
                                }
                            }
                        }
                        "YEAR" -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        val curY = selectedYearKey.toIntOrNull() ?: currentYearKey.toInt()
                                        selectedYearKey = (curY - 1).toString()
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChevronLeft,
                                        contentDescription = "Previous Year",
                                        tint = Emerald700
                                    )
                                }

                                Box(contentAlignment = Alignment.Center) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Emerald700.copy(alpha = 0.09f),
                                        border = BorderStroke(1.dp, Emerald700.copy(alpha = 0.25f)),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { showYearDropdown = true }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DateRange,
                                                contentDescription = null,
                                                tint = Emerald700,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (lang == Localization.Language.ENGLISH) "Year $selectedYearKey" else "Έτος $selectedYearKey",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = null,
                                                tint = Emerald700,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = showYearDropdown,
                                        onDismissRequest = { showYearDropdown = false }
                                    ) {
                                        availableYears.forEach { year ->
                                            DropdownMenuItem(
                                                leadingIcon = {
                                                    Icon(Icons.Default.DateRange, contentDescription = null, tint = Emerald700, modifier = Modifier.size(16.dp))
                                                },
                                                text = {
                                                    Text(
                                                        text = if (lang == Localization.Language.ENGLISH) "Year $year" else "Έτος $year",
                                                        fontWeight = if (selectedYearKey == year) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                },
                                                onClick = {
                                                    selectedYearKey = year
                                                    showYearDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        val curY = selectedYearKey.toIntOrNull() ?: currentYearKey.toInt()
                                        selectedYearKey = (curY + 1).toString()
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Next Year",
                                        tint = Emerald700
                                    )
                                }
                            }
                        }
                        "ALL" -> {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Emerald700.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, Emerald700.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AllInclusive,
                                        contentDescription = null,
                                        tint = Emerald700,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (lang == Localization.Language.ENGLISH)
                                            "All-Time Records (${periodExpenses.size} expenses, ${periodIncomes.size} incomes)"
                                        else
                                            "Σύνολο Όλων των Εγγραφών (${periodExpenses.size} έξοδα, ${periodIncomes.size} έσοδα)",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Emerald700
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Feature 3: Monthly Budget Health & Exceeded Alerts Card
        item {
            BudgetAlertsCard(
                expenses = periodExpenses,
                categories = categories,
                appLanguage = appLanguage,
                onManageBudgets = onManageCategories
            )
        }

        // Top KPI Card with Export Excel Button
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Emerald100,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.PieChart,
                                        contentDescription = null,
                                        tint = Emerald700,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = Localization.statisticalAnalysis(lang),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = activePeriodDisplay,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Triple KPI: Incomes, Expenses, Net Balance
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.28f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(Localization.incomes(lang), fontSize = 11.sp, color = Color(0xFF047857), fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = String.format(Locale.US, "+%.2f €", totalIncome),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF059669)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = Localization.entriesCount(periodIncomes.size, lang),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFEF4444).copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.22f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(Localization.expenses(lang), fontSize = 11.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = String.format(Locale.US, "%.2f €", totalSpend),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFDC2626)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = Localization.transactionsCount(periodExpenses.size, lang),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = (if (netBalance >= 0) Color(0xFF10B981) else Color(0xFFEF4444)).copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, (if (netBalance >= 0) Color(0xFF10B981) else Color(0xFFEF4444)).copy(alpha = 0.28f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(Localization.netBalanceTitle(lang), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = String.format(Locale.US, "%s%.2f €", if (netBalance >= 0) "+" else "", netBalance),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (netBalance >= 0) Color(0xFF059669) else Color(0xFFDC2626)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (totalIncome > 0) Localization.savingsRateLabel(savingsRate.toInt(), lang) else Localization.balanceLabel(lang),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (netBalance >= 0) Color(0xFF059669) else Color(0xFFDC2626)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // 3 Universal Smart Statistics: Daily Average, Savings Ratio, Peak Spending Day
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Metric 1: Daily Average
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 9.dp, vertical = 8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = Emerald700,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = Localization.dailyAverageTitle(lang),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = String.format(Locale.US, "%.2f €", dailyAverageExpense),
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = Localization.dailyAverageUnit(lang),
                                    fontSize = 9.5.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        // Metric 2: Savings Ratio
                        val savingsColor = if (netBalance >= 0) Color(0xFF059669) else Color(0xFFDC2626)
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = savingsColor.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, savingsColor.copy(alpha = 0.22f))
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 9.dp, vertical = 8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.TrendingUp,
                                        contentDescription = null,
                                        tint = savingsColor,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = Localization.savingsRatioTitle(lang),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = if (totalIncome > 0) String.format(Locale.US, "%s%.1f%%", if (savingsRate >= 0) "+" else "", savingsRate) else "0.0%",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = savingsColor
                                )
                                Text(
                                    text = if (netBalance >= 0) Localization.withinLimit(lang) else Localization.deficitLabel(lang),
                                    fontSize = 9.5.sp,
                                    color = savingsColor
                                )
                            }
                        }

                        // Metric 3: Peak Day
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 9.dp, vertical = 8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ReceiptLong,
                                        contentDescription = null,
                                        tint = Color(0xFFEA580C),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = Localization.peakDayTitle(lang),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = peakSpendingDayInfo?.let { String.format(Locale.US, "%.0f €", it.second) } ?: "-",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = peakSpendingDayInfo?.first ?: Localization.noPeakDay(lang),
                                    fontSize = 9.5.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: Έσοδα Μελών Οικογένειας & Κατηγορίες Εσόδων
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.15f),
                                modifier = Modifier.size(34.dp)
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
                                text = Localization.familyIncome(lang),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Bar vs Pie icon-only toggle
                        ChartTypeToggle(
                            selectedType = chartTypeIncome,
                            onSelectType = { chartTypeIncome = it },
                            activeColor = Color(0xFF059669),
                            appLanguage = appLanguage
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (periodIncomes.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = Localization.noIncomesYet(lang),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = Localization.registerIncomeHint(lang),
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    } else {
                        val safeTotalIncome = totalIncome.coerceAtLeast(1.0)

                        if (chartTypeIncome == 1) {
                            // Pie Chart for Member Incomes
                            val incomeSlices = members.mapNotNull { m ->
                                val inc = memberIncomeMap[m] ?: 0.0
                                if (inc > 0.001) {
                                    PieChartSlice(
                                        label = m.name,
                                        value = inc,
                                        color = Color(m.avatarColorHex)
                                    )
                                } else null
                            } + if (unassignedMemberIncomes > 0.001) {
                                listOf(PieChartSlice(Localization.commonOther(lang), unassignedMemberIncomes, Color(0xFF10B981)))
                            } else emptyList()

                            PieChartView(
                                slices = incomeSlices,
                                totalAmount = totalIncome,
                                appLanguage = appLanguage
                            )
                        } else {
                            // Bar List for Member Incomes
                            val maxIncome = (memberIncomeMap.values + unassignedMemberIncomes).maxOrNull()?.coerceAtLeast(1.0) ?: 1.0

                            members.forEach { member ->
                                val inc = memberIncomeMap[member] ?: 0.0
                                val percent = ((inc / safeTotalIncome) * 100).toInt()
                                val progress = (inc / maxIncome).toFloat().coerceIn(0f, 1f)
                                val avatarColor = Color(member.avatarColorHex)

                                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(avatarColor),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = member.name.take(1),
                                                    color = Color.White,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = member.name,
                                                    fontSize = 13.5.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "${Localization.translateRole(member.role, lang)} • ${Localization.percentOfTotalIncome(percent, lang)}",
                                                    fontSize = 10.5.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Text(
                                            text = String.format(Locale.US, "+%.2f €", inc),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (inc > 0) Color(0xFF059669) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(CircleShape),
                                        color = if (inc > 0) avatarColor else Color.Transparent,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            }

                            if (unassignedMemberIncomes > 0.01) {
                                val unassignedPct = ((unassignedMemberIncomes / safeTotalIncome) * 100).toInt()
                                val unassignedProgress = (unassignedMemberIncomes / maxIncome).toFloat().coerceIn(0f, 1f)
                                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF10B981)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.Person,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = Localization.commonOtherIncome(lang),
                                                    fontSize = 13.5.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = Localization.unassignedMemberIncome(unassignedPct, lang),
                                                    fontSize = 10.5.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Text(
                                            text = String.format(Locale.US, "+%.2f €", unassignedMemberIncomes),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF059669)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    LinearProgressIndicator(
                                        progress = { unassignedProgress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(CircleShape),
                                        color = Color(0xFF10B981),
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            }
                        }

                        // Categories of Income breakdown
                        if (categoryIncomeMap.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = Localization.incomeSourcesByCategory(lang),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                categoryIncomeMap.forEach { (catName, amount) ->
                                    val catPct = ((amount / safeTotalIncome) * 100).toInt()
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF059669).copy(alpha = 0.10f),
                                        border = BorderStroke(1.dp, Color(0xFF059669).copy(alpha = 0.25f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = Localization.translateCategory(catName, lang),
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF047857)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = String.format(Locale.US, "+%.2f€ (%d%%)", amount, catPct),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF059669)
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

        // Section: Deficit Alert if any member paid more in expenses than declared income
        val membersWithDeficit = members.mapNotNull { m ->
            val paid = payerSpendMap[m] ?: 0.0
            val income = memberIncomeMap[m] ?: 0.0
            val deficit = paid - income
            if (deficit > 0.01 && (paid > 0.0 || income > 0.0)) {
                Triple(m, paid, income)
            } else null
        }

        if (membersWithDeficit.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth().testTag("members_deficit_alert_card")
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFEF4444).copy(alpha = 0.14f),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = null,
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = Localization.memberIncomeDeficitTitle(lang),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            membersWithDeficit.forEach { (m, paid, income) ->
                                val deficit = paid - income
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFFFEF2F2),
                                    border = BorderStroke(1.dp, Color(0xFFFCA5A5).copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f, fill = false)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(m.avatarColorHex)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = m.name.take(1),
                                                    color = Color.White,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = m.name,
                                                    fontSize = 13.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF991B1B)
                                                )
                                                Text(
                                                    text = "Έσοδα: ${String.format(Locale.US, "%.1f€", income)} • Έξοδα: ${String.format(Locale.US, "%.1f€", paid)}",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFFB91C1C)
                                                )
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFDC2626)
                                        ) {
                                            Text(
                                                text = "-${String.format(Locale.US, "%.2f €", deficit)}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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

        // Section: Γονείς vs Παιδιά vs Κοινό (ΜΟΝΟ αν υπάρχουν γονείς και παιδιά)
        if (hasFamilyWithKids) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF0284C7).copy(alpha = 0.12f),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.FamilyRestroom,
                                            contentDescription = null,
                                            tint = Color(0xFF0284C7),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = Localization.expenseDistribution(lang),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Bar vs Pie icon-only toggle
                            ChartTypeToggle(
                                selectedType = chartTypeFamily,
                                onSelectType = { chartTypeFamily = it },
                                activeColor = Emerald700,
                                appLanguage = appLanguage
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        val safeTotal = totalSpend.coerceAtLeast(1.0)
                        val parentsPercent = ((parentsTotalSpend / safeTotal) * 100).toInt()
                        val kidsPercent = ((kidsSpend / safeTotal) * 100).toInt()

                        if (chartTypeFamily == 1) {
                            // Pie Chart View
                            PieChartView(
                                slices = listOf(
                                    PieChartSlice(Localization.parents(lang), parentsTotalSpend, Color(0xFF0284C7)),
                                    PieChartSlice(Localization.children(lang), kidsSpend, Color(0xFF10B981))
                                ),
                                totalAmount = totalSpend,
                                appLanguage = appLanguage
                            )
                        } else {
                            // Stacked visual bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(12.dp)
                                    .clip(CircleShape)
                            ) {
                                if (parentsTotalSpend > 0) {
                                    Box(
                                        modifier = Modifier
                                            .weight(parentsTotalSpend.toFloat().coerceAtLeast(0.001f))
                                            .fillMaxSize()
                                            .background(Color(0xFF0284C7))
                                    )
                                }
                                if (kidsSpend > 0) {
                                    Box(
                                        modifier = Modifier
                                            .weight(kidsSpend.toFloat().coerceAtLeast(0.001f))
                                            .fillMaxSize()
                                            .background(Color(0xFF10B981))
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Legend and figures
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                // Parents
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF0284C7)))
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text("${Localization.parents(lang)} ($parentsPercent%)", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = String.format(Locale.US, "%.2f €", parentsTotalSpend),
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0284C7)
                                    )
                                }

                                // Kids
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF10B981)))
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text("${Localization.children(lang)} ($kidsPercent%)", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = String.format(Locale.US, "%.2f €", kidsSpend),
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF10B981)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Dedicated Card: Έξοδα Γονέων για τα Παιδιά
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth().testTag("kids_spending_highlight_card")
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF6366F1).copy(alpha = 0.12f),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.FamilyRestroom,
                                            contentDescription = null,
                                            tint = Color(0xFF4F46E5),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = if (lang == Localization.Language.ENGLISH) "Parents' Expenses for Children" else "Έξοδα Γονέων για τα Παιδιά",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF4F46E5).copy(alpha = 0.10f)
                            ) {
                                Text(
                                    text = String.format(Locale.US, "%.2f €", kidsSpend),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF4338CA),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Per-child breakdown
                        if (kidsMembers.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = Localization.noKidsRegisteredHint(lang),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                kidsMembers.forEach { kid ->
                                    val kidSpend = individualSpendMap[kid] ?: 0.0
                                    val kidPercent = if (kidsSpend > 0) (kidSpend / kidsSpend).toFloat().coerceIn(0f, 1f) else 0f
                                    val kidColor = Color(kid.avatarColorHex)

                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(30.dp)
                                                            .clip(CircleShape)
                                                            .background(kidColor),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = kid.name.take(1),
                                                            color = Color.White,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Column {
                                                        Text(
                                                            text = kid.name,
                                                            fontSize = 13.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Text(
                                                            text = if (lang == Localization.Language.ENGLISH) "100% Covered by parents" else "🛡️ 100% Κάλυψη από γονείς",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = Emerald700
                                                        )
                                                    }
                                                }

                                                Text(
                                                    text = String.format(Locale.US, "%.2f €", kidSpend),
                                                    fontSize = 14.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }

                                            if (kidsSpend > 0) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                LinearProgressIndicator(
                                                    progress = { kidPercent },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(5.dp)
                                                        .clip(CircleShape),
                                                    color = kidColor,
                                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
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

        // Section: Καταμερισμός ανά Μέλος Οικογένειας
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = Localization.breakdownByMember(lang),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        )

                        // Bar vs Pie icon-only toggle
                        ChartTypeToggle(
                            selectedType = chartTypeMembers,
                            onSelectType = { chartTypeMembers = it },
                            activeColor = Emerald700,
                            appLanguage = appLanguage
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Mode selection modern segmented pill control:
                    // If family with parents & kids: Individual (0), Parents (1), Kids (2), Who Paid (3)
                    // Otherwise: Individual (0), Who Paid (3)
                    val availableModes = if (hasFamilyWithKids) {
                        listOf(
                            0 to Localization.individualMode(lang),
                            1 to Localization.parentsMode(lang),
                            2 to Localization.kidsMode(lang),
                            3 to (if (lang == Localization.Language.ENGLISH) "Who Paid" else "Ποιος Πλήρωσε")
                        )
                    } else {
                        listOf(
                            0 to Localization.individualMode(lang),
                            3 to (if (lang == Localization.Language.ENGLISH) "Who Paid" else "Ποιος Πλήρωσε")
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(3.dp)) {
                            availableModes.forEach { (modeIndex, title) ->
                                val isSelected = splitMode == modeIndex
                                val bgColor by animateColorAsState(
                                    targetValue = if (isSelected) Emerald700 else Color.Transparent,
                                    animationSpec = tween(220),
                                    label = "split_tab_bg_$modeIndex"
                                )
                                val textColor by animateColorAsState(
                                    targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    animationSpec = tween(220),
                                    label = "split_tab_text_$modeIndex"
                                )
                                Surface(
                                    shape = RoundedCornerShape(11.dp),
                                    color = bgColor,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { splitMode = modeIndex }
                                        .testTag("split_mode_tab_$modeIndex")
                                ) {
                                    Text(
                                        text = title,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = textColor,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (displayedMembers.isEmpty()) {
                        Text(
                            text = if (splitMode == 2) Localization.noKidsRegisteredHint(lang) else Localization.noMembersInCategoryHint(lang),
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else if (chartTypeMembers == 1) {
                        // Pie Chart View for members
                        val memberSlices = displayedMembers.map { m ->
                            PieChartSlice(
                                label = m.name,
                                value = memberSpendMap[m] ?: 0.0,
                                color = Color(m.avatarColorHex)
                            )
                        }

                        val displayedTotal = memberSpendMap.values.sum()
                        PieChartView(
                            slices = memberSlices,
                            totalAmount = displayedTotal,
                            appLanguage = appLanguage
                        )
                    } else {
                        // Bar View for members
                        val safeMax = memberSpendMap.values.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0

                        displayedMembers.forEach { member ->
                            val spend = memberSpendMap[member] ?: 0.0
                            val ind = individualSpendMap[member] ?: 0.0
                            val com = (spend - ind).coerceAtLeast(0.0)
                            val progress = (spend / safeMax).toFloat().coerceIn(0f, 1f)
                            val avatarColor = Color(member.avatarColorHex)

                            Column(modifier = Modifier.padding(vertical = 7.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(avatarColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = member.name.take(1),
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = member.name,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                val mPaid = payerSpendMap[member] ?: 0.0
                                                val mInc = memberIncomeMap[member] ?: 0.0
                                                if (mPaid > mInc && (mPaid > 0.0 || mInc > 0.0)) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = Color(0xFFEF4444).copy(alpha = 0.12f)
                                                    ) {
                                                        Text(
                                                            text = Localization.expensesExceedIncomeTag(lang),
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFFDC2626),
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            val subtitle = when (splitMode) {
                                                3 -> if (lang == Localization.Language.ENGLISH) "Total paid: ${String.format(Locale.US, "%.2f €", spend)}" else "Πλήρωσε συνολικά: ${String.format(Locale.US, "%.2f €", spend)}"
                                                0 -> if (com > 0.01) {
                                                    if (lang == Localization.Language.ENGLISH) "Direct: ${String.format(Locale.US, "%.1f€", ind)} • Shared: ${String.format(Locale.US, "%.1f€", com)}"
                                                    else "Ατομικά: ${String.format(Locale.US, "%.1f€", ind)} • Κοινό: ${String.format(Locale.US, "%.1f€", com)}"
                                                } else {
                                                    Localization.translateRole(member.role, lang)
                                                }
                                                else -> Localization.translateRole(member.role, lang)
                                            }
                                            Text(
                                                text = subtitle,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Text(
                                        text = String.format(Locale.US, "%.2f €", spend),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(CircleShape),
                                    color = avatarColor,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: Έλεγχος Budgets ανά Κατηγορία
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Emerald100,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = Emerald700,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = Localization.budgetMonitoring(lang),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = Localization.categoriesCount(categories.size, lang),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val activeCategories = categories
                        .filter { (categorySpendMap[it] ?: 0.0) > 0.01 || it.monthlyBudget > 0.01 }
                        .sortedWith(
                            compareByDescending<ExpenseCategory> { cat ->
                                val spent = categorySpendMap[cat] ?: 0.0
                                val budget = cat.monthlyBudget
                                if (budget > 0.0) spent / budget else 0.0
                            }.thenByDescending { categorySpendMap[it] ?: 0.0 }
                        )
                    val displayedCats = if (activeCategories.isNotEmpty()) {
                        activeCategories
                    } else {
                        categories.sortedWith(
                            compareByDescending<ExpenseCategory> { cat ->
                                val spent = categorySpendMap[cat] ?: 0.0
                                val budget = cat.monthlyBudget
                                if (budget > 0.0) spent / budget else 0.0
                            }.thenByDescending { categorySpendMap[it] ?: 0.0 }
                        ).take(6)
                    }

                    if (displayedCats.isEmpty()) {
                        Text(
                            text = if (lang == Localization.Language.ENGLISH) "No expenses recorded for this period." else "Δεν υπάρχουν καταγεγραμμένα έξοδα για αυτή την περίοδο.",
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        displayedCats.forEach { cat ->
                            val spent = categorySpendMap[cat] ?: 0.0
                            val budget = cat.monthlyBudget
                            val ratio = if (budget > 0) spent / budget else 0.0
                            val progress = ratio.toFloat().coerceIn(0f, 1f)

                            val isExact = budget > 0 && (Math.abs(spent - budget) < 0.05 || (ratio >= 0.999 && ratio <= 1.001))
                            val isExceeded = budget > 0 && spent > budget + 0.05
                            val isWarning = budget > 0 && !isExact && !isExceeded && ratio >= 0.75

                            val statusText = when {
                                budget <= 0 -> "${String.format(Locale.US, "%.2f", spent)} €"
                                isExceeded -> Localization.exceededBudget(lang)
                                isWarning -> Localization.warningBudget(lang)
                                else -> Localization.withinLimit(lang)
                            }

                            val barColor = when {
                                isExceeded -> Color(0xFFEF4444)
                                isWarning -> Color(0xFFF59E0B)
                                else -> Emerald700
                            }

                            Column(modifier = Modifier.padding(vertical = 5.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .clip(RoundedCornerShape(7.dp))
                                                .background(Color(cat.colorHex).copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = CategoryIconHelper.getIcon(cat.iconKey),
                                                contentDescription = null,
                                                tint = Color(cat.colorHex),
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = Localization.translateCategory(cat.name, lang),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (budget > 0) {
                                            Text(
                                                text = String.format(Locale.US, "%.2f€ / %.0f€", spent, budget),
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = barColor.copy(alpha = 0.12f)
                                            ) {
                                                Text(
                                                    text = "${(ratio * 100).toInt()}%",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = barColor,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        } else {
                                            Text(
                                                text = String.format(Locale.US, "%.2f €", spent),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Emerald700
                                            )
                                        }
                                    }
                                }

                                if (budget > 0) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(5.dp)
                                            .clip(CircleShape),
                                        color = barColor,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
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

@Composable
private fun ChartTypeToggle(
    selectedType: Int, // 0 = Bar, 1 = Pie
    onSelectType: (Int) -> Unit,
    activeColor: Color = Emerald700,
    appLanguage: String = "EN",
    modifier: Modifier = Modifier
) {
    val lang = Localization.Language.fromCode(appLanguage)
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(7.dp),
                color = if (selectedType == 0) activeColor else Color.Transparent,
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .clickable { onSelectType(0) }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = Localization.barChartView(lang),
                        tint = if (selectedType == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(7.dp),
                color = if (selectedType == 1) activeColor else Color.Transparent,
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .clickable { onSelectType(1) }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PieChart,
                        contentDescription = Localization.pieChartView(lang),
                        tint = if (selectedType == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}
