package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.curriculum.SubjectWithLevelsDTO;
import org.fyp.tmssep490be.entities.Level;
import org.fyp.tmssep490be.entities.Subject;
import org.fyp.tmssep490be.entities.enums.SubjectStatus;
import org.fyp.tmssep490be.repositories.LevelRepository;
import org.fyp.tmssep490be.repositories.SubjectRepository;
import org.fyp.tmssep490be.services.CurriculumService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CurriculumServiceImpl implements CurriculumService {

    private final SubjectRepository subjectRepository;
    private final LevelRepository levelRepository;

    @Override
    public List<SubjectWithLevelsDTO> getAllSubjectsWithLevels() {
        log.debug("Fetching all subjects with their levels");

        // Get all subjects with ACTIVE status
        List<Subject> subjects = subjectRepository.findByStatusOrderByCode(SubjectStatus.ACTIVE);

        List<SubjectWithLevelsDTO> result = subjects.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        log.debug("Found {} subjects with levels", result.size());
        return result;
    }

    private SubjectWithLevelsDTO convertToDTO(Subject subject) {
        // Get levels for this subject, sorted by sort_order
        List<Level> levels = levelRepository.findBySubjectIdOrderBySortOrderAsc(subject.getId());

        List<SubjectWithLevelsDTO.LevelDTO> levelDTOs = levels.stream()
                .map(this::convertLevelToDTO)
                .collect(Collectors.toList());

        return SubjectWithLevelsDTO.builder()
                .id(subject.getId())
                .code(subject.getCode())
                .name(subject.getName())
                .description(subject.getDescription())
                .status(subject.getStatus().name())
                .createdAt(subject.getCreatedAt())
                .levels(levelDTOs)
                .build();
    }

    private SubjectWithLevelsDTO.LevelDTO convertLevelToDTO(Level level) {
        return SubjectWithLevelsDTO.LevelDTO.builder()
                .id(level.getId())
                .code(level.getCode())
                .name(level.getName())
                .description(level.getDescription())
                .expectedDurationHours(level.getExpectedDurationHours())
                .sortOrder(level.getSortOrder())
                .build();
    }
}