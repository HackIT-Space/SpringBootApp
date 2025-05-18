package org.hackit.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hackit.auth.config.SecurityTestConfig;
import org.hackit.auth.entity.User;
import org.hackit.auth.mapper.UserRegistrationMapper;
import org.hackit.auth.service.EmailVerificationService;
import org.hackit.auth.service.UserRegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = RegistrationController.class)
@Import(SecurityTestConfig.class)
@ActiveProfiles("test")
class RegistrationControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private UserRegistrationService userRegistrationService;

    @MockitoBean private EmailVerificationService emailVerificationService;

    @MockitoBean private UserRegistrationMapper userRegistrationMapper;

    @Test
    void shouldRegisterUser() throws Exception {
        // given
        String username = "testuser";
        String email = "testuser@example.com";
        String password = "password123";
        User registeredUser = new User();
        registeredUser.setUsername(username);
        registeredUser.setEmail(email);

        when(userRegistrationService.registerUser(any(User.class))).thenReturn(registeredUser);

        when(userRegistrationMapper.toEntity(any())).thenReturn(new User());

        String requestBody =
                String.format(
                        "{\"username\": \"%s\", \"email\": \"%s\", \"password\": \"%s\"}",
                        username, email, password);

        // when/then
        mockMvc.perform(
                        post("/api/auth/sign-up")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                .andExpect(status().isOk());
    }
}
