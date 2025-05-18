package org.hackit.auth.service;

import static org.hackit.auth.exception.ErrorType.RESOURCE_ALREADY_EXISTS;
import static org.hackit.auth.exception.ProblemDetailBuilder.forStatusAndDetail;
import static org.springframework.http.HttpStatus.CONFLICT;

import java.util.HashMap;
import java.util.List;

import org.hackit.auth.entity.User;
import org.hackit.auth.exception.RestErrorResponseException;
import org.hackit.auth.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserRegistrationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User registerUser(final User user) {
        final var errors = new HashMap<String, List<String>>();

        if (userRepository.existsByEmail(user.getEmail())) {
            errors.put("email", List.of("Email is already taken"));
        }

        if (userRepository.existsByUsername(user.getUsername())) {
            errors.put("username", List.of("Username is already taken"));
        }

        if (!errors.isEmpty()) {
            throw new RestErrorResponseException(
                    forStatusAndDetail(CONFLICT, "Request validation failed")
                            .withProperty("errors", errors)
                            .withErrorType(RESOURCE_ALREADY_EXISTS)
                            .build());
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        return userRepository.save(user);
    }
}
