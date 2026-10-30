package com.rojas.syncpass.backend.service;

import com.rojas.syncpass.backend.domain.*;
import com.rojas.syncpass.backend.dto.ApiDtos.*;
import com.rojas.syncpass.backend.repo.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Service
public class SyncService {

    private final ProjectService projectService;
    private final EventService eventService;
    private final ProjectRepository projects;
    private final EventRepository events;
    private final EnrollmentRepository enrollments;
    private final AttendanceRepository attendances;
    private final SurveyRepository surveys;
    private final CurrentUserService currentUser;
    private final DtoMapper mapper;

    public SyncService(
            ProjectService projectService,
            EventService eventService,
            ProjectRepository projects,
            EventRepository events,
            EnrollmentRepository enrollments,
            AttendanceRepository attendances,
            SurveyRepository surveys,
            CurrentUserService currentUser,
            DtoMapper mapper
    ) {
        this.projectService = projectService;
        this.eventService = eventService;
        this.projects = projects;
        this.events = events;
        this.enrollments = enrollments;
        this.attendances = attendances;
        this.surveys = surveys;
        this.currentUser = currentUser;
        this.mapper = mapper;
    }

    @Transactional
    public SyncBatchResponse batch(SyncBatchRequest request) {
        User me = currentUser.require();
        List<SyncResultDto> results = new ArrayList<>();
        for (SyncOperationDto operation : request.operations()) {
            try {
                results.add(process(operation, me));
            } catch (ResponseStatusException ex) {
                results.add(new SyncResultDto(operation.clientId(), "ERROR",
                        ex.getReason() != null ? ex.getReason() : "No se pudo sincronizar", null));
            } catch (Exception ex) {
                results.add(new SyncResultDto(operation.clientId(), "ERROR",
                        "Error inesperado al sincronizar", null));
            }
        }
        return new SyncBatchResponse(Instant.now(), results);
    }

    private SyncResultDto process(SyncOperationDto op, User me) {
        if (op.clientId() == null || op.clientId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "clientId obligatorio");
        }
        return switch (op.type()) {
            case ATTENDANCE_QR -> processQr(op, me);
            case ATTENDANCE_WALK_IN -> processWalkIn(op, me);
            case SURVEY -> processSurvey(op, me);
        };
    }

    private SyncResultDto processQr(SyncOperationDto op, User me) {
        var existingByClient = attendances.findByClientId(op.clientId());
        if (existingByClient.isPresent()) {
            return new SyncResultDto(op.clientId(), "DUPLICATE", "Asistencia ya sincronizada",
                    existingByClient.get().getId());
        }
        Event event = eventService.find(op.eventId());
        String rawCode = op.qrCode() == null || op.qrCode().isBlank()
                ? event.getQrToken()
                : op.qrCode();
        if (!rawCode.contains(event.getQrToken())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El código QR no corresponde al evento");
        }
        var existing = attendances.findByEventIdAndUserId(event.getId(), me.getId());
        if (existing.isPresent()) {
            return new SyncResultDto(op.clientId(), "DUPLICATE", "La asistencia ya estaba registrada",
                    existing.get().getId());
        }
        Attendance attendance = Attendance.builder()
                .event(event).user(me).participantName(me.getFullName()).participantEmail(me.getEmail())
                .method(AttendanceMethod.QR).registeredBy(me).status("PRESENTE")
                .clientId(op.clientId())
                .registeredAt(op.occurredAt() != null ? op.occurredAt() : Instant.now())
                .build();
        return new SyncResultDto(op.clientId(), "SYNCED", "Asistencia sincronizada",
                attendances.save(attendance).getId());
    }

    private SyncResultDto processWalkIn(SyncOperationDto op, User me) {
        var existingByClient = attendances.findByClientId(op.clientId());
        if (existingByClient.isPresent()) {
            return new SyncResultDto(op.clientId(), "DUPLICATE", "Registro ya sincronizado",
                    existingByClient.get().getId());
        }
        if (op.participantName() == null || op.participantName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nombre del participante obligatorio");
        }
        Event event = eventService.find(op.eventId());
        Attendance attendance = Attendance.builder()
                .event(event).user(null)
                .participantName(op.participantName().trim())
                .participantEmail(blankToNull(op.participantEmail()))
                .participantDocument(blankToNull(op.participantDocument()))
                .method(AttendanceMethod.NO_INSCRITO).registeredBy(me).status("PRESENTE")
                .clientId(op.clientId())
                .registeredAt(op.occurredAt() != null ? op.occurredAt() : Instant.now())
                .build();
        return new SyncResultDto(op.clientId(), "SYNCED", "Participante no inscrito sincronizado",
                attendances.save(attendance).getId());
    }

    private SyncResultDto processSurvey(SyncOperationDto op, User me) {
        var existingByClient = surveys.findByClientId(op.clientId());
        if (existingByClient.isPresent()) {
            return new SyncResultDto(op.clientId(), "DUPLICATE", "Encuesta ya sincronizada",
                    existingByClient.get().getId());
        }

        if (op.attendanceClientId() == null && op.attendanceServerId() == null) {
            return processSelfSurvey(op, me);
        }

        Event event = eventService.find(op.eventId());
        Attendance attendance = resolveSurveyAttendance(op, event.getId());

        if (me.getRole() != Role.DOCENTE_COORDINADOR) {
            boolean registeredByMe = attendance.getRegisteredBy() != null
                    && Objects.equals(attendance.getRegisteredBy().getId(), me.getId());
            if (!registeredByMe) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Solo puedes encuestar asistencias que tú registraste o escaneaste");
            }
        }

        var existingByAttendance = surveys.findByAttendanceId(attendance.getId());
        if (existingByAttendance.isPresent()) {
            return new SyncResultDto(op.clientId(), "DUPLICATE",
                    "La asistencia ya tiene una encuesta registrada",
                    existingByAttendance.get().getId());
        }

        if (attendance.getUser() != null) {
            var existingByUser = surveys.findByEventIdAndUserId(event.getId(), attendance.getUser().getId());
            if (existingByUser.isPresent()) {
                return new SyncResultDto(op.clientId(), "DUPLICATE",
                        "El participante ya respondió la encuesta de este evento",
                        existingByUser.get().getId());
            }
        }

        SurveyResponse response = buildSurvey(op, event, attendance.getUser(), attendance, me);
        return new SyncResultDto(op.clientId(), "SYNCED", "Encuesta sincronizada",
                surveys.save(response).getId());
    }

    private SyncResultDto processSelfSurvey(SyncOperationDto op, User me) {
        Event event = eventService.find(op.eventId());
        var existing = surveys.findByEventIdAndUserId(event.getId(), me.getId());
        if (existing.isPresent()) {
            return new SyncResultDto(op.clientId(), "DUPLICATE", "La encuesta ya estaba registrada",
                    existing.get().getId());
        }
        SurveyResponse response = buildSurvey(op, event, me, null, me);
        return new SyncResultDto(op.clientId(), "SYNCED", "Encuesta sincronizada",
                surveys.save(response).getId());
    }

    private Attendance resolveSurveyAttendance(SyncOperationDto op, Long eventId) {
        if (op.attendanceServerId() != null) {
            Optional<Attendance> byId = attendances.findById(op.attendanceServerId());
            if (byId.isPresent()) {
                Attendance attendance = byId.get();
                if (!Objects.equals(attendance.getEvent().getId(), eventId)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "La asistencia no pertenece al evento indicado");
                }
                return attendance;
            }
        }
        if (op.attendanceClientId() != null) {
            Optional<Attendance> byClient = attendances.findByClientId(op.attendanceClientId());
            if (byClient.isPresent()) {
                Attendance attendance = byClient.get();
                if (!Objects.equals(attendance.getEvent().getId(), eventId)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "La asistencia no pertenece al evento indicado");
                }
                return attendance;
            }
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT,
                "La asistencia aún no está sincronizada. Se reintentará automáticamente.");
    }

    private SurveyResponse buildSurvey(SyncOperationDto op, Event event, User respondent,
                                       Attendance attendance, User collector) {
        return SurveyResponse.builder()
                .event(event)
                .user(respondent)
                .attendance(attendance)
                .collectedBy(collector)
                .rating(clamp(op.rating()))
                .organizationRating(clamp(op.organizationRating()))
                .impactRating(clamp(op.impactRating()))
                .comment(blankToNull(op.comment()))
                .wouldRecommend(Boolean.TRUE.equals(op.wouldRecommend()))
                .clientId(op.clientId())
                .submittedAt(op.occurredAt() != null ? op.occurredAt() : Instant.now())
                .build();
    }

    @Transactional(readOnly = true)
    public SyncPullDto pull() {
        User me = currentUser.require();
        boolean coordinator = me.getRole() == Role.DOCENTE_COORDINADOR;

        List<ProjectDto> projectDtos = projectService.list();
        List<EventDto> eventDtos = events.findAllByOrderByStartsAtAsc().stream().map(mapper::event).toList();
        List<EnrollmentDto> enrollmentDtos = (coordinator
                ? enrollments.findAll()
                : enrollments.findByUserIdOrderByCreatedAtDesc(me.getId()))
                .stream().map(mapper::enrollment).toList();
        List<AttendanceDto> attendanceDtos = (coordinator
                ? attendances.findAll()
                : attendances.findForMonitor(me.getId()))
                .stream().map(mapper::attendance).toList();

        List<SurveyDto> surveyDtos = coordinator
                ? surveys.findAll().stream().map(mapper::survey).toList()
                : List.of();

        List<SurveyReceiptDto> receiptDtos = coordinator
                ? List.of()
                : surveys.findForUserOrCollector(me.getId()).stream().map(mapper::surveyReceipt).toList();

        return new SyncPullDto(Instant.now(), projectDtos, eventDtos, enrollmentDtos,
                attendanceDtos, surveyDtos, receiptDtos);
    }

    private int clamp(Integer value) {
        if (value == null) {
            return 0;
        }
        return Math.max(1, Math.min(5, value));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
