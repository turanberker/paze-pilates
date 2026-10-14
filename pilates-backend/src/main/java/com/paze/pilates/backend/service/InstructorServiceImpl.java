package com.paze.pilates.backend.service;

import com.paze.pilates.backend.domain.Instructor;
import com.paze.pilates.backend.mapper.InstructorMapper;
import com.paze.pilates.backend.repository.AppUserRepository;
import com.paze.pilates.backend.repository.InstructorRepository;
import com.paze.pilates.backend.security.PermissionGuard;
import com.paze.pilates.dto.BusinessException;
import com.paze.pilates.dto.InstructorDto;
import com.paze.pilates.dto.Permission;
import com.paze.pilates.service.InstructorService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InstructorServiceImpl implements InstructorService {

    private static final int NAME_MAX_LENGTH = 50;

    private final InstructorRepository instructorRepository;
    private final AppUserRepository userRepository;
    private final PermissionGuard guard;
    private final InstructorMapper instructorMapper;

    @Override
    @Transactional(readOnly = true)
    public List<InstructorDto> getInstructors() {
        guard.require(Permission.INSTRUCTOR_MANAGE);
        return instructorRepository.findByActiveTrueOrderByFirstNameAscLastNameAsc()
                .stream()
                .map(instructorMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public InstructorDto saveInstructor(InstructorDto dto) {
        guard.require(Permission.INSTRUCTOR_MANAGE);
        String firstName = normalize(dto.firstName(), "instructor.error.first_name_required",
                "instructor.error.first_name_too_long");
        String lastName = normalize(dto.lastName(), "instructor.error.last_name_required",
                "instructor.error.last_name_too_long");

        Instructor instructor;
        if (dto.id() == null) {
            instructor = new Instructor(firstName, lastName);
        } else {
            instructor = instructorRepository.findById(dto.id())
                    .orElseThrow(() -> new BusinessException("instructor.error.not_found"));
            instructor.setFirstName(firstName);
            instructor.setLastName(lastName);
        }
        return instructorMapper.toDto(instructorRepository.save(instructor));
    }

    /** Hoca pasife alınır; derslerde kullanılan kayıtlar bozulmaz, bağlı kullanıcısı da pasife alınır. */
    @Override
    @Transactional
    public void deleteInstructor(Long id) {
        guard.require(Permission.INSTRUCTOR_MANAGE);
        instructorRepository.findById(id).ifPresent(instructor -> {
            instructor.setActive(false);
            instructorRepository.save(instructor);
            userRepository.findByInstructorId(id).ifPresent(user -> {
                user.setActive(false);
                userRepository.save(user);
            });
        });
    }

    private static String normalize(String value, String requiredKey, String tooLongKey) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(requiredKey);
        }
        String trimmed = value.trim();
        if (trimmed.length() > NAME_MAX_LENGTH) {
            throw new BusinessException(tooLongKey);
        }
        return trimmed;
    }
}
