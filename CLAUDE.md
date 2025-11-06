# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a **Java Spring Boot 3.5.7** application using **Java 21** for a Tuition Management System (TMS). The application follows a clean layered architecture with JWT-based authentication and PostgreSQL database.

**Key Technologies:**
- Spring Boot 3.5.7 with Java 21
- PostgreSQL with Hibernate JPA
- JWT authentication with role-based authorization
- SpringDoc OpenAPI for API documentation
- Testcontainers for integration testing
- Maven for build management


## Implementation Plan: Core Principles

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

## Acknowledging Correct Feedback

When feedback IS correct:

✅ "Fixed. [Brief description of what changed]"
✅ "Good catch – [specific issue]. Fixed in [location]."
✅ [Just fix it and show in the code]

❌ "You're absolutely right!"
❌ "Great point!"
❌ "Thanks for catching that!"
❌ "Thanks for [anything]!"
❌ ANY gratitude expression

**Why no thanks:** Actions speak. Just fix it. The code itself shows you heard the feedback.

**If you catch yourself about to write "Thanks":** DELETE IT. State the fix instead.

---

## Gracefully Correcting Your Pushback

If you pushed back and were wrong:

✅ "You were right – I checked [X] and it does [Y]. Implementing now."
✅ "Verified this and you're correct. My initial understanding was wrong because [reason]. Fixing."

❌ Long apology  
❌ Defending why you pushed back  
❌ Over-explaining

State the correction factually and move on.

## Development Commands

### Build and Run
```bash
# Build the project
mvn clean compile

# Run the application
mvn spring-boot:run

# Build JAR file
mvn clean package

# Run JAR file
java -jar target/tms-sep490-be-0.0.1-SNAPSHOT.jar

# Skip tests during build
mvn clean package -DskipTests
```

### Testing Commands
```bash
# Run all tests (unit + integration + coverage)
mvn clean verify

# Run only unit tests
mvn test

# Run specific test class
mvn test -Dtest=CenterServiceImplTest

# Run specific test method
mvn test -Dtest=CenterServiceImplTest#shouldFindCenterById

# Run tests with coverage report
mvn clean verify jacoco:report
# View coverage report: target/site/jacoco/index.html

# Run tests in parallel (faster)
mvn -T 1C clean verify

# Run integration tests only
mvn verify -DskipUnitTests
```

### Database Setup
```bash
# Start PostgreSQL with Docker (for local development)
docker run --name tms-postgres -e POSTGRES_PASSWORD=979712 -p 5432:5432 -d postgres

# Connect to database
docker exec -it tms-postgres psql -U postgres
CREATE DATABASE tms;
```

### Environment Setup on Windows
```bash
# Set JAVA_HOME (adjust path to your JDK installation)
export JAVA_HOME="/c/Users/YourUsername/.jdks/openjdk-21.0.1"
export PATH="$JAVA_HOME/bin:$PATH"

# Verify setup
java -version
mvn -version
```

## Architecture Overview

### Package Structure
```
org.fyp.tmssep490be/
├── config/              # Security, OpenAPI, JPA configurations
├── controllers/         # REST API endpoints (/api/v1/*)
├── services/           # Business logic interfaces
├── services/impl/      # Service implementations
├── repositories/       # Data access layer
├── entities/           # JPA entities
├── entities/enums/     # Enum definitions using PostgreSQL enums
├── dtos/common/        # Common DTOs (ResponseObject for standardized responses)
├── security/           # JWT authentication components
├── exceptions/         # Global exception handling
└── utils/              # Test utilities and builders
```

### Layered Architecture Pattern
- **Controllers**: Handle HTTP requests, validation, responses
- **Services**: Business logic, transaction management
- **Repositories**: Data access, database operations
- **Entities**: JPA domain models with auditing

### Key Configuration Classes
- `SecurityConfiguration`: JWT authentication and role-based authorization
- `SecurityBeanConfiguration`: JWT bean configuration and utilities
- `OpenAPIConfiguration`: Swagger UI at `/swagger-ui.html`
- `JpaAuditingConfiguration`: Automatic timestamps (`created_at`, `updated_at`)

## Security & Authentication

### JWT Implementation
- **Access tokens**: 15 minutes expiration
- **Refresh tokens**: 7 days expiration
- **Stateless sessions**: No server-side session storage
- **Role-based authorization**: ADMIN, MANAGER, CENTER_HEAD, etc.

### Authentication Flow
1. POST `/api/v1/auth/login` � JWT tokens
2. Include `Authorization: Bearer <access_token>` in API calls
3. Use refresh token to get new access token

### Security Configuration
- Public endpoints: `/api/v1/auth/login`, `/api/v1/auth/refresh`
- All other endpoints require JWT authentication
- CORS configured for frontend integration
- Role-based access control with method-level security (`@PreAuthorize`)
- JWT tokens with configurable secret via `JWT_SECRET` environment variable

## Database & Persistence

### PostgreSQL Configuration
- Development mode: `ddl-auto: create-drop` (schema recreated each run)
- PostgreSQL enum types with CHECK constraints for data validation
- Enum types initialized via `enum-init.sql`
- Comprehensive seed data loaded via `seed-data.sql` covering all business scenarios
- Automatic auditing with `@CreatedDate` and `@LastModifiedDate`

### Entity Base Class
All entities extend `BaseEntity` which provides:
- `createdAt`: Auto-populated timestamp
- `updatedAt`: Auto-updated timestamp
- Standard ID generation

### Test Database
Integration tests use Testcontainers with PostgreSQL for realistic testing.
- `PostgreSQLTestContainer` singleton pattern for better test performance
- Base test classes for consistent test setup

## API Design Standards

### REST API Conventions
- Base path: `/api/v1/`
- Standardized response format using `ResponseObject<T>` from `dtos/common` package
- Pagination support with Spring Data `Pageable`
- Validation using `@Valid` annotations
- Global exception handling via `exceptions` package

### Response Format
All API responses use `ResponseObject<T>` for consistency:
```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": { ... }
}
```

## Testing Strategy (Spring Boot 3.5.7 + Java 21)

### ⚠️ CRITICAL: Modern Testing Standards (2025)

**MUST READ:** See detailed testing guidelines in [src/test/README.md](src/test/README.md)

**Key Rules:**
1. ✅ **USE**: `@SpringBootTest` + `@MockitoBean` (Spring Boot 3.4+)
2. ❌ **NEVER USE**: `@ExtendWith(MockitoExtension.class)` + `@Mock` + `@InjectMocks`
3. ✅ Import: `org.springframework.test.context.bean.override.mockito.MockitoBean`
4. ❌ NOT: `org.springframework.boot.test.mock.mockito.MockBean` (deprecated)

**Quick Reference:**
```java
// ✅ CORRECT
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class ServiceTest {
    @Autowired private YourService service;
    @MockitoBean private YourRepository repository;
}

// ❌ WRONG - DO NOT USE
@ExtendWith(MockitoExtension.class)  // ❌ DEPRECATED
class ServiceTest {
    @Mock private YourRepository repository;  // ❌ No Spring context
    @InjectMocks private YourService service;  // ❌ Bypasses Spring DI
}
```

**Test Templates & Best Practices:** See [src/test/README.md](src/test/README.md)

## Development Workflow

### Feature Development
1. Create/update entities in `entities/` package
2. Add/update repositories in `repositories/` package
3. Implement business logic in `services/` package
4. Create/update DTOs in `dtos/` package
5. Add/update controllers in `controllers/` package
6. Write comprehensive tests for all layers

### Code Quality Standards
- Use Lombok annotations to reduce boilerplate
- Follow Spring Boot best practices
- Maintain 80%+ test coverage
- Use AssertJ for fluent assertions in tests
- Apply AAA pattern (Arrange-Act-Assert) in tests

## Common Development Tasks

### Adding New Entity
1. Create entity class extending `BaseEntity`
2. Add repository interface extending `JpaRepository`
3. Create service interface and implementation
4. Add DTOs for request/response
5. Create controller with REST endpoints
6. Write comprehensive tests

### Testing New Features
```bash
# Ensure JAVA_HOME is set first
export JAVA_HOME="/c/Users/YourUsername/.jdks/openjdk-21.0.1"

# Run specific test during development
mvn test -Dtest=CenterServiceImplTest

# Run REST API tests with REST Assured
mvn test -Dtest=RestAssuredIT

# Run tests with coverage
mvn clean verify jacoco:report

# Run tests with parallel execution
mvn -T 1C clean verify

# Debug failing tests
mvn test -X

# Use Maven wrapper if JAVA_HOME setup is problematic
./mvnw test -Dtest=CenterServiceImplTest
```

### API Documentation
- Swagger UI available at: `http://localhost:8080/swagger-ui.html`
- OpenAPI spec at: `http://localhost:8080/v3/api-docs`
- Custom styling configured in `OpenAPIConfiguration`

## Environment Variables

### Configuration
- `JWT_SECRET`: Override default JWT secret key (IMPORTANT for production)
- Database settings in `application.yml` (hardcoded for development)

### Production Considerations
- Change JWT secret via `JWT_SECRET` environment variable
- Update database credentials for production
- Change `ddl-auto` to `validate` or `none` for production
- Configure proper logging levels
- Set up proper CORS origins

## Data Import Capabilities

### Excel/CSV Import
- Apache POI for Excel file parsing (.xlsx format)
- Support for bulk data operations
- Import endpoints for courses, students, and enrollments
- CSV file processing capabilities