package com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ref_types", schema = "app_root")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String code;

    private String description;

    @Column(name = "is_active")
    private boolean active;
}

