package com.yanis.api_gateway;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Disabled("Integration tests require Docker/Infrastructure which is currently unavailable. Unit tests cover filter logic.")
@SpringBootTest(properties = {
		"spring.cloud.config.enabled=false",
		"spring.cloud.discovery.enabled=false",
		"spring.data.redis.host=localhost",
		"spring.data.redis.port=6379",
		"spring.profiles.active=test"
})
class ApiGatewayApplicationTests {

	@MockitoBean
	private ReactiveRedisConnectionFactory reactiveRedisConnectionFactory;

	@MockitoBean
	private RedisTemplate<?, ?> redisTemplate;

	@MockitoBean
	private ReactiveRedisTemplate<?, ?> reactiveRedisTemplate;

	@Test
	void contextLoads() {
	}
}
