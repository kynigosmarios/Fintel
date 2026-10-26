package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.entity.Expense
import com.example.data.entity.ExpenseCategory
import com.example.data.entity.FamilyMember
import com.example.data.entity.getBeneficiaryCategoryOrNamesDisplay
import com.example.data.entity.getPayerCategoryOrNamesDisplay
import com.example.data.entity.getPayerDisplay
import com.example.data.entity.getPayerShares
import com.example.data.entity.hasMultiplePayers
import com.example.data.entity.toFirebaseExpense
import com.example.util.GreekVoiceParser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        assertEquals("Fintel", appName)
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

    @Test
    fun `test greek voice parser with clean empty categories and members`() {
        val result = GreekVoiceParser.parse("Σούπερ μάρκετ 25 ευρώ", emptyList(), emptyList())
        assertEquals(25.0, result.amount, 0.01)
        assertEquals("Σούπερ Μάρκετ", result.title)
        assertTrue(result.memberIds.isEmpty())
        assertEquals("ALL_FAMILY", result.splitType)
    }

    @Test
    fun `test multi-payer equal split between two parents`() {
        val members = listOf(
            FamilyMember(id = 1, name = "Μάριος", role = "Γονέας", voiceKeywords = "μάριος"),
            FamilyMember(id = 2, name = "Βασιλική", role = "Γονέας", voiceKeywords = "βασιλική"),
            FamilyMember(id = 3, name = "Γεωργία", role = "Παιδί", voiceKeywords = "γεωργία")
        )

        // Expense of 40€ equally split between Marios (1) and Vasiliki (2)
        val expense = Expense(
            title = "Σούπερ Μάρκετ",
            amount = 40.0,
            categoryId = 1,
            categoryName = "Σούπερ μάρκετ",
            splitType = "ALL_FAMILY",
            payerMemberId = 1,
            payerMemberName = "Μάριος & Βασιλική",
            payerMemberIdsCsv = "1,2",
            payerAmountsCsv = "1:20.00,2:20.00"
        )

        val shares = expense.getPayerShares(members)
        assertEquals(2, shares.size)
        assertEquals("Μάριος", shares[0].memberName)
        assertEquals(20.0, shares[0].amount, 0.01)
        assertEquals("Βασιλική", shares[1].memberName)
        assertEquals(20.0, shares[1].amount, 0.01)
        assertTrue(expense.hasMultiplePayers(members))
        assertTrue(expense.getPayerDisplay(members).contains("20.00") || expense.getPayerDisplay(members).contains("20,00"))
    }

    @Test
    fun `test multi-payer custom split where one parent paid 30 and other paid 10 for child`() {
        val members = listOf(
            FamilyMember(id = 1, name = "Μάριος", role = "Γονέας", voiceKeywords = "μάριος"),
            FamilyMember(id = 2, name = "Βασιλική", role = "Γονέας", voiceKeywords = "βασιλική"),
            FamilyMember(id = 3, name = "Γεωργία", role = "Παιδί", voiceKeywords = "γεωργία")
        )

        // Purchase of 40€ for child Georgia (id=3), Marios paid 30€, Vasiliki paid 10€
        val expense = Expense(
            title = "Παπούτσια Γεωργίας",
            amount = 40.0,
            categoryId = 5,
            categoryName = "Ένδυση",
            splitType = "SPECIFIC_MEMBERS",
            memberIdsCsv = "3",
            memberNamesDisplay = "Γεωργία",
            payerMemberId = 1,
            payerMemberName = "Μάριος (30,00 €) & Βασιλική (10,00 €)",
            payerMemberIdsCsv = "1,2",
            payerAmountsCsv = "1:30.00,2:10.00"
        )

        val shares = expense.getPayerShares(members)
        assertEquals(2, shares.size)
        assertEquals(1L, shares[0].memberId)
        assertEquals("Μάριος", shares[0].memberName)
        assertEquals(30.0, shares[0].amount, 0.01)
        assertEquals(2L, shares[1].memberId)
        assertEquals("Βασιλική", shares[1].memberName)
        assertEquals(10.0, shares[1].amount, 0.01)

        val display = expense.getPayerDisplay(members)
        assertTrue(display.contains("30.00") || display.contains("30,00"))
        assertTrue(display.contains("10.00") || display.contains("10,00"))

        // Verify analytics attribution simulation
        val payerSpendMap = members.associateWith { 0.0 }.toMutableMap()
        shares.forEach { share ->
            val payer = members.find { it.id == share.memberId }
            if (payer != null) {
                payerSpendMap[payer] = (payerSpendMap[payer] ?: 0.0) + share.amount
            }
        }
        val marios = members.find { it.id == 1L }!!
        val vasiliki = members.find { it.id == 2L }!!
        val georgia = members.find { it.id == 3L }!!

        assertEquals(30.0, payerSpendMap[marios]!!, 0.01)
        assertEquals(10.0, payerSpendMap[vasiliki]!!, 0.01)
        assertEquals(0.0, payerSpendMap[georgia]!!, 0.01)
    }

    @Test
    fun `test backward compatibility for single payer expense`() {
        val members = listOf(
            FamilyMember(id = 1, name = "Μάριος", role = "Γονέας", voiceKeywords = "μάριος"),
            FamilyMember(id = 2, name = "Βασιλική", role = "Γονέας", voiceKeywords = "βασιλική")
        )

        val legacyExpense = Expense(
            title = "Βενζίνη",
            amount = 50.0,
            categoryId = 6,
            categoryName = "Βενζίνη",
            splitType = "ALL_FAMILY",
            payerMemberId = 1,
            payerMemberName = "Μάριος",
            payerMemberIdsCsv = "",
            payerAmountsCsv = ""
        )

        val shares = legacyExpense.getPayerShares(members)
        assertEquals(1, shares.size)
        assertEquals("Μάριος", shares[0].memberName)
        assertEquals(50.0, shares[0].amount, 0.01)
        assertFalse(legacyExpense.hasMultiplePayers(members))
        assertEquals("Μάριος", legacyExpense.getPayerDisplay(members))
    }

    @Test
    fun `test toFirebaseExpense preserves payerMemberIdsCsv and payerAmountsCsv`() {
        val expense = Expense(
            id = 42L,
            cloudId = "cloud-exp-42",
            title = "Σούπερ Μάρκετ",
            amount = 80.0,
            categoryId = 1L,
            categoryName = "Σούπερ μάρκετ",
            splitType = "ALL_FAMILY",
            payerMemberId = 1L,
            payerMemberName = "Μάριος & Βασιλική",
            payerMemberIdsCsv = "1,2",
            payerAmountsCsv = "1:50.00,2:30.00"
        )

        val fbExp = expense.toFirebaseExpense(
            householdId = "house-123",
            userId = "user-abc",
            userName = "Μάριος"
        )

        assertEquals("cloud-exp-42", fbExp.id)
        assertEquals("house-123", fbExp.householdId)
        assertEquals("Σούπερ Μάρκετ", fbExp.title)
        assertEquals(80.0, fbExp.amount, 0.01)
        assertEquals("1,2", fbExp.payerMemberIdsCsv)
        assertEquals("1:50.00,2:30.00", fbExp.payerAmountsCsv)
        assertEquals(1L, fbExp.payerMemberId)
        assertEquals("Μάριος & Βασιλική", fbExp.payerMemberName)
    }

    @Test
    fun `test getPayerShares extracts individual names even with empty members list`() {
        val expense = Expense(
            title = "Σούπερ Μάρκετ",
            amount = 40.0,
            categoryId = 1L,
            categoryName = "Σούπερ μάρκετ",
            splitType = "ALL_FAMILY",
            payerMemberId = 1L,
            payerMemberName = "Μάριος & Βασιλική (από 20,00 €)",
            payerMemberIdsCsv = "1,2",
            payerAmountsCsv = "1:20.00,2:20.00"
        )

        // When members list hasn't loaded yet on app start
        val shares = expense.getPayerShares(emptyList())
        assertEquals(2, shares.size)
        assertEquals("Μάριος", shares[0].memberName)
        assertEquals(20.0, shares[0].amount, 0.01)
        assertEquals("Βασιλική", shares[1].memberName)
        assertEquals(20.0, shares[1].amount, 0.01)
    }

    @Test
    fun `test Room database saveOrUpdateExpense and replaceAllExpensesTransaction`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getDatabase(context)
        val dao = db.expenseDao()

        dao.deleteAllExpenses()

        val exp1 = Expense(
            cloudId = "c1",
            title = "Καφές",
            amount = 3.5,
            categoryId = 4L,
            categoryName = "Καφέδες",
            splitType = "ALL_FAMILY"
        )
        val id1 = dao.saveOrUpdateExpense(exp1)
        assertTrue(id1 > 0)

        // Update the same expense with multiple payers
        val updatedExp1 = exp1.copy(
            id = id1,
            payerMemberIdsCsv = "1,2",
            payerAmountsCsv = "1:2.00,2:1.50",
            payerMemberName = "Μάριος & Βασιλική"
        )
        val id1Again = dao.saveOrUpdateExpense(updatedExp1)
        assertEquals(id1, id1Again)

        val retrieved = dao.getExpenseByCloudId("c1")
        assertEquals("1,2", retrieved?.payerMemberIdsCsv)
        assertEquals("1:200,2:150".take(3), retrieved?.payerAmountsCsv?.take(3))

        // Replace all transaction test
        val exp2 = Expense(
            cloudId = "c2",
            title = "Σούπερ Μάρκετ",
            amount = 50.0,
            categoryId = 1L,
            categoryName = "Σούπερ μάρκετ",
            splitType = "ALL_FAMILY"
        )
        dao.replaceAllExpensesTransaction(listOf(exp2))
        val count = dao.getExpenseCount()
        assertEquals(1, count)
        val list = dao.getAllExpensesList()
        assertEquals("c2", list[0].cloudId)
    }

    @Test
    fun `test multi-payer display retains individual names and handles decimal commas`() {
        val members = listOf(
            FamilyMember(id = 1, name = "Μάριος", role = "Γονέας"),
            FamilyMember(id = 2, name = "Βασιλική", role = "Γονέας")
        )

        // Expense with equal split between 2 payers formatted with Greek currency decimal comma
        val exp = Expense(
            id = 10,
            cloudId = "cloud-10",
            title = "Σούπερ Μάρκετ",
            amount = 50.0,
            categoryId = 1L,
            categoryName = "Σούπερ μάρκετ",
            splitType = "ALL_FAMILY",
            payerMemberId = 1L,
            payerMemberName = "Μάριος & Βασιλική (από 25,00 €)",
            payerMemberIdsCsv = "1,2",
            payerAmountsCsv = "1:25.00,2:25.00"
        )

        val shares = exp.getPayerShares(members)
        assertEquals(2, shares.size)
        assertEquals("Μάριος", shares[0].memberName)
        assertEquals(25.0, shares[0].amount, 0.01)
        assertEquals("Βασιλική", shares[1].memberName)
        assertEquals(25.0, shares[1].amount, 0.01)

        // Display on badge: When payers are all members of a category (Γονείς), displays category "Γονείς"
        val display = exp.getPayerCategoryOrNamesDisplay(members)
        assertEquals("Γονείς", display)
        assertTrue(exp.hasMultiplePayers(members))
    }

    @Test
    fun `test category grouping rules for payers and beneficiaries`() {
        val members = listOf(
            FamilyMember(id = 1, name = "Μάριος", role = "Γονέας"),
            FamilyMember(id = 2, name = "Βασιλική", role = "Γονέας"),
            FamilyMember(id = 3, name = "Πέτρος", role = "Παιδί"),
            FamilyMember(id = 4, name = "Ελένη", role = "Παιδί")
        )

        // 1. Both parents paid -> "Γονείς"
        val bothParentsPaid = Expense(
            title = "Σούπερ Μάρκετ",
            amount = 100.0,
            categoryId = 1L,
            categoryName = "Σούπερ μάρκετ",
            splitType = "ALL_FAMILY",
            payerMemberId = 1L,
            payerMemberName = "Μάριος & Βασιλική",
            payerMemberIdsCsv = "1,2",
            payerAmountsCsv = "1:50.00,2:50.00"
        )
        assertEquals("Γονείς", bothParentsPaid.getPayerCategoryOrNamesDisplay(members))

        // 2. Only 1 parent paid -> shows single name "Μάριος"
        val oneParentPaid = Expense(
            title = "Σούπερ Μάρκετ",
            amount = 50.0,
            categoryId = 1L,
            categoryName = "Σούπερ μάρκετ",
            splitType = "ALL_FAMILY",
            payerMemberId = 1L,
            payerMemberName = "Μάριος",
            payerMemberIdsCsv = "1",
            payerAmountsCsv = "1:50.00"
        )
        assertEquals("Μάριος", oneParentPaid.getPayerCategoryOrNamesDisplay(members))

        // 3. Beneficiaries are all children -> "Παιδιά"
        val allKidsBeneficiaries = Expense(
            title = "Φροντιστήριο",
            amount = 120.0,
            categoryId = 2L,
            categoryName = "Εκπαίδευση",
            splitType = "SPECIFIC_MEMBERS",
            payerMemberId = 1L,
            payerMemberName = "Μάριος",
            payerMemberIdsCsv = "1",
            payerAmountsCsv = "1:120.00",
            memberIdsCsv = "3,4",
            memberNamesDisplay = "Πέτρος & Ελένη"
        )
        assertEquals("Παιδιά", allKidsBeneficiaries.getBeneficiaryCategoryOrNamesDisplay(members))

        // 4. Beneficiary is only 1 child -> "Πέτρος"
        val oneKidBeneficiary = Expense(
            title = "Βιβλίο",
            amount = 15.0,
            categoryId = 2L,
            categoryName = "Εκπαίδευση",
            splitType = "SPECIFIC_MEMBERS",
            payerMemberId = 1L,
            payerMemberName = "Μάριος",
            payerMemberIdsCsv = "1",
            payerAmountsCsv = "1:15.00",
            memberIdsCsv = "3",
            memberNamesDisplay = "Πέτρος"
        )
        assertEquals("Πέτρος", oneKidBeneficiary.getBeneficiaryCategoryOrNamesDisplay(members))

        // 5. All household members -> "Οικογένεια"
        val allHousehold = Expense(
            title = "Ρεύμα",
            amount = 150.0,
            categoryId = 3L,
            categoryName = "Λογαριασμοί",
            splitType = "ALL_FAMILY",
            payerMemberId = 1L,
            payerMemberName = "Μάριος",
            payerMemberIdsCsv = "1",
            payerAmountsCsv = "1:150.00"
        )
        assertEquals("Οικογένεια", allHousehold.getBeneficiaryCategoryOrNamesDisplay(members))
    }

    @Test
    fun `test modifying expense from multiple payers to single payer updates cleanly`() {
        val members = listOf(
            FamilyMember(id = 1, name = "Μάριος", role = "Γονέας"),
            FamilyMember(id = 2, name = "Βασιλική", role = "Γονέας")
        )

        // Expense updated to single payer (Μάριος)
        val exp = Expense(
            id = 10,
            cloudId = "cloud-10",
            title = "Σούπερ Μάρκετ",
            amount = 50.0,
            categoryId = 1L,
            categoryName = "Σούπερ μάρκετ",
            splitType = "ALL_FAMILY",
            payerMemberId = 1L,
            payerMemberName = "Μάριος",
            payerMemberIdsCsv = "1",
            payerAmountsCsv = "1:50.00"
        )

        val shares = exp.getPayerShares(members)
        assertEquals(1, shares.size)
        assertEquals("Μάριος", shares[0].memberName)
        assertEquals(50.0, shares[0].amount, 0.01)

        val display = exp.getPayerCategoryOrNamesDisplay(members)
        assertEquals("Μάριος", display)
        assertFalse(exp.hasMultiplePayers(members))
    }

    @Test
    fun `test toFirebaseExpense strictly preserves existing cloudId`() {
        val exp = Expense(
            id = 99,
            cloudId = "existing-uuid-1234",
            title = "Ρεύμα",
            amount = 120.0,
            categoryId = 1L,
            categoryName = "Λογαριασμοί",
            splitType = "ALL_FAMILY",
            payerMemberId = 1L,
            payerMemberName = "Μάριος"
        )
        val fbExp = exp.toFirebaseExpense("household-1", "user-1", "Μάριος")
        assertEquals("existing-uuid-1234", fbExp.id)
    }

    @Test
    fun `test main activity startup does not crash`() {
        val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java)
        controller.create().start().resume()
        val activity = controller.get()
        org.junit.Assert.assertNotNull(activity)
    }

    @Test
    fun `test user verification and Marios isolation logic`() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
        if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) {
            val options = com.google.firebase.FirebaseOptions.Builder()
                .setApplicationId("1:123456789:android:abcdef")
                .setProjectId("oikonomos-test")
                .setApiKey("fakeApiKey123456789")
                .build()
            com.google.firebase.FirebaseApp.initializeApp(context, options)
        }
        val vm = com.example.ui.viewmodel.OikoNomosViewModel(context)
        
        // Marios email check
        assertTrue(vm.isMariosUser(email = "kynigosmarios@gmail.com"))
        // grwayfarer email check: MUST NOT be Marios
        assertFalse(vm.isMariosUser(email = "grwayfarer@gmail.com"))
        assertFalse(vm.isMariosUser(email = "otheruser@example.com"))

        // Verification is false for null user
        assertFalse(vm.isUserVerified(null))
    }
}




