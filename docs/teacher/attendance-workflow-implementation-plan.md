# ATTENDANCE WORKFLOW IMPLEMENTATION PLAN

**Version:** 1.0
**Last Updated:** 2025-11-03
**Author:** Technical Team
**Focus:** Teacher-facing attendance workflows (Take Attendance, Save, Report)

---

## MỤC LỤC

1. [Tổng quan & Vai trò](#1-tổng-quan--vai-trò)
2. [Context & Background](#2-context--background)
3. [Các luồng chính (MVP)](#3-các-luồng-chính-mvp)
4. [Screen-by-Screen (Teacher UX)](#4-screen-by-screen-teacher-ux)
5. [API đề xuất (backend-first)](#5-api-đề-xuất-backend-first)
6. [Business rules cốt lõi](#6-business-rules-cốt-lõi)
7. [Database touchpoints](#7-database-touchpoints)
8. [Email/Notification luồng](#8-email--notification-luồng)
9. [Database Queries & Logic](#9-database-queries--logic)
10. [Test nhanh (SQL mẫu, dữ liệu seed)](#10-test-nhanh-sql-mẫu-dữ-liệu-seed)
11. [Implementation Phases](#11-implementation-phases)
12. [Testing Strategy](#12-testing-strategy)
13. [Dependencies & Risks](#13-dependencies--risks)
14. [Success Metrics](#14-success-metrics)

---

## 1. TỔNG QUAN & VAI TRÒ

| Actor       | Vai trò   | Trách nhiệm trong Attendance Flow                                                                                    |
| ----------- | --------- | -------------------------------------------------------------------------------------------------------------------- |
| **TEACHER** | Giáo viên | - Xem sessions hôm nay<br>- Điểm danh students<br>- Ghi nhận homework status<br>- Submit session report              |
| **SYSTEM**  | Hệ thống  | - Validate time constraints<br>- Check authorization<br>- Batch update attendance records<br>- Update session status |

Pre-conditions tổng quát:

- JWT + role TEACHER hoạt động; teacher có `user_account` và record `teacher` hợp lệ.
- Sessions đã được tạo sẵn với `teaching_slot` phân công teacher.
- Students đã enrolled trong class và có `student_session` records.

Post-conditions tổng quát:

- `student_session` records được cập nhật với `attendance_status` và `homework_status`.
- `session.status` chuyển từ `planned` → `done` khi Submit Report.
- `session.teacher_note` được lưu khi Submit Report.

---

## 2. CONTEXT & BACKGROUND

- Backend đã có đầy đủ entity: `Session`, `StudentSession`, `TeachingSlot`, `Student`, `Class`, `Course`, `CourseSession`, `TimeSlotTemplate`.
- Security JWT sẵn sàng.
- Mục tiêu Teacher: điểm danh nhanh – chính xác – tối thiểu bước; real-time summary statistics.

---

## 3. CÁC LUỒNG CHÍNH (MVP)

### 3.1 Take Attendance (Điểm danh)

1. Teacher vào "Take Attendance" tab → System hiển thị **TẤT CẢ sessions hôm nay** (bao gồm cả sessions chưa đến giờ).
2. System đánh dấu sessions nào **có thể điểm danh** (đã và đang diễn ra) và sessions nào **chưa thể điểm danh** (chưa đến giờ).
3. Teacher chọn session có thể điểm danh → System load session details và danh sách students.
4. Teacher điểm danh từng student (Present/Absent) và chọn homework status (nếu có).
5. Teacher click "Save Attendance" → System validate time constraints và batch update `student_session` records.
6. Teacher click "Report" → System load session report form.
7. Teacher điền note và Submit Report → System update `session.status = 'done'` và `session.teacher_note`.

### 3.2 Edit Attendance (Sửa điểm danh)

1. Teacher quay lại "Take Attendance" và chọn session đã điểm danh (trong cùng ngày, đã và đang diễn ra).
2. System load lại attendance data hiện tại.
3. Teacher sửa và Save lại → System update records tương tự.

**Business Rules:**

- **Hiển thị:** Tất cả sessions hôm nay (`s.date = CURRENT_DATE`) của teacher được phân công.
- **Có thể điểm danh/sửa:** Chỉ khi:
  - Session đã đến giờ bắt đầu (`CURRENT_TIME >= tst.start_time`) HOẶC
  - Session đã chốt (`s.status = 'done'`) - có thể sửa trong ngày
- **Chưa thể điểm danh:** Sessions chưa đến giờ bắt đầu (`CURRENT_TIME < tst.start_time`) - hiển thị nhưng button disabled.
- Chỉ teacher được phân công (`teaching_slot` với status `scheduled` hoặc `substituted`).

---

## 4. SCREEN-BY-SCREEN (Teacher UX)

### Take Attendance Tab

```
┌─────────────────────────────────────────────────────────┐
│ Take Attendance                                         │
├─────────────────────────────────────────────────────────┤
│ Today's Sessions                                        │
│ ┌─────────────────────────────────────────────────────┐ │
│ │ ⚪ IELTS-F-2024-001 - IELTS Foundation              │ │
│ │    08:00-09:30 | Chưa đến giờ        [Điểm danh] ⚠ │
│ │    (Starts at 08:00 - Button disabled)              │ │
│ ├─────────────────────────────────────────────────────┤ │
│ │ 🟡 JPN-B1-2024-002 - Japanese Basic                │ │
│ │    09:00-10:30 | Chưa điểm danh      [Điểm danh]   │ │
│ │    (Currently 09:15 - Can take attendance)            │ │
│ ├─────────────────────────────────────────────────────┤ │
│ │ 🟢 ENG-A1-2024-003 - English Advanced              │ │
│ │    07:00-08:30 | Đã điểm danh        [Sửa điểm danh]│ │
│ │    (Session completed - Can edit today)              │ │
│ └─────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
```

**Note:**

- ⚪ Sessions chưa đến giờ: hiển thị nhưng button "Điểm danh" bị disabled
- 🟡 Sessions đang diễn ra: có thể điểm danh
- 🟢 Sessions đã chốt: có thể sửa điểm danh trong ngày

### Attendance Page

```
┌─────────────────────────────────────────────────────────┐
│ Session: IELTS-F-2024-001 | Jan 15, 2024 | 09:00-10:30 │
├─────────────────────────────────────────────────────────┤
│ Code │ Name        │ Makeup │ Attendance │ Homework │ Note │
│ ST001│ Nguyễn Văn A│  ☐     │ ○ Absent   │ ✅ Completed│     │
│ ST002│ Trần Thị B  │  ☐     │ ● Present  │ ❌ Incomplete│     │
│ ...                                                      │
│ Summary: Present 12 | Absent 3 | HW Completed 8          │
│ [Save Attendance] [Report] [Cancel]                     │
└─────────────────────────────────────────────────────────┘
```

### Session Report

```
┌─────────────────────────────────────────────────────────┐
│ Session Report                                          │
├─────────────────────────────────────────────────────────┤
│ Class: IELTS-F-2024-001 | Jan 15, 2024 | 09:00-10:30   │
│ Attendance: 12/15 students (80%)                       │
│ Topic: [Introduction to English Alphabet...]           │
│ Teacher Note: [___________________________]             │
│ [Cancel] [Submit Report]                                │
└─────────────────────────────────────────────────────────┘
```

---

## 5. API ĐỀ XUẤT (BACKEND-FIRST)

- GET `/api/v1/teachers/me/today-sessions` - Danh sách sessions hôm nay
- GET `/api/v1/teachers/me/sessions/{id}/attendance` - Load attendance data
- POST `/api/v1/teachers/me/sessions/{id}/attendance` - Save attendance (batch)
- GET `/api/v1/teachers/me/sessions/{id}/report` - Load session report data
- POST `/api/v1/teachers/me/sessions/{id}/report` - Submit session report
- PATCH `/api/v1/teachers/me/sessions/{id}/attendance` - Update attendance (edit)

Security:

- Teacher endpoints: `hasRole('TEACHER')` và ràng buộc ownership (teacher chỉ thấy sessions được phân công).

---

## 6. BUSINESS RULES CỐT LÕI

**Time Validation:**

- Chỉ điểm danh/sửa trong ngày (`s.date = CURRENT_DATE`).
- Chỉ điểm danh khi đã đến giờ bắt đầu (`CURRENT_TIME >= tst.start_time`) hoặc đã chốt (`s.status = 'done'`).

**Authorization:**

- Chỉ teacher được phân công (`teaching_slot` với status `scheduled` hoặc `substituted`).

**Session Status:**

- Chỉ cho phép với `planned` (đã đến giờ) hoặc `done` (đã chốt, có thể sửa).

**Homework Validation:**

- Chỉ có thể chọn homework status nếu session có bài tập (`course_session.student_task IS NOT NULL AND course_session.student_task != ''`).
- Nếu không có bài tập: hiển thị "No Homework" (disabled).

**Status Flow:**

- `planned` → (Save Attendance) → `planned` (vẫn giữ nguyên)
- `planned` → (Submit Report) → `done`
- `done` → (Edit Attendance) → `done` (vẫn giữ nguyên, có thể sửa trong ngày)

---

## 7. DATABASE TOUCHPOINTS

- `session(id, date, status, teacher_note, ...)` — cập nhật status và note khi Submit Report.
- `student_session(student_id, session_id, attendance_status, homework_status, note, recorded_at, ...)` — batch insert/update khi Save Attendance.
- `teaching_slot(teacher_id, session_id, status)` — validate authorization.
- `course_session(id, student_task, topic, ...)` — lấy homework type và topic.
- `time_slot_template(id, start_time, end_time)` — validate time constraints.

Chỉ số/Index gợi ý:

- `idx_student_session_session_student`, `idx_session_date_status`, `idx_teaching_slot_teacher_session`.

---

## 8. EMAIL / NOTIFICATION LUỒNG

**MVP:** Không có email/notification cho attendance flow (có thể bổ sung sau nếu cần).

**Future Enhancement:**

- Gửi email cho Academic Staff khi session được chốt (Submit Report).
- Gửi email cho parents/students về attendance summary (nếu có requirement).

---

## 9. DATABASE QUERIES & LOGIC

### 9.1 Load Sessions Hôm Nay

```sql
SELECT
  s.id AS session_id,
  s.date,
  tst.start_time,
  tst.end_time,
  c.code AS class_code,
  c.name AS class_name,
  co.name AS course_name,
  s.status,
  CASE WHEN ts.status = 'substituted' THEN true ELSE false END AS is_substituted,
  CASE
    WHEN s.status = 'planned' AND EXISTS (
      SELECT 1 FROM student_session ss WHERE ss.session_id = s.id
    ) THEN 'Đang điểm danh'
    WHEN s.status = 'planned' THEN 'Chưa điểm danh'
    WHEN s.status = 'done' THEN 'Đã điểm danh'
    ELSE 'N/A'
  END AS attendance_status_display,
  -- Tính canTakeAttendance: chỉ khi đã đến giờ hoặc đã chốt
  CASE
    WHEN (s.status = 'planned' AND CURRENT_TIME >= tst.start_time) THEN true
    WHEN s.status = 'done' THEN true
    ELSE false
  END AS can_take_attendance
FROM session s
JOIN teaching_slot ts ON ts.session_id = s.id
JOIN class c ON s.class_id = c.id
JOIN course co ON c.course_id = co.id
JOIN time_slot_template tst ON s.time_slot_template_id = tst.id
WHERE ts.teacher_id = :teacher_id
  AND ts.status IN ('scheduled','substituted')
  AND s.date = CURRENT_DATE
ORDER BY tst.start_time;
```

### 9.2 Load Attendance Data

```sql
SELECT
  ss.student_id,
  ss.session_id,
  student.student_code,
  user_account.full_name,
  ss.attendance_status,
  ss.is_makeup,
  ss.homework_status,
  ss.note,
  ss.recorded_at,
  cs.student_task,
  CASE
    WHEN cs.student_task IS NULL OR cs.student_task = '' THEN 'no_homework'
    ELSE 'has_homework'
  END AS homework_type
FROM student_session ss
JOIN student ON ss.student_id = student.id
JOIN user_account ON student.user_id = user_account.id
JOIN session s ON ss.session_id = s.id
LEFT JOIN course_session cs ON s.course_session_id = cs.id
WHERE ss.session_id = :session_id
  AND EXISTS (
    SELECT 1
    FROM teaching_slot ts
    WHERE ts.session_id = s.id
      AND ts.teacher_id = :teacher_id
      AND ts.status IN ('scheduled','substituted')
  )
ORDER BY student.student_code;
```

### 9.3 Batch Save Attendance

```sql
INSERT INTO student_session (student_id, session_id, attendance_status, homework_status, note, recorded_at)
VALUES
  (:student_id_1, :session_id, :attendance_status_1, :homework_status_1, :note_1, NOW()),
  (:student_id_2, :session_id, :attendance_status_2, :homework_status_2, :note_2, NOW()),
  ...
ON CONFLICT (student_id, session_id)
DO UPDATE SET
  attendance_status = EXCLUDED.attendance_status,
  homework_status = EXCLUDED.homework_status,
  note = EXCLUDED.note,
  recorded_at = NOW(),
  updated_at = NOW();
```

### 9.4 Submit Session Report

```sql
UPDATE session
SET
  status = 'done',
  teacher_note = :teacher_note,
  updated_at = NOW()
WHERE id = :session_id
  AND EXISTS (
    SELECT 1
    FROM teaching_slot ts
    WHERE ts.session_id = :session_id
      AND ts.teacher_id = :teacher_id
      AND ts.status IN ('scheduled','substituted')
  )
  AND date = CURRENT_DATE
  AND (
    (status = 'planned' AND EXISTS (
      SELECT 1
      FROM time_slot_template tst
      WHERE tst.id = (
        SELECT time_slot_template_id FROM session WHERE id = :session_id
      )
      AND CURRENT_TIME >= tst.start_time
    ))
    OR status = 'done'
  );
```

---

## 10. TEST NHANH (SQL MẪU, DỮ LIỆU SEED)

### Test với Teacher ID = 4

```sql
-- 1. Xem TẤT CẢ sessions hôm nay của teacher 4 (bao gồm cả chưa đến giờ)
SELECT
  s.id AS session_id,
  s.date,
  tst.start_time,
  c.code AS class_code,
  s.status,
  CASE
    WHEN (s.status = 'planned' AND CURRENT_TIME >= tst.start_time) THEN true
    WHEN s.status = 'done' THEN true
    ELSE false
  END AS can_take_attendance
FROM session s
JOIN teaching_slot ts ON ts.session_id = s.id
JOIN class c ON s.class_id = c.id
JOIN time_slot_template tst ON s.time_slot_template_id = tst.id
WHERE ts.teacher_id = 4
  AND ts.status IN ('scheduled','substituted')
  AND s.date = CURRENT_DATE
ORDER BY tst.start_time;

-- 2. Load attendance data cho session cụ thể
SELECT
  ss.student_id,
  student.student_code,
  ss.attendance_status,
  ss.homework_status
FROM student_session ss
JOIN student ON ss.student_id = student.id
WHERE ss.session_id = :session_id
ORDER BY student.student_code;

-- 3. Test batch save
INSERT INTO student_session (student_id, session_id, attendance_status, homework_status, recorded_at)
VALUES
  (1, :session_id, 'present', 'completed', NOW()),
  (2, :session_id, 'present', 'incomplete', NOW())
ON CONFLICT (student_id, session_id)
DO UPDATE SET
  attendance_status = EXCLUDED.attendance_status,
  homework_status = EXCLUDED.homework_status,
  recorded_at = NOW();
```

---

## 11. IMPLEMENTATION PHASES

1. **AttendanceController:** GET today-sessions, GET/POST attendance, GET/POST report
2. **AttendanceService:** Load sessions, load attendance data, batch save, submit report
3. **Validation logic:** Time constraints, authorization, homework type
4. **Tests:** Unit tests cho service; Integration tests với Testcontainers

Trọng tâm: đơn giản – đúng quy tắc – real-time summary statistics; batch save hiệu quả.

---

## 12. TESTING STRATEGY

- **Unit tests:** Service cho load sessions, save attendance, submit report.
- **Integration tests (Testcontainers):** Xác minh batch update `student_session` và trạng thái `session`.
- **Edge cases:** Time validation, authorization, homework type, session status changes.

---

## 13. DEPENDENCIES & RISKS

- **Phụ thuộc:** Dữ liệu seed (teachers, sessions, students, teaching_slots).
- **Rủi ro:** Xung đột đồng thời khi nhiều teacher cùng điểm danh (unlikely, nhưng cần validate); giải pháp: transaction isolation.

---

## 14. SUCCESS METRICS

- Thời gian load sessions < 500ms; load attendance data < 300ms.
- Batch save < 1s cho 50 students.
- Không phát sinh attendance records mồ côi; 0 lỗi schema-validate trong CI.
- Unit coverage > 85% cho service attendance; integration smoke tests pass.

---

**Status:** Ready for implementation
