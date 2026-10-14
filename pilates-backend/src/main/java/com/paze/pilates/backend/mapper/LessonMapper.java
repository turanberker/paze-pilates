package com.paze.pilates.backend.mapper;

import com.paze.pilates.backend.domain.Instructor;
import com.paze.pilates.backend.domain.Lesson;
import com.paze.pilates.backend.domain.LessonParticipant;
import com.paze.pilates.backend.domain.Student;
import com.paze.pilates.backend.domain.StudentLessonHistory;
import com.paze.pilates.dto.LessonDto;
import com.paze.pilates.dto.LessonParticipantDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LessonMapper {

    @Mapping(target = "id", source = "lesson.id")
    @Mapping(target = "instructorId", source = "lesson.instructor.id")
    @Mapping(target = "instructorName", source = "lesson.instructor")
    @Mapping(target = "roomId", source = "lesson.room.id")
    @Mapping(target = "roomName", source = "lesson.room.name")
    @Mapping(target = "capacity", source = "lesson.room.capacity")
    @Mapping(target = "date", source = "lesson.lessonDate")
    @Mapping(target = "startTime", source = "lesson.startTime")
    @Mapping(target = "endTime", source = "lesson.endTime")
    @Mapping(target = "completed", source = "lesson.completed")
    LessonDto toDto(Lesson lesson, int participantCount);

    /** Ders tamamlanmadıysa {@code history} null'dır; katılım ve paket bilgisi boş kalır. */
    @Mapping(target = "id", source = "participant.id")
    @Mapping(target = "lessonId", source = "participant.lesson.id")
    @Mapping(target = "studentId", source = "participant.student.id")
    @Mapping(target = "studentName", source = "participant.student")
    @Mapping(target = "phone", source = "participant.student.phone")
    @Mapping(target = "attended", source = "history.attended")
    @Mapping(target = "charged", source = "history.charged")
    @Mapping(target = "packageName", source = "history.studentPackage.sessionPackage.name")
    LessonParticipantDto toParticipantDto(LessonParticipant participant, StudentLessonHistory history);

    default String fullName(Instructor instructor) {
        return instructor == null ? null : instructor.getFirstName() + " " + instructor.getLastName();
    }

    default String fullName(Student student) {
        return student == null ? null : student.getFirstName() + " " + student.getLastName();
    }
}
