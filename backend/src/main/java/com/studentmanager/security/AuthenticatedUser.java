package com.studentmanager.security;

import java.util.UUID;

/**
 * The authenticated principal attached to the Spring Security context after
 * a valid access token is presented. Controllers and services read the
 * current user's id from here - NEVER from a request body or path
 * parameter - which is what makes "trust the client's userId" impossible.
 */
public record AuthenticatedUser(UUID userId, String role) {
}
