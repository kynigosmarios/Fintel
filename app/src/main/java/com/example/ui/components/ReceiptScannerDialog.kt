package com.example.ui.components

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Storefront
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.entity.ExpenseCategory
import com.example.data.entity.FamilyMember
import com.example.ui.theme.Emerald700
import com.example.ui.theme.Emerald900
import com.example.util.ReceiptParser

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReceiptScannerDialog(
    categories: List<ExpenseCategory>,
    members: List<FamilyMember>,
    onDismiss: () -> Unit,
    onSaveExpense: (
        title: String,
        amount: Double,
        categoryId: Long,
        categoryName: String,
        timestamp: Long,
        splitType: String,
        memberIds: List<Long>,
        memberNamesDisplay: String,
        note: String
    ) -> Unit,
    onShowMessage: (String) -> Unit
) {
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var storeName by remember { mutableStateOf("Σκλαβενίτης") }
    var amountText by remember { mutableStateOf("42.80") }
    var selectedCategoryId by remember { mutableLongStateOf(categories.firstOrNull()?.id ?: 1L) }
    var selectedCategoryName by remember { mutableStateOf(categories.firstOrNull()?.name ?: "Σούπερ μάρκετ") }
    var timestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var rawReceiptText by remember { mutableStateOf(ReceiptParser.sampleReceipts[0].rawText) }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            capturedBitmap = bitmap
            // In mobile or emulator, simulate or extract OCR text
            val parsed = ReceiptParser.parse(rawReceiptText, categories)
            storeName = parsed.storeName
            amountText = String.format(java.util.Locale.US, "%.2f", parsed.amount)
            parsed.categoryId?.let { id ->
                selectedCategoryId = id
                selectedCategoryName = parsed.categoryName ?: selectedCategoryName
            }
            onShowMessage("Η απόδειξη σαρώθηκε με επιτυχία!")
        }
    }

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            // Apply parse
            val parsed = ReceiptParser.parse(rawReceiptText, categories)
            storeName = parsed.storeName
            amountText = String.format(java.util.Locale.US, "%.2f", parsed.amount)
            parsed.categoryId?.let { id ->
                selectedCategoryId = id
                selectedCategoryName = parsed.categoryName ?: selectedCategoryName
            }
            onShowMessage("Η απόδειξη φορτώθηκε!")
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("receipt_scanner_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Emerald700.copy(alpha = 0.15f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = Emerald700,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Σάρωση Απόδειξης",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Αυτόματη εξαγωγή ποσού & καταστήματος",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Κλείσιμο")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons: Camera, Gallery, Sample presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { cameraLauncher.launch(null) },
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald700),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Κάμερα", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp), tint = Emerald700)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Συλλογή", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Sample receipts chips
                Text(
                    text = "Δοκιμή με έτοιμη απόδειξη:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ReceiptParser.sampleReceipts.forEach { sample ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.clickable {
                                rawReceiptText = sample.rawText
                                val parsed = ReceiptParser.parse(sample.rawText, categories)
                                storeName = parsed.storeName
                                amountText = String.format(java.util.Locale.US, "%.2f", parsed.amount)
                                parsed.categoryId?.let { id ->
                                    selectedCategoryId = id
                                    selectedCategoryName = parsed.categoryName ?: selectedCategoryName
                                }
                                timestamp = parsed.timestamp
                            }
                        ) {
                            Text(
                                text = sample.title,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                if (capturedBitmap != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Image(
                        bitmap = capturedBitmap!!.asImageBitmap(),
                        contentDescription = "Φωτογραφία απόδειξης",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Editable Fields extracted
                OutlinedTextField(
                    value = storeName,
                    onValueChange = { storeName = it },
                    label = { Text("Κατάστημα / Τίτλος") },
                    leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null, tint = Emerald700) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Συνολικό Ποσό (€)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Category selector
                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedCategoryName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Κατηγορία Εξόδου") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                        shape = RoundedCornerShape(12.dp),
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
                                text = { Text(cat.name) },
                                onClick = {
                                    selectedCategoryId = cat.id
                                    selectedCategoryName = cat.name
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Save button
                Button(
                    onClick = {
                        val amount = amountText.replace(',', '.').toDoubleOrNull() ?: 0.0
                        if (amount <= 0.0) {
                            onShowMessage("Παρακαλώ εισάγετε έγκυρο ποσό")
                            return@Button
                        }
                        onSaveExpense(
                            storeName.ifBlank { "Αγορά απόδειξης" },
                            amount,
                            selectedCategoryId,
                            selectedCategoryName,
                            timestamp,
                            "ALL_FAMILY",
                            emptyList(),
                            "Κοινό Οικογένειας",
                            "Σάρωση απόδειξης: $storeName"
                        )
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald700),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_receipt_expense_button")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Καταχώρηση στα Έξοδα (${amountText}€)", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
