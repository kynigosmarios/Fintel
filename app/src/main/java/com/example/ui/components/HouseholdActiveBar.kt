package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Household
import com.example.ui.theme.Emerald700

@Composable
fun HouseholdActiveBar(
    currentHousehold: Household?,
    households: List<Household>,
    appLanguage: String,
    isDarkTheme: Boolean,
    onSelectHousehold: (Household) -> Unit,
    onManageHouseholdsClick: () -> Unit,
    onCreateNewHouseholdClick: () -> Unit,
    onSwitchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayName = currentHousehold?.name?.ifBlank { "Το Εξοδολόγιό μου" } ?: "Το Εξοδολόγιό μου"

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(0.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onManageHouseholdsClick() }
            .testTag("household_active_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Active Household",
                    tint = Emerald700,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = displayName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (currentHousehold?.isPremium == true) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Emerald700.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "PRO",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Emerald700,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            IconButton(
                onClick = onSwitchClick,
                modifier = Modifier
                    .size(28.dp)
                    .testTag("household_switch_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = "Switch Household",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
