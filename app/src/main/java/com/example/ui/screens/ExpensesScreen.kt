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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.Expense
import com.example.data.entity.ExpenseCategory
import com.example.data.entity.FamilyMember
import com.example.ui.components.CategoryIconHelper
import com.example.ui.theme.Emerald100
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

@Composable
fun ExpensesScreen(
    expenses: List<Expense>,
    categories: List<ExpenseCategory>,
    members: List<FamilyMember>,
    selectedMemberFilter: Long?,
    selectedCategoryFilter: Long?,
    searchQuery: String,
    onMemberFilterChange: (Long?) -> Unit,
    onCategoryFilterChange: (Long?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onAddManualExpense: () -> Unit,
    onEditExpense: (Expense) -> Unit,
    onDeleteExpense: (Expense) -> Unit,
    modifier: Modifier = Modifier
) {
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }

    // Summary calculations
    val totalSpend = expenses.sumOf { it.amount }
    val commonSpend = expenses.filter { it.splitType == "ALL_FAMILY" }.sumOf { it.amount }
    val individualSpend = totalSpend - commonSpend
    val totalBudget = categories.sumOf { it.monthlyBudget }.coerceAtLeast(1.0)
    val budgetProgress = (totalSpend / totalBudget).toFloat().coerceIn(0f, 1f)
    val remainingBudget = (totalBudget - totalSpend).coerceAtLeast(0.0)

    // Group expenses by friendly date
    val groupedExpenses = remember(expenses) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = dateFormat.format(Date())
        val yesterdayStr = dateFormat.format(Date(System.currentTimeMillis() - 24L * 3600 * 1000))
        val displayFormat = SimpleDateFormat("EEEE, d MMMM", Locale.forLanguageTag("el-GR"))

        expenses.groupBy { exp ->
            val dateKey = dateFormat.format(Date(exp.timestamp))
            when (dateKey) {
                todayStr -> "Σήμερα"
                yesterdayStr -> "Χθες"
                else -> displayFormat.format(Date(exp.timestamp)).replaceFirstChar { it.uppercase() }
            }
        }
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
                        // Top row: Month & Status tag + Add Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Month badge
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
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
                                        text = "Έξοδα Μήνα",
                                        color = Emerald100,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            // Modern glass manual add button
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Emerald500.copy(alpha = 0.25f),
                                border = BorderStroke(1.dp, Emerald400.copy(alpha = 0.4f)),
                                modifier = Modifier.clickable { onAddManualExpense() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Νέο έξοδο",
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Χειροκίνητο",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Amount Display
                        Row(
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = String.format(Locale.US, "%.2f", totalSpend),
                                color = Color.White,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-1).sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "€",
                                color = Emerald300,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Budget Progress & Remaining Track
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Υπόλοιπο: ${String.format(Locale.US, "%.2f €", remainingBudget)}",
                                color = Emerald100,
                                fontSize = 12.sp,
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

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { budgetProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = if (budgetProgress > 0.9f) Color(0xFFF87171) else Emerald400,
                            trackColor = Color.White.copy(alpha = 0.2f)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Dual Breakdown Capsules (Common vs Individual)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
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
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = String.format(Locale.US, "%.2f €", commonSpend),
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
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
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = String.format(Locale.US, "%.2f €", individualSpend),
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Modern Search Bar & Horizontal Filter Rows
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

                Spacer(modifier = Modifier.height(10.dp))

                // Member Filter Row with Colored Avatar Badges
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedMemberFilter == null,
                        onClick = { onMemberFilterChange(null) },
                        label = { Text("Όλοι", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Emerald700.copy(alpha = 0.15f),
                            selectedLabelColor = Emerald700
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedMemberFilter == null,
                            borderColor = if (selectedMemberFilter == null) Emerald700 else MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                    FilterChip(
                        selected = selectedMemberFilter == -1L,
                        onClick = { onMemberFilterChange(if (selectedMemberFilter == -1L) null else -1L) },
                        label = { Text("Κοινό Οικογένειας", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Emerald700.copy(alpha = 0.15f),
                            selectedLabelColor = Emerald700
                        )
                    )
                    members.forEach { m ->
                        val isSelected = selectedMemberFilter == m.id
                        val avatarColor = Color(m.avatarColorHex)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onMemberFilterChange(if (isSelected) null else m.id) },
                            label = { Text(m.name, fontSize = 11.sp) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(avatarColor)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = avatarColor.copy(alpha = 0.2f),
                                selectedLabelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) avatarColor else MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Category Filter Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedCategoryFilter == null,
                        onClick = { onCategoryFilterChange(null) },
                        label = { Text("Όλες οι Κατηγορίες", fontSize = 11.sp) }
                    )
                    categories.forEach { cat ->
                        val isSelected = selectedCategoryFilter == cat.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { onCategoryFilterChange(if (isSelected) null else cat.id) },
                            label = { Text(cat.name, fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = CategoryIconHelper.getIcon(cat.iconKey),
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = Color(cat.colorHex)
                                )
                            }
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(10.dp)) }

        // Empty state
        if (expenses.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Emerald100,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = Emerald700,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Δεν βρέθηκαν έξοδα",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Πατήστε το κουμπί του μικροφώνου για φωνητική καταχώρηση ή το «Χειροκίνητο» για να προσθέσετε το πρώτο σας έξοδο.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // Grouped expenses list
        groupedExpenses.forEach { (dateHeader, dayExpenses) ->
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dateHeader,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = String.format(Locale.US, "%.2f €", dayExpenses.sumOf { it.amount }),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
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
                        .padding(horizontal = 16.dp, vertical = 5.dp)
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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Category Icon Squircle
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(catColor.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = catIcon,
                                contentDescription = expense.categoryName,
                                tint = catColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Title & split badge info
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = expense.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(3.dp))

                            // Split badge capsule
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Group,
                                        contentDescription = null,
                                        tint = Emerald700,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (expense.splitType == "ALL_FAMILY") {
                                            "Κοινό Οικογένειας"
                                        } else {
                                            "${expense.memberNamesDisplay} • ${String.format(Locale.US, "%.2f€/άτομο", expense.splitAmountPerPerson)}"
                                        },
                                        fontSize = 11.sp,
                                        color = Emerald700,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // Original Voice prompt if present
                            if (expense.originalVoicePrompt.isNotBlank()) {
                                Spacer(modifier = Modifier.height(3.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "«${expense.originalVoicePrompt}»",
                                        fontSize = 10.sp,
                                        fontStyle = FontStyle.Italic,
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

    // Delete Confirmation Dialog
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
}
