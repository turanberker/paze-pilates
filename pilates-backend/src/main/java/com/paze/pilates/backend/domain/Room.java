package com.paze.pilates.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

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

    protected Room() {
    }

    public Room(String name, int capacity) {
        this.name = name;
        this.capacity = capacity;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public String getEquipment() {
        return equipment;
    }

    public void setEquipment(String equipment) {
        this.equipment = equipment;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
