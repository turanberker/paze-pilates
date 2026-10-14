package com.paze.pilates.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.paze.pilates.backend.domain.Instructor;
import com.paze.pilates.backend.domain.Lesson;
import com.paze.pilates.backend.domain.LessonParticipant;
import com.paze.pilates.backend.domain.Room;
import com.paze.pilates.backend.domain.SessionPackage;
import com.paze.pilates.backend.domain.Student;
import com.paze.pilates.backend.repository.InstructorRepository;
import com.paze.pilates.backend.repository.LessonParticipantRepository;
import com.paze.pilates.backend.repository.LessonRepository;
import com.paze.pilates.backend.repository.RoomRepository;
import com.paze.pilates.backend.repository.SessionDebtRepository;
import com.paze.pilates.backend.repository.SessionPackageRepository;
import com.paze.pilates.backend.repository.StudentLessonHistoryRepository;
import com.paze.pilates.backend.repository.StudentRepository;
import com.paze.pilates.backend.security.SessionContext;
import com.paze.pilates.dto.AttendanceDto;
import com.paze.pilates.dto.LessonParticipantDto;
import com.paze.pilates.dto.StudentDto;
import com.paze.pilates.dto.StudentPackageDto;
import com.paze.pilates.service.LessonService;
import com.paze.pilates.service.StudentPackageService;
import com.paze.pilates.service.StudentService;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/** Tamamla → borç → paket atanınca borç düşer → geri al akışı, gerçek veritabanıyla (test sonunda geri alınır). */
@SpringBootTest
@Transactional
class LessonCompletionIntegrationTest {

    @Autowired private LessonService lessonService;
    @Autowired private StudentPackageService studentPackageService;
    @Autowired private StudentService studentService;
    @Autowired private SessionContext session;
    @Autowired private EntityManager entityManager;
    @Autowired private InstructorRepository instructorRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private SessionPackageRepository packageRepository;
    @Autowired private LessonRepository lessonRepository;
    @Autowired private LessonParticipantRepository participantRepository;
    @Autowired private StudentLessonHistoryRepository historyRepository;
    @Autowired private SessionDebtRepository debtRepository;

    @AfterEach
    void logout() {
        session.clear();
    }

    @Test
    void completeWithDebtThenAssignPackageThenUndo() {
        session.set(TestSecurity.admin());
        Instructor ayse = instructorRepository.save(new Instructor("Ayşe", "Yılmaz"));
        Room room = roomRepository.save(new Room("Reformer", 6));
        Student ali = studentRepository.save(new Student("Ali", "Kaya"));
        Student zeynep = studentRepository.save(new Student("Zeynep", "Kara"));
        Lesson lesson = lessonRepository.save(new Lesson(ayse, room, LocalDate.now().minusDays(1),
                LocalTime.of(10, 0), LocalTime.of(11, 0)));
        LessonParticipant aliP = participantRepository.save(new LessonParticipant(lesson, ali));
        LessonParticipant zeynepP = participantRepository.save(new LessonParticipant(lesson, zeynep));
        SessionPackage pkg = packageRepository.save(
                new SessionPackage("4'lü", new BigDecimal("800"), 4, LocalDate.now().minusDays(10)));
        StudentPackageDto zeynepPkg = studentPackageService.assign(zeynep.getId(), pkg.getId(), true, LocalDate.now());

        // Ali geldi ama paketi yok → borç; Zeynep gelmedi, hoca düşmeye karar verdi → paketinden düşer.
        lessonService.completeLesson(lesson.getId(), List.of(
                new AttendanceDto(aliP.getId(), true, false),
                new AttendanceDto(zeynepP.getId(), false, true)));
        entityManager.flush();

        List<LessonParticipantDto> participants = lessonService.getParticipants(lesson.getId());
        assertThat(participants).filteredOn(p -> p.studentId().equals(ali.getId()))
                .singleElement().satisfies(p -> assertThat(p.inDebt()).isTrue());
        assertThat(participants).filteredOn(p -> p.studentId().equals(zeynep.getId()))
                .singleElement().satisfies(p -> {
                    assertThat(p.attended()).isFalse();
                    assertThat(p.packageName()).isEqualTo("4'lü");
                });
        assertThat(studentService.search("Ali")).extracting(StudentDto::sessionDebt).containsExactly(1);
        assertThatThrownBy(() -> lessonService.completeLesson(lesson.getId(), List.of()))
                .hasMessage("lesson.error.already_completed");

        // Ali'ye paket atanınca borç bu paketten düşer ve borç satırı silinir.
        StudentPackageDto aliPkg = studentPackageService.assign(ali.getId(), pkg.getId(), false, null);
        entityManager.flush();
        assertThat(aliPkg.usedSessions()).isEqualTo(1);
        assertThat(debtRepository.count()).isZero();
        assertThat(studentService.search("Ali")).extracting(StudentDto::sessionDebt).containsExactly(0);

        // Geri al: iki paket de iade edilir, geçmiş silinir.
        lessonService.undoCompletion(lesson.getId());
        entityManager.flush();
        entityManager.clear();
        assertThat(historyRepository.count()).isZero();
        assertThat(studentPackageService.search("", null))
                .filteredOn(sp -> sp.id().equals(aliPkg.id()) || sp.id().equals(zeynepPkg.id()))
                .extracting(StudentPackageDto::usedSessions).containsOnly(0);
        assertThat(lessonRepository.findById(lesson.getId()).orElseThrow().isCompleted()).isFalse();
    }
}
