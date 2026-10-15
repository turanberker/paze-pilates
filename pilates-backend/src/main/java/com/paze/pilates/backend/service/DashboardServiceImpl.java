package com.paze.pilates.backend.service;

import com.paze.pilates.backend.domain.Lesson;
import com.paze.pilates.backend.repository.LessonParticipantRepository;
import com.paze.pilates.backend.repository.LessonRepository;
import com.paze.pilates.backend.repository.SessionDebtRepository;
import com.paze.pilates.backend.repository.StudentLessonHistoryRepository;
import com.paze.pilates.backend.repository.StudentPackageRepository;
import com.paze.pilates.backend.security.PermissionGuard;
import com.paze.pilates.dto.DailySummaryDto;
import com.paze.pilates.dto.Permission;
import com.paze.pilates.service.DashboardService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final LessonRepository lessonRepository;
    private final LessonParticipantRepository participantRepository;
    private final StudentLessonHistoryRepository historyRepository;
    private final StudentPackageRepository studentPackageRepository;
    private final SessionDebtRepository debtRepository;
    private final PermissionGuard guard;

    @Override
    @Transactional(readOnly = true)
    public DailySummaryDto getDailySummary(LocalDate date) {
        guard.require(Permission.REPORT_VIEW);

        List<Lesson> lessons = lessonRepository.findBetween(date, date);
        int completed = (int) lessons.stream().filter(Lesson::isCompleted).count();
        int capacity = lessons.stream().mapToInt(l -> l.getRoom().getCapacity()).sum();
        int reservations = 0;
        if (!lessons.isEmpty()) {
            for (Object[] row : participantRepository.countByLessonIds(lessons.stream().map(Lesson::getId).toList())) {
                reservations += ((Number) row[1]).intValue();
            }
        }
        int occupancy = capacity == 0 ? 0 : Math.min(100, Math.round(reservations * 100f / capacity));

        return new DailySummaryDto(
                date,
                lessons.size(),
                completed,
                reservations,
                (int) participantRepository.countDistinctStudentsOn(date),
                (int) historyRepository.countAttendedOn(date),
                occupancy,
                studentPackageRepository.sumPriceCollectedOn(date),
                (int) studentPackageRepository.countCollectedOn(date),
                (int) studentPackageRepository.countExpiring(EXPIRING_REMAINING_THRESHOLD),
                (int) studentPackageRepository.countUnpaid(),
                studentPackageRepository.sumPriceUnpaid(),
                (int) debtRepository.countDebtStudents(),
                (int) lessonRepository.countByLessonDateBeforeAndCompletedAtIsNull(date));
    }
}
