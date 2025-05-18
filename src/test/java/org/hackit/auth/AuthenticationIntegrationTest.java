package org.hackit.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hackit.auth.model.AuthTokens.REFRESH_TOKEN_COOKIE_NAME;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.hackit.auth.dto.AuthenticationRequestDto;
import org.hackit.auth.entity.User;
import org.hackit.auth.repository.UserRepository;
import org.hackit.config.TestRedisConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfiguration.class)
class AuthenticationIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private UserRepository userRepository;

    @Autowired private PasswordEncoder passwordEncoder;

    @Autowired private ObjectMapper objectMapper;

    private final String username = "testuser";
    private final String email = "testuser@test.com";
    private final String password = "password123";

    @BeforeEach
    void setUp() {
        // Delete test user if exists
        userRepository.findByUsername(username).ifPresent(user -> userRepository.delete(user));

        // Create test user directly
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmailVerified(true);
        userRepository.save(user);
    }

    @AfterEach
    void tearDown() {
        // Clean up test data
        userRepository.findByUsername(username).ifPresent(user -> userRepository.delete(user));
    }

    @Test
    void shouldAuthenticateAndRefreshToken() throws Exception {
        // Authenticate the user
        AuthenticationRequestDto authRequest = new AuthenticationRequestDto(username, password);

        MvcResult result =
                mockMvc.perform(
                                post("/api/auth/sign-in")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(authRequest)))
                        .andExpect(status().isOk())
                        .andExpect(cookie().exists(REFRESH_TOKEN_COOKIE_NAME))
                        .andExpect(jsonPath("$.accessToken").isNotEmpty())
                        .andReturn();

        // Get refresh token from cookie
        String refreshTokenCookie =
                result.getResponse().getCookie(REFRESH_TOKEN_COOKIE_NAME).getValue();
        assertThat(refreshTokenCookie).isNotEmpty();

        // Refresh the token
        mockMvc.perform(post("/api/auth/refresh").cookie(result.getResponse().getCookies()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        // Sign out
        mockMvc.perform(post("/api/auth/sign-out").cookie(result.getResponse().getCookies()))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge(REFRESH_TOKEN_COOKIE_NAME, 0));

        // Try to refresh with the revoked token (should fail)
        mockMvc.perform(post("/api/auth/refresh").cookie(result.getResponse().getCookies()))
                .andExpect(status().isUnauthorized());
    }
}
