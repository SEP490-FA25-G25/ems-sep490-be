package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.entities.Level;
import org.fyp.tmssep490be.entities.ReplacementSkillAssessment;

import java.util.List;
import java.util.Optional;

/**
 * Service for retrieving student level information from skill assessments
 * Replaces the direct student.level field with assessment-based level retrieval
 */
public interface StudentLevelService {

    /**
     * Get the latest skill assessment for a student in a specific subject
     * @param studentId Student ID
     * @param subjectId Subject ID
     * @return Latest assessment or empty if not found
     */
    Optional<ReplacementSkillAssessment> getLatestAssessmentByStudentAndSubject(Long studentId, Long subjectId);

    /**
     * Get all latest skill assessments for a student (one per subject)
     * @param studentId Student ID
     * @return List of latest assessments per subject
     */
    List<ReplacementSkillAssessment> getLatestAssessmentsByStudent(Long studentId);

    /**
     * Get the general level for a student (from GENERAL skill assessment)
     * @param studentId Student ID
     * @return Level code or "Not Assessed" if not found
     */
    String getStudentGeneralLevel(Long studentId);

    /**
     * Get the level for a student in a specific subject
     * @param studentId Student ID
     * @param subjectId Subject ID
     * @return Level code or "Not Assessed" if not found
     */
    String getStudentLevelBySubject(Long studentId, Long subjectId);

    /**
     * Create initial skill assessment for new student
     * @param studentId Student ID
     * @param levelCode Level code (e.g., "A1", "B2", "N3")
     * @param assessedBy User ID of assessor
     * @return Created assessment or empty if level not found
     */
    Optional<ReplacementSkillAssessment> createInitialAssessment(Long studentId, String levelCode, Long assessedBy);

    /**
     * Get multiple latest assessments for multiple students in a specific subject
     * @param studentIds List of student IDs
     * @param subjectId Subject ID
     * @return List of latest assessments
     */
    List<ReplacementSkillAssessment> getLatestAssessmentsForStudentsAndSubject(List<Long> studentIds, Long subjectId);

    /**
     * Check if student has been assessed in a specific subject
     * @param studentId Student ID
     * @param subjectId Subject ID
     * @return true if assessment exists
     */
    boolean hasStudentBeenAssessedInSubject(Long studentId, Long subjectId);

    /**
     * Get student proficiency summary across all subjects
     * @param studentId Student ID
     * @return Map of subject name to level code
     */
    java.util.Map<String, String> getStudentProficiencySummary(Long studentId);
}