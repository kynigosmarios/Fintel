package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.RecurringExpense
import com.example.data.entity.SavingsGoal
import com.example.ui.theme.Emerald700
import com.example.ui.theme.Emerald900

@Composable
fun SavingsAndRecurringScreen(
    goals: List<SavingsGoal>,
    recurringExpenses: List<RecurringExpense>,
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("savings_and_recurring_screen")
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Tab Row: Goals vs Recurring
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = Emerald700,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Savings, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Στόχοι (${goals.size})", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EventRepeat, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Πάγιες (${recurringExpenses.size})", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (selectedTab == 0) {
            // SAVINGS GOALS TAB
            val totalSaved = goals.sumOf { it.currentAmount }
            val totalTarget = goals.sumOf { it.targetAmount }
            val overallPercentage = if (totalTarget > 0) (totalSaved / totalTarget).toFloat() else 0f

            // Summary Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Emerald900),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Συνολική Οικογενειακή Αποταμίευση", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "${String.format(java.util.Locale.US, "%.0f", totalSaved)} €",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "από ${String.format(java.util.Locale.US, "%.0f", totalTarget)} € (${(overallPercentage * 100).toInt()}%)",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { overallPercentage.coerceIn(0f, 1f) },
                        color = Color(0xFF34D399),
                        trackColor = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Οι Στόχοι Μας", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Button(
                    onClick = onAddGoal,
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald700),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Νέος Στόχος", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(goals, key = { it.id }) { goal ->
                    val percentage = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat() else 0f
                    val isCompleted = goal.currentAmount >= goal.targetAmount

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, if (isCompleted) Color(0xFF10B981) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isCompleted) Color(0xFF10B981).copy(alpha = 0.15f) else Color(goal.colorHex).copy(alpha = 0.15f),
                                        modifier = Modifier.size(36.dp)
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
                                        Text(goal.title, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                        Text("Ημερομηνία: ${goal.deadlineDate}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Row {
                                    IconButton(onClick = { onEditGoal(goal) }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Επεξεργασία", modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(onClick = { onDeleteGoal(goal) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Διαγραφή", modifier = Modifier.size(18.dp), tint = Color(0xFFEF4444))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${String.format(java.util.Locale.US, "%.0f", goal.currentAmount)}€ / ${String.format(java.util.Locale.US, "%.0f", goal.targetAmount)}€",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCompleted) Color(0xFF10B981) else Emerald700
                                )
                                Text(
                                    text = "${(percentage * 100).toInt()}%",
                                    fontSize = 13.sp,
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
                                    text = "🎉 Συγχαρητήρια! Ο στόχος επετεύχθη!",
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
                                            color = Emerald700.copy(alpha = 0.1f),
                                            border = BorderStroke(1.dp, Emerald700.copy(alpha = 0.3f)),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { onDepositToGoal(goal, amount) }
                                        ) {
                                            Text(
                                                text = "+${amount.toInt()}€",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Emerald700,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
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
        } else {
            // RECURRING EXPENSES TAB
            val monthlyTotal = recurringExpenses.filter { it.isActive }.sumOf { it.amount }

            // Summary Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Σύνολο Πάγιων Εντολών", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${String.format(java.util.Locale.US, "%.2f", monthlyTotal)} € / μήνα",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Emerald700
                        )
                    }
                    Button(
                        onClick = onAddRecurring,
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald700),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Νέα Πάγια", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(recurringExpenses, key = { it.id }) { recurring ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
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
                                Text(recurring.title, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${recurring.categoryName} • Κάθε ${recurring.dayOfMonth}η του μήνα",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${String.format(java.util.Locale.US, "%.2f", recurring.amount)} €",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Emerald700
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Run now button
                                IconButton(
                                    onClick = { onExecuteRecurringNow(recurring) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Εκτέλεση τώρα", tint = Emerald700)
                                }

                                IconButton(
                                    onClick = { onEditRecurring(recurring) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Επεξεργασία", modifier = Modifier.size(18.dp))
                                }

                                IconButton(
                                    onClick = { onDeleteRecurring(recurring) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Διαγραφή", modifier = Modifier.size(18.dp), tint = Color(0xFFEF4444))
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
