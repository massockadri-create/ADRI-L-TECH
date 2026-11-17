package com.example.domain

import com.example.model.Assessment
import com.example.model.AttendanceRecord
import com.example.model.GradeRecord
import com.example.model.Student
import com.example.model.Subject
import kotlin.math.roundToInt

data class SubjectAverageResult(
    val subjectId: String,
    val subjectName: String,
    val coefficient: Double,
    val averageScore: Double,
    val weightedPoints: Double,
    val gradeCount: Int
)

data class StudentBulletinSummary(
    val student: Student,
    val term: String,
    val subjectResults: List<SubjectAverageResult>,
    val overallAverage: Double,
    val totalCoefficients: Double,
    val totalWeightedPoints: Double,
    val rank: Int,
    val totalStudentsInClass: Int,
    val mention: String,
    val suggestedDecision: String,
    val missingSubjectNames: List<String>
)

data class MissingGradeAlert(
    val student: Student,
    val assessment: Assessment
)

data class AbsenceAlert(
    val student: Student,
    val unjustifiedAbsences: Int,
    val totalLates: Int
)

object AcademicCalculator {

    fun computeStudentSubjectAverages(
        studentId: String,
        term: String,
        subjects: List<Subject>,
        grades: List<GradeRecord>
    ): List<SubjectAverageResult> {
        val studentGrades = grades.filter { it.studentId == studentId && it.term == term }
        return subjects.mapNotNull { subject ->
            val subjectGrades = studentGrades.filter { it.subjectId == subject.id }
            if (subjectGrades.isEmpty()) {
                null
            } else {
                val avg = round2(subjectGrades.map { it.score }.average())
                SubjectAverageResult(
                    subjectId = subject.id,
                    subjectName = subject.name,
                    coefficient = subject.coefficient,
                    averageScore = avg,
                    weightedPoints = round2(avg * subject.coefficient),
                    gradeCount = subjectGrades.size
                )
            }
        }
    }

    fun computeClassBulletinSummaries(
        classStudents: List<Student>,
        classSubjects: List<Subject>,
        classGrades: List<GradeRecord>,
        term: String,
        passingAverage: Double = 10.0
    ): List<StudentBulletinSummary> {
        val rawList = classStudents.map { student ->
            val subjectResults = computeStudentSubjectAverages(
                studentId = student.id,
                term = term,
                subjects = classSubjects,
                grades = classGrades
            )
            val totalCoef = subjectResults.sumOf { it.coefficient }
            val totalPoints = subjectResults.sumOf { it.weightedPoints }
            val avg = if (totalCoef > 0.0) round2(totalPoints / totalCoef) else 0.0
            val gradedSubjectIds = subjectResults.map { it.subjectId }.toSet()
            val missingSubjects = classSubjects.filter { it.id !in gradedSubjectIds }.map { it.name }
            Triple(student, subjectResults, Triple(totalCoef, totalPoints, avg to missingSubjects))
        }

        // Sort descending by average to assign dense/standard competition ranks (ties share same rank)
        val sorted = rawList.sortedByDescending { it.third.third.first }
        var currentRank = 1
        var previousAvg: Double? = null

        return sorted.mapIndexed { index, (student, subjectResults, stats) ->
            val (totalCoef, totalPoints, avgAndMissing) = stats
            val (avg, missingSubjects) = avgAndMissing
            if (previousAvg != null && avg < previousAvg!!) {
                currentRank = index + 1
            }
            previousAvg = avg

            val mention = computeMention(avg)
            val decision = computeDecision(avg, passingAverage)
            StudentBulletinSummary(
                student = student,
                term = term,
                subjectResults = subjectResults,
                overallAverage = avg,
                totalCoefficients = round2(totalCoef),
                totalWeightedPoints = round2(totalPoints),
                rank = if (subjectResults.isEmpty()) classStudents.size.coerceAtLeast(1) else currentRank,
                totalStudentsInClass = classStudents.size,
                mention = mention,
                suggestedDecision = decision,
                missingSubjectNames = missingSubjects
            )
        }
    }

    fun detectMissingGrades(
        students: List<Student>,
        assessments: List<Assessment>,
        grades: List<GradeRecord>
    ): List<MissingGradeAlert> {
        val existingPairs = grades.map { it.studentId to it.assessmentId }.toSet()
        val alerts = mutableListOf<MissingGradeAlert>()
        for (assessment in assessments) {
            val classStudents = students.filter { it.classId == assessment.classId }
            for (student in classStudents) {
                if ((student.id to assessment.id) !in existingPairs) {
                    alerts.add(MissingGradeAlert(student, assessment))
                }
            }
        }
        return alerts
    }

    fun detectRepeatedAbsences(
        students: List<Student>,
        attendance: List<AttendanceRecord>,
        threshold: Int = 3
    ): List<AbsenceAlert> {
        return students.mapNotNull { student ->
            val studentAttendance = attendance.filter { it.studentId == student.id }
            val unjustifiedAbsences = studentAttendance.count { it.status == "ABSENT" && !it.justified }
            val totalLates = studentAttendance.count { it.status == "LATE" }
            if (unjustifiedAbsences >= threshold || totalLates >= threshold * 2) {
                AbsenceAlert(student, unjustifiedAbsences, totalLates)
            } else {
                null
            }
        }.sortedByDescending { it.unjustifiedAbsences }
    }

    fun computeMention(average: Double): String = when {
        average >= 16.0 -> "Très Bien"
        average >= 14.0 -> "Bien"
        average >= 12.0 -> "Assez Bien"
        average >= 10.0 -> "Passable"
        else -> "Insuffisant"
    }

    fun computeDecision(average: Double, passingAverage: Double = 10.0): String = when {
        average >= 14.0 -> "HONOR_ROLL"
        average >= passingAverage -> "ADMITTED"
        average >= passingAverage - 2.0 -> "WARNING"
        else -> "REPEAT"
    }

    fun round2(value: Double): Double = (value * 100.0).roundToInt() / 100.0
}
