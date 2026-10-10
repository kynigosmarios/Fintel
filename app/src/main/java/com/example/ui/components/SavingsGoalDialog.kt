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
import androidx.compose.material.icons.filled.Euro
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.data.entity.SavingsGoal
import com.example.ui.theme.Emerald700
import com.example.util.Localization

@Composable
fun SavingsGoalDialog(
    goalToEdit: SavingsGoal? = null,
    appLanguage: String = "EL",
    onDismiss: () -> Unit,
    onSave: (id: Long, title: String, targetAmount: Double, currentAmount: Double, deadline: String, note: String) -> Unit
) {
    val lang = Localization.Language.fromCode(appLanguage)

    var title by remember { mutableStateOf(goalToEdit?.title ?: "") }
    var targetText by remember { mutableStateOf(goalToEdit?.let { String.format(java.util.Locale.US, "%.0f", it.targetAmount) } ?: "") }
    var currentText by remember { mutableStateOf(goalToEdit?.let { String.format(java.util.Locale.US, "%.0f", it.currentAmount) } ?: "0") }
    var deadline by remember {
        mutableStateOf(
            goalToEdit?.deadlineDate ?: (if (lang == Localization.Language.ENGLISH) "Summer 2027" else "Καλοκαίρι 2027")
        )
    }
    var note by remember { mutableStateOf(goalToEdit?.note ?: "") }

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
                .testTag("savings_goal_dialog")
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
                            text = if (goalToEdit == null) Localization.newGoal(lang) else Localization.editGoalTitle(lang),
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (lang == Localization.Language.ENGLISH) "Set your target & deadline" else "Ορίστε το ποσό και την προθεσμία σας",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = Localization.close(lang))
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Field 1: Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(Localization.goalTitleLabel(lang)) },
                    placeholder = { Text(Localization.goalTitlePlaceholder(lang)) },
                    leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null, tint = Emerald700) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Field 2 & 3: Clean Amounts Row with concise non-wrapping labels
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = targetText,
                        onValueChange = { targetText = it },
                        label = { Text(if (lang == Localization.Language.ENGLISH) "Goal (€)" else "Στόχος (€)") },
                        placeholder = { Text("0") },
                        trailingIcon = { Text("€", fontWeight = FontWeight.Bold, color = Emerald700, modifier = Modifier.padding(end = 12.dp)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = currentText,
                        onValueChange = { currentText = it },
                        label = { Text(if (lang == Localization.Language.ENGLISH) "Saved (€)" else "Μαζεμένα (€)") },
                        placeholder = { Text("0") },
                        trailingIcon = { Text("€", fontWeight = FontWeight.Bold, color = Emerald700, modifier = Modifier.padding(end = 12.dp)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Field 4: Deadline
                OutlinedTextField(
                    value = deadline,
                    onValueChange = { deadline = it },
                    label = { Text(Localization.dateLabel(lang)) },
                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Emerald700, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

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
                        val target = targetText.replace(',', '.').toDoubleOrNull() ?: 100.0
                        val current = currentText.replace(',', '.').toDoubleOrNull() ?: 0.0
                        onSave(goalToEdit?.id ?: 0L, title.ifBlank { Localization.newGoal(lang) }, target, current, deadline, note)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald700, contentColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    enabled = title.isNotBlank() && targetText.isNotBlank(),
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
