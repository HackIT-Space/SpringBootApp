package org.hackit.auth.mapper;

import org.hackit.auth.dto.UserProfileDto;
import org.hackit.auth.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserProfileDto toUserProfileDto(final User user) {
        return new UserProfileDto(user.getEmail(), user.getUsername(), user.isEmailVerified());
    }
}
