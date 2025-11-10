package org.fyp.tmssep490be.services.impl;

import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestListDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestResponseDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherSessionDTO;
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
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@org.junit.jupiter.api.DisplayName("TeacherRequestService View Tests")
class TeacherRequestServiceImplViewTest {

    @Autowired
    private TeacherRequestService service;

    @MockitoBean private TeacherRequestRepository teacherRequestRepository;
    @MockitoBean private TeacherRepository teacherRepository;
    @MockitoBean private SessionRepository sessionRepository;
    @MockitoBean private TeachingSlotRepository teachingSlotRepository;
    @MockitoBean private UserAccountRepository userAccountRepository;
    @MockitoBean private TimeSlotTemplateRepository timeSlotTemplateRepository;

    private Teacher mockTeacher(Long id, Long userId) {
        Teacher t = new Teacher();
        t.setId(id);
        UserAccount ua = new UserAccount();
        ua.setId(userId);
        ua.setFullName("Teacher " + id);
        ua.setEmail("teacher" + id + "@tms-edu.vn");
        t.setUserAccount(ua);
        t.setContractType("full-time");
        return t;
    }

    private Session mockSession(Long id, ClassEntity classEntity, TimeSlotTemplate timeSlot, LocalDate date) {
        Session s = new Session();
        s.setId(id);
        s.setClassEntity(classEntity);
        s.setTimeSlotTemplate(timeSlot);
        s.setDate(date);
        s.setStatus(SessionStatus.PLANNED);
        CourseSession cs = new CourseSession();
        cs.setSkillSet(new Skill[]{Skill.SPEAKING});
        cs.setTopic("Test Topic");
        s.setCourseSession(cs);
        return s;
    }

    private ClassEntity mockClass() {
        ClassEntity ce = new ClassEntity();
        ce.setId(111L);
        ce.setCode("C-001");
        ce.setName("Test Class");
        ce.setModality(Modality.OFFLINE);
        Branch b = new Branch();
        b.setId(1L);
        ce.setBranch(b);
        Course course = new Course();
        course.setId(1L);
        course.setName("Test Course");
        ce.setCourse(course);
        return ce;
    }

    private TimeSlotTemplate mockTimeSlot(Long id) {
        TimeSlotTemplate t = new TimeSlotTemplate();
        t.setId(id);
        t.setName("Morning");
        t.setStartTime(LocalTime.of(9, 0));
        t.setEndTime(LocalTime.of(11, 0));
        return t;
    }

    @Test
    @org.junit.jupiter.api.DisplayName("getMyRequests - teacher sees own created requests")
    void getMyRequests_teacherSeesOwnRequests() {
        Long teacherId = 10L;
        Long userId = 20L;
        Teacher teacher = mockTeacher(teacherId, userId);

        TeacherRequest request1 = TeacherRequest.builder()
                .id(1L)
                .teacher(teacher)
                .requestType(TeacherRequestType.MODALITY_CHANGE)
                .status(RequestStatus.PENDING)
                .submittedAt(OffsetDateTime.now())
                .build();

        TeacherRequest request2 = TeacherRequest.builder()
                .id(2L)
                .teacher(teacher)
                .requestType(TeacherRequestType.RESCHEDULE)
                .status(RequestStatus.APPROVED)
                .submittedAt(OffsetDateTime.now().minusDays(1))
                .build();

        when(teacherRepository.findByUserAccountId(userId)).thenReturn(Optional.of(teacher));
        when(teacherRequestRepository.findByTeacherIdOrderBySubmittedAtDesc(teacherId))
                .thenReturn(Arrays.asList(request1, request2));
        when(teacherRequestRepository.findByReplacementTeacherIdOrderBySubmittedAtDesc(teacherId))
                .thenReturn(List.of());

        List<TeacherRequestListDTO> result = service.getMyRequests(userId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(1).getId()).isEqualTo(2L);
    }

    @Test
    @org.junit.jupiter.api.DisplayName("getMyRequests - replacement teacher sees requests waiting for confirmation")
    void getMyRequests_replacementTeacherSeesWaitingConfirmRequests() {
        Long replacementTeacherId = 20L;
        Long replacementUserId = 30L;
        Teacher replacementTeacher = mockTeacher(replacementTeacherId, replacementUserId);

        Teacher originalTeacher = mockTeacher(10L, 15L);

        TeacherRequest swapRequest = TeacherRequest.builder()
                .id(100L)
                .teacher(originalTeacher)
                .replacementTeacher(replacementTeacher)
                .requestType(TeacherRequestType.SWAP)
                .status(RequestStatus.WAITING_CONFIRM)
                .submittedAt(OffsetDateTime.now())
                .build();

        when(teacherRepository.findByUserAccountId(replacementUserId)).thenReturn(Optional.of(replacementTeacher));
        when(teacherRequestRepository.findByTeacherIdOrderBySubmittedAtDesc(replacementTeacherId))
                .thenReturn(List.of());
        when(teacherRequestRepository.findByReplacementTeacherIdOrderBySubmittedAtDesc(replacementTeacherId))
                .thenReturn(List.of(swapRequest));

        List<TeacherRequestListDTO> result = service.getMyRequests(replacementUserId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(100L);
        assertThat(result.get(0).getStatus()).isEqualTo(RequestStatus.WAITING_CONFIRM);
    }

    @Test
    @org.junit.jupiter.api.DisplayName("getMyRequests - teacher sees both own requests and replacement requests (deduplicate)")
    void getMyRequests_teacherSeesBothOwnAndReplacementRequests() {
        Long teacherId = 10L;
        Long userId = 20L;
        Teacher teacher = mockTeacher(teacherId, userId);

        TeacherRequest ownRequest = TeacherRequest.builder()
                .id(1L)
                .teacher(teacher)
                .requestType(TeacherRequestType.MODALITY_CHANGE)
                .status(RequestStatus.PENDING)
                .submittedAt(OffsetDateTime.now())
                .build();

        Teacher otherTeacher = mockTeacher(50L, 60L);
        TeacherRequest replacementRequest = TeacherRequest.builder()
                .id(2L)
                .teacher(otherTeacher)
                .replacementTeacher(teacher)
                .requestType(TeacherRequestType.SWAP)
                .status(RequestStatus.WAITING_CONFIRM)
                .submittedAt(OffsetDateTime.now().minusHours(1))
                .build();

        when(teacherRepository.findByUserAccountId(userId)).thenReturn(Optional.of(teacher));
        when(teacherRequestRepository.findByTeacherIdOrderBySubmittedAtDesc(teacherId))
                .thenReturn(List.of(ownRequest));
        when(teacherRequestRepository.findByReplacementTeacherIdOrderBySubmittedAtDesc(teacherId))
                .thenReturn(List.of(replacementRequest));

        List<TeacherRequestListDTO> result = service.getMyRequests(userId);

        assertThat(result).hasSize(2);
        // Should be sorted by submittedAt desc (newest first)
        assertThat(result.get(0).getId()).isEqualTo(1L); // ownRequest (newer)
        assertThat(result.get(1).getId()).isEqualTo(2L); // replacementRequest (older)
    }

    @Test
    @org.junit.jupiter.api.DisplayName("getRequestById - teacher can see own request")
    void getRequestById_teacherCanSeeOwnRequest() {
        Long teacherId = 10L;
        Long userId = 20L;
        Teacher teacher = mockTeacher(teacherId, userId);

        Session session = mockSession(100L, mockClass(), mockTimeSlot(1L), LocalDate.now().plusDays(1));
        TeacherRequest request = TeacherRequest.builder()
                .id(1L)
                .teacher(teacher)
                .session(session)
                .requestType(TeacherRequestType.MODALITY_CHANGE)
                .status(RequestStatus.PENDING)
                .build();

        when(teacherRequestRepository.findByIdWithTeacherAndSession(1L)).thenReturn(Optional.of(request));
        when(teacherRepository.findByUserAccountId(userId)).thenReturn(Optional.of(teacher));

        TeacherRequestResponseDTO result = service.getRequestById(1L, userId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @org.junit.jupiter.api.DisplayName("getRequestById - replacement teacher can see request they are assigned to")
    void getRequestById_replacementTeacherCanSeeRequest() {
        Long replacementTeacherId = 20L;
        Long replacementUserId = 30L;
        Teacher replacementTeacher = mockTeacher(replacementTeacherId, replacementUserId);

        Teacher originalTeacher = mockTeacher(10L, 15L);
        Session session = mockSession(100L, mockClass(), mockTimeSlot(1L), LocalDate.now().plusDays(1));

        TeacherRequest request = TeacherRequest.builder()
                .id(100L)
                .teacher(originalTeacher)
                .replacementTeacher(replacementTeacher)
                .session(session)
                .requestType(TeacherRequestType.SWAP)
                .status(RequestStatus.WAITING_CONFIRM)
                .build();

        when(teacherRequestRepository.findByIdWithTeacherAndSession(100L)).thenReturn(Optional.of(request));
        when(teacherRepository.findByUserAccountId(replacementUserId)).thenReturn(Optional.of(replacementTeacher));

        TeacherRequestResponseDTO result = service.getRequestById(100L, replacementUserId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getReplacementTeacherId()).isEqualTo(replacementTeacherId);
    }

    @Test
    @org.junit.jupiter.api.DisplayName("getRequestById - teacher cannot see other teacher's request")
    void getRequestById_teacherCannotSeeOtherTeacherRequest() {
        Long teacherId = 10L;
        Long userId = 20L;
        Teacher teacher = mockTeacher(teacherId, userId);

        Teacher otherTeacher = mockTeacher(50L, 60L);
        Session session = mockSession(100L, mockClass(), mockTimeSlot(1L), LocalDate.now().plusDays(1));

        TeacherRequest request = TeacherRequest.builder()
                .id(1L)
                .teacher(otherTeacher)
                .session(session)
                .requestType(TeacherRequestType.MODALITY_CHANGE)
                .status(RequestStatus.PENDING)
                .build();

        when(teacherRequestRepository.findByIdWithTeacherAndSession(1L)).thenReturn(Optional.of(request));
        when(teacherRepository.findByUserAccountId(userId)).thenReturn(Optional.of(teacher));

        assertThatThrownBy(() -> service.getRequestById(1L, userId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @org.junit.jupiter.api.DisplayName("getMyFutureSessions - returns sessions in next 7 days")
    void getMyFutureSessions_returnsNext7Days() {
        Long teacherId = 10L;
        Long userId = 20L;
        Teacher teacher = mockTeacher(teacherId, userId);

        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        LocalDate day3 = today.plusDays(3);

        ClassEntity classEntity = mockClass();
        TimeSlotTemplate timeSlot = mockTimeSlot(1L);

        Session session1 = mockSession(100L, classEntity, timeSlot, tomorrow);
        Session session2 = mockSession(101L, classEntity, timeSlot, day3);

        TeachingSlot teachingSlot1 = new TeachingSlot();
        teachingSlot1.setId(new TeachingSlot.TeachingSlotId(100L, teacherId));
        teachingSlot1.setStatus(TeachingSlotStatus.SCHEDULED);
        teachingSlot1.setSession(session1);

        TeachingSlot teachingSlot2 = new TeachingSlot();
        teachingSlot2.setId(new TeachingSlot.TeachingSlotId(101L, teacherId));
        teachingSlot2.setStatus(TeachingSlotStatus.SCHEDULED);
        teachingSlot2.setSession(session2);

        when(teacherRepository.findByUserAccountId(userId)).thenReturn(Optional.of(teacher));
        when(teachingSlotRepository.findByTeacherIdAndDateRange(
                eq(teacherId),
                anyList(),
                eq(SessionStatus.PLANNED),
                any(LocalDate.class),
                any(LocalDate.class)))
                .thenReturn(Arrays.asList(teachingSlot1, teachingSlot2));
        when(teacherRequestRepository.findBySessionIdInAndStatusIn(anyList(), anyList()))
                .thenReturn(List.of());

        List<TeacherSessionDTO> result = service.getMyFutureSessions(userId, null);

        assertThat(result).hasSize(2);
        assertThat(result).extracting("sessionId").containsExactlyInAnyOrder(100L, 101L);
    }

    @Test
    @org.junit.jupiter.api.DisplayName("getMyFutureSessions - filter by specific date")
    void getMyFutureSessions_filterBySpecificDate() {
        Long teacherId = 10L;
        Long userId = 20L;
        Teacher teacher = mockTeacher(teacherId, userId);

        LocalDate targetDate = LocalDate.now().plusDays(2);
        ClassEntity classEntity = mockClass();
        TimeSlotTemplate timeSlot = mockTimeSlot(1L);

        Session session = mockSession(100L, classEntity, timeSlot, targetDate);
        TeachingSlot teachingSlot = new TeachingSlot();
        teachingSlot.setId(new TeachingSlot.TeachingSlotId(100L, teacherId));
        teachingSlot.setStatus(TeachingSlotStatus.SCHEDULED);
        teachingSlot.setSession(session);

        when(teacherRepository.findByUserAccountId(userId)).thenReturn(Optional.of(teacher));
        when(teachingSlotRepository.findByTeacherIdAndDateRange(
                eq(teacherId),
                anyList(),
                eq(SessionStatus.PLANNED),
                eq(targetDate),
                eq(targetDate)))
                .thenReturn(List.of(teachingSlot));
        when(teacherRequestRepository.findBySessionIdInAndStatusIn(anyList(), anyList()))
                .thenReturn(List.of());

        List<TeacherSessionDTO> result = service.getMyFutureSessions(userId, targetDate);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSessionId()).isEqualTo(100L);
        assertThat(result.get(0).getDate()).isEqualTo(targetDate);
    }

    @Test
    @org.junit.jupiter.api.DisplayName("getMyFutureSessions - shows hasPendingRequest flag")
    void getMyFutureSessions_showsHasPendingRequestFlag() {
        Long teacherId = 10L;
        Long userId = 20L;
        Teacher teacher = mockTeacher(teacherId, userId);

        LocalDate tomorrow = LocalDate.now().plusDays(1);
        ClassEntity classEntity = mockClass();
        TimeSlotTemplate timeSlot = mockTimeSlot(1L);

        Session session = mockSession(100L, classEntity, timeSlot, tomorrow);
        TeachingSlot teachingSlot = new TeachingSlot();
        teachingSlot.setId(new TeachingSlot.TeachingSlotId(100L, teacherId));
        teachingSlot.setStatus(TeachingSlotStatus.SCHEDULED);
        teachingSlot.setSession(session);

        TeacherRequest pendingRequest = TeacherRequest.builder()
                .id(1L)
                .teacher(teacher)
                .session(session)
                .requestType(TeacherRequestType.MODALITY_CHANGE)
                .status(RequestStatus.PENDING)
                .build();

        when(teacherRepository.findByUserAccountId(userId)).thenReturn(Optional.of(teacher));
        when(teachingSlotRepository.findByTeacherIdAndDateRange(
                eq(teacherId),
                anyList(),
                eq(SessionStatus.PLANNED),
                any(LocalDate.class),
                any(LocalDate.class)))
                .thenReturn(List.of(teachingSlot));
        when(teacherRequestRepository.findBySessionIdInAndStatusIn(
                eq(List.of(100L)),
                eq(Arrays.asList(RequestStatus.PENDING, RequestStatus.WAITING_CONFIRM, RequestStatus.APPROVED))))
                .thenReturn(List.of(pendingRequest));

        List<TeacherSessionDTO> result = service.getMyFutureSessions(userId, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isHasPendingRequest()).isTrue();
        assertThat(result.get(0).getRequestStatus()).contains("Đang chờ xử lý");
    }

    @Test
    @org.junit.jupiter.api.DisplayName("getMyFutureSessions - shows no pending request when none exists")
    void getMyFutureSessions_showsNoPendingRequest() {
        Long teacherId = 10L;
        Long userId = 20L;
        Teacher teacher = mockTeacher(teacherId, userId);

        LocalDate tomorrow = LocalDate.now().plusDays(1);
        ClassEntity classEntity = mockClass();
        TimeSlotTemplate timeSlot = mockTimeSlot(1L);

        Session session = mockSession(100L, classEntity, timeSlot, tomorrow);
        TeachingSlot teachingSlot = new TeachingSlot();
        teachingSlot.setId(new TeachingSlot.TeachingSlotId(100L, teacherId));
        teachingSlot.setStatus(TeachingSlotStatus.SCHEDULED);
        teachingSlot.setSession(session);

        when(teacherRepository.findByUserAccountId(userId)).thenReturn(Optional.of(teacher));
        when(teachingSlotRepository.findByTeacherIdAndDateRange(
                eq(teacherId),
                anyList(),
                eq(SessionStatus.PLANNED),
                any(LocalDate.class),
                any(LocalDate.class)))
                .thenReturn(List.of(teachingSlot));
        when(teacherRequestRepository.findBySessionIdInAndStatusIn(anyList(), anyList()))
                .thenReturn(List.of());

        List<TeacherSessionDTO> result = service.getMyFutureSessions(userId, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isHasPendingRequest()).isFalse();
        assertThat(result.get(0).getRequestStatus()).contains("Có thể tạo request");
    }
}

