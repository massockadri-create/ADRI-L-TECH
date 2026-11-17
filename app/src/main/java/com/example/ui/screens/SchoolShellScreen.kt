package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.PdfGenerator
import com.example.localization.LocalAppLanguage
import com.example.localization.LocalAppStrings
import com.example.model.School
import com.example.model.UiState
import com.example.ui.components.Base64ImageBox
import com.example.ui.components.StatusBadge
import com.example.ui.components.parseSafeColor
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.Turquoise
import com.example.viewmodel.PlatformViewModel
import com.example.viewmodel.SchoolViewModel
import kotlinx.coroutines.launch

enum class SchoolSection(val icon: ImageVector) {
    DASHBOARD(Icons.Default.Dashboard),
    ACADEMICS(Icons.Default.School),
    STUDENTS_STAFF(Icons.Default.Groups),
    FINANCE(Icons.Default.AccountBalanceWallet),
    DOCUMENTS(Icons.Default.Description),
    PARENT_PORTAL(Icons.Default.FamilyRestroom),
    BRANDING(Icons.Default.Palette),
    AUTOMATION_SYNC(Icons.Default.AutoAwesome)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchoolShellScreen(
    schoolViewModel: SchoolViewModel,
    platformViewModel: PlatformViewModel,
    isDarkMode: Boolean,
    offlineEntryMode: Boolean,
    onToggleTheme: () -> Unit,
    onToggleLanguage: () -> Unit,
    onToggleOfflineMode: (Boolean) -> Unit,
    onExitToWorkspaces: () -> Unit
) {
    val strings = LocalAppStrings.current
    val language = LocalAppLanguage.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }

    val schoolState by schoolViewModel.schoolState.collectAsStateWithLifecycle()
    val feedback by schoolViewModel.feedback.collectAsStateWithLifecycle()
    val platformMessage by platformViewModel.actionMessage.collectAsStateWithLifecycle()
    val pendingOps by schoolViewModel.pendingSyncOps.collectAsStateWithLifecycle()
    val conflicts by schoolViewModel.syncConflicts.collectAsStateWithLifecycle()

    var currentSection by remember { mutableStateOf(SchoolSection.DASHBOARD) }

    if (currentSection != SchoolSection.DASHBOARD) {
        BackHandler { currentSection = SchoolSection.DASHBOARD }
    }

    LaunchedEffect(feedback) {
        feedback?.let {
            snackbarHostState.showSnackbar(it)
            schoolViewModel.clearFeedback()
        }
    }
    LaunchedEffect(platformMessage) {
        platformMessage?.let {
            snackbarHostState.showSnackbar(it)
            platformViewModel.clearMessage()
        }
    }

    val school = (schoolState as? UiState.Success)?.data ?: School(name = "ADRIÈL SCHOOL")
    val brandPrimary = parseSafeColor(school.primaryColorHex, RoyalBlue)
    val isExpiredOrSuspended = school.subscriptionStatus == "EXPIRED" || school.subscriptionStatus == "SUSPENDED"

    fun sectionLabel(section: SchoolSection): String = when (section) {
        SchoolSection.DASHBOARD -> strings.navDashboard
        SchoolSection.ACADEMICS -> strings.navAcademics
        SchoolSection.STUDENTS_STAFF -> strings.navStudentsStaff
        SchoolSection.FINANCE -> strings.navFinance
        SchoolSection.DOCUMENTS -> strings.navDocuments
        SchoolSection.PARENT_PORTAL -> strings.navParentPortal
        SchoolSection.BRANDING -> strings.navBranding
        SchoolSection.AUTOMATION_SYNC -> strings.navAutomationSync
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 840.dp

        val mainScaffold: @Composable () -> Unit = {
            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Base64ImageBox(
                                    base64 = school.logoPrimaryBase64,
                                    contentDescription = school.name,
                                    modifier = Modifier.size(34.dp),
                                    placeholderIcon = Icons.Default.Badge
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = school.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${sectionLabel(currentSection)} • ${school.activeAcademicYearName.ifBlank { school.code }}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            if (!isWideScreen) {
                                IconButton(
                                    onClick = { scope.launch { drawerState.open() } },
                                    modifier = Modifier.testTag("school_drawer_btn")
                                ) {
                                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                                }
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { onToggleOfflineMode(!offlineEntryMode) },
                                modifier = Modifier.testTag("offline_toggle_btn")
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (pendingOps.isNotEmpty() || conflicts.isNotEmpty()) {
                                            Badge(containerColor = if (conflicts.isNotEmpty()) CrimsonError else AmberWarning) {
                                                Text("${pendingOps.size + conflicts.size}")
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (offlineEntryMode) Icons.Default.CloudOff else Icons.Default.CloudQueue,
                                        contentDescription = if (offlineEntryMode) strings.offlineMode else strings.onlineMode,
                                        tint = if (offlineEntryMode) AmberWarning else Turquoise
                                    )
                                }
                            }
                            if (pendingOps.isNotEmpty()) {
                                IconButton(
                                    onClick = { schoolViewModel.syncOfflineQueue() },
                                    modifier = Modifier.testTag("quick_sync_btn")
                                ) {
                                    Icon(Icons.Default.Sync, contentDescription = strings.syncNow)
                                }
                            }
                            TextButton(onClick = onToggleLanguage, modifier = Modifier.testTag("school_lang_btn")) {
                                Icon(Icons.Default.Language, contentDescription = strings.languageLabel, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(language.code)
                            }
                            IconButton(onClick = onToggleTheme, modifier = Modifier.testTag("school_theme_btn")) {
                                Icon(
                                    if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = strings.darkMode
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                    )
                },
                bottomBar = {
                    if (!isWideScreen) {
                        val primaryTabs = listOf(
                            SchoolSection.DASHBOARD,
                            SchoolSection.ACADEMICS,
                            SchoolSection.STUDENTS_STAFF,
                            SchoolSection.FINANCE,
                            SchoolSection.DOCUMENTS
                        )
                        NavigationBar(windowInsets = WindowInsets.navigationBars) {
                            primaryTabs.forEach { tab ->
                                NavigationBarItem(
                                    selected = currentSection == tab,
                                    onClick = { currentSection = tab },
                                    icon = { Icon(tab.icon, contentDescription = sectionLabel(tab)) },
                                    label = { Text(sectionLabel(tab), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    modifier = Modifier.testTag("bottom_nav_${tab.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isWideScreen) {
                        NavigationRail(
                            modifier = Modifier
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            containerColor = MaterialTheme.colorScheme.surface,
                            header = {
                                IconButton(onClick = onExitToWorkspaces) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.navSwitchWorkspace)
                                }
                            }
                        ) {
                            SchoolSection.entries.forEach { section ->
                                NavigationRailItem(
                                    selected = currentSection == section,
                                    onClick = { currentSection = section },
                                    icon = { Icon(section.icon, contentDescription = sectionLabel(section)) },
                                    label = { Text(sectionLabel(section), maxLines = 1) },
                                    modifier = Modifier.testTag("rail_nav_${section.name.lowercase()}")
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        if (isExpiredOrSuspended) {
                            Surface(
                                color = CrimsonError.copy(alpha = 0.12f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (school.subscriptionStatus == "SUSPENDED") strings.subscriptionSuspendedBanner else strings.subscriptionExpiredBanner,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CrimsonError,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(
                                        onClick = {
                                            val json = schoolViewModel.buildExportJson()
                                            PdfGenerator.exportSchoolDataJson(context, school.code, json)
                                                .onSuccess { /* snackbar handled by user */ }
                                        }
                                    ) {
                                        Text(strings.exportDataJson)
                                    }
                                }
                            }
                        }

                        if (conflicts.isNotEmpty()) {
                            Surface(
                                color = AmberWarning.copy(alpha = 0.16f),
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { currentSection = SchoolSection.AUTOMATION_SYNC }
                            ) {
                                Text(
                                    text = "${conflicts.size} ${strings.conflictsDetected} — Touchez pour résoudre",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = AmberWarning,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }

                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            when (currentSection) {
                                SchoolSection.DASHBOARD -> SchoolDashboardScreen(
                                    school = school,
                                    schoolViewModel = schoolViewModel,
                                    onNavigate = { currentSection = it }
                                )
                                SchoolSection.ACADEMICS -> AcademicsScreen(
                                    school = school,
                                    schoolViewModel = schoolViewModel,
                                    offlineEntryMode = offlineEntryMode
                                )
                                SchoolSection.STUDENTS_STAFF -> StudentsStaffScreen(
                                    school = school,
                                    schoolViewModel = schoolViewModel,
                                    offlineEntryMode = offlineEntryMode
                                )
                                SchoolSection.FINANCE -> FinanceScreen(
                                    school = school,
                                    schoolViewModel = schoolViewModel,
                                    offlineEntryMode = offlineEntryMode
                                )
                                SchoolSection.DOCUMENTS -> DocumentsPortalScreen(
                                    school = school,
                                    schoolViewModel = schoolViewModel,
                                    initialTab = 0
                                )
                                SchoolSection.PARENT_PORTAL -> DocumentsPortalScreen(
                                    school = school,
                                    schoolViewModel = schoolViewModel,
                                    initialTab = 1
                                )
                                SchoolSection.BRANDING -> SchoolBrandingScreen(
                                    school = school,
                                    schoolViewModel = schoolViewModel
                                )
                                SchoolSection.AUTOMATION_SYNC -> AutomationSyncScreen(
                                    school = school,
                                    schoolViewModel = schoolViewModel,
                                    platformViewModel = platformViewModel,
                                    offlineEntryMode = offlineEntryMode,
                                    onToggleOfflineMode = onToggleOfflineMode
                                )
                            }
                        }
                    }
                }
            }
        }

        if (isWideScreen) {
            mainScaffold()
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(brandPrimary)
                                .padding(18.dp)
                        ) {
                            Text(school.name, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                            Text("${school.code} • ${school.city}", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                            Spacer(Modifier.size(6.dp))
                            StatusBadge(school.subscriptionStatus)
                        }
                        HorizontalDivider()
                        Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
                            SchoolSection.entries.forEach { section ->
                                NavigationDrawerItem(
                                    label = { Text(sectionLabel(section)) },
                                    icon = { Icon(section.icon, contentDescription = null) },
                                    selected = currentSection == section,
                                    onClick = {
                                        currentSection = section
                                        scope.launch { drawerState.close() }
                                    },
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                                )
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            NavigationDrawerItem(
                                label = { Text(strings.navSwitchWorkspace) },
                                icon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) },
                                selected = false,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    onExitToWorkspaces()
                                },
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }
                },
                content = mainScaffold
            )
        }
    }
}
