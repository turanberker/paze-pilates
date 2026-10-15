package com.paze.pilates.backend.repository;

import com.paze.pilates.backend.domain.Expense;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByExpenseDateBetweenOrderByExpenseDateDescIdDesc(LocalDate from, LocalDate to);

    long countByExpenseDateBetween(LocalDate from, LocalDate to);

    /** Verilen tarih aralığındaki (uçlar dahil) giderlerin toplamı. */
    @Query("select coalesce(sum(e.amount), 0) from Expense e where e.expenseDate between :from and :to")
    BigDecimal sumAmountBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
