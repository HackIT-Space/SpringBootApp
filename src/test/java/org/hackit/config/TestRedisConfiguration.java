package org.hackit.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.test.context.ActiveProfiles;

@TestConfiguration
@ActiveProfiles("test")
public class TestRedisConfiguration {

  @Bean
  @Primary
  public RedisConnectionFactory redisConnectionFactory() {
    // Создаем тестовую конфигурацию Redis для использования в тестах
    // В тестовом профиле Redis отключен, но мы все равно предоставляем bean
    // для избежания ошибок внедрения зависимостей
    return new LettuceConnectionFactory(new RedisStandaloneConfiguration("localhost", 6379));
  }
} 