package com.paze.pilates.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.paze.pilates.backend.TestSecurity;
import com.paze.pilates.backend.domain.AppUser;
import com.paze.pilates.backend.domain.Instructor;
import com.paze.pilates.backend.mapper.InstructorMapper;
import com.paze.pilates.backend.repository.AppUserRepository;
import com.paze.pilates.backend.repository.InstructorRepository;
import com.paze.pilates.dto.BusinessException;
import com.paze.pilates.dto.InstructorDto;
import com.paze.pilates.dto.UserRole;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InstructorServiceImplTest {

    @Mock
    private InstructorRepository instructorRepository;

    @Mock
    private AppUserRepository userRepository;

    private InstructorServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new InstructorServiceImpl(instructorRepository, userRepository, TestSecurity.adminGuard(),
                Mappers.getMapper(InstructorMapper.class));
    }

    @Test
    void getInstructors_mapsActiveInstructorsToDtos() {
        when(instructorRepository.findByActiveTrueOrderByFirstNameAscLastNameAsc())
                .thenReturn(List.of(new Instructor("Ayşe", "Yılmaz")));

        assertThat(service.getInstructors())
                .containsExactly(new InstructorDto(null, "Ayşe", "Yılmaz"));
    }

    @Test
    void saveInstructor_newInstructor_trimsAndSaves() {
        when(instructorRepository.save(any(Instructor.class))).thenAnswer(inv -> inv.getArgument(0));

        InstructorDto saved = service.saveInstructor(new InstructorDto(null, "  Ayşe ", " Yılmaz  "));

        assertThat(saved.firstName()).isEqualTo("Ayşe");
        assertThat(saved.lastName()).isEqualTo("Yılmaz");
    }

    @Test
    void saveInstructor_existingInstructor_updatesNames() {
        Instructor existing = new Instructor("Ayşe", "Yılmaz");
        when(instructorRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(instructorRepository.save(existing)).thenReturn(existing);

        service.saveInstructor(new InstructorDto(5L, "Ayşe", "Demir"));

        assertThat(existing.getLastName()).isEqualTo("Demir");
    }

    @Test
    void saveInstructor_existingNotFound_throws() {
        when(instructorRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.saveInstructor(new InstructorDto(5L, "Ayşe", "Yılmaz")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("instructor.error.not_found");
    }

    @Test
    void saveInstructor_blankFirstName_throws() {
        assertThatThrownBy(() -> service.saveInstructor(new InstructorDto(null, "  ", "Yılmaz")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("instructor.error.first_name_required");
        verify(instructorRepository, never()).save(any());
    }

    @Test
    void saveInstructor_nullLastName_throws() {
        assertThatThrownBy(() -> service.saveInstructor(new InstructorDto(null, "Ayşe", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("instructor.error.last_name_required");
    }

    @Test
    void saveInstructor_tooLongName_throws() {
        String longName = "a".repeat(51);
        assertThatThrownBy(() -> service.saveInstructor(new InstructorDto(null, longName, "Yılmaz")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("instructor.error.first_name_too_long");
    }

    @Test
    void deleteInstructor_marksInactive() {
        Instructor existing = new Instructor("Ayşe", "Yılmaz");
        when(instructorRepository.findById(5L)).thenReturn(Optional.of(existing));

        service.deleteInstructor(5L);

        assertThat(existing.isActive()).isFalse();
        verify(instructorRepository).save(existing);
    }

    @Test
    void deleteInstructor_deactivatesLinkedUser() {
        Instructor existing = new Instructor("Ayşe", "Yılmaz");
        AppUser linked = new AppUser("ayse", "hash", "Ayşe Yılmaz", UserRole.INSTRUCTOR);
        when(instructorRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(userRepository.findByInstructorId(5L)).thenReturn(Optional.of(linked));

        service.deleteInstructor(5L);

        assertThat(linked.isActive()).isFalse();
        verify(userRepository).save(linked);
    }

    @Test
    void getInstructors_instructorRoleIsForbidden() {
        InstructorServiceImpl restricted = new InstructorServiceImpl(instructorRepository, userRepository,
                TestSecurity.guardFor(TestSecurity.instructor(2L, 7L)),
                Mappers.getMapper(InstructorMapper.class));

        assertThatThrownBy(restricted::getInstructors)
                .isInstanceOf(BusinessException.class)
                .hasMessage("auth.error.forbidden");
        verify(instructorRepository, never()).findByActiveTrueOrderByFirstNameAscLastNameAsc();
    }
}
