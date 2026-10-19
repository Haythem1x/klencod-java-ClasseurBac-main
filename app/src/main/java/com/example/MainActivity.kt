package com.example

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.BinderViewModel
import com.example.ui.UiEvent
import com.example.ui.screens.binder.BinderScreen
import com.example.ui.screens.chapter.ChapterDetailsScreen
import com.example.ui.screens.favorites.FavoritesScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.importer.ImportDocumentScreen
import com.example.ui.screens.onboarding.OnboardingScreen
import com.example.ui.screens.progress.RevisionProgressScreen
import com.example.ui.screens.search.SearchScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.subject.SubjectDetailsScreen
import com.example.ui.screens.viewer.DocumentViewerScreen
import com.example.ui.theme.MonClasseurBacTheme
import java.util.Locale

sealed class AppScreen {
    object Onboarding : AppScreen()
    object Home : AppScreen()
    object Binder : AppScreen()
    object Ai : AppScreen()
    object Favorites : AppScreen()
    object Settings : AppScreen()
    object Search : AppScreen()
    object Progress : AppScreen()
    data class SubjectDetails(val subjectId: String) : AppScreen()
    data class ChapterDetails(val chapterId: String) : AppScreen()
    data class ImportDocument(val subjectId: String? = null, val chapterId: String? = null) : AppScreen()
    data class DocumentViewer(val documentId: String) : AppScreen()
}

class MainActivity : ComponentActivity() {

    private val viewModel: BinderViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val userPrefs by viewModel.userPreferences.collectAsStateWithLifecycle()
            val activity = this@MainActivity

            val localizedConfig = remember(userPrefs.language) {
                val locale = Locale(userPrefs.language)
                Locale.setDefault(locale)
                val config = Configuration(activity.resources.configuration).apply {
                    setLocale(locale)
                    setLayoutDirection(locale)
                }
                @Suppress("DEPRECATION")
                activity.resources.updateConfiguration(config, activity.resources.displayMetrics)
                config
            }

            val layoutDirection = if (userPrefs.language == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr

            val isDark = when (userPrefs.themeMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            CompositionLocalProvider(
                LocalConfiguration provides localizedConfig,
                LocalLayoutDirection provides layoutDirection,
                androidx.activity.compose.LocalActivityResultRegistryOwner provides activity
            ) {
                MonClasseurBacTheme(darkTheme = isDark) {
                    AppNavigationHost(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun AppNavigationHost(viewModel: BinderViewModel) {
    val prefs by viewModel.userPreferences.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Navigation backstack
    val backstack = remember { mutableStateListOf<AppScreen>() }

    // Initialize initial screen based on onboarding completion
    LaunchedEffect(prefs.onboardingCompleted) {
        if (backstack.isEmpty()) {
            if (!prefs.onboardingCompleted) {
                backstack.add(AppScreen.Onboarding)
            } else {
                backstack.add(AppScreen.Home)
            }
        }
    }

    // Handle viewmodel events
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                is UiEvent.Error -> snackbarHostState.showSnackbar(event.error)
            }
        }
    }

    val currentScreen = backstack.lastOrNull() ?: AppScreen.Home

    fun navigateTo(screen: AppScreen) {
        backstack.add(screen)
    }

    fun navigateBack() {
        if (backstack.size > 1) {
            backstack.removeAt(backstack.lastIndex)
        }
    }

    fun navigateToTab(screen: AppScreen) {
        // Clear backstack down to root tab
        backstack.clear()
        backstack.add(screen)
    }

    // Intercept back button for sub-screens
    if (backstack.size > 1) {
        BackHandler {
            navigateBack()
        }
    }

    val showBottomBar = currentScreen is AppScreen.Home ||
            currentScreen is AppScreen.Binder ||
            currentScreen is AppScreen.Ai ||
            currentScreen is AppScreen.Favorites ||
            currentScreen is AppScreen.Settings

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentScreen is AppScreen.Home,
                        onClick = { navigateToTab(AppScreen.Home) },
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_home)) },
                        modifier = Modifier.testTag("nav_tab_home")
                    )
                    NavigationBarItem(
                        selected = currentScreen is AppScreen.Binder,
                        onClick = { navigateToTab(AppScreen.Binder) },
                        icon = { Icon(Icons.Default.Folder, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_binder)) },
                        modifier = Modifier.testTag("nav_tab_binder")
                    )
                    NavigationBarItem(
                        selected = currentScreen is AppScreen.Ai,
                        onClick = { navigateToTab(AppScreen.Ai) },
                        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_ai)) },
                        modifier = Modifier.testTag("nav_tab_ai")
                    )
                    NavigationBarItem(
                        selected = currentScreen is AppScreen.Favorites,
                        onClick = { navigateToTab(AppScreen.Favorites) },
                        icon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_favorites)) },
                        modifier = Modifier.testTag("nav_tab_favorites")
                    )
                    NavigationBarItem(
                        selected = currentScreen is AppScreen.Settings,
                        onClick = { navigateToTab(AppScreen.Settings) },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_settings)) },
                        modifier = Modifier.testTag("nav_tab_settings")
                    )
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        when (val screen = currentScreen) {
            is AppScreen.Onboarding -> {
                OnboardingScreen(
                    viewModel = viewModel,
                    currentLanguage = prefs.language,
                    onFinish = {
                        navigateToTab(AppScreen.Home)
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            is AppScreen.Home -> {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToSubject = { subjectId -> navigateTo(AppScreen.SubjectDetails(subjectId)) },
                    onNavigateToDocument = { docId -> navigateTo(AppScreen.DocumentViewer(docId)) },
                    onNavigateToSearch = { navigateTo(AppScreen.Search) },
                    onNavigateToImport = { subId, chapId -> navigateTo(AppScreen.ImportDocument(subId, chapId)) },
                    onNavigateToProgress = { navigateTo(AppScreen.Progress) },
                    onNavigateToAi = { navigateTo(AppScreen.Ai) },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            is AppScreen.Binder -> {
                BinderScreen(
                    viewModel = viewModel,
                    onNavigateToSubject = { subjectId -> navigateTo(AppScreen.SubjectDetails(subjectId)) },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            is AppScreen.Ai -> {
                com.example.ui.screens.ai.AiAssistantScreen(
                    viewModel = viewModel,
                    onNavigateToDocument = { docId -> navigateTo(AppScreen.DocumentViewer(docId)) },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            is AppScreen.Favorites -> {
                FavoritesScreen(
                    viewModel = viewModel,
                    onNavigateToDocument = { docId -> navigateTo(AppScreen.DocumentViewer(docId)) },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            is AppScreen.Settings -> {
                SettingsScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }

            is AppScreen.Search -> {
                SearchScreen(
                    viewModel = viewModel,
                    onBack = { navigateBack() },
                    onNavigateToDocument = { docId -> navigateTo(AppScreen.DocumentViewer(docId)) },
                    onNavigateToSubject = { subjectId -> navigateTo(AppScreen.SubjectDetails(subjectId)) },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            is AppScreen.Progress -> {
                RevisionProgressScreen(
                    viewModel = viewModel,
                    onBack = { navigateBack() },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            is AppScreen.SubjectDetails -> {
                SubjectDetailsScreen(
                    subjectId = screen.subjectId,
                    viewModel = viewModel,
                    onBack = { navigateBack() },
                    onNavigateToChapter = { chapterId -> navigateTo(AppScreen.ChapterDetails(chapterId)) },
                    onNavigateToDocument = { docId -> navigateTo(AppScreen.DocumentViewer(docId)) },
                    onNavigateToImport = { subId, chapId -> navigateTo(AppScreen.ImportDocument(subId, chapId)) },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            is AppScreen.ChapterDetails -> {
                ChapterDetailsScreen(
                    chapterId = screen.chapterId,
                    viewModel = viewModel,
                    onBack = { navigateBack() },
                    onNavigateToDocument = { docId -> navigateTo(AppScreen.DocumentViewer(docId)) },
                    onNavigateToImport = { subId, chapId -> navigateTo(AppScreen.ImportDocument(subId, chapId)) },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            is AppScreen.ImportDocument -> {
                ImportDocumentScreen(
                    preselectedSubjectId = screen.subjectId,
                    preselectedChapterId = screen.chapterId,
                    viewModel = viewModel,
                    onBack = { navigateBack() },
                    onImportComplete = { navigateBack() },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            is AppScreen.DocumentViewer -> {
                DocumentViewerScreen(
                    documentId = screen.documentId,
                    viewModel = viewModel,
                    onBack = { navigateBack() },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}
