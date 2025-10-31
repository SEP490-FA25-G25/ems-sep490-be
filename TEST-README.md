# Testing Guide for TMS Project

This document provides a quick reference for testing in the TMS project.

## Quick Start

### Prerequisites
- Docker Desktop installed and running (for Testcontainers)
- Maven 3.8+
- Java 21

### Run All Tests
```bash
mvn clean verify
```

## Test Types

### 1. Unit Tests (Service Layer)
**Location:** `src/test/java/**/services/impl/*Test.java`

**Purpose:** Test business logic in isolation with mocked dependencies

**Example:**
```java
@ExtendWith(MockitoExtension.class)
class CenterServiceImplTest {
    @Mock
    private CenterRepository centerRepository;

    @InjectMocks
    private CenterServiceImpl centerService;

    @Test
    void shouldFindCenterById() {
        // Given
        when(centerRepository.findById(1L))
            .thenReturn(Optional.of(testCenter));

        // When
        Optional<Center> result = centerRepository.findById(1L);

        // Then
        assertThat(result).isPresent();
        verify(centerRepository).findById(1L);
    }
}
```

**Run Unit Tests Only:**
```bash
mvn test
```

### 2. Integration Tests (Repository Layer)
**Location:** `src/test/java/**/repositories/*Test.java`

**Purpose:** Test database operations with real PostgreSQL (via Testcontainers)

**Example:**
```java
@DisplayName("CenterRepository Integration Tests")
class CenterRepositoryTest extends AbstractRepositoryTest {
    @Autowired
    private CenterRepository centerRepository;

    @Test
    void shouldSaveAndRetrieve() {
        // Given
        Center center = TestDataBuilder.buildCenter().build();

        // When
        Center saved = centerRepository.save(center);

        // Then
        assertThat(saved.getId()).isNotNull();
    }
}
```

**Run Integration Tests Only:**
```bash
mvn verify -DskipUnitTests
```

### 3. Full Integration Tests (End-to-End)
**Location:** `src/test/java/**/*IT.java` or `**/*IntegrationTest.java`

**Purpose:** Test complete workflows across multiple services

**Example:**
```java
class CourseApprovalWorkflowIT extends AbstractIntegrationTest {
    @Autowired
    private CourseService courseService;

    @Test
    void shouldCompleteCourseApprovalWorkflow() {
        // Test full workflow from creation to approval
    }
}
```

## Base Test Classes

### AbstractRepositoryTest
Use for repository layer tests with `@DataJpaTest`
```java
class MyRepositoryTest extends AbstractRepositoryTest {
    @Autowired
    private MyRepository repository;
}
```

### AbstractIntegrationTest
Use for full integration tests with `@SpringBootTest`
```java
class MyIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private MyService service;
}
```

## Test Data Builders

Use `TestDataBuilder` to create test entities with sensible defaults:

```java
// Simple entity
Center center = TestDataBuilder.buildCenter()
    .name("Test Center")
    .email("test@center.com")
    .build();

// Entity with relationships
Course course = TestDataBuilder.buildCourse()
    .level(level)
    .status(CourseStatus.ACTIVE)
    .approvalStatus(ApprovalStatus.APPROVED)
    .build();
```

## Common Commands

```bash
# Run all tests
mvn clean verify

# Run only unit tests
mvn test

# Run specific test class
mvn test -Dtest=CenterServiceImplTest

# Run specific test method
mvn test -Dtest=CenterServiceImplTest#shouldFindCenterById

# Run tests with coverage report
mvn clean verify jacoco:report
# Report: target/site/jacoco/index.html

# Skip tests during build
mvn clean package -DskipTests

# Run tests in parallel (faster)
mvn -T 1C clean verify
```

## Debugging Tests

### View Test Logs
```bash
mvn test -X  # Debug mode with verbose output
```

### Keep Testcontainers Running
Uncomment in test class:
```java
@Container
static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
    .withReuse(true);  // Keep container between runs
```

### Connect to Test Database
When tests are running, find container port:
```bash
docker ps
# Connect using shown port
psql -h localhost -p <PORT> -U test -d tms_test
```

## Best Practices

### Test Naming
- ✅ `shouldSaveCenter()` - Clear, describes behavior
- ✅ `shouldThrowExceptionWhenCenterNotFound()` - Explicit about expectations
- ❌ `test1()` - Not descriptive
- ❌ `testSave()` - Too vague

### Test Structure (AAA Pattern)
```java
@Test
void shouldDoSomething() {
    // Arrange (Given) - Set up test data
    Center center = TestDataBuilder.buildCenter().build();

    // Act (When) - Execute the code under test
    Center saved = centerRepository.save(center);

    // Assert (Then) - Verify the results
    assertThat(saved.getId()).isNotNull();
}
```

### AssertJ Fluent Assertions
```java
// ✅ Good - Fluent and readable
assertThat(result)
    .isPresent()
    .hasValueSatisfying(center -> {
        assertThat(center.getName()).isEqualTo("Test Center");
        assertThat(center.getEmail()).isEqualTo("test@center.com");
    });

// ❌ Avoid - Less readable
assertTrue(result.isPresent());
assertEquals("Test Center", result.get().getName());
```

### Mock Verification
```java
// Verify method was called
verify(repository).findById(1L);

// Verify method was called N times
verify(repository, times(2)).save(any());

// Verify method was never called
verify(repository, never()).delete(any());

// Verify no more interactions
verifyNoMoreInteractions(repository);
```

## Troubleshooting

### Testcontainers Not Starting
1. Ensure Docker Desktop is running
2. Check Docker has enough resources (4GB+ RAM recommended)
3. Enable container reuse: `testcontainers.reuse.enable=true` in `testcontainers.properties`

### Tests Pass Individually But Fail Together
- Likely issue: Test data pollution
- Solution: Ensure `@BeforeEach` cleans up properly
```java
@BeforeEach
void setUp() {
    repository.deleteAll();  // Clean slate for each test
}
```

### Slow Tests
1. Enable container reuse (see `testcontainers.properties`)
2. Use `@DataJpaTest` instead of `@SpringBootTest` when possible
3. Run tests in parallel: `mvn -T 1C verify`

### Coverage Not Generating
```bash
# Ensure you run verify (not just test)
mvn clean verify

# Check report location
ls target/site/jacoco/index.html
```

## CI/CD Integration

### GitHub Actions Example
```yaml
name: Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Run tests
        run: mvn clean verify

      - name: Upload coverage
        uses: codecov/codecov-action@v3
        with:
          files: ./target/site/jacoco/jacoco.xml
```

## Coverage Goals

- **Overall:** 80%+
- **Service Layer:** 90%+ (critical business logic)
- **Repository Layer:** 70%+ (simpler CRUD)
- **Entity Layer:** 60%+ (getters/setters often excluded)

## Resources

- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [AssertJ Documentation](https://assertj.github.io/doc/)
- [Testcontainers Documentation](https://www.testcontainers.org/)
- [Spring Boot Testing](https://docs.spring.io/spring-boot/reference/testing/index.html)
