package com.rojas.syncpass.backend.web;

import com.rojas.syncpass.backend.config.CustomUserDetailsService;
import com.rojas.syncpass.backend.config.JwtService;
import com.rojas.syncpass.backend.domain.User;
import com.rojas.syncpass.backend.dto.ApiDtos;
import com.rojas.syncpass.backend.repo.UserCognitoLinkRepository;
import com.rojas.syncpass.backend.repo.UserRepository;
import com.rojas.syncpass.backend.service.CognitoTokenValidator;
import com.nimbusds.jwt.JWTClaimsSet;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthCognitoController {

    private final CognitoTokenValidator cognitoTokenValidator;
    private final UserCognitoLinkRepository userCognitoLinkRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public AuthCognitoController(
            CognitoTokenValidator cognitoTokenValidator,
            UserCognitoLinkRepository userCognitoLinkRepository,
            UserRepository userRepository,
            JwtService jwtService,
            CustomUserDetailsService userDetailsService
    ) {
        this.cognitoTokenValidator = cognitoTokenValidator;
        this.userCognitoLinkRepository = userCognitoLinkRepository;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    public record CognitoExchangeRequest(
            @NotBlank(message = "El token Cognito es obligatorio") String idToken,
            String accessToken
    ) {
    }

    @PostMapping("/cognito/exchange")
    public ApiDtos.LoginResponse exchange(@Valid @RequestBody CognitoExchangeRequest request) {
        String token = request.idToken();
        if (token == null || token.isBlank()) {
            token = request.accessToken();
        }
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token Cognito requerido");
        }
        JWTClaimsSet claims = cognitoTokenValidator.validate(token);
        String sub = claims.getSubject();
        if (sub == null || sub.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token Cognito inválido");
        }
        var link = userCognitoLinkRepository.findByCognitoSub(sub)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Identidad Cognito no vinculada"));
        User user = userRepository.findById(link.getUser().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuario no autorizado"));
        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuario no autorizado");
        }
        userDetailsService.loadUserByUsername(user.getUsername());
        String accessToken = jwtService.generateToken(user.getUsername(), user.getId(), user.getRole().name());
        return new ApiDtos.LoginResponse(accessToken, "Bearer", jwtService.getExpirationSeconds(), toDto(user));
    }

    private ApiDtos.UserDto toDto(User user) {
        return new ApiDtos.UserDto(user.getId(), user.getUsername(), user.getEmail(),
                user.getFullName(), user.getRole());
    }
}
