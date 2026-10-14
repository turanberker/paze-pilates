package com.paze.pilates.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.paze.pilates.backend.repository.AppUserRepository;
import com.paze.pilates.backend.repository.InstructorRepository;
import com.paze.pilates.dto.BusinessException;
import com.paze.pilates.dto.InstructorDto;
import com.paze.pilates.dto.SetupRequestDto;
import com.paze.pilates.dto.UserDto;
import com.paze.pilates.dto.UserRole;
import com.paze.pilates.service.AuthService;
import com.paze.pilates.service.InstructorService;
import com.paze.pilates.service.LessonService;
import com.paze.pilates.service.SetupService;
import com.paze.pilates.service.StudentService;
import com.paze.pilates.service.UserService;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** Kurulum → giriş → hoca kullanıcısı → yetki akışı, gerçek Spring bağlantılarıyla. */
@SpringBootTest(properties = {
        "paze.security.static-admin.username=sistem",
        "paze.security.static-admin.password-hash=$2a$10$PCPYryAvrQEB2zgiywJNN.L733bDubYXMa4ydcA/L5XPvoGQJlDwu"
})
class UserRolesIntegrationTest {

    @Autowired
    private SetupService setupService;
    @Autowired
    private AuthService authService;
    @Autowired
    private UserService userService;
    @Autowired
    private InstructorService instructorService;
    @Autowired
    private StudentService studentService;
    @Autowired
    private LessonService lessonService;
    @Autowired
    private AppUserRepository userRepository;
    @Autowired
    private InstructorRepository instructorRepository;

    @BeforeEach
    void cleanDatabase() {
        authService.logout();
        userRepository.deleteAll();
        instructorRepository.deleteAll();
    }

    @Test
    void setupThenInstructorUserFlow() {
        assertThat(setupService.isSetupRequired()).isTrue();
        assertThat(setupService.isStaticAdminAvailable()).isTrue();

        SetupRequestDto request = new SetupRequestDto("Paze Pilates", "Ayşe Yılmaz", "ayse", "sifre1234", "sifre1234");
        setupService.completeSetup(request);
        assertThat(setupService.isSetupRequired()).isFalse();
        assertThatThrownBy(() -> setupService.completeSetup(request))
                .isInstanceOf(BusinessException.class).hasMessage("setup.error.already_done");

        assertThat(authService.authenticate("ayse", "sifre1234")).isTrue();
        InstructorDto instructor = instructorService.saveInstructor(new InstructorDto(null, "Zeynep", "Kaya"));
        UserDto created = userService.createUser("zeynep", null, UserRole.INSTRUCTOR, instructor.id(), "gecici1234");
        assertThat(created.instructorName()).isEqualTo("Zeynep Kaya");
        assertThat(userService.findAll()).extracting(UserDto::username).containsExactly("ayse", "zeynep");
        assertThatThrownBy(() -> userService.deactivate(userService.findAll().get(0).id()))
                .hasMessage("user.error.self_deactivate");

        // Hoca geçici şifreyle girer: şifre değiştirene kadar hiçbir servis çalışmaz.
        assertThat(authService.authenticate("zeynep", "gecici1234")).isTrue();
        assertThat(authService.getCurrentUser().orElseThrow().mustChangePassword()).isTrue();
        assertThatThrownBy(() -> lessonService.findBetween(LocalDate.now(), LocalDate.now()))
                .hasMessage("auth.error.password_change_required");
        authService.changeOwnPassword("gecici1234", "yeniSifre99", "yeniSifre99");

        // Hoca yalnızca kendi derslerini görür; diğer servisler kapalı.
        assertThat(lessonService.findBetween(LocalDate.now(), LocalDate.now())).isEmpty();
        assertThat(studentService.findAll()).isEmpty();
        assertThatThrownBy(() -> userService.findAll()).hasMessage("auth.error.forbidden");
        assertThatThrownBy(() -> instructorService.getInstructors()).hasMessage("auth.error.forbidden");

        // Yeni şifreyle tekrar girebilir, eskisiyle giremez.
        assertThat(authService.authenticate("zeynep", "gecici1234")).isFalse();
        assertThat(authService.authenticate("zeynep", "yeniSifre99")).isTrue();
        assertThat(authService.getCurrentUser().orElseThrow().mustChangePassword()).isFalse();
    }

    @Test
    void lastActiveAdminIsProtectedAndInstructorDeactivationDeactivatesUser() {
        setupService.completeSetup(new SetupRequestDto("Paze", "Ayşe Yılmaz", "ayse", "sifre1234", "sifre1234"));
        authService.authenticate("ayse", "sifre1234");
        Long adminId = userService.findAll().get(0).id();
        assertThatThrownBy(() -> userService.changeRole(adminId, UserRole.INSTRUCTOR,
                instructorService.saveInstructor(new InstructorDto(null, "Zeynep", "Kaya")).id()))
                .hasMessage("user.error.last_admin");

        InstructorDto instructor = instructorService.getInstructors().get(0);
        UserDto hocaUser = userService.createUser("zeynep", null, UserRole.INSTRUCTOR, instructor.id(), "gecici1234");
        instructorService.deleteInstructor(instructor.id());
        assertThat(userService.findAll()).filteredOn(u -> u.id().equals(hocaUser.id()))
                .singleElement().satisfies(u -> assertThat(u.active()).isFalse());
        assertThatThrownBy(() -> userService.activate(hocaUser.id()))
                .hasMessage("user.error.instructor_not_active");
    }

    @Test
    void staticAdminLogsInWithoutDatabaseUser() {
        assertThat(userRepository.count()).isZero();

        assertThat(authService.authenticate("sistem", "yanlis")).isFalse();
        assertThat(authService.authenticate("sistem", "SabitSifre123")).isTrue();

        assertThat(authService.getCurrentUser().orElseThrow().staticAdmin()).isTrue();
        assertThat(studentService.findAll()).isEmpty();
        assertThat(userService.findAll()).isEmpty();
        assertThat(userRepository.count()).isZero();
    }
}
