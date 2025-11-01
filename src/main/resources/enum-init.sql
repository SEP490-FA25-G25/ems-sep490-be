-- =========================================
-- PostgreSQL Enum Types for TMS
-- This script creates enum types before Hibernate creates tables
--
-- QUAN TRỌNG:
-- - File này CHỈ định nghĩa ENUM TYPES, KHÔNG tạo tables
-- - Hibernate sẽ TỰ ĐỘNG tạo/update tables dựa trên entities
-- - Khi thêm enum mới: Chỉ cần thêm dòng CREATE TYPE ở đây
--
-- Note: If types already exist, errors will be ignored due to continue-on-error setting
-- =========================================

-- Enum for Session Status
CREATE TYPE session_status_enum AS ENUM ('planned', 'cancelled', 'done');

-- Enum for Session Type
CREATE TYPE session_type_enum AS ENUM ('class', 'teacher_reschedule');

-- Enum for Attendance Status
CREATE TYPE attendance_status_enum AS ENUM ('planned', 'present', 'absent');

-- Enum for Enrollment Status
CREATE TYPE enrollment_status_enum AS ENUM ('enrolled', 'transferred', 'dropped', 'completed');

-- Enum for Request Status
CREATE TYPE request_status_enum AS ENUM ('pending', 'waiting_confirm', 'approved', 'rejected');

-- Enum for Teacher Request Type
CREATE TYPE teacher_request_type_enum AS ENUM ('swap', 'reschedule', 'modality_change');

-- Enum for Student Request Type
CREATE TYPE student_request_type_enum AS ENUM ('absence', 'makeup', 'transfer');

-- Enum for Resource Type
CREATE TYPE resource_type_enum AS ENUM ('room', 'virtual');

-- Enum for Modality
CREATE TYPE modality_enum AS ENUM ('offline', 'online', 'hybrid');

-- Enum for Skill
CREATE TYPE skill_enum AS ENUM ('general', 'reading', 'writing', 'speaking', 'listening');

-- Enum for Teaching Role
CREATE TYPE teaching_role_enum AS ENUM ('primary', 'assistant');

-- Enum for Branch Status
CREATE TYPE branch_status_enum AS ENUM ('active', 'inactive', 'closed', 'planned');

-- Enum for Class Status
CREATE TYPE class_status_enum AS ENUM ('draft', 'scheduled', 'ongoing', 'completed', 'cancelled');

-- Enum for Subject Status
CREATE TYPE subject_status_enum AS ENUM ('draft', 'active', 'inactive');

-- Enum for Assessment Kind
CREATE TYPE assessment_kind_enum AS ENUM ('quiz', 'midterm', 'final', 'assignment', 'project', 'oral', 'practice', 'other');

-- Enum for Teaching Slot Status
CREATE TYPE teaching_slot_status_enum AS ENUM ('scheduled', 'on_leave', 'substituted');

-- Enum for Homework Status
CREATE TYPE homework_status_enum AS ENUM ('completed', 'incomplete', 'no_homework');

-- Enum for Course Status
CREATE TYPE course_status_enum AS ENUM ('draft', 'active', 'inactive');

-- Enum for Approval Status
CREATE TYPE approval_status_enum AS ENUM ('pending', 'approved', 'rejected');

-- Enum for Material Type
CREATE TYPE material_type_enum AS ENUM ('video', 'pdf', 'slide', 'audio', 'document', 'other');

-- Enum for Mapping Status
CREATE TYPE mapping_status_enum AS ENUM ('active', 'inactive');

-- Enum for Gender
CREATE TYPE gender_enum AS ENUM ('male', 'female', 'other');

-- Enum for User Status
CREATE TYPE user_status_enum AS ENUM ('active', 'inactive', 'suspended');