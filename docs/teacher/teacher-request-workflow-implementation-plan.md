# TEACHER WORKFLOW IMPLEMENTATION PLAN

**Version:** 1.0
**Last Updated:** 2025-11-03
**Author:** Technical Team
**Focus:** Teacher-facing workflows (Swap, Reschedule, Modality Change)

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

| Actor              | Vai trò   | Trách nhiệm trong Teacher Request|
| ------------------ | --------- | ---------------------------------|
| **TEACHER**        | Giáo viên | - Tạo request (Swap/Reschedule/Modality)<br>- Cung cấp đề xuất (ngày/slot/resource/replacement), lý do<br>- Theo dõi trạng thái request                              |
| **ACADEMIC STAFF** | Giáo vụ   | - Review & Approve/Reject<br>- Chọn/giữ replacement teacher<br>- Xác nhận slot/resource hợp lệ                                                                       |
| **SYSTEM**         | Hệ thống  | - Validate conflicts (teacher/resource/time-slot)<br>- Áp business rules & cập nhật `teaching_slot/session/session_resource/class.modality`<br>- Gửi email & ghi log |

Pre-conditions tổng quát:

- JWT + role TEACHER hoạt động; teacher có `user_account` và record `teacher` hợp lệ.
- Lịch dạy (sessions) đã được tạo sẵn theo course/class.

Post-conditions tổng quát:

- Trạng thái `teacher_request` phản ánh đúng vòng đời (PENDING → WAITING_CONFIRM → APPROVED/REJECTED).
- `teaching_slot` và `session` được cập nhật theo loại yêu cầu.
- Email được gửi theo từng mốc.

---

## 2. CONTEXT & BACKGROUND

- Backend đã có đầy đủ entity: `TeacherRequest`, `TeachingSlot`, `Session`, `SessionResource`, `Teacher`, `TeacherAvailability`, `TeacherSkill`.
- Security JWT sẵn sàng; cấu hình `ddl-auto: validate` yêu cầu DB phải có schema trước (đã xử lý bằng `enum-init.sql` + `schema.sql`).
- Mục tiêu Teacher: thao tác nhanh – chính xác – tối thiểu bước; các quyết định cuối cùng do Academic Staff chịu trách nhiệm.

---

## 3. CÁC LUỒNG CHÍNH (MVP)

### 2.1 Swap (Đổi ca giữa giáo viên)

1. Teacher mở Requests → chọn loại request `SWAP`.
2. Teacher chọn session mục tiêu và (tùy chọn) đề xuất `replacement_teacher_id`.
3. Academic Staff duyệt: có thể giữ đề xuất hoặc chọn giáo viên khác (phù hợp skill/availability, không trùng lịch).
4. Replacement Teacher xác nhận (WAITING_CONFIRM).
5. System cập nhật `teaching_slot`: teacher gốc `on_leave`, teacher thay `substituted` cho session mục tiêu.
6. Email: Teacher → Academic Staff → Replacement Teacher → Teacher (xác nhận cuối).

### 2.2 Reschedule (Dời lịch của một session)

1. Teacher mở Requests → chọn loại request `RESCHEDULE`.
2. Teacher chọn session → đề xuất `new_date` + `new_time_slot_id` (và resource gợi ý nếu cần).
3. Academic Staff duyệt.
4. System: session cũ chuyển `CANCELLED`; tạo session mới (same class/course) ở ngày/slot mới; cập nhật `teaching_slot` và `student_session` tương ứng.
5. Email: Teacher → Academic Staff → Teacher (kết quả).

### 2.3 Modality Change (Đổi hình thức/nguồn lực)

1. Teacher mở Requests → chọn loại request `MODALITY_CHANGE`.
2. Teacher chọn session → đề xuất `new_resource_id` hoặc modality khác (nếu policy cho phép).
3. Academic Staff duyệt và chọn resource khả dụng.
4. System cập nhật `session_resource` và `class.modality` theo resource được chọn.
5. Email: Teacher → Academic Staff → Teacher (kết quả).

---

## 4. SCREEN-BY-SCREEN (Teacher UX)

Requests Center (các yêu cầu của tôi)

- Danh sách yêu cầu theo trạng thái: pending, waiting_confirm, approved, rejected
- Chi tiết request + lịch sử xử lý (submitted_at, decided_at, decided_by)
- Tạo mới: Step 1 chọn loại request (Swap/Reschedule/Modality) → Step 2 chọn session phù hợp → Step 3 nhập thông tin bổ sung → Submit

```
┌─────────────────────────────────────────────────────────┐
│ Requests Center (My Requests)                   [+ New Request] │
├─────────────────────────────────────────────────────────┤
│ ID  Type       Session  Date      Status           Actions │
│ 12  SWAP       #124     2025-01-28 WAITING_CONFIRM  [View] │
│ 15  RESCHEDULE #125     2025-02-02 APPROVED        [View] │
│ 18  MODALITY   #130     2025-02-10 PENDING         [View] │
└─────────────────────────────────────────────────────────┘
```

New Request (wizard - rút gọn)

```
[Step 1] Type:  ○ Swap   ○ Reschedule   ○ Modality  → [Next]
[Step 2] Pick Session: table sessions with [Pick]             → [Next]
[Step 3A] Swap: Replacement picker + Reason                   → [Submit]
[Step 3B] Reschedule: New Date/Time Slot (+ Resource opt)     → [Submit]
[Step 3C] Modality: New Modality/Resource                     → [Submit]
```

---

## 5. API ĐỀ XUẤT (BACKEND-FIRST)

- GET `/api/v1/teachers/me/sessions?from=&to=`
- GET `/api/v1/teachers/me/sessions/{id}`
- POST `/api/v1/teacher-requests` (body: `{ requestType, sessionId, replacementTeacherId?, newDate?, newTimeSlotId?, newResourceId?, reason }`)
- GET `/api/v1/teacher-requests/me`
- POST `/api/v1/teacher-requests/{id}/confirm` (replacement teacher xác nhận)
- PATCH `/api/v1/teacher-requests/{id}/approve` (staff)
- PATCH `/api/v1/teacher-requests/{id}/reject` (staff)

Security:

- Teacher endpoints: `hasRole('TEACHER')` và ràng buộc ownership (teacher chỉ thấy sessions của mình).
- Staff endpoints: `hasRole('ACADEMIC_STAFF')`.

---

## 6. BUSINESS RULES CỐT LÕI

- Swap:
  - Replacement teacher phải không trùng `session(date, time_slot_template_id)` ở trạng thái `PLANNED/DONE`.
  - Ưu tiên skill khớp `course_session.skill_set` hoặc `general`.
- Reschedule:
  - Ngày/khung giờ mới không được xung đột phòng/resource.
  - Session cũ chuyển `CANCELLED`; session mới tạo cùng `class_id`, copy metadata cần thiết.
- Modality:
  - Resource phải `AVAILABLE` tại `date + time_slot_template`.
  - Sau khi approve, cập nhật `class.modality` tương ứng (virtual → online, room → offline).
- Time window:
  - Chỉ cho phép tạo request với các session chưa diễn ra trong vòng 7 ngày tới.

Request lifecycle:

- `PENDING` (teacher gửi) → `APPROVED/REJECTED` (staff) → nếu Swap: `WAITING_CONFIRM` (replacement) → `APPROVED` khi xác nhận.

---

## 7. DATABASE TOUCHPOINTS

- `teacher_request(request_type, teacher_id, session_id, replacement_teacher_id, new_date, new_time_slot_id, new_resource_id, status, submitted_at, decided_at, note)`
- `teaching_slot(teacher_id, session_id, status)` — cập nhật theo Swap; tạo/cập nhật cho Reschedule.
- `session(id, date, time_slot_template_id, status, ...)` — hủy/tạo mới (Reschedule), giữ nguyên (Swap).
- `class(id, modality)` — cập nhật modality khi Modality Change được approve.
- `session_resource(session_id, resource_id)` — cập nhật theo Modality.
- `student_session(student_id, session_id, attendance_status, ...)` — Reschedule: di chuyển/cập nhật theo session mới.

Chỉ số/Index gợi ý:

- `idx_teaching_slot_teacher_session`, `idx_session_date_slot_status`, `idx_session_resource_unique(session_id, resource_id)`.

---

## 8. EMAIL / NOTIFICATION LUỒNG

### Swap

1. Teacher → Academic Staff: Thông báo yêu cầu Swap.
2. Academic Staff → Replacement Teacher: Yêu cầu xác nhận dạy thay.
3. Replacement Teacher → System: Confirm → System cập nhật slots.
4. System → Teacher gốc: Xác nhận hoàn tất.

### Reschedule

1. Teacher → Academic Staff: Thông báo đề xuất dời lịch.
2. Academic Staff → Teacher: Kết quả duyệt (và thông tin session mới).

### Modality Change

1. Teacher → Academic Staff: Thông báo đề xuất thay đổi resource/modality.
2. Academic Staff → Teacher: Kết quả duyệt.

Email content: dùng template đơn giản (subject + tóm tắt session + thời gian + liên hệ) — triển khai thực tế có thể chuyển sang NotificationService.

---

## 9. DATABASE QUERIES & LOGIC

### 9.1 Kiểm tra xung đột khi Swap/Reschedule

```sql
-- Trùng lịch cho giáo viên X ở ngày/khung giờ
SELECT 1
FROM teaching_slot ts
JOIN session s ON s.id = ts.session_id
WHERE ts.teacher_id = :teacher_id
  AND s.date = :date
  AND s.time_slot_template_id = :time_slot_template_id
  AND s.status IN ('planned','done')
LIMIT 1;
```

### 9.2 Kiểm tra resource rảnh (Modality/Reschedule)

```sql
SELECT 1
FROM session s
JOIN session_resource sr ON sr.session_id = s.id
WHERE sr.resource_id = :resource_id
  AND s.date = :date
  AND s.time_slot_template_id = :time_slot_template_id
  AND s.status IN ('planned','done')
LIMIT 1;
```

---

## 11. IMPLEMENTATION PHASES

1. TeacherRequestController: tạo request (3 loại) + list của tôi
2. Staff: approve/reject; Swap confirm của replacement
3. Service logic cập nhật `teaching_slot`/`session`/`session_resource` theo loại
4. Email hooks (gửi thông báo đơn giản)
5. Tests: unit cho service; SQL playground với seed-data

Trọng tâm: đơn giản – đúng quy tắc – tránh over-engineering; giữ transaction ngắn và rõ ràng.

---

## 12. TESTING STRATEGY

- Unit tests: Service cho 3 luồng (Swap/Reschedule/Modality).
- Integration tests (Testcontainers): xác minh transaction cập nhật `teaching_slot/session/session_resource` và trạng thái `teacher_request`.
- Edge cases: trùng lịch, resource bận, teacher không sở hữu session, request double-submit.

---

## 13. DEPENDENCIES & RISKS

- Phụ thuộc: dữ liệu seed (teachers, skills, availability), email service (có thể stub/mock).
- Rủi ro: xung đột đồng thời khi duyệt/confirm; giải pháp: lock ngắn hoặc kiểm tra lại trước commit.

---

## 14. SUCCESS METRICS

- Thời gian tạo request < 1s; duyệt/confirm < 500ms.
- Không phát sinh session/slot mồ côi; 0 lỗi schema-validate trong CI.
- Unit coverage > 85% cho service teacher-request; integration smoke tests pass.

---

**Status:** Ready for implementation
