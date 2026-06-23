package com.AQARK.AQARK_RENT_MANAGEMENT.Security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLContext;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.List;

/**
 * Creates and updates users in Keycloak via its Admin REST API. This replaces the previous
 * external SAS (Spring Authorization Server) integration: AQARK self-registration now provisions
 * the identity directly in Keycloak, which is also the OIDC provider the BFF login flow uses.
 *
 * <p>Uses the JDK {@link HttpClient} (not the reactor-netty {@code WebClient}) on purpose: the
 * JDK client honours the JVM default {@link SSLContext}, which {@link KeycloakTlsTrustConfig}
 * configures to trust Keycloak's dev CA. Reactor-netty would need its own SSL context.
 *
 * <p>An admin token is obtained with the {@code admin-cli} client via the password grant against
 * the configured admin realm (dev default {@code master}). For production, prefer a dedicated
 * service-account client granted {@code realm-management:manage-users}.
 */
@Slf4j
@Service
public class KeycloakAdminUserService {

    @Value("${keycloak.base-url:https://localhost:8443}")
    private String baseUrl;

    @Value("${keycloak.target-realm:AQARK-realm}")
    private String targetRealm;

    @Value("${keycloak.admin.realm:master}")
    private String adminRealm;

    @Value("${keycloak.admin.client-id:admin-cli}")
    private String adminClientId;

    @Value("${keycloak.admin.username:admin}")
    private String adminUsername;

    @Value("${keycloak.admin.password:admin_pass}")
    private String adminPassword;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private HttpClient httpClient() {
        try {
            return HttpClient.newBuilder()
                    .sslContext(SSLContext.getDefault())
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Default SSLContext unavailable", e);
        }
    }

    /**
     * Creates a Keycloak user with a permanent password and a {@code refId} attribute linking
     * back to the AQARK user. Returns the Keycloak user id (UUID).
     *
     * @throws KeycloakUserConflictException if the email/username already exists (HTTP 409)
     * @throws KeycloakUserException         for password-policy violations and other failures
     */
    public String createUser(String email, String firstName, String lastName,
                             String password, Long refId, List<String> realmRoles) {
        String token = obtainAdminToken();

        ObjectNode user = objectMapper.createObjectNode();
        user.put("username", email);
        user.put("email", email);
        user.put("firstName", firstName);
        user.put("lastName", lastName);
        user.put("enabled", true);
        user.put("emailVerified", true);

        ObjectNode attributes = user.putObject("attributes");
        ArrayNode refIds = attributes.putArray("refId");
        refIds.add(String.valueOf(refId));

        ArrayNode credentials = user.putArray("credentials");
        ObjectNode credential = credentials.addObject();
        credential.put("type", "password");
        credential.put("value", password);
        credential.put("temporary", false);

        HttpResponse<String> response = send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/admin/realms/" + targetRealm + "/users"))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(toJson(user)))
                .build());

        int status = response.statusCode();
        if (status == 201) {
            String location = response.headers().firstValue("Location").orElse("");
            String id = location.substring(location.lastIndexOf('/') + 1);
            assignRealmRoles(token, id, realmRoles);
            log.info("Created Keycloak user, refId={}, keycloakId={}, roles={}", refId, id, realmRoles);
            return id;
        }

        if (status == 409) {
            throw new KeycloakUserConflictException(
                    "An account with this email already exists.");
        }

        throw new KeycloakUserException(
                "Keycloak user creation failed (HTTP " + status + "): " + extractError(response.body()));
    }

    /**
     * Grants the given realm roles to the user. Each role must already exist in the realm
     * (e.g. ROLE_LANDLORD, ROLE_TENANT); unknown roles are skipped with a warning.
     */
    private void assignRealmRoles(String token, String userId, List<String> realmRoles) {
        if (realmRoles == null || realmRoles.isEmpty()) {
            return;
        }

        ArrayNode payload = objectMapper.createArrayNode();
        for (String roleName : realmRoles) {
            HttpResponse<String> roleResp = send(HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/admin/realms/" + targetRealm
                            + "/roles/" + enc(roleName)))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build());
            if (roleResp.statusCode() != 200) {
                log.warn("Skipping unknown realm role '{}' (HTTP {})", roleName, roleResp.statusCode());
                continue;
            }
            try {
                JsonNode role = objectMapper.readTree(roleResp.body());
                ObjectNode entry = payload.addObject();
                entry.put("id", role.path("id").asText());
                entry.put("name", role.path("name").asText());
            } catch (Exception e) {
                throw new KeycloakUserException("Failed to parse realm role '" + roleName + "'", e);
            }
        }

        if (payload.isEmpty()) {
            return;
        }

        HttpResponse<String> assignResp = send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/admin/realms/" + targetRealm
                        + "/users/" + userId + "/role-mappings/realm"))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(toJson(payload)))
                .build());
        if (assignResp.statusCode() >= 300) {
            throw new KeycloakUserException(
                    "Failed to assign realm roles " + realmRoles + " (HTTP " + assignResp.statusCode()
                            + "): " + extractError(assignResp.body()));
        }
    }

    /**
     * Resets a Keycloak user's password (and email) by looking them up via the {@code refId}
     * attribute. Used by the profile-update flow.
     */
    public void updateUser(Long refId, String email, String password) {
        String token = obtainAdminToken();
        String userId = findUserIdByRefId(token, refId);
        if (userId == null) {
            throw new KeycloakUserException("No Keycloak user found for refId=" + refId);
        }

        ObjectNode update = objectMapper.createObjectNode();
        if (email != null && !email.isBlank()) {
            update.put("email", email);
            update.put("username", email);
        }
        HttpResponse<String> putResp = send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/admin/realms/" + targetRealm + "/users/" + userId))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(toJson(update)))
                .build());
        if (putResp.statusCode() >= 300) {
            throw new KeycloakUserException(
                    "Keycloak user update failed (HTTP " + putResp.statusCode() + "): "
                            + extractError(putResp.body()));
        }

        if (password != null && !password.isBlank()) {
            ObjectNode credential = objectMapper.createObjectNode();
            credential.put("type", "password");
            credential.put("value", password);
            credential.put("temporary", false);
            HttpResponse<String> pwResp = send(HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/admin/realms/" + targetRealm
                            + "/users/" + userId + "/reset-password"))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(toJson(credential)))
                    .build());
            if (pwResp.statusCode() >= 300) {
                throw new KeycloakUserException(
                        "Keycloak password reset failed (HTTP " + pwResp.statusCode() + "): "
                                + extractError(pwResp.body()));
            }
        }
        log.info("Updated Keycloak user, refId={}, keycloakId={}", refId, userId);
    }

    private String findUserIdByRefId(String token, Long refId) {
        HttpResponse<String> response = send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/admin/realms/" + targetRealm
                        + "/users?q=" + enc("refId:" + refId) + "&exact=true"))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build());
        try {
            JsonNode arr = objectMapper.readTree(response.body());
            if (arr.isArray() && !arr.isEmpty()) {
                return arr.get(0).path("id").asText(null);
            }
        } catch (Exception e) {
            throw new KeycloakUserException("Failed to parse Keycloak user lookup response", e);
        }
        return null;
    }

    private String obtainAdminToken() {
        String form = "grant_type=password"
                + "&client_id=" + enc(adminClientId)
                + "&username=" + enc(adminUsername)
                + "&password=" + enc(adminPassword);

        HttpResponse<String> response = send(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/realms/" + adminRealm
                        + "/protocol/openid-connect/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build());

        if (response.statusCode() != 200) {
            throw new KeycloakUserException(
                    "Failed to obtain Keycloak admin token (HTTP " + response.statusCode() + ")");
        }
        try {
            return objectMapper.readTree(response.body()).path("access_token").asText();
        } catch (Exception e) {
            throw new KeycloakUserException("Failed to parse Keycloak admin token response", e);
        }
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return httpClient().send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new KeycloakUserException("Keycloak request failed: " + e.getMessage(), e);
        }
    }

    private String toJson(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            throw new KeycloakUserException("Failed to serialize Keycloak request body", e);
        }
    }

    /** Pulls a human-readable message out of a Keycloak error body, falling back to the raw body. */
    private String extractError(String body) {
        if (body == null || body.isBlank()) {
            return "no response body";
        }
        try {
            JsonNode node = objectMapper.readTree(body);
            if (node.hasNonNull("errorMessage")) return node.get("errorMessage").asText();
            if (node.hasNonNull("error_description")) return node.get("error_description").asText();
            if (node.hasNonNull("error")) return node.get("error").asText();
        } catch (Exception ignored) {
            // not JSON — return as-is
        }
        return body;
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /** Email/username already taken in Keycloak (HTTP 409). */
    public static class KeycloakUserConflictException extends RuntimeException {
        public KeycloakUserConflictException(String message) {
            super(message);
        }
    }

    /** Any other Keycloak provisioning failure (password policy, transport, etc.). */
    public static class KeycloakUserException extends RuntimeException {
        public KeycloakUserException(String message) {
            super(message);
        }

        public KeycloakUserException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
