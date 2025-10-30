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
  - **Acceptance Criteria**: Xem danh sách students, mark present/absent/late/excused, save attendance

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

- **US-REQ-TEA-001**: Là một Teacher, tôi muốn xin nghỉ một buổi học, để giải quyết việc cá nhân.
  - **Acceptance Criteria**: Chọn session, nhập lý do, Academic Affair phải tìm substitute hoặc reschedule trước khi approve

- **US-REQ-TEA-002**: Là một Teacher, tôi muốn đăng ký OT để có thêm thu nhập, để dạy thay khi giáo viên khác nghỉ.
  - **Acceptance Criteria**: Đăng ký availability override cho ngày/giờ cụ thể, khi được assign → auto create OT request

- **US-REQ-TEA-003**: Là một Teacher, tôi muốn đổi lịch buổi học (trong 7 ngày tới), để phù hợp với lịch cá nhân.
  - **Acceptance Criteria**: Chọn session, chọn new date/time/resource, submit, Academic Affair approve → create new session với type="teacher_reschedule"

**Epic 7: Báo cáo và Dashboard (Reporting & Analytics)**

- **US-RPT-001**: Là một Manager, tôi muốn xem dashboard KPI (enrollment, attendance, workload), để đưa ra quyết định.
  - **Acceptance Criteria**: Dashboard hiển thị enrollment rate, attendance rate, teacher workload, room utilization

---

#### Should-Have (P1 - Important but not blockers)

- **US-QA-001**: Là một QA, tôi muốn tạo QA reports cho classes/sessions, để theo dõi chất lượng.
- **US-ASS-001**: Là một Teacher, tôi muốn nhập điểm cho assessments, để đánh giá học viên.
- **US-FB-001**: Là một Student, tôi muốn đánh giá buổi học (rating + comment), để cải thiện chất lượng.
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
- Upload materials cho Course/Phase/Session (title, url, uploaded_by)

**FR-1.10: Course Approval Workflow**
- Subject Leader submit course → Manager review → Approve/Reject
- Khi approve: status = "active", course có thể dùng để tạo class

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
- Khi approve: status = "scheduled", có thể enroll students

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
- Select students (multi-select)
- Capacity validation: enrolled_count + selected_count ≤ max_capacity
- Schedule conflict check: students không học 2 classes cùng lúc
- Capacity override với lý do (nếu vượt)

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
- Mark attendance: present/absent/late/excused/remote
- Mark homework: completed/incomplete/no_homework (nếu có student_task)
- Real-time summary: present_count, absent_count, homework_completed_count

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
- Student chọn session cần xin nghỉ (date >= TODAY)
- Chọn lớp → session → nhập lý do
- Validation: session.status = "planned", không duplicate request
- Academic Affair approve → update student_session.attendance_status = "excused"

**FR-5.2: Makeup Request**
- **Case 1: Học bù cho buổi đã nghỉ**
  - Load missed sessions (attendance_status = "absent"/"late", trong X tuần gần nhất)
  - Tìm makeup sessions: same course_session_id, status = "planned", date >= CURRENT_DATE, còn chỗ
  - Prioritize: same branch → same modality → soonest date → most available slots
  
- **Case 2: Đăng ký học bù trước cho buổi tương lai**
  - Chọn future session sẽ nghỉ → tìm makeup sessions (tương tự Case 1)
  
- Validation: course_session_id phải match, capacity available, không duplicate
- Academic Affair approve → update target session = "excused", create new student_session (is_makeup = TRUE)

**FR-5.3: Transfer Request (Class Transfer)**
- Student chọn current_class → target_class (same course_id) → effective_date
- Validation:
  - Both classes same course_id
  - Target class status = "scheduled"/"ongoing"
  - Target class có capacity
  - Check content gap (course_session_id mapping)
  
- Academic Affair approve → transaction:
  - Update current enrollment: status = "transferred", left_at, left_session_id
  - Create new enrollment: status = "enrolled", enrolled_at, join_session_id
  - Update future sessions in current class: attendance_status = "excused"
  - Generate student_sessions for future sessions in target class

---

#### FR-6: Yêu cầu Giáo viên (Teacher Request Management Module)

**FR-6.1: Leave Request**
- Teacher chọn session cần xin nghỉ (trong 7 ngày tới) → nhập lý do
- Academic Affair phải tìm solution trước khi approve:
  - **Option A: Find Substitute**
    - Search teachers: skill match, availability (OT registrations first), no conflict
    - Academic Affair contact candidate → chọn substitute
    - Approve → update teaching_slot.teacher_id, create OT request for substitute
  - **Option B: Reschedule Session**
    - Chọn new date/time → validate resource + teacher availability
    - Create new session, transfer student_sessions/teaching_slots, cancel old session
  - **Option C: Cancel Session**
    - Update session.status = "cancelled", mark all students "excused"

**FR-6.2: OT Registration**
- Teacher đăng ký availability override (date, start_time, end_time)
- Khi được assign vào session → auto create OT request (for payroll tracking)

**FR-6.3: Reschedule Request**
- Teacher chọn session (trong 7 ngày tới) → chọn new date/time/resource
- Validation: resource available, no student conflicts
- Academic Affair approve → create new session với type = "teacher_reschedule", cancel old session
- Track: teacher_request.session_id (old), teacher_request.new_session_id (new)

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

```
Student:
1. Login → My Requests → Create Request
2. Select "Makeup" → Select missed session
3. System finds available makeup sessions (prioritized)
4. Select preferred makeup session → Fill reason
5. Submit → Pending approval
6. Academic Affair reviews → Approve
7. System updates: target session = "excused", create new student_session (is_makeup=true)
8. Student nhận email confirmation
9. Teacher sees student in makeup session với badge "Makeup Student"
```

**User Flow 4: Teacher Leave Request** (xem `teacher-reschedule.md` + business-context)

```
Teacher:
1. Login → Requests → Create Request
2. Select "Leave" → Select session (trong 7 ngày tới)
3. Fill reason → Submit → Pending
4. Academic Affair reviews → Find solution:
   - Option A: Find substitute (from OT teachers)
   - Option B: Reschedule session
   - Option C: Cancel session (last resort)
5. Academic Affair execute solution → Approve request
6. System updates: teaching_slot, session, notifications
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
- `teacher_availability`: Teacher regular schedule & OT registrations
- `student_request`, `teacher_request`: Request management
- `assessment`, `course_assessment`, `score`: Grading
- `student_feedback`, `qa_report`: Quality assurance

**Key Relationships:**
- `course` → nhiều `course_phase` → nhiều `course_session` (1:N:N)
- `class` → nhiều `session` (1:N)
- `session` → nhiều `student_session` (1:N)
- `session` → nhiều `teaching_slot` (1:N)
- `session` → nhiều `session_resource` (1:N)

**Enum Types:**
- `session_status`: planned, cancelled, done
- `attendance_status`: planned, present, absent, late, excused, remote
- `enrollment_status`: enrolled, waitlisted, transferred, dropped, completed
- `request_status`: pending, waiting_confirm, approved, rejected, cancelled
- `modality`: offline, online, hybrid
- `skill`: general, reading, writing, speaking, listening

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
- Q8: Capacity override policy: Ai có quyền override? Có limit không?
- Q9: Attendance lock policy: Lock sau bao nhiêu giờ? Ai có quyền unlock?

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
| 2025-10-29 | 1.0 | Initial PRD draft created | Product Team |
| | | - Extracted from business-context.md, schema.sql, and feature drafts | |
| | | - Structured according to PRD template | |

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

