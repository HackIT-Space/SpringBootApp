package org.hackit.auth.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Duration;

import org.hackit.auth.config.SecurityTestConfig;
import org.hackit.auth.dto.AuthenticationRequestDto;
import org.hackit.auth.dto.RefreshTokenRequestDto;
import org.hackit.auth.model.AuthTokens;
import org.hackit.auth.service.AuthenticationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(MobileAuthController.class)
@Import(SecurityTestConfig.class)
@ActiveProfiles("test")
class MobileAuthControllerTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private AuthenticationService authenticationService;

    @Test
    void shouldAuthenticateUser() throws Exception {
        // given
        String username = "testuser";
        String password = "password";
        String accessToken = "test.access.token";
        String refreshToken = "test-refresh-token";

        AuthTokens authTokens = new AuthTokens(accessToken, refreshToken, Duration.ofDays(7));
        when(authenticationService.authenticate(username, password)).thenReturn(authTokens);
        when(authenticationService.getAccessTokenTtl()).thenReturn(Duration.ofMinutes(15));
        when(authenticationService.getRefreshTokenTtl()).thenReturn(Duration.ofDays(7));

        AuthenticationRequestDto requestDto = new AuthenticationRequestDto(username, password);

        // when/then
        mockMvc.perform(
                        post("/api/auth/mobile/sign-in")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value(accessToken))
                .andExpect(jsonPath("$.refresh_token").value(refreshToken))
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.access_token_expires_at").isNotEmpty())
                .andExpect(jsonPath("$.refresh_token_expires_at").isNotEmpty());

        verify(authenticationService).authenticate(username, password);
    }

    @Test
    void shouldRefreshToken() throws Exception {
        // given
        String refreshToken = "test-refresh-token";
        String newAccessToken = "new.access.token";

        AuthTokens authTokens = new AuthTokens(newAccessToken, refreshToken, Duration.ofDays(7));
        when(authenticationService.refreshToken(refreshToken)).thenReturn(authTokens);
        when(authenticationService.getAccessTokenTtl()).thenReturn(Duration.ofMinutes(15));
        when(authenticationService.getRefreshTokenTtl()).thenReturn(Duration.ofDays(7));

        RefreshTokenRequestDto requestDto = new RefreshTokenRequestDto(refreshToken);

        // when/then
        mockMvc.perform(
                        post("/api/auth/mobile/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value(newAccessToken))
                .andExpect(jsonPath("$.refresh_token").value(refreshToken))
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.access_token_expires_at").isNotEmpty())
                .andExpect(jsonPath("$.refresh_token_expires_at").isNotEmpty());

        verify(authenticationService).refreshToken(refreshToken);
    }

    @Test
    void shouldRevokeToken() throws Exception {
        // given
        String refreshToken = "test-refresh-token";
        RefreshTokenRequestDto requestDto = new RefreshTokenRequestDto(refreshToken);

        doNothing().when(authenticationService).revokeRefreshToken(anyString());

        // when/then
        mockMvc.perform(
                        post("/api/auth/mobile/sign-out")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isNoContent());

        verify(authenticationService).revokeRefreshToken(refreshToken);
    }
}
