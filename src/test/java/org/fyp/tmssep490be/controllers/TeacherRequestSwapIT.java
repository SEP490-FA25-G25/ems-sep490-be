package org.fyp.tmssep490be.controllers;

import org.fyp.tmssep490be.dtos.teacherrequest.SwapCandidateDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestApproveDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestCreateDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestResponseDTO;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.*;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.repositories.*;
import org.fyp.tmssep490be.services.TeacherRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@org.junit.jupiter.api.DisplayName("Teacher Request Swap Integration Tests")
class TeacherRequestSwapIT {

    @Autowired private TeacherRequestService teacherRequestService;
    @Autowired private TeacherRequestRepository teacherRequestRepository;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private SessionRepository sessionRepository;
    @Autowired private TeachingSlotRepository teachingSlotRepository;
    @Autowired private ClassRepository classRepository;
    @Autowired private TimeSlotTemplateRepository timeSlotTemplateRepository;
    @Autowired private CenterRepository centerRepository;
    @Autowired private BranchRepository branchRepository;
    @Autowired private SubjectRepository subjectRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private TeacherSkillRepository teacherSkillRepository;

    private UserAccount staff;
    private Teacher originalTeacher;
    private Teacher replacementTeacher;
    private Session session;
    private TimeSlotTemplate timeSlot;
    private ClassEntity classEntity;
    private String uniqueSuffix;

    @BeforeEach
    void setup() {
        uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        
        // Staff user
        staff = new UserAccount();
        staff.setEmail("staff+" + uniqueSuffix + "@test.com");
        staff.setFullName("Staff User");
        staff.setGender(Gender.MALE);
        staff.setStatus(UserStatus.ACTIVE);
        staff.setPasswordHash("x");
        staff = userAccountRepository.save(staff);

        // Original teacher
        UserAccount originalTeacherAccount = new UserAccount();
        originalTeacherAccount.setEmail("teacher1+" + uniqueSuffix + "@test.com");
        originalTeacherAccount.setFullName("Original Teacher");
        originalTeacherAccount.setGender(Gender.MALE);
        originalTeacherAccount.setStatus(UserStatus.ACTIVE);
        originalTeacherAccount.setPasswordHash("x");
        originalTeacherAccount = userAccountRepository.save(originalTeacherAccount);

        originalTeacher = new Teacher();
        originalTeacher.setUserAccount(originalTeacherAccount);
        originalTeacher.setContractType("full-time");
        originalTeacher = teacherRepository.save(originalTeacher);

        // Replacement teacher
        UserAccount replacementTeacherAccount = new UserAccount();
        replacementTeacherAccount.setEmail("teacher2+" + uniqueSuffix + "@test.com");
        replacementTeacherAccount.setFullName("Replacement Teacher");
        replacementTeacherAccount.setGender(Gender.FEMALE);
        replacementTeacherAccount.setStatus(UserStatus.ACTIVE);
        replacementTeacherAccount.setPasswordHash("x");
        replacementTeacherAccount = userAccountRepository.save(replacementTeacherAccount);

        replacementTeacher = new Teacher();
        replacementTeacher.setUserAccount(replacementTeacherAccount);
        replacementTeacher.setContractType("full-time");
        replacementTeacher = teacherRepository.save(replacementTeacher);

        // Add skills to replacement teacher
        TeacherSkill skill = new TeacherSkill();
        TeacherSkill.TeacherSkillId skillId = new TeacherSkill.TeacherSkillId();
        skillId.setTeacherId(replacementTeacher.getId());
        skillId.setSkill(Skill.GENERAL);
        skill.setId(skillId);
        skill.setTeacher(replacementTeacher);
        teacherSkillRepository.save(skill);

        // Minimal org structure
        Center center = new Center();
        center.setCode("CEN-" + uniqueSuffix);
        center.setName("Center 1");
        center = centerRepository.save(center);

        Branch branch = new Branch();
        branch.setCenter(center);
        branch.setCode("BR-" + uniqueSuffix);
        branch.setName("Branch 1");
        branch = branchRepository.save(branch);

        Subject subject = new Subject();
        subject.setCode("SUB-" + uniqueSuffix);
        subject.setName("Subject 1");
        subject = subjectRepository.save(subject);

        Course course = new Course();
        course.setSubject(subject);
        course.setCode("COURSE-" + uniqueSuffix);
        course.setName("Course 1");
        course = courseRepository.save(course);

        // Class
        classEntity = new ClassEntity();
        classEntity.setBranch(branch);
        classEntity.setCourse(course);
        classEntity.setCode("C-IT-" + uniqueSuffix);
        classEntity.setName("IT Class");
        classEntity.setModality(Modality.OFFLINE);
        classEntity.setStartDate(LocalDate.now());
        classEntity = classRepository.save(classEntity);

        // Time slot
        timeSlot = new TimeSlotTemplate();
        timeSlot.setBranch(branch);
        timeSlot.setName("Morning");
        timeSlot.setStartTime(java.time.LocalTime.of(8, 0));
        timeSlot.setEndTime(java.time.LocalTime.of(10, 0));
        timeSlot = timeSlotTemplateRepository.save(timeSlot);

        // Session (planned, within 7 days)
        session = new Session();
        session.setClassEntity(classEntity);
        session.setDate(LocalDate.now().plusDays(1));
        session.setStatus(SessionStatus.PLANNED);
        session.setTimeSlotTemplate(timeSlot);
        session = sessionRepository.save(session);

        // Create teaching slot for original teacher
        TeachingSlot teachingSlot = new TeachingSlot();
        teachingSlot.setId(new TeachingSlot.TeachingSlotId(session.getId(), originalTeacher.getId()));
        teachingSlot.setSession(session);
        teachingSlot.setTeacher(originalTeacher);
        teachingSlot.setStatus(TeachingSlotStatus.SCHEDULED);
        teachingSlotRepository.save(teachingSlot);
    }

    @Test
    @org.junit.jupiter.api.DisplayName("Create swap request - success")
    void createSwapRequest_success() {
        TeacherRequestCreateDTO dto = TeacherRequestCreateDTO.builder()
                .sessionId(session.getId())
                .requestType(TeacherRequestType.SWAP)
                .replacementTeacherId(replacementTeacher.getId())
                .reason("Need someone to cover")
                .build();

        TeacherRequestResponseDTO resp = teacherRequestService.createRequest(
                dto, originalTeacher.getUserAccount().getId());

        assertThat(resp.getRequestType()).isEqualTo(TeacherRequestType.SWAP);
        assertThat(resp.getStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(resp.getReplacementTeacherId()).isEqualTo(replacementTeacher.getId());
    }

    @Test
    @org.junit.jupiter.api.DisplayName("Create swap request - without replacement teacher - success")
    void createSwapRequest_withoutReplacement_success() {
        TeacherRequestCreateDTO dto = TeacherRequestCreateDTO.builder()
                .sessionId(session.getId())
                .requestType(TeacherRequestType.SWAP)
                .replacementTeacherId(null) // Teacher không chọn, để staff chọn
                .reason("Need someone to cover")
                .build();

        TeacherRequestResponseDTO resp = teacherRequestService.createRequest(
                dto, originalTeacher.getUserAccount().getId());

        assertThat(resp.getRequestType()).isEqualTo(TeacherRequestType.SWAP);
        assertThat(resp.getStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(resp.getReplacementTeacherId()).isNull();
    }

    @Test
    @org.junit.jupiter.api.DisplayName("Suggest swap candidates - returns valid teachers")
    void suggestSwapCandidates_returnsValidTeachers() {
        List<SwapCandidateDTO> candidates = teacherRequestService.suggestSwapCandidates(
                session.getId(), originalTeacher.getUserAccount().getId());

        assertThat(candidates).isNotEmpty();
        assertThat(candidates).extracting("teacherId").contains(replacementTeacher.getId());
        assertThat(candidates).extracting("hasConflict").containsOnly(false);
    }

    @Test
    @org.junit.jupiter.api.DisplayName("Approve swap - sets WAITING_CONFIRM")
    void approveSwap_setsWaitingConfirm() {
        // Create pending swap request
        TeacherRequest req = TeacherRequest.builder()
                .teacher(originalTeacher)
                .session(session)
                .requestType(TeacherRequestType.SWAP)
                .status(RequestStatus.PENDING)
                .replacementTeacher(replacementTeacher)
                .requestReason("Need cover")
                .submittedBy(originalTeacher.getUserAccount())
                .build();
        req = teacherRequestRepository.save(req);

        TeacherRequestApproveDTO approve = TeacherRequestApproveDTO.builder()
                .note("Approved")
                .build();

        TeacherRequestResponseDTO resp = teacherRequestService.approveRequest(
                req.getId(), approve, staff.getId());

        assertThat(resp.getStatus()).isEqualTo(RequestStatus.WAITING_CONFIRM);
        assertThat(resp.getReplacementTeacherId()).isEqualTo(replacementTeacher.getId());
    }

    @Test
    @org.junit.jupiter.api.DisplayName("Confirm swap - updates teaching slots and sets APPROVED")
    void confirmSwap_updatesTeachingSlots() {
        // Create WAITING_CONFIRM swap request
        TeacherRequest req = TeacherRequest.builder()
                .teacher(originalTeacher)
                .session(session)
                .requestType(TeacherRequestType.SWAP)
                .status(RequestStatus.WAITING_CONFIRM)
                .replacementTeacher(replacementTeacher)
                .requestReason("Need cover")
                .submittedBy(originalTeacher.getUserAccount())
                .decidedBy(staff)
                .build();
        req = teacherRequestRepository.save(req);

        TeacherRequestResponseDTO resp = teacherRequestService.confirmSwap(
                req.getId(), replacementTeacher.getUserAccount().getId());

        assertThat(resp.getStatus()).isEqualTo(RequestStatus.APPROVED);

        // Verify teaching slots updated
        TeachingSlot originalSlot = teachingSlotRepository.findById(
                new TeachingSlot.TeachingSlotId(session.getId(), originalTeacher.getId())).orElseThrow();
        assertThat(originalSlot.getStatus()).isEqualTo(TeachingSlotStatus.ON_LEAVE);

        TeachingSlot replacementSlot = teachingSlotRepository.findById(
                new TeachingSlot.TeachingSlotId(session.getId(), replacementTeacher.getId())).orElseThrow();
        assertThat(replacementSlot.getStatus()).isEqualTo(TeachingSlotStatus.SUBSTITUTED);
    }

    @Test
    @org.junit.jupiter.api.DisplayName("Decline swap - resets to PENDING")
    void declineSwap_resetsToPending() {
        // Create WAITING_CONFIRM swap request
        TeacherRequest req = TeacherRequest.builder()
                .teacher(originalTeacher)
                .session(session)
                .requestType(TeacherRequestType.SWAP)
                .status(RequestStatus.WAITING_CONFIRM)
                .replacementTeacher(replacementTeacher)
                .requestReason("Need cover")
                .submittedBy(originalTeacher.getUserAccount())
                .decidedBy(staff)
                .build();
        req = teacherRequestRepository.save(req);

        TeacherRequestResponseDTO resp = teacherRequestService.declineSwap(
                req.getId(), "Too busy", replacementTeacher.getUserAccount().getId());

        assertThat(resp.getStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(resp.getReplacementTeacherId()).isNull();

        // Verify request updated in DB
        TeacherRequest updated = teacherRequestRepository.findById(req.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(updated.getReplacementTeacher()).isNull();
    }

    @Test
    @org.junit.jupiter.api.DisplayName("Confirm swap - wrong teacher - throws FORBIDDEN")
    void confirmSwap_wrongTeacher_throws() {
        // Create WAITING_CONFIRM swap request
        TeacherRequest req = TeacherRequest.builder()
                .teacher(originalTeacher)
                .session(session)
                .requestType(TeacherRequestType.SWAP)
                .status(RequestStatus.WAITING_CONFIRM)
                .replacementTeacher(replacementTeacher)
                .requestReason("Need cover")
                .submittedBy(originalTeacher.getUserAccount())
                .decidedBy(staff)
                .build();
        TeacherRequest savedReq = teacherRequestRepository.save(req);

        // Try to confirm with wrong teacher (original teacher instead of replacement)
        assertThatThrownBy(() -> teacherRequestService.confirmSwap(
                savedReq.getId(), originalTeacher.getUserAccount().getId()))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("forbidden");
    }
}

