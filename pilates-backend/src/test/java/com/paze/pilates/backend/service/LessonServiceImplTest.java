package com.paze.pilates.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.paze.pilates.backend.domain.Instructor;
import com.paze.pilates.backend.domain.Lesson;
import com.paze.pilates.backend.domain.Room;
import com.paze.pilates.backend.repository.InstructorRepository;
import com.paze.pilates.backend.repository.LessonRepository;
import com.paze.pilates.backend.repository.RoomRepository;
import com.paze.pilates.dto.BusinessException;
import com.paze.pilates.dto.LessonDto;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LessonServiceImplTest {

    private static final LocalDate DATE = LocalDate.now().plusDays(3);
    private static final LocalTime START = LocalTime.of(10, 0);
    private static final LocalTime END = LocalTime.of(11, 0);

    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private InstructorRepository instructorRepository;
    @Mock
    private RoomRepository roomRepository;

    private LessonServiceImpl service;
    private Instructor instructor;
    private Room room;

    @BeforeEach
    void setUp() {
        service = new LessonServiceImpl(lessonRepository, instructorRepository, roomRepository);
        instructor = new Instructor("Ayşe", "Yılmaz");
        room = new Room("Reformer Salonu", 6);
    }

    private void stubActive() {
        when(instructorRepository.findById(1L)).thenReturn(Optional.of(instructor));
        when(roomRepository.findById(2L)).thenReturn(Optional.of(room));
    }

    @Test
    @DisplayName("openLesson: çakışma yoksa ders kaydedilir")
    void openLesson_ok() {
        stubActive();
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(i -> i.getArgument(0));

        LessonDto result = service.openLesson(1L, 2L, DATE, START, END);

        assertThat(result.instructorName()).isEqualTo("Ayşe Yılmaz");
        assertThat(result.roomName()).isEqualTo("Reformer Salonu");
        assertThat(result.date()).isEqualTo(DATE);
        assertThat(result.startTime()).isEqualTo(START);
        assertThat(result.endTime()).isEqualTo(END);
    }

    @Test
    @DisplayName("openLesson: oda aynı saat aralığında doluysa reddedilir")
    void openLesson_roomConflict() {
        stubActive();
        when(lessonRepository.existsRoomOverlap(2L, DATE, START, END)).thenReturn(true);

        assertThatThrownBy(() -> service.openLesson(1L, 2L, DATE, START, END))
                .isInstanceOf(BusinessException.class)
                .hasMessage("lesson.error.room_conflict");
        verify(lessonRepository, never()).save(any());
    }

    @Test
    @DisplayName("openLesson: hoca aynı saat aralığında başka derste ise reddedilir")
    void openLesson_instructorConflict() {
        stubActive();
        when(lessonRepository.existsInstructorOverlap(1L, DATE, START, END)).thenReturn(true);

        assertThatThrownBy(() -> service.openLesson(1L, 2L, DATE, START, END))
                .isInstanceOf(BusinessException.class)
                .hasMessage("lesson.error.instructor_conflict");
        verify(lessonRepository, never()).save(any());
    }

    @Test
    @DisplayName("openLesson: pasif hoca seçilemez")
    void openLesson_inactiveInstructor() {
        instructor.setActive(false);
        when(instructorRepository.findById(1L)).thenReturn(Optional.of(instructor));

        assertThatThrownBy(() -> service.openLesson(1L, 2L, DATE, START, END))
                .isInstanceOf(BusinessException.class)
                .hasMessage("lesson.error.instructor_not_active");
    }

    @Test
    @DisplayName("openLesson: pasif oda seçilemez")
    void openLesson_inactiveRoom() {
        room.setActive(false);
        when(instructorRepository.findById(1L)).thenReturn(Optional.of(instructor));
        when(roomRepository.findById(2L)).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> service.openLesson(1L, 2L, DATE, START, END))
                .isInstanceOf(BusinessException.class)
                .hasMessage("lesson.error.room_not_active");
    }

    @Test
    @DisplayName("openLesson: bitiş saati başlangıçtan sonra olmalı")
    void openLesson_timeOrder() {
        assertThatThrownBy(() -> service.openLesson(1L, 2L, DATE, END, START))
                .isInstanceOf(BusinessException.class)
                .hasMessage("lesson.error.time_order");
        assertThatThrownBy(() -> service.openLesson(1L, 2L, DATE, START, START))
                .isInstanceOf(BusinessException.class)
                .hasMessage("lesson.error.time_order");
    }

    @Test
    @DisplayName("openLesson: geçmişe ders açılamaz")
    void openLesson_past() {
        assertThatThrownBy(() -> service.openLesson(1L, 2L, LocalDate.now().minusDays(1), START, END))
                .isInstanceOf(BusinessException.class)
                .hasMessage("lesson.error.past");
    }

    @Test
    @DisplayName("openLesson: zorunlu alanlar boşsa reddedilir")
    void openLesson_required() {
        assertThatThrownBy(() -> service.openLesson(null, 2L, DATE, START, END))
                .hasMessage("lesson.error.instructor_required");
        assertThatThrownBy(() -> service.openLesson(1L, null, DATE, START, END))
                .hasMessage("lesson.error.room_required");
        assertThatThrownBy(() -> service.openLesson(1L, 2L, null, START, END))
                .hasMessage("lesson.error.date_required");
        assertThatThrownBy(() -> service.openLesson(1L, 2L, DATE, null, END))
                .hasMessage("lesson.error.time_required");
    }

    @Test
    @DisplayName("findBetween: geçersiz aralık reddedilir")
    void findBetween_invalidRange() {
        assertThatThrownBy(() -> service.findBetween(DATE, DATE.minusDays(1)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("lesson.error.range_invalid");
        verify(lessonRepository, never()).findBetween(eq(DATE), any());
    }
}
