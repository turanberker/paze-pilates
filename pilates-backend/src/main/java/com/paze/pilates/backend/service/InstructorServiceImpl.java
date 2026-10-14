package com.paze.pilates.backend.service;

import com.paze.pilates.backend.domain.Instructor;
import com.paze.pilates.backend.repository.InstructorRepository;
import com.paze.pilates.dto.BusinessException;
import com.paze.pilates.dto.InstructorDto;
import com.paze.pilates.service.InstructorService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InstructorServiceImpl implements InstructorService {

    private static final int NAME_MAX_LENGTH = 50;

    private final InstructorRepository instructorRepository;

    public InstructorServiceImpl(InstructorRepository instructorRepository) {
        this.instructorRepository = instructorRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstructorDto> getInstructors() {
        return instructorRepository.findByActiveTrueOrderByFirstNameAscLastNameAsc()
                .stream()
                .map(InstructorServiceImpl::toDto)
                .toList();
    }

    @Override
    @Transactional
    public InstructorDto saveInstructor(InstructorDto dto) {
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
        return toDto(instructorRepository.save(instructor));
    }

    /** Hoca pasife alınır; ileride derslerde kullanılan kayıtlar bozulmaz. */
    @Override
    @Transactional
    public void deleteInstructor(Long id) {
        instructorRepository.findById(id).ifPresent(instructor -> {
            instructor.setActive(false);
            instructorRepository.save(instructor);
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

    private static InstructorDto toDto(Instructor instructor) {
        return new InstructorDto(instructor.getId(), instructor.getFirstName(), instructor.getLastName());
    }
}
