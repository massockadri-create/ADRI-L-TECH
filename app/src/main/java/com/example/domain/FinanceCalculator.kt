package com.example.domain

import com.example.model.ExpenseRecord
import com.example.model.PaymentRecord
import com.example.model.SchoolClass
import com.example.model.Student
import java.text.NumberFormat
import java.util.Locale

enum class StudentPaymentStatus {
    PAID,
    PARTIAL,
    UNPAID
}

data class StudentBalanceSummary(
    val student: Student,
    val expectedFeeFcfa: Long,
    val paidAmountFcfa: Long,
    val remainingBalanceFcfa: Long,
    val status: StudentPaymentStatus
)

data class CashRegisterSummary(
    val totalRevenueFcfa: Long,
    val totalRefundedFcfa: Long,
    val totalExpensesFcfa: Long,
    val netBalanceFcfa: Long,
    val revenueByMethod: Map<String, Long>
)

object FinanceCalculator {

    const val STANDARD_MONTHLY_SUBSCRIPTION_FCFA = 20_000L

    fun computeStudentBalance(
        student: Student,
        schoolClass: SchoolClass?,
        payments: List<PaymentRecord>
    ): StudentBalanceSummary {
        val registrationFee = schoolClass?.registrationFeeFcfa ?: 0L
        val tuitionFee = schoolClass?.tuitionFeeFcfa ?: 0L
        val grossExpected = (registrationFee + tuitionFee).coerceAtLeast(0L)
        val expected = (grossExpected - student.scholarshipDiscountFcfa).coerceAtLeast(0L)

        val paid = payments
            .filter { it.studentId == student.id && it.status == "COMPLETED" }
            .sumOf { it.amountFcfa }

        val remaining = (expected - paid).coerceAtLeast(0L)
        val status = when {
            expected == 0L && paid == 0L -> StudentPaymentStatus.PAID
            remaining == 0L -> StudentPaymentStatus.PAID
            paid > 0L -> StudentPaymentStatus.PARTIAL
            else -> StudentPaymentStatus.UNPAID
        }

        return StudentBalanceSummary(
            student = student,
            expectedFeeFcfa = expected,
            paidAmountFcfa = paid,
            remainingBalanceFcfa = remaining,
            status = status
        )
    }

    fun computeAllBalances(
        students: List<Student>,
        classes: List<SchoolClass>,
        payments: List<PaymentRecord>
    ): List<StudentBalanceSummary> {
        val classMap = classes.associateBy { it.id }
        return students.map { student ->
            computeStudentBalance(student, classMap[student.classId], payments)
        }
    }

    fun computeCashRegister(
        payments: List<PaymentRecord>,
        expenses: List<ExpenseRecord>
    ): CashRegisterSummary {
        val completed = payments.filter { it.status == "COMPLETED" }
        val refunded = payments.filter { it.status == "REFUNDED" }
        val totalRevenue = completed.sumOf { it.amountFcfa }
        val totalRefunded = refunded.sumOf { it.amountFcfa }
        val totalExpenses = expenses.sumOf { it.amountFcfa }
        val byMethod = completed.groupBy { it.method }.mapValues { (_, list) -> list.sumOf { it.amountFcfa } }

        return CashRegisterSummary(
            totalRevenueFcfa = totalRevenue,
            totalRefundedFcfa = totalRefunded,
            totalExpensesFcfa = totalExpenses,
            netBalanceFcfa = totalRevenue - totalExpenses,
            revenueByMethod = byMethod
        )
    }

    fun computeSubscriptionTotal(monthsCount: Long, monthlyFeeFcfa: Long = STANDARD_MONTHLY_SUBSCRIPTION_FCFA): Long {
        return monthsCount.coerceAtLeast(1L) * monthlyFeeFcfa.coerceAtLeast(0L)
    }

    fun computeSubscriptionEndMillis(startMillis: Long, monthsCount: Long): Long {
        val daysPerMonth = 30L
        return startMillis + (monthsCount.coerceAtLeast(1L) * daysPerMonth * 24L * 60L * 60L * 1000L)
    }

    fun formatFcfa(amount: Long): String {
        val nf = NumberFormat.getNumberInstance(Locale.FRANCE)
        return "${nf.format(amount)} FCFA"
    }
}
