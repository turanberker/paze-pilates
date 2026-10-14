package com.paze.pilates.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "room")
public class Room extends BaseEntity {

    @Column(nullable = false, length = 100, unique = true)
    private String name;

    @Column(nullable = false)
    private int capacity;

    /** Serbest metin ekipman listesi (ör. "Reformer x4, Cadillac"). */
    @Column(length = 1000)
    private String equipment;

    @Column(nullable = false)
    private boolean active = true;

    public Room(String name, int capacity) {
        this.name = name;
        this.capacity = capacity;
    }
}
