package org.hackit.auth.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.hackit.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByIdAndExpiresAtAfter(UUID id, Instant expiresAt);
}
