package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.entity.ExpenseCategory
import com.example.data.entity.FamilyMember
import com.example.util.GreekVoiceParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("OikoNomos", appName)
    }

    @Test
    fun `test greek voice parser extraction with Georgia, Melina and new categories`() {
        val members = listOf(
            FamilyMember(id = 1, name = "Μάριος", role = "Γονέας", voiceKeywords = "μάριος, μπαμπάς"),
            FamilyMember(id = 2, name = "Βασιλική", role = "Γονέας", voiceKeywords = "βασιλική, μαμά"),
            FamilyMember(id = 3, name = "Γεωργία", role = "Παιδί", voiceKeywords = "γεωργία, κόρη"),
            FamilyMember(id = 4, name = "Μελίνα", role = "Παιδί", voiceKeywords = "μελίνα, κόρη")
        )

        val categories = listOf(
            ExpenseCategory(id = 1, name = "Σούπερ μάρκετ", iconKey = "cart", monthlyBudget = 450.0, colorHex = 0xFF059669, voiceKeywords = "σούπερ μάρκετ, σουπερμάρκετ, σκλαβενίτης"),
            ExpenseCategory(id = 2, name = "Ψιλικά", iconKey = "kiosk", monthlyBudget = 40.0, colorHex = 0xFF0284C7, voiceKeywords = "ψιλικά, περίπτερο"),
            ExpenseCategory(id = 3, name = "Φαγητό έξω", iconKey = "food", monthlyBudget = 150.0, colorHex = 0xFFEF4444, voiceKeywords = "φαγητό έξω, σουβλάκια, σουβλάκι, πίτσα"),
            ExpenseCategory(id = 4, name = "Καφέδες", iconKey = "coffee", monthlyBudget = 60.0, colorHex = 0xFFD97706, voiceKeywords = "καφέδες, καφές, πρωινό"),
            ExpenseCategory(id = 5, name = "Δραστηριότητες", iconKey = "sports", monthlyBudget = 180.0, colorHex = 0xFF8B5CF6, voiceKeywords = "δραστηριότητες, κολυμβητήριο, αγγλικά"),
            ExpenseCategory(id = 6, name = "Βενζίνη", iconKey = "gas_station", monthlyBudget = 120.0, colorHex = 0xFFEA580C, voiceKeywords = "βενζίνη, καύσιμα"),
            ExpenseCategory(id = 14, name = "Έξοδα γατιών", iconKey = "pets", monthlyBudget = 45.0, colorHex = 0xFF14B8A6, voiceKeywords = "έξοδα γατιών, γάτες, γάτα, γατοτροφή, τροφή")
        )

        // Case 1: «Καφέδες 4 ευρώ και 70 λεπτά, Μάριος Βασιλική»
        val p1 = GreekVoiceParser.parse("Καφέδες 4 ευρώ και 70 λεπτά, Μάριος Βασιλική", categories, members)
        assertEquals(4.70, p1.amount, 0.01)
        assertEquals(2, p1.memberIds.size)
        assertEquals(2.35, p1.splitAmountPerPerson, 0.01)
        assertEquals("Καφέδες", p1.categoryName)

        // Case 2: «Σουβλάκια 12 και 30, Μάριος»
        val p2 = GreekVoiceParser.parse("Σουβλάκια 12 και 30, Μάριος", categories, members)
        assertEquals(12.30, p2.amount, 0.01)
        assertEquals(1, p2.memberIds.size)
        assertEquals(12.30, p2.splitAmountPerPerson, 0.01)
        assertEquals("Φαγητό έξω", p2.categoryName)
        assertEquals("Σουβλάκια", p2.title)

        // Case 3: «Δραστηριότητες 40 ευρώ Γεωργία Μελίνα»
        val p3 = GreekVoiceParser.parse("Δραστηριότητες 40 ευρώ Γεωργία Μελίνα", categories, members)
        assertEquals(40.0, p3.amount, 0.01)
        assertEquals(2, p3.memberIds.size) // Γεωργία & Μελίνα
        assertEquals(20.0, p3.splitAmountPerPerson, 0.01)
        assertEquals("Δραστηριότητες", p3.categoryName)

        // Case 4: «Έξοδα γατιών 18 ευρώ τροφή»
        val p4 = GreekVoiceParser.parse("Έξοδα γατιών 18 ευρώ τροφή", categories, members)
        assertEquals(18.0, p4.amount, 0.01)
        assertEquals("Έξοδα γατιών", p4.categoryName)
        assertEquals("ALL_FAMILY", p4.splitType)
    }
}
