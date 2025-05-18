package org.hackit.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "DTO для запроса обновления токена")
public record RefreshTokenRequestDto(
        @Schema(description = "Refresh токен для обновления", required = true)
                @JsonProperty("refresh_token")
                @NotBlank(message = "Refresh token не может быть пустым")
                String refreshToken) {}
