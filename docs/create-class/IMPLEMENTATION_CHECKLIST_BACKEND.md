# ✅ BACKEND IMPLEMENTATION CHECKLIST: Class Creation Workflow

**Project:** Training Management System (TMS)
**Feature:** Complete Class Creation Workflow (7 Steps)
**Scope:** Backend Only (Spring Boot + PostgreSQL)
**Version:** 1.1.0
**Date:** January 4, 2025

---

## 📑 TABLE OF CONTENTS

1. [Pre-Implementation Setup](#pre-implementation-setup)
2. [Step 1: Create Class](#step-1-create-class)
3. [Step 2: Generate Sessions](#step-2-generate-sessions-auto)
4. [Step 3: Assign Time Slots](#step-3-assign-time-slots)
5. [Step 4: Assign Resources](#step-4-assign-resources)
6. [Step 5: Assign Teachers](#step-5-assign-teachers)
7. [Step 6: Validate](#step-6-validate)
8. [Step 7: Submit & Approve](#step-7-submit--approve)
9. [Testing Tasks](#testing-tasks)
10. [Deployment Tasks](#deployment-tasks)
11. [Documentation Tasks](#documentation-tasks)

---

## 🔧 PRE-IMPLEMENTATION SETUP

### Database Setup
- [ ] Review existing schema for `class` table
- [ ] Review existing schema for `session` table
- [ ] Review existing schema for `session_resource` table
- [ ] Review existing schema for `teaching_slot` table
- [ ] Review existing schema for `time_slot_template` table
- [ ] Verify PostgreSQL enums exist: `modality_enum`, `class_status_enum`, `approval_status_enum`, `resource_type_enum`
- [ ] Add missing columns to `class` table:
  - [ ] `submitted_at TIMESTAMP WITH TIME ZONE`
  - [ ] `approved_by BIGINT REFERENCES user_account(id)`
  - [ ] `approved_at TIMESTAMP WITH TIME ZONE`
  - [ ] `rejection_reason TEXT`
- [ ] Add indexes:
  - [ ] `CREATE INDEX idx_class_branch_code ON class(branch_id, code);`
  - [ ] `CREATE INDEX idx_session_class_date ON session(class_id, date);`
  - [ ] `CREATE INDEX idx_session_resource_resource_session ON session_resource(resource_id, session_id);`

### Project Structure
- [ ] Create package `org.fyp.tmssep490be.dtos.classmanagement`
- [ ] Verify package `org.fyp.tmssep490be.services.impl` exists
- [ ] Create `ClassManagementController.java` in controllers package

### Dependencies
- [ ] Verify Spring Boot 3.5.7 and Java 21 configured
- [ ] Verify PostgreSQL driver up to date
- [ ] Verify Lombok working
- [ ] Verify validation dependencies (`spring-boot-starter-validation`)
- [ ] Verify TestContainers for integration tests

---

## 📘 STEP 1: CREATE CLASS

### Backend - Entities
- [ ] Update `ClassEntity.java`
  - [ ] Add `scheduleDays` field (List<Integer> with SMALLINT[] column)
  - [ ] Add `plannedEndDate` field
  - [ ] Add `submittedAt`, `approvedBy`, `approvedAt`, `rejectionReason` fields
  - [ ] Add `@UniqueConstraint` on `(branch_id, code)`
  - [ ] Verify extends `BaseEntity` for auditing

### Backend - DTOs
- [ ] Create `CreateClassRequest.java`
  - [ ] Add all fields with validation annotations
  - [ ] `@Pattern` for class code format
  - [ ] `@FutureOrPresent` for start date
  - [ ] `scheduleDays` validation (1-7 values, @Min @Max)
  - [ ] `maxCapacity` validation (1-100)
- [ ] Create `CreateClassResponse.java`
  - [ ] Include `sessionsGenerated` field
  - [ ] Include `plannedEndDate` field
  - [ ] Include all class metadata

### Backend - Repository
- [ ] Update `ClassRepository.java`
  - [ ] Add `existsByBranchAndCode(Branch, String)` method
  - [ ] Add `findByIdAndBranch()` for security
  - [ ] Add `findByApprovalStatusAndBranch()` for filtering

### Backend - Service
- [ ] Update `ClassService.java` interface
  - [ ] Add `CreateClassResponse createClass(CreateClassRequest, Long userId)` signature
- [ ] Update `ClassServiceImpl.java`
  - [ ] Implement `createClass()` method
  - [ ] Validate course exists and is APPROVED
  - [ ] Validate branch exists
  - [ ] **Validate start_date is in schedule_days (ISODOW)**
  - [ ] Check unique (branch_id, code)
  - [ ] Create ClassEntity with status=DRAFT, approval_status=PENDING
  - [ ] Call `sessionGenerationService.generateSessions()`
  - [ ] Calculate and set `planned_end_date`
  - [ ] Add comprehensive logging
  - [ ] Return response

### Backend - Controller
- [ ] Update `ClassManagementController.java`
  - [ ] Add `@RestController` and `@RequestMapping("/api/v1/classes")`
  - [ ] Implement `POST /` endpoint for createClass
  - [ ] Add `@PreAuthorize("hasRole('ACADEMIC_STAFF')")`
  - [ ] Add `@Valid` for request validation
  - [ ] Return `ResponseObject<CreateClassResponse>` with 201 status
  - [ ] Add logging

### Testing - Step 1
- [ ] **Unit Tests** (`ClassServiceImplTest.java`)
  - [ ] Test successful class creation
  - [ ] Test course not approved throws BusinessException
  - [ ] Test start date not in schedule days throws ValidationException
  - [ ] Test duplicate class code throws DuplicateResourceException
  - [ ] Test branch not found throws ResourceNotFoundException
  - [ ] Test session generation is triggered
  - [ ] Test planned_end_date is set correctly
- [ ] **Integration Tests**
  - [ ] Test POST `/api/v1/classes` with valid data returns 201
  - [ ] Test POST with invalid data returns 400
  - [ ] Test POST without authentication returns 401
  - [ ] Test POST without ACADEMIC_STAFF role returns 403
  - [ ] Verify class persisted in database
  - [ ] Verify sessions generated in database

---

## 🔄 STEP 2: GENERATE SESSIONS (AUTO)

### Backend - Service
- [ ] Create `SessionGenerationService.java`
  - [ ] Implement `generateSessions(ClassEntity)` method
  - [ ] Load course_sessions from course (ordered by phase, sequence)
  - [ ] **Implement date calculation algorithm**
    - [ ] Use rotating pattern based on schedule_days
    - [ ] Handle week boundaries correctly
    - [ ] Use ISODOW for day matching
  - [ ] Create Session entities with calculated dates
  - [ ] Set type=CLASS, status=PLANNED
  - [ ] **Batch save all sessions (performance)**
  - [ ] Return count of sessions generated
  - [ ] Add comprehensive logging (trace for each session)

### Backend - Repository
- [ ] Update `SessionRepository.java`
  - [ ] Verify `saveAll()` method exists
  - [ ] Add `findTopByClassEntityOrderByDateDesc()` for end date calculation
  - [ ] Add `countByClassEntityId()` method

### Testing - Step 2
- [ ] **Unit Tests** (`SessionGenerationServiceTest.java`)
  - [ ] Test correct number of sessions generated
  - [ ] Test sessions on correct dates (verify algorithm)
  - [ ] Test date calculation for Mon/Wed/Fri pattern (36 sessions)
  - [ ] Test date calculation for different schedule patterns
  - [ ] Test sessions linked to correct course_sessions
  - [ ] Test exception when course has no sessions
  - [ ] Test week boundary transitions
- [ ] **Integration Tests**
  - [ ] Test end-to-end: create class → verify sessions in DB
  - [ ] Test sessions have correct dates matching schedule_days
  - [ ] Test first session date = start_date
  - [ ] Test last session date = planned_end_date

---

## ⏰ STEP 3: ASSIGN TIME SLOTS

### Backend - DTOs
- [ ] Create `AssignTimeSlotsRequest.java`
  - [ ] Create nested `Assignment` class (dayOfWeek, timeSlotTemplateId)
  - [ ] Add validation annotations (@Min @Max for dayOfWeek)
- [ ] Create `AssignTimeSlotsResponse.java`
  - [ ] Include assigned sessions count
  - [ ] Create nested `AssignmentResult` class
  - [ ] Include breakdown by day

### Backend - Repository
- [ ] Update `SessionRepository.java`
  - [ ] Add `updateTimeSlotByClassAndDayOfWeek()` method
  - [ ] **Use JPQL with `EXTRACT(ISODOW FROM date)`**
  - [ ] Add `@Modifying` annotation
  - [ ] Update `updatedAt` timestamp automatically

### Backend - Service
- [ ] Create `TimeSlotAssignmentService.java`
  - [ ] Implement `assignTimeSlots(classId, request)` method
  - [ ] Validate class exists
  - [ ] Validate all time slot templates exist
  - [ ] Loop through assignments
  - [ ] Call repository method to bulk update sessions per day
  - [ ] Build response with results per day
  - [ ] Add logging

### Backend - Controller
- [ ] Update `ClassManagementController.java`
  - [ ] Add `POST /{classId}/time-slots` endpoint
  - [ ] Add `@PreAuthorize("hasRole('ACADEMIC_STAFF')")`
  - [ ] Add `@Valid` for request validation
  - [ ] Return `ResponseObject<AssignTimeSlotsResponse>`
  - [ ] Dynamic success message

### Testing - Step 3
- [ ] **Unit Tests** (`TimeSlotAssignmentServiceTest.java`)
  - [ ] Test successful time slot assignment
  - [ ] Test class not found throws ResourceNotFoundException
  - [ ] Test time slot template not found throws exception
  - [ ] Test correct number of sessions updated per day
- [ ] **Repository Tests**
  - [ ] Test `updateTimeSlotByClassAndDayOfWeek()` SQL query
  - [ ] Test EXTRACT(ISODOW) works correctly
  - [ ] Test bulk update performance
- [ ] **Integration Tests**
  - [ ] Test POST `/api/v1/classes/{id}/time-slots` returns 200
  - [ ] Test time slots correctly assigned in database
  - [ ] Test different time slots for different days

---

## 🏫 STEP 4: ASSIGN RESOURCES

### Backend - DTOs
- [ ] Create `AssignResourcesRequest.java`
  - [ ] Create nested `PatternItem` class (dayOfWeek, resourceId)
  - [ ] Add validation
- [ ] Create `AssignResourcesResponse.java`
  - [ ] Include successCount, conflictCount
  - [ ] Create nested `ConflictDetail` class
  - [ ] Include session info, conflict reason, conflicting class
  - [ ] Define `ConflictType` enum
- [ ] Create `ResourceDTO.java` for available resources

### Backend - Repository
- [ ] Update `SessionResourceRepository.java`
  - [ ] **Add `bulkAssignResource()` native SQL query**
  - [ ] Use INSERT with NOT EXISTS for conflict avoidance
  - [ ] Use EXTRACT(ISODOW FROM date) for day filtering
  - [ ] Return count of inserted rows
  - [ ] Optimize for performance (target: 50-100ms)
- [ ] Update `SessionRepository.java`
  - [ ] Add `findUnassignedSessionsByDayOfWeek()` query
  - [ ] Add `findConflictingSession()` query for analysis
- [ ] Update `ResourceRepository.java`
  - [ ] Add `findAvailableResources()` query
  - [ ] Filter by branch, resource type, capacity, date, time slot

### Backend - Service
- [ ] Create `ResourceAssignmentService.java`
  - [ ] Implement `assignResources(classId, request)` method
    - [ ] **PHASE 1: SQL Bulk Assignment (Fast - 50-100ms)**
      - [ ] Loop through pattern items
      - [ ] Call `bulkAssignResource()` for each day
      - [ ] Accumulate success count
    - [ ] **PHASE 2: Conflict Analysis (50-100ms)**
      - [ ] Find unassigned sessions per day
      - [ ] Analyze each conflict with `analyzeResourceConflict()`
      - [ ] Build conflict details with reason
  - [ ] Implement `analyzeResourceConflict()` helper
    - [ ] Find conflicting session (same resource, date, time)
    - [ ] Determine reason (booked by class X, maintenance, etc.)
    - [ ] Return ConflictDetail
  - [ ] Implement `getAvailableResources()` method
    - [ ] Determine search criteria (from session or params)
    - [ ] Query available resources
    - [ ] Filter by modality
  - [ ] Add performance logging

### Backend - Controller
- [ ] Update `ClassManagementController.java`
  - [ ] Add `POST /{classId}/resources` endpoint
  - [ ] Add `GET /{classId}/available-resources` endpoint with query params
  - [ ] Add authorization checks
  - [ ] Handle success/conflict messages dynamically
  - [ ] Return proper HTTP status

### Testing - Step 4
- [ ] **Unit Tests** (`ResourceAssignmentServiceTest.java`)
  - [ ] Test bulk assignment returns correct count
  - [ ] Test conflict detection works
  - [ ] Test conflict analysis provides correct reason
  - [ ] Test available resources query filters correctly
  - [ ] Test HYBRID approach completes in < 200ms
- [ ] **Repository Tests**
  - [ ] Test `bulkAssignResource()` SQL performance
  - [ ] Test conflict detection query accuracy
  - [ ] Test available resources query
- [ ] **Integration Tests**
  - [ ] Test POST `/api/v1/classes/{id}/resources` with no conflicts
  - [ ] Test POST with conflicts returns conflict details
  - [ ] Test GET `/api/v1/classes/{id}/available-resources` returns filtered list
  - [ ] Test bulk insert in database

---

## 👨‍🏫 STEP 5: ASSIGN TEACHERS

### Backend - DTOs
- [ ] Create `AvailableTeachersResponse.java`
  - [ ] Include classId, totalSessions
  - [ ] Include list of `TeacherAvailabilityDTO`
- [ ] Create `TeacherAvailabilityDTO.java`
  - [ ] Teacher basic info (id, name, employee code, contract type)
  - [ ] Skills array, maxLevel, hasGeneralSkill
  - [ ] Availability stats (total, available, percentage)
  - [ ] Availability status (fully/partially/unavailable)
  - [ ] Create nested `ConflictCounts` class
- [ ] Create `AssignTeacherRequest.java`
  - [ ] teacherId, role (primary/substitute)
  - [ ] sessionIds (optional, for substitute)
  - [ ] Add validation
- [ ] Create `AssignTeacherResponse.java`
  - [ ] Include assignedCount, needsSubstitute
  - [ ] Create nested `RemainingSession` class

### Backend - Repository
- [ ] Update `TeacherRepository.java`
  - [ ] **Add `findAvailableTeachersWithPreCheck()` native SQL query**
  - [ ] **CTE 1: skill_matched_teachers**
    - [ ] Match teachers by skill (or has 'general')
    - [ ] Aggregate skills per teacher
  - [ ] **CTE 2: session_conflicts**
    - [ ] Cross join teachers with sessions
    - [ ] Count 3 conflict types: no_availability, teaching_conflict, leave_conflict
  - [ ] **Main Query**
    - [ ] Calculate availableSessions = total - conflicts
    - [ ] Calculate availability percentage
    - [ ] Determine availability status (CASE)
    - [ ] Order by contract type, available sessions DESC
  - [ ] Optimize for performance (target: 200-300ms)
- [ ] Update `SessionRepository.java`
  - [ ] Add `findSessionsMatchingTeacherSkills()` query
  - [ ] Add `findUnassignedSessions()` query

### Backend - Service
- [ ] Create `TeacherAssignmentService.java`
  - [ ] Implement `getAvailableTeachersWithPreCheck(classId, skillFilter)` method
    - [ ] Call repository PRE-CHECK query
    - [ ] Return response with teacher list sorted by availability
    - [ ] Add performance logging
  - [ ] Implement `assignTeacher(classId, request)` method
    - [ ] Validate class and teacher exist
    - [ ] Determine sessions to assign (all matching or specific IDs)
    - [ ] Create TeachingSlot entities
    - [ ] Batch save teaching slots
    - [ ] Check if needs substitute (assigned < total)
    - [ ] Return response with assignment details
    - [ ] Add logging

### Backend - Controller
- [ ] Update `ClassManagementController.java`
  - [ ] Add `GET /{classId}/available-teachers` endpoint
    - [ ] Optional skillSet query param
    - [ ] Return `AvailableTeachersResponse`
  - [ ] Add `POST /{classId}/teachers` endpoint
    - [ ] Accept `AssignTeacherRequest`
    - [ ] Return `AssignTeacherResponse`
    - [ ] Dynamic success message based on needsSubstitute

### Testing - Step 5
- [ ] **Unit Tests** (`TeacherAssignmentServiceTest.java`)
  - [ ] Test PRE-CHECK returns fully available teachers
  - [ ] Test PRE-CHECK returns partially available teachers
  - [ ] Test PRE-CHECK calculates conflict counts correctly
  - [ ] Test PRE-CHECK query completes in < 300ms
  - [ ] Test direct assignment without re-checking
  - [ ] Test partial assignment sets needsSubstitute = true
  - [ ] Test 'general' skill matches all sessions
- [ ] **Repository Tests**
  - [ ] Test `findAvailableTeachersWithPreCheck()` SQL query
  - [ ] Test skill matching logic (including 'general')
  - [ ] Test conflict detection (3 types)
  - [ ] Test sorting order (contract type, availability)
- [ ] **Integration Tests**
  - [ ] Test GET `/api/v1/classes/{id}/available-teachers` returns sorted list
  - [ ] Test POST `/api/v1/classes/{id}/teachers` assigns successfully
  - [ ] Test POST with specific sessionIds (substitute scenario)
  - [ ] Verify teaching_slot records in database

---

## ✅ STEP 6: VALIDATE

### Backend - DTOs
- [ ] Create `ValidateClassResponse.java`
  - [ ] isValid, canSubmit flags
  - [ ] totalSessions count
  - [ ] Create nested `Checks` class (3 boolean flags)
  - [ ] errors list (blocking issues)
  - [ ] warnings list (non-blocking issues)

### Backend - Repository
- [ ] Update `SessionRepository.java`
  - [ ] Add `countByClassEntityIdAndTimeSlotTemplateIsNull()` method
  - [ ] Add `countByClassEntityIdAndNotHavingResources()` custom query
  - [ ] Add `countByClassEntityIdAndNotHavingTeachers()` custom query
- [ ] Update `TeachingSlotRepository.java`
  - [ ] Add `countDistinctTeachersByClassId()` method

### Backend - Service
- [ ] Update `ClassServiceImpl.java`
  - [ ] Implement `validateClass(classId)` method
    - [ ] **Check 1: Time slots**
      - [ ] Count sessions without time_slot_template
      - [ ] Add error if count > 0
    - [ ] **Check 2: Resources**
      - [ ] Count sessions without session_resource
      - [ ] Add error if count > 0
    - [ ] **Check 3: Teachers**
      - [ ] Count sessions without teaching_slot
      - [ ] Add error if count > 0
    - [ ] **Warning 1: Multiple teachers**
      - [ ] Count distinct teachers
      - [ ] Add warning if > 1
    - [ ] **Warning 2: Start date in past**
      - [ ] Check if start_date < today
      - [ ] Add warning if true
    - [ ] Set isValid = errors.isEmpty()
    - [ ] Return validation response
    - [ ] Add logging

### Backend - Controller
- [ ] Update `ClassManagementController.java`
  - [ ] Add `POST /{classId}/validate` endpoint
  - [ ] Return `ResponseObject<ValidateClassResponse>`
  - [ ] Dynamic message based on isValid

### Testing - Step 6
- [ ] **Unit Tests** (`ClassServiceImplTest.java`)
  - [ ] Test validation passes when all complete
  - [ ] Test validation fails when missing time slots
  - [ ] Test validation fails when missing resources
  - [ ] Test validation fails when missing teachers
  - [ ] Test validation includes warnings
  - [ ] Test canSubmit = false when errors exist
- [ ] **Repository Tests**
  - [ ] Test counting methods for incomplete assignments
- [ ] **Integration Tests**
  - [ ] Test POST `/api/v1/classes/{id}/validate` returns valid response
  - [ ] Test validation with incomplete class returns errors
  - [ ] Test validation with warnings still valid

---

## 📨 STEP 7: SUBMIT & APPROVE

### Backend - DTOs
- [ ] Create `SubmitClassResponse.java`
  - [ ] Include classId, code, status, approvalStatus
  - [ ] Include submittedAt, submittedBy
- [ ] Create `ApproveClassResponse.java`
  - [ ] Include classId, code, status, approvalStatus
  - [ ] Include approvedBy, approvedAt
- [ ] Create `RejectClassRequest.java`
  - [ ] reason field (validation: @NotBlank, @Size min=10)
- [ ] Create `RejectClassResponse.java`
  - [ ] Include classId, code, status, approvalStatus
  - [ ] Include rejectionReason, rejectedBy, rejectedAt

### Backend - Service
- [ ] Update `ClassServiceImpl.java`
  - [ ] Implement `submitClass(classId, userId)` method
    - [ ] Validate class exists
    - [ ] Call validateClass() to ensure completeness
    - [ ] Throw exception if !canSubmit
    - [ ] Set submittedAt = now
    - [ ] Save class
    - [ ] (Optional) Call notificationService to notify Center Head
    - [ ] Return response
    - [ ] Add logging
  - [ ] Implement `approveClass(classId, userId)` method
    - [ ] Validate class exists
    - [ ] Check submittedAt is not null
    - [ ] Set status = SCHEDULED
    - [ ] Set approvalStatus = APPROVED
    - [ ] Set approvedBy = userId
    - [ ] Set approvedAt = now
    - [ ] Save class
    - [ ] (Optional) Call notificationService
    - [ ] Return response
    - [ ] Add logging
  - [ ] Implement `rejectClass(classId, reason, userId)` method
    - [ ] Validate class exists
    - [ ] Set status = DRAFT
    - [ ] Set approvalStatus = REJECTED
    - [ ] Set rejectionReason = reason
    - [ ] Set submittedAt = null (reset)
    - [ ] Save class
    - [ ] (Optional) Call notificationService
    - [ ] Return response
    - [ ] Add logging

### Backend - Controller
- [ ] Update `ClassManagementController.java`
  - [ ] Add `POST /{classId}/submit` endpoint
    - [ ] `@PreAuthorize("hasRole('ACADEMIC_STAFF')")`
    - [ ] Return `SubmitClassResponse`
  - [ ] Add `POST /{classId}/approve` endpoint
    - [ ] `@PreAuthorize("hasRole('CENTER_HEAD')")`
    - [ ] Return `ApproveClassResponse`
  - [ ] Add `POST /{classId}/reject` endpoint
    - [ ] `@PreAuthorize("hasRole('CENTER_HEAD')")`
    - [ ] Accept `RejectClassRequest` with validation
    - [ ] Return `RejectClassResponse`
  - [ ] Add logging for all endpoints

### Backend - Notification Service (Optional/Future)
- [ ] Create `NotificationService.java` (if not exists)
  - [ ] Implement `notifyClassSubmission(classEntity)` stub
  - [ ] Implement `notifyClassApproved(classEntity)` stub
  - [ ] Implement `notifyClassRejected(classEntity, reason)` stub
  - [ ] *Note: Full notification implementation can be done later*

### Testing - Step 7
- [ ] **Unit Tests** (`ClassServiceImplTest.java`)
  - [ ] Test submitClass() succeeds when valid
  - [ ] Test submitClass() throws exception when invalid
  - [ ] Test approveClass() updates status correctly
  - [ ] Test approveClass() throws exception if not submitted
  - [ ] Test rejectClass() resets submission and sets reason
  - [ ] Test rejectClass() sets status = DRAFT
- [ ] **Integration Tests**
  - [ ] Test POST `/api/v1/classes/{id}/submit` returns 200
  - [ ] Test POST `/api/v1/classes/{id}/approve` by CENTER_HEAD returns 200
  - [ ] Test POST `/api/v1/classes/{id}/approve` by ACADEMIC_STAFF returns 403
  - [ ] Test POST `/api/v1/classes/{id}/reject` with reason returns 200
  - [ ] Test POST `/api/v1/classes/{id}/reject` without reason returns 400
  - [ ] Verify status transitions in database

---

## 🧪 TESTING TASKS

### Unit Testing Setup
- [ ] Set up `@SpringBootTest` with test profile
- [ ] Configure `@MockitoBean` for all service tests
- [ ] Create test data builders (`TestDataBuilder.java`)
- [ ] Set up AssertJ for fluent assertions
- [ ] Configure coverage reporting (JaCoCo)

### Unit Tests - Service Layer
- [ ] Write tests for all ClassService methods (90%+ coverage)
- [ ] Write tests for SessionGenerationService
- [ ] Write tests for TimeSlotAssignmentService
- [ ] Write tests for ResourceAssignmentService
- [ ] Write tests for TeacherAssignmentService
- [ ] Test all exception scenarios
- [ ] Test edge cases (boundary values)
- [ ] Follow AAA pattern (Arrange-Act-Assert)

### Unit Tests - Repository Layer
- [ ] Test custom JPQL queries
- [ ] Test native SQL queries
- [ ] Test EXTRACT(ISODOW) functionality
- [ ] Test bulk operations performance
- [ ] Test transaction boundaries

### Integration Testing Setup
- [ ] Set up TestContainers with PostgreSQL
- [ ] Create seed data scripts for test database
- [ ] Configure test database rollback
- [ ] Set up REST Assured for API testing

### Integration Tests - End-to-End Workflow
- [ ] Test: Create → Generate → Assign All → Submit → Approve
- [ ] Test: Create → Partial Assign → Validate Fails
- [ ] Test: Submit → Reject → Fix → Resubmit → Approve
- [ ] Verify database state at each step

### Integration Tests - API Endpoints
- [ ] Test all CRUD operations
- [ ] Test authentication (401 when no token)
- [ ] Test authorization (403 when wrong role)
- [ ] Test validation (400 when invalid data)
- [ ] Test pagination and filtering (if applicable)
- [ ] Test error responses (proper format)

### Performance Testing
- [ ] Test bulk resource assignment (target: < 200ms for 36 sessions)
- [ ] Test teacher PRE-CHECK query (target: < 300ms)
- [ ] Test session generation (target: < 500ms)
- [ ] Test full workflow end-to-end (target: < 5s)
- [ ] Identify and optimize slow queries
- [ ] Use PostgreSQL EXPLAIN ANALYZE for query optimization

### Test Data Management
- [ ] Create comprehensive test data builders
- [ ] Create helper methods for common setups
- [ ] Ensure test data is isolated (no cross-test dependencies)
- [ ] Clean up test data after each test

### Test Coverage
- [ ] Achieve 90%+ unit test coverage
- [ ] Achieve 80%+ integration test coverage
- [ ] Run `mvn clean verify jacoco:report`
- [ ] Review coverage report and fill gaps

---

## 🚀 DEPLOYMENT TASKS

### Pre-Deployment Checklist
- [ ] Run all tests and ensure passing
  - [ ] `mvn clean test` (unit tests)
  - [ ] `mvn clean verify` (integration tests)
- [ ] Review test coverage reports
- [ ] Code review completed and approved
- [ ] Update OpenAPI specification (`/docs/create-class/openapi.yaml`)
- [ ] Generate API documentation (Swagger UI accessible)
- [ ] Performance testing completed
- [ ] Security audit completed (SQL injection prevention, input validation)
- [ ] Environment variables documented
  - [ ] `JWT_SECRET`
  - [ ] Database credentials

### Database Migration Scripts
- [ ] Write Flyway/Liquibase migration scripts
  - [ ] `V1__add_class_workflow_columns.sql`
    - [ ] Add new columns to class table
  - [ ] `V2__create_class_indexes.sql`
    - [ ] Create performance indexes
  - [ ] `V3__add_constraints.sql` (if needed)
    - [ ] Add check constraints
    - [ ] Add foreign key constraints
- [ ] Test migrations on local database
- [ ] Test migrations on staging database
- [ ] Prepare rollback scripts
- [ ] Document migration order and dependencies

### Staging Deployment
- [ ] Build backend: `mvn clean package -DskipTests`
- [ ] Verify JAR file created: `target/tms-sep490-be-0.0.1-SNAPSHOT.jar`
- [ ] Deploy JAR to staging server
- [ ] Run database migrations on staging
  - [ ] `flyway migrate` or equivalent
- [ ] Verify application starts successfully
- [ ] Check logs for errors
- [ ] Run smoke tests on staging
  - [ ] Test POST `/api/v1/classes` (create class)
  - [ ] Test complete workflow
- [ ] Test authentication and authorization
- [ ] Load testing on staging (if applicable)

### Production Deployment
- [ ] Backup production database
- [ ] Schedule maintenance window (if needed)
- [ ] Deploy backend to production
  - [ ] Stop old application
  - [ ] Deploy new JAR
  - [ ] Start new application
- [ ] Run database migrations on production
  - [ ] Verify migrations complete successfully
- [ ] Verify application health
  - [ ] Check `/actuator/health` endpoint
  - [ ] Check logs for startup errors
- [ ] Run smoke tests on production
  - [ ] Test critical endpoints
  - [ ] Verify authentication works
- [ ] Monitor logs for errors (first 30 minutes)
- [ ] Monitor performance metrics
  - [ ] API response times
  - [ ] Database query performance
  - [ ] Memory usage
  - [ ] CPU usage
- [ ] Notify stakeholders of completion

### Rollback Plan (if needed)
- [ ] Keep previous Docker image/JAR ready
- [ ] Keep database backup ready
- [ ] Document rollback procedure
  - [ ] Stop current application
  - [ ] Restore database from backup (if schema changed)
  - [ ] Deploy previous version
  - [ ] Verify rollback successful
- [ ] Test rollback procedure in staging

### Post-Deployment Monitoring
- [ ] Monitor application logs for 24 hours
- [ ] Monitor error rates
- [ ] Monitor API response times (p50, p95, p99)
- [ ] Check database performance
- [ ] Review slow query logs
- [ ] Gather user feedback
- [ ] Create release notes
- [ ] Update project documentation

---

## 📚 DOCUMENTATION TASKS

### Code Documentation
- [ ] Add JavaDoc comments to all public methods
- [ ] Add JavaDoc comments to all service classes
- [ ] Add JavaDoc comments to all DTOs
- [ ] Document complex algorithms (session generation, conflict detection)
- [ ] Document SQL queries with explanations
  - [ ] Explain ISODOW usage
  - [ ] Explain HYBRID approach
  - [ ] Explain PRE-CHECK approach
- [ ] Add inline comments for non-obvious logic
- [ ] Document PostgreSQL enum usage

### API Documentation
- [ ] Ensure all endpoints in OpenAPI spec
- [ ] Add request/response examples for each endpoint
- [ ] Document error codes and messages
- [ ] Document authentication requirements
- [ ] Document query parameters
- [ ] Update Swagger UI annotations in controllers
  - [ ] `@Operation` for each endpoint
  - [ ] `@ApiResponse` for different status codes
  - [ ] `@Parameter` for path/query params

### Technical Documentation
- [ ] Update README.md with new features
- [ ] Document HYBRID approach rationale and performance
- [ ] Document PRE-CHECK approach rationale
- [ ] Document PostgreSQL ISODOW usage
- [ ] Document 'general' skill behavior
- [ ] Create/update database schema diagram
- [ ] Create sequence diagrams for complex flows
  - [ ] Session generation flow
  - [ ] HYBRID resource assignment flow
  - [ ] PRE-CHECK teacher assignment flow

### Architecture Documentation
- [ ] Update architecture overview in CLAUDE.md
- [ ] Document new package structure
- [ ] Document new entities and relationships
- [ ] Document service layer design
- [ ] Document repository patterns
- [ ] Document security configuration changes

### Testing Documentation
- [ ] Document test strategy
- [ ] Document how to run tests
  - [ ] Unit tests: `mvn test`
  - [ ] Integration tests: `mvn verify`
  - [ ] Coverage report: `mvn jacoco:report`
- [ ] Document TestContainers setup
- [ ] Document test data creation utilities
- [ ] Create testing best practices guide

### Performance Documentation
- [ ] Document performance benchmarks
  - [ ] Bulk resource assignment: ~100-200ms
  - [ ] Teacher PRE-CHECK: ~200-300ms
  - [ ] Session generation: ~100-500ms
- [ ] Document optimization techniques used
- [ ] Document database indexing strategy

---

## 📊 PROGRESS TRACKING

### Overall Progress by Step
- [ ] **Step 1: Create Class** (0/9 sections completed)
- [ ] **Step 2: Generate Sessions** (0/4 sections completed)
- [ ] **Step 3: Assign Time Slots** (0/6 sections completed)
- [ ] **Step 4: Assign Resources** (0/6 sections completed)
- [ ] **Step 5: Assign Teachers** (0/6 sections completed)
- [ ] **Step 6: Validate** (0/5 sections completed)
- [ ] **Step 7: Submit & Approve** (0/5 sections completed)
- [ ] **Testing** (0/10 sections completed)
- [ ] **Deployment** (0/4 sections completed)
- [ ] **Documentation** (0/6 sections completed)

### Milestones
- [ ] **M1:** Steps 1-2 complete with tests (Week 1 - Target: Day 5)
- [ ] **M2:** Steps 3-4 complete with tests (Week 2 - Target: Day 10)
- [ ] **M3:** Steps 5-6 complete with tests (Week 3 - Target: Day 15)
- [ ] **M4:** Step 7 complete, E2E tested (Week 4 - Target: Day 17)
- [ ] **M5:** Production deployment (Week 4 - Target: Day 20)

---

## ✅ DEFINITION OF DONE

A task is considered "done" when:

### 1. Code Complete
- [ ] Implementation matches specification
- [ ] Code follows project coding standards (CLAUDE.md)
- [ ] No code smells or obvious bugs
- [ ] No hardcoded values (use constants/configs)
- [ ] Logging added for important operations
- [ ] Error handling properly implemented

### 2. Tested
- [ ] Unit tests written and passing
- [ ] Integration tests written and passing (if applicable)
- [ ] Coverage meets targets (90% unit, 80% integration)
- [ ] Manual testing completed
- [ ] Edge cases tested

### 3. Reviewed
- [ ] Code review completed by peer
- [ ] All review comments addressed
- [ ] Approved by tech lead
- [ ] No merge conflicts

### 4. Documented
- [ ] JavaDoc added for public methods
- [ ] API documentation updated
- [ ] Complex logic explained in comments
- [ ] README updated if needed

### 5. Integrated
- [ ] Merged to develop branch
- [ ] CI/CD pipeline passing
- [ ] No breaking changes to existing features
- [ ] Database migrations tested

---

## 📝 IMPORTANT NOTES

### Technical Reminders

1. **PostgreSQL ISODOW**: Day numbering is 1=Monday through 7=Sunday
2. **'general' Skill**: Acts as a universal skill that can teach any session
3. **HYBRID Approach**: Use SQL bulk operations first (fast), then Java for conflict analysis (detailed)
4. **PRE-CHECK v1.1**: Always check availability BEFORE showing teachers to user (no trial-and-error)
5. **Dual Status Fields**: Classes have both `status` (DRAFT/SCHEDULED) and `approval_status` (PENDING/APPROVED/REJECTED)
6. **Validation Before Submit**: Always call `validateClass()` before allowing submission
7. **Performance Targets**:
   - Bulk resource assignment: < 200ms
   - Teacher PRE-CHECK: < 300ms
   - Session generation: < 500ms

### Best Practices

1. **Always use @Transactional** for service methods that modify data
2. **Use MockitoBean** (not @Mock + @InjectMocks) for Spring Boot 3.4+ tests
3. **Use AssertJ** for fluent test assertions
4. **Follow AAA pattern** in tests (Arrange-Act-Assert)
5. **Add comprehensive logging** (INFO for operations, DEBUG for details, TRACE for debugging)
6. **Validate input** at controller level with `@Valid`
7. **Handle exceptions** with global exception handler
8. **Use builder pattern** for DTOs and entities
9. **Optimize SQL queries** with proper indexing
10. **Test performance** with realistic data volumes

### Known Limitations

- Resource assignment assumes no overlapping time slots on same date
- Teacher availability must be pre-configured (no on-the-fly updates)
- Rejection reason is free text (no predefined categories)
- No support for recurring maintenance windows yet

### Future Enhancements (Technical Debt)

- [ ] Implement full notification system (email/in-app)
- [ ] Add caching for frequently accessed data (courses, branches, time slots)
- [ ] Add audit log for all state changes
- [ ] Implement soft delete for classes
- [ ] Add bulk operations for multiple classes
- [ ] Add class cloning feature
- [ ] Add undo/redo functionality
- [ ] Optimize teacher PRE-CHECK query further

---

**Status:** 🚀 Ready to Start Implementation (Backend Only)
**Last Updated:** January 4, 2025
**Version:** 1.1.0 (Backend Focus)
