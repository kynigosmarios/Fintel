package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Household
import com.example.data.model.UserProfile
import com.example.ui.theme.Amber700
import com.example.ui.theme.Emerald700
import com.example.util.Localization
import com.google.firebase.auth.FirebaseUser

@Composable
fun BurgerMenuDrawer(
    isOpen: Boolean,
    currentUser: FirebaseUser?,
    userProfile: UserProfile?,
    currentHousehold: Household?,
    currentTab: Int,
    appLanguage: String = "EL",
    onLanguageChange: (String) -> Unit = {},
    onSelectTab: (Int) -> Unit,
    onOpenProfile: () -> Unit,
    onOpenExodologio: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenGuide: (() -> Unit)? = null,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit
) {
    val lang = Localization.Language.fromCode(appLanguage)
    var showLegalDialog by remember { mutableStateOf(false) }
    var selectedLegalTab by remember { mutableStateOf("PRIVACY") }

    AnimatedVisibility(
        visible = isOpen,
        enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(150)),
        exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(120))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag("burger_menu_drawer_scrim")
        ) {
            // Semi-transparent background overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { onDismiss() }
            )

            // Slide-in drawer from right
            AnimatedVisibility(
                visible = isOpen,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(min = 300.dp, max = 360.dp)
                    .fillMaxWidth(0.85f)
                    .clickable(enabled = false) {}, // absorb clicks
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
                tonalElevation = 8.dp,
                shadowElevation = 16.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 18.dp)
                ) {
                    // Header with title and close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = Localization.menuTitle(lang),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier
                                .size(34.dp)
                                .clickable { onDismiss() }
                                .testTag("burger_menu_close_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = Localization.close(lang),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // User Mini Summary Header Card
                    if (currentUser != null || userProfile != null) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDismiss()
                                    onOpenProfile()
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                UserAvatar(
                                    photoUrl = userProfile?.photoUrl?.ifBlank { null } ?: currentUser?.photoUrl?.toString(),
                                    displayName = userProfile?.displayName?.ifBlank { null } ?: currentUser?.displayName,
                                    modifier = Modifier.size(42.dp),
                                    fontSize = 17.sp
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = userProfile?.displayName?.ifBlank { null } ?: currentUser?.displayName?.ifBlank { null } ?: Localization.fintelUser(lang),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = userProfile?.email?.ifBlank { null } ?: currentUser?.email ?: (if (lang == Localization.Language.ENGLISH) "Connected" else "Συνδεδεμένος"),
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (userProfile?.isPremium == true) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Amber700.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "PRO",
                                            color = Amber700,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    } else {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Emerald700.copy(alpha = 0.10f),
                            border = BorderStroke(1.dp, Emerald700.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDismiss()
                                    onOpenSettings()
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Emerald700,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (lang == Localization.Language.ENGLISH) "Sign In / Cloud Sync" else "Σύνδεση / Cloud Sync",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Emerald700
                                    )
                                    Text(
                                        text = if (lang == Localization.Language.ENGLISH) "Connect Google Account" else "Συνδέστε λογαριασμό Google",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Section: Quick Language Selector
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    tint = Emerald700,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = Localization.language(lang),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Greek toggle button
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (appLanguage == "EL") Emerald700 else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, if (appLanguage == "EL") Emerald700 else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .testTag("language_button_el")
                                        .clickable { onLanguageChange("EL") }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("🇬🇷", fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Ελληνικά",
                                            fontSize = 12.sp,
                                            fontWeight = if (appLanguage == "EL") FontWeight.Bold else FontWeight.Medium,
                                            color = if (appLanguage == "EL") Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                // English toggle button
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (appLanguage == "EN") Emerald700 else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, if (appLanguage == "EN") Emerald700 else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .testTag("language_button_en")
                                        .clickable { onLanguageChange("EN") }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("🇬🇧", fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "English",
                                            fontSize = 12.sp,
                                            fontWeight = if (appLanguage == "EN") FontWeight.Bold else FontWeight.Medium,
                                            color = if (appLanguage == "EN") Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Section: Primary Navigation Items
                    Text(
                        text = if (lang == Localization.Language.ENGLISH) "MANAGEMENT & SETTINGS" else "ΔΙΑΧΕΙΡΙΣΗ & ΡΥΘΜΙΣΕΙΣ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                    )

                    // 1. Ρυθμίσεις Εφαρμογής
                    BurgerNavigationItem(
                        title = Localization.appSettings(lang),
                        subtitle = Localization.appSettingsSubtitle(lang),
                        icon = Icons.Default.Settings,
                        iconTint = Color(0xFF475569),
                        iconBg = Color(0xFF475569).copy(alpha = 0.12f),
                        testTag = "burger_item_settings",
                        onClick = {
                            onDismiss()
                            onOpenSettings()
                        }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // 2. Διαχείριση Εξοδολογίων
                    BurgerNavigationItem(
                        title = Localization.householdManager(lang),
                        subtitle = currentHousehold?.name ?: (if (lang == Localization.Language.ENGLISH) "Active Household" else "Ενεργό εξοδολόγιο"),
                        icon = Icons.Default.MenuBook,
                        iconTint = Color(0xFF0284C7),
                        iconBg = Color(0xFF0284C7).copy(alpha = 0.12f),
                        testTag = "burger_item_exodologio",
                        onClick = {
                            onDismiss()
                            onOpenExodologio()
                        }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // 3. Το Προφίλ μου
                    BurgerNavigationItem(
                        title = Localization.myProfile(lang),
                        subtitle = if (userProfile?.isPremium == true)
                            (if (lang == Localization.Language.ENGLISH) "Fintel Pro Subscriber" else "Fintel Pro συνδρομητής")
                        else
                            (if (lang == Localization.Language.ENGLISH) "Account details & Pro" else "Στοιχεία λογαριασμού & Pro"),
                        icon = Icons.Default.Person,
                        iconTint = Emerald700,
                        iconBg = Emerald700.copy(alpha = 0.12f),
                        testTag = "burger_item_profile",
                        onClick = {
                            onDismiss()
                            onOpenProfile()
                        }
                    )

                    if (onOpenGuide != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        BurgerNavigationItem(
                            title = if (lang == Localization.Language.ENGLISH) "Instructions" else "Οδηγίες",
                            subtitle = if (lang == Localization.Language.ENGLISH) "6-step tutorial for new users" else "Οδηγός 6 βημάτων για νέα μέλη",
                            icon = Icons.Default.Info,
                            iconTint = Color(0xFFD97706),
                            iconBg = Color(0xFFD97706).copy(alpha = 0.12f),
                            testTag = "burger_item_guide",
                            onClick = {
                                onDismiss()
                                onOpenGuide()
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Sign Out Option
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onDismiss()
                                onSignOut()
                            }
                            .testTag("burger_menu_signout_button"),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.22f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = Localization.signOut(lang),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = Localization.signOutAccount(lang),
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Discreet Legal Links (Terms & Conditions, Privacy Policy)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (lang == Localization.Language.ENGLISH) "Terms of Service" else "Όροι Χρήσης",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Emerald700,
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier
                                .clickable {
                                    selectedLegalTab = "TERMS"
                                    showLegalDialog = true
                                }
                                .testTag("terms_of_service_link")
                        )
                        Text(
                            text = "  •  ",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = if (lang == Localization.Language.ENGLISH) "Privacy Policy" else "Πολιτική Απορρήτου",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Emerald700,
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier
                                .clickable {
                                    selectedLegalTab = "PRIVACY"
                                    showLegalDialog = true
                                }
                                .testTag("privacy_policy_link")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

    if (showLegalDialog) {
        LegalDocumentsDialog(
            initialTab = selectedLegalTab,
            appLanguage = appLanguage,
            onDismiss = { showLegalDialog = false }
        )
    }
}

@Composable
private fun BurgerNavigationItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = iconBg,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                modifier = Modifier.size(13.dp)
            )
        }
    }
}
