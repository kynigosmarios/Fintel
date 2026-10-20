package com.example.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Localization {
    enum class Language(val code: String, val displayName: String, val flag: String) {
        GREEK("el", "Ελληνικά", "🇬🇷"),
        ENGLISH("en", "English", "🇬🇧");

        companion object {
            fun fromCode(code: String): Language = when (code.lowercase()) {
                "en", "english" -> ENGLISH
                else -> GREEK
            }
        }
    }

    fun getDateLocale(lang: Language): Locale = if (lang == Language.ENGLISH) Locale.ENGLISH else Locale("el", "GR")

    fun formatMonthYear(timestamp: Long, lang: Language): String {
        val sdf = SimpleDateFormat("MMMM yyyy", getDateLocale(lang))
        return sdf.format(Date(timestamp))
    }

    fun formatDayMonth(timestamp: Long, lang: Language): String {
        val sdf = SimpleDateFormat("d MMM", getDateLocale(lang))
        return sdf.format(Date(timestamp))
    }
    fun appName(lang: Language): String = "Fintel"
    fun appTagline(lang: Language): String = if (lang == Language.ENGLISH) "Smart Expense Tracker" else "Έξυπνο Εξοδολόγιο"
    fun tabExpenses(lang: Language): String = if (lang == Language.ENGLISH) "Expenses" else "Έξοδα"
    fun tabAnalytics(lang: Language): String = if (lang == Language.ENGLISH) "Analytics" else "Αναλυτικά"
    fun tabMembers(lang: Language): String = if (lang == Language.ENGLISH) "Members" else "Μέλη"
    fun tabCategories(lang: Language): String = if (lang == Language.ENGLISH) "Categories" else "Κατηγορίες"
    fun tabSavings(lang: Language): String = if (lang == Language.ENGLISH) "Savings" else "Αποταμίευση"
    fun today(lang: Language): String = if (lang == Language.ENGLISH) "Today" else "Σήμερα"
    fun yesterday(lang: Language): String = if (lang == Language.ENGLISH) "Yesterday" else "Χθες"
    fun close(lang: Language): String = if (lang == Language.ENGLISH) "Close" else "Κλείσιμο"
    fun cancel(lang: Language): String = if (lang == Language.ENGLISH) "Cancel" else "Ακύρωση"
    fun save(lang: Language): String = if (lang == Language.ENGLISH) "Save" else "Αποθήκευση"
    fun edit(lang: Language): String = if (lang == Language.ENGLISH) "Edit" else "Επεξεργασία"
    fun delete(lang: Language): String = if (lang == Language.ENGLISH) "Delete" else "Διαγραφή"
    fun confirm(lang: Language): String = if (lang == Language.ENGLISH) "Confirm" else "Επιβεβαίωση"
    fun clear(lang: Language): String = if (lang == Language.ENGLISH) "Clear" else "Καθαρισμός"
    fun view(lang: Language): String = if (lang == Language.ENGLISH) "View" else "Προβολή"
    fun add(lang: Language): String = if (lang == Language.ENGLISH) "Add" else "Προσθήκη"
    fun newAction(lang: Language): String = if (lang == Language.ENGLISH) "New" else "Νέα"
    fun select(lang: Language): String = if (lang == Language.ENGLISH) "Select" else "Επιλογή"
    fun copy(lang: Language): String = if (lang == Language.ENGLISH) "Copy" else "Αντιγραφή"
    fun share(lang: Language): String = if (lang == Language.ENGLISH) "Share" else "Κοινοποίηση"
    fun netBalanceLabel(lang: Language): String = if (lang == Language.ENGLISH) "NET BALANCE" else "ΚΑΘΑΡΟ ΥΠΟΛΟΙΠΟ"
    fun withinBudget(lang: Language): String = if (lang == Language.ENGLISH) "Within budget" else "Εντός προϋπολογισμού"
    fun overBudget(lang: Language): String = if (lang == Language.ENGLISH) "Over budget" else "Υπέρβαση προϋπολογισμού"
    fun income(lang: Language): String = if (lang == Language.ENGLISH) "Income" else "Έσοδα"
    fun expenses(lang: Language): String = if (lang == Language.ENGLISH) "Expenses" else "Έξοδα"
    fun singleExpense(lang: Language): String = if (lang == Language.ENGLISH) "Expense" else "Έξοδο"
    fun singleIncome(lang: Language): String = if (lang == Language.ENGLISH) "Income" else "Έσοδο"
    fun remainingBudgetPrefix(lang: Language): String = if (lang == Language.ENGLISH) "Remaining Budget:" else "Υπόλοιπο Budget:"
    fun ofBudget(lang: Language): String = if (lang == Language.ENGLISH) "of budget" else "του budget"
    fun commonHousehold(lang: Language): String = if (lang == Language.ENGLISH) "Household Shared" else "Κοινά Σπιτιού"
    fun individualPersonal(lang: Language): String = if (lang == Language.ENGLISH) "Personal" else "Ατομικά"
    fun searchExpensesPlaceholder(lang: Language): String = if (lang == Language.ENGLISH) "Search expense, member, amount..." else "Αναζήτηση εξόδου, μέλους, ποσού..."
    fun allFilter(lang: Language): String = if (lang == Language.ENGLISH) "All" else "Όλα"
    fun allCategoriesFilter(lang: Language): String = if (lang == Language.ENGLISH) "All Categories" else "Όλες οι Κατηγορίες"
    fun commonFamilyFilter(lang: Language): String = if (lang == Language.ENGLISH) "Family Shared" else "Κοινά Οικογένειας"
    fun selectMonthYear(lang: Language): String = if (lang == Language.ENGLISH) "Select month & year" else "Επιλογή μήνα και έτους"
    fun allMonthsCombined(lang: Language): String = if (lang == Language.ENGLISH) "All combined (All months)" else "Όλα μαζί (Όλοι οι μήνες)"
    fun currentMonth(lang: Language): String = if (lang == Language.ENGLISH) "Current Month" else "Τρέχων Μήνας"
    fun noExpensesFound(lang: Language): String = if (lang == Language.ENGLISH) "No expenses found" else "Δεν βρέθηκαν έξοδα"
    fun noExpensesForPeriod(lang: Language): String = if (lang == Language.ENGLISH) "No recorded expenses for the selected period or member." else "Δεν υπάρχουν καταχωρημένα έξοδα για το επιλεγμένο διάστημα ή μέλος."
    fun noIncomesFound(lang: Language): String = if (lang == Language.ENGLISH) "No income found" else "Δεν βρέθηκαν έσοδα"
    fun noIncomesForPeriod(lang: Language): String = if (lang == Language.ENGLISH) "No recorded income for the selected period or member." else "Δεν υπάρχουν καταχωρημένα έσοδα για το επιλεγμένο διάστημα ή μέλος."
    fun recordNewExpense(lang: Language): String = if (lang == Language.ENGLISH) "Record New Expense" else "Καταχώρηση Νέου Εξόδου"
    fun recordNewIncome(lang: Language): String = if (lang == Language.ENGLISH) "Record New Income" else "Καταχώρηση Νέου Εσόδου"
    fun voiceInput(lang: Language): String = if (lang == Language.ENGLISH) "Voice Input" else "Φωνητική Καταχώρηση"
    fun addExpense(lang: Language): String = if (lang == Language.ENGLISH) "Manual Entry" else "Χειροκίνητη Προσθήκη"
    fun addIncome(lang: Language): String = if (lang == Language.ENGLISH) "Manual Income" else "Χειροκίνητο Έσοδο"
    fun monthlyBudgetLimits(lang: Language): String = if (lang == Language.ENGLISH) "Monthly Budget Limits" else "Όρια Προϋπολογισμού Μήνα"
    fun allBudgetsHealthy(lang: Language): String = if (lang == Language.ENGLISH) "All category budgets are healthy (<80%)" else "Όλα τα όρια κατηγοριών είναι εντάξει (<80%)"
    fun statisticalAnalysis(lang: Language): String = if (lang == Language.ENGLISH) "Statistical Analysis" else "Στατιστική Ανάλυση"
    fun monthlyBalanceSheet(lang: Language): String = if (lang == Language.ENGLISH) "Monthly balance sheet" else "Μηνιαίος Ισολογισμός"
    fun balanceLabel(lang: Language): String = if (lang == Language.ENGLISH) "Balance" else "Υπόλοιπο"
    fun breakEven(lang: Language): String = if (lang == Language.ENGLISH) "Break-even" else "Ισοζύγιο"
    fun familyIncomeTitle(lang: Language): String = if (lang == Language.ENGLISH) "Total Income" else "Συνολικά Έσοδα"
    fun contributionPerMemberSources(lang: Language): String = if (lang == Language.ENGLISH) "Contribution per member & sources" else "Συνεισφορά ανά μέλος & πηγές"
    fun noIncomeRecordedYet(lang: Language): String = if (lang == Language.ENGLISH) "No income recorded yet" else "Δεν έχουν καταχωρηθεί ακόμα έσοδα"
    fun recordIncomeHint(lang: Language): String = if (lang == Language.ENGLISH) "Record salaries, rent, bonuses etc. on the home screen with \"+ Income\"." else "Καταχωρήστε μισθούς, ενοίκια, επιδοτήσεις κ.α. στην αρχική οθόνη με το κουμπί «+ Έσοδο»."
    fun splitModeIndividual(lang: Language): String = if (lang == Language.ENGLISH) "Individual" else "Ατομικά"
    fun splitModeParentsShared(lang: Language): String = if (lang == Language.ENGLISH) "Parents + Shared" else "Γονείς + Κοινά"
    fun splitModeEqualAll(lang: Language): String = if (lang == Language.ENGLISH) "Equal to all" else "Ισόποσα σε όλους"
    fun chartBars(lang: Language): String = if (lang == Language.ENGLISH) "Bars" else "Μπάρες"
    fun chartPie(lang: Language): String = if (lang == Language.ENGLISH) "Pie" else "Πίτα"
    fun excelButton(lang: Language): String = "Excel"
    fun analyticsHeader(lang: Language): String = if (lang == Language.ENGLISH) "EXPENSE ANALYTICS" else "ΑΝΑΛΥΣΗ ΕΞΟΔΩΝ"
    fun totalExpensesCard(lang: Language): String = if (lang == Language.ENGLISH) "Total Expenses" else "Συνολικά Έξοδα"
    fun averagePerDay(lang: Language): String = if (lang == Language.ENGLISH) "Daily Average" else "Ημερήσιος Μέσος Όρος"
    fun topCategory(lang: Language): String = if (lang == Language.ENGLISH) "Top Category" else "Κορυφαία Κατηγορία"
    fun distributionByCategory(lang: Language): String = if (lang == Language.ENGLISH) "Distribution by Category" else "Κατανομή ανά Κατηγορία"
    fun distributionByMember(lang: Language): String = if (lang == Language.ENGLISH) "Distribution by Member" else "Κατανομή ανά Μέλος"
    fun monthlyComparison(lang: Language): String = if (lang == Language.ENGLISH) "Monthly Comparison" else "Μηνιαία Σύγκριση"
    fun inviteMembersTitle(lang: Language): String = if (lang == Language.ENGLISH) "Invite Users to Household" else "Πρόσκληση Χρηστών στο Εξοδολόγιο"
    fun proPaidFeature(lang: Language): String = if (lang == Language.ENGLISH) "🔒 Paid feature (Fintel Pro)" else "🔒 Λειτουργία επί πληρωμή (Fintel Pro)"
    fun proActiveLiveSync(lang: Language): String = if (lang == Language.ENGLISH) "Pro Active: Real-time expense sharing" else "Pro Ενεργό: Μοιραστείτε τα έξοδα ζωντανά"
    fun inviteCardDesc(lang: Language): String = "Connect family, partner or roommates to the same household to view and log expenses in real-time."
    fun upgradeToProToInvite(lang: Language): String = if (lang == Language.ENGLISH) "Upgrade to Pro to Invite" else "Αναβάθμιση σε Pro για Πρόσκληση"
    fun inviteMemberToHousehold(lang: Language): String = if (lang == Language.ENGLISH) "Invite Member to Household" else "Πρόσκληση Μέλους στο Εξοδολόγιο"
    fun familyMembersHeader(lang: Language): String = if (lang == Language.ENGLISH) "Family Members" else "Μέλη Οικογένειας"
    fun membersVoiceKeywordsHint(lang: Language): String = "Set recognition keywords (e.g. \"dad\", \"mom\", \"kids\") so voice recognition knows who participates in each expense."
    fun selectColor(lang: Language): String = if (lang == Language.ENGLISH) "Choose color" else "Επιλογή χρώματος"
    fun voiceKeywordsLabel(lang: Language): String = if (lang == Language.ENGLISH) "Voice keywords:" else "Φωνητικές λέξεις:"
    fun addMember(lang: Language): String = if (lang == Language.ENGLISH) "Add Member" else "Προσθήκη Μέλους"
    fun addMemberFab(lang: Language): String = if (lang == Language.ENGLISH) "Add Member" else "Προσθήκη Μέλους"
    fun editMemberTitle(lang: Language): String = if (lang == Language.ENGLISH) "Edit Member" else "Επεξεργασία Μέλους"
    fun memberNameLabel(lang: Language): String = if (lang == Language.ENGLISH) "Member Name *" else "Όνομα Μέλους *"
    fun memberNamePlaceholder(lang: Language): String = if (lang == Language.ENGLISH) "e.g. George, Mom, Maria" else "π.χ. Γιώργος, Μαμά, Μαρία"
    fun memberRoleLabel(lang: Language): String = if (lang == Language.ENGLISH) "Role in household" else "Ρόλος στο σπίτι"
    fun memberKeywordsPlaceholder(lang: Language): String = if (lang == Language.ENGLISH) "e.g. dad, father, george" else "π.χ. μπαμπάς, πατέρας, γιώργος"
    fun deleteMemberConfirmTitle(lang: Language): String = if (lang == Language.ENGLISH) "Delete Member?" else "Διαγραφή Μέλους"
    fun categoriesAndVoice(lang: Language): String = if (lang == Language.ENGLISH) "Categories & Voice" else "Κατηγορίες & Φωνή"
    fun categoriesDesc(lang: Language): String = "Each category connects to voice keywords. When you speak them, the app automatically detects the category."
    fun addCategory(lang: Language): String = if (lang == Language.ENGLISH) "Add Category" else "Προσθήκη Κατηγορίας"
    fun addCategoryFab(lang: Language): String = if (lang == Language.ENGLISH) "Add Category" else "Προσθήκη Κατηγορίας"
    fun editCategoryTitle(lang: Language): String = if (lang == Language.ENGLISH) "Edit Category" else "Επεξεργασία Κατηγορίας"
    fun categoryNameLabel(lang: Language): String = if (lang == Language.ENGLISH) "Category Name *" else "Όνομα Κατηγορίας *"
    fun categoryNamePlaceholder(lang: Language): String = if (lang == Language.ENGLISH) "e.g. Gym, Pets, Entertainment" else "π.χ. Γυμναστήριο, Κατοικίδια, Χόμπι"
    fun monthlyBudgetLimitLabel(lang: Language): String = if (lang == Language.ENGLISH) "Monthly Budget Limit (€) - optional" else "Μηνιαίο Όριο Budget (€) - προαιρετικό"
    fun categoryIconLabel(lang: Language): String = if (lang == Language.ENGLISH) "Choose Icon" else "Επιλογή Εικονιδίου"
    fun categoryKeywordsLabel(lang: Language): String = if (lang == Language.ENGLISH) "Voice Keywords (comma separated)" else "Φωνητικές Λέξεις (χωρισμένες με κόμμα)"
    fun categoryKeywordsPlaceholder(lang: Language): String = if (lang == Language.ENGLISH) "e.g. supermarket, groceries, food" else "π.χ. σουπερ μαρκετ, σκλαβενιτης, φαγητο"
    fun deleteCategoryConfirmTitle(lang: Language): String = if (lang == Language.ENGLISH) "Delete Category?" else "Διαγραφή Κατηγορίας"
    fun savingsTabTitle(lang: Language): String = if (lang == Language.ENGLISH) "Savings Goals" else "Στόχοι Αποταμίευσης"
    fun recurringTabTitle(lang: Language): String = if (lang == Language.ENGLISH) "Standing Orders" else "Πάγιες Εντολές"
    fun totalFamilySavings(lang: Language): String = if (lang == Language.ENGLISH) "Total Savings" else "Συνολική Αποταμίευση"
    fun ofTarget(lang: Language): String = if (lang == Language.ENGLISH) "of" else "από"
    fun ourGoals(lang: Language): String = if (lang == Language.ENGLISH) "Our Goals" else "Οι Στόχοι Μας"
    fun newGoal(lang: Language): String = if (lang == Language.ENGLISH) "New Goal" else "Νέος Στόχος"
    fun addGoal(lang: Language): String = if (lang == Language.ENGLISH) "New Goal" else "Νέος Στόχος"
    fun editGoalTitle(lang: Language): String = if (lang == Language.ENGLISH) "Edit Savings Goal" else "Επεξεργασία Στόχου"
    fun deadlinePrefix(lang: Language): String = if (lang == Language.ENGLISH) "Deadline: " else "Ημερομηνία: "
    fun goalAchievedCongratulations(lang: Language): String = if (lang == Language.ENGLISH) "🎉 Congratulations! Goal achieved!" else "🎉 Συγχαρητήρια! Ο στόχος επετεύχθη!"
    fun totalStandingOrders(lang: Language): String = if (lang == Language.ENGLISH) "Total Standing Orders" else "Σύνολο Πάγιων Εντολών"
    fun newStandingOrder(lang: Language): String = if (lang == Language.ENGLISH) "New Order" else "Νέα Πάγια"
    fun addRecurring(lang: Language): String = if (lang == Language.ENGLISH) "New Order" else "Νέα Πάγια"
    fun editStandingOrder(lang: Language): String = if (lang == Language.ENGLISH) "Edit Standing Order" else "Επεξεργασία Πάγιας Εντολής"
    fun perMonthSuffix(lang: Language): String = if (lang == Language.ENGLISH) "/ month" else "/ μήνα"
    fun executeNowAction(lang: Language): String = if (lang == Language.ENGLISH) "Execute now" else "Εκτέλεση τώρα"
    fun goalTitleLabel(lang: Language): String = if (lang == Language.ENGLISH) "Goal Title" else "Τίτλος στόχου"
    fun goalTitlePlaceholder(lang: Language): String = if (lang == Language.ENGLISH) "e.g. Vacation, Car, Emergency" else "π.χ. Διακοπές, Αυτοκίνητο, Έκτακτη ανάγκη"
    fun targetAmountLabel(lang: Language): String = if (lang == Language.ENGLISH) "Target Amount (€)" else "Ποσό στόχου (€)"
    fun currentSavedLabel(lang: Language): String = if (lang == Language.ENGLISH) "Already Saved (€)" else "Αρχικό ποσό (€)"
    fun recurringDescLabel(lang: Language): String = if (lang == Language.ENGLISH) "Description" else "Περιγραφή πάγιας"
    fun recurringDescPlaceholder(lang: Language): String = if (lang == Language.ENGLISH) "e.g. Rent, Netflix, Electric bill" else "π.χ. Ενοίκιο, Netflix, ΔΕΗ"
    fun dayOfMonthLabel(lang: Language): String = if (lang == Language.ENGLISH) "Day of month (1-31)" else "Ημέρα μήνα (1-31)"
    fun recordExpenseTitle(lang: Language): String = if (lang == Language.ENGLISH) "Record Expense" else "Καταγραφή Εξόδου"
    fun editExpenseTitle(lang: Language): String = if (lang == Language.ENGLISH) "Edit Expense" else "Επεξεργασία Εξόδου"
    fun expenseDescLabel(lang: Language): String = if (lang == Language.ENGLISH) "Description" else "Περιγραφή εξόδου"
    fun expenseDescPlaceholder(lang: Language): String = if (lang == Language.ENGLISH) "e.g. Sklavenitis, Pharmacy, Coffee" else "π.χ. Σκλαβενίτης, Φαρμακείο, Καφές"
    fun expenseAmountLabel(lang: Language): String = if (lang == Language.ENGLISH) "Amount (€)" else "Ποσό (€)"
    fun memberWhoPaid(lang: Language): String = if (lang == Language.ENGLISH) "Payer" else "Πληρωτής"
    fun payersSectionTitle(lang: Language): String = if (lang == Language.ENGLISH) "Who paid?" else "Ποιος πλήρωσε"
    fun payersSubtitle(lang: Language): String = if (lang == Language.ENGLISH) "Select one or more members who contributed" else "Επιλέξτε ένα ή περισσότερα μέλη που συνεισέφεραν"
    fun payerSplitModeEqual(lang: Language): String = if (lang == Language.ENGLISH) "Equal split" else "Ισόποσα"
    fun payerSplitModeCustom(lang: Language): String = if (lang == Language.ENGLISH) "Custom amounts" else "Προσαρμοσμένα ποσά"
    fun payerFullyCovered(lang: Language): String = if (lang == Language.ENGLISH) "Fully covered ✓" else "Πλήρης κάλυψη ✓"
    fun paidSingle(lang: Language): String = if (lang == Language.ENGLISH) "Paid:" else "Πλήρωσε:"
    fun paidMultiple(lang: Language): String = if (lang == Language.ENGLISH) "Paid by:" else "Πλήρωσαν:"
    fun noMembersExpenseHint(lang: Language): String = if (lang == Language.ENGLISH) "No members registered. The expense will be logged as Common." else "Δεν έχουν καταχωρηθεί μέλη. Το έξοδο θα καταχωρηθεί ως Κοινό."
    fun categoryLabel(lang: Language): String = if (lang == Language.ENGLISH) "Category" else "Κατηγορία"
    fun date(lang: Language): String = if (lang == Language.ENGLISH) "Date" else "Ημερομηνία"
    fun dateLabel(lang: Language): String = if (lang == Language.ENGLISH) "Target Date" else "Προθεσμία"
    fun changeAction(lang: Language): String = if (lang == Language.ENGLISH) "Change" else "Αλλαγή"
    fun noteLabel(lang: Language): String = if (lang == Language.ENGLISH) "Note (optional)" else "Σημείωση (προαιρετικό)"
    fun notePlaceholder(lang: Language): String = if (lang == Language.ENGLISH) "e.g. Paid by card" else "π.χ. Πληρώθηκε με κάρτα"
    fun fillDescriptionError(lang: Language): String = if (lang == Language.ENGLISH) "Please enter a description" else "Παρακαλώ συμπληρώστε περιγραφή"
    fun fillAmountError(lang: Language): String = if (lang == Language.ENGLISH) "Please enter a valid amount" else "Παρακαλώ συμπληρώστε έγκυρο ποσό"
    fun recordIncomeTitle(lang: Language): String = if (lang == Language.ENGLISH) "Record Income" else "Καταγραφή Εσόδου"
    fun editIncomeTitle(lang: Language): String = if (lang == Language.ENGLISH) "Edit Income" else "Επεξεργασία Εσόδου"
    fun incomeDescLabel(lang: Language): String = if (lang == Language.ENGLISH) "Income Description *" else "Περιγραφή Εσόδου *"
    fun incomeDescPlaceholder(lang: Language): String = if (lang == Language.ENGLISH) "e.g. October Salary, Rent" else "π.χ. Μισθός Οκτωβρίου, Ενοίκιο"
    fun incomeAmountLabel(lang: Language): String = if (lang == Language.ENGLISH) "Income Amount (€) *" else "Ποσό Εσόδου (€) *"
    fun memberWhoContributed(lang: Language): String = if (lang == Language.ENGLISH) "MEMBER WHO CONTRIBUTED *" else "ΜΕΛΟΣ ΠΟΥ ΣΥΝΕΙΣΕΦΕΡΕ *"
    fun noMembersIncomeHint(lang: Language): String = if (lang == Language.ENGLISH) "No members registered. The income will be logged as Common." else "Δεν έχουν καταχωρηθεί μέλη. Το έσοδο θα καταχωρηθεί ως Κοινό."
    fun incomeCategoryLabel(lang: Language): String = if (lang == Language.ENGLISH) "INCOME CATEGORY" else "ΚΑΤΗΓΟΡΙΑ ΕΣΟΔΟΥ"
    fun voiceLoggingTitle(lang: Language): String = if (lang == Language.ENGLISH) "Voice Logging" else "Φωνητική Καταγραφή"
    fun speakNowLabel(lang: Language): String = if (lang == Language.ENGLISH) "Speak now..." else "Μιλήστε τώρα..."
    fun listeningStatus(lang: Language): String = if (lang == Language.ENGLISH) "Listening..." else "Ακρόαση..."
    fun speechExamplesText(lang: Language): String = "Say for example:\n• \"Supermarket 45 euros dad\"\n• \"Pharmacy 12 euros mom\"\n• \"Coffee 3.50\""
    fun voiceConfirmationTitle(lang: Language): String = if (lang == Language.ENGLISH) "Confirm Expense" else "Επιβεβαίωση Εξόδου"
    fun voiceDetectedDetails(lang: Language): String = if (lang == Language.ENGLISH) "Detected Details" else "Αναγνωρισμένα Στοιχεία"
    fun tryAgainAction(lang: Language): String = if (lang == Language.ENGLISH) "Try Again" else "Δοκιμάστε Ξανά"
    fun quickActionTitle(lang: Language): String = if (lang == Language.ENGLISH) "Quick Expense Logging" else "Γρήγορη Καταγραφή Εξόδου"
    fun chooseMethodSubtitle(lang: Language): String = if (lang == Language.ENGLISH) "Choose how you want to log your expense" else "Επιλέξτε πώς θέλετε να καταχωρήσετε το έξοδο"
    fun voiceOptionTitle(lang: Language): String = if (lang == Language.ENGLISH) "Voice Recognition" else "Φωνητική Αναγνώριση"
    fun voiceOptionSubtitle(lang: Language): String = if (lang == Language.ENGLISH) "Fast & easy with your voice" else "Γρήγορα & απλά με τη φωνή σας"
    fun manualOptionTitle(lang: Language): String = if (lang == Language.ENGLISH) "Manual Entry" else "Χειροκίνητη Καταχώρηση"
    fun manualOptionSubtitle(lang: Language): String = if (lang == Language.ENGLISH) "Type details in form" else "Πληκτρολόγηση στη φόρμα"
    fun dontShowAgainCheckbox(lang: Language): String = if (lang == Language.ENGLISH) "Don't show again on launch" else "Να μην εμφανίζεται στην εκκίνηση"
    fun cloudSyncTitle(lang: Language): String = if (lang == Language.ENGLISH) "Cloud Sync & Live Sharing" else "Συγχρονισμός Cloud & Κοινή Χρήση"
    fun cloudSyncSubtitle(lang: Language): String = "Collaborate in real-time with family or partners across multiple devices."
    fun householdCodeLabel(lang: Language): String = if (lang == Language.ENGLISH) "Household Code:" else "Κωδικός Εξοδολογίου:"
    fun joinHouseholdTitle(lang: Language): String = if (lang == Language.ENGLISH) "Join Existing Household" else "Σύνδεση σε Υπάρχον Εξοδολόγιο"
    fun enterCodePlaceholder(lang: Language): String = if (lang == Language.ENGLISH) "Enter household code" else "Εισαγωγή κωδικού εξοδολογίου"
    fun joinAction(lang: Language): String = if (lang == Language.ENGLISH) "Join" else "Σύνδεση"
    fun exportReportTitle(lang: Language): String = if (lang == Language.ENGLISH) "Export Report" else "Εξαγωγή Αναφοράς"
    fun exportReportSubtitle(lang: Language): String = if (lang == Language.ENGLISH) "In Excel / CSV format" else "Σε μορφή Excel / CSV"
    fun periodLabel(lang: Language): String = if (lang == Language.ENGLISH) "Period:" else "Περίοδος:"
    fun expenseCountLabel(lang: Language): String = if (lang == Language.ENGLISH) "Expense Count:" else "Πλήθος Εξόδων:"
    fun totalAmountLabel(lang: Language): String = if (lang == Language.ENGLISH) "Total Amount:" else "Συνολικό Ποσό:"
    fun shareReportAction(lang: Language): String = if (lang == Language.ENGLISH) "Share Report (Email / Viber / Drive)" else "Κοινοποίηση (Email / Viber / Drive)"
    fun copyCsvAction(lang: Language): String = if (lang == Language.ENGLISH) "Copy CSV text" else "Αντιγραφή κειμένου CSV"
    fun csvCopiedMessage(lang: Language): String = if (lang == Language.ENGLISH) "CSV report copied to clipboard!" else "Η αναφορά CSV αντιγράφηκε στο πρόχειρο!"
    fun myProfile(lang: Language): String = if (lang == Language.ENGLISH) "My Profile" else "Το Προφίλ μου"
    fun fintelUser(lang: Language): String = if (lang == Language.ENGLISH) "Fintel User" else "Χρήστης Fintel"
    fun freePlan(lang: Language): String = if (lang == Language.ENGLISH) "Free Plan" else "Δωρεάν Πλάνο"
    fun proSubscriber(lang: Language): String = if (lang == Language.ENGLISH) "Pro Subscriber" else "Συνδρομητής Pro"
    fun upgradeToPro(lang: Language): String = if (lang == Language.ENGLISH) "Upgrade to Pro" else "Αναβάθμιση σε Pro"
    fun transactionsLabel(lang: Language): String = if (lang == Language.ENGLISH) "Transactions" else "Συναλλαγές"
    fun totalExpensesLabel(lang: Language): String = if (lang == Language.ENGLISH) "Total Expenses" else "Σύνολο Εξόδων"
    fun signOutAccount(lang: Language): String = if (lang == Language.ENGLISH) "Sign Out Account" else "Αποσύνδεση Λογαριασμού"
    fun signOut(lang: Language): String = if (lang == Language.ENGLISH) "Sign Out" else "Αποσύνδεση"
    fun myExodologio(lang: Language): String = if (lang == Language.ENGLISH) "My Household Book" else "Το Εξοδολόγιό μου"
    fun activeExodologioHeader(lang: Language): String = if (lang == Language.ENGLISH) "ACTIVE HOUSEHOLD" else "ΕΝΕΡΓΟ ΕΞΟΔΟΛΟΓΙΟ"
    fun exodologioMembers(lang: Language): String = if (lang == Language.ENGLISH) "Household Members" else "Μέλη Εξοδολογίου"
    fun createNewExodologio(lang: Language): String = if (lang == Language.ENGLISH) "Create New Household" else "Δημιουργία Νέου Εξοδολογίου"
    fun separateExodologio(lang: Language): String = if (lang == Language.ENGLISH) "Separate household" else "Ξεχωριστό εξοδολόγιο"
    fun clearAllExpenses(lang: Language): String = if (lang == Language.ENGLISH) "Clear all expenses" else "Διαγραφή όλων των εξόδων"
    fun clearAllExpensesConfirmTitle(lang: Language): String = if (lang == Language.ENGLISH) "Delete All Expenses?" else "Διαγραφή Όλων των Εξόδων"
    fun clearAllExpensesConfirmMsg(lang: Language): String = "Are you sure you want to delete all expenses in this household? This cannot be undone."
    fun menuTitle(lang: Language): String = if (lang == Language.ENGLISH) "Menu Options" else "Μενού Επιλογών"
    fun language(lang: Language): String = if (lang == Language.ENGLISH) "Language" else "Γλώσσα"
    fun languageSection(lang: Language): String = if (lang == Language.ENGLISH) "APP LANGUAGE" else "ΓΛΩΣΣΑ ΕΦΑΡΜΟΓΗΣ"
    fun appSettings(lang: Language): String = if (lang == Language.ENGLISH) "App Settings" else "Ρυθμίσεις Εφαρμογής"
    fun appSettingsSubtitle(lang: Language): String = if (lang == Language.ENGLISH) "Theme, language & preferences" else "Θέμα εμφάνισης, γλώσσα & προτιμήσεις"
    fun householdManager(lang: Language): String = if (lang == Language.ENGLISH) "Manage Households" else "Διαχείριση Εξοδολογίων"
    fun themeDisplay(lang: Language): String = if (lang == Language.ENGLISH) "DISPLAY THEME" else "ΘΕΜΑ ΕΜΦΑΝΙΣΗΣ"
    fun themeLight(lang: Language): String = if (lang == Language.ENGLISH) "Light" else "Φωτεινό"
    fun themeDark(lang: Language): String = if (lang == Language.ENGLISH) "Dark" else "Σκοτεινό"
    fun themeSystem(lang: Language): String = if (lang == Language.ENGLISH) "System" else "Σύστημα"
    fun visualDesign(lang: Language): String = if (lang == Language.ENGLISH) "VISUAL DESIGN & FONTS" else "ΣΧΕΔΙΑΣΜΟΣ & ΓΡΑΜΜΑΤΟΣΕΙΡΕΣ"
    fun styleModernFintech(lang: Language): String = if (lang == Language.ENGLISH) "Modern FinTech" else "Μοντέρνο FinTech"
    fun styleModernFintechDesc(lang: Language): String = if (lang == Language.ENGLISH) "New rounded geometric cards, donut chart & Plus Jakarta Sans" else "Νέες στρογγυλεμένες κάρτες, Plus Jakarta Sans & γραφιστικά"
    fun styleClassic(lang: Language): String = if (lang == Language.ENGLISH) "Classic" else "Κλασικό"
    fun styleClassicDesc(lang: Language): String = if (lang == Language.ENGLISH) "Original clean Material 3 look" else "Αρχικό κλασικό στυλ Material 3"
    fun preferencesSection(lang: Language): String = if (lang == Language.ENGLISH) "PREFERENCES" else "ΠΡΟΤΙΜΗΣΕΙΣ"
    fun quickActionLaunch(lang: Language): String = if (lang == Language.ENGLISH) "Quick Action on Launch" else "Γρήγορη επιλογή κατά την έναρξη"
    fun quickActionLaunchSubtitle(lang: Language): String = if (lang == Language.ENGLISH) "Show Voice / Typing prompt dialog" else "Εμφάνιση μενού φωνής/πληκτρολόγησης"
    fun dataManagement(lang: Language): String = if (lang == Language.ENGLISH) "DATA MANAGEMENT" else "ΔΙΑΧΕΙΡΙΣΗ ΔΕΔΟΜΕΝΩΝ"
    fun clearAllData(lang: Language): String = if (lang == Language.ENGLISH) "Clear all local data" else "Εκκαθάριση όλων των δεδομένων"
    fun clearAllDataSubtitle(lang: Language): String = if (lang == Language.ENGLISH) "Delete local records and reset app" else "Διαγραφή τοπικών εξόδων & επαναφορά"
    fun resetAction(lang: Language): String = if (lang == Language.ENGLISH) "Reset" else "Επαναφορά"
    fun analyticsStatisticalAnalysis(lang: Language): String = if (lang == Language.ENGLISH) "Statistical Analysis" else "Στατιστική Ανάλυση"
    fun familyMonthlyBalance(lang: Language): String = if (lang == Language.ENGLISH) "Monthly balance" else "Μηνιαίος Ισολογισμός"
    fun exportToExcel(lang: Language): String = if (lang == Language.ENGLISH) "Export to Excel" else "Εξαγωγή σε Excel"
    fun balanceWord(lang: Language): String = if (lang == Language.ENGLISH) "Balance" else "Ισοζύγιο"
    fun netBalanceTitle(lang: Language): String = if (lang == Language.ENGLISH) "Balance" else "Υπόλοιπο"
    fun familyIncome(lang: Language): String = if (lang == Language.ENGLISH) "Total Incomes" else "Συνολικά Έσοδα"
    fun contributionByMember(lang: Language): String = if (lang == Language.ENGLISH) "Contribution by member & sources" else "Συνεισφορά ανά μέλος & πηγές"
    fun noIncomesYet(lang: Language): String = if (lang == Language.ENGLISH) "No income registered yet" else "Δεν έχουν καταχωρηθεί ακόμα έσοδα"
    fun registerIncomeHint(lang: Language): String = if (lang == Language.ENGLISH) "Register salaries, rents, allowances, etc. from the home screen with «+ Income»." else "Καταχωρήστε μισθούς, ενοίκια, επιδοτήσεις κ.α. από την αρχική οθόνη με το κουμπί «+ Έσοδο»."
    fun commonOther(lang: Language): String = if (lang == Language.ENGLISH) "Common / Other" else "Κοινό / Άλλο"
    fun commonOtherIncome(lang: Language): String = if (lang == Language.ENGLISH) "Common / Other Income" else "Κοινό / Λοιπά Έσοδα"
    fun incomeSourcesByCategory(lang: Language): String = if (lang == Language.ENGLISH) "Income Sources by Category" else "Πηγές Εσόδων ανά Κατηγορία"
    fun expenseDistribution(lang: Language): String = if (lang == Language.ENGLISH) "Expense Distribution" else "Κατανομή Εξόδων"
    fun parents(lang: Language): String = if (lang == Language.ENGLISH) "Parents" else "Γονείς"
    fun children(lang: Language): String = if (lang == Language.ENGLISH) "Children" else "Παιδιά"
    fun breakdownByMember(lang: Language): String = if (lang == Language.ENGLISH) "Breakdown by Member" else "Καταμερισμός ανά Μέλος"
    fun individualSpendingSubtitle(lang: Language): String = if (lang == Language.ENGLISH) "Personal expenses per family member" else "Προσωπικά έξοδα ανά μέλος της οικογένειας"
    fun parentsSpendingSubtitle(lang: Language): String = if (lang == Language.ENGLISH) "Parents expenses & shared household coverage" else "Έξοδα γονέων & κάλυψη κοινών εξόδων σπιτιού"
    fun kidsSpendingSubtitle(lang: Language): String = if (lang == Language.ENGLISH) "Expenses & activities for children" else "Έξοδα & δραστηριότητες για τα παιδιά"
    fun individualMode(lang: Language): String = if (lang == Language.ENGLISH) "Individual" else "Ατομικά"
    fun parentsMode(lang: Language): String = if (lang == Language.ENGLISH) "Parents" else "Γονείς"
    fun kidsMode(lang: Language): String = if (lang == Language.ENGLISH) "Children" else "Παιδιά"
    fun budgetMonitoring(lang: Language): String = if (lang == Language.ENGLISH) "Budget Monitoring" else "Έλεγχος Budgets"
    fun exceededBudget(lang: Language): String = if (lang == Language.ENGLISH) "Exceeded" else "Υπέρβαση"
    fun warningBudget(lang: Language): String = if (lang == Language.ENGLISH) "Warning" else "Προσοχή"
    fun atBudget(lang: Language): String = if (lang == Language.ENGLISH) "At budget limit (100%)" else "Στο όριο (100%)"
    fun withinLimit(lang: Language): String = if (lang == Language.ENGLISH) "Within limit" else "Εντός ορίου"
    fun memberIncomeDeficitTitle(lang: Language): String = if (lang == Language.ENGLISH) "Expenses Exceeded Declared Income" else "Υπέρβαση Εσόδων Μέλους"
    fun memberIncomeDeficitDesc(lang: Language): String = if (lang == Language.ENGLISH) "Member has paid more in expenses than their declared income for this period." else "Το μέλος έχει πληρώσει περισσότερα έξοδα από όσα έχει δηλώσει ως έσοδα για το επιλεγμένο διάστημα."
    fun deficitLabel(lang: Language): String = if (lang == Language.ENGLISH) "Deficit" else "Έλλειμμα"
    fun expensesExceedIncomeTag(lang: Language): String = if (lang == Language.ENGLISH) "Expenses > Income" else "Έξοδα > Έσοδα"
    fun barChartView(lang: Language): String = if (lang == Language.ENGLISH) "Bar Chart View" else "Προβολή Μπάρας"
    fun pieChartView(lang: Language): String = if (lang == Language.ENGLISH) "Pie Chart View" else "Προβολή Πίτας"
    fun noExpensesRegistered(lang: Language): String = if (lang == Language.ENGLISH) "No expenses registered" else "Δεν υπάρχουν καταχωρημένα έξοδα"
    fun total(lang: Language): String = if (lang == Language.ENGLISH) "Total" else "Σύνολο"
    fun noKidsRegisteredHint(lang: Language): String = if (lang == Language.ENGLISH) "No members with role «Child» registered." else "Δεν έχουν καταχωρηθεί μέλη με ρόλο «Παιδί»."
    fun noMembersInCategoryHint(lang: Language): String = if (lang == Language.ENGLISH) "No members in this category." else "Δεν υπάρχουν μέλη στη συγκεκριμένη κατηγορία."
    fun kidsExpensesSubtitle(lang: Language): String = if (lang == Language.ENGLISH) "Child • Expenses & activities" else "Παιδί • Έξοδα & δραστηριότητες"
    fun incomes(lang: Language): String = if (lang == Language.ENGLISH) "Income" else "Έσοδα"
    fun loadingExpenses(lang: Language): String = if (lang == Language.ENGLISH) "Loading expenses..." else "Φόρτωση εξόδων..."
    fun dailyAverageTitle(lang: Language): String = if (lang == Language.ENGLISH) "Daily Average" else "Μέσο Ημερήσιο"
    fun dailyAverageUnit(lang: Language): String = if (lang == Language.ENGLISH) "/ day" else "/ ημέρα"
    fun savingsRatioTitle(lang: Language): String = if (lang == Language.ENGLISH) "Savings Rate" else "Δείκτης Αποταμίευσης"
    fun peakDayTitle(lang: Language): String = if (lang == Language.ENGLISH) "Peak Spending Day" else "Υψηλότερη Ημέρα"
    fun noPeakDay(lang: Language): String = if (lang == Language.ENGLISH) "No expenses" else "Χωρίς έξοδα"
    fun fixedObligationsTitle(lang: Language): String = if (lang == Language.ENGLISH) "Standing Orders" else "Πάγιες Υποχρεώσεις"
    fun deleteAccountTitle(lang: Language): String = if (lang == Language.ENGLISH) "Delete Account" else "Διαγραφή Λογαριασμού"
    fun deleteAccountButton(lang: Language): String = if (lang == Language.ENGLISH) "Delete Account & Data" else "Διαγραφή Λογαριασμού & Δεδομένων"
    fun deleteAccountConfirmTitle(lang: Language): String = if (lang == Language.ENGLISH) "Permanent Account Deletion?" else "Οριστική Διαγραφή Λογαριασμού"
    fun deleteAccountConfirmMsg(lang: Language): String = "Are you sure you want to permanently delete your account and all associated financial data from the cloud? This action cannot be undone."
    fun privacyAndLegal(lang: Language): String = if (lang == Language.ENGLISH) "Privacy Policy & Terms" else "Πολιτική Απορρήτου & Όροι"
    fun accountDeletedSuccess(lang: Language): String = if (lang == Language.ENGLISH) "Your account and data were permanently deleted." else "Ο λογαριασμός και τα δεδομένα σας διαγράφηκαν οριστικά."
    fun inviteViaEmailTitle(lang: Language): String = if (lang == Language.ENGLISH) "Invite Member (Email & Link)" else "Πρόσκληση Μέλους (Email & Link)"
    fun partnerEmailLabel(lang: Language): String = if (lang == Language.ENGLISH) "Partner / Roommate Email" else "Email Συνεργάτη / Συγκατοίκου"
    fun sendInviteAction(lang: Language): String = if (lang == Language.ENGLISH) "Send Email Invitation" else "Αποστολή Πρόσκλησης με Email"
    fun copyInviteLinkAction(lang: Language): String = if (lang == Language.ENGLISH) "Copy Invite Link" else "Αντιγραφή Συνδέσμου"
    fun inviteCopiedMsg(lang: Language): String = if (lang == Language.ENGLISH) "Invite link copied to clipboard!" else "Ο σύνδεσμος πρόσκλησης αντιγράφηκε!"

    fun translateCategory(name: String, lang: Language): String {
        if (lang != Language.ENGLISH) return name
        return when (name.trim().lowercase()) {
            "σουπερ μαρκετ", "σούπερ μάρκετ", "supermarket" -> "Supermarket"
            "διατροφή", "φαγητό", "food" -> "Food"
            "λογαριασμοί", "bills" -> "Bills"
            "ενοίκιο", "rent" -> "Rent"
            "μετακίνηση", "transport", "βενζίνη" -> "Transportation"
            "υγεία", "health", "φάρμακα" -> "Health & Care"
            "διασκέδαση", "entertainment" -> "Entertainment"
            "παιδιά", "kids" -> "Children"
            "σπίτι", "home" -> "Home & Maintenance"
            "ρούχα", "ένδυση", "clothing" -> "Clothing"
            "εκπαίδευση", "education" -> "Education"
            "μισθός", "salary" -> "Salary"
            "άλλο", "λοιπά", "other" -> "Other"
            else -> name
        }
    }
    fun translateCategory(lang: Language, name: String): String = translateCategory(name, lang)

    fun translateRole(role: String, lang: Language): String {
        if (lang != Language.ENGLISH) return role
        return when (role.trim().lowercase()) {
            "γονέας", "γονεις", "parent" -> "Parent"
            "παιδί", "παιδια", "child" -> "Child"
            "συγκάτοικος", "roommate" -> "Roommate"
            "άλλο", "other" -> "Other"
            else -> role
        }
    }
    fun translateRole(lang: Language, role: String): String = translateRole(role, lang)

    fun pluralRoleName(role: String, lang: Language): String {
        val r = role.trim().lowercase()
        return if (lang == Language.ENGLISH) {
            when {
                r.contains("γον") || r.contains("parent") -> "Parents"
                r.contains("παιδ") || r.contains("child") || r.contains("kid") -> "Children"
                r.contains("συγκ") || r.contains("room") -> "Roommates"
                else -> role
            }
        } else {
            when {
                r.contains("γον") || r.contains("parent") -> "Γονείς"
                r.contains("παιδ") || r.contains("child") || r.contains("kid") -> "Παιδιά"
                r.contains("συγκ") || r.contains("room") -> "Συγκάτοικοι"
                else -> role
            }
        }
    }
    fun pluralRoleName(lang: Language, role: String): String = pluralRoleName(role, lang)

    fun translateHouseholdName(name: String, lang: Language): String {
        if (lang != Language.ENGLISH) return name
        return when {
            name.contains("Το Εξοδολόγιό μου", ignoreCase = true) -> "My Household"
            name.contains("Οικογένεια", ignoreCase = true) -> name.replace("Οικογένεια", "Family")
            name.contains("Επιχείρηση", ignoreCase = true) -> "Business"
            else -> name
        }
    }
    fun translateHouseholdName(lang: Language, name: String): String = translateHouseholdName(name, lang)

    fun categoriesExceededLimit(lang: Language, count: Int): String = if (lang == Language.ENGLISH) "$count categories exceeded budget!" else "$count κατηγορίες ξεπέρασαν το όριο!"
    fun categoriesExceededLimit(count: Int, lang: Language): String = categoriesExceededLimit(lang, count)

    fun categoriesApproachingLimit(lang: Language, count: Int): String = if (lang == Language.ENGLISH) "$count categories approaching budget limit" else "$count κατηγορίες πλησιάζουν το όριο"
    fun categoriesApproachingLimit(count: Int, lang: Language): String = categoriesApproachingLimit(lang, count)

    fun activeMembersCount(lang: Language, count: Int): String = if (lang == Language.ENGLISH) "$count active members" else "$count ενεργά μέλη"
    fun activeMembersCount(count: Int, lang: Language): String = activeMembersCount(lang, count)

    fun activeHouseholdCount(lang: Language, count: Int): String = if (lang == Language.ENGLISH) "$count household" else "$count εξοδολόγιο"
    fun activeHouseholdCount(count: Int, lang: Language): String = activeHouseholdCount(lang, count)

    fun payerRemaining(lang: Language, formattedAmount: String): String = if (lang == Language.ENGLISH) "Remaining to assign: $formattedAmount" else "Απομένουν προς κατανομή: $formattedAmount"
    fun payerRemaining(formattedAmount: String, lang: Language): String = payerRemaining(lang, formattedAmount)

    fun payerQuickBalance(lang: Language, formattedAmount: String): String = if (lang == Language.ENGLISH) "Balance: $formattedAmount" else "Υπόλοιπο: $formattedAmount"
    fun payerQuickBalance(formattedAmount: String, lang: Language): String = payerQuickBalance(lang, formattedAmount)
    fun payerQuickBalance(lang: Language, memberName: String, formattedAmount: String): String = if (lang == Language.ENGLISH) "+$formattedAmount to $memberName" else "+$formattedAmount στον/στην $memberName"

    fun payerExceeded(lang: Language, formattedAmount: String): String = if (lang == Language.ENGLISH) "Exceeded by: $formattedAmount" else "Υπέρβαση κατά: $formattedAmount"
    fun payerExceeded(formattedAmount: String, lang: Language): String = payerExceeded(lang, formattedAmount)

    fun entriesCount(count: Int, lang: Language): String = if (lang == Language.ENGLISH) "$count entries" else "$count εγγραφές"
    fun entriesCount(lang: Language, count: Int): String = entriesCount(count, lang)

    fun transactionsCount(count: Int, lang: Language): String = if (lang == Language.ENGLISH) "$count transactions" else "$count συναλλαγές"
    fun transactionsCount(lang: Language, count: Int): String = transactionsCount(count, lang)

    fun savingsRateLabel(rate: Int, lang: Language): String = if (lang == Language.ENGLISH) "Savings Rate: $rate%" else "Αποταμίευση: $rate%"
    fun savingsRateLabel(lang: Language, rate: Int): String = savingsRateLabel(rate, lang)

    fun percentOfTotalIncome(percent: Int, lang: Language): String = if (lang == Language.ENGLISH) "$percent% of total income" else "$percent% του συνολικού εισοδήματος"
    fun percentOfTotalIncome(percent: Double, lang: Language): String = percentOfTotalIncome(percent.toInt(), lang)
    fun percentOfTotalIncome(lang: Language, percent: Int): String = percentOfTotalIncome(percent, lang)

    fun unassignedMemberIncome(percent: Int, lang: Language): String = if (lang == Language.ENGLISH) "$percent% unassigned" else "$percent% μη κατανεμημένο"
    fun unassignedMemberIncome(lang: Language, percent: Int): String = unassignedMemberIncome(percent, lang)

    fun categoriesCount(count: Int, lang: Language): String = if (lang == Language.ENGLISH) "$count categories" else "$count κατηγορίες"
    fun categoriesCount(lang: Language, count: Int): String = categoriesCount(count, lang)

    fun everyDayOfMonth(lang: Language, day: Int): String = if (lang == Language.ENGLISH) "Every $day of month" else "Κάθε $day του μήνα"
    fun everyDayOfMonth(day: Int, lang: Language): String = everyDayOfMonth(lang, day)
}
