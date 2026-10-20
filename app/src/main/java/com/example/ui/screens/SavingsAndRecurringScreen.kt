package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.RecurringExpense
import com.example.data.entity.SavingsGoal
import com.example.ui.theme.Emerald700
import com.example.ui.theme.Emerald900
import com.example.util.Localization
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SavingsAndRecurringScreen(
    goals: List<SavingsGoal>,
    recurringExpenses: List<RecurringExpense>,
    appLanguage: String = "EN",
    onAddGoal: () -> Unit,
    onEditGoal: (SavingsGoal) -> Unit,
    onDeleteGoal: (SavingsGoal) -> Unit,
    onDepositToGoal: (SavingsGoal, Double) -> Unit,
    onAddRecurring: () -> Unit,
    onEditRecurring: (RecurringExpense) -> Unit,
    onDeleteRecurring: (RecurringExpense) -> Unit,
    onExecuteRecurringNow: (RecurringExpense) -> Unit,
    onToggleRecurringActive: (RecurringExpense) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val lang = Localization.Language.fromCode(appLanguage)

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("savings_and_recurring_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Modern Segmented Pill Switcher: Στόχοι vs Πάγιες (Uniform & Responsive)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(4.dp)) {
                    val goalsBgColor by animateColorAsState(
                        targetValue = if (selectedTab == 0) Emerald700 else Color.Transparent,
                        animationSpec = tween(220),
                        label = "goals_tab_bg"
                    )
                    val goalsTextColor by animateColorAsState(
                        targetValue = if (selectedTab == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = tween(220),
                        label = "goals_tab_text"
                    )
                    val recurringBgColor by animateColorAsState(
                        targetValue = if (selectedTab == 1) Emerald700 else Color.Transparent,
                        animationSpec = tween(220),
                        label = "recurring_tab_bg"
                    )
                    val recurringTextColor by animateColorAsState(
                        targetValue = if (selectedTab == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = tween(220),
                        label = "recurring_tab_text"
                    )

                    // Tab 0: Στόχοι
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = goalsBgColor,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTab = 0 }
                            .testTag("tab_goals_segmented")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Savings,
                                contentDescription = null,
                                tint = goalsTextColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val goalsTitle = if (lang == Localization.Language.ENGLISH) "Goals" else "Στόχοι"
                            Text(
                                text = "$goalsTitle (${goals.size})",
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                                color = goalsTextColor
                            )
                        }
                    }

                    // Tab 1: Πάγιες
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = recurringBgColor,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTab = 1 }
                            .testTag("tab_recurring_segmented")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.EventRepeat,
                                contentDescription = null,
                                tint = recurringTextColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val recurringTitle = if (lang == Localization.Language.ENGLISH) "Standing Orders" else "Πάγιες Εντολές"
                            Text(
                                text = "$recurringTitle (${recurringExpenses.size})",
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                color = recurringTextColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tab Content with proper Column wrappers so items NEVER overlap!
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(140)) togetherWith fadeOut(animationSpec = tween(100)))
                        .using(SizeTransform(clip = false))
                },
                label = "savings_recurring_tab_animated",
                modifier = Modifier.fillMaxSize()
            ) { tabIndex ->
                if (tabIndex == 0) {
                    // TAB 0: SAVINGS GOALS
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val totalSaved = goals.sumOf { it.currentAmount }
                        val totalTarget = goals.sumOf { it.targetAmount }
                        val overallPercentage = if (totalTarget > 0) (totalSaved / totalTarget).toFloat() else 0f

                        // Forest Green Summary Card (Uniform with app style)
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = Color(0xFF044837),
                            border = BorderStroke(1.dp, Color(0xFF065F46).copy(alpha = 0.6f)),
                            tonalElevation = 4.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color(0xFF044837), Color(0xFF023628))
                                        )
                                    )
                                    .padding(18.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = Localization.totalFamilySavings(lang).uppercase(Locale.getDefault()),
                                            color = Color(0xFFA7F3D0),
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.8.sp
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color(0xFF065F46).copy(alpha = 0.8f)
                                        ) {
                                            Text(
                                                text = "${goals.size} ${if (lang == Localization.Language.ENGLISH) "goals" else "στόχοι"}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFFA7F3D0),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        Text(
                                            text = String.format(Locale.US, "%.0f €", totalSaved),
                                            fontSize = 30.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "${Localization.ofTarget(lang)} ${String.format(Locale.US, "%.0f", totalTarget)} € (${(overallPercentage * 100).toInt()}%)",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFFA7F3D0)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    LinearProgressIndicator(
                                        progress = { overallPercentage.coerceIn(0f, 1f) },
                                        color = Color(0xFF34D399),
                                        trackColor = Color(0xFF065F46).copy(alpha = 0.8f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(7.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Section Header Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = Localization.ourGoals(lang),
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Button(
                                onClick = onAddGoal,
                                colors = ButtonDefaults.buttonColors(containerColor = Emerald700, contentColor = Color.White),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = Localization.newGoal(lang),
                                    fontSize = 12.5.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Content List or Beautiful Uniform Empty State
                        if (goals.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                tonalElevation = 1.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFD1FAE5),
                                        modifier = Modifier.size(64.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Savings,
                                                contentDescription = null,
                                                tint = Color(0xFF059669),
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = if (lang == Localization.Language.ENGLISH) "No savings goals yet" else "Δεν υπάρχουν ακόμη στόχοι αποταμίευσης",
                                        fontSize = 16.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = if (lang == Localization.Language.ENGLISH)
                                            "Set a goal for holidays, emergency fund, or major purchases and track your progress easily."
                                        else
                                            "Ορίστε έναν στόχο για διακοπές, αυτοκίνητο, αποθεματικό ανάγκης ή νέο εξοπλισμό και παρακολουθήστε την πορεία σας.",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 18.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )

                                    Spacer(modifier = Modifier.height(18.dp))

                                    Button(
                                        onClick = onAddGoal,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Emerald700)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (lang == Localization.Language.ENGLISH) "+ Add Goal" else "+ Προσθήκη Στόχου",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(bottom = 100.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(goals, key = { it.id }) { goal ->
                                    val percentage = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat() else 0f
                                    val isCompleted = goal.currentAmount >= goal.targetAmount

                                    Card(
                                        shape = RoundedCornerShape(18.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(1.dp, if (isCompleted) Color(0xFF10B981) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = if (isCompleted) Color(0xFF10B981).copy(alpha = 0.15f) else Color(goal.colorHex).copy(alpha = 0.15f),
                                                        modifier = Modifier.size(38.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(
                                                                imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.Flag,
                                                                contentDescription = null,
                                                                tint = if (isCompleted) Color(0xFF10B981) else Color(goal.colorHex),
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Column {
                                                        Text(
                                                            text = goal.title,
                                                            fontSize = 15.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        if (goal.deadlineDate.isNotBlank()) {
                                                            Text(
                                                                text = "${Localization.deadlinePrefix(lang)}${goal.deadlineDate}",
                                                                fontSize = 11.5.sp,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                }

                                                Row {
                                                    IconButton(onClick = { onEditGoal(goal) }) {
                                                        Icon(Icons.Default.Edit, contentDescription = Localization.edit(lang), modifier = Modifier.size(18.dp))
                                                    }
                                                    IconButton(onClick = { onDeleteGoal(goal) }) {
                                                        Icon(Icons.Default.Delete, contentDescription = Localization.delete(lang), modifier = Modifier.size(18.dp), tint = Color(0xFFEF4444))
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "${String.format(Locale.US, "%.0f", goal.currentAmount)}€ / ${String.format(Locale.US, "%.0f", goal.targetAmount)}€",
                                                    fontSize = 13.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isCompleted) Color(0xFF10B981) else Emerald700
                                                )
                                                Text(
                                                    text = "${(percentage * 100).toInt()}%",
                                                    fontSize = 13.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isCompleted) Color(0xFF10B981) else Emerald700
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))

                                            LinearProgressIndicator(
                                                progress = { percentage.coerceIn(0f, 1f) },
                                                color = if (isCompleted) Color(0xFF10B981) else Color(goal.colorHex),
                                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(8.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                            )

                                            if (isCompleted) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = Localization.goalAchievedCongratulations(lang),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF10B981)
                                                )
                                            } else {
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    listOf(20.0, 50.0, 100.0).forEach { amount ->
                                                        Surface(
                                                            shape = RoundedCornerShape(8.dp),
                                                            color = Emerald700.copy(alpha = 0.12f),
                                                            border = BorderStroke(1.dp, Emerald700.copy(alpha = 0.35f)),
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .clickable { onDepositToGoal(goal, amount) }
                                                        ) {
                                                            Text(
                                                                text = "+${amount.toInt()}€",
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Emerald700,
                                                                textAlign = TextAlign.Center,
                                                                modifier = Modifier.padding(vertical = 6.dp)
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
                } else {
                    // TAB 1: RECURRING EXPENSES
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val monthlyTotal = recurringExpenses.filter { it.isActive }.sumOf { it.amount }
                        val activeCount = recurringExpenses.count { it.isActive }

                        // Forest Green Summary Card (Uniform with Goals card)
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = Color(0xFF044837),
                            border = BorderStroke(1.dp, Color(0xFF065F46).copy(alpha = 0.6f)),
                            tonalElevation = 4.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color(0xFF044837), Color(0xFF023628))
                                        )
                                    )
                                    .padding(18.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = Localization.totalStandingOrders(lang).uppercase(Locale.getDefault()),
                                            color = Color(0xFFA7F3D0),
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.8.sp
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color(0xFF065F46).copy(alpha = 0.8f)
                                        ) {
                                            Text(
                                                text = "$activeCount ${if (lang == Localization.Language.ENGLISH) "active" else "ενεργές"}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFFA7F3D0),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        Text(
                                            text = String.format(Locale.US, "%.2f €", monthlyTotal),
                                            fontSize = 30.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                        Text(
                                            text = Localization.perMonthSuffix(lang),
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFFA7F3D0)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Section Header Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (lang == Localization.Language.ENGLISH) "Our Standing Orders" else "Οι Πάγιες Εντολές μας",
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Button(
                                onClick = onAddRecurring,
                                colors = ButtonDefaults.buttonColors(containerColor = Emerald700, contentColor = Color.White),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = Localization.newStandingOrder(lang),
                                    fontSize = 12.5.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Content List or Beautiful Uniform Empty State
                        if (recurringExpenses.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                tonalElevation = 1.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFD1FAE5),
                                        modifier = Modifier.size(64.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.EventRepeat,
                                                contentDescription = null,
                                                tint = Color(0xFF059669),
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = if (lang == Localization.Language.ENGLISH) "No standing orders yet" else "Δεν υπάρχουν ακόμη πάγιες εντολές",
                                        fontSize = 16.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = if (lang == Localization.Language.ENGLISH)
                                            "Add monthly recurring obligations like rent, bills, internet, or subscriptions for automatic logging."
                                        else
                                            "Καταχωρήστε μηνιαία πάγια έξοδα (ενοίκιο, λογαριασμούς, τηλέφωνο, συνδρομές) για αυτόματη δημιουργία εξόδου κάθε μήνα.",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 18.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )

                                    Spacer(modifier = Modifier.height(18.dp))

                                    Button(
                                        onClick = onAddRecurring,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Emerald700)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (lang == Localization.Language.ENGLISH) "+ Add Standing Order" else "+ Προσθήκη Πάγιας Εντολής",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(bottom = 100.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(recurringExpenses, key = { it.id }) { recurring ->
                                    Card(
                                        shape = RoundedCornerShape(18.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = recurring.title,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${Localization.translateCategory(recurring.categoryName, lang)} • ${Localization.everyDayOfMonth(lang, recurring.dayOfMonth)}",
                                                    fontSize = 11.5.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = String.format(Locale.US, "%.2f €", recurring.amount),
                                                    fontSize = 16.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Emerald700
                                                )

                                                // Clean payer display without any parentheses
                                                val cleanPayer = recurring.payerMemberName.replace(Regex("\\s*\\([^)]*\\)"), "").trim()
                                                if (cleanPayer.isNotBlank()) {
                                                    Spacer(modifier = Modifier.height(3.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = Emerald700.copy(alpha = 0.12f)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.CreditCard,
                                                                contentDescription = null,
                                                                tint = Emerald700,
                                                                modifier = Modifier.size(11.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(
                                                                text = if (lang == Localization.Language.ENGLISH) "Paid by: $cleanPayer" else "Πληρωμή από: $cleanPayer",
                                                                fontSize = 10.5.sp,
                                                                color = Emerald700,
                                                                fontWeight = FontWeight.SemiBold
                                                            )
                                                        }
                                                    }
                                                }

                                                val currentMonthKey = remember { SimpleDateFormat("yyyy-MM", Locale.US).format(Date()) }
                                                val isGeneratedThisMonth = recurring.lastGeneratedMonth == currentMonthKey
                                                Spacer(modifier = Modifier.height(4.dp))
                                                if (isGeneratedThisMonth) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = Emerald700.copy(alpha = 0.12f)
                                                    ) {
                                                        Text(
                                                            text = if (lang == Localization.Language.ENGLISH) "Registered for this month ✓" else "Καταχωρήθηκε για αυτόν τον μήνα ✓",
                                                            fontSize = 10.5.sp,
                                                            color = Emerald700,
                                                            fontWeight = FontWeight.SemiBold,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                } else if (recurring.isActive) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = MaterialTheme.colorScheme.surfaceVariant
                                                    ) {
                                                        Text(
                                                            text = if (lang == Localization.Language.ENGLISH) "Auto on day ${recurring.dayOfMonth}" else "Αυτόματη καταχώρηση στις ${recurring.dayOfMonth}η",
                                                            fontSize = 10.5.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            fontWeight = FontWeight.Medium,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                } else {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                    ) {
                                                        Text(
                                                            text = if (lang == Localization.Language.ENGLISH) "Inactive" else "Ανενεργή",
                                                            fontSize = 10.5.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                // Run now button
                                                IconButton(
                                                    onClick = { onExecuteRecurringNow(recurring) },
                                                    modifier = Modifier.size(34.dp)
                                                ) {
                                                    Icon(Icons.Default.PlayArrow, contentDescription = Localization.executeNowAction(lang), tint = Emerald700)
                                                }

                                                IconButton(
                                                    onClick = { onEditRecurring(recurring) },
                                                    modifier = Modifier.size(34.dp)
                                                ) {
                                                    Icon(Icons.Default.Edit, contentDescription = Localization.edit(lang), modifier = Modifier.size(18.dp))
                                                }

                                                IconButton(
                                                    onClick = { onDeleteRecurring(recurring) },
                                                    modifier = Modifier.size(34.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = Localization.delete(lang), modifier = Modifier.size(18.dp), tint = Color(0xFFEF4444))
                                                }

                                                Switch(
                                                    checked = recurring.isActive,
                                                    onCheckedChange = { onToggleRecurringActive(recurring) },
                                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Emerald700)
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
