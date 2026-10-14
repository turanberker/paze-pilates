package com.paze.pilates.backend.mapper;

import com.paze.pilates.backend.domain.Student;
import com.paze.pilates.backend.domain.StudentPackage;
import com.paze.pilates.dto.StudentPackageDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StudentPackageMapper {

    @Mapping(target = "studentId", source = "student.id")
    @Mapping(target = "studentName", source = "student")
    @Mapping(target = "packageId", source = "sessionPackage.id")
    @Mapping(target = "packageName", source = "sessionPackage.name")
    StudentPackageDto toDto(StudentPackage studentPackage);

    default String fullName(Student student) {
        return student == null ? null : student.getFirstName() + " " + student.getLastName();
    }
}
