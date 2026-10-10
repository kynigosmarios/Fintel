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
        "pets" to "Έξοδα Γατιών",
        "debt" to "Χρωστούμενα",
        "entertainment" to "Διασκέδαση",
        "gift" to "Δώρα",
        "other" to "Άλλα / Διάφορα"
    )
}
