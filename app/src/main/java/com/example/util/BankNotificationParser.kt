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

    /**
     * Strictly whitelisted banking apps:
     * - Eurobank
     * - Alpha Bank
     * - Εθνική Τράπεζα (NBG)
     * - Τράπεζα Πειραιώς (Piraeus bank)
     * - Revolut
     * - Payzy
     * - Viber pay
     * - PayPal
     * - Snappi
     */
    private val ALLOWED_BANK_PACKAGES = mapOf(
        // Eurobank
        "gr.eurobank.mobile" to "Eurobank",
        "gr.eurobank.cards" to "Eurobank",
        // Alpha Bank
        "gr.alphabank.mobilebanking" to "Alpha Bank",
        "gr.alpha.bonusextra" to "Alpha Bank",
        // NBG (Εθνική Τράπεζα)
        "gr.nbg.mobilebanking" to "Εθνική Τράπεζα",
        // Piraeus (Τράπεζα Πειραιώς)
        "gr.winbank.mobile" to "Τράπεζα Πειραιώς",
        "gr.piraeusbank.winbank" to "Τράπεζα Πειραιώς",
        // Revolut
        "com.revolut.revolut" to "Revolut",
        // Payzy
        "gr.cosmote.payzy" to "Payzy",
        // Viber Pay
        "com.viber.voip" to "Viber Pay",
        // PayPal
        "com.paypal.android.p2pmobile" to "PayPal",
        // Snappi
        "eu.snappi.app" to "Snappi",
        "gr.snappi.mobile" to "Snappi"
    )

    fun isBankPackage(packageName: String): Boolean {
        return ALLOWED_BANK_PACKAGES.containsKey(packageName)
    }

    fun parse(
        packageName: String,
        title: String,
        text: String
    ): PendingBankTransaction? {
        // STRICT RULE 1.2: Reject completely if the package is NOT in the allowed bank packages list.
        // This ensures emails (Gmail, Outlook, Yahoo) and other non-bank apps NEVER trigger bank transactions.
        val bankName = ALLOWED_BANK_PACKAGES[packageName] ?: return null

        val combined = "$title $text".trim()
        if (combined.isBlank()) return null

        val lower = combined.lowercase(Locale.ROOT)

        // For Viber Pay, verify it's an actual payment/financial transaction notification and not a normal chat
        if (packageName == "com.viber.voip") {
            val hasViberPayKeywords = listOf(
                "viber pay", "pay", "πληρωμή", "πληρωμη", "συναλλαγή", "συναλλαγη",
                "χρέωση", "χρεωση", "μεταφορά", "μεταφορα", "payment", "money", "ευρώ", "ευρω", "€"
            ).any { lower.contains(it) }
            if (!hasViberPayKeywords) return null
        }

        // Must have transaction-related keywords or financial indicators
        val hasTransactionKeywords = hasTransactionKeywords(combined)
        if (!hasTransactionKeywords && packageName != "com.revolut.revolut" && packageName != "com.paypal.android.p2pmobile") {
            return null
        }

        // Extract transaction amount
        val amount = extractAmount(combined) ?: return null
        if (amount <= 0.05) return null

        // Extract merchant / recipient
        val merchant = extractMerchant(combined)

        return PendingBankTransaction(
            bankName = bankName,
            merchantName = merchant.ifBlank { "Συναλλαγή $bankName" },
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
            "έξοδο", "εξοδο", "μεταφορά", "μεταφορα",
            "purchase", "spent", "transaction", "payment", "card charged", "paid", "sent",
            "received", "money", "debit", "approved"
        )
        return keywords.any { lower.contains(it) }
    }

    private fun extractAmount(text: String): Double? {
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
        val merchantPatterns = listOf(
            Regex("(?:στο κατάστημα|στο|στην|στη|σε)\\s+([a-zA-Zα-ωΑ-Ωά-ώΆ-Ώ0-9\\s&.-]+?)(?:\\s+με|\\s+την|\\s+στις|\\.|,|\\z)", RegexOption.IGNORE_CASE),
            Regex("(?:at|in|to)\\s+([a-zA-Zα-ωΑ-Ωά-ώΆ-Ώ0-9\\s&.-]+?)(?:\\s+with|\\s+on|\\.|,|\\z)", RegexOption.IGNORE_CASE),
            Regex("(?:επιχείρηση|εμπορος|merchant|προς)[:\\s]+([a-zA-Zα-ωΑ-Ωά-ώΆ-Ώ0-9\\s&.-]+?)(?:\\s+με|\\.|,|\\z)", RegexOption.IGNORE_CASE)
        )

        for (pattern in merchantPatterns) {
            val match = pattern.find(text)
            if (match != null) {
                val candidate = match.groupValues[1].trim()
                if (candidate.length in 2..40 &&
                    !candidate.contains("κάρτα", ignoreCase = true) &&
                    !candidate.contains("card", ignoreCase = true)
                ) {
                    return candidate
                }
            }
        }
        return ""
    }
}
