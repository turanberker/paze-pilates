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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @OneToMany(mappedBy = "instructor", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dayOfWeek, startTime")
    private List<InstructorAvailability> availabilities = new ArrayList<>();

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
}
