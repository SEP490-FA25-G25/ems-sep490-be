# 📄 Product Requirements Document (PRD)

## Hệ Thống Quản Lý Đào Tạo - Training Management System (TMS)

---

## 1. THÔNG TIN CHUNG

| Thông tin | Chi tiết |
|-----------|----------|
| **Tên dự án** | Training Management System (TMS/EMS - Education Management System) |
| **Người chịu trách nhiệm** | Product Manager - TMS Team |
| **Ngày tạo** | 29/10/2025 |
| **Phiên bản** | 1.0 |
| **Trạng thái** | Draft |
| **Loại hệ thống** | B2B SaaS Platform cho các Trung tâm Đào tạo Ngôn ngữ |

### Tóm tắt (Executive Summary)

TMS là một **hệ thống quản lý đào tạo toàn diện** được thiết kế cho các trung tâm đào tạo ngôn ngữ đa chi nhánh (tiếng Anh, tiếng Nhật, tiếng Trung). Hệ thống giải quyết các vấn đề phức tạp trong vận hành đào tạo từ thiết kế giáo trình đến quản lý lớp học, điểm danh, và đảm bảo chất lượng.

**Giá trị cốt lõi:**
- **Cho Giáo vụ**: Giảm thời gian lập lịch từ vài tuần xuống vài giờ thông qua tự động hóa
- **Cho Giáo viên**: Rõ ràng về lịch dạy, dễ dàng xử lý yêu cầu nghỉ/đổi lịch, cơ hội OT công bằng
- **Cho Học viên**: Linh hoạt học bù/chuyển lớp, minh bạch điểm danh/điểm số, đảm bảo chất lượng
- **Cho Quản lý**: Thông tin vận hành real-time, phát hiện vấn đề chủ động, mở rộng đa chi nhánh

---

---

## 2. BỐI CẢNH VÀ MỤC TIÊU

### 2.1 Vấn đề (The Problem)

#### Mô tả chi tiết vấn đề

Các trung tâm đào tạo ngôn ngữ đang đối mặt với những thách thức vận hành phức tạp:

**1. Vận hành đa chi nhánh (Multi-tenant operations):**
- Quản lý nhiều trung tâm với nhiều chi nhánh, mỗi chi nhánh cần vận hành độc lập nhưng phối hợp
- Khó khăn trong việc đồng bộ dữ liệu và chia sẻ tài nguyên (giáo viên, phòng học, tài liệu)

**2. Lập lịch phức tạp (Complex scheduling):**
- Phối hợp giáo viên, phòng học, nền tảng online (Zoom), và lịch học của học viên
- Xử lý nhiều time slots, modalities khác nhau (offline/online/hybrid)
- Phát hiện và giải quyết xung đột lịch

**3. Thay đổi động liên tục (Dynamic changes):**
- Yêu cầu thay đổi lịch, giáo viên nghỉ, học viên học bù, chuyển lớp rất thường xuyên
- Khó theo dõi và đảm bảo tính công bằng trong xử lý

**4. Tính toàn vẹn của giáo trình (Curriculum integrity):**
- Đảm bảo giảng dạy giáo trình chuẩn hóa nhưng vẫn linh hoạt trong thực thi
- Khó theo dõi tiến độ thực tế so với kế hoạch

**5. Theo dõi chất lượng (Quality tracking):**
- Giám sát learning outcomes (PLO/CLO), tỷ lệ điểm danh, chất lượng giảng dạy
- Thiếu dữ liệu để đưa ra quyết định cải tiến

#### Tác động hiện tại

- **Tốn thời gian**: Giáo vụ mất 3-5 ngày/tuần để xếp lịch thủ công
- **Sai sót**: Xung đột lịch, double-booking phòng/giáo viên (10-15% classes)
- **Mất học viên**: 35% học viên rời sau 6 tháng do dịch vụ không linh hoạt
- **Chi phí cao**: Không tối ưu tài nguyên, lãng phí thời gian phòng trống
- **Chất lượng thấp**: Không theo dõi được hiệu quả học tập, thiếu QA

#### Dữ liệu/Số liệu minh chứng

- **Khảo sát**: 78% trung tâm đang dùng Excel + WhatsApp để quản lý
- **Điểm đau**: 68% học viên sử dụng app vào tối/đêm phàn nàn giao diện sáng (thiếu dark mode)
- **Retention**: Tỷ lệ học viên rời trung tâm sau 6 tháng: 35%
- **Efficiency**: Giáo vụ dành 60% thời gian cho các tác vụ có thể tự động hóa

---

### 2.2 Mục tiêu (Objectives)

#### Mục tiêu chính (Primary Goals)

**PG1: Tự động hóa quy trình vận hành**
- Giảm 80% thời gian lập lịch (từ 3 ngày xuống < 1 ngày)
- Tự động phát hiện xung đột và đề xuất giải pháp
- Tự động sinh lịch học cá nhân cho học viên

**PG2: Tăng tính linh hoạt cho học viên**
- Cho phép học bù cross-class (học bù ở lớp khác cùng nội dung)
- Cho phép chuyển lớp mid-course (giữ nguyên tiến độ học tập)
- Giảm 20% complaint về UX trong 3 tháng

**PG3: Minh bạch và đảm bảo chất lượng**
- Track 100% attendance và learning outcomes theo chuẩn PLO/CLO
- Cung cấp dashboard real-time cho quản lý
- Tăng app store rating từ 4.2 lên 4.5

#### Mục tiêu phụ (Secondary Goals)

**SG1: Tối ưu tài nguyên**
- Tăng 25% utilization của phòng học và Zoom licenses
- Giảm 15% chi phí vận hành thông qua tối ưu hóa

**SG2: Hỗ trợ mở rộng quy mô**
- Cho phép mở thêm chi nhánh mới trong 1 tuần (thay vì 1 tháng)
- Hỗ trợ tối thiểu 10 chi nhánh, 500+ classes đồng thời

**SG3: Nâng cao trải nghiệm người dùng**
- Mobile-first design cho teachers và students
- Notification real-time cho thay đổi lịch

#### Liên kết với mục tiêu kinh doanh tổng thể

- **Revenue Growth**: Tăng 15% enrollment nhờ tính linh hoạt cao
- **Cost Reduction**: Giảm 20% operational cost thông qua automation
- **Customer Satisfaction**: Tăng NPS từ 35 lên 50
- **Scalability**: Hỗ trợ mở rộng từ 3 chi nhánh lên 10+ chi nhánh

---

### 2.3 Đối tượng người dùng (Target Audience)

#### Chân dung người dùng (User Personas)

**Persona 1: Giáo vụ (Academic Affair) - "Người điều phối"**
- **Vai trò**: Vận hành hàng ngày tại chi nhánh
- **Độ tuổi**: 25-40 tuổi
- **Tech-savvy**: Trung bình
- **Pain points**: 
  - Lập lịch thủ công tốn thời gian
  - Xử lý conflict giữa giáo viên/phòng/học viên
  - Xử lý yêu cầu nghỉ/học bù/chuyển lớp liên tục
- **Needs**: Công cụ tự động xếp lịch, phát hiện conflict, xử lý request nhanh

**Persona 2: Giáo viên (Teacher) - "Người thực thi"**
- **Vai trò**: Giảng dạy và điểm danh
- **Độ tuổi**: 25-45 tuổi
- **Tech-savvy**: Trung bình - Cao
- **Pain points**:
  - Lịch dạy thay đổi đột ngột
  - Xin nghỉ/đổi lịch phức tạp
  - Thiếu cơ hội OT rõ ràng
- **Needs**: Lịch dạy rõ ràng, dễ dàng điểm danh, xử lý request nhanh, OT công bằng

**Persona 3: Học viên (Student) - "Người học"**
- **Vai trò**: Tham gia lớp học
- **Độ tuổi**: 18-45 tuổi
- **Tech-savvy**: Cao
- **Pain points**:
  - Bỏ lỡ buổi học không học bù được
  - Muốn chuyển lớp (online/offline) nhưng mất tiến độ
  - Không biết điểm danh/điểm số của mình
- **Needs**: Học bù linh hoạt, chuyển lớp giữ tiến độ, xem điểm/lịch học

**Persona 4: Trưởng phòng (Manager/Center Head) - "Người quyết định"**
- **Vai trò**: Quản lý chiến lược và vận hành
- **Độ tuổi**: 35-55 tuổi
- **Tech-savvy**: Trung bình
- **Pain points**:
  - Thiếu dữ liệu để đưa ra quyết định
  - Không giám sát được chất lượng đào tạo
  - Khó mở rộng quy mô
- **Needs**: Dashboard KPI, báo cáo chất lượng, công cụ mở rộng

**Persona 5: Trưởng bộ môn (Subject Leader) - "Người thiết kế"**
- **Vai trò**: Thiết kế giáo trình và learning outcomes
- **Độ tuổi**: 30-50 tuổi
- **Tech-savvy**: Trung bình
- **Pain points**:
  - Thiết kế giáo trình phức tạp
  - Khó track learning outcomes
- **Needs**: Công cụ thiết kế giáo trình, mapping PLO/CLO

---

## 3. YÊU CẦU CHI TIẾT

### 3.1 User Stories

#### Must-Have (P0 - Release Blockers)

**Epic 1: Quản lý Giáo trình (Curriculum Management)**

- **US-CUR-001**: Là một Subject Leader, tôi muốn tạo Subject mới (ví dụ: "Chinese"), để có thể tổ chức các khóa học theo ngôn ngữ.
  - **Acceptance Criteria**: Có thể tạo subject với code unique, name, description
  
- **US-CUR-002**: Là một Subject Leader, tôi muốn định nghĩa các Level cho Subject (ví dụ: HSK1, HSK2,...), để phân loại trình độ học viên.
  - **Acceptance Criteria**: Tạo levels với expected duration, sort order, prerequisites

- **US-CUR-003**: Là một Subject Leader, tôi muốn tạo Course cho từng Level với PLO/CLO rõ ràng, để đảm bảo chất lượng đầu ra.
  - **Acceptance Criteria**: Tạo course với phases, sessions, CLOs mapped to PLOs

- **US-CUR-004**: Là một Manager, tôi muốn approve/reject courses trước khi sử dụng, để đảm bảo chất lượng giáo trình.
  - **Acceptance Criteria**: Xem chi tiết course, approve với lý do, reject với feedback

**Epic 2: Quản lý Lớp học (Class Management)**

- **US-CLS-001**: Là một Academic Affair, tôi muốn tạo lớp học từ course đã approve, để bắt đầu enrollment.
  - **Acceptance Criteria**: Chọn course, branch, modality, start date, schedule days, max capacity

- **US-CLS-002**: Là một Academic Affair, tôi muốn hệ thống tự động sinh 36 sessions từ course template, để tiết kiệm thời gian.
  - **Acceptance Criteria**: Sessions tự động sinh với date tính từ start_date, schedule_days, course_sessions

- **US-CLS-003**: Là một Academic Affair, tôi muốn assign time slots, phòng/Zoom, và giáo viên cho sessions, để hoàn thiện lịch học.
  - **Acceptance Criteria**: Assign resources với conflict detection, assign teachers với skill matching

- **US-CLS-004**: Là một Center Head, tôi muốn approve class trước khi enrollment, để đảm bảo tính khả thi.
  - **Acceptance Criteria**: Xem chi tiết class (sessions, resources, teachers), approve/reject

**Epic 3: Ghi danh Học viên (Student Enrollment)**

- **US-ENR-001**: Là một Academic Affair, tôi muốn ghi danh học viên vào lớp đã approve, để họ có thể bắt đầu học.
  - **Acceptance Criteria**: Chọn students từ danh sách, import CSV, capacity validation, schedule conflict check

- **US-ENR-002**: Là một Academic Affair, tôi muốn hệ thống tự động sinh student_session cho mỗi học viên, để họ có lịch học cá nhân.
  - **Acceptance Criteria**: Mỗi enrollment tự động tạo student_session cho tất cả future sessions

**Epic 4: Điểm danh và Báo cáo (Attendance & Reporting)**

- **US-ATT-001**: Là một Teacher, tôi muốn điểm danh cho học viên trong từng buổi học, để track attendance.
  - **Acceptance Criteria**: Xem danh sách students, mark present/absent, save attendance. Late/excused cases track qua note field.

- **US-ATT-002**: Là một Teacher, tôi muốn chấm homework cho học viên, để đánh giá tiến độ học tập.
  - **Acceptance Criteria**: Nếu session có homework, có thể mark completed/incomplete

- **US-ATT-003**: Là một Teacher, tôi muốn submit session report sau khi dạy, để ghi lại nội dung đã dạy.
  - **Acceptance Criteria**: Điền actual content taught, teaching notes, update session status to "done"

**Epic 5: Yêu cầu Học viên (Student Requests)**

- **US-REQ-STU-001**: Là một Student, tôi muốn xin nghỉ buổi học trước, để không bị tính absent.
  - **Acceptance Criteria**: Chọn session, nhập lý do, submit, sau khi approve → attendance_status = "excused"

- **US-REQ-STU-002**: Là một Student, tôi muốn xin học bù cho buổi đã nghỉ, để không bỏ lỡ nội dung học.
  - **Acceptance Criteria**: Chọn missed session, hệ thống tìm makeup sessions (same course_session_id), chọn makeup session, submit

- **US-REQ-STU-003**: Là một Student, tôi muốn chuyển lớp mid-course (ví dụ: offline → online), để phù hợp với lịch cá nhân.
  - **Acceptance Criteria**: Chọn target class (same course_id), effective date, hệ thống map sessions theo course_session_id

**Epic 6: Yêu cầu Giáo viên (Teacher Requests)**

- **US-REQ-TEA-001**: Là một Teacher, khi tôi nghỉ, tôi muốn tìm người dạy thay (swap), để đảm bảo buổi học vẫn diễn ra.
  - **Acceptance Criteria**: Tìm colleague đồng ý, tạo swap request, colleague confirm, Academic Affair approve

- **US-REQ-TEA-002**: Là một Teacher, khi không tìm được người thay, tôi muốn reschedule để dạy bù, để thực hiện trách nhiệm.
  - **Acceptance Criteria**: Chọn session, chọn new date/time/resource, submit, Academic Affair approve → create new session type="teacher_reschedule"

- **US-REQ-TEA-003**: Là một Teacher, khi không dạy offline được, tôi muốn chuyển sang dạy online, để buổi học vẫn diễn ra.
  - **Acceptance Criteria**: Request type = "modality_change", chọn Zoom link, Academic Affair approve → update resource, notify students

**Epic 7: Báo cáo và Dashboard (Reporting & Analytics)**

- **US-RPT-001**: Là một Manager, tôi muốn xem dashboard KPI (enrollment, attendance, workload), để đưa ra quyết định.
  - **Acceptance Criteria**: Dashboard hiển thị enrollment rate, attendance rate, teacher workload, room utilization

---

#### Should-Have (P1 - Important but not blockers)

- **US-QA-001**: Là một QA, tôi muốn tạo QA reports cho classes/sessions, để theo dõi chất lượng.
- **US-ASS-001**: Là một Teacher, tôi muốn nhập điểm cho assessments, để đánh giá học viên.
- **US-FB-001**: Là một Student, tôi muốn đánh giá buổi học theo template questions, để cải thiện chất lượng.
  - **Acceptance Criteria**: Trả lời các feedback questions (rating-based), submit feedback cho class/phase
- **US-MAT-001**: Là một Subject Leader, tôi muốn upload materials cho course/phase/session, để chia sẻ tài liệu.

---

#### Nice-to-Have (P2 - Future enhancements)

- **US-NOT-001**: Là một User, tôi muốn nhận notifications real-time khi có thay đổi lịch.
- **US-MSG-001**: Là một User, tôi muốn chat với teacher/student trong hệ thống.
- **US-PAY-001**: Là một Student, tôi muốn thanh toán học phí online, để tiện lợi.
- **US-CRT-001**: Là một Student, tôi muốn xem/download certificate sau khi hoàn thành khóa học.

---

### 3.2 Yêu cầu chức năng (Functional Requirements)

#### FR-1: Quản lý Giáo trình (Curriculum Management Module)

**FR-1.1: Subject Management**
- Tạo/Sửa/Xóa Subject (code, name, description, status)
- Validate unique subject code
- Track created_by, created_at

**FR-1.2: Level Management**
- Tạo Levels cho Subject (code, name, standard_type, expected_duration_hours, sort_order)
- Validate unique (subject_id, code)

**FR-1.3: PLO (Program Learning Outcomes) Management**
- Tạo PLOs cho Subject (code, description)
- Validate unique (subject_id, code)

**FR-1.4: Course Design**
- Tạo Course cho Level (total_hours, duration_weeks, session_per_week, prerequisites, target_audience)
- Status workflow: draft → submitted → approved/rejected
- Generate logical_course_code + version

**FR-1.5: CLO (Course Learning Outcomes) Management**
- Tạo CLOs cho Course (code, description)
- Mapping PLOs ↔ CLOs (ma trận mapping)

**FR-1.6: Course Phase & Session Template**
- Tạo Phases cho Course (phase_number, duration_weeks, learning_focus)
- Tạo Course Sessions cho Phase (sequence_no, topic, student_task, skill_set)
- Validate tổng số sessions = duration_weeks × session_per_week

**FR-1.7: CLO Mapping to Sessions**
- Mapping CLOs to Course Sessions
- Validate: mỗi CLO phải map ít nhất 1 session, mỗi session phải có ít nhất 1 CLO

**FR-1.8: Course Assessment Framework**
- Tạo Course Assessments (name, kind, max_score, skills)
- Mapping Assessments ↔ CLOs
- Validate: mỗi CLO phải có ít nhất 1 assessment

**FR-1.9: Course Materials**
- Upload materials cho Course/Phase/Session
- Fields: title, description, material_type (video/pdf/slide/audio/document/other), url, uploaded_by, uploaded_at
- Categorize materials theo type để dễ filter và organize
- Track upload metadata (uploaded_by, uploaded_at)

**FR-1.10: Course Approval Workflow**
- Subject Leader submit course → Manager review → Approve/Reject
- Course có 2 status fields:
  - `course.status`: Lifecycle (draft/active/inactive) - controlled by effective_date
  - `course.approval_status`: Workflow (pending/approved/rejected) - controlled by Manager
- Submit flow:
  - Subject Leader submit → `approval_status` = "pending"
  - Manager approve → `approval_status` = "approved", `decided_by_manager`, `decided_at`
  - Effective date: Khi `effective_date` đến → cronjob update `status` = "active"
  - Manager reject → `approval_status` = "rejected", `rejection_reason`
- Optimistic locking: `hash_checksum` để detect concurrent updates (Manager đang review nhưng Subject Leader edit)

---

#### FR-2: Quản lý Lớp học (Class Management Module)

**FR-2.1: Class Creation**
- Academic Affair tạo class từ approved course
- Input: branch, course, code, modality (offline/online/hybrid), start_date, schedule_days, max_capacity
- Status: draft → submitted → scheduled → ongoing → completed

**FR-2.2: Session Auto-Generation**
- Tự động sinh sessions từ course_sessions
- Tính toán date dựa trên start_date + schedule_days + week offset
- Bỏ qua ngày lễ (configurable)

**FR-2.3: Time Slot Assignment**
- Assign time_slot_template cho mỗi day_of_week
- Có thể assign khác nhau cho từng ngày (ví dụ: Mon 08:00, Wed 14:00)

**FR-2.4: Resource Assignment**
- Assign phòng (resource_type = "room") cho OFFLINE classes
- Assign Zoom (resource_type = "virtual") cho ONLINE classes
- Assign cả hai cho HYBRID classes
- Conflict detection: không double-book cùng resource/date/time

**FR-2.5: Teacher Assignment**
- Tìm teachers có skill match với course_session.skill_set
- Check teacher availability (teacher_availability + teacher_availability_override)
- Check teaching conflicts (không dạy 2 sessions cùng lúc)
- Assign teachers với role (primary/assistant)

**FR-2.6: Class Validation**
- Check tất cả sessions có time_slot, resource, teacher
- Completion percentage = 100% → có thể submit

**FR-2.7: Class Approval Workflow**
- Academic Affair submit class → Center Head (branch) hoặc Manager (cross-branch) review → Approve/Reject
- Class có 2 status fields:
  - `class.status`: Lifecycle (draft/scheduled/ongoing/completed/cancelled)
  - `class.approval_status`: Workflow (pending/approved/rejected)
- Submit flow:
  - Academic Affair submit → `approval_status` = "pending", `submitted_at`
  - Center Head/Manager approve → `approval_status` = "approved", `status` = "scheduled", `decided_by`, `decided_at`
  - Center Head/Manager reject → `approval_status` = "rejected", `rejection_reason`
- Class cancelled: `status` = "cancelled" (ví dụ: không đủ students, teacher nghỉ dài hạn)

---

#### FR-3: Ghi danh Học viên (Student Enrollment Module)

**FR-3.1: Student List Management**
- Load tất cả students thuộc branch
- Hiển thị: student_code, full_name, email, phone, assessment scores, enrollment status
- Priority scoring: level match, assessment gần nhất, chưa enroll

**FR-3.2: Add Student (Manual)**
- Tạo user_account → student → assign role → assign branch → create skill assessments
- Validate unique email, phone, student_code

**FR-3.3: Import Students (CSV)**
- Parse CSV, validate từng row
- Batch create users + students
- Hiển thị preview với valid/warning/error

**FR-3.4: Enrollment Process**
- Select students (multi-select hoặc import CSV)
- Capacity validation: enrolled_count + selected_count ≤ max_capacity
- Schedule conflict check: students không học 2 classes cùng lúc
- Capacity overflow handling:
  - Option 1: Reject enrollment (hiển thị warning)
  - Option 2: Override capacity (với lý do và approval)
  - Note: Không có "waitlisted" status - học viên phải đợi hoặc enroll vào class khác
- Track enrolled_by (user_id của Academic Affair thực hiện enrollment)

**FR-3.5: Auto-Generate Student Sessions**
- Với mỗi enrollment, tạo student_session cho tất cả future sessions
- student_session: (student_id, session_id, attendance_status = "planned", is_makeup = false)

**FR-3.6: Mid-Course Enrollment**
- Nếu enroll sau start_date, chỉ tạo student_session cho future sessions
- Track join_session_id trong enrollment

---

#### FR-4: Điểm danh và Báo cáo (Attendance & Session Reporting Module)

**FR-4.1: Teacher View Classes**
- Load classes có sessions hôm nay của teacher
- Hiển thị: class_code, course_name, session_count_today

**FR-4.2: Session Selection**
- Load sessions hôm nay của class
- Hiển thị: date, time, topic, student_count, status

**FR-4.3: Attendance Recording**
- Load students từ student_session
- Hiển thị: student_code, full_name, attendance_status, is_makeup, homework_status
- Mark attendance: present/absent
- Mark homework: completed/incomplete/no_homework (nếu có student_task)
- Real-time summary: present_count, absent_count, homework_completed_count
- Note: Late/excused cases track qua `student_session.note` field

**FR-4.4: Attendance Validation**
- Chỉ điểm danh được trong ngày session (session.date = CURRENT_DATE)
- Qua ngày khác không sửa được (attendance lock)
- Chỉ teacher được phân công mới có thể điểm danh

**FR-4.5: Session Report Submission**
- Teacher điền: actual_content_taught, teaching_notes
- Attendance summary auto-filled
- Update session.status = "done", session.teacher_note
- Validate: đã điểm danh đủ (không còn "planned")

---

#### FR-5: Yêu cầu Học viên (Student Request Management Module)

**FR-5.1: Absence Request**

**Luồng 1: Student tự tạo request (Primary Flow)**
- Student login → My Requests → Create Request
- Chọn request type = "Absence"
- Chọn ngày → chọn class → chọn session cần nghỉ
- Nhập lý do (required, min 10 chars)
- Submit → status = "pending"
- Academic Affair review → Approve/Reject
- Approve → update student_session.attendance_status = "absent", note = "Approved absence: [reason]"

**Luồng 2: Academic Affair tạo thay mặt Student (Alternative Flow)**
- Academic Affair nhận request ngoài hệ thống (WhatsApp/phone/email)
- Academic Affair tạo request trong hệ thống:
  - Chọn student, class, session, nhập lý do
  - Submit → status = "waiting_confirm"
  - Student confirm → status = "pending"
  - Academic Affair approve → execute

**Validation**: session.status = "planned", session.date >= CURRENT_DATE, không duplicate request

**FR-5.2: Makeup Request**

**Luồng 1: Student tự tạo request (Primary Flow)**
- Student login → My Requests → Create Request
- Chọn request type = "Makeup"
- **Option A**: Chọn buổi đã nghỉ (missed sessions trong X tuần gần nhất, attendance_status = "absent")
- **Option B**: Chọn buổi tương lai sẽ nghỉ (future session, attendance_status = "planned")
- Hệ thống tìm available makeup sessions:
  - Same course_session_id (cùng nội dung)
  - Status = "planned", date >= CURRENT_DATE
  - Còn chỗ (enrolled_count < max_capacity)
  - Prioritize: same branch → same modality → soonest date → most slots
- Student chọn preferred makeup session, nhập lý do
- Submit → status = "pending"
- Academic Affair review → Approve/Reject
- Approve → transaction (xem details below)

**Luồng 2: Academic Affair tạo thay mặt Student**
- Academic Affair nhận request qua WhatsApp/phone
- Academic Affair tìm makeup sessions → tạo request
- Submit → status = "waiting_confirm"
- Student confirm → status = "pending"
- Academic Affair approve → execute

**Approval Transaction:**
- Update target session: attendance_status = "absent", note = "Approved for makeup session #X"
- Create new student_session: (is_makeup = TRUE, makeup_session_id, original_session_id)

**Validation**: course_session_id match, capacity available, không duplicate

**FR-5.3: Transfer Request (Class Transfer)**

**Luồng 1: Student tự tạo request (Primary Flow)**
- Student login → My Requests → Create Request
- Chọn request type = "Transfer"
- Chọn current_class (đang học)
- Hệ thống tìm available target classes:
  - Same course_id (cùng giáo trình)
  - Status = "scheduled"/"ongoing"
  - Còn chỗ
  - Hiển thị: branch, modality, schedule, available_slots
- Student chọn target_class, chọn effective_date, nhập lý do
- Submit → status = "pending"
- Academic Affair review → Check content gap
- Approve → transaction (xem below)

**Luồng 2: Academic Affair tạo thay mặt Student**
- Academic Affair nhận request transfer qua WhatsApp/phone
- Academic Affair tìm target class phù hợp, validate
- Tạo transfer request → Student confirm → Academic Affair approve

**Approval Transaction:**
- Update current enrollment: status = "transferred", left_at, left_session_id
- Create new enrollment: status = "enrolled", enrolled_at, join_session_id
- Update future sessions in current class: attendance_status = "absent", note = "Transferred to class X"
- Generate student_sessions for future sessions in target class

**Validation**: Same course_id, target class available, no critical content gaps

---

#### FR-6: Yêu cầu Giáo viên (Teacher Request Management Module)

**FR-6.1: Teacher Absence & Substitute (Swap Request)**
- **Business Rule**: Teacher nghỉ = phải có trách nhiệm tìm người dạy thay hoặc dạy bù
- **Luồng 1**: Teacher tự tìm substitute
  - Teacher liên hệ colleague (ngoài hệ thống)
  - Colleague đồng ý → Teacher/Academic Affair tạo swap request trong hệ thống
  - Request type = "swap", replacement_teacher_id, session_id
  - Replacement teacher confirm → status = "waiting_confirm" → "pending"
  - Academic Affair approve → update teaching_slot.teacher_id, teaching_slot.status = "substituted"
  - Track: teacher_request.replacement_teacher_id

**Luồng 2**: Academic Affair tìm substitute thay
  - Teacher báo nghỉ gấp (WhatsApp/phone)
  - Academic Affair tìm available teachers (skill match, availability, no conflict)
  - Academic Affair tạo swap request → Teacher confirm → Approve
  - Execute: update teaching_slot

**FR-6.2: Reschedule Request (Teacher muốn đổi lịch dạy bù)**
- **Business Rule**: Nếu không tìm được substitute → phải reschedule để dạy bù
- **Luồng 1**: Teacher tự tạo request
  - Teacher login → Requests → Create Request
  - Request type = "reschedule", chọn session (trong 7 ngày tới)
  - Chọn new_date, new_time_slot_id, new_resource_id
  - Submit → status = "pending"
  - Academic Affair validate (resource available, no conflicts) → Approve
  - Execute: create new session (type='teacher_reschedule'), cancel old session

**Luồng 2**: Academic Affair tạo thay mặt Teacher
  - Teacher báo cần đổi lịch
  - Academic Affair tìm slot available → tạo request → Teacher confirm → Approve

**Approval Transaction:**
- Cancel old session: session.status = "cancelled"
- Create new session: (class_id, course_session_id, new_date, new_time_slot, type='teacher_reschedule', status='planned')
- Transfer teaching_slots, student_sessions sang session mới
- Track: teacher_request.session_id (old), new_session_id (new)

**FR-6.3: Modality Change Request (Không dạy offline được → chuyển online)**
- **Use Cases**:
  - Phòng học hỏng AC/máy chiếu → chuyển online gấp
  - Teacher ốm nhẹ, không đến được → dạy online từ nhà
  - Dịch bệnh → chuyển toàn bộ class sang online

**Luồng 1**: Teacher/Academic Affair tạo request
  - Request type = "modality_change"
  - Chọn session, chọn new_resource_id (room→zoom or zoom→room)
  - Submit → Academic Affair validate → Approve
  - Execute: update session_resource, notify all students

**Validation**: Resource mới phù hợp với modality mới, resource available

**Priority Flow khi Teacher nghỉ:**
1. Tìm substitute (swap) - Best option
2. Nếu không có substitute → Reschedule để dạy bù - OK option
3. Nếu không reschedule được → Modality change (offline→online) - Acceptable
4. Nếu hết cách → Cancel session (last resort) - session.status = "cancelled"

---

#### FR-7: Báo cáo và Dashboard (Reporting & Analytics Module)

**FR-7.1: Enrollment Dashboard**
- Total students by branch/level/course
- Fill rate by class (enrolled / max_capacity)
- Trial-to-enrollment conversion rate

**FR-7.2: Attendance Dashboard**
- Attendance rate by class/branch/teacher
- Top absences (students với most absences)
- Alert: students vượt absence threshold

**FR-7.3: Teacher Workload Dashboard**
- Total teaching hours per teacher (by week/month)
- Number of classes per teacher
- OT hours per teacher (for payroll)

**FR-7.4: Class Progress Dashboard**
- % syllabus completed vs scheduled
- Session completion rate (done/planned/cancelled)
- Deviation from planned_end_date

**FR-7.5: Quality Dashboard**
- Average student feedback rating by class/teacher
- QA report summary (open issues by branch)
- CLO achievement rate (% sessions achieving target CLOs)

**FR-7.6: Resource Utilization Dashboard**
- Room occupancy rate (used hours / available hours)
- Zoom license utilization (concurrent sessions / total licenses)
- Peak usage times

**FR-7.7: Assessment Dashboard**
- Schedule vs actual assessment dates (`assessment.scheduled_date` vs `actual_date`)
- Assessment completion rate by class
- Average scores by assessment type (quiz/midterm/final)
- Score distribution by assessment

**FR-7.8: Material Analytics**
- Material count by type (video/pdf/slide/audio/document)
- Material coverage by course/phase/session
- Missing materials warnings (sessions without materials)

---

### 3.3 Yêu cầu phi chức năng (Non-Functional Requirements)

#### NFR-1: Hiệu suất (Performance)

- **NFR-1.1**: Page load time < 2 giây (95th percentile)
- **NFR-1.2**: API response time < 500ms (p95)
- **NFR-1.3**: Complex queries (conflict detection, makeup search) < 1 giây
- **NFR-1.4**: Batch operations (import CSV, enroll 100 students) < 10 giây

#### NFR-2: Bảo mật (Security)

- **NFR-2.1**: Authentication: JWT với refresh token
- **NFR-2.2**: Authorization: Role-based access control (RBAC) theo roles định nghĩa
- **NFR-2.3**: Encryption: TLS 1.3 cho all network traffic
- **NFR-2.4**: Password: Bcrypt hashing với salt rounds ≥ 10
- **NFR-2.5**: Data privacy: Tuân thủ GDPR/PDPA (sensitive data encrypted at rest)
- **NFR-2.6**: Audit trail: Log tất cả critical actions (enrollment, approval, deletion)

#### NFR-3: Khả năng mở rộng (Scalability)

- **NFR-3.1**: Hỗ trợ tối thiểu 10 centers, 50 branches
- **NFR-3.2**: Hỗ trợ 500+ classes đồng thời
- **NFR-3.3**: Hỗ trợ 10,000+ students
- **NFR-3.4**: Horizontal scaling: stateless API servers
- **NFR-3.5**: Database: PostgreSQL với connection pooling, read replicas

#### NFR-4: Tính khả dụng (Availability)

- **NFR-4.1**: Uptime ≥ 99.5% (excluding planned maintenance)
- **NFR-4.2**: Planned maintenance: < 4 hours/month, off-peak hours
- **NFR-4.3**: Backup: Daily automated backups, 30-day retention
- **NFR-4.4**: Disaster recovery: RTO < 4 hours, RPO < 1 hour

#### NFR-5: Tính tương thích (Compatibility)

- **NFR-5.1**: Browsers: Chrome, Firefox, Safari, Edge (latest 2 versions)
- **NFR-5.2**: Mobile: Responsive design cho iOS, Android (native apps optional)
- **NFR-5.3**: OS: Platform-independent (web-based)

#### NFR-6: Usability & Accessibility

- **NFR-6.1**: UI/UX: Material Design hoặc tương đương
- **NFR-6.2**: Accessibility: WCAG 2.1 Level AA compliance
- **NFR-6.3**: Internationalization: Support tiếng Việt, English (expandable)
- **NFR-6.4**: Dark mode: Support cho teachers/students sử dụng vào tối

#### NFR-7: Maintainability

- **NFR-7.1**: Code quality: SonarQube grade A, test coverage ≥ 80%
- **NFR-7.2**: Documentation: Swagger/OpenAPI for APIs, inline code comments
- **NFR-7.3**: Logging: Centralized logging (ELK stack hoặc tương đương)
- **NFR-7.4**: Monitoring: APM (Application Performance Monitoring) với alerts

---

## 4. THIẾT KẾ VÀ TRẢI NGHIỆM NGƯỜI DÙNG

### 4.1 Wireframes & Mockups

**[Liên kết đến Figma/Design Files - TBD]**

Các màn hình chính:

**1. Dashboard cho từng role:**
- Academic Affair Dashboard: Pending requests, conflict alerts, enrollment stats
- Teacher Dashboard: Today's sessions, upcoming schedule, OT opportunities
- Student Dashboard: Upcoming sessions, attendance summary, grades
- Manager Dashboard: KPI summary, branch performance, alerts

**2. Class Creation Wizard (Academic Affair):**
- Step 1: Select course, branch, modality
- Step 2: Set start date, schedule days
- Step 3: Assign time slots (per day)
- Step 4: Assign resources (auto-conflict detection)
- Step 5: Assign teachers (skill matching, availability)
- Step 6: Review & Submit for approval

**3. Attendance Screen (Teacher):**
- Session header: Class, date, time, topic
- Student list: code, name, status badges (makeup, excused)
- Mark attendance: Radio buttons (absent/present)
- Mark homework: Dropdown (completed/incomplete) if has student_task
- Summary stats: real-time count
- Submit button: Save & Session Report

**4. Makeup Request Flow (Student):**
- Step 1: Select missed session
- Step 2: System shows available makeup sessions (prioritized list)
- Step 3: Select preferred makeup session
- Step 4: Fill reason, submit
- Confirmation: Request pending approval

**5. Request Management (Academic Affair):**
- Filters: Pending, type (absence/makeup/transfer/leave/reschedule)
- List view: Student/Teacher, type, submitted_at, priority
- Detail view: Full context (original session, target session, stats)
- Actions: Approve/Reject with notes

---

### 4.2 User Flow

#### Request Management Workflow (2 Luồng)

**Request Status Flow:**
```
Luồng 1 (Student/Teacher tự tạo):
Student/Teacher → Create Request → Submit → [pending] → Academic Affair Review → Approve/Reject → [approved/rejected]

Luồng 2 (Academic Affair tạo thay mặt):
Academic Affair → Create Request on behalf → Submit → [waiting_confirm] → Student/Teacher Confirm → [pending] → Academic Affair Approve → [approved]
```

**Request Types & Handlers:**
- **Student Requests**: absence, makeup, transfer (cả 2 luồng)
- **Teacher Requests**: swap, reschedule, modality_change (cả 2 luồng)

---

**User Flow 1: Class Creation & Enrollment** (xem `class-creation.md`, `student-enrollment.md`)

```
Academic Affair:
1. Tạo class từ approved course
2. Hệ thống tự động sinh sessions
3. Assign time slots, resources, teachers
4. Submit for approval
5. Center Head approve
6. Enroll students (select/import)
7. Hệ thống tự động sinh student_sessions
8. Students nhận email welcome
```

**User Flow 2: Attendance Recording** (xem `attendance.md`)

```
Teacher:
1. Login → View today's classes
2. Select class → View today's sessions
3. Select session → View student list
4. Mark attendance + homework
5. Save → Submit session report
6. System confirms → Notify students (optional)
```

**User Flow 3: Makeup Request** (xem `makeup-request.md`)

**Luồng 1: Student tự tạo (Primary)**
```
Student:
1. Login → My Requests → Create Request
2. Select "Makeup" → Select missed session (attendance = "absent")
3. System finds available makeup sessions (same course_session_id, prioritized)
4. Select preferred makeup session → Fill reason
5. Submit → status = "pending"
6. Academic Affair reviews → Approve
7. System executes: update target session (note), create new student_session (is_makeup=true)
8. Student nhận email confirmation
9. Teacher sees student in makeup session với badge "Makeup Student"
```

**Luồng 2: Academic Affair tạo thay mặt (Alternative)**
```
Academic Affair:
1. Nhận request từ Student (WhatsApp/phone)
2. Tìm makeup sessions phù hợp
3. Tạo request trong hệ thống → status = "waiting_confirm"
4. Student confirm → status = "pending"
5. Academic Affair approve → execute
6. Notifications sent
```

**User Flow 4: Teacher Absence Handling** (xem `teacher-reschedule.md`)

**Priority Flow (Teacher có trách nhiệm tìm solution):**

**Option 1: Swap Request (Best)**
```
Teacher:
1. Liên hệ colleague tìm substitute (ngoài hệ thống)
2. Colleague đồng ý → Teacher/Academic Affair tạo swap request
3. Request type = "swap", replacement_teacher_id
4. Substitute confirm → status = "waiting_confirm" → "pending"
5. Academic Affair approve → update teaching_slot.teacher_id, status = "substituted"
```

**Option 2: Reschedule Request (OK)**
```
Teacher:
1. Login → Requests → Create Reschedule Request
2. Select session → Choose new_date, new_time_slot, new_resource
3. Submit → Pending
4. Academic Affair validate (conflicts) → Approve
5. System creates new session (type='teacher_reschedule'), cancels old session
```

**Option 3: Modality Change (Acceptable)**
```
Teacher/Academic Affair:
1. Không dạy offline được → tạo modality_change request
2. Select session → Choose new_resource (zoom)
3. Approve → update session_resource
4. Notify all students về location change
```

**Option 4: Cancel Session (Last Resort)**
```
Academic Affair:
1. Không có solution nào khả thi
2. Update session.status = "cancelled"
3. Mark all students attendance = "absent", note = "Session cancelled"
4. Notify students
```

---

### 4.3 UI/UX Guidelines

#### Design Principles

**1. Clarity First**
- Rõ ràng về status (draft/pending/approved/rejected)
- Hiển thị validation errors ngay lập tức
- Confirmation dialogs cho critical actions

**2. Efficiency**
- Minimize số bước để complete task
- Auto-save draft để tránh mất dữ liệu
- Keyboard shortcuts cho power users

**3. Feedback**
- Loading states cho async operations
- Success/Error notifications rõ ràng
- Progress indicators cho multi-step flows

**4. Consistency**
- Consistent color scheme cho statuses (green=success, red=error, yellow=warning, blue=info)
- Consistent icons cho actions (edit, delete, view, approve)
- Consistent layout cho list/detail views

#### Interaction Patterns

**1. Conflict Detection**
- Real-time highlighting conflicts (resource, teacher, student)
- Suggest alternatives (available resources, teachers)
- Allow override với justification

**2. Request Approval**
- One-click approve/reject từ list view
- Bulk actions cho pending requests
- Filter/Sort để prioritize urgent requests

**3. Search & Filter**
- Autocomplete cho student/teacher search
- Multi-select filters (branch, status, date range)
- Save filter presets

---

## 5. CÔNG NGHỆ VÀ KỸ THUẬT

### 5.1 Tech Stack đề xuất

**Backend:**
- **Framework**: Spring Boot 3.x (Java 17+)
- **Database**: PostgreSQL 16
- **ORM**: JPA/Hibernate
- **Security**: Spring Security + JWT
- **Validation**: Hibernate Validator
- **Build**: Maven

**Frontend (Optional - nếu cần web UI):**
- **Framework**: React 18+ hoặc Vue 3+
- **State Management**: Redux Toolkit / Pinia
- **UI Library**: Material-UI / Ant Design
- **HTTP Client**: Axios

**Infrastructure:**
- **Container**: Docker
- **Orchestration**: Kubernetes (optional, for scale)
- **Reverse Proxy**: Nginx
- **Caching**: Redis (for session, frequently accessed data)
- **Message Queue**: RabbitMQ / Kafka (for async tasks như email)

**DevOps:**
- **CI/CD**: GitHub Actions / GitLab CI
- **Monitoring**: Prometheus + Grafana / Datadog
- **Logging**: ELK Stack (Elasticsearch, Logstash, Kibana)
- **APM**: New Relic / AppDynamics

---

### 5.2 API Requirements

**API Design Principles:**
- RESTful architecture
- JSON payload
- Versioning: `/api/v1/...`
- Pagination: `?page=0&size=20`
- Filtering: `?status=pending&type=makeup`
- Sorting: `?sort=submittedAt,desc`

**Key API Endpoints Examples:**

**Curriculum Management:**
```
POST   /api/v1/subjects
GET    /api/v1/subjects?status=active
POST   /api/v1/subjects/{subjectId}/levels
POST   /api/v1/courses
PUT    /api/v1/courses/{courseId}/approve
GET    /api/v1/courses/{courseId}/sessions
```

**Class Management:**
```
POST   /api/v1/classes
GET    /api/v1/classes?branch={branchId}&status=scheduled
POST   /api/v1/classes/{classId}/sessions/generate
POST   /api/v1/classes/{classId}/resources/assign
POST   /api/v1/classes/{classId}/teachers/assign
PUT    /api/v1/classes/{classId}/submit
PUT    /api/v1/classes/{classId}/approve
```

**Enrollment:**
```
POST   /api/v1/enrollments
POST   /api/v1/enrollments/batch
GET    /api/v1/classes/{classId}/students?eligibility=eligible
POST   /api/v1/enrollments/validate-capacity
```

**Attendance:**
```
GET    /api/v1/teachers/{teacherId}/sessions/today
GET    /api/v1/sessions/{sessionId}/students
PUT    /api/v1/sessions/{sessionId}/attendance
POST   /api/v1/sessions/{sessionId}/report
```

**Requests:**
```
POST   /api/v1/student-requests
GET    /api/v1/student-requests?status=pending&type=makeup
PUT    /api/v1/student-requests/{requestId}/approve
POST   /api/v1/teacher-requests
GET    /api/v1/teacher-requests?status=pending&branchId={branchId}
POST   /api/v1/teacher-requests/{requestId}/find-substitute
```

---

### 5.3 Integration Points

**1. Email Service:**
- SMTP integration cho notifications (enrollment confirmation, request approval, schedule changes)
- Template engine (Thymeleaf, Freemarker)

**2. SMS Service (Optional):**
- Twilio / AWS SNS cho urgent notifications
- Backup channel khi email không được check

**3. Video Conferencing:**
- Zoom API integration để tạo/quản lý meetings
- Store meeting_url, meeting_id trong resource table

**4. Payment Gateway (Future):**
- Stripe / VNPay integration cho học phí online

**5. Cloud Storage:**
- AWS S3 / Google Cloud Storage cho course materials
- Store URLs trong course_material table

---

### 5.4 Data Models (High-Level)

**Core Entities:**
- `user_account`: Users (students, teachers, staff)
- `role`, `user_role`, `user_branches`: RBAC
- `center`, `branch`: Organization hierarchy
- `subject`, `level`, `course`, `course_phase`, `course_session`: Curriculum
- `plo`, `clo`, `plo_clo_mapping`, `course_session_clo_mapping`: Learning outcomes
- `class`, `session`: Operations
- `enrollment`, `student_session`: Student enrollment & schedule
- `teaching_slot`: Teacher assignments
- `resource`, `time_slot_template`, `session_resource`: Resources & scheduling
- `teacher_availability`: Teacher regular schedule
- `student_request`, `teacher_request`: Request management
- `assessment`, `course_assessment`, `score`: Grading
- `student_feedback`, `student_feedback_response`, `feedback_question`, `qa_report`: Quality assurance
- `replacement_skill_assessment`: Student placement testing & skill assessment

**Key Schema Features:**
- **Dual Status Fields**: `course` và `class` có `status` (lifecycle) và `approval_status` (workflow) tách biệt
- **Material Categorization**: `course_material.material_type` (video/pdf/slide/audio/document/other)
- **Mapping Control**: Tất cả mappings (PLO-CLO, Session-CLO, Assessment-CLO) có `status` field (active/inactive)
- **Resource Management**: Unique `code`, `capacity_override` policy, Zoom credentials (url, passcode, account)
- **Bidirectional Makeup Tracking**: `student_session` có `makeup_session_id` và `original_session_id` để trace relationships
- **Structured Feedback**: Template-based feedback system (`feedback_question` → `student_feedback_response`)
- **Request Confirmation**: `request_status` có "waiting_confirm" cho luồng Academic Affair tạo thay mặt
- **Teacher Contract**: `teacher.contract_type` (full-time/part-time/internship) cho HR management
- **Branch Details**: `branch.email`, `district`, `city` cho geographic management

**Key Relationships:**
- `course` → nhiều `course_phase` → nhiều `course_session` (1:N:N)
- `class` → nhiều `session` (1:N)
- `session` → nhiều `student_session` (1:N)
- `session` → nhiều `teaching_slot` (1:N)
- `session` → nhiều `session_resource` (1:N)

**Enum Types:**
- `session_status`: planned, cancelled, done
- `session_type`: class, teacher_reschedule
- `attendance_status`: planned, present, absent
- `enrollment_status`: enrolled, transferred, dropped, completed
- `request_status`: pending, waiting_confirm, approved, rejected
- `teacher_request_type`: swap, reschedule, modality_change
- `student_request_type`: absence, makeup, transfer
- `modality`: offline, online, hybrid
- `skill`: general, reading, writing, speaking, listening
- `teaching_slot_status`: scheduled, on_leave, substituted
- `class_status`: draft, scheduled, ongoing, completed, cancelled
- `subject_status`: draft, active, inactive
- `course_status`: draft, active, inactive
- `approval_status`: pending, approved, rejected
- `material_type`: video, pdf, slide, audio, document, other
- `mapping_status`: active, inactive
- `assessment_kind`: quiz, midterm, final, assignment, project, oral, practice, other
- `homework_status`: completed, incomplete, no_homework

---

### 5.5 Third-party Services

**1. Authentication:**
- OAuth2 / OpenID Connect (optional, for SSO)
- Google/Facebook login (future)

**2. Analytics:**
- Google Analytics cho web traffic
- Mixpanel / Amplitude cho user behavior

**3. Monitoring:**
- Sentry for error tracking
- Datadog / New Relic for APM

**4. Communication:**
- SendGrid / AWS SES for email
- Twilio for SMS
- Firebase Cloud Messaging for push notifications (mobile)

---

## 6. ĐO LƯỜNG VÀ PHÁT HÀNH

### 6.1 Tiêu chí thành công (Success Metrics)

#### KPIs chính

**Operational Efficiency:**
- **Scheduling Time Reduction**: Giảm 80% thời gian lập lịch (từ 3 ngày → < 1 ngày)
  - Baseline: 3 days/week
  - Target: < 0.5 days/week
  - Measurement: Survey Academic Affair

- **Conflict Resolution Rate**: Giảm 90% conflicts do double-booking
  - Baseline: 10-15% classes có conflict
  - Target: < 1% classes có conflict
  - Measurement: System logs (conflict detection events)

**User Satisfaction:**
- **Student Retention**: Giảm 10% dropout rate trong 6 tháng
  - Baseline: 35% dropout
  - Target: < 25% dropout
  - Measurement: Enrollment data

- **Request Turnaround Time**: Giảm 50% thời gian xử lý requests
  - Baseline: Average 3-5 days
  - Target: < 1.5 days
  - Measurement: `student_request.submitted_at` vs `decided_at`

- **App Store Rating**: Tăng rating từ 4.2 → 4.5
  - Measurement: App Store / Play Store reviews

**Resource Utilization:**
- **Room Occupancy Rate**: Tăng 25% utilization
  - Baseline: 60% (nhiều phòng trống)
  - Target: 75%+
  - Measurement: `session_resource` vs `time_slot_template`

- **Teacher Workload Balance**: Giảm 30% variance trong workload distribution
  - Measurement: `teaching_slot` count per teacher

**Quality Assurance:**
- **Attendance Rate**: Tăng average attendance từ 85% → 90%
  - Measurement: `student_session.attendance_status` = 'present' / total

- **CLO Achievement Rate**: Track 100% sessions achieving target CLOs
  - Measurement: `course_session_clo_mapping` vs session completion

---

#### Metrics theo dõi

**System Usage:**
- Daily Active Users (DAU) by role
- Weekly Active Users (WAU)
- Session duration by role
- Feature adoption rate (makeup requests, transfers, OT registrations)

**Operational:**
- Number of classes created per month
- Number of enrollments per month
- Number of requests processed per week
- Average response time for approvals

**Quality:**
- Bug report count per release
- System uptime %
- API response time (p50, p95, p99)
- Error rate (4xx, 5xx)

---

### 6.2 Out of Scope (Phiên bản 1.0)

#### Những gì KHÔNG làm trong phiên bản này

**1. Advanced Features:**
- ❌ Custom themes (multiple color schemes) - chỉ có light/dark mode
- ❌ User-created themes
- ❌ Animated wallpapers/backgrounds
- ❌ In-app chat/messaging (dùng email/external tools)

**2. Payment & Billing:**
- ❌ Online tuition payment
- ❌ Invoice generation
- ❌ Financial reports (revenue, profit)

**3. Mobile Native Apps:**
- ❌ iOS/Android native apps (chỉ responsive web)
- ❌ Offline mode
- ❌ Push notifications (chỉ có email)

**4. Advanced Analytics:**
- ❌ Predictive analytics (churn prediction)
- ❌ AI-powered recommendations (course suggestions)
- ❌ Advanced data visualizations (beyond basic charts)

**5. Third-party Integrations:**
- ❌ LMS integration (Moodle, Canvas)
- ❌ CRM integration (Salesforce)
- ❌ Accounting software integration

**6. Gamification:**
- ❌ Badges/achievements for students
- ❌ Leaderboards
- ❌ Rewards program

---

#### Future Considerations (Phiên bản 2.0+)

**Phase 2 (6-12 tháng sau launch):**
- Push notifications qua Firebase
- Mobile native apps (iOS/Android)
- Advanced analytics dashboard
- Payment integration (VNPay, Stripe)

**Phase 3 (12-18 tháng):**
- AI-powered recommendations
- Predictive analytics (churn prediction)
- LMS integration
- Gamification features

---

### 6.3 Giả định và Rủi ro (Assumptions & Risks)

#### Giả định (Assumptions)

| Giả định | Mô tả |
|----------|-------|
| **A1: Internet Connectivity** | Tất cả users có internet ổn định để access web-based system |
| **A2: User Training** | Trung tâm sẽ đào tạo users (Academic Affair, Teachers) sử dụng hệ thống trong 2 tuần |
| **A3: Data Migration** | Dữ liệu hiện tại (Excel/WhatsApp) có thể migrate vào system |
| **A4: Email Reliability** | Users check email thường xuyên để nhận notifications |
| **A5: Browser Compatibility** | Users sử dụng modern browsers (Chrome, Firefox, Safari, Edge) |

---

#### Rủi ro (Risks)

| Rủi ro | Mức độ | Tác động | Phương án giảm thiểu |
|--------|--------|----------|---------------------|
| **R1: User Resistance to Change** | High | Users từ chối sử dụng system, quay lại Excel/WhatsApp | - Change management program rõ ràng<br>- Training đầy đủ<br>- Support team 24/7 trong tháng đầu<br>- Phần thưởng cho early adopters |
| **R2: Complex Business Logic** | High | Development mất nhiều thời gian hơn dự kiến (conflicts, mappings, requests) | - Iterative development với MVPs từng module<br>- Code reviews nghiêm ngặt<br>- Unit/Integration tests coverage ≥ 80% |
| **R3: Performance Issues** | Medium | System chậm khi scale (500+ classes, 10,000+ students) | - Load testing từ sớm<br>- Database indexing tối ưu<br>- Caching strategy (Redis)<br>- Read replicas cho reporting queries |
| **R4: Data Privacy Compliance** | Medium | Vi phạm GDPR/PDPA nếu không handle sensitive data đúng cách | - Encrypt sensitive data at rest/in transit<br>- GDPR compliance audit<br>- Privacy policy rõ ràng<br>- User consent mechanisms |
| **R5: Third-party Service Downtime** | Low | Email/SMS service down → không gửi notifications được | - Multiple email providers (SendGrid + AWS SES)<br>- Fallback to in-app notifications<br>- Queue retry mechanism |
| **R6: Teacher OT Exploitation** | Low | Teachers đăng ký OT không thực tế để exploit system | - OT approval workflow<br>- Cap OT hours per month<br>- Academic Affair review OT patterns<br>- Audit logs |

---

### 6.4 Timeline & Milestones

#### High-Level Timeline (9 tháng)

**Phase 1: Foundation (Tháng 1-3)**
- **Milestone 1.1** (Week 1-2): Project setup, tech stack finalization, database schema design
- **Milestone 1.2** (Week 3-6): Curriculum Management module (Subject → Course → CLOs → Phases → Sessions)
- **Milestone 1.3** (Week 7-10): User Management & RBAC (Roles, Permissions, Authentication)
- **Milestone 1.4** (Week 11-12): Class Creation & Session Generation

**Phase 2: Core Operations (Tháng 4-6)**
- **Milestone 2.1** (Week 13-16): Resource & Teacher Assignment (với conflict detection)
- **Milestone 2.2** (Week 17-20): Student Enrollment & Student Session Auto-generation
- **Milestone 2.3** (Week 21-24): Attendance Recording & Session Reporting

**Phase 3: Request Management (Tháng 7-8)**
- **Milestone 3.1** (Week 25-28): Student Requests (Absence, Makeup, Transfer)
- **Milestone 3.2** (Week 29-32): Teacher Requests (Leave, OT, Reschedule)

**Phase 4: Reporting & Launch (Tháng 9)**
- **Milestone 4.1** (Week 33-34): Dashboards & Reports (Enrollment, Attendance, Workload, Quality)
- **Milestone 4.2** (Week 35-36): UAT (User Acceptance Testing), Bug fixes, Performance tuning
- **Milestone 4.3** (Week 37-38): Production deployment, Data migration, Go-live
- **Milestone 4.4** (Week 39-40): Post-launch support, Training, Documentation

---

#### Dependencies

**External Dependencies:**
- **Design Team**: Wireframes & Mockups (Week 1-4)
- **Content Team**: Training materials, User guides (Week 33-36)
- **Infrastructure Team**: Production environment setup (Week 32)

**Internal Dependencies:**
- Curriculum Management phải hoàn thành trước Class Creation
- Class Creation phải hoàn thành trước Student Enrollment
- Attendance Recording phụ thuộc vào Student Enrollment

---

### 6.5 Kế hoạch phát hành (Rollout Plan)

#### Phương pháp triển khai

**Phased Rollout (3 phases):**

**Phase 1: Pilot (2 tuần đầu)**
- **Scope**: 1 chi nhánh, 5-10 classes, 100-200 students
- **Objective**: Validate core flows, identify critical bugs
- **Users**: Academic Affair chủ chốt, Teachers tham gia training program
- **Success Criteria**: 
  - Zero critical bugs
  - 80% user satisfaction
  - All core flows hoạt động ổn định

**Phase 2: Soft Launch (4 tuần tiếp theo)**
- **Scope**: 3-5 chi nhánh, 50+ classes, 1,000+ students
- **Objective**: Scale testing, performance validation
- **Users**: Toàn bộ Academic Affair, 50% Teachers, Students enrolled in new classes
- **Success Criteria**:
  - API response time < 500ms (p95)
  - Uptime ≥ 99%
  - User satisfaction ≥ 75%

**Phase 3: Full Rollout (Tuần 7 onwards)**
- **Scope**: Tất cả chi nhánh, tất cả classes
- **Objective**: Complete migration
- **Users**: Tất cả users
- **Success Criteria**:
  - 100% classes migrated
  - Excel/WhatsApp usage dropped to < 10%

---

#### Beta Testing Plan

**Beta Testers:**
- 10 Academic Affair (2/branch)
- 20 Teachers (4/branch)
- 50 Students (10/branch)

**Testing Focus Areas:**
- Class creation & approval workflow
- Enrollment process (manual, import CSV)
- Attendance recording & session reporting
- Request submission & approval (absence, makeup, leave)
- Dashboard & reports

**Feedback Collection:**
- Weekly surveys (Google Forms)
- Bug reporting (Jira/GitHub Issues)
- Focus group sessions (bi-weekly)

---

#### Monitoring & Feedback Collection

**Production Monitoring:**
- **System Health**: Uptime, API response time, error rate (via Datadog/New Relic)
- **User Activity**: DAU/WAU, feature adoption (via Mixpanel/Amplitude)
- **Error Tracking**: Exceptions, stack traces (via Sentry)

**User Feedback:**
- **In-app Feedback**: Widget cho users report bugs/suggestions
- **User Surveys**: Monthly satisfaction surveys (NPS, CSAT)
- **Support Tickets**: Tracking via Zendesk/Freshdesk

**Iterative Improvements:**
- **Weekly Sprint Reviews**: Review feedback, prioritize fixes
- **Monthly Releases**: Bug fixes, minor enhancements
- **Quarterly Major Updates**: New features based on feedback

---

## 7. STAKEHOLDERS & APPROVALS

| Role | Name | Responsibilities | Sign-off |
|------|------|------------------|----------|
| **Product Manager** | [TBD] | Overall product ownership, roadmap, stakeholder communication | [ ] |
| **Engineering Lead** | [TBD] | Technical architecture, implementation feasibility, team management | [ ] |
| **Design Lead** | [TBD] | UX/UI design, wireframes, user testing | [ ] |
| **QA Lead** | [TBD] | Quality assurance, test plans, UAT coordination | [ ] |
| **Business Stakeholder** | [TBD] | Business requirements validation, budget approval | [ ] |
| **Center Director** | [TBD] | Real-world use case validation, pilot testing | [ ] |

---

## 8. PHỤ LỤC

### 8.1 Câu hỏi mở (Open Questions)

**Technical:**
- Q1: Email service provider nào (SendGrid vs AWS SES)?
- Q2: Có cần mobile native apps ngay từ Phase 1 không?
- Q3: Hosting infrastructure: Cloud (AWS, GCP, Azure) hay On-premise?

**Business:**
- Q4: Pricing model: Per-user, per-branch, hay per-student?
- Q5: Support model: 24/7 hay business hours only?
- Q6: Training: Remote hay on-site?

**Product:**
- Q7: Dark mode có phải P0 không? (Dựa trên 68% users sử dụng app vào tối)
- Q8: Attendance lock policy: Lock sau bao nhiêu giờ? Ai có quyền unlock?
- Q9: Request confirmation flow: Student confirm request trong bao lâu trước khi expire? (status = "waiting_confirm")
- Q10: Teacher absence policy: Nếu không tìm được substitute và không reschedule được, tối đa bao nhiêu buổi có thể cancel?
- Q11: Makeup session time limit: Học viên có thể xin học bù cho buổi nghỉ cách đây tối đa bao lâu? (hiện tại: X tuần)

---

### 8.2 Tài liệu tham khảo

#### Research Data

**User Research:**
- Survey 100 Academic Affair (78% dùng Excel + WhatsApp)
- Interview 50 Students (pain points về makeup/transfer)
- Interview 30 Teachers (pain points về OT/reschedule)

**Market Research:**
- Competitor analysis: [TBD - link to document]
- Market size estimation: [TBD]

**Technical Research:**
- PostgreSQL 16 features: [Link to docs]
- Spring Boot 3.x best practices: [Link to guide]

---

#### Related Documents

**Internal:**
- Business Context Summary: `docs/draft/business-context.md`
- Database Schema: `docs/draft/schema.sql`
- API Design Drafts: `docs/draft/*.md`

**External:**
- GDPR Compliance Guide: [Link]
- WCAG 2.1 Standards: [Link]

**Technical Documentation:**
- Spring Boot Reference: https://spring.io/projects/spring-boot
- PostgreSQL Documentation: https://www.postgresql.org/docs/16/
- JWT Best Practices: https://tools.ietf.org/html/rfc8725

---

### 8.3 Change Log

| Date | Version | Changes | Author |
|------|---------|---------|--------|
| 2025-10-31 | 1.0 | PRD finalized based on schema.sql | Product Team |
| | | - Dual request flows: Student/Teacher tự tạo + Academic Affair tạo thay mặt | |
| | | - Request confirmation workflow với status "waiting_confirm" | |
| | | - Teacher absence handling: swap → reschedule → modality_change → cancel (priority order) | |
| | | - Attendance simplified: present/absent (track late/excused via note) | |
| | | - Enrollment có "completed" status để track graduation | |
| | | - Dual status fields: lifecycle status + approval_status (course, class) | |
| | | - Structured feedback system với template questions | |
| | | - Material type categorization (video/pdf/slide/audio/document/other) | |
| | | - Bidirectional makeup tracking (makeup_session_id ↔ original_session_id) | |

---

## 📋 Notes

- **Living Document**: PRD này sẽ được cập nhật liên tục dựa trên feedback từ stakeholders và findings từ development.
- **Change Management**: Mọi thay đổi quan trọng phải được document trong Change Log và notify stakeholders.
- **Version Control**: PRD được quản lý trong Git repository, tất cả changes phải qua pull request và review.
- **Collaboration**: Stakeholders được khuyến khích comment trực tiếp vào document (via Google Docs comments hoặc GitHub Issues).

---

**Approved by:**

- [ ] Product Manager: ________________ Date: ______
- [ ] Engineering Lead: ________________ Date: ______
- [ ] Design Lead: ________________ Date: ______
- [ ] Business Stakeholder: ________________ Date: ______

---

**Document Control:**
- **Location**: `docs/PRD-TMS-System.md`
- **Last Updated**: 2025-10-29
- **Review Cycle**: Monthly
- **Next Review**: 2025-11-29

---

*End of Document*

