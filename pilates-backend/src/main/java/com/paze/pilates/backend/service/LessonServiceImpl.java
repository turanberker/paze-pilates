package com.paze.pilates.backend.service;

import com.paze.pilates.backend.domain.Instructor;
import com.paze.pilates.backend.domain.Lesson;
import com.paze.pilates.backend.domain.LessonParticipant;
import com.paze.pilates.backend.domain.Room;
import com.paze.pilates.backend.domain.Student;
import com.paze.pilates.backend.repository.InstructorRepository;
import com.paze.pilates.backend.repository.LessonParticipantRepository;
import com.paze.pilates.backend.repository.LessonRepository;
import com.paze.pilates.backend.repository.RoomRepository;
import com.paze.pilates.backend.repository.StudentRepository;
import com.paze.pilates.backend.security.CurrentUser;
import com.paze.pilates.backend.security.PermissionGuard;
import com.paze.pilates.dto.BusinessException;
import com.paze.pilates.dto.LessonDto;
import com.paze.pilates.dto.LessonParticipantDto;
import com.paze.pilates.dto.Permission;
import com.paze.pilates.service.LessonService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LessonServiceImpl implements LessonService {

    private final LessonRepository lessonRepository;
    private final InstructorRepository instructorRepository;
    private final RoomRepository roomRepository;
    private final LessonParticipantRepository participantRepository;
    private final StudentRepository studentRepository;
    private final PermissionGuard guard;

    public LessonServiceImpl(LessonRepository lessonRepository,
                             InstructorRepository instructorRepository,
                             RoomRepository roomRepository,
                             LessonParticipantRepository participantRepository,
                             StudentRepository studentRepository,
                             PermissionGuard guard) {
        this.lessonRepository = lessonRepository;
        this.instructorRepository = instructorRepository;
        this.roomRepository = roomRepository;
        this.participantRepository = participantRepository;
        this.studentRepository = studentRepository;
        this.guard = guard;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LessonDto> findBetween(LocalDate from, LocalDate to) {
        CurrentUser user = guard.requireAny(Permission.LESSON_VIEW_ALL, Permission.LESSON_VIEW_OWN);
        if (from == null || to == null || to.isBefore(from)) {
            throw new BusinessException("lesson.error.range_invalid");
        }
        List<Lesson> lessons = lessonRepository.findBetween(from, to);
        if (!guard.has(Permission.LESSON_VIEW_ALL)) {
            lessons = lessons.stream().filter(l -> isOwnLesson(l, user)).toList();
        }
        Map<Long, Integer> counts = new HashMap<>();
        if (!lessons.isEmpty()) {
            for (Object[] row : participantRepository.countByLessonIds(lessons.stream().map(Lesson::getId).toList())) {
                counts.put((Long) row[0], ((Long) row[1]).intValue());
            }
        }
        return lessons.stream().map(l -> toDto(l, counts.getOrDefault(l.getId(), 0))).toList();
    }

    @Override
    @Transactional
    public LessonDto openLesson(Long instructorId, Long roomId, LocalDate date,
                                LocalTime startTime, LocalTime endTime) {
        guard.require(Permission.LESSON_MANAGE);
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

        return toDto(lessonRepository.save(new Lesson(instructor, room, date, startTime, endTime)), 0);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LessonParticipantDto> getParticipants(Long lessonId) {
        CurrentUser user = guard.requireAny(Permission.LESSON_VIEW_ALL, Permission.LESSON_VIEW_OWN);
        if (!guard.has(Permission.LESSON_VIEW_ALL)) {
            Lesson lesson = lessonRepository.findById(lessonId == null ? -1L : lessonId)
                    .orElseThrow(() -> new BusinessException("lesson.error.not_found"));
            if (!isOwnLesson(lesson, user)) {
                throw new BusinessException("auth.error.forbidden");
            }
        }
        return participantRepository.findByLessonId(lessonId).stream()
                .map(LessonServiceImpl::toParticipantDto)
                .toList();
    }

    @Override
    @Transactional
    public LessonParticipantDto addParticipant(Long lessonId, Long studentId) {
        guard.require(Permission.LESSON_MANAGE);
        if (lessonId == null) {
            throw new BusinessException("lesson.error.not_found");
        }
        if (studentId == null) {
            throw new BusinessException("lesson.error.student_required");
        }
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new BusinessException("lesson.error.not_found"));
        if (LocalDateTime.of(lesson.getLessonDate(), lesson.getEndTime()).isBefore(LocalDateTime.now())) {
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
        return toParticipantDto(participantRepository.save(new LessonParticipant(lesson, student)));
    }

    @Override
    @Transactional
    public void removeParticipant(Long participantId) {
        guard.require(Permission.LESSON_MANAGE);
        LessonParticipant participant = participantRepository.findById(participantId == null ? -1L : participantId)
                .orElseThrow(() -> new BusinessException("lesson.error.participant_not_found"));
        Lesson lesson = participant.getLesson();
        if (LocalDateTime.of(lesson.getLessonDate(), lesson.getEndTime()).isBefore(LocalDateTime.now())) {
            throw new BusinessException("lesson.error.already_finished_remove");
        }
        participantRepository.delete(participant);
    }

    private static boolean isOwnLesson(Lesson lesson, CurrentUser user) {
        return user.instructorId() != null && user.instructorId().equals(lesson.getInstructor().getId());
    }

    private static LessonParticipantDto toParticipantDto(LessonParticipant p) {
        Student s = p.getStudent();
        return new LessonParticipantDto(p.getId(), p.getLesson().getId(), s.getId(),
                s.getFirstName() + " " + s.getLastName(), s.getPhone());
    }

    private static LessonDto toDto(Lesson l, int participantCount) {
        Instructor i = l.getInstructor();
        Room r = l.getRoom();
        return new LessonDto(l.getId(), i.getId(), i.getFirstName() + " " + i.getLastName(),
                r.getId(), r.getName(), r.getCapacity(), participantCount,
                l.getLessonDate(), l.getStartTime(), l.getEndTime());
    }
}
