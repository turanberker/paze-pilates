package com.paze.pilates.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.paze.pilates.backend.domain.AppUser;
import com.paze.pilates.backend.domain.Instructor;
import com.paze.pilates.backend.domain.Lesson;
import com.paze.pilates.backend.domain.LessonParticipant;
import com.paze.pilates.backend.domain.Room;
import com.paze.pilates.backend.domain.SessionPackage;
import com.paze.pilates.backend.domain.Student;
import com.paze.pilates.backend.domain.StudentPackage;
import com.paze.pilates.backend.domain.UserRole;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Flyway şeması ile entity eşlemelerinin uyumunu ve temel sorguları doğrular.
 */
@DataJpaTest
class RepositoryMappingTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private InstructorRepository instructorRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private SessionPackageRepository packageRepository;

    @Autowired
    private StudentPackageRepository studentPackageRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private LessonParticipantRepository participantRepository;

    @Test
    void savesUserWithTimestampsAndRejectsDuplicateUsername() {
        AppUser user = userRepository.saveAndFlush(new AppUser("admin", "hash", "Yönetici", UserRole.ADMIN));

        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(userRepository.findByUsername("admin")).isPresent();
        assertThatThrownBy(() -> userRepository.saveAndFlush(new AppUser("admin", "x", "Başka", UserRole.RECEPTIONIST)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void listsOnlyActiveRoomsByName() {
        roomRepository.save(new Room("Reformer Salonu", 6));
        Room inactive = new Room("Eski Salon", 4);
        inactive.setActive(false);
        roomRepository.save(inactive);
        roomRepository.save(new Room("Mat Salonu", 12));

        assertThat(roomRepository.findByActiveTrueOrderByNameAsc())
                .extracting(Room::getName)
                .containsExactly("Mat Salonu", "Reformer Salonu");
        assertThat(roomRepository.existsByNameIgnoreCase("mat salonu")).isTrue();
    }

    @Test
    void persistsInstructorAvailabilitiesInOrder() {
        Instructor instructor = new Instructor("Ayşe", "Yılmaz");
        instructor.addAvailability(DayOfWeek.WEDNESDAY, LocalTime.of(10, 0), LocalTime.of(14, 0));
        instructor.addAvailability(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0));
        Long id = instructorRepository.saveAndFlush(instructor).getId();
        entityManager.clear();

        Instructor loaded = instructorRepository.findWithAvailabilitiesById(id).orElseThrow();

        assertThat(loaded.getAvailabilities())
                .extracting(a -> a.getDayOfWeek())
                .containsExactly(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY);
    }

    @Test
    void removesOrphanAvailability() {
        Instructor instructor = new Instructor("Can", "Demir");
        instructor.addAvailability(DayOfWeek.FRIDAY, LocalTime.of(17, 0), LocalTime.of(20, 0));
        Long id = instructorRepository.saveAndFlush(instructor).getId();

        instructor.removeAvailability(instructor.getAvailabilities().get(0));
        instructorRepository.saveAndFlush(instructor);
        entityManager.clear();

        assertThat(instructorRepository.findWithAvailabilitiesById(id).orElseThrow().getAvailabilities()).isEmpty();
    }

    @Test
    void searchesActiveStudentsByNameOrPhone() {
        Student ali = new Student("Ali", "Kaya");
        ali.setPhone("05321112233");
        studentRepository.save(ali);
        Student zeynep = new Student("Zeynep", "Kara");
        zeynep.setPhone("05449998877");
        studentRepository.save(zeynep);
        Student passive = new Student("Ali", "Pasif");
        passive.setActive(false);
        studentRepository.save(passive);

        assertThat(studentRepository.searchActive("ali")).extracting(Student::getLastName).containsExactly("Kaya");
        assertThat(studentRepository.searchActive("9998")).extracting(Student::getFirstName).containsExactly("Zeynep");
        assertThat(studentRepository.searchActive("ka")).hasSize(2);
    }

    @Test
    void searchesStudentPackagesByNameAndRemaining() {
        Student ali = studentRepository.save(new Student("Ali", "Kaya"));
        Student zeynep = studentRepository.save(new Student("Zeynep", "Kara"));
        SessionPackage pkg = packageRepository.save(
                new SessionPackage("4'lü", new BigDecimal("800"), 4, LocalDate.of(2026, 1, 1)));

        studentPackageRepository.save(new StudentPackage(ali, pkg, LocalDate.of(2026, 2, 1)));
        StudentPackage finished = new StudentPackage(zeynep, pkg, LocalDate.of(2026, 3, 1));
        finished.setUsedSessions(4);
        studentPackageRepository.saveAndFlush(finished);
        entityManager.clear();

        assertThat(studentPackageRepository.search("", false, false))
                .extracting(sp -> sp.getStudent().getFirstName())
                .containsExactly("Zeynep", "Ali");
        assertThat(studentPackageRepository.search("", true, false))
                .extracting(sp -> sp.getStudent().getFirstName()).containsExactly("Ali");
        assertThat(studentPackageRepository.search("", false, true))
                .extracting(sp -> sp.getStudent().getFirstName()).containsExactly("Zeynep");
        assertThat(studentPackageRepository.search("ZEY", false, false)).hasSize(1);
        assertThat(packageRepository.findByActiveTrueOrderByNameAsc()).hasSize(1);
    }

    @Test
    void detectsRoomAndInstructorOverlapButNotBackToBack() {
        Room room = roomRepository.save(new Room("Reformer", 6));
        Room otherRoom = roomRepository.save(new Room("Mat", 10));
        Instructor ayse = instructorRepository.save(new Instructor("Ayşe", "Yılmaz"));
        Instructor can = instructorRepository.save(new Instructor("Can", "Demir"));
        LocalDate day = LocalDate.of(2026, 11, 2);
        lessonRepository.saveAndFlush(new Lesson(ayse, room, day, LocalTime.of(10, 0), LocalTime.of(11, 0)));
        entityManager.clear();

        // kısmi çakışma
        assertThat(lessonRepository.existsRoomOverlap(room.getId(), day, LocalTime.of(10, 30), LocalTime.of(11, 30))).isTrue();
        assertThat(lessonRepository.existsInstructorOverlap(ayse.getId(), day, LocalTime.of(9, 30), LocalTime.of(10, 30))).isTrue();
        // arka arkaya çakışma değil
        assertThat(lessonRepository.existsRoomOverlap(room.getId(), day, LocalTime.of(11, 0), LocalTime.of(12, 0))).isFalse();
        assertThat(lessonRepository.existsInstructorOverlap(ayse.getId(), day, LocalTime.of(9, 0), LocalTime.of(10, 0))).isFalse();
        // başka oda / hoca / gün
        assertThat(lessonRepository.existsRoomOverlap(otherRoom.getId(), day, LocalTime.of(10, 0), LocalTime.of(11, 0))).isFalse();
        assertThat(lessonRepository.existsInstructorOverlap(can.getId(), day, LocalTime.of(10, 0), LocalTime.of(11, 0))).isFalse();
        assertThat(lessonRepository.existsRoomOverlap(room.getId(), day.plusDays(1), LocalTime.of(10, 0), LocalTime.of(11, 0))).isFalse();
        // hafta sorgusu
        assertThat(lessonRepository.findBetween(day, day.plusDays(6))).hasSize(1);
        assertThat(lessonRepository.findBetween(day.plusDays(1), day.plusDays(6))).isEmpty();
    }

    @Test
    void storesParticipantsAndRejectsDuplicates() {
        Room room = roomRepository.save(new Room("Reformer", 6));
        Instructor ayse = instructorRepository.save(new Instructor("Ayşe", "Yılmaz"));
        Student ali = studentRepository.save(new Student("Ali", "Kaya"));
        Student zeynep = studentRepository.save(new Student("Zeynep", "Kara"));
        LocalDate day = LocalDate.of(2026, 11, 2);
        Lesson lesson = lessonRepository.save(new Lesson(ayse, room, day, LocalTime.of(10, 0), LocalTime.of(11, 0)));
        participantRepository.save(new LessonParticipant(lesson, zeynep));
        participantRepository.saveAndFlush(new LessonParticipant(lesson, ali));
        entityManager.clear();

        assertThat(participantRepository.findByLessonId(lesson.getId()))
                .extracting(p -> p.getStudent().getFirstName()).containsExactly("Zeynep", "Ali");
        assertThat(participantRepository.countByLessonId(lesson.getId())).isEqualTo(2);
        assertThat(participantRepository.existsStudentOverlap(ali.getId(), day, LocalTime.of(10, 30), LocalTime.of(11, 30))).isTrue();
        assertThat(participantRepository.existsStudentOverlap(ali.getId(), day, LocalTime.of(11, 0), LocalTime.of(12, 0))).isFalse();
        assertThat(participantRepository.countByLessonIds(java.util.List.of(lesson.getId())))
                .hasSize(1).first().satisfies(r -> assertThat(r[1]).isEqualTo(2L));
        assertThatThrownBy(() -> participantRepository.saveAndFlush(
                new LessonParticipant(lessonRepository.findById(lesson.getId()).orElseThrow(),
                        studentRepository.findById(ali.getId()).orElseThrow())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
