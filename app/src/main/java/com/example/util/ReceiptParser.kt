package com.example.util

import com.example.data.entity.ExpenseCategory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

data class ParsedReceipt(
    val storeName: String,
    val amount: Double,
    val dateDisplay: String,
    val timestamp: Long,
    val categoryId: Long?,
    val categoryName: String?,
    val rawText: String,
    val confidenceNotes: String
)

data class SampleReceipt(
    val title: String,
    val rawText: String,
    val expectedStore: String,
    val expectedAmount: Double
)

object ReceiptParser {

    val sampleReceipts = listOf(
        SampleReceipt(
            title = "🛒 Σκλαβενίτης 42,80€",
            expectedStore = "Σκλαβενίτης",
            expectedAmount = 42.80,
            rawText = """
                Ι. & Σ. ΣΚΛΑΒΕΝΙΤΗΣ Α.Ε.Ε.
                ΥΠΟΚΑΤΑΣΤΗΜΑ: ΜΑΡΟΥΣΙ
                ΗΜ/ΝΙΑ: 02/10/2026  18:42
                
                ΓΑΛΑ ΦΡΕΣΚΟ 1L              1,65
                ΨΩΜΙ ΤΟΣΤ                   1,42
                ΤΥΡΙ ΦΕΤΑ 400G              4,80
                ΕΛΑΙΟΛΑΔΟ 1L                9,90
                ΑΠΟΡΡΥΠΑΝΤΙΚΟ              14,50
                ΦΡΟΥΤΑ & ΛΑΧΑΝΙΚΑ          10,53
                --------------------------------
                ΣΥΝΟΛΟ ΕΥΡΩ                42,80
                ΚΑΡΤΑ VISA                 42,80
                ΕΥΧΑΡΙΣΤΟΥΜΕ ΓΙΑ ΤΗΝ ΠΡΟΤΙΜΗΣΗ ΣΑΣ
            """.trimIndent()
        ),
        SampleReceipt(
            title = "⛽ ΕΚΟ Βενζίνη 35,00€",
            expectedStore = "ΕΚΟ Βενζίνη",
            expectedAmount = 35.00,
            rawText = """
                ΕΛΛΗΝΙΚΑ ΚΑΥΣΙΜΑ ΕΚΟ Α.Β.Ε.Ε.
                ΠΡΑΤΗΡΙΟ ΥΓΡΩΝ ΚΑΥΣΙΜΩΝ
                ΗΜΕΡΟΜΗΝΙΑ: 01/10/2026 09:15
                
                ΑΜΟΛΥΒΔΗ 95 (19.44 LT)      35,00
                --------------------------------
                ΠΛΗΡΩΤΕΟ ΠΟΣΟ:              35,00 €
                ΠΛΗΡΩΜΗ: POS EUROBANK
            """.trimIndent()
        ),
        SampleReceipt(
            title = "☕ Coffee Island 4,60€",
            expectedStore = "Coffee Island",
            expectedAmount = 4.60,
            rawText = """
                COFFEE ISLAND
                ΚΑΤΑΣΤΗΜΑ ΚΗΦΙΣΙΑΣ
                ΗΜ/ΝΙΑ: 03/10/2026 08:30
                
                1 FREDDO ESPRESSO METRIO     2,20
                1 FREDDO CAPPUCCINO GLYKO    2,40
                --------------------------------
                ΤΕΛΙΚΟ ΣΥΝΟΛΟ                4,60 €
                ΜΕΤΡΗΤΑ:                     5,00
                ΡΕΣΤΑ:                       0,40
            """.trimIndent()
        ),
        SampleReceipt(
            title = "🐱 Pet City 22,50€",
            expectedStore = "Pet City",
            expectedAmount = 22.50,
            rawText = """
                PET CITY Α.Ε.Β.Ε.
                ΕΙΔΗ ΚΑΤΟΙΚΙΔΙΩΝ
                ΗΜΕΡΟΜΗΝΙΑ: 29/09/2026
                
                ΞΗΡΑ ΤΡΟΦΗ ΓΑΤΑΣ 2KG        16,50
                ΚΟΝΣΕΡΒΑ GOURMET x4          6,00
                --------------------------------
                ΣΥΝΟΛΙΚΗ ΑΞΙΑ:              22,50 €
                ΕΥΧΑΡΙΣΤΟΥΜΕ!
            """.trimIndent()
        )
    )

    fun parse(rawText: String, categories: List<ExpenseCategory>): ParsedReceipt {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }

        // 1. Detect Store Name
        var storeName = detectStoreName(lines)
        if (storeName.isBlank()) {
            storeName = lines.firstOrNull { it.length in 3..35 && !it.contains(Regex("[0-9]")) } ?: "Αγορά απόδειξης"
        }

        // 2. Detect Amount
        val amount = detectTotalAmount(lines)

        // 3. Detect Date
        val (dateDisplay, timestamp) = detectDate(rawText)

        // 4. Map to best matching Category
        val matchedCategory = findBestCategory(storeName, rawText, categories)

        val notes = "Αυτόματη εξαγωγή από απόδειξη ($storeName)"

        return ParsedReceipt(
            storeName = storeName,
            amount = amount,
            dateDisplay = dateDisplay,
            timestamp = timestamp,
            categoryId = matchedCategory?.id,
            categoryName = matchedCategory?.name,
            rawText = rawText,
            confidenceNotes = notes
        )
    }

    private fun detectStoreName(lines: List<String>): String {
        val lowerText = lines.joinToString(" ").lowercase()
        return when {
            lowerText.contains("σκλαβενιτη") || lowerText.contains("σκλαβενίτη") -> "Σκλαβενίτης"
            lowerText.contains("βασιλοπουλο") || lowerText.contains("βασιλόπουλο") || lowerText.contains("ab ") -> "ΑΒ Βασιλόπουλος"
            lowerText.contains("lidl") || lowerText.contains("λιντλ") -> "Lidl"
            lowerText.contains("my market") || lowerText.contains("μαρκετ") -> "My Market"
            lowerText.contains("μασουτη") || lowerText.contains("μασούτη") -> "Μασούτης"
            lowerText.contains("εκο") || lowerText.contains("eko") -> "ΕΚΟ Βενζίνη"
            lowerText.contains("shell") -> "Shell Βενζίνη"
            lowerText.contains("bp ") -> "BP Βενζίνη"
            lowerText.contains("avin") -> "Avin Βενζίνη"
            lowerText.contains("coffee island") -> "Coffee Island"
            lowerText.contains("mikel") -> "Mikel Coffee"
            lowerText.contains("everest") -> "Everest"
            lowerText.contains("γρηγορη") || lowerText.contains("γρηγόρη") -> "Γρηγόρης"
            lowerText.contains("pet city") || lowerText.contains("γατ") -> "Pet City"
            lowerText.contains("φαρμακειο") || lowerText.contains("φαρμακείο") -> "Φαρμακείο"
            else -> lines.firstOrNull { it.length > 2 } ?: "Κατάστημα"
        }
    }

    private fun detectTotalAmount(lines: List<String>): Double {
        // High priority: lines containing ΣΥΝΟΛΟ, TOTAL, ΠΛΗΡΩΤΕΟ, ΑΞΙΑ, ΕΥΡΩ
        val totalKeywords = listOf("συνολο", "total", "πληρωτεο", "αξια", "τελικο", "ποσο", "ευρω")

        for (line in lines.reversed()) {
            val lower = line.lowercase()
            if (totalKeywords.any { lower.contains(it) }) {
                val found = extractHighestAmountFromLine(line)
                if (found > 0.0) return found
            }
        }

        // Fallback: look through all lines for max reasonable price
        var maxAmount = 0.0
        for (line in lines) {
            val amount = extractHighestAmountFromLine(line)
            if (amount in 0.50..5000.0 && amount > maxAmount) {
                maxAmount = amount
            }
        }

        return if (maxAmount > 0.0) maxAmount else 15.00
    }

    private fun extractHighestAmountFromLine(line: String): Double {
        val pattern = Pattern.compile("(\\d+)[.,](\\d{2})")
        val matcher = pattern.matcher(line)
        var max = 0.0
        while (matcher.find()) {
            val intPart = matcher.group(1) ?: "0"
            val decPart = matcher.group(2) ?: "00"
            val value = "$intPart.$decPart".toDoubleOrNull() ?: 0.0
            if (value > max) {
                max = value
            }
        }
        return max
    }

    private fun detectDate(text: String): Pair<String, Long> {
        val datePattern = Pattern.compile("(\\d{1,2})[/.-](\\d{1,2})[/.-](\\d{2,4})")
        val matcher = datePattern.matcher(text)
        if (matcher.find()) {
            val day = matcher.group(1)?.padStart(2, '0')
            val month = matcher.group(2)?.padStart(2, '0')
            var year = matcher.group(3)
            if (year != null && year.length == 2) {
                year = "20$year"
            }
            val formatted = "$day/$month/$year"
            try {
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val parsed = sdf.parse(formatted)
                if (parsed != null) {
                    return Pair(formatted, parsed.time)
                }
            } catch (e: Exception) {
                // Ignore
            }
        }

        val nowSdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return Pair(nowSdf.format(Date()), System.currentTimeMillis())
    }

    private fun findBestCategory(storeName: String, rawText: String, categories: List<ExpenseCategory>): ExpenseCategory? {
        val lowerStore = storeName.lowercase()
        val lowerFull = rawText.lowercase()

        // Direct matching by keywords
        for (cat in categories) {
            val keywords = cat.voiceKeywords.split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }
            if (keywords.any { lowerStore.contains(it) || lowerFull.contains(it) }) {
                return cat
            }
            if (lowerStore.contains(cat.name.lowercase())) {
                return cat
            }
        }

        // Default heuristic
        return when {
            lowerStore.contains("σκλαβενιτη") || lowerStore.contains("βασιλοπουλο") || lowerStore.contains("lidl") ->
                categories.find { it.name.contains("Σούπερ") }
            lowerStore.contains("eko") || lowerStore.contains("shell") || lowerStore.contains("βενζιν") ->
                categories.find { it.name.contains("Βενζίνη") }
            lowerStore.contains("coffee") || lowerStore.contains("mikel") ->
                categories.find { it.name.contains("Καφέδες") }
            lowerStore.contains("pet") || lowerStore.contains("γατ") ->
                categories.find { it.name.contains("γατιών") || it.name.contains("Κατοικίδια") }
            lowerStore.contains("φαρμακειο") ->
                categories.find { it.name.contains("Υγεία") || it.name.contains("Φαρμακείο") }
            else -> categories.firstOrNull()
        }
    }
}
