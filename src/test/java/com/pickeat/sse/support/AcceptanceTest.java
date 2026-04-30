package com.pickeat.sse.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pickeat.sse.support.utility.PickeatSubscribeManagerCleaner;
import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AcceptanceTest {

    private static final GenericContainer<?> REDIS_CONTAINER;

    @LocalServerPort
    int port;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected StringRedisTemplate redisTemplate;

    @Autowired
    protected PickeatSubscribeManagerCleaner subscribeManagerCleaner;

    @BeforeEach
    void setup() {
        RestAssured.port = port;
    }

    @AfterEach
    void afterEach() {
        subscribeManagerCleaner.clean();
    }

    static {
        REDIS_CONTAINER = new GenericContainer<>(DockerImageName.parse("redis:latest"))
                .withExposedPorts(6379)
                .withCommand("redis-server --requirepass pickeat123");
        REDIS_CONTAINER.start();
    }

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
        registry.add("spring.data.redis.port", () -> REDIS_CONTAINER.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> "pickeat123");
    }

    public int getPort() {
        return port;
    }
}
