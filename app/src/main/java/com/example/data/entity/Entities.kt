package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "subjects",
    indices = [Index(value = ["name"])]
)
data class SubjectEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String? = null,
    val iconKey: String = "book",
    val colorArgb: Int = 0xFF2563EB.toInt(),
    val isPinned: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "folders",
    indices = [Index(value = ["subjectId"]), Index(value = ["parentFolderId"])]
)
data class FolderEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val parentFolderId: String? = null,
    val subjectId: String? = null,
    val description: String? = null,
    val iconKey: String = "folder",
    val colorArgb: Int = 0xFF0D9488.toInt(),
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "chapters",
    indices = [Index(value = ["subjectId"]), Index(value = ["parentFolderId"])]
)
data class ChapterEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val subjectId: String,
    val parentFolderId: String? = null,
    val name: String,
    val description: String? = null,
    val iconKey: String = "bookmark",
    val colorArgb: Int = 0xFF2563EB.toInt(),
    val progressStatus: String = "NOT_STARTED", // "NOT_STARTED", "IN_PROGRESS", "COMPLETED"
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "documents",
    indices = [
        Index(value = ["subjectId"]),
        Index(value = ["chapterId"]),
        Index(value = ["folderId"]),
        Index(value = ["isFavorite"]),
        Index(value = ["revisionStatus"]),
        Index(value = ["lastOpenedAt"])
    ]
)
data class DocumentEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val fileName: String,
    val relativeFilePath: String,
    val mimeType: String,
    val fileSizeBytes: Long = 0L,
    val contentHash: String? = null,
    val subjectId: String? = null,
    val chapterId: String? = null,
    val folderId: String? = null,
    val description: String? = null,
    val isFavorite: Boolean = false,
    val revisionStatus: String = "TODO", // "TODO", "IN_PROGRESS", "MASTERED"
    val sortOrder: Int = 0,
    val pageCount: Int? = null,
    val lastViewedPage: Int? = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastOpenedAt: Long? = null
)
