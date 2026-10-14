package com.paze.pilates.service;

import com.paze.pilates.dto.InstructorDto;
import java.util.List;

public interface InstructorService {

    List<InstructorDto> getInstructors();

    InstructorDto saveInstructor(InstructorDto dto);

    void deleteInstructor(Long id);
}
