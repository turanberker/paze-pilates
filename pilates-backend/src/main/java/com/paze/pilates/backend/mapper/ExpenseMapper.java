package com.paze.pilates.backend.mapper;

import com.paze.pilates.backend.domain.Expense;
import com.paze.pilates.dto.ExpenseDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ExpenseMapper {

    ExpenseDto toDto(Expense expense);
}
