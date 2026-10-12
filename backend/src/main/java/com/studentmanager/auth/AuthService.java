package com.studentmanager.auth;

import com.studentmanager.auth.dto.AuthResponse;
import com.studentmanager.auth.dto.LoginRequest;
import com.studentmanager.auth.dto.RegisterRequest;
import com.studentmanager.common.exception.ApiExceptions.ConflictException;
import com.studentmanager.common.exception.ApiExceptions.UnauthorizedException;
import com.studentmanager.security.JwtService;
import com.studentmanager.security.RefreshTokenGenerator;
import com.studentmanager.user.User;
import com.studentmanager.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final Duration refreshTtl;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenGenerator refreshTokenGenerator,
            @org.springframework.beans.factory.annotation.Value("${app.jwt.refresh-token-days}") long refreshDays
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenGenerator = refreshTokenGenerator;
        this.refreshTtl = Duration.ofDays(refreshDays);
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmailIgnoreCase(req.email())) {
            // Same account, logged in from another device, is exactly the
            // "no duplicate accounts per email" guarantee the person asked
            // for - this is where it's enforced.
            throw new ConflictException("An account with this email already exists.");
        }
        User user = new User();
        user.setName(req.name());
        user.setEmail(req.email());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user = userRepository.save(user);

        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmailIgnoreCase(req.email())
                .orElseThrow(() -> new UnauthorizedException("Email or password is incorrect."));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Email or password is incorrect.");
        }
        return issueTokens(user);
    }

    /**
     * Refresh token ROTATION: every refresh consumes the old token and
     * issues a brand new one. If an already-used (revoked) refresh token is
     * presented again, every session for that user is revoked - that
     * pattern indicates the token was stolen and reused.
     */
    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        String hash = refreshTokenGenerator.hash(rawRefreshToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new UnauthorizedException("Refresh token is invalid."));

        if (stored.getRevokedAt() != null) {
            refreshTokenRepository.revokeAllForUser(stored.getUser().getId(), Instant.now());
            throw new UnauthorizedException("This session was already used elsewhere and has been signed out everywhere for safety.");
        }
        if (!stored.isActive()) {
            throw new UnauthorizedException("Refresh token has expired. Please log in again.");
        }

        stored.setRevokedAt(Instant.now());
        refreshTokenRepository.save(stored);

        return issueTokens(stored.getUser());
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        String hash = refreshTokenGenerator.hash(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(t -> {
            t.setRevokedAt(Instant.now());
            refreshTokenRepository.save(t);
        });
        // Deliberately silent if the token is unknown/already revoked:
        // logging out twice is not an error from the client's point of view.
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.issueAccessToken(user.getId(), user.getRole().name());

        String rawRefresh = refreshTokenGenerator.generate();
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(refreshTokenGenerator.hash(rawRefresh));
        token.setExpiresAt(Instant.now().plus(refreshTtl));
        refreshTokenRepository.save(token);

        return new AuthResponse(
                accessToken,
                rawRefresh,
                jwtService.accessTtlSeconds(),
                new AuthResponse.UserSummary(user.getId(), user.getName(), user.getEmail())
        );
    }
}
