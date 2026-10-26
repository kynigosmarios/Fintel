package com.example.data.seed

import com.example.data.entity.Expense
import com.example.data.entity.ExpenseCategory
import com.example.data.entity.FamilyMember
import com.example.data.entity.Income

object FirestoreDataSeed {
    const val DEFAULT_HOUSEHOLD_ID = "3e9a25a0-2e7"
    const val DEFAULT_HOUSEHOLD_NAME = "Οικογένεια Μάριος"
    const val DEFAULT_USER_EMAIL = "kynigosmarios@gmail.com"
    const val DEFAULT_USER_NAME = "Μάριος Κυνηγός"
    const val DEFAULT_USER_UID = "t5kN4RvPRugIxXUz18Fep2khSrC2"

    val members: List<FamilyMember> = listOf(
        FamilyMember(id = 1L, name = "Μάριος", role = "Γονέας", avatarColorHex = 4278556265L, voiceKeywords = "μάριος", isDefault = true, sortOrder = 0),
        FamilyMember(id = 1513475L, name = "Βασιλική", role = "Γονέας", avatarColorHex = 4287323382L, voiceKeywords = "Βασιλική, βασιλική", isDefault = false, sortOrder = 1),
        FamilyMember(id = 1692L, name = "Μελίνα", role = "Παιδί", avatarColorHex = 4294286859L, voiceKeywords = "μελίνα", isDefault = false, sortOrder = 2),
        FamilyMember(id = 1695L, name = "Γεωργία", role = "Παιδί", avatarColorHex = 4293675161L, voiceKeywords = "γεωργία", isDefault = false, sortOrder = 3),
    )

    val categories: List<ExpenseCategory> = listOf(
        ExpenseCategory(id = 1L, name = "Σούπερ μάρκετ", iconKey = "cart", monthlyBudget = 450.0, colorHex = 4278556265L, voiceKeywords = "σούπερ μάρκετ, σουπερμάρκετ, σουπερ μαρκετ, σκλαβενίτης, σκλαβενιτης, βασιλόπουλος, βασιλοπουλος, lidl, λιντλ, μασούτης, μάρκετ, τροφιμα, τρόφιμα, ψώνια, μανάβικο, κρεοπωλείο, ιχθυοπωλείο, μαναβης", sortOrder = 0, fontColorHex = 0L),
        ExpenseCategory(id = 10L, name = "Αέριο", iconKey = "gas", monthlyBudget = 70.0, colorHex = 4294538006L, voiceKeywords = "αέριο, αεριο, φυσικό αέριο, φυσικο αεριο, θέρμανση, θερμανση, λέβητας, λεβητας, efa, ζενίθ, ΔΕΗ αεριο", sortOrder = 1, fontColorHex = 0L),
        ExpenseCategory(id = 11L, name = "Κοινόχρηστα", iconKey = "apartment", monthlyBudget = 40.0, colorHex = 4284704497L, voiceKeywords = "κοινόχρηστα, κοινοχρηστα, κοινόχρηστα πολυκατοικίας", sortOrder = 2, fontColorHex = 0L),
        ExpenseCategory(id = 12L, name = "Δάνεια", iconKey = "loan", monthlyBudget = 200.0, colorHex = 4282865001L, voiceKeywords = "δάνεια, δανεια, δάνειο, δανειο, τράπεζα, τραπεζα, πιστωτική, πιστωτικη, eurobank, alpha bank, εθνική, πειραιώς, κάρτα δόση", sortOrder = 3, fontColorHex = 0L),
        ExpenseCategory(id = 13L, name = "Δόση αυτοκινήτου", iconKey = "car_payment", monthlyBudget = 332.0, colorHex = 4281549141L, voiceKeywords = "δόση αυτοκινήτου, σίνης", sortOrder = 4, fontColorHex = 0L),
        ExpenseCategory(id = 14L, name = "Έξοδα γατιών", iconKey = "pets", monthlyBudget = 50.0, colorHex = 4279548070L, voiceKeywords = "τροφή γατιών, τροφή γάτες, άμμος, άμμος γάτες, άμμος γατιών, άμμος για τα γατιά, τροφή για τα γατιά, άδης, ήρα", sortOrder = 5, fontColorHex = 0L),
        ExpenseCategory(id = 15L, name = "Χρωστούμενα", iconKey = "debt", monthlyBudget = 200.0, colorHex = 4287837162L, voiceKeywords = "χρωστούμενα, χρωστάω, δανεικά, επιστροφή χρημάτων, εξόφληση, οφειλή, χρέος", sortOrder = 6, fontColorHex = 0L),
        ExpenseCategory(id = 1512450L, name = "Έξοδα παιδιών", iconKey = "other", monthlyBudget = 120.0, colorHex = 4278556265L, voiceKeywords = "πάνες, μωρομάντηλα, ρούχα Γεωργία, ρούχα Μελίνα, ρούχα παιδιά", sortOrder = 7, fontColorHex = 0L),
        ExpenseCategory(id = 1513469L, name = "Ψιλικά", iconKey = "kiosk", monthlyBudget = 50.0, colorHex = 4278355143L, voiceKeywords = "ψιλικά, ψιλικα, περίπτερο, περιπτερο, μίνι μάρκετ, μινι μαρκετ, τσιγάρα, τσιγαρα, καπνός, καπνος, εφημερίδα, αναψυκτικά, κρουασάν, παγωτά, πατατάκια, ψιλικατζίδικο, ψιλικατζιδικο, τσίχλες", sortOrder = 8, fontColorHex = 0L),
        ExpenseCategory(id = 1513472L, name = "Δραστηριότητες", iconKey = "sports", monthlyBudget = 150.0, colorHex = 4287323382L, voiceKeywords = "Κολυμβητήριο, γυμναστήριο, messy play, μουσική", sortOrder = 9, fontColorHex = 0L),
        ExpenseCategory(id = 1513477L, name = "Καφέδες", iconKey = "coffee", monthlyBudget = 50.0, colorHex = 4278556265L, voiceKeywords = "καφές, καφέδες, καφεδάκια, for all, yes", sortOrder = 10, fontColorHex = 0L),
        ExpenseCategory(id = 17L, name = "Δώρα", iconKey = "gift", monthlyBudget = 50.0, colorHex = 4292429551L, voiceKeywords = "δώρα, δώρο, γενέθλια, γιορτή, παιχνίδια, παιχνιδι, βαφτίσια, γάμος, δωράκι", sortOrder = 11, fontColorHex = 0L),
        ExpenseCategory(id = 19L, name = "Στοίχημα", iconKey = "other", monthlyBudget = 50.0, colorHex = 4278556265L, voiceKeywords = "Στοίχημα, superbet, novibet, bet365", sortOrder = 12, fontColorHex = 0L),
        ExpenseCategory(id = 2L, name = "Φαγητό από έξω", iconKey = "food", monthlyBudget = 100.0, colorHex = 4278556265L, voiceKeywords = "φαγητό έξω, φαγητό, σουβλάκια, πίτσα, πίτσες, γύρος, σάντουιτς, κινέζικο, πρωινό, μπράντς, γλυκά, παγωτό", sortOrder = 13, fontColorHex = 0L),
        ExpenseCategory(id = 20L, name = "Έξοδα σπιτιού", iconKey = "house", monthlyBudget = 80.0, colorHex = 4278556265L, voiceKeywords = "Έπιπλα, υδραυλικά, μπογιές, επισκευή σπίτι", sortOrder = 14, fontColorHex = 0L),
        ExpenseCategory(id = 21L, name = "Έξοδα αυτοκινήτου", iconKey = "car_payment", monthlyBudget = 50.0, colorHex = 4278556265L, voiceKeywords = "Αυτοκίνητο, service, ασφάλεια αυτοκινήτου", sortOrder = 15, fontColorHex = 0L),
        ExpenseCategory(id = 22L, name = "Συνδρομές", iconKey = "entertainment", monthlyBudget = 40.0, colorHex = 4278556265L, voiceKeywords = "Netflix, Apple tv, Google, Συνδρομή", sortOrder = 16, fontColorHex = 0L),
        ExpenseCategory(id = 23L, name = "Τηλέφωνα / Internet", iconKey = "cart", monthlyBudget = 80.0, colorHex = 4278556265L, voiceKeywords = "Τηλέφωνο, Κινητό, Internet, Nova, Inalan", sortOrder = 17, fontColorHex = 0L),
        ExpenseCategory(id = 3L, name = "Νερό", iconKey = "house", monthlyBudget = 30.0, colorHex = 4279080072L, voiceKeywords = "νερό, λογαριασμός νερού, ΕΥΑΘ", sortOrder = 18, fontColorHex = 0L),
        ExpenseCategory(id = 4L, name = "Μετακινήσεις & Καύσιμα", iconKey = "gas_station", monthlyBudget = 120.0, colorHex = 4293548044L, voiceKeywords = "βενζίνη, καύσιμα, βενζινάδικο, διόδια, πάρκινγκ, λεωφορείο, μετρό, ταξί, εισιτήρια, αεροπλάνο", sortOrder = 19, fontColorHex = 0L),
        ExpenseCategory(id = 5L, name = "Γιατρός & Φαρμακείο", iconKey = "medical", monthlyBudget = 120.0, colorHex = 4293675161L, voiceKeywords = "γιατρός, φαρμακείο, φάρμακα, εξετάσεις, οδοντίατρος, υγεία, παιδίατρος, αλλεργιολόγος", sortOrder = 20, fontColorHex = 0L),
        ExpenseCategory(id = 6L, name = "Διασκέδαση & Ψυχαγωγία", iconKey = "entertainment", monthlyBudget = 80.0, colorHex = 4287323382L, voiceKeywords = "διασκέδαση, σινεμά, θέατρο, ποτό, συναυλία, έξοδος, μπύρες, μπύρα, ποτά", sortOrder = 21, fontColorHex = 0L),
        ExpenseCategory(id = 8L, name = "Ενοίκιο", iconKey = "house", monthlyBudget = 370.0, colorHex = 4279080072L, voiceKeywords = "ενοίκιο, σπίτι ενοίκιο, νοίκ", sortOrder = 22, fontColorHex = 0L),
        ExpenseCategory(id = 9L, name = "Ρεύμα", iconKey = "electric", monthlyBudget = 80.0, colorHex = 4294286859L, voiceKeywords = "ρεύμα, ρευμα, δεη, dehi, ήρωνας, ηρωνας, protergia, προτέργια, elpedison, ελπεντισον, watt volt, λογαριασμός ρεύματος, ζενίθ ρεύματος, ρεύματος", sortOrder = 23, fontColorHex = 0L),
    )

    val incomes: List<Income> = emptyList()

    val expenses: List<Expense> = emptyList()
}
