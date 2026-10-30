package com.rojas.syncpass.backend.dto;

import com.rojas.syncpass.backend.domain.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * All API contracts live here as nested records so the JSON shape is easy to audit.
 * The Android client mirrors these names (camelCase).
 */
public final class ApiDtos {

    private ApiDtos() {
    }

    // ---------------------------------------------------------------- auth

    public record LoginRequest(
            @NotBlank(message = "El usuario o correo es obligatorio") String username,
            @NotBlank(message = "La contraseña es obligatoria") String password
    ) {
    }

    public record UserDto(
            Long id,
            String username,
            String email,
            String fullName,
            Role role
    ) {
    }

    public record LoginResponse(
            String accessToken,
            String tokenType,
            long expiresIn,
            UserDto user
    ) {
    }

    // ------------------------------------------------------------ admin users

    public record AdminUserDto(
            Long id,
            String username,
            String email,
            String fullName,
            Role role,
            boolean active,
            boolean cognitoLinked
    ) {
    }

    public record AdminUserCreateRequest(
            @NotBlank(message = "El usuario es obligatorio")
            @Size(max = 80, message = "El usuario no puede superar 80 caracteres")
            String username,

            @NotBlank(message = "El correo es obligatorio")
            @Email(message = "El correo no es válido")
            @Size(max = 160, message = "El correo no puede superar 160 caracteres")
            String email,

            @NotBlank(message = "El nombre completo es obligatorio")
            @Size(max = 160, message = "El nombre no puede superar 160 caracteres")
            String fullName,

            @NotNull(message = "El rol es obligatorio")
            Role role
    ) {
    }

    public record UserActiveRequest(
            @NotNull(message = "El estado activo es obligatorio")
            Boolean active
    ) {
    }

    // ------------------------------------------------------- cognito link requests

    public record CognitoLinkRequestDto(
            Long id,
            String cognitoSub,
            String email,
            String fullName,
            String status,
            Instant createdAt,
            Instant decidedAt,
            String decidedBy,
            Long linkedUserId,
            String linkedUsername,
            String rejectionReason
    ) {
    }

    public record CognitoLinkAccessRequest(
            @NotBlank(message = "El token de Cognito es obligatorio")
            String idToken
    ) {
    }

    public record CognitoLinkApproveRequest(
            Long userId,

            @Valid
            AdminUserCreateRequest newUser
    ) {
    }

    public record CognitoLinkRejectRequest(
            @Size(max = 500, message = "El motivo no puede superar 500 caracteres")
            String reason
    ) {
    }

    // ------------------------------------------------------------ projects

    public record ProjectRequest(
            @NotBlank(message = "El código es obligatorio") @Size(max = 40) String code,
            @NotBlank(message = "El nombre es obligatorio") @Size(max = 160) String name,
            @NotBlank(message = "La descripción es obligatoria") @Size(max = 1200) String description,
            @NotNull(message = "La fecha de inicio es obligatoria") LocalDate startDate,
            @NotNull(message = "La fecha de fin es obligatoria") LocalDate endDate,
            ProjectStatus status,
            @NotBlank(message = "El responsable es obligatorio") @Size(max = 160) String responsible
    ) {
    }

    public record ProjectDto(
            Long id,
            String code,
            String name,
            String description,
            LocalDate startDate,
            LocalDate endDate,
            ProjectStatus status,
            String responsible,
            long eventCount,
            Instant createdAt
    ) {
    }

    // -------------------------------------------------------------- events

    public record EventRequest(
            @NotNull(message = "El proyecto es obligatorio") Long projectId,
            @NotBlank(message = "El título es obligatorio") @Size(max = 180) String title,
            @NotBlank(message = "La descripción es obligatoria") @Size(max = 1200) String description,
            @NotBlank(message = "La ubicación es obligatoria") @Size(max = 200) String location,
            @NotNull(message = "La fecha de inicio es obligatoria") LocalDateTime startsAt,
            @NotNull(message = "La fecha de fin es obligatoria") LocalDateTime endsAt,
            @Positive(message = "El cupo debe ser mayor a cero") int capacity,
            @Size(max = 120) String qrToken,
            EventStatus status
    ) {
    }

    public record EventDto(
            Long id,
            Long projectId,
            String projectName,
            String title,
            String description,
            String location,
            LocalDateTime startsAt,
            LocalDateTime endsAt,
            int capacity,
            String qrToken,
            EventStatus status,
            long enrolledCount,
            long attendedCount,
            Double averageRating
    ) {
    }

    public record EnrollmentDto(
            Long id,
            Long eventId,
            Long userId,
            String userName,
            EnrollmentStatus status,
            Instant createdAt
    ) {
    }

    public record ParticipantDto(
            Long userId,
            String fullName,
            String username,
            String email,
            boolean enrolled,
            boolean attended,
            String method,
            String registeredAt
    ) {
    }

    public record EventDetailDto(
            EventDto event,
            List<ParticipantDto> participants,
            SurveySummaryDto surveySummary,
            boolean enrolled,
            boolean attended,
            boolean surveyCompleted
    ) {
    }

    // ---------------------------------------------------------- attendance

    public record ScanRequest(
            @NotBlank(message = "El código QR es obligatorio") String qrCode
    ) {
    }

    public record WalkInRequest(
            @NotBlank(message = "El nombre del participante es obligatorio") @Size(max = 180) String participantName,
            @Email(message = "Correo inválido") @Size(max = 160) String participantEmail,
            @Size(max = 60) String participantDocument
    ) {
    }

    public record AttendanceDto(
            Long id,
            Long eventId,
            String eventTitle,
            Long userId,
            String participantName,
            String participantEmail,
            String participantDocument,
            AttendanceMethod method,
            String status,
            Instant registeredAt,
            Long registeredByUserId,
            String registeredByName,
            String clientId
    ) {
    }

    // -------------------------------------------------------------- surveys

    public record SurveyRequest(
            @NotNull(message = "La calificación general es obligatoria")
            @Min(value = 1, message = "La calificación debe estar entre 1 y 5")
            @Max(value = 5, message = "La calificación debe estar entre 1 y 5") Integer rating,
            @NotNull(message = "La calificación de organización es obligatoria")
            @Min(1) @Max(5) Integer organizationRating,
            @NotNull(message = "La calificación de impacto es obligatoria")
            @Min(1) @Max(5) Integer impactRating,
            @Size(max = 1000, message = "El comentario no puede superar 1000 caracteres") String comment,
            boolean wouldRecommend
    ) {
    }

    public record SurveyDto(
            Long id,
            Long eventId,
            Long userId,
            Long attendanceId,
            String attendanceClientId,
            Long collectedByUserId,
            String collectedByName,
            String participantName,
            int rating,
            int organizationRating,
            int impactRating,
            String comment,
            boolean wouldRecommend,
            Instant submittedAt,
            String clientId
    ) {
    }

    /**
     * Minimal survey state for monitors. It intentionally excludes ratings,
     * comments and respondent identity so the mobile client can mark an
     * attendance as already surveyed without downloading survey content.
     */
    public record SurveyReceiptDto(
            Long id,
            Long eventId,
            Long attendanceId,
            String attendanceClientId,
            Instant submittedAt
    ) {
    }

    public record SurveySummaryDto(
            Long eventId,
            long responseCount,
            double averageRating,
            double averageOrganization,
            double averageImpact,
            double recommendRate,
            List<String> comments
    ) {
    }

    // -------------------------------------------------------------- reports

    public record EventStatDto(
            Long eventId,
            String title,
            String projectName,
            LocalDateTime startsAt,
            long enrolled,
            long attended,
            double attendanceRate,
            double averageRating
    ) {
    }

    public record ImpactReportDto(
            Instant generatedAt,
            long totalProjects,
            long totalEvents,
            long totalEnrollments,
            long totalAttendances,
            long uniqueParticipants,
            double attendanceRate,
            double averageSatisfaction,
            double recommendRate,
            List<EventStatDto> events
    ) {
    }

    // ----------------------------------------------------------------- sync

    public record SyncOperationDto(
            @NotBlank String clientId,
            @NotNull SyncOperationType type,
            @NotNull Long eventId,
            String attendanceClientId,
            Long attendanceServerId,
            String qrCode,
            String participantName,
            String participantEmail,
            String participantDocument,
            Integer rating,
            Integer organizationRating,
            Integer impactRating,
            String comment,
            Boolean wouldRecommend,
            Instant occurredAt
    ) {
    }

    public record SyncBatchRequest(
            @NotEmpty(message = "El lote no puede estar vacío") List<SyncOperationDto> operations
    ) {
    }

    public record SyncResultDto(
            String clientId,
            String status,
            String message,
            Long serverId
    ) {
    }

    public record SyncBatchResponse(
            Instant serverTime,
            List<SyncResultDto> results
    ) {
    }

    public record SyncPullDto(
            Instant serverTime,
            List<ProjectDto> projects,
            List<EventDto> events,
            List<EnrollmentDto> enrollments,
            List<AttendanceDto> attendances,
            List<SurveyDto> surveys,
            List<SurveyReceiptDto> surveyReceipts
    ) {
    }

    // --------------------------------------------------------------- misc

    public record MessageResponse(String message) {
    }

    public record ApiError(
            Instant timestamp,
            int status,
            String error,
            String message
    ) {
    }
}
