package com.example.data.remote

import android.content.Context
import com.example.R
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
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class SchoolRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("User must be signed in with Google.")

    fun <T : Any> observeSubcollection(
        schoolId: String,
        subcollection: String,
        isSuperAdmin: Boolean,
        clazz: Class<T>
    ): Flow<List<T>> = flow {
        val uid = requireUserId()
        val path = "schools/$schoolId/$subcollection"
        val base = db.collection("schools").document(schoolId).collection(subcollection)
        val query = if (isSuperAdmin) base else base.whereArrayContains("memberUids", uid)
        emitAll(
            query.snapshots()
                .map { snap -> snap.toObjects(clazz, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    private fun baseScopeFields(school: School): MutableMap<String, Any> {
        val uid = requireUserId()
        val owner = school.ownerUid.ifBlank { uid }
        val members = (school.memberUids + owner + uid).distinct().take(50)
        return mutableMapOf(
            "schoolId" to school.id,
            "ownerUid" to owner,
            "memberUids" to members,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    suspend fun createAcademicYear(school: School, year: AcademicYear): Result<String> =
        createDoc(school.id, "academic_years") {
            baseScopeFields(school).apply {
                put("name", year.name.trim())
                put("startDate", year.startDate.trim())
                put("endDate", year.endDate.trim())
                put("status", year.status)
                put("termSystem", year.termSystem)
                put("passingAverage", year.passingAverage)
            }
        }

    suspend fun setActiveAcademicYear(school: School, year: AcademicYear): Result<Unit> {
        val path = "schools/${school.id}"
        return try {
            requireUserId()
            db.document("schools/${school.id}/academic_years/${year.id}").update(
                mapOf("status" to "ACTIVE", "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            db.document(path).update(
                mapOf(
                    "activeAcademicYearId" to year.id,
                    "activeAcademicYearName" to year.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun createSchoolMember(school: School, member: SchoolMember): Result<String> =
        createDoc(school.id, "school_members") {
            baseScopeFields(school).apply {
                put("fullName", member.fullName.trim())
                put("role", member.role)
                put("active", member.active)
                put("salaryFcfa", member.salaryFcfa)
                put("linkedStudentIds", member.linkedStudentIds.take(20))
                if (member.email.isNotBlank()) put("email", member.email.trim())
                if (member.phone.isNotBlank()) put("phone", member.phone.trim())
                if (member.subjectSpecialty.isNotBlank()) put("subjectSpecialty", member.subjectSpecialty.trim())
                if (member.linkedUserUid.isNotBlank()) put("linkedUserUid", member.linkedUserUid.trim())
            }
        }

    suspend fun createSchoolClass(school: School, item: SchoolClass): Result<String> =
        createDoc(school.id, "classes") {
            baseScopeFields(school).apply {
                put("name", item.name.trim())
                put("level", item.level.trim())
                put("academicYearId", item.academicYearId)
                put("capacity", item.capacity)
                put("tuitionFeeFcfa", item.tuitionFeeFcfa)
                put("registrationFeeFcfa", item.registrationFeeFcfa)
                if (item.mainTeacherName.isNotBlank()) put("mainTeacherName", item.mainTeacherName.trim())
            }
        }

    suspend fun createSubject(school: School, item: Subject): Result<String> =
        createDoc(school.id, "subjects") {
            baseScopeFields(school).apply {
                put("name", item.name.trim())
                put("code", item.code.trim().uppercase())
                put("classId", item.classId)
                put("className", item.className)
                put("coefficient", item.coefficient)
                put("weeklyHours", item.weeklyHours)
                if (item.teacherName.isNotBlank()) put("teacherName", item.teacherName.trim())
                if (item.syllabusSummary.isNotBlank()) put("syllabusSummary", item.syllabusSummary.trim())
            }
        }

    suspend fun createScheduleSlot(school: School, item: ScheduleSlot): Result<String> =
        createDoc(school.id, "schedules") {
            baseScopeFields(school).apply {
                put("classId", item.classId)
                put("className", item.className)
                put("subjectId", item.subjectId)
                put("subjectName", item.subjectName)
                put("dayOfWeek", item.dayOfWeek)
                put("startTime", item.startTime.trim())
                put("endTime", item.endTime.trim())
                if (item.teacherName.isNotBlank()) put("teacherName", item.teacherName.trim())
                if (item.room.isNotBlank()) put("room", item.room.trim())
            }
        }

    suspend fun createStudent(school: School, item: Student): Result<String> =
        createDoc(school.id, "students") {
            baseScopeFields(school).apply {
                put("matricule", item.matricule.trim())
                put("firstName", item.firstName.trim())
                put("lastName", item.lastName.trim())
                put("gender", item.gender)
                put("classId", item.classId)
                put("className", item.className)
                put("academicYearId", item.academicYearId)
                put("scholarshipDiscountFcfa", item.scholarshipDiscountFcfa)
                put("enrollmentStatus", item.enrollmentStatus)
                if (item.birthDate.isNotBlank()) put("birthDate", item.birthDate.trim())
                if (item.birthPlace.isNotBlank()) put("birthPlace", item.birthPlace.trim())
                if (item.parentName.isNotBlank()) put("parentName", item.parentName.trim())
                if (item.parentPhone.isNotBlank()) put("parentPhone", item.parentPhone.trim())
                if (item.parentEmail.isNotBlank()) put("parentEmail", item.parentEmail.trim())
                if (item.parentUid.isNotBlank()) put("parentUid", item.parentUid.trim())
            }
        }

    suspend fun reEnrollStudent(schoolId: String, studentId: String, newClassId: String, newClassName: String, yearId: String): Result<Unit> =
        updateDoc("schools/$schoolId/students/$studentId", mapOf(
            "classId" to newClassId,
            "className" to newClassName,
            "academicYearId" to yearId,
            "enrollmentStatus" to "RE_ENROLLED",
            "updatedAt" to FieldValue.serverTimestamp()
        ))

    suspend fun createAssessment(school: School, item: Assessment): Result<String> =
        createDoc(school.id, "assessments") {
            baseScopeFields(school).apply {
                put("title", item.title.trim())
                put("type", item.type)
                put("term", item.term)
                put("classId", item.classId)
                put("subjectId", item.subjectId)
                put("subjectName", item.subjectName)
                put("maxScore", item.maxScore)
                put("dateMillis", item.dateMillis)
            }
        }

    suspend fun createGrade(school: School, item: GradeRecord): Result<String> =
        createDoc(school.id, "grades") {
            baseScopeFields(school).apply {
                put("studentId", item.studentId)
                put("studentName", item.studentName)
                put("studentMatricule", item.studentMatricule)
                put("classId", item.classId)
                put("subjectId", item.subjectId)
                put("subjectName", item.subjectName)
                put("coefficient", item.coefficient)
                put("assessmentId", item.assessmentId)
                put("assessmentTitle", item.assessmentTitle)
                put("term", item.term)
                put("score", item.score)
                if (item.teacherRemark.isNotBlank()) put("teacherRemark", item.teacherRemark.trim())
            }
        }

    suspend fun createAttendance(school: School, item: AttendanceRecord): Result<String> =
        createDoc(school.id, "attendance") {
            baseScopeFields(school).apply {
                put("studentId", item.studentId)
                put("studentName", item.studentName)
                put("classId", item.classId)
                put("className", item.className)
                put("dateIso", item.dateIso)
                put("status", item.status)
                put("justified", item.justified)
                put("minutesLate", item.minutesLate)
                if (item.disciplineNote.isNotBlank()) put("disciplineNote", item.disciplineNote.trim())
            }
        }

    suspend fun createPayment(school: School, item: PaymentRecord): Result<String> =
        createDoc(school.id, "payments") {
            baseScopeFields(school).apply {
                put("receiptNumber", item.receiptNumber)
                put("studentId", item.studentId)
                put("studentName", item.studentName)
                put("studentMatricule", item.studentMatricule)
                put("classId", item.classId)
                put("className", item.className)
                put("feeCategory", item.feeCategory)
                put("amountFcfa", item.amountFcfa)
                put("method", item.method)
                put("paidAtMillis", item.paidAtMillis)
                put("status", "COMPLETED")
                if (item.referenceNumber.isNotBlank()) put("referenceNumber", item.referenceNumber.trim())
                if (item.note.isNotBlank()) put("note", item.note.trim())
            }
        }

    suspend fun refundPayment(schoolId: String, paymentId: String, reason: String): Result<Unit> =
        updateDoc("schools/$schoolId/payments/$paymentId", mapOf(
            "status" to "REFUNDED",
            "note" to reason.trim(),
            "updatedAt" to FieldValue.serverTimestamp()
        ))

    suspend fun createExpense(school: School, item: ExpenseRecord): Result<String> =
        createDoc(school.id, "expenses") {
            baseScopeFields(school).apply {
                put("title", item.title.trim())
                put("category", item.category)
                put("amountFcfa", item.amountFcfa)
                put("method", item.method)
                put("expenseDateMillis", item.expenseDateMillis)
                if (item.beneficiary.isNotBlank()) put("beneficiary", item.beneficiary.trim())
                if (item.note.isNotBlank()) put("note", item.note.trim())
            }
        }

    suspend fun createNotification(school: School, item: SchoolNotification): Result<String> =
        createDoc(school.id, "communications") {
            baseScopeFields(school).apply {
                put("title", item.title.trim())
                put("message", item.message.trim())
                put("audience", item.audience)
                put("senderName", item.senderName.ifBlank { "Direction" })
                put("priority", item.priority)
                if (item.targetClassId.isNotBlank()) put("targetClassId", item.targetClassId)
            }
        }

    suspend fun createCouncilDecision(school: School, item: CouncilDecision): Result<String> =
        createDoc(school.id, "council_decisions") {
            baseScopeFields(school).apply {
                put("academicYearId", item.academicYearId)
                put("classId", item.classId)
                put("className", item.className)
                put("term", item.term)
                put("studentId", item.studentId)
                put("studentName", item.studentName)
                put("average", item.average)
                put("rank", item.rank)
                put("mention", item.mention)
                put("decision", item.decision)
                if (item.councilRemark.isNotBlank()) put("councilRemark", item.councilRemark.trim())
            }
        }

    suspend fun createAutomationLog(school: School, item: AutomationLog): Result<String> =
        createDoc(school.id, "automation_logs") {
            baseScopeFields(school).apply {
                put("jobType", item.jobType)
                put("summary", item.summary)
                put("affectedCount", item.affectedCount)
                put("status", item.status)
                put("executedBy", item.executedBy.ifBlank { auth.currentUser?.email ?: "System" })
            }
        }

    suspend fun createAuditLog(
        school: School,
        action: String,
        entityType: String,
        entityId: String,
        details: String
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Unauthenticated"))
        val email = auth.currentUser?.email ?: "user"
        return createDoc(school.id, "audit_logs") {
            baseScopeFields(school).apply {
                put("actorUid", uid)
                put("actorEmail", email)
                put("action", action)
                put("entityType", entityType)
                put("entityId", entityId)
                put("details", details.take(500))
            }
        }
    }

    suspend fun deleteSubcollectionDoc(schoolId: String, subcollection: String, docId: String): Result<Unit> {
        val path = "schools/$schoolId/$subcollection/$docId"
        return try {
            requireUserId()
            db.document(path).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    private suspend fun createDoc(
        schoolId: String,
        subcollection: String,
        payloadBuilder: () -> Map<String, Any>
    ): Result<String> {
        var path = "schools/$schoolId/$subcollection"
        return try {
            requireUserId()
            val docRef = db.collection("schools").document(schoolId).collection(subcollection).document()
            path = docRef.path
            docRef.set(payloadBuilder()).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    private suspend fun updateDoc(path: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            requireUserId()
            db.document(path).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }
}
