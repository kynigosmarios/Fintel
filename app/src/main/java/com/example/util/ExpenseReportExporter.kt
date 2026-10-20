package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.entity.Expense
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExpenseReportExporter {
    private const val TAG = "ExpenseReportExporter"

    fun generateCsv(expenses: List<Expense>, periodTitle: String = "Όλα τα Έξοδα"): String {
        val totalAmount = expenses.sumOf { it.amount }
        val sb = StringBuilder()
        sb.append("FINTEL - $periodTitle\n")
        sb.append("Ημερομηνία,Τίτλος,Κατηγορία,Μέλη,Ποσό (€)\n")
        val dateSdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        for (expense in expenses) {
            val date = dateSdf.format(Date(expense.timestamp))
            val amount = String.format(Locale.US, "%.2f", expense.amount)
            sb.append("\"$date\",\"${expense.title}\",\"${expense.categoryName}\",\"${expense.memberNamesDisplay}\",$amount\n")
        }
        val totalStr = String.format(Locale.US, "%.2f", totalAmount)
        sb.append("ΣΥΝΟΛΟ,,,,$totalStr\n")
        return sb.toString()
    }

    fun exportExcel(context: Context, expenses: List<Expense>, periodTitle: String = "Όλα τα Έξοδα") {
        try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(exportDir, "Fintel_Exoda_\${timeStamp}.xls")
            val dateSdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val totalAmount = expenses.sumOf { it.amount }

            val sb = StringBuilder()
            sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
            sb.append("<html xmlns:o=\"urn:schemas-microsoft-com:office:office\" xmlns:x=\"urn:schemas-microsoft-com:office:excel\" xmlns=\"http://www.w3.org/TR/REC-html40\">\n")
            sb.append("<head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/>\n")
            sb.append("<style>\n")
            sb.append("body { font-family: Arial, sans-serif; }\n")
            sb.append("table { border-collapse: collapse; width: 100%; }\n")
            sb.append("th { background-color: #047857; color: white; border: 1px solid #ddd; padding: 8px; text-align: left; }\n")
            sb.append("td { border: 1px solid #ddd; padding: 8px; }\n")
            sb.append(".amount { text-align: right; }\n")
            sb.append("</style></head><body>\n")
            sb.append("<h2>FINTEL - $periodTitle</h2>\n")
            sb.append("<table>\n<thead><tr><th>Ημερομηνία</th><th>Τίτλος</th><th>Κατηγορία</th><th>Πληρωτής</th><th>Μέλη</th><th>Ποσό (€)</th><th>Σημείωση</th></tr></thead>\n<tbody>\n")

            for (expense in expenses) {
                val date = dateSdf.format(Date(expense.timestamp))
                val amount = String.format(Locale.US, "%.2f €", expense.amount)
                val payer = expense.payerMemberName.ifBlank { "-" }
                sb.append("<tr><td>$date</td><td>\${expense.title}</td><td>\${expense.categoryName}</td><td>$payer</td><td>\${expense.memberNamesDisplay}</td><td class=\"amount\">$amount</td><td>\${expense.note}</td></tr>\n")
            }
            val totalStr = String.format(Locale.US, "%.2f €", totalAmount)
            sb.append("<tr style=\"font-weight: bold; background-color: #E2E8F0;\"><td colspan=\"5\">ΣΥΝΟΛΟ</td><td class=\"amount\">$totalStr</td><td></td></tr>\n")
            sb.append("</tbody></table></body></html>")

            file.writeBytes(sb.toString().toByteArray(Charsets.UTF_8))
            shareFile(context, file, "application/vnd.ms-excel", "Αναφορά Εξόδων Fintel (Excel) - $periodTitle")
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting Excel: \${e.message}", e)
        }
    }

    fun exportPdf(context: Context, expenses: List<Expense>, periodTitle: String = "Όλα τα Έξοδα") {
        try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(exportDir, "Fintel_Exoda_\${timeStamp}.pdf")

            val pdfDocument = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842
            val paint = Paint().apply { isAntiAlias = true }
            val dateSdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

            val itemsPerPage = 25
            val totalPages = (expenses.size + itemsPerPage - 1) / itemsPerPage.coerceAtLeast(1)
            val pagesCount = if (totalPages == 0) 1 else totalPages

            var currentItemIndex = 0
            for (p in 1..pagesCount) {
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, p).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                // Header
                paint.color = Color.parseColor("#047857")
                paint.textSize = 20f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("Fintel - $periodTitle", 40f, 50f, paint)

                paint.color = Color.DKGRAY
                paint.textSize = 10f
                paint.typeface = Typeface.DEFAULT
                canvas.drawText("Σελίδα $p από $pagesCount", (pageWidth - 100).toFloat(), 50f, paint)

                paint.color = Color.parseColor("#CBD5E1")
                paint.strokeWidth = 1f
                canvas.drawLine(40f, 65f, (pageWidth - 40).toFloat(), 65f, paint)

                // Table Header
                var y = 90f
                paint.color = Color.parseColor("#1E293B")
                paint.textSize = 11f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("Ημερομηνία", 40f, y, paint)
                canvas.drawText("Περιγραφή", 120f, y, paint)
                canvas.drawText("Κατηγορία", 270f, y, paint)
                canvas.drawText("Πληρωτής", 380f, y, paint)
                canvas.drawText("Ποσό (€)", 480f, y, paint)

                y += 10f
                canvas.drawLine(40f, y, (pageWidth - 40).toFloat(), y, paint)
                y += 18f

                paint.typeface = Typeface.DEFAULT
                paint.textSize = 10f
                paint.color = Color.BLACK

                val pageEnd = (currentItemIndex + itemsPerPage).coerceAtMost(expenses.size)
                while (currentItemIndex < pageEnd) {
                    val exp = expenses[currentItemIndex]
                    val date = dateSdf.format(Date(exp.timestamp))
                    val title = if (exp.title.length > 22) exp.title.take(20) + "..." else exp.title
                    val cat = if (exp.categoryName.length > 16) exp.categoryName.take(14) + "..." else exp.categoryName
                    val payer = if (exp.payerMemberName.length > 14) exp.payerMemberName.take(12) + "..." else exp.payerMemberName
                    val amount = String.format(Locale.US, "%.2f €", exp.amount)

                    canvas.drawText(date, 40f, y, paint)
                    canvas.drawText(title, 120f, y, paint)
                    canvas.drawText(cat, 270f, y, paint)
                    canvas.drawText(payer, 380f, y, paint)
                    canvas.drawText(amount, 480f, y, paint)

                    y += 20f
                    currentItemIndex++
                }

                // If last page, draw total
                if (p == pagesCount) {
                    y += 10f
                    paint.color = Color.parseColor("#047857")
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    paint.textSize = 12f
                    val total = expenses.sumOf { it.amount }
                    val totalStr = String.format(Locale.US, "ΣΥΝΟΛΟ: %.2f €", total)
                    canvas.drawText(totalStr, 400f, y, paint)
                }

                pdfDocument.finishPage(page)
            }

            val fos = FileOutputStream(file)
            pdfDocument.writeTo(fos)
            fos.close()
            pdfDocument.close()

            shareFile(context, file, "application/pdf", "Αναφορά Εξόδων Fintel (PDF) - $periodTitle")
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting PDF: \${e.message}", e)
        }
    }

    private fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "\${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
