package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.classmanagement.*;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.*;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.*;
import org.fyp.tmssep490be.services.ClassService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of ClassService for Academic Affairs class management
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ClassServiceImpl implements ClassService {

    private final ClassRepository classRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final SessionRepository sessionRepository;
    private final UserBranchesRepository userBranchesRepository;
    private final TeachingSlotRepository teachingSlotRepository;
    private final StudentRepository studentRepository;
    private final ReplacementSkillAssessmentRepository skillAssessmentRepository;

    @Override
    public Page<ClassListItemDTO> getClasses(
            List<Long> branchIds,
            Long courseId,
            ClassStatus status,
            ApprovalStatus approvalStatus,
            Modality modality,
            String search,
            Pageable pageable,
            Long userId
    ) {
        log.debug("Getting classes for user {} with filters: branchIds={}, courseId={}, status={}, approvalStatus={}, modality={}, search={}",
                userId, branchIds, courseId, status, approvalStatus, modality, search);

        // Get user's branch access
        List<Long> accessibleBranchIds = getUserAccessibleBranches(userId);

        // Filter by provided branch IDs if any
        List<Long> finalBranchIds = branchIds != null ? branchIds : accessibleBranchIds;
        if (finalBranchIds.isEmpty()) {
            throw new CustomException(ErrorCode.CLASS_NO_BRANCH_ACCESS);
        }

        // Query classes with filters (null status/approvalStatus = all)
        Page<ClassEntity> classes = classRepository.findClassesForAcademicAffairs(
                finalBranchIds,
                approvalStatus,  // null = all approval statuses
                status,          // null = all class statuses
                courseId,
                modality,
                search,
                pageable
        );

        return classes.map(this::convertToClassListItemDTO);
    }

    @Override
    public ClassDetailDTO getClassDetail(Long classId, Long userId) {
        log.debug("Getting class detail for class {} by user {}", classId, userId);

        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));

        // Validate user has access to this class's branch (no status restrictions for detail view)
        validateClassBranchAccess(classEntity, userId);

        // Get enrollment summary
        Integer currentEnrolled = enrollmentRepository.countByClassIdAndStatus(classId, EnrollmentStatus.ENROLLED);
        ClassDetailDTO.EnrollmentSummary enrollmentSummary = calculateEnrollmentSummary(
                currentEnrolled, classEntity.getMaxCapacity()
        );

        // Get upcoming sessions (next 5)
        List<Session> upcomingSessions = sessionRepository.findUpcomingSessions(
                classId, PageRequest.of(0, 5)
        );
        List<ClassDetailDTO.SessionDTO> sessionDTOs = upcomingSessions.stream()
                .map(this::convertToSessionDTO)
                .collect(Collectors.toList());

        // Get all teachers teaching this class
        List<TeacherSummaryDTO> teachers = getTeachersForClass(classId);

        return ClassDetailDTO.builder()
                .id(classEntity.getId())
                .code(classEntity.getCode())
                .name(classEntity.getName())
                .course(convertToCourseDTO(classEntity.getCourse()))
                .branch(convertToBranchDTO(classEntity.getBranch()))
                .modality(classEntity.getModality())
                .startDate(classEntity.getStartDate())
                .plannedEndDate(classEntity.getPlannedEndDate())
                .actualEndDate(classEntity.getActualEndDate())
                .scheduleDays(classEntity.getScheduleDays())
                .maxCapacity(classEntity.getMaxCapacity())
                .status(classEntity.getStatus())
                .approvalStatus(classEntity.getApprovalStatus())
                .rejectionReason(classEntity.getRejectionReason())
                .submittedAt(classEntity.getSubmittedAt() != null ?
                        classEntity.getSubmittedAt().toLocalDate() : null)
                .decidedAt(classEntity.getDecidedAt() != null ?
                        classEntity.getDecidedAt().toLocalDate() : null)
                .decidedByName(classEntity.getDecidedBy() != null ?
                        classEntity.getDecidedBy().getFullName() : null)
                .teachers(teachers)
                .scheduleSummary(formatScheduleSummary(classEntity.getScheduleDays()))
                .enrollmentSummary(enrollmentSummary)
                .upcomingSessions(sessionDTOs)
                .build();
    }

    @Override
    public Page<ClassStudentDTO> getClassStudents(
            Long classId,
            String search,
            Pageable pageable,
            Long userId
    ) {
        log.debug("Getting students for class {} by user {} with search: {}", classId, userId, search);

        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));

        // Validate user has access to this class's branch (with status restrictions for enrollment operations)
        validateClassAccess(classEntity, userId);

        // Get enrolled students
        Page<Enrollment> enrollments = enrollmentRepository.findEnrolledStudentsByClass(
                classId, EnrollmentStatus.ENROLLED, search, pageable
        );

        return enrollments.map(this::convertToClassStudentDTO);
    }

    @Override
    public ClassEnrollmentSummaryDTO getClassEnrollmentSummary(Long classId, Long userId) {
        log.debug("Getting enrollment summary for class {} by user {}", classId, userId);

        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));

        // Validate user has access to this class's branch
        validateClassAccess(classEntity, userId);

        // Calculate enrollment data
        Integer currentEnrolled = enrollmentRepository.countByClassIdAndStatus(classId, EnrollmentStatus.ENROLLED);
        Integer maxCapacity = classEntity.getMaxCapacity();
        Integer availableSlots = maxCapacity - currentEnrolled;
        Double utilizationRate = maxCapacity > 0 ? (double) currentEnrolled / maxCapacity * 100 : 0.0;

        // Determine if enrollment is possible
        boolean canEnroll = (classEntity.getStatus() == ClassStatus.SCHEDULED
                     || classEntity.getStatus() == ClassStatus.ONGOING)
                && classEntity.getApprovalStatus() == ApprovalStatus.APPROVED
                && availableSlots > 0;

        String restrictionReason = null;
        if (!canEnroll) {
            if (classEntity.getStatus() == ClassStatus.COMPLETED) {
                restrictionReason = "Class has completed";
            } else if (classEntity.getStatus() == ClassStatus.CANCELLED) {
                restrictionReason = "Class was cancelled";
            } else if (classEntity.getStatus() != ClassStatus.SCHEDULED
                    && classEntity.getStatus() != ClassStatus.ONGOING) {
                restrictionReason = "Class is not available for enrollment";
            } else if (classEntity.getApprovalStatus() != ApprovalStatus.APPROVED) {
                restrictionReason = "Class is not approved";
            } else if (availableSlots <= 0) {
                restrictionReason = "Class is at full capacity";
            }
        }

        return ClassEnrollmentSummaryDTO.builder()
                .classId(classEntity.getId())
                .classCode(classEntity.getCode())
                .className(classEntity.getName())
                .currentEnrolled(currentEnrolled)
                .maxCapacity(maxCapacity)
                .availableSlots(availableSlots)
                .utilizationRate(utilizationRate)
                .canEnrollStudents(canEnroll)
                .enrollmentRestrictionReason(restrictionReason)
                .status(classEntity.getStatus().name())
                .approvalStatus(classEntity.getApprovalStatus().name())
                .startDate(classEntity.getStartDate())
                .build();
    }

    @Override
    public Page<AvailableStudentDTO> getAvailableStudentsForClass(
            Long classId,
            String search,
            Pageable pageable,
            Long userId
    ) {
        log.debug("Getting available students for class {} by user {} with search: {}", classId, userId, search);

        // Validate class exists and user has access (with status restrictions for enrollment operations)
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new CustomException(ErrorCode.CLASS_NOT_FOUND));
        validateClassAccess(classEntity, userId);

        // Get available students from the same branch, excluding already enrolled ones
        // Remove sort from pageable since matchPriority doesn't exist in entity
        Long branchId = classEntity.getBranch().getId();
        Pageable unsortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        Page<Student> availableStudents = studentRepository.findAvailableStudentsForClass(
                classId, branchId, search, unsortedPageable
        );

        // Get class's course subject and level for skill assessment matching
        Long classSubjectId = classEntity.getCourse().getLevel().getSubject().getId();
        Long classLevelId = classEntity.getCourse().getLevel().getId();

        // Batch fetch skill assessments for all students
        List<Long> studentIds = availableStudents.getContent().stream()
                .map(Student::getId)
                .collect(Collectors.toList());

        List<ReplacementSkillAssessment> assessments = studentIds.isEmpty() ?
                List.of() :
                skillAssessmentRepository.findLatestAssessmentsByStudentsAndSubject(studentIds, classSubjectId);

        // Create a map for quick lookup: studentId -> assessment
        var assessmentMap = assessments.stream()
                .collect(Collectors.toMap(
                        a -> a.getStudent().getId(),
                        a -> a,
                        (a1, a2) -> a1.getAssessmentDate().isAfter(a2.getAssessmentDate()) ? a1 : a2
                ));

        // Convert to DTOs with priority calculation and sort by matchPriority
        List<AvailableStudentDTO> dtos = availableStudents.getContent().stream()
                .map(student -> convertToAvailableStudentDTO(student, assessmentMap.get(student.getId()),
                        classSubjectId, classLevelId))
                .sorted((dto1, dto2) -> {
                    // Sort by matchPriority (ascending: 1, 2, 3)
                    // Then by student name if same priority
                    int priorityCompare = dto1.getMatchPriority().compareTo(dto2.getMatchPriority());
                    if (priorityCompare != 0) {
                        return priorityCompare;
                    }
                    return dto1.getFullName().compareTo(dto2.getFullName());
                })
                .collect(Collectors.toList());

        // Create new Page with sorted DTOs
        return new org.springframework.data.domain.PageImpl<>(
                dtos,
                pageable,
                availableStudents.getTotalElements()
        );
    }

    // Helper methods

    private List<Long> getUserAccessibleBranches(Long userId) {
        return userBranchesRepository.findBranchIdsByUserId(userId);
    }

    /**
     * Validate user can access class from their branch only (no status restrictions)
     * Used for read-only operations like viewing class details
     */
    private void validateClassBranchAccess(ClassEntity classEntity, Long userId) {
        List<Long> accessibleBranchIds = getUserAccessibleBranches(userId);

        if (!accessibleBranchIds.contains(classEntity.getBranch().getId())) {
            throw new CustomException(ErrorCode.CLASS_ACCESS_DENIED);
        }
    }

    /**
     * Validate user can access class with status restrictions
     * Used for enrollment-related operations (viewing students, enrolling new students)
     */
    private void validateClassAccess(ClassEntity classEntity, Long userId) {
        List<Long> accessibleBranchIds = getUserAccessibleBranches(userId);

        if (!accessibleBranchIds.contains(classEntity.getBranch().getId())) {
            throw new CustomException(ErrorCode.CLASS_ACCESS_DENIED);
        }

        if (classEntity.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new CustomException(ErrorCode.CLASS_NOT_APPROVED_FOR_ENROLLMENT);
        }

        // Allow enrollment-related operations for SCHEDULED and ONGOING classes
        if (classEntity.getStatus() != ClassStatus.SCHEDULED
                && classEntity.getStatus() != ClassStatus.ONGOING) {
            throw new CustomException(ErrorCode.CLASS_NOT_SCHEDULED);
        }
    }

    private ClassListItemDTO convertToClassListItemDTO(ClassEntity classEntity) {
        // Get enrollment data
        Integer currentEnrolled = enrollmentRepository.countByClassIdAndStatus(
                classEntity.getId(), EnrollmentStatus.ENROLLED
        );
        Integer maxCapacity = classEntity.getMaxCapacity();
        Integer availableSlots = maxCapacity - currentEnrolled;
        Double utilizationRate = maxCapacity > 0 ? (double) currentEnrolled / maxCapacity * 100 : 0.0;

        // Get all teachers teaching this class
        List<TeacherSummaryDTO> teachers = getTeachersForClass(classEntity.getId());

        // Determine if enrollment is possible
        boolean canEnroll = availableSlots > 0
                && (classEntity.getStatus() == ClassStatus.SCHEDULED
                     || classEntity.getStatus() == ClassStatus.ONGOING)
                && classEntity.getApprovalStatus() == ApprovalStatus.APPROVED;

        return ClassListItemDTO.builder()
                .id(classEntity.getId())
                .code(classEntity.getCode())
                .name(classEntity.getName())
                .courseName(classEntity.getCourse().getName())
                .courseCode(classEntity.getCourse().getCode())
                .branchName(classEntity.getBranch().getName())
                .branchCode(classEntity.getBranch().getCode())
                .modality(classEntity.getModality())
                .startDate(classEntity.getStartDate())
                .plannedEndDate(classEntity.getPlannedEndDate())
                .status(classEntity.getStatus())
                .approvalStatus(classEntity.getApprovalStatus())
                .maxCapacity(maxCapacity)
                .currentEnrolled(currentEnrolled)
                .availableSlots(availableSlots)
                .utilizationRate(utilizationRate)
                .teachers(teachers)
                .scheduleSummary(formatScheduleSummary(classEntity.getScheduleDays()))
                .canEnrollStudents(canEnroll)
                .enrollmentRestrictionReason(canEnroll ? null :
                        availableSlots <= 0 ? "Class is full" :
                        classEntity.getStatus() == ClassStatus.COMPLETED ? "Class has completed" :
                        classEntity.getStatus() == ClassStatus.CANCELLED ? "Class was cancelled" :
                        "Class not available for enrollment")
                .build();
    }

    private ClassStudentDTO convertToClassStudentDTO(Enrollment enrollment) {
        Student student = enrollment.getStudent();
        UserAccount userAccount = student.getUserAccount();

        return ClassStudentDTO.builder()
                .id(enrollment.getId())
                .studentId(student.getId())
                .studentCode(student.getStudentCode())
                .fullName(userAccount.getFullName())
                .email(userAccount.getEmail())
                .phone(userAccount.getPhone())
                .branchName(student.getUserAccount().getUserBranches().iterator().next().getBranch().getName())
                .enrolledAt(enrollment.getEnrolledAt())
                .enrolledBy(enrollment.getEnrolledByUser() != null ?
                        enrollment.getEnrolledByUser().getFullName() : "System")
                .enrolledById(enrollment.getEnrolledBy())
                .status(enrollment.getStatus())
                .joinSessionId(enrollment.getJoinSessionId())
                .joinSessionDate(enrollment.getJoinSession() != null ?
                        enrollment.getJoinSession().getDate().toString() : null)
                .capacityOverride(enrollment.getCapacityOverride())
                .overrideReason(enrollment.getOverrideReason())
                .build();
    }

    private ClassDetailDTO.CourseDTO convertToCourseDTO(Course course) {
        return ClassDetailDTO.CourseDTO.builder()
                .id(course.getId())
                .code(course.getCode())
                .name(course.getName())
                .description(course.getDescription())
                .totalHours(course.getTotalHours())
                .durationWeeks(course.getDurationWeeks())
                .sessionPerWeek(course.getSessionPerWeek())
                .build();
    }

    private ClassDetailDTO.BranchDTO convertToBranchDTO(Branch branch) {
        return ClassDetailDTO.BranchDTO.builder()
                .id(branch.getId())
                .code(branch.getCode())
                .name(branch.getName())
                .address(branch.getAddress())
                .phone(branch.getPhone())
                .email(branch.getEmail())
                .build();
    }

    private ClassDetailDTO.SessionDTO convertToSessionDTO(Session session) {
        return ClassDetailDTO.SessionDTO.builder()
                .id(session.getId())
                .date(session.getDate())
                .startTime(session.getTimeSlotTemplate() != null ?
                        session.getTimeSlotTemplate().getStartTime().toString() : null)
                .endTime(session.getTimeSlotTemplate() != null ?
                        session.getTimeSlotTemplate().getEndTime().toString() : null)
                .teachers(getTeachersForSession(session))
                .room(null) // Room info not available in current entity structure
                .status(session.getStatus().name())
                .type(session.getType().name())
                .build();
    }

    private ClassDetailDTO.EnrollmentSummary calculateEnrollmentSummary(Integer currentEnrolled, Integer maxCapacity) {
        Integer availableSlots = maxCapacity - currentEnrolled;
        Double utilizationRate = maxCapacity > 0 ? (double) currentEnrolled / maxCapacity * 100 : 0.0;

        boolean canEnroll = availableSlots > 0;
        String restrictionReason = canEnroll ? null : "Class is at full capacity";

        return ClassDetailDTO.EnrollmentSummary.builder()
                .currentEnrolled(currentEnrolled)
                .maxCapacity(maxCapacity)
                .availableSlots(availableSlots)
                .utilizationRate(utilizationRate)
                .canEnrollStudents(canEnroll)
                .enrollmentRestrictionReason(restrictionReason)
                .build();
    }

    /**
     * Get all teachers teaching in a class with their session counts
     * Groups by teacher and counts how many sessions each teacher teaches
     */
    private List<TeacherSummaryDTO> getTeachersForClass(Long classId) {
        List<TeachingSlot> teachingSlots = teachingSlotRepository
                .findByClassEntityIdAndStatus(classId, TeachingSlotStatus.SCHEDULED);

        // Group by teacher and count sessions
        Map<Teacher, Long> teacherSessionCounts = teachingSlots.stream()
                .filter(slot -> slot.getTeacher() != null)
                .collect(Collectors.groupingBy(
                        TeachingSlot::getTeacher,
                        Collectors.counting()
                ));

        // Convert to DTOs sorted by session count (descending)
        return teacherSessionCounts.entrySet().stream()
                .map(entry -> {
                    Teacher teacher = entry.getKey();
                    UserAccount userAccount = teacher.getUserAccount();
                    return TeacherSummaryDTO.builder()
                            .id(userAccount.getId())
                            .teacherId(teacher.getId())
                            .fullName(userAccount.getFullName())
                            .email(userAccount.getEmail())
                            .phone(userAccount.getPhone())
                            .employeeCode(teacher.getEmployeeCode())
                            .sessionCount(entry.getValue().intValue())
                            .build();
                })
                .sorted(Comparator.comparing(TeacherSummaryDTO::getSessionCount).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Get all teachers teaching in a specific session
     */
    private List<TeacherSummaryDTO> getTeachersForSession(Session session) {
        return session.getTeachingSlots().stream()
                .filter(slot -> slot.getStatus() == TeachingSlotStatus.SCHEDULED)
                .filter(slot -> slot.getTeacher() != null)
                .map(slot -> {
                    Teacher teacher = slot.getTeacher();
                    UserAccount userAccount = teacher.getUserAccount();
                    return TeacherSummaryDTO.builder()
                            .id(userAccount.getId())
                            .teacherId(teacher.getId())
                            .fullName(userAccount.getFullName())
                            .email(userAccount.getEmail())
                            .phone(userAccount.getPhone())
                            .employeeCode(teacher.getEmployeeCode())
                            .sessionCount(1) // Single session context
                            .build();
                })
                .collect(Collectors.toList());
    }

    private String formatScheduleSummary(Short[] scheduleDays) {
        if (scheduleDays == null || scheduleDays.length == 0) {
            return "Not specified";
        }

        String[] dayNames = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        return Arrays.stream(scheduleDays)
                .filter(day -> day != null && day >= 1 && day <= 7)
                .map(day -> dayNames[day - 1])
                .collect(Collectors.joining(", "));
    }

    private AvailableStudentDTO convertToAvailableStudentDTO(
            Student student,
            ReplacementSkillAssessment assessment,
            Long classSubjectId,
            Long classLevelId
    ) {
        UserAccount userAccount = student.getUserAccount();

        // Get branch info (take first branch)
        String branchName = null;
        Long branchId = null;
        if (!userAccount.getUserBranches().isEmpty()) {
            branchId = userAccount.getUserBranches().iterator().next().getBranch().getId();
            branchName = userAccount.getUserBranches().iterator().next().getBranch().getName();
        }

        // Calculate match priority and context
        Integer matchPriority = 3; // Default: No match
        String matchingSkillLevel = null;
        String notes = "No skill assessment found for this course's subject";

        if (assessment != null && assessment.getLevel() != null) {
            Long assessmentSubjectId = assessment.getLevel().getSubject().getId();
            Long assessmentLevelId = assessment.getLevel().getId();

            if (assessmentSubjectId.equals(classSubjectId)) {
                if (assessmentLevelId.equals(classLevelId)) {
                    // Perfect match: Same subject AND same level
                    matchPriority = 1;
                    matchingSkillLevel = assessment.getLevel().getCode();
                    notes = "Perfect match - Assessment level matches class level";
                } else {
                    // Partial match: Same subject, different level
                    matchPriority = 2;
                    matchingSkillLevel = assessment.getLevel().getCode();
                    notes = "Subject match - Different level (assessed at " + matchingSkillLevel + ")";
                }
            }
        }

        // Get active enrollments count
        int activeEnrollments = enrollmentRepository.countByStudentIdAndStatus(
                student.getId(), EnrollmentStatus.ENROLLED
        );
        boolean canEnroll = activeEnrollments < 3; // Max concurrent enrollments

        return AvailableStudentDTO.builder()
                .id(student.getId())
                .studentCode(student.getStudentCode())
                .fullName(userAccount.getFullName())
                .email(userAccount.getEmail())
                .phone(userAccount.getPhone())
                .branchId(branchId)
                .branchName(branchName)
                .matchPriority(matchPriority)
                .matchingSkillLevel(matchingSkillLevel)
                .lastAssessmentDate(assessment != null ? assessment.getAssessmentDate() : null)
                .lastAssessmentScore(assessment != null ? assessment.getScore() : null)
                .activeEnrollments(activeEnrollments)
                .canEnroll(canEnroll)
                .notes(notes)
                .build();
    }
}
