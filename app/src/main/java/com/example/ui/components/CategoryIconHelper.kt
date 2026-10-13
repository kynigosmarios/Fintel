package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.util.GreekVoiceParser

object CategoryIconHelper {
    fun getIcon(key: String): ImageVector {
        return when (key.lowercase()) {
            "cart" -> Icons.Default.ShoppingCart
            "kiosk" -> Icons.Default.Storefront
            "food" -> Icons.Default.Restaurant
            "coffee" -> Icons.Default.LocalCafe
            "sports" -> Icons.Default.FitnessCenter
            "gas_station", "car" -> Icons.Default.LocalGasStation
            "medical" -> Icons.Default.LocalHospital
            "house" -> Icons.Default.Home
            "electric" -> Icons.Default.Bolt
            "gas" -> Icons.Default.Whatshot
            "apartment" -> Icons.Default.Apartment
            "loan" -> Icons.Default.AccountBalance
            "car_payment" -> Icons.Default.DirectionsCar
            "pets" -> Icons.Default.Pets
            "debt" -> Icons.Default.Receipt
            "entertainment" -> Icons.Default.Celebration
            "gift" -> Icons.Default.CardGiftcard
            "bill" -> Icons.AutoMirrored.Filled.ReceiptLong
            "school" -> Icons.Default.School
            "travel", "flight" -> Icons.Default.Flight
            "tech", "phone" -> Icons.Default.Smartphone
            "work" -> Icons.Default.Work
            "beauty", "care" -> Icons.Default.Face
            "star" -> Icons.Default.Star
            else -> Icons.Default.Category
        }
    }

    fun getIconForCategoryName(categoryName: String, iconKey: String? = null): ImageVector {
        if (!iconKey.isNullOrBlank() && iconKey != "other") {
            return getIcon(iconKey)
        }
        val norm = GreekVoiceParser.normalizeGreek(categoryName.trim().lowercase())
        return when {
            norm.contains("σουπερ") || norm.contains("supermarket") || norm.contains("μαρκετ") -> getIcon("cart")
            norm.contains("φαγητο") || norm.contains("food") || norm.contains("ταβερν") || norm.contains("σουβλακ") || norm.contains("πιτσ") -> getIcon("food")
            norm.contains("καφ") || norm.contains("coffee") -> getIcon("coffee")
            norm.contains("βενζιν") || norm.contains("καυσιμ") || norm.contains("διοδι") -> getIcon("gas_station")
            norm.contains("λογαριασμ") || norm.contains("σπιτι") || norm.contains("ενοικι") || norm.contains("δεη") || norm.contains("ρευμα") || norm.contains("νερο") -> getIcon("house")
            norm.contains("υγεια") || norm.contains("φαρμακ") || norm.contains("γιατρ") -> getIcon("medical")
            norm.contains("διασκεδασ") || norm.contains("ψυχαγωγ") || norm.contains("ποτο") || norm.contains("σινεμα") -> getIcon("entertainment")
            norm.contains("σχολει") || norm.contains("φροντιστηρ") || norm.contains("μαθημα") -> getIcon("school")
            norm.contains("ταξιδ") || norm.contains("διακοπ") || norm.contains("ξενοδοχει") -> getIcon("flight")
            else -> getIcon(iconKey ?: "other")
        }
    }

    fun getColorForCategoryName(categoryName: String): Color {
        val norm = GreekVoiceParser.normalizeGreek(categoryName.trim().lowercase())
        return when {
            norm.contains("σουπερ") || norm.contains("supermarket") || norm.contains("μαρκετ") -> Color(0xFF059669)
            norm.contains("φαγητο") || norm.contains("food") || norm.contains("καφ") -> Color(0xFFEF4444)
            norm.contains("λογαριασμ") || norm.contains("σπιτι") || norm.contains("ενοικι") -> Color(0xFF0D9488)
            norm.contains("βενζιν") || norm.contains("καυσιμ") || norm.contains("μετακινησ") -> Color(0xFFEA580C)
            norm.contains("υγεια") || norm.contains("φαρμακ") -> Color(0xFFEC4899)
            norm.contains("διασκεδασ") || norm.contains("ψυχαγωγ") -> Color(0xFF8B5CF6)
            else -> Color(0xFF059669)
        }
    }

    val availableIcons = listOf(
        "cart" to "Σούπερ Μάρκετ",
        "kiosk" to "Ψιλικά / Περίπτερο",
        "food" to "Φαγητό έξω / Delivery",
        "coffee" to "Καφέδες / Πρωινό",
        "sports" to "Δραστηριότητες",
        "gas_station" to "Βενζίνη / Καύσιμα",
        "medical" to "Γιατρός - Φαρμακείο",
        "house" to "Ενοίκιο / Σπίτι",
        "electric" to "Ρεύμα / ΔΕΗ",
        "gas" to "Αέριο / Θέρμανση",
        "apartment" to "Κοινόχρηστα",
        "loan" to "Δάνεια / Τράπεζα",
        "car_payment" to "Δόση Αυτοκινήτου",
        "pets" to "Έξοδα Γατιών / Κατοικίδια",
        "debt" to "Χρωστούμενα",
        "entertainment" to "Διασκέδαση",
        "gift" to "Δώρα",
        "school" to "Σχολείο / Εκπαίδευση",
        "travel" to "Ταξίδια / Διακοπές",
        "tech" to "Τεχνολογία / Κινητό",
        "work" to "Επαγγελματικά / Γραφείο",
        "beauty" to "Περιποίηση / Κομμωτήριο",
        "star" to "Ειδικά / Προσωπικά",
        "other" to "Άλλα / Διάφορα"
    )

    // Curated category badge / border colors
    val availableCategoryColors = listOf(
        0xFF059669L to "Σμαραγδί (Emerald)",
        0xFF0D9488L to "Τιρκουάζ (Teal)",
        0xFF0284C7L to "Μπλε του Ουρανού (Sky)",
        0xFF2563EBL to "Βαθύ Μπλε (Royal Blue)",
        0xFF6366F1L to "Indigo / Μπλε-Μοβ",
        0xFF8B5CF6L to "Μωβ (Purple)",
        0xFFC026D3L to "Φούξια (Fuchsia)",
        0xFFEC4899L to "Ροζ (Pink)",
        0xFFEF4444L to "Κόκκινο (Red)",
        0xFFEA580CL to "Πορτοκαλί (Orange)",
        0xFFD97706L to "Κεχριμπαρένιο (Amber)",
        0xFF475569L to "Σκούρο Γκρι (Slate)",
        0xFF10B981L to "Πράσινο Μέντας (Mint)",
        0xFF1E293BL to "Midnight Navy"
    )

    // Curated font colors for ledger items in this category
    val availableFontColors = listOf(
        0L to "Προεπιλογή Συστήματος (Adaptive)",
        0xFF0F172AL to "Μαύρο / Σκούρο Ανθρακί",
        0xFF1E3A8AL to "Βαθύ Μπλε (Navy)",
        0xFF065F46L to "Βαθύ Σμαραγδί (Emerald)",
        0xFF991B1BL to "Βαθύ Κόκκινο (Crimson)",
        0xFF581C87L to "Βαθύ Μωβ (Deep Purple)",
        0xFF9A3412L to "Βαθύ Πορτοκαλί / Κανελί",
        0xFF134E4AL to "Σκούρο Τιρκουάζ (Deep Teal)",
        0xFF831843L to "Βαθύ Ροζ / Μπορντό",
        0xFF334155L to "Σκούρο Σχιστόλιθο (Slate)",
        0xFF047857L to "Πράσινο Δάσους",
        0xFF1D4ED8L to "Ηλεκτρικό Μπλε"
    )
}
