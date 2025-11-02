package org.fyp.tmssep490be.services.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.enrollment.*;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.*;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.*;
import org.fyp.tmssep490be.services.EnrollmentService;
import org.fyp.tmssep490be.services.ExcelParserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnrollmentServiceImpl implements EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final ClassRepository classRepository;
    private final StudentRepository studentRepository;
    private final UserAccountRepository userAccountRepository;
    private final SessionRepository sessionRepository;
    private final StudentSessionRepository studentSessionRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserBranchesRepository userBranchesRepository;
    private final ExcelParserService excelParserService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public ClassEnrollmentImportPreview previewClassEnrollmentImport(
            Long classId,
            MultipartFile file,
            Long enrolledBy
    ) {
        log.info("Previewing enrollment import for class ID: {}", classId);

        // 1. Validate class exists và đủ điều kiện enroll
        ClassEntity classEntity = validateClassForEnrollment(classId);

        // 2. Parse Excel file
        List<StudentEnrollmentData> parsedData = excelParserService.parseStudentEnrollment(file);

        if (parsedData.isEmpty()) {
            throw new CustomException(ErrorCode.EXCEL_FILE_EMPTY);
        }

        log.info("Parsed {} students from Excel", parsedData.size());

        // 3. Resolve từng student (FOUND/CREATE/ERROR)
        resolveStudents(parsedData);

        // 4. Calculate capacity
        int currentEnrolled = enrollmentRepository.countByClassIdAndStatus(
                classId, EnrollmentStatus.ENROLLED
        );
        int maxCapacity = classEntity.getMaxCapacity();
        int availableSlots = maxCapacity - currentEnrolled;

        int validStudentsCount = (int) parsedData.stream()
                .filter(d -> d.getStatus() == StudentResolutionStatus.FOUND
                        || d.getStatus() == StudentResolutionStatus.CREATE)
                .count();

        int errorCount = (int) parsedData.stream()
                .filter(d -> d.getStatus() == StudentResolutionStatus.ERROR
                        || d.getStatus() == StudentResolutionStatus.DUPLICATE)
                .count();

        boolean exceedsCapacity = validStudentsCount > availableSlots;
        int exceededBy = exceedsCapacity ? (validStudentsCount - availableSlots) : 0;

        log.info("Capacity check: {}/{} enrolled, {} valid students, {} available slots",
                currentEnrolled, maxCapacity, validStudentsCount, availableSlots);

        // 5. Determine recommendation
        EnrollmentRecommendation recommendation = determineRecommendation(
                validStudentsCount,
                availableSlots,
                maxCapacity,
                currentEnrolled
        );

        // 6. Build warnings and errors
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        if (exceedsCapacity) {
            warnings.add(String.format(
                    "Import will exceed capacity by %d students (%d enrolled + %d new = %d/%d)",
                    exceededBy, currentEnrolled, validStudentsCount,
                    currentEnrolled + validStudentsCount, maxCapacity
            ));
        }
        if (errorCount > 0) {
            errors.add(String.format("%d students have validation errors or duplicates", errorCount));
        }

        // 7. Return preview
        return ClassEnrollmentImportPreview.builder()
                .classId(classId)
                .classCode(classEntity.getCode())
                .className(classEntity.getName())
                .students(parsedData)
                .foundCount((int) parsedData.stream().filter(d -> d.getStatus() == StudentResolutionStatus.FOUND).count())
                .createCount((int) parsedData.stream().filter(d -> d.getStatus() == StudentResolutionStatus.CREATE).count())
                .errorCount(errorCount)
                .totalValid(validStudentsCount)
                .currentEnrolled(currentEnrolled)
                .maxCapacity(maxCapacity)
                .availableSlots(availableSlots)
                .exceedsCapacity(exceedsCapacity)
                .exceededBy(exceededBy)
                .warnings(warnings)
                .errors(errors)
                .recommendation(recommendation)
                .build();
    }

    @Override
    @Transactional
    public EnrollmentResult executeClassEnrollmentImport(
            ClassEnrollmentImportExecuteRequest request,
            Long enrolledBy
    ) {
        log.info("Executing enrollment import for class ID: {} with strategy: {}",
                request.getClassId(), request.getStrategy());

        // 1. Lock class để đảm bảo consistency (tránh race condition)
        ClassEntity classEntity = classRepository.findByIdWithLock(request.getClassId())
                .orElseThrow(() -> new EntityNotFoundException("Class not found: " + request.getClassId()));

        // 2. Re-validate capacity (double-check for race condition)
        int currentEnrolled = enrollmentRepository.countByClassIdAndStatus(
                request.getClassId(), EnrollmentStatus.ENROLLED
        );

        log.info("Class locked. Current enrollment: {}/{}", currentEnrolled, classEntity.getMaxCapacity());

        // 3. Filter students theo strategy
        List<StudentEnrollmentData> studentsToEnroll = filterStudentsByStrategy(
                request,
                classEntity,
                currentEnrolled,
                enrolledBy
        );

        log.info("Filtered {} students to enroll based on strategy", studentsToEnroll.size());

        // 4. Create new students nếu cần
        List<Long> allStudentIds = new ArrayList<>();
        int studentsCreated = 0;

        for (StudentEnrollmentData data : studentsToEnroll) {
            if (data.getStatus() == StudentResolutionStatus.CREATE) {
                Student newStudent = createStudentQuick(data, classEntity.getBranch().getId());
                allStudentIds.add(newStudent.getId());
                studentsCreated++;
                log.info("Created new student: {} ({})", newStudent.getId(), data.getEmail());
            } else if (data.getStatus() == StudentResolutionStatus.FOUND) {
                allStudentIds.add(data.getResolvedStudentId());
            }
        }

        log.info("Created {} new students, total {} students to enroll", studentsCreated, allStudentIds.size());

        // 5. Determine if this is capacity override
        boolean isOverride = request.getStrategy() == EnrollmentStrategy.OVERRIDE;
        String overrideReason = isOverride ? request.getOverrideReason() : null;

        // 6. Batch enroll all students
        EnrollmentResult result = enrollStudents(
                request.getClassId(),
                allStudentIds,
                enrolledBy,
                isOverride,
                overrideReason
        );
        result.setStudentsCreated(studentsCreated);

        log.info("Enrollment completed. Enrolled: {}, Created: {}, Sessions per student: {}",
                result.getEnrolledCount(), studentsCreated, result.getSessionsGeneratedPerStudent());

        return result;
    }

    /**
     * Resolve từng student: tìm trong DB hoặc mark as CREATE
     */
    private void resolveStudents(List<StudentEnrollmentData> parsedData) {
        Set<String> seenEmails = new HashSet<>();

        for (StudentEnrollmentData data : parsedData) {
            // Skip if already has error from parsing
            if (data.getStatus() == StudentResolutionStatus.ERROR) {
                continue;
            }

            // Validate required fields
            if (data.getEmail() == null || data.getEmail().isBlank()) {
                data.setStatus(StudentResolutionStatus.ERROR);
                data.setErrorMessage("Email is required");
                continue;
            }
            if (data.getFullName() == null || data.getFullName().isBlank()) {
                data.setStatus(StudentResolutionStatus.ERROR);
                data.setErrorMessage("Full name is required");
                continue;
            }

            // Check duplicate trong file Excel
            String emailLower = data.getEmail().toLowerCase();
            if (seenEmails.contains(emailLower)) {
                data.setStatus(StudentResolutionStatus.DUPLICATE);
                data.setErrorMessage("Duplicate email in Excel file");
                continue;
            }
            seenEmails.add(emailLower);

            // Try to find by student_code
            if (data.getStudentCode() != null && !data.getStudentCode().isBlank()) {
                Optional<Student> existing = studentRepository.findByStudentCode(data.getStudentCode());
                if (existing.isPresent()) {
                    data.setStatus(StudentResolutionStatus.FOUND);
                    data.setResolvedStudentId(existing.get().getId());
                    log.debug("Found student by code: {} -> ID: {}", data.getStudentCode(), existing.get().getId());
                    continue;
                }
            }

            // Try to find by email
            Optional<UserAccount> userByEmail = userAccountRepository.findByEmail(data.getEmail());
            if (userByEmail.isPresent()) {
                Optional<Student> student = studentRepository.findByUserAccountId(userByEmail.get().getId());
                if (student.isPresent()) {
                    data.setStatus(StudentResolutionStatus.FOUND);
                    data.setResolvedStudentId(student.get().getId());
                    log.debug("Found student by email: {} -> ID: {}", data.getEmail(), student.get().getId());
                    continue;
                }
            }

            // Mark as CREATE (student mới)
            data.setStatus(StudentResolutionStatus.CREATE);
            log.debug("Student will be created: {}", data.getEmail());
        }
    }

    /**
     * Determine recommendation based on capacity
     */
    private EnrollmentRecommendation determineRecommendation(
            int toEnroll,
            int available,
            int maxCapacity,
            int currentEnrolled
    ) {
        if (toEnroll <= available) {
            // Case 1: Capacity đủ
            return EnrollmentRecommendation.builder()
                    .type(RecommendationType.OK)
                    .message("Sufficient capacity. All students can be enrolled.")
                    .suggestedEnrollCount(toEnroll)
                    .build();
        }

        int exceededBy = toEnroll - available;
        double exceededPercentage = (double) exceededBy / maxCapacity * 100;

        if (exceededPercentage <= 20) {
            // Case 2: Vượt <= 20% → suggest override
            return EnrollmentRecommendation.builder()
                    .type(RecommendationType.OVERRIDE_AVAILABLE)
                    .message(String.format(
                            "Exceeds capacity by %d students (%.1f%%). You can override with approval reason.",
                            exceededBy, exceededPercentage
                    ))
                    .suggestedEnrollCount(null)
                    .build();
        }

        if (available > 0) {
            // Case 3: Vượt > 20% nhưng vẫn còn slots → suggest partial
            return EnrollmentRecommendation.builder()
                    .type(RecommendationType.PARTIAL_SUGGESTED)
                    .message(String.format(
                            "Exceeds capacity significantly (%.1f%%). Recommend enrolling only %d students (available slots).",
                            exceededPercentage, available
                    ))
                    .suggestedEnrollCount(available)
                    .build();
        }

        // Case 4: Class đã full
        return EnrollmentRecommendation.builder()
                .type(RecommendationType.BLOCKED)
                .message("Class is full. Cannot enroll any students without capacity override.")
                .suggestedEnrollCount(0)
                .build();
    }

    /**
     * Filter students theo enrollment strategy
     */
    private List<StudentEnrollmentData> filterStudentsByStrategy(
            ClassEnrollmentImportExecuteRequest request,
            ClassEntity classEntity,
            int currentEnrolled,
            Long enrolledBy
    ) {
        List<StudentEnrollmentData> studentsToEnroll;

        switch (request.getStrategy()) {
            case ALL:
                // Enroll tất cả valid students
                studentsToEnroll = request.getStudents().stream()
                        .filter(s -> s.getStatus() == StudentResolutionStatus.FOUND
                                || s.getStatus() == StudentResolutionStatus.CREATE)
                        .collect(Collectors.toList());

                // Validate capacity
                if (currentEnrolled + studentsToEnroll.size() > classEntity.getMaxCapacity()) {
                    throw new CustomException(ErrorCode.CLASS_CAPACITY_EXCEEDED);
                }
                break;

            case PARTIAL:
                // Enroll chỉ selected students
                if (request.getSelectedStudentIds() == null || request.getSelectedStudentIds().isEmpty()) {
                    throw new CustomException(ErrorCode.PARTIAL_STRATEGY_MISSING_IDS);
                }

                Set<Long> selectedIds = new HashSet<>(request.getSelectedStudentIds());
                studentsToEnroll = request.getStudents().stream()
                        .filter(s -> (s.getStatus() == StudentResolutionStatus.FOUND
                                && selectedIds.contains(s.getResolvedStudentId()))
                                || (s.getStatus() == StudentResolutionStatus.CREATE
                                && selectedIds.contains(Long.valueOf(s.getEmail().hashCode())))) // Temp ID for CREATE
                        .collect(Collectors.toList());

                // Validate capacity
                if (currentEnrolled + studentsToEnroll.size() > classEntity.getMaxCapacity()) {
                    throw new CustomException(ErrorCode.SELECTED_STUDENTS_EXCEED_CAPACITY);
                }
                break;

            case OVERRIDE:
                // Override capacity và enroll tất cả
                if (request.getOverrideReason() == null || request.getOverrideReason().length() < 20) {
                    throw new CustomException(ErrorCode.OVERRIDE_REASON_REQUIRED);
                }

                studentsToEnroll = request.getStudents().stream()
                        .filter(s -> s.getStatus() == StudentResolutionStatus.FOUND
                                || s.getStatus() == StudentResolutionStatus.CREATE)
                        .collect(Collectors.toList());

                log.warn("CAPACITY_OVERRIDE: Class {} will enroll {} students (capacity: {}). Reason: {}. Approved by user {}",
                        request.getClassId(), studentsToEnroll.size(), classEntity.getMaxCapacity(),
                        request.getOverrideReason(), enrolledBy);
                break;

            default:
                throw new CustomException(ErrorCode.INVALID_ENROLLMENT_STRATEGY);
        }

        return studentsToEnroll;
    }

    /**
     * Core enrollment logic - batch enroll students vào class
     *
     * @param classId Class ID
     * @param studentIds List of student IDs to enroll
     * @param enrolledBy User ID performing enrollment
     * @param capacityOverride True if this enrollment exceeds capacity
     * @param overrideReason Reason for override (required if capacityOverride = true)
     */
    @Transactional
    public EnrollmentResult enrollStudents(
            Long classId,
            List<Long> studentIds,
            Long enrolledBy,
            boolean capacityOverride,
            String overrideReason
    ) {
        log.info("Enrolling {} students into class {} (override: {})", studentIds.size(), classId, capacityOverride);

        // 1. Validate class
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new EntityNotFoundException("Class not found: " + classId));

        // 2. Get all future sessions của class
        List<Session> futureSessions = sessionRepository.findByClassEntityIdAndDateGreaterThanEqualAndStatusOrderByDateAsc(
                classId,
                LocalDate.now(),
                SessionStatus.PLANNED
        );

        if (futureSessions.isEmpty()) {
            throw new CustomException(ErrorCode.NO_FUTURE_SESSIONS);
        }

        log.info("Found {} future sessions for class", futureSessions.size());

        // 3. Batch insert enrollments
        List<Enrollment> enrollments = new ArrayList<>();
        for (Long studentId : studentIds) {
            // Check duplicate enrollment
            boolean alreadyEnrolled = enrollmentRepository.existsByClassIdAndStudentIdAndStatus(
                    classId, studentId, EnrollmentStatus.ENROLLED
            );
            if (alreadyEnrolled) {
                log.warn("Student {} is already enrolled in class {}", studentId, classId);
                throw new CustomException(ErrorCode.ENROLLMENT_ALREADY_EXISTS);
            }

            Enrollment enrollment = Enrollment.builder()
                    .classId(classId)
                    .studentId(studentId)
                    .status(EnrollmentStatus.ENROLLED)
                    .enrolledAt(OffsetDateTime.now())
                    .enrolledBy(enrolledBy)
                    .capacityOverride(capacityOverride)
                    .overrideReason(overrideReason)
                    .build();

            // Mid-course enrollment: track join_session_id
            if (LocalDate.now().isAfter(classEntity.getStartDate())) {
                Session firstFutureSession = futureSessions.get(0);
                enrollment.setJoinSessionId(firstFutureSession.getId());
                log.debug("Mid-course enrollment for student {}. Join session: {}",
                        studentId, firstFutureSession.getId());
            }

            enrollments.add(enrollment);
        }
        enrollmentRepository.saveAll(enrollments);

        log.info("Saved {} enrollment records", enrollments.size());

        // 4. Auto-generate student_session records
        List<StudentSession> studentSessions = new ArrayList<>();
        for (Enrollment enrollment : enrollments) {
            for (Session session : futureSessions) {
                StudentSession.StudentSessionId ssId = new StudentSession.StudentSessionId();
                ssId.setStudentId(enrollment.getStudentId());
                ssId.setSessionId(session.getId());

                StudentSession ss = new StudentSession();
                ss.setId(ssId);
                ss.setAttendanceStatus(AttendanceStatus.PLANNED);
                ss.setIsMakeup(false);
                studentSessions.add(ss);
            }
        }
        studentSessionRepository.saveAll(studentSessions);

        log.info("Generated {} student_session records ({} sessions per student)",
                studentSessions.size(), futureSessions.size());

        // 5. Send welcome emails (async) - COMMENTED OUT for future implementation
        // for (Long studentId : studentIds) {
        //     emailService.sendEnrollmentConfirmation(studentId, classId);
        // }
        log.info("Email sending skipped (not implemented yet)");

        // 6. Return result
        List<String> warnings = new ArrayList<>();
        if (LocalDate.now().isAfter(classEntity.getStartDate())) {
            warnings.add("Mid-course enrollment: Students will only be enrolled in future sessions");
        }

        return EnrollmentResult.builder()
                .enrolledCount(enrollments.size())
                .sessionsGeneratedPerStudent(futureSessions.size())
                .totalStudentSessionsCreated(studentSessions.size())
                .warnings(warnings)
                .build();
    }

    /**
     * Create student nhanh từ Excel data
     */
    private Student createStudentQuick(StudentEnrollmentData data, Long branchId) {
        log.info("Creating new student: {}", data.getEmail());

        // 1. Create user_account
        UserAccount user = new UserAccount();
        user.setEmail(data.getEmail());
        user.setFullName(data.getFullName());
        user.setPhone(data.getPhone());
        user.setGender(data.getGender());
        user.setDob(data.getDob());
        user.setStatus(UserStatus.ACTIVE);
        user.setPasswordHash(passwordEncoder.encode(generateTemporaryPassword()));
        UserAccount savedUser = userAccountRepository.save(user);

        log.debug("Created user_account: ID {}", savedUser.getId());

        // 2. Create student
        Student student = new Student();
        student.setUserAccount(savedUser);
        student.setStudentCode(generateStudentCode(branchId));
        student.setLevel(data.getLevel());
        Student savedStudent = studentRepository.save(student);

        log.debug("Created student: ID {}, Code {}", savedStudent.getId(), savedStudent.getStudentCode());

        // 3. Assign STUDENT role
        Role studentRole = roleRepository.findByCode("STUDENT")
                .orElseThrow(() -> new EntityNotFoundException("STUDENT role not found"));

        UserRole.UserRoleId userRoleId = new UserRole.UserRoleId();
        userRoleId.setUserId(savedUser.getId());
        userRoleId.setRoleId(studentRole.getId());

        UserRole userRole = new UserRole();
        userRole.setId(userRoleId);
        userRole.setUserAccount(savedUser);
        userRole.setRole(studentRole);
        userRoleRepository.save(userRole);

        log.debug("Assigned STUDENT role to user {}", savedUser.getId());

        // 4. Assign to branch
        Branch branch = new Branch();
        branch.setId(branchId);

        UserBranches.UserBranchesId userBranchId = new UserBranches.UserBranchesId();
        userBranchId.setUserId(savedUser.getId());
        userBranchId.setBranchId(branchId);

        UserBranches userBranch = new UserBranches();
        userBranch.setId(userBranchId);
        userBranch.setUserAccount(savedUser);
        userBranch.setBranch(branch);
        // Note: assignedBy should be the enrolledBy user, but we don't have it here
        // This is acceptable as the enrollment record tracks enrolled_by
        userBranchesRepository.save(userBranch);

        log.debug("Assigned user {} to branch {}", savedUser.getId(), branchId);

        return savedStudent;
    }

    /**
     * Validate class có đủ điều kiện để enroll không
     */
    private ClassEntity validateClassForEnrollment(Long classId) {
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new EntityNotFoundException("Class not found: " + classId));

        if (!classEntity.getApprovalStatus().equals(ApprovalStatus.APPROVED)) {
            throw new CustomException(ErrorCode.CLASS_NOT_APPROVED);
        }

        if (!classEntity.getStatus().equals(ClassStatus.SCHEDULED)) {
            throw new CustomException(ErrorCode.CLASS_INVALID_STATUS);
        }

        return classEntity;
    }

    /**
     * Generate temporary password cho student mới
     */
    private String generateTemporaryPassword() {
        // Generate random 8-character password
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder password = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 8; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        return password.toString();
    }

    /**
     * Generate student code
     * Format: ST{branchId}{timestamp}
     */
    private String generateStudentCode(Long branchId) {
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(8);
        return String.format("ST%d%s", branchId, timestamp);
    }
}
