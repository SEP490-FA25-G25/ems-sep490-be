# ACADEMIC AFFAIRS WORKFLOW IMPLEMENTATION PLAN

**Version:** 1.0
**Last Updated:** 2025-11-02
**Author:** Technical Team
**Focus:** Academic Affairs (Giáo vụ) Workflow Implementation

---

## MỤC LỤC

1. [Context & Background](#1-context--background)
2. [Current Implementation Status](#2-current-implementation-status)
3. [Academic Affairs User Workflow](#3-academic-affairs-user-workflow)
4. [Implementation Plan](#4-implementation-plan)
5. [Screen-by-Screen Requirements](#5-screen-by-screen-requirements)
6. [API Endpoints Specification](#6-api-endpoints-specification)
7. [Database Queries & Business Logic](#7-database-queries--business-logic)
8. [Implementation Phases](#8-implementation-phases)
9. [Testing Strategy](#9-testing-strategy)
10. [Dependencies & Risks](#10-dependencies--risks)

---

## 1. CONTEXT & BACKGROUND

### 1.1 Current Situation
- **Enrollment backend is 85% complete** with robust Excel import functionality
- **Missing UI workflow**: Academic Affairs users cannot access the enrollment system
- **Critical gap**: No way to view classes, see current students, or navigate to enrollment
- **Business impact**: Enrollment process cannot be used despite having complete backend logic

### 1.2 The Academic Affairs User Role
- **Primary responsibility**: Student enrollment management into scheduled classes
- **Workflow trigger**: Receives Excel files from Sale team with grouped students by class
- **Daily tasks**:
  - View list of scheduled classes in their branch
  - Check class details and current enrollment status
  - Import Excel files to enroll students
  - Monitor enrollment capacity and make override decisions
  - View branch student database

### 1.3 Business Process Alignment
The implementation must align with the **Excel-based enrollment workflow** documented in `enrollment-implementation-guide.md`:

```
Sale (Offline) → Excel Files → Academic Affairs (Online) → Import Excel → System Auto-Process
```

---

## 2. CURRENT IMPLEMENTATION STATUS

### 2.1 ✅ ALREADY IMPLEMENTED

#### Complete Backend Enrollment System:
- **EnrollmentController**: 2 endpoints for Excel import (preview + execute)
- **EnrollmentServiceImpl**: Complete business logic with capacity management
- **ExcelParserServiceImpl**: Robust Excel file parsing with error handling
- **9 DTOs and Enums**: Complete data transfer objects
- **Updated Enrollment Entity**: Capacity override fields with audit trail
- **Repository Methods**: Custom queries with pessimistic locking
- **21 Comprehensive Unit Tests**: All passing

#### Key Features Available:
- Excel import with preview functionality
- Student resolution (code → email → create new)
- Capacity validation and override with audit
- 3 enrollment strategies (ALL/PARTIAL/OVERRIDE)
- Auto-generation of student sessions
- Transaction management and error handling

### 2.2 ❌ MISSING CRITICAL COMPONENTS

#### No Access Points for Academic Affairs:
- **ClassController**: Cannot view list of classes
- **StudentController**: Cannot view student database
- **Navigation endpoints**: Cannot reach existing enrollment functionality

#### The Paradox:
> **"We have a Ferrari (enrollment backend) but no garage door (UI access) to drive it out"**

---

## 3. ACADEMIC AFFAIRS USER WORKFLOW

### 3.1 Complete User Journey

```
┌─────────────────────────────────────────────────────────────────┐
│ SCREEN 1: LOGIN → CLASS LIST                                   │
├─────────────────────────────────────────────────────────────────┤
│ Academic Affairs logs in → Views Class List                    │
│ Filters: Branch (auto), Status=Scheduled, Course, Modality     │
│ Display: Class code, name, current/max enrolled, start date    │
│ Action: Click "View Details" for specific class                │
└─────────────────────────────────────────────────────────────────┘
                                ↓ Click Class
┌─────────────────────────────────────────────────────────────────┐
│ SCREEN 2: CLASS DETAIL + CURRENT STUDENTS                      │
├─────────────────────────────────────────────────────────────────┤
│ View: Class information, capacity (15/20), schedule            │
│ View: Current enrolled students table                          │
│ Action: "Enroll Students" button (PRIMARY action)              │
└─────────────────────────────────────────────────────────────────┘
                                ↓ Click Enroll Students
┌─────────────────────────────────────────────────────────────────┐
│ SCREEN 3: ENROLLMENT MODAL (3 TABS)                           │
├─────────────────────────────────────────────────────────────────┤
│ Tab 1: Select Existing Students (rare use)                     │
│ Tab 2: Add Single Student (emergency use)                      │
│ Tab 3: Import Excel ⭐ PRIMARY METHOD                          │
│ Action: Upload Excel file from Sale team                       │
└─────────────────────────────────────────────────────────────────┘
                                ↓ Upload & Preview
┌─────────────────────────────────────────────────────────────────┐
│ SCREEN 4: PREVIEW & CAPACITY ANALYSIS                          │
├─────────────────────────────────────────────────────────────────┤
│ Display: Parsed students with status (FOUND/CREATE/ERROR)      │
│ Display: Capacity analysis and warnings                        │
│ Display: System recommendation (OK/PARTIAL/OVERRIDE)           │
│ Action: Choose enrollment strategy + provide reason if needed  │
└─────────────────────────────────────────────────────────────────┘
                                ↓ Confirm Enrollment
┌─────────────────────────────────────────────────────────────────┐
│ SCREEN 5: PROCESSING & CONFIRMATION                           │
├─────────────────────────────────────────────────────────────────┤
│ System: Creates students, enrollments, student sessions         │
│ Display: Success message with summary statistics                │
│ Action: Return to class detail or class list                   │
└─────────────────────────────────────────────────────────────────┘
```

### 3.2 Alternative Workflow: Branch Student Management

```
┌─────────────────────────────────────────────────────────────────┐
│ ALTERNATIVE WORKFLOW: BRANCH STUDENTS                          │
├─────────────────────────────────────────────────────────────────┤
│ From Navigation → View Branch Students                         │
│ Display: All students in academic affairs' branch              │
│ Filters: Search by code/name/email, level, enrollment status   │
│ Action: View student details, enrollment history               │
└─────────────────────────────────────────────────────────────────┘
```

### 3.3 Key User Experience Requirements

1. **Clear Visual Indicators**:
   - Capacity status: "15/20 enrolled" with color coding
   - Class status badges: ✅ SCHEDULED, 🟡 DRAFT, ❌ CANCELLED
   - Student status badges: ✅ ENROLLED, 🆕 NEW, ❌ ERROR

2. **Contextual Information**:
   - Always show current enrollment count
   - Show available slots before enrollment
   - Display warnings before capacity override

3. **Efficient Navigation**:
   - Quick filters for common scenarios
   - Breadcrumb navigation
   - Quick actions from list view

---

## 4. IMPLEMENTATION PLAN

### 4.1 Phase 1: Core Workflow (Class Management)
**Priority**: CRITICAL - Enables access to existing enrollment system

#### 4.1.1 ClassController Implementation
```java
@RestController
@RequestMapping("/api/v1/classes")
@PreAuthorize("hasRole('ACADEMIC_STAFF')")  // Note: Fix role name from bug
public class ClassController {

    // GET /api/v1/classes - List classes for academic affairs
    @GetMapping
    public Page<ClassListItemDTO> getClasses(
        @RequestParam(required = false) Long branchId,
        @RequestParam(required = false) Long courseId,
        @RequestParam(required = false) ClassStatus status,
        @RequestParam(required = false) Modality modality,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "startDate") String sort,
        @AuthenticationPrincipal UserPrincipal currentUser
    );

    // GET /api/v1/classes/{id} - Class details with enrollment summary
    @GetMapping("/{id}")
    public ClassDetailDTO getClassDetail(@PathVariable Long id);

    // GET /api/v1/classes/{id}/students - Current enrolled students
    @GetMapping("/{id}/students")
    public Page<ClassStudentDTO> getClassStudents(
        @PathVariable Long id,
        @RequestParam(required = false) String search,
        Pageable pageable
    );

    // GET /api/v1/classes/{id}/summary - Quick enrollment summary
    @GetMapping("/{id}/summary")
    public ClassEnrollmentSummaryDTO getClassEnrollmentSummary(@PathVariable Long id);
}
```

#### 4.1.2 Required DTOs
```java
// List view - compact information
@Data
@Builder
public class ClassListItemDTO {
    private Long id;
    private String code;
    private String name;
    private String courseName;
    private String branchName;
    private Modality modality;
    private LocalDate startDate;
    private ClassStatus status;
    private ApprovalStatus approvalStatus;
    private Integer currentEnrolled;
    private Integer maxCapacity;
    private Integer availableSlots;
    private String teacherName;
    private String scheduleSummary;
}

// Detail view - comprehensive information
@Data
@Builder
public class ClassDetailDTO {
    private Long id;
    private String code;
    private String name;
    private CourseDTO course;
    private BranchDTO branch;
    private Modality modality;
    private LocalDate startDate;
    private LocalDate plannedEndDate;
    private Short[] scheduleDays;
    private Integer maxCapacity;
    private ClassStatus status;
    private ApprovalStatus approvalStatus;
    private String room;
    private String teacherName;
    private String scheduleSummary;
    private List<SessionDTO> upcomingSessions;
    private EnrollmentSummary enrollmentSummary;
}

// Students in class
@Data
@Builder
public class ClassStudentDTO {
    private Long id;
    private String studentCode;
    private String fullName;
    private String email;
    private String phone;
    private String level;
    private OffsetDateTime enrolledAt;
    private String enrolledBy;
    private EnrollmentStatus status;
}

// Quick summary for class detail page
@Data
@Builder
public class ClassEnrollmentSummaryDTO {
    private Long classId;
    private String classCode;
    private Integer currentEnrolled;
    private Integer maxCapacity;
    private Integer availableSlots;
    private Double utilizationRate;
    private Boolean canEnrollStudents;
    private String enrollmentRestrictionReason;
}
```

### 4.2 Phase 2: Student Management
**Priority**: HIGH - Complete academic affairs workflow

#### 4.2.1 StudentController Implementation
```java
@RestController
@RequestMapping("/api/v1/students")
@PreAuthorize("hasRole('ACADEMIC_STAFF')")
public class StudentController {

    // GET /api/v1/students - Branch students with filters
    @GetMapping
    public Page<StudentListItemDTO> getStudents(
        @RequestParam(required = false) Long branchId,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) String level,
        @RequestParam(required = false) EnrollmentStatus enrollmentStatus,
        @RequestParam(required = false) Long courseId,
        Pageable pageable,
        @AuthenticationPrincipal UserPrincipal currentUser
    );

    // GET /api/v1/students/{id} - Student details
    @GetMapping("/{id}")
    public StudentDetailDTO getStudentDetail(@PathVariable Long id);

    // GET /api/v1/students/search - Quick search for enrollment
    @GetMapping("/search")
    public List<StudentSearchResultDTO> searchStudents(
        @RequestParam String query,
        @RequestParam(required = false) Long branchId
    );

    // GET /api/v1/students/{id}/enrollments - Student enrollment history
    @GetMapping("/{id}/enrollments")
    public Page<StudentEnrollmentDTO> getStudentEnrollments(
        @PathVariable Long id,
        Pageable pageable
    );
}
```

#### 4.2.2 Required DTOs
```java
// Student list view
@Data
@Builder
public class StudentListItemDTO {
    private Long id;
    private String studentCode;
    private String fullName;
    private String email;
    private String phone;
    private String level;
    private String branchName;
    private Integer activeEnrollments;
    private LocalDate lastEnrollmentDate;
    private UserStatus status;
}

// Student detail view
@Data
@Builder
public class StudentDetailDTO {
    private Long id;
    private String studentCode;
    private UserAccountDTO userAccount;
    private String level;
    private String branchName;
    private List<StudentEnrollmentDTO> enrollmentHistory;
    private StudentStatisticsDTO statistics;
}

// Quick search results (for enrollment modal)
@Data
@Builder
public class StudentSearchResultDTO {
    private Long id;
    private String studentCode;
    private String fullName;
    private String email;
    private String level;
    private String branchName;
    private Boolean canEnroll; // Based on existing enrollments
    private String restrictionReason;
}
```

### 4.3 Phase 3: Enhanced Features (Future)
**Priority**: MEDIUM - Nice to have

- GET /api/v1/enrollments/class/{id}/summary - Enrollment history
- GET /api/v1/reports/branch-enrollment - Branch-level reports
- PUT /api/v1/enrollments/{id}/status - Update enrollment status
- DELETE /api/v1/enrollments/{id} - Remove enrollment

---

## 5. SCREEN-BY-SCREEN REQUIREMENTS

### 5.1 Screen 1: Class List Page

#### URL: `/classes` (or dashboard with class list widget)

#### Layout Requirements:
```
┌─────────────────────────────────────────────────────────────────┐
│ 📚 Classes Management - Academic Affairs                         │
├─────────────────────────────────────────────────────────────────┤
│ Filters:                                                        │
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │ Branch: [My Branch ▼] Status: [Scheduled ▼]                │ │
│ │ Course: [All Courses ▼] Modality: [All ▼]                  │ │
│ │ Search: [__________________] [🔍 Search]                    │ │
│ └─────────────────────────────────────────────────────────────┘ │
│                                                                 │
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │ Class Code    | Name              | Course    | Enrolled |    │ │
│ │ ENG-A1-001    | Basic English A1  | English   | 15/20     │    │ │
│ │ ENG-B1-002    | Intermediate B1   | English   | 8/15      │    │ │
│ │ JPN-A1-003    | Basic Japanese A1 | Japanese  | 20/20     │    │
│ └─────────────────────────────────────────────────────────────┘ │
│                                                                 │
│ Pagination: [< Previous] 1 2 3 [Next >]                        │
└─────────────────────────────────────────────────────────────────┘
```

#### Data Requirements:
- **Automatic branch filtering** based on current user's branch assignments
- **Default filter**: Only show `status = 'scheduled'` and `approval_status = 'approved'`
- **Sorting options**: Start date, course name, enrollment rate
- **Real-time enrollment counts** from database

#### Interactive Elements:
- Click row → Navigate to class detail
- Quick actions: View students, Enroll students (if capacity available)
- Export functionality: Export class list to Excel

### 5.2 Screen 2: Class Detail Page

#### URL: `/classes/{id}`

#### Layout Requirements:
```
┌─────────────────────────────────────────────────────────────────┐
│ 📖 Class Details: ENG-A1-001 - Basic English A1                 │
├─────────────────────────────────────────────────────────────────┤
│ ┌─────────────────┐ ┌─────────────────────────────────────────┐ │
│ │ Class Info      │ │ Current Students (15/20 enrolled)       │ │
│ │ ──────────────  │ │ ┌─────────────────────────────────────┐ │ │
│ │ Course: English │ │ │ [Search students...]               │ │ │
│ │ Branch: Hanoi   │ │ │                                     │ │ │
│ │ Modality: Online│ │ │ ST001 | Nguyen Van A | nguyenvana@  │ │ │
│ │ Start: 2025-01-15│ │ │ ST002 | Tran Thi B   | tranthib@    │ │ │
│ │ Teacher: Mr. John│ │ │ ST003 | Le Van C     | levanc@      │ │ │
│ │ Room: Online    │ │ │ ... (12 more)                     │ │ │
│ │ Schedule: TTh   │ │ └─────────────────────────────────────┘ │ │
│ │ 18:00-20:00     │ │                                     │ │ │
│ └─────────────────┘ │ [📥 Enroll Students] [📤 Export List] │ │
│                     └─────────────────────────────────────────┘ │
│                                                                 │
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │ Upcoming Sessions (Next 5)                                  │ │
│ │ Session 7  | 2025-01-28 18:00-20:00 | Teacher: Mr. John    │ │
│ │ Session 8  | 2025-01-30 18:00-20:00 | Teacher: Mr. John    │ │
│ │ ... (3 more)                                                  │ │
│ └─────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

#### Data Requirements:
- **Real-time enrollment summary**: Current/max enrolled, utilization rate
- **Student list with pagination**: Search, sort by enrollment date
- **Upcoming sessions**: Next 5 sessions with date/time/teacher
- **Class metadata**: All course, branch, teacher information

#### Interactive Elements:
- **Primary action**: "Enroll Students" button (triggers existing enrollment flow)
- **Secondary actions**: Export student list, view all sessions
- **Student interactions**: Click student → view student details

### 5.3 Screen 3: Branch Students Page

#### URL: `/students` (alternative navigation)

#### Layout Requirements:
```
┌─────────────────────────────────────────────────────────────────┐
│ 👥 Students Management - Hanoi Branch                          │
├─────────────────────────────────────────────────────────────────┤
│ Filters:                                                        │
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │ Search: [__________________] Level: [All ▼]                 │ │
│ │ Status: [Active ▼] Enrolled: [All ▼]                       │ │
│ └─────────────────────────────────────────────────────────────┘ │
│                                                                 │
│ ┌─────────────────────────────────────────────────────────────┐ │
│ │ Student Code | Name             | Email           | Level |    │ │
│ │ ST001        | Nguyen Van A     | nguyenvana@...  | A1    │    │ │
│ │ ST002        | Tran Thi B       | tranthib@...    | A2    │    │ │
│ │ ST003        | Le Van C         | levanc@...      | B1    │    │ │
│ │ ... (hundreds more)                                         │ │
│ └─────────────────────────────────────────────────────────────┘ │
│                                                                 │
│ Pagination: [< Previous] 1 2 3 ... 20 [Next >]                │
└─────────────────────────────────────────────────────────────────┘
```

#### Data Requirements:
- **Branch-based filtering**: Only show students from user's branch
- **Comprehensive search**: By code, name, email, phone
- **Status indicators**: Active/inactive, current enrollments
- **Quick statistics**: Total students, active enrollments, new this month

---

## 6. API ENDPOINTS SPECIFICATION

### 6.1 ClassController Endpoints

#### GET /api/v1/classes
**Purpose**: List classes accessible to academic affairs user

**Query Parameters**:
- `branchId` (Optional): Filter by branch (defaults to user's branches)
- `courseId` (Optional): Filter by course
- `status` (Optional): Filter by class status (default: SCHEDULED)
- `modality` (Optional): Filter by modality (ONLINE/OFFLINE/HYBRID)
- `search` (Optional): Search in class code, name, course name
- `page` (Optional): Page number (default: 0)
- `size` (Optional): Page size (default: 20)
- `sort` (Optional): Sort field (default: startDate)

**Response**:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "code": "ENG-A1-001",
        "name": "Basic English A1",
        "courseName": "English General",
        "branchName": "Hanoi",
        "modality": "ONLINE",
        "startDate": "2025-01-15",
        "status": "SCHEDULED",
        "approvalStatus": "APPROVED",
        "currentEnrolled": 15,
        "maxCapacity": 20,
        "availableSlots": 5,
        "teacherName": "John Smith",
        "scheduleSummary": "Tue, Thu 18:00-20:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 45,
    "totalPages": 3
  }
}
```

**Business Logic**:
- Filter classes by user's assigned branches
- Default to SCHEDULED status classes (ready for enrollment)
- Calculate current enrollment count from enrollment table
- Include capacity analysis for UI decisions
- Apply role-based access control

#### GET /api/v1/classes/{id}
**Purpose**: Get detailed class information

**Response**:
```json
{
  "success": true,
  "data": {
    "id": 1,
    "code": "ENG-A1-001",
    "name": "Basic English A1",
    "course": {
      "id": 1,
      "name": "English General",
      "code": "ENG-GEN",
      "description": "General English Course"
    },
    "branch": {
      "id": 1,
      "name": "Hanoi",
      "address": "123 Main St, Hanoi"
    },
    "modality": "ONLINE",
    "startDate": "2025-01-15",
    "plannedEndDate": "2025-03-15",
    "scheduleDays": [2, 4],
    "maxCapacity": 20,
    "status": "SCHEDULED",
    "approvalStatus": "APPROVED",
    "teacherName": "John Smith",
    "scheduleSummary": "Tue, Thu 18:00-20:00",
    "enrollmentSummary": {
      "currentEnrolled": 15,
      "maxCapacity": 20,
      "availableSlots": 5,
      "utilizationRate": 75.0,
      "canEnrollStudents": true,
      "enrollmentRestrictionReason": null
    },
    "upcomingSessions": [
      {
        "id": 7,
        "date": "2025-01-28",
        "startTime": "18:00",
        "endTime": "20:00",
        "teacherName": "John Smith",
        "status": "PLANNED"
      }
    ]
  }
}
```

#### GET /api/v1/classes/{id}/students
**Purpose**: List currently enrolled students

**Query Parameters**:
- `search` (Optional): Search by student code, name, email
- `status` (Optional): Filter by enrollment status
- `page`, `size`, `sort`: Pagination

**Response**:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 101,
        "studentCode": "ST001",
        "fullName": "Nguyen Van A",
        "email": "nguyenvana@email.com",
        "phone": "0901234567",
        "level": "A1",
        "enrolledAt": "2025-01-10T10:30:00Z",
        "enrolledBy": "Academic Staff",
        "status": "ENROLLED"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 15,
    "totalPages": 1
  }
}
```

### 6.2 StudentController Endpoints

#### GET /api/v1/students
**Purpose**: List students in academic affairs' branch

**Query Parameters**:
- `branchId` (Optional): Filter by branch (defaults to user's branches)
- `search` (Optional): Search by code, name, email, phone
- `level` (Optional): Filter by student level (A1, A2, B1, etc.)
- `enrollmentStatus` (Optional): Filter by current enrollment status
- `courseId` (Optional): Filter by current course enrollment
- `page`, `size`, `sort`: Pagination

**Response**:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 101,
        "studentCode": "ST001",
        "fullName": "Nguyen Van A",
        "email": "nguyenvana@email.com",
        "phone": "0901234567",
        "level": "A1",
        "branchName": "Hanoi",
        "activeEnrollments": 1,
        "lastEnrollmentDate": "2025-01-10",
        "status": "ACTIVE"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8
  }
}
```

#### GET /api/v1/students/search
**Purpose**: Quick search for enrollment (used in modal)

**Query Parameters**:
- `query` (Required): Search term (student code, email, or name)
- `branchId` (Optional): Limit to specific branch

**Response**:
```json
{
  "success": true,
  "data": [
    {
      "id": 101,
      "studentCode": "ST001",
      "fullName": "Nguyen Van A",
      "email": "nguyenvana@email.com",
      "level": "A1",
      "branchName": "Hanoi",
      "canEnroll": true,
      "restrictionReason": null
    }
  ]
}
```

---

## 7. DATABASE QUERIES & BUSINESS LOGIC

### 7.1 Class List Query

```sql
-- Core query for class list
SELECT DISTINCT
    c.id,
    c.code,
    c.name,
    c.start_date,
    c.max_capacity,
    c.status,
    c.approval_status,
    c.modality,
    co.name as course_name,
    b.name as branch_name,
    u.full_name as teacher_name,

    -- Enrollment counts
    COALESCE(enrolled_count.current_count, 0) as current_enrolled,
    c.max_capacity - COALESCE(enrolled_count.current_count, 0) as available_slots,

    -- Schedule summary
    array_to_string(c.schedule_days, ', ') as schedule_summary

FROM class c
INNER JOIN course co ON c.course_id = co.id
INNER JOIN branch b ON c.branch_id = b.id
LEFT JOIN teaching_slot ts ON c.id = ts.class_id AND ts.status = 'ACTIVE'
LEFT JOIN user_account u ON ts.teacher_id = u.id
LEFT JOIN (
    SELECT
        e.class_id,
        COUNT(*) as current_count
    FROM enrollment e
    WHERE e.status = 'ENROLLED'
    GROUP BY e.class_id
) enrolled_count ON c.id = enrolled_count.class_id

WHERE c.approval_status = 'APPROVED'
  AND c.status = 'SCHEDULED'
  AND c.branch_id IN (:user_branch_ids)

-- Dynamic filters
  AND (:course_id IS NULL OR c.course_id = :course_id)
  AND (:modality IS NULL OR c.modality = :modality)
  AND (:search IS NULL OR (
      LOWER(c.code) LIKE LOWER(CONCAT('%', :search, '%')) OR
      LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR
      LOWER(co.name) LIKE LOWER(CONCAT('%', :search, '%'))
  ))

ORDER BY c.start_date ASC
```

### 7.2 Class Students Query

```sql
-- Query for enrolled students in a class
SELECT
    s.id,
    s.student_code,
    u.full_name,
    u.email,
    u.phone,
    s.level,
    e.enrolled_at,
    enrolled_by.full_name as enrolled_by_name,
    e.status

FROM enrollment e
INNER JOIN student s ON e.student_id = s.id
INNER JOIN user_account u ON s.user_id = u.id
LEFT JOIN user_account enrolled_by ON e.enrolled_by = enrolled_by.id

WHERE e.class_id = :class_id
  AND e.status = 'ENROLLED'

-- Dynamic filters
  AND (:search IS NULL OR (
      LOWER(s.student_code) LIKE LOWER(CONCAT('%', :search, '%')) OR
      LOWER(u.full_name) LIKE LOWER(CONCAT('%', :search, '%')) OR
      LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
  ))

ORDER BY e.enrolled_at DESC
```

### 7.3 Branch Students Query

```sql
-- Query for students in branch
SELECT DISTINCT
    s.id,
    s.student_code,
    u.full_name,
    u.email,
    u.phone,
    s.level,
    b.name as branch_name,
    u.status as user_status,

    -- Active enrollments count
    COALESCE(active_enrollments.count, 0) as active_enrollments,

    -- Last enrollment date
    last_enrollment.last_enrollment_date

FROM student s
INNER JOIN user_account u ON s.user_id = u.id
INNER JOIN user_branches ub ON u.id = ub.user_id
INNER JOIN branch b ON ub.branch_id = b.id

LEFT JOIN (
    SELECT
        e.student_id,
        COUNT(*) as count
    FROM enrollment e
    WHERE e.status = 'ENROLLED'
    GROUP BY e.student_id
) active_enrollments ON s.id = active_enrollments.student_id

LEFT JOIN (
    SELECT
        e.student_id,
        MAX(e.enrolled_at) as last_enrollment_date
    FROM enrollment e
    WHERE e.status = 'ENROLLED'
    GROUP BY e.student_id
) last_enrollment ON s.id = last_enrollment.student_id

WHERE ub.branch_id IN (:user_branch_ids)
  AND u.status = 'ACTIVE'

-- Dynamic filters
  AND (:search IS NULL OR (
      LOWER(s.student_code) LIKE LOWER(CONCAT('%', :search, '%')) OR
      LOWER(u.full_name) LIKE LOWER(CONCAT('%', :search, '%')) OR
      LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR
      LOWER(u.phone) LIKE LOWER(CONCAT('%', :search, '%'))
  ))
  AND (:level IS NULL OR s.level = :level)

ORDER BY u.full_name ASC
```

### 7.4 Business Logic Requirements

#### Access Control Logic:
```java
// Academic Affairs user can only access:
// 1. Classes in their assigned branches
// 2. Students in their assigned branches
// 3. Classes with status = SCHEDULED and approval_status = APPROVED

public List<Long> getUserBranchIds(Long userId) {
    return userBranchesRepository.findByUserId(userId)
        .stream()
        .map(UserBranches::getBranchId)
        .collect(Collectors.toList());
}
```

#### Capacity Calculation Logic:
```java
public ClassEnrollmentSummary calculateEnrollmentSummary(Long classId) {
    int currentEnrolled = enrollmentRepository.countByClassIdAndStatus(
        classId, EnrollmentStatus.ENROLLED
    );

    ClassEntity classEntity = classRepository.findById(classId)
        .orElseThrow(() -> new EntityNotFoundException("Class not found"));

    boolean canEnroll = classEntity.getStatus() == ClassStatus.SCHEDULED
        && classEntity.getApprovalStatus() == ApprovalStatus.APPROVED
        && currentEnrolled < classEntity.getMaxCapacity();

    return ClassEnrollmentSummary.builder()
        .classId(classId)
        .currentEnrolled(currentEnrolled)
        .maxCapacity(classEntity.getMaxCapacity())
        .availableSlots(classEntity.getMaxCapacity() - currentEnrolled)
        .utilizationRate((double) currentEnrolled / classEntity.getMaxCapacity() * 100)
        .canEnrollStudents(canEnroll)
        .enrollmentRestrictionReason(canEnroll ? null : getRestrictionReason(classEntity, currentEnrolled))
        .build();
}
```

---

## 8. IMPLEMENTATION PHASES

### 8.1 Phase 1: Core Access (Week 1-2)
**Goal**: Enable access to existing enrollment system

#### Tasks:
1. **Fix Security Bug**: Change `ACADEMIC_AFFAIR` to `ACADEMIC_STAFF` in existing code
2. **Implement ClassController**: All 4 endpoints with DTOs
3. **Create Service Layer**: ClassService with business logic
4. **Repository Methods**: Custom queries for class and student data
5. **Unit Tests**: Comprehensive test coverage for ClassController

#### Deliverables:
- Academic Affairs can view class list
- Academic Affairs can view class details and current students
- Access to existing enrollment endpoints (already implemented)
- Basic filtering and search functionality

#### Success Criteria:
- Academic Affairs user can log in and see list of classes
- Can click on class to view details and current students
- Can access existing Excel enrollment functionality
- All endpoints properly secured and filtered by user's branch

### 8.2 Phase 2: Student Management (Week 3)
**Goal**: Complete student management workflow

#### Tasks:
1. **Implement StudentController**: All 4 endpoints with DTOs
2. **Create StudentService**: Business logic for student operations
3. **Repository Methods**: Student queries with filtering
4. **Integration Tests**: End-to-end workflow testing
5. **Performance Optimization**: Indexes and query optimization

#### Deliverables:
- Academic Affairs can view branch student database
- Search and filter functionality for students
- Quick search for enrollment modal
- Student enrollment history

#### Success Criteria:
- Can search and filter students by various criteria
- Quick search works efficiently for enrollment workflow
- Student details show comprehensive information
- Performance acceptable with large datasets (1000+ students)

### 8.3 Phase 3: Enhanced Features (Week 4)
**Goal**: Additional features for complete workflow

#### Tasks:
1. **Reporting Endpoints**: Branch-level enrollment reports
2. **Export Functionality**: Excel export for class lists and student data
3. **Enhanced Filtering**: Advanced filtering options
4. **UI Polish**: Better error handling and user feedback
5. **Documentation**: API documentation update

#### Deliverables:
- Export class lists to Excel
- Branch enrollment reports and statistics
- Advanced filtering and sorting options
- Complete API documentation

#### Success Criteria:
- Academic Affairs can export data for offline work
- Reports provide meaningful insights
- System handles edge cases gracefully
- API is well-documented for frontend team

---

## 9. TESTING STRATEGY

### 9.1 Unit Tests (Target: 90% coverage)

#### ClassController Tests:
```java
@ExtendWith(MockitoExtension.class)
class ClassControllerTest {

    @Mock
    private ClassService classService;

    @InjectMocks
    private ClassController classController;

    @Test
    void shouldGetClassesForAcademicStaff() {
        // Given
        UserPrincipal currentUser = createAcademicStaffUser();
        PageRequest pageRequest = PageRequest.of(0, 20);
        ClassListItemDTO mockClass = createMockClass();
        Page<ClassListItemDTO> mockPage = new PageImpl<>(List.of(mockClass));

        when(classService.getClasses(any(), any(), any(), eq(pageRequest), eq(currentUser.getId())))
            .thenReturn(mockPage);

        // When
        ResponseEntity<ResponseObject> response = classService.getClasses(
            null, null, null, null, pageRequest, currentUser
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();
        verify(classService).getClasses(any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldReturnUnauthorizedForNonAcademicStaff() {
        // Test access control for different roles
    }

    @Test
    void shouldFilterClassesByBranch() {
        // Test branch-based filtering
    }

    @Test
    void shouldHandleClassNotFound() {
        // Test error handling
    }
}
```

#### StudentController Tests:
```java
@ExtendWith(MockitoExtension.class)
class StudentControllerTest {

    @Test
    void shouldSearchStudentsInBranch() {
        // Test student search functionality
    }

    @Test
    void shouldFilterStudentsByLevel() {
        // Test level-based filtering
    }

    @Test
    void shouldGetStudentEnrollmentHistory() {
        // Test enrollment history retrieval
    }
}
```

### 9.2 Integration Tests

#### Database Integration:
```java
@SpringBootTest
@Testcontainers
@Transactional
class ClassControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    void shouldGetRealClassesFromDatabase() {
        // Setup: Create test data
        ClassEntity testClass = createTestClass();
        Student testStudent = createTestStudent();
        Enrollment testEnrollment = createTestEnrollment(testClass, testStudent);

        // Execute: Call endpoint
        ResponseEntity<ResponseObject> response = restTemplate.getForEntity(
            "/api/v1/classes", ResponseObject.class
        );

        // Verify: Real database query results
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();
    }

    @Test
    void shouldRespectBranchFiltering() {
        // Test multi-branch scenario
    }

    @Test
    void shouldHandleConcurrentEnrollments() {
        // Test race conditions
    }
}
```

### 9.3 End-to-End Tests

#### Complete Workflow Tests:
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AcademicAffairsWorkflowE2ETest {

    @Test
    void completeEnrollmentWorkflow() {
        // 1. Academic Affairs logs in
        // 2. Views class list
        // 3. Clicks on specific class
        // 4. Views current students
        // 5. Initiates enrollment process
        // 6. Uploads Excel file
        // 7. Reviews preview
        // 8. Confirms enrollment
        // 9. Verifies results

        // This tests integration with existing enrollment system
    }

    @Test
    void capacityOverrideWorkflow() {
        // Test override scenario with approval
    }

    @Test
    void errorHandlingWorkflow() {
        // Test various error scenarios
    }
}
```

### 9.4 Performance Tests

#### Load Testing:
- **Class List**: Handle 100+ classes with complex filtering
- **Student Search**: Handle 1000+ students with instant search
- **Concurrent Users**: Support 10+ concurrent Academic Affairs users

#### Database Performance:
- **Query Optimization**: Ensure all queries use proper indexes
- **Pagination**: Test large dataset pagination
- **Caching**: Implement appropriate caching strategies

---

## 10. DEPENDENCIES & RISKS

### 10.1 Technical Dependencies

#### Existing Components:
- ✅ **Enrollment System**: Fully implemented and tested
- ✅ **Security Configuration**: JWT authentication working
- ✅ **Database Schema**: All required tables and relationships exist
- ✅ **Entity Models**: Complete domain model with proper relationships

#### New Components Required:
- 🔄 **Security Fix**: Role name correction (`ACADEMIC_AFFAIR` → `ACADEMIC_STAFF`)
- 🆕 **ClassController**: New REST endpoints
- 🆕 **StudentController**: New REST endpoints
- 🆕 **Service Layer**: Business logic for new endpoints
- 🆕 **Repository Methods**: Custom database queries

### 10.2 Business Dependencies

#### User Requirements:
- Academic Affairs staff need training on new interface
- Integration with existing Sale team Excel workflow
- Clear documentation of enrollment policies and procedures

#### Process Alignment:
- Excel file format must match Sale team's current format
- Capacity override policies need to be documented
- Branch assignment rules must be clarified

### 10.3 Risk Assessment

#### High Risk Items:
1. **Security Bug**: Role name mismatch could block access
   - **Mitigation**: Fix immediately in Phase 1
   - **Impact**: Users cannot access the system

2. **Performance Issues**: Large student databases could be slow
   - **Mitigation**: Implement proper indexing and pagination
   - **Impact**: Poor user experience, system unusability

3. **Data Consistency**: Race conditions in concurrent enrollments
   - **Mitigation**: Existing pessimistic locking should handle this
   - **Impact**: Data integrity issues

#### Medium Risk Items:
1. **User Adoption**: Academic Affairs staff may resist new system
   - **Mitigation**: Involve users in design, provide training
   - **Impact**: Low adoption, continued manual processes

2. **Excel Format Changes**: Sale team might change file format
   - **Mitigation**: Flexible parsing with good error messages
   - **Impact**: Enrollment errors, support tickets

#### Low Risk Items:
1. **UI/UX Issues**: Interface design may need refinement
   - **Mitigation**: User testing and feedback loops
   - **Impact**: Minor usability problems

### 10.4 Success Metrics

#### Technical Metrics:
- **API Response Time**: < 500ms for all endpoints
- **Database Query Performance**: < 100ms for indexed queries
- **Test Coverage**: > 90% for new code
- **System Availability**: > 99.9% uptime

#### Business Metrics:
- **User Adoption**: 80% of Academic Affairs staff using system within 1 month
- **Enrollment Efficiency**: 50% reduction in manual enrollment time
- **Error Reduction**: 90% reduction in enrollment data entry errors
- **Capacity Utilization**: Improved class filling rates due to better visibility

---

## CONCLUSION

This implementation plan addresses the critical gap between the **complete enrollment backend** and the **Academic Affairs user interface**. By implementing the ClassController and StudentController endpoints, we will enable Academic Affairs staff to:

1. **Access the existing enrollment system** through a proper workflow
2. **View class information and current enrollment status**
3. **Navigate seamlessly to the Excel enrollment functionality**
4. **Manage student information effectively**

The phased approach ensures **quick wins** (Phase 1 enables core functionality) while building toward a **complete solution** (Phases 2-3). The existing robust enrollment backend means we can deliver significant business value with minimal backend development.

**Key Success Factors:**
- Fix the security role issue immediately
- Implement proper branch-based access control
- Ensure excellent performance with large datasets
- Provide comprehensive testing coverage
- Align closely with existing business workflows

This plan will transform the currently inaccessible enrollment system into a **fully functional academic affairs workflow** that directly supports the core business process of student enrollment management.

---

**Document Status:** ✅ Ready for Implementation
**Review Status:** Pending Technical Review
**Approval Status:** Pending Product Owner Approval
**Estimated Timeline:** 3-4 weeks
**Priority:** HIGH - Enables core business functionality