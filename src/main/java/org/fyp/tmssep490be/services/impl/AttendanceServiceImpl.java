package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import org.fyp.tmssep490be.dtos.attendance.*;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.AttendanceStatus;
import org.fyp.tmssep490be.entities.enums.EnrollmentStatus;
import org.fyp.tmssep490be.entities.enums.SessionStatus;
import org.fyp.tmssep490be.entities.enums.TeachingSlotStatus;
import org.fyp.tmssep490be.exceptions.ResourceNotFoundException;
import org.fyp.tmssep490be.repositories.EnrollmentRepository;
import org.fyp.tmssep490be.repositories.SessionRepository;
import org.fyp.tmssep490be.repositories.StudentSessionRepository;
import org.fyp.tmssep490be.repositories.TeachingSlotRepository;
import org.fyp.tmssep490be.services.AttendanceService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceServiceImpl implements AttendanceService {

    private static final List<TeachingSlotStatus> OWNERSHIP_STATUSES = List.of(
            TeachingSlotStatus.SCHEDULED,
            TeachingSlotStatus.SUBSTITUTED
    );

    private final TeachingSlotRepository teachingSlotRepository;
    private final StudentSessionRepository studentSessionRepository;
    private final SessionRepository sessionRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Override
    public List<SessionTodayDTO> getSessionsForDate(Long teacherId, LocalDate date) {
        List<TeachingSlot> slots = teachingSlotRepository.findByTeacherIdAndDate(teacherId, date);
        if (slots.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> sessionIds = slots.stream()
                .map(slot -> slot.getSession().getId())
                .distinct()
                .toList();

        Map<Long, List<StudentSession>> sessionStudentMap = studentSessionRepository.findBySessionIds(sessionIds)
                .stream()
                .collect(Collectors.groupingBy(ss -> ss.getSession().getId()));

        return slots.stream()
                .map(TeachingSlot::getSession)
                .collect(Collectors.toMap(Session::getId, session -> session, (a, b) -> a, LinkedHashMap::new))
                .values()
                .stream()
                .map(session -> {
                    List<StudentSession> studentSessions = sessionStudentMap.getOrDefault(session.getId(), List.of());
                    AttendanceSummaryDTO summary = buildSummary(studentSessions);
                    boolean submitted = studentSessions.stream()
                            .anyMatch(ss -> ss.getAttendanceStatus() != null && ss.getAttendanceStatus() != AttendanceStatus.PLANNED);
                    return SessionTodayDTO.builder()
                            .sessionId(session.getId())
                            .classId(session.getClassEntity().getId())
                            .classCode(session.getClassEntity().getCode())
                            .courseCode(session.getClassEntity().getCourse().getCode())
                            .courseName(session.getClassEntity().getCourse().getName())
                            .date(session.getDate())
                            .startTime(session.getTimeSlotTemplate().getStartTime())
                            .endTime(session.getTimeSlotTemplate().getEndTime())
                            .status(session.getStatus().name())
                            .attendanceSubmitted(submitted)
                            .totalStudents(summary.getTotalStudents())
                            .presentCount(summary.getPresentCount())
                            .absentCount(summary.getAbsentCount())
                            .build();
                })
                .toList();
    }

    @Override
    public StudentsAttendanceResponseDTO getSessionStudents(Long teacherId, Long sessionId) {
        assertOwnership(teacherId, sessionId);
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        List<StudentSession> studentSessions = studentSessionRepository.findBySessionId(sessionId);
        AttendanceSummaryDTO summary = buildSummary(studentSessions);

        List<StudentAttendanceDTO> students = studentSessions.stream()
                .map(this::toStudentAttendanceDTO)
                .toList();

        return StudentsAttendanceResponseDTO.builder()
                .sessionId(session.getId())
                .classId(session.getClassEntity().getId())
                .classCode(session.getClassEntity().getCode())
                .courseCode(session.getClassEntity().getCourse().getCode())
                .courseName(session.getClassEntity().getCourse().getName())
                .date(session.getDate())
                .timeSlotName(session.getTimeSlotTemplate().getName())
                .summary(summary)
                .students(students)
                .build();
    }

    @Override
    @Transactional
    public AttendanceSaveResponseDTO saveAttendance(Long teacherId, Long sessionId, AttendanceSaveRequestDTO request) {
        assertOwnership(teacherId, sessionId);
        if (request.getRecords() == null || request.getRecords().isEmpty()) {
            throw new IllegalArgumentException("Attendance records must not be empty");
        }

        OffsetDateTime now = OffsetDateTime.now();
        for (AttendanceRecordDTO record : request.getRecords()) {
            StudentSession.StudentSessionId id = new StudentSession.StudentSessionId(record.getStudentId(), sessionId);
            StudentSession studentSession = studentSessionRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Student is not part of this session"));
            studentSession.setAttendanceStatus(record.getAttendanceStatus());
            studentSession.setHomeworkStatus(record.getHomeworkStatus());
            studentSession.setNote(record.getNote());
            studentSession.setRecordedAt(now);
        }

        List<StudentSession> updatedSessions = studentSessionRepository.findBySessionId(sessionId);
        AttendanceSummaryDTO summary = buildSummary(updatedSessions);

        return AttendanceSaveResponseDTO.builder()
                .sessionId(sessionId)
                .summary(summary)
                .build();
    }

    @Override
    @Transactional
    public MarkAllResponseDTO markAllPresent(Long teacherId, Long sessionId) {
        assertOwnership(teacherId, sessionId);
        List<StudentSession> studentSessions = studentSessionRepository.findBySessionId(sessionId);
        // Do not persist changes here. Only propose the summary as if all are PRESENT.
        AttendanceSummaryDTO summary = AttendanceSummaryDTO.builder()
                .totalStudents(studentSessions.size())
                .presentCount(studentSessions.size())
                .absentCount(0)
                .build();
        return MarkAllResponseDTO.builder()
                .sessionId(sessionId)
                .summary(summary)
                .build();
    }

    @Override
    @Transactional
    public MarkAllResponseDTO markAllAbsent(Long teacherId, Long sessionId) {
        assertOwnership(teacherId, sessionId);
        List<StudentSession> studentSessions = studentSessionRepository.findBySessionId(sessionId);
        // Do not persist changes here. Only propose the summary as if all are ABSENT.
        AttendanceSummaryDTO summary = AttendanceSummaryDTO.builder()
                .totalStudents(studentSessions.size())
                .presentCount(0)
                .absentCount(studentSessions.size())
                .build();
        return MarkAllResponseDTO.builder()
                .sessionId(sessionId)
                .summary(summary)
                .build();
    }

    @Override
    public SessionReportResponseDTO getSessionReport(Long teacherId, Long sessionId) {
        assertOwnership(teacherId, sessionId);
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        List<StudentSession> studentSessions = studentSessionRepository.findBySessionId(sessionId);
        AttendanceSummaryDTO summary = buildSummary(studentSessions);

        return SessionReportResponseDTO.builder()
                .sessionId(session.getId())
                .classId(session.getClassEntity().getId())
                .classCode(session.getClassEntity().getCode())
                .courseCode(session.getClassEntity().getCourse().getCode())
                .courseName(session.getClassEntity().getCourse().getName())
                .date(session.getDate())
                .timeSlotName(session.getTimeSlotTemplate().getName())
                .teacherNote(session.getTeacherNote())
                .summary(summary)
                .build();
    }

    @Override
    @Transactional
    public SessionReportResponseDTO submitSessionReport(Long teacherId, Long sessionId, SessionReportSubmitDTO request) {
        assertOwnership(teacherId, sessionId);
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        session.setTeacherNote(request.getTeacherNote());
        // Mark session as DONE upon report submission
        session.setStatus(SessionStatus.DONE);

        List<StudentSession> studentSessions = studentSessionRepository.findBySessionId(sessionId);
        AttendanceSummaryDTO summary = buildSummary(studentSessions);

        return SessionReportResponseDTO.builder()
                .sessionId(session.getId())
                .classId(session.getClassEntity().getId())
                .classCode(session.getClassEntity().getCode())
                .courseCode(session.getClassEntity().getCourse().getCode())
                .courseName(session.getClassEntity().getCourse().getName())
                .date(session.getDate())
                .timeSlotName(session.getTimeSlotTemplate().getName())
                .teacherNote(session.getTeacherNote())
                .summary(summary)
                .build();
    }

    @Override
    public AttendanceMatrixDTO getClassAttendanceMatrix(Long teacherId, Long classId) {
        List<Session> sessions = sessionRepository.findAllByClassIdOrderByDateAndTime(classId);
        if (sessions.isEmpty()) {
            throw new ResourceNotFoundException("Class has no sessions");
        }

        boolean ownsAtLeastOne = sessions.stream()
                .anyMatch(session -> teachingSlotRepository.existsByIdSessionIdAndIdTeacherIdAndStatusIn(
                        session.getId(),
                        teacherId,
                        OWNERSHIP_STATUSES
                ));

        if (!ownsAtLeastOne) {
            throw new AccessDeniedException("Teacher does not own this class");
        }

        List<Long> sessionIds = sessions.stream().map(Session::getId).toList();
        Map<Long, List<StudentSession>> sessionStudentMap = studentSessionRepository.findBySessionIds(sessionIds)
                .stream()
                .collect(Collectors.groupingBy(ss -> ss.getSession().getId()));

        Map<Long, StudentAttendanceMatrixDTO.StudentAttendanceMatrixDTOBuilder> rowBuilders = new LinkedHashMap<>();
        Map<Long, Map<Long, StudentAttendanceMatrixDTO.Cell.CellBuilder>> cellBuilders = new HashMap<>();

        for (Session session : sessions) {
            List<StudentSession> studentSessions = sessionStudentMap.getOrDefault(session.getId(), List.of());
            for (StudentSession ss : studentSessions) {
                Long studentId = ss.getStudent().getId();
                rowBuilders.computeIfAbsent(studentId, id -> StudentAttendanceMatrixDTO.builder()
                                .studentId(studentId)
                                .studentCode(ss.getStudent().getStudentCode())
                                .fullName(ss.getStudent().getUserAccount().getFullName())
                                .cells(new ArrayList<>()));

                cellBuilders.computeIfAbsent(studentId, id -> new HashMap<>())
                        .put(session.getId(), StudentAttendanceMatrixDTO.Cell.builder()
                                .sessionId(session.getId())
                                .attendanceStatus(resolveDisplayStatus(ss))
                                .makeup(Boolean.TRUE.equals(ss.getIsMakeup())));
            }
        }

        // Include enrolled students even if they have no student sessions (edge case)
        List<Enrollment> enrollments = enrollmentRepository.findByClassIdAndStatus(classId, EnrollmentStatus.ENROLLED);
        for (Enrollment enrollment : enrollments) {
            Long studentId = enrollment.getStudentId();
            rowBuilders.computeIfAbsent(studentId, id -> StudentAttendanceMatrixDTO.builder()
                            .studentId(studentId)
                            .studentCode(enrollment.getStudent().getStudentCode())
                            .fullName(enrollment.getStudent().getUserAccount().getFullName())
                            .cells(new ArrayList<>()));
            cellBuilders.computeIfAbsent(studentId, id -> new HashMap<>());
        }

        List<SessionMatrixInfoDTO> sessionDtos = sessions.stream()
                .map(session -> SessionMatrixInfoDTO.builder()
                        .sessionId(session.getId())
                        .date(session.getDate())
                        .timeSlotName(session.getTimeSlotTemplate().getName())
                        .status(session.getStatus().name())
                        .build())
                .toList();

        List<StudentAttendanceMatrixDTO> studentDtos = rowBuilders.values().stream()
                .map(builder -> {
                    Long studentId = builder.build().getStudentId();
                    Map<Long, StudentAttendanceMatrixDTO.Cell.CellBuilder> cellsBySession = cellBuilders.getOrDefault(studentId, Map.of());
                    List<StudentAttendanceMatrixDTO.Cell> cells = sessions.stream()
                            .map(session -> cellsBySession.getOrDefault(session.getId(),
                                            StudentAttendanceMatrixDTO.Cell.builder()
                                                    .sessionId(session.getId())
                                                    .attendanceStatus(AttendanceStatus.ABSENT)
                                                    .makeup(false))
                                    .build())
                            .toList();
                    return StudentAttendanceMatrixDTO.builder()
                            .studentId(studentId)
                            .studentCode(builder.build().getStudentCode())
                            .fullName(builder.build().getFullName())
                            .cells(cells)
                            .build();
                })
                .sorted(Comparator.comparing(StudentAttendanceMatrixDTO::getStudentCode, Comparator.nullsLast(String::compareTo)))
                .toList();

        ClassEntity classEntity = sessions.get(0).getClassEntity();

        return AttendanceMatrixDTO.builder()
                .classId(classId)
                .classCode(classEntity.getCode())
                .sessions(sessionDtos)
                .students(studentDtos)
                .build();
    }

    private void assertOwnership(Long teacherId, Long sessionId) {
        boolean owns = teachingSlotRepository.existsByIdSessionIdAndIdTeacherIdAndStatusIn(
                sessionId,
                teacherId,
                OWNERSHIP_STATUSES
        );
        if (!owns) {
            throw new AccessDeniedException("Teacher does not own this session");
        }
    }

    private StudentAttendanceDTO toStudentAttendanceDTO(StudentSession studentSession) {
        AttendanceStatus status = resolveDisplayStatus(studentSession);
        Session makeupSession = studentSession.getMakeupSession();
        return StudentAttendanceDTO.builder()
                .studentId(studentSession.getStudent().getId())
                .studentCode(studentSession.getStudent().getStudentCode())
                .fullName(studentSession.getStudent().getUserAccount().getFullName())
                .attendanceStatus(status)
                .homeworkStatus(studentSession.getHomeworkStatus())
                .note(studentSession.getNote())
                .makeup(Boolean.TRUE.equals(studentSession.getIsMakeup()))
                .makeupSessionId(makeupSession != null ? makeupSession.getId() : null)
                .build();
    }

    private AttendanceStatus resolveDisplayStatus(StudentSession studentSession) {
        AttendanceStatus status = studentSession.getAttendanceStatus();
        if (status == null || status == AttendanceStatus.PLANNED) {
            return AttendanceStatus.ABSENT;
        }
        return status;
    }

    private AttendanceSummaryDTO buildSummary(Collection<StudentSession> studentSessions) {
        int total = studentSessions.size();
        int present = 0;
        int absent = 0;
        for (StudentSession ss : studentSessions) {
            AttendanceStatus status = resolveDisplayStatus(ss);
            if (status == AttendanceStatus.PRESENT) {
                present++;
            } else {
                absent++;
            }
        }
        return AttendanceSummaryDTO.builder()
                .totalStudents(total)
                .presentCount(present)
                .absentCount(absent)
                .build();
    }
}

