package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
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
import com.example.ui.components.AppSettingsDialog
import com.example.ui.components.CategoryDialog
import com.example.ui.components.ExportReportDialog
import com.example.ui.components.ManualExpenseDialog
import com.example.ui.components.MemberDialog
import com.example.ui.components.QuickActionChooserDialog
import com.example.ui.components.ReceiptScannerDialog
import com.example.ui.components.RecurringExpenseDialog
import com.example.ui.components.SavingsGoalDialog
import com.example.ui.components.TileSettingsDialog
import com.example.ui.components.VoiceConfirmationDialog
import com.example.ui.components.VoiceInputDialog
import com.example.ui.components.VoiceListeningOverlay
import com.example.ui.components.VoiceMicButton
import com.example.ui.screens.SavingsAndRecurringScreen
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.outlined.Savings
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Emerald700
import com.example.ui.theme.Emerald800
import com.example.ui.theme.Emerald900
import com.example.ui.theme.Emerald950
import com.example.ui.viewmodel.OikoNomosViewModel
import com.example.util.SpeechRecognitionHelper
import kotlinx.coroutines.launch

enum class AppTab(val title: String) {
    EXPENSES("Έξοδα"),
    ANALYTICS("Στατιστικά"),
    GOALS("Στόχοι"),
    MEMBERS("Μέλη"),
    CATEGORIES("Κατηγορίες")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: OikoNomosViewModel) {
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

    val showMemberDialog by viewModel.showMemberDialog.collectAsStateWithLifecycle()
    val editingMember by viewModel.editingMember.collectAsStateWithLifecycle()

    val showCategoryDialog by viewModel.showCategoryDialog.collectAsStateWithLifecycle()
    val editingCategory by viewModel.editingCategory.collectAsStateWithLifecycle()

    val showSettingsDialog by viewModel.showSettingsDialog.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()

    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val currentHousehold by viewModel.currentHousehold.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()

    val showQuickActionChooser by viewModel.showQuickActionChooser.collectAsStateWithLifecycle()
    val showVoiceInputDialog by viewModel.showVoiceInputDialog.collectAsStateWithLifecycle()
    val showReceiptScannerDialog by viewModel.showReceiptScannerDialog.collectAsStateWithLifecycle()
    val showExportReportDialog by viewModel.showExportReportDialog.collectAsStateWithLifecycle()
    val showSavingsGoalDialog by viewModel.showSavingsGoalDialog.collectAsStateWithLifecycle()
    val editingSavingsGoal by viewModel.editingSavingsGoal.collectAsStateWithLifecycle()
    val showRecurringExpenseDialog by viewModel.showRecurringExpenseDialog.collectAsStateWithLifecycle()
    val editingRecurringExpense by viewModel.editingRecurringExpense.collectAsStateWithLifecycle()
    val recurringExpenses by viewModel.allRecurringExpenses.collectAsStateWithLifecycle()
    val savingsGoals by viewModel.allSavingsGoals.collectAsStateWithLifecycle()
    val launchWithChooser by viewModel.launchWithChooser.collectAsStateWithLifecycle()

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
            speechHelper.stopListening()
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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_oikonomos_logo),
                            contentDescription = "OikoNomos Logo",
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "OikoNomos",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            letterSpacing = (-0.3).sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Emerald400)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.openReceiptScanner() },
                        modifier = Modifier.testTag("scan_receipt_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Σάρωση απόδειξης",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.openExportReportDialog() },
                        modifier = Modifier.testTag("export_report_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.TableChart,
                            contentDescription = "Εξαγωγή σε Excel",
                            tint = Color.White
                        )
                    }
                    IconButton(
                        onClick = { viewModel.openSettingsDialog() },
                        modifier = Modifier.testTag("open_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Ρυθμίσεις",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Emerald950
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
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    modifier = Modifier.border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )
                ) {
                    NavigationBarItem(
                        selected = currentTab == 0,
                        onClick = { currentTab = 0 },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == 0) Icons.Default.Receipt else Icons.Outlined.Receipt,
                                contentDescription = "Έξοδα"
                            )
                        },
                        label = { Text("Έξοδα", fontSize = 10.sp, fontWeight = if (currentTab == 0) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Emerald700,
                            selectedTextColor = Emerald700,
                            indicatorColor = Emerald100
                        ),
                        modifier = Modifier.testTag("tab_expenses")
                    )

                    NavigationBarItem(
                        selected = currentTab == 1,
                        onClick = { currentTab = 1 },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == 1) Icons.Default.PieChart else Icons.Outlined.PieChart,
                                contentDescription = "Στατιστικά"
                            )
                        },
                        label = { Text("Στατιστικά", fontSize = 10.sp, fontWeight = if (currentTab == 1) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Emerald700,
                            selectedTextColor = Emerald700,
                            indicatorColor = Emerald100
                        ),
                        modifier = Modifier.testTag("tab_analytics")
                    )

                    NavigationBarItem(
                        selected = currentTab == 2,
                        onClick = { currentTab = 2 },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == 2) Icons.Default.Savings else Icons.Outlined.Savings,
                                contentDescription = "Στόχοι"
                            )
                        },
                        label = { Text("Στόχοι", fontSize = 10.sp, fontWeight = if (currentTab == 2) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Emerald700,
                            selectedTextColor = Emerald700,
                            indicatorColor = Emerald100
                        ),
                        modifier = Modifier.testTag("tab_goals")
                    )

                    NavigationBarItem(
                        selected = currentTab == 3,
                        onClick = { currentTab = 3 },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == 3) Icons.Default.Group else Icons.Outlined.Group,
                                contentDescription = "Μέλη"
                            )
                        },
                        label = { Text("Μέλη", fontSize = 10.sp, fontWeight = if (currentTab == 3) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Emerald700,
                            selectedTextColor = Emerald700,
                            indicatorColor = Emerald100
                        ),
                        modifier = Modifier.testTag("tab_members")
                    )

                    NavigationBarItem(
                        selected = currentTab == 4,
                        onClick = { currentTab = 4 },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == 4) Icons.Default.Category else Icons.Outlined.Category,
                                contentDescription = "Κατηγορίες"
                            )
                        },
                        label = { Text("Κατηγορίες", fontSize = 10.sp, fontWeight = if (currentTab == 4) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Emerald700,
                            selectedTextColor = Emerald700,
                            indicatorColor = Emerald100
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                0 -> ExpensesScreen(
                    expenses = expenses,
                    categories = categories,
                    members = members,
                    selectedMemberFilter = selectedMemberFilter,
                    selectedCategoryFilter = selectedCategoryFilter,
                    searchQuery = searchQuery,
                    onMemberFilterChange = { viewModel.setMemberFilter(it) },
                    onCategoryFilterChange = { viewModel.setCategoryFilter(it) },
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    onAddManualExpense = { viewModel.openManualExpenseDialog() },
                    onEditExpense = { viewModel.openManualExpenseDialog(it) },
                    onDeleteExpense = { viewModel.deleteExpense(it) }
                )
                1 -> AnalyticsScreen(
                    expenses = allExpenses,
                    categories = categories,
                    members = members,
                    onExportReport = { viewModel.openExportReportDialog() },
                    onManageCategories = { currentTab = 4 }
                )
                2 -> SavingsAndRecurringScreen(
                    goals = savingsGoals,
                    recurringExpenses = recurringExpenses,
                    onAddGoal = { viewModel.openSavingsGoalDialog() },
                    onEditGoal = { viewModel.openSavingsGoalDialog(it) },
                    onDeleteGoal = { viewModel.deleteSavingsGoal(it) },
                    onDepositToGoal = { goal, amt -> viewModel.depositToGoal(goal, amt) },
                    onAddRecurring = { viewModel.openRecurringExpenseDialog() },
                    onEditRecurring = { viewModel.openRecurringExpenseDialog(it) },
                    onDeleteRecurring = { viewModel.deleteRecurringExpense(it) },
                    onExecuteRecurringNow = { viewModel.executeRecurringExpenseNow(it) },
                    onToggleRecurringActive = { viewModel.toggleRecurringActive(it) }
                )
                3 -> MembersScreen(
                    members = members,
                    onAddMember = { viewModel.openMemberDialog() },
                    onEditMember = { viewModel.openMemberDialog(it) },
                    onDeleteMember = { viewModel.deleteMember(it) }
                )
                4 -> CategoriesScreen(
                    categories = categories,
                    onAddCategory = { viewModel.openCategoryDialog() },
                    onEditCategory = { viewModel.openCategoryDialog(it) },
                    onDeleteCategory = { viewModel.deleteCategory(it) }
                )
            }
        }
    }

    // Voice Confirmation Dialog with editable Amount, Category, Members, Date (Requirement 2)
    VoiceConfirmationDialog(
        pendingExpense = pendingParsedExpense,
        categories = categories,
        members = members,
        onConfirm = { title, amount, categoryId, categoryName, timestamp, splitType, memberIds, memberNamesDisplay, prompt ->
            viewModel.confirmVoiceExpense(
                title = title,
                amount = amount,
                categoryId = categoryId,
                categoryName = categoryName,
                timestamp = timestamp,
                splitType = splitType,
                memberIds = memberIds,
                memberNamesDisplay = memberNamesDisplay,
                originalPrompt = prompt
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
            onSave = { id, title, amount, categoryId, categoryName, timestamp, splitType, memberIds, memberNamesDisplay, note ->
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
            onDismiss = { viewModel.dismissCategoryDialog() },
            onSave = { id, name, iconKey, monthlyBudget, colorHex, voiceKeywords ->
                viewModel.saveCategory(id, name, iconKey, monthlyBudget, colorHex, voiceKeywords)
            }
        )
    }

    // Quick Action Chooser (Triggered on App Launch, Widget, or Manual Action)
    if (showQuickActionChooser) {
        QuickActionChooserDialog(
            onVoiceClick = { viewModel.openVoiceInputDialog() },
            onManualClick = { viewModel.openManualExpenseDialog() },
            onScanReceiptClick = { viewModel.openReceiptScanner() },
            onExportReportClick = { viewModel.openExportReportDialog() },
            onOpenAppClick = { viewModel.dismissQuickActionChooser() },
            onDismiss = { viewModel.dismissQuickActionChooser() }
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
                speechHelper.stopListening()
                viewModel.dismissVoiceInputDialog()
            },
            onShowMessage = { viewModel.showSnackbar(it) }
        )
    }

    // Feature 1: Receipt Scanner Dialog
    if (showReceiptScannerDialog) {
        ReceiptScannerDialog(
            categories = categories,
            members = members,
            onDismiss = { viewModel.dismissReceiptScanner() },
            onSaveExpense = { title, amount, categoryId, categoryName, timestamp, splitType, memberIds, memberNamesDisplay, note ->
                viewModel.saveExpense(
                    id = 0L,
                    title = title,
                    amount = amount,
                    categoryId = categoryId,
                    categoryName = categoryName,
                    timestamp = timestamp,
                    splitType = splitType,
                    memberIds = memberIds,
                    memberNamesDisplay = memberNamesDisplay,
                    note = note
                )
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
            onDismiss = { viewModel.dismissRecurringExpenseDialog() },
            onSave = { id, title, amount, categoryId, categoryName, dayOfMonth, note ->
                viewModel.saveRecurringExpense(id, title, amount, categoryId, categoryName, dayOfMonth, note)
            }
        )
    }

    // App Settings Dialog (Profile, Desktop Tile / Widget, Theme, Family Cloud Sync)
    if (showSettingsDialog) {
        AppSettingsDialog(
            currentUser = currentUser,
            household = currentHousehold,
            isSyncing = isSyncing,
            themeMode = themeMode,
            launchWithChooser = launchWithChooser,
            onThemeChange = { viewModel.setThemeMode(it) },
            onToggleLaunchWithChooser = { viewModel.setLaunchWithChooser(it) },
            onCreateHousehold = { viewModel.createHousehold(it) },
            onJoinHousehold = { viewModel.joinHousehold(it) },
            onSignOut = { viewModel.signOutUser() },
            onMessage = { viewModel.showSnackbar(it) },
            onDismiss = { viewModel.dismissSettingsDialog() }
        )
    }
}
