package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.teacherrequest.ModalityResourceSuggestionDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.RescheduleResourceSuggestionDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.RescheduleSlotSuggestionDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.SwapCandidateDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestApproveDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestCreateDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestListDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestResponseDTO;
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
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
            
            // Validate replacement teacher is not the same as original teacher
            if (replacementTeacher.getId().equals(teacher.getId())) {
                throw new CustomException(ErrorCode.INVALID_INPUT);
            }
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

        // Reload request with all relationships for response
        request = teacherRequestRepository.findByIdWithTeacherAndSession(request.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_REQUEST_NOT_FOUND));

        return mapToResponseDTO(request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherRequestListDTO> getMyRequests(Long userId) {
        log.info("Getting requests for user {}", userId);

        Teacher teacher = teacherRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_NOT_FOUND));

        // Get all requests: both created by teacher and where teacher is replacement teacher
        List<TeacherRequest> requests = teacherRequestRepository
                .findByTeacherIdOrReplacementTeacherIdOrderBySubmittedAtDesc(teacher.getId());

        return requests.stream()
                .map(this::mapToListDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherRequestListDTO> getPendingRequestsForStaff() {
        log.info("Getting pending teacher requests for staff");
        return getRequestsForStaff(RequestStatus.PENDING);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherRequestListDTO> getRequestsForStaff(RequestStatus status) {
        log.info("Getting teacher requests for staff with status {}", status);
        List<TeacherRequest> requests = status != null
                ? teacherRequestRepository.findByStatusOrderBySubmittedAtDesc(status)
                : teacherRequestRepository.findAllByOrderBySubmittedAtDesc();

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
        
        // Force load relationships to avoid lazy loading issues
        if (request.getNewResource() != null) {
            request.getNewResource().getName(); // Force load
        }
        if (request.getNewTimeSlot() != null) {
            request.getNewTimeSlot().getName(); // Force load
        }
        
        // Debug log
        log.debug("Request {} loaded: type={}, newResource={}, newTimeSlot={}, newDate={}", 
                requestId, request.getRequestType(),
                request.getNewResource() != null ? request.getNewResource().getId() : "null",
                request.getNewTimeSlot() != null ? request.getNewTimeSlot().getId() : "null",
                request.getNewDate());

        UserAccount currentUser = userAccountRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (isAcademicAffair(currentUser)) {
            return mapToResponseDTO(request);
        }

        // Check authorization: 
        // - Teacher can see requests they created
        // - Replacement teacher can see requests where they are selected as replacement
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
    @Transactional(readOnly = true)
    public TeacherRequestResponseDTO getRequestForStaff(Long requestId) {
        log.info("Getting request {} for staff", requestId);

        TeacherRequest request = teacherRequestRepository.findByIdWithTeacherAndSession(requestId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_REQUEST_NOT_FOUND));
        
        // Force load relationships to avoid lazy loading issues
        if (request.getNewResource() != null) {
            request.getNewResource().getName(); // Force load
        }
        if (request.getNewTimeSlot() != null) {
            request.getNewTimeSlot().getName(); // Force load
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
            // SWAP status = WAITING_CONFIRM (chờ replacement teacher confirm)
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

        // Validate timeSlotId exists
        if (!timeSlotTemplateRepository.existsById(timeSlotId)) {
            throw new CustomException(ErrorCode.TIMESLOT_NOT_FOUND);
        }

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

    @Override
    @Transactional(readOnly = true)
    public List<ModalityResourceSuggestionDTO> suggestModalityResources(Long sessionId, Long userId) {
        log.info("Suggesting modality resources for session {} by user {}", sessionId, userId);

        Teacher teacher = teacherRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_NOT_FOUND));
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new CustomException(ErrorCode.SESSION_NOT_FOUND));

        validateTeacherOwnsSession(sessionId, teacher.getId());
        validateTimeWindow(session.getDate());

        ClassEntity classEntity = session.getClassEntity();
        if (classEntity == null) {
            throw new CustomException(ErrorCode.CLASS_NOT_FOUND);
        }

        Branch branch = classEntity.getBranch();
        if (branch == null) {
            throw new CustomException(ErrorCode.BRANCH_NOT_FOUND);
        }

        TimeSlotTemplate timeSlotTemplate = session.getTimeSlotTemplate();
        if (timeSlotTemplate == null) {
            throw new CustomException(ErrorCode.TIMESLOT_NOT_FOUND);
        }

        Set<Long> currentResourceIds = sessionResourceRepository.findBySessionId(sessionId).stream()
                .map(SessionResource::getResource)
                .filter(Objects::nonNull)
                .map(Resource::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));

        return resourceRepository.findAll().stream()
                .filter(resource -> resource.getBranch() != null && branch.getId().equals(resource.getBranch().getId()))
                .filter(resource -> {
                    try {
                        validateResourceTypeForModality(resource, classEntity);
                        return true;
                    } catch (CustomException ex) {
                        return false;
                    }
                })
                .filter(resource -> {
                    try {
                        validateResourceAvailability(resource.getId(), session.getDate(), timeSlotTemplate.getId(), session.getId());
                        return true;
                    } catch (CustomException ex) {
                        return false;
                    }
                })
                .filter(resource -> {
                    try {
                        validateResourceCapacity(resource, sessionId);
                        return true;
                    } catch (CustomException ex) {
                        return false;
                    }
                })
                .map(resource -> ModalityResourceSuggestionDTO.builder()
                        .resourceId(resource.getId())
                        .name(resource.getName())
                        .resourceType(resource.getResourceType() != null ? resource.getResourceType().name() : null)
                        .capacity(resource.getCapacity())
                        .branchId(branch.getId())
                        .currentResource(currentResourceIds.contains(resource.getId()))
                        .build())
                .sorted(Comparator.comparing(ModalityResourceSuggestionDTO::isCurrentResource).reversed()
                        .thenComparing(dto -> dto.getName() != null ? dto.getName().toLowerCase() : ""))
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
        OffsetDateTime now = OffsetDateTime.now();
        Session newSession = Session.builder()
                .classEntity(oldSession.getClassEntity())
                .courseSession(oldSession.getCourseSession())
                .timeSlotTemplate(newTimeSlot)
                .date(newDate)
                .type(oldSession.getType())
                .status(SessionStatus.PLANNED)
                .teacherNote(null)
                .createdAt(now)
                .updatedAt(now)
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
            // Create StudentSessionId with studentId and sessionId
            StudentSession.StudentSessionId newSsId = new StudentSession.StudentSessionId();
            newSsId.setStudentId(oldSs.getStudent().getId());
            newSsId.setSessionId(newSession.getId());
            
            StudentSession newSs = StudentSession.builder()
                    .id(newSsId)
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
     * Approve SWAP request
     * - Staff can override replacement teacher
     * - Status = WAITING_CONFIRM (chờ replacement teacher confirm)
     */
    private void approveSwap(TeacherRequest request, TeacherRequestApproveDTO approveDTO, UserAccount decidedBy) {
        Session session = sessionRepository.findById(request.getSession().getId())
                .orElseThrow(() -> new CustomException(ErrorCode.SESSION_NOT_FOUND));

        Teacher replacementTeacher;

        // Staff có thể override replacement teacher
        // Priority: approveDTO.replacementTeacherId (staff override) > request.replacementTeacher (teacher chọn)
        if (approveDTO.getReplacementTeacherId() != null) {
            // Staff override replacement teacher
            replacementTeacher = teacherRepository.findById(approveDTO.getReplacementTeacherId())
                    .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_NOT_FOUND));
        } else if (request.getReplacementTeacher() != null) {
            // Dùng replacement teacher mà teacher đã chọn
            replacementTeacher = request.getReplacementTeacher();
        } else {
            // Cả hai đều null -> không thể approve
            // Với SWAP, phải có ít nhất một replacement teacher (từ teacher hoặc staff)
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        // Validate replacement teacher is not the same as original teacher
        if (replacementTeacher.getId().equals(request.getTeacher().getId())) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        // Validate replacement teacher has no conflict at session date/time
        validateTeacherConflict(replacementTeacher.getId(), 
                session.getDate(), 
                session.getTimeSlotTemplate().getId(), 
                null);

        // Set replacement teacher
        request.setReplacementTeacher(replacementTeacher);

        log.info("Swap approved: Replacement teacher {} will replace teacher {} for session {}", 
                replacementTeacher.getId(), request.getTeacher().getId(), session.getId());
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
            throw new CustomException(ErrorCode.RESOURCE_CAPACITY_INSUFFICIENT);
        }
        // Nếu capacity = null (unlimited) hoặc >= studentCount thì OK
    }

    /**
     * Map TeacherRequest entity to ResponseDTO
     */
    private TeacherRequestResponseDTO mapToResponseDTO(TeacherRequest request) {
        Session session = request.getSession();
        ClassEntity classEntity = session != null ? session.getClassEntity() : null;
        Teacher teacher = request.getTeacher();
        UserAccount teacherAccount = teacher != null ? teacher.getUserAccount() : null;
        Teacher replacementTeacher = request.getReplacementTeacher();
        UserAccount replacementTeacherAccount = replacementTeacher != null ? replacementTeacher.getUserAccount() : null;
        
        // Debug log
        log.debug("Mapping request {}: type={}, replacementTeacher={}, newResource={}, newTimeSlot={}, newDate={}", 
                request.getId(), request.getRequestType(), 
                replacementTeacher != null ? replacementTeacher.getId() : "null",
                request.getNewResource() != null ? request.getNewResource().getId() : "null",
                request.getNewTimeSlot() != null ? request.getNewTimeSlot().getId() : "null",
                request.getNewDate());

        TeacherRequestResponseDTO.TeacherRequestResponseDTOBuilder builder = TeacherRequestResponseDTO.builder()
                .id(request.getId())
                .requestType(request.getRequestType())
                .status(request.getStatus())
                .sessionId(session != null ? session.getId() : null)
                .classCode(classEntity != null ? classEntity.getCode() : null)
                .sessionDate(session != null ? session.getDate() : null)
                .teacherId(teacher != null ? teacher.getId() : null)
                .teacherName(teacherAccount != null ? teacherAccount.getFullName() : null)
                .teacherEmail(teacherAccount != null ? teacherAccount.getEmail() : null)
                .requestReason(request.getRequestReason())
                .note(request.getNote())
                .submittedAt(request.getSubmittedAt())
                .decidedAt(request.getDecidedAt());

        // Chỉ populate các fields liên quan đến request type
        switch (request.getRequestType()) {
            case MODALITY_CHANGE:
                // Chỉ cần newResourceId và newResourceName
                Resource modalityResource = request.getNewResource();
                builder.newResourceId(modalityResource != null ? modalityResource.getId() : null)
                        .newResourceName(modalityResource != null ? modalityResource.getName() : null)
                        .newDate(null)
                        .newTimeSlotId(null)
                        .newTimeSlotName(null)
                        .replacementTeacherId(null)
                        .replacementTeacherName(null)
                        .replacementTeacherEmail(null)
                        .newSessionId(null);
                break;
            case RESCHEDULE:
                // Cần newDate, newTimeSlotId, newTimeSlotName, newResourceId, newResourceName, newSessionId
                TimeSlotTemplate rescheduleTimeSlot = request.getNewTimeSlot();
                Resource rescheduleResource = request.getNewResource();
                builder.newDate(request.getNewDate())
                        .newTimeSlotId(rescheduleTimeSlot != null ? rescheduleTimeSlot.getId() : null)
                        .newTimeSlotName(rescheduleTimeSlot != null ? rescheduleTimeSlot.getName() : null)
                        .newResourceId(rescheduleResource != null ? rescheduleResource.getId() : null)
                        .newResourceName(rescheduleResource != null ? rescheduleResource.getName() : null)
                        .newSessionId(request.getNewSession() != null ? request.getNewSession().getId() : null)
                        .replacementTeacherId(null)
                        .replacementTeacherName(null)
                        .replacementTeacherEmail(null);
                break;
            case SWAP:
                // Cần replacementTeacherId, replacementTeacherName, replacementTeacherEmail
                builder.replacementTeacherId(replacementTeacher != null ? replacementTeacher.getId() : null)
                        .replacementTeacherName(replacementTeacherAccount != null ? replacementTeacherAccount.getFullName() : null)
                        .replacementTeacherEmail(replacementTeacherAccount != null ? replacementTeacherAccount.getEmail() : null)
                        .newDate(null)
                        .newTimeSlotId(null)
                        .newTimeSlotName(null)
                        .newResourceId(null)
                        .newResourceName(null)
                        .newSessionId(null);
                break;
            default:
                // Fallback: set tất cả null
                builder.newDate(null)
                        .newTimeSlotId(null)
                        .newTimeSlotName(null)
                        .newResourceId(null)
                        .newResourceName(null)
                        .replacementTeacherId(null)
                        .replacementTeacherName(null)
                        .replacementTeacherEmail(null)
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
        Teacher teacher = request.getTeacher();
        UserAccount teacherAccount = teacher != null ? teacher.getUserAccount() : null;

        return TeacherRequestListDTO.builder()
                .id(request.getId())
                .requestType(request.getRequestType())
                .status(request.getStatus())
                .sessionId(session != null ? session.getId() : null)
                .sessionDate(session != null ? session.getDate() : null)
                .className(classEntity != null ? classEntity.getName() : null)
                .classCode(classEntity != null ? classEntity.getCode() : null)
                .teacherId(teacher != null ? teacher.getId() : null)
                .teacherName(teacherAccount != null ? teacherAccount.getFullName() : null)
                .teacherEmail(teacherAccount != null ? teacherAccount.getEmail() : null)
                .requestReason(request.getRequestReason()) // Lý do tạo request - frontend có thể truncate nếu cần
                .submittedAt(request.getSubmittedAt())
                .decidedAt(request.getDecidedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherSessionDTO> getMySessions(Long userId, LocalDate date) {
        log.info("Getting sessions for user {} with date filter {}", userId, date);

        // 1. Get teacher from user account
        Teacher teacher = teacherRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_NOT_FOUND));

        // 2. Calculate date range
        LocalDate today = LocalDate.now();
        LocalDate fromDate;
        LocalDate toDate;

        if (date != null) {
            // Filter by specific date
            if (date.isBefore(today)) {
                throw new CustomException(ErrorCode.INVALID_INPUT);
            }
            fromDate = date;
            toDate = date;
        } else {
            // Default: next 7 days
            fromDate = today;
            toDate = today.plusDays(7);
        }

        // 3. Query teaching slots
        List<TeachingSlot> teachingSlots = teachingSlotRepository.findByTeacherIdAndDateRange(
                teacher.getId(), fromDate, toDate);

        // 4. Get all pending/waiting requests for these sessions to check hasPendingRequest
        // Only check PENDING and WAITING_CONFIRM - APPROVED requests are already processed
        List<Long> sessionIds = teachingSlots.stream()
                .map(ts -> ts.getSession().getId())
                .collect(Collectors.toList());

        List<TeacherRequest> pendingRequests = sessionIds.isEmpty() 
                ? List.of()
                : teacherRequestRepository.findBySessionIdInAndStatusIn(
                        sessionIds,
                        Arrays.asList(RequestStatus.PENDING, RequestStatus.WAITING_CONFIRM));

        // Create a set of session IDs that have pending/waiting requests
        java.util.Set<Long> sessionsWithPendingRequests = pendingRequests.stream()
                .map(tr -> tr.getSession().getId())
                .collect(Collectors.toSet());

        // 5. Map to DTO
        return teachingSlots.stream()
                .map(ts -> {
                    Session session = ts.getSession();
                    ClassEntity classEntity = session.getClassEntity();
                    TimeSlotTemplate timeSlot = session.getTimeSlotTemplate();
                    CourseSession courseSession = session.getCourseSession();

                    boolean hasPending = sessionsWithPendingRequests.contains(session.getId());
                    long daysFromNow = java.time.temporal.ChronoUnit.DAYS.between(today, session.getDate());

                    return TeacherSessionDTO.builder()
                            .sessionId(session.getId())
                            .date(session.getDate())
                            .startTime(timeSlot != null ? timeSlot.getStartTime() : null)
                            .endTime(timeSlot != null ? timeSlot.getEndTime() : null)
                            .className(classEntity != null ? classEntity.getName() : null)
                            .classCode(classEntity != null ? classEntity.getCode() : null)
                            .courseName(classEntity != null && classEntity.getCourse() != null 
                                    ? classEntity.getCourse().getName() : null)
                            .topic(courseSession != null ? courseSession.getTopic() : null)
                            .daysFromNow((int) daysFromNow)
                            .requestStatus(hasPending ? "Đang chờ xử lý" : "Có thể tạo request")
                            .hasPendingRequest(hasPending)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SwapCandidateDTO> suggestSwapCandidates(Long sessionId, Long userId) {
        log.info("Suggesting swap candidates for session {} by user {}", sessionId, userId);

        // 1. Get teacher from user account
        Teacher currentTeacher = teacherRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_NOT_FOUND));

        // 2. Get session and validate
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new CustomException(ErrorCode.SESSION_NOT_FOUND));

        validateTeacherOwnsSession(sessionId, currentTeacher.getId());
        validateTimeWindow(session.getDate());

        // 3. Get declined teachers for this session (teachers who have declined swap requests for this session)
        List<TeacherRequest> swapRequestsForSession = teacherRequestRepository.findAll().stream()
                .filter(tr -> tr.getSession() != null && tr.getSession().getId().equals(sessionId))
                .filter(tr -> tr.getRequestType() == TeacherRequestType.SWAP)
                .filter(tr -> tr.getNote() != null && tr.getNote().contains("DECLINED_BY_TEACHER_ID_"))
                .collect(Collectors.toList());

        java.util.Set<Long> declinedTeacherIds = new java.util.HashSet<>();
        for (TeacherRequest tr : swapRequestsForSession) {
            String note = tr.getNote();
            if (note != null && note.contains("DECLINED_BY_TEACHER_ID_")) {
                // Parse teacher ID from note: "DECLINED_BY_TEACHER_ID_{teacherId}: {reason}"
                try {
                    String prefix = "DECLINED_BY_TEACHER_ID_";
                    int startIndex = note.indexOf(prefix) + prefix.length();
                    int endIndex = note.indexOf(":", startIndex);
                    if (endIndex == -1) {
                        // If no colon found, try to find end of number
                        endIndex = startIndex;
                        while (endIndex < note.length() && Character.isDigit(note.charAt(endIndex))) {
                            endIndex++;
                        }
                    }
                    if (endIndex > startIndex) {
                        String teacherIdStr = note.substring(startIndex, endIndex).trim();
                        Long teacherId = Long.parseLong(teacherIdStr);
                        declinedTeacherIds.add(teacherId);
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse declined teacher ID from note: {}", note, e);
                }
            }
        }

        // 4. Get all teachers except current teacher and declined teachers
        List<Teacher> allTeachers = teacherRepository.findAll().stream()
                .filter(t -> !t.getId().equals(currentTeacher.getId()))
                .filter(t -> !declinedTeacherIds.contains(t.getId()))
                .collect(Collectors.toList());

        // 5. Get all teacher skills for quick lookup
        // TODO: In future, can improve to match specific subject/course skills based on session's class
        List<TeacherSkill> allTeacherSkills = teacherSkillRepository.findAll();
        java.util.Map<Long, List<TeacherSkill>> teacherSkillsMap = allTeacherSkills.stream()
                .collect(Collectors.groupingBy(ts -> ts.getTeacher().getId()));

        // 6. Map to candidates with priority calculation
        List<SwapCandidateDTO> candidates = allTeachers.stream()
                .map(teacher -> {
                    UserAccount teacherAccount = teacher.getUserAccount();
                    boolean hasConflict = hasTeacherConflict(teacher.getId(), session.getDate(), 
                            session.getTimeSlotTemplate().getId(), null);

                    // Simple skill priority: teacher has skills = 1, no skills = 0
                    // In future, can improve to match specific subject/course skills
                    int skillPriority = teacherSkillsMap.containsKey(teacher.getId()) ? 1 : 0;
                    
                    // Availability priority: no conflict = 1, has conflict = 0
                    int availabilityPriority = hasConflict ? 0 : 1;

                    return SwapCandidateDTO.builder()
                            .teacherId(teacher.getId())
                            .fullName(teacherAccount != null ? teacherAccount.getFullName() : null)
                            .email(teacherAccount != null ? teacherAccount.getEmail() : null)
                            .skillPriority(skillPriority)
                            .availabilityPriority(availabilityPriority)
                            .hasConflict(hasConflict)
                            .build();
                })
                .sorted((a, b) -> {
                    // Sort by: skillPriority DESC, availabilityPriority DESC, name ASC
                    int skillCompare = Integer.compare(b.getSkillPriority(), a.getSkillPriority());
                    if (skillCompare != 0) return skillCompare;
                    
                    int availCompare = Integer.compare(b.getAvailabilityPriority(), a.getAvailabilityPriority());
                    if (availCompare != 0) return availCompare;
                    
                    String nameA = a.getFullName() != null ? a.getFullName() : "";
                    String nameB = b.getFullName() != null ? b.getFullName() : "";
                    return nameA.compareToIgnoreCase(nameB);
                })
                .collect(Collectors.toList());

        return candidates;
    }

    /**
     * Check if teacher has conflict at specific date and time slot
     */
    private boolean hasTeacherConflict(Long teacherId, LocalDate date, Long timeSlotTemplateId, Long excludeSessionId) {
        List<TeachingSlot> slots = teachingSlotRepository.findAll().stream()
                .filter(ts -> ts.getId().getTeacherId().equals(teacherId))
                .filter(ts -> ts.getSession().getDate().equals(date))
                .filter(ts -> ts.getSession().getTimeSlotTemplate().getId().equals(timeSlotTemplateId))
                .filter(ts -> excludeSessionId == null || !ts.getSession().getId().equals(excludeSessionId))
                .filter(ts -> ts.getSession().getStatus() == SessionStatus.PLANNED || 
                               ts.getSession().getStatus() == SessionStatus.DONE)
                .filter(ts -> ts.getStatus() == TeachingSlotStatus.SCHEDULED || 
                             ts.getStatus() == TeachingSlotStatus.SUBSTITUTED)
                .collect(Collectors.toList());

        return !slots.isEmpty();
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

        // Validate user is the replacement teacher
        Teacher replacementTeacher = teacherRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_NOT_FOUND));

        if (request.getReplacementTeacher() == null || 
            !request.getReplacementTeacher().getId().equals(replacementTeacher.getId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        Session session = request.getSession();
        Teacher originalTeacher = request.getTeacher();

        // Update teaching slots
        // Original teacher slot → ON_LEAVE
        TeachingSlot.TeachingSlotId originalSlotId = new TeachingSlot.TeachingSlotId(
                session.getId(), originalTeacher.getId());
        TeachingSlot originalSlot = teachingSlotRepository.findById(originalSlotId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_SCHEDULE_NOT_FOUND));
        originalSlot.setStatus(TeachingSlotStatus.ON_LEAVE);
        teachingSlotRepository.save(originalSlot);

        // Replacement teacher slot → SUBSTITUTED
        TeachingSlot.TeachingSlotId replacementSlotId = new TeachingSlot.TeachingSlotId(
                session.getId(), replacementTeacher.getId());
        TeachingSlot replacementSlot = teachingSlotRepository.findById(replacementSlotId).orElse(null);
        
        if (replacementSlot == null) {
            // Create new slot if doesn't exist
            replacementSlot = TeachingSlot.builder()
                    .id(replacementSlotId)
                    .session(session)
                    .teacher(replacementTeacher)
                    .status(TeachingSlotStatus.SUBSTITUTED)
                    .build();
        } else {
            // Update existing slot
            replacementSlot.setStatus(TeachingSlotStatus.SUBSTITUTED);
        }
        teachingSlotRepository.save(replacementSlot);

        // Update request status
        request.setStatus(RequestStatus.APPROVED);
        request.setDecidedAt(OffsetDateTime.now());
        request = teacherRequestRepository.save(request);

        log.info("Swap request {} confirmed successfully", requestId);
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

        // Validate user is the replacement teacher
        Teacher replacementTeacher = teacherRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.TEACHER_NOT_FOUND));

        if (request.getReplacementTeacher() == null || 
            !request.getReplacementTeacher().getId().equals(replacementTeacher.getId())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        // Update request: status back to PENDING, clear replacement teacher
        // Store declined teacher ID in note for tracking: "DECLINED_BY_TEACHER_ID_{teacherId}: {reason}"
        String noteWithDeclinedTeacher = String.format("DECLINED_BY_TEACHER_ID_%d: %s", 
                replacementTeacher.getId(), reason);
        request.setStatus(RequestStatus.PENDING);
        request.setReplacementTeacher(null);
        request.setNote(noteWithDeclinedTeacher);
        request.setDecidedAt(OffsetDateTime.now());
        request = teacherRequestRepository.save(request);

        log.info("Swap request {} declined, status reset to PENDING", requestId);
        return mapToResponseDTO(request);
    }

    /**
     * Check if user has ACADEMIC_AFFAIR role
     */
    private boolean isAcademicAffair(UserAccount userAccount) {
        if (userAccount == null || userAccount.getUserRoles() == null) {
            return false;
        }
        return userAccount.getUserRoles().stream()
                .anyMatch(ur -> ur.getRole() != null && "ACADEMIC_AFFAIR".equals(ur.getRole().getCode()));
    }
}
