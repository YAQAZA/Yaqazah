package com.yaqazah;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.azure.storage.blob.BlobServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class YaqazahApplicationTests {

    // 1. Tell Docker to pull and run a PostgreSQL image
    @Container
    // 2. Magically wire the URL, Username, and Password into Spring Boot
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    // This tells Spring Boot: "Fake the Azure connection during tests so it doesn't crash"
    @MockitoBean
    private BlobServiceClient blobServiceClient;

    @Test
    void contextLoads() {
        // If this passes, your app successfully connected to the Docker database!
    }
}