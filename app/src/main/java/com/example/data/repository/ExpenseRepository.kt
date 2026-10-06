package com.example.data.repository

import android.content.Context
import com.example.data.dao.ExpenseDao
import com.example.data.entity.Expense
import com.example.data.entity.ExpenseCategory
import com.example.data.entity.FamilyMember
import com.example.data.entity.Income
import com.example.data.entity.RecurringExpense
import com.example.data.entity.SavingsGoal
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(
    private val expenseDao: ExpenseDao,
    private val context: Context? = null
) {

    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses()
    val allIncomes: Flow<List<Income>> = expenseDao.getAllIncomes()
    val allMembers: Flow<List<FamilyMember>> = expenseDao.getAllMembers()
    val allCategories: Flow<List<ExpenseCategory>> = expenseDao.getAllCategories()
    val allRecurringExpenses: Flow<List<RecurringExpense>> = expenseDao.getAllRecurringExpenses()
    val allSavingsGoals: Flow<List<SavingsGoal>> = expenseDao.getAllSavingsGoals()

    suspend fun insertExpense(expense: Expense): Long = expenseDao.insertExpense(expense)
    suspend fun updateExpense(expense: Expense) = expenseDao.updateExpense(expense)
    suspend fun deleteExpense(expense: Expense) = expenseDao.deleteExpense(expense)
    suspend fun deleteExpenseById(id: Long) = expenseDao.deleteExpenseById(id)
    suspend fun deleteAllExpenses() = expenseDao.deleteAllExpenses()

    suspend fun insertIncome(income: Income): Long = expenseDao.insertIncome(income)
    suspend fun updateIncome(income: Income) = expenseDao.updateIncome(income)
    suspend fun deleteIncome(income: Income) = expenseDao.deleteIncome(income)
    suspend fun deleteIncomeById(id: Long) = expenseDao.deleteIncomeById(id)
    suspend fun deleteAllIncomes() = expenseDao.deleteAllIncomes()

    suspend fun insertMember(member: FamilyMember): Long = expenseDao.insertMember(member)
    suspend fun updateMember(member: FamilyMember) = expenseDao.updateMember(member)
    suspend fun deleteMember(member: FamilyMember) = expenseDao.deleteMember(member)

    suspend fun insertCategory(category: ExpenseCategory): Long = expenseDao.insertCategory(category)
    suspend fun updateCategory(category: ExpenseCategory) = expenseDao.updateCategory(category)
    suspend fun deleteCategory(category: ExpenseCategory) = expenseDao.deleteCategory(category)

    suspend fun insertRecurringExpense(recurring: RecurringExpense): Long = expenseDao.insertRecurringExpense(recurring)
    suspend fun updateRecurringExpense(recurring: RecurringExpense) = expenseDao.updateRecurringExpense(recurring)
    suspend fun deleteRecurringExpense(recurring: RecurringExpense) = expenseDao.deleteRecurringExpense(recurring)

    suspend fun insertSavingsGoal(goal: SavingsGoal): Long = expenseDao.insertSavingsGoal(goal)
    suspend fun updateSavingsGoal(goal: SavingsGoal) = expenseDao.updateSavingsGoal(goal)
    suspend fun deleteSavingsGoal(goal: SavingsGoal) = expenseDao.deleteSavingsGoal(goal)

    suspend fun clearAllData() {
        expenseDao.deleteAllExpenses()
        expenseDao.deleteAllIncomes()
        expenseDao.deleteAllMembers()
        expenseDao.deleteAllCategories()
    }

    suspend fun seedInitialDataIfEmpty() {
        val categoryCount = expenseDao.getCategoryCount()
        if (categoryCount == 0) {
            val defaultCategories = listOf(
                ExpenseCategory(
                    name = "Σούπερ μάρκετ",
                    iconKey = "cart",
                    monthlyBudget = 450.0,
                    colorHex = 0xFF059669,
                    voiceKeywords = "σούπερ μάρκετ, σουπερμάρκετ, σουπερ μαρκετ, σκλαβενίτης, σκλαβενιτης, βασιλόπουλος, βασιλοπουλος, lidl, λιντλ, μάρκετ, τροφιμα, τρόφιμα, ψώνια, μανάβικο, κρεοπωλείο, ιχθυοπωλείο, μαναβης"
                ),
                ExpenseCategory(
                    name = "Ψιλικά",
                    iconKey = "kiosk",
                    monthlyBudget = 40.0,
                    colorHex = 0xFF0284C7,
                    voiceKeywords = "ψιλικά, ψιλικα, περίπτερο, περιπτερο, μίνι μάρκετ, μινι μαρκετ, τσιγάρα, τσιγαρα, καπνός, καπνος, εφημερίδα, ψιλικατζίδικο, ψιλικατζιδικο, τσίχλες"
                ),
                ExpenseCategory(
                    name = "Φαγητό έξω",
                    iconKey = "food",
                    monthlyBudget = 150.0,
                    colorHex = 0xFFEF4444,
                    voiceKeywords = "φαγητό έξω, φαγητο εξω, σουβλάκια, σουβλακια, σουβλάκι, σουβλακι, πίτσα, πιτσα, πίτσες, πιτσες, delivery, ντελίβερι, efood, wolt, ταβέρνα, ταβερνα, εστιατόριο, burger, μπέργκερ, κρέπα, κρεπα, φαγητό"
                ),
                ExpenseCategory(
                    name = "Καφέδες",
                    iconKey = "coffee",
                    monthlyBudget = 60.0,
                    colorHex = 0xFFD97706,
                    voiceKeywords = "καφέδες, καφεδες, καφές, καφες, καφε, καφέ, freddo, cappuccino, espresso, κουλούρι, κουλουρι, πρωινό, πρωινο, bakery, φούρνος, φουρνος, croissant, κρουασάν"
                ),
                ExpenseCategory(
                    name = "Δραστηριότητες",
                    iconKey = "sports",
                    monthlyBudget = 180.0,
                    colorHex = 0xFF8B5CF6,
                    voiceKeywords = "δραστηριότητες, δραστηριοτητες, κολυμβητήριο, κολυμβητηριο, αγγλικά, αγγλικα, φροντιστήριο, φροντιστηριο, μπαλέτο, μπαλετο, χορός, χορος, ωδείο, ωδειο, τένις, τενις, μπάσκετ, μπασκετ, ποδόσφαιρο, σχολικά, ιδιαίτερα, πιάνο, κιθάρα"
                ),
                ExpenseCategory(
                    name = "Βενζίνη",
                    iconKey = "gas_station",
                    monthlyBudget = 120.0,
                    colorHex = 0xFFEA580C,
                    voiceKeywords = "βενζίνη, βενζινη, καύσιμα, καυσιμα, βενζινάδικο, βενζιναδικο, πετρέλαιο, πετρελαιο, shell, bp, eko, avin, revoil, διόδια, διοδια, πάρκινγκ, παρκινγκ"
                ),
                ExpenseCategory(
                    name = "Γιατρός - φαρμακείο",
                    iconKey = "medical",
                    monthlyBudget = 70.0,
                    colorHex = 0xFFEC4899,
                    voiceKeywords = "γιατρός, γιατρος, φαρμακείο, φαρμακειο, γιατροί, γιατροι, φάρμακα, φαρμακα, παιδίατρος, παιδιατρος, οδοντίατρος, οδοντιατρος, εξετάσεις, εξετασεις, μικροβιολόγος, φυσικοθεραπεία, υγεία, υγεια"
                ),
                ExpenseCategory(
                    name = "Ενοίκιο",
                    iconKey = "house",
                    monthlyBudget = 500.0,
                    colorHex = 0xFF0D9488,
                    voiceKeywords = "ενοίκιο, ενοικιο, σπίτι ενοίκιο, νοίκι, νοικι, ιδιοκτήτης, μίσθωμα, μισθωμα"
                ),
                ExpenseCategory(
                    name = "Ρεύμα",
                    iconKey = "electric",
                    monthlyBudget = 90.0,
                    colorHex = 0xFFF59E0B,
                    voiceKeywords = "ρεύμα, ρευμα, δεη, dehi, ήρωνας, ηρωνας, protergia, προτέργια, elpedison, ελπεντισον, watt volt, λογαριασμός ρεύματος, ρεύματος"
                ),
                ExpenseCategory(
                    name = "Αέριο",
                    iconKey = "gas",
                    monthlyBudget = 60.0,
                    colorHex = 0xFFF97316,
                    voiceKeywords = "αέριο, αεριο, φυσικό αέριο, φυσικο αεριο, θέρμανση, θερμανση, λέβητας, λεβητας, efa"
                ),
                ExpenseCategory(
                    name = "Κοινόχρηστα",
                    iconKey = "apartment",
                    monthlyBudget = 40.0,
                    colorHex = 0xFF6366F1,
                    voiceKeywords = "κοινόχρηστα, κοινοχρηστα, κοινόχρηστα πολυκατοικίας, διαχειριστής, διαχειριση, ασανσέρ, καθαριότητα πολυκατοικίας"
                ),
                ExpenseCategory(
                    name = "Δάνεια",
                    iconKey = "loan",
                    monthlyBudget = 150.0,
                    colorHex = 0xFF475569,
                    voiceKeywords = "δάνεια, δανεια, δάνειο, δανειο, τράπεζα, τραπεζα, πιστωτική, πιστωτικη, eurobank, alpha bank, εθνική, πειραιώς, κάρτα δόση"
                ),
                ExpenseCategory(
                    name = "Δόση αυτοκινήτου",
                    iconKey = "car_payment",
                    monthlyBudget = 180.0,
                    colorHex = 0xFF334155,
                    voiceKeywords = "δόση αυτοκινήτου, δοση αυτοκινητου, αυτοκίνητο δόση, γραμμάτιο, γραμματιο, leasing, ασφάλεια αυτοκινήτου, τέλη κυκλοφορίας"
                ),
                ExpenseCategory(
                    name = "Έξοδα γατιών",
                    iconKey = "pets",
                    monthlyBudget = 45.0,
                    colorHex = 0xFF14B8A6,
                    voiceKeywords = "έξοδα γατιών, εξοδα γατιων, γάτες, γατες, γάτα, γατα, γατιά, γατια, γατοτροφή, γατοτροφη, άμμος γάτας, αμμος γατας, κτηνίατρος, κτηνιατρος, pet shop, πετ σοπ, λιχουδιές γάτας, τροφή"
                ),
                ExpenseCategory(
                    name = "Χρωστούμενα",
                    iconKey = "debt",
                    monthlyBudget = 50.0,
                    colorHex = 0xFF9333EA,
                    voiceKeywords = "χρωστούμενα, χρωστουμενα, χρωστάω, χρωσταω, δανεικά, δανεικα, επιστροφή χρημάτων, εξόφληση, οφειλή, οφειλη, χρέος, χρεος"
                ),
                ExpenseCategory(
                    name = "Διασκέδαση",
                    iconKey = "entertainment",
                    monthlyBudget = 80.0,
                    colorHex = 0xFFE11D48,
                    voiceKeywords = "διασκέδαση, διασκεδαση, σινεμά, σινεμα, σινεμάς, θέατρο, θεατρο, συναυλία, συναυλια, ποτό, ποτο, μπαρ, bar, έξοδος, εξοδος, netflix, cinema, παιδότοπος, παιδοτοπος, escape room, bowling"
                ),
                ExpenseCategory(
                    name = "Δώρα",
                    iconKey = "gift",
                    monthlyBudget = 50.0,
                    colorHex = 0xFFD946EF,
                    voiceKeywords = "δώρα, δωρα, δώρο, δωρο, γενέθλια, γενεθλια, γιορτή, γιορτη, παιχνίδια, παιχνιδια, βαφτίσια, γάμος, δωράκι, δωρακι"
                )
            )
            expenseDao.insertCategories(defaultCategories)
        }
    }
}
