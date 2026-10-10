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
    val splitType: String, // "ALL_FAMILY" or "SPECIFIC_MEMBERS"
    val memberIds: List<Long>,
    val memberNamesDisplay: String,
    val splitAmountPerPerson: Double,
    val originalPrompt: String,
    val timestamp: Long = System.currentTimeMillis()
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
        "χιλια" to GreekWordToken(1000.0, NumScale.THOUSAND),
        "χιλιες" to GreekWordToken(1000.0, NumScale.THOUSAND),
        "χιλιαδες" to GreekWordToken(1000.0, NumScale.THOUSAND),

        // Hundreds
        "εννιακοσια" to GreekWordToken(900.0, NumScale.HUNDRED),
        "εννεακοσια" to GreekWordToken(900.0, NumScale.HUNDRED),
        "εννιακοσιες" to GreekWordToken(900.0, NumScale.HUNDRED),
        "οκτακοσια" to GreekWordToken(800.0, NumScale.HUNDRED),
        "οχτακοσια" to GreekWordToken(800.0, NumScale.HUNDRED),
        "οκτακοσιες" to GreekWordToken(800.0, NumScale.HUNDRED),
        "επτακοσια" to GreekWordToken(700.0, NumScale.HUNDRED),
        "εφτακοσια" to GreekWordToken(700.0, NumScale.HUNDRED),
        "επτακοσιες" to GreekWordToken(700.0, NumScale.HUNDRED),
        "εξακοσια" to GreekWordToken(600.0, NumScale.HUNDRED),
        "εξακοσιες" to GreekWordToken(600.0, NumScale.HUNDRED),
        "πεντακοσια" to GreekWordToken(500.0, NumScale.HUNDRED),
        "πεντακοσιες" to GreekWordToken(500.0, NumScale.HUNDRED),
        "τετρακοσια" to GreekWordToken(400.0, NumScale.HUNDRED),
        "τετρακοσιες" to GreekWordToken(400.0, NumScale.HUNDRED),
        "τριακοσια" to GreekWordToken(300.0, NumScale.HUNDRED),
        "τριακοσιες" to GreekWordToken(300.0, NumScale.HUNDRED),
        "διακοσια" to GreekWordToken(200.0, NumScale.HUNDRED),
        "διακοσιες" to GreekWordToken(200.0, NumScale.HUNDRED),
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
        "εικοσιπεντε" to GreekWordToken(25.0, NumScale.TENS),

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

    fun extractAmount(norm: String): Double? {
        val words = norm.split(Regex("\\s+")).map { it.trim('.', ',', ':', ';', '!', '?', '-', '/') }.filter { it.isNotBlank() }

        // 1. Try word-based parsing by analyzing sequences of Greek numbers / digits
        val amountFromTokens = parseAmountFromWordsAndDigits(words)
        if (amountFromTokens != null && amountFromTokens > 0.0) {
            return amountFromTokens
        }

        // 2. Check for explicit decimal pattern e.g. "4.30", "5.30", "12.50", "4.30 ευρω"
        val regexDecimal = Regex("(\\d+\\.\\d{1,2})\\s*(?:ευρω|ευρα)?")
        val decMatch = regexDecimal.find(norm)
        if (decMatch != null) {
            val num = decMatch.groupValues[1].toDoubleOrNull()
            if (num != null && num > 0.0) {
                return num
            }
        }

        // 3. Fallback: Check standalone numbers with / without currency e.g. "15 ευρω", "540"
        val regexDigits = Regex("(\\d+(?:\\.\\d+)?)\\s*(?:ευρω|ευρα)?")
        val matches = regexDigits.findAll(norm).toList()
        for (m in matches) {
            val num = m.groupValues[1].toDoubleOrNull()
            if (num != null && num > 0.0) {
                return num
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
                total += mult * 1000.0
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
            raw in 10.0..99.0 -> raw / 100.0               // 30 -> 0.30, 40 -> 0.40
            else -> 0.0
        }
    }

    private fun parseAmountFromWordsAndDigits(words: List<String>): Double? {
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
                    val asNum = w.toDoubleOrNull()
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
            if (t.type == TokenType.NUMBER && t.original.matches(Regex("^\\d+\\.\\d+$"))) {
                if (t.value > 0.0) return t.value
            }
        }

        // Special case: "μισό ευρώ"
        for (i in tokens.indices) {
            if (tokens[i].original == "μισο" &&
                ((i + 1 < tokens.size && tokens[i + 1].type == TokenType.CURRENCY) ||
                 (i > 0 && tokens[i - 1].type == TokenType.CURRENCY))
            ) {
                return 0.50
            }
        }

        // 2. CURRENCY-based parsing ("ευρω" / "ευρα")
        // Examples: "4 ευρώ και 30 λεπτά", "4 ευρώ 30 λεπτά", "4 ευρώ και 30", "4 ευρώ 30",
        // "τέσσερα ευρώ και τριάντα λεπτά", "πέντε ευρώ και τριάντα", "4 ευρώ"
        val curIndices = tokens.indices.filter { tokens[it].type == TokenType.CURRENCY }
        for (cIdx in curIndices) {
            // Collect euros backwards from cIdx - 1
            var startEuros = cIdx - 1
            while (startEuros >= 0 && tokens[startEuros].type == TokenType.NUMBER) {
                startEuros--
            }
            startEuros++
            val eurosTokens = tokens.subList(startEuros, cIdx)
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

                return euros + cents
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
                return euros + cents
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
                    // If the last token is in 1..99 and cannot be a compound addition with the previous
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
                            return euros + cents
                        }
                    }
                }

                // 5. Cents only: e.g. "30 λεπτά", "πενήντα λεπτά"
                if (hasCentsUnit) {
                    val cents = computeCents(numTokens, hasCentsUnit = true)
                    if (cents > 0.0) {
                        return cents
                    }
                }

                // 6. Simple number: e.g. "πεντακόσια σαράντα", "500"
                val singleNum = computeNumberSequence(numTokens)
                if (singleNum > 0.0) {
                    return singleNum
                }

                idx = endSeq
            } else {
                idx++
            }
        }

        return null
    }

    fun parse(
        prompt: String,
        categories: List<ExpenseCategory>,
        allMembers: List<FamilyMember>
    ): ParsedVoiceExpense {
        val norm = normalizeGreek(prompt)

        // 1. Requirement 8: Date Extraction (e.g. "3 Οκτωβρίου", "χθες", etc.)
        val (detectedTimestamp, cleanedNorm) = extractDate(norm)

        // 2. Requirement 7: Amount Extraction (e.g. "πέντε σαράντα" -> 5.40, "πεντακόσια σαράντα" -> 540)
        val amount = extractAmount(cleanedNorm) ?: extractAmount(norm) ?: 10.0

        // 3. Member Extraction
        val (splitType, matchedMembers, memberDisplay) = extractMembers(cleanedNorm, allMembers)

        // 4. Category & Title Extraction
        val (category, title) = extractCategoryAndTitle(cleanedNorm, prompt, categories)

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
            timestamp = detectedTimestamp
        )
    }

    private fun extractMembers(
        norm: String,
        allMembers: List<FamilyMember>
    ): Triple<String, List<FamilyMember>, String> {
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

        return Triple("ALL_FAMILY", allMembers, "Κοινό Οικογένειας")
    }

    private fun extractCategoryAndTitle(
        norm: String,
        originalPrompt: String,
        categories: List<ExpenseCategory>
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
