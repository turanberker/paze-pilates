package com.paze.pilates.desktop.ui.main;

import com.paze.pilates.dto.AppUserDto;
import com.paze.pilates.dto.Permission;
import java.util.List;

/**
 * Menü ve panel hedefleri ile görünmesi için gereken yetkiler (herhangi biri yeterli).
 * Bu yalnızca sunumdur; asıl kontrol servis katmanındadır.
 */
public enum NavTarget {
    DASHBOARD("dashboard", Permission.STUDIO_MANAGE),
    STUDIO("studio", Permission.STUDIO_MANAGE),
    INSTRUCTOR("instructor", Permission.INSTRUCTOR_MANAGE),
    USERS("users", Permission.USER_MANAGE),
    STUDENT("student", Permission.STUDENT_MANAGE),
    STUDENT_PACKAGE("student_package", Permission.PAYMENT_MANAGE),
    CALENDAR("calendar", Permission.LESSON_VIEW_ALL),
    PACKAGE("package", Permission.PACKAGE_MANAGE),
    FINANCE("finance", Permission.PAYMENT_MANAGE),
    REPORT("report", Permission.REPORT_VIEW);

    private final String key;
    private final List<Permission> anyOf;

    NavTarget(String key, Permission... anyOf) {
        this.key = key;
        this.anyOf = List.of(anyOf);
    }

    public String key() {
        return key;
    }

    public boolean isAllowedFor(AppUserDto user) {
        return anyOf.stream().anyMatch(user::has);
    }
}
