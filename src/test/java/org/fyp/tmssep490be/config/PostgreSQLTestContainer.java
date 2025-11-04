package org.fyp.tmssep490be.config;

import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Singleton PostgreSQL container for integration tests.
 * Reuses the same container across all tests for better performance.
 * Uses modern Spring Boot 3.5.7 @DynamicPropertySource pattern.
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
                    .withReuse(true)
                    .withLabel("reuse", "true");  // Testcontainers reuse optimization
        }
        return container;
    }

    /**
     * Get container without System.setProperty (deprecated approach)
     * Use @DynamicPropertySource in test classes instead
     */
    public static PostgreSQLTestContainer getContainer() {
        return getInstance();
    }

    @Override
    public void start() {
        super.start();
        // Note: System.setProperty approach is deprecated
        // Use @DynamicPropertySource in test classes for proper Spring Boot integration
    }

    @Override
    public void stop() {
        // Do nothing, JVM handles shut down
        // This allows container reuse across test classes
    }

    /**
     * Get database URL for @DynamicPropertySource
     */
    public String getDatabaseUrl() {
        return super.getJdbcUrl();
    }

    /**
     * Get database username for @DynamicPropertySource
     */
    public String getDatabaseUsername() {
        return super.getUsername();
    }

    /**
     * Get database password for @DynamicPropertySource
     */
    public String getDatabasePassword() {
        return super.getPassword();
    }
}
