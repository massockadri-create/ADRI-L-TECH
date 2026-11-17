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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.FinanceCalculator
import com.example.localization.LocalAppStrings
import com.example.model.School
import com.example.model.Student
import com.example.model.UiState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SimpleBarChart
import com.example.ui.components.StatusBadge
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.Turquoise
import com.example.ui.theme.tabularNumbers
import com.example.viewmodel.SchoolViewModel

@Composable
fun FinanceScreen(
    school: School,
    schoolViewModel: SchoolViewModel,
    offlineEntryMode: Boolean
) {
    val strings = LocalAppStrings.current
    val students = (schoolViewModel.studentsState.collectAsStateWithLifecycle().value as? UiState.Success)?.data ?: emptyList()
    val payments = (schoolViewModel.paymentsState.collectAsStateWithLifecycle().value as? UiState.Success)?.data ?: emptyList()
    val expenses = (schoolViewModel.expensesState.collectAsStateWithLifecycle().value as? UiState.Success)?.data ?: emptyList()
    val balances by schoolViewModel.studentBalances.collectAsStateWithLifecycle()
    val cashSummary by schoolViewModel.cashRegisterSummary.collectAsStateWithLifecycle()

    var subTab by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            TabRow(selectedTabIndex = subTab) {
                Tab(selected = subTab == 0, onClick = { subTab = 0 }, text = { Text("Paiements (${payments.size})") })
                Tab(selected = subTab == 1, onClick = { subTab = 1 }, text = { Text("Impayés") })
                Tab(selected = subTab == 2, onClick = { subTab = 2 }, text = { Text("Dépenses (${expenses.size})") })
                Tab(selected = subTab == 3, onClick = { subTab = 3 }, text = { Text("Caisse") })
            }

            LazyColumn(
                modifier = Modifier.widthIn(max = 920.dp).fillMaxSize(),
                contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (subTab) {
                    0 -> {
                        if (payments.isEmpty()) {
                            item { EmptyStateCard("Aucun paiement enregistré", "Encaissez en Espèces, Orange Money, MTN MoMo ou Virement.") }
                        } else {
                            items(payments, key = { it.id }) { pay ->
                                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                    Column(Modifier.padding(14.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Column(Modifier.weight(1f)) {
                                                Text("${pay.receiptNumber} • ${pay.studentName}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                                Text("${pay.feeCategory} • ${pay.method} ${pay.referenceNumber}", style = MaterialTheme.typography.bodySmall)
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(FinanceCalculator.formatFcfa(pay.amountFcfa), style = MaterialTheme.typography.titleSmall.tabularNumbers(), fontWeight = FontWeight.Bold, color = Turquoise)
                                                StatusBadge(pay.status)
                                            }
                                        }
                                        if (pay.status == "COMPLETED") {
                                            Spacer(Modifier.height(6.dp))
                                            OutlinedButton(onClick = { schoolViewModel.refundPayment(pay, "Remboursement autorisé") }) {
                                                Text("Rembourser", color = CrimsonError, style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        val unpaid = balances.filter { it.remainingBalanceFcfa > 0L }
                        if (unpaid.isEmpty()) {
                            item { EmptyStateCard("Aucun impayé détecté", "Tous les dossiers inscrits sont soldés ou aucune classe/élève enregistré.") }
                        } else {
                            items(unpaid, key = { it.student.id }) { bal ->
                                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text("${bal.student.fullName} (${bal.student.className})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                            Text("Attendu: ${FinanceCalculator.formatFcfa(bal.expectedFeeFcfa)} • Payé: ${FinanceCalculator.formatFcfa(bal.paidAmountFcfa)}", style = MaterialTheme.typography.bodySmall.tabularNumbers())
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(FinanceCalculator.formatFcfa(bal.remainingBalanceFcfa), style = MaterialTheme.typography.titleSmall.tabularNumbers(), fontWeight = FontWeight.Bold, color = CrimsonError)
                                            StatusBadge(bal.status.name)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        if (expenses.isEmpty()) {
                            item { EmptyStateCard("Aucune dépense", "Enregistrez vos sorties de caisse (salaires, charges, fournitures).") }
                        } else {
                            items(expenses, key = { it.id }) { exp ->
                                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(exp.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                            Text("${exp.category} • ${exp.method} • ${exp.beneficiary}", style = MaterialTheme.typography.bodySmall)
                                        }
                                        Text(FinanceCalculator.formatFcfa(exp.amountFcfa), style = MaterialTheme.typography.titleSmall.tabularNumbers(), fontWeight = FontWeight.Bold, color = CrimsonError)
                                    }
                                }
                            }
                        }
                    }

                    3 -> {
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Situation de Caisse & Méthodes de Paiement", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("Total Recettes: ${FinanceCalculator.formatFcfa(cashSummary.totalRevenueFcfa)}", style = MaterialTheme.typography.bodyMedium.tabularNumbers(), color = Turquoise)
                                    Text("Total Dépenses: ${FinanceCalculator.formatFcfa(cashSummary.totalExpensesFcfa)}", style = MaterialTheme.typography.bodyMedium.tabularNumbers(), color = CrimsonError)
                                    Text("Remboursements: ${FinanceCalculator.formatFcfa(cashSummary.totalRefundedFcfa)}", style = MaterialTheme.typography.bodySmall.tabularNumbers())
                                    Text("SOLDE NET: ${FinanceCalculator.formatFcfa(cashSummary.netBalanceFcfa)}", style = MaterialTheme.typography.headlineSmall.tabularNumbers(), fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(10.dp))
                                    val methodBars = cashSummary.revenueByMethod.entries.map { it.key to it.value }
                                    SimpleBarChart(items = methodBars, barColor = RoyalBlue)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (subTab == 0 || subTab == 2) {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = RoyalBlue,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag("finance_add_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = strings.add)
            }
        }
    }

    if (showAddDialog) {
        if (subTab == 0) {
            RecordPaymentDialog(students, offlineEntryMode, schoolViewModel) { showAddDialog = false }
        } else {
            RecordExpenseDialog(schoolViewModel) { showAddDialog = false }
        }
    }
}

@Composable
private fun RecordPaymentDialog(
    students: List<Student>,
    offlineMode: Boolean,
    schoolViewModel: SchoolViewModel,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    var selectedStudent by remember { mutableStateOf(students.firstOrNull()) }
    var amount by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("ORANGE_MONEY") }
    var category by remember { mutableStateOf("TUITION") }
    var reference by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Encaisser un paiement") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (students.isEmpty()) {
                    Text("Inscrivez d'abord un élève.", color = MaterialTheme.colorScheme.error)
                } else {
                    Text("Élève : ${selectedStudent?.fullName ?: "-"}", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        students.take(4).forEach { st ->
                            OutlinedButton(onClick = { selectedStudent = st }) { Text(st.firstName.take(8)) }
                        }
                    }
                    OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Montant (FCFA) *") }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("CASH", "ORANGE_MONEY", "MTN_MOMO", "BANK_TRANSFER").forEach { m ->
                            OutlinedButton(onClick = { method = m }) {
                                Text(if (method == m) "✓ ${m.take(4)}" else m.take(4), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("REGISTRATION", "TUITION", "INSTALLMENT").forEach { c ->
                            OutlinedButton(onClick = { category = c }) {
                                Text(if (category == c) "✓ ${c.take(5)}" else c.take(5), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    OutlinedTextField(value = reference, onValueChange = { reference = it }, label = { Text("Réf transaction (MoMo/OM/Banque)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Observation") }, modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amount.toLongOrNull() ?: 0L
                    selectedStudent?.let { st ->
                        schoolViewModel.recordPayment(st, category, amt, method, reference, note, offlineMode)
                        onDismiss()
                    }
                },
                enabled = selectedStudent != null && (amount.toLongOrNull() ?: 0L) > 0L
            ) {
                Text(strings.save)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

@Composable
private fun RecordExpenseDialog(
    schoolViewModel: SchoolViewModel,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("SUPPLIES") }
    var beneficiary by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enregistrer une dépense") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Libellé *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Montant (FCFA) *") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("SALARY", "UTILITIES", "MAINTENANCE", "SUPPLIES").forEach { c ->
                        OutlinedButton(onClick = { category = c }) {
                            Text(if (category == c) "✓ ${c.take(4)}" else c.take(4), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                OutlinedTextField(value = beneficiary, onValueChange = { beneficiary = it }, label = { Text("Bénéficiaire") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    schoolViewModel.recordExpense(title, category, amount.toLongOrNull() ?: 0L, "CASH", beneficiary, "")
                    onDismiss()
                },
                enabled = title.trim().length >= 2 && (amount.toLongOrNull() ?: 0L) > 0L
            ) {
                Text(strings.save)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}
