package org.fyp.tmssep490be.controllers;

import org.fyp.tmssep490be.dtos.teacherrequest.RescheduleResourceSuggestionDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.RescheduleSlotSuggestionDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestApproveDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestCreateDTO;
import org.fyp.tmssep490be.dtos.teacherrequest.TeacherRequestResponseDTO;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.*;
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

@SpringBootTest
@ActiveProfiles("test")
@org.junit.jupiter.api.DisplayName("Teacher Request Reschedule Integration Tests")
class TeacherRequestRescheduleIT {

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
    @Autowired private ResourceRepository resourceRepository;
    @Autowired private SessionResourceRepository sessionResourceRepository;

    private UserAccount staff;
    private Teacher teacher;
    private Session session;
    private TimeSlotTemplate oldTimeSlot;
    private TimeSlotTemplate newTimeSlot;
    private ClassEntity classEntity;
    private Resource oldResource;
    private Resource newResource;
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

        // Teacher
        UserAccount teacherAccount = new UserAccount();
        teacherAccount.setEmail("teacher+" + uniqueSuffix + "@test.com");
        teacherAccount.setFullName("Teacher User");
        teacherAccount.setGender(Gender.MALE);
        teacherAccount.setStatus(UserStatus.ACTIVE);
        teacherAccount.setPasswordHash("x");
        teacherAccount = userAccountRepository.save(teacherAccount);

        teacher = new Teacher();
        teacher.setUserAccount(teacherAccount);
        teacher = teacherRepository.save(teacher);

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

        // Old time slot
        oldTimeSlot = new TimeSlotTemplate();
        oldTimeSlot.setBranch(branch);
        oldTimeSlot.setName("Morning");
        oldTimeSlot.setStartTime(java.time.LocalTime.of(8, 0));
        oldTimeSlot.setEndTime(java.time.LocalTime.of(10, 0));
        oldTimeSlot = timeSlotTemplateRepository.save(oldTimeSlot);

        // New time slot
        newTimeSlot = new TimeSlotTemplate();
        newTimeSlot.setBranch(branch);
        newTimeSlot.setName("Afternoon");
        newTimeSlot.setStartTime(java.time.LocalTime.of(14, 0));
        newTimeSlot.setEndTime(java.time.LocalTime.of(16, 0));
        newTimeSlot = timeSlotTemplateRepository.save(newTimeSlot);

        // Old resource
        oldResource = new Resource();
        oldResource.setBranch(branch);
        oldResource.setCode("ROOM-" + uniqueSuffix);
        oldResource.setName("Room-1");
        oldResource.setResourceType(ResourceType.ROOM);
        oldResource.setCapacity(30);
        oldResource = resourceRepository.save(oldResource);

        // New resource
        newResource = new Resource();
        newResource.setBranch(branch);
        newResource.setCode("ROOM2-" + uniqueSuffix);
        newResource.setName("Room-2");
        newResource.setResourceType(ResourceType.ROOM);
        newResource.setCapacity(50);
        newResource = resourceRepository.save(newResource);

        // Session (planned, within 7 days)
        session = new Session();
        session.setClassEntity(classEntity);
        session.setDate(LocalDate.now().plusDays(1));
        session.setStatus(SessionStatus.PLANNED);
        session.setTimeSlotTemplate(oldTimeSlot);
        session = sessionRepository.save(session);

        // Create teaching slot for teacher
        TeachingSlot teachingSlot = new TeachingSlot();
        teachingSlot.setId(new TeachingSlot.TeachingSlotId(session.getId(), teacher.getId()));
        teachingSlot.setSession(session);
        teachingSlot.setTeacher(teacher);
        teachingSlot.setStatus(TeachingSlotStatus.SCHEDULED);
        teachingSlotRepository.save(teachingSlot);

        // Create session resource
        SessionResource sessionResource = SessionResource.builder()
                .id(new SessionResource.SessionResourceId(session.getId(), oldResource.getId()))
                .session(session)
                .resource(oldResource)
                .build();
        sessionResourceRepository.save(sessionResource);
    }

    @Test
    @org.junit.jupiter.api.DisplayName("Create reschedule request - success")
    void createRescheduleRequest_success() {
        LocalDate newDate = LocalDate.now().plusDays(3);

        TeacherRequestCreateDTO dto = TeacherRequestCreateDTO.builder()
                .sessionId(session.getId())
                .requestType(TeacherRequestType.RESCHEDULE)
                .newDate(newDate)
                .newTimeSlotId(newTimeSlot.getId())
                .newResourceId(newResource.getId())
                .reason("Need to reschedule")
                .build();

        TeacherRequestResponseDTO resp = teacherRequestService.createRequest(
                dto, teacher.getUserAccount().getId());

        assertThat(resp.getRequestType()).isEqualTo(TeacherRequestType.RESCHEDULE);
        assertThat(resp.getStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(resp.getNewDate()).isEqualTo(newDate);
        assertThat(resp.getNewTimeSlotId()).isEqualTo(newTimeSlot.getId());
        assertThat(resp.getNewResourceId()).isEqualTo(newResource.getId());
    }

    @Test
    @org.junit.jupiter.api.DisplayName("Suggest slots - returns available time slots")
    void suggestSlots_returnsAvailableSlots() {
        LocalDate date = LocalDate.now().plusDays(3);

        List<RescheduleSlotSuggestionDTO> slots = teacherRequestService.suggestSlots(
                session.getId(), date, teacher.getUserAccount().getId());

        assertThat(slots).isNotEmpty();
        assertThat(slots).extracting("timeSlotId").contains(newTimeSlot.getId());
    }

    @Test
    @org.junit.jupiter.api.DisplayName("Suggest resources - returns available resources")
    void suggestResources_returnsAvailableResources() {
        LocalDate date = LocalDate.now().plusDays(3);

        List<RescheduleResourceSuggestionDTO> resources = teacherRequestService.suggestResources(
                session.getId(), date, newTimeSlot.getId(), teacher.getUserAccount().getId());

        assertThat(resources).isNotEmpty();
        assertThat(resources).extracting("resourceId").contains(newResource.getId());
    }

    @Test
    @org.junit.jupiter.api.DisplayName("Approve reschedule - creates new session and cancels old")
    void approveReschedule_createsNewSession() {
        LocalDate newDate = LocalDate.now().plusDays(3);

        // Create pending reschedule request
        TeacherRequest req = TeacherRequest.builder()
                .teacher(teacher)
                .session(session)
                .requestType(TeacherRequestType.RESCHEDULE)
                .status(RequestStatus.PENDING)
                .newDate(newDate)
                .newTimeSlot(newTimeSlot)
                .newResource(newResource)
                .requestReason("Need to reschedule")
                .submittedBy(teacher.getUserAccount())
                .build();
        req = teacherRequestRepository.save(req);

        TeacherRequestApproveDTO approve = TeacherRequestApproveDTO.builder()
                .note("Approved")
                .build();

        TeacherRequestResponseDTO resp = teacherRequestService.approveRequest(
                req.getId(), approve, staff.getId());

        assertThat(resp.getStatus()).isEqualTo(RequestStatus.APPROVED);

        // Verify old session is cancelled
        Session oldSession = sessionRepository.findById(session.getId()).orElseThrow();
        assertThat(oldSession.getStatus()).isEqualTo(SessionStatus.CANCELLED);

        // Verify new session is created
        assertThat(resp.getNewSessionId()).isNotNull();
        Session newSession = sessionRepository.findById(resp.getNewSessionId()).orElseThrow();
        assertThat(newSession.getStatus()).isEqualTo(SessionStatus.PLANNED);
        assertThat(newSession.getDate()).isEqualTo(newDate);
        assertThat(newSession.getTimeSlotTemplate().getId()).isEqualTo(newTimeSlot.getId());

        // Verify new teaching slot exists
        TeachingSlot newTeachingSlot = teachingSlotRepository.findById(
                new TeachingSlot.TeachingSlotId(newSession.getId(), teacher.getId())).orElseThrow();
        assertThat(newTeachingSlot.getStatus()).isEqualTo(TeachingSlotStatus.SCHEDULED);

        // Verify new session resource exists
        List<SessionResource> newSessionResources = sessionResourceRepository.findBySessionId(newSession.getId());
        assertThat(newSessionResources).isNotEmpty();
        assertThat(newSessionResources).extracting(sr -> sr.getResource().getId()).contains(newResource.getId());
    }
}

