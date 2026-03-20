package org.kukro.ezbill.util

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime

internal fun formatExpenseCreatedAt(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    runCatching {
        val chinaDateTime =
            Instant.parse(raw).toLocalDateTime(TimeZone.of("Asia/Shanghai"))
        return "${chinaDateTime.month.number.pad2()}-${chinaDateTime.day.pad2()} " +
            "${chinaDateTime.hour.pad2()}:${chinaDateTime.minute.pad2()}"
    }

    val noZone = raw
        .replace('T', ' ')
        .substringBefore('+')
        .substringBefore('Z')
    return when {
        noZone.length >= 16 -> noZone.substring(5, 16)
        else -> noZone
    }
}

private fun Int.pad2(): String = toString().padStart(2, '0')
