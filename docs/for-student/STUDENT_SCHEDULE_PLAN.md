# Student Schedule Feature - Backend Implementation Plan

**Feature**: Student My Schedule Page + Session Detail Modal
**Target Role**: `ROLE_STUDENT`
**Status**: Draft
**Date**: 2025-11-07

---

## 1. Feature Overview

### 1.1 User Story
```
As a STUDENT,
I want to view my weekly class schedule in a timetable format,
So that I can know WHEN, WHERE, and WHAT classes I need to attend.
```

### 1.2 Acceptance Criteria
- ✅ Student can view their weekly schedule (Monday-Sunday)
- ✅ Schedule organized by timeslots (rows) and days (columns)
- ✅ Each session shows: Topic, Time, Location, Status
- ✅ Student can navigate between weeks (prev/next)
- ✅ Student can click a session to view full details in modal
- ✅ Session details include: Materials, Homework, Teacher, Attendance status
- ✅ Only authenticated students can access their own schedule
- ✅ Makeup sessions are clearly marked

---

## 2. UX/UI Context (For Backend Understanding)

### 2.1 Page Layout: My Schedule

```
┌─────────────────────────────────────────────────────────────────────┐
│  My Schedule                                 Week: ← Nov 4-10 →     │
├─────────┬──────────┬──────────┬──────────┬──────────┬──────────┬───┤
│ Time    │ Monday 4 │ Tuesday 5│ Wed 6    │ Thu 7    │ Friday 8 │...│
├─────────┼──────────┼──────────┼──────────┼──────────┼──────────┼───┤
│ 08:00-  │ [Cell A] │          │ [Cell B] │          │ [Cell C] │   │
│ 10:00   │ IELTS    │          │ IELTS    │          │ IELTS    │   │
│         │ Intro    │          │ Reading  │          │ Grammar  │   │
│         │ Room 301 │          │ Room 301 │          │ Online   │   │
├─────────┼──────────┼──────────┼──────────┼──────────┼──────────┼───┤
│ 10:15-  │          │ [Cell D] │          │ [Cell E] │          │   │
│ 12:15   │          │ TOEIC    │          │ TOEIC    │          │   │
│         │          │ Part 3   │          │ Part 4   │          │   │
│         │          │ Room 302 │          │ Online   │          │   │
└─────────┴──────────┴──────────┴──────────┴──────────┴──────────┴───┘
```

**Cell Content** (rendered by frontend, data provided by backend):
- Class Code badge (color-coded)
- Session topic (primary text)
- Location (Room number or "Online")
- Status indicator (Planned/Done/Cancelled)
- Makeup badge (if applicable)

**Interactions**:
- **Click cell** → Open Session Detail Modal
- **Week navigation** → Fetch new week data
- **Today button** → Jump to current week

---

### 2.2 Modal: Session Detail

```
┌───────────────────────────────────────────────────────────┐
│ Session Details                                      [✕]  │
├───────────────────────────────────────────────────────────┤
│ IELTS Foundation - HN-FOUND-O1                            │
│ Monday, Nov 4, 2025 · 08:00 - 10:00                       │
│                                                           │
│ 📖 Topic: Introduction to IELTS                           │
│ 📍 Location: Room 301, Hanoi Branch (OFFLINE)             │
│ 👨‍🏫 Teacher: Mr. Nguyen Van A                             │
│                                                           │
│ ───────────────────────────────────────────────────────   │
│ 📝 Homework Assignment:                                   │
│ • Complete Workbook pages 10-15                           │
│ • Due: Nov 6, 2025                                        │
│ • Status: Not submitted                                   │
│                                                           │
│ 📎 Materials:                                             │
│ • Slide_Intro_IELTS.pdf [Download]                        │
│ • Vocabulary_List.pdf [Download]                          │
│                                                           │
│ ✅ Attendance: Planned (Not marked yet)                   │
│                                                           │
│ [Makeup Session Info if applicable]                       │
│ Original Session: Oct 28 (Cancelled)                      │
│ Makeup scheduled for: Nov 4                               │
└───────────────────────────────────────────────────────────┘
```

**Data Requirements**:
- All session details (topic, time, location)
- Related class information (code, name, teacher)
- Student-specific data (attendance, homework status)
- Materials download links
- Makeup session context (if applicable)

---

## 3. Backend Architecture

### 3.1 New Components to Create

```
Backend Structure:
├── controllers/
│   └── StudentScheduleController.java (NEW)
├── services/
│   └── StudentScheduleService.java (NEW - interface)
├── services/impl/
│   └── StudentScheduleServiceImpl.java (NEW)
├── repositories/
│   └── StudentSessionRepository.java (ENHANCE - add custom queries)
├── dtos/schedule/ (NEW package)
│   ├── WeeklyScheduleResponseDTO.java
│   ├── SessionDetailDTO.java
│   ├── TimeSlotDTO.java
│   └── DayScheduleDTO.java
└── utils/
    └── StudentContextHelper.java (NEW - extract student ID from JWT)
```

### 3.2 Existing Components to Use

**Entities** (No modification needed):
- `Student`
- `Session`
- `StudentSession`
- `TimeSlotTemplate`
- `ClassEntity`
- `CourseSession`
- `Enrollment`

**Repositories** (Already exist):
- `StudentRepository`
- `SessionRepository`
- `EnrollmentRepository`

---

## 4. API Endpoints Specification

### 4.1 Endpoint 1: Get Weekly Schedule

**Endpoint**: `GET /api/v1/students/me/schedule`

**Authentication**: Required (JWT)
**Authorization**: `@PreAuthorize("hasRole('STUDENT')")`

**Query Parameters**:
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `weekStart` | `LocalDate` (ISO 8601) | No | Current week Monday | Start date of the week (must be Monday) |

**Example Requests**:
```http
GET /api/v1/students/me/schedule
GET /api/v1/students/me/schedule?weekStart=2025-11-04
```

**Response**: `200 OK`
```json
{
  "success": true,
  "message": "Weekly schedule retrieved successfully",
  "data": {
    "weekStart": "2025-11-04",
    "weekEnd": "2025-11-10",
    "studentId": 123,
    "studentName": "Nguyen Van B",
    "timeSlots": [
      {
        "timeSlotTemplateId": 1,
        "name": "HN Morning 1",
        "startTime": "08:00:00",
        "endTime": "10:00:00"
      },
      {
        "timeSlotTemplateId": 2,
        "name": "HN Morning 2",
        "startTime": "10:15:00",
        "endTime": "12:15:00"
      }
    ],
    "schedule": {
      "MONDAY": [
        {
          "sessionId": 1001,
          "studentSessionId": 5001,
          "date": "2025-11-04",
          "dayOfWeek": "MONDAY",
          "timeSlotTemplateId": 1,
          "startTime": "08:00:00",
          "endTime": "10:00:00",
          "classCode": "HN-FOUND-O1",
          "className": "IELTS Foundation - Oct 2025",
          "courseId": 1,
          "courseName": "IELTS Foundation",
          "topic": "Introduction to IELTS",
          "sessionType": "CLASS",
          "sessionStatus": "PLANNED",
          "modality": "OFFLINE",
          "location": "Room 301",
          "branchName": "Hanoi Branch",
          "attendanceStatus": "PLANNED",
          "isMakeup": false,
          "makeupInfo": null
        }
      ],
      "TUESDAY": [
        {
          "sessionId": 1002,
          "studentSessionId": 5002,
          "date": "2025-11-05",
          "dayOfWeek": "TUESDAY",
          "timeSlotTemplateId": 2,
          "startTime": "10:15:00",
          "endTime": "12:15:00",
          "classCode": "HN-TOEIC-A1",
          "className": "TOEIC Advanced - Oct 2025",
          "courseId": 2,
          "courseName": "TOEIC Advanced",
          "topic": "Part 3: Conversations",
          "sessionType": "CLASS",
          "sessionStatus": "PLANNED",
          "modality": "ONLINE",
          "location": "https://zoom.us/j/123456789",
          "branchName": "Hanoi Branch",
          "attendanceStatus": "PLANNED",
          "isMakeup": false,
          "makeupInfo": null
        }
      ],
      "WEDNESDAY": [],
      "THURSDAY": [],
      "FRIDAY": [],
      "SATURDAY": [],
      "SUNDAY": []
    }
  }
}
```

**Makeup Session Example**:
```json
{
  "sessionId": 1050,
  "isMakeup": true,
  "makeupInfo": {
    "originalSessionId": 1001,
    "originalDate": "2025-10-28",
    "reason": "Teacher unavailable"
  }
}
```

**Error Responses**:

`401 Unauthorized` - No JWT token or invalid token
```json
{
  "success": false,
  "message": "Authentication required",
  "data": null
}
```

`403 Forbidden` - User is not a student
```json
{
  "success": false,
  "message": "Access denied: Student role required",
  "data": null
}
```

`400 Bad Request` - Invalid weekStart parameter
```json
{
  "success": false,
  "message": "weekStart must be a Monday (ISO 8601 format: YYYY-MM-DD)",
  "data": null
}
```

---

### 4.2 Endpoint 2: Get Session Detail

**Endpoint**: `GET /api/v1/students/me/sessions/{sessionId}`

**Authentication**: Required (JWT)
**Authorization**: `@PreAuthorize("hasRole('STUDENT')")`

**Path Parameters**:
| Parameter | Type | Description |
|-----------|------|-------------|
| `sessionId` | `Long` | ID of the session to retrieve |

**Example Request**:
```http
GET /api/v1/students/me/sessions/1001
```

**Response**: `200 OK`
```json
{
  "success": true,
  "message": "Session details retrieved successfully",
  "data": {
    "sessionId": 1001,
    "studentSessionId": 5001,
    "date": "2025-11-04",
    "dayOfWeek": "MONDAY",
    "startTime": "08:00:00",
    "endTime": "10:00:00",
    "timeSlotName": "HN Morning 1",

    "class": {
      "classId": 10,
      "classCode": "HN-FOUND-O1",
      "className": "IELTS Foundation - Oct 2025",
      "courseId": 1,
      "courseName": "IELTS Foundation",
      "teacherId": 50,
      "teacherName": "Mr. Nguyen Van A",
      "branchId": 1,
      "branchName": "Hanoi Branch",
      "modality": "OFFLINE"
    },

    "sessionInfo": {
      "topic": "Introduction to IELTS",
      "description": "Overview of IELTS exam structure and scoring",
      "sessionType": "CLASS",
      "sessionStatus": "PLANNED",
      "location": "Room 301",
      "onlineLink": null
    },

    "studentStatus": {
      "attendanceStatus": "PLANNED",
      "homeworkStatus": "NOT_SUBMITTED",
      "homeworkDueDate": "2025-11-06",
      "homeworkDescription": "Complete Workbook pages 10-15"
    },

    "materials": [
      {
        "materialId": 201,
        "fileName": "Slide_Intro_IELTS.pdf",
        "fileUrl": "/api/v1/materials/download/201",
        "uploadedAt": "2025-10-30T10:00:00"
      },
      {
        "materialId": 202,
        "fileName": "Vocabulary_List.pdf",
        "fileUrl": "/api/v1/materials/download/202",
        "uploadedAt": "2025-10-30T10:05:00"
      }
    ],

    "makeupInfo": null
  }
}
```

**Makeup Session Detail Example**:
```json
{
  "sessionId": 1050,
  "makeupInfo": {
    "isMakeup": true,
    "originalSessionId": 1001,
    "originalDate": "2025-10-28",
    "originalStatus": "CANCELLED",
    "reason": "Teacher unavailable",
    "makeupDate": "2025-11-04"
  }
}
```

**Error Responses**:

`404 Not Found` - Session does not exist or student not enrolled
```json
{
  "success": false,
  "message": "Session not found or you are not enrolled in this session",
  "data": null
}
```

`403 Forbidden` - Student trying to access another student's session
```json
{
  "success": false,
  "message": "Access denied: You can only view your own sessions",
  "data": null
}
```

---

## 5. Database Schema & Queries

### 5.1 Existing Tables (No Modification Needed)

**Relevant Tables**:
- `student` (id, user_id, student_code, full_name, ...)
- `session` (id, class_id, course_session_id, time_slot_template_id, date, type, status, ...)
- `student_session` (student_id, session_id, attendance_status, homework_status, is_makeup, ...)
- `time_slot_template` (id, branch_id, name, start_time, end_time, ...)
- `class` (id, code, name, course_id, teacher_id, branch_id, modality, ...)
- `course_session` (id, topic, description, materials, ...)
- `enrollment` (id, class_id, student_id, status, enrolled_at, ...)

**Relationships**:
```sql
student_session.student_id → student.id
student_session.session_id → session.id
session.time_slot_template_id → time_slot_template.id
session.class_id → class.id
session.course_session_id → course_session.id
class.teacher_id → user_account.id (teacher)
class.branch_id → branch.id
enrollment.student_id → student.id
enrollment.class_id → class.id
```

---

### 5.2 Required Custom Queries

#### Query 1: Get Student ID from User Account ID

**Repository**: `StudentRepository` (already exists)

**Method**: `Optional<Student> findByUserId(Long userId)`

**Purpose**: Convert JWT's `UserAccount.id` → `Student.id`

**SQL**:
```sql
SELECT * FROM student WHERE user_id = :userId;
```

---

#### Query 2: Get Weekly Schedule (Main Query)

**Repository**: `StudentSessionRepository` (add custom method)

**Method**:
```java
@Query("SELECT ss FROM StudentSession ss " +
       "JOIN FETCH ss.session s " +
       "JOIN FETCH s.timeSlotTemplate tst " +
       "JOIN FETCH s.classEntity c " +
       "JOIN FETCH c.course course " +
       "JOIN FETCH c.branch branch " +
       "JOIN FETCH s.courseSession cs " +
       "WHERE ss.student.id = :studentId " +
       "AND s.date BETWEEN :startDate AND :endDate " +
       "ORDER BY s.date ASC, tst.startTime ASC")
List<StudentSession> findWeeklyScheduleByStudentId(
    @Param("studentId") Long studentId,
    @Param("startDate") LocalDate startDate,
    @Param("endDate") LocalDate endDate
);
```

**Purpose**: Fetch all sessions for a student in a given week (with eager loading to avoid N+1 queries)

**SQL Equivalent**:
```sql
SELECT
    ss.*,
    s.*,
    tst.*,
    c.*,
    course.*,
    branch.*,
    cs.*
FROM student_session ss
JOIN session s ON ss.session_id = s.id
JOIN time_slot_template tst ON s.time_slot_template_id = tst.id
JOIN class c ON s.class_id = c.id
JOIN course course ON c.course_id = course.id
JOIN branch branch ON c.branch_id = branch.id
JOIN course_session cs ON s.course_session_id = cs.id
WHERE ss.student_id = :studentId
  AND s.date BETWEEN :startDate AND :endDate
ORDER BY s.date ASC, tst.start_time ASC;
```

**Performance Considerations**:
- Uses `JOIN FETCH` to load all related entities in a single query
- Prevents N+1 query problem
- Index recommendation: `CREATE INDEX idx_student_session_schedule ON student_session(student_id, session_id);`

---

#### Query 3: Get Session Detail (with Authorization Check)

**Repository**: `StudentSessionRepository`

**Method**:
```java
@Query("SELECT ss FROM StudentSession ss " +
       "JOIN FETCH ss.session s " +
       "JOIN FETCH s.timeSlotTemplate tst " +
       "JOIN FETCH s.classEntity c " +
       "JOIN FETCH c.course course " +
       "JOIN FETCH c.branch branch " +
       "JOIN FETCH c.teacher teacher " +
       "JOIN FETCH s.courseSession cs " +
       "WHERE ss.student.id = :studentId " +
       "AND ss.session.id = :sessionId")
Optional<StudentSession> findByStudentIdAndSessionId(
    @Param("studentId") Long studentId,
    @Param("sessionId") Long sessionId
);
```

**Purpose**:
- Fetch session details for a specific student + session combination
- Ensures student can only access their own sessions (authorization)
- Returns `Optional.empty()` if student is not enrolled in that session

**SQL Equivalent**:
```sql
SELECT
    ss.*,
    s.*,
    tst.*,
    c.*,
    course.*,
    branch.*,
    teacher.*,
    cs.*
FROM student_session ss
JOIN session s ON ss.session_id = s.id
JOIN time_slot_template tst ON s.time_slot_template_id = tst.id
JOIN class c ON s.class_id = c.id
JOIN course course ON c.course_id = course.id
JOIN branch branch ON c.branch_id = branch.id
JOIN user_account teacher ON c.teacher_id = teacher.id
JOIN course_session cs ON s.course_session_id = cs.id
WHERE ss.student_id = :studentId
  AND ss.session_id = :sessionId;
```

---

#### Query 4: Get Unique TimeSlots for a Week

**Repository**: `TimeSlotTemplateRepository`

**Method**:
```java
@Query("SELECT DISTINCT tst FROM TimeSlotTemplate tst " +
       "JOIN Session s ON s.timeSlotTemplate.id = tst.id " +
       "JOIN StudentSession ss ON ss.session.id = s.id " +
       "WHERE ss.student.id = :studentId " +
       "AND s.date BETWEEN :startDate AND :endDate " +
       "ORDER BY tst.startTime ASC")
List<TimeSlotTemplate> findTimeSlotsByStudentAndWeek(
    @Param("studentId") Long studentId,
    @Param("startDate") LocalDate startDate,
    @Param("endDate") LocalDate endDate
);
```

**Purpose**: Get all distinct time slots used in a student's weekly schedule (for Y-axis of timetable)

**SQL Equivalent**:
```sql
SELECT DISTINCT tst.*
FROM time_slot_template tst
JOIN session s ON s.time_slot_template_id = tst.id
JOIN student_session ss ON ss.session_id = s.id
WHERE ss.student_id = :studentId
  AND s.date BETWEEN :startDate AND :endDate
ORDER BY tst.start_time ASC;
```

---

#### Query 5: Get Makeup Session Info (If Applicable)

**Repository**: `SessionRepository`

**Method**:
```java
@Query("SELECT s FROM Session s " +
       "WHERE s.id = :makeupSessionId " +
       "OR s.id = :originalSessionId")
List<Session> findMakeupRelatedSessions(
    @Param("makeupSessionId") Long makeupSessionId,
    @Param("originalSessionId") Long originalSessionId
);
```

**Purpose**: Fetch related makeup sessions for context display

**SQL Equivalent**:
```sql
SELECT * FROM session
WHERE id = :makeupSessionId
   OR id = :originalSessionId;
```

**Usage**:
- If `studentSession.is_makeup = true`, fetch `original_session_id` details
- If session is the original for a makeup, fetch the `makeup_session_id` details

---

### 5.3 Database Indexes (Recommendations)

**Add these indexes for performance optimization**:

```sql
-- Index for student session schedule queries
CREATE INDEX idx_student_session_schedule
ON student_session(student_id, session_id);

-- Index for session date range queries
CREATE INDEX idx_session_date_range
ON session(date, status);

-- Index for session class lookup
CREATE INDEX idx_session_class
ON session(class_id, date);

-- Index for enrollment lookup
CREATE INDEX idx_enrollment_student_status
ON enrollment(student_id, status);
```

---

## 6. DTO Structure

### 6.1 WeeklyScheduleResponseDTO

**File**: `dtos/schedule/WeeklyScheduleResponseDTO.java`

**Purpose**: Response for `GET /api/v1/students/me/schedule`

**Structure**:
```java
@Data
@Builder
public class WeeklyScheduleResponseDTO {
    private LocalDate weekStart;
    private LocalDate weekEnd;
    private Long studentId;
    private String studentName;
    private List<TimeSlotDTO> timeSlots;
    private Map<DayOfWeek, List<SessionSummaryDTO>> schedule;
}
```

**Fields**:
- `weekStart`: Monday of the week (e.g., 2025-11-04)
- `weekEnd`: Sunday of the week (e.g., 2025-11-10)
- `studentId`: Current student's ID
- `studentName`: Current student's full name
- `timeSlots`: List of all time slots used in this week (for Y-axis)
- `schedule`: Map of day → sessions (for grid rendering)

---

### 6.2 SessionSummaryDTO

**File**: `dtos/schedule/SessionSummaryDTO.java`

**Purpose**: Summary info for each session in weekly view

**Structure**:
```java
@Data
@Builder
public class SessionSummaryDTO {
    // Identifiers
    private Long sessionId;
    private Long studentSessionId;

    // Date & Time
    private LocalDate date;
    private DayOfWeek dayOfWeek;
    private Long timeSlotTemplateId;
    private LocalTime startTime;
    private LocalTime endTime;

    // Class Info
    private String classCode;
    private String className;
    private Long courseId;
    private String courseName;

    // Session Info
    private String topic;
    private SessionType sessionType;      // CLASS, MAKEUP, ASSESSMENT
    private SessionStatus sessionStatus;  // PLANNED, DONE, CANCELLED

    // Location
    private ClassModality modality;       // OFFLINE, ONLINE, HYBRID
    private String location;              // "Room 301" or "https://zoom.us/..."
    private String branchName;

    // Student Status
    private AttendanceStatus attendanceStatus;

    // Makeup Info
    private Boolean isMakeup;
    private MakeupInfoDTO makeupInfo;     // null if not makeup
}
```

---

### 6.3 TimeSlotDTO

**File**: `dtos/schedule/TimeSlotDTO.java`

**Purpose**: Represent a time slot for Y-axis of timetable

**Structure**:
```java
@Data
@Builder
public class TimeSlotDTO {
    private Long timeSlotTemplateId;
    private String name;           // "HN Morning 1"
    private LocalTime startTime;   // 08:00:00
    private LocalTime endTime;     // 10:00:00
}
```

---

### 6.4 SessionDetailDTO

**File**: `dtos/schedule/SessionDetailDTO.java`

**Purpose**: Full session details for modal view (response for `GET /api/v1/students/me/sessions/{sessionId}`)

**Structure**:
```java
@Data
@Builder
public class SessionDetailDTO {
    // Session Identifiers
    private Long sessionId;
    private Long studentSessionId;

    // Date & Time
    private LocalDate date;
    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private String timeSlotName;

    // Class Information
    private ClassInfoDTO classInfo;

    // Session Content
    private SessionInfoDTO sessionInfo;

    // Student-Specific Status
    private StudentStatusDTO studentStatus;

    // Materials
    private List<MaterialDTO> materials;

    // Makeup Context
    private MakeupInfoDTO makeupInfo;
}

// Nested DTOs:

@Data
@Builder
class ClassInfoDTO {
    private Long classId;
    private String classCode;
    private String className;
    private Long courseId;
    private String courseName;
    private Long teacherId;
    private String teacherName;
    private Long branchId;
    private String branchName;
    private ClassModality modality;
}

@Data
@Builder
class SessionInfoDTO {
    private String topic;
    private String description;
    private SessionType sessionType;
    private SessionStatus sessionStatus;
    private String location;
    private String onlineLink;
}

@Data
@Builder
class StudentStatusDTO {
    private AttendanceStatus attendanceStatus;
    private HomeworkStatus homeworkStatus;
    private LocalDate homeworkDueDate;
    private String homeworkDescription;
}

@Data
@Builder
class MaterialDTO {
    private Long materialId;
    private String fileName;
    private String fileUrl;
    private LocalDateTime uploadedAt;
}
```

---

### 6.5 MakeupInfoDTO

**File**: `dtos/schedule/MakeupInfoDTO.java`

**Purpose**: Contextual information about makeup sessions

**Structure**:
```java
@Data
@Builder
public class MakeupInfoDTO {
    private Boolean isMakeup;
    private Long originalSessionId;
    private LocalDate originalDate;
    private SessionStatus originalStatus;
    private String reason;
    private LocalDate makeupDate;
}
```

---

## 7. Service Layer Implementation

### 7.1 StudentScheduleService Interface

**File**: `services/StudentScheduleService.java`

**Methods**:
```java
public interface StudentScheduleService {

    /**
     * Get weekly schedule for a student
     * @param studentId The student's ID
     * @param weekStart Monday of the target week
     * @return Weekly schedule organized by day and timeslot
     * @throws ResourceNotFoundException if student not found
     */
    WeeklyScheduleResponseDTO getWeeklySchedule(Long studentId, LocalDate weekStart);

    /**
     * Get detailed information for a specific session
     * @param studentId The student's ID
     * @param sessionId The session's ID
     * @return Full session details
     * @throws ResourceNotFoundException if session not found or student not enrolled
     */
    SessionDetailDTO getSessionDetail(Long studentId, Long sessionId);

    /**
     * Helper: Calculate Monday of current week
     * @return LocalDate of current Monday
     */
    LocalDate getCurrentWeekStart();
}
```

---

### 7.2 StudentScheduleServiceImpl Logic

**File**: `services/impl/StudentScheduleServiceImpl.java`

**Key Methods**:

#### Method 1: getWeeklySchedule()

**Algorithm**:
```java
1. Validate weekStart is a Monday
2. Calculate weekEnd (weekStart + 6 days)
3. Fetch student info by studentId
4. Query StudentSessionRepository.findWeeklyScheduleByStudentId()
   → Returns List<StudentSession> with all related data
5. Extract unique TimeSlotTemplates from results
6. Group StudentSessions by DayOfWeek:
   - Use EXTRACT(ISODOW FROM session.date) or LocalDate.getDayOfWeek()
   - Create Map<DayOfWeek, List<SessionSummaryDTO>>
7. Convert entities to DTOs:
   - StudentSession + Session → SessionSummaryDTO
   - TimeSlotTemplate → TimeSlotDTO
8. Return WeeklyScheduleResponseDTO
```

**Edge Cases**:
- **No sessions in week**: Return empty schedule map with all days = empty list
- **Multiple sessions same time**: Allow multiple items in same day list (frontend handles stacking)
- **Makeup sessions**: Set `isMakeup = true`, populate `makeupInfo` by querying original session

**Example Code Structure**:
```java
@Override
public WeeklyScheduleResponseDTO getWeeklySchedule(Long studentId, LocalDate weekStart) {
    // 1. Validate weekStart is Monday
    if (weekStart.getDayOfWeek() != DayOfWeek.MONDAY) {
        throw new InvalidRequestException("weekStart must be a Monday");
    }

    // 2. Calculate week range
    LocalDate weekEnd = weekStart.plusDays(6);

    // 3. Fetch student
    Student student = studentRepository.findById(studentId)
        .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

    // 4. Fetch all sessions for this week (with JOIN FETCH)
    List<StudentSession> studentSessions = studentSessionRepository
        .findWeeklyScheduleByStudentId(studentId, weekStart, weekEnd);

    // 5. Extract unique timeslots
    List<TimeSlotTemplate> timeSlots = studentSessions.stream()
        .map(ss -> ss.getSession().getTimeSlotTemplate())
        .distinct()
        .sorted(Comparator.comparing(TimeSlotTemplate::getStartTime))
        .toList();

    // 6. Group by day of week
    Map<DayOfWeek, List<SessionSummaryDTO>> scheduleMap = studentSessions.stream()
        .collect(Collectors.groupingBy(
            ss -> ss.getSession().getDate().getDayOfWeek(),
            Collectors.mapping(this::mapToSessionSummaryDTO, Collectors.toList())
        ));

    // 7. Ensure all days exist in map (even if empty)
    for (DayOfWeek day : DayOfWeek.values()) {
        scheduleMap.putIfAbsent(day, new ArrayList<>());
    }

    // 8. Build response
    return WeeklyScheduleResponseDTO.builder()
        .weekStart(weekStart)
        .weekEnd(weekEnd)
        .studentId(student.getId())
        .studentName(student.getFullName())
        .timeSlots(timeSlots.stream().map(this::mapToTimeSlotDTO).toList())
        .schedule(scheduleMap)
        .build();
}
```

---

#### Method 2: getSessionDetail()

**Algorithm**:
```java
1. Query StudentSessionRepository.findByStudentIdAndSessionId()
   → Ensures student has access to this session
   → Returns Optional<StudentSession> with all related data
2. If empty → throw ResourceNotFoundException (either session doesn't exist or student not enrolled)
3. Extract all related entities:
   - Session, TimeSlotTemplate, ClassEntity, CourseSession, Teacher
4. Fetch materials from CourseSession (if stored as JSON or separate table)
5. If is_makeup = true:
   - Query Session table for original_session_id
   - Build MakeupInfoDTO
6. Convert to SessionDetailDTO
7. Return response
```

**Example Code Structure**:
```java
@Override
public SessionDetailDTO getSessionDetail(Long studentId, Long sessionId) {
    // 1. Fetch with authorization check
    StudentSession studentSession = studentSessionRepository
        .findByStudentIdAndSessionId(studentId, sessionId)
        .orElseThrow(() -> new ResourceNotFoundException(
            "Session not found or you are not enrolled in this session"
        ));

    // 2. Extract entities
    Session session = studentSession.getSession();
    TimeSlotTemplate timeSlot = session.getTimeSlotTemplate();
    ClassEntity classEntity = session.getClassEntity();
    CourseSession courseSession = session.getCourseSession();

    // 3. Build ClassInfoDTO
    ClassInfoDTO classInfo = ClassInfoDTO.builder()
        .classId(classEntity.getId())
        .classCode(classEntity.getCode())
        .className(classEntity.getName())
        .teacherId(classEntity.getTeacher().getId())
        .teacherName(classEntity.getTeacher().getFullName())
        .branchName(classEntity.getBranch().getName())
        .modality(classEntity.getModality())
        .build();

    // 4. Build SessionInfoDTO
    SessionInfoDTO sessionInfo = SessionInfoDTO.builder()
        .topic(courseSession.getTopic())
        .description(courseSession.getDescription())
        .sessionType(session.getType())
        .sessionStatus(session.getStatus())
        .location(determineLocation(classEntity, session))
        .onlineLink(classEntity.getModality() == ONLINE ? classEntity.getOnlineLink() : null)
        .build();

    // 5. Build StudentStatusDTO
    StudentStatusDTO studentStatus = StudentStatusDTO.builder()
        .attendanceStatus(studentSession.getAttendanceStatus())
        .homeworkStatus(studentSession.getHomeworkStatus())
        .homeworkDueDate(courseSession.getHomeworkDueDate())
        .homeworkDescription(courseSession.getHomeworkDescription())
        .build();

    // 6. Extract materials (from JSON or separate table)
    List<MaterialDTO> materials = extractMaterials(courseSession);

    // 7. Build MakeupInfoDTO if applicable
    MakeupInfoDTO makeupInfo = null;
    if (studentSession.getIsMakeup()) {
        Session originalSession = sessionRepository.findById(studentSession.getOriginalSessionId())
            .orElse(null);
        if (originalSession != null) {
            makeupInfo = MakeupInfoDTO.builder()
                .isMakeup(true)
                .originalSessionId(originalSession.getId())
                .originalDate(originalSession.getDate())
                .originalStatus(originalSession.getStatus())
                .reason("Session rescheduled") // Or from a dedicated field
                .makeupDate(session.getDate())
                .build();
        }
    }

    // 8. Build final DTO
    return SessionDetailDTO.builder()
        .sessionId(session.getId())
        .studentSessionId(studentSession.getId())
        .date(session.getDate())
        .dayOfWeek(session.getDate().getDayOfWeek())
        .startTime(timeSlot.getStartTime())
        .endTime(timeSlot.getEndTime())
        .timeSlotName(timeSlot.getName())
        .classInfo(classInfo)
        .sessionInfo(sessionInfo)
        .studentStatus(studentStatus)
        .materials(materials)
        .makeupInfo(makeupInfo)
        .build();
}
```

---

#### Helper Method: mapToSessionSummaryDTO()

**Purpose**: Convert `StudentSession` entity to `SessionSummaryDTO`

**Example**:
```java
private SessionSummaryDTO mapToSessionSummaryDTO(StudentSession studentSession) {
    Session session = studentSession.getSession();
    ClassEntity classEntity = session.getClassEntity();
    CourseSession courseSession = session.getCourseSession();
    TimeSlotTemplate timeSlot = session.getTimeSlotTemplate();

    MakeupInfoDTO makeupInfo = null;
    if (studentSession.getIsMakeup()) {
        makeupInfo = buildMakeupInfo(studentSession);
    }

    return SessionSummaryDTO.builder()
        .sessionId(session.getId())
        .studentSessionId(studentSession.getId())
        .date(session.getDate())
        .dayOfWeek(session.getDate().getDayOfWeek())
        .timeSlotTemplateId(timeSlot.getId())
        .startTime(timeSlot.getStartTime())
        .endTime(timeSlot.getEndTime())
        .classCode(classEntity.getCode())
        .className(classEntity.getName())
        .courseId(classEntity.getCourse().getId())
        .courseName(classEntity.getCourse().getName())
        .topic(courseSession.getTopic())
        .sessionType(session.getType())
        .sessionStatus(session.getStatus())
        .modality(classEntity.getModality())
        .location(determineLocation(classEntity, session))
        .branchName(classEntity.getBranch().getName())
        .attendanceStatus(studentSession.getAttendanceStatus())
        .isMakeup(studentSession.getIsMakeup())
        .makeupInfo(makeupInfo)
        .build();
}
```

---

### 7.3 StudentContextHelper Utility

**File**: `utils/StudentContextHelper.java`

**Purpose**: Extract `Student.id` from JWT's `UserPrincipal`

**Methods**:
```java
@Component
public class StudentContextHelper {

    private final StudentRepository studentRepository;

    /**
     * Get Student entity from authenticated UserPrincipal
     * @param userPrincipal The authenticated user from JWT
     * @return Student entity
     * @throws ResourceNotFoundException if user is not a student
     */
    public Student getStudentFromPrincipal(UserPrincipal userPrincipal) {
        Long userId = userPrincipal.getId(); // UserAccount.id from JWT
        return studentRepository.findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Student profile not found for user ID: " + userId
            ));
    }

    /**
     * Get Student ID from authenticated UserPrincipal
     * @param userPrincipal The authenticated user from JWT
     * @return Student.id
     */
    public Long getStudentId(UserPrincipal userPrincipal) {
        return getStudentFromPrincipal(userPrincipal).getId();
    }
}
```

**Usage in Controller**:
```java
@GetMapping("/me/schedule")
public ResponseEntity<ResponseObject<WeeklyScheduleResponseDTO>> getMySchedule(
    @AuthenticationPrincipal UserPrincipal userPrincipal,
    @RequestParam(required = false) LocalDate weekStart
) {
    Long studentId = studentContextHelper.getStudentId(userPrincipal);
    // ... rest of logic
}
```

---

## 8. Controller Implementation

### 8.1 StudentScheduleController

**File**: `controllers/StudentScheduleController.java`

**Annotations**:
```java
@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
@Validated
public class StudentScheduleController {

    private final StudentScheduleService studentScheduleService;
    private final StudentContextHelper studentContextHelper;
}
```

---

### 8.2 Endpoint 1: Get Weekly Schedule

```java
@GetMapping("/me/schedule")
@PreAuthorize("hasRole('STUDENT')")
public ResponseEntity<ResponseObject<WeeklyScheduleResponseDTO>> getMySchedule(
    @AuthenticationPrincipal UserPrincipal userPrincipal,
    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart
) {
    // 1. Extract student ID from JWT
    Long studentId = studentContextHelper.getStudentId(userPrincipal);

    // 2. Default to current week if not specified
    if (weekStart == null) {
        weekStart = studentScheduleService.getCurrentWeekStart();
    }

    // 3. Validate weekStart is Monday
    if (weekStart.getDayOfWeek() != DayOfWeek.MONDAY) {
        return ResponseEntity.badRequest().body(
            ResponseObject.<WeeklyScheduleResponseDTO>builder()
                .success(false)
                .message("weekStart must be a Monday (ISO 8601 format: YYYY-MM-DD)")
                .build()
        );
    }

    // 4. Fetch schedule
    WeeklyScheduleResponseDTO schedule = studentScheduleService.getWeeklySchedule(studentId, weekStart);

    // 5. Return response
    return ResponseEntity.ok(
        ResponseObject.<WeeklyScheduleResponseDTO>builder()
            .success(true)
            .message("Weekly schedule retrieved successfully")
            .data(schedule)
            .build()
    );
}
```

---

### 8.3 Endpoint 2: Get Session Detail

```java
@GetMapping("/me/sessions/{sessionId}")
@PreAuthorize("hasRole('STUDENT')")
public ResponseEntity<ResponseObject<SessionDetailDTO>> getMySessionDetail(
    @AuthenticationPrincipal UserPrincipal userPrincipal,
    @PathVariable Long sessionId
) {
    // 1. Extract student ID from JWT
    Long studentId = studentContextHelper.getStudentId(userPrincipal);

    // 2. Fetch session detail (with authorization check inside service)
    SessionDetailDTO sessionDetail = studentScheduleService.getSessionDetail(studentId, sessionId);

    // 3. Return response
    return ResponseEntity.ok(
        ResponseObject.<SessionDetailDTO>builder()
            .success(true)
            .message("Session details retrieved successfully")
            .data(sessionDetail)
            .build()
    );
}
```

---

## 9. Security Considerations

### 9.1 Authorization Rules

**Rule 1**: Students can ONLY access their own schedules
- Enforced by: `studentContextHelper.getStudentId(userPrincipal)` → always uses authenticated user's student ID
- No way to pass arbitrary `studentId` in request

**Rule 2**: Students can ONLY view sessions they are enrolled in
- Enforced by: `StudentSessionRepository.findByStudentIdAndSessionId()`
- Query filters by both `student_id` AND `session_id`
- Returns `Optional.empty()` if student not enrolled → 404 error

**Rule 3**: Role-based access control
- `@PreAuthorize("hasRole('STUDENT')")` on all endpoints
- JWT must contain `ROLE_STUDENT` authority

### 9.2 Data Exposure Prevention

**What students CAN see**:
- Their own session details
- Teacher names (public info)
- Materials uploaded for their sessions
- Their own attendance/homework status

**What students CANNOT see**:
- Other students' attendance records
- Teacher internal notes
- Other classes' schedules (unless enrolled)
- Sensitive enrollment data

### 9.3 JWT Token Validation

**Flow**:
```
1. Client sends: Authorization: Bearer <JWT>
2. Spring Security validates:
   - Token signature (using JWT_SECRET)
   - Token expiration
   - Token claims
3. If valid → Extract UserPrincipal (UserAccount.id, roles)
4. Controller uses @AuthenticationPrincipal to access authenticated user
5. StudentContextHelper converts UserAccount.id → Student.id
6. Service queries using Student.id (cannot be tampered)
```

---

## 10. Error Handling

### 10.1 Exception Types

**ResourceNotFoundException** (404):
- Student not found
- Session not found
- Student not enrolled in session

**InvalidRequestException** (400):
- weekStart is not a Monday
- Invalid date format

**UnauthorizedException** (401):
- No JWT token
- Invalid JWT token
- Expired JWT token

**ForbiddenException** (403):
- User is not a student
- Student trying to access another student's data

### 10.2 Global Exception Handler (Already Exists)

**Location**: `exceptions/GlobalExceptionHandler.java`

**Example Handling**:
```java
@ExceptionHandler(ResourceNotFoundException.class)
public ResponseEntity<ResponseObject<Void>> handleResourceNotFound(ResourceNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
        ResponseObject.<Void>builder()
            .success(false)
            .message(ex.getMessage())
            .build()
    );
}

@ExceptionHandler(InvalidRequestException.class)
public ResponseEntity<ResponseObject<Void>> handleInvalidRequest(InvalidRequestException ex) {
    return ResponseEntity.badRequest().body(
        ResponseObject.<Void>builder()
            .success(false)
            .message(ex.getMessage())
            .build()
    );
}
```

---

## 11. Testing Strategy

### 11.1 Unit Tests (Service Layer)

**Test Class**: `StudentScheduleServiceImplTest`

**Test Cases**:

```java
@SpringBootTest
@ActiveProfiles("test")
class StudentScheduleServiceImplTest {

    @Autowired
    private StudentScheduleService studentScheduleService;

    @MockitoBean
    private StudentSessionRepository studentSessionRepository;

    @MockitoBean
    private StudentRepository studentRepository;

    // Test 1: Get weekly schedule - success
    @Test
    void shouldReturnWeeklySchedule_WhenStudentExists() {
        // Given
        Long studentId = 1L;
        LocalDate weekStart = LocalDate.of(2025, 11, 4);

        Student student = Student.builder().id(studentId).fullName("Test Student").build();
        List<StudentSession> mockSessions = createMockStudentSessions();

        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(studentSessionRepository.findWeeklyScheduleByStudentId(studentId, weekStart, weekStart.plusDays(6)))
            .thenReturn(mockSessions);

        // When
        WeeklyScheduleResponseDTO result = studentScheduleService.getWeeklySchedule(studentId, weekStart);

        // Then
        assertThat(result.getWeekStart()).isEqualTo(weekStart);
        assertThat(result.getSchedule().get(DayOfWeek.MONDAY)).hasSize(1);
    }

    // Test 2: Get weekly schedule - student not found
    @Test
    void shouldThrowException_WhenStudentNotFound() {
        // Given
        Long studentId = 999L;
        LocalDate weekStart = LocalDate.of(2025, 11, 4);

        when(studentRepository.findById(studentId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> studentScheduleService.getWeeklySchedule(studentId, weekStart))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Student not found");
    }

    // Test 3: Get weekly schedule - invalid weekStart (not Monday)
    @Test
    void shouldThrowException_WhenWeekStartNotMonday() {
        // Given
        Long studentId = 1L;
        LocalDate weekStart = LocalDate.of(2025, 11, 5); // Tuesday

        // When & Then
        assertThatThrownBy(() -> studentScheduleService.getWeeklySchedule(studentId, weekStart))
            .isInstanceOf(InvalidRequestException.class)
            .hasMessageContaining("must be a Monday");
    }

    // Test 4: Get session detail - success
    @Test
    void shouldReturnSessionDetail_WhenStudentEnrolled() {
        // Given
        Long studentId = 1L;
        Long sessionId = 100L;

        StudentSession mockStudentSession = createMockStudentSession();
        when(studentSessionRepository.findByStudentIdAndSessionId(studentId, sessionId))
            .thenReturn(Optional.of(mockStudentSession));

        // When
        SessionDetailDTO result = studentScheduleService.getSessionDetail(studentId, sessionId);

        // Then
        assertThat(result.getSessionId()).isEqualTo(sessionId);
        assertThat(result.getClassInfo()).isNotNull();
    }

    // Test 5: Get session detail - student not enrolled
    @Test
    void shouldThrowException_WhenStudentNotEnrolled() {
        // Given
        Long studentId = 1L;
        Long sessionId = 100L;

        when(studentSessionRepository.findByStudentIdAndSessionId(studentId, sessionId))
            .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> studentScheduleService.getSessionDetail(studentId, sessionId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("not enrolled");
    }

    // Test 6: Makeup session info included
    @Test
    void shouldIncludeMakeupInfo_WhenSessionIsMakeup() {
        // Given
        Long studentId = 1L;
        Long sessionId = 100L;

        StudentSession mockStudentSession = createMockMakeupStudentSession();
        when(studentSessionRepository.findByStudentIdAndSessionId(studentId, sessionId))
            .thenReturn(Optional.of(mockStudentSession));

        // When
        SessionDetailDTO result = studentScheduleService.getSessionDetail(studentId, sessionId);

        // Then
        assertThat(result.getMakeupInfo()).isNotNull();
        assertThat(result.getMakeupInfo().getIsMakeup()).isTrue();
    }
}
```

---

### 11.2 Integration Tests (Controller Layer)

**Test Class**: `StudentScheduleControllerIntegrationTest`

**Test Cases**:

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StudentScheduleControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StudentScheduleService studentScheduleService;

    @MockitoBean
    private StudentContextHelper studentContextHelper;

    private String studentJwtToken;

    @BeforeEach
    void setup() {
        // Generate JWT token for ROLE_STUDENT
        studentJwtToken = generateStudentJWT();
    }

    // Test 1: Get weekly schedule - success
    @Test
    void shouldReturnWeeklySchedule_WithValidToken() throws Exception {
        // Given
        Long studentId = 1L;
        LocalDate weekStart = LocalDate.of(2025, 11, 4);
        WeeklyScheduleResponseDTO mockResponse = createMockWeeklySchedule();

        when(studentContextHelper.getStudentId(any())).thenReturn(studentId);
        when(studentScheduleService.getWeeklySchedule(studentId, weekStart)).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/students/me/schedule")
                .param("weekStart", "2025-11-04")
                .header("Authorization", "Bearer " + studentJwtToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.weekStart").value("2025-11-04"))
            .andExpect(jsonPath("$.data.schedule.MONDAY").isArray());
    }

    // Test 2: Get weekly schedule - no token (401)
    @Test
    void shouldReturn401_WhenNoToken() throws Exception {
        mockMvc.perform(get("/api/v1/students/me/schedule"))
            .andExpect(status().isUnauthorized());
    }

    // Test 3: Get weekly schedule - invalid role (403)
    @Test
    void shouldReturn403_WhenUserNotStudent() throws Exception {
        String teacherToken = generateTeacherJWT(); // ROLE_TEACHER

        mockMvc.perform(get("/api/v1/students/me/schedule")
                .header("Authorization", "Bearer " + teacherToken))
            .andExpect(status().isForbidden());
    }

    // Test 4: Get session detail - success
    @Test
    void shouldReturnSessionDetail_WithValidToken() throws Exception {
        // Given
        Long studentId = 1L;
        Long sessionId = 100L;
        SessionDetailDTO mockDetail = createMockSessionDetail();

        when(studentContextHelper.getStudentId(any())).thenReturn(studentId);
        when(studentScheduleService.getSessionDetail(studentId, sessionId)).thenReturn(mockDetail);

        // When & Then
        mockMvc.perform(get("/api/v1/students/me/sessions/{sessionId}", sessionId)
                .header("Authorization", "Bearer " + studentJwtToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.sessionId").value(sessionId))
            .andExpect(jsonPath("$.data.classInfo").exists());
    }

    // Test 5: Get session detail - not enrolled (404)
    @Test
    void shouldReturn404_WhenStudentNotEnrolled() throws Exception {
        Long studentId = 1L;
        Long sessionId = 999L;

        when(studentContextHelper.getStudentId(any())).thenReturn(studentId);
        when(studentScheduleService.getSessionDetail(studentId, sessionId))
            .thenThrow(new ResourceNotFoundException("Session not found or you are not enrolled"));

        mockMvc.perform(get("/api/v1/students/me/sessions/{sessionId}", sessionId)
                .header("Authorization", "Bearer " + studentJwtToken))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false));
    }
}
```

---

### 11.3 Repository Tests

**Test Class**: `StudentSessionRepositoryTest`

**Test Cases**:

```java
@DataJpaTest
@ActiveProfiles("test")
class StudentSessionRepositoryTest {

    @Autowired
    private StudentSessionRepository studentSessionRepository;

    @Autowired
    private TestEntityManager entityManager;

    // Test 1: Find weekly schedule - success
    @Test
    void shouldFindWeeklySchedule_WhenStudentHasSessions() {
        // Given
        Student student = createAndSaveStudent();
        Session session1 = createAndSaveSession(LocalDate.of(2025, 11, 4));
        Session session2 = createAndSaveSession(LocalDate.of(2025, 11, 6));
        createAndSaveStudentSession(student, session1);
        createAndSaveStudentSession(student, session2);

        // When
        List<StudentSession> result = studentSessionRepository.findWeeklyScheduleByStudentId(
            student.getId(),
            LocalDate.of(2025, 11, 4),
            LocalDate.of(2025, 11, 10)
        );

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getSession()).isNotNull();
        assertThat(result.get(0).getSession().getTimeSlotTemplate()).isNotNull();
    }

    // Test 2: Find by student and session - success
    @Test
    void shouldFindStudentSession_WhenEnrolled() {
        // Given
        Student student = createAndSaveStudent();
        Session session = createAndSaveSession(LocalDate.of(2025, 11, 4));
        createAndSaveStudentSession(student, session);

        // When
        Optional<StudentSession> result = studentSessionRepository
            .findByStudentIdAndSessionId(student.getId(), session.getId());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getStudent().getId()).isEqualTo(student.getId());
    }

    // Test 3: Find by student and session - not enrolled
    @Test
    void shouldReturnEmpty_WhenStudentNotEnrolled() {
        // Given
        Student student = createAndSaveStudent();
        Session session = createAndSaveSession(LocalDate.of(2025, 11, 4));
        // No StudentSession created

        // When
        Optional<StudentSession> result = studentSessionRepository
            .findByStudentIdAndSessionId(student.getId(), session.getId());

        // Then
        assertThat(result).isEmpty();
    }
}
```

---

## 12. Performance Considerations

### 12.1 Query Optimization

**Problem**: N+1 queries when fetching sessions with related data

**Solution**: Use `JOIN FETCH` in queries
```java
@Query("SELECT ss FROM StudentSession ss " +
       "JOIN FETCH ss.session s " +
       "JOIN FETCH s.timeSlotTemplate " +
       "JOIN FETCH s.classEntity c " +
       "JOIN FETCH c.course " +
       "JOIN FETCH c.branch " +
       "JOIN FETCH s.courseSession " +
       "WHERE ...")
```

**Benefit**: All related data loaded in 1 query instead of N+1 queries

---

### 12.2 Database Indexes

**Required Indexes**:
```sql
CREATE INDEX idx_student_session_schedule ON student_session(student_id, session_id);
CREATE INDEX idx_session_date_range ON session(date, status);
CREATE INDEX idx_session_class ON session(class_id, date);
```

**Impact**: Faster filtering on `student_id` and `date` range queries

---

### 12.3 Response Size

**Weekly Schedule Response**:
- Typical: 5-10 sessions per week
- Each session: ~500 bytes (JSON)
- Total: ~5KB per request

**Session Detail Response**:
- Single session: ~2KB (with materials)

**Caching Strategy** (Future Enhancement):
- Cache weekly schedule for 5 minutes (rarely changes)
- Invalidate cache when:
  - Student enrolls/drops
  - Session status changes
  - Makeup session created

---

## 13. Deployment Checklist

### 13.1 Before Deployment

- [ ] All unit tests passing (`mvn test`)
- [ ] All integration tests passing (`mvn verify`)
- [ ] Code coverage > 80% for new code
- [ ] Database indexes created
- [ ] API documentation updated in Swagger
- [ ] Security review completed (authorization checks)
- [ ] Performance testing done (load test with 100 concurrent students)

### 13.2 Database Migration

**No schema changes required** - All entities already exist

**Optional Indexes**:
```sql
-- Run in production database
CREATE INDEX CONCURRENTLY idx_student_session_schedule
ON student_session(student_id, session_id);

CREATE INDEX CONCURRENTLY idx_session_date_range
ON session(date, status);
```

### 13.3 Environment Variables

**No new environment variables required** - Uses existing:
- `JWT_SECRET`: Already configured
- Database credentials: Already configured

---

## 14. API Documentation (Swagger)

### 14.1 Swagger Annotations

**Controller-Level**:
```java
@Tag(name = "Student Schedule", description = "APIs for students to view their class schedules")
@RestController
@RequestMapping("/api/v1/students")
public class StudentScheduleController {
```

**Method-Level**:
```java
@Operation(
    summary = "Get weekly schedule",
    description = "Get the authenticated student's class schedule for a specific week. " +
                  "Returns sessions organized by day and timeslot.",
    security = @SecurityRequirement(name = "bearerAuth")
)
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "Schedule retrieved successfully"),
    @ApiResponse(responseCode = "400", description = "Invalid weekStart parameter"),
    @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token"),
    @ApiResponse(responseCode = "403", description = "Forbidden - User is not a student")
})
@GetMapping("/me/schedule")
public ResponseEntity<ResponseObject<WeeklyScheduleResponseDTO>> getMySchedule(...) {
```

**DTO Documentation**:
```java
@Schema(description = "Weekly schedule response containing all sessions for a week")
public class WeeklyScheduleResponseDTO {

    @Schema(description = "Monday of the week", example = "2025-11-04")
    private LocalDate weekStart;

    @Schema(description = "Sunday of the week", example = "2025-11-10")
    private LocalDate weekEnd;

    @Schema(description = "Map of day of week to list of sessions")
    private Map<DayOfWeek, List<SessionSummaryDTO>> schedule;
}
```

---

## 15. Future Enhancements (Out of Scope for MVP)

### 15.1 Phase 2 Features

- [ ] **Filter by course**: Show only specific course's schedule
- [ ] **Export schedule**: Download as PDF or iCal format
- [ ] **Push notifications**: Remind students 1 day before class
- [ ] **Homework submission**: Submit homework directly from session detail modal
- [ ] **Attendance self-report**: Students report if they will be absent

### 15.2 Performance Enhancements

- [ ] **Response caching**: Redis cache for weekly schedules
- [ ] **Lazy loading materials**: Load materials only when modal opened
- [ ] **Pagination for history**: Paginate past sessions if needed

### 15.3 UX Enhancements

- [ ] **Today's focus panel**: Sidebar showing today's classes + upcoming
- [ ] **Quick stats**: Attendance rate, homework completion rate
- [ ] **Mobile app**: Native app with offline schedule caching

---

## 16. Success Metrics

### 16.1 Technical Metrics

- **API Response Time**: < 500ms for weekly schedule
- **Database Query Time**: < 100ms for week query
- **Test Coverage**: > 80% for new code
- **Error Rate**: < 1% of API calls

### 16.2 User Experience Metrics

- **Page Load Time**: < 2 seconds for weekly view
- **Modal Open Time**: < 500ms for session detail
- **User Engagement**: 70%+ of students view schedule weekly

---

## 17. Implementation Timeline

### 17.1 Estimated Effort

| Task | Effort | Priority |
|------|--------|----------|
| **Repository Queries** | 2 hours | P0 |
| **DTOs** | 3 hours | P0 |
| **Service Implementation** | 4 hours | P0 |
| **Controller Implementation** | 2 hours | P0 |
| **StudentContextHelper** | 1 hour | P0 |
| **Unit Tests** | 4 hours | P0 |
| **Integration Tests** | 3 hours | P0 |
| **Swagger Documentation** | 1 hour | P1 |
| **Code Review + Fixes** | 2 hours | P1 |
| **Total** | **22 hours (~3 days)** | |

### 17.2 Development Phases

**Phase 1: Core Implementation (Day 1)**
- Repository queries
- DTOs
- Service layer

**Phase 2: API Layer (Day 2)**
- Controller
- StudentContextHelper
- Basic testing

**Phase 3: Testing & Polish (Day 3)**
- Comprehensive tests
- Swagger docs
- Code review
- Bug fixes

---

## 18. Risk Assessment

### 18.1 Technical Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| **N+1 queries** | High | Use JOIN FETCH in all queries |
| **Slow date range queries** | Medium | Add database indexes on `date` field |
| **JWT token without student mapping** | High | StudentContextHelper validates user is student |
| **Large response size** | Low | Typical response < 10KB, acceptable |

### 18.2 Security Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| **Student accessing other students' data** | Critical | Always use `studentContextHelper.getStudentId()` from JWT |
| **Unauthorized role access** | High | `@PreAuthorize("hasRole('STUDENT')")` on all endpoints |
| **Session detail exposure** | Medium | Query filters by `student_id AND session_id` |

---

## 19. Rollback Plan

### 19.1 If Feature Fails

**Rollback Steps**:
1. Revert controller routes (disable endpoints)
2. No database rollback needed (no schema changes)
3. Keep code in separate branch for future retry

**Impact**: No impact on existing features (new endpoints only)

---

## 20. Approval & Sign-off

### 20.1 Stakeholders

- [ ] **Product Owner**: Approve UX/feature scope
- [ ] **Backend Lead**: Approve architecture & security
- [ ] **Frontend Lead**: Approve API contract
- [ ] **QA Lead**: Approve test strategy

### 20.2 Definition of Done

- [ ] All acceptance criteria met
- [ ] Code reviewed and approved
- [ ] All tests passing (unit + integration)
- [ ] API documentation complete
- [ ] Security review passed
- [ ] Performance benchmarks met
- [ ] Deployed to staging environment
- [ ] User acceptance testing (UAT) passed

---

## End of Plan

**Document Version**: 1.0
**Last Updated**: 2025-11-07
**Next Review**: After implementation completion
