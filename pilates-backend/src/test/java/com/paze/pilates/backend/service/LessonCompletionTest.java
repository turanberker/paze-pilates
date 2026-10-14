package com.paze.pilates.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.paze.pilates.backend.TestSecurity;
import com.paze.pilates.backend.domain.Instructor;
import com.paze.pilates.backend.domain.Lesson;
import com.paze.pilates.backend.domain.LessonParticipant;
import com.paze.pilates.backend.domain.Room;
import com.paze.pilates.backend.domain.SessionDebt;
import com.paze.pilates.backend.domain.SessionPackage;
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
import com.paze.pilates.backend.security.PermissionGuard;
import com.paze.pilates.dto.AttendanceDto;
import com.paze.pilates.dto.BusinessException;
import com.paze.pilates.dto.LessonDto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** Ders tamamlama, yoklama, seans düşme, borç ve geri alma kuralları. */
@ExtendWith(MockitoExtension.class)
class LessonCompletionTest {

    private static final LocalDate YESTERDAY = LocalDate.now().minusDays(1);

    @Mock private LessonRepository lessonRepository;
    @Mock private InstructorRepository instructorRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private LessonParticipantRepository participantRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private StudentPackageRepository studentPackageRepository;
    @Mock private StudentLessonHistoryRepository historyRepository;
    @Mock private SessionDebtRepository debtRepository;

    private Lesson lesson;
    private LessonParticipant ali;
    private LessonParticipant zeynep;
    private SessionPackage eight;

    @BeforeEach
    void setUp() {
        Instructor instructor = withId(new Instructor("Ayşe", "Yılmaz"), 7L);
        lesson = withId(new Lesson(instructor, new Room("Reformer", 6), YESTERDAY,
                LocalTime.of(10, 0), LocalTime.of(11, 0)), 100L);
        ali = withId(new LessonParticipant(lesson, withId(new Student("Ali", "Kaya"), 1L)), 11L);
        zeynep = withId(new LessonParticipant(lesson, withId(new Student("Zeynep", "Kara"), 2L)), 12L);
        eight = new SessionPackage("8'li", new BigDecimal("1500"), 8, YESTERDAY);
    }

    private LessonServiceImpl service(PermissionGuard guard) {
        return new LessonServiceImpl(lessonRepository, instructorRepository, roomRepository, participantRepository,
                studentRepository, studentPackageRepository, historyRepository, debtRepository, guard,
                Mappers.getMapper(LessonMapper.class),
                Mappers.getMapper(InstructorMapper.class),
                Mappers.getMapper(RoomMapper.class));
    }

    private LessonServiceImpl admin() {
        return service(TestSecurity.adminGuard());
    }

    private void stubLesson() {
        when(lessonRepository.findById(100L)).thenReturn(Optional.of(lesson));
    }

    private void stubParticipantsAndSaves() {
        stubLesson();
        when(participantRepository.findByLessonId(100L)).thenReturn(List.of(ali, zeynep));
        when(historyRepository.save(any(StudentLessonHistory.class))).thenAnswer(i -> i.getArgument(0));
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    @DisplayName("completeLesson: gelenden düşülür, gelmeyenden hoca istemezse düşülmez")
    void complete_chargesAttendedOnly() {
        stubParticipantsAndSaves();
        StudentPackage aliPkg = new StudentPackage(ali.getStudent(), eight, YESTERDAY);
        when(studentPackageRepository.findUsableForStudent(1L)).thenReturn(List.of(aliPkg));

        LessonDto result = admin().completeLesson(100L, List.of(
                new AttendanceDto(11L, true, false),
                new AttendanceDto(12L, false, false)));

        assertThat(result.completed()).isTrue();
        assertThat(aliPkg.getUsedSessions()).isEqualTo(1);
        assertThat(lesson.getCompletedBy()).isEqualTo("admin");
        ArgumentCaptor<StudentLessonHistory> captor = ArgumentCaptor.forClass(StudentLessonHistory.class);
        verify(historyRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        StudentLessonHistory zeynepHistory = captor.getAllValues().get(1);
        assertThat(zeynepHistory.isAttended()).isFalse();
        assertThat(zeynepHistory.isCharged()).isFalse();
        verify(studentPackageRepository, never()).findUsableForStudent(2L);
        verify(debtRepository, never()).save(any());
    }

    @Test
    @DisplayName("completeLesson: hoca isterse gelmeyenin seansı da düşülür")
    void complete_chargesNoShowWhenInstructorDecides() {
        stubParticipantsAndSaves();
        StudentPackage aliPkg = new StudentPackage(ali.getStudent(), eight, YESTERDAY);
        StudentPackage zeynepPkg = new StudentPackage(zeynep.getStudent(), eight, YESTERDAY);
        when(studentPackageRepository.findUsableForStudent(1L)).thenReturn(List.of(aliPkg));
        when(studentPackageRepository.findUsableForStudent(2L)).thenReturn(List.of(zeynepPkg));

        admin().completeLesson(100L, List.of(
                new AttendanceDto(11L, true, false),
                new AttendanceDto(12L, false, true)));

        assertThat(zeynepPkg.getUsedSessions()).isEqualTo(1);
    }

    @Test
    @DisplayName("completeLesson: sıradaki ilk uygun paketten (repository sırası) düşülür")
    void complete_usesFirstUsablePackage() {
        stubParticipantsAndSaves();
        StudentPackage first = new StudentPackage(ali.getStudent(), eight, YESTERDAY.minusDays(30));
        StudentPackage second = new StudentPackage(ali.getStudent(), eight, YESTERDAY);
        when(studentPackageRepository.findUsableForStudent(1L)).thenReturn(List.of(first, second));

        admin().completeLesson(100L, List.of(
                new AttendanceDto(11L, true, false),
                new AttendanceDto(12L, false, false)));

        assertThat(first.getUsedSessions()).isEqualTo(1);
        assertThat(second.getUsedSessions()).isZero();
    }

    @Test
    @DisplayName("completeLesson: paketi olmayan öğrenci için borç satırı açılır")
    void complete_noPackage_createsDebt() {
        stubParticipantsAndSaves();
        when(studentPackageRepository.findUsableForStudent(1L)).thenReturn(List.of());

        admin().completeLesson(100L, List.of(
                new AttendanceDto(11L, true, false),
                new AttendanceDto(12L, false, false)));

        ArgumentCaptor<SessionDebt> captor = ArgumentCaptor.forClass(SessionDebt.class);
        verify(debtRepository).save(captor.capture());
        assertThat(captor.getValue().getStudent().getId()).isEqualTo(1L);
        assertThat(captor.getValue().getHistory().getStudentPackage()).isNull();
    }

    @Test
    @DisplayName("completeLesson: tamamlanmış ders tekrar tamamlanamaz (çift düşme yok)")
    void complete_twice_throws() {
        lesson.markCompleted(java.time.LocalDateTime.now(), "admin");
        stubLesson();

        assertThatThrownBy(() -> admin().completeLesson(100L, List.of()))
                .isInstanceOf(BusinessException.class).hasMessage("lesson.error.already_completed");
        verify(historyRepository, never()).save(any());
    }

    @Test
    @DisplayName("completeLesson: başlamamış ders tamamlanamaz")
    void complete_notStarted_throws() {
        Lesson future = withId(new Lesson(lesson.getInstructor(), lesson.getRoom(), LocalDate.now().plusDays(1),
                LocalTime.of(10, 0), LocalTime.of(11, 0)), 200L);
        when(lessonRepository.findById(200L)).thenReturn(Optional.of(future));

        assertThatThrownBy(() -> admin().completeLesson(200L, List.of()))
                .hasMessage("lesson.error.not_started");
    }

    @Test
    @DisplayName("completeLesson: her katılımcı için yoklama verilmeli")
    void complete_missingAttendance_throws() {
        stubLesson();
        when(participantRepository.findByLessonId(100L)).thenReturn(List.of(ali, zeynep));

        assertThatThrownBy(() -> admin().completeLesson(100L, List.of(new AttendanceDto(11L, true, false))))
                .hasMessage("lesson.error.attendance_incomplete");
        assertThatThrownBy(() -> admin().completeLesson(100L, List.of(
                new AttendanceDto(11L, true, false), new AttendanceDto(11L, false, false))))
                .hasMessage("lesson.error.attendance_invalid");
        verify(historyRepository, never()).save(any());
    }

    @Test
    @DisplayName("completeLesson: hoca başka hocanın dersini tamamlayamaz, kendi dersini tamamlar")
    void complete_instructorOnlyOwnLesson() {
        stubLesson();
        when(participantRepository.findByLessonId(100L)).thenReturn(List.of());
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(i -> i.getArgument(0));

        assertThatThrownBy(() -> service(TestSecurity.guardFor(TestSecurity.instructor(2L, 9L)))
                .completeLesson(100L, List.of()))
                .hasMessage("auth.error.forbidden");
        assertThat(service(TestSecurity.guardFor(TestSecurity.instructor(2L, 7L)))
                .completeLesson(100L, List.of()).completed()).isTrue();
    }

    @Test
    @DisplayName("undoCompletion: düşülen seans pakete iade edilir, borç ve geçmiş silinir")
    void undo_refundsAndDeletes() {
        lesson.markCompleted(java.time.LocalDateTime.now(), "admin");
        stubLesson();
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(i -> i.getArgument(0));
        StudentPackage aliPkg = new StudentPackage(ali.getStudent(), eight, YESTERDAY);
        StudentLessonHistory charged = withId(new StudentLessonHistory(ali, true, true), 21L);
        charged.chargeTo(aliPkg);
        StudentLessonHistory debtHistory = withId(new StudentLessonHistory(zeynep, true, true), 22L);
        SessionDebt debt = new SessionDebt(debtHistory);
        when(historyRepository.findByLessonId(100L)).thenReturn(List.of(charged, debtHistory));
        when(debtRepository.findByHistoryId(21L)).thenReturn(Optional.empty());
        when(debtRepository.findByHistoryId(22L)).thenReturn(Optional.of(debt));

        LessonDto result = admin().undoCompletion(100L);

        assertThat(result.completed()).isFalse();
        assertThat(aliPkg.getUsedSessions()).isZero();
        verify(debtRepository).delete(debt);
        verify(historyRepository).delete(charged);
        verify(historyRepository).delete(debtHistory);
    }

    @Test
    @DisplayName("undoCompletion: yalnızca yönetici geri alabilir")
    void undo_instructorForbidden() {
        assertThatThrownBy(() -> service(TestSecurity.guardFor(TestSecurity.instructor(2L, 7L)))
                .undoCompletion(100L))
                .hasMessage("auth.error.forbidden");
    }

    @Test
    @DisplayName("undoCompletion: tamamlanmamış ders geri alınamaz")
    void undo_notCompleted_throws() {
        stubLesson();

        assertThatThrownBy(() -> admin().undoCompletion(100L)).hasMessage("lesson.error.not_completed");
    }

    @Test
    @DisplayName("addParticipant/removeParticipant: tamamlanan ders kilitlidir")
    void completedLesson_isLocked() {
        lesson.markCompleted(java.time.LocalDateTime.now(), "admin");
        stubLesson();
        when(participantRepository.findById(11L)).thenReturn(Optional.of(ali));

        assertThatThrownBy(() -> admin().addParticipant(100L, 3L)).hasMessage("lesson.error.completed_locked");
        assertThatThrownBy(() -> admin().removeParticipant(11L)).hasMessage("lesson.error.completed_locked");
    }

    private static <T> T withId(T entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }
}
