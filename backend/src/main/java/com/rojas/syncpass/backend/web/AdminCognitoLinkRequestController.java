package com.rojas.syncpass.backend.web;

import com.rojas.syncpass.backend.dto.ApiDtos.CognitoLinkApproveRequest;
import com.rojas.syncpass.backend.dto.ApiDtos.CognitoLinkRejectRequest;
import com.rojas.syncpass.backend.dto.ApiDtos.CognitoLinkRequestDto;
import com.rojas.syncpass.backend.service.CognitoLinkRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/cognito-link-requests")
@PreAuthorize("hasRole('DOCENTE_COORDINADOR')")
@RequiredArgsConstructor
public class AdminCognitoLinkRequestController {

    private final CognitoLinkRequestService linkRequestService;

    @GetMapping
    public List<CognitoLinkRequestDto> listPending() {
        return linkRequestService.listPending();
    }

    @PostMapping("/{id}/approve")
    public CognitoLinkRequestDto approve(
            @PathVariable Long id,
            @Valid @RequestBody CognitoLinkApproveRequest request
    ) {
        return linkRequestService.approve(id, request);
    }

    @PostMapping("/{id}/reject")
    public CognitoLinkRequestDto reject(
            @PathVariable Long id,
            @Valid @RequestBody CognitoLinkRejectRequest request
    ) {
        return linkRequestService.reject(id, request);
    }
}
