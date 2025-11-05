package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.entities.Level;
import org.fyp.tmssep490be.entities.ReplacementSkillAssessment;
import org.fyp.tmssep490be.entities.enums.Skill;
import org.fyp.tmssep490be.entities.Student;
import org.fyp.tmssep490be.entities.UserAccount;
import org.fyp.tmssep490be.repositories.LevelRepository;
import org.fyp.tmssep490be.repositories.ReplacementSkillAssessmentRepository;
import org.fyp.tmssep490be.repositories.StudentRepository;
import org.fyp.tmssep490be.services.StudentLevelService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StudentLevelServiceImpl implements StudentLevelService {

    private final ReplacementSkillAssessmentRepository assessmentRepository;
    private final LevelRepository levelRepository;
    private final StudentRepository studentRepository;

    @Override
    public Optional<ReplacementSkillAssessment> getLatestAssessmentByStudentAndSubject(Long studentId, Long subjectId) {
        return assessmentRepository.findLatestAssessmentByStudentAndSubject(studentId, subjectId);
    }

    @Override
    public List<ReplacementSkillAssessment> getLatestAssessmentsByStudent(Long studentId) {
        // Get all assessments for the student, then group by subject and get latest from each group
        List<ReplacementSkillAssessment> allAssessments = assessmentRepository.findByStudentIdOrderByAssessmentDateDesc(studentId);

        Map<Long, ReplacementSkillAssessment> latestBySubject = new HashMap<>();

        for (ReplacementSkillAssessment assessment : allAssessments) {
            Long subjectId = assessment.getLevel().getSubject().getId();
            // Keep only the latest assessment for each subject
            if (!latestBySubject.containsKey(subjectId) ||
                assessment.getAssessmentDate().isAfter(latestBySubject.get(subjectId).getAssessmentDate()) ||
                (assessment.getAssessmentDate().isEqual(latestBySubject.get(subjectId).getAssessmentDate()) &&
                 assessment.getCreatedAt().isAfter(latestBySubject.get(subjectId).getCreatedAt()))) {
                latestBySubject.put(subjectId, assessment);
            }
        }

        return List.copyOf(latestBySubject.values());
    }

    @Override
    public String getStudentGeneralLevel(Long studentId) {
        Optional<ReplacementSkillAssessment> generalAssessment = assessmentRepository
                .findLatestAssessmentByStudentAndSkill(studentId, Skill.GENERAL);

        return generalAssessment
                .map(assessment -> assessment.getLevel().getCode())
                .orElse("Not Assessed");
    }

    @Override
    public String getStudentLevelBySubject(Long studentId, Long subjectId) {
        Optional<ReplacementSkillAssessment> assessment = getLatestAssessmentByStudentAndSubject(studentId, subjectId);

        return assessment
                .map(a -> a.getLevel().getCode())
                .orElse("Not Assessed");
    }

    @Override
    @Transactional
    public Optional<ReplacementSkillAssessment> createInitialAssessment(Long studentId, String levelCode, Long assessedBy) {
        log.debug("Creating initial assessment for student {} with level {}", studentId, levelCode);

        if (levelCode == null || levelCode.trim().isEmpty()) {
            log.debug("Level code is empty, skipping assessment creation");
            return Optional.empty();
        }

        try {
            // Find level by code
            Level level = levelRepository.findByCodeIgnoreCase(levelCode.trim())
                    .orElse(null);

            if (level == null) {
                log.warn("Level with code '{}' not found", levelCode);
                return Optional.empty();
            }

            // Verify student exists
            Student student = studentRepository.findById(studentId)
                    .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));

            // Create initial skill assessment with GENERAL skill
            ReplacementSkillAssessment assessment = new ReplacementSkillAssessment();
            assessment.setStudent(student);
            assessment.setSkill(Skill.GENERAL);
            assessment.setLevel(level);
            // Set default scores (can be updated later)
            assessment.setRawScore(BigDecimal.ZERO);
            assessment.setScaledScore(BigDecimal.ZERO);
            assessment.setScoreScale("0-100");
            assessment.setAssessmentCategory("PLACEMENT");
            assessment.setAssessmentDate(LocalDate.now());
            assessment.setAssessmentType("enrollment_initial");
            assessment.setNote("Initial assessment created during student enrollment");

            // Set assessed by user if provided
            if (assessedBy != null) {
                UserAccount assessedByUser = new UserAccount();
                assessedByUser.setId(assessedBy);
                assessment.setAssessedBy(assessedByUser);
            }

            ReplacementSkillAssessment savedAssessment = assessmentRepository.save(assessment);
            log.debug("Created initial skill assessment for student {} at level {}", studentId, levelCode);

            return Optional.of(savedAssessment);

        } catch (Exception e) {
            log.error("Failed to create initial skill assessment for student {}: {}", studentId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public List<ReplacementSkillAssessment> getLatestAssessmentsForStudentsAndSubject(List<Long> studentIds, Long subjectId) {
        if (studentIds.isEmpty()) {
            return List.of();
        }
        return assessmentRepository.findLatestAssessmentsByStudentsAndSubject(studentIds, subjectId);
    }

    @Override
    public boolean hasStudentBeenAssessedInSubject(Long studentId, Long subjectId) {
        return assessmentRepository.existsByStudentIdAndLevelSubjectId(studentId, subjectId);
    }

    @Override
    public Map<String, String> getStudentProficiencySummary(Long studentId) {
        List<ReplacementSkillAssessment> latestAssessments = getLatestAssessmentsByStudent(studentId);

        return latestAssessments.stream()
                .collect(Collectors.toMap(
                        assessment -> assessment.getLevel().getSubject().getName(),
                        assessment -> assessment.getLevel().getCode(),
                        (existing, replacement) -> existing // Keep existing if duplicate keys
                ));
    }

    /**
     * Create multiple skill assessments for a student from skill assessment data
     * Used for Excel import with multi-skill data
     */
    @Transactional
    public List<ReplacementSkillAssessment> createMultipleAssessments(
            Long studentId,
            Map<Skill, org.fyp.tmssep490be.dtos.enrollment.SkillAssessmentData> skillDataMap,
            Long assessedBy
    ) {
        log.debug("Creating multiple assessments for student {} with {} skills", studentId, skillDataMap.size());

        // Verify student exists
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));

        List<ReplacementSkillAssessment> createdAssessments = new ArrayList<>();

        for (Map.Entry<Skill, org.fyp.tmssep490be.dtos.enrollment.SkillAssessmentData> entry : skillDataMap.entrySet()) {
            Skill skill = entry.getKey();
            org.fyp.tmssep490be.dtos.enrollment.SkillAssessmentData data = entry.getValue();

            if (data == null || !data.isValid()) {
                log.debug("Skipping {} assessment for student {} - invalid data", skill, studentId);
                continue;
            }

            try {
                // Find level by code
                Level level = levelRepository.findByCodeIgnoreCase(data.getLevelCode())
                        .orElse(null);

                if (level == null) {
                    log.warn("Level with code '{}' not found for skill {}, student {}",
                            data.getLevelCode(), skill, studentId);
                    continue;
                }

                // Create skill assessment
                ReplacementSkillAssessment assessment = new ReplacementSkillAssessment();
                assessment.setStudent(student);
                assessment.setSkill(skill);
                assessment.setLevel(level);
                // New flexible scoring fields
                assessment.setRawScore(data.getRawScore());
                assessment.setScaledScore(data.getScaledScore());
                assessment.setScoreScale(data.getScoreScale());
                assessment.setAssessmentCategory(data.getAssessmentCategory());
                assessment.setAssessmentDate(LocalDate.now());
                assessment.setAssessmentType("bulk_import");
                assessment.setNote("Bulk import of multiple skill assessments");

                // Set assessed by user if provided
                if (assessedBy != null) {
                    UserAccount assessedByUser = new UserAccount();
                    assessedByUser.setId(assessedBy);
                    assessment.setAssessedBy(assessedByUser);
                }

                ReplacementSkillAssessment savedAssessment = assessmentRepository.save(assessment);
                createdAssessments.add(savedAssessment);
                log.debug("Created {} assessment for student {} at level {} with scaled score {}",
                        skill, studentId, data.getLevelCode(), data.getScaledScore());

            } catch (Exception e) {
                log.error("Failed to create {} assessment for student {}: {}",
                        skill, studentId, e.getMessage(), e);
                // Continue with other assessments even if one fails
            }
        }

        log.debug("Successfully created {} assessments for student {}", createdAssessments.size(), studentId);
        return createdAssessments;
    }

    /**
     * Create a single skill assessment with skill-specific data
     */
    @Transactional
    public Optional<ReplacementSkillAssessment> createSingleAssessment(
            Long studentId,
            Skill skill,
            String levelCode,
            Integer score,
            String assessmentType,
            String note,
            Long assessedBy
    ) {
        log.debug("Creating {} assessment for student {} with level {} and score {}",
                skill, studentId, levelCode, score);

        if (levelCode == null || levelCode.trim().isEmpty()) {
            log.debug("Level code is empty, skipping assessment creation");
            return Optional.empty();
        }

        try {
            // Verify student exists
            Student student = studentRepository.findById(studentId)
                    .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));

            // Find level by code
            Level level = levelRepository.findByCodeIgnoreCase(levelCode.trim())
                    .orElse(null);

            if (level == null) {
                log.warn("Level with code '{}' not found", levelCode);
                return Optional.empty();
            }

            // Create skill assessment
            ReplacementSkillAssessment assessment = new ReplacementSkillAssessment();
            assessment.setStudent(student);
            assessment.setSkill(skill);
            assessment.setLevel(level);
            assessment.setRawScore(score != null ? BigDecimal.valueOf(score) : BigDecimal.ZERO);
            assessment.setScaledScore(score != null ? BigDecimal.valueOf(score) : BigDecimal.ZERO);
            assessment.setScoreScale("0-100");
            assessment.setAssessmentCategory("PLACEMENT");
            assessment.setAssessmentDate(LocalDate.now());
            assessment.setAssessmentType(assessmentType != null ? assessmentType : "manual_entry");
            assessment.setNote(note != null ? note : "Manual assessment entry");

            // Set assessed by user if provided
            if (assessedBy != null) {
                UserAccount assessedByUser = new UserAccount();
                assessedByUser.setId(assessedBy);
                assessment.setAssessedBy(assessedByUser);
            }

            ReplacementSkillAssessment savedAssessment = assessmentRepository.save(assessment);
            log.debug("Created {} assessment for student {} at level {} with score {}",
                    skill, studentId, levelCode, score);

            return Optional.of(savedAssessment);

        } catch (Exception e) {
            log.error("Failed to create {} assessment for student {}: {}",
                    skill, studentId, e.getMessage(), e);
            return Optional.empty();
        }
    }
}