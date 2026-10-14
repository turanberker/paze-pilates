package com.paze.pilates.backend.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/** Paketi olmadığı için düşülemeyen seans. Öğrenciye paket atanınca düşülür ve satır silinir. */
@Entity
@Table(name = "session_debt")
public class SessionDebt extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "history_id", nullable = false, unique = true)
    private StudentLessonHistory history;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    protected SessionDebt() {
    }

    public SessionDebt(StudentLessonHistory history) {
        this.history = history;
        this.student = history.getStudent();
    }

    public StudentLessonHistory getHistory() { return history; }

    public Student getStudent() { return student; }
}
