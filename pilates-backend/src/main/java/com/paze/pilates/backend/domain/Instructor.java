package com.paze.pilates.backend.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "instructor")
public class Instructor extends BaseEntity {

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(length = 100)
    private String branch;

    @Column(length = 30)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "instructor", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dayOfWeek, startTime")
    private List<InstructorAvailability> availabilities = new ArrayList<>();

    protected Instructor() {
    }

    public Instructor(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public InstructorAvailability addAvailability(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
        InstructorAvailability availability = new InstructorAvailability(this, dayOfWeek, startTime, endTime);
        availabilities.add(availability);
        return availability;
    }

    public void removeAvailability(InstructorAvailability availability) {
        availabilities.remove(availability);
    }

    public List<InstructorAvailability> getAvailabilities() {
        return Collections.unmodifiableList(availabilities);
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
