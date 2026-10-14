package com.paze.pilates.backend.mapper;

import com.paze.pilates.backend.domain.SessionPackage;
import com.paze.pilates.dto.SessionPackageDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SessionPackageMapper {

    SessionPackageDto toDto(SessionPackage sessionPackage);
}
