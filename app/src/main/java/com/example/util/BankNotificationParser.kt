package com.example.util

import java.util.Locale
import java.util.UUID

data class PendingBankTransaction(
    val id: String = UUID.randomUUID().toString(),
    val bankName: String,
    val merchantName: String,
    val amount: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val rawText: String = ""
)

object BankNotificationParser {

    private val BANK_PACKAGE_MAP = mapOf(
        "gr.eurobank.mobile" to "Eurobank",
        "gr.alphabank.mobilebanking" to "Alpha Bank",
        "gr.winbank.mobile" to "Τράπεζα Πειραιώς",
        "gr.nbg.mobilebanking" to "Εθνική Τράπεζα",
        "com.revolut.revolut" to "Revolut",
        "gr.cosmote.payzy" to "Payzy",
        "com.viva.wallet" to "Viva Wallet"
    )

    fun isBankPackage(packageName: String): Boolean {
        return BANK_PACKAGE_MAP.containsKey(packageName)
    }

    fun parse(
        packageName: String,
        title: String,
        text: String
    ): PendingBankTransaction? {
        val combined = "$title $text".trim()
        if (combined.isBlank()) return null

        // 1. Check if package matches bank or text contains strong banking keywords
        val detectedBank = BANK_PACKAGE_MAP[packageName] ?: detectBankFromText(combined)
        val hasTransactionKeywords = hasTransactionKeywords(combined)

        if (detectedBank == null && !hasTransactionKeywords) {
            return null
        }

        // 2. Extract amount
        val amount = extractAmount(combined) ?: return null
        if (amount <= 0.05) return null

        // 3. Extract merchant / store
        val merchant = extractMerchant(combined)

        return PendingBankTransaction(
            bankName = detectedBank ?: "Τράπεζα",
            merchantName = merchant.ifBlank { "Τραπεζική Συναλλαγή" },
            amount = amount,
            timestamp = System.currentTimeMillis(),
            rawText = combined
        )
    }

    private fun hasTransactionKeywords(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT)
        val keywords = listOf(
            "αγορά", "αγορα", "συναλλαγή", "συναλλαγη", "χρέωση", "χρεωση",
            "πληρωμή", "πληρωμη", "εγκρίθηκε", "εγκριθηκε", "κάρτα", "καρτα",
            "purchase", "spent", "transaction", "payment", "card charged"
        )
        return keywords.any { lower.contains(it) }
    }

    private fun detectBankFromText(text: String): String? {
        val lower = text.lowercase(Locale.ROOT)
        return when {
            lower.contains("eurobank") -> "Eurobank"
            lower.contains("alpha") -> "Alpha Bank"
            lower.contains("πειραιως") || lower.contains("piraeus") || lower.contains("winbank") -> "Τράπεζα Πειραιώς"
            lower.contains("εθνικη") || lower.contains("nbg") -> "Εθνική Τράπεζα"
            lower.contains("revolut") -> "Revolut"
            lower.contains("payzy") -> "Payzy"
            lower.contains("viva") -> "Viva Wallet"
            else -> null
        }
    }

    private fun extractAmount(text: String): Double? {
        // Match numbers like: 14,50€, 14.50 EUR, 14,50 €, 14.50€
        val amountPatterns = listOf(
            Regex("(?:€|EUR|eur)\\s*([0-9]+[.,][0-9]{2})"),
            Regex("([0-9]+[.,][0-9]{2})\\s*(?:€|EUR|eur|ευρώ|ευρω)"),
            Regex("(?:ποσό|ποσο|amount|sum)[:\\s]+([0-9]+[.,][0-9]{2})", RegexOption.IGNORE_CASE),
            Regex("([0-9]+[.,][0-9]{2})")
        )

        for (pattern in amountPatterns) {
            val match = pattern.find(text)
            if (match != null) {
                val numStr = match.groupValues[1].replace(',', '.')
                val parsed = numStr.toDoubleOrNull()
                if (parsed != null && parsed > 0.05 && parsed < 50000.0) {
                    return parsed
                }
            }
        }
        return null
    }

    private fun extractMerchant(text: String): String {
        // Matches "στο/στην/στη [Merchant]" or "at [Merchant]" or "επιχείρηση [Merchant]"
        val merchantPatterns = listOf(
            Regex("(?:στο κατάστημα|στο|στην|στη|σε)\\s+([a-zA-Zα-ωΑ-Ωά-ώΆ-Ώ0-9\\s&.-]+?)(?:\\s+με|\\s+την|\\s+στις|\\.|,|\\z)", RegexOption.IGNORE_CASE),
            Regex("(?:at|in)\\s+([a-zA-Zα-ωΑ-Ωά-ώΆ-Ώ0-9\\s&.-]+?)(?:\\s+with|\\s+on|\\.|,|\\z)", RegexOption.IGNORE_CASE),
            Regex("(?:επιχείρηση|εμπορος|merchant)[:\\s]+([a-zA-Zα-ωΑ-Ωά-ώΆ-Ώ0-9\\s&.-]+?)(?:\\s+με|\\.|,|\\z)", RegexOption.IGNORE_CASE)
        )

        for (pattern in merchantPatterns) {
            val match = pattern.find(text)
            if (match != null) {
                val candidate = match.groupValues[1].trim()
                if (candidate.length in 2..40 && !candidate.contains("κάρτα", ignoreCase = true) && !candidate.contains("card", ignoreCase = true)) {
                    return candidate
                }
            }
        }
        return ""
    }
}
