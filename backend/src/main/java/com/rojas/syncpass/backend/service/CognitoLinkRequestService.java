package com.rojas.syncpass.backend.service;

import com.nimbusds.jwt.JWTClaimsSet;
import com.rojas.syncpass.backend.domain.CognitoLinkRequest;
import com.rojas.syncpass.backend.domain.CognitoLinkRequest.RequestStatus;
import com.rojas.syncpass.backend.domain.User;
import com.rojas.syncpass.backend.domain.UserCognitoLink;
import com.rojas.syncpass.backend.dto.ApiDtos;
import com.rojas.syncpass.backend.dto.ApiDtos.AdminUserCreateRequest;
import com.rojas.syncpass.backend.dto.ApiDtos.CognitoLinkApproveRequest;
import com.rojas.syncpass.backend.dto.ApiDtos.CognitoLinkRejectRequest;
import com.rojas.syncpass.backend.dto.ApiDtos.CognitoLinkRequestDto;
import com.rojas.syncpass.backend.repo.CognitoLinkRequestRepository;
import com.rojas.syncpass.backend.repo.UserCognitoLinkRepository;
import com.rojas.syncpass.backend.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CognitoLinkRequestService {

    private final CognitoLinkRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final UserCognitoLinkRepository linkRepository;
    private final CognitoTokenValidator cognitoTokenValidator;
    private final CurrentUserService currentUserService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public CognitoLinkRequestDto requestAccess(String idToken) {
        JWTClaimsSet claims = cognitoTokenValidator.validate(idToken);
        String sub = claims.getSubject();
        if (sub == null || sub.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token Cognito inválido");
        }

        String email = getStringClaim(claims, "email");
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El token no contiene un correo electrónico");
        }

        if (linkRepository.existsByCognitoSub(sub)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esta identidad Cognito ya está vinculada");
        }

        CognitoLinkRequest existing = requestRepository.findByCognitoSub(sub).orElse(null);
        if (existing != null) {
            if (existing.getStatus() == RequestStatus.PENDING) {
                return toDto(existing);
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe una solicitud procesada para esta identidad");
        }

        String fullName = getStringClaim(claims, "name");
        if (fullName == null) {
            fullName = getStringClaim(claims, "cognito:username");
        }
        if (fullName == null) {
            fullName = "";
        }

        CognitoLinkRequest request = CognitoLinkRequest.builder()
                .cognitoSub(sub)
                .email(email.trim().toLowerCase())
                .fullName(fullName.trim())
                .status(RequestStatus.PENDING)
                .build();

        return toDto(requestRepository.save(request));
    }

    private String getStringClaim(JWTClaimsSet claims, String name) {
        try {
            return claims.getStringClaim(name);
        } catch (Exception e) {
            return null;
        }
    }

    @Transactional(readOnly = true)
    public List<CognitoLinkRequestDto> listPending() {
        return requestRepository.findByStatusOrderByCreatedAtDesc(RequestStatus.PENDING)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public CognitoLinkRequestDto approve(Long requestId, CognitoLinkApproveRequest request) {
        CognitoLinkRequest pending = findPendingRequest(requestId);

        if (linkRepository.existsByCognitoSub(pending.getCognitoSub())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La identidad Cognito ya fue vinculada");
        }

        User targetUser = resolveTargetUser(request);

        if (!targetUser.isActive()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El usuario seleccionado está desactivado");
        }

        if (linkRepository.existsByUser_Id(targetUser.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El usuario seleccionado ya tiene una vinculación Cognito");
        }

        UserCognitoLink link = UserCognitoLink.builder()
                .cognitoSub(pending.getCognitoSub())
                .user(targetUser)
                .build();
        linkRepository.save(link);

        User current = currentUserService.require();
        pending.setStatus(RequestStatus.APPROVED);
        pending.setDecidedAt(Instant.now());
        pending.setDecidedBy(current.getUsername());
        pending.setLinkedUser(targetUser);
        pending.setRejectionReason(null);

        return toDto(requestRepository.save(pending));
    }

    @Transactional
    public CognitoLinkRequestDto reject(Long requestId, CognitoLinkRejectRequest request) {
        CognitoLinkRequest pending = findPendingRequest(requestId);

        User current = currentUserService.require();
        pending.setStatus(RequestStatus.REJECTED);
        pending.setDecidedAt(Instant.now());
        pending.setDecidedBy(current.getUsername());
        pending.setRejectionReason(request.reason());

        return toDto(requestRepository.save(pending));
    }

    private CognitoLinkRequest findPendingRequest(Long requestId) {
        CognitoLinkRequest pending = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitud no encontrada"));
        if (pending.getStatus() != RequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La solicitud ya fue procesada");
        }
        return pending;
    }

    private User resolveTargetUser(CognitoLinkApproveRequest request) {
        if (request.userId() != null) {
            return userRepository.findById(request.userId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        }

        AdminUserCreateRequest newUser = request.newUser();
        if (newUser == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Debe indicar un usuario existente o los datos de un nuevo usuario");
        }

        if (userRepository.existsByUsernameIgnoreCase(newUser.username().trim()) ||
                userRepository.existsByEmailIgnoreCase(newUser.email().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El usuario o correo del nuevo usuario ya existe");
        }

        User user = User.builder()
                .username(newUser.username().trim())
                .email(newUser.email().trim().toLowerCase())
                .fullName(newUser.fullName().trim())
                .role(newUser.role())
                .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                .active(true)
                .build();

        return userRepository.save(user);
    }

    private CognitoLinkRequestDto toDto(CognitoLinkRequest request) {
        User linked = request.getLinkedUser();
        return new CognitoLinkRequestDto(
                request.getId(),
                request.getCognitoSub(),
                request.getEmail(),
                request.getFullName(),
                request.getStatus().name(),
                request.getCreatedAt(),
                request.getDecidedAt(),
                request.getDecidedBy(),
                linked != null ? linked.getId() : null,
                linked != null ? linked.getUsername() : null,
                request.getRejectionReason()
        );
    }
}
