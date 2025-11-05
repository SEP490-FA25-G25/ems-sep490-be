package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestApproveDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestCreateDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestListDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestResponseDTO;
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
            if (createDTO.getNewResourceId() == null) {
                throw new CustomException(ErrorCode.INVALID_INPUT);
            }

            // Validate resource availability at the session's date + time slot (exclude current session)
            boolean resourceBooked = sessionResourceRepository.existsByResourceIdAndDateAndTimeSlotAndStatusIn(
                    createDTO.getNewResourceId(),
                    session.getDate(),
                    session.getTimeSlotTemplate().getId(),
                    Arrays.asList(SessionStatus.PLANNED, SessionStatus.DONE),
                    session.getId()
            );
            if (resourceBooked) {
                throw new CustomException(ErrorCode.RESOURCE_NOT_AVAILABLE);
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

        TeacherRequest request = TeacherRequest.builder()
                .teacher(teacher)
                .session(session)
                .requestType(createDTO.getRequestType())
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
        } else {
            // For SWAP and RESCHEDULE - will be implemented later
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

        // Use resource from approveDTO if provided, otherwise use from request
        if (approveDTO.getNewResourceId() != null) {
            newResource = resourceRepository.findById(approveDTO.getNewResourceId())
                    .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
        } else if (request.getNewResource() != null) {
            newResource = request.getNewResource();
        } else {
            throw new CustomException(ErrorCode.INVALID_RESOURCE_FOR_MODALITY);
        }

        // Validate resource availability at session date + time
        // Exclude current session when checking (we're replacing its resource)
        validateResourceAvailability(newResource.getId(), session.getDate(), 
                session.getTimeSlotTemplate().getId(), session.getId());

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

    

    /**
     * Map TeacherRequest entity to ResponseDTO
     */
    private TeacherRequestResponseDTO mapToResponseDTO(TeacherRequest request) {
        return TeacherRequestResponseDTO.builder()
                .id(request.getId())
                .requestType(request.getRequestType())
                .status(request.getStatus())
                .sessionId(request.getSession() != null ? request.getSession().getId() : null)
                .replacementTeacherId(request.getReplacementTeacher() != null ? 
                        request.getReplacementTeacher().getId() : null)
                .newDate(request.getNewDate())
                .newTimeSlotId(request.getNewTimeSlot() != null ? 
                        request.getNewTimeSlot().getId() : null)
                .newResourceId(request.getNewResource() != null ? 
                        request.getNewResource().getId() : null)
                .requestReason(request.getRequestReason())
                .note(request.getNote())
                .submittedAt(request.getSubmittedAt())
                .decidedAt(request.getDecidedAt())
                .newSessionId(request.getNewSession() != null ? 
                        request.getNewSession().getId() : null)
                .build();
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
                .submittedAt(request.getSubmittedAt())
                .decidedAt(request.getDecidedAt())
                .build();
    }
}
