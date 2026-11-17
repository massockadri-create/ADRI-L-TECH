package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.AcademicCalculator
import com.example.domain.FinanceCalculator
import com.example.domain.PdfGenerator
import com.example.domain.SchoolDocumentType
import com.example.localization.AppLanguage
import com.example.localization.LocalAppLanguage
import com.example.localization.LocalAppStrings
import com.example.model.PaymentRecord
import com.example.model.School
import com.example.model.SchoolClass
import com.example.model.Student
import com.example.model.UiState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.SearchFilterBar
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.Turquoise
import com.example.ui.theme.tabularNumbers
import com.example.viewmodel.SchoolViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DocumentsPortalScreen(
    school: School,
    schoolViewModel: SchoolViewModel,
    initialTab: Int = 0
) {
    val strings = LocalAppStrings.current
    val language = LocalAppLanguage.current
    val context = LocalContext.current

    val studentsState by schoolViewModel.studentsState.collectAsStateWithLifecycle()
    val classesState by schoolViewModel.classesState.collectAsStateWithLifecycle()
    val subjectsState by schoolViewModel.subjectsState.collectAsStateWithLifecycle()
    val gradesState by schoolViewModel.gradesState.collectAsStateWithLifecycle()
    val attendanceState by schoolViewModel.attendanceState.collectAsStateWithLifecycle()
    val schedulesState by schoolViewModel.schedulesState.collectAsStateWithLifecycle()
    val paymentsState by schoolViewModel.paymentsState.collectAsStateWithLifecycle()
    val notificationsState by schoolViewModel.notificationsState.collectAsStateWithLifecycle()
    val studentBalances by schoolViewModel.studentBalances.collectAsStateWithLifecycle()
    val cashSummary by schoolViewModel.cashRegisterSummary.collectAsStateWithLifecycle()

    val students = (studentsState as? UiState.Success)?.data ?: emptyList()
    val classes = (classesState as? UiState.Success)?.data ?: emptyList()
    val subjects = (subjectsState as? UiState.Success)?.data ?: emptyList()
    val grades = (gradesState as? UiState.Success)?.data ?: emptyList()
    val attendance = (attendanceState as? UiState.Success)?.data ?: emptyList()
    val schedules = (schedulesState as? UiState.Success)?.data ?: emptyList()
    val payments = (paymentsState as? UiState.Success)?.data ?: emptyList()
    val notifications = (notificationsState as? UiState.Success)?.data ?: emptyList()

    var selectedTab by remember { mutableIntStateOf(initialTab) }
    LaunchedEffect(initialTab) { selectedTab = initialTab }

    var showNotifyDialog by remember { mutableStateOf(false) }

    val tabs = if (language == AppLanguage.FR) {
        listOf("Documents PDF", "Portail Parents/Élèves", "Communication")
    } else {
        listOf("PDF Documents", "Parent & Student Portal", "Communications")
    }

    Scaffold(
        floatingActionButton = {
            if (selectedTab == 2) {
                FloatingActionButton(
                    onClick = { showNotifyDialog = true },
                    containerColor = RoyalBlue,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_notification_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Nouvelle notification")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { idx, title ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = { Text(title, maxLines = 1) },
                        modifier = Modifier.testTag("docs_tab_$idx")
                    )
                }
            }

            when (selectedTab) {
                0 -> {
                    var selectedDocType by remember { mutableStateOf(SchoolDocumentType.BULLETIN) }
                    var selectedStudent by remember(students) { mutableStateOf(students.firstOrNull()) }
                    var selectedClass by remember(classes) { mutableStateOf(classes.firstOrNull()) }
                    var selectedPayment by remember(payments) { mutableStateOf(payments.firstOrNull()) }
                    var selectedTerm by remember { mutableStateOf("T1") }
                    var lastGeneratedFile by remember { mutableStateOf<File?>(null) }
                    var generationStatus by remember { mutableStateOf<String?>(null) }

                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                        LazyColumn(
                            modifier = Modifier.widthIn(max = 900.dp).fillMaxSize(),
                            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                SectionHeader(
                                    title = strings.navDocuments,
                                    subtitle = if (language == AppLanguage.FR) {
                                        "9 documents officiels A4 intégrant automatiquement logos, cachet et signature"
                                    } else {
                                        "9 official A4 PDF documents embedding school logos, stamp, and signature"
                                    }
                                )
                            }

                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = if (language == AppLanguage.FR) "Type de document" else "Document Type",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            SchoolDocumentType.entries.forEach { docType ->
                                                FilterChip(
                                                    selected = selectedDocType == docType,
                                                    onClick = { selectedDocType = docType },
                                                    label = {
                                                        Text(if (language == AppLanguage.EN) docType.labelEn else docType.labelFr)
                                                    },
                                                    modifier = Modifier.testTag("doc_type_${docType.name.lowercase()}")
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (selectedDocType in listOf(
                                    SchoolDocumentType.BULLETIN,
                                    SchoolDocumentType.INVOICE,
                                    SchoolDocumentType.ENROLLMENT_CERT,
                                    SchoolDocumentType.ATTESTATION,
                                    SchoolDocumentType.STUDENT_CARD
                                )
                            ) {
                                item {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = if (language == AppLanguage.FR) "Élève concerné" else "Target Student",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (students.isEmpty()) {
                                                Text(
                                                    text = if (language == AppLanguage.FR) "Aucun élève inscrit pour le moment." else "No students enrolled yet.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            } else {
                                                FlowRow(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    students.take(25).forEach { student ->
                                                        FilterChip(
                                                            selected = selectedStudent?.id == student.id,
                                                            onClick = { selectedStudent = student },
                                                            label = { Text("${student.matricule} • ${student.fullName}") }
                                                        )
                                                    }
                                                }
                                            }
                                            if (selectedDocType == SchoolDocumentType.BULLETIN) {
                                                Spacer(Modifier.height(4.dp))
                                                Text(
                                                    text = if (language == AppLanguage.FR) "Trimestre / Période" else "Term",
                                                    style = MaterialTheme.typography.labelLarge
                                                )
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    listOf("T1", "T2", "T3").forEach { term ->
                                                        FilterChip(
                                                            selected = selectedTerm == term,
                                                            onClick = { selectedTerm = term },
                                                            label = { Text(term) }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (selectedDocType == SchoolDocumentType.CLASS_ROSTER) {
                                item {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = if (language == AppLanguage.FR) "Classe concernée" else "Target Class",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (classes.isEmpty()) {
                                                Text(
                                                    text = if (language == AppLanguage.FR) "Aucune classe créée." else "No classes created.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            } else {
                                                FlowRow(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    classes.forEach { cls ->
                                                        FilterChip(
                                                            selected = selectedClass?.id == cls.id,
                                                            onClick = { selectedClass = cls },
                                                            label = { Text(cls.name) }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (selectedDocType == SchoolDocumentType.RECEIPT) {
                                item {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = if (language == AppLanguage.FR) "Paiement / Reçu" else "Payment Receipt",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (payments.isEmpty()) {
                                                Text(
                                                    text = if (language == AppLanguage.FR) "Aucun paiement enregistré." else "No payments recorded.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            } else {
                                                FlowRow(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    payments.take(20).forEach { pay ->
                                                        FilterChip(
                                                            selected = selectedPayment?.id == pay.id,
                                                            onClick = { selectedPayment = pay },
                                                            label = { Text("${pay.receiptNumber} • ${pay.studentName} (${FinanceCalculator.formatFcfa(pay.amountFcfa)})") }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                Button(
                                    onClick = {
                                        val targetStudent = selectedStudent
                                        val targetClass = when (selectedDocType) {
                                            SchoolDocumentType.CLASS_ROSTER -> selectedClass
                                            else -> classes.firstOrNull { it.id == targetStudent?.classId }
                                        }
                                        val targetBulletin = if (selectedDocType == SchoolDocumentType.BULLETIN && targetStudent != null) {
                                            val classStudents = students.filter { it.classId == targetStudent.classId }
                                            val classSubjects = subjects.filter { it.classId == targetStudent.classId }
                                            val classGrades = grades.filter { it.classId == targetStudent.classId }
                                            AcademicCalculator.computeClassBulletinSummaries(
                                                classStudents = classStudents,
                                                classSubjects = classSubjects,
                                                classGrades = classGrades,
                                                term = selectedTerm
                                            ).firstOrNull { it.student.id == targetStudent.id }
                                        } else null

                                        val balance = targetStudent?.let { st ->
                                            studentBalances.firstOrNull { it.student.id == st.id }
                                        } ?: selectedPayment?.let { pay ->
                                            studentBalances.firstOrNull { it.student.id == pay.studentId }
                                        }

                                        val roster = targetClass?.let { cls ->
                                            students.filter { it.classId == cls.id }
                                        } ?: emptyList()

                                        PdfGenerator.generateDocumentPdf(
                                            context = context,
                                            school = school,
                                            docType = selectedDocType,
                                            student = targetStudent,
                                            schoolClass = targetClass,
                                            bulletin = targetBulletin,
                                            payment = selectedPayment,
                                            studentBalance = balance,
                                            classRoster = roster,
                                            cashSummary = cashSummary,
                                            unpaidList = studentBalances,
                                            languageCode = language.code
                                        ).onSuccess { file ->
                                            lastGeneratedFile = file
                                            generationStatus = if (language == AppLanguage.FR) {
                                                "Document généré : ${file.name}"
                                            } else {
                                                "Generated PDF: ${file.name}"
                                            }
                                            PdfGenerator.openOrSharePdf(context, file)
                                        }.onFailure { err ->
                                            generationStatus = err.localizedMessage ?: "PDF Error"
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("generate_pdf_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                                    Spacer(Modifier.width(10.dp))
                                    Text(strings.generatePdf, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            if (generationStatus != null || lastGeneratedFile != null) {
                                item {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = generationStatus ?: "",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                lastGeneratedFile?.let { file ->
                                                    Text(
                                                        text = "${file.length() / 1024 + 1} Ko",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            lastGeneratedFile?.let { file ->
                                                OutlinedButton(
                                                    onClick = { PdfGenerator.openOrSharePdf(context, file) },
                                                    modifier = Modifier.testTag("share_last_pdf_btn")
                                                ) {
                                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                                    Spacer(Modifier.width(6.dp))
                                                    Text(strings.sharePdf)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    var query by remember { mutableStateOf("") }
                    val filteredStudents = remember(students, query) {
                        if (query.isBlank()) students else students.filter {
                            it.fullName.contains(query, ignoreCase = true) ||
                                it.matricule.contains(query, ignoreCase = true) ||
                                it.parentName.contains(query, ignoreCase = true) ||
                                it.parentPhone.contains(query, ignoreCase = true)
                        }
                    }
                    var selectedPortalStudent by remember(filteredStudents) {
                        mutableStateOf(filteredStudents.firstOrNull())
                    }
                    var portalTerm by remember { mutableStateOf("T1") }

                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                        LazyColumn(
                            modifier = Modifier.widthIn(max = 900.dp).fillMaxSize(),
                            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                SectionHeader(
                                    title = strings.navParentPortal,
                                    subtitle = if (language == AppLanguage.FR) {
                                        "Suivi individuel : notes, moyenne, absences, emploi du temps et paiements"
                                    } else {
                                        "Individual tracking: grades, average, attendance, timetable, and payments"
                                    }
                                )
                            }
                            item {
                                SearchFilterBar(
                                    query = query,
                                    onQueryChange = { query = it },
                                    placeholder = if (language == AppLanguage.FR) "Rechercher élève, matricule ou parent..." else "Search student, ID, or parent..."
                                )
                            }

                            if (filteredStudents.isEmpty()) {
                                item { EmptyStateCard() }
                            } else {
                                item {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        filteredStudents.take(20).forEach { st ->
                                            FilterChip(
                                                selected = selectedPortalStudent?.id == st.id,
                                                onClick = { selectedPortalStudent = st },
                                                label = { Text("${st.matricule} • ${st.fullName}") }
                                            )
                                        }
                                    }
                                }

                                selectedPortalStudent?.let { st ->
                                    val classObj = classes.firstOrNull { it.id == st.classId }
                                    val balance = studentBalances.firstOrNull { it.student.id == st.id }
                                    val classStudents = students.filter { it.classId == st.classId }
                                    val classSubjects = subjects.filter { it.classId == st.classId }
                                    val classGrades = grades.filter { it.classId == st.classId }
                                    val bulletin = AcademicCalculator.computeClassBulletinSummaries(
                                        classStudents = classStudents,
                                        classSubjects = classSubjects,
                                        classGrades = classGrades,
                                        term = portalTerm
                                    ).firstOrNull { it.student.id == st.id }

                                    val studentAttendance = attendance.filter { it.studentId == st.id }
                                    val absencesCount = studentAttendance.count { it.status == "ABSENT" }
                                    val latesCount = studentAttendance.count { it.status == "LATE" }
                                    val studentSchedules = schedules.filter { it.classId == st.classId }
                                        .sortedWith(compareBy({ it.dayOfWeek }, { it.startTime }))
                                    val studentPayments = payments.filter { it.studentId == st.id }

                                    item {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            listOf("T1", "T2", "T3").forEach { term ->
                                                FilterChip(
                                                    selected = portalTerm == term,
                                                    onClick = { portalTerm = term },
                                                    label = { Text(term) }
                                                )
                                            }
                                        }
                                    }

                                    item {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(st.fullName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                                    StatusBadge(st.enrollmentStatus)
                                                }
                                                Text(
                                                    text = "${st.matricule} • ${st.className} • Parent: ${st.parentName.ifBlank { "-" }} (${st.parentPhone.ifBlank { "-" }})",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    KpiMetricCard(
                                                        title = if (language == AppLanguage.FR) "Moyenne ($portalTerm)" else "Average ($portalTerm)",
                                                        value = String.format(Locale.US, "%.2f / 20", bulletin?.overallAverage ?: 0.0),
                                                        subtitle = "Rang ${bulletin?.rank ?: "-"} / ${bulletin?.totalStudentsInClass ?: 0}",
                                                        icon = Icons.Default.FamilyRestroom,
                                                        accentColor = RoyalBlue,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    KpiMetricCard(
                                                        title = if (language == AppLanguage.FR) "Reste à payer" else "Balance Due",
                                                        value = FinanceCalculator.formatFcfa(balance?.remainingBalanceFcfa ?: 0L),
                                                        subtitle = "Absences: $absencesCount • Retards: $latesCount",
                                                        icon = Icons.Default.Description,
                                                        accentColor = if ((balance?.remainingBalanceFcfa ?: 0L) > 0L) CrimsonError else EmeraldSuccess,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                                Spacer(Modifier.height(6.dp))
                                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    OutlinedButton(
                                                        onClick = {
                                                            PdfGenerator.generateDocumentPdf(
                                                                context = context,
                                                                school = school,
                                                                docType = SchoolDocumentType.BULLETIN,
                                                                student = st,
                                                                schoolClass = classObj,
                                                                bulletin = bulletin,
                                                                languageCode = language.code
                                                            ).onSuccess { PdfGenerator.openOrSharePdf(context, it) }
                                                        },
                                                        modifier = Modifier.testTag("portal_bulletin_pdf_btn")
                                                    ) {
                                                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                                        Spacer(Modifier.width(6.dp))
                                                        Text("Bulletin PDF")
                                                    }
                                                    OutlinedButton(
                                                        onClick = {
                                                            PdfGenerator.generateDocumentPdf(
                                                                context = context,
                                                                school = school,
                                                                docType = SchoolDocumentType.ENROLLMENT_CERT,
                                                                student = st,
                                                                schoolClass = classObj,
                                                                languageCode = language.code
                                                            ).onSuccess { PdfGenerator.openOrSharePdf(context, it) }
                                                        },
                                                        modifier = Modifier.testTag("portal_cert_pdf_btn")
                                                    ) {
                                                        Text(if (language == AppLanguage.FR) "Certificat scolarité" else "Certificate")
                                                    }
                                                    OutlinedButton(
                                                        onClick = {
                                                            PdfGenerator.generateDocumentPdf(
                                                                context = context,
                                                                school = school,
                                                                docType = SchoolDocumentType.INVOICE,
                                                                student = st,
                                                                schoolClass = classObj,
                                                                studentBalance = balance,
                                                                languageCode = language.code
                                                            ).onSuccess { PdfGenerator.openOrSharePdf(context, it) }
                                                        },
                                                        modifier = Modifier.testTag("portal_invoice_pdf_btn")
                                                    ) {
                                                        Text(if (language == AppLanguage.FR) "Facture PDF" else "Invoice PDF")
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (bulletin != null && bulletin.subjectResults.isNotEmpty()) {
                                        item {
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Text(
                                                        text = if (language == AppLanguage.FR) "Détail des notes ($portalTerm)" else "Subject Breakdown ($portalTerm)",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    bulletin.subjectResults.forEach { res ->
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Text("${res.subjectName} (coef ${res.coefficient})", modifier = Modifier.weight(1f))
                                                            Text(
                                                                text = String.format(Locale.US, "%.2f / 20", res.averageScore),
                                                                style = MaterialTheme.typography.bodyMedium.tabularNumbers(),
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = Turquoise
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (studentSchedules.isNotEmpty()) {
                                        item {
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.Schedule, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(18.dp))
                                                        Spacer(Modifier.width(8.dp))
                                                        Text(
                                                            text = if (language == AppLanguage.FR) "Emploi du temps (${st.className})" else "Class Timetable",
                                                            style = MaterialTheme.typography.titleMedium,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                    studentSchedules.forEach { slot ->
                                                        Text(
                                                            text = "${dayLabel(slot.dayOfWeek, language)} • ${slot.startTime}-${slot.endTime} : ${slot.subjectName} (${slot.room.ifBlank { "-" }})",
                                                            style = MaterialTheme.typography.bodySmall
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (studentPayments.isNotEmpty()) {
                                        item {
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Text(
                                                        text = if (language == AppLanguage.FR) "Historique des paiements" else "Payment History",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    studentPayments.forEach { pay ->
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text("${pay.receiptNumber} • ${pay.feeCategory}", fontWeight = FontWeight.Medium)
                                                                Text("${pay.method} • ${formatDate(pay.paidAtMillis)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                            }
                                                            Text(
                                                                text = FinanceCalculator.formatFcfa(pay.amountFcfa),
                                                                style = MaterialTheme.typography.bodyMedium.tabularNumbers(),
                                                                fontWeight = FontWeight.Bold
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
                    }
                }

                2 -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                        LazyColumn(
                            modifier = Modifier.widthIn(max = 900.dp).fillMaxSize(),
                            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                SectionHeader(
                                    title = if (language == AppLanguage.FR) "Communication Parents & Équipe" else "School Communications",
                                    subtitle = if (language == AppLanguage.FR) {
                                        "Annonces ciblées parents, enseignants ou tout l'établissement"
                                    } else {
                                        "Announcements for parents, teachers, or entire school"
                                    }
                                )
                            }
                            if (notifications.isEmpty()) {
                                item { EmptyStateCard() }
                            } else {
                                items(notifications, key = { it.id }) { notif ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                    Icon(Icons.Default.Campaign, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(18.dp))
                                                    Spacer(Modifier.width(8.dp))
                                                    Text(notif.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                                }
                                                StatusBadge(notif.priority)
                                            }
                                            Text(notif.message, style = MaterialTheme.typography.bodyMedium)
                                            Text(
                                                text = "Cible: ${notif.audience} • Par: ${notif.senderName}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
    }

    if (showNotifyDialog) {
        var title by remember { mutableStateOf("") }
        var message by remember { mutableStateOf("") }
        var audience by remember { mutableStateOf("PARENTS") }
        var priority by remember { mutableStateOf("NORMAL") }

        AlertDialog(
            onDismissRequest = { showNotifyDialog = false },
            title = { Text(if (language == AppLanguage.FR) "Nouvelle communication" else "New Announcement") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Titre *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("notif_title_input")
                    )
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        label = { Text("Message *") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth().testTag("notif_message_input")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("ALL", "PARENTS", "TEACHERS").forEach { aud ->
                            FilterChip(
                                selected = audience == aud,
                                onClick = { audience = aud },
                                label = { Text(aud) }
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("NORMAL", "URGENT").forEach { prio ->
                            FilterChip(
                                selected = priority == prio,
                                onClick = { priority = prio },
                                label = { Text(prio) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        schoolViewModel.sendNotification(title, message, audience, priority)
                        showNotifyDialog = false
                    },
                    enabled = title.trim().length >= 2 && message.trim().length >= 2,
                    modifier = Modifier.testTag("confirm_send_notif_btn")
                ) {
                    Text(strings.save)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotifyDialog = false }) { Text(strings.cancel) }
            }
        )
    }
}

private fun dayLabel(day: Long, language: AppLanguage): String = when (day.toInt()) {
    1 -> if (language == AppLanguage.FR) "Lun" else "Mon"
    2 -> if (language == AppLanguage.FR) "Mar" else "Tue"
    3 -> if (language == AppLanguage.FR) "Mer" else "Wed"
    4 -> if (language == AppLanguage.FR) "Jeu" else "Thu"
    5 -> if (language == AppLanguage.FR) "Ven" else "Fri"
    6 -> if (language == AppLanguage.FR) "Sam" else "Sat"
    else -> "J$day"
}

private fun formatDate(millis: Long): String {
    if (millis <= 0L) return "-"
    return SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(Date(millis))
}
