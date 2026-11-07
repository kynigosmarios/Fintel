package com.example.util

import com.example.data.entity.ExpenseCategory
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

    // Known popular brands and merchant keywords mapping to their canonical name & category domain
    data class KnownBrand(
        val canonicalName: String,
        val domain: String, // "SUPERMARKET", "HOME", "FOOD", "COFFEE", "FUEL", "CLOTHING", "BILLS", "ENTERTAINMENT", "HEALTH", "PETS"
        val aliases: List<String>
    )

    private val KNOWN_BRANDS = listOf(
        // Supermarket / Groceries
        KnownBrand("Lidl", "SUPERMARKET", listOf("lidl", "λιντλ")),
        KnownBrand("Σκλαβενίτης", "SUPERMARKET", listOf("σκλαβενιτης", "σκλαβενίτης", "sklavenitis")),
        KnownBrand("ΑΒ Βασιλόπουλος", "SUPERMARKET", listOf("βασιλοπουλος", "βασιλόπουλος", "ab vassilopoulos", "ab basilopoulos", "ab market")),
        KnownBrand("Μασούτης", "SUPERMARKET", listOf("μασουτης", "μασούτης", "masoutis")),
        KnownBrand("My Market", "SUPERMARKET", listOf("my market", "mymarket")),
        KnownBrand("Γαλαξίας", "SUPERMARKET", listOf("γαλαξιας", "γαλαξίας", "galaxias")),
        KnownBrand("Market In", "SUPERMARKET", listOf("market in", "marketin")),
        KnownBrand("Κρητικός", "SUPERMARKET", listOf("κρητικος", "κρητικός", "kritikos")),
        KnownBrand("Bazaar", "SUPERMARKET", listOf("bazaar")),

        // Home / Furniture / Electronics
        KnownBrand("IKEA", "HOME", listOf("ikea", "ικεα")),
        KnownBrand("Leroy Merlin", "HOME", listOf("leroy merlin", "leroymerlin", "λεροι μερλιν")),
        KnownBrand("Praktiker", "HOME", listOf("praktiker", "πρακτικερ")),
        KnownBrand("Jysk", "HOME", listOf("jysk", "γιουσκ")),
        KnownBrand("Zara Home", "HOME", listOf("zara home")),
        KnownBrand("Vicko", "HOME", listOf("vicko", "βικο")),
        KnownBrand("Κωτσόβολος", "HOME", listOf("κωτσοβολος", "κωτσόβολος", "kotsovolos")),
        KnownBrand("Πλαίσιο", "HOME", listOf("πλαισιο", "πλαίσιο", "plaisio")),
        KnownBrand("Public", "HOME", listOf("public", "παμπλικ")),
        KnownBrand("Media Markt", "HOME", listOf("media markt", "mediamarkt")),

        // Food / Dining / Delivery
        KnownBrand("Wolt", "FOOD", listOf("wolt", "βολτ")),
        KnownBrand("e-food", "FOOD", listOf("e-food", "efood", "ιφουντ")),
        KnownBrand("Box", "FOOD", listOf("box delivery")),
        KnownBrand("McDonald's", "FOOD", listOf("mcdonalds", "mcdonald's", "μακντοναλντς")),
        KnownBrand("Goody's", "FOOD", listOf("goodys", "goody's", "γκουντις")),
        KnownBrand("Everest", "FOOD", listOf("everest", "εβερεστ")),
        KnownBrand("Γρηγόρης", "FOOD", listOf("γρηγορης", "γρηγόρης", "gregorys")),
        KnownBrand("Pizza Fan", "FOOD", listOf("pizza fan", "pizzafan")),
        KnownBrand("Domino's", "FOOD", listOf("dominos", "domino's")),
        KnownBrand("KFC", "FOOD", listOf("kfc")),

        // Coffee
        KnownBrand("Coffee Island", "COFFEE", listOf("coffee island")),
        KnownBrand("Mikel", "COFFEE", listOf("mikel", "μαικελ")),
        KnownBrand("Starbucks", "COFFEE", listOf("starbucks", "σταρμπακς")),

        // Fuel & Transport
        KnownBrand("Shell", "FUEL", listOf("shell", "σελ")),
        KnownBrand("EKO", "FUEL", listOf("eko", "εκο")),
        KnownBrand("BP", "FUEL", listOf("bp")),
        KnownBrand("Avin", "FUEL", listOf("avin", "αβιν")),
        KnownBrand("Revoil", "FUEL", listOf("revoil")),
        KnownBrand("Elin", "FUEL", listOf("elin", "ελιν")),
        KnownBrand("Αττική Οδός", "FUEL", listOf("αττικη οδος", "αττική οδός", "attiki odos")),

        // Clothing & Shopping
        KnownBrand("Zara", "CLOTHING", listOf("zara", "ζαρα")),
        KnownBrand("H&M", "CLOTHING", listOf("h&m", "hm", "η&μ")),
        KnownBrand("Pull&Bear", "CLOTHING", listOf("pull&bear", "pull and bear")),
        KnownBrand("Bershka", "CLOTHING", listOf("bershka")),
        KnownBrand("Stradivarius", "CLOTHING", listOf("stradivarius")),
        KnownBrand("Massimo Dutti", "CLOTHING", listOf("massimo dutti")),
        KnownBrand("Nike", "CLOTHING", listOf("nike", "ναικι")),
        KnownBrand("Adidas", "CLOTHING", listOf("adidas", "αντιντας")),
        KnownBrand("Intersport", "CLOTHING", listOf("intersport")),
        KnownBrand("Cosmos Sport", "CLOTHING", listOf("cosmos sport", "cosmossport")),
        KnownBrand("Sephora", "CLOTHING", listOf("sephora")),
        KnownBrand("Hondos Center", "CLOTHING", listOf("hondos center", "hondos")),

        // Utilities & Subscriptions
        KnownBrand("Cosmote", "BILLS", listOf("cosmote", "κοσμοτε")),
        KnownBrand("Vodafone", "BILLS", listOf("vodafone", "βονταφον")),
        KnownBrand("Nova", "BILLS", listOf("nova", "νοβα")),
        KnownBrand("ΔΕΗ", "BILLS", listOf("δεη", "ppc", "dei")),
        KnownBrand("Protergia", "BILLS", listOf("protergia", "προτεργια")),
        KnownBrand("Elpedison", "BILLS", listOf("elpedison", "ελπεντισον")),
        KnownBrand("Ήρων", "BILLS", listOf("heron", "ηρων", "ήρων")),
        KnownBrand("ΕΥΔΑΠ", "BILLS", listOf("ευδαπ", "eydap")),
        KnownBrand("ΕΥΑΘ", "BILLS", listOf("ευαθ", "eyath")),
        KnownBrand("Netflix", "BILLS", listOf("netflix", "νετφλιξ")),
        KnownBrand("Spotify", "BILLS", listOf("spotify")),
        KnownBrand("Apple", "BILLS", listOf("apple", "itunes")),
        KnownBrand("Google", "BILLS", listOf("google play", "google")),

        // Pets
        KnownBrand("Pet City", "PETS", listOf("pet city", "petcity"))
    )

    fun isBankPackage(packageName: String): Boolean {
        return ALLOWED_BANK_PACKAGES.containsKey(packageName)
    }

    fun parse(
        packageName: String,
        title: String,
        text: String
    ): PendingBankTransaction? {
        val bankName = ALLOWED_BANK_PACKAGES[packageName] ?: return null

        val combined = "$title $text".trim()
        if (combined.isBlank()) return null

        val lower = combined.lowercase(Locale.ROOT)

        // Reject incoming funds/credits so only actual expenses/debits are tracked
        val incomingKeywords = listOf(
            "λάβατε", "λαβατε", "κατατέθηκαν", "κατατεθηκαν", "κατάθεση", "καταθεση",
            "πίστωση", "πιστωση", "πιστώθηκε", "πιστωθηκε", "πληρωθήκατε", "πληρωθηκατε",
            "received", "credited", "deposit", "incoming payment", "salary"
        )
        if (incomingKeywords.any { lower.contains(it) }) {
            return null
        }

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

        // Extract transaction amount (ignoring any number after "υπόλοιπο" or "balance")
        val amount = extractAmount(combined) ?: return null
        if (amount <= 0.05) return null

        // Extract merchant / recipient / brand
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
            "money", "debit", "approved", "€", "eur", "ευρώ", "ευρω"
        )
        return keywords.any { lower.contains(it) }
    }

    /**
     * Requirement 3: Do NOT consider as expense any number that is after the word
     * "υπόλοιπο", "Υπόλοιπο", or "balance".
     * Any number that appears after "υπόλοιπο" or "balance" (up to end of sentence or clause)
     * is completely eliminated so it can never be parsed as an expense.
     */
    private fun removeBalanceClauses(text: String): String {
        // Matches "υπόλοιπο" or "balance" followed by any characters until a number, and that number
        // Examples: "υπόλοιπο της κάρτας σας είναι 350.00 EUR", "Balance: €450.00", "διαθέσιμο υπόλοιπο: 120.50€"
        var clean = text.replace(
            Regex("(?i)(?:νέο\\s+|διαθέσιμο\\s+|λογιστικό\\s+)?(?:υπόλοιπο|υπολοιπο|υπολοίπου|υπολοιπου|balance|bal\\.)[^.,;!\\n]*?[0-9]+(?:[.,][0-9]{1,2})?\\s*(?:€|EUR|eur|ευρώ|ευρω)?"),
            " "
        )
        // Also if text contains balance and after it there is a number
        clean = clean.replace(
            Regex("(?i)(?:υπόλοιπο|υπολοιπο|balance)[^0-9]*[0-9]+(?:[.,][0-9]{1,2})?\\s*(?:€|EUR|eur|ευρώ|ευρω)?"),
            " "
        )
        return clean
    }

    private fun extractAmount(text: String): Double? {
        val cleaned = removeBalanceClauses(text)

        val amountPatterns = listOf(
            Regex("(?:€|EUR|eur)\\s*([0-9]+(?:[.,][0-9]{1,2})?)"),
            Regex("([0-9]+(?:[.,][0-9]{1,2})?)\\s*(?:€|EUR|eur|ευρώ|ευρω)"),
            Regex("(?i)(?:ποσό|ποσο|amount|sum)[:\\s]+([0-9]+(?:[.,][0-9]{1,2})?)"),
            Regex("([0-9]+[.,][0-9]{2})")
        )

        for (pattern in amountPatterns) {
            val match = pattern.find(cleaned)
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
        val lower = text.lowercase(Locale.ROOT)

        // 1. Pattern-based extraction from notification phrasing
        val merchantPatterns = listOf(
            Regex("(?:στο κατάστημα|στο|στην|στη|σε)\\s+([a-zA-Zα-ωΑ-Ωά-ώΆ-Ώ0-9\\s&.-]+?)(?:\\s+με|\\s+την|\\s+στις|\\.|,|\\z)", RegexOption.IGNORE_CASE),
            Regex("(?:at|in|to)\\s+([a-zA-Zα-ωΑ-Ωά-ώΆ-Ώ0-9\\s&.-]+?)(?:\\s+with|\\s+on|\\.|,|\\z)", RegexOption.IGNORE_CASE),
            Regex("(?:επιχείρηση|εμπορος|merchant|προς)[:\\s]+([a-zA-Zα-ωΑ-Ωά-ώΆ-Ώ0-9\\s&.-]+?)(?:\\s+με|\\.|,|\\z)", RegexOption.IGNORE_CASE),
            Regex("(?:αγορά|αγορα|πληρωμή|πληρωμη|purchase|spent|payment)\\s+(?:ποσού|ποσου|of)?\\s*[0-9.,€eurEUR\\s]+\\s*(?:σε|στο|στην|στη|at|to)?\\s*([a-zA-Zα-ωΑ-Ωά-ώΆ-Ώ0-9\\s&.-]+?)(?:\\s+με|\\s+στις|\\s+την|\\.|,|\\z)", RegexOption.IGNORE_CASE)
        )

        for (pattern in merchantPatterns) {
            val match = pattern.find(text)
            if (match != null) {
                val candidate = match.groupValues[1].trim()
                if (candidate.length in 2..40 &&
                    !candidate.contains("κάρτα", ignoreCase = true) &&
                    !candidate.contains("card", ignoreCase = true) &&
                    !candidate.contains("υπόλοιπο", ignoreCase = true) &&
                    !candidate.contains("balance", ignoreCase = true)
                ) {
                    return candidate
                }
            }
        }

        // 2. Check known popular brands anywhere in text as fallback
        for (brand in KNOWN_BRANDS) {
            for (alias in brand.aliases) {
                val pattern = "(?:^|[^\\p{L}\\p{Nd}])" + Regex.escape(alias) + "(?:$|[^\\p{L}\\p{Nd}])"
                if (Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(lower)) {
                    return brand.canonicalName
                }
            }
        }

        return ""
    }

    /**
     * Requirement 5: Categorize recognized notification merchant / brand into matching ExpenseCategory.
     * If the merchant cannot be categorized, returns null (so description field remains blank).
     */
    fun categorizeMerchant(
        merchant: String,
        rawText: String,
        categories: List<ExpenseCategory>
    ): ExpenseCategory? {
        val search = "$merchant $rawText".lowercase(Locale.ROOT)

        // 1. Check known brand domains first
        val matchedBrand = KNOWN_BRANDS.firstOrNull { brand ->
            search.contains(brand.canonicalName.lowercase(Locale.ROOT)) ||
            brand.aliases.any { alias ->
                val p = "(?:^|[^\\p{L}\\p{Nd}])" + Regex.escape(alias) + "(?:$|[^\\p{L}\\p{Nd}])"
                Regex(p, RegexOption.IGNORE_CASE).containsMatchIn(search)
            }
        }

        if (matchedBrand != null) {
            val targetDomain = matchedBrand.domain
            val found = when (targetDomain) {
                "SUPERMARKET" -> categories.find { it.name.contains("Σούπερ", ignoreCase = true) || it.name.contains("Super", ignoreCase = true) || it.name.contains("Τρόφιμα", ignoreCase = true) }
                "HOME" -> categories.find { it.name.contains("Σπίτι", ignoreCase = true) || it.name.contains("Έπιπλα", ignoreCase = true) || it.name.contains("Home", ignoreCase = true) || it.name.contains("Σπιτιού", ignoreCase = true) }
                "FOOD" -> categories.find { it.name.contains("Φαγητό", ignoreCase = true) || it.name.contains("Food", ignoreCase = true) || it.name.contains("Εστίαση", ignoreCase = true) }
                "COFFEE" -> categories.find { it.name.contains("Καφέ", ignoreCase = true) || it.name.contains("Coffee", ignoreCase = true) }
                    ?: categories.find { it.name.contains("Φαγητό", ignoreCase = true) }
                "FUEL" -> categories.find { it.name.contains("Καύσιμα", ignoreCase = true) || it.name.contains("Μετακινήσεις", ignoreCase = true) || it.name.contains("Βενζίνη", ignoreCase = true) || it.name.contains("Αυτοκίνητο", ignoreCase = true) }
                "CLOTHING" -> categories.find { it.name.contains("Ρούχα", ignoreCase = true) || it.name.contains("Ένδυση", ignoreCase = true) || it.name.contains("Shopping", ignoreCase = true) }
                "BILLS" -> categories.find { it.name.contains("Συνδρομές", ignoreCase = true) || it.name.contains("Τηλέφων", ignoreCase = true) || it.name.contains("Ρεύμα", ignoreCase = true) || it.name.contains("Bills", ignoreCase = true) }
                "PETS" -> categories.find { it.name.contains("Γατι", ignoreCase = true) || it.name.contains("Κατοικίδια", ignoreCase = true) || it.name.contains("Pets", ignoreCase = true) }
                else -> null
            }
            if (found != null) return found
        }

        // 2. Check against custom categories and their voiceKeywords
        for (cat in categories) {
            val kwList = cat.voiceKeywords.split(",").map { it.trim().lowercase(Locale.ROOT) }.filter { it.isNotBlank() }
            val catName = cat.name.trim().lowercase(Locale.ROOT)
            val allKeywords = kwList + catName

            for (kw in allKeywords) {
                if (kw.length >= 3) {
                    val pattern = "(?:^|[^\\p{L}\\p{Nd}])" + Regex.escape(kw) + "(?:$|[^\\p{L}\\p{Nd}])"
                    if (Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(search)) {
                        return cat
                    }
                }
            }
        }

        // Not recognized or cannot be categorized -> null
        return null
    }
}
