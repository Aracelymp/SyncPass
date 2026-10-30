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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DisabledUserJwtIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    @Test
    void deactivatedUserCannotUseExistingJwt() {
        String monitorToken = login("monitor");
        long monitorId = userId("monitor");

        String coordinator = login("coordinador");

        ResponseEntity<Map> deactivated = rest.exchange(
                "/api/admin/users/" + monitorId + "/active",
                HttpMethod.PATCH,
                authorized(coordinator, Map.of("active", false)),
                Map.class
        );
        assertThat(deactivated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(deactivated.getBody()).containsEntry("active", false);

        ResponseEntity<Map> response = rest.exchange(
                "/api/projects",
                HttpMethod.GET,
                authorized(monitorToken, null),
                Map.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // Reactivate the user so other tests in this class are not affected.
        ResponseEntity<Map> reactivated = rest.exchange(
                "/api/admin/users/" + monitorId + "/active",
                HttpMethod.PATCH,
                authorized(coordinator, Map.of("active", true)),
                Map.class
        );
        assertThat(reactivated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reactivated.getBody()).containsEntry("active", true);
    }

    @Test
    void activeUserCanStillAccessProtectedEndpoints() {
        String monitor = login("monitor");

        ResponseEntity<List> projects = rest.exchange(
                "/api/projects",
                HttpMethod.GET,
                authorized(monitor, null),
                List.class
        );
        assertThat(projects.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private long userId(String username) {
        ResponseEntity<Map> response = rest.postForEntity(
                "/api/auth/login",
                json(Map.of("username", username, "password", "Demo1234")),
                Map.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> user = (Map<?, ?>) response.getBody().get("user");
        return ((Number) user.get("id")).longValue();
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
