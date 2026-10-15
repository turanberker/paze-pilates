package com.paze.pilates.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.paze.pilates.backend.domain.Instructor;
import com.paze.pilates.backend.domain.Lesson;
import com.paze.pilates.backend.domain.Room;
import com.paze.pilates.backend.repository.LessonParticipantRepository;
import com.paze.pilates.backend.repository.LessonRepository;
import com.paze.pilates.backend.repository.SessionDebtRepository;
import com.paze.pilates.backend.repository.StudentLessonHistoryRepository;
import com.paze.pilates.backend.repository.StudentPackageRepository;
import com.paze.pilates.backend.security.PermissionGuard;
import com.paze.pilates.dto.BusinessException;
import com.paze.pilates.dto.DailySummaryDto;
import com.paze.pilates.dto.Permission;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 15);

    @Mock private LessonRepository lessonRepository;
    @Mock private LessonParticipantRepository participantRepository;
    @Mock private StudentLessonHistoryRepository historyRepository;
    @Mock private StudentPackageRepository studentPackageRepository;
    @Mock private SessionDebtRepository debtRepository;
    @Mock private PermissionGuard guard;

    private DashboardServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DashboardServiceImpl(lessonRepository, participantRepository, historyRepository,
                studentPackageRepository, debtRepository, guard);
    }

    private Lesson lesson(long id, int capacity, boolean completed) {
        Lesson l = new Lesson(org.mockito.Mockito.mock(Instructor.class), new Room("Oda", capacity), DAY,
                LocalTime.of(10, 0), LocalTime.of(11, 0));
        ReflectionTestUtils.setField(l, "id", id);
        if (completed) {
            l.markCompleted(LocalDateTime.of(DAY, LocalTime.of(11, 5)), "admin");
        }
        return l;
    }

    @Test
    void summaryAggregatesLessonsAndMoney() {
        when(lessonRepository.findBetween(DAY, DAY)).thenReturn(List.of(lesson(1, 4, true), lesson(2, 6, false)));
        when(participantRepository.countByLessonIds(any())).thenReturn(List.of(new Object[] {1L, 3L}, new Object[] {2L, 2L}));
        when(participantRepository.countDistinctStudentsOn(DAY)).thenReturn(4L);
        when(historyRepository.countAttendedOn(DAY)).thenReturn(3L);
        when(studentPackageRepository.sumPriceCollectedOn(DAY)).thenReturn(new BigDecimal("1500.00"));
        when(studentPackageRepository.countCollectedOn(DAY)).thenReturn(2L);
        when(studentPackageRepository.countExpiring(2)).thenReturn(5L);
        when(studentPackageRepository.countUnpaid()).thenReturn(1L);
        when(studentPackageRepository.sumPriceUnpaid()).thenReturn(new BigDecimal("700.00"));
        when(debtRepository.countDebtStudents()).thenReturn(2L);
        when(lessonRepository.countByLessonDateBeforeAndCompletedAtIsNull(DAY)).thenReturn(1L);

        DailySummaryDto s = service.getDailySummary(DAY);

        assertThat(s.lessonCount()).isEqualTo(2);
        assertThat(s.completedLessonCount()).isEqualTo(1);
        assertThat(s.reservationCount()).isEqualTo(5);
        assertThat(s.studentCount()).isEqualTo(4);
        assertThat(s.attendedCount()).isEqualTo(3);
        assertThat(s.occupancyPercent()).isEqualTo(50);
        assertThat(s.collectedAmount()).isEqualByComparingTo("1500");
        assertThat(s.collectedPackageCount()).isEqualTo(2);
        assertThat(s.expiringPackageCount()).isEqualTo(5);
        assertThat(s.unpaidPackageCount()).isEqualTo(1);
        assertThat(s.unpaidAmount()).isEqualByComparingTo("700");
        assertThat(s.debtStudentCount()).isEqualTo(2);
        assertThat(s.pendingLessonCount()).isEqualTo(1);
    }

    @Test
    void emptyDayHasZeroOccupancy() {
        when(lessonRepository.findBetween(DAY, DAY)).thenReturn(Collections.emptyList());
        when(studentPackageRepository.sumPriceCollectedOn(DAY)).thenReturn(BigDecimal.ZERO);
        when(studentPackageRepository.sumPriceUnpaid()).thenReturn(BigDecimal.ZERO);

        DailySummaryDto s = service.getDailySummary(DAY);

        assertThat(s.lessonCount()).isZero();
        assertThat(s.occupancyPercent()).isZero();
    }

    @Test
    void requiresReportPermission() {
        doThrow(new BusinessException("auth.error.forbidden")).when(guard).require(Permission.REPORT_VIEW);

        assertThatThrownBy(() -> service.getDailySummary(DAY)).isInstanceOf(BusinessException.class);
    }
}
