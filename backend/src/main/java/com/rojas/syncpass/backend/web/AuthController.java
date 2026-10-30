package com.rojas.syncpass.backend.web;

import com.rojas.syncpass.backend.config.CustomUserDetailsService;
import com.rojas.syncpass.backend.config.JwtService;
import com.rojas.syncpass.backend.domain.User;
import com.rojas.syncpass.backend.dto.ApiDtos.LoginRequest;
import com.rojas.syncpass.backend.dto.ApiDtos.LoginResponse;
import com.rojas.syncpass.backend.dto.ApiDtos.UserDto;
import com.rojas.syncpass.backend.service.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final CurrentUserService currentUser;

    public AuthController(
            AuthenticationManager authenticationManager,
            CustomUserDetailsService userDetailsService,
            JwtService jwtService,
            CurrentUserService currentUser
    ) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.currentUser = currentUser;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username().trim(), request.password()));
        } catch (BadCredentialsException | org.springframework.security.core.userdetails.UsernameNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos");
        }
        User user = userDetailsService.findByUsernameOrEmail(request.username().trim());
        String token = jwtService.generateToken(user.getUsername(), user.getId(), user.getRole().name());
        return new LoginResponse(token, "Bearer", jwtService.getExpirationSeconds(), toDto(user));
    }

    @GetMapping("/me")
    public UserDto me() {
        return toDto(currentUser.require());
    }

    private UserDto toDto(User user) {
        return new UserDto(user.getId(), user.getUsername(), user.getEmail(),
                user.getFullName(), user.getRole());
    }
}
