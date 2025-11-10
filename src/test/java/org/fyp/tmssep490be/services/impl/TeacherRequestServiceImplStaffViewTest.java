package org.fyp.tmssep490be.services.impl;

import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestListDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestResponseDTO;
import org.fyp.tmssep490be.entities.ClassEntity;
import org.fyp.tmssep490be.entities.Session;
import org.fyp.tmssep490be.entities.Teacher;
import org.fyp.tmssep490be.entities.TeacherRequest;
import org.fyp.tmssep490be.entities.TimeSlotTemplate;
import org.fyp.tmssep490be.entities.UserAccount;
import org.fyp.tmssep490be.entities.enums.Modality;
import org.fyp.tmssep490be.entities.enums.RequestStatus;
import org.fyp.tmssep490be.entities.enums.SessionStatus;
import org.fyp.tmssep490be.entities.enums.TeacherRequestType;
import org.fyp.tmssep490be.repositories.*;
import org.fyp.tmssep490be.services.TeacherRequestService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("TeacherRequestService staff view tests")
class TeacherRequestServiceImplStaffViewTest {

    @Autowired
    private TeacherRequestService teacherRequestService;

    @MockitoBean private TeacherRequestRepository teacherRequestRepository;
    @MockitoBean private TeacherRepository teacherRepository;
    @MockitoBean private SessionRepository sessionRepository;
    @MockitoBean private ResourceRepository resourceRepository;
    @MockitoBean private SessionResourceRepository sessionResourceRepository;
    @MockitoBean private TeachingSlotRepository teachingSlotRepository;
    @MockitoBean private UserAccountRepository userAccountRepository;
    @MockitoBean private TimeSlotTemplateRepository timeSlotTemplateRepository;
    @MockitoBean private StudentSessionRepository studentSessionRepository;

    @Test
    void getPendingRequestsForStaff_shouldReturnTeacherAndClassInfo() {
        TeacherRequest request = mockRequest(RequestStatus.PENDING);
        when(teacherRequestRepository.findByStatusOrderBySubmittedAtDesc(RequestStatus.PENDING))
                .thenReturn(List.of(request));

        List<TeacherRequestListDTO> result = teacherRequestService.getPendingRequestsForStaff();

        assertThat(result).hasSize(1);
        TeacherRequestListDTO dto = result.get(0);
        assertThat(dto.getTeacherName()).isEqualTo("Teacher One");
        assertThat(dto.getTeacherEmail()).isEqualTo("teacher1@tms.test");
        assertThat(dto.getClassCode()).isEqualTo("CLS-001");
        assertThat(dto.getSessionDate()).isEqualTo(request.getSession().getDate());

        verify(teacherRequestRepository).findByStatusOrderBySubmittedAtDesc(RequestStatus.PENDING);
        verify(teacherRequestRepository, never()).findAllByOrderBySubmittedAtDesc();
    }

    @Test
    void getRequestsForStaff_withoutStatus_shouldFallbackToAll() {
        TeacherRequest request = mockRequest(RequestStatus.APPROVED);
        when(teacherRequestRepository.findAllByOrderBySubmittedAtDesc())
                .thenReturn(List.of(request));

        List<TeacherRequestListDTO> result = teacherRequestService.getRequestsForStaff(null);

        assertThat(result).hasSize(1);
        TeacherRequestListDTO dto = result.get(0);
        assertThat(dto.getStatus()).isEqualTo(RequestStatus.APPROVED);
        assertThat(dto.getTeacherId()).isEqualTo(request.getTeacher().getId());

        verify(teacherRequestRepository).findAllByOrderBySubmittedAtDesc();
        verify(teacherRequestRepository, never()).findByStatusOrderBySubmittedAtDesc(any());
    }

    @Test
    void getRequestForStaff_shouldReturnDetailedInfo() {
        TeacherRequest request = mockRequest(RequestStatus.PENDING);
        when(teacherRequestRepository.findByIdWithTeacherAndSession(99L))
                .thenReturn(Optional.of(request));

        TeacherRequestResponseDTO response = teacherRequestService.getRequestForStaff(99L);

        assertThat(response.getId()).isEqualTo(request.getId());
        assertThat(response.getTeacherId()).isEqualTo(request.getTeacher().getId());
        assertThat(response.getTeacherName()).isEqualTo("Teacher One");
        assertThat(response.getClassCode()).isEqualTo("CLS-001");
        assertThat(response.getSessionDate()).isEqualTo(request.getSession().getDate());

        verify(teacherRequestRepository).findByIdWithTeacherAndSession(99L);
    }

    private TeacherRequest mockRequest(RequestStatus status) {
        UserAccount teacherAccount = new UserAccount();
        teacherAccount.setId(10L);
        teacherAccount.setEmail("teacher1@tms.test");
        teacherAccount.setFullName("Teacher One");

        Teacher teacher = new Teacher();
        teacher.setId(20L);
        teacher.setUserAccount(teacherAccount);

        ClassEntity classEntity = new ClassEntity();
        classEntity.setId(30L);
        classEntity.setCode("CLS-001");
        classEntity.setName("Class A");
        classEntity.setModality(Modality.OFFLINE);

        TimeSlotTemplate timeSlot = new TimeSlotTemplate();
        timeSlot.setId(40L);
        timeSlot.setName("Morning");

        Session session = new Session();
        session.setId(50L);
        session.setClassEntity(classEntity);
        session.setTimeSlotTemplate(timeSlot);
        session.setStatus(SessionStatus.PLANNED);
        session.setDate(LocalDate.now().plusDays(1));

        return TeacherRequest.builder()
                .id(60L)
                .teacher(teacher)
                .session(session)
                .requestType(TeacherRequestType.RESCHEDULE)
                .status(status)
                .submittedAt(OffsetDateTime.now().minusHours(1))
                .requestReason("Need to adjust schedule")
                .build();
    }
}


