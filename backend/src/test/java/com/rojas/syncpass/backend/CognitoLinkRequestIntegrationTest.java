package com.rojas.syncpass.backend;

import com.nimbusds.jwt.JWTClaimsSet;
import com.rojas.syncpass.backend.service.CognitoTokenValidator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CognitoLinkRequestIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    @MockitoBean
    private CognitoTokenValidator cognitoTokenValidator;

    @Test
    void unlinkedIdentityCanRequestAccessAndCoordinatorCanApproveWithExistingUser() {
        String sub = UUID.randomUUID().toString();
        String email = "solicitante-" + UUID.randomUUID() + "@syncpass.test";
        mockToken(sub, email, "Solicitante Prueba");

        ResponseEntity<Map> request = rest.postForEntity(
                "/api/auth/cognito/link-request",
                json(Map.of("idToken", "fake-token-" + sub)),
                Map.class
        );
        assertThat(request.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(request.getBody()).containsEntry("status", "PENDING");
        assertThat(request.getBody()).containsEntry("email", email);
        Long requestId = ((Number) request.getBody().get("id")).longValue();

        String coordinator = login("coordinador");

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Map<String, Object> newUser = new LinkedHashMap<>();
        newUser.put("username", "aprobado-" + suffix);
        newUser.put("email", "aprobado-" + suffix + "@syncpass.test");
        newUser.put("fullName", "Aprobado " + suffix);
        newUser.put("role", "ESTUDIANTE_VOLUNTARIO_MONITOR");

        ResponseEntity<Map> created = rest.exchange(
                "/api/admin/users",
                HttpMethod.POST,
                authorized(coordinator, newUser),
                Map.class
        );
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
        Long userId = ((Number) created.getBody().get("id")).longValue();

        ResponseEntity<List> pendingBefore = rest.exchange(
                "/api/admin/cognito-link-requests",
                HttpMethod.GET,
                authorized(coordinator, null),
                List.class
        );
        assertThat(pendingBefore.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(pendingBefore.getBody()).anySatisfy(item -> {
            Map<?, ?> map = (Map<?, ?>) item;
            assertThat(map.get("id")).isEqualTo(requestId.intValue());
        });

        ResponseEntity<Map> approved = rest.exchange(
                "/api/admin/cognito-link-requests/" + requestId + "/approve",
                HttpMethod.POST,
                authorized(coordinator, Map.of("userId", userId)),
                Map.class
        );
        assertThat(approved.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(approved.getBody()).containsEntry("status", "APPROVED");
        assertThat(approved.getBody()).containsEntry("linkedUserId", userId.intValue());

        ResponseEntity<List> pendingAfter = rest.exchange(
                "/api/admin/cognito-link-requests",
                HttpMethod.GET,
                authorized(coordinator, null),
                List.class
        );
        assertThat(pendingAfter.getBody()).noneSatisfy(item -> {
            Map<?, ?> map = (Map<?, ?>) item;
            assertThat(map.get("id")).isEqualTo(requestId.intValue());
        });

        ResponseEntity<List> users = rest.exchange(
                "/api/admin/users",
                HttpMethod.GET,
                authorized(coordinator, null),
                List.class
        );
        Map<String, Object> linked = ((List<Map<String, Object>>) users.getBody()).stream()
                .filter(u -> userId.equals(((Number) u.get("id")).longValue()))
                .findFirst()
                .orElseThrow();
        assertThat(linked.get("cognitoLinked")).isEqualTo(true);
    }

    @Test
    void coordinatorCanApproveRequestCreatingNewUserInSameTransaction() {
        String sub = UUID.randomUUID().toString();
        String email = "nuevo-" + UUID.randomUUID() + "@syncpass.test";
        mockToken(sub, email, "Nuevo Usuario");

        ResponseEntity<Map> request = rest.postForEntity(
                "/api/auth/cognito/link-request",
                json(Map.of("idToken", "fake-token-" + sub)),
                Map.class
        );
        assertThat(request.getStatusCode()).isEqualTo(HttpStatus.OK);
        Long requestId = ((Number) request.getBody().get("id")).longValue();

        String coordinator = login("coordinador");
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Map<String, Object> newUser = new LinkedHashMap<>();
        newUser.put("username", "transaccional-" + suffix);
        newUser.put("email", "transaccional-" + suffix + "@syncpass.test");
        newUser.put("fullName", "Usuario Transaccional " + suffix);
        newUser.put("role", "ESTUDIANTE_VOLUNTARIO_MONITOR");

        Map<String, Object> approveBody = new LinkedHashMap<>();
        approveBody.put("newUser", newUser);

        ResponseEntity<Map> approved = rest.exchange(
                "/api/admin/cognito-link-requests/" + requestId + "/approve",
                HttpMethod.POST,
                authorized(coordinator, approveBody),
                Map.class
        );
        assertThat(approved.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(approved.getBody()).containsEntry("status", "APPROVED");
        assertThat(approved.getBody()).containsEntry("linkedUsername", "transaccional-" + suffix);
    }

    @Test
    void coordinatorCanRejectRequest() {
        String sub = UUID.randomUUID().toString();
        mockToken(sub, "rechazado-" + UUID.randomUUID() + "@syncpass.test", "Rechazado");

        ResponseEntity<Map> request = rest.postForEntity(
                "/api/auth/cognito/link-request",
                json(Map.of("idToken", "fake-token-" + sub)),
                Map.class
        );
        assertThat(request.getStatusCode()).isEqualTo(HttpStatus.OK);
        Long requestId = ((Number) request.getBody().get("id")).longValue();

        String coordinator = login("coordinador");
        ResponseEntity<Map> rejected = rest.exchange(
                "/api/admin/cognito-link-requests/" + requestId + "/reject",
                HttpMethod.POST,
                authorized(coordinator, Map.of("reason", "Solicitud no autorizada")),
                Map.class
        );
        assertThat(rejected.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(rejected.getBody()).containsEntry("status", "REJECTED");
        assertThat(rejected.getBody()).containsEntry("rejectionReason", "Solicitud no autorizada");
    }

    @Test
    void duplicateCognitoSubOrUserCannotBeLinkedTwice() {
        String sub = UUID.randomUUID().toString();
        String email = "duplicado-" + UUID.randomUUID() + "@syncpass.test";
        mockToken(sub, email, "Duplicado");

        ResponseEntity<Map> request = rest.postForEntity(
                "/api/auth/cognito/link-request",
                json(Map.of("idToken", "fake-token-" + sub)),
                Map.class
        );
        assertThat(request.getStatusCode()).isEqualTo(HttpStatus.OK);
        Long requestId = ((Number) request.getBody().get("id")).longValue();

        String coordinator = login("coordinador");
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Map<String, Object> newUser = new LinkedHashMap<>();
        newUser.put("username", "dup-user-" + suffix);
        newUser.put("email", "dup-user-" + suffix + "@syncpass.test");
        newUser.put("fullName", "Usuario Duplicado " + suffix);
        newUser.put("role", "ESTUDIANTE_VOLUNTARIO_MONITOR");

        ResponseEntity<Map> approved = rest.exchange(
                "/api/admin/cognito-link-requests/" + requestId + "/approve",
                HttpMethod.POST,
                authorized(coordinator, Map.of("newUser", newUser)),
                Map.class
        );
        assertThat(approved.getStatusCode()).isEqualTo(HttpStatus.OK);
        Long userId = ((Number) approved.getBody().get("linkedUserId")).longValue();

        // A new access request with the already-linked sub must be rejected.
        ResponseEntity<Map> sameSubRequest = rest.postForEntity(
                "/api/auth/cognito/link-request",
                json(Map.of("idToken", "fake-token-" + sub)),
                Map.class
        );
        assertThat(sameSubRequest.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        // A different sub cannot be linked to the same user.
        String sub2 = UUID.randomUUID().toString();
        mockToken(sub2, "otro-" + UUID.randomUUID() + "@syncpass.test", "Otro");
        ResponseEntity<Map> secondRequest = rest.postForEntity(
                "/api/auth/cognito/link-request",
                json(Map.of("idToken", "fake-token-" + sub2)),
                Map.class
        );
        Long secondRequestId = ((Number) secondRequest.getBody().get("id")).longValue();

        ResponseEntity<Map> sameUserConflict = rest.exchange(
                "/api/admin/cognito-link-requests/" + secondRequestId + "/approve",
                HttpMethod.POST,
                authorized(coordinator, Map.of("userId", userId)),
                Map.class
        );
        assertThat(sameUserConflict.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void monitorCannotAccessAdminLinkRequests() {
        String token = login("monitor");
        ResponseEntity<Map> response = rest.exchange(
                "/api/admin/cognito-link-requests",
                HttpMethod.GET,
                authorized(token, null),
                Map.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private void mockToken(String sub, String email, String name) {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(sub)
                .claim("email", email)
                .claim("name", name)
                .claim("token_use", "id")
                .build();
        when(cognitoTokenValidator.validate(anyString())).thenReturn(claims);
    }

    private String login(String username) {
        ResponseEntity<Map> response = rest.postForEntity(
                "/api/auth/login",
                json(Map.of("username", username, "password", "Demo1234")),
                Map.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return (String) response.getBody().get("accessToken");
    }

    private HttpEntity<Map<String, Object>> authorized(String token, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private HttpEntity<Map<String, Object>> json(Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}
