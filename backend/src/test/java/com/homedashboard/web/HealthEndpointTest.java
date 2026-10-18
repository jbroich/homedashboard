package com.homedashboard.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;

import java.sql.SQLException;

import com.fasterxml.jackson.databind.JsonNode;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HealthEndpointTest {

    @Autowired
    private TestRestTemplate rest;

    @MockitoSpyBean
    private DataSource dataSource;

    @Test
    void reportsHealthyDatabaseWithoutExposingConnectionDetails() {
        var response = rest.getForEntity("/actuator/health", JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("status").asText()).isEqualTo("UP");
        assertThat(response.getBody().size()).isEqualTo(1);
    }

    @Test
    void rejectsDeploymentWhenDatabaseCannotBeReached() throws SQLException {
        doThrow(new SQLException("Database unavailable during deployment"))
                .when(dataSource).getConnection();

        var response = rest.getForEntity("/actuator/health", JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().get("status").asText()).isEqualTo("DOWN");
        assertThat(response.getBody().size()).isEqualTo(1);
    }
}
