package com.paze.pilates.backend.service;

import com.paze.pilates.backend.TestSecurity;
import com.paze.pilates.backend.domain.Expense;
import com.paze.pilates.backend.mapper.ExpenseMapper;
import com.paze.pilates.backend.repository.ExpenseRepository;
import com.paze.pilates.backend.repository.StudentPackageRepository;
import com.paze.pilates.dto.BusinessException;
import com.paze.pilates.dto.ExpenseDto;
import com.paze.pilates.dto.MonthlyFinanceSummaryDto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
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
class ExpenseServiceImplTest {

    private static final LocalDate DAY = LocalDate.of(2026, 3, 10);

    @Mock
    private ExpenseRepository repository;

    @Mock
    private StudentPackageRepository studentPackageRepository;

    private ExpenseServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ExpenseServiceImpl(repository, studentPackageRepository, TestSecurity.adminGuard(),
                Mappers.getMapper(ExpenseMapper.class));
    }

    @Test
    @DisplayName("save: yeni gider tutar 2 basamağa yuvarlanıp açıklama kırpılarak kaydedilir")
    void save_new_persistsExpense() {
        when(repository.save(any(Expense.class))).thenAnswer(i -> i.getArgument(0));

        ExpenseDto result = service.save(new ExpenseDto(null, DAY, new BigDecimal("1250.555"), "  Kira  "));

        ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("1250.56");
        assertThat(captor.getValue().getDescription()).isEqualTo("Kira");
        assertThat(result.expenseDate()).isEqualTo(DAY);
    }

    @Test
    @DisplayName("save: mevcut gider güncellenir")
    void save_existing_updatesFields() {
        Expense existing = new Expense(DAY, BigDecimal.TEN, "Eski");
        when(repository.findById(3L)).thenReturn(Optional.of(existing));
        when(repository.save(any(Expense.class))).thenAnswer(i -> i.getArgument(0));

        ExpenseDto result = service.save(new ExpenseDto(3L, DAY.minusDays(1), new BigDecimal("300"), "Fatura"));

        assertThat(result.description()).isEqualTo("Fatura");
        assertThat(result.amount()).isEqualByComparingTo("300.00");
        assertThat(result.expenseDate()).isEqualTo(DAY.minusDays(1));
    }

    @Test
    @DisplayName("save: sıfır/negatif tutar, boş açıklama ve ileri tarih reddedilir")
    void save_invalid_throws() {
        assertThatThrownBy(() -> service.save(new ExpenseDto(null, DAY, BigDecimal.ZERO, "Kira")))
                .isInstanceOf(BusinessException.class).hasMessage("expense.error.amount_invalid");
        assertThatThrownBy(() -> service.save(new ExpenseDto(null, DAY, new BigDecimal("-5"), "Kira")))
                .isInstanceOf(BusinessException.class).hasMessage("expense.error.amount_invalid");
        assertThatThrownBy(() -> service.save(new ExpenseDto(null, DAY, BigDecimal.TEN, "  ")))
                .isInstanceOf(BusinessException.class).hasMessage("expense.error.description_required");
        assertThatThrownBy(() -> service.save(new ExpenseDto(null, null, BigDecimal.TEN, "Kira")))
                .isInstanceOf(BusinessException.class).hasMessage("expense.error.date_required");
        assertThatThrownBy(() -> service.save(
                new ExpenseDto(null, LocalDate.now().plusDays(1), BigDecimal.TEN, "Kira")))
                .isInstanceOf(BusinessException.class).hasMessage("expense.error.date_future");
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("delete: olmayan gider hata verir, olan silinir")
    void delete_removesExisting() {
        Expense existing = new Expense(DAY, BigDecimal.TEN, "Kira");
        when(repository.findById(3L)).thenReturn(Optional.of(existing));
        when(repository.findById(4L)).thenReturn(Optional.empty());

        service.delete(3L);

        verify(repository).delete(existing);
        assertThatThrownBy(() -> service.delete(4L))
                .isInstanceOf(BusinessException.class).hasMessage("expense.error.not_found");
    }

    @Test
    @DisplayName("getMonthlySummary: ayın ilk ve son günü arasında gelir, gider ve net hesaplanır")
    void getMonthlySummary_computesNet() {
        LocalDate from = LocalDate.of(2026, 2, 1);
        LocalDate to = LocalDate.of(2026, 2, 28);
        when(studentPackageRepository.sumPriceCollectedBetween(from, to)).thenReturn(new BigDecimal("5000.00"));
        when(studentPackageRepository.countCollectedBetween(from, to)).thenReturn(3L);
        when(repository.sumAmountBetween(from, to)).thenReturn(new BigDecimal("1750.50"));
        when(repository.countByExpenseDateBetween(from, to)).thenReturn(2L);

        MonthlyFinanceSummaryDto s = service.getMonthlySummary(YearMonth.of(2026, 2));

        assertThat(s.income()).isEqualByComparingTo("5000.00");
        assertThat(s.incomePackageCount()).isEqualTo(3);
        assertThat(s.expense()).isEqualByComparingTo("1750.50");
        assertThat(s.expenseCount()).isEqualTo(2);
        assertThat(s.net()).isEqualByComparingTo("3249.50");
    }

    @Test
    @DisplayName("Hoca giderleri göremez")
    void instructor_denied() {
        ExpenseServiceImpl asInstructor = new ExpenseServiceImpl(repository, studentPackageRepository,
                TestSecurity.guardFor(TestSecurity.instructor(2L, 5L)), Mappers.getMapper(ExpenseMapper.class));

        assertThatThrownBy(() -> asInstructor.findByMonth(YearMonth.of(2026, 2)))
                .isInstanceOf(BusinessException.class);
    }
}
