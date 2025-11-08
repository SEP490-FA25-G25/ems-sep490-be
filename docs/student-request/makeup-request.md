# Makeup Request Implementation Guide

**Version:** 1.0  
**Date:** 2025-11-07  
**Request Type:** MAKEUP  

---

## Overview

**Purpose:** Student xin học bù cho buổi đã nghỉ  
**Complexity:** Medium  
**Flow Support:** Dual (Self-Service + On-Behalf)  
**Business Impact:** Completion rate, content mastery  
**Key Feature:** Cross-class, cross-branch, cross-modality support

---

## 📱 Student UX Flow

### UX Principle
> **Smart Recommendations & Progressive Disclosure:** System suggests best makeup options based on branch, modality, and date. Student only sees relevant choices at each step.

### Flow Diagram
```
My Requests Page → [+ New Request] 
  → Modal: Choose Type (Makeup)
  → Step 1: Choose Scenario (Past absence / Future absence)
  → Step 2: Select Missed Session (from list)
  → Step 3: Select Makeup Session (ranked recommendations)
  → Step 4: Fill Form (Reason)
  → Submit → Success Message
```

---

### 🖥️ Screen 1: Choose Scenario

**Purpose:** Determine if student is making up a past absence or pre-registering for future absence

**UI Components:** `RadioGroup`, `Card`

**Layout:**
```
┌─────────────────────────────────────────────────────────┐
│ ← Back    Makeup Request                        [✕]     │
├─────────────────────────────────────────────────────────┤
│ Step 1 of 4                                             │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │
│                                                         │
│ Which situation applies to you?                         │
│                                                         │
│ ┌─────────────────────────────────────────────────┐   │
│ │ ○ I already missed a class                      │   │
│ │   Make up for a past absence                    │   │
│ └─────────────────────────────────────────────────┘   │
│                                                         │
│ ┌─────────────────────────────────────────────────┐   │
│ │ ○ I know I will miss a future class             │   │
│ │   Pre-register makeup for planned absence       │   │
│ └─────────────────────────────────────────────────┘   │
│                                                         │
│                                        [Cancel] [Next]  │
└─────────────────────────────────────────────────────────┘
```

**User Actions:**
1. Choose between past absence or future absence
2. Click [Next] → Go to Step 2

**No API Call** - Just UI state selection

---

### 🖥️ Screen 2: Select Missed Session

**Purpose:** Show list of missed sessions within eligible timeframe (default 4 weeks)

**UI Components:** `RadioGroup`, `Card`, `Badge`

**Layout:**
```
┌─────────────────────────────────────────────────────────┐
│ ← Back    Makeup Request                        [✕]     │
├─────────────────────────────────────────────────────────┤
│ Step 2 of 4                                             │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │
│                                                         │
│ Select the session you missed                           │
│                                                         │
│ You have 3 missed sessions in the last 4 weeks          │
│                                                         │
│ ┌─────────────────────────────────────────────────┐   │
│ │ ○ Session 12: Grammar                           │   │
│ │   CHN-A1-01 • Nov 3, 2025 • 5 days ago         │   │
│ └─────────────────────────────────────────────────┘   │
│                                                         │
│ ┌─────────────────────────────────────────────────┐   │
│ │ ○ Session 15: Vocabulary                        │   │
│ │   CHN-A1-01 • Oct 28, 2025 • 10 days ago       │   │
│ └─────────────────────────────────────────────────┘   │
│                                                         │
│ ┌─────────────────────────────────────────────────┐   │
│ │ ○ Session 10: Reading Comprehension             │   │
│ │   CHN-A1-01 • Oct 25, 2025 • 13 days ago       │   │
│ └─────────────────────────────────────────────────┘   │
│                                                         │
│                                        [Cancel] [Next]  │
└─────────────────────────────────────────────────────────┘
```

**User Actions:**
1. See list of eligible missed sessions (past 4 weeks)
2. Select one session
3. Click [Next] → Trigger API to find makeup options → Go to Step 3

**API Call Trigger:** When entering Step 2 (to load missed sessions)

---

### 🖥️ Screen 3: Select Makeup Session (Smart Recommendations)

**Purpose:** Show ranked makeup options with visual priority indicators

**UI Components:** `RadioGroup`, `Card`, `Badge`, `Separator`

**Layout:**
```
┌─────────────────────────────────────────────────────────┐
│ ← Back    Makeup Request                        [✕]     │
├─────────────────────────────────────────────────────────┤
│ Step 3 of 4                                             │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │
│                                                         │
│ Choose a makeup session                                 │
│ Missed: Session 12 - Grammar (Nov 3)                    │
│                                                         │
│ 🏆 Best Match                                           │
│ ┌─────────────────────────────────────────────────┐   │
│ │ ○ CHN-A1-02 • Session 12: Grammar               │   │
│ │   Nov 12 (Mon) • 14:00-16:00                    │   │
│ │   ⭐ Same Branch • 🏠 Offline • 5 slots left    │   │
│ └─────────────────────────────────────────────────┘   │
│                                                         │
│ Good Match                                              │
│ ┌─────────────────────────────────────────────────┐   │
│ │ ○ CHN-A1-ONLINE-01 • Session 12: Grammar        │   │
│ │   Nov 14 (Wed) • 18:00-20:00                    │   │
│ │   ⭐ Same Branch • Online • 7 slots left        │   │
│ └─────────────────────────────────────────────────┘   │
│                                                         │
│ Other Options                                           │
│ ┌─────────────────────────────────────────────────┐   │
│ │ ○ CHN-A1-NTH-01 • Session 12: Grammar           │   │
│ │   Nov 18 (Fri) • 08:00-10:00                    │   │
│ │   North Branch • 🏠 Offline • 6 slots left      │   │
│ └─────────────────────────────────────────────────┘   │
│                                                         │
│ ⓘ Can't find a suitable session? Contact AA            │
│                                        [Cancel] [Next]  │
└─────────────────────────────────────────────────────────┘
```

**User Actions:**
1. See ranked makeup options (Best Match → Good Match → Other Options)
2. Visual indicators: ⭐ (same branch), 🏠 (same modality)
3. Select one makeup session
4. Click [Next] → Go to Step 4

**API Call Trigger:** When student clicks [Next] from Step 2 (after selecting missed session)

**Ranking Algorithm:**
- **Best Match:** Same branch + same modality
- **Good Match:** Same branch OR same modality
- **Other Options:** Cross-branch, cross-modality

---

### 🖥️ Screen 4: Fill Form & Submit

**Purpose:** Confirm selection and provide reason

**UI Components:** `Card`, `Textarea`, `Button`

**Layout:**
```
┌─────────────────────────────────────────────────────────┐
│ ← Back    Makeup Request                        [✕]     │
├─────────────────────────────────────────────────────────┤
│ Step 4 of 4                                             │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │
│                                                         │
│ Confirm Your Makeup Request                             │
│                                                         │
│ Missed Session                                          │
│ ┌─────────────────────────────────────────────────┐   │
│ │ Session 12: Grammar                             │   │
│ │ CHN-A1-01 • Nov 3, 2025                         │   │
│ └─────────────────────────────────────────────────┘   │
│                                                         │
│ Makeup Session                                          │
│ ┌─────────────────────────────────────────────────┐   │
│ │ Session 12: Grammar                             │   │
│ │ CHN-A1-02 • Nov 12, 2025 (Mon)                  │   │
│ │ 14:00-16:00 • Central Branch • Offline          │   │
│ │ ⭐ Same Branch • 5 slots available              │   │
│ └─────────────────────────────────────────────────┘   │
│                                                         │
│ Reason *                                                │
│ ┌─────────────────────────────────────────────────┐   │
│ │ I missed Session 12 due to illness. I have      │   │
│ │ recovered and would like to make up...          │   │
│ └─────────────────────────────────────────────────┘   │
│ Minimum 10 characters (52/10)                           │
│                                                         │
│                                    [Cancel] [Submit]    │
└─────────────────────────────────────────────────────────┘
```

**User Actions:**
1. Review missed session and makeup session details
2. Enter reason (minimum 10 characters)
3. Click [Submit] → API call to create request

**Client-Side Validation:**
- Reason must be ≥ 10 characters
- Show character counter

**API Call Trigger:** When student clicks [Submit]

---

### 🖥️ Screen 5: Success State

**Purpose:** Confirm submission with important reminders

**Layout:**
```
┌─────────────────────────────────────────────────────────┐
│                         ✓                               │
│                                                         │
│          Makeup Request Submitted Successfully          │
│                                                         │
│     Your makeup request has been sent to                │
│     Academic Affairs for review.                        │
│                                                         │
│     Request ID: #043                                    │
│     Status: Pending                                     │
│                                                         │
│     Makeup Session Details:                             │
│     • Class: CHN-A1-02                                  │
│     • Date: Nov 12, 2025 (Monday)                       │
│     • Time: 14:00-16:00                                 │
│     • Location: Central Branch (Offline)                │
│                                                         │
│     ⓘ You'll receive confirmation email once approved   │
│                                                         │
│                            [View My Requests]           │
└─────────────────────────────────────────────────────────┘
```

---

## Business Rules

| Rule ID | Description | Enforcement |
|---------|-------------|-------------|
| BR-MKP-001 | Makeup chỉ cho buổi nghỉ trong X tuần gần nhất | Blocking |
| BR-MKP-002 | `course_session_id` must match (same content) | Blocking |
| BR-MKP-003 | Makeup class must have capacity | Blocking |
| BR-MKP-004 | Cross-class, cross-branch, cross-modality allowed | Feature |
| BR-MKP-005 | Bidirectional tracking (`original_session_id` ↔ `makeup_session_id`) | Data Integrity |
| BR-MKP-006 | No duplicate makeup request for same target session | Blocking |
| BR-MKP-007 | Reason required, min 10 chars | Blocking |
| BR-MKP-008 | No schedule conflict with student's other classes | Blocking |

**Configuration:**
```yaml
makeup_request:
  eligible_weeks_lookback: 4
  max_concurrent_pending: 3
  reason_min_length: 10
  priority_scoring:
    same_branch_weight: 10
    same_modality_weight: 5
    soonest_date_weight: 3
```

---

## API Endpoints

### 1. Get Missed Sessions

**When to Call:** When entering Step 2 (Select Missed Session screen)

**Purpose:** Load all eligible missed sessions within timeframe (default 4 weeks)

**Request:**
```http
GET /api/v1/students/me/missed-sessions?weeksBack=4&excludeRequested=true
Authorization: Bearer {access_token}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "totalCount": 3,
    "sessions": [
      {
        "sessionId": 1012,
        "date": "2025-11-03",
        "daysAgo": 5,
        "courseSessionNumber": 12,
        "courseSessionTitle": "Grammar",
        "courseSessionId": 120,
        "class": {
          "id": 101,
          "code": "CHN-A1-01",
          "name": "Chinese A1 - Morning Class"
        },
        "timeSlot": {
          "startTime": "08:00:00",
          "endTime": "10:00:00"
        },
        "attendanceStatus": "ABSENT",
        "sessionStatus": "COMPLETED",
        "hasExistingMakeupRequest": false
      }
    ]
  }
}
```

**Frontend Usage:**
```typescript
// Step 2: Load missed sessions on mount
useEffect(() => {
  const loadMissedSessions = async () => {
    const response = await fetch(
      '/api/v1/students/me/missed-sessions?weeksBack=4&excludeRequested=true',
      { headers: { Authorization: `Bearer ${token}` } }
    );
    const data = await response.json();
    
    setMissedSessions(data.data.sessions);
  };
  
  loadMissedSessions();
}, []);
```

---

### 2. Get Makeup Options

**When to Call:** When student selects a missed session and clicks [Next] from Step 2

**Purpose:** Find all available makeup sessions that match the missed session's content (courseSessionId)

**Request:**
```http
GET /api/v1/student-requests/makeup-options?targetSessionId=1012
Authorization: Bearer {access_token}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "targetSession": {
      "sessionId": 1012,
      "date": "2025-11-03",
      "courseSessionNumber": 12,
      "courseSessionTitle": "Grammar",
      "courseSessionId": 120,
      "class": {
        "id": 101,
        "code": "CHN-A1-01",
        "branchId": 1,
        "branchName": "Central Branch",
        "modality": "OFFLINE"
      }
    },
    "matchingAlgorithm": {
      "primaryCriteria": "Same course session content (courseSessionId = 120)",
      "priorityFactors": [
        "1. Same branch preferred",
        "2. Same modality preferred",
        "3. Soonest date (minimize learning gap)",
        "4. Most available slots"
      ]
    },
    "makeupOptions": [
      {
        "sessionId": 2012,
        "date": "2025-11-12",
        "daysFromNow": 9,
        "courseSessionNumber": 12,
        "courseSessionTitle": "Grammar",
        "courseSessionId": 120,
        "class": {
          "id": 102,
          "code": "CHN-A1-02",
          "name": "Chinese A1 - Afternoon Class",
          "branchId": 1,
          "branchName": "Central Branch",
          "modality": "OFFLINE",
          "maxCapacity": 20,
          "enrolledCount": 15,
          "availableSlots": 5
        },
        "timeSlot": {
          "startTime": "14:00:00",
          "endTime": "16:00:00"
        },
        "sessionStatus": "PLANNED",
        "matchScore": {
          "branchMatch": true,
          "modalityMatch": true,
          "priority": "HIGH"
        }
      },
      {
        "sessionId": 3012,
        "date": "2025-11-14",
        "daysFromNow": 11,
        "courseSessionNumber": 12,
        "courseSessionTitle": "Grammar",
        "courseSessionId": 120,
        "class": {
          "id": 203,
          "code": "CHN-A1-ONLINE-01",
          "name": "Chinese A1 - Online Evening",
          "branchId": 1,
          "branchName": "Central Branch",
          "modality": "ONLINE",
          "maxCapacity": 25,
          "enrolledCount": 18,
          "availableSlots": 7
        },
        "timeSlot": {
          "startTime": "18:00:00",
          "endTime": "20:00:00"
        },
        "sessionStatus": "PLANNED",
        "matchScore": {
          "branchMatch": true,
          "modalityMatch": false,
          "priority": "MEDIUM"
        }
      }
    ]
  }
}
```

**Frontend Usage:**
```typescript
// Step 2 → Step 3: When student selects missed session
const handleMissedSessionSelect = async (sessionId: number) => {
  setSelectedMissedSession(sessionId);
};

const handleNextToStep3 = async () => {
  // Call API to get makeup options
  const response = await fetch(
    `/api/v1/student-requests/makeup-options?targetSessionId=${selectedMissedSession}`,
    { headers: { Authorization: `Bearer ${token}` } }
  );
  const data = await response.json();
  
  // Group by priority for UI
  const bestMatch = data.data.makeupOptions.filter(o => o.matchScore.priority === 'HIGH');
  const goodMatch = data.data.makeupOptions.filter(o => o.matchScore.priority === 'MEDIUM');
  const others = data.data.makeupOptions.filter(o => o.matchScore.priority === 'LOW');
  
  setMakeupOptions({ bestMatch, goodMatch, others });
  goToStep(3);
};
```

**Important Notes:**
- All results have same `courseSessionId` (guaranteed by backend)
- Results are pre-sorted by priority score
- Visual indicators: `branchMatch` (⭐), `modalityMatch` (🏠)

---

### 3. Submit Makeup Request

**When to Call:** When student clicks [Submit] in Step 4

**Request:**
```http
POST /api/v1/student-requests
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "requestType": "MAKEUP",
  "currentClassId": 101,
  "targetSessionId": 1012,
  "makeupSessionId": 2012,
  "requestReason": "I missed Session 12 due to illness. I have recovered and would like to make up the content."
}
```

**Response:**
```json
{
  "success": true,
  "message": "Makeup request submitted successfully",
  "data": {
    "id": 43,
    "student": {
      "id": 123,
      "studentCode": "STU2024001",
      "fullName": "John Doe"
    },
    "requestType": "MAKEUP",
    "currentClass": {
      "id": 101,
      "code": "CHN-A1-01"
    },
    "targetSession": {
      "sessionId": 1012,
      "date": "2025-11-03",
      "courseSessionNumber": 12,
      "courseSessionTitle": "Grammar"
    },
    "makeupSession": {
      "sessionId": 2012,
      "date": "2025-11-12",
      "courseSessionNumber": 12,
      "courseSessionTitle": "Grammar",
      "class": {
        "id": 102,
        "code": "CHN-A1-02",
        "branchName": "Central Branch",
        "modality": "OFFLINE"
      },
      "timeSlot": {
        "startTime": "14:00:00",
        "endTime": "16:00:00"
      },
      "availableSlots": 5
    },
    "requestReason": "I missed Session 12 due to illness.",
    "status": "PENDING",
    "submittedAt": "2025-11-07T16:20:00+07:00"
  }
}
```

**Frontend Usage:**
```typescript
// Step 4: When student clicks Submit
const handleSubmit = async (formData: MakeupFormData) => {
  try {
    const response = await fetch('/api/v1/student-requests', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        requestType: 'MAKEUP',
        currentClassId: missedSession.class.id,
        targetSessionId: missedSession.sessionId,
        makeupSessionId: selectedMakeupSession.sessionId,
        requestReason: formData.reason
      })
    });
    
    const result = await response.json();
    
    if (result.success) {
      // Show success screen with makeup session details
      showSuccessDialog({
        requestId: result.data.id,
        makeupSession: result.data.makeupSession
      });
    }
  } catch (error) {
    // Handle errors
    if (error.message.includes('full')) {
      showError('This makeup session is now full. Please select another session.');
    } else {
      showError(error.message);
    }
  }
};
```

**Error Scenarios to Handle:**
- Session became full (race condition)
- Schedule conflict with student's other classes
- Duplicate request for same target session
- Session no longer in PLANNED status

---

### 4. Approve Request (Academic Affairs Only)

**Request:**
```http
PUT /api/v1/student-requests/{id}/approve
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "note": "Approved. Same branch and good reason provided."
}
```

**Response:**
```json
{
  "success": true,
  "message": "Makeup request approved and student added to makeup session",
  "data": {
    "request": {
      "id": 43,
      "status": "APPROVED",
      "decidedBy": {
        "id": 789,
        "fullName": "AA Staff Nguyen"
      },
      "decidedAt": "2025-11-07T17:00:00+07:00"
    },
    "studentSession": {
      "studentId": 123,
      "sessionId": 2012,
      "attendanceStatus": "PLANNED",
      "isMakeup": true,
      "makeupSessionId": 2012,
      "originalSessionId": 1012
    }
  }
}
```

---

## Database Schema

### Table: `student_request`

```sql
CREATE TABLE student_request (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES student(id),
    request_type request_type_enum NOT NULL, -- 'MAKEUP'
    current_class_id BIGINT NOT NULL REFERENCES class(id),
    target_session_id BIGINT NOT NULL REFERENCES session(id),
    makeup_session_id BIGINT REFERENCES session(id),
    request_reason TEXT NOT NULL,
    note TEXT,
    status request_status_enum NOT NULL DEFAULT 'pending',
    submitted_by BIGINT NOT NULL REFERENCES user(id),
    submitted_at TIMESTAMP NOT NULL DEFAULT NOW(),
    decided_by BIGINT REFERENCES user(id),
    decided_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    CONSTRAINT chk_makeup_valid CHECK (
        request_type != 'MAKEUP' OR (
            target_session_id IS NOT NULL AND
            makeup_session_id IS NOT NULL AND
            target_class_id IS NULL
        )
    )
);

CREATE INDEX idx_student_request_makeup_session ON student_request(makeup_session_id);
```

### Table: `student_session` (Enhanced)

```sql
CREATE TABLE student_session (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES student(id),
    session_id BIGINT NOT NULL REFERENCES session(id),
    attendance_status attendance_status_enum NOT NULL DEFAULT 'planned',
    is_makeup BOOLEAN NOT NULL DEFAULT FALSE,
    makeup_session_id BIGINT REFERENCES session(id), -- Forward reference
    original_session_id BIGINT REFERENCES session(id), -- Backlink
    note TEXT,
    recorded_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    UNIQUE(student_id, session_id)
);

CREATE INDEX idx_student_session_makeup ON student_session(is_makeup, makeup_session_id);
CREATE INDEX idx_student_session_original ON student_session(original_session_id);
```

---

## Matching Algorithm

### Query Logic

```java
// Find makeup sessions that match courseSessionId
public List<MakeupSessionDTO> findMakeupOptions(Long targetSessionId) {
    Session targetSession = sessionRepository.findById(targetSessionId)
        .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
    
    // Get course session ID (content identifier)
    Long courseSessionId = targetSession.getCourseSession().getId();
    
    // Find all future sessions with same course session
    List<Session> makeupSessions = sessionRepository
        .findByCourseSessionIdAndStatusAndDateAfter(
            courseSessionId, 
            SessionStatus.PLANNED, 
            LocalDate.now()
        );
    
    // Exclude student's own class
    makeupSessions = makeupSessions.stream()
        .filter(s -> !s.getClassEntity().getId().equals(targetSession.getClassEntity().getId()))
        .collect(Collectors.toList());
    
    // Score and sort
    return makeupSessions.stream()
        .map(s -> scoreMakeupSession(s, targetSession))
        .sorted(Comparator.comparingInt(MakeupSessionDTO::getScore).reversed())
        .collect(Collectors.toList());
}

private MakeupSessionDTO scoreMakeupSession(Session makeup, Session target) {
    int score = 0;
    
    // Same branch: +10 points
    if (makeup.getClassEntity().getBranch().getId()
            .equals(target.getClassEntity().getBranch().getId())) {
        score += 10;
    }
    
    // Same modality: +5 points
    if (makeup.getClassEntity().getModality()
            .equals(target.getClassEntity().getModality())) {
        score += 5;
    }
    
    // Soonest date: +3 points per week closer
    long weeksUntil = ChronoUnit.WEEKS.between(LocalDate.now(), makeup.getDate());
    score += Math.max(0, (4 - weeksUntil)) * 3;
    
    // More capacity: +1 point per 5 slots
    int availableSlots = makeup.getClassEntity().getMaxCapacity() 
                       - makeup.getClassEntity().getEnrollmentCount();
    score += (availableSlots / 5);
    
    return MakeupSessionDTO.builder()
        .session(makeup)
        .score(score)
        .priority(score >= 15 ? "HIGH" : score >= 8 ? "MEDIUM" : "LOW")
        .build();
}
```

---

## Backend Transaction Logic

### Validation (Submit)

```java
public StudentRequestResponseDTO submitMakeupRequest(MakeupRequestDTO dto) {
    // 1. Validate target session exists and is absent
    StudentSession targetStudentSession = studentSessionRepository
        .findByStudentIdAndSessionId(getCurrentUserId(), dto.getTargetSessionId())
        .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
    
    if (!targetStudentSession.getAttendanceStatus().equals(AttendanceStatus.ABSENT)) {
        throw new BusinessRuleException("Can only makeup absent sessions");
    }
    
    // 2. Check eligible timeframe (within X weeks)
    Session targetSession = targetStudentSession.getSession();
    long weeksAgo = ChronoUnit.WEEKS.between(targetSession.getDate(), LocalDate.now());
    if (weeksAgo > configProperties.getMakeup().getEligibleWeeksLookback()) {
        throw new BusinessRuleException("Session too old for makeup (limit: " + 
            configProperties.getMakeup().getEligibleWeeksLookback() + " weeks)");
    }
    
    // 3. Validate makeup session
    Session makeupSession = sessionRepository.findById(dto.getMakeupSessionId())
        .orElseThrow(() -> new ResourceNotFoundException("Makeup session not found"));
    
    if (!makeupSession.getStatus().equals(SessionStatus.PLANNED)) {
        throw new BusinessRuleException("Makeup session must be PLANNED");
    }
    
    if (makeupSession.getDate().isBefore(LocalDate.now())) {
        throw new BusinessRuleException("Makeup session must be in the future");
    }
    
    // 4. Validate course session match (CRITICAL)
    if (!targetSession.getCourseSession().getId()
            .equals(makeupSession.getCourseSession().getId())) {
        throw new BusinessRuleException("Makeup session must have same content (courseSessionId)");
    }
    
    // 5. Check capacity
    int enrolledCount = studentSessionRepository.countBySessionId(makeupSession.getId());
    if (enrolledCount >= makeupSession.getClassEntity().getMaxCapacity()) {
        throw new BusinessRuleException("Makeup session is full");
    }
    
    // 6. Check schedule conflict
    List<Session> studentSessions = sessionRepository.findByStudentIdAndDate(
        getCurrentUserId(), makeupSession.getDate());
    
    for (Session existing : studentSessions) {
        if (hasTimeOverlap(existing.getTimeSlot(), makeupSession.getTimeSlot())) {
            throw new BusinessRuleException("Schedule conflict with your other classes");
        }
    }
    
    // 7. Check duplicate request
    boolean hasDuplicate = studentRequestRepository.existsByStudentIdAndTargetSessionIdAndRequestTypeAndStatusIn(
        getCurrentUserId(), dto.getTargetSessionId(), RequestType.MAKEUP,
        List.of(RequestStatus.PENDING, RequestStatus.APPROVED));
    
    if (hasDuplicate) {
        throw new BusinessRuleException("Duplicate makeup request for this session");
    }
    
    // 8. Create request
    StudentRequest request = StudentRequest.builder()
        .student(studentRepository.getReferenceById(getCurrentUserId()))
        .requestType(RequestType.MAKEUP)
        .currentClass(classRepository.getReferenceById(dto.getCurrentClassId()))
        .targetSession(targetSession)
        .makeupSession(makeupSession)
        .requestReason(dto.getRequestReason())
        .status(RequestStatus.PENDING)
        .submittedBy(userRepository.getReferenceById(getCurrentUserId()))
        .submittedAt(LocalDateTime.now())
        .build();
    
    request = studentRequestRepository.save(request);
    
    // 9. Send notification
    notificationService.notifyAcademicAffair(request);
    
    return mapper.toResponseDTO(request);
}
```

### Approval Transaction

```java
@Transactional
public StudentRequestResponseDTO approveMakeupRequest(Long requestId, ApprovalDTO dto) {
    // 1. Load request
    StudentRequest request = studentRequestRepository.findById(requestId)
        .orElseThrow(() -> new ResourceNotFoundException("Request not found"));
    
    if (!request.getRequestType().equals(RequestType.MAKEUP)) {
        throw new BusinessRuleException("Not a makeup request");
    }
    
    if (!request.getStatus().equals(RequestStatus.PENDING)) {
        throw new BusinessRuleException("Request not in PENDING status");
    }
    
    // 2. Re-validate capacity (race condition check)
    int currentEnrolled = studentSessionRepository.countBySessionId(
        request.getMakeupSession().getId());
    
    if (currentEnrolled >= request.getMakeupSession().getClassEntity().getMaxCapacity()) {
        throw new BusinessRuleException("Makeup session became full");
    }
    
    // 3. Update request status
    request.setStatus(RequestStatus.APPROVED);
    request.setDecidedBy(userRepository.getReferenceById(getCurrentUserId()));
    request.setDecidedAt(LocalDateTime.now());
    request.setNote(dto.getNote());
    request = studentRequestRepository.save(request);
    
    // 4. Update original student_session note
    StudentSession originalStudentSession = studentSessionRepository
        .findByStudentIdAndSessionId(
            request.getStudent().getId(), 
            request.getTargetSession().getId())
        .orElseThrow(() -> new ResourceNotFoundException("Original session not found"));
    
    originalStudentSession.setNote(String.format(
        "Makeup approved: Session %d on %s. Request ID: %d",
        request.getMakeupSession().getCourseSession().getCourseSessionNumber(),
        request.getMakeupSession().getDate(),
        requestId));
    originalStudentSession.setMakeupSessionId(request.getMakeupSession().getId());
    studentSessionRepository.save(originalStudentSession);
    
    // 5. Create NEW student_session for makeup
    StudentSession makeupStudentSession = StudentSession.builder()
        .student(request.getStudent())
        .session(request.getMakeupSession())
        .attendanceStatus(AttendanceStatus.PLANNED)
        .isMakeup(true)
        .makeupSessionId(request.getMakeupSession().getId())
        .originalSessionId(request.getTargetSession().getId())
        .note("Makeup student from " + request.getCurrentClass().getCode())
        .build();
    
    studentSessionRepository.save(makeupStudentSession);
    
    // 6. Send notifications
    notificationService.notifyStudent(request, "approved");
    notificationService.notifyTeacher(request.getMakeupSession(), makeupStudentSession);
    
    return mapper.toResponseDTO(request);
}
```

---

## Status State Machine

```
[Student submits] → PENDING → [AA reviews] → APPROVED → [Create makeup student_session]
                                            → REJECTED
```

**Key Difference from Absence:**
- Approval creates a **NEW** `student_session` record in makeup class
- Bidirectional tracking: `original_session.makeup_session_id` ↔ `makeup_session.original_session_id`

---

## Teacher View (Makeup Student Badge)

### Query for Attendance Screen

```sql
SELECT 
    ss.id,
    s.student_code,
    s.full_name,
    ss.attendance_status,
    ss.is_makeup,
    ss.original_session_id,
    orig_class.code AS original_class_code
FROM student_session ss
JOIN student s ON ss.student_id = s.id
LEFT JOIN session orig_session ON ss.original_session_id = orig_session.id
LEFT JOIN class orig_class ON orig_session.class_id = orig_class.id
WHERE ss.session_id = ?
ORDER BY ss.is_makeup ASC, s.full_name ASC;
```

### UI Display

```
Student List:
┌──────────────────────────────────────────────────┐
│ 1. Alice Nguyen      [Present] [Absent]          │
│ 2. Bob Tran          [Present] [Absent]          │
│ ...                                              │
│ 15. Zoe Le           [Present] [Absent]          │
│ 16. 🏷️ John Doe      [Present] [Absent]         │
│     (Makeup from CHN-A1-01)                      │
└──────────────────────────────────────────────────┘
```

---

## Notifications

### Email to Student (Approved)

```
Subject: Your Makeup Request has been Approved

Dear {student_name},

Your makeup request has been approved!

Makeup Session Details:
- Class: {makeup_class_code}
- Branch: {branch_name}
- Date: {date} ({day_of_week})
- Time: {start_time} - {end_time}
- Location: {modality} {location_details}
- Teacher: {teacher_name}

Important:
- Join on time
- You'll be marked as "Makeup Student"
- Bring questions about the missed content

See you there!

Best regards,
Academic Affairs Team
```

### Email to Teacher (New Makeup Student)

```
Subject: New Makeup Student in Your Class (Session {number})

Dear {teacher_name},

You will have a makeup student in your session:

Session: Session {number} - {title}
Date: {date} {time}

Makeup Student:
- Name: {student_name} ({student_code})
- Original Class: {original_class_code}
- Reason: {reason}

Please:
- Include in attendance (badge shows automatically)
- Provide same attention as regular students

Thank you!
Academic Affairs Team
```

---

## Key Points for Implementation

1. **courseSessionId Matching:** CRITICAL - ensures same content
2. **Bidirectional Tracking:** Both original and makeup sessions reference each other
3. **Capacity Validation:** Check twice (submit + approve) to prevent race conditions
4. **Schedule Conflict Check:** Prevent double-booking student
5. **Teacher Notification:** Alert teacher about makeup student
6. **Separate Record:** Makeup has its own `student_session` with `is_makeup = TRUE`
7. **Cross-Flexibility:** Allow different branch/modality for max student convenience

---

**End of Makeup Request Guide**
