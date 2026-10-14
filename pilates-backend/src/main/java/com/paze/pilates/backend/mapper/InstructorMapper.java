package com.paze.pilates.backend.mapper;

import com.paze.pilates.backend.domain.Instructor;
import com.paze.pilates.dto.InstructorDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InstructorMapper {

    InstructorDto toDto(Instructor instructor);
}
