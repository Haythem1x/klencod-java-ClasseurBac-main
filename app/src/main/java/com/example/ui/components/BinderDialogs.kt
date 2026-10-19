package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.entity.ChapterEntity
import com.example.data.entity.SubjectEntity

@Composable
fun AddEditSubjectDialog(
    initialSubject: SubjectEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String?, iconKey: String, colorArgb: Int, isPinned: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(initialSubject?.name ?: "") }
    var description by remember { mutableStateOf(initialSubject?.description ?: "") }
    var selectedIcon by remember { mutableStateOf(initialSubject?.iconKey ?: "book") }
    var selectedColor by remember { mutableStateOf(initialSubject?.colorArgb ?: 0xFF2563EB.toInt()) }
    var isPinned by remember { mutableStateOf(initialSubject?.isPinned ?: false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialSubject == null) stringResource(R.string.binder_add_subject) else stringResource(R.string.action_edit),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = { Text(stringResource(R.string.form_subject_name)) },
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_subject_name"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.form_subject_desc)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = stringResource(R.string.form_subject_color),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(IconAndColorHelper.AVAILABLE_COLORS) { colorInt ->
                        val color = Color(colorInt)
                        val isSelected = colorInt == selectedColor
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = colorInt }
                                .then(
                                    if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = stringResource(R.string.form_subject_icon),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(IconAndColorHelper.AVAILABLE_ICONS) { (key, iconVec) ->
                        val isSelected = key == selectedIcon
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { selectedIcon = key },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = iconVec,
                                contentDescription = key,
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = stringResource(R.string.form_subject_pin), style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = isPinned, onCheckedChange = { isPinned = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "Veuillez saisir un nom"
                    } else {
                        onConfirm(name, description.takeIf { it.isNotBlank() }, selectedIcon, selectedColor, isPinned)
                    }
                },
                modifier = Modifier.testTag("btn_save_subject")
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditChapterDialog(
    subjects: List<SubjectEntity>,
    preselectedSubjectId: String,
    initialChapter: ChapterEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (subjectId: String, name: String, description: String?, iconKey: String, colorArgb: Int) -> Unit
) {
    var subjectId by remember { mutableStateOf(initialChapter?.subjectId ?: preselectedSubjectId) }
    var name by remember { mutableStateOf(initialChapter?.name ?: "") }
    var description by remember { mutableStateOf(initialChapter?.description ?: "") }
    var selectedColor by remember { mutableStateOf(initialChapter?.colorArgb ?: 0xFF2563EB.toInt()) }
    var selectedIcon by remember { mutableStateOf(initialChapter?.iconKey ?: "bookmark") }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val currentSubject = subjects.firstOrNull { it.id == subjectId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialChapter == null) stringResource(R.string.chapter_add) else stringResource(R.string.action_edit),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Subject picker if multiple subjects
                if (subjects.size > 1) {
                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded,
                        onExpandedChange = { dropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = currentSubject?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.form_chapter_parent_subject)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            subjects.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text(s.name) },
                                    onClick = {
                                        subjectId = s.id
                                        selectedColor = s.colorArgb
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = { Text(stringResource(R.string.form_chapter_name)) },
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_chapter_name"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.form_chapter_desc)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "Veuillez saisir un nom"
                    } else {
                        onConfirm(subjectId, name, description.takeIf { it.isNotBlank() }, selectedIcon, selectedColor)
                    }
                },
                modifier = Modifier.testTag("btn_save_chapter")
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
fun RenameDialog(
    initialValue: String,
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_rename")
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (text.isNotBlank()) onConfirm(text)
                }
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveDocumentDialog(
    subjects: List<SubjectEntity>,
    chapters: List<ChapterEntity>,
    currentSubjectId: String?,
    currentChapterId: String?,
    onDismiss: () -> Unit,
    onConfirm: (targetSubjectId: String?, targetChapterId: String?) -> Unit
) {
    var selectedSubjectId by remember { mutableStateOf(currentSubjectId) }
    var selectedChapterId by remember { mutableStateOf(currentChapterId) }
    var subjectMenuOpen by remember { mutableStateOf(false) }
    var chapterMenuOpen by remember { mutableStateOf(false) }

    val filteredChapters = chapters.filter { it.subjectId == selectedSubjectId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.action_move), fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Select subject
                ExposedDropdownMenuBox(
                    expanded = subjectMenuOpen,
                    onExpandedChange = { subjectMenuOpen = it }
                ) {
                    val currentSub = subjects.firstOrNull { it.id == selectedSubjectId }
                    OutlinedTextField(
                        value = currentSub?.name ?: "Aucune matière (Général)",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.import_dest_subject)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectMenuOpen) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = subjectMenuOpen,
                        onDismissRequest = { subjectMenuOpen = false }
                    ) {
                        subjects.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s.name) },
                                onClick = {
                                    selectedSubjectId = s.id
                                    selectedChapterId = null
                                    subjectMenuOpen = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Select chapter
                if (filteredChapters.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = chapterMenuOpen,
                        onExpandedChange = { chapterMenuOpen = it }
                    ) {
                        val currentChap = filteredChapters.firstOrNull { it.id == selectedChapterId }
                        OutlinedTextField(
                            value = currentChap?.name ?: "Aucun chapitre (Direct)",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.import_dest_chapter)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = chapterMenuOpen) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = chapterMenuOpen,
                            onDismissRequest = { chapterMenuOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Direct dans la matière (sans chapitre)") },
                                onClick = {
                                    selectedChapterId = null
                                    chapterMenuOpen = false
                                }
                            )
                            filteredChapters.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text(c.name) },
                                    onClick = {
                                        selectedChapterId = c.id
                                        chapterMenuOpen = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedSubjectId, selectedChapterId) }) {
                Text(stringResource(R.string.action_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
