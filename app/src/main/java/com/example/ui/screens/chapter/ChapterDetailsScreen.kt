package com.example.ui.screens.chapter

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.entity.DocumentEntity
import com.example.ui.BinderViewModel
import com.example.ui.components.DocumentCard
import com.example.ui.components.IconAndColorHelper
import com.example.ui.components.MoveDocumentDialog
import com.example.ui.components.RenameDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterDetailsScreen(
    chapterId: String,
    viewModel: BinderViewModel,
    onBack: () -> Unit,
    onNavigateToDocument: (String) -> Unit,
    onNavigateToImport: (subjectId: String?, chapterId: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val chapterFlow = remember(chapterId) { viewModel.repository.observeChapter(chapterId) }
    val chapter by chapterFlow.collectAsStateWithLifecycle(initialValue = null)

    val docsFlow = remember(chapterId) { viewModel.repository.observeDocumentsForChapter(chapterId) }
    val docs by docsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val subject = subjects.firstOrNull { it.id == chapter?.subjectId }

    var statusMenuOpen by remember { mutableStateOf(false) }
    var documentToRename by remember { mutableStateOf<DocumentEntity?>(null) }
    var documentToMove by remember { mutableStateOf<DocumentEntity?>(null) }

    if (chapter == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.viewer_error_render))
        }
        return
    }

    val (statusLabel, statusColor) = when (chapter!!.progressStatus) {
        "COMPLETED" -> stringResource(R.string.chapter_status_completed) to Color(0xFF10B981)
        "IN_PROGRESS" -> stringResource(R.string.chapter_status_in_progress) to Color(0xFFF59E0B)
        else -> stringResource(R.string.chapter_status_not_started) to Color(0xFF94A3B8)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = chapter!!.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = subject?.name ?: "",
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
                onClick = { onNavigateToImport(chapter!!.subjectId, chapter!!.id) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_import_to_chapter")
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
            // Chapter info & status card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.chapter_status),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            Box {
                                Surface(
                                    onClick = { statusMenuOpen = true },
                                    color = statusColor.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(statusColor, shape = RoundedCornerShape(4.dp))
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = statusLabel, color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                DropdownMenu(expanded = statusMenuOpen, onDismissRequest = { statusMenuOpen = false }) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.chapter_status_not_started)) },
                                        onClick = {
                                            viewModel.updateChapterProgress(chapter!!.id, "NOT_STARTED")
                                            statusMenuOpen = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.chapter_status_in_progress)) },
                                        onClick = {
                                            viewModel.updateChapterProgress(chapter!!.id, "IN_PROGRESS")
                                            statusMenuOpen = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.chapter_status_completed)) },
                                        onClick = {
                                            viewModel.updateChapterProgress(chapter!!.id, "COMPLETED")
                                            statusMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }

                        if (!chapter!!.description.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = chapter!!.description!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Document Count
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.home_stats_documents),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${docs.size} document(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (docs.isEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                        shadowElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = stringResource(R.string.chapter_empty),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.chapter_empty_action),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { onNavigateToImport(chapter!!.subjectId, chapter!!.id) }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.home_action_import_doc))
                            }
                        }
                    }
                }
            } else {
                items(docs, key = { it.id }) { doc ->
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
            chapters = emptyList(),
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
