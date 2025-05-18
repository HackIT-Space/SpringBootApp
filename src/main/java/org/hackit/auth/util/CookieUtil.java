package org.hackit.auth.util;

import java.time.Duration;

import org.springframework.http.ResponseCookie;

public class CookieUtil {

    public static ResponseCookie addCookie(
            final String name, final String value, final Duration duration) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(duration)
                .build();
    }

    public static ResponseCookie removeCookie(final String name) {
        return ResponseCookie.from(name)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
                .build();
    }
}
