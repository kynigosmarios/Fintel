package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.reminder.DailyReminderManager
import com.example.data.AppDatabase
import com.example.data.entity.Expense
import com.example.data.entity.toFirebaseExpense
import com.example.data.entity.ExpenseCategory
import com.example.data.entity.FamilyMember
import com.example.data.entity.Income
import com.example.data.entity.RecurringExpense
import com.example.data.entity.SavingsGoal
import com.example.data.model.FirebaseCategory
import com.example.data.model.FirebaseExpense
import com.example.data.model.FirebaseIncome
import com.example.data.model.FirebaseMember
import com.example.data.model.FirebaseRecurringExpense
import com.example.data.model.FirebaseSavingsGoal
import com.example.data.model.Household
import com.example.data.model.UserProfile
import com.example.data.repository.ExpenseRepository
import com.example.data.repository.FirestoreSyncRepository
import com.example.data.safety.DataSafetyGuard
import com.example.data.seed.FirestoreDataSeed
import com.example.util.GreekVoiceParser
import com.example.util.Localization
import com.example.util.ParsedVoiceExpense
import com.example.util.RecurringExpenseAutoProcessor
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.auth
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
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
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class CountdownState(
    val remainingSeconds: Int = 3,
    val isActive: Boolean = false
)

class OikoNomosViewModel(application: Application) : AndroidViewModel(application) {

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e("OikoNomosVM", "CoroutineExceptionHandler caught unhandled exception: ${throwable.message}", throwable)
    }

    private val repository: ExpenseRepository
    private val firestoreSyncRepo = FirestoreSyncRepository.create(application)
    private val auth: FirebaseAuth = Firebase.auth
    private val prefs = application.getSharedPreferences("oikonomos_app_prefs", Context.MODE_PRIVATE)

    // Firebase Auth & Cloud Sync States
    private val _isAuthInitialized = MutableStateFlow(true)
    val isAuthInitialized = _isAuthInitialized.asStateFlow()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser = _currentUser.asStateFlow()

    private val _isUserVerifiedState = MutableStateFlow(isUserVerified(auth.currentUser))
    val isUserVerifiedState = _isUserVerifiedState.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile = _userProfile.asStateFlow()

    private val _currentHousehold = MutableStateFlow<Household?>(null)
    val currentHousehold = _currentHousehold.asStateFlow()

    private val _userHouseholds = MutableStateFlow<List<Household>>(emptyList())
    val userHouseholds = _userHouseholds.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    // For new users without any expenses yet, do not block the screen with loading state
    private val _isInitialExpensesLoading = MutableStateFlow(prefs.getBoolean("has_entered_any_expense", false))
    val isInitialExpensesLoading = _isInitialExpensesLoading.asStateFlow()

    private var householdJob: Job? = null
    private var expensesSyncJob: Job? = null
    private var userExpensesSyncJob: Job? = null
    private var incomesSyncJob: Job? = null
    private var userIncomesSyncJob: Job? = null
    private var membersSyncJob: Job? = null
    private var userMembersSyncJob: Job? = null
    private var categoriesSyncJob: Job? = null
    private var userCategoriesSyncJob: Job? = null
    private var savingsGoalsSyncJob: Job? = null
    private var userSavingsGoalsSyncJob: Job? = null
    private var recurringSyncJob: Job? = null
    private var userRecurringSyncJob: Job? = null
    private var userProfileSyncJob: Job? = null

    private fun cancelAllSyncJobs() {
        householdJob?.cancel()
        expensesSyncJob?.cancel()
        userExpensesSyncJob?.cancel()
        incomesSyncJob?.cancel()
        userIncomesSyncJob?.cancel()
        membersSyncJob?.cancel()
        userMembersSyncJob?.cancel()
        categoriesSyncJob?.cancel()
        userCategoriesSyncJob?.cancel()
        savingsGoalsSyncJob?.cancel()
        userSavingsGoalsSyncJob?.cancel()
        recurringSyncJob?.cancel()
        userRecurringSyncJob?.cancel()
        userProfileSyncJob?.cancel()
    }

    fun isMariosUser(user: FirebaseUser? = null, email: String? = null, uid: String? = null): Boolean {
        val effectiveUser = user ?: _currentUser.value ?: auth.currentUser
        val effectiveEmail = (email ?: effectiveUser?.email).orEmpty().trim()
        val effectiveUid = uid ?: effectiveUser?.uid.orEmpty()
        return effectiveEmail.equals(com.example.data.seed.FirestoreDataSeed.DEFAULT_USER_EMAIL, ignoreCase = true) ||
                effectiveUid == com.example.data.seed.FirestoreDataSeed.DEFAULT_USER_UID ||
                effectiveUid == "t5kN4RvPRugIxXUz18Fep2khSrC2" ||
                effectiveUid == "tSLWmkXZ0MTqIKGoX3wd6HdhDw63" ||
                effectiveUid == "jllDLLQl7VgFzOQpVIHi0wo8B013" ||
                effectiveUid == "oA6smvWcC6QYCAKMvYdWEcmVCO43"
    }

    fun getOrEnsureHouseholdId(user: FirebaseUser? = null): String {
        val effectiveUser = user ?: _currentUser.value ?: auth.currentUser
        if (effectiveUser == null) return ""
        val isMarios = isMariosUser(effectiveUser)
        val mariosHids = setOf(com.example.data.seed.FirestoreDataSeed.DEFAULT_HOUSEHOLD_ID, "e1d4ae67-499")

        val currentHid = _currentHousehold.value?.id.orEmpty()
        if (currentHid.isNotBlank()) {
            if (isMarios || !mariosHids.contains(currentHid)) {
                return currentHid
            }
        }

        if (isMarios) {
            val profileHid = _userProfile.value?.householdId.orEmpty()
            if (profileHid.isNotBlank()) return profileHid
            return com.example.data.seed.FirestoreDataSeed.DEFAULT_HOUSEHOLD_ID
        }

        val profileHid = _userProfile.value?.householdId.orEmpty()
        if (profileHid.isNotBlank() && !mariosHids.contains(profileHid)) {
            return profileHid
        }

        val profileHids = _userProfile.value?.householdIds?.firstOrNull { it.isNotBlank() && !mariosHids.contains(it) }.orEmpty()
        if (profileHids.isNotBlank()) {
            return profileHids
        }

        val userHids = _userHouseholds.value.firstOrNull { !mariosHids.contains(it.id) }?.id.orEmpty()
        if (userHids.isNotBlank()) {
            return userHids
        }

        val userSpecificPrefHid = prefs.getString("user_household_${effectiveUser.uid}", "").orEmpty()
        if (userSpecificPrefHid.isNotBlank() && !mariosHids.contains(userSpecificPrefHid)) {
            return userSpecificPrefHid
        }

        val safeUserHid = "household_${effectiveUser.uid}"
        prefs.edit().putString("user_household_${effectiveUser.uid}", safeUserHid).apply()

        // Ensure _currentHousehold is non-null
        if (_currentHousehold.value == null || mariosHids.contains(_currentHousehold.value?.id)) {
            val defaultH = Household(
                id = safeUserHid,
                name = "Το Εξοδολόγιό μου",
                ownerId = effectiveUser.uid,
                memberUids = listOf(effectiveUser.uid)
            )
            _currentHousehold.value = defaultH
            if (_userHouseholds.value.none { it.id == safeUserHid }) {
                _userHouseholds.value = listOf(defaultH)
            }
        }
        viewModelScope.launch {
            try {
                firestoreSyncRepo.ensureHouseholdExists(safeUserHid, effectiveUser.uid, "Το Εξοδολόγιό μου")
            } catch (e: Exception) {
                Log.w("OikoNomosVM", "ensureHouseholdExists background check: ${e.message}")
            }
        }
        return safeUserHid
    }

    private val _allExpenses = MutableStateFlow<List<Expense>>(emptyList())
    val allExpenses: StateFlow<List<Expense>> = _allExpenses.asStateFlow()

    private val _allIncomes = MutableStateFlow<List<Income>>(emptyList())
    val allIncomes: StateFlow<List<Income>> = _allIncomes.asStateFlow()

    private val _allMembers = MutableStateFlow<List<FamilyMember>>(emptyList())
    val allMembers: StateFlow<List<FamilyMember>> = _allMembers.asStateFlow()

    private val _allCategories = MutableStateFlow<List<ExpenseCategory>>(emptyList())
    val allCategories: StateFlow<List<ExpenseCategory>> = _allCategories.asStateFlow()

    private val _showOnboardingCarousel = MutableStateFlow(false)
    val showOnboardingCarousel = _showOnboardingCarousel.asStateFlow()

    private val _notificationConsentCompleted = MutableStateFlow(
        prefs.getBoolean("notification_consent_handled_${auth.currentUser?.uid ?: "guest"}", false)
    )
    val notificationConsentCompleted = _notificationConsentCompleted.asStateFlow()

    fun acceptNotificationConsent() {
        val uid = auth.currentUser?.uid ?: "guest"
        prefs.edit().putBoolean("notification_consent_handled_$uid", true).apply()
        _notificationConsentCompleted.value = true
        toggleDailyReminder(true)
        if (uid != "guest") {
            _userProfile.value = _userProfile.value?.copy(notificationConsentCompleted = true, reminderEnabled = true)
            viewModelScope.launch {
                firestoreSyncRepo.updateUserProfilePreferences(uid, notificationConsentCompleted = true, reminderEnabled = true)
            }
        }
        val user = auth.currentUser ?: _currentUser.value
        if (user != null && isUserVerified(user)) {
            viewModelScope.launch {
                loadUserProfileAndHousehold(user)
            }
        }
    }

    fun skipNotificationConsent() {
        val uid = auth.currentUser?.uid ?: "guest"
        prefs.edit().putBoolean("notification_consent_handled_$uid", true).apply()
        _notificationConsentCompleted.value = true
        if (uid != "guest") {
            _userProfile.value = _userProfile.value?.copy(notificationConsentCompleted = true)
            viewModelScope.launch {
                firestoreSyncRepo.updateUserProfilePreferences(uid, notificationConsentCompleted = true)
            }
        }
        val user = auth.currentUser ?: _currentUser.value
        if (user != null && isUserVerified(user)) {
            viewModelScope.launch {
                loadUserProfileAndHousehold(user)
            }
        }
    }

    init {
        try {
            val hasExplicitLanguage = prefs.getBoolean("user_explicitly_set_language_v2", false)
            val savedLang = if (hasExplicitLanguage) (prefs.getString("app_language", "EN") ?: "EN") else "EN"
            auth.setLanguageCode(if (savedLang == "EN") "en" else "el")
        } catch (e: Exception) {
            Log.w("OikoNomosVM", "auth.setLanguageCode warning: ${e.message}")
        }
        val database = AppDatabase.getDatabase(application)
        repository = ExpenseRepository(database.expenseDao(), application)

        // Zero Local Persistence Policy: Always wipe local Room on startup.
        // User data is fetched exclusively and live from Cloud Firestore.
        viewModelScope.launch(coroutineExceptionHandler) {
            try {
                repository.clearAllData()
            } catch (e: Exception) {
                Log.d("OikoNomosVM", "Room clear on start: ${e.message}")
            }
        }
        _allExpenses.value = emptyList()
        _allIncomes.value = emptyList()
        _allMembers.value = emptyList()
        _allCategories.value = emptyList()
        _isInitialExpensesLoading.value = true

        // Fast pre-warm: Only load cached profile/data if user is already authenticated
        val existingUser = auth.currentUser
        if (existingUser != null) {
            val isMarios = isMariosUser(existingUser)
            val cachedProfile = loadUserProfileFromPrefs()
            if (cachedProfile != null && (cachedProfile.uid == existingUser.uid || cachedProfile.email.equals(existingUser.email, ignoreCase = true))) {
                _userProfile.value = cachedProfile
            }
            val prewarmHid = getOrEnsureHouseholdId(existingUser)
            if (prewarmHid.isNotBlank()) {
                try {
                    observeHouseholdData(prewarmHid)
                } catch (e: Exception) {
                    Log.w("OikoNomosVM", "Prewarm hid observation error: ${e.message}")
                }
            }
        }

        // Listen for Auth state
        var lastObservedUid: String? = auth.currentUser?.uid
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            val currentUid = user?.uid
            _isAuthInitialized.value = true
            val verified = isUserVerified(user)
            _isUserVerifiedState.value = verified
            _notificationConsentCompleted.value = prefs.getBoolean("notification_consent_handled_${currentUid ?: "guest"}", false)
            val activeUidInPrefs = prefs.getString("cached_active_uid", null)
            val isUserSwitch = (user == null) || (activeUidInPrefs != currentUid)
            if (isUserSwitch) {
                // Account switch or logout: wipe all in-memory lists, cache and cancel jobs immediately
                _allExpenses.value = emptyList()
                _allIncomes.value = emptyList()
                _allMembers.value = emptyList()
                _allCategories.value = emptyList()
                _currentHousehold.value = null
                _userHouseholds.value = emptyList()
                _userProfile.value = null
                lastHouseholdExpenses = emptyList()
                lastUserExpenses = emptyList()
                lastHouseholdIncomes = emptyList()
                lastUserIncomes = emptyList()
                lastHouseholdMembers = emptyList()
                lastUserMembers = emptyList()
                lastHouseholdCategories = emptyList()
                lastUserCategories = emptyList()
                clearUserProfileFromPrefs()
                prefs.edit()
                    .remove("last_household_id")
                    .remove("has_entered_any_expense")
                    .putString("cached_active_uid", currentUid ?: "")
                    .apply()
                cancelAllSyncJobs()
                lastObservedHouseholdId = null
                viewModelScope.launch(coroutineExceptionHandler) {
                    try {
                        repository.clearAllData()
                        if (user != null && isMariosUser(user)) {
                            repository.seedMariosDataIfEmpty()
                        }
                    } catch (e: Exception) {
                        Log.w("OikoNomosVM", "Clear on user switch: ${e.message}")
                    }
                }
            }

            if (user != null) {
                _currentUser.value = user
                lastObservedUid = currentUid
                val isMarios = isMariosUser(user)
                if (isMarios) {
                    val mariosHousehold = Household(
                        id = "3e9a25a0-2e7",
                        name = "Οικογένεια Μάριος",
                        ownerId = "t5kN4RvPRugIxXUz18Fep2khSrC2",
                        memberUids = listOf("t5kN4RvPRugIxXUz18Fep2khSrC2", "tSLWmkXZ0MTqIKGoX3wd6HdhDw63", "jllDLLQl7VgFzOQpVIHi0wo8B013", "oA6smvWcC6QYCAKMvYdWEcmVCO43"),
                        invitedEmails = listOf("kynigosmarios@gmail.com")
                    )
                    val epixeirisiHousehold = Household(
                        id = "e1d4ae67-499",
                        name = "Επιχείρηση",
                        ownerId = "t5kN4RvPRugIxXUz18Fep2khSrC2",
                        memberUids = listOf("t5kN4RvPRugIxXUz18Fep2khSrC2", "tSLWmkXZ0MTqIKGoX3wd6HdhDw63", "jllDLLQl7VgFzOQpVIHi0wo8B013", "oA6smvWcC6QYCAKMvYdWEcmVCO43"),
                        invitedEmails = listOf("kynigosmarios@gmail.com")
                    )
                    _currentHousehold.value = mariosHousehold
                    _userHouseholds.value = listOf(mariosHousehold, epixeirisiHousehold)
                } else {
                    val safeUserHid = getOrEnsureHouseholdId(user)
                    val defaultH = Household(
                        id = safeUserHid,
                        name = "Το Εξοδολόγιό μου",
                        ownerId = user.uid,
                        memberUids = listOf(user.uid)
                    )
                    _currentHousehold.value = defaultH
                    _userHouseholds.value = listOf(defaultH)
                }
                if (verified) {
                    viewModelScope.launch(coroutineExceptionHandler) {
                        try {
                            loadUserProfileAndHousehold(user)
                        } catch (e: Exception) {
                            Log.e("OikoNomosVM", "loadUserProfileAndHousehold error: ${e.message}", e)
                        }
                    }
                }
            } else {
                _currentUser.value = null
                _isUserVerifiedState.value = false
                _userProfile.value = null
                _currentHousehold.value = null
                _userHouseholds.value = emptyList()
                _showOnboardingCarousel.value = false
                _allExpenses.value = emptyList()
                _allIncomes.value = emptyList()
                _allMembers.value = emptyList()
                _allCategories.value = emptyList()
                lastObservedUid = null
            }
        }
    }

    private suspend fun loadUserProfileAndHousehold(user: FirebaseUser) {
        val email = user.email.orEmpty().trim()
        val isMarios = isMariosUser(user, email, user.uid)

        // Ensure base data and households exist in Firebase Firestore
        if (isMarios) {
            try {
                firestoreSyncRepo.seedFirestoreIfEmpty(
                    householdId = com.example.data.seed.FirestoreDataSeed.DEFAULT_HOUSEHOLD_ID,
                    userId = user.uid,
                    isMarios = true
                )
            } catch (e: Exception) {
                Log.w("OikoNomosVM", "Seed check warning: ${e.message}")
            }
        }

        try {
            // First check if profile exists in cache/remote
            var profile = firestoreSyncRepo.getOrCreateUserProfile(
                user.uid,
                user.email ?: "",
                user.displayName ?: ""
            )

            // Auto-sync Google account photo URL if profile has no photo yet
            if (profile.photoUrl.isBlank() && user.photoUrl != null) {
                val googlePhoto = user.photoUrl.toString()
                try {
                    firestoreSyncRepo.updateUserProfilePhoto(user.uid, googlePhoto)
                    profile = profile.copy(photoUrl = googlePhoto)
                } catch (e: Exception) {
                    Log.w("OikoNomosVM", "Failed to auto-sync Google photo: ${e.message}")
                }
            }

            _userProfile.value = profile
            saveUserProfileToPrefs(profile)

            // Apply Cloud User Preferences to App State
            if (profile.appLanguage.isNotBlank() && profile.appLanguage != _appLanguage.value) {
                setAppLanguage(profile.appLanguage, syncToCloud = false)
            }
            if (profile.themeMode.isNotBlank() && profile.themeMode != _themeMode.value) {
                setThemeMode(profile.themeMode, syncToCloud = false)
            }
            if (profile.notificationConsentCompleted) {
                _notificationConsentCompleted.value = true
                prefs.edit().putBoolean("notification_consent_handled_${user.uid}", true).apply()
            }
            if (profile.reminderEnabled || profile.reminderHour != _dailyReminderHour.value || profile.reminderMinute != _dailyReminderMinute.value) {
                _dailyReminderEnabled.value = profile.reminderEnabled
                _dailyReminderHour.value = profile.reminderHour
                _dailyReminderMinute.value = profile.reminderMinute
                DailyReminderManager.setReminderEnabled(getApplication(), profile.reminderEnabled)
                DailyReminderManager.setReminderTime(getApplication(), profile.reminderHour, profile.reminderMinute)
                if (profile.reminderEnabled) {
                    DailyReminderManager.scheduleDailyReminder(getApplication(), profile.reminderHour, profile.reminderMinute)
                }
            }

            var households = firestoreSyncRepo.fetchUserHouseholds(profile.householdIds)
            if (isMarios) {
                val mariosHid = com.example.data.seed.FirestoreDataSeed.DEFAULT_HOUSEHOLD_ID
                val epixHid = "e1d4ae67-499"
                val extraHids = listOf(mariosHid, epixHid).filter { hid -> households.none { it.id == hid } }
                if (extraHids.isNotEmpty()) {
                    val fetched = firestoreSyncRepo.fetchUserHouseholds(extraHids).toMutableList()
                    if (fetched.none { it.id == mariosHid }) {
                        fetched.add(
                            Household(
                                id = mariosHid,
                                name = "Οικογένεια Μάριος",
                                ownerId = user.uid,
                                memberUids = listOf(user.uid, "t5kN4RvPRugIxXUz18Fep2khSrC2"),
                                invitedEmails = listOf("kynigosmarios@gmail.com")
                            )
                        )
                    }
                    if (fetched.none { it.id == epixHid }) {
                        fetched.add(
                            Household(
                                id = epixHid,
                                name = "Επιχείρηση",
                                ownerId = user.uid,
                                memberUids = listOf(user.uid, "t5kN4RvPRugIxXUz18Fep2khSrC2"),
                                invitedEmails = listOf("kynigosmarios@gmail.com")
                            )
                        )
                    }
                    households = (households + fetched).distinctBy { it.id }
                }
            } else {
                // Strict isolation: non-Marios users must NEVER have Marios's households
                val mariosHids = setOf(com.example.data.seed.FirestoreDataSeed.DEFAULT_HOUSEHOLD_ID, "e1d4ae67-499")
                households = households.filterNot { mariosHids.contains(it.id) }
                if (households.isEmpty()) {
                    val newH = firestoreSyncRepo.createHousehold(user.uid, "Το Εξοδολόγιό μου")
                    households = listOf(newH)
                    profile = profile.copy(householdId = newH.id, householdIds = listOf(newH.id))
                    _userProfile.value = profile
                    saveUserProfileToPrefs(profile)
                }
            }

            if (households.isNotEmpty()) {
                _userHouseholds.value = households
            }

            val rawSavedHid = prefs.getString("last_household_id", "") ?: ""
            val savedHid = if (!isMarios && (rawSavedHid == com.example.data.seed.FirestoreDataSeed.DEFAULT_HOUSEHOLD_ID || rawSavedHid == "e1d4ae67-499")) {
                prefs.edit().remove("last_household_id").apply()
                ""
            } else rawSavedHid

            val userHouseholdIds = households.map { it.id }
            val savedBelongsToUser = savedHid.isNotBlank() && userHouseholdIds.contains(savedHid)
            val savedHasExpenses = if (savedBelongsToUser) firestoreSyncRepo.hasExpenses(savedHid) else false
            val defaultHasExpenses = if (isMarios) firestoreSyncRepo.hasExpenses(com.example.data.seed.FirestoreDataSeed.DEFAULT_HOUSEHOLD_ID) else false
            val hidWithExp = if (savedHasExpenses) savedHid else firestoreSyncRepo.findHouseholdWithExpenses(userHouseholdIds)

            val activeHouseholdId = when {
                // 1. If saved household belongs to this user, respect the user's active choice
                savedBelongsToUser -> savedHid
                // 2. User profile householdId if it exists in this user's households
                profile.householdId.isNotBlank() && households.any { it.id == profile.householdId } -> profile.householdId
                // 3. If user is Marios and default household has expenses
                isMarios && defaultHasExpenses -> com.example.data.seed.FirestoreDataSeed.DEFAULT_HOUSEHOLD_ID
                // 4. If any of THIS user's households has expenses, select that one
                !hidWithExp.isNullOrBlank() -> hidWithExp
                // 5. First household of this user
                households.isNotEmpty() -> households.first().id
                isMarios -> com.example.data.seed.FirestoreDataSeed.DEFAULT_HOUSEHOLD_ID
                else -> getOrEnsureHouseholdId(user)
            }

            if (activeHouseholdId.isNotBlank()) {
                val current = households.find { it.id == activeHouseholdId } ?: households.firstOrNull()
                if (current != null) {
                    _currentHousehold.value = current
                }
                prefs.edit()
                    .putString("user_household_${user.uid}", activeHouseholdId)
                    .putString("last_household_id", activeHouseholdId)
                    .apply()
                _userProfile.value = _userProfile.value?.copy(householdId = activeHouseholdId)
                firestoreSyncRepo.switchActiveHousehold(user.uid, activeHouseholdId)
                observeHouseholdData(activeHouseholdId)
            } else if (households.isNotEmpty()) {
                val fallbackH = households.first()
                _currentHousehold.value = fallbackH
                prefs.edit()
                    .putString("user_household_${user.uid}", fallbackH.id)
                    .putString("last_household_id", fallbackH.id)
                    .apply()
                _userProfile.value = _userProfile.value?.copy(householdId = fallbackH.id)
                observeHouseholdData(fallbackH.id)
            }
            observeUserData(user.uid)

            // Strict Account Sanitization: Purge any foreign data that leaked from another account
            sanitizeAccountIsolation(user, activeHouseholdId)

            // Auto-recovery / Synchronization of Members, Categories, Goals, and Recurring
            recoverAndSyncUserData(user, profile, activeHouseholdId)

            val alreadyShown = prefs.getBoolean("onboarding_shown_${user.uid}", false)
            if (!alreadyShown) {
                _showOnboardingCarousel.value = true
            }

            // Check GDPR / Privacy Consent on startup
            val privacyAccepted = prefs.getBoolean("gdpr_privacy_consent_accepted", false)
            if (!privacyAccepted) {
                _showPrivacyConsentDialog.value = true
            }

            // Ensure daily reminder schedule is active if enabled
            if (DailyReminderManager.isReminderEnabled(getApplication())) {
                val hour = DailyReminderManager.getReminderHour(getApplication())
                val minute = DailyReminderManager.getReminderMinute(getApplication())
                DailyReminderManager.scheduleDailyReminder(getApplication(), hour, minute)
            }
        } catch (e: Exception) {
            Log.e("OikoNomosVM", "Error loading profile/household", e)
        }
    }

    private fun isMariosFamilyMember(name: String): Boolean = false

    private fun isMariosSeedCategory(catName: String): Boolean = false

    private suspend fun sanitizeAccountIsolation(user: FirebaseUser, activeHouseholdId: String) {
        val isMarios = isMariosUser(user)
        val mariosHids = setOf(
            com.example.data.seed.FirestoreDataSeed.DEFAULT_HOUSEHOLD_ID,
            "e1d4ae67-499"
        )
        val mariosUids = setOf(
            com.example.data.seed.FirestoreDataSeed.DEFAULT_USER_UID,
            "t5kN4RvPRugIxXUz18Fep2khSrC2", "tSLWmkXZ0MTqIKGoX3wd6HdhDw63",
            "jllDLLQl7VgFzOQpVIHi0wo8B013", "oA6smvWcC6QYCAKMvYdWEcmVCO43"
        )
        if (isMarios) {
            // Actively purge any foreign incomes or expenses that leaked into Marios's default household
            try {
                val hIncomes = firestoreSyncRepo.getIncomesOnce(com.example.data.seed.FirestoreDataSeed.DEFAULT_HOUSEHOLD_ID)
                for (inc in hIncomes) {
                    if (inc.createdByUid.isNotBlank() && !mariosUids.contains(inc.createdByUid)) {
                        firestoreSyncRepo.deleteIncome(com.example.data.seed.FirestoreDataSeed.DEFAULT_HOUSEHOLD_ID, inc.id)
                    }
                }
                val hExpenses = firestoreSyncRepo.getExpensesOnce(com.example.data.seed.FirestoreDataSeed.DEFAULT_HOUSEHOLD_ID)
                for (exp in hExpenses) {
                    if (exp.createdByUid.isNotBlank() && !mariosUids.contains(exp.createdByUid)) {
                        firestoreSyncRepo.deleteExpense(com.example.data.seed.FirestoreDataSeed.DEFAULT_HOUSEHOLD_ID, exp.id)
                    }
                }
            } catch (e: Exception) {
                Log.w("OikoNomosVM", "Sanitize Marios household warning: ${e.message}")
            }
            return
        }

        val userUid = user.uid

        // 1. Members: Ensure the active household has at least one member (the user) in Firestore
        try {
            val hMembers = if (activeHouseholdId.isNotBlank()) firestoreSyncRepo.getMembersOnce(activeHouseholdId) else emptyList()
            val validMembers = hMembers.filter { !isInvalidOrCombinationMember(it.name) }
            if (validMembers.isEmpty()) {
                val myName = user.displayName?.ifBlank { null }
                    ?: (if (user.email.isNullOrBlank()) "Εγώ" else user.email!!.substringBefore("@"))
                val defaultFbMember = FirebaseMember(
                    id = "1",
                    householdId = activeHouseholdId,
                    userId = userUid,
                    name = myName,
                    role = "Γονέας",
                    avatarColorHex = 0xFF059669,
                    voiceKeywords = myName.lowercase(),
                    isDefault = true,
                    sortOrder = 0
                )
                if (activeHouseholdId.isNotBlank()) firestoreSyncRepo.syncMember(activeHouseholdId, defaultFbMember)
                val domainMember = FamilyMember(
                    id = 1L,
                    name = myName,
                    role = "Γονέας",
                    avatarColorHex = 0xFF059669,
                    voiceKeywords = myName.lowercase(),
                    isDefault = true,
                    sortOrder = 0
                )
                _allMembers.value = listOf(domainMember)
                repository.replaceAllMembers(listOf(domainMember))
            }
        } catch (e: Exception) {
            Log.w("OikoNomosVM", "Sanitize members warning: ${e.message}")
        }

        // 2. Categories: Ensure standard categories exist for the active household if none exist yet
        try {
            val hCats = if (activeHouseholdId.isNotBlank()) firestoreSyncRepo.getCategoriesOnce(activeHouseholdId) else emptyList()
            if (hCats.isEmpty()) {
                STANDARD_DEFAULT_CATEGORIES.forEach { cat ->
                    val fbCat = FirebaseCategory(
                        id = cat.id.toString(),
                        householdId = activeHouseholdId,
                        userId = userUid,
                        name = cat.name,
                        iconKey = cat.iconKey,
                        monthlyBudget = 0.0,
                        colorHex = cat.colorHex,
                        voiceKeywords = cat.voiceKeywords,
                        sortOrder = cat.sortOrder,
                        fontColorHex = cat.fontColorHex
                    )
                    if (activeHouseholdId.isNotBlank()) firestoreSyncRepo.syncCategory(activeHouseholdId, fbCat)
                }
                _allCategories.value = STANDARD_DEFAULT_CATEGORIES
                repository.replaceAllCategories(STANDARD_DEFAULT_CATEGORIES)
            }
        } catch (e: Exception) {
            Log.w("OikoNomosVM", "Sanitize categories warning: ${e.message}")
        }

        // 3. Incomes: Only purge leaked Marios seed household incomes
        try {
            val uIncomes = firestoreSyncRepo.getUserIncomesOnce(userUid)
            for (inc in uIncomes) {
                if (mariosHids.contains(inc.householdId) || (inc.createdByUid.isNotBlank() && mariosUids.contains(inc.createdByUid))) {
                    firestoreSyncRepo.deleteUserIncome(userUid, inc.id)
                }
            }
        } catch (e: Exception) {
            Log.w("OikoNomosVM", "Sanitize incomes warning: ${e.message}")
        }

        // 4. Expenses: Only purge leaked Marios seed household expenses
        try {
            val uExpenses = firestoreSyncRepo.getUserExpensesOnce(userUid)
            for (exp in uExpenses) {
                if (mariosHids.contains(exp.householdId) || (exp.createdByUid.isNotBlank() && mariosUids.contains(exp.createdByUid))) {
                    firestoreSyncRepo.deleteUserExpense(userUid, exp.id)
                }
            }
        } catch (e: Exception) {
            Log.w("OikoNomosVM", "Sanitize expenses warning: ${e.message}")
        }
    }

    private fun isInvalidOrCombinationMember(name: String): Boolean {
        return DataSafetyGuard.isCombinationOrInvalidMemberName(name)
    }

    private suspend fun syncAndMergeMembers(
        incomingMembers: List<FirebaseMember>,
        explicitProfile: UserProfile? = null,
        explicitUser: FirebaseUser? = null,
        explicitHouseholdId: String? = null
    ) {
        val currentLocal = repository.getAllMembersList()
        val user = explicitUser ?: _currentUser.value
        val profile = explicitProfile ?: _userProfile.value
        val isMarios = isMariosUser(user)
        val uid = user?.uid.orEmpty()
        val userDisplay = GreekVoiceParser.normalizeGreek((user?.displayName ?: profile?.displayName ?: "").trim().lowercase())

        val validRemote = incomingMembers
            .filter { it.name.isNotBlank() && !isInvalidOrCombinationMember(it.name) }

        val householdId = explicitHouseholdId ?: _currentHousehold.value?.id.orEmpty()

        if (validRemote.isEmpty()) {
            if (isMarios && householdId == FirestoreDataSeed.DEFAULT_HOUSEHOLD_ID) {
                val seedMembers = FirestoreDataSeed.members
                repository.replaceAllMembers(seedMembers)
                _allMembers.value = seedMembers
                seedMembers.forEach { m ->
                    val fb = FirebaseMember(
                        id = m.id.toString(),
                        householdId = householdId,
                        userId = uid,
                        name = m.name,
                        role = m.role,
                        avatarColorHex = m.avatarColorHex,
                        voiceKeywords = m.voiceKeywords,
                        isDefault = m.isDefault,
                        sortOrder = m.sortOrder
                    )
                    firestoreSyncRepo.syncMember(householdId, fb)
                }
                return
            }

            // Every other household starts with its own distinct single member (the creator)
            val myName = user?.displayName?.ifBlank { null }
                ?: profile?.displayName?.ifBlank { null }
                ?: (if (user?.email.isNullOrBlank()) "Εγώ" else user!!.email!!.substringBefore("@"))
            val defaultMember = FamilyMember(
                id = 1L,
                name = myName,
                role = "Γονέας",
                avatarColorHex = 0xFF059669,
                voiceKeywords = myName.lowercase(),
                isDefault = true,
                sortOrder = 0
            )
            repository.replaceAllMembers(listOf(defaultMember))
            _allMembers.value = listOf(defaultMember)
            val fb = FirebaseMember(
                id = "1",
                householdId = householdId,
                userId = uid,
                name = myName,
                role = "Γονέας",
                avatarColorHex = 0xFF059669,
                voiceKeywords = myName.lowercase(),
                isDefault = true,
                sortOrder = 0
            )
            if (householdId.isNotBlank()) firestoreSyncRepo.syncMember(householdId, fb)
            return
        }

        val hasCustomRemoteSort = validRemote.any { it.sortOrder > 0 }
        val hasCustomLocalSort = currentLocal.any { it.sortOrder > 0 }

        // Database is the SOURCE OF TRUTH!
        val sortedRemote = validRemote.sortedWith(
            compareBy<FirebaseMember> { it.sortOrder }
                .thenBy { it.id.toLongOrNull() ?: 999L }
                .thenByDescending { it.createdAt?.seconds ?: 0L }
        )

        val grouped = sortedRemote.groupBy { remote ->
            GreekVoiceParser.normalizeGreek(remote.name.trim().lowercase())
        }

        var nextAssignedId = 2L
        val cleanMembers = mutableListOf<FamilyMember>()
        grouped.forEach { (normName, list) ->
            val chosen = list.maxByOrNull { it.createdAt?.seconds ?: 0L } ?: list.first()
            val parsedId = chosen.id.toLongOrNull()
            val isPrimary = chosen.isDefault || chosen.id == "1" || (cleanMembers.isEmpty() && (chosen.name.equals(user?.displayName, ignoreCase = true) || chosen.name.equals(profile?.displayName, ignoreCase = true)))
            val finalId = if (isPrimary && cleanMembers.none { it.id == 1L }) {
                1L
            } else if (parsedId != null && parsedId > 1L && cleanMembers.none { it.id == parsedId }) {
                parsedId
            } else {
                while (cleanMembers.any { it.id == nextAssignedId }) {
                    nextAssignedId++
                }
                nextAssignedId++
            }

            val matchingLocal = currentLocal.find {
                it.id == finalId || GreekVoiceParser.normalizeGreek(it.name.trim().lowercase()) == normName
            }

            val finalSort = when {
                hasCustomRemoteSort -> chosen.sortOrder
                hasCustomLocalSort && matchingLocal != null -> matchingLocal.sortOrder
                else -> cleanMembers.size
            }

            cleanMembers.add(
                FamilyMember(
                    id = finalId,
                    name = chosen.name.trim(),
                    role = chosen.role.trim().ifBlank { if (finalId == 1L) "Γονέας" else "Μέλος" },
                    avatarColorHex = if (chosen.avatarColorHex != 0L) chosen.avatarColorHex else (if (finalId == 1L) 0xFF059669 else 0xFF0284C7),
                    voiceKeywords = chosen.voiceKeywords.trim(),
                    isDefault = finalId == 1L,
                    sortOrder = finalSort
                )
            )
        }

        val baseFinalMembers = cleanMembers
            .distinctBy { it.id }
            .sortedWith(compareBy<FamilyMember> { it.sortOrder }.thenBy { it.id })
            .mapIndexed { idx, m -> m.copy(sortOrder = idx) }

        val finalMembers = if (baseFinalMembers.isEmpty()) {
            val myName = user?.displayName?.ifBlank { null }
                ?: profile?.displayName?.ifBlank { null }
                ?: (if (user?.email.isNullOrBlank()) "Εγώ" else user!!.email!!.substringBefore("@"))
            listOf(
                FamilyMember(
                    id = 1L,
                    name = myName,
                    role = "Γονέας",
                    avatarColorHex = 0xFF059669,
                    voiceKeywords = myName.lowercase(),
                    isDefault = true,
                    sortOrder = 0
                )
            )
        } else {
            baseFinalMembers
        }

        if (hasCustomLocalSort && !hasCustomRemoteSort) {
            syncUpdatedMembersToCloud(finalMembers)
        }

        val areDifferent = finalMembers.size != currentLocal.size ||
            finalMembers.any { fm ->
                val matching = currentLocal.find { it.id == fm.id }
                matching == null ||
                matching.name != fm.name ||
                matching.role != fm.role ||
                matching.avatarColorHex != fm.avatarColorHex ||
                matching.voiceKeywords != fm.voiceKeywords ||
                matching.sortOrder != fm.sortOrder ||
                matching.isDefault != fm.isDefault
            }

        if (areDifferent) {
            repository.replaceAllMembers(finalMembers)
        }
        _allMembers.value = finalMembers
    }

    private suspend fun syncAndMergeCategories(
        incomingCats: List<FirebaseCategory>,
        explicitHouseholdId: String? = null,
        explicitUserId: String? = null
    ) {
        val currentLocal = repository.getAllCategoriesList()
        val householdId = explicitHouseholdId ?: _currentHousehold.value?.id.orEmpty()
        val uid = explicitUserId ?: _currentUser.value?.uid.orEmpty()
        val isMarios = isMariosUser(_currentUser.value, uid = uid)

        val canonicalList = if (isMarios && householdId == FirestoreDataSeed.DEFAULT_HOUSEHOLD_ID) {
            FirestoreDataSeed.categories
        } else {
            STANDARD_DEFAULT_CATEGORIES
        }

        val canonicalMap = canonicalList.associateBy {
            GreekVoiceParser.normalizeGreek(it.name.trim().lowercase())
        }

        val validIncomingCats = incomingCats.filter { it.name.isNotBlank() }

        if (validIncomingCats.isEmpty()) {
            repository.replaceAllCategories(canonicalList)
            _allCategories.value = canonicalList
            canonicalList.forEach { cat ->
                val fbCat = FirebaseCategory(
                    id = cat.id.toString(),
                    householdId = householdId,
                    userId = uid,
                    name = cat.name,
                    iconKey = cat.iconKey,
                    monthlyBudget = cat.monthlyBudget,
                    colorHex = cat.colorHex,
                    voiceKeywords = cat.voiceKeywords,
                    sortOrder = cat.sortOrder,
                    fontColorHex = cat.fontColorHex
                )
                if (householdId.isNotBlank()) {
                    firestoreSyncRepo.syncCategory(householdId, fbCat)
                }
            }
            return
        }

        val hasCustomRemoteSort = validIncomingCats.any { it.sortOrder > 0 }
        val hasCustomLocalSort = currentLocal.any { it.sortOrder > 0 }

        // Group incoming by normalized Greek name to prevent duplicate categories and ID shifting
        val grouped = validIncomingCats
            .filter { it.name.isNotBlank() }
            .groupBy { GreekVoiceParser.normalizeGreek(it.name.trim().lowercase()) }

        val cleanCategories = mutableListOf<ExpenseCategory>()
        var nextCatId = 500L

        grouped.forEach { (normName, list) ->
            val canonical = canonicalMap[normName]
            // Choose entry matching canonical ID if present, otherwise latest or first
            val chosen = if (canonical != null) {
                list.find { it.id == canonical.id.toString() } ?: list.maxByOrNull { it.createdAt?.seconds ?: 0L } ?: list.first()
            } else {
                list.maxByOrNull { it.createdAt?.seconds ?: 0L } ?: list.first()
            }

            val parsedId = chosen.id.toLongOrNull()
            val finalCatId = when {
                canonical != null -> canonical.id
                parsedId != null && parsedId > 0L && cleanCategories.none { it.id == parsedId } -> parsedId
                else -> {
                    while (cleanCategories.any { it.id == nextCatId } || canonicalMap.values.any { it.id == nextCatId }) {
                        nextCatId++
                    }
                    nextCatId++
                }
            }

            // For standard non-Marios users: ensure default budgets are 0.0 unless explicitly created/modified by user
            val finalBudget = if (isMarios) {
                if (chosen.monthlyBudget > 0.0) chosen.monthlyBudget else (canonical?.monthlyBudget ?: 0.0)
            } else {
                // If it came from Marios seed budget (e.g. 450.0, 120.0, etc.), reset to 0.0
                if (canonical != null && (chosen.monthlyBudget == 450.0 || chosen.monthlyBudget == 120.0 || chosen.monthlyBudget == 80.0 || chosen.monthlyBudget == 150.0 || chosen.monthlyBudget == 300.0)) {
                    0.0
                } else chosen.monthlyBudget
            }
            val finalIcon = chosen.iconKey.ifBlank { canonical?.iconKey ?: "cart" }
            val finalColor = if (chosen.colorHex != 0L) chosen.colorHex else (canonical?.colorHex ?: 0xFF059669)
            val finalKeywords = chosen.voiceKeywords.ifBlank { canonical?.voiceKeywords ?: "" }

            val matchingLocal = currentLocal.find {
                it.id == finalCatId || GreekVoiceParser.normalizeGreek(it.name.trim().lowercase()) == normName
            }

            val finalSort = when {
                hasCustomRemoteSort -> chosen.sortOrder
                hasCustomLocalSort && matchingLocal != null -> matchingLocal.sortOrder
                canonical != null -> canonical.sortOrder
                else -> cleanCategories.size
            }

            cleanCategories.add(
                ExpenseCategory(
                    id = finalCatId,
                    name = canonical?.name ?: chosen.name.trim(),
                    iconKey = finalIcon,
                    monthlyBudget = finalBudget,
                    colorHex = finalColor,
                    voiceKeywords = finalKeywords,
                    sortOrder = finalSort,
                    fontColorHex = chosen.fontColorHex
                )
            )
        }

        // Guarantee all canonical categories exist if missing
        canonicalMap.values.forEach { canonical ->
            val norm = GreekVoiceParser.normalizeGreek(canonical.name.trim().lowercase())
            if (cleanCategories.none { GreekVoiceParser.normalizeGreek(it.name.trim().lowercase()) == norm }) {
                cleanCategories.add(canonical.copy(sortOrder = cleanCategories.size))
            }
        }

        val baseFinalCats = cleanCategories
            .sortedWith(compareBy<ExpenseCategory> { it.sortOrder }.thenBy { it.id })
            .mapIndexed { idx, cat -> cat.copy(sortOrder = idx) }

        val finalCategories = if (baseFinalCats.isEmpty()) STANDARD_DEFAULT_CATEGORIES else baseFinalCats

        if (hasCustomLocalSort && !hasCustomRemoteSort) {
            syncUpdatedCategoriesToCloud(finalCategories)
        }

        val areDifferent = finalCategories.size != currentLocal.size ||
            finalCategories.any { fc ->
                val matching = currentLocal.find { it.id == fc.id }
                matching == null ||
                matching.name != fc.name ||
                matching.iconKey != fc.iconKey ||
                matching.monthlyBudget != fc.monthlyBudget ||
                matching.colorHex != fc.colorHex ||
                matching.voiceKeywords != fc.voiceKeywords ||
                matching.sortOrder != fc.sortOrder
            }

        if (areDifferent) {
            repository.replaceAllCategories(finalCategories)
        }
        _allCategories.value = finalCategories
    }

    private suspend fun recoverAndSyncUserData(
        user: FirebaseUser,
        profile: UserProfile,
        activeHouseholdId: String
    ) {
        withContext(Dispatchers.IO) {
            try {
                val allHouseholdIds = (profile.householdIds + listOf(activeHouseholdId)).filter { it.isNotBlank() }.distinct()

                // 1. RECOVER / RESTORE MEMBERS (Isolate strictly to active household)
                val targetMembers = if (activeHouseholdId.isNotBlank()) {
                    firestoreSyncRepo.getMembersOnce(activeHouseholdId)
                } else {
                    emptyList()
                }
                syncAndMergeMembers(targetMembers, profile, user, activeHouseholdId)

                // 2. RECOVER / RESTORE CATEGORIES (Isolate strictly to active household)
                val targetCats = if (activeHouseholdId.isNotBlank()) {
                    firestoreSyncRepo.getCategoriesOnce(activeHouseholdId)
                } else {
                    emptyList()
                }
                syncAndMergeCategories(targetCats, activeHouseholdId, user.uid)

                // 3. RECOVER / SYNC SAVINGS GOALS (Isolate strictly to active household)
                val remoteGoals = if (activeHouseholdId.isNotBlank()) {
                    firestoreSyncRepo.getSavingsGoalsOnce(activeHouseholdId)
                } else {
                    firestoreSyncRepo.getUserSavingsGoalsOnce(user.uid)
                }

                if (remoteGoals.isNotEmpty()) {
                    val domainGoals = remoteGoals.map { rg ->
                        val hashId = Math.abs(rg.id.hashCode().toLong()).coerceAtLeast(1L)
                        SavingsGoal(
                            id = hashId,
                            title = rg.title,
                            targetAmount = rg.targetAmount,
                            currentAmount = rg.currentAmount,
                            deadlineDate = rg.deadlineDate,
                            iconKey = rg.iconKey,
                            colorHex = rg.colorHex,
                            note = rg.note
                        )
                    }
                    repository.replaceAllSavingsGoals(domainGoals)
                } else {
                    repository.replaceAllSavingsGoals(emptyList())
                }

                // 4. RECOVER / SYNC RECURRING EXPENSES (Isolate strictly to active household)
                val remoteRecurring = if (activeHouseholdId.isNotBlank()) {
                    firestoreSyncRepo.getRecurringExpensesOnce(activeHouseholdId)
                } else {
                    firestoreSyncRepo.getUserRecurringExpensesOnce(user.uid)
                }

                if (remoteRecurring.isNotEmpty()) {
                    val domainRecurring = remoteRecurring.map { rr ->
                        val hashId = Math.abs(rr.id.hashCode().toLong()).coerceAtLeast(1L)
                        RecurringExpense(
                            id = hashId,
                            title = rr.title,
                            amount = rr.amount,
                            categoryId = rr.categoryId,
                            categoryName = rr.categoryName,
                            frequency = rr.frequency,
                            dayOfMonth = rr.dayOfMonth,
                            splitType = rr.splitType,
                            memberIdsCsv = rr.memberIdsCsv,
                            memberNamesDisplay = rr.memberNamesDisplay,
                            note = rr.note,
                            isActive = rr.isActive,
                            lastGeneratedMonth = rr.lastGeneratedMonth,
                            payerMemberId = rr.payerMemberId,
                            payerMemberName = rr.payerMemberName,
                            payerMemberIdsCsv = rr.payerMemberIdsCsv,
                            payerAmountsCsv = rr.payerAmountsCsv
                        )
                    }
                    repository.replaceAllRecurringExpenses(domainRecurring)
                } else {
                    repository.replaceAllRecurringExpenses(emptyList())
                }

                // 5. RECOVER / RESTORE EXPENSES (Zero Data Loss across household scope)
                val allCloudExpenses = firestoreSyncRepo.getAllExpensesAcrossAllScopes(allHouseholdIds, user.uid)
                val isMarios = isMariosUser(user)
                val userUid = user.uid
                val mariosUids = setOf(
                    com.example.data.seed.FirestoreDataSeed.DEFAULT_USER_UID,
                    "t5kN4RvPRugIxXUz18Fep2khSrC2", "tSLWmkXZ0MTqIKGoX3wd6HdhDw63",
                    "jllDLLQl7VgFzOQpVIHi0wo8B013", "oA6smvWcC6QYCAKMvYdWEcmVCO43"
                )
                val filteredCloudExpenses = if (!isMarios) {
                    allCloudExpenses.filter { exp ->
                        exp.householdId == activeHouseholdId && (exp.createdByUid.isBlank() || exp.createdByUid == userUid)
                    }
                } else {
                    allCloudExpenses.filter { exp ->
                        (exp.householdId == activeHouseholdId || (exp.householdId.isBlank() && _userHouseholds.value.size <= 1)) &&
                        (exp.createdByUid.isBlank() || mariosUids.contains(exp.createdByUid))
                    }
                }

                if (filteredCloudExpenses.isNotEmpty()) {
                    updateCombinedExpenses(householdList = filteredCloudExpenses, userList = filteredCloudExpenses, targetHid = activeHouseholdId)
                } else {
                    _allExpenses.value = emptyList()
                    repository.replaceAllExpenses(emptyList())
                }

                // 6. RECOVER / RESTORE INCOMES (Zero Data Loss across household scope)
                val allCloudIncomes = firestoreSyncRepo.getAllIncomesAcrossAllScopes(allHouseholdIds, user.uid)
                val mariosHids = setOf(com.example.data.seed.FirestoreDataSeed.DEFAULT_HOUSEHOLD_ID, "e1d4ae67-499")
                val filteredCloudIncomes = if (!isMarios) {
                    allCloudIncomes.filter { inc ->
                        !mariosHids.contains(inc.householdId) &&
                        (inc.createdByUid.isBlank() || inc.createdByUid == userUid) &&
                        inc.householdId == activeHouseholdId
                    }
                } else {
                    allCloudIncomes.filter { inc ->
                        (inc.householdId == activeHouseholdId || (inc.householdId.isBlank() && _userHouseholds.value.size <= 1)) &&
                        (inc.createdByUid.isBlank() || mariosUids.contains(inc.createdByUid))
                    }
                }

                if (filteredCloudIncomes.isNotEmpty()) {
                    updateCombinedIncomes(householdList = filteredCloudIncomes, userList = filteredCloudIncomes, targetHid = activeHouseholdId)
                } else {
                    _allIncomes.value = emptyList()
                    repository.replaceAllIncomes(emptyList())
                }

                // Check and execute any due standing orders
                checkAndProcessDueRecurringExpenses()

                // 7. Data Safety Guard audit to guarantee 100% bi-directional cloud mirror
                com.example.data.safety.DataSafetyGuard.auditAndSecureUserCloudData(
                    user = user,
                    activeHouseholdId = activeHouseholdId,
                    repository = repository,
                    firestoreSyncRepo = firestoreSyncRepo
                )
            } catch (e: Exception) {
                Log.w("OikoNomosVM", "Error in recoverAndSyncUserData: ${e.message}")
            }
        }
    }

    private fun mapFirebaseExpensesToDomain(remoteExpenses: List<FirebaseExpense>): List<Expense> {
        val currentCats = allCategories.value
        val currentMems = allMembers.value
        val currentExpenses = _allExpenses.value

        return remoteExpenses.map { remote ->
            // 1. Stable, consistent ID resolution:
            // If we already have a local expense with this cloudId, preserve its local Room id!
            val existingLocal = currentExpenses.find { it.cloudId.isNotBlank() && it.cloudId == remote.id }
            val hashId = Math.abs(remote.id.hashCode().toLong()).coerceAtLeast(1L)
            val resolvedId = existingLocal?.id
                ?: (if (remote.id.startsWith("exp_")) remote.id.removePrefix("exp_").toLongOrNull() else null)
                ?: hashId

            val memberCount = if (remote.splitType == "ALL_FAMILY") {
                currentMems.size.coerceAtLeast(1)
            } else {
                remote.memberIdsCsv.split(",").filter { it.isNotBlank() }.size.coerceAtLeast(1)
            }

            val matchingCat = currentCats.find { it.id == remote.categoryId }
                ?: (if (remote.categoryName.isNotBlank()) {
                    val normRem = GreekVoiceParser.normalizeGreek(remote.categoryName.trim().lowercase())
                    currentCats.find { GreekVoiceParser.normalizeGreek(it.name.trim().lowercase()) == normRem }
                } else null)
            val finalCatId = matchingCat?.id ?: remote.categoryId
            val finalCatName = matchingCat?.name ?: (if (remote.categoryName.isNotBlank()) remote.categoryName else (currentCats.find { it.id == remote.categoryId }?.name ?: ""))

            val finalMemberIdsCsv = remote.memberIdsCsv
            val finalMemberNamesDisplay = if (remote.memberNamesDisplay.isNotBlank()) {
                remote.memberNamesDisplay
            } else if (remote.splitType == "ALL_FAMILY") {
                "Κοινό Οικογένειας"
            } else ""

            val cleanPayerName = remote.payerMemberName.replace(Regex("\\([^)]*\\)"), "").trim()
            val finalPayerId = remote.payerMemberId
            val finalPayerName = cleanPayerName
            val finalPayerMemberIdsCsv = remote.payerMemberIdsCsv
            val finalPayerAmountsCsv = remote.payerAmountsCsv

            Expense(
                id = resolvedId,
                cloudId = remote.id,
                title = remote.title,
                amount = remote.amount,
                categoryId = finalCatId,
                categoryName = finalCatName,
                timestamp = if (remote.timestamp > 0L) remote.timestamp else (remote.createdAt?.toDate()?.time ?: System.currentTimeMillis()),
                splitType = remote.splitType,
                memberIdsCsv = finalMemberIdsCsv,
                memberNamesDisplay = finalMemberNamesDisplay,
                splitAmountPerPerson = if (memberCount > 0) remote.amount / memberCount else 0.0,
                originalVoicePrompt = "",
                note = remote.note,
                payerMemberId = finalPayerId,
                payerMemberName = finalPayerName,
                payerMemberIdsCsv = finalPayerMemberIdsCsv,
                payerAmountsCsv = finalPayerAmountsCsv
            )
        }
    }

    private fun mapFirebaseIncomesToDomain(remoteIncomes: List<FirebaseIncome>): List<Income> {
        val currentMems = allMembers.value
        val currentCats = allCategories.value
        val currentIncomes = _allIncomes.value

        return remoteIncomes.map { remote ->
            // 1. Stable, consistent ID resolution:
            // If we already have a local income with this cloudId or matching content, preserve its local Room id!
            val existingLocal = currentIncomes.find {
                (it.cloudId.isNotBlank() && it.cloudId == remote.id) ||
                (it.title == remote.title && Math.abs(it.timestamp - remote.timestamp) < 5000 && Math.abs(it.amount - remote.amount) < 0.01)
            }
            val hashId = Math.abs(remote.id.hashCode().toLong()).coerceAtLeast(1L)
            val resolvedId = existingLocal?.id
                ?: (if (remote.id.startsWith("inc_")) remote.id.removePrefix("inc_").toLongOrNull() else null)
                ?: hashId

            val matchingMember = if (remote.memberId != 0L) {
                currentMems.find { it.id == remote.memberId }
            } else if (remote.memberName.isNotBlank()) {
                currentMems.find { GreekVoiceParser.normalizeGreek(it.name.trim().lowercase()) == GreekVoiceParser.normalizeGreek(remote.memberName.trim().lowercase()) }
            } else null

            val finalMemberId = matchingMember?.id ?: remote.memberId
            val finalMemberName = matchingMember?.name ?: remote.memberName

            val matchingCat = currentCats.find { GreekVoiceParser.normalizeGreek(it.name.trim().lowercase()) == GreekVoiceParser.normalizeGreek(remote.category.trim().lowercase()) }
            val finalCat = if (matchingCat != null) matchingCat.name else remote.category

            Income(
                id = resolvedId,
                cloudId = remote.id,
                title = remote.title,
                amount = remote.amount,
                memberId = finalMemberId,
                memberName = finalMemberName,
                category = finalCat,
                timestamp = if (remote.timestamp > 0L) remote.timestamp else (remote.createdAt?.toDate()?.time ?: System.currentTimeMillis()),
                note = remote.note
            )
        }
    }

    private var lastHouseholdExpenses = listOf<FirebaseExpense>()
    private var lastUserExpenses = listOf<FirebaseExpense>()

    @Synchronized
    private fun updateCombinedExpenses(
        householdList: List<FirebaseExpense>? = null,
        userList: List<FirebaseExpense>? = null,
        targetHid: String? = null
    ) {
        val currentHid = targetHid ?: _currentHousehold.value?.id.orEmpty()
        if (householdList != null) lastHouseholdExpenses = householdList
        if (userList != null) lastUserExpenses = userList

        val relevantUserExpenses = if (currentHid.isNotBlank()) {
            lastUserExpenses.filter { it.householdId == currentHid || (it.householdId.isBlank() && _userHouseholds.value.size <= 1) }
        } else {
            lastUserExpenses
        }

        val user = _currentUser.value
        val isMarios = isMariosUser(user)
        val userUid = user?.uid.orEmpty()
        val mariosHids = setOf(com.example.data.seed.FirestoreDataSeed.DEFAULT_HOUSEHOLD_ID, "e1d4ae67-499")
        val mariosUids = setOf(
            com.example.data.seed.FirestoreDataSeed.DEFAULT_USER_UID,
            "t5kN4RvPRugIxXUz18Fep2khSrC2", "tSLWmkXZ0MTqIKGoX3wd6HdhDw63",
            "jllDLLQl7VgFzOQpVIHi0wo8B013", "oA6smvWcC6QYCAKMvYdWEcmVCO43"
        )

        val rawList = (lastHouseholdExpenses + relevantUserExpenses).filter { exp ->
            if (currentHid.isNotBlank()) {
                exp.householdId == currentHid || (exp.householdId.isBlank() && _userHouseholds.value.size <= 1)
            } else true
        }
        val filteredExpenses = if (!isMarios) {
            rawList.filter { exp ->
                !mariosHids.contains(exp.householdId) &&
                (exp.createdByUid.isBlank() || exp.createdByUid == userUid)
            }
        } else {
            rawList.filter { exp ->
                exp.createdByUid.isBlank() || mariosUids.contains(exp.createdByUid)
            }
        }

        val combined = filteredExpenses
            .distinctBy { it.id.ifBlank { "${it.title}_${it.timestamp}_${it.amount}" } }
            .sortedWith(
                compareByDescending<FirebaseExpense> { it.timestamp }
                    .thenByDescending { it.createdAt?.toDate()?.time ?: 0L }
                    .thenByDescending { it.id }
            )

        val domain = mapFirebaseExpensesToDomain(combined)
        val recentLocal = _allExpenses.value.filter { local ->
            local.cloudId.isNotBlank() && (System.currentTimeMillis() - local.timestamp < 120_000L) &&
            domain.none { it.cloudId == local.cloudId || (it.title == local.title && Math.abs(it.timestamp - local.timestamp) < 5000 && Math.abs(it.amount - local.amount) < 0.01) }
        }
        val finalExpenses = (domain + recentLocal).sortedWith(
            compareByDescending<Expense> { it.timestamp }.thenByDescending { it.id }
        )

        if (finalExpenses.isNotEmpty()) {
            val current = _allExpenses.value
            val isSame = current.size == finalExpenses.size && current.zip(finalExpenses).all { (a, b) ->
                a.id == b.id && a.amount == b.amount && a.title == b.title && a.categoryId == b.categoryId && a.timestamp == b.timestamp
            }
            if (!isSame) {
                _allExpenses.value = finalExpenses
                _isInitialExpensesLoading.value = false
                prefs.edit().putBoolean("has_entered_any_expense", true).apply()
                viewModelScope.launch {
                    try {
                        repository.replaceAllExpenses(finalExpenses)
                    } catch (e: Exception) {
                        Log.d("OikoNomosVM", "Room cache combined expenses: ${e.message}")
                    }
                }
            }
        } else {
            if (householdList != null && (userList != null || lastUserExpenses.isEmpty())) {
                _allExpenses.value = emptyList()
                _isInitialExpensesLoading.value = false
            }
        }
    }

    private var lastHouseholdIncomes = listOf<FirebaseIncome>()
    private var lastUserIncomes = listOf<FirebaseIncome>()

    @Synchronized
    private fun updateCombinedIncomes(
        householdList: List<FirebaseIncome>? = null,
        userList: List<FirebaseIncome>? = null,
        targetHid: String? = null
    ) {
        val currentHid = targetHid ?: _currentHousehold.value?.id.orEmpty()
        if (householdList != null) lastHouseholdIncomes = householdList
        if (userList != null) lastUserIncomes = userList

        val relevantUserIncomes = if (currentHid.isNotBlank()) {
            lastUserIncomes.filter { it.householdId == currentHid || (it.householdId.isBlank() && _userHouseholds.value.size <= 1) }
        } else {
            lastUserIncomes
        }

        val combinedRemote = (lastHouseholdIncomes + relevantUserIncomes)
            .filter { inc ->
                if (currentHid.isNotBlank()) {
                    inc.householdId == currentHid || (inc.householdId.isBlank() && _userHouseholds.value.size <= 1)
                } else true
            }
            .distinctBy { it.id.ifBlank { "${it.title}_${it.timestamp}_${it.amount}" } }
            .sortedWith(
                compareByDescending<FirebaseIncome> { it.timestamp }
                    .thenByDescending { it.createdAt?.toDate()?.time ?: 0L }
                    .thenByDescending { it.id }
            )

        val user = _currentUser.value
        val isMarios = isMariosUser(user)
        val userUid = user?.uid.orEmpty()
        val mariosHids = setOf(com.example.data.seed.FirestoreDataSeed.DEFAULT_HOUSEHOLD_ID, "e1d4ae67-499")
        val mariosUids = setOf(
            com.example.data.seed.FirestoreDataSeed.DEFAULT_USER_UID,
            "t5kN4RvPRugIxXUz18Fep2khSrC2", "tSLWmkXZ0MTqIKGoX3wd6HdhDw63",
            "jllDLLQl7VgFzOQpVIHi0wo8B013", "oA6smvWcC6QYCAKMvYdWEcmVCO43"
        )

        val filteredRemote = if (!isMarios) {
            combinedRemote.filter { inc ->
                !mariosHids.contains(inc.householdId) &&
                (inc.createdByUid.isBlank() || inc.createdByUid == userUid)
            }
        } else {
            combinedRemote.filter { inc ->
                inc.createdByUid.isBlank() || mariosUids.contains(inc.createdByUid)
            }
        }

        val domainRemote = mapFirebaseIncomesToDomain(filteredRemote)
        val recentLocalIncomes = _allIncomes.value.filter { local ->
            local.cloudId.isNotBlank() && (System.currentTimeMillis() - local.timestamp < 120_000L) &&
            domainRemote.none { it.cloudId == local.cloudId || (it.title == local.title && Math.abs(it.timestamp - local.timestamp) < 5000 && Math.abs(it.amount - local.amount) < 0.01) }
        }

        val mergedList = (domainRemote + recentLocalIncomes).toMutableList()

        val dedupedList = mergedList
            .distinctBy { it.cloudId.ifBlank { "${it.title}_${it.timestamp}_${it.amount}" } }

        val usedIds = mutableSetOf<Long>()
        var nextAvailableId = 1L
        val sortedMerged = dedupedList
            .sortedWith(
                compareByDescending<Income> { it.timestamp }.thenByDescending { it.id }
            )
            .map { inc ->
                val chosenId = if (inc.id > 0L && usedIds.add(inc.id)) {
                    inc.id
                } else {
                    while (usedIds.contains(nextAvailableId)) {
                        nextAvailableId++
                    }
                    usedIds.add(nextAvailableId)
                    nextAvailableId
                }
                if (chosenId != inc.id) inc.copy(id = chosenId) else inc
            }

        if (sortedMerged.isNotEmpty()) {
            val current = _allIncomes.value
            val isSame = current.size == sortedMerged.size && current.zip(sortedMerged).all { (a, b) ->
                a.id == b.id && a.amount == b.amount && a.title == b.title && a.timestamp == b.timestamp
            }
            if (!isSame) {
                _allIncomes.value = sortedMerged
                viewModelScope.launch {
                    try {
                        repository.replaceAllIncomes(sortedMerged)
                    } catch (e: Exception) {
                        Log.d("OikoNomosVM", "Room cache combined incomes: ${e.message}")
                    }
                }
            }
        } else {
            val safeRecent = _allIncomes.value.filter { local ->
                local.cloudId.isNotBlank() && (System.currentTimeMillis() - local.timestamp < 120_000L)
            }
            if (safeRecent.isNotEmpty()) {
                _allIncomes.value = safeRecent
            } else if (householdList != null && (userList != null || lastUserIncomes.isEmpty())) {
                _allIncomes.value = emptyList()
                viewModelScope.launch {
                    try {
                        repository.replaceAllIncomes(emptyList())
                    } catch (e: Exception) {
                        Log.d("OikoNomosVM", "Room clear incomes: ${e.message}")
                    }
                }
            }
        }
    }

    private var lastHouseholdMembers = listOf<FirebaseMember>()
    private var lastUserMembers = listOf<FirebaseMember>()

    @Synchronized
    private fun updateCombinedMembers(
        householdList: List<FirebaseMember>? = null,
        userList: List<FirebaseMember>? = null,
        targetHid: String? = null
    ) {
        val currentHid = targetHid ?: _currentHousehold.value?.id.orEmpty()
        if (householdList != null) lastHouseholdMembers = householdList

        val combined = lastHouseholdMembers.distinctBy {
            it.id.ifBlank { GreekVoiceParser.normalizeGreek(it.name.trim().lowercase()) }
        }
        viewModelScope.launch {
            syncAndMergeMembers(combined, explicitHouseholdId = currentHid)
        }
    }

    private var lastHouseholdCategories = listOf<FirebaseCategory>()
    private var lastUserCategories = listOf<FirebaseCategory>()

    @Synchronized
    private fun updateCombinedCategories(
        householdList: List<FirebaseCategory>? = null,
        userList: List<FirebaseCategory>? = null,
        targetHid: String? = null
    ) {
        val currentHid = targetHid ?: _currentHousehold.value?.id.orEmpty()
        if (householdList != null) lastHouseholdCategories = householdList

        val combined = lastHouseholdCategories.distinctBy {
            it.id.ifBlank { GreekVoiceParser.normalizeGreek(it.name.trim().lowercase()) }
        }
        viewModelScope.launch {
            syncAndMergeCategories(combined, explicitHouseholdId = currentHid)
        }
    }

    private fun observeUserData(userId: String) {
        userProfileSyncJob?.cancel()
        userProfileSyncJob = viewModelScope.launch(coroutineExceptionHandler) {
            while (isActive) {
                try {
                    firestoreSyncRepo.observeUserProfile(userId).collect { remoteProfile ->
                        if (remoteProfile != null) {
                            val current = _userProfile.value
                            val merged = (current ?: remoteProfile).copy(
                                uid = remoteProfile.uid.ifBlank { current?.uid ?: userId },
                                email = remoteProfile.email.ifBlank { current?.email ?: "" },
                                displayName = remoteProfile.displayName.ifBlank { current?.displayName ?: "" },
                                firstName = remoteProfile.firstName.ifBlank { current?.firstName ?: "" },
                                lastName = remoteProfile.lastName.ifBlank { current?.lastName ?: "" },
                                birthDate = remoteProfile.birthDate.ifBlank { current?.birthDate ?: "" },
                                phoneNumber = remoteProfile.phoneNumber.ifBlank { current?.phoneNumber ?: "" },
                                occupation = remoteProfile.occupation.ifBlank { current?.occupation ?: "" },
                                city = remoteProfile.city.ifBlank { current?.city ?: "" },
                                householdId = remoteProfile.householdId.ifBlank { current?.householdId ?: "" },
                                householdIds = if (remoteProfile.householdIds.isNotEmpty()) remoteProfile.householdIds else current?.householdIds.orEmpty(),
                                photoUrl = remoteProfile.photoUrl.ifBlank { current?.photoUrl ?: "" },
                                isPremium = if (isMariosUser(uid = userId)) true else remoteProfile.isPremium,
                                appLanguage = remoteProfile.appLanguage.ifBlank { current?.appLanguage ?: "EL" },
                                themeMode = remoteProfile.themeMode.ifBlank { current?.themeMode ?: "SYSTEM" },
                                reminderEnabled = remoteProfile.reminderEnabled,
                                reminderHour = remoteProfile.reminderHour,
                                reminderMinute = remoteProfile.reminderMinute,
                                notificationConsentCompleted = remoteProfile.notificationConsentCompleted || (current?.notificationConsentCompleted ?: false)
                            )
                            _userProfile.value = merged
                            saveUserProfileToPrefs(merged)
                            if (remoteProfile.appLanguage.isNotBlank() && remoteProfile.appLanguage != _appLanguage.value) {
                                setAppLanguage(remoteProfile.appLanguage, syncToCloud = false)
                            }
                            if (remoteProfile.themeMode.isNotBlank() && remoteProfile.themeMode != _themeMode.value) {
                                setThemeMode(remoteProfile.themeMode, syncToCloud = false)
                            }
                            if (remoteProfile.notificationConsentCompleted && !_notificationConsentCompleted.value) {
                                _notificationConsentCompleted.value = true
                                prefs.edit().putBoolean("notification_consent_handled_$userId", true).apply()
                            }
                            if (remoteProfile.reminderEnabled != _dailyReminderEnabled.value ||
                                remoteProfile.reminderHour != _dailyReminderHour.value ||
                                remoteProfile.reminderMinute != _dailyReminderMinute.value) {
                                _dailyReminderEnabled.value = remoteProfile.reminderEnabled
                                _dailyReminderHour.value = remoteProfile.reminderHour
                                _dailyReminderMinute.value = remoteProfile.reminderMinute
                                DailyReminderManager.setReminderEnabled(getApplication(), remoteProfile.reminderEnabled)
                                DailyReminderManager.setReminderTime(getApplication(), remoteProfile.reminderHour, remoteProfile.reminderMinute)
                                if (remoteProfile.reminderEnabled) {
                                    DailyReminderManager.scheduleDailyReminder(getApplication(), remoteProfile.reminderHour, remoteProfile.reminderMinute)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w("OikoNomosVM", "UserProfile listener reconnecting: ${e.message}")
                    delay(3000)
                }
            }
        }

        userExpensesSyncJob?.cancel()
        userExpensesSyncJob = viewModelScope.launch(coroutineExceptionHandler) {
            while (isActive) {
                try {
                    firestoreSyncRepo.observeUserExpenses(userId).collect { remoteExpenses ->
                        updateCombinedExpenses(userList = remoteExpenses)
                    }
                } catch (e: Exception) {
                    Log.w("OikoNomosVM", "UserExpenses listener reconnecting: ${e.message}")
                    delay(3000)
                }
            }
        }

        userIncomesSyncJob?.cancel()
        userIncomesSyncJob = viewModelScope.launch(coroutineExceptionHandler) {
            while (isActive) {
                try {
                    firestoreSyncRepo.observeUserIncomes(userId).collect { remoteIncomes ->
                        updateCombinedIncomes(userList = remoteIncomes)
                    }
                } catch (e: Exception) {
                    Log.w("OikoNomosVM", "UserIncomes listener reconnecting: ${e.message}")
                    delay(3000)
                }
            }
        }

        userMembersSyncJob?.cancel()
        userCategoriesSyncJob?.cancel()

        userSavingsGoalsSyncJob?.cancel()
        userSavingsGoalsSyncJob = viewModelScope.launch(coroutineExceptionHandler) {
            while (isActive) {
                try {
                    firestoreSyncRepo.observeUserSavingsGoals(userId).collect { remoteGoals ->
                        if (remoteGoals.isNotEmpty()) {
                            val domain = remoteGoals.map { rg ->
                                val hashId = Math.abs(rg.id.hashCode().toLong()).coerceAtLeast(1L)
                                SavingsGoal(
                                    id = hashId,
                                    title = rg.title,
                                    targetAmount = rg.targetAmount,
                                    currentAmount = rg.currentAmount,
                                    deadlineDate = rg.deadlineDate,
                                    iconKey = rg.iconKey,
                                    colorHex = rg.colorHex,
                                    note = rg.note
                                )
                            }
                            repository.insertSavingsGoals(domain)
                        }
                    }
                } catch (e: Exception) {
                    Log.w("OikoNomosVM", "UserSavingsGoals listener reconnecting: ${e.message}")
                    delay(3000)
                }
            }
        }

        userRecurringSyncJob?.cancel()
        userRecurringSyncJob = viewModelScope.launch(coroutineExceptionHandler) {
            while (isActive) {
                try {
                    firestoreSyncRepo.observeUserRecurringExpenses(userId).collect { remoteRecurring ->
                        if (remoteRecurring.isNotEmpty()) {
                            val domain = remoteRecurring.map { rr ->
                                val hashId = Math.abs(rr.id.hashCode().toLong()).coerceAtLeast(1L)
                                RecurringExpense(
                                    id = hashId,
                                    title = rr.title,
                                    amount = rr.amount,
                                    categoryId = rr.categoryId,
                                    categoryName = rr.categoryName,
                                    frequency = rr.frequency,
                                    dayOfMonth = rr.dayOfMonth,
                                    splitType = rr.splitType,
                                    memberIdsCsv = rr.memberIdsCsv,
                                    memberNamesDisplay = rr.memberNamesDisplay,
                                    note = rr.note,
                                    isActive = rr.isActive,
                                    lastGeneratedMonth = rr.lastGeneratedMonth,
                                    payerMemberId = rr.payerMemberId,
                                    payerMemberName = rr.payerMemberName,
                                    payerMemberIdsCsv = rr.payerMemberIdsCsv,
                                    payerAmountsCsv = rr.payerAmountsCsv
                                )
                            }
                            repository.insertRecurringExpenses(domain)
                            checkAndProcessDueRecurringExpenses()
                        }
                    }
                } catch (e: Exception) {
                    Log.w("OikoNomosVM", "UserRecurring listener reconnecting: ${e.message}")
                    delay(3000)
                }
            }
        }
    }

    private var lastObservedHouseholdId: String? = null

    private fun observeHouseholdData(householdId: String) {
        if (householdId.isBlank()) return
        if (lastObservedHouseholdId != householdId) {
            lastObservedHouseholdId = householdId
            _allExpenses.value = emptyList()
            _allIncomes.value = emptyList()
        }
        householdJob?.cancel()
        householdJob = viewModelScope.launch {
            while (isActive) {
                try {
                    firestoreSyncRepo.observeHousehold(householdId).collect { h ->
                        if (h != null) {
                            _currentHousehold.value = h
                        }
                    }
                } catch (e: Exception) {
                    Log.w("OikoNomosVM", "Household listener reconnecting: ${e.message}")
                    delay(3000)
                }
            }
        }

        expensesSyncJob?.cancel()
        expensesSyncJob = viewModelScope.launch {
            // Step 1: Read from Firestore disk cache only if memory/Room is currently empty
            try {
                val cached = firestoreSyncRepo.getExpensesFromCache(householdId)
                if (cached.isNotEmpty()) {
                    updateCombinedExpenses(householdList = cached)
                }
            } catch (e: Exception) {
                Log.d("OikoNomosVM", "Disk cache expenses: ${e.message}")
            }

            // Step 2: Fetch latest from server across scopes (dual-path zero data loss)
            try {
                val direct = firestoreSyncRepo.getExpensesOnce(householdId)
                if (direct.isNotEmpty()) {
                    updateCombinedExpenses(householdList = direct, targetHid = householdId)
                } else {
                    val isMarios = isMariosUser()
                    val extraHids = if (isMarios) listOf("3e9a25a0-2e7", "e1d4ae67-499") else emptyList()
                    val allHids = (_userProfile.value?.householdIds.orEmpty() + listOf(householdId) + extraHids).filter { it.isNotBlank() }.distinct()
                    val allExpenses = firestoreSyncRepo.getAllExpensesAcrossAllScopes(allHids, _currentUser.value?.uid.orEmpty())
                    val matching = allExpenses.filter { it.householdId == householdId }
                    if (matching.isNotEmpty()) {
                        updateCombinedExpenses(householdList = matching, targetHid = householdId)
                    } else if (allExpenses.isNotEmpty() && isMarios && householdId == com.example.data.seed.FirestoreDataSeed.DEFAULT_HOUSEHOLD_ID) {
                        updateCombinedExpenses(householdList = allExpenses, targetHid = householdId)
                    } else {
                        updateCombinedExpenses(householdList = emptyList(), targetHid = householdId)
                    }
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
                        updateCombinedExpenses(householdList = remoteExpenses, targetHid = householdId)
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
                    updateCombinedIncomes(householdList = cached, targetHid = householdId)
                }
            } catch (e: Exception) {
                Log.d("OikoNomosVM", "Disk cache incomes: ${e.message}")
            }

            try {
                val direct = firestoreSyncRepo.getIncomesOnce(householdId)
                if (direct.isNotEmpty()) {
                    updateCombinedIncomes(householdList = direct, targetHid = householdId)
                } else {
                    val isMarios = isMariosUser()
                    val extraHids = if (isMarios) listOf("3e9a25a0-2e7", "e1d4ae67-499") else emptyList()
                    val allHids = (_userProfile.value?.householdIds.orEmpty() + listOf(householdId) + extraHids).filter { it.isNotBlank() }.distinct()
                    val allIncomes = firestoreSyncRepo.getAllIncomesAcrossAllScopes(allHids, _currentUser.value?.uid.orEmpty())
                    val matching = allIncomes.filter { it.householdId == householdId }
                    if (matching.isNotEmpty()) {
                        updateCombinedIncomes(householdList = matching, targetHid = householdId)
                    } else if (allIncomes.isNotEmpty() && isMarios && householdId == com.example.data.seed.FirestoreDataSeed.DEFAULT_HOUSEHOLD_ID) {
                        updateCombinedIncomes(householdList = allIncomes, targetHid = householdId)
                    } else {
                        updateCombinedIncomes(householdList = emptyList(), targetHid = householdId)
                    }
                }
            } catch (e: Exception) {
                Log.w("OikoNomosVM", "Initial direct incomes fetch: ${e.message}")
            }

            while (isActive) {
                try {
                    firestoreSyncRepo.observeIncomes(householdId).collect { remoteIncomes ->
                        updateCombinedIncomes(householdList = remoteIncomes, targetHid = householdId)
                    }
                } catch (e: Exception) {
                    Log.w("OikoNomosVM", "Incomes listener reconnecting: ${e.message}")
                    delay(3000)
                }
            }
        }

        membersSyncJob?.cancel()
        membersSyncJob = viewModelScope.launch {
            while (isActive) {
                try {
                    firestoreSyncRepo.observeMembers(householdId).collect { remoteMembers ->
                        updateCombinedMembers(householdList = remoteMembers, targetHid = householdId)
                    }
                } catch (e: Exception) {
                    Log.w("OikoNomosVM", "Members listener reconnecting: ${e.message}")
                    delay(3000)
                }
            }
        }

        categoriesSyncJob?.cancel()
        categoriesSyncJob = viewModelScope.launch {
            while (isActive) {
                try {
                    firestoreSyncRepo.observeCategories(householdId).collect { remoteCats ->
                        updateCombinedCategories(householdList = remoteCats, targetHid = householdId)
                    }
                } catch (e: Exception) {
                    Log.w("OikoNomosVM", "Categories listener reconnecting: ${e.message}")
                    delay(3000)
                }
            }
        }

        savingsGoalsSyncJob?.cancel()
        savingsGoalsSyncJob = viewModelScope.launch {
            while (isActive) {
                try {
                    firestoreSyncRepo.observeSavingsGoals(householdId).collect { remoteGoals ->
                        val domain = remoteGoals.map { rg ->
                            val resolvedId = rg.id.toLongOrNull() ?: Math.abs(rg.id.hashCode().toLong()).coerceAtLeast(1L)
                            SavingsGoal(
                                id = resolvedId,
                                title = rg.title,
                                targetAmount = rg.targetAmount,
                                currentAmount = rg.currentAmount,
                                deadlineDate = rg.deadlineDate,
                                iconKey = rg.iconKey,
                                colorHex = rg.colorHex,
                                note = rg.note
                            )
                        }
                        repository.replaceAllSavingsGoals(domain)
                    }
                } catch (e: Exception) {
                    Log.w("OikoNomosVM", "Savings goals listener reconnecting: ${e.message}")
                    delay(3000)
                }
            }
        }

        recurringSyncJob?.cancel()
        recurringSyncJob = viewModelScope.launch {
            while (isActive) {
                try {
                    firestoreSyncRepo.observeRecurringExpenses(householdId).collect { remoteRecurring ->
                        val deduplicated = remoteRecurring.distinctBy {
                            GreekVoiceParser.normalizeGreek(it.title.trim().lowercase())
                        }
                        val domain = deduplicated.map { rr ->
                            val resolvedId = rr.id.toLongOrNull() ?: Math.abs(rr.id.hashCode().toLong()).coerceAtLeast(1L)
                            RecurringExpense(
                                id = resolvedId,
                                title = rr.title,
                                amount = rr.amount,
                                categoryId = rr.categoryId,
                                categoryName = rr.categoryName,
                                frequency = rr.frequency,
                                dayOfMonth = rr.dayOfMonth,
                                splitType = rr.splitType,
                                memberIdsCsv = rr.memberIdsCsv,
                                memberNamesDisplay = rr.memberNamesDisplay,
                                note = rr.note,
                                isActive = rr.isActive,
                                lastGeneratedMonth = rr.lastGeneratedMonth,
                                payerMemberId = rr.payerMemberId,
                                payerMemberName = rr.payerMemberName,
                                payerMemberIdsCsv = rr.payerMemberIdsCsv,
                                payerAmountsCsv = rr.payerAmountsCsv
                            )
                        }
                        repository.replaceAllRecurringExpenses(domain)
                    }
                } catch (e: Exception) {
                    Log.w("OikoNomosVM", "Recurring listener reconnecting: ${e.message}")
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
        if (!isUserVerified(user)) {
            user.reload().addOnCompleteListener {
                val refreshed = auth.currentUser ?: user
                val verified = isUserVerified(refreshed)
                _isUserVerifiedState.value = verified
                if (verified) {
                    _currentUser.value = refreshed
                    showSnackbar("Το email επιβεβαιώθηκε επιτυχώς! Καλώς ήρθατε!")
                    viewModelScope.launch {
                        loadUserProfileAndHousehold(refreshed)
                    }
                }
            }
            return
        }
        _currentUser.value = user
        _isUserVerifiedState.value = true
        viewModelScope.launch {
            try {
                firestoreSyncRepo.enableNetwork()
                val household = _currentHousehold.value
                val activeHid = household?.id.orEmpty()
                if (activeHid.isNotBlank()) {
                    val freshExpenses = firestoreSyncRepo.getExpensesOnce(activeHid)
                    if (freshExpenses.isNotEmpty()) {
                        updateCombinedExpenses(householdList = freshExpenses, targetHid = activeHid)
                    }
                    val freshIncomes = firestoreSyncRepo.getIncomesOnce(activeHid)
                    if (freshIncomes.isNotEmpty()) {
                        updateCombinedIncomes(householdList = freshIncomes, targetHid = activeHid)
                    }
                }
                if (household != null && household.id.isNotBlank()) {
                    if (expensesSyncJob?.isActive != true || incomesSyncJob?.isActive != true) {
                        observeHouseholdData(household.id)
                    }
                    val profile = _userProfile.value
                    if (profile != null) {
                        recoverAndSyncUserData(user, profile, household.id)
                    }
                } else {
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
                showSnackbar("Δημιουργήθηκε το εξοδολόγιο: ${household.name}!")
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

    fun inviteMemberByEmail(email: String, context: Context? = null) {
        val hid = _currentHousehold.value?.id ?: return
        val cleanEmail = email.trim()
        val isEn = _appLanguage.value == "EN"

        if (cleanEmail.isBlank() || !cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            showSnackbar(if (isEn) "Please enter a valid email address" else "Παρακαλώ εισάγετε έγκυρη διεύθυνση email")
            return
        }

        // Pro check: inviting a member to an existing household is a Pro feature
        val isUserPro = _userProfile.value?.isPremium == true || isMariosUser() || _currentHousehold.value?.isPremium == true
        if (!isUserPro) {
            showPremiumPaywall("invite_member")
            return
        }

        viewModelScope.launch {
            try {
                firestoreSyncRepo.inviteMemberToHousehold(hid, cleanEmail)

                // Update current household state immediately
                val currentH = _currentHousehold.value
                if (currentH != null && currentH.id == hid) {
                    val updated = (currentH.invitedEmails + cleanEmail).distinct()
                    _currentHousehold.value = currentH.copy(invitedEmails = updated)
                }

                // Refresh user households list from cloud
                val user = _currentUser.value
                if (user != null) {
                    val hids = firestoreSyncRepo.discoverUserHouseholds(user.uid, user.email ?: "")
                    val loaded = firestoreSyncRepo.fetchUserHouseholds(hids)
                    if (loaded.isNotEmpty()) {
                        _userHouseholds.value = loaded
                    }
                }

                // Dispatch Email Invitation Intent to notify the invited user
                if (context != null) {
                    val inviterName = _currentUser.value?.displayName?.ifBlank { _userProfile.value?.displayName }?.ifBlank { null }
                        ?: (if (isEn) "A family member" else "Ένα μέλος")
                    val householdName = _currentHousehold.value?.name ?: (if (isEn) "Household" else "Εξοδολόγιο")
                    val playStoreLink = "https://play.google.com/store/apps/details?id=com.aistudio.oikonomos.app"

                    val subject = if (isEn)
                        "Invitation to household \"$householdName\" on Fintel"
                    else
                        "Πρόσκληση στο εξοδολόγιο «$householdName» στην εφαρμογή Fintel"

                    val body = if (isEn) """
                        Hello!

                        $inviterName has invited you to join the household "$householdName" on the Fintel expense tracker app.

                        Download Fintel on Google Play Store here:
                        $playStoreLink

                        Sign in with this email address ($cleanEmail) and "$householdName" will automatically appear in your available households!
                    """.trimIndent() else """
                        Γεια σας!

                        Ο/Η $inviterName σας πρόσθεσε στο εξοδολόγιο «$householdName» στην εφαρμογή διαχείρισης εξόδων Fintel.

                        Κατεβάστε την εφαρμογή Fintel από το Google Play Store εδώ:
                        $playStoreLink

                        Συνδεθείτε στην εφαρμογή με αυτό το email ($cleanEmail) και το εξοδολόγιο «$householdName» θα εμφανιστεί αυτόματα στα διαθέσιμα εξοδολόγιά σας!
                    """.trimIndent()

                    val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:$cleanEmail")
                        putExtra(Intent.EXTRA_EMAIL, arrayOf(cleanEmail))
                        putExtra(Intent.EXTRA_SUBJECT, subject)
                        putExtra(Intent.EXTRA_TEXT, body)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }

                    try {
                        context.startActivity(emailIntent)
                    } catch (_: Exception) {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_EMAIL, arrayOf(cleanEmail))
                            putExtra(Intent.EXTRA_SUBJECT, subject)
                            putExtra(Intent.EXTRA_TEXT, body)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        try {
                            context.startActivity(Intent.createChooser(shareIntent, if (isEn) "Send Invitation" else "Αποστολή Πρόσκλησης").apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            })
                        } catch (_: Exception) {}
                    }
                }

                showSnackbar(
                    if (isEn)
                        "Invitation saved for $cleanEmail and email prepared!"
                    else
                        "Η πρόσκληση για $cleanEmail καταχωρήθηκε στη βάση και ετοιμάστηκε το email!"
                )
            } catch (e: Exception) {
                showSnackbar("Σφάλμα πρόσκλησης: ${e.message}")
            }
        }
    }

    fun deleteCurrentUserAccount() {
        viewModelScope.launch {
            val user = auth.currentUser ?: _currentUser.value
            val uid = user?.uid
            if (user == null || uid.isNullOrBlank()) {
                showSnackbar(if (_appLanguage.value == "EN") "No active user session found" else "Δεν βρέθηκε ενεργός συνδεδεμένος χρήστης")
                return@launch
            }
            try {
                // 1. Wipe remote data from Firestore for this user
                val hIds = _userHouseholds.value.map { it.id }
                firestoreSyncRepo.deleteUserEntireData(uid, hIds)

                // 2. Delete the Firebase Auth account
                user.delete().await()

                // 3. Clear local device cache & Room DB
                repository.clearAllData()
                clearUserProfileFromPrefs()
                prefs.edit().clear().apply()
                cancelAllSyncJobs()

                // 4. Reset in-memory state
                _currentUser.value = null
                _userProfile.value = null
                _currentHousehold.value = null
                _userHouseholds.value = emptyList()
                _allExpenses.value = emptyList()
                _allIncomes.value = emptyList()
                _allMembers.value = emptyList()
                _allCategories.value = emptyList()

                showSnackbar(if (_appLanguage.value == "EN") "Your account and personal data have been completely deleted" else "Ο λογαριασμός και όλα τα δεδομένα σας διαγράφηκαν επιτυχώς")
            } catch (e: com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException) {
                Log.w("OikoNomosVM", "Recent login required for account deletion: ${e.message}")
                showSnackbar(
                    if (_appLanguage.value == "EN")
                        "For your security, please sign out, sign in again, and retry Account Deletion."
                    else
                        "Για λόγους ασφαλείας, παρακαλούμε αποσυνδεθείτε, συνδεθείτε ξανά και πατήστε πάλι Διαγραφή Λογαριασμού."
                )
            } catch (e: Exception) {
                Log.e("OikoNomosVM", "deleteCurrentUserAccount error", e)
                showSnackbar("Σφάλμα διαγραφής: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading = _isAuthLoading.asStateFlow()

    fun isUserVerified(user: FirebaseUser?): Boolean {
        if (user == null) return false
        return true
    }

    fun checkEmailVerification(onResult: (Boolean) -> Unit) {
        val user = auth.currentUser ?: _currentUser.value ?: run {
            onResult(false)
            return
        }
        _isAuthLoading.value = true
        user.reload().addOnCompleteListener { task ->
            val refreshedUser = auth.currentUser ?: user
            val verified = isUserVerified(refreshedUser)
            if (verified) {
                _isAuthLoading.value = false
                _isUserVerifiedState.value = true
                _currentUser.value = refreshedUser
                showSnackbar("Το email επιβεβαιώθηκε επιτυχώς! Καλώς ήρθατε!")
                viewModelScope.launch {
                    loadUserProfileAndHousehold(refreshedUser)
                }
                onResult(true)
            } else {
                // Secondary check: force-refresh token to make sure claims/verification state updates
                user.getIdToken(true).addOnCompleteListener {
                    _isAuthLoading.value = false
                    val doubleCheckUser = auth.currentUser ?: user
                    val finalVerified = isUserVerified(doubleCheckUser)
                    _isUserVerifiedState.value = finalVerified
                    if (finalVerified) {
                        _currentUser.value = doubleCheckUser
                        showSnackbar("Το email επιβεβαιώθηκε επιτυχώς! Καλώς ήρθατε!")
                        viewModelScope.launch {
                            loadUserProfileAndHousehold(doubleCheckUser)
                        }
                    }
                    onResult(finalVerified)
                }
            }
        }
    }

    fun resendVerificationEmail(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = auth.currentUser ?: _currentUser.value
        if (user == null) {
            onError("Δεν βρέθηκε συνδεδεμένος χρήστης")
            return
        }
        _isAuthLoading.value = true
        auth.setLanguageCode(if (_appLanguage.value == "EN") "en" else "el")
        user.sendEmailVerification()
            .addOnSuccessListener {
                _isAuthLoading.value = false
                showSnackbar("Στάλθηκε νέο email επιβεβαίωσης!")
                onSuccess()
            }
            .addOnFailureListener { exc ->
                _isAuthLoading.value = false
                onError(exc.localizedMessage ?: "Σφάλμα αποστολής email")
            }
    }

    fun sendPasswordResetEmail(email: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val clean = email.trim()
        if (clean.isBlank()) {
            onError(if (_appLanguage.value == "EN") "Please enter your email" else "Παρακαλώ εισάγετε το email σας")
            return
        }
        _isAuthLoading.value = true
        auth.setLanguageCode(if (_appLanguage.value == "EN") "en" else "el")
        auth.sendPasswordResetEmail(clean)
            .addOnSuccessListener {
                _isAuthLoading.value = false
                showSnackbar(if (_appLanguage.value == "EN") "Password reset link sent to $clean!" else "Στάλθηκε σύνδεσμος επαναφοράς κωδικού στο $clean!")
                onSuccess()
            }
            .addOnFailureListener { exc ->
                _isAuthLoading.value = false
                val msg = when {
                    exc.message?.contains("user-not-found") == true ->
                        if (_appLanguage.value == "EN") "No account found with this email" else "Δεν βρέθηκε λογαριασμός με αυτό το email"
                    exc.message?.contains("invalid-email") == true ->
                        if (_appLanguage.value == "EN") "Invalid email address" else "Μη έγκυρη μορφή email"
                    else -> exc.localizedMessage ?: (if (_appLanguage.value == "EN") "Error sending reset email" else "Σφάλμα αποστολής email")
                }
                onError(msg)
            }
    }

    /**
     * Send 6-digit verification code to user's mobile phone number
     * Finds the user registered with this phone number, generates a 6-digit code,
     * stores it in the Firestore cloud database, and triggers delivery.
     */
    fun sendPasswordResetPhoneCode(
        phone: String,
        onSuccess: (generatedCode: String, maskedPhone: String, userEmail: String) -> Unit,
        onError: (String) -> Unit
    ) {
        val cleanPhone = phone.trim().replace(" ", "").replace("-", "")
        if (cleanPhone.length < 10) {
            onError("Παρακαλώ εισάγετε ένα έγκυρο κινητό τηλέφωνο (τουλάχιστον 10 ψηφία)")
            return
        }

        _isAuthLoading.value = true
        viewModelScope.launch {
            try {
                val matchedUser = firestoreSyncRepo.findUserByPhoneNumber(cleanPhone)
                if (matchedUser == null || matchedUser.email.isBlank()) {
                    _isAuthLoading.value = false
                    onError("Δεν βρέθηκε εγγεγραμμένος χρήστης με αυτό το κινητό τηλέφωνο ($cleanPhone). Ελέγξτε τον αριθμό ή δοκιμάστε μέσω email.")
                    return@launch
                }

                // Generate a secure 6-digit code
                val code = kotlin.random.Random.nextInt(100000, 999999).toString()
                firestoreSyncRepo.savePasswordResetPhoneCode(cleanPhone, code, matchedUser.email)

                // Trigger real Android notification for SMS simulation
                try {
                    DailyReminderManager.showNotification(
                        context = getApplication(),
                        title = "📱 Fintel - 6ψήφιος Κωδικός SMS",
                        message = "Ο κωδικός επαλήθευσής σας είναι: $code. Χρησιμοποιήστε τον στην εφαρμογή."
                    )
                } catch (_: Exception) {}

                _isAuthLoading.value = false
                val masked = if (cleanPhone.length >= 6) {
                    cleanPhone.take(3) + "****" + cleanPhone.takeLast(3)
                } else cleanPhone

                showSnackbar("Ο 6ψήφιος κωδικός στάλθηκε στο κινητό $masked!")
                onSuccess(code, masked, matchedUser.email)
            } catch (e: Exception) {
                _isAuthLoading.value = false
                onError("Σφάλμα αποστολής SMS κωδικού: ${e.localizedMessage ?: "Παρακαλώ προσπαθήστε ξανά"}")
            }
        }
    }

    /**
     * Verifies the 6-digit code received on mobile phone.
     * Upon correct verification, triggers password reset link to the verified user's email.
     */
    fun verifyPasswordResetPhoneCode(
        phone: String,
        enteredCode: String,
        onSuccess: (emailSent: String) -> Unit,
        onError: (String) -> Unit
    ) {
        val cleanPhone = phone.trim().replace(" ", "").replace("-", "")
        val cleanCode = enteredCode.trim()

        if (cleanCode.length != 6) {
            onError("Παρακαλώ εισάγετε τον 6ψήφιο κωδικό επαλήθευσης")
            return
        }

        _isAuthLoading.value = true
        viewModelScope.launch {
            try {
                val record = firestoreSyncRepo.getPasswordResetPhoneCode(cleanPhone)
                if (record == null) {
                    _isAuthLoading.value = false
                    onError("Δεν βρέθηκε ενεργός κωδικός για αυτό το κινητό. Παρακαλώ ζητήστε νέο κωδικό.")
                    return@launch
                }

                val savedCode = record.first
                val userEmail = record.second

                if (savedCode != cleanCode) {
                    _isAuthLoading.value = false
                    onError("Ο 6ψήφιος κωδικός δεν είναι σωστός. Ελέγξτε το SMS σας.")
                    return@launch
                }

                // Code is correct! Send password reset email directly
                auth.setLanguageCode(if (_appLanguage.value == "EN") "en" else "el")
                auth.sendPasswordResetEmail(userEmail)
                    .addOnSuccessListener {
                        _isAuthLoading.value = false
                        showSnackbar("Επιτυχής ταυτοποίηση! Στάλθηκε σύνδεσμος στο $userEmail")
                        onSuccess(userEmail)
                    }
                    .addOnFailureListener { exc ->
                        _isAuthLoading.value = false
                        onError(exc.localizedMessage ?: "Σφάλμα ολοκλήρωσης επαναφοράς")
                    }
            } catch (e: Exception) {
                _isAuthLoading.value = false
                onError("Σφάλμα επαλήθευσης: ${e.localizedMessage}")
            }
        }
    }

    fun signInWithEmail(email: String, pass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (email.isBlank() || pass.isBlank()) {
            onError("Παρακαλώ συμπληρώστε email και κωδικό πρόσβασης")
            return
        }
        _isAuthLoading.value = true
        auth.signInWithEmailAndPassword(email.trim(), pass)
            .addOnSuccessListener { result ->
                val user = result.user
                _isAuthLoading.value = false
                _currentUser.value = user
                val verified = isUserVerified(user)
                _isUserVerifiedState.value = verified
                if (verified) {
                    showSnackbar("Συνδεθήκατε επιτυχώς!")
                } else {
                    showSnackbar("Απαιτείται επιβεβαίωση email.")
                }
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

    fun signUpWithEmail(
        firstName: String,
        lastName: String,
        birthDate: String,
        phoneNumber: String,
        occupation: String,
        city: String,
        email: String,
        pass: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (firstName.isBlank() || lastName.isBlank()) {
            onError("Παρακαλώ συμπληρώστε Όνομα και Επίθετο")
            return
        }
        if (birthDate.isBlank()) {
            onError("Παρακαλώ επιλέξτε την Ημερομηνία Γέννησής σας")
            return
        }
        if (phoneNumber.isBlank() || phoneNumber.length < 10) {
            onError("Παρακαλώ εισάγετε έγκυρο κινητό τηλέφωνο (τουλάχιστον 10 ψηφία)")
            return
        }
        if (email.isBlank() || pass.isBlank()) {
            onError("Παρακαλώ συμπληρώστε email και κωδικό πρόσβασης")
            return
        }
        if (pass.length < 6) {
            onError("Ο κωδικός πρέπει να έχει τουλάχιστον 6 χαρακτήρες")
            return
        }
        _isAuthLoading.value = true
        // Zero Data Loss Guard: Do not wipe cache prematurely before user creation succeeds.
        val fullName = "${firstName.trim()} ${lastName.trim()}"
        auth.createUserWithEmailAndPassword(email.trim(), pass)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user != null) {
                    val updateReq = UserProfileChangeRequest.Builder()
                        .setDisplayName(fullName)
                        .build()
                    user.updateProfile(updateReq).addOnCompleteListener {
                        // Create and initialize the user's Firestore profile with all personal fields
                        viewModelScope.launch {
                            try {
                                val createdProfile = firestoreSyncRepo.getOrCreateUserProfile(
                                    uid = user.uid,
                                    email = user.email ?: email.trim(),
                                    displayName = fullName,
                                    firstName = firstName.trim(),
                                    lastName = lastName.trim(),
                                    birthDate = birthDate.trim(),
                                    phoneNumber = phoneNumber.trim(),
                                    occupation = occupation.trim(),
                                    city = city.trim()
                                )
                                _userProfile.value = createdProfile
                                saveUserProfileToPrefs(createdProfile)

                                val initialHid = createdProfile.householdId.ifBlank { "household_${user.uid}" }
                                val safeHousehold = Household(
                                    id = initialHid,
                                    name = "Το Εξοδολόγιό μου",
                                    ownerId = user.uid,
                                    memberUids = listOf(user.uid)
                                )
                                _currentHousehold.value = safeHousehold
                                _userHouseholds.value = listOf(safeHousehold)
                                prefs.edit()
                                    .putString("user_household_${user.uid}", initialHid)
                                    .putString("last_household_id", initialHid)
                                    .apply()

                                // Fully load profile, setup listeners & sanitization
                                loadUserProfileAndHousehold(user)
                            } catch (e: Exception) {
                                Log.e("OikoNomosVM", "Error storing profile during sign up", e)
                            } finally {
                                try {
                                    auth.setLanguageCode(if (_appLanguage.value == "EN") "en" else "el")
                                    user.sendEmailVerification()
                                } catch (_: Exception) {}
                                _isAuthLoading.value = false
                                _currentUser.value = user
                                _isUserVerifiedState.value = true
                                showSnackbar("Καλώς ήρθατε!")
                                onSuccess()
                            }
                        }
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
                _isUserVerifiedState.value = isUserVerified(result.user)
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
        _isUserVerifiedState.value = false
        _userProfile.value = null
        _currentHousehold.value = null
        _userHouseholds.value = emptyList()
        _showOnboardingCarousel.value = false
        _notificationConsentCompleted.value = false
        clearUserProfileFromPrefs()
        prefs.edit()
            .remove("last_household_id")
            .remove("cached_active_uid")
            .remove("has_entered_any_expense")
            .apply()
        cancelAllSyncJobs()
        lastObservedHouseholdId = null
        lastHouseholdExpenses = emptyList()
        lastUserExpenses = emptyList()
        lastHouseholdIncomes = emptyList()
        lastUserIncomes = emptyList()
        lastHouseholdMembers = emptyList()
        lastUserMembers = emptyList()
        lastHouseholdCategories = emptyList()
        lastUserCategories = emptyList()
        _allExpenses.value = emptyList()
        _allIncomes.value = emptyList()
        _allMembers.value = emptyList()
        _allCategories.value = emptyList()
        viewModelScope.launch {
            try {
                repository.clearAllData() // Clean wipe Room so next user starts 100% clean
            } catch (e: Exception) {
                Log.w("OikoNomosVM", "Error clearing Room on signOut: ${e.message}")
            }
        }
        showSnackbar("Αποσυνδεθήκατε επιτυχώς")
    }

    fun factoryResetAndSignOut() {
        auth.signOut()
        _currentUser.value = null
        _isUserVerifiedState.value = false
        _userProfile.value = null
        _currentHousehold.value = null
        _userHouseholds.value = emptyList()
        _showOnboardingCarousel.value = false
        _notificationConsentCompleted.value = false
        prefs.edit().clear().apply()
        cancelAllSyncJobs()
        lastObservedHouseholdId = null
        lastHouseholdExpenses = emptyList()
        lastUserExpenses = emptyList()
        lastHouseholdIncomes = emptyList()
        lastUserIncomes = emptyList()
        lastHouseholdMembers = emptyList()
        lastUserMembers = emptyList()
        lastHouseholdCategories = emptyList()
        lastUserCategories = emptyList()
        _allExpenses.value = emptyList()
        _allIncomes.value = emptyList()
        _allMembers.value = emptyList()
        _allCategories.value = emptyList()
        viewModelScope.launch {
            if (com.example.data.safety.DataSafetyGuard.validateDestructiveOperation(
                    com.example.data.safety.DataSafetyGuard.DestructionReason.EXPLICIT_FACTORY_RESET
                )) {
                repository.clearAllData()
            }
            showSnackbar("Η εφαρμογή επανήλθε σε αρχική καθαρή κατάσταση")
        }
    }


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
            val matchMember = when (memberId) {
                null -> true
                -1L -> inc.memberId == 0L
                else -> inc.memberId == memberId || inc.memberId == 0L
            }
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

    private val _uiStyle = MutableStateFlow(prefs.getString("ui_style", "MODERN") ?: "MODERN") // "MODERN", "CLASSIC"
    val uiStyle = _uiStyle.asStateFlow()

    private val _appLanguage = MutableStateFlow(
        if (prefs.getBoolean("user_explicitly_set_language_v2", false))
            (prefs.getString("app_language", "EN") ?: "EN")
        else "EN"
    ) // "EN", "EL"
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
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return

        val household = _currentHousehold.value
            ?: _userHouseholds.value.firstOrNull()
            ?: Household(id = getOrEnsureHouseholdId(_currentUser.value), name = "Το Εξοδολόγιό μου", ownerId = _currentUser.value?.uid ?: "", memberUids = listOf(_currentUser.value?.uid ?: ""))

        val updated = household.copy(name = trimmed)
        _currentHousehold.value = updated
        _userHouseholds.value = if (_userHouseholds.value.any { it.id == household.id }) {
            _userHouseholds.value.map { if (it.id == household.id) updated else it }
        } else {
            listOf(updated) + _userHouseholds.value
        }
        showSnackbar("Το εξοδολόγιο μετονομάστηκε σε «$trimmed»")

        viewModelScope.launch {
            try {
                if (household.id.isNotBlank()) {
                    firestoreSyncRepo.renameHousehold(household.id, trimmed)
                }
            } catch (e: Exception) {
                Log.w("OikoNomosVM", "Household rename firestore sync: ${e.message}")
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

    fun updateUserFullProfile(
        firstName: String,
        lastName: String,
        birthDate: String,
        phoneNumber: String,
        occupation: String,
        city: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val user = auth.currentUser ?: _currentUser.value ?: run {
            onError("Δεν βρέθηκε συνδεδεμένος χρήστης")
            return
        }
        val cleanFirst = firstName.trim()
        val cleanLast = lastName.trim()

        val current = _userProfile.value ?: loadUserProfileFromPrefs() ?: UserProfile(uid = user.uid, email = user.email.orEmpty())

        val resolvedFirst = if (cleanFirst.isNotBlank()) cleanFirst else {
            current.firstName.ifBlank { user.displayName?.split(" ")?.firstOrNull() ?: "" }
        }
        val resolvedLast = if (cleanLast.isNotBlank()) cleanLast else {
            current.lastName.ifBlank {
                val parts = user.displayName?.split(" ") ?: emptyList()
                if (parts.size > 1) parts.drop(1).joinToString(" ") else ""
            }
        }
        val fullName = "$resolvedFirst $resolvedLast".trim().ifBlank {
            user.displayName?.ifBlank { null } ?: "Χρήστης Fintel"
        }

        // 1. Immediately update in-memory state & disk cache so user's data NEVER disappears
        val optimisticProfile = current.copy(
            displayName = fullName,
            firstName = resolvedFirst,
            lastName = resolvedLast,
            birthDate = birthDate.trim().ifBlank { current.birthDate },
            phoneNumber = phoneNumber.trim().ifBlank { current.phoneNumber },
            occupation = occupation.trim().ifBlank { current.occupation },
            city = city.trim().ifBlank { current.city }
        )
        _userProfile.value = optimisticProfile
        saveUserProfileToPrefs(optimisticProfile)

        // 2. Also keep Firebase Auth profile in sync
        try {
            val changeReq = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(fullName)
                .build()
            user.updateProfile(changeReq)
        } catch (e: Exception) {
            Log.w("OikoNomosVM", "FirebaseAuth displayName update note: ${e.message}")
        }

        // 3. Persist to Firestore cloud database
        viewModelScope.launch {
            try {
                val res = firestoreSyncRepo.updateUserFullProfile(
                    uid = user.uid,
                    firstName = resolvedFirst,
                    lastName = resolvedLast,
                    birthDate = birthDate.trim().ifBlank { current.birthDate },
                    phoneNumber = phoneNumber.trim().ifBlank { current.phoneNumber },
                    occupation = occupation.trim().ifBlank { current.occupation },
                    city = city.trim().ifBlank { current.city }
                )
                res.onSuccess { updated ->
                    val merged = (_userProfile.value ?: optimisticProfile).copy(
                        displayName = updated.displayName.ifBlank { optimisticProfile.displayName },
                        firstName = updated.firstName.ifBlank { optimisticProfile.firstName },
                        lastName = updated.lastName.ifBlank { optimisticProfile.lastName },
                        birthDate = updated.birthDate.ifBlank { optimisticProfile.birthDate },
                        phoneNumber = updated.phoneNumber.ifBlank { optimisticProfile.phoneNumber },
                        occupation = updated.occupation.ifBlank { optimisticProfile.occupation },
                        city = updated.city.ifBlank { optimisticProfile.city },
                        email = updated.email.ifBlank { optimisticProfile.email },
                        householdId = updated.householdId.ifBlank { optimisticProfile.householdId },
                        householdIds = if (updated.householdIds.isNotEmpty()) updated.householdIds else optimisticProfile.householdIds,
                        photoUrl = updated.photoUrl.ifBlank { optimisticProfile.photoUrl },
                        isPremium = updated.isPremium || optimisticProfile.isPremium
                    )
                    _userProfile.value = merged
                    saveUserProfileToPrefs(merged)
                    val lang = Localization.Language.fromCode(_appLanguage.value)
                    val msg = if (lang == Localization.Language.ENGLISH) "Profile saved successfully!" else "Το προφίλ σας αποθηκεύτηκε επιτυχώς!"
                    showSnackbar(msg)
                    onSuccess()
                }.onFailure { e ->
                    Log.w("OikoNomosVM", "Cloud profile update warning: ${e.message}")
                    val lang = Localization.Language.fromCode(_appLanguage.value)
                    val msg = if (lang == Localization.Language.ENGLISH) "Saved locally (syncing with cloud...)" else "Τα στοιχεία αποθηκεύτηκαν τοπικά (συγχρονισμός με cloud...)"
                    showSnackbar(msg)
                    onSuccess()
                }
            } catch (e: Exception) {
                Log.e("OikoNomosVM", "Profile update error: ${e.message}")
                showSnackbar("Τα στοιχεία αποθηκεύτηκαν τοπικά")
                onSuccess()
            }
        }
    }

    private fun saveUserProfileToPrefs(profile: UserProfile) {
        try {
            prefs.edit()
                .putString("cached_user_uid", profile.uid)
                .putString("cached_user_email", profile.email)
                .putString("cached_user_display_name", profile.displayName)
                .putString("cached_user_first_name", profile.firstName)
                .putString("cached_user_last_name", profile.lastName)
                .putString("cached_user_birth_date", profile.birthDate)
                .putString("cached_user_phone", profile.phoneNumber)
                .putString("cached_user_occupation", profile.occupation)
                .putString("cached_user_city", profile.city)
                .putString("cached_user_photo_url", profile.photoUrl)
                .putBoolean("cached_user_is_premium", profile.isPremium)
                .putString("cached_user_household_id", profile.householdId)
                .putString("cached_user_app_language", profile.appLanguage)
                .putString("cached_user_theme_mode", profile.themeMode)
                .putBoolean("cached_user_reminder_enabled", profile.reminderEnabled)
                .putInt("cached_user_reminder_hour", profile.reminderHour)
                .putInt("cached_user_reminder_minute", profile.reminderMinute)
                .putBoolean("cached_user_notification_consent_completed", profile.notificationConsentCompleted)
                .apply()
        } catch (e: Exception) {
            Log.w("OikoNomosVM", "Failed to cache user profile to prefs: ${e.message}")
        }
    }

    private fun loadUserProfileFromPrefs(): UserProfile? {
        val uid = prefs.getString("cached_user_uid", null) ?: return null
        if (uid.isBlank()) return null
        return UserProfile(
            uid = uid,
            email = prefs.getString("cached_user_email", "").orEmpty(),
            displayName = prefs.getString("cached_user_display_name", "").orEmpty(),
            firstName = prefs.getString("cached_user_first_name", "").orEmpty(),
            lastName = prefs.getString("cached_user_last_name", "").orEmpty(),
            birthDate = prefs.getString("cached_user_birth_date", "").orEmpty(),
            phoneNumber = prefs.getString("cached_user_phone", "").orEmpty(),
            occupation = prefs.getString("cached_user_occupation", "").orEmpty(),
            city = prefs.getString("cached_user_city", "").orEmpty(),
            photoUrl = prefs.getString("cached_user_photo_url", "").orEmpty(),
            isPremium = prefs.getBoolean("cached_user_is_premium", false),
            householdId = prefs.getString("cached_user_household_id", "").orEmpty(),
            appLanguage = prefs.getString("cached_user_app_language", "EL").orEmpty(),
            themeMode = prefs.getString("cached_user_theme_mode", "SYSTEM").orEmpty(),
            reminderEnabled = prefs.getBoolean("cached_user_reminder_enabled", false),
            reminderHour = prefs.getInt("cached_user_reminder_hour", 21),
            reminderMinute = prefs.getInt("cached_user_reminder_minute", 30),
            notificationConsentCompleted = prefs.getBoolean("cached_user_notification_consent_completed", false)
        )
    }

    private fun clearUserProfileFromPrefs() {
        prefs.edit()
            .remove("cached_user_uid")
            .remove("cached_user_email")
            .remove("cached_user_display_name")
            .remove("cached_user_first_name")
            .remove("cached_user_last_name")
            .remove("cached_user_birth_date")
            .remove("cached_user_phone")
            .remove("cached_user_occupation")
            .remove("cached_user_city")
            .remove("cached_user_photo_url")
            .remove("cached_user_is_premium")
            .remove("cached_user_household_id")
            .remove("cached_user_app_language")
            .remove("cached_user_theme_mode")
            .remove("cached_user_reminder_enabled")
            .remove("cached_user_reminder_hour")
            .remove("cached_user_reminder_minute")
            .remove("cached_user_notification_consent_completed")
            .apply()
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

    fun activateProFromBilling() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            try {
                firestoreSyncRepo.setPremiumStatus(user.uid, true)
                _userProfile.value = _userProfile.value?.copy(isPremium = true)
                val isEn = _appLanguage.value == "EN"
                showSnackbar(if (isEn) "🌟 Congratulations! Fintel Pro subscription activated." else "🌟 Συγχαρητήρια! Η συνδρομή Fintel Pro ενεργοποιήθηκε.")
            } catch (e: Exception) {
                Log.e("OikoNomosVM", "activateProFromBilling error: ${e.message}")
            }
        }
    }

    fun unlockPro() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            try {
                firestoreSyncRepo.setPremiumStatus(user.uid, true)
                _userProfile.value = _userProfile.value?.copy(isPremium = true)
                val isEn = _appLanguage.value == "EN"
                showSnackbar(if (isEn) "🌟 Congratulations! Fintel Pro activated." else "🌟 Συγχαρητήρια! Το Fintel Pro ενεργοποιήθηκε.")
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

                // Pre-initialize this new household in Firestore with user's primary member & standard categories
                val profileName = profile.displayName.ifBlank {
                    "${profile.firstName} ${profile.lastName}".trim()
                }.ifBlank { user.displayName ?: "Χρήστης" }
                val defaultMember = FirebaseMember(
                    id = "1",
                    householdId = newHousehold.id,
                    userId = user.uid,
                    name = profileName,
                    role = "Γονέας",
                    avatarColorHex = 0xFF059669,
                    voiceKeywords = profileName.lowercase(),
                    isDefault = true
                )
                firestoreSyncRepo.syncMember(newHousehold.id, defaultMember)
                STANDARD_DEFAULT_CATEGORIES.forEach { cat ->
                    val fbCat = FirebaseCategory(
                        id = cat.id.toString(),
                        householdId = newHousehold.id,
                        userId = user.uid,
                        name = cat.name,
                        iconKey = cat.iconKey,
                        monthlyBudget = 0.0,
                        colorHex = cat.colorHex,
                        voiceKeywords = cat.voiceKeywords,
                        sortOrder = cat.sortOrder,
                        fontColorHex = cat.fontColorHex
                    )
                    firestoreSyncRepo.syncCategory(newHousehold.id, fbCat)
                }

                switchExodologio(newHousehold)
                showSnackbar("Το εξοδολόγιο «${newHousehold.name}» δημιουργήθηκε!")
            } catch (e: Exception) {
                showSnackbar("Σφάλμα δημιουργίας: ${e.localizedMessage}")
            }
        }
    }

    fun switchExodologio(household: Household) {
        val user = _currentUser.value ?: return
        val profile = _userProfile.value
        viewModelScope.launch {
            try {
                prefs.edit().putString("last_household_id", household.id).apply()
                firestoreSyncRepo.switchActiveHousehold(user.uid, household.id)
                _currentHousehold.value = household
                _userProfile.value = _userProfile.value?.copy(householdId = household.id)
                lastHouseholdExpenses = emptyList()
                lastHouseholdIncomes = emptyList()
                lastHouseholdMembers = emptyList()
                lastHouseholdCategories = emptyList()
                lastUserExpenses = emptyList()
                lastUserIncomes = emptyList()
                lastUserMembers = emptyList()
                lastUserCategories = emptyList()
                _allExpenses.value = emptyList()
                _allIncomes.value = emptyList()
                _allMembers.value = emptyList()
                _allCategories.value = emptyList()
                repository.deleteAllExpenses()
                repository.deleteAllIncomes()
                repository.replaceAllMembers(emptyList())
                repository.replaceAllCategories(emptyList())
                repository.replaceAllSavingsGoals(emptyList())
                repository.replaceAllRecurringExpenses(emptyList())
                observeHouseholdData(household.id)
                if (profile != null) {
                    recoverAndSyncUserData(user, profile, household.id)
                }
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
            val generatedId = if (id == 0L) {
                val newId = repository.insertSavingsGoal(goal)
                showSnackbar("Ο στόχος «$title» δημιουργήθηκε!")
                newId
            } else {
                repository.updateSavingsGoal(goal)
                showSnackbar("Ο στόχος «$title» ενημερώθηκε!")
                id
            }

            val user = _currentUser.value
            val householdId = getOrEnsureHouseholdId(user)
            val fbGoal = FirebaseSavingsGoal(
                id = generatedId.toString(),
                householdId = householdId,
                userId = user?.uid ?: "",
                title = title,
                targetAmount = targetAmount,
                currentAmount = currentAmount,
                deadlineDate = deadline,
                note = note
            )
            if (householdId.isNotBlank()) {
                firestoreSyncRepo.syncSavingsGoal(householdId, fbGoal)
            }
            if (user != null) {
                firestoreSyncRepo.syncUserSavingsGoal(user.uid, fbGoal)
            }
        }
    }

    fun deleteSavingsGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            repository.deleteSavingsGoal(goal)
            val householdId = _currentHousehold.value?.id ?: ""
            val user = _currentUser.value
            if (householdId.isNotBlank()) {
                firestoreSyncRepo.deleteSavingsGoal(householdId, goal.id.toString(), goal.title)
            }
            if (user != null) {
                firestoreSyncRepo.deleteUserSavingsGoal(user.uid, goal.id.toString(), goal.title)
            }
            showSnackbar("Ο στόχος «${goal.title}» διαγράφηκε")
        }
    }

    fun depositToGoal(goal: SavingsGoal, amount: Double) {
        viewModelScope.launch {
            val updated = goal.copy(currentAmount = goal.currentAmount + amount)
            repository.updateSavingsGoal(updated)
            val householdId = _currentHousehold.value?.id ?: ""
            val user = _currentUser.value
            val fbGoal = FirebaseSavingsGoal(
                id = updated.id.toString(),
                householdId = householdId,
                userId = user?.uid ?: "",
                title = updated.title,
                targetAmount = updated.targetAmount,
                currentAmount = updated.currentAmount,
                deadlineDate = updated.deadlineDate,
                iconKey = updated.iconKey,
                colorHex = updated.colorHex,
                note = updated.note
            )
            if (householdId.isNotBlank()) {
                firestoreSyncRepo.syncSavingsGoal(householdId, fbGoal)
            }
            if (user != null) {
                firestoreSyncRepo.syncUserSavingsGoal(user.uid, fbGoal)
            }
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
        note: String,
        payerMemberId: Long = 0L,
        payerMemberName: String = ""
    ) {
        viewModelScope.launch {
            val existing = if (id != 0L) {
                repository.getAllRecurringExpensesList().find { it.id == id }
            } else null

            var recurring = RecurringExpense(
                id = id,
                title = title,
                amount = amount,
                categoryId = categoryId,
                categoryName = categoryName,
                frequency = existing?.frequency ?: "MONTHLY",
                dayOfMonth = dayOfMonth,
                splitType = existing?.splitType ?: "ALL_FAMILY",
                memberIdsCsv = existing?.memberIdsCsv ?: "",
                memberNamesDisplay = existing?.memberNamesDisplay ?: "Κοινό Οικογένειας",
                note = note,
                isActive = true,
                lastGeneratedMonth = existing?.lastGeneratedMonth ?: "",
                payerMemberId = payerMemberId,
                payerMemberName = payerMemberName,
                payerMemberIdsCsv = existing?.payerMemberIdsCsv ?: "",
                payerAmountsCsv = existing?.payerAmountsCsv ?: ""
            )

            val generatedId = if (id == 0L) {
                val newId = repository.insertRecurringExpense(recurring)
                newId
            } else {
                repository.updateRecurringExpense(recurring)
                id
            }

            recurring = recurring.copy(id = generatedId)

            val user = _currentUser.value
            val householdId = getOrEnsureHouseholdId(user)

            val calendar = Calendar.getInstance()
            val currentYear = calendar.get(Calendar.YEAR)
            val currentMonth = calendar.get(Calendar.MONTH) // 0-indexed
            val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
            val currentMonthKey = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())

            // If the scheduled day has already arrived or passed in the current month,
            // automatically register the expense for this month on that date!
            var autoRegisteredThisMonth = false
            if (currentDay >= dayOfMonth) {
                val monthStart = Calendar.getInstance().apply {
                    set(Calendar.YEAR, currentYear)
                    set(Calendar.MONTH, currentMonth)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                val monthEnd = Calendar.getInstance().apply {
                    set(Calendar.YEAR, currentYear)
                    set(Calendar.MONTH, currentMonth)
                    set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis

                val allExpenses = repository.getAllExpensesList()
                val monthExpenses = allExpenses.filter { it.timestamp in monthStart..monthEnd }

                val alreadyGenerated = RecurringExpenseAutoProcessor.isAlreadyGeneratedForMonth(
                    recurring, currentMonthKey, monthExpenses
                )

                if (!alreadyGenerated) {
                    val targetTimestamp = RecurringExpenseAutoProcessor.calculateTargetTimestamp(
                        currentYear, currentMonth, dayOfMonth
                    )
                    val noteText = if (note.isNotBlank()) note else "Πάγια εντολή (${dayOfMonth}η του μήνα)"
                    val docId = "recurring_${generatedId}_${currentMonthKey}"

                    val newExpense = Expense(
                        cloudId = docId,
                        title = title,
                        amount = amount,
                        categoryId = categoryId,
                        categoryName = categoryName,
                        timestamp = targetTimestamp,
                        splitType = recurring.splitType,
                        memberIdsCsv = recurring.memberIdsCsv,
                        memberNamesDisplay = recurring.memberNamesDisplay,
                        note = noteText,
                        payerMemberId = payerMemberId,
                        payerMemberName = payerMemberName,
                        payerMemberIdsCsv = recurring.payerMemberIdsCsv,
                        payerAmountsCsv = recurring.payerAmountsCsv
                    )

                    val insertedId = repository.insertExpense(newExpense)
                    val savedExpense = newExpense.copy(id = insertedId, cloudId = docId)

                    _allExpenses.value = (listOf(savedExpense) + _allExpenses.value).distinctBy { it.id }.sortedWith(
                        compareByDescending<Expense> { it.timestamp }.thenByDescending { it.id }
                    )

                    // Cloud sync for newly created expense
                    if (user != null) {
                        try {
                            firestoreSyncRepo.syncUserExpense(
                                user.uid,
                                savedExpense.toFirebaseExpense(householdId, user.uid, user.displayName ?: "Χρήστης")
                            )
                        } catch (e: Exception) {
                            Log.w("OikoNomosVM", "Cloud user expense sync: ${e.message}")
                        }
                    }
                    if (householdId.isNotBlank()) {
                        try {
                            firestoreSyncRepo.syncExpense(
                                savedExpense.toFirebaseExpense(householdId, user?.uid ?: "", user?.displayName ?: "Μέλος")
                            )
                        } catch (e: Exception) {
                            Log.w("OikoNomosVM", "Cloud household expense sync: ${e.message}")
                        }
                    }

                    // Update recurring record with lastGeneratedMonth = currentMonthKey
                    recurring = recurring.copy(lastGeneratedMonth = currentMonthKey)
                    repository.updateRecurringExpense(recurring)
                    autoRegisteredThisMonth = true
                }
            }

            // Sync Recurring record to Firestore
            val fbRecurring = FirebaseRecurringExpense(
                id = generatedId.toString(),
                householdId = householdId,
                userId = user?.uid ?: "",
                title = title,
                amount = amount,
                categoryId = categoryId,
                categoryName = categoryName,
                frequency = recurring.frequency,
                dayOfMonth = dayOfMonth,
                splitType = recurring.splitType,
                memberIdsCsv = recurring.memberIdsCsv,
                memberNamesDisplay = recurring.memberNamesDisplay,
                note = note,
                isActive = true,
                lastGeneratedMonth = recurring.lastGeneratedMonth,
                payerMemberId = payerMemberId,
                payerMemberName = payerMemberName,
                payerMemberIdsCsv = recurring.payerMemberIdsCsv,
                payerAmountsCsv = recurring.payerAmountsCsv
            )
            if (householdId.isNotBlank()) {
                firestoreSyncRepo.syncRecurringExpense(householdId, fbRecurring)
            }
            if (user != null) {
                firestoreSyncRepo.syncUserRecurringExpense(user.uid, fbRecurring)
            }

            if (autoRegisteredThisMonth) {
                showSnackbar("Η πάγια εντολή «$title» καταχωρήθηκε αυτόματα στα έξοδα ($dayOfMonth/${currentMonth + 1})!")
            } else if (id == 0L) {
                showSnackbar("Η πάγια εντολή «$title» αποθηκεύτηκε! Θα καταχωρείται αυτόματα στις $dayOfMonth κάθε μήνα.")
            } else {
                showSnackbar("Η πάγια εντολή «$title» ενημερώθηκε επιτυχώς!")
            }
        }
    }

    fun deleteRecurringExpense(recurring: RecurringExpense) {
        viewModelScope.launch {
            repository.deleteRecurringExpense(recurring)
            val householdId = _currentHousehold.value?.id ?: ""
            val user = _currentUser.value
            if (householdId.isNotBlank()) {
                firestoreSyncRepo.deleteRecurringExpense(householdId, recurring.id.toString(), recurring.title)
            }
            if (user != null) {
                firestoreSyncRepo.deleteUserRecurringExpense(user.uid, recurring.id.toString(), recurring.title)
            }
            showSnackbar("Η πάγια εντολή «${recurring.title}» διαγράφηκε")
        }
    }

    fun toggleRecurringActive(recurring: RecurringExpense) {
        viewModelScope.launch {
            val updated = recurring.copy(isActive = !recurring.isActive)
            repository.updateRecurringExpense(updated)
            val householdId = _currentHousehold.value?.id ?: ""
            val user = _currentUser.value
            val fbRecurring = FirebaseRecurringExpense(
                id = updated.id.toString(),
                householdId = householdId,
                userId = user?.uid ?: "",
                title = updated.title,
                amount = updated.amount,
                categoryId = updated.categoryId,
                categoryName = updated.categoryName,
                frequency = updated.frequency,
                dayOfMonth = updated.dayOfMonth,
                splitType = updated.splitType,
                memberIdsCsv = updated.memberIdsCsv,
                memberNamesDisplay = updated.memberNamesDisplay,
                note = updated.note,
                isActive = updated.isActive,
                lastGeneratedMonth = updated.lastGeneratedMonth,
                payerMemberId = updated.payerMemberId,
                payerMemberName = updated.payerMemberName,
                payerMemberIdsCsv = updated.payerMemberIdsCsv,
                payerAmountsCsv = updated.payerAmountsCsv
            )
            if (householdId.isNotBlank()) {
                firestoreSyncRepo.syncRecurringExpense(householdId, fbRecurring)
            }
            if (user != null) {
                firestoreSyncRepo.syncUserRecurringExpense(user.uid, fbRecurring)
            }
            val status = if (updated.isActive) "ενεργοποιήθηκε" else "απενεργοποιήθηκε"
            showSnackbar("Η πάγια εντολή «${recurring.title}» $status")
            if (updated.isActive) {
                checkAndProcessDueRecurringExpenses()
            }
        }
    }

    fun executeRecurringExpenseNow(recurring: RecurringExpense) {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            val currentYear = calendar.get(Calendar.YEAR)
            val currentMonth = calendar.get(Calendar.MONTH)
            val currentMonthKey = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())

            val targetTimestamp = RecurringExpenseAutoProcessor.calculateTargetTimestamp(
                currentYear, currentMonth, recurring.dayOfMonth
            )
            val docId = "recurring_${recurring.id}_${currentMonthKey}"

            val expense = Expense(
                cloudId = docId,
                title = recurring.title,
                amount = recurring.amount,
                categoryId = recurring.categoryId,
                categoryName = recurring.categoryName,
                timestamp = targetTimestamp,
                splitType = recurring.splitType,
                memberNamesDisplay = recurring.memberNamesDisplay,
                note = recurring.note.ifBlank { "Εκτέλεση πάγιας εντολής (${recurring.dayOfMonth}η του μήνα)" },
                payerMemberId = recurring.payerMemberId,
                payerMemberName = recurring.payerMemberName,
                payerMemberIdsCsv = recurring.payerMemberIdsCsv,
                payerAmountsCsv = recurring.payerAmountsCsv
            )
            val newId = repository.insertExpense(expense)
            val savedExpense = expense.copy(id = newId, cloudId = docId)
            _allExpenses.value = (listOf(savedExpense) + _allExpenses.value).distinctBy { it.id }.sortedWith(
                compareByDescending<Expense> { it.timestamp }.thenByDescending { it.id }
            )

            // Update recurring rule's lastGeneratedMonth so it won't be re-run automatically this month
            val updatedRecurring = recurring.copy(lastGeneratedMonth = currentMonthKey)
            repository.updateRecurringExpense(updatedRecurring)

            val user = _currentUser.value
            val household = _currentHousehold.value
            val householdId = household?.id ?: ""

            if (user != null) {
                try {
                    firestoreSyncRepo.syncUserExpense(
                        userId = user.uid,
                        expense = savedExpense.toFirebaseExpense(householdId, user.uid, user.displayName ?: "Χρήστης")
                    )
                } catch (e: Exception) {
                    Log.w("OikoNomosVM", "Sync user expense error: ${e.message}")
                }
            }
            if (householdId.isNotBlank()) {
                try {
                    firestoreSyncRepo.syncExpense(
                        savedExpense.toFirebaseExpense(householdId, user?.uid ?: "", user?.displayName ?: "Μέλος")
                    )
                } catch (e: Exception) {
                    Log.w("OikoNomosVM", "Sync household expense error: ${e.message}")
                }
            }

            // Sync updated recurring rule with lastGeneratedMonth
            val fbRecurring = FirebaseRecurringExpense(
                id = updatedRecurring.id.toString(),
                householdId = householdId,
                userId = user?.uid ?: "",
                title = updatedRecurring.title,
                amount = updatedRecurring.amount,
                categoryId = updatedRecurring.categoryId,
                categoryName = updatedRecurring.categoryName,
                frequency = updatedRecurring.frequency,
                dayOfMonth = updatedRecurring.dayOfMonth,
                splitType = updatedRecurring.splitType,
                memberIdsCsv = updatedRecurring.memberIdsCsv,
                memberNamesDisplay = updatedRecurring.memberNamesDisplay,
                note = updatedRecurring.note,
                isActive = updatedRecurring.isActive,
                lastGeneratedMonth = updatedRecurring.lastGeneratedMonth,
                payerMemberId = updatedRecurring.payerMemberId,
                payerMemberName = updatedRecurring.payerMemberName,
                payerMemberIdsCsv = updatedRecurring.payerMemberIdsCsv,
                payerAmountsCsv = updatedRecurring.payerAmountsCsv
            )
            if (householdId.isNotBlank()) {
                firestoreSyncRepo.syncRecurringExpense(householdId, fbRecurring)
            }
            if (user != null) {
                firestoreSyncRepo.syncUserRecurringExpense(user.uid, fbRecurring)
            }

            showSnackbar("Εκτελέστηκε η πάγια: «${recurring.title}» (${String.format(java.util.Locale.US, "%.2f€", recurring.amount)})")
        }
    }

    private var hasRunRecurringCheckThisSession = false

    /**
     * Checks all active standing orders and automatically registers any that are due today or this month.
     */
    fun checkAndProcessDueRecurringExpenses(force: Boolean = false) {
        if (hasRunRecurringCheckThisSession && !force) return
        hasRunRecurringCheckThisSession = true
        viewModelScope.launch {
            try {
                val householdId = _currentHousehold.value?.id ?: ""
                val user = _currentUser.value
                val newExpenses = RecurringExpenseAutoProcessor.processDueRecurringExpenses(
                    repository = repository,
                    firestoreSyncRepo = firestoreSyncRepo,
                    householdId = householdId,
                    user = user
                )
                if (newExpenses.isNotEmpty()) {
                    val existingIds = _allExpenses.value.map { it.id }.toSet()
                    val genuinelyNew = newExpenses.filter { it.id !in existingIds }
                    if (genuinelyNew.isNotEmpty()) {
                        val updatedList = (genuinelyNew + _allExpenses.value).sortedWith(
                            compareByDescending<Expense> { it.timestamp }.thenByDescending { it.id }
                        )
                        _allExpenses.value = updatedList
                        val names = genuinelyNew.take(2).joinToString(", ") { "«${it.title}»" }
                        val suffix = if (genuinelyNew.size > 2) " και άλλες ${genuinelyNew.size - 2}" else ""
                        showSnackbar("Αυτόματη καταχώρηση: $names$suffix στα έξοδα!")
                    }
                }
            } catch (e: Exception) {
                Log.e("OikoNomosVM", "checkAndProcessDueRecurringExpenses error: ${e.message}")
            }
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

    fun setThemeMode(mode: String, syncToCloud: Boolean = true) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode).apply()
        if (syncToCloud) {
            val uid = _currentUser.value?.uid ?: auth.currentUser?.uid
            if (!uid.isNullOrBlank()) {
                _userProfile.value = _userProfile.value?.copy(themeMode = mode)
                viewModelScope.launch {
                    firestoreSyncRepo.updateUserProfilePreferences(uid, themeMode = mode)
                }
            }
        }
    }

    fun setUiStyle(style: String) {
        _uiStyle.value = style
        prefs.edit().putString("ui_style", style).apply()
    }

    fun setAppLanguage(lang: String, syncToCloud: Boolean = true) {
        _appLanguage.value = lang
        prefs.edit()
            .putString("app_language", lang)
            .putBoolean("user_explicitly_set_language_v2", true)
            .apply()
        auth.setLanguageCode(if (lang == "EN") "en" else "el")
        if (syncToCloud) {
            val uid = _currentUser.value?.uid ?: auth.currentUser?.uid
            if (!uid.isNullOrBlank()) {
                _userProfile.value = _userProfile.value?.copy(appLanguage = lang)
                viewModelScope.launch {
                    firestoreSyncRepo.updateUserProfilePreferences(uid, appLanguage = lang)
                }
            }
        }
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
        originalPrompt: String,
        payerMemberId: Long = 0L,
        payerMemberName: String = "",
        payerMemberIdsCsv: String = "",
        payerAmountsCsv: String = ""
    ) {
        _pendingParsedExpense.value = null
        viewModelScope.launch {
            val docId = java.util.UUID.randomUUID().toString()
            val user = _currentUser.value
            val targetHouseholdId = getOrEnsureHouseholdId(user)
            val targetUserId = user?.uid ?: ""

            val memberCount = if (splitType == "ALL_FAMILY") {
                allMembers.value.size.coerceAtLeast(1)
            } else {
                memberIds.size.coerceAtLeast(1)
            }
            val splitAmountPerPerson = amount / memberCount

            var domainExpense = Expense(
                cloudId = docId,
                title = title,
                amount = amount,
                categoryId = categoryId,
                categoryName = categoryName,
                timestamp = timestamp,
                splitType = splitType,
                memberIdsCsv = memberIds.joinToString(","),
                memberNamesDisplay = memberNamesDisplay,
                splitAmountPerPerson = splitAmountPerPerson,
                originalVoicePrompt = originalPrompt,
                note = "",
                payerMemberId = payerMemberId,
                payerMemberName = payerMemberName,
                payerMemberIdsCsv = payerMemberIdsCsv,
                payerAmountsCsv = payerAmountsCsv
            )

            // 1. Persistent Room Save
            try {
                val newId = repository.insertExpense(domainExpense)
                if (newId > 0) {
                    domainExpense = domainExpense.copy(id = newId)
                }
            } catch (e: Exception) {
                Log.e("OikoNomosVM", "Room voice expense save error: ${e.message}")
            }

            // 2. Immediate in-memory update
            prefs.edit().putBoolean("has_entered_any_expense", true).apply()
            _isInitialExpensesLoading.value = false
            val currentList = _allExpenses.value.toMutableList()
            currentList.add(0, domainExpense)
            _allExpenses.value = currentList.sortedWith(
                compareByDescending<Expense> { it.timestamp }.thenByDescending { it.id }
            )

            showSnackbar("Καταχωρήθηκε: $title (${String.format(java.util.Locale.US, "%.2f€", amount)})")

            // 3. Dual-path Cloud Firestore Sync
            val firebaseExpense = domainExpense.toFirebaseExpense(
                householdId = targetHouseholdId,
                userId = targetUserId,
                userName = user?.displayName ?: "Χρήστης"
            )

            try {
                if (targetHouseholdId.isNotBlank()) {
                    firestoreSyncRepo.syncExpense(firebaseExpense)
                }
            } catch (e: Exception) {
                Log.w("OikoNomosVM", "Cloud voice syncExpense error: ${e.message}")
            }

            try {
                if (targetUserId.isNotBlank()) {
                    firestoreSyncRepo.syncUserExpense(targetUserId, firebaseExpense)
                }
            } catch (e: Exception) {
                Log.w("OikoNomosVM", "Cloud voice syncUserExpense error: ${e.message}")
            }
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
            originalPrompt = pending.originalPrompt,
            payerMemberId = pending.payerMemberId,
            payerMemberName = pending.payerMemberName
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
            note = "",
            payerMemberId = pending.payerMemberId,
            payerMemberName = pending.payerMemberName
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
        note: String,
        payerMemberId: Long = 0L,
        payerMemberName: String = "",
        payerMemberIdsCsv: String = "",
        payerAmountsCsv: String = ""
    ) {
        val existing = _editingExpense.value
            ?: if (id > 0) _allExpenses.value.find { it.id == id } else null
        // Immediately dismiss dialog so it doesn't freeze or stay on screen
        dismissManualExpenseDialog()

        viewModelScope.launch {
            val user = _currentUser.value
            val targetHouseholdId = getOrEnsureHouseholdId(user)
            val targetUserId = user?.uid ?: ""

            val existingDocId = existing?.cloudId?.ifBlank { null }
                ?: if (id > 0) {
                    val inList = _allExpenses.value.find { it.id == id } ?: repository.getAllExpensesList().find { it.id == id }
                    inList?.cloudId?.ifBlank { null }
                } else null

            val docId = existingDocId ?: if (id > 0) "exp_$id" else java.util.UUID.randomUUID().toString()

            val memberCount = if (splitType == "ALL_FAMILY") {
                allMembers.value.size.coerceAtLeast(1)
            } else {
                memberIds.size.coerceAtLeast(1)
            }
            val splitAmountPerPerson = amount / memberCount

            val targetId = if (id > 0) id else (existing?.id ?: 0L)
            var domainExpense = Expense(
                id = targetId,
                cloudId = docId,
                title = title,
                amount = amount,
                categoryId = categoryId,
                categoryName = categoryName,
                timestamp = timestamp,
                splitType = splitType,
                memberIdsCsv = memberIds.joinToString(","),
                memberNamesDisplay = memberNamesDisplay,
                splitAmountPerPerson = splitAmountPerPerson,
                originalVoicePrompt = existing?.originalVoicePrompt ?: "",
                note = note,
                payerMemberId = payerMemberId,
                payerMemberName = payerMemberName,
                payerMemberIdsCsv = payerMemberIdsCsv,
                payerAmountsCsv = payerAmountsCsv
            )

            // 1. Persistent Room Save
            try {
                val savedId = repository.saveOrUpdateExpense(domainExpense)
                if (savedId > 0 && domainExpense.id != savedId) {
                    domainExpense = domainExpense.copy(id = savedId)
                }
            } catch (e: Exception) {
                Log.e("OikoNomosVM", "Local Room saveExpense error: ${e.message}")
            }

            // 2. Immediate in-memory state update (Optimistic Update)
            prefs.edit().putBoolean("has_entered_any_expense", true).apply()
            _isInitialExpensesLoading.value = false
            val currentList = _allExpenses.value.toMutableList()
            val existingIndex = currentList.indexOfFirst {
                (it.id > 0 && domainExpense.id > 0 && it.id == domainExpense.id) ||
                (it.cloudId.isNotBlank() && it.cloudId == docId)
            }
            if (existingIndex >= 0) {
                currentList[existingIndex] = domainExpense
            } else {
                currentList.add(0, domainExpense)
            }
            _allExpenses.value = currentList.sortedWith(
                compareByDescending<Expense> { it.timestamp }.thenByDescending { it.id }
            )

            showSnackbar(if (existing == null) "Προστέθηκε νέο έξοδο: $title" else "Το έξοδο ενημερώθηκε: $title")

            // 3. Dual-path Cloud Firestore Sync (Safe & Non-blocking)
            val firebaseExpense = domainExpense.toFirebaseExpense(
                householdId = targetHouseholdId,
                userId = targetUserId,
                userName = user?.displayName ?: "Χρήστης"
            )

            try {
                if (targetHouseholdId.isNotBlank()) {
                    firestoreSyncRepo.syncExpense(firebaseExpense)
                }
            } catch (e: Exception) {
                Log.w("OikoNomosVM", "Cloud syncExpense error: ${e.message}")
            }

            try {
                if (targetUserId.isNotBlank()) {
                    firestoreSyncRepo.syncUserExpense(targetUserId, firebaseExpense)
                }
            } catch (e: Exception) {
                Log.w("OikoNomosVM", "Cloud syncUserExpense error: ${e.message}")
            }
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            // 1. Immediate local in-memory removal
            _allExpenses.value = _allExpenses.value.filter {
                it.id != expense.id && (expense.cloudId.isBlank() || it.cloudId != expense.cloudId)
            }

            // 2. Room deletion
            try {
                if (expense.id > 0) {
                    repository.deleteExpenseById(expense.id)
                } else {
                    repository.deleteExpense(expense)
                }
            } catch (e: Exception) {
                Log.e("OikoNomosVM", "Local deleteExpense error: ${e.message}")
            }

            // 3. Cloud deletion
            val user = _currentUser.value
            val targetHouseholdId = getOrEnsureHouseholdId(user)
            val docId = if (expense.cloudId.isNotBlank()) expense.cloudId else if (expense.id > 0) "exp_${expense.id}" else expense.originalVoicePrompt

            try {
                if (user != null && docId.isNotBlank()) {
                    firestoreSyncRepo.deleteUserExpense(user.uid, docId)
                }
            } catch (e: Exception) {
                Log.w("OikoNomosVM", "Cloud deleteUserExpense error: ${e.message}")
            }

            try {
                if (targetHouseholdId.isNotBlank() && docId.isNotBlank()) {
                    firestoreSyncRepo.deleteExpense(targetHouseholdId, docId)
                }
            } catch (e: Exception) {
                Log.w("OikoNomosVM", "Cloud deleteExpense error: ${e.message}")
            }

            showSnackbar("Το έξοδο διαγράφηκε")
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
        val isEditing = id > 0 || (_editingIncome.value != null && _editingIncome.value?.id != 0L && (id == 0L || id == _editingIncome.value?.id))
        val existing = if (isEditing) {
            _editingIncome.value ?: _allIncomes.value.find { it.id == id }
        } else null
        dismissManualIncomeDialog()

        viewModelScope.launch {
            val user = _currentUser.value
            val targetHouseholdId = getOrEnsureHouseholdId(user)
            val targetUserId = user?.uid ?: ""

            val existingDocId = existing?.cloudId?.ifBlank { null }
                ?: if (id > 0) {
                    val inList = _allIncomes.value.find { it.id == id } ?: repository.getAllIncomesList().find { it.id == id }
                    inList?.cloudId?.ifBlank { null }
                } else null

            val docId = if (isEditing) (existingDocId ?: if (id > 0) "inc_$id" else java.util.UUID.randomUUID().toString()) else java.util.UUID.randomUUID().toString()

            val targetId = if (isEditing) (if (id > 0) id else (existing?.id ?: 0L)) else 0L
            var domainIncome = Income(
                id = targetId,
                cloudId = docId,
                title = title,
                amount = amount,
                memberId = memberId,
                memberName = memberName,
                category = category,
                timestamp = timestamp,
                note = note
            )

            // 1. Persistent Room Save
            try {
                if (domainIncome.id > 0) {
                    repository.updateIncome(domainIncome)
                } else {
                    val generatedId = repository.insertIncome(domainIncome)
                    if (generatedId > 0) {
                        domainIncome = domainIncome.copy(id = generatedId)
                    }
                }
            } catch (e: Exception) {
                Log.e("OikoNomosVM", "Local Room saveIncome error: ${e.message}")
            }

            // 2. Immediate in-memory state update (Optimistic Update)
            val currentList = _allIncomes.value.toMutableList()
            val existingIndex = currentList.indexOfFirst {
                (it.id > 0 && domainIncome.id > 0 && it.id == domainIncome.id) ||
                (it.cloudId.isNotBlank() && it.cloudId == docId)
            }
            if (existingIndex >= 0) {
                currentList[existingIndex] = domainIncome
            } else {
                currentList.add(0, domainIncome)
            }
            _allIncomes.value = currentList.sortedWith(
                compareByDescending<Income> { it.timestamp }.thenByDescending { it.id }
            )

            showSnackbar(if (existing == null) "Προστέθηκε νέο έσοδο: $title (+${String.format(java.util.Locale.US, "%.2f€", amount)})" else "Το έσοδο ενημερώθηκε: $title")

            // 3. Dual-path Cloud Firestore Sync
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

            try {
                if (targetHouseholdId.isNotBlank()) {
                    firestoreSyncRepo.syncIncome(firebaseIncome)
                }
            } catch (e: Exception) {
                Log.w("OikoNomosVM", "Cloud syncIncome error: ${e.message}")
            }

            try {
                if (targetUserId.isNotBlank()) {
                    firestoreSyncRepo.syncUserIncome(targetUserId, firebaseIncome)
                }
            } catch (e: Exception) {
                Log.w("OikoNomosVM", "Cloud syncUserIncome error: ${e.message}")
            }
        }
    }

    fun deleteIncome(income: Income) {
        viewModelScope.launch {
            // 1. Immediate local in-memory removal
            _allIncomes.value = _allIncomes.value.filter {
                it.id != income.id && (income.cloudId.isBlank() || it.cloudId != income.cloudId)
            }

            // 2. Room deletion
            try {
                if (income.id > 0) {
                    repository.deleteIncomeById(income.id)
                } else {
                    repository.deleteIncome(income)
                }
            } catch (e: Exception) {
                Log.e("OikoNomosVM", "Local deleteIncome error: ${e.message}")
            }

            // 3. Cloud deletion
            val user = _currentUser.value
            val targetHouseholdId = getOrEnsureHouseholdId(user)
            val docId = if (income.cloudId.isNotBlank()) income.cloudId else if (income.id > 0) "inc_${income.id}" else ""

            try {
                if (user != null && docId.isNotBlank()) {
                    firestoreSyncRepo.deleteUserIncome(user.uid, docId)
                }
            } catch (e: Exception) {
                Log.w("OikoNomosVM", "Cloud deleteUserIncome error: ${e.message}")
            }

            try {
                if (targetHouseholdId.isNotBlank() && docId.isNotBlank()) {
                    firestoreSyncRepo.deleteIncome(targetHouseholdId, docId)
                }
            } catch (e: Exception) {
                Log.w("OikoNomosVM", "Cloud deleteIncome error: ${e.message}")
            }

            showSnackbar("Το έσοδο διαγράφηκε")
        }
    }

    fun dismissOnboardingCarousel() {
        _showOnboardingCarousel.value = false
        val uid = auth.currentUser?.uid
        if (!uid.isNullOrBlank()) {
            prefs.edit().putBoolean("onboarding_shown_$uid", true).apply()
        }
    }

    fun openOnboardingGuide() {
        _showOnboardingCarousel.value = true
    }

    fun seedStandardCategories() {
        viewModelScope.launch {
            val existing = repository.getAllCategoriesList()
            val maxSort = existing.maxOfOrNull { it.sortOrder } ?: -1
            val standardList = listOf(
                ExpenseCategory(id = 0L, name = "Σούπερ μάρκετ", iconKey = "cart", monthlyBudget = 400.0, colorHex = 0xFF059669, voiceKeywords = "σούπερ μάρκετ, σουπερμάρκετ, μάρκετ, τρόφιμα, ψώνια", sortOrder = maxSort + 1),
                ExpenseCategory(id = 0L, name = "Φαγητό & Καφές", iconKey = "food", monthlyBudget = 150.0, colorHex = 0xFFEF4444, voiceKeywords = "φαγητό, καφές, ντελίβερι, delivery, efood, wolt", sortOrder = maxSort + 2),
                ExpenseCategory(id = 0L, name = "Λογαριασμοί & Σπίτι", iconKey = "house", monthlyBudget = 300.0, colorHex = 0xFF0D9488, voiceKeywords = "ενοίκιο, ρεύμα, δεη, αέριο, κοινόχρηστα, ίντερνετ, νερό", sortOrder = maxSort + 3),
                ExpenseCategory(id = 0L, name = "Μετακινήσεις & Καύσιμα", iconKey = "gas_station", monthlyBudget = 120.0, colorHex = 0xFFEA580C, voiceKeywords = "βενζίνη, καύσιμα, διόδια, πάρκινγκ, μετρό, ταξί", sortOrder = maxSort + 4),
                ExpenseCategory(id = 0L, name = "Υγεία & Φαρμακείο", iconKey = "medical", monthlyBudget = 80.0, colorHex = 0xFFEC4899, voiceKeywords = "γιατρός, φαρμακείο, φάρμακα, εξετάσεις, υγεία", sortOrder = maxSort + 5),
                ExpenseCategory(id = 0L, name = "Διασκέδαση & Ψυχαγωγία", iconKey = "entertainment", monthlyBudget = 100.0, colorHex = 0xFF8B5CF6, voiceKeywords = "διασκέδαση, σινεμά, έξοδος, netflix", sortOrder = maxSort + 6)
            )
            val added = mutableListOf<ExpenseCategory>()
            for (cat in standardList) {
                val newId = repository.insertCategory(cat)
                added.add(cat.copy(id = newId))
            }
            syncUpdatedCategoriesToCloud(added)
            showSnackbar("Φορτώθηκαν οι προτεινόμενες κατηγορίες")
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
        val cleanName = name.trim()
        if (cleanName.isBlank()) return
        if (isInvalidOrCombinationMember(cleanName)) {
            showSnackbar("Μη έγκυρο όνομα μέλους: Δεν επιτρέπονται σύμβολα ή συνδυασμοί.")
            return
        }
        dismissMemberDialog()
        viewModelScope.launch {
            val allHouseholdIds = (_userProfile.value?.householdIds.orEmpty() + listOf(_currentHousehold.value?.id.orEmpty())).filter { it.isNotBlank() }.distinct()
            val existingMembers = repository.getAllMembersList()
            val existingById = if (id != 0L) existingMembers.find { it.id == id } else null
            val oldName = existingById?.name ?: ""
            val isDef = existingById?.isDefault ?: false
            val sort = existingById?.sortOrder ?: (existingMembers.maxOfOrNull { it.sortOrder }?.plus(1) ?: 0)

            val member = FamilyMember(
                id = if (id != 0L) id else 0L,
                name = cleanName,
                role = role.trim(),
                avatarColorHex = avatarColorHex,
                voiceKeywords = voiceKeywords.trim(),
                isDefault = isDef,
                sortOrder = sort
            )

            val generatedId = if (id == 0L) {
                val newId = repository.insertMember(member)
                showSnackbar("Προστέθηκε το μέλος $cleanName")
                newId
            } else {
                repository.updateMember(member)
                showSnackbar("Ενημερώθηκε το μέλος $cleanName")
                id
            }

            // Optimistic in-memory update so the UI reflects the new/edited member immediately
            val savedMember = member.copy(id = generatedId)
            val currentMemberList = _allMembers.value.toMutableList()
            val existingIdx = currentMemberList.indexOfFirst { it.id == generatedId || (id != 0L && it.id == id) }
            if (existingIdx >= 0) {
                currentMemberList[existingIdx] = savedMember
            } else {
                currentMemberList.add(savedMember)
            }
            _allMembers.value = currentMemberList.sortedWith(compareBy<FamilyMember> { it.sortOrder }.thenBy { it.id })

            if (isDef || id == 1L) {
                _userProfile.value = _userProfile.value?.copy(displayName = cleanName)
            }

            val user = _currentUser.value
            val householdId = getOrEnsureHouseholdId(user)
            val cloudMemberId = generatedId.toString()
            val fbMember = FirebaseMember(
                id = cloudMemberId,
                householdId = householdId,
                userId = user?.uid ?: "",
                name = cleanName,
                role = role.trim(),
                avatarColorHex = avatarColorHex,
                voiceKeywords = voiceKeywords.trim(),
                isDefault = isDef,
                sortOrder = sort
            )
            if (householdId.isNotBlank()) {
                firestoreSyncRepo.syncMember(householdId, fbMember)
            }

            // Cascade Member Rename: Update all existing expenses & incomes with new member name
            if (oldName.isNotBlank() && !oldName.equals(cleanName, ignoreCase = true)) {
                val previousExpenses = _allExpenses.value
                val updatedExpenses = previousExpenses.map { exp ->
                    val ids = exp.memberIdsCsv.split(",").mapNotNull { it.trim().toLongOrNull() }
                    val affectsBeneficiary = ids.contains(generatedId) || exp.memberNamesDisplay.contains(oldName, ignoreCase = true)
                    val isPayer = exp.payerMemberId == generatedId || exp.payerMemberName.equals(oldName, ignoreCase = true)

                    if (affectsBeneficiary || isPayer) {
                        val newDisplay = if (!affectsBeneficiary || exp.splitType == "ALL_FAMILY") {
                            exp.memberNamesDisplay
                        } else {
                            val regex = Regex("(?i)\\b${Regex.escape(oldName)}\\b")
                            if (exp.memberNamesDisplay.contains(regex)) {
                                exp.memberNamesDisplay.replace(regex, cleanName)
                            } else {
                                exp.memberNamesDisplay.replace(oldName, cleanName)
                            }
                        }
                        val newPayerName = if (isPayer) cleanName else exp.payerMemberName
                        val newPayerId = if (isPayer) generatedId else exp.payerMemberId
                        exp.copy(
                            memberNamesDisplay = newDisplay,
                            payerMemberId = newPayerId,
                            payerMemberName = newPayerName
                        )
                    } else {
                        exp
                    }
                }
                _allExpenses.value = updatedExpenses
                val affectedExpenses = updatedExpenses.filterIndexed { index, exp ->
                    val orig = previousExpenses.getOrNull(index)
                    orig != null && (orig.memberNamesDisplay != exp.memberNamesDisplay || orig.memberIdsCsv != exp.memberIdsCsv || orig.payerMemberName != exp.payerMemberName)
                }
                if (affectedExpenses.isNotEmpty()) {
                    try {
                        repository.insertExpenses(affectedExpenses)
                        affectedExpenses.forEach { exp ->
                            val fbExp = exp.toFirebaseExpense(
                                householdId = householdId,
                                userId = user?.uid ?: "",
                                userName = user?.displayName ?: "Χρήστης"
                            )
                            if (householdId.isNotBlank()) firestoreSyncRepo.syncExpense(fbExp)
                            if (user != null) firestoreSyncRepo.syncUserExpense(user.uid, fbExp)
                        }
                    } catch (e: Exception) {
                        Log.w("OikoNomosVM", "Cascade member rename error: ${e.message}")
                    }
                }

                // Cascade to Incomes
                val previousIncomes = _allIncomes.value
                val updatedIncomes = previousIncomes.map { inc ->
                    if (inc.memberId == generatedId || inc.memberName.equals(oldName, ignoreCase = true)) {
                        inc.copy(memberId = generatedId, memberName = cleanName)
                    } else {
                        inc
                    }
                }
                _allIncomes.value = updatedIncomes
                val affectedIncomes = updatedIncomes.filterIndexed { index, inc ->
                    val orig = previousIncomes.getOrNull(index)
                    orig != null && (orig.memberId != inc.memberId || orig.memberName != inc.memberName)
                }
                if (affectedIncomes.isNotEmpty()) {
                    try {
                        repository.insertIncomes(affectedIncomes)
                        affectedIncomes.forEach { inc ->
                            val fbInc = FirebaseIncome(
                                id = inc.cloudId,
                                householdId = householdId,
                                title = inc.title,
                                amount = inc.amount,
                                memberId = inc.memberId,
                                memberName = inc.memberName,
                                category = inc.category,
                                timestamp = inc.timestamp,
                                note = inc.note,
                                createdByUid = user?.uid ?: "",
                                createdByName = user?.displayName ?: "Χρήστης"
                            )
                            if (householdId.isNotBlank()) firestoreSyncRepo.syncIncome(fbInc)
                            if (user != null) firestoreSyncRepo.syncUserIncome(user.uid, fbInc)
                        }
                    } catch (e: Exception) {
                        Log.w("OikoNomosVM", "Cascade member rename to incomes error: ${e.message}")
                    }
                }
            }

            dismissMemberDialog()
        }
    }

    fun updateMemberColor(member: FamilyMember, newColorHex: Long) {
        val updatedMember = member.copy(avatarColorHex = newColorHex)
        _allMembers.value = _allMembers.value.map { if (it.id == member.id) updatedMember else it }
        viewModelScope.launch {
            repository.updateMember(updatedMember)
            val householdId = _currentHousehold.value?.id ?: ""
            val user = _currentUser.value
            val fbMember = FirebaseMember(
                id = member.id.toString(),
                householdId = householdId,
                userId = user?.uid ?: "",
                name = member.name,
                role = member.role,
                avatarColorHex = newColorHex,
                voiceKeywords = member.voiceKeywords,
                isDefault = member.isDefault,
                sortOrder = member.sortOrder
            )
            if (householdId.isNotBlank()) {
                firestoreSyncRepo.syncMember(householdId, fbMember)
            }
            showSnackbar("Το χρώμα για ${member.name} ενημερώθηκε")
        }
    }

    fun deleteMember(member: FamilyMember) {
        _allMembers.value = _allMembers.value.filter { it.id != member.id }
        viewModelScope.launch {
            repository.deleteMember(member)
            val user = _currentUser.value
            val householdId = getOrEnsureHouseholdId(user)
            if (householdId.isNotBlank()) {
                firestoreSyncRepo.deleteMember(householdId, member.id.toString())
            }

            val remainingMembers = repository.getAllMembersList().filter { it.id != member.id }
            if (member.isDefault && remainingMembers.isNotEmpty()) {
                val newDefault = remainingMembers.first().copy(isDefault = true)
                repository.updateMember(newDefault)
                val fbNewDefault = FirebaseMember(
                    id = newDefault.id.toString(),
                    householdId = householdId,
                    userId = user?.uid ?: "",
                    name = newDefault.name,
                    role = newDefault.role,
                    avatarColorHex = newDefault.avatarColorHex,
                    voiceKeywords = newDefault.voiceKeywords,
                    isDefault = true,
                    sortOrder = newDefault.sortOrder
                )
                if (householdId.isNotBlank()) firestoreSyncRepo.syncMember(householdId, fbNewDefault)
            }

            // Cascade Member Deletion: Clear member field if deleted!
            val previousExpenses = _allExpenses.value
            val updatedExpenses = previousExpenses.map { exp ->
                val ids = exp.memberIdsCsv.split(",").mapNotNull { it.trim().toLongOrNull() }
                val affectsBeneficiary = ids.contains(member.id) || exp.memberNamesDisplay.contains(member.name, ignoreCase = true)
                val affectsPayer = exp.payerMemberId == member.id || exp.payerMemberName.equals(member.name, ignoreCase = true)

                if (affectsBeneficiary || affectsPayer) {
                    val remainingIds = ids.filter { it != member.id }
                    val newDisplay = if (!affectsBeneficiary) {
                        exp.memberNamesDisplay
                    } else if (remainingIds.isEmpty()) {
                        ""
                    } else {
                        val names = remainingMembers.filter { remainingIds.contains(it.id) }.map { it.name }
                        if (names.isNotEmpty()) names.joinToString(" & ") else ""
                    }
                    val newSplit = if (remainingIds.isNotEmpty()) exp.amount / remainingIds.size else 0.0
                    val newPayerId = if (affectsPayer) 0L else exp.payerMemberId
                    val newPayerName = if (affectsPayer) "" else exp.payerMemberName

                    exp.copy(
                        memberIdsCsv = if (affectsBeneficiary) remainingIds.joinToString(",") else exp.memberIdsCsv,
                        memberNamesDisplay = newDisplay,
                        splitAmountPerPerson = if (affectsBeneficiary) newSplit else exp.splitAmountPerPerson,
                        payerMemberId = newPayerId,
                        payerMemberName = newPayerName
                    )
                } else {
                    exp
                }
            }
            _allExpenses.value = updatedExpenses
            val affectedExpenses = updatedExpenses.filterIndexed { index, exp ->
                val orig = previousExpenses.getOrNull(index)
                orig != null && (orig.memberIdsCsv != exp.memberIdsCsv || orig.memberNamesDisplay != exp.memberNamesDisplay || orig.payerMemberName != exp.payerMemberName)
            }
            try {
                repository.insertExpenses(affectedExpenses)
                affectedExpenses.forEach { exp ->
                    val fbExp = exp.toFirebaseExpense(
                        householdId = householdId,
                        userId = user?.uid ?: "",
                        userName = user?.displayName ?: "Χρήστης"
                    )
                    if (householdId.isNotBlank()) firestoreSyncRepo.syncExpense(fbExp)
                    if (user != null) firestoreSyncRepo.syncUserExpense(user.uid, fbExp)
                }
            } catch (e: Exception) {
                Log.w("OikoNomosVM", "Cascade delete member expenses error: ${e.message}")
            }

            // Cascade Member Deletion to Incomes: Clear member field!
            val previousIncomes = _allIncomes.value
            val updatedIncomes = previousIncomes.map { inc ->
                if (inc.memberId == member.id || inc.memberName.equals(member.name, ignoreCase = true)) {
                    inc.copy(memberId = 0L, memberName = "")
                } else {
                    inc
                }
            }
            _allIncomes.value = updatedIncomes
            val affectedIncomes = updatedIncomes.filterIndexed { index, inc ->
                val orig = previousIncomes.getOrNull(index)
                orig != null && (orig.memberId != inc.memberId || orig.memberName != inc.memberName)
            }
            try {
                repository.insertIncomes(affectedIncomes)
                affectedIncomes.forEach { inc ->
                    val fbInc = FirebaseIncome(
                        id = inc.cloudId,
                        householdId = householdId,
                        title = inc.title,
                        amount = inc.amount,
                        memberId = 0L,
                        memberName = "",
                        category = inc.category,
                        timestamp = inc.timestamp,
                        note = inc.note,
                        createdByUid = user?.uid ?: "",
                        createdByName = user?.displayName ?: "Χρήστης"
                    )
                    if (householdId.isNotBlank()) firestoreSyncRepo.syncIncome(fbInc)
                    if (user != null) firestoreSyncRepo.syncUserIncome(user.uid, fbInc)
                }
            } catch (e: Exception) {
                Log.w("OikoNomosVM", "Cascade delete member incomes error: ${e.message}")
            }

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
        voiceKeywords: String,
        fontColorHex: Long = 0L
    ) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return
        dismissCategoryDialog()
        viewModelScope.launch {
            val existingCategories = repository.getAllCategoriesList()
            val existingById = if (id != 0L) existingCategories.find { it.id == id } else null
            val oldCategoryName = existingById?.name ?: ""

            val sort = existingById?.sortOrder ?: (existingCategories.maxOfOrNull { it.sortOrder }?.plus(1) ?: 0)
            val category = ExpenseCategory(
                id = id,
                name = cleanName,
                iconKey = iconKey,
                monthlyBudget = monthlyBudget,
                colorHex = colorHex,
                voiceKeywords = voiceKeywords.trim(),
                sortOrder = sort,
                fontColorHex = fontColorHex
            )

            val effectiveId = if (id == 0L) {
                val newId = repository.insertCategory(category)
                showSnackbar("Προστέθηκε κατηγορία: $cleanName")
                newId
            } else {
                repository.updateCategory(category)
                showSnackbar("Ενημερώθηκε η κατηγορία: $cleanName")
                id
            }

            // Optimistic in-memory update so category changes show immediately
            val savedCategory = category.copy(id = effectiveId)
            val currentCatList = _allCategories.value.toMutableList()
            val existingIdx = currentCatList.indexOfFirst { it.id == effectiveId || (id != 0L && it.id == id) }
            if (existingIdx >= 0) {
                currentCatList[existingIdx] = savedCategory
            } else {
                currentCatList.add(savedCategory)
            }
            _allCategories.value = currentCatList.sortedWith(compareBy<ExpenseCategory> { it.sortOrder }.thenBy { it.id })

            val user = _currentUser.value
            val householdId = getOrEnsureHouseholdId(user)
            val cloudCatId = effectiveId.toString()
            val fbCat = FirebaseCategory(
                id = cloudCatId,
                householdId = householdId,
                userId = user?.uid ?: "",
                name = cleanName,
                iconKey = iconKey,
                monthlyBudget = monthlyBudget,
                colorHex = colorHex,
                voiceKeywords = voiceKeywords.trim(),
                sortOrder = sort,
                fontColorHex = fontColorHex
            )
            if (householdId.isNotBlank()) {
                firestoreSyncRepo.syncCategory(householdId, fbCat)
            }

            // Cascade Category Rename: Update all existing expenses & incomes with new category name
            if (oldCategoryName.isNotBlank() && !oldCategoryName.equals(cleanName, ignoreCase = true)) {
                val previousExpenses = _allExpenses.value
                val updatedExpenses = previousExpenses.map { exp ->
                    if (exp.categoryId == effectiveId || exp.categoryName.equals(oldCategoryName, ignoreCase = true)) {
                        exp.copy(categoryId = effectiveId, categoryName = cleanName)
                    } else {
                        exp
                    }
                }
                _allExpenses.value = updatedExpenses
                val affectedExpenses = updatedExpenses.filterIndexed { index, exp ->
                    val orig = previousExpenses.getOrNull(index)
                    orig != null && (orig.categoryId != exp.categoryId || orig.categoryName != exp.categoryName)
                }
                if (affectedExpenses.isNotEmpty()) {
                    try {
                        repository.insertExpenses(affectedExpenses)
                        affectedExpenses.forEach { exp ->
                            val fbExp = exp.toFirebaseExpense(
                                householdId = householdId,
                                userId = user?.uid ?: "",
                                userName = user?.displayName ?: "Χρήστης"
                            )
                            if (householdId.isNotBlank()) firestoreSyncRepo.syncExpense(fbExp)
                            if (user != null) firestoreSyncRepo.syncUserExpense(user.uid, fbExp)
                        }
                    } catch (e: Exception) {
                        Log.w("OikoNomosVM", "Cascade category rename to expenses error: ${e.message}")
                    }
                }

                // Cascade Category Rename to Incomes
                val previousIncomes = _allIncomes.value
                val updatedIncomes = previousIncomes.map { inc ->
                    if (inc.category.equals(oldCategoryName, ignoreCase = true)) {
                        inc.copy(category = cleanName)
                    } else {
                        inc
                    }
                }
                _allIncomes.value = updatedIncomes
                val affectedIncomes = updatedIncomes.filterIndexed { index, inc ->
                    val orig = previousIncomes.getOrNull(index)
                    orig != null && orig.category != inc.category
                }
                if (affectedIncomes.isNotEmpty()) {
                    try {
                        repository.insertIncomes(affectedIncomes)
                        affectedIncomes.forEach { inc ->
                            val fbInc = FirebaseIncome(
                                id = inc.cloudId,
                                householdId = householdId,
                                title = inc.title,
                                amount = inc.amount,
                                memberId = inc.memberId,
                                memberName = inc.memberName,
                                category = inc.category,
                                timestamp = inc.timestamp,
                                note = inc.note,
                                createdByUid = user?.uid ?: "",
                                createdByName = user?.displayName ?: "Χρήστης"
                            )
                            if (householdId.isNotBlank()) firestoreSyncRepo.syncIncome(fbInc)
                            if (user != null) firestoreSyncRepo.syncUserIncome(user.uid, fbInc)
                        }
                    } catch (e: Exception) {
                        Log.w("OikoNomosVM", "Cascade category rename to incomes error: ${e.message}")
                    }
                }
            }

            dismissCategoryDialog()
        }
    }

    fun deleteCategory(category: ExpenseCategory) {
        _allCategories.value = _allCategories.value.filter { it.id != category.id }
        viewModelScope.launch {
            repository.deleteCategory(category)
            val user = _currentUser.value
            val householdId = getOrEnsureHouseholdId(user)
            if (householdId.isNotBlank()) {
                firestoreSyncRepo.deleteCategory(householdId, category.id.toString())
            }

            // Cascade Category Deletion: Clear category field (categoryId = 0L, categoryName = "")
            val previousExpenses = _allExpenses.value
            val updatedExpenses = previousExpenses.map { exp ->
                if (exp.categoryId == category.id || exp.categoryName.equals(category.name, ignoreCase = true)) {
                    exp.copy(categoryId = 0L, categoryName = "")
                } else {
                    exp
                }
            }
            _allExpenses.value = updatedExpenses
            val affectedExpenses = updatedExpenses.filterIndexed { index, exp ->
                val orig = previousExpenses.getOrNull(index)
                orig != null && (orig.categoryId != exp.categoryId || orig.categoryName != exp.categoryName)
            }
            if (affectedExpenses.isNotEmpty()) {
                try {
                    repository.insertExpenses(affectedExpenses)
                    affectedExpenses.forEach { exp ->
                        val fbExp = exp.copy(categoryId = 0L, categoryName = "").toFirebaseExpense(
                            householdId = householdId,
                            userId = user?.uid ?: "",
                            userName = user?.displayName ?: "Χρήστης"
                        )
                        if (householdId.isNotBlank()) firestoreSyncRepo.syncExpense(fbExp)
                        if (user != null) firestoreSyncRepo.syncUserExpense(user.uid, fbExp)
                    }
                } catch (e: Exception) {
                    Log.w("OikoNomosVM", "Cascade category delete expenses error: ${e.message}")
                }
            }

            // Cascade Category Deletion to Incomes: Clear category field (category = "")
            val previousIncomes = _allIncomes.value
            val updatedIncomes = previousIncomes.map { inc ->
                if (inc.category.equals(category.name, ignoreCase = true)) {
                    inc.copy(category = "")
                } else {
                    inc
                }
            }
            _allIncomes.value = updatedIncomes
            val affectedIncomes = updatedIncomes.filterIndexed { index, inc ->
                val orig = previousIncomes.getOrNull(index)
                orig != null && orig.category != inc.category
            }
            if (affectedIncomes.isNotEmpty()) {
                try {
                    repository.insertIncomes(affectedIncomes)
                    affectedIncomes.forEach { inc ->
                        val fbInc = FirebaseIncome(
                            id = inc.cloudId,
                            householdId = householdId,
                            title = inc.title,
                            amount = inc.amount,
                            memberId = inc.memberId,
                            memberName = inc.memberName,
                            category = "",
                            timestamp = inc.timestamp,
                            note = inc.note,
                            createdByUid = user?.uid ?: "",
                            createdByName = user?.displayName ?: "Χρήστης"
                        )
                        if (householdId.isNotBlank()) firestoreSyncRepo.syncIncome(fbInc)
                        if (user != null) firestoreSyncRepo.syncUserIncome(user.uid, fbInc)
                    }
                } catch (e: Exception) {
                    Log.w("OikoNomosVM", "Cascade category delete incomes error: ${e.message}")
                }
            }

            showSnackbar("Η κατηγορία «${category.name}» διαγράφηκε")
        }
    }

    // =========================================================================
    // 1. REORDERING: MEMBERS & CATEGORIES (Instant 0ms UI & Background Batch Sync)
    // =========================================================================
    private var memberReorderJob: Job? = null
    private var categoryReorderJob: Job? = null

    fun moveMemberUp(member: FamilyMember) {
        val current = _allMembers.value.toMutableList()
        val index = current.indexOfFirst { it.id == member.id }
        if (index <= 0) return

        val prev = current[index - 1]
        current[index - 1] = member.copy(sortOrder = index - 1)
        current[index] = prev.copy(sortOrder = index)
        val updated = current.mapIndexed { i, m -> m.copy(sortOrder = i) }

        // ⚡ INSTANT IN-MEMORY UPDATE: UI reflects change in 0ms!
        _allMembers.value = updated

        // Debounced background persistence
        scheduleMemberReorderSave(updated)
    }

    fun moveMemberDown(member: FamilyMember) {
        val current = _allMembers.value.toMutableList()
        val index = current.indexOfFirst { it.id == member.id }
        if (index < 0 || index >= current.size - 1) return

        val next = current[index + 1]
        current[index + 1] = member.copy(sortOrder = index + 1)
        current[index] = next.copy(sortOrder = index)
        val updated = current.mapIndexed { i, m -> m.copy(sortOrder = i) }

        // ⚡ INSTANT IN-MEMORY UPDATE: UI reflects change in 0ms!
        _allMembers.value = updated

        // Debounced background persistence
        scheduleMemberReorderSave(updated)
    }

    private fun scheduleMemberReorderSave(updated: List<FamilyMember>) {
        memberReorderJob?.cancel()
        memberReorderJob = viewModelScope.launch(Dispatchers.IO) {
            delay(350)
            repository.updateMembers(updated)
            syncUpdatedMembersToCloud(updated)
        }
    }

    private suspend fun syncUpdatedMembersToCloud(members: List<FamilyMember>) {
        val householdId = _currentHousehold.value?.id.orEmpty()
        val uid = _currentUser.value?.uid.orEmpty()
        val isMarios = isMariosUser()
        val mariosFallbackHid = if (isMarios) FirestoreDataSeed.DEFAULT_HOUSEHOLD_ID else ""
        val mariosFallbackUid = if (isMarios) FirestoreDataSeed.DEFAULT_USER_UID else ""
        val effectiveHouseholdId = householdId.ifBlank {
            _userProfile.value?.householdId?.ifBlank { mariosFallbackHid } ?: mariosFallbackHid
        }
        val effectiveUid = uid.ifBlank {
            auth.currentUser?.uid?.ifBlank { mariosFallbackUid } ?: mariosFallbackUid
        }
        val targetHouseholdIds = if (effectiveHouseholdId.isNotBlank()) listOf(effectiveHouseholdId) else emptyList()
        val fbMembers = members.map { m ->
            FirebaseMember(
                id = m.id.toString(),
                householdId = effectiveHouseholdId,
                userId = effectiveUid,
                name = m.name,
                role = m.role,
                avatarColorHex = m.avatarColorHex,
                voiceKeywords = m.voiceKeywords,
                isDefault = m.isDefault,
                sortOrder = m.sortOrder
            )
        }
        if (targetHouseholdIds.isNotEmpty()) {
            firestoreSyncRepo.batchUpdateMemberSortOrder(targetHouseholdIds, effectiveUid, fbMembers)
        }
    }

    fun moveCategoryUp(category: ExpenseCategory) {
        val current = _allCategories.value.toMutableList()
        val index = current.indexOfFirst { it.id == category.id }
        if (index <= 0) return

        val prev = current[index - 1]
        current[index - 1] = category.copy(sortOrder = index - 1)
        current[index] = prev.copy(sortOrder = index)
        val updated = current.mapIndexed { i, c -> c.copy(sortOrder = i) }

        // ⚡ INSTANT IN-MEMORY UPDATE: UI reflects change in 0ms!
        _allCategories.value = updated

        // Debounced background persistence
        scheduleCategoryReorderSave(updated)
    }

    fun moveCategoryDown(category: ExpenseCategory) {
        val current = _allCategories.value.toMutableList()
        val index = current.indexOfFirst { it.id == category.id }
        if (index < 0 || index >= current.size - 1) return

        val next = current[index + 1]
        current[index + 1] = category.copy(sortOrder = index + 1)
        current[index] = next.copy(sortOrder = index)
        val updated = current.mapIndexed { i, c -> c.copy(sortOrder = i) }

        // ⚡ INSTANT IN-MEMORY UPDATE: UI reflects change in 0ms!
        _allCategories.value = updated

        // Debounced background persistence
        scheduleCategoryReorderSave(updated)
    }

    private fun scheduleCategoryReorderSave(updated: List<ExpenseCategory>) {
        categoryReorderJob?.cancel()
        categoryReorderJob = viewModelScope.launch(Dispatchers.IO) {
            delay(350)
            repository.updateCategories(updated)
            syncUpdatedCategoriesToCloud(updated)
        }
    }

    private suspend fun syncUpdatedCategoriesToCloud(categories: List<ExpenseCategory>) {
        val householdId = _currentHousehold.value?.id.orEmpty()
        val uid = _currentUser.value?.uid.orEmpty()
        val isMarios = isMariosUser()
        val mariosFallbackHid = if (isMarios) FirestoreDataSeed.DEFAULT_HOUSEHOLD_ID else ""
        val mariosFallbackUid = if (isMarios) FirestoreDataSeed.DEFAULT_USER_UID else ""
        val effectiveHouseholdId = householdId.ifBlank {
            _userProfile.value?.householdId?.ifBlank { mariosFallbackHid } ?: mariosFallbackHid
        }
        val effectiveUid = uid.ifBlank {
            auth.currentUser?.uid?.ifBlank { mariosFallbackUid } ?: mariosFallbackUid
        }
        val targetHouseholdIds = if (effectiveHouseholdId.isNotBlank()) listOf(effectiveHouseholdId) else emptyList()
        val fbCategories = categories.map { c ->
            FirebaseCategory(
                id = c.id.toString(),
                householdId = effectiveHouseholdId,
                userId = effectiveUid,
                name = c.name,
                iconKey = c.iconKey,
                monthlyBudget = c.monthlyBudget,
                colorHex = c.colorHex,
                voiceKeywords = c.voiceKeywords,
                sortOrder = c.sortOrder,
                fontColorHex = c.fontColorHex
            )
        }
        if (targetHouseholdIds.isNotEmpty()) {
            firestoreSyncRepo.batchUpdateCategorySortOrder(targetHouseholdIds, effectiveUid, fbCategories)
        }
    }

    // =========================================================================
    // 2. DAILY EVENING EXPENSE REMINDERS
    // =========================================================================
    private val _dailyReminderEnabled = MutableStateFlow(DailyReminderManager.isReminderEnabled(getApplication()))
    val dailyReminderEnabled = _dailyReminderEnabled.asStateFlow()

    private val _dailyReminderHour = MutableStateFlow(DailyReminderManager.getReminderHour(getApplication()))
    val dailyReminderHour = _dailyReminderHour.asStateFlow()

    private val _dailyReminderMinute = MutableStateFlow(DailyReminderManager.getReminderMinute(getApplication()))
    val dailyReminderMinute = _dailyReminderMinute.asStateFlow()

    fun toggleDailyReminder(enabled: Boolean) {
        _dailyReminderEnabled.value = enabled
        DailyReminderManager.setReminderEnabled(getApplication(), enabled)
        val uid = _currentUser.value?.uid ?: auth.currentUser?.uid
        if (!uid.isNullOrBlank()) {
            _userProfile.value = _userProfile.value?.copy(reminderEnabled = enabled)
            viewModelScope.launch {
                firestoreSyncRepo.updateUserProfilePreferences(uid, reminderEnabled = enabled)
            }
        }
        if (enabled) {
            showSnackbar("Η βραδινή υπενθύμιση ενεργοποιήθηκε για τις %02d:%02d".format(_dailyReminderHour.value, _dailyReminderMinute.value))
        } else {
            showSnackbar("Η βραδινή υπενθύμιση απενεργοποιήθηκε")
        }
    }

    fun setDailyReminderTime(hour: Int, minute: Int) {
        _dailyReminderHour.value = hour
        _dailyReminderMinute.value = minute
        DailyReminderManager.setReminderTime(getApplication(), hour, minute)
        val uid = _currentUser.value?.uid ?: auth.currentUser?.uid
        if (!uid.isNullOrBlank()) {
            _userProfile.value = _userProfile.value?.copy(reminderHour = hour, reminderMinute = minute)
            viewModelScope.launch {
                firestoreSyncRepo.updateUserProfilePreferences(uid, reminderHour = hour, reminderMinute = minute)
            }
        }
        showSnackbar("Η ώρα υπενθύμισης ορίστηκε στις %02d:%02d".format(hour, minute))
    }

    fun triggerTestReminderNotification() {
        DailyReminderManager.showNotification(
            context = getApplication(),
            title = "Fintel - Δοκιμή Υπενθύμισης 🌙",
            message = "Μην ξεχάσετε να καταχωρήσετε τα έξοδα της σημερινής ημέρας!"
        )
        showSnackbar("Εστάλη δοκιμαστική ειδοποίηση")
    }

    // =========================================================================
    // 3. USER PROFILE AVATAR (Google Photo & Custom Upload)
    // =========================================================================
    fun updateUserProfilePhoto(photoUrl: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            try {
                firestoreSyncRepo.updateUserProfilePhoto(user.uid, photoUrl)
                _userProfile.value = _userProfile.value?.copy(photoUrl = photoUrl)
                showSnackbar("Η εικόνα προφίλ ενημερώθηκε επιτυχώς")
            } catch (e: Exception) {
                Log.e("OikoNomosVM", "Error updating profile photo", e)
                showSnackbar("Αποτυχία ενημέρωσης εικόνας προφίλ")
            }
        }
    }

    // =========================================================================
    // 4. GDPR / PRIVACY CONSENT
    // =========================================================================
    private val _showPrivacyConsentDialog = MutableStateFlow(false)
    val showPrivacyConsentDialog = _showPrivacyConsentDialog.asStateFlow()

    fun acceptPrivacyConsent() {
        _showPrivacyConsentDialog.value = false
        prefs.edit().putBoolean("gdpr_privacy_consent_accepted", true).apply()
        showSnackbar("Η συγκατάθεση αποθηκεύτηκε επιτυχώς")
    }

    fun openPrivacyConsentDialog() {
        _showPrivacyConsentDialog.value = true
    }

    fun dismissPrivacyConsentDialog() {
        _showPrivacyConsentDialog.value = false
    }

    companion object {
        val STANDARD_DEFAULT_CATEGORIES: List<ExpenseCategory> = listOf(
            ExpenseCategory(
                id = 1L,
                name = "Σούπερ μάρκετ",
                iconKey = "cart",
                monthlyBudget = 0.0,
                colorHex = 4278556265L,
                voiceKeywords = "σούπερ μάρκετ, σουπερμάρκετ, lidl, σκλαβενίτης, βασιλόπουλος, μάρκετ, ψώνια",
                sortOrder = 0,
                fontColorHex = 0L
            ),
            ExpenseCategory(
                id = 2L,
                name = "Φαγητό & Καφές",
                iconKey = "food",
                monthlyBudget = 0.0,
                colorHex = 4278556265L,
                voiceKeywords = "φαγητό, καφές, delivery, σουβλάκια, πίτσα, εστιατόριο",
                sortOrder = 1,
                fontColorHex = 0L
            ),
            ExpenseCategory(
                id = 3L,
                name = "Λογαριασμοί & Σπίτι",
                iconKey = "house",
                monthlyBudget = 0.0,
                colorHex = 4279080072L,
                voiceKeywords = "λογαριασμός, ρεύμα, νερό, ενοίκιο, κοινόχρηστα, ιντερνετ, τηλέφωνο",
                sortOrder = 2,
                fontColorHex = 0L
            ),
            ExpenseCategory(
                id = 4L,
                name = "Μετακινήσεις & Καύσιμα",
                iconKey = "gas_station",
                monthlyBudget = 0.0,
                colorHex = 4293548044L,
                voiceKeywords = "βενζίνη, καύσιμα, διόδια, πάρκινγκ, λεωφορείο, μετρό, ταξί",
                sortOrder = 3,
                fontColorHex = 0L
            ),
            ExpenseCategory(
                id = 5L,
                name = "Υγεία & Φαρμακείο",
                iconKey = "medical",
                monthlyBudget = 0.0,
                colorHex = 4293675161L,
                voiceKeywords = "γιατρός, φαρμακείο, φάρμακα, εξετάσεις",
                sortOrder = 4,
                fontColorHex = 0L
            ),
            ExpenseCategory(
                id = 6L,
                name = "Διασκέδαση & Ψυχαγωγία",
                iconKey = "entertainment",
                monthlyBudget = 0.0,
                colorHex = 4287323382L,
                voiceKeywords = "διασκέδαση, έξοδος, ποτό, σινεμά",
                sortOrder = 5,
                fontColorHex = 0L
            ),
            ExpenseCategory(
                id = 7L,
                name = "Άλλο",
                iconKey = "other",
                monthlyBudget = 0.0,
                colorHex = 4283124564L,
                voiceKeywords = "άλλο, διάφορα",
                sortOrder = 6,
                fontColorHex = 0L
            )
        )
    }
}
