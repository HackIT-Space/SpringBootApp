package org.hackit.auth.controller;

import java.time.Instant;

import org.hackit.auth.dto.ApiErrorResponse;
import org.hackit.auth.dto.AuthenticationRequestDto;
import org.hackit.auth.dto.MobileAuthResponseDto;
import org.hackit.auth.dto.RefreshTokenRequestDto;
import org.hackit.auth.service.AuthenticationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth/mobile")
@RequiredArgsConstructor
@Tag(name = "Mobile Authentication", description = "API для аутентификации мобильных клиентов")
public class MobileAuthController {

    private final AuthenticationService authenticationService;

    @PostMapping("/sign-in")
    @Operation(
            summary = "Аутентификация мобильного пользователя",
            description = "Аутентифицирует пользователя и возвращает access и refresh токены")
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "200", description = "Успешная аутентификация"),
                @ApiResponse(
                        responseCode = "401",
                        description = "Неверные учетные данные",
                        content =
                                @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
            })
    public ResponseEntity<MobileAuthResponseDto> authenticate(
            @Valid @RequestBody final AuthenticationRequestDto requestDto) {
        final var authTokens =
                authenticationService.authenticate(requestDto.username(), requestDto.password());

        final var response =
                MobileAuthResponseDto.from(
                        authTokens.accessToken(),
                        authTokens.refreshToken(),
                        Instant.now(),
                        authenticationService.getAccessTokenTtl(),
                        authenticationService.getRefreshTokenTtl());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Обновление токена доступа для мобильного клиента",
            description = "Обновляет access token на основе refresh token")
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "200", description = "Токен успешно обновлен"),
                @ApiResponse(
                        responseCode = "401",
                        description = "Невалидный или истекший refresh token",
                        content =
                                @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
            })
    public ResponseEntity<MobileAuthResponseDto> refreshToken(
            @Valid @RequestBody final RefreshTokenRequestDto requestDto) {
        final var authTokens = authenticationService.refreshToken(requestDto.refreshToken());

        final var response =
                MobileAuthResponseDto.from(
                        authTokens.accessToken(),
                        authTokens.refreshToken(),
                        Instant.now(),
                        authenticationService.getAccessTokenTtl(),
                        authenticationService.getRefreshTokenTtl());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/sign-out")
    @Operation(
            summary = "Выход из системы для мобильного клиента",
            description = "Отзывает refresh token")
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "204", description = "Успешный выход из системы"),
                @ApiResponse(
                        responseCode = "400",
                        description = "Отсутствует refresh token",
                        content =
                                @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
            })
    public ResponseEntity<Void> revokeToken(
            @Valid @RequestBody final RefreshTokenRequestDto requestDto) {
        authenticationService.revokeRefreshToken(requestDto.refreshToken());

        return ResponseEntity.noContent().build();
    }
}
