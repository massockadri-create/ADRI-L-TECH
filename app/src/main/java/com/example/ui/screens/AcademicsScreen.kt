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
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
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
import com.example.domain.AcademicCalculator
import com.example.domain.FinanceCalculator
import com.example.localization.LocalAppStrings
import com.example.model.School
import com.example.model.SchoolClass
import com.example.model.Subject
import com.example.model.UiState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.Turquoise
import com.example.ui.theme.tabularNumbers
import com.example.viewmodel.SchoolViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademicsScreen(
    school: School,
    schoolViewModel: SchoolViewModel,
    offlineEntryMode: Boolean
) {
    val strings = LocalAppStrings.current
    val years = (schoolViewModel.yearsState.collectAsStateWithLifecycle().value as? UiState.Success)?.data ?: emptyList()
    val classes = (schoolViewModel.classesState.collectAsStateWithLifecycle().value as? UiState.Success)?.data ?: emptyList()
    val subjects = (schoolViewModel.subjectsState.collectAsStateWithLifecycle().value as? UiState.Success)?.data ?: emptyList()
    val schedules = (schoolViewModel.schedulesState.collectAsStateWithLifecycle().value as? UiState.Success)?.data ?: emptyList()
    val students = (schoolViewModel.studentsState.collectAsStateWithLifecycle().value as? UiState.Success)?.data ?: emptyList()
    val assessments = (schoolViewModel.assessmentsState.collectAsStateWithLifecycle().value as? UiState.Success)?.data ?: emptyList()
    val grades = (schoolViewModel.gradesState.collectAsStateWithLifecycle().value as? UiState.Success)?.data ?: emptyList()
    val council = (schoolViewModel.councilState.collectAsStateWithLifecycle().value as? UiState.Success)?.data ?: emptyList()

    var subTab by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedTerm by remember { mutableStateOf("T1") }
    var selectedClassForStats by remember(classes) { mutableStateOf(classes.firstOrNull()) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            ScrollableTabRow(selectedTabIndex = subTab, edgePadding = 12.dp) {
                listOf("Années", "Classes & Frais", "Matières", "Emploi du temps", "Évals & Notes", "Moyennes & Conseil").forEachIndexed { idx, title ->
                    Tab(selected = subTab == idx, onClick = { subTab = idx }, text = { Text(title) })
                }
            }

            LazyColumn(
                modifier = Modifier.widthIn(max = 920.dp).fillMaxSize(),
                contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (subTab) {
                    0 -> {
                        if (years.isEmpty()) item { EmptyStateCard("Aucune année scolaire", "Ajoutez votre année scolaire (ex: 2026-2027).") }
                        else items(years, key = { it.id }) { yr ->
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(yr.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text("${yr.termSystem} • Seuil passage: ${yr.passingAverage}/20", style = MaterialTheme.typography.bodySmall)
                                    }
                                    StatusBadge(yr.status)
                                }
                            }
                        }
                    }

                    1 -> {
                        if (classes.isEmpty()) item { EmptyStateCard("Aucune classe configurée", "Créez vos classes avec les frais d'inscription et de scolarité.") }
                        else items(classes, key = { it.id }) { cls ->
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                Column(Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("${cls.name} (${cls.level})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                        Text("Cap: ${cls.capacity}", style = MaterialTheme.typography.labelMedium)
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "Inscription: ${FinanceCalculator.formatFcfa(cls.registrationFeeFcfa)} • Scolarité: ${FinanceCalculator.formatFcfa(cls.tuitionFeeFcfa)}",
                                        style = MaterialTheme.typography.bodySmall.tabularNumbers(),
                                        color = Turquoise
                                    )
                                    if (cls.mainTeacherName.isNotBlank()) {
                                        Text("Titulaire: ${cls.mainTeacherName}", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        if (subjects.isEmpty()) item { EmptyStateCard("Aucune matière", "Ajoutez les matières, coefficients et programmes par classe.") }
                        else items(subjects, key = { it.id }) { subj ->
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                Column(Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("${subj.name} [${subj.code}]", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                        StatusBadge("Coef ${subj.coefficient}")
                                    }
                                    Text("Classe: ${subj.className} • ${subj.weeklyHours}h/sem • ${subj.teacherName}", style = MaterialTheme.typography.bodySmall)
                                    if (subj.syllabusSummary.isNotBlank()) {
                                        Text("Programme: ${subj.syllabusSummary}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }

                    3 -> {
                        if (schedules.isEmpty()) item { EmptyStateCard("Aucun emploi du temps", "Ajoutez les créneaux horaires par classe.") }
                        else items(schedules, key = { it.id }) { slot ->
                            val dayName = listOf("Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche").getOrElse((slot.dayOfWeek - 1).toInt()) { "Jour" }
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text("$dayName ${slot.startTime}-${slot.endTime} • ${slot.subjectName}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Text("${slot.className} • Salle: ${slot.room.ifBlank { "-" }} • ${slot.teacherName}", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }

                    4 -> {
                        item {
                            Text("Évaluations (${assessments.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        if (assessments.isEmpty()) {
                            item { EmptyStateCard("Aucune évaluation", "Créez une évaluation puis saisissez les notes (y compris hors connexion).") }
                        } else {
                            items(assessments, key = { it.id }) { asmt ->
                                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text("${asmt.title} (${asmt.subjectName})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                            Text("${asmt.type} • ${asmt.term} • /${asmt.maxScore}", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            Spacer(Modifier.height(8.dp))
                            Text("Dernières notes saisies (${grades.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        items(grades, key = { it.id }) { g ->
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text("${g.studentName} (${g.studentMatricule})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Text("${g.subjectName} • ${g.assessmentTitle} (${g.term})", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text(
                                        "${String.format(Locale.US, "%.2f", g.score)}/20",
                                        style = MaterialTheme.typography.titleMedium.tabularNumbers(),
                                        fontWeight = FontWeight.Bold,
                                        color = Turquoise
                                    )
                                }
                            }
                        }
                    }

                    5 -> {
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Calcul des Moyennes, Classements & Conseil de Classe", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf("T1", "T2", "T3").forEach { t ->
                                            OutlinedButton(onClick = { selectedTerm = t }) {
                                                Text(if (selectedTerm == t) "✓ $t" else t)
                                            }
                                        }
                                    }
                                    if (classes.isNotEmpty()) {
                                        ClassPickerRow(classes = classes, selected = selectedClassForStats, onSelect = { selectedClassForStats = it })
                                    }
                                    selectedClassForStats?.let { cls ->
                                        Button(
                                            onClick = { schoolViewModel.runCouncilDeliberation(cls, selectedTerm) },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.Calculate, contentDescription = null)
                                            Text("  Calculer & Enregistrer Conseil (${cls.name})")
                                        }
                                    }
                                }
                            }
                        }

                        val activeCls = selectedClassForStats
                        if (activeCls != null) {
                            val clsStudents = students.filter { it.classId == activeCls.id }
                            val clsSubjects = subjects.filter { it.classId == activeCls.id }
                            val clsGrades = grades.filter { it.classId == activeCls.id }
                            val summaries = AcademicCalculator.computeClassBulletinSummaries(clsStudents, clsSubjects, clsGrades, selectedTerm)
                            if (summaries.isEmpty()) {
                                item { EmptyStateCard("Aucun élève dans ${activeCls.name}", "Inscrivez des élèves et saisissez leurs notes.") }
                            } else {
                                items(summaries, key = { it.student.id }) { sum ->
                                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Column(Modifier.weight(1f)) {
                                                Text("#${sum.rank} — ${sum.student.fullName}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                                Text("Moyenne: ${String.format(Locale.US, "%.2f", sum.overallAverage)}/20 • ${sum.mention}", style = MaterialTheme.typography.bodySmall.tabularNumbers())
                                                if (sum.missingSubjectNames.isNotEmpty()) {
                                                    Text("Manquantes: ${sum.missingSubjectNames.joinToString()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                                }
                                            }
                                            StatusBadge(sum.suggestedDecision)
                                        }
                                    }
                                }
                            }
                        }

                        if (council.isNotEmpty()) {
                            item { Text("Décisions enregistrées (${council.size})", style = MaterialTheme.typography.titleSmall) }
                            items(council.take(10), key = { it.id }) { c ->
                                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text("${c.studentName} (${c.className} ${c.term}): ${c.average}/20", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                        StatusBadge(c.decision)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (subTab in 0..4) {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = RoyalBlue,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag("academics_add_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = strings.add)
            }
        }
    }

    if (showAddDialog) {
        AcademicsAddDialog(
            subTab = subTab,
            classes = classes,
            subjects = subjects,
            students = students,
            assessments = assessments,
            offlineMode = offlineEntryMode,
            schoolViewModel = schoolViewModel,
            onDismiss = { showAddDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassPickerRow(
    classes: List<SchoolClass>,
    selected: SchoolClass?,
    onSelect: (SchoolClass) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected?.name ?: "Sélectionner une classe",
            onValueChange = {},
            readOnly = true,
            label = { Text("Classe") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            classes.forEach { cls ->
                DropdownMenuItem(
                    text = { Text("${cls.name} (${cls.level})") },
                    onClick = { onSelect(cls); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun AcademicsAddDialog(
    subTab: Int,
    classes: List<SchoolClass>,
    subjects: List<Subject>,
    students: List<com.example.model.Student>,
    assessments: List<com.example.model.Assessment>,
    offlineMode: Boolean,
    schoolViewModel: SchoolViewModel,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    var field1 by remember { mutableStateOf("") }
    var field2 by remember { mutableStateOf("") }
    var field3 by remember { mutableStateOf("") }
    var field4 by remember { mutableStateOf("") }
    var selectedClass by remember { mutableStateOf(classes.firstOrNull()) }
    var selectedSubject by remember { mutableStateOf(subjects.firstOrNull()) }
    var selectedStudent by remember { mutableStateOf(students.firstOrNull()) }
    var selectedAssessment by remember { mutableStateOf(assessments.firstOrNull()) }
    var gradeMode by remember { mutableStateOf(assessments.isNotEmpty() && students.isNotEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when (subTab) {
                    0 -> "Nouvelle année scolaire"
                    1 -> "Nouvelle classe"
                    2 -> "Nouvelle matière"
                    3 -> "Nouveau créneau emploi du temps"
                    else -> if (gradeMode) "Saisir une note" else "Nouvelle évaluation"
                }
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (subTab) {
                    0 -> {
                        OutlinedTextField(value = field1, onValueChange = { field1 = it }, label = { Text("Année (ex: 2026-2027) *") })
                        OutlinedTextField(value = field2, onValueChange = { field2 = it }, label = { Text("Début (2026-09-01)") })
                        OutlinedTextField(value = field3, onValueChange = { field3 = it }, label = { Text("Fin (2027-06-30)") })
                        OutlinedTextField(value = field4, onValueChange = { field4 = it }, label = { Text("Moyenne de passage (10.0)") })
                    }
                    1 -> {
                        OutlinedTextField(value = field1, onValueChange = { field1 = it }, label = { Text("Nom classe (ex: 6ème A) *") })
                        OutlinedTextField(value = field2, onValueChange = { field2 = it }, label = { Text("Niveau (ex: Collège)") })
                        OutlinedTextField(value = field3, onValueChange = { field3 = it }, label = { Text("Scolarité FCFA (ex: 150000)") })
                        OutlinedTextField(value = field4, onValueChange = { field4 = it }, label = { Text("Inscription FCFA (ex: 25000)") })
                    }
                    2 -> {
                        if (classes.isNotEmpty()) ClassPickerRow(classes, selectedClass) { selectedClass = it }
                        OutlinedTextField(value = field1, onValueChange = { field1 = it }, label = { Text("Matière (ex: Mathématiques) *") })
                        OutlinedTextField(value = field2, onValueChange = { field2 = it }, label = { Text("Code (ex: MATH)") })
                        OutlinedTextField(value = field3, onValueChange = { field3 = it }, label = { Text("Coefficient (ex: 4)") })
                        OutlinedTextField(value = field4, onValueChange = { field4 = it }, label = { Text("Enseignant / Programme") })
                    }
                    3 -> {
                        if (classes.isNotEmpty()) ClassPickerRow(classes, selectedClass) { selectedClass = it }
                        OutlinedTextField(value = field1, onValueChange = { field1 = it }, label = { Text("Jour (1=Lun..6=Sam)") })
                        OutlinedTextField(value = field2, onValueChange = { field2 = it }, label = { Text("Début (ex: 08:00)") })
                        OutlinedTextField(value = field3, onValueChange = { field3 = it }, label = { Text("Fin (ex: 10:00)") })
                        OutlinedTextField(value = field4, onValueChange = { field4 = it }, label = { Text("Salle") })
                    }
                    else -> {
                        if (assessments.isNotEmpty() && students.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { gradeMode = false }) { Text(if (!gradeMode) "✓ Évaluation" else "Évaluation") }
                                OutlinedButton(onClick = { gradeMode = true }) { Text(if (gradeMode) "✓ Note" else "Note") }
                            }
                        }
                        if (gradeMode && selectedAssessment != null && selectedStudent != null) {
                            Text("Élève: ${selectedStudent!!.fullName}", style = MaterialTheme.typography.labelLarge)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                students.take(4).forEach { st ->
                                    OutlinedButton(onClick = { selectedStudent = st }) {
                                        Text(st.firstName.take(8))
                                    }
                                }
                            }
                            Text("Éval: ${selectedAssessment!!.title}", style = MaterialTheme.typography.labelMedium)
                            OutlinedTextField(value = field1, onValueChange = { field1 = it }, label = { Text("Note /20 *") })
                            OutlinedTextField(value = field2, onValueChange = { field2 = it }, label = { Text("Appréciation") })
                        } else {
                            if (classes.isNotEmpty()) ClassPickerRow(classes, selectedClass) { selectedClass = it }
                            OutlinedTextField(value = field1, onValueChange = { field1 = it }, label = { Text("Titre évaluation (ex: Devoir 1) *") })
                            OutlinedTextField(value = field2, onValueChange = { field2 = it }, label = { Text("Trimestre (T1, T2, T3)") })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when (subTab) {
                        0 -> schoolViewModel.addAcademicYear(
                            field1.ifBlank { "2026-2027" },
                            field2.ifBlank { "2026-09-01" },
                            field3.ifBlank { "2027-06-30" },
                            "TRIMESTER",
                            field4.toDoubleOrNull() ?: 10.0
                        )
                        1 -> schoolViewModel.addClass(
                            field1.ifBlank { "6ème A" },
                            field2.ifBlank { "Collège" },
                            50L,
                            field3.toLongOrNull() ?: 0L,
                            field4.toLongOrNull() ?: 0L,
                            ""
                        )
                        2 -> selectedClass?.let { cls ->
                            schoolViewModel.addSubject(
                                field1.ifBlank { "Mathématiques" },
                                field2,
                                cls,
                                field3.toDoubleOrNull() ?: 2.0,
                                4L,
                                field4,
                                ""
                            )
                        }
                        3 -> {
                            val cls = selectedClass
                            val subj = selectedSubject ?: subjects.firstOrNull()
                            if (cls != null && subj != null) {
                                schoolViewModel.addScheduleSlot(
                                    cls,
                                    subj,
                                    (field1.toLongOrNull() ?: 1L).coerceIn(1L, 7L),
                                    field2.ifBlank { "08:00" },
                                    field3.ifBlank { "10:00" },
                                    field4
                                )
                            }
                        }
                        else -> {
                            if (gradeMode && selectedAssessment != null && selectedStudent != null) {
                                val subj = subjects.firstOrNull { it.id == selectedAssessment!!.subjectId }
                                    ?: Subject(id = selectedAssessment!!.subjectId, name = selectedAssessment!!.subjectName, coefficient = 1.0)
                                schoolViewModel.recordGrade(
                                    selectedStudent!!,
                                    selectedAssessment!!,
                                    subj,
                                    field1.toDoubleOrNull() ?: 0.0,
                                    field2,
                                    offlineMode
                                )
                            } else {
                                val cls = selectedClass
                                val subj = subjects.firstOrNull { it.classId == cls?.id } ?: subjects.firstOrNull()
                                if (cls != null && subj != null) {
                                    schoolViewModel.addAssessment(
                                        field1.ifBlank { "Devoir 1" },
                                        "DEVOIR",
                                        field2.uppercase().ifBlank { "T1" },
                                        cls,
                                        subj
                                    )
                                }
                            }
                        }
                    }
                    onDismiss()
                }
            ) {
                Text(strings.save)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}
