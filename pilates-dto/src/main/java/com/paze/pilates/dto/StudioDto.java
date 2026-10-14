package com.paze.pilates.dto;

public record StudioDto(Long id, String name) {

    /** Header logosu için izin verilen en büyük dosya boyutu (bayt). */
    public static final int LOGO_MAX_BYTES = 512 * 1024;
}
