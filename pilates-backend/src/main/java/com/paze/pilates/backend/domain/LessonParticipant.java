package com.paze.pilates.backend.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Bir derse eklenmiş öğrenci (katılımcı). */
@Entity
@Table(name = "lesson_participant")
public class LessonParticipant extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    protected LessonParticipant() {
    }

    public LessonParticipant(Lesson lesson, Student student) {
        this.lesson = lesson;
        this.student = student;
    }

    public Lesson getLesson() { return lesson; }

    public Student getStudent() { return student; }
}
