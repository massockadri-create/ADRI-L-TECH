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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.FinanceCalculator
import com.example.localization.LocalAppStrings
import com.example.model.School
import com.example.model.UiState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.SimpleBarChart
import com.example.ui.components.StatusBadge
import com.example.ui.components.parseSafeColor
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.Turquoise
import com.example.viewmodel.SchoolViewModel

@Composable
fun SchoolDashboardScreen(
    school: School,
    schoolViewModel: SchoolViewModel,
    onNavigate: (SchoolSection) -> Unit
) {
    val strings = LocalAppStrings.current
    val studentsState by schoolViewModel.studentsState.collectAsStateWithLifecycle()
    val classesState by schoolViewModel.classesState.collectAsStateWithLifecycle()
    val cashSummary by schoolViewModel.cashRegisterSummary.collectAsStateWithLifecycle()
    val balances by schoolViewModel.studentBalances.collectAsStateWithLifecycle()
    val missingGrades by schoolViewModel.missingGradeAlerts.collectAsStateWithLifecycle()
    val absenceAlerts by schoolViewModel.absenceAlerts.collectAsStateWithLifecycle()
    val notificationsState by schoolViewModel.notificationsState.collectAsStateWithLifecycle()

    val students = (studentsState as? UiState.Success)?.data ?: emptyList()
    val classes = (classesState as? UiState.Success)?.data ?: emptyList()
    val notifications = (notificationsState as? UiState.Success)?.data ?: emptyList()
    val unpaidTotal = balances.sumOf { it.remainingBalanceFcfa }
    val brandPrimary = parseSafeColor(school.primaryColorHex, RoyalBlue)

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 980.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = brandPrimary),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(school.name, style = MaterialTheme.typography.headlineSmall, color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold)
                                Text(
                                    school.motto.ifBlank { "ADRIÈL SCHOOL MANAGER • ${school.code}" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f)
                                )
                            }
                            StatusBadge(school.subscriptionStatus)
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onNavigate(SchoolSection.STUDENTS_STAFF) },
                                modifier = Modifier.testTag("dash_quick_students")
                            ) {
                                Text(strings.navStudentsStaff)
                            }
                            OutlinedButton(
                                onClick = { onNavigate(SchoolSection.FINANCE) },
                                modifier = Modifier.testTag("dash_quick_finance")
                            ) {
                                Text(strings.navFinance, color = androidx.compose.ui.graphics.Color.White)
                            }
                        }
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    KpiMetricCard(
                        title = strings.studentsLabel,
                        value = "${students.size}",
                        subtitle = "${classes.size} ${strings.classesLabel}",
                        icon = Icons.Default.Groups,
                        accentColor = RoyalBlue,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(SchoolSection.STUDENTS_STAFF) }
                    )
                    KpiMetricCard(
                        title = strings.cashBalanceLabel,
                        value = FinanceCalculator.formatFcfa(cashSummary.netBalanceFcfa),
                        subtitle = "${strings.revenueLabel}: ${FinanceCalculator.formatFcfa(cashSummary.totalRevenueFcfa)}",
                        icon = Icons.Default.AccountBalance,
                        accentColor = Turquoise,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(SchoolSection.FINANCE) }
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    KpiMetricCard(
                        title = strings.unpaidLabel,
                        value = FinanceCalculator.formatFcfa(unpaidTotal),
                        subtitle = "${balances.count { it.remainingBalanceFcfa > 0L }} dossier(s)",
                        icon = Icons.AutoMirrored.Filled.TrendingDown,
                        accentColor = AmberWarning,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(SchoolSection.FINANCE) }
                    )
                    KpiMetricCard(
                        title = strings.expensesLabel,
                        value = FinanceCalculator.formatFcfa(cashSummary.totalExpensesFcfa),
                        subtitle = "Caisse nette",
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        accentColor = CrimsonError,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(SchoolSection.FINANCE) }
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    KpiMetricCard(
                        title = strings.missingGradesLabel,
                        value = "${missingGrades.size}",
                        icon = Icons.Default.Grade,
                        accentColor = AmberWarning,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(SchoolSection.ACADEMICS) }
                    )
                    KpiMetricCard(
                        title = strings.absenceAlertsLabel,
                        value = "${absenceAlerts.size}",
                        icon = Icons.Default.Warning,
                        accentColor = CrimsonError,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(SchoolSection.STUDENTS_STAFF) }
                    )
                }
            }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Flux Financiers (FCFA)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(10.dp))
                        val chartItems = listOf(
                            "Recettes" to cashSummary.totalRevenueFcfa,
                            "Dépenses" to cashSummary.totalExpensesFcfa,
                            "Impayés" to unpaidTotal,
                            "Net" to cashSummary.netBalanceFcfa.coerceAtLeast(0L)
                        )
                        SimpleBarChart(items = chartItems, barColor = Turquoise)
                    }
                }
            }

            if (students.isEmpty() && classes.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "Établissement vierge prêt",
                        body = "Commencez par créer une année scolaire, vos classes (tarifs FCFA), vos matières et vos premiers élèves."
                    )
                }
            }

            item {
                SectionHeader(
                    title = "Communications récentes",
                    trailing = {
                        OutlinedButton(onClick = { onNavigate(SchoolSection.DOCUMENTS) }) {
                            Icon(Icons.Default.Notifications, contentDescription = null)
                        }
                    }
                )
            }
            if (notifications.isEmpty()) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Text(
                            "Aucune annonce publiée.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(notifications.take(4), key = { it.id }) { notif ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(notif.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                StatusBadge(notif.priority)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(notif.message, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
