package com.rojas.syncpass.backend.web;

import com.rojas.syncpass.backend.dto.ApiDtos.AdminUserCreateRequest;
import com.rojas.syncpass.backend.dto.ApiDtos.AdminUserDto;
import com.rojas.syncpass.backend.dto.ApiDtos.UserActiveRequest;
import com.rojas.syncpass.backend.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('DOCENTE_COORDINADOR')")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public List<AdminUserDto> list() {
        return adminUserService.listUsers();
    }

    @PostMapping
    public AdminUserDto create(@Valid @RequestBody AdminUserCreateRequest request) {
        return adminUserService.createUser(request);
    }

    @PatchMapping("/{id}/active")
    public AdminUserDto setActive(@PathVariable Long id, @Valid @RequestBody UserActiveRequest request) {
        return adminUserService.setActive(id, request.active());
    }
}
