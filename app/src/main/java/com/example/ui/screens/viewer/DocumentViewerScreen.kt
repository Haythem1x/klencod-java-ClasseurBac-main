package com.example.ui.screens.viewer

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.ui.BinderViewModel
import com.example.ui.components.RenameDialog
import com.example.ui.components.RevisionStatusBadge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentViewerScreen(
    documentId: String,
    viewModel: BinderViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val docFlow = remember(documentId) { viewModel.repository.observeDocument(documentId) }
    val document by docFlow.collectAsStateWithLifecycle(initialValue = null)

    var isFullscreen by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }

    if (document == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val file = remember(document) { File(context.filesDir, document!!.relativeFilePath) }
    val isPdf = document!!.mimeType.equals("application/pdf", ignoreCase = true)

    // Share action
    fun shareDocument() {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = document!!.mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Partager le cours"))
        } catch (_: Exception) {}
    }

    Scaffold(
        topBar = {
            AnimatedVisibility(visible = !isFullscreen, enter = fadeIn(), exit = fadeOut()) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = document!!.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isPdf) "PDF" else "Image",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                RevisionStatusBadge(
                                    status = document!!.revisionStatus,
                                    onClick = {
                                        val next = when (document!!.revisionStatus) {
                                            "TODO" -> "IN_PROGRESS"
                                            "IN_PROGRESS" -> "MASTERED"
                                            else -> "TODO"
                                        }
                                        viewModel.updateRevisionStatus(document!!.id, next)
                                    }
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.toggleFavorite(document!!.id) }) {
                            Icon(
                                imageVector = if (document!!.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (document!!.isFavorite) Color(0xFFE11D48) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { isFullscreen = true }) {
                            Icon(Icons.Default.Fullscreen, contentDescription = "Fullscreen")
                        }
                        IconButton(onClick = { shareDocument() }) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_rename)) },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                    onClick = {
                                        menuOpen = false
                                        showRenameDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        menuOpen = false
                                        viewModel.deleteDocument(document!!.id)
                                        onBack()
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF0F172A))
                .testTag("document_viewer_container")
        ) {
            if (!file.exists()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.viewer_error_render),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else if (isPdf) {
                PdfViewerContent(
                    pdfFile = file,
                    initialPage = document!!.lastViewedPage ?: 0,
                    onPageChanged = { page ->
                        viewModel.updateLastViewed(document!!.id, page)
                    }
                )
            } else {
                ImageViewerContent(imageFile = file)
            }

            // Fullscreen exit button
            if (isFullscreen) {
                Surface(
                    onClick = { isFullscreen = false },
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = CircleShape,
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.TopEnd)
                        .size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.FullscreenExit,
                            contentDescription = "Exit Fullscreen",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }

    if (showRenameDialog) {
        RenameDialog(
            initialValue = document!!.title,
            title = stringResource(R.string.action_rename),
            onDismiss = { showRenameDialog = false },
            onConfirm = {
                viewModel.renameDocument(document!!.id, it)
                showRenameDialog = false
            }
        )
    }
}

@Composable
private fun PdfViewerContent(
    pdfFile: File,
    initialPage: Int,
    onPageChanged: (Int) -> Unit
) {
    var pfd by remember { mutableStateOf<ParcelFileDescriptor?>(null) }
    var renderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var pageCount by remember { mutableIntStateOf(0) }
    var currentPage by remember { mutableIntStateOf(initialPage) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Page bitmaps cache
    val pageBitmaps = remember { mutableStateMapOf<Int, Bitmap>() }
    val scope = rememberCoroutineScope()

    DisposableEffect(pdfFile) {
        try {
            val fd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            pfd = fd
            val r = PdfRenderer(fd)
            renderer = r
            pageCount = r.pageCount
        } catch (e: Exception) {
            errorMessage = e.localizedMessage
        }

        onDispose {
            pageBitmaps.values.forEach { it.recycle() }
            pageBitmaps.clear()
            renderer?.close()
            pfd?.close()
        }
    }

    // Function to load a specific page bitmap
    fun loadPage(index: Int) {
        if (renderer == null || index !in 0 until pageCount || pageBitmaps.containsKey(index)) return
        scope.launch(Dispatchers.IO) {
            try {
                renderer?.let { r ->
                    synchronized(r) {
                        r.openPage(index).use { page ->
                            val scale = 1080f / page.width.toFloat()
                            val width = 1080
                            val height = (page.height * scale).toInt().coerceAtLeast(1)
                            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            bitmap.eraseColor(android.graphics.Color.WHITE)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            pageBitmaps[index] = bitmap
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(currentPage, renderer) {
        loadPage(currentPage)
        if (currentPage + 1 < pageCount) loadPage(currentPage + 1)
        if (currentPage - 1 >= 0) loadPage(currentPage - 1)
        onPageChanged(currentPage)
    }

    if (errorMessage != null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "Erreur: $errorMessage", color = Color.White)
        }
        return
    }

    if (pageCount == 0) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    // Zoom & pan state
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 4f)
        offset = if (scale > 1f) offset + offsetChange else Offset.Zero
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .transformable(state = transformState)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        scale = if (scale > 1.2f) 1f else 2.2f
                        offset = Offset.Zero
                    }
                )
            }
    ) {
        val currentBitmap = pageBitmaps[currentPage]
        if (currentBitmap != null) {
            Image(
                bitmap = currentBitmap.asImageBitmap(),
                contentDescription = "PDF Page ${currentPage + 1}",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        }

        // Floating Page Navigation Controls
        Surface(
            color = Color.Black.copy(alpha = 0.7f),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                IconButton(
                    onClick = {
                        if (currentPage > 0) currentPage--
                    },
                    enabled = currentPage > 0
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.NavigateBefore,
                        contentDescription = "Previous Page",
                        tint = if (currentPage > 0) Color.White else Color.Gray
                    )
                }

                Text(
                    text = "${currentPage + 1} / $pageCount",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                IconButton(
                    onClick = {
                        if (currentPage < pageCount - 1) currentPage++
                    },
                    enabled = currentPage < pageCount - 1
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                        contentDescription = "Next Page",
                        tint = if (currentPage < pageCount - 1) Color.White else Color.Gray
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(onClick = { scale = (scale + 0.5f).coerceAtMost(4f) }) {
                    Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", tint = Color.White)
                }

                IconButton(onClick = {
                    scale = (scale - 0.5f).coerceAtLeast(1f)
                    if (scale == 1f) offset = Offset.Zero
                }) {
                    Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun ImageViewerContent(imageFile: File) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var rotation by remember { mutableFloatStateOf(0f) }

    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        offset = if (scale > 1f) offset + offsetChange else Offset.Zero
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .transformable(state = transformState)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        scale = if (scale > 1.2f) 1f else 2.5f
                        offset = Offset.Zero
                    }
                )
            }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageFile)
                .crossfade(true)
                .build(),
            contentDescription = "Document image",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    rotationZ = rotation,
                    translationX = offset.x,
                    translationY = offset.y
                )
        )

        // Floating controls for Rotate and Zoom
        Surface(
            color = Color.Black.copy(alpha = 0.7f),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                IconButton(onClick = { rotation = (rotation + 90f) % 360f }) {
                    Icon(Icons.Default.RotateRight, contentDescription = "Rotate", tint = Color.White)
                }
                IconButton(onClick = { scale = (scale + 0.5f).coerceAtMost(5f) }) {
                    Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", tint = Color.White)
                }
                IconButton(onClick = {
                    scale = (scale - 0.5f).coerceAtLeast(1f)
                    if (scale == 1f) offset = Offset.Zero
                }) {
                    Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", tint = Color.White)
                }
            }
        }
    }
}
