package org.hackit.auth.dto;

import java.time.Duration;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "DTO ответа аутентификации для мобильных клиентов")
public record MobileAuthResponseDto(
        @Schema(description = "Токен доступа (JWT)") @JsonProperty("access_token")
                String accessToken,
        @Schema(description = "Refresh токен для обновления access token")
                @JsonProperty("refresh_token")
                String refreshToken,
        @Schema(description = "Время истечения токена доступа (в миллисекундах с эпохи)")
                @JsonProperty("access_token_expires_at")
                long accessTokenExpiresAt,
        @Schema(description = "Время истечения refresh токена (в миллисекундах с эпохи)")
                @JsonProperty("refresh_token_expires_at")
                long refreshTokenExpiresAt,
        @Schema(description = "Тип токена") @JsonProperty("token_type") String tokenType) {
    public static MobileAuthResponseDto from(
            String accessToken,
            String refreshToken,
            Instant now,
            Duration accessTokenTtl,
            Duration refreshTokenTtl) {
        return new MobileAuthResponseDto(
                accessToken,
                refreshToken,
                now.plus(accessTokenTtl).toEpochMilli(),
                now.plus(refreshTokenTtl).toEpochMilli(),
                "Bearer");
    }
}
