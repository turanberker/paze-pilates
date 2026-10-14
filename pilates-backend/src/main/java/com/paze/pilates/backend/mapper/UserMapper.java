package com.paze.pilates.backend.mapper;

import com.paze.pilates.backend.domain.AppUser;
import com.paze.pilates.backend.domain.Instructor;
import com.paze.pilates.dto.UserDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "instructorId", source = "instructor.id")
    @Mapping(target = "instructorName", source = "instructor")
    UserDto toDto(AppUser user);

    default String fullName(Instructor instructor) {
        return instructor == null ? null : instructor.getFirstName() + " " + instructor.getLastName();
    }
}
