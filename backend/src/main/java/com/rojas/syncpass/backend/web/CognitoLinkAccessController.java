package com.rojas.syncpass.backend.web;

import com.rojas.syncpass.backend.dto.ApiDtos.CognitoLinkAccessRequest;
import com.rojas.syncpass.backend.dto.ApiDtos.CognitoLinkRequestDto;
import com.rojas.syncpass.backend.service.CognitoLinkRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/cognito")
@RequiredArgsConstructor
public class CognitoLinkAccessController {

    private final CognitoLinkRequestService linkRequestService;

    @PostMapping("/link-request")
    public CognitoLinkRequestDto requestAccess(@Valid @RequestBody CognitoLinkAccessRequest request) {
        return linkRequestService.requestAccess(request.idToken());
    }
}
