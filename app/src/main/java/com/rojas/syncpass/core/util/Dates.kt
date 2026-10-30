package com.rojas.syncpass.core.util

import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

object Dates {

    private val locale = Locale.forLanguageTag("es-CO")
    private val dayFormat = DateTimeFormatter.ofPattern("EEE d MMM", locale)
    private val dateFormat = DateTimeFormatter.ofPattern("d MMM yyyy", locale)
    private val timeFormat = DateTimeFormatter.ofPattern("h:mm a", locale)
    private val inputFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")

    fun nowIso(): String = Instant.now().toString()

    fun parseDateTime(raw: String?): LocalDateTime? {
        if (raw.isNullOrBlank()) return null
        return try {
            LocalDateTime.parse(raw)
        } catch (_: DateTimeParseException) {
            try {
                OffsetDateTime.parse(raw).toLocalDateTime()
            } catch (_: DateTimeParseException) {
                try {
                    Instant.parse(raw).atZone(ZoneId.systemDefault()).toLocalDateTime()
                } catch (_: DateTimeParseException) {
                    null
                }
            }
        }
    }

    fun formatEventDate(raw: String?): String {
        val value = parseDateTime(raw) ?: return "Fecha por confirmar"
        return "${value.format(dayFormat).replaceFirstChar { it.uppercase(locale) }} · ${value.format(timeFormat)}"
    }

    fun formatEventDay(raw: String?): String {
        val value = parseDateTime(raw) ?: return "—"
        return value.format(dayFormat).replaceFirstChar { it.uppercase(locale) }
    }

    fun formatTime(raw: String?): String {
        val value = parseDateTime(raw) ?: return "—"
        return value.format(timeFormat)
    }

    fun formatDate(raw: String?): String {
        if (raw.isNullOrBlank()) return "—"
        return try {
            LocalDate.parse(raw).format(dateFormat)
        } catch (_: DateTimeParseException) {
            raw
        }
    }

    fun formatInstant(raw: String?): String {
        val value = parseDateTime(raw) ?: return "—"
        return "${value.format(dayFormat)} · ${value.format(timeFormat)}"
    }

    fun formatTimestamp(timestamp: Long): String {
        val value = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault())
        return "${value.format(dayFormat).replaceFirstChar { it.uppercase(locale) }} · ${value.format(timeFormat)}"
    }

    fun relativeLastSync(timestamp: Long?): String {
        if (timestamp == null) return "Aún no se ha sincronizado"
        val minutes = (System.currentTimeMillis() - timestamp) / 60_000
        return when {
            minutes < 1 -> "Hace unos segundos"
            minutes < 60 -> "Hace $minutes min"
            minutes < 1440 -> "Hace ${minutes / 60} h"
            else -> "Hace ${minutes / 1440} días"
        }
    }

    fun normalizeInputDate(raw: String): String? {
        val cleaned = raw.trim()
        return try {
            LocalDateTime.parse(cleaned, inputFormat).toString()
        } catch (_: DateTimeParseException) {
            try {
                LocalDateTime.parse(cleaned).toString()
            } catch (_: DateTimeParseException) {
                null
            }
        }
    }

    fun toInstant(raw: String?): Instant? {
        val value = parseDateTime(raw) ?: return null
        return value.atZone(ZoneId.systemDefault()).toInstant()
    }

    fun isToday(raw: String?): Boolean {
        val value = parseDateTime(raw) ?: return false
        return value.toLocalDate() == LocalDate.now()
    }

    fun isUpcoming(raw: String?): Boolean {
        val value = parseDateTime(raw) ?: return false
        return value.isAfter(LocalDateTime.now().minusHours(1))
    }
}

object Hash {
    fun localPassword(username: String, password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("syncpass:${username.trim().lowercase()}:$password".toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

fun roleLabel(role: String): String = when (role) {
    "DOCENTE_COORDINADOR" -> "Docente · Coordinador"
    "ESTUDIANTE_VOLUNTARIO_MONITOR" -> "Estudiante · Monitor"
    else -> role
}

fun eventStatusLabel(status: String): String = when (status) {
    "PROGRAMADO" -> "Programado"
    "EN_CURSO" -> "En curso"
    "FINALIZADO" -> "Finalizado"
    "CANCELADO" -> "Cancelado"
    else -> status
}

fun projectStatusLabel(status: String): String = when (status) {
    "PLANIFICADO" -> "Planificado"
    "EN_CURSO" -> "En curso"
    "FINALIZADO" -> "Finalizado"
    else -> status
}

fun methodLabel(method: String?): String = when (method) {
    "QR" -> "Código QR"
    "MANUAL" -> "Manual"
    "NO_INSCRITO" -> "No inscrito"
    else -> "—"
}
