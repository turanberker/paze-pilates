package com.paze.pilates.backend.service;

import com.paze.pilates.backend.domain.SessionPackage;
import com.paze.pilates.backend.domain.Student;
import com.paze.pilates.backend.domain.StudentPackage;
import com.paze.pilates.backend.repository.SessionPackageRepository;
import com.paze.pilates.backend.repository.StudentPackageRepository;
import com.paze.pilates.backend.repository.StudentRepository;
import com.paze.pilates.dto.BusinessException;
import com.paze.pilates.dto.RemainingSessionFilter;
import com.paze.pilates.dto.StudentPackageDto;
import com.paze.pilates.service.StudentPackageService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentPackageServiceImpl implements StudentPackageService {

    private final StudentPackageRepository repository;
    private final StudentRepository studentRepository;
    private final SessionPackageRepository packageRepository;

    public StudentPackageServiceImpl(
            StudentPackageRepository repository,
            StudentRepository studentRepository,
            SessionPackageRepository packageRepository) {
        this.repository = repository;
        this.studentRepository = studentRepository;
        this.packageRepository = packageRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentPackageDto> search(String studentName, RemainingSessionFilter filter) {
        String term = studentName == null ? "" : studentName.trim();
        RemainingSessionFilter f = filter == null ? RemainingSessionFilter.ALL : filter;
        return repository.search(term,
                        f == RemainingSessionFilter.HAS_REMAINING,
                        f == RemainingSessionFilter.FINISHED)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public StudentPackageDto assign(Long studentId, Long packageId, boolean paid, LocalDate paymentDate) {
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
        return toDto(repository.save(sp));
    }

    @Override
    @Transactional
    public StudentPackageDto completePayment(Long studentPackageId, LocalDate paymentDate) {
        StudentPackage sp = findOrThrow(studentPackageId);
        if (sp.isCancelled()) {
            throw new BusinessException("student_package.error.cancelled");
        }
        if (sp.isPaid()) {
            throw new BusinessException("student_package.error.already_paid");
        }
        validatePaymentDate(paymentDate);

        sp.setPaid(true);
        sp.setPaymentDate(paymentDate);
        return toDto(repository.save(sp));
    }

    @Override
    @Transactional
    public StudentPackageDto cancel(Long studentPackageId) {
        StudentPackage sp = findOrThrow(studentPackageId);
        if (sp.isCancelled()) {
            throw new BusinessException("student_package.error.already_cancelled");
        }
        if (sp.isPaid()) {
            throw new BusinessException("student_package.error.cancel_paid");
        }

        sp.setCancelled(true);
        sp.setCancelledDate(LocalDate.now());
        return toDto(repository.save(sp));
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

    private StudentPackageDto toDto(StudentPackage sp) {
        Student s = sp.getStudent();
        SessionPackage p = sp.getSessionPackage();
        return new StudentPackageDto(
                sp.getId(),
                s.getId(),
                s.getFirstName() + " " + s.getLastName(),
                p.getId(),
                p.getName(),
                sp.getTotalSessions(),
                sp.getUsedSessions(),
                sp.getRemainingSessions(),
                sp.isPaid(),
                sp.getPaymentDate(),
                sp.getAssignedDate(),
                sp.isCancelled(),
                sp.getCancelledDate());
    }
}
