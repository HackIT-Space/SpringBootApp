package org.hackit.auth.controller;

import static org.hackit.auth.model.AuthTokens.REFRESH_TOKEN_COOKIE_NAME;
import static org.hackit.auth.util.CookieUtil.addCookie;
import static org.springframework.http.HttpHeaders.SET_COOKIE;

import org.hackit.auth.dto.AuthenticationResponseDto;
import org.hackit.auth.dto.EmailVerificationRequestDto;
import org.hackit.auth.service.AuthenticationService;
import org.hackit.auth.service.EmailVerificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    private final AuthenticationService authenticationService;

    @PostMapping("/request-verification-email")
    public ResponseEntity<Void> resendVerificationOtp(@RequestParam final String email) {
        emailVerificationService.resendEmailVerificationOtp(email);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/verify-email")
    public ResponseEntity<AuthenticationResponseDto> verifyOtp(
            @Valid @RequestBody final EmailVerificationRequestDto requestDto) {
        final var verifiedUser =
                emailVerificationService.verifyEmailOtp(requestDto.email(), requestDto.otp());
        final var authTokens = authenticationService.authenticate(verifiedUser);

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
}
