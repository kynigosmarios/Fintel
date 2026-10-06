package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.entity.FamilyMember
import com.example.ui.theme.Emerald700

val MemberPalette = listOf(
    0xFF0284C7, // Sky Blue
    0xFF10B981, // Emerald
    0xFFF59E0B, // Amber
    0xFFEF4444, // Coral / Red
    0xFF8B5CF6, // Purple
    0xFFD946EF, // Fuchsia
    0xFFEC4899, // Pink
    0xFF06B6D4, // Cyan
    0xFF84CC16, // Lime
    0xFF6366F1  // Indigo
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MemberDialog(
    memberToEdit: FamilyMember?,
    onDismiss: () -> Unit,
    onSave: (id: Long, name: String, role: String, avatarColorHex: Long, voiceKeywords: String) -> Unit
) {
    var name by remember { mutableStateOf(memberToEdit?.name ?: "") }
    var role by remember { mutableStateOf(memberToEdit?.role ?: "Γονέας") }
    var selectedColor by remember {
        mutableLongStateOf(memberToEdit?.avatarColorHex ?: 0xFF0284C7)
    }
    var voiceKeywords by remember {
        mutableStateOf(memberToEdit?.voiceKeywords ?: "")
    }
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
                .testTag("member_dialog"),
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
                        text = if (memberToEdit == null) "Προσθήκη Μέλους" else "Επεξεργασία Μέλους",
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
                            Icon(Icons.Default.Close, contentDescription = "Κλείσιμο", modifier = Modifier.size(16.dp))
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
                    // Name Input
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            errorText = null
                        },
                        label = { Text("Όνομα μέλους (π.χ. Μάριος)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("member_name_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Role selection
                    Text(text = "Ρόλος:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Γονέας", "Παιδί", "Φίλος/η", "Συνεργάτης", "Άλλο").forEach { r ->
                            FilterChip(
                                selected = role == r,
                                onClick = { role = r },
                                label = { Text(r, fontSize = 12.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Color picker - Clickable without showing all options initially
                    var showColorPalette by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showColorPalette = !showColorPalette }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(Color(selectedColor))
                                .border(1.5.dp, Emerald700, CircleShape)
                        )
                        Text(
                            text = "Επιλογή χρώματος",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = if (showColorPalette) "Απόκρυψη" else "Αλλαγή",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Emerald700
                        )
                    }

                    if (showColorPalette) {
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            MemberPalette.forEach { colorVal ->
                                val isSelected = selectedColor == colorVal
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(colorVal))
                                        .clickable {
                                            selectedColor = colorVal
                                            showColorPalette = false
                                        }
                                        .then(
                                            if (isSelected) Modifier.border(2.dp, Emerald700, CircleShape)
                                            else Modifier
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Voice keywords
                    OutlinedTextField(
                        value = voiceKeywords,
                        onValueChange = { voiceKeywords = it },
                        label = { Text("Φωνητικές λέξεις αναγνώρισης") },
                        placeholder = { Text("π.χ. μπαμπάς, πατέρας, μάριος") },
                        supportingText = { Text("Διαχωρίστε με κόμμα λέξεις/ψευδώνυμα", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 2
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

                // 3. Fixed Bottom Action buttons - ALWAYS accessible & visible
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
                            text = "Ακύρωση",
                            maxLines = 1,
                            softWrap = false,
                            fontSize = 14.sp
                        )
                    }

                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                errorText = "Συμπληρώστε όνομα μέλους"
                                return@Button
                            }
                            onSave(
                                memberToEdit?.id ?: 0L,
                                name.trim(),
                                role,
                                selectedColor,
                                voiceKeywords.trim()
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald700),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("save_member_button"),
                        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Αποθήκευση",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
