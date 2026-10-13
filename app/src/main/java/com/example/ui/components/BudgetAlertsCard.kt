package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.example.ui.theme.Emerald700
import com.example.util.Localization
import java.util.Calendar

data class CategoryBudgetStatus(
    val category: ExpenseCategory,
    val spent: Double,
    val budget: Double,
    val percentage: Float,
    val isExact: Boolean = false,
    val isExceeded: Boolean = false,
    val isWarning: Boolean = false
)

@Composable
fun BudgetAlertsCard(
    expenses: List<Expense>,
    categories: List<ExpenseCategory>,
    appLanguage: String = "EL",
    onManageBudgets: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val lang = Localization.Language.fromCode(appLanguage)
    // Current month filter
    val currentMonthExpenses = remember(expenses) {
        val cal = Calendar.getInstance()
        val curYear = cal.get(Calendar.YEAR)
        val curMonth = cal.get(Calendar.MONTH)
        expenses.filter {
            val expCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            expCal.get(Calendar.YEAR) == curYear && expCal.get(Calendar.MONTH) == curMonth
        }
    }

    val budgetStatuses = remember(currentMonthExpenses, categories) {
        categories.filter { it.monthlyBudget > 0 }.map { cat ->
            val spent = currentMonthExpenses.filter { it.categoryId == cat.id }.sumOf { it.amount }
            val budget = cat.monthlyBudget
            val percentage = (spent / budget).toFloat()
            val isExact = Math.abs(spent - budget) < 0.05 || (percentage >= 0.999f && percentage <= 1.001f)
            val isExceeded = spent > budget + 0.05
            val isWarning = !isExact && !isExceeded && percentage >= 0.80f
            CategoryBudgetStatus(
                category = cat,
                spent = spent,
                budget = budget,
                percentage = percentage,
                isExact = isExact,
                isExceeded = isExceeded,
                isWarning = isWarning
            )
        }.sortedByDescending { it.percentage }
    }

    val exceededCount = budgetStatuses.count { it.isExceeded }
    val warningCount = budgetStatuses.count { it.isWarning }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            when {
                exceededCount > 0 -> Color(0xFFEF4444).copy(alpha = 0.5f)
                warningCount > 0 -> Color(0xFFF59E0B).copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("budget_alerts_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = when {
                            exceededCount > 0 -> Color(0xFFEF4444).copy(alpha = 0.15f)
                            warningCount > 0 -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                            else -> Emerald700.copy(alpha = 0.15f)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = when {
                                    exceededCount > 0 -> Icons.Default.ErrorOutline
                                    warningCount > 0 -> Icons.Default.WarningAmber
                                    else -> Icons.Default.NotificationsActive
                                },
                                contentDescription = null,
                                tint = when {
                                    exceededCount > 0 -> Color(0xFFEF4444)
                                    warningCount > 0 -> Color(0xFFF59E0B)
                                    else -> Emerald700
                                },
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = Localization.monthlyBudgetLimits(lang),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = when {
                                exceededCount > 0 -> Localization.categoriesExceededLimit(lang, exceededCount)
                                warningCount > 0 -> Localization.categoriesApproachingLimit(lang, warningCount)
                                else -> Localization.allBudgetsHealthy(lang)
                            },
                            fontSize = 11.sp,
                            fontWeight = if (exceededCount > 0 || warningCount > 0) FontWeight.SemiBold else FontWeight.Normal,
                            color = when {
                                exceededCount > 0 -> Color(0xFFEF4444)
                                warningCount > 0 -> Color(0xFFF59E0B)
                                else -> Emerald700
                            }
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clickable { onManageBudgets() }
                ) {
                    Text(
                        text = Localization.tabCategories(lang),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Emerald700,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Show top budget categories
            budgetStatuses.take(4).forEach { status ->
                val progressClamped = status.percentage.coerceIn(0f, 1f)
                val barColor = when {
                    status.isExact -> Color(0xFF2563EB)
                    status.isExceeded -> Color(0xFFEF4444)
                    status.isWarning -> Color(0xFFF59E0B)
                    else -> Emerald700
                }

                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = Localization.translateCategory(status.category.name, lang),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${String.format(java.util.Locale.US, "%.1f", status.spent)}€ / ${String.format(java.util.Locale.US, "%.0f", status.budget)}€ (${(status.percentage * 100).toInt()}%)",
                            fontSize = 11.sp,
                            fontWeight = if (status.isExceeded) FontWeight.Bold else FontWeight.Normal,
                            color = barColor
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { progressClamped },
                        color = barColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }
            }
        }
    }
}
