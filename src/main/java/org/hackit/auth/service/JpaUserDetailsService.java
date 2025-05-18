package org.hackit.auth.service;

import static org.hackit.auth.exception.ErrorType.EMAIL_VERIFICATION_REQUIRED;
import static org.hackit.auth.exception.ProblemDetailBuilder.forStatusAndDetail;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import org.hackit.auth.exception.RestErrorResponseException;
import org.hackit.auth.repository.UserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JpaUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(final String username) {
        return userRepository
                .findByUsername(username)
                .map(
                        user -> {
                            if (!user.isEmailVerified()) {
                                throw new RestErrorResponseException(
                                        forStatusAndDetail(
                                                        UNAUTHORIZED, "Email verification required")
                                                .withProperty("email", user.getEmail())
                                                .withErrorType(EMAIL_VERIFICATION_REQUIRED)
                                                .build());
                            }
                            return User.builder()
                                    .username(username)
                                    .password(user.getPassword())
                                    .build();
                        })
                .orElseThrow(
                        () ->
                                new UsernameNotFoundException(
                                        "User with username [%s] not found".formatted(username)));
    }
}
