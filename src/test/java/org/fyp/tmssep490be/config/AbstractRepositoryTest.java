package org.fyp.tmssep490be.config;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for repository layer tests with Testcontainers.
 * Uses @DataJpaTest for faster test execution (only JPA components are loaded).
 *
 * Usage:
 * <pre>
 * public class CenterRepositoryTest extends AbstractRepositoryTest {
 *     @Autowired
 *     private CenterRepository centerRepository;
 *     // Your test code here
 * }
 * </pre>
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(JpaAuditingConfiguration.class)
public abstract class AbstractRepositoryTest {

    @Container
    protected static final PostgreSQLContainer<?> postgresContainer =
            PostgreSQLTestContainer.getInstance();

    @BeforeAll
    static void beforeAll() {
        postgresContainer.start();
    }

    @DynamicPropertySource
    static void setDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
    }
}
