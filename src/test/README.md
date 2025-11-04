# Test Documentation

## ⚠️ CRITICAL: Modern Testing Standards (Spring Boot 3.4+ / Spring Framework 6.2+)

### @MockitoBean vs @MockBean - Use the Correct One!

**Spring Boot 3.4.0+ (November 2024) introduced breaking changes:**

```java
// ✅ CORRECT (Spring Boot 3.4+)
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class ServiceTest {
    @Autowired private YourService service;
    @MockitoBean private YourRepository repository;  // ✅ Use @MockitoBean
}
```

```java
// ❌ DEPRECATED (Spring Boot < 3.4)
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class ServiceTest {
    @Autowired private YourService service;
    @MockBean private YourRepository repository;  // ❌ Deprecated, will be removed in Spring Boot 4.0
}
```

**Why the change?**
- New Bean Override mechanism in Spring Framework 6.2
- Better integration with Spring Test Context Framework
- Part of framework core (not Boot-specific)
- Consistent with `@TestBean` and other override annotations

### ❌ NEVER Use These Patterns for Spring Boot Tests:

```java
// ❌ WRONG - Bypasses Spring Context
@ExtendWith(MockitoExtension.class)
class ServiceTest {
    @Mock private YourRepository repository;
    @InjectMocks private YourService service;
}
```

**Why wrong?**
- No Spring context → no dependency injection
- No transaction management
- No Spring AOP (if used)
- Not testing real Spring bean wiring

## Overview

This directory contains the comprehensive test suite for the TMS SEP490 BE Spring Boot application. The test setup follows modern Spring Boot 3.5.7 and Java 21 best practices with a three-tier testing strategy.

## Test Architecture

### Three-Tier Testing Strategy

1. **Unit Tests** (`*Test.java` - Service Layer)
   - Test business logic in isolation
   - Use `@SpringBootTest` with `@MockitoBean` for dependencies (Spring Boot 3.4+)
   - Focus on service methods, business rules, and error handling
   - Coverage Goal: 90%+

2. **Integration Tests** (`*RepositoryTest.java` - Data Layer)
   - Test database operations with real PostgreSQL via Testcontainers
   - Use `@DataJpaTest` with singleton container pattern
   - Validate entity relationships, queries, and constraints
   - Coverage Goal: 70%+

3. **End-to-End Tests** (`*IT.java`, `*IntegrationTest.java` - API Layer)
   - Test complete API workflows with MockMvc
   - Use `@SpringBootTest` with `@AutoConfigureMockMvc`
   - Validate request/response cycles, security, and integration
   - Coverage Goal: 80%+

## Test Configuration

### Testcontainers Setup
- **Database**: PostgreSQL 16 with singleton pattern
- **Container**: Reusable across test classes for performance
- **Schema**: Fresh creation for each test run (`ddl-auto: create-drop`)
- **Configuration**: `PostgreSQLTestContainer` and `AbstractRepositoryTest`

### Base Classes

#### `AbstractRepositoryTest`
```java
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = NONE)
@Testcontainers
@Import(JpaAuditingConfiguration.class)
```
- Base class for all repository tests
- Provides Testcontainers PostgreSQL instance
- Includes JPA auditing support

#### TestDataBuilder Utility
- Fluent API for creating test entities
- Pre-built builders for all entities (`Center`, `Branch`, `ClassEntity`, etc.)
- Sensible defaults with customizable fields
- Supports entity relationships and constraints

### Test Profiles

#### `application-test.yml`
- Dedicated test configuration
- Testcontainers database connection
- JWT test configuration
- Debug logging for test troubleshooting
- Security context enabled for authentication tests

## Running Tests

### Command Reference

```bash
# Run all tests with coverage report
mvn clean verify jacoco:report

# Run unit tests only
mvn test

# Run integration tests only
mvn verify -DskipUnitTests

# Run tests in parallel (Java 21 virtual threads)
mvn -T 1C clean verify

# Run specific test class
mvn test -Dtest=CenterServiceImplTest

# Run specific test method
mvn test -Dtest=CenterServiceImplTest#shouldCreateCenterSuccessfully

# Run tests with coverage
mvn clean verify jacoco:report
# View coverage: target/site/jacoco/index.html
```

### Maven Configuration

#### Surefire Plugin (Unit Tests)
- Pattern: `**/*Test.java`, `**/*Tests.java`
- Includes JaCoCo agent for coverage
- Java 21 dynamic agent loading enabled

#### Failsafe Plugin (Integration Tests)
- Pattern: `**/*IT.java`, `**/*IntegrationTest.java`
- Runs after unit tests in verify phase
- Same coverage integration as unit tests

## Testing Best Practices

### Service Layer Tests

```java
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(OrderAnnotation.class)
class ServiceImplTest {

    @Autowired
    private Service service;

    @MockitoBean  // Use @MockitoBean (Spring Boot 3.4+)
    private Repository repository;

    @Test
    @DisplayName("Should handle business logic correctly")
    @Order(1)
    void shouldHandleBusinessLogic() {
        // Given
        when(repository.findById(anyLong())).thenReturn(Optional.of(entity));

        // When
        Result result = service.performOperation(1L);

        // Then
        assertThat(result).isNotNull();
        verify(repository).findById(1L);
    }
}
```

### Repository Layer Tests

```java
@DataJpaTest
@Testcontainers
class RepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private Repository repository;

    @Test
    @DisplayName("Should persist and retrieve entity")
    void shouldPersistAndRetrieveEntity() {
        // Given
        Entity entity = TestDataBuilder.buildEntity()
            .name("Test")
            .build();

        // When
        Entity saved = repository.save(entity);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Test");
    }
}
```

### Integration Tests (API)

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = "admin", roles = "ADMIN")
class ControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Should handle API request")
    void shouldHandleApiRequest() throws Exception {
        mockMvc.perform(post("/api/v1/resource")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
```

## Test Data Management

### TestDataBuilder Patterns

#### Center Entity
```java
Center center = TestDataBuilder.buildCenter()
    .code("TC001")
    .name("Test Center")
    .email("test@center.com")
    .build();
```

#### Class Entity with Relationships
```java
ClassEntity classEntity = TestDataBuilder.buildClassEntity()
    .branch(branch)
    .course(course)
    .code("CLASS001")
    .modality(Modality.ONLINE)
    .build();
```

#### Student with User Account
```java
Student student = TestDataBuilder.buildStudent()
    .userAccount(userAccount)
    .studentCode("ST001")
    .build();
```

### Entity Relationships
- Centers have many Branches
- Branches belong to Centers and have Classes
- Classes belong to Courses and Branches
- Students belong to User Accounts and have Enrollments
- Courses belong to Subjects and Levels

## Security Testing

### Authentication Patterns
```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthTests {

    @Test
    @WithMockUser(username = "academic.affairs@example.com", roles = {"ACADEMIC_AFFAIRS"})
    void shouldAllowAcademicAffairsAccess() throws Exception {
        mockMvc.perform(get("/api/v1/classes"))
                .andExpect(status().isOk());
    }
}
```

### JWT Token Testing
- Test tokens configured in `application-test.yml`
- Use `@WithMockUser` for role-based testing
- Custom security contexts for complex scenarios

## Coverage Goals

- **Overall Coverage**: 80% minimum
- **Service Layer**: 90%+ (business logic)
- **Repository Layer**: 70%+ (data access)
- **Entity Layer**: 60%+ (domain models)
- **Controller Layer**: 80%+ (API endpoints)

## Test Organization

```
src/test/java/org/fyp/tmssep490be/
├── config/                    # Test configuration classes
│   ├── AbstractRepositoryTest.java
│   └── PostgreSQLTestContainer.java
├── utils/                     # Test utilities
│   └── TestDataBuilder.java
├── repositories/              # Repository integration tests
│   ├── CenterRepositoryTest.java
│   ├── ClassRepositoryTest.java
│   └── EnrollmentRepositoryTest.java
├── services/impl/            # Service unit tests
│   ├── CenterServiceImplTest.java
│   ├── AuthServiceImplTest.java
│   └── ClassServiceImplTest.java
└── controllers/              # Controller integration tests
    ├── CenterControllerIT.java
    └── ClassControllerIT.java
```

## Troubleshooting

### Common Issues

1. **Testcontainers Connection Issues**
   - Ensure Docker is running
   - Check port 5432 availability
   - Verify container reuse configuration

2. **Spring Context Load Failures**
   - Check profile configuration (`@ActiveProfiles("test")`)
   - Verify bean definitions and dependencies
   - Review component scanning

3. **Test Data Persistence Issues**
   - Use `@Transactional` for automatic rollback
   - Verify `ddl-auto: create-drop` in test config
   - Check entity relationships and constraints

### Debugging

Enable debug logging in `application-test.yml`:
```yaml
logging:
  level:
    org.fyp.tmssep490be: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
    org.testcontainers: INFO
```

## Continuous Integration

The test suite is designed for CI/CD pipelines:
- Fast execution with parallel test runs
- Comprehensive coverage reporting
- Isolated test environments
- Consistent test data management

For local development, use `mvn clean verify` for full test execution with coverage reporting.