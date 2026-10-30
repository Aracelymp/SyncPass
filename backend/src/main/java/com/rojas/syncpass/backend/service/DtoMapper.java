package com.rojas.syncpass.backend.service;

import com.rojas.syncpass.backend.domain.*;
import com.rojas.syncpass.backend.dto.ApiDtos.*;
import com.rojas.syncpass.backend.repo.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DtoMapper {

    private final EnrollmentRepository enrollments;
    private final AttendanceRepository attendances;
    private final SurveyRepository surveys;
    private final CurrentUserService currentUser;

    public DtoMapper(
            EnrollmentRepository enrollments,
            AttendanceRepository attendances,
            SurveyRepository surveys,
            CurrentUserService currentUser
    ) {
        this.enrollments = enrollments;
        this.attendances = attendances;
        this.surveys = surveys;
        this.currentUser = currentUser;
    }

    public UserDto user(User user) {
        return new UserDto(user.getId(), user.getUsername(), user.getEmail(),
                user.getFullName(), user.getRole());
    }

    public ProjectDto project(Project project, long eventCount) {
        return new ProjectDto(
                project.getId(), project.getCode(), project.getName(), project.getDescription(),
                project.getStartDate(), project.getEndDate(), project.getStatus(),
                project.getResponsible(), eventCount, project.getCreatedAt()
        );
    }

    public EventDto event(Event event) {
        long enrolled = enrollments.countByEventIdAndStatus(event.getId(), EnrollmentStatus.INSCRITO);
        long attended = attendances.countByEventId(event.getId());
        return new EventDto(
                event.getId(), event.getProject().getId(), event.getProject().getName(),
                event.getTitle(), event.getDescription(), event.getLocation(),
                event.getStartsAt(), event.getEndsAt(), event.getCapacity(), event.getQrToken(),
                event.getStatus(), enrolled, attended,
                currentUser.isCoordinator() ? surveys.averageRatingByEvent(event.getId()) : null
        );
    }

    public EnrollmentDto enrollment(Enrollment enrollment) {
        return new EnrollmentDto(
                enrollment.getId(), enrollment.getEvent().getId(), enrollment.getUser().getId(),
                enrollment.getUser().getFullName(), enrollment.getStatus(), enrollment.getCreatedAt()
        );
    }

    public AttendanceDto attendance(Attendance attendance) {
        return new AttendanceDto(
                attendance.getId(), attendance.getEvent().getId(), attendance.getEvent().getTitle(),
                attendance.getUser() != null ? attendance.getUser().getId() : null,
                attendance.getParticipantName(), attendance.getParticipantEmail(),
                attendance.getParticipantDocument(), attendance.getMethod(), attendance.getStatus(),
                attendance.getRegisteredAt(),
                attendance.getRegisteredBy() != null ? attendance.getRegisteredBy().getId() : null,
                attendance.getRegisteredBy() != null ? attendance.getRegisteredBy().getFullName() : null,
                attendance.getClientId()
        );
    }

    public SurveyDto survey(SurveyResponse response) {
        Attendance attendance = response.getAttendance();
        String participantName = response.getUser() != null
                ? response.getUser().getFullName()
                : attendance != null ? attendance.getParticipantName() : "Participante";
        return new SurveyDto(
                response.getId(), response.getEvent().getId(),
                response.getUser() != null ? response.getUser().getId() : null,
                attendance != null ? attendance.getId() : null,
                attendance != null ? attendance.getClientId() : null,
                response.getCollectedBy() != null ? response.getCollectedBy().getId() : null,
                response.getCollectedBy() != null ? response.getCollectedBy().getFullName() : null,
                participantName,
                response.getRating(), response.getOrganizationRating(), response.getImpactRating(),
                response.getComment(), response.isWouldRecommend(), response.getSubmittedAt(),
                response.getClientId()
        );
    }

    public SurveyReceiptDto surveyReceipt(SurveyResponse response) {
        Attendance attendance = response.getAttendance();
        return new SurveyReceiptDto(
                response.getId(),
                response.getEvent().getId(),
                attendance != null ? attendance.getId() : null,
                attendance != null ? attendance.getClientId() : null,
                response.getSubmittedAt()
        );
    }

    public SurveySummaryDto surveySummary(Event event) {
        List<SurveyResponse> responses = surveys.findByEventIdOrderBySubmittedAtDesc(event.getId());
        if (responses.isEmpty()) {
            return new SurveySummaryDto(event.getId(), 0, 0, 0, 0, 0, List.of());
        }
        double avg = responses.stream().mapToInt(SurveyResponse::getRating).average().orElse(0);
        double org = responses.stream().mapToInt(SurveyResponse::getOrganizationRating).average().orElse(0);
        double impact = responses.stream().mapToInt(SurveyResponse::getImpactRating).average().orElse(0);
        double recommend = responses.stream().filter(SurveyResponse::isWouldRecommend).count()
                * 100.0 / responses.size();
        List<String> comments = responses.stream()
                .map(SurveyResponse::getComment)
                .filter(c -> c != null && !c.isBlank())
                .limit(10)
                .toList();
        return new SurveySummaryDto(event.getId(), responses.size(),
                round(avg), round(org), round(impact), round(recommend), comments);
    }

    public static double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
