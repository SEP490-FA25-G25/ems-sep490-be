package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.studentmanagement.*;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.EnrollmentStatus;
import org.fyp.tmssep490be.entities.enums.UserStatus;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.*;
import org.fyp.tmssep490be.services.StudentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserBranchesRepository userBranchesRepository;

    @Override
    public Page<StudentListItemDTO> getStudents(
            List<Long> branchIds,
            String search,
            UserStatus status,
            Long courseId,
            Pageable pageable,
            Long userId
    ) {
        log.debug("Getting students for user {} with filters: branchIds={}, search={}, status={}, courseId={}",
                userId, branchIds, search, status, courseId);

        // Get user's accessible branches if not provided
        if (branchIds == null || branchIds.isEmpty()) {
            branchIds = getUserAccessibleBranches(userId);
        }

        Page<Student> students;

        // Filter by course if specified
        if (courseId != null) {
            students = studentRepository.findStudentsByCourse(courseId, branchIds, pageable);
        } else {
            students = studentRepository.findStudentsInBranchesWithSearch(branchIds, search, pageable);
        }

        return students.map(this::convertToStudentListItemDTO);
    }

    @Override
    public StudentDetailDTO getStudentDetail(Long studentId, Long userId) {
        log.debug("Getting student detail for student {} by user {}", studentId, userId);

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new CustomException(ErrorCode.STUDENT_NOT_FOUND));

        // Validate student access
        validateStudentAccess(student, userId);

        return convertToStudentDetailDTO(student);
    }

    @Override
    public Page<StudentEnrollmentHistoryDTO> getStudentEnrollmentHistory(
            Long studentId,
            List<Long> branchIds,
            Pageable pageable,
            Long userId
    ) {
        log.debug("Getting enrollment history for student {} by user {}", studentId, userId);

        // Validate student access
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new CustomException(ErrorCode.STUDENT_NOT_FOUND));
        validateStudentAccess(student, userId);

        // Get user's accessible branches if not provided
        if (branchIds == null || branchIds.isEmpty()) {
            branchIds = getUserAccessibleBranches(userId);
        }

        Page<Enrollment> enrollments = enrollmentRepository.findStudentEnrollmentHistory(
                studentId, branchIds, pageable);

        return enrollments.map(this::convertToStudentEnrollmentHistoryDTO);
    }

    private List<Long> getUserAccessibleBranches(Long userId) {
        return userBranchesRepository.findBranchIdsByUserId(userId);
    }

    private void validateStudentAccess(Student student, Long userId) {
        List<Long> accessibleBranches = getUserAccessibleBranches(userId);

        // Check if student belongs to any accessible branch
        boolean hasAccess = student.getUserAccount().getUserBranches().stream()
                .anyMatch(ub -> accessibleBranches.contains(ub.getBranch().getId()));

        if (!hasAccess) {
            throw new CustomException(ErrorCode.STUDENT_ACCESS_DENIED);
        }
    }

    private StudentListItemDTO convertToStudentListItemDTO(Student student) {
        UserAccount user = student.getUserAccount();

        // Get branch from user's branches (take first one)
        String branchName = null;
        Long branchId = null;
        if (!user.getUserBranches().isEmpty()) {
            branchId = user.getUserBranches().iterator().next().getBranch().getId();
            branchName = user.getUserBranches().iterator().next().getBranch().getName();
        }

        // Get enrollment counts
        int activeEnrollments = enrollmentRepository.countByStudentIdAndStatus(
                student.getId(), EnrollmentStatus.ENROLLED);

        Optional<Enrollment> latestEnrollment = Optional.ofNullable(
                enrollmentRepository.findLatestEnrollmentByStudent(student.getId()));

        return StudentListItemDTO.builder()
                .id(student.getId())
                .studentCode(student.getStudentCode())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .level(student.getLevel())
                .branchName(branchName)
                .branchId(branchId)
                .activeEnrollments((long) activeEnrollments)
                .lastEnrollmentDate(latestEnrollment
                        .map(e -> e.getEnrolledAt().toLocalDate())
                        .orElse(null))
                .canEnroll(activeEnrollments < 3) // Example max concurrent enrollments
                .build();
    }

    private StudentDetailDTO convertToStudentDetailDTO(Student student) {
        UserAccount user = student.getUserAccount();

        // Get branch info
        String branchName = null;
        Long branchId = null;
        if (!user.getUserBranches().isEmpty()) {
            branchId = user.getUserBranches().iterator().next().getBranch().getId();
            branchName = user.getUserBranches().iterator().next().getBranch().getName();
        }

        // Get enrollment statistics
        int totalEnrollments = student.getEnrollments().size();
        int activeEnrollments = (int) student.getEnrollments().stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.ENROLLED)
                .count();
        int completedEnrollments = (int) student.getEnrollments().stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.COMPLETED)
                .count();

        // Get first and last enrollment dates
        LocalDate firstEnrollmentDate = student.getEnrollments().stream()
                .map(e -> e.getEnrolledAt().toLocalDate())
                .min(LocalDate::compareTo)
                .orElse(null);

        LocalDate lastEnrollmentDate = student.getEnrollments().stream()
                .map(e -> e.getEnrolledAt().toLocalDate())
                .max(LocalDate::compareTo)
                .orElse(null);

        // Get current active classes
        List<StudentActiveClassDTO> currentClasses = student.getEnrollments().stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.ENROLLED)
                .map(this::convertToStudentActiveClassDTO)
                .collect(Collectors.toList());

        return StudentDetailDTO.builder()
                .id(student.getId())
                .studentCode(student.getStudentCode())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .address(user.getAddress())
                .gender(user.getGender().name())
                .dateOfBirth(user.getDob())
                .level(student.getLevel())
                .status(user.getStatus().name())
                .lastLoginAt(user.getLastLoginAt())
                .branchName(branchName)
                .branchId(branchId)
                .totalEnrollments((long) totalEnrollments)
                .activeEnrollments((long) activeEnrollments)
                .completedEnrollments((long) completedEnrollments)
                .firstEnrollmentDate(firstEnrollmentDate)
                .lastEnrollmentDate(lastEnrollmentDate)
                .currentClasses(currentClasses)
                .build();
    }

    private StudentActiveClassDTO convertToStudentActiveClassDTO(Enrollment enrollment) {
        ClassEntity classEntity = enrollment.getClassEntity();

        return StudentActiveClassDTO.builder()
                .classId(classEntity.getId())
                .classCode(classEntity.getCode())
                .className(classEntity.getName())
                .courseName(classEntity.getCourse().getName())
                .branchName(classEntity.getBranch().getName())
                .startDate(classEntity.getStartDate())
                .plannedEndDate(classEntity.getPlannedEndDate())
                .enrollmentStatus(enrollment.getStatus().name())
                .enrolledAt(enrollment.getEnrolledAt())
                .build();
    }

    private StudentEnrollmentHistoryDTO convertToStudentEnrollmentHistoryDTO(Enrollment enrollment) {
        Student student = enrollment.getStudent();
        ClassEntity classEntity = enrollment.getClassEntity();
        UserAccount studentUser = student.getUserAccount();

        return StudentEnrollmentHistoryDTO.builder()
                .id(enrollment.getId())
                .studentId(student.getId())
                .studentCode(student.getStudentCode())
                .studentName(studentUser.getFullName())
                .classId(classEntity.getId())
                .classCode(classEntity.getCode())
                .className(classEntity.getName())
                .courseName(classEntity.getCourse().getName())
                .branchName(classEntity.getBranch().getName())
                .status(enrollment.getStatus().name())
                .enrolledAt(enrollment.getEnrolledAt())
                .leftAt(enrollment.getLeftAt())
                .enrolledByName(enrollment.getEnrolledByUser() != null ?
                        enrollment.getEnrolledByUser().getFullName() : null)
                .classStartDate(classEntity.getStartDate())
                .classEndDate(classEntity.getPlannedEndDate())
                .modality(classEntity.getModality().name())
                .totalSessions(0) // TODO: Calculate from sessions
                .attendedSessions(0) // TODO: Calculate from student_sessions
                .attendanceRate(0.0) // TODO: Calculate from student_sessions
                .averageScore(null) // TODO: Calculate from scores
                .build();
    }
}
