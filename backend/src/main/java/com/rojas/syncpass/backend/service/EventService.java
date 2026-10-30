package com.rojas.syncpass.backend.service;

import com.rojas.syncpass.backend.domain.*;
import com.rojas.syncpass.backend.dto.ApiDtos.*;
import com.rojas.syncpass.backend.repo.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class EventService {

    private final EventRepository events;
    private final ProjectRepository projects;
    private final EnrollmentRepository enrollments;
    private final AttendanceRepository attendances;
    private final SurveyRepository surveys;
    private final CurrentUserService currentUser;
    private final DtoMapper mapper;

    public EventService(
            EventRepository events,
            ProjectRepository projects,
            EnrollmentRepository enrollments,
            AttendanceRepository attendances,
            SurveyRepository surveys,
            CurrentUserService currentUser,
            DtoMapper mapper
    ) {
        this.events = events;
        this.projects = projects;
        this.enrollments = enrollments;
        this.attendances = attendances;
        this.surveys = surveys;
        this.currentUser = currentUser;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<EventDto> list(Long projectId) {
        List<Event> result = projectId == null
                ? events.findAllByOrderByStartsAtAsc()
                : events.findByProjectIdOrderByStartsAtAsc(projectId);
        return result.stream().map(mapper::event).toList();
    }

    @Transactional(readOnly = true)
    public EventDto get(Long id) {
        return mapper.event(find(id));
    }

    @Transactional(readOnly = true)
    public EventDetailDto detail(Long id) {
        Event event = find(id);
        User me = currentUser.require();
        boolean enrolled = enrollments.findByEventIdAndUserId(id, me.getId())
                .map(e -> e.getStatus() == EnrollmentStatus.INSCRITO).orElse(false);
        boolean attended = attendances.findByEventIdAndUserId(id, me.getId()).isPresent();
        boolean surveyCompleted = surveys.findByEventIdAndUserId(id, me.getId()).isPresent();
        boolean coordinator = me.getRole() == Role.DOCENTE_COORDINADOR;
        List<ParticipantDto> visibleParticipants = coordinator ? participants(id) : List.of();
        return new EventDetailDto(
                mapper.event(event), visibleParticipants,
                coordinator ? mapper.surveySummary(event) : null,
                enrolled, attended, surveyCompleted
        );
    }

    @Transactional
    public EventDto create(EventRequest request) {
        Project project = projects.findById(request.projectId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado"));
        validateDates(request.startsAt(), request.endsAt());
        String token = normalizeToken(request.qrToken());
        if (token == null) {
            token = generateToken();
        } else if (events.existsByQrToken(token)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El código QR ya está en uso");
        }
        User creator = currentUser.require();
        Event event = Event.builder()
                .project(project)
                .title(request.title().trim())
                .description(request.description().trim())
                .location(request.location().trim())
                .startsAt(request.startsAt())
                .endsAt(request.endsAt())
                .capacity(request.capacity())
                .qrToken(token)
                .status(request.status() != null ? request.status() : EventStatus.PROGRAMADO)
                .createdBy(creator)
                .build();
        return mapper.event(events.save(event));
    }

    @Transactional
    public EventDto update(Long id, EventRequest request) {
        Event event = find(id);
        Project project = projects.findById(request.projectId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado"));
        validateDates(request.startsAt(), request.endsAt());
        String token = normalizeToken(request.qrToken());
        if (token != null && !token.equals(event.getQrToken()) && events.existsByQrToken(token)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El código QR ya está en uso");
        }
        event.setProject(project);
        event.setTitle(request.title().trim());
        event.setDescription(request.description().trim());
        event.setLocation(request.location().trim());
        event.setStartsAt(request.startsAt());
        event.setEndsAt(request.endsAt());
        event.setCapacity(request.capacity());
        event.setQrToken(token != null ? token : event.getQrToken());
        event.setStatus(request.status() != null ? request.status() : event.getStatus());
        return mapper.event(events.save(event));
    }

    @Transactional
    public MessageResponse delete(Long id) {
        Event event = find(id);
        long enrollmentCount = enrollments.countByEventId(id);
        long attendanceCount = attendances.countByEventId(id);
        long surveyCount = surveys.countByEventId(id);
        if (enrollmentCount + attendanceCount + surveyCount > 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede eliminar el evento porque tiene registros asociados: "
                            + enrollmentCount + " inscripción(es), "
                            + attendanceCount + " asistencia(s) y "
                            + surveyCount + " encuesta(s)."
            );
        }
        events.delete(event);
        return new MessageResponse("Evento eliminado");
    }

    @Transactional(readOnly = true)
    public List<AttendanceDto> attendance(Long eventId) {
        find(eventId);
        return attendances.findByEventIdOrderByRegisteredAtDesc(eventId).stream()
                .map(mapper::attendance).toList();
    }

    @Transactional(readOnly = true)
    public List<ParticipantDto> participants(Long eventId) {
        Event event = find(eventId);
        Map<Long, Attendance> attendanceByUser = attendances.findByEventIdOrderByRegisteredAtDesc(eventId)
                .stream()
                .filter(a -> a.getUser() != null)
                .collect(Collectors.toMap(a -> a.getUser().getId(), Function.identity(), (a, b) -> a));

        List<ParticipantDto> result = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (Enrollment enrollment : enrollments.findByEventIdOrderByCreatedAtAsc(eventId)) {
            if (enrollment.getStatus() != EnrollmentStatus.INSCRITO) {
                continue;
            }
            User user = enrollment.getUser();
            Attendance attendance = attendanceByUser.get(user.getId());
            seen.add(user.getId());
            result.add(new ParticipantDto(
                    user.getId(), user.getFullName(), user.getUsername(), user.getEmail(),
                    true, attendance != null,
                    attendance != null ? attendance.getMethod().name() : null,
                    attendance != null ? attendance.getRegisteredAt().toString() : null
            ));
        }
        // Walk-ins and attendees that were not previously enrolled.
        for (Attendance attendance : attendanceByUser.values()) {
            if (!seen.contains(attendance.getUser().getId())) {
                User user = attendance.getUser();
                result.add(new ParticipantDto(
                        user.getId(), user.getFullName(), user.getUsername(), user.getEmail(),
                        false, true, attendance.getMethod().name(),
                        attendance.getRegisteredAt().toString()
                ));
            }
        }
        // Anonymous walk-ins.
        for (Attendance attendance : attendances.findByEventIdOrderByRegisteredAtDesc(eventId)) {
            if (attendance.getUser() == null) {
                result.add(new ParticipantDto(
                        null, attendance.getParticipantName(), null, attendance.getParticipantEmail(),
                        false, true, attendance.getMethod().name(),
                        attendance.getRegisteredAt().toString()
                ));
            }
        }
        return result;
    }

    @Transactional
    public AttendanceDto scan(Long eventId, ScanRequest request, User user) {
        Attendance existing = byScanCode(request.qrCode(), user);
        if (existing != null) {
            return mapper.attendance(existing);
        }
        Event event = resolveQr(request.qrCode());
        if (!event.getId().equals(eventId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El código QR no corresponde al evento seleccionado");
        }
        Attendance attendance = Attendance.builder()
                .event(event)
                .user(user)
                .participantName(user.getFullName())
                .participantEmail(user.getEmail())
                .method(AttendanceMethod.QR)
                .registeredBy(user)
                .status("PRESENTE")
                .clientId(UUID.randomUUID().toString())
                .registeredAt(Instant.now())
                .build();
        return mapper.attendance(attendances.save(attendance));
    }

    @Transactional
    public AttendanceDto walkIn(Long eventId, WalkInRequest request) {
        Event event = find(eventId);
        User registrar = currentUser.require();
        Attendance attendance = Attendance.builder()
                .event(event)
                .user(null)
                .participantName(request.participantName().trim())
                .participantEmail(blankToNull(request.participantEmail()))
                .participantDocument(blankToNull(request.participantDocument()))
                .method(AttendanceMethod.NO_INSCRITO)
                .registeredBy(registrar)
                .status("PRESENTE")
                .clientId(UUID.randomUUID().toString())
                .registeredAt(Instant.now())
                .build();
        return mapper.attendance(attendances.save(attendance));
    }

    @Transactional
    public EnrollmentDto enroll(Long eventId, User user) {
        Event event = find(eventId);
        Optional<Enrollment> existing = enrollments.findByEventIdAndUserId(eventId, user.getId());
        if (existing.isPresent()) {
            Enrollment enrollment = existing.get();
            if (enrollment.getStatus() == EnrollmentStatus.INSCRITO) {
                return mapper.enrollment(enrollment);
            }
            enrollment.setStatus(EnrollmentStatus.INSCRITO);
            return mapper.enrollment(enrollments.save(enrollment));
        }
        long count = enrollments.countByEventIdAndStatus(eventId, EnrollmentStatus.INSCRITO);
        if (count >= event.getCapacity()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El evento alcanzó su cupo máximo");
        }
        Enrollment enrollment = Enrollment.builder()
                .event(event).user(user).status(EnrollmentStatus.INSCRITO).build();
        return mapper.enrollment(enrollments.save(enrollment));
    }

    @Transactional
    public MessageResponse cancelEnrollment(Long eventId, User user) {
        Enrollment enrollment = enrollments.findByEventIdAndUserId(eventId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No estás inscrito en este evento"));
        enrollment.setStatus(EnrollmentStatus.CANCELADO);
        enrollments.save(enrollment);
        return new MessageResponse("Inscripción cancelada");
    }

    @Transactional
    public SurveyDto submitSurvey(Long eventId, SurveyRequest request, User user) {
        Event event = find(eventId);
        if (surveys.findByEventIdAndUserId(eventId, user.getId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya enviaste la encuesta de este evento");
        }
        SurveyResponse response = SurveyResponse.builder()
                .event(event).user(user).collectedBy(user)
                .rating(request.rating()).organizationRating(request.organizationRating())
                .impactRating(request.impactRating()).comment(blankToNull(request.comment()))
                .wouldRecommend(request.wouldRecommend())
                .clientId(UUID.randomUUID().toString())
                .submittedAt(Instant.now())
                .build();
        return mapper.survey(surveys.save(response));
    }

    @Transactional(readOnly = true)
    public SurveySummaryDto surveySummary(Long eventId) {
        return mapper.surveySummary(find(eventId));
    }

    @Transactional(readOnly = true)
    public List<SurveyDto> surveys(Long eventId) {
        find(eventId);
        return surveys.findByEventIdOrderBySubmittedAtDesc(eventId).stream().map(mapper::survey).toList();
    }

    public Event find(Long id) {
        return events.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento no encontrado"));
    }

    public Event resolveQr(String rawCode) {
        String code = rawCode == null ? "" : rawCode.trim();
        if (code.startsWith("SYNCPASS|EVENT|")) {
            String[] parts = code.split("\\|");
            if (parts.length < 4) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código QR inválido");
            }
            try {
                Long eventId = Long.parseLong(parts[2]);
                String token = parts[3];
                Event event = find(eventId);
                if (!event.getQrToken().equals(token)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El código QR no corresponde al evento");
                }
                return event;
            } catch (NumberFormatException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código QR inválido");
            }
        }
        return events.findByQrToken(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Código QR no reconocido"));
    }

    private Attendance byScanCode(String rawCode, User user) {
        Event event = resolveQr(rawCode);
        return attendances.findByEventIdAndUserId(event.getId(), user.getId()).orElse(null);
    }

    private String normalizeToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        return token.trim().toUpperCase(Locale.ROOT);
    }

    private String generateToken() {
        String token;
        do {
            token = "SP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        } while (events.existsByQrToken(token));
        return token;
    }

    private void validateDates(LocalDateTime start, LocalDateTime end) {
        if (end.isBefore(start) || end.isEqual(start)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha de fin debe ser posterior al inicio");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
