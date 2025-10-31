package org.fyp.tmssep490be.config;

import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Singleton PostgreSQL container for integration tests.
 * Reuses the same container across all tests for better performance.
 */
public class PostgreSQLTestContainer extends PostgreSQLContainer<PostgreSQLTestContainer> {

    private static final String IMAGE_VERSION = "postgres:16";
    private static PostgreSQLTestContainer container;

    private PostgreSQLTestContainer() {
        super(IMAGE_VERSION);
    }

    public static PostgreSQLTestContainer getInstance() {
        if (container == null) {
            container = new PostgreSQLTestContainer()
                    .withDatabaseName("tms_test")
                    .withUsername("test")
                    .withPassword("test")
                    .withReuse(true);
        }
        return container;
    }

    @Override
    public void start() {
        super.start();
        System.setProperty("spring.datasource.url", container.getJdbcUrl());
        System.setProperty("spring.datasource.username", container.getUsername());
        System.setProperty("spring.datasource.password", container.getPassword());
    }

    @Override
    public void stop() {
        // Do nothing, JVM handles shut down
        // This allows container reuse across test classes
    }
}
