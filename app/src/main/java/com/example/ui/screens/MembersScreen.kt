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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.FamilyMember
import com.example.data.model.Household
import com.example.ui.theme.Amber700
import com.example.ui.theme.Emerald700
import com.example.util.Localization
import java.util.Locale

@Composable
fun MembersScreen(
    members: List<FamilyMember>,
    currentHousehold: Household?,
    isPremium: Boolean,
    appLanguage: String,
    onAddMember: () -> Unit,
    onEditMember: (FamilyMember) -> Unit,
    onDeleteMember: (FamilyMember) -> Unit,
    onUpdateMemberColor: (FamilyMember, Long) -> Unit,
    onMoveMemberUp: (FamilyMember) -> Unit,
    onMoveMemberDown: (FamilyMember) -> Unit,
    onShowPremiumPaywall: (String) -> Unit,
    onShareInviteCode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = Localization.Language.fromCode(appLanguage)
    var memberToDelete by remember { mutableStateOf<FamilyMember?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Household Members Header Card (Spacious, balanced, gorgeous)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Top Row: Header Info & Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFD1FAE5),
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Text(
                                text = if (lang == Localization.Language.ENGLISH) "MEMBERS & ROLES" else "ΜΕΛΗ & ΡΟΛΟΙ",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF047857),
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Text(
                            text = Localization.activeMembersCount(members.size, lang),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Primary Add Member Button (Modern Pill)
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
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (lang == Localization.Language.ENGLISH) "Add" else "Προσθήκη",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Invite Member (* Pro) Button below active members and above the manual member list
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Amber700.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, Amber700.copy(alpha = 0.45f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clickable { onShareInviteCode() }
                .testTag("invite_member_pro_button")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Amber700.copy(alpha = 0.15f),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = null,
                                tint = Amber700,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = if (lang == Localization.Language.ENGLISH) "Invite" else "Πρόσκληση",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (lang == Localization.Language.ENGLISH)
                                "additional user to household"
                            else
                                "επιπλέον χρήστη στο εξοδολόγιο",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Amber700.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, Amber700.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Amber700,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Pro",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Black,
                            color = Amber700
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
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Group,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (lang == Localization.Language.ENGLISH) "No members added" else "Δεν υπάρχουν μέλη",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (lang == Localization.Language.ENGLISH)
                            "Add family members to track who pays what."
                        else
                            "Προσθέστε μέλη της οικογένειας για να παρακολουθείτε ποιος πληρώνει τι.",
                        fontSize = 13.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = onAddMember,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald700),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (lang == Localization.Language.ENGLISH) "Add Member" else "Προσθήκη Μέλους",
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(2.dp))
                }

                itemsIndexed(members, key = { index, m -> if (m.id > 0) "mem_${m.id}_$index" else "mem_idx_$index" }) { index, member ->
                    val color = Color(member.avatarColorHex)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem()
                            .testTag("member_card_${member.id}"),
                        shape = RoundedCornerShape(18.dp),
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
                                    .clickable { onEditMember(member) }
                                    .padding(vertical = 4.dp, horizontal = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Avatar initial with vibrant color
                                Surface(
                                    shape = CircleShape,
                                    color = color,
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = member.name.take(1).uppercase(Locale.getDefault()),
                                            color = Color.White,
                                            fontSize = 19.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Details (Name + Role + Default badge)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = member.name,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = color.copy(alpha = 0.14f)
                                        ) {
                                            Text(
                                                text = Localization.translateRole(member.role, lang),
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = color,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                            )
                                        }
                                        if (member.isDefault) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Emerald700.copy(alpha = 0.14f)
                                            ) {
                                                Text(
                                                    text = if (lang == Localization.Language.ENGLISH) "★ Default" else "★ Προεπιλογή",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Emerald700,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Actions: Reorder, Edit, Delete (isolated, distinct, responsive)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (members.size > 1) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
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
                                            modifier = Modifier.size(16.dp),
                                            tint = if (index > 0) MaterialTheme.colorScheme.onSurfaceVariant else Color.Transparent
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
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
                                            modifier = Modifier.size(16.dp),
                                            tint = if (index < members.size - 1) MaterialTheme.colorScheme.onSurfaceVariant else Color.Transparent
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Emerald700.copy(alpha = 0.12f))
                                        .clickable { onEditMember(member) }
                                        .testTag("edit_mem_${member.id}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        modifier = Modifier.size(16.dp),
                                        tint = Emerald700
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEF4444).copy(alpha = 0.12f))
                                        .clickable { memberToDelete = member }
                                        .testTag("delete_mem_${member.id}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        modifier = Modifier.size(16.dp),
                                        tint = Color(0xFFEF4444)
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    // Delete Member Confirmation Dialog
    if (memberToDelete != null) {
        val m = memberToDelete!!
        AlertDialog(
            onDismissRequest = { memberToDelete = null },
            title = {
                Text(
                    text = Localization.deleteMemberConfirmTitle(lang),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (lang == Localization.Language.ENGLISH)
                        "Are you sure you want to delete member \"${m.name}\"?"
                    else
                        "Είστε βέβαιοι ότι θέλετε να διαγράψετε το μέλος «${m.name}»;"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteMember(m)
                        memberToDelete = null
                    }
                ) {
                    Text(
                        text = Localization.delete(lang),
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToDelete = null }) {
                    Text(Localization.cancel(lang))
                }
            }
        )
    }
}
