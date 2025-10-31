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
CREATE TYPE session_status_enum AS ENUM ('PLANNED', 'CANCELLED', 'DONE');

-- Enum for Session Type
CREATE TYPE session_type_enum AS ENUM ('CLASS', 'TEACHER_RESCHEDULE');

-- Enum for Attendance Status
CREATE TYPE attendance_status_enum AS ENUM ('PLANNED', 'PRESENT', 'ABSENT');

-- Enum for Enrollment Status
CREATE TYPE enrollment_status_enum AS ENUM ('ENROLLED', 'TRANSFERRED', 'DROPPED', 'COMPLETED');

-- Enum for Request Status
CREATE TYPE request_status_enum AS ENUM ('PENDING', 'WAITING_CONFIRM', 'APPROVED', 'REJECTED');

-- Enum for Teacher Request Type
CREATE TYPE teacher_request_type_enum AS ENUM ('SWAP', 'RESCHEDULE', 'MODALITY_CHANGE');

-- Enum for Student Request Type
CREATE TYPE student_request_type_enum AS ENUM ('ABSENCE', 'MAKEUP', 'TRANSFER');

-- Enum for Resource Type
CREATE TYPE resource_type_enum AS ENUM ('ROOM', 'VIRTUAL');

-- Enum for Modality
CREATE TYPE modality_enum AS ENUM ('OFFLINE', 'ONLINE', 'HYBRID');

-- Enum for Skill
CREATE TYPE skill_enum AS ENUM ('GENERAL', 'READING', 'WRITING', 'SPEAKING', 'LISTENING');

-- Enum for Teaching Role
CREATE TYPE teaching_role_enum AS ENUM ('PRIMARY', 'ASSISTANT');

-- Enum for Branch Status
CREATE TYPE branch_status_enum AS ENUM ('ACTIVE', 'INACTIVE', 'CLOSED', 'PLANNED');

-- Enum for Class Status
CREATE TYPE class_status_enum AS ENUM ('DRAFT', 'SCHEDULED', 'ONGOING', 'COMPLETED', 'CANCELLED');

-- Enum for Subject Status
CREATE TYPE subject_status_enum AS ENUM ('DRAFT', 'ACTIVE', 'INACTIVE');

-- Enum for Assessment Kind
CREATE TYPE assessment_kind_enum AS ENUM ('QUIZ', 'MIDTERM', 'FINAL', 'ASSIGNMENT', 'PROJECT', 'ORAL', 'PRACTICE', 'OTHER');

-- Enum for Teaching Slot Status
CREATE TYPE teaching_slot_status_enum AS ENUM ('SCHEDULED', 'ON_LEAVE', 'SUBSTITUTED');

-- Enum for Homework Status
CREATE TYPE homework_status_enum AS ENUM ('COMPLETED', 'INCOMPLETE', 'NO_HOMEWORK');

-- Enum for Course Status
CREATE TYPE course_status_enum AS ENUM ('DRAFT', 'ACTIVE', 'INACTIVE');

-- Enum for Approval Status
CREATE TYPE approval_status_enum AS ENUM ('PENDING', 'APPROVED', 'REJECTED');

-- Enum for Material Type
CREATE TYPE material_type_enum AS ENUM ('VIDEO', 'PDF', 'SLIDE', 'AUDIO', 'DOCUMENT', 'OTHER');

-- Enum for Mapping Status
CREATE TYPE mapping_status_enum AS ENUM ('ACTIVE', 'INACTIVE');
