package org.hackit.auth.controller;

import static org.hackit.auth.model.AuthTokens.REFRESH_TOKEN_COOKIE_NAME;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.hackit.auth.config.SecurityTestConfig;
import org.hackit.auth.model.AuthTokens;
import org.hackit.auth.service.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockCookie;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthenticationController.class)
@Import(SecurityTestConfig.class)
@ActiveProfiles("test")
class AuthenticationControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AuthenticationService authenticationService;

    private static final String USERNAME = "testuser";
    private static final String PASSWORD = "password";
    private static final String ACCESS_TOKEN = "mock.access.token";
    private static final String REFRESH_TOKEN = "mock-refresh-token";
    private static final Duration TOKEN_TTL = Duration.of(7, ChronoUnit.DAYS);

    @BeforeEach
    void setUp() {
        when(authenticationService.authenticate(eq(USERNAME), eq(PASSWORD)))
                .thenReturn(new AuthTokens(ACCESS_TOKEN, REFRESH_TOKEN, TOKEN_TTL));

        when(authenticationService.refreshToken(eq(REFRESH_TOKEN)))
                .thenReturn(new AuthTokens(ACCESS_TOKEN, REFRESH_TOKEN, TOKEN_TTL));
    }

    @Test
    void shouldAuthenticateUser() throws Exception {
        // given
        String requestBody =
                String.format("{\"username\": \"%s\", \"password\": \"%s\"}", USERNAME, PASSWORD);

        // when/then
        mockMvc.perform(
                        post("/api/auth/sign-in")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(cookie().exists(REFRESH_TOKEN_COOKIE_NAME))
                .andExpect(cookie().value(REFRESH_TOKEN_COOKIE_NAME, REFRESH_TOKEN))
                .andExpect(jsonPath("$.accessToken").value(ACCESS_TOKEN));

        verify(authenticationService).authenticate(USERNAME, PASSWORD);
    }

    @Test
    void shouldRefreshToken() throws Exception {
        // when/then
        mockMvc.perform(
                        post("/api/auth/refresh")
                                .cookie(new MockCookie(REFRESH_TOKEN_COOKIE_NAME, REFRESH_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(ACCESS_TOKEN));

        verify(authenticationService).refreshToken(REFRESH_TOKEN);
    }

    @Test
    void shouldRevokeToken() throws Exception {
        // when/then
        mockMvc.perform(
                        post("/api/auth/sign-out")
                                .cookie(new MockCookie(REFRESH_TOKEN_COOKIE_NAME, REFRESH_TOKEN)))
                .andExpect(status().isNoContent())
                .andExpect(cookie().exists(REFRESH_TOKEN_COOKIE_NAME))
                .andExpect(cookie().maxAge(REFRESH_TOKEN_COOKIE_NAME, 0));

        verify(authenticationService).revokeRefreshToken(REFRESH_TOKEN);
    }
}
