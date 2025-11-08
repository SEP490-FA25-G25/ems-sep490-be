package org.fyp.tmssep490be.services.impl;

import org.fyp.tmssep490be.dtos.curriculum.SubjectWithLevelsDTO;
import org.fyp.tmssep490be.entities.Level;
import org.fyp.tmssep490be.entities.Subject;
import org.fyp.tmssep490be.entities.enums.SubjectStatus;
import org.fyp.tmssep490be.repositories.LevelRepository;
import org.fyp.tmssep490be.repositories.SubjectRepository;
import org.fyp.tmssep490be.services.CurriculumService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("CurriculumService Unit Tests")
class CurriculumServiceImplTest {

    @Autowired
    private CurriculumService curriculumService;

    @MockitoBean
    private SubjectRepository subjectRepository;

    @MockitoBean
    private LevelRepository levelRepository;

    private Subject englishSubject;
    private Subject chineseSubject;
    private List<Level> englishLevels;
    private List<Level> chineseLevels;

    @BeforeEach
    void setUp() {
        // Create test subjects
        englishSubject = Subject.builder()
                .id(1L)
                .code("ENG")
                .name("English")
                .description("English Language Courses")
                .status(SubjectStatus.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .build();

        chineseSubject = Subject.builder()
                .id(2L)
                .code("CHI")
                .name("Chinese")
                .description("Chinese Language Courses")
                .status(SubjectStatus.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .build();

        // Create test levels for English
        englishLevels = List.of(
                Level.builder()
                        .id(101L)
                        .subject(englishSubject)
                        .code("A1")
                        .name("Beginner A1")
                        .description("Beginner Level A1")
                        .expectedDurationHours(80)
                        .sortOrder(1)
                        .build(),
                Level.builder()
                        .id(102L)
                        .subject(englishSubject)
                        .code("A2")
                        .name("Beginner A2")
                        .description("Beginner Level A2")
                        .expectedDurationHours(100)
                        .sortOrder(2)
                        .build(),
                Level.builder()
                        .id(103L)
                        .subject(englishSubject)
                        .code("B1")
                        .name("Intermediate B1")
                        .description("Intermediate Level B1")
                        .expectedDurationHours(120)
                        .sortOrder(3)
                        .build()
        );

        // Create test levels for Chinese
        chineseLevels = List.of(
                Level.builder()
                        .id(201L)
                        .subject(chineseSubject)
                        .code("HSK1")
                        .name("HSK Level 1")
                        .description("HSK Level 1")
                        .expectedDurationHours(60)
                        .sortOrder(1)
                        .build(),
                Level.builder()
                        .id(202L)
                        .subject(chineseSubject)
                        .code("HSK2")
                        .name("HSK Level 2")
                        .description("HSK Level 2")
                        .expectedDurationHours(80)
                        .sortOrder(2)
                        .build()
        );
    }

    @Test
    @DisplayName("Should return all subjects with their levels sorted correctly")
    void shouldReturnAllSubjectsWithLevelsSortedCorrectly() {
        // Given
        List<Subject> subjects = List.of(englishSubject, chineseSubject);
        when(subjectRepository.findByStatusOrderByCode(SubjectStatus.ACTIVE)).thenReturn(subjects);
        when(levelRepository.findBySubjectIdOrderBySortOrderAsc(1L)).thenReturn(englishLevels);
        when(levelRepository.findBySubjectIdOrderBySortOrderAsc(2L)).thenReturn(chineseLevels);

        // When
        List<SubjectWithLevelsDTO> result = curriculumService.getAllSubjectsWithLevels();

        // Then
        assertThat(result).hasSize(2);

        // Verify subjects are sorted by code
        assertThat(result)
                .extracting("code", "name")
                .containsExactly(
                        tuple("ENG", "English"),
                        tuple("CHI", "Chinese")
                );

        // Verify English subject and levels
        SubjectWithLevelsDTO englishResult = result.get(0);
        assertThat(englishResult.getId()).isEqualTo(1L);
        assertThat(englishResult.getCode()).isEqualTo("ENG");
        assertThat(englishResult.getName()).isEqualTo("English");
        assertThat(englishResult.getLevels()).hasSize(3);

        // Verify English levels are sorted by sortOrder
        assertThat(englishResult.getLevels())
                .extracting("code", "name", "sortOrder")
                .containsExactly(
                        tuple("A1", "Beginner A1", 1),
                        tuple("A2", "Beginner A2", 2),
                        tuple("B1", "Intermediate B1", 3)
                );

        // Verify Chinese subject and levels
        SubjectWithLevelsDTO chineseResult = result.get(1);
        assertThat(chineseResult.getId()).isEqualTo(2L);
        assertThat(chineseResult.getCode()).isEqualTo("CHI");
        assertThat(chineseResult.getName()).isEqualTo("Chinese");
        assertThat(chineseResult.getLevels()).hasSize(2);

        // Verify Chinese levels are sorted by sortOrder
        assertThat(chineseResult.getLevels())
                .extracting("code", "name", "sortOrder")
                .containsExactly(
                        tuple("HSK1", "HSK Level 1", 1),
                        tuple("HSK2", "HSK Level 2", 2)
                );

        // Verify repository interactions
        verify(subjectRepository, times(1)).findByStatusOrderByCode(SubjectStatus.ACTIVE);
        verify(levelRepository, times(1)).findBySubjectIdOrderBySortOrderAsc(1L);
        verify(levelRepository, times(1)).findBySubjectIdOrderBySortOrderAsc(2L);
    }

    @Test
    @DisplayName("Should return empty list when no active subjects exist")
    void shouldReturnEmptyListWhenNoActiveSubjectsExist() {
        // Given
        when(subjectRepository.findByStatusOrderByCode(SubjectStatus.ACTIVE)).thenReturn(List.of());

        // When
        List<SubjectWithLevelsDTO> result = curriculumService.getAllSubjectsWithLevels();

        // Then
        assertThat(result).isEmpty();

        verify(subjectRepository, times(1)).findByStatusOrderByCode(SubjectStatus.ACTIVE);
        verify(levelRepository, never()).findBySubjectIdOrderBySortOrderAsc(any());
    }

    @Test
    @DisplayName("Should handle subject with no levels")
    void shouldHandleSubjectWithNoLevels() {
        // Given
        Subject subjectWithoutLevels = Subject.builder()
                .id(3L)
                .code("NO_LEVELS")
                .name("Subject Without Levels")
                .status(SubjectStatus.ACTIVE)
                .build();

        List<Subject> subjects = List.of(subjectWithoutLevels);
        when(subjectRepository.findByStatusOrderByCode(SubjectStatus.ACTIVE)).thenReturn(subjects);
        when(levelRepository.findBySubjectIdOrderBySortOrderAsc(3L)).thenReturn(List.of());

        // When
        List<SubjectWithLevelsDTO> result = curriculumService.getAllSubjectsWithLevels();

        // Then
        assertThat(result).hasSize(1);
        SubjectWithLevelsDTO subjectResult = result.get(0);
        assertThat(subjectResult.getCode()).isEqualTo("NO_LEVELS");
        assertThat(subjectResult.getLevels()).isEmpty();

        verify(levelRepository, times(1)).findBySubjectIdOrderBySortOrderAsc(3L);
    }

    @Test
    @DisplayName("Should handle mixed active and inactive subjects")
    void shouldHandleMixedActiveAndInactiveSubjects() {
        // Given
        Subject inactiveSubject = Subject.builder()
                .id(3L)
                .code("INACTIVE")
                .name("Inactive Subject")
                .status(SubjectStatus.INACTIVE)
                .build();

        List<Subject> activeSubjects = List.of(englishSubject, chineseSubject);
        when(subjectRepository.findByStatusOrderByCode(SubjectStatus.ACTIVE)).thenReturn(activeSubjects);
        when(levelRepository.findBySubjectIdOrderBySortOrderAsc(1L)).thenReturn(englishLevels);
        when(levelRepository.findBySubjectIdOrderBySortOrderAsc(2L)).thenReturn(chineseLevels);

        // When
        List<SubjectWithLevelsDTO> result = curriculumService.getAllSubjectsWithLevels();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting("code")
                .containsExactly("ENG", "CHI")
                .doesNotContain("INACTIVE");
    }

    @Test
    @DisplayName("Should preserve all level information in DTO")
    void shouldPreserveAllLevelInformationInDTO() {
        // Given
        when(subjectRepository.findByStatusOrderByCode(SubjectStatus.ACTIVE)).thenReturn(List.of(englishSubject));
        when(levelRepository.findBySubjectIdOrderBySortOrderAsc(1L)).thenReturn(englishLevels);

        // When
        List<SubjectWithLevelsDTO> result = curriculumService.getAllSubjectsWithLevels();

        // Then
        assertThat(result).hasSize(1);
        SubjectWithLevelsDTO subjectResult = result.get(0);
        assertThat(subjectResult.getCode()).isEqualTo("ENG");

        List<SubjectWithLevelsDTO.LevelDTO> levelDTOs = subjectResult.getLevels();
        assertThat(levelDTOs).hasSize(3);

        SubjectWithLevelsDTO.LevelDTO a1Level = levelDTOs.get(0);
        assertThat(a1Level.getId()).isEqualTo(101L);
        assertThat(a1Level.getCode()).isEqualTo("A1");
        assertThat(a1Level.getName()).isEqualTo("Beginner A1");
        assertThat(a1Level.getDescription()).isEqualTo("Beginner Level A1");
        assertThat(a1Level.getExpectedDurationHours()).isEqualTo(80);
        assertThat(a1Level.getSortOrder()).isEqualTo(1);
    }
}