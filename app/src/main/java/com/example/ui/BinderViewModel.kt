package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.backup.BackupManager
import com.example.data.backup.BackupSummary
import com.example.data.database.AppDatabase
import com.example.data.entity.ChapterEntity
import com.example.data.entity.DocumentEntity
import com.example.data.entity.SubjectEntity
import com.example.data.preferences.AppUserPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.BinderRepository
import com.example.data.storage.DocumentStorageManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream

sealed class UiEvent {
    data class ShowMessage(val message: String) : UiEvent()
    data class Error(val error: String) : UiEvent()
}

class BinderViewModel(application: Application) : AndroidViewModel(application) {

    val database = AppDatabase.getInstance(application)
    val storageManager = DocumentStorageManager(application)
    val preferencesRepository = UserPreferencesRepository(application)
    val repository = BinderRepository(database, storageManager)
    val backupManager = BackupManager(application, database, storageManager)
    val aiService = com.example.data.ai.GeminiAiService()
    val sheetGenerator = com.example.data.ai.SheetDocumentGenerator(application, storageManager)

    private val _chatMessages = MutableStateFlow<List<com.example.data.ai.ChatMessage>>(
        listOf(
            com.example.data.ai.ChatMessage(
                role = "model",
                text = "Bonjour ! Je suis ton Assistant Bac IA. Je peux t'aider à réviser, t'expliquer n'importe quelle notion du programme tunisien, résoudre un exercice ou te générer des fiches de révision complètes en PDF ou en image."
            )
        )
    )
    val chatMessages: StateFlow<List<com.example.data.ai.ChatMessage>> = _chatMessages.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    val userPreferences: StateFlow<AppUserPreferences> = preferencesRepository.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppUserPreferences()
        )

    val subjects: StateFlow<List<SubjectEntity>> = repository.allSubjects
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allDocuments: StateFlow<List<DocumentEntity>> = repository.allDocuments
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val recentDocuments: StateFlow<List<DocumentEntity>> = repository.recentDocuments
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val favoriteDocuments: StateFlow<List<DocumentEntity>> = repository.favoriteDocuments
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val totalDocsCount: StateFlow<Int> = repository.totalDocumentsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalSubjectsCount: StateFlow<Int> = repository.totalSubjectsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val masteredDocsCount: StateFlow<Int> = repository.masteredDocumentsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    fun setLanguage(lang: String) = viewModelScope.launch {
        preferencesRepository.setLanguage(lang)
    }

    fun setTheme(theme: String) = viewModelScope.launch {
        preferencesRepository.setThemeMode(theme)
    }

    fun setViewMode(mode: String) = viewModelScope.launch {
        preferencesRepository.setViewMode(mode)
    }

    fun setSortOrder(order: String) = viewModelScope.launch {
        preferencesRepository.setSortOrder(order)
    }

    fun completeOnboarding() = viewModelScope.launch {
        preferencesRepository.setOnboardingCompleted(true)
    }

    fun resetPreferences() = viewModelScope.launch {
        preferencesRepository.resetPreferences()
        _events.emit(UiEvent.ShowMessage("Préférences réinitialisées"))
    }

    fun initBacPack() = viewModelScope.launch {
        try {
            repository.initializeTunisianBacSubjects()
            _events.emit(UiEvent.ShowMessage("Matières du Bac créées avec succès"))
        } catch (e: Exception) {
            _events.emit(UiEvent.Error("Erreur lors de la création : ${e.localizedMessage}"))
        }
    }

    fun createSubject(name: String, description: String?, iconKey: String, colorArgb: Int, isPinned: Boolean) =
        viewModelScope.launch {
            try {
                if (name.isBlank()) {
                    _events.emit(UiEvent.Error("Le nom de la matière ne peut être vide"))
                    return@launch
                }
                val subject = SubjectEntity(
                    name = name.trim(),
                    description = description?.trim(),
                    iconKey = iconKey,
                    colorArgb = colorArgb,
                    isPinned = isPinned
                )
                repository.insertSubject(subject)
                _events.emit(UiEvent.ShowMessage("Matière créée"))
            } catch (e: Exception) {
                _events.emit(UiEvent.Error(e.localizedMessage ?: "Erreur"))
            }
        }

    fun updateSubject(subject: SubjectEntity) = viewModelScope.launch {
        repository.updateSubject(subject)
        _events.emit(UiEvent.ShowMessage("Matière mise à jour"))
    }

    fun deleteSubject(subjectId: String, cascade: Boolean) = viewModelScope.launch {
        repository.deleteSubject(subjectId, cascade)
        _events.emit(UiEvent.ShowMessage("Matière supprimée"))
    }

    fun togglePinSubject(subjectId: String) = viewModelScope.launch {
        repository.togglePinSubject(subjectId)
    }

    fun createChapter(subjectId: String, name: String, description: String?, iconKey: String, colorArgb: Int) =
        viewModelScope.launch {
            if (name.isBlank()) {
                _events.emit(UiEvent.Error("Le nom du chapitre ne peut être vide"))
                return@launch
            }
            val chapter = ChapterEntity(
                subjectId = subjectId,
                name = name.trim(),
                description = description?.trim(),
                iconKey = iconKey,
                colorArgb = colorArgb
            )
            repository.insertChapter(chapter)
            _events.emit(UiEvent.ShowMessage("Chapitre créé"))
        }

    fun updateChapter(chapter: ChapterEntity) = viewModelScope.launch {
        repository.updateChapter(chapter)
    }

    fun deleteChapter(chapterId: String, cascade: Boolean) = viewModelScope.launch {
        repository.deleteChapter(chapterId, cascade)
        _events.emit(UiEvent.ShowMessage("Chapitre supprimé"))
    }

    fun updateChapterProgress(chapterId: String, status: String) = viewModelScope.launch {
        repository.updateChapterProgress(chapterId, status)
    }

    fun importDocument(
        uri: Uri,
        customTitle: String?,
        subjectId: String?,
        chapterId: String?
    ) = viewModelScope.launch {
        _isImporting.value = true
        try {
            val result = storageManager.saveDocumentFromUri(uri, customTitle)
            val doc = DocumentEntity(
                title = result.title,
                fileName = result.fileName,
                relativeFilePath = result.relativeFilePath,
                mimeType = result.mimeType,
                fileSizeBytes = result.fileSizeBytes,
                contentHash = result.contentHash,
                subjectId = subjectId,
                chapterId = chapterId,
                pageCount = result.pageCount,
                lastViewedPage = 0
            )
            repository.insertDocument(doc)
            _events.emit(UiEvent.ShowMessage("Document importé : ${doc.title}"))
        } catch (e: Exception) {
            _events.emit(UiEvent.Error("Erreur d'importation : ${e.localizedMessage}"))
        } finally {
            _isImporting.value = false
        }
    }

    fun importCapturedImage(
        file: File,
        customTitle: String?,
        subjectId: String?,
        chapterId: String?
    ) = viewModelScope.launch {
        _isImporting.value = true
        try {
            file.inputStream().use { stream ->
                val result = storageManager.saveDocumentFromStream(stream, file.name, "image/jpeg")
                val doc = DocumentEntity(
                    title = customTitle?.takeIf { it.isNotBlank() } ?: "Photo_${System.currentTimeMillis()}",
                    fileName = result.fileName,
                    relativeFilePath = result.relativeFilePath,
                    mimeType = "image/jpeg",
                    fileSizeBytes = result.fileSizeBytes,
                    contentHash = result.contentHash,
                    subjectId = subjectId,
                    chapterId = chapterId,
                    pageCount = 1
                )
                repository.insertDocument(doc)
                _events.emit(UiEvent.ShowMessage("Photo enregistrée dans vos cours"))
            }
        } catch (e: Exception) {
            _events.emit(UiEvent.Error("Erreur photo : ${e.localizedMessage}"))
        } finally {
            _isImporting.value = false
            file.delete()
        }
    }

    fun toggleFavorite(documentId: String) = viewModelScope.launch {
        repository.toggleFavorite(documentId)
    }

    fun updateRevisionStatus(documentId: String, status: String) = viewModelScope.launch {
        repository.updateDocumentRevisionStatus(documentId, status)
    }

    fun updateLastViewed(documentId: String, page: Int) = viewModelScope.launch {
        repository.updateLastViewed(documentId, page)
    }

    fun renameDocument(documentId: String, newTitle: String) = viewModelScope.launch {
        if (newTitle.isNotBlank()) {
            repository.renameDocument(documentId, newTitle.trim())
            _events.emit(UiEvent.ShowMessage("Document renommé"))
        }
    }

    fun moveDocument(documentId: String, targetSubjectId: String?, targetChapterId: String?) = viewModelScope.launch {
        repository.moveDocument(documentId, targetSubjectId, targetChapterId)
        _events.emit(UiEvent.ShowMessage("Document déplacé"))
    }

    fun deleteDocument(documentId: String) = viewModelScope.launch {
        repository.deleteDocument(documentId)
        _events.emit(UiEvent.ShowMessage("Document supprimé"))
    }

    fun resetAllRevisions() = viewModelScope.launch {
        repository.resetAllRevisionStatus()
        _events.emit(UiEvent.ShowMessage("Toutes les révisions ont été remises à l'état initial"))
    }

    fun clearCache() = viewModelScope.launch {
        storageManager.clearCache()
        _events.emit(UiEvent.ShowMessage("Cache vidé avec succès"))
    }

    fun exportBackup(uri: Uri, onResult: (Boolean, String) -> Unit) = viewModelScope.launch {
        val res = backupManager.exportCompleteBackup(uri)
        if (res.isSuccess) {
            onResult(true, "Sauvegarde réussie (${res.getOrNull()} documents archivés)")
        } else {
            onResult(false, res.exceptionOrNull()?.localizedMessage ?: "Erreur de sauvegarde")
        }
    }

    fun inspectBackup(uri: Uri, onResult: (BackupSummary?) -> Unit) = viewModelScope.launch {
        val res = backupManager.inspectBackup(uri)
        onResult(res.getOrNull())
    }

    fun restoreBackup(uri: Uri, replaceCurrent: Boolean, onResult: (Boolean, String) -> Unit) = viewModelScope.launch {
        val res = backupManager.restoreBackup(uri, replaceCurrent)
        if (res.isSuccess) {
            onResult(true, "Restauration terminée (${res.getOrNull()} documents restaurés)")
        } else {
            onResult(false, res.exceptionOrNull()?.localizedMessage ?: "Erreur de restauration")
        }
    }

    fun setGeminiApiKey(apiKey: String) = viewModelScope.launch {
        preferencesRepository.setGeminiApiKey(apiKey)
    }

    suspend fun testGeminiApiKey(apiKey: String): Result<String> {
        return aiService.testApiKey(apiKey)
    }

    // --- AI Assistant Features ---

    fun sendChatMessage(prompt: String) = viewModelScope.launch {
        if (prompt.isBlank()) return@launch
        val userMsg = com.example.data.ai.ChatMessage(role = "user", text = prompt)
        _chatMessages.value = _chatMessages.value + userMsg
        _isAiLoading.value = true

        val currentKey = userPreferences.value.geminiApiKey
        val result = aiService.generateResponse(prompt, _chatMessages.value, currentKey)
        _isAiLoading.value = false

        if (result.isSuccess) {
            val reply = result.getOrNull() ?: ""
            _chatMessages.value = _chatMessages.value + com.example.data.ai.ChatMessage(role = "model", text = reply)
        } else {
            val error = result.exceptionOrNull()?.localizedMessage ?: "Erreur réseau"
            val errorReply = "Désolé, une erreur est survenue lors de l'appel à l'Assistant IA : $error\n\nVous pouvez vérifier ou modifier votre clé Gemini dans les Paramètres ou via l'icône de clé en haut."
            _chatMessages.value = _chatMessages.value + com.example.data.ai.ChatMessage(role = "model", text = errorReply)
        }
    }

    fun generateSheet(
        subjectName: String,
        chapterName: String?,
        sheetType: String,
        topicDetails: String,
        onGenerated: (title: String, content: String) -> Unit
    ) = viewModelScope.launch {
        _isAiLoading.value = true
        val title = "Fiche Bac - ${topicDetails.take(30).trim()}"
        val currentKey = userPreferences.value.geminiApiKey
        val result = if (aiService.hasValidApiKey(currentKey)) {
            aiService.generateRevisionSheetContent(subjectName, chapterName, sheetType, topicDetails, currentKey)
        } else {
            // High quality pre-crafted Bac template so user can test PDF/Image generation without API key!
            Result.success(
                """
                # FICHE DE RÉVISION BAC : ${topicDetails.uppercase()}
                Matière : $subjectName ${if (chapterName != null) "• $chapterName" else ""}
                Type : $sheetType

                ## 1. OBJECTIFS & DÉFINITIONS CLÉS
                • Comprendre le comportement du système et ses grandeurs caractéristiques.
                • Établir l'équation différentielle régissant le phénomène.
                • Connaître les conditions initiales et les régimes transitoire et permanent.

                ## 2. FORMULES & RÈGLES ESSENTIELLES
                • Constante de temps : τ = R · C (en secondes, avec R en Ω et C en Farads).
                • Équation différentielle de charge : u_C(t) + τ · (du_C/dt) = E.
                • Solution : u_C(t) = E · (1 - e^(-t/τ)). À t = τ, u_C = 0.63 · E.
                • Énergie emmagasinée : E_C = (1/2) · C · u_C².

                ## 3. MÉTHODOLOGIE DE RÉSOLUTION BAC
                1. Repérer le schéma du circuit et orienter les tensions en convention récepteur.
                2. Appliquer la loi des mailles (loi d'additivité des tensions).
                3. Remplacer i(t) par C · (du_C/dt) ou dq/dt.
                4. Déterminer graphiquement τ par la méthode de la tangente à l'origine ou par 0.63 · E.

                ## 4. PIÈGES FRÉQUENTS & CONSEILS DE L'EXAMINATEUR
                • Ne pas confondre la charge (croissance exponentielle) et la décharge (décroissance).
                • Attention aux unités : convertir les microfarads (μF) en Farads (10⁻⁶ F) et millisecondes en secondes.
                • Soigner la justification des asymptotes horizontales.
                """.trimIndent()
            )
        }
        _isAiLoading.value = false

        if (result.isSuccess) {
            val content = result.getOrNull() ?: ""
            _chatMessages.value = _chatMessages.value + com.example.data.ai.ChatMessage(
                role = "model",
                text = "Voici votre fiche de révision générée ! Vous pouvez maintenant l'enregistrer en PDF ou en Image dans votre classeur.",
                isSheet = true,
                sheetTitle = title
            )
            onGenerated(title, content)
        } else {
            _events.emit(UiEvent.Error(result.exceptionOrNull()?.localizedMessage ?: "Erreur de génération"))
        }
    }

    fun saveGeneratedSheetAsPdf(
        title: String,
        subjectId: String?,
        chapterId: String?,
        subjectName: String,
        chapterName: String?,
        content: String,
        onCompleted: (String) -> Unit
    ) = viewModelScope.launch {
        try {
            _isAiLoading.value = true
            val savedDoc = sheetGenerator.generatePdfSheet(title, subjectName, chapterName, content)
            val docEntity = DocumentEntity(
                title = savedDoc.title,
                fileName = savedDoc.fileName,
                relativeFilePath = savedDoc.relativeFilePath,
                mimeType = savedDoc.mimeType,
                fileSizeBytes = savedDoc.fileSizeBytes,
                contentHash = savedDoc.contentHash,
                subjectId = subjectId,
                chapterId = chapterId,
                pageCount = savedDoc.pageCount
            )
            repository.insertDocument(docEntity)
            _events.emit(UiEvent.ShowMessage("Fiche PDF enregistrée dans votre classeur"))
            onCompleted(docEntity.id)
        } catch (e: Exception) {
            _events.emit(UiEvent.Error("Erreur PDF: ${e.localizedMessage}"))
        } finally {
            _isAiLoading.value = false
        }
    }

    fun saveGeneratedSheetAsImage(
        title: String,
        subjectId: String?,
        chapterId: String?,
        subjectName: String,
        chapterName: String?,
        content: String,
        onCompleted: (String) -> Unit
    ) = viewModelScope.launch {
        try {
            _isAiLoading.value = true
            val savedDoc = sheetGenerator.generateImageSheet(title, subjectName, chapterName, content)
            val docEntity = DocumentEntity(
                title = savedDoc.title,
                fileName = savedDoc.fileName,
                relativeFilePath = savedDoc.relativeFilePath,
                mimeType = savedDoc.mimeType,
                fileSizeBytes = savedDoc.fileSizeBytes,
                contentHash = savedDoc.contentHash,
                subjectId = subjectId,
                chapterId = chapterId,
                pageCount = 1
            )
            repository.insertDocument(docEntity)
            _events.emit(UiEvent.ShowMessage("Fiche Image enregistrée dans votre classeur"))
            onCompleted(docEntity.id)
        } catch (e: Exception) {
            _events.emit(UiEvent.Error("Erreur Image: ${e.localizedMessage}"))
        } finally {
            _isAiLoading.value = false
        }
    }

    fun clearChat() {
        _chatMessages.value = listOf(
            com.example.data.ai.ChatMessage(
                role = "model",
                text = "Conversation réinitialisée. Pose ta question ou génère une nouvelle fiche de révision pour le Bac !"
            )
        )
    }
}
