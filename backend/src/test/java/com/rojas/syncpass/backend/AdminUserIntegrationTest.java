package com.rojas.syncpass.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AdminUserIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    @Test
    void coordinatorCanListAndCreateUsers() {
        String token = login("coordinador");

        ResponseEntity<List> list = rest.exchange(
                "/api/admin/users",
                HttpMethod.GET,
                authorized(token, null),
                List.class
        );
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(list.getBody()).hasSizeGreaterThanOrEqualTo(5);

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("username", "nuevo-" + suffix);
        body.put("email", "nuevo-" + suffix + "@syncpass.test");
        body.put("fullName", "Usuario Nuevo " + suffix);
        body.put("role", "ESTUDIANTE_VOLUNTARIO_MONITOR");

        ResponseEntity<Map> created = rest.exchange(
                "/api/admin/users",
                HttpMethod.POST,
                authorized(token, body),
                Map.class
        );
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(created.getBody()).containsEntry("username", "nuevo-" + suffix);
        assertThat(created.getBody()).containsEntry("active", true);
        assertThat(created.getBody()).containsEntry("cognitoLinked", false);
    }

    @Test
    void monitorCannotAccessAdminUsers() {
        String token = login("monitor");

        ResponseEntity<Map> response = rest.exchange(
                "/api/admin/users",
                HttpMethod.GET,
                authorized(token, null),
                Map.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void createUserRejectsDuplicateUsernameOrEmail() {
        String token = login("coordinador");
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String username = "dup-" + suffix;
        String email = "dup-" + suffix + "@syncpass.test";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("username", username);
        body.put("email", email);
        body.put("fullName", "Usuario Dup " + suffix);
        body.put("role", "ESTUDIANTE_VOLUNTARIO_MONITOR");

        ResponseEntity<Map> first = rest.exchange(
                "/api/admin/users",
                HttpMethod.POST,
                authorized(token, body),
                Map.class
        );
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<String, Object> duplicateUsername = new LinkedHashMap<>(body);
        duplicateUsername.put("email", "otro-" + suffix + "@syncpass.test");
        ResponseEntity<Map> conflictUser = rest.exchange(
                "/api/admin/users",
                HttpMethod.POST,
                authorized(token, duplicateUsername),
                Map.class
        );
        assertThat(conflictUser.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        Map<String, Object> duplicateEmail = new LinkedHashMap<>(body);
        duplicateEmail.put("username", "otro-" + suffix);
        ResponseEntity<Map> conflictEmail = rest.exchange(
                "/api/admin/users",
                HttpMethod.POST,
                authorized(token, duplicateEmail),
                Map.class
        );
        assertThat(conflictEmail.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void coordinatorCanToggleUserActiveButCannotDisableLastCoordinator() {
        String token = login("coordinador");

        ResponseEntity<List> list = rest.exchange(
                "/api/admin/users",
                HttpMethod.GET,
                authorized(token, null),
                List.class
        );
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> coordinator = ((List<Map<String, Object>>) list.getBody()).stream()
                .filter(u -> "DOCENTE_COORDINADOR".equals(u.get("role")))
                .findFirst()
                .orElseThrow();

        ResponseEntity<Map> disabled = rest.exchange(
                "/api/admin/users/" + coordinator.get("id") + "/active",
                HttpMethod.PATCH,
                authorized(token, Map.of("active", false)),
                Map.class
        );
        assertThat(disabled.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        Map<String, Object> monitor = ((List<Map<String, Object>>) list.getBody()).stream()
                .filter(u -> "ESTUDIANTE_VOLUNTARIO_MONITOR".equals(u.get("role")) && !(Boolean) u.get("cognitoLinked"))
                .findFirst()
                .orElseThrow();

        ResponseEntity<Map> deactivated = rest.exchange(
                "/api/admin/users/" + monitor.get("id") + "/active",
                HttpMethod.PATCH,
                authorized(token, Map.of("active", false)),
                Map.class
        );
        assertThat(deactivated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(deactivated.getBody()).containsEntry("active", false);
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
