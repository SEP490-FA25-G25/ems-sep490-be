package org.fyp.tmssep490be.services.impl;

import org.fyp.tmssep490be.dtos.center.CenterRequest;
import org.fyp.tmssep490be.dtos.center.CenterResponse;
import org.fyp.tmssep490be.entities.Center;
import org.fyp.tmssep490be.repositories.CenterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CenterService Unit Tests")
class CenterServiceImplTest {

    @Mock
    private CenterRepository centerRepository;

    @InjectMocks
    private CenterServiceImpl centerService;

    private Center testCenter;
    private CenterRequest testRequest;

    @BeforeEach
    void setUp() {
        testCenter = Center.builder()
                .id(1L)
                .code("TC001")
                .name("Test Center")
                .description("Test Description")
                .phone("0123456789")
                .email("test@center.com")
                .address("Test Address")
                .build();

        testRequest = CenterRequest.builder()
                .code("TC001")
                .name("Test Center")
                .description("Test Description")
                .phone("0123456789")
                .email("test@center.com")
                .address("Test Address")
                .build();
    }

    @Test
    @DisplayName("Should create center successfully")
    void shouldCreateCenterSuccessfully() {
        // Arrange
        when(centerRepository.save(any(Center.class))).thenReturn(testCenter);

        // Act
        CenterResponse response = centerService.createCenter(testRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getCode()).isEqualTo("TC001");
        assertThat(response.getName()).isEqualTo("Test Center");
        verify(centerRepository).save(any(Center.class));
    }

    @Test
    @DisplayName("Should get center by id successfully")
    void shouldGetCenterByIdSuccessfully() {
        // Arrange
        when(centerRepository.findById(1L)).thenReturn(Optional.of(testCenter));

        // Act
        CenterResponse response = centerService.getCenterById(1L);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getCode()).isEqualTo("TC001");
        verify(centerRepository).findById(1L);
    }

    @Test
    @DisplayName("Should throw exception when center not found")
    void shouldThrowExceptionWhenCenterNotFound() {
        // Arrange
        when(centerRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> centerService.getCenterById(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Center not found with id: 999");
        verify(centerRepository).findById(999L);
    }

    @Test
    @DisplayName("Should get all centers with pagination")
    void shouldGetAllCentersWithPagination() {
        // Arrange
        Center center2 = Center.builder()
                .id(2L)
                .code("TC002")
                .name("Test Center 2")
                .build();

        Page<Center> centerPage = new PageImpl<>(Arrays.asList(testCenter, center2));
        Pageable pageable = PageRequest.of(0, 20);

        when(centerRepository.findAll(pageable)).thenReturn(centerPage);

        // Act
        Page<CenterResponse> response = centerService.getAllCenters(pageable);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getContent().get(0).getCode()).isEqualTo("TC001");
        assertThat(response.getContent().get(1).getCode()).isEqualTo("TC002");
        verify(centerRepository).findAll(pageable);
    }

    @Test
    @DisplayName("Should update center successfully")
    void shouldUpdateCenterSuccessfully() {
        // Arrange
        CenterRequest updateRequest = CenterRequest.builder()
                .code("TC001")
                .name("Updated Center Name")
                .description("Updated Description")
                .phone("0987654321")
                .email("updated@center.com")
                .address("Updated Address")
                .build();

        Center updatedCenter = Center.builder()
                .id(1L)
                .code("TC001")
                .name("Updated Center Name")
                .description("Updated Description")
                .phone("0987654321")
                .email("updated@center.com")
                .address("Updated Address")
                .build();

        when(centerRepository.findById(1L)).thenReturn(Optional.of(testCenter));
        when(centerRepository.save(any(Center.class))).thenReturn(updatedCenter);

        // Act
        CenterResponse response = centerService.updateCenter(1L, updateRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Updated Center Name");
        assertThat(response.getEmail()).isEqualTo("updated@center.com");
        verify(centerRepository).findById(1L);
        verify(centerRepository).save(any(Center.class));
    }

    @Test
    @DisplayName("Should delete center successfully")
    void shouldDeleteCenterSuccessfully() {
        // Arrange
        when(centerRepository.existsById(1L)).thenReturn(true);
        doNothing().when(centerRepository).deleteById(1L);

        // Act
        centerService.deleteCenter(1L);

        // Assert
        verify(centerRepository).existsById(1L);
        verify(centerRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent center")
    void shouldThrowExceptionWhenDeletingNonExistentCenter() {
        // Arrange
        when(centerRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> centerService.deleteCenter(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Center not found with id: 999");
        verify(centerRepository).existsById(999L);
        verify(centerRepository, never()).deleteById(any());
    }
}
