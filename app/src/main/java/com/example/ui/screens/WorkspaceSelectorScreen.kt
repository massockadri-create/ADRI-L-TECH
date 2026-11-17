package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.localization.LocalAppLanguage
import com.example.localization.LocalAppStrings
import com.example.model.School
import com.example.model.UiState
import com.example.ui.components.Base64ImageBox
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.parseSafeColor
import com.example.ui.theme.DeepNightBlue
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SubtleGold
import com.example.ui.theme.Turquoise
import com.example.viewmodel.PlatformViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceSelectorScreen(
    platformViewModel: PlatformViewModel,
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit,
    onToggleLanguage: () -> Unit,
    onOpenSuperAdmin: () -> Unit,
    onOpenSchool: (School) -> Unit,
    onSignOut: () -> Unit
) {
    val strings = LocalAppStrings.current
    val language = LocalAppLanguage.current
    val schoolsState by platformViewModel.schoolsState.collectAsStateWithLifecycle()
    val message by platformViewModel.actionMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCreateDialog by remember { mutableStateOf(false) }
    var schoolToDelete by remember { mutableStateOf<School?>(null) }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            platformViewModel.clearMessage()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(strings.workspaceSelectorTitle, style = MaterialTheme.typography.titleLarge)
                        Text(
                            platformViewModel.currentUserEmail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                actions = {
                    TextButton(onClick = onToggleLanguage, modifier = Modifier.testTag("ws_lang_btn")) {
                        Icon(Icons.Default.Language, contentDescription = strings.languageLabel)
                        Spacer(Modifier.width(4.dp))
                        Text(language.code)
                    }
                    IconButton(onClick = onToggleTheme, modifier = Modifier.testTag("ws_theme_btn")) {
                        Icon(
                            if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = strings.darkMode
                        )
                    }
                    IconButton(onClick = onSignOut, modifier = Modifier.testTag("sign_out_button")) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = strings.signOut)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(strings.createSchool) },
                containerColor = RoyalBlue,
                contentColor = Color.White,
                modifier = Modifier.testTag("create_school_fab")
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 820.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (platformViewModel.isSuperAdmin) {
                    item {
                        Card(
                            onClick = onOpenSuperAdmin,
                            colors = CardDefaults.cardColors(containerColor = DeepNightBlue),
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("open_super_admin_card")
                        ) {
                            Row(
                                modifier = Modifier.padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(SubtleGold.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = SubtleGold)
                                }
                                Spacer(Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        strings.superAdminSpace,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        strings.superAdminSubtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFCBD5E1)
                                    )
                                }
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = strings.superAdminSpace, tint = SubtleGold)
                            }
                        }
                    }
                }

                when (val state = schoolsState) {
                    is UiState.Loading -> {
                        item {
                            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    is UiState.Error -> {
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                                Text(state.message, modifier = Modifier.padding(16.dp))
                            }
                        }
                    }
                    is UiState.Success -> {
                        if (state.data.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    title = strings.noSchoolsTitle,
                                    body = strings.noSchoolsBody
                                )
                            }
                        } else {
                            items(state.data, key = { it.id }) { school ->
                                val brandPrimary = parseSafeColor(school.primaryColorHex, RoyalBlue)
                                Card(
                                    onClick = { onOpenSchool(school) },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    shape = MaterialTheme.shapes.large,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("school_card_${school.code}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(18.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Base64ImageBox(
                                            base64 = school.logoPrimaryBase64,
                                            contentDescription = school.name,
                                            modifier = Modifier.size(56.dp),
                                            placeholderIcon = Icons.Default.School
                                        )
                                        Spacer(Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = school.name,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = brandPrimary,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                StatusBadge(school.subscriptionStatus)
                                            }
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = "${school.code} • ${school.schoolType} • ${school.city.ifBlank { school.country }}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (school.motto.isNotBlank()) {
                                                Text(
                                                    text = "« ${school.motto} »",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = Turquoise
                                                )
                                            }
                                        }
                                        if (school.ownerUid == platformViewModel.currentUserId || platformViewModel.isSuperAdmin) {
                                            IconButton(
                                                onClick = { schoolToDelete = school },
                                                modifier = Modifier.testTag("delete_school_${school.code}")
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Supprimer l'établissement",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = strings.openSchool)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    schoolToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { schoolToDelete = null },
            title = { Text("Supprimer l'établissement ?") },
            text = {
                Text("« ${target.name} » (${target.code}) sera supprimé définitivement. Cette action est irréversible.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        platformViewModel.deleteSchool(target)
                        schoolToDelete = null
                    }
                ) {
                    Text("Supprimer", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { schoolToDelete = null }) { Text("Annuler") }
            }
        )
    }

    if (showCreateDialog) {
        CreateSchoolDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, code, type, city, phone, email, director, motto ->
                platformViewModel.createSchool(name, code, type, city, phone, email, director, motto) {
                    showCreateDialog = false
                }
            }
        )
    }
}

@Composable
fun CreateSchoolDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, String, String, String, String, String, String) -> Unit
) {
    val strings = LocalAppStrings.current
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Lycée / Collège") }
    var city by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var director by remember { mutableStateOf("") }
    var motto by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.createSchool) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom de l'établissement *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("create_school_name_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("Code (auto si vide)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = type,
                        onValueChange = { type = it },
                        label = { Text("Type") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("Ville") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Téléphone") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email établissement") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = director,
                    onValueChange = { director = it },
                    label = { Text("Directeur / Proviseur") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = motto,
                    onValueChange = { motto = it },
                    label = { Text("Devise") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name, code, type, city, phone, email, director, motto) },
                enabled = name.trim().length >= 2,
                modifier = Modifier.testTag("confirm_create_school_btn")
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}
