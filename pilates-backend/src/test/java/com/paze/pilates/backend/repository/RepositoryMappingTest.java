package com.paze.pilates.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.paze.pilates.backend.domain.AppUser;
import com.paze.pilates.backend.domain.Instructor;
import com.paze.pilates.backend.domain.Room;
import com.paze.pilates.backend.domain.Student;
import com.paze.pilates.backend.domain.UserRole;
import jakarta.persistence.EntityManager;
import java.time.DayOfWeek;
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
}
