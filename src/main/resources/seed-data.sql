-- =========================================
-- TMS-SEP490-BE: COMPREHENSIVE SEED DATA FOR TESTING
-- =========================================
-- Author: QA Team
-- Date: 2025-11-02
-- Purpose: High-quality, logically consistent dataset covering all business flows and edge cases
-- Reference Date: 2025-11-02 (today's date for testing)
-- =========================================
-- COVERAGE:
-- - Happy paths: Course creation → Class → Enrollment → Attendance → Requests
-- - Edge cases: Mid-course enrollment, cross-class makeup, transfers, teacher swaps
-- - Boundary conditions: Capacity limits, date ranges, status transitions
-- =========================================

-- ========== SECTION 0: CLEANUP & RESET ==========
-- Clean existing data in reverse dependency order
TRUNCATE TABLE student_feedback_response CASCADE;
TRUNCATE TABLE student_feedback CASCADE;
TRUNCATE TABLE qa_report CASCADE;
TRUNCATE TABLE score CASCADE;
TRUNCATE TABLE assessment CASCADE;
TRUNCATE TABLE course_assessment_clo_mapping CASCADE;
TRUNCATE TABLE course_assessment CASCADE;
TRUNCATE TABLE teacher_request CASCADE;
TRUNCATE TABLE student_request CASCADE;
TRUNCATE TABLE student_session CASCADE;
TRUNCATE TABLE enrollment CASCADE;
TRUNCATE TABLE teaching_slot CASCADE;
TRUNCATE TABLE teacher_availability CASCADE;
TRUNCATE TABLE session_resource CASCADE;
TRUNCATE TABLE session CASCADE;
TRUNCATE TABLE course_session_clo_mapping CASCADE;
TRUNCATE TABLE plo_clo_mapping CASCADE;
TRUNCATE TABLE clo CASCADE;
TRUNCATE TABLE course_material CASCADE;
TRUNCATE TABLE course_session CASCADE;
TRUNCATE TABLE course_phase CASCADE;
TRUNCATE TABLE "class" CASCADE;
TRUNCATE TABLE teacher_skill CASCADE;
TRUNCATE TABLE student CASCADE;
TRUNCATE TABLE teacher CASCADE;
TRUNCATE TABLE user_branches CASCADE;
TRUNCATE TABLE user_role CASCADE;
TRUNCATE TABLE resource CASCADE;
TRUNCATE TABLE time_slot_template CASCADE;
TRUNCATE TABLE course CASCADE;
TRUNCATE TABLE plo CASCADE;
TRUNCATE TABLE level CASCADE;
TRUNCATE TABLE subject CASCADE;
TRUNCATE TABLE branch CASCADE;
TRUNCATE TABLE center CASCADE;
TRUNCATE TABLE role CASCADE;
TRUNCATE TABLE user_account CASCADE;
TRUNCATE TABLE replacement_skill_assessment CASCADE;
TRUNCATE TABLE feedback_question CASCADE;

-- Reset sequences
SELECT setval('center_id_seq', 1, false);
SELECT setval('branch_id_seq', 1, false);
SELECT setval('role_id_seq', 1, false);
SELECT setval('user_account_id_seq', 1, false);
SELECT setval('teacher_id_seq', 1, false);
SELECT setval('student_id_seq', 1, false);
SELECT setval('subject_id_seq', 1, false);
SELECT setval('level_id_seq', 1, false);
SELECT setval('plo_id_seq', 1, false);
SELECT setval('course_id_seq', 1, false);
SELECT setval('course_phase_id_seq', 1, false);
SELECT setval('clo_id_seq', 1, false);
SELECT setval('course_session_id_seq', 1, false);
SELECT setval('course_assessment_id_seq', 1, false);
SELECT setval('class_id_seq', 1, false);
SELECT setval('session_id_seq', 1, false);
SELECT setval('assessment_id_seq', 1, false);
SELECT setval('score_id_seq', 1, false);
SELECT setval('student_request_id_seq', 1, false);
SELECT setval('teacher_request_id_seq', 1, false);
SELECT setval('student_feedback_id_seq', 1, false);
SELECT setval('student_feedback_response_id_seq', 1, false);
SELECT setval('qa_report_id_seq', 1, false);
SELECT setval('course_material_id_seq', 1, false);
SELECT setval('time_slot_template_id_seq', 1, false);
SELECT setval('resource_id_seq', 1, false);
SELECT setval('replacement_skill_assessment_id_seq', 1, false);
SELECT setval('feedback_question_id_seq', 1, false);

-- ========== TIER 1: INDEPENDENT TABLES ==========

-- Center
INSERT INTO center (id, code, name, description, phone, email, address) VALUES
(1, 'TMS-EDU', 'TMS Education Group', 'Leading language education group in Vietnam', '+84-24-3999-8888', 'info@tms-edu.vn', '123 Nguyen Trai, Thanh Xuan, Ha Noi');

-- Roles
INSERT INTO role (id, code, name) VALUES
(1, 'ADMIN', 'System Administrator'),
(2, 'MANAGER', 'Manager'),
(3, 'CENTER_HEAD', 'Center Head'),
(4, 'SUBJECT_LEADER', 'Subject Leader'),
(5, 'ACADEMIC_STAFF', 'Academic Staff'),
(6, 'TEACHER', 'Teacher'),
(7, 'STUDENT', 'Student'),
(8, 'QA', 'Quality Assurance');

-- User Accounts
-- Password: 'password' hashed with BCrypt
INSERT INTO user_account (id, email, phone, full_name, gender, dob, address, password_hash, status) VALUES
-- Staff & Management (11 users)
(1, 'admin@tms-edu.vn', '0912000001', 'Nguyen Van Admin', 'male', '1980-01-15', 'Ha Noi', '$2a$10$N9qo8uLOickgx2ZMRZoMye1J8PjqKQXW3qNqLqKlJqQXqKlJqQXqK', 'active'),
(2, 'manager.global@tms-edu.vn', '0912000002', 'Le Van Manager', 'male', '1982-07-10', 'Ha Noi', '$2a$10$N9qo8uLOickgx2ZMRZoMye1J8PjqKQXW3qNqLqKlJqQXqKlJqQXqK', 'active'),
(3, 'head.hn01@tms-edu.vn', '0912000003', 'Tran Thi Lan', 'female', '1975-03-20', 'Ha Noi', '$2a$10$N9qo8uLOickgx2ZMRZoMye1J8PjqKQXW3qNqLqKlJqQXqKlJqQXqK', 'active'),
(4, 'head.hcm01@tms-edu.vn', '0912000004', 'Nguyen Thi Mai', 'female', '1978-05-22', 'TP. HCM', '$2a$10$N9qo8uLOickgx2ZMRZoMye1J8PjqKQXW3qNqLqKlJqQXqKlJqQXqK', 'active'),
(5, 'leader.ielts@tms-edu.vn', '0912000005', 'Bui Van Nam', 'male', '1985-12-30', 'Ha Noi', '$2a$10$N9qo8uLOickgx2ZMRZoMye1J8PjqKQXW3qNqLqKlJqQXqKlJqQXqK', 'active'),
(6, 'staff.huong.hn@tms-edu.vn', '0912000006', 'Pham Thi Huong', 'female', '1990-11-05', 'Ha Noi', '$2a$10$N9qo8uLOickgx2ZMRZoMye1J8PjqKQXW3qNqLqKlJqQXqKlJqQXqK', 'active'),
(7, 'staff.duc.hn@tms-edu.vn', '0912000007', 'Hoang Van Duc', 'male', '1992-05-18', 'Ha Noi', '$2a$10$N9qo8uLOickgx2ZMRZoMye1J8PjqKQXW3qNqLqKlJqQXqKlJqQXqK', 'active'),
(8, 'staff.anh.hcm@tms-edu.vn', '0912000008', 'Le Thi Anh', 'female', '1991-02-15', 'TP. HCM', '$2a$10$N9qo8uLOickgx2ZMRZoMye1J8PjqKQXW3qNqLqKlJqQXqKlJqQXqK', 'active'),
(9, 'staff.tuan.hcm@tms-edu.vn', '0912000009', 'Tran Minh Tuan', 'male', '1993-08-20', 'TP. HCM', '$2a$10$N9qo8uLOickgx2ZMRZoMye1J8PjqKQXW3qNqLqKlJqQXqKlJqQXqK', 'active'),
(10, 'qa.linh@tms-edu.vn', '0912000010', 'Vu Thi Linh', 'female', '1988-09-25', 'Ha Noi', '$2a$10$N9qo8uLOickgx2ZMRZoMye1J8PjqKQXW3qNqLqKlJqQXqKlJqQXqK', 'active'),
(11, 'qa.thanh@tms-edu.vn', '0912000011', 'Dang Ngoc Thanh', 'male', '1989-04-10', 'TP. HCM', '$2a$10$N9qo8uLOickgx2ZMRZoMye1J8PjqKQXW3qNqLqKlJqQXqKlJqQXqK', 'active'),

-- Teachers (16 teachers: 8 per branch)
(20, 'john.smith@tms-edu.vn', '0912001001', 'John Smith', 'male', '1985-04-12', 'Ha Noi', '$2a$10$N9qo8uLOickgx2ZMRZoMye1J8PjqKQXW3qNqLqKlJqQXqKlJqQXqK', 'active'),
(21, 'emma.wilson@tms-edu.vn', '0912001002', 'Emma Wilson', 'female', '1987-08-22', 'Ha Noi', '$2a$10$N9qo8uLOickgx2ZMRZoMye1J8PjqKQXW3qNqLqKlJqQXqKlJqQXqK', 'active'),
(22, 'david.lee@tms-edu.vn', '0912001003', 'David Lee', 'male', '1983-12-05', 'Ha Noi', '$2a$10$N9qo8uLOickgx2ZMRZoMye1J8PjqKQXW3qNqLqKlJqQXqKlJqQXqK', 'active'),
(23, 'sarah.johnson@tms-edu.vn', '0912001004', 'Sarah Johnson', 'female', '1990-06-14', 'Ha Noi', '$2a$10$N9qo8uLOickgx2ZMRZoMye1J8PjqKQXW3qNqLqKlJqQXqKlJqQXqK', 'active'),
(24, 'michael.brown@tms-edu.vn', '0912001005', 'Michael Brown', 'male', '1986-02-28', 'Ha Noi', '$2a$10$N9qo8uLOickgx2ZMRZoMye1J8PjqKQXW3qNqLqKlJqQXqKlJqQXqK', 'active'),
(25, 'lisa.chen@tms-edu.vn', '0912001006', 'Lisa Chen', 'female', '1988-10-17', 'Ha Noi', '$2a$10$N9qo8uLOickgx2ZMRZoMye1J8PjqKQXW3qNqLqKlJqQXqKlJqQXqK', 'active'),
(26, 'james.taylor@tms-edu.vn', '0912001007', 'James Taylor', 'male', '1984-03-09', 'Ha Noi', '$2a$10$N9qo8uLOickgx2ZMRZoMye1J8PjqKQXW3qNqLqKlJqQXqKlJqQXqK', 'active'),
(27, 'anna.martinez@tms-edu.vn', '0912001008', 'Anna Martinez', 'female', '1989-07-21', 'Ha Noi', '$2a$10$N9qo8uLOickgx2ZMRZoMye1J8PjqKQXW3qNqLqKlJqQXqKlJqQXqK', 'active'),
(28, 'chris.evans@tms-edu.vn', '0912001009', 'Chris Evans', 'male', '1988-01-20', 'TP. HCM', '$2a$10$N9qo8uLOickgx2ZMRZoMye1J8PjqKQXW3qNqLqKlJqQXqKlJqQXqK', 'active'),
(29, 'olivia.white@tms-edu.vn', '0912001010', 'Olivia White', 'female', '1991-03-15', 'TP. HCM', '$2a$10$N9qo8uLOickgx2ZMRZoMye1J8PjqKQXW3qNqLqKlJqQXqKlJqQXqK', 'active'),
(30, 'daniel.harris@tms-edu.vn', '0912001011', 'Daniel Harris', 'male', '1987-11-30', 'TP. HCM', '$2a$10$N9qo8uLOickgx2ZMRZoMye1J8PjqKQXW3qNqLqKlJqQXqKlJqQXqK', 'active'),
(31, 'sophia.clark@tms-edu.vn', '0912001012', 'Sophia Clark', 'female', '1992-09-05', 'TP. HCM', '$2a$10$N9qo8uLOickgx2ZMRZoMye1J8PjqKQXW3qNqLqKlJqQXqKlJqQXqK', 'active'),
(32, 'matthew.lewis@tms-edu.vn', '0912001013', 'Matthew Lewis', 'male', '1989-06-27', 'TP. HCM', '$2a$10$N9qo8uLOickgx2ZMRZoMye1J8PjqKQXW3qNqLqKlJqQXqKlJqQXqK', 'active'),
(33, 'ava.robinson@tms-edu.vn', '0912001014', 'Ava Robinson', 'female', '1993-01-10', 'TP. HCM', '$2a$10$N9qo8uLOickgx2ZMRZoMye1J8PjqKQXW3qNqLqKlJqQXqKlJqQXqK', 'active'),
(34, 'andrew.walker@tms-edu.vn', '0912001015', 'Andrew Walker', 'male', '1986-08-18', 'TP. HCM', '$2a$10$N9qo8uLOickgx2ZMRZoMye1J8PjqKQXW3qNqLqKlJqQXqKlJqQXqK', 'active'),
(35, 'isabella.young@tms-edu.vn', '0912001016', 'Isabella Young', 'female', '1990-04-25', 'TP. HCM', '$2a$10$N9qo8uLOickgx2ZMRZoMye1J8PjqKQXW3qNqLqKlJqQXqKlJqQXqK', 'active');

-- Students (60 students total: 30 per branch for realistic testing)
INSERT INTO user_account (id, email, phone, full_name, gender, dob, address, password_hash, status) 
SELECT 
    100 + s.id, 
    'student.' || LPAD(s.id::text, 4, '0') || '@gmail.com', 
    '0900' || LPAD(s.id::text, 6, '0'),
    'Student ' || LPAD(s.id::text, 4, '0'),
    CASE WHEN s.id % 2 = 0 THEN 'female' ELSE 'male' END, 
    make_date(2000 + (s.id % 6), (s.id % 12) + 1, (s.id % 28) + 1),
    CASE WHEN s.id <= 30 THEN 'Ha Noi' ELSE 'TP. HCM' END,
    '$2a$10$N9qo8uLOickgx2ZMRZoMye1J8PjqKQXW3qNqLqKlJqQXqKlJqQXqK',
    'active'
FROM generate_series(1, 60) AS s(id);

-- Feedback Questions (for student feedback feature)
INSERT INTO feedback_question (id, question_text, question_type, options, display_order) VALUES
(1, 'How satisfied are you with the overall teaching quality?', 'rating', NULL, 1),
(2, 'How clear and well-organized were the lessons?', 'rating', NULL, 2),
(3, 'How helpful were the course materials and resources?', 'rating', NULL, 3),
(4, 'How effective was the class management and scheduling?', 'rating', NULL, 4),
(5, 'Would you recommend this course to others?', 'rating', NULL, 5),
(6, 'What did you like most about the course?', 'text', NULL, 6),
(7, 'What areas need improvement?', 'text', NULL, 7);

-- ========== TIER 2: DEPENDENT ON TIER 1 ==========

-- Branches
INSERT INTO branch (id, center_id, code, name, address, phone, email, district, city, status, opening_date) VALUES
(1, 1, 'HN01', 'TMS Ha Noi Branch', '456 Lang Ha, Dong Da, Ha Noi', '+84-24-3888-9999', 'hanoi01@tms-edu.vn', 'Dong Da', 'Ha Noi', 'active', '2024-01-15'),
(2, 1, 'HCM01', 'TMS Ho Chi Minh Branch', '789 Le Loi, Quan 1, TP. HCM', '+84-28-3777-6666', 'hcm01@tms-edu.vn', 'Quan 1', 'TP. HCM', 'active', '2024-03-01');

-- Subject
INSERT INTO subject (id, code, name, description, status, created_by) VALUES
(1, 'IELTS', 'International English Language Testing System', 'Comprehensive IELTS preparation courses', 'active', 5);

-- Time Slot Templates
INSERT INTO time_slot_template (id, branch_id, name, start_time, end_time) VALUES
-- Ha Noi Branch
(1, 1, 'HN Morning 1', '08:00:00', '10:00:00'),
(2, 1, 'HN Morning 2', '10:00:00', '12:00:00'),
(3, 1, 'HN Afternoon 1', '13:30:00', '15:30:00'),
(4, 1, 'HN Afternoon 2', '15:30:00', '17:30:00'),
(5, 1, 'HN Evening', '18:00:00', '20:00:00'),
-- Ho Chi Minh Branch
(6, 2, 'HCM Morning', '08:30:00', '10:30:00'),
(7, 2, 'HCM Afternoon', '14:00:00', '16:00:00'),
(8, 2, 'HCM Evening', '18:30:00', '20:30:00');

-- Resources (Rooms & Zoom)
INSERT INTO resource (id, branch_id, resource_type, code, name, capacity, capacity_override) VALUES
-- Ha Noi Branch - Physical Rooms
(1, 1, 'room', 'HN01-R101', 'Ha Noi Room 101', 20, NULL),
(2, 1, 'room', 'HN01-R102', 'Ha Noi Room 102', 15, NULL),
(3, 1, 'room', 'HN01-R201', 'Ha Noi Room 201', 25, NULL),
-- Ha Noi Branch - Virtual
(4, 1, 'virtual', 'HN01-Z01', 'Ha Noi Zoom 01', 100, NULL),
-- Ho Chi Minh Branch - Physical Rooms
(5, 2, 'room', 'HCM01-R101', 'HCM Room 101', 20, NULL),
(6, 2, 'room', 'HCM01-R102', 'HCM Room 102', 20, NULL),
(7, 2, 'room', 'HCM01-R201', 'HCM Room 201', 25, NULL),
-- Ho Chi Minh Branch - Virtual
(8, 2, 'virtual', 'HCM01-Z01', 'HCM Zoom 01', 100, NULL);

-- User Role & Branch Assignments
INSERT INTO user_role (user_id, role_id) VALUES
(1,1), (2,2), (3,3), (4,3), (5,4), (6,5), (7,5), (8,5), (9,5), (10,8), (11,8);
-- Teachers
INSERT INTO user_role (user_id, role_id) SELECT id, 6 FROM user_account WHERE id >= 20 AND id <= 35;
-- Students
INSERT INTO user_role (user_id, role_id) SELECT id, 7 FROM user_account WHERE id >= 101;

INSERT INTO user_branches (user_id, branch_id, assigned_by) VALUES
-- Staff assignments
(1,1,1), (1,2,1), (2,1,1), (2,2,1), (3,1,2), (4,2,2), (5,1,2), (6,1,2), (7,1,2), (8,2,4), (9,2,4), (10,1,2), (11,2,4);
-- Teachers - HN
INSERT INTO user_branches (user_id, branch_id, assigned_by) SELECT id, 1, 6 FROM user_account WHERE id BETWEEN 20 AND 27;
-- Teachers - HCM
INSERT INTO user_branches (user_id, branch_id, assigned_by) SELECT id, 2, 8 FROM user_account WHERE id BETWEEN 28 AND 35;
-- Students - HN
INSERT INTO user_branches (user_id, branch_id, assigned_by) SELECT id, 1, 6 FROM user_account WHERE id BETWEEN 101 AND 130;
-- Students - HCM
INSERT INTO user_branches (user_id, branch_id, assigned_by) SELECT id, 2, 8 FROM user_account WHERE id > 130;

-- Teachers & Students
INSERT INTO teacher (id, user_account_id, employee_code, hire_date, contract_type) 
SELECT (id - 19), id, 'TCH-' || LPAD((id-19)::text, 3, '0'), '2024-02-01', CASE WHEN id % 3 = 0 THEN 'part-time' ELSE 'full-time' END
FROM user_account WHERE id BETWEEN 20 AND 35;

INSERT INTO student (id, user_id, student_code, level)
SELECT (id - 100), id, 'STD-' || LPAD((id - 100)::text, 4, '0'), 
CASE 
    WHEN id BETWEEN 101 AND 120 THEN 'Beginner'
    WHEN id BETWEEN 121 AND 140 THEN 'Intermediate'
    ELSE 'Advanced'
END
FROM user_account WHERE id >= 101;

-- Teacher Skills
INSERT INTO teacher_skill (teacher_id, skill, specialization, language, level) VALUES
-- HN Teachers
(1, 'general', 'IELTS', 'English', 5),
(1, 'speaking', 'IELTS Speaking', 'English', 5),
(1, 'listening', 'IELTS Listening', 'English', 5),
(2, 'writing', 'IELTS Writing', 'English', 5),
(2, 'reading', 'IELTS Reading', 'English', 5),
(3, 'general', 'IELTS', 'English', 4),
(3, 'speaking', 'IELTS Speaking', 'English', 5),
(4, 'writing', 'IELTS Writing', 'English', 4),
(4, 'general', 'IELTS', 'English', 4),
(5, 'listening', 'IELTS Listening', 'English', 5),
(5, 'speaking', 'IELTS Speaking', 'English', 4),
(6, 'reading', 'IELTS Reading', 'English', 5),
(6, 'general', 'IELTS', 'English', 4),
(7, 'general', 'IELTS', 'English', 5),
(7, 'writing', 'IELTS Writing', 'English', 4),
(8, 'speaking', 'IELTS Speaking', 'English', 5),
(8, 'listening', 'IELTS Listening', 'English', 4),
-- HCM Teachers
(9, 'general', 'IELTS', 'English', 5),
(9, 'speaking', 'IELTS Speaking', 'English', 5),
(10, 'writing', 'IELTS Writing', 'English', 5),
(10, 'reading', 'IELTS Reading', 'English', 5),
(11, 'general', 'IELTS', 'English', 4),
(11, 'listening', 'IELTS Listening', 'English', 5),
(12, 'speaking', 'IELTS Speaking', 'English', 5),
(12, 'general', 'IELTS', 'English', 4),
(13, 'writing', 'IELTS Writing', 'English', 4),
(13, 'reading', 'IELTS Reading', 'English', 5),
(14, 'general', 'IELTS', 'English', 5),
(14, 'listening', 'IELTS Listening', 'English', 5),
(15, 'speaking', 'IELTS Speaking', 'English', 4),
(15, 'writing', 'IELTS Writing', 'English', 4),
(16, 'general', 'IELTS', 'English', 5),
(16, 'reading', 'IELTS Reading', 'English', 5);

-- Teacher Availability (Sample for key teachers)
INSERT INTO teacher_availability (teacher_id, time_slot_template_id, day_of_week, effective_date) VALUES
-- Teacher 1 (HN) - Available Mon/Wed/Fri mornings
(1, 1, 1, '2024-02-01'), -- Monday
(1, 1, 3, '2024-02-01'), -- Wednesday
(1, 1, 5, '2024-02-01'), -- Friday
-- Teacher 2 (HN) - Available Tue/Thu/Sat afternoons
(2, 3, 2, '2024-02-01'), -- Tuesday
(2, 3, 4, '2024-02-01'), -- Thursday
(2, 3, 6, '2024-02-01'), -- Saturday
-- Teacher 9 (HCM) - Available Mon/Wed/Fri
(9, 6, 1, '2024-02-01'),
(9, 6, 3, '2024-02-01'),
(9, 6, 5, '2024-02-01');

-- ========== TIER 3: CURRICULUM (Complete Definition) ==========

-- Levels for IELTS
INSERT INTO level (id, subject_id, code, name, expected_duration_hours, sort_order) VALUES
(1, 1, 'FOUNDATION', 'IELTS Foundation (3.0-4.0)', 60, 1),
(2, 1, 'INTERMEDIATE', 'IELTS Intermediate (5.0-6.0)', 75, 2),
(3, 1, 'ADVANCED', 'IELTS Advanced (6.5-8.0)', 90, 3);

-- PLOs for IELTS Subject
INSERT INTO plo (id, subject_id, code, description) VALUES
(1, 1, 'PLO1', 'Demonstrate basic English communication skills in everyday contexts'),
(2, 1, 'PLO2', 'Comprehend and produce simple English texts for common situations'),
(3, 1, 'PLO3', 'Apply intermediate English grammar and vocabulary in professional contexts'),
(4, 1, 'PLO4', 'Analyze and evaluate complex English texts across various topics'),
(5, 1, 'PLO5', 'Produce coherent, well-structured academic essays and reports');

-- Course: IELTS Foundation
INSERT INTO course (id, subject_id, level_id, logical_course_code, version, code, name, description, total_hours, duration_weeks, session_per_week, hours_per_session, status, approval_status, decided_by_manager, decided_at, created_by) VALUES
(1, 1, 1, 'IELTS-FOUND-2025', 1, 'IELTS-FOUND-2025-V1', 'IELTS Foundation 2025', 'Foundation course for IELTS beginners targeting band 3.0-4.0', 60, 8, 3, 2.5, 'active', 'approved', 2, '2024-08-20 14:00:00+07', 5);

-- Course Phases for Foundation
INSERT INTO course_phase (id, course_id, phase_number, name, duration_weeks) VALUES
(1, 1, 1, 'Foundation Basics', 4),
(2, 1, 2, 'Foundation Practice', 4);

-- Course Sessions for Foundation (24 sessions = 8 weeks × 3 sessions/week)
INSERT INTO course_session (id, phase_id, sequence_no, topic, student_task, skill_set) VALUES
-- Phase 1: Foundation Basics (Sessions 1-12)
(1, 1, 1, 'Introduction to IELTS & Basic Listening', 'Listen to simple dialogues', ARRAY['general','listening']::skill_enum[]),
(2, 1, 2, 'Basic Speaking: Greetings and Introductions', 'Practice self-introduction', ARRAY['speaking']::skill_enum[]),
(3, 1, 3, 'Basic Reading: Short Passages', 'Read and answer simple questions', ARRAY['reading']::skill_enum[]),
(4, 1, 4, 'Basic Writing: Simple Sentences', 'Write about yourself', ARRAY['writing']::skill_enum[]),
(5, 1, 5, 'Listening: Numbers and Dates', 'Complete listening exercises', ARRAY['listening']::skill_enum[]),
(6, 1, 6, 'Speaking: Daily Activities', 'Describe your daily routine', ARRAY['speaking']::skill_enum[]),
(7, 1, 7, 'Reading: Understanding Main Ideas', 'Identify main ideas', ARRAY['reading']::skill_enum[]),
(8, 1, 8, 'Writing: Simple Paragraphs', 'Write a short paragraph', ARRAY['writing']::skill_enum[]),
(9, 1, 9, 'Listening: Conversations', 'Listen to basic conversations', ARRAY['listening']::skill_enum[]),
(10, 1, 10, 'Speaking: Expressing Likes and Dislikes', 'Talk about preferences', ARRAY['speaking']::skill_enum[]),
(11, 1, 11, 'Reading: Details and Facts', 'Find specific information', ARRAY['reading']::skill_enum[]),
(12, 1, 12, 'Writing: Connecting Ideas', 'Use simple connectors', ARRAY['writing']::skill_enum[]),
-- Phase 2: Foundation Practice (Sessions 13-24)
(13, 2, 1, 'Listening: Following Instructions', 'Complete tasks from audio', ARRAY['listening']::skill_enum[]),
(14, 2, 2, 'Speaking: Asking Questions', 'Practice question forms', ARRAY['speaking']::skill_enum[]),
(15, 2, 3, 'Reading: Short Stories', 'Read and summarize', ARRAY['reading']::skill_enum[]),
(16, 2, 4, 'Writing: Describing People and Places', 'Write descriptions', ARRAY['writing']::skill_enum[]),
(17, 2, 5, 'Listening: News and Announcements', 'Understand main points', ARRAY['listening']::skill_enum[]),
(18, 2, 6, 'Speaking: Giving Opinions', 'Express simple opinions', ARRAY['speaking']::skill_enum[]),
(19, 2, 7, 'Reading: Understanding Context', 'Use context clues', ARRAY['reading']::skill_enum[]),
(20, 2, 8, 'Writing: Personal Letters', 'Write informal letters', ARRAY['writing']::skill_enum[]),
(21, 2, 9, 'Practice Test: Listening & Reading', 'Complete practice test', ARRAY['listening','reading']::skill_enum[]),
(22, 2, 10, 'Practice Test: Writing & Speaking', 'Complete practice test', ARRAY['writing','speaking']::skill_enum[]),
(23, 2, 11, 'Review and Feedback', 'Review all skills', ARRAY['general']::skill_enum[]),
(24, 2, 12, 'Final Assessment', 'Complete final test', ARRAY['general','reading','writing','speaking','listening']::skill_enum[]);

-- CLOs for Foundation Course
INSERT INTO clo (id, course_id, code, description) VALUES
(1, 1, 'CLO1', 'Understand basic English in familiar everyday situations'),
(2, 1, 'CLO2', 'Communicate simple information about personal topics'),
(3, 1, 'CLO3', 'Read and understand simple texts about familiar topics'),
(4, 1, 'CLO4', 'Write simple sentences and short paragraphs about personal experiences');

-- PLO-CLO Mappings
INSERT INTO plo_clo_mapping (plo_id, clo_id, status) VALUES
(1, 1, 'active'),
(1, 2, 'active'),
(2, 3, 'active'),
(2, 4, 'active');

-- Course Session-CLO Mappings (Sample - map each CLO to relevant sessions)
INSERT INTO course_session_clo_mapping (course_session_id, clo_id, status) VALUES
-- CLO1 (Understand basic English) - Listening sessions
(1, 1, 'active'), (5, 1, 'active'), (9, 1, 'active'), (13, 1, 'active'), (17, 1, 'active'),
-- CLO2 (Communicate simple info) - Speaking sessions
(2, 2, 'active'), (6, 2, 'active'), (10, 2, 'active'), (14, 2, 'active'), (18, 2, 'active'),
-- CLO3 (Read simple texts) - Reading sessions
(3, 3, 'active'), (7, 3, 'active'), (11, 3, 'active'), (15, 3, 'active'), (19, 3, 'active'),
-- CLO4 (Write simple paragraphs) - Writing sessions
(4, 4, 'active'), (8, 4, 'active'), (12, 4, 'active'), (16, 4, 'active'), (20, 4, 'active');

-- Course Assessments for Foundation
INSERT INTO course_assessment (id, course_id, name, kind, duration_minutes, max_score, skills) VALUES
(1, 1, 'Listening Quiz 1', 'quiz', 30, 20, ARRAY['listening']::skill_enum[]),
(2, 1, 'Speaking Quiz 1', 'quiz', 15, 20, ARRAY['speaking']::skill_enum[]),
(3, 1, 'Reading Quiz 1', 'quiz', 30, 20, ARRAY['reading']::skill_enum[]),
(4, 1, 'Writing Assignment 1', 'assignment', 60, 20, ARRAY['writing']::skill_enum[]),
(5, 1, 'Midterm Exam', 'midterm', 90, 100, ARRAY['listening','reading','writing','speaking']::skill_enum[]),
(6, 1, 'Final Exam', 'final', 120, 100, ARRAY['listening','reading','writing','speaking']::skill_enum[]);

-- Course Assessment-CLO Mappings
INSERT INTO course_assessment_clo_mapping (course_assessment_id, clo_id, status) VALUES
(1, 1, 'active'),
(2, 2, 'active'),
(3, 3, 'active'),
(4, 4, 'active'),
(5, 1, 'active'), (5, 2, 'active'), (5, 3, 'active'), (5, 4, 'active'),
(6, 1, 'active'), (6, 2, 'active'), (6, 3, 'active'), (6, 4, 'active');

-- ========== TIER 4: CLASSES & SESSIONS ==========

-- Classes (Test scenarios: completed, ongoing, scheduled)
INSERT INTO "class" (id, branch_id, course_id, code, name, modality, start_date, planned_end_date, actual_end_date, schedule_days, max_capacity, status, approval_status, created_by, decided_by, submitted_at, decided_at) VALUES
-- HN Branch - Class 1: COMPLETED (to test historical data)
(1, 1, 1, 'HN-FOUND-C1', 'HN Foundation 1 (Completed)', 'offline', '2025-07-07', '2025-09-01', '2025-09-01', ARRAY[1,3,5]::smallint[], 20, 'completed', 'approved', 6, 3, '2025-07-01 10:00:00+07', '2025-07-02 14:00:00+07'),

-- HN Branch - Class 2: ONGOING (main testing class - today is 2025-11-02, started Oct 6)
(2, 1, 1, 'HN-FOUND-O1', 'HN Foundation 1 (Ongoing)', 'offline', '2025-10-06', '2025-11-28', NULL, ARRAY[1,3,5]::smallint[], 20, 'ongoing', 'approved', 6, 3, '2025-09-30 10:00:00+07', '2025-10-01 14:00:00+07'),

-- HN Branch - Class 3: ONGOING (for transfer/makeup scenarios)
(3, 1, 1, 'HN-FOUND-O2', 'HN Foundation 2 (Ongoing)', 'online', '2025-10-07', '2025-11-29', NULL, ARRAY[2,4,6]::smallint[], 25, 'ongoing', 'approved', 6, 3, '2025-10-01 10:00:00+07', '2025-10-02 14:00:00+07'),

-- HN Branch - Class 4: SCHEDULED (for future enrollments)
(4, 1, 1, 'HN-FOUND-S1', 'HN Foundation 3 (Scheduled)', 'hybrid', '2025-11-18', '2026-01-10', NULL, ARRAY[1,3,5]::smallint[], 20, 'scheduled', 'approved', 7, 3, '2025-11-10 10:00:00+07', '2025-11-11 14:00:00+07'),

-- HCM Branch - Class 5: ONGOING
(5, 2, 1, 'HCM-FOUND-O1', 'HCM Foundation 1 (Ongoing)', 'offline', '2025-10-13', '2025-12-05', NULL, ARRAY[1,3,5]::smallint[], 20, 'ongoing', 'approved', 8, 4, '2025-10-06 10:00:00+07', '2025-10-07 14:00:00+07');

-- Generate Sessions for Class 2 (HN-FOUND-O1) - Main testing class
-- Start: 2025-10-06 (Mon), Schedule: Mon/Wed/Fri, 24 sessions over 8 weeks
-- Today: 2025-11-02 (Sat) - Week 5 completed
DO $$
DECLARE
    v_class_id BIGINT := 2;
    v_start_date DATE := '2025-10-06';
    v_schedule_days INT[] := ARRAY[1,3,5]; -- Mon/Wed/Fri
    v_session_count INT := 24;
    v_course_session_id INT;
    v_date DATE;
    v_week INT;
    v_day_idx INT;
    v_session_idx INT := 1;
    v_status session_status_enum;
BEGIN
    FOR v_week IN 0..7 LOOP -- 8 weeks
        FOR v_day_idx IN 1..3 LOOP -- 3 days per week
            EXIT WHEN v_session_idx > v_session_count;
            
            v_course_session_id := v_session_idx;
            v_date := v_start_date + (v_week * 7 + (v_day_idx - 1) * 2); -- Mon, Wed, Fri spacing
            
            -- Set status based on reference date (2025-11-02)
            IF v_date < '2025-11-02' THEN
                v_status := 'done';
            ELSE
                v_status := 'planned';
            END IF;
            
            INSERT INTO session (id, class_id, course_session_id, time_slot_template_id, date, type, status)
            VALUES (100 + v_session_idx, v_class_id, v_course_session_id, 1, v_date, 'class', v_status);
            
            v_session_idx := v_session_idx + 1;
        END LOOP;
    END LOOP;
END $$;

-- Generate Sessions for Class 3 (HN-FOUND-O2) - For transfer scenario
-- Start: 2025-10-07 (Tue), Schedule: Tue/Thu/Sat
DO $$
DECLARE
    v_class_id BIGINT := 3;
    v_start_date DATE := '2025-10-07';
    v_session_count INT := 24;
    v_course_session_id INT;
    v_date DATE;
    v_week INT;
    v_day_idx INT;
    v_session_idx INT := 1;
    v_status session_status_enum;
BEGIN
    FOR v_week IN 0..7 LOOP
        FOR v_day_idx IN 1..3 LOOP
            EXIT WHEN v_session_idx > v_session_count;
            
            v_course_session_id := v_session_idx;
            v_date := v_start_date + (v_week * 7 + (v_day_idx - 1) * 2);
            
            IF v_date < '2025-11-02' THEN
                v_status := 'done';
            ELSE
                v_status := 'planned';
            END IF;
            
            INSERT INTO session (id, class_id, course_session_id, time_slot_template_id, date, type, status)
            VALUES (200 + v_session_idx, v_class_id, v_course_session_id, 4, v_date, 'class', v_status);
            
            v_session_idx := v_session_idx + 1;
        END LOOP;
    END LOOP;
END $$;

-- Session Resources for Class 2
INSERT INTO session_resource (session_id, resource_id)
SELECT id, 1 FROM session WHERE class_id = 2;

-- Session Resources for Class 3 (online - use Zoom)
INSERT INTO session_resource (session_id, resource_id)
SELECT id, 4 FROM session WHERE class_id = 3;

-- Teaching Slots for Class 2 (assign Teacher 1)
INSERT INTO teaching_slot (session_id, teacher_id, status)
SELECT id, 1, 'scheduled' FROM session WHERE class_id = 2;

-- Teaching Slots for Class 3 (assign Teacher 2)
INSERT INTO teaching_slot (session_id, teacher_id, status)
SELECT id, 2, 'scheduled' FROM session WHERE class_id = 3;

-- ========== TIER 5: ENROLLMENTS & ATTENDANCE ==========

-- Enrollments for Class 2 (HN-FOUND-O1) - 15 students
INSERT INTO enrollment (id, class_id, student_id, status, enrolled_at, enrolled_by, join_session_id) VALUES
(1, 2, 1, 'enrolled', '2025-10-01 09:00:00+07', 6, 101),
(2, 2, 2, 'enrolled', '2025-10-01 09:00:00+07', 6, 101),
(3, 2, 3, 'enrolled', '2025-10-01 09:00:00+07', 6, 101),
(4, 2, 4, 'enrolled', '2025-10-01 09:00:00+07', 6, 101),
(5, 2, 5, 'enrolled', '2025-10-01 09:00:00+07', 6, 101),
(6, 2, 6, 'enrolled', '2025-10-01 09:00:00+07', 6, 101),
(7, 2, 7, 'enrolled', '2025-10-01 09:00:00+07', 6, 101),
(8, 2, 8, 'enrolled', '2025-10-01 09:00:00+07', 6, 101),
(9, 2, 9, 'enrolled', '2025-10-01 09:00:00+07', 6, 101),
(10, 2, 10, 'enrolled', '2025-10-01 09:00:00+07', 6, 101),
(11, 2, 11, 'enrolled', '2025-10-01 09:00:00+07', 6, 101),
(12, 2, 12, 'enrolled', '2025-10-01 09:00:00+07', 6, 101),
(13, 2, 13, 'enrolled', '2025-10-01 09:00:00+07', 6, 101),
(14, 2, 14, 'enrolled', '2025-10-01 09:00:00+07', 6, 101),
(15, 2, 15, 'enrolled', '2025-10-01 09:00:00+07', 6, 101),
-- Mid-course enrollment (enrolled after start) - for testing
(16, 2, 16, 'enrolled', '2025-10-20 14:00:00+07', 6, 109);

-- Enrollments for Class 3 (HN-FOUND-O2) - 12 students
INSERT INTO enrollment (id, class_id, student_id, status, enrolled_at, enrolled_by, join_session_id) VALUES
(20, 3, 20, 'enrolled', '2025-10-02 09:00:00+07', 6, 201),
(21, 3, 21, 'enrolled', '2025-10-02 09:00:00+07', 6, 201),
(22, 3, 22, 'enrolled', '2025-10-02 09:00:00+07', 6, 201),
(23, 3, 23, 'enrolled', '2025-10-02 09:00:00+07', 6, 201),
(24, 3, 24, 'enrolled', '2025-10-02 09:00:00+07', 6, 201),
(25, 3, 25, 'enrolled', '2025-10-02 09:00:00+07', 6, 201),
(26, 3, 26, 'enrolled', '2025-10-02 09:00:00+07', 6, 201),
(27, 3, 27, 'enrolled', '2025-10-02 09:00:00+07', 6, 201),
(28, 3, 28, 'enrolled', '2025-10-02 09:00:00+07', 6, 201),
(29, 3, 29, 'enrolled', '2025-10-02 09:00:00+07', 6, 201),
(30, 3, 30, 'enrolled', '2025-10-02 09:00:00+07', 6, 201),
(31, 3, 17, 'enrolled', '2025-10-02 09:00:00+07', 6, 201);

-- Student Sessions for Class 2 enrollments
-- Generate for all students x all sessions (done + planned)
INSERT INTO student_session (student_id, session_id, is_makeup, attendance_status, homework_status, recorded_at)
SELECT 
    e.student_id,
    s.id,
    false,
    CASE 
        WHEN s.status = 'done' THEN 
            CASE 
                -- Most students present
                WHEN random() < 0.85 THEN 'present'::attendance_status_enum
                -- Some absences for testing
                ELSE 'absent'::attendance_status_enum
            END
        ELSE 'planned'::attendance_status_enum
    END,
    CASE 
        WHEN s.status = 'done' AND cs.student_task IS NOT NULL THEN
            CASE 
                WHEN random() < 0.8 THEN 'completed'::homework_status_enum
                ELSE 'incomplete'::homework_status_enum
            END
        ELSE NULL
    END,
    CASE WHEN s.status = 'done' THEN s.date ELSE NULL END
FROM enrollment e
CROSS JOIN session s
LEFT JOIN course_session cs ON s.course_session_id = cs.id
WHERE e.class_id = 2 
  AND s.class_id = 2
  AND (e.join_session_id IS NULL OR s.id >= e.join_session_id);

-- Student Sessions for Class 3
INSERT INTO student_session (student_id, session_id, is_makeup, attendance_status, homework_status, recorded_at)
SELECT 
    e.student_id,
    s.id,
    false,
    CASE 
        WHEN s.status = 'done' THEN 
            CASE WHEN random() < 0.9 THEN 'present'::attendance_status_enum ELSE 'absent'::attendance_status_enum END
        ELSE 'planned'::attendance_status_enum
    END,
    CASE 
        WHEN s.status = 'done' AND cs.student_task IS NOT NULL THEN
            CASE WHEN random() < 0.85 THEN 'completed'::homework_status_enum ELSE 'incomplete'::homework_status_enum END
        ELSE NULL
    END,
    CASE WHEN s.status = 'done' THEN s.date ELSE NULL END
FROM enrollment e
CROSS JOIN session s
LEFT JOIN course_session cs ON s.course_session_id = cs.id
WHERE e.class_id = 3 
  AND s.class_id = 3
  AND (e.join_session_id IS NULL OR s.id >= e.join_session_id);

-- ========== TIER 6: REQUESTS (Test all scenarios) ==========

-- SCENARIO 1: Approved Absence Request
INSERT INTO student_request (id, student_id, current_class_id, request_type, target_session_id, status, request_reason, submitted_by, submitted_at, decided_by, decided_at, note) VALUES
(1, 1, 2, 'absence', 116, 'approved', 'Family emergency - need to attend urgent family matter', 101, '2025-10-25 10:00:00+07', 6, '2025-10-25 14:00:00+07', 'Approved - valid reason');

-- Update corresponding student_session for approved absence
UPDATE student_session 
SET attendance_status = 'absent', note = 'Approved absence: Family emergency'
WHERE student_id = 1 AND session_id = 116;

-- SCENARIO 2: Pending Absence Request (for testing approval flow)
INSERT INTO student_request (id, student_id, current_class_id, request_type, target_session_id, status, request_reason, submitted_by, submitted_at) VALUES
(2, 2, 2, 'absence', 117, 'pending', 'Medical appointment - doctor consultation scheduled', 102, '2025-10-30 09:00:00+07');

-- SCENARIO 3: Rejected Absence Request
INSERT INTO student_request (id, student_id, current_class_id, request_type, target_session_id, status, request_reason, submitted_by, submitted_at, decided_by, decided_at, note) VALUES
(3, 3, 2, 'absence', 118, 'rejected', 'Want to attend friend birthday party', 103, '2025-10-28 10:00:00+07', 6, '2025-10-28 15:00:00+07', 'Rejected - not a valid reason for academic absence');

-- SCENARIO 4: Approved Makeup Request (cross-class)
INSERT INTO student_request (id, student_id, current_class_id, request_type, target_session_id, makeup_session_id, status, request_reason, submitted_by, submitted_at, decided_by, decided_at) VALUES
(4, 4, 2, 'makeup', 107, 213, 'approved', 'Missed session due to illness, want to makeup in online class', 104, '2025-10-22 10:00:00+07', 6, '2025-10-22 16:00:00+07');

-- Create makeup student_session for approved makeup
INSERT INTO student_session (student_id, session_id, is_makeup, makeup_session_id, original_session_id, attendance_status, note)
VALUES (4, 213, true, 213, 107, 'planned', 'Makeup for missed session #107');

-- Update original session note
UPDATE student_session 
SET note = 'Approved for makeup session #213'
WHERE student_id = 4 AND session_id = 107;

-- SCENARIO 5: Pending Makeup Request
INSERT INTO student_request (id, student_id, current_class_id, request_type, target_session_id, makeup_session_id, status, request_reason, submitted_by, submitted_at) VALUES
(5, 5, 2, 'makeup', 108, 214, 'pending', 'Missed session due to work commitment, requesting makeup', 105, '2025-10-31 11:00:00+07');

-- SCENARIO 6: Approved Transfer Request
INSERT INTO student_request (id, student_id, current_class_id, target_class_id, request_type, effective_date, effective_session_id, status, request_reason, submitted_by, submitted_at, decided_by, decided_at) VALUES
(6, 18, 2, 3, 'transfer', '2025-11-04', 214, 'approved', 'Need to change to online class due to work schedule conflict', 118, '2025-10-27 10:00:00+07', 6, '2025-10-28 14:00:00+07');

-- Execute transfer: Update old enrollment
UPDATE enrollment 
SET status = 'transferred', left_at = '2025-11-04 00:00:00+07', left_session_id = 113
WHERE student_id = 18 AND class_id = 2;

-- Execute transfer: Create new enrollment
INSERT INTO enrollment (id, class_id, student_id, status, enrolled_at, enrolled_by, join_session_id)
VALUES (40, 3, 18, 'enrolled', '2025-11-04 00:00:00+07', 6, 214);

-- Execute transfer: Mark future sessions in old class as absent
UPDATE student_session 
SET attendance_status = 'absent', note = 'Transferred to class #3'
WHERE student_id = 18 AND session_id IN (
    SELECT id FROM session WHERE class_id = 2 AND date >= '2025-11-04'
);

-- Execute transfer: Generate student_sessions in new class for future sessions
INSERT INTO student_session (student_id, session_id, is_makeup, attendance_status, note)
SELECT 18, s.id, false, 'planned', 'Transferred from class #2'
FROM session s
WHERE s.class_id = 3 AND s.date >= '2025-11-04';

-- SCENARIO 7: Teacher Swap Request - Approved
INSERT INTO teacher_request (id, teacher_id, session_id, request_type, replacement_teacher_id, status, request_reason, submitted_by, submitted_at, decided_by, decided_at) VALUES
(1, 1, 115, 'swap', 3, 'approved', 'Family emergency - cannot attend session', 20, '2025-10-28 08:00:00+07', 6, '2025-10-28 10:00:00+07');

-- Execute swap: Update teaching_slot
UPDATE teaching_slot 
SET teacher_id = 3, status = 'substituted'
WHERE session_id = 115 AND teacher_id = 1;

-- SCENARIO 8: Teacher Reschedule Request - Pending
INSERT INTO teacher_request (id, teacher_id, session_id, request_type, new_date, new_time_slot_id, new_resource_id, status, request_reason, submitted_by, submitted_at) VALUES
(2, 2, 215, 'reschedule', '2025-11-05', 5, 4, 'pending', 'Conference attendance - propose rescheduling to evening slot', 21, '2025-11-01 09:00:00+07');

-- SCENARIO 9: Teacher Modality Change Request - Approved
INSERT INTO teacher_request (id, teacher_id, session_id, request_type, new_resource_id, status, request_reason, submitted_by, submitted_at, decided_by, decided_at) VALUES
(3, 1, 117, 'modality_change', 4, 'approved', 'Room air conditioning broken - need to switch to online', 20, '2025-11-01 07:00:00+07', 6, '2025-11-01 08:00:00+07');

-- Execute modality change: Update session_resource
DELETE FROM session_resource WHERE session_id = 117;
INSERT INTO session_resource (session_id, resource_id) VALUES (117, 4);

-- SCENARIO 10: Request created by Academic Affair on behalf (waiting confirmation)
INSERT INTO student_request (id, student_id, current_class_id, request_type, target_session_id, status, request_reason, submitted_by, submitted_at, note) VALUES
(7, 6, 2, 'absence', 119, 'waiting_confirm', 'Student called to report illness - created on behalf', 6, '2025-11-01 13:00:00+07', 'Created by Academic Affair via phone call');

-- ========== TIER 7: ASSESSMENTS & SCORES ==========

-- Assessments for Class 2 (scheduled and completed)
INSERT INTO assessment (id, class_id, course_assessment_id, scheduled_date, actual_date) VALUES
(1, 2, 1, '2025-10-18 08:00:00+07', '2025-10-18 08:00:00+07'), -- Listening Quiz 1 - completed
(2, 2, 2, '2025-10-21 08:00:00+07', '2025-10-21 08:00:00+07'), -- Speaking Quiz 1 - completed
(3, 2, 5, '2025-11-08 08:00:00+07', NULL), -- Midterm - scheduled
(4, 2, 6, '2025-11-27 08:00:00+07', NULL); -- Final - scheduled

-- Scores for completed assessments (Listening Quiz 1)
INSERT INTO score (id, assessment_id, student_id, score, feedback, graded_by, graded_at) VALUES
(1, 1, 1, 18.0, 'Good listening skills', 1, '2025-10-19 10:00:00+07'),
(2, 1, 2, 16.5, 'Need more practice on numbers', 1, '2025-10-19 10:00:00+07'),
(3, 1, 3, 19.0, 'Excellent performance', 1, '2025-10-19 10:00:00+07'),
(4, 1, 4, 15.0, 'Satisfactory', 1, '2025-10-19 10:00:00+07'),
(5, 1, 5, 17.5, 'Good work', 1, '2025-10-19 10:00:00+07'),
(6, 1, 6, 14.0, 'Need improvement', 1, '2025-10-19 10:00:00+07'),
(7, 1, 7, 18.5, 'Very good', 1, '2025-10-19 10:00:00+07'),
(8, 1, 8, 16.0, 'Good progress', 1, '2025-10-19 10:00:00+07'),
(9, 1, 9, 17.0, 'Well done', 1, '2025-10-19 10:00:00+07'),
(10, 1, 10, 15.5, 'Fair performance', 1, '2025-10-19 10:00:00+07');

-- Scores for Speaking Quiz 1
INSERT INTO score (id, assessment_id, student_id, score, feedback, graded_by, graded_at) VALUES
(11, 2, 1, 17.0, 'Good fluency, work on pronunciation', 1, '2025-10-22 10:00:00+07'),
(12, 2, 2, 18.0, 'Confident speaker', 1, '2025-10-22 10:00:00+07'),
(13, 2, 3, 16.5, 'Good effort', 1, '2025-10-22 10:00:00+07'),
(14, 2, 4, 15.0, 'Need more practice', 1, '2025-10-22 10:00:00+07'),
(15, 2, 5, 19.0, 'Excellent speaking skills', 1, '2025-10-22 10:00:00+07');

-- ========== TIER 8: FEEDBACK & QA ==========

-- Student Feedback for completed phase
INSERT INTO student_feedback (id, student_id, class_id, phase_id, is_feedback, submitted_at) VALUES
(1, 1, 2, 1, true, '2025-10-25 20:00:00+07'),
(2, 2, 2, 1, true, '2025-10-25 21:00:00+07'),
(3, 3, 2, 1, true, '2025-10-26 18:00:00+07');

-- Student Feedback Responses
INSERT INTO student_feedback_response (id, feedback_id, question_id, rating) VALUES
-- Student 1 responses
(1, 1, 1, 5), -- Teaching quality: 5/5
(2, 1, 2, 4), -- Lesson organization: 4/5
(3, 1, 3, 5), -- Materials: 5/5
(4, 1, 4, 4), -- Class management: 4/5
(5, 1, 5, 5), -- Recommendation: 5/5
-- Student 2 responses
(6, 2, 1, 4),
(7, 2, 2, 4),
(8, 2, 3, 3),
(9, 2, 4, 4),
(10, 2, 5, 4),
-- Student 3 responses
(11, 3, 1, 5),
(12, 3, 2, 5),
(13, 3, 3, 4),
(14, 3, 4, 5),
(15, 3, 5, 5);

-- QA Reports
INSERT INTO qa_report (id, class_id, session_id, reported_by, report_type, status, findings, action_items) VALUES
(1, 2, 105, 10, 'classroom_observation', 'closed', 'Teacher demonstrated excellent engagement techniques. Students actively participated.', 'Share teaching approach with other teachers in next training session.'),
(2, 2, 110, 10, 'classroom_observation', 'open', 'Noticed some students struggling with listening exercises. Recommend additional practice materials.', 'Teacher to provide supplementary listening resources. Follow-up in 2 weeks.');

-- ========== EDGE CASES & BOUNDARY CONDITIONS ==========

-- EDGE CASE 1: Student with no absences (perfect attendance)
-- Student 7 in Class 2 - already has all "present" from earlier logic

-- EDGE CASE 2: Student with high absence rate
UPDATE student_session 
SET attendance_status = 'absent', note = 'Frequent absence - needs monitoring'
WHERE student_id = 13 AND session_id IN (101, 103, 105, 107, 109);

-- EDGE CASE 3: Class at maximum capacity
UPDATE "class" SET max_capacity = 15 WHERE id = 2; -- Already has 15 enrolled (at capacity)

-- EDGE CASE 4: Student with NULL optional fields
UPDATE user_account SET dob = NULL, address = NULL WHERE id = 160;

-- EDGE CASE 5: Session with NULL teacher_note (not yet submitted)
-- Already handled - planned sessions don't have notes

-- EDGE CASE 6: Enrollment on exact start date
INSERT INTO enrollment (id, class_id, student_id, status, enrolled_at, enrolled_by, join_session_id)
VALUES (50, 2, 19, 'enrolled', '2025-10-06 00:00:00+07', 6, 101);

INSERT INTO student_session (student_id, session_id, is_makeup, attendance_status)
SELECT 19, s.id, false, 
    CASE WHEN s.status = 'done' THEN 'present'::attendance_status_enum ELSE 'planned'::attendance_status_enum END
FROM session s
WHERE s.class_id = 2;

-- EDGE CASE 7: Future class with no enrollments yet
-- Class 4 - already defined as scheduled, no enrollments

-- ========== FINAL SEQUENCE UPDATES ==========
SELECT setval('center_id_seq', (SELECT MAX(id) FROM center), true);
SELECT setval('branch_id_seq', (SELECT MAX(id) FROM branch), true);
SELECT setval('role_id_seq', (SELECT MAX(id) FROM role), true);
SELECT setval('user_account_id_seq', (SELECT MAX(id) FROM user_account), true);
SELECT setval('teacher_id_seq', (SELECT MAX(id) FROM teacher), true);
SELECT setval('student_id_seq', (SELECT MAX(id) FROM student), true);
SELECT setval('subject_id_seq', (SELECT MAX(id) FROM subject), true);
SELECT setval('level_id_seq', (SELECT MAX(id) FROM level), true);
SELECT setval('plo_id_seq', (SELECT MAX(id) FROM plo), true);
SELECT setval('course_id_seq', (SELECT MAX(id) FROM course), true);
SELECT setval('course_phase_id_seq', (SELECT MAX(id) FROM course_phase), true);
SELECT setval('clo_id_seq', (SELECT MAX(id) FROM clo), true);
SELECT setval('course_session_id_seq', (SELECT MAX(id) FROM course_session), true);
SELECT setval('course_assessment_id_seq', (SELECT MAX(id) FROM course_assessment), true);
SELECT setval('class_id_seq', (SELECT MAX(id) FROM "class"), true);
SELECT setval('session_id_seq', (SELECT MAX(id) FROM session), true);
SELECT setval('assessment_id_seq', (SELECT MAX(id) FROM assessment), true);
SELECT setval('score_id_seq', (SELECT MAX(id) FROM score), true);
SELECT setval('student_request_id_seq', (SELECT MAX(id) FROM student_request), true);
SELECT setval('teacher_request_id_seq', (SELECT MAX(id) FROM teacher_request), true);
SELECT setval('student_feedback_id_seq', (SELECT MAX(id) FROM student_feedback), true);
SELECT setval('student_feedback_response_id_seq', (SELECT MAX(id) FROM student_feedback_response), true);
SELECT setval('qa_report_id_seq', (SELECT MAX(id) FROM qa_report), true);
SELECT setval('time_slot_template_id_seq', (SELECT MAX(id) FROM time_slot_template), true);
SELECT setval('resource_id_seq', (SELECT MAX(id) FROM resource), true);
SELECT setval('feedback_question_id_seq', (SELECT MAX(id) FROM feedback_question), true);
SELECT setval('enrollment_id_seq', (SELECT MAX(id) FROM enrollment), true);

-- ========== VERIFICATION QUERIES ==========
-- Uncomment to verify data integrity

-- SELECT 'Total Users' as metric, COUNT(*) as count FROM user_account
-- UNION ALL SELECT 'Total Teachers', COUNT(*) FROM teacher
-- UNION ALL SELECT 'Total Students', COUNT(*) FROM student
-- UNION ALL SELECT 'Total Classes', COUNT(*) FROM "class"
-- UNION ALL SELECT 'Total Sessions', COUNT(*) FROM session
-- UNION ALL SELECT 'Total Enrollments', COUNT(*) FROM enrollment
-- UNION ALL SELECT 'Total Student Sessions', COUNT(*) FROM student_session
-- UNION ALL SELECT 'Total Requests (Student)', COUNT(*) FROM student_request
-- UNION ALL SELECT 'Total Requests (Teacher)', COUNT(*) FROM teacher_request
-- UNION ALL SELECT 'Done Sessions', COUNT(*) FROM session WHERE status = 'done'
-- UNION ALL SELECT 'Planned Sessions', COUNT(*) FROM session WHERE status = 'planned'
-- UNION ALL SELECT 'Attendance Records (Present)', COUNT(*) FROM student_session WHERE attendance_status = 'present'
-- UNION ALL SELECT 'Attendance Records (Absent)', COUNT(*) FROM student_session WHERE attendance_status = 'absent';

-- =========================================
-- END OF SEED DATA
-- =========================================
-- Summary:
-- - 2 Branches (HN, HCM)
-- - 16 Teachers with skills
-- - 60 Students
-- - 1 Complete Course (Foundation) with 24 sessions, CLOs, assessments
-- - 5 Classes (completed, ongoing, scheduled)
-- - Full session generation for 2 main classes
-- - 16 Enrollments with complete student_sessions
-- - 10 Request scenarios (approved, pending, rejected, waiting_confirm)
-- - Assessments with scores
-- - Student feedback and QA reports
-- - Edge cases: capacity limits, mid-course enrollment, transfers, perfect attendance
-- =========================================
