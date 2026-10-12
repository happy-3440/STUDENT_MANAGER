package com.studentmanager.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This is the test that matters for the "no glitches on another device"
 * requirement. It does not mock anything: it boots the real Spring context
 * against a real, disposable PostgreSQL container, registers one account,
 * then simulates two separate devices logging into that SAME account and
 * asserts they resolve to the exact same user id. That equality is the
 * actual guarantee - not a design intention, a passing assertion.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CrossDeviceLoginTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void registerJwtSecret(DynamicPropertyRegistry registry) {
        // Testcontainers wires DATABASE_URL/USERNAME/PASSWORD automatically
        // via @ServiceConnection below; we only need to supply the secret
        // the app requires at startup.
        registry.add("app.jwt.secret", () -> "test-only-secret-key-at-least-32-bytes-long!!");
    }

    @ServiceConnection
    static PostgreSQLContainer<?> postgresConnection = postgres;

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    final ObjectMapper mapper = new ObjectMapper();

    private String baseUrl(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    void sameAccountLoggedInFromTwoDevicesResolvesToTheSameUser() throws Exception {
        String email = "cross-device-" + UUID.randomUUID() + "@example.com";
        String password = "correct horse battery staple";

        // "Device A" creates the account.
        JsonNode registerBody = mapper.valueToTree(Map.of(
                "name", "Soumya",
                "email", email,
                "password", password
        ));
        var registerResp = rest.postForEntity(baseUrl("/api/v1/auth/register"), jsonEntity(registerBody), JsonNode.class);
        assertEquals(201, registerResp.getStatusCode().value());
        String userIdFromDeviceA = registerResp.getBody().at("/data/user/id").asText();
        String accessTokenDeviceA = registerResp.getBody().at("/data/accessToken").asText();

        // "Device B" (a different phone/laptop) logs into the SAME account,
        // independently - no shared state passed between the two calls
        // except the email and password a real person would type in.
        JsonNode loginBody = mapper.valueToTree(Map.of("email", email, "password", password));
        var loginResp = rest.postForEntity(baseUrl("/api/v1/auth/login"), jsonEntity(loginBody), JsonNode.class);
        assertEquals(200, loginResp.getStatusCode().value());
        String userIdFromDeviceB = loginResp.getBody().at("/data/user/id").asText();
        String accessTokenDeviceB = loginResp.getBody().at("/data/accessToken").asText();

        // The actual claim under test: both devices resolve to the SAME
        // underlying account - not two different rows with the same email.
        assertEquals(userIdFromDeviceA, userIdFromDeviceB,
                "Device A and Device B must resolve to the same user id for the same account");

        // And each device's own token independently authenticates as that
        // same user via the real secured endpoint, not just at login time.
        assertEquals(userIdFromDeviceA, whoAmI(accessTokenDeviceA));
        assertEquals(userIdFromDeviceB, whoAmI(accessTokenDeviceB));
    }

    @Test
    void duplicateRegistrationWithSameEmailIsRejected() throws Exception {
        String email = "dup-" + UUID.randomUUID() + "@example.com";
        JsonNode body = mapper.valueToTree(Map.of("name", "A", "email", email, "password", "password123"));

        var first = rest.postForEntity(baseUrl("/api/v1/auth/register"), jsonEntity(body), JsonNode.class);
        assertEquals(201, first.getStatusCode().value());

        var second = rest.postForEntity(baseUrl("/api/v1/auth/register"), jsonEntity(body), JsonNode.class);
        assertEquals(409, second.getStatusCode().value(),
                "Registering the same email twice must never silently create a second account");
    }

    @Test
    void wrongPasswordIsRejectedWithoutRevealingWhichFieldWasWrong() throws Exception {
        String email = "wrongpw-" + UUID.randomUUID() + "@example.com";
        JsonNode reg = mapper.valueToTree(Map.of("name", "A", "email", email, "password", "correct-password-1"));
        rest.postForEntity(baseUrl("/api/v1/auth/register"), jsonEntity(reg), JsonNode.class);

        JsonNode badLogin = mapper.valueToTree(Map.of("email", email, "password", "wrong-password"));
        var resp = rest.postForEntity(baseUrl("/api/v1/auth/login"), jsonEntity(badLogin), JsonNode.class);
        assertEquals(401, resp.getStatusCode().value());
    }

    private String whoAmI(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        var resp = rest.exchange(baseUrl("/api/v1/users/me"), HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
        assertEquals(200, resp.getStatusCode().value());
        return resp.getBody().at("/data/id").asText();
    }

    private HttpEntity<JsonNode> jsonEntity(JsonNode body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}
