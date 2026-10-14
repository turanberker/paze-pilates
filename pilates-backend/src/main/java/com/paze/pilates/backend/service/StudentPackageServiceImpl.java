package com.paze.pilates.backend.service;

import com.paze.pilates.backend.domain.SessionDebt;
import com.paze.pilates.backend.domain.SessionPackage;
import com.paze.pilates.backend.domain.Student;
import com.paze.pilates.backend.domain.StudentPackage;
import com.paze.pilates.backend.mapper.StudentPackageMapper;
import com.paze.pilates.backend.repository.SessionDebtRepository;
import com.paze.pilates.backend.repository.SessionPackageRepository;
import com.paze.pilates.backend.repository.StudentPackageRepository;
import com.paze.pilates.backend.repository.StudentRepository;
import com.paze.pilates.backend.security.PermissionGuard;
import com.paze.pilates.dto.BusinessException;
import com.paze.pilates.dto.Permission;
import com.paze.pilates.dto.RemainingSessionFilter;
import com.paze.pilates.dto.StudentPackageDto;
import com.paze.pilates.service.StudentPackageService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudentPackageServiceImpl implements StudentPackageService {

    private final StudentPackageRepository repository;
    private final StudentRepository studentRepository;
    private final SessionPackageRepository packageRepository;
    private final SessionDebtRepository debtRepository;
    private final PermissionGuard guard;
    private final StudentPackageMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<StudentPackageDto> search(String studentName, RemainingSessionFilter filter) {
        guard.require(Permission.PAYMENT_MANAGE);
        String term = studentName == null ? "" : studentName.trim();
        RemainingSessionFilter f = filter == null ? RemainingSessionFilter.ALL : filter;
        return repository.search(term,
                        f == RemainingSessionFilter.HAS_REMAINING,
                        f == RemainingSessionFilter.FINISHED)
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public StudentPackageDto assign(Long studentId, Long packageId, boolean paid, LocalDate paymentDate) {
        guard.require(Permission.PAYMENT_MANAGE);
        if (studentId == null) {
            throw new BusinessException("student_package.error.student_required");
        }
        if (packageId == null) {
            throw new BusinessException("student_package.error.package_required");
        }
        if (paid) {
            validatePaymentDate(paymentDate);
        }

        Student student = studentRepository.findById(studentId)
                .filter(Student::isActive)
                .orElseThrow(() -> new BusinessException("student_package.error.student_not_active"));
        SessionPackage pkg = packageRepository.findById(packageId)
                .filter(SessionPackage::isActive)
                .orElseThrow(() -> new BusinessException("student_package.error.package_not_active"));

        StudentPackage sp = new StudentPackage(student, pkg, LocalDate.now());
        if (paid) {
            sp.setPaid(true);
            sp.setPaymentDate(paymentDate);
        }
        sp = repository.save(sp);
        settleDebts(sp);
        return mapper.toDto(sp);
    }

    /** Öğrencinin borçlu seanslarını en eski dersten başlayarak yeni paketten düşer; düşülen borç satırı silinir. */
    private void settleDebts(StudentPackage sp) {
        for (SessionDebt debt : debtRepository.findByStudentIdOldestFirst(sp.getStudent().getId())) {
            if (!sp.hasRemaining()) {
                return;
            }
            debt.getHistory().chargeTo(sp);
            debtRepository.delete(debt);
        }
    }

    @Override
    @Transactional
    public StudentPackageDto completePayment(Long studentPackageId, LocalDate paymentDate) {
        guard.require(Permission.PAYMENT_MANAGE);
        StudentPackage sp = findOrThrow(studentPackageId);
        if (sp.isPaid()) {
            throw new BusinessException("student_package.error.already_paid");
        }
        validatePaymentDate(paymentDate);

        sp.setPaid(true);
        sp.setPaymentDate(paymentDate);
        return mapper.toDto(repository.save(sp));
    }

    @Override
    @Transactional
    public void cancel(Long studentPackageId) {
        guard.require(Permission.PAYMENT_MANAGE);
        StudentPackage sp = findOrThrow(studentPackageId);
        if (sp.isPaid()) {
            throw new BusinessException("student_package.error.cancel_paid");
        }
        if (sp.getUsedSessions() > 0) {
            throw new BusinessException("student_package.error.cancel_used");
        }
        repository.delete(sp);
    }

    private StudentPackage findOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException("student_package.error.not_found"));
    }

    private void validatePaymentDate(LocalDate paymentDate) {
        if (paymentDate == null) {
            throw new BusinessException("student_package.error.payment_date_required");
        }
        if (paymentDate.isAfter(LocalDate.now())) {
            throw new BusinessException("student_package.error.payment_date_future");
        }
    }
}
