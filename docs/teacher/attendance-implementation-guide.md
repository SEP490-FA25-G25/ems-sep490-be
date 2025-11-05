# ATTENDANCE IMPLEMENTATION GUIDE

Version: 1.0  
Last Updated: 2025-11-03  
Author: Technical Team

---

## MỤC LỤC

1. [Tổng quan](#1-tổng-quan)
2. [Actors & Scope](#2-actors--scope)
3. [Pre/Post-conditions](#3-prepost-conditions)
4. [Luồng nghiệp vụ chi tiết](#4-luồng-nghiệp-vụ-chi-tiết)
   - [4.1 Take Attendance](#41-take-attendance)
   - [4.2 Save Attendance](#42-save-attendance)
   - [4.3 Session Report](#43-session-report)
   - [4.4 Edit Attendance](#44-edit-attendance)
5. [DTOs & Payloads](#5-dtos--payloads)
6. [API Endpoints](#6-api-endpoints)
7. [Service Logic](#7-service-logic)
8. [Database Schema Details](#8-database-schema-details)
9. [Business Rules](#9-business-rules)
10. [Edge Cases & Error Handling](#10-edge-cases--error-handling)
11. [SQL Snippets for Validation](#11-sql-snippets-for-validation)
12. [Testing Strategy](#12-testing-strategy)
13. [Security & Authorization](#13-security--authorization)
14. [Performance Notes](#14-performance-notes)

---

## 1. TỔNG QUAN

Hướng dẫn chi tiết triển khai luồng Attendance: Take Attendance, Save Attendance, Session Report. Tài liệu này bám sát `docs/teacher/attendance-workflow-implementation-plan.md` và mở rộng thành hướng dẫn triển khai kèm DTOs, API, logic dịch vụ, và SQL kiểm tra.

---

## 2. ACTORS & SCOPE

- **Teacher:** điểm danh students, ghi nhận homework status, submit session report.
- **System:** kiểm tra time constraints, cập nhật attendance records, update session status.

Scope backend: Controller + Service + Repository + Validation logic.

---

## 3. PRE/POST-CONDITIONS

**Pre:**

- JWT chạy, role `TEACHER` cấu hình đầy đủ.
- DB đã khởi tạo (`enum-init.sql` + `schema.sql`), seed có dữ liệu tối thiểu (sessions, students, teaching_slots).

**Post:**

- `student_session` records phản ánh đúng attendance và homework status; `session.status` và `session.teacher_note` được cập nhật khi Submit Report.

---

## 4. LUỒNG NGHIỆP VỤ CHI TIẾT

### 4.0 User Journey (Overview)

```
┌──────────────────────────────────────────────────────────────────┐
│ TEACHER (Trong Hệ Thống)                                         │
├──────────────────────────────────────────────────────────────────┤
│ Take Attendance Tab (xem sessions hôm nay)                      │
│ 1. Xem danh sách sessions đã và đang diễn ra                    │
│ 2. Chọn session muốn điểm danh                                    │
│                                                                  │
│ Attendance Page (điểm danh)                                      │
│ 3. Xem danh sách students                                         │
│ 4. Điểm danh (Present/Absent) và chọn homework status            │
│ 5. Save Attendance                                               │
│                                                                  │
│ Session Report (báo cáo)                                         │
│ 6. Xem attendance summary                                         │
│ 7. Điền teacher note và Submit Report                            │
└──────────────────────────────────────────────────────────────────┘
                             ↓
┌──────────────────────────────────────────────────────────────────┐
│ SYSTEM AUTO-PROCESSING                                           │
├──────────────────────────────────────────────────────────────────┤
│ 8. Batch update student_session records                          │
│ 9. Update session.status = 'done' khi Submit Report               │
│ 10. Update session.teacher_note                                 │
└──────────────────────────────────────────────────────────────────┘
```

### 4.1 Take Attendance

1. Teacher vào "Take Attendance" tab → System hiển thị **TẤT CẢ sessions hôm nay** (bao gồm cả sessions chưa đến giờ).
2. System đánh dấu sessions nào **có thể điểm danh** (đã và đang diễn ra) và sessions nào **chưa thể điểm danh** (chưa đến giờ).
3. Teacher chọn session có thể điểm danh → System load session details và danh sách students.
4. Teacher điểm danh từng student (Present/Absent) và chọn homework status (nếu session có bài tập).
5. Teacher click "Save Attendance" → System validate time constraints và batch update `student_session` records.

**Business Rules:**

- **Hiển thị:** Tất cả sessions hôm nay (`s.date = CURRENT_DATE`) của teacher được phân công.
- **Có thể điểm danh/sửa:** Chỉ khi:
  - Session đã đến giờ bắt đầu (`CURRENT_TIME >= tst.start_time`) HOẶC
  - Session đã chốt (`s.status = 'done'`) - có thể sửa trong ngày
- **Chưa thể điểm danh:** Sessions chưa đến giờ bắt đầu (`CURRENT_TIME < tst.start_time`) - hiển thị nhưng button disabled.
- Chỉ teacher được phân công (`teaching_slot` với status `scheduled` hoặc `substituted`).

### 4.2 Save Attendance

1. Teacher click "Save Attendance" → System validate tất cả records.
2. System batch insert/update `student_session` records.
3. System hiển thị success message với summary statistics.

**State machine:**

- Session status: `planned` → (Save Attendance) → `planned` (vẫn giữ nguyên)
- Attendance records: Insert hoặc Update theo conflict resolution.

### 4.3 Session Report

1. Teacher click "Report" từ Attendance page hoặc success message.
2. System load session report data (topic, attendance summary).
3. Teacher điền teacher note (optional).
4. Teacher click "Submit Report" → System update `session.status = 'done'` và `session.teacher_note`.

**State machine:**

- `planned` → (Submit Report) → `done`

### 4.4 Edit Attendance

1. Teacher quay lại "Take Attendance" và chọn session đã điểm danh (trong cùng ngày).
2. System load lại attendance data hiện tại.
3. Teacher sửa và Save lại → System update records tương tự.

**Business Rules:**

- Chỉ cho phép sửa trong cùng ngày (`s.date = CURRENT_DATE`).
- Chỉ cho phép với session `planned` (đang diễn ra) hoặc `done` (đã chốt, có thể sửa trong ngày).
- Session status vẫn giữ nguyên (`planned` hoặc `done`).

#### Wireframes (Take Attendance)

```
┌─────────────────────────────────────────────────────────┐
│ Take Attendance                                         │
├─────────────────────────────────────────────────────────┤
│ Today's Sessions                                        │
│ ┌─────────────────────────────────────────────────────┐ │
│ │ 🟢 ENG-A1-2024-003 - English Advanced              │ │
│ │    Jan 15, 2024 | 07:00-08:30                       │ │
│ │    Đã điểm danh                    [Sửa điểm danh] │ │
│ │    (Session completed - Can edit today)              │ │
│ ├─────────────────────────────────────────────────────┤ │
│ │ 🟡 JPN-B1-2024-002 - Japanese Basic                │ │
│ │    Jan 15, 2024 | 09:00-10:30                       │ │
│ │    Chưa điểm danh                      [Điểm danh] │ │
│ │    (Currently 09:15 - Can take attendance)           │ │
│ ├─────────────────────────────────────────────────────┤ │
│ │ ⚪ IELTS-F-2024-001 - IELTS Foundation              │ │
│ │    Jan 15, 2024 | 16:00-17:30                       │ │
│ │    Chưa đến giờ                      [Điểm danh] ⚠ │ │
│ │    (Starts at 16:00 - Button disabled)               │ │
│ └─────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
```

**Note:**

- ⚪ Sessions chưa đến giờ: hiển thị button "Điểm danh" bị disabled
- 🟡 Sessions đang diễn ra: có thể điểm danh
- 🟢 Sessions đã chốt: có thể sửa điểm danh trong ngày

#### Wireframes (Attendance Page)

```
┌─────────────────────────────────────────────────────────┐
│ Session: JPN-B1-2024-002 | Jan 15, 2024 | 09:00-10:30 │
├─────────────────────────────────────────────────────────┤
│ Code │ Name        │ Makeup │ Attendance │ Homework     │ Note │
│ ST001│ Nguyễn Văn A│  ☐     │ ○ Absent   │ ✅ Completed│     │
│ ST002│ Trần Thị B  │  ☐     │ ● Present  │ ❌ Incomplete│     │
│ ST003│ Lê Văn C    │  ☐     │ ● Present  │ ✅ Completed│     │
│ ...                                                      │
│ Summary: Present 12 | Absent 3 | HW Completed 8         │
│ [Save Attendance] [Report] [Cancel]                     │
└─────────────────────────────────────────────────────────┘
```

#### Wireframes (Session Report)

```
┌─────────────────────────────────────────────────────────┐
│ Session Report                                          │
├─────────────────────────────────────────────────────────┤
│ 📋 Session Information                                  │
│ Class: JPN-B1-2024-002 - Japanese Basic                │
│ Date: January 15, 2024                                  │
│ Time: 09:00-10:30                                       │
│                                                         │
│ 📊 Attendance Summary                                   │
│ 👥 Attendance: 12 / 15 students (80%)                    │
│ Present: 12 students | Absent: 3 students               │
│                                                         │
│ 📚 Session Topic                                        │
│ Introduction to Japanese Hiragana and Basic Greetings │
│                                                         │
│ 📝 Teacher Note                                         │
│ [___________________________________________________]  │
│ Character count: 0 / 1000                               │
│                                                         │
│ [Cancel] [Submit Report]                                │
└─────────────────────────────────────────────────────────┘
```

---

## 5. DTOs & PAYLOADS

### Request DTOs

```java
@Data
public class AttendanceSaveDTO {
    @NotNull
    private Long sessionId;

    @NotEmpty
    private List<StudentAttendanceDTO> attendances;
}

@Data
public class StudentAttendanceDTO {
    @NotNull
    private Long studentId;

    @NotNull
    private AttendanceStatus attendanceStatus; // PRESENT, ABSENT

    private HomeworkStatus homeworkStatus; // COMPLETED, INCOMPLETE, NO_HOMEWORK

    private Boolean isMakeup;

    private String note;
}

@Data
public class SessionReportSubmitDTO {
    @NotNull
    private Long sessionId;

    @Size(max = 1000)
    private String teacherNote;
}
```

### Response DTOs

```java
@Data
public class TodaySessionDTO {
    private Long sessionId;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private String classCode;
    private String className;
    private String courseName;
    private SessionStatus status;
    private Boolean isSubstituted;
    private String attendanceStatusDisplay; // "Chưa điểm danh", "Đang điểm danh", "Đã điểm danh"
    private Boolean canTakeAttendance; // true nếu đã đến giờ hoặc đã chốt, false nếu chưa đến giờ
}

@Data
public class AttendanceDataDTO {
    private SessionInfoDTO session;
    private List<StudentAttendanceDataDTO> students;
    private AttendanceSummaryDTO summary;
}

@Data
public class StudentAttendanceDataDTO {
    private Long studentId;
    private String studentCode;
    private String fullName;
    private AttendanceStatus attendanceStatus;
    private HomeworkStatus homeworkStatus;
    private Boolean isMakeup;
    private String note;
    private LocalDateTime recordedAt;
    private String homeworkType; // "no_homework", "has_homework"
}

@Data
public class AttendanceSummaryDTO {
    private Integer presentCount;
    private Integer absentCount;
    private Integer totalStudents;
    private Integer homeworkCompletedCount;
    private Integer homeworkIncompleteCount;
}

@Data
public class SessionReportDTO {
    private SessionInfoDTO session;
    private AttendanceSummaryDTO attendanceSummary;
    private String topic;
    private String teacherNote;
}
```

---

## 6. API ENDPOINTS

- GET `/api/v1/teachers/me/today-sessions` - Danh sách sessions hôm nay
- GET `/api/v1/teachers/me/sessions/{id}/attendance` - Load attendance data
- POST `/api/v1/teachers/me/sessions/{id}/attendance` - Save attendance (batch)
- GET `/api/v1/teachers/me/sessions/{id}/report` - Load session report data
- POST `/api/v1/teachers/me/sessions/{id}/report` - Submit session report

Tất cả trả về theo `ResponseObject<T>` thống nhất.

Examples:

**Get Today Sessions:**

```json
GET /api/v1/teachers/me/today-sessions

Response:
{
  "success": true,
  "message": "Sessions loaded successfully",
  "data": [
    {
      "sessionId": 120,
      "date": "2025-01-15",
      "startTime": "08:00:00",
      "endTime": "09:30:00",
      "classCode": "IELTS-F-2024-001",
      "className": "IELTS Foundation",
      "courseName": "IELTS Preparation",
      "status": "PLANNED",
      "isSubstituted": false,
      "attendanceStatusDisplay": "Chưa điểm danh",
      "canTakeAttendance": false
    },
    {
      "sessionId": 124,
      "date": "2025-01-15",
      "startTime": "09:00:00",
      "endTime": "10:30:00",
      "classCode": "JPN-B1-2024-002",
      "className": "Japanese Basic",
      "courseName": "Japanese Language",
      "status": "PLANNED",
      "isSubstituted": false,
      "attendanceStatusDisplay": "Chưa điểm danh",
      "canTakeAttendance": true
    },
    {
      "sessionId": 125,
      "date": "2025-01-15",
      "startTime": "14:00:00",
      "endTime": "15:30:00",
      "classCode": "ENG-A1-2024-003",
      "className": "English Advanced",
      "courseName": "English Advanced",
      "status": "DONE",
      "isSubstituted": false,
      "attendanceStatusDisplay": "Đã điểm danh",
      "canTakeAttendance": true
    }
  ]
}
```

**Get Attendance Data:**

```json
GET /api/v1/teachers/me/sessions/124/attendance

Response:
{
  "success": true,
  "message": "Attendance data loaded",
  "data": {
    "session": {
      "id": 124,
      "date": "2025-01-15",
      "classCode": "IELTS-F-2024-001",
      "className": "IELTS Foundation"
    },
    "students": [
      {
        "studentId": 1,
        "studentCode": "ST001",
        "fullName": "Nguyễn Văn A",
        "attendanceStatus": null,
        "homeworkStatus": null,
        "isMakeup": false,
        "note": null,
        "recordedAt": null,
        "homeworkType": "has_homework"
      }
    ],
    "summary": {
      "presentCount": 0,
      "absentCount": 0,
      "totalStudents": 15,
      "homeworkCompletedCount": 0,
      "homeworkIncompleteCount": 0
    }
  }
}
```

**Save Attendance:**

```json
POST /api/v1/teachers/me/sessions/124/attendance
{
  "sessionId": 124,
  "attendances": [
    {
      "studentId": 1,
      "attendanceStatus": "PRESENT",
      "homeworkStatus": "COMPLETED",
      "isMakeup": false,
      "note": null
    },
    {
      "studentId": 2,
      "attendanceStatus": "PRESENT",
      "homeworkStatus": "INCOMPLETE",
      "isMakeup": false,
      "note": null
    }
  ]
}

Response:
{
  "success": true,
  "message": "Attendance saved successfully",
  "data": {
    "sessionId": 124,
    "savedCount": 15,
    "summary": {
      "presentCount": 12,
      "absentCount": 3,
      "totalStudents": 15,
      "homeworkCompletedCount": 8,
      "homeworkIncompleteCount": 7
    }
  }
}
```

**Submit Report:**

```json
POST /api/v1/teachers/me/sessions/124/report
{
  "sessionId": 124,
  "teacherNote": "Đã dạy Speaking Part 2, focus vào fluency và vocabulary. Học viên tham gia tích cực."
}

Response:
{
  "success": true,
  "message": "Session report submitted successfully",
  "data": {
    "sessionId": 124,
    "status": "DONE",
    "teacherNote": "Đã dạy Speaking Part 2..."
  }
}
```

---

## 7. SERVICE LOGIC

### 7.1 Get Today Sessions

- Validate teacher authorization (từ JWT).
- Query sessions với filters:
  - `s.date = CURRENT_DATE` (TẤT CẢ sessions hôm nay, không filter theo time)
  - `teaching_slot.teacher_id = current_teacher_id AND status IN ('scheduled','substituted')`
- Tính `attendanceStatusDisplay` dựa trên session status và existence of attendance records.
- Tính `canTakeAttendance`:
  - `true` nếu: `(s.status = 'planned' AND CURRENT_TIME >= tst.start_time) OR s.status = 'done'`
  - `false` nếu: `s.status = 'planned' AND CURRENT_TIME < tst.start_time`

### 7.2 Get Attendance Data

- Validate session access (teacher được phân công).
- Validate time constraints (trong ngày, đã đến giờ hoặc đã chốt).
- Load students từ `student_session` (LEFT JOIN để include students chưa có record).
- Tính `homeworkType` từ `course_session.student_task`.
- Tính summary statistics (present/absent, homework completed/incomplete).

### 7.3 Save Attendance

- Validate session access.
- **Validate time constraints:** Chỉ cho phép khi:
  - `(s.status = 'planned' AND CURRENT_TIME >= tst.start_time)` HOẶC
  - `s.status = 'done'` (có thể sửa trong ngày)
- Validate homework status (chỉ có thể chọn nếu `homeworkType = 'has_homework'`).
- Batch insert/update `student_session` records với `ON CONFLICT` resolution.
- Tính lại summary statistics và trả về.

### 7.4 Submit Report

- Validate session access và time constraints.
- Validate session status (chỉ cho phép với `planned` hoặc `done`).
- Update `session.status = 'done'` và `session.teacher_note`.
- Trả về session info sau khi update.

---

## 8. DATABASE SCHEMA DETAILS

Tables chính: `session`, `student_session`, `teaching_slot`, `student`, `class`, `course`, `course_session`, `time_slot_template`.

Indexes khuyến nghị: `idx_student_session_session_student`, `idx_session_date_status`, `idx_teaching_slot_teacher_session`.

---

## 9. BUSINESS RULES

**Time Validation:**

- **Hiển thị:** Tất cả sessions trong ngày (`s.date = CURRENT_DATE`).
- **Có thể điểm danh/sửa:** Chỉ khi:
  - Session đã đến giờ bắt đầu (`CURRENT_TIME >= tst.start_time`) HOẶC
  - Session đã chốt (`s.status = 'done'`) - có thể sửa trong ngày
- **Chưa thể điểm danh:** Sessions chưa đến giờ bắt đầu (`CURRENT_TIME < tst.start_time`) - hiển thị nhưng button disabled.

**Authorization:**

- Chỉ teacher được phân công (`teaching_slot` với status `scheduled` hoặc `substituted`).

**Session Status:**

- Chỉ cho phép với `planned` (đã đến giờ) hoặc `done` (đã chốt, có thể sửa).

**Homework Validation:**

- Chỉ có thể chọn homework status nếu session có bài tập (`course_session.student_task IS NOT NULL AND course_session.student_task != ''`).
- Nếu không có bài tập: `homeworkStatus` phải là `NO_HOMEWORK` hoặc `null`.

**Status Flow:**

- `planned` → (Save Attendance) → `planned` (vẫn giữ nguyên)
- `planned` → (Submit Report) → `done`
- `done` → (Edit Attendance) → `done` (vẫn giữ nguyên, có thể sửa trong ngày)

---

## 10. EDGE CASES & ERROR HANDLING

- **Session chưa đến giờ bắt đầu:** trả lỗi business với message rõ ràng.
- **Session khác ngày:** trả lỗi business.
- **Teacher không được phân công:** trả lỗi authorization.
- **Session đã bị cancelled:** trả lỗi business.
- **Homework status không hợp lệ:** validate và trả lỗi validation.
- **Network error khi save:** retry mechanism hoặc rollback transaction.

---

## 11. SQL SNIPPETS FOR VALIDATION

Kiểm tra session access và time constraints (dùng khi Save Attendance):

```sql
SELECT 1
FROM session s
JOIN teaching_slot ts ON ts.session_id = s.id
JOIN time_slot_template tst ON s.time_slot_template_id = tst.id
WHERE s.id = :session_id
  AND ts.teacher_id = :teacher_id
  AND ts.status IN ('scheduled','substituted')
  AND s.date = CURRENT_DATE
  AND (
    (s.status = 'planned' AND CURRENT_TIME >= tst.start_time)
    OR s.status = 'done'
  )
LIMIT 1;
```

**Note:** Query này chỉ validate khi user click "Save Attendance", không dùng để filter danh sách sessions hôm nay.

Kiểm tra homework type:

```sql
SELECT
  CASE
    WHEN cs.student_task IS NULL OR cs.student_task = '' THEN 'no_homework'
    ELSE 'has_homework'
  END AS homework_type
FROM session s
LEFT JOIN course_session cs ON s.course_session_id = cs.id
WHERE s.id = :session_id;
```

---

## 12. TESTING STRATEGY

- **Unit tests:** AttendanceService (getTodaySessions, getAttendanceData, saveAttendance, submitReport).
- **Integration:** transaction batch update `student_session`; xác thực session status update.
- **SQL playground:** chạy các snippet trên với seed.

---

## 13. SECURITY & AUTHORIZATION

- `@PreAuthorize("hasRole('TEACHER')")` cho tất cả endpoints; kiểm tra ownership bằng `teacher_id` từ `SecurityContext`.
- Validate session access qua `teaching_slot` trong service layer.

---

## 14. PERFORMANCE NOTES

- Batch save sử dụng `ON CONFLICT` để xử lý insert/update hiệu quả.
- Thêm index theo gợi ý để giữ truy vấn < 100ms.
- Cache session info nếu cần (optional, không bắt buộc cho MVP).

---

Document Status: Ready for Implementation
