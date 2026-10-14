package com.paze.pilates.backend.service;

import com.paze.pilates.backend.TestSecurity;
import com.paze.pilates.backend.domain.SessionPackage;
import com.paze.pilates.backend.domain.Student;
import com.paze.pilates.backend.domain.StudentPackage;
import com.paze.pilates.backend.domain.Instructor;
import com.paze.pilates.backend.domain.Lesson;
import com.paze.pilates.backend.domain.LessonParticipant;
import com.paze.pilates.backend.domain.Room;
import com.paze.pilates.backend.domain.SessionDebt;
import com.paze.pilates.backend.domain.StudentLessonHistory;
import com.paze.pilates.backend.repository.SessionDebtRepository;
import com.paze.pilates.backend.repository.SessionPackageRepository;
import com.paze.pilates.backend.repository.StudentPackageRepository;
import com.paze.pilates.backend.repository.StudentRepository;
import com.paze.pilates.dto.BusinessException;
import com.paze.pilates.dto.RemainingSessionFilter;
import com.paze.pilates.dto.StudentPackageDto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentPackageServiceImplTest {

    private static final LocalDate TODAY = LocalDate.now();

    @Mock
    private StudentPackageRepository repository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private SessionPackageRepository packageRepository;
    @Mock
    private SessionDebtRepository debtRepository;

    private StudentPackageServiceImpl service;

    private Student student;
    private SessionPackage pkg;

    @BeforeEach
    void setUp() {
        service = new StudentPackageServiceImpl(repository, studentRepository, packageRepository, debtRepository,
                TestSecurity.adminGuard());
        student = new Student("Ayşe", "Yılmaz");
        pkg = new SessionPackage("8'li Paket", new BigDecimal("1500"), 8, TODAY);
    }

    @Test
    @DisplayName("assign: ödemesiz atamada toplam seans paketten kopyalanır, kalan = toplam")
    void assign_unpaid_copiesSessionCount() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(packageRepository.findById(2L)).thenReturn(Optional.of(pkg));
        when(repository.save(any(StudentPackage.class))).thenAnswer(i -> i.getArgument(0));

        StudentPackageDto result = service.assign(1L, 2L, false, TODAY);

        assertThat(result.studentName()).isEqualTo("Ayşe Yılmaz");
        assertThat(result.packageName()).isEqualTo("8'li Paket");
        assertThat(result.totalSessions()).isEqualTo(8);
        assertThat(result.remainingSessions()).isEqualTo(8);
        assertThat(result.paid()).isFalse();
        assertThat(result.paymentDate()).isNull();
        assertThat(result.assignedDate()).isEqualTo(TODAY);
    }

    @Test
    @DisplayName("assign: ödeme alındıysa tarih kaydedilir")
    void assign_paid_storesPaymentDate() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(packageRepository.findById(2L)).thenReturn(Optional.of(pkg));
        when(repository.save(any(StudentPackage.class))).thenAnswer(i -> i.getArgument(0));

        StudentPackageDto result = service.assign(1L, 2L, true, TODAY.minusDays(1));

        assertThat(result.paid()).isTrue();
        assertThat(result.paymentDate()).isEqualTo(TODAY.minusDays(1));
    }

    @Test
    @DisplayName("assign: pasif paket atanamaz")
    void assign_inactivePackage_throws() {
        pkg.setActive(false);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(packageRepository.findById(2L)).thenReturn(Optional.of(pkg));

        assertThatThrownBy(() -> service.assign(1L, 2L, false, null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("student_package.error.package_not_active");
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("assign: pasif öğrenciye atama yapılamaz")
    void assign_inactiveStudent_throws() {
        student.setActive(false);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));

        assertThatThrownBy(() -> service.assign(1L, 2L, false, null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("student_package.error.student_not_active");
    }

    @Test
    @DisplayName("assign: öğrenci veya paket seçilmemişse hata")
    void assign_missingSelection_throws() {
        assertThatThrownBy(() -> service.assign(null, 2L, false, null))
                .hasMessage("student_package.error.student_required");
        assertThatThrownBy(() -> service.assign(1L, null, false, null))
                .hasMessage("student_package.error.package_required");
    }

    @Test
    @DisplayName("assign: ödeme alındı ama tarih yoksa veya ileri tarihse hata")
    void assign_paidWithInvalidDate_throws() {
        assertThatThrownBy(() -> service.assign(1L, 2L, true, null))
                .hasMessage("student_package.error.payment_date_required");
        assertThatThrownBy(() -> service.assign(1L, 2L, true, TODAY.plusDays(1)))
                .hasMessage("student_package.error.payment_date_future");
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("completePayment: kayıt getirilir, ödeme alanları set edilip kaydedilir")
    void completePayment_setsPaidAndDate() {
        StudentPackage sp = new StudentPackage(student, pkg, TODAY);
        when(repository.findById(5L)).thenReturn(Optional.of(sp));
        when(repository.save(any(StudentPackage.class))).thenAnswer(i -> i.getArgument(0));

        StudentPackageDto result = service.completePayment(5L, TODAY);

        ArgumentCaptor<StudentPackage> captor = ArgumentCaptor.forClass(StudentPackage.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().isPaid()).isTrue();
        assertThat(captor.getValue().getPaymentDate()).isEqualTo(TODAY);
        assertThat(result.paid()).isTrue();
    }

    @Test
    @DisplayName("completePayment: zaten ödenmiş kayıt tekrar ödenemez (çift ödeme)")
    void completePayment_alreadyPaid_throws() {
        StudentPackage sp = new StudentPackage(student, pkg, TODAY);
        sp.setPaid(true);
        sp.setPaymentDate(TODAY);
        when(repository.findById(5L)).thenReturn(Optional.of(sp));

        assertThatThrownBy(() -> service.completePayment(5L, TODAY))
                .hasMessage("student_package.error.already_paid");
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("completePayment: kayıt bulunamazsa hata")
    void completePayment_notFound_throws() {
        when(repository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.completePayment(5L, TODAY))
                .hasMessage("student_package.error.not_found");
    }

    @Test
    @DisplayName("search: filtre repository bayraklarına çevrilir, null ad boş metin olur")
    void search_mapsFilter() {
        when(repository.search("", true, false)).thenReturn(List.of());
        when(repository.search("ay", false, true)).thenReturn(List.of());
        when(repository.search("", false, false)).thenReturn(List.of());

        service.search(null, RemainingSessionFilter.HAS_REMAINING);
        service.search(" ay ", RemainingSessionFilter.FINISHED);
        service.search("", null);

        verify(repository).search("", true, false);
        verify(repository).search("ay", false, true);
        verify(repository).search("", false, false);
    }

    @Test
    @DisplayName("cancel: ödemesi alınmamış atama tamamen silinir")
    void cancel_unpaid_deletes() {
        StudentPackage sp = new StudentPackage(student, pkg, TODAY);
        when(repository.findById(5L)).thenReturn(Optional.of(sp));

        service.cancel(5L);

        verify(repository).delete(sp);
    }

    @Test
    @DisplayName("cancel: seansı kullanılmış atama iptal edilemez")
    void cancel_used_throws() {
        StudentPackage sp = new StudentPackage(student, pkg, TODAY);
        sp.useSession();
        when(repository.findById(5L)).thenReturn(Optional.of(sp));

        assertThatThrownBy(() -> service.cancel(5L))
                .hasMessage("student_package.error.cancel_used");
        verify(repository, never()).delete(any());
    }

    @Test
    @DisplayName("assign: seans borcu yeni paketten düşülür ve borç satırları silinir")
    void assign_settlesDebts() {
        SessionDebt d1 = debtFor(student);
        SessionDebt d2 = debtFor(student);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(packageRepository.findById(2L)).thenReturn(Optional.of(pkg));
        when(repository.save(any(StudentPackage.class))).thenAnswer(i -> i.getArgument(0));
        when(debtRepository.findByStudentIdOldestFirst(any())).thenReturn(List.of(d1, d2));

        StudentPackageDto result = service.assign(1L, 2L, false, null);

        assertThat(result.usedSessions()).isEqualTo(2);
        assertThat(result.remainingSessions()).isEqualTo(6);
        assertThat(d1.getHistory().getStudentPackage()).isNotNull();
        verify(debtRepository).delete(d1);
        verify(debtRepository).delete(d2);
    }

    @Test
    @DisplayName("assign: paket borçtan küçükse kalan borç satırı silinmez")
    void assign_settlesDebtsUpToPackageSize() {
        SessionPackage single = new SessionPackage("Tek Seans", new BigDecimal("250"), 1, TODAY);
        SessionDebt d1 = debtFor(student);
        SessionDebt d2 = debtFor(student);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(packageRepository.findById(3L)).thenReturn(Optional.of(single));
        when(repository.save(any(StudentPackage.class))).thenAnswer(i -> i.getArgument(0));
        when(debtRepository.findByStudentIdOldestFirst(any())).thenReturn(List.of(d1, d2));

        StudentPackageDto result = service.assign(1L, 3L, false, null);

        assertThat(result.remainingSessions()).isZero();
        verify(debtRepository).delete(d1);
        verify(debtRepository, never()).delete(d2);
        assertThat(d2.getHistory().getStudentPackage()).isNull();
    }

    private SessionDebt debtFor(Student s) {
        Lesson lesson = new Lesson(new Instructor("Hoca", "Bir"), new Room("Salon", 6), TODAY.minusDays(3),
                LocalTime.of(10, 0), LocalTime.of(11, 0));
        StudentLessonHistory history = new StudentLessonHistory(new LessonParticipant(lesson, s), true, true);
        return new SessionDebt(history);
    }

    @Test
    @DisplayName("cancel: ödemesi alınmış atama iptal edilemez")
    void cancel_paid_throws() {
        StudentPackage sp = new StudentPackage(student, pkg, TODAY);
        sp.setPaid(true);
        sp.setPaymentDate(TODAY);
        when(repository.findById(5L)).thenReturn(Optional.of(sp));

        assertThatThrownBy(() -> service.cancel(5L))
                .hasMessage("student_package.error.cancel_paid");
        verify(repository, never()).delete(any());
    }

    @Test
    @DisplayName("cancel: kayıt bulunamazsa hata")
    void cancel_notFound_throws() {
        when(repository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancel(5L))
                .hasMessage("student_package.error.not_found");
    }
}
