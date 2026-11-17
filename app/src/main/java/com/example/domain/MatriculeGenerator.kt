package com.example.domain

import java.util.Calendar

object MatriculeGenerator {

    fun generateNextMatricule(
        schoolCode: String,
        existingMatricules: Collection<String>,
        year: Int = Calendar.getInstance().get(Calendar.YEAR)
    ): String {
        val prefix = schoolCode
            .uppercase()
            .replace(Regex("[^A-Z0-9]"), "")
            .take(4)
            .ifBlank { "ASM" }
        val yearShort = (year % 100).toString().padStart(2, '0')
        val existingSet = existingMatricules.toSet()
        var counter = existingSet.size + 1
        while (true) {
            val candidate = "$prefix-$yearShort-${counter.toString().padStart(4, '0')}"
            if (candidate !in existingSet) return candidate
            counter++
        }
    }

    fun generateNextReceiptNumber(
        existingReceipts: Collection<String>,
        year: Int = Calendar.getInstance().get(Calendar.YEAR)
    ): String {
        val existingSet = existingReceipts.toSet()
        var counter = existingSet.size + 1
        while (true) {
            val candidate = "REC-$year-${counter.toString().padStart(5, '0')}"
            if (candidate !in existingSet) return candidate
            counter++
        }
    }

    fun generateSchoolCode(schoolName: String): String {
        val tokens = schoolName
            .uppercase()
            .replace(Regex("[^A-Z0-9 ]"), " ")
            .trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
        val initials = tokens.joinToString("") { it.take(1) }.take(4)
        val suffix = (100..999).random()
        return "${initials.ifBlank { "SCH" }}$suffix"
    }
}
