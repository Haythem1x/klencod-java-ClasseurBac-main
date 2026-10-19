package com.example.ui.screens.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.backup.BackupSummary
import com.example.ui.BinderViewModel
import com.example.ui.components.formatFileSize
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: BinderViewModel,
    modifier: Modifier = Modifier
) {
    val prefs by viewModel.userPreferences.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var storageBytes by remember { mutableLongStateOf(0L) }
    var cacheBytes by remember { mutableLongStateOf(0L) }
    var isOperatingBackup by remember { mutableStateOf(false) }
    var backupStatusMessage by remember { mutableStateOf<String?>(null) }
    var restoreSummary by remember { mutableStateOf<BackupSummary?>(null) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var replaceOnRestore by remember { mutableStateOf(false) }

    var showApiKeyDialog by remember { mutableStateOf(false) }
    var apiKeyInput by remember { mutableStateOf(prefs.geminiApiKey) }
    var isTestingKey by remember { mutableStateOf(false) }
    var testResultText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        storageBytes = viewModel.storageManager.getUsedStorageBytes()
        cacheBytes = viewModel.storageManager.getCacheSize()
    }

    // SAF Export launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        uri?.let {
            isOperatingBackup = true
            viewModel.exportBackup(it) { success, msg ->
                isOperatingBackup = false
                backupStatusMessage = msg
            }
        }
    }

    // SAF Restore launcher
    val restorePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            pendingRestoreUri = it
            viewModel.inspectBackup(it) { summary ->
                restoreSummary = summary
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("settings_screen"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Section: Language
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.settings_language),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = stringResource(R.string.settings_lang_fr), style = MaterialTheme.typography.bodyLarge)
                        RadioButton(
                            selected = prefs.language == "fr",
                            onClick = { viewModel.setLanguage("fr") },
                            modifier = Modifier.testTag("radio_lang_fr")
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = stringResource(R.string.settings_lang_ar), style = MaterialTheme.typography.bodyLarge)
                        RadioButton(
                            selected = prefs.language == "ar",
                            onClick = { viewModel.setLanguage("ar") },
                            modifier = Modifier.testTag("radio_lang_ar")
                        )
                    }
                }
            }
        }

        // Section: Theme
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.settings_theme),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    ThemeOptionRow("system", stringResource(R.string.settings_theme_system), prefs.themeMode) { viewModel.setTheme("system") }
                    ThemeOptionRow("light", stringResource(R.string.settings_theme_light), prefs.themeMode) { viewModel.setTheme("light") }
                    ThemeOptionRow("dark", stringResource(R.string.settings_theme_dark), prefs.themeMode) { viewModel.setTheme("dark") }
                }
            }
        }

        // Section: Assistant Bac IA & Clé Gemini API
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth().testTag("settings_ai_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Assistant Bac IA (Gemini)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "L'Assistant IA répond à toutes vos questions sur le programme du Bac tunisien et génère des fiches de révision en PDF ou image.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Clé API active & connectée",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                val keyDisplay = if (prefs.geminiApiKey.length > 10) {
                                    "${prefs.geminiApiKey.take(7)}...${prefs.geminiApiKey.takeLast(4)}"
                                } else {
                                    "Prête"
                                }
                                Text(
                                    text = "Clé: $keyDisplay",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (testResultText != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = testResultText!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (testResultText!!.startsWith("✓")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                apiKeyInput = prefs.geminiApiKey
                                showApiKeyDialog = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Modifier la clé", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                isTestingKey = true
                                testResultText = null
                                scope.launch {
                                    val res = viewModel.testGeminiApiKey(prefs.geminiApiKey)
                                    isTestingKey = false
                                    testResultText = if (res.isSuccess) {
                                        "✓ Connexion réussie à l'API Gemini !"
                                    } else {
                                        "✗ Erreur : ${res.exceptionOrNull()?.localizedMessage}"
                                    }
                                }
                            },
                            enabled = !isTestingKey,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isTestingKey) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Tester", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Section: Storage & Cache
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.settings_storage),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Documents sauvegardés :", style = MaterialTheme.typography.bodyMedium)
                        Text(formatFileSize(storageBytes), fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Cache des miniatures :", style = MaterialTheme.typography.bodyMedium)
                        Text(formatFileSize(cacheBytes), fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            viewModel.clearCache()
                            cacheBytes = viewModel.storageManager.getCacheSize()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.action_clear_cache))
                    }
                }
            }
        }

        // Section: Backup & Restore (Screen Q)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Archive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.settings_section_backup),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Sauvegardez l'ensemble de votre classeur (cours, résumés, fiches, statuts de révision) dans un fichier ZIP exportable.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    if (isOperatingBackup) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Opération en cours…")
                        }
                    } else {
                        Button(
                            onClick = {
                                exportLauncher.launch("MonClasseurBac_Backup_${System.currentTimeMillis()}.zip")
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_export_backup")
                        ) {
                            Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.settings_backup_export))
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                restorePickerLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_restore_backup")
                        ) {
                            Icon(Icons.Default.Unarchive, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.settings_backup_restore))
                        }
                    }
                }
            }
        }

        // Section: About
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.settings_about_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.settings_about_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.settings_version),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = { viewModel.resetPreferences() }) {
                        Text(stringResource(R.string.settings_reset_prefs))
                    }
                }
            }
        }
    }

    // Backup Result Alert
    backupStatusMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { backupStatusMessage = null },
            title = { Text("Information") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { backupStatusMessage = null }) {
                    Text(stringResource(R.string.action_close))
                }
            }
        )
    }

    // Restore Confirmation Dialog
    restoreSummary?.let { summary ->
        AlertDialog(
            onDismissRequest = {
                restoreSummary = null
                pendingRestoreUri = null
            },
            title = { Text("Aperçu de la sauvegarde", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("• Version de sauvegarde : ${summary.version}")
                    Text("• Matières : ${summary.subjectsCount}")
                    Text("• Chapitres : ${summary.chaptersCount}")
                    Text("• Documents : ${summary.documentsCount}")
                    Text("• Taille totale : ${formatFileSize(summary.totalFileBytes)}")
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Voulez-vous fusionner avec vos matières actuelles ou remplacer complètement votre classeur ?",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Row {
                    TextButton(
                        onClick = {
                            pendingRestoreUri?.let { uri ->
                                isOperatingBackup = true
                                viewModel.restoreBackup(uri, replaceCurrent = false) { success, msg ->
                                    isOperatingBackup = false
                                    backupStatusMessage = msg
                                }
                            }
                            restoreSummary = null
                            pendingRestoreUri = null
                        }
                    ) {
                        Text("Fusionner")
                    }
                    Button(
                        onClick = {
                            pendingRestoreUri?.let { uri ->
                                isOperatingBackup = true
                                viewModel.restoreBackup(uri, replaceCurrent = true) { success, msg ->
                                    isOperatingBackup = false
                                    backupStatusMessage = msg
                                }
                            }
                            restoreSummary = null
                            pendingRestoreUri = null
                        }
                    ) {
                        Text("Remplacer")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    restoreSummary = null
                    pendingRestoreUri = null
                }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showApiKeyDialog) {
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = { Text("Clé API Google Gemini", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Configurez votre clé API Gemini pour l'Assistant Bac IA :",
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
                    TextButton(
                        onClick = {
                            apiKeyInput = "AIzaSyAykWKD5d9qxr6veTHajsDY8JkIfO1YwPA"
                        }
                    ) {
                        Text("Rétablir la clé par défaut", fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = apiKeyInput.trim()
                        if (trimmed.isNotBlank()) {
                            viewModel.setGeminiApiKey(trimmed)
                            testResultText = null
                        }
                        showApiKeyDialog = false
                    }
                ) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
private fun ThemeOptionRow(
    modeKey: String,
    label: String,
    currentMode: String,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        RadioButton(
            selected = currentMode == modeKey,
            onClick = onSelect
        )
    }
}
