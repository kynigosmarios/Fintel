package com.example.util

import com.example.data.entity.ExpenseCategory
import com.example.data.entity.FamilyMember
import java.util.Locale

data class ParsedVoiceExpense(
    val title: String,
    val amount: Double,
    val categoryId: Long,
    val categoryName: String,
    val splitType: String, // "ALL_FAMILY" or "SPECIFIC_MEMBERS"
    val memberIds: List<Long>,
    val memberNamesDisplay: String,
    val splitAmountPerPerson: Double,
    val originalPrompt: String
)

object GreekVoiceParser {

    fun containsWord(text: String, word: String): Boolean {
        if (word.isBlank() || text.isBlank()) return false
        val pattern = "(?:^|[^\\p{L}\\p{Nd}])" + Regex.escape(word) + "(?:$|[^\\p{L}\\p{Nd}])"
        return Regex(pattern).containsMatchIn(text)
    }

    fun normalizeGreek(text: String): String {
        var res = text.lowercase(Locale.ROOT)
            .replace('ά', 'α')
            .replace('έ', 'ε')
            .replace('ή', 'η')
            .replace('ί', 'ι')
            .replace('ΐ', 'ι')
            .replace('ϊ', 'ι')
            .replace('ό', 'ο')
            .replace('ύ', 'υ')
            .replace('ΰ', 'υ')
            .replace('ϋ', 'υ')
            .replace('ώ', 'ω')
            .replace("€", " ευρω ")
            .replace(",", ".")

        // Replace common word numbers
        val numberWords = listOf(
            "μισο ευρω" to "0.5 ευρω",
            "μισο" to "0.5",
            "εναμιση" to "1.5",
            "δυομιση" to "2.5",
            "τρεις" to "3",
            "τρια" to "3",
            "τεσσερα" to "4",
            "τεσσερις" to "4",
            "πεντε" to "5",
            "εξι" to "6",
            "επτα" to "7",
            "εφτα" to "7",
            "οκτω" to "8",
            "οχτω" to "8",
            "εννια" to "9",
            "εννεα" to "9",
            "δεκα" to "10",
            "εντεκα" to "11",
            "δωδεκα" to "12",
            "δεκαπεντε" to "15",
            "εικοσι" to "20",
            "εικοσιπεντε" to "25",
            "τριαντα" to "30",
            "σαραντα" to "40",
            "πενηντα" to "50",
            "εξηντα" to "60",
            "εβδομηντα" to "70",
            "ογδοντα" to "80",
            "ενενηντα" to "90",
            "εκατο" to "100",
            "εκατον" to "100",
            "διακοσια" to "200",
            "τριακοσια" to "300"
        )

        for ((word, num) in numberWords) {
            val pattern = "(^|[^\\p{L}\\p{Nd}])" + Regex.escape(word) + "($|[^\\p{L}\\p{Nd}])"
            res = res.replace(Regex(pattern), "$1$num$2")
        }
        return res
    }

    fun parse(
        prompt: String,
        categories: List<ExpenseCategory>,
        allMembers: List<FamilyMember>
    ): ParsedVoiceExpense {
        val norm = normalizeGreek(prompt)

        // 1. Amount Extraction
        val amount = extractAmount(norm) ?: 10.0

        // 2. Member Extraction
        val (splitType, matchedMembers, memberDisplay) = extractMembers(norm, allMembers)

        // 3. Category & Title Extraction
        val (category, title) = extractCategoryAndTitle(norm, prompt, categories)

        val splitPerPerson = if (matchedMembers.isNotEmpty()) {
            amount / matchedMembers.size
        } else {
            amount
        }

        return ParsedVoiceExpense(
            title = title,
            amount = amount,
            categoryId = category.id,
            categoryName = category.name,
            splitType = splitType,
            memberIds = matchedMembers.map { it.id },
            memberNamesDisplay = memberDisplay,
            splitAmountPerPerson = splitPerPerson,
            originalPrompt = prompt
        )
    }

    private fun extractAmount(norm: String): Double? {
        // Check "X ευρώ και Y λεπτά" or "X και Y" (e.g. "4 ευρώ και 70 λεπτά", "12 και 30", "18 και 50")
        val andCentsRegex = Regex("(\\d+)\\s*(?:ευρω|ευρα|€)?\\s*(?:και|κι)\\s*(\\d{1,2})\\s*(?:λεπτα|ευρω)?")
        val andMatch = andCentsRegex.find(norm)
        if (andMatch != null) {
            val euros = andMatch.groupValues[1].toDoubleOrNull() ?: 0.0
            val centsStr = andMatch.groupValues[2]
            val cents = if (centsStr.length == 1) centsStr.toDouble() * 10 else centsStr.toDouble()
            return euros + (cents / 100.0)
        }

        // Check "X και μισο"
        val halfRegex = Regex("(\\d+)\\s*(?:και|κι)\\s*0\\.5")
        val halfMatch = halfRegex.find(norm)
        if (halfMatch != null) {
            val base = halfMatch.groupValues[1].toDoubleOrNull() ?: 0.0
            return base + 0.5
        }

        // Check standard "X.Y ευρω" or "X ευρω" or just standalone numbers
        val standardRegex = Regex("(\\d+(?:\\.\\d+)?)\\s*(?:ευρω|ευρα)?")
        val matches = standardRegex.findAll(norm).toList()
        for (m in matches) {
            val num = m.groupValues[1].toDoubleOrNull()
            if (num != null && num > 0.0) {
                return num
            }
        }

        return null
    }

    private fun extractMembers(
        norm: String,
        allMembers: List<FamilyMember>
    ): Triple<String, List<FamilyMember>, String> {
        // Group keywords
        val allFamilyKeywords = listOf(
            "οικογενεια", "ολοι", "σπιτι", "κοινο", "σπιτιου", "ολο το σπιτι", "παντες", "κοινα"
        )
        for (kw in allFamilyKeywords) {
            if (containsWord(norm, kw)) {
                return Triple("ALL_FAMILY", allMembers, "Κοινό Οικογένειας")
            }
        }

        val kidsKeywords = listOf("παιδια", "παιδιων", "στα παιδια", "για τα παιδια", "τα παιδια")
        for (kw in kidsKeywords) {
            if (containsWord(norm, kw)) {
                val kids = allMembers.filter { it.role.equals("Παιδί", ignoreCase = true) }
                if (kids.isNotEmpty()) {
                    val display = kids.joinToString(" & ") { it.name }
                    return Triple("SPECIFIC_MEMBERS", kids, display)
                }
            }
        }

        val parentsKeywords = listOf("γονεις", "γονιων", "στους γονεις", "για τους γονεις", "οι γονεις")
        for (kw in parentsKeywords) {
            if (containsWord(norm, kw)) {
                val parents = allMembers.filter { it.role.equals("Γονέας", ignoreCase = true) }
                if (parents.isNotEmpty()) {
                    val display = parents.joinToString(" & ") { it.name }
                    return Triple("SPECIFIC_MEMBERS", parents, display)
                }
            }
        }

        // Check individual member voice keywords
        val matchedMembers = mutableListOf<FamilyMember>()
        for (member in allMembers) {
            val keywords = member.voiceKeywords.split(",").map { normalizeGreek(it.trim()) }
            val nameNorm = normalizeGreek(member.name)
            val allMemberKw = (keywords + nameNorm).filter { it.isNotBlank() }

            val found = allMemberKw.any { kw ->
                containsWord(norm, kw)
            }
            if (found && !matchedMembers.contains(member)) {
                matchedMembers.add(member)
            }
        }

        if (matchedMembers.isNotEmpty()) {
            val display = if (matchedMembers.size == allMembers.size) {
                "Κοινό Οικογένειας"
            } else {
                matchedMembers.joinToString(" & ") { it.name }
            }
            return Triple("SPECIFIC_MEMBERS", matchedMembers, display)
        }

        // Default to all family if no specific person mentioned
        return Triple("ALL_FAMILY", allMembers, "Κοινό Οικογένειας")
    }

    private fun extractCategoryAndTitle(
        norm: String,
        originalPrompt: String,
        categories: List<ExpenseCategory>
    ): Pair<ExpenseCategory, String> {
        // Specific item titles mapping
        val specificTitles = listOf(
            "σουβλακια" to "Σουβλάκια",
            "σουβλακι" to "Σουβλάκι",
            "πιτσα" to "Πίτσα",
            "πιτσες" to "Πίτσες",
            "καφεδες" to "Καφέδες",
            "καφες" to "Καφές",
            "κολυμβητηριο" to "Κολυμβητήριο",
            "σουπερ μαρκετ" to "Σούπερ Μάρκετ",
            "σκλαβενιτης" to "Σκλαβενίτης",
            "βασιλοπουλος" to "ΑΒ Βασιλόπουλος",
            "lidl" to "Lidl",
            "βενζινη" to "Βενζίνη",
            "ψιλικα" to "Ψιλικά",
            "περιπτερο" to "Περίπτερο",
            "ενοικιο" to "Ενοίκιο",
            "ρευμα" to "Ρεύμα",
            "δεη" to "ΔΕΗ / Ρεύμα",
            "αεριο" to "Αέριο",
            "κοινοχρηστα" to "Κοινόχρηστα",
            "δανεια" to "Δάνεια",
            "δανειο" to "Δάνειο",
            "δοση αυτοκινητου" to "Δόση Αυτοκινήτου",
            "δοση" to "Δόση Αυτοκινήτου",
            "εξοδα γατιων" to "Έξοδα Γατιών",
            "γατες" to "Έξοδα Γατιών",
            "γατα" to "Έξοδα Γατιών",
            "γατοτροφη" to "Έξοδα Γατιών (Τροφή)",
            "χρωστουμενα" to "Χρωστούμενα",
            "διασκεδαση" to "Διασκέδαση",
            "δωρα" to "Δώρα",
            "δωρο" to "Δώρο",
            "φαρμακειο" to "Φαρμακείο",
            "γιατρος" to "Γιατρός",
            "αγγλικα" to "Αγγλικά",
            "φροντιστηριο" to "Φροντιστήριο",
            "τενις" to "Τένις",
            "μπασκετ" to "Μπάσκετ",
            "ποδοσφαιρο" to "Ποδόσφαιρο"
        )

        var detectedTitle: String? = null
        for ((word, title) in specificTitles) {
            if (containsWord(norm, word)) {
                detectedTitle = title
                break
            }
        }

        // Match category by keyword length descending (most specific first)
        var bestCategory: ExpenseCategory? = null
        var maxKeywordLength = 0

        for (cat in categories) {
            val keywords = cat.voiceKeywords.split(",").map { normalizeGreek(it.trim()) }
            val catNameNorm = normalizeGreek(cat.name)
            val allKw = (keywords + catNameNorm).filter { it.isNotBlank() }

            for (kw in allKw) {
                if (containsWord(norm, kw)) {
                    if (kw.length > maxKeywordLength) {
                        maxKeywordLength = kw.length
                        bestCategory = cat
                    }
                }
            }
        }

        val category = bestCategory
            ?: categories.find { it.name.contains("Σούπερ", ignoreCase = true) }
            ?: categories.firstOrNull()
            ?: ExpenseCategory(
                id = 1,
                name = "Σούπερ μάρκετ",
                iconKey = "cart",
                monthlyBudget = 450.0,
                colorHex = 0xFF059669,
                voiceKeywords = ""
            )

        val finalTitle = detectedTitle ?: category.name
        return Pair(category, finalTitle)
    }
}
