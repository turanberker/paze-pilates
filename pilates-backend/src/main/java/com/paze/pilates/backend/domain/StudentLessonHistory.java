package com.paze.pilates.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Öğrencinin tamamlanan bir dersteki kaydı: geldi mi, seansı düşüldü mü ve hangi paketten düşüldü.
 * Seans düşülüp paket bulunamadıysa paket null kalır ve {@link SessionDebt} satırı açılır.
 */
@Entity
@Table(name = "student_lesson_history")
public class StudentLessonHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_participant_id", nullable = false, unique = true)
    private LessonParticipant participant;

    @Column(nullable = false)
    private boolean attended;

    @Column(nullable = false)
    private boolean charged;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_package_id")
    private StudentPackage studentPackage;

    protected StudentLessonHistory() {
    }

    public StudentLessonHistory(LessonParticipant participant, boolean attended, boolean charged) {
        this.lesson = participant.getLesson();
        this.student = participant.getStudent();
        this.participant = participant;
        this.attended = attended;
        this.charged = charged;
    }

    /** Seansı verilen paketten düşer. */
    public void chargeTo(StudentPackage pkg) {
        pkg.useSession();
        this.studentPackage = pkg;
    }

    public Lesson getLesson() { return lesson; }

    public Student getStudent() { return student; }

    public LessonParticipant getParticipant() { return participant; }

    public boolean isAttended() { return attended; }

    public boolean isCharged() { return charged; }

    public StudentPackage getStudentPackage() { return studentPackage; }
}
