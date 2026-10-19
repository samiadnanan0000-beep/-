package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.PersonEntity
import com.example.data.TransactionEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfStatementExporter {

    fun generateAndSharePdf(
        context: Context,
        person: PersonEntity,
        transactions: List<TransactionEntity>,
        currency: String
    ): Result<File> {
        return try {
            val pdfDoc = PdfDocument()
            val pageWidth = 595 // Standard A4 points (72 dpi)
            val pageHeight = 842
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDoc.startPage(pageInfo)
            val canvas = page.canvas

            // Paints
            val textPaint = Paint().apply {
                color = Color.DKGRAY
                textSize = 10f
                isAntiAlias = true
            }

            val titlePaint = Paint().apply {
                color = Color.parseColor("#0C66E4")
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val subtitlePaint = Paint().apply {
                color = Color.parseColor("#1C2541")
                textSize = 13f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val headerBgPaint = Paint().apply {
                color = Color.parseColor("#EEF2F6")
                style = Paint.Style.FILL
            }

            val linePaint = Paint().apply {
                color = Color.parseColor("#D0D7DE")
                strokeWidth = 1f
            }

            val watermarkPaint = Paint().apply {
                color = Color.parseColor("#10000000")
                textSize = 54f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }

            // Draw subtle watermark across center
            canvas.save()
            canvas.rotate(-30f, pageWidth / 2f, pageHeight / 2f)
            canvas.drawText("وافي - دفتر الديون والذهب", pageWidth / 2f, pageHeight / 2f, watermarkPaint)
            canvas.restore()

            var y = 50f

            // Header Banner
            canvas.drawText("وافي - كشف حساب مالي رسمي", 40f, y, titlePaint)
            y += 20f

            val dateStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.ENGLISH).format(Date())
            textPaint.textSize = 9f
            canvas.drawText("تاريخ الإصدار: $dateStr", 40f, y, textPaint)
            y += 25f

            // Person Info Card Box
            val boxPaint = Paint().apply {
                color = Color.parseColor("#F8F9FA")
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(40f, y, (pageWidth - 40).toFloat(), y + 60f, 12f, 12f, boxPaint)
            canvas.drawRoundRect(40f, y, (pageWidth - 40).toFloat(), y + 60f, 12f, 12f, Paint().apply {
                color = Color.parseColor("#E2E8F0")
                style = Paint.Style.STROKE
                strokeWidth = 1f
            })

            canvas.drawText("جهة التعامل: ${person.name}", 55f, y + 25f, subtitlePaint)
            val phoneText = if (person.phone.isNotEmpty()) "الهاتف: ${person.phone}" else "الهاتف: غير محدد"
            canvas.drawText(phoneText, 55f, y + 45f, textPaint)

            // Balance Summary
            val currentBalance = transactions.filter { it.currency == currency }.sumOf {
                if (it.type == TransactionEntity.TYPE_DEBTOR_HIM) it.amount else -it.amount
            }
            val balanceStr = String.format(Locale.ENGLISH, "%.2f %s", currentBalance, currency)
            val balanceColor = if (currentBalance > 0) Color.parseColor("#E5484D") else Color.parseColor("#30A46C")
            val balancePaint = Paint().apply {
                color = balanceColor
                textSize = 14f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }
            val statusLabel = if (currentBalance > 0) "المستحق لك (عليه):" else if (currentBalance < 0) "الالتزام عليك (له):" else "الحساب مصفى:"
            canvas.drawText(statusLabel, (pageWidth - 55).toFloat(), y + 25f, Paint().apply {
                color = Color.DKGRAY
                textSize = 10f
                textAlign = Paint.Align.RIGHT
            })
            canvas.drawText(balanceStr, (pageWidth - 55).toFloat(), y + 48f, balancePaint)

            y += 80f

            // Table Header
            canvas.drawRect(40f, y, (pageWidth - 40).toFloat(), y + 26f, headerBgPaint)
            canvas.drawLine(40f, y + 26f, (pageWidth - 40).toFloat(), y + 26f, linePaint)

            val thPaint = Paint().apply {
                color = Color.BLACK
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            canvas.drawText("#", 50f, y + 17f, thPaint)
            canvas.drawText("التاريخ", 75f, y + 17f, thPaint)
            canvas.drawText("النوع", 160f, y + 17f, thPaint)
            canvas.drawText("البيان", 220f, y + 17f, thPaint)
            canvas.drawText("المبلغ", 380f, y + 17f, thPaint)
            canvas.drawText("الرصيد التراكمي", 470f, y + 17f, thPaint)

            y += 28f

            val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH)
            transactions.take(28).forEachIndexed { index, tx ->
                val typeStr = if (tx.type == TransactionEntity.TYPE_DEBTOR_HIM) "عليه (له مدين)" else "له (سداد/دائن)"
                val typeColor = if (tx.type == TransactionEntity.TYPE_DEBTOR_HIM) Color.parseColor("#E5484D") else Color.parseColor("#30A46C")
                val rowTypePaint = Paint().apply {
                    color = typeColor
                    textSize = 9f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }

                textPaint.textSize = 8.5f
                canvas.drawText("${index + 1}", 50f, y + 14f, textPaint)
                canvas.drawText(dateFormat.format(Date(tx.date)), 75f, y + 14f, textPaint)
                canvas.drawText(typeStr, 160f, y + 14f, rowTypePaint)

                val detailsSnippet = if (tx.details.length > 25) tx.details.take(23) + "..." else tx.details.ifEmpty { "حركة قيد" }
                canvas.drawText(detailsSnippet, 220f, y + 14f, textPaint)

                val amtStr = String.format(Locale.ENGLISH, "%.2f", tx.amount)
                canvas.drawText(amtStr, 380f, y + 14f, thPaint)

                val balStr = String.format(Locale.ENGLISH, "%.2f", tx.balanceAfter)
                canvas.drawText(balStr, 470f, y + 14f, thPaint)

                canvas.drawLine(40f, y + 20f, (pageWidth - 40).toFloat(), y + 20f, Paint().apply {
                    color = Color.parseColor("#F1F3F5")
                    strokeWidth = 1f
                })
                y += 20f
            }

            // Footer
            y = (pageHeight - 40).toFloat()
            canvas.drawLine(40f, y - 10f, (pageWidth - 40).toFloat(), y - 10f, linePaint)
            canvas.drawText("تم استخراج هذا التقرير آلياً عبر تطبيق وافي لإدارة الديون والذهب - لا يتطلب خوادم خارجية", 40f, y + 5f, Paint().apply {
                color = Color.GRAY
                textSize = 8f
            })

            pdfDoc.finishPage(page)

            // Save PDF
            val outDir = File(context.cacheDir, "statements")
            if (!outDir.exists()) outDir.mkdirs()
            val fileName = "statement_${person.name.replace(" ", "_")}_${System.currentTimeMillis()}.pdf"
            val file = File(outDir, fileName)

            FileOutputStream(file).use { pdfDoc.writeTo(it) }
            pdfDoc.close()

            // Open share intent
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "كشف حساب: ${person.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, "مشاركة كشف الحساب عبر").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })

            Result.success(file)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
