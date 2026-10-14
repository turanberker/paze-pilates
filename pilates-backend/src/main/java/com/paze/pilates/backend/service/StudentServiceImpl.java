package com.paze.pilates.backend.service;

import com.paze.pilates.backend.domain.Student;
import com.paze.pilates.backend.repository.StudentRepository;
import com.paze.pilates.backend.security.PermissionGuard;
import com.paze.pilates.dto.Permission;
import com.paze.pilates.dto.BusinessException;
import com.paze.pilates.dto.StudentDto;
import com.paze.pilates.service.StudentService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentServiceImpl implements StudentService {

    private final PermissionGuard guard;

    private final StudentRepository studentRepository;

    public StudentServiceImpl(StudentRepository studentRepository, PermissionGuard guard) {
        this.guard = guard;
        this.studentRepository = studentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentDto> findAll() {
        guard.require(Permission.STUDENT_MANAGE);
        return studentRepository.findByActiveTrueOrderByLastNameAscFirstNameAsc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentDto> search(String term) {
        guard.require(Permission.STUDENT_MANAGE);
        if (term == null || term.isBlank()) {
            return findAll();
        }
        return studentRepository.searchActive(term.trim())
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public StudentDto save(StudentDto dto) {
        guard.require(Permission.STUDENT_MANAGE);
        validate(dto);

        if (dto.id() == null) {
            // Yeni kayıt: telefon benzersizlik kontrolü
            if (dto.phone() != null && !dto.phone().isBlank()
                    && studentRepository.existsByPhoneAndActiveTrue(dto.phone().trim())) {
                throw new BusinessException("student.error.phone_duplicate");
            }
            Student s = new Student(dto.firstName().trim(), dto.lastName().trim());
            applyFields(s, dto);
            return toDto(studentRepository.save(s));
        } else {
            // Güncelleme: kendi kaydı dışında telefon benzersizlik kontrolü
            Student s = studentRepository.findById(dto.id())
                    .orElseThrow(() -> new BusinessException("student.error.not_found"));
            if (dto.phone() != null && !dto.phone().isBlank()
                    && studentRepository.existsByPhoneAndActiveTrueAndIdNot(
                            dto.phone().trim(), dto.id())) {
                throw new BusinessException("student.error.phone_duplicate");
            }
            applyFields(s, dto);
            return toDto(studentRepository.save(s));
        }
    }

    @Override
    @Transactional
    public void deactivate(Long id) {
        guard.require(Permission.STUDENT_MANAGE);
        studentRepository.findById(id).ifPresent(s -> {
            s.setActive(false);
            studentRepository.save(s);
        });
    }

    // ── Yardımcılar ──────────────────────────────────────────────────────────

    private void validate(StudentDto dto) {
        if (dto.firstName() == null || dto.firstName().isBlank()) {
            throw new BusinessException("student.error.firstname_required");
        }
        if (dto.firstName().trim().length() > 50) {
            throw new BusinessException("student.error.firstname_too_long");
        }
        if (dto.lastName() == null || dto.lastName().isBlank()) {
            throw new BusinessException("student.error.lastname_required");
        }
        if (dto.lastName().trim().length() > 50) {
            throw new BusinessException("student.error.lastname_too_long");
        }
        if (dto.phone() == null || dto.phone().isBlank()) {
            throw new BusinessException("student.error.phone_required");
        }
        if (dto.phone().trim().length() > 20) {
            throw new BusinessException("student.error.phone_too_long");
        }
        if (dto.email() != null && dto.email().length() > 100) {
            throw new BusinessException("student.error.email_too_long");
        }
        if (dto.notes() != null && dto.notes().length() > 500) {
            throw new BusinessException("student.error.notes_too_long");
        }
    }

    private void applyFields(Student s, StudentDto dto) {
        s.setFirstName(dto.firstName().trim());
        s.setLastName(dto.lastName().trim());
        s.setPhone(dto.phone() == null ? null : dto.phone().trim());
        s.setEmail(dto.email() == null || dto.email().isBlank() ? null : dto.email().trim());
        s.setBirthDate(dto.birthDate());
        s.setNotes(dto.notes() == null || dto.notes().isBlank() ? null : dto.notes().trim());
    }

    private StudentDto toDto(Student s) {
        return new StudentDto(
                s.getId(),
                s.getFirstName(),
                s.getLastName(),
                s.getPhone(),
                s.getEmail(),
                s.getBirthDate(),
                s.getNotes(),
                s.isActive()
        );
    }
}
