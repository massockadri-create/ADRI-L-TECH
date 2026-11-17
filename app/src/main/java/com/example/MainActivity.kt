package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.local.AppDatabase
import com.example.data.local.AppPreferences
import com.example.data.remote.AuthManager
import com.example.data.remote.OfflineSyncManager
import com.example.data.remote.PlatformRepository
import com.example.data.remote.SchoolRepository
import com.example.data.remote.authStateFlow
import com.example.localization.AppLanguage
import com.example.localization.EnglishStrings
import com.example.localization.FrenchStrings
import com.example.localization.LocalAppLanguage
import com.example.localization.LocalAppStrings
import com.example.model.School
import com.example.ui.components.parseSafeColor
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.SchoolShellScreen
import com.example.ui.screens.SuperAdminScreen
import com.example.ui.screens.WorkspaceSelectorScreen
import com.example.ui.theme.AdrielSchoolTheme
import com.example.viewmodel.PlatformViewModel
import com.example.viewmodel.SchoolViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

private enum class AppWorkspace {
    SELECTOR,
    SUPER_ADMIN,
    SCHOOL
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AdrielSchoolManagerRoot()
        }
    }
}

@Composable
fun AdrielSchoolManagerRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = remember { AppPreferences(context.applicationContext) }
    val credentialManager = remember { CredentialManager.create(context) }

    val isDarkMode by preferences.darkModeFlow.collectAsStateWithLifecycle(initialValue = false)
    val language by preferences.languageFlow.collectAsStateWithLifecycle(initialValue = AppLanguage.FR)
    val offlineEntryMode by preferences.offlineEntryModeFlow.collectAsStateWithLifecycle(initialValue = false)

    val currentUser by remember { Firebase.auth.authStateFlow() }
        .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    var autoSignInChecked by remember { mutableStateOf(Firebase.auth.currentUser != null) }
    var selectedWorkspace by remember(currentUser?.uid) { mutableStateOf(AppWorkspace.SELECTOR) }
    var selectedSchool by remember(currentUser?.uid) { mutableStateOf<School?>(null) }

    LaunchedEffect(Unit) {
        if (!autoSignInChecked) {
            AuthManager.attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = { autoSignInChecked = true },
                onUnauthenticated = { autoSignInChecked = true },
                scope = scope
            )
        }
    }

    val toggleTheme: () -> Unit = {
        scope.launch { preferences.setDarkMode(!isDarkMode) }
    }
    val toggleLanguage: () -> Unit = {
        val next = if (language == AppLanguage.FR) AppLanguage.EN else AppLanguage.FR
        scope.launch { preferences.setLanguage(next) }
    }
    val toggleOfflineMode: (Boolean) -> Unit = { enabled ->
        scope.launch { preferences.setOfflineEntryMode(enabled) }
    }

    val activeSchool = if (selectedWorkspace == AppWorkspace.SCHOOL) selectedSchool else null
    val customPrimary = activeSchool?.let { parseSafeColor(it.primaryColorHex, com.example.ui.theme.RoyalBlue) }
    val customSecondary = activeSchool?.let { parseSafeColor(it.secondaryColorHex, com.example.ui.theme.Turquoise) }

    val appStrings = if (language == AppLanguage.EN) EnglishStrings else FrenchStrings

    CompositionLocalProvider(
        LocalAppLanguage provides language,
        LocalAppStrings provides appStrings
    ) {
        AdrielSchoolTheme(
            darkTheme = isDarkMode,
            customPrimaryColor = customPrimary,
            customSecondaryColor = customSecondary
        ) {
            when {
                !autoSignInChecked -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                currentUser == null -> {
                    AuthScreen(
                        isDarkMode = isDarkMode,
                        onToggleTheme = toggleTheme,
                        onToggleLanguage = toggleLanguage,
                        onAuthSuccess = { /* authStateFlow updates automatically */ }
                    )
                }

                else -> {
                    val user = currentUser!!
                    key(user.uid) {
                        val platformRepo = remember(user.uid) { PlatformRepository(context) }
                        val schoolRepo = remember(user.uid) { SchoolRepository(context) }
                        val db = remember { AppDatabase.getInstance(context) }
                        val syncManager = remember(user.uid) { OfflineSyncManager(context, db.offlineSyncDao()) }

                        val platformViewModel: PlatformViewModel = viewModel(
                            key = "platform_${user.uid}",
                            factory = viewModelFactory {
                                initializer {
                                    PlatformViewModel(
                                        repository = platformRepo,
                                        currentUserId = user.uid,
                                        currentUserEmail = user.email ?: "",
                                        currentUserName = user.displayName ?: ""
                                    )
                                }
                            }
                        )

                        when (selectedWorkspace) {
                            AppWorkspace.SELECTOR -> {
                                WorkspaceSelectorScreen(
                                    platformViewModel = platformViewModel,
                                    isDarkMode = isDarkMode,
                                    onToggleTheme = toggleTheme,
                                    onToggleLanguage = toggleLanguage,
                                    onOpenSuperAdmin = { selectedWorkspace = AppWorkspace.SUPER_ADMIN },
                                    onOpenSchool = { school ->
                                        selectedSchool = school
                                        selectedWorkspace = AppWorkspace.SCHOOL
                                    },
                                    onSignOut = {
                                        selectedSchool = null
                                        selectedWorkspace = AppWorkspace.SELECTOR
                                        AuthManager.signOut(credentialManager, scope)
                                    }
                                )
                            }

                            AppWorkspace.SUPER_ADMIN -> {
                                BackHandler {
                                    selectedWorkspace = AppWorkspace.SELECTOR
                                }
                                SuperAdminScreen(
                                    platformViewModel = platformViewModel,
                                    onBack = { selectedWorkspace = AppWorkspace.SELECTOR },
                                    onEnterSchoolWithSupport = { school ->
                                        selectedSchool = school
                                        selectedWorkspace = AppWorkspace.SCHOOL
                                    }
                                )
                            }

                            AppWorkspace.SCHOOL -> {
                                val school = selectedSchool
                                if (school == null) {
                                    selectedWorkspace = AppWorkspace.SELECTOR
                                } else {
                                    BackHandler {
                                        selectedSchool = null
                                        selectedWorkspace = AppWorkspace.SELECTOR
                                    }
                                    val schoolViewModel: SchoolViewModel = viewModel(
                                        key = "school_${user.uid}_${school.id}",
                                        factory = viewModelFactory {
                                            initializer {
                                                SchoolViewModel(
                                                    schoolId = school.id,
                                                    isSuperAdmin = platformViewModel.isSuperAdmin,
                                                    currentUserId = user.uid,
                                                    currentUserEmail = user.email ?: "",
                                                    platformRepo = platformRepo,
                                                    schoolRepo = schoolRepo,
                                                    syncManager = syncManager
                                                )
                                            }
                                        }
                                    )
                                    SchoolShellScreen(
                                        schoolViewModel = schoolViewModel,
                                        platformViewModel = platformViewModel,
                                        isDarkMode = isDarkMode,
                                        offlineEntryMode = offlineEntryMode,
                                        onToggleTheme = toggleTheme,
                                        onToggleLanguage = toggleLanguage,
                                        onToggleOfflineMode = toggleOfflineMode,
                                        onExitToWorkspaces = {
                                            selectedSchool = null
                                            selectedWorkspace = AppWorkspace.SELECTOR
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
