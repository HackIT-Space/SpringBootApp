package org.hackit.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock private JwtEncoder jwtEncoder;

    private JwtService jwtService;

    private final String issuer = "http://test-app.com";
    private final String audience = "test-api";
    private final Duration ttl = Duration.ofMinutes(30);
    private final String username = "testuser";
    private final String expectedToken = "test.jwt.token";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(issuer, ttl, jwtEncoder);
        ReflectionTestUtils.setField(jwtService, "audience", audience);

        Jwt jwt = mock(Jwt.class);
        when(jwt.getTokenValue()).thenReturn(expectedToken);
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwt);
    }

    @Test
    void shouldGenerateToken() {
        // when
        String token = jwtService.generateToken(username);

        // then
        assertThat(token).isEqualTo(expectedToken);

        // Verify that encode was called with the correct parameters
        var captor = org.mockito.ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(jwtEncoder).encode(captor.capture());

        JwtClaimsSet claims = captor.getValue().getClaims();
        assertThat(claims.getSubject()).isEqualTo(username);
        assertThat(claims.getIssuer().toString()).isEqualTo(issuer);

        Instant issuedAt = claims.getIssuedAt();
        Instant expiresAt = claims.getExpiresAt();

        assertThat(issuedAt).isNotNull();
        assertThat(expiresAt).isNotNull();
        assertThat(Duration.between(issuedAt, expiresAt)).isEqualTo(ttl);
        assertThat(claims.getId()).isNotNull();
        assertThat(claims.getAudience()).contains(audience);
    }
}
