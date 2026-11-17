package com.example.domain

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.model.PaymentRecord
import com.example.model.School
import com.example.model.SchoolClass
import com.example.model.Student
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SchoolDocumentType(val labelFr: String, val labelEn: String) {
    BULLETIN("Bulletin de notes", "Report Card"),
    RECEIPT("Reçu de paiement", "Payment Receipt"),
    INVOICE("Facture scolaire", "Tuition Invoice"),
    ENROLLMENT_CERT("Certificat de scolarité", "Enrollment Certificate"),
    ATTESTATION("Attestation de fréquentation", "Attendance Attestation"),
    STUDENT_CARD("Carte d'élève", "Student ID Card"),
    CLASS_ROSTER("Liste de classe", "Class Roster"),
    FINANCIAL_REPORT("Rapport financier", "Financial Report"),
    UNPAID_REPORT("État des impayés", "Unpaid Balances Report")
}

object PdfGenerator {

    private const val PAGE_WIDTH = 595 // A4 at 72 dpi
    private const val PAGE_HEIGHT = 842

    fun generateDocumentPdf(
        context: Context,
        school: School,
        docType: SchoolDocumentType,
        student: Student? = null,
        schoolClass: SchoolClass? = null,
        bulletin: StudentBulletinSummary? = null,
        payment: PaymentRecord? = null,
        studentBalance: StudentBalanceSummary? = null,
        classRoster: List<Student> = emptyList(),
        cashSummary: CashRegisterSummary? = null,
        unpaidList: List<StudentBalanceSummary> = emptyList(),
        languageCode: String = "FR"
    ): Result<File> {
        return try {
            val dir = File(context.cacheDir, "pdfs").apply { mkdirs() }
            val safeName = "${docType.name.lowercase()}_${System.currentTimeMillis()}.pdf"
            val outFile = File(dir, safeName)

            val pdf = PdfDocument()
            try {
                val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
                val page = pdf.startPage(pageInfo)
                val canvas = page.canvas

                val primaryColor = parseHexColor(school.primaryColorHex, Color.rgb(11, 21, 40))
                val secondaryColor = parseHexColor(school.secondaryColorHex, Color.rgb(30, 64, 175))
                val accentColor = parseHexColor(school.accentColorHex, Color.rgb(13, 148, 136))

                var y = drawSchoolHeader(canvas, school, primaryColor, secondaryColor, accentColor, docType, languageCode)

                y = when (docType) {
                    SchoolDocumentType.BULLETIN -> drawBulletinBody(canvas, y, bulletin, school, primaryColor, accentColor, languageCode)
                    SchoolDocumentType.RECEIPT -> drawReceiptBody(canvas, y, payment, studentBalance, primaryColor, accentColor, languageCode)
                    SchoolDocumentType.INVOICE -> drawInvoiceBody(canvas, y, student, schoolClass, studentBalance, primaryColor, languageCode)
                    SchoolDocumentType.ENROLLMENT_CERT -> drawCertificateBody(canvas, y, school, student, isAttestation = false, primaryColor, languageCode)
                    SchoolDocumentType.ATTESTATION -> drawCertificateBody(canvas, y, school, student, isAttestation = true, primaryColor, languageCode)
                    SchoolDocumentType.STUDENT_CARD -> drawStudentCardBody(canvas, y, school, student, primaryColor, secondaryColor, accentColor, languageCode)
                    SchoolDocumentType.CLASS_ROSTER -> drawClassRosterBody(canvas, y, schoolClass, classRoster, primaryColor, languageCode)
                    SchoolDocumentType.FINANCIAL_REPORT -> drawFinancialReportBody(canvas, y, cashSummary, primaryColor, accentColor, languageCode)
                    SchoolDocumentType.UNPAID_REPORT -> drawUnpaidReportBody(canvas, y, unpaidList, primaryColor, languageCode)
                }

                drawOfficialFooter(canvas, school, y.coerceAtLeast(660f), primaryColor, languageCode)
                pdf.finishPage(page)

                FileOutputStream(outFile).use { pdf.writeTo(it) }
            } finally {
                pdf.close()
            }
            Result.success(outFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getShareUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun openOrSharePdf(context: Context, file: File) {
        val uri = getShareUri(context, file)
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            if (viewIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(viewIntent)
            } else {
                context.startActivity(Intent.createChooser(shareIntent, "PDF").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        } catch (_: Exception) {
            context.startActivity(Intent.createChooser(shareIntent, "PDF").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    fun exportSchoolDataJson(context: Context, schoolCode: String, jsonPayload: String): Result<File> {
        return try {
            val dir = File(context.filesDir, "exports").apply { mkdirs() }
            val outFile = File(dir, "export_${schoolCode}_${System.currentTimeMillis()}.json")
            outFile.writeText(jsonPayload)
            Result.success(outFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun drawSchoolHeader(
        canvas: Canvas,
        school: School,
        primaryColor: Int,
        secondaryColor: Int,
        accentColor: Int,
        docType: SchoolDocumentType,
        lang: String
    ): Float {
        val headerPaint = Paint().apply { color = primaryColor; style = Paint.Style.FILL }
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 112f, headerPaint)

        val accentPaint = Paint().apply { color = accentColor; style = Paint.Style.FILL }
        canvas.drawRect(0f, 112f, PAGE_WIDTH.toFloat(), 117f, accentPaint)

        val primaryLogo: Bitmap? = ImageCodec.decodeBase64ToBitmap(school.logoPrimaryBase64)
        val secondaryLogo: Bitmap? = ImageCodec.decodeBase64ToBitmap(school.logoSecondaryBase64)

        primaryLogo?.let { bmp ->
            canvas.drawBitmap(bmp, null, RectF(24f, 18f, 92f, 86f), null)
        }
        secondaryLogo?.let { bmp ->
            canvas.drawBitmap(bmp, null, RectF(PAGE_WIDTH - 92f, 18f, PAGE_WIDTH - 24f, 86f), null)
        }

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(226, 232, 240)
            textSize = 10f
            textAlign = Paint.Align.CENTER
        }
        val centerX = PAGE_WIDTH / 2f
        canvas.drawText(school.name.ifBlank { "ÉTABLISSEMENT" }.uppercase(), centerX, 38f, titlePaint)
        if (school.motto.isNotBlank()) {
            canvas.drawText("« ${school.motto} »", centerX, 55f, subPaint)
        }
        val contactLine = listOfNotNull(
            school.city.takeIf { it.isNotBlank() },
            school.phone.takeIf { it.isNotBlank() },
            school.email.takeIf { it.isNotBlank() }
        ).joinToString(" • ")
        if (contactLine.isNotBlank()) {
            canvas.drawText(contactLine, centerX, 72f, subPaint)
        }
        if (school.registrationNumber.isNotBlank()) {
            canvas.drawText("N° Agrément: ${school.registrationNumber}", centerX, 88f, subPaint)
        }

        // Document title pill
        val bannerPaint = Paint().apply { color = secondaryColor; style = Paint.Style.FILL }
        canvas.drawRoundRect(RectF(40f, 132f, PAGE_WIDTH - 40f, 162f), 8f, 8f, bannerPaint)
        val docTitle = if (lang == "EN") docType.labelEn.uppercase() else docType.labelFr.uppercase()
        canvas.drawText(docTitle, centerX, 152f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        })
        return 184f
    }

    private fun drawBulletinBody(
        canvas: Canvas,
        startY: Float,
        bulletin: StudentBulletinSummary?,
        school: School,
        primaryColor: Int,
        accentColor: Int,
        lang: String
    ): Float {
        var y = startY
        val labelPaint = labelPaint(primaryColor)
        val bodyPaint = bodyPaint()

        if (bulletin == null) {
            canvas.drawText(if (lang == "EN") "No bulletin data available." else "Aucune donnée de bulletin disponible.", 40f, y, bodyPaint)
            return y + 40f
        }

        val s = bulletin.student
        canvas.drawText("${if (lang == "EN") "Student" else "Élève"}: ${s.fullName}", 40f, y, labelPaint)
        canvas.drawText("Matricule: ${s.matricule}", 360f, y, labelPaint)
        y += 18f
        canvas.drawText("${if (lang == "EN") "Class" else "Classe"}: ${s.className}", 40f, y, bodyPaint)
        canvas.drawText("${if (lang == "EN") "Term" else "Trimestre"}: ${bulletin.term}  •  ${school.activeAcademicYearName}", 360f, y, bodyPaint)
        y += 24f

        // Table Header
        val headerBg = Paint().apply { color = Color.rgb(241, 245, 249) }
        canvas.drawRect(40f, y - 14f, PAGE_WIDTH - 40f, y + 8f, headerBg)
        canvas.drawText(if (lang == "EN") "Subject" else "Matière", 48f, y, labelPaint)
        canvas.drawText("Coef", 280f, y, labelPaint)
        canvas.drawText(if (lang == "EN") "Avg /20" else "Moy /20", 350f, y, labelPaint)
        canvas.drawText(if (lang == "EN") "Weighted" else "Points", 450f, y, labelPaint)
        y += 20f

        for (item in bulletin.subjectResults) {
            canvas.drawText(item.subjectName.take(30), 48f, y, bodyPaint)
            canvas.drawText(String.format(Locale.US, "%.1f", item.coefficient), 280f, y, bodyPaint)
            canvas.drawText(String.format(Locale.US, "%.2f", item.averageScore), 350f, y, bodyPaint)
            canvas.drawText(String.format(Locale.US, "%.2f", item.weightedPoints), 450f, y, bodyPaint)
            y += 18f
            if (y > 610f) break
        }

        y += 10f
        val boxPaint = Paint().apply { color = Color.rgb(248, 250, 252); style = Paint.Style.FILL }
        val borderPaint = Paint().apply { color = accentColor; style = Paint.Style.STROKE; strokeWidth = 1.5f }
        canvas.drawRoundRect(RectF(40f, y, PAGE_WIDTH - 40f, y + 72f), 8f, 8f, boxPaint)
        canvas.drawRoundRect(RectF(40f, y, PAGE_WIDTH - 40f, y + 72f), 8f, 8f, borderPaint)

        y += 22f
        canvas.drawText(
            "${if (lang == "EN") "Overall Average" else "Moyenne Générale"}: ${String.format(Locale.US, "%.2f", bulletin.overallAverage)} / 20",
            56f, y, labelPaint
        )
        canvas.drawText(
            "${if (lang == "EN") "Rank" else "Rang"}: ${bulletin.rank} / ${bulletin.totalStudentsInClass}",
            350f, y, labelPaint
        )
        y += 22f
        canvas.drawText("Mention: ${bulletin.mention}", 56f, y, bodyPaint)
        canvas.drawText("${if (lang == "EN") "Decision" else "Décision"}: ${bulletin.suggestedDecision}", 350f, y, bodyPaint)
        return y + 40f
    }

    private fun drawReceiptBody(
        canvas: Canvas,
        startY: Float,
        payment: PaymentRecord?,
        balance: StudentBalanceSummary?,
        primaryColor: Int,
        accentColor: Int,
        lang: String
    ): Float {
        var y = startY
        val labelPaint = labelPaint(primaryColor)
        val bodyPaint = bodyPaint()
        if (payment == null) {
            canvas.drawText("Aucun paiement sélectionné.", 40f, y, bodyPaint)
            return y + 40f
        }
        val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE).format(Date(payment.paidAtMillis))
        canvas.drawText("${if (lang == "EN") "Receipt N°" else "Reçu N°"}: ${payment.receiptNumber}", 40f, y, labelPaint)
        canvas.drawText("Date: $dateStr", 350f, y, bodyPaint)
        y += 24f
        canvas.drawText("${if (lang == "EN") "Student" else "Élève"}: ${payment.studentName} (${payment.studentMatricule})", 40f, y, bodyPaint)
        y += 20f
        canvas.drawText("${if (lang == "EN") "Class" else "Classe"}: ${payment.className}", 40f, y, bodyPaint)
        y += 20f
        canvas.drawText("${if (lang == "EN") "Fee Category" else "Motif"}: ${payment.feeCategory}", 40f, y, bodyPaint)
        y += 20f
        canvas.drawText("${if (lang == "EN") "Payment Method" else "Mode de paiement"}: ${payment.method} ${payment.referenceNumber}", 40f, y, bodyPaint)
        y += 28f

        val highlightPaint = Paint().apply { color = accentColor; style = Paint.Style.FILL }
        canvas.drawRoundRect(RectF(40f, y, PAGE_WIDTH - 40f, y + 48f), 8f, 8f, highlightPaint)
        canvas.drawText(
            "${if (lang == "EN") "AMOUNT PAID" else "MONTANT ENCAISSÉ"}: ${FinanceCalculator.formatFcfa(payment.amountFcfa)}",
            PAGE_WIDTH / 2f, y + 30f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 15f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
        )
        y += 70f
        balance?.let {
            canvas.drawText("${if (lang == "EN") "Remaining Balance" else "Reste à payer"}: ${FinanceCalculator.formatFcfa(it.remainingBalanceFcfa)}", 40f, y, labelPaint)
            y += 20f
        }
        return y + 20f
    }

    private fun drawInvoiceBody(
        canvas: Canvas,
        startY: Float,
        student: Student?,
        schoolClass: SchoolClass?,
        balance: StudentBalanceSummary?,
        primaryColor: Int,
        lang: String
    ): Float {
        var y = startY
        val labelPaint = labelPaint(primaryColor)
        val bodyPaint = bodyPaint()
        if (student == null) return y + 40f

        canvas.drawText("${if (lang == "EN") "Student" else "Élève"}: ${student.fullName} (${student.matricule})", 40f, y, labelPaint)
        y += 20f
        canvas.drawText("${if (lang == "EN") "Class" else "Classe"}: ${student.className}", 40f, y, bodyPaint)
        y += 24f
        canvas.drawText("${if (lang == "EN") "Registration Fee" else "Frais d'inscription"}: ${FinanceCalculator.formatFcfa(schoolClass?.registrationFeeFcfa ?: 0L)}", 40f, y, bodyPaint)
        y += 20f
        canvas.drawText("${if (lang == "EN") "Annual Tuition" else "Frais de scolarité"}: ${FinanceCalculator.formatFcfa(schoolClass?.tuitionFeeFcfa ?: 0L)}", 40f, y, bodyPaint)
        y += 20f
        canvas.drawText("${if (lang == "EN") "Scholarship Discount" else "Bourse / Réduction"}: -${FinanceCalculator.formatFcfa(student.scholarshipDiscountFcfa)}", 40f, y, bodyPaint)
        y += 24f
        balance?.let {
            canvas.drawText("${if (lang == "EN") "Net Expected" else "Net à payer"}: ${FinanceCalculator.formatFcfa(it.expectedFeeFcfa)}", 40f, y, labelPaint)
            y += 20f
            canvas.drawText("${if (lang == "EN") "Already Paid" else "Déjà versé"}: ${FinanceCalculator.formatFcfa(it.paidAmountFcfa)}", 40f, y, bodyPaint)
            y += 20f
            canvas.drawText("${if (lang == "EN") "Balance Due" else "Solde restant dû"}: ${FinanceCalculator.formatFcfa(it.remainingBalanceFcfa)}", 40f, y, labelPaint)
        }
        return y + 40f
    }

    private fun drawCertificateBody(
        canvas: Canvas,
        startY: Float,
        school: School,
        student: Student?,
        isAttestation: Boolean,
        primaryColor: Int,
        lang: String
    ): Float {
        var y = startY + 20f
        val bodyPaint = bodyPaint().apply { textSize = 13f }
        val labelPaint = labelPaint(primaryColor).apply { textSize = 13f }
        if (student == null) return y + 40f

        val intro = if (lang == "EN") {
            "The Principal of ${school.name} hereby certifies that:"
        } else {
            "Le Chef d'Établissement de ${school.name} soussigné certifie que :"
        }
        canvas.drawText(intro, 40f, y, bodyPaint)
        y += 32f
        canvas.drawText("${student.fullName} (${student.matricule})", 60f, y, labelPaint)
        y += 24f
        if (student.birthDate.isNotBlank()) {
            canvas.drawText("${if (lang == "EN") "Born on" else "Né(e) le"}: ${student.birthDate} ${student.birthPlace}", 60f, y, bodyPaint)
            y += 22f
        }
        val statusLine = if (isAttestation) {
            if (lang == "EN") "Regularly attends classes in ${student.className} for academic year ${school.activeAcademicYearName}."
            else "Fréquente régulièrement la classe de ${student.className} au titre de l'année ${school.activeAcademicYearName}."
        } else {
            if (lang == "EN") "Is duly enrolled in ${student.className} for academic year ${school.activeAcademicYearName}."
            else "Est régulièrement inscrit(e) en classe de ${student.className} pour l'année scolaire ${school.activeAcademicYearName}."
        }
        canvas.drawText(statusLine, 40f, y + 10f, bodyPaint)
        return y + 60f
    }

    private fun drawStudentCardBody(
        canvas: Canvas,
        startY: Float,
        school: School,
        student: Student?,
        primaryColor: Int,
        secondaryColor: Int,
        accentColor: Int,
        lang: String
    ): Float {
        var y = startY + 10f
        if (student == null) return y + 40f
        val cardRect = RectF(70f, y, PAGE_WIDTH - 70f, y + 220f)
        val border = Paint().apply { color = primaryColor; style = Paint.Style.STROKE; strokeWidth = 2.5f }
        val bg = Paint().apply { color = Color.rgb(248, 250, 252); style = Paint.Style.FILL }
        canvas.drawRoundRect(cardRect, 14f, 14f, bg)
        canvas.drawRoundRect(cardRect, 14f, 14f, border)

        val strip = Paint().apply { color = secondaryColor; style = Paint.Style.FILL }
        canvas.drawRoundRect(RectF(70f, y, PAGE_WIDTH - 70f, y + 44f), 14f, 14f, strip)
        canvas.drawText(
            school.name.uppercase(),
            PAGE_WIDTH / 2f, y + 28f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 13f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
        )

        val label = labelPaint(primaryColor)
        val body = bodyPaint()
        canvas.drawText("MATRICULE: ${student.matricule}", 95f, y + 78f, label)
        canvas.drawText("${if (lang == "EN") "FULL NAME" else "NOM & PRÉNOM"}: ${student.fullName}", 95f, y + 104f, body)
        canvas.drawText("${if (lang == "EN") "CLASS" else "CLASSE"}: ${student.className}", 95f, y + 128f, body)
        canvas.drawText("${if (lang == "EN") "ACADEMIC YEAR" else "ANNÉE"}: ${school.activeAcademicYearName}", 95f, y + 152f, body)
        canvas.drawText("${if (lang == "EN") "PARENT" else "TUTEUR"}: ${student.parentName} (${student.parentPhone})", 95f, y + 176f, body)
        canvas.drawLine(95f, y + 195f, PAGE_WIDTH - 95f, y + 195f, Paint().apply { color = accentColor; strokeWidth = 2f })
        return y + 250f
    }

    private fun drawClassRosterBody(
        canvas: Canvas,
        startY: Float,
        schoolClass: SchoolClass?,
        roster: List<Student>,
        primaryColor: Int,
        lang: String
    ): Float {
        var y = startY
        val label = labelPaint(primaryColor)
        val body = bodyPaint()
        canvas.drawText("${if (lang == "EN") "Class" else "Classe"}: ${schoolClass?.name ?: "-"}  (${roster.size} ${if (lang == "EN") "students" else "élèves"})", 40f, y, label)
        y += 24f
        for ((idx, student) in roster.withIndex()) {
            canvas.drawText("${idx + 1}.  ${student.matricule}  —  ${student.fullName}  (${student.gender})", 44f, y, body)
            y += 18f
            if (y > 630f) break
        }
        return y + 20f
    }

    private fun drawFinancialReportBody(
        canvas: Canvas,
        startY: Float,
        cash: CashRegisterSummary?,
        primaryColor: Int,
        accentColor: Int,
        lang: String
    ): Float {
        var y = startY
        val label = labelPaint(primaryColor)
        val body = bodyPaint()
        if (cash == null) return y + 40f
        canvas.drawText("${if (lang == "EN") "Total Collected Revenue" else "Total Recettes Encaissées"}: ${FinanceCalculator.formatFcfa(cash.totalRevenueFcfa)}", 40f, y, label)
        y += 22f
        canvas.drawText("${if (lang == "EN") "Total Expenses" else "Total Dépenses"}: ${FinanceCalculator.formatFcfa(cash.totalExpensesFcfa)}", 40f, y, body)
        y += 22f
        canvas.drawText("${if (lang == "EN") "Refunds" else "Remboursements"}: ${FinanceCalculator.formatFcfa(cash.totalRefundedFcfa)}", 40f, y, body)
        y += 26f
        canvas.drawText("${if (lang == "EN") "NET CASH BALANCE" else "SOLDE NET EN CAISSE"}: ${FinanceCalculator.formatFcfa(cash.netBalanceFcfa)}", 40f, y, labelPaint(accentColor))
        y += 30f
        cash.revenueByMethod.forEach { (method, total) ->
            canvas.drawText("• $method : ${FinanceCalculator.formatFcfa(total)}", 56f, y, body)
            y += 18f
        }
        return y + 20f
    }

    private fun drawUnpaidReportBody(
        canvas: Canvas,
        startY: Float,
        unpaid: List<StudentBalanceSummary>,
        primaryColor: Int,
        lang: String
    ): Float {
        var y = startY
        val label = labelPaint(primaryColor)
        val body = bodyPaint()
        val onlyUnpaid = unpaid.filter { it.remainingBalanceFcfa > 0L }
        val totalDue = onlyUnpaid.sumOf { it.remainingBalanceFcfa }
        canvas.drawText("${if (lang == "EN") "Total Unpaid Balances" else "Total des Impayés"}: ${FinanceCalculator.formatFcfa(totalDue)} (${onlyUnpaid.size})", 40f, y, label)
        y += 24f
        for (item in onlyUnpaid) {
            canvas.drawText("${item.student.matricule} • ${item.student.fullName} (${item.student.className}): ${FinanceCalculator.formatFcfa(item.remainingBalanceFcfa)}", 44f, y, body)
            y += 18f
            if (y > 630f) break
        }
        return y + 20f
    }

    private fun drawOfficialFooter(
        canvas: Canvas,
        school: School,
        y: Float,
        primaryColor: Int,
        lang: String
    ) {
        val label = labelPaint(primaryColor)
        val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(Date())
        canvas.drawText("${school.city.ifBlank { "Cameroun" }}, ${if (lang == "EN") "on" else "le"} $dateStr", 40f, y, bodyPaint())
        canvas.drawText(if (lang == "EN") "The Principal / Director" else "La Direction", 380f, y, label)
        if (school.directorName.isNotBlank()) {
            canvas.drawText(school.directorName, 380f, y + 16f, bodyPaint())
        }

        val stampBmp = ImageCodec.decodeBase64ToBitmap(school.stampBase64)
        val sigBmp = ImageCodec.decodeBase64ToBitmap(school.signatureBase64)
        stampBmp?.let { canvas.drawBitmap(it, null, RectF(260f, y + 18f, 340f, y + 98f), null) }
        sigBmp?.let { canvas.drawBitmap(it, null, RectF(380f, y + 24f, 510f, y + 84f), null) }

        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(100, 116, 139)
            textSize = 9f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("ADRIÈL SCHOOL MANAGER — ADRIÈL TECH SaaS ERP", PAGE_WIDTH / 2f, PAGE_HEIGHT - 20f, footerPaint)
    }

    private fun labelPaint(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textSize = 12f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private fun bodyPaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(30, 41, 59)
        textSize = 11.5f
    }

    private fun parseHexColor(hex: String, fallback: Int): Int {
        return try {
            if (hex.isBlank()) fallback else Color.parseColor(hex)
        } catch (_: Exception) {
            fallback
        }
    }
}
