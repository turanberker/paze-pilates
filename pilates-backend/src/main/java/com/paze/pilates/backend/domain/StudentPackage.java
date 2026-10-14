package com.paze.pilates.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Öğrenciye atanmış paket. Toplam seans atama anında paketten kopyalanır;
 * paket tanımı sonradan değişse de öğrencinin hakkı değişmez.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "student_package")
public class StudentPackage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_package_id", nullable = false)
    private SessionPackage sessionPackage;

    @Column(name = "total_sessions", nullable = false)
    private int totalSessions;

    @Column(name = "used_sessions", nullable = false)
    private int usedSessions;

    @Setter
    @Column(nullable = false)
    private boolean paid;

    @Setter
    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Column(name = "assigned_date", nullable = false)
    private LocalDate assignedDate;

    public StudentPackage(Student student, SessionPackage sessionPackage, LocalDate assignedDate) {
        this.student = student;
        this.sessionPackage = sessionPackage;
        this.totalSessions = sessionPackage.getSessionCount();
        this.assignedDate = assignedDate;
    }

    public boolean hasRemaining() { return usedSessions < totalSessions; }

    /** Bir seans düşer; kalan yoksa hata (çağıran önce {@link #hasRemaining()} bakar). */
    public void useSession() {
        if (!hasRemaining()) {
            throw new IllegalStateException("No remaining session in student package " + getId());
        }
        usedSessions++;
    }

    /** Tamamlama geri alınınca düşülen seansı iade eder. */
    public void refundSession() {
        if (usedSessions == 0) {
            throw new IllegalStateException("No used session to refund in student package " + getId());
        }
        usedSessions--;
    }

    public int getRemainingSessions() { return Math.max(0, totalSessions - usedSessions); }
}
