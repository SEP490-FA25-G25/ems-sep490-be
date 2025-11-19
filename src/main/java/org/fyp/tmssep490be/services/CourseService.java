package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.course.*;

import java.util.List;

public interface CourseService {
    List<StudentCourseDTO> getStudentCourses(Long studentId);
    List<StudentCourseDTO> getStudentCoursesByUserId(Long userId);
    CourseDetailDTO getCourseDetail(Long courseId);
    CourseDetailDTO getCourseSyllabus(Long courseId);
    MaterialHierarchyDTO getCourseMaterials(Long courseId, Long studentId);
    List<CoursePLODTO> getCoursePLOs(Long courseId);
    List<CourseCLODTO> getCourseCLOs(Long courseId);
}
