package com.rojas.syncpass.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QrAndHashTest {

    @Test
    fun `parses full event payload`() {
        val payload = Qr.parse("SYNCPASS|EVENT|42|SP-DEMO-2026")

        assertEquals(42L, payload?.eventId)
        assertEquals("SP-DEMO-2026", payload?.token)
        assertEquals("SYNCPASS|EVENT|42|SP-DEMO-2026", payload?.raw)
    }

    @Test
    fun `accepts a raw token and trims whitespace`() {
        val payload = Qr.parse("  SP-ROBOTICA-2026  ")

        assertNull(payload?.eventId)
        assertEquals("SP-ROBOTICA-2026", payload?.token)
    }

    @Test
    fun `rejects blank and malformed payloads`() {
        assertNull(Qr.parse("   "))
        assertNull(Qr.parse("SYNCPASS|EVENT|not-a-number|TOKEN"))
        assertNull(Qr.parse("SYNCPASS|EVENT|7|"))
    }

    @Test
    fun `offline password hash is deterministic and normalized`() {
        val expected = "65cd463aa0181f0cd9f23784cc1e6177521a9d7db026fffc6c2929d1165ad62c"

        assertEquals(expected, Hash.localPassword("Coordinador", "Demo1234"))
        assertEquals(expected, Hash.localPassword("  coordinador  ", "Demo1234"))
    }

    @Test
    fun `labels map backend enum values`() {
        assertEquals("Docente \u00b7 Coordinador", roleLabel("DOCENTE_COORDINADOR"))
        assertEquals("Estudiante \u00b7 Monitor", roleLabel("ESTUDIANTE_VOLUNTARIO_MONITOR"))
        assertEquals("En curso", eventStatusLabel("EN_CURSO"))
        assertEquals("Finalizado", projectStatusLabel("FINALIZADO"))
        assertEquals("C\u00f3digo QR", methodLabel("QR"))
    }
}
