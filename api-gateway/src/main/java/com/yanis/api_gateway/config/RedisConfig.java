package com.yanis.api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis configuration for API Gateway.
 *
 * <p>
 * Configures Redis for:
 * <ul>
 * <li>Rate limiting (sliding window counters)</li>
 * <li>Token blacklist (revoked JWT tokens)</li>
 * <li>Session management (if needed)</li>
 * </ul>
 */
@Configuration
public class RedisConfig {

    /**
     * Configures RedisTemplate for object storage.
     *
     * <p>
     * Uses String serializer for keys and JSON serializer for values.
     * </p>
     *
     * @param connectionFactory Redis connection factory.
     * @return Configured RedisTemplate.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use String serializer for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Use JSON serializer for values
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        template.afterPropertiesSet();
        return template;
    }
}
