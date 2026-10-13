package com.example.util

import android.content.Context
import android.content.Intent
import com.example.data.entity.Expense
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExpenseReportExporter {

    fun generateCsv(expenses: List<Expense>, periodTitle: String = "Όλα τα Έξοδα"): String {
        val sb = StringBuilder()
        // UTF-8 BOM for Microsoft Excel compatibility with Greek letters
        sb.append("\uFEFF")

        val nowSdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val dateSdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val timeSdf = SimpleDateFormat("HH:mm", Locale.getDefault())

        val totalAmount = expenses.sumOf { it.amount }
        val count = expenses.size

        sb.append("ΑΝΑΦΟΡΑ ΕΞΟΔΩΝ FINTEL - $periodTitle\n")
        sb.append("Ημερομηνία εξαγωγής:;${nowSdf.format(Date())}\n")
        sb.append("Σύνολο εξόδων:;${String.format(Locale.US, "%.2f", totalAmount)} €;Πλήθος συναλλαγών:;$count\n\n")

        // CSV Header
        sb.append("Ημερομηνία;Ώρα;Τίτλος;Ποσό (€);Κατηγορία;Ποιος Πλήρωσε;Αφορά Μέλος;Καταμερισμός;Σημειώσεις;Φωνητική Εντολή\n")

        for (exp in expenses) {
            val d = Date(exp.timestamp)
            val dateStr = dateSdf.format(d)
            val timeStr = timeSdf.format(d)
            val titleClean = exp.title.replace(";", ",")
            val amountStr = String.format(Locale.US, "%.2f", exp.amount)
            val catClean = exp.categoryName.replace(";", ",")
            val payerClean = exp.payerMemberName.ifBlank { "Οικογένεια" }.replace(";", ",")
            val membersClean = exp.memberNamesDisplay.replace(";", ",")
            val splitClean = if (exp.splitType == "ALL_FAMILY") "Κοινό Οικογένειας" else "Επιλεγμένα Μέλη"
            val noteClean = exp.note.replace(";", ",").replace("\n", " ")
            val voiceClean = exp.originalVoicePrompt.replace(";", ",").replace("\n", " ")

            sb.append("$dateStr;$timeStr;$titleClean;$amountStr;$catClean;$payerClean;$membersClean;$splitClean;$noteClean;$voiceClean\n")
        }

        return sb.toString()
    }

    fun shareReport(context: Context, expenses: List<Expense>, periodTitle: String = "Όλα τα Έξοδα") {
        val csvContent = generateCsv(expenses, periodTitle)
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_SUBJECT, "Αναφορά Εξόδων Fintel - $periodTitle")
            putExtra(Intent.EXTRA_TEXT, csvContent)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Κοινοποίηση Αναφοράς Εξόδων (Excel / CSV)")
        context.startActivity(shareIntent)
    }
}
