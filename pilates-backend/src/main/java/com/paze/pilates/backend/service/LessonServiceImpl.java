package com.paze.pilates.backend.service;

import com.paze.pilates.backend.domain.Instructor;
import com.paze.pilates.backend.domain.Lesson;
import com.paze.pilates.backend.domain.Room;
import com.paze.pilates.backend.repository.InstructorRepository;
import com.paze.pilates.backend.repository.LessonRepository;
import com.paze.pilates.backend.repository.RoomRepository;
import com.paze.pilates.dto.BusinessException;
import com.paze.pilates.dto.LessonDto;
import com.paze.pilates.service.LessonService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LessonServiceImpl implements LessonService {

    private final LessonRepository lessonRepository;
    private final InstructorRepository instructorRepository;
    private final RoomRepository roomRepository;

    public LessonServiceImpl(LessonRepository lessonRepository,
                             InstructorRepository instructorRepository,
                             RoomRepository roomRepository) {
        this.lessonRepository = lessonRepository;
        this.instructorRepository = instructorRepository;
        this.roomRepository = roomRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LessonDto> findBetween(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) {
            throw new BusinessException("lesson.error.range_invalid");
        }
        return lessonRepository.findBetween(from, to).stream().map(LessonServiceImpl::toDto).toList();
    }

    @Override
    @Transactional
    public LessonDto openLesson(Long instructorId, Long roomId, LocalDate date,
                                LocalTime startTime, LocalTime endTime) {
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

        return toDto(lessonRepository.save(new Lesson(instructor, room, date, startTime, endTime)));
    }

    private static LessonDto toDto(Lesson l) {
        Instructor i = l.getInstructor();
        Room r = l.getRoom();
        return new LessonDto(l.getId(), i.getId(), i.getFirstName() + " " + i.getLastName(),
                r.getId(), r.getName(), l.getLessonDate(), l.getStartTime(), l.getEndTime());
    }
}
