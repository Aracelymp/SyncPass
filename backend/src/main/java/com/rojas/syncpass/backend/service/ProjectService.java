package com.rojas.syncpass.backend.service;

import com.rojas.syncpass.backend.domain.Project;
import com.rojas.syncpass.backend.domain.ProjectStatus;
import com.rojas.syncpass.backend.dto.ApiDtos.MessageResponse;
import com.rojas.syncpass.backend.dto.ApiDtos.ProjectDto;
import com.rojas.syncpass.backend.dto.ApiDtos.ProjectRequest;
import com.rojas.syncpass.backend.repo.EventRepository;
import com.rojas.syncpass.backend.repo.ProjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
public class ProjectService {

    private final ProjectRepository projects;
    private final EventRepository events;
    private final DtoMapper mapper;

    public ProjectService(ProjectRepository projects, EventRepository events, DtoMapper mapper) {
        this.projects = projects;
        this.events = events;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<ProjectDto> list() {
        return projects.findAllByOrderByStartDateDesc().stream()
                .map(p -> mapper.project(p, events.countByProjectId(p.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectDto get(Long id) {
        Project project = find(id);
        return mapper.project(project, events.countByProjectId(id));
    }

    @Transactional
    public ProjectDto create(ProjectRequest request) {
        String code = normalizeCode(request);
        if (projects.existsByCodeIgnoreCase(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un proyecto con ese código");
        }
        validateDates(request);
        Project project = Project.builder()
                .code(code)
                .name(request.name().trim())
                .description(request.description().trim())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .status(request.status() != null ? request.status() : ProjectStatus.PLANIFICADO)
                .responsible(request.responsible().trim())
                .build();
        return mapper.project(projects.save(project), 0);
    }

    @Transactional
    public ProjectDto update(Long id, ProjectRequest request) {
        Project project = find(id);
        String code = normalizeCode(request);
        if (projects.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un proyecto con ese código");
        }
        validateDates(request);
        project.setCode(code);
        project.setName(request.name().trim());
        project.setDescription(request.description().trim());
        project.setStartDate(request.startDate());
        project.setEndDate(request.endDate());
        project.setStatus(request.status() != null ? request.status() : project.getStatus());
        project.setResponsible(request.responsible().trim());
        return mapper.project(projects.save(project), events.countByProjectId(id));
    }

    @Transactional
    public MessageResponse delete(Long id) {
        Project project = find(id);
        // Attendance, enrollment and survey rows depend on events. Blocking any
        // project with events also protects those dependent records.
        long eventCount = events.countByProjectId(id);
        if (eventCount > 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede eliminar el proyecto porque tiene " + eventCount
                            + " evento(s) asociado(s). Elimina o reasigna los eventos primero."
            );
        }
        projects.delete(project);
        return new MessageResponse("Proyecto eliminado");
    }

    private String normalizeCode(ProjectRequest request) {
        return request.code().trim().toUpperCase(Locale.ROOT);
    }

    private void validateDates(ProjectRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La fecha de fin no puede ser anterior al inicio"
            );
        }
    }

    public Project find(Long id) {
        return projects.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado"));
    }
}
