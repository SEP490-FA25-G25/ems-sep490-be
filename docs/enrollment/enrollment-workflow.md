# ENROLLMENT WORKFLOW

**Purpose:** Academic Affairs enrolls students into scheduled classes via Excel import  
**Last Updated:** 2025-11-04

---

## BUSINESS FLOW

```
Sale (Offline) → Excel File → Academic Affairs (Online) → System Import → Enrollment Complete
```

### Actors & Responsibilities

| Role | Responsibility |
|------|---------------|
| **Sale** (offline) | Collect student info, group by class, create Excel file |
| **Academic Affairs** | Upload Excel, validate capacity, confirm enrollment |
| **System** | Parse Excel, resolve students, create enrollments + sessions |

### Prerequisites

**Class must be:**
- `approval_status = 'APPROVED'`
- `status = 'SCHEDULED'`
- Has sessions created from course template

**Students:**
- May exist in DB (found by code/email) or created on-the-fly
- Must not be already enrolled in same class

---

## USER JOURNEY

### 1. View Class List
**Endpoint:** `GET /api/v1/classes`  
**Filters:** Branch, status=SCHEDULED, course, modality  
**Display:** Class code, name, enrollment (15/20), start date  
**Action:** Click class → View detail

### 2. Class Detail Page
**Endpoint:** `GET /api/v1/classes/{classId}`  
**Display:** Class info, capacity, schedule, current students  
**Action:** Click "Enroll Students" → Opens modal

### 3. Enrollment Modal (3 Tabs)
- **Tab 1:** Select existing students (rare)
- **Tab 2:** Add single student (emergency)
- **Tab 3:** Import Excel ⭐ **PRIMARY METHOD**

### 4. Import Excel (Tab 3)
**Upload Excel:**
- **Endpoint:** `POST /api/v1/enrollments/classes/{classId}/import/preview`
- **Format:** `.xlsx` with columns: `student_code, full_name, email, phone, gender, dob, level`
- **Resolution:**
  - Has `student_code` → Find by code
  - No code → Find by email
  - Not found → Mark as `CREATE`
  - Error → Mark as `ERROR`

**Preview Response:**
- List of students with status (FOUND/CREATE/ERROR)
- Capacity analysis (15/20 enrolled, 10 to add → 25/20 = EXCEEDED)
- Recommendation: `OK | PARTIAL_SUGGESTED | OVERRIDE_AVAILABLE | BLOCKED`

### 5. Confirm Strategy
**Endpoint:** `POST /api/v1/enrollments/classes/{classId}/import/execute`

**Strategies:**
- `ALL`: Enroll all (if capacity OK)
- `PARTIAL`: Select specific students (provide `selectedStudentIds`)
- `OVERRIDE`: Exceed capacity (provide `overrideReason`, min 20 chars)

**System Processing:**
1. Lock class (pessimistic)
2. Create new students (if status=CREATE)
3. Create `enrollment` records with `enrolled_by`, `capacity_override`, `override_reason`
4. Auto-generate `student_session` for all future sessions (date >= today, status=PLANNED)
5. Log override actions (WARN level)

**Result:**
- Enrollment count
- Students created count
- Sessions generated per student
- Warnings

---

## KEY ENTITIES

### Enrollment
```sql
enrollment:
  - student_id (FK)
  - class_id (FK)
  - status (ENROLLED, DROPPED, COMPLETED)
  - enrolled_by (user_id of Academic Affairs)
  - enrolled_at (timestamp)
  - capacity_override (boolean, default false)
  - override_reason (text, required if override=true)
  - join_session_id (nullable, for mid-course enrollment)
```

### Student Session (Auto-generated)
```sql
student_session:
  - student_id (FK)
  - session_id (FK)
  - attendance_status (PLANNED, PRESENT, ABSENT, etc.)
  - is_makeup (boolean, default false)
```

**Generation Logic:**
- Cartesian product: enrolled students × future sessions
- Future = `session.date >= today AND session.status = 'PLANNED'`

---

## CAPACITY MANAGEMENT

### Calculation
```java
currentEnrolled = COUNT(enrollment WHERE class_id=X AND status='ENROLLED')
availableSlots = class.max_capacity - currentEnrolled
toEnroll = validStudentsFromExcel
exceededBy = toEnroll - availableSlots (if > 0)
```

### Recommendations

| Scenario | Recommendation | Action |
|----------|---------------|---------|
| `toEnroll <= availableSlots` | `OK` | Enroll all |
| `exceededBy <= 20%` | `OVERRIDE_AVAILABLE` | Allow override with reason |
| `exceededBy > 20%` | `PARTIAL_SUGGESTED` | Suggest partial enrollment |
| `exceededBy >> capacity` | `BLOCKED` | Reject |

### Override Audit
- Stored in `enrollment.capacity_override = true`
- Reason stored in `enrollment.override_reason`
- Logged: `"CAPACITY_OVERRIDE: Class {id} enrolling {count} students (capacity: {max}). Reason: {...}. Approved by user {userId}"`

---

## EXCEL FORMAT

### Columns (Order flexible, case-insensitive)
```
student_code | full_name | email | phone | gender | dob | level
-------------|-----------|-------|-------|--------|-----|------
ST001        | Nguyen A  | a@...| 0901..| male  |1995-01-15| A1
(empty)      | Tran B    | b@...| 0902..| female|1996-03-20| A1
```

**Notes:**
- `student_code` optional (empty = new student)
- `email` required, unique
- `gender`: male/female/other (case-insensitive, supports m/f/o)
- `dob`: yyyy-MM-dd, dd/MM/yyyy, MM/dd/yyyy
- Empty rows skipped

---

## ERROR HANDLING

### Validation Errors (ErrorCode)
- `EXCEL_FILE_EMPTY` (1205)
- `EXCEL_PARSE_FAILED` (1206)
- `CLASS_NOT_APPROVED` (1207)
- `CLASS_INVALID_STATUS` (1208)
- `NO_FUTURE_SESSIONS` (1209)
- `OVERRIDE_REASON_REQUIRED` (1210)
- `INVALID_ENROLLMENT_STRATEGY` (1211)
- `PARTIAL_STRATEGY_MISSING_IDS` (1212)
- `SELECTED_STUDENTS_EXCEED_CAPACITY` (1213)
- `INVALID_FILE_TYPE_XLSX` (1214)

### Student Resolution Errors
- Duplicate email in Excel → Status `DUPLICATE`
- Invalid email format → Status `ERROR`
- Missing required fields → Status `ERROR`
- Already enrolled → Status `ERROR`

---

## ALTERNATIVE WORKFLOWS

### Enroll Existing Students (Tab 1)
**Endpoint:** `POST /api/v1/enrollments/classes/{classId}/students`  
**Body:** `{ studentIds: [1, 2, 3] }`  
**Use:** Manual selection from available students

### Create Single Student (Tab 2)
**Endpoint:** `POST /api/v1/students`  
**Body:** Full student info  
**Auto-generated:** `student_code = "ST{branchId}{name}{random}"`, `password = "12345678"`  
**Use:** Emergency add before enrollment

### View Students in Class
**Endpoint:** `GET /api/v1/classes/{classId}/students`  
**Display:** Enrolled students, enrollment date, status

### View Branch Students
**Endpoint:** `GET /api/v1/students`  
**Filters:** Branch, search, status, course  
**Display:** Student list with enrollment history
