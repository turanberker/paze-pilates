package com.paze.pilates.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * Öğrenciye atanmış paket. Toplam seans atama anında paketten kopyalanır;
 * paket tanımı sonradan değişse de öğrencinin hakkı değişmez.
 */
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

    @Column(nullable = false)
    private boolean paid;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Column(name = "assigned_date", nullable = false)
    private LocalDate assignedDate;

    @Column(nullable = false)
    private boolean cancelled;

    @Column(name = "cancelled_date")
    private LocalDate cancelledDate;

    protected StudentPackage() {
    }

    public StudentPackage(Student student, SessionPackage sessionPackage, LocalDate assignedDate) {
        this.student = student;
        this.sessionPackage = sessionPackage;
        this.totalSessions = sessionPackage.getSessionCount();
        this.assignedDate = assignedDate;
    }

    public Student getStudent() { return student; }

    public SessionPackage getSessionPackage() { return sessionPackage; }

    public int getTotalSessions() { return totalSessions; }

    public int getUsedSessions() { return usedSessions; }
    public void setUsedSessions(int usedSessions) { this.usedSessions = usedSessions; }

    public int getRemainingSessions() { return Math.max(0, totalSessions - usedSessions); }

    public boolean isPaid() { return paid; }
    public void setPaid(boolean paid) { this.paid = paid; }

    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }

    public LocalDate getAssignedDate() { return assignedDate; }

    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

    public LocalDate getCancelledDate() { return cancelledDate; }
    public void setCancelledDate(LocalDate cancelledDate) { this.cancelledDate = cancelledDate; }
}
