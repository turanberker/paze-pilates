package com.paze.pilates.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "session_package")
public class SessionPackage extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "session_count", nullable = false)
    private int sessionCount;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private boolean active = true;

    protected SessionPackage() {
    }

    public SessionPackage(String name, BigDecimal price, int sessionCount, LocalDate startDate) {
        this.name = name;
        this.price = price;
        this.sessionCount = sessionCount;
        this.startDate = startDate;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public int getSessionCount() { return sessionCount; }
    public void setSessionCount(int sessionCount) { this.sessionCount = sessionCount; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
