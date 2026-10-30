package com.rojas.syncpass.backend.config;

import com.rojas.syncpass.backend.domain.*;
import com.rojas.syncpass.backend.repo.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Fictional, anonymised demo data. Every account uses the password "Demo1234".
 * This makes the Android app usable immediately, including offline login.
 */
@Component
public class DemoDataSeeder implements CommandLineRunner {

    private final UserRepository users;
    private final ProjectRepository projects;
    private final EventRepository events;
    private final EnrollmentRepository enrollments;
    private final AttendanceRepository attendances;
    private final SurveyRepository surveys;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed-demo-data:true}")
    private boolean seed;

    public DemoDataSeeder(
            UserRepository users,
            ProjectRepository projects,
            EventRepository events,
            EnrollmentRepository enrollments,
            AttendanceRepository attendances,
            SurveyRepository surveys,
            PasswordEncoder passwordEncoder
    ) {
        this.users = users;
        this.projects = projects;
        this.events = events;
        this.enrollments = enrollments;
        this.attendances = attendances;
        this.surveys = surveys;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!seed || users.count() > 0) {
            return;
        }

        String hash = passwordEncoder.encode("Demo1234");
        User coordinator = users.save(User.builder()
                .username("coordinador").email("coordinador@syncpass.demo")
                .fullName("Laura Méndez").passwordHash(hash)
                .role(Role.DOCENTE_COORDINADOR).build());
        User monitor = users.save(User.builder()
                .username("monitor").email("monitor@syncpass.demo")
                .fullName("Diego Ramírez").passwordHash(hash)
                .role(Role.ESTUDIANTE_VOLUNTARIO_MONITOR).build());
        User student = users.save(User.builder()
                .username("estudiante").email("estudiante@syncpass.demo")
                .fullName("Ana Torres").passwordHash(hash)
                .role(Role.ESTUDIANTE_VOLUNTARIO_MONITOR).build());
        User valentina = users.save(User.builder()
                .username("valentina").email("valentina@syncpass.demo")
                .fullName("Valentina Ríos").passwordHash(hash)
                .role(Role.ESTUDIANTE_VOLUNTARIO_MONITOR).build());
        User camilo = users.save(User.builder()
                .username("camilo").email("camilo@syncpass.demo")
                .fullName("Camilo Herrera").passwordHash(hash)
                .role(Role.ESTUDIANTE_VOLUNTARIO_MONITOR).build());

        LocalDate today = LocalDate.now();

        Project tech = projects.save(Project.builder()
                .code("PRY-TECH").name("TechGuía: tutorías comunitarias")
                .description("Acompañamiento académico y talleres de tecnología para jóvenes de la comunidad.")
                .startDate(today.minusMonths(2)).endDate(today.plusMonths(4))
                .status(ProjectStatus.EN_CURSO).responsible("Laura Méndez").build());
        Project green = projects.save(Project.builder()
                .code("PRY-VERDE").name("Semillas de cambio")
                .description("Jornadas de reforestación, educación ambiental y cuidado de espacios públicos.")
                .startDate(today.minusMonths(1)).endDate(today.plusMonths(3))
                .status(ProjectStatus.EN_CURSO).responsible("Laura Méndez").build());
        Project conecta = projects.save(Project.builder()
                .code("PRY-CONECTA").name("Conecta Mayor")
                .description("Alfabetización digital para personas mayores: trámites, comunicación y seguridad en línea.")
                .startDate(today.minusMonths(5)).endDate(today.minusDays(10))
                .status(ProjectStatus.FINALIZADO).responsible("Laura Méndez").build());

        Event robotics = events.save(Event.builder()
                .project(tech).title("Taller de robótica básica")
                .description("Construcción de un robot con sensores y programación por bloques. Se entregan materiales.")
                .location("Laboratorio 204 · Sede Central")
                .startsAt(today.plusDays(1).atTime(15, 0))
                .endsAt(today.plusDays(1).atTime(17, 0))
                .capacity(30).qrToken("SP-ROBOTICA-2026").status(EventStatus.PROGRAMADO)
                .createdBy(coordinator).build());
        Event reforest = events.save(Event.builder()
                .project(green).title("Jornada de reforestación")
                .description("Plantación de 120 árboles nativos. Lleva ropa cómoda y botella de agua.")
                .location("Parque Ecológico Municipal")
                .startsAt(today.plusDays(5).atTime(8, 0))
                .endsAt(today.plusDays(5).atTime(12, 0))
                .capacity(60).qrToken("SP-REVERDE-2026").status(EventStatus.PROGRAMADO)
                .createdBy(coordinator).build());
        Event feria = events.save(Event.builder()
                .project(tech).title("Feria de ciencia y tecnología")
                .description("Muestra de proyectos estudiantiles, retos de programación y charlas abiertas.")
                .location("Plaza de la Innovación")
                .startsAt(today.plusDays(12).atTime(9, 0))
                .endsAt(today.plusDays(12).atTime(13, 0))
                .capacity(120).qrToken("SP-FERIA-2026").status(EventStatus.PROGRAMADO)
                .createdBy(coordinator).build());
        Event digital = events.save(Event.builder()
                .project(conecta).title("Alfabetización digital para adultos")
                .description("Sesión práctica sobre trámites en línea, videollamadas y contraseñas seguras.")
                .location("Centro Comunitario El Nogal")
                .startsAt(today.minusDays(20).atTime(10, 0))
                .endsAt(today.minusDays(20).atTime(12, 0))
                .capacity(40).qrToken("SP-CONECTA-2026").status(EventStatus.FINALIZADO)
                .createdBy(coordinator).build());

        // Registrations
        enroll(robotics, monitor);
        enroll(robotics, student);
        enroll(robotics, valentina);
        enroll(reforest, monitor);
        enroll(reforest, valentina);
        enroll(reforest, camilo);
        enroll(feria, student);
        enroll(feria, camilo);
        enroll(digital, student);
        enroll(digital, monitor);
        enroll(digital, valentina);

        // Past-event attendance
        attendance(digital, student, "QR", "seed-att-digital-estudiante", digital.getStartsAt().plusMinutes(3));
        attendance(digital, monitor, "QR", "seed-att-digital-monitor", digital.getStartsAt().plusMinutes(8));
        attendance(digital, valentina, "NO_INSCRITO", "seed-att-digital-valentina", digital.getStartsAt().plusMinutes(12));
        attendance(digital, null, "MANUAL", "seed-att-digital-invitada", digital.getStartsAt().plusMinutes(15));

        survey(digital, student, 5, 5, 5, true,
                "Muy útil. Aprendí a hacer videollamadas con mi familia.", "seed-survey-digital-estudiante", 4);
        survey(digital, monitor, 4, 5, 4, true,
                "Buen ritmo y acompañamiento cercano.", "seed-survey-digital-monitor", 3);
        survey(digital, valentina, 5, 4, 5, true,
                "Ojalá haya una segunda parte.", "seed-survey-digital-valentina", 2);

        System.out.println("""
                ==================================================================
                 SyncPass demo data ready
                 Coordinator : coordinador / Demo1234
                 Monitor     : monitor     / Demo1234
                 Student     : estudiante  / Demo1234
                 Extra users : valentina, camilo / Demo1234
                ==================================================================
                """);
    }

    private Enrollment enroll(Event event, User user) {
        return enrollments.save(Enrollment.builder()
                .event(event).user(user).status(EnrollmentStatus.INSCRITO).build());
    }

    private void attendance(Event event, User user, String method, String clientId, LocalDateTime at) {
        String name = user != null ? user.getFullName() : "Invitada especial";
        String email = user != null ? user.getEmail() : "invitada@syncpass.demo";
        attendances.save(Attendance.builder()
                .event(event).user(user).participantName(name).participantEmail(email)
                .method(AttendanceMethod.valueOf(method))
                .registeredBy(event.getCreatedBy()).status("PRESENTE")
                .clientId(clientId).registeredAt(at.toInstant(java.time.ZoneOffset.UTC)).build());
    }

    private void survey(Event event, User user, int rating, int organization, int impact,
                        boolean recommend, String comment, String clientId, int daysAgo) {
        surveys.save(SurveyResponse.builder()
                .event(event).user(user).rating(rating).organizationRating(organization)
                .impactRating(impact).wouldRecommend(recommend).comment(comment)
                .clientId(clientId).submittedAt(Instant.now().minus(daysAgo, java.time.temporal.ChronoUnit.DAYS))
                .build());
    }
}
