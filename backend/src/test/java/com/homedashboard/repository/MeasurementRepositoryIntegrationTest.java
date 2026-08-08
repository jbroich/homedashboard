package com.homedashboard.repository;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;

import com.homedashboard.TestContainers;

@SpringBootTest
@ImportTestcontainers(TestContainers.class)
public class MeasurementRepositoryIntegrationTest {

    @Test
    public void ensureStart() {
        assertTrue(TestContainers.postgres.isRunning());
    }

}