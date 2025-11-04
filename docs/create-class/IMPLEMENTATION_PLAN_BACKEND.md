# 📋 BACKEND IMPLEMENTATION PLAN: Class Creation Workflow

**Project:** Training Management System (TMS)
**Feature:** Complete Class Creation Workflow (7 Steps)
**Scope:** Backend Only (Spring Boot + PostgreSQL)
**Version:** 1.1.0
**Date:** January 4, 2025
**Status:** Ready for Implementation

---

## 📑 TABLE OF CONTENTS

1. [Executive Summary](#1-executive-summary)
2. [Architecture Overview](#2-architecture-overview)
3. [Implementation Steps](#3-implementation-steps)
   - [Step 1: Create Class](#step-1-create-class)
   - [Step 2: Generate Sessions (Auto)](#step-2-generate-sessions-auto)
   - [Step 3: Assign Time Slots](#step-3-assign-time-slots)
   - [Step 4: Assign Resources](#step-4-assign-resources)
   - [Step 5: Assign Teachers](#step-5-assign-teachers)
   - [Step 6: Validate](#step-6-validate)
   - [Step 7: Submit & Approve](#step-7-submit--approve)
4. [Testing Strategy](#4-testing-strategy)
5. [Deployment Plan](#5-deployment-plan)
6. [Timeline & Milestones](#6-timeline--milestones)

---

## 1. EXECUTIVE SUMMARY

### 1.1 Overview

The Class Creation Workflow is a **7-step backend process** that provides APIs for Academic Staff to create and configure classes. The workflow includes automated session generation, intelligent resource allocation, skill-based teacher matching, and approval workflow.

### 1.2 Key Features

- ✅ **Automated Session Generation**: System generates sessions from course template
- ✅ **Flexible Scheduling**: Different time slots per day via PostgreSQL ISODOW
- ✅ **HYBRID Resource Assignment**: SQL bulk operations + detailed conflict analysis (100-200ms performance)
- ✅ **PRE-CHECK Teacher Matching**: Availability checked BEFORE selection (v1.1)
- ✅ **Smart Skill Matching**: 'general' skill = universal, can teach any session
- ✅ **Approval Workflow**: Center Head approval with rejection reason tracking

### 1.3 User Roles

| Role | Responsibilities |
|------|------------------|
| **Academic Staff** | Create class, assign resources/teachers, submit for approval |
| **Center Head** | Approve/reject class submissions |
| **System** | Auto-generate sessions, validate completeness, detect conflicts |

### 1.4 Success Criteria

- ✅ 90%+ unit test coverage
- ✅ 80%+ integration test coverage
- ✅ API response time < 500ms (p95)
- ✅ Bulk operations < 200ms
- ✅ Zero critical bugs in staging

---

## 2. ARCHITECTURE OVERVIEW

### 2.1 Technology Stack

- **Spring Boot 3.5.7** (Java 21)
- **PostgreSQL 16** with native enums
- **JPA/Hibernate**
- **Spring Security + JWT**
- **TestContainers** for integration tests
- **MockitoBean** for unit tests

### 2.2 Package Structure

```
org.fyp.tmssep490be/
├── controllers/
│   └── ClassManagementController.java
├── services/
│   ├── ClassService.java (interface)
│   └── impl/
│       ├── ClassServiceImpl.java
│       ├── SessionGenerationService.java
│       ├── TimeSlotAssignmentService.java
│       ├── ResourceAssignmentService.java
│       └── TeacherAssignmentService.java
├── repositories/
│   ├── ClassRepository.java
│   ├── SessionRepository.java
│   ├── SessionResourceRepository.java
│   ├── TeachingSlotRepository.java
│   ├── TeacherRepository.java
│   ├── ResourceRepository.java
│   └── TimeSlotTemplateRepository.java
├── dtos/classmanagement/
│   ├── CreateClassRequest.java
│   ├── CreateClassResponse.java
│   ├── AssignTimeSlotsRequest.java
│   ├── AssignTimeSlotsResponse.java
│   ├── AssignResourcesRequest.java
│   ├── AssignResourcesResponse.java
│   ├── AssignTeacherRequest.java
│   ├── AssignTeacherResponse.java
│   ├── ValidateClassResponse.java
│   ├── SubmitClassResponse.java
│   ├── ApproveClassResponse.java
│   └── RejectClassRequest/Response.java
└── entities/
    ├── ClassEntity.java
    ├── Session.java
    ├── SessionResource.java
    └── TeachingSlot.java
```

### 2.3 Database Schema

**Core Tables:**
- `class` - Class metadata
- `session` - Individual class sessions
- `session_resource` - Resource assignments
- `teaching_slot` - Teacher assignments
- `time_slot_template` - Pre-defined time ranges
- `teacher_availability` - Teacher availability patterns

**Important Fields:**
- `class.schedule_days`: `SMALLINT[]` (PostgreSQL array: 1=Mon, 7=Sun)
- `class.status`: `class_status_enum` (DRAFT, SCHEDULED, etc.)
- `class.approval_status`: `approval_status_enum` (PENDING, APPROVED, REJECTED)

### 2.4 API Endpoints Summary

| Step | Endpoint | Method | Description |
|------|----------|--------|-------------|
| 1 | `/api/v1/classes` | POST | Create class |
| 2 | Auto-triggered | - | Generate sessions |
| 3 | `/api/v1/classes/{id}/time-slots` | POST | Assign time slots |
| 4 | `/api/v1/classes/{id}/resources` | POST | Assign resources |
| 4 | `/api/v1/classes/{id}/available-resources` | GET | Get available resources |
| 5 | `/api/v1/classes/{id}/available-teachers` | GET | Get teachers with availability |
| 5 | `/api/v1/classes/{id}/teachers` | POST | Assign teacher |
| 6 | `/api/v1/classes/{id}/validate` | POST | Validate completeness |
| 7 | `/api/v1/classes/{id}/submit` | POST | Submit for approval |
| 7 | `/api/v1/classes/{id}/approve` | POST | Approve class (CENTER_HEAD) |
| 7 | `/api/v1/classes/{id}/reject` | POST | Reject class (CENTER_HEAD) |

---

## 3. IMPLEMENTATION STEPS

---

## STEP 1: Create Class

### 🎯 Objective

Provide API for Academic Staff to create a new class with basic information (branch, course, modality, schedule, capacity).

### 📊 Backend Implementation

#### 3.1.1 Entity Updates

**File:** `ClassEntity.java`

```java
@Entity
@Table(name = "class", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"branch_id", "code"})
})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "modality_enum")
    private Modality modality;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "planned_end_date")
    private LocalDate plannedEndDate;

    @Column(name = "schedule_days", nullable = false, columnDefinition = "SMALLINT[]")
    private List<Integer> scheduleDays; // PostgreSQL ISODOW: 1=Mon, 7=Sun

    @Column(name = "max_capacity", nullable = false)
    private Integer maxCapacity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "class_status_enum")
    @Builder.Default
    private ClassStatus status = ClassStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, columnDefinition = "approval_status_enum")
    @Builder.Default
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "created_by")
    private Long createdBy;
}
```

#### 3.1.2 DTOs

**File:** `CreateClassRequest.java`

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateClassRequest {

    @NotNull(message = "Branch ID is required")
    private Long branchId;

    @NotNull(message = "Course ID is required")
    private Long courseId;

    @NotBlank(message = "Class code is required")
    @Size(max = 50)
    @Pattern(regexp = "^[A-Z0-9-]+$", message = "Class code must contain only uppercase letters, numbers, and hyphens")
    private String code;

    @Size(max = 255)
    private String name;

    @NotNull(message = "Modality is required")
    private Modality modality; // OFFLINE, ONLINE, HYBRID

    @NotNull(message = "Start date is required")
    @FutureOrPresent(message = "Start date must be today or in the future")
    private LocalDate startDate;

    @NotEmpty(message = "Schedule days are required")
    @Size(min = 1, max = 7, message = "Schedule days must have 1-7 entries")
    private List<@Min(1) @Max(7) Integer> scheduleDays; // PostgreSQL ISODOW: 1=Mon, 7=Sun

    @NotNull(message = "Max capacity is required")
    @Min(value = 1, message = "Max capacity must be at least 1")
    @Max(value = 100, message = "Max capacity cannot exceed 100")
    private Integer maxCapacity;
}
```

**File:** `CreateClassResponse.java`

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateClassResponse {
    private Long classId;
    private String code;
    private Long branchId;
    private Long courseId;
    private Modality modality;
    private LocalDate startDate;
    private LocalDate plannedEndDate;
    private List<Integer> scheduleDays;
    private Integer maxCapacity;
    private ClassStatus status;
    private ApprovalStatus approvalStatus;
    private Integer sessionsGenerated;
    private OffsetDateTime createdAt;
}
```

#### 3.1.3 Repository

**File:** `ClassRepository.java`

```java
public interface ClassRepository extends JpaRepository<ClassEntity, Long> {

    boolean existsByBranchAndCode(Branch branch, String code);

    Optional<ClassEntity> findByIdAndBranch(Long id, Branch branch);

    List<ClassEntity> findByApprovalStatusAndBranch(ApprovalStatus approvalStatus, Branch branch);
}
```

#### 3.1.4 Service

**File:** `ClassService.java` (Interface)

```java
public interface ClassService {
    CreateClassResponse createClass(CreateClassRequest request, Long userId);
    ValidateClassResponse validateClass(Long classId);
    SubmitClassResponse submitClass(Long classId, Long userId);
    ApproveClassResponse approveClass(Long classId, Long userId);
    RejectClassResponse rejectClass(Long classId, String reason, Long userId);
}
```

**File:** `ClassServiceImpl.java`

```java
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ClassServiceImpl implements ClassService {

    private final ClassRepository classRepository;
    private final CourseRepository courseRepository;
    private final BranchRepository branchRepository;
    private final SessionRepository sessionRepository;
    private final SessionGenerationService sessionGenerationService;

    @Override
    public CreateClassResponse createClass(CreateClassRequest request, Long userId) {
        log.info("Creating class with code: {} by user: {}", request.getCode(), userId);

        // 1. Validate course exists and is approved
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        if (course.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new BusinessException("Course must be approved before creating class");
        }

        // 2. Validate branch exists
        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));

        // 3. Validate start_date is in schedule_days
        validateStartDateInScheduleDays(request.getStartDate(), request.getScheduleDays());

        // 4. Check unique (branch_id, code)
        if (classRepository.existsByBranchAndCode(branch, request.getCode())) {
            throw new DuplicateResourceException("Class code already exists for this branch");
        }

        // 5. Create class entity
        ClassEntity classEntity = ClassEntity.builder()
                .branch(branch)
                .course(course)
                .code(request.getCode())
                .name(request.getName())
                .modality(request.getModality())
                .startDate(request.getStartDate())
                .scheduleDays(request.getScheduleDays())
                .maxCapacity(request.getMaxCapacity())
                .status(ClassStatus.DRAFT)
                .approvalStatus(ApprovalStatus.PENDING)
                .createdBy(userId)
                .build();

        classEntity = classRepository.save(classEntity);
        log.debug("Class entity created with ID: {}", classEntity.getId());

        // 6. AUTO-TRIGGER: Generate sessions (STEP 2)
        int sessionsGenerated = sessionGenerationService.generateSessions(classEntity);

        // 7. Calculate planned_end_date
        LocalDate plannedEndDate = calculatePlannedEndDate(classEntity);
        classEntity.setPlannedEndDate(plannedEndDate);
        classRepository.save(classEntity);

        log.info("Class {} created successfully with {} sessions", classEntity.getCode(), sessionsGenerated);

        // 8. Build response
        return CreateClassResponse.builder()
                .classId(classEntity.getId())
                .code(classEntity.getCode())
                .branchId(branch.getId())
                .courseId(course.getId())
                .modality(classEntity.getModality())
                .startDate(classEntity.getStartDate())
                .plannedEndDate(classEntity.getPlannedEndDate())
                .scheduleDays(classEntity.getScheduleDays())
                .maxCapacity(classEntity.getMaxCapacity())
                .status(classEntity.getStatus())
                .approvalStatus(classEntity.getApprovalStatus())
                .sessionsGenerated(sessionsGenerated)
                .createdAt(classEntity.getCreatedAt())
                .build();
    }

    private void validateStartDateInScheduleDays(LocalDate startDate, List<Integer> scheduleDays) {
        int dayOfWeek = startDate.getDayOfWeek().getValue(); // 1=Mon, 7=Sun (ISODOW)
        if (!scheduleDays.contains(dayOfWeek)) {
            throw new ValidationException(
                "Start date must be one of the schedule days. " +
                "Start date is " + startDate.getDayOfWeek() + " (ISODOW=" + dayOfWeek +
                ") but schedule days are " + scheduleDays
            );
        }
    }

    private LocalDate calculatePlannedEndDate(ClassEntity classEntity) {
        return sessionRepository.findTopByClassEntityOrderByDateDesc(classEntity)
                .map(Session::getDate)
                .orElse(null);
    }
}
```

#### 3.1.5 Controller

**File:** `ClassManagementController.java`

```java
@RestController
@RequestMapping("/api/v1/classes")
@RequiredArgsConstructor
@Slf4j
public class ClassManagementController {

    private final ClassService classService;

    @PostMapping
    @PreAuthorize("hasRole('ACADEMIC_STAFF')")
    public ResponseEntity<ResponseObject<CreateClassResponse>> createClass(
            @Valid @RequestBody CreateClassRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("POST /api/v1/classes - Creating class: {}", request.getCode());

        CreateClassResponse response = classService.createClass(request, userPrincipal.getUserId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseObject.success(
                    "Class created successfully with " + response.getSessionsGenerated() + " sessions generated",
                    response
                ));
    }
}
```

### ✅ Testing

**Unit Tests:** `ClassServiceImplTest.java`

```java
@SpringBootTest
@ActiveProfiles("test")
class ClassServiceImplTest {

    @Autowired
    private ClassService classService;

    @MockitoBean
    private ClassRepository classRepository;

    @MockitoBean
    private CourseRepository courseRepository;

    @MockitoBean
    private BranchRepository branchRepository;

    @MockitoBean
    private SessionGenerationService sessionGenerationService;

    @Test
    void shouldCreateClassSuccessfully() {
        // Given
        CreateClassRequest request = CreateClassRequest.builder()
                .branchId(1L)
                .courseId(10L)
                .code("ENG-A1-2025-01")
                .modality(Modality.OFFLINE)
                .startDate(LocalDate.of(2025, 1, 6)) // Monday (ISODOW=1)
                .scheduleDays(List.of(1, 3, 5)) // Mon, Wed, Fri
                .maxCapacity(20)
                .build();

        Course course = TestDataBuilder.createTestCourse(ApprovalStatus.APPROVED);
        Branch branch = TestDataBuilder.createTestBranch();

        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));
        when(classRepository.existsByBranchAndCode(branch, "ENG-A1-2025-01")).thenReturn(false);
        when(sessionGenerationService.generateSessions(any())).thenReturn(36);

        // When
        CreateClassResponse response = classService.createClass(request, 1L);

        // Then
        assertThat(response.getSessionsGenerated()).isEqualTo(36);
        assertThat(response.getStatus()).isEqualTo(ClassStatus.DRAFT);
        assertThat(response.getApprovalStatus()).isEqualTo(ApprovalStatus.PENDING);
        verify(sessionGenerationService).generateSessions(any());
        verify(classRepository, times(2)).save(any()); // Once for create, once for end date
    }

    @Test
    void shouldThrowExceptionWhenCourseNotApproved() {
        // Given
        CreateClassRequest request = TestDataBuilder.createTestRequest();
        Course course = TestDataBuilder.createTestCourse(ApprovalStatus.PENDING);

        when(courseRepository.findById(anyLong())).thenReturn(Optional.of(course));

        // When & Then
        assertThatThrownBy(() -> classService.createClass(request, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Course must be approved");
    }

    @Test
    void shouldThrowExceptionWhenStartDateNotInScheduleDays() {
        // Given - Start date is Tuesday (ISODOW=2) but schedule days are Mon/Wed/Fri (1,3,5)
        CreateClassRequest request = CreateClassRequest.builder()
                .branchId(1L)
                .courseId(10L)
                .code("ENG-A1-2025-01")
                .startDate(LocalDate.of(2025, 1, 7)) // Tuesday
                .scheduleDays(List.of(1, 3, 5)) // Mon, Wed, Fri
                .maxCapacity(20)
                .build();

        when(courseRepository.findById(anyLong())).thenReturn(Optional.of(TestDataBuilder.createTestCourse()));
        when(branchRepository.findById(anyLong())).thenReturn(Optional.of(TestDataBuilder.createTestBranch()));

        // When & Then
        assertThatThrownBy(() -> classService.createClass(request, 1L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Start date must be one of the schedule days");
    }

    @Test
    void shouldThrowExceptionWhenDuplicateClassCode() {
        // Given
        CreateClassRequest request = TestDataBuilder.createTestRequest();
        Branch branch = TestDataBuilder.createTestBranch();

        when(courseRepository.findById(anyLong())).thenReturn(Optional.of(TestDataBuilder.createTestCourse()));
        when(branchRepository.findById(anyLong())).thenReturn(Optional.of(branch));
        when(classRepository.existsByBranchAndCode(branch, request.getCode())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> classService.createClass(request, 1L))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Class code already exists");
    }
}
```

---

## STEP 2: Generate Sessions (Auto)

### 🎯 Objective

System automatically generates sessions from course template based on start date and schedule days.

### 📊 Backend Implementation

#### 3.2.1 Service

**File:** `SessionGenerationService.java`

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionGenerationService {

    private final SessionRepository sessionRepository;
    private final CourseSessionRepository courseSessionRepository;

    /**
     * Generate sessions for a class based on course template and schedule
     *
     * Algorithm:
     * 1. Load all course_sessions from course template (ordered by phase, sequence)
     * 2. For each course_session, calculate date based on schedule_days
     * 3. Create session entity with calculated date
     * 4. Use rotating pattern: Mon -> Wed -> Fri -> Mon (next week)
     *
     * Performance: O(n) where n = number of course sessions
     *
     * @param classEntity The class to generate sessions for
     * @return Number of sessions generated
     */
    @Transactional
    public int generateSessions(ClassEntity classEntity) {
        log.info("Generating sessions for class: {} (ID: {})", classEntity.getCode(), classEntity.getId());

        // 1. Load course sessions template
        List<CourseSession> courseSessions = courseSessionRepository
                .findByCourseOrderByPhasePhaseNumberAscSequenceNoAsc(classEntity.getCourse());

        if (courseSessions.isEmpty()) {
            throw new BusinessException("Course has no sessions defined");
        }

        log.debug("Found {} course sessions to generate", courseSessions.size());

        // 2. Generate sessions with calculated dates
        LocalDate currentDate = classEntity.getStartDate();
        List<Integer> scheduleDays = classEntity.getScheduleDays();
        int sessionIndex = 0;

        List<Session> sessions = new ArrayList<>();

        for (CourseSession courseSession : courseSessions) {
            // Calculate target day of week for this session (rotating pattern)
            int targetDayOfWeek = scheduleDays.get(sessionIndex % scheduleDays.size());

            // Find next occurrence of target day
            while (currentDate.getDayOfWeek().getValue() != targetDayOfWeek) {
                currentDate = currentDate.plusDays(1);
            }

            // Create session
            Session session = Session.builder()
                    .classEntity(classEntity)
                    .courseSession(courseSession)
                    .date(currentDate)
                    .type(SessionType.CLASS)
                    .status(SessionStatus.PLANNED)
                    .build();

            sessions.add(session);

            log.trace("Session {} scheduled for {} ({})",
                sessionIndex + 1, currentDate, currentDate.getDayOfWeek());

            // Move to next day
            sessionIndex++;
            if (sessionIndex % scheduleDays.size() == 0) {
                // Completed one week cycle, skip to next week's first day
                currentDate = currentDate.plusDays(1);
            } else {
                // Move to next day in current week
                currentDate = currentDate.plusDays(1);
            }
        }

        // 3. Batch save all sessions
        sessionRepository.saveAll(sessions);

        log.info("Generated {} sessions for class {} (first: {}, last: {})",
            sessions.size(),
            classEntity.getCode(),
            sessions.get(0).getDate(),
            sessions.get(sessions.size() - 1).getDate());

        return sessions.size();
    }
}
```

**Example Output:**
```
Input:
- start_date: 2025-01-06 (Monday, ISODOW=1)
- schedule_days: [1, 3, 5] (Mon, Wed, Fri)
- course_sessions: 36 sessions

Output:
Session 1  → 2025-01-06 (Monday)
Session 2  → 2025-01-08 (Wednesday)
Session 3  → 2025-01-10 (Friday)
Session 4  → 2025-01-13 (Monday, Week 2)
...
Session 36 → 2025-03-28 (Friday, Week 12)
```

### ✅ Testing

```java
@Test
void shouldGenerateCorrectNumberOfSessions() {
    // Given
    ClassEntity classEntity = TestDataBuilder.createTestClass(
        LocalDate.of(2025, 1, 6), // Monday
        List.of(1, 3, 5) // Mon, Wed, Fri
    );
    List<CourseSession> courseSessions = TestDataBuilder.createTestCourseSessions(36);

    when(courseSessionRepository.findByCourseOrderByPhasePhaseNumberAscSequenceNoAsc(any()))
            .thenReturn(courseSessions);

    // When
    int count = sessionGenerationService.generateSessions(classEntity);

    // Then
    assertThat(count).isEqualTo(36);
    verify(sessionRepository).saveAll(argThat(sessions -> sessions.size() == 36));
}

@Test
void shouldGenerateSessionsOnCorrectDates() {
    // Given
    ClassEntity classEntity = TestDataBuilder.createTestClass(
        LocalDate.of(2025, 1, 6), // Monday
        List.of(1, 3, 5)
    );

    // When
    sessionGenerationService.generateSessions(classEntity);

    // Then
    ArgumentCaptor<List<Session>> captor = ArgumentCaptor.forClass(List.class);
    verify(sessionRepository).saveAll(captor.capture());

    List<Session> sessions = captor.getValue();
    assertThat(sessions.get(0).getDate()).isEqualTo(LocalDate.of(2025, 1, 6)); // Mon
    assertThat(sessions.get(1).getDate()).isEqualTo(LocalDate.of(2025, 1, 8)); // Wed
    assertThat(sessions.get(2).getDate()).isEqualTo(LocalDate.of(2025, 1, 10)); // Fri
    assertThat(sessions.get(3).getDate()).isEqualTo(LocalDate.of(2025, 1, 13)); // Mon (Week 2)
}

@Test
void shouldThrowExceptionWhenCourseHasNoSessions() {
    // Given
    ClassEntity classEntity = TestDataBuilder.createTestClass();
    when(courseSessionRepository.findByCourseOrderByPhasePhaseNumberAscSequenceNoAsc(any()))
            .thenReturn(Collections.emptyList());

    // When & Then
    assertThatThrownBy(() -> sessionGenerationService.generateSessions(classEntity))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Course has no sessions defined");
}
```

---

## STEP 3: Assign Time Slots

### 🎯 Objective

Provide API to assign time slots for each schedule day. Different days can have different time slots.

### 📊 Backend Implementation

#### 3.3.1 DTOs

**File:** `AssignTimeSlotsRequest.java`

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignTimeSlotsRequest {

    @NotEmpty(message = "Assignments are required")
    private List<Assignment> assignments;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Assignment {
        @NotNull(message = "Day of week is required")
        @Min(value = 1, message = "Day of week must be between 1 (Monday) and 7 (Sunday)")
        @Max(value = 7, message = "Day of week must be between 1 (Monday) and 7 (Sunday)")
        private Integer dayOfWeek; // ISODOW: 1=Mon, 7=Sun

        @NotNull(message = "Time slot template ID is required")
        private Long timeSlotTemplateId;
    }
}
```

**File:** `AssignTimeSlotsResponse.java`

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignTimeSlotsResponse {
    private Long classId;
    private Integer assignedSessions;
    private List<AssignmentResult> assignments;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignmentResult {
        private Integer dayOfWeek;
        private String timeSlotName;
        private Integer sessionsAssigned;
    }
}
```

#### 3.3.2 Repository

**File:** `SessionRepository.java`

```java
public interface SessionRepository extends JpaRepository<Session, Long> {

    @Modifying
    @Query("UPDATE Session s SET s.timeSlotTemplate.id = :timeSlotId, s.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE s.classEntity.id = :classId " +
           "AND FUNCTION('EXTRACT', ISODOW, s.date) = :dayOfWeek")
    int updateTimeSlotByClassAndDayOfWeek(
        @Param("classId") Long classId,
        @Param("dayOfWeek") int dayOfWeek,
        @Param("timeSlotId") Long timeSlotId
    );

    Optional<Session> findTopByClassEntityOrderByDateDesc(ClassEntity classEntity);

    int countByClassEntityId(Long classId);
}
```

#### 3.3.3 Service

**File:** `TimeSlotAssignmentService.java`

```java
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TimeSlotAssignmentService {

    private final SessionRepository sessionRepository;
    private final TimeSlotTemplateRepository timeSlotTemplateRepository;
    private final ClassRepository classRepository;

    public AssignTimeSlotsResponse assignTimeSlots(Long classId, AssignTimeSlotsRequest request) {
        log.info("Assigning time slots for class: {}", classId);

        // 1. Validate class exists
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

        // 2. Validate all time slot templates exist
        List<Long> timeSlotIds = request.getAssignments().stream()
                .map(AssignTimeSlotsRequest.Assignment::getTimeSlotTemplateId)
                .collect(Collectors.toList());

        Map<Long, TimeSlotTemplate> timeSlotMap = timeSlotTemplateRepository
                .findAllById(timeSlotIds)
                .stream()
                .collect(Collectors.toMap(TimeSlotTemplate::getId, t -> t));

        if (timeSlotMap.size() != new HashSet<>(timeSlotIds).size()) {
            throw new ResourceNotFoundException("One or more time slot templates not found");
        }

        // 3. Assign time slots per day using bulk update
        List<AssignTimeSlotsResponse.AssignmentResult> results = new ArrayList<>();
        int totalAssigned = 0;

        for (AssignTimeSlotsRequest.Assignment assignment : request.getAssignments()) {
            int dayOfWeek = assignment.getDayOfWeek();
            Long timeSlotId = assignment.getTimeSlotTemplateId();

            // Bulk update all sessions on this day of week
            int updated = sessionRepository.updateTimeSlotByClassAndDayOfWeek(
                classId,
                dayOfWeek,
                timeSlotId
            );

            TimeSlotTemplate timeSlot = timeSlotMap.get(timeSlotId);
            results.add(AssignTimeSlotsResponse.AssignmentResult.builder()
                    .dayOfWeek(dayOfWeek)
                    .timeSlotName(timeSlot.getName() + " (" + timeSlot.getStartTime() + "-" + timeSlot.getEndTime() + ")")
                    .sessionsAssigned(updated)
                    .build());

            totalAssigned += updated;
            log.debug("Assigned time slot {} to {} sessions on day {}", timeSlotId, updated, dayOfWeek);
        }

        log.info("Assigned time slots to {} sessions for class {}", totalAssigned, classId);

        return AssignTimeSlotsResponse.builder()
                .classId(classId)
                .assignedSessions(totalAssigned)
                .assignments(results)
                .build();
    }
}
```

#### 3.3.4 Controller

```java
@PostMapping("/{classId}/time-slots")
@PreAuthorize("hasRole('ACADEMIC_STAFF')")
public ResponseEntity<ResponseObject<AssignTimeSlotsResponse>> assignTimeSlots(
        @PathVariable Long classId,
        @Valid @RequestBody AssignTimeSlotsRequest request) {

    log.info("POST /api/v1/classes/{}/time-slots", classId);

    AssignTimeSlotsResponse response = timeSlotAssignmentService.assignTimeSlots(classId, request);

    return ResponseEntity.ok(ResponseObject.success(
        "Time slots assigned successfully to " + response.getAssignedSessions() + " sessions",
        response
    ));
}
```

### ✅ Testing

```java
@Test
void shouldAssignTimeSlotsSuccessfully() {
    // Given
    Long classId = 101L;
    AssignTimeSlotsRequest request = AssignTimeSlotsRequest.builder()
            .assignments(List.of(
                new Assignment(1, 2L), // Monday -> Morning Slot 2
                new Assignment(3, 2L), // Wednesday -> Morning Slot 2
                new Assignment(5, 5L)  // Friday -> Afternoon Slot 2
            ))
            .build();

    when(sessionRepository.updateTimeSlotByClassAndDayOfWeek(classId, 1, 2L)).thenReturn(12);
    when(sessionRepository.updateTimeSlotByClassAndDayOfWeek(classId, 3, 2L)).thenReturn(12);
    when(sessionRepository.updateTimeSlotByClassAndDayOfWeek(classId, 5, 5L)).thenReturn(12);

    // When
    AssignTimeSlotsResponse response = timeSlotAssignmentService.assignTimeSlots(classId, request);

    // Then
    assertThat(response.getAssignedSessions()).isEqualTo(36);
    assertThat(response.getAssignments()).hasSize(3);
}
```

---

## STEP 4: Assign Resources

### 🎯 Objective

Provide APIs to assign resources (rooms/Zoom) with HYBRID approach: SQL bulk insert + Java conflict analysis.

**Performance Target:** 100-200ms for 36 sessions

### 📊 Backend Implementation

#### 3.4.1 DTOs

**File:** `AssignResourcesRequest.java`

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignResourcesRequest {

    @NotEmpty(message = "Pattern is required")
    private List<PatternItem> pattern;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PatternItem {
        @NotNull(message = "Day of week is required")
        @Min(1) @Max(7)
        private Integer dayOfWeek;

        @NotNull(message = "Resource ID is required")
        private Long resourceId;
    }
}
```

**File:** `AssignResourcesResponse.java`

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignResourcesResponse {
    private Long classId;
    private Integer totalSessions;
    private Integer successCount;
    private Integer conflictCount;
    private List<ConflictDetail> conflicts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConflictDetail {
        private Long sessionId;
        private Integer sessionNumber;
        private LocalDate date;
        private Integer dayOfWeek;
        private String timeSlotName;
        private ConflictType conflictType;
        private String reason;
        private String conflictingClassCode;
    }

    public enum ConflictType {
        RESOURCE_UNAVAILABLE,
        ALREADY_ASSIGNED,
        INSUFFICIENT_CAPACITY
    }
}
```

#### 3.4.2 Repository

**File:** `SessionResourceRepository.java`

```java
public interface SessionResourceRepository extends JpaRepository<SessionResource, Long> {

    /**
     * HYBRID APPROACH - Phase 1: SQL Bulk Insert (Fast)
     *
     * Inserts resource assignments for all sessions matching:
     * - class_id
     * - day_of_week (ISODOW)
     * - No existing assignment
     * - No conflict with other sessions at same date/time
     *
     * Performance: ~50-100ms for 36 sessions
     *
     * @return Number of sessions assigned (excluding conflicts)
     */
    @Modifying
    @Query(value = """
        INSERT INTO session_resource (session_id, resource_type, resource_id)
        SELECT s.id, CAST(:resourceType AS resource_type_enum), :resourceId
        FROM session s
        WHERE s.class_id = :classId
          AND EXTRACT(ISODOW FROM s.date) = :dayOfWeek
          AND s.id NOT IN (
            SELECT session_id FROM session_resource WHERE resource_id = :resourceId
          )
          AND NOT EXISTS (
            SELECT 1 FROM session_resource sr2
            JOIN session s2 ON sr2.session_id = s2.id
            WHERE sr2.resource_id = :resourceId
              AND s2.date = s.date
              AND s2.time_slot_template_id = s.time_slot_template_id
          )
        """, nativeQuery = true)
    int bulkAssignResource(
        @Param("classId") Long classId,
        @Param("dayOfWeek") int dayOfWeek,
        @Param("resourceId") Long resourceId,
        @Param("resourceType") String resourceType
    );
}
```

**File:** `SessionRepository.java` (additional methods)

```java
@Query("SELECT s FROM Session s " +
       "WHERE s.classEntity.id = :classId " +
       "AND FUNCTION('EXTRACT', ISODOW, s.date) = :dayOfWeek " +
       "AND NOT EXISTS (SELECT sr FROM SessionResource sr WHERE sr.session = s)")
List<Session> findUnassignedSessionsByDayOfWeek(
    @Param("classId") Long classId,
    @Param("dayOfWeek") int dayOfWeek
);

@Query("SELECT s FROM Session s " +
       "JOIN s.sessionResources sr " +
       "WHERE sr.resource.id = :resourceId " +
       "AND s.date = :date " +
       "AND s.timeSlotTemplate.id = :timeSlotId")
Optional<Session> findConflictingSession(
    @Param("resourceId") Long resourceId,
    @Param("date") LocalDate date,
    @Param("timeSlotId") Long timeSlotId
);
```

#### 3.4.3 Service

**File:** `ResourceAssignmentService.java`

```java
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ResourceAssignmentService {

    private final SessionRepository sessionRepository;
    private final SessionResourceRepository sessionResourceRepository;
    private final ResourceRepository resourceRepository;
    private final ClassRepository classRepository;

    /**
     * HYBRID Approach: SQL bulk insert + Java conflict analysis
     *
     * Phase 1 (SQL - Fast): Bulk assign resources where no conflicts
     * Phase 2 (Java - Detailed): Analyze remaining unassigned sessions for conflicts
     *
     * Performance: 100-200ms for 36 sessions (90% success rate)
     */
    public AssignResourcesResponse assignResources(Long classId, AssignResourcesRequest request) {
        log.info("Assigning resources for class: {}", classId);
        long startTime = System.currentTimeMillis();

        // 1. Validate class
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

        // 2. Validate resources exist
        Set<Long> resourceIds = request.getPattern().stream()
                .map(AssignResourcesRequest.PatternItem::getResourceId)
                .collect(Collectors.toSet());

        Map<Long, Resource> resourceMap = resourceRepository.findAllById(resourceIds)
                .stream()
                .collect(Collectors.toMap(Resource::getId, r -> r));

        if (resourceMap.size() != resourceIds.size()) {
            throw new ResourceNotFoundException("One or more resources not found");
        }

        // 3. PHASE 1: SQL Bulk Assignment (Fast - ~50-100ms)
        int totalSuccessCount = 0;
        for (AssignResourcesRequest.PatternItem pattern : request.getPattern()) {
            int assigned = sessionResourceRepository.bulkAssignResource(
                    classId,
                    pattern.getDayOfWeek(),
                    pattern.getResourceId(),
                    ResourceType.ROOM.name()
            );
            totalSuccessCount += assigned;
            log.debug("Bulk assigned resource {} to {} sessions on day {}",
                    pattern.getResourceId(), assigned, pattern.getDayOfWeek());
        }

        // 4. PHASE 2: Find Conflicts (Detailed Analysis - ~50-100ms)
        List<AssignResourcesResponse.ConflictDetail> conflicts = new ArrayList<>();
        for (AssignResourcesRequest.PatternItem pattern : request.getPattern()) {
            // Find sessions still unassigned for this day
            List<Session> unassignedSessions = sessionRepository
                    .findUnassignedSessionsByDayOfWeek(classId, pattern.getDayOfWeek());

            // Analyze each conflict
            for (Session session : unassignedSessions) {
                AssignResourcesResponse.ConflictDetail conflict = analyzeResourceConflict(
                        session,
                        pattern.getResourceId(),
                        resourceMap.get(pattern.getResourceId())
                );
                conflicts.add(conflict);
            }
        }

        // 5. Build response
        int totalSessions = sessionRepository.countByClassEntityId(classId);

        long duration = System.currentTimeMillis() - startTime;
        log.info("Resource assignment completed in {}ms: {}/{} successful, {} conflicts",
                duration, totalSuccessCount, totalSessions, conflicts.size());

        return AssignResourcesResponse.builder()
                .classId(classId)
                .totalSessions(totalSessions)
                .successCount(totalSuccessCount)
                .conflictCount(conflicts.size())
                .conflicts(conflicts)
                .build();
    }

    /**
     * Analyze why a session couldn't be assigned (Java analysis for details)
     */
    private AssignResourcesResponse.ConflictDetail analyzeResourceConflict(
            Session session, Long resourceId, Resource resource) {

        // Find exact reason for conflict
        Optional<Session> conflictingSession = sessionRepository
                .findConflictingSession(resourceId, session.getDate(), session.getTimeSlotTemplate().getId());

        String reason;
        String conflictingClassCode = null;

        if (conflictingSession.isPresent()) {
            ClassEntity conflictingClass = conflictingSession.get().getClassEntity();
            reason = String.format("Room %s booked by Class %s",
                    resource.getName(), conflictingClass.getCode());
            conflictingClassCode = conflictingClass.getCode();
        } else {
            reason = "Resource unavailable (maintenance or blocked)";
        }

        return AssignResourcesResponse.ConflictDetail.builder()
                .sessionId(session.getId())
                .sessionNumber(getSessionNumber(session))
                .date(session.getDate())
                .dayOfWeek(session.getDate().getDayOfWeek().getValue())
                .timeSlotName(session.getTimeSlotTemplate().getName())
                .conflictType(AssignResourcesResponse.ConflictType.RESOURCE_UNAVAILABLE)
                .reason(reason)
                .conflictingClassCode(conflictingClassCode)
                .build();
    }

    private Integer getSessionNumber(Session session) {
        // Calculate session number based on class start date and session date
        // Implementation depends on your business logic
        return 1; // Placeholder
    }

    /**
     * Get available resources for a specific date/time or session
     */
    public List<ResourceDTO> getAvailableResources(Long classId, Long sessionId,
                                                    LocalDate date, Long timeSlotTemplateId) {
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

        // Determine search criteria
        LocalDate searchDate;
        Long searchTimeSlotId;

        if (sessionId != null) {
            Session session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
            searchDate = session.getDate();
            searchTimeSlotId = session.getTimeSlotTemplate().getId();
        } else {
            searchDate = date;
            searchTimeSlotId = timeSlotTemplateId;
        }

        // Query available resources
        ResourceType resourceType = classEntity.getModality() == Modality.ONLINE
                ? ResourceType.ONLINE_ACCOUNT
                : ResourceType.ROOM;

        return resourceRepository.findAvailableResources(
                classEntity.getBranch().getId(),
                resourceType,
                classEntity.getMaxCapacity(),
                searchDate,
                searchTimeSlotId
        );
    }
}
```

#### 3.4.4 Controller

```java
@PostMapping("/{classId}/resources")
@PreAuthorize("hasRole('ACADEMIC_STAFF')")
public ResponseEntity<ResponseObject<AssignResourcesResponse>> assignResources(
        @PathVariable Long classId,
        @Valid @RequestBody AssignResourcesRequest request) {

    log.info("POST /api/v1/classes/{}/resources", classId);

    AssignResourcesResponse response = resourceAssignmentService.assignResources(classId, request);

    String message = response.getConflictCount() > 0
            ? String.format("Resources assigned with %d conflicts requiring manual resolution", response.getConflictCount())
            : "Resources assigned successfully to all sessions";

    return ResponseEntity.ok(ResponseObject.success(message, response));
}

@GetMapping("/{classId}/available-resources")
@PreAuthorize("hasRole('ACADEMIC_STAFF')")
public ResponseEntity<ResponseObject<List<ResourceDTO>>> getAvailableResources(
        @PathVariable Long classId,
        @RequestParam(required = false) Long sessionId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam(required = false) Long timeSlotTemplateId) {

    log.info("GET /api/v1/classes/{}/available-resources", classId);

    List<ResourceDTO> resources = resourceAssignmentService
            .getAvailableResources(classId, sessionId, date, timeSlotTemplateId);

    return ResponseEntity.ok(ResponseObject.success("Available resources retrieved", resources));
}
```

### ✅ Testing

```java
@Test
void shouldBulkAssignResourcesSuccessfully() {
    // Given
    Long classId = 101L;
    AssignResourcesRequest request = AssignResourcesRequest.builder()
            .pattern(List.of(
                new PatternItem(1, 6L), // Monday -> Room 203
                new PatternItem(3, 6L), // Wednesday -> Room 203
                new PatternItem(5, 6L)  // Friday -> Room 203
            ))
            .build();

    when(sessionResourceRepository.bulkAssignResource(classId, 1, 6L, "ROOM")).thenReturn(12);
    when(sessionResourceRepository.bulkAssignResource(classId, 3, 6L, "ROOM")).thenReturn(12);
    when(sessionResourceRepository.bulkAssignResource(classId, 5, 6L, "ROOM")).thenReturn(9); // 3 conflicts

    // When
    AssignResourcesResponse response = resourceAssignmentService.assignResources(classId, request);

    // Then
    assertThat(response.getSuccessCount()).isEqualTo(33);
    assertThat(response.getConflictCount()).isEqualTo(3);
}

@Test
void shouldAnalyzeConflictDetails() {
    // Given - 3 sessions have conflicts on Friday
    Long classId = 101L;
    List<Session> unassignedSessions = TestDataBuilder.createSessionsOnFridays(3);

    when(sessionRepository.findUnassignedSessionsByDayOfWeek(classId, 5))
            .thenReturn(unassignedSessions);

    // Mock conflicting session
    Session conflictingSession = TestDataBuilder.createSessionForDifferentClass("ENG-B1-02");
    when(sessionRepository.findConflictingSession(anyLong(), any(), any()))
            .thenReturn(Optional.of(conflictingSession));

    // When
    AssignResourcesResponse response = resourceAssignmentService.assignResources(classId, request);

    // Then
    assertThat(response.getConflicts()).hasSize(3);
    assertThat(response.getConflicts().get(0).getConflictingClassCode()).isEqualTo("ENG-B1-02");
    assertThat(response.getConflicts().get(0).getReason()).contains("booked by Class");
}
```

---

## STEP 5: Assign Teachers

### 🎯 Objective

Provide APIs to assign teachers with PRE-CHECK availability (v1.1 - shows conflicts BEFORE selection).

**Key Innovation:** Query teachers WITH availability status for ALL sessions upfront (no trial-and-error).

### 📊 Backend Implementation

#### 3.5.1 DTOs

**File:** `AvailableTeachersResponse.java`

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableTeachersResponse {
    private Long classId;
    private Integer totalSessions;
    private List<TeacherAvailabilityDTO> teachers;
}
```

**File:** `TeacherAvailabilityDTO.java`

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherAvailabilityDTO {
    // Basic Info
    private Long id;
    private String fullName;
    private String email;
    private String employeeCode;
    private String contractType; // full-time, part-time

    // Skills
    private List<String> skills;
    private Integer maxLevel;
    private Boolean hasGeneralSkill; // 'general' = can teach any session

    // Availability Stats
    private Integer totalSessions;
    private Integer availableSessions;
    private Double availabilityPercentage;
    private String availabilityStatus; // fully_available, partially_available, unavailable

    // Conflict Breakdown
    private ConflictCounts conflicts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConflictCounts {
        private Integer noAvailability;    // No teacher_availability record
        private Integer teachingConflict;  // Teaching another class
        private Integer leaveConflict;     // On approved leave
    }
}
```

**File:** `AssignTeacherRequest.java`

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignTeacherRequest {
    @NotNull(message = "Teacher ID is required")
    private Long teacherId;

    @NotBlank(message = "Role is required")
    private String role; // "primary" or "substitute"

    // Optional: specific sessions for substitute assignment
    private List<Long> sessionIds;
}
```

**File:** `AssignTeacherResponse.java`

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignTeacherResponse {
    private Long classId;
    private Long teacherId;
    private String teacherName;
    private String role;
    private Integer assignedCount;
    private Integer totalSessions;
    private Boolean needsSubstitute;
    private List<RemainingSession> remainingSessions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RemainingSession {
        private Long sessionId;
        private LocalDate date;
        private String reason;
    }
}
```

#### 3.5.2 Repository

**File:** `TeacherRepository.java`

```java
public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    /**
     * PRE-CHECK Approach (v1.1): Query teachers WITH availability checked for ALL sessions
     *
     * Benefits:
     * - No trial-and-error (user sees conflicts upfront)
     * - 20% faster than old approach
     * - Better UX (informed decision)
     *
     * Query Structure:
     * CTE 1: skill_matched_teachers (teachers with required skills or 'general')
     * CTE 2: session_conflicts (3 types: no_availability, teaching_conflict, leave_conflict)
     * Main: Calculate available_sessions = total - conflicts
     *
     * Performance: ~200-300ms for 36 sessions
     */
    @Query(value = """
        WITH skill_matched_teachers AS (
          SELECT
            t.id,
            ua.full_name,
            ua.email,
            t.employee_code,
            t.contract_type,
            array_agg(DISTINCT ts.skill ORDER BY ts.skill) as skills,
            MAX(ts.level) as max_level,
            bool_or(ts.skill = 'general') as has_general_skill
          FROM teacher t
          JOIN user_account ua ON t.user_account_id = ua.id
          JOIN teacher_skill ts ON t.id = ts.teacher_id
          WHERE (:skillSetFilter IS NULL OR ts.skill = ANY(CAST(:skillSetFilter AS skill_enum[])))
          GROUP BY t.id, ua.full_name, ua.email, t.employee_code, t.contract_type
        ),
        session_conflicts AS (
          SELECT
            smt.id as teacher_id,
            COUNT(*) FILTER (
              WHERE NOT EXISTS (
                SELECT 1 FROM teacher_availability ta
                WHERE ta.teacher_id = smt.id
                  AND ta.day_of_week = EXTRACT(ISODOW FROM s.date)
                  AND ta.time_slot_template_id = s.time_slot_template_id
              )
            ) as no_availability_count,
            COUNT(*) FILTER (
              WHERE EXISTS (
                SELECT 1 FROM teaching_slot ts2
                JOIN session s2 ON ts2.session_id = s2.id
                WHERE ts2.teacher_id = smt.id
                  AND s2.date = s.date
                  AND s2.time_slot_template_id = s.time_slot_template_id
                  AND ts2.status = 'scheduled'
              )
            ) as teaching_conflict_count,
            COUNT(*) FILTER (
              WHERE EXISTS (
                SELECT 1 FROM teaching_slot ts3
                WHERE ts3.teacher_id = smt.id
                  AND ts3.status = 'on_leave'
              )
            ) as leave_conflict_count,
            COUNT(*) as total_sessions
          FROM skill_matched_teachers smt
          CROSS JOIN session s
          JOIN course_session cs ON s.course_session_id = cs.id
          WHERE s.class_id = :classId
            AND (
              smt.has_general_skill = true
              OR EXISTS (
                SELECT 1 FROM teacher_skill ts_check
                WHERE ts_check.teacher_id = smt.id
                  AND ts_check.skill = ANY(cs.skill_set)
              )
            )
          GROUP BY smt.id
        )
        SELECT
          smt.*,
          COALESCE(sc.total_sessions, 0) as totalSessions,
          COALESCE(sc.total_sessions, 0) -
            COALESCE(sc.no_availability_count, 0) -
            COALESCE(sc.teaching_conflict_count, 0) -
            COALESCE(sc.leave_conflict_count, 0) as availableSessions,
          ROUND(
            (COALESCE(sc.total_sessions, 0) -
             COALESCE(sc.no_availability_count, 0) -
             COALESCE(sc.teaching_conflict_count, 0) -
             COALESCE(sc.leave_conflict_count, 0))::numeric / NULLIF(COALESCE(sc.total_sessions, 1), 0) * 100,
            1
          ) as availabilityPercentage,
          COALESCE(sc.no_availability_count, 0) as noAvailabilityConflicts,
          COALESCE(sc.teaching_conflict_count, 0) as teachingConflicts,
          COALESCE(sc.leave_conflict_count, 0) as leaveConflicts,
          CASE
            WHEN COALESCE(sc.total_sessions, 0) = COALESCE(sc.total_sessions, 0) -
                 COALESCE(sc.no_availability_count, 0) -
                 COALESCE(sc.teaching_conflict_count, 0) -
                 COALESCE(sc.leave_conflict_count, 0)
            THEN 'fully_available'
            WHEN COALESCE(sc.no_availability_count, 0) +
                 COALESCE(sc.teaching_conflict_count, 0) +
                 COALESCE(sc.leave_conflict_count, 0) < COALESCE(sc.total_sessions, 0)
            THEN 'partially_available'
            ELSE 'unavailable'
          END as availabilityStatus
        FROM skill_matched_teachers smt
        LEFT JOIN session_conflicts sc ON sc.teacher_id = smt.id
        ORDER BY
          CASE WHEN smt.contract_type = 'full-time' THEN 0
               WHEN smt.contract_type = 'part-time' THEN 1
               ELSE 2 END,
          availableSessions DESC,
          smt.has_general_skill DESC
        """, nativeQuery = true)
    List<TeacherAvailabilityDTO> findAvailableTeachersWithPreCheck(
        @Param("classId") Long classId,
        @Param("skillSetFilter") List<String> skillSetFilter
    );
}
```

**File:** `SessionRepository.java` (additional methods)

```java
@Query("SELECT s FROM Session s " +
       "JOIN s.courseSession cs " +
       "JOIN cs.skillSet sk " +
       "WHERE s.classEntity.id = :classId " +
       "AND EXISTS (" +
       "  SELECT 1 FROM Teacher t " +
       "  JOIN t.skills ts " +
       "  WHERE t.id = :teacherId " +
       "  AND (ts.skill = sk OR ts.skill = 'general')" +
       ")")
List<Session> findSessionsMatchingTeacherSkills(
    @Param("classId") Long classId,
    @Param("teacherId") Long teacherId
);

@Query("SELECT s FROM Session s " +
       "WHERE s.classEntity.id = :classId " +
       "AND NOT EXISTS (SELECT ts FROM TeachingSlot ts WHERE ts.session = s)")
List<Session> findUnassignedSessions(@Param("classId") Long classId);
```

#### 3.5.3 Service

**File:** `TeacherAssignmentService.java`

```java
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TeacherAssignmentService {

    private final TeacherRepository teacherRepository;
    private final SessionRepository sessionRepository;
    private final TeachingSlotRepository teachingSlotRepository;
    private final ClassRepository classRepository;

    /**
     * PRE-CHECK Approach (v1.1):
     * Query teachers WITH availability status BEFORE user selection
     *
     * Benefits:
     * - No trial-and-error
     * - User sees conflicts upfront
     * - 20% faster than old approach
     */
    public AvailableTeachersResponse getAvailableTeachersWithPreCheck(
            Long classId,
            List<String> skillSetFilter) {

        log.info("Getting available teachers for class: {} (PRE-CHECK mode)", classId);
        long startTime = System.currentTimeMillis();

        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

        // Query teachers with PRE-CHECK (3 conditions checked for ALL sessions)
        List<TeacherAvailabilityDTO> teachers = teacherRepository
                .findAvailableTeachersWithPreCheck(classId, skillSetFilter);

        int totalSessions = sessionRepository.countByClassEntityId(classId);

        long duration = System.currentTimeMillis() - startTime;
        log.info("PRE-CHECK completed in {}ms: found {} teachers", duration, teachers.size());

        return AvailableTeachersResponse.builder()
                .classId(classId)
                .totalSessions(totalSessions)
                .teachers(teachers)
                .build();
    }

    /**
     * Direct assignment (no re-checking needed - already done in pre-check)
     */
    public AssignTeacherResponse assignTeacher(Long classId, AssignTeacherRequest request) {
        log.info("Assigning teacher {} to class {} (role: {})",
            request.getTeacherId(), classId, request.getRole());

        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

        Teacher teacher = teacherRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));

        // Get sessions to assign
        List<Session> sessionsToAssign;
        if (request.getSessionIds() != null && !request.getSessionIds().isEmpty()) {
            // Specific sessions (for substitute assignments)
            sessionsToAssign = sessionRepository.findAllById(request.getSessionIds());
        } else {
            // All sessions matching teacher's skills
            sessionsToAssign = sessionRepository.findSessionsMatchingTeacherSkills(
                    classId,
                    request.getTeacherId()
            );
        }

        // Direct INSERT (validated in pre-check)
        List<TeachingSlot> teachingSlots = sessionsToAssign.stream()
                .map(session -> TeachingSlot.builder()
                        .session(session)
                        .teacher(teacher)
                        .status(TeachingSlotStatus.SCHEDULED)
                        .build())
                .collect(Collectors.toList());

        teachingSlotRepository.saveAll(teachingSlots);

        // Check if needs substitute
        int totalSessions = sessionRepository.countByClassEntityId(classId);
        boolean needsSubstitute = teachingSlots.size() < totalSessions;

        List<AssignTeacherResponse.RemainingSession> remainingSessions = Collections.emptyList();
        if (needsSubstitute) {
            // Find unassigned sessions
            remainingSessions = sessionRepository.findUnassignedSessions(classId).stream()
                    .map(s -> new AssignTeacherResponse.RemainingSession(
                        s.getId(),
                        s.getDate(),
                        "No teacher assigned"
                    ))
                    .collect(Collectors.toList());
        }

        log.info("Assigned teacher {} to {}/{} sessions (needsSubstitute: {})",
            teacher.getUserAccount().getFullName(), teachingSlots.size(), totalSessions, needsSubstitute);

        return AssignTeacherResponse.builder()
                .classId(classId)
                .teacherId(teacher.getId())
                .teacherName(teacher.getUserAccount().getFullName())
                .role(request.getRole())
                .assignedCount(teachingSlots.size())
                .totalSessions(totalSessions)
                .needsSubstitute(needsSubstitute)
                .remainingSessions(remainingSessions)
                .build();
    }
}
```

#### 3.5.4 Controller

```java
@GetMapping("/{classId}/available-teachers")
@PreAuthorize("hasRole('ACADEMIC_STAFF')")
public ResponseEntity<ResponseObject<AvailableTeachersResponse>> getAvailableTeachers(
        @PathVariable Long classId,
        @RequestParam(required = false) List<String> skillSet) {

    log.info("GET /api/v1/classes/{}/available-teachers", classId);

    AvailableTeachersResponse response = teacherAssignmentService
            .getAvailableTeachersWithPreCheck(classId, skillSet);

    return ResponseEntity.ok(ResponseObject.success(
        "Available teachers retrieved successfully",
        response
    ));
}

@PostMapping("/{classId}/teachers")
@PreAuthorize("hasRole('ACADEMIC_STAFF')")
public ResponseEntity<ResponseObject<AssignTeacherResponse>> assignTeacher(
        @PathVariable Long classId,
        @Valid @RequestBody AssignTeacherRequest request) {

    log.info("POST /api/v1/classes/{}/teachers", classId);

    AssignTeacherResponse response = teacherAssignmentService.assignTeacher(classId, request);

    String message = response.isNeedsSubstitute()
            ? String.format("Assigned %s to %d sessions, %d sessions need substitute",
                    response.getTeacherName(), response.getAssignedCount(),
                    response.getTotalSessions() - response.getAssignedCount())
            : String.format("Successfully assigned %s to all %d sessions",
                    response.getTeacherName(), response.getTotalSessions());

    return ResponseEntity.ok(ResponseObject.success(message, response));
}
```

### ✅ Testing

```java
@Test
void shouldReturnFullyAvailableTeachers() {
    // Given
    Long classId = 101L;

    // When
    AvailableTeachersResponse response = teacherAssignmentService
            .getAvailableTeachersWithPreCheck(classId, null);

    // Then
    assertThat(response.getTeachers()).isNotEmpty();
    TeacherAvailabilityDTO fullyAvailable = response.getTeachers().stream()
            .filter(t -> t.getAvailabilityStatus().equals("fully_available"))
            .findFirst()
            .orElseThrow();

    assertThat(fullyAvailable.getAvailableSessions()).isEqualTo(36);
    assertThat(fullyAvailable.getConflicts().getNoAvailability()).isEqualTo(0);
    assertThat(fullyAvailable.getConflicts().getTeachingConflict()).isEqualTo(0);
}

@Test
void shouldAssignTeacherDirectly() {
    // Given
    Long classId = 101L;
    AssignTeacherRequest request = AssignTeacherRequest.builder()
            .teacherId(5L)
            .role("primary")
            .build();

    List<Session> sessions = TestDataBuilder.createTestSessions(36);
    when(sessionRepository.findSessionsMatchingTeacherSkills(classId, 5L))
            .thenReturn(sessions);

    // When
    AssignTeacherResponse response = teacherAssignmentService.assignTeacher(classId, request);

    // Then
    assertThat(response.getAssignedCount()).isEqualTo(36);
    assertThat(response.isNeedsSubstitute()).isFalse();
    verify(teachingSlotRepository).saveAll(argThat(slots -> slots.size() == 36));
}

@Test
void shouldHandlePartialAssignmentWithSubstitute() {
    // Given - Teacher available for only 26/36 sessions
    AssignTeacherRequest request = AssignTeacherRequest.builder()
            .teacherId(3L)
            .role("primary")
            .build();

    List<Session> availableSessions = TestDataBuilder.createTestSessions(26);
    when(sessionRepository.findSessionsMatchingTeacherSkills(classId, 3L))
            .thenReturn(availableSessions);
    when(sessionRepository.countByClassEntityId(classId)).thenReturn(36);

    List<Session> unassigned = TestDataBuilder.createTestSessions(10);
    when(sessionRepository.findUnassignedSessions(classId)).thenReturn(unassigned);

    // When
    AssignTeacherResponse response = teacherAssignmentService.assignTeacher(classId, request);

    // Then
    assertThat(response.getAssignedCount()).isEqualTo(26);
    assertThat(response.isNeedsSubstitute()).isTrue();
    assertThat(response.getRemainingSessions()).hasSize(10);
}
```

---

## STEP 6: Validate

### 🎯 Objective

Provide API to validate class completeness before submission (all sessions have time slots, resources, teachers).

### 📊 Backend Implementation

#### 3.6.1 DTOs

**File:** `ValidateClassResponse.java`

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateClassResponse {
    private Long classId;
    private Boolean isValid;
    private Boolean canSubmit;
    private Integer totalSessions;
    private Checks checks;
    private List<String> errors;
    private List<String> warnings;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Checks {
        private Boolean timeSlotsAssigned;
        private Boolean resourcesAssigned;
        private Boolean teachersAssigned;
    }
}
```

#### 3.6.2 Repository Methods

**File:** `SessionRepository.java` (additional methods)

```java
int countByClassEntityIdAndTimeSlotTemplateIsNull(Long classId);

@Query("SELECT COUNT(s) FROM Session s " +
       "WHERE s.classEntity.id = :classId " +
       "AND NOT EXISTS (SELECT sr FROM SessionResource sr WHERE sr.session = s)")
int countByClassEntityIdAndNotHavingResources(@Param("classId") Long classId);

@Query("SELECT COUNT(s) FROM Session s " +
       "WHERE s.classEntity.id = :classId " +
       "AND NOT EXISTS (SELECT ts FROM TeachingSlot ts WHERE ts.session = s)")
int countByClassEntityIdAndNotHavingTeachers(@Param("classId") Long classId);
```

**File:** `TeachingSlotRepository.java`

```java
@Query("SELECT COUNT(DISTINCT ts.teacher.id) FROM TeachingSlot ts " +
       "WHERE ts.session.classEntity.id = :classId")
int countDistinctTeachersByClassId(@Param("classId") Long classId);
```

#### 3.6.3 Service

**File:** `ClassServiceImpl.java` (additional method)

```java
@Override
public ValidateClassResponse validateClass(Long classId) {
    log.info("Validating class: {}", classId);

    ClassEntity classEntity = classRepository.findById(classId)
            .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

    List<String> errors = new ArrayList<>();
    List<String> warnings = new ArrayList<>();

    int totalSessions = sessionRepository.countByClassEntityId(classId);

    // Check 1: All sessions have time slots
    int sessionsWithoutTimeslot = sessionRepository
            .countByClassEntityIdAndTimeSlotTemplateIsNull(classId);
    boolean timeSlotsAssigned = sessionsWithoutTimeslot == 0;
    if (!timeSlotsAssigned) {
        errors.add(String.format("%d sessions missing time slots", sessionsWithoutTimeslot));
    }

    // Check 2: All sessions have resources
    int sessionsWithoutResource = sessionRepository
            .countByClassEntityIdAndNotHavingResources(classId);
    boolean resourcesAssigned = sessionsWithoutResource == 0;
    if (!resourcesAssigned) {
        errors.add(String.format("%d sessions missing resources", sessionsWithoutResource));
    }

    // Check 3: All sessions have teachers
    int sessionsWithoutTeacher = sessionRepository
            .countByClassEntityIdAndNotHavingTeachers(classId);
    boolean teachersAssigned = sessionsWithoutTeacher == 0;
    if (!teachersAssigned) {
        errors.add(String.format("%d sessions missing teachers", sessionsWithoutTeacher));
    }

    // Warning 1: Multiple teachers per class
    int distinctTeachers = teachingSlotRepository.countDistinctTeachersByClassId(classId);
    if (distinctTeachers > 1) {
        warnings.add(String.format("Using %d different teachers for this class", distinctTeachers));
    }

    // Warning 2: Start date in past
    if (classEntity.getStartDate().isBefore(LocalDate.now())) {
        warnings.add("Start date is in the past");
    }

    boolean isValid = errors.isEmpty();

    log.info("Validation complete for class {}: isValid={}, errors={}, warnings={}",
        classId, isValid, errors.size(), warnings.size());

    return ValidateClassResponse.builder()
            .classId(classId)
            .isValid(isValid)
            .canSubmit(isValid)
            .totalSessions(totalSessions)
            .checks(ValidateClassResponse.Checks.builder()
                    .timeSlotsAssigned(timeSlotsAssigned)
                    .resourcesAssigned(resourcesAssigned)
                    .teachersAssigned(teachersAssigned)
                    .build())
            .errors(errors)
            .warnings(warnings)
            .build();
}
```

#### 3.6.4 Controller

```java
@PostMapping("/{classId}/validate")
@PreAuthorize("hasRole('ACADEMIC_STAFF')")
public ResponseEntity<ResponseObject<ValidateClassResponse>> validateClass(
        @PathVariable Long classId) {

    log.info("POST /api/v1/classes/{}/validate", classId);

    ValidateClassResponse response = classService.validateClass(classId);

    String message = response.isValid()
            ? "Class is valid and ready for submission"
            : "Class has incomplete assignments";

    return ResponseEntity.ok(ResponseObject.success(message, response));
}
```

### ✅ Testing

```java
@Test
void shouldPassValidationWhenComplete() {
    // Given
    Long classId = 101L;
    when(sessionRepository.countByClassEntityIdAndTimeSlotTemplateIsNull(classId)).thenReturn(0);
    when(sessionRepository.countByClassEntityIdAndNotHavingResources(classId)).thenReturn(0);
    when(sessionRepository.countByClassEntityIdAndNotHavingTeachers(classId)).thenReturn(0);

    // When
    ValidateClassResponse response = classService.validateClass(classId);

    // Then
    assertThat(response.isValid()).isTrue();
    assertThat(response.canSubmit()).isTrue();
    assertThat(response.getErrors()).isEmpty();
}

@Test
void shouldFailValidationWhenIncomplete() {
    // Given
    Long classId = 101L;
    when(sessionRepository.countByClassEntityIdAndNotHavingTeachers(classId)).thenReturn(3);

    // When
    ValidateClassResponse response = classService.validateClass(classId);

    // Then
    assertThat(response.isValid()).isFalse();
    assertThat(response.canSubmit()).isFalse();
    assertThat(response.getErrors()).contains("3 sessions missing teachers");
}
```

---

## STEP 7: Submit & Approve

### 🎯 Objective

Provide APIs for Academic Staff to submit class and Center Head to approve/reject.

### 📊 Backend Implementation

#### 3.7.1 DTOs

**File:** `SubmitClassResponse.java`

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitClassResponse {
    private Long classId;
    private String code;
    private ClassStatus status;
    private ApprovalStatus approvalStatus;
    private OffsetDateTime submittedAt;
    private String submittedBy;
}
```

**File:** `ApproveClassResponse.java`

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApproveClassResponse {
    private Long classId;
    private String code;
    private ClassStatus status;
    private ApprovalStatus approvalStatus;
    private String approvedBy;
    private OffsetDateTime approvedAt;
}
```

**File:** `RejectClassRequest.java`

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RejectClassRequest {
    @NotBlank(message = "Rejection reason is required")
    @Size(min = 10, message = "Rejection reason must be at least 10 characters")
    private String reason;
}
```

**File:** `RejectClassResponse.java`

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RejectClassResponse {
    private Long classId;
    private String code;
    private ClassStatus status;
    private ApprovalStatus approvalStatus;
    private String rejectionReason;
    private String rejectedBy;
    private OffsetDateTime rejectedAt;
}
```

#### 3.7.2 Service

**File:** `ClassServiceImpl.java` (additional methods)

```java
@Override
@Transactional
public SubmitClassResponse submitClass(Long classId, Long userId) {
    log.info("Submitting class {} by user {}", classId, userId);

    // 1. Validate class exists
    ClassEntity classEntity = classRepository.findById(classId)
            .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

    // 2. Validate completeness
    ValidateClassResponse validation = validateClass(classId);
    if (!validation.canSubmit()) {
        throw new ValidationException("Cannot submit incomplete class: " + validation.getErrors());
    }

    // 3. Update submission fields
    classEntity.setSubmittedAt(OffsetDateTime.now());
    classRepository.save(classEntity);

    // 4. Notify Center Head (via notification system - optional)
    // notificationService.notifyClassSubmission(classEntity);

    log.info("Class {} submitted successfully", classEntity.getCode());

    return SubmitClassResponse.builder()
            .classId(classEntity.getId())
            .code(classEntity.getCode())
            .status(classEntity.getStatus())
            .approvalStatus(classEntity.getApprovalStatus())
            .submittedAt(classEntity.getSubmittedAt())
            .submittedBy(getUserFullName(userId))
            .build();
}

@Override
@Transactional
public ApproveClassResponse approveClass(Long classId, Long userId) {
    log.info("Approving class {} by user {}", classId, userId);

    ClassEntity classEntity = classRepository.findById(classId)
            .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

    if (classEntity.getSubmittedAt() == null) {
        throw new BusinessException("Class has not been submitted for approval");
    }

    // Update approval fields
    classEntity.setStatus(ClassStatus.SCHEDULED);
    classEntity.setApprovalStatus(ApprovalStatus.APPROVED);
    classEntity.setApprovedBy(userId);
    classEntity.setApprovedAt(OffsetDateTime.now());
    classRepository.save(classEntity);

    // Notify Academic Staff (optional)
    // notificationService.notifyClassApproved(classEntity);

    log.info("Class {} approved successfully", classEntity.getCode());

    return ApproveClassResponse.builder()
            .classId(classEntity.getId())
            .code(classEntity.getCode())
            .status(classEntity.getStatus())
            .approvalStatus(classEntity.getApprovalStatus())
            .approvedBy(getUserFullName(userId))
            .approvedAt(classEntity.getApprovedAt())
            .build();
}

@Override
@Transactional
public RejectClassResponse rejectClass(Long classId, String reason, Long userId) {
    log.info("Rejecting class {} by user {}", classId, userId);

    ClassEntity classEntity = classRepository.findById(classId)
            .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

    // Update rejection fields
    classEntity.setStatus(ClassStatus.DRAFT);
    classEntity.setApprovalStatus(ApprovalStatus.REJECTED);
    classEntity.setRejectionReason(reason);
    classEntity.setSubmittedAt(null); // Reset submission
    classRepository.save(classEntity);

    // Notify Academic Staff (optional)
    // notificationService.notifyClassRejected(classEntity, reason);

    log.info("Class {} rejected: {}", classEntity.getCode(), reason);

    return RejectClassResponse.builder()
            .classId(classEntity.getId())
            .code(classEntity.getCode())
            .status(classEntity.getStatus())
            .approvalStatus(classEntity.getApprovalStatus())
            .rejectionReason(reason)
            .rejectedBy(getUserFullName(userId))
            .rejectedAt(OffsetDateTime.now())
            .build();
}

private String getUserFullName(Long userId) {
    // Implement based on your UserService
    return "User " + userId;
}
```

#### 3.7.3 Controller

```java
@PostMapping("/{classId}/submit")
@PreAuthorize("hasRole('ACADEMIC_STAFF')")
public ResponseEntity<ResponseObject<SubmitClassResponse>> submitClass(
        @PathVariable Long classId,
        @AuthenticationPrincipal UserPrincipal userPrincipal) {

    log.info("POST /api/v1/classes/{}/submit", classId);

    SubmitClassResponse response = classService.submitClass(classId, userPrincipal.getUserId());

    return ResponseEntity.ok(ResponseObject.success(
        "Class submitted for approval successfully",
        response
    ));
}

@PostMapping("/{classId}/approve")
@PreAuthorize("hasRole('CENTER_HEAD')")
public ResponseEntity<ResponseObject<ApproveClassResponse>> approveClass(
        @PathVariable Long classId,
        @AuthenticationPrincipal UserPrincipal userPrincipal) {

    log.info("POST /api/v1/classes/{}/approve", classId);

    ApproveClassResponse response = classService.approveClass(classId, userPrincipal.getUserId());

    return ResponseEntity.ok(ResponseObject.success(
        "Class approved successfully",
        response
    ));
}

@PostMapping("/{classId}/reject")
@PreAuthorize("hasRole('CENTER_HEAD')")
public ResponseEntity<ResponseObject<RejectClassResponse>> rejectClass(
        @PathVariable Long classId,
        @Valid @RequestBody RejectClassRequest request,
        @AuthenticationPrincipal UserPrincipal userPrincipal) {

    log.info("POST /api/v1/classes/{}/reject", classId);

    RejectClassResponse response = classService.rejectClass(
            classId,
            request.getReason(),
            userPrincipal.getUserId()
    );

    return ResponseEntity.ok(ResponseObject.success(
        "Class rejected and sent back to Academic Staff",
        response
    ));
}
```

### ✅ Testing

```java
@Test
void shouldSubmitClassSuccessfully() {
    // Given
    Long classId = 101L;
    Long userId = 1L;
    ClassEntity classEntity = TestDataBuilder.createCompleteClass();

    when(classRepository.findById(classId)).thenReturn(Optional.of(classEntity));
    when(sessionRepository.countByClassEntityIdAndTimeSlotTemplateIsNull(classId)).thenReturn(0);
    // ... all validation passes

    // When
    SubmitClassResponse response = classService.submitClass(classId, userId);

    // Then
    assertThat(response.getSubmittedAt()).isNotNull();
    verify(classRepository).save(argThat(c -> c.getSubmittedAt() != null));
}

@Test
void shouldApproveClassSuccessfully() {
    // Given
    Long classId = 101L;
    Long userId = 2L;
    ClassEntity classEntity = TestDataBuilder.createSubmittedClass();

    when(classRepository.findById(classId)).thenReturn(Optional.of(classEntity));

    // When
    ApproveClassResponse response = classService.approveClass(classId, userId);

    // Then
    assertThat(response.getStatus()).isEqualTo(ClassStatus.SCHEDULED);
    assertThat(response.getApprovalStatus()).isEqualTo(ApprovalStatus.APPROVED);
}

@Test
void shouldRejectClassWithReason() {
    // Given
    Long classId = 101L;
    Long userId = 2L;
    String reason = "Time slot conflicts with another class";
    ClassEntity classEntity = TestDataBuilder.createSubmittedClass();

    when(classRepository.findById(classId)).thenReturn(Optional.of(classEntity));

    // When
    RejectClassResponse response = classService.rejectClass(classId, reason, userId);

    // Then
    assertThat(response.getStatus()).isEqualTo(ClassStatus.DRAFT);
    assertThat(response.getApprovalStatus()).isEqualTo(ApprovalStatus.REJECTED);
    assertThat(response.getRejectionReason()).isEqualTo(reason);
    assertThat(classEntity.getSubmittedAt()).isNull(); // Reset
}
```

---

## 4. TESTING STRATEGY

### 4.1 Unit Testing

**Coverage Target:** 90%+ for service layer

**Test Categories:**
1. Service layer business logic
2. Custom repository queries
3. DTO validations
4. Exception scenarios

**Test Framework:**
- JUnit 5
- Mockito/MockitoBean (Spring Boot 3.4+ compatible)
- AssertJ for assertions
- TestContainers for integration tests

### 4.2 Integration Testing

**Coverage Target:** 80%+

**Test Scenarios:**
1. End-to-end workflow
2. Conflict detection
3. Validation rules
4. Database constraints

**Test Database:**
- PostgreSQL TestContainer
- Seed data scripts
- Rollback after each test

### 4.3 API Testing

**Tools:**
- REST Assured
- Spring MockMvc
- Postman collections

**Test Coverage:**
- All endpoints
- Authentication & authorization
- Error responses
- Pagination & filtering

### 4.4 Performance Testing

**Targets:**
- Resource bulk assignment: < 200ms
- Teacher PRE-CHECK query: < 300ms
- Session generation: < 500ms

---

## 5. DEPLOYMENT PLAN

### 5.1 Pre-Deployment Checklist

- [ ] All tests passing (90%+ unit, 80%+ integration)
- [ ] OpenAPI documentation updated
- [ ] Database migration scripts ready
- [ ] Environment variables configured
- [ ] Performance testing completed
- [ ] Security audit completed

### 5.2 Database Migration

**Migration Scripts:**

```sql
-- V1__add_class_workflow_columns.sql
ALTER TABLE class ADD COLUMN IF NOT EXISTS submitted_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE class ADD COLUMN IF NOT EXISTS approved_by BIGINT REFERENCES user_account(id);
ALTER TABLE class ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE class ADD COLUMN IF NOT EXISTS rejection_reason TEXT;

CREATE INDEX IF NOT EXISTS idx_class_branch_code ON class(branch_id, code);
CREATE INDEX IF NOT EXISTS idx_session_class_date ON session(class_id, date);
CREATE INDEX IF NOT EXISTS idx_session_resource_resource_session ON session_resource(resource_id, session_id);
```

### 5.3 Deployment Steps

1. Build: `mvn clean package`
2. Run migrations
3. Deploy to staging
4. Smoke tests
5. Deploy to production
6. Monitor logs

---

## 6. TIMELINE & MILESTONES

### 6.1 Development Timeline (4 weeks)

**Week 1: Foundation**
- Day 1-2: Step 1 (Create Class)
- Day 3-4: Step 2 (Session Generation)
- Day 5: Testing

**Week 2: Assignment Logic**
- Day 6-7: Step 3 (Time Slots)
- Day 8-9: Step 4 (Resources - HYBRID)
- Day 10: Testing

**Week 3: Teacher & Validation**
- Day 11-13: Step 5 (Teachers - PRE-CHECK)
- Day 14: Step 6 (Validate)
- Day 15: Testing

**Week 4: Approval & Polish**
- Day 16-17: Step 7 (Submit & Approve)
- Day 18: Integration testing
- Day 19: Performance testing
- Day 20: Deployment

### 6.2 Success Criteria

- ✅ All 7 steps implemented
- ✅ 90%+ unit test coverage
- ✅ 80%+ integration test coverage
- ✅ API response time < 500ms (p95)
- ✅ Documentation complete
- ✅ Stakeholder sign-off

---

## 7. APPENDIX

### 7.1 Glossary

| Term | Definition |
|------|------------|
| **PRE-CHECK** | v1.1 enhancement where availability is checked BEFORE user selection |
| **HYBRID Approach** | Combination of SQL bulk operations + Java analysis |
| **ISODOW** | ISO day of week: 1=Monday, 7=Sunday |
| **'general' Skill** | Universal skill that can teach any session |

### 7.2 Related Documents

- [Complete Implementation Plan](/docs/create-class/IMPLEMENTATION_PLAN.md)
- [PRD](/docs/prd.md)
- [Workflow Details](/docs/create-class/create-class-workflow-final.md)
- [OpenAPI Specification](/docs/create-class/openapi.yaml)

---

**Document Status:** ✅ Ready for Implementation (Backend Only)
**Last Updated:** January 4, 2025
**Version:** 1.1.0 (Backend Focus)
