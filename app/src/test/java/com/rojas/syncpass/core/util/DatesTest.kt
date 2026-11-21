package com.rojas.syncpass.core.util

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class DatesTest {

    @Test
    fun `parseDateTime returns null for blank input`() {
        assertNull(Dates.parseDateTime(null))
        assertNull(Dates.parseDateTime(""))
        assertNull(Dates.parseDateTime("   "))
    }

    @Test
    fun `parseDateTime parses LocalDateTime`() {
        val result = Dates.parseDateTime("2026-08-15T14:30:00")
        assertEquals(LocalDateTime.of(2026, 8, 15, 14, 30, 0), result)
    }

    @Test
    fun `parseDateTime parses OffsetDateTime`() {
        val result = Dates.parseDateTime("2026-08-15T14:30:00-05:00")
        assertNotNull(result)
        assertEquals(LocalDateTime.of(2026, 8, 15, 14, 30, 0), result)
    }

    @Test
    fun `parseDateTime parses Instant`() {
        val instant = Instant.parse("2026-08-15T19:30:00Z")
        val result = Dates.parseDateTime(instant.toString())
        assertNotNull(result)
        assertEquals(LocalDateTime.of(2026, 8, 15, 19, 30, 0), result)
    }

    @Test
    fun `formatEventDate returns fallback for null`() {
        assertEquals("Fecha por confirmar", Dates.formatEventDate(null))
    }

    @Test
    fun `formatEventDate returns formatted string`() {
        val formatted = Dates.formatEventDate("2026-08-15T14:30:00")
        assertTrue(formatted.contains("15"))
        assertTrue(formatted.contains("ago"))
        assertTrue(formatted.contains("2:30"))
    }

    @Test
    fun `formatDate returns raw when invalid`() {
        assertEquals("invalid", Dates.formatDate("invalid"))
    }

    @Test
    fun `formatDate parses yyyy-MM-dd`() {
        val formatted = Dates.formatDate("2026-08-15")
        assertTrue(formatted.contains("15"))
        assertTrue(formatted.contains("ago"))
        assertTrue(formatted.contains("2026"))
    }

    @Test
    fun `formatTimestamp formats a millis value`() {
        val instant = Instant.parse("2026-08-15T14:30:00Z")
        val formatted = Dates.formatTimestamp(instant.toEpochMilli())
        assertTrue(formatted.contains("15"))
        assertTrue(formatted.contains("ago"))
    }

    @Test
    fun `relativeLastSync returns never synced when null`() {
        assertEquals("Aún no se ha sincronizado", Dates.relativeLastSync(null))
    }

    @Test
    fun `relativeLastSync returns seconds for very recent timestamp`() {
        val result = Dates.relativeLastSync(System.currentTimeMillis() - 500)
        assertEquals("Hace unos segundos", result)
    }

    @Test
    fun `relativeLastSync returns minutes`() {
        val result = Dates.relativeLastSync(System.currentTimeMillis() - 5 * 60_000)
        assertEquals("Hace 5 min", result)
    }

    @Test
    fun `relativeLastSync returns hours`() {
        val result = Dates.relativeLastSync(System.currentTimeMillis() - 2 * 60 * 60_000)
        assertTrue(result.startsWith("Hace 2 h"))
    }

    @Test
    fun `relativeLastSync returns days`() {
        val result = Dates.relativeLastSync(System.currentTimeMillis() - 2 * 24 * 60 * 60_000)
        assertTrue(result.startsWith("Hace 2"))
        assertTrue(result.contains("días"))
    }

    @Test
    fun `normalizeInputDate parses expected format`() {
        assertEquals("2026-08-15T14:30", Dates.normalizeInputDate("2026-08-15T14:30"))
    }

    @Test
    fun `normalizeInputDate returns null for invalid`() {
        assertNull(Dates.normalizeInputDate("not-a-date"))
    }

    @Test
    fun `toInstant converts parsed datetime`() {
        val instant = Dates.toInstant("2026-08-15T14:30:00")
        assertNotNull(instant)
        val expected = LocalDateTime.of(2026, 8, 15, 14, 30, 0)
            .atZone(ZoneId.systemDefault()).toInstant()
        assertEquals(expected, instant)
    }

    @Test
    fun `isToday returns true for current date`() {
        val now = LocalDateTime.now().toString()
        assertTrue(Dates.isToday(now))
    }

    @Test
    fun `isToday returns false for null`() {
        assertFalse(Dates.isToday(null))
    }

    @Test
    fun `isUpcoming returns true for future date`() {
        val future = LocalDateTime.now().plusDays(1).toString()
        assertTrue(Dates.isUpcoming(future))
    }

    @Test
    fun `isUpcoming returns false for null`() {
        assertFalse(Dates.isUpcoming(null))
    }

    @Test
    fun `roleLabel maps known roles`() {
        assertTrue(roleLabel("DOCENTE_COORDINADOR").contains("Docente"))
        assertTrue(roleLabel("ESTUDIANTE_VOLUNTARIO_MONITOR").contains("Estudiante"))
        assertEquals("OTHER", roleLabel("OTHER"))
    }

    @Test
    fun `eventStatusLabel maps known statuses`() {
        assertEquals("Programado", eventStatusLabel("PROGRAMADO"))
        assertEquals("En curso", eventStatusLabel("EN_CURSO"))
        assertEquals("Finalizado", eventStatusLabel("FINALIZADO"))
        assertEquals("Cancelado", eventStatusLabel("CANCELADO"))
        assertEquals("OTHER", eventStatusLabel("OTHER"))
    }

    @Test
    fun `projectStatusLabel maps known statuses`() {
        assertEquals("Planificado", projectStatusLabel("PLANIFICADO"))
        assertEquals("En curso", projectStatusLabel("EN_CURSO"))
        assertEquals("Finalizado", projectStatusLabel("FINALIZADO"))
        assertEquals("OTHER", projectStatusLabel("OTHER"))
    }

    @Test
    fun `methodLabel maps known methods`() {
        assertTrue(methodLabel("QR").contains("QR"))
        assertEquals("Manual", methodLabel("MANUAL"))
        assertEquals("No inscrito", methodLabel("NO_INSCRITO"))
        val fallback = methodLabel(null)
        assertEquals(fallback, methodLabel("OTHER"))
    }
}
