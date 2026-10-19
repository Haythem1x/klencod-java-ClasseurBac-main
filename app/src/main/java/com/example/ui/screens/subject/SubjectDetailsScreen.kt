package com.example.ui.screens.subject

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.entity.ChapterEntity
import com.example.data.entity.DocumentEntity
import com.example.ui.BinderViewModel
import com.example.ui.components.AddEditChapterDialog
import com.example.ui.components.DocumentCard
import com.example.ui.components.IconAndColorHelper
import com.example.ui.components.MoveDocumentDialog
import com.example.ui.components.RenameDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectDetailsScreen(
    subjectId: String,
    viewModel: BinderViewModel,
    onBack: () -> Unit,
    onNavigateToChapter: (String) -> Unit,
    onNavigateToDocument: (String) -> Unit,
    onNavigateToImport: (subjectId: String?, chapterId: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val subject = subjects.firstOrNull { it.id == subjectId }
    val allDocs by viewModel.allDocuments.collectAsStateWithLifecycle()

    val chaptersFlow = remember(subjectId) { viewModel.repository.observeChaptersForSubject(subjectId) }
    val chapters by chaptersFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    val looseDocsFlow = remember(subjectId) { viewModel.repository.observeLooseDocumentsForSubject(subjectId) }
    val looseDocs by looseDocsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    var showAddChapterDialog by remember { mutableStateOf(false) }
    var chapterToEdit by remember { mutableStateOf<ChapterEntity?>(null) }
    var chapterToDelete by remember { mutableStateOf<ChapterEntity?>(null) }

    var documentToRename by remember { mutableStateOf<DocumentEntity?>(null) }
    var documentToMove by remember { mutableStateOf<DocumentEntity?>(null) }

    if (subject == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.viewer_error_render))
        }
        return
    }

    val subjectColor = IconAndColorHelper.getColor(subject.colorArgb)
    val subjectIcon = IconAndColorHelper.getIcon(subject.iconKey)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = subject.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.binder_title),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToImport(subjectId, null) },
                containerColor = subjectColor,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_import_to_subject")
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = stringResource(R.string.action_import))
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Subject Header Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = subjectColor.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = subjectColor,
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = subjectIcon,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = subject.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (!subject.description.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = subject.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${chapters.size} ${stringResource(R.string.subject_chapters).lowercase()} • ${allDocs.count { it.subjectId == subjectId }} ${stringResource(R.string.home_stats_documents).lowercase()}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = subjectColor
                            )
                        }
                    }
                }
            }

            // Chapters Section Header & Add Button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.subject_chapters),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedButton(
                        onClick = { showAddChapterDialog = true },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.chapter_add), fontSize = 12.sp)
                    }
                }
            }

            // Chapters List
            if (chapters.isEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Aucun chapitre pour cette matière.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(chapters, key = { it.id }) { chapter ->
                    val docsInChapter = allDocs.count { it.chapterId == chapter.id }
                    ChapterCard(
                        chapter = chapter,
                        subjectColor = subjectColor,
                        documentCount = docsInChapter,
                        onClick = { onNavigateToChapter(chapter.id) },
                        onEdit = { chapterToEdit = chapter },
                        onDelete = { chapterToDelete = chapter },
                        onChangeStatus = { newStatus -> viewModel.updateChapterProgress(chapter.id, newStatus) }
                    )
                }
            }

            // Loose Documents in Subject Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.subject_loose_docs),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${looseDocs.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (looseDocs.isEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Aucun document direct. Vous pouvez en importer ici ou dans un chapitre.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(looseDocs, key = { it.id }) { doc ->
                    DocumentCard(
                        document = doc,
                        onClick = { onNavigateToDocument(doc.id) },
                        onToggleFavorite = { viewModel.toggleFavorite(doc.id) },
                        onRename = { documentToRename = doc },
                        onMove = { documentToMove = doc },
                        onDelete = { viewModel.deleteDocument(doc.id) },
                        onShare = {},
                        onChangeStatus = { viewModel.updateRevisionStatus(doc.id, it) },
                        isGridView = false
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(60.dp)) }
        }
    }

    if (showAddChapterDialog) {
        AddEditChapterDialog(
            subjects = subjects,
            preselectedSubjectId = subjectId,
            onDismiss = { showAddChapterDialog = false },
            onConfirm = { subId, name, desc, iconKey, colorArgb ->
                viewModel.createChapter(subId, name, desc, iconKey, colorArgb)
                showAddChapterDialog = false
            }
        )
    }

    chapterToEdit?.let { chap ->
        AddEditChapterDialog(
            subjects = subjects,
            preselectedSubjectId = chap.subjectId,
            initialChapter = chap,
            onDismiss = { chapterToEdit = null },
            onConfirm = { subId, name, desc, iconKey, colorArgb ->
                viewModel.updateChapter(
                    chap.copy(
                        subjectId = subId,
                        name = name,
                        description = desc,
                        iconKey = iconKey,
                        colorArgb = colorArgb,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                chapterToEdit = null
            }
        )
    }

    chapterToDelete?.let { chap ->
        AlertDialog(
            onDismissRequest = { chapterToDelete = null },
            title = { Text("Supprimer le chapitre ?", fontWeight = FontWeight.Bold) },
            text = { Text("Voulez-vous supprimer « ${chap.name} » ? Ses documents seront conservés dans la matière principale.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteChapter(chap.id, cascade = false)
                        chapterToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { chapterToDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    documentToRename?.let { doc ->
        RenameDialog(
            initialValue = doc.title,
            title = stringResource(R.string.action_rename),
            onDismiss = { documentToRename = null },
            onConfirm = {
                viewModel.renameDocument(doc.id, it)
                documentToRename = null
            }
        )
    }

    documentToMove?.let { doc ->
        MoveDocumentDialog(
            subjects = subjects,
            chapters = chapters,
            currentSubjectId = doc.subjectId,
            currentChapterId = doc.chapterId,
            onDismiss = { documentToMove = null },
            onConfirm = { subId, chapId ->
                viewModel.moveDocument(doc.id, subId, chapId)
                documentToMove = null
            }
        )
    }
}

@Composable
fun ChapterCard(
    chapter: ChapterEntity,
    subjectColor: Color,
    documentCount: Int,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onChangeStatus: (String) -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }

    val (statusText, statusColor, statusIcon) = when (chapter.progressStatus) {
        "COMPLETED" -> Triple("Terminé", Color(0xFF10B981), Icons.Default.CheckCircle)
        "IN_PROGRESS" -> Triple("En cours", Color(0xFFF59E0B), Icons.Default.HourglassTop)
        else -> Triple("Non commencé", Color(0xFF94A3B8), Icons.Default.RadioButtonUnchecked)
    }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = subjectColor.copy(alpha = 0.15f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = null,
                        tint = subjectColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chapter.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!chapter.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = chapter.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = statusIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = statusText, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "$documentCount doc(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }

            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Marquer Non commencé") },
                        onClick = { menuOpen = false; onChangeStatus("NOT_STARTED") }
                    )
                    DropdownMenuItem(
                        text = { Text("Marquer En cours") },
                        onClick = { menuOpen = false; onChangeStatus("IN_PROGRESS") }
                    )
                    DropdownMenuItem(
                        text = { Text("Marquer Terminé") },
                        onClick = { menuOpen = false; onChangeStatus("COMPLETED") }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_edit)) },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = { menuOpen = false; onEdit() }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { menuOpen = false; onDelete() }
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
