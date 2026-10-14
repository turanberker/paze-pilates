package com.paze.pilates.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Takvimde açılmış tek seferlik ders: hoca + oda + gün + saat aralığı. */
@Entity
@Table(name = "lesson")
public class Lesson extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instructor_id", nullable = false)
    private Instructor instructor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "lesson_date", nullable = false)
    private LocalDate lessonDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "completed_by", length = 100)
    private String completedBy;

    protected Lesson() {
    }

    public Lesson(Instructor instructor, Room room, LocalDate lessonDate, LocalTime startTime, LocalTime endTime) {
        this.instructor = instructor;
        this.room = room;
        this.lessonDate = lessonDate;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Instructor getInstructor() { return instructor; }

    public Room getRoom() { return room; }

    public LocalDate getLessonDate() { return lessonDate; }

    public LocalTime getStartTime() { return startTime; }

    public LocalTime getEndTime() { return endTime; }

    public LocalDateTime getStartDateTime() { return LocalDateTime.of(lessonDate, startTime); }

    public LocalDateTime getEndDateTime() { return LocalDateTime.of(lessonDate, endTime); }

    public boolean isCompleted() { return completedAt != null; }

    public LocalDateTime getCompletedAt() { return completedAt; }

    public String getCompletedBy() { return completedBy; }

    public void markCompleted(LocalDateTime at, String by) {
        this.completedAt = at;
        this.completedBy = by;
    }

    public void clearCompletion() {
        this.completedAt = null;
        this.completedBy = null;
    }
}
