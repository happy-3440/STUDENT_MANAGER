package com.studentmanager.auth.dto;

import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        UserSummary user
) {
    public record UserSummary(UUID id, String name, String email) {
    }
}
