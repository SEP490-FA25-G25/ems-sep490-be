# Transfer Request Implementation Guide

**Version:** 2.0
**Date:** 2025-11-10
**Request Type:** TRANSFER
**Last Verified:** Against actual codebase implementation

---

## Overview

**Purpose:** Allow students to transfer between classes within the same course
**Key Constraint:** **ONE transfer per student per course** (enforced via business logic)
**Flow Support:**
- **Tier 1 (Self-Service):** Student changes schedule only (same branch + same modality) → 4-8 hours approval
- **Tier 2 (AA Direct):** AA creates transfer on-behalf for branch/modality changes → Immediate execution

**Business Impact:** Student retention, satisfaction, operational flexibility

---

## System Architecture

### Entities (Actual Implementation)

#### StudentRequest
**Path:** `src/main/java/org/fyp/tmssep490be/entities/StudentRequest.java`

| Field | Type | DB Column | Notes |
|-------|------|-----------|-------|
| `id` | Long | `id` | PK |
| `student` | Student | `student_id` | FK, NOT NULL |
| `currentClass` | ClassEntity | `current_class_id` | FK, nullable |
| `targetClass` | ClassEntity | `target_class_id` | FK, nullable |
| `requestType` | StudentRequestType | `request_type` | TRANSFER |
| `effectiveDate` | LocalDate | `effective_date` | Transfer effective date |
| `effectiveSession` | Session | `effective_session_id` | FK, first session in new class |
| `status` | RequestStatus | `status` | PENDING/APPROVED/REJECTED |
| `requestReason` | String | `request_reason` | Min 20 chars |
| `note` | String | `note` | AA staff notes |
| `submittedBy` | UserAccount | `submitted_by` | FK |
| `submittedAt` | OffsetDateTime | `submitted_at` | Timestamp |
| `decidedBy` | UserAccount | `decided_by` | FK, nullable |
| `decidedAt` | OffsetDateTime | `decided_at` | Timestamp, nullable |

**Relationships:**
- Student → StudentRequest (1:N)
- ClassEntity → StudentRequest (1:N, bidirectional: currentClass + targetClass)
- Session → StudentRequest (1:N)
- UserAccount → StudentRequest (1:N, bidirectional: submittedBy + decidedBy)

#### Enrollment
**Path:** `src/main/java/org/fyp/tmssep490be/entities/Enrollment.java`

| Field | Type | DB Column | Notes |
|-------|------|-----------|-------|
| `id` | Long | `id` | PK |
| `student` | Student | `student_id` | FK |
| `classEntity` | ClassEntity | `class_id` | FK |
| `status` | EnrollmentStatus | `status` | ENROLLED/TRANSFERRED/DROPPED/COMPLETED |
| `enrolledAt` | OffsetDateTime | `enrolled_at` | |
| `leftAt` | OffsetDateTime | `left_at` | Nullable |
| `joinSessionId` | Long | `join_session_id` | Session when joined |
| `leftSessionId` | Long | `left_session_id` | Session when left |
| `joinSession` | Session | `join_session_id` | FK |
| `leftSession` | Session | `left_session_id` | FK |

**Transfer Count Enforcement:**
- Currently: Business logic only (no DB constraint)
- Future: Add `transferCount` field with CHECK constraint `<= 1`

#### ClassEntity
**Path:** `src/main/java/org/fyp/tmssep490be/entities/ClassEntity.java`

Key fields: `branch`, `course`, `modality` (OFFLINE/ONLINE/HYBRID), `status` (SCHEDULED/ONGOING/COMPLETED), `maxCapacity`

#### Session
**Path:** `src/main/java/org/fyp/tmssep490be/entities/Session.java`

Key fields: `classEntity`, `courseSession`, `date`, `status` (PLANNED/CANCELLED/DONE)

#### StudentSession
**Path:** `src/main/java/org/fyp/tmssep490be/entities/StudentSession.java`

Key fields: `student`, `session`, `attendanceStatus`, `isMakeup`, `note`

### Enums (Actual Values)

| Enum | Path | Values |
|------|------|--------|
| StudentRequestType | `entities/enums/StudentRequestType.java` | ABSENCE, MAKEUP, **TRANSFER** |
| RequestStatus | `entities/enums/RequestStatus.java` | PENDING, WAITING_CONFIRM, APPROVED, REJECTED, CANCELLED |
| EnrollmentStatus | `entities/enums/EnrollmentStatus.java` | ENROLLED, **TRANSFERRED**, DROPPED, COMPLETED |
| ClassStatus | `entities/enums/ClassStatus.java` | DRAFT, **SCHEDULED**, ONGOING, COMPLETED, CANCELLED |
| Modality | `entities/enums/Modality.java` | OFFLINE, ONLINE, HYBRID |
| SessionStatus | `entities/enums/SessionStatus.java` | PLANNED, CANCELLED, DONE |
| AttendanceStatus | `entities/enums/AttendanceStatus.java` | PLANNED, PRESENT, ABSENT |

**Note:** ClassStatus uses `SCHEDULED` (not `PLANNED` as in older docs)

---

## Actor Journeys & API Flow

### TIER 1: Student Self-Service Transfer

**Conditions:**
- Same branch AND same modality
- Only schedule change (different time/days)

#### Journey Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                    TIER 1: STUDENT JOURNEY                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Step 1: Check Eligibility                                      │
│  ├─► API: GET /api/v1/students/me/transfer-eligibility         │
│  ├─► Shows: Classes with transfer quota (used/limit)           │
│  └─► Action: Click [Start Transfer] on eligible class          │
│                                                                 │
│  Step 2: Choose Transfer Type                                   │
│  ├─► UI Only: No API call                                       │
│  ├─► Shows: "Schedule Only" vs "Branch/Modality"               │
│  └─► If "Branch/Modality" → Show contact info (exit flow)      │
│                                                                 │
│  Step 3: Select Target Class (Tier 1 only)                     │
│  ├─► API: GET /api/v1/student-requests/transfer-options        │
│  │         ?currentClassId=101                                  │
│  ├─► Returns: Classes with same branch+modality                │
│  ├─► Shows: Content gap analysis with severity badges          │
│  └─► Action: Select target class → Next                        │
│                                                                 │
│  Step 4: Set Effective Date & Submit                           │
│  ├─► UI: Date picker (must be class session date)              │
│  ├─► UI: Reason textarea (min 20 chars)                        │
│  └─► API: POST /api/v1/student-requests                        │
│       Body: {                                                   │
│         "requestType": "TRANSFER",                              │
│         "currentClassId": 101,                                  │
│         "targetClassId": 103,                                   │
│         "effectiveDate": "2025-11-15",                          │
│         "requestReason": "...",                                 │
│         "note": ""                                              │
│       }                                                         │
│                                                                 │
│  Step 5: Success Confirmation                                   │
│  └─► Shows: Request ID, status, expected approval time         │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

### TIER 2: AA Direct Transfer (On-Behalf)

**Conditions:**
- Branch change OR modality change
- Student contacts AA outside system (phone/email/in-person)
- AA creates transfer directly

#### Journey Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                 TIER 2: AA ON-BEHALF JOURNEY                    │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Prerequisite: Student contacted AA via phone/email/office      │
│                                                                 │
│  Step 1: AA Dashboard → Find Student                            │
│  ├─► Navigate to student profile                               │
│  └─► View current enrollments                                  │
│                                                                 │
│  Step 2: Create Transfer Request                               │
│  ├─► Click [Create Transfer On-Behalf]                         │
│  ├─► API: GET /api/v1/classes?courseId=X&status=SCHEDULED      │
│  │         (Get all available classes for the course)          │
│  ├─► Select: Current class + Target class (any branch/modality)│
│  ├─► Enter: Effective date + Reason                            │
│  └─► API: POST /api/v1/student-requests/on-behalf              │
│       Body: {                                                   │
│         "studentId": 123,                                       │
│         "requestType": "TRANSFER",                              │
│         "currentClassId": 101,                                  │
│         "targetClassId": 301,                                   │
│         "effectiveDate": "2025-11-20",                          │
│         "requestReason": "Student relocating to North area"     │
│       }                                                         │
│                                                                 │
│  Step 3: Immediate Approval & Execution                        │
│  ├─► Status: PENDING → APPROVED (auto or manual)               │
│  ├─► API: PUT /api/v1/student-requests/{id}/approve            │
│  └─► System auto-executes transfer (see Transaction below)     │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## API Endpoints Specification

### 1. Check Transfer Eligibility

**Endpoint:** `GET /api/v1/students/me/transfer-eligibility`
**Auth:** Bearer token (student)
**Purpose:** Show which classes student can transfer from and remaining quota

**Response:**
```json
{
  "success": true,
  "data": {
    "currentEnrollments": [
      {
        "enrollmentId": 1001,
        "classId": 101,
        "classCode": "CHN-A1-01",
        "className": "Chinese A1 - Morning Class",
        "courseId": 10,
        "courseName": "Chinese A1",
        "branchId": 1,
        "branchName": "Central Branch",
        "modality": "OFFLINE",
        "enrollmentStatus": "ENROLLED",
        "transferQuota": {
          "used": 0,
          "limit": 1,
          "remaining": 1
        },
        "hasPendingTransfer": false,
        "canTransfer": true
      }
    ]
  }
}
```

**Business Rules:**
- `canTransfer = false` when:
  - `transferQuota.remaining = 0` (already transferred once)
  - `hasPendingTransfer = true` (existing PENDING/WAITING_CONFIRM request)
  - `enrollmentStatus != 'ENROLLED'`

---

### 2. Get Transfer Options (Tier 1)

**Endpoint:** `GET /api/v1/student-requests/transfer-options?currentClassId=101`
**Auth:** Bearer token (student)
**Purpose:** Get available target classes with same branch+modality and content gap analysis

**Response:**
```json
{
  "success": true,
  "data": {
    "currentClass": {
      "id": 101,
      "code": "CHN-A1-01",
      "name": "Morning Class",
      "courseId": 10,
      "branchId": 1,
      "branchName": "Central Branch",
      "modality": "OFFLINE",
      "currentSession": 12
    },
    "availableClasses": [
      {
        "classId": 102,
        "classCode": "CHN-A1-02",
        "className": "Afternoon Class",
        "branchId": 1,
        "branchName": "Central Branch",
        "modality": "OFFLINE",
        "scheduleDays": "Tue, Thu, Sat",
        "scheduleTime": "14:00-16:00",
        "currentSession": 14,
        "maxCapacity": 20,
        "enrolledCount": 16,
        "availableSlots": 4,
        "classStatus": "ONGOING",
        "contentGap": {
          "missedSessions": 2,
          "gapSessions": [
            {
              "courseSessionNumber": 13,
              "courseSessionTitle": "Listening Practice"
            },
            {
              "courseSessionNumber": 14,
              "courseSessionTitle": "Speaking Practice"
            }
          ],
          "severity": "MINOR",
          "recommendation": "You will miss 2 session(s). Review materials or request makeup."
        }
      }
    ]
  }
}
```

**Content Gap Severity:**
- `NONE`: 0 sessions missed
- `MINOR`: 1-2 sessions missed
- `MODERATE`: 3-5 sessions missed
- `MAJOR`: >5 sessions missed

**Filters Applied (Tier 1):**
- Same `courseId`
- Same `branchId`
- Same `modality`
- `status IN ('SCHEDULED', 'ONGOING')`
- Has available capacity
- Different schedule (different `scheduleDays` or `scheduleTime`)

---

### 3. Submit Transfer Request (Student - Tier 1)

**Endpoint:** `POST /api/v1/student-requests`
**Auth:** Bearer token (student)
**Purpose:** Student submits transfer request for schedule change

**Request Body:**
```json
{
  "requestType": "TRANSFER",
  "currentClassId": 101,
  "targetClassId": 103,
  "effectiveDate": "2025-11-15",
  "requestReason": "I need to change to evening schedule due to new work commitments starting next week.",
  "note": ""
}
```

**Validation Rules:**
1. `requestReason` min 20 characters
2. `effectiveDate` must be:
   - >= today
   - A valid session date in target class
3. Student must be ENROLLED in currentClass
4. Transfer quota not exceeded (business logic check)
5. No concurrent PENDING/WAITING_CONFIRM transfer requests
6. Target class must have capacity
7. Target class status must be SCHEDULED or ONGOING
8. Same course, same branch, same modality (Tier 1)

**Response:**
```json
{
  "success": true,
  "message": "Transfer request submitted successfully",
  "data": {
    "id": 44,
    "student": {
      "id": 123,
      "studentCode": "STU2024001",
      "fullName": "John Doe"
    },
    "requestType": "TRANSFER",
    "currentClass": {
      "id": 101,
      "code": "CHN-A1-01",
      "name": "Morning Class"
    },
    "targetClass": {
      "id": 103,
      "code": "CHN-A1-03",
      "name": "Evening Class"
    },
    "effectiveDate": "2025-11-15",
    "effectiveSession": {
      "sessionId": 3010,
      "courseSessionNumber": 13
    },
    "requestReason": "I need to change to evening schedule...",
    "status": "PENDING",
    "submittedAt": "2025-11-07T18:30:00+07:00"
  }
}
```

**Error Responses:**
- `400`: "Transfer quota exceeded. Maximum 1 transfer per course."
- `400`: "You already have a pending transfer request"
- `400`: "Target class is full"
- `400`: "No session on effective date"
- `400`: "Effective date must be in the future"

---

### 4. Create Transfer On-Behalf (AA - Tier 2)

**Endpoint:** `POST /api/v1/student-requests/on-behalf`
**Auth:** Bearer token (AA staff)
**Purpose:** AA creates transfer request for branch/modality changes after consultation

**Request Body:**
```json
{
  "studentId": 123,
  "requestType": "TRANSFER",
  "currentClassId": 101,
  "targetClassId": 301,
  "effectiveDate": "2025-11-20",
  "requestReason": "Student relocating to North area. Discussed via phone on Nov 8. Student confirmed preference for offline class at North Branch."
}
```

**Validation Rules:**
1. Same as student submission, but:
   - No branch/modality restriction (can change both)
   - AA can override some validations if needed
2. Transfer quota check still applies
3. Target class capacity check still applies

**Response:**
```json
{
  "success": true,
  "message": "Transfer request created on behalf of student",
  "data": {
    "id": 45,
    "student": {
      "id": 123,
      "studentCode": "STU2024001",
      "fullName": "John Doe"
    },
    "requestType": "TRANSFER",
    "currentClass": {
      "id": 101,
      "code": "CHN-A1-01"
    },
    "targetClass": {
      "id": 301,
      "code": "CHN-A1-NORTH-01",
      "branchName": "North Branch",
      "modality": "OFFLINE"
    },
    "effectiveDate": "2025-11-20",
    "status": "PENDING",
    "submittedAt": "2025-11-08T10:30:00+07:00",
    "submittedBy": {
      "id": 890,
      "fullName": "AA Staff Nguyen"
    }
  }
}
```

---

### 5. Approve Transfer Request (AA)

**Endpoint:** `PUT /api/v1/student-requests/{id}/approve`
**Auth:** Bearer token (AA staff)
**Purpose:** Approve transfer request and auto-execute transfer

**Request Body:**
```json
{
  "note": "Approved. Valid reason and no content gap issues."
}
```

**Response:**
```json
{
  "success": true,
  "message": "Transfer approved and executed successfully",
  "data": {
    "request": {
      "id": 44,
      "status": "APPROVED",
      "decidedAt": "2025-11-07T19:00:00+07:00",
      "decidedBy": {
        "id": 789,
        "fullName": "AA Staff Nguyen"
      }
    },
    "enrollmentChanges": {
      "oldEnrollment": {
        "id": 1001,
        "classId": 101,
        "status": "TRANSFERRED",
        "leftAt": "2025-11-07T19:00:00+07:00",
        "leftSessionId": 1012
      },
      "newEnrollment": {
        "id": 1050,
        "classId": 103,
        "status": "ENROLLED",
        "enrolledAt": "2025-11-07T19:00:00+07:00",
        "joinSessionId": 3010
      }
    }
  }
}
```

**Auto-Execution Transaction (see Backend Logic section)**

---

### 6. Get Available Classes (AA - For On-Behalf)

**Endpoint:** `GET /api/v1/classes?courseId=10&status=SCHEDULED,ONGOING&hasCapacity=true`
**Auth:** Bearer token (AA staff)
**Purpose:** Get all available classes for a course (no branch/modality restriction)

**Response:**
```json
{
  "success": true,
  "data": {
    "classes": [
      {
        "id": 301,
        "code": "CHN-A1-NORTH-01",
        "name": "Chinese A1 - North Branch Morning",
        "branchName": "North Branch",
        "modality": "OFFLINE",
        "scheduleDays": "Mon, Wed, Fri",
        "scheduleTime": "08:00-10:00",
        "currentSession": 11,
        "maxCapacity": 20,
        "enrolledCount": 15,
        "availableSlots": 5,
        "status": "ONGOING"
      }
    ]
  }
}
```

---

## Business Rules

| Rule ID | Description | Enforcement |
|---------|-------------|-------------|
| BR-TRF-001 | **ONE transfer per student per course** | Business logic (count APPROVED transfers) |
| BR-TRF-002 | Both classes must have same `courseId` | Blocking |
| BR-TRF-003 | Target class must have capacity | Blocking |
| BR-TRF-004 | Target class `status IN ('SCHEDULED', 'ONGOING')` | Blocking |
| BR-TRF-005 | Effective date must be >= CURRENT_DATE | Blocking |
| BR-TRF-006 | Effective date must be a class session date | Blocking |
| BR-TRF-007 | No concurrent transfer requests | Blocking |
| BR-TRF-008 | Content gap detection and warning | Warning only |
| BR-TRF-009 | Transfer reason min 20 characters | Blocking |
| BR-TRF-010 | Tier 1: Same branch AND same modality | Blocking (student UI) |
| BR-TRF-011 | Tier 2: AA can change branch OR modality | Allowed (AA only) |
| BR-TRF-012 | Preserve audit trail (status updates only) | Data Integrity |

---

## Backend Implementation Logic

### Transfer Quota Check Algorithm

```java
public boolean hasTransferQuotaRemaining(Long studentId, Long courseId) {
    // Count approved transfers for this student+course
    long approvedTransfers = studentRequestRepository
        .countByStudentIdAndRequestTypeAndCurrentClassCourseIdAndStatus(
            studentId,
            StudentRequestType.TRANSFER,
            courseId,
            RequestStatus.APPROVED
        );

    return approvedTransfers < 1; // Limit: 1 transfer per course
}
```

### Content Gap Analysis Algorithm

```java
public ContentGapDTO analyzeContentGap(Long currentClassId, Long targetClassId) {
    // 1. Get completed course sessions in current class
    List<Session> completedSessions = sessionRepository
        .findByClassIdAndStatusIn(currentClassId,
            List.of(SessionStatus.DONE, SessionStatus.CANCELLED));

    Set<Long> completedCourseSessionIds = completedSessions.stream()
        .map(s -> s.getCourseSession().getId())
        .collect(Collectors.toSet());

    // 2. Get target class's past sessions (already happened)
    List<Session> targetPastSessions = sessionRepository
        .findByClassIdAndDateBefore(targetClassId, LocalDate.now());

    // 3. Find gap: sessions target class covered but current class hasn't
    List<Session> gapSessions = targetPastSessions.stream()
        .filter(s -> !completedCourseSessionIds.contains(s.getCourseSession().getId()))
        .collect(Collectors.toList());

    // 4. Calculate severity
    int gapCount = gapSessions.size();
    String severity = gapCount == 0 ? "NONE" :
                      gapCount <= 2 ? "MINOR" :
                      gapCount <= 5 ? "MODERATE" : "MAJOR";

    return ContentGapDTO.builder()
        .missedSessions(gapCount)
        .gapSessions(gapSessions.stream()
            .map(s -> new GapSessionDTO(
                s.getCourseSession().getCourseSessionNumber(),
                s.getCourseSession().getCourseSessionTitle()))
            .collect(Collectors.toList()))
        .severity(severity)
        .recommendation(generateRecommendation(severity, gapCount))
        .build();
}
```

### Submit Transfer Request (Student - Tier 1)

```java
@Transactional
public StudentRequestResponseDTO submitTransferRequest(TransferRequestDTO dto) {
    // 1. Validate enrollment
    Enrollment currentEnrollment = enrollmentRepository
        .findByStudentIdAndClassIdAndStatus(
            getCurrentUserId(), dto.getCurrentClassId(), EnrollmentStatus.ENROLLED)
        .orElseThrow(() -> new BusinessRuleException("Not enrolled in current class"));

    // 2. Check transfer quota
    if (!hasTransferQuotaRemaining(getCurrentUserId(), currentEnrollment.getClassEntity().getCourse().getId())) {
        throw new BusinessRuleException("Transfer quota exceeded. Maximum 1 transfer per course.");
    }

    // 3. Check concurrent requests
    boolean hasPendingTransfer = studentRequestRepository
        .existsByStudentIdAndRequestTypeAndStatusIn(
            getCurrentUserId(),
            StudentRequestType.TRANSFER,
            List.of(RequestStatus.PENDING, RequestStatus.WAITING_CONFIRM));

    if (hasPendingTransfer) {
        throw new BusinessRuleException("You already have a pending transfer request");
    }

    // 4. Validate target class
    ClassEntity targetClass = classRepository.findById(dto.getTargetClassId())
        .orElseThrow(() -> new ResourceNotFoundException("Target class not found"));

    if (!targetClass.getCourse().getId().equals(currentEnrollment.getClassEntity().getCourse().getId())) {
        throw new BusinessRuleException("Target class must be for the same course");
    }

    if (!List.of(ClassStatus.SCHEDULED, ClassStatus.ONGOING).contains(targetClass.getStatus())) {
        throw new BusinessRuleException("Target class must be SCHEDULED or ONGOING");
    }

    // 5. Check capacity
    int enrolledCount = enrollmentRepository.countByClassIdAndStatus(
        dto.getTargetClassId(), EnrollmentStatus.ENROLLED);

    if (enrolledCount >= targetClass.getMaxCapacity()) {
        throw new BusinessRuleException("Target class is full");
    }

    // 6. Validate effective date
    if (dto.getEffectiveDate().isBefore(LocalDate.now())) {
        throw new BusinessRuleException("Effective date must be in the future");
    }

    Session effectiveSession = sessionRepository
        .findByClassEntityIdAndDate(dto.getTargetClassId(), dto.getEffectiveDate())
        .orElseThrow(() -> new BusinessRuleException("No session on effective date"));

    // 7. Tier 1 validation (student submission only)
    ClassEntity currentClass = currentEnrollment.getClassEntity();
    boolean sameBranch = currentClass.getBranch().getId().equals(targetClass.getBranch().getId());
    boolean sameModality = currentClass.getModality().equals(targetClass.getModality());

    if (!sameBranch || !sameModality) {
        throw new BusinessRuleException(
            "You can only change schedule. For branch/modality changes, contact Academic Affairs.");
    }

    // 8. Create request
    StudentRequest request = StudentRequest.builder()
        .student(studentRepository.getReferenceById(getCurrentUserId()))
        .requestType(StudentRequestType.TRANSFER)
        .currentClass(currentClass)
        .targetClass(targetClass)
        .effectiveDate(dto.getEffectiveDate())
        .effectiveSession(effectiveSession)
        .requestReason(dto.getRequestReason())
        .note(dto.getNote())
        .status(RequestStatus.PENDING)
        .submittedBy(userRepository.getReferenceById(getCurrentUserId()))
        .submittedAt(OffsetDateTime.now())
        .build();

    request = studentRequestRepository.save(request);

    // 9. Send notification to AA
    notificationService.notifyAcademicAffair(request);

    return mapper.toResponseDTO(request);
}
```

### Approve Transfer Request & Auto-Execute

```java
@Transactional
public StudentRequestResponseDTO approveTransferRequest(Long requestId, ApprovalDTO dto) {
    // 1. Load request
    StudentRequest request = studentRequestRepository.findById(requestId)
        .orElseThrow(() -> new ResourceNotFoundException("Request not found"));

    if (!request.getRequestType().equals(StudentRequestType.TRANSFER)) {
        throw new BusinessRuleException("Not a transfer request");
    }

    if (!request.getStatus().equals(RequestStatus.PENDING)) {
        throw new BusinessRuleException("Request not in PENDING status");
    }

    // 2. Re-validate capacity (race condition check)
    int currentEnrolled = enrollmentRepository.countByClassIdAndStatus(
        request.getTargetClass().getId(), EnrollmentStatus.ENROLLED);

    if (currentEnrolled >= request.getTargetClass().getMaxCapacity()) {
        throw new BusinessRuleException("Target class became full");
    }

    // 3. Update request status
    request.setStatus(RequestStatus.APPROVED);
    request.setDecidedBy(userRepository.getReferenceById(getCurrentUserId()));
    request.setDecidedAt(OffsetDateTime.now());
    request.setNote(dto.getNote());
    request = studentRequestRepository.save(request);

    // 4. Execute transfer
    executeTransfer(request);

    return mapper.toResponseDTO(request);
}

@Transactional
private void executeTransfer(StudentRequest request) {
    Long studentId = request.getStudent().getId();
    Long currentClassId = request.getCurrentClass().getId();
    Long targetClassId = request.getTargetClass().getId();
    LocalDate effectiveDate = request.getEffectiveDate();

    // 1. Update old enrollment
    Enrollment oldEnrollment = enrollmentRepository
        .findByStudentIdAndClassIdAndStatus(studentId, currentClassId, EnrollmentStatus.ENROLLED)
        .orElseThrow(() -> new ResourceNotFoundException("Old enrollment not found"));

    Session lastSession = sessionRepository
        .findByClassEntityIdAndDateBefore(currentClassId, effectiveDate)
        .stream()
        .max(Comparator.comparing(Session::getDate))
        .orElse(null);

    oldEnrollment.setStatus(EnrollmentStatus.TRANSFERRED);
    oldEnrollment.setLeftAt(OffsetDateTime.now());
    oldEnrollment.setLeftSessionId(lastSession != null ? lastSession.getId() : null);
    enrollmentRepository.save(oldEnrollment);

    // 2. Create new enrollment
    Enrollment newEnrollment = Enrollment.builder()
        .studentId(request.getStudent().getId())
        .classId(request.getTargetClass().getId())
        .status(EnrollmentStatus.ENROLLED)
        .enrolledAt(OffsetDateTime.now())
        .joinSessionId(request.getEffectiveSession().getId())
        .build();

    newEnrollment = enrollmentRepository.save(newEnrollment);

    // 3. Update old future student_sessions (mark as ABSENT)
    List<StudentSession> oldFutureSessions = studentSessionRepository
        .findByStudentIdAndSessionClassIdAndSessionDateGreaterThanEqual(
            studentId, currentClassId, effectiveDate);

    for (StudentSession oldSession : oldFutureSessions) {
        oldSession.setAttendanceStatus(AttendanceStatus.ABSENT);
        oldSession.setNote("Transferred to " + request.getTargetClass().getCode() +
            " on " + effectiveDate);
    }
    studentSessionRepository.saveAll(oldFutureSessions);

    // 4. Create new student_sessions for target class (all future sessions)
    List<Session> newFutureSessions = sessionRepository
        .findByClassEntityIdAndDateGreaterThanEqualAndStatusOrderByDateAsc(
            targetClassId, effectiveDate, SessionStatus.PLANNED);

    List<StudentSession> newStudentSessions = newFutureSessions.stream()
        .map(session -> {
            StudentSession ss = new StudentSession();
            StudentSessionId id = new StudentSessionId();
            id.setStudentId(studentId);
            id.setSessionId(session.getId());
            ss.setId(id);
            ss.setStudent(request.getStudent());
            ss.setSession(session);
            ss.setAttendanceStatus(AttendanceStatus.PLANNED);
            ss.setIsMakeup(false);
            ss.setNote("Joined via transfer from " + request.getCurrentClass().getCode());
            return ss;
        })
        .collect(Collectors.toList());

    studentSessionRepository.saveAll(newStudentSessions);

    // 5. Send notifications
    notificationService.notifyStudent(request, "approved");
    notificationService.notifyTeacher(request.getCurrentClass(), "student_left", request.getStudent());
    notificationService.notifyTeacher(request.getTargetClass(), "student_joined", request.getStudent());
}
```

---

## Status State Machine

### Tier 1 (Student Self-Service)
```
[Student submits] → PENDING → [AA approves] → APPROVED → [Auto-execute]
                              ↓
                         [AA rejects] → REJECTED
```

### Tier 2 (AA On-Behalf)
```
[AA creates on-behalf] → PENDING → [AA approves] → APPROVED → [Auto-execute]
                                   ↓
                              [AA rejects] → REJECTED
```

**Key States:**
- `PENDING`: Waiting for AA review
- `APPROVED`: Approved and executed
- `REJECTED`: Rejected by AA
- `CANCELLED`: Cancelled by student (before AA decision)

---

## UI Components & Validation

### Client-Side Validation (React/TypeScript)

#### Effective Date Validation
```typescript
const validateEffectiveDate = (date: Date, targetClass: Class) => {
  const dayOfWeek = date.getDay(); // 0=Sunday, 1=Monday

  // Check if date matches target class schedule days
  // e.g., targetClass.scheduleDays = [1, 3, 5] (Mon, Wed, Fri)
  if (!targetClass.scheduleDays.includes(dayOfWeek)) {
    return {
      valid: false,
      message: `Selected date is not a class day. Class meets on ${getScheduleDayNames(targetClass.scheduleDays)}`
    };
  }

  if (date < new Date()) {
    return { valid: false, message: 'Effective date must be in the future' };
  }

  return { valid: true };
};
```

#### Reason Validation
```typescript
const validateReason = (reason: string): { valid: boolean; message?: string } => {
  if (reason.trim().length < 20) {
    return {
      valid: false,
      message: `Reason must be at least 20 characters (current: ${reason.trim().length})`
    };
  }
  return { valid: true };
};
```

### Content Gap Severity Badge

```typescript
const getSeverityBadge = (severity: string, count: number) => {
  switch (severity) {
    case 'NONE':
      return <Badge variant="success">✅ No Content Gap</Badge>;
    case 'MINOR':
      return <Badge variant="warning">⚠️ Minor Gap: {count} sessions</Badge>;
    case 'MODERATE':
      return <Badge variant="warning">⚠️ Moderate Gap: {count} sessions</Badge>;
    case 'MAJOR':
      return <Badge variant="destructive">🛑 Major Gap: {count} sessions</Badge>;
  }
};
```

---

## Database Indexes for Performance

```sql
-- Enrollment queries
CREATE INDEX idx_enrollment_student_class_status ON enrollment(student_id, class_id, status);

-- StudentRequest queries
CREATE INDEX idx_student_request_student_type_status ON student_request(student_id, request_type, status);
CREATE INDEX idx_student_request_status ON student_request(status);

-- Session queries
CREATE INDEX idx_session_class_date ON session(class_id, date);
CREATE INDEX idx_session_class_status_date ON session(class_id, status, date);

-- StudentSession queries
CREATE INDEX idx_student_session_student_session ON student_session(student_id, session_id);
```

---

## Notifications

### Email to Student (Approved)
```
Subject: Your Transfer Request has been Approved

Dear {student_name},

Your transfer request has been approved!

Transfer Details:
• From: {current_class_code} - {current_class_name}
• To: {target_class_code} - {target_class_name}
• Effective Date: {effective_date}

New Class Information:
• Branch: {branch_name}
• Schedule: {schedule_days} • {schedule_time}
• Teacher: {teacher_name}
• Location: {modality}

Important:
- Your first class in the new schedule is on {effective_date}
- Your schedule has been updated automatically
- Contact Academic Affairs if you have questions

Welcome to your new class!

Best regards,
Academic Affairs Team
```

### Email to Old Teacher
```
Subject: Student Transfer Notice - {student_name}

Dear {teacher_name},

A student will be leaving your class:

Student: {student_name} ({student_code})
Last Class Date: {last_session_date}
Reason: Transfer to {target_class_code}

Please update your records accordingly.

Thank you,
Academic Affairs Team
```

### Email to New Teacher
```
Subject: New Student Joining Your Class - {student_name}

Dear {teacher_name},

A new student will join your class via transfer:

Student: {student_name} ({student_code})
First Class Date: {effective_date}
Previous Class: {current_class_code}

Please welcome the student and provide any catch-up materials if needed.

Thank you,
Academic Affairs Team
```

---

## Key Implementation Notes

1. **Transfer Quota:** Currently enforced via business logic (count approved transfers). Future: Add `transferCount` field to Enrollment with CHECK constraint.

2. **Race Conditions:** Capacity is re-checked in approval transaction with pessimistic locking on target class.

3. **Audit Trail:** All status changes preserved. Old enrollment marked TRANSFERRED, new enrollment created with ENROLLED status.

4. **Content Gap:** Calculated on-demand, not stored. Based on comparing completed course_sessions between classes.

5. **Tier Detection:**
   - Tier 1: Student submits (same branch + modality only)
   - Tier 2: AA creates on-behalf (any branch/modality)

6. **No Soft Delete:** Enrollments and requests are never deleted. Status updates only.

7. **Effective Date:** Must be a future class session date in target class. System creates StudentSessions for all future sessions starting from effective date.

8. **Notification Timing:** Sent after successful transaction commit to avoid inconsistent state.

---

## Error Handling

### Common Error Codes

| HTTP Status | Error Code | Message |
|-------------|------------|---------|
| 400 | TRF_QUOTA_EXCEEDED | "Transfer quota exceeded. Maximum 1 transfer per course." |
| 400 | TRF_PENDING_EXISTS | "You already have a pending transfer request" |
| 400 | TRF_CLASS_FULL | "Target class is full" |
| 400 | TRF_INVALID_DATE | "No session on effective date" |
| 400 | TRF_PAST_DATE | "Effective date must be in the future" |
| 400 | TRF_TIER_VIOLATION | "You can only change schedule. Contact AA for branch/modality changes." |
| 400 | TRF_SAME_CLASS | "Cannot transfer to the same class" |
| 400 | TRF_DIFFERENT_COURSE | "Target class must be for the same course" |
| 400 | TRF_CLASS_STATUS | "Target class must be SCHEDULED or ONGOING" |
| 404 | TRF_CLASS_NOT_FOUND | "Target class not found" |
| 404 | TRF_ENROLLMENT_NOT_FOUND | "Not enrolled in current class" |
| 409 | TRF_CONCURRENT_UPDATE | "Target class became full. Please select another class." |

---

## Testing Scenarios

### Unit Tests

1. **Transfer Quota:**
   - ✅ Student with 0 transfers → Can transfer
   - ✅ Student with 1 approved transfer → Cannot transfer
   - ✅ Student with 1 pending transfer → Cannot submit new

2. **Content Gap:**
   - ✅ Same progress → No gap
   - ✅ Target ahead by 2 sessions → Minor gap
   - ✅ Target ahead by 5 sessions → Moderate gap
   - ✅ Target ahead by 8 sessions → Major gap

3. **Tier Validation:**
   - ✅ Same branch + modality → Tier 1 allowed
   - ✅ Different branch → Tier 1 blocked
   - ✅ Different modality → Tier 1 blocked
   - ✅ AA on-behalf → Any class allowed

4. **Effective Date:**
   - ✅ Past date → Rejected
   - ✅ Non-session date → Rejected
   - ✅ Valid future session date → Accepted

### Integration Tests

1. **End-to-End Transfer:**
   - Submit request → Approve → Verify enrollments
   - Verify old StudentSessions marked ABSENT
   - Verify new StudentSessions created

2. **Concurrency:**
   - Two students transfer to same class with 1 slot left
   - Only first should succeed

3. **Notifications:**
   - Verify emails sent to student, old teacher, new teacher

---

**End of Transfer Request Implementation Guide**
