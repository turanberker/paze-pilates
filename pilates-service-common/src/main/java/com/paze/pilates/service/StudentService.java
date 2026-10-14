package com.paze.pilates.service;

import com.paze.pilates.dto.StudentDto;
import java.util.List;

public interface StudentService {

    /** Tüm aktif öğrencileri soyadı / adı sırasıyla döner. */
    List<StudentDto> findAll();

    /**
     * Ad, soyad veya telefon üzerinden öğrenci arar.
     *
     * @param term arama terimi (boşsa tüm aktif öğrenciler döner)
     */
    List<StudentDto> search(String term);

    /**
     * Yeni öğrenci oluşturur veya mevcut olanı günceller.
     * Kaydedilen öğrencinin DTO'sunu döner.
     *
     * @throws com.paze.pilates.dto.BusinessException doğrulama hatasında
     */
    StudentDto save(StudentDto dto);

    /**
     * Öğrenciyi pasife alır (soft delete).
     * Geçmiş rezervasyon ve ödemeler korunur.
     */
    void deactivate(Long id);
}
