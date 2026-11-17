package com.example.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.FinanceCalculator
import com.example.localization.LocalAppStrings
import com.example.model.PlatformConfig
import com.example.model.School
import com.example.model.UiState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.SimpleBarChart
import com.example.ui.components.StatusBadge
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.DeepNightBlue
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SubtleGold
import com.example.ui.theme.Turquoise
import com.example.viewmodel.PlatformViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperAdminScreen(
    platformViewModel: PlatformViewModel,
    onBack: () -> Unit,
    onEnterSchoolWithSupport: (School) -> Unit
) {
    val strings = LocalAppStrings.current
    val schoolsState by platformViewModel.schoolsState.collectAsStateWithLifecycle()
    val subsState by platformViewModel.subscriptionsState.collectAsStateWithLifecycle()
    val ticketsState by platformViewModel.ticketsState.collectAsStateWithLifecycle()
    val interventionsState by platformViewModel.interventionsState.collectAsStateWithLifecycle()
    val adminsState by platformViewModel.adminsState.collectAsStateWithLifecycle()
    val configState by platformViewModel.platformConfig.collectAsStateWithLifecycle()
    val message by platformViewModel.actionMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showCreateSchool by remember { mutableStateOf(false) }
    var interventionTarget by remember { mutableStateOf<School?>(null) }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            platformViewModel.clearMessage()
        }
    }

    val schools = (schoolsState as? UiState.Success)?.data ?: emptyList()
    val subscriptions = (subsState as? UiState.Success)?.data ?: emptyList()
    val tickets = (ticketsState as? UiState.Success)?.data ?: emptyList()
    val interventions = (interventionsState as? UiState.Success)?.data ?: emptyList()
    val admins = (adminsState as? UiState.Success)?.data ?: emptyList()
    val config = (configState as? UiState.Success)?.data ?: PlatformConfig()

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("ADRIÈL TECH • Super Admin", style = MaterialTheme.typography.titleLarge)
                        Text("SaaS 20 000 FCFA/mois • v${config.appVersion}", style = MaterialTheme.typography.bodySmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("super_admin_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepNightBlue,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { showCreateSchool = true },
                    containerColor = RoyalBlue,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("super_admin_add_school_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = strings.createSchool)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Écoles (${schools.size})") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Abonnements") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Support (${tickets.size})") })
                Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Config") })
            }

            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 920.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        val activeCount = schools.count { it.subscriptionStatus == "ACTIVE" }
                        val mrr = activeCount * config.standardMonthlyFeeFcfa
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                KpiMetricCard(
                                    title = "Établissements",
                                    value = "${schools.size}",
                                    subtitle = "$activeCount actifs",
                                    icon = Icons.Default.Business,
                                    modifier = Modifier.weight(1f)
                                )
                                KpiMetricCard(
                                    title = "MRR Estimé",
                                    value = FinanceCalculator.formatFcfa(mrr),
                                    subtitle = strings.subscriptionRate,
                                    icon = Icons.Default.CreditCard,
                                    accentColor = Turquoise,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        if (schools.isEmpty()) {
                            item { EmptyStateCard(title = strings.noSchoolsTitle, body = strings.noSchoolsBody) }
                        } else {
                            items(schools, key = { it.id }) { school ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(school.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                                Text("${school.code} • ${school.city} • ${school.email}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            StatusBadge(school.subscriptionStatus)
                                        }
                                        Spacer(Modifier.height(10.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedButton(
                                                onClick = { platformViewModel.updateSubscriptionStatus(school, "ACTIVE", 1L) },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSuccess)
                                                Spacer(Modifier.width(4.dp))
                                                Text("Activer 1 mois")
                                            }
                                            OutlinedButton(
                                                onClick = { platformViewModel.updateSubscriptionStatus(school, "SUSPENDED", 0L) },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.PauseCircle, contentDescription = null, tint = CrimsonError)
                                                Spacer(Modifier.width(4.dp))
                                                Text("Suspendre")
                                            }
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Button(
                                            onClick = { interventionTarget = school },
                                            enabled = school.supportAccessEnabled,
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = DeepNightBlue)
                                        ) {
                                            Icon(Icons.Default.SupportAgent, contentDescription = null)
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                if (school.supportAccessEnabled) "Accès Support Autorisé (Journalisé)"
                                                else "Accès Support Verrouillé par l'École"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        val totalCollected = subscriptions.filter { it.status == "ACTIVE" }.sumOf { it.totalAmountFcfa }
                        val pendingCount = subscriptions.count { it.status == "PENDING" }
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Revenus Abonnements Encaissés", style = MaterialTheme.typography.titleMedium)
                                    Text(FinanceCalculator.formatFcfa(totalCollected), style = MaterialTheme.typography.headlineSmall, color = Turquoise, fontWeight = FontWeight.Bold)
                                    Text("$pendingCount demande(s) en attente de validation", style = MaterialTheme.typography.bodySmall)
                                    Spacer(Modifier.height(12.dp))
                                    val chartData = subscriptions
                                        .filter { it.status == "ACTIVE" }
                                        .groupBy { it.paymentMethod }
                                        .map { (k, v) -> k to v.sumOf { it.totalAmountFcfa } }
                                    SimpleBarChart(items = chartData, barColor = SubtleGold)
                                }
                            }
                        }
                        if (subscriptions.isEmpty()) {
                            item { EmptyStateCard("Aucun abonnement", "Les paiements et renouvellements SaaS apparaîtront ici.") }
                        } else {
                            items(subscriptions, key = { it.id }) { sub ->
                                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Column(Modifier.weight(1f)) {
                                                Text(sub.schoolName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                                Text("${sub.monthsCount} mois • ${FinanceCalculator.formatFcfa(sub.totalAmountFcfa)} • ${sub.paymentMethod}", style = MaterialTheme.typography.bodySmall)
                                                if (sub.referenceCode.isNotBlank()) {
                                                    Text("Réf: ${sub.referenceCode}", style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                            StatusBadge(sub.status)
                                        }
                                        if (sub.status == "PENDING") {
                                            Spacer(Modifier.height(8.dp))
                                            Button(onClick = { platformViewModel.approveSubscription(sub) }) {
                                                Text("Valider & Activer")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        if (tickets.isEmpty()) {
                            item { EmptyStateCard("Aucun ticket support", "Les incidents déclarés par les écoles apparaîtront ici.") }
                        } else {
                            items(tickets, key = { it.id }) { ticket ->
                                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Column(Modifier.weight(1f)) {
                                                Text(ticket.subject, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                                Text("${ticket.schoolName} • ${ticket.priority}", style = MaterialTheme.typography.bodySmall)
                                            }
                                            StatusBadge(ticket.status)
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        Text(ticket.description, style = MaterialTheme.typography.bodySmall)
                                        Spacer(Modifier.height(8.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedButton(onClick = { platformViewModel.updateTicket(ticket.id, "IN_PROGRESS", "Pris en charge") }) {
                                                Text("En cours")
                                            }
                                            Button(onClick = { platformViewModel.updateTicket(ticket.id, "RESOLVED", "Résolu par ADRIÈL TECH") }) {
                                                Text("Résoudre")
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(Modifier.height(8.dp))
                            Text("Journal des interventions (${interventions.size})", style = MaterialTheme.typography.titleMedium)
                        }
                        items(interventions, key = { it.id }) { log ->
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("${log.schoolName} — ${log.adminEmail}", style = MaterialTheme.typography.labelLarge)
                                    Text("Motif : ${log.reason}", style = MaterialTheme.typography.bodySmall)
                                    Text("Action : ${log.actionSummary}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    3 -> {
                        item {
                            PlatformConfigEditor(
                                current = config,
                                onSave = { platformViewModel.savePlatformConfig(it) }
                            )
                        }
                        item {
                            AdminAccountsCard(
                                admins = admins,
                                onAddAdmin = { uid, email, name, role ->
                                    platformViewModel.saveAdmin(uid, email, name, role)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateSchool) {
        CreateSchoolDialog(
            onDismiss = { showCreateSchool = false },
            onCreate = { name, code, type, city, phone, email, director, motto ->
                platformViewModel.createSchool(name, code, type, city, phone, email, director, motto) {
                    showCreateSchool = false
                }
            }
        )
    }

    interventionTarget?.let { target ->
        var reason by remember { mutableStateOf("") }
        var action by remember { mutableStateOf("Assistance technique autorisée") }
        AlertDialog(
            onDismissRequest = { interventionTarget = null },
            title = { Text("Journaliser l'intervention") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Conformément à la politique d'isolation ADRIÈL TECH, toute intervention sur ${target.name} est journalisée.", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Motif de l'intervention *") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = action, onValueChange = { action = it }, label = { Text("Action prévue *") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        platformViewModel.logSupportIntervention(target, reason, action) {
                            interventionTarget = null
                            onEnterSchoolWithSupport(target)
                        }
                    },
                    enabled = reason.trim().length >= 2
                ) {
                    Text("Journaliser & Ouvrir")
                }
            },
            dismissButton = {
                TextButton(onClick = { interventionTarget = null }) { Text(strings.cancel) }
            }
        )
    }
}

@Composable
private fun PlatformConfigEditor(
    current: PlatformConfig,
    onSave: (PlatformConfig) -> Unit
) {
    var version by remember(current) { mutableStateOf(current.appVersion) }
    var minVer by remember(current) { mutableStateOf(current.minSupportedVersion) }
    var fee by remember(current) { mutableStateOf(current.standardMonthlyFeeFcfa.toString()) }
    var email by remember(current) { mutableStateOf(current.supportEmail) }
    var phone by remember(current) { mutableStateOf(current.supportPhone) }
    var notes by remember(current) { mutableStateOf(current.releaseNotes) }
    var maintenance by remember(current) { mutableStateOf(current.maintenanceMode) }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Configuration Générale & Versions Logiciel", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = version, onValueChange = { version = it }, label = { Text("Version actuelle") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = minVer, onValueChange = { minVer = it }, label = { Text("Version min") }, modifier = Modifier.weight(1f))
            }
            OutlinedTextField(value = fee, onValueChange = { fee = it }, label = { Text("Tarif mensuel standard (FCFA)") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email support") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Tél support") }, modifier = Modifier.weight(1f))
            }
            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes de version") }, modifier = Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Mode maintenance", modifier = Modifier.weight(1f))
                Switch(checked = maintenance, onCheckedChange = { maintenance = it })
            }
            Button(
                onClick = {
                    onSave(
                        current.copy(
                            appVersion = version,
                            minSupportedVersion = minVer,
                            standardMonthlyFeeFcfa = fee.toLongOrNull() ?: 20000L,
                            supportEmail = email,
                            supportPhone = phone,
                            releaseNotes = notes,
                            maintenanceMode = maintenance
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Enregistrer Configuration")
            }
        }
    }
}

@Composable
private fun AdminAccountsCard(
    admins: List<com.example.model.PlatformAdmin>,
    onAddAdmin: (String, String, String, String) -> Unit
) {
    var uid by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = RoyalBlue)
                Spacer(Modifier.width(8.dp))
                Text("Comptes Administrateurs ADRIÈL TECH", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            admins.forEach { admin ->
                Text("• ${admin.displayName} (${admin.email}) — ${admin.role}", style = MaterialTheme.typography.bodySmall)
            }
            OutlinedTextField(value = uid, onValueChange = { uid = it }, label = { Text("UID Firebase") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email Google") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nom complet") }, modifier = Modifier.fillMaxWidth())
            OutlinedButton(
                onClick = {
                    onAddAdmin(uid, email, name, "SUPPORT_ADMIN")
                    uid = ""; email = ""; name = ""
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ajouter Administrateur Support")
            }
        }
    }
}
