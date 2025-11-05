# TEACHER IMPLEMENTATION GUIDE

Version: 1.0  
Last Updated: 2025-11-03  
Author: Technical Team

---

## MỤC LỤC

1. [Tổng quan](#1-tổng-quan)
2. [Actors & Scope](#2-actors--scope)
3. [Pre/Post-conditions](#3-prepost-conditions)
4. [Luồng nghiệp vụ chi tiết](#4-luồng-nghiệp-vụ-chi-tiết)
   - [4.1 Swap](#41-swap)
   - [4.2 Reschedule](#42-reschedule)
   - [4.3 Modality Change](#43-modality-change)
5. [DTOs & Payloads](#5-dtos--payloads)
6. [API Endpoints](#6-api-endpoints)
7. [Service Logic](#7-service-logic)
8. [Database Schema Details](#8-database-schema-details)
9. [Business Rules](#9-business-rules)
10. [Edge Cases & Error Handling](#10-edge-cases--error-handling)
11. [Email/Notification Flow](#11-emailnotification-flow)
12. [SQL Snippets for Validation](#12-sql-snippets-for-validation)
13. [Testing Strategy](#13-testing-strategy)
14. [Security & Authorization](#14-security--authorization)
15. [Performance Notes](#15-performance-notes)

---

## 1. TỔNG QUAN

Hướng dẫn chi tiết triển khai các luồng Teacher Requests: Swap, Reschedule, Modality Change. Tài liệu này bám sát `docs/teacher/teacher-workflow-implementation-plan.md` và mở rộng thành hướng dẫn triển khai kèm DTOs, API, logic dịch vụ, và SQL kiểm tra.

---

## 2. ACTORS & SCOPE

- Teacher: khởi tạo yêu cầu.
- Academic Staff: duyệt/điều phối yêu cầu, chọn giáo viên thay thế hoặc resource.
- System: kiểm tra xung đột, cập nhật session/slot, gửi email.

Scope backend: Controller + Service + Repository + Email hook (simple).

---

## 3. PRE/POST-CONDITIONS

Pre:

- JWT chạy, role `TEACHER` & `ACADEMIC_STAFF` cấu hình đầy đủ.
- DB đã khởi tạo (`enum-init.sql` + `schema.sql`), seed có dữ liệu tối thiểu.

Post:

- `teacher_request` phản ánh đúng lifecycle; session/slot/resource cập nhật nhất quán; email log được ghi (nếu bật).

---

### 3.1 Status Glossary (Teacher Requests)

- PENDING: Teacher đã gửi yêu cầu; chờ Academic Staff duyệt.
- REJECTED: Academic Staff từ chối yêu cầu; luồng kết thúc.
- APPROVED: Yêu cầu được chấp thuận và đã hoàn tất (Reschedule/Modality) hoặc sau khi Replacement confirm (Swap).
- WAITING_CONFIRM (chỉ Swap): Staff duyệt và chọn replacement → chờ replacement xác nhận.

Rules:

- Swap: PENDING → (Staff) WAITING_CONFIRM → (Replacement) APPROVED | (Staff) REJECTED
- Reschedule: PENDING → (Staff) APPROVED | REJECTED
- Modality: PENDING → (Staff) APPROVED | REJECTED

---

## 4. LUỒNG NGHIỆP VỤ CHI TIẾT

### 4.0 User Journey (Overview)

```
┌──────────────────────────────────────────────────────────────────┐
│ TEACHER (Trong Hệ Thống)                                         │
├──────────────────────────────────────────────────────────────────┤
│ Requests Tab (tạo request)                                       │
│ 1. Chọn loại: Swap | Reschedule | Modality Change                │
│ 2. Chọn Session mục tiêu                                         │
│ 3. Nhập thông tin (replacement/new date/slot/resource, reason)   │
│ 4. Submit                                                        │
│                                                                  │
│ My Schedule (xem lịch)                                           │
│ • Xem lịch (Calendar/List)                                       │
│ • Mở Session Detail (view only)                                  │
└──────────────────────────────────────────────────────────────────┘
                             ↓
┌──────────────────────────────────────────────────────────────────┐
│ ACADEMIC STAFF (Trong Hệ Thống)                                  │
├──────────────────────────────────────────────────────────────────┤
│ 5. Nhận thông báo → Duyệt (Approve/Reject)                       │
│ 6. Nếu Swap: chọn/giữ replacement teacher                        │
│ 7. Nếu Reschedule/Modality: xác nhận slot/resource               │
└──────────────────────────────────────────────────────────────────┘
                             ↓
┌──────────────────────────────────────────────────────────────────┐
│ SYSTEM AUTO-PROCESSING                                           │
├──────────────────────────────────────────────────────────────────┤
│ 8. Cập nhật teaching_slot/session/session_resource/class.modality│
│ 9. Gửi email theo luồng                                          │
└──────────────────────────────────────────────────────────────────┘
```

### 4.1 Swap

1. Teacher mở Requests → chọn loại `SWAP`.
2. Teacher chọn session mục tiêu và (tùy chọn) đề xuất `replacement_teacher_id`.
3. Academic Staff duyệt: giữ đề xuất hoặc chọn giáo viên khác.
4. Replacement Teacher xác nhận (WAITING_CONFIRM).
5. System cập nhật `teaching_slot` của 2 giáo viên cho session đó.
6. Email: Teacher → Staff → Replacement → Teacher.

State machine (Swap):

- PENDING → (Staff Approve + chọn replacement) → WAITING_CONFIRM → (Replacement Confirm) → APPROVED
- PENDING → (Staff Reject) → REJECTED

### 4.2 Reschedule

1. Teacher mở Requests → chọn loại `RESCHEDULE`.
2. Teacher chọn session và chọn `new_date` + `new_time_slot_id` (đã bàn với students trước đó).
3. Teacher chọn `new_resource_id` (hệ thống gợi ý resource phù hợp dựa trên modality của class).
4. Teacher nhập lý do → Submit (hệ thống không validate xung đột lúc này).
5. Staff duyệt: có thể giữ nguyên ngày/giờ/resource Teacher chọn hoặc thay đổi (Staff cần validate xung đột).
6. System hủy session cũ (CANCELLED), tạo session mới cùng class, cập nhật `teaching_slot` và `student_session`.
7. Email: Teacher ↔ Staff (kết quả).

State machine (Reschedule):

- PENDING → (Staff Approve) → APPROVED
- PENDING → (Staff Reject) → REJECTED

### 4.3 Modality Change

1. Teacher mở Requests → chọn loại `MODALITY_CHANGE`.
2. Teacher chọn session và đề xuất `new_resource_id` (hoặc modality theo policy).
3. Staff duyệt, system cập nhật `session_resource` và `class.modality` theo resource được chọn.
4. Email: Teacher ↔ Staff.

State machine (Modality):

- PENDING → (Staff Approve) → APPROVED
- PENDING → (Staff Reject) → REJECTED

#### Wireframes (Requests Center)

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

##### New Request (Wizard)

```
┌─────────────────────────────────────────────────────────┐
│ New Teacher Request                                     │
├─────────────────────────────────────────────────────────┤
│ Step 1: Choose Type                                     │
│  ○ Swap   ○ Reschedule   ○ Modality Change              │
│                                                         │
│ [Next]                                          [Cancel]│
└─────────────────────────────────────────────────────────┘
```

```
┌─────────────────────────────────────────────────────────┐
│ Step 2: Pick Session                                    │
├───────┬───────────────┬───────────────┬──────────┬───────┤
│ ID    │ Date          │ Time          │ Class    │ Action│
├───────┼───────────────┼───────────────┼──────────┼───────┤
│ 124   │ 2025-01-28     │ 18:00-20:00   │ ENG-A1   │ [Pick]│
│ 125   │ 2025-02-02     │ 08:00-10:00   │ JPN-B1   │ [Pick]│
└───────┴───────────────┴───────────────┴──────────┴───────┘
[Back]                                                   [Next]
```

```
┌─────────────────────────────────────────────────────────┐
│ Step 3A (Swap): Replacement Teacher                     │
├─────────────────────────────────────────────────────────┤
│ Search: [___________]  Filters: [Skill ▼] [Contract ▼]  │
│ ┌─────────────────────────────────────────────────────┐ │
│ │ ID  Name           Contract  SkillMatch  Conflicts  │ │
│ │  3  John Nguyen    Full-time ★★★         0          │ │
│ │  4  Alice Tran     Part-time ★★          0          │ │
│ │  5  Bob Le         Intern    ★           1 ⚠        │ │
│ └─────────────────────────────────────────────────────┘ │
│ Notes/Reason: [______________________________________]  │
│ [Back]                                          [Submit]│
└─────────────────────────────────────────────────────────┘
```

```
┌─────────────────────────────────────────────────────────┐
│ Step 3B (Reschedule): New Date, Slot & Resource       │
├─────────────────────────────────────────────────────────┤
│ New Date: [ 2025-01-03 ]  Time Slot: [ Morning #1 ▼ ]  │
│ Resource: [ Zoom-01 ▼ ] (Suggested: Zoom-01, Zoom-02) │
│ Conflicts: Teacher 0 | Students 0 | Resource 0       │
│ Note: Đã bàn với students về ngày mới                   │
│ Reason: [___________________________________________]  │
│ [Back]                                          [Submit]│
└─────────────────────────────────────────────────────────┘
```

```
┌─────────────────────────────────────────────────────────┐
│ Step 3C (Modality): Resource / Modality                 │
├─────────────────────────────────────────────────────────┤
│ Current: Offline (Room 101)                             │
│ New Modality: [ Online ▼ ]                              │
│ Resource:   [ Zoom-01 ▼ ]                               │
│ Conflicts: Resource 0                                   │
│ Reason: [___________________________________________]  │
│ [Back]                                          [Submit]│
└─────────────────────────────────────────────────────────┘
```

##### Request Detail

```
┌─────────────────────────────────────────────────────────┐
│ Request #912                                            │
├─────────────────────────────────────────────────────────┤
│ Type: SWAP         Status: WAITING_CONFIRM              │
│ Session: #124  2025-01-28  18:00-20:00  Class: ENG-A1   │
│ Proposed Replacement: John Nguyen (ID:3)                │
│ Reason: Bận họp vào slot này                            │
│ Timeline: Submitted → Approved (Staff) → Waiting Confirm│
│ Actions: (Staff) [Approve]/[Reject]  (Replacement) [Confirm] │
└─────────────────────────────────────────────────────────┘
```

---

## 5. DTOs & PAYLOADS

Request tạo Teacher Request (đa dụng cho 3 loại):

```java
@Data
public class TeacherRequestCreateDTO {
    @NotNull
    private Long sessionId;

    @NotNull
    private TeacherRequestType requestType; // SWAP/RESCHEDULE/MODALITY_CHANGE

    private Long replacementTeacherId;      // SWAP (optional)
    private LocalDate newDate;              // RESCHEDULE (optional - Teacher chọn, Staff có thể đổi khi approve)
    private Long newTimeSlotId;             // RESCHEDULE (optional - Teacher chọn, Staff có thể đổi khi approve)
    private Long newResourceId;             // RESCHEDULE/MODALITY_CHANGE (optional - Teacher chọn, Staff có thể đổi khi approve)
    private String reason;                  // optional
}
```

---

## 6. API ENDPOINTS

- GET `/api/v1/teachers/me/sessions?from=&to=`
- GET `/api/v1/teachers/me/sessions/{id}`
- GET `/api/v1/teacher-requests/suggest-resources?classId={id}&date={date}&timeSlotId={id}` - Gợi ý resources cho RESCHEDULE
- POST `/api/v1/teacher-requests`
- GET `/api/v1/teacher-requests/me`
- POST `/api/v1/teacher-requests/{id}/confirm`
- PATCH `/api/v1/teacher-requests/{id}/approve`
- PATCH `/api/v1/teacher-requests/{id}/reject`

Tất cả trả về theo `ResponseObject<T>` thống nhất.

Examples:

Create request (Swap):

```json
POST /api/v1/teacher-requests
{
  "sessionId": 124,
  "requestType": "SWAP",
  "replacementTeacherId": 3,
  "reason": "Bận họp vào slot này"
}
```

Response:

```json
{
  "success": true,
  "message": "Request created",
  "data": { "id": 912, "status": "PENDING" }
}
```

Get suggested resources (khi Teacher chọn date + slot):

```json
GET /api/v1/teacher-requests/suggest-resources?classId=10&date=2025-01-03&timeSlotId=1

Response:
{
  "success": true,
  "message": "Suggested resources",
  "data": [
    {
      "id": 5,
      "name": "Zoom-01",
      "type": "VIRTUAL",
      "available": true,
      "conflicts": 0
    },
    {
      "id": 6,
      "name": "Zoom-02",
      "type": "VIRTUAL",
      "available": true,
      "conflicts": 0
    }
  ]
}
```

Create request (Reschedule):

```json
{
  "sessionId": 224,
  "requestType": "RESCHEDULE",
  "newDate": "2025-01-03",
  "newTimeSlotId": 1,
  "newResourceId": 5,
  "reason": "Xin dời lịch vì công tác (đã bàn với students về ngày mới)"
}
```

Approve Reschedule (Staff - giữ nguyên ngày/resource Teacher chọn):

```json
PATCH /api/v1/teacher-requests/913/approve
{
  "note": "Đã kiểm tra, ngày mới và resource phù hợp với lịch"
}
```

Approve Reschedule (Staff - thay đổi ngày/resource):

```json
PATCH /api/v1/teacher-requests/913/approve
{
  "newDate": "2025-01-05",
  "newTimeSlotId": 2,
  "newResourceId": 7,
  "note": "Đã thay đổi sang ngày khác do xung đột resource"
}
```

Approve (Staff):

```json
PATCH /api/v1/teacher-requests/912/approve
{
  "replacementTeacherId": 3,
  "note": "OK về skill và lịch"
}
```

Response (Swap → WAITING_CONFIRM):

```json
{
  "success": true,
  "message": "Approved and waiting for replacement confirmation",
  "data": { "id": 912, "status": "WAITING_CONFIRM" }
}
```

Reject (Staff):

```json
PATCH /api/v1/teacher-requests/913/reject
{
  "reason": "Không phù hợp thời gian lớp"
}
```

Response:

```json
{
  "success": true,
  "message": "Request rejected",
  "data": { "id": 913, "status": "REJECTED" }
}
```

Confirm (Replacement Teacher for Swap):

```json
POST /api/v1/teacher-requests/912/confirm
{}
```

Response:

```json
{
  "success": true,
  "message": "Swap confirmed",
  "data": { "id": 912, "status": "APPROVED" }
}
```

---

## 7. SERVICE LOGIC

### 7.1 Create Request

- Validate quyền sở hữu session (teacher hiện tại phải là giáo viên của session).
- Theo `requestType` lưu các trường tương ứng vào `teacher_request`.
- Đối với RESCHEDULE: Teacher gửi `new_date`, `new_time_slot_id`, và `new_resource_id` (đã bàn với students) - hệ thống không validate xung đột lúc này, chỉ lưu vào database.
- Status mặc định: `PENDING`.

### 7.1.1 Suggest Resources (RESCHEDULE)

- Endpoint: `GET /api/v1/teacher-requests/suggest-resources?classId={id}&date={date}&timeSlotId={id}`
- Logic: Dựa trên `class.modality` để gợi ý resources phù hợp:
  - `ONLINE` → gợi ý resources có `type = VIRTUAL` (Zoom, Google Meet, etc.)
  - `OFFLINE` → gợi ý resources có `type = ROOM` (physical rooms)
- Kiểm tra availability: Resources không bị book tại `date + time_slot_id` đã chọn.
- Sắp xếp theo: Available first, sau đó theo name.

### 7.2 Approve/Reject (Staff)

- Swap: set `WAITING_CONFIRM` và lưu `replacement_teacher_id` (nếu staff chọn). Gửi email replacement.
- Reschedule: khi approve → Staff có thể giữ nguyên `new_date`, `new_time_slot_id`, và `new_resource_id` từ Teacher hoặc thay đổi (Staff phải validate xung đột: teacher/resource/students) → gọi transaction hủy session cũ, tạo session mới với resource đã chọn, cập nhật `teaching_slot` và `student_session`.
- Modality: khi approve → gọi transaction cập nhật session/resource.
- Reject: cập nhật status `REJECTED` + `decided_by/decided_at`.

### 7.3 Confirm (Replacement Teacher)

- Kiểm tra không trùng lịch; nếu OK → cập nhật `teaching_slot` cho session đó: teacher gốc `on_leave`, replacement `substituted`.
- Cập nhật status `APPROVED`.

---

## 8. DATABASE SCHEMA DETAILS

Tables chính: `teacher_request`, `teaching_slot`, `session`, `session_resource`, `class`, `teacher`, `teacher_availability`, `teacher_skill`.

Indexes khuyến nghị: `idx_session_date_slot_status`, `idx_teaching_slot_teacher_session`, `idx_session_resource_unique(session_id, resource_id)`.

---

## 9. BUSINESS RULES

- Swap: replacement không trùng lịch; ưu tiên skill khớp.
- Reschedule:
  - Teacher chọn `new_date`, `new_time_slot_id`, và `new_resource_id` (đã bàn với students) - hệ thống gợi ý resource dựa trên `class.modality` (ONLINE → VIRTUAL resources, OFFLINE → ROOM resources).
  - Hệ thống không validate xung đột lúc Teacher tạo request, chỉ lưu vào database.
  - Staff khi approve: có thể giữ nguyên hoặc thay đổi ngày/slot/resource; Staff phải validate xung đột (teacher/resource/students) trước khi approve.
  - Session cũ → CANCELLED, session mới copy metadata chính và sử dụng resource đã chọn.
- Modality: resource rảnh trên khung giờ; cập nhật `session_resource` và `class.modality` theo loại resource.
- Time window: chỉ cho phép tạo request cho các session chưa diễn ra trong 7 ngày tới.

---

## 10. EDGE CASES & ERROR HANDLING

- Double submit request cùng session & type → trả lỗi business.
- Replacement thay đổi sau khi đã confirm → chặn hoặc tạo request mới.
- Reschedule sang ngày quá khứ → chặn.
- Email lỗi: log cảnh báo, không rollback transaction chính.

---

## 11. EMAIL/NOTIFICATION FLOW

- Swap: Teacher → Staff → Replacement → Teacher (final).
- Reschedule: Teacher ↔ Staff.
- Modality: Teacher ↔ Staff.

Nội dung: subject + session summary + thời gian + call-to-action link.

---

## 12. SQL SNIPPETS FOR VALIDATION

Kiểm tra trùng lịch giáo viên:

```sql
SELECT 1 FROM teaching_slot ts
JOIN session s ON s.id = ts.session_id
WHERE ts.teacher_id = :teacher_id
  AND s.date = :date
  AND s.time_slot_template_id = :time_slot_template_id
  AND s.status IN ('planned','done')
LIMIT 1;
```

Kiểm tra resource bận:

```sql
SELECT 1 FROM session s
JOIN session_resource sr ON sr.session_id = s.id
WHERE sr.resource_id = :resource_id
  AND s.date = :date
  AND s.time_slot_template_id = :time_slot_template_id
  AND s.status IN ('planned','done')
LIMIT 1;
```

---

## 13. TESTING STRATEGY

- Unit tests: TeacherRequestService (create/approve/reject/confirm).
- Integration: transaction cập nhật session/slot/resource; xác thực lifecycle `teacher_request`.
- SQL playground: chạy các snippet trên với seed.

---

## 14. SECURITY & AUTHORIZATION

- `@PreAuthorize("hasRole('TEACHER')")` cho endpoints teacher; kiểm tra ownership bằng `teacher_id` từ `SecurityContext`.
- `@PreAuthorize("hasRole('ACADEMIC_STAFF')")` cho approve/reject.

---

## 15. PERFORMANCE NOTES

- Trước khi commit Swap/Reschedule, kiểm tra lại xung đột (double-check) để tránh race conditions.
- Thêm index theo gợi ý để giữ truy vấn < 100ms.

---

Document Status: Ready for Implementation
