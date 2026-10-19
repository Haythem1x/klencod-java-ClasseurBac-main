package com.example.ui.screens.search

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.entity.DocumentEntity
import com.example.ui.BinderViewModel
import com.example.ui.components.DocumentCard
import com.example.ui.components.MoveDocumentDialog
import com.example.ui.components.RenameDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: BinderViewModel,
    onBack: () -> Unit,
    onNavigateToDocument: (String) -> Unit,
    onNavigateToSubject: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val allDocs by viewModel.allDocuments.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf("ALL") } // ALL, PDF, IMAGE
    var filterStatus by remember { mutableStateOf("ALL") } // ALL, TODO, IN_PROGRESS, MASTERED
    var filterFavoritesOnly by remember { mutableStateOf(false) }

    var documentToRename by remember { mutableStateOf<DocumentEntity?>(null) }
    var documentToMove by remember { mutableStateOf<DocumentEntity?>(null) }

    val searchResults = if (searchQuery.isBlank()) {
        emptyList()
    } else {
        val query = searchQuery.trim().lowercase()
        allDocs.filter { doc ->
            val matchesText = doc.title.lowercase().contains(query) ||
                    doc.fileName.lowercase().contains(query) ||
                    (doc.description?.lowercase()?.contains(query) == true)

            val matchesType = when (filterType) {
                "PDF" -> doc.mimeType.equals("application/pdf", ignoreCase = true)
                "IMAGE" -> doc.mimeType.startsWith("image/", ignoreCase = true)
                else -> true
            }

            val matchesStatus = if (filterStatus == "ALL") true else doc.revisionStatus == filterStatus
            val matchesFav = if (filterFavoritesOnly) doc.isFavorite else true

            matchesText && matchesType && matchesStatus && matchesFav
        }
    }

    val matchedSubjects = if (searchQuery.isNotBlank()) {
        val query = searchQuery.trim().lowercase()
        subjects.filter { it.name.lowercase().contains(query) || (it.description?.lowercase()?.contains(query) == true) }
    } else emptyList()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.search_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .testTag("search_screen")
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(stringResource(R.string.search_placeholder)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_input_field")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = filterType == "ALL",
                        onClick = { filterType = "ALL" },
                        label = { Text(stringResource(R.string.search_filter_all)) }
                    )
                }
                item {
                    FilterChip(
                        selected = filterType == "PDF",
                        onClick = { filterType = "PDF" },
                        label = { Text(stringResource(R.string.search_filter_pdf)) }
                    )
                }
                item {
                    FilterChip(
                        selected = filterType == "IMAGE",
                        onClick = { filterType = "IMAGE" },
                        label = { Text(stringResource(R.string.search_filter_image)) }
                    )
                }
                item {
                    FilterChip(
                        selected = filterFavoritesOnly,
                        onClick = { filterFavoritesOnly = !filterFavoritesOnly },
                        label = { Text(stringResource(R.string.nav_favorites)) }
                    )
                }
                item {
                    FilterChip(
                        selected = filterStatus == "MASTERED",
                        onClick = { filterStatus = if (filterStatus == "MASTERED") "ALL" else "MASTERED" },
                        label = { Text(stringResource(R.string.status_mastered)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (searchQuery.isBlank()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tapez des mots-clés pour rechercher vos cours, résumés et fiches",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (searchResults.isEmpty() && matchedSubjects.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.search_no_results, searchQuery),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (matchedSubjects.isNotEmpty()) {
                        item {
                            Text(
                                text = "Matières trouvées (${matchedSubjects.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        items(matchedSubjects) { sub ->
                            Surface(
                                onClick = { onNavigateToSubject(sub.id) },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = sub.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "Voir la matière",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }

                    if (searchResults.isNotEmpty()) {
                        item {
                            Text(
                                text = "Documents trouvés (${searchResults.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        items(searchResults, key = { it.id }) { doc ->
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
                }
            }
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
