package org.hackit.auth.service;

import static org.hackit.auth.exception.ErrorType.ACCOUNT_UNAVAILABLE;
import static org.hackit.auth.exception.ProblemDetailBuilder.forStatusAndDetail;
import static org.springframework.http.HttpStatus.GONE;

import org.hackit.auth.entity.User;
import org.hackit.auth.exception.RestErrorResponseException;
import org.hackit.auth.repository.UserRepository;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User getUserByUsername(final String username) {
        return userRepository
                .findByUsername(username)
                .orElseThrow(
                        () ->
                                new RestErrorResponseException(
                                        forStatusAndDetail(
                                                        GONE,
                                                        "The user account has been deleted or inactivated")
                                                .withErrorType(ACCOUNT_UNAVAILABLE)
                                                .build()));
    }
}
