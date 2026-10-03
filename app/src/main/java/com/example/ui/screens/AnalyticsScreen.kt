package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.Expense
import com.example.data.entity.ExpenseCategory
import com.example.data.entity.FamilyMember
import com.example.ui.components.BudgetAlertsCard
import com.example.ui.components.CategoryIconHelper
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald700
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import java.util.Locale

@Composable
fun AnalyticsScreen(
    expenses: List<Expense>,
    categories: List<ExpenseCategory>,
    members: List<FamilyMember>,
    onExportReport: () -> Unit = {},
    onManageCategories: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val totalSpend = expenses.sumOf { it.amount }

    // 1. Parents vs Kids breakdown
    var parentsSpend = 0.0
    var kidsSpend = 0.0
    var commonSpend = 0.0

    expenses.forEach { exp ->
        if (exp.splitType == "ALL_FAMILY") {
            commonSpend += exp.amount
        } else {
            val ids = exp.memberIdsCsv.split(",").mapNotNull { it.trim().toLongOrNull() }
            if (ids.isNotEmpty()) {
                val perPerson = exp.amount / ids.size
                ids.forEach { id ->
                    val member = members.find { it.id == id }
                    if (member?.role.equals("Παιδί", ignoreCase = true)) {
                        kidsSpend += perPerson
                    } else if (member?.role.equals("Γονέας", ignoreCase = true)) {
                        parentsSpend += perPerson
                    } else {
                        commonSpend += perPerson
                    }
                }
            } else {
                commonSpend += exp.amount
            }
        }
    }

    // 2. Member spending distribution (individual assigned + equal share of common)
    val memberSpendMap = members.associateWith { member ->
        var sum = 0.0
        val commonShare = if (members.isNotEmpty()) commonSpend / members.size else 0.0
        sum += commonShare

        expenses.filter { it.splitType != "ALL_FAMILY" }.forEach { exp ->
            val ids = exp.memberIdsCsv.split(",").mapNotNull { it.trim().toLongOrNull() }
            if (ids.contains(member.id)) {
                sum += exp.amount / ids.size
            }
        }
        sum
    }

    // 3. Category budget analysis
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
                                    text = "Στατιστική Ανάλυση",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Οικογενειακός ισολογισμός μήνα",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = onExportReport,
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald700),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Excel", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Σύνολο Εξόδων", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = String.format(Locale.US, "%.2f €", totalSpend),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Emerald700
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Συναλλαγές", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${expenses.size}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                            text = "Κατανομή: Γονείς vs Παιδιά vs Κοινό",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val safeTotal = totalSpend.coerceAtLeast(1.0)
                    val parentsPercent = ((parentsSpend / safeTotal) * 100).toInt()
                    val kidsPercent = ((kidsSpend / safeTotal) * 100).toInt()
                    val commonPercent = ((commonSpend / safeTotal) * 100).toInt()

                    // Stacked visual bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(CircleShape)
                    ) {
                        if (parentsSpend > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(parentsSpend.toFloat().coerceAtLeast(0.001f))
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
                        if (commonSpend > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(commonSpend.toFloat().coerceAtLeast(0.001f))
                                    .fillMaxSize()
                                    .background(Color(0xFFF59E0B))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Legend and figures
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Parents
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF0284C7)))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("Γονείς ($parentsPercent%)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = String.format(Locale.US, "%.2f €", parentsSpend),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0284C7)
                            )
                        }

                        // Kids
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF10B981)))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("Παιδιά ($kidsPercent%)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = String.format(Locale.US, "%.2f €", kidsSpend),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                        }

                        // Common
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFF59E0B)))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("Κοινά ($commonPercent%)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = String.format(Locale.US, "%.2f €", commonSpend),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF59E0B)
                            )
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
                    Text(
                        text = "Καταμερισμός ανά Μέλος",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Ατομικά έξοδα + ισόποσο μερίδιο κοινών εξόδων σπιτιού",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val safeMax = memberSpendMap.values.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0

                    members.forEach { member ->
                        val spend = memberSpendMap[member] ?: 0.0
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
                                        Text(
                                            text = member.role,
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
                                text = "Έλεγχος Budgets",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "${categories.size} κατηγορίες",
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
                            ratio >= 1.0 -> "Υπέρβαση"
                            ratio >= 0.75 -> "Προσοχή"
                            else -> "Εντός ορίου"
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
                                        text = cat.name,
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
