package com.example.data.repository

import com.example.data.database.AppDatabase
import com.example.data.entity.ChapterEntity
import com.example.data.entity.DocumentEntity
import com.example.data.entity.FolderEntity
import com.example.data.entity.SubjectEntity
import com.example.data.storage.DocumentStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.UUID

class BinderRepository(
    private val database: AppDatabase,
    private val storageManager: DocumentStorageManager
) {
    private val subjectDao = database.subjectDao()
    private val chapterDao = database.chapterDao()
    private val folderDao = database.folderDao()
    private val documentDao = database.documentDao()

    val allSubjects: Flow<List<SubjectEntity>> = subjectDao.getAllSubjects()
    val allDocuments: Flow<List<DocumentEntity>> = documentDao.getAllDocuments()
    val recentDocuments: Flow<List<DocumentEntity>> = documentDao.getRecentDocuments()
    val favoriteDocuments: Flow<List<DocumentEntity>> = documentDao.getFavoriteDocuments()
    val totalDocumentsCount: Flow<Int> = documentDao.getTotalDocumentsCount()
    val totalSubjectsCount: Flow<Int> = subjectDao.getSubjectsCount()
    val masteredDocumentsCount: Flow<Int> = documentDao.getMasteredDocumentsCount()

    // Subject operations
    fun observeSubject(subjectId: String): Flow<SubjectEntity?> = subjectDao.observeSubjectById(subjectId)
    
    suspend fun getSubject(subjectId: String): SubjectEntity? = subjectDao.getSubjectById(subjectId)

    suspend fun insertSubject(subject: SubjectEntity) = subjectDao.insertSubject(subject)

    suspend fun updateSubject(subject: SubjectEntity) = subjectDao.updateSubject(subject)

    suspend fun deleteSubject(subjectId: String, cascadeDeleteDocs: Boolean) = withContext(Dispatchers.IO) {
        if (cascadeDeleteDocs) {
            val docs = documentDao.getAllDocumentsForSubject(subjectId).first()
            docs.forEach { doc ->
                storageManager.deletePhysicalFile(doc.relativeFilePath)
            }
            documentDao.deleteDocumentsBySubjectId(subjectId)
        } else {
            // Unlink documents from subject
            val docs = documentDao.getAllDocumentsForSubject(subjectId).first()
            docs.forEach { doc ->
                documentDao.updateDocument(doc.copy(subjectId = null, chapterId = null))
            }
        }
        chapterDao.deleteChaptersBySubjectId(subjectId)
        folderDao.deleteFoldersBySubjectId(subjectId)
        subjectDao.deleteSubjectById(subjectId)
    }

    suspend fun togglePinSubject(subjectId: String) = withContext(Dispatchers.IO) {
        val subject = subjectDao.getSubjectById(subjectId) ?: return@withContext
        subjectDao.updateSubject(subject.copy(isPinned = !subject.isPinned, updatedAt = System.currentTimeMillis()))
    }

    // Chapter operations
    fun observeChaptersForSubject(subjectId: String): Flow<List<ChapterEntity>> =
        chapterDao.getChaptersBySubject(subjectId)

    fun observeChapter(chapterId: String): Flow<ChapterEntity?> =
        chapterDao.observeChapterById(chapterId)

    suspend fun insertChapter(chapter: ChapterEntity) = chapterDao.insertChapter(chapter)

    suspend fun updateChapter(chapter: ChapterEntity) = chapterDao.updateChapter(chapter)

    suspend fun deleteChapter(chapterId: String, cascadeDeleteDocs: Boolean) = withContext(Dispatchers.IO) {
        if (cascadeDeleteDocs) {
            val docs = documentDao.getDocumentsByChapter(chapterId).first()
            docs.forEach { doc ->
                storageManager.deletePhysicalFile(doc.relativeFilePath)
            }
            documentDao.deleteDocumentsByChapterId(chapterId)
        } else {
            val docs = documentDao.getDocumentsByChapter(chapterId).first()
            docs.forEach { doc ->
                documentDao.updateDocument(doc.copy(chapterId = null))
            }
        }
        chapterDao.deleteChapterById(chapterId)
    }

    suspend fun updateChapterProgress(chapterId: String, status: String) = withContext(Dispatchers.IO) {
        val chapter = chapterDao.getChapterById(chapterId) ?: return@withContext
        chapterDao.updateChapter(chapter.copy(progressStatus = status, updatedAt = System.currentTimeMillis()))
    }

    // Document operations
    fun observeLooseDocumentsForSubject(subjectId: String): Flow<List<DocumentEntity>> =
        documentDao.getLooseDocumentsForSubject(subjectId)

    fun observeAllDocumentsForSubject(subjectId: String): Flow<List<DocumentEntity>> =
        documentDao.getAllDocumentsForSubject(subjectId)

    fun observeDocumentsForChapter(chapterId: String): Flow<List<DocumentEntity>> =
        documentDao.getDocumentsByChapter(chapterId)

    fun observeDocument(documentId: String): Flow<DocumentEntity?> =
        documentDao.observeDocumentById(documentId)

    suspend fun getDocument(documentId: String): DocumentEntity? =
        documentDao.getDocumentById(documentId)

    suspend fun insertDocument(document: DocumentEntity) =
        documentDao.insertDocument(document)

    suspend fun updateDocument(document: DocumentEntity) =
        documentDao.updateDocument(document)

    suspend fun toggleFavorite(documentId: String) = withContext(Dispatchers.IO) {
        val doc = documentDao.getDocumentById(documentId) ?: return@withContext
        documentDao.updateDocument(doc.copy(isFavorite = !doc.isFavorite, updatedAt = System.currentTimeMillis()))
    }

    suspend fun updateDocumentRevisionStatus(documentId: String, status: String) = withContext(Dispatchers.IO) {
        val doc = documentDao.getDocumentById(documentId) ?: return@withContext
        documentDao.updateDocument(doc.copy(revisionStatus = status, updatedAt = System.currentTimeMillis()))
    }

    suspend fun updateLastViewed(documentId: String, page: Int) = withContext(Dispatchers.IO) {
        val doc = documentDao.getDocumentById(documentId) ?: return@withContext
        documentDao.updateDocument(
            doc.copy(
                lastViewedPage = page,
                lastOpenedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun renameDocument(documentId: String, newTitle: String) = withContext(Dispatchers.IO) {
        val doc = documentDao.getDocumentById(documentId) ?: return@withContext
        documentDao.updateDocument(doc.copy(title = newTitle, updatedAt = System.currentTimeMillis()))
    }

    suspend fun moveDocument(documentId: String, targetSubjectId: String?, targetChapterId: String?) = withContext(Dispatchers.IO) {
        val doc = documentDao.getDocumentById(documentId) ?: return@withContext
        documentDao.updateDocument(
            doc.copy(
                subjectId = targetSubjectId,
                chapterId = targetChapterId,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteDocument(documentId: String) = withContext(Dispatchers.IO) {
        val doc = documentDao.getDocumentById(documentId) ?: return@withContext
        storageManager.deletePhysicalFile(doc.relativeFilePath)
        documentDao.deleteDocumentById(documentId)
    }

    suspend fun resetAllRevisionStatus() = documentDao.resetAllRevisionStatus()

    fun search(query: String): Flow<List<DocumentEntity>> = documentDao.searchDocuments(query)

    // Pre-populate standard Tunisian Baccalaureate (Bac Sciences) subjects & chapters
    suspend fun initializeTunisianBacSubjects() = withContext(Dispatchers.IO) {
        val subjects = listOf(
            SubjectEntity(
                id = UUID.randomUUID().toString(),
                name = "Sciences physiques",
                description = "Physique et Chimie - Baccalauréat",
                iconKey = "science",
                colorArgb = 0xFF2563EB.toInt(),
                isPinned = true,
                sortOrder = 1
            ),
            SubjectEntity(
                id = UUID.randomUUID().toString(),
                name = "Sciences de la vie et de la terre (SVT)",
                description = "Génétique, Immunologie, Reproduction, Système nerveux",
                iconKey = "biotech",
                colorArgb = 0xFF059669.toInt(),
                isPinned = true,
                sortOrder = 2
            ),
            SubjectEntity(
                id = UUID.randomUUID().toString(),
                name = "Mathématiques",
                description = "Analyse, Géométrie, Nombres complexes, Probabilités",
                iconKey = "calculate",
                colorArgb = 0xFFEA580C.toInt(),
                isPinned = true,
                sortOrder = 3
            ),
            SubjectEntity(
                id = UUID.randomUUID().toString(),
                name = "Français",
                description = "Textes, Essais, Production écrite",
                iconKey = "menu_book",
                colorArgb = 0xFF0D9488.toInt(),
                sortOrder = 4
            ),
            SubjectEntity(
                id = UUID.randomUUID().toString(),
                name = "Arabe",
                description = "الأدب والحضارة والإنشاء",
                iconKey = "auto_stories",
                colorArgb = 0xFF9333EA.toInt(),
                sortOrder = 5
            ),
            SubjectEntity(
                id = UUID.randomUUID().toString(),
                name = "Philosophie",
                description = "الفلسفة - نصوص ومفاهيم",
                iconKey = "psychology",
                colorArgb = 0xFFD97706.toInt(),
                sortOrder = 6
            ),
            SubjectEntity(
                id = UUID.randomUUID().toString(),
                name = "Anglais",
                description = "Vocabulary, Grammar and Writing",
                iconKey = "translate",
                colorArgb = 0xFF0284C7.toInt(),
                sortOrder = 7
            ),
            SubjectEntity(
                id = UUID.randomUUID().toString(),
                name = "Informatique",
                description = "Algorithmique et Programmation",
                iconKey = "code",
                colorArgb = 0xFF4F46E5.toInt(),
                sortOrder = 8
            )
        )

        subjectDao.insertSubjects(subjects)

        // Seed some chapters for the core subjects
        val physiqueSubject = subjects[0]
        val svtSubject = subjects[1]
        val mathsSubject = subjects[2]

        val physiqueChapters = listOf(
            ChapterEntity(subjectId = physiqueSubject.id, name = "Dipôle RC (Charge et décharge)", sortOrder = 1),
            ChapterEntity(subjectId = physiqueSubject.id, name = "Dipôle RL & Circuit RLC", sortOrder = 2),
            ChapterEntity(subjectId = physiqueSubject.id, name = "Ondes mécaniques progressives", sortOrder = 3),
            ChapterEntity(subjectId = physiqueSubject.id, name = "Cinétique chimique", sortOrder = 4),
            ChapterEntity(subjectId = physiqueSubject.id, name = "Équilibres acido-basiques", sortOrder = 5)
        )

        val svtChapters = listOf(
            ChapterEntity(subjectId = svtSubject.id, name = "Reproduction humaine", sortOrder = 1),
            ChapterEntity(subjectId = svtSubject.id, name = "Génétique formelle & humaine", sortOrder = 2),
            ChapterEntity(subjectId = svtSubject.id, name = "Immunologie & Défense de l'organisme", sortOrder = 3),
            ChapterEntity(subjectId = svtSubject.id, name = "Fonctionnement du système nerveux", sortOrder = 4)
        )

        val mathsChapters = listOf(
            ChapterEntity(subjectId = mathsSubject.id, name = "Limites et Continuité", sortOrder = 1),
            ChapterEntity(subjectId = mathsSubject.id, name = "Dérivation et Étude de fonctions", sortOrder = 2),
            ChapterEntity(subjectId = mathsSubject.id, name = "Nombres complexes", sortOrder = 3),
            ChapterEntity(subjectId = mathsSubject.id, name = "Primitives et Calcul intégral", sortOrder = 4),
            ChapterEntity(subjectId = mathsSubject.id, name = "Probabilités & Statistiques", sortOrder = 5)
        )

        chapterDao.insertChapters(physiqueChapters + svtChapters + mathsChapters)
    }
}
