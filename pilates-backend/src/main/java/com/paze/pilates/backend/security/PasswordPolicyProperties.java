package com.paze.pilates.backend.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Şifre kuralları ({@code paze.security.password.*}); verilmeyen değerler varsayılanı alır. */
@ConfigurationProperties(prefix = "paze.security.password")
public record PasswordPolicyProperties(
        Integer minLength,
        boolean requireUppercase,
        boolean requireLowercase,
        boolean requireDigit,
        boolean requireSpecial) {

    public static final int DEFAULT_MIN_LENGTH = 8;

    public PasswordPolicyProperties {
        if (minLength == null || minLength < 1) {
            minLength = DEFAULT_MIN_LENGTH;
        }
    }
}
