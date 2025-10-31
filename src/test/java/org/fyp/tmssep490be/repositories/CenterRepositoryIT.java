package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.config.AbstractRepositoryTest;
import org.fyp.tmssep490be.entities.Center;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CenterRepository Integration Tests")
class CenterRepositoryIT extends AbstractRepositoryTest {

    @Autowired
    private CenterRepository centerRepository;

    @BeforeEach
    void setUp() {
        centerRepository.deleteAll();
    }

    @Test
    @DisplayName("Should save and retrieve center with timestamps")
    void shouldSaveAndRetrieveCenterWithTimestamps() {
        // Arrange
        Center center = Center.builder()
                .code("TC001")
                .name("Test Center")
                .description("Test Description")
                .phone("0123456789")
                .email("test@center.com")
                .address("123 Test Street")
                .build();

        // Act
        Center savedCenter = centerRepository.save(center);

        // Assert
        assertThat(savedCenter.getId()).isNotNull();
        assertThat(savedCenter.getCode()).isEqualTo("TC001");
        assertThat(savedCenter.getName()).isEqualTo("Test Center");
        assertThat(savedCenter.getCreatedAt()).isNotNull();
        assertThat(savedCenter.getUpdatedAt()).isNotNull();

        // Retrieve
        Optional<Center> retrieved = centerRepository.findById(savedCenter.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getCode()).isEqualTo("TC001");
    }

    @Test
    @DisplayName("Should find center by code")
    void shouldFindCenterByCode() {
        // Arrange
        Center center = Center.builder()
                .code("TC001")
                .name("Test Center")
                .build();
        centerRepository.save(center);

        // Act
        Optional<Center> found = centerRepository.findByCode("TC001");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test Center");
    }

    @Test
    @DisplayName("Should check if center exists by code")
    void shouldCheckIfCenterExistsByCode() {
        // Arrange
        Center center = Center.builder()
                .code("TC001")
                .name("Test Center")
                .build();
        centerRepository.save(center);

        // Act & Assert
        assertThat(centerRepository.existsByCode("TC001")).isTrue();
        assertThat(centerRepository.existsByCode("NON_EXISTENT")).isFalse();
    }

    @Test
    @DisplayName("Should update center and modify updatedAt timestamp")
    void shouldUpdateCenterAndModifyTimestamp() throws InterruptedException {
        // Arrange
        Center center = Center.builder()
                .code("TC001")
                .name("Original Name")
                .build();
        Center saved = centerRepository.save(center);
        centerRepository.flush();

        // Capture original timestamps
        OffsetDateTime originalCreatedAt = saved.getCreatedAt();
        OffsetDateTime originalUpdatedAt = saved.getUpdatedAt();

        // Small delay to ensure different timestamp
        Thread.sleep(100);

        // Act
        saved.setName("Updated Name");
        Center updated = centerRepository.save(saved);
        centerRepository.flush();

        // Assert
        assertThat(updated.getName()).isEqualTo("Updated Name");
        assertThat(updated.getCreatedAt()).isEqualTo(originalCreatedAt);
        assertThat(updated.getUpdatedAt()).isAfter(originalUpdatedAt);
    }

    @Test
    @DisplayName("Should delete center")
    void shouldDeleteCenter() {
        // Arrange
        Center center = Center.builder()
                .code("TC001")
                .name("Test Center")
                .build();
        Center saved = centerRepository.save(center);

        // Act
        centerRepository.deleteById(saved.getId());

        // Assert
        assertThat(centerRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    @DisplayName("Should find all centers")
    void shouldFindAllCenters() {
        // Arrange
        Center center1 = Center.builder()
                .code("TC001")
                .name("Center 1")
                .email("center1@test.com")
                .build();

        Center center2 = Center.builder()
                .code("TC002")
                .name("Center 2")
                .email("center2@test.com")
                .build();

        centerRepository.save(center1);
        centerRepository.save(center2);

        // Act
        List<Center> centers = centerRepository.findAll();

        // Assert
        assertThat(centers).hasSize(2);
        assertThat(centers)
                .extracting(Center::getCode)
                .containsExactlyInAnyOrder("TC001", "TC002");
    }

    @Test
    @DisplayName("Should count centers")
    void shouldCountCenters() {
        // Arrange
        centerRepository.save(Center.builder().code("TC001").name("C1").email("c1@test.com").build());
        centerRepository.save(Center.builder().code("TC002").name("C2").email("c2@test.com").build());
        centerRepository.save(Center.builder().code("TC003").name("C3").email("c3@test.com").build());

        // Act
        long count = centerRepository.count();

        // Assert
        assertThat(count).isEqualTo(3);
    }
}
