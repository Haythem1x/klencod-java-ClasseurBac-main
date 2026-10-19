package com.example.ui.screens.binder

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.entity.SubjectEntity
import com.example.ui.BinderViewModel
import com.example.ui.components.AddEditSubjectDialog
import com.example.ui.components.SubjectFolderCard

@Composable
fun BinderScreen(
    viewModel: BinderViewModel,
    onNavigateToSubject: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val allDocs by viewModel.allDocuments.collectAsStateWithLifecycle()
    val prefs by viewModel.userPreferences.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var isGridView by remember { mutableStateOf(prefs.viewMode == "grid") }
    var sortOrder by remember { mutableStateOf(prefs.sortOrder) }
    var sortMenuOpen by remember { mutableStateOf(false) }

    var subjectToEdit by remember { mutableStateOf<SubjectEntity?>(null) }
    var subjectToDelete by remember { mutableStateOf<SubjectEntity?>(null) }
    var cascadeDelete by remember { mutableStateOf(true) }
    var showCreateDialog by remember { mutableStateOf(false) }

    // Filter and sort
    val filteredSubjects = subjects
        .filter { it.name.contains(searchQuery, ignoreCase = true) || (it.description?.contains(searchQuery, ignoreCase = true) == true) }
        .sortedWith { s1, s2 ->
            if (s1.isPinned != s2.isPinned) {
                if (s1.isPinned) -1 else 1
            } else when (sortOrder) {
                "updated" -> s2.updatedAt.compareTo(s1.updatedAt)
                "count" -> {
                    val count1 = allDocs.count { it.subjectId == s1.id }
                    val count2 = allDocs.count { it.subjectId == s2.id }
                    count2.compareTo(count1)
                }
                else -> s1.name.compareTo(s2.name, ignoreCase = true)
            }
        }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_subject")
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.binder_add_subject))
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Search bar & controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.binder_search_hint)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("binder_search_input")
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Sort dropdown
                Box {
                    IconButton(onClick = { sortMenuOpen = true }) {
                        Icon(Icons.Default.Sort, contentDescription = "Sort")
                    }
                    DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.binder_sort_name)) },
                            onClick = {
                                sortOrder = "name"
                                viewModel.setSortOrder("name")
                                sortMenuOpen = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.binder_sort_updated)) },
                            onClick = {
                                sortOrder = "updated"
                                viewModel.setSortOrder("updated")
                                sortMenuOpen = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.binder_sort_count)) },
                            onClick = {
                                sortOrder = "count"
                                viewModel.setSortOrder("count")
                                sortMenuOpen = false
                            }
                        )
                    }
                }

                // Grid / List toggle
                IconButton(onClick = {
                    isGridView = !isGridView
                    viewModel.setViewMode(if (isGridView) "grid" else "list")
                }) {
                    Icon(
                        imageVector = if (isGridView) Icons.Default.List else Icons.Default.GridView,
                        contentDescription = "Toggle View"
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (filteredSubjects.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (searchQuery.isNotEmpty()) stringResource(R.string.search_no_results, searchQuery)
                            else stringResource(R.string.home_empty_title),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredSubjects, key = { it.id }) { subject ->
                        val docCount = allDocs.count { it.subjectId == subject.id }
                        SubjectFolderCard(
                            subject = subject,
                            chaptersCount = 0, // dynamic count
                            documentsCount = docCount,
                            onClick = { onNavigateToSubject(subject.id) },
                            onPinToggle = { viewModel.togglePinSubject(subject.id) },
                            onEdit = { subjectToEdit = subject },
                            onDelete = { subjectToDelete = subject }
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredSubjects, key = { it.id }) { subject ->
                        val docCount = allDocs.count { it.subjectId == subject.id }
                        SubjectFolderCard(
                            subject = subject,
                            chaptersCount = 0,
                            documentsCount = docCount,
                            onClick = { onNavigateToSubject(subject.id) },
                            onPinToggle = { viewModel.togglePinSubject(subject.id) },
                            onEdit = { subjectToEdit = subject },
                            onDelete = { subjectToDelete = subject }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AddEditSubjectDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, desc, iconKey, colorArgb, isPinned ->
                viewModel.createSubject(name, desc, iconKey, colorArgb, isPinned)
                showCreateDialog = false
            }
        )
    }

    subjectToEdit?.let { subject ->
        AddEditSubjectDialog(
            initialSubject = subject,
            onDismiss = { subjectToEdit = null },
            onConfirm = { name, desc, iconKey, colorArgb, isPinned ->
                viewModel.updateSubject(
                    subject.copy(
                        name = name,
                        description = desc,
                        iconKey = iconKey,
                        colorArgb = colorArgb,
                        isPinned = isPinned,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                subjectToEdit = null
            }
        )
    }

    subjectToDelete?.let { subject ->
        AlertDialog(
            onDismissRequest = { subjectToDelete = null },
            title = { Text(stringResource(R.string.binder_delete_subject_title), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(stringResource(R.string.binder_delete_subject_msg))
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = cascadeDelete, onCheckedChange = { cascadeDelete = it })
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.binder_delete_cascade), style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSubject(subject.id, cascadeDelete)
                        subjectToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { subjectToDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
