package com.paze.pilates.service;

import com.paze.pilates.dto.RemainingSessionFilter;
import com.paze.pilates.dto.StudentPackageDto;
import java.time.LocalDate;
import java.util.List;

public interface StudentPackageService {

    /**
     * Öğrenci adına ve kalan seans durumuna göre paket atamalarını yeniden eskiye döner.
     *
     * @param studentName ad/soyad arama metni (boş veya null → hepsi)
     * @param filter      kalan seans filtresi (null → hepsi)
     */
    List<StudentPackageDto> search(String studentName, RemainingSessionFilter filter);

    /**
     * Aktif öğrenciye aktif paket atar. Atama tarihi bugündür.
     * Öğrencinin seans borcu varsa en eski dersten başlayarak yeni paketten düşülür ve borç satırları silinir.
     *
     * @param paid        ödeme alındı mı
     * @param paymentDate ödeme alındıysa tarihi (zorunlu, ileri tarih olamaz)
     * @throws com.paze.pilates.dto.BusinessException doğrulama hatasında
     */
    StudentPackageDto assign(Long studentId, Long packageId, boolean paid, LocalDate paymentDate);

    /**
     * Ödemesi alınmamış atamanın ödemesini tamamlar.
     *
     * @throws com.paze.pilates.dto.BusinessException kayıt yoksa, zaten ödenmişse veya tarih geçersizse
     */
    StudentPackageDto completePayment(Long studentPackageId, LocalDate paymentDate);

    /**
     * Ödemesi alınmamış ve hiç seansı kullanılmamış atamayı iptal eder; kayıt tamamen silinir.
     * Ödemesi alınmış atamanın iptali (kalan bakiye ve iade hesabı) sonraki aşamadadır.
     *
     * @throws com.paze.pilates.dto.BusinessException kayıt yoksa, ödemesi alınmışsa veya seansı kullanılmışsa
     */
    void cancel(Long studentPackageId);
}
