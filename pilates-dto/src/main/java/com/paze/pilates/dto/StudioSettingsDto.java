package com.paze.pilates.dto;

/**
 * Stüdyo ayarları ekranı: stüdyo adı ve header logosu.
 *
 * @param logo PNG veya JPEG dosya içeriği; null ise logo yok (kaydederken mevcut logo kaldırılır)
 */
public record StudioSettingsDto(String name, byte[] logo) {
}
