package com.paze.pilates.backend.service;

import com.paze.pilates.backend.domain.Expense;
import com.paze.pilates.backend.mapper.ExpenseMapper;
import com.paze.pilates.backend.repository.ExpenseRepository;
import com.paze.pilates.backend.repository.StudentPackageRepository;
import com.paze.pilates.backend.security.PermissionGuard;
import com.paze.pilates.dto.BusinessException;
import com.paze.pilates.dto.ExpenseDto;
import com.paze.pilates.dto.MonthlyFinanceSummaryDto;
import com.paze.pilates.dto.Permission;
import com.paze.pilates.service.ExpenseService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExpenseServiceImpl implements ExpenseService {

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("99999999.99");
    private static final int DESCRIPTION_MAX = 255;

    private final ExpenseRepository repository;
    private final StudentPackageRepository studentPackageRepository;
    private final PermissionGuard guard;
    private final ExpenseMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<ExpenseDto> findByMonth(YearMonth month) {
        guard.require(Permission.PAYMENT_MANAGE);
        YearMonth m = requireMonth(month);
        return repository.findByExpenseDateBetweenOrderByExpenseDateDescIdDesc(m.atDay(1), m.atEndOfMonth())
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public ExpenseDto save(ExpenseDto dto) {
        guard.require(Permission.PAYMENT_MANAGE);
        validate(dto);

        BigDecimal amount = dto.amount().setScale(2, RoundingMode.HALF_UP);
        String description = dto.description().trim();
        Expense e;
        if (dto.id() == null) {
            e = new Expense(dto.expenseDate(), amount, description);
        } else {
            e = findOrThrow(dto.id());
            e.setExpenseDate(dto.expenseDate());
            e.setAmount(amount);
            e.setDescription(description);
        }
        return mapper.toDto(repository.save(e));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        guard.require(Permission.PAYMENT_MANAGE);
        repository.delete(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public MonthlyFinanceSummaryDto getMonthlySummary(YearMonth month) {
        guard.require(Permission.PAYMENT_MANAGE);
        YearMonth m = requireMonth(month);
        LocalDate from = m.atDay(1);
        LocalDate to = m.atEndOfMonth();

        BigDecimal income = studentPackageRepository.sumPriceCollectedBetween(from, to);
        BigDecimal expense = repository.sumAmountBetween(from, to);
        return new MonthlyFinanceSummaryDto(
                m,
                income,
                (int) studentPackageRepository.countCollectedBetween(from, to),
                expense,
                (int) repository.countByExpenseDateBetween(from, to),
                income.subtract(expense));
    }

    private Expense findOrThrow(Long id) {
        if (id == null) {
            throw new BusinessException("expense.error.not_found");
        }
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException("expense.error.not_found"));
    }

    private YearMonth requireMonth(YearMonth month) {
        if (month == null) {
            throw new BusinessException("expense.error.month_required");
        }
        return month;
    }

    private void validate(ExpenseDto dto) {
        if (dto.expenseDate() == null) {
            throw new BusinessException("expense.error.date_required");
        }
        if (dto.expenseDate().isAfter(LocalDate.now())) {
            throw new BusinessException("expense.error.date_future");
        }
        if (dto.amount() == null || dto.amount().signum() <= 0 || dto.amount().compareTo(MAX_AMOUNT) > 0) {
            throw new BusinessException("expense.error.amount_invalid");
        }
        if (dto.description() == null || dto.description().isBlank()) {
            throw new BusinessException("expense.error.description_required");
        }
        if (dto.description().trim().length() > DESCRIPTION_MAX) {
            throw new BusinessException("expense.error.description_too_long");
        }
    }
}
