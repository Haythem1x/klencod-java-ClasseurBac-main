package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.entity.DocumentEntity
import com.example.data.entity.SubjectEntity
import java.io.File

@Composable
fun RevisionStatusBadge(
    status: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val (color, textRes) = when (status) {
        "MASTERED" -> Color(0xFF10B981) to R.string.status_mastered
        "IN_PROGRESS" -> Color(0xFFF59E0B) to R.string.status_in_progress
        else -> Color(0xFFEF4444) to R.string.status_todo
    }

    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(textRes),
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun DocumentCard(
    document: DocumentEntity,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onChangeStatus: (String) -> Unit,
    modifier: Modifier = Modifier,
    isGridView: Boolean = true
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var statusMenuExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val isPdf = document.mimeType.equals("application/pdf", ignoreCase = true)
    val thumbFile = File(context.cacheDir, "thumbnails/${document.fileName}.thumb.jpg")
    val physicalFile = File(context.filesDir, document.relativeFilePath)
    val imageModel = if (thumbFile.exists()) thumbFile else if (physicalFile.exists() && !isPdf) physicalFile else null

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("document_card_${document.id}")
    ) {
        if (isGridView) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(115.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageModel != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(imageModel)
                                .crossfade(true)
                                .build(),
                            contentDescription = document.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = if (isPdf) Icons.Default.PictureAsPdf else Icons.Default.Folder,
                            contentDescription = null,
                            tint = if (isPdf) Color(0xFFDC2626) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(42.dp)
                        )
                    }

                    // Top Bar inside thumbnail (Favorite & Menu)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                            .align(Alignment.TopCenter),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            IconButton(onClick = onToggleFavorite, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    imageVector = if (document.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = if (document.isFavorite) Color(0xFFE11D48) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Box {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                modifier = Modifier.size(32.dp)
                            ) {
                                IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(32.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Menu",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            DocumentContextMenu(
                                expanded = menuExpanded,
                                onDismiss = { menuExpanded = false },
                                onRename = onRename,
                                onMove = onMove,
                                onShare = onShare,
                                onDelete = onDelete
                            )
                        }
                    }

                    // Format badge (bottom left of thumbnail)
                    Surface(
                        color = Color.Black.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = if (isPdf) "PDF" else "IMG",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = document.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatFileSize(document.fileSizeBytes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                        Box {
                            RevisionStatusBadge(
                                status = document.revisionStatus,
                                onClick = { statusMenuExpanded = true }
                            )
                            StatusDropdown(
                                expanded = statusMenuExpanded,
                                onDismiss = { statusMenuExpanded = false },
                                onSelect = {
                                    onChangeStatus(it)
                                    statusMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        } else {
            // List layout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageModel != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(imageModel).crossfade(true).build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = if (isPdf) Icons.Default.PictureAsPdf else Icons.Default.Folder,
                            contentDescription = null,
                            tint = if (isPdf) Color(0xFFDC2626) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = document.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isPdf) "PDF • ${document.pageCount ?: 1} p." else "Image",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = " • ${formatFileSize(document.fileSizeBytes)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Box {
                        RevisionStatusBadge(
                            status = document.revisionStatus,
                            onClick = { statusMenuExpanded = true }
                        )
                        StatusDropdown(
                            expanded = statusMenuExpanded,
                            onDismiss = { statusMenuExpanded = false },
                            onSelect = {
                                onChangeStatus(it)
                                statusMenuExpanded = false
                            }
                        )
                    }
                }

                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (document.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (document.isFavorite) Color(0xFFE11D48) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DocumentContextMenu(
                        expanded = menuExpanded,
                        onDismiss = { menuExpanded = false },
                        onRename = onRename,
                        onMove = onMove,
                        onShare = onShare,
                        onDelete = onDelete
                    )
                }
            }
        }
    }
}

@Composable
fun SubjectFolderCard(
    subject: SubjectEntity,
    chaptersCount: Int,
    documentsCount: Int,
    onClick: () -> Unit,
    onPinToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val subjectColor = IconAndColorHelper.getColor(subject.colorArgb)
    val subjectIcon = IconAndColorHelper.getIcon(subject.iconKey)

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("subject_card_${subject.id}")
    ) {
        Column {
            // Header colored accent tab (Binder style)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(subjectColor)
            )

            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = subjectColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = subjectIcon,
                                contentDescription = null,
                                tint = subjectColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Row {
                        IconButton(onClick = onPinToggle, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = if (subject.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                                contentDescription = "Pin",
                                tint = if (subject.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Box {
                            IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Options",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_edit)) },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        onEdit()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        menuExpanded = false
                                        onDelete()
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = subject.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!subject.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subject.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.binder_chapters_count, chaptersCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                    Text(
                        text = stringResource(R.string.binder_documents_count, documentsCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = subjectColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun DocumentContextMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.action_share)) },
            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
            onClick = { onDismiss(); onShare() }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.action_rename)) },
            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
            onClick = { onDismiss(); onRename() }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.action_move)) },
            leadingIcon = { Icon(Icons.Default.DriveFileMove, contentDescription = null) },
            onClick = { onDismiss(); onMove() }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) },
            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            onClick = { onDismiss(); onDelete() }
        )
    }
}

@Composable
private fun StatusDropdown(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.status_todo)) },
            onClick = { onSelect("TODO") }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.status_in_progress)) },
            onClick = { onSelect("IN_PROGRESS") }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.status_mastered)) },
            onClick = { onSelect("MASTERED") }
        )
    }
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) {
        "%.1f Mo".format(mb)
    } else {
        "%.0f Ko".format(kb)
    }
}
