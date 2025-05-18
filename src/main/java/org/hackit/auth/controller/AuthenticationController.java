package org.hackit.auth.controller;

import static org.hackit.auth.model.AuthTokens.REFRESH_TOKEN_COOKIE_NAME;
import static org.hackit.auth.util.CookieUtil.addCookie;
import static org.hackit.auth.util.CookieUtil.removeCookie;
import static org.springframework.http.HttpHeaders.SET_COOKIE;

import org.hackit.auth.dto.ApiErrorResponse;
import org.hackit.auth.dto.AuthenticationRequestDto;
import org.hackit.auth.dto.AuthenticationResponseDto;
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
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "API для аутентификации и управления токенами")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/sign-in")
    @Operation(
            summary = "Аутентификация пользователя",
            description =
                    "Аутентифицирует пользователя и возвращает access token, а также устанавливает refresh token в cookie")
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "200", description = "Успешная аутентификация"),
                @ApiResponse(
                        responseCode = "401",
                        description = "Неверные учетные данные",
                        content =
                                @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
            })
    public ResponseEntity<AuthenticationResponseDto> authenticate(
            @Valid @RequestBody final AuthenticationRequestDto requestDto) {
        final var authTokens =
                authenticationService.authenticate(requestDto.username(), requestDto.password());

        return ResponseEntity.ok()
                .header(
                        SET_COOKIE,
                        addCookie(
                                        REFRESH_TOKEN_COOKIE_NAME,
                                        authTokens.refreshToken(),
                                        authTokens.refreshTokenTtl())
                                .toString())
                .body(new AuthenticationResponseDto(authTokens.accessToken()));
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Обновление токена доступа",
            description = "Обновляет access token на основе refresh token, хранящегося в cookie")
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "200", description = "Токен успешно обновлен"),
                @ApiResponse(
                        responseCode = "401",
                        description = "Невалидный или истекший refresh token",
                        content =
                                @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
            })
    public ResponseEntity<AuthenticationResponseDto> refreshToken(
            @CookieValue(REFRESH_TOKEN_COOKIE_NAME) final String refreshToken) {
        final var authTokens = authenticationService.refreshToken(refreshToken);

        return ResponseEntity.ok()
                .header(
                        SET_COOKIE,
                        addCookie(
                                        REFRESH_TOKEN_COOKIE_NAME,
                                        authTokens.refreshToken(),
                                        authTokens.refreshTokenTtl())
                                .toString())
                .body(new AuthenticationResponseDto(authTokens.accessToken()));
    }

    @PostMapping("/sign-out")
    @Operation(
            summary = "Выход из системы",
            description = "Отзывает refresh token и очищает cookie")
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "204", description = "Успешный выход из системы"),
                @ApiResponse(
                        responseCode = "400",
                        description = "Отсутствует cookie с refresh token",
                        content =
                                @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
            })
    public ResponseEntity<Void> revokeToken(
            @CookieValue(REFRESH_TOKEN_COOKIE_NAME) final String refreshToken) {
        authenticationService.revokeRefreshToken(refreshToken);

        return ResponseEntity.noContent()
                .header(SET_COOKIE, removeCookie(REFRESH_TOKEN_COOKIE_NAME).toString())
                .build();
    }
}
