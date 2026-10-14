package com.paze.pilates.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "studio")
public class Studio extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String address;

    @Column(length = 30)
    private String phone;

    /** Header logosu (PNG/JPEG); null ise varsayılan görünüm kullanılır. */
    @Lob
    @Column(name = "logo")
    private byte[] logo;

    public Studio(String name) {
        this.name = name;
    }
}
