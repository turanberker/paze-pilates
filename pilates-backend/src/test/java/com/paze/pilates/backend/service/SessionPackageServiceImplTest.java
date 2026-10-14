package com.paze.pilates.backend.service;

import com.paze.pilates.backend.domain.SessionPackage;
import com.paze.pilates.backend.repository.SessionPackageRepository;
import com.paze.pilates.dto.BusinessException;
import com.paze.pilates.dto.SessionPackageDto;
import java.math.BigDecimal;
import java.time.LocalDate;
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
class SessionPackageServiceImplTest {

    private static final LocalDate START = LocalDate.of(2026, 1, 1);

    @Mock
    private SessionPackageRepository repository;

    private SessionPackageServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SessionPackageServiceImpl(repository);
    }

    @Test
    @DisplayName("save: yeni paket ücret 2 basamağa yuvarlanarak kaydedilir")
    void save_new_persistsPackage() {
        when(repository.save(any(SessionPackage.class))).thenAnswer(i -> i.getArgument(0));

        SessionPackageDto result = service.save(
                new SessionPackageDto(null, " 8'li Paket ", new BigDecimal("1500.555"), 8, START, true));

        ArgumentCaptor<SessionPackage> captor = ArgumentCaptor.forClass(SessionPackage.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("8'li Paket");
        assertThat(captor.getValue().getPrice()).isEqualByComparingTo("1500.56");
        assertThat(result.sessionCount()).isEqualTo(8);
        assertThat(result.startDate()).isEqualTo(START);
    }

    @Test
    @DisplayName("save: güncellemede tanım alanları değişir, aktiflik değişmez")
    void save_existing_keepsActive() {
        SessionPackage existing = new SessionPackage("Eski", BigDecimal.TEN, 4, START);
        when(repository.findById(5L)).thenReturn(Optional.of(existing));
        when(repository.save(any(SessionPackage.class))).thenAnswer(i -> i.getArgument(0));

        SessionPackageDto result = service.save(
                new SessionPackageDto(5L, "Yeni", BigDecimal.ONE, 12, START, false));

        assertThat(result.name()).isEqualTo("Yeni");
        assertThat(result.active()).isTrue();
        assertThat(result.sessionCount()).isEqualTo(12);
    }

    @Test
    @DisplayName("deactivate / activate: kayıt getirilir ve aktiflik değişir")
    void deactivateAndActivate_toggleActive() {
        SessionPackage existing = new SessionPackage("Paket", BigDecimal.TEN, 4, START);
        when(repository.findById(5L)).thenReturn(Optional.of(existing));
        when(repository.save(any(SessionPackage.class))).thenAnswer(i -> i.getArgument(0));

        assertThat(service.deactivate(5L).active()).isFalse();
        assertThat(service.activate(5L).active()).isTrue();
    }

    @Test
    @DisplayName("deactivate: olmayan paket için hata")
    void deactivate_unknownId_throws() {
        when(repository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deactivate(9L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("package.error.not_found");
    }

    @Test
    @DisplayName("save: olmayan paket güncellenemez")
    void save_unknownId_throws() {
        when(repository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.save(
                new SessionPackageDto(9L, "X", BigDecimal.ONE, 1, START, true)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("package.error.not_found");
    }

    @Test
    @DisplayName("save: geçersiz alanlar BusinessException fırlatır")
    void save_invalid_throws() {
        assertInvalid(new SessionPackageDto(null, " ", BigDecimal.ONE, 1, START, true),
                "package.error.name_required");
        assertInvalid(new SessionPackageDto(null, "A", null, 1, START, true),
                "package.error.price_invalid");
        assertInvalid(new SessionPackageDto(null, "A", new BigDecimal("-1"), 1, START, true),
                "package.error.price_invalid");
        assertInvalid(new SessionPackageDto(null, "A", BigDecimal.ONE, 0, START, true),
                "package.error.session_count_invalid");
        assertInvalid(new SessionPackageDto(null, "A", BigDecimal.ONE, 1, null, true),
                "package.error.start_date_required");
        verify(repository, never()).save(any());
    }

    private void assertInvalid(SessionPackageDto dto, String key) {
        assertThatThrownBy(() -> service.save(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessage(key);
    }
}
