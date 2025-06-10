package com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

@Entity
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_first_name", nullable = false)
    private String tenant_first_name;

    @Column(name = "tenant_last_name", nullable = false)
    private String tenant_last_name;

    @Column(name = "mobile_number", nullable = false)
    private String mobile_number;

    @Size(max = 100)
    @Column(name = "id_card_number", nullable = false)
    private Long id_card_number;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTenant_first_name() {
        return tenant_first_name;
    }

    public void setTenant_first_name(String tenant_first_name) {
        this.tenant_first_name = tenant_first_name;
    }

    public String getTenant_last_name() {
        return tenant_last_name;
    }

    public void setTenant_last_name(String tenant_last_name) {
        this.tenant_last_name = tenant_last_name;
    }

    public String getMobile_number() {
        return mobile_number;
    }

    public void setMobile_number(String mobile_number) {
        this.mobile_number = mobile_number;
    }

    public @Size(max = 100) Long getId_card_number() {
        return id_card_number;
    }

    public void setId_card_number(@Size(max = 100) Long id_card_number) {
        this.id_card_number = id_card_number;
    }

    @Override
    public String toString() {
        return "Tenant{" +
                "id=" + id +
                ", tenant_first_name='" + tenant_first_name + '\'' +
                ", tenant_last_name='" + tenant_last_name + '\'' +
                ", mobile_number='" + mobile_number + '\'' +
                ", id_card_number=" + id_card_number +
                '}';
    }
}
