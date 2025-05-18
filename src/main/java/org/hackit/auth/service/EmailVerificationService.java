package org.hackit.auth.service;

import static org.hackit.auth.exception.ErrorType.EMAIL_ALREADY_VERIFIED;
import static org.hackit.auth.exception.ErrorType.EMAIL_VERIFICATION_FAILED;
import static org.hackit.auth.exception.ProblemDetailBuilder.forStatusAndDetail;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

import java.util.UUID;

import org.hackit.auth.entity.User;
import org.hackit.auth.exception.RestErrorResponseException;
import org.hackit.auth.repository.UserRepository;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {

    private final OtpService otpService;

    private final UserRepository userRepository;

    private final JavaMailSender mailSender;

    @Async
    public void sendEmailVerificationOtp(final UUID userId, final String email) {
        final var token = otpService.generateAndStoreOtp(userId);
        final var emailText = "Enter the following email verification code: " + token;

        final var message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Email Verification");
        message.setFrom("System");
        message.setText(emailText);

        mailSender.send(message);
    }

    public void resendEmailVerificationOtp(final String email) {
        userRepository
                .findByEmail(email)
                .filter(user -> !user.isEmailVerified())
                .ifPresentOrElse(
                        user -> sendEmailVerificationOtp(user.getId(), user.getEmail()),
                        () ->
                                log.warn(
                                        "Attempt to resend verification token for non existing or already validated email: [{}]",
                                        email));
    }

    @Transactional
    public User verifyEmailOtp(final String email, final String otp) {
        final var user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(
                                () ->
                                        new RestErrorResponseException(
                                                forStatusAndDetail(
                                                                BAD_REQUEST,
                                                                "Invalid email or token")
                                                        .withErrorType(EMAIL_VERIFICATION_FAILED)
                                                        .build()));

        if (!otpService.isOtpValid(user.getId(), otp)) {
            throw new RestErrorResponseException(
                    forStatusAndDetail(BAD_REQUEST, "Invalid email or token")
                            .withErrorType(EMAIL_VERIFICATION_FAILED)
                            .build());
        }
        otpService.deleteOtp(user.getId());

        if (user.isEmailVerified()) {
            throw new RestErrorResponseException(
                    forStatusAndDetail(BAD_REQUEST, "Email is already verified")
                            .withErrorType(EMAIL_ALREADY_VERIFIED)
                            .build());
        }

        user.setEmailVerified(true);

        return user;
    }
}
