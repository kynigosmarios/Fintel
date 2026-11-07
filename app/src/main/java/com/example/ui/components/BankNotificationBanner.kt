package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.ui.theme.Emerald700
import com.example.util.PendingBankTransaction
import java.util.Locale

@Composable
fun BankNotificationBanner(
    transactions: List<PendingBankTransaction>,
    appLanguage: String,
    onConfirmTransaction: (PendingBankTransaction) -> Unit,
    onDismissTransaction: (PendingBankTransaction) -> Unit,
    modifier: Modifier = Modifier
) {
    if (transactions.isEmpty()) return
    val isGreek = appLanguage != "EN"

    var selectedIndex by remember { mutableIntStateOf(0) }
    val currentIndex = selectedIndex.coerceIn(0, (transactions.size - 1).coerceAtLeast(0))
    val tx = transactions[currentIndex]

    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            // Requirement 4: If more than 1 notification, render realistic stacked card deck layers underneath
            // giving the user an instant, tactile impression that notifications are stacked ("το ένα κάτω από το άλλο")
            if (transactions.size >= 3) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 10.dp)
                        .height(115.dp)
                        .zIndex(1f),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, Emerald700.copy(alpha = 0.2f)),
                    shadowElevation = 1.dp
                ) {}
            }

            if (transactions.size >= 2) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .padding(top = 5.dp)
                        .height(115.dp)
                        .zIndex(2f),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                    border = BorderStroke(1.dp, Emerald700.copy(alpha = 0.35f)),
                    shadowElevation = 2.dp
                ) {}
            }

            // Top (Foreground) Active Card - displayed at identical full size ("στο ίδιο μέγεθος")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(3f)
                    .testTag("bank_notification_banner"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.2.dp, Emerald700.copy(alpha = 0.7f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Emerald700.copy(alpha = 0.15f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = Emerald700,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (isGreek) "Εντοπίστηκε Συναλλαγή" else "Bank Transaction Detected",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = tx.bankName,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${tx.merchantName} • ${String.format(Locale.GERMANY, "%.2f €", tx.amount)}",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Emerald700
                                )
                            }
                        }

                        // Right actions: Stack Total Indicator Badge (e.g. 1/3) + Navigation Arrows + Dismiss X
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (transactions.size > 1) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Emerald700.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, Emerald700.copy(alpha = 0.35f))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                            contentDescription = "Previous",
                                            tint = if (currentIndex > 0) Emerald700 else Emerald700.copy(alpha = 0.3f),
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable(enabled = currentIndex > 0) {
                                                    selectedIndex = currentIndex - 1
                                                }
                                        )
                                        Text(
                                            text = "${currentIndex + 1}/${transactions.size}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Emerald700
                                        )
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = "Next",
                                            tint = if (currentIndex < transactions.size - 1) Emerald700 else Emerald700.copy(alpha = 0.3f),
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable(enabled = currentIndex < transactions.size - 1) {
                                                    selectedIndex = currentIndex + 1
                                                }
                                        )
                                    }
                                }
                            }

                            IconButton(
                                onClick = { onDismissTransaction(tx) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onConfirmTransaction(tx) },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("confirm_bank_transaction_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald700)
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isGreek) "Καταχώρηση" else "Record",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = { onDismissTransaction(tx) },
                            modifier = Modifier.height(38.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (isGreek) "Αγνόηση" else "Ignore",
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
