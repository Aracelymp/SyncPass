package com.rojas.syncpass.data.local

import com.rojas.syncpass.core.util.Hash
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Fictional data available before the first online sync. It mirrors the backend seed
 * (same ids and QR tokens) so the app can be explored fully offline.
 * Demo password for every account: Demo1234.
 */
object DemoLocalSeeder {

    suspend fun seedIfEmpty(db: AppDatabase) {
        if (db.userDao().count() > 0) return

        val hash = Hash.localPassword("coordinador", "Demo1234")
        fun h(user: String) = Hash.localPassword(user, "Demo1234")

        db.userDao().upsert(
            UserEntity(1, "coordinador", "coordinador@syncpass.demo", "Laura Méndez",
                "DOCENTE_COORDINADOR", hash, 1)
        )
        db.userDao().upsert(
            UserEntity(2, "monitor", "monitor@syncpass.demo", "Diego Ramírez",
                "ESTUDIANTE_VOLUNTARIO_MONITOR", h("monitor"), 1)
        )
        db.userDao().upsert(
            UserEntity(3, "estudiante", "estudiante@syncpass.demo", "Ana Torres",
                "ESTUDIANTE_VOLUNTARIO_MONITOR", h("estudiante"), 1)
        )
        db.userDao().upsert(
            UserEntity(4, "valentina", "valentina@syncpass.demo", "Valentina Ríos",
                "ESTUDIANTE_VOLUNTARIO_MONITOR", h("valentina"), 1)
        )
        db.userDao().upsert(
            UserEntity(5, "camilo", "camilo@syncpass.demo", "Camilo Herrera",
                "ESTUDIANTE_VOLUNTARIO_MONITOR", h("camilo"), 1)
        )

        val today = LocalDate.now()
        val createdAt = LocalDateTime.now().minusMonths(2).toString()
        db.projectDao().upsertAll(
            listOf(
                ProjectEntity(1, "PRY-TECH", "TechGuía: tutorías comunitarias",
                    "Acompañamiento académico y talleres de tecnología para jóvenes de la comunidad.",
                    today.minusMonths(2).toString(), today.plusMonths(4).toString(),
                    "EN_CURSO", "Laura Méndez", 2, createdAt),
                ProjectEntity(2, "PRY-VERDE", "Semillas de cambio",
                    "Jornadas de reforestación, educación ambiental y cuidado de espacios públicos.",
                    today.minusMonths(1).toString(), today.plusMonths(3).toString(),
                    "EN_CURSO", "Laura Méndez", 1, createdAt),
                ProjectEntity(3, "PRY-CONECTA", "Conecta Mayor",
                    "Alfabetización digital para personas mayores: trámites, comunicación y seguridad en línea.",
                    today.minusMonths(5).toString(), today.minusDays(10).toString(),
                    "FINALIZADO", "Laura Méndez", 1, createdAt)
            )
        )

        fun at(days: Long, hour: Int, minute: Int = 0): String =
            LocalDateTime.now().toLocalDate().plusDays(days).atTime(hour, minute).toString()

        db.eventDao().upsertAll(
            listOf(
                EventEntity(1, 1, "TechGuía: tutorías comunitarias", "Taller de robótica básica",
                    "Construcción de un robot con sensores y programación por bloques. Se entregan materiales.",
                    "Laboratorio 204 · Sede Central", at(1, 15), at(1, 17), 30, "SP-ROBOTICA-2026",
                    "PROGRAMADO", 3, 0, 0.0),
                EventEntity(2, 2, "Semillas de cambio", "Jornada de reforestación",
                    "Plantación de 120 árboles nativos. Lleva ropa cómoda y botella de agua.",
                    "Parque Ecológico Municipal", at(5, 8), at(5, 12), 60, "SP-REVERDE-2026",
                    "PROGRAMADO", 3, 0, 0.0),
                EventEntity(3, 1, "TechGuía: tutorías comunitarias", "Feria de ciencia y tecnología",
                    "Muestra de proyectos estudiantiles, retos de programación y charlas abiertas.",
                    "Plaza de la Innovación", at(12, 9), at(12, 13), 120, "SP-FERIA-2026",
                    "PROGRAMADO", 2, 0, 0.0),
                EventEntity(4, 3, "Conecta Mayor", "Alfabetización digital para adultos",
                    "Sesión práctica sobre trámites en línea, videollamadas y contraseñas seguras.",
                    "Centro Comunitario El Nogal", at(-20, 10), at(-20, 12), 40, "SP-CONECTA-2026",
                    "FINALIZADO", 3, 4, 4.7)
            )
        )

        val enrollCreated = LocalDateTime.now().minusDays(15).toString()
        db.enrollmentDao().upsertAll(
            listOf(
                EnrollmentEntity(1, 1, 2, "Diego Ramírez", "INSCRITO", enrollCreated),
                EnrollmentEntity(2, 1, 3, "Ana Torres", "INSCRITO", enrollCreated),
                EnrollmentEntity(3, 1, 4, "Valentina Ríos", "INSCRITO", enrollCreated),
                EnrollmentEntity(4, 2, 2, "Diego Ramírez", "INSCRITO", enrollCreated),
                EnrollmentEntity(5, 2, 4, "Valentina Ríos", "INSCRITO", enrollCreated),
                EnrollmentEntity(6, 2, 5, "Camilo Herrera", "INSCRITO", enrollCreated),
                EnrollmentEntity(7, 3, 3, "Ana Torres", "INSCRITO", enrollCreated),
                EnrollmentEntity(8, 3, 5, "Camilo Herrera", "INSCRITO", enrollCreated),
                EnrollmentEntity(9, 4, 3, "Ana Torres", "INSCRITO", enrollCreated),
                EnrollmentEntity(10, 4, 2, "Diego Ramírez", "INSCRITO", enrollCreated),
                EnrollmentEntity(11, 4, 4, "Valentina Ríos", "INSCRITO", enrollCreated)
            )
        )

        db.attendanceDao().upsertAll(
            listOf(
                AttendanceEntity(1, 1, 4, "Alfabetización digital para adultos", 3, "Ana Torres",
                    "estudiante@syncpass.demo", null, "QR", "PRESENTE",
                    at(-20, 10, 3), "Laura Méndez", "seed-att-digital-estudiante"),
                AttendanceEntity(2, 2, 4, "Alfabetización digital para adultos", 2, "Diego Ramírez",
                    "monitor@syncpass.demo", null, "QR", "PRESENTE",
                    at(-20, 10, 8), "Laura Méndez", "seed-att-digital-monitor"),
                AttendanceEntity(3, 3, 4, "Alfabetización digital para adultos", 4, "Valentina Ríos",
                    "valentina@syncpass.demo", null, "NO_INSCRITO", "PRESENTE",
                    at(-20, 10, 12), "Laura Méndez", "seed-att-digital-valentina"),
                AttendanceEntity(4, 4, 4, "Alfabetización digital para adultos", null, "Invitada especial",
                    "invitada@syncpass.demo", null, "MANUAL", "PRESENTE",
                    at(-20, 10, 15), "Laura Méndez", "seed-att-digital-invitada")
            )
        )

        db.surveyDao().upsertAll(
            listOf(
                SurveyEntity(1, 1, 4, 3, "Ana Torres", 5, 5, 5,
                    "Muy útil. Aprendí a hacer videollamadas con mi familia.", true,
                    at(-20, 13), "seed-survey-digital-estudiante"),
                SurveyEntity(2, 2, 4, 2, "Diego Ramírez", 4, 5, 4,
                    "Buen ritmo y acompañamiento cercano.", true,
                    at(-19, 9), "seed-survey-digital-monitor"),
                SurveyEntity(3, 3, 4, 4, "Valentina Ríos", 5, 4, 5,
                    "Ojalá haya una segunda parte.", true,
                    at(-18, 10), "seed-survey-digital-valentina")
            )
        )
    }
}
