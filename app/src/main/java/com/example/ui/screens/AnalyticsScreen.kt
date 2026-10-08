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
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    appLanguage: String = "EL",
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
            modifier = Modifier.size(160.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                val strokeWidth = 24.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val centerOffset = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
                val rect = androidx.compose.ui.geometry.Rect(
                    centerOffset.x - radius,
                    centerOffset.y - radius,
                    centerOffset.x + radius,
                    centerOffset.y + radius
                )

                var startAngle = -90f
                validSlices.forEach { slice ->
                    val sweepAngle = ((slice.value / total) * 360f).toFloat()
                    drawArc(
                        color = slice.color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = rect.topLeft,
                        size = rect.size,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                    )
                    startAngle += sweepAngle
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = String.format(Locale.US, "%.1f€", totalAmount),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = Localization.total(lang),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
    appLanguage: String = "EL",
    onExportReport: () -> Unit = {},
    onManageCategories: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val lang = Localization.Language.fromCode(appLanguage)
    var splitMode by remember { mutableIntStateOf(0) } // 0 = Ατομικά, 1 = Γονείς + Κοινά, 2 = Ισόποσα σε όλους
    var chartTypeFamily by remember { mutableIntStateOf(0) } // 0 = Μπάρες, 1 = Πίτα (Requirement 6)
    var chartTypeMembers by remember { mutableIntStateOf(0) } // 0 = Μπάρες, 1 = Πίτα (Requirement 6)
    var chartTypeIncome by remember { mutableIntStateOf(0) } // 0 = Μπάρες, 1 = Πίτα

    val totalSpend = expenses.sumOf { it.amount }
    val totalIncome = incomes.sumOf { it.amount }
    val netBalance = totalIncome - totalSpend
    val savingsRate = if (totalIncome > 0) ((netBalance / totalIncome) * 100).coerceIn(-100.0, 100.0) else 0.0

    // Income breakdown by Member
    val memberIncomeMap = members.associateWith { member ->
        incomes.filter { it.memberId == member.id }.sumOf { it.amount }
    }
    val unassignedMemberIncomes = incomes.filter { inc ->
        members.none { it.id == inc.memberId }
    }.sumOf { it.amount }

    // Income breakdown by Category
    val categoryIncomeMap = incomes.groupBy { it.category }
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

        // 2. By name in memberNamesDisplay
        if (exp.memberNamesDisplay.isNotBlank() && !exp.memberNamesDisplay.equals("Κοινό Οικογένειας", ignoreCase = true)) {
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

    expenses.forEach { exp ->
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

    // 3. Member spending distribution based on splitMode: 0 = Ατομικά, 1 = Γονείς, 2 = Παιδιά
    val displayedMembers = when (splitMode) {
        0 -> members // Ατομικά
        1 -> if (parentsList.isNotEmpty()) parentsList else members.filter { !it.role.equals("Παιδί", ignoreCase = true) }.ifEmpty { members } // Γονείς
        2 -> members.filter { it.role.equals("Παιδί", ignoreCase = true) } // Παιδιά
        else -> members
    }

    val memberSpendMap = displayedMembers.associateWith { member ->
        val ind = individualSpendMap[member] ?: 0.0
        val comShare = if (splitMode == 1) {
            if (parentsList.isNotEmpty()) commonHouseholdSpend / parentsList.size else 0.0
        } else {
            0.0
        }
        ind + comShare
    }

    // 4. Category budget analysis
    val categorySpendMap = categories.associateWith { cat ->
        expenses.filter { it.categoryId == cat.id }.sumOf { it.amount }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("analytics_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Feature 3: Monthly Budget Health & Exceeded Alerts Card
        item {
            BudgetAlertsCard(
                expenses = expenses,
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
                                    text = Localization.familyMonthlyBalance(lang),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Requirement 5: Sleek and discrete Excel export button
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Emerald700.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, Emerald700.copy(alpha = 0.25f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onExportReport() }
                                .testTag("export_excel_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TableChart,
                                    contentDescription = Localization.exportToExcel(lang),
                                    tint = Emerald700,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Excel",
                                    color = Emerald700,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold
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
                                    text = Localization.entriesCount(incomes.size, lang),
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
                                    text = Localization.transactionsCount(expenses.size, lang),
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
                            Column {
                                Text(
                                    text = Localization.familyIncome(lang),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = Localization.contributionByMember(lang),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
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

                    if (incomes.isEmpty()) {
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

        // Section: Γονείς vs Παιδιά vs Κοινό
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

                    Text(
                        text = when (splitMode) {
                            0 -> Localization.individualSpendingSubtitle(lang)
                            1 -> Localization.parentsSpendingSubtitle(lang)
                            else -> Localization.kidsSpendingSubtitle(lang)
                        },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Mode selection modern segmented pill control: Individual, Parents, Kids
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(3.dp)) {
                            val modes = listOf(Localization.individualMode(lang), Localization.parentsMode(lang), Localization.kidsMode(lang))
                            modes.forEachIndexed { index, title ->
                                val isSelected = splitMode == index
                                val bgColor by animateColorAsState(
                                    targetValue = if (isSelected) Emerald700 else Color.Transparent,
                                    animationSpec = tween(220),
                                    label = "split_tab_bg_$index"
                                )
                                val textColor by animateColorAsState(
                                    targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    animationSpec = tween(220),
                                    label = "split_tab_text_$index"
                                )
                                Surface(
                                    shape = RoundedCornerShape(11.dp),
                                    color = bgColor,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { splitMode = index }
                                        .testTag("split_mode_tab_$index")
                                ) {
                                    Text(
                                        text = title,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = textColor,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp)
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
                                            Text(
                                                text = member.name,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            val subtitle = when (splitMode) {
                                                0 -> Localization.individualExpensesSubtitle(member.role, lang)
                                                1 -> if (com > 0.01) "${Localization.translateRole(member.role, lang)} • ${if (lang == Localization.Language.ENGLISH) "Ind" else "Ατομ"}: ${String.format(Locale.US, "%.1f€", ind)} + ${if (lang == Localization.Language.ENGLISH) "Home" else "Σπίτι"}: ${String.format(Locale.US, "%.1f€", com)}" else Localization.individualExpensesSubtitle(member.role, lang)
                                                else -> Localization.kidsExpensesSubtitle(lang)
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

                    Spacer(modifier = Modifier.height(14.dp))

                    categories.forEach { cat ->
                        val spent = categorySpendMap[cat] ?: 0.0
                        val budget = cat.monthlyBudget
                        val ratio = if (budget > 0) spent / budget else 0.0
                        val progress = ratio.toFloat().coerceIn(0f, 1f)

                        val statusText = when {
                            ratio >= 1.0 -> Localization.exceededBudget(lang)
                            ratio >= 0.75 -> Localization.warningBudget(lang)
                            else -> Localization.withinBudget(lang)
                        }

                        val barColor = when {
                            ratio >= 1.0 -> Color(0xFFEF4444)
                            ratio >= 0.75 -> Color(0xFFF59E0B)
                            else -> Emerald700
                        }

                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(cat.colorHex).copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = CategoryIconHelper.getIcon(cat.iconKey),
                                            contentDescription = null,
                                            tint = Color(cat.colorHex),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = Localization.translateCategory(cat.name, lang),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = barColor.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = statusText,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = barColor,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(CircleShape),
                                color = barColor,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = String.format(Locale.US, "%.2f€ / %.0f€", spent, budget),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${(ratio * 100).toInt()}%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = barColor
                                )
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
    appLanguage: String = "EL",
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
