package com.fininsight.auth;

import com.fininsight.auth.dto.AuthResponse;
import com.fininsight.auth.dto.LoginRequest;
import com.fininsight.auth.dto.RegisterRequest;
import com.fininsight.auth.dto.UserSummaryResponse;
import com.fininsight.common.exception.BusinessException;
import com.fininsight.user.Role;
import com.fininsight.user.User;
import com.fininsight.user.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Authentication service implementing user registration, credential verification,
 * JWT access token generation, refresh token rotation with reuse detection, and logout.
 */
@Slf4j
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    @Value("${application.security.jwt.cookie-name:refresh_token}")
    private String cookieName;

    @Value("${application.security.jwt.cookie-secure:false}")
    private boolean cookieSecure;

    @Value("${application.security.jwt.cookie-same-site:Strict}")
    private String cookieSameSite;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            AuthenticationManager authenticationManager
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationManager = authenticationManager;
    }

    /**
     * Registers a new user with BCrypt hashed password and unique email validation.
     */
    @Transactional
    public UserSummaryResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BusinessException("An account with this email already exists", HttpStatus.CONFLICT);
        }

        String encodedPassword = passwordEncoder.encode(request.password());
        User user = new User(
                normalizedEmail,
                encodedPassword,
                request.firstName().trim(),
                request.lastName().trim(),
                Role.ROLE_USER
        );

        User savedUser = userRepository.save(user);
        log.info("User registered successfully with id: {}", savedUser.getId());

        return UserSummaryResponse.fromUser(savedUser);
    }

    /**
     * Authenticates user credentials, issues a short-lived JWT access token in the response body,
     * and sets an HttpOnly refresh token cookie.
     */
    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletResponse response) {
        String normalizedEmail = request.email().trim().toLowerCase();

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.password())
            );

            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            User user = userRepository.findById(principal.getId())
                    .orElseThrow(() -> new BusinessException("Invalid credentials", HttpStatus.UNAUTHORIZED));

            if (!user.isEnabled()) {
                throw new BusinessException("User account is disabled", HttpStatus.FORBIDDEN);
            }

            return createAuthenticationSession(user, response);

        } catch (BadCredentialsException ex) {
            log.warn("Authentication failed for email {}: Invalid credentials", normalizedEmail);
            throw new BusinessException("Invalid credentials", HttpStatus.UNAUTHORIZED);
        } catch (DisabledException ex) {
            log.warn("Authentication failed for email {}: Account disabled", normalizedEmail);
            throw new BusinessException("User account is disabled", HttpStatus.FORBIDDEN);
        }
    }

    /**
     * Refreshes access token using the HttpOnly refresh token cookie.
     * Implements Automatic Token Rotation and Token Reuse Detection.
     */
    @Transactional
    public AuthResponse refresh(String rawRefreshToken, HttpServletResponse response) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new BusinessException("Refresh token is required", HttpStatus.UNAUTHORIZED);
        }

        String tokenHash = jwtTokenProvider.hashToken(rawRefreshToken);
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BusinessException("Invalid refresh token", HttpStatus.UNAUTHORIZED));

        // Token Reuse Detection: If a previously revoked token is re-submitted,
        // it indicates a potential breach. Revoke all active sessions for this user.
        if (storedToken.isRevoked()) {
            log.warn("SECURITY ALERT: Compromised refresh token reuse detected for user id: {}. Revoking all sessions.",
                    storedToken.getUserId());
            refreshTokenRepository.revokeAllByUserId(storedToken.getUserId());
            clearRefreshTokenCookie(response);
            throw new BusinessException(
                    "Token reuse detected. All active sessions have been terminated for security. Please log in again.",
                    HttpStatus.UNAUTHORIZED
            );
        }

        if (storedToken.isExpired()) {
            log.warn("Expired refresh token for user id: {}", storedToken.getUserId());
            clearRefreshTokenCookie(response);
            throw new BusinessException("Refresh token has expired. Please log in again.", HttpStatus.UNAUTHORIZED);
        }

        User user = userRepository.findById(storedToken.getUserId())
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.UNAUTHORIZED));

        if (!user.isEnabled()) {
            throw new BusinessException("User account is disabled", HttpStatus.FORBIDDEN);
        }

        // Rotate Token: Revoke current refresh token and issue a fresh pair
        storedToken.revoke();
        refreshTokenRepository.save(storedToken);

        return createAuthenticationSession(user, response);
    }

    /**
     * Logs out the user by revoking the refresh token in database and clearing the HttpOnly cookie.
     */
    @Transactional
    public void logout(String rawRefreshToken, HttpServletResponse response) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            String tokenHash = jwtTokenProvider.hashToken(rawRefreshToken);
            refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
                token.revoke();
                refreshTokenRepository.save(token);
                log.info("Refresh token revoked successfully for user id: {}", token.getUserId());
            });
        }
        clearRefreshTokenCookie(response);
    }

    /**
     * Fetches current authenticated user details.
     */
    @Transactional(readOnly = true)
    public UserSummaryResponse getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found", HttpStatus.NOT_FOUND));
        return UserSummaryResponse.fromUser(user);
    }

    private AuthResponse createAuthenticationSession(User user, HttpServletResponse response) {
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        String rawRefreshToken = jwtTokenProvider.generateRawRefreshToken();
        String tokenHash = jwtTokenProvider.hashToken(rawRefreshToken);

        Instant expiresAt = Instant.now().plusMillis(jwtTokenProvider.getRefreshTokenExpirationMs());
        RefreshToken refreshToken = new RefreshToken(user.getId(), tokenHash, expiresAt);
        refreshTokenRepository.save(refreshToken);

        long maxAgeSeconds = jwtTokenProvider.getRefreshTokenExpirationMs() / 1000;
        setRefreshTokenCookie(response, rawRefreshToken, maxAgeSeconds);

        return AuthResponse.of(
                accessToken,
                jwtTokenProvider.getAccessTokenExpirationSeconds(),
                UserSummaryResponse.fromUser(user)
        );
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String value, long maxAgeSeconds) {
        ResponseCookie cookie = ResponseCookie.from(cookieName, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/api/v1/auth")
                .maxAge(maxAgeSeconds)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/api/v1/auth")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
