package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestApproveDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestCreateDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestListDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestResponseDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.RescheduleSlotSuggestionDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.RescheduleResourceSuggestionDTO;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.*;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.*;
import org.fyp.tmssep490be.services.TeacherRequestService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeacherRequestServiceImpl implements TeacherRequestService {

    private final TeacherRequestRepository teacherRequestRepository;
    private final TeacherRepository teacherRepository;
    private final SessionRepository sessionRepository;
    private final ResourceRepository resourceRepository;
    private final SessionResourceRepository sessionResourceRepository;
    private final TeachingSlotRepository teachingSlotRepository;
    private final UserAccountRepository userAccountRepository;
    private final TimeSlotTemplateRepository timeSlotTemplateRepository;
    private final StudentSessionRepository studentSessionRepository;

    @Override
    @Transactional
    public TeacherRequestResponseDTO createRequest(TeacherRequestCreateDTO createDTO, Long userId) {
        log.info("Creating teacher request type {} for session {} by user {}", 
                createDTO.getRequestType(), createDTO.getSessionId(), userId);

        // 1. Get teacher from user account
        Teacher teacher = teacherRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_NOT_FOUND));

        // 2. Get session
        Session session = sessionRepository.findById(createDTO.getSessionId())
                .orElseThrow(() -> new CustomException(ErrorCode.SESSION_NOT_FOUND));

        // 3. Validate teacher owns session
        validateTeacherOwnsSession(session.getId(), teacher.getId());

        // 4. Validate time window (session must be within 7 days from today)
        validateTimeWindow(session.getDate());

        // 5. Validate request type specific requirements
        if (createDTO.getRequestType() == TeacherRequestType.MODALITY_CHANGE) {
            // newResourceId là bắt buộc - teacher phải chọn resource
            if (createDTO.getNewResourceId() == null) {
                throw new CustomException(ErrorCode.INVALID_INPUT);
            }

            // Get new resource
            Resource newResource = resourceRepository.findById(createDTO.getNewResourceId())
                    .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

            // Validate resource availability at the session's date + time slot (exclude current session)
            validateResourceAvailability(newResource.getId(), session.getDate(), 
                    session.getTimeSlotTemplate().getId(), session.getId());

            // Validate resource type phù hợp với class modality
            validateResourceTypeForModality(newResource, session.getClassEntity());

            // Validate resource capacity >= số học viên trong session
            validateResourceCapacity(newResource, session.getId());
        } else if (createDTO.getRequestType() == TeacherRequestType.RESCHEDULE) {
            // Validate required fields for RESCHEDULE
            if (createDTO.getNewDate() == null || createDTO.getNewTimeSlotId() == null || createDTO.getNewResourceId() == null) {
                throw new CustomException(ErrorCode.INVALID_INPUT);
            }

            // Validate newDate is not in the past
            if (createDTO.getNewDate().isBefore(LocalDate.now())) {
                throw new CustomException(ErrorCode.INVALID_INPUT);
            }

            // Validate newDate is within time window (7 days from today)
            validateTimeWindow(createDTO.getNewDate());
        }

        // 6. Check for duplicate request
        if (teacherRequestRepository.existsBySessionIdAndRequestTypeAndStatus(
                createDTO.getSessionId(), 
                createDTO.getRequestType(), 
                RequestStatus.PENDING)) {
            throw new CustomException(ErrorCode.TEACHER_REQUEST_DUPLICATE);
        }

        // 7. Get user account for submittedBy
        UserAccount userAccount = userAccountRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 8. Create and save request
        Resource newResource = null;
        if (createDTO.getNewResourceId() != null) {
            newResource = resourceRepository.findById(createDTO.getNewResourceId())
                    .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
        }

        TimeSlotTemplate newTimeSlot = null;
        if (createDTO.getNewTimeSlotId() != null) {
            newTimeSlot = timeSlotTemplateRepository.findById(createDTO.getNewTimeSlotId())
                    .orElseThrow(() -> new CustomException(ErrorCode.TIMESLOT_NOT_FOUND));
        }

        TeacherRequest request = TeacherRequest.builder()
                .teacher(teacher)
                .session(session)
                .requestType(createDTO.getRequestType())
                .newDate(createDTO.getNewDate())
                .newTimeSlot(newTimeSlot)
                .newResource(newResource)
                .requestReason(createDTO.getReason())
                .status(RequestStatus.PENDING)
                .submittedBy(userAccount)
                .submittedAt(OffsetDateTime.now())
                .build();

        request = teacherRequestRepository.save(request);
        log.info("Created teacher request with ID: {}", request.getId());

        return mapToResponseDTO(request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherRequestListDTO> getMyRequests(Long userId) {
        log.info("Getting requests for user {}", userId);

        Teacher teacher = teacherRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_NOT_FOUND));

        List<TeacherRequest> requests = teacherRequestRepository
                .findByTeacherIdOrderBySubmittedAtDesc(teacher.getId());

        return requests.stream()
                .map(this::mapToListDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherRequestResponseDTO getRequestById(Long requestId, Long userId) {
        log.info("Getting request {} for user {}", requestId, userId);

        TeacherRequest request = teacherRequestRepository.findByIdWithTeacherAndSession(requestId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_REQUEST_NOT_FOUND));

        // Check authorization: Teacher can only see their own requests
        Teacher teacher = teacherRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_NOT_FOUND));

        if (!request.getTeacher().getId().equals(teacher.getId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        return mapToResponseDTO(request);
    }

    @Override
    @Transactional
    public TeacherRequestResponseDTO approveRequest(Long requestId, TeacherRequestApproveDTO approveDTO, Long userId) {
        log.info("Approving request {} by user {}", requestId, userId);

        TeacherRequest request = teacherRequestRepository.findByIdWithTeacherAndSession(requestId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_REQUEST_NOT_FOUND));

        // Validate request status
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new CustomException(ErrorCode.TEACHER_REQUEST_NOT_PENDING);
        }

        // Get user account for decidedBy
        UserAccount userAccount = userAccountRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // Handle based on request type
        if (request.getRequestType() == TeacherRequestType.MODALITY_CHANGE) {
            approveModalityChange(request, approveDTO, userAccount);
        } else if (request.getRequestType() == TeacherRequestType.RESCHEDULE) {
            approveReschedule(request, approveDTO, userAccount);
        } else {
            // For SWAP - will be implemented later
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        request.setStatus(RequestStatus.APPROVED);
        request.setDecidedBy(userAccount);
        request.setDecidedAt(OffsetDateTime.now());
        request.setNote(approveDTO.getNote());

        request = teacherRequestRepository.save(request);
        log.info("Request {} approved successfully", requestId);

        return mapToResponseDTO(request);
    }

    @Override
    @Transactional
    public TeacherRequestResponseDTO rejectRequest(Long requestId, String reason, Long userId) {
        log.info("Rejecting request {} by user {}", requestId, userId);

        TeacherRequest request = teacherRequestRepository.findByIdWithTeacherAndSession(requestId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_REQUEST_NOT_FOUND));

        // Validate request status
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new CustomException(ErrorCode.TEACHER_REQUEST_NOT_PENDING);
        }

        // Get user account for decidedBy
        UserAccount userAccount = userAccountRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        request.setStatus(RequestStatus.REJECTED);
        request.setDecidedBy(userAccount);
        request.setDecidedAt(OffsetDateTime.now());
        request.setNote(reason);

        request = teacherRequestRepository.save(request);
        log.info("Request {} rejected successfully", requestId);

        return mapToResponseDTO(request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RescheduleSlotSuggestionDTO> suggestSlots(Long sessionId, LocalDate date, Long userId) {
        Teacher teacher = teacherRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_NOT_FOUND));
        if (!sessionRepository.existsById(sessionId)) {
            throw new CustomException(ErrorCode.SESSION_NOT_FOUND);
        }
        validateTeacherOwnsSession(sessionId, teacher.getId());
        validateTimeWindow(date);

        return timeSlotTemplateRepository.findAll().stream()
                .filter(t -> {
                    try {
                        validateTeacherConflict(teacher.getId(), date, t.getId(), sessionId);
                        ensureNoStudentConflicts(sessionId, date, t.getId());
                        return true;
                    } catch (CustomException ex) {
                        return false;
                    }
                })
                .map(t -> RescheduleSlotSuggestionDTO.builder()
                        .timeSlotId(t.getId())
                        .label(t.getName())
                        .hasAvailableResource(null)
                        .availableResourceCount(null)
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RescheduleResourceSuggestionDTO> suggestResources(Long sessionId, LocalDate date, Long timeSlotId, Long userId) {
        Teacher teacher = teacherRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_NOT_FOUND));
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new CustomException(ErrorCode.SESSION_NOT_FOUND));
        validateTeacherOwnsSession(sessionId, teacher.getId());
        validateTimeWindow(date);

        ClassEntity classEntity = session.getClassEntity();

        return resourceRepository.findAll().stream()
                .filter(r -> r.getBranch().getId().equals(classEntity.getBranch().getId()))
                .filter(r -> {
                    try {
                        validateResourceTypeForModality(r, classEntity);
                        return true;
                    } catch (CustomException ex) {
                        return false;
                    }
                })
                .filter(r -> {
                    try {
                        validateResourceAvailability(r.getId(), date, timeSlotId, null);
                        return true;
                    } catch (CustomException ex) {
                        return false;
                    }
                })
                .filter(r -> {
                    try {
                        validateResourceCapacity(r, sessionId);
                        return true;
                    } catch (CustomException ex) {
                        return false;
                    }
                })
                .filter(r -> {
                    try {
                        validateTeacherConflict(teacher.getId(), date, timeSlotId, sessionId);
                        return true;
                    } catch (CustomException ex) {
                        return false;
                    }
                })
                .filter(r -> ensureNoStudentConflicts(sessionId, date, timeSlotId))
                .map(r -> RescheduleResourceSuggestionDTO.builder()
                        .resourceId(r.getId())
                        .name(r.getName())
                        .resourceType(r.getResourceType().name())
                        .capacity(r.getCapacity())
                        .branchId(r.getBranch().getId())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Approve MODALITY_CHANGE request
     * - Updates session_resource (delete old, insert new)
     * - Only changes resource for this specific session, does not affect class.modality
     */
    private void approveModalityChange(TeacherRequest request, TeacherRequestApproveDTO approveDTO, UserAccount decidedBy) {
        // Load session with timeSlotTemplate
        Session session = sessionRepository.findById(request.getSession().getId())
                .orElseThrow(() -> new CustomException(ErrorCode.SESSION_NOT_FOUND));
        
        Resource newResource;

        // Teacher bắt buộc phải chọn resource khi tạo request
        // Staff có thể override resource từ teacher nếu cần
        // Priority: approveDTO.newResourceId (staff override) > request.newResource (teacher chọn)
        if (request.getNewResource() == null) {
            // Teacher bắt buộc phải chọn resource khi tạo request
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        if (approveDTO.getNewResourceId() != null) {
            // Staff override resource từ teacher (có thể vì resource của teacher bị conflict)
            newResource = resourceRepository.findById(approveDTO.getNewResourceId())
                    .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
        } else {
            // Dùng resource mà teacher đã chọn
            newResource = request.getNewResource();
        }

        // Validate resource availability at session date + time
        // Exclude current session when checking (we're replacing its resource)
        validateResourceAvailability(newResource.getId(), session.getDate(), 
                session.getTimeSlotTemplate().getId(), session.getId());

        // Validate resource type phù hợp với class modality
        validateResourceTypeForModality(newResource, session.getClassEntity());

        // Validate resource capacity >= số học viên trong session
        validateResourceCapacity(newResource, session.getId());

        // Update session_resource
        // Delete old session_resource
        sessionResourceRepository.deleteBySessionId(session.getId());

        // Create new session_resource
        SessionResource newSessionResource = SessionResource.builder()
                .id(new SessionResource.SessionResourceId(session.getId(), newResource.getId()))
                .session(session)
                .resource(newResource)
                .build();
        sessionResourceRepository.save(newSessionResource);

        log.info("Modality change approved: Session {} updated to resource {}", 
                session.getId(), newResource.getId());
    }

    /**
     * Approve RESCHEDULE request
     * - Creates new session with new date/slot/resource
     * - Copies teaching_slot and student_sessions
     * - Cancels old session
     */
    private void approveReschedule(TeacherRequest request, TeacherRequestApproveDTO approveDTO, UserAccount decidedBy) {
        // Load old session with relationships
        Session oldSession = sessionRepository.findById(request.getSession().getId())
                .orElseThrow(() -> new CustomException(ErrorCode.SESSION_NOT_FOUND));

        // Determine new date, time slot, and resource (Staff can override)
        LocalDate newDate = approveDTO.getNewDate() != null ? approveDTO.getNewDate() : request.getNewDate();
        Long newTimeSlotId = approveDTO.getNewTimeSlotId() != null ? approveDTO.getNewTimeSlotId() : 
                (request.getNewTimeSlot() != null ? request.getNewTimeSlot().getId() : null);
        Long newResourceId = approveDTO.getNewResourceId() != null ? approveDTO.getNewResourceId() : 
                (request.getNewResource() != null ? request.getNewResource().getId() : null);

        // Validate required fields
        if (newDate == null || newTimeSlotId == null || newResourceId == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        // Validate newDate is not in the past
        if (newDate.isBefore(LocalDate.now())) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        // Get new time slot template
        TimeSlotTemplate newTimeSlot = timeSlotTemplateRepository.findById(newTimeSlotId)
                .orElseThrow(() -> new CustomException(ErrorCode.TIMESLOT_NOT_FOUND));

        // Get new resource
        Resource newResource = resourceRepository.findById(newResourceId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        // Validate conflicts
        // 1. Teacher conflict: Check if teacher has another session at new date/time
        validateTeacherConflict(request.getTeacher().getId(), newDate, newTimeSlotId, oldSession.getId());

        // 2. Resource conflict: Check if resource is available at new date/time
        validateResourceAvailability(newResourceId, newDate, newTimeSlotId, null);

        // Create new session
        Session newSession = Session.builder()
                .classEntity(oldSession.getClassEntity())
                .courseSession(oldSession.getCourseSession())
                .timeSlotTemplate(newTimeSlot)
                .date(newDate)
                .type(oldSession.getType())
                .status(SessionStatus.PLANNED)
                .teacherNote(null)
                .build();
        newSession = sessionRepository.save(newSession);

        // Copy teaching slot
        // Validate old teaching slot exists
        boolean oldTeachingSlotExists = teachingSlotRepository.existsById(
                new TeachingSlot.TeachingSlotId(oldSession.getId(), request.getTeacher().getId()));
        if (!oldTeachingSlotExists) {
            throw new CustomException(ErrorCode.TEACHER_SCHEDULE_NOT_FOUND);
        }

        TeachingSlot newTeachingSlot = TeachingSlot.builder()
                .id(new TeachingSlot.TeachingSlotId(newSession.getId(), request.getTeacher().getId()))
                .session(newSession)
                .teacher(request.getTeacher())
                .status(TeachingSlotStatus.SCHEDULED)
                .build();
        teachingSlotRepository.save(newTeachingSlot);

        // Copy student sessions (only for enrolled students with PLANNED status)
        List<StudentSession> oldStudentSessions = studentSessionRepository.findAll().stream()
                .filter(ss -> ss.getSession().getId().equals(oldSession.getId()))
                .filter(ss -> ss.getAttendanceStatus() == null || 
                        ss.getAttendanceStatus() == org.fyp.tmssep490be.entities.enums.AttendanceStatus.PLANNED)
                .collect(Collectors.toList());

        for (StudentSession oldSs : oldStudentSessions) {
            StudentSession newSs = StudentSession.builder()
                    .student(oldSs.getStudent())
                    .session(newSession)
                    .isMakeup(false)
                    .attendanceStatus(org.fyp.tmssep490be.entities.enums.AttendanceStatus.PLANNED)
                    .homeworkStatus(null)
                    .note(null)
                    .build();
            studentSessionRepository.save(newSs);
        }

        // Create session resource
        SessionResource newSessionResource = SessionResource.builder()
                .id(new SessionResource.SessionResourceId(newSession.getId(), newResourceId))
                .session(newSession)
                .resource(newResource)
                .build();
        sessionResourceRepository.save(newSessionResource);

        // Cancel old session
        oldSession.setStatus(SessionStatus.CANCELLED);
        sessionRepository.save(oldSession);

        // Set newSessionId in request
        request.setNewSession(newSession);

        log.info("Reschedule approved: Old session {} cancelled, new session {} created", 
                oldSession.getId(), newSession.getId());
    }

    /**
     * Validate teacher conflict at date/time
     */
    private void validateTeacherConflict(Long teacherId, LocalDate date, Long timeSlotTemplateId, Long excludeSessionId) {
        boolean hasConflict = teachingSlotRepository.findAll().stream()
                .anyMatch(ts -> ts.getId().getTeacherId().equals(teacherId)
                        && ts.getSession().getDate().equals(date)
                        && ts.getSession().getTimeSlotTemplate().getId().equals(timeSlotTemplateId)
                        && !ts.getSession().getId().equals(excludeSessionId)
                        && (ts.getSession().getStatus() == SessionStatus.PLANNED || 
                            ts.getSession().getStatus() == SessionStatus.DONE));

        if (hasConflict) {
            throw new CustomException(ErrorCode.TEACHER_AVAILABILITY_CONFLICT);
        }
    }

    /**
     * Validate teacher owns session via TeachingSlot
     */
    private void validateTeacherOwnsSession(Long sessionId, Long teacherId) {
        boolean owns = teachingSlotRepository.existsByIdSessionIdAndIdTeacherIdAndStatusIn(
                sessionId,
                teacherId,
                Arrays.asList(TeachingSlotStatus.SCHEDULED, TeachingSlotStatus.SUBSTITUTED)
        );

        if (!owns) {
            throw new CustomException(ErrorCode.TEACHER_DOES_NOT_OWN_SESSION);
        }
    }

    /**
     * Validate session is within 7 days window
     */
    private void validateTimeWindow(LocalDate sessionDate) {
        LocalDate today = LocalDate.now();
        LocalDate maxDate = today.plusDays(7);

        if (sessionDate.isBefore(today) || sessionDate.isAfter(maxDate)) {
            throw new CustomException(ErrorCode.SESSION_NOT_IN_TIME_WINDOW);
        }
    }

    /**
     * Validate resource availability at date + time slot
     * @param excludeSessionId Session ID to exclude from check (when updating existing session)
     */
    private void validateResourceAvailability(Long resourceId, LocalDate date, Long timeSlotTemplateId, Long excludeSessionId) {
        // Check if resource is already booked at this date + time slot (excluding current session if provided)
        boolean isBooked = sessionResourceRepository.existsByResourceIdAndDateAndTimeSlotAndStatusIn(
                resourceId,
                date,
                timeSlotTemplateId,
                Arrays.asList(SessionStatus.PLANNED, SessionStatus.DONE),
                excludeSessionId
        );

        if (isBooked) {
            throw new CustomException(ErrorCode.RESOURCE_NOT_AVAILABLE);
        }
    }

    private boolean ensureNoStudentConflicts(Long sessionId, LocalDate date, Long timeSlotTemplateId) {
        List<StudentSession> all = studentSessionRepository.findAll();
        List<Long> studentIds = all.stream()
                .filter(ss -> ss.getSession().getId().equals(sessionId))
                .map(ss -> ss.getStudent().getId())
                .distinct()
                .collect(Collectors.toList());

        boolean anyConflict = all.stream()
                .filter(ss -> studentIds.contains(ss.getStudent().getId()))
                .anyMatch(ss -> ss.getSession().getDate().equals(date)
                        && ss.getSession().getTimeSlotTemplate().getId().equals(timeSlotTemplateId)
                        && (ss.getSession().getStatus() == SessionStatus.PLANNED || ss.getSession().getStatus() == SessionStatus.DONE));

        if (anyConflict) {
            throw new CustomException(ErrorCode.SCHEDULE_CONFLICT);
        }
        return true;
    }

    /**
     * Validate resource type phù hợp với class modality
     * - OFFLINE class → VIRTUAL resource (chuyển sang online cho session này)
     * - ONLINE class → ROOM resource (chuyển sang offline cho session này)
     * - HYBRID class → có thể dùng cả ROOM hoặc VIRTUAL
     */
    private void validateResourceTypeForModality(Resource resource, ClassEntity classEntity) {
        Modality classModality = classEntity.getModality();
        ResourceType resourceType = resource.getResourceType();

        if (classModality == Modality.OFFLINE) {
            // OFFLINE class muốn chuyển sang online → cần VIRTUAL resource
            if (resourceType != ResourceType.VIRTUAL) {
                throw new CustomException(ErrorCode.INVALID_RESOURCE_FOR_MODALITY);
            }
        } else if (classModality == Modality.ONLINE) {
            // ONLINE class muốn chuyển sang offline → cần ROOM resource
            if (resourceType != ResourceType.ROOM) {
                throw new CustomException(ErrorCode.INVALID_RESOURCE_FOR_MODALITY);
            }
        }
        // HYBRID class có thể dùng cả ROOM hoặc VIRTUAL, không cần validate
    }

    /**
     * Validate resource capacity >= số học viên trong session
     * Đếm tất cả học viên có trong session (bao gồm cả học viên học bù)
     */
    private void validateResourceCapacity(Resource resource, Long sessionId) {
        // Đếm số học viên trong session (từ student_sessions)
        long studentCount = studentSessionRepository.countBySessionId(sessionId);

        // Nếu resource có capacity (not null), phải >= số học viên trong session
        if (resource.getCapacity() != null && resource.getCapacity() < studentCount) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        // Nếu capacity = null (unlimited) hoặc >= studentCount thì OK
    }

    /**
     * Map TeacherRequest entity to ResponseDTO
     */
    private TeacherRequestResponseDTO mapToResponseDTO(TeacherRequest request) {
        TeacherRequestResponseDTO.TeacherRequestResponseDTOBuilder builder = TeacherRequestResponseDTO.builder()
                .id(request.getId())
                .requestType(request.getRequestType())
                .status(request.getStatus())
                .sessionId(request.getSession() != null ? request.getSession().getId() : null)
                .requestReason(request.getRequestReason())
                .note(request.getNote())
                .submittedAt(request.getSubmittedAt())
                .decidedAt(request.getDecidedAt());

        // Chỉ populate các fields liên quan đến request type
        switch (request.getRequestType()) {
            case MODALITY_CHANGE:
                // Chỉ cần newResourceId
                builder.newResourceId(request.getNewResource() != null ? 
                        request.getNewResource().getId() : null)
                        .newDate(null)
                        .newTimeSlotId(null)
                        .replacementTeacherId(null)
                        .newSessionId(null);
                break;
            case RESCHEDULE:
                // Cần newDate, newTimeSlotId, newResourceId, newSessionId
                builder.newDate(request.getNewDate())
                        .newTimeSlotId(request.getNewTimeSlot() != null ? 
                                request.getNewTimeSlot().getId() : null)
                        .newResourceId(request.getNewResource() != null ? 
                                request.getNewResource().getId() : null)
                .newSessionId(request.getNewSession() != null ? 
                        request.getNewSession().getId() : null)
                        .replacementTeacherId(null);
                break;
            case SWAP:
                // Chỉ cần replacementTeacherId
                builder.replacementTeacherId(request.getReplacementTeacher() != null ? 
                        request.getReplacementTeacher().getId() : null)
                        .newDate(null)
                        .newTimeSlotId(null)
                        .newResourceId(null)
                        .newSessionId(null);
                break;
            default:
                // Fallback: set tất cả null
                builder.newDate(null)
                        .newTimeSlotId(null)
                        .newResourceId(null)
                        .replacementTeacherId(null)
                        .newSessionId(null);
                break;
        }

        return builder.build();
    }

    /**
     * Map TeacherRequest entity to ListDTO
     */
    private TeacherRequestListDTO mapToListDTO(TeacherRequest request) {
        Session session = request.getSession();
        ClassEntity classEntity = session != null ? session.getClassEntity() : null;

        return TeacherRequestListDTO.builder()
                .id(request.getId())
                .requestType(request.getRequestType())
                .status(request.getStatus())
                .sessionId(session != null ? session.getId() : null)
                .sessionDate(session != null ? session.getDate() : null)
                .className(classEntity != null ? classEntity.getName() : null)
                .classCode(classEntity != null ? classEntity.getCode() : null)
                .requestReason(request.getRequestReason()) // Lý do tạo request - frontend có thể truncate nếu cần
                .submittedAt(request.getSubmittedAt())
                .decidedAt(request.getDecidedAt())
                .build();
    }
}
