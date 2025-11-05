package org.fyp.tmssep490be.services.impl;

import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestApproveDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestCreateDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestResponseDTO;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.*;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.repositories.*;
import org.fyp.tmssep490be.services.TeacherRequestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@org.junit.jupiter.api.DisplayName("TeacherRequestService Modality Unit Tests")
class TeacherRequestServiceImplModalityTest {

    @Autowired
    private TeacherRequestService service;

    @MockitoBean private TeacherRequestRepository teacherRequestRepository;
    @MockitoBean private TeacherRepository teacherRepository;
    @MockitoBean private SessionRepository sessionRepository;
    @MockitoBean private ResourceRepository resourceRepository;
    @MockitoBean private SessionResourceRepository sessionResourceRepository;
    @MockitoBean private ClassRepository classRepository;
    @MockitoBean private TeachingSlotRepository teachingSlotRepository;
    @MockitoBean private UserAccountRepository userAccountRepository;

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
        return r;
    }

    @Test
    @org.junit.jupiter.api.DisplayName("createRequest - modality change - success")
    void createRequest_modality_success() {
        Long userId = 10L;
        Long teacherId = 20L;
        Long sessionId = 30L;
        Long resourceId = 40L;

        Teacher teacher = mockTeacher(teacherId, userId);
        ClassEntity classEntity = mockClass(Modality.ONLINE);
        TimeSlotTemplate timeSlot = mockTimeSlot(5L);
        Session session = mockSession(sessionId, classEntity, timeSlot, LocalDate.now().plusDays(2));
        Resource resource = mockResource(resourceId, ResourceType.VIRTUAL);

        when(teacherRepository.findByUserAccountId(userId)).thenReturn(Optional.of(teacher));
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(teachingSlotRepository.existsByIdSessionIdAndIdTeacherIdAndStatusIn(eq(sessionId), eq(teacherId), anyList()))
                .thenReturn(true);
        when(teacherRequestRepository.existsBySessionIdAndRequestTypeAndStatus(eq(sessionId), eq(TeacherRequestType.MODALITY_CHANGE), eq(RequestStatus.PENDING)))
                .thenReturn(false);
        UserAccount ua = new UserAccount(); ua.setId(userId);
        when(userAccountRepository.findById(userId)).thenReturn(Optional.of(ua));
        when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(resource));
        when(teacherRequestRepository.save(any(TeacherRequest.class))).thenAnswer(invocation -> {
            TeacherRequest tr = invocation.getArgument(0);
            tr.setId(999L);
            return tr;
        });

        TeacherRequestCreateDTO dto = TeacherRequestCreateDTO.builder()
                .sessionId(sessionId)
                .requestType(TeacherRequestType.MODALITY_CHANGE)
                .newResourceId(resourceId)
                .reason("Need to switch to online")
                .build();

        TeacherRequestResponseDTO resp = service.createRequest(dto, userId);

        assertThat(resp.getId()).isEqualTo(999L);
        assertThat(resp.getRequestType()).isEqualTo(TeacherRequestType.MODALITY_CHANGE);
        assertThat(resp.getStatus()).isEqualTo(RequestStatus.PENDING);
        verify(teacherRequestRepository).save(any(TeacherRequest.class));
    }

    @Test
    @org.junit.jupiter.api.DisplayName("createRequest - missing newResourceId - throws INVALID_INPUT")
    void createRequest_modality_missingResource_throwsInvalidInput() {
        Long userId = 10L;
        Long teacherId = 20L;
        Long sessionId = 30L;

        Teacher teacher = mockTeacher(teacherId, userId);
        ClassEntity classEntity = mockClass(Modality.ONLINE);
        TimeSlotTemplate timeSlot = mockTimeSlot(5L);
        Session session = mockSession(sessionId, classEntity, timeSlot, LocalDate.now().plusDays(2));

        when(teacherRepository.findByUserAccountId(userId)).thenReturn(Optional.of(teacher));
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(teachingSlotRepository.existsByIdSessionIdAndIdTeacherIdAndStatusIn(eq(sessionId), eq(teacherId), anyList()))
                .thenReturn(true);

        TeacherRequestCreateDTO dto = TeacherRequestCreateDTO.builder()
                .sessionId(sessionId)
                .requestType(TeacherRequestType.MODALITY_CHANGE)
                .reason("Need to switch")
                .build();

        assertThatThrownBy(() -> service.createRequest(dto, userId))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Invalid input");
        verify(teacherRequestRepository, never()).save(any());
    }

    @Test
    @org.junit.jupiter.api.DisplayName("approveRequest - modality - updates session_resource only")
    void approveRequest_modality_success_updatesResourceOnly() {
        Long staffId = 99L;
        Long requestId = 777L;
        Long sessionId = 30L;
        Long newResourceId = 123L;

        ClassEntity classEntity = mockClass(Modality.OFFLINE);
        TimeSlotTemplate timeSlot = mockTimeSlot(5L);
        Session session = mockSession(sessionId, classEntity, timeSlot, LocalDate.now().plusDays(2));
        Resource newResource = mockResource(newResourceId, ResourceType.VIRTUAL);

        Teacher teacher = new Teacher(); teacher.setId(20L);
        TeacherRequest tr = TeacherRequest.builder()
                .id(requestId)
                .teacher(teacher)
                .session(session)
                .requestType(TeacherRequestType.MODALITY_CHANGE)
                .status(RequestStatus.PENDING)
                .build();

        when(teacherRequestRepository.findByIdWithTeacherAndSession(requestId)).thenReturn(Optional.of(tr));
        UserAccount staff = new UserAccount(); staff.setId(staffId);
        when(userAccountRepository.findById(staffId)).thenReturn(Optional.of(staff));
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(resourceRepository.findById(newResourceId)).thenReturn(Optional.of(newResource));
        when(sessionResourceRepository.existsByResourceIdAndDateAndTimeSlotAndStatusIn(eq(newResourceId), any(), eq(timeSlot.getId()), anyList(), eq(sessionId)))
                .thenReturn(false);
        when(teacherRequestRepository.save(any(TeacherRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TeacherRequestApproveDTO approve = TeacherRequestApproveDTO.builder()
                .newResourceId(newResourceId)
                .note("ok")
                .build();

        TeacherRequestResponseDTO resp = service.approveRequest(requestId, approve, staffId);

        assertThat(resp.getStatus()).isEqualTo(RequestStatus.APPROVED);
        verify(sessionResourceRepository).deleteBySessionId(sessionId);
        verify(sessionResourceRepository).save(any(SessionResource.class));
        verify(classRepository, never()).save(any(ClassEntity.class));
    }

    @Test
    @org.junit.jupiter.api.DisplayName("approveRequest - not pending - throws")
    void approveRequest_notPending_throws() {
        Long staffId = 99L;
        Long requestId = 888L;
        TeacherRequest tr = TeacherRequest.builder()
                .id(requestId)
                .teacher(new Teacher())
                .status(RequestStatus.APPROVED)
                .requestType(TeacherRequestType.MODALITY_CHANGE)
                .build();
        when(teacherRequestRepository.findByIdWithTeacherAndSession(requestId)).thenReturn(Optional.of(tr));

        assertThatThrownBy(() -> service.approveRequest(requestId, TeacherRequestApproveDTO.builder().build(), staffId))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("pending");
    }

    @Test
    @org.junit.jupiter.api.DisplayName("rejectRequest - sets REJECTED and note")
    void rejectRequest_setsRejected() {
        Long staffId = 99L;
        Long requestId = 889L;
        TeacherRequest tr = TeacherRequest.builder()
                .id(requestId)
                .teacher(new Teacher())
                .status(RequestStatus.PENDING)
                .requestType(TeacherRequestType.MODALITY_CHANGE)
                .build();
        when(teacherRequestRepository.findByIdWithTeacherAndSession(requestId)).thenReturn(Optional.of(tr));
        UserAccount staff = new UserAccount(); staff.setId(staffId);
        when(userAccountRepository.findById(staffId)).thenReturn(Optional.of(staff));
        when(teacherRequestRepository.save(any(TeacherRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        TeacherRequestResponseDTO resp = service.rejectRequest(requestId, "not appropriate", staffId);
        assertThat(resp.getStatus()).isEqualTo(RequestStatus.REJECTED);
        assertThat(resp.getNote()).contains("not appropriate");
    }
}


