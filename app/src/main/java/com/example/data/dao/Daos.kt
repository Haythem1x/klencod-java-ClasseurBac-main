package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.ChapterEntity
import com.example.data.entity.DocumentEntity
import com.example.data.entity.FolderEntity
import com.example.data.entity.SubjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects ORDER BY isPinned DESC, sortOrder ASC, name ASC")
    fun getAllSubjects(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE id = :id")
    suspend fun getSubjectById(id: String): SubjectEntity?

    @Query("SELECT * FROM subjects WHERE id = :id")
    fun observeSubjectById(id: String): Flow<SubjectEntity?>

    @Query("SELECT * FROM subjects WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name)) LIMIT 1")
    suspend fun getSubjectByName(name: String): SubjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: SubjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubjects(subjects: List<SubjectEntity>)

    @Update
    suspend fun updateSubject(subject: SubjectEntity)

    @Delete
    suspend fun deleteSubject(subject: SubjectEntity)

    @Query("DELETE FROM subjects WHERE id = :id")
    suspend fun deleteSubjectById(id: String)

    @Query("SELECT COUNT(*) FROM subjects")
    fun getSubjectsCount(): Flow<Int>
}

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders WHERE subjectId = :subjectId ORDER BY sortOrder ASC, name ASC")
    fun getFoldersBySubject(subjectId: String): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE subjectId IS NULL ORDER BY sortOrder ASC, name ASC")
    fun getStandaloneFolders(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE id = :id")
    suspend fun getFolderById(id: String): FolderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolders(folders: List<FolderEntity>)

    @Update
    suspend fun updateFolder(folder: FolderEntity)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteFolderById(id: String)

    @Query("DELETE FROM folders WHERE subjectId = :subjectId")
    suspend fun deleteFoldersBySubjectId(subjectId: String)
}

@Dao
interface ChapterDao {
    @Query("SELECT * FROM chapters WHERE subjectId = :subjectId ORDER BY sortOrder ASC, createdAt ASC")
    fun getChaptersBySubject(subjectId: String): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters ORDER BY sortOrder ASC")
    fun getAllChapters(): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE id = :id")
    suspend fun getChapterById(id: String): ChapterEntity?

    @Query("SELECT * FROM chapters WHERE id = :id")
    fun observeChapterById(id: String): Flow<ChapterEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: ChapterEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<ChapterEntity>)

    @Update
    suspend fun updateChapter(chapter: ChapterEntity)

    @Query("DELETE FROM chapters WHERE id = :id")
    suspend fun deleteChapterById(id: String)

    @Query("DELETE FROM chapters WHERE subjectId = :subjectId")
    suspend fun deleteChaptersBySubjectId(subjectId: String)

    @Query("SELECT COUNT(*) FROM chapters WHERE subjectId = :subjectId")
    fun getChaptersCountForSubject(subjectId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM chapters")
    fun getTotalChaptersCount(): Flow<Int>
}

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents ORDER BY createdAt DESC")
    fun getAllDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getDocumentById(id: String): DocumentEntity?

    @Query("SELECT * FROM documents WHERE id = :id")
    fun observeDocumentById(id: String): Flow<DocumentEntity?>

    @Query("SELECT * FROM documents WHERE subjectId = :subjectId AND chapterId IS NULL ORDER BY sortOrder ASC, createdAt DESC")
    fun getLooseDocumentsForSubject(subjectId: String): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE subjectId = :subjectId ORDER BY sortOrder ASC, createdAt DESC")
    fun getAllDocumentsForSubject(subjectId: String): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE chapterId = :chapterId ORDER BY sortOrder ASC, createdAt DESC")
    fun getDocumentsByChapter(chapterId: String): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE isFavorite = 1 ORDER BY updatedAt DESC")
    fun getFavoriteDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE lastOpenedAt IS NOT NULL ORDER BY lastOpenedAt DESC LIMIT 10")
    fun getRecentDocuments(): Flow<List<DocumentEntity>>

    @Query("""
        SELECT * FROM documents 
        WHERE title LIKE '%' || :query || '%' 
           OR description LIKE '%' || :query || '%'
           OR fileName LIKE '%' || :query || '%'
        ORDER BY updatedAt DESC
    """)
    fun searchDocuments(query: String): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE contentHash = :hash LIMIT 1")
    suspend fun findByContentHash(hash: String): DocumentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocuments(documents: List<DocumentEntity>)

    @Update
    suspend fun updateDocument(document: DocumentEntity)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteDocumentById(id: String)

    @Query("DELETE FROM documents WHERE subjectId = :subjectId")
    suspend fun deleteDocumentsBySubjectId(subjectId: String)

    @Query("DELETE FROM documents WHERE chapterId = :chapterId")
    suspend fun deleteDocumentsByChapterId(chapterId: String)

    @Query("SELECT COUNT(*) FROM documents")
    fun getTotalDocumentsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM documents WHERE subjectId = :subjectId")
    fun getDocumentsCountForSubject(subjectId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM documents WHERE chapterId = :chapterId")
    fun getDocumentsCountForChapter(chapterId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM documents WHERE revisionStatus = 'MASTERED'")
    fun getMasteredDocumentsCount(): Flow<Int>

    @Query("UPDATE documents SET revisionStatus = 'TODO'")
    suspend fun resetAllRevisionStatus()
}
