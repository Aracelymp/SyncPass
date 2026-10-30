package com.rojas.syncpass.backend.service;

import com.rojas.syncpass.backend.domain.User;
import com.rojas.syncpass.backend.repo.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User require() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new org.springframework.security.access.AccessDeniedException("Sesión no válida");
        }
        return userRepository.findByUsernameIgnoreCase(authentication.getName())
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Sesión no válida"));
    }

    public boolean isCoordinator() {
        return require().getRole() == com.rojas.syncpass.backend.domain.Role.DOCENTE_COORDINADOR;
    }
}
