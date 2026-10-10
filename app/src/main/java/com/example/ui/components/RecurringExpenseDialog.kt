package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.entity.ExpenseCategory
import com.example.data.entity.RecurringExpense
import com.example.ui.theme.Emerald700
import com.example.util.Localization

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringExpenseDialog(
    recurringToEdit: RecurringExpense? = null,
    categories: List<ExpenseCategory>,
    appLanguage: String = "EL",
    onDismiss: () -> Unit,
    onSave: (
        id: Long,
        title: String,
        amount: Double,
        categoryId: Long,
        categoryName: String,
        dayOfMonth: Int,
        note: String
    ) -> Unit
) {
    val lang = Localization.Language.fromCode(appLanguage)

    var title by remember { mutableStateOf(recurringToEdit?.title ?: "") }
    var amountText by remember { mutableStateOf(recurringToEdit?.let { String.format(java.util.Locale.US, "%.2f", it.amount) } ?: "") }
    var dayOfMonth by remember { mutableIntStateOf(recurringToEdit?.dayOfMonth ?: 1) }
    var selectedCategoryId by remember { mutableLongStateOf(recurringToEdit?.categoryId ?: categories.firstOrNull()?.id ?: 1L) }
    var selectedCategoryName by remember { mutableStateOf(recurringToEdit?.categoryName ?: categories.firstOrNull()?.name ?: "Σπίτι") }
    var note by remember {
        mutableStateOf(
            recurringToEdit?.note ?: (if (lang == Localization.Language.ENGLISH) "Standing order" else "Πάγια εντολή")
        )
    }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Emerald700,
        focusedLabelColor = Emerald700,
        cursorColor = Emerald700,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (recurringToEdit == null) Localization.newStandingOrder(lang) else Localization.editStandingOrder(lang),
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (lang == Localization.Language.ENGLISH) "Monthly recurring expenses" else "Μηνιαίες προγραμματισμένες πληρωμές",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = Localization.close(lang))
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Field 1: Description
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(Localization.recurringDescLabel(lang)) },
                    placeholder = { Text(Localization.recurringDescPlaceholder(lang)) },
                    leadingIcon = { Icon(Icons.Default.EventRepeat, contentDescription = null, tint = Emerald700) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Field 2 & 3: Clean Amount & Day Row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text(if (lang == Localization.Language.ENGLISH) "Amount (€)" else "Ποσό (€)") },
                        placeholder = { Text("0,00") },
                        trailingIcon = { Text("€", fontWeight = FontWeight.Bold, color = Emerald700, modifier = Modifier.padding(end = 12.dp)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors,
                        modifier = Modifier.weight(1.2f)
                    )

                    OutlinedTextField(
                        value = dayOfMonth.toString(),
                        onValueChange = { dayOfMonth = it.toIntOrNull()?.coerceIn(1, 31) ?: 1 },
                        label = { Text(if (lang == Localization.Language.ENGLISH) "Day (1-31)" else "Ημέρα (1-31)") },
                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Emerald700, modifier = Modifier.size(17.dp)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Field 4: Category Dropdown
                val selectedCat = categories.firstOrNull { it.id == selectedCategoryId }
                val catIcon = CategoryIconHelper.getIcon(selectedCat?.iconKey ?: "cart")

                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = Localization.translateCategory(selectedCategoryName, lang),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(Localization.categoryLabel(lang)) },
                        leadingIcon = { Icon(catIcon, contentDescription = null, tint = Emerald700, modifier = Modifier.size(20.dp)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors,
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(CategoryIconHelper.getIcon(cat.iconKey), contentDescription = null, tint = Emerald700, modifier = Modifier.size(18.dp))
                                },
                                text = { Text(Localization.translateCategory(cat.name, lang)) },
                                onClick = {
                                    selectedCategoryId = cat.id
                                    selectedCategoryName = cat.name
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Field 5: Note
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(Localization.noteLabel(lang)) },
                    placeholder = { Text(Localization.notePlaceholder(lang)) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(22.dp))

                // Save button
                Button(
                    onClick = {
                        val amount = amountText.replace(',', '.').toDoubleOrNull() ?: 0.0
                        if (amount > 0.0 && title.isNotBlank()) {
                            onSave(recurringToEdit?.id ?: 0L, title, amount, selectedCategoryId, selectedCategoryName, dayOfMonth, note)
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald700, contentColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    enabled = title.isNotBlank() && amountText.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = Localization.save(lang),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
