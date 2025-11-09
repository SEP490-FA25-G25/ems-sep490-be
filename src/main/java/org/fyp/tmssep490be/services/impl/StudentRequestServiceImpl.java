package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.studentrequest.*;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.*;
import org.fyp.tmssep490be.exceptions.BusinessRuleException;
import org.fyp.tmssep490be.exceptions.DuplicateRequestException;
import org.fyp.tmssep490be.exceptions.ResourceNotFoundException;
import org.fyp.tmssep490be.repositories.*;
import org.fyp.tmssep490be.services.StudentRequestService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentRequestServiceImpl implements StudentRequestService {

    private final StudentRequestRepository studentRequestRepository;
    private final StudentRepository studentRepository;
    private final ClassRepository classRepository;
    private final SessionRepository sessionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentSessionRepository studentSessionRepository;
    private final UserAccountRepository userAccountRepository;

    // Configuration values (in real implementation, these would come from properties)
    private static final int LEAD_TIME_DAYS = 1;
    private static final double ABSENCE_THRESHOLD_PERCENT = 20.0;
    private static final int REASON_MIN_LENGTH = 10;

    @Override
    public Page<StudentRequestResponseDTO> getMyRequests(Long userId, RequestFilterDTO filter) {
        // Lookup actual student ID from user_account ID
        Student student = studentRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found for user ID: " + userId));

        List<RequestStatus> statuses = filter.getStatus() != null ?
                List.of(RequestStatus.valueOf(filter.getStatus())) :
                List.of(RequestStatus.values());

        Sort sort = Sort.by(Sort.Direction.fromString(filter.getSort().split(",")[1]),
                filter.getSort().split(",")[0]);
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), sort);

        Page<StudentRequest> requests = studentRequestRepository.findByStudentIdAndStatusIn(
                student.getId(), statuses, pageable);

        return requests.map(this::mapToStudentResponseDTO);
    }

    @Override
    public StudentRequestDetailDTO getRequestById(Long requestId, Long userId) {
        // Lookup actual student ID from user_account ID
        Student student = studentRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found for user ID: " + userId));

        StudentRequest request = studentRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found with id: " + requestId));

        if (!request.getStudent().getId().equals(student.getId())) {
            throw new BusinessRuleException("ACCESS_DENIED", "You can only view your own requests");
        }

        return mapToDetailDTO(request);
    }

    @Override
    @Transactional
    public StudentRequestResponseDTO submitAbsenceRequest(Long userId, AbsenceRequestDTO dto) {
        log.info("Submitting absence request for user {} and session {}", userId, dto.getTargetSessionId());

        // 0. Lookup actual student ID from user_account ID
        Student student = studentRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found for user ID: " + userId));

        log.info("Resolved user ID {} to student ID {}", userId, student.getId());

        // 1. Validate session exists and is future
        Session session = sessionRepository.findById(dto.getTargetSessionId())
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        if (session.getDate().isBefore(LocalDate.now())) {
            throw new BusinessRuleException("PAST_SESSION", "Cannot request absence for past sessions");
        }

        if (!session.getStatus().equals(SessionStatus.PLANNED)) {
            throw new BusinessRuleException("INVALID_SESSION_STATUS", "Session must be in PLANNED status");
        }

        // 2. Validate student enrollment
        Enrollment enrollment = enrollmentRepository
                .findByStudentIdAndClassIdAndStatus(student.getId(), dto.getCurrentClassId(), EnrollmentStatus.ENROLLED);

        if (enrollment == null) {
            throw new BusinessRuleException("NOT_ENROLLED", "You are not enrolled in this class");
        }

        // 3. Check duplicate request
        if (hasDuplicateRequest(student.getId(), dto.getTargetSessionId(), StudentRequestType.ABSENCE)) {
            throw new DuplicateRequestException("Duplicate absence request for this session");
        }

        // 4. Check lead time (warning only)
        long daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), session.getDate());
        if (daysUntil < LEAD_TIME_DAYS) {
            log.warn("Absence request submitted with insufficient lead time: {} days", daysUntil);
        }

        // 5. Check absence threshold (warning only)
        double absenceRate = calculateAbsenceRate(student.getId(), dto.getCurrentClassId());
        if (absenceRate > ABSENCE_THRESHOLD_PERCENT) {
            log.warn("Student absence rate {}% exceeds threshold", absenceRate);
        }

        // 6. Get entities
        ClassEntity classEntity = classRepository.findById(dto.getCurrentClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        UserAccount submittedBy = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 7. Create request
        StudentRequest request = StudentRequest.builder()
                .student(student)
                .requestType(StudentRequestType.ABSENCE)
                .currentClass(classEntity)
                .targetSession(session)
                .requestReason(dto.getRequestReason())
                .note(dto.getNote())
                .status(RequestStatus.PENDING)
                .submittedBy(submittedBy)
                .submittedAt(OffsetDateTime.now())
                .build();

        request = studentRequestRepository.save(request);
        log.info("Absence request created successfully with id: {}", request.getId());

        // TODO: Send notification to Academic Affairs

        return mapToStudentResponseDTO(request);
    }

    @Override
    @Transactional
    public StudentRequestResponseDTO cancelRequest(Long requestId, Long userId) {
        // Lookup actual student ID from user_account ID
        Student student = studentRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found for user ID: " + userId));

        StudentRequest request = studentRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));

        if (!request.getStudent().getId().equals(student.getId())) {
            throw new BusinessRuleException("ACCESS_DENIED", "You can only cancel your own requests");
        }

        if (!request.getStatus().equals(RequestStatus.PENDING)) {
            throw new BusinessRuleException("INVALID_STATUS", "Only pending requests can be cancelled");
        }

        request.setStatus(RequestStatus.CANCELLED);
        request = studentRequestRepository.save(request);

        log.info("Request {} cancelled by student {} (user {})", requestId, student.getId(), userId);
        return mapToStudentResponseDTO(request);
    }

    @Override
    public List<SessionAvailabilityDTO> getAvailableSessionsForDate(Long userId, LocalDate date, StudentRequestType requestType) {
        // Lookup actual student ID from user_account ID
        Student student = studentRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found for user ID: " + userId));

        // Find all sessions for the student's classes on the given date
        List<Session> sessions = sessionRepository.findSessionsForStudentByDate(student.getId(), date);

        return sessions.stream()
                .collect(Collectors.groupingBy(session -> session.getClassEntity()))
                .entrySet().stream()
                .map(entry -> {
                    ClassEntity classEntity = entry.getKey();
                    List<Session> classSessions = entry.getValue();

                    return SessionAvailabilityDTO.builder()
                            .classId(classEntity.getId())
                            .classCode(classEntity.getCode())
                            .className(classEntity.getName())
                            .courseId(classEntity.getCourse().getId())
                            .courseName(classEntity.getCourse().getName())
                            .branchId(classEntity.getBranch().getId())
                            .branchName(classEntity.getBranch().getName())
                            .modality(classEntity.getModality().toString())
                            .sessionCount(classSessions.size())
                            .sessions(classSessions.stream()
                                    .map(this::mapToSessionDTO)
                                    .collect(Collectors.toList()))
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public Page<AARequestResponseDTO> getPendingRequests(AARequestFilterDTO filter) {
        Sort sort = Sort.by(Sort.Direction.fromString(filter.getSort().split(",")[1]),
                filter.getSort().split(",")[0]);

        // Fetch all pending requests without pagination for filtering
        List<StudentRequest> allRequests = studentRequestRepository.findByStatus(RequestStatus.PENDING, sort);

        // Apply filtering
        List<StudentRequest> filteredRequests = allRequests.stream()
                .filter(request -> {
                    // Filter by request type
                    if (filter.getRequestType() != null) {
                        StudentRequestType requestType = StudentRequestType.valueOf(filter.getRequestType());
                        if (!request.getRequestType().equals(requestType)) {
                            return false;
                        }
                    }

                    // Filter by branch
                    if (filter.getBranchId() != null && request.getCurrentClass() != null) {
                        if (!request.getCurrentClass().getBranch().getId().equals(filter.getBranchId())) {
                            return false;
                        }
                    }

                    // Filter by student name
                    if (filter.getStudentName() != null && !filter.getStudentName().trim().isEmpty()) {
                        String searchLower = filter.getStudentName().toLowerCase();
                        boolean matchesName = (request.getStudent().getUserAccount().getFullName() != null &&
                                request.getStudent().getUserAccount().getFullName().toLowerCase().contains(searchLower)) ||
                                (request.getStudent().getStudentCode() != null &&
                                request.getStudent().getStudentCode().toLowerCase().contains(searchLower));
                        if (!matchesName) {
                            return false;
                        }
                    }

                    // Filter by class code
                    if (filter.getClassCode() != null && !filter.getClassCode().trim().isEmpty()) {
                        if (request.getCurrentClass() == null || request.getCurrentClass().getCode() == null ||
                                !request.getCurrentClass().getCode().toLowerCase().contains(filter.getClassCode().toLowerCase())) {
                            return false;
                        }
                    }

                    // Filter by session date
                    if (filter.getSessionDateFrom() != null && request.getTargetSession() != null) {
                        LocalDate sessionDateFrom = LocalDate.parse(filter.getSessionDateFrom());
                        if (request.getTargetSession().getDate().isBefore(sessionDateFrom)) {
                            return false;
                        }
                    }

                    if (filter.getSessionDateTo() != null && request.getTargetSession() != null) {
                        LocalDate sessionDateTo = LocalDate.parse(filter.getSessionDateTo());
                        if (request.getTargetSession().getDate().isAfter(sessionDateTo)) {
                            return false;
                        }
                    }

                    return true;
                })
                .collect(Collectors.toList());

        // Calculate pagination manually
        int start = Math.min(filter.getPage() * filter.getSize(), filteredRequests.size());
        int end = Math.min(start + filter.getSize(), filteredRequests.size());
        List<StudentRequest> paginatedRequests = filteredRequests.subList(start, end);

        // Create properly paginated result
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), sort);
        return new org.springframework.data.domain.PageImpl<>(
                paginatedRequests.stream().map(this::mapToAAResponseDTO).collect(Collectors.toList()),
                pageable,
                filteredRequests.size() // Correct total count
        );
    }

    @Override
    public Page<AARequestResponseDTO> getAllRequests(AARequestFilterDTO filter) {
        Sort sort = Sort.by(Sort.Direction.fromString(filter.getSort().split(",")[1]),
                filter.getSort().split(",")[0]);

        // Fetch all requests without pagination for proper filtering
        List<StudentRequest> allRequests;
        RequestStatus status = filter.getStatus() != null ?
                RequestStatus.valueOf(filter.getStatus()) : null;

        if (status != null) {
            allRequests = studentRequestRepository.findByStatus(status, sort);
        } else {
            allRequests = studentRequestRepository.findAll(sort);
        }

        // Apply additional filtering in service layer
        List<StudentRequest> filteredRequests = allRequests.stream()
                .filter(request -> {
                    // Filter by request type
                    if (filter.getRequestType() != null) {
                        StudentRequestType requestType = StudentRequestType.valueOf(filter.getRequestType());
                        if (!request.getRequestType().equals(requestType)) {
                            return false;
                        }
                    }

                    // Filter by branch
                    if (filter.getBranchId() != null && request.getCurrentClass() != null) {
                        if (!request.getCurrentClass().getBranch().getId().equals(filter.getBranchId())) {
                            return false;
                        }
                    }

                    // Filter by student name
                    if (filter.getStudentName() != null && !filter.getStudentName().trim().isEmpty()) {
                        String searchLower = filter.getStudentName().toLowerCase();
                        boolean matchesName = (request.getStudent().getUserAccount().getFullName() != null &&
                                request.getStudent().getUserAccount().getFullName().toLowerCase().contains(searchLower)) ||
                                (request.getStudent().getStudentCode() != null &&
                                request.getStudent().getStudentCode().toLowerCase().contains(searchLower));
                        if (!matchesName) {
                            return false;
                        }
                    }

                    // Filter by class code
                    if (filter.getClassCode() != null && !filter.getClassCode().trim().isEmpty()) {
                        if (request.getCurrentClass() == null || request.getCurrentClass().getCode() == null ||
                                !request.getCurrentClass().getCode().toLowerCase().contains(filter.getClassCode().toLowerCase())) {
                            return false;
                        }
                    }

                    // Filter by decided by
                    if (filter.getDecidedBy() != null && request.getDecidedBy() != null) {
                        if (!request.getDecidedBy().getId().equals(filter.getDecidedBy())) {
                            return false;
                        }
                    }

                    // Filter by session date
                    if (filter.getSessionDateFrom() != null && request.getTargetSession() != null) {
                        LocalDate sessionDateFrom = LocalDate.parse(filter.getSessionDateFrom());
                        if (request.getTargetSession().getDate().isBefore(sessionDateFrom)) {
                            return false;
                        }
                    }

                    if (filter.getSessionDateTo() != null && request.getTargetSession() != null) {
                        LocalDate sessionDateTo = LocalDate.parse(filter.getSessionDateTo());
                        if (request.getTargetSession().getDate().isAfter(sessionDateTo)) {
                            return false;
                        }
                    }

                    // Filter by submitted date
                    if (filter.getSubmittedDateFrom() != null) {
                        LocalDate submittedDateFrom = LocalDate.parse(filter.getSubmittedDateFrom());
                        if (request.getSubmittedAt().toLocalDate().isBefore(submittedDateFrom)) {
                            return false;
                        }
                    }

                    if (filter.getSubmittedDateTo() != null) {
                        LocalDate submittedDateTo = LocalDate.parse(filter.getSubmittedDateTo());
                        if (request.getSubmittedAt().toLocalDate().isAfter(submittedDateTo)) {
                            return false;
                        }
                    }

                    return true;
                })
                .collect(Collectors.toList());

        // Calculate pagination manually
        int start = Math.min(filter.getPage() * filter.getSize(), filteredRequests.size());
        int end = Math.min(start + filter.getSize(), filteredRequests.size());
        List<StudentRequest> paginatedRequests = filteredRequests.subList(start, end);

        // Create properly paginated result with correct total count
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), sort);
        return new org.springframework.data.domain.PageImpl<>(
                paginatedRequests.stream().map(this::mapToAAResponseDTO).collect(Collectors.toList()),
                pageable,
                filteredRequests.size() // Correct total count
        );
    }

    @Override
    public StudentRequestDetailDTO getRequestDetailsForAA(Long requestId) {
        StudentRequest request = studentRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found with id: " + requestId));

        StudentRequestDetailDTO detailDTO = mapToDetailDTO(request);

        // Add additional info for AA
        if (request.getTargetSession() != null) {
            long daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), request.getTargetSession().getDate());
            double absenceRate = calculateAbsenceRate(request.getStudent().getId(), request.getCurrentClass().getId());

            StudentRequestDetailDTO.AdditionalInfoDTO additionalInfo = StudentRequestDetailDTO.AdditionalInfoDTO.builder()
                    .daysUntilSession(daysUntil)
                    .studentAbsenceStats(calculateAbsenceStats(request.getStudent().getId(), request.getCurrentClass().getId()))
                    .previousRequests(calculatePreviousRequests(request.getStudent().getId()))
                    .build();

            detailDTO.setAdditionalInfo(additionalInfo);
        }

        return detailDTO;
    }

    @Override
    @Transactional
    public StudentRequestResponseDTO approveRequest(Long requestId, Long decidedById, ApprovalDTO dto) {
        StudentRequest request = studentRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));

        if (!request.getStatus().equals(RequestStatus.PENDING)) {
            throw new BusinessRuleException("INVALID_STATUS", "Only pending requests can be approved");
        }

        // Update request status
        UserAccount decidedBy = userAccountRepository.findById(decidedById)
                .orElseThrow(() -> new ResourceNotFoundException("Deciding user not found"));

        request.setStatus(RequestStatus.APPROVED);
        request.setDecidedBy(decidedBy);
        request.setDecidedAt(OffsetDateTime.now());
        request.setNote(dto.getNote());
        request = studentRequestRepository.save(request);

        // If it's an absence request, update student_session attendance
        if (request.getRequestType().equals(StudentRequestType.ABSENCE) && request.getTargetSession() != null) {
            Optional<StudentSession> studentSession = studentSessionRepository
                    .findById(new StudentSession.StudentSessionId(request.getStudent().getId(), request.getTargetSession().getId()));

            if (studentSession.isPresent()) {
                StudentSession ss = studentSession.get();
                ss.setAttendanceStatus(AttendanceStatus.ABSENT);
                ss.setNote(String.format("Excused absence approved on %s. Request ID: %d",
                        OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), requestId));
                studentSessionRepository.save(ss);
            }
        }

        log.info("Request {} approved by user {}", requestId, decidedById);
        // TODO: Send notification to student

        return mapToStudentResponseDTO(request);
    }

    @Override
    @Transactional
    public StudentRequestResponseDTO rejectRequest(Long requestId, Long decidedById, RejectionDTO dto) {
        StudentRequest request = studentRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));

        if (!request.getStatus().equals(RequestStatus.PENDING)) {
            throw new BusinessRuleException("INVALID_STATUS", "Only pending requests can be rejected");
        }

        UserAccount decidedBy = userAccountRepository.findById(decidedById)
                .orElseThrow(() -> new ResourceNotFoundException("Deciding user not found"));

        request.setStatus(RequestStatus.REJECTED);
        request.setDecidedBy(decidedBy);
        request.setDecidedAt(OffsetDateTime.now());
        request.setNote(dto.getRejectionReason());
        request = studentRequestRepository.save(request);

        log.info("Request {} rejected by user {}", requestId, decidedById);
        // TODO: Send notification to student

        return mapToStudentResponseDTO(request);
    }

    @Override
    public RequestSummaryDTO getRequestSummary(AARequestFilterDTO filter) {
        long totalPending = studentRequestRepository.countByStatus(RequestStatus.PENDING);

        // Count urgent requests (sessions in next 2 days)
        LocalDate twoDaysFromNow = LocalDate.now().plusDays(2);
        long urgentCount = studentRequestRepository.countByStatus(RequestStatus.PENDING); // Simplified, would need date filtering

        long absenceRequests = studentRequestRepository.countByRequestTypeAndStatus(
                StudentRequestType.ABSENCE, RequestStatus.PENDING);
        long makeupRequests = studentRequestRepository.countByRequestTypeAndStatus(
                StudentRequestType.MAKEUP, RequestStatus.PENDING);
        long transferRequests = studentRequestRepository.countByRequestTypeAndStatus(
                StudentRequestType.TRANSFER, RequestStatus.PENDING);

        return RequestSummaryDTO.builder()
                .totalPending((int) totalPending)
                .needsUrgentReview((int) urgentCount)
                .absenceRequests((int) absenceRequests)
                .makeupRequests((int) makeupRequests)
                .transferRequests((int) transferRequests)
                .build();
    }

    @Override
    public boolean hasDuplicateRequest(Long studentId, Long sessionId, StudentRequestType requestType) {
        return studentRequestRepository.existsByStudentIdAndTargetSessionIdAndRequestTypeAndStatusIn(
                studentId, sessionId, requestType,
                List.of(RequestStatus.PENDING, RequestStatus.APPROVED));
    }

    @Override
    public double calculateAbsenceRate(Long studentId, Long classId) {
        // Simplified calculation - in real implementation, would count actual absences
        List<StudentSession> sessions = studentSessionRepository.findByStudentIdAndClassEntityId(studentId, classId);
        if (sessions.isEmpty()) {
            return 0.0;
        }

        long absenceCount = sessions.stream()
                .filter(ss -> ss.getAttendanceStatus() == AttendanceStatus.ABSENT)
                .count();

        return (double) absenceCount / sessions.size() * 100;
    }

    // Helper methods for mapping entities to DTOs
    private StudentRequestResponseDTO mapToStudentResponseDTO(StudentRequest request) {
        // When rejected, the note contains the rejection reason. When approved, it contains approval note.
        String rejectionReason = request.getStatus() == RequestStatus.REJECTED ? request.getNote() : null;

        return StudentRequestResponseDTO.builder()
                .id(request.getId())
                .requestType(request.getRequestType().toString())
                .status(request.getStatus().toString())
                .currentClass(mapToClassSummaryDTO(request.getCurrentClass()))
                .targetSession(mapToSessionSummaryDTO(request.getTargetSession()))
                .requestReason(request.getRequestReason())
                .note(request.getNote())
                .submittedAt(request.getSubmittedAt())
                .submittedBy(mapToUserSummaryDTO(request.getSubmittedBy()))
                .decidedAt(request.getDecidedAt())
                .decidedBy(mapToUserSummaryDTO(request.getDecidedBy()))
                .rejectionReason(rejectionReason)
                .build();
    }

    private AARequestResponseDTO mapToAAResponseDTO(StudentRequest request) {
        String rejectionReason = request.getStatus() == RequestStatus.REJECTED ? request.getNote() : null;

        return AARequestResponseDTO.builder()
                .id(request.getId())
                .requestType(request.getRequestType().toString())
                .status(request.getStatus().toString())
                .student(mapToStudentSummaryDTO(request.getStudent()))
                .currentClass(mapToAAClassSummaryDTO(request.getCurrentClass()))
                .targetSession(mapToAASessionSummaryDTO(request.getTargetSession()))
                .requestReason(request.getRequestReason())
                .note(request.getNote())
                .submittedAt(request.getSubmittedAt())
                .submittedBy(mapToAAUserSummaryDTO(request.getSubmittedBy()))
                .decidedAt(request.getDecidedAt())
                .decidedBy(mapToAAUserSummaryDTO(request.getDecidedBy()))
                .rejectionReason(rejectionReason)
                .daysUntilSession(request.getTargetSession() != null ?
                        ChronoUnit.DAYS.between(LocalDate.now(), request.getTargetSession().getDate()) : null)
                .studentAbsenceRate(request.getCurrentClass() != null ?
                        calculateAbsenceRate(request.getStudent().getId(), request.getCurrentClass().getId()) : null)
                .build();
    }

    private StudentRequestDetailDTO mapToDetailDTO(StudentRequest request) {
        String rejectionReason = request.getStatus() == RequestStatus.REJECTED ? request.getNote() : null;

        return StudentRequestDetailDTO.builder()
                .id(request.getId())
                .requestType(request.getRequestType().toString())
                .status(request.getStatus().toString())
                .student(mapToDetailStudentSummaryDTO(request.getStudent()))
                .currentClass(mapToDetailClassDTO(request.getCurrentClass()))
                .targetSession(mapToDetailSessionDTO(request.getTargetSession()))
                .requestReason(request.getRequestReason())
                .note(request.getNote())
                .submittedAt(request.getSubmittedAt())
                .submittedBy(mapToDetailUserSummaryDTO(request.getSubmittedBy()))
                .decidedAt(request.getDecidedAt())
                .decidedBy(mapToDetailUserSummaryDTO(request.getDecidedBy()))
                .rejectionReason(rejectionReason)
                .build();
    }

    // Various mapping helper methods
    private StudentRequestResponseDTO.ClassSummaryDTO mapToClassSummaryDTO(ClassEntity classEntity) {
        if (classEntity == null) return null;
        return StudentRequestResponseDTO.ClassSummaryDTO.builder()
                .id(classEntity.getId())
                .code(classEntity.getCode())
                .name(classEntity.getName())
                .build();
    }

    private StudentRequestResponseDTO.SessionSummaryDTO mapToSessionSummaryDTO(Session session) {
        if (session == null) return null;
        return StudentRequestResponseDTO.SessionSummaryDTO.builder()
                .id(session.getId())
                .date(session.getDate().format(DateTimeFormatter.ISO_DATE))
                .courseSessionNumber(session.getCourseSession() != null ? session.getCourseSession().getSequenceNo() : null)
                .courseSessionTitle(session.getCourseSession() != null ? session.getCourseSession().getTopic() : null)
                .timeSlot(mapToTimeSlotSummaryDTO(session.getTimeSlotTemplate()))
                .build();
    }

    private StudentRequestResponseDTO.TimeSlotSummaryDTO mapToTimeSlotSummaryDTO(TimeSlotTemplate timeSlot) {
        if (timeSlot == null) return null;
        return StudentRequestResponseDTO.TimeSlotSummaryDTO.builder()
                .startTime(timeSlot.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")))
                .endTime(timeSlot.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm")))
                .build();
    }

    private AARequestResponseDTO.TimeSlotSummaryDTO mapToAATimeSlotSummaryDTO(TimeSlotTemplate timeSlot) {
        if (timeSlot == null) return null;
        return AARequestResponseDTO.TimeSlotSummaryDTO.builder()
                .startTime(timeSlot.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")))
                .endTime(timeSlot.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm")))
                .build();
    }

    private StudentRequestResponseDTO.UserSummaryDTO mapToUserSummaryDTO(UserAccount user) {
        if (user == null) return null;
        return StudentRequestResponseDTO.UserSummaryDTO.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .build();
    }

    private AARequestResponseDTO.UserSummaryDTO mapToAAUserSummaryDTO(UserAccount user) {
        if (user == null) return null;
        return AARequestResponseDTO.UserSummaryDTO.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .build();
    }

    private AARequestResponseDTO.StudentSummaryDTO mapToStudentSummaryDTO(Student student) {
        if (student == null) return null;
        return AARequestResponseDTO.StudentSummaryDTO.builder()
                .id(student.getId())
                .studentCode(student.getStudentCode())
                .fullName(student.getUserAccount().getFullName())
                .email(student.getUserAccount().getEmail())
                .phone(student.getUserAccount().getPhone())
                .build();
    }

    private AARequestResponseDTO.ClassSummaryDTO mapToAAClassSummaryDTO(ClassEntity classEntity) {
        if (classEntity == null) return null;
        return AARequestResponseDTO.ClassSummaryDTO.builder()
                .id(classEntity.getId())
                .code(classEntity.getCode())
                .name(classEntity.getName())
                .branch(mapToBranchSummaryDTO(classEntity.getBranch()))
                .build();
    }

    private AARequestResponseDTO.BranchSummaryDTO mapToBranchSummaryDTO(Branch branch) {
        if (branch == null) return null;
        return AARequestResponseDTO.BranchSummaryDTO.builder()
                .id(branch.getId())
                .name(branch.getName())
                .build();
    }

    private AARequestResponseDTO.SessionSummaryDTO mapToAASessionSummaryDTO(Session session) {
        if (session == null) return null;
        return AARequestResponseDTO.SessionSummaryDTO.builder()
                .id(session.getId())
                .date(session.getDate().format(DateTimeFormatter.ISO_DATE))
                .dayOfWeek(session.getDate().getDayOfWeek().toString())
                .courseSessionNumber(session.getCourseSession() != null ? session.getCourseSession().getSequenceNo() : null)
                .courseSessionTitle(session.getCourseSession() != null ? session.getCourseSession().getTopic() : null)
                .timeSlot(mapToAATimeSlotSummaryDTO(session.getTimeSlotTemplate()))
                .status(session.getStatus().toString())
                .teacher(null) // Teacher mapping to be implemented via TeachingSlot relationship
                .build();
    }

    private AARequestResponseDTO.TeacherSummaryDTO mapToTeacherSummaryDTO(Teacher teacher) {
        if (teacher == null) return null;
        return AARequestResponseDTO.TeacherSummaryDTO.builder()
                .id(teacher.getId())
                .fullName(teacher.getUserAccount().getFullName())
                .email(teacher.getUserAccount().getEmail())
                .build();
    }

    private StudentRequestDetailDTO.StudentSummaryDTO mapToDetailStudentSummaryDTO(Student student) {
        if (student == null) return null;
        return StudentRequestDetailDTO.StudentSummaryDTO.builder()
                .id(student.getId())
                .studentCode(student.getStudentCode())
                .fullName(student.getUserAccount().getFullName())
                .email(student.getUserAccount().getEmail())
                .phone(student.getUserAccount().getPhone())
                .build();
    }

    private StudentRequestDetailDTO.ClassDetailDTO mapToDetailClassDTO(ClassEntity classEntity) {
        if (classEntity == null) return null;
        return StudentRequestDetailDTO.ClassDetailDTO.builder()
                .id(classEntity.getId())
                .code(classEntity.getCode())
                .name(classEntity.getName())
                .branch(mapToDetailBranchSummaryDTO(classEntity.getBranch()))
                .teacher(null) // Teacher mapping to be implemented
                .build();
    }

    private StudentRequestDetailDTO.BranchSummaryDTO mapToDetailBranchSummaryDTO(Branch branch) {
        if (branch == null) return null;
        return StudentRequestDetailDTO.BranchSummaryDTO.builder()
                .id(branch.getId())
                .name(branch.getName())
                .build();
    }

    private StudentRequestDetailDTO.TeacherSummaryDTO mapToDetailTeacherSummaryDTO(Teacher teacher) {
        if (teacher == null) return null;
        return StudentRequestDetailDTO.TeacherSummaryDTO.builder()
                .id(teacher.getId())
                .fullName(teacher.getUserAccount().getFullName())
                .email(teacher.getUserAccount().getEmail())
                .build();
    }

    private StudentRequestDetailDTO.SessionDetailDTO mapToDetailSessionDTO(Session session) {
        if (session == null) return null;
        return StudentRequestDetailDTO.SessionDetailDTO.builder()
                .id(session.getId())
                .date(session.getDate().format(DateTimeFormatter.ISO_DATE))
                .dayOfWeek(session.getDate().getDayOfWeek().toString())
                .courseSessionNumber(session.getCourseSession() != null ? session.getCourseSession().getSequenceNo() : null)
                .courseSessionTitle(session.getCourseSession() != null ? session.getCourseSession().getTopic() : null)
                .timeSlot(mapToDetailTimeSlotDTO(session.getTimeSlotTemplate()))
                .status(session.getStatus().toString())
                .teacher(null) // Teacher mapping to be implemented via TeachingSlot relationship
                .build();
    }

    private StudentRequestDetailDTO.TimeSlotDTO mapToDetailTimeSlotDTO(TimeSlotTemplate timeSlot) {
        if (timeSlot == null) return null;
        return StudentRequestDetailDTO.TimeSlotDTO.builder()
                .startTime(timeSlot.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")))
                .endTime(timeSlot.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm")))
                .build();
    }

    private StudentRequestDetailDTO.UserSummaryDTO mapToDetailUserSummaryDTO(UserAccount user) {
        if (user == null) return null;
        return StudentRequestDetailDTO.UserSummaryDTO.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .build();
    }

    private SessionAvailabilityDTO.SessionDTO mapToSessionDTO(Session session) {
        return SessionAvailabilityDTO.SessionDTO.builder()
                .sessionId(session.getId())
                .date(session.getDate().format(DateTimeFormatter.ISO_DATE))
                .courseSessionNumber(session.getCourseSession() != null ? session.getCourseSession().getSequenceNo() : null)
                .courseSessionTitle(session.getCourseSession() != null ? session.getCourseSession().getTopic() : null)
                .timeSlot(mapToSessionTimeSlotDTO(session.getTimeSlotTemplate()))
                .status(session.getStatus().toString())
                .type(session.getType().toString())
                .teacher(null) // Teacher mapping to be implemented via TeachingSlot relationship
                .build();
    }

    private SessionAvailabilityDTO.TimeSlotDTO mapToSessionTimeSlotDTO(TimeSlotTemplate timeSlot) {
        if (timeSlot == null) return null;
        return SessionAvailabilityDTO.TimeSlotDTO.builder()
                .startTime(timeSlot.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")))
                .endTime(timeSlot.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm")))
                .build();
    }

    private SessionAvailabilityDTO.TeacherDTO mapToSessionTeacherDTO(Teacher teacher) {
        if (teacher == null) return null;
        return SessionAvailabilityDTO.TeacherDTO.builder()
                .id(teacher.getId())
                .fullName(teacher.getUserAccount().getFullName())
                .build();
    }

    private StudentRequestDetailDTO.StudentAbsenceStatsDTO calculateAbsenceStats(Long studentId, Long classId) {
        List<StudentSession> sessions = studentSessionRepository.findByStudentIdAndClassEntityId(studentId, classId);

        long totalSessions = sessions.size();
        long absences = sessions.stream().filter(ss -> ss.getAttendanceStatus() == AttendanceStatus.ABSENT).count();

        // For now, assuming all absences are unexcused (would need more complex logic for excused vs unexcused)
        return StudentRequestDetailDTO.StudentAbsenceStatsDTO.builder()
                .totalAbsences((int) absences)
                .totalSessions((int) totalSessions)
                .absenceRate(totalSessions > 0 ? (double) absences / totalSessions * 100 : 0.0)
                .excusedAbsences(0)
                .unexcusedAbsences((int) absences)
                .build();
    }

    private StudentRequestDetailDTO.PreviousRequestsDTO calculatePreviousRequests(Long studentId) {
        List<StudentRequest> requests = studentRequestRepository.findByStudentId(studentId);

        long total = requests.size();
        long approved = requests.stream().filter(r -> r.getStatus() == RequestStatus.APPROVED).count();
        long rejected = requests.stream().filter(r -> r.getStatus() == RequestStatus.REJECTED).count();
        long cancelled = requests.stream().filter(r -> r.getStatus() == RequestStatus.CANCELLED).count();

        return StudentRequestDetailDTO.PreviousRequestsDTO.builder()
                .totalRequests((int) total)
                .approvedRequests((int) approved)
                .rejectedRequests((int) rejected)
                .cancelledRequests((int) cancelled)
                .build();
    }
}
