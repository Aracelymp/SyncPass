package com.rojas.syncpass.backend.web;

import com.rojas.syncpass.backend.dto.ApiDtos.*;
import com.rojas.syncpass.backend.service.CurrentUserService;
import com.rojas.syncpass.backend.service.EventService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;
    private final CurrentUserService currentUser;

    public EventController(EventService eventService, CurrentUserService currentUser) {
        this.eventService = eventService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<EventDto> list(@RequestParam(required = false) Long projectId) {
        return eventService.list(projectId);
    }

    @GetMapping("/{id}")
    public EventDto get(@PathVariable Long id) {
        return eventService.get(id);
    }

    @GetMapping("/{id}/detail")
    public EventDetailDto detail(@PathVariable Long id) {
        return eventService.detail(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('DOCENTE_COORDINADOR')")
    public EventDto create(@Valid @RequestBody EventRequest request) {
        return eventService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('DOCENTE_COORDINADOR')")
    public EventDto update(@PathVariable Long id, @Valid @RequestBody EventRequest request) {
        return eventService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('DOCENTE_COORDINADOR')")
    public MessageResponse delete(@PathVariable Long id) {
        return eventService.delete(id);
    }

    @GetMapping("/{id}/participants")
    @PreAuthorize("hasRole('DOCENTE_COORDINADOR')")
    public List<ParticipantDto> participants(@PathVariable Long id) {
        return eventService.participants(id);
    }

    @GetMapping("/{id}/attendance")
    @PreAuthorize("hasRole('DOCENTE_COORDINADOR')")
    public List<AttendanceDto> attendance(@PathVariable Long id) {
        return eventService.attendance(id);
    }

    @PostMapping("/{id}/attendance/scan")
    public AttendanceDto scan(@PathVariable Long id, @Valid @RequestBody ScanRequest request) {
        return eventService.scan(id, request, currentUser.require());
    }

    @PostMapping("/{id}/attendance/walk-in")
    @PreAuthorize("hasAnyRole('DOCENTE_COORDINADOR', 'ESTUDIANTE_VOLUNTARIO_MONITOR')")
    public AttendanceDto walkIn(@PathVariable Long id, @Valid @RequestBody WalkInRequest request) {
        return eventService.walkIn(id, request);
    }

    @PostMapping("/{id}/enroll")
    public EnrollmentDto enroll(@PathVariable Long id) {
        return eventService.enroll(id, currentUser.require());
    }

    @DeleteMapping("/{id}/enroll")
    public MessageResponse cancelEnrollment(@PathVariable Long id) {
        return eventService.cancelEnrollment(id, currentUser.require());
    }

    @PostMapping("/{id}/survey")
    public SurveyDto submitSurvey(@PathVariable Long id, @Valid @RequestBody SurveyRequest request) {
        return eventService.submitSurvey(id, request, currentUser.require());
    }

    @GetMapping("/{id}/survey/summary")
    @PreAuthorize("hasRole('DOCENTE_COORDINADOR')")
    public SurveySummaryDto surveySummary(@PathVariable Long id) {
        return eventService.surveySummary(id);
    }

    @GetMapping("/{id}/surveys")
    @PreAuthorize("hasRole('DOCENTE_COORDINADOR')")
    public List<SurveyDto> surveys(@PathVariable Long id) {
        return eventService.surveys(id);
    }
}
