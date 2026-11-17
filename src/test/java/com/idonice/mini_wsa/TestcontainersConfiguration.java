package com.idonice.mini_wsa;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

// A throwaway PostgreSQL (same version as compose.yaml) for tests.
// @ServiceConnection points the app's datasource at it instead of localhost:5433.
// Tests that need a database add: @Import(TestcontainersConfiguration.class)
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer("postgres:17");
    }
}
