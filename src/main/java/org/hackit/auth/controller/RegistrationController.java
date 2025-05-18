package org.hackit.auth.controller;

import org.hackit.auth.dto.RegistrationRequestDto;
import org.hackit.auth.dto.RegistrationResponseDto;
import org.hackit.auth.mapper.UserRegistrationMapper;
import org.hackit.auth.service.EmailVerificationService;
import org.hackit.auth.service.UserRegistrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class RegistrationController {

    private final UserRegistrationService userRegistrationService;

    private final EmailVerificationService emailVerificationService;

    private final UserRegistrationMapper userRegistrationMapper;

    @PostMapping("/sign-up")
    public ResponseEntity<RegistrationResponseDto> registerUser(
            @Valid @RequestBody final RegistrationRequestDto registrationDTO) {
        final var registeredUser =
                userRegistrationService.registerUser(
                        userRegistrationMapper.toEntity(registrationDTO));

        emailVerificationService.sendEmailVerificationOtp(
                registeredUser.getId(), registeredUser.getEmail());

        return ResponseEntity.ok(userRegistrationMapper.toResponseDto(registeredUser));
    }
}
