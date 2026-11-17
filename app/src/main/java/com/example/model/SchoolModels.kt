package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class PlatformConfig(
    @DocumentId val id: String = "global",
    val appVersion: String = "2.1.0",
    val minSupportedVersion: String = "2.0.0",
    val maintenanceMode: Boolean = false,
    val standardMonthlyFeeFcfa: Long = 20000L,
    val supportEmail: String = "support@adrieltech.cm",
    val supportPhone: String = "+237 600 000 000",
    val releaseNotes: String = "ADRIÈL SCHOOL MANAGER V2.1",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PlatformAdmin(
    @DocumentId val id: String = "",
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: String = "SUPPORT_ADMIN",
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class School(
    @DocumentId val id: String = "",
    val ownerUid: String = "",
    val memberUids: List<String> = emptyList(),
    val name: String = "",
    val code: String = "",
    val schoolType: String = "",
    val city: String = "",
    val country: String = "Cameroun",
    val subscriptionStatus: String = "TRIAL",
    val subscriptionEndMillis: Long = 0L,
    val monthlyFeeFcfa: Long = 20000L,
    val supportAccessEnabled: Boolean = false,
    val motto: String = "",
    val address: String = "",
    val phone: String = "",
    val email: String = "",
    val directorName: String = "",
    val registrationNumber: String = "",
    val primaryColorHex: String = "#0B1528",
    val secondaryColorHex: String = "#1E40AF",
    val accentColorHex: String = "#0D9488",
    val logoPrimaryBase64: String = "",
    val logoSecondaryBase64: String = "",
    val stampBase64: String = "",
    val signatureBase64: String = "",
    val activeAcademicYearId: String = "",
    val activeAcademicYearName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Subscription(
    @DocumentId val id: String = "",
    val schoolId: String = "",
    val schoolName: String = "",
    val ownerUid: String = "",
    val planName: String = "Standard SaaS",
    val monthlyFeeFcfa: Long = 20000L,
    val monthsCount: Long = 1L,
    val totalAmountFcfa: Long = 20000L,
    val paymentMethod: String = "ORANGE_MONEY",
    val referenceCode: String = "",
    val proofNote: String = "",
    val status: String = "PENDING",
    val startDateMillis: Long = 0L,
    val endDateMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SupportTicket(
    @DocumentId val id: String = "",
    val schoolId: String = "",
    val schoolName: String = "",
    val requesterUid: String = "",
    val requesterName: String = "",
    val subject: String = "",
    val description: String = "",
    val priority: String = "MEDIUM",
    val status: String = "OPEN",
    val resolutionNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class InterventionLog(
    @DocumentId val id: String = "",
    val schoolId: String = "",
    val schoolName: String = "",
    val adminUid: String = "",
    val adminEmail: String = "",
    val reason: String = "",
    val actionSummary: String = "",
    val authorizedByOwnerUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AcademicYear(
    @DocumentId val id: String = "",
    val schoolId: String = "",
    val ownerUid: String = "",
    val memberUids: List<String> = emptyList(),
    val name: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val status: String = "ACTIVE",
    val termSystem: String = "TRIMESTER",
    val passingAverage: Double = 10.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SchoolMember(
    @DocumentId val id: String = "",
    val schoolId: String = "",
    val ownerUid: String = "",
    val memberUids: List<String> = emptyList(),
    val fullName: String = "",
    val role: String = "TEACHER",
    val active: Boolean = true,
    val salaryFcfa: Long = 0L,
    val linkedStudentIds: List<String> = emptyList(),
    val email: String = "",
    val phone: String = "",
    val subjectSpecialty: String = "",
    val linkedUserUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SchoolClass(
    @DocumentId val id: String = "",
    val schoolId: String = "",
    val ownerUid: String = "",
    val memberUids: List<String> = emptyList(),
    val name: String = "",
    val level: String = "",
    val academicYearId: String = "",
    val capacity: Long = 50L,
    val tuitionFeeFcfa: Long = 0L,
    val registrationFeeFcfa: Long = 0L,
    val mainTeacherName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Subject(
    @DocumentId val id: String = "",
    val schoolId: String = "",
    val ownerUid: String = "",
    val memberUids: List<String> = emptyList(),
    val name: String = "",
    val code: String = "",
    val classId: String = "",
    val className: String = "",
    val coefficient: Double = 1.0,
    val weeklyHours: Long = 2L,
    val teacherName: String = "",
    val syllabusSummary: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ScheduleSlot(
    @DocumentId val id: String = "",
    val schoolId: String = "",
    val ownerUid: String = "",
    val memberUids: List<String> = emptyList(),
    val classId: String = "",
    val className: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val dayOfWeek: Long = 1L,
    val startTime: String = "",
    val endTime: String = "",
    val teacherName: String = "",
    val room: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Student(
    @DocumentId val id: String = "",
    val schoolId: String = "",
    val ownerUid: String = "",
    val memberUids: List<String> = emptyList(),
    val matricule: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val gender: String = "M",
    val classId: String = "",
    val className: String = "",
    val academicYearId: String = "",
    val scholarshipDiscountFcfa: Long = 0L,
    val enrollmentStatus: String = "ENROLLED",
    val birthDate: String = "",
    val birthPlace: String = "",
    val parentName: String = "",
    val parentPhone: String = "",
    val parentEmail: String = "",
    val parentUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val fullName: String get() = "$firstName $lastName".trim()
}

@Keep
@IgnoreExtraProperties
data class Assessment(
    @DocumentId val id: String = "",
    val schoolId: String = "",
    val ownerUid: String = "",
    val memberUids: List<String> = emptyList(),
    val title: String = "",
    val type: String = "DEVOIR",
    val term: String = "T1",
    val classId: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val maxScore: Double = 20.0,
    val dateMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class GradeRecord(
    @DocumentId val id: String = "",
    val schoolId: String = "",
    val ownerUid: String = "",
    val memberUids: List<String> = emptyList(),
    val studentId: String = "",
    val studentName: String = "",
    val studentMatricule: String = "",
    val classId: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val coefficient: Double = 1.0,
    val assessmentId: String = "",
    val assessmentTitle: String = "",
    val term: String = "T1",
    val score: Double = 0.0,
    val teacherRemark: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AttendanceRecord(
    @DocumentId val id: String = "",
    val schoolId: String = "",
    val ownerUid: String = "",
    val memberUids: List<String> = emptyList(),
    val studentId: String = "",
    val studentName: String = "",
    val classId: String = "",
    val className: String = "",
    val dateIso: String = "",
    val status: String = "PRESENT",
    val justified: Boolean = false,
    val minutesLate: Long = 0L,
    val disciplineNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PaymentRecord(
    @DocumentId val id: String = "",
    val schoolId: String = "",
    val ownerUid: String = "",
    val memberUids: List<String> = emptyList(),
    val receiptNumber: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val studentMatricule: String = "",
    val classId: String = "",
    val className: String = "",
    val feeCategory: String = "TUITION",
    val amountFcfa: Long = 0L,
    val method: String = "CASH",
    val paidAtMillis: Long = 0L,
    val status: String = "COMPLETED",
    val referenceNumber: String = "",
    val note: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ExpenseRecord(
    @DocumentId val id: String = "",
    val schoolId: String = "",
    val ownerUid: String = "",
    val memberUids: List<String> = emptyList(),
    val title: String = "",
    val category: String = "SUPPLIES",
    val amountFcfa: Long = 0L,
    val method: String = "CASH",
    val expenseDateMillis: Long = 0L,
    val beneficiary: String = "",
    val note: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SchoolNotification(
    @DocumentId val id: String = "",
    val schoolId: String = "",
    val ownerUid: String = "",
    val memberUids: List<String> = emptyList(),
    val title: String = "",
    val message: String = "",
    val audience: String = "ALL",
    val senderName: String = "",
    val priority: String = "NORMAL",
    val targetClassId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CouncilDecision(
    @DocumentId val id: String = "",
    val schoolId: String = "",
    val ownerUid: String = "",
    val memberUids: List<String> = emptyList(),
    val academicYearId: String = "",
    val classId: String = "",
    val className: String = "",
    val term: String = "T1",
    val studentId: String = "",
    val studentName: String = "",
    val average: Double = 0.0,
    val rank: Long = 1L,
    val mention: String = "",
    val decision: String = "ADMITTED",
    val councilRemark: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AutomationLog(
    @DocumentId val id: String = "",
    val schoolId: String = "",
    val ownerUid: String = "",
    val memberUids: List<String> = emptyList(),
    val jobType: String = "",
    val summary: String = "",
    val affectedCount: Long = 0L,
    val status: String = "SUCCESS",
    val executedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AuditLog(
    @DocumentId val id: String = "",
    val schoolId: String = "",
    val ownerUid: String = "",
    val memberUids: List<String> = emptyList(),
    val actorUid: String = "",
    val actorEmail: String = "",
    val action: String = "",
    val entityType: String = "",
    val entityId: String = "",
    val details: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
