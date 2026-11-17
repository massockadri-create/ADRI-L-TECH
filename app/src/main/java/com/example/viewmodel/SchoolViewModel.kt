package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.PendingSyncOpEntity
import com.example.data.local.SyncConflictEntity
import com.example.data.remote.OfflineSyncManager
import com.example.data.remote.PlatformRepository
import com.example.data.remote.SchoolRepository
import com.example.domain.AbsenceAlert
import com.example.domain.AcademicCalculator
import com.example.domain.CashRegisterSummary
import com.example.domain.FinanceCalculator
import com.example.domain.MatriculeGenerator
import com.example.domain.MissingGradeAlert
import com.example.domain.StudentBalanceSummary
import com.example.model.AcademicYear
import com.example.model.Assessment
import com.example.model.AttendanceRecord
import com.example.model.AuditLog
import com.example.model.AutomationLog
import com.example.model.CouncilDecision
import com.example.model.ExpenseRecord
import com.example.model.GradeRecord
import com.example.model.PaymentRecord
import com.example.model.ScheduleSlot
import com.example.model.School
import com.example.model.SchoolClass
import com.example.model.SchoolMember
import com.example.model.SchoolNotification
import com.example.model.Student
import com.example.model.Subject
import com.example.model.UiState
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class SchoolViewModel(
    private val schoolId: String,
    private val isSuperAdmin: Boolean,
    private val currentUserId: String,
    private val currentUserEmail: String,
    private val platformRepo: PlatformRepository,
    private val schoolRepo: SchoolRepository,
    private val syncManager: OfflineSyncManager
) : ViewModel() {

    val schoolState: StateFlow<UiState<School?>> = platformRepo.observeSchool(schoolId)
        .asUiState("School")

    val yearsState = schoolRepo.observeSubcollection(schoolId, "academic_years", isSuperAdmin, AcademicYear::class.java).asListUiState("Years")
    val membersState = schoolRepo.observeSubcollection(schoolId, "school_members", isSuperAdmin, SchoolMember::class.java).asListUiState("Members")
    val classesState = schoolRepo.observeSubcollection(schoolId, "classes", isSuperAdmin, SchoolClass::class.java).asListUiState("Classes")
    val subjectsState = schoolRepo.observeSubcollection(schoolId, "subjects", isSuperAdmin, Subject::class.java).asListUiState("Subjects")
    val schedulesState = schoolRepo.observeSubcollection(schoolId, "schedules", isSuperAdmin, ScheduleSlot::class.java).asListUiState("Schedules")
    val studentsState = schoolRepo.observeSubcollection(schoolId, "students", isSuperAdmin, Student::class.java).asListUiState("Students")
    val assessmentsState = schoolRepo.observeSubcollection(schoolId, "assessments", isSuperAdmin, Assessment::class.java).asListUiState("Assessments")
    val gradesState = schoolRepo.observeSubcollection(schoolId, "grades", isSuperAdmin, GradeRecord::class.java).asListUiState("Grades")
    val attendanceState = schoolRepo.observeSubcollection(schoolId, "attendance", isSuperAdmin, AttendanceRecord::class.java).asListUiState("Attendance")
    val paymentsState = schoolRepo.observeSubcollection(schoolId, "payments", isSuperAdmin, PaymentRecord::class.java).asListUiState("Payments")
    val expensesState = schoolRepo.observeSubcollection(schoolId, "expenses", isSuperAdmin, ExpenseRecord::class.java).asListUiState("Expenses")
    val notificationsState = schoolRepo.observeSubcollection(schoolId, "communications", isSuperAdmin, SchoolNotification::class.java).asListUiState("Notifications")
    val councilState = schoolRepo.observeSubcollection(schoolId, "council_decisions", isSuperAdmin, CouncilDecision::class.java).asListUiState("Council")
    val automationLogsState = schoolRepo.observeSubcollection(schoolId, "automation_logs", isSuperAdmin, AutomationLog::class.java).asListUiState("Automations")
    val auditLogsState = schoolRepo.observeSubcollection(schoolId, "audit_logs", isSuperAdmin, AuditLog::class.java).asListUiState("Audit")

    val pendingSyncOps: StateFlow<List<PendingSyncOpEntity>> = syncManager.observePendingOps(schoolId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val syncConflicts: StateFlow<List<SyncConflictEntity>> = syncManager.observeConflicts(schoolId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val studentBalances: StateFlow<List<StudentBalanceSummary>> = combine(
        studentsState,
        classesState,
        paymentsState
    ) { st, cl, pay ->
        val students = (st as? UiState.Success)?.data ?: emptyList()
        val classes = (cl as? UiState.Success)?.data ?: emptyList()
        val payments = (pay as? UiState.Success)?.data ?: emptyList()
        FinanceCalculator.computeAllBalances(students, classes, payments)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val cashRegisterSummary: StateFlow<CashRegisterSummary> = combine(
        paymentsState,
        expensesState
    ) { pay, exp ->
        val payments = (pay as? UiState.Success)?.data ?: emptyList()
        val expenses = (exp as? UiState.Success)?.data ?: emptyList()
        FinanceCalculator.computeCashRegister(payments, expenses)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), CashRegisterSummary(0, 0, 0, 0, emptyMap()))

    val missingGradeAlerts: StateFlow<List<MissingGradeAlert>> = combine(
        studentsState,
        assessmentsState,
        gradesState
    ) { st, asmt, gr ->
        AcademicCalculator.detectMissingGrades(
            (st as? UiState.Success)?.data ?: emptyList(),
            (asmt as? UiState.Success)?.data ?: emptyList(),
            (gr as? UiState.Success)?.data ?: emptyList()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val absenceAlerts: StateFlow<List<AbsenceAlert>> = combine(
        studentsState,
        attendanceState
    ) { st, att ->
        AcademicCalculator.detectRepeatedAbsences(
            (st as? UiState.Success)?.data ?: emptyList(),
            (att as? UiState.Success)?.data ?: emptyList()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _feedback = MutableStateFlow<String?>(null)
    val feedback: StateFlow<String?> = _feedback.asStateFlow()

    fun clearFeedback() {
        _feedback.value = null
    }

    private fun activeSchoolOrNotify(): School? {
        val school = (schoolState.value as? UiState.Success)?.data
        if (school == null) {
            _feedback.value = "Établissement non chargé."
            return null
        }
        return school
    }

    private fun canWrite(school: School): Boolean {
        if (school.subscriptionStatus == "EXPIRED" || school.subscriptionStatus == "SUSPENDED") {
            _feedback.value = "Mode consultation & exportation uniquement (${school.subscriptionStatus})."
            return false
        }
        return true
    }

    fun updateSchoolBranding(updated: School) {
        if (!canWrite(updated)) return
        viewModelScope.launch {
            platformRepo.updateSchoolBranding(updated)
                .onSuccess {
                    schoolRepo.createAuditLog(updated, "UPDATE_BRANDING", "School", updated.id, "Identité mise à jour")
                    _feedback.value = "Identité de l'établissement enregistrée."
                }
                .onFailure { _feedback.value = it.localizedMessage ?: "Erreur mise à jour identité" }
        }
    }

    fun addAcademicYear(name: String, startDate: String, endDate: String, termSystem: String, passingAvg: Double) {
        val school = activeSchoolOrNotify() ?: return
        if (!canWrite(school)) return
        viewModelScope.launch {
            schoolRepo.createAcademicYear(
                school,
                AcademicYear(name = name, startDate = startDate, endDate = endDate, status = "ACTIVE", termSystem = termSystem, passingAverage = passingAvg)
            ).onSuccess { id ->
                schoolRepo.setActiveAcademicYear(school, AcademicYear(id = id, name = name))
                _feedback.value = "Année scolaire $name créée."
            }.onFailure { _feedback.value = it.localizedMessage }
        }
    }

    fun addSchoolMember(fullName: String, role: String, email: String, phone: String, specialty: String, salaryFcfa: Long) {
        val school = activeSchoolOrNotify() ?: return
        if (!canWrite(school)) return
        viewModelScope.launch {
            schoolRepo.createSchoolMember(
                school,
                SchoolMember(fullName = fullName, role = role, email = email, phone = phone, subjectSpecialty = specialty, salaryFcfa = salaryFcfa)
            ).onSuccess { _feedback.value = "Membre enregistré : $fullName" }
                .onFailure { _feedback.value = it.localizedMessage }
        }
    }

    fun addClass(name: String, level: String, capacity: Long, tuitionFcfa: Long, regFeeFcfa: Long, mainTeacher: String) {
        val school = activeSchoolOrNotify() ?: return
        if (!canWrite(school)) return
        viewModelScope.launch {
            schoolRepo.createSchoolClass(
                school,
                SchoolClass(
                    name = name,
                    level = level,
                    academicYearId = school.activeAcademicYearId,
                    capacity = capacity.coerceAtLeast(1L),
                    tuitionFeeFcfa = tuitionFcfa.coerceAtLeast(0L),
                    registrationFeeFcfa = regFeeFcfa.coerceAtLeast(0L),
                    mainTeacherName = mainTeacher
                )
            ).onSuccess { _feedback.value = "Classe $name ajoutée." }
                .onFailure { _feedback.value = it.localizedMessage }
        }
    }

    fun addSubject(name: String, code: String, schoolClass: SchoolClass, coef: Double, weeklyHours: Long, teacher: String, syllabus: String) {
        val school = activeSchoolOrNotify() ?: return
        if (!canWrite(school)) return
        viewModelScope.launch {
            schoolRepo.createSubject(
                school,
                Subject(
                    name = name,
                    code = code.ifBlank { name.take(3).uppercase() },
                    classId = schoolClass.id,
                    className = schoolClass.name,
                    coefficient = coef.coerceIn(0.5, 20.0),
                    weeklyHours = weeklyHours.coerceIn(0, 40),
                    teacherName = teacher,
                    syllabusSummary = syllabus
                )
            ).onSuccess { _feedback.value = "Matière $name ajoutée." }
                .onFailure { _feedback.value = it.localizedMessage }
        }
    }

    fun addScheduleSlot(schoolClass: SchoolClass, subject: Subject, dayOfWeek: Long, startTime: String, endTime: String, room: String) {
        val school = activeSchoolOrNotify() ?: return
        if (!canWrite(school)) return
        viewModelScope.launch {
            schoolRepo.createScheduleSlot(
                school,
                ScheduleSlot(
                    classId = schoolClass.id,
                    className = schoolClass.name,
                    subjectId = subject.id,
                    subjectName = subject.name,
                    dayOfWeek = dayOfWeek,
                    startTime = startTime,
                    endTime = endTime,
                    teacherName = subject.teacherName,
                    room = room
                )
            ).onSuccess { _feedback.value = "Créneau emploi du temps ajouté." }
                .onFailure { _feedback.value = it.localizedMessage }
        }
    }

    fun enrollStudent(
        firstName: String,
        lastName: String,
        gender: String,
        schoolClass: SchoolClass,
        birthDate: String,
        birthPlace: String,
        parentName: String,
        parentPhone: String,
        parentEmail: String,
        scholarshipFcfa: Long
    ) {
        val school = activeSchoolOrNotify() ?: return
        if (!canWrite(school)) return
        val existingMatricules = (studentsState.value as? UiState.Success)?.data?.map { it.matricule } ?: emptyList()
        val matricule = MatriculeGenerator.generateNextMatricule(school.code, existingMatricules)
        viewModelScope.launch {
            schoolRepo.createStudent(
                school,
                Student(
                    matricule = matricule,
                    firstName = firstName,
                    lastName = lastName,
                    gender = gender,
                    classId = schoolClass.id,
                    className = schoolClass.name,
                    academicYearId = school.activeAcademicYearId,
                    scholarshipDiscountFcfa = scholarshipFcfa.coerceAtLeast(0L),
                    enrollmentStatus = "ENROLLED",
                    birthDate = birthDate,
                    birthPlace = birthPlace,
                    parentName = parentName,
                    parentPhone = parentPhone,
                    parentEmail = parentEmail
                )
            ).onSuccess { id ->
                schoolRepo.createAutomationLog(
                    school,
                    AutomationLog(jobType = "MATRICULE_GEN", summary = "Matricule $matricule généré pour $firstName $lastName", affectedCount = 1L, status = "SUCCESS", executedBy = currentUserEmail)
                )
                schoolRepo.createAuditLog(school, "ENROLL_STUDENT", "Student", id, "Inscription $matricule")
                _feedback.value = "Élève inscrit avec matricule $matricule"
            }.onFailure { _feedback.value = it.localizedMessage }
        }
    }

    fun addAssessment(title: String, type: String, term: String, schoolClass: SchoolClass, subject: Subject) {
        val school = activeSchoolOrNotify() ?: return
        if (!canWrite(school)) return
        viewModelScope.launch {
            schoolRepo.createAssessment(
                school,
                Assessment(
                    title = title,
                    type = type,
                    term = term,
                    classId = schoolClass.id,
                    subjectId = subject.id,
                    subjectName = subject.name,
                    maxScore = 20.0,
                    dateMillis = System.currentTimeMillis()
                )
            ).onSuccess { _feedback.value = "Évaluation créée : $title" }
                .onFailure { _feedback.value = it.localizedMessage }
        }
    }

    fun recordGrade(student: Student, assessment: Assessment, subject: Subject, score: Double, remark: String, offlineMode: Boolean) {
        val school = activeSchoolOrNotify() ?: return
        if (!canWrite(school)) return
        val safeScore = score.coerceIn(0.0, 20.0)
        viewModelScope.launch {
            if (offlineMode) {
                syncManager.enqueueOperation(
                    school = school,
                    collectionName = "grades",
                    documentId = "",
                    opType = "CREATE",
                    fields = mapOf(
                        "studentId" to student.id,
                        "studentName" to student.fullName,
                        "studentMatricule" to student.matricule,
                        "classId" to student.classId,
                        "subjectId" to subject.id,
                        "subjectName" to subject.name,
                        "coefficient" to subject.coefficient,
                        "assessmentId" to assessment.id,
                        "assessmentTitle" to assessment.title,
                        "term" to assessment.term,
                        "score" to safeScore,
                        "teacherRemark" to remark
                    ),
                    summaryLabel = "Note ${student.fullName}: $safeScore/20 (${subject.name})"
                )
                _feedback.value = "Note enregistrée hors connexion (en attente de synchro)"
            } else {
                schoolRepo.createGrade(
                    school,
                    GradeRecord(
                        studentId = student.id,
                        studentName = student.fullName,
                        studentMatricule = student.matricule,
                        classId = student.classId,
                        subjectId = subject.id,
                        subjectName = subject.name,
                        coefficient = subject.coefficient,
                        assessmentId = assessment.id,
                        assessmentTitle = assessment.title,
                        term = assessment.term,
                        score = safeScore,
                        teacherRemark = remark
                    )
                ).onSuccess { _feedback.value = "Note enregistrée : ${student.fullName} ($safeScore/20)" }
                    .onFailure { _feedback.value = it.localizedMessage }
            }
        }
    }

    fun recordAttendance(student: Student, dateIso: String, status: String, justified: Boolean, minutesLate: Long, note: String, offlineMode: Boolean) {
        val school = activeSchoolOrNotify() ?: return
        if (!canWrite(school)) return
        viewModelScope.launch {
            if (offlineMode) {
                syncManager.enqueueOperation(
                    school = school,
                    collectionName = "attendance",
                    documentId = "",
                    opType = "CREATE",
                    fields = mapOf(
                        "studentId" to student.id,
                        "studentName" to student.fullName,
                        "classId" to student.classId,
                        "className" to student.className,
                        "dateIso" to dateIso,
                        "status" to status,
                        "justified" to justified,
                        "minutesLate" to minutesLate.coerceIn(0L, 300L),
                        "disciplineNote" to note
                    ),
                    summaryLabel = "Présence ${student.fullName} ($status - $dateIso)"
                )
                _feedback.value = "Présence stockée hors connexion"
            } else {
                schoolRepo.createAttendance(
                    school,
                    AttendanceRecord(
                        studentId = student.id,
                        studentName = student.fullName,
                        classId = student.classId,
                        className = student.className,
                        dateIso = dateIso,
                        status = status,
                        justified = justified,
                        minutesLate = minutesLate.coerceIn(0L, 300L),
                        disciplineNote = note
                    )
                ).onSuccess { _feedback.value = "Présence enregistrée ($status)" }
                    .onFailure { _feedback.value = it.localizedMessage }
            }
        }
    }

    fun recordPayment(student: Student, feeCategory: String, amountFcfa: Long, method: String, ref: String, note: String, offlineMode: Boolean) {
        val school = activeSchoolOrNotify() ?: return
        if (!canWrite(school)) return
        if (amountFcfa <= 0L) {
            _feedback.value = "Le montant doit être supérieur à 0 FCFA."
            return
        }
        val existingReceipts = (paymentsState.value as? UiState.Success)?.data?.map { it.receiptNumber } ?: emptyList()
        val receiptNumber = MatriculeGenerator.generateNextReceiptNumber(existingReceipts)
        val paidAt = System.currentTimeMillis()
        viewModelScope.launch {
            if (offlineMode) {
                syncManager.enqueueOperation(
                    school = school,
                    collectionName = "payments",
                    documentId = "",
                    opType = "CREATE",
                    fields = mapOf(
                        "receiptNumber" to receiptNumber,
                        "studentId" to student.id,
                        "studentName" to student.fullName,
                        "studentMatricule" to student.matricule,
                        "classId" to student.classId,
                        "className" to student.className,
                        "feeCategory" to feeCategory,
                        "amountFcfa" to amountFcfa,
                        "method" to method,
                        "paidAtMillis" to paidAt,
                        "status" to "COMPLETED",
                        "referenceNumber" to ref,
                        "note" to note
                    ),
                    summaryLabel = "Paiement $receiptNumber: ${FinanceCalculator.formatFcfa(amountFcfa)}"
                )
                _feedback.value = "Paiement $receiptNumber mis en file hors connexion"
            } else {
                schoolRepo.createPayment(
                    school,
                    PaymentRecord(
                        receiptNumber = receiptNumber,
                        studentId = student.id,
                        studentName = student.fullName,
                        studentMatricule = student.matricule,
                        classId = student.classId,
                        className = student.className,
                        feeCategory = feeCategory,
                        amountFcfa = amountFcfa,
                        method = method,
                        paidAtMillis = paidAt,
                        status = "COMPLETED",
                        referenceNumber = ref,
                        note = note
                    )
                ).onSuccess { id ->
                    schoolRepo.createAuditLog(school, "PAYMENT_RECORDED", "Payment", id, "$receiptNumber - $amountFcfa FCFA ($method)")
                    _feedback.value = "Paiement encaissé : $receiptNumber"
                }.onFailure { _feedback.value = it.localizedMessage }
            }
        }
    }

    fun refundPayment(payment: PaymentRecord, reason: String) {
        val school = activeSchoolOrNotify() ?: return
        if (!canWrite(school)) return
        viewModelScope.launch {
            schoolRepo.refundPayment(school.id, payment.id, reason.ifBlank { "Remboursement autorisé" })
                .onSuccess {
                    schoolRepo.createAuditLog(school, "PAYMENT_REFUNDED", "Payment", payment.id, "${payment.receiptNumber}: $reason")
                    _feedback.value = "Paiement ${payment.receiptNumber} remboursé."
                }
                .onFailure { _feedback.value = it.localizedMessage }
        }
    }

    fun recordExpense(title: String, category: String, amountFcfa: Long, method: String, beneficiary: String, note: String) {
        val school = activeSchoolOrNotify() ?: return
        if (!canWrite(school)) return
        if (amountFcfa <= 0L) {
            _feedback.value = "Montant invalide."
            return
        }
        viewModelScope.launch {
            schoolRepo.createExpense(
                school,
                ExpenseRecord(
                    title = title,
                    category = category,
                    amountFcfa = amountFcfa,
                    method = method,
                    expenseDateMillis = System.currentTimeMillis(),
                    beneficiary = beneficiary,
                    note = note
                )
            ).onSuccess { id ->
                schoolRepo.createAuditLog(school, "EXPENSE_RECORDED", "Expense", id, "$title - $amountFcfa FCFA")
                _feedback.value = "Dépense enregistrée."
            }.onFailure { _feedback.value = it.localizedMessage }
        }
    }

    fun sendNotification(title: String, message: String, audience: String, priority: String) {
        val school = activeSchoolOrNotify() ?: return
        if (!canWrite(school)) return
        viewModelScope.launch {
            schoolRepo.createNotification(
                school,
                SchoolNotification(
                    title = title,
                    message = message,
                    audience = audience,
                    senderName = school.directorName.ifBlank { currentUserEmail },
                    priority = priority
                )
            ).onSuccess { _feedback.value = "Notification publiée." }
                .onFailure { _feedback.value = it.localizedMessage }
        }
    }

    fun runCouncilDeliberation(schoolClass: SchoolClass, term: String) {
        val school = activeSchoolOrNotify() ?: return
        if (!canWrite(school)) return
        val students = ((studentsState.value as? UiState.Success)?.data ?: emptyList()).filter { it.classId == schoolClass.id }
        val subjects = ((subjectsState.value as? UiState.Success)?.data ?: emptyList()).filter { it.classId == schoolClass.id }
        val grades = ((gradesState.value as? UiState.Success)?.data ?: emptyList()).filter { it.classId == schoolClass.id }
        val summaries = AcademicCalculator.computeClassBulletinSummaries(students, subjects, grades, term)
        viewModelScope.launch {
            summaries.forEach { summary ->
                schoolRepo.createCouncilDecision(
                    school,
                    CouncilDecision(
                        academicYearId = school.activeAcademicYearId,
                        classId = schoolClass.id,
                        className = schoolClass.name,
                        term = term,
                        studentId = summary.student.id,
                        studentName = summary.student.fullName,
                        average = summary.overallAverage,
                        rank = summary.rank.toLong(),
                        mention = summary.mention,
                        decision = summary.suggestedDecision,
                        councilRemark = "Calculé automatiquement par le conseil de classe"
                    )
                )
            }
            schoolRepo.createAutomationLog(
                school,
                AutomationLog(jobType = "AVERAGE_CALC", summary = "Conseil de classe ${schoolClass.name} ($term) : ${summaries.size} moyennes & classements", affectedCount = summaries.size.toLong(), status = "SUCCESS", executedBy = currentUserEmail)
            )
            _feedback.value = "Conseil de classe calculé (${summaries.size} élèves)."
        }
    }

    fun runAllAutomations() {
        val school = activeSchoolOrNotify() ?: return
        if (!canWrite(school)) return
        val missing = missingGradeAlerts.value.size
        val absences = absenceAlerts.value.size
        val unpaid = studentBalances.value.count { it.remainingBalanceFcfa > 0L }
        viewModelScope.launch {
            schoolRepo.createAutomationLog(
                school,
                AutomationLog(jobType = "ABSENCE_ALERT", summary = "Détection absences répétées : $absences élève(s) signalé(s)", affectedCount = absences.toLong(), status = if (absences > 0) "WARNING" else "SUCCESS", executedBy = currentUserEmail)
            )
            schoolRepo.createAutomationLog(
                school,
                AutomationLog(jobType = "PAYMENT_REMINDER", summary = "Contrôle des impayés : $unpaid dossier(s) avec solde restant", affectedCount = unpaid.toLong(), status = if (unpaid > 0) "WARNING" else "SUCCESS", executedBy = currentUserEmail)
            )
            schoolRepo.createAutomationLog(
                school,
                AutomationLog(jobType = "MISSING_GRADES", summary = "Audit des notes manquantes : $missing note(s) attendue(s)", affectedCount = missing.toLong(), status = if (missing > 0) "WARNING" else "SUCCESS", executedBy = currentUserEmail)
            )
            _feedback.value = "Automatisations exécutées : $missing notes manquantes, $absences alertes absences, $unpaid rappels paiement."
        }
    }

    fun syncOfflineQueue() {
        val school = activeSchoolOrNotify() ?: return
        viewModelScope.launch {
            syncManager.syncPendingOperations(schoolId)
                .onSuccess { report ->
                    if (report.syncedCount > 0 || report.conflictCount > 0) {
                        schoolRepo.createAutomationLog(
                            school,
                            AutomationLog(
                                jobType = "BACKUP_SYNC",
                                summary = "Synchro hors connexion : ${report.syncedCount} synchronisée(s), ${report.conflictCount} conflit(s), ${report.failedCount} échec(s)",
                                affectedCount = report.syncedCount.toLong(),
                                status = if (report.conflictCount > 0) "WARNING" else "SUCCESS",
                                executedBy = currentUserEmail
                            )
                        )
                    }
                    _feedback.value = "Sync : ${report.syncedCount} envoyées, ${report.conflictCount} conflits détectés"
                }
                .onFailure { _feedback.value = it.localizedMessage ?: "Échec synchronisation" }
        }
    }

    fun resolveConflictKeepServer(conflict: SyncConflictEntity) {
        viewModelScope.launch {
            syncManager.resolveConflictKeepServer(conflict)
            _feedback.value = "Conflit résolu (version serveur conservée)."
        }
    }

    fun resolveConflictForceLocal(conflict: SyncConflictEntity) {
        viewModelScope.launch {
            syncManager.resolveConflictForceLocal(conflict)
                .onSuccess { _feedback.value = "Conflit résolu (version locale appliquée)." }
                .onFailure { _feedback.value = it.localizedMessage }
        }
    }

    fun buildExportJson(): String {
        val school = (schoolState.value as? UiState.Success)?.data
        val students = (studentsState.value as? UiState.Success)?.data ?: emptyList()
        val classes = (classesState.value as? UiState.Success)?.data ?: emptyList()
        val payments = (paymentsState.value as? UiState.Success)?.data ?: emptyList()
        return JSONObject().apply {
            put("schoolName", school?.name ?: "")
            put("schoolCode", school?.code ?: "")
            put("exportedAtMillis", System.currentTimeMillis())
            put("classesCount", classes.size)
            put("students", JSONArray(students.map { s ->
                JSONObject().apply {
                    put("matricule", s.matricule)
                    put("fullName", s.fullName)
                    put("className", s.className)
                    put("parentPhone", s.parentPhone)
                }
            }))
            put("payments", JSONArray(payments.map { p ->
                JSONObject().apply {
                    put("receiptNumber", p.receiptNumber)
                    put("studentName", p.studentName)
                    put("amountFcfa", p.amountFcfa)
                    put("method", p.method)
                    put("status", p.status)
                }
            }))
        }.toString(2)
    }

    private fun <T> Flow<T>.asUiState(tag: String): StateFlow<UiState<T>> =
        map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w("SchoolVM-$tag", "Stream error", rootError)
                emit(UiState.Error(rootError.message ?: "Erreur"))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private fun <T> Flow<List<T>>.asListUiState(tag: String): StateFlow<UiState<List<T>>> =
        asUiState(tag)
}
