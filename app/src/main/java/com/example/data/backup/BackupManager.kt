package com.example.data.backup

import android.content.Context
import android.net.Uri
import com.example.data.database.AppDatabase
import com.example.data.entity.ChapterEntity
import com.example.data.entity.DocumentEntity
import com.example.data.entity.FolderEntity
import com.example.data.entity.SubjectEntity
import com.example.data.storage.DocumentStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupManager(
    private val context: Context,
    private val database: AppDatabase,
    private val storageManager: DocumentStorageManager
) {

    companion object {
        const val BACKUP_VERSION = 1
        const val MAX_ENTRIES = 500
        const val MAX_FILE_SIZE = 50 * 1024 * 1024L // 50 MB
        const val MAX_TOTAL_SIZE = 500 * 1024 * 1024L // 500 MB
    }

    suspend fun exportCompleteBackup(outputUri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val subjects = database.subjectDao().getAllSubjects().first()
            val folders = database.folderDao().getStandaloneFolders().first()
            val chapters = database.chapterDao().getAllChapters().first()
            val documents = database.documentDao().getAllDocuments().first()

            val manifest = JSONObject().apply {
                put("version", BACKUP_VERSION)
                put("app", "Mon Classeur Bac")
                put("timestamp", System.currentTimeMillis())

                val subjectsArray = JSONArray()
                subjects.forEach { s ->
                    subjectsArray.put(JSONObject().apply {
                        put("id", s.id)
                        put("name", s.name)
                        put("description", s.description ?: "")
                        put("iconKey", s.iconKey)
                        put("colorArgb", s.colorArgb)
                        put("isPinned", s.isPinned)
                        put("sortOrder", s.sortOrder)
                        put("createdAt", s.createdAt)
                        put("updatedAt", s.updatedAt)
                    })
                }
                put("subjects", subjectsArray)

                val foldersArray = JSONArray()
                folders.forEach { f ->
                    foldersArray.put(JSONObject().apply {
                        put("id", f.id)
                        put("name", f.name)
                        put("parentFolderId", f.parentFolderId ?: "")
                        put("subjectId", f.subjectId ?: "")
                        put("description", f.description ?: "")
                        put("iconKey", f.iconKey)
                        put("colorArgb", f.colorArgb)
                        put("sortOrder", f.sortOrder)
                    })
                }
                put("folders", foldersArray)

                val chaptersArray = JSONArray()
                chapters.forEach { c ->
                    chaptersArray.put(JSONObject().apply {
                        put("id", c.id)
                        put("subjectId", c.subjectId)
                        put("parentFolderId", c.parentFolderId ?: "")
                        put("name", c.name)
                        put("description", c.description ?: "")
                        put("iconKey", c.iconKey)
                        put("colorArgb", c.colorArgb)
                        put("progressStatus", c.progressStatus)
                        put("sortOrder", c.sortOrder)
                        put("createdAt", c.createdAt)
                        put("updatedAt", c.updatedAt)
                    })
                }
                put("chapters", chaptersArray)

                val docsArray = JSONArray()
                documents.forEach { d ->
                    docsArray.put(JSONObject().apply {
                        put("id", d.id)
                        put("title", d.title)
                        put("fileName", d.fileName)
                        put("relativeFilePath", d.relativeFilePath)
                        put("mimeType", d.mimeType)
                        put("fileSizeBytes", d.fileSizeBytes)
                        put("contentHash", d.contentHash ?: "")
                        put("subjectId", d.subjectId ?: "")
                        put("chapterId", d.chapterId ?: "")
                        put("folderId", d.folderId ?: "")
                        put("description", d.description ?: "")
                        put("isFavorite", d.isFavorite)
                        put("revisionStatus", d.revisionStatus)
                        put("sortOrder", d.sortOrder)
                        put("pageCount", d.pageCount ?: 1)
                        put("lastViewedPage", d.lastViewedPage ?: 0)
                        put("createdAt", d.createdAt)
                        put("updatedAt", d.updatedAt)
                    })
                }
                put("documents", docsArray)
            }

            val contentResolver = context.contentResolver
            contentResolver.openOutputStream(outputUri)?.use { outputStream ->
                ZipOutputStream(BufferedOutputStream(outputStream)).use { zipOut ->
                    // Write manifest.json
                    val manifestBytes = manifest.toString(2).toByteArray(Charsets.UTF_8)
                    zipOut.putNextEntry(ZipEntry("manifest.json"))
                    zipOut.write(manifestBytes)
                    zipOut.closeEntry()

                    // Write each document file
                    documents.forEach { doc ->
                        val file = storageManager.getFile(doc.relativeFilePath)
                        if (file.exists() && file.isFile) {
                            val entryPath = "documents/${file.name}"
                            zipOut.putNextEntry(ZipEntry(entryPath))
                            FileInputStream(file).use { fileIn ->
                                fileIn.copyTo(zipOut, bufferSize = 16 * 1024)
                            }
                            zipOut.closeEntry()
                        }
                    }
                }
            } ?: return@withContext Result.failure(IllegalStateException("Could not open output stream for backup"))

            Result.success(documents.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun inspectBackup(inputUri: Uri): Result<BackupSummary> = withContext(Dispatchers.IO) {
        try {
            var manifestContent: String? = null
            var totalFiles = 0
            var totalBytes = 0L

            context.contentResolver.openInputStream(inputUri)?.use { rawIn ->
                ZipInputStream(BufferedInputStream(rawIn)).use { zipIn ->
                    var entry = zipIn.nextEntry
                    var entryCount = 0
                    while (entry != null) {
                        entryCount++
                        if (entryCount > MAX_ENTRIES) {
                            return@withContext Result.failure(IllegalStateException("Archive contains too many entries (max $MAX_ENTRIES)"))
                        }
                        // Path traversal check
                        val name = entry.name
                        if (name.contains("..") || name.startsWith("/")) {
                            return@withContext Result.failure(SecurityException("Invalid path in archive: $name"))
                        }
                        if (name == "manifest.json") {
                            manifestContent = zipIn.bufferedReader(Charsets.UTF_8).readText()
                        } else if (!entry.isDirectory) {
                            totalFiles++
                            totalBytes += entry.size.coerceAtLeast(0)
                        }
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                    }
                }
            }

            if (manifestContent == null) {
                return@withContext Result.failure(IllegalStateException("Manifest file not found in backup archive"))
            }

            val json = JSONObject(manifestContent!!)
            val version = json.optInt("version", 0)
            if (version > BACKUP_VERSION) {
                return@withContext Result.failure(IllegalStateException("Backup version $version is newer than app version $BACKUP_VERSION"))
            }

            val subjectsCount = json.optJSONArray("subjects")?.length() ?: 0
            val chaptersCount = json.optJSONArray("chapters")?.length() ?: 0
            val documentsCount = json.optJSONArray("documents")?.length() ?: 0

            Result.success(
                BackupSummary(
                    version = version,
                    subjectsCount = subjectsCount,
                    chaptersCount = chaptersCount,
                    documentsCount = documentsCount,
                    totalFileBytes = totalBytes
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreBackup(inputUri: Uri, replaceCurrent: Boolean): Result<Int> = withContext(Dispatchers.IO) {
        val stagingDir = File(context.cacheDir, "restore_staging_${System.currentTimeMillis()}").apply { mkdirs() }
        try {
            var manifestContent: String? = null
            var totalExtractedBytes = 0L
            var entryCount = 0

            // 1. Stage files safely
            context.contentResolver.openInputStream(inputUri)?.use { rawIn ->
                ZipInputStream(BufferedInputStream(rawIn)).use { zipIn ->
                    var entry = zipIn.nextEntry
                    while (entry != null) {
                        entryCount++
                        if (entryCount > MAX_ENTRIES) {
                            throw IllegalStateException("Too many archive entries (max $MAX_ENTRIES)")
                        }
                        val name = entry.name
                        if (name.contains("..") || name.startsWith("/")) {
                            throw SecurityException("Path traversal attempt in archive: $name")
                        }

                        if (name == "manifest.json") {
                            val bytes = zipIn.readBytes()
                            manifestContent = String(bytes, Charsets.UTF_8)
                        } else if (!entry.isDirectory && name.startsWith("documents/")) {
                            val fileName = name.substringAfter("documents/")
                            val targetFile = File(stagingDir, fileName)
                            FileOutputStream(targetFile).use { out ->
                                val buffer = ByteArray(16 * 1024)
                                var read: Int
                                var fileBytes = 0L
                                while (zipIn.read(buffer).also { read = it } != -1) {
                                    fileBytes += read
                                    totalExtractedBytes += read
                                    if (fileBytes > MAX_FILE_SIZE) throw IllegalStateException("File $name exceeds max size")
                                    if (totalExtractedBytes > MAX_TOTAL_SIZE) throw IllegalStateException("Archive exceeds max total uncompressed size")
                                    out.write(buffer, 0, read)
                                }
                            }
                        }
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                    }
                }
            } ?: throw IllegalStateException("Could not read backup file")

            if (manifestContent == null) {
                throw IllegalStateException("No manifest.json in archive")
            }

            val json = JSONObject(manifestContent!!)
            val subjectsArray = json.optJSONArray("subjects") ?: JSONArray()
            val chaptersArray = json.optJSONArray("chapters") ?: JSONArray()
            val documentsArray = json.optJSONArray("documents") ?: JSONArray()

            val subjectsToInsert = mutableListOf<SubjectEntity>()
            for (i in 0 until subjectsArray.length()) {
                val obj = subjectsArray.getJSONObject(i)
                subjectsToInsert.add(
                    SubjectEntity(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        description = obj.optString("description").takeIf { it.isNotEmpty() },
                        iconKey = obj.optString("iconKey", "book"),
                        colorArgb = obj.optInt("colorArgb", 0xFF2563EB.toInt()),
                        isPinned = obj.optBoolean("isPinned", false),
                        sortOrder = obj.optInt("sortOrder", i),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }

            val chaptersToInsert = mutableListOf<ChapterEntity>()
            for (i in 0 until chaptersArray.length()) {
                val obj = chaptersArray.getJSONObject(i)
                chaptersToInsert.add(
                    ChapterEntity(
                        id = obj.getString("id"),
                        subjectId = obj.getString("subjectId"),
                        parentFolderId = obj.optString("parentFolderId").takeIf { it.isNotEmpty() },
                        name = obj.getString("name"),
                        description = obj.optString("description").takeIf { it.isNotEmpty() },
                        iconKey = obj.optString("iconKey", "bookmark"),
                        colorArgb = obj.optInt("colorArgb", 0xFF2563EB.toInt()),
                        progressStatus = obj.optString("progressStatus", "NOT_STARTED"),
                        sortOrder = obj.optInt("sortOrder", i),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }

            val documentsToInsert = mutableListOf<DocumentEntity>()
            val docsDir = File(context.filesDir, "documents").apply { if (!exists()) mkdirs() }

            for (i in 0 until documentsArray.length()) {
                val obj = documentsArray.getJSONObject(i)
                val relativePath = obj.getString("relativeFilePath")
                val fileName = obj.getString("fileName")
                val stagedFile = File(stagingDir, fileName)

                if (stagedFile.exists()) {
                    val destFile = File(docsDir, fileName)
                    stagedFile.copyTo(destFile, overwrite = true)
                }

                documentsToInsert.add(
                    DocumentEntity(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        fileName = fileName,
                        relativeFilePath = relativePath,
                        mimeType = obj.getString("mimeType"),
                        fileSizeBytes = obj.optLong("fileSizeBytes", 0L),
                        contentHash = obj.optString("contentHash").takeIf { it.isNotEmpty() },
                        subjectId = obj.optString("subjectId").takeIf { it.isNotEmpty() },
                        chapterId = obj.optString("chapterId").takeIf { it.isNotEmpty() },
                        folderId = obj.optString("folderId").takeIf { it.isNotEmpty() },
                        description = obj.optString("description").takeIf { it.isNotEmpty() },
                        isFavorite = obj.optBoolean("isFavorite", false),
                        revisionStatus = obj.optString("revisionStatus", "TODO"),
                        sortOrder = obj.optInt("sortOrder", i),
                        pageCount = obj.optInt("pageCount", 1),
                        lastViewedPage = obj.optInt("lastViewedPage", 0),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }

            // Commit transactionally
            if (replaceCurrent) {
                // Remove existing DB entries
                val currentSubjects = database.subjectDao().getAllSubjects().first()
                currentSubjects.forEach { database.subjectDao().deleteSubject(it) }
            }

            database.subjectDao().insertSubjects(subjectsToInsert)
            database.chapterDao().insertChapters(chaptersToInsert)
            database.documentDao().insertDocuments(documentsToInsert)

            Result.success(documentsToInsert.size)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            stagingDir.deleteRecursively()
        }
    }
}

data class BackupSummary(
    val version: Int,
    val subjectsCount: Int,
    val chaptersCount: Int,
    val documentsCount: Int,
    val totalFileBytes: Long
)
