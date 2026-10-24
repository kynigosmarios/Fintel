package com.example.util

import com.example.data.entity.ExpenseCategory
import com.example.data.entity.FamilyMember
import java.util.Calendar
import java.util.Locale

data class ParsedVoiceExpense(
    val title: String,
    val amount: Double,
    val categoryId: Long,
    val categoryName: String,
    val splitType: String = "ALL_FAMILY", // "ALL_FAMILY" or "SPECIFIC_MEMBERS"
    val memberIds: List<Long> = emptyList(),
    val memberNamesDisplay: String = "Κοινό Οικογένειας",
    val splitAmountPerPerson: Double = 0.0,
    val originalPrompt: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val payerMemberId: Long = 0L,
    val payerMemberName: String = "",
    val memberIdsCsv: String = "",
    val payerMemberIdsCsv: String = "",
    val payerAmountsCsv: String = "",
    val note: String = ""
)

object GreekVoiceParser {

    fun containsWord(text: String, word: String): Boolean {
        if (word.isBlank() || text.isBlank()) return false
        val normWord = word.replace('ς', 'σ')
        val normText = text.replace('ς', 'σ')
        val pattern = "(?:^|[^\\p{L}\\p{Nd}])" + Regex.escape(normWord) + "(?:$|[^\\p{L}\\p{Nd}])"
        return Regex(pattern).containsMatchIn(normText)
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
        // Normalize time-like speech transcription (e.g. "5:30" -> "5.30")
        res = res.replace(Regex("(\\d+):(\\d{2})"), "$1.$2")
        // Normalize comma/dot decimals with surrounding digits (e.g. "4,30" or "4 , 30" or "4.30" -> "4.30")
        res = res.replace(Regex("(\\d+)\\s*[,.]\\s*(\\d{1,2})"), "$1.$2")
        // Any other comma is punctuation (e.g. "12 και 30, έξοδα") -> replace with space
        res = res.replace(",", " ")
        return res
    }

    /**
     * Requirement 8: Extract Greek date expressions like "3 Οκτωβρίου", "στις 3 Οκτωβρίου",
     * "3 του Οκτώβρη", "χθες", "προχθές", etc.
     * Returns the timestamp (in millis) and the cleaned prompt with date expressions removed.
     */
    data class DateExtractionResult(
        val timestamp: Long,
        val cleanedNorm: String
    )

    fun extractDate(norm: String): DateExtractionResult {
        val nowCal = Calendar.getInstance()
        val currentYear = nowCal.get(Calendar.YEAR)

        // 1. Relative dates: "σημερα", "χθες", "χτες", "προχθες", "προχτες"
        if (containsWord(norm, "προχθες") || containsWord(norm, "προχτες")) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -2)
            cal.set(Calendar.HOUR_OF_DAY, 12)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val cleaned = norm.replace(Regex("(?:\\bπροχθες\\b|\\bπροχτες\\b)"), " ").trim()
            return DateExtractionResult(cal.timeInMillis, cleaned)
        }

        if (containsWord(norm, "χθες") || containsWord(norm, "χτες")) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -1)
            cal.set(Calendar.HOUR_OF_DAY, 12)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val cleaned = norm.replace(Regex("(?:\\bχθες\\b|\\bχτες\\b)"), " ").trim()
            return DateExtractionResult(cal.timeInMillis, cleaned)
        }

        if (containsWord(norm, "σημερα")) {
            val cleaned = norm.replace(Regex("\\bσημερα\\b"), " ").trim()
            return DateExtractionResult(System.currentTimeMillis(), cleaned)
        }

        // Relative days ago: "πριν 2 μερες", "πριν 3 ημερες"
        val daysAgoRegex = Regex("πριν\\s*(\\d+)\\s*(?:μερες|ημερες)")
        val daysAgoMatch = daysAgoRegex.find(norm)
        if (daysAgoMatch != null) {
            val days = daysAgoMatch.groupValues[1].toIntOrNull() ?: 1
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -days)
            cal.set(Calendar.HOUR_OF_DAY, 12)
            cal.set(Calendar.MINUTE, 0)
            val cleaned = norm.replace(daysAgoMatch.value, " ").trim()
            return DateExtractionResult(cal.timeInMillis, cleaned)
        }

        // 2. Specific day + month
        // Greek Month names mapping (1..12)
        val monthMap = listOf(
            1 to listOf("ιανουαριου", "ιανουαριος", "γεναρη", "γεναρης"),
            2 to listOf("φεβρουαριου", "φεβρουαριος", "φλεβαρη", "φλεβαρης"),
            3 to listOf("μαρτιου", "μαρτιος", "μαρτη", "μαρτης"),
            4 to listOf("απριλιου", "απριλιος", "απριλη", "απριλης"),
            5 to listOf("μαιου", "μαιος", "μαη", "μαης"),
            6 to listOf("ιουνιου", "ιουνιος", "ιουνη", "ιουνης"),
            7 to listOf("ιουλιου", "ιουλιος", "ιουλη", "ιουλης"),
            8 to listOf("αυγουστου", "αυγουστος"),
            9 to listOf("σεπτεμβριου", "σεπτεμβριος", "σεπτεμβρη", "σεπτεμβρης"),
            10 to listOf("οκτωβριου", "οκτωβριος", "οκτωβρη", "οκτωβρης"),
            11 to listOf("νοεμβριου", "νοεμβριος", "νοεμβρη", "νοεμβρης"),
            12 to listOf("δεκεμβριου", "δεκεμβριος", "δεκεμβρη", "δεκεμβρης")
        )

        // Day numbers in Greek words
        val wordDays = mapOf(
            "πρωτη" to 1, "μια" to 1, "δυο" to 2, "τρεις" to 3, "τρια" to 3,
            "τεσσερις" to 4, "τεσσερα" to 4, "πεντε" to 5, "εξι" to 6,
            "επτα" to 7, "εφτα" to 7, "οκτω" to 8, "οχτω" to 8, "εννια" to 9,
            "δεκα" to 10, "εντεκα" to 11, "δωδεκα" to 12, "δεκαπεντε" to 15,
            "εικοσι" to 20, "εικοσιπεντε" to 25
        )

        for ((monthNum, monthNames) in monthMap) {
            for (monthName in monthNames) {
                // Check pattern: [στις ] [day] [του/ης ] [monthName]
                val pattern = Regex("(?:στις\\s+)?(?:την\\s+)?(\\d{1,2}|${wordDays.keys.joinToString("|")})\\s*(?:η|ου|ης)?\\s*(?:του\\s+)?(?:του\\s+μηνος\\s+)?$monthName")
                val match = pattern.find(norm)
                if (match != null) {
                    val dayStr = match.groupValues[1]
                    val day = dayStr.toIntOrNull() ?: wordDays[dayStr] ?: 1

                    val cal = Calendar.getInstance()
                    cal.set(Calendar.YEAR, currentYear)
                    cal.set(Calendar.MONTH, monthNum - 1)
                    cal.set(Calendar.DAY_OF_MONTH, day.coerceIn(1, 31))
                    cal.set(Calendar.HOUR_OF_DAY, 12)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)

                    val cleaned = norm.replace(match.value, " ").trim()
                    return DateExtractionResult(cal.timeInMillis, cleaned)
                }
            }
        }

        // Numeric date format: "3/10", "03/10", "3-10"
        val numericDatePattern = Regex("(?:στις\\s+)?(\\d{1,2})[/-](\\d{1,2})")
        val numMatch = numericDatePattern.find(norm)
        if (numMatch != null) {
            val d = numMatch.groupValues[1].toIntOrNull() ?: 1
            val m = numMatch.groupValues[2].toIntOrNull() ?: 1
            if (m in 1..12 && d in 1..31) {
                val cal = Calendar.getInstance()
                cal.set(Calendar.YEAR, currentYear)
                cal.set(Calendar.MONTH, m - 1)
                cal.set(Calendar.DAY_OF_MONTH, d)
                cal.set(Calendar.HOUR_OF_DAY, 12)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val cleaned = norm.replace(numMatch.value, " ").trim()
                return DateExtractionResult(cal.timeInMillis, cleaned)
            }
        }

        return DateExtractionResult(System.currentTimeMillis(), norm)
    }

    /**
     * Requirement 7: Advanced Greek Spoken Amount & Number Recognition.
     * Examples:
     * - "πέντε σαράντα" -> 5.40€
     * - "πέντε ευρώ και σαράντα λεπτά" -> 5.40€
     * - "πέντε και σαράντα" -> 5.40€
     * - "πέντε σαράντα ευρώ" -> 5.40€
     * - "πεντακόσια σαράντα" -> 540€
     * - "πεντακόσια σαράντα ευρώ" -> 540€
     * - "πεντακόσια σαράντα πέντε" -> 545€
     * - "πεντακόσια σαράντα ευρώ και είκοσι λεπτά" -> 540.20€
     * - "τριακόσια είκοσι" -> 320€
     * - "δώδεκα πενήντα" -> 12.50€
     * - "είκοσι πέντε σαράντα" -> 25.40€
     * - "εκατόν δέκα" -> 110€
     * - "μισό ευρώ" -> 0.50€
     * - "πενήντα λεπτά" -> 0.50€
     */
    enum class NumScale {
        THOUSAND, HUNDRED, TENS, TEEN_OR_UNIT, HALF
    }

    data class GreekWordToken(val num: Double, val scale: NumScale)

    private val GREEK_WORDS_MAP = mapOf(
        // Thousands
        "εκατομμυριο" to GreekWordToken(1000000.0, NumScale.THOUSAND),
        "εκατομμυρια" to GreekWordToken(1000000.0, NumScale.THOUSAND),
        "χιλια" to GreekWordToken(1000.0, NumScale.THOUSAND),
        "χιλιες" to GreekWordToken(1000.0, NumScale.THOUSAND),
        "χιλιοι" to GreekWordToken(1000.0, NumScale.THOUSAND),
        "χιλιαδες" to GreekWordToken(1000.0, NumScale.THOUSAND),
        "χιλιαδων" to GreekWordToken(1000.0, NumScale.THOUSAND),
        "χιλιαρικα" to GreekWordToken(1000.0, NumScale.THOUSAND),
        "χιλιαρικο" to GreekWordToken(1000.0, NumScale.THOUSAND),

        // Hundreds
        "εννιακοσια" to GreekWordToken(900.0, NumScale.HUNDRED),
        "εννεακοσια" to GreekWordToken(900.0, NumScale.HUNDRED),
        "εννιακοσιες" to GreekWordToken(900.0, NumScale.HUNDRED),
        "εννιακοσιοι" to GreekWordToken(900.0, NumScale.HUNDRED),
        "εννιακοσιους" to GreekWordToken(900.0, NumScale.HUNDRED),
        "εννιακοσιων" to GreekWordToken(900.0, NumScale.HUNDRED),
        "οκτακοσια" to GreekWordToken(800.0, NumScale.HUNDRED),
        "οχτακοσια" to GreekWordToken(800.0, NumScale.HUNDRED),
        "οκτακοσιες" to GreekWordToken(800.0, NumScale.HUNDRED),
        "οχτακοσιες" to GreekWordToken(800.0, NumScale.HUNDRED),
        "οκτακοσιοι" to GreekWordToken(800.0, NumScale.HUNDRED),
        "οχτακοσιοι" to GreekWordToken(800.0, NumScale.HUNDRED),
        "οκτακοσιους" to GreekWordToken(800.0, NumScale.HUNDRED),
        "οχτακοσιους" to GreekWordToken(800.0, NumScale.HUNDRED),
        "επτακοσια" to GreekWordToken(700.0, NumScale.HUNDRED),
        "εφτακοσια" to GreekWordToken(700.0, NumScale.HUNDRED),
        "επτακοσιες" to GreekWordToken(700.0, NumScale.HUNDRED),
        "εφτακοσιες" to GreekWordToken(700.0, NumScale.HUNDRED),
        "επτακοσιοι" to GreekWordToken(700.0, NumScale.HUNDRED),
        "εφτακοσιοι" to GreekWordToken(700.0, NumScale.HUNDRED),
        "επτακοσιους" to GreekWordToken(700.0, NumScale.HUNDRED),
        "εφτακοσιους" to GreekWordToken(700.0, NumScale.HUNDRED),
        "εξακοσια" to GreekWordToken(600.0, NumScale.HUNDRED),
        "εξακοσιες" to GreekWordToken(600.0, NumScale.HUNDRED),
        "εξακοσιοι" to GreekWordToken(600.0, NumScale.HUNDRED),
        "εξακοσιους" to GreekWordToken(600.0, NumScale.HUNDRED),
        "πεντακοσια" to GreekWordToken(500.0, NumScale.HUNDRED),
        "πεντακοσιες" to GreekWordToken(500.0, NumScale.HUNDRED),
        "πεντακοσιοι" to GreekWordToken(500.0, NumScale.HUNDRED),
        "πεντακοσιους" to GreekWordToken(500.0, NumScale.HUNDRED),
        "τετρακοσια" to GreekWordToken(400.0, NumScale.HUNDRED),
        "τετρακοσιες" to GreekWordToken(400.0, NumScale.HUNDRED),
        "τετρακοσιοι" to GreekWordToken(400.0, NumScale.HUNDRED),
        "τετρακοσιους" to GreekWordToken(400.0, NumScale.HUNDRED),
        "τετρακοσιων" to GreekWordToken(400.0, NumScale.HUNDRED),
        "τριακοσια" to GreekWordToken(300.0, NumScale.HUNDRED),
        "τριακοσιες" to GreekWordToken(300.0, NumScale.HUNDRED),
        "τριακοσιοι" to GreekWordToken(300.0, NumScale.HUNDRED),
        "τριακοσιους" to GreekWordToken(300.0, NumScale.HUNDRED),
        "τριακοσιων" to GreekWordToken(300.0, NumScale.HUNDRED),
        "διακοσια" to GreekWordToken(200.0, NumScale.HUNDRED),
        "διακοσιες" to GreekWordToken(200.0, NumScale.HUNDRED),
        "διακοσιοι" to GreekWordToken(200.0, NumScale.HUNDRED),
        "διακοσιους" to GreekWordToken(200.0, NumScale.HUNDRED),
        "διακοσιων" to GreekWordToken(200.0, NumScale.HUNDRED),
        "εκατον" to GreekWordToken(100.0, NumScale.HUNDRED),
        "εκατο" to GreekWordToken(100.0, NumScale.HUNDRED),

        // Tens
        "ενενηντα" to GreekWordToken(90.0, NumScale.TENS),
        "ογδοντα" to GreekWordToken(80.0, NumScale.TENS),
        "εβδομηντα" to GreekWordToken(70.0, NumScale.TENS),
        "εξηντα" to GreekWordToken(60.0, NumScale.TENS),
        "πενηντα" to GreekWordToken(50.0, NumScale.TENS),
        "σαραντα" to GreekWordToken(40.0, NumScale.TENS),
        "τριαντα" to GreekWordToken(30.0, NumScale.TENS),
        "εικοσι" to GreekWordToken(20.0, NumScale.TENS),

        // Compound Tens (21-99)
        "εικοσιενα" to GreekWordToken(21.0, NumScale.TENS),
        "εικοσιδυο" to GreekWordToken(22.0, NumScale.TENS),
        "εικοσιτρια" to GreekWordToken(23.0, NumScale.TENS),
        "εικοσιτρεις" to GreekWordToken(23.0, NumScale.TENS),
        "εικοσιτεσσερα" to GreekWordToken(24.0, NumScale.TENS),
        "εικοσιτεσσερις" to GreekWordToken(24.0, NumScale.TENS),
        "εικοσιπεντε" to GreekWordToken(25.0, NumScale.TENS),
        "εικοσιεξι" to GreekWordToken(26.0, NumScale.TENS),
        "εικοσιεπτα" to GreekWordToken(27.0, NumScale.TENS),
        "εικοσιεφτα" to GreekWordToken(27.0, NumScale.TENS),
        "εικοσιοκτω" to GreekWordToken(28.0, NumScale.TENS),
        "εικοσιοχτω" to GreekWordToken(28.0, NumScale.TENS),
        "εικοσιεννια" to GreekWordToken(29.0, NumScale.TENS),
        "εικοσιεννεα" to GreekWordToken(29.0, NumScale.TENS),

        "τριανταενα" to GreekWordToken(31.0, NumScale.TENS),
        "τριανταδυο" to GreekWordToken(32.0, NumScale.TENS),
        "τριαντατρια" to GreekWordToken(33.0, NumScale.TENS),
        "τριαντατρεις" to GreekWordToken(33.0, NumScale.TENS),
        "τριαντατεσσερα" to GreekWordToken(34.0, NumScale.TENS),
        "τριαντατεσσερις" to GreekWordToken(34.0, NumScale.TENS),
        "τριανταπεντε" to GreekWordToken(35.0, NumScale.TENS),
        "τριανταεξι" to GreekWordToken(36.0, NumScale.TENS),
        "τριανταεπτα" to GreekWordToken(37.0, NumScale.TENS),
        "τριανταεφτα" to GreekWordToken(37.0, NumScale.TENS),
        "τριανταοκτω" to GreekWordToken(38.0, NumScale.TENS),
        "τριανταοχτω" to GreekWordToken(38.0, NumScale.TENS),
        "τριανταεννια" to GreekWordToken(39.0, NumScale.TENS),

        "σαρανταενα" to GreekWordToken(41.0, NumScale.TENS),
        "σαρανταδυο" to GreekWordToken(42.0, NumScale.TENS),
        "σαραντατρια" to GreekWordToken(43.0, NumScale.TENS),
        "σαραντατρεις" to GreekWordToken(43.0, NumScale.TENS),
        "σαραντατεσσερα" to GreekWordToken(44.0, NumScale.TENS),
        "σαραντατεσσερις" to GreekWordToken(44.0, NumScale.TENS),
        "σαρανταπεντε" to GreekWordToken(45.0, NumScale.TENS),
        "σαρανταεξι" to GreekWordToken(46.0, NumScale.TENS),
        "σαρανταεπτα" to GreekWordToken(47.0, NumScale.TENS),
        "σαρανταεφτα" to GreekWordToken(47.0, NumScale.TENS),
        "σαρανταοκτω" to GreekWordToken(48.0, NumScale.TENS),
        "σαρανταοχτω" to GreekWordToken(48.0, NumScale.TENS),
        "σαρανταεννια" to GreekWordToken(49.0, NumScale.TENS),

        "πενηνταενα" to GreekWordToken(51.0, NumScale.TENS),
        "πενηνταδυο" to GreekWordToken(52.0, NumScale.TENS),
        "πενηντατρια" to GreekWordToken(53.0, NumScale.TENS),
        "πενηντατρεις" to GreekWordToken(53.0, NumScale.TENS),
        "πενηντατεσσερα" to GreekWordToken(54.0, NumScale.TENS),
        "πενηντατεσσερις" to GreekWordToken(54.0, NumScale.TENS),
        "πενηνταπεντε" to GreekWordToken(55.0, NumScale.TENS),
        "πενηνταεξι" to GreekWordToken(56.0, NumScale.TENS),
        "πενηνταεπτα" to GreekWordToken(57.0, NumScale.TENS),
        "πενηνταεφτα" to GreekWordToken(57.0, NumScale.TENS),
        "πενηνταοκτω" to GreekWordToken(58.0, NumScale.TENS),
        "πενηνταοχτω" to GreekWordToken(58.0, NumScale.TENS),
        "πενηνταεννια" to GreekWordToken(59.0, NumScale.TENS),

        "εξηνταενα" to GreekWordToken(61.0, NumScale.TENS),
        "εξηνταδυο" to GreekWordToken(62.0, NumScale.TENS),
        "εξηντατρια" to GreekWordToken(63.0, NumScale.TENS),
        "εξηντατεσσερα" to GreekWordToken(64.0, NumScale.TENS),
        "εξηνταπεντε" to GreekWordToken(65.0, NumScale.TENS),
        "εξηνταεξι" to GreekWordToken(66.0, NumScale.TENS),
        "εξηνταεπτα" to GreekWordToken(67.0, NumScale.TENS),
        "εξηνταεφτα" to GreekWordToken(67.0, NumScale.TENS),
        "εξηνταοκτω" to GreekWordToken(68.0, NumScale.TENS),
        "εξηνταεννια" to GreekWordToken(69.0, NumScale.TENS),

        "εβδομηνταενα" to GreekWordToken(71.0, NumScale.TENS),
        "εβδομηνταδυο" to GreekWordToken(72.0, NumScale.TENS),
        "εβδομηντατρια" to GreekWordToken(73.0, NumScale.TENS),
        "εβδομηντατεσσερα" to GreekWordToken(74.0, NumScale.TENS),
        "εβδομηνταπεντε" to GreekWordToken(75.0, NumScale.TENS),
        "εβδομηνταεξι" to GreekWordToken(76.0, NumScale.TENS),
        "εβδομηνταεπτα" to GreekWordToken(77.0, NumScale.TENS),
        "εβδομηνταεφτα" to GreekWordToken(77.0, NumScale.TENS),
        "εβδομηνταοκτω" to GreekWordToken(78.0, NumScale.TENS),
        "εβδομηνταεννια" to GreekWordToken(79.0, NumScale.TENS),

        "ογδονταενα" to GreekWordToken(81.0, NumScale.TENS),
        "ογδονταδυο" to GreekWordToken(82.0, NumScale.TENS),
        "ογδοντατρια" to GreekWordToken(83.0, NumScale.TENS),
        "ογδοντατεσσερα" to GreekWordToken(84.0, NumScale.TENS),
        "ογδονταπεντε" to GreekWordToken(85.0, NumScale.TENS),
        "ογδονταεξι" to GreekWordToken(86.0, NumScale.TENS),
        "ογδονταεπτα" to GreekWordToken(87.0, NumScale.TENS),
        "ογδονταεφτα" to GreekWordToken(87.0, NumScale.TENS),
        "ογδονταοκτω" to GreekWordToken(88.0, NumScale.TENS),
        "ογδονταεννια" to GreekWordToken(89.0, NumScale.TENS),

        "ενενηνταενα" to GreekWordToken(91.0, NumScale.TENS),
        "ενενηνταδυο" to GreekWordToken(92.0, NumScale.TENS),
        "ενενηντατρια" to GreekWordToken(93.0, NumScale.TENS),
        "ενενηντατεσσερα" to GreekWordToken(94.0, NumScale.TENS),
        "ενενηνταπεντε" to GreekWordToken(95.0, NumScale.TENS),
        "ενενηνταεξι" to GreekWordToken(96.0, NumScale.TENS),
        "ενενηνταεπτα" to GreekWordToken(97.0, NumScale.TENS),
        "ενενηνταεφτα" to GreekWordToken(97.0, NumScale.TENS),
        "ενενηνταοκτω" to GreekWordToken(98.0, NumScale.TENS),
        "ενενηνταεννια" to GreekWordToken(99.0, NumScale.TENS),

        // Teens
        "δεκαεννια" to GreekWordToken(19.0, NumScale.TEEN_OR_UNIT),
        "δεκαεννεα" to GreekWordToken(19.0, NumScale.TEEN_OR_UNIT),
        "δεκαοκτω" to GreekWordToken(18.0, NumScale.TEEN_OR_UNIT),
        "δεκαοχτω" to GreekWordToken(18.0, NumScale.TEEN_OR_UNIT),
        "δεκαεπτα" to GreekWordToken(17.0, NumScale.TEEN_OR_UNIT),
        "δεκαεφτα" to GreekWordToken(17.0, NumScale.TEEN_OR_UNIT),
        "δεκαεξι" to GreekWordToken(16.0, NumScale.TEEN_OR_UNIT),
        "δεκαξι" to GreekWordToken(16.0, NumScale.TEEN_OR_UNIT),
        "δεκαπεντε" to GreekWordToken(15.0, NumScale.TEEN_OR_UNIT),
        "δεκατεσσερα" to GreekWordToken(14.0, NumScale.TEEN_OR_UNIT),
        "δεκατεσσερις" to GreekWordToken(14.0, NumScale.TEEN_OR_UNIT),
        "δεκατεσσαρα" to GreekWordToken(14.0, NumScale.TEEN_OR_UNIT),
        "δεκατρια" to GreekWordToken(13.0, NumScale.TEEN_OR_UNIT),
        "δεκατρεις" to GreekWordToken(13.0, NumScale.TEEN_OR_UNIT),
        "δωδεκα" to GreekWordToken(12.0, NumScale.TEEN_OR_UNIT),
        "εντεκα" to GreekWordToken(11.0, NumScale.TEEN_OR_UNIT),
        "ενδεκα" to GreekWordToken(11.0, NumScale.TEEN_OR_UNIT),
        "δεκα" to GreekWordToken(10.0, NumScale.TEEN_OR_UNIT),
        "δεκατεσσερις" to GreekWordToken(14.0, NumScale.TEEN_OR_UNIT),
        "δεκατρια" to GreekWordToken(13.0, NumScale.TEEN_OR_UNIT),
        "δεκατρεις" to GreekWordToken(13.0, NumScale.TEEN_OR_UNIT),
        "δωδεκα" to GreekWordToken(12.0, NumScale.TEEN_OR_UNIT),
        "εντεκα" to GreekWordToken(11.0, NumScale.TEEN_OR_UNIT),
        "δεκα" to GreekWordToken(10.0, NumScale.TEEN_OR_UNIT),

        // Units
        "εννια" to GreekWordToken(9.0, NumScale.TEEN_OR_UNIT),
        "εννεα" to GreekWordToken(9.0, NumScale.TEEN_OR_UNIT),
        "οκτω" to GreekWordToken(8.0, NumScale.TEEN_OR_UNIT),
        "οχτω" to GreekWordToken(8.0, NumScale.TEEN_OR_UNIT),
        "επτα" to GreekWordToken(7.0, NumScale.TEEN_OR_UNIT),
        "εφτα" to GreekWordToken(7.0, NumScale.TEEN_OR_UNIT),
        "εξι" to GreekWordToken(6.0, NumScale.TEEN_OR_UNIT),
        "πεντε" to GreekWordToken(5.0, NumScale.TEEN_OR_UNIT),
        "τεσσερα" to GreekWordToken(4.0, NumScale.TEEN_OR_UNIT),
        "τεσσερις" to GreekWordToken(4.0, NumScale.TEEN_OR_UNIT),
        "τρια" to GreekWordToken(3.0, NumScale.TEEN_OR_UNIT),
        "τρεις" to GreekWordToken(3.0, NumScale.TEEN_OR_UNIT),
        "δυο" to GreekWordToken(2.0, NumScale.TEEN_OR_UNIT),
        "ενα" to GreekWordToken(1.0, NumScale.TEEN_OR_UNIT),
        "ενας" to GreekWordToken(1.0, NumScale.TEEN_OR_UNIT),
        "μια" to GreekWordToken(1.0, NumScale.TEEN_OR_UNIT),
        "μηδεν" to GreekWordToken(0.0, NumScale.TEEN_OR_UNIT),

        // Half & fractions
        "μισο" to GreekWordToken(0.5, NumScale.HALF),
        "εναμιση" to GreekWordToken(1.5, NumScale.HALF),
        "εναμισι" to GreekWordToken(1.5, NumScale.HALF),
        "δυομιση" to GreekWordToken(2.5, NumScale.HALF),
        "δυομισι" to GreekWordToken(2.5, NumScale.HALF),
        "τρεισιμιση" to GreekWordToken(3.5, NumScale.HALF),
        "τριαμιση" to GreekWordToken(3.5, NumScale.HALF),
        "τεσσερισιμιση" to GreekWordToken(4.5, NumScale.HALF),
        "τεσσεραμιση" to GreekWordToken(4.5, NumScale.HALF),
        "πεντεμιση" to GreekWordToken(5.5, NumScale.HALF)
    )

    enum class TokenType {
        NUMBER, CURRENCY, CENTS_UNIT, AND, OTHER
    }

    data class ParsedToken(
        val type: TokenType,
        val value: Double = 0.0,
        val scale: NumScale = NumScale.TEEN_OR_UNIT,
        val original: String = ""
    )

    data class AmountResult(
        val amount: Double,
        val consumedWords: Set<String>
    )

    fun extractAmount(norm: String): Double? {
        return extractAmountDetailed(norm)?.amount
    }

    fun extractAmountDetailed(norm: String): AmountResult? {
        val words = norm.split(Regex("\\s+")).map { it.trim('.', ',', ':', ';', '!', '?', '-', '/') }.filter { it.isNotBlank() }

        // 1. Try word-based parsing by analyzing sequences of Greek numbers / digits
        val amountFromTokens = parseAmountFromWordsAndDigitsDetailed(words)
        if (amountFromTokens != null && amountFromTokens.amount > 0.0) {
            return amountFromTokens
        }

        // 2. Check for explicit decimal pattern e.g. "4.30", "5.30", "12.50", "4.30 ευρω", "14726,70"
        val regexThousandsWithComma = Regex("(\\d{1,3}(?:[.]\\d{3})+)[,](\\d{1,2})\\s*(?:ευρω|ευρα)?")
        val thMatch = regexThousandsWithComma.find(norm)
        if (thMatch != null) {
            val numStr = thMatch.groupValues[1].replace(".", "") + "." + thMatch.groupValues[2]
            val num = numStr.toDoubleOrNull()
            if (num != null && num > 0.0) {
                return AmountResult(num, setOf(thMatch.value.trim(), thMatch.groupValues[1], thMatch.groupValues[2], "ευρω", "ευρα"))
            }
        }

        val regexDecimal = Regex("(\\d+[.,]\\d{1,2})\\s*(?:ευρω|ευρα)?")
        val decMatch = regexDecimal.find(norm)
        if (decMatch != null) {
            val numStr = decMatch.groupValues[1].replace(',', '.')
            val num = numStr.toDoubleOrNull()
            if (num != null && num > 0.0) {
                return AmountResult(num, setOf(decMatch.value.trim(), decMatch.groupValues[1], "ευρω", "ευρα"))
            }
        }

        // 3. Fallback: Check standalone numbers with / without currency e.g. "15 ευρω", "540"
        val regexDigits = Regex("(\\d+(?:[.,]\\d+)?)\\s*(?:ευρω|ευρα)?")
        val matches = regexDigits.findAll(norm).toList()
        for (m in matches) {
            val numStr = m.groupValues[1].replace(',', '.')
            val num = numStr.toDoubleOrNull()
            if (num != null && num > 0.0) {
                return AmountResult(num, setOf(m.value.trim(), m.groupValues[1], "ευρω", "ευρα"))
            }
        }

        return null
    }

    private fun computeNumberSequence(tokens: List<ParsedToken>): Double {
        if (tokens.isEmpty()) return 0.0
        if (tokens.size == 1) return tokens[0].value

        var total = 0.0
        var current = 0.0
        for (t in tokens) {
            if (t.scale == NumScale.THOUSAND) {
                val mult = if (current == 0.0) 1.0 else current
                total += mult * t.value
                current = 0.0
            } else {
                current += t.value
            }
        }
        return total + current
    }

    private fun computeCents(tokens: List<ParsedToken>, hasCentsUnit: Boolean): Double {
        if (tokens.isEmpty()) return 0.0
        if (tokens.size == 1 && tokens[0].scale == NumScale.HALF) return 0.50
        val raw = computeNumberSequence(tokens)
        return when {
            raw in 1.0..9.0 && hasCentsUnit -> raw / 100.0 // 5 λεπτά -> 0.05
            raw in 1.0..9.0 -> raw * 10.0 / 100.0          // "πέντε και τρία" -> 0.30
            raw in 10.0..99.0 -> raw / 100.0               // 30 -> 0.30, 40 -> 0.40, 70 -> 0.70
            else -> 0.0
        }
    }

    private fun parseAmountFromWordsAndDigits(words: List<String>): Double? {
        return parseAmountFromWordsAndDigitsDetailed(words)?.amount
    }

    private fun parseAmountFromWordsAndDigitsDetailed(words: List<String>): AmountResult? {
        if (words.isEmpty()) return null

        val tokens = mutableListOf<ParsedToken>()
        for (w in words) {
            when {
                w == "ευρω" || w == "ευρα" -> {
                    tokens.add(ParsedToken(TokenType.CURRENCY, original = w))
                }
                w == "λεπτα" || w == "λεπτο" -> {
                    tokens.add(ParsedToken(TokenType.CENTS_UNIT, original = w))
                }
                w == "και" || w == "κι" -> {
                    tokens.add(ParsedToken(TokenType.AND, original = w))
                }
                GREEK_WORDS_MAP.containsKey(w) -> {
                    val gw = GREEK_WORDS_MAP[w]!!
                    tokens.add(ParsedToken(TokenType.NUMBER, gw.num, gw.scale, original = w))
                }
                else -> {
                    val asNum = w.replace(',', '.').toDoubleOrNull()
                    if (asNum != null) {
                        val scale = when {
                            asNum >= 1000 -> NumScale.THOUSAND
                            asNum >= 100 -> NumScale.HUNDRED
                            asNum >= 20 -> NumScale.TENS
                            asNum.rem(1.0) != 0.0 -> NumScale.HALF
                            else -> NumScale.TEEN_OR_UNIT
                        }
                        tokens.add(ParsedToken(TokenType.NUMBER, asNum, scale, original = w))
                    } else {
                        tokens.add(ParsedToken(TokenType.OTHER, original = w))
                    }
                }
            }
        }

        // 1. Direct decimal token check (e.g. speech recognizer returned "4.30" or "5.30")
        for (t in tokens) {
            if (t.type == TokenType.NUMBER && t.original.matches(Regex("^\\d+[.,]\\d+$"))) {
                if (t.value > 0.0) return AmountResult(t.value, setOf(t.original, "ευρω", "ευρα"))
            }
        }

        // Special case: "μισό ευρώ"
        for (i in tokens.indices) {
            if (tokens[i].original == "μισο" &&
                ((i + 1 < tokens.size && tokens[i + 1].type == TokenType.CURRENCY) ||
                 (i > 0 && tokens[i - 1].type == TokenType.CURRENCY))
            ) {
                return AmountResult(0.50, setOf("μισο", "ευρω", "ευρα"))
            }
        }

        // 2. CURRENCY-based parsing ("ευρω" / "ευρα")
        // Examples: "4 ευρώ και 30 λεπτά", "4 ευρώ 30 λεπτά", "4 ευρώ και 30", "4 ευρώ 30",
        // "τετρακόσια ευρώ", "δεκατέσσερις χιλιάδες επτακόσια εικοσιέξι ευρώ και εβδομήντα λεπτά"
        val curIndices = tokens.indices.filter { tokens[it].type == TokenType.CURRENCY }
        for (cIdx in curIndices) {
            // Collect euros backwards from cIdx - 1
            var startEuros = cIdx - 1
            while (startEuros >= 0 && (tokens[startEuros].type == TokenType.NUMBER || tokens[startEuros].type == TokenType.AND)) {
                startEuros--
            }
            startEuros++
            val eurosTokens = tokens.subList(startEuros, cIdx).filter { it.type == TokenType.NUMBER }
            val euros = computeNumberSequence(eurosTokens)

            if (euros > 0.0) {
                // Collect cents forward from cIdx + 1
                var nextIdx = cIdx + 1
                if (nextIdx < tokens.size && tokens[nextIdx].type == TokenType.AND) {
                    nextIdx++
                }
                var endCents = nextIdx
                while (endCents < tokens.size && tokens[endCents].type == TokenType.NUMBER) {
                    endCents++
                }
                val centsTokens = tokens.subList(nextIdx, endCents)
                val hasCentsUnit = (endCents < tokens.size && tokens[endCents].type == TokenType.CENTS_UNIT)
                val cents = computeCents(centsTokens, hasCentsUnit)

                val consumed = mutableSetOf<String>()
                for (k in startEuros until (if (hasCentsUnit) endCents + 1 else endCents)) {
                    consumed.add(tokens[k].original)
                }
                consumed.add("ευρω")
                consumed.add("ευρα")

                return AmountResult(euros + cents, consumed)
            }
        }

        // 3. AND-based parsing ("και" / "κι") without currency
        // Examples: "πέντε και τριάντα", "πέντε και τριάντα λεπτά", "4 και 30", "4 και 30 λεπτά", "5 και 30"
        val andIndices = tokens.indices.filter { tokens[it].type == TokenType.AND }
        for (aIdx in andIndices) {
            var startEuros = aIdx - 1
            while (startEuros >= 0 && tokens[startEuros].type == TokenType.NUMBER) {
                startEuros--
            }
            startEuros++
            val eurosTokens = tokens.subList(startEuros, aIdx)
            val euros = computeNumberSequence(eurosTokens)

            var endCents = aIdx + 1
            while (endCents < tokens.size && tokens[endCents].type == TokenType.NUMBER) {
                endCents++
            }
            val centsTokens = tokens.subList(aIdx + 1, endCents)
            val hasCentsUnit = (endCents < tokens.size && tokens[endCents].type == TokenType.CENTS_UNIT)
            val cents = computeCents(centsTokens, hasCentsUnit)

            if (euros > 0.0 && cents > 0.0) {
                val consumed = mutableSetOf<String>()
                for (k in startEuros until (if (hasCentsUnit) endCents + 1 else endCents)) {
                    consumed.add(tokens[k].original)
                }
                return AmountResult(euros + cents, consumed)
            }
        }

        // 4. Colloquial <euros> <cents> without "και" and without "ευρω"
        // Examples: "πέντε τριάντα", "πέντε τριάντα λεπτά", "5 30", "4 30", "δώδεκα πενήντα", "είκοσι πέντε σαράντα"
        var idx = 0
        while (idx < tokens.size) {
            if (tokens[idx].type == TokenType.NUMBER) {
                var endSeq = idx
                while (endSeq < tokens.size && tokens[endSeq].type == TokenType.NUMBER) {
                    endSeq++
                }
                val numTokens = tokens.subList(idx, endSeq)
                val hasCentsUnit = (endSeq < tokens.size && tokens[endSeq].type == TokenType.CENTS_UNIT)

                if (numTokens.size >= 2) {
                    val lastToken = numTokens.last()
                    val prevTokens = numTokens.dropLast(1)
                    val prevScale = prevTokens.last().scale
                    val lastScale = lastToken.scale

                    val isCompoundAddition = (prevScale == NumScale.THOUSAND && lastScale == NumScale.HUNDRED) ||
                            (prevScale == NumScale.HUNDRED && (lastScale == NumScale.TENS || lastScale == NumScale.TEEN_OR_UNIT)) ||
                            (prevScale == NumScale.TENS && lastScale == NumScale.TEEN_OR_UNIT && lastToken.value < 10.0)

                    if (!isCompoundAddition && lastToken.value in 1.0..99.0) {
                        val euros = computeNumberSequence(prevTokens)
                        val cents = computeCents(listOf(lastToken), hasCentsUnit)
                        if (euros > 0.0 && cents > 0.0) {
                            val consumed = numTokens.map { it.original }.toMutableSet()
                            if (hasCentsUnit) consumed.add(tokens[endSeq].original)
                            return AmountResult(euros + cents, consumed)
                        }
                    }
                }

                // 5. Cents only: e.g. "30 λεπτά", "πενήντα λεπτά"
                if (hasCentsUnit) {
                    val cents = computeCents(numTokens, hasCentsUnit = true)
                    if (cents > 0.0) {
                        val consumed = numTokens.map { it.original }.toMutableSet()
                        consumed.add(tokens[endSeq].original)
                        return AmountResult(cents, consumed)
                    }
                }

                // 6. Simple number: e.g. "πεντακόσια σαράντα", "500", "τετρακοσια"
                val singleNum = computeNumberSequence(numTokens)
                if (singleNum > 0.0) {
                    val consumed = numTokens.map { it.original }.toMutableSet()
                    return AmountResult(singleNum, consumed)
                }

                idx = endSeq
            } else {
                idx++
            }
        }

        return null
    }

    data class ParsedPayerResult(
        val primaryId: Long,
        val displayName: String,
        val payerIdsCsv: String,
        val payerAmountsCsv: String,
        val payers: List<FamilyMember>
    )

    /**
     * Generates Greek inflected cases for member names (nominative, accusative, genitive)
     * e.g. "Μάριος" -> ["μαριος", "μαριο", "μαριου", "μαριε", "μαριον", "μαριοσ"]
     * e.g. "Βασιλική" -> ["βασιλικη", "βασιλικης", "βασιλικησ"]
     */
    fun getMemberNameVariations(member: FamilyMember): List<String> {
        val list = mutableListOf<String>()

        fun addWithInflections(raw: String) {
            val clean = raw.trim()
                .replace("\"", "")
                .replace("'", "")
                .replace("«", "")
                .replace("»", "")
                .replace("“", "")
                .replace("”", "")
            if (clean.isBlank()) return

            val parts = clean.split(Regex("[\\s,;/-]+")).filter { it.isNotBlank() }
            if (parts.size > 1) {
                for (p in parts) {
                    addWithInflections(p)
                }
            }

            val norm = normalizeGreek(clean)
            if (norm.length >= 2) {
                list.add(norm)
                list.add(norm.replace('ς', 'σ'))
                list.add(norm.replace('σ', 'ς'))

                val normSigma = norm.replace('σ', 'ς')
                when {
                    normSigma.endsWith("ος") -> {
                        val stem = normSigma.dropLast(2)
                        list.add(stem + "ο")
                        list.add(stem + "ου")
                        list.add(stem + "ε")
                        list.add(stem + "ον")
                        list.add(stem + "οσ")
                        list.add(stem + "ουσ")
                    }
                    normSigma.endsWith("ης") -> {
                        val stem = normSigma.dropLast(2)
                        list.add(stem + "η")
                        list.add(stem + "ηδες")
                        list.add(stem + "ηδων")
                        list.add(stem + "ησ")
                    }
                    normSigma.endsWith("ας") -> {
                        val stem = normSigma.dropLast(2)
                        list.add(stem + "α")
                        list.add(stem + "αδες")
                        list.add(stem + "αδων")
                        list.add(stem + "ασ")
                    }
                    normSigma.endsWith("α") -> {
                        list.add(normSigma + "ς")
                        list.add(normSigma + "σ")
                    }
                    normSigma.endsWith("η") -> {
                        list.add(normSigma + "ς")
                        list.add(normSigma + "σ")
                    }
                }

                when (norm.replace('ς', 'σ')) {
                    "μαριοσ" -> {
                        list.add("marios")
                        list.add("mario")
                    }
                    "βασιλικη" -> {
                        list.add("vasiliki")
                        list.add("vicky")
                    }
                    "μελινα" -> {
                        list.add("melina")
                    }
                    "γεωργια" -> {
                        list.add("georgia")
                    }
                }
            }
        }

        addWithInflections(member.name)
        member.voiceKeywords.split(Regex("[,;/\n]+")).forEach { kw ->
            addWithInflections(kw)
        }

        return list.distinct().filter { it.length >= 2 }
    }

    fun parse(
        prompt: String,
        categories: List<ExpenseCategory>,
        allMembers: List<FamilyMember>
    ): ParsedVoiceExpense {
        val norm = normalizeGreek(prompt)

        // 1. Date Extraction (e.g. "3 Οκτωβρίου", "χθες", etc.)
        val (detectedTimestamp, cleanedNorm) = extractDate(norm)

        // 2. Amount Extraction (supports numbers from 0.01 to 1,000,000+)
        val amountDetailed = extractAmountDetailed(cleanedNorm) ?: extractAmountDetailed(norm)
        val amount = amountDetailed?.amount ?: 10.0
        val consumedAmountWords = amountDetailed?.consumedWords ?: emptySet()

        // 3. Payer vs Beneficiary Extraction with magic words: "για", "αφορά", "αφορούν"
        val (payerResult, beneficiaryInfo, consumedMemberWords) = extractPayerAndBeneficiaries(
            cleanedNorm = cleanedNorm,
            allMembers = allMembers,
            amount = amount
        )
        val (splitType, matchedMembers, memberDisplay) = beneficiaryInfo

        // 4. Category & Title/Description Extraction
        val (category, title) = extractCategoryAndTitleDetailed(
            norm = cleanedNorm,
            originalPrompt = prompt,
            categories = categories,
            allMembers = allMembers,
            consumedAmountWords = consumedAmountWords,
            consumedMemberWords = consumedMemberWords
        )

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
            originalPrompt = prompt,
            timestamp = detectedTimestamp,
            payerMemberId = payerResult.primaryId,
            payerMemberName = payerResult.displayName,
            payerMemberIdsCsv = payerResult.payerIdsCsv,
            payerAmountsCsv = payerResult.payerAmountsCsv,
            memberIdsCsv = matchedMembers.joinToString(",") { it.id.toString() },
            note = title
        )
    }

    /**
     * Requirement 3: Payer vs Beneficiaries with "για" / "αφορά".
     * Supports single and multiple payers, as well as single and multiple beneficiaries.
     * E.g. "Έξι ευρώ καφέδες Μάριος για Μάριο και Βασιλική":
     * - Payer: Μάριος
     * - Beneficiaries: [Μάριος, Βασιλική]
     */
    private fun extractPayerAndBeneficiaries(
        cleanedNorm: String,
        allMembers: List<FamilyMember>,
        amount: Double
    ): Triple<ParsedPayerResult, Triple<String, List<FamilyMember>, String>, Set<String>> {
        val defaultParent = allMembers.firstOrNull { it.role.equals("Γονέας", ignoreCase = true) }
            ?: allMembers.firstOrNull()
        val defaultPayerId = defaultParent?.id ?: 0L
        val defaultPayerName = defaultParent?.name ?: ""

        val consumedWords = mutableSetOf<String>()

        fun buildPayerResult(payers: List<FamilyMember>): ParsedPayerResult {
            val list = if (payers.isNotEmpty()) payers else listOfNotNull(defaultParent)
            val primary = list.firstOrNull()
            val primaryId = primary?.id ?: defaultPayerId
            val displayName = if (list.size >= 2) {
                list.joinToString(" & ") { it.name }
            } else {
                primary?.name ?: defaultPayerName
            }
            val idsCsv = list.joinToString(",") { it.id.toString() }
            val splitAmt = if (list.isNotEmpty()) amount / list.size else amount
            val amountsCsv = list.joinToString(",") { "${it.id}:${String.format(Locale.US, "%.2f", splitAmt)}" }
            return ParsedPayerResult(primaryId, displayName, idsCsv, amountsCsv, list)
        }

        // Magic words for beneficiaries: "για", "αφορα", "αφορουν" (Unicode-safe word boundaries)
        val beneficiaryPattern = Regex("(?:^|[^\\p{L}\\p{Nd}])(για|αφορα|αφορουν)(?:\\s+(?:τον|την|το|τους|τις|τα|ολη\\s+την|ολο\\s+το))?(?=$|[^\\p{L}\\p{Nd}])", RegexOption.IGNORE_CASE)
        val match = beneficiaryPattern.find(cleanedNorm)

        if (match != null) {
            val kwGroup = match.groups[1]!!
            val beforePart = cleanedNorm.substring(0, kwGroup.range.first).trim()
            val afterPart = cleanedNorm.substring(match.range.last + 1).trim()

            match.value.split(Regex("[^\\p{L}\\p{Nd}]+")).filter { it.isNotBlank() }.forEach { consumedWords.add(it) }
            consumedWords.add("για")
            consumedWords.add("αφορα")
            consumedWords.add("αφορουν")

            // 1. Beneficiaries extracted from afterPart
            val beneficiaryResult = extractBeneficiariesFromText(afterPart, allMembers, consumedWords)

            // 2. Payers extracted from beforePart (who paid)
            val matchedPayers = mutableListOf<FamilyMember>()
            for (m in allMembers) {
                val variations = getMemberNameVariations(m)
                val found = variations.any { v -> containsWord(beforePart, v) }
                if (found && !matchedPayers.contains(m)) {
                    matchedPayers.add(m)
                    variations.filter { containsWord(beforePart, it) }.forEach { consumedWords.add(it) }
                }
            }

            if (matchedPayers.size >= 2) {
                consumedWords.add("και")
                consumedWords.add("κι")
            }

            val payerResult = buildPayerResult(matchedPayers)
            return Triple(payerResult, beneficiaryResult, consumedWords)
        } else {
            // No "για" / "αφορά" present:
            // If explicit payer phrase: "πληρωσε ο/η..."
            var explicitPayer: FamilyMember? = null
            for (m in allMembers) {
                val variations = getMemberNameVariations(m)
                for (v in variations) {
                    if (cleanedNorm.contains("πληρωσε $v") ||
                        cleanedNorm.contains("πληρωσε ο $v") ||
                        cleanedNorm.contains("πληρωσε η $v") ||
                        cleanedNorm.contains("$v πληρωσε") ||
                        cleanedNorm.contains("πληρωμη απο $v") ||
                        cleanedNorm.contains("εβαλε ο $v") ||
                        cleanedNorm.contains("εβαλε η $v") ||
                        cleanedNorm.contains("εβαλε $v")
                    ) {
                        explicitPayer = m
                        consumedWords.add(v)
                        consumedWords.add("πληρωσε")
                        consumedWords.add("εβαλε")
                        consumedWords.add("πληρωμη")
                        break
                    }
                }
                if (explicitPayer != null) break
            }

            val matchedPayers = mutableListOf<FamilyMember>()
            if (explicitPayer != null) {
                matchedPayers.add(explicitPayer)
            } else {
                for (m in allMembers) {
                    val variations = getMemberNameVariations(m)
                    if (variations.any { containsWord(cleanedNorm, it) } && !matchedPayers.contains(m)) {
                        matchedPayers.add(m)
                        variations.filter { containsWord(cleanedNorm, it) }.forEach { consumedWords.add(it) }
                    }
                }
            }

            if (matchedPayers.size >= 2) {
                consumedWords.add("και")
                consumedWords.add("κι")
            }

            val payerResult = buildPayerResult(matchedPayers)
            val beneficiaryResult = if (matchedPayers.isEmpty()) {
                Triple("ALL_FAMILY", allMembers, "Κοινό Οικογένειας")
            } else if (matchedPayers.size == allMembers.size) {
                Triple("ALL_FAMILY", allMembers, "Κοινό Οικογένειας")
            } else {
                val display = if (matchedPayers.size >= 2) {
                    val firstRolePlural = Localization.pluralRoleName(matchedPayers.first().role, Localization.Language.GREEK)
                    val allSame = matchedPayers.all {
                        Localization.pluralRoleName(it.role, Localization.Language.GREEK).equals(firstRolePlural, ignoreCase = true)
                    }
                    if (allSame) firstRolePlural else matchedPayers.joinToString(" & ") { it.name }
                } else {
                    matchedPayers.first().name
                }
                Triple("SPECIFIC_MEMBERS", matchedPayers, display)
            }

            return Triple(payerResult, beneficiaryResult, consumedWords)
        }
    }

    private fun extractBeneficiariesFromText(
        text: String,
        allMembers: List<FamilyMember>,
        consumedWords: MutableSet<String>
    ): Triple<String, List<FamilyMember>, String> {
        val allFamilyKeywords = listOf(
            "οικογενεια", "ολοι", "σπιτι", "κοινο", "σπιτιου", "ολο το σπιτι", "παντες", "κοινα"
        )
        for (kw in allFamilyKeywords) {
            if (containsWord(text, kw)) {
                consumedWords.add(kw)
                return Triple("ALL_FAMILY", allMembers, "Κοινό Οικογένειας")
            }
        }

        val kidsKeywords = listOf("παιδια", "παιδιων", "στα παιδια", "τα παιδια")
        for (kw in kidsKeywords) {
            if (containsWord(text, kw)) {
                consumedWords.add(kw)
                val kids = allMembers.filter { it.role.equals("Παιδί", ignoreCase = true) }
                if (kids.isNotEmpty()) {
                    val display = if (kids.size >= 2) "Παιδιά" else kids.first().name
                    return Triple("SPECIFIC_MEMBERS", kids, display)
                }
            }
        }

        val parentsKeywords = listOf("γονεις", "γονιων", "στους γονεις", "οι γονεις")
        for (kw in parentsKeywords) {
            if (containsWord(text, kw)) {
                consumedWords.add(kw)
                val parents = allMembers.filter { it.role.equals("Γονέας", ignoreCase = true) }
                if (parents.isNotEmpty()) {
                    val display = if (parents.size >= 2) "Γονείς" else parents.first().name
                    return Triple("SPECIFIC_MEMBERS", parents, display)
                }
            }
        }

        // Match individual members by name variations
        val matchedMembers = mutableListOf<FamilyMember>()
        for (member in allMembers) {
            val variations = getMemberNameVariations(member)
            val found = variations.any { v -> containsWord(text, v) }
            if (found && !matchedMembers.contains(member)) {
                matchedMembers.add(member)
                variations.filter { containsWord(text, it) }.forEach { consumedWords.add(it) }
            }
        }

        if (matchedMembers.isNotEmpty()) {
            val display = if (matchedMembers.size >= 2) {
                matchedMembers.joinToString(" & ") { it.name }
            } else {
                matchedMembers.first().name
            }
            return Triple("SPECIFIC_MEMBERS", matchedMembers, display)
        }

        return Triple("ALL_FAMILY", allMembers, "Κοινό Οικογένειας")
    }

    private fun extractCategoryAndTitleDetailed(
        norm: String,
        originalPrompt: String,
        categories: List<ExpenseCategory>,
        allMembers: List<FamilyMember>,
        consumedAmountWords: Set<String>,
        consumedMemberWords: Set<String>
    ): Pair<ExpenseCategory, String> {
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

        var bestCategory: ExpenseCategory? = null
        var matchedCategoryWord: String? = null
        var maxKeywordLength = 0

        // 1. Search category keywords in normalized prompt
        for (cat in categories) {
            val keywords = cat.voiceKeywords.split(",").map { normalizeGreek(it.trim()) }
            val catNameNorm = normalizeGreek(cat.name)
            val allKw = (keywords + catNameNorm).filter { it.isNotBlank() }

            for (kw in allKw) {
                if (containsWord(norm, kw)) {
                    if (kw.length > maxKeywordLength) {
                        maxKeywordLength = kw.length
                        bestCategory = cat
                        matchedCategoryWord = kw
                    }
                }
            }
        }

        // 2. Fallback domain category matching
        if (bestCategory == null) {
            val searchTarget = norm.lowercase(Locale.ROOT)
            if (searchTarget.contains("παπουτσ") || searchTarget.contains("ρουχ") || searchTarget.contains("μπλουζ") || searchTarget.contains("zara")) {
                bestCategory = categories.find { it.name.contains("Ένδυση", ignoreCase = true) || it.name.contains("Αγορές", ignoreCase = true) || it.name.contains("Shopping", ignoreCase = true) }
            } else if (searchTarget.contains("λαδι") || searchTarget.contains("συνεργει") || searchTarget.contains("λαστιχ") || searchTarget.contains("βενζιν") || searchTarget.contains("γκαραζ") || searchTarget.contains("parking")) {
                bestCategory = categories.find { it.name.contains("Αυτοκίνητο", ignoreCase = true) || it.name.contains("Μετακίνηση", ignoreCase = true) || it.name.contains("Βενζίνη", ignoreCase = true) }
            } else if (searchTarget.contains("κουρεμ") || searchTarget.contains("κομμωτηρι") || searchTarget.contains("φαρμακ") || searchTarget.contains("γιατρ")) {
                bestCategory = categories.find { it.name.contains("Υγεία", ignoreCase = true) || it.name.contains("Φαρμακείο", ignoreCase = true) || it.name.contains("Προσωπικά", ignoreCase = true) }
            } else if (searchTarget.contains("βιβλι") || searchTarget.contains("σχολει") || searchTarget.contains("μαθημ") || searchTarget.contains("φροντιστηρι")) {
                bestCategory = categories.find { it.name.contains("Εκπαίδευση", ignoreCase = true) || it.name.contains("Παιδιά", ignoreCase = true) || it.name.contains("Σχολείο", ignoreCase = true) }
            } else if (searchTarget.contains("φαγητ") || searchTarget.contains("εστιατορι") || searchTarget.contains("ταβερν") || searchTarget.contains("delivery") || searchTarget.contains("σουβλακ")) {
                bestCategory = categories.find { it.name.contains("Φαγητό", ignoreCase = true) || it.name.contains("Έξω", ignoreCase = true) || it.name.contains("Εστιατόριο", ignoreCase = true) }
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

        // Requirement 4: Extract leftover words from originalPrompt as "Περιγραφή" (Description)
        val rawTokens = originalPrompt.trim().split(Regex("[\\s,;!?]+")).filter { it.isNotBlank() }

        val ignoredWords = mutableSetOf<String>()
        ignoredWords.addAll(consumedAmountWords.map { normalizeGreek(it) })
        ignoredWords.addAll(consumedMemberWords.map { normalizeGreek(it) })
        if (matchedCategoryWord != null) {
            matchedCategoryWord.split(Regex("\\s+")).forEach { ignoredWords.add(normalizeGreek(it)) }
        }
        val catNameNorm = normalizeGreek(category.name)
        catNameNorm.split(Regex("\\s+")).forEach { ignoredWords.add(it) }

        // Standard structural words to ignore in description
        val systemWords = setOf(
            "ευρω", "ευρα", "λεπτα", "λεπτο", "cent", "cents", "και", "κι",
            "για", "αφορα", "αφορουν", "τον", "την", "το", "τους", "τις", "τα",
            "στο", "στον", "στην", "στα", "στους", "στις", "σε", "απο", "του", "της", "των",
            "ο", "η", "πληρωσε", "εβαλε", "πληρωμη", "χρεωση",
            "καταχωρησε", "βαλε", "γραψε", "σημειωσε", "περασε", "προσθεσε", "εξοδο", "εξοδα", "νεο",
            "σημερα", "χθες", "χτες", "προχθες", "προχτες"
        )
        ignoredWords.addAll(systemWords)

        for (m in allMembers) {
            getMemberNameVariations(m).forEach { ignoredWords.add(it) }
        }

        val leftoverTokens = mutableListOf<String>()
        val n = rawTokens.size
        for (i in 0 until n) {
            val token = rawTokens[i]
            val clean = token.trim().replace(Regex("[^\\p{L}\\p{Nd}]"), "")
            if (clean.isBlank()) continue
            val normClean = normalizeGreek(clean)

            if (clean.matches(Regex("\\d+([.,]\\d+)?"))) continue

            val isIgnored = ignoredWords.any { it.replace('ς', 'σ') == normClean.replace('ς', 'σ') }
            if (isIgnored) continue

            if (GREEK_WORDS_MAP.containsKey(normClean)) continue

            // Preserve connector "και" / "κι" only if connecting two valid leftover descriptive tokens
            if (normClean == "και" || normClean == "κι") {
                if (leftoverTokens.isNotEmpty() && i + 1 < n) {
                    val nextClean = rawTokens[i + 1].trim().replace(Regex("[^\\p{L}\\p{Nd}]"), "")
                    val nextNorm = normalizeGreek(nextClean)
                    val nextIgnored = ignoredWords.any { it.replace('ς', 'σ') == nextNorm.replace('ς', 'σ') } ||
                            GREEK_WORDS_MAP.containsKey(nextNorm) ||
                            nextClean.matches(Regex("\\d+([.,]\\d+)?"))
                    if (!nextIgnored && nextClean.isNotBlank()) {
                        leftoverTokens.add(clean)
                    }
                }
                continue
            }

            leftoverTokens.add(clean)
        }

        // If leftover tokens exist, they form the spoken custom description!
        val customDescription = if (leftoverTokens.isNotEmpty()) {
            val joined = leftoverTokens.joinToString(" ").trim()
            if (joined.length >= 2) {
                joined.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            } else {
                null
            }
        } else {
            null
        }

        val finalTitle = customDescription
            ?: detectedTitle
            ?: category.name

        return Pair(category, finalTitle)
    }
}
