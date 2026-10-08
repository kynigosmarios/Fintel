package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Euro
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.entity.ExpenseCategory
import com.example.ui.theme.Emerald700
import com.example.util.Localization
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryDialog(
    categoryToEdit: ExpenseCategory?,
    appLanguage: String = "EL",
    onDismiss: () -> Unit,
    onSave: (
        id: Long,
        name: String,
        iconKey: String,
        monthlyBudget: Double,
        colorHex: Long,
        voiceKeywords: String
    ) -> Unit
) {
    val lang = Localization.Language.fromCode(appLanguage)

    var name by remember { mutableStateOf(categoryToEdit?.name ?: "") }
    var budgetText by remember {
        mutableStateOf(
            categoryToEdit?.let { String.format(Locale.US, "%.0f", it.monthlyBudget) } ?: "100"
        )
    }
    var selectedIconKey by remember { mutableStateOf(categoryToEdit?.iconKey ?: "cart") }
    var voiceKeywords by remember { mutableStateOf(categoryToEdit?.voiceKeywords ?: "") }
    var errorText by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .widthIn(max = 500.dp)
                .fillMaxHeight(0.88f)
                .padding(vertical = 12.dp)
                .testTag("category_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // 1. Fixed Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (categoryToEdit == null) Localization.addCategory(lang) else Localization.editCategoryTitle(lang),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Emerald700
                    )
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .size(32.dp)
                            .clickable { onDismiss() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Close, contentDescription = Localization.close(lang), modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Middle Scrollable Form Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Name
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            errorText = null
                        },
                        label = { Text(Localization.categoryNameLabel(lang)) },
                        placeholder = { Text(Localization.categoryNamePlaceholder(lang)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("category_name_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Monthly Budget
                    OutlinedTextField(
                        value = budgetText,
                        onValueChange = {
                            budgetText = it.replace(',', '.')
                            errorText = null
                        },
                        label = { Text(Localization.monthlyBudgetLimitLabel(lang)) },
                        leadingIcon = { Icon(Icons.Default.Euro, contentDescription = null, tint = Emerald700) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Icon selection
                    Text(
                        text = Localization.categoryIconLabel(lang),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CategoryIconHelper.availableIcons.forEach { (key, label) ->
                            val isSelected = selectedIconKey == key
                            val displayLabel = Localization.translateCategory(label, lang)
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedIconKey = key },
                                label = { Text(displayLabel, fontSize = 11.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = CategoryIconHelper.getIcon(key),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isSelected) Emerald700 else Color.Gray
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Voice Keywords
                    OutlinedTextField(
                        value = voiceKeywords,
                        onValueChange = { voiceKeywords = it },
                        label = { Text(Localization.categoryKeywordsLabel(lang)) },
                        placeholder = { Text(Localization.categoryKeywordsPlaceholder(lang)) },
                        supportingText = {
                            Text(
                                if (lang == Localization.Language.ENGLISH)
                                    "Keywords that will be automatically detected via speech"
                                else
                                    "Λέξεις που θα αναγνωρίζονται αυτόματα στη φωνή",
                                fontSize = 11.sp
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 3
                    )

                    if (errorText != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = errorText ?: "", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    modifier = Modifier.padding(vertical = 10.dp)
                )

                // 3. Fixed Bottom Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 8.dp)
                    ) {
                        Text(
                            text = Localization.cancel(lang),
                            maxLines = 1,
                            softWrap = false,
                            fontSize = 14.sp
                        )
                    }

                    Button(
                        onClick = {
                            val parsedBudget = budgetText.toDoubleOrNull()
                            if (name.isBlank()) {
                                errorText = if (lang == Localization.Language.ENGLISH) "Please enter category name" else "Συμπληρώστε όνομα κατηγορίας"
                                return@Button
                            }
                            if (parsedBudget == null || parsedBudget < 0) {
                                errorText = if (lang == Localization.Language.ENGLISH) "Please enter a valid budget" else "Συμπληρώστε έγκυρο budget"
                                return@Button
                            }

                            onSave(
                                categoryToEdit?.id ?: 0L,
                                name.trim(),
                                selectedIconKey,
                                parsedBudget,
                                categoryToEdit?.colorHex ?: 0xFF059669,
                                voiceKeywords.trim()
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald700, contentColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("save_category_button"),
                        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = Localization.save(lang),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
