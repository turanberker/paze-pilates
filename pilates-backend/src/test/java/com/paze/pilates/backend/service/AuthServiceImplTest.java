package com.paze.pilates.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.paze.pilates.backend.TestSecurity;
import com.paze.pilates.backend.domain.AppUser;
import com.paze.pilates.backend.repository.AppUserRepository;
import com.paze.pilates.backend.security.FixedRolePermissionResolver;
import com.paze.pilates.backend.security.SessionContext;
import com.paze.pilates.backend.security.StaticAdminProperties;
import com.paze.pilates.dto.AppUserDto;
import com.paze.pilates.dto.BusinessException;
import com.paze.pilates.dto.Permission;
import com.paze.pilates.dto.UserRole;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final SessionContext session = new SessionContext();

    @Mock
    private AppUserRepository userRepository;

    private AuthServiceImpl service;

    @BeforeEach
    void setUp() {
        service = newService(new StaticAdminProperties("sistem", encoder.encode("sabitSifre1")));
    }

    private AuthServiceImpl newService(StaticAdminProperties staticAdmin) {
        return new AuthServiceImpl(userRepository, encoder, session, new FixedRolePermissionResolver(), staticAdmin,
                TestSecurity.defaultPasswordPolicy());
    }

    private AppUser user(String username, String password, UserRole role, boolean active) {
        AppUser user = new AppUser(username, encoder.encode(password), "Ad Soyad", role);
        user.setActive(active);
        return user;
    }

    @Test
    void authenticate_staticAdminNeverTouchesDatabase() {
        assertThat(service.authenticate("sistem", "sabitSifre1")).isTrue();

        AppUserDto current = service.getCurrentUser().orElseThrow();
        assertThat(current.staticAdmin()).isTrue();
        assertThat(current.id()).isNull();
        assertThat(current.role()).isEqualTo(UserRole.ADMIN);
        assertThat(current.permissions()).containsExactlyInAnyOrder(Permission.values());
        verify(userRepository, never()).findByUsername("sistem");
    }

    @Test
    void authenticate_staticAdminWrongPasswordFails() {
        assertThat(service.authenticate("sistem", "yanlis")).isFalse();
        assertThat(service.getCurrentUser()).isEmpty();
    }

    @Test
    void authenticate_staticAdminDisabledWhenHashBlank() {
        AuthServiceImpl disabled = newService(new StaticAdminProperties("sistem", ""));

        assertThat(disabled.authenticate("sistem", "")).isFalse();
        assertThat(disabled.authenticate("sistem", "sabitSifre1")).isFalse();
        assertThat(disabled.getCurrentUser()).isEmpty();
    }

    @Test
    void authenticate_databaseUserOk_inactiveAndWrongPasswordFail() {
        when(userRepository.findByUsername("ayse")).thenReturn(Optional.of(user("ayse", "sifre1234", UserRole.ADMIN, true)));
        when(userRepository.findByUsername("pasif")).thenReturn(Optional.of(user("pasif", "sifre1234", UserRole.ADMIN, false)));

        assertThat(service.authenticate("ayse", "yanlis")).isFalse();
        assertThat(service.authenticate("pasif", "sifre1234")).isFalse();
        assertThat(service.authenticate("ayse", "sifre1234")).isTrue();
        assertThat(service.getCurrentUser().orElseThrow().username()).isEqualTo("ayse");
    }

    @Test
    void authenticate_instructorGetsLessonViewAndStudentPermissions() {
        when(userRepository.findByUsername("hoca")).thenReturn(Optional.of(user("hoca", "sifre1234", UserRole.INSTRUCTOR, true)));

        assertThat(service.authenticate("hoca", "sifre1234")).isTrue();
        assertThat(service.getCurrentUser().orElseThrow().permissions())
                .containsExactlyInAnyOrder(Permission.LESSON_VIEW_ALL, Permission.LESSON_MANAGE_OWN,
                        Permission.STUDENT_MANAGE);
    }

    @Test
    void authenticate_temporaryPasswordUserMustChange() {
        AppUser temp = user("hoca", "gecici1234", UserRole.INSTRUCTOR, true);
        temp.setMustChangePassword(true);
        when(userRepository.findByUsername("hoca")).thenReturn(Optional.of(temp));

        service.authenticate("hoca", "gecici1234");

        assertThat(service.getCurrentUser().orElseThrow().mustChangePassword()).isTrue();
    }

    @Test
    void changeOwnPassword_clearsMustChangeFlag() {
        AppUser temp = user("hoca", "gecici1234", UserRole.INSTRUCTOR, true);
        temp.setMustChangePassword(true);
        when(userRepository.findByUsername("hoca")).thenReturn(Optional.of(temp));
        when(userRepository.findById(temp.getId())).thenReturn(Optional.of(temp));
        service.authenticate("hoca", "gecici1234");

        service.changeOwnPassword("gecici1234", "yeniSifre99", "yeniSifre99");

        assertThat(temp.isMustChangePassword()).isFalse();
        assertThat(encoder.matches("yeniSifre99", temp.getPasswordHash())).isTrue();
        assertThat(service.getCurrentUser().orElseThrow().mustChangePassword()).isFalse();
    }

    @Test
    void changeOwnPassword_rejectsWrongCurrentAndWeakNew() {
        AppUser existing = user("ayse", "sifre1234", UserRole.ADMIN, true);
        when(userRepository.findByUsername("ayse")).thenReturn(Optional.of(existing));
        when(userRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        service.authenticate("ayse", "sifre1234");

        assertThatThrownBy(() -> service.changeOwnPassword("yanlis", "yeniSifre99", "yeniSifre99"))
                .hasMessage("auth.error.current_password_wrong");
        assertThatThrownBy(() -> service.changeOwnPassword("sifre1234", "kisa", "kisa"))
                .hasMessage("password.error.too_short");
        assertThatThrownBy(() -> service.changeOwnPassword("sifre1234", "sifre1234", "sifre1234"))
                .hasMessage("password.error.same_as_current");
    }

    @Test
    void changeOwnPassword_staticAdminOrAnonymousRejected() {
        assertThatThrownBy(() -> service.changeOwnPassword("a", "bbbbbbbbb", "bbbbbbbbb"))
                .isInstanceOf(BusinessException.class).hasMessage("auth.error.not_authenticated");
        service.authenticate("sistem", "sabitSifre1");
        assertThatThrownBy(() -> service.changeOwnPassword("sabitSifre1", "bbbbbbbbb", "bbbbbbbbb"))
                .hasMessage("auth.error.static_admin_password");
    }

    @Test
    void logout_clearsSession() {
        service.authenticate("sistem", "sabitSifre1");
        service.logout();
        assertThat(service.getCurrentUser()).isEmpty();
    }
}
