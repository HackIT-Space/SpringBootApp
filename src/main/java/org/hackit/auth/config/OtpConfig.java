package org.hackit.auth.config;

import java.time.Duration;

import org.hackit.auth.service.OtpService;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import lombok.Getter;
import lombok.Setter;

@Configuration
@Setter
@Getter
@ConfigurationProperties(prefix = "otp")
public class OtpConfig {

    private OtpConfigProperties emailVerification;

    @Bean
    public OtpService emailVerificationOtpService(
            final RedisTemplate<String, String> redisTemplate,
            final PasswordEncoder passwordEncoder) {
        return new OtpService(emailVerification, redisTemplate, passwordEncoder);
    }

    public record OtpConfigProperties(String cachePrefix, Duration ttl, Integer length) {}
}
