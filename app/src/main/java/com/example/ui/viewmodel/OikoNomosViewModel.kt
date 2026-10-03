package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.entity.Expense
import com.example.data.entity.ExpenseCategory
import com.example.data.entity.FamilyMember
import com.example.data.entity.RecurringExpense
import com.example.data.entity.SavingsGoal
import com.example.data.model.FirebaseExpense
import com.example.data.model.Household
import com.example.data.model.UserProfile
import com.example.data.repository.ExpenseRepository
import com.example.data.repository.FirestoreSyncRepository
import com.example.util.GreekVoiceParser
import com.example.util.ParsedVoiceExpense
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.auth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CountdownState(
    val remainingSeconds: Int = 3,
    val isActive: Boolean = false
)

class OikoNomosViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ExpenseRepository
    private val firestoreSyncRepo = FirestoreSyncRepository.create(application)
    private val auth: FirebaseAuth = Firebase.auth

    // Firebase Auth & Cloud Sync States
    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser = _currentUser.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile = _userProfile.asStateFlow()

    private val _currentHousehold = MutableStateFlow<Household?>(null)
    val currentHousehold = _currentHousehold.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    private var householdJob: Job? = null
    private var expensesSyncJob: Job? = null
    private var userExpensesSyncJob: Job? = null

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ExpenseRepository(database.expenseDao(), application)
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }

        // Listen for Google Auth state
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            _currentUser.value = user
            if (user != null) {
                viewModelScope.launch {
                    loadUserProfileAndHousehold(user)
                }
            } else {
                _userProfile.value = null
                _currentHousehold.value = null
                householdJob?.cancel()
                expensesSyncJob?.cancel()
                userExpensesSyncJob?.cancel()
            }
        }
    }

    private suspend fun loadUserProfileAndHousehold(user: FirebaseUser) {
        try {
            val profile = firestoreSyncRepo.getOrCreateUserProfile(
                user.uid,
                user.email ?: "",
                user.displayName ?: ""
            )
            _userProfile.value = profile
            observeUserData(user.uid)
            if (profile.householdId.isNotBlank()) {
                observeHouseholdData(profile.householdId)
            }
        } catch (e: Exception) {
            // Log/ignore
        }
    }

    private fun observeUserData(userId: String) {
        userExpensesSyncJob?.cancel()
        userExpensesSyncJob = viewModelScope.launch {
            firestoreSyncRepo.observeUserExpenses(userId).collect { remoteExpenses ->
                val currentLocal = repository.allExpenses.first()
                for (remote in remoteExpenses) {
                    val exists = currentLocal.any { it.title == remote.title && it.amount == remote.amount && it.timestamp == remote.timestamp }
                    if (!exists) {
                        repository.insertExpense(
                            Expense(
                                title = remote.title,
                                amount = remote.amount,
                                categoryId = remote.categoryId,
                                categoryName = remote.categoryName,
                                timestamp = remote.timestamp,
                                splitType = remote.splitType,
                                memberIdsCsv = "",
                                memberNamesDisplay = remote.memberNamesDisplay,
                                splitAmountPerPerson = remote.amount,
                                originalVoicePrompt = "",
                                note = remote.note
                            )
                        )
                    }
                }
            }
        }
    }

    private fun observeHouseholdData(householdId: String) {
        householdJob?.cancel()
        householdJob = viewModelScope.launch {
            firestoreSyncRepo.observeHousehold(householdId).collect { h ->
                _currentHousehold.value = h
            }
        }

        expensesSyncJob?.cancel()
        expensesSyncJob = viewModelScope.launch {
            firestoreSyncRepo.observeExpenses(householdId).collect { remoteExpenses ->
                val currentLocal = repository.allExpenses.first()
                for (remote in remoteExpenses) {
                    val exists = currentLocal.any { it.title == remote.title && it.amount == remote.amount && it.timestamp == remote.timestamp }
                    if (!exists) {
                        repository.insertExpense(
                            Expense(
                                title = remote.title,
                                amount = remote.amount,
                                categoryId = remote.categoryId,
                                categoryName = remote.categoryName,
                                timestamp = remote.timestamp,
                                splitType = remote.splitType,
                                memberIdsCsv = "",
                                memberNamesDisplay = remote.memberNamesDisplay,
                                splitAmountPerPerson = remote.amount,
                                originalVoicePrompt = "",
                                note = remote.note
                            )
                        )
                    }
                }
            }
        }
    }

    fun createHousehold(familyName: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            try {
                _isSyncing.value = true
                val household = firestoreSyncRepo.createHousehold(user.uid, familyName)
                _currentHousehold.value = household
                observeHouseholdData(household.id)
                showSnackbar("Δημιουργήθηκε: ${household.name}! Κωδικός: ${household.inviteCode}")
            } catch (e: Exception) {
                showSnackbar("Σφάλμα δημιουργίας: ${e.message}")
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun joinHousehold(code: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            try {
                _isSyncing.value = true
                val result = firestoreSyncRepo.joinHouseholdByCode(user.uid, code)
                result.onSuccess { h ->
                    _currentHousehold.value = h
                    observeHouseholdData(h.id)
                    showSnackbar("Συνδεθήκατε στο εξοδολόγιο: ${h.name}!")
                }.onFailure { err ->
                    showSnackbar(err.message ?: "Αποτυχία σύνδεσης")
                }
            } catch (e: Exception) {
                showSnackbar("Σφάλμα σύνδεσης: ${e.message}")
            } finally {
                _isSyncing.value = false
            }
        }
    }

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading = _isAuthLoading.asStateFlow()

    fun signInWithEmail(email: String, pass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (email.isBlank() || pass.isBlank()) {
            onError("Παρακαλώ συμπληρώστε email και κωδικό πρόσβασης")
            return
        }
        _isAuthLoading.value = true
        auth.signInWithEmailAndPassword(email.trim(), pass)
            .addOnSuccessListener { result ->
                _isAuthLoading.value = false
                _currentUser.value = result.user
                showSnackbar("Συνδεθήκατε επιτυχώς!")
                onSuccess()
            }
            .addOnFailureListener { exc ->
                _isAuthLoading.value = false
                val msg = when {
                    exc.message?.contains("user-not-found") == true -> "Δεν βρέθηκε λογαριασμός με αυτό το email"
                    exc.message?.contains("wrong-password") == true || exc.message?.contains("invalid-credential") == true -> "Λανθασμένος κωδικός ή email"
                    exc.message?.contains("invalid-email") == true -> "Μη έγκυρη μορφή email"
                    else -> exc.localizedMessage ?: "Σφάλμα σύνδεσης"
                }
                onError(msg)
            }
    }

    fun signUpWithEmail(name: String, email: String, pass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (email.isBlank() || pass.isBlank()) {
            onError("Παρακαλώ συμπληρώστε όλα τα πεδία")
            return
        }
        if (pass.length < 6) {
            onError("Ο κωδικός πρέπει να έχει τουλάχιστον 6 χαρακτήρες")
            return
        }
        _isAuthLoading.value = true
        auth.createUserWithEmailAndPassword(email.trim(), pass)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user != null) {
                    val updateReq = UserProfileChangeRequest.Builder()
                        .setDisplayName(name.trim().ifBlank { "Χρήστης" })
                        .build()
                    user.updateProfile(updateReq).addOnCompleteListener {
                        _isAuthLoading.value = false
                        _currentUser.value = user
                        showSnackbar("Ο λογαριασμός σας δημιουργήθηκε επιτυχώς!")
                        onSuccess()
                    }
                } else {
                    _isAuthLoading.value = false
                    onSuccess()
                }
            }
            .addOnFailureListener { exc ->
                _isAuthLoading.value = false
                val msg = when {
                    exc.message?.contains("email-already-in-use") == true -> "Αυτό το email χρησιμοποιείται ήδη"
                    exc.message?.contains("weak-password") == true -> "Ο κωδικός είναι πολύ αδύναμος"
                    exc.message?.contains("invalid-email") == true -> "Μη έγκυρη μορφή email"
                    else -> exc.localizedMessage ?: "Σφάλμα δημιουργίας λογαριασμού"
                }
                onError(msg)
            }
    }

    fun signInWithGoogleToken(idToken: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        _isAuthLoading.value = true
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener { result ->
                _isAuthLoading.value = false
                _currentUser.value = result.user
                showSnackbar("Συνδεθήκατε μέσω Google!")
                onSuccess()
            }
            .addOnFailureListener { exc ->
                _isAuthLoading.value = false
                onError(exc.localizedMessage ?: "Σφάλμα σύνδεσης Google")
            }
    }

    fun signOutUser() {
        auth.signOut()
        _currentUser.value = null
        _userProfile.value = null
        _currentHousehold.value = null
        householdJob?.cancel()
        expensesSyncJob?.cancel()
        showSnackbar("Αποσυνδεθήκατε επιτυχώς")
    }

    val allExpenses: StateFlow<List<Expense>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMembers: StateFlow<List<FamilyMember>> = repository.allMembers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<ExpenseCategory>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRecurringExpenses: StateFlow<List<RecurringExpense>> = repository.allRecurringExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSavingsGoals: StateFlow<List<SavingsGoal>> = repository.allSavingsGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filters
    private val _selectedMemberFilter = MutableStateFlow<Long?>(null) // null = all, -1 = common family, id = specific
    val selectedMemberFilter = _selectedMemberFilter.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow<Long?>(null)
    val selectedCategoryFilter = _selectedCategoryFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // Filtered Expenses
    val filteredExpenses: StateFlow<List<Expense>> = combine(
        allExpenses,
        _selectedMemberFilter,
        _selectedCategoryFilter,
        _searchQuery
    ) { expenses, memberId, catId, query ->
        expenses.filter { exp ->
            val matchMember = when (memberId) {
                null -> true
                -1L -> exp.splitType == "ALL_FAMILY"
                else -> {
                    val ids = exp.memberIdsCsv.split(",").mapNotNull { it.trim().toLongOrNull() }
                    ids.contains(memberId) || (exp.splitType == "ALL_FAMILY")
                }
            }

            val matchCat = catId == null || exp.categoryId == catId

            val matchQuery = if (query.isBlank()) {
                true
            } else {
                val q = GreekVoiceParser.normalizeGreek(query.trim())
                val titleNorm = GreekVoiceParser.normalizeGreek(exp.title)
                val catNorm = GreekVoiceParser.normalizeGreek(exp.categoryName)
                val membersNorm = GreekVoiceParser.normalizeGreek(exp.memberNamesDisplay)
                val noteNorm = GreekVoiceParser.normalizeGreek(exp.note)
                titleNorm.contains(q) || catNorm.contains(q) || membersNorm.contains(q) || noteNorm.contains(q)
            }

            matchMember && matchCat && matchQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Voice parsing & Countdown Confirmation
    private val _pendingParsedExpense = MutableStateFlow<ParsedVoiceExpense?>(null)
    val pendingParsedExpense = _pendingParsedExpense.asStateFlow()

    private val _countdownState = MutableStateFlow(CountdownState())
    val countdownState = _countdownState.asStateFlow()

    private var countdownJob: Job? = null

    // Voice state
    private val _isListening = MutableStateFlow(false)
    val isListening = _isListening.asStateFlow()

    private val _micSoundLevel = MutableStateFlow(0f)
    val micSoundLevel = _micSoundLevel.asStateFlow()

    // Dialogs state
    private val _showManualExpenseDialog = MutableStateFlow(false)
    val showManualExpenseDialog = _showManualExpenseDialog.asStateFlow()

    private val _editingExpense = MutableStateFlow<Expense?>(null)
    val editingExpense = _editingExpense.asStateFlow()

    private val _showMemberDialog = MutableStateFlow(false)
    val showMemberDialog = _showMemberDialog.asStateFlow()

    private val _editingMember = MutableStateFlow<FamilyMember?>(null)
    val editingMember = _editingMember.asStateFlow()

    private val _showCategoryDialog = MutableStateFlow(false)
    val showCategoryDialog = _showCategoryDialog.asStateFlow()

    private val _editingCategory = MutableStateFlow<ExpenseCategory?>(null)
    val editingCategory = _editingCategory.asStateFlow()

    // Settings Dialog & User Preferences
    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog = _showSettingsDialog.asStateFlow()

    private val _themeMode = MutableStateFlow("SYSTEM") // "SYSTEM", "LIGHT", "DARK"
    val themeMode = _themeMode.asStateFlow()

    private val _currencySymbol = MutableStateFlow("€")
    val currencySymbol = _currencySymbol.asStateFlow()

    // Quick Action Chooser (Home Screen Tile / App Launch)
    private val _launchWithChooser = MutableStateFlow(true)
    val launchWithChooser = _launchWithChooser.asStateFlow()

    private val _showQuickActionChooser = MutableStateFlow(true)
    val showQuickActionChooser = _showQuickActionChooser.asStateFlow()

    private val _showVoiceInputDialog = MutableStateFlow(false)
    val showVoiceInputDialog = _showVoiceInputDialog.asStateFlow()

    private val _showReceiptScannerDialog = MutableStateFlow(false)
    val showReceiptScannerDialog = _showReceiptScannerDialog.asStateFlow()

    private val _showExportReportDialog = MutableStateFlow(false)
    val showExportReportDialog = _showExportReportDialog.asStateFlow()

    private val _showSavingsGoalDialog = MutableStateFlow(false)
    val showSavingsGoalDialog = _showSavingsGoalDialog.asStateFlow()

    private val _editingSavingsGoal = MutableStateFlow<SavingsGoal?>(null)
    val editingSavingsGoal = _editingSavingsGoal.asStateFlow()

    private val _showRecurringExpenseDialog = MutableStateFlow(false)
    val showRecurringExpenseDialog = _showRecurringExpenseDialog.asStateFlow()

    private val _editingRecurringExpense = MutableStateFlow<RecurringExpense?>(null)
    val editingRecurringExpense = _editingRecurringExpense.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage = _snackbarMessage.asStateFlow()

    fun openVoiceInputDialog() {
        _showVoiceInputDialog.value = true
    }

    fun dismissVoiceInputDialog() {
        _showVoiceInputDialog.value = false
    }

    fun openReceiptScanner() {
        _showReceiptScannerDialog.value = true
    }

    fun dismissReceiptScanner() {
        _showReceiptScannerDialog.value = false
    }

    fun openExportReportDialog() {
        _showExportReportDialog.value = true
    }

    fun dismissExportReportDialog() {
        _showExportReportDialog.value = false
    }

    fun openSavingsGoalDialog(goal: SavingsGoal? = null) {
        _editingSavingsGoal.value = goal
        _showSavingsGoalDialog.value = true
    }

    fun dismissSavingsGoalDialog() {
        _editingSavingsGoal.value = null
        _showSavingsGoalDialog.value = false
    }

    fun saveSavingsGoal(
        id: Long,
        title: String,
        targetAmount: Double,
        currentAmount: Double,
        deadline: String,
        note: String
    ) {
        viewModelScope.launch {
            val goal = SavingsGoal(
                id = id,
                title = title,
                targetAmount = targetAmount,
                currentAmount = currentAmount,
                deadlineDate = deadline,
                note = note
            )
            if (id == 0L) {
                repository.insertSavingsGoal(goal)
                showSnackbar("Ο στόχος «$title» δημιουργήθηκε!")
            } else {
                repository.updateSavingsGoal(goal)
                showSnackbar("Ο στόχος «$title» ενημερώθηκε!")
            }
        }
    }

    fun deleteSavingsGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            repository.deleteSavingsGoal(goal)
            showSnackbar("Ο στόχος «${goal.title}» διαγράφηκε")
        }
    }

    fun depositToGoal(goal: SavingsGoal, amount: Double) {
        viewModelScope.launch {
            val updated = goal.copy(currentAmount = goal.currentAmount + amount)
            repository.updateSavingsGoal(updated)
            if (updated.currentAmount >= updated.targetAmount) {
                showSnackbar("🎉 Συγχαρητήρια! Πετύχατε το στόχο «${goal.title}»!")
            } else {
                showSnackbar("Προστέθηκαν ${amount.toInt()}€ στο στόχο «${goal.title}»")
            }
        }
    }

    fun openRecurringExpenseDialog(recurring: RecurringExpense? = null) {
        _editingRecurringExpense.value = recurring
        _showRecurringExpenseDialog.value = true
    }

    fun dismissRecurringExpenseDialog() {
        _editingRecurringExpense.value = null
        _showRecurringExpenseDialog.value = false
    }

    fun saveRecurringExpense(
        id: Long,
        title: String,
        amount: Double,
        categoryId: Long,
        categoryName: String,
        dayOfMonth: Int,
        note: String
    ) {
        viewModelScope.launch {
            val recurring = RecurringExpense(
                id = id,
                title = title,
                amount = amount,
                categoryId = categoryId,
                categoryName = categoryName,
                dayOfMonth = dayOfMonth,
                note = note,
                isActive = true
            )
            if (id == 0L) {
                repository.insertRecurringExpense(recurring)
                showSnackbar("Η πάγια εντολή «$title» δημιουργήθηκε!")
            } else {
                repository.updateRecurringExpense(recurring)
                showSnackbar("Η πάγια εντολή «$title» ενημερώθηκε!")
            }
        }
    }

    fun deleteRecurringExpense(recurring: RecurringExpense) {
        viewModelScope.launch {
            repository.deleteRecurringExpense(recurring)
            showSnackbar("Η πάγια εντολή «${recurring.title}» διαγράφηκε")
        }
    }

    fun toggleRecurringActive(recurring: RecurringExpense) {
        viewModelScope.launch {
            val updated = recurring.copy(isActive = !recurring.isActive)
            repository.updateRecurringExpense(updated)
            val status = if (updated.isActive) "ενεργοποιήθηκε" else "απενεργοποιήθηκε"
            showSnackbar("Η πάγια εντολή «${recurring.title}» $status")
        }
    }

    fun executeRecurringExpenseNow(recurring: RecurringExpense) {
        viewModelScope.launch {
            val expense = Expense(
                title = recurring.title,
                amount = recurring.amount,
                categoryId = recurring.categoryId,
                categoryName = recurring.categoryName,
                timestamp = System.currentTimeMillis(),
                splitType = recurring.splitType,
                memberNamesDisplay = recurring.memberNamesDisplay,
                note = "Εκτέλεση πάγιας εντολής (${recurring.dayOfMonth}η του μήνα)"
            )
            val newId = repository.insertExpense(expense)
            val user = _currentUser.value
            val household = _currentHousehold.value
            if (user != null) {
                firestoreSyncRepo.syncUserExpense(
                    userId = user.uid,
                    expense = FirebaseExpense(
                        id = newId.toString(),
                        householdId = household?.id ?: "",
                        title = expense.title,
                        amount = expense.amount,
                        categoryId = expense.categoryId,
                        categoryName = expense.categoryName,
                        timestamp = expense.timestamp,
                        splitType = expense.splitType,
                        memberNamesDisplay = expense.memberNamesDisplay,
                        note = expense.note,
                        createdByUid = user.uid,
                        createdByName = user.displayName ?: "Χρήστης"
                    )
                )
            }
            if (household != null && household.id.isNotBlank()) {
                firestoreSyncRepo.syncExpense(
                    FirebaseExpense(
                        id = newId.toString(),
                        householdId = household.id,
                        title = expense.title,
                        amount = expense.amount,
                        categoryId = expense.categoryId,
                        categoryName = expense.categoryName,
                        timestamp = expense.timestamp,
                        splitType = expense.splitType,
                        memberNamesDisplay = expense.memberNamesDisplay,
                        note = expense.note,
                        createdByUid = user?.uid ?: "",
                        createdByName = user?.displayName ?: "Μέλος"
                    )
                )
            }
            showSnackbar("Εκτελέστηκε η πάγια: ${recurring.title} (${String.format(java.util.Locale.US, "%.2f€", recurring.amount)})")
        }
    }

    fun setLaunchWithChooser(enabled: Boolean) {
        _launchWithChooser.value = enabled
    }

    fun openSettingsDialog() {
        _showSettingsDialog.value = true
    }

    fun dismissSettingsDialog() {
        _showSettingsDialog.value = false
    }

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
    }

    fun setCurrencySymbol(symbol: String) {
        _currencySymbol.value = symbol
    }

    fun openQuickActionChooser() {
        _showQuickActionChooser.value = true
    }

    fun dismissQuickActionChooser() {
        _showQuickActionChooser.value = false
    }

    fun setMemberFilter(memberId: Long?) {
        _selectedMemberFilter.value = memberId
    }

    fun setCategoryFilter(categoryId: Long?) {
        _selectedCategoryFilter.value = categoryId
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setListening(listening: Boolean) {
        _isListening.value = listening
        if (!listening) {
            _micSoundLevel.value = 0f
        }
    }

    fun setMicSoundLevel(level: Float) {
        _micSoundLevel.value = level
    }

    fun showSnackbar(message: String) {
        _snackbarMessage.value = message
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun processVoiceInput(spokenText: String) {
        if (spokenText.isBlank()) return

        val parsed = GreekVoiceParser.parse(
            prompt = spokenText,
            categories = allCategories.value,
            allMembers = allMembers.value
        )
        // Show confirmation dialog with parsed values for review and editing
        _pendingParsedExpense.value = parsed
    }

    fun cancelPendingExpense() {
        countdownJob?.cancel()
        countdownJob = null
        _countdownState.value = CountdownState(remainingSeconds = 3, isActive = false)
        _pendingParsedExpense.value = null
        showSnackbar("Η καταχώρηση ακυρώθηκε")
    }

    fun confirmVoiceExpense(
        title: String,
        amount: Double,
        categoryId: Long,
        categoryName: String,
        timestamp: Long,
        splitType: String,
        memberIds: List<Long>,
        memberNamesDisplay: String,
        originalPrompt: String
    ) {
        _pendingParsedExpense.value = null
        viewModelScope.launch {
            val count = if (memberIds.isNotEmpty()) memberIds.size else 1
            val splitPerPerson = amount / count
            val expense = Expense(
                title = title,
                amount = amount,
                categoryId = categoryId,
                categoryName = categoryName,
                timestamp = timestamp,
                splitType = splitType,
                memberIdsCsv = memberIds.joinToString(","),
                memberNamesDisplay = memberNamesDisplay,
                splitAmountPerPerson = splitPerPerson,
                originalVoicePrompt = originalPrompt,
                note = ""
            )
            repository.insertExpense(expense)
            val user = _currentUser.value
            val household = _currentHousehold.value
            if (user != null) {
                firestoreSyncRepo.syncUserExpense(
                    userId = user.uid,
                    expense = FirebaseExpense(
                        id = expense.id.toString(),
                        householdId = household?.id ?: "",
                        title = title,
                        amount = amount,
                        categoryId = categoryId,
                        categoryName = categoryName,
                        timestamp = timestamp,
                        splitType = splitType,
                        memberNamesDisplay = memberNamesDisplay,
                        note = "",
                        createdByUid = user.uid,
                        createdByName = user.displayName ?: "Χρήστης"
                    )
                )
            }
            if (household != null && household.id.isNotBlank()) {
                firestoreSyncRepo.syncExpense(
                    FirebaseExpense(
                        id = expense.id.toString(),
                        householdId = household.id,
                        title = title,
                        amount = amount,
                        categoryId = categoryId,
                        categoryName = categoryName,
                        timestamp = timestamp,
                        splitType = splitType,
                        memberNamesDisplay = memberNamesDisplay,
                        note = "",
                        createdByUid = user?.uid ?: "",
                        createdByName = user?.displayName ?: "Μέλος"
                    )
                )
            }
            showSnackbar("Καταχωρήθηκε: $title (${String.format(java.util.Locale.US, "%.2f€", amount)})")
        }
    }

    fun confirmPendingExpense() {
        val pending = _pendingParsedExpense.value ?: return
        confirmVoiceExpense(
            title = pending.title,
            amount = pending.amount,
            categoryId = pending.categoryId,
            categoryName = pending.categoryName,
            timestamp = System.currentTimeMillis(),
            splitType = pending.splitType,
            memberIds = pending.memberIds,
            memberNamesDisplay = pending.memberNamesDisplay,
            originalPrompt = pending.originalPrompt
        )
    }

    fun editPendingExpense() {
        countdownJob?.cancel()
        countdownJob = null
        _countdownState.value = CountdownState(remainingSeconds = 3, isActive = false)

        val pending = _pendingParsedExpense.value ?: return
        val expense = Expense(
            title = pending.title,
            amount = pending.amount,
            categoryId = pending.categoryId,
            categoryName = pending.categoryName,
            timestamp = System.currentTimeMillis(),
            splitType = pending.splitType,
            memberIdsCsv = pending.memberIds.joinToString(","),
            memberNamesDisplay = pending.memberNamesDisplay,
            splitAmountPerPerson = pending.splitAmountPerPerson,
            originalVoicePrompt = pending.originalPrompt,
            note = ""
        )
        _pendingParsedExpense.value = null
        openManualExpenseDialog(expense)
    }

    // Expense CRUD
    fun openManualExpenseDialog(expense: Expense? = null) {
        _editingExpense.value = expense
        _showManualExpenseDialog.value = true
    }

    fun dismissManualExpenseDialog() {
        _editingExpense.value = null
        _showManualExpenseDialog.value = false
    }

    fun saveExpense(
        id: Long = 0,
        title: String,
        amount: Double,
        categoryId: Long,
        categoryName: String,
        timestamp: Long,
        splitType: String,
        memberIds: List<Long>,
        memberNamesDisplay: String,
        note: String
    ) {
        viewModelScope.launch {
            val splitPerPerson = if (memberIds.isNotEmpty()) amount / memberIds.size else amount
            val expense = Expense(
                id = id,
                title = title,
                amount = amount,
                categoryId = categoryId,
                categoryName = categoryName,
                timestamp = timestamp,
                splitType = splitType,
                memberIdsCsv = memberIds.joinToString(","),
                memberNamesDisplay = memberNamesDisplay,
                splitAmountPerPerson = splitPerPerson,
                note = note
            )
            if (id == 0L) {
                repository.insertExpense(expense)
                showSnackbar("Προστέθηκε νέο έξοδο: $title")
            } else {
                repository.updateExpense(expense)
                showSnackbar("Το έξοδο ενημερώθηκε")
            }

            val user = _currentUser.value
            val household = _currentHousehold.value
            if (user != null) {
                firestoreSyncRepo.syncUserExpense(
                    userId = user.uid,
                    expense = FirebaseExpense(
                        id = expense.id.toString(),
                        householdId = household?.id ?: "",
                        title = title,
                        amount = amount,
                        categoryId = categoryId,
                        categoryName = categoryName,
                        timestamp = timestamp,
                        splitType = splitType,
                        memberNamesDisplay = memberNamesDisplay,
                        note = note,
                        createdByUid = user.uid,
                        createdByName = user.displayName ?: "Χρήστης"
                    )
                )
            }
            if (household != null && household.id.isNotBlank()) {
                firestoreSyncRepo.syncExpense(
                    FirebaseExpense(
                        id = expense.id.toString(),
                        householdId = household.id,
                        title = title,
                        amount = amount,
                        categoryId = categoryId,
                        categoryName = categoryName,
                        timestamp = timestamp,
                        splitType = splitType,
                        memberNamesDisplay = memberNamesDisplay,
                        note = note,
                        createdByUid = user?.uid ?: "",
                        createdByName = user?.displayName ?: "Μέλος"
                    )
                )
            }
            dismissManualExpenseDialog()
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
            val user = _currentUser.value
            if (user != null) {
                firestoreSyncRepo.deleteUserExpense(user.uid, expense.id.toString())
            }
            val household = _currentHousehold.value
            if (household != null && household.id.isNotBlank()) {
                firestoreSyncRepo.deleteExpense(household.id, expense.id.toString())
            }
            showSnackbar("Το έξοδο διαγράφηκε")
        }
    }

    // Member CRUD
    fun openMemberDialog(member: FamilyMember? = null) {
        _editingMember.value = member
        _showMemberDialog.value = true
    }

    fun dismissMemberDialog() {
        _editingMember.value = null
        _showMemberDialog.value = false
    }

    fun saveMember(
        id: Long = 0,
        name: String,
        role: String,
        avatarColorHex: Long,
        voiceKeywords: String
    ) {
        viewModelScope.launch {
            val member = FamilyMember(
                id = id,
                name = name.trim(),
                role = role.trim(),
                avatarColorHex = avatarColorHex,
                voiceKeywords = voiceKeywords.trim()
            )
            if (id == 0L) {
                repository.insertMember(member)
                showSnackbar("Προστέθηκε το μέλος $name")
            } else {
                repository.updateMember(member)
                showSnackbar("Ενημερώθηκε το μέλος $name")
            }
            dismissMemberDialog()
        }
    }

    fun deleteMember(member: FamilyMember) {
        viewModelScope.launch {
            repository.deleteMember(member)
            showSnackbar("Το μέλος ${member.name} αφαιρέθηκε")
        }
    }

    // Category CRUD
    fun openCategoryDialog(category: ExpenseCategory? = null) {
        _editingCategory.value = category
        _showCategoryDialog.value = true
    }

    fun dismissCategoryDialog() {
        _editingCategory.value = null
        _showCategoryDialog.value = false
    }

    fun saveCategory(
        id: Long,
        name: String,
        iconKey: String,
        monthlyBudget: Double,
        colorHex: Long,
        voiceKeywords: String
    ) {
        viewModelScope.launch {
            val category = ExpenseCategory(
                id = id,
                name = name.trim(),
                iconKey = iconKey,
                monthlyBudget = monthlyBudget,
                colorHex = colorHex,
                voiceKeywords = voiceKeywords.trim()
            )
            if (id == 0L) {
                repository.insertCategory(category)
                showSnackbar("Προστέθηκε κατηγορία: $name")
            } else {
                repository.updateCategory(category)
                showSnackbar("Ενημερώθηκε η κατηγορία: $name")
            }
            dismissCategoryDialog()
        }
    }

    fun deleteCategory(category: ExpenseCategory) {
        viewModelScope.launch {
            repository.deleteCategory(category)
            showSnackbar("Η κατηγορία «${category.name}» διαγράφηκε")
        }
    }
}
