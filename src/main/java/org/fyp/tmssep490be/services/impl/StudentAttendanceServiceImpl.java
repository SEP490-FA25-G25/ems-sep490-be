package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import org.fyp.tmssep490be.dtos.studentattendance.StudentAttendanceOverviewItemDTO;
import org.fyp.tmssep490be.dtos.studentattendance.StudentAttendanceOverviewResponseDTO;
import org.fyp.tmssep490be.dtos.studentattendance.StudentAttendanceReportResponseDTO;
import org.fyp.tmssep490be.dtos.studentattendance.StudentAttendanceReportSessionDTO;
import org.fyp.tmssep490be.entities.Session;
import org.fyp.tmssep490be.entities.StudentSession;
import org.fyp.tmssep490be.entities.enums.AttendanceStatus;
import org.fyp.tmssep490be.repositories.StudentSessionRepository;
import org.fyp.tmssep490be.services.StudentAttendanceService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentAttendanceServiceImpl implements StudentAttendanceService {

    private final StudentSessionRepository studentSessionRepository;

    @Override
    public StudentAttendanceOverviewResponseDTO getOverview(Long studentId) {
        List<StudentSession> all = studentSessionRepository.findAllByStudentId(studentId);
        Map<Long, List<StudentSession>> byClass = all.stream()
                .collect(Collectors.groupingBy(ss -> ss.getSession().getClassEntity().getId(), LinkedHashMap::new, Collectors.toList()));

        List<StudentAttendanceOverviewItemDTO> items = byClass.entrySet().stream()
                .map(entry -> {
                    Long clsId = entry.getKey();
                    List<StudentSession> list = entry.getValue();
                    int total = list.size();
                    int attended = 0;
                    int absent = 0;
                    int upcoming = 0;
                    for (StudentSession ss : list) {
                        AttendanceStatus st = ss.getAttendanceStatus();
                        if (st == null) continue;
                        switch (st) {
                            case PRESENT -> attended++;
                            case ABSENT -> absent++;
                            case PLANNED -> upcoming++;
                        }
                    }
                    double rate = total == 0 ? 0d : (double) attended / (double) total;
                    Session any = list.get(0).getSession();
                    return StudentAttendanceOverviewItemDTO.builder()
                            .classId(clsId)
                            .classCode(any.getClassEntity().getCode())
                            .courseId(any.getClassEntity().getCourse().getId())
                            .courseCode(any.getClassEntity().getCourse().getCode())
                            .courseName(any.getClassEntity().getCourse().getName())
                            .totalSessions(total)
                            .attended(attended)
                            .absent(absent)
                            .upcoming(upcoming)
                            .attendanceRate(rate)
                            .status(any.getClassEntity().getStatus().name())
                            .build();
                })
                .collect(Collectors.toList());

        return StudentAttendanceOverviewResponseDTO.builder()
                .classes(items)
                .build();
    }

    @Override
    public StudentAttendanceReportResponseDTO getReport(Long studentId, Long classId) {
        List<StudentSession> studentSessions = studentSessionRepository.findByStudentIdAndClassEntityId(studentId, classId);

        // Tính summary
        int total = studentSessions.size();
        int attended = 0;
        int absent = 0;
        int upcoming = 0;

        for (StudentSession ss : studentSessions) {
            AttendanceStatus st = ss.getAttendanceStatus();
            if (st == null) continue;
            switch (st) {
                case PRESENT -> attended++;
                case ABSENT -> absent++;
                case PLANNED -> upcoming++;
            }
        }
        double rate = total == 0 ? 0d : (double) attended / (double) total;

        // Map sessions DTO
        List<StudentAttendanceReportSessionDTO> sessionItems = studentSessions.stream()
                .sorted(Comparator.comparing(ss -> ss.getSession().getDate()))
                .map(ss -> {
                    Session s = ss.getSession();
                    StudentAttendanceReportSessionDTO.MakeupInfo makeupInfo = null;
                    if (Boolean.TRUE.equals(ss.getIsMakeup()) && ss.getMakeupSession() != null) {
                        Session ms = ss.getMakeupSession();
                        makeupInfo = StudentAttendanceReportSessionDTO.MakeupInfo.builder()
                                .sessionId(ms.getId())
                                .classId(ms.getClassEntity().getId())
                                .classCode(ms.getClassEntity().getCode())
                                .date(ms.getDate())
                                .attended(ss.getAttendanceStatus() == AttendanceStatus.PRESENT)
                                .build();
                    }

                    return StudentAttendanceReportSessionDTO.builder()
                            .sessionId(s.getId())
                            .date(s.getDate())
                            .index(null)
                            .status(s.getStatus() != null ? s.getStatus().name() : null)
                            .attendanceStatus(ss.getAttendanceStatus())
                            .homeworkStatus(ss.getHomeworkStatus())
                            .isMakeup(Boolean.TRUE.equals(ss.getIsMakeup()))
                            .note(ss.getNote())
                            .makeupSessionInfo(makeupInfo)
                            .build();
                })
                .collect(Collectors.toList());

        // Lấy thông tin lớp/khoá học từ một session bất kỳ (nếu có)
        Long courseId = null;
        String courseCode = null;
        String courseName = null;
        String classCode = null;
        if (!studentSessions.isEmpty()) {
            Session any = studentSessions.get(0).getSession();
            courseId = any.getClassEntity().getCourse().getId();
            courseCode = any.getClassEntity().getCourse().getCode();
            courseName = any.getClassEntity().getCourse().getName();
            classCode = any.getClassEntity().getCode();
        }

        StudentAttendanceReportResponseDTO.Summary summary = StudentAttendanceReportResponseDTO.Summary.builder()
                .totalSessions(total)
                .attended(attended)
                .absent(absent)
                .upcoming(upcoming)
                .attendanceRate(rate)
                .build();

        return StudentAttendanceReportResponseDTO.builder()
                .classId(classId)
                .classCode(classCode)
                .courseId(courseId)
                .courseCode(courseCode)
                .courseName(courseName)
                .summary(summary)
                .sessions(sessionItems)
                .build();
    }

}


