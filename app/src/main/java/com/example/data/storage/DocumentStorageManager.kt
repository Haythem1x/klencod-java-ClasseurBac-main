package com.example.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.UUID

class DocumentStorageManager(private val context: Context) {

    private val documentsDir: File by lazy {
        File(context.filesDir, "documents").apply {
            if (!exists()) mkdirs()
        }
    }

    private val thumbnailsDir: File by lazy {
        File(context.cacheDir, "thumbnails").apply {
            if (!exists()) mkdirs()
        }
    }

    fun getFile(relativeFilePath: String): File {
        return File(context.filesDir, relativeFilePath)
    }

    suspend fun saveDocumentFromUri(
        uri: Uri,
        customTitle: String? = null
    ): SavedDocumentResult = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(uri) ?: detectMimeTypeFromUri(uri)
        val originalFileName = getFileNameFromUri(uri) ?: "doc_${System.currentTimeMillis()}"
        val extension = getExtension(originalFileName, mimeType)
        
        val uniqueFileName = "${UUID.randomUUID()}_$originalFileName"
        val destinationFile = File(documentsDir, uniqueFileName)
        
        val digest = MessageDigest.getInstance("SHA-256")
        var totalBytes = 0L

        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destinationFile).use { output ->
                val buffer = ByteArray(16 * 1024)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    digest.update(buffer, 0, bytesRead)
                    totalBytes += bytesRead
                }
                output.flush()
            }
        } ?: throw IllegalArgumentException("Could not open input stream from URI: $uri")

        val contentHash = digest.digest().joinToString("") { "%02x".format(it) }
        val relativePath = "documents/$uniqueFileName"
        val title = customTitle?.takeIf { it.isNotBlank() } ?: originalFileName.substringBeforeLast(".")

        var pageCount: Int? = null
        if (isPdf(mimeType)) {
            pageCount = getPdfPageCount(destinationFile)
            generatePdfThumbnail(destinationFile, uniqueFileName)
        } else if (isImage(mimeType)) {
            generateImageThumbnail(destinationFile, uniqueFileName)
        }

        SavedDocumentResult(
            fileName = uniqueFileName,
            title = title,
            relativeFilePath = relativePath,
            mimeType = mimeType,
            fileSizeBytes = totalBytes,
            contentHash = contentHash,
            pageCount = pageCount
        )
    }

    suspend fun saveDocumentFromStream(
        inputStream: InputStream,
        originalName: String,
        mimeType: String
    ): SavedDocumentResult = withContext(Dispatchers.IO) {
        val uniqueFileName = "${UUID.randomUUID()}_$originalName"
        val destinationFile = File(documentsDir, uniqueFileName)
        val digest = MessageDigest.getInstance("SHA-256")
        var totalBytes = 0L

        FileOutputStream(destinationFile).use { output ->
            val buffer = ByteArray(16 * 1024)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
                digest.update(buffer, 0, bytesRead)
                totalBytes += bytesRead
            }
            output.flush()
        }

        val contentHash = digest.digest().joinToString("") { "%02x".format(it) }
        val relativePath = "documents/$uniqueFileName"
        val title = originalName.substringBeforeLast(".")

        var pageCount: Int? = null
        if (isPdf(mimeType)) {
            pageCount = getPdfPageCount(destinationFile)
            generatePdfThumbnail(destinationFile, uniqueFileName)
        } else if (isImage(mimeType)) {
            generateImageThumbnail(destinationFile, uniqueFileName)
        }

        SavedDocumentResult(
            fileName = uniqueFileName,
            title = title,
            relativeFilePath = relativePath,
            mimeType = mimeType,
            fileSizeBytes = totalBytes,
            contentHash = contentHash,
            pageCount = pageCount
        )
    }

    fun getThumbnailFile(uniqueFileName: String): File? {
        val thumb = File(thumbnailsDir, "${uniqueFileName}.thumb.jpg")
        return if (thumb.exists()) thumb else null
    }

    fun getPdfPageCount(pdfFile: File): Int {
        return try {
            ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    renderer.pageCount
                }
            }
        } catch (e: Exception) {
            1
        }
    }

    suspend fun renderPdfPageBitmap(pdfFile: File, pageIndex: Int, targetWidth: Int = 1080): Bitmap? = withContext(Dispatchers.IO) {
        try {
            ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (pageIndex in 0 until renderer.pageCount) {
                        renderer.openPage(pageIndex).use { page ->
                            val scale = targetWidth.toFloat() / page.width.toFloat()
                            val width = (page.width * scale).toInt().coerceAtLeast(1)
                            val height = (page.height * scale).toInt().coerceAtLeast(1)
                            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            bitmap.eraseColor(android.graphics.Color.WHITE)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            bitmap
                        }
                    } else null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun generatePdfThumbnail(pdfFile: File, uniqueFileName: String) {
        try {
            ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (renderer.pageCount > 0) {
                        renderer.openPage(0).use { firstPage ->
                            val scale = 240f / firstPage.width.toFloat()
                            val width = 240
                            val height = (firstPage.height * scale).toInt().coerceAtLeast(1)
                            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            bitmap.eraseColor(android.graphics.Color.WHITE)
                            firstPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            
                            val thumbFile = File(thumbnailsDir, "${uniqueFileName}.thumb.jpg")
                            FileOutputStream(thumbFile).use { out ->
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
                            }
                            bitmap.recycle()
                        }
                    }
                }
            }
        } catch (_: Exception) {}
    }

    private fun generateImageThumbnail(imageFile: File, uniqueFileName: String) {
        try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(imageFile.absolutePath, options)
            
            val targetSize = 250
            var sampleSize = 1
            while (options.outWidth / (sampleSize * 2) >= targetSize && options.outHeight / (sampleSize * 2) >= targetSize) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            val sampledBitmap = BitmapFactory.decodeFile(imageFile.absolutePath, decodeOptions) ?: return

            val thumbFile = File(thumbnailsDir, "${uniqueFileName}.thumb.jpg")
            FileOutputStream(thumbFile).use { out ->
                sampledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }
            sampledBitmap.recycle()
        } catch (_: Exception) {}
    }

    fun deletePhysicalFile(relativeFilePath: String): Boolean {
        return try {
            val file = getFile(relativeFilePath)
            val thumb = getThumbnailFile(file.name)
            thumb?.delete()
            if (file.exists()) file.delete() else true
        } catch (e: Exception) {
            false
        }
    }

    fun getUsedStorageBytes(): Long {
        return calculateDirSize(documentsDir)
    }

    fun getCacheSize(): Long {
        return calculateDirSize(thumbnailsDir)
    }

    fun clearCache(): Boolean {
        return try {
            thumbnailsDir.listFiles()?.forEach { it.delete() }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun calculateDirSize(dir: File): Long {
        var size = 0L
        dir.listFiles()?.forEach { file ->
            size += if (file.isDirectory) calculateDirSize(file) else file.length()
        }
        return size
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) name = it.getString(index)
                }
            }
        }
        if (name == null) {
            name = uri.path?.let {
                val cut = it.lastIndexOf('/')
                if (cut != -1) it.substring(cut + 1) else it
            }
        }
        return name
    }

    private fun detectMimeTypeFromUri(uri: Uri): String {
        val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
        return extension?.let { MimeTypeMap.getSingleton().getMimeTypeFromExtension(it.lowercase()) }
            ?: "application/octet-stream"
    }

    private fun getExtension(fileName: String, mimeType: String): String {
        val extFromFileName = fileName.substringAfterLast('.', "")
        if (extFromFileName.isNotBlank()) return extFromFileName
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "bin"
    }

    fun isPdf(mimeType: String): Boolean = mimeType.equals("application/pdf", ignoreCase = true)
    
    fun isImage(mimeType: String): Boolean = mimeType.startsWith("image/", ignoreCase = true)
}

data class SavedDocumentResult(
    val fileName: String,
    val title: String,
    val relativeFilePath: String,
    val mimeType: String,
    val fileSizeBytes: Long,
    val contentHash: String,
    val pageCount: Int?
)
