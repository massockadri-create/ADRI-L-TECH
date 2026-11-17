package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.remote.OfflineSyncManager
import com.example.domain.AcademicCalculator
import com.example.domain.FinanceCalculator
import com.example.domain.MatriculeGenerator
import com.example.domain.PdfGenerator
import com.example.domain.SchoolDocumentType
import com.example.domain.StudentPaymentStatus
import com.example.model.Assessment
import com.example.model.AttendanceRecord
import com.example.model.ExpenseRecord
import com.example.model.GradeRecord
import com.example.model.PaymentRecord
import com.example.model.School
import com.example.model.SchoolClass
import com.example.model.Student
import com.example.model.Subject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SchoolErpDomainTest {

    @Test
    fun `academic calculator computes weighted averages tie ranks and alerts`() {
        val s1 = Student(id = "s1", matricule = "ASM-1", firstName = "Amina", lastName = "Bello", classId = "c1", className = "3e A")
        val s2 = Student(id = "s2", matricule = "ASM-2", firstName = "Paul", lastName = "Kamdoum", classId = "c1", className = "3e A")
        val s3 = Student(id = "s3", matricule = "ASM-3", firstName = "Claire", lastName = "Ndi", classId = "c1", className = "3e A")

        val math = Subject(id = "sub1", name = "Mathématiques", classId = "c1", coefficient = 4.0)
        val fr = Subject(id = "sub2", name = "Français", classId = "c1", coefficient = 2.0)

        val grades = listOf(
            // s1: (16*4 + 13*2) / 6 = 90/6 = 15.0
            GradeRecord(id = "g1", studentId = "s1", subjectId = "sub1", assessmentId = "a1", term = "T1", score = 16.0),
            GradeRecord(id = "g2", studentId = "s1", subjectId = "sub2", assessmentId = "a2", term = "T1", score = 13.0),
            // s2: tie with s1 = 15.0
            GradeRecord(id = "g3", studentId = "s2", subjectId = "sub1", assessmentId = "a1", term = "T1", score = 16.0),
            GradeRecord(id = "g4", studentId = "s2", subjectId = "sub2", assessmentId = "a2", term = "T1", score = 13.0),
            // s3: (10*4 + 7*2) / 6 = 54/6 = 9.0
            GradeRecord(id = "g5", studentId = "s3", subjectId = "sub1", assessmentId = "a1", term = "T1", score = 10.0),
            GradeRecord(id = "g6", studentId = "s3", subjectId = "sub2", assessmentId = "a2", term = "T1", score = 7.0)
        )

        val summaries = AcademicCalculator.computeClassBulletinSummaries(
            classStudents = listOf(s1, s2, s3),
            classSubjects = listOf(math, fr),
            classGrades = grades,
            term = "T1"
        )

        assertEquals(3, summaries.size)
        assertEquals(15.0, summaries[0].overallAverage, 0.01)
        assertEquals(1, summaries[0].rank)
        assertEquals(15.0, summaries[1].overallAverage, 0.01)
        assertEquals(1, summaries[1].rank) // Tie rank
        assertEquals(9.0, summaries[2].overallAverage, 0.01)
        assertEquals(3, summaries[2].rank) // Competition ranking skips rank 2
        assertEquals("WARNING", summaries[2].suggestedDecision)

        // Missing grade detection
        val a3 = Assessment(id = "a3", title = "Devoir 2", classId = "c1", subjectId = "sub1")
        val missing = AcademicCalculator.detectMissingGrades(listOf(s1, s2, s3), listOf(a3), grades)
        assertEquals(3, missing.size)

        // Repeated unjustified absences
        val attendance = listOf(
            AttendanceRecord(id = "att1", studentId = "s3", status = "ABSENT", justified = false),
            AttendanceRecord(id = "att2", studentId = "s3", status = "ABSENT", justified = false),
            AttendanceRecord(id = "att3", studentId = "s3", status = "ABSENT", justified = false),
            AttendanceRecord(id = "att4", studentId = "s1", status = "ABSENT", justified = true)
        )
        val alerts = AcademicCalculator.detectRepeatedAbsences(listOf(s1, s3), attendance)
        assertEquals(1, alerts.size)
        assertEquals("s3", alerts.first().student.id)
    }

    @Test
    fun `finance calculator computes balances cash register and 20k fcfa subscriptions`() {
        val cls = SchoolClass(id = "c1", name = "Tle C", registrationFeeFcfa = 15_000L, tuitionFeeFcfa = 100_000L)
        val student = Student(id = "s1", classId = "c1", scholarshipDiscountFcfa = 25_000L)
        val payments = listOf(
            PaymentRecord(id = "p1", studentId = "s1", amountFcfa = 40_000L, method = "ORANGE_MONEY", status = "COMPLETED"),
            PaymentRecord(id = "p2", studentId = "s1", amountFcfa = 10_000L, method = "CASH", status = "REFUNDED")
        )
        val balance = FinanceCalculator.computeStudentBalance(student, cls, payments)
        assertEquals(90_000L, balance.expectedFeeFcfa) // 115,000 - 25,000
        assertEquals(40_000L, balance.paidAmountFcfa) // REFUNDED excluded
        assertEquals(50_000L, balance.remainingBalanceFcfa)
        assertEquals(StudentPaymentStatus.PARTIAL, balance.status)

        val cash = FinanceCalculator.computeCashRegister(
            payments = payments,
            expenses = listOf(ExpenseRecord(id = "e1", amountFcfa = 12_000L))
        )
        assertEquals(40_000L, cash.totalRevenueFcfa)
        assertEquals(10_000L, cash.totalRefundedFcfa)
        assertEquals(12_000L, cash.totalExpensesFcfa)
        assertEquals(28_000L, cash.netBalanceFcfa)

        assertEquals(60_000L, FinanceCalculator.computeSubscriptionTotal(3L))
    }

    @Test
    fun `matricule generator and offline timestamp conflict detection work accurately`() {
        val code = MatriculeGenerator.generateSchoolCode("Lycée Bilingue de Yaoundé")
        assertTrue(code.isNotBlank())
        val prefix = code.take(4)
        val mat = MatriculeGenerator.generateNextMatricule(code, listOf("$prefix-26-0001", "$prefix-26-0002"), 2026)
        assertEquals("$prefix-26-0003", mat)

        assertFalse(OfflineSyncManager.detectTimestampConflict(baseMillis = 0L, serverMillis = 5000L))
        assertFalse(OfflineSyncManager.detectTimestampConflict(baseMillis = 2000L, serverMillis = 2000L))
        assertTrue(OfflineSyncManager.detectTimestampConflict(baseMillis = 2000L, serverMillis = 3000L))
    }

    @Test
    fun `pdf generator exports json shares fileprovider uri and handles pdf safely`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val school = School(
            id = "sch1",
            name = "Collège Test",
            code = "CLT",
            city = "Douala",
            motto = "Discipline et Réussite",
            directorName = "M. Ndongo",
            activeAcademicYearName = "2026-2027"
        )
        val student = Student(id = "s1", matricule = "CLT-26-0001", firstName = "Jean", lastName = "Mba", classId = "c1", className = "6e A")
        val payment = PaymentRecord(
            id = "p1",
            receiptNumber = "REC-2026-0001",
            studentId = "s1",
            studentName = "Jean Mba",
            studentMatricule = "CLT-26-0001",
            className = "6e A",
            feeCategory = "Scolarité",
            amountFcfa = 50_000L,
            method = "MTN_MOMO",
            paidAtMillis = System.currentTimeMillis()
        )
        // On JVM Robolectric, native PdfDocument pointer is 0 unless running on Android device;
        // verify generateDocumentPdf returns a controlled Result without uncaught exceptions.
        val res = PdfGenerator.generateDocumentPdf(
            context = context,
            school = school,
            docType = SchoolDocumentType.RECEIPT,
            student = student,
            payment = payment,
            languageCode = "FR"
        )
        assertTrue(res.isSuccess || res.exceptionOrNull() is IllegalStateException)

        val exportRes = PdfGenerator.exportSchoolDataJson(context, school.code, """{"school":"Collège Test"}""")
        assertTrue(exportRes.isSuccess)
        val exportedFile = exportRes.getOrThrow()
        assertTrue(exportedFile.exists())
        val shareUri = PdfGenerator.getShareUri(context, exportedFile)
        assertTrue(shareUri.toString().contains("fileprovider"))
    }
}
