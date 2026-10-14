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
import com.paze.pilates.backend.domain.LessonParticipant;
import com.paze.pilates.backend.domain.Room;
import com.paze.pilates.backend.domain.Student;
import com.paze.pilates.backend.repository.LessonParticipantRepository;
import com.paze.pilates.backend.repository.StudentRepository;
import com.paze.pilates.backend.repository.InstructorRepository;
import com.paze.pilates.backend.repository.LessonRepository;
import com.paze.pilates.backend.repository.RoomRepository;
import com.paze.pilates.dto.BusinessException;
import com.paze.pilates.dto.LessonDto;
import com.paze.pilates.dto.LessonParticipantDto;
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
    @Mock
    private LessonParticipantRepository participantRepository;
    @Mock
    private StudentRepository studentRepository;

    private LessonServiceImpl service;
    private Instructor instructor;
    private Room room;

    @BeforeEach
    void setUp() {
        service = new LessonServiceImpl(lessonRepository, instructorRepository, roomRepository,
                participantRepository, studentRepository);
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

    private Lesson futureLesson() {
        return new Lesson(instructor, room, DATE, START, END);
    }

    private void stubParticipantBase(Student student) {
        when(lessonRepository.findById(5L)).thenReturn(Optional.of(futureLesson()));
        when(studentRepository.findById(7L)).thenReturn(Optional.of(student));
    }

    @Test
    @DisplayName("addParticipant: kapasite doluysa reddedilir")
    void addParticipant_full() {
        stubParticipantBase(new Student("Ali", "Kaya"));
        when(participantRepository.countByLessonId(5L)).thenReturn(6L);

        assertThatThrownBy(() -> service.addParticipant(5L, 7L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("lesson.error.capacity_full");
        verify(participantRepository, never()).save(any());
    }

    @Test
    @DisplayName("addParticipant: öğrenci derste zaten varsa reddedilir")
    void addParticipant_duplicate() {
        stubParticipantBase(new Student("Ali", "Kaya"));
        when(participantRepository.existsByLessonIdAndStudentId(5L, 7L)).thenReturn(true);

        assertThatThrownBy(() -> service.addParticipant(5L, 7L))
                .hasMessage("lesson.error.participant_duplicate");
    }

    @Test
    @DisplayName("addParticipant: öğrenci aynı saatte başka derste ise reddedilir")
    void addParticipant_studentConflict() {
        stubParticipantBase(new Student("Ali", "Kaya"));
        when(participantRepository.existsStudentOverlap(7L, DATE, START, END)).thenReturn(true);

        assertThatThrownBy(() -> service.addParticipant(5L, 7L))
                .hasMessage("lesson.error.student_conflict");
    }

    @Test
    @DisplayName("addParticipant: pasif öğrenci eklenemez")
    void addParticipant_inactiveStudent() {
        Student passive = new Student("Ali", "Kaya");
        passive.setActive(false);
        stubParticipantBase(passive);

        assertThatThrownBy(() -> service.addParticipant(5L, 7L))
                .hasMessage("lesson.error.student_not_active");
    }

    @Test
    @DisplayName("addParticipant: bitmiş derse öğrenci eklenemez")
    void addParticipant_finishedLesson() {
        Lesson past = new Lesson(instructor, room, LocalDate.now().minusDays(1), START, END);
        when(lessonRepository.findById(5L)).thenReturn(Optional.of(past));

        assertThatThrownBy(() -> service.addParticipant(5L, 7L))
                .hasMessage("lesson.error.already_finished");
    }

    @Test
    @DisplayName("addParticipant: uygun öğrenci derse eklenir")
    void addParticipant_ok() {
        stubParticipantBase(new Student("Ali", "Kaya"));
        when(participantRepository.save(any(LessonParticipant.class))).thenAnswer(i -> i.getArgument(0));

        LessonParticipantDto result = service.addParticipant(5L, 7L);

        assertThat(result.studentName()).isEqualTo("Ali Kaya");
    }

    @Test
    @DisplayName("removeParticipant: gelecek dersten öğrenci çıkarılır")
    void removeParticipant_ok() {
        LessonParticipant p = new LessonParticipant(futureLesson(), new Student("Ali", "Kaya"));
        when(participantRepository.findById(9L)).thenReturn(Optional.of(p));

        service.removeParticipant(9L);

        verify(participantRepository).delete(p);
    }

    @Test
    @DisplayName("removeParticipant: bitmiş dersten çıkarma yapılamaz")
    void removeParticipant_finishedLesson() {
        Lesson past = new Lesson(instructor, room, LocalDate.now().minusDays(1), START, END);
        when(participantRepository.findById(9L))
                .thenReturn(Optional.of(new LessonParticipant(past, new Student("Ali", "Kaya"))));

        assertThatThrownBy(() -> service.removeParticipant(9L))
                .hasMessage("lesson.error.already_finished_remove");
        verify(participantRepository, never()).delete(any());
    }

    @Test
    @DisplayName("removeParticipant: katılım kaydı yoksa hata verir")
    void removeParticipant_notFound() {
        when(participantRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.removeParticipant(9L))
                .hasMessage("lesson.error.participant_not_found");
    }
}
