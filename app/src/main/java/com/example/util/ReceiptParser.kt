package com.example.util

import com.example.data.entity.ExpenseCategory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

data class ParsedReceiptData(
    val title: String,
    val amount: Double,
    val timestamp: Long,
    val suggestedCategoryName: String,
    val merchantAfm: String = "",
    val rawExtractedText: String = ""
)

object ReceiptParser {

    private val GREEK_KNOWN_MERCHANTS = mapOf(
        "ΣΚΛΑΒΕΝΙΤΗΣ" to "Σούπερ Μάρκετ",
        "SKLAVENITIS" to "Σούπερ Μάρκετ",
        "ΑΒ ΒΑΣΙΛΟΠΟΥΛΟΣ" to "Σούπερ Μάρκετ",
        "AB VASSILOPOULOS" to "Σούπερ Μάρκετ",
        "LIDL" to "Σούπερ Μάρκετ",
        "MY MARKET" to "Σούπερ Μάρκετ",
        "ΜΑΣΟΥΤΗΣ" to "Σούπερ Μάρκετ",
        "MASOUTIS" to "Σούπερ Μάρκετ",
        "ΓΑΛΑΞΙΑΣ" to "Σούπερ Μάρκετ",
        "MARKET IN" to "Σούπερ Μάρκετ",
        "BAZAAR" to "Σούπερ Μάρκετ",
        "ΚΡΗΤΙΚΟΣ" to "Σούπερ Μάρκετ",
        "EFOOD" to "Φαγητό & Delivery",
        "WOLT" to "Φαγητό & Delivery",
        "BOX" to "Φαγητό & Delivery",
        "EVEREST" to "Καφές & Έξω",
        "GRIGORIS" to "Καφές & Έξω",
        "ΓΡΗΓΟΡΗΣ" to "Καφές & Έξω",
        "COFFEE ISLAND" to "Καφές & Έξω",
        "SHELL" to "Μετακινήσεις",
        "BP" to "Μετακινήσεις",
        "EKO" to "Μετακινήσεις",
        "AVIN" to "Μετακινήσεις",
        "REVOIL" to "Μετακινήσεις",
        "ΚΩΤΣΟΒΟΛΟΣ" to "Σπίτι & Ηλεκτρικά",
        "KOTSOVOLOS" to "Σπίτι & Ηλεκτρικά",
        "PUBLIC" to "Αγορές & Τεχνολογία",
        "PLAISIO" to "Αγορές & Τεχνολογία",
        "ΠΛΑΙΣΙΟ" to "Αγορές & Τεχνολογία",
        "IKEA" to "Σπίτι & Εξοπλισμός",
        "JUMBO" to "Παιδιά & Σπίτι",
        "ZARA" to "Ένδυση & Υπόδηση",
        "H&M" to "Ένδυση & Υπόδηση",
        "BERSHKA" to "Ένδυση & Υπόδηση",
        "STRADIVARIUS" to "Ένδυση & Υπόδηση",
        "PULL&BEAR" to "Ένδυση & Υπόδηση",
        "COSMOTE" to "Λογαριασμοί & Τηλεφωνία",
        "VODAFONE" to "Λογαριασμοί & Τηλεφωνία",
        "NOVA" to "Λογαριασμοί & Τηλεφωνία",
        "ΔΕΗ" to "Λογαριασμοί & Ρεύμα",
        "DEI" to "Λογαριασμοί & Ρεύμα",
        "ΕΥΔΑΠ" to "Λογαριασμοί & Νερό",
        "EYDAP" to "Λογαριασμοί & Νερό",
        "ΦΑΡΜΑΚΕΙΟ" to "Υγεία & Φαρμακείο",
        "PHARMACY" to "Υγεία & Φαρμακείο"
    )

    fun parseOcrText(
        text: String,
        availableCategories: List<ExpenseCategory> = emptyList()
    ): ParsedReceiptData {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) {
            return ParsedReceiptData(
                title = "Απόδειξη",
                amount = 0.0,
                timestamp = System.currentTimeMillis(),
                suggestedCategoryName = defaultCategory(availableCategories)
            )
        }

        // 1. Detect store name & suggested category
        var merchant = ""
        var suggestedCategory = ""

        val upperText = text.uppercase(Locale.ROOT)
        for ((knownStore, cat) in GREEK_KNOWN_MERCHANTS) {
            if (upperText.contains(knownStore)) {
                merchant = knownStore.lowercase().replaceFirstChar { it.titlecase() }
                suggestedCategory = cat
                break
            }
        }

        if (merchant.isBlank()) {
            // Pick candidate from the first 4 non-numeric lines
            for (line in lines.take(4)) {
                val cleaned = line.replace(Regex("[^a-zA-Zα-ωΑ-Ωά-ώΆ-Ώ0-9\\s&.-]"), "").trim()
                if (cleaned.length >= 3 && !cleaned.any { it.isDigit() } &&
                    !cleaned.contains("ΑΦΜ", ignoreCase = true) &&
                    !cleaned.contains("ΔΟΥ", ignoreCase = true) &&
                    !cleaned.contains("ΤΗΛ", ignoreCase = true)
                ) {
                    merchant = cleaned
                    break
                }
            }
        }

        if (merchant.isBlank()) {
            merchant = "Αγορά απόδειξης"
        }

        // 2. Extract Amount
        var amount = extractTotalAmount(lines)

        // 3. Extract Date
        val timestamp = extractDate(text) ?: System.currentTimeMillis()

        // 4. Resolve category with availableCategories
        if (suggestedCategory.isBlank()) {
            suggestedCategory = matchCategoryByName(merchant, availableCategories)
        } else {
            // Find existing category matching suggested
            val matched = availableCategories.find {
                it.name.contains(suggestedCategory, ignoreCase = true) ||
                suggestedCategory.contains(it.name, ignoreCase = true)
            }
            if (matched != null) {
                suggestedCategory = matched.name
            }
        }

        if (suggestedCategory.isBlank()) {
            suggestedCategory = defaultCategory(availableCategories)
        }

        return ParsedReceiptData(
            title = merchant,
            amount = amount,
            timestamp = timestamp,
            suggestedCategoryName = suggestedCategory,
            rawExtractedText = text
        )
    }

    fun parseAadeQr(
        qrText: String,
        availableCategories: List<ExpenseCategory> = emptyList()
    ): ParsedReceiptData {
        // Typical Greek AADE QR formats:
        // 1) URL format: https://mydatainfo.aade.gr/... or https://www.aade.gr/...
        // Query params or path often contains amount, afm, date
        var amount = 0.0
        var merchantAfm = ""
        var timestamp = System.currentTimeMillis()

        // Match amount in QR string
        val amountRegex = Regex("(?:amount|poso|total|sum|val)=([0-9]+(?:[.,][0-9]{1,2})?)", RegexOption.IGNORE_CASE)
        val amountMatch = amountRegex.find(qrText)
        if (amountMatch != null) {
            val raw = amountMatch.groupValues[1].replace(',', '.')
            amount = raw.toDoubleOrNull() ?: 0.0
        }

        // Match AFM in QR string
        val afmRegex = Regex("(?:tin|afm|vat)=([0-9]{9})", RegexOption.IGNORE_CASE)
        val afmMatch = afmRegex.find(qrText)
        if (afmMatch != null) {
            merchantAfm = afmMatch.groupValues[1]
        }

        // Match date in QR string (e.g. 20250312 or 2025-03-12)
        val dateRegex = Regex("(?:date|d)=([0-9]{4}[-]?[0-9]{2}[-]?[0-9]{2})", RegexOption.IGNORE_CASE)
        val dateMatch = dateRegex.find(qrText)
        if (dateMatch != null) {
            val dStr = dateMatch.groupValues[1].replace("-", "")
            if (dStr.length == 8) {
                try {
                    val sdf = SimpleDateFormat("yyyyMMdd", Locale.ROOT)
                    timestamp = sdf.parse(dStr)?.time ?: System.currentTimeMillis()
                } catch (_: Exception) {}
            }
        }

        // If no URL param, scan the raw QR text with OCR heuristics
        if (amount == 0.0) {
            val ocrParsed = parseOcrText(qrText, availableCategories)
            amount = ocrParsed.amount
            if (ocrParsed.merchantAfm.isNotBlank()) merchantAfm = ocrParsed.merchantAfm
        }

        val title = if (merchantAfm.isNotBlank()) "Απόδειξη (ΑΦΜ $merchantAfm)" else "Απόδειξη QR"

        return ParsedReceiptData(
            title = title,
            amount = amount,
            timestamp = timestamp,
            suggestedCategoryName = defaultCategory(availableCategories),
            merchantAfm = merchantAfm,
            rawExtractedText = qrText
        )
    }

    private fun extractTotalAmount(lines: List<String>): Double {
        val totalKeywords = listOf(
            "ΣΥΝΟΛΟ", "ΠΛΗΡΩΤΕΟ", "ΣΥΝΟΛΟ ΕΥΡΩ", "TOTAL", "POS", "ΚΑΡΤΑ", "VISA", "MASTERCARD",
            "ΤΕΛΙΚΟ", "ΚΑΘΑΡΟ", "ΕΥΡΩ", "EUR", "EURO", "€"
        )

        val priceRegex = Regex("([0-9]+[.,][0-9]{2})")

        // 1. Search lines from bottom up that contain a total keyword
        for (i in lines.indices.reversed()) {
            val line = lines[i]
            val upper = line.uppercase(Locale.ROOT)
            val hasKeyword = totalKeywords.any { upper.contains(it) }

            if (hasKeyword) {
                // Find numbers on this line
                val matches = priceRegex.findAll(line).toList()
                if (matches.isNotEmpty()) {
                    val lastVal = matches.last().value.replace(',', '.').toDoubleOrNull()
                    if (lastVal != null && lastVal > 0.0) {
                        return lastVal
                    }
                }
                // Check next line if exists
                if (i + 1 < lines.size) {
                    val nextLine = lines[i + 1]
                    val nextMatches = priceRegex.findAll(nextLine).toList()
                    if (nextMatches.isNotEmpty()) {
                        val v = nextMatches.first().value.replace(',', '.').toDoubleOrNull()
                        if (v != null && v > 0.0) {
                            return v
                        }
                    }
                }
            }
        }

        // 2. Fallback: Search all matches across all lines and take maximum plausible amount (< 5000)
        val allPrices = mutableListOf<Double>()
        for (line in lines) {
            for (match in priceRegex.findAll(line)) {
                val v = match.value.replace(',', '.').toDoubleOrNull()
                if (v != null && v > 0.10 && v < 5000.0) {
                    allPrices.add(v)
                }
            }
        }

        return allPrices.maxOrNull() ?: 0.0
    }

    private fun extractDate(text: String): Long? {
        val datePatterns = listOf(
            Pattern.compile("([0-3]?[0-9])[/.\\-]([0-1]?[0-9])[/.\\-](20[2-3][0-9])"),
            Pattern.compile("([0-3]?[0-9])[/.\\-]([0-1]?[0-9])[/.\\-]([2-3][0-9])")
        )

        for (pattern in datePatterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val day = matcher.group(1)?.toIntOrNull() ?: continue
                val month = matcher.group(2)?.toIntOrNull() ?: continue
                var year = matcher.group(3)?.toIntOrNull() ?: continue
                if (year < 100) year += 2000

                if (day in 1..31 && month in 1..12 && year in 2020..2030) {
                    val cal = Calendar.getInstance()
                    cal.set(Calendar.YEAR, year)
                    cal.set(Calendar.MONTH, month - 1)
                    cal.set(Calendar.DAY_OF_MONTH, day)
                    return cal.timeInMillis
                }
            }
        }
        return null
    }

    private fun matchCategoryByName(
        storeName: String,
        categories: List<ExpenseCategory>
    ): String {
        val norm = storeName.lowercase(Locale.ROOT)
        for (cat in categories) {
            val catNorm = cat.name.lowercase(Locale.ROOT)
            if (norm.contains(catNorm) || catNorm.contains(norm)) {
                return cat.name
            }
        }
        return ""
    }

    private fun defaultCategory(categories: List<ExpenseCategory>): String {
        return categories.firstOrNull {
            it.name.contains("Σούπερ", ignoreCase = true) ||
            it.name.contains("Super", ignoreCase = true)
        }?.name ?: categories.firstOrNull()?.name ?: "Σούπερ Μάρκετ"
    }
}
