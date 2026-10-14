package com.paze.pilates.backend.service;

import com.paze.pilates.backend.domain.Student;
import com.paze.pilates.backend.repository.StudentRepository;
import com.paze.pilates.dto.BusinessException;
import com.paze.pilates.dto.StudentDto;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentServiceImplTest {

    @Mock
    private StudentRepository repository;

    private StudentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new StudentServiceImpl(repository);
    }

    // ── findAll ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAll: aktif öğrencileri döner")
    void findAll_returnsActiveStudents() {
        Student s = makeStudent(1L, "Ayşe", "Yılmaz", "05551234567");
        when(repository.findByActiveTrueOrderByLastNameAscFirstNameAsc()).thenReturn(List.of(s));

        List<StudentDto> result = service.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).firstName()).isEqualTo("Ayşe");
        assertThat(result.get(0).lastName()).isEqualTo("Yılmaz");
    }

    // ── search ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("search: boş terim ile tüm aktif öğrenciler döner")
    void search_emptyTerm_returnsAll() {
        when(repository.findByActiveTrueOrderByLastNameAscFirstNameAsc()).thenReturn(List.of());
        service.search("");
        verify(repository).findByActiveTrueOrderByLastNameAscFirstNameAsc();
        verify(repository, never()).searchActive(anyString());
    }

    @Test
    @DisplayName("search: dolu terim ile searchActive çağrılır")
    void search_withTerm_callsSearchActive() {
        when(repository.searchActive("Ayşe")).thenReturn(List.of());
        service.search("Ayşe");
        verify(repository).searchActive("Ayşe");
    }

    // ── save (yeni) ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("save yeni: geçerli dto kaydedilir")
    void save_new_validDto_savesStudent() {
        StudentDto dto = new StudentDto(null, "Ayşe", "Yılmaz", "0555", null, null, null, true);
        when(repository.existsByPhoneAndActiveTrue("0555")).thenReturn(false);
        Student saved = makeStudent(10L, "Ayşe", "Yılmaz", "0555");
        when(repository.save(any())).thenReturn(saved);

        StudentDto result = service.save(dto);

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.firstName()).isEqualTo("Ayşe");
    }

    @Test
    @DisplayName("save yeni: ad boşsa BusinessException fırlatır")
    void save_new_emptyFirstName_throws() {
        StudentDto dto = new StudentDto(null, "", "Yılmaz", "0555", null, null, null, true);
        assertThatThrownBy(() -> service.save(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessage("student.error.firstname_required");
    }

    @Test
    @DisplayName("save yeni: soyad boşsa BusinessException fırlatır")
    void save_new_emptyLastName_throws() {
        StudentDto dto = new StudentDto(null, "Ayşe", "", "0555", null, null, null, true);
        assertThatThrownBy(() -> service.save(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessage("student.error.lastname_required");
    }

    @Test
    @DisplayName("save yeni: telefon boşsa BusinessException fırlatır")
    void save_new_emptyPhone_throws() {
        StudentDto dto = new StudentDto(null, "Ayşe", "Yılmaz", "", null, null, null, true);
        assertThatThrownBy(() -> service.save(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessage("student.error.phone_required");
    }

    @Test
    @DisplayName("save yeni: mükerrer telefon BusinessException fırlatır")
    void save_new_duplicatePhone_throws() {
        StudentDto dto = new StudentDto(null, "Ayşe", "Yılmaz", "0555", null, null, null, true);
        when(repository.existsByPhoneAndActiveTrue("0555")).thenReturn(true);
        assertThatThrownBy(() -> service.save(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessage("student.error.phone_duplicate");
    }

    @Test
    @DisplayName("save yeni: doğum tarihi ve notlar kaydedilir")
    void save_new_withAllFields_savesCorrectly() {
        LocalDate dob = LocalDate.of(1990, 3, 15);
        StudentDto dto = new StudentDto(null, "Zeynep", "Kaya", "0532",
                "zeynep@test.com", dob, "Bel fıtığı var", true);
        when(repository.existsByPhoneAndActiveTrue("0532")).thenReturn(false);
        Student saved = makeStudent(5L, "Zeynep", "Kaya", "0532");
        saved.setBirthDate(dob);
        saved.setNotes("Bel fıtığı var");
        when(repository.save(any())).thenReturn(saved);

        StudentDto result = service.save(dto);

        ArgumentCaptor<Student> captor = ArgumentCaptor.forClass(Student.class);
        verify(repository).save(captor.capture());
        Student persisted = captor.getValue();
        assertThat(persisted.getBirthDate()).isEqualTo(dob);
        assertThat(persisted.getNotes()).isEqualTo("Bel fıtığı var");
        assertThat(result.id()).isEqualTo(5L);
    }

    // ── save (güncelleme) ────────────────────────────────────────────────────

    @Test
    @DisplayName("save güncelleme: mevcut öğrenci güncellenir")
    void save_update_existingStudent_updated() {
        Student existing = makeStudent(7L, "Eski", "Ad", "0500");
        when(repository.findById(7L)).thenReturn(Optional.of(existing));
        when(repository.existsByPhoneAndActiveTrueAndIdNot("0555", 7L)).thenReturn(false);
        when(repository.save(any())).thenReturn(existing);

        StudentDto dto = new StudentDto(7L, "Yeni", "Ad", "0555", null, null, null, true);
        service.save(dto);

        verify(repository).save(existing);
        assertThat(existing.getFirstName()).isEqualTo("Yeni");
        assertThat(existing.getPhone()).isEqualTo("0555");
    }

    @Test
    @DisplayName("save güncelleme: başka öğrencinin telefonu girilirse hata")
    void save_update_phoneConflict_throws() {
        Student existing = makeStudent(7L, "Ayşe", "Yılmaz", "0500");
        when(repository.findById(7L)).thenReturn(Optional.of(existing));
        when(repository.existsByPhoneAndActiveTrueAndIdNot("0555", 7L)).thenReturn(true);

        StudentDto dto = new StudentDto(7L, "Ayşe", "Yılmaz", "0555", null, null, null, true);
        assertThatThrownBy(() -> service.save(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessage("student.error.phone_duplicate");
    }

    // ── deactivate ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("deactivate: öğrenci pasife alınır")
    void deactivate_setsActiveFalse() {
        Student s = makeStudent(3L, "Fatma", "Demir", "0533");
        when(repository.findById(3L)).thenReturn(Optional.of(s));
        when(repository.save(any())).thenReturn(s);

        service.deactivate(3L);

        assertThat(s.isActive()).isFalse();
        verify(repository).save(s);
    }

    @Test
    @DisplayName("deactivate: bulunamayan id için hiçbir şey yapılmaz")
    void deactivate_notFound_noOp() {
        when(repository.findById(anyLong())).thenReturn(Optional.empty());
        service.deactivate(99L);
        verify(repository, never()).save(any());
    }

    // ── Yardımcılar ──────────────────────────────────────────────────────────

    private static Student makeStudent(Long id, String first, String last, String phone) {
        Student s = new Student(first, last);
        s.setPhone(phone);
        if (id != null) {
            ReflectionTestUtils.setField(s, "id", id);
        }
        return s;
    }
}
