package com.example

import com.example.data.entity.Expense
import com.example.data.entity.ExpenseCategory
import com.example.util.ExpenseReportExporter
import com.example.util.ReceiptParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testReceiptParserExtraction() {
        val categories = listOf(
            ExpenseCategory(id = 1, name = "Σούπερ μάρκετ", iconKey = "cart", monthlyBudget = 450.0, colorHex = 0xFF059669, voiceKeywords = "σκλαβενίτης, σούπερ"),
            ExpenseCategory(id = 6, name = "Βενζίνη", iconKey = "gas_station", monthlyBudget = 120.0, colorHex = 0xFFEA580C, voiceKeywords = "εκο, βενζίνη")
        )

        // Case 1: Σκλαβενίτης sample
        val sample1 = ReceiptParser.sampleReceipts[0]
        val parsed1 = ReceiptParser.parse(sample1.rawText, categories)
        assertEquals("Σκλαβενίτης", parsed1.storeName)
        assertEquals(42.80, parsed1.amount, 0.01)
        assertEquals("Σούπερ μάρκετ", parsed1.categoryName)

        // Case 2: EKO Βενζίνη sample
        val sample2 = ReceiptParser.sampleReceipts[1]
        val parsed2 = ReceiptParser.parse(sample2.rawText, categories)
        assertEquals("ΕΚΟ Βενζίνη", parsed2.storeName)
        assertEquals(35.00, parsed2.amount, 0.01)
        assertEquals("Βενζίνη", parsed2.categoryName)
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
        // Check for UTF-8 BOM
        assertTrue(csv.startsWith("\uFEFF"))
        assertTrue(csv.contains("Σούπερ μάρκετ"))
        assertTrue(csv.contains("45.50"))
        assertTrue(csv.contains("Μάριος & Βασιλική"))
        assertTrue(csv.contains("50.20 €")) // total amount
    }
}
