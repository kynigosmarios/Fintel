package com.example.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Localization helper providing instant, dynamic translations in Greek (EL) and English (EN)
 * across the entire Fintel application.
 */
object Localization {

    enum class Language(val code: String, val displayName: String, val flag: String) {
        GREEK("EL", "Ελληνικά", "🇬🇷"),
        ENGLISH("EN", "English", "🇬🇧");

        companion object {
            fun fromCode(code: String): Language {
                return if (code.equals("EN", ignoreCase = true)) ENGLISH else GREEK
            }
        }
    }

    // App Branding & Navigation
    fun appName(lang: Language) = "Fintel"
    fun appTagline(lang: Language) = if (lang == Language.ENGLISH) "Smart Expense Tracker" else "Έξυπνο Εξοδολόγιο"

    // Bottom Nav Tabs
    fun tabExpenses(lang: Language) = if (lang == Language.ENGLISH) "Expenses" else "Έξοδα"
    fun tabAnalytics(lang: Language) = if (lang == Language.ENGLISH) "Analytics" else "Αναλυτικά"
    fun tabMembers(lang: Language) = if (lang == Language.ENGLISH) "Members" else "Μέλη"
    fun tabCategories(lang: Language) = if (lang == Language.ENGLISH) "Categories" else "Κατηγορίες"
    fun tabSavings(lang: Language) = if (lang == Language.ENGLISH) "Savings" else "Αποταμίευση"

    // Date / Time Format helpers
    fun getDateLocale(lang: Language): Locale = if (lang == Language.ENGLISH) Locale.US else Locale.forLanguageTag("el-GR")

    fun formatMonthYear(timestamp: Long, lang: Language): String {
        val sdf = SimpleDateFormat("LLLL yyyy", getDateLocale(lang))
        return sdf.format(Date(timestamp)).replaceFirstChar { it.uppercase() }
    }

    fun formatDayMonth(timestamp: Long, lang: Language): String {
        val sdf = SimpleDateFormat("EEEE, d MMMM", getDateLocale(lang))
        return sdf.format(Date(timestamp)).replaceFirstChar { it.uppercase() }
    }

    fun today(lang: Language) = if (lang == Language.ENGLISH) "Today" else "Σήμερα"
    fun yesterday(lang: Language) = if (lang == Language.ENGLISH) "Yesterday" else "Χθες"

    // Common Actions & Dialogs
    fun close(lang: Language) = if (lang == Language.ENGLISH) "Close" else "Κλείσιμο"
    fun cancel(lang: Language) = if (lang == Language.ENGLISH) "Cancel" else "Ακύρωση"
    fun save(lang: Language) = if (lang == Language.ENGLISH) "Save" else "Αποθήκευση"
    fun edit(lang: Language) = if (lang == Language.ENGLISH) "Edit" else "Επεξεργασία"
    fun delete(lang: Language) = if (lang == Language.ENGLISH) "Delete" else "Διαγραφή"
    fun confirm(lang: Language) = if (lang == Language.ENGLISH) "Confirm" else "Επιβεβαίωση"
    fun clear(lang: Language) = if (lang == Language.ENGLISH) "Clear" else "Καθαρισμός"
    fun view(lang: Language) = if (lang == Language.ENGLISH) "View" else "Προβολή"
    fun add(lang: Language) = if (lang == Language.ENGLISH) "Add" else "Προσθήκη"
    fun newAction(lang: Language) = if (lang == Language.ENGLISH) "New" else "Νέα"
    fun select(lang: Language) = if (lang == Language.ENGLISH) "Select" else "Επιλογή"
    fun copy(lang: Language) = if (lang == Language.ENGLISH) "Copy" else "Αντιγραφή"
    fun share(lang: Language) = if (lang == Language.ENGLISH) "Share" else "Κοινοποίηση"

    // Expenses Screen Hero Card & Metrics
    fun netBalanceLabel(lang: Language) = if (lang == Language.ENGLISH) "NET BALANCE" else "ΚΑΘΑΡΟ ΥΠΟΛΟΙΠΟ"
    fun withinBudget(lang: Language) = if (lang == Language.ENGLISH) "Within budget" else "Εντός προϋπολογισμού"
    fun overBudget(lang: Language) = if (lang == Language.ENGLISH) "Over budget" else "Υπέρβαση προϋπολογισμού"
    fun income(lang: Language) = if (lang == Language.ENGLISH) "Income" else "Έσοδα"
    fun expenses(lang: Language) = if (lang == Language.ENGLISH) "Expenses" else "Έξοδα"
    fun singleExpense(lang: Language) = if (lang == Language.ENGLISH) "Expense" else "Έξοδο"
    fun singleIncome(lang: Language) = if (lang == Language.ENGLISH) "Income" else "Έσοδο"
    fun expensesCount(lang: Language, count: Int) = if (lang == Language.ENGLISH) "💸 Expenses ($count)" else "💸 Έξοδα ($count)"
    fun incomesCount(lang: Language, count: Int) = if (lang == Language.ENGLISH) "💰 Income ($count)" else "💰 Έσοδα ($count)"
    fun remainingBudgetPrefix(lang: Language) = if (lang == Language.ENGLISH) "Remaining Budget:" else "Υπόλοιπο Budget:"
    fun ofBudget(lang: Language) = if (lang == Language.ENGLISH) "of budget" else "του budget"
    fun commonHousehold(lang: Language) = if (lang == Language.ENGLISH) "Household Shared" else "Κοινά Σπιτιού"
    fun individualPersonal(lang: Language) = if (lang == Language.ENGLISH) "Personal" else "Ατομικά"
    fun searchExpensesPlaceholder(lang: Language) = if (lang == Language.ENGLISH) "Search expense, member, amount..." else "Αναζήτηση εξόδου, μέλους, ποσού..."
    fun allFilter(lang: Language) = if (lang == Language.ENGLISH) "All" else "Όλα"
    fun allCategoriesFilter(lang: Language) = if (lang == Language.ENGLISH) "All Categories" else "Όλες οι Κατηγορίες"
    fun commonFamilyFilter(lang: Language) = if (lang == Language.ENGLISH) "Family Shared" else "Κοινά Οικογένειας"
    fun transactionsPlural(lang: Language, count: Int) = if (lang == Language.ENGLISH) "$count transactions" else "$count συναλλαγές"
    fun entriesPlural(lang: Language, count: Int) = if (lang == Language.ENGLISH) "$count entries" else "$count εγγραφές"
    fun selectMonthYear(lang: Language) = if (lang == Language.ENGLISH) "Select month & year" else "Επιλογή μήνα και έτους"
    fun allMonthsCombined(lang: Language) = if (lang == Language.ENGLISH) "All combined (All months)" else "Όλα μαζί (Όλοι οι μήνες)"
    fun currentMonth(lang: Language) = if (lang == Language.ENGLISH) "Current Month" else "Τρέχων Μήνας"
    fun noExpensesFound(lang: Language) = if (lang == Language.ENGLISH) "No expenses found" else "Δεν βρέθηκαν έξοδα"
    fun noExpensesForPeriod(lang: Language) = if (lang == Language.ENGLISH) "No recorded expenses for the selected period or member." else "Δεν υπάρχουν καταχωρημένα έξοδα για το επιλεγμένο διάστημα ή μέλος."
    fun noIncomesFound(lang: Language) = if (lang == Language.ENGLISH) "No income found" else "Δεν βρέθηκαν έσοδα"
    fun noIncomesForPeriod(lang: Language) = if (lang == Language.ENGLISH) "No recorded income for the selected period or member." else "Δεν υπάρχουν καταχωρημένα έσοδα για το επιλεγμένο διάστημα ή μέλος."
    fun recordNewExpense(lang: Language) = if (lang == Language.ENGLISH) "Record New Expense" else "Καταχώρηση Νέου Εξόδου"
    fun recordNewIncome(lang: Language) = if (lang == Language.ENGLISH) "Record New Income" else "Καταχώρηση Νέου Εσόδου"
    fun voiceInput(lang: Language) = if (lang == Language.ENGLISH) "Voice Input" else "Φωνητική Καταχώρηση"
    fun addExpense(lang: Language) = if (lang == Language.ENGLISH) "Manual Entry" else "Χειροκίνητη Προσθήκη"
    fun addIncome(lang: Language) = if (lang == Language.ENGLISH) "Manual Income" else "Χειροκίνητο Έσοδο"
    fun paidBy(lang: Language, member: String) = if (lang == Language.ENGLISH) "Paid by $member" else "Πληρώθηκε από $member"
    fun contributedBy(lang: Language, member: String) = if (lang == Language.ENGLISH) "Contributed by $member" else "Συνεισέφερε $member"

    // Analytics Screen & BudgetAlertsCard
    fun monthlyBudgetLimits(lang: Language) = if (lang == Language.ENGLISH) "Monthly Budget Limits" else "Όρια Προϋπολογισμού Μήνα"
    fun categoriesApproachingLimit(lang: Language, count: Int) = if (lang == Language.ENGLISH) "$count categories approaching limit (80%+)" else "$count κατηγορίες πλησιάζουν το όριο (80%+)"
    fun categoriesExceededLimit(lang: Language, count: Int) = if (lang == Language.ENGLISH) "$count categories exceeded budget limit!" else "$count κατηγορίες υπερέβησαν το όριο!"
    fun allBudgetsHealthy(lang: Language) = if (lang == Language.ENGLISH) "All category budgets are healthy (<80%)" else "Όλα τα όρια κατηγοριών είναι εντάξει (<80%)"
    fun statisticalAnalysis(lang: Language) = if (lang == Language.ENGLISH) "Statistical Analysis" else "Στατιστική Ανάλυση"
    fun monthlyBalanceSheet(lang: Language) = if (lang == Language.ENGLISH) "Monthly family balance sheet" else "Οικογενειακός ισολογισμός μήνα"
    fun balanceLabel(lang: Language) = if (lang == Language.ENGLISH) "Balance" else "Υπόλοιπο"
    fun breakEven(lang: Language) = if (lang == Language.ENGLISH) "Break-even" else "Ισοζύγιο"
    fun familyIncomeTitle(lang: Language) = if (lang == Language.ENGLISH) "Family Income" else "Έσοδα Οικογένειας"
    fun contributionPerMemberSources(lang: Language) = if (lang == Language.ENGLISH) "Contribution per member & sources" else "Συνεισφορά ανά μέλος & πηγές"
    fun noIncomeRecordedYet(lang: Language) = if (lang == Language.ENGLISH) "No income recorded yet" else "Δεν έχουν καταχωρηθεί ακόμα έσοδα"
    fun recordIncomeHint(lang: Language) = if (lang == Language.ENGLISH) "Record salaries, rent, bonuses etc. on the home screen with \"+ Income\"." else "Καταχωρήστε μισθούς, ενοίκια, επιδοτήσεις κ.α. στην αρχική οθόνη με το κουμπί «+ Έσοδο»."
    fun splitModeIndividual(lang: Language) = if (lang == Language.ENGLISH) "Individual" else "Ατομικά"
    fun splitModeParentsShared(lang: Language) = if (lang == Language.ENGLISH) "Parents + Shared" else "Γονείς + Κοινά"
    fun splitModeEqualAll(lang: Language) = if (lang == Language.ENGLISH) "Equal to all" else "Ισόποσα σε όλους"
    fun chartBars(lang: Language) = if (lang == Language.ENGLISH) "Bars" else "Μπάρες"
    fun chartPie(lang: Language) = if (lang == Language.ENGLISH) "Pie" else "Πίτα"
    fun excelButton(lang: Language) = "Excel"
    fun analyticsHeader(lang: Language) = if (lang == Language.ENGLISH) "EXPENSE ANALYTICS" else "ΑΝΑΛΥΣΗ ΕΞΟΔΩΝ"
    fun totalExpensesCard(lang: Language) = if (lang == Language.ENGLISH) "Total Expenses" else "Συνολικά Έξοδα"
    fun averagePerDay(lang: Language) = if (lang == Language.ENGLISH) "Daily Average" else "Ημερήσιος Μέσος Όρος"
    fun topCategory(lang: Language) = if (lang == Language.ENGLISH) "Top Category" else "Κορυφαία Κατηγορία"
    fun distributionByCategory(lang: Language) = if (lang == Language.ENGLISH) "Distribution by Category" else "Κατανομή ανά Κατηγορία"
    fun distributionByMember(lang: Language) = if (lang == Language.ENGLISH) "Distribution by Member" else "Κατανομή ανά Μέλος"
    fun monthlyComparison(lang: Language) = if (lang == Language.ENGLISH) "Monthly Comparison" else "Μηνιαία Σύγκριση"

    // Members Screen
    fun inviteMembersTitle(lang: Language) = if (lang == Language.ENGLISH) "Invite Users to Household" else "Πρόσκληση Χρηστών στο Εξοδολόγιο"
    fun proPaidFeature(lang: Language) = if (lang == Language.ENGLISH) "🔒 Paid feature (Fintel Pro)" else "🔒 Λειτουργία επί πληρωμή (Fintel Pro)"
    fun proActiveLiveSync(lang: Language) = if (lang == Language.ENGLISH) "Pro Active: Real-time expense sharing" else "Pro Ενεργό: Μοιραστείτε τα έξοδα ζωντανά"
    fun inviteCardDesc(lang: Language) = if (lang == Language.ENGLISH)
        "Connect family, partner or roommates to the same household to view and log expenses in real-time."
    else
        "Συνδέστε την οικογένεια, τον/την σύντροφο ή συνεργάτες στο ίδιο εξοδολόγιο ώστε να βλέπουν και να καταχωρούν έξοδα σε πραγματικό χρόνο."
    fun upgradeToProToInvite(lang: Language) = if (lang == Language.ENGLISH) "Upgrade to Pro to Invite" else "Αναβάθμιση σε Pro για Πρόσκληση"
    fun inviteMemberToHousehold(lang: Language) = if (lang == Language.ENGLISH) "Invite Member to Household" else "Πρόσκληση Μέλους στο Εξοδολόγιο"
    fun familyMembersHeader(lang: Language) = if (lang == Language.ENGLISH) "Family Members" else "Μέλη Οικογένειας"
    fun activeMembersCount(lang: Language, count: Int) = if (lang == Language.ENGLISH) "$count active members" else "$count ενεργά μέλη"
    fun membersVoiceKeywordsHint(lang: Language) = if (lang == Language.ENGLISH)
        "Set recognition keywords (e.g. \"dad\", \"mom\", \"kids\") so voice recognition knows who participates in each expense."
    else
        "Ορίστε λέξεις αναγνώρισης (π.χ. «μπαμπάς», «μαμά», «παιδιά») για να αναγνωρίζει η φωνή ποιος συμμετέχει σε κάθε έξοδο."
    fun selectColor(lang: Language) = if (lang == Language.ENGLISH) "Choose color" else "Επιλογή χρώματος"
    fun voiceKeywordsLabel(lang: Language) = if (lang == Language.ENGLISH) "Voice keywords:" else "Φωνητικές λέξεις:"
    fun addMember(lang: Language) = if (lang == Language.ENGLISH) "Add Member" else "Προσθήκη Μέλους"
    fun addMemberFab(lang: Language) = if (lang == Language.ENGLISH) "+ Add Member" else "+ Προσθήκη Μέλους"
    fun editMemberTitle(lang: Language) = if (lang == Language.ENGLISH) "Edit Member" else "Επεξεργασία Μέλους"
    fun memberNameLabel(lang: Language) = if (lang == Language.ENGLISH) "Member Name *" else "Όνομα Μέλους *"
    fun memberNamePlaceholder(lang: Language) = if (lang == Language.ENGLISH) "e.g. George, Mom, Maria" else "π.χ. Γιώργος, Μαμά, Μαρία"
    fun memberRoleLabel(lang: Language) = if (lang == Language.ENGLISH) "Role in household" else "Ρόλος στο σπίτι"
    fun memberKeywordsPlaceholder(lang: Language) = if (lang == Language.ENGLISH) "e.g. dad, father, george" else "π.χ. μπαμπάς, πατέρας, γιώργος"
    fun deleteMemberConfirmTitle(lang: Language) = if (lang == Language.ENGLISH) "Delete Member?" else "Διαγραφή Μέλους;"
    fun deleteMemberConfirmMsg(lang: Language, name: String) = if (lang == Language.ENGLISH)
        "Are you sure you want to delete member \"$name\"?"
    else
        "Είστε σίγουροι ότι θέλετε να διαγράψετε το μέλος «$name»;"

    // Categories Screen
    fun categoriesAndVoice(lang: Language) = if (lang == Language.ENGLISH) "Categories & Voice" else "Κατηγορίες & Φωνή"
    fun totalBudgetMonthly(lang: Language, amount: Double) = if (lang == Language.ENGLISH)
        "Total Budget: ${String.format(Locale.US, "%.0f €", amount)}/month"
    else
        "Συνολικό Budget: ${String.format(Locale.US, "%.0f €", amount)}/μήνα"
    fun categoriesDesc(lang: Language) = if (lang == Language.ENGLISH)
        "Each category connects to voice keywords. When you speak them, the app automatically detects the category."
    else
        "Κάθε κατηγορία συνδέεται με φωνητικές λέξεις-κλειδιά. Όταν τις προφέρετε, η εφαρμογή εντοπίζει αυτόματα την κατηγορία."
    fun budgetMonthlyLabel(lang: Language, amount: Double) = if (lang == Language.ENGLISH)
        "Budget: ${String.format(Locale.US, "%.0f €", amount)} / month"
    else
        "Budget: ${String.format(Locale.US, "%.0f €", amount)} / μήνα"
    fun addCategory(lang: Language) = if (lang == Language.ENGLISH) "Add Category" else "Προσθήκη Κατηγορίας"
    fun addCategoryFab(lang: Language) = if (lang == Language.ENGLISH) "+ Add Category" else "+ Προσθήκη Κατηγορίας"
    fun editCategoryTitle(lang: Language) = if (lang == Language.ENGLISH) "Edit Category" else "Επεξεργασία Κατηγορίας"
    fun categoryNameLabel(lang: Language) = if (lang == Language.ENGLISH) "Category Name *" else "Όνομα Κατηγορίας *"
    fun categoryNamePlaceholder(lang: Language) = if (lang == Language.ENGLISH) "e.g. Gym, Pets, Entertainment" else "π.χ. Γυμναστήριο, Κατοικίδια, Χόμπι"
    fun monthlyBudgetLimitLabel(lang: Language) = if (lang == Language.ENGLISH) "Monthly Budget Limit (€) - optional" else "Μηνιαίο Όριο Budget (€) - προαιρετικό"
    fun categoryIconLabel(lang: Language) = if (lang == Language.ENGLISH) "Choose Icon" else "Επιλογή Εικονιδίου"
    fun categoryKeywordsLabel(lang: Language) = if (lang == Language.ENGLISH) "Voice Keywords (comma separated)" else "Φωνητικές Λέξεις (χωρισμένες με κόμμα)"
    fun categoryKeywordsPlaceholder(lang: Language) = if (lang == Language.ENGLISH) "e.g. supermarket, groceries, food" else "π.χ. σουπερ μαρκετ, σκλαβενιτης, φαγητο"
    fun deleteCategoryConfirmTitle(lang: Language) = if (lang == Language.ENGLISH) "Delete Category?" else "Διαγραφή Κατηγορίας;"
    fun deleteCategoryConfirmMsg(lang: Language, name: String) = if (lang == Language.ENGLISH)
        "Are you sure you want to delete category \"$name\"?"
    else
        "Είστε σίγουροι ότι θέλετε να διαγράψετε την κατηγορία «$name»;"

    // Savings & Recurring Screen
    fun savingsTabTitle(lang: Language) = if (lang == Language.ENGLISH) "Savings Goals" else "Στόχοι Αποταμίευσης"
    fun recurringTabTitle(lang: Language) = if (lang == Language.ENGLISH) "Standing Orders" else "Πάγιες Εντολές"
    fun totalFamilySavings(lang: Language) = if (lang == Language.ENGLISH) "Total Family Savings" else "Συνολική Οικογενειακή Αποταμίευση"
    fun ofTarget(lang: Language) = if (lang == Language.ENGLISH) "of" else "από"
    fun ourGoals(lang: Language) = if (lang == Language.ENGLISH) "Our Goals" else "Οι Στόχοι Μας"
    fun newGoal(lang: Language) = if (lang == Language.ENGLISH) "New Goal" else "Νέος Στόχος"
    fun addGoal(lang: Language) = if (lang == Language.ENGLISH) "New Goal" else "Νέος Στόχος"
    fun editGoalTitle(lang: Language) = if (lang == Language.ENGLISH) "Edit Savings Goal" else "Επεξεργασία Στόχου"
    fun deadlinePrefix(lang: Language) = if (lang == Language.ENGLISH) "Deadline: " else "Ημερομηνία: "
    fun goalAchievedCongratulations(lang: Language) = if (lang == Language.ENGLISH) "🎉 Congratulations! Goal achieved!" else "🎉 Συγχαρητήρια! Ο στόχος επετεύχθη!"
    fun totalStandingOrders(lang: Language) = if (lang == Language.ENGLISH) "Total Standing Orders" else "Σύνολο Πάγιων Εντολών"
    fun newStandingOrder(lang: Language) = if (lang == Language.ENGLISH) "New Order" else "Νέα Πάγια"
    fun addRecurring(lang: Language) = if (lang == Language.ENGLISH) "New Order" else "Νέα Πάγια"
    fun editStandingOrder(lang: Language) = if (lang == Language.ENGLISH) "Edit Standing Order" else "Επεξεργασία Πάγιας Εντολής"
    fun perMonthSuffix(lang: Language) = if (lang == Language.ENGLISH) "/ month" else "/ μήνα"
    fun everyDayOfMonth(lang: Language, day: Int) = if (lang == Language.ENGLISH) "Every day $day of the month" else "Κάθε ${day}η του μήνα"
    fun executeNowAction(lang: Language) = if (lang == Language.ENGLISH) "Execute now" else "Εκτέλεση τώρα"
    fun goalTitleLabel(lang: Language) = if (lang == Language.ENGLISH) "Goal Title" else "Τίτλος στόχου"
    fun goalTitlePlaceholder(lang: Language) = if (lang == Language.ENGLISH) "e.g. Vacation, Car, Emergency" else "π.χ. Διακοπές, Αυτοκίνητο, Έκτακτη ανάγκη"
    fun targetAmountLabel(lang: Language) = if (lang == Language.ENGLISH) "Target Amount (€)" else "Ποσό στόχου (€)"
    fun currentSavedLabel(lang: Language) = if (lang == Language.ENGLISH) "Already Saved (€)" else "Αρχικό ποσό (€)"
    fun recurringDescLabel(lang: Language) = if (lang == Language.ENGLISH) "Description" else "Περιγραφή πάγιας"
    fun recurringDescPlaceholder(lang: Language) = if (lang == Language.ENGLISH) "e.g. Rent, Netflix, Electric bill" else "π.χ. Ενοίκιο, Netflix, ΔΕΗ"
    fun dayOfMonthLabel(lang: Language) = if (lang == Language.ENGLISH) "Day of month (1-31)" else "Ημέρα μήνα (1-31)"

    // Manual Expense Dialog
    fun recordExpenseTitle(lang: Language) = if (lang == Language.ENGLISH) "Record Expense" else "Καταγραφή Εξόδου"
    fun editExpenseTitle(lang: Language) = if (lang == Language.ENGLISH) "Edit Expense" else "Επεξεργασία Εξόδου"
    fun expenseDescLabel(lang: Language) = if (lang == Language.ENGLISH) "Description" else "Περιγραφή εξόδου"
    fun expenseDescPlaceholder(lang: Language) = if (lang == Language.ENGLISH) "e.g. Sklavenitis, Pharmacy, Coffee" else "π.χ. Σκλαβενίτης, Φαρμακείο, Καφές"
    fun expenseAmountLabel(lang: Language) = if (lang == Language.ENGLISH) "Amount (€)" else "Ποσό (€)"
    fun memberWhoPaid(lang: Language) = if (lang == Language.ENGLISH) "Payer" else "Πληρωτής"
    fun payersSectionTitle(lang: Language) = if (lang == Language.ENGLISH) "Who paid?" else "Ποιος πλήρωσε;"
    fun payersSubtitle(lang: Language) = if (lang == Language.ENGLISH) "Select one or more members who contributed" else "Επιλέξτε ένα ή περισσότερα μέλη που συνεισέφεραν"
    fun payerSplitModeEqual(lang: Language) = if (lang == Language.ENGLISH) "Equal split" else "Ισόποσα"
    fun payerSplitModeCustom(lang: Language) = if (lang == Language.ENGLISH) "Custom amounts" else "Προσαρμοσμένα ποσά"
    fun payerAllocationTitle(lang: Language, count: Int) = if (lang == Language.ENGLISH) "Payment split" else "Επιμερισμός πληρωμής"
    fun payerFullyCovered(lang: Language) = if (lang == Language.ENGLISH) "Fully covered ✓" else "Πλήρης κάλυψη ✓"
    fun payerRemaining(lang: Language, amount: String) = if (lang == Language.ENGLISH) "Remaining: $amount" else "Απομένουν: $amount"
    fun payerExceeded(lang: Language, amount: String) = if (lang == Language.ENGLISH) "Exceeded: +$amount" else "Υπέρβαση: +$amount"
    fun payerQuickBalance(lang: Language, name: String, amount: String) = if (lang == Language.ENGLISH) "Fill remainder to $name (+$amount)" else "Συμπλήρωση σε $name (+$amount)"
    fun paidSingle(lang: Language) = if (lang == Language.ENGLISH) "Paid:" else "Πλήρωσε:"
    fun paidMultiple(lang: Language) = if (lang == Language.ENGLISH) "Paid by:" else "Πλήρωσαν:"
    fun noMembersExpenseHint(lang: Language) = if (lang == Language.ENGLISH) "No members registered. The expense will be logged as Common." else "Δεν έχουν καταχωρηθεί μέλη. Το έξοδο θα καταχωρηθεί ως Κοινό."
    fun categoryLabel(lang: Language) = if (lang == Language.ENGLISH) "Category" else "Κατηγορία"
    fun date(lang: Language) = if (lang == Language.ENGLISH) "Date" else "Ημερομηνία"
    fun dateLabel(lang: Language) = if (lang == Language.ENGLISH) "Target Date" else "Προθεσμία"
    fun changeAction(lang: Language) = if (lang == Language.ENGLISH) "Change" else "Αλλαγή"
    fun noteLabel(lang: Language) = if (lang == Language.ENGLISH) "Note (optional)" else "Σημείωση (προαιρετικό)"
    fun notePlaceholder(lang: Language) = if (lang == Language.ENGLISH) "e.g. Paid by card" else "π.χ. Πληρώθηκε με κάρτα"
    fun fillDescriptionError(lang: Language) = if (lang == Language.ENGLISH) "Please enter a description" else "Παρακαλώ συμπληρώστε περιγραφή"
    fun fillAmountError(lang: Language) = if (lang == Language.ENGLISH) "Please enter a valid amount" else "Παρακαλώ συμπληρώστε έγκυρο ποσό"

    // Manual Income Dialog
    fun recordIncomeTitle(lang: Language) = if (lang == Language.ENGLISH) "Record Income" else "Καταγραφή Εσόδου"
    fun editIncomeTitle(lang: Language) = if (lang == Language.ENGLISH) "Edit Income" else "Επεξεργασία Εσόδου"
    fun incomeDescLabel(lang: Language) = if (lang == Language.ENGLISH) "Income Description *" else "Περιγραφή Εσόδου *"
    fun incomeDescPlaceholder(lang: Language) = if (lang == Language.ENGLISH) "e.g. October Salary, Rent" else "π.χ. Μισθός Οκτωβρίου, Ενοίκιο"
    fun incomeAmountLabel(lang: Language) = if (lang == Language.ENGLISH) "Income Amount (€) *" else "Ποσό Εσόδου (€) *"
    fun memberWhoContributed(lang: Language) = if (lang == Language.ENGLISH) "MEMBER WHO CONTRIBUTED *" else "ΜΕΛΟΣ ΠΟΥ ΣΥΝΕΙΣΕΦΕΡΕ *"
    fun noMembersIncomeHint(lang: Language) = if (lang == Language.ENGLISH) "No members registered. The income will be logged as Common." else "Δεν έχουν καταχωρηθεί μέλη. Το έσοδο θα καταχωρηθεί ως Κοινό."
    fun incomeCategoryLabel(lang: Language) = if (lang == Language.ENGLISH) "INCOME CATEGORY" else "ΚΑΤΗΓΟΡΙΑ ΕΣΟΔΟΥ"

    // Voice Dialogs
    fun voiceLoggingTitle(lang: Language) = if (lang == Language.ENGLISH) "Voice Logging" else "Φωνητική Καταγραφή"
    fun speakNowLabel(lang: Language) = if (lang == Language.ENGLISH) "Speak now..." else "Μιλήστε τώρα..."
    fun listeningStatus(lang: Language) = if (lang == Language.ENGLISH) "Listening..." else "Ακρόαση..."
    fun speechExamplesText(lang: Language) = if (lang == Language.ENGLISH)
        "Say for example:\n• \"Supermarket 45 euros dad\"\n• \"Pharmacy 12 euros mom\"\n• \"Coffee 3.50\""
    else
        "Πείτε για παράδειγμα:\n• «Σκλαβενίτης 45 ευρώ ο μπαμπάς»\n• «Φαρμακείο 12 ευρώ η μαμά»\n• «Καφές 3,50»"
    fun voiceConfirmationTitle(lang: Language) = if (lang == Language.ENGLISH) "Confirm Expense" else "Επιβεβαίωση Εξόδου"
    fun voiceDetectedDetails(lang: Language) = if (lang == Language.ENGLISH) "Detected Details" else "Αναγνωρισμένα Στοιχεία"
    fun tryAgainAction(lang: Language) = if (lang == Language.ENGLISH) "Try Again" else "Δοκιμάστε Ξανά"

    // Quick Action Chooser Dialog
    fun quickActionTitle(lang: Language) = if (lang == Language.ENGLISH) "Quick Expense Logging" else "Γρήγορη Καταγραφή Εξόδου"
    fun chooseMethodSubtitle(lang: Language) = if (lang == Language.ENGLISH) "Choose how you want to log your expense" else "Επιλέξτε πώς θέλετε να καταχωρήσετε το έξοδο"
    fun voiceOptionTitle(lang: Language) = if (lang == Language.ENGLISH) "Voice Recognition" else "Φωνητική Αναγνώριση"
    fun voiceOptionSubtitle(lang: Language) = if (lang == Language.ENGLISH) "Fast & easy with your voice" else "Γρήγορα & απλά με τη φωνή σας"
    fun manualOptionTitle(lang: Language) = if (lang == Language.ENGLISH) "Manual Entry" else "Χειροκίνητη Καταχώρηση"
    fun manualOptionSubtitle(lang: Language) = if (lang == Language.ENGLISH) "Type details in form" else "Πληκτρολόγηση στη φόρμα"
    fun dontShowAgainCheckbox(lang: Language) = if (lang == Language.ENGLISH) "Don't show again on launch" else "Να μην εμφανίζεται στην εκκίνηση"

    // Cloud Sync Card
    fun cloudSyncTitle(lang: Language) = if (lang == Language.ENGLISH) "Cloud Sync & Live Sharing" else "Συγχρονισμός Cloud & Κοινή Χρήση"
    fun cloudSyncSubtitle(lang: Language) = if (lang == Language.ENGLISH)
        "Collaborate in real-time with family or partners across multiple devices."
    else
        "Συνεργαστείτε σε πραγματικό χρόνο με την οικογένεια ή συνεργάτες σε πολλές συσκευές."
    fun householdCodeLabel(lang: Language) = if (lang == Language.ENGLISH) "Household Code:" else "Κωδικός Εξοδολογίου:"
    fun joinHouseholdTitle(lang: Language) = if (lang == Language.ENGLISH) "Join Existing Household" else "Σύνδεση σε Υπάρχον Εξοδολόγιο"
    fun enterCodePlaceholder(lang: Language) = if (lang == Language.ENGLISH) "Enter household code" else "Εισαγωγή κωδικού εξοδολογίου"
    fun joinAction(lang: Language) = if (lang == Language.ENGLISH) "Join" else "Σύνδεση"

    // Export Report Dialog
    fun exportReportTitle(lang: Language) = if (lang == Language.ENGLISH) "Export Report" else "Εξαγωγή Αναφοράς"
    fun exportReportSubtitle(lang: Language) = if (lang == Language.ENGLISH) "In Excel / CSV format" else "Σε μορφή Excel / CSV"
    fun periodLabel(lang: Language) = if (lang == Language.ENGLISH) "Period:" else "Περίοδος:"
    fun expenseCountLabel(lang: Language) = if (lang == Language.ENGLISH) "Expense Count:" else "Πλήθος Εξόδων:"
    fun totalAmountLabel(lang: Language) = if (lang == Language.ENGLISH) "Total Amount:" else "Συνολικό Ποσό:"
    fun shareReportAction(lang: Language) = if (lang == Language.ENGLISH) "Share Report (Email / Viber / Drive)" else "Κοινοποίηση (Email / Viber / Drive)"
    fun copyCsvAction(lang: Language) = if (lang == Language.ENGLISH) "Copy CSV text" else "Αντιγραφή κειμένου CSV"
    fun csvCopiedMessage(lang: Language) = if (lang == Language.ENGLISH) "CSV report copied to clipboard!" else "Η αναφορά CSV αντιγράφηκε στο πρόχειρο!"

    // User Profile Dialog
    fun myProfile(lang: Language) = if (lang == Language.ENGLISH) "My Profile" else "Το Προφίλ μου"
    fun fintelUser(lang: Language) = if (lang == Language.ENGLISH) "Fintel User" else "Χρήστης Fintel"
    fun freePlan(lang: Language) = if (lang == Language.ENGLISH) "Free Plan" else "Δωρεάν Πλάνο"
    fun proSubscriber(lang: Language) = if (lang == Language.ENGLISH) "Pro Subscriber" else "Συνδρομητής Pro"
    fun activeHouseholdCount(lang: Language, count: Int) = if (lang == Language.ENGLISH) "$count active household" else "$count ενεργό εξοδολόγιο"
    fun upgradeToPro(lang: Language) = if (lang == Language.ENGLISH) "Upgrade to Pro" else "Αναβάθμιση σε Pro"
    fun transactionsLabel(lang: Language) = if (lang == Language.ENGLISH) "Transactions" else "Συναλλαγές"
    fun totalExpensesLabel(lang: Language) = if (lang == Language.ENGLISH) "Total Expenses" else "Σύνολο Εξόδων"
    fun signOutAccount(lang: Language) = if (lang == Language.ENGLISH) "Sign Out Account" else "Αποσύνδεση Λογαριασμού"
    fun signOut(lang: Language) = if (lang == Language.ENGLISH) "Sign Out" else "Αποσύνδεση"

    // Exodologio Details & Manager Dialog
    fun myExodologio(lang: Language) = if (lang == Language.ENGLISH) "My Household Book" else "Το Εξοδολόγιό μου"
    fun activeExodologioHeader(lang: Language) = if (lang == Language.ENGLISH) "ACTIVE HOUSEHOLD" else "ΕΝΕΡΓΟ ΕΞΟΔΟΛΟΓΙΟ"
    fun exodologioMembers(lang: Language) = if (lang == Language.ENGLISH) "Household Members" else "Μέλη Εξοδολογίου"
    fun createNewExodologio(lang: Language) = if (lang == Language.ENGLISH) "Create New Household" else "Δημιουργία Νέου Εξοδολογίου"
    fun separateExodologio(lang: Language) = if (lang == Language.ENGLISH) "Separate household" else "Ξεχωριστό εξοδολόγιο"
    fun clearAllExpenses(lang: Language) = if (lang == Language.ENGLISH) "Clear all expenses" else "Διαγραφή όλων των εξόδων"
    fun clearAllExpensesConfirmTitle(lang: Language) = if (lang == Language.ENGLISH) "Delete All Expenses?" else "Διαγραφή Όλων των Εξόδων;"
    fun clearAllExpensesConfirmMsg(lang: Language) = if (lang == Language.ENGLISH)
        "Are you sure you want to delete all expenses in this household? This cannot be undone."
    else
        "Είστε σίγουροι ότι θέλετε να διαγράψετε όλα τα καταχωρημένα έξοδα αυτού του εξοδολογίου; Η ενέργεια δεν αναιρείται."

    // Burger Menu & Settings
    fun menuTitle(lang: Language) = if (lang == Language.ENGLISH) "Menu Options" else "Μενού Επιλογών"
    fun language(lang: Language) = if (lang == Language.ENGLISH) "Language" else "Γλώσσα"
    fun languageSection(lang: Language) = if (lang == Language.ENGLISH) "APP LANGUAGE" else "ΓΛΩΣΣΑ ΕΦΑΡΜΟΓΗΣ"
    fun appSettings(lang: Language) = if (lang == Language.ENGLISH) "App Settings" else "Ρυθμίσεις Εφαρμογής"
    fun appSettingsSubtitle(lang: Language) = if (lang == Language.ENGLISH) "Theme, language & preferences" else "Θέμα εμφάνισης, γλώσσα & προτιμήσεις"
    fun householdManager(lang: Language) = if (lang == Language.ENGLISH) "Manage Households" else "Διαχείριση Εξοδολογίων"
    fun themeDisplay(lang: Language) = if (lang == Language.ENGLISH) "DISPLAY THEME" else "ΘΕΜΑ ΕΜΦΑΝΙΣΗΣ"
    fun themeLight(lang: Language) = if (lang == Language.ENGLISH) "Light" else "Φωτεινό"
    fun themeDark(lang: Language) = if (lang == Language.ENGLISH) "Dark" else "Σκοτεινό"
    fun themeSystem(lang: Language) = if (lang == Language.ENGLISH) "System" else "Σύστημα"
    fun visualDesign(lang: Language) = if (lang == Language.ENGLISH) "VISUAL DESIGN & FONTS" else "ΣΧΕΔΙΑΣΜΟΣ & ΓΡΑΜΜΑΤΟΣΕΙΡΕΣ"
    fun styleModernFintech(lang: Language) = if (lang == Language.ENGLISH) "Modern FinTech" else "Μοντέρνο FinTech"
    fun styleModernFintechDesc(lang: Language) = if (lang == Language.ENGLISH) "New rounded geometric cards, donut chart & Plus Jakarta Sans" else "Νέες στρογγυλεμένες κάρτες, Plus Jakarta Sans & γραφιστικά"
    fun styleClassic(lang: Language) = if (lang == Language.ENGLISH) "Classic" else "Κλασικό"
    fun styleClassicDesc(lang: Language) = if (lang == Language.ENGLISH) "Original clean Material 3 look" else "Αρχικό κλασικό στυλ Material 3"
    fun preferencesSection(lang: Language) = if (lang == Language.ENGLISH) "PREFERENCES" else "ΠΡΟΤΙΜΗΣΕΙΣ"
    fun quickActionLaunch(lang: Language) = if (lang == Language.ENGLISH) "Quick Action on Launch" else "Γρήγορη επιλογή κατά την έναρξη"
    fun quickActionLaunchSubtitle(lang: Language) = if (lang == Language.ENGLISH) "Show Voice / Typing prompt dialog" else "Εμφάνιση μενού φωνής/πληκτρολόγησης"
    fun dataManagement(lang: Language) = if (lang == Language.ENGLISH) "DATA MANAGEMENT" else "ΔΙΑΧΕΙΡΙΣΗ ΔΕΔΟΜΕΝΩΝ"
    fun clearAllData(lang: Language) = if (lang == Language.ENGLISH) "Clear all local data" else "Εκκαθάριση όλων των δεδομένων"
    fun clearAllDataSubtitle(lang: Language) = if (lang == Language.ENGLISH) "Delete local records and reset app" else "Διαγραφή τοπικών εξόδων & επαναφορά"
    fun resetAction(lang: Language) = if (lang == Language.ENGLISH) "Reset" else "Επαναφορά"

    // Role translation
    fun translateRole(role: String, lang: Language): String {
        if (lang == Language.GREEK) return role
        return when (role.trim().lowercase()) {
            "γονέας", "γονεας" -> "Parent"
            "παιδί", "παιδι" -> "Child"
            "σύντροφος", "συντροφος" -> "Partner"
            "συγκάτοικος", "συγκατοικος" -> "Roommate"
            "συνεργάτης", "συνεργατης" -> "Associate"
            "φίλος / φίλη", "φιλος / φιλη", "φίλος", "φιλος", "φίλη", "φιλη" -> "Friend"
            "άλλο", "αλλο" -> "Other"
            "διαχειριστής", "διαχειριστης", "admin" -> "Admin"
            "μέλος", "μελος", "member" -> "Member"
            else -> role
        }
    }

    fun pluralRoleName(role: String, lang: Language): String {
        val r = role.trim().lowercase()
        return if (lang == Language.ENGLISH) {
            when {
                r.contains("γον") || r.contains("parent") -> "Parents"
                r.contains("παιδ") || r.contains("child") || r.contains("kid") -> "Children"
                r.contains("συγκάτοικ") || r.contains("συγκατοικ") || r.contains("roommate") -> "Roommates"
                r.contains("συνεργάτ") || r.contains("συνεργατ") || r.contains("partner") || r.contains("associate") -> "Partners"
                r.contains("σύντροφ") || r.contains("συντροφ") -> "Partners"
                r.contains("φίλ") || r.contains("φιλ") || r.contains("friend") -> "Friends"
                else -> role
            }
        } else {
            when {
                r.contains("γον") || r.contains("parent") -> "Γονείς"
                r.contains("παιδ") || r.contains("child") || r.contains("kid") -> "Παιδιά"
                r.contains("συγκάτοικ") || r.contains("συγκατοικ") || r.contains("roommate") -> "Συγκάτοικοι"
                r.contains("συνεργάτ") || r.contains("συνεργατ") || r.contains("partner") || r.contains("associate") -> "Συνεργάτες"
                r.contains("σύντροφ") || r.contains("συντροφ") -> "Σύντροφοι"
                r.contains("φίλ") || r.contains("φιλ") || r.contains("friend") -> "Φίλοι"
                else -> role
            }
        }
    }

    // Category Name translation for standard default categories
    fun translateCategory(name: String, lang: Language): String {
        if (lang == Language.GREEK) return name
        return when (name.trim().lowercase()) {
            "σούπερ μάρκετ", "σουπερ μαρκετ", "supermarket" -> "Supermarket"
            "ψιλικά", "ψιλικα", "kiosk" -> "Kiosk / Convenience"
            "δώρα", "δωρα", "gifts" -> "Gifts"
            "διασκέδαση", "διασκεδαση", "entertainment" -> "Entertainment"
            "γιατρός - φαρμακείο", "γιατρος - φαρμακειο", "υγεία", "υγεια", "pharmacy", "medical" -> "Doctor & Pharmacy"
            "λογαριασμοί", "λογαριασμοι", "bills", "utilities" -> "Bills & Utilities"
            "μετακινήσεις", "μετακινησεις", "transportation", "transport" -> "Transport"
            "σπίτι", "σπιτι", "home", "household" -> "Home & Living"
            "φαγητό έξω", "φαγητο εξω", "dining", "restaurants" -> "Dining Out"
            "εκπαίδευση", "εκπαιδευση", "education" -> "Education"
            "ρούχα", "ρουχα", "shopping", "clothes" -> "Shopping & Clothes"
            "διάφορα", "διαφορα", "other", "general" -> "General & Other"
            // Income categories
            "μισθός", "μισθος", "salary" -> "Salary"
            "ενοίκιο", "ενοικιο", "rent" -> "Rent"
            "επιχείρηση / freelance", "επιχειρηση / freelance", "freelance" -> "Business / Freelance"
            "επιδότηση", "επιδοτηση", "subsidy" -> "Subsidy / Allowance"
            "bonus / δώρο", "bonus / δωρο", "bonus" -> "Bonus / Gift"
            "επενδύσεις", "επενδυσεις", "investments" -> "Investments"
            else -> name
        }
    }

    fun translateHouseholdName(name: String, lang: Language): String {
        if (lang == Language.GREEK) return name
        if (name.trim().equals("Το Εξοδολόγιό μου", ignoreCase = true)) {
            return "My Household Book"
        }
        if (name.trim().equals("Οικογενειακό Εξοδολόγιο", ignoreCase = true)) {
            return "Family Household Book"
        }
        if (name.trim().equals("Προσωπικό Εξοδολόγιο", ignoreCase = true)) {
            return "Personal Household Book"
        }
        return name
    }

    // Analytics Screen Strings
    fun analyticsStatisticalAnalysis(lang: Language) = if (lang == Language.ENGLISH) "Statistical Analysis" else "Στατιστική Ανάλυση"
    fun familyMonthlyBalance(lang: Language) = if (lang == Language.ENGLISH) "Family monthly balance" else "Οικογενειακός ισολογισμός μήνα"
    fun exportToExcel(lang: Language) = if (lang == Language.ENGLISH) "Export to Excel" else "Εξαγωγή σε Excel"
    fun entriesCount(count: Int, lang: Language) = if (lang == Language.ENGLISH) "$count entries" else "$count εγγραφές"
    fun transactionsCount(count: Int, lang: Language) = if (lang == Language.ENGLISH) "$count transactions" else "$count συναλλαγές"
    fun savingsRateLabel(pct: Int, lang: Language) = if (lang == Language.ENGLISH) "Savings: $pct%" else "Αποταμ: $pct%"
    fun balanceWord(lang: Language) = if (lang == Language.ENGLISH) "Balance" else "Ισοζύγιο"
    fun netBalanceTitle(lang: Language) = if (lang == Language.ENGLISH) "Balance" else "Υπόλοιπο"
    fun familyIncome(lang: Language) = if (lang == Language.ENGLISH) "Family Income" else "Έσοδα Οικογένειας"
    fun contributionByMember(lang: Language) = if (lang == Language.ENGLISH) "Contribution by member & sources" else "Συνεισφορά ανά μέλος & πηγές"
    fun noIncomesYet(lang: Language) = if (lang == Language.ENGLISH) "No income registered yet" else "Δεν έχουν καταχωρηθεί ακόμα έσοδα"
    fun registerIncomeHint(lang: Language) = if (lang == Language.ENGLISH) "Register salaries, rents, allowances, etc. from the home screen with «+ Income»." else "Καταχωρήστε μισθούς, ενοίκια, επιδοτήσεις κ.α. από την αρχική οθόνη με το κουμπί «+ Έσοδο»."
    fun commonOther(lang: Language) = if (lang == Language.ENGLISH) "Common / Other" else "Κοινό / Άλλο"
    fun commonOtherIncome(lang: Language) = if (lang == Language.ENGLISH) "Common / Other Income" else "Κοινό / Λοιπά Έσοδα"
    fun unassignedMemberIncome(pct: Int, lang: Language) = if (lang == Language.ENGLISH) "Unassigned to member ($pct%)" else "Μη αντιστοιχισμένα σε μέλος ($pct%)"
    fun incomeSourcesByCategory(lang: Language) = if (lang == Language.ENGLISH) "Income Sources by Category" else "Πηγές Εσόδων ανά Κατηγορία"
    fun expenseDistribution(lang: Language) = if (lang == Language.ENGLISH) "Expense Distribution" else "Κατανομή Εξόδων"
    fun parents(lang: Language) = if (lang == Language.ENGLISH) "Parents" else "Γονείς"
    fun children(lang: Language) = if (lang == Language.ENGLISH) "Children" else "Παιδιά"
    fun breakdownByMember(lang: Language) = if (lang == Language.ENGLISH) "Breakdown by Member" else "Καταμερισμός ανά Μέλος"
    fun individualSpendingSubtitle(lang: Language) = if (lang == Language.ENGLISH) "Personal expenses per family member" else "Προσωπικά έξοδα ανά μέλος της οικογένειας"
    fun parentsSpendingSubtitle(lang: Language) = if (lang == Language.ENGLISH) "Parents expenses & shared household coverage" else "Έξοδα γονέων & κάλυψη κοινών εξόδων σπιτιού"
    fun kidsSpendingSubtitle(lang: Language) = if (lang == Language.ENGLISH) "Expenses & activities for children" else "Έξοδα & δραστηριότητες για τα παιδιά"
    fun individualMode(lang: Language) = if (lang == Language.ENGLISH) "Individual" else "Ατομικά"
    fun parentsMode(lang: Language) = if (lang == Language.ENGLISH) "Parents" else "Γονείς"
    fun kidsMode(lang: Language) = if (lang == Language.ENGLISH) "Children" else "Παιδιά"
    fun budgetMonitoring(lang: Language) = if (lang == Language.ENGLISH) "Budget Monitoring" else "Έλεγχος Budgets"
    fun categoriesCount(count: Int, lang: Language) = if (lang == Language.ENGLISH) "$count categories" else "$count κατηγορίες"
    fun exceededBudget(lang: Language) = if (lang == Language.ENGLISH) "Exceeded" else "Υπέρβαση"
    fun warningBudget(lang: Language) = if (lang == Language.ENGLISH) "Warning" else "Προσοχή"
    fun atBudget(lang: Language) = if (lang == Language.ENGLISH) "At budget limit (100%)" else "Στο όριο (100%)"
    fun withinLimit(lang: Language) = if (lang == Language.ENGLISH) "Within limit" else "Εντός ορίου"
    fun memberIncomeDeficitTitle(lang: Language) = if (lang == Language.ENGLISH) "Expenses Exceeded Declared Income" else "Υπέρβαση Εσόδων Μέλους"
    fun memberIncomeDeficitDesc(lang: Language) = if (lang == Language.ENGLISH) "Member has paid more in expenses than their declared income for this period." else "Το μέλος έχει πληρώσει περισσότερα έξοδα από όσα έχει δηλώσει ως έσοδα για το επιλεγμένο διάστημα."
    fun deficitLabel(lang: Language) = if (lang == Language.ENGLISH) "Deficit" else "Έλλειμμα"
    fun expensesExceedIncomeTag(lang: Language) = if (lang == Language.ENGLISH) "Expenses > Income" else "Έξοδα > Έσοδα"
    fun barChartView(lang: Language) = if (lang == Language.ENGLISH) "Bar Chart View" else "Προβολή Μπάρας"
    fun pieChartView(lang: Language) = if (lang == Language.ENGLISH) "Pie Chart View" else "Προβολή Πίτας"
    fun noExpensesRegistered(lang: Language) = if (lang == Language.ENGLISH) "No expenses registered" else "Δεν υπάρχουν καταχωρημένα έξοδα"
    fun total(lang: Language) = if (lang == Language.ENGLISH) "Total" else "Σύνολο"
    fun noKidsRegisteredHint(lang: Language) = if (lang == Language.ENGLISH) "No members with role «Child» registered." else "Δεν έχουν καταχωρηθεί μέλη με ρόλο «Παιδί»."
    fun noMembersInCategoryHint(lang: Language) = if (lang == Language.ENGLISH) "No members in this category." else "Δεν υπάρχουν μέλη στη συγκεκριμένη κατηγορία."
    fun percentOfTotalIncome(pct: Int, lang: Language) = if (lang == Language.ENGLISH) "$pct% of total income" else "$pct% του συνόλου εσόδων"
    fun individualExpensesSubtitle(role: String, lang: Language) = if (lang == Language.ENGLISH) "${translateRole(role, lang)} • Individual expenses" else "$role • Ατομικά έξοδα"
    fun kidsExpensesSubtitle(lang: Language) = if (lang == Language.ENGLISH) "Child • Expenses & activities" else "Παιδί • Έξοδα & δραστηριότητες"
    fun incomes(lang: Language) = if (lang == Language.ENGLISH) "Income" else "Έσοδα"
    fun loadingExpenses(lang: Language) = if (lang == Language.ENGLISH) "Loading expenses..." else "Φόρτωση εξόδων..."
}
