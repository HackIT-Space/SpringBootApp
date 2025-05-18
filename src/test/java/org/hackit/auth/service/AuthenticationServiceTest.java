package org.hackit.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.hackit.auth.entity.RefreshToken;
import org.hackit.auth.entity.User;
import org.hackit.auth.model.AuthTokens;
import org.hackit.auth.repository.RefreshTokenRepository;
import org.hackit.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock private AuthenticationManager authenticationManager;

    @Mock private JwtService jwtService;

    @Mock private UserRepository userRepository;

    @Mock private RefreshTokenRepository refreshTokenRepository;

    @Mock private Authentication authentication;

    private AuthenticationService authenticationService;

    private final String username = "testuser";
    private final String password = "password";
    private final String accessToken = "mock.access.token";
    private final UUID refreshTokenId = UUID.randomUUID();
    private final User user = new User();

    @BeforeEach
    void setUp() {
        authenticationService =
                new AuthenticationService(
                        authenticationManager, jwtService, userRepository, refreshTokenRepository);

        // Set the refresh token TTL using reflection (normally set by @Value)
        try {
            var refreshTokenTtlField =
                    AuthenticationService.class.getDeclaredField("refreshTokenTtl");
            refreshTokenTtlField.setAccessible(true);
            refreshTokenTtlField.set(authenticationService, Duration.ofDays(7));
            refreshTokenTtlField.setAccessible(false);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set refreshTokenTtl field", e);
        }

        // Set up user
        user.setUsername(username);
    }

    @Test
    void shouldAuthenticateWithUsernameAndPassword() {
        // given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(jwtService.generateToken(username)).thenReturn(accessToken);

        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        when(refreshTokenRepository.save(tokenCaptor.capture()))
                .thenAnswer(
                        invocation -> {
                            RefreshToken token = invocation.getArgument(0);
                            // Mock the UUID generation to be predictable in tests
                            try {
                                var idField = RefreshToken.class.getDeclaredField("id");
                                idField.setAccessible(true);
                                idField.set(token, refreshTokenId);
                                idField.setAccessible(false);
                            } catch (Exception e) {
                                throw new RuntimeException("Failed to set id field", e);
                            }
                            return token;
                        });

        // when
        AuthTokens result = authenticationService.authenticate(username, password);

        // then
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findByUsername(username);
        verify(jwtService).generateToken(username);
        verify(refreshTokenRepository).save(any(RefreshToken.class));

        RefreshToken savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getUser()).isEqualTo(user);
        assertThat(savedToken.getExpiresAt()).isAfter(Instant.now());

        assertThat(result.accessToken()).isEqualTo(accessToken);
        assertThat(result.refreshToken()).isEqualTo(refreshTokenId.toString());
    }

    @Test
    void shouldThrowExceptionWhenUserNotFound() {
        // given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // when/then
        assertThatThrownBy(() -> authenticationService.authenticate(username, password))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining(username);
    }

    @Test
    void shouldAuthenticate() {
        // given
        when(jwtService.generateToken(username)).thenReturn(accessToken);

        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        when(refreshTokenRepository.save(tokenCaptor.capture()))
                .thenAnswer(
                        invocation -> {
                            RefreshToken token = invocation.getArgument(0);
                            // Mock the UUID generation
                            try {
                                var idField = RefreshToken.class.getDeclaredField("id");
                                idField.setAccessible(true);
                                idField.set(token, refreshTokenId);
                                idField.setAccessible(false);
                            } catch (Exception e) {
                                throw new RuntimeException("Failed to set id field", e);
                            }
                            return token;
                        });

        // when
        AuthTokens result = authenticationService.authenticate(user);

        // then
        verify(jwtService).generateToken(username);
        verify(refreshTokenRepository).save(any(RefreshToken.class));

        RefreshToken savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getUser()).isEqualTo(user);
        assertThat(savedToken.getExpiresAt()).isAfter(Instant.now());

        assertThat(result.accessToken()).isEqualTo(accessToken);
        assertThat(result.refreshToken()).isEqualTo(refreshTokenId.toString());
    }

    @Test
    void shouldRefreshToken() {
        // given
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setExpiresAt(Instant.now().plus(Duration.ofDays(7)));

        // Set the id field using reflection
        try {
            var idField = RefreshToken.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(token, refreshTokenId);
            idField.setAccessible(false);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set id field", e);
        }

        when(refreshTokenRepository.findByIdAndExpiresAtAfter(
                        eq(refreshTokenId), any(Instant.class)))
                .thenReturn(Optional.of(token));
        when(jwtService.generateToken(username)).thenReturn(accessToken);

        // when
        AuthTokens result = authenticationService.refreshToken(refreshTokenId.toString());

        // then
        verify(refreshTokenRepository)
                .findByIdAndExpiresAtAfter(eq(refreshTokenId), any(Instant.class));
        verify(jwtService).generateToken(username);

        assertThat(result.accessToken()).isEqualTo(accessToken);
        assertThat(result.refreshToken()).isEqualTo(refreshTokenId.toString());
    }

    @Test
    void shouldThrowExceptionWhenRefreshingInvalidToken() {
        // given
        String invalidToken = "invalid-token-format";

        // when/then
        assertThatThrownBy(() -> authenticationService.refreshToken(invalidToken))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid refresh token format");
    }

    @Test
    void shouldThrowExceptionWhenRefreshingExpiredToken() {
        // given
        when(refreshTokenRepository.findByIdAndExpiresAtAfter(
                        eq(refreshTokenId), any(Instant.class)))
                .thenReturn(Optional.empty());

        // when/then
        assertThatThrownBy(() -> authenticationService.refreshToken(refreshTokenId.toString()))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Refresh token expired or not found");
    }

    @Test
    void shouldRevokeRefreshToken() {
        // given
        String refreshToken = refreshTokenId.toString();

        // when
        authenticationService.revokeRefreshToken(refreshToken);

        // then
        verify(refreshTokenRepository).deleteById(refreshTokenId);
    }

    @Test
    void shouldThrowExceptionWhenRevokingInvalidToken() {
        // given
        String invalidToken = "invalid-token-format";

        // when/then
        assertThatThrownBy(() -> authenticationService.revokeRefreshToken(invalidToken))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid refresh token format");
    }
}
