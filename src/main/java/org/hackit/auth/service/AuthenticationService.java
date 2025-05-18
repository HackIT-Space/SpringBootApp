package org.hackit.auth.service;

import static java.time.Duration.between;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.hackit.auth.entity.RefreshToken;
import org.hackit.auth.entity.User;
import org.hackit.auth.model.AuthTokens;
import org.hackit.auth.repository.RefreshTokenRepository;
import org.hackit.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    @Value("${jwt.refresh-token-ttl}")
    private Duration refreshTokenTtl;

    @Value("${jwt.access-token-ttl}")
    private Duration accessTokenTtl;

    private final AuthenticationManager authenticationManager;

    private final JwtService jwtService;

    private final UserRepository userRepository;

    private final RefreshTokenRepository refreshTokenRepository;

    public AuthTokens authenticate(final String username, final String password) {
        final var authToken =
                UsernamePasswordAuthenticationToken.unauthenticated(username, password);
        final var authentication = authenticationManager.authenticate(authToken);

        final var user =
                userRepository
                        .findByUsername(username)
                        .orElseThrow(
                                () ->
                                        new UsernameNotFoundException(
                                                "User with username [%s] not found"
                                                        .formatted(username)));

        return authenticate(user);
    }

    public AuthTokens authenticate(final User user) {
        final var accessToken = jwtService.generateToken(user.getUsername());

        final var refreshTokenEntity = new RefreshToken();
        refreshTokenEntity.setUser(user);
        refreshTokenEntity.setExpiresAt(Instant.now().plus(refreshTokenTtl));
        refreshTokenRepository.save(refreshTokenEntity);

        return new AuthTokens(
                accessToken,
                refreshTokenEntity.getId().toString(),
                between(Instant.now(), refreshTokenEntity.getExpiresAt()));
    }

    public AuthTokens refreshToken(final String refreshToken) {
        UUID tokenId = validateRefreshTokenFormat(refreshToken);

        final var refreshTokenEntity =
                refreshTokenRepository
                        .findByIdAndExpiresAtAfter(tokenId, Instant.now())
                        .orElseThrow(
                                () ->
                                        new BadCredentialsException(
                                                "Refresh token expired or not found"));

        final var newAccessToken =
                jwtService.generateToken(refreshTokenEntity.getUser().getUsername());

        return new AuthTokens(
                newAccessToken,
                refreshToken,
                between(Instant.now(), refreshTokenEntity.getExpiresAt()));
    }

    public void revokeRefreshToken(String refreshToken) {
        refreshTokenRepository.deleteById(validateRefreshTokenFormat(refreshToken));
    }

    private UUID validateRefreshTokenFormat(final String refreshToken) {
        try {
            return UUID.fromString(refreshToken);
        } catch (IllegalArgumentException e) {
            throw new BadCredentialsException("Invalid refresh token format");
        }
    }

    public Duration getAccessTokenTtl() {
        return accessTokenTtl;
    }

    public Duration getRefreshTokenTtl() {
        return refreshTokenTtl;
    }
}
