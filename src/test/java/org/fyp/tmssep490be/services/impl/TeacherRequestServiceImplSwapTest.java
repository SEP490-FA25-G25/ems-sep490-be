package org.fyp.tmssep490be.services.impl;

import org.fyp.tmssep490be.dtos.teacherrequest.SwapCandidateDTO;
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
@org.junit.jupiter.api.DisplayName("TeacherRequestService Swap Unit Tests")
class TeacherRequestServiceImplSwapTest {

    @Autowired
    private TeacherRequestService service;

    @MockitoBean private TeacherRequestRepository teacherRequestRepository;
    @MockitoBean private TeacherRepository teacherRepository;
    @MockitoBean private SessionRepository sessionRepository;
    @MockitoBean private TeachingSlotRepository teachingSlotRepository;
    @MockitoBean private UserAccountRepository userAccountRepository;
    @MockitoBean private TeacherSkillRepository teacherSkillRepository;
    @MockitoBean private TeacherAvailabilityRepository teacherAvailabilityRepository;
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
        cs.setSkillSet(new Skill[]{Skill.SPEAKING, Skill.LISTENING});
        s.setCourseSession(cs);
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

    @Test
    @org.junit.jupiter.api.DisplayName("createRequest - swap - success with replacement teacher")
    void createRequest_swap_successWithReplacement() {
        Long userId = 10L;
        Long teacherId = 20L;
        Long sessionId = 30L;
        Long replacementTeacherId = 21L;

        Teacher teacher = mockTeacher(teacherId, userId);
        Teacher replacementTeacher = mockTeacher(replacementTeacherId, 11L);
        ClassEntity classEntity = mockClass(Modality.OFFLINE);
        TimeSlotTemplate timeSlot = mockTimeSlot(5L);
        Session session = mockSession(sessionId, classEntity, timeSlot, LocalDate.now().plusDays(2));

        when(teacherRepository.findByUserAccountId(userId)).thenReturn(Optional.of(teacher));
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(teachingSlotRepository.existsByIdSessionIdAndIdTeacherIdAndStatusIn(eq(sessionId), eq(teacherId), anyList()))
                .thenReturn(true);
        when(teacherRequestRepository.existsBySessionIdAndRequestTypeAndStatus(eq(sessionId), eq(TeacherRequestType.SWAP), eq(RequestStatus.PENDING)))
                .thenReturn(false);
        when(teacherRepository.findById(replacementTeacherId)).thenReturn(Optional.of(replacementTeacher));
        when(teachingSlotRepository.findAll()).thenReturn(Arrays.asList()); // No conflicts
        UserAccount ua = new UserAccount();
        ua.setId(userId);
        when(userAccountRepository.findById(userId)).thenReturn(Optional.of(ua));
        when(teacherRequestRepository.save(any(TeacherRequest.class))).thenAnswer(invocation -> {
            TeacherRequest tr = invocation.getArgument(0);
            tr.setId(999L);
            return tr;
        });

        TeacherRequestCreateDTO dto = TeacherRequestCreateDTO.builder()
                .sessionId(sessionId)
                .requestType(TeacherRequestType.SWAP)
                .replacementTeacherId(replacementTeacherId)
                .reason("Need someone to cover")
                .build();

        TeacherRequestResponseDTO resp = service.createRequest(dto, userId);

        assertThat(resp.getId()).isEqualTo(999L);
        assertThat(resp.getRequestType()).isEqualTo(TeacherRequestType.SWAP);
        assertThat(resp.getStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(resp.getReplacementTeacherId()).isEqualTo(replacementTeacherId);
        verify(teacherRequestRepository).save(any(TeacherRequest.class));
    }

    @Test
    @org.junit.jupiter.api.DisplayName("createRequest - swap - success without replacement teacher")
    void createRequest_swap_successWithoutReplacement() {
        Long userId = 10L;
        Long teacherId = 20L;
        Long sessionId = 30L;

        Teacher teacher = mockTeacher(teacherId, userId);
        ClassEntity classEntity = mockClass(Modality.OFFLINE);
        TimeSlotTemplate timeSlot = mockTimeSlot(5L);
        Session session = mockSession(sessionId, classEntity, timeSlot, LocalDate.now().plusDays(2));

        when(teacherRepository.findByUserAccountId(userId)).thenReturn(Optional.of(teacher));
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(teachingSlotRepository.existsByIdSessionIdAndIdTeacherIdAndStatusIn(eq(sessionId), eq(teacherId), anyList()))
                .thenReturn(true);
        when(teacherRequestRepository.existsBySessionIdAndRequestTypeAndStatus(eq(sessionId), eq(TeacherRequestType.SWAP), eq(RequestStatus.PENDING)))
                .thenReturn(false);
        UserAccount ua = new UserAccount();
        ua.setId(userId);
        when(userAccountRepository.findById(userId)).thenReturn(Optional.of(ua));
        when(teacherRequestRepository.save(any(TeacherRequest.class))).thenAnswer(invocation -> {
            TeacherRequest tr = invocation.getArgument(0);
            tr.setId(999L);
            return tr;
        });

        TeacherRequestCreateDTO dto = TeacherRequestCreateDTO.builder()
                .sessionId(sessionId)
                .requestType(TeacherRequestType.SWAP)
                .replacementTeacherId(null) // Teacher không chọn, để staff chọn
                .reason("Need someone to cover")
                .build();

        TeacherRequestResponseDTO resp = service.createRequest(dto, userId);

        assertThat(resp.getId()).isEqualTo(999L);
        assertThat(resp.getRequestType()).isEqualTo(TeacherRequestType.SWAP);
        assertThat(resp.getStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(resp.getReplacementTeacherId()).isNull();
        verify(teacherRequestRepository).save(any(TeacherRequest.class));
    }

    @Test
    @org.junit.jupiter.api.DisplayName("createRequest - swap - replacement teacher same as original - throws")
    void createRequest_swap_sameTeacher_throws() {
        Long userId = 10L;
        Long teacherId = 20L;
        Long sessionId = 30L;

        Teacher teacher = mockTeacher(teacherId, userId);
        ClassEntity classEntity = mockClass(Modality.OFFLINE);
        TimeSlotTemplate timeSlot = mockTimeSlot(5L);
        Session session = mockSession(sessionId, classEntity, timeSlot, LocalDate.now().plusDays(2));

        when(teacherRepository.findByUserAccountId(userId)).thenReturn(Optional.of(teacher));
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(teachingSlotRepository.existsByIdSessionIdAndIdTeacherIdAndStatusIn(eq(sessionId), eq(teacherId), anyList()))
                .thenReturn(true);
        when(teacherRepository.findById(teacherId)).thenReturn(Optional.of(teacher)); // Same teacher

        TeacherRequestCreateDTO dto = TeacherRequestCreateDTO.builder()
                .sessionId(sessionId)
                .requestType(TeacherRequestType.SWAP)
                .replacementTeacherId(teacherId) // Same as original teacher
                .reason("Test")
                .build();

        assertThatThrownBy(() -> service.createRequest(dto, userId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @org.junit.jupiter.api.DisplayName("createRequest - swap - replacement teacher has conflict - throws")
    void createRequest_swap_replacementHasConflict_throws() {
        Long userId = 10L;
        Long teacherId = 20L;
        Long sessionId = 30L;
        Long replacementTeacherId = 21L;

        Teacher teacher = mockTeacher(teacherId, userId);
        Teacher replacementTeacher = mockTeacher(replacementTeacherId, 11L);
        ClassEntity classEntity = mockClass(Modality.OFFLINE);
        TimeSlotTemplate timeSlot = mockTimeSlot(5L);
        Session session = mockSession(sessionId, classEntity, timeSlot, LocalDate.now().plusDays(2));

        when(teacherRepository.findByUserAccountId(userId)).thenReturn(Optional.of(teacher));
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(teachingSlotRepository.existsByIdSessionIdAndIdTeacherIdAndStatusIn(eq(sessionId), eq(teacherId), anyList()))
                .thenReturn(true);
        when(teacherRepository.findById(replacementTeacherId)).thenReturn(Optional.of(replacementTeacher));
        
        // Mock conflict: replacement teacher has another session at same time
        TeachingSlot conflictingSlot = new TeachingSlot();
        conflictingSlot.setId(new TeachingSlot.TeachingSlotId(999L, replacementTeacherId));
        Session conflictingSession = new Session();
        conflictingSession.setId(999L);
        conflictingSession.setDate(session.getDate());
        conflictingSession.setTimeSlotTemplate(timeSlot);
        conflictingSession.setStatus(SessionStatus.PLANNED);
        conflictingSlot.setSession(conflictingSession);
        when(teachingSlotRepository.findAll()).thenReturn(Arrays.asList(conflictingSlot));

        TeacherRequestCreateDTO dto = TeacherRequestCreateDTO.builder()
                .sessionId(sessionId)
                .requestType(TeacherRequestType.SWAP)
                .replacementTeacherId(replacementTeacherId)
                .reason("Test")
                .build();

        assertThatThrownBy(() -> service.createRequest(dto, userId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TEACHER_SCHEDULE_CONFLICT);
    }

    @Test
    @org.junit.jupiter.api.DisplayName("suggestSwapCandidates - returns valid candidates")
    void suggestSwapCandidates_returnsValidCandidates() {
        Long userId = 10L;
        Long teacherId = 20L;
        Long sessionId = 30L;

        Teacher teacher = mockTeacher(teacherId, userId);
        ClassEntity classEntity = mockClass(Modality.OFFLINE);
        TimeSlotTemplate timeSlot = mockTimeSlot(5L);
        Session session = mockSession(sessionId, classEntity, timeSlot, LocalDate.now().plusDays(2));

        Teacher candidate1 = mockTeacher(21L, 11L);
        Teacher candidate2 = mockTeacher(22L, 12L);

        when(teacherRepository.findByUserAccountId(userId)).thenReturn(Optional.of(teacher));
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(teachingSlotRepository.existsByIdSessionIdAndIdTeacherIdAndStatusIn(eq(sessionId), eq(teacherId), anyList()))
                .thenReturn(true);
        when(teacherRepository.findAll()).thenReturn(Arrays.asList(teacher, candidate1, candidate2));
        
        // Mock teacher skills
        TeacherSkill skill1 = new TeacherSkill();
        TeacherSkill.TeacherSkillId skillId1 = new TeacherSkill.TeacherSkillId();
        skillId1.setTeacherId(21L);
        skillId1.setSkill(Skill.SPEAKING);
        skill1.setId(skillId1);
        skill1.setTeacher(candidate1);

        TeacherSkill skill2 = new TeacherSkill();
        TeacherSkill.TeacherSkillId skillId2 = new TeacherSkill.TeacherSkillId();
        skillId2.setTeacherId(22L);
        skillId2.setSkill(Skill.GENERAL);
        skill2.setId(skillId2);
        skill2.setTeacher(candidate2);

        when(teacherSkillRepository.findAll()).thenReturn(Arrays.asList(skill1, skill2));
        when(teacherAvailabilityRepository.findAll()).thenReturn(Arrays.asList());
        when(teachingSlotRepository.findAll()).thenReturn(Arrays.asList()); // No conflicts

        List<SwapCandidateDTO> candidates = service.suggestSwapCandidates(sessionId, userId);

        assertThat(candidates).isNotEmpty();
        assertThat(candidates).extracting("teacherId").contains(21L, 22L);
        assertThat(candidates).extracting("hasConflict").containsOnly(false);
    }

    @Test
    @org.junit.jupiter.api.DisplayName("approveRequest - swap - sets WAITING_CONFIRM")
    void approveRequest_swap_setsWaitingConfirm() {
        Long staffId = 99L;
        Long requestId = 777L;
        Long sessionId = 30L;
        Long teacherId = 20L;
        Long replacementTeacherId = 21L;

        Teacher teacher = mockTeacher(teacherId, 10L);
        Teacher replacementTeacher = mockTeacher(replacementTeacherId, 11L);
        ClassEntity classEntity = mockClass(Modality.OFFLINE);
        TimeSlotTemplate timeSlot = mockTimeSlot(5L);
        Session session = mockSession(sessionId, classEntity, timeSlot, LocalDate.now().plusDays(2));

        TeacherRequest tr = TeacherRequest.builder()
                .id(requestId)
                .teacher(teacher)
                .session(session)
                .requestType(TeacherRequestType.SWAP)
                .status(RequestStatus.PENDING)
                .replacementTeacher(replacementTeacher) // Teacher đã chọn
                .build();

        when(teacherRequestRepository.findByIdWithTeacherAndSession(requestId)).thenReturn(Optional.of(tr));
        UserAccount staff = new UserAccount();
        staff.setId(staffId);
        when(userAccountRepository.findById(staffId)).thenReturn(Optional.of(staff));
        when(teacherRepository.findById(replacementTeacherId)).thenReturn(Optional.of(replacementTeacher));
        when(teachingSlotRepository.findAll()).thenReturn(Arrays.asList()); // No conflicts
        when(teacherRequestRepository.save(any(TeacherRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TeacherRequestApproveDTO approve = TeacherRequestApproveDTO.builder()
                .note("Approved")
                .build();

        TeacherRequestResponseDTO resp = service.approveRequest(requestId, approve, staffId);

        assertThat(resp.getStatus()).isEqualTo(RequestStatus.WAITING_CONFIRM);
        assertThat(resp.getReplacementTeacherId()).isEqualTo(replacementTeacherId);
        verify(teacherRequestRepository).save(any(TeacherRequest.class));
    }

    @Test
    @org.junit.jupiter.api.DisplayName("approveRequest - swap - staff override replacement teacher")
    void approveRequest_swap_staffOverrideReplacement() {
        Long staffId = 99L;
        Long requestId = 777L;
        Long sessionId = 30L;
        Long teacherId = 20L;
        Long teacherReplacementId = 21L;
        Long staffReplacementId = 22L;

        Teacher teacher = mockTeacher(teacherId, 10L);
        Teacher staffReplacement = mockTeacher(staffReplacementId, 12L);
        ClassEntity classEntity = mockClass(Modality.OFFLINE);
        TimeSlotTemplate timeSlot = mockTimeSlot(5L);
        Session session = mockSession(sessionId, classEntity, timeSlot, LocalDate.now().plusDays(2));

        TeacherRequest tr = TeacherRequest.builder()
                .id(requestId)
                .teacher(teacher)
                .session(session)
                .requestType(TeacherRequestType.SWAP)
                .status(RequestStatus.PENDING)
                .replacementTeacher(mockTeacher(teacherReplacementId, 11L)) // Teacher đã chọn
                .build();

        when(teacherRequestRepository.findByIdWithTeacherAndSession(requestId)).thenReturn(Optional.of(tr));
        UserAccount staff = new UserAccount();
        staff.setId(staffId);
        when(userAccountRepository.findById(staffId)).thenReturn(Optional.of(staff));
        when(teacherRepository.findById(staffReplacementId)).thenReturn(Optional.of(staffReplacement));
        when(teachingSlotRepository.findAll()).thenReturn(Arrays.asList()); // No conflicts
        when(teacherRequestRepository.save(any(TeacherRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TeacherRequestApproveDTO approve = TeacherRequestApproveDTO.builder()
                .replacementTeacherId(staffReplacementId) // Staff override
                .note("Changed to different teacher")
                .build();

        TeacherRequestResponseDTO resp = service.approveRequest(requestId, approve, staffId);

        assertThat(resp.getStatus()).isEqualTo(RequestStatus.WAITING_CONFIRM);
        assertThat(resp.getReplacementTeacherId()).isEqualTo(staffReplacementId);
        verify(teacherRepository).findById(staffReplacementId);
    }

    @Test
    @org.junit.jupiter.api.DisplayName("confirmSwap - success - updates teaching slots")
    void confirmSwap_success_updatesTeachingSlots() {
        Long replacementUserId = 11L;
        Long replacementTeacherId = 21L;
        Long requestId = 777L;
        Long sessionId = 30L;
        Long originalTeacherId = 20L;

        Teacher replacementTeacher = mockTeacher(replacementTeacherId, replacementUserId);
        Teacher originalTeacher = mockTeacher(originalTeacherId, 10L);
        ClassEntity classEntity = mockClass(Modality.OFFLINE);
        TimeSlotTemplate timeSlot = mockTimeSlot(5L);
        Session session = mockSession(sessionId, classEntity, timeSlot, LocalDate.now().plusDays(2));

        TeacherRequest tr = TeacherRequest.builder()
                .id(requestId)
                .teacher(originalTeacher)
                .session(session)
                .requestType(TeacherRequestType.SWAP)
                .status(RequestStatus.WAITING_CONFIRM)
                .replacementTeacher(replacementTeacher)
                .build();

        when(teacherRequestRepository.findByIdWithTeacherAndSession(requestId)).thenReturn(Optional.of(tr));
        when(teacherRepository.findByUserAccountId(replacementUserId)).thenReturn(Optional.of(replacementTeacher));
        when(teachingSlotRepository.findAll()).thenReturn(Arrays.asList()); // No conflicts

        TeachingSlot.TeachingSlotId originalSlotId = new TeachingSlot.TeachingSlotId(sessionId, originalTeacherId);
        TeachingSlot originalSlot = new TeachingSlot();
        originalSlot.setId(originalSlotId);
        originalSlot.setStatus(TeachingSlotStatus.SCHEDULED);
        when(teachingSlotRepository.findById(originalSlotId)).thenReturn(Optional.of(originalSlot));

        TeachingSlot.TeachingSlotId replacementSlotId = new TeachingSlot.TeachingSlotId(sessionId, replacementTeacherId);
        when(teachingSlotRepository.findById(replacementSlotId)).thenReturn(Optional.empty()); // New slot

        when(teachingSlotRepository.save(any(TeachingSlot.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(teacherRequestRepository.save(any(TeacherRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TeacherRequestResponseDTO resp = service.confirmSwap(requestId, replacementUserId);

        assertThat(resp.getStatus()).isEqualTo(RequestStatus.APPROVED);
        verify(teachingSlotRepository).save(argThat(ts -> ts.getId().getTeacherId().equals(originalTeacherId) 
                && ts.getStatus() == TeachingSlotStatus.ON_LEAVE));
        verify(teachingSlotRepository).save(argThat(ts -> ts.getId().getTeacherId().equals(replacementTeacherId) 
                && ts.getStatus() == TeachingSlotStatus.SUBSTITUTED));
    }

    @Test
    @org.junit.jupiter.api.DisplayName("confirmSwap - not waiting_confirm - throws")
    void confirmSwap_notWaitingConfirm_throws() {
        Long replacementUserId = 11L;
        Long replacementTeacherId = 21L;
        Long requestId = 777L;

        Teacher replacementTeacher = mockTeacher(replacementTeacherId, replacementUserId);
        TeacherRequest tr = TeacherRequest.builder()
                .id(requestId)
                .teacher(mockTeacher(20L, 10L))
                .requestType(TeacherRequestType.SWAP)
                .status(RequestStatus.PENDING) // Not WAITING_CONFIRM
                .replacementTeacher(replacementTeacher)
                .build();

        when(teacherRequestRepository.findByIdWithTeacherAndSession(requestId)).thenReturn(Optional.of(tr));
        when(teacherRepository.findByUserAccountId(replacementUserId)).thenReturn(Optional.of(replacementTeacher));

        assertThatThrownBy(() -> service.confirmSwap(requestId, replacementUserId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    @org.junit.jupiter.api.DisplayName("declineSwap - success - resets to PENDING")
    void declineSwap_success_resetsToPending() {
        Long replacementUserId = 11L;
        Long replacementTeacherId = 21L;
        Long requestId = 777L;

        Teacher replacementTeacher = mockTeacher(replacementTeacherId, replacementUserId);
        TeacherRequest tr = TeacherRequest.builder()
                .id(requestId)
                .teacher(mockTeacher(20L, 10L))
                .requestType(TeacherRequestType.SWAP)
                .status(RequestStatus.WAITING_CONFIRM)
                .replacementTeacher(replacementTeacher)
                .build();

        when(teacherRequestRepository.findByIdWithTeacherAndSession(requestId)).thenReturn(Optional.of(tr));
        when(teacherRepository.findByUserAccountId(replacementUserId)).thenReturn(Optional.of(replacementTeacher));
        when(teacherRequestRepository.save(any(TeacherRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TeacherRequestResponseDTO resp = service.declineSwap(requestId, "Too busy", replacementUserId);

        assertThat(resp.getStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(resp.getReplacementTeacherId()).isNull();
        verify(teacherRequestRepository).save(argThat(r -> r.getStatus() == RequestStatus.PENDING 
                && r.getReplacementTeacher() == null));
    }
}


