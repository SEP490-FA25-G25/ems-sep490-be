# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Training Management System (TMS)** - A comprehensive B2B SaaS platform for multi-branch language training centers. This backend service manages the complete training lifecycle from curriculum design through graduation, supporting offline, online, and hybrid learning modalities.

**Tech Stack:**
- Spring Boot 3.5.7 with Java 21
- PostgreSQL 16 database
- JPA/Hibernate ORM
- Spring Security + JWT authentication
- Lombok for boilerplate reduction
- Maven build system

## Project Structure

```
src/main/java/org/fyp/tmssep490be/
├── entities/          # JPA entities mapped to database tables
│   ├── base/         # BaseEntity for common fields
│   └── enums/        # Enum types matching PostgreSQL enums
├── dtos/             # Data Transfer Objects
├── exceptions/       # Custom exceptions and global error handling
├── repositories/     # Spring Data JPA repositories
├── services/         # Business logic layer (service interfaces)
│   └── impl/         # Service implementations
└── controllers/      # REST API endpoints (to be created)

src/main/resources/
├── application.yml   # Main configuration file
├── schema.sql        # Complete database schema (reference only)
├── enum-init.sql     # PostgreSQL enum type definitions
└── seed-data.sql     # Initial data for testing
```

## Database Architecture

The system uses a **tiered table structure** with clear dependency hierarchy:

**TIER 1** - Foundation: `center`, `role`, `user_account`
**TIER 2** - Organization: `branch`, `subject`, `time_slot_template`, `resource`, `teacher`, `student`
**TIER 3** - Curriculum: `level`, `course`, `course_phase`, `course_session`, `plo`, `clo`, mappings
**TIER 4** - Operations: `class`, `session`, `enrollment`, `student_session`, `teaching_slot`
**TIER 5** - Assessment: `assessment`, `score`, `feedback`, `qa_report`
**TIER 6** - Requests: `student_request`, `teacher_request`

### Key Schema Patterns

1. **Dual Status Fields**: `course` and `class` entities have both:
   - `status` (lifecycle): draft → active → completed/cancelled
   - `approval_status` (workflow): pending → approved/rejected

2. **Request Confirmation Flow**: Two workflows for requests:
   - **User-initiated**: Student/Teacher creates → pending → approved
   - **Staff-initiated**: Academic Affair creates → waiting_confirm → user confirms → pending → approved

3. **Bidirectional Makeup Tracking**: `student_session` has both `makeup_session_id` and `original_session_id` for complete relationship tracking

4. **Resource Management**: Resources (`room`/`virtual`) have capacity overrides, Zoom credentials, and license tracking

5. **Enum-based Type Safety**: All status/type fields use PostgreSQL enums with matching Java enums in `entities/enums/`

## **Implementation Plan: Core Principles**

**1. Code Quality & Structure:**

- **Clean Implementation:** The implementation must be clean, avoiding unnecessary code, complexity, and "code smells." Adhere strictly to established coding standards and best practices (e.g., SOLID, DRY).
- **No Redundancy (DRY - Don't Repeat Yourself):** Actively prevent code duplication. Abstract and reuse components, functions, and logic wherever possible.
- **Logical Soundness & Correct Algorithms:** Ensure all logic is correct and the algorithms used are efficient and appropriate for the given problem.

**2. System Integrity & Performance:**

- **Prevent Race Conditions:** Proactively identify and prevent potential race conditions to ensure data integrity and system stability, especially in concurrent operations.
- **Avoid Over-engineering:** The solution must not be over-engineered. Implement what is necessary to meet the current requirements without adding speculative features or unnecessary complexity.

**3. Development Approach:**

- **Adhere to Best Practices:** Always follow the best and most current industry-standard approaches for the technologies and patterns being used.
- **Maintain a Holistic View:** Always consider the overall architecture and the impact of your changes on the entire system. Ensure new implementations integrate seamlessly.
- **Focus on the Story & Scope:** Concentrate on delivering the user story at hand. Ensure the implementation directly serves the story's requirements and stays within the defined scope for the MVP (Minimum Viable Product). The primary goal is a functional, demonstrable feature that meets the story's acceptance criteria.

**4. Final Deliverable:**

- **Solid & Maintainable Code:** The final code must be robust, reliable, well-documented, and easy for other developers to understand, modify, and maintain in the future.

## Development Commands

### Build and Run
```bash
# Build project (skip tests)
mvn clean package -DskipTests

# Build with tests
mvn clean package

# Run application
mvn spring-boot:run

# Run on specific port
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

### Testing
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=YourTestClassName

# Run tests with coverage
mvn clean verify jacoco:report
```

### Database Operations
```bash
# Access PostgreSQL container
docker exec -it some-postgres psql -U postgres -d tms

# Reset database (WARNING: drops all data)
docker exec -i some-postgres psql -U postgres -d tms < src/main/resources/schema.sql

# Initialize enums only
docker exec -i some-postgres psql -U postgres -d tms < src/main/resources/enum-init.sql

# Load seed data
docker exec -i some-postgres psql -U postgres -d tms < src/main/resources/seed-data.sql
```

### Docker Database Setup
```bash
# Start PostgreSQL container
docker run --name some-postgres -e POSTGRES_PASSWORD=979712 -p 5432:5432 -d postgres

# Create database
docker exec -it some-postgres psql -U postgres -c "CREATE DATABASE tms;"

# Stop container
docker stop some-postgres

# Start existing container
docker start some-postgres
```

## Configuration Notes

### Application Properties (application.yml)

**Database Connection:**
- URL: `jdbc:postgresql://localhost:5432/tms`
- Username: `postgres`
- Password: `979712` (change in production!)

**Hibernate DDL:**
- Current mode: `update` (recommended for development)
- Change to `validate` for production to prevent auto-schema changes
- Use `create` only for initial setup (will drop existing tables)

**SQL Initialization:**
- `enum-init.sql` runs first to create PostgreSQL enum types
- `seed-data.sql` runs after to populate initial data
- Set `spring.sql.init.mode: never` after first run to prevent rerunning

**JWT Security:**
- Access token: 15 minutes (900000 ms)
- Refresh token: 7 days (604800000 ms)
- **CRITICAL**: Change `JWT_SECRET` in production via environment variable

**Swagger UI:**
- Access at: `http://localhost:8080/swagger-ui.html`
- API docs at: `http://localhost:8080/api-docs`
- Note: Package path in config references `emssep490be` but actual package is `tmssep490be` - this needs fixing

## Architecture Guidelines

### Entity Design Principles

1. **Extend BaseEntity**: All entities should extend `entities/base/BaseEntity.java` for common fields (`id`, `createdAt`, `updatedAt`)

2. **Use PostgreSQL Array Types**: For array fields like `schedule_days`, `skill_set`, use `@Column(columnDefinition = "smallint[]")` or `@Column(columnDefinition = "skill_enum[]")`

3. **Enum Mapping**: Use `@Enumerated(EnumType.STRING)` to map Java enums to PostgreSQL enum types

4. **Composite Keys**: For junction tables, create separate `@Embeddable` ID classes

5. **Bidirectional Relationships**: Use `@OneToMany(mappedBy = "...")` and `@ManyToOne` carefully; consider fetch strategies (LAZY vs EAGER)

### Naming Conventions

- **Entities**: PascalCase (e.g., `ClassEntity` for "class" table)
- **Fields**: camelCase matching database snake_case (e.g., `startDate` → `start_date`)
- **Enums**: Match database enum names exactly (e.g., `SessionStatus.PLANNED` → `'planned'`)
- **DTOs**: Suffix with purpose (e.g., `CourseCreateRequest`, `CourseResponse`)

### Business Logic Patterns

1. **Request Management**: Implement both user-initiated and staff-initiated flows
   - Check `request_status`: `pending` vs `waiting_confirm`
   - Handle user confirmation step for staff-created requests

2. **Conflict Detection**: Before assigning resources or teachers:
   - Check `session_resource` for room/zoom double-booking
   - Check `teaching_slot` for teacher schedule conflicts
   - Check `student_session` for student schedule conflicts

3. **Session Generation**: When creating a class:
   - Auto-generate sessions from `course_session` templates
   - Calculate dates based on `start_date` + `schedule_days` + week offset
   - Skip holidays (configurable)

4. **Enrollment Auto-Generation**: When enrolling students:
   - Auto-create `student_session` records for all future sessions
   - Set `attendance_status = 'planned'`
   - For mid-course enrollment, only create sessions from `join_session_id` forward

5. **Makeup Session Logic**:
   - Find sessions with same `course_session_id` (same content)
   - Check capacity available
   - Prioritize: same branch → same modality → soonest date
   - Create bidirectional links: `makeup_session_id` ↔ `original_session_id`

### API Development Guidelines

1. **Versioning**: All endpoints should be under `/api/v1/...`

2. **Response Format**: Use `ResponseObject` DTO with standard structure:
   ```json
   {
     "success": true,
     "message": "Operation successful",
     "data": {...}
   }
   ```

3. **Error Handling**: Use `@ControllerAdvice` in `GlobalExceptionHandler`
   - Map custom exceptions to appropriate HTTP status codes
   - Return consistent error response format

4. **Pagination**: Use Spring Data `Pageable` with default page size 20
   - Support `?page=0&size=20&sort=field,asc`

5. **Filtering**: Support common filters:
   - Status filters: `?status=pending`
   - Date ranges: `?startDate=2024-01-01&endDate=2024-12-31`
   - Branch/Class filters: `?branchId=1&classId=2`

## Critical Business Rules

### Course Approval Workflow
- `course.approval_status`: pending → approved (by MANAGER)
- `course.status`: draft → active (by cronjob on `effective_date`)
- Use `hash_checksum` for optimistic locking (detect concurrent edits)

### Class Approval Workflow
- `class.approval_status`: pending → approved (by CENTER HEAD or MANAGER)
- `class.status`: draft → scheduled → ongoing → completed
- Must validate: all sessions have time_slot, resource, teacher (100% complete)

### Teacher Absence Priority
When a teacher cannot teach, follow this priority:
1. **Swap** (best): Find substitute teacher → update `teaching_slot.teacher_id`
2. **Reschedule** (ok): Create new session with `type='teacher_reschedule'`
3. **Modality change** (acceptable): Switch offline → online or vice versa
4. **Cancel** (last resort): Mark `session.status='cancelled'`

### Attendance Rules
- Can only mark attendance on `session.date = CURRENT_DATE`
- After day ends, attendance is locked (no modifications)
- Only assigned teachers can mark attendance
- Status: `planned` → `present` or `absent`
- Late/excused cases tracked via `student_session.note`

### Capacity Management
- Check `class.max_capacity` before enrollment
- Support capacity override with approval and reason
- No waitlist status - students must choose different class

## Testing Strategy

The project uses a comprehensive testing approach with JUnit 5, Mockito, Testcontainers, and AssertJ.

### Test Infrastructure

**Dependencies (in pom.xml):**
- JUnit 5 (Jupiter) - Testing framework (included with spring-boot-starter-test)
- Mockito - Mocking framework for unit tests
- AssertJ - Fluent assertions library
- Testcontainers 1.20.4 - Docker containers for integration tests
- REST Assured - API testing (optional)
- JaCoCo 0.8.12 - Code coverage reporting

**Base Test Classes:**
- `AbstractIntegrationTest` - For full integration tests with `@SpringBootTest`
- `AbstractRepositoryTest` - For repository layer tests with `@DataJpaTest`
- `PostgreSQLTestContainer` - Singleton PostgreSQL container for all tests

**Test Configuration:**
- `src/test/resources/application-test.yml` - Test-specific configuration
- PostgreSQL container auto-configured via Testcontainers
- Each test runs in transaction with automatic rollback

### Unit Tests (Service Layer)

**Pattern:** Test business logic in isolation using Mockito

```java
@ExtendWith(MockitoExtension.class)
class MyServiceTest {
    @Mock
    private MyRepository repository;

    @InjectMocks
    private MyServiceImpl service;

    @Test
    void shouldDoSomething() {
        // Arrange: Mock dependencies
        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        // Act: Call service method
        Result result = service.doSomething(1L);

        // Assert: Verify results
        assertThat(result).isNotNull();
        verify(repository).findById(1L);
    }
}
```

**Best Practices:**
- Name tests clearly: `shouldDoXWhenY()` or `givenX_whenY_thenZ()`
- Use `@DisplayName` for readable test descriptions
- Mock only external dependencies (repositories, external services)
- Focus on edge cases: null values, empty lists, exceptions
- Verify interactions: `verify(mock).method()`
- Use AssertJ fluent assertions for readability

### Integration Tests (Repository Layer)

**Pattern:** Test with real PostgreSQL database via Testcontainers

```java
@DisplayName("MyRepository Integration Tests")
class MyRepositoryTest extends AbstractRepositoryTest {
    @Autowired
    private MyRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void shouldSaveAndRetrieve() {
        // Arrange
        Entity entity = TestDataBuilder.buildEntity().build();

        // Act
        Entity saved = repository.save(entity);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(repository.findById(saved.getId())).isPresent();
    }
}
```

**Best Practices:**
- Extend `AbstractRepositoryTest` for automatic PostgreSQL setup
- Clean database in `@BeforeEach` to ensure test isolation
- Test actual database constraints (unique, foreign keys)
- Verify cascade operations work correctly
- Test complex queries and relationships
- Use `TestDataBuilder` utility for creating test entities

### Integration Tests (Full Application)

**Pattern:** Test complete workflows with `@SpringBootTest`

```java
@DisplayName("Course Approval Workflow Integration Test")
class CourseApprovalWorkflowIT extends AbstractIntegrationTest {
    @Autowired
    private CourseService courseService;

    @Test
    void shouldCompleteCourseApprovalWorkflow() {
        // Test end-to-end workflow
    }
}
```

**Use Cases:**
- Multi-service workflows (course creation → approval → class generation)
- Security/authorization scenarios
- REST API endpoints (with MockMvc or RestAssured)
- Transaction management and rollback scenarios

### Test Data Management

**TestDataBuilder Utility:**
```java
// Fluent API for creating test entities with sensible defaults
Center center = TestDataBuilder.buildCenter()
    .name("Test Center")
    .email("test@center.com")
    .build();

Course course = TestDataBuilder.buildCourse()
    .level(level)
    .status(CourseStatus.ACTIVE)
    .build();
```

**Guidelines:**
- Use `TestDataBuilder` for consistent test data
- Set only fields relevant to the test
- Create hierarchical data (Center → Subject → Level → Course) in correct order
- Clean up data after tests (handled automatically by `@Transactional`)

### Running Tests

**Maven Commands:**
```bash
# Run all tests (unit + integration)
mvn clean test

# Run only unit tests (naming: *Test.java)
mvn test

# Run only integration tests (naming: *IT.java, *IntegrationTest.java)
mvn verify

# Run tests with coverage report
mvn clean verify jacoco:report
# Report location: target/site/jacoco/index.html

# Run specific test class
mvn test -Dtest=CenterServiceImplTest

# Run specific test method
mvn test -Dtest=CenterServiceImplTest#shouldFindCenterById

# Skip tests during build
mvn clean package -DskipTests
```

**Test Execution Flow:**
1. **Surefire Plugin** runs `*Test.java` files in `mvn test` phase
2. **Failsafe Plugin** runs `*IT.java` files in `mvn verify` phase
3. **JaCoCo** generates coverage report in `mvn verify` phase

### Key Test Scenarios

Priority scenarios for comprehensive testing:

1. **Conflict Detection**
   - Resource double-booking (same room/zoom at same time)
   - Teacher schedule conflicts
   - Student schedule conflicts
   - Capacity violations

2. **Request Workflows**
   - User-initiated requests (student/teacher creates → pending → approved)
   - Staff-initiated requests (staff creates → waiting_confirm → user confirms → pending → approved)
   - Request rejection handling
   - State transition validations

3. **Session Generation**
   - Auto-generate sessions from course templates
   - Date calculation based on schedule_days and week offset
   - Holiday skipping logic
   - Session count matches course.total_sessions

4. **Enrollment Management**
   - Capacity validation before enrollment
   - Auto-generate student_session records
   - Mid-course enrollment (only future sessions)
   - Enrollment cancellation and cleanup

5. **Makeup Session Logic**
   - Find eligible makeup sessions (same course_session_id)
   - Check capacity availability
   - Priority matching (branch → modality → date)
   - Bidirectional linking (makeup_session_id ↔ original_session_id)

6. **Course/Class Approval Workflow**
   - Dual status handling (status + approval_status)
   - Optimistic locking with hash_checksum
   - Permission-based approval (MANAGER, CENTER HEAD)
   - Status transitions validation

### Test Coverage Goals

**Targets:**
- Overall: 80%+ code coverage
- Service layer: 90%+ (critical business logic)
- Repository layer: 70%+ (simpler CRUD operations)
- Entity layer: 60%+ (getters/setters excluded)

**Focus Areas:**
- All business logic methods in services
- Complex query methods in repositories
- State transition validations
- Exception handling paths
- Edge cases and error scenarios

### CI/CD Integration

Tests are designed to run in CI/CD pipelines:
- Testcontainers works with Docker in CI environments
- Tests are fast (container reuse across test classes)
- No manual database setup required
- Deterministic and repeatable results

**Example GitHub Actions workflow:**
```yaml
- name: Run tests
  run: mvn clean verify
- name: Upload coverage
  uses: codecov/codecov-action@v3
```

## Common Pitfalls to Avoid

1. **Don't confuse status fields**: Remember `course` and `class` have both `status` (lifecycle) and `approval_status` (workflow)

2. **Handle array types correctly**: PostgreSQL arrays require special column definitions and proper Java List/Array mapping

3. **Cascade carefully**: Review `@OnDelete` annotations - some relationships should `SET NULL`, others `CASCADE`

4. **Lazy loading traps**: Accessing lazy-loaded collections outside transaction causes `LazyInitializationException` - use fetch joins or DTOs

5. **Enum mismatches**: Java enum names must match PostgreSQL enum values exactly (case-sensitive)

6. **Time zones**: Use `TIMESTAMPTZ` in database and `@Column(columnDefinition = "TIMESTAMPTZ")` for proper timezone handling

7. **Package name inconsistency**: Config references `emssep490be` but actual package is `tmssep490be` - be consistent

## Documentation References

- **Product Requirements**: See [docs/prd.md](docs/prd.md) for complete feature specifications
- **Business Flows**: See [docs/business-flow-usecase.md](docs/business-flow-usecase.md) for detailed use cases
- **Database Schema**: See [src/main/resources/schema.sql](src/main/resources/schema.sql) for complete DDL
- **Spring Boot Docs**: https://docs.spring.io/spring-boot/3.5.7/reference/
- **PostgreSQL 16 Docs**: https://www.postgresql.org/docs/16/

## Next Development Steps

The project currently has:
- ✅ Complete entity layer with all JPA entities (39 entities)
- ✅ Database schema with enums and seed data
- ✅ Exception handling structure
- ✅ Basic DTO pattern
- ✅ **Repository layer** (39 Spring Data JPA interfaces)
- ✅ **Service layer** (39 service interfaces with implementations)
- ✅ **Test infrastructure** (Unit tests, Integration tests with Testcontainers)

Still needed:
- ⏳ Controller layer (REST API endpoints)
- ⏳ Security configuration (JWT authentication/authorization)
- ⏳ Expand test coverage (write tests for all services/repositories)
- ⏳ API documentation (Swagger/OpenAPI annotations)

When implementing new features, follow the pattern: Repository → Service → Controller → Tests

### Repository Layer Details

All 39 repositories are now available in `src/main/java/org/fyp/tmssep490be/repositories/`:

**Core & Organization (10 repositories):**
- CenterRepository, BranchRepository, RoleRepository
- UserAccountRepository, UserRoleRepository, UserBranchesRepository
- TeacherRepository, StudentRepository
- ResourceRepository, TimeSlotTemplateRepository

**Curriculum (12 repositories):**
- SubjectRepository, LevelRepository, CourseRepository
- CoursePhaseRepository, CourseSessionRepository, CourseMaterialRepository
- PLORepository, CLORepository
- PLOCLOMappingRepository, CourseSessionCLOMappingRepository
- CourseAssessmentRepository, CourseAssessmentCLOMappingRepository

**Operations (8 repositories):**
- ClassRepository, SessionRepository
- EnrollmentRepository, StudentSessionRepository
- TeachingSlotRepository, SessionResourceRepository
- TeacherAvailabilityRepository, TeacherSkillRepository

**Assessment & Feedback (7 repositories):**
- AssessmentRepository, ScoreRepository
- FeedbackQuestionRepository, StudentFeedbackRepository, StudentFeedbackResponseRepository
- QAReportRepository, ReplacementSkillAssessmentRepository

**Requests (2 repositories):**
- StudentRequestRepository, TeacherRequestRepository

All repositories extend `JpaRepository<Entity, Long>` and include `@Repository` annotation for Spring component scanning.

### Service Layer Details

All 39 services are now available in `src/main/java/org/fyp/tmssep490be/services/` (interfaces) and `src/main/java/org/fyp/tmssep490be/services/impl/` (implementations):

**Core & Organization (10 services):**
- CenterService, BranchService, RoleService
- UserAccountService, UserRoleService, UserBranchesService
- TeacherService, StudentService
- ResourceService, TimeSlotTemplateService

**Curriculum (12 services):**
- SubjectService, LevelService, CourseService
- CoursePhaseService, CourseSessionService, CourseMaterialService
- PLOService, CLOService
- PLOCLOMappingService, CourseSessionCLOMappingService
- CourseAssessmentService, CourseAssessmentCLOMappingService

**Operations (8 services):**
- ClassService, SessionService
- EnrollmentService, StudentSessionService
- TeachingSlotService, SessionResourceService
- TeacherAvailabilityService, TeacherSkillService

**Assessment & Feedback (7 services):**
- AssessmentService, ScoreService
- FeedbackQuestionService, StudentFeedbackService, StudentFeedbackResponseService
- QAReportService, ReplacementSkillAssessmentService

**Requests (2 services):**
- StudentRequestService, TeacherRequestService

All service implementations use `@Service` annotation, `@RequiredArgsConstructor` for dependency injection, and inject their corresponding repository. The services are currently templates ready for business logic implementation.

### Test Infrastructure Details

Complete test setup is now available in `src/test/java/org/fyp/tmssep490be/`:

**Test Configuration:**
- `src/test/resources/application-test.yml` - Test-specific Spring configuration
- `config/PostgreSQLTestContainer.java` - Singleton PostgreSQL container
- `config/AbstractIntegrationTest.java` - Base class for full integration tests
- `config/AbstractRepositoryTest.java` - Base class for repository tests

**Test Utilities:**
- `utils/TestDataBuilder.java` - Fluent API for creating test entities with sensible defaults

**Example Tests:**
- `services/impl/CenterServiceImplTest.java` - Unit test example with Mockito
- `repositories/CenterRepositoryTest.java` - Simple repository integration test
- `repositories/CourseRepositoryIntegrationTest.java` - Complex integration test with relationships

**Test Dependencies (pom.xml):**
- JUnit 5 + Mockito + AssertJ (via spring-boot-starter-test)
- Testcontainers 1.20.4 (PostgreSQL module)
- REST Assured (for API testing)
- JaCoCo 0.8.12 (code coverage)
- Maven Surefire Plugin (unit tests)
- Maven Failsafe Plugin (integration tests)

**Running Tests:**
```bash
# Unit tests only
mvn test

# All tests (unit + integration)
mvn verify

# With coverage report
mvn clean verify jacoco:report
```

**Test Naming Conventions:**
- Unit tests: `*Test.java` (e.g., `CenterServiceImplTest.java`)
- Integration tests: `*IT.java` or `*IntegrationTest.java` (e.g., `CenterRepositoryIT.java`)

**Key Features:**
- Real PostgreSQL database via Testcontainers (no H2 incompatibility issues)
- Container reuse across test classes for performance
- Automatic transaction rollback after each test
- Fluent test data builders for easy setup
- CI/CD ready (works with GitHub Actions, GitLab CI, etc.)

## Lessons Learned from Authentication Implementation

### Critical Issues Encountered and Solutions

#### 1. **Hibernate Enum Column Definition Conflict**

**Problem:** Using `@Enumerated(EnumType.STRING)` together with `columnDefinition = "enum_type"` causes Hibernate to generate invalid DDL with CHECK constraints using Java enum names (UPPERCASE) instead of PostgreSQL enum values (lowercase).

```java
// ❌ WRONG - Causes: check (gender in ('MALE','FEMALE','OTHER'))
@Enumerated(EnumType.STRING)
@Column(columnDefinition = "gender_enum", nullable = false)
private Gender gender;
```

**Solution:** Remove `columnDefinition` for single enum fields. Hibernate will map correctly when enum names match PostgreSQL values:

```java
// ✅ CORRECT
@Enumerated(EnumType.STRING)
@Column(nullable = false)
private Gender gender;
```

**Note:** Keep `columnDefinition` for **enum arrays** as Hibernate doesn't auto-map array types:
```java
// ✅ CORRECT for arrays
@Column(columnDefinition = "skill_enum[]")
@Enumerated(EnumType.STRING)
private List<Skill> skillSet;
```

**File:** [UserAccount.java:40,52](src/main/java/org/fyp/tmssep490be/entities/UserAccount.java)

---

#### 2. **PostgreSQL Enum Values Missing**

**Problem:** Java enum `UserStatus` has `ACTIVE, INACTIVE, SUSPENDED` but PostgreSQL enum only had `('active', 'inactive')`, causing save failures.

**Root Cause:** `enum-init.sql` was incomplete.

**Solution:**
1. Update enum definition in [enum-init.sql:80](src/main/resources/enum-init.sql#L80)
2. Alter existing database: `ALTER TYPE user_status_enum ADD VALUE 'suspended';`

**Lesson:** Always ensure Java enums and PostgreSQL enum types are **in sync**. When adding new enum values:
- Update `enum-init.sql`
- For existing databases, use `ALTER TYPE` to add values
- Cannot remove enum values in PostgreSQL without recreating the type

---

#### 3. **Role Mapping Using Wrong Field**

**Problem:** Authentication returned wrong role names because `UserPrincipal` used `role.getName()` ("Administrator") instead of `role.getCode()` ("ADMIN").

```java
// ❌ WRONG
.map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))

// ✅ CORRECT
.map(role -> new SimpleGrantedAuthority("ROLE_" + role.getCode()))
```

**Lesson:** Use `role.getCode()` for authorities and permissions. Use `role.getName()` only for display purposes.

**File:** [UserPrincipal.java:36](src/main/java/org/fyp/tmssep490be/security/UserPrincipal.java#L36)

---

#### 4. **Lazy Loading Not Fetching Related Entities**

**Problem:** `UserAccount.userRoles` collection was empty during authentication because of lazy loading.

**Solution:** Add `@EntityGraph` to eagerly fetch required associations:

```java
@EntityGraph(attributePaths = {"userRoles", "userRoles.role"})
Optional<UserAccount> findByEmail(String email);

@EntityGraph(attributePaths = {"userRoles", "userRoles.role"})
Optional<UserAccount> findById(Long id);
```

**Lesson:**
- Use `@EntityGraph` for specific queries needing eager loading
- Avoid global `EAGER` fetch which loads unnecessarily in all scenarios
- Specify nested paths when fetching associations of associations

**File:** [UserAccountRepository.java:17,29](src/main/java/org/fyp/tmssep490be/repositories/UserAccountRepository.java)

---

#### 5. **Hibernate Session Duplicate Instance Error**

**Problem:** Test failed with "A different object with the same identifier value was already associated with the session" when adding entity to collection after save.

**Root Cause:** Adding `userRole` to `testUser.getUserRoles()` after `userRoleRepository.save(userRole)` creates duplicate instances in Hibernate session due to cascade operations.

**Wrong Approach:**
```java
// ❌ Creates duplicate in session
userRoleRepository.save(userRole);
testUser.getUserRoles().add(userRole);  // Conflict!
entityManager.flush();  // Error here
```

**Solution:** Don't manually add to collection. Let Hibernate fetch fresh data:

```java
// ✅ CORRECT
userRoleRepository.save(userRole);
entityManager.flush();   // Persist to DB
entityManager.clear();   // Detach all entities

// Later, when user is loaded, @EntityGraph fetches fresh data
```

**Lesson:**
- Don't mix manual collection management with Hibernate-managed entities
- Use `flush()` + `clear()` to ensure fresh data in tests
- Trust `@EntityGraph` to load relationships correctly

**File:** [AuthControllerIntegrationTest.java:102,305](src/test/java/org/fyp/tmssep490be/controllers/AuthControllerIntegrationTest.java)

---

### Best Practices Summary

1. **Enum Handling:**
   - Keep Java enum names matching PostgreSQL enum values (case-sensitive)
   - Don't use `columnDefinition` for single enums with `@Enumerated(EnumType.STRING)`
   - Use `columnDefinition` ONLY for enum arrays
   - Keep `enum-init.sql` and Java enums synchronized

2. **Entity Relationships:**
   - Use `@EntityGraph` for controlled eager loading
   - Use `role.getCode()` for authorities, not `role.getName()`
   - Don't manually manage bidirectional relationships in Hibernate session

3. **Testing with JPA:**
   - Use `entityManager.flush()` and `clear()` to ensure fresh data
   - Let `@EntityGraph` handle relationship loading
   - Avoid mixing manual collection updates with Hibernate-managed entities

4. **Security:**
   - Always use environment variables for secrets in production
   - Keep token expiration times reasonable (15min access, 7 days refresh)
   - Log authentication events for audit trails

---

### Authentication Implementation Checklist

When implementing similar features, ensure:

- [ ] Java enums match PostgreSQL enum values exactly
- [ ] No `columnDefinition` on single enum fields with `@Enumerated`
- [ ] Use `role.getCode()` for authorities
- [ ] Add `@EntityGraph` for required eager loading
- [ ] Test with `entityManager.flush()/clear()` for fresh data
- [ ] Comprehensive test coverage (happy path + edge cases + errors)
- [ ] Proper exception handling with meaningful messages
- [ ] Security configuration validates token types
- [ ] Logging for security audit trail
