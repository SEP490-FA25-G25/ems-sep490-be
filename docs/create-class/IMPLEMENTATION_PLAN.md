# 📋 IMPLEMENTATION PLAN: Class Creation Workflow

**Project:** Training Management System (TMS)
**Feature:** Complete Class Creation Workflow (7 Steps)
**Version:** 1.1.0
**Date:** January 4, 2025
**Status:** Ready for Implementation

---

## 📑 TABLE OF CONTENTS

1. [Executive Summary](#1-executive-summary)
2. [Architecture Overview](#2-architecture-overview)
3. [Implementation Steps](#3-implementation-steps)
   - [Step 1: Create Class](#step-1-create-class)
   - [Step 2: Generate Sessions](#step-2-generate-sessions-auto)
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

The Class Creation Workflow is a **7-step process** that enables Academic Staff to create and configure classes for student enrollment. The workflow includes automated session generation, intelligent resource allocation, skill-based teacher matching, and approval workflow.

### 1.2 Key Features

- ✅ **Automated Session Generation**: System generates 36 sessions from course template
- ✅ **Flexible Scheduling**: Different time slots per day (Mon/Wed/Fri can have different times)
- ✅ **HYBRID Resource Assignment**: SQL bulk operations + detailed conflict analysis
- ✅ **PRE-CHECK Teacher Matching**: Shows availability BEFORE user selection (v1.1)
- ✅ **Smart Skill Matching**: 'general' skill = universal, can teach any session
- ✅ **Approval Workflow**: Center Head approval with rejection reason

### 1.3 User Roles

| Role | Responsibilities |
|------|------------------|
| **Academic Staff** | Create class, assign resources/teachers, submit for approval |
| **Center Head** | Approve/reject class submissions |
| **System** | Auto-generate sessions, validate completeness, detect conflicts |

### 1.4 Success Criteria

- ✅ Academic Staff can create a class in < 10 minutes
- ✅ 90% of sessions assigned without conflicts
- ✅ Teachers see availability status before assignment
- ✅ Class validation catches 100% of incomplete assignments
- ✅ Approval workflow completes in < 1 day

---

## 2. ARCHITECTURE OVERVIEW

### 2.1 Technology Stack

**Backend:**
- Spring Boot 3.5.7 (Java 21)
- PostgreSQL 16
- JPA/Hibernate
- Spring Security + JWT

**Frontend (Reference):**
- React 18+ / Vue 3+
- TypeScript
- Axios for API calls
- Ant Design / Material-UI

### 2.2 Package Structure

```
org.fyp.tmssep490be/
├── controllers/
│   └── ClassManagementController.java
├── services/
│   ├── ClassService.java
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
│   └── TimeSlotTemplateRepository.java
├── dtos/
│   └── classmanagement/
│       ├── CreateClassRequest.java
│       ├── AssignTimeSlotsRequest.java
│       ├── AssignResourcesRequest.java
│       ├── AssignTeacherRequest.java
│       └── [Response DTOs...]
└── entities/
    ├── ClassEntity.java
    ├── Session.java
    ├── SessionResource.java
    └── TeachingSlot.java
```

### 2.3 Database Schema (Key Tables)

```sql
-- Core Tables
class
session
session_resource
teaching_slot
time_slot_template
resource
teacher
teacher_skill
teacher_availability

-- Related Tables
course
course_session
branch
user_account
```

### 2.4 API Endpoints Summary

| Step | Endpoint | Method | Description |
|------|----------|--------|-------------|
| 1 | `/api/v1/classes` | POST | Create class |
| 2 | Auto-triggered | - | Generate sessions |
| 3 | `/api/v1/classes/{id}/time-slots` | POST | Assign time slots |
| 4 | `/api/v1/classes/{id}/resources` | POST | Assign resources |
| 5a | `/api/v1/classes/{id}/available-teachers` | GET | Get teachers with availability |
| 5b | `/api/v1/classes/{id}/teachers` | POST | Assign teacher |
| 6 | `/api/v1/classes/{id}/validate` | POST | Validate completeness |
| 7a | `/api/v1/classes/{id}/submit` | POST | Submit for approval |
| 7b | `/api/v1/classes/{id}/approve` | POST | Approve class |
| 7c | `/api/v1/classes/{id}/reject` | POST | Reject class |

---

## 3. IMPLEMENTATION STEPS

---

## STEP 1: Create Class

### 🎯 Objective

Academic Staff creates a new class with basic information (branch, course, modality, schedule, capacity).

### 📊 Backend Implementation

#### 3.1.1 Controller

**File:** `ClassManagementController.java`

```java
@RestController
@RequestMapping("/api/v1/classes")
@RequiredArgsConstructor
public class ClassManagementController {

    private final ClassService classService;

    @PostMapping
    @PreAuthorize("hasRole('ACADEMIC_STAFF')")
    public ResponseEntity<ResponseObject<CreateClassResponse>> createClass(
            @Valid @RequestBody CreateClassRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        CreateClassResponse response = classService.createClass(request, userPrincipal.getUserId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseObject.success(
                    "Class created successfully with " + response.getSessionsGenerated() + " sessions generated",
                    response
                ));
    }
}
```

#### 3.1.2 Service Interface

**File:** `ClassService.java`

```java
public interface ClassService {
    CreateClassResponse createClass(CreateClassRequest request, Long userId);
    // ... other methods
}
```

#### 3.1.3 Service Implementation

**File:** `ClassServiceImpl.java`

```java
@Service
@RequiredArgsConstructor
@Transactional
public class ClassServiceImpl implements ClassService {

    private final ClassRepository classRepository;
    private final CourseRepository courseRepository;
    private final BranchRepository branchRepository;
    private final SessionGenerationService sessionGenerationService;

    @Override
    public CreateClassResponse createClass(CreateClassRequest request, Long userId) {
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

        // 6. AUTO-TRIGGER: Generate sessions (STEP 2)
        int sessionsGenerated = sessionGenerationService.generateSessions(classEntity);

        // 7. Calculate planned_end_date
        LocalDate plannedEndDate = calculatePlannedEndDate(classEntity);
        classEntity.setPlannedEndDate(plannedEndDate);
        classRepository.save(classEntity);

        // 8. Build response
        return CreateClassResponse.builder()
                .classId(classEntity.getId())
                .code(classEntity.getCode())
                .branchId(branch.getId())
                .courseId(course.getId())
                .modality(classEntity.getModality())
                .startDate(classEntity.getStartDate())
                .scheduleDays(classEntity.getScheduleDays())
                .maxCapacity(classEntity.getMaxCapacity())
                .status(classEntity.getStatus())
                .approvalStatus(classEntity.getApprovalStatus())
                .sessionsGenerated(sessionsGenerated)
                .createdAt(classEntity.getCreatedAt())
                .build();
    }

    private void validateStartDateInScheduleDays(LocalDate startDate, List<Integer> scheduleDays) {
        int dayOfWeek = startDate.getDayOfWeek().getValue(); // 1=Mon, 7=Sun
        if (!scheduleDays.contains(dayOfWeek)) {
            throw new ValidationException(
                "Start date must be one of the schedule days. " +
                "Start date is " + startDate.getDayOfWeek() + " but schedule days are " + scheduleDays
            );
        }
    }

    private LocalDate calculatePlannedEndDate(ClassEntity classEntity) {
        // Get last session date
        return sessionRepository.findTopByClassEntityOrderByDateDesc(classEntity)
                .map(Session::getDate)
                .orElse(null);
    }
}
```

#### 3.1.4 DTOs

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
    @Size(min = 1, max = 7)
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
    private List<Integer> scheduleDays;
    private Integer maxCapacity;
    private ClassStatus status;
    private ApprovalStatus approvalStatus;
    private Integer sessionsGenerated;
    private OffsetDateTime createdAt;
}
```

### 🎨 Frontend Screen Design

#### Screen 1.1: Create Class Form

**Route:** `/classes/create`

**Layout:**

```
┌─────────────────────────────────────────────────────────────────┐
│ 📘 Create New Class                                   [X] Close  │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│ Step 1 of 7: Basic Information                                 │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│                                                                 │
│ Branch *                                                        │
│ ┌──────────────────────────────────────────────────┐           │
│ │ Select branch...                              ▼  │           │
│ └──────────────────────────────────────────────────┘           │
│   Options: Main Campus, Downtown, Northside                    │
│                                                                 │
│ Course *                                                        │
│ ┌──────────────────────────────────────────────────┐           │
│ │ Select course...                              ▼  │           │
│ └──────────────────────────────────────────────────┘           │
│   Only shows APPROVED courses                                  │
│   Filter by: Subject ▼  Level ▼                                │
│                                                                 │
│ Class Code *                                                    │
│ ┌──────────────────────────────────────────────────┐           │
│ │ ENG-A1-2025-01                                   │           │
│ └──────────────────────────────────────────────────┘           │
│   Format: [SUBJECT]-[LEVEL]-[YEAR]-[SEQUENCE]                  │
│   ⓘ Must be unique within selected branch                      │
│                                                                 │
│ Class Name (Optional)                                           │
│ ┌──────────────────────────────────────────────────┐           │
│ │ English A1 Foundation - Morning Class            │           │
│ └──────────────────────────────────────────────────┘           │
│                                                                 │
│ ┌─────────────────────────────────────────────────────────────┐│
││ Modality *                                                    ││
││                                                               ││
││ ┌─────────┐  ┌─────────┐  ┌─────────┐                       ││
││ │ OFFLINE │  │ ONLINE  │  │ HYBRID  │                       ││
││ │   ●     │  │    ○    │  │    ○    │                       ││
││ │🏫 Room  │  │💻 Zoom  │  │🏫💻Both │                       ││
││ └─────────┘  └─────────┘  └─────────┘                       ││
│└─────────────────────────────────────────────────────────────┘│
│                                                                 │
│ Start Date *                           Max Capacity *           │
│ ┌──────────────────────┐             ┌────────────────┐        │
│ │ 2025-01-06      📅   │             │ 20             │        │
│ └──────────────────────┘             └────────────────┘        │
│   Must be in schedule days             students (1-100)        │
│                                                                 │
│ Schedule Days * (Select days of the week)                      │
│ ┌─────┬─────┬─────┬─────┬─────┬─────┬─────┐                  │
│ │ Mon │ Tue │ Wed │ Thu │ Fri │ Sat │ Sun │                  │
│ │  ☑  │  ☐  │  ☑  │  ☐  │  ☑  │  ☐  │  ☐  │                  │
│ └─────┴─────┴─────┴─────┴─────┴─────┴─────┘                  │
│   Selected: Monday, Wednesday, Friday                          │
│   ⓘ Start date (2025-01-06) is Monday ✓                       │
│                                                                 │
│ ┌─────────────────────────────────────────────────────────────┐│
││ 📊 Estimated Schedule:                                        ││
││   • Duration: 12 weeks (from course template)                ││
││   • Sessions per week: 3 (Mon/Wed/Fri)                       ││
││   • Total sessions: 36 sessions                              ││
││   • Estimated end date: 2025-03-28                           ││
│└─────────────────────────────────────────────────────────────┘│
│                                                                 │
│               [Cancel]  [Save as Draft]  [Create Class] →      │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**UI Components:**

```typescript
// React/TypeScript Example
interface CreateClassFormProps {
  branches: Branch[];
  onSubmit: (data: CreateClassRequest) => Promise<void>;
}

const CreateClassForm: React.FC<CreateClassFormProps> = ({ branches, onSubmit }) => {
  const [formData, setFormData] = useState<CreateClassRequest>({
    branchId: null,
    courseId: null,
    code: '',
    name: '',
    modality: Modality.OFFLINE,
    startDate: null,
    scheduleDays: [],
    maxCapacity: 20
  });

  const [courses, setCourses] = useState<Course[]>([]);
  const [loading, setLoading] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});

  // Load approved courses when branch is selected
  useEffect(() => {
    if (formData.branchId) {
      fetchApprovedCourses(formData.branchId).then(setCourses);
    }
  }, [formData.branchId]);

  // Validate start date is in schedule days
  useEffect(() => {
    if (formData.startDate && formData.scheduleDays.length > 0) {
      const dayOfWeek = formData.startDate.isoWeekday(); // 1=Mon, 7=Sun
      if (!formData.scheduleDays.includes(dayOfWeek)) {
        setErrors(prev => ({
          ...prev,
          startDate: `Start date must be one of the schedule days (${getDayNames(formData.scheduleDays)})`
        }));
      } else {
        setErrors(prev => {
          const { startDate, ...rest } = prev;
          return rest;
        });
      }
    }
  }, [formData.startDate, formData.scheduleDays]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    // Client-side validation
    const validationErrors = validateForm(formData);
    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      return;
    }

    setLoading(true);
    try {
      await onSubmit(formData);
      // Navigate to next step (Session Generation - auto)
    } catch (error) {
      handleApiError(error, setErrors);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Form onSubmit={handleSubmit}>
      {/* Form fields as shown in layout */}
    </Form>
  );
};
```

**Validation Rules (Client-side):**

```typescript
function validateForm(data: CreateClassRequest): Record<string, string> {
  const errors: Record<string, string> = {};

  if (!data.branchId) errors.branchId = 'Branch is required';
  if (!data.courseId) errors.courseId = 'Course is required';
  if (!data.code) {
    errors.code = 'Class code is required';
  } else if (!/^[A-Z0-9-]+$/.test(data.code)) {
    errors.code = 'Class code must contain only uppercase letters, numbers, and hyphens';
  }
  if (!data.modality) errors.modality = 'Modality is required';
  if (!data.startDate) errors.startDate = 'Start date is required';
  if (data.scheduleDays.length === 0) errors.scheduleDays = 'At least one schedule day is required';
  if (!data.maxCapacity || data.maxCapacity < 1 || data.maxCapacity > 100) {
    errors.maxCapacity = 'Max capacity must be between 1 and 100';
  }

  // Validate start date is in schedule days
  if (data.startDate && data.scheduleDays.length > 0) {
    const dayOfWeek = data.startDate.isoWeekday();
    if (!data.scheduleDays.includes(dayOfWeek)) {
      errors.startDate = 'Start date must be one of the selected schedule days';
    }
  }

  return errors;
}
```

#### Screen 1.2: Success Modal

After successful creation:

```
┌─────────────────────────────────────────────┐
│            ✅ Success!                       │
├─────────────────────────────────────────────┤
│                                             │
│  Class "ENG-A1-2025-01" created successfully│
│                                             │
│  📊 Summary:                                │
│  • 36 sessions generated                    │
│  • Duration: 12 weeks                       │
│  • Start: 2025-01-06                        │
│  • End: 2025-03-28                          │
│                                             │
│  Next step: Assign time slots               │
│                                             │
│        [View Class]  [Continue Setup] →     │
│                                             │
└─────────────────────────────────────────────┘
```

### 🔗 API Integration (Frontend)

```typescript
// API Service
export class ClassService {

  async createClass(request: CreateClassRequest): Promise<CreateClassResponse> {
    const response = await axios.post<ResponseObject<CreateClassResponse>>(
      '/api/v1/classes',
      request,
      {
        headers: {
          'Authorization': `Bearer ${getAuthToken()}`
        }
      }
    );

    if (!response.data.success) {
      throw new ApiError(response.data.message);
    }

    return response.data.data;
  }

  async getApprovedCourses(branchId: number): Promise<Course[]> {
    const response = await axios.get<ResponseObject<Course[]>>(
      '/api/v1/courses',
      {
        params: {
          branchId,
          approvalStatus: 'APPROVED',
          status: 'ACTIVE'
        }
      }
    );
    return response.data.data;
  }
}
```

### ✅ Testing

**Unit Tests:**

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
                .startDate(LocalDate.of(2025, 1, 6)) // Monday
                .scheduleDays(List.of(1, 3, 5)) // Mon, Wed, Fri
                .maxCapacity(20)
                .build();

        Course course = createTestCourse(ApprovalStatus.APPROVED);
        Branch branch = createTestBranch();

        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));
        when(classRepository.existsByBranchAndCode(branch, "ENG-A1-2025-01")).thenReturn(false);
        when(sessionGenerationService.generateSessions(any())).thenReturn(36);

        // When
        CreateClassResponse response = classService.createClass(request, 1L);

        // Then
        assertThat(response.getSessionsGenerated()).isEqualTo(36);
        assertThat(response.getStatus()).isEqualTo(ClassStatus.DRAFT);
        verify(sessionGenerationService).generateSessions(any());
    }

    @Test
    void shouldThrowExceptionWhenCourseNotApproved() {
        // Given
        CreateClassRequest request = createTestRequest();
        Course course = createTestCourse(ApprovalStatus.PENDING);

        when(courseRepository.findById(anyLong())).thenReturn(Optional.of(course));

        // When & Then
        assertThatThrownBy(() -> classService.createClass(request, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Course must be approved");
    }

    @Test
    void shouldThrowExceptionWhenStartDateNotInScheduleDays() {
        // Given - Start date is Tuesday but schedule days are Mon/Wed/Fri
        CreateClassRequest request = CreateClassRequest.builder()
                .branchId(1L)
                .courseId(10L)
                .code("ENG-A1-2025-01")
                .startDate(LocalDate.of(2025, 1, 7)) // Tuesday
                .scheduleDays(List.of(1, 3, 5)) // Mon, Wed, Fri
                .maxCapacity(20)
                .build();

        // When & Then
        assertThatThrownBy(() -> classService.createClass(request, 1L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Start date must be one of the schedule days");
    }
}
```

---

## STEP 2: Generate Sessions (Auto)

### 🎯 Objective

System automatically generates sessions from course template based on start date and schedule days.

### 📊 Backend Implementation

#### 3.2.1 Service Implementation

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
     *
     * @param classEntity The class to generate sessions for
     * @return Number of sessions generated
     */
    @Transactional
    public int generateSessions(ClassEntity classEntity) {
        log.info("Generating sessions for class: {}", classEntity.getCode());

        // 1. Load course sessions template
        List<CourseSession> courseSessions = courseSessionRepository
                .findByCourseOrderByPhasePhaseNumberAscSequenceNoAsc(classEntity.getCourse());

        if (courseSessions.isEmpty()) {
            throw new BusinessException("Course has no sessions defined");
        }

        // 2. Generate sessions with calculated dates
        LocalDate currentDate = classEntity.getStartDate();
        List<Integer> scheduleDays = classEntity.getScheduleDays();
        int sessionIndex = 0;

        List<Session> sessions = new ArrayList<>();

        for (CourseSession courseSession : courseSessions) {
            // Calculate target day of week for this session
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

        // 3. Save all sessions
        sessionRepository.saveAll(sessions);

        log.info("Generated {} sessions for class {}", sessions.size(), classEntity.getCode());
        return sessions.size();
    }
}
```

**Example Output:**

```
Input:
- start_date: 2025-01-06 (Monday)
- schedule_days: [1, 3, 5] (Mon, Wed, Fri)
- course_sessions: 36 sessions

Output:
Session 1 (course_session_id=1)  → 2025-01-06 (Monday)
Session 2 (course_session_id=2)  → 2025-01-08 (Wednesday)
Session 3 (course_session_id=3)  → 2025-01-10 (Friday)
Session 4 (course_session_id=4)  → 2025-01-13 (Monday, Week 2)
...
Session 36 (course_session_id=36) → 2025-03-28 (Friday, Week 12)
```

### 🎨 Frontend Screen Design

#### Screen 2.1: Auto-Generation Progress

**This step is automatic - show progress indicator**

```
┌─────────────────────────────────────────────┐
│  ⏳ Generating Sessions...                  │
├─────────────────────────────────────────────┤
│                                             │
│  Class: ENG-A1-2025-01                      │
│                                             │
│  ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓░░░░ 80%                  │
│                                             │
│  ✓ Loaded course template (36 sessions)    │
│  ✓ Calculated dates for 12 weeks           │
│  ⏳ Creating session records...             │
│  □ Setting up time slots                    │
│                                             │
└─────────────────────────────────────────────┘
```

#### Screen 2.2: Generation Complete

```
┌─────────────────────────────────────────────┐
│  ✅ Sessions Generated Successfully!        │
├─────────────────────────────────────────────┤
│                                             │
│  📊 Generation Summary:                     │
│  • Total sessions: 36                       │
│  • Duration: 12 weeks                       │
│  • First session: Mon, Jan 6, 2025          │
│  • Last session: Fri, Mar 28, 2025          │
│                                             │
│  Schedule pattern:                          │
│  Monday: 12 sessions                        │
│  Wednesday: 12 sessions                     │
│  Friday: 12 sessions                        │
│                                             │
│  [View Session List]  [Continue Setup] →    │
│                                             │
└─────────────────────────────────────────────┘
```

### ✅ Testing

```java
@Test
void shouldGenerateCorrectNumberOfSessions() {
    // Given
    ClassEntity classEntity = createTestClass(
        LocalDate.of(2025, 1, 6), // Monday
        List.of(1, 3, 5) // Mon, Wed, Fri
    );
    List<CourseSession> courseSessions = createTestCourseSessions(36);

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
    ClassEntity classEntity = createTestClass(
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
```

---

## STEP 3: Assign Time Slots

### 🎯 Objective

Academic Staff assigns time slots for each schedule day. Different days can have different time slots.

### 📊 Backend Implementation

#### 3.3.1 Controller

```java
@PostMapping("/{classId}/time-slots")
@PreAuthorize("hasRole('ACADEMIC_STAFF')")
public ResponseEntity<ResponseObject<AssignTimeSlotsResponse>> assignTimeSlots(
        @PathVariable Long classId,
        @Valid @RequestBody AssignTimeSlotsRequest request) {

    AssignTimeSlotsResponse response = timeSlotAssignmentService.assignTimeSlots(classId, request);

    return ResponseEntity.ok(ResponseObject.success(
        "Time slots assigned successfully to " + response.getAssignedSessions() + " sessions",
        response
    ));
}
```

#### 3.3.2 Service Implementation

**File:** `TimeSlotAssignmentService.java`

```java
@Service
@RequiredArgsConstructor
@Transactional
public class TimeSlotAssignmentService {

    private final SessionRepository sessionRepository;
    private final TimeSlotTemplateRepository timeSlotTemplateRepository;
    private final ClassRepository classRepository;

    public AssignTimeSlotsResponse assignTimeSlots(Long classId, AssignTimeSlotsRequest request) {
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

        // 3. Assign time slots per day
        List<AssignTimeSlotsResponse.AssignmentResult> results = new ArrayList<>();
        int totalAssigned = 0;

        for (AssignTimeSlotsRequest.Assignment assignment : request.getAssignments()) {
            int dayOfWeek = assignment.getDayOfWeek();
            Long timeSlotId = assignment.getTimeSlotTemplateId();

            // Update all sessions on this day of week
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
        }

        return AssignTimeSlotsResponse.builder()
                .classId(classId)
                .assignedSessions(totalAssigned)
                .assignments(results)
                .build();
    }
}
```

#### 3.3.3 Repository Method

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
}
```

### 🎨 Frontend Screen Design

#### Screen 3.1: Assign Time Slots

```
┌────────────────────────────────────────────────────────────────────┐
│ 📘 Class Setup: ENG-A1-2025-01                       Step 3 of 7    │
├────────────────────────────────────────────────────────────────────┤
│                                                                    │
│ ⏰ Assign Time Slots for Schedule Days                            │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│                                                                    │
│ Assign a time slot for each day in your class schedule.           │
│ You can use the same time slot for all days or different ones.    │
│                                                                    │
│ ┌────────────────────────────────────────────────────────────────┐ │
│ │ Monday (12 sessions)                                           │ │
│ │ ┌──────────────────────────────────────────────────────┐       │ │
│ │ │ Select time slot...                               ▼  │       │ │
│ │ └──────────────────────────────────────────────────────┘       │ │
│ │                                                                │ │
│ │ Available time slots for Main Campus:                         │ │
│ │ ● Morning Slot 1 (07:00-08:30) - 90 min                       │ │
│ │ ● Morning Slot 2 (08:45-10:15) - 90 min     ← Selected        │ │
│ │ ● Morning Slot 3 (10:30-12:00) - 90 min                       │ │
│ │ ● Afternoon Slot 1 (13:00-14:30) - 90 min                     │ │
│ │ ● Afternoon Slot 2 (14:45-16:15) - 90 min                     │ │
│ │ ● Evening Slot 1 (18:15-19:45) - 90 min                       │ │
│ └────────────────────────────────────────────────────────────────┘ │
│                                                                    │
│ ┌────────────────────────────────────────────────────────────────┐ │
│ │ Wednesday (12 sessions)                                        │ │
│ │ ┌──────────────────────────────────────────────────────┐       │ │
│ │ │ Morning Slot 2 (08:45-10:15)                     ✓   │       │ │
│ │ └──────────────────────────────────────────────────────┘       │ │
│ │ 💡 Using same time slot as Monday                             │ │
│ └────────────────────────────────────────────────────────────────┘ │
│                                                                    │
│ ┌────────────────────────────────────────────────────────────────┐ │
│ │ Friday (12 sessions)                                           │ │
│ │ ┌──────────────────────────────────────────────────────┐       │ │
│ │ │ Afternoon Slot 2 (14:45-16:15)                   ✓   │       │ │
│ │ └──────────────────────────────────────────────────────┘       │ │
│ │ ⚠️ Different time slot than other days                        │ │
│ └────────────────────────────────────────────────────────────────┘ │
│                                                                    │
│ ┌────────────────────────────────────────────────────────────────┐ │
│ │ 🔧 Quick Actions:                                              │ │
│ │ [Apply Same Time Slot to All Days]                            │ │
│ │ [Reset All]                                                    │ │
│ └────────────────────────────────────────────────────────────────┘ │
│                                                                    │
│ ┌────────────────────────────────────────────────────────────────┐ │
│ │ 📊 Summary:                                                    │ │
│ │ • Monday: Morning Slot 2 (08:45-10:15) - 12 sessions           │ │
│ │ • Wednesday: Morning Slot 2 (08:45-10:15) - 12 sessions        │ │
│ │ • Friday: Afternoon Slot 2 (14:45-16:15) - 12 sessions         │ │
│ │ • Total: 36/36 sessions assigned ✓                             │ │
│ └────────────────────────────────────────────────────────────────┘ │
│                                                                    │
│          ← [Back]  [Save Progress]  [Continue to Resources] →     │
│                                                                    │
└────────────────────────────────────────────────────────────────────┘
```

**UI Components:**

```typescript
interface AssignTimeSlotsScreenProps {
  classId: number;
  scheduleDays: number[]; // [1, 3, 5]
  onComplete: () => void;
}

const AssignTimeSlotsScreen: React.FC<AssignTimeSlotsScreenProps> = ({
  classId,
  scheduleDays,
  onComplete
}) => {
  const [timeSlots, setTimeSlots] = useState<TimeSlotTemplate[]>([]);
  const [assignments, setAssignments] = useState<Record<number, number>>({}); // dayOfWeek -> timeSlotId
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    // Load available time slot templates for branch
    fetchTimeSlotTemplates(branchId).then(setTimeSlots);
  }, [branchId]);

  const handleAssign = (dayOfWeek: number, timeSlotId: number) => {
    setAssignments(prev => ({
      ...prev,
      [dayOfWeek]: timeSlotId
    }));
  };

  const handleApplyToAll = () => {
    const firstAssignment = assignments[scheduleDays[0]];
    if (firstAssignment) {
      const newAssignments = {};
      scheduleDays.forEach(day => {
        newAssignments[day] = firstAssignment;
      });
      setAssignments(newAssignments);
    }
  };

  const handleSubmit = async () => {
    // Validate all days assigned
    const allAssigned = scheduleDays.every(day => assignments[day]);
    if (!allAssigned) {
      showError('Please assign time slots for all schedule days');
      return;
    }

    setLoading(true);
    try {
      const request: AssignTimeSlotsRequest = {
        assignments: scheduleDays.map(day => ({
          dayOfWeek: day,
          timeSlotTemplateId: assignments[day]
        }))
      };

      await classService.assignTimeSlots(classId, request);
      onComplete(); // Navigate to next step
    } catch (error) {
      handleApiError(error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="assign-timeslots-screen">
      {/* UI as shown in layout */}
    </div>
  );
};
```

### 🔗 API Integration

```typescript
export class ClassService {
  async assignTimeSlots(
    classId: number,
    request: AssignTimeSlotsRequest
  ): Promise<AssignTimeSlotsResponse> {
    const response = await axios.post<ResponseObject<AssignTimeSlotsResponse>>(
      `/api/v1/classes/${classId}/time-slots`,
      request
    );
    return response.data.data;
  }

  async getTimeSlotTemplates(branchId: number): Promise<TimeSlotTemplate[]> {
    const response = await axios.get<ResponseObject<TimeSlotTemplate[]>>(
      `/api/v1/branches/${branchId}/time-slot-templates`
    );
    return response.data.data;
  }
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

Academic Staff assigns resources (rooms/Zoom) to sessions with HYBRID auto-propagation (SQL bulk + conflict analysis).

### 📊 Backend Implementation

#### 3.4.1 Controller

```java
@PostMapping("/{classId}/resources")
@PreAuthorize("hasRole('ACADEMIC_STAFF')")
public ResponseEntity<ResponseObject<AssignResourcesResponse>> assignResources(
        @PathVariable Long classId,
        @Valid @RequestBody AssignResourcesRequest request) {

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
        @RequestParam(required = false) LocalDate date,
        @RequestParam(required = false) Long timeSlotTemplateId) {

    List<ResourceDTO> resources = resourceAssignmentService
            .getAvailableResources(classId, sessionId, date, timeSlotTemplateId);

    return ResponseEntity.ok(ResponseObject.success("Available resources retrieved", resources));
}
```

#### 3.4.2 Service Implementation

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
     * Phase 1: SQL bulk assign (fast - 90% of sessions)
     * Phase 2: Detect conflicts (detailed analysis - 10% of sessions)
     */
    public AssignResourcesResponse assignResources(Long classId, AssignResourcesRequest request) {
        log.info("Assigning resources for class: {}", classId);

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

        // 3. PHASE 1: SQL Bulk Assignment (Fast)
        int totalSuccessCount = 0;
        for (AssignResourcesRequest.PatternItem pattern : request.getPattern()) {
            int assigned = sessionResourceRepository.bulkAssignResource(
                    classId,
                    pattern.getDayOfWeek(),
                    pattern.getResourceId(),
                    ResourceType.ROOM
            );
            totalSuccessCount += assigned;
            log.debug("Bulk assigned resource {} to {} sessions on day {}",
                    pattern.getResourceId(), assigned, pattern.getDayOfWeek());
        }

        // 4. PHASE 2: Find Conflicts (Detailed Analysis)
        List<ConflictDetail> conflicts = new ArrayList<>();
        for (AssignResourcesRequest.PatternItem pattern : request.getPattern()) {
            // Find sessions still unassigned for this day
            List<Session> unassignedSessions = sessionRepository
                    .findUnassignedSessionsByDayOfWeek(classId, pattern.getDayOfWeek());

            // Analyze each conflict
            for (Session session : unassignedSessions) {
                ConflictDetail conflict = analyzeResourceConflict(
                        session,
                        pattern.getResourceId(),
                        resourceMap.get(pattern.getResourceId())
                );
                conflicts.add(conflict);
            }
        }

        // 5. Build response
        int totalSessions = sessionRepository.countByClassEntityId(classId);

        return AssignResourcesResponse.builder()
                .classId(classId)
                .totalSessions(totalSessions)
                .successCount(totalSuccessCount)
                .conflictCount(conflicts.size())
                .conflicts(conflicts)
                .build();
    }

    private ConflictDetail analyzeResourceConflict(Session session, Long resourceId, Resource resource) {
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

        return ConflictDetail.builder()
                .sessionId(session.getId())
                .sessionNumber(getSessionNumber(session))
                .date(session.getDate())
                .dayOfWeek(session.getDate().getDayOfWeek().getValue())
                .timeSlotName(session.getTimeSlotTemplate().getName())
                .conflictType(ConflictType.RESOURCE_UNAVAILABLE)
                .reason(reason)
                .conflictingClassCode(conflictingClassCode)
                .build();
    }

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

#### 3.4.3 Repository Methods

**File:** `SessionResourceRepository.java`

```java
public interface SessionResourceRepository extends JpaRepository<SessionResource, Long> {

    /**
     * Bulk assign resource using SQL - Fast path
     * Returns number of sessions assigned (excluding conflicts)
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

**File:** `SessionRepository.java`

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

### 🎨 Frontend Screen Design

#### Screen 4.1: Assign Resources - Week 1 Pattern

```
┌────────────────────────────────────────────────────────────────────┐
│ 📘 Class Setup: ENG-A1-2025-01                       Step 4 of 7    │
├────────────────────────────────────────────────────────────────────┤
│                                                                    │
│ 🏫 Assign Resources for Class Sessions                            │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│                                                                    │
│ Assign rooms or online accounts for your class schedule.          │
│ System will auto-propagate to all matching days.                  │
│                                                                    │
│ ⚙️ Modality: OFFLINE (Rooms required)                             │
│                                                                    │
│ ┌────────────────────────────────────────────────────────────────┐ │
│ │ 📅 Week 1 Pattern (Representative Sessions)                    │ │
│ ├────────────────────────────────────────────────────────────────┤ │
│ │                                                                │ │
│ │ Session 1 - Monday, Jan 6, 2025                                │ │
│ │ ⏰ Time: 08:45-10:15 (Morning Slot 2)                          │ │
│ │ 📖 Topic: Introduction to English A1                           │ │
│ │ 👥 Skills: Listening, Reading                                  │
│ │                                                                │ │
│ │ Select Room:                                                   │ │
│ │ ┌──────────────────────────────────────────────────────┐       │ │
│ │ │ Room 203 (Capacity: 20)                          ✓   │       │ │
│ │ └──────────────────────────────────────────────────────┘       │ │
│ │                                                                │ │
│ │ Available Rooms:                                               │ │
│ │ ✅ Room 203 (Cap: 20, Location: Building A, Floor 2)          │ │
│ │ ✅ Room 301 (Cap: 25, Location: Building A, Floor 3)          │ │
│ │ ✅ Room 302 (Cap: 30, Location: Building A, Floor 3)          │ │
│ │ ❌ Room 101 (Cap: 25) - Booked by ENG-B1-02                   │ │
│ │ ❌ Room 201 (Cap: 15) - Insufficient capacity (needs 20)      │ │
│ ├────────────────────────────────────────────────────────────────┤ │
│ │                                                                │ │
│ │ Session 2 - Wednesday, Jan 8, 2025                             │ │
│ │ ⏰ Time: 08:45-10:15 (Morning Slot 2)                          │ │
│ │ 📖 Topic: Basic Greetings                                      │ │
│ │                                                                │ │
│ │ 💡 Using same room as Monday: Room 203 ✓                      │ │
│ ├────────────────────────────────────────────────────────────────┤ │
│ │                                                                │ │
│ │ Session 3 - Friday, Jan 10, 2025                               │ │
│ │ ⏰ Time: 14:45-16:15 (Afternoon Slot 2)                        │ │
│ │ 📖 Topic: Self Introduction                                    │ │
│ │                                                                │ │
│ │ 💡 Using same room: Room 203 ✓                                │ │
│ └────────────────────────────────────────────────────────────────┘ │
│                                                                    │
│ ┌────────────────────────────────────────────────────────────────┐ │
│ │ 🚀 Auto-Propagation Settings                                   │ │
│ │                                                                │ │
│ │ ☑ Apply to all Mondays (12 sessions)                          │ │
│ │ ☑ Apply to all Wednesdays (12 sessions)                       │ │
│ │ ☑ Apply to all Fridays (12 sessions)                          │ │
│ │                                                                │ │
│ │ [Assign Resources with Auto-Propagation] →                    │ │
│ └────────────────────────────────────────────────────────────────┘ │
│                                                                    │
│                    ← [Back]  [Save Progress]                       │
│                                                                    │
└────────────────────────────────────────────────────────────────────┘
```

#### Screen 4.2: Auto-Propagation Progress

```
┌────────────────────────────────────────────┐
│ ⚡ Auto-Assigning Resources...             │
├────────────────────────────────────────────┤
│                                            │
│ ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓░░ 90%                  │
│                                            │
│ ✓ Assigned Room 203 to Monday sessions    │
│ ✓ Assigned Room 203 to Wednesday sessions │
│ ⏳ Assigning to Friday sessions...         │
│                                            │
│ Progress: 33/36 sessions assigned          │
│                                            │
└────────────────────────────────────────────┘
```

#### Screen 4.3: Conflict Report

```
┌────────────────────────────────────────────────────────────────────┐
│ ⚠️ Resource Assignment Results                                     │
├────────────────────────────────────────────────────────────────────┤
│                                                                    │
│ ✅ Successfully Assigned: 33/36 sessions                           │
│ ⚠️ Conflicts Found: 3 sessions need manual resolution             │
│                                                                    │
│ ┌──────────────────────────────────────────────────────────────┐   │
│ │ Conflict Details:                                            │   │
│ ├──────────────────────────────────────────────────────────────┤   │
│ │                                                              │   │
│ │ Session 15 - Monday, Dec 16, 2024                            │   │
│ │ ⏰ Time: 08:45-10:15                                          │   │
│ │ ❌ Reason: Room 203 booked by Class ENG-B1-02                │   │
│ │                                                              │   │
│ │ Alternative Rooms:                                           │   │
│ │ ● Room 301 (Cap: 25) ✅ Available                            │   │
│ │ ● Room 302 (Cap: 30) ✅ Available                            │   │
│ │                                                              │   │
│ │ [Assign Room 301]  [View Alternatives]                       │   │
│ ├──────────────────────────────────────────────────────────────┤   │
│ │                                                              │   │
│ │ Session 22 - Monday, Jan 13, 2025                            │   │
│ │ ⏰ Time: 08:45-10:15                                          │   │
│ │ ❌ Reason: Room 203 under maintenance                        │   │
│ │                                                              │   │
│ │ Alternative Rooms:                                           │   │
│ │ ● Room 201 (Cap: 20) ✅ Available                            │   │
│ │ ● Room 301 (Cap: 25) ✅ Available                            │   │
│ │                                                              │   │
│ │ [Assign Room 201]  [View Alternatives]                       │   │
│ ├──────────────────────────────────────────────────────────────┤   │
│ │                                                              │   │
│ │ Session 28 - Monday, Feb 3, 2025                             │   │
│ │ ⏰ Time: 08:45-10:15                                          │   │
│ │ ❌ Reason: Room 203 booked by Class ENG-A2-01                │   │
│ │                                                              │   │
│ │ Alternative Rooms:                                           │   │
│ │ ● Room 302 (Cap: 30) ✅ Available                            │   │
│ │                                                              │   │
│ │ [Assign Room 302]  [View Alternatives]                       │   │
│ └──────────────────────────────────────────────────────────────┘   │
│                                                                    │
│ [Resolve All Conflicts]  [Continue with Conflicts]  [Cancel]      │
│                                                                    │
└────────────────────────────────────────────────────────────────────┘
```

### 🔗 API Integration

```typescript
export class ClassService {
  async assignResources(
    classId: number,
    request: AssignResourcesRequest
  ): Promise<AssignResourcesResponse> {
    const response = await axios.post<ResponseObject<AssignResourcesResponse>>(
      `/api/v1/classes/${classId}/resources`,
      request
    );
    return response.data.data;
  }

  async getAvailableResources(
    classId: number,
    params: {
      sessionId?: number;
      date?: string;
      timeSlotTemplateId?: number;
    }
  ): Promise<ResourceDTO[]> {
    const response = await axios.get<ResponseObject<ResourceDTO[]>>(
      `/api/v1/classes/${classId}/available-resources`,
      { params }
    );
    return response.data.data;
  }

  async resolveConflict(
    classId: number,
    sessionId: number,
    resourceId: number
  ): Promise<void> {
    await axios.post(
      `/api/v1/classes/${classId}/sessions/${sessionId}/resource`,
      { resourceId }
    );
  }
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
```

---

## STEP 5: Assign Teachers

### 🎯 Objective

Academic Staff assigns teachers with PRE-CHECK availability (v1.1 - shows conflicts BEFORE selection).

### 📊 Backend Implementation

#### 3.5.1 Controller

```java
@GetMapping("/{classId}/available-teachers")
@PreAuthorize("hasRole('ACADEMIC_STAFF')")
public ResponseEntity<ResponseObject<AvailableTeachersResponse>> getAvailableTeachers(
        @PathVariable Long classId,
        @RequestParam(required = false) List<String> skillSet) {

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

#### 3.5.2 Service Implementation (PRE-CHECK)

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

        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));

        // Query teachers with PRE-CHECK (3 conditions checked for ALL sessions)
        List<TeacherAvailabilityDTO> teachers = teacherRepository
                .findAvailableTeachersWithPreCheck(classId, skillSetFilter);

        int totalSessions = sessionRepository.countByClassEntityId(classId);

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
        log.info("Assigning teacher {} to class {}", request.getTeacherId(), classId);

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

        List<RemainingSession> remainingSessions = Collections.emptyList();
        if (needsSubstitute) {
            // Find unassigned sessions
            remainingSessions = sessionRepository.findUnassignedSessions(classId).stream()
                    .map(s -> new RemainingSession(s.getId(), s.getDate(), "No teacher assigned"))
                    .collect(Collectors.toList());
        }

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

#### 3.5.3 Repository Query (PRE-CHECK)

**File:** `TeacherRepository.java`

```java
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
         COALESCE(sc.leave_conflict_count, 0))::numeric / COALESCE(sc.total_sessions, 1) * 100,
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
```

### 🎨 Frontend Screen Design

#### Screen 5.1: Teacher Selection with PRE-CHECK

```
┌────────────────────────────────────────────────────────────────────┐
│ 📘 Class Setup: ENG-A1-2025-01                       Step 5 of 7    │
├────────────────────────────────────────────────────────────────────┤
│                                                                    │
│ 👨‍🏫 Assign Teachers to Sessions                                    │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│                                                                    │
│ System analyzed 36 sessions and checked teacher availability.      │
│ Select teachers with best availability first.                      │
│                                                                    │
│ Filter by skill: [All ▼]  [Listening]  [Speaking]  [Reading]      │
│                                                                    │
│ ┌────────────────────────────────────────────────────────────────┐ │
│ │ ✅ FULLY AVAILABLE (36/36 sessions)                            │ │
│ ├────────────────────────────────────────────────────────────────┤ │
│ │                                                                │ │
│ │ 👤 Jane Doe (T001) - Full-time                                │ │
│ │ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │ │
│ │ 📚 Skills: Listening (Lv3), Reading (Lv5)                     │ │
│ │ ✅ All sessions available                                      │ │
│ │ 📊 Availability: 36/36 (100%)                                 │ │
│ │                                                                │ │
│ │ Conflicts: None ✓                                             │ │
│ │                                                                │ │
│ │          [Assign to All 36 Sessions] ← Recommended            │ │
│ ├────────────────────────────────────────────────────────────────┤ │
│ │                                                                │ │
│ │ 👤 David Smith (T002) - Full-time                             │ │
│ │ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │ │
│ │ 📚 Skills: Listening (Lv4), Reading (Lv4)                     │ │
│ │ ✅ All sessions available                                      │ │
│ │ 📊 Availability: 36/36 (100%)                                 │ │
│ │                                                                │ │
│ │          [Assign to All 36 Sessions]                           │ │
│ └────────────────────────────────────────────────────────────────┘ │
│                                                                    │
│ ┌────────────────────────────────────────────────────────────────┐ │
│ │ ⚠️ PARTIALLY AVAILABLE                                         │ │
│ ├────────────────────────────────────────────────────────────────┤ │
│ │                                                                │ │
│ │ 👤 John Smith (T003) - Full-time                              │ │
│ │ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │ │
│ │ 📚 Skills: Listening (Lv5), Reading (Lv4), Speaking (Lv3)     │ │
│ │ ⚠️ 26/36 sessions available (72.2%)                           │ │
│ │                                                                │ │
│ │ ❌ Conflicts (10 sessions):                                    │ │
│ │   • 5 sessions: No availability registered                    │ │
│ │   • 3 sessions: Teaching ENG-B1-02 at same time              │ │
│ │   • 2 sessions: On approved leave                             │ │
│ │                                                                │ │
│ │ [Assign to 26 Available Sessions] [View Conflict Details]     │ │
│ ├────────────────────────────────────────────────────────────────┤ │
│ │                                                                │ │
│ │ 👤 Bob Wilson (T008) - Part-time                              │ │
│ │ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │ │
│ │ 📚 Skills: General (Lv5) ⭐ Can teach ANY session             │ │
│ │ ⚠️ 18/36 sessions available (50%)                             │ │
│ │                                                                │ │
│ │ ❌ Conflicts (18 sessions):                                    │ │
│ │   • 18 sessions: No Wednesday availability                    │ │
│ │                                                                │ │
│ │ [Assign to 18 Available Sessions] [View Details]              │ │
│ └────────────────────────────────────────────────────────────────┘ │
│                                                                    │
│ 💡 Recommendation: Assign Jane Doe (fully available, high level)   │
│                                                                    │
│                 ← [Back]  [Save Progress]  [Continue] →            │
│                                                                    │
└────────────────────────────────────────────────────────────────────┘
```

#### Screen 5.2: Assignment Success

```
┌────────────────────────────────────────────────────────────────────┐
│ ✅ Teacher Assignment Complete                                     │
├────────────────────────────────────────────────────────────────────┤
│                                                                    │
│ Successfully assigned Jane Doe to all 36 sessions!                 │
│                                                                    │
│ 📊 Assignment Summary:                                             │
│ • Primary Teacher: Jane Doe (T001)                                 │
│ • Sessions Assigned: 36/36 (100%)                                  │
│ • No substitute needed ✓                                           │
│                                                                    │
│ Next step: Validate class completeness                             │
│                                                                    │
│            [View Teaching Schedule]  [Continue Setup] →            │
│                                                                    │
└────────────────────────────────────────────────────────────────────┘
```

#### Screen 5.3: Partial Assignment - Need Substitute

```
┌────────────────────────────────────────────────────────────────────┐
│ ⚠️ Teacher Assignment Incomplete                                   │
├────────────────────────────────────────────────────────────────────┤
│                                                                    │
│ Assigned John Smith to 26/36 sessions                              │
│                                                                    │
│ ❌ 10 sessions still need a teacher:                               │
│                                                                    │
│ ┌──────────────────────────────────────────────────────────────┐   │
│ │ Session 5 - Wed, Jan 15, 2025                                │   │
│ │ Reason: John has no Wednesday availability                   │   │
│ ├──────────────────────────────────────────────────────────────┤   │
│ │ Session 15 - Mon, Dec 16, 2024                               │   │
│ │ Reason: Teaching conflict with ENG-B1-02                     │   │
│ ├──────────────────────────────────────────────────────────────┤   │
│ │ Session 22 - Mon, Jan 13, 2025                               │   │
│ │ Reason: On approved leave                                    │   │
│ └──────────────────────────────────────────────────────────────┘   │
│ ... and 7 more sessions                                            │
│                                                                    │
│ 🔍 Find Substitute Teacher:                                        │
│                                                                    │
│ Available for ALL 10 sessions:                                     │
│ ✅ Jane Doe (36/36 available)                                      │
│    [Quick Assign as Substitute]                                    │
│                                                                    │
│ Available for SOME sessions:                                       │
│ ⚠️ Bob Wilson (Available for 5/10 sessions)                       │
│    [Assign + Find Another Substitute]                              │
│                                                                    │
│        [Find Substitute]  [Continue Without Substitute]            │
│                                                                    │
└────────────────────────────────────────────────────────────────────┘
```

### 🔗 API Integration

```typescript
export class ClassService {
  async getAvailableTeachers(
    classId: number,
    skillSet?: string[]
  ): Promise<AvailableTeachersResponse> {
    const response = await axios.get<ResponseObject<AvailableTeachersResponse>>(
      `/api/v1/classes/${classId}/available-teachers`,
      {
        params: { skillSet }
      }
    );
    return response.data.data;
  }

  async assignTeacher(
    classId: number,
    request: AssignTeacherRequest
  ): Promise<AssignTeacherResponse> {
    const response = await axios.post<ResponseObject<AssignTeacherResponse>>(
      `/api/v1/classes/${classId}/teachers`,
      request
    );
    return response.data.data;
  }
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

    List<Session> sessions = createTestSessions(36);
    when(sessionRepository.findSessionsMatchingTeacherSkills(classId, 5L))
            .thenReturn(sessions);

    // When
    AssignTeacherResponse response = teacherAssignmentService.assignTeacher(classId, request);

    // Then
    assertThat(response.getAssignedCount()).isEqualTo(36);
    assertThat(response.isNeedsSubstitute()).isFalse();
    verify(teachingSlotRepository).saveAll(argThat(slots -> slots.size() == 36));
}
```

---

## STEP 6: Validate

### 🎯 Objective

System validates class completeness before submission (all sessions have time slots, resources, teachers).

### 📊 Backend Implementation

#### 3.6.1 Controller

```java
@PostMapping("/{classId}/validate")
@PreAuthorize("hasRole('ACADEMIC_STAFF')")
public ResponseEntity<ResponseObject<ValidateClassResponse>> validateClass(
        @PathVariable Long classId) {

    ValidateClassResponse response = classService.validateClass(classId);

    String message = response.isValid()
            ? "Class is valid and ready for submission"
            : "Class has incomplete assignments";

    return ResponseEntity.ok(ResponseObject.success(message, response));
}
```

#### 3.6.2 Service Implementation

```java
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
        errors.add(String.format("%d sessions missing teachers (Sessions: %s)",
                sessionsWithoutTeacher,
                getSessionIdsWithoutTeachers(classId)));
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

### 🎨 Frontend Screen Design

#### Screen 6.1: Validation Success

```
┌────────────────────────────────────────────────────────────────────┐
│ 📘 Class Setup: ENG-A1-2025-01                       Step 6 of 7    │
├────────────────────────────────────────────────────────────────────┤
│                                                                    │
│ ✅ Validation Complete                                             │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│                                                                    │
│ Your class setup is complete and ready for submission!             │
│                                                                    │
│ ┌──────────────────────────────────────────────────────────────┐   │
│ │ 📊 Setup Progress                                            │   │
│ │                                                              │   │
│ │ ✅ Step 1: Class Created                                     │   │
│ │ ✅ Step 2: 36 Sessions Generated                             │   │
│ │ ✅ Step 3: Time Slots Assigned                               │   │
│ │    • Monday: 08:45-10:15 (12 sessions)                       │   │
│ │    • Wednesday: 08:45-10:15 (12 sessions)                    │   │
│ │    • Friday: 14:45-16:15 (12 sessions)                       │   │
│ │                                                              │   │
│ │ ✅ Step 4: Resources Assigned                                │   │
│ │    • Room 203: 33 sessions                                   │   │
│ │    • Room 301: 2 sessions (conflict resolution)              │   │
│ │    • Room 201: 1 session (conflict resolution)               │   │
│ │    • 100% coverage ✅                                        │   │
│ │                                                              │   │
│ │ ✅ Step 5: Teachers Assigned                                 │   │
│ │    • Jane Doe (Listening/Reading): 36 sessions               │   │
│ │    • 100% coverage ✅                                        │   │
│ │                                                              │   │
│ │ ✅ Step 6: Validation Passed                                 │   │
│ │    All requirements met ✅                                   │   │
│ └──────────────────────────────────────────────────────────────┘   │
│                                                                    │
│ ┌──────────────────────────────────────────────────────────────┐   │
│ │ ⚠️ Warnings (non-blocking):                                  │   │
│ │ • Using alternative rooms for 3 sessions (conflict res.)     │   │
│ └──────────────────────────────────────────────────────────────┘   │
│                                                                    │
│ ┌──────────────────────────────────────────────────────────────┐   │
│ │ 📅 Class Summary:                                            │   │
│ │ • Code: ENG-A1-2025-01                                       │   │
│ │ • Branch: Main Campus                                        │   │
│ │ • Course: English A1 Foundation                              │   │
│ │ • Duration: 12 weeks (Jan 6 - Mar 28, 2025)                  │   │
│ │ • Sessions: 36 (Mon/Wed/Fri)                                 │   │
│ │ • Capacity: 20 students                                      │   │
│ │ • Status: Ready for Approval ✅                              │   │
│ └──────────────────────────────────────────────────────────────┘   │
│                                                                    │
│        ← [Back]  [View Schedule]  [Submit for Approval] →          │
│                                                                    │
└────────────────────────────────────────────────────────────────────┘
```

#### Screen 6.2: Validation Failed

```
┌────────────────────────────────────────────────────────────────────┐
│ ❌ Validation Failed                                               │
├────────────────────────────────────────────────────────────────────┤
│                                                                    │
│ Cannot submit - class has incomplete assignments                   │
│                                                                    │
│ ┌──────────────────────────────────────────────────────────────┐   │
│ │ ❌ Errors Found (Must Fix):                                  │   │
│ │                                                              │   │
│ │ 1. 3 sessions missing teachers                               │   │
│ │    Sessions: 15, 22, 28                                      │   │
│ │    [Go to Step 5 - Assign Teachers]                          │   │
│ │                                                              │   │
│ │ 2. 2 sessions missing resources                              │   │
│ │    Sessions: 18, 25                                          │   │
│ │    [Go to Step 4 - Assign Resources]                         │   │
│ └──────────────────────────────────────────────────────────────┘   │
│                                                                    │
│ ┌──────────────────────────────────────────────────────────────┐   │
│ │ ✅ Completed Steps:                                          │   │
│ │ • Time slots: 36/36 sessions ✓                               │   │
│ │ • Resources: 34/36 sessions (94%)                            │   │
│ │ • Teachers: 33/36 sessions (92%)                             │   │
│ └──────────────────────────────────────────────────────────────┘   │
│                                                                    │
│            ← [Back]  [Fix Issues]  [Save as Draft]                 │
│                                                                    │
└────────────────────────────────────────────────────────────────────┘
```

### 🔗 API Integration

```typescript
export class ClassService {
  async validateClass(classId: number): Promise<ValidateClassResponse> {
    const response = await axios.post<ResponseObject<ValidateClassResponse>>(
      `/api/v1/classes/${classId}/validate`
    );
    return response.data.data;
  }
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

Academic Staff submits class for Center Head approval. Center Head approves/rejects with reason.

### 📊 Backend Implementation

#### 3.7.1 Controller

```java
@PostMapping("/{classId}/submit")
@PreAuthorize("hasRole('ACADEMIC_STAFF')")
public ResponseEntity<ResponseObject<SubmitClassResponse>> submitClass(
        @PathVariable Long classId,
        @AuthenticationPrincipal UserPrincipal userPrincipal) {

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

#### 3.7.2 Service Implementation

```java
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

    // 4. Notify Center Head (via notification system)
    notificationService.notifyClassSubmission(classEntity);

    return SubmitClassResponse.builder()
            .classId(classEntity.getId())
            .code(classEntity.getCode())
            .status(classEntity.getStatus())
            .approvalStatus(classEntity.getApprovalStatus())
            .submittedAt(classEntity.getSubmittedAt())
            .submittedBy(getUserFullName(userId))
            .build();
}

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

    // Notify Academic Staff
    notificationService.notifyClassApproved(classEntity);

    return ApproveClassResponse.builder()
            .classId(classEntity.getId())
            .code(classEntity.getCode())
            .status(classEntity.getStatus())
            .approvalStatus(classEntity.getApprovalStatus())
            .approvedBy(getUserFullName(userId))
            .approvedAt(classEntity.getApprovedAt())
            .build();
}

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

    // Notify Academic Staff
    notificationService.notifyClassRejected(classEntity, reason);

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
```

### 🎨 Frontend Screen Design

#### Screen 7.1: Submit Confirmation (Academic Staff)

```
┌────────────────────────────────────────────────────────────────────┐
│ 📋 Submit Class for Approval                                       │
├────────────────────────────────────────────────────────────────────┤
│                                                                    │
│ You are about to submit this class to Center Head for approval.    │
│                                                                    │
│ ┌──────────────────────────────────────────────────────────────┐   │
│ │ Class: ENG-A1-2025-01                                        │   │
│ │ Branch: Main Campus                                          │   │
│ │ Course: English A1 Foundation                                │   │
│ │ Duration: Jan 6 - Mar 28, 2025 (12 weeks)                    │   │
│ │ Sessions: 36 (Mon/Wed/Fri)                                   │   │
│ │ Capacity: 20 students                                        │   │
│ └──────────────────────────────────────────────────────────────┘   │
│                                                                    │
│ ✅ All requirements met:                                           │
│ • 36/36 sessions have time slots                                   │
│ • 36/36 sessions have resources                                    │
│ • 36/36 sessions have teachers                                     │
│                                                                    │
│ After submission:                                                  │
│ • You will be notified when Center Head reviews                    │
│ • You can track approval status                                    │
│ • If approved, class will be ready for enrollment                  │
│ • If rejected, you can fix issues and resubmit                     │
│                                                                    │
│              [Cancel]  [Submit for Approval] →                     │
│                                                                    │
└────────────────────────────────────────────────────────────────────┘
```

#### Screen 7.2: Submission Success

```
┌────────────────────────────────────────────────┐
│ ✅ Class Submitted Successfully!               │
├────────────────────────────────────────────────┤
│                                                │
│ Your class "ENG-A1-2025-01" has been          │
│ submitted to Center Head for approval.         │
│                                                │
│ 📧 Notification sent to:                       │
│ • Dr. John (Center Head - Main Campus)         │
│                                                │
│ 📊 Status: Pending Approval                    │
│                                                │
│ You will receive notification when reviewed.   │
│                                                │
│    [View Class]  [Track Status]  [Done]        │
│                                                │
└────────────────────────────────────────────────┘
```

#### Screen 7.3: Center Head Approval Screen

```
┌────────────────────────────────────────────────────────────────────┐
│ 📋 Class Approval Request                                          │
├────────────────────────────────────────────────────────────────────┤
│                                                                    │
│ ⚠️ Review carefully before approval                                │
│                                                                    │
│ ┌──────────────────────────────────────────────────────────────┐   │
│ │ 📘 Basic Information                                         │   │
│ ├──────────────────────────────────────────────────────────────┤   │
│ │ Class Code: ENG-A1-2025-01                                   │   │
│ │ Branch: Main Campus                                          │   │
│ │ Course: English A1 Foundation                                │   │
│ │ Modality: OFFLINE                                            │   │
│ │ Start Date: Jan 6, 2025                                      │   │
│ │ Schedule: Monday, Wednesday, Friday                          │   │
│ │ Capacity: 20 students                                        │   │
│ │                                                              │   │
│ │ Submitted by: Alice (Academic Staff)                         │   │
│ │ Submitted at: Jan 3, 2025 2:30 PM                            │   │
│ └──────────────────────────────────────────────────────────────┘   │
│                                                                    │
│ ┌──────────────────────────────────────────────────────────────┐   │
│ │ 📊 Sessions Summary                                          │   │
│ ├──────────────────────────────────────────────────────────────┤   │
│ │ Total: 36 sessions                                           │   │
│ │ Duration: Jan 6 - Mar 28, 2025 (12 weeks)                    │   │
│ │                                                              │   │
│ │ Time Slots:                                                  │   │
│ │ • Monday: 08:45-10:15 (12 sessions)                          │   │
│ │ • Wednesday: 08:45-10:15 (12 sessions)                       │   │
│ │ • Friday: 14:45-16:15 (12 sessions)                          │   │
│ └──────────────────────────────────────────────────────────────┘   │
│                                                                    │
│ ┌──────────────────────────────────────────────────────────────┐   │
│ │ 🏫 Resource Assignment                                       │   │
│ ├──────────────────────────────────────────────────────────────┤   │
│ │ • Room 203: 33 sessions                                      │   │
│ │ • Room 301: 2 sessions (Dec 16, Jan 13)                      │   │
│ │ • Room 201: 1 session (Feb 3)                                │   │
│ │ 100% coverage ✅                                             │   │
│ └──────────────────────────────────────────────────────────────┘   │
│                                                                    │
│ ┌──────────────────────────────────────────────────────────────┐   │
│ │ 👨‍🏫 Teacher Assignment                                        │   │
│ ├──────────────────────────────────────────────────────────────┤   │
│ │ • Jane Doe (T001): 36 sessions (Listening, Reading)          │   │
│ │ 100% coverage ✅                                             │   │
│ └──────────────────────────────────────────────────────────────┘   │
│                                                                    │
│ ┌──────────────────────────────────────────────────────────────┐   │
│ │ ⚠️ Warnings:                                                 │   │
│ │ • 3 sessions using alternative rooms (conflict resolution)   │   │
│ └──────────────────────────────────────────────────────────────┘   │
│                                                                    │
│ [View Detailed Schedule]  [View Session List]                      │
│                                                                    │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│                                                                    │
│ Decision:                                                          │
│                                                                    │
│ ○ Approve - Class is ready for student enrollment                 │
│ ○ Reject - Send back to Academic Staff with feedback              │
│                                                                    │
│ Rejection Reason (required if rejecting):                          │
│ ┌──────────────────────────────────────────────────────────────┐   │
│ │                                                              │   │
│ │                                                              │   │
│ │                                                              │   │
│ └──────────────────────────────────────────────────────────────┘   │
│ Min 10 characters                                                  │
│                                                                    │
│                [Cancel]  [Submit Decision] →                       │
│                                                                    │
└────────────────────────────────────────────────────────────────────┘
```

#### Screen 7.4: Approval Success

```
┌────────────────────────────────────────────┐
│ ✅ Class Approved Successfully!            │
├────────────────────────────────────────────┤
│                                            │
│ Class "ENG-A1-2025-01" is now scheduled    │
│ and ready for student enrollment.          │
│                                            │
│ 📊 Updated Status: SCHEDULED               │
│ 📅 Approved at: Jan 3, 2025 4:00 PM        │
│                                            │
│ 📧 Notification sent to:                   │
│ • Alice (Academic Staff)                   │
│                                            │
│    [View Class]  [Approve Next]  [Done]    │
│                                            │
└────────────────────────────────────────────┘
```

#### Screen 7.5: Rejection Success

```
┌────────────────────────────────────────────┐
│ 📨 Class Rejected & Feedback Sent          │
├────────────────────────────────────────────┤
│                                            │
│ Class "ENG-A1-2025-01" has been rejected   │
│ and sent back to Academic Staff.           │
│                                            │
│ 📝 Rejection Reason:                       │
│ "Time slot conflicts with another class.   │
│ Please use different time slots for        │
│ Wednesday sessions."                       │
│                                            │
│ 📊 Updated Status: DRAFT                   │
│ 📧 Notification sent to Academic Staff     │
│                                            │
│    [View Class]  [Review Next]  [Done]     │
│                                            │
└────────────────────────────────────────────┘
```

### 🔗 API Integration

```typescript
export class ClassService {
  async submitClass(classId: number): Promise<SubmitClassResponse> {
    const response = await axios.post<ResponseObject<SubmitClassResponse>>(
      `/api/v1/classes/${classId}/submit`
    );
    return response.data.data;
  }

  async approveClass(classId: number): Promise<ApproveClassResponse> {
    const response = await axios.post<ResponseObject<ApproveClassResponse>>(
      `/api/v1/classes/${classId}/approve`
    );
    return response.data.data;
  }

  async rejectClass(
    classId: number,
    reason: string
  ): Promise<RejectClassResponse> {
    const response = await axios.post<ResponseObject<RejectClassResponse>>(
      `/api/v1/classes/${classId}/reject`,
      { reason }
    );
    return response.data.data;
  }
}
```

### ✅ Testing

```java
@Test
void shouldSubmitClassSuccessfully() {
    // Given
    Long classId = 101L;
    Long userId = 1L;
    ClassEntity classEntity = createCompleteClass();

    when(classRepository.findById(classId)).thenReturn(Optional.of(classEntity));
    when(sessionRepository.countByClassEntityIdAndTimeSlotTemplateIsNull(classId)).thenReturn(0);
    // ... all validation passes

    // When
    SubmitClassResponse response = classService.submitClass(classId, userId);

    // Then
    assertThat(response.getSubmittedAt()).isNotNull();
    verify(notificationService).notifyClassSubmission(classEntity);
}

@Test
void shouldApproveClassSuccessfully() {
    // Given
    Long classId = 101L;
    Long userId = 2L;
    ClassEntity classEntity = createSubmittedClass();

    when(classRepository.findById(classId)).thenReturn(Optional.of(classEntity));

    // When
    ApproveClassResponse response = classService.approveClass(classId, userId);

    // Then
    assertThat(response.getStatus()).isEqualTo(ClassStatus.SCHEDULED);
    assertThat(response.getApprovalStatus()).isEqualTo(ApprovalStatus.APPROVED);
    verify(notificationService).notifyClassApproved(classEntity);
}

@Test
void shouldRejectClassWithReason() {
    // Given
    Long classId = 101L;
    Long userId = 2L;
    String reason = "Time slot conflicts with another class";
    ClassEntity classEntity = createSubmittedClass();

    when(classRepository.findById(classId)).thenReturn(Optional.of(classEntity));

    // When
    RejectClassResponse response = classService.rejectClass(classId, reason, userId);

    // Then
    assertThat(response.getStatus()).isEqualTo(ClassStatus.DRAFT);
    assertThat(response.getApprovalStatus()).isEqualTo(ApprovalStatus.REJECTED);
    assertThat(response.getRejectionReason()).isEqualTo(reason);
    assertThat(classEntity.getSubmittedAt()).isNull(); // Reset
    verify(notificationService).notifyClassRejected(classEntity, reason);
}
```

---

## 4. TESTING STRATEGY

### 4.1 Unit Testing

**Coverage Target:** 90%+ for service layer

**Test Categories:**

1. **Service Layer Tests**
   - Business logic validation
   - Error handling
   - State transitions
   - Edge cases

2. **Repository Tests**
   - Custom queries
   - Complex joins
   - Native SQL queries

3. **DTO Validation Tests**
   - Bean validation annotations
   - Custom validators

**Test Framework:**
- JUnit 5
- Mockito / MockitoBean
- AssertJ for assertions
- TestContainers for integration tests

### 4.2 Integration Testing

**Coverage Target:** 80%+

**Test Scenarios:**

1. **End-to-End Workflow**
   - Create class → Generate sessions → Assign all → Submit → Approve
   - Verify database state at each step

2. **Conflict Detection**
   - Resource conflicts (double-booking)
   - Teacher conflicts (teaching two classes)
   - Capacity overflow

3. **Validation Rules**
   - Start date in schedule days
   - Unique class codes
   - Course approval status

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
- All endpoints (CRUD operations)
- Authentication & authorization
- Error responses (4xx, 5xx)
- Pagination & filtering

### 4.4 Frontend Testing (Reference)

**Unit Tests:**
- Component rendering
- User interactions
- Form validation
- State management

**Integration Tests:**
- API integration
- Navigation flows
- Error handling

**E2E Tests:**
- Complete workflow scenarios
- Cross-browser testing

---

## 5. DEPLOYMENT PLAN

### 5.1 Pre-Deployment Checklist

- [ ] All unit tests passing (90%+ coverage)
- [ ] Integration tests passing (80%+ coverage)
- [ ] API documentation updated (OpenAPI spec)
- [ ] Database migration scripts ready
- [ ] Environment variables configured
- [ ] Performance testing completed
- [ ] Security audit completed

### 5.2 Database Migration

**Migration Scripts:**

```sql
-- V1__create_class_workflow_tables.sql
CREATE TABLE IF NOT EXISTS class (
  id BIGSERIAL PRIMARY KEY,
  branch_id BIGINT NOT NULL REFERENCES branch(id),
  course_id BIGINT NOT NULL REFERENCES course(id),
  code VARCHAR(50) NOT NULL,
  name VARCHAR(255),
  modality modality_enum NOT NULL,
  start_date DATE NOT NULL,
  schedule_days SMALLINT[] NOT NULL,
  max_capacity INTEGER NOT NULL,
  status class_status_enum DEFAULT 'DRAFT',
  approval_status approval_status_enum DEFAULT 'PENDING',
  submitted_at TIMESTAMP WITH TIME ZONE,
  approved_by BIGINT REFERENCES user_account(id),
  approved_at TIMESTAMP WITH TIME ZONE,
  rejection_reason TEXT,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (branch_id, code)
);

-- Additional tables...
```

### 5.3 Deployment Steps

**Backend:**
1. Build JAR: `mvn clean package`
2. Run database migrations
3. Deploy to staging
4. Smoke tests
5. Deploy to production
6. Monitor logs

**Frontend:**
1. Build optimized bundle
2. Deploy to CDN
3. Update API endpoints
4. Test in staging
5. Production deployment

### 5.4 Rollback Plan

- Database backup before migration
- Previous version Docker image ready
- Feature flags for gradual rollout
- Monitoring alerts configured

---

## 6. TIMELINE & MILESTONES

### 6.1 Development Timeline (4 weeks)

**Week 1: Foundation (Steps 1-2)**
- Day 1-2: Create Class API & Service
- Day 3-4: Session Generation Logic
- Day 5: Unit tests & integration tests

**Week 2: Assignment Logic (Steps 3-4)**
- Day 6-7: Time Slot Assignment
- Day 8-9: Resource Assignment (HYBRID)
- Day 10: Testing & optimization

**Week 3: Teacher & Validation (Steps 5-6)**
- Day 11-13: Teacher Assignment (PRE-CHECK)
- Day 14: Validation Logic
- Day 15: Testing & bug fixes

**Week 4: Approval & Polish (Step 7)**
- Day 16-17: Submit & Approve workflow
- Day 18: Integration testing
- Day 19: Performance testing
- Day 20: Documentation & deployment

### 6.2 Milestones

| Milestone | Deliverable | Date |
|-----------|-------------|------|
| M1 | Steps 1-2 complete with tests | Week 1 |
| M2 | Steps 3-4 complete with tests | Week 2 |
| M3 | Steps 5-6 complete with tests | Week 3 |
| M4 | Step 7 complete, E2E tested | Week 4 |
| M5 | Production deployment | Week 4 |

### 6.3 Success Criteria

- ✅ All 7 steps implemented and tested
- ✅ 90%+ unit test coverage
- ✅ 80%+ integration test coverage
- ✅ API response time < 500ms (p95)
- ✅ Zero critical bugs in staging
- ✅ Documentation complete
- ✅ Stakeholder sign-off

---

## 7. APPENDIX

### 7.1 Glossary

| Term | Definition |
|------|------------|
| **PRE-CHECK** | v1.1 enhancement where availability is checked BEFORE user selection |
| **HYBRID Approach** | Combination of SQL bulk operations + Java analysis |
| **Session** | A single class meeting (e.g., Monday Jan 6, 08:45-10:15) |
| **Course Session** | Template for session content from course |
| **Time Slot Template** | Pre-defined time range (e.g., "Morning Slot 2") |
| **Teaching Slot** | Teacher-to-session assignment record |
| **Session Resource** | Resource-to-session assignment (room/Zoom) |

### 7.2 Related Documents

- [PRD](/docs/prd.md)
- [Business Flow & Use Cases](/docs/business-flow-usecase.md)
- [Workflow Details](/docs/create-class/create-class-workflow-final.md)
- [OpenAPI Specification](/docs/create-class/openapi.yaml)

### 7.3 Contact & Support

**Development Team:**
- Backend Lead: [TBD]
- Frontend Lead: [TBD]
- QA Lead: [TBD]

**Stakeholders:**
- Product Owner: [TBD]
- Academic Staff Representative: [TBD]
- Center Head Representative: [TBD]

---

**Document Status:** ✅ Ready for Implementation
**Last Updated:** January 4, 2025
**Version:** 1.1.0
