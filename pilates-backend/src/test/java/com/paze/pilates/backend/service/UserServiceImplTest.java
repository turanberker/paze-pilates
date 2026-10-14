package com.paze.pilates.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.paze.pilates.backend.TestSecurity;
import com.paze.pilates.backend.domain.AppUser;
import com.paze.pilates.backend.domain.Instructor;
import com.paze.pilates.backend.repository.AppUserRepository;
import com.paze.pilates.backend.repository.InstructorRepository;
import com.paze.pilates.backend.security.StaticAdminProperties;
import com.paze.pilates.dto.BusinessException;
import com.paze.pilates.dto.UserDto;
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
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final StaticAdminProperties staticAdmin = new StaticAdminProperties("sistem", "");

    @Mock
    private AppUserRepository userRepository;
    @Mock
    private InstructorRepository instructorRepository;

    private UserServiceImpl service;
    private Instructor instructor;

    @BeforeEach
    void setUp() {
        service = new UserServiceImpl(userRepository, instructorRepository, encoder,
                TestSecurity.adminGuard(), staticAdmin);
        instructor = new Instructor("Ayşe", "Yılmaz");
        ReflectionTestUtils.setField(instructor, "id", 7L);
    }

    private AppUser stored(long id, String username, UserRole role, boolean active) {
        AppUser user = new AppUser(username, encoder.encode("sifre1234"), "Ad Soyad", role);
        ReflectionTestUtils.setField(user, "id", id);
        user.setActive(active);
        return user;
    }

    @Test
    void createUser_instructor_linksInstructorAndForcesPasswordChange() {
        when(userRepository.existsByUsername("ayse")).thenReturn(false);
        when(instructorRepository.findById(7L)).thenReturn(Optional.of(instructor));
        when(userRepository.existsByInstructorId(7L)).thenReturn(false);
        when(userRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDto created = service.createUser(" ayse ", null, UserRole.INSTRUCTOR, 7L, "gecici1234");

        ArgumentCaptor<AppUser> saved = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getUsername()).isEqualTo("ayse");
        assertThat(saved.getValue().getFullName()).isEqualTo("Ayşe Yılmaz");
        assertThat(saved.getValue().isMustChangePassword()).isTrue();
        assertThat(encoder.matches("gecici1234", saved.getValue().getPasswordHash())).isTrue();
        assertThat(created.instructorId()).isEqualTo(7L);
        assertThat(created.role()).isEqualTo(UserRole.INSTRUCTOR);
    }

    @Test
    void createUser_instructorRoleRequiresFreeActiveInstructor() {
        when(userRepository.existsByUsername("ayse")).thenReturn(false);

        assertThatThrownBy(() -> service.createUser("ayse", null, UserRole.INSTRUCTOR, null, "gecici1234"))
                .hasMessage("user.error.instructor_required");

        when(instructorRepository.findById(7L)).thenReturn(Optional.of(instructor));
        when(userRepository.existsByInstructorId(7L)).thenReturn(true);
        assertThatThrownBy(() -> service.createUser("ayse", null, UserRole.INSTRUCTOR, 7L, "gecici1234"))
                .hasMessage("user.error.instructor_already_linked");

        instructor.setActive(false);
        assertThatThrownBy(() -> service.createUser("ayse", null, UserRole.INSTRUCTOR, 7L, "gecici1234"))
                .hasMessage("user.error.instructor_not_active");
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_admin_needsFullNameAndNoInstructor() {
        when(userRepository.existsByUsername("yonetici")).thenReturn(false);
        when(userRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> service.createUser("yonetici", " ", UserRole.ADMIN, null, "gecici1234"))
                .hasMessage("user.error.full_name_required");
        assertThatThrownBy(() -> service.createUser("yonetici", "Veli", UserRole.ADMIN, 7L, "gecici1234"))
                .hasMessage("user.error.instructor_only_for_instructor_role");

        UserDto created = service.createUser("yonetici", " Veli Can ", UserRole.ADMIN, null, "gecici1234");
        assertThat(created.fullName()).isEqualTo("Veli Can");
        assertThat(created.instructorId()).isNull();
    }

    @Test
    void createUser_rejectsDuplicateReservedAndWeakPassword() {
        when(userRepository.existsByUsername("ayse")).thenReturn(true);

        assertThatThrownBy(() -> service.createUser("ayse", "Ayşe", UserRole.ADMIN, null, "gecici1234"))
                .hasMessage("user.error.username_taken");
        assertThatThrownBy(() -> service.createUser("sistem", "Ayşe", UserRole.ADMIN, null, "gecici1234"))
                .hasMessage("user.error.username_reserved");
        assertThatThrownBy(() -> service.createUser("a b", "Ayşe", UserRole.ADMIN, null, "gecici1234"))
                .hasMessage("user.error.username_invalid");
        when(userRepository.existsByUsername("yeni")).thenReturn(false);
        assertThatThrownBy(() -> service.createUser("yeni", "Ayşe", UserRole.ADMIN, null, "kisa"))
                .hasMessage("password.error.too_short");
    }

    @Test
    void resetPassword_setsNewHashAndForcesChange() {
        AppUser user = stored(5L, "hoca", UserRole.INSTRUCTOR, true);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        service.resetPassword(5L, "yeniGecici1");

        assertThat(user.isMustChangePassword()).isTrue();
        assertThat(encoder.matches("yeniGecici1", user.getPasswordHash())).isTrue();
    }

    @Test
    void deactivate_lastActiveAdminIsRejected() {
        AppUser lastAdmin = stored(5L, "ayse", UserRole.ADMIN, true);
        when(userRepository.findById(5L)).thenReturn(Optional.of(lastAdmin));
        when(userRepository.countByRoleAndActiveTrue(UserRole.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> service.deactivate(5L)).hasMessage("user.error.last_admin");
        assertThat(lastAdmin.isActive()).isTrue();
        verify(userRepository, never()).save(any());
    }

    @Test
    void deactivate_secondAdminAllowed_ownAccountRejected() {
        AppUser other = stored(5L, "veli", UserRole.ADMIN, true);
        when(userRepository.findById(5L)).thenReturn(Optional.of(other));
        when(userRepository.countByRoleAndActiveTrue(UserRole.ADMIN)).thenReturn(2L);
        when(userRepository.save(other)).thenReturn(other);

        assertThat(service.deactivate(5L).active()).isFalse();

        AppUser self = stored(1L, "admin", UserRole.ADMIN, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(self));
        assertThatThrownBy(() -> service.deactivate(1L)).hasMessage("user.error.self_deactivate");
    }

    @Test
    void deactivate_instructorUserNeverCountsAsAdmin() {
        AppUser hoca = stored(6L, "hoca", UserRole.INSTRUCTOR, true);
        when(userRepository.findById(6L)).thenReturn(Optional.of(hoca));
        when(userRepository.save(hoca)).thenReturn(hoca);

        assertThat(service.deactivate(6L).active()).isFalse();
        verify(userRepository, never()).countByRoleAndActiveTrue(any());
    }

    @Test
    void activate_instructorWithInactiveInstructorRejected() {
        AppUser hoca = stored(6L, "hoca", UserRole.INSTRUCTOR, false);
        instructor.setActive(false);
        hoca.setInstructor(instructor);
        when(userRepository.findById(6L)).thenReturn(Optional.of(hoca));

        assertThatThrownBy(() -> service.activate(6L)).hasMessage("user.error.instructor_not_active");
    }

    @Test
    void changeRole_lastAdminCannotBeDemoted() {
        AppUser lastAdmin = stored(5L, "ayse", UserRole.ADMIN, true);
        when(userRepository.findById(5L)).thenReturn(Optional.of(lastAdmin));
        when(instructorRepository.findById(7L)).thenReturn(Optional.of(instructor));
        when(userRepository.existsByInstructorIdAndIdNot(7L, 5L)).thenReturn(false);
        when(userRepository.countByRoleAndActiveTrue(UserRole.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> service.changeRole(5L, UserRole.INSTRUCTOR, 7L))
                .hasMessage("user.error.last_admin");
        verify(userRepository, never()).save(any());
    }

    @Test
    void changeRole_instructorToAdminClearsLink_adminToInstructorLinks() {
        AppUser hoca = stored(6L, "hoca", UserRole.INSTRUCTOR, true);
        hoca.setInstructor(instructor);
        when(userRepository.findById(6L)).thenReturn(Optional.of(hoca));
        when(userRepository.save(hoca)).thenReturn(hoca);

        UserDto promoted = service.changeRole(6L, UserRole.ADMIN, null);
        assertThat(promoted.role()).isEqualTo(UserRole.ADMIN);
        assertThat(hoca.getInstructor()).isNull();

        when(instructorRepository.findById(7L)).thenReturn(Optional.of(instructor));
        when(userRepository.existsByInstructorIdAndIdNot(7L, 6L)).thenReturn(false);
        when(userRepository.countByRoleAndActiveTrue(UserRole.ADMIN)).thenReturn(2L);
        UserDto demoted = service.changeRole(6L, UserRole.INSTRUCTOR, 7L);
        assertThat(demoted.role()).isEqualTo(UserRole.INSTRUCTOR);
        assertThat(demoted.instructorId()).isEqualTo(7L);
    }

    @Test
    void allMethods_forbiddenForInstructorRole() {
        UserServiceImpl restricted = new UserServiceImpl(userRepository, instructorRepository, encoder,
                TestSecurity.guardFor(TestSecurity.instructor(2L, 7L)), staticAdmin);

        assertThatThrownBy(restricted::findAll).hasMessage("auth.error.forbidden");
        assertThatThrownBy(() -> restricted.createUser("x", "y", UserRole.ADMIN, null, "gecici1234"))
                .hasMessage("auth.error.forbidden");
        assertThatThrownBy(() -> restricted.resetPassword(1L, "gecici1234")).hasMessage("auth.error.forbidden");
        assertThatThrownBy(() -> restricted.deactivate(1L)).hasMessage("auth.error.forbidden");
        assertThatThrownBy(() -> restricted.changeRole(1L, UserRole.ADMIN, null))
                .isInstanceOf(BusinessException.class).hasMessage("auth.error.forbidden");
    }
}
