package com.paze.pilates.backend.mapper;

import com.paze.pilates.backend.domain.Student;
import com.paze.pilates.dto.StudentDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StudentMapper {

    /** Borçlu seans sayısı entity'de tutulmaz; servis ayrıca sayıp verir. */
    StudentDto toDto(Student student, int sessionDebt);
}
