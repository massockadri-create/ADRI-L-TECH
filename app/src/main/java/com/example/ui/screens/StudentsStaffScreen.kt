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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.FinanceCalculator
import com.example.localization.LocalAppStrings
import com.example.model.School
import com.example.model.SchoolClass
import com.example.model.Student
import com.example.model.UiState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SearchFilterBar
import com.example.ui.components.StatusBadge
import com.example.ui.theme.RoyalBlue
import com.example.viewmodel.SchoolViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StudentsStaffScreen(
    school: School,
    schoolViewModel: SchoolViewModel,
    offlineEntryMode: Boolean
) {
    val strings = LocalAppStrings.current
    val students = (schoolViewModel.studentsState.collectAsStateWithLifecycle().value as? UiState.Success)?.data ?: emptyList()
    val classes = (schoolViewModel.classesState.collectAsStateWithLifecycle().value as? UiState.Success)?.data ?: emptyList()
    val members = (schoolViewModel.membersState.collectAsStateWithLifecycle().value as? UiState.Success)?.data ?: emptyList()
    val attendance = (schoolViewModel.attendanceState.collectAsStateWithLifecycle().value as? UiState.Success)?.data ?: emptyList()

    var subTab by remember { mutableIntStateOf(0) }
    var search by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredStudents = remember(students, search) {
        if (search.isBlank()) students
        else students.filter {
            it.fullName.contains(search, ignoreCase = true) ||
                it.matricule.contains(search, ignoreCase = true) ||
                it.className.contains(search, ignoreCase = true)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            TabRow(selectedTabIndex = subTab) {
                Tab(selected = subTab == 0, onClick = { subTab = 0 }, text = { Text("Élèves (${students.size})") })
                Tab(selected = subTab == 1, onClick = { subTab = 1 }, text = { Text("Personnel (${members.size})") })
                Tab(selected = subTab == 2, onClick = { subTab = 2 }, text = { Text("Présences (${attendance.size})") })
            }

            LazyColumn(
                modifier = Modifier.widthIn(max = 920.dp).fillMaxSize(),
                contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (subTab) {
                    0 -> {
                        item {
                            SearchFilterBar(search, { search = it }, strings.searchPlaceholder)
                        }
                        if (filteredStudents.isEmpty()) {
                            item { EmptyStateCard("Aucun élève inscrit", "Inscrivez un élève : son matricule est généré automatiquement.") }
                        } else {
                            items(filteredStudents, key = { it.id }) { student ->
                                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                    Column(Modifier.padding(14.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Column(Modifier.weight(1f)) {
                                                Text(student.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                                Text("${student.matricule} • ${student.className} • ${student.gender}", style = MaterialTheme.typography.bodySmall)
                                            }
                                            StatusBadge(student.enrollmentStatus)
                                        }
                                        if (student.parentName.isNotBlank() || student.parentPhone.isNotBlank()) {
                                            Spacer(Modifier.height(4.dp))
                                            Text("Parent/Tuteur: ${student.parentName} (${student.parentPhone})", style = MaterialTheme.typography.labelSmall)
                                        }
                                        if (student.scholarshipDiscountFcfa > 0L) {
                                            Text("Bourse/Réduction: ${FinanceCalculator.formatFcfa(student.scholarshipDiscountFcfa)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        if (members.isEmpty()) {
                            item { EmptyStateCard("Aucun membre enregistré", "Ajoutez enseignants, personnel administratif et comptes parents.") }
                        } else {
                            items(members, key = { it.id }) { member ->
                                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(member.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Text("${member.phone} • ${member.email} • ${member.subjectSpecialty}", style = MaterialTheme.typography.bodySmall)
                                            if (member.salaryFcfa > 0L) {
                                                Text("Salaire: ${FinanceCalculator.formatFcfa(member.salaryFcfa)}", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                        StatusBadge(member.role)
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        if (attendance.isEmpty()) {
                            item { EmptyStateCard("Aucune fiche de présence", "Enregistrez présences, absences, retards et notes de discipline.") }
                        } else {
                            items(attendance, key = { it.id }) { att ->
                                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text("${att.studentName} (${att.className})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                            Text("${att.dateIso} • Retard: ${att.minutesLate} min • ${att.disciplineNote}", style = MaterialTheme.typography.bodySmall)
                                        }
                                        StatusBadge(att.status)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = RoyalBlue,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("students_staff_add_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = strings.add)
        }
    }

    if (showAddDialog) {
        when (subTab) {
            0 -> EnrollStudentDialog(classes, schoolViewModel) { showAddDialog = false }
            1 -> AddMemberDialog(schoolViewModel) { showAddDialog = false }
            else -> RecordAttendanceDialog(students, offlineEntryMode, schoolViewModel) { showAddDialog = false }
        }
    }
}

@Composable
private fun EnrollStudentDialog(
    classes: List<SchoolClass>,
    schoolViewModel: SchoolViewModel,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("M") }
    var selectedClass by remember { mutableStateOf(classes.firstOrNull()) }
    var birthDate by remember { mutableStateOf("") }
    var parentName by remember { mutableStateOf("") }
    var parentPhone by remember { mutableStateOf("") }
    var parentEmail by remember { mutableStateOf("") }
    var scholarship by remember { mutableStateOf("0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Inscrire un élève (Matricule auto)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (classes.isEmpty()) {
                    Text("Veuillez d'abord créer une classe dans Pédagogie > Classes.", color = MaterialTheme.colorScheme.error)
                } else {
                    ClassPickerRow(classes, selectedClass) { selectedClass = it }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = firstName, onValueChange = { firstName = it }, label = { Text("Prénom *") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = lastName, onValueChange = { lastName = it }, label = { Text("Nom *") }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { gender = "M" }) { Text(if (gender == "M") "✓ M" else "M") }
                    OutlinedButton(onClick = { gender = "F" }) { Text(if (gender == "F") "✓ F" else "F") }
                    OutlinedTextField(value = birthDate, onValueChange = { birthDate = it }, label = { Text("Né(e) le") }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = parentName, onValueChange = { parentName = it }, label = { Text("Nom Parent/Tuteur") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = parentPhone, onValueChange = { parentPhone = it }, label = { Text("Tél parent") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = scholarship, onValueChange = { scholarship = it }, label = { Text("Bourse FCFA") }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = parentEmail, onValueChange = { parentEmail = it }, label = { Text("Email parent (Portail)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedClass?.let { cls ->
                        schoolViewModel.enrollStudent(
                            firstName, lastName, gender, cls, birthDate, "", parentName, parentPhone, parentEmail,
                            scholarship.toLongOrNull() ?: 0L
                        )
                        onDismiss()
                    }
                },
                enabled = selectedClass != null && firstName.isNotBlank() && lastName.isNotBlank()
            ) {
                Text(strings.save)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

@Composable
private fun AddMemberDialog(
    schoolViewModel: SchoolViewModel,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("TEACHER") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var specialty by remember { mutableStateOf("") }
    var salary by remember { mutableStateOf("0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter Personnel / Enseignant / Parent") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nom complet *") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("TEACHER", "BURSAR", "SECRETARY", "PARENT").forEach { r ->
                        OutlinedButton(onClick = { role = r }) {
                            Text(if (role == r) "✓ ${r.take(4)}" else r.take(4), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Téléphone") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = specialty, onValueChange = { specialty = it }, label = { Text("Spécialité / Poste") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = salary, onValueChange = { salary = it }, label = { Text("Salaire FCFA") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    schoolViewModel.addSchoolMember(name, role, email, phone, specialty, salary.toLongOrNull() ?: 0L)
                    onDismiss()
                },
                enabled = name.trim().length >= 2
            ) {
                Text(strings.save)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

@Composable
private fun RecordAttendanceDialog(
    students: List<Student>,
    offlineMode: Boolean,
    schoolViewModel: SchoolViewModel,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    var selectedStudent by remember { mutableStateOf(students.firstOrNull()) }
    var status by remember { mutableStateOf("ABSENT") }
    var justified by remember { mutableStateOf(false) }
    var minutesLate by remember { mutableStateOf("0") }
    var note by remember { mutableStateOf("") }
    val todayIso = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Saisie Présence / Discipline") },
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
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("PRESENT", "ABSENT", "LATE", "EXCUSED").forEach { st ->
                            OutlinedButton(onClick = { status = st }) {
                                Text(if (status == st) "✓ ${st.take(4)}" else st.take(4), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Justifiée", modifier = Modifier.weight(1f))
                        Switch(checked = justified, onCheckedChange = { justified = it })
                    }
                    OutlinedTextField(value = minutesLate, onValueChange = { minutesLate = it }, label = { Text("Minutes retard") })
                    OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note discipline") })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedStudent?.let { st ->
                        schoolViewModel.recordAttendance(st, todayIso, status, justified, minutesLate.toLongOrNull() ?: 0L, note, offlineMode)
                        onDismiss()
                    }
                },
                enabled = selectedStudent != null
            ) {
                Text(strings.save)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}
