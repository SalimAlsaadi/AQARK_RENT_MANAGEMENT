package com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.StatusDTO;

import com.AQARK.AQARK_RENT_MANAGEMENT.Data.Entities.StatusDomain;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DomainWithCodeDTO {
    private StatusDomain statusDomain;
    private String code;
}
