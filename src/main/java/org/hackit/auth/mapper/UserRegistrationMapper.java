package org.hackit.auth.mapper;

import org.hackit.auth.dto.RegistrationRequestDto;
import org.hackit.auth.dto.RegistrationResponseDto;
import org.hackit.auth.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserRegistrationMapper {

    public User toEntity(final RegistrationRequestDto registrationRequestDto) {
        final var user = new User();

        user.setEmail(registrationRequestDto.email());
        user.setUsername(registrationRequestDto.username());
        user.setPassword(registrationRequestDto.password());

        return user;
    }

    public RegistrationResponseDto toResponseDto(final User user) {
        return new RegistrationResponseDto(user.getEmail(), user.getUsername());
    }
}
