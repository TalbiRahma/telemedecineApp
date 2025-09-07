package com.telemedecine.api.speciality;

import com.telemedecine.api.dao.SpecialtyRepository;
import com.telemedecine.api.dto.SpecialtyDto;
import com.telemedecine.api.mapper.SpecialtyMapper;
import com.telemedecine.api.model.Specialty;
import com.telemedecine.api.service.impl.SpecialtyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SpecialtyServiceImpl Unit Tests")
class SpecialtyServiceImplTest {

    @Mock
    private SpecialtyRepository specialtyRepository;

    @Mock
    private SpecialtyMapper specialtyMapper;

    @InjectMocks
    private SpecialtyServiceImpl specialtyService;

    private SpecialtyDto specialtyDto;
    private Specialty specialty;

    @BeforeEach
    void setUp() {
        specialtyDto = SpecialtyDto.builder()
                .id(1L)
                .name("Neurologie")
                .description("Spécialité du système nerveux")
                .build();

        specialty = Specialty.builder()
                .id(1L)
                .name("Neurologie")
                .description("Spécialité du système nerveux")
                .build();
    }

    @Nested
    @DisplayName("Create Specialty Tests")
    class CreateSpecialtyRequest {

        @Test
        @DisplayName("Should create specialty successfully when valid name and description exist")
        void shouldCreateSpecialtySuccessfully() {
            // Arrange
            when(specialtyMapper.toEntity(specialtyDto)).thenReturn(specialty);
            when(specialtyRepository.save(specialty)).thenReturn(specialty);
            when(specialtyMapper.toDto(specialty)).thenReturn(specialtyDto);

            // Act
            SpecialtyDto result = specialtyService.createSpecialty(specialtyDto);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Neurologie");
            assertThat(result.getDescription()).isEqualTo("Spécialité du système nerveux");

            // Verify interactions
            verify(specialtyMapper, times(1)).toEntity(specialtyDto);
            verify(specialtyRepository, times(1)).save(specialty);
            verify(specialtyMapper, times(1)).toDto(specialty);
        }
    }

    @Nested
    @DisplayName("Get Specialty Tests")
    class GetSpecialtyRequest {

        @Test
        @DisplayName("Should get specialty by id successfully")
        void shouldGetSpecialtyByIdSuccessfully() {
            // Arrange
            when(specialtyRepository.findById(1L)).thenReturn(java.util.Optional.of(specialty));
            when(specialtyMapper.toDto(specialty)).thenReturn(specialtyDto);

            // Act
            SpecialtyDto result = specialtyService.getSpecialtyById(1L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            verify(specialtyRepository, times(1)).findById(1L);
            verify(specialtyMapper, times(1)).toDto(specialty);
        }
    }

    @Nested
    @DisplayName("Update Specialty Tests")
    class UpdateSpecialtyRequest {

        @Test
        @DisplayName("Should update specialty successfully")
        void shouldUpdateSpecialtySuccessfully() {
            // Arrange
            SpecialtyDto updatedDto = SpecialtyDto.builder()
                    .name("Updated Neurology")
                    .description("Updated description")
                    .build();

            when(specialtyRepository.findById(1L)).thenReturn(java.util.Optional.of(specialty));
            when(specialtyRepository.save(specialty)).thenReturn(specialty);
            when(specialtyMapper.toDto(specialty)).thenReturn(updatedDto);

            // Act
            SpecialtyDto result = specialtyService.updateSpecialty(updatedDto, 1L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Updated Neurology");
            verify(specialtyRepository, times(1)).findById(1L);
            verify(specialtyRepository, times(1)).save(specialty);
        }
    }

    @Nested
    @DisplayName("Delete Specialty Tests")
    class DeleteSpecialtyRequest {

        @Test
        @DisplayName("Should delete specialty successfully")
        void shouldDeleteSpecialtySuccessfully() {
            // Arrange
            when(specialtyRepository.findById(1L)).thenReturn(java.util.Optional.of(specialty));
            doNothing().when(specialtyRepository).delete(specialty);

            // Act
            specialtyService.deleteSpecialty(1L);

            // Assert
            verify(specialtyRepository, times(1)).findById(1L);
            verify(specialtyRepository, times(1)).delete(specialty);
        }
    }

    @Nested
    @DisplayName("Get All Specialties Tests")
    class GetAllSpecialtiesRequest {

        @Test
        @DisplayName("Should get all specialties successfully")
        void shouldGetAllSpecialtiesSuccessfully() {
            // Arrange
            when(specialtyRepository.findAll()).thenReturn(java.util.List.of(specialty));
            when(specialtyMapper.toDtoList(java.util.List.of(specialty))).thenReturn(java.util.List.of(specialtyDto));

            // Act
            var result = specialtyService.getAllSpecialties();

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.size()).isEqualTo(1);
            verify(specialtyRepository, times(1)).findAll();
            verify(specialtyMapper, times(1)).toDtoList(java.util.List.of(specialty));
        }
    }
}