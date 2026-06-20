package com.AQARK.AQARK_RENT_MANAGEMENT.Data.DTO.FlatDTO;


import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class HostelSearchRequestDTO {


    private String roomNumber;
    private String bathroomNumber;
    private Boolean hasLivingRoom;
    private Integer floorNumber;
    private String tenantType;
    private String rentTimeType;
    private String region;
    private String wilaya;
    private Float price;

}
