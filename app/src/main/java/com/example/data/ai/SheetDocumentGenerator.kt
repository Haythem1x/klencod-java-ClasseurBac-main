package com.example.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.storage.DocumentStorageManager
import com.example.data.storage.SavedDocumentResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class SheetDocumentGenerator(
    private val context: Context,
    private val storageManager: DocumentStorageManager
) {

    private val documentsDir: File by lazy {
        File(context.filesDir, "documents").apply { if (!exists()) mkdirs() }
    }

    suspend fun generatePdfSheet(
        title: String,
        subjectName: String,
        chapterName: String?,
        content: String
    ): SavedDocumentResult = withContext(Dispatchers.IO) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 points
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        // Background
        canvas.drawColor(Color.rgb(248, 250, 252))

        // Top Header Banner
        val bannerPaint = Paint().apply {
            color = Color.rgb(26, 54, 93) // Deep Academic Blue
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, 595f, 90f, bannerPaint)

        // Banner Titles
        val whiteTitlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("MON CLASSEUR BAC • FICHE DE RÉVISION", 30f, 38f, whiteTitlePaint)

        val subTitlePaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            textSize = 11f
            isAntiAlias = true
        }
        val dateStr = SimpleDateFormat("dd MMMM yyyy", Locale.FRENCH).format(Date())
        canvas.drawText("$subjectName ${if (chapterName != null) "• $chapterName" else ""} • $dateStr", 30f, 62f, subTitlePaint)

        // Accent line
        val accentPaint = Paint().apply {
            color = Color.rgb(13, 148, 136) // Soft Teal
            strokeWidth = 4f
        }
        canvas.drawLine(0f, 90f, 595f, 90f, accentPaint)

        // Document Main Title
        val docTitlePaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(title.take(60), 30f, 122f, docTitlePaint)

        // Divider
        val dividerPaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 1f
        }
        canvas.drawLine(30f, 132f, 565f, 132f, dividerPaint)

        // Content Text Wrapping
        val textPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 9.5f
            isAntiAlias = true
            typeface = Typeface.DEFAULT
        }
        val boldHeaderPaint = Paint().apply {
            color = Color.rgb(26, 54, 93)
            textSize = 10.5f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        var yPos = 152f
        val maxWidth = 535f
        val lines = content.split("\n")

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty()) {
                yPos += 8f
                continue
            }

            val isHeader = line.startsWith("#") || line.uppercase() == line && line.length < 50 || line.contains("DÉFINITIONS") || line.contains("FORMULES") || line.contains("MÉTHODOLOGIE") || line.contains("CONSEILS")

            val currentPaint = if (isHeader) boldHeaderPaint else textPaint

            // Word wrap
            val words = line.split(" ")
            var currentLine = ""
            for (word in words) {
                val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                val measure = currentPaint.measureText(testLine)
                if (measure > maxWidth) {
                    if (yPos > 800f) break // Avoid page overflow on single sheet
                    canvas.drawText(currentLine, 30f, yPos, currentPaint)
                    yPos += 13.5f
                    currentLine = word
                } else {
                    currentLine = testLine
                }
            }
            if (currentLine.isNotEmpty() && yPos <= 800f) {
                canvas.drawText(currentLine, 30f, yPos, currentPaint)
                yPos += 14f
            }
        }

        // Footer
        val footerPaint = Paint().apply {
            color = Color.rgb(148, 163, 184)
            textSize = 8.5f
            isAntiAlias = true
        }
        canvas.drawText("Généré par l'Assistant Bac IA • Mon Classeur Bac • Page 1/1", 30f, 825f, footerPaint)

        document.finishPage(page)

        // Save PDF file
        val uniqueFileName = "${UUID.randomUUID()}_Fiche_${System.currentTimeMillis()}.pdf"
        val destinationFile = File(documentsDir, uniqueFileName)
        val digest = MessageDigest.getInstance("SHA-256")
        var totalBytes = 0L

        FileOutputStream(destinationFile).use { out ->
            document.writeTo(out)
        }
        document.close()

        val fileBytes = destinationFile.readBytes()
        digest.update(fileBytes)
        val contentHash = digest.digest().joinToString("") { "%02x".format(it) }

        // Generate thumbnail
        storageManager.getPdfPageCount(destinationFile)
        val thumbFile = File(context.cacheDir, "thumbnails/${uniqueFileName}.thumb.jpg")
        val rendered = storageManager.renderPdfPageBitmap(destinationFile, 0, 300)
        rendered?.let { b ->
            FileOutputStream(thumbFile).use { b.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            b.recycle()
        }

        SavedDocumentResult(
            fileName = uniqueFileName,
            title = title,
            relativeFilePath = "documents/$uniqueFileName",
            mimeType = "application/pdf",
            fileSizeBytes = destinationFile.length(),
            contentHash = contentHash,
            pageCount = 1
        )
    }

    suspend fun generateImageSheet(
        title: String,
        subjectName: String,
        chapterName: String?,
        content: String
    ): SavedDocumentResult = withContext(Dispatchers.IO) {
        val width = 1080
        val height = 1520
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Clean Slate Background
        canvas.drawColor(Color.rgb(248, 250, 252))

        // Outer Border
        val borderPaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        canvas.drawRect(20f, 20f, width - 20f, height - 20f, borderPaint)

        // Header Background
        val headerPaint = Paint().apply {
            color = Color.rgb(26, 54, 93) // Deep Academic Navy
            style = Paint.Style.FILL
        }
        canvas.drawRect(20f, 20f, width - 20f, 180f, headerPaint)

        // Header Text
        val headerTitlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("MON CLASSEUR BAC", 60f, 85f, headerTitlePaint)

        val headerSubPaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            textSize = 22f
            isAntiAlias = true
        }
        val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH).format(Date())
        canvas.drawText("FICHE DE COURS • $subjectName ${if (chapterName != null) "• $chapterName" else ""} • $dateStr", 60f, 135f, headerSubPaint)

        // Accent strip
        val accentPaint = Paint().apply {
            color = Color.rgb(13, 148, 136) // Soft Teal
            strokeWidth = 8f
        }
        canvas.drawLine(20f, 180f, width - 20f, 180f, accentPaint)

        // Sheet Title
        val docTitlePaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(title.take(50), 60f, 240f, docTitlePaint)

        // Content
        val textPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 21f
            isAntiAlias = true
            typeface = Typeface.DEFAULT
        }
        val headerSectionPaint = Paint().apply {
            color = Color.rgb(26, 54, 93)
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        var y = 295f
        val maxTextWidth = (width - 120).toFloat()
        val lines = content.split("\n")

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty()) {
                y += 18f
                continue
            }

            val isHeader = line.startsWith("#") || line.contains("DÉFINITIONS") || line.contains("FORMULES") || line.contains("MÉTHODOLOGIE") || line.contains("CONSEILS")
            val p = if (isHeader) headerSectionPaint else textPaint

            val words = line.split(" ")
            var cur = ""
            for (w in words) {
                val test = if (cur.isEmpty()) w else "$cur $w"
                if (p.measureText(test) > maxTextWidth) {
                    if (y > height - 100) break
                    canvas.drawText(cur, 60f, y, p)
                    y += 30f
                    cur = w
                } else {
                    cur = test
                }
            }
            if (cur.isNotEmpty() && y <= height - 100) {
                canvas.drawText(cur, 60f, y, p)
                y += 32f
            }
        }

        // Save Image file
        val uniqueFileName = "${UUID.randomUUID()}_Fiche_${System.currentTimeMillis()}.jpg"
        val destinationFile = File(documentsDir, uniqueFileName)

        FileOutputStream(destinationFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        bitmap.recycle()

        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(destinationFile.readBytes())
        val contentHash = digest.digest().joinToString("") { "%02x".format(it) }

        SavedDocumentResult(
            fileName = uniqueFileName,
            title = title,
            relativeFilePath = "documents/$uniqueFileName",
            mimeType = "image/jpeg",
            fileSizeBytes = destinationFile.length(),
            contentHash = contentHash,
            pageCount = 1
        )
    }
}
