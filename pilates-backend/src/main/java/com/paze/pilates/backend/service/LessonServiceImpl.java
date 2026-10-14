package com.paze.pilates.backend.service;

import com.paze.pilates.backend.domain.Instructor;
import com.paze.pilates.backend.domain.Lesson;
import com.paze.pilates.backend.domain.LessonParticipant;
import com.paze.pilates.backend.domain.Room;
import com.paze.pilates.backend.domain.SessionDebt;
import com.paze.pilates.backend.domain.Student;
import com.paze.pilates.backend.domain.StudentLessonHistory;
import com.paze.pilates.backend.domain.StudentPackage;
import com.paze.pilates.backend.mapper.InstructorMapper;
import com.paze.pilates.backend.mapper.LessonMapper;
import com.paze.pilates.backend.mapper.RoomMapper;
import com.paze.pilates.backend.repository.InstructorRepository;
import com.paze.pilates.backend.repository.LessonParticipantRepository;
import com.paze.pilates.backend.repository.LessonRepository;
import com.paze.pilates.backend.repository.RoomRepository;
import com.paze.pilates.backend.repository.SessionDebtRepository;
import com.paze.pilates.backend.repository.StudentLessonHistoryRepository;
import com.paze.pilates.backend.repository.StudentPackageRepository;
import com.paze.pilates.backend.repository.StudentRepository;
import com.paze.pilates.backend.security.CurrentUser;
import com.paze.pilates.backend.security.PermissionGuard;
import com.paze.pilates.dto.AttendanceDto;
import com.paze.pilates.dto.BusinessException;
import com.paze.pilates.dto.InstructorDto;
import com.paze.pilates.dto.LessonDto;
import com.paze.pilates.dto.LessonParticipantDto;
import com.paze.pilates.dto.Permission;
import com.paze.pilates.dto.RoomDto;
import com.paze.pilates.service.LessonService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LessonServiceImpl implements LessonService {

    private final LessonRepository lessonRepository;
    private final InstructorRepository instructorRepository;
    private final RoomRepository roomRepository;
    private final LessonParticipantRepository participantRepository;
    private final StudentRepository studentRepository;
    private final StudentPackageRepository studentPackageRepository;
    private final StudentLessonHistoryRepository historyRepository;
    private final SessionDebtRepository debtRepository;
    private final PermissionGuard guard;
    private final LessonMapper lessonMapper;
    private final InstructorMapper instructorMapper;
    private final RoomMapper roomMapper;

    @Override
    @Transactional(readOnly = true)
    public List<LessonDto> findBetween(LocalDate from, LocalDate to) {
        guard.require(Permission.LESSON_VIEW_ALL);
        if (from == null || to == null || to.isBefore(from)) {
            throw new BusinessException("lesson.error.range_invalid");
        }
        return withCounts(lessonRepository.findBetween(from, to));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LessonDto> findMyLessonsOn(LocalDate date) {
        CurrentUser user = guard.require(Permission.LESSON_VIEW_ALL);
        if (date == null) {
            throw new BusinessException("lesson.error.date_required");
        }
        boolean all = guard.has(Permission.LESSON_MANAGE);
        return withCounts(lessonRepository.findBetween(date, date).stream()
                .filter(l -> all || isOwnLesson(l, user))
                .toList());
    }

    private List<LessonDto> withCounts(List<Lesson> lessons) {
        Map<Long, Integer> counts = new HashMap<>();
        if (!lessons.isEmpty()) {
            for (Object[] row : participantRepository.countByLessonIds(lessons.stream().map(Lesson::getId).toList())) {
                counts.put((Long) row[0], ((Long) row[1]).intValue());
            }
        }
        return lessons.stream().map(l -> lessonMapper.toDto(l, counts.getOrDefault(l.getId(), 0))).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstructorDto> findInstructorsForLessons() {
        CurrentUser user = guard.requireAny(Permission.LESSON_MANAGE, Permission.LESSON_MANAGE_OWN);
        boolean all = guard.has(Permission.LESSON_MANAGE);
        return instructorRepository.findByActiveTrueOrderByFirstNameAscLastNameAsc().stream()
                .filter(i -> all || (user.instructorId() != null && user.instructorId().equals(i.getId())))
                .map(instructorMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomDto> findRoomsForLessons() {
        guard.requireAny(Permission.LESSON_MANAGE, Permission.LESSON_MANAGE_OWN);
        return roomRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(roomMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public LessonDto openLesson(Long instructorId, Long roomId, LocalDate date,
                                LocalTime startTime, LocalTime endTime) {
        CurrentUser user = guard.requireAny(Permission.LESSON_MANAGE, Permission.LESSON_MANAGE_OWN);
        if (!guard.has(Permission.LESSON_MANAGE)
                && (user.instructorId() == null || !user.instructorId().equals(instructorId))) {
            throw new BusinessException("auth.error.forbidden");
        }
        if (instructorId == null) {
            throw new BusinessException("lesson.error.instructor_required");
        }
        if (roomId == null) {
            throw new BusinessException("lesson.error.room_required");
        }
        if (date == null) {
            throw new BusinessException("lesson.error.date_required");
        }
        if (startTime == null || endTime == null) {
            throw new BusinessException("lesson.error.time_required");
        }
        if (!startTime.isBefore(endTime)) {
            throw new BusinessException("lesson.error.time_order");
        }
        if (LocalDateTime.of(date, startTime).isBefore(LocalDateTime.now())) {
            throw new BusinessException("lesson.error.past");
        }

        Instructor instructor = instructorRepository.findById(instructorId)
                .filter(Instructor::isActive)
                .orElseThrow(() -> new BusinessException("lesson.error.instructor_not_active"));
        Room room = roomRepository.findById(roomId)
                .filter(Room::isActive)
                .orElseThrow(() -> new BusinessException("lesson.error.room_not_active"));

        if (lessonRepository.existsRoomOverlap(roomId, date, startTime, endTime)) {
            throw new BusinessException("lesson.error.room_conflict");
        }
        if (lessonRepository.existsInstructorOverlap(instructorId, date, startTime, endTime)) {
            throw new BusinessException("lesson.error.instructor_conflict");
        }

        return lessonMapper.toDto(lessonRepository.save(new Lesson(instructor, room, date, startTime, endTime)), 0);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LessonParticipantDto> getParticipants(Long lessonId) {
        guard.require(Permission.LESSON_VIEW_ALL);
        Map<Long, StudentLessonHistory> history = historyRepository.findByLessonId(lessonId).stream()
                .collect(Collectors.toMap(h -> h.getParticipant().getId(), Function.identity()));
        return participantRepository.findByLessonId(lessonId).stream()
                .map(p -> lessonMapper.toParticipantDto(p, history.get(p.getId())))
                .toList();
    }

    @Override
    @Transactional
    public LessonParticipantDto addParticipant(Long lessonId, Long studentId) {
        CurrentUser user = guard.requireAny(Permission.LESSON_MANAGE, Permission.LESSON_MANAGE_OWN);
        if (lessonId == null) {
            throw new BusinessException("lesson.error.not_found");
        }
        if (studentId == null) {
            throw new BusinessException("lesson.error.student_required");
        }
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new BusinessException("lesson.error.not_found"));
        requireCanManage(lesson, user);
        if (lesson.isCompleted()) {
            throw new BusinessException("lesson.error.completed_locked");
        }
        if (lesson.getEndDateTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("lesson.error.already_finished");
        }
        Student student = studentRepository.findById(studentId)
                .filter(Student::isActive)
                .orElseThrow(() -> new BusinessException("lesson.error.student_not_active"));

        if (participantRepository.existsByLessonIdAndStudentId(lessonId, studentId)) {
            throw new BusinessException("lesson.error.participant_duplicate");
        }
        if (participantRepository.countByLessonId(lessonId) >= lesson.getRoom().getCapacity()) {
            throw new BusinessException("lesson.error.capacity_full");
        }
        if (participantRepository.existsStudentOverlap(studentId, lesson.getLessonDate(),
                lesson.getStartTime(), lesson.getEndTime())) {
            throw new BusinessException("lesson.error.student_conflict");
        }
        return lessonMapper.toParticipantDto(participantRepository.save(new LessonParticipant(lesson, student)), null);
    }

    @Override
    @Transactional
    public void removeParticipant(Long participantId) {
        CurrentUser user = guard.requireAny(Permission.LESSON_MANAGE, Permission.LESSON_MANAGE_OWN);
        LessonParticipant participant = participantRepository.findById(participantId == null ? -1L : participantId)
                .orElseThrow(() -> new BusinessException("lesson.error.participant_not_found"));
        Lesson lesson = participant.getLesson();
        requireCanManage(lesson, user);
        if (lesson.isCompleted()) {
            throw new BusinessException("lesson.error.completed_locked");
        }
        if (lesson.getEndDateTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("lesson.error.already_finished_remove");
        }
        participantRepository.delete(participant);
    }

    @Override
    @Transactional
    public LessonDto completeLesson(Long lessonId, List<AttendanceDto> attendance) {
        CurrentUser user = guard.requireAny(Permission.LESSON_MANAGE, Permission.LESSON_MANAGE_OWN);
        Lesson lesson = lessonRepository.findById(lessonId == null ? -1L : lessonId)
                .orElseThrow(() -> new BusinessException("lesson.error.not_found"));
        requireCanManage(lesson, user);
        if (lesson.isCompleted()) {
            throw new BusinessException("lesson.error.already_completed");
        }
        if (lesson.getStartDateTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException("lesson.error.not_started");
        }

        List<LessonParticipant> participants = participantRepository.findByLessonId(lessonId);
        Map<Long, AttendanceDto> byParticipant = new HashMap<>();
        for (AttendanceDto a : attendance == null ? List.<AttendanceDto>of() : attendance) {
            if (a == null || a.participantId() == null || byParticipant.put(a.participantId(), a) != null) {
                throw new BusinessException("lesson.error.attendance_invalid");
            }
        }
        if (byParticipant.size() != participants.size()
                || !participants.stream().allMatch(p -> byParticipant.containsKey(p.getId()))) {
            throw new BusinessException("lesson.error.attendance_incomplete");
        }

        for (LessonParticipant p : participants) {
            AttendanceDto a = byParticipant.get(p.getId());
            StudentLessonHistory history = new StudentLessonHistory(p, a.attended(), a.charged());
            if (a.charged()) {
                List<StudentPackage> usable = studentPackageRepository.findUsableForStudent(p.getStudent().getId());
                if (!usable.isEmpty()) {
                    history.chargeTo(usable.get(0));
                }
            }
            history = historyRepository.save(history);
            if (a.charged() && history.getStudentPackage() == null) {
                debtRepository.save(new SessionDebt(history));
            }
        }
        lesson.markCompleted(LocalDateTime.now(), user.username());
        return lessonMapper.toDto(lessonRepository.save(lesson), participants.size());
    }

    @Override
    @Transactional
    public LessonDto undoCompletion(Long lessonId) {
        guard.require(Permission.LESSON_MANAGE);
        Lesson lesson = lessonRepository.findById(lessonId == null ? -1L : lessonId)
                .orElseThrow(() -> new BusinessException("lesson.error.not_found"));
        if (!lesson.isCompleted()) {
            throw new BusinessException("lesson.error.not_completed");
        }
        for (StudentLessonHistory h : historyRepository.findByLessonId(lessonId)) {
            if (h.getStudentPackage() != null) {
                h.getStudentPackage().refundSession();
            }
            debtRepository.findByHistoryId(h.getId()).ifPresent(debtRepository::delete);
            historyRepository.delete(h);
        }
        lesson.clearCompletion();
        return lessonMapper.toDto(lessonRepository.save(lesson), (int) participantRepository.countByLessonId(lessonId));
    }

    /** Yönetici her derse, hoca yalnızca kendi dersine işlem yapabilir. */
    private void requireCanManage(Lesson lesson, CurrentUser user) {
        if (!guard.has(Permission.LESSON_MANAGE) && !isOwnLesson(lesson, user)) {
            throw new BusinessException("auth.error.forbidden");
        }
    }

    private static boolean isOwnLesson(Lesson lesson, CurrentUser user) {
        return user.instructorId() != null && user.instructorId().equals(lesson.getInstructor().getId());
    }

}
