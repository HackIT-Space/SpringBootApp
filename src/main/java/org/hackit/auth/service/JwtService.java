package org.hackit.auth.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class JwtService {

    private final String issuer;

    @Value("${jwt.audience:hackit-api}")
    private String audience;

    private final Duration ttl;

    private final JwtEncoder jwtEncoder;

    public String generateToken(final String username) {
        final var issuedAt = Instant.now();
        final var jwtId = UUID.randomUUID().toString();

        final var claimsSet =
                JwtClaimsSet.builder()
                        .id(jwtId)
                        .subject(username)
                        .issuer(issuer)
                        .audience(List.of(audience))
                        .issuedAt(issuedAt)
                        .notBefore(issuedAt)
                        .expiresAt(issuedAt.plus(ttl))
                        .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claimsSet)).getTokenValue();
    }

    public Duration getTokenTtl() {
        return ttl;
    }
}
