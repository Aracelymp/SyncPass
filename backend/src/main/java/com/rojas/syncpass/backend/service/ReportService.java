package com.rojas.syncpass.backend.service;

import com.rojas.syncpass.backend.domain.*;
import com.rojas.syncpass.backend.dto.ApiDtos.EventStatDto;
import com.rojas.syncpass.backend.dto.ApiDtos.ImpactReportDto;
import com.rojas.syncpass.backend.repo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ReportService {

    private final ProjectRepository projects;
    private final EventRepository events;
    private final EnrollmentRepository enrollments;
    private final AttendanceRepository attendances;
    private final SurveyRepository surveys;

    public ReportService(
            ProjectRepository projects,
            EventRepository events,
            EnrollmentRepository enrollments,
            AttendanceRepository attendances,
            SurveyRepository surveys
    ) {
        this.projects = projects;
        this.events = events;
        this.enrollments = enrollments;
        this.attendances = attendances;
        this.surveys = surveys;
    }

    @Transactional(readOnly = true)
    public ImpactReportDto impact() {
        List<Event> allEvents = events.findAllByOrderByStartsAtAsc();
        List<Attendance> allAttendance = attendances.findAll();

        long totalEnrollments = allEvents.stream()
                .mapToLong(e -> enrollments.countByEventIdAndStatus(e.getId(), EnrollmentStatus.INSCRITO))
                .sum();
        long totalAttendances = allAttendance.size();
        Set<String> unique = new HashSet<>();
        for (Attendance a : allAttendance) {
            unique.add(a.getUser() != null ? "u:" + a.getUser().getId()
                    : "g:" + a.getParticipantName() + ":" + a.getParticipantEmail());
        }
        double attendanceRate = totalEnrollments == 0
                ? 0
                : Math.min(100.0, totalAttendances * 100.0 / totalEnrollments);

        List<EventStatDto> stats = allEvents.stream().map(event -> {
            long enrolled = enrollments.countByEventIdAndStatus(event.getId(), EnrollmentStatus.INSCRITO);
            long attended = attendances.countByEventId(event.getId());
            double rate = enrolled == 0 ? (attended > 0 ? 100.0 : 0.0)
                    : Math.min(100.0, attended * 100.0 / enrolled);
            Double avg = surveys.averageRatingByEvent(event.getId());
            return new EventStatDto(
                    event.getId(), event.getTitle(), event.getProject().getName(),
                    event.getStartsAt(), enrolled, attended, DtoMapper.round(rate),
                    DtoMapper.round(avg == null ? 0 : avg)
            );
        }).toList();

        Double avgSatisfaction = surveys.averageRating();
        List<SurveyResponse> allSurveys = surveys.findAll();
        double recommendRate = allSurveys.isEmpty()
                ? 0
                : allSurveys.stream().filter(SurveyResponse::isWouldRecommend).count() * 100.0 / allSurveys.size();

        return new ImpactReportDto(
                Instant.now(), projects.count(), allEvents.size(), totalEnrollments,
                totalAttendances, unique.size(), DtoMapper.round(attendanceRate),
                DtoMapper.round(avgSatisfaction == null ? 0 : avgSatisfaction),
                DtoMapper.round(recommendRate), stats
        );
    }
}
