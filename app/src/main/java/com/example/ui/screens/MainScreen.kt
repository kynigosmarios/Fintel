package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBar
import com.example.ui.components.OikoNomosLogo
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.outlined.Savings
import com.example.ui.components.AppSettingsDialog
import com.example.ui.components.BurgerMenuDrawer
import com.example.ui.components.CategoryDialog
import com.example.ui.components.ExodologioDetailsDialog
import com.example.ui.components.ExodologioManagerDialog
import com.example.ui.components.HouseholdActiveBar
import com.example.ui.components.ExportReportDialog
import com.example.ui.components.ManualExpenseDialog
import com.example.ui.components.ManualIncomeDialog
import com.example.ui.components.MemberDialog
import com.example.ui.components.OnboardingCarouselDialog
import com.example.ui.components.PrivacyConsentDialog
import com.example.ui.components.UserAvatar
import com.example.ui.components.PremiumPaywallDialog
import com.example.ui.components.QuickActionChooserDialog
import com.example.ui.components.RecurringExpenseDialog
import com.example.ui.components.SavingsGoalDialog
import com.example.ui.components.TileSettingsDialog
import com.example.ui.components.UserProfileDialog
import com.example.ui.components.InviteMemberDialog
import com.example.ui.components.VoiceConfirmationDialog
import com.example.ui.components.VoiceInputDialog
import com.example.ui.components.VoiceListeningOverlay
import com.example.ui.components.VoiceMicButton
import com.example.ui.theme.Amber700
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Emerald700
import com.example.ui.theme.Emerald800
import com.example.ui.theme.Emerald900
import com.example.ui.theme.Emerald950
import com.example.ui.theme.ModernFontFamily
import com.example.ui.viewmodel.OikoNomosViewModel
import com.example.util.SpeechRecognitionHelper
import kotlinx.coroutines.launch

enum class AppTab(val title: String) {
    EXPENSES("Έξοδα"),
    ANALYTICS("Αναλυτικά"),
    MEMBERS("Μέλη"),
    CATEGORIES("Κατηγορίες")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: OikoNomosViewModel,
    onLaunchBilling: (android.app.Activity) -> Boolean = { false }
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentTab by remember { mutableIntStateOf(0) }

    // State collected from ViewModel
    val expenses by viewModel.filteredExpenses.collectAsStateWithLifecycle()
    val allExpenses by viewModel.allExpenses.collectAsStateWithLifecycle()
    val members by viewModel.allMembers.collectAsStateWithLifecycle()
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()
    val selectedMemberFilter by viewModel.selectedMemberFilter.collectAsStateWithLifecycle()
    val selectedCategoryFilter by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    val pendingParsedExpense by viewModel.pendingParsedExpense.collectAsStateWithLifecycle()
    val countdownState by viewModel.countdownState.collectAsStateWithLifecycle()
    val isListening by viewModel.isListening.collectAsStateWithLifecycle()

    val showManualExpenseDialog by viewModel.showManualExpenseDialog.collectAsStateWithLifecycle()
    val editingExpense by viewModel.editingExpense.collectAsStateWithLifecycle()

    val allIncomes by viewModel.allIncomes.collectAsStateWithLifecycle()
    val filteredIncomes by viewModel.filteredIncomes.collectAsStateWithLifecycle()
    val showManualIncomeDialog by viewModel.showManualIncomeDialog.collectAsStateWithLifecycle()
    val editingIncome by viewModel.editingIncome.collectAsStateWithLifecycle()

    val showMemberDialog by viewModel.showMemberDialog.collectAsStateWithLifecycle()
    val editingMember by viewModel.editingMember.collectAsStateWithLifecycle()

    val showCategoryDialog by viewModel.showCategoryDialog.collectAsStateWithLifecycle()
    val editingCategory by viewModel.editingCategory.collectAsStateWithLifecycle()

    val showSettingsDialog by viewModel.showSettingsDialog.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()
    val lang = com.example.util.Localization.Language.fromCode(appLanguage)

    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val currentHousehold by viewModel.currentHousehold.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val isInitialExpensesLoading by viewModel.isInitialExpensesLoading.collectAsStateWithLifecycle()

    val userHouseholds by viewModel.userHouseholds.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val showExodologioManagerDialog by viewModel.showExodologioManagerDialog.collectAsStateWithLifecycle()
    val showBurgerMenu by viewModel.showBurgerMenu.collectAsStateWithLifecycle()
    val showUserProfileDialog by viewModel.showUserProfileDialog.collectAsStateWithLifecycle()
    val showExodologioDetailsDialog by viewModel.showExodologioDetailsDialog.collectAsStateWithLifecycle()
    val premiumPaywallReason by viewModel.premiumPaywallReason.collectAsStateWithLifecycle()

    val showQuickActionChooser by viewModel.showQuickActionChooser.collectAsStateWithLifecycle()
    val showVoiceInputDialog by viewModel.showVoiceInputDialog.collectAsStateWithLifecycle()
    val showExportReportDialog by viewModel.showExportReportDialog.collectAsStateWithLifecycle()
    val showSavingsGoalDialog by viewModel.showSavingsGoalDialog.collectAsStateWithLifecycle()
    val editingSavingsGoal by viewModel.editingSavingsGoal.collectAsStateWithLifecycle()
    val showRecurringExpenseDialog by viewModel.showRecurringExpenseDialog.collectAsStateWithLifecycle()
    val editingRecurringExpense by viewModel.editingRecurringExpense.collectAsStateWithLifecycle()
    val recurringExpenses by viewModel.allRecurringExpenses.collectAsStateWithLifecycle()
    val savingsGoals by viewModel.allSavingsGoals.collectAsStateWithLifecycle()
    val launchWithChooser by viewModel.launchWithChooser.collectAsStateWithLifecycle()
    val showOnboardingCarousel by viewModel.showOnboardingCarousel.collectAsStateWithLifecycle()
    val dailyReminderEnabled by viewModel.dailyReminderEnabled.collectAsStateWithLifecycle()
    val dailyReminderHour by viewModel.dailyReminderHour.collectAsStateWithLifecycle()
    val dailyReminderMinute by viewModel.dailyReminderMinute.collectAsStateWithLifecycle()
    val showPrivacyConsentDialog by viewModel.showPrivacyConsentDialog.collectAsStateWithLifecycle()
    var showInviteDialog by remember { mutableStateOf(false) }

    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    // Handle snackbar
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    // Speech Recognition Helper
    val speechHelper = remember {
        SpeechRecognitionHelper(
            context = context,
            onListeningStateChanged = { listening ->
                viewModel.setListening(listening)
            },
            onResult = { text ->
                viewModel.processVoiceInput(text)
                viewModel.dismissVoiceInputDialog()
            },
            onError = { errMsg ->
                viewModel.showSnackbar(errMsg)
            },
            onRmsChanged = { rms ->
                viewModel.setMicSoundLevel(rms)
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            speechHelper.cancelListening()
        }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                speechHelper.startListening()
            } catch (t: Throwable) {
                viewModel.showSnackbar("Σφάλμα εκκίνησης μικροφώνου")
            }
        } else {
            viewModel.showSnackbar("Απαιτείται άδεια μικροφώνου για φωνητική καταχώρηση. Μπορείτε να χρησιμοποιήσετε τα έτοιμα παραδείγματα!")
        }
    }

    fun handleMicToggle() {
        try {
            if (isListening) {
                speechHelper.stopListening()
            } else {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED

                if (hasPermission) {
                    speechHelper.startListening()
                } else {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        } catch (t: Throwable) {
            viewModel.showSnackbar("Σφάλμα μικροφώνου: ${t.localizedMessage ?: "μη διαθέσιμο"}")
        }
    }

    val isDarkTheme = when {
        themeMode.equals("DARK", ignoreCase = true) -> true
        themeMode.equals("LIGHT", ignoreCase = true) -> false
        else -> isSystemInDarkTheme()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                currentTab = 0
                                viewModel.setSearchQuery("")
                            }
                            .padding(vertical = 4.dp, horizontal = 2.dp)
                            .testTag("app_brand_home_button")
                    ) {
                        OikoNomosLogo(
                            isDarkTheme = isDarkTheme,
                            size = 36.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Fintel",
                            fontFamily = ModernFontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 25.sp,
                            letterSpacing = (-0.5).sp,
                            color = if (isDarkTheme) Color(0xFF34D399) else Color(0xFF0F5132)
                        )
                    }
                },
                actions = {
                    Surface(
                        shape = CircleShape,
                        color = Emerald700.copy(alpha = 0.12f),
                        modifier = Modifier
                            .size(34.dp)
                            .clickable { viewModel.openUserProfileDialog() }
                            .testTag("topbar_user_avatar")
                    ) {
                        UserAvatar(
                            photoUrl = userProfile?.photoUrl?.ifBlank { null } ?: currentUser?.photoUrl?.toString(),
                            displayName = userProfile?.displayName?.ifBlank { null } ?: currentUser?.displayName,
                            modifier = Modifier.fillMaxSize(),
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { viewModel.openBurgerMenu() },
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .testTag("burger_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = com.example.util.Localization.menuTitle(lang),
                            tint = if (isDarkTheme) Color.White else Emerald950,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDarkTheme) Color(0xFF0F172A) else Color.White,
                    titleContentColor = if (isDarkTheme) Color.White else Emerald950,
                    actionIconContentColor = if (isDarkTheme) Color.White else Emerald950
                ),
                modifier = Modifier.border(
                    width = 1.dp,
                    color = if (isDarkTheme) Color(0xFF334155).copy(alpha = 0.5f) else Color(0xFFE2E8F0).copy(alpha = 0.6f)
                )
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                // Active Voice Listening notification banner
                VoiceListeningOverlay(
                    isListening = isListening,
                    onCloseListening = {
                        speechHelper.stopListening()
                    }
                )

                // Navigation Bar
                NavigationBar(
                    containerColor = if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFF1F5F2),
                    tonalElevation = 4.dp,
                    modifier = Modifier.border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                    )
                ) {
                    NavigationBarItem(
                        selected = currentTab == 0,
                        onClick = { currentTab = 0 },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == 0) Icons.Default.Receipt else Icons.Outlined.Receipt,
                                contentDescription = com.example.util.Localization.tabExpenses(lang)
                            )
                        },
                        label = { Text(com.example.util.Localization.tabExpenses(lang), fontSize = 10.5.sp, fontWeight = if (currentTab == 0) FontWeight.Bold else FontWeight.Medium) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF064E3B),
                            selectedTextColor = Color(0xFF064E3B),
                            indicatorColor = Color(0xFFD1FAE5),
                            unselectedIconColor = Color(0xFF475569),
                            unselectedTextColor = Color(0xFF64748B)
                        ),
                        modifier = Modifier.testTag("tab_expenses")
                    )

                    NavigationBarItem(
                        selected = currentTab == 1,
                        onClick = { currentTab = 1 },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == 1) Icons.Default.PieChart else Icons.Outlined.PieChart,
                                contentDescription = com.example.util.Localization.tabAnalytics(lang)
                            )
                        },
                        label = { Text(com.example.util.Localization.tabAnalytics(lang), fontSize = 10.5.sp, fontWeight = if (currentTab == 1) FontWeight.Bold else FontWeight.Medium) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF064E3B),
                            selectedTextColor = Color(0xFF064E3B),
                            indicatorColor = Color(0xFFD1FAE5),
                            unselectedIconColor = Color(0xFF475569),
                            unselectedTextColor = Color(0xFF64748B)
                        ),
                        modifier = Modifier.testTag("tab_analytics")
                    )

                    NavigationBarItem(
                        selected = currentTab == 2,
                        onClick = { currentTab = 2 },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == 2) Icons.Default.Group else Icons.Outlined.Group,
                                contentDescription = com.example.util.Localization.tabMembers(lang)
                            )
                        },
                        label = { Text(com.example.util.Localization.tabMembers(lang), fontSize = 10.5.sp, fontWeight = if (currentTab == 2) FontWeight.Bold else FontWeight.Medium) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF064E3B),
                            selectedTextColor = Color(0xFF064E3B),
                            indicatorColor = Color(0xFFD1FAE5),
                            unselectedIconColor = Color(0xFF475569),
                            unselectedTextColor = Color(0xFF64748B)
                        ),
                        modifier = Modifier.testTag("tab_members")
                    )

                    NavigationBarItem(
                        selected = currentTab == 3,
                        onClick = { currentTab = 3 },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == 3) Icons.Default.Category else Icons.Outlined.Category,
                                contentDescription = com.example.util.Localization.tabCategories(lang)
                            )
                        },
                        label = { Text(com.example.util.Localization.tabCategories(lang), fontSize = 10.5.sp, fontWeight = if (currentTab == 3) FontWeight.Bold else FontWeight.Medium) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF064E3B),
                            selectedTextColor = Color(0xFF064E3B),
                            indicatorColor = Color(0xFFD1FAE5),
                            unselectedIconColor = Color(0xFF475569),
                            unselectedTextColor = Color(0xFF64748B)
                        ),
                        modifier = Modifier.testTag("tab_categories")
                    )
                }
            }
        },
        floatingActionButton = {
            VoiceMicButton(
                isListening = isListening,
                onMicClick = { viewModel.openVoiceInputDialog() }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Active Household Bar (Visible across all tabs so user can always see and switch active exodologio)
            HouseholdActiveBar(
                currentHousehold = currentHousehold,
                households = userHouseholds,
                appLanguage = appLanguage,
                isDarkTheme = isDarkTheme,
                onSelectHousehold = { viewModel.switchExodologio(it) },
                onManageHouseholdsClick = { viewModel.openExodologioDetailsDialog() },
                onCreateNewHouseholdClick = { viewModel.openExodologioManager() },
                onSwitchClick = { viewModel.openExodologioManager() }
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Crossfade(
                targetState = currentTab,
                animationSpec = tween(durationMillis = 150),
                label = "tab_navigation_crossfade"
            ) { tabIndex ->
                when (tabIndex) {
                    0 -> ExpensesScreen(
                        expenses = expenses,
                        incomes = filteredIncomes,
                        categories = categories,
                        members = members,
                        selectedMemberFilter = selectedMemberFilter,
                        selectedCategoryFilter = selectedCategoryFilter,
                        searchQuery = searchQuery,
                        appLanguage = appLanguage,
                        onMemberFilterChange = { viewModel.setMemberFilter(it) },
                        onCategoryFilterChange = { viewModel.setCategoryFilter(it) },
                        onSearchQueryChange = { viewModel.setSearchQuery(it) },
                        onAddManualExpense = { viewModel.openManualExpenseDialog() },
                        onAddManualIncome = { viewModel.openManualIncomeDialog() },
                        onVoiceExpense = { viewModel.openVoiceInputDialog() },
                        onEditExpense = { viewModel.openManualExpenseDialog(it) },
                        onDeleteExpense = { viewModel.deleteExpense(it) },
                        onEditIncome = { viewModel.openManualIncomeDialog(it) },
                        onDeleteIncome = { viewModel.deleteIncome(it) },
                        onExportExcel = { viewModel.openExportReportDialog() },
                        onExportPdf = { viewModel.openExportReportDialog() },
                        onShareReport = { viewModel.openExportReportDialog() },
                        onAddMember = { viewModel.openMemberDialog() },
                        onAddCategory = { viewModel.openCategoryDialog() },
                        onSeedStandardCategories = { viewModel.seedStandardCategories() },
                        isInitialLoading = isInitialExpensesLoading
                    )
                    1 -> AnalyticsScreen(
                        expenses = allExpenses,
                        incomes = allIncomes,
                        categories = categories,
                        members = members,
                        appLanguage = appLanguage,
                        onExportReport = { viewModel.openExportReportDialog() },
                        onManageCategories = { currentTab = 3 }
                    )
                    2 -> MembersScreen(
                        members = members,
                        currentHousehold = currentHousehold,
                        isPremium = userProfile?.isPremium == true,
                        appLanguage = appLanguage,
                        onAddMember = { viewModel.openMemberDialog() },
                        onEditMember = { viewModel.openMemberDialog(it) },
                        onDeleteMember = { viewModel.deleteMember(it) },
                        onUpdateMemberColor = { member, color -> viewModel.updateMemberColor(member, color) },
                        onMoveMemberUp = { viewModel.moveMemberUp(it) },
                        onMoveMemberDown = { viewModel.moveMemberDown(it) },
                        onShowPremiumPaywall = { viewModel.showPremiumPaywall(it) },
                        onShareInviteCode = { showInviteDialog = true }
                    )
                    3 -> CategoriesScreen(
                        categories = categories,
                        appLanguage = appLanguage,
                        onAddCategory = { viewModel.openCategoryDialog() },
                        onEditCategory = { viewModel.openCategoryDialog(it) },
                        onDeleteCategory = { viewModel.deleteCategory(it) },
                        onMoveCategoryUp = { viewModel.moveCategoryUp(it) },
                        onMoveCategoryDown = { viewModel.moveCategoryDown(it) },
                        onSeedStandardCategories = { viewModel.seedStandardCategories() }
                    )
                }
            }

            // Burger Menu Drawer (Requirement 4: Contained between top header and bottom footer)
            BurgerMenuDrawer(
                isOpen = showBurgerMenu,
                currentUser = currentUser,
                userProfile = userProfile,
                currentHousehold = currentHousehold,
                currentTab = currentTab,
                appLanguage = appLanguage,
                onLanguageChange = { viewModel.setAppLanguage(it) },
                onSelectTab = { currentTab = it },
                onOpenProfile = { viewModel.openUserProfileDialog() },
                onOpenExodologio = { viewModel.openExodologioDetailsDialog() },
                onOpenSettings = { viewModel.openSettingsDialog() },
                onOpenGuide = { viewModel.openOnboardingGuide() },
                onSignOut = { viewModel.signOutUser() },
                onDismiss = { viewModel.dismissBurgerMenu() }
            )
        }
    }
}

    // Voice Confirmation Dialog with editable Amount, Category, Members, Date (Requirement 2)
    VoiceConfirmationDialog(
        pendingExpense = pendingParsedExpense,
        categories = categories,
        members = members,
        onConfirm = { title, amount, categoryId, categoryName, timestamp, splitType, memberIds, memberNamesDisplay, prompt, payerId, payerName, payerMemberIdsCsv, payerAmountsCsv ->
            viewModel.confirmVoiceExpense(
                title = title,
                amount = amount,
                categoryId = categoryId,
                categoryName = categoryName,
                timestamp = timestamp,
                splitType = splitType,
                memberIds = memberIds,
                memberNamesDisplay = memberNamesDisplay,
                originalPrompt = prompt,
                payerMemberId = payerId,
                payerMemberName = payerName,
                payerMemberIdsCsv = payerMemberIdsCsv,
                payerAmountsCsv = payerAmountsCsv
            )
        },
        onCancel = { viewModel.cancelPendingExpense() }
    )

    // Manual Expense Add/Edit Dialog
    if (showManualExpenseDialog) {
        ManualExpenseDialog(
            expenseToEdit = editingExpense,
            categories = categories,
            members = members,
            onDismiss = { viewModel.dismissManualExpenseDialog() },
            onSave = { id, title, amount, categoryId, categoryName, timestamp, splitType, memberIds, memberNamesDisplay, note, payerId, payerName, payerMemberIdsCsv, payerAmountsCsv ->
                viewModel.saveExpense(
                    id = id,
                    title = title,
                    amount = amount,
                    categoryId = categoryId,
                    categoryName = categoryName,
                    timestamp = timestamp,
                    splitType = splitType,
                    memberIds = memberIds,
                    memberNamesDisplay = memberNamesDisplay,
                    note = note,
                    payerMemberId = payerId,
                    payerMemberName = payerName,
                    payerMemberIdsCsv = payerMemberIdsCsv,
                    payerAmountsCsv = payerAmountsCsv
                )
            }
        )
    }

    // Manual Income Dialog
    if (showManualIncomeDialog) {
        ManualIncomeDialog(
            incomeToEdit = editingIncome,
            members = members,
            onDismiss = { viewModel.dismissManualIncomeDialog() },
            onSave = { id, title, amount, memberId, memberName, category, timestamp, note ->
                viewModel.saveIncome(
                    id = id,
                    title = title,
                    amount = amount,
                    memberId = memberId,
                    memberName = memberName,
                    category = category,
                    timestamp = timestamp,
                    note = note
                )
            }
        )
    }

    // Member Dialog
    if (showMemberDialog) {
        MemberDialog(
            memberToEdit = editingMember,
            onDismiss = { viewModel.dismissMemberDialog() },
            onSave = { id, name, role, avatarColorHex, voiceKeywords ->
                viewModel.saveMember(id, name, role, avatarColorHex, voiceKeywords)
            }
        )
    }

    // Category Dialog
    if (showCategoryDialog) {
        CategoryDialog(
            categoryToEdit = editingCategory,
            appLanguage = appLanguage,
            onDismiss = { viewModel.dismissCategoryDialog() },
            onSave = { id, name, iconKey, monthlyBudget, colorHex, voiceKeywords, fontColorHex ->
                viewModel.saveCategory(id, name, iconKey, monthlyBudget, colorHex, voiceKeywords, fontColorHex)
            }
        )
    }

    // Quick Action Chooser (Triggered on App Launch, Widget, or Manual Action)
    if (showQuickActionChooser) {
        QuickActionChooserDialog(
            appLanguage = appLanguage,
            onVoiceClick = { viewModel.openVoiceInputDialog() },
            onManualClick = { viewModel.openManualExpenseDialog() },
            onIncomeClick = { viewModel.openManualIncomeDialog() },
            onOpenAppClick = { viewModel.dismissQuickActionChooser() },
            onDismiss = { viewModel.dismissQuickActionChooser() }
        )
    }

    // User Profile Dialog
    if (showUserProfileDialog) {
        UserProfileDialog(
            currentUser = currentUser,
            userProfile = userProfile,
            expenseCount = expenses.size,
            totalSpent = expenses.sumOf { it.amount },
            currencySymbol = currencySymbol,
            appLanguage = appLanguage,
            onSaveDisplayName = { viewModel.updateUserDisplayName(it) },
            onSaveFullProfile = { first, last, birth, phone, occ, city ->
                viewModel.updateUserFullProfile(first, last, birth, phone, occ, city)
            },
            onUpdateProfilePhoto = { viewModel.updateUserProfilePhoto(it) },
            onShowPremiumPaywall = { viewModel.showPremiumPaywall("profile_upgrade") },
            onSignOut = { viewModel.signOutUser() },
            onDeleteAccount = { viewModel.deleteCurrentUserAccount() },
            onDismiss = { viewModel.dismissUserProfileDialog() }
        )
    }

    // Invite Member Dialog (Email & Share Link)
    val activeHouseholdForInvite = currentHousehold ?: userHouseholds.firstOrNull() ?: com.example.data.model.Household(
        id = "primary_household",
        name = if (appLanguage == "EN") "My Household" else "Το Εξοδολόγιό μου",
        inviteCode = "OIKO-${System.currentTimeMillis().toString().takeLast(4)}"
    )
    if (showInviteDialog) {
        InviteMemberDialog(
            household = activeHouseholdForInvite,
            isPro = userProfile?.isPremium == true || activeHouseholdForInvite.isPremium,
            appLanguage = appLanguage,
            onInviteEmail = { email -> viewModel.inviteMemberByEmail(email, context) },
            onMessage = { msg -> viewModel.showSnackbar(msg) },
            onShowPremiumPaywall = { viewModel.showPremiumPaywall("invite_member") },
            onDismiss = { showInviteDialog = false }
        )
    }

    // Exodologio Details Dialog (Rename, Members, Switch, Clear)
    if (showExodologioDetailsDialog) {
        ExodologioDetailsDialog(
            currentHousehold = currentHousehold,
            allHouseholds = userHouseholds,
            isPremium = userProfile?.isPremium == true,
            appLanguage = appLanguage,
            onRenameHousehold = { viewModel.renameCurrentHousehold(it) },
            onSelectHousehold = { viewModel.switchExodologio(it) },
            onCreateNewHousehold = { viewModel.createNewExodologio(it) },
            onManageMembers = { currentTab = 3 },
            onShareInviteCode = { showInviteDialog = true },
            onShowPremiumPaywall = { viewModel.showPremiumPaywall(it) },
            onClearExpenses = { viewModel.clearCurrentExodologioExpenses() },
            onDismiss = { viewModel.dismissExodologioDetailsDialog() }
        )
    }

    // Exodologio Manager Dialog
    if (showExodologioManagerDialog) {
        ExodologioManagerDialog(
            currentHousehold = currentHousehold,
            allHouseholds = userHouseholds,
            isPremium = userProfile?.isPremium == true,
            appLanguage = appLanguage,
            onSelectHousehold = { viewModel.switchExodologio(it) },
            onCreateNewHousehold = { viewModel.createNewExodologio(it) },
            onShowPremiumPaywall = { viewModel.showPremiumPaywall(it) },
            onInviteMember = {
                viewModel.dismissExodologioManager()
                showInviteDialog = true
            },
            onDismiss = { viewModel.dismissExodologioManager() }
        )
    }

    // Premium Paywall Dialog
    if (premiumPaywallReason != null) {
        PremiumPaywallDialog(
            reason = premiumPaywallReason!!,
            appLanguage = appLanguage,
            onUnlockPro = {
                val act = context as? android.app.Activity
                val launched = if (act != null) onLaunchBilling(act) else false
                if (!launched) {
                    try {
                        val playIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.aistudio.oikonomos.app")).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(playIntent)
                    } catch (e: Exception) {
                        Log.w("MainScreen", "Could not open Google Play Store: ${e.message}")
                    }
                    viewModel.unlockPro()
                }
            },
            onDismiss = { viewModel.dismissPremiumPaywall() }
        )
    }

    // Voice Input Dialog (Robust Speech Recording, Manual Fallback, and Preset Chips)
    if (showVoiceInputDialog) {
        VoiceInputDialog(
            isListening = isListening,
            onStartListening = { handleMicToggle() },
            onStopListening = { speechHelper.stopListening() },
            onSubmitVoiceText = { text ->
                viewModel.processVoiceInput(text)
                viewModel.dismissVoiceInputDialog()
            },
            onDismiss = {
                speechHelper.cancelListening()
                viewModel.dismissVoiceInputDialog()
            },
            onShowMessage = { viewModel.showSnackbar(it) }
        )
    }

    // Feature 4: Excel / CSV Export Report Dialog
    if (showExportReportDialog) {
        ExportReportDialog(
            expenses = allExpenses,
            onDismiss = { viewModel.dismissExportReportDialog() },
            onShowMessage = { viewModel.showSnackbar(it) }
        )
    }

    // Feature 5: Savings Goal Add/Edit Dialog
    if (showSavingsGoalDialog) {
        SavingsGoalDialog(
            goalToEdit = editingSavingsGoal,
            onDismiss = { viewModel.dismissSavingsGoalDialog() },
            onSave = { id, title, target, current, deadline, note ->
                viewModel.saveSavingsGoal(id, title, target, current, deadline, note)
            }
        )
    }

    // Feature 2: Recurring Expense Add/Edit Dialog
    if (showRecurringExpenseDialog) {
        RecurringExpenseDialog(
            recurringToEdit = editingRecurringExpense,
            categories = categories,
            members = members,
            appLanguage = appLanguage,
            onDismiss = { viewModel.dismissRecurringExpenseDialog() },
            onSave = { id, title, amount, categoryId, categoryName, dayOfMonth, note, payerId, payerName ->
                viewModel.saveRecurringExpense(id, title, amount, categoryId, categoryName, dayOfMonth, note, payerId, payerName)
            }
        )
    }

    // App Settings Dialog (Profile, Desktop Tile / Widget, Theme, Family Cloud Sync, Evening Reminders)
    if (showSettingsDialog) {
        AppSettingsDialog(
            currentUser = currentUser,
            household = currentHousehold,
            isSyncing = isSyncing,
            themeMode = themeMode,
            appLanguage = appLanguage,
            launchWithChooser = launchWithChooser,
            userProfile = userProfile,
            reminderEnabled = dailyReminderEnabled,
            reminderHour = dailyReminderHour,
            reminderMinute = dailyReminderMinute,
            onToggleReminder = { viewModel.toggleDailyReminder(it) },
            onSetReminderTime = { h, m -> viewModel.setDailyReminderTime(h, m) },
            onTestReminder = { viewModel.triggerTestReminderNotification() },
            onThemeChange = { viewModel.setThemeMode(it) },
            onLanguageChange = { viewModel.setAppLanguage(it) },
            onToggleLaunchWithChooser = { viewModel.setLaunchWithChooser(it) },
            onCreateHousehold = { viewModel.createHousehold(it) },
            onJoinHousehold = { viewModel.joinHousehold(it) },
            onSignOut = { viewModel.signOutUser() },
            onClearAllDataAndSignOut = { viewModel.factoryResetAndSignOut() },
            onMessage = { viewModel.showSnackbar(it) },
            onDismiss = { viewModel.dismissSettingsDialog() }
        )
    }

    // Onboarding Pop-up Carousel for New Users (Next-Next-Next with Auto-advance every 5 seconds)
    if (showOnboardingCarousel) {
        OnboardingCarouselDialog(
            appLanguage = appLanguage,
            onDismiss = { viewModel.dismissOnboardingCarousel() },
            onAddMember = {
                viewModel.dismissOnboardingCarousel()
                viewModel.openMemberDialog()
            },
            onAddCategory = {
                viewModel.dismissOnboardingCarousel()
                viewModel.openCategoryDialog()
            },
            onSeedCategories = {
                viewModel.dismissOnboardingCarousel()
                viewModel.seedStandardCategories()
            }
        )
    }

    // GDPR / Privacy Consent Modal on App Start
    if (showPrivacyConsentDialog) {
        PrivacyConsentDialog(
            appLanguage = appLanguage,
            onAccept = { viewModel.acceptPrivacyConsent() }
        )
    }
}
