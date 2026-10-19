package com.example.ui.screens.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.ui.BinderViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantScreen(
    viewModel: BinderViewModel,
    onNavigateToDocument: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val prefs by viewModel.userPreferences.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()

    var showKeyDialog by remember { mutableStateOf(false) }
    var apiKeyInput by remember { mutableStateOf(prefs.geminiApiKey) }
    var isTestingKey by remember { mutableStateOf(false) }
    var testResultText by remember { mutableStateOf<String?>(null) }

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: Chat, 1: Fiche Generator

    // Sheet Generator State
    var selectedSubjectId by remember { mutableStateOf(subjects.firstOrNull()?.id) }
    var selectedChapterId by remember { mutableStateOf<String?>(null) }
    var selectedSheetType by remember { mutableStateOf("Fiche de synthèse de cours") }
    var topicDetails by remember { mutableStateOf("") }
    var generatedSheetTitle by remember { mutableStateOf<String?>(null) }
    var generatedSheetContent by remember { mutableStateOf<String?>(null) }
    var savedDocId by remember { mutableStateOf<String?>(null) }

    var subjectMenuOpen by remember { mutableStateOf(false) }
    var chapterMenuOpen by remember { mutableStateOf(false) }

    // Chapters for selected subject
    val chaptersFlow = remember(selectedSubjectId) {
        if (selectedSubjectId != null) viewModel.repository.observeChaptersForSubject(selectedSubjectId!!)
        else kotlinx.coroutines.flow.flowOf(emptyList())
    }
    val chapters by chaptersFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    val currentSubject = subjects.firstOrNull { it.id == selectedSubjectId }
    val currentChapter = chapters.firstOrNull { it.id == selectedChapterId }

    // Chat input
    var chatInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("ai_assistant_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.ai_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.ai_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        apiKeyInput = prefs.geminiApiKey
                        testResultText = null
                        showKeyDialog = true
                    }) {
                        Icon(
                            Icons.Default.Key,
                            contentDescription = "Clé API Gemini",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (selectedTabIndex == 0) {
                        IconButton(onClick = { viewModel.clearChat() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear Chat", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Tab Selector
            PrimaryTabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Discussion & Tutorat") },
                    icon = { Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Créateur de Fiches") },
                    icon = { Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            if (selectedTabIndex == 0) {
                // --- CHAT INTERFACE ---
                Column(modifier = Modifier.fillMaxSize()) {
                    // Chat messages list
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(chatMessages) { message ->
                            ChatBubble(message = message, onNavigateToDocument = onNavigateToDocument)
                        }

                        if (isAiLoading) {
                            item {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = stringResource(R.string.ai_generating),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Quick Suggestion Chips
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            SuggestionChipItem("Dipôle RC : équations et constante de temps") {
                                viewModel.sendChatMessage("Explique-moi le Dipôle RC, l'équation différentielle de charge et la méthode pour trouver la constante de temps tau.")
                            }
                        }
                        item {
                            SuggestionChipItem("Acides & bases : pH et demi-équivalence") {
                                viewModel.sendChatMessage("Fais-moi un récapitulatif sur les réactions acido-basiques et le calcul du pH au point de demi-équivalence.")
                            }
                        }
                        item {
                            SuggestionChipItem("Limites & Continuité en Maths") {
                                viewModel.sendChatMessage("Donne-moi les astuces indispensables pour lever les indéterminations dans le calcul des limites au Bac.")
                            }
                        }
                        item {
                            SuggestionChipItem("SVT : Méiose et brassage génétique") {
                                viewModel.sendChatMessage("Explique la différence entre brassage interchromosomique et intrachromosomique avec des exemples.")
                            }
                        }
                    }

                    // Input Field & Send Button
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = chatInput,
                                onValueChange = { chatInput = it },
                                placeholder = { Text(stringResource(R.string.ai_placeholder), fontSize = 13.sp) },
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_ai_chat"),
                                maxLines = 3
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (chatInput.isNotBlank()) {
                                        viewModel.sendChatMessage(chatInput.trim())
                                        chatInput = ""
                                    }
                                },
                                shape = CircleShape,
                                enabled = chatInput.isNotBlank() && !isAiLoading,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("btn_send_ai_chat")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                // --- SHEET GENERATOR INTERFACE ---
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Créer une Fiche de Révision Officielle",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Select Subject
                    ExposedDropdownMenuBox(
                        expanded = subjectMenuOpen,
                        onExpandedChange = { subjectMenuOpen = it }
                    ) {
                        OutlinedTextField(
                            value = currentSubject?.name ?: "Sélectionner une matière",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Matière cible") },
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

                    // Select Chapter (if available)
                    if (chapters.isNotEmpty()) {
                        ExposedDropdownMenuBox(
                            expanded = chapterMenuOpen,
                            onExpandedChange = { chapterMenuOpen = it }
                        ) {
                            OutlinedTextField(
                                value = currentChapter?.name ?: "Direct dans la matière",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Chapitre (optionnel)") },
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
                                    text = { Text("Direct dans la matière") },
                                    onClick = {
                                        selectedChapterId = null
                                        chapterMenuOpen = false
                                    }
                                )
                                chapters.forEach { c ->
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

                    // Sheet Type Chips
                    Text(
                        text = "Type de fiche",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            stringResource(R.string.ai_sheet_type_summary),
                            stringResource(R.string.ai_sheet_type_formulas),
                            stringResource(R.string.ai_sheet_type_method)
                        ).forEach { type ->
                            FilterChip(
                                selected = selectedSheetType == type,
                                onClick = { selectedSheetType = type },
                                label = { Text(type, fontSize = 11.sp) }
                            )
                        }
                    }

                    // Topic Input
                    OutlinedTextField(
                        value = topicDetails,
                        onValueChange = { topicDetails = it },
                        label = { Text("Thème / Notions à inclure") },
                        placeholder = { Text("Ex: Dipôle RC, Lois de Newton, Équilibres chimiques, Suites...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_sheet_topic")
                    )

                    // Generate Button
                    Button(
                        onClick = {
                            val subName = currentSubject?.name ?: "Sciences"
                            val chapName = currentChapter?.name
                            val topic = if (topicDetails.isNotBlank()) topicDetails else "Révision générale"
                            viewModel.generateSheet(subName, chapName, selectedSheetType, topic) { title, content ->
                                generatedSheetTitle = title
                                generatedSheetContent = content
                                savedDocId = null
                            }
                        },
                        enabled = !isAiLoading,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_generate_sheet")
                    ) {
                        if (isAiLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Génération en cours…")
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.ai_generate_sheet_btn))
                        }
                    }

                    // Preview and Export Card (When Generated)
                    if (generatedSheetContent != null && generatedSheetTitle != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
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
                                        text = generatedSheetTitle!!,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = generatedSheetContent!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Save Buttons (PDF & Image)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.saveGeneratedSheetAsPdf(
                                                title = generatedSheetTitle!!,
                                                subjectId = selectedSubjectId,
                                                chapterId = selectedChapterId,
                                                subjectName = currentSubject?.name ?: "Bac",
                                                chapterName = currentChapter?.name,
                                                content = generatedSheetContent!!
                                            ) { id ->
                                                savedDocId = id
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("btn_save_sheet_pdf")
                                    ) {
                                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(stringResource(R.string.ai_save_as_pdf), fontSize = 12.sp)
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.saveGeneratedSheetAsImage(
                                                title = generatedSheetTitle!!,
                                                subjectId = selectedSubjectId,
                                                chapterId = selectedChapterId,
                                                subjectName = currentSubject?.name ?: "Bac",
                                                chapterName = currentChapter?.name,
                                                content = generatedSheetContent!!
                                            ) { id ->
                                                savedDocId = id
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("btn_save_sheet_image")
                                    ) {
                                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(stringResource(R.string.ai_save_as_image), fontSize = 12.sp)
                                    }
                                }

                                if (savedDocId != null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    OutlinedButton(
                                        onClick = { onNavigateToDocument(savedDocId!!) },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Ouvrir la fiche dans le classeur")
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(50.dp))
                }
            }
        }

        if (showKeyDialog) {
            AlertDialog(
                onDismissRequest = { showKeyDialog = false },
                title = { Text("Clé API Google Gemini", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(
                            "Clé API actuellement active pour l'Assistant Bac IA :",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = { apiKeyInput = it },
                            label = { Text("Clé API") },
                            placeholder = { Text("AIzaSy...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    apiKeyInput = "AIzaSyAykWKD5d9qxr6veTHajsDY8JkIfO1YwPA"
                                }
                            ) {
                                Text("Clé par défaut", fontSize = 12.sp)
                            }
                            TextButton(
                                onClick = {
                                    isTestingKey = true
                                    testResultText = null
                                    scope.launch {
                                        val res = viewModel.testGeminiApiKey(apiKeyInput.trim())
                                        isTestingKey = false
                                        testResultText = if (res.isSuccess) {
                                            "✓ Connexion réussie !"
                                        } else {
                                            "✗ Erreur : ${res.exceptionOrNull()?.localizedMessage}"
                                        }
                                    }
                                },
                                enabled = !isTestingKey
                            ) {
                                Text(if (isTestingKey) "Test..." else "Tester", fontSize = 12.sp)
                            }
                        }
                        if (testResultText != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = testResultText!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (testResultText!!.startsWith("✓")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val trimmed = apiKeyInput.trim()
                            if (trimmed.isNotBlank()) {
                                viewModel.setGeminiApiKey(trimmed)
                            }
                            showKeyDialog = false
                        }
                    ) {
                        Text("Enregistrer")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showKeyDialog = false }) {
                        Text("Fermer")
                    }
                }
            )
        }
    }
}

@Composable
private fun ChatBubble(
    message: com.example.data.ai.ChatMessage,
    onNavigateToDocument: (String) -> Unit
) {
    val isUser = message.role == "user"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .size(32.dp)
                    .align(Alignment.Top)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (message.isSheet && message.sheetTitle != null) {
                    Text(
                        text = "📄 ${message.sheetTitle}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(
                    text = message.text,
                    color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun SuggestionChipItem(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = text, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
        }
    }
}
