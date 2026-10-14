package com.paze.pilates.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.paze.pilates.backend.TestSecurity;
import com.paze.pilates.backend.domain.AppUser;
import com.paze.pilates.backend.domain.Studio;
import com.paze.pilates.backend.repository.AppUserRepository;
import com.paze.pilates.backend.repository.StudioRepository;
import com.paze.pilates.backend.security.StaticAdminProperties;
import com.paze.pilates.dto.BusinessException;
import com.paze.pilates.dto.SetupRequestDto;
import com.paze.pilates.dto.UserRole;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class SetupServiceImplTest {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);

    @Mock
    private AppUserRepository userRepository;
    @Mock
    private StudioRepository studioRepository;

    private SetupServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SetupServiceImpl(userRepository, studioRepository, encoder,
                new StaticAdminProperties("sistem", ""), TestSecurity.defaultPasswordPolicy());
    }

    private static SetupRequestDto request(String password, String confirm) {
        return new SetupRequestDto("Paze Pilates", "Ayşe Yılmaz", "ayse", password, confirm);
    }

    @Test
    void isSetupRequired_trueOnlyWhenNoUsers() {
        when(userRepository.count()).thenReturn(0L, 2L);

        assertThat(service.isSetupRequired()).isTrue();
        assertThat(service.isSetupRequired()).isFalse();
    }

    @Test
    void completeSetup_createsStudioAndHashedAdmin() {
        when(userRepository.count()).thenReturn(0L);
        when(studioRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());

        service.completeSetup(request("sifre1234", "sifre1234"));

        ArgumentCaptor<Studio> studio = ArgumentCaptor.forClass(Studio.class);
        verify(studioRepository).save(studio.capture());
        assertThat(studio.getValue().getName()).isEqualTo("Paze Pilates");
        ArgumentCaptor<AppUser> user = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(user.capture());
        assertThat(user.getValue().getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(user.getValue().getUsername()).isEqualTo("ayse");
        assertThat(user.getValue().isMustChangePassword()).isFalse();
        assertThat(user.getValue().getPasswordHash()).isNotEqualTo("sifre1234");
        assertThat(encoder.matches("sifre1234", user.getValue().getPasswordHash())).isTrue();
    }

    @Test
    void completeSetup_rejectedWhenUsersExist() {
        when(userRepository.count()).thenReturn(1L);

        assertThatThrownBy(() -> service.completeSetup(request("sifre1234", "sifre1234")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("setup.error.already_done");
        verify(userRepository, never()).save(any());
        verify(studioRepository, never()).save(any());
    }

    @Test
    void completeSetup_validatesPassword() {
        when(userRepository.count()).thenReturn(0L);

        assertThatThrownBy(() -> service.completeSetup(request("kisa", "kisa")))
                .hasMessage("password.error.too_short");
        assertThatThrownBy(() -> service.completeSetup(request("sifre1234", "sifre12345")))
                .hasMessage("password.error.mismatch");
        verify(userRepository, never()).save(any());
    }

    @Test
    void completeSetup_validatesRequiredFieldsAndReservedUsername() {
        when(userRepository.count()).thenReturn(0L);

        assertThatThrownBy(() -> service.completeSetup(
                new SetupRequestDto(" ", "Ayşe Yılmaz", "ayse", "sifre1234", "sifre1234")))
                .hasMessage("setup.error.studio_name_required");
        assertThatThrownBy(() -> service.completeSetup(
                new SetupRequestDto("Paze", "", "ayse", "sifre1234", "sifre1234")))
                .hasMessage("setup.error.full_name_required");
        assertThatThrownBy(() -> service.completeSetup(
                new SetupRequestDto("Paze", "Ayşe", "sistem", "sifre1234", "sifre1234")))
                .hasMessage("user.error.username_reserved");
    }

    @Test
    void isStaticAdminAvailable_requiresHash() {
        assertThat(service.isStaticAdminAvailable()).isFalse();
        SetupServiceImpl enabled = new SetupServiceImpl(userRepository, studioRepository, encoder,
                new StaticAdminProperties("sistem", encoder.encode("x")), TestSecurity.defaultPasswordPolicy());
        assertThat(enabled.isStaticAdminAvailable()).isTrue();
    }
}
