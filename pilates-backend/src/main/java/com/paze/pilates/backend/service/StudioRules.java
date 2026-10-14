package com.paze.pilates.backend.service;

import com.paze.pilates.dto.BusinessException;
import com.paze.pilates.dto.StudioDto;
import java.util.Arrays;

/** Stüdyo adı ve logosu için ortak kurallar; kurulum ve stüdyo ayarları aynı kuralı kullanır. */
final class StudioRules {

    static final int NAME_MAX_LENGTH = 100;

    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
    private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

    private StudioRules() {
    }

    /** Stüdyo adını kırpıp doğrular. */
    static String name(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException("studio.error.name_required");
        }
        String trimmed = value.trim();
        if (trimmed.length() > NAME_MAX_LENGTH) {
            throw new BusinessException("studio.error.name_too_long", NAME_MAX_LENGTH);
        }
        return trimmed;
    }

    /** Logo isteğe bağlıdır (boşsa null döner); verildiyse boyutu sınırlıdır ve PNG ya da JPEG olmalıdır. */
    static byte[] logo(byte[] logo) {
        if (logo == null || logo.length == 0) {
            return null;
        }
        if (logo.length > StudioDto.LOGO_MAX_BYTES) {
            throw new BusinessException("studio.error.logo_too_large", StudioDto.LOGO_MAX_BYTES / 1024);
        }
        if (!startsWith(logo, PNG_SIGNATURE) && !startsWith(logo, JPEG_SIGNATURE)) {
            throw new BusinessException("studio.error.logo_invalid_format");
        }
        return logo;
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {
        return data.length >= prefix.length
                && Arrays.equals(data, 0, prefix.length, prefix, 0, prefix.length);
    }
}
