package com.example.ui.screens.favorites

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.entity.DocumentEntity
import com.example.ui.BinderViewModel
import com.example.ui.components.DocumentCard
import com.example.ui.components.MoveDocumentDialog
import com.example.ui.components.RenameDialog

@Composable
fun FavoritesScreen(
    viewModel: BinderViewModel,
    onNavigateToDocument: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val favorites by viewModel.favoriteDocuments.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()

    var filterType by remember { mutableStateOf("ALL") } // ALL, PDF, IMAGE
    var sortOrder by remember { mutableStateOf("DATE") } // DATE, TITLE
    var sortMenuOpen by remember { mutableStateOf(false) }

    var documentToRename by remember { mutableStateOf<DocumentEntity?>(null) }
    var documentToMove by remember { mutableStateOf<DocumentEntity?>(null) }

    val filteredList = favorites
        .filter {
            when (filterType) {
                "PDF" -> it.mimeType.equals("application/pdf", ignoreCase = true)
                "IMAGE" -> it.mimeType.startsWith("image/", ignoreCase = true)
                else -> true
            }
        }
        .sortedWith { d1, d2 ->
            if (sortOrder == "TITLE") d1.title.compareTo(d2.title, ignoreCase = true)
            else d2.updatedAt.compareTo(d1.updatedAt)
        }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("favorites_screen")
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Title and Sort
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.favorites_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${filteredList.size} document(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box {
                IconButton(onClick = { sortMenuOpen = true }) {
                    Icon(Icons.Default.Sort, contentDescription = "Sort")
                }
                DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Trier par date") },
                        onClick = { sortOrder = "DATE"; sortMenuOpen = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Trier par titre (A-Z)") },
                        onClick = { sortOrder = "TITLE"; sortMenuOpen = false }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Filter chips (All, PDF, Images)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = filterType == "ALL",
                onClick = { filterType = "ALL" },
                label = { Text(stringResource(R.string.search_filter_all)) }
            )
            FilterChip(
                selected = filterType == "PDF",
                onClick = { filterType = "PDF" },
                label = { Text(stringResource(R.string.search_filter_pdf)) }
            )
            FilterChip(
                selected = filterType == "IMAGE",
                onClick = { filterType = "IMAGE" },
                label = { Text(stringResource(R.string.search_filter_image)) }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color(0xFFE11D48).copy(alpha = 0.5f),
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.favorites_empty),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.favorites_empty_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredList, key = { it.id }) { doc ->
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
