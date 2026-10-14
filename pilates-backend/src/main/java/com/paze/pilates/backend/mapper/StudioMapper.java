package com.paze.pilates.backend.mapper;

import com.paze.pilates.backend.domain.Studio;
import com.paze.pilates.dto.StudioDto;
import com.paze.pilates.dto.StudioSettingsDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StudioMapper {

    StudioDto toDto(Studio studio);

    StudioSettingsDto toSettingsDto(Studio studio);
}
