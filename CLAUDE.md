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

### Unit Tests
- Test business logic in service layer
- Mock repository dependencies
- Focus on edge cases: conflicts, validations, state transitions

### Integration Tests
- Test complete workflows: course creation → approval → class creation → enrollment
- Use `@SpringBootTest` with test database
- Clean up data after each test with `@Transactional` + rollback

### Key Test Scenarios
1. **Conflict Detection**: Double-booking resources, teacher conflicts
2. **Request Workflows**: Both user-initiated and staff-initiated flows
3. **Session Generation**: Correct date calculation, holiday skipping
4. **Enrollment**: Capacity validation, mid-course enrollment
5. **Makeup Logic**: Session matching, capacity checking, bidirectional linking

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

Still needed:
- ⏳ Controller layer (REST API endpoints)
- ⏳ Security configuration (JWT authentication/authorization)
- ⏳ Unit and integration tests
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
