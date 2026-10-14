package com.paze.pilates.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.paze.pilates.backend.TestSecurity;
import com.paze.pilates.backend.domain.Studio;
import com.paze.pilates.backend.mapper.RoomMapper;
import com.paze.pilates.backend.mapper.StudioMapper;
import com.paze.pilates.backend.repository.RoomRepository;
import com.paze.pilates.backend.repository.StudioRepository;
import com.paze.pilates.backend.security.PermissionGuard;
import com.paze.pilates.dto.StudioDto;
import com.paze.pilates.dto.StudioSettingsDto;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StudioServiceImplTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 1, 2, 3};

    @Mock
    private StudioRepository studioRepository;
    @Mock
    private RoomRepository roomRepository;

    private StudioServiceImpl service(PermissionGuard guard) {
        return new StudioServiceImpl(studioRepository, roomRepository, guard,
                Mappers.getMapper(StudioMapper.class), Mappers.getMapper(RoomMapper.class));
    }

    private StudioServiceImpl adminService() {
        return service(TestSecurity.adminGuard());
    }

    @Test
    void getSettings_returnsNameAndLogo() {
        Studio studio = new Studio("Paze");
        studio.setLogo(PNG);
        when(studioRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(studio));

        StudioSettingsDto settings = adminService().getSettings();

        assertThat(settings.name()).isEqualTo("Paze");
        assertThat(settings.logo()).isEqualTo(PNG);
    }

    @Test
    void updateSettings_updatesExistingStudioNameAndLogo() {
        Studio studio = new Studio("Eski");
        when(studioRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(studio));

        adminService().updateSettings(new StudioSettingsDto("  Paze Pilates ", PNG));

        verify(studioRepository).save(studio);
        assertThat(studio.getName()).isEqualTo("Paze Pilates");
        assertThat(studio.getLogo()).isEqualTo(PNG);
    }

    @Test
    void updateSettings_nullLogoRemovesExistingLogo() {
        Studio studio = new Studio("Paze");
        studio.setLogo(PNG);
        when(studioRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(studio));

        adminService().updateSettings(new StudioSettingsDto("Paze", null));

        assertThat(studio.getLogo()).isNull();
    }

    @Test
    void updateSettings_validatesNameAndLogo() {
        StudioServiceImpl service = adminService();
        byte[] tooLarge = new byte[StudioDto.LOGO_MAX_BYTES + 1];
        System.arraycopy(PNG, 0, tooLarge, 0, PNG.length);

        assertThatThrownBy(() -> service.updateSettings(new StudioSettingsDto(" ", null)))
                .hasMessage("studio.error.name_required");
        assertThatThrownBy(() -> service.updateSettings(new StudioSettingsDto("x".repeat(101), null)))
                .hasMessage("studio.error.name_too_long");
        assertThatThrownBy(() -> service.updateSettings(new StudioSettingsDto("Paze", new byte[] {'G', 'I', 'F'})))
                .hasMessage("studio.error.logo_invalid_format");
        assertThatThrownBy(() -> service.updateSettings(new StudioSettingsDto("Paze", tooLarge)))
                .hasMessage("studio.error.logo_too_large");
        verify(studioRepository, never()).save(any());
    }

    @Test
    void updateSettings_neverCreatesSecondStudio() {
        when(studioRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService().updateSettings(new StudioSettingsDto("Paze", null)))
                .hasMessage("studio.error.not_found");
        verify(studioRepository, never()).save(any());
    }

    @Test
    void settings_requireStudioManagePermission() {
        StudioServiceImpl instructorService = service(TestSecurity.guardFor(TestSecurity.instructor(2L, 3L)));

        assertThatThrownBy(instructorService::getSettings).hasMessage("auth.error.forbidden");
        assertThatThrownBy(() -> instructorService.updateSettings(new StudioSettingsDto("Paze", null)))
                .hasMessage("auth.error.forbidden");
    }

    @Test
    void getLogo_availableToAnyLoggedInUser() {
        Studio studio = new Studio("Paze");
        studio.setLogo(PNG);
        when(studioRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(studio));

        assertThat(service(TestSecurity.guardFor(TestSecurity.instructor(2L, 3L))).getLogo()).contains(PNG);
    }
}
