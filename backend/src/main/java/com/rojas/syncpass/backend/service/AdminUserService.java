package com.rojas.syncpass.backend.service;

import com.rojas.syncpass.backend.domain.Role;
import com.rojas.syncpass.backend.domain.User;
import com.rojas.syncpass.backend.dto.ApiDtos;
import com.rojas.syncpass.backend.dto.ApiDtos.AdminUserCreateRequest;
import com.rojas.syncpass.backend.dto.ApiDtos.AdminUserDto;
import com.rojas.syncpass.backend.repo.UserCognitoLinkRepository;
import com.rojas.syncpass.backend.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final UserCognitoLinkRepository linkRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<AdminUserDto> listUsers() {
        List<User> users = userRepository.findAll();
        Set<Long> linkedUserIds = linkRepository.findAll().stream()
                .map(link -> link.getUser().getId())
                .collect(Collectors.toSet());

        return users.stream()
                .map(user -> toDto(user, linkedUserIds.contains(user.getId())))
                .toList();
    }

    @Transactional
    public AdminUserDto createUser(AdminUserCreateRequest request) {
        validateUniqueUsernameAndEmail(request.username(), request.email());

        User user = User.builder()
                .username(request.username().trim())
                .email(request.email().trim().toLowerCase())
                .fullName(request.fullName().trim())
                .role(request.role())
                .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                .active(true)
                .build();

        return toDto(userRepository.save(user), false);
    }

    @Transactional
    public AdminUserDto setActive(Long userId, boolean active) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        if (!active && user.getRole() == Role.DOCENTE_COORDINADOR) {
            long activeCoordinators = userRepository.countByRoleAndActiveTrue(Role.DOCENTE_COORDINADOR);
            if (activeCoordinators <= 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "No se puede desactivar el último coordinador activo");
            }
        }

        user.setActive(active);
        return toDto(userRepository.save(user), linkRepository.existsByUser_Id(user.getId()));
    }

    private void validateUniqueUsernameAndEmail(String username, String email) {
        if (userRepository.existsByUsernameIgnoreCase(username.trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El usuario ya existe");
        }
        if (userRepository.existsByEmailIgnoreCase(email.trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya está registrado");
        }
    }

    AdminUserDto toDto(User user, boolean linked) {
        return new AdminUserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.isActive(),
                linked
        );
    }
}
