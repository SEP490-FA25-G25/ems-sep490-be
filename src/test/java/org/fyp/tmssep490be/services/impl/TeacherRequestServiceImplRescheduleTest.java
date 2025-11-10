package org.fyp.tmssep490be.services.impl;

import org.fyp.tmssep490be.dtos.teacherrequest.RescheduleResourceSuggestionDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.RescheduleSlotSuggestionDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestApproveDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestCreateDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestResponseDTO;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.*;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.*;
import org.fyp.tmssep490be.services.TeacherRequestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@org.junit.jupiter.api.DisplayName("TeacherRequestService Reschedule Unit Tests")
class TeacherRequestServiceImplRescheduleTest {

    @Autowired
    private TeacherRequestService service;

    @MockitoBean private TeacherRequestRepository teacherRequestRepository;
    @MockitoBean private TeacherRepository teacherRepository;
    @MockitoBean private SessionRepository sessionRepository;
    @MockitoBean private ResourceRepository resourceRepository;
    @MockitoBean private SessionResourceRepository sessionResourceRepository;
    @MockitoBean private TeachingSlotRepository teachingSlotRepository;
    @MockitoBean private UserAccountRepository userAccountRepository;
    @MockitoBean private TimeSlotTemplateRepository timeSlotTemplateRepository;
    @MockitoBean private StudentSessionRepository studentSessionRepository;
    @MockitoBean private ClassRepository classRepository;

    private Teacher mockTeacher(Long id, Long userId) {
        Teacher t = new Teacher();
        t.setId(id);
        UserAccount ua = new UserAccount();
        ua.setId(userId);
        t.setUserAccount(ua);
        return t;
    }

    private Session mockSession(Long id, ClassEntity classEntity, TimeSlotTemplate timeSlot, LocalDate date) {
        Session s = new Session();
        s.setId(id);
        s.setClassEntity(classEntity);
        s.setTimeSlotTemplate(timeSlot);
        s.setDate(date);
        s.setStatus(SessionStatus.PLANNED);
        return s;
    }

    private ClassEntity mockClass(Modality modality) {
        ClassEntity ce = new ClassEntity();
        ce.setId(111L);
        ce.setCode("C-001");
        ce.setModality(modality);
        Branch b = new Branch();
        b.setId(1L);
        ce.setBranch(b);
        return ce;
    }

    private TimeSlotTemplate mockTimeSlot(Long id) {
        TimeSlotTemplate t = new TimeSlotTemplate();
        t.setId(id);
        t.setName("Morning");
        return t;
    }

    private Resource mockResource(Long id, ResourceType type) {
        Resource r = new Resource();
        r.setId(id);
        r.setName(type == ResourceType.VIRTUAL ? "Zoom-1" : "Room-101");
        r.setResourceType(type);
        r.setCapacity(50);
        Branch b = new Branch();
        b.setId(1L);
        r.setBranch(b);
        return r;
    }

    @Test
    @org.junit.jupiter.api.DisplayName("createRequest - reschedule - success")
    void createRequest_reschedule_success() {
        Long userId = 10L;
        Long teacherId = 20L;
        Long sessionId = 30L;
        Long newTimeSlotId = 5L;
        Long newResourceId = 40L;
        LocalDate newDate = LocalDate.now().plusDays(2);

        Teacher teacher = mockTeacher(teacherId, userId);
        ClassEntity classEntity = mockClass(Modality.OFFLINE);
        TimeSlotTemplate timeSlot = mockTimeSlot(3L);
        Session session = mockSession(sessionId, classEntity, timeSlot, LocalDate.now().plusDays(1));
        Resource resource = mockResource(newResourceId, ResourceType.VIRTUAL);
        TimeSlotTemplate newTimeSlot = mockTimeSlot(newTimeSlotId);

        when(teacherRepository.findByUserAccountId(userId)).thenReturn(Optional.of(teacher));
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(teachingSlotRepository.existsByIdSessionIdAndIdTeacherIdAndStatusIn(eq(sessionId), eq(teacherId), anyList()))
                .thenReturn(true);
        when(teacherRequestRepository.existsBySessionIdAndRequestTypeAndStatus(eq(sessionId), eq(TeacherRequestType.RESCHEDULE), eq(RequestStatus.PENDING)))
                .thenReturn(false);
        UserAccount ua = new UserAccount();
        ua.setId(userId);
        when(userAccountRepository.findById(userId)).thenReturn(Optional.of(ua));
        when(timeSlotTemplateRepository.findById(newTimeSlotId)).thenReturn(Optional.of(newTimeSlot));
        when(resourceRepository.findById(newResourceId)).thenReturn(Optional.of(resource));
        when(sessionResourceRepository.existsByResourceIdAndDateAndTimeSlotAndStatusIn(eq(newResourceId), eq(newDate), eq(newTimeSlotId), anyList(), isNull()))
                .thenReturn(false);
        when(teachingSlotRepository.findAll()).thenReturn(Arrays.asList()); // No teacher conflicts
        when(studentSessionRepository.findAll()).thenReturn(Arrays.asList()); // No student conflicts
        when(teacherRequestRepository.save(any(TeacherRequest.class))).thenAnswer(invocation -> {
            TeacherRequest tr = invocation.getArgument(0);
            tr.setId(999L);
            return tr;
        });

        TeacherRequestCreateDTO dto = TeacherRequestCreateDTO.builder()
                .sessionId(sessionId)
                .requestType(TeacherRequestType.RESCHEDULE)
                .newDate(newDate)
                .newTimeSlotId(newTimeSlotId)
                .newResourceId(newResourceId)
                .reason("Need to reschedule")
                .build();

        TeacherRequestResponseDTO resp = service.createRequest(dto, userId);

        assertThat(resp.getId()).isEqualTo(999L);
        assertThat(resp.getRequestType()).isEqualTo(TeacherRequestType.RESCHEDULE);
        assertThat(resp.getStatus()).isEqualTo(RequestStatus.PENDING);
        verify(teacherRequestRepository).save(any(TeacherRequest.class));
    }

    @Test
    @org.junit.jupiter.api.DisplayName("createRequest - reschedule - missing required fields - throws")
    void createRequest_reschedule_missingFields_throws() {
        Long userId = 10L;
        Long teacherId = 20L;
        Long sessionId = 30L;

        Teacher teacher = mockTeacher(teacherId, userId);
        ClassEntity classEntity = mockClass(Modality.OFFLINE);
        TimeSlotTemplate timeSlot = mockTimeSlot(3L);
        Session session = mockSession(sessionId, classEntity, timeSlot, LocalDate.now().plusDays(1));

        when(teacherRepository.findByUserAccountId(userId)).thenReturn(Optional.of(teacher));
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(teachingSlotRepository.existsByIdSessionIdAndIdTeacherIdAndStatusIn(eq(sessionId), eq(teacherId), anyList()))
                .thenReturn(true);

        // Missing newDate
        TeacherRequestCreateDTO dto1 = TeacherRequestCreateDTO.builder()
                .sessionId(sessionId)
                .requestType(TeacherRequestType.RESCHEDULE)
                .newTimeSlotId(5L)
                .newResourceId(40L)
                .reason("Test")
                .build();

        assertThatThrownBy(() -> service.createRequest(dto1, userId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @org.junit.jupiter.api.DisplayName("suggestSlots - returns available time slots")
    void suggestSlots_returnsAvailableSlots() {
        Long userId = 10L;
        Long teacherId = 20L;
        Long sessionId = 30L;
        LocalDate date = LocalDate.now().plusDays(2);

        Teacher teacher = mockTeacher(teacherId, userId);
        ClassEntity classEntity = mockClass(Modality.OFFLINE);
        TimeSlotTemplate timeSlot = mockTimeSlot(3L);
        Session session = mockSession(sessionId, classEntity, timeSlot, LocalDate.now().plusDays(1));

        TimeSlotTemplate slot1 = mockTimeSlot(5L);
        TimeSlotTemplate slot2 = mockTimeSlot(6L);

        when(teacherRepository.findByUserAccountId(userId)).thenReturn(Optional.of(teacher));
        when(sessionRepository.existsById(sessionId)).thenReturn(true);
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(teachingSlotRepository.existsByIdSessionIdAndIdTeacherIdAndStatusIn(eq(sessionId), eq(teacherId), anyList()))
                .thenReturn(true);
        when(timeSlotTemplateRepository.findAll()).thenReturn(Arrays.asList(slot1, slot2));
        when(teachingSlotRepository.findAll()).thenReturn(Arrays.asList()); // No conflicts
        when(studentSessionRepository.findAll()).thenReturn(Arrays.asList()); // No student conflicts

        List<RescheduleSlotSuggestionDTO> slots = service.suggestSlots(sessionId, date, userId);

        assertThat(slots).isNotEmpty();
        assertThat(slots).extracting("timeSlotId").contains(5L, 6L);
    }

    @Test
    @org.junit.jupiter.api.DisplayName("suggestResources - returns available resources")
    void suggestResources_returnsAvailableResources() {
        Long userId = 10L;
        Long teacherId = 20L;
        Long sessionId = 30L;
        Long timeSlotId = 5L;
        LocalDate date = LocalDate.now().plusDays(2);

        Teacher teacher = mockTeacher(teacherId, userId);
        ClassEntity classEntity = mockClass(Modality.OFFLINE);
        TimeSlotTemplate timeSlot = mockTimeSlot(3L);
        Session session = mockSession(sessionId, classEntity, timeSlot, LocalDate.now().plusDays(1));
        // For RESCHEDULE, OFFLINE class needs ROOM resource (not VIRTUAL)
        Resource resource1 = mockResource(40L, ResourceType.ROOM);
        Resource resource2 = mockResource(41L, ResourceType.ROOM);

        when(teacherRepository.findByUserAccountId(userId)).thenReturn(Optional.of(teacher));
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(classRepository.findById(classEntity.getId())).thenReturn(Optional.of(classEntity));
        when(teachingSlotRepository.existsByIdSessionIdAndIdTeacherIdAndStatusIn(eq(sessionId), eq(teacherId), anyList()))
                .thenReturn(true);
        when(resourceRepository.findAll()).thenReturn(Arrays.asList(resource1, resource2));
        when(sessionResourceRepository.existsByResourceIdAndDateAndTimeSlotAndStatusIn(anyLong(), eq(date), eq(timeSlotId), anyList(), isNull()))
                .thenReturn(false); // No resource conflicts
        when(studentSessionRepository.countBySessionId(sessionId)).thenReturn(10L); // 10 students
        when(teachingSlotRepository.findAll()).thenReturn(Arrays.asList()); // No teacher conflicts
        when(studentSessionRepository.findAll()).thenReturn(Arrays.asList()); // No student conflicts

        List<RescheduleResourceSuggestionDTO> resources = service.suggestResources(sessionId, date, timeSlotId, userId);

        assertThat(resources).isNotEmpty();
        assertThat(resources).extracting("resourceId").contains(40L, 41L);
    }

    @Test
    @org.junit.jupiter.api.DisplayName("approveRequest - reschedule - creates new session and cancels old")
    void approveRequest_reschedule_createsNewSession() {
        Long staffId = 99L;
        Long requestId = 777L;
        Long sessionId = 30L;
        Long teacherId = 20L;
        Long newTimeSlotId = 5L;
        Long newResourceId = 40L;
        LocalDate newDate = LocalDate.now().plusDays(3);

        Teacher teacher = mockTeacher(teacherId, 10L);
        ClassEntity classEntity = mockClass(Modality.OFFLINE);
        TimeSlotTemplate oldTimeSlot = mockTimeSlot(3L);
        TimeSlotTemplate newTimeSlot = mockTimeSlot(newTimeSlotId);
        Session oldSession = mockSession(sessionId, classEntity, oldTimeSlot, LocalDate.now().plusDays(1));
        Resource newResource = mockResource(newResourceId, ResourceType.VIRTUAL);

        TeacherRequest tr = TeacherRequest.builder()
                .id(requestId)
                .teacher(teacher)
                .session(oldSession)
                .requestType(TeacherRequestType.RESCHEDULE)
                .status(RequestStatus.PENDING)
                .newDate(newDate)
                .newTimeSlot(newTimeSlot)
                .newResource(newResource)
                .build();

        when(teacherRequestRepository.findByIdWithTeacherAndSession(requestId)).thenReturn(Optional.of(tr));
        UserAccount staff = new UserAccount();
        staff.setId(staffId);
        when(userAccountRepository.findById(staffId)).thenReturn(Optional.of(staff));
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(oldSession));
        when(timeSlotTemplateRepository.findById(newTimeSlotId)).thenReturn(Optional.of(newTimeSlot));
        when(resourceRepository.findById(newResourceId)).thenReturn(Optional.of(newResource));
        when(teachingSlotRepository.existsById(any(TeachingSlot.TeachingSlotId.class))).thenReturn(true);
        when(teachingSlotRepository.findAll()).thenReturn(Arrays.asList()); // No conflicts
        when(sessionResourceRepository.existsByResourceIdAndDateAndTimeSlotAndStatusIn(eq(newResourceId), eq(newDate), eq(newTimeSlotId), anyList(), isNull()))
                .thenReturn(false);
        when(studentSessionRepository.findAll()).thenReturn(Arrays.asList()); // No student sessions to copy
        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> {
            Session s = invocation.getArgument(0);
            if (s.getStatus() == SessionStatus.PLANNED) {
                s.setId(888L); // New session ID
            }
            return s;
        });
        when(teachingSlotRepository.save(any(TeachingSlot.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sessionResourceRepository.save(any(SessionResource.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(teacherRequestRepository.save(any(TeacherRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TeacherRequestApproveDTO approve = TeacherRequestApproveDTO.builder()
                .note("Approved")
                .build();

        TeacherRequestResponseDTO resp = service.approveRequest(requestId, approve, staffId);

        assertThat(resp.getStatus()).isEqualTo(RequestStatus.APPROVED);
        verify(sessionRepository).save(argThat(s -> s.getStatus() == SessionStatus.PLANNED && s.getDate().equals(newDate)));
        verify(sessionRepository).save(argThat(s -> s.getId().equals(sessionId) && s.getStatus() == SessionStatus.CANCELLED));
    }
}

