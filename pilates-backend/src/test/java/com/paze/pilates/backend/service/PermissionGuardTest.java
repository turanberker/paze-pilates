package com.paze.pilates.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.paze.pilates.backend.TestSecurity;
import com.paze.pilates.backend.security.CurrentUser;
import com.paze.pilates.backend.security.PermissionGuard;
import com.paze.pilates.dto.BusinessException;
import com.paze.pilates.dto.Permission;
import com.paze.pilates.dto.UserRole;
import org.junit.jupiter.api.Test;

class PermissionGuardTest {

    @Test
    void anonymousIsRejected() {
        PermissionGuard guard = TestSecurity.guardFor(null);

        assertThatThrownBy(() -> guard.require(Permission.STUDENT_MANAGE))
                .isInstanceOf(BusinessException.class).hasMessage("auth.error.not_authenticated");
        assertThat(guard.has(Permission.STUDENT_MANAGE)).isFalse();
    }

    @Test
    void adminHasEveryPermission() {
        PermissionGuard guard = TestSecurity.adminGuard();

        for (Permission permission : Permission.values()) {
            assertThat(guard.has(permission)).isTrue();
            guard.require(permission);
        }
    }

    @Test
    void instructorHasOwnLessonPermissionsAndStudentManage() {
        PermissionGuard guard = TestSecurity.guardFor(TestSecurity.instructor(2L, 7L));

        guard.require(Permission.LESSON_VIEW_OWN);
        guard.require(Permission.LESSON_MANAGE_OWN);
        guard.require(Permission.STUDENT_MANAGE);
        for (Permission permission : Permission.values()) {
            if (permission != Permission.LESSON_VIEW_OWN && permission != Permission.LESSON_MANAGE_OWN
                    && permission != Permission.STUDENT_MANAGE) {
                assertThat(guard.has(permission)).as(permission.name()).isFalse();
                assertThatThrownBy(() -> guard.require(permission)).hasMessage("auth.error.forbidden");
            }
        }
    }

    @Test
    void userWithTemporaryPasswordCannotDoAnything() {
        PermissionGuard guard = TestSecurity.guardFor(
                new CurrentUser(3L, "ayse", "Ayşe", UserRole.ADMIN, null, false, true));

        assertThatThrownBy(() -> guard.require(Permission.STUDENT_MANAGE))
                .hasMessage("auth.error.password_change_required");
    }
}
