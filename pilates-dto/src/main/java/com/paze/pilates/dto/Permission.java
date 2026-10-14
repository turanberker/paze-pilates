package com.paze.pilates.dto;

/** Servis katmanında kontrol edilen yetkiler. Rol → yetki eşlemesi backend'dedir. */
public enum Permission {
    STUDIO_MANAGE,
    INSTRUCTOR_MANAGE,
    USER_MANAGE,
    STUDENT_MANAGE,
    PACKAGE_MANAGE,
    PAYMENT_MANAGE,
    LESSON_MANAGE,
    LESSON_VIEW_ALL,
    LESSON_VIEW_OWN,
    REPORT_VIEW
}
