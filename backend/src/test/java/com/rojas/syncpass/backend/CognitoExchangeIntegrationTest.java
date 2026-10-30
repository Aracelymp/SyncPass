package com.rojas.syncpass.backend;

import com.nimbusds.jwt.JWTClaimsSet;
import com.rojas.syncpass.backend.domain.Role;
import com.rojas.syncpass.backend.domain.User;
import com.rojas.syncpass.backend.domain.UserCognitoLink;
import com.rojas.syncpass.backend.repo.UserCognitoLinkRepository;
import com.rojas.syncpass.backend.repo.UserRepository;
import com.rojas.syncpass.backend.service.CognitoTokenValidator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CognitoExchangeIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserCognitoLinkRepository linkRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private CognitoTokenValidator cognitoTokenValidator;

    @Test
    void alreadyLinkedUserCanStillExchangeToken() {
        String sub = UUID.randomUUID().toString();
        String username = "cognito-legacy-" + UUID.randomUUID().toString().substring(0, 8);
        User user = userRepository.save(User.builder()
                .username(username)
                .email(username + "@syncpass.test")
                .fullName("Legacy Cognito User")
                .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(Role.ESTUDIANTE_VOLUNTARIO_MONITOR)
                .active(true)
                .build());

        linkRepository.save(UserCognitoLink.builder()
                .cognitoSub(sub)
                .user(user)
                .build());

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(sub)
                .claim("email", user.getEmail())
                .claim("token_use", "id")
                .build();
        when(cognitoTokenValidator.validate(anyString())).thenReturn(claims);

        ResponseEntity<Map> response = rest.postForEntity(
                "/api/auth/cognito/exchange",
                json(Map.of("idToken", "fake-token")),
                Map.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("accessToken");
        assertThat(((Map<?, ?>) response.getBody().get("user")).get("username")).isEqualTo(username);
    }

    @Test
    void unlinkedIdentityStillGetsForbidden() {
        String sub = UUID.randomUUID().toString();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(sub)
                .claim("email", "unlinked-" + UUID.randomUUID() + "@syncpass.test")
                .claim("token_use", "id")
                .build();
        when(cognitoTokenValidator.validate(anyString())).thenReturn(claims);

        ResponseEntity<Map> response = rest.postForEntity(
                "/api/auth/cognito/exchange",
                json(Map.of("idToken", "fake-token")),
                Map.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void deactivatedLinkedUserCannotExchangeToken() {
        String sub = UUID.randomUUID().toString();
        String username = "inactive-cognito-" + UUID.randomUUID().toString().substring(0, 8);
        User user = userRepository.save(User.builder()
                .username(username)
                .email(username + "@syncpass.test")
                .fullName("Inactive Cognito User")
                .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(Role.ESTUDIANTE_VOLUNTARIO_MONITOR)
                .active(false)
                .build());

        linkRepository.save(UserCognitoLink.builder()
                .cognitoSub(sub)
                .user(user)
                .build());

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(sub)
                .claim("email", user.getEmail())
                .claim("token_use", "id")
                .build();
        when(cognitoTokenValidator.validate(anyString())).thenReturn(claims);

        ResponseEntity<Map> response = rest.postForEntity(
                "/api/auth/cognito/exchange",
                json(Map.of("idToken", "fake-token")),
                Map.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private HttpEntity<Map<String, Object>> json(Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}
