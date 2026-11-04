package com.example.ui.screens

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.entity.Expense
import com.example.data.entity.FamilyMember
import com.example.data.model.Household
import com.example.ui.theme.Amber700
import com.example.ui.theme.Emerald700
import com.example.util.Localization
import com.example.util.SettleUpCalculator
import com.example.util.SettleUpResult
import java.util.Locale

@Composable
fun MembersScreen(
    members: List<FamilyMember>,
    currentHousehold: Household?,
    isPremium: Boolean,
    appLanguage: String,
    canManage: Boolean = true,
    currentUserEmail: String = "",
    deletedMembers: List<FamilyMember> = emptyList(),
    expenses: List<Expense> = emptyList(),
    onAddMember: () -> Unit,
    onEditMember: (FamilyMember) -> Unit,
    onDeleteMember: (FamilyMember) -> Unit,
    onRestoreMember: (FamilyMember) -> Unit = {},
    onPermanentDeleteMember: (FamilyMember) -> Unit = {},
    onRemoveInvitedEmail: ((String) -> Unit)? = null,
    onUpdateMemberColor: (FamilyMember, Long) -> Unit,
    onMoveMemberUp: (FamilyMember) -> Unit,
    onMoveMemberDown: (FamilyMember) -> Unit,
    onShowPremiumPaywall: (String) -> Unit,
    onShareInviteCode: () -> Unit = {},
    onShowMessage: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val lang = Localization.Language.fromCode(appLanguage)
    val isGreek = lang != Localization.Language.ENGLISH
    val clipboardManager = LocalClipboardManager.current

    val settleUpResult = remember(expenses, members) {
        SettleUpCalculator.calculate(expenses, members)
    }
    var isSettleUpExpanded by remember { mutableStateOf(false) }

    var memberToDelete by remember { mutableStateOf<FamilyMember?>(null) }
    var showDeletedMembersDialog by remember { mutableStateOf(false) }
    var memberToPermanentlyDelete by remember { mutableStateOf<FamilyMember?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                tonalElevation = 1.dp
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
                            text = if (isGreek) "Μέλη Εξοδολογίου" else "Household Members",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isGreek) "Προφίλ για τον επιμερισμό εξόδων & τα στατιστικά" else "Profiles for splitting expenses and analytics",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (canManage) {
                        Button(
                            onClick = {
                                if (!isPremium && members.size >= 4) {
                                    onShowPremiumPaywall("members_limit")
                                } else {
                                    onAddMember()
                                }
                            },
                            modifier = Modifier
                                .height(38.dp)
                                .testTag("add_member_button"),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Emerald700,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isGreek) "Προσθήκη" else "Add",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            if (members.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFD1FAE5),
                            modifier = Modifier.size(68.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Group,
                                    contentDescription = null,
                                    tint = Emerald700,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = if (isGreek) "Δεν υπάρχουν μέλη" else "No members added",
                            fontSize = 16.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = if (isGreek)
                                "Προσθέστε μέλη για να παρακολουθείτε ποιος πληρώνει τι."
                            else
                                "Add members to track who pays what.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = onAddMember,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald700),
                            modifier = Modifier.height(42.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isGreek) "Προσθήκη Μέλους" else "Add Member",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(2.dp))
                    }

                    // Settle-Up Calculator Card ("Ποιος Χρωστάει σε Ποιον")
                    if (members.size >= 2) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settle_up_calculator_card"),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { isSettleUpExpanded = !isSettleUpExpanded },
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                modifier = Modifier.size(38.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.SwapHoriz,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = if (isGreek) "Εκκαθάριση Εξόδων" else "Expense Settle-Up",
                                                        fontSize = 14.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = Emerald700.copy(alpha = 0.12f)
                                                    ) {
                                                        Text(
                                                            text = if (settleUpResult.isBalanced) (if (isGreek) "ΙΣΟΣΚΕΛΙΣΜΕΝΟ" else "BALANCED") else "${settleUpResult.transfers.size} ${if (isGreek) "μεταφορές" else "transfers"}",
                                                            fontSize = 9.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (settleUpResult.isBalanced) Emerald700 else Amber700,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = if (isGreek) "Ποιος χρωστάει σε ποιον για τα καταχωρημένα έξοδα" else "Who owes whom for logged expenses",
                                                    fontSize = 11.5.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Icon(
                                            imageVector = if (isSettleUpExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Toggle",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Primary summary line (always visible)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    if (settleUpResult.transfers.isEmpty()) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Emerald700.copy(alpha = 0.08f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = if (isGreek) "✅ Όλα τα μερίδια είναι τακτοποιημένα!" else "✅ All member shares are balanced!",
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Emerald700,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                            )
                                        }
                                    } else {
                                        // Show top recommended settlement
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            settleUpResult.transfers.take(if (isSettleUpExpanded) settleUpResult.transfers.size else 1).forEach { t ->
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = Amber700.copy(alpha = 0.08f),
                                                    border = BorderStroke(1.dp, Amber700.copy(alpha = 0.25f)),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "👉 ${t.fromMember.name} ➔ ${t.toMember.name}",
                                                            fontSize = 12.5.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Text(
                                                            text = String.format(Locale.GERMANY, "%.2f €", t.amount),
                                                            fontSize = 13.5.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = Amber700
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Detailed Breakdown when expanded
                                    if (isSettleUpExpanded) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = if (isGreek) "Ανάλυση ανά Μέλος:" else "Per-Member Breakdown:",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))

                                        settleUpResult.memberSummaries.forEach { s ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 3.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = Color(s.member.avatarColorHex),
                                                        modifier = Modifier.size(10.dp)
                                                    ) {}
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = s.member.name,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }

                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = "Πλήρωσε: ${String.format(Locale.GERMANY, "%.0f€", s.totalPaid)} | Μερίδιο: ${String.format(Locale.GERMANY, "%.0f€", s.totalShare)}",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    val isPositive = s.netBalance > 0.01
                                                    val isNegative = s.netBalance < -0.01
                                                    Text(
                                                        text = when {
                                                            isPositive -> "+${String.format(Locale.GERMANY, "%.2f €", s.netBalance)}"
                                                            isNegative -> "-${String.format(Locale.GERMANY, "%.2f €", kotlin.math.abs(s.netBalance))}"
                                                            else -> "0,00 €"
                                                        },
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = when {
                                                            isPositive -> Emerald700
                                                            isNegative -> MaterialTheme.colorScheme.error
                                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        OutlinedButton(
                                            onClick = {
                                                val householdTitle = currentHousehold?.name ?: (if (isGreek) "Εξοδολόγιο" else "Household")
                                                val text = SettleUpCalculator.formatSummaryText(settleUpResult, householdTitle, isGreek)
                                                clipboardManager.setText(AnnotatedString(text))
                                                onShowMessage(if (isGreek) "Η εκκαθάριση αντιγράφηκε στο πρόχειρο!" else "Settlement copied to clipboard!")
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(36.dp)
                                                .testTag("copy_settle_up_button"),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isGreek) "Αντιγραφή Εκκαθάρισης για Κοινοποίηση" else "Copy Settlement Summary",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    itemsIndexed(members, key = { index, m -> if (m.id > 0) "member_${m.id}_${m.name}" else "mem_idx_${index}_${m.name}" }) { index, member ->
                        val color = Color(member.avatarColorHex)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem()
                                .testTag("member_card_${member.id}"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left clickable content (Avatar + Details) opens Edit Dialog
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable(enabled = canManage) { onEditMember(member) }
                                        .padding(vertical = 4.dp, horizontal = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = color,
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = member.name.take(1).uppercase(Locale.getDefault()),
                                                color = Color.White,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = member.name,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = color.copy(alpha = 0.14f)
                                            ) {
                                                Text(
                                                    text = Localization.translateRole(member.role, lang),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = color,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            if (member.isDefault) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Emerald700.copy(alpha = 0.14f)
                                                ) {
                                                    Text(
                                                        text = if (isGreek) "★ Προεπιλογή" else "★ Default",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Emerald700,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                if (canManage) {
                                    Spacer(modifier = Modifier.width(4.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (members.size > 1) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (index > 0) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                                        else Color.Transparent
                                                    )
                                                    .clickable(enabled = index > 0) { onMoveMemberUp(member) }
                                                    .testTag("move_up_mem_${member.id}"),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ArrowUpward,
                                                    contentDescription = "Move Up",
                                                    modifier = Modifier.size(14.dp),
                                                    tint = if (index > 0) MaterialTheme.colorScheme.onSurfaceVariant else Color.Transparent
                                                )
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (index < members.size - 1) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                                        else Color.Transparent
                                                    )
                                                    .clickable(enabled = index < members.size - 1) { onMoveMemberDown(member) }
                                                    .testTag("move_down_mem_${member.id}"),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ArrowDownward,
                                                    contentDescription = "Move Down",
                                                    modifier = Modifier.size(14.dp),
                                                    tint = if (index < members.size - 1) MaterialTheme.colorScheme.onSurfaceVariant else Color.Transparent
                                                )
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(Emerald700.copy(alpha = 0.12f))
                                                .clickable { onEditMember(member) }
                                                .testTag("edit_mem_${member.id}"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit",
                                                modifier = Modifier.size(14.dp),
                                                tint = Emerald700
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFEF4444).copy(alpha = 0.12f))
                                                .clickable { memberToDelete = member }
                                                .testTag("delete_mem_${member.id}"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                modifier = Modifier.size(14.dp),
                                                tint = Color(0xFFEF4444)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Recycle Bin / Soft-deleted members access button at the bottom
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { showDeletedMembersDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("deleted_members_button"),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isGreek) "Διαγραμμένα Μέλη (${deletedMembers.size})" else "Deleted Members (${deletedMembers.size})",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
    }

    // =========================================================================
    // DIALOGS
    // =========================================================================

    // 1. Soft-Delete Member Confirmation Dialog
    if (memberToDelete != null) {
        val m = memberToDelete!!
        AlertDialog(
            onDismissRequest = { memberToDelete = null },
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = if (isGreek) "Διαγραφή Μέλους;" else "Delete Member?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (isGreek)
                            "Είστε βέβαιοι ότι θέλετε να διαγράψετε το μέλος «${m.name}»;"
                        else
                            "Are you sure you want to delete member \"${m.name}\"?",
                        fontSize = 14.sp
                    )
                    Text(
                        text = if (isGreek)
                            "Το μέλος θα μεταφερθεί στα «Διαγραμμένα Μέλη», απ' όπου μπορείτε να το επαναφέρετε ανά πάσα στιγμή. Τα ήδη καταχωρημένα έξοδα και τα στατιστικά διατηρούνται 100% ανέπαφα."
                        else
                            "The member will be moved to \"Deleted Members\", from where you can restore it anytime. Existing expenses and statistics remain intact.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteMember(m)
                        memberToDelete = null
                    }
                ) {
                    Text(
                        text = if (isGreek) "Διαγραφή" else "Delete",
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToDelete = null }) {
                    Text(if (isGreek) "Ακύρωση" else "Cancel")
                }
            }
        )
    }

    // 2. Recycle Bin Dialog ("Διαγραμμένα Μέλη")
    if (showDeletedMembersDialog) {
        Dialog(onDismissRequest = { showDeletedMembersDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .testTag("deleted_members_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = if (isGreek) "Διαγραμμένα Μέλη" else "Deleted Members",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isGreek) "Επαναφορά ή οριστική κατάργηση" else "Restore or permanently delete",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (deletedMembers.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isGreek) "Δεν υπάρχουν διεγραμμένα μέλη." else "No deleted members.",
                                fontSize = 13.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((deletedMembers.size * 65).coerceAtMost(300).dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(deletedMembers, key = { _, m -> "del_mem_${m.id}_${m.name}" }) { _, member ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = Color(member.avatarColorHex),
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = member.name.take(1).uppercase(Locale.getDefault()),
                                                        color = Color.White,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                            Column {
                                                Text(
                                                    text = member.name,
                                                    fontSize = 13.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = Localization.translateRole(member.role, lang),
                                                    fontSize = 10.5.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            // Restore button
                                            Button(
                                                onClick = {
                                                    onRestoreMember(member)
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Emerald700),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Restore,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = if (isGreek) "Επαναφορά" else "Restore",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            // Permanent delete icon
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFEF4444).copy(alpha = 0.12f))
                                                    .clickable { memberToPermanentlyDelete = member }
                                                    .testTag("perm_del_${member.id}"),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Permanent Delete",
                                                    modifier = Modifier.size(14.dp),
                                                    tint = Color(0xFFEF4444)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ) {
                        Text(
                            text = if (isGreek)
                                "ℹ️ Η διαγραφή μέλους δεν αλλοιώνει τα παλαιότερα στατιστικά και καταχωρημένα έξοδα."
                            else
                                "ℹ️ Deleting a member preserves all historical statistics and recorded expenses.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    TextButton(
                        onClick = { showDeletedMembersDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            text = if (isGreek) "Κλείσιμο" else "Close",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // 3. Permanent Member Deletion Confirmation
    if (memberToPermanentlyDelete != null) {
        val target = memberToPermanentlyDelete!!
        AlertDialog(
            onDismissRequest = { memberToPermanentlyDelete = null },
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = if (isGreek) "Οριστική Διαγραφή;" else "Permanently Delete?",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEF4444)
                )
            },
            text = {
                Text(
                    text = if (isGreek)
                        "Είστε βέβαιοι ότι θέλετε να καταργήσετε οριστικά το μέλος «${target.name}»; Τα ιστορικά έξοδα και τα στατιστικά δεν θα αλλοιωθούν."
                    else
                        "Are you sure you want to permanently remove member \"${target.name}\"? Historical expenses and stats will remain intact.",
                    fontSize = 13.5.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onPermanentDeleteMember(target)
                        memberToPermanentlyDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (isGreek) "Οριστική Διαγραφή" else "Permanently Delete",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToPermanentlyDelete = null }) {
                    Text(if (isGreek) "Ακύρωση" else "Cancel")
                }
            }
        )
    }
}
