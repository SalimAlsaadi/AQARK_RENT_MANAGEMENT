package com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class EntityUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(name = "first_name", nullable = false)
    private String firstName;

    @NotBlank
    @Size(max = 100)
    @Column(name = "last_name", nullable = false)
    private String lastName;

    @NotBlank
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Invalid phone number")
    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Column(name = "date_of_birth")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "national_id", unique = true, nullable = false)
    private String nationalId;

    /**
     * Commercial Registration (CR) number. Required for landlords, null for tenants.
     * Not declared {@code unique} at the column level because SQL Server allows only a single
     * NULL under a UNIQUE constraint; uniqueness is enforced for non-null values via a filtered
     * index (see migration) and the service-layer {@code existsByCrNumber} check.
     */
    @Column(name = "cr_number", length = 50)
    private String crNumber;

    /* =========================
        SAS LINK FIELD
       ========================= */
    @Column(name = "sas_user_id", unique = true)
    private Long sasUserId;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EntityUserTypeMapping> userTypeMappings;

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EntityProperty> properties;

    /* =========================
       AUDIT FIELDS
       ========================= */

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}