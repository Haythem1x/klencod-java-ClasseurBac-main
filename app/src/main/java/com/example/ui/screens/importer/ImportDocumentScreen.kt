package com.example.ui.screens.importer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.BinderViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportDocumentScreen(
    preselectedSubjectId: String?,
    preselectedChapterId: String?,
    viewModel: BinderViewModel,
    onBack: () -> Unit,
    onImportComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val isImporting by viewModel.isImporting.collectAsStateWithLifecycle()

    var selectedSubjectId by remember { mutableStateOf(preselectedSubjectId ?: subjects.firstOrNull()?.id) }
    var selectedChapterId by remember { mutableStateOf(preselectedChapterId) }
    var customTitle by remember { mutableStateOf("") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileType by remember { mutableStateOf<String?>(null) }

    var subjectMenuExpanded by remember { mutableStateOf(false) }
    var chapterMenuExpanded by remember { mutableStateOf(false) }

    // Chapters for selected subject
    val chaptersFlow = remember(selectedSubjectId) {
        if (selectedSubjectId != null) viewModel.repository.observeChaptersForSubject(selectedSubjectId!!)
        else kotlinx.coroutines.flow.flowOf(emptyList())
    }
    val chapters by chaptersFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    // SAF PDF Picker
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            selectedUri = it
            selectedFileType = "pdf"
            if (customTitle.isBlank()) {
                customTitle = uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.') ?: "Cours_Bac"
            }
        }
    }

    // SAF Image Picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedUri = it
            selectedFileType = "image"
            if (customTitle.isBlank()) {
                customTitle = uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.') ?: "Fiche_Bac"
            }
        }
    }

    // Camera Capture Flow
    var cameraTempFile by remember { mutableStateOf<File?>(null) }
    var cameraTempUri by remember { mutableStateOf<Uri?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && cameraTempFile != null) {
            viewModel.importCapturedImage(
                file = cameraTempFile!!,
                customTitle = customTitle.takeIf { it.isNotBlank() } ?: "Photo_Fiche_${System.currentTimeMillis()}",
                subjectId = selectedSubjectId,
                chapterId = selectedChapterId
            )
            onImportComplete()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        if (granted) {
            val file = File(context.cacheDir, "scan_${System.currentTimeMillis()}.jpg")
            cameraTempFile = file
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            cameraTempUri = uri
            takePictureLauncher.launch(uri)
        }
    }

    fun launchCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            val file = File(context.cacheDir, "scan_${System.currentTimeMillis()}.jpg")
            cameraTempFile = file
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            cameraTempUri = uri
            takePictureLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.import_title), fontWeight = FontWeight.Bold) },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Source Options (PDF, Image, Camera)
            Text(
                text = "1. Choisir le type de document",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // PDF Option
            ImportSourceCard(
                icon = Icons.Default.PictureAsPdf,
                iconColor = Color(0xFFDC2626),
                title = stringResource(R.string.import_pdf_title),
                subtitle = stringResource(R.string.import_pdf_subtitle),
                isSelected = selectedFileType == "pdf",
                onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                testTag = "btn_pick_pdf"
            )

            // Image Option
            ImportSourceCard(
                icon = Icons.Default.Image,
                iconColor = Color(0xFF0D9488),
                title = stringResource(R.string.import_image_title),
                subtitle = stringResource(R.string.import_image_subtitle),
                isSelected = selectedFileType == "image",
                onClick = { imagePickerLauncher.launch("image/*") },
                testTag = "btn_pick_image"
            )

            // Camera Option
            ImportSourceCard(
                icon = Icons.Default.CameraAlt,
                iconColor = MaterialTheme.colorScheme.primary,
                title = stringResource(R.string.import_camera_title),
                subtitle = stringResource(R.string.import_camera_subtitle),
                isSelected = false,
                onClick = { launchCamera() },
                testTag = "btn_camera_scan"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Destination and Title Section
            Text(
                text = "2. Emplacement et titre",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = customTitle,
                onValueChange = { customTitle = it },
                label = { Text(stringResource(R.string.import_file_name)) },
                placeholder = { Text("Ex: Résumé Dipôle RC, Formules Acide-Base") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_doc_title")
            )

            // Select Subject
            ExposedDropdownMenuBox(
                expanded = subjectMenuExpanded,
                onExpandedChange = { subjectMenuExpanded = it }
            ) {
                val currentSub = subjects.firstOrNull { it.id == selectedSubjectId }
                OutlinedTextField(
                    value = currentSub?.name ?: "Sélectionner une matière",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.import_dest_subject)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectMenuExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = subjectMenuExpanded,
                    onDismissRequest = { subjectMenuExpanded = false }
                ) {
                    subjects.forEach { s ->
                        DropdownMenuItem(
                            text = { Text(s.name) },
                            onClick = {
                                selectedSubjectId = s.id
                                selectedChapterId = null
                                subjectMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // Select Chapter (optional)
            if (chapters.isNotEmpty()) {
                ExposedDropdownMenuBox(
                    expanded = chapterMenuExpanded,
                    onExpandedChange = { chapterMenuExpanded = it }
                ) {
                    val currentChap = chapters.firstOrNull { it.id == selectedChapterId }
                    OutlinedTextField(
                        value = currentChap?.name ?: "Aucun chapitre (Direct dans la matière)",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.import_dest_chapter)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = chapterMenuExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = chapterMenuExpanded,
                        onDismissRequest = { chapterMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Direct dans la matière") },
                            onClick = {
                                selectedChapterId = null
                                chapterMenuExpanded = false
                            }
                        )
                        chapters.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c.name) },
                                onClick = {
                                    selectedChapterId = c.id
                                    chapterMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            if (isImporting) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(stringResource(R.string.import_progress), style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    if (selectedUri != null) {
                        viewModel.importDocument(
                            uri = selectedUri!!,
                            customTitle = customTitle.takeIf { it.isNotBlank() },
                            subjectId = selectedSubjectId,
                            chapterId = selectedChapterId
                        )
                        onImportComplete()
                    }
                },
                enabled = selectedUri != null && !isImporting,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_confirm_import")
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.action_save))
            }
        }
    }
}

@Composable
private fun ImportSourceCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = iconColor.copy(alpha = 0.15f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(26.dp))
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
