package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.FinanceCalculator
import com.example.domain.PdfGenerator
import com.example.localization.AppLanguage
import com.example.localization.LocalAppLanguage
import com.example.localization.LocalAppStrings
import com.example.model.School
import com.example.model.UiState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.Turquoise
import com.example.viewmodel.PlatformViewModel
import com.example.viewmodel.SchoolViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AutomationSyncScreen(
    school: School,
    schoolViewModel: SchoolViewModel,
    platformViewModel: PlatformViewModel,
    offlineEntryMode: Boolean,
    onToggleOfflineMode: (Boolean) -> Unit
) {
    val strings = LocalAppStrings.current
    val language = LocalAppLanguage.current
    val context = LocalContext.current

    val automationLogsState by schoolViewModel.automationLogsState.collectAsStateWithLifecycle()
    val auditLogsState by schoolViewModel.auditLogsState.collectAsStateWithLifecycle()
    val missingGrades by schoolViewModel.missingGradeAlerts.collectAsStateWithLifecycle()
    val absenceAlerts by schoolViewModel.absenceAlerts.collectAsStateWithLifecycle()
    val studentBalances by schoolViewModel.studentBalances.collectAsStateWithLifecycle()
    val pendingOps by schoolViewModel.pendingSyncOps.collectAsStateWithLifecycle()
    val syncConflicts by schoolViewModel.syncConflicts.collectAsStateWithLifecycle()

    val automationLogs = (automationLogsState as? UiState.Success)?.data ?: emptyList()
    val auditLogs = (auditLogsState as? UiState.Success)?.data ?: emptyList()
    val unpaidCount = studentBalances.count { it.remainingBalanceFcfa > 0L }

    var selectedTab by remember { mutableIntStateOf(0) }
    var showTicketDialog by remember { mutableStateOf(false) }
    var showRenewDialog by remember { mutableStateOf(false) }
    var exportStatus by remember { mutableStateOf<String?>(null) }

    val tabs = if (language == AppLanguage.FR) {
        listOf("Automatisations", "Synchro & Conflits", "Audit, Support & Abonnement")
    } else {
        listOf("Automations", "Sync & Conflicts", "Audit, Support & Plan")
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = { Text(title, maxLines = 1) },
                    modifier = Modifier.testTag("auto_sync_tab_$idx")
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 900.dp).fillMaxSize(),
                contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        item {
                            SectionHeader(
                                title = strings.navAutomationSync,
                                subtitle = if (language == AppLanguage.FR) {
                                    "Moteur de tâches automatisées et historique des exécutions"
                                } else {
                                    "Automated control tasks and execution history"
                                }
                            )
                        }
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                KpiMetricCard(
                                    title = strings.missingGradesLabel,
                                    value = "${missingGrades.size}",
                                    icon = Icons.Default.Warning,
                                    accentColor = if (missingGrades.isNotEmpty()) AmberWarning else EmeraldSuccess,
                                    modifier = Modifier.weight(1f)
                                )
                                KpiMetricCard(
                                    title = strings.absenceAlertsLabel,
                                    value = "${absenceAlerts.size}",
                                    icon = Icons.Default.Warning,
                                    accentColor = if (absenceAlerts.isNotEmpty()) CrimsonError else EmeraldSuccess,
                                    modifier = Modifier.weight(1f)
                                )
                                KpiMetricCard(
                                    title = strings.unpaidLabel,
                                    value = "$unpaidCount",
                                    icon = Icons.Default.CreditCard,
                                    accentColor = if (unpaidCount > 0) AmberWarning else EmeraldSuccess,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        item {
                            Button(
                                onClick = { schoolViewModel.runAllAutomations() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("run_all_automations_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                Spacer(Modifier.width(10.dp))
                                Text(strings.runAutomations, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        if (automationLogs.isEmpty()) {
                            item { EmptyStateCard() }
                        } else {
                            items(automationLogs, key = { it.id }) { log ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(log.jobType, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = RoyalBlue)
                                            StatusBadge(log.status)
                                        }
                                        Text(log.summary, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            text = "Impact: ${log.affectedCount} • Par: ${log.executedBy}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(
                                            Icons.Default.CloudOff,
                                            contentDescription = null,
                                            tint = if (offlineEntryMode) AmberWarning else Turquoise
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = if (language == AppLanguage.FR) "Saisie hors connexion (File locale Room)" else "Offline Entry Queue (Local Room)",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = if (language == AppLanguage.FR) {
                                                    "Stocke notes, présences et paiements localement avant synchronisation contrôlée"
                                                } else {
                                                    "Queues grades, attendance, and payments locally for controlled sync"
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Switch(
                                        checked = offlineEntryMode,
                                        onCheckedChange = onToggleOfflineMode,
                                        modifier = Modifier.testTag("offline_mode_switch")
                                    )
                                }
                            }
                        }

                        item {
                            Button(
                                onClick = { schoolViewModel.syncOfflineQueue() },
                                enabled = pendingOps.isNotEmpty(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("sync_offline_queue_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = Turquoise)
                            ) {
                                Icon(Icons.Default.Sync, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("${strings.syncNow} (${pendingOps.size})")
                            }
                        }

                        if (syncConflicts.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    title = "${strings.conflictsDetected} (${syncConflicts.size})",
                                    subtitle = if (language == AppLanguage.FR) {
                                        "Des modifications serveur plus récentes ont été détectées : choisissez la version à retenir"
                                    } else {
                                        "Newer server updates detected: resolve each conflict explicitly"
                                    }
                                )
                            }
                            items(syncConflicts, key = { it.conflictId }) { conflict ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = AmberWarning.copy(alpha = 0.12f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(conflict.summaryLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = "${conflict.collectionName}/${conflict.documentId}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            OutlinedButton(
                                                onClick = { schoolViewModel.resolveConflictKeepServer(conflict) },
                                                modifier = Modifier.weight(1f).testTag("conflict_keep_server_${conflict.conflictId}")
                                            ) {
                                                Text(strings.keepServer)
                                            }
                                            Button(
                                                onClick = { schoolViewModel.resolveConflictForceLocal(conflict) },
                                                modifier = Modifier.weight(1f).testTag("conflict_force_local_${conflict.conflictId}"),
                                                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                                            ) {
                                                Text(strings.forceLocal)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            SectionHeader(
                                title = "${strings.pendingSyncCount} (${pendingOps.size})",
                                subtitle = if (language == AppLanguage.FR) "Opérations en attente d'envoi" else "Queued operations"
                            )
                        }
                        if (pendingOps.isEmpty()) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = if (language == AppLanguage.FR) "Aucune opération locale en attente." else "No pending local operations.",
                                        modifier = Modifier.padding(16.dp),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            items(pendingOps, key = { it.localId }) { op ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(op.summaryLabel, fontWeight = FontWeight.SemiBold)
                                            Text("${op.collectionName} • ${op.opType}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        StatusBadge(op.syncStatus)
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = if (language == AppLanguage.FR) "Abonnement ADRIÈL SCHOOL" else "ADRIÈL SCHOOL Plan",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${FinanceCalculator.formatFcfa(school.monthlyFeeFcfa)} / mois",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        StatusBadge(school.subscriptionStatus)
                                    }
                                    if (school.subscriptionEndMillis > 0L) {
                                        val endDate = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(Date(school.subscriptionEndMillis))
                                        Text(
                                            text = if (language == AppLanguage.FR) "Échéance actuelle : $endDate" else "Valid until: $endDate",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Button(
                                            onClick = { showRenewDialog = true },
                                            modifier = Modifier.weight(1f).testTag("renew_subscription_btn"),
                                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                                        ) {
                                            Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text(if (language == AppLanguage.FR) "Renouveler" else "Renew")
                                        }
                                        OutlinedButton(
                                            onClick = { showTicketDialog = true },
                                            modifier = Modifier.weight(1f).testTag("open_support_ticket_btn")
                                        ) {
                                            Icon(Icons.Default.SupportAgent, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Support")
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            val json = schoolViewModel.buildExportJson()
                                            PdfGenerator.exportSchoolDataJson(context, school.code, json)
                                                .onSuccess { file -> exportStatus = "${strings.exportSuccess} : ${file.name}" }
                                                .onFailure { err -> exportStatus = err.localizedMessage }
                                        },
                                        modifier = Modifier.fillMaxWidth().testTag("export_school_json_btn")
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(strings.exportDataJson)
                                    }
                                    exportStatus?.let {
                                        Text(it, style = MaterialTheme.typography.labelSmall, color = Turquoise)
                                    }
                                }
                            }
                        }

                        item {
                            SectionHeader(
                                title = if (language == AppLanguage.FR) "Journal d'audit de l'établissement" else "School Audit Trail",
                                subtitle = if (language == AppLanguage.FR) "Traçabilité des opérations sensibles" else "Sensitive operations log"
                            )
                        }
                        if (auditLogs.isEmpty()) {
                            item { EmptyStateCard() }
                        } else {
                            items(auditLogs, key = { it.id }) { log ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Security, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("${log.action} • ${log.entityType}", fontWeight = FontWeight.SemiBold)
                                            Text(log.details, style = MaterialTheme.typography.bodySmall)
                                            Text(log.actorEmail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTicketDialog) {
        var subject by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        var priority by remember { mutableStateOf("MEDIUM") }

        AlertDialog(
            onDismissRequest = { showTicketDialog = false },
            title = { Text("Ticket Support ADRIÈL TECH") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Objet *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("ticket_subject_input")
                    )
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Description détaillée *") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth().testTag("ticket_desc_input")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("LOW", "MEDIUM", "HIGH", "CRITICAL").forEach { p ->
                            FilterChip(
                                selected = priority == p,
                                onClick = { priority = p },
                                label = { Text(p) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        platformViewModel.createSupportTicket(school, subject, desc, priority)
                        showTicketDialog = false
                    },
                    enabled = subject.trim().length >= 2 && desc.trim().length >= 2,
                    modifier = Modifier.testTag("confirm_ticket_btn")
                ) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTicketDialog = false }) { Text(strings.cancel) }
            }
        )
    }

    if (showRenewDialog) {
        var months by remember { mutableStateOf("1") }
        var method by remember { mutableStateOf("ORANGE_MONEY") }
        var refCode by remember { mutableStateOf("") }
        var note by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showRenewDialog = false },
            title = { Text("Renouvellement Abonnement (20 000 FCFA/mois)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = months,
                        onValueChange = { months = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Nombre de mois") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("renew_months_input")
                    )
                    val mCount = months.toLongOrNull()?.coerceAtLeast(1L) ?: 1L
                    Text(
                        text = "Total : ${FinanceCalculator.formatFcfa(FinanceCalculator.computeSubscriptionTotal(mCount, school.monthlyFeeFcfa))}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Turquoise
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("ORANGE_MONEY", "MTN_MOMO", "VIREMENT").forEach { m ->
                            FilterChip(
                                selected = method == m,
                                onClick = { method = m },
                                label = { Text(m) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = refCode,
                        onValueChange = { refCode = it },
                        label = { Text("Référence transaction *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("renew_ref_input")
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Note justificative") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val mCount = months.toLongOrNull()?.coerceAtLeast(1L) ?: 1L
                        platformViewModel.submitSubscriptionPayment(school, mCount, method, refCode, note)
                        showRenewDialog = false
                    },
                    enabled = refCode.trim().isNotBlank(),
                    modifier = Modifier.testTag("confirm_renew_btn")
                ) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenewDialog = false }) { Text(strings.cancel) }
            }
        )
    }
}
