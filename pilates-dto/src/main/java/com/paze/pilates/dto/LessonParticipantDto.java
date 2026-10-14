package com.paze.pilates.dto;

/**
 * Derse eklenmiş öğrenci.
 *
 * @param id          katılım kaydı kimliği
 * @param lessonId    ders kimliği
 * @param studentId   öğrenci kimliği
 * @param studentName öğrenci ad soyad
 * @param phone       öğrenci telefonu
 * @param attended    yoklama: geldi mi (ders tamamlanmadıysa null)
 * @param charged     seansı düşüldü mü
 * @param packageName seansın düşüldüğü paket; düşüldüğü halde null ise borç olarak kayıtlıdır
 */
public record LessonParticipantDto(Long id, Long lessonId, Long studentId, String studentName, String phone,
                                   Boolean attended, boolean charged, String packageName) {

    /** Seansı düşüldü ama paketi olmadığı için borç olarak kayıtlı. */
    public boolean inDebt() {
        return charged && packageName == null;
    }
}
