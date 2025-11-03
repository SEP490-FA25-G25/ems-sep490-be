package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.ReplacementSkillAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReplacementSkillAssessmentRepository extends JpaRepository<ReplacementSkillAssessment, Long> {

    /**
     * Find latest skill assessment for a student that matches a specific subject
     * Used for smart student sorting when enrolling in classes
     */
    @Query("SELECT rsa FROM ReplacementSkillAssessment rsa " +
           "INNER JOIN rsa.level l " +
           "INNER JOIN l.subject s " +
           "WHERE rsa.student.id = :studentId " +
           "AND s.id = :subjectId " +
           "ORDER BY rsa.assessmentDate DESC, rsa.createdAt DESC " +
           "LIMIT 1")
    Optional<ReplacementSkillAssessment> findLatestAssessmentByStudentAndSubject(
            @Param("studentId") Long studentId,
            @Param("subjectId") Long subjectId
    );

    /**
     * Find all assessments for multiple students and specific subject
     * Used for batch processing when loading available students
     */
    @Query("SELECT rsa FROM ReplacementSkillAssessment rsa " +
           "INNER JOIN rsa.level l " +
           "INNER JOIN l.subject s " +
           "WHERE rsa.student.id IN :studentIds " +
           "AND s.id = :subjectId " +
           "AND rsa.id IN (" +
           "  SELECT MAX(rsa2.id) FROM ReplacementSkillAssessment rsa2 " +
           "  INNER JOIN rsa2.level l2 " +
           "  INNER JOIN l2.subject s2 " +
           "  WHERE rsa2.student.id = rsa.student.id " +
           "  AND s2.id = :subjectId " +
           "  GROUP BY rsa2.student.id" +
           ")")
    List<ReplacementSkillAssessment> findLatestAssessmentsByStudentsAndSubject(
            @Param("studentIds") List<Long> studentIds,
            @Param("subjectId") Long subjectId
    );

    /**
     * Find latest skill assessment for a student with specific skill
     * Used for getting general level or specific skill level
     */
    @Query("SELECT rsa FROM ReplacementSkillAssessment rsa " +
           "INNER JOIN rsa.level l " +
           "WHERE rsa.student.id = :studentId " +
           "AND rsa.skill = :skill " +
           "ORDER BY rsa.assessmentDate DESC, rsa.createdAt DESC " +
           "LIMIT 1")
    Optional<ReplacementSkillAssessment> findLatestAssessmentByStudentAndSkill(
            @Param("studentId") Long studentId,
            @Param("skill") org.fyp.tmssep490be.entities.enums.Skill skill
    );

    /**
     * Find all assessments for a student, ordered by date (newest first)
     */
    List<ReplacementSkillAssessment> findByStudentIdOrderByAssessmentDateDesc(Long studentId);

    /**
     * Check if student has any assessment in a specific subject
     */
    @Query("SELECT CASE WHEN COUNT(rsa) > 0 THEN true ELSE false END " +
           "FROM ReplacementSkillAssessment rsa " +
           "INNER JOIN rsa.level l " +
           "WHERE rsa.student.id = :studentId " +
           "AND l.subject.id = :subjectId")
    boolean existsByStudentIdAndLevelSubjectId(
            @Param("studentId") Long studentId,
            @Param("subjectId") Long subjectId
    );
}
