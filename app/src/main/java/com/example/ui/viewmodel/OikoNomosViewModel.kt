package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.entity.Expense
import com.example.data.entity.ExpenseCategory
import com.example.data.entity.FamilyMember
import com.example.data.entity.Income
import com.example.data.entity.RecurringExpense
import com.example.data.entity.SavingsGoal
import com.example.data.model.FirebaseExpense
import com.example.data.model.FirebaseIncome
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class CountdownState(
    val remainingSeconds: Int = 3,
    val isActive: Boolean = false
)

class OikoNomosViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ExpenseRepository
    private val firestoreSyncRepo = FirestoreSyncRepository.create(application)
    private val auth: FirebaseAuth = Firebase.auth
    private val prefs = application.getSharedPreferences("oikonomos_app_prefs", Context.MODE_PRIVATE)

    // Firebase Auth & Cloud Sync States
    private val _isAuthInitialized = MutableStateFlow(false)
    val isAuthInitialized = _isAuthInitialized.asStateFlow()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser = _currentUser.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile = _userProfile.asStateFlow()

    private val _currentHousehold = MutableStateFlow<Household?>(null)
    val currentHousehold = _currentHousehold.asStateFlow()

    private val _userHouseholds = MutableStateFlow<List<Household>>(emptyList())
    val userHouseholds = _userHouseholds.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    private val _isInitialExpensesLoading = MutableStateFlow(true)
    val isInitialExpensesLoading = _isInitialExpensesLoading.asStateFlow()

    private var householdJob: Job? = null
    private var expensesSyncJob: Job? = null
    private var userExpensesSyncJob: Job? = null
    private var incomesSyncJob: Job? = null
    private var userIncomesSyncJob: Job? = null

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ExpenseRepository(database.expenseDao(), application)
        viewModelScope.launch {
            repository.deleteAllExpenses() // Zero local storage: everything lives purely in Cloud Firestore
            repository.deleteAllIncomes()
            repository.seedInitialDataIfEmpty()
        }

        // Fast pre-warm: If we have a cached active household ID, kick off disk cache observation immediately!
        val cachedHouseholdId = prefs.getString("last_household_id", null)
        if (!cachedHouseholdId.isNullOrBlank()) {
            observeHouseholdData(cachedHouseholdId)
        }

        // Listen for Google Auth state
        var lastObservedUid: String? = null
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            val currentUid = user?.uid
            _isAuthInitialized.value = true
            if (user != null) {
                _currentUser.value = user
                // If user is already authenticated and sync listeners are active, do not discard or restart listeners on resume
                if (currentUid != lastObservedUid || _currentHousehold.value == null || expensesSyncJob?.isActive != true) {
                    lastObservedUid = currentUid
                    viewModelScope.launch {
                        loadUserProfileAndHousehold(user)
                    }
                }
            } else if (lastObservedUid != null && _currentUser.value == null) {
                // Only clear if user intentionally logged out
                lastObservedUid = null
                _userProfile.value = null
                _currentHousehold.value = null
                _userHouseholds.value = emptyList()
                prefs.edit().remove("last_household_id").apply()
                householdJob?.cancel()
                expensesSyncJob?.cancel()
                userExpensesSyncJob?.cancel()
                incomesSyncJob?.cancel()
                userIncomesSyncJob?.cancel()
            }
        }
    }

    private suspend fun loadUserProfileAndHousehold(user: FirebaseUser) {
        try {
            // First check if profile exists in cache/remote
            val profile = firestoreSyncRepo.getOrCreateUserProfile(
                user.uid,
                user.email ?: "",
                user.displayName ?: ""
            )
            _userProfile.value = profile

            if (profile.householdId.isNotBlank()) {
                prefs.edit().putString("last_household_id", profile.householdId).apply()
                // Fast path: if not already observing this household, observe it right away
                if (_currentHousehold.value?.id != profile.householdId || expensesSyncJob?.isActive != true) {
                    observeHouseholdData(profile.householdId)
                }
            }

            val households = firestoreSyncRepo.fetchUserHouseholds(profile.householdIds)
            _userHouseholds.value = households

            if (profile.householdId.isNotBlank()) {
                val current = households.find { it.id == profile.householdId }
                if (current != null) {
                    _currentHousehold.value = current
                } else if (households.isNotEmpty()) {
                    _currentHousehold.value = households.first()
                }
            }
            observeUserData(user.uid)
        } catch (e: Exception) {
            Log.e("OikoNomosVM", "Error loading profile/household", e)
        }
    }

    private fun mapFirebaseExpensesToDomain(remoteExpenses: List<FirebaseExpense>): List<Expense> {
        return remoteExpenses.map { remote ->
            val hashId = Math.abs(remote.id.hashCode().toLong())
            val memberCount = remote.memberIdsCsv.split(",").filter { it.isNotBlank() }.size.coerceAtLeast(1)
            Expense(
                id = if (hashId == 0L) 1L else hashId,
                cloudId = remote.id,
                title = remote.title,
                amount = remote.amount,
                categoryId = remote.categoryId,
                categoryName = remote.categoryName,
                timestamp = remote.timestamp,
                splitType = remote.splitType,
                memberIdsCsv = remote.memberIdsCsv,
                memberNamesDisplay = remote.memberNamesDisplay,
                splitAmountPerPerson = remote.amount / memberCount,
                originalVoicePrompt = "",
                note = remote.note
            )
        }
    }

    private fun mapFirebaseIncomesToDomain(remoteIncomes: List<FirebaseIncome>): List<Income> {
        return remoteIncomes.map { remote ->
            val hashId = Math.abs(remote.id.hashCode().toLong())
            Income(
                id = if (hashId == 0L) 1L else hashId,
                cloudId = remote.id,
                title = remote.title,
                amount = remote.amount,
                memberId = remote.memberId,
                memberName = remote.memberName,
                category = remote.category,
                timestamp = remote.timestamp,
                note = remote.note
            )
        }
    }

    private fun observeUserData(userId: String) {
        userExpensesSyncJob?.cancel()
        userExpensesSyncJob = viewModelScope.launch {
            firestoreSyncRepo.observeUserExpenses(userId).collect { remoteExpenses ->
                if (_currentHousehold.value == null) {
                    _allExpenses.value = mapFirebaseExpensesToDomain(remoteExpenses)
                }
            }
        }

        userIncomesSyncJob?.cancel()
        userIncomesSyncJob = viewModelScope.launch {
            firestoreSyncRepo.observeUserIncomes(userId).collect { remoteIncomes ->
                if (_currentHousehold.value == null) {
                    _allIncomes.value = mapFirebaseIncomesToDomain(remoteIncomes)
                }
            }
        }
    }

    private fun observeHouseholdData(householdId: String) {
        householdJob?.cancel()
        householdJob = viewModelScope.launch {
            while (isActive) {
                try {
                    firestoreSyncRepo.observeHousehold(householdId).collect { h ->
                        _currentHousehold.value = h
                    }
                } catch (e: Exception) {
                    Log.w("OikoNomosVM", "Household listener reconnecting: ${e.message}")
                    delay(3000)
                }
            }
        }

        expensesSyncJob?.cancel()
        expensesSyncJob = viewModelScope.launch {
            // Step 1: Immediately read from local disk cache (< 10ms, offline-first)
            try {
                val cached = firestoreSyncRepo.getExpensesFromCache(householdId)
                if (cached.isNotEmpty()) {
                    _allExpenses.value = mapFirebaseExpensesToDomain(cached)
                    _isInitialExpensesLoading.value = false
                }
            } catch (e: Exception) {
                Log.d("OikoNomosVM", "Disk cache expenses: ${e.message}")
            }

            // Step 2: Fetch latest from server
            try {
                val direct = firestoreSyncRepo.getExpensesOnce(householdId)
                if (direct.isNotEmpty() || _allExpenses.value.isEmpty()) {
                    _allExpenses.value = mapFirebaseExpensesToDomain(direct)
                }
            } catch (e: Exception) {
                Log.w("OikoNomosVM", "Initial direct expenses fetch: ${e.message}")
            } finally {
                _isInitialExpensesLoading.value = false
            }

            // Step 3: Real-time listener for continuous updates
            while (isActive) {
                try {
                    firestoreSyncRepo.observeExpenses(householdId).collect { remoteExpenses ->
                        _allExpenses.value = mapFirebaseExpensesToDomain(remoteExpenses)
                        _isInitialExpensesLoading.value = false
                    }
                } catch (e: Exception) {
                    Log.w("OikoNomosVM", "Expenses listener reconnecting: ${e.message}")
                    delay(3000)
                }
            }
        }

        incomesSyncJob?.cancel()
        incomesSyncJob = viewModelScope.launch {
            try {
                val cached = firestoreSyncRepo.getIncomesFromCache(householdId)
                if (cached.isNotEmpty()) {
                    _allIncomes.value = mapFirebaseIncomesToDomain(cached)
                }
            } catch (e: Exception) {
                Log.d("OikoNomosVM", "Disk cache incomes: ${e.message}")
            }

            try {
                val direct = firestoreSyncRepo.getIncomesOnce(householdId)
                if (direct.isNotEmpty() || _allIncomes.value.isEmpty()) {
                    _allIncomes.value = mapFirebaseIncomesToDomain(direct)
                }
            } catch (e: Exception) {
                Log.w("OikoNomosVM", "Initial direct incomes fetch: ${e.message}")
            }

            while (isActive) {
                try {
                    firestoreSyncRepo.observeIncomes(householdId).collect { remoteIncomes ->
                        _allIncomes.value = mapFirebaseIncomesToDomain(remoteIncomes)
                    }
                } catch (e: Exception) {
                    Log.w("OikoNomosVM", "Incomes listener reconnecting: ${e.message}")
                    delay(3000)
                }
            }
        }
    }

    /**
     * Called when the app resumes (e.g. from lock screen or switching from another app).
     * Re-enables Firestore network, restarts observation if needed, and fetches latest balances.
     */
    fun onAppResumed() {
        val user = _currentUser.value ?: auth.currentUser ?: return
        _currentUser.value = user
        viewModelScope.launch {
            try {
                firestoreSyncRepo.enableNetwork()
                val household = _currentHousehold.value
                if (household != null && household.id.isNotBlank()) {
                    val freshExpenses = firestoreSyncRepo.getExpensesOnce(household.id)
                    if (freshExpenses.isNotEmpty()) {
                        _allExpenses.value = mapFirebaseExpensesToDomain(freshExpenses)
                    }
                    val freshIncomes = firestoreSyncRepo.getIncomesOnce(household.id)
                    if (freshIncomes.isNotEmpty()) {
                        _allIncomes.value = mapFirebaseIncomesToDomain(freshIncomes)
                    }
                    if (expensesSyncJob?.isActive != true || incomesSyncJob?.isActive != true) {
                        observeHouseholdData(household.id)
                    }
                } else {
                    // Single-user mode without household: fetch directly from user documents
                    val userExpenses = firestoreSyncRepo.getUserExpensesOnce(user.uid)
                    if (userExpenses.isNotEmpty()) {
                        _allExpenses.value = mapFirebaseExpensesToDomain(userExpenses)
                    }
                    val userIncomes = firestoreSyncRepo.getUserIncomesOnce(user.uid)
                    if (userIncomes.isNotEmpty()) {
                        _allIncomes.value = mapFirebaseIncomesToDomain(userIncomes)
                    }
                    loadUserProfileAndHousehold(user)
                }
            } catch (e: Exception) {
                Log.w("OikoNomosVM", "Error in onAppResumed: ${e.message}")
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
        _userHouseholds.value = emptyList()
        householdJob?.cancel()
        expensesSyncJob?.cancel()
        userExpensesSyncJob?.cancel()
        _allExpenses.value = emptyList()
        viewModelScope.launch {
            repository.deleteAllExpenses()
        }
        showSnackbar("Αποσυνδεθήκατε επιτυχώς")
    }

    fun factoryResetAndSignOut() {
        auth.signOut()
        _currentUser.value = null
        _userProfile.value = null
        _currentHousehold.value = null
        _userHouseholds.value = emptyList()
        householdJob?.cancel()
        expensesSyncJob?.cancel()
        userExpensesSyncJob?.cancel()
        _allExpenses.value = emptyList()
        viewModelScope.launch {
            repository.clearAllData()
            repository.seedInitialDataIfEmpty()
            showSnackbar("Η εφαρμογή επανήλθε σε αρχική καθαρή κατάσταση")
        }
    }

    private val _allExpenses = MutableStateFlow<List<Expense>>(emptyList())
    val allExpenses: StateFlow<List<Expense>> = _allExpenses.asStateFlow()

    private val _allIncomes = MutableStateFlow<List<Income>>(emptyList())
    val allIncomes: StateFlow<List<Income>> = _allIncomes.asStateFlow()

    val allMembers: StateFlow<List<FamilyMember>> = repository.allMembers
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allCategories: StateFlow<List<ExpenseCategory>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allRecurringExpenses: StateFlow<List<RecurringExpense>> = repository.allRecurringExpenses
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allSavingsGoals: StateFlow<List<SavingsGoal>> = repository.allSavingsGoals
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

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
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Filtered Incomes
    val filteredIncomes: StateFlow<List<Income>> = combine(
        allIncomes,
        _selectedMemberFilter,
        _searchQuery
    ) { incomes, memberId, query ->
        incomes.filter { inc ->
            val matchMember = memberId == null || inc.memberId == memberId
            val matchQuery = if (query.isBlank()) {
                true
            } else {
                val q = GreekVoiceParser.normalizeGreek(query.trim())
                val titleNorm = GreekVoiceParser.normalizeGreek(inc.title)
                val catNorm = GreekVoiceParser.normalizeGreek(inc.category)
                val memberNorm = GreekVoiceParser.normalizeGreek(inc.memberName)
                val noteNorm = GreekVoiceParser.normalizeGreek(inc.note)
                titleNorm.contains(q) || catNorm.contains(q) || memberNorm.contains(q) || noteNorm.contains(q)
            }
            matchMember && matchQuery
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Income Dialogs state
    private val _showManualIncomeDialog = MutableStateFlow(false)
    val showManualIncomeDialog = _showManualIncomeDialog.asStateFlow()

    private val _editingIncome = MutableStateFlow<Income?>(null)
    val editingIncome = _editingIncome.asStateFlow()

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

    private val _themeMode = MutableStateFlow(prefs.getString("theme_mode", "SYSTEM") ?: "SYSTEM") // "SYSTEM", "LIGHT", "DARK"
    val themeMode = _themeMode.asStateFlow()

    private val _appLanguage = MutableStateFlow(prefs.getString("app_language", "EL") ?: "EL") // "EL", "EN"
    val appLanguage = _appLanguage.asStateFlow()

    private val _currencySymbol = MutableStateFlow(prefs.getString("currency_symbol", "€") ?: "€")
    val currencySymbol = _currencySymbol.asStateFlow()

    // Quick Action Chooser (Triggered via FAB or Widget)
    private val _launchWithChooser = MutableStateFlow(prefs.getBoolean("launch_with_chooser", false))
    val launchWithChooser = _launchWithChooser.asStateFlow()

    private val _showQuickActionChooser = MutableStateFlow(false)
    val showQuickActionChooser = _showQuickActionChooser.asStateFlow()

    private val _showVoiceInputDialog = MutableStateFlow(false)
    val showVoiceInputDialog = _showVoiceInputDialog.asStateFlow()

    private val _showExodologioManagerDialog = MutableStateFlow(false)
    val showExodologioManagerDialog = _showExodologioManagerDialog.asStateFlow()

    private val _showBurgerMenu = MutableStateFlow(false)
    val showBurgerMenu = _showBurgerMenu.asStateFlow()

    private val _showUserProfileDialog = MutableStateFlow(false)
    val showUserProfileDialog = _showUserProfileDialog.asStateFlow()

    private val _showExodologioDetailsDialog = MutableStateFlow(false)
    val showExodologioDetailsDialog = _showExodologioDetailsDialog.asStateFlow()

    private val _premiumPaywallReason = MutableStateFlow<String?>(null)
    val premiumPaywallReason = _premiumPaywallReason.asStateFlow()

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

    fun openBurgerMenu() {
        _showBurgerMenu.value = true
    }

    fun dismissBurgerMenu() {
        _showBurgerMenu.value = false
    }

    fun openUserProfileDialog() {
        _showUserProfileDialog.value = true
    }

    fun dismissUserProfileDialog() {
        _showUserProfileDialog.value = false
    }

    fun openExodologioDetailsDialog() {
        _showExodologioDetailsDialog.value = true
    }

    fun dismissExodologioDetailsDialog() {
        _showExodologioDetailsDialog.value = false
    }

    fun openExodologioManager() {
        _showExodologioManagerDialog.value = true
    }

    fun dismissExodologioManager() {
        _showExodologioManagerDialog.value = false
    }

    fun renameCurrentHousehold(newName: String) {
        val household = _currentHousehold.value ?: return
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return

        viewModelScope.launch {
            try {
                firestoreSyncRepo.renameHousehold(household.id, trimmed)
                val updated = household.copy(name = trimmed)
                _currentHousehold.value = updated
                _userHouseholds.value = _userHouseholds.value.map {
                    if (it.id == household.id) updated else it
                }
                showSnackbar("Το εξοδολόγιο μετονομάστηκε σε «$trimmed»")
            } catch (e: Exception) {
                showSnackbar("Σφάλμα μετονομασίας: ${e.localizedMessage}")
            }
        }
    }

    fun updateUserDisplayName(newName: String) {
        val user = _currentUser.value ?: return
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return

        viewModelScope.launch {
            try {
                firestoreSyncRepo.updateUserProfileName(user.uid, trimmed)
                _userProfile.value = _userProfile.value?.copy(displayName = trimmed)
                showSnackbar("Το όνομά σας ενημερώθηκε σε «$trimmed»")
            } catch (e: Exception) {
                showSnackbar("Σφάλμα ενημέρωσης: ${e.localizedMessage}")
            }
        }
    }

    fun clearCurrentExodologioExpenses() {
        val household = _currentHousehold.value
        val user = _currentUser.value
        viewModelScope.launch {
            try {
                firestoreSyncRepo.clearAllExpenses(household?.id ?: "", user?.uid ?: "")
                _allExpenses.value = emptyList()
                showSnackbar("Τα έξοδα του εξοδολογίου «${household?.name ?: ""}» εκκαθαρίστηκαν από τη βάση δεδομένων")
            } catch (e: Exception) {
                showSnackbar("Σφάλμα εκκαθάρισης: ${e.localizedMessage}")
            }
        }
    }

    fun showPremiumPaywall(reason: String) {
        _premiumPaywallReason.value = reason
    }

    fun dismissPremiumPaywall() {
        _premiumPaywallReason.value = null
    }

    fun unlockPro() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            try {
                firestoreSyncRepo.setPremiumStatus(user.uid, true)
                _userProfile.value = _userProfile.value?.copy(isPremium = true)
                showSnackbar("🌟 Συγχαρητήρια! Το OikoNomos Pro ενεργοποιήθηκε.")
            } catch (e: Exception) {
                showSnackbar("Σφάλμα ενεργοποίησης Pro")
            }
        }
    }

    fun createNewExodologio(name: String) {
        val user = _currentUser.value ?: return
        val profile = _userProfile.value ?: return

        if (!profile.isPremium && _userHouseholds.value.size >= 1) {
            showPremiumPaywall("multi_exodologio")
            return
        }

        viewModelScope.launch {
            try {
                val newHousehold = firestoreSyncRepo.createHousehold(user.uid, name)
                _userHouseholds.value = _userHouseholds.value + newHousehold
                switchExodologio(newHousehold)
                showSnackbar("Το εξοδολόγιο «${newHousehold.name}» δημιουργήθηκε!")
            } catch (e: Exception) {
                showSnackbar("Σφάλμα δημιουργίας: ${e.localizedMessage}")
            }
        }
    }

    fun switchExodologio(household: Household) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            try {
                prefs.edit().putString("last_household_id", household.id).apply()
                firestoreSyncRepo.switchActiveHousehold(user.uid, household.id)
                _currentHousehold.value = household
                _userProfile.value = _userProfile.value?.copy(householdId = household.id)
                repository.deleteAllExpenses()
                observeHouseholdData(household.id)
                showSnackbar("Ενεργό εξοδολόγιο: ${household.name}")
            } catch (e: Exception) {
                showSnackbar("Σφάλμα αλλαγής: ${e.localizedMessage}")
            }
        }
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
        prefs.edit().putBoolean("launch_with_chooser", enabled).apply()
    }

    fun openSettingsDialog() {
        _showSettingsDialog.value = true
    }

    fun dismissSettingsDialog() {
        _showSettingsDialog.value = false
    }

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode).apply()
    }

    fun setAppLanguage(lang: String) {
        _appLanguage.value = lang
        prefs.edit().putString("app_language", lang).apply()
    }

    fun setCurrencySymbol(symbol: String) {
        _currencySymbol.value = symbol
        prefs.edit().putString("currency_symbol", symbol).apply()
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
            val docId = java.util.UUID.randomUUID().toString()
            val user = _currentUser.value
            val household = _currentHousehold.value
            val firebaseExpense = FirebaseExpense(
                id = docId,
                householdId = household?.id ?: "",
                title = title,
                amount = amount,
                categoryId = categoryId,
                categoryName = categoryName,
                timestamp = timestamp,
                splitType = splitType,
                memberIdsCsv = memberIds.joinToString(","),
                memberNamesDisplay = memberNamesDisplay,
                note = "",
                createdByUid = user?.uid ?: "",
                createdByName = user?.displayName ?: "Χρήστης"
            )

            if (user != null) {
                firestoreSyncRepo.syncUserExpense(user.uid, firebaseExpense)
            }
            if (household != null && household.id.isNotBlank()) {
                firestoreSyncRepo.syncExpense(firebaseExpense)
            }
            showSnackbar("Καταχωρήθηκε στη βάση: $title (${String.format(java.util.Locale.US, "%.2f€", amount)})")
        }
    }

    fun confirmPendingExpense() {
        val pending = _pendingParsedExpense.value ?: return
        confirmVoiceExpense(
            title = pending.title,
            amount = pending.amount,
            categoryId = pending.categoryId,
            categoryName = pending.categoryName,
            timestamp = pending.timestamp,
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
            timestamp = pending.timestamp,
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
            val user = _currentUser.value
            val household = _currentHousehold.value
            val targetHouseholdId = household?.id ?: ""
            val targetUserId = user?.uid ?: ""

            val existing = _editingExpense.value
            val docId = if (!existing?.cloudId.isNullOrBlank()) {
                existing!!.cloudId
            } else if (!existing?.originalVoicePrompt.isNullOrBlank()) {
                existing!!.originalVoicePrompt
            } else {
                java.util.UUID.randomUUID().toString()
            }

            val firebaseExpense = FirebaseExpense(
                id = docId,
                householdId = targetHouseholdId,
                title = title,
                amount = amount,
                categoryId = categoryId,
                categoryName = categoryName,
                timestamp = timestamp,
                splitType = splitType,
                memberIdsCsv = memberIds.joinToString(","),
                memberNamesDisplay = memberNamesDisplay,
                note = note,
                createdByUid = targetUserId,
                createdByName = user?.displayName ?: "Χρήστης"
            )

            // Direct save to Cloud Firestore - NO data saved on device storage
            if (targetHouseholdId.isNotBlank()) {
                firestoreSyncRepo.syncExpense(firebaseExpense)
            }
            if (targetUserId.isNotBlank()) {
                firestoreSyncRepo.syncUserExpense(targetUserId, firebaseExpense)
            }
            showSnackbar(if (existing == null) "Προστέθηκε νέο έξοδο στη βάση: $title" else "Το έξοδο ενημερώθηκε στη βάση")
            dismissManualExpenseDialog()
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            val user = _currentUser.value
            val household = _currentHousehold.value
            val docId = if (expense.cloudId.isNotBlank()) expense.cloudId else expense.originalVoicePrompt

            if (user != null && docId.isNotBlank()) {
                firestoreSyncRepo.deleteUserExpense(user.uid, docId)
            }
            if (household != null && household.id.isNotBlank() && docId.isNotBlank()) {
                firestoreSyncRepo.deleteExpense(household.id, docId)
            }
            showSnackbar("Το έξοδο διαγράφηκε από τη βάση")
        }
    }

    // Income CRUD
    fun openManualIncomeDialog(income: Income? = null) {
        _editingIncome.value = income
        _showManualIncomeDialog.value = true
    }

    fun dismissManualIncomeDialog() {
        _editingIncome.value = null
        _showManualIncomeDialog.value = false
    }

    fun saveIncome(
        id: Long = 0,
        title: String,
        amount: Double,
        memberId: Long,
        memberName: String,
        category: String,
        timestamp: Long,
        note: String
    ) {
        viewModelScope.launch {
            val user = _currentUser.value
            val household = _currentHousehold.value
            val targetHouseholdId = household?.id ?: ""
            val targetUserId = user?.uid ?: ""

            val existing = _editingIncome.value
            val docId = if (!existing?.cloudId.isNullOrBlank()) {
                existing!!.cloudId
            } else {
                java.util.UUID.randomUUID().toString()
            }

            val firebaseIncome = FirebaseIncome(
                id = docId,
                householdId = targetHouseholdId,
                title = title,
                amount = amount,
                memberId = memberId,
                memberName = memberName,
                category = category,
                timestamp = timestamp,
                note = note,
                createdByUid = targetUserId,
                createdByName = user?.displayName ?: "Χρήστης"
            )

            if (targetHouseholdId.isNotBlank()) {
                firestoreSyncRepo.syncIncome(firebaseIncome)
            }
            if (targetUserId.isNotBlank()) {
                firestoreSyncRepo.syncUserIncome(targetUserId, firebaseIncome)
            }
            showSnackbar(if (existing == null) "Προστέθηκε νέο έσοδο: $title (+${String.format(java.util.Locale.US, "%.2f€", amount)})" else "Το έσοδο ενημερώθηκε")
            dismissManualIncomeDialog()
        }
    }

    fun deleteIncome(income: Income) {
        viewModelScope.launch {
            val user = _currentUser.value
            val household = _currentHousehold.value
            val docId = income.cloudId

            if (user != null && docId.isNotBlank()) {
                firestoreSyncRepo.deleteUserIncome(user.uid, docId)
            }
            if (household != null && household.id.isNotBlank() && docId.isNotBlank()) {
                firestoreSyncRepo.deleteIncome(household.id, docId)
            }
            showSnackbar("Το έσοδο διαγράφηκε")
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

    fun updateMemberColor(member: FamilyMember, newColorHex: Long) {
        viewModelScope.launch {
            repository.updateMember(member.copy(avatarColorHex = newColorHex))
            showSnackbar("Το χρώμα για ${member.name} ενημερώθηκε")
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
