package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestApproveDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestCreateDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestListDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestResponseDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.RescheduleSlotSuggestionDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.RescheduleResourceSuggestionDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.SwapCandidateDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherSessionDTO;
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
import java.util.*;
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
    private final TeacherSkillRepository teacherSkillRepository;
    private final TeacherAvailabilityRepository teacherAvailabilityRepository;

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
            // newResourceId là optional - teacher có thể không chọn, staff sẽ chọn khi approve
            if (createDTO.getNewResourceId() != null) {
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
            }
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
        } else if (createDTO.getRequestType() == TeacherRequestType.SWAP) {
            // For SWAP: replacementTeacherId is optional (teacher can suggest or leave to staff)
            // If provided, validate replacement teacher exists and is valid
            if (createDTO.getReplacementTeacherId() != null) {
                Teacher replacementTeacher = teacherRepository.findById(createDTO.getReplacementTeacherId())
                        .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_NOT_FOUND));
                
                // Validate replacement teacher is not the same as original teacher
                if (replacementTeacher.getId().equals(teacher.getId())) {
                    throw new CustomException(ErrorCode.INVALID_INPUT);
                }
                
                // Validate replacement teacher has no conflict at session time
                validateTeacherSwapConflict(replacementTeacher.getId(), session);
            }
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

        Teacher replacementTeacher = null;
        if (createDTO.getReplacementTeacherId() != null) {
            replacementTeacher = teacherRepository.findById(createDTO.getReplacementTeacherId())
                    .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_NOT_FOUND));
        }

        TeacherRequest request = TeacherRequest.builder()
                .teacher(teacher)
                .session(session)
                .requestType(createDTO.getRequestType())
                .newDate(createDTO.getNewDate())
                .newTimeSlot(newTimeSlot)
                .newResource(newResource)
                .replacementTeacher(replacementTeacher)
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

        // Get requests created by this teacher
        List<TeacherRequest> myCreatedRequests = teacherRequestRepository
                .findByTeacherIdOrderBySubmittedAtDesc(teacher.getId());

        // Get requests where this teacher is the replacement teacher (waiting for confirmation)
        List<TeacherRequest> myReplacementRequests = teacherRequestRepository
                .findByReplacementTeacherIdOrderBySubmittedAtDesc(teacher.getId());

        // Combine and deduplicate (in case teacher created a request and is also replacement)
        Set<TeacherRequest> allRequests = new LinkedHashSet<>(myCreatedRequests);
        allRequests.addAll(myReplacementRequests);

        return allRequests.stream()
                .sorted(Comparator.comparing(TeacherRequest::getSubmittedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::mapToListDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherRequestResponseDTO getRequestById(Long requestId, Long userId) {
        log.info("Getting request {} for user {}", requestId, userId);

        TeacherRequest request = teacherRequestRepository.findByIdWithTeacherAndSession(requestId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_REQUEST_NOT_FOUND));

        // Check authorization: 
        // - Teacher can see their own requests (created by them)
        // - Replacement teacher can see requests where they are the replacement teacher
        Teacher teacher = teacherRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_NOT_FOUND));

        boolean isRequestOwner = request.getTeacher().getId().equals(teacher.getId());
        boolean isReplacementTeacher = request.getReplacementTeacher() != null && 
                request.getReplacementTeacher().getId().equals(teacher.getId());

        if (!isRequestOwner && !isReplacementTeacher) {
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
            request.setStatus(RequestStatus.APPROVED);
        } else if (request.getRequestType() == TeacherRequestType.RESCHEDULE) {
            approveReschedule(request, approveDTO, userAccount);
            request.setStatus(RequestStatus.APPROVED);
        } else if (request.getRequestType() == TeacherRequestType.SWAP) {
            approveSwap(request, approveDTO, userAccount);
            // SWAP sets status to WAITING_CONFIRM (not APPROVED)
            request.setStatus(RequestStatus.WAITING_CONFIRM);
        } else {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
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
                    // For RESCHEDULE, resource type must match class modality (not change it)
                    // OFFLINE class → ROOM resource
                    // ONLINE class → VIRTUAL resource
                    // HYBRID class → can use both
                    Modality classModality = classEntity.getModality();
                    ResourceType resourceType = r.getResourceType();
                    
                    if (classModality == Modality.OFFLINE) {
                        return resourceType == ResourceType.ROOM;
                    } else if (classModality == Modality.ONLINE) {
                        return resourceType == ResourceType.VIRTUAL;
                    } else {
                        // HYBRID can use both
                        return true;
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

        // Teacher có thể chọn hoặc không chọn resource khi tạo request
        // Staff có thể override resource từ teacher nếu cần
        // Priority: approveDTO.newResourceId (staff override) > request.newResource (teacher chọn)
        if (approveDTO.getNewResourceId() != null) {
            // Staff override resource từ teacher (có thể vì resource của teacher bị conflict)
            newResource = resourceRepository.findById(approveDTO.getNewResourceId())
                    .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
        } else if (request.getNewResource() != null) {
            // Dùng resource mà teacher đã chọn
            newResource = request.getNewResource();
        } else {
            // Staff không chọn resource và teacher cũng không chọn -> không thể approve
            throw new CustomException(ErrorCode.INVALID_INPUT);
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
     * Validate replacement teacher has no conflict at session time
     */
    private void validateTeacherSwapConflict(Long replacementTeacherId, Session session) {
        boolean hasConflict = teachingSlotRepository.findAll().stream()
                .anyMatch(ts -> ts.getId().getTeacherId().equals(replacementTeacherId)
                        && ts.getSession().getDate().equals(session.getDate())
                        && ts.getSession().getTimeSlotTemplate().getId().equals(session.getTimeSlotTemplate().getId())
                        && (ts.getSession().getStatus() == SessionStatus.PLANNED || 
                            ts.getSession().getStatus() == SessionStatus.DONE));

        if (hasConflict) {
            throw new CustomException(ErrorCode.TEACHER_SCHEDULE_CONFLICT);
        }
    }

    /**
     * Approve SWAP request (Staff)
     * Sets replacement teacher (can override teacher's choice) and status to WAITING_CONFIRM
     */
    private void approveSwap(TeacherRequest request, TeacherRequestApproveDTO approveDTO, UserAccount decidedBy) {
        // Determine replacement teacher: Staff override > Teacher suggestion
        Long replacementTeacherId = approveDTO.getReplacementTeacherId() != null ? 
                approveDTO.getReplacementTeacherId() : 
                (request.getReplacementTeacher() != null ? request.getReplacementTeacher().getId() : null);

        if (replacementTeacherId == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        Teacher replacementTeacher = teacherRepository.findById(replacementTeacherId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_NOT_FOUND));

        // Validate replacement teacher is not the same as original teacher
        if (replacementTeacher.getId().equals(request.getTeacher().getId())) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        // Validate replacement teacher has no conflict
        validateTeacherSwapConflict(replacementTeacherId, request.getSession());

        // Set replacement teacher
        request.setReplacementTeacher(replacementTeacher);

        log.info("Swap request approved: Replacement teacher {} set for session {}", 
                replacementTeacherId, request.getSession().getId());
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
            throw new CustomException(ErrorCode.RESOURCE_CAPACITY_INSUFFICIENT);
        }
        // Nếu capacity = null (unlimited) hoặc >= studentCount thì OK
    }

    @Override
    @Transactional(readOnly = true)
    public List<SwapCandidateDTO> suggestSwapCandidates(Long sessionId, Long userId) {
        Teacher teacher = teacherRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_NOT_FOUND));
        
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new CustomException(ErrorCode.SESSION_NOT_FOUND));
        
        validateTeacherOwnsSession(sessionId, teacher.getId());
        validateTimeWindow(session.getDate());

        // Get session skill requirements
        Skill[] requiredSkills = session.getCourseSession() != null && 
                session.getCourseSession().getSkillSet() != null ?
                session.getCourseSession().getSkillSet() : new Skill[]{Skill.GENERAL};

        // Get all teachers except the original teacher
        List<Teacher> allTeachers = teacherRepository.findAll().stream()
                .filter(t -> !t.getId().equals(teacher.getId()))
                .collect(Collectors.toList());

        return allTeachers.stream()
                .map(t -> {
                    // Get teacher skills
                    List<TeacherSkill> teacherSkills = teacherSkillRepository.findAll().stream()
                            .filter(ts -> ts.getTeacher().getId().equals(t.getId()))
                            .collect(Collectors.toList());

                    // Calculate skill priority
                    int skillPriority = 1;
                    boolean hasExactMatch = teacherSkills.stream()
                            .anyMatch(ts -> Arrays.asList(requiredSkills).contains(ts.getId().getSkill()));
                    boolean hasGeneral = teacherSkills.stream()
                            .anyMatch(ts -> ts.getId().getSkill() == Skill.GENERAL);

                    if (hasExactMatch) {
                        skillPriority = 3;
                    } else if (hasGeneral) {
                        skillPriority = 2;
                    }

                    // Calculate availability priority
                    int availabilityPriority = 1;
                    if (t.getContractType() != null && t.getContractType().equals("full-time")) {
                        availabilityPriority = 2;
                    } else {
                        // Check if part-time teacher has availability for this day/time
                        int dayOfWeek = session.getDate().getDayOfWeek().getValue() % 7; // 0=Sunday, 1=Monday, etc.
                        boolean hasAvailability = teacherAvailabilityRepository.findAll().stream()
                                .anyMatch(ta -> ta.getTeacher().getId().equals(t.getId())
                                        && ta.getId().getDayOfWeek() == dayOfWeek
                                        && ta.getTimeSlotTemplate().getId().equals(session.getTimeSlotTemplate().getId()));
                        if (hasAvailability) {
                            availabilityPriority = 2;
                        }
                    }

                    // Check for conflict
                    boolean hasConflict = teachingSlotRepository.findAll().stream()
                            .anyMatch(ts -> ts.getId().getTeacherId().equals(t.getId())
                                    && ts.getSession().getDate().equals(session.getDate())
                                    && ts.getSession().getTimeSlotTemplate().getId().equals(session.getTimeSlotTemplate().getId())
                                    && (ts.getSession().getStatus() == SessionStatus.PLANNED || 
                                        ts.getSession().getStatus() == SessionStatus.DONE));

                    // Only include if no conflict and has matching skill
                    if (hasConflict || (skillPriority == 1 && !hasGeneral)) {
                        return null;
                    }

                    UserAccount userAccount = t.getUserAccount();
                    return SwapCandidateDTO.builder()
                            .teacherId(t.getId())
                            .fullName(userAccount != null ? userAccount.getFullName() : null)
                            .email(userAccount != null ? userAccount.getEmail() : null)
                            .skillPriority(skillPriority)
                            .availabilityPriority(availabilityPriority)
                            .hasConflict(false)
                            .build();
                })
                .filter(c -> c != null)
                .sorted((a, b) -> {
                    // Sort by skill priority DESC, then availability priority DESC, then name
                    int skillCompare = Integer.compare(b.getSkillPriority(), a.getSkillPriority());
                    if (skillCompare != 0) return skillCompare;
                    int availCompare = Integer.compare(b.getAvailabilityPriority(), a.getAvailabilityPriority());
                    if (availCompare != 0) return availCompare;
                    return a.getFullName().compareTo(b.getFullName());
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TeacherRequestResponseDTO confirmSwap(Long requestId, Long userId) {
        log.info("Confirming swap request {} by replacement teacher {}", requestId, userId);

        TeacherRequest request = teacherRequestRepository.findByIdWithTeacherAndSession(requestId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_REQUEST_NOT_FOUND));

        // Validate request type
        if (request.getRequestType() != TeacherRequestType.SWAP) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        // Validate request status
        if (request.getStatus() != RequestStatus.WAITING_CONFIRM) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        // Validate replacement teacher is the current user
        Teacher replacementTeacher = teacherRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_NOT_FOUND));

        if (request.getReplacementTeacher() == null || 
                !request.getReplacementTeacher().getId().equals(replacementTeacher.getId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        Session session = request.getSession();
        Teacher originalTeacher = request.getTeacher();

        // Re-validate no conflict (race condition check)
        validateTeacherSwapConflict(replacementTeacher.getId(), session);

        // Update original teacher's teaching slot to ON_LEAVE
        TeachingSlot.TeachingSlotId originalSlotId = new TeachingSlot.TeachingSlotId(
                session.getId(), originalTeacher.getId());
        TeachingSlot originalSlot = teachingSlotRepository.findById(originalSlotId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHING_SLOT_NOT_FOUND));
        originalSlot.setStatus(TeachingSlotStatus.ON_LEAVE);
        teachingSlotRepository.save(originalSlot);

        // Create or update replacement teacher's teaching slot
        TeachingSlot.TeachingSlotId replacementSlotId = new TeachingSlot.TeachingSlotId(
                session.getId(), replacementTeacher.getId());
        TeachingSlot replacementSlot = teachingSlotRepository.findById(replacementSlotId)
                .orElse(TeachingSlot.builder()
                        .id(replacementSlotId)
                        .session(session)
                        .teacher(replacementTeacher)
                        .status(TeachingSlotStatus.SUBSTITUTED)
                        .build());
        replacementSlot.setStatus(TeachingSlotStatus.SUBSTITUTED);
        teachingSlotRepository.save(replacementSlot);

        // Update request status
        request.setStatus(RequestStatus.APPROVED);
        request.setDecidedAt(OffsetDateTime.now());
        request = teacherRequestRepository.save(request);

        log.info("Swap request {} confirmed: Teaching slots updated", requestId);

        return mapToResponseDTO(request);
    }

    @Override
    @Transactional
    public TeacherRequestResponseDTO declineSwap(Long requestId, String reason, Long userId) {
        log.info("Declining swap request {} by replacement teacher {}", requestId, userId);

        TeacherRequest request = teacherRequestRepository.findByIdWithTeacherAndSession(requestId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_REQUEST_NOT_FOUND));

        // Validate request type
        if (request.getRequestType() != TeacherRequestType.SWAP) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        // Validate request status
        if (request.getStatus() != RequestStatus.WAITING_CONFIRM) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        // Validate replacement teacher is the current user
        Teacher replacementTeacher = teacherRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_NOT_FOUND));

        if (request.getReplacementTeacher() == null || 
                !request.getReplacementTeacher().getId().equals(replacementTeacher.getId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        // Reset request to PENDING and clear replacement teacher
        request.setStatus(RequestStatus.PENDING);
        request.setReplacementTeacher(null);
        request.setNote(reason != null ? reason : "Replacement teacher declined");
        request = teacherRequestRepository.save(request);

        log.info("Swap request {} declined: Reset to PENDING", requestId);

        return mapToResponseDTO(request);
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

    @Override
    @Transactional(readOnly = true)
    public List<TeacherSessionDTO> getMyFutureSessions(Long userId, LocalDate date) {
        log.info("Getting future sessions for teacher user {}, filter date: {}", userId, date);

        // 1. Get teacher from user account
        Teacher teacher = teacherRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_NOT_FOUND));

        // 2. Determine date range
        LocalDate today = LocalDate.now();
        LocalDate startDate;
        LocalDate endDate;
        
        if (date != null) {
            // Filter by specific date
            if (date.isBefore(today)) {
                throw new CustomException(ErrorCode.INVALID_INPUT);
            }
            startDate = date;
            endDate = date;
        } else {
            // Default: next 7 days
            startDate = today;
            endDate = today.plusDays(7);
        }

        // 3. Query sessions: teacher's sessions in date range, status PLANNED
        List<TeachingSlot> teachingSlots = teachingSlotRepository.findByTeacherIdAndDateRange(
                teacher.getId(),
                Arrays.asList(TeachingSlotStatus.SCHEDULED, TeachingSlotStatus.SUBSTITUTED),
                SessionStatus.PLANNED,
                startDate,
                endDate
        );

        // 4. Check which sessions have pending requests
        List<Long> sessionIdsWithPendingRequests = teacherRequestRepository
                .findBySessionIdInAndStatusIn(
                        teachingSlots.stream()
                                .map(ts -> ts.getSession().getId())
                                .collect(Collectors.toList()),
                        Arrays.asList(RequestStatus.PENDING, RequestStatus.WAITING_CONFIRM, RequestStatus.APPROVED)
                )
                .stream()
                .map(tr -> tr.getSession().getId())
                .distinct()
                .collect(Collectors.toList());

        // 5. Map to DTO
        return teachingSlots.stream()
                .map(ts -> {
                    Session session = ts.getSession();
                    ClassEntity classEntity = session.getClassEntity();
                    Course course = classEntity != null ? classEntity.getCourse() : null;
                    CourseSession courseSession = session.getCourseSession();
                    TimeSlotTemplate timeSlot = session.getTimeSlotTemplate();

                    boolean hasPendingRequest = sessionIdsWithPendingRequests.contains(session.getId());
                    long daysFromNow = java.time.temporal.ChronoUnit.DAYS.between(today, session.getDate());

                    return TeacherSessionDTO.builder()
                            .sessionId(session.getId())
                            .date(session.getDate())
                            .startTime(timeSlot != null ? timeSlot.getStartTime() : null)
                            .endTime(timeSlot != null ? timeSlot.getEndTime() : null)
                            .className(classEntity != null ? classEntity.getName() : null)
                            .courseName(course != null ? course.getName() : null)
                            .topic(courseSession != null ? courseSession.getTopic() : null)
                            .daysFromNow(daysFromNow)
                            .requestStatus(hasPendingRequest ? "Đang chờ xử lý" : "Có thể tạo request")
                            .hasPendingRequest(hasPendingRequest)
                            .build();
                })
                .sorted((a, b) -> {
                    int dateCompare = a.getDate().compareTo(b.getDate());
                    if (dateCompare != 0) return dateCompare;
                    if (a.getStartTime() != null && b.getStartTime() != null) {
                        return a.getStartTime().compareTo(b.getStartTime());
                    }
                    return 0;
                })
                .collect(Collectors.toList());
    }
}
