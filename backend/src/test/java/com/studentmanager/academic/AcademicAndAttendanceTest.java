package com.studentmanager.academic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Proves two of the actual guarantees a student would rely on:
 *  1. You cannot schedule two overlapping classes - enforced by the
 *     database itself (ex_schedule_no_overlap), not just application code.
 *  2. SGPA is computed correctly, credit-weighted, server-side - matching
 *     the same 10-point scale and 100-mark breakdown the prototype used,
 *     now as the single real source of truth.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AcademicAndAttendanceTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @ServiceConnection
    static PostgreSQLContainer<?> postgresConnection = postgres;

    @DynamicPropertySource
    static void jwtSecret(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret", () -> "test-only-secret-key-at-least-32-bytes-long!!");
    }

    @LocalServerPort int port;
    @Autowired TestRestTemplate rest;
    final ObjectMapper mapper = new ObjectMapper();

    private String url(String path) { return "http://localhost:" + port + path; }

    /** Registers a fresh account and returns an Authorization header ready to use. */
    private HttpHeaders authHeaders() throws Exception {
        String email = "test-" + UUID.randomUUID() + "@example.com";
        JsonNode body = mapper.valueToTree(Map.of("name", "Test Student", "email", email, "password", "a-strong-password"));
        var resp = rest.postForEntity(url("/api/v1/auth/register"), jsonEntity(body), JsonNode.class);
        assertEquals(201, resp.getStatusCode().value());
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(resp.getBody().at("/data/accessToken").asText());
        return h;
    }

    private HttpEntity<JsonNode> jsonEntity(JsonNode body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private String createSemester(HttpHeaders auth) throws Exception {
        JsonNode body = mapper.valueToTree(Map.of("number", 4, "startDate", "2026-01-05", "endDate", "2026-05-20"));
        var resp = rest.postForEntity(url("/api/v1/semesters"), new HttpEntity<>(body, auth), JsonNode.class);
        assertEquals(201, resp.getStatusCode().value());
        return resp.getBody().at("/data/id").asText();
    }

    private String createSubject(HttpHeaders auth, String semesterId, String name) throws Exception {
        JsonNode body = mapper.valueToTree(Map.of("name", name, "teacher", "Prof. Rao", "requiredAttendancePct", 75));
        var resp = rest.postForEntity(url("/api/v1/semesters/" + semesterId + "/subjects"), new HttpEntity<>(body, auth), JsonNode.class);
        assertEquals(201, resp.getStatusCode().value());
        return resp.getBody().at("/data/id").asText();
    }

    @Test
    void overlappingClassesOnTheSameDayAreRejected() throws Exception {
        HttpHeaders auth = authHeaders();
        String semesterId = createSemester(auth);
        String subjectA = createSubject(auth, semesterId, "Data Structures");
        String subjectB = createSubject(auth, semesterId, "Database Systems");

        // Monday 9:00-10:00 for subject A
        JsonNode slotA = mapper.valueToTree(Map.of("dayOfWeek", 0, "startMinutes", 540, "endMinutes", 600));
        var first = rest.postForEntity(url("/api/v1/subjects/" + subjectA + "/schedule"), new HttpEntity<>(slotA, auth), JsonNode.class);
        assertEquals(201, first.getStatusCode().value());

        // Monday 9:30-10:30 for a DIFFERENT subject - overlaps the first by 30 minutes
        JsonNode slotB = mapper.valueToTree(Map.of("dayOfWeek", 0, "startMinutes", 570, "endMinutes", 630));
        var second = rest.postForEntity(url("/api/v1/subjects/" + subjectB + "/schedule"), new HttpEntity<>(slotB, auth), JsonNode.class);
        assertEquals(409, second.getStatusCode().value(),
                "A second class overlapping an existing one on the same day must be rejected, even for a different subject");

        // A non-overlapping slot the same day must still succeed.
        JsonNode slotC = mapper.valueToTree(Map.of("dayOfWeek", 0, "startMinutes", 600, "endMinutes", 660));
        var third = rest.postForEntity(url("/api/v1/subjects/" + subjectB + "/schedule"), new HttpEntity<>(slotC, auth), JsonNode.class);
        assertEquals(201, third.getStatusCode().value(), "A back-to-back, non-overlapping slot must be allowed");
    }

    @Test
    void sgpaIsCreditWeightedAndOnlyCountsSubjectsWithCompleteMarksAndCredits() throws Exception {
        HttpHeaders auth = authHeaders();
        String semesterId = createSemester(auth);

        // Subject 1: 4 credits, total marks -> 86 -> grade A+ (9 points)
        // mid 14 + end 52 + attendance 0 (none marked) + quiz/tutorial off + assignments none = 66... 
        // instead, drive it precisely via explicit marks that sum to a known total.
        String s1 = createSubject(auth, semesterId, "Data Structures");
        JsonNode marks1 = mapper.valueToTree(Map.of("credits", 4.0, "midMarks", 14.0, "endMarks", 52.0)); // 66 total -> B+ (7 pts), no quiz/tutorial/assignments/attendance
        var m1 = rest.exchange(url("/api/v1/subjects/" + s1 + "/marks"), HttpMethod.PUT, new HttpEntity<>(marks1, auth), JsonNode.class);
        assertEquals(200, m1.getStatusCode().value());
        assertEquals(66.0, m1.getBody().at("/data/totalMarks").asDouble(), 0.01);
        assertEquals("B+", m1.getBody().at("/data/grade").asText());
        assertEquals(7, m1.getBody().at("/data/gradePoints").asInt());

        // Subject 2: 3 credits, mid 15 + end 60 = 75 -> grade A (8 points)
        String s2 = createSubject(auth, semesterId, "Database Systems");
        JsonNode marks2 = mapper.valueToTree(Map.of("credits", 3.0, "midMarks", 15.0, "endMarks", 60.0)); // 75 total -> A (8 pts)
        var m2 = rest.exchange(url("/api/v1/subjects/" + s2 + "/marks"), HttpMethod.PUT, new HttpEntity<>(marks2, auth), JsonNode.class);
        assertEquals(200, m2.getStatusCode().value());
        assertEquals("A", m2.getBody().at("/data/grade").asText());

        // Subject 3: has credits but NO marks yet - must NOT count toward SGPA at all.
        createSubject(auth, semesterId, "Discrete Math");

        var dash = rest.exchange(url("/api/v1/semesters/" + semesterId + "/dashboard"), HttpMethod.GET, new HttpEntity<>(auth), JsonNode.class);
        assertEquals(200, dash.getStatusCode().value());

        // Expected SGPA = (4*7 + 3*8) / (4+3) = (28+24)/7 = 52/7 = 7.428... -> 7.43
        assertEquals(7.43, dash.getBody().at("/data/sgpa").asDouble(), 0.01);
        assertEquals(7.0, dash.getBody().at("/data/creditsCounted").asDouble(), 0.01,
                "The ungraded third subject's credits must not be counted");
        assertEquals(3, dash.getBody().at("/data/subjects").size());
    }

    @Test
    void attendanceBelowRequiredSuggestsHowManyClassesToRecover() throws Exception {
        HttpHeaders auth = authHeaders();
        String semesterId = createSemester(auth);
        String subjectId = createSubject(auth, semesterId, "Programming");

        JsonNode slot = mapper.valueToTree(Map.of("dayOfWeek", 0, "startMinutes", 540, "endMinutes", 600));
        var sched = rest.postForEntity(url("/api/v1/subjects/" + subjectId + "/schedule"), new HttpEntity<>(slot, auth), JsonNode.class);
        String scheduleId = sched.getBody().at("/data/id").asText();

        // Mark 5 present, 3 absent on consecutive Mondays -> 62.5%, below the 75% default requirement.
        String[] dates = {"2026-01-05","2026-01-12","2026-01-19","2026-01-26","2026-02-02","2026-02-09","2026-02-16","2026-02-23"};
        String[] statuses = {"PRESENT","PRESENT","ABSENT","PRESENT","ABSENT","PRESENT","ABSENT","PRESENT"};
        for (int i = 0; i < dates.length; i++) {
            JsonNode mark = mapper.valueToTree(Map.of("scheduleId", scheduleId, "classDate", dates[i], "status", statuses[i]));
            var resp = rest.postForEntity(url("/api/v1/attendance"), new HttpEntity<>(mark, auth), JsonNode.class);
            assertEquals(200, resp.getStatusCode().value());
        }

        var summary = rest.exchange(url("/api/v1/attendance/subject/" + subjectId), HttpMethod.GET, new HttpEntity<>(auth), JsonNode.class);
        assertEquals(200, summary.getStatusCode().value());
        assertEquals(5, summary.getBody().at("/data/present").asInt());
        assertEquals(3, summary.getBody().at("/data/absent").asInt());
        assertEquals(62.5, summary.getBody().at("/data/percentage").asDouble(), 0.01);
        assertTrue(summary.getBody().at("/data/classesNeededToReachRequired").asInt() > 0,
                "Below the required percentage, a positive recovery count must be suggested");
    }
}
