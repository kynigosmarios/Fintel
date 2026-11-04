package com.example

import com.example.data.entity.Expense
import com.example.data.entity.FamilyMember
import com.example.data.entity.getPayerShares
import com.example.data.entity.hasMultiplePayers
import com.example.util.ExpenseReportExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testExpenseReportExporterCsvGeneration() {
        val expenses = listOf(
            Expense(
                id = 1,
                title = "Σούπερ μάρκετ",
                amount = 45.50,
                categoryId = 1,
                categoryName = "Σούπερ μάρκετ",
                timestamp = 1727800000000L,
                splitType = "ALL_FAMILY",
                memberNamesDisplay = "Κοινό Οικογένειας"
            ),
            Expense(
                id = 2,
                title = "Καφέδες",
                amount = 4.70,
                categoryId = 4,
                categoryName = "Καφέδες",
                timestamp = 1727805000000L,
                splitType = "SPECIFIC_MEMBERS",
                memberNamesDisplay = "Μάριος & Βασιλική"
            )
        )

        val csv = ExpenseReportExporter.generateCsv(expenses, "Οκτώβριος 2026")
        assertTrue(csv.contains("FINTEL"))
        assertTrue(csv.contains("Σούπερ μάρκετ"))
        assertTrue(csv.contains("45.50"))
        assertTrue(csv.contains("Καφέδες"))
        assertTrue(csv.contains("4.70"))
        assertTrue(csv.contains("50.20")) // Total
    }

    @Test
    fun testGreekVoiceParserAmounts() {
        val parser = com.example.util.GreekVoiceParser

        assertEquals(4.30, parser.extractAmount(parser.normalizeGreek("4 ευρώ και 30 λεπτά"))!!, 0.01)
        assertEquals(4.30, parser.extractAmount(parser.normalizeGreek("4 ευρώ 30 λεπτά"))!!, 0.01)
        assertEquals(4.30, parser.extractAmount(parser.normalizeGreek("4 ευρώ και 30"))!!, 0.01)
        assertEquals(4.30, parser.extractAmount(parser.normalizeGreek("τέσσερα ευρώ και τριάντα λεπτά"))!!, 0.01)
        assertEquals(5.30, parser.extractAmount(parser.normalizeGreek("πέντε τριάντα"))!!, 0.01)
        assertEquals(5.30, parser.extractAmount(parser.normalizeGreek("πέντε και τριάντα"))!!, 0.01)
        assertEquals(5.30, parser.extractAmount(parser.normalizeGreek("5 και 30"))!!, 0.01)
        assertEquals(4.30, parser.extractAmount(parser.normalizeGreek("4 και 30"))!!, 0.01)
        assertEquals(5.30, parser.extractAmount(parser.normalizeGreek("5:30"))!!, 0.01)
        assertEquals(4.30, parser.extractAmount(parser.normalizeGreek("4,30"))!!, 0.01)
        assertEquals(0.30, parser.extractAmount(parser.normalizeGreek("30 λεπτά"))!!, 0.01)
        assertEquals(0.50, parser.extractAmount(parser.normalizeGreek("μισό ευρώ"))!!, 0.01)
        assertEquals(0.50, parser.extractAmount(parser.normalizeGreek("πενήντα λεπτά"))!!, 0.01)
        assertEquals(540.0, parser.extractAmount(parser.normalizeGreek("πεντακόσια σαράντα"))!!, 0.01)
        assertEquals(540.0, parser.extractAmount(parser.normalizeGreek("πεντακόσια σαράντα ευρώ"))!!, 0.01)
        assertEquals(12.50, parser.extractAmount(parser.normalizeGreek("δώδεκα πενήντα"))!!, 0.01)
        assertEquals(400.0, parser.extractAmount(parser.normalizeGreek("τετρακόσια ευρώ"))!!, 0.01)
        assertEquals(377.40, parser.extractAmount(parser.normalizeGreek("τριακόσια εβδομήντα εφτά ευρώ και σαράντα λεπτά"))!!, 0.01)
        assertEquals(14726.70, parser.extractAmount(parser.normalizeGreek("δεκατέσσερις χιλιάδες επτακόσια εικοσιέξι ευρώ και εβδομήντα λεπτά"))!!, 0.01)
    }

    @Test
    fun testPayerBeneficiariesAndDescription() {
        val parser = com.example.util.GreekVoiceParser
        val categories = listOf(
            com.example.data.entity.ExpenseCategory(id = 1, name = "Καφέδες", voiceKeywords = "καφέδες, καφές"),
            com.example.data.entity.ExpenseCategory(id = 2, name = "gadgets", voiceKeywords = "gadgets, τεχνολογία")
        )
        val members = listOf(
            com.example.data.entity.FamilyMember(id = 1, name = "Μάριος", role = "Γονέας"),
            com.example.data.entity.FamilyMember(id = 2, name = "Βασιλική", role = "Γονέας")
        )

        // Case: "Έξι ευρώ καφέδες Μάριος για Μάριο και Βασιλική"
        val p1 = parser.parse("Έξι ευρώ καφέδες Μάριος για Μάριο και Βασιλική", categories, members)
        assertEquals(6.0, p1.amount, 0.01)
        assertEquals(1L, p1.payerMemberId)
        assertEquals("Μάριος", p1.payerMemberName)
        assertEquals(2, p1.memberIds.size)
        assertEquals("Καφέδες", p1.title)

        // Case: "Δεκατέσσερα ευρώ, πληκτρολόγιο, gadgets, Μάριος Για Βασιλική"
        val p2 = parser.parse("Δεκατέσσερα ευρώ, πληκτρολόγιο, gadgets, Μάριος Για Βασιλική", categories, members)
        assertEquals(14.0, p2.amount, 0.01)
        assertEquals(1L, p2.payerMemberId)
        assertEquals("Μάριος", p2.payerMemberName)
        assertEquals(listOf(2L), p2.memberIds)
        assertEquals(2L, p2.categoryId)
        assertEquals("gadgets", p2.categoryName)
        assertEquals("Πληκτρολόγιο", p2.title)

        // User case: "εικοσιέξι ευρώ και εβδομήντα λεπτά, σούπερ μάρκετ, στραγάλια και σταφίδες, Μάριος και Βασιλική για Μάριος Βασιλική"
        val fullCategories = categories + listOf(
            com.example.data.entity.ExpenseCategory(id = 3, name = "Σούπερ μάρκετ", voiceKeywords = "σούπερ μάρκετ, σουπερ μαρκετ, μάρκετ")
        )
        val pUser = parser.parse("εικοσιέξι ευρώ και εβδομήντα λεπτά, σούπερ μάρκετ, στραγάλια και σταφίδες, Μάριος και Βασιλική για Μάριος Βασιλική", fullCategories, members)
        assertEquals(26.70, pUser.amount, 0.01)
        assertEquals(3L, pUser.categoryId)
        assertEquals("Σούπερ μάρκετ", pUser.categoryName)
        assertEquals("Στραγάλια σταφίδες", pUser.title)
        assertEquals("Μάριος & Βασιλική", pUser.payerMemberName)
        assertEquals("1,2", pUser.payerMemberIdsCsv)
        assertEquals(listOf(1L, 2L), pUser.memberIds)
        assertEquals("Μάριος & Βασιλική", pUser.memberNamesDisplay)
    }

    @Test
    fun testMultiPayerSharesCalculation() {
        val members = listOf(
            FamilyMember(id = 1, name = "Μπαμπάς", role = "Γονέας"),
            FamilyMember(id = 2, name = "Μαμά", role = "Γονέας")
        )

        // Equal split test (40 € split equally between 2 members)
        val expEqual = Expense(
            id = 10,
            title = "Παιχνίδι",
            amount = 40.0,
            categoryId = 1,
            categoryName = "Παιδιά",
            splitType = "ALL_FAMILY",
            payerMemberIdsCsv = "1,2",
            payerAmountsCsv = "",
            payerMemberName = "Μπαμπάς & Μαμά"
        )
        val sharesEqual = expEqual.getPayerShares(members)
        assertEquals(2, sharesEqual.size)
        assertEquals(20.0, sharesEqual[0].amount, 0.01)
        assertEquals(20.0, sharesEqual[1].amount, 0.01)
        assertEquals("Μπαμπάς", sharesEqual[0].memberName)
        assertEquals("Μαμά", sharesEqual[1].memberName)

        // Custom split test (40 € with 30 € from dad and 10 € from mom)
        val expCustom = Expense(
            id = 11,
            title = "Ρούχα παιδιού",
            amount = 40.0,
            categoryId = 1,
            categoryName = "Παιδιά",
            splitType = "SPECIFIC_MEMBERS",
            payerMemberIdsCsv = "1,2",
            payerAmountsCsv = "1:30.0,2:10.0",
            payerMemberName = "Μπαμπάς (30 €) & Μαμά (10 €)"
        )
        val sharesCustom = expCustom.getPayerShares(members)
        assertEquals(2, sharesCustom.size)
        assertEquals(30.0, sharesCustom[0].amount, 0.01)
        assertEquals(10.0, sharesCustom[1].amount, 0.01)
        assertTrue(expCustom.hasMultiplePayers())

        // Single payer backward-compatibility test
        val expSingle = Expense(
            id = 12,
            title = "Βενζίνη",
            amount = 50.0,
            categoryId = 2,
            categoryName = "Μετακινήσεις",
            splitType = "ALL_FAMILY",
            payerMemberId = 1,
            payerMemberName = "Μπαμπάς"
        )
        val sharesSingle = expSingle.getPayerShares(members)
        assertEquals(1, sharesSingle.size)
        assertEquals(50.0, sharesSingle[0].amount, 0.01)
        assertEquals("Μπαμπάς", sharesSingle[0].memberName)
    }

    @Test
    fun testSettleUpCalculator() {
        val marios = FamilyMember(id = 1, name = "Μάριος", role = "Γονέας")
        val vasiliki = FamilyMember(id = 2, name = "Βασιλική", role = "Γονέας")
        val members = listOf(marios, vasiliki)

        // Marios pays 100€ for an ALL_FAMILY expense (shared 50-50)
        val exp1 = Expense(
            id = 1,
            title = "Σούπερ Μάρκετ",
            amount = 100.0,
            categoryId = 1,
            categoryName = "Σούπερ Μάρκετ",
            splitType = "ALL_FAMILY",
            payerMemberId = 1,
            payerMemberName = "Μάριος"
        )

        // Vasiliki pays 20€ for an ALL_FAMILY expense (shared 50-50)
        val exp2 = Expense(
            id = 2,
            title = "Φούρνος",
            amount = 20.0,
            categoryId = 1,
            categoryName = "Φούρνος",
            splitType = "ALL_FAMILY",
            payerMemberId = 2,
            payerMemberName = "Βασιλική"
        )

        val result = com.example.util.SettleUpCalculator.calculate(listOf(exp1, exp2), members)
        assertEquals(120.0, result.totalHouseholdExpenses, 0.01)

        val mariosSummary = result.memberSummaries.find { it.member.id == 1L }!!
        val vasilikiSummary = result.memberSummaries.find { it.member.id == 2L }!!

        // Total expenses 120€, each person's fair share is 60€
        // Marios paid 100€ -> net = +40€
        // Vasiliki paid 20€ -> net = -40€
        assertEquals(100.0, mariosSummary.totalPaid, 0.01)
        assertEquals(60.0, mariosSummary.totalShare, 0.01)
        assertEquals(40.0, mariosSummary.netBalance, 0.01)

        assertEquals(20.0, vasilikiSummary.totalPaid, 0.01)
        assertEquals(60.0, vasilikiSummary.totalShare, 0.01)
        assertEquals(-40.0, vasilikiSummary.netBalance, 0.01)

        // Settlement transfer: Vasiliki -> Marios 40€
        assertEquals(1, result.transfers.size)
        assertEquals(2L, result.transfers[0].fromMember.id)
        assertEquals(1L, result.transfers[0].toMember.id)
        assertEquals(40.0, result.transfers[0].amount, 0.01)

        val summaryText = com.example.util.SettleUpCalculator.formatSummaryText(result, "Σπίτι", true)
        assertTrue(summaryText.contains("Βασιλική ➜ Μάριος"))
        assertTrue(summaryText.contains("40,00 €"))
    }

    @Test
    fun testReceiptParserOcrAndQr() {
        // Greek supermarket receipt OCR mock
        val sampleReceiptText = """
            ΣΚΛΑΒΕΝΙΤΗΣ
            Ι. & Σ. ΣΚΛΑΒΕΝΙΤΗΣ Α.Ε.Ε.
            ΑΦΜ: 094056252
            ΗΜ/ΝΙΑ: 14/03/2025  12:45
            ΓΑΛΑ ΦΡΕΣΚΟ          1,85
            ΨΩΜΙ ΤΟΣΤ            2,10
            ΤΥΡΙ ΦΕΤΑ            7,50
            ΣΥΝΟΛΟ ΕΥΡΩ         11,45
            ΚΑΡΤΑ VISA          11,45
        """.trimIndent()

        val parsedOcr = com.example.util.ReceiptParser.parseOcrText(sampleReceiptText)
        assertEquals("Σκλαβενιτης", parsedOcr.title)
        assertEquals(11.45, parsedOcr.amount, 0.01)
        assertEquals("Σούπερ Μάρκετ", parsedOcr.suggestedCategoryName)

        // AADE QR code string
        val aadeQr = "https://mydatainfo.aade.gr/receipt?tin=094056252&amount=24.80&date=20250314"
        val parsedQr = com.example.util.ReceiptParser.parseAadeQr(aadeQr)
        assertEquals(24.80, parsedQr.amount, 0.01)
        assertEquals("094056252", parsedQr.merchantAfm)
    }

    @Test
    fun testBankNotificationParser() {
        val parser = com.example.util.BankNotificationParser

        // Eurobank notification
        val eurobankTx = parser.parse(
            packageName = "gr.eurobank.mobile",
            title = "Eurobank Cards",
            text = "Συναλλαγή 14,50€ με κάρτα στο SKLAVENITIS"
        )
        assertTrue(eurobankTx != null)
        assertEquals(14.50, eurobankTx!!.amount, 0.01)
        assertEquals("SKLAVENITIS", eurobankTx.merchantName)
        assertEquals("Eurobank", eurobankTx.bankName)

        // Piraeus Bank notification
        val piraeusTx = parser.parse(
            packageName = "gr.winbank.mobile",
            title = "winbank",
            text = "Αγορά 25,00€ με την κάρτα σας στο κατάστημα LIDL"
        )
        assertTrue(piraeusTx != null)
        assertEquals(25.00, piraeusTx!!.amount, 0.01)
        assertEquals("LIDL", piraeusTx.merchantName)
        assertEquals("Τράπεζα Πειραιώς", piraeusTx.bankName)

        // Revolut notification
        val revolutTx = parser.parse(
            packageName = "com.revolut.revolut",
            title = "Revolut",
            text = "Ξοδέψατε 4,80 € στο Coffee Island"
        )
        assertTrue(revolutTx != null)
        assertEquals(4.80, revolutTx!!.amount, 0.01)
        assertEquals("Coffee Island", revolutTx.merchantName)
        assertEquals("Revolut", revolutTx.bankName)
    }
}
