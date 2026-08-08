package com.homedashboard;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

public interface TestContainers {

    @Container
    public static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15")
            .withDatabaseName("foo")
            .withUsername("foo")
            .withPassword("secret");

}
