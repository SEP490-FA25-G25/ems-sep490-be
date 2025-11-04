package org.fyp.tmssep490be.config;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for repository layer tests with Testcontainers.
 * Uses @DataJpaTest for faster test execution (only JPA components are loaded).
 * Implements modern Spring Boot 3.5.7 testing patterns.
 *
 * Features:
 * - PostgreSQL Testcontainers with singleton pattern
 * - @DynamicPropertySource for proper Spring Boot integration
 * - Transaction rollback for test isolation
 * - Optimized for Java 21 performance
 *
 * Usage:
 * <pre>
 * @DataJpaTest
 * class CenterRepositoryTest extends AbstractRepositoryTest {
 *     @Autowired
 *     private CenterRepository centerRepository;
 *
 *     @Test
 *     void shouldSaveAndRetrieveCenter() {
 *         // Your test code here
 *     }
 * }
 * </pre>
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import({JpaAuditingConfiguration.class})
@Transactional  // Rollback changes after each test
public abstract class AbstractRepositoryTest {

    @Container
    protected static final PostgreSQLTestContainer postgresContainer =
            PostgreSQLTestContainer.getContainer();

    @BeforeAll
    static void beforeAll() {
        // Ensure container is started before running tests
        postgresContainer.start();
    }

    @DynamicPropertySource
    static void setDatasourceProperties(DynamicPropertyRegistry registry) {
        // Modern Spring Boot 3.5.7 approach for Testcontainers integration
        registry.add("spring.datasource.url", postgresContainer::getDatabaseUrl);
        registry.add("spring.datasource.username", postgresContainer::getDatabaseUsername);
        registry.add("spring.datasource.password", postgresContainer::getDatabasePassword);

        // Optimize JPA for test performance
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> "false");  // Enable for debugging
        registry.add("spring.jpa.properties.hibernate.format_sql", () -> "true");
        registry.add("spring.jpa.properties.hibernate.jdbc.batch_size", () -> "20");
        registry.add("spring.jpa.properties.hibernate.order_inserts", () -> "true");
        registry.add("spring.jpa.properties.hibernate.order_updates", () -> "true");
    }
}
