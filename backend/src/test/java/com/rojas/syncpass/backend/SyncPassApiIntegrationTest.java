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
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SyncPassApiIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    @Test
    void healthEndpointIsPublic() {
        ResponseEntity<Map> response = rest.getForEntity("/api/health", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "UP");
    }

    @Test
    void coordinatorCanLoginAndReadProtectedData() {
        String token = login("coordinador");

        ResponseEntity<Map> me = rest.exchange(
                "/api/auth/me",
                HttpMethod.GET,
                authorized(token, null),
                Map.class
        );

        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(me.getBody()).containsEntry("username", "coordinador");
        assertThat(me.getBody()).containsEntry("role", "DOCENTE_COORDINADOR");

        ResponseEntity<List> projects = rest.exchange(
                "/api/projects",
                HttpMethod.GET,
                authorized(token, null),
                List.class
        );
        assertThat(projects.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(projects.getBody()).isNotEmpty();
    }

    @Test
    void monitorCannotCreateProjectsButCanReadThem() {
        String token = login("monitor");

        ResponseEntity<List> projects = rest.exchange(
                "/api/projects",
                HttpMethod.GET,
                authorized(token, null),
                List.class
        );
        assertThat(projects.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<String, Object> project = new LinkedHashMap<>();
        project.put("code", "PRY-FORBIDDEN");
        project.put("name", "Intento no autorizado");
        project.put("description", "El rol monitor no puede crear proyectos.");
        project.put("startDate", LocalDate.now().toString());
        project.put("endDate", LocalDate.now().plusMonths(1).toString());
        project.put("status", "EN_CURSO");
        project.put("responsible", "Monitor de prueba");

        ResponseEntity<Map> created = rest.exchange(
                "/api/projects",
                HttpMethod.POST,
                authorized(token, project),
                Map.class
        );

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void protectedEndpointRejectsMissingAndInvalidTokens() {
        ResponseEntity<Map> anonymous = rest.getForEntity("/api/projects", Map.class);
        assertThat(anonymous.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        ResponseEntity<Map> invalid = rest.exchange(
                "/api/projects",
                HttpMethod.GET,
                authorized("not-a-real-token", null),
                Map.class
        );
        assertThat(invalid.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void badPasswordIsRejected() {
        ResponseEntity<Map> response = rest.postForEntity(
                "/api/auth/login",
                json(Map.of("username", "coordinador", "password", "wrong-password")),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void monitorCanRegisterWalkInAndCollectAnonymousSurveyWithoutSeeingResults() {
        String coordinator = login("coordinador");
        long eventId = createEvent(coordinator);
        String monitor = login("monitor");
        long monitorId = ((Number) ((Map<?, ?>) loginBody("monitor").get("user")).get("id")).longValue();

        Map<String, Object> walkIn = walkIn(monitor, eventId, "María López");
        assertThat(walkIn.get("userId")).isNull();
        assertThat(((Number) walkIn.get("registeredByUserId")).longValue()).isEqualTo(monitorId);

        String surveyClientId = "survey-" + UUID.randomUUID();
        Map<String, Object> survey = surveyOperation(
                eventId,
                surveyClientId,
                (String) walkIn.get("clientId"),
                ((Number) walkIn.get("id")).longValue(),
                "Aprendí algo nuevo."
        );

        Map<String, Object> result = syncSurvey(monitor, survey);
        assertThat(result).containsEntry("status", "SYNCED");

        ResponseEntity<List> surveysResponse = rest.exchange(
                "/api/events/" + eventId + "/surveys",
                HttpMethod.GET,
                authorized(coordinator, null),
                List.class
        );
        assertThat(surveysResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> surveys = (List<Map<String, Object>>) surveysResponse.getBody();
        Map<String, Object> saved = surveys.stream()
                .filter(item -> surveyClientId.equals(item.get("clientId")))
                .findFirst()
                .orElseThrow();
        assertThat(saved.get("userId")).isNull();
        assertThat(((Number) saved.get("attendanceId")).longValue())
                .isEqualTo(((Number) walkIn.get("id")).longValue());
        assertThat(((Number) saved.get("collectedByUserId")).longValue()).isEqualTo(monitorId);
        assertThat(saved.get("participantName")).isEqualTo("María López");

        ResponseEntity<Map> summary = rest.exchange(
                "/api/events/" + eventId + "/survey/summary",
                HttpMethod.GET,
                authorized(monitor, null),
                Map.class
        );
        assertThat(summary.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<Map> monitorSurveyList = rest.exchange(
                "/api/events/" + eventId + "/surveys",
                HttpMethod.GET,
                authorized(monitor, null),
                Map.class
        );
        assertThat(monitorSurveyList.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<Map> coordinatorSummary = rest.exchange(
                "/api/events/" + eventId + "/survey/summary",
                HttpMethod.GET,
                authorized(coordinator, null),
                Map.class
        );
        assertThat(coordinatorSummary.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Number) coordinatorSummary.getBody().get("responseCount")).longValue()).isEqualTo(1);
        assertThat((List<String>) coordinatorSummary.getBody().get("comments"))
                .containsExactly("Aprendí algo nuevo.");

        ResponseEntity<Map> monitorEvent = rest.exchange(
                "/api/events/" + eventId,
                HttpMethod.GET,
                authorized(monitor, null),
                Map.class
        );
        assertThat(monitorEvent.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(monitorEvent.getBody().get("averageRating")).isNull();

        ResponseEntity<Map> coordinatorEvent = rest.exchange(
                "/api/events/" + eventId,
                HttpMethod.GET,
                authorized(coordinator, null),
                Map.class
        );
        assertThat(coordinatorEvent.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Number) coordinatorEvent.getBody().get("averageRating")).doubleValue())
                .isEqualTo(5.0);

        ResponseEntity<Map> detail = rest.exchange(
                "/api/events/" + eventId + "/detail",
                HttpMethod.GET,
                authorized(monitor, null),
                Map.class
        );
        assertThat(detail.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(detail.getBody().get("surveySummary")).isNull();

        ResponseEntity<Map> pull = rest.exchange(
                "/api/sync/pull",
                HttpMethod.GET,
                authorized(monitor, null),
                Map.class
        );
        assertThat(pull.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((List<?>) pull.getBody().get("surveys")).isEmpty();
        List<Map<String, Object>> receipts = (List<Map<String, Object>>) pull.getBody().get("surveyReceipts");
        assertThat(receipts).anySatisfy(receipt -> {
            assertThat(((Number) receipt.get("attendanceId")).longValue())
                    .isEqualTo(((Number) walkIn.get("id")).longValue());
            assertThat(receipt).doesNotContainKeys("rating", "comment", "userId", "participantName");
        });

        Map<String, Object> duplicate = syncSurvey(monitor, surveyOperation(
                eventId,
                "survey-other-" + UUID.randomUUID(),
                (String) walkIn.get("clientId"),
                ((Number) walkIn.get("id")).longValue(),
                "Segundo intento."
        ));
        assertThat(duplicate).containsEntry("status", "DUPLICATE");
    }

    @Test
    void monitorCannotCollectSurveyForAttendanceRegisteredBySomeoneElse() {
        String coordinator = login("coordinador");
        long eventId = createEvent(coordinator);
        String monitor = login("monitor");

        Map<String, Object> coordinatorWalkIn = walkIn(coordinator, eventId, "Participante del coordinador");
        Map<String, Object> result = syncSurvey(monitor, surveyOperation(
                eventId,
                "survey-forbidden-" + UUID.randomUUID(),
                (String) coordinatorWalkIn.get("clientId"),
                ((Number) coordinatorWalkIn.get("id")).longValue(),
                "No debería guardarse."
        ));

        assertThat(result).containsEntry("status", "ERROR");
        assertThat((String) result.get("message")).contains("Solo puedes encuestar");
    }

    @Test
    void ownSurveyFlowStillWorksForAuthenticatedUser() {
        String coordinator = login("coordinador");
        long eventId = createEvent(coordinator);
        String monitor = login("monitor");
        long monitorId = ((Number) ((Map<?, ?>) loginBody("monitor").get("user")).get("id")).longValue();

        Map<String, Object> result = syncSurvey(monitor, surveyOperation(
                eventId,
                "self-survey-" + UUID.randomUUID(),
                null,
                null,
                "Mi propia experiencia."
        ));
        assertThat(result).containsEntry("status", "SYNCED");

        ResponseEntity<List> surveysResponse = rest.exchange(
                "/api/events/" + eventId + "/surveys",
                HttpMethod.GET,
                authorized(coordinator, null),
                List.class
        );
        List<Map<String, Object>> surveys = (List<Map<String, Object>>) surveysResponse.getBody();
        assertThat(surveys).anySatisfy(survey -> {
            assertThat(((Number) survey.get("userId")).longValue()).isEqualTo(monitorId);
            assertThat(survey.get("attendanceId")).isNull();
        });
    }

    @Test
    void coordinatorCanCreateUpdateAndDeleteProject() {
        String token = login("coordinador");
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String code = ("PRY-CRUD-" + suffix).toUpperCase();

        Map<String, Object> created = createProject(token, code, "Proyecto CRUD " + suffix);
        long projectId = ((Number) created.get("id")).longValue();
        assertThat(created).containsEntry("code", code);

        Map<String, Object> update = projectPayload(code, "Proyecto CRUD actualizado " + suffix);
        update.put("status", "FINALIZADO");
        ResponseEntity<Map> updated = rest.exchange(
                "/api/projects/" + projectId,
                HttpMethod.PUT,
                authorized(token, update),
                Map.class
        );
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody()).containsEntry("name", "Proyecto CRUD actualizado " + suffix);
        assertThat(updated.getBody()).containsEntry("status", "FINALIZADO");

        String monitor = login("monitor");
        ResponseEntity<Map> forbidden = rest.exchange(
                "/api/projects/" + projectId,
                HttpMethod.DELETE,
                authorized(monitor, null),
                Map.class
        );
        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<Map> stillExists = rest.exchange(
                "/api/projects/" + projectId,
                HttpMethod.GET,
                authorized(token, null),
                Map.class
        );
        assertThat(stillExists.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Map> deleted = rest.exchange(
                "/api/projects/" + projectId,
                HttpMethod.DELETE,
                authorized(token, null),
                Map.class
        );
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(deleted.getBody()).containsEntry("message", "Proyecto eliminado");

        ResponseEntity<Map> missing = rest.exchange(
                "/api/projects/" + projectId,
                HttpMethod.GET,
                authorized(token, null),
                Map.class
        );
        assertThat(missing.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void projectUpdateRejectsDuplicateCodeAndInvalidDates() {
        String token = login("coordinador");
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Map<String, Object> first = createProject(
                token, "PRY-DUP-A-" + suffix, "Proyecto A " + suffix
        );
        Map<String, Object> second = createProject(
                token, "PRY-DUP-B-" + suffix, "Proyecto B " + suffix
        );
        long secondId = ((Number) second.get("id")).longValue();

        Map<String, Object> duplicateCode = projectPayload(
                (String) first.get("code"), "Proyecto B duplicado " + suffix
        );
        ResponseEntity<Map> duplicate = rest.exchange(
                "/api/projects/" + secondId,
                HttpMethod.PUT,
                authorized(token, duplicateCode),
                Map.class
        );
        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat((String) duplicate.getBody().get("message")).contains("código");

        Map<String, Object> invalidDates = projectPayload(
                (String) second.get("code"), "Proyecto B con fechas inválidas " + suffix
        );
        invalidDates.put("startDate", LocalDate.now().toString());
        invalidDates.put("endDate", LocalDate.now().minusDays(1).toString());
        ResponseEntity<Map> invalid = rest.exchange(
                "/api/projects/" + secondId,
                HttpMethod.PUT,
                authorized(token, invalidDates),
                Map.class
        );
        assertThat(invalid.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat((String) invalid.getBody().get("message")).contains("fecha de fin");
    }

    @Test
    void projectDeleteIsBlockedWhenEventsExist() {
        String token = login("coordinador");
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Map<String, Object> project = createProject(
                token, "PRY-WITH-EVENT-" + suffix, "Proyecto con evento " + suffix
        );
        long projectId = ((Number) project.get("id")).longValue();
        long eventId = createEvent(token, projectId, suffix);

        ResponseEntity<Map> deleted = rest.exchange(
                "/api/projects/" + projectId,
                HttpMethod.DELETE,
                authorized(token, null),
                Map.class
        );
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat((String) deleted.getBody().get("message")).contains("evento");

        ResponseEntity<Map> stillExists = rest.exchange(
                "/api/projects/" + projectId,
                HttpMethod.GET,
                authorized(token, null),
                Map.class
        );
        assertThat(stillExists.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Map> eventStillExists = rest.exchange(
                "/api/events/" + eventId,
                HttpMethod.GET,
                authorized(token, null),
                Map.class
        );
        assertThat(eventStillExists.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void coordinatorCanCreateUpdateAndDeleteEvent() {
        String coordinator = login("coordinador");
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Map<String, Object> project = createProject(
                coordinator, "PRY-EVT-CRUD-" + suffix, "Proyecto eventos CRUD " + suffix
        );
        long projectId = ((Number) project.get("id")).longValue();

        Map<String, Object> created = postEvent(
                coordinator,
                eventPayload(projectId, "Evento CRUD " + suffix, null)
        );
        long eventId = ((Number) created.get("id")).longValue();
        String qrToken = (String) created.get("qrToken");
        assertThat(qrToken).isNotBlank();

        Map<String, Object> update = eventPayload(
                projectId, "Evento actualizado " + suffix, qrToken
        );
        update.put("status", "FINALIZADO");
        ResponseEntity<Map> updated = rest.exchange(
                "/api/events/" + eventId,
                HttpMethod.PUT,
                authorized(coordinator, update),
                Map.class
        );
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody()).containsEntry("title", "Evento actualizado " + suffix);
        assertThat(updated.getBody()).containsEntry("status", "FINALIZADO");
        assertThat(updated.getBody()).containsEntry("qrToken", qrToken);

        String monitor = login("monitor");
        ResponseEntity<Map> forbidden = rest.exchange(
                "/api/events/" + eventId,
                HttpMethod.DELETE,
                authorized(monitor, null),
                Map.class
        );
        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<Map> deleted = rest.exchange(
                "/api/events/" + eventId,
                HttpMethod.DELETE,
                authorized(coordinator, null),
                Map.class
        );
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(deleted.getBody()).containsEntry("message", "Evento eliminado");

        ResponseEntity<Map> missing = rest.exchange(
                "/api/events/" + eventId,
                HttpMethod.GET,
                authorized(coordinator, null),
                Map.class
        );
        assertThat(missing.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void eventUpdateRejectsDuplicateQrAndInvalidDates() {
        String token = login("coordinador");
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Map<String, Object> project = createProject(
                token, "PRY-EVT-UPD-" + suffix, "Proyecto eventos update " + suffix
        );
        long projectId = ((Number) project.get("id")).longValue();

        Map<String, Object> first = postEvent(
                token, eventPayload(projectId, "Evento uno " + suffix, "SP-EVT-A-" + suffix)
        );
        Map<String, Object> second = postEvent(
                token, eventPayload(projectId, "Evento dos " + suffix, "SP-EVT-B-" + suffix)
        );
        long secondId = ((Number) second.get("id")).longValue();

        Map<String, Object> duplicateQr = eventPayload(
                projectId, "Evento dos duplicado " + suffix, (String) first.get("qrToken")
        );
        ResponseEntity<Map> duplicate = rest.exchange(
                "/api/events/" + secondId,
                HttpMethod.PUT,
                authorized(token, duplicateQr),
                Map.class
        );
        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat((String) duplicate.getBody().get("message")).contains("QR");

        Map<String, Object> invalidDates = eventPayload(
                projectId, "Evento con fechas inválidas " + suffix, (String) second.get("qrToken")
        );
        invalidDates.put("startsAt", LocalDateTime.now().plusDays(2).withNano(0).toString());
        invalidDates.put("endsAt", LocalDateTime.now().plusDays(1).withNano(0).toString());
        ResponseEntity<Map> invalid = rest.exchange(
                "/api/events/" + secondId,
                HttpMethod.PUT,
                authorized(token, invalidDates),
                Map.class
        );
        assertThat(invalid.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat((String) invalid.getBody().get("message")).contains("fecha de fin");

        ResponseEntity<Map> missing = rest.exchange(
                "/api/events/99999999",
                HttpMethod.PUT,
                authorized(token, eventPayload(projectId, "Evento inexistente", null)),
                Map.class
        );
        assertThat(missing.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void eventDeleteIsBlockedByRelatedRecords() {
        String coordinator = login("coordinador");
        String monitor = login("monitor");

        long enrolledEvent = createEvent(coordinator);
        ResponseEntity<Map> enrollment = rest.exchange(
                "/api/events/" + enrolledEvent + "/enroll",
                HttpMethod.POST,
                authorized(monitor, null),
                Map.class
        );
        assertThat(enrollment.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertDeleteConflict(coordinator, enrolledEvent, "inscripción");

        long attendedEvent = createEvent(coordinator);
        walkIn(coordinator, attendedEvent, "Participante de asistencia");
        assertDeleteConflict(coordinator, attendedEvent, "asistencia");

        long surveyedEvent = createEvent(coordinator);
        Map<String, Object> survey = new LinkedHashMap<>();
        survey.put("rating", 5);
        survey.put("organizationRating", 4);
        survey.put("impactRating", 5);
        survey.put("comment", "Encuesta de prueba para bloqueo de borrado.");
        survey.put("wouldRecommend", true);
        ResponseEntity<Map> surveyResponse = rest.exchange(
                "/api/events/" + surveyedEvent + "/survey",
                HttpMethod.POST,
                authorized(monitor, survey),
                Map.class
        );
        assertThat(surveyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertDeleteConflict(coordinator, surveyedEvent, "encuesta");
    }

    @Test
    void scanAcceptsQrForSelectedEvent() {
        String coordinator = login("coordinador");
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Map<String, Object> project = createProject(
                coordinator, "PRY-SCAN-OK-" + suffix, "Proyecto scan OK " + suffix
        );
        long projectId = ((Number) project.get("id")).longValue();

        Map<String, Object> eventA = postEvent(
                coordinator, eventPayload(projectId, "Evento scan OK " + suffix, "SP-SCAN-OK-A-" + suffix)
        );
        long eventAId = ((Number) eventA.get("id")).longValue();
        String tokenA = (String) eventA.get("qrToken");

        String monitor = login("monitor");
        ResponseEntity<Map> response = scanResponse(
                monitor, eventAId, "SYNCPASS|EVENT|" + eventAId + "|" + tokenA
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("method", "QR");
        assertThat(((Number) response.getBody().get("eventId")).longValue()).isEqualTo(eventAId);
    }

    @Test
    void scanRejectsQrFromDifferentEvent() {
        String coordinator = login("coordinador");
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Map<String, Object> project = createProject(
                coordinator, "PRY-SCAN-DIFF-" + suffix, "Proyecto scan diferente " + suffix
        );
        long projectId = ((Number) project.get("id")).longValue();

        Map<String, Object> eventA = postEvent(
                coordinator, eventPayload(projectId, "Evento scan A " + suffix, "SP-SCAN-DIFF-A-" + suffix)
        );
        long eventAId = ((Number) eventA.get("id")).longValue();

        Map<String, Object> eventB = postEvent(
                coordinator, eventPayload(projectId, "Evento scan B " + suffix, "SP-SCAN-DIFF-B-" + suffix)
        );
        long eventBId = ((Number) eventB.get("id")).longValue();
        String tokenB = (String) eventB.get("qrToken");

        String monitor = login("monitor");
        ResponseEntity<Map> response = scanResponse(
                monitor, eventAId, "SYNCPASS|EVENT|" + eventBId + "|" + tokenB
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat((String) response.getBody().get("message"))
                .contains("no corresponde al evento seleccionado");
    }

    private ResponseEntity<Map> scanResponse(String token, long eventId, String qrCode) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("qrCode", qrCode);
        return rest.exchange(
                "/api/events/" + eventId + "/attendance/scan",
                HttpMethod.POST,
                authorized(token, body),
                Map.class
        );
    }

    private void assertDeleteConflict(String token, long eventId, String expectedText) {
        ResponseEntity<Map> deleted = rest.exchange(
                "/api/events/" + eventId,
                HttpMethod.DELETE,
                authorized(token, null),
                Map.class
        );
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat((String) deleted.getBody().get("message")).contains(expectedText);

        ResponseEntity<Map> stillExists = rest.exchange(
                "/api/events/" + eventId,
                HttpMethod.GET,
                authorized(token, null),
                Map.class
        );
        assertThat(stillExists.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private String login(String username) {
        return (String) loginBody(username).get("accessToken");
    }

    private Map<String, Object> loginBody(String username) {
        ResponseEntity<Map> response = rest.postForEntity(
                "/api/auth/login",
                json(Map.of("username", username, "password", "Demo1234")),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("accessToken");
        return response.getBody();
    }

    private long createEvent(String token) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Map<String, Object> project = createProject(
                token, "PRY-TEST-" + suffix, "Proyecto de prueba " + suffix
        );
        return createEvent(token, ((Number) project.get("id")).longValue(), suffix);
    }

    private long createEvent(String token, long projectId, String suffix) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("projectId", projectId);
        event.put("title", "Evento de prueba " + suffix);
        event.put("description", "Evento temporal para pruebas de encuestas.");
        event.put("location", "Aula de pruebas");
        event.put("startsAt", LocalDateTime.now().plusDays(1).withNano(0).toString());
        event.put("endsAt", LocalDateTime.now().plusDays(1).plusHours(2).withNano(0).toString());
        event.put("capacity", 20);
        event.put("status", "PROGRAMADO");

        ResponseEntity<Map> eventResponse = rest.exchange(
                "/api/events", HttpMethod.POST, authorized(token, event), Map.class
        );
        assertThat(eventResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        return ((Number) eventResponse.getBody().get("id")).longValue();
    }

    private Map<String, Object> postEvent(String token, Map<String, Object> payload) {
        ResponseEntity<Map> response = rest.exchange(
                "/api/events",
                HttpMethod.POST,
                authorized(token, payload),
                Map.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private Map<String, Object> eventPayload(long projectId, String title, String qrToken) {
        LocalDateTime startsAt = LocalDateTime.now().plusDays(2).withNano(0);
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("projectId", projectId);
        event.put("title", title);
        event.put("description", "Evento temporal para pruebas de eventos.");
        event.put("location", "Aula de pruebas");
        event.put("startsAt", startsAt.toString());
        event.put("endsAt", startsAt.plusHours(2).toString());
        event.put("capacity", 20);
        event.put("status", "PROGRAMADO");
        if (qrToken != null && !qrToken.isBlank()) {
            event.put("qrToken", qrToken);
        }
        return event;
    }

    private Map<String, Object> createProject(String token, String code, String name) {
        ResponseEntity<Map> response = rest.exchange(
                "/api/projects",
                HttpMethod.POST,
                authorized(token, projectPayload(code, name)),
                Map.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private Map<String, Object> projectPayload(String code, String name) {
        Map<String, Object> project = new LinkedHashMap<>();
        project.put("code", code);
        project.put("name", name);
        project.put("description", "Proyecto temporal para pruebas de proyectos.");
        project.put("startDate", LocalDate.now().toString());
        project.put("endDate", LocalDate.now().plusMonths(1).toString());
        project.put("status", "EN_CURSO");
        project.put("responsible", "Pruebas SyncPass");
        return project;
    }

    private Map<String, Object> walkIn(String token, long eventId, String participantName) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("participantName", participantName);
        body.put("participantEmail", "participante-" + UUID.randomUUID() + "@syncpass.test");
        body.put("participantDocument", "DOC-" + UUID.randomUUID().toString().substring(0, 8));

        ResponseEntity<Map> response = rest.exchange(
                "/api/events/" + eventId + "/attendance/walk-in",
                HttpMethod.POST,
                authorized(token, body),
                Map.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private Map<String, Object> surveyOperation(long eventId, String clientId,
                                                String attendanceClientId,
                                                Long attendanceServerId,
                                                String comment) {
        Map<String, Object> operation = new LinkedHashMap<>();
        operation.put("clientId", clientId);
        operation.put("type", "SURVEY");
        operation.put("eventId", eventId);
        operation.put("attendanceClientId", attendanceClientId);
        operation.put("attendanceServerId", attendanceServerId);
        operation.put("rating", 5);
        operation.put("organizationRating", 4);
        operation.put("impactRating", 5);
        operation.put("comment", comment);
        operation.put("wouldRecommend", true);
        return operation;
    }

    private Map<String, Object> syncSurvey(String token, Map<String, Object> operation) {
        Map<String, Object> batch = Map.of("operations", List.of(operation));
        ResponseEntity<Map> response = rest.exchange(
                "/api/sync/batch",
                HttpMethod.POST,
                authorized(token, batch),
                Map.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> results = (List<Map<String, Object>>) response.getBody().get("results");
        assertThat(results).hasSize(1);
        return results.get(0);
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
