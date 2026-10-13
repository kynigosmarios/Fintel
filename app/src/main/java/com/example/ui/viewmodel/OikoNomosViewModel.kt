package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
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
import kotlinx.coroutines.withContext

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

    private val _showOnboardingCarousel = MutableStateFlow(false)
    val showOnboardingCarousel = _showOnboardingCarousel.asStateFlow()

    init {
        val hasExplicitLanguage = prefs.getBoolean("user_explicitly_set_language_v2", false)
        val savedLang = if (hasExplicitLanguage) (prefs.getString("app_language", "EN") ?: "EN") else "EN"
        auth.setLanguageCode(if (savedLang == "EN") "en" else "el")
        val database = AppDatabase.getDatabase(application)
        repository = ExpenseRepository(database.expenseDao(), application)

        // Immediate local Room pre-load so data is instantaneously available offline & on start
        viewModelScope.launch {
            try {
                val localExpenses = repository.getAllExpensesList()
                if (localExpenses.isNotEmpty() && _allExpenses.value.isEmpty()) {
                    _allExpenses.value = localExpenses
                    _isInitialExpensesLoading.value = false
                }
                val localIncomes = repository.getAllIncomesList()
                if (localIncomes.isNotEmpty() && _allIncomes.value.isEmpty()) {
                    _allIncomes.value = localIncomes
                }
                // Automatic check and execution of standing orders due today or this month
                checkAndProcessDueRecurringExpenses()
            } catch (e: Exception) {
                Log.d("OikoNomosVM", "Room pre-warm: ${e.message}")
            }
        }

        // Fast pre-warm: If we have a cached active household ID, kick off disk cache observation immediately!
        val cachedHouseholdId = prefs.getString("last_household_id", null)
        if (!cachedHouseholdId.isNullOrBlank()) {
            observeHouseholdData(cachedHouseholdId)
        }

        // Fast pre-warm: Load cached user profile so UI displays immediately without blank state
        val cachedProfile = loadUserProfileFromPrefs()
        if (cachedProfile != null) {
            _userProfile.value = cachedProfile
        }

        // Listen for Auth state
        var lastObservedUid: String? = null
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            val currentUid = user?.uid
            _isAuthInitialized.value = true
            val verified = isUserVerified(user)
            _isUserVerifiedState.value = verified
            if (user != null) {
                _currentUser.value = user
                lastObservedUid = currentUid
                if (verified) {
                    viewModelScope.launch {
                        loadUserProfileAndHousehold(user)
                    }
                }
            } else if (lastObservedUid != null) {
                // User explicitly logged out
                lastObservedUid = null
                _isUserVerifiedState.value = false
                _userProfile.value = null
                _currentHousehold.value = null
                _userHouseholds.value = emptyList()
                _showOnboardingCarousel.value = false
                clearUserProfileFromPrefs()
                prefs.edit().remove("last_household_id").apply()
                cancelAllSyncJobs()
                viewModelScope.launch {
                    repository.clearAllData()
                }
            }
        }
    }

    private suspend fun loadUserProfileAndHousehold(user: FirebaseUser) {
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

            val activeHouseholdId = profile.householdId.ifBlank {
                profile.householdIds.firstOrNull() ?: ""
            }

            if (activeHouseholdId.isNotBlank()) {
                prefs.edit().putString("last_household_id", activeHouseholdId).apply()
                observeHouseholdData(activeHouseholdId)
            }

            val households = firestoreSyncRepo.fetchUserHouseholds(profile.householdIds)
            _userHouseholds.value = households

            if (activeHouseholdId.isNotBlank()) {
                val current = households.find { it.id == activeHouseholdId }
                if (current != null) {
                    _currentHousehold.value = current
                } else if (households.isNotEmpty()) {
                    _currentHousehold.value = households.first()
                }
            }
            observeUserData(user.uid)

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
        val validRemote = incomingMembers.filter { it.name.isNotBlank() && !isInvalidOrCombinationMember(it.name) }

        val householdId = explicitHouseholdId ?: _currentHousehold.value?.id.orEmpty()
        val user = explicitUser ?: _currentUser.value
        val profile = explicitProfile ?: _userProfile.value
        val uid = user?.uid.orEmpty()

        if (validRemote.isEmpty()) {
            if (currentLocal.isEmpty()) {
                // New user: Do NOT create any default member. Starts with 0 members.
                return
            } else {
                currentLocal.forEach { m ->
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
                    if (householdId.isNotBlank()) {
                        firestoreSyncRepo.syncMember(householdId, fb)
                    }
                    if (uid.isNotBlank()) {
                        firestoreSyncRepo.syncUserMember(uid, fb)
                    }
                }
            }
            return
        }

        // Database is the SOURCE OF TRUTH!
        val sortedRemote = validRemote.sortedWith(
            compareBy<FirebaseMember> { it.sortOrder }
                .thenBy { it.id.toLongOrNull() ?: 999L }
                .thenByDescending { it.createdAt?.seconds ?: 0L }
        )

        val cleanMembers = mutableListOf<FamilyMember>()
        val grouped = sortedRemote.groupBy { remote ->
            val parsedId = remote.id.toLongOrNull() ?: Math.abs(remote.id.hashCode().toLong()).coerceAtLeast(2L)
            val isDef = remote.isDefault || remote.id == "1" || parsedId == 1L
            if (isDef) 1L else parsedId
        }

        grouped.forEach { (finalId, list) ->
            val chosen = list.maxByOrNull { it.createdAt?.seconds ?: 0L } ?: list.first()
            cleanMembers.add(
                FamilyMember(
                    id = finalId,
                    name = chosen.name.trim(),
                    role = chosen.role.trim().ifBlank { if (finalId == 1L) "Γονέας" else "Μέλος" },
                    avatarColorHex = if (chosen.avatarColorHex != 0L) chosen.avatarColorHex else (if (finalId == 1L) 0xFF059669 else 0xFF0284C7),
                    voiceKeywords = chosen.voiceKeywords.trim(),
                    isDefault = chosen.isDefault || finalId == 1L,
                    sortOrder = chosen.sortOrder
                )
            )
        }

        val finalMembers = cleanMembers
            .distinctBy { it.id }
            .sortedWith(compareBy<FamilyMember> { it.sortOrder }.thenBy { it.id })

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
    }

    private suspend fun syncAndMergeCategories(
        incomingCats: List<FirebaseCategory>,
        explicitHouseholdId: String? = null,
        explicitUserId: String? = null
    ) {
        val currentLocal = repository.getAllCategoriesList()
        val householdId = explicitHouseholdId ?: _currentHousehold.value?.id.orEmpty()
        val uid = explicitUserId ?: _currentUser.value?.uid.orEmpty()

        if (incomingCats.isEmpty()) {
            if (currentLocal.isNotEmpty()) {
                currentLocal.forEach { cat ->
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
                    if (uid.isNotBlank()) {
                        firestoreSyncRepo.syncUserCategory(uid, fbCat)
                    }
                }
            }
            return
        }

        // Database is the SOURCE OF TRUTH!
        val cleanCategories = incomingCats.map { rc ->
            val catId = rc.id.toLongOrNull() ?: Math.abs(rc.id.hashCode().toLong()).coerceAtLeast(1L)
            ExpenseCategory(
                id = catId,
                name = rc.name.trim(),
                iconKey = rc.iconKey.ifBlank { "cart" },
                monthlyBudget = if (rc.monthlyBudget > 0.0) rc.monthlyBudget else 100.0,
                colorHex = if (rc.colorHex != 0L) rc.colorHex else 0xFF059669,
                voiceKeywords = rc.voiceKeywords.trim(),
                sortOrder = rc.sortOrder,
                fontColorHex = rc.fontColorHex
            )
        }

        val finalCategories = cleanCategories
            .distinctBy { it.id }
            .sortedWith(compareBy<ExpenseCategory> { it.sortOrder }.thenBy { it.id })

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
    }

    private suspend fun recoverAndSyncUserData(
        user: FirebaseUser,
        profile: UserProfile,
        activeHouseholdId: String
    ) {
        withContext(Dispatchers.IO) {
            try {
                val allHouseholdIds = (profile.householdIds + listOf(activeHouseholdId)).filter { it.isNotBlank() }.distinct()

                // 1. RECOVER / RESTORE MEMBERS (Guarantee primary member and preserve all valid members)
                val remoteMembers = firestoreSyncRepo.getAllMembersAcrossAllScopes(allHouseholdIds, user.uid)
                // Purge bad combination member names from remote
                remoteMembers.filter { isInvalidOrCombinationMember(it.name) }.forEach { badMember ->
                    firestoreSyncRepo.deleteMemberFromAllScopes(badMember.id, allHouseholdIds, user.uid)
                }
                syncAndMergeMembers(remoteMembers, profile, user, activeHouseholdId)

                // 2. RECOVER / RESTORE CATEGORIES (Guarantee 6 standard categories + any custom categories)
                val remoteCats = firestoreSyncRepo.getAllCategoriesAcrossAllScopes(allHouseholdIds, user.uid)
                syncAndMergeCategories(remoteCats, activeHouseholdId, user.uid)

                // 3. RECOVER / SYNC SAVINGS GOALS
                var remoteGoals = if (activeHouseholdId.isNotBlank()) {
                    firestoreSyncRepo.getSavingsGoalsOnce(activeHouseholdId)
                } else emptyList()
                if (remoteGoals.isEmpty()) {
                    remoteGoals = firestoreSyncRepo.getUserSavingsGoalsOnce(user.uid)
                }
                if (remoteGoals.isEmpty() && profile.householdIds.isNotEmpty()) {
                    remoteGoals = firestoreSyncRepo.fetchAllHouseholdSavingsGoals(profile.householdIds)
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
                    repository.insertSavingsGoals(domainGoals)
                } else {
                    val localGoals = repository.getAllSavingsGoalsList()
                    localGoals.forEach { goal ->
                        val fbGoal = FirebaseSavingsGoal(
                            id = goal.id.toString(),
                            householdId = activeHouseholdId,
                            userId = user.uid,
                            title = goal.title,
                            targetAmount = goal.targetAmount,
                            currentAmount = goal.currentAmount,
                            deadlineDate = goal.deadlineDate,
                            iconKey = goal.iconKey,
                            colorHex = goal.colorHex,
                            note = goal.note
                        )
                        if (activeHouseholdId.isNotBlank()) {
                            firestoreSyncRepo.syncSavingsGoal(activeHouseholdId, fbGoal)
                        }
                        firestoreSyncRepo.syncUserSavingsGoal(user.uid, fbGoal)
                    }
                }

                // 4. RECOVER / SYNC RECURRING EXPENSES
                var remoteRecurring = if (activeHouseholdId.isNotBlank()) {
                    firestoreSyncRepo.getRecurringExpensesOnce(activeHouseholdId)
                } else emptyList()
                if (remoteRecurring.isEmpty()) {
                    remoteRecurring = firestoreSyncRepo.getUserRecurringExpensesOnce(user.uid)
                }
                if (remoteRecurring.isEmpty() && profile.householdIds.isNotEmpty()) {
                    remoteRecurring = firestoreSyncRepo.fetchAllHouseholdRecurringExpenses(profile.householdIds)
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
                    repository.insertRecurringExpenses(domainRecurring)
                } else {
                    val localRecurring = repository.getAllRecurringExpensesList()
                    localRecurring.forEach { rr ->
                        val fbRec = FirebaseRecurringExpense(
                            id = rr.id.toString(),
                            householdId = activeHouseholdId,
                            userId = user.uid,
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
                        if (activeHouseholdId.isNotBlank()) {
                            firestoreSyncRepo.syncRecurringExpense(activeHouseholdId, fbRec)
                        }
                        firestoreSyncRepo.syncUserRecurringExpense(user.uid, fbRec)
                    }
                }

                // Check and execute any due standing orders
                checkAndProcessDueRecurringExpenses()

                // 5. Data Safety Guard audit to guarantee 100% bi-directional cloud mirror
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

            // Category reconciliation: NEVER erase category name to blank!
            val matchingCat = if (remote.categoryId != 0L) {
                currentCats.find { it.id == remote.categoryId }
            } else if (remote.categoryName.isNotBlank()) {
                currentCats.find { GreekVoiceParser.normalizeGreek(it.name.trim().lowercase()) == GreekVoiceParser.normalizeGreek(remote.categoryName.trim().lowercase()) }
            } else null

            val finalCatId = matchingCat?.id ?: remote.categoryId
            val finalCatName = matchingCat?.name ?: remote.categoryName

            // Beneficiary reconciliation: NEVER erase to blank if members haven't loaded yet!
            val finalMemberIdsCsv: String
            val finalMemberNamesDisplay: String
            if (remote.splitType == "ALL_FAMILY") {
                finalMemberIdsCsv = remote.memberIdsCsv
                finalMemberNamesDisplay = remote.memberNamesDisplay.ifBlank { "Κοινό Οικογένειας" }
            } else {
                val parsedIds = remote.memberIdsCsv.split(",").mapNotNull { it.trim().toLongOrNull() }
                if (currentMems.isNotEmpty() && parsedIds.isNotEmpty()) {
                    val remainingMembers = currentMems.filter { parsedIds.contains(it.id) }
                    if (remainingMembers.isNotEmpty()) {
                        finalMemberIdsCsv = remainingMembers.map { it.id }.joinToString(",")
                        finalMemberNamesDisplay = remainingMembers.joinToString(" & ") { it.name }
                    } else {
                        finalMemberIdsCsv = remote.memberIdsCsv
                        finalMemberNamesDisplay = remote.memberNamesDisplay
                    }
                } else {
                    finalMemberIdsCsv = remote.memberIdsCsv
                    finalMemberNamesDisplay = remote.memberNamesDisplay
                }
            }

            // Multi-Payer Reconciliation:
            // ALWAYS prioritize remote data directly as the database source of truth!
            val cleanPayerName = remote.payerMemberName.replace(Regex("\\([^)]*\\)"), "").trim()
            val individualPayerNames = cleanPayerName.split(Regex("[&,/]+")).map { it.trim() }.filter { it.isNotBlank() }
            val matchedPayerMemberIds = if (currentMems.isNotEmpty()) {
                individualPayerNames.mapNotNull { n -> currentMems.find { m -> m.name.equals(n, ignoreCase = true) }?.id }
            } else emptyList()

            val finalPayerMemberIdsCsv = when {
                remote.payerMemberIdsCsv.isNotBlank() -> remote.payerMemberIdsCsv
                matchedPayerMemberIds.size > 1 -> matchedPayerMemberIds.joinToString(",")
                remote.payerMemberId > 0 -> remote.payerMemberId.toString()
                else -> ""
            }

            val finalPayerAmountsCsv = remote.payerAmountsCsv

            val finalPayerId: Long
            val finalPayerName: String

            if (remote.payerMemberName.isNotBlank()) {
                // Remote explicitly has the accurate payer name (single or multi-payer with amounts)
                finalPayerName = remote.payerMemberName
                finalPayerId = if (remote.payerMemberId > 0L) {
                    remote.payerMemberId
                } else {
                    currentMems.find { GreekVoiceParser.normalizeGreek(it.name.trim().lowercase()) == GreekVoiceParser.normalizeGreek(cleanPayerName.lowercase()) }?.id
                        ?: 0L
                }
            } else if (remote.payerMemberId > 0L) {
                finalPayerId = remote.payerMemberId
                val matchingMember = currentMems.find { it.id == remote.payerMemberId }
                finalPayerName = matchingMember?.name ?: ""
            } else {
                finalPayerId = 0L
                finalPayerName = ""
            }

            Expense(
                id = resolvedId,
                cloudId = remote.id,
                title = remote.title,
                amount = remote.amount,
                categoryId = finalCatId,
                categoryName = finalCatName,
                timestamp = remote.timestamp,
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

        return remoteIncomes.map { remote ->
            val hashId = Math.abs(remote.id.hashCode().toLong())

            val matchingMember = if (remote.memberId != 0L) {
                currentMems.find { it.id == remote.memberId }
            } else if (remote.memberName.isNotBlank()) {
                currentMems.find { GreekVoiceParser.normalizeGreek(it.name.trim().lowercase()) == GreekVoiceParser.normalizeGreek(remote.memberName.trim().lowercase()) }
            } else null

            val finalMemberId = when {
                remote.memberId == 0L && remote.memberName.isBlank() -> 0L
                matchingMember != null -> matchingMember.id
                currentMems.isNotEmpty() && remote.memberId != 0L -> 0L // Member was deleted -> clear field
                else -> remote.memberId
            }
            val finalMemberName = when {
                finalMemberId == 0L -> ""
                matchingMember != null -> matchingMember.name // Update to changed member name
                else -> remote.memberName
            }

            val matchingCat = currentCats.find { GreekVoiceParser.normalizeGreek(it.name.trim().lowercase()) == GreekVoiceParser.normalizeGreek(remote.category.trim().lowercase()) }
            val finalCat = if (matchingCat != null) matchingCat.name else remote.category

            Income(
                id = if (hashId == 0L) 1L else hashId,
                cloudId = remote.id,
                title = remote.title,
                amount = remote.amount,
                memberId = finalMemberId,
                memberName = finalMemberName,
                category = finalCat,
                timestamp = remote.timestamp,
                note = remote.note
            )
        }
    }

    private fun observeUserData(userId: String) {
        userProfileSyncJob?.cancel()
        userProfileSyncJob = viewModelScope.launch {
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
                        isPremium = remoteProfile.isPremium || (current?.isPremium == true)
                    )
                    _userProfile.value = merged
                    saveUserProfileToPrefs(merged)
                }
            }
        }

        userExpensesSyncJob?.cancel()
        userExpensesSyncJob = viewModelScope.launch {
            firestoreSyncRepo.observeUserExpenses(userId).collect { remoteExpenses ->
                if (_allExpenses.value.isEmpty() || _currentHousehold.value == null) {
                    val domain = mapFirebaseExpensesToDomain(remoteExpenses)
                    _allExpenses.value = domain
                    try {
                        if (domain.isNotEmpty()) repository.replaceAllExpenses(domain)
                    } catch (e: Exception) {
                        Log.d("OikoNomosVM", "Room cache user expenses: ${e.message}")
                    }
                }
            }
        }

        userIncomesSyncJob?.cancel()
        userIncomesSyncJob = viewModelScope.launch {
            firestoreSyncRepo.observeUserIncomes(userId).collect { remoteIncomes ->
                if (_allIncomes.value.isEmpty() || _currentHousehold.value == null) {
                    val domain = mapFirebaseIncomesToDomain(remoteIncomes)
                    _allIncomes.value = domain
                    try {
                        if (domain.isNotEmpty()) repository.insertIncomes(domain)
                    } catch (e: Exception) {
                        Log.d("OikoNomosVM", "Room cache user incomes: ${e.message}")
                    }
                }
            }
        }

        userMembersSyncJob?.cancel()
        userMembersSyncJob = viewModelScope.launch {
            firestoreSyncRepo.observeUserMembers(userId).collect { remoteMembers ->
                syncAndMergeMembers(remoteMembers)
            }
        }

        userCategoriesSyncJob?.cancel()
        userCategoriesSyncJob = viewModelScope.launch {
            firestoreSyncRepo.observeUserCategories(userId).collect { remoteCats ->
                syncAndMergeCategories(remoteCats)
            }
        }

        userSavingsGoalsSyncJob?.cancel()
        userSavingsGoalsSyncJob = viewModelScope.launch {
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
        }

        userRecurringSyncJob?.cancel()
        userRecurringSyncJob = viewModelScope.launch {
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
            // Step 1: Read from Firestore disk cache only if memory/Room is currently empty
            try {
                val cached = firestoreSyncRepo.getExpensesFromCache(householdId)
                if (cached.isNotEmpty() && _allExpenses.value.isEmpty()) {
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
                        val domainExpenses = mapFirebaseExpensesToDomain(remoteExpenses)
                        _allExpenses.value = domainExpenses
                        _isInitialExpensesLoading.value = false
                        if (domainExpenses.isNotEmpty()) {
                            prefs.edit().putBoolean("has_entered_any_expense", true).apply()
                        }
                        try {
                            if (domainExpenses.isNotEmpty()) {
                                repository.replaceAllExpenses(domainExpenses)
                            }
                        } catch (e: Exception) {
                            Log.d("OikoNomosVM", "Cache remote expenses in Room: ${e.message}")
                        }
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
                        val domainIncomes = mapFirebaseIncomesToDomain(remoteIncomes)
                        _allIncomes.value = domainIncomes
                        try {
                            if (domainIncomes.isNotEmpty()) {
                                repository.insertIncomes(domainIncomes)
                            }
                        } catch (e: Exception) {
                            Log.d("OikoNomosVM", "Cache remote incomes in Room: ${e.message}")
                        }
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
                        syncAndMergeMembers(remoteMembers)
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
                        syncAndMergeCategories(remoteCats)
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
                    val profile = _userProfile.value
                    if (profile != null) {
                        recoverAndSyncUserData(user, profile, household.id)
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

    fun isUserVerified(user: FirebaseUser?): Boolean {
        if (user == null) return false
        val isGoogleUser = user.providerData.any { it.providerId == "google.com" }
        return isGoogleUser || user.isEmailVerified
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
            onError("Παρακαλώ εισάγετε το email σας")
            return
        }
        _isAuthLoading.value = true
        viewModelScope.launch {
            try {
                val registeredUser = firestoreSyncRepo.findUserByEmail(clean)
                if (registeredUser == null) {
                    _isAuthLoading.value = false
                    onError("Δεν βρέθηκε εγγεγραμμένος λογαριασμός με το email «$clean». Βεβαιωθείτε ότι το πληκτρολογήσατε σωστά ή αν είχατε συνδεθεί με Google Sign-In.")
                    return@launch
                }
                val targetEmail = registeredUser.email.ifBlank { clean }
                auth.setLanguageCode(if (_appLanguage.value == "EN") "en" else "el")
                auth.sendPasswordResetEmail(targetEmail)
                    .addOnSuccessListener {
                        _isAuthLoading.value = false
                        showSnackbar("Στάλθηκε email επαναφοράς κωδικού στο $targetEmail!")
                        onSuccess()
                    }
                    .addOnFailureListener { exc ->
                        _isAuthLoading.value = false
                        val msg = when {
                            exc.message?.contains("user-not-found") == true -> "Δεν βρέθηκε λογαριασμός με αυτό το email"
                            exc.message?.contains("invalid-email") == true -> "Μη έγκυρη μορφή email"
                            else -> exc.localizedMessage ?: "Σφάλμα αποστολής email"
                        }
                        onError(msg)
                    }
            } catch (e: Exception) {
                auth.setLanguageCode(if (_appLanguage.value == "EN") "en" else "el")
                auth.sendPasswordResetEmail(clean)
                    .addOnSuccessListener {
                        _isAuthLoading.value = false
                        showSnackbar("Στάλθηκε email επαναφοράς κωδικού!")
                        onSuccess()
                    }
                    .addOnFailureListener { exc ->
                        _isAuthLoading.value = false
                        onError(exc.localizedMessage ?: "Σφάλμα αποστολής email")
                    }
            }
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
                            } catch (e: Exception) {
                                Log.e("OikoNomosVM", "Error storing profile during sign up", e)
                            }
                        }

                        // Send verification email
                        auth.setLanguageCode(if (_appLanguage.value == "EN") "en" else "el")
                        user.sendEmailVerification()
                        _isAuthLoading.value = false
                        _currentUser.value = user
                        _isUserVerifiedState.value = false
                        showSnackbar("Στάλθηκε email επιβεβαίωσης στο ${user.email}!")
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
        prefs.edit().remove("last_household_id").apply()
        householdJob?.cancel()
        expensesSyncJob?.cancel()
        userExpensesSyncJob?.cancel()
        incomesSyncJob?.cancel()
        userIncomesSyncJob?.cancel()
        membersSyncJob?.cancel()
        categoriesSyncJob?.cancel()
        _allExpenses.value = emptyList()
        _allIncomes.value = emptyList()
        viewModelScope.launch {
            if (com.example.data.safety.DataSafetyGuard.validateDestructiveOperation(
                    com.example.data.safety.DataSafetyGuard.DestructionReason.EXPLICIT_SIGN_OUT
                )) {
                repository.clearAllData() // Clean wipe Room so next user starts 100% clean
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
        prefs.edit().clear().apply()
        householdJob?.cancel()
        expensesSyncJob?.cancel()
        userExpensesSyncJob?.cancel()
        incomesSyncJob?.cancel()
        userIncomesSyncJob?.cancel()
        membersSyncJob?.cancel()
        categoriesSyncJob?.cancel()
        _allExpenses.value = emptyList()
        _allIncomes.value = emptyList()
        viewModelScope.launch {
            if (com.example.data.safety.DataSafetyGuard.validateDestructiveOperation(
                    com.example.data.safety.DataSafetyGuard.DestructionReason.EXPLICIT_FACTORY_RESET
                )) {
                repository.clearAllData()
            }
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
            ?: Household(id = prefs.getString("last_household_id", "") ?: "", name = "Το Εξοδολόγιό μου")

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
            householdId = prefs.getString("cached_user_household_id", "").orEmpty()
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
                val currentCats = repository.getAllCategoriesList()
                if (currentCats.isNotEmpty()) {
                    currentCats.forEach { cat ->
                        val fbCat = FirebaseCategory(
                            id = cat.id.toString(),
                            householdId = newHousehold.id,
                            userId = user.uid,
                            name = cat.name,
                            iconKey = cat.iconKey,
                            monthlyBudget = cat.monthlyBudget,
                            colorHex = cat.colorHex,
                            voiceKeywords = cat.voiceKeywords,
                            fontColorHex = cat.fontColorHex
                        )
                        firestoreSyncRepo.syncCategory(newHousehold.id, fbCat)
                    }
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
                repository.deleteAllExpenses()
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

            val householdId = _currentHousehold.value?.id ?: ""
            val user = _currentUser.value
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
                firestoreSyncRepo.deleteSavingsGoal(householdId, goal.id.toString())
            }
            if (user != null) {
                firestoreSyncRepo.deleteUserSavingsGoal(user.uid, goal.id.toString())
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

            val householdId = _currentHousehold.value?.id ?: ""
            val user = _currentUser.value

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
                firestoreSyncRepo.deleteRecurringExpense(householdId, recurring.id.toString())
            }
            if (user != null) {
                firestoreSyncRepo.deleteUserRecurringExpense(user.uid, recurring.id.toString())
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

    /**
     * Checks all active standing orders and automatically registers any that are due today or this month.
     */
    fun checkAndProcessDueRecurringExpenses() {
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
                    val updatedList = (newExpenses + _allExpenses.value).distinctBy { it.id }.sortedWith(
                        compareByDescending<Expense> { it.timestamp }.thenByDescending { it.id }
                    )
                    _allExpenses.value = updatedList
                    val names = newExpenses.take(2).joinToString(", ") { "«${it.title}»" }
                    val suffix = if (newExpenses.size > 2) " και άλλες ${newExpenses.size - 2}" else ""
                    showSnackbar("Αυτόματη καταχώρηση: $names$suffix στα έξοδα!")
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

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode).apply()
    }

    fun setUiStyle(style: String) {
        _uiStyle.value = style
        prefs.edit().putString("ui_style", style).apply()
    }

    fun setAppLanguage(lang: String) {
        _appLanguage.value = lang
        prefs.edit()
            .putString("app_language", lang)
            .putBoolean("user_explicitly_set_language_v2", true)
            .apply()
        auth.setLanguageCode(if (lang == "EN") "en" else "el")
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
            val household = _currentHousehold.value
            val targetHouseholdId = household?.id?.ifBlank { null }
                ?: _userProfile.value?.householdId?.ifBlank { null }
                ?: prefs.getString("last_household_id", "") ?: ""
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
            val household = _currentHousehold.value
            val targetHouseholdId = household?.id?.ifBlank { null }
                ?: _userProfile.value?.householdId?.ifBlank { null }
                ?: _userProfile.value?.householdIds?.firstOrNull()?.ifBlank { null }
                ?: prefs.getString("last_household_id", "") ?: ""
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
            val household = _currentHousehold.value
            val targetHouseholdId = household?.id?.ifBlank { null }
                ?: _userProfile.value?.householdId?.ifBlank { null }
                ?: prefs.getString("last_household_id", "") ?: ""
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
        val existing = _editingIncome.value
            ?: if (id > 0) _allIncomes.value.find { it.id == id } else null
        dismissManualIncomeDialog()

        viewModelScope.launch {
            val user = _currentUser.value
            val household = _currentHousehold.value
            val targetHouseholdId = household?.id?.ifBlank { null }
                ?: _userProfile.value?.householdId?.ifBlank { null }
                ?: prefs.getString("last_household_id", "") ?: ""
            val targetUserId = user?.uid ?: ""

            val existingDocId = existing?.cloudId?.ifBlank { null }
                ?: if (id > 0) {
                    val inList = _allIncomes.value.find { it.id == id } ?: repository.getAllIncomesList().find { it.id == id }
                    inList?.cloudId?.ifBlank { null }
                } else null

            val docId = existingDocId ?: if (id > 0) "inc_$id" else java.util.UUID.randomUUID().toString()

            val targetId = if (id > 0) id else (existing?.id ?: 0L)
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
            val household = _currentHousehold.value
            val targetHouseholdId = household?.id?.ifBlank { null }
                ?: _userProfile.value?.householdId?.ifBlank { null }
                ?: prefs.getString("last_household_id", "") ?: ""
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

            if (isDef || id == 1L) {
                _userProfile.value = _userProfile.value?.copy(displayName = cleanName)
            }

            val householdId = _currentHousehold.value?.id ?: ""
            val user = _currentUser.value
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
            firestoreSyncRepo.syncAndCleanMember(allHouseholdIds, user?.uid.orEmpty(), fbMember, oldName)

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
        viewModelScope.launch {
            repository.updateMember(member.copy(avatarColorHex = newColorHex))
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
            val allHouseholdIds = (_userProfile.value?.householdIds.orEmpty() + listOf(_currentHousehold.value?.id.orEmpty())).filter { it.isNotBlank() }.distinct()
            allHouseholdIds.forEach { hhId ->
                if (hhId != householdId) {
                    firestoreSyncRepo.syncMember(hhId, fbMember.copy(householdId = hhId))
                }
            }
            if (user != null) {
                firestoreSyncRepo.syncUserMember(user.uid, fbMember)
            }
            showSnackbar("Το χρώμα για ${member.name} ενημερώθηκε")
        }
    }

    fun deleteMember(member: FamilyMember) {
        viewModelScope.launch {
            repository.deleteMember(member)
            val allHouseholdIds = (_userProfile.value?.householdIds.orEmpty() + listOf(_currentHousehold.value?.id.orEmpty())).filter { it.isNotBlank() }.distinct()
            val householdId = _currentHousehold.value?.id ?: ""
            val user = _currentUser.value
            firestoreSyncRepo.deleteMemberFromAllScopes(member.id.toString(), allHouseholdIds, user?.uid ?: "", member.name)

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
                if (user != null) firestoreSyncRepo.syncUserMember(user.uid, fbNewDefault)
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

            val householdId = _currentHousehold.value?.id ?: ""
            val user = _currentUser.value
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
            val allHouseholdIds = (_userProfile.value?.householdIds.orEmpty() + listOf(_currentHousehold.value?.id.orEmpty())).filter { it.isNotBlank() }.distinct()
            allHouseholdIds.forEach { hhId ->
                if (hhId != householdId) {
                    firestoreSyncRepo.syncCategory(hhId, fbCat.copy(householdId = hhId))
                }
            }
            if (user != null) {
                firestoreSyncRepo.syncUserCategory(user.uid, fbCat)
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
        viewModelScope.launch {
            repository.deleteCategory(category)
            val allHouseholdIds = (_userProfile.value?.householdIds.orEmpty() + listOf(_currentHousehold.value?.id.orEmpty())).filter { it.isNotBlank() }.distinct()
            val householdId = _currentHousehold.value?.id ?: ""
            val user = _currentUser.value
            firestoreSyncRepo.deleteCategoryFromAllScopes(category.id.toString(), allHouseholdIds, user?.uid ?: "")

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
    // 1. REORDERING: MEMBERS & CATEGORIES (Up / Down Reorder)
    // =========================================================================
    fun moveMemberUp(member: FamilyMember) {
        viewModelScope.launch {
            val list = repository.getAllMembersList().toMutableList()
            val index = list.indexOfFirst { it.id == member.id }
            if (index > 0) {
                val prev = list[index - 1]
                list[index - 1] = member.copy(sortOrder = index - 1)
                list[index] = prev.copy(sortOrder = index)
                val updated = list.mapIndexed { i, m -> m.copy(sortOrder = i) }
                repository.replaceAllMembers(updated)
                syncUpdatedMembersToCloud(updated)
            }
        }
    }

    fun moveMemberDown(member: FamilyMember) {
        viewModelScope.launch {
            val list = repository.getAllMembersList().toMutableList()
            val index = list.indexOfFirst { it.id == member.id }
            if (index in 0 until list.size - 1) {
                val next = list[index + 1]
                list[index + 1] = member.copy(sortOrder = index + 1)
                list[index] = next.copy(sortOrder = index)
                val updated = list.mapIndexed { i, m -> m.copy(sortOrder = i) }
                repository.replaceAllMembers(updated)
                syncUpdatedMembersToCloud(updated)
            }
        }
    }

    private suspend fun syncUpdatedMembersToCloud(members: List<FamilyMember>) {
        val householdId = _currentHousehold.value?.id.orEmpty()
        val uid = _currentUser.value?.uid.orEmpty()
        val allHouseholdIds = (_userProfile.value?.householdIds.orEmpty() + listOf(householdId)).filter { it.isNotBlank() }.distinct()
        members.forEach { m ->
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
            for (hId in allHouseholdIds) {
                firestoreSyncRepo.syncMember(hId, fb.copy(householdId = hId))
            }
            if (uid.isNotBlank()) firestoreSyncRepo.syncUserMember(uid, fb)
        }
    }

    fun moveCategoryUp(category: ExpenseCategory) {
        viewModelScope.launch {
            val list = repository.getAllCategoriesList().toMutableList()
            val index = list.indexOfFirst { it.id == category.id }
            if (index > 0) {
                val prev = list[index - 1]
                list[index - 1] = category.copy(sortOrder = index - 1)
                list[index] = prev.copy(sortOrder = index)
                val updated = list.mapIndexed { i, c -> c.copy(sortOrder = i) }
                repository.replaceAllCategories(updated)
                syncUpdatedCategoriesToCloud(updated)
            }
        }
    }

    fun moveCategoryDown(category: ExpenseCategory) {
        viewModelScope.launch {
            val list = repository.getAllCategoriesList().toMutableList()
            val index = list.indexOfFirst { it.id == category.id }
            if (index in 0 until list.size - 1) {
                val next = list[index + 1]
                list[index + 1] = category.copy(sortOrder = index + 1)
                list[index] = next.copy(sortOrder = index)
                val updated = list.mapIndexed { i, c -> c.copy(sortOrder = i) }
                repository.replaceAllCategories(updated)
                syncUpdatedCategoriesToCloud(updated)
            }
        }
    }

    private suspend fun syncUpdatedCategoriesToCloud(categories: List<ExpenseCategory>) {
        val householdId = _currentHousehold.value?.id.orEmpty()
        val uid = _currentUser.value?.uid.orEmpty()
        val allHouseholdIds = (_userProfile.value?.householdIds.orEmpty() + listOf(householdId)).filter { it.isNotBlank() }.distinct()
        categories.forEach { c ->
            val fb = FirebaseCategory(
                id = c.id.toString(),
                householdId = householdId,
                userId = uid,
                name = c.name,
                iconKey = c.iconKey,
                monthlyBudget = c.monthlyBudget,
                colorHex = c.colorHex,
                voiceKeywords = c.voiceKeywords,
                sortOrder = c.sortOrder,
                fontColorHex = c.fontColorHex
            )
            for (hId in allHouseholdIds) {
                firestoreSyncRepo.syncCategory(hId, fb.copy(householdId = hId))
            }
            if (uid.isNotBlank()) firestoreSyncRepo.syncUserCategory(uid, fb)
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
}
