package com.rojas.syncpass.backend.web;

import com.rojas.syncpass.backend.dto.ApiDtos.MessageResponse;
import com.rojas.syncpass.backend.dto.ApiDtos.ProjectDto;
import com.rojas.syncpass.backend.dto.ApiDtos.ProjectRequest;
import com.rojas.syncpass.backend.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public List<ProjectDto> list() {
        return projectService.list();
    }

    @GetMapping("/{id}")
    public ProjectDto get(@PathVariable Long id) {
        return projectService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('DOCENTE_COORDINADOR')")
    public ProjectDto create(@Valid @RequestBody ProjectRequest request) {
        return projectService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('DOCENTE_COORDINADOR')")
    public ProjectDto update(@PathVariable Long id, @Valid @RequestBody ProjectRequest request) {
        return projectService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('DOCENTE_COORDINADOR')")
    public MessageResponse delete(@PathVariable Long id) {
        return projectService.delete(id);
    }
}
